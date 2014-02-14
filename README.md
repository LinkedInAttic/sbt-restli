# restli-sbt-plugin
A plugin for rest.li that provides all the build tasks required to build rest.li applications.  This includes `.pdsc` to data template code generation, `.pdsc` to `.avro` translation, rest client builder generation and idl publishing and validation.

## Requirements

Java 1.6+
Gradle 1.8+ (optional, sort of, if not installed on local machine './gradlew' can be used and it will download the gradle jar)

## Building

(if you already have gradle installed, you can just run "gradle" instead of "./gradlew", if you like.  gradle 1.8+ required)

To build and install as a snapshot in your local maven repo, run:

For Ivy:

    ./gradlew uploadArchives

Ivy artifacts will be written into ~/.ivy2/local.

For Maven:

    ./gradlew install

Maven artifacts will be written into ~/.m2/repository.

And be sure you include this in your project/Build.scala:

    val baseSettings = Seq(
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    )

and this in your project/plugins.scala:

    resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

## Usage

### Adding the plugin dependency

In your project, create a file for plugin library dependencies `project/plugins/build.sbt` and add the following lines:

    // add any resolvers required for the below library dependency here

    libraryDependencies += "com.linkedin.pegasus" %% "sbt-plugin" % "0.0.1"

### Importing sbt-plugin settings
To actually "activate" the plugin, its settings need to be included in the build.

##### build.sbt

    import com.linkedin.sbt._

##### build.scala

    import com.linkedin.sbt._
    import sbt._
    import Keys._

    object Example extends Build with restli.All {

      val baseSettings = Seq(
        // add any resolvers required for the below library dependencies here
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

      /**
       * This project is for handwritten Rest.li server code -- resource classes and support code.
       */
      lazy val sampleServer = Project("sample-server", file("sample-server"))
        .dependsOn(dataTemplate)
        .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-server" % "1.13.4")
        .settings(baseSettings: _*)

      /**
       * This project is for generated client bindings, which are entirely generated code.  Clients to your rest.li
       * service should depend on this project or it's published artifacts.
       */
      lazy val rest = Project("rest", file("rest"))
        .dependsOn(dataTemplate)
        .settings(baseSettings:_*)
        .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-client" % "1.13.4")
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
