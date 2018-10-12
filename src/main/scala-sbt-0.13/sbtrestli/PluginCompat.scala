package sbtrestli

import sbt._
import Keys._

object PluginCompat {
  def watchSourcesSetting(scope: Scoped) = {
    watchSources in Defaults.ConfigGlobal ++=
      ((sourceDirectory in scope).value ** (includeFilter in scope).value).get
  }

  def cleanFilesSetting(scope: Scoped) = {
    cleanFilesTask in Defaults.ConfigGlobal += (target in scope).value
  }
}