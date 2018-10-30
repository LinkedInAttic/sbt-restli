name := "depends-on-simple"

organization := "com.linkedin.sbt-restli"

version := "0.1.0"

lazy val fortune = (project in file("fortune-api"))
  .enablePlugins(RestliSchemaPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )

dependsOn(fortune)
enablePlugins(RestliSchemaPlugin)
libraryDependencies ++= Seq(
  "com.linkedin.pegasus" % "data" % "24.0.2",
  "com.google.code.findbugs" % "jsr305" % "3.0.0"
)

