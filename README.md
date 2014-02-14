rest.li-sbt-plugin
==================

SBT plugin for [rest.li](https://github.com/linkedin/rest.li) that provides all the build tasks required to build
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

To build from source and install as a snapshot in your local repo, clone this repo and build it:

For Ivy:

    ./gradlew uploadArchives

Ivy artifacts will be written into `~/.ivy2/local`.

For Maven:

    ./gradlew install

Maven artifacts will be written into `~/.m2/repository`.

Once built, you'll need to remember to add resolvers to these repositories when you use the plugin in your sbt projects
When following the below "usage" directions, remember to put this in your project/Build.scala:

    val baseSettings = Seq(
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
      // OR
      resolvers += Resolver.file("Local Ivy Repository", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    )

and this in your `project/plugins.scala`:

    resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"
    // OR
    resolvers += Resolver.file("Local Ivy Repository", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

Getting Started
---------------

### Adding the plugin dependency

In your project, create a file for plugin library dependencies `project/plugins.sbt` and add the following lines:

    // add any resolvers required for the plugin library dependency here (see above if building from source)

    libraryDependencies += "com.linkedin.pegasus" %% "sbt-plugin" % "0.0.1"

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

  val baseSettings = Seq(
    // add any resolvers required for the plugin library dependency here (see above if building from source)
  )

  /**
   * This project is for hand written *.pdsc files.  It will generate "data template" class bindings into the
   * target/classes directory.
   */
  lazy val dataTemplate = Project("data-template", file("data-template"))
    .compilePegasus()
    .settings(libraryDependencies += "com.linkedin.pegasus" % "data" % "1.13.4")
    .settings(baseSettings: _*)
    // add any dependencies other data template modules, to depend on their .pdscs, here.
    // e.g. .settings(libraryDependencies += "{group}" % "{name}" % "{version}" % "dataTemplate")
    // or, .dependsOn(someOtherDataTemplateProject)

  /**
   * This project contains your handwritten Rest.li "resource" implementations.  See rest.li documentation for detail
   * on how to write resource classes.
   */
  lazy val sampleServer = Project("sample-server", file("sample-server"))
    .dependsOn(dataTemplate)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "1.13.4")
    .settings(baseSettings: _*)

  /**
   * This project contains your API contract and will generate "client binding" class into the
   * target/classes directory.  Clients to your rest.li service should depend on this project
   * or it's published artifacts (depend on the "restClient" configuration).
   *
   * Files under the src/idl and src/snapshot directories must be checked in to source control.  They are the
   * API contract and are used to generate client bindings and perform compatibility checking.
   */
  lazy val rest = Project("rest", file("rest"))
    .dependsOn(dataTemplate)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "1.13.4")
    .settings(baseSettings:_*)
    .compileRestspec(
      apiName = "sample",
      resourceProject = sampleServer,
      resourcePackages = List("com.linkedin.pegasus.example"), // change this to match the package name where your *Resource.scala files reside.
      dataTemplateProject = dataTemplate,

      // change to "backwards" to enable rest.li's backward compatibility checker.  May also
      // be set to "equivalent", which is useful in continuous integration machinery to validate
      // that the rest project is in exact sync with the server implementation code.
      compatMode = "ignore"
    )
}
```