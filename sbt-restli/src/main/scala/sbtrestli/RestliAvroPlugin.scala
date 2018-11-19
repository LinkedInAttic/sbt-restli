package sbtrestli

import java.io.File

import com.linkedin.data.avro.generator.AvroSchemaGenerator
import org.apache.logging.log4j.{Level => XLevel}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object RestliAvroPlugin extends AutoPlugin {
  object autoImport {
    val restliAvroGenerate = taskKey[Seq[File]]("Generates avro schemas from pegasus data-schemas.")
    val restliAvroPackage = taskKey[File]("Packages avro schemas into *-avro-schema.jar")

    val restliAvroSettings: Seq[Def.Setting[_]] = Seq(
      includeFilter in restliAvroGenerate := "*.pdsc",
      excludeFilter in restliAvroGenerate := HiddenFileFilter,

      sourceDirectory in restliAvroGenerate := sourceDirectory.value / "pegasus",
      sourceDirectories in restliAvroGenerate := Seq((sourceDirectory in restliAvroGenerate).value),

      sources in restliAvroGenerate := Defaults.collectFiles(
        sourceDirectories in restliAvroGenerate,
        includeFilter in restliAvroGenerate,
        excludeFilter in restliAvroGenerate).value,

      target in restliAvroGenerate := file(sourceDirectory.value + "GeneratedAvroSchema"),

      PluginCompat.watchSourcesSetting(restliAvroGenerate),
      PluginCompat.cleanFilesSetting(restliAvroGenerate),

      restliAvroGenerate := generate.value,

      resourceGenerators += restliAvroGenerate.taskValue,
      managedResourceDirectories += (target in restliAvroGenerate).value,
      compile := compile.dependsOn(restliAvroGenerate).value,

      artifactClassifier in restliAvroPackage := Some("avro-schema"),
      publishArtifact in restliAvroPackage := true,

      packagedArtifacts in Defaults.ConfigGlobal ++= Classpaths.packaged(Seq(restliAvroPackage)).value,
      artifacts in Defaults.ConfigGlobal ++= Classpaths.artifactDefs(Seq(restliAvroPackage)).value
    ) ++ Defaults.packageTaskSettings(restliAvroPackage, Defaults.relativeMappings(restliAvroGenerate, managedResourceDirectories))
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(restliAvroSettings) ++ inConfig(Test)(restliAvroSettings)

  private lazy val generate = Def.task {
    val resolverFiles = (sourceDirectories in restliAvroGenerate).value ++
      managedClasspath.value.files ++
      internalDependencyClasspath.value.files
    val resolverPath = resolverFiles.map(_.getAbsolutePath).mkString(File.pathSeparator)
    val targetDir = (target in restliAvroGenerate).value
    val sourceFiles = (sources in restliAvroGenerate).value.map(_.getAbsolutePath).toArray

    PluginCompat.setLogLevel(classOf[AvroSchemaGenerator].getName, XLevel.INFO)

    AvroSchemaGenerator.run(resolverPath, null, false, targetDir.getAbsolutePath, sourceFiles)

    (targetDir ** "*.avsc").get
  }
}
