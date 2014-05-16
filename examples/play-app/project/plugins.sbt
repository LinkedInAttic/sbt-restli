resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

unmanagedJars in Compile ~= {uj =>
    Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

libraryDependencies += "com.linkedin.pegasus" % "sbt-plugin_2.10" % "0.1.4"


// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")