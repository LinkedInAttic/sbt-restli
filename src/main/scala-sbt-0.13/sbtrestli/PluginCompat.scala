package sbtrestli

import sbt._
import Keys._

object PluginCompat {
  val watchSourcesSetting =
    watchSources in Defaults.ConfigGlobal ++= (sourceDirectory.value ** includeFilter.value).get
}