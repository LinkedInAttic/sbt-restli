package com.linkedin.sbtrestli.tools.scala

import scala.tools.nsc.doc.base._
import scala.tools.nsc.doc.base.comment._

trait DocString {
  def apply(body: Body): String = body.blocks.map(blockDocString).mkString.trim

  protected def linkToDocString: PartialFunction[LinkTo, String] = {
    case LinkToMember(_, _) => "" // unsupported
    case LinkToTpl(_) => "" // unsupported
    case Tooltip(name) => name
  }

  protected def blockDocString: PartialFunction[Block, String] = {
    case Paragraph(inline) => s"<p>${inlineDocString(inline)}</p>"
    case Title(text, level) => s"<h$level>${inlineDocString(text)}</h$level>"
    case Code(data) => s"<pre>$data</pre>"
    case UnorderedList(items) =>
      "<ul>" + items.map(i => s"<li>${blockDocString(i)}</li>").mkString + "</ul>"
    case OrderedList(items, _) =>
      "<ol>" + items.map(i => s"<li>${blockDocString(i)}</li>").mkString + "</ol>"
    case DefinitionList(items) =>
      "<dl>" + items.map{ case (key, value) => s"<dt>$key</dt><dd>$value</dd>"}.mkString + "</dl>"
    case HorizontalRule() => "<hr>"
  }

  // We're using html formatting here, like is done by rest.li already for javadoc
  private def inlineDocString: PartialFunction[Inline, String] = {
    case Bold(inline) => s"<b>${inlineDocString(inline)}</b>"
    case Chain(items) => items.map(inlineDocString).mkString
    case Italic(inline) => s"<i>${inlineDocString(inline)}</i>"
    case Link(target, inline) => s"""<a href="$target">${inlineDocString(inline)}</a>"""
    case Monospace(inline) => s"<code>${inlineDocString(inline)}</code>"
    case Summary(inline) => inlineDocString(inline)
    case Superscript(inline) => s"<sup>${inlineDocString(inline)}</sup>"
    case Subscript(inline) => s"<sub>${inlineDocString(inline)}</sub>"
    // we don't have a way to retain scaladoc (or javadoc) entity links, so we'll just include the fully qualified name
    case EntityLink(title, linkTo) => s"""<a href="${linkToDocString(linkTo)}">${inlineDocString(title)}</a>"""
    case Text(text) => text
    // underlining is discouraged in html because it makes text resemble a link, so we'll go with em, a popular alternative
    case Underline(inline) => s"<em>${inlineDocString(inline)}</em>"
    case HtmlTag(rawHtml) => rawHtml
  }
}
