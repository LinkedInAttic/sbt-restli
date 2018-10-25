val javaProjectSettings = Seq(
  crossPaths := false,
  autoScalaLibrary := false,
  managedScalaInstance := false
)

lazy val fortuneApi = (project in file("fortune-api"))
  .enablePlugins(RestliSchemaPlugin)
  .settings(javaProjectSettings)

lazy val api = (project in file("api"))
  .enablePlugins(RestliSchemaPlugin)
  .dependsOn(fortuneApi)
  .settings(javaProjectSettings)
  .settings(
    target := target.value / "schema"
  )

lazy val clientBindings = (project in file("api"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(javaProjectSettings)
  .settings(
    target := target.value / "client"
  )

lazy val client = (project in file("client"))
  .dependsOn(clientBindings)

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api
  )
