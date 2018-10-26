lazy val api = (project in file("api"))
    .enablePlugins(RestliSchemaPlugin)
    .settings(
      name := "simple-api",
      organization := "com.linkedin.pegasus",
      version := "0.1.0",
      libraryDependencies ++= Seq(
        "com.linkedin.pegasus" % "data" % "24.0.2",
        "com.google.code.findbugs" % "jsr305" % "3.0.0"
      )
    )

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api,
    name := "simple-server",
    organization := "com.linkedin.pegasus",
    version := "0.1.0",
    libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "24.0.2"
  )


