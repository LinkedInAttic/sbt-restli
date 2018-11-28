/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.sbtrestli.tools.scala

object TestCompat {
  val resourceTestString: String =
    """<p>A scala rest.li service.</p><p>Let's test some scaladoc.  First the wiki formats.</p><p>Styles: <b>bold</b>, <i>italic</i>, <code>monospace</code>, <em>underline</em>, <sup>superscript</sup>, <sub>subscript</sub></p><h1>Header</h1><h3>sub-heading</h3><p><a href="http://scala-lang.org">Scala</a></p><pre>x match {
      |  case Some(v) => println(v)
      |  case None => ()
      |}</pre><ul><li><p>unordered bullet 1</p></li><li><p>unordered bullet 2</p></li></ul><ol><li><p>ordered bullet 1</p></li><li><p>ordered bullet 2
      |</p></li></ol>""".stripMargin

  val tableTestString: String =
    "<p>Test table markdown</p><p>|abc|defghi|jk||:-:|-----------:|---||bar|baz|foo|</p>"
}
