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
package sbtrestli.util

import sbt._
import xsbti.{Position, Problem, Severity}

/**
 * In order to produce exceptions that can be pretty printed by play (or any framework built on SBT).  We must
 * produce a xsbti.CompilationFailed exception.   Because sbt only provides an interface, we must implement all intefaces
 * from xsbti that we require.
 */
class RestliCompilationException(source: Option[File], message: String, atLine: Option[Int], column: Option[Int], severity: Severity) extends xsbti.CompileFailed {
  def arguments(): Array[String] = Array()
  def problems(): Array[Problem] = Array(new RestliCompilationProblem(source, message, atLine, column, severity))
  def line: Integer = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
  def position: Integer = column.map(_.asInstanceOf[java.lang.Integer]).orNull
  def sourceName: String = source.map(_.getAbsolutePath).orNull
}

class RestliCompilationProblem(source: Option[File], msg: String, atLine: Option[Int], column: Option[Int], svrty: Severity) extends Problem {
  def category(): String = "Rest.li"
  def severity(): Severity = svrty
  def message(): String = msg
  def position(): Position = new RestliCompilationErrorPosition(source, atLine, column)
}

