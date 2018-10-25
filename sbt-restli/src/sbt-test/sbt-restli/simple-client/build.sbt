lazy val api = (project in file("api"))
    .enablePlugins(RestliClientPlugin, RestliSchemaPlugin)
    .settings(
      name := "simple-api",
      organization := "com.linkedin.pegasus",
      version := "0.1.0"
    )

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api,
    name := "simple-server",
    organization := "com.linkedin.pegasus",
    version := "0.1.0"
  )


