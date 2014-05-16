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
