package sbtrestli.util

import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel
import com.linkedin.restli.tools.snapshot.check.RestLiSnapshotCompatibilityChecker
import sbt.Logger

object SnapshotChecker extends RestModelChecker {
  override def name = "snapshot"
  override def fileGlob = "*.snapshot.json"

  /**
   * Checks each pair of snapshot.json files (current, previous) for compatibility.  Throws an exception containing
   * incompatibility details if incompatible changes, for the given mode, were found.
   *
   * Snapshot files contain idl and pdsc information and are ideal for perform exhaustive compatibility checking
   *
   * @param filePairs (current, previous) file pairs
   * @param compatLevel provides the compatibility mode to use.  Must be one of: "equivalent", "backwards", "ignore", "off"
   * @param log provides a logger
   * @return A error message string if ANY compatibility differences were found (even if compatMode is "ignore"),
   *         always None if compatMode is "off"
   */
  override def checkCompatibility(filePairs: Seq[(java.io.File, java.io.File)], resolverPath: String,
                                  compatLevel: CompatibilityLevel, log: Logger): Option[String] = {
    val snapshotChecker = new RestLiSnapshotCompatibilityChecker()
    snapshotChecker.setResolverPath(resolverPath)

    val compatibilityMap = new CompatibilityInfoMap

    // check compatibility of each set of files
    filePairs.foreach { case (currentFile, previousFile) =>
      val infoMap = snapshotChecker.check(previousFile.getAbsolutePath, currentFile.getAbsolutePath, compatLevel)
      compatibilityMap.addAll(infoMap)
    }

    if (compatibilityMap.isEquivalent) {
      None
    } else {
      val allCheckMessage = compatibilityMap.createSummary()
      val allCheckMessageWithDirections = allCheckMessage + directionsMessage(compatLevel)

      if (compatibilityMap.isCompatible(compatLevel)) {
        Some(allCheckMessageWithDirections)
      } else {
        throw new Exception(allCheckMessageWithDirections)
      }
    }
  }
}
