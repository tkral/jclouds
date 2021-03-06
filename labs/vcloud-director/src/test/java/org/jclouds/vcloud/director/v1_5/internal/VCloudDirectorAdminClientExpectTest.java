/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.vcloud.director.v1_5.internal;

import java.util.Properties;

import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.logging.config.NullLoggingModule;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.vcloud.director.v1_5.VCloudDirectorContext;
import org.jclouds.vcloud.director.v1_5.admin.VCloudDirectorAdminClient;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * 
 * @author Adrian Cole
 */
public abstract class VCloudDirectorAdminClientExpectTest extends
         BaseVCloudDirectorExpectTest<VCloudDirectorAdminClient> implements
         Function<VCloudDirectorContext, VCloudDirectorAdminClient> {

   @Override
   public VCloudDirectorAdminClient createClient(Function<HttpRequest, HttpResponse> fn, Module module, Properties props) {
      return apply(createVCloudDirectorContext(fn, module, props));
   }

   @Override
   public VCloudDirectorAdminClient apply(VCloudDirectorContext input) {
      return input.getAdminContext().getApi();
   }

   private VCloudDirectorContext createVCloudDirectorContext(Function<HttpRequest, HttpResponse> fn, Module module,
            Properties props) {
      return VCloudDirectorContext.class.cast(new RestContextFactory(setupRestProperties()).createContext(provider,
               identity, credential, ImmutableSet.<Module> of(new ExpectModule(fn), new NullLoggingModule(), module), props));
   }
}
