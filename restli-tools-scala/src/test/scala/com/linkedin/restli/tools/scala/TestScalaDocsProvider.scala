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
package com.linkedin.restli.tools.scala

import java.nio.file.Paths
import java.util.{Collections => JavaCollections}

import org.specs2.matcher.FileMatchers
import org.specs2.mutable._

class TestScalaDocsProvider extends Specification with FileMatchers {
  override def is = s2"""
    The ScalaGreetingsResource should
      exist                             $t1

    The ScalaDocsProvider should generate docs for
      a resource class                  $t2
      a resource method                 $t3
      a resource method parameter       $t4
      an action                         $t5
      an actions first parameter        $t6
      an actions second parameter       $t7
      a markdown table                  $t8
      a deprecated resource tag         $t9
      a deprecated action tag           $t10
    """

  private val resource = "src/test/scala/com/linkedin/restli/tools/scala/ScalaGreetingsResource.scala"

  private def t1 = resource must beAnExistingPath

  private val files = JavaCollections.singleton(Paths.get(resource).toAbsolutePath.toString)

  private val provider = new ScalaDocsProvider(null)
  provider.registerSourceFiles(files)

  private val method = classOf[ScalaGreetingsResource].getMethod("get", classOf[java.lang.Long])

  // behavior appears to have regressed in 2.10.  The code in the below <pre> tag no longer is tabbed properly like it was in 2.9.  Lame.
  // ^ Fixed in scala 2.12. Since we still support 2.10 must test both variations.
  private def t2 = provider.getClassDoc(classOf[ScalaGreetingsResource]) mustEqual TestCompat.resourceTestString

  private def t3 = compareDocString(
    """<p>Now let's test some html formatted scaladoc.</p>
      |<p><b>Some html</b> with a <a href="http://rest.li">link</a>. x<sup>a</sup><sub>b</sub>.</p>
      |<ul>
      |<li><p>unordered bullet 1</p></li>
      |<li><p>unordered bullet 2</p></li>
      |</ul>""".stripMargin,
    provider.getMethodDoc(method))

  private def t4 = compareDocString("<p>provides the key.</p>", provider.getParamDoc(method, "id"))

  private val action = classOf[ScalaGreetingsResource].getMethod("action", classOf[java.lang.String], classOf[java.lang.Boolean], classOf[java.lang.Boolean])

  private def t5 = compareDocString("<p>An action.</p>".stripMargin, provider.getMethodDoc(action))
  private def t6 = compareDocString("<p>provides a String</p>", provider.getParamDoc(action, "param1"))
  private def t7 = compareDocString("<p>provides a Boolean</p>", provider.getParamDoc(action, "param2"))

  private val tableAction = classOf[ScalaGreetingsResource].getMethod("tableAction")

  // Markdown table support added in 2.12.7
  private def t8 = compareDocString(TestCompat.tableTestString, provider.getMethodDoc(tableAction))

  private def t9 = compareDocString("<p><i>(Since version 1.0)</i> Resource deprecated</p>", provider.getClassDeprecatedTag(classOf[ScalaGreetingsResource]))

  private val deprecatedAction = classOf[ScalaGreetingsResource].getMethod("deprecatedAction")

  private def t10 = compareDocString("<p><i>(Since version 0.1)</i> Deprecated action</p>", provider.getMethodDeprecatedTag(deprecatedAction))

  private def strip(string: String): String = {
    string.replaceAll("\n", "").trim
  }

  private def compareDocString(expected: String, actual: String) = {
    strip(actual) mustEqual strip(expected)
  }
}