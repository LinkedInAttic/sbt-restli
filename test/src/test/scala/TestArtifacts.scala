package com.linkedin.pegasus.restli.sbt.plugin.test

import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import org.apache.commons.io.FileUtils
import scala.xml.{XML, Elem, Node}
import java.util.{Collection=>JavaCollection}

/**
 * Validate that ivy xml files are generated as part of SBT publication and that they contain the configurations and
 * publication artifacts the plugin directs SBT to add.
 */
class TestArtifacts {
  val artifactsDir = System.getProperty("test.artifactsDir")

  @Test
  def testDataTemplateArtifacts() = {
    val ivyModule = loadIvyForProject("data-template")

    Assert.assertTrue(findConf(ivyModule, "dataModel").isDefined)
    Assert.assertTrue(findConf(ivyModule, "dataTemplate").isDefined)

    val dataModelArtifactOpt = findArtifact(ivyModule, "data-template-dataModel")
    Assert.assertTrue(dataModelArtifactOpt.isDefined)
    Assert.assertEquals((dataModelArtifactOpt.get \ "@conf").text, "dataModel")
  }

  @Test
  def testRestClientArtifacts() = {
    val ivyModule = loadIvyForProject("rest")

    Assert.assertTrue(findConf(ivyModule, "restModel").isDefined)
    Assert.assertTrue(findConf(ivyModule, "restClient").isDefined)

    val restModelArtifactOpt = findArtifact(ivyModule, "rest-restModel")
    Assert.assertTrue(restModelArtifactOpt.isDefined)
    Assert.assertEquals((restModelArtifactOpt.get \ "@conf").text, "restModel")
  }

  private def loadIvyForProject(projectName: String): Elem = {
    val dataTemplateArtifactRootDir = new File(artifactsDir + File.separator + projectName)
    val ivyFiles = FileUtils.listFiles(dataTemplateArtifactRootDir, Array("xml"), true).asInstanceOf[JavaCollection[File]]

    Assert.assertEquals(ivyFiles.size, 1)

    val ivyFile = ivyFiles.iterator.next
    XML.loadFile(ivyFile)
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