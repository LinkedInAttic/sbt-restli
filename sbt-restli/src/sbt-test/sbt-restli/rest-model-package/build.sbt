lazy val api = (project in file("api"))
    .enablePlugins(RestliSchemaPlugin)
    .settings(
      name := "rest-model-package-api",
      organization := "com.linkedin.pegasus",
      version := "0.1.0"
    )

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api,
    name := "rest-model-package-server",
    organization := "com.linkedin.pegasus",
    version := "0.1.0",
    unzipPackage := {
      IO.unzip((restliModelPackage in Compile).value, target.value / "rest-model")
    }
  )

lazy val unzipPackage = taskKey[Unit]("extract jar file")
