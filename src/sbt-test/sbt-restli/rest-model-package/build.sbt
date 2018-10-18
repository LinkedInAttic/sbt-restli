lazy val api = (project in file("api"))
    .enablePlugins(DataTemplatePlugin)
    .settings(
      name := "rest-model-package-api",
      organization := "com.linkedin.pegasus",
      version := "0.1.0"
    )

lazy val server = (project in file("server"))
  .enablePlugins(RestModelPlugin)
  .dependsOn(api)
  .settings(
    restModelApi := api,
    name := "rest-model-package-server",
    organization := "com.linkedin.pegasus",
    version := "0.1.0",
    unzipPackage := {
      IO.unzip((restModelPackage in Compile).value, target.value / "rest-model")
    }
  )

lazy val unzipPackage = taskKey[Unit]("extract jar file")
