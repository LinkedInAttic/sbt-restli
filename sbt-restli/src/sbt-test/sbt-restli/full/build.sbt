val javaProjectSettings = Seq(
  crossPaths := false,
  autoScalaLibrary := false,
  managedScalaInstance := false
)

lazy val fortuneApi = (project in file("fortune-api"))
  .enablePlugins(RestliSchemaPlugin)
  .settings(javaProjectSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )

lazy val api = (project in file("api"))
  .enablePlugins(RestliSchemaPlugin)
  .dependsOn(fortuneApi)
  .settings(javaProjectSettings)
  .settings(
    target := target.value / "schema",
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )

lazy val clientBindings = (project in file("api"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(javaProjectSettings)
  .settings(
    target := target.value / "client",
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "24.0.2"
  )

lazy val client = (project in file("client"))
  .dependsOn(clientBindings)

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api,
    libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "24.0.2"
  )
