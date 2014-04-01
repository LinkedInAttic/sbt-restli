/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.sbt.restli


import sbt._
import sbt.Keys._
import com.linkedin.data.avro.generator.AvroSchemaGenerator
import java.io.File.pathSeparator
import scala.util.matching.Regex
import xsbti.Severity


/**
 * This class allows users to generate the Avro schema files (.avsc) from the Pegasus files (.pdsc) files
 * and packages them and also includes it in the artifact_spec.json
 * @author Keith Dsouza <kdsouza@linkedin.com>
 */

class AvroProject(val project : Project) extends Avro {

  def generateAvroSchema() = {
     project
      .settings(avroArtifacts : _*)
      .settings(
        compile in Compile <<= (compile in Compile) dependsOn avroSchemaGenerator,

        pegasusPdscDir := (sourceDirectory in Compile).value / "pegasus",
        pegasusResolverPath := {
          val resolverPathFiles = Seq(pegasusPdscDir.value.getAbsolutePath) ++
                  (managedClasspath in Compile).value.map(_.data.getAbsolutePath) ++
                  (internalDependencyClasspath in Compile).value.map(_.data.getAbsolutePath) // adds in .pdscs from projects that this project .dependsOn
          resolverPathFiles.mkString(pathSeparator)
        },
        pegasusAvroDir := (baseDirectory in Compile).value / "mainGeneratedAvroSchema",

        pegasusAvroCacheSources := streams.value.cacheDirectory / "pdsc.avro.sources"
    )
  }
}

trait Avro extends Restli
{
  val pegasusPdscDir = settingKey[File]("the directory where pegasus files are located")
  val pegasusAvroDir = settingKey[File]("generated avro files based on pegasus files are put here")

  val pegasusResolverPath = taskKey[String]("Sets the System property for Pegasus/Avro projects: generator.resolver.path before running the generator")

  val pegasusAvroCacheSources = taskKey[File]("The cache sources for the pdsc files")
  val packageAvroSchema = taskKey[File]("Produces a avro jar containing only avsc files")

  def avroArtifacts = {
    def packageAvroSchemaMappings = pegasusAvroDir.map{ (dir) =>
      mappings(dir, AvroFileGlobExpr)
    }

    val defaultConfig = config("default").extend(Runtime).describedAs("Configuration for default artifacts.")

    val avroConfig = new Configuration("avroSchema", "avro schema files",
                                             isPublic=true,
                                             extendsConfigs=List(Compile),
                                             transitive=true)

    Defaults.packageTaskSettings(packageAvroSchema, packageAvroSchemaMappings) ++
    restliArtifactSettings(packageAvroSchema)("avroSchema") ++
    Seq(
      packagedArtifacts <++= Classpaths.packaged(Seq(packageAvroSchema)),
      artifacts <++= Classpaths.artifactDefs(Seq(packageAvroSchema)),
      ivyConfigurations ++= List(avroConfig, defaultConfig),
      artifact in (Compile, packageBin) ~= { (art: Artifact) =>
        art.copy(configurations = art.configurations ++ List(avroConfig))
      }
    )
  }

  //transforms a Project to a AvroProject if needed, i.e. when you call a method that exists only on AvroProject
  implicit def projectToAvroProject(project : Project) = new AvroProject(project)


  val avroSchemaGenerator = Def.task {
    val s = streams.value
    val cacheFileSources = pegasusAvroCacheSources.value
    val sourceDir = pegasusPdscDir.value
    val resolverPath = pegasusResolverPath.value
    val avroDir = pegasusAvroDir.value

    if (!sourceDir.exists()) {
     throw new MessageOnlyException("Pegasus source directory does not exist: " + sourceDir)
    }

    val sourceFiles = (sourceDir ** PdscFileGlobExpr).get
    s.log.debug("source files: (" + sourceFiles.size + ")" + sourceFiles.toList)

    val (anyFilesChanged, cacheSourceFiles) = {
     prepareCacheUpdate(cacheFileSources, sourceFiles, s)
    }
    s.log.debug("detected changed files: " + anyFilesChanged)

    if (anyFilesChanged) {
     avroDir.mkdirs()
     try {
       val pdscFiles = sourceFiles.map(_.getAbsolutePath())
       s.log.debug("found pdsc files: " + pdscFiles.toString)
       AvroSchemaGenerator.run(resolverPath, null, avroDir.getAbsolutePath, pdscFiles.toArray)
     } catch {
       case e: java.io.IOException => {
         e.getMessage match {
           case JsonParseExceptionRegExp(source, line, column) =>
             throw new RestliCompilationException(
               Some(file(source)),
               "JSON parse error in " + source + ": line: "  +  line.toInt + ", column:  " + column.toInt,
               Option(line.toInt), Option(column.toInt),
               Severity.Error)
           case _ =>
             throw new MessageOnlyException("Restli generator error" + "Error message: " + e.getMessage)
         }
       }
       case e: Throwable => {
         throw e
       }
     }
     cacheSourceFiles()
    }

  }

}
