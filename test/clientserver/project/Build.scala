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

object ClientServerTestBuild extends Build with restli.All {

  val restliVersion = System.getProperty("pegasusVersion")

  val baseSettings = Seq(
    resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
    publishTo := Some(Resolver.file("local artifacts temp repository", new File("artifacts"))),
    publishMavenStyle := false
  )

  lazy val dataTemplate = Project("data-template", file("data-template"))
    .compilePegasus()
    .settings(baseSettings: _*)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "data" % restliVersion)

  lazy val server = Project("server", file("server"))
    .dependsOn(dataTemplate, rest)
    .aggregate(dataTemplate, rest)
    .settings(baseSettings: _*)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-server" % restliVersion)
    .compileRestspec(
      apiName = "client-server",
      apiProject = rest,
      resourcePackages = List("com.linkedin.pegasus.example"), // change this to match the package name where your *Resource.scala files reside.
      dataTemplateProject = dataTemplate,
      compatMode = "backwards"
    )

  lazy val rest = Project("rest", file("rest"))
    .dependsOn(dataTemplate)
    .settings(baseSettings:_*)
    .settings(libraryDependencies += "com.linkedin.pegasus" % "restli-client" % restliVersion)
    .generateRequestBuilders(
      dataTemplateProject = dataTemplate
    )

  override lazy val rootProject = Option(server)
}