package sbtrestli

import java.io.{File, OutputStream, PrintStream}

import com.linkedin.pegasus.generator.PegasusDataTemplateGenerator
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

object DataTemplatePlugin extends AutoPlugin {
  object autoImport {
    val dataTemplateGenerate = taskKey[Seq[File]]("Compiles Pegasus data-schemas into java source files.")
    val dataTemplatePackage = taskKey[File]("Packages Pegasus data-templates into *-data-template.jar")

    val dataTemplateSettings: Seq[Def.Setting[_]] = Seq(
      includeFilter in dataTemplateGenerate := "*.pdsc",
      excludeFilter in dataTemplateGenerate := HiddenFileFilter,

      sourceDirectory in dataTemplateGenerate := sourceDirectory.value / "pegasus",
      sourceDirectories in dataTemplateGenerate := Seq((sourceDirectory in dataTemplateGenerate).value),
      sourceDirectories ++= (sourceDirectories in dataTemplateGenerate).value,

      sources in dataTemplateGenerate := Defaults.collectFiles(
        sourceDirectories in dataTemplateGenerate,
        includeFilter in dataTemplateGenerate,
        excludeFilter in dataTemplateGenerate).value,

      target in dataTemplateGenerate := file(sourceDirectory.value + "GeneratedDataTemplate") / "java",

      PluginCompat.watchSourcesSetting(dataTemplateGenerate),
      PluginCompat.cleanFilesSetting(dataTemplateGenerate),

      dataTemplateGenerate := generate.value,

      sourceGenerators += dataTemplateGenerate.taskValue,
      managedSourceDirectories += (target in dataTemplateGenerate).value,
      exportedProducts ++= (sourceDirectories in dataTemplateGenerate).value,

      artifactClassifier in dataTemplatePackage := Some("data-template"),
      publishArtifact in dataTemplatePackage := true,

      packagedArtifacts in Defaults.ConfigGlobal ++= Classpaths.packaged(Seq(dataTemplatePackage)).value,
      artifacts in Defaults.ConfigGlobal ++= Classpaths.artifactDefs(Seq(dataTemplatePackage)).value
    ) ++ Defaults.packageTaskSettings(dataTemplatePackage, Def.task {
        val sourceDir = (sourceDirectory in dataTemplateGenerate).value
        val originalSources = (sources in dataTemplateGenerate).value
        val rebasedSources = originalSources pair Path.rebase(sourceDir, "pegasus/")

        rebasedSources ++ (mappings in packageBin).value
      })
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(dataTemplateSettings) ++ inConfig(Test)(dataTemplateSettings) ++ Seq(
      // For @Nonnull annotation in generated sources
      libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.+",
      libraryDependencies += "com.linkedin.pegasus" % "data" % BuildInfo.pegasusVersion
    )

  private object DataTemplateCompileException extends Exception with FeedbackProvidedException

  private object NullOutputStream extends OutputStream {
    override def write(i: Int): Unit = ()
  }

  private lazy val generate = Def.task {
    val resolverFiles = (sourceDirectories in dataTemplateGenerate).value ++
      managedClasspath.value.files ++
      internalDependencyClasspath.value.files // adds in .pdscs from projects that this project .dependsOn
    val resolverPath = resolverFiles.map(_.getAbsolutePath).mkString(File.pathSeparator)
    val pegasusSources = (sources in dataTemplateGenerate).value.map(_.getAbsolutePath).toArray
    val targetDir = (target in dataTemplateGenerate).value.getAbsolutePath
    val log = streams.value.log

    val count = pegasusSources.length
    val plural = if (count == 1) "" else "s"
    log.info(s"Compiling $count Pegasus data-template$plural to $targetDir ...")

    // Silence error messages
    val stdErr = System.err
    System.setErr(new PrintStream(NullOutputStream))

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

    generatorResult.getTargetFiles.asScala.toSeq
  }
}