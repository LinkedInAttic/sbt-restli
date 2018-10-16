package sbtrestli

import java.io.File
import java.net.URLClassLoader

import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel
import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp
import com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporterCmdLineApp
import org.scaladebugger.SbtJdiTools
import sbt.Keys._
import sbt._
import sbtrestli.util.{IdlChecker, RestModelChecker, SnapshotChecker}

import scala.annotation.tailrec

object RestModelPlugin extends AutoPlugin {
  object autoImport {
    val restModelApi = settingKey[ProjectReference]("API project to publish idl and snapshot files to.")
    val restModelCompat = settingKey[String]("Rest model backwards compatibility level (defaults to equivalent).")
    val restModelPackages = settingKey[Seq[String]]("List of packages containing Restli resources (optional, by default searches all packages in sourceDirectory).")
    val restModelGenerate = taskKey[Unit]("Generates *.restspec.json & *.snapshot.json files from Restli resources.")
    val restModelPublish = taskKey[Unit]("Validates and publishes idl and snapshot files to the API project.")

    val restModelDefaults: Seq[Def.Setting[_]] = Seq(
      restModelPackages := Seq(),
      restModelCompat := "backwards"
    )

    val restModelSettings: Seq[Def.Setting[_]] = Seq(
      target in restModelGenerate :=
        baseDirectory.value / "src" / (Defaults.nameForSrc(configuration.value.name) + "GeneratedRest"),

      PluginCompat.cleanFilesSetting(restModelGenerate),

      runner in restModelGenerate := new ForkRun(ForkOptions(
        javaHome.value,
        outputStrategy = Some(StdoutOutput),
        bootJars = Vector.empty, workingDirectory = Some(baseDirectory.value),
        runJVMOptions = javaOptions.value.toVector :+
          s"-Dgenerator.resolver.path=${resolverPath.value}",
        connectInput = false,
        envVars.value
      )),

      restModelGenerate := generate.value,
      restModelPublish := publish.dependsOn(restModelGenerate).triggeredBy(compile).value
    )
  }

  import autoImport._

  override def requires: Plugins = SbtJdiTools

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(restModelSettings) ++ inConfig(Test)(restModelSettings) ++ restModelDefaults ++ Seq(
      libraryDependencies += "com.linkedin.pegasus" % "restli-server" % BuildInfo.pegasusVersion
    )

  private lazy val resolverPath = Def.task {
    fullClasspath.value.files.map(_.getAbsolutePath).mkString(File.pathSeparator)
  }

  private def generatorTarget(targetDir: String) = Def.task {
    (target in restModelGenerate).value / targetDir
  }

  private def generatorArgs(targetDir: String) = Def.task {
    val sourcepath = "-sourcepath" +: sourceDirectories.value.map(_.getAbsolutePath)
    val outdir = "-outdir" :: generatorTarget(targetDir).value.getAbsolutePath :: Nil
    val resourcepackages = if (restModelPackages.value.nonEmpty) {
      "-resourcepackages" +: restModelPackages.value
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

  private lazy val generate = Def.taskDyn {
    val idlGeneratorArgs = generatorArgs("idl").value
    val snapshotGeneratorArgs = generatorArgs("snapshot").value

    val scopedRunner = (runner in restModelGenerate).value
    val classpath = fullClasspath.value.files ++ localClasspath(getClass.getClassLoader)

    Def.task {
      // Clean target dir
      IO.delete((target in restModelGenerate).value)
      streams.value.log

      scopedRunner.run(classOf[RestLiResourceModelExporterCmdLineApp].getName, classpath, idlGeneratorArgs, streams.value.log)
      scopedRunner.run(classOf[RestLiSnapshotExporterCmdLineApp].getName, classpath, snapshotGeneratorArgs, streams.value.log)
    }
  }

  private def fileMappings(inputDir: File, outputDir: File, fileFilter: FileFilter): Seq[(File, File)] = {
    val inputFiles = (inputDir ** fileFilter).get.view
    val outputFiles = (outputDir ** fileFilter).get.view

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

  object IncompatibleChangesException extends Exception with FeedbackProvidedException

  private lazy val publish = Def.taskDyn {
    val apiProj = restModelApi.value
    val compatLevel = CompatibilityLevel.valueOf(restModelCompat.value.toUpperCase())
    val resPath = resolverPath.value
    val log = streams.value.log

    Def.task {
      log.info(s"Running rest model compatibility checker (compat level: '$compatLevel').")

      val targetDir = (sourceDirectory in apiProj).value

      val idlFilePairs = fileMappings(generatorTarget("idl").value, targetDir / "idl", "*.restspec.json")
      val snapshotFilePairs = fileMappings(generatorTarget("snapshot").value, targetDir / "snapshot", "*.snapshot.json")

      val idlChecker = new IdlChecker()
      val snapshotChecker = new SnapshotChecker()

      val compatibilityInfoMap = new CompatibilityInfoMap

      // Only validate existence of idl files, and do full check for snapshot files (snapshots are a superset of idl files).
      checkFiles(missingFiles(idlFilePairs), idlChecker, compatibilityInfoMap, resPath, compatLevel)
      checkFiles(snapshotFilePairs, snapshotChecker, compatibilityInfoMap, resPath, compatLevel)

      if (compatibilityInfoMap.isEquivalent) {
        log.info("Rest model is equivalent.")
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
  }
}
