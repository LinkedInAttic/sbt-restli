package com.linkedin.restli.tools.scala

import scala.tools.nsc.doc.base._
import scala.tools.nsc.doc.base.comment._

object DocStringCompat extends DocString {
  override protected def linkToDocString: PartialFunction[LinkTo, String] = super.linkToDocString orElse {
    case LinkToExternalTpl(name, baseUrl, _) => s"""<a href=$baseUrl>$name</a>"""
  }

  override protected def blockDocString: PartialFunction[Block, String] = super.blockDocString orElse {
    case Table(header, columnOptions, rows) =>
      val head = rowDocString(header, columnOptions, isHeader = true)
      val body = rows.map(row => rowDocString(row, columnOptions)).mkString
      s"<table><thead>$head</thead><tbody>$body</tbody></table>"
  }

  private def alignAttr(columnOption: ColumnOption): String = {
    columnOption match {
      case ColumnOption.ColumnOptionCenter => " align=\"center\""
      case ColumnOption.ColumnOptionRight => " align=\"right\""
      case _ => ""
    }
  }

  private def rowDocString(row: Row, columnOptions: Seq[ColumnOption], isHeader: Boolean = false): String = {
    val Row(cells) = row

    val cellDocStrings = cells
      .zip(columnOptions)
      .map { case (cell, columnOption) =>
        cellDocString(cell, columnOption, isHeader)
      }

    s"<tr>${cellDocStrings.mkString}</tr>"
  }

  private def cellDocString(cell: Cell, columnOption: ColumnOption, isHeader: Boolean = false): String = {
    val Cell(blocks) = cell
    val tag = if (isHeader) "th" else "td"
    s"<$tag${alignAttr(columnOption)}>${blocks.map(blockDocString).mkString}</$tag>"
  }
}