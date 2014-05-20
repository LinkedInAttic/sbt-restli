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

import com.linkedin.common.util.None
import com.linkedin.pegasus.example.client.SamplesBuilders
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter
import com.linkedin.r2.transport.http.client.HttpClientFactory
import java.util.concurrent.TimeUnit
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TestClient extends PlaySpec with OneServerPerSuite {

  "the rest.li client" must {
    "send a request to a rest.li server and get back a valid response" in {

      // in a real play application, this setup code (and the below shutdown code) should be put into a plugin
      val baseUrl = s"http://localhost:$port/"
      val httpClientFactory = new HttpClientFactory
      val r2Client = httpClientFactory.getClient(java.util.Collections.emptyMap[String, String]())
      val restliPlayClient = new RestLiPlayClient(new TransportClientAdapter(r2Client), baseUrl)


      // example request:
      val request = new SamplesBuilders().get().id(1L).build()
      val future = restliPlayClient.sendRequest(request)

      // in a real application, do NOT block like this, instead, map the future to an appropriate play application result
      // e.g. "future.map( result => Results.Ok(result.getEntity.getMessage))"
      Await.result(future, Duration(30, TimeUnit.SECONDS)).getEntity.getMessage mustBe "Hello, Rest.li!"


      // in a real play application, this shtudown code (and the above setup code) should be put into a plugin
      Await.result(restliPlayClient.shutdown(), Duration(30, TimeUnit.SECONDS))

      val callback = new CallbackPromiseAdapter[None]
      httpClientFactory.shutdown(callback)
      Await.result(callback.promise.future, Duration(30, TimeUnit.SECONDS))
    }
  }
}