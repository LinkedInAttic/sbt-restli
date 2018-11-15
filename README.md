# sbt-restli

A collection of sbt plugins providing build integration for the [rest.li](https://github.com/linkedin/rest.li) REST framework.

> **NOTE**: sbt-restli is built and tested against pegasus v24.0.2. While it should be compatible with other versions, no guarantees can be made.

Setup
-----

Install the plugins to your sbt project in `project/plugins.sbt`:
```scala
addSbtPlugin("com.linkedin.sbt-restli" % "sbt-restli" % "TODO")
```

Usage
-----

Sbt-restli is made up of 4 individual plugins.

### RestliSchemaPlugin

The rest.li schema plugin compiles pegasus data-schemas (`*.pdsc` files) into Java data-template classes.

Apply the plugin to your project in `build.sbt` and place data-schemas in the `src/main/pegasus` directory.

```scala
lazy val api = (project in file("api"))
  .enablePlugins(RestliSchemaPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )
```

### RestliModelPlugin

The rest.li model plugin generates rest models (restspec & snapshot files) from your rest.li resource annotations, checks if they are compatible, and publishes them to your API project.

Apply the plugin to your project in `build.sbt` and set your API project using `restliModelApi`, then set your compatibility mode using `restliModelCompat` to one of `OFF`, `IGNORE`, `BACKWARDS` (default), or `EQUIVALENT`. 

Publishing your changes using the `restliModelPublish` task will copy rest models into your API project if they are compatible according to the compatibility mode selected.

```scala
lazy val server = (project in file("server"))
  .enablePlugins(RestliModelPlugin)
  .dependsOn(api)
  .settings(
    restliModelApi := api,
    restliModelCompat := "BACKWARDS",
    libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "24.0.2"
  )
```

### RestliClientPlugin

The rest.li client plugin generates Java client bindings from rest models.

Apply the plugin to your project in `build.sbt`. It is possible to apply the plugin to your API project directly, but it is best practice to create a new project in order to produce separate artifacts.

```scala
lazy val clientBindings = (project in file("api"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(
    target := target.value / "client", // Change target to avoid conflicts
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "24.0.2"
  )
```

### RestliAvroPlugin

The rest.li avro plugin generates avro data-schemas from pegasus data-schemas in `src/main/pegasus`.

Apply the plugin to your project in `build.sbt` and place data-schemas in the `src/main/pegasus` directory.

```scala
lazy val avro = (project in file("api"))
  .enablePlugins(RestliAvroPlugin)
  .settings(
    target := target.value / "avro" // Change target to avoid conflicts
  )
```
