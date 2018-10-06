package sbtrestli.util

import com.linkedin.pegasus.generator.GeneratorResult
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._

trait CheckerGenerator {
  def name: String
  def fileGlob: String
  protected def checkCompatibility(filePairs: Seq[(java.io.File, java.io.File)], resolverPath: String, compatLevel: CompatibilityLevel, log: Logger): Option[String]
  protected def generate(apiName: String, classpath: Seq[String], resourceSourcePaths: Seq[String], resourcePackages: Seq[String], generatedJsonDir: File, resolverPath: String): GeneratorResult

  def runCompatibilityChecker(jsonFiles: Seq[java.io.File], outdir: java.io.File, compatMode: String, resolverPath: String, log: Logger) = {
    outdir.mkdirs()
    val filePairs = jsonFiles map { f =>
      (f, outdir / f.getName)
    }

    checkCompatibility(filePairs, resolverPath, CompatibilityLevel.valueOf(compatMode.toUpperCase), log) match {
      case Some(message) => {
        log.info(message)
        log.info(s"Publishing $name files to API project ...")
        filePairs foreach { case (src, dest) =>
          IO.copyFile(src, dest)
        }
      }
      case None => {
        log.info(s"$name files are equivalent. No need to publish.")
      }
    }
    outdir.get
  }

  def runGenerator(resourceProducts: Seq[File],
    resourcePackages: Seq[String],
    streams: std.TaskStreams[_],
    generatedJsonDir: File,
    resourceClasspath: Classpath,
    apiName: String,
    resourceSourcePaths: Seq[String],
    resolverPath: String) {

    generatedJsonDir.mkdirs()

    val previousJsonFiles = (generatedJsonDir ** fileGlob).get
    val cp = resourceClasspath map (_.data.absolutePath)
    val cl = ClasspathUtil.classLoaderFromClasspath(cp, this.getClass.getClassLoader)

    val generatedModel = try {
      ClasspathUtil.withContextClassLoader(cl) {
        generate(apiName, cp, resourceSourcePaths, resourcePackages, generatedJsonDir, resolverPath)
      }
    } catch {
      case e: Throwable =>
        streams.log.error("Running %s exporter for %s: %s".format(name, apiName, e.toString))
        streams.log.error("Resource project products: " + resourceProducts.mkString(", "))
        streams.log.error("Resource classpath: " + resourceClasspath.mkString(", "))
        streams.log.error("Exporter classpath: " + resourceClasspath.mkString(", "))
        streams.log.error("Source paths: " + resourceSourcePaths.mkString(", "))
        streams.log.error("Resource packages: " + resourcePackages.mkString(", "))
        streams.log.error("Generated JSON dir: " + generatedJsonDir)
        streams.log.error("JSON file glob expression: " + fileGlob)
        throw e
    }

    val generatedJsonFiles = generatedModel.getModifiedFiles.asScala.toSeq ++ generatedModel.getTargetFiles.asScala.toSeq

    val staleFiles = previousJsonFiles.sorted.diff(generatedJsonFiles.sorted)
    streams.log.debug("deleting stale files: " + staleFiles)
    IO.delete(staleFiles)
  }

  protected def directionsMessage(compatLevel: CompatibilityLevel): String = {
    s"This check was run on compatibility level ${compatLevel.toString.toLowerCase}.\n"
  } + {
    if(compatLevel == CompatibilityLevel.EQUIVALENT) {
      "You may set compatibility to 'backwards' to the build command to allow backwards compatible changes in interface.\n"
    } else ""
  } + {
    if(compatLevel == CompatibilityLevel.BACKWARDS || compatLevel == CompatibilityLevel.EQUIVALENT) {
      "You may set compatibility to 'ignore' to ignore compatibility errors.\n"
    } else ""
  } + {
    """In SBT, you can change the mode using the 'compatMode' param on the compileRestspec() method in Build.scala.
      |E.g. .compileRestspec(..., compatMode = "backwards")
      |Documentation: https://github.com/linkedin/rest.li/wiki/Resource-Compatibility-Checking
    """.stripMargin
  }
}
