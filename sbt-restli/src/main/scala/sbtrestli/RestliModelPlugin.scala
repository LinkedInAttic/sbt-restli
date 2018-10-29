package sbtrestli

import java.io.File
import java.net.URLClassLoader

import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap
import com.linkedin.restli.tools.idlcheck.{CompatibilityLevel, RestLiResourceModelCompatibilityChecker}
import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp
import com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker
import com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporterCmdLineApp
import org.scaladebugger.SbtJdiTools
import sbt.Keys._
import sbt._

import scala.annotation.tailrec

object RestliModelPlugin extends AutoPlugin {
  object autoImport {
    val restliModelApi = settingKey[ProjectReference]("API project to publish idl and snapshot files to.")
    val restliModelCompat = settingKey[String]("Rest model backwards compatibility level (defaults to equivalent).")
    val restliModelResourcePackages = settingKey[Seq[String]]("List of packages containing Restli resources (optional, by default searches all packages in sourceDirectory).")
    val restliModelGenerate = taskKey[Seq[File]]("Generates *.restspec.json & *.snapshot.json files from Restli resources.")
    val restliModelPublish = taskKey[Unit]("Validates and publishes idl and snapshot files to the API project.")
    val restliModelPackage = taskKey[File]("Package idl files into *-rest-model.jar")

    val restliModelDefaults: Seq[Def.Setting[_]] = Seq(
      restliModelApi := thisProjectRef.value,
      restliModelResourcePackages := Seq(),
      restliModelCompat := "backwards"
    )

    val restliModelSettings: Seq[Def.Setting[_]] = Seq(
      target in restliModelGenerate :=
        baseDirectory.value / "src" / (Defaults.nameForSrc(configuration.value.name) + "GeneratedRest"),

      PluginCompat.cleanFilesSetting(restliModelGenerate),

      runner in restliModelGenerate := new ForkRun(ForkOptions(
        javaHome.value,
        outputStrategy = Some(StdoutOutput),
        bootJars = Vector.empty, workingDirectory = Some(baseDirectory.value),
        runJVMOptions = javaOptions.value.toVector :+
          s"-Dgenerator.resolver.path=${resolverPath.value}",
        connectInput = false,
        envVars.value
      )),

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

  override def requires: Plugins = SbtJdiTools

  override def projectSettings: Seq[Def.Setting[_]] = restliModelDefaults ++
    inConfig(Compile)(restliModelSettings) ++ inConfig(Test)(restliModelSettings)

  private lazy val resolverFiles = Def.task {
    managedClasspath.value.files ++ internalDependencyClasspath.value.files
  }

  private lazy val resolverPath = Def.task {
    resolverFiles.value.map(_.getAbsolutePath).mkString(File.pathSeparator)
  }

  private def generatorTarget(targetDir: String) = Def.task {
    (target in restliModelGenerate).value / targetDir
  }

  private def generatorArgs(targetDir: String) = Def.task {
    val sourcepath = "-sourcepath" +: sourceDirectories.value.map(_.getAbsolutePath)
    val outdir = "-outdir" :: generatorTarget(targetDir).value.getAbsolutePath :: Nil
    val resourcepackages = if (restliModelResourcePackages.value.nonEmpty) {
      "-resourcepackages" +: restliModelResourcePackages.value
    } else Nil

    sourcepath ++ outdir ++ resourcepackages :+ "-loadAdditionalDocProviders"
  }

  @tailrec
  private def localClasspath(cl: ClassLoader, files: List[File] = Nil): List[File] = cl match {
    case null => files
    case classLoader: URLClassLoader =>
      localClasspath(cl.getParent, files ::: classLoader.getURLs.map(url => new File(url.toURI)).toList)
    case _ => localClasspath(cl.getParent, files)
  }

  private def packageToDir(p: String) = p.replace(".", java.io.File.separator)

  private lazy val generate = Def.taskDyn {
    val idlGeneratorArgs = generatorArgs("idl").value
    val snapshotGeneratorArgs = generatorArgs("snapshot").value

    val scopedRunner = (runner in restliModelGenerate).value
    val classpath = fullClasspath.value.files ++ localClasspath(getClass.getClassLoader)
    val restliModelTarget = (target in restliModelGenerate).value

    val resourceProducts = products.value
    val resourcePackages = restliModelResourcePackages.value
    val classFiles = if (resourcePackages.nonEmpty) {
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

    val idlTarget = generatorTarget("idl").value
    val snapshotTarget = generatorTarget("snapshot").value

    Def.task {
      val cachedAction = FileFunction.cached(cacheDir, FilesInfo.lastModified, FilesInfo.exists){ _ =>
        log.info(s"Compiling rest model to $restliModelTarget ...")

        // Clean target dir
        IO.delete(restliModelTarget)

        scopedRunner.run(classOf[RestLiResourceModelExporterCmdLineApp].getName, classpath, idlGeneratorArgs, Logger.Null)
        scopedRunner.run(classOf[RestLiSnapshotExporterCmdLineApp].getName, classpath, snapshotGeneratorArgs, Logger.Null)

        val idlFiles = (idlTarget * "*.restspec.json").get
        val snapshotFiles = (snapshotTarget * "*.snapshot.json").get

        (idlFiles ++ snapshotFiles).toSet
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

  trait RestModelChecker {
    def setResolverPath(resolverPath: String): Unit
    def check(prevPath: String, curPath: String, compatLevel: CompatibilityLevel): Any
    def getInfoMap: CompatibilityInfoMap
  }

  class SnapshotChecker extends RestLiSnapshotCompatibilityChecker with RestModelChecker
  class IdlChecker extends RestLiResourceModelCompatibilityChecker with RestModelChecker

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
