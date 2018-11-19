package com.linkedin.sbtrestli.tools.scala

import scala.tools.nsc.doc.base._

object DocStringCompat extends DocString {
  override protected def linkToDocString: PartialFunction[LinkTo, String] = super.linkToDocString orElse {
    case LinkToExternal(name, baseUrl) => s"""<a href="$baseUrl">$name</a>"""
  }
}