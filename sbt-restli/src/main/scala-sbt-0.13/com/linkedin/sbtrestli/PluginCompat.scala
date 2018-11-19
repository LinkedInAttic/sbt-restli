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