package com.linkedin.pegasus.restli.sbt.plugin.test

import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.{FileInputStream, File}
import org.apache.commons.io.FileUtils
import scala.xml.{XML, Elem, Node}
import java.util.{Collection=>JavaCollection}
import scala.collection.JavaConverters._
import scala.io.Source
import java.util.zip.ZipInputStream

/**
 * Validate that ivy xml files are generated as part of SBT publication and that they contain the configurations and
 * publication artifacts the plugin adds.
 */
class TestArtifacts {
  val artifactsDir = System.getProperty("test.artifactsDir")
  val version = "0.1-SNAPSHOT"

  @Test
  def testDataTemplateIvy() = {
    val ivyModule = loadIvyForProject("data-template")

    Assert.assertTrue(findConf(ivyModule, "dataModel").isDefined)
    Assert.assertTrue(findConf(ivyModule, "dataTemplate").isDefined)

    val dataModelArtifactOpt = findArtifact(ivyModule, "data-template-dataModel")
    Assert.assertTrue(dataModelArtifactOpt.isDefined)
    Assert.assertEquals((dataModelArtifactOpt.get \ "@conf").text, "dataModel")
  }

  @Test
  def testDataTemplateJar() = {
    val filenames = listFilesInJarForProject("data-template")
    Assert.assertTrue(filenames.contains("pegasus/com/linkedin/pegasus/example/Sample.pdsc"))
    Assert.assertTrue(filenames.contains("com/linkedin/pegasus/example/Sample.class"))
  }

  @Test
  def testDataTemplateDataModelJar() = {
    val filenames = listFilesInJarForProject("data-template", Some("dataModel"))
    Assert.assertTrue(filenames.contains("com/linkedin/pegasus/example/Sample.pdsc"))
  }

  @Test
  def testRestClientIvy() = {
    val ivyModule = loadIvyForProject("rest")

    Assert.assertTrue(findConf(ivyModule, "restModel").isDefined)
    Assert.assertTrue(findConf(ivyModule, "restClient").isDefined)

    val restModelArtifactOpt = findArtifact(ivyModule, "rest-restModel")
    Assert.assertTrue(restModelArtifactOpt.isDefined)
    Assert.assertEquals((restModelArtifactOpt.get \ "@conf").text, "restModel")
  }

  @Test
  def testRestJar() = {
    val filenames = listFilesInJarForProject("rest")
    Assert.assertTrue(filenames.contains("com/linkedin/pegasus/example/client/SamplesBuilders.class"))
    Assert.assertTrue(filenames.contains("com/linkedin/pegasus/example/client/SamplesGetBuilder.class"))
    Assert.assertTrue(filenames.contains("com/linkedin/pegasus/example/client/SamplesCreateBuilder.class"))
  }

  @Test
  def testRestRestModelJar() = {
    val filenames = listFilesInJarForProject("rest", Some("restModel"))
    Assert.assertTrue(filenames.contains("sample-com.linkedin.pegasus.example.client.samples.restspec.json"))
  }

  private def listFilesInJarForProject(projectName: String, conf: Option[String] = None): List[String] = {
    val jarFileName = projectName + conf.map("-" + _).getOrElse("") + "-" + version + ".jar"

    val matchingJarFiles = listFilesForProjectWithExtension(projectName, "jar").filter(_.getName == jarFileName)

    Assert.assertEquals(matchingJarFiles.size, 1)

    val zipIn = new ZipInputStream(new FileInputStream(matchingJarFiles.head))
    try {
      Stream.continually(zipIn.getNextEntry).takeWhile(null !=).map(_.getName).toList
    } finally {
      zipIn.close()
    }
  }

  private def loadIvyForProject(projectName: String): Elem = {
    val ivyFiles = listFilesForProjectWithExtension(projectName, "xml")
    Assert.assertEquals(ivyFiles.size, 1)

    val ivyFile = ivyFiles.head
    XML.loadFile(ivyFile)
  }

  private def listFilesForProjectWithExtension(projectName: String, extension: String): List[File] = {
    val dataTemplateArtifactRootDir = new File(artifactsDir + File.separator + projectName)
    FileUtils.listFiles(dataTemplateArtifactRootDir, Array(extension), true).asInstanceOf[JavaCollection[File]].asScala.toList
  }

  private def findConf(ivyModule: Elem, confName: String): Option[Node] = {
    val matches = for {
      configurations <- ivyModule \ "configurations"
      conf <- configurations \ "conf"
      if (conf \ "@name").text == confName
    } yield conf
    matches.flatten.headOption
  }

  private def findArtifact(ivyModule: Elem, artifactName: String): Option[Node] = {
    val matches = for {
      publications <- ivyModule \ "publications"
      artifact <- publications \ "artifact"
      if (artifact \ "@name").text == artifactName
    } yield artifact
    matches.flatten.headOption
  }
}