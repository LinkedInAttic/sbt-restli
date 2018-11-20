/*
   Copyright (c) 2018 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.sbtrestli

import java.io.File

import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider
import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap
import com.linkedin.restli.tools.idlcheck.{CompatibilityLevel, RestLiResourceModelCompatibilityChecker}
import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporter
import com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker
import com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporter
import com.linkedin.sbtrestli.tools.scala.ScalaDocsProvider
import org.apache.logging.log4j.{Level => XLevel}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

object RestliModelPlugin extends AutoPlugin {
  object autoImport {
    val restliModelApi = settingKey[ProjectReference]("API project to publish idl and snapshot files to.")
    val restliModelCompat = settingKey[String]("Rest model backwards compatibility level (defaults to backwards).")
    val restliModelResourcePackages = taskKey[Seq[String]]("List of packages containing Restli resources (optional, by default searches all packages in sourceDirectory).")
    val restliModelGenerate = taskKey[Seq[File]]("Generates *.restspec.json & *.snapshot.json files from Restli resources.")
    val restliModelPublish = taskKey[Unit]("Validates and publishes idl and snapshot files to the API project.")
    val restliModelPackage = taskKey[File]("Package idl files into *-rest-model.jar")

    val restliModelDefaults: Seq[Def.Setting[_]] = Seq(
      restliModelApi := thisProjectRef.value,
      restliModelResourcePackages := Seq(),
      restliModelCompat := "backwards"
    )

    val restliModelSettings: Seq[Def.Setting[_]] = Seq(
      target in restliModelGenerate := file(sourceDirectory.value + "GeneratedRest"),

      PluginCompat.cleanFilesSetting(restliModelGenerate),

      restliModelGenerate := generate.value,
      restliModelPublish := publish.dependsOn(restliModelGenerate).value,

      artifactClassifier in restliModelPackage := Some("data-template"),
      publishArtifact in restliModelPackage := true,

      packagedArtifacts in Defaults.ConfigGlobal ++= Classpaths.packaged(Seq(restliModelPackage)).value,
      artifacts in Defaults.ConfigGlobal ++= Classpaths.artifactDefs(Seq(restliModelPackage)).value
    ) ++ Defaults.packageTaskSettings(restliModelPackage, Def.task {
      // Generate idl files
      restliModelGenerate.value

      val sourceDir = generatorTarget("idl").value
      val originalSources = sourceDir * "*.restspec.json"
      originalSources pair Path.rebase(sourceDir, "idl/")
    })
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = restliModelDefaults ++
    inConfig(Compile)(restliModelSettings) ++ inConfig(Test)(restliModelSettings)

  private lazy val resolverFiles = Def.task {
    sourceDirectories.value ++ managedClasspath.value.files ++ internalDependencyClasspath.value.files
  }

  private lazy val resolverPath = Def.task {
    resolverFiles.value.map(_.getAbsolutePath).mkString(File.pathSeparator)
  }

  private lazy val javaTools = Def.task {
    val javaVersion = System.getProperty("java.specification.version")
    val home = javaHome.value.getOrElse(file(System.getProperty("java.home")).getParentFile)
    val jar =  home / "lib" / "tools.jar"

    // Versions strings prior to java 9 start with major version number 1
    if (javaVersion.startsWith("1.")) {
      require(jar.exists(), s"lib/tools.jar not found in $home\n Configure home location using the javaHome setting.")
      Some(jar.toURI.toURL)
    } else {
      // tools.jar not included in JDK 9+
      // https://openjdk.java.net/jeps/220#Removed:-rt-jar-and-tools-jar
      None
    }
  }

  private lazy val generatorClasspath = Def.task {
    fullClasspath.value.files.map(_.getAbsolutePath).toArray
  }

  private lazy val generatorClassLoader = Def.task {
    val classLoader = getClass.getClassLoader.asInstanceOf[PluginCompat.PluginClassLoader]

    // Add java tools.jar URL to plugin classloader instead of the new classloader since:
    // 1) Tools.jar must be on the classpath of the classloader which loads Restli DocletDocsProvider.
    // 2) RestLiClasspathScanner checks annotation types of loaded resources, and class types are only guaranteed
    //    to be singletons (and thus have reference equality) if they are loaded by the same classloader.
    classLoader.add(javaTools.value.toSeq)

    ClasspathUtil.classLoaderFromClasspath(generatorClasspath.value, classLoader)
  }

  private def generatorTarget(targetDir: String) = Def.task {
    (target in restliModelGenerate).value / targetDir
  }

  private def packageToDir(p: String) = p.replace(".", java.io.File.separator)

  private lazy val generate = Def.taskDyn {
    val resourceProducts = products.value
    // Resource packages should be null if none are provided, so exporters will scan all packages for Restli annotations
    val resourcePackages = Option(restliModelResourcePackages.value).filter(_.nonEmpty).map(_.toArray).orNull

    val classFiles = if (resourcePackages != null) {
      // Find class files corresponding to each resource package if specified
      val resourceClassFiles = for {
        targetDir <- resourceProducts
        resourcePackage <- resourcePackages
      } yield {
        ((targetDir / packageToDir(resourcePackage)) ** "*.class").get
      }
      resourceClassFiles.flatten
    } else {
      // Otherwise we scan all class files
      (resourceProducts ** "*.class").get
    }

    val dataSchemas = (resolverFiles.value ** "*.pdsc").get
    val allSources = (classFiles ++ dataSchemas).toSet
    val cacheDir = streams.value.cacheDirectory / "restliModelGenerate"
    val log = streams.value.log

    val sourceDirs = unmanagedSourceDirectories.value.map(_.getAbsolutePath).toArray
    val classpath = generatorClasspath.value
    val resPath = resolverPath.value

    val restliModelTarget = (target in restliModelGenerate).value
    val idlTarget = generatorTarget("idl").value.getAbsolutePath
    val snapshotTarget = generatorTarget("snapshot").value.getAbsolutePath

    val scalaDocsProvider: DocsProvider = new ScalaDocsProvider(resolverFiles.value.map(_.getAbsolutePath).toArray)
    val docsProviders = List(scalaDocsProvider).asJava

    val classLoader = generatorClassLoader.value

    Def.task {
      val cachedAction = FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.exists){ _ =>
        log.info(s"Compiling rest model to $restliModelTarget ...")

        // Clean target dir
        IO.delete(restliModelTarget)

        ClasspathUtil.withContextClassLoader(classLoader) {
          val snapshotExporter = new RestLiSnapshotExporter()
          val idlExporter = new RestLiResourceModelExporter()

          PluginCompat.setLogLevel(classOf[RestLiSnapshotExporter].getName, XLevel.WARN)
          PluginCompat.setLogLevel(classOf[RestLiResourceModelExporter].getName, XLevel.WARN)

          snapshotExporter.setResolverPath(resPath)
          val snapshotRes = snapshotExporter.export(
            null,
            classpath,
            sourceDirs,
            resourcePackages,
            null,
            snapshotTarget,
            docsProviders
          )

          val idlRes = idlExporter.export(
            null,
            classpath,
            sourceDirs,
            resourcePackages,
            null,
            idlTarget,
            docsProviders
          )

          (snapshotRes.getTargetFiles.asScala ++ idlRes.getTargetFiles.asScala).toSet
        }
      }

      cachedAction(allSources).toSeq
    }
  }

  private def fileMappings(inputDir: File, outputDir: File, fileFilter: FileFilter): Seq[(File, File)] = {
    val inputFiles = (inputDir * fileFilter).get
    val outputFiles = (outputDir * fileFilter).get

    val allFiles = (inputFiles ++ outputFiles).map(_.getName).toSet

    allFiles.map(file => (inputDir / file, outputDir / file)).toSeq
  }

  private def missingFiles(filePairs: Seq[(File, File)]): Seq[(File, File)] = {
    filePairs.filterNot {
      case (curFile, prevFile) => curFile.exists && prevFile.exists
    }
  }

  private def checkFiles(
    filePairs: Seq[(File, File)],
    checker: RestModelChecker,
    compatibilityInfoMap: CompatibilityInfoMap,
    resolverPath: String,
    compatibilityLevel: CompatibilityLevel
  ): Unit = {
    checker.setResolverPath(resolverPath)
    for ((curFile, prevFile) <- filePairs) {
      checker.check(prevFile.toString, curFile.toString, compatibilityLevel)
    }
    compatibilityInfoMap.addAll(checker.getInfoMap)
  }

  private object IncompatibleChangesException extends Exception with FeedbackProvidedException

  private trait RestModelChecker {
    def setResolverPath(resolverPath: String): Unit
    def check(prevPath: String, curPath: String, compatLevel: CompatibilityLevel): Any
    def getInfoMap: CompatibilityInfoMap
  }

  private class SnapshotChecker extends RestLiSnapshotCompatibilityChecker with RestModelChecker
  private class IdlChecker extends RestLiResourceModelCompatibilityChecker with RestModelChecker

  private lazy val publish = Def.taskDyn {
    val apiProj = restliModelApi.value
    val compatLevel = CompatibilityLevel.valueOf(restliModelCompat.value.toUpperCase())
    val resPath = resolverPath.value
    val log = streams.value.log
    val cacheDir = streams.value.cacheDirectory / "restliModelPublish"

    Def.task {
      val targetDir = (sourceDirectory in apiProj).value
      val idlFilePairs = fileMappings(generatorTarget("idl").value, targetDir / "idl", "*.restspec.json")
      val snapshotFilePairs = fileMappings(generatorTarget("snapshot").value, targetDir / "snapshot", "*.snapshot.json")

      val cachedAction = FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.lastModified) { _ =>
        log.info(s"Running rest model compatibility tests (compat level: '$compatLevel') ...")

        val idlChecker = new IdlChecker()
        val snapshotChecker = new SnapshotChecker()

        val compatibilityInfoMap = new CompatibilityInfoMap

        // Only validate existence of idl files, and do full check for snapshot files (snapshots are a superset of idl files).
        checkFiles(missingFiles(idlFilePairs), idlChecker, compatibilityInfoMap, resPath, compatLevel)
        checkFiles(snapshotFilePairs, snapshotChecker, compatibilityInfoMap, resPath, compatLevel)

        if (compatibilityInfoMap.isEquivalent) {
          log.info("Rest model is equivalent: not publishing.")
          (idlFilePairs ++ snapshotFilePairs).map(_._2).toSet
        } else {
          val summary = compatibilityInfoMap.createSummary()

          if (compatibilityInfoMap.isCompatible(compatLevel)) {
            val apiProjectName = (name in apiProj).value
            val apiProjectDir = (baseDirectory in apiProj).value.getAbsolutePath

            log.info(summary)
            log.info(s"Publishing rest model to API project ($apiProjectName, $apiProjectDir) ...")

            IO.copy(idlFilePairs ++ snapshotFilePairs)
          } else {
            log.error(summary)
            throw IncompatibleChangesException
          }
        }
      }

      val allSources = (idlFilePairs ++ snapshotFilePairs).map(_._1).toSet
      cachedAction(allSources).toSeq
    }
  }
}
