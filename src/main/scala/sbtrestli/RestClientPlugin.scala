package sbtrestli

import java.io.File

import com.linkedin.restli.internal.common.RestliVersion
import com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

import scala.collection.JavaConverters._

object RestClientPlugin extends AutoPlugin {
  object autoImport {
    val restClientDefaultPackage = settingKey[String]("Default package for client bindings.")
    val restClientGenerate = taskKey[Seq[File]]("Generates client bindings from API project.")

    val restClientDefaults: Seq[Def.Setting[_]] = Seq(
      restClientDefaultPackage := ""
    )

    val restClientSettings: Seq[Def.Setting[_]] = Seq(
      includeFilter in restClientGenerate := "*.restspec.json",
      excludeFilter in restClientGenerate := HiddenFileFilter,

      sourceDirectory in restClientGenerate := sourceDirectory.value / "idl",
      sourceDirectories in restClientGenerate := Seq((sourceDirectory in restClientGenerate).value),
      sourceDirectories ++= (sourceDirectories in restClientGenerate).value,

      sources in restClientGenerate := Defaults.collectFiles(
        sourceDirectories in restClientGenerate,
        includeFilter in restClientGenerate,
        excludeFilter in restClientGenerate).value,

      target in restClientGenerate := file(sourceDirectory.value + "GeneratedRest") / "java",

      PluginCompat.watchSourcesSetting(restClientGenerate),
      PluginCompat.cleanFilesSetting(restClientGenerate),

      restClientGenerate := generate.value,

      sourceGenerators += restClientGenerate.taskValue,
      managedSourceDirectories += (target in restClientGenerate).value,
      exportedProducts ++= (sourceDirectories in restClientGenerate).value
    )
  }

  import autoImport._

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = restClientDefaults ++
    inConfig(Compile)(restClientSettings) ++ inConfig(Test)(restClientSettings) ++ Seq(
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % BuildInfo.pegasusVersion
  )

  private lazy val generate = Def.task {
    val resolverFiles = sourceDirectories.value ++
      managedClasspath.value.files ++
      internalDependencyClasspath.value.files
    val resolverPath = resolverFiles.map(_.getAbsolutePath).mkString(File.pathSeparator)

    (target in restClientGenerate).value.mkdirs()
    val generatorResult = RestRequestBuilderGenerator.run(
      resolverPath,
      restClientDefaultPackage.value,
      baseDirectory.value.getAbsolutePath,
      false,
      false,
      RestliVersion.RESTLI_2_0_0,
      null,
      (target in restClientGenerate).value.getAbsolutePath,
      (sources in restClientGenerate).value.map(_.getAbsolutePath).toArray
    )

    generatorResult.getTargetFiles.asScala.toSeq
  }

}
