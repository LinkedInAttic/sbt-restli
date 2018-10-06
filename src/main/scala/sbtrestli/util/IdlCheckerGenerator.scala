package sbtrestli.util

import com.linkedin.pegasus.generator.GeneratorResult
import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider
import com.linkedin.restli.tools.idlcheck.{CompatibilityLevel, RestLiResourceModelCompatibilityChecker}
import com.linkedin.restli.tools.idlgen.RestLiResourceModelExporter
import com.linkedin.restli.tools.scala.ScalaDocsProvider
import sbt.{File, Logger}

import scala.collection.JavaConverters._

object IdlCheckerGenerator extends CheckerGenerator {
  override def name = "idl"
  override def fileGlob = "*.restspec.json"

  /**
    * Checks each pair of restspec.json files (current, previous) for compatibility.  Throws an exception containing
    * incompatibility details if incompatible changes, for the given mode, were found.
    *
    * @param filePairs (current, previous) file pairs
    * @param compatLevel provides the compatibility mode to use.  Must be one of: "equivalent", "backwards", "ignore", "off"
    * @param log provides a logger
    * @return A error message string if ANY compatibility differences were found (even if compatMode is "ignore"),
    *         always None if compatMode is "off"
    */
  override def checkCompatibility(filePairs: Seq[(java.io.File, java.io.File)], resolverPath: String,
    compatLevel: CompatibilityLevel, log: Logger): Option[String] = {
    val idlChecker = new RestLiResourceModelCompatibilityChecker()
    idlChecker.setResolverPath(resolverPath)

    filePairs.map { case (currentFile, previousFile) =>
      idlChecker.check(previousFile.getAbsolutePath, currentFile.getAbsolutePath, compatLevel)
    }

    if (idlChecker.getInfoMap.isEquivalent) {
      None
    } else {
      val allCheckMessage = idlChecker.getInfoMap.createSummary()
      val allCheckMessageWithDirections = allCheckMessage + directionsMessage(compatLevel)

      if (idlChecker.getInfoMap.isCompatible(compatLevel)) {
        Some(allCheckMessageWithDirections)
      } else {
        throw new Exception(allCheckMessageWithDirections)
      }
    }
  }

  override def generate(apiName: String, classpath: Seq[String], resourceSourcePaths: Seq[String],
    resourcePackages: Seq[String], generatedJsonDir: File, resolverPath: String): GeneratorResult = {
    val restliResourceModelExporter = new RestLiResourceModelExporter()
    restliResourceModelExporter.export(apiName, classpath.toArray, resourceSourcePaths.toArray,
      resourcePackages.toArray, null, generatedJsonDir.getAbsolutePath, List[DocsProvider](new ScalaDocsProvider(classpath.toArray)).asJava)
  }
}
