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

package com.linkedin.pegasus.example


import com.linkedin.restli.server.resources.CollectionResourceTemplate
import com.linkedin.restli.server.CreateResponse
import com.linkedin.restli.server.annotations.RestLiCollection
import com.linkedin.pegasus.example.client.SamplesBuilders

import javax.inject.{Inject, Named}

@RestLiCollection(name="samples", namespace = "com.linkedin.pegasus.example.client")
class SamplesResource extends CollectionResourceTemplate[java.lang.Long, Sample] {

  // Depending on this resources client builders here to verify we correctly support this use case.
  // Support for resources using their own builds is to allow for resource implementations to calls peers.
  val builders = new SamplesBuilders

  override def get(key: java.lang.Long): Sample = {

    // attempt to use builder to this resource, to verify we can handle this use case
    builders.get()

    new Sample()
            .setMessage("Hello, Rest.li!")
            .setId(key)
  }

  override def create(sample: Sample): CreateResponse = {
    new CreateResponse()
  }
}
