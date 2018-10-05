package sbtrestli

import sbt._
import Keys._

object PluginCompat {
  val watchSourcesSetting = watchSources in Defaults.ConfigGlobal +=
    WatchSource(sourceDirectory.value, includeFilter.value, excludeFilter.value)
}

