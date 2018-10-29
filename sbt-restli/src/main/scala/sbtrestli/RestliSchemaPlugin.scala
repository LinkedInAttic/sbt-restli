package sbtrestli

import java.io.{File, OutputStream, PrintStream}

import com.linkedin.pegasus.generator.PegasusDataTemplateGenerator
import org.apache.logging.log4j.{Level => XLevel}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

object RestliSchemaPlugin extends AutoPlugin {
  object autoImport {
    val restliSchemaGenerate = taskKey[Seq[File]]("Compiles Pegasus data-schemas into java source files.")
    val restliSchemaPackage = taskKey[File]("Packages Pegasus data-templates into *-data-template.jar")

    val restliSchemaSettings: Seq[Def.Setting[_]] = Seq(
      includeFilter in restliSchemaGenerate := "*.pdsc",
      excludeFilter in restliSchemaGenerate := HiddenFileFilter,

      sourceDirectory in restliSchemaGenerate := sourceDirectory.value / "pegasus",
      sourceDirectories in restliSchemaGenerate := Seq((sourceDirectory in restliSchemaGenerate).value),
      sourceDirectories ++= (sourceDirectories in restliSchemaGenerate).value,

      sources in restliSchemaGenerate := Defaults.collectFiles(
        sourceDirectories in restliSchemaGenerate,
        includeFilter in restliSchemaGenerate,
        excludeFilter in restliSchemaGenerate).value,

      target in restliSchemaGenerate := file(sourceDirectory.value + "GeneratedDataTemplate") / "java",

      PluginCompat.watchSourcesSetting(restliSchemaGenerate),
      PluginCompat.cleanFilesSetting(restliSchemaGenerate),

      restliSchemaGenerate := generate.value,

      sourceGenerators += restliSchemaGenerate.taskValue,
      managedSourceDirectories += (target in restliSchemaGenerate).value,
      exportedProducts ++= (sourceDirectories in restliSchemaGenerate).value,

      artifactClassifier in restliSchemaPackage := Some("data-template"),
      publishArtifact in restliSchemaPackage := true,

      packagedArtifacts in Defaults.ConfigGlobal ++= Classpaths.packaged(Seq(restliSchemaPackage)).value,
      artifacts in Defaults.ConfigGlobal ++= Classpaths.artifactDefs(Seq(restliSchemaPackage)).value
    ) ++ Defaults.packageTaskSettings(restliSchemaPackage, Def.task {
        val sourceDir = (sourceDirectory in restliSchemaGenerate).value
        val originalSources = (sources in restliSchemaGenerate).value
        val rebasedSources = originalSources pair Path.rebase(sourceDir, "pegasus/")

        rebasedSources ++ (mappings in packageBin).value
      })
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(restliSchemaSettings) ++ inConfig(Test)(restliSchemaSettings)

  private object DataTemplateCompileException extends Exception with FeedbackProvidedException

  private object NullOutputStream extends OutputStream {
    override def write(i: Int): Unit = ()
  }

  private lazy val generate = Def.task {
    val resolverFiles = (sourceDirectories in restliSchemaGenerate).value ++
      managedClasspath.value.files ++
      internalDependencyClasspath.value.files // adds in .pdscs from projects that this project .dependsOn
    val resolverPath = resolverFiles.map(_.getAbsolutePath).mkString(File.pathSeparator)
    val pegasusSources = (sources in restliSchemaGenerate).value.map(_.getAbsolutePath).toArray
    val targetDir = (target in restliSchemaGenerate).value.getAbsolutePath
    val log = streams.value.log

    // PegasusDataTemplateGenerator writes some errors to stderr before throwing them as exceptions.
    // Disable stderr temporarily so errors aren't written twice.
    val stdErr = System.err
    System.setErr(new PrintStream(NullOutputStream))

    PluginCompat.setLogLevel(classOf[PegasusDataTemplateGenerator].getName, XLevel.WARN)

    val generatorResult = try {
      PegasusDataTemplateGenerator.run(
        resolverPath,
        null,
        baseDirectory.value.getAbsolutePath,
        false, // Class files included in "data-template" dependencies, no need to generate (I think)
        targetDir,
        pegasusSources
      )
    } catch {
      case e: Throwable =>
        log.error(e.getMessage)
        throw DataTemplateCompileException
    } finally {
      // Reset error stream
      System.setErr(stdErr)
    }

    // PegasusDataTemplateGenerator does internal caching. Only print message if files were generated
    val count = generatorResult.getModifiedFiles.size
    if (count > 0) {
      val plural = if (count == 1) "" else "s"
      log.info(s"Generated $count Pegasus data-template$plural in $targetDir")
    }

    generatorResult.getTargetFiles.asScala.toSeq
  }
}
