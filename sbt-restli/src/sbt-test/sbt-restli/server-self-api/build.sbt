enablePlugins(RestliModelPlugin, RestliSchemaPlugin)

name := "server-self-api"

organization := "com.linkedin.pegasus"

version := "0.1.0"

libraryDependencies ++= Seq(
  "com.linkedin.pegasus" % "data" % "24.0.2",
  "com.google.code.findbugs" % "jsr305" % "3.0.0",
  "com.linkedin.pegasus" % "restli-server" % "24.0.2"
)
