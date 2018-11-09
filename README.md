# sbt-restli

A collection of sbt plugins providing build integration for the [rest.li](https://github.com/linkedin/rest.li) REST framework.

Setup
-----

Install the plugins to your sbt project:
```scala
// project/plugins.sbt
addSbtPlugin("com.linkedin.sbt-restli" % "sbt-restli" % "0.3.0")
```

Usage
-----

Sbt-restli is made up of 4 individual plugins.

### RestliSchemaPlugin

The rest.li schema plugin compiles pegasus data-schemas (`.pdsc` files) into java data-templates.

Apply the plugin to your project and place data-schemas in the `src/main/pegasus` directory.

```scala
// build.sbt
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

Set your API project using `restliModelApi`, and set your compatibility mode using `restliModelCompat` to one of `OFF`, `IGNORE`, `BACKWARDS` (default), or `EQUIVALENT`. 

Publishing your changes using `restliModelPublish` will copy rest models into your API project if they are compatible according to the compatibility mode selected.

```scala
// build.sbt
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

The rest.li client plugin generates java client bindings from rest models.

It is possible to apply the plugin to your API project directly, but it is best practice to create a new project in order to produce separate artifacts.
```scala
// build.sbt
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

```scala
// build.sbt
lazy val avro = (project in file("api"))
  .enablePlugins(RestliAvroPlugin)
  .settings(
    target := target.value / "avro" // Change target to avoid conflicts
  )
```

