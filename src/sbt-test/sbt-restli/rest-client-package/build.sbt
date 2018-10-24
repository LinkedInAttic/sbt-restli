lazy val api = (project in file("api"))
  .enablePlugins(RestliSchemaPlugin)
  .settings(
    target := target.value / "schema"
  )

lazy val client = (project in file("api"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(
    target := target.value / "client",
    unzipPackage := {
      IO.unzip((restliClientPackage in Compile).value, target.value / "rest-client")
    }
  )

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api
  )

lazy val unzipPackage = taskKey[Unit]("extract jar file")
