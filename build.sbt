val pegasusVersion = "24.0.2"

val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .dependsOn(restliToolsScala)
  .settings(
    name := "sbt-restli",
    version := "0.3.0",
    organization := "com.linkedin.pegasus",
    crossSbtVersions := Seq("1.2.3", "0.13.17"),
    libraryDependencies ++= Seq(
    )
  )

lazy val restliToolsScala = (project in file("restli-tools-scala"))
  .settings(
    name := "restli-tools-scala",
    version := "24.0.2",
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