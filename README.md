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

#### Tasks and Settings

|Name|Description|Default|
|----|-----------|-------|
|restliSchemaGenerate|Compiles Pegasus data-schemas into java source files (triggered on compile).|N/A|
|restliSchemaPackage|Packages Pegasus data-templates into *-data-template.jar|N/A|

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

#### Tasks and Settings

|Name|Description|Default|
|----|-----------|-------|
|restliModelApi|API project to publish idl and snapshot files to.|thisProjectRef|
|restliModelCompat|Rest model backwards compatibility level.|"BACKWARDS"|
|restliModelResourcePackages|List of packages containing Restli resources.|All packages|
|restliModelGenerate|Generates *.restspec.json & *.snapshot.json files from Restli resources.|N/A|
|restliModelPublish|Validates and publishes idl and snapshot files to the API project.|N/A|
|restliModelPackage|Package idl files into *-rest-model.jar|N/A|

### RestliClientPlugin

The rest.li client plugin generates Java client bindings from rest models.

Apply the plugin to your project in `build.sbt` and set your API project using `restliClientApi`.

```scala
lazy val client = (project in file("client"))
  .enablePlugins(RestliClientPlugin)
  .dependsOn(api)
  .settings(
    restliClientApi := api,
    libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "24.0.2"
  )
```

#### Tasks and Settings

|Name|Description|Default|
|----|-----------|-------|
|restliClientApi|API project containing resource idl files.|thisProjectRef|
|restliClientDefaultPackage|Default package for client bindings.|""|
|restliClientGenerate|Generates client bindings from API project (triggered on compile).|N/A|
|restliClientPackage|Packages restli client bindings into *-rest-client.jar|N/A|

### RestliAvroPlugin

The rest.li avro plugin generates avro data-schemas from pegasus data-schemas in `src/main/pegasus`.

Apply the plugin to your project in `build.sbt` and place data-schemas in the `src/main/pegasus` directory.

```scala
lazy val avro = (project in file("api"))
  .enablePlugins(RestliAvroPlugin)
```
 
The avro plugin may be used in conjunction with the schema plugin by applying them to the same project.

```scala
lazy val api = (project in file("api"))
  .enablePlugins(RestliSchemaPlugin, RestliAvroPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.linkedin.pegasus" % "data" % "24.0.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0"
    )
  )
```

#### Tasks and Settings

|Name|Description|Default|
|----|-----------|-------|
|restliAvroGenerate|Generates avro schemas from pegasus data-schemas (triggered on compile).|N/A|
|restliAvroPackage|Packages avro schemas into *-avro-schema.jar|N/A|
