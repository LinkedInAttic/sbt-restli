enablePlugins(SbtPlugin)

name := "sbt-restli"

version := "0.3.0"

organization := "com.linkedin.pegasus"

crossSbtVersions := Seq("1.2.3")

val pegasusVersion = "23.0.+"

libraryDependencies ++= Seq(
  "com.linkedin.pegasus" % "restli-server" % pegasusVersion,
  "com.linkedin.pegasus" % "restli-int-test-server" % pegasusVersion % Test,
  "org.testng" % "testng" % "6.14.3" % Test,
)
