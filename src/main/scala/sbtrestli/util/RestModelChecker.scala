package sbtrestli.util

import com.linkedin.restli.tools.idlcheck.CompatibilityLevel
import sbt._

trait RestModelChecker {
  def name: String
  def fileGlob: String
  protected def checkCompatibility(filePairs: Seq[(java.io.File, java.io.File)], resolverPath: String, compatLevel: CompatibilityLevel, log: Logger): Option[String]

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
