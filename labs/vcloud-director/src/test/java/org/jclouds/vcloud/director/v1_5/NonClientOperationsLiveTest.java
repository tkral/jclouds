/*
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
package org.jclouds.vcloud.director.v1_5;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.jclouds.crypto.CryptoStreams;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.util.Strings2;
import org.jclouds.vcloud.director.v1_5.domain.OrgList;
import org.jclouds.vcloud.director.v1_5.domain.SessionWithToken;
import org.jclouds.vcloud.director.v1_5.features.admin.AdminCatalogClient;
import org.jclouds.vcloud.director.v1_5.internal.BaseVCloudDirectorClientLiveTest;
import org.jclouds.xml.internal.JAXBParser;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;

/**
 * Tests live behavior of {@link AdminCatalogClient}.
 * 
 * @author danikov
 */
@Test(groups = { "live", "user", "nonClient" }, singleThreaded = true, testName = "NonClientOperationsLiveTest")
public class NonClientOperationsLiveTest extends BaseVCloudDirectorClientLiveTest {
   
   private JAXBParser parser = new JAXBParser();
   private SessionWithToken sessionWithToken;

   @Override
   protected void setupRequiredClients() throws Exception {
      setupCredentials();
   }
   
   @Test(testName = "POST /login")
   public void testPostLogin() throws IOException {
      testLoginWithMethod("POST");
   }
   
   @Test(testName = "GET /login")
   public void testGetLogin() throws IOException {
      testLoginWithMethod("GET");
   }
   
   private void testLoginWithMethod(String method) throws IOException {
      String user = identity.substring(0, identity.lastIndexOf('@'));
      String org = identity.substring(identity.lastIndexOf('@') + 1);
      String password = credential;
      
      String authHeader = "Basic " + CryptoStreams.base64(String.format("%s@%s:%s", 
                  checkNotNull(user),
                  checkNotNull(org),
                  checkNotNull(password)).getBytes("UTF-8"));
      
      HttpResponse response = context.getUtils().getHttpClient().invoke(HttpRequest.builder()
         .method(method)
         .endpoint(URI.create(endpoint+"/login"))
         .headers(ImmutableMultimap.of(
            "Authorization", authHeader,
            "Accept", "*/*"))
         .build());
      
      sessionWithToken = SessionWithToken.builder()
         .session(session)
         .token(response.getFirstHeaderOrNull("x-vcloud-authorization"))
      .build();
      
      assertEquals(sessionWithToken.getSession().getUser(), user);
      assertEquals(sessionWithToken.getSession().getOrg(), org);
      assertTrue(sessionWithToken.getSession().getLinks().size() > 0);
      assertNotNull(sessionWithToken.getToken());
      
      OrgList orgList = parser.fromXML(
            Strings2.toStringAndClose(response.getPayload().getInput()), OrgList.class);
      
      assertTrue(orgList.getOrgs().size() > 0, "must have orgs");
      
      context.getApi().getOrgClient().getOrg(Iterables.getLast(orgList.getOrgs()).getHref());
   }
   
   @Test(testName = "GET /schema/{schemaFileName}", 
         dependsOnMethods = {"testPostLogin", "testGetLogin"} )
   public void testGetSchema() throws IOException {
      String schemafileName = "master.xsd";
      HttpResponse response = context.getUtils().getHttpClient().invoke(HttpRequest.builder()
         .method("GET")
         .endpoint(URI.create(endpoint+"/v1.5/schema/"+schemafileName))
         .headers(ImmutableMultimap.of(
            "x-vcloud-authorization", sessionWithToken.getToken(),
            "Accept", "*/*"))
         .build());
      
      String schema = Strings2.toStringAndClose(response.getPayload().getInput());
      
      // TODO: asserting something about the schema
   }
}
