name := "avro-schema-package"

organization := "com.linkedin.sbt-restli"

version := "0.1.0"

crossPaths := false

enablePlugins(RestliAvroPlugin)

// https://github.com/sbt/sbt/blob/18a0141fc03c96e4b1f7a67f0d5777e6b709817b/sbt/src/sbt-test/package/mappings/build.sbt#L13-L16
lazy val unzipPackage = taskKey[Unit]("extract jar file")
unzipPackage := {
  IO.unzip((restliAvroPackage in Compile).value, target.value / "avro-schema")
}