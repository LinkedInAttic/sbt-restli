name := "data-template-package"

organization := "com.linkedin.pegasus"

version := "0.1.0"

crossPaths := false

enablePlugins(RestliSchemaPlugin)

libraryDependencies ++= Seq(
  "com.linkedin.pegasus" % "data" % "24.0.2",
  "com.google.code.findbugs" % "jsr305" % "3.0.0"
)

// https://github.com/sbt/sbt/blob/18a0141fc03c96e4b1f7a67f0d5777e6b709817b/sbt/src/sbt-test/package/mappings/build.sbt#L13-L16
lazy val unzipPackage = taskKey[Unit]("extract jar file")
unzipPackage := {
  IO.unzip((restliSchemaPackage in Compile).value, target.value / "data-template")
}