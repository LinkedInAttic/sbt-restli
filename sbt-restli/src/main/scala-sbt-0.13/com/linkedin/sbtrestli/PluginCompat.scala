/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.sbtrestli

import sbt._
import Keys._
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.{ConsoleAppender => XConsoleAppender}
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.config.{AbstractConfiguration, LoggerConfig}
import org.apache.logging.log4j.{LogManager, Level => XLevel}

object PluginCompat {
  type PluginClassLoader = sbt.PluginManagement.PluginClassLoader

  def watchSourcesSetting(scope: Scoped) = {
    watchSources in Defaults.ConfigGlobal ++=
      ((sourceDirectory in scope).value ** (includeFilter in scope).value).get
  }

  def cleanFilesSetting(scope: Scoped) = {
    cleanFilesTask in Defaults.ConfigGlobal += (target in scope).value
  }

  def setLogLevel(logger: String, level: XLevel): Unit = {
    val context = LogManager.getContext(false) match { case x: LoggerContext => x }
    val config = context.getConfiguration match { case x: AbstractConfiguration => x }

    if (config.getLogger(logger) != null) {
      val loggerConfig = config.getLoggerConfig(logger)
      loggerConfig.setLevel(level)
    } else {
      val loggerConfig = new LoggerConfig(logger, level, true)
      config.addLogger(logger, loggerConfig)
    }
    context.updateLoggers(config)
  }
}