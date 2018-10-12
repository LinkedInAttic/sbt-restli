package sbtrestli

import java.io.File
import java.net.URLClassLoader

import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp
import com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporterCmdLineApp
import org.scaladebugger.SbtJdiTools
import sbt.Keys._
import sbt._

import scala.annotation.tailrec

object RestModelPlugin extends AutoPlugin {
  object autoImport {
    val restModelPackages = settingKey[Seq[String]]("List of packages containing Restli resources (optional, by default searches all packages in sourceDirectory).")
    val restModelGenerate = taskKey[Unit]("Generates *.restspec.json & *.snapshot.json files from Restli resources.")

    val restModelSettings: Seq[Def.Setting[_]] = Seq(
      restModelPackages := Seq(),

      target in restModelGenerate :=
        baseDirectory.value / "src" / (Defaults.nameForSrc(configuration.value.name) + "GeneratedRest"),

      PluginCompat.cleanFilesSetting(restModelGenerate),

      runner in restModelGenerate := new ForkRun(ForkOptions(
        javaHome.value,
        outputStrategy = Some(StdoutOutput),
        bootJars = Vector.empty,
        workingDirectory = Some(baseDirectory.value),
        runJVMOptions = javaOptions.value.toVector :+
          s"-Dgenerator.resolver.path=${fullClasspath.value.files.map(_.getAbsolutePath).mkString(File.pathSeparator)}",
        connectInput = false,
        envVars.value
      )),

      restModelGenerate := generate.triggeredBy(compile).value
    )
  }

  import autoImport._

  override def requires: Plugins = SbtJdiTools

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(restModelSettings) ++ inConfig(Test)(restModelSettings) ++ Seq(
      libraryDependencies += "com.linkedin.pegasus" % "restli-server" % BuildInfo.pegasusVersion
    )

  private def generatorArgs(targetDir: String) = Def.task {
    val sourcepath = "-sourcepath" +: sourceDirectories.value.map(_.getAbsolutePath)
    val outdir = "-outdir" :: ((target in restModelGenerate).value / targetDir).getAbsolutePath :: Nil
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
      scopedRunner.run(classOf[RestLiResourceModelExporterCmdLineApp].getName, classpath, idlGeneratorArgs, streams.value.log)
      scopedRunner.run(classOf[RestLiSnapshotExporterCmdLineApp].getName, classpath, snapshotGeneratorArgs, streams.value.log)
    }
  }
}
