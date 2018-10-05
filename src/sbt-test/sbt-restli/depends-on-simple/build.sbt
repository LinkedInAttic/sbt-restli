name := "depends-on-simple"

organization := "com.linkedin.pegasus"

version := "0.1.0"

lazy val fortune = (project in file("fortune-api"))
  .enablePlugins(DataTemplatePlugin)

dependsOn(fortune)
enablePlugins(DataTemplatePlugin)
