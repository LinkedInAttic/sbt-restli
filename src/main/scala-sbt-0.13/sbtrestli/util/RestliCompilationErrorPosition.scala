package sbtrestli.util

import sbt._
import xsbti.{Position, Maybe}

class RestliCompilationErrorPosition(source: Option[File], atLine: Option[Int], column: Option[Int]) extends Position {
  private class Just[T](value: T) extends Maybe[T] {
    def isDefined: Boolean = true
    def get(): T = value
  }

  private class Nothing[T] extends Maybe[T] {
    def isDefined: Boolean = false
    def get(): T = throw new IllegalStateException("Cannot call get() on a Maybe that is Nothing.")
  }

  private def toMaybe[T](option: Option[T]): Maybe[T] = option.map { optionValue => new Just(optionValue)}.getOrElse(new Nothing[T])

  def line(): Maybe[Integer] = toMaybe(atLine.map(_.asInstanceOf[java.lang.Integer]))
  def lineContent(): String = ""
  def offset(): Maybe[Integer] = toMaybe(column.map(_.asInstanceOf[java.lang.Integer]))
  def pointer(): Maybe[Integer] = new Nothing[Integer]
  def pointerSpace(): Maybe[String] = new Nothing[String]
  def sourcePath(): Maybe[String] = toMaybe(source.map(_.getAbsolutePath))
  def sourceFile(): Maybe[File] = toMaybe(source)
}

