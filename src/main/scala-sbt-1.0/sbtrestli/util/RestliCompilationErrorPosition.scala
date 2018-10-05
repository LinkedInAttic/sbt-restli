package sbtrestli.util

import java.util.Optional

import sbt._
import xsbti.Position

class RestliCompilationErrorPosition(source: Option[File], atLine: Option[Int], column: Option[Int]) extends Position {
  def line(): Optional[Integer] = atLine.map(_.asInstanceOf[java.lang.Integer]).asJava
  def lineContent(): String = ""
  def offset(): Optional[Integer] = column.map(_.asInstanceOf[java.lang.Integer]).asJava
  def pointer(): Optional[Integer] = Optional.empty()
  def pointerSpace(): Optional[String] = Optional.empty()
  def sourcePath(): Optional[String] = source.map(_.getAbsolutePath).asJava
  def sourceFile(): Optional[File] = source.asJava
}

