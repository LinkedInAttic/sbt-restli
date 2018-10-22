name := "depends-on-simple"

organization := "com.linkedin.pegasus"

version := "0.1.0"

lazy val fortune = (project in file("fortune-api"))
  .enablePlugins(RestliSchemaPlugin)

dependsOn(fortune)
enablePlugins(RestliSchemaPlugin)
