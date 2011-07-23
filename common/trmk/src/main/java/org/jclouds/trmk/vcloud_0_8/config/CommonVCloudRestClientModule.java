/**
 *
 * Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.trmk.vcloud_0_8.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static org.jclouds.Constants.PROPERTY_API_VERSION;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;
import static org.jclouds.trmk.vcloud_0_8.reference.VCloudConstants.PROPERTY_VCLOUD_TIMEOUT_TASK_COMPLETED;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.domain.Location;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.RequiresHttp;
import org.jclouds.http.annotation.ClientError;
import org.jclouds.http.annotation.Redirection;
import org.jclouds.http.annotation.ServerError;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.rest.AsyncClientFactory;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.ConfiguresRestClient;
import org.jclouds.rest.config.RestClientModule;
import org.jclouds.rest.suppliers.MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier;
import org.jclouds.trmk.vcloud_0_8.CommonVCloudAsyncClient;
import org.jclouds.trmk.vcloud_0_8.CommonVCloudClient;
import org.jclouds.trmk.vcloud_0_8.VCloudToken;
import org.jclouds.trmk.vcloud_0_8.VCloudVersionsAsyncClient;
import org.jclouds.trmk.vcloud_0_8.compute.functions.FindLocationForResource;
import org.jclouds.trmk.vcloud_0_8.domain.Catalog;
import org.jclouds.trmk.vcloud_0_8.domain.CatalogItem;
import org.jclouds.trmk.vcloud_0_8.domain.Org;
import org.jclouds.trmk.vcloud_0_8.domain.ReferenceType;
import org.jclouds.trmk.vcloud_0_8.domain.VCloudSession;
import org.jclouds.trmk.vcloud_0_8.domain.VDC;
import org.jclouds.trmk.vcloud_0_8.endpoints.OrgList;
import org.jclouds.trmk.vcloud_0_8.functions.AllCatalogItemsInCatalog;
import org.jclouds.trmk.vcloud_0_8.functions.AllCatalogItemsInOrg;
import org.jclouds.trmk.vcloud_0_8.functions.AllCatalogsInOrg;
import org.jclouds.trmk.vcloud_0_8.functions.AllVDCsInOrg;
import org.jclouds.trmk.vcloud_0_8.functions.OrgsForLocations;
import org.jclouds.trmk.vcloud_0_8.functions.OrgsForNames;
import org.jclouds.trmk.vcloud_0_8.handlers.ParseVCloudErrorFromHttpResponse;
import org.jclouds.trmk.vcloud_0_8.predicates.TaskSuccess;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

/**
 * Configures the VCloud authentication service connection, including logging
 * and http transport.
 * 
 * @author Adrian Cole
 */
@RequiresHttp
@ConfiguresRestClient
public class CommonVCloudRestClientModule<S extends CommonVCloudClient, A extends CommonVCloudAsyncClient> extends
      RestClientModule<S, A> {

   public CommonVCloudRestClientModule(Class<S> syncClientType, Class<A> asyncClientType) {
      super(syncClientType, asyncClientType);
   }

   public CommonVCloudRestClientModule(Class<S> syncClientType, Class<A> asyncClientType,
         Map<Class<?>, Class<?>> delegateMap) {
      super(syncClientType, asyncClientType, delegateMap);
   }

   @Override
   protected void configure() {
      super.configure();
      // Ensures we don't retry on authorization failures
      bind(new TypeLiteral<AtomicReference<AuthorizationException>>() {
      }).toInstance(new AtomicReference<AuthorizationException>());
      installDefaultVCloudEndpointsModule();
      bind(new TypeLiteral<Function<ReferenceType, Location>>() {
      }).to(new TypeLiteral<FindLocationForResource>() {
      });
      bind(new TypeLiteral<Function<Org, Iterable<? extends Catalog>>>() {
      }).to(new TypeLiteral<AllCatalogsInOrg>() {
      });
      bind(new TypeLiteral<Function<Org, Iterable<? extends VDC>>>() {
      }).to(new TypeLiteral<AllVDCsInOrg>() {
      });
      bind(new TypeLiteral<Function<Iterable<String>, Iterable<? extends Org>>>() {
      }).to(new TypeLiteral<OrgsForNames>() {
      });
      bind(new TypeLiteral<Function<Iterable<? extends Location>, Iterable<? extends Org>>>() {
      }).to(new TypeLiteral<OrgsForLocations>() {
      });
      bind(new TypeLiteral<Function<Catalog, Iterable<? extends CatalogItem>>>() {
      }).to(new TypeLiteral<AllCatalogItemsInCatalog>() {
      });
      bind(new TypeLiteral<Function<Org, Iterable<? extends CatalogItem>>>() {
      }).to(new TypeLiteral<AllCatalogItemsInOrg>() {
      });
   }

   @Singleton
   @Provides
   CommonVCloudAsyncClient provideCommonVCloudAsyncClient(A in) {
      return in;
   }

   @Singleton
   @Provides
   CommonVCloudClient provideCommonVCloudClient(S in) {
      return in;
   }

   @Provides
   @Singleton
   @org.jclouds.trmk.vcloud_0_8.endpoints.VDC
   protected Supplier<Map<String, String>> provideVDCtoORG(Supplier<Map<String, ? extends Org>> orgNameToOrgSuppier) {
      return Suppliers.compose(new Function<Map<String, ? extends Org>, Map<String, String>>() {

         @Override
         public Map<String, String> apply(Map<String, ? extends Org> arg0) {
            Builder<String, String> returnVal = ImmutableMap.<String, String> builder();
            for (Entry<String, ? extends Org> orgr : arg0.entrySet()) {
               for (String vdc : orgr.getValue().getVDCs().keySet()) {
                  returnVal.put(vdc, orgr.getKey());
               }
            }
            return returnVal.build();
         }
      }, orgNameToOrgSuppier);

   }

   @Provides
   @Singleton
   protected Supplier<Map<String, ? extends Org>> provideOrgMapCache(@Named(PROPERTY_SESSION_INTERVAL) long seconds,
         AtomicReference<AuthorizationException> authException, OrgMapSupplier supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<String, ? extends Org>>(
            authException, seconds, supplier);
   }

   @Provides
   @Singleton
   @OrgList
   URI provideOrgListURI(Supplier<VCloudSession> sessionSupplier) {
      VCloudSession session = sessionSupplier.get();
      return URI.create(getLast(session.getOrgs().values()).getHref().toASCIIString().replaceAll("org/.*", "org"));
   }

   @Singleton
   public static class OrgMapSupplier implements Supplier<Map<String, ? extends Org>> {
      protected final Supplier<VCloudSession> sessionSupplier;
      protected final Function<Iterable<String>, Iterable<? extends Org>> organizationsForNames;

      @Inject
      protected OrgMapSupplier(Supplier<VCloudSession> sessionSupplier,
            Function<Iterable<String>, Iterable<? extends Org>> organizationsForNames) {
         this.sessionSupplier = sessionSupplier;
         this.organizationsForNames = organizationsForNames;
      }

      @Override
      public Map<String, ? extends Org> get() {
         return uniqueIndex(organizationsForNames.apply(sessionSupplier.get().getOrgs().keySet()), name);
      }
   }

   protected void installDefaultVCloudEndpointsModule() {
      install(new DefaultVCloudReferencesModule());
   }

   @Singleton
   public static class OrgCatalogSupplier implements
         Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>> {
      protected final Supplier<Map<String, ? extends Org>> orgSupplier;
      protected final Function<Org, Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>> allCatalogsInOrg;

      @Inject
      protected OrgCatalogSupplier(Supplier<Map<String, ? extends Org>> orgSupplier,
            Function<Org, Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>> allCatalogsInOrg) {
         this.orgSupplier = orgSupplier;
         this.allCatalogsInOrg = allCatalogsInOrg;
      }

      @Override
      public Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>> get() {
         return transformValues(
               transformValues(orgSupplier.get(), allCatalogsInOrg),
               new Function<Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>() {

                  @Override
                  public Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog> apply(
                        Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog> from) {
                     return uniqueIndex(from, name);
                  }

               });
      }
   }

   @VCloudToken
   @Provides
   String provideVCloudToken(Supplier<VCloudSession> cache) {
      return checkNotNull(cache.get().getVCloudToken(), "No token present in session");
   }

   @Provides
   @org.jclouds.trmk.vcloud_0_8.endpoints.Org
   @Singleton
   protected Supplier<Map<String, ReferenceType>> provideVDCtoORG(@Named(PROPERTY_SESSION_INTERVAL) long seconds,
         AtomicReference<AuthorizationException> authException, OrgNameToOrgSupplier supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<String, ReferenceType>>(
            authException, seconds, supplier);
   }

   @Provides
   @Singleton
   protected Supplier<Map<URI, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>> provideURIToVDC(
         @Named(PROPERTY_SESSION_INTERVAL) long seconds, AtomicReference<AuthorizationException> authException,
         URItoVDC supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<URI, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>(
            authException, seconds, supplier);
   }

   @Singleton
   public static class URItoVDC implements Supplier<Map<URI, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>> {
      private final Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>> orgVDCMap;

      @Inject
      URItoVDC(Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>> orgVDCMap) {
         this.orgVDCMap = orgVDCMap;
      }

      @Override
      public Map<URI, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC> get() {
         return uniqueIndex(
               concat(transform(
                     orgVDCMap.get().values(),
                     new Function<Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>, Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>() {

                        @Override
                        public Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC> apply(
                              Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC> from) {
                           return from.values();
                        }

                     })), new Function<org.jclouds.trmk.vcloud_0_8.domain.VDC, URI>() {

                  @Override
                  public URI apply(org.jclouds.trmk.vcloud_0_8.domain.VDC from) {
                     return from.getHref();
                  }

               });
      }

   }

   final static Function<ReferenceType, String> name = new Function<ReferenceType, String>() {

      @Override
      public String apply(ReferenceType from) {
         return from.getName();
      }

   };

   @Provides
   @Singleton
   @org.jclouds.trmk.vcloud_0_8.endpoints.VCloudLogin
   protected URI provideAuthenticationURI(VCloudVersionsAsyncClient versionService,
         @Named(PROPERTY_API_VERSION) String version) throws InterruptedException, ExecutionException, TimeoutException {
      SortedMap<String, URI> versions = versionService.getSupportedVersions().get(180, TimeUnit.SECONDS);
      checkState(versions.size() > 0, "No versions present");
      checkState(versions.containsKey(version), "version " + version + " not present in: " + versions);
      return versions.get(version);
   }

   @Singleton
   private static class OrgNameToOrgSupplier implements Supplier<Map<String, ReferenceType>> {
      private final Supplier<VCloudSession> sessionSupplier;

      @SuppressWarnings("unused")
      @Inject
      OrgNameToOrgSupplier(Supplier<VCloudSession> sessionSupplier) {
         this.sessionSupplier = sessionSupplier;
      }

      @Override
      public Map<String, ReferenceType> get() {
         return sessionSupplier.get().getOrgs();
      }

   }

   @Provides
   @Singleton
   protected VCloudVersionsAsyncClient provideVCloudVersions(AsyncClientFactory factory) {
      return factory.create(VCloudVersionsAsyncClient.class);
   }

   @Provides
   @Singleton
   protected Org provideOrg(Supplier<Map<String, ? extends Org>> orgSupplier,
         @org.jclouds.trmk.vcloud_0_8.endpoints.Org ReferenceType defaultOrg) {
      return orgSupplier.get().get(defaultOrg.getName());
   }

   @Provides
   @Singleton
   protected Predicate<URI> successTester(Injector injector,
         @Named(PROPERTY_VCLOUD_TIMEOUT_TASK_COMPLETED) long completed) {
      return new RetryablePredicate<URI>(injector.getInstance(TaskSuccess.class), completed);
   }

   @Provides
   @Singleton
   protected Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>> provideOrgCatalogItemMapSupplierCache(
         @Named(PROPERTY_SESSION_INTERVAL) long seconds, AtomicReference<AuthorizationException> authException,
         OrgCatalogSupplier supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>>(
            authException, seconds, supplier);
   }
   
   @Provides
   @Singleton
   protected Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>> provideOrgVDCSupplierCache(
         @Named(PROPERTY_SESSION_INTERVAL) long seconds, AtomicReference<AuthorizationException> authException,
         OrgVDCSupplier supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>>(
            authException, seconds, supplier);
   }

   @Singleton
   public static class OrgVDCSupplier implements
         Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>> {
      protected final Supplier<Map<String, ? extends Org>> orgSupplier;
      private final Function<Org, Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>> allVDCsInOrg;

      @Inject
      protected OrgVDCSupplier(Supplier<Map<String, ? extends Org>> orgSupplier,
            Function<Org, Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>> allVDCsInOrg) {
         this.orgSupplier = orgSupplier;
         this.allVDCsInOrg = allVDCsInOrg;
      }

      @Override
      public Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>> get() {
         return transformValues(
               transformValues(orgSupplier.get(), allVDCsInOrg),
               new Function<Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC>>() {

                  @Override
                  public Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.VDC> apply(
                        Iterable<? extends org.jclouds.trmk.vcloud_0_8.domain.VDC> from) {
                     return uniqueIndex(from, name);
                  }

               });
      }
   }

   @Singleton
   public static class OrgCatalogItemSupplier implements
         Supplier<Map<String, Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>>> {
      protected final Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>> catalogSupplier;
      protected final Function<org.jclouds.trmk.vcloud_0_8.domain.Catalog, Iterable<? extends CatalogItem>> allCatalogItemsInCatalog;

      @Inject
      protected OrgCatalogItemSupplier(
            Supplier<Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>>> catalogSupplier,
            Function<org.jclouds.trmk.vcloud_0_8.domain.Catalog, Iterable<? extends CatalogItem>> allCatalogItemsInCatalog) {
         this.catalogSupplier = catalogSupplier;
         this.allCatalogItemsInCatalog = allCatalogItemsInCatalog;
      }

      @Override
      public Map<String, Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>> get() {
         return transformValues(
               catalogSupplier.get(),
               new Function<Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog>, Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>>() {

                  @Override
                  public Map<String, Map<String, ? extends CatalogItem>> apply(
                        Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.Catalog> from) {
                     return transformValues(
                           from,
                           new Function<org.jclouds.trmk.vcloud_0_8.domain.Catalog, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>() {

                              @Override
                              public Map<String, ? extends CatalogItem> apply(org.jclouds.trmk.vcloud_0_8.domain.Catalog from) {
                                 return uniqueIndex(allCatalogItemsInCatalog.apply(from), name);
                              }
                           });

                  }
               });
      }
   }

   @Provides
   @Singleton
   protected Supplier<Map<String, Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>>> provideOrgCatalogItemSupplierCache(
         @Named(PROPERTY_SESSION_INTERVAL) long seconds, AtomicReference<AuthorizationException> authException,
         OrgCatalogItemSupplier supplier) {
      return new MemoizedRetryOnTimeOutButNotOnAuthorizationExceptionSupplier<Map<String, Map<String, Map<String, ? extends org.jclouds.trmk.vcloud_0_8.domain.CatalogItem>>>>(
            authException, seconds, supplier);
   }

   @Override
   protected void bindErrorHandlers() {
      bind(HttpErrorHandler.class).annotatedWith(Redirection.class).to(ParseVCloudErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ClientError.class).to(ParseVCloudErrorFromHttpResponse.class);
      bind(HttpErrorHandler.class).annotatedWith(ServerError.class).to(ParseVCloudErrorFromHttpResponse.class);
   }
}