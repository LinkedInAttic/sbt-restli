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


import play.api.{Application, Plugin, Play}
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher
import play.api.mvc.{RequestHeader, SimpleResult, Request}
import com.linkedin.r2.message.RequestContext
import play.core.j.JavaHelpers
import com.linkedin.restli.server.resources.{PrototypeResourceFactory, ResourceFactory}
import com.linkedin.restli.server.{RestLiServer, DelegatingTransportDispatcher, RestLiConfig}
import play.mvc.Http.Context
import java.util.concurrent.{Executors, ScheduledExecutorService}
import com.linkedin.parseq.{EngineBuilder, Engine}
import scala.concurrent.{Promise, Future}
import java.net.URI
import com.linkedin.r2.message.rest.{RestResponse, QueryTunnelUtil, RestRequestBuilder, RestRequest}
import com.linkedin.r2.transport.http.server.HttpDispatcher
import com.linkedin.r2.transport.common.bridge.common.{TransportResponse, TransportCallback}
import scala.collection.JavaConverters._

/**
 * @author jbetz@linkedin.com
 */
object RestliServerPlugin {
  def getInstance = Play.current.plugin(classOf[RestliServerPlugin]).get
}

class RestliServerPlugin(app: Application) extends Plugin with RestliServerPluginHandlers {

  var restliDispatcher: Option[TransportDispatcher] = None

  override def onStart() {
    restliDispatcher = Some(buildRestliDispatcher)
  }

  /**
   * Adds the Context (for Java users) and Request (for Scala users) to the Rest.li RequestContext so it's available
   * in Rest.li resources. This method also adds the Context to thread local, as is the convention in Play for Java.
   */
  private def initRestliContext(playRequest: Request[Array[Byte]])(restliContext: RequestContext) {
    val context = JavaHelpers.createJavaContext(playRequest)
    Context.current.set(context)

    restliContext.putLocalAttr("play.playRequest", playRequest)
    restliContext.putLocalAttr("play.playContext", context)
  }

  def buildRestliDispatcher: TransportDispatcher = {
    val resourceFactory: ResourceFactory = new PrototypeResourceFactory
    val config: RestLiConfig = new RestLiConfig
    config.setResourcePackageNamesSet(java.util.Collections.singleton[String]("com.linkedin.pegasus.example"))
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(24)
    val engine: Engine = new EngineBuilder().setTaskExecutor(scheduler).setTimerScheduler(scheduler).build
    new DelegatingTransportDispatcher(new RestLiServer(config, resourceFactory, engine))
  }

  def handleRestRequest(playRequest: Request[Array[Byte]]): Future[SimpleResult] = {
    val promise = Promise[SimpleResult]()
    val restliRequest = createRestRequest(playRequest, playRequest.body)
    val callback = new RestliTransportCallback(promise, this)
    handleRequest(restliRequest, callback, playRequest)
    promise.future
  }

  private def createRequestUri(header: RequestHeader): URI = {
    new URI(header.uri)
  }

  private def createRestRequest(header: RequestHeader, body: Array[Byte]): RestRequest = {
    val builder = new RestRequestBuilder(createRequestUri(header))
    builder.setMethod(header.method)
    builder.setHeaders(header.headers.toSimpleMap.asJava)
    builder.setEntity(body)
    QueryTunnelUtil.decode(builder.build())
  }

  private def handleRequest(restliRequest: RestRequest, callback: RestliTransportCallback, playRequest: Request[Array[Byte]]) {
    val playDispatcher = new PlayDispatcher(restliDispatcher.get, initRestliContext(playRequest))
    val httpDispatcher = new HttpDispatcher(playDispatcher) // a normal R2 http dispatcher
    httpDispatcher.handleRequest(restliRequest, callback)
  }

  private class RestliTransportCallback(promise: Promise[SimpleResult], restliDispatcher: RestliServerPluginHandlers) extends TransportCallback[RestResponse] {
    override def onResponse(response: TransportResponse[RestResponse]) {
      restliDispatcher.redeemPromiseWithRestliResponse(promise, response)
    }
  }
}