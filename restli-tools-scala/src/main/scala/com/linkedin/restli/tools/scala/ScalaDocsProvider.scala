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
package com.linkedin.restli.tools.scala

import java.lang.reflect.Method
import java.util.{Collection => JCollection, Collections => JCollections, Set => JSet}

import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.tools.nsc.doc.base.LinkTo
import scala.tools.nsc.doc.base.comment._
import scala.tools.nsc.doc.model.{Def, DocTemplateEntity, MemberEntity, TemplateEntity}
import scala.tools.nsc.doc.{DocFactory, Settings}
import scala.tools.nsc.reporters.ConsoleReporter

/** Scaladoc version of a rest.li DocsProvider. */
class ScalaDocsProvider(classpath: Array[String]) extends DocsProvider {
  val log: Logger = LoggerFactory.getLogger(classOf[ScalaDocsProvider])

  def this() = this(Array())

  private var root: Option[DocTemplateEntity] = None

  override def registerSourceFiles(files: JCollection[String]) {
    root = if(files.size() == 0) {
      None
    } else {
      val settings = new Settings(log.error)
      if(classpath == null) {
        settings.usejavacp.value = true
      } else {
        settings.classpath.value = classpath.mkString(":")
      }
      val reporter = new ConsoleReporter(settings)
      val docFactory = new DocFactory(reporter, settings)
      val filelist = if (files == null || files.size == 0) List() else files.asScala.toList
      val universe = docFactory.makeUniverse(Left(filelist))
      universe.map(_.rootPackage.asInstanceOf[DocTemplateEntity])
    }
  }

  override def supportedFileExtensions: JSet[String] = {
    JCollections.singleton(".scala")
  }

  override def getClassDoc(resourceClass: Class[_]): String = {
    findTemplate(resourceClass)
      .flatMap(_.comment)
      .map(comment => DocStringCompat(comment.body))
      .orNull
  }

  override def getClassDeprecatedTag(resourceClass: Class[_]): String = {
    findTemplate(resourceClass)
      .flatMap(_.deprecation)
      .map(DocStringCompat.apply)
      .orNull
  }

  override def getMethodDoc(method: Method): String = {
    findMethod(method)
      .flatMap(_.comment)
      .map(comment => DocStringCompat(comment.body))
      .orNull
  }

  override def getMethodDeprecatedTag(method: Method): String = {
    findMethod(method)
      .flatMap(_.deprecation)
      .map(DocStringCompat.apply)
      .orNull
  }

  override def getParamDoc(method: Method, name: String): String = {
    findMethod(method)
      .flatMap(_.comment)
      .flatMap(_.valueParams.get(name))
      .map(DocStringCompat.apply)
      .orNull
  }

  override def getReturnDoc(method: Method): String = {
    findMethod(method)
      .flatMap(_.comment)
      .flatMap(_.result)
      .map(DocStringCompat.apply)
      .orNull
  }

  private def filterDocTemplates(templates: List[TemplateEntity with MemberEntity]): List[DocTemplateEntity] = {
    templates.collect {
      case template if template.isDocTemplate && template.isClass =>
        template.asInstanceOf[DocTemplateEntity]
    }
  }

  /** Searches the AST starting at "root" for the given class.  E.g. "com.example.Foo.class" is searched for
   *  by traversing first down the docTemplate for the template named "com", then "example", then finally "Foo".
   */
  private def findTemplate(resourceClass: Class[_]): Option[DocTemplateEntity] = {
    def findAtPath(docTemplate: DocTemplateEntity, namespaceParts: List[String]): Option[DocTemplateEntity] = {
      namespaceParts match {
        case Nil => None
        case namespacePart :: Nil => filterDocTemplates(docTemplate.templates).find(_.name == namespacePart)
        case namespacePart :: remainingNamespaceParts =>
          docTemplate.templates.find(_.name == namespacePart) match {
            case Some(childDocTemplate: DocTemplateEntity) => findAtPath(childDocTemplate, remainingNamespaceParts)
            case _ => None
          }
      }
    }

    root flatMap {
      r =>
        findAtPath(r, resourceClass.getCanonicalName.split('.').toList)
    }
  }

  /** Given a Method signature (where Method is a method from a JVM .class), finds the matching scala method "Def"
   *  (a AST type from the new scala compiler) so we can get it's scaladoc.
   *
   *  This can be a bit tricky given that scala "Def" can have represent all possible scala signatures, which
   *  includes stuff like:
   *
   *  def foo = {}
   *  def foo() = {}
   *  def foo(a: Int)(b: Int) = {}
   *  ...
   */
  private def findMethod(methodToFind: Method): Option[Def]  = {
    findTemplate(methodToFind.getDeclaringClass).flatMap { docTemplateForClass =>
      docTemplateForClass.methods find { templateMethod =>

      // e.g. the scala method "foo(a: Int, b: Int)(c: String)" has two "valueParams", one with two params and a second with one param
        val templateValueParamSetCount = templateMethod.valueParams.length

        // e.g. the JVM method "bar(Integer a, Integer b)" has two parameters
        val methodToFindParamCount = methodToFind.getParameterTypes.length

        // true if both have no params, this covers the special case of a scala "Def" method that has no params, e.g. "def baz" instead of "def baz()"
        val bothHaveNoParams = templateValueParamSetCount == 0 && methodToFindParamCount == 0

        // true if the scala "Def" method has only one "valueParams" and it has the same number of parameters as the "Method".
        val bothHaveSameParamCount = templateValueParamSetCount == 1 && templateMethod.valueParams.head.length == methodToFindParamCount

        val haveMatchingParams = bothHaveNoParams || bothHaveSameParamCount

        // To be precise here, we should check all param types match, but this is exceedingly complex.
        // Method is from java.lang.reflect which has java types and templateMethod is from scala's AST
        // which has scala types.  The mapping between the two, particularly for primitive types, is involved.
        // Given that rest.li has strong method naming conventions,  name and param count should be sufficient
        // in all but the most pathological cases.  One option would be to check the annotations if
        // additional disambiguation is needed.

        (templateMethod.name == methodToFind.getName) && haveMatchingParams
      }
    }
  }
}
