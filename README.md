rest.li-sbt-plugin
==================

SBT plugin for [rest.li](https://github.com/linkedin/rest.li).  This plugin provides full SBT integration for building
rest.li applications.  This includes `.pdsc` to data template code generation, rest client builder generation
and interface definitions (idl) publishing and validation.

All features support SBT hot reload.  To use with Play, simply define server projects as a `play.Project` (rather than
just a `Project`) and put all source in the server project under an `app` directory instead of `src/main/scala`.

Requirements
------------

* Java 1.6+
* Gradle 1.8+ (optional)

Building from Source
--------------------

To build from source and install as a snapshot in your local maven repo, clone this repo and build it:

```sh
./gradlew install
```

Maven artifacts will be written into `~/.m2/repository`.

Once built, you'll need to remember to add resolvers to these repositories when you use the plugin in your sbt projects
When following the below "usage" directions, remember to put this in your project/Build.scala:

```scala
val baseSettings = Seq(
  resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
```

and this in your `project/plugins.scala`:

```scala
unmanagedJars in Compile ~= {uj =>
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"
```

Getting Started
---------------

### Adding the plugin dependency

In your project, create a file for plugin library dependencies `project/plugins.sbt` and add the following lines:

    // add any resolvers required for the plugin library dependency here (see above if building from source)

    libraryDependencies += "com.linkedin.pegasus" %% "sbt-plugin" % "0.1.0"

### Importing sbt-plugin settings

To actually "activate" the plugin, its settings need to be included in the build.  The main steps are:

* `import com.linkedin.sbt._`
* add the `restli.All` trait to your `Build`
* Use `.compilePegasus()` and `.compileRestspec()` in your projects

##### project/Build.scala

```scala
import com.linkedin.sbt._
import sbt._
import Keys._

/**
 * This build includes the rest.All trait, enabling all pegasus project types.
 */
object Example extends Build with restli.All {

  val restliVersion = "1.15.4"

  val baseSettings = Seq(
    organization := "com.linkedin.pegasus.example",
    version := "0.0.1",
    resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
  )

  /**
   * This project is for hand written *.pdsc files.  It will generate "data template" class bindings into the
   * target/classes directory.
   */
  lazy val dataTemplate = Project("data-template", file("data-template"))
    .compilePegasus()
    .settings(libraryDependencies += "com.linkedin.pegasus" % "data" % restliVersion)
    .settings(baseSettings: _*)
    // add any dependencies other data template modules, to depend on their .pdscs, here.
    // e.g. .settings(libraryDependencies += "{group}" % "{name}" % "{version}" % "dataTemplate")
    // or,  .dependsOn(someOtherDataTemplateProject)

  /**
   * This project contains your handwritten Rest.li "resource" implementations.  See rest.li documentation for detail
   * on how to write resource classes.
   */
  lazy val sampleServer = Project("sample-server", file("sample-server"))
    .dependsOn(dataTemplate)
    .aggregate(dataTemplate, rest)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-server" % restliVersion)
    .settings(baseSettings: _*)
    .compileRestspec(
      apiName = "sample",
      apiProject = rest,
      resourcePackages = List("com.linkedin.pegasus.example"), // change this to match the package name where your *Resource.scala files reside.
      dataTemplateProject = dataTemplate,

      // change to "backwards" to enable rest.li's backward compatibility checker.  May also
      // be set to "equivalent", which is useful in continuous integration machinery to validate
      // that the rest project is in exact sync with the server implementation code.
      compatMode = "ignore"
    )

  /**
   * This project contains your API contract and will generate "client binding" classes into the
   * target/classes directory.  Clients to your rest.li service should depend on this project
   * or it's published artifacts (depend on the "restClient" configuration).
   *
   * Files under the src/idl and src/snapshot directories must be checked in to source control.  They are the
   * API contract and are used to generate client bindings and perform compatibility checking.
   */
  lazy val rest = Project("rest", file("rest"))
    .dependsOn(dataTemplate)
    .settings(baseSettings:_*)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-client" % restliVersion)
    .generateRequestBuilders(
      dataTemplateProject = dataTemplate
    )

  override lazy val rootProject = Option(sampleServer)
}
```