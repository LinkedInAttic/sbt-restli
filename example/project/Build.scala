/*
   Copyright (c) 2014 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


import com.linkedin.sbt._
import sbt._
import Keys._

/**
 * This project is for hand written *.pdsc files.  It will generate "data template" class bindings into the
 * target/classes directory.
 */
object Example extends Build with restli.All {

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
    .settings(libraryDependencies += "com.linkedin.pegasus" % "data" % "1.13.4")
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
    .settings(autoScalaLibrary := false)
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

  override lazy val rootProject = Option(sampleServer)
}