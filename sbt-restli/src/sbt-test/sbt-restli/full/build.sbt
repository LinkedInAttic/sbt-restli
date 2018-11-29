val javaProjectSettings = Seq(
  crossPaths := false,
  autoScalaLibrary := false,
  managedScalaInstance := false
)

def unzip(jar: File, dest: File): Unit = {
  IO.unzip(jar, dest / "jar")
}

lazy val unzipPackage = taskKey[Unit]("extract jar files")

lazy val fortuneApi = (project in file("fortune-api"))
  .enablePlugins(RestliSchemaPlugin, RestliAvroPlugin)
  .settings(javaProjectSettings)
  .settings(
    name := "fortune-api",
    version := "0.1.0",
    unzipPackage := {
      unzip((restliSchemaPackage in Compile).value, target.value / "schema")
      unzip((restliAvroPackage in Compile).value, target.value / "avro")
    },
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
    name := "api",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )

lazy val client = (project in file("client"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(javaProjectSettings)
  .settings(
    name := "client",
    version := "0.1.0",
    restliClientApi := api,
    unzipPackage := unzip((restliClientPackage in Compile).value, target.value),
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "24.0.2"
  )

lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(javaProjectSettings)
  .settings(
    name := "server",
    version := "0.1.0",
    restliModelApi := api,
    unzipPackage := unzip((restliModelPackage in Compile).value, target.value),
    libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "24.0.2"
  )
