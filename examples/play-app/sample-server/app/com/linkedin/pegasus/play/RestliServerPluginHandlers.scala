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


import scala.concurrent.Promise
import play.api.mvc.{ResponseHeader, Results, SimpleResult}
import com.linkedin.r2.transport.common.bridge.common.TransportResponse
import com.linkedin.r2.message.rest.{RestException, RestResponse}
import play.api.libs.json.Json
import scala.collection.JavaConverters._

/**
 * @author jbetz@linkedin.com
 */
trait RestliServerPluginHandlers {
  def redeemPromiseWithRestliResponse(promise: Promise[SimpleResult], response: TransportResponse[RestResponse]) = {
    if (response.hasError) {
      onRestliError(response.getError, promise)
    } else {
      onRestliSuccess(response.getResponse, promise)
    }
  }

  def onRestliSuccess(response: RestResponse, promise: Promise[SimpleResult]) {
    val entity = response.restBuilder().getEntity
    val entityAsBytes = entity.copyBytes()
    val result = Results.Status(response.getStatus).apply(entityAsBytes)

    // TODO: even though this method is called "onRestliSuccess", it seems that Rest.li considers 5xx responses
    // successful, so we'll end up here even if there was an exception thrown by the Resource. This is a temporary
    // workaround to log these exceptions until we figure out the proper way to do it.
    if (response.getStatus >= 500) {
      play.Logger.error("Rest.li returned a %d response code with body: %s".format(response.getStatus, entity.asString("UTF-8")))
    }

    // Ensure that we *only* include headers from the RestResponse and not any that the Play Status object may add by default
    val resultWithHeaders = result.copy(header = ResponseHeader(response.getStatus, response.getHeaders.asScala.toMap))
    promise.success(resultWithHeaders)
  }

  def onRestliError(error: Throwable, promise: Promise[SimpleResult]) {
    error match {
      case restException: RestException => onRestException(restException, promise)
      case _ => promise.failure(error)
    }
  }

  def onRestException(restException: RestException, promise: Promise[SimpleResult]) {
    val response = restException.getResponse
    val entity = Json.parse(response.restBuilder.getEntity.asString("UTF-8"))

    val errorMessage = (entity \ "message").asOpt[String].getOrElse("Unspecified Rest.li error")
    val errorHeader = ("X-LinkedIn-Error", errorMessage)

    if (response.getStatus >= 500) {
      play.Logger.error("Rest.li returned a %d response code".format(response.getStatus), restException)
    }

    val result = Results.Status(response.getStatus).withHeaders(errorHeader)
    promise.success(result)
  }
}
