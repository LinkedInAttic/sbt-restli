package sbtrestli

import java.io.File

import com.linkedin.restli.internal.common.RestliVersion
import com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

import scala.collection.JavaConverters._

object RestliClientPlugin extends AutoPlugin {
  object autoImport {
    val restliClientDefaultPackage = settingKey[String]("Default package for client bindings.")
    val restliClientGenerate = taskKey[Seq[File]]("Generates client bindings from API project.")

    val restliClientDefaults: Seq[Def.Setting[_]] = Seq(
      restliClientDefaultPackage := ""
    )

    val restliClientSettings: Seq[Def.Setting[_]] = Seq(
      includeFilter in restliClientGenerate := "*.restspec.json",
      excludeFilter in restliClientGenerate := HiddenFileFilter,

      sourceDirectory in restliClientGenerate := sourceDirectory.value / "idl",
      sourceDirectories in restliClientGenerate := Seq((sourceDirectory in restliClientGenerate).value),
      sourceDirectories ++= (sourceDirectories in restliClientGenerate).value,

      sources in restliClientGenerate := Defaults.collectFiles(
        sourceDirectories in restliClientGenerate,
        includeFilter in restliClientGenerate,
        excludeFilter in restliClientGenerate).value,

      target in restliClientGenerate := file(sourceDirectory.value + "GeneratedRest") / "java",

      PluginCompat.watchSourcesSetting(restliClientGenerate),
      PluginCompat.cleanFilesSetting(restliClientGenerate),

      restliClientGenerate := generate.value,

      sourceGenerators += restliClientGenerate.taskValue,
      managedSourceDirectories += (target in restliClientGenerate).value,
      exportedProducts ++= (sourceDirectories in restliClientGenerate).value
    )
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = restliClientDefaults ++
    inConfig(Compile)(restliClientSettings) ++ inConfig(Test)(restliClientSettings) ++ Seq(
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % BuildInfo.pegasusVersion
  )

  private lazy val generate = Def.task {
    val resolverFiles = sourceDirectories.value ++
      managedClasspath.value.files ++
      internalDependencyClasspath.value.files
    val resolverPath = resolverFiles.map(_.getAbsolutePath).mkString(File.pathSeparator)

    (target in restliClientGenerate).value.mkdirs()
    val generatorResult = RestRequestBuilderGenerator.run(
      resolverPath,
      restliClientDefaultPackage.value,
      baseDirectory.value.getAbsolutePath,
      false,
      false,
      RestliVersion.RESTLI_2_0_0,
      null,
      (target in restliClientGenerate).value.getAbsolutePath,
      (sources in restliClientGenerate).value.map(_.getAbsolutePath).toArray
    )

    generatorResult.getTargetFiles.asScala.toSeq
  }

}
