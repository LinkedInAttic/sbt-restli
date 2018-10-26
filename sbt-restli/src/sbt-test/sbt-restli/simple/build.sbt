name := "simple"

organization := "com.linkedin.pegasus"

version := "0.1.0"

enablePlugins(RestliSchemaPlugin)
libraryDependencies ++= Seq(
  "com.linkedin.pegasus" % "data" % "24.0.2",
  "com.google.code.findbugs" % "jsr305" % "3.0.0"
)
