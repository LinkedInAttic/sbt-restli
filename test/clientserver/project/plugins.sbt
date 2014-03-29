val restliSbtPluginVersion = System.getProperty("restliSbtPluginVersion")

resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

unmanagedJars in Compile ~= {uj =>
    Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

libraryDependencies += "com.linkedin.pegasus" % "sbt-plugin_2.10" % restliSbtPluginVersion
