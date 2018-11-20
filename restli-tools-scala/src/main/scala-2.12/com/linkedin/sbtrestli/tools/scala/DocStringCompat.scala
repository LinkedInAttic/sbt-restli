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

import scala.tools.nsc.doc.base._
import scala.tools.nsc.doc.base.comment._

object DocStringCompat extends DocString {
  override protected def linkToDocString: PartialFunction[LinkTo, String] = super.linkToDocString orElse {
    case LinkToExternalTpl(name, baseUrl, _) => s"""<a href="$baseUrl">$name</a>"""
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