val pegasusVersion = "24.0.2"
val specs2Version = "3.9.4"
val log4jVersion = "2.8.1"

// Adds java tools.jar to the classpath. Needed for javadoc within the resource model exporter (RestliModelPlugin).
def toolsPluginDependency(sbtVersion: String, scalaVersion: String): ModuleID = {
  val toolsV = sbtVersion match {
    case "1.0" => "1.1.1"
    case "0.13" => "1.0.1"
  }

  Defaults.sbtPluginExtra("org.scala-debugger" % "sbt-jdi-tools" % toolsV, sbtVersion, scalaVersion)
}

// Starting with sbt 1.0, log4j is included with sbt
def log4jDependencies(sbtVersion: String): Seq[ModuleID] = {
  if (sbtVersion == "0.13") Seq(
    "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
  ) else Nil
}

lazy val sbtRestli = (project in file("sbt-restli"))
  .enablePlugins(SbtPlugin)
  .dependsOn(restliToolsScala)
  .settings(
    name := "sbt-restli",
    version := "0.3.0-SNAPSHOT",
    organization := "com.linkedin.pegasus",
    crossSbtVersions := Seq("1.2.6", "0.13.17"),
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "generator" % pegasusVersion,
      "com.linkedin.pegasus" % "restli-tools" % pegasusVersion,
      "com.linkedin.pegasus" % "data-avro-generator" % pegasusVersion
    ),
    libraryDependencies ++= {
      val sbtVersion = (sbtBinaryVersion in pluginCrossBuild).value
      val scalaVersion = (scalaBinaryVersion in update).value

      val tools = toolsPluginDependency(sbtVersion, scalaVersion)
      val log4j = log4jDependencies(sbtVersion)

      log4j :+ tools
    }
  )

lazy val restliToolsScala = (project in file("restli-tools-scala"))
  .settings(
    name := "restli-tools-scala",
    version := "0.3.0-SNAPSHOT",
    organization := "com.linkedin.pegasus",
    crossScalaVersions := Seq("2.10.7", "2.12.7"),
    // Do not remove this line or tests break. Sbt mangles the java.class.path system property unless forking is enabled :(
    fork in Test := true,
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-matcher-extra" % specs2Version % Test,
      "com.linkedin.pegasus" % "restli-int-test-api" % pegasusVersion % Test classifier "all",
      "com.linkedin.pegasus" % "restli-server" % pegasusVersion,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    )
  )
