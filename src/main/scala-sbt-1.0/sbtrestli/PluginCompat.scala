package sbtrestli

import sbt._
import Keys._

object PluginCompat {
  def watchSourcesSetting(scope: Scoped) = {
    watchSources in Defaults.ConfigGlobal +=
      WatchSource((sourceDirectory in scope).value, (includeFilter in scope).value, (excludeFilter in scope).value)
  }

  def cleanFilesSetting(scope: Scoped) = {
    cleanFiles in Defaults.ConfigGlobal += (target in scope).value
  }
}

