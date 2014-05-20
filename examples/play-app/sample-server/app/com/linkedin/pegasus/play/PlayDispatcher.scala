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


import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher
import com.linkedin.r2.message.RequestContext
import com.linkedin.r2.message.rpc.{RpcResponse, RpcRequest}
import com.linkedin.r2.transport.common.bridge.common.TransportCallback
import com.linkedin.r2.message.rest.{RestResponse, RestRequest}
import java.util.{Map=>JMap}

/**
 * A TransportDispatcher that lets us set up Play-specific context data before handing off the request to the given
 * TransportDispatcher to do the real work. This dispatcher will call the given initRestliContext with the Rest.li
 * RequestContext object just before handling a rest request.
 */
class PlayDispatcher(transportDispatcher: TransportDispatcher, initRestliContext: RequestContext => Unit) extends TransportDispatcher {

  def handleRpcRequest(req: RpcRequest, wireAttrs: JMap[String, String], callback: TransportCallback[RpcResponse]) {
    throw new UnsupportedOperationException("RPC requests are not supported by the PlayDispatcher")
  }

  def handleRestRequest(req: RestRequest, wireAttrs: JMap[String, String], requestContext: RequestContext, callback: TransportCallback[RestResponse]) {
    initRestliContext(requestContext)
    transportDispatcher.handleRestRequest(req, wireAttrs, requestContext, callback)
  }
}
