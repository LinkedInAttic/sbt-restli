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
package com.linkedin.pegasus.play


import play.api.mvc.{Action, Results, BodyParser}
import play.api.libs.iteratee.{Iteratee, Traversable}
import play.api.libs.concurrent.Execution.Implicits._
/**
 * @author jbetz@linkedin.com
 */

object PlayRestliServer {
  def byteArrayBodyParser: BodyParser[Array[Byte]] = {
    BodyParser("Rest.li body parser") { request =>
      Traversable
        .takeUpTo[Array[Byte]](1024*1024*100 /* 100 MB */)
        .transform(Iteratee.consume[Array[Byte]]())
        .flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
    }
  }

  def handler(path: String): Action[Array[Byte]] = Action.async(byteArrayBodyParser) { request =>
    RestliServerPlugin.getInstance.handleRestRequest(request)
  }
}
