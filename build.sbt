val pegasusVersion = "24.0.2"

lazy val sbtRestli = (project in file("sbt-restli"))
  .enablePlugins(SbtPlugin)
  .dependsOn(restliToolsScala)
  .settings(
    name := "sbt-restli",
    version := "0.3.0-SNAPSHOT",
    organization := "com.linkedin.pegasus",
    crossSbtVersions := Seq("1.2.3", "0.13.17"),
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "generator" % pegasusVersion,
      "com.linkedin.pegasus" % "restli-tools" % pegasusVersion,
      "com.linkedin.pegasus" % "data-avro-generator" % pegasusVersion,
      "com.linkedin.pegasus" %% "restli-tools-scala" % "0.3.0-SNAPSHOT"
    ),
    libraryDependencies ++= {
      val sbtV = (sbtBinaryVersion in pluginCrossBuild).value
      val scalaV = (scalaBinaryVersion in update).value

      val toolsV = sbtV match {
        case "1.0" => "1.1.1"
        case "0.13" => "1.0.1"
      }

      val tools = Defaults.sbtPluginExtra("org.scala-debugger" % "sbt-jdi-tools" % toolsV, sbtV, scalaV)
      val log4j = if (sbtV == "0.13") Seq(
        "org.apache.logging.log4j" % "log4j-api" % "2.8.1",
        "org.apache.logging.log4j" % "log4j-core" % "2.8.1",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8.1"
      ) else Nil

      log4j :+ tools
    }
  )

lazy val restliToolsScala = (project in file("restli-tools-scala"))
  .settings(
    name := "restli-tools-scala",
    version := "0.3.0-SNAPSHOT",
    organization := "com.linkedin.pegasus",
    crossScalaVersions := Seq("2.10.7", "2.12.6"),
    // Do not remove this line or tests break. Sbt mangles the java.class.path system property unless forking is enabled :(
    fork in Test := true,
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "3.9.4" % Test,
      "org.specs2" %% "specs2-matcher-extra" % "3.9.4" % Test,
      "com.linkedin.pegasus" % "restli-int-test-api" % pegasusVersion % Test classifier "all",
      "com.linkedin.pegasus" % "restli-server" % pegasusVersion,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    )
  )
