package com.linkedin.restli.tools.scala

import scala.tools.nsc.doc.base.{LinkTo, LinkToMember, LinkToTpl, LinkToExternal, Tooltip}

object Compat {
  def toDocString(linkTo: LinkTo):String = linkTo match {
    case LinkToMember(_, _) => "" // unsupported
    case LinkToTpl(_) => "" // unsupported
    case LinkToExternal(string, url) => s"""<a href="$url">$string</a>"""
    case Tooltip(name) => name
  }
}
