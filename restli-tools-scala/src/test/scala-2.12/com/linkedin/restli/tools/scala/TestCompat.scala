package com.linkedin.restli.tools.scala

object TestCompat {
  val resourceTestString: String =
    """<p>A scala rest.li service.</p><p>Let's test some scaladoc.  First the wiki formats.</p><p>Styles: <b>bold</b>, <i>italic</i>, <code>monospace</code>, <em>underline</em>, <sup>superscript</sup>, <sub>subscript</sub></p><h1>Header</h1><h3>sub-heading</h3><p><a href="http://scala-lang.org">Scala</a></p><pre>x match {
      |  case Some(v) => println(v)
      |  case None => ()
      |}</pre><ul><li><p>unordered bullet 1</p></li><li><p>unordered bullet 2</p></li></ul><ol><li><p>ordered bullet 1</p></li><li><p>ordered bullet 2
      |</p></li></ol>""".stripMargin
}
