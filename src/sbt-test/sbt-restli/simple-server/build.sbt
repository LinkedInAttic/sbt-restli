lazy val api = (project in file("api"))
    .enablePlugins(DataTemplatePlugin)
    .settings(
      name := "simple-api",
      organization := "com.linkedin.pegasus",
      version := "0.1.0"
    )

lazy val server = (project in file("server"))
  .enablePlugins(RestModelPlugin)
  .dependsOn(api)
  .settings(
    restModelApi := api,
    name := "simple-server",
    organization := "com.linkedin.pegasus",
    version := "0.1.0"
  )


