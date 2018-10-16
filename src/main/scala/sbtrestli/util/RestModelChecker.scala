package sbtrestli.util

import com.linkedin.restli.tools.compatibility.CompatibilityInfoMap
import com.linkedin.restli.tools.idlcheck.CompatibilityLevel

trait RestModelChecker {
  def setResolverPath(resolverPath: String): Unit
  def check(prevPath: String, curPath: String, compatLevel: CompatibilityLevel): Any
  def getInfoMap: CompatibilityInfoMap
}
