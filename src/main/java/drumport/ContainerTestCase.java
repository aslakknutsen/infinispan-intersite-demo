/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package drumport;

import javax.inject.Inject;

import junit.framework.Assert;

import org.infinispan.Cache;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.DeploymentTarget;
import org.jboss.arquillian.api.Target;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.dependencies.Dependencies;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Galder Zamarreño
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
@RunWith(Arquillian.class)
public class ContainerTestCase {

   @Deployment(name = "standby-dep", testable = false) @Target("standby")
   public static WebArchive createStandbyDeployment()
   {
      return createStandByArchive();
   }
   
   @Deployment(name = "active-1-dep") @Target("active-1")
   public static WebArchive createTestDeployment()
   {
      return createActiveArchive("active1");
   }

   @Deployment(name = "active-2-dep") @Target("active-2")
   public static WebArchive createTestDeployment2()
   {
      return createActiveArchive("active2");
   }
   
   public static WebArchive createStandByArchive() 
   {
      return ShrinkWrap.create(WebArchive.class, "standby.war")
            .addPackage("drumport.standby")
            .addLibraries(
                  Dependencies.artifact("org.infinispan:infinispan-core:4.2.0.BETA1")
                              .artifact("org.infinispan:infinispan-cachestore-remote:4.2.0.BETA1")
                              .artifact("org.infinispan:infinispan-server-hotrod:4.2.0.BETA1")
                              .artifact("org.jboss.weld.servlet:weld-servlet:1.1.0-SNAPSHOT")
                              .artifact("log4j:log4j:1.2.16").resolve())
            .addWebResource(EmptyAsset.INSTANCE, "beans.xml")
            .addResource("standby-in-container-context.xml", "/META-INF/context.xml")
            .addResource("standby-infinispan.xml", "/WEB-INF/classes/standby-infinispan.xml")
            .setWebXML("standby-in-container-web.xml");
   }

   public static WebArchive createActiveArchive(String name) 
   {
      return ShrinkWrap.create(WebArchive.class, name + ".war")
            .addPackage("drumport.client")
            .addLibraries(
                  Dependencies.artifact("org.infinispan:infinispan-core:4.2.0.BETA1")
                              .artifact("org.infinispan:infinispan-cachestore-remote:4.2.0.BETA1")
                              .artifact("org.jboss.weld.servlet:weld-servlet:1.1.0-SNAPSHOT").resolve())
            .addWebResource(EmptyAsset.INSTANCE, "beans.xml")
            .addResource("in-container-context.xml", "/META-INF/context.xml")
            .addResource("infinispan.xml", "/WEB-INF/classes/infinispan.xml")
            .addResource("hotrod-client.properties", "/WEB-INF/classes/hotrod-client.properties")
            .setWebXML("in-container-web.xml");
   }

   
   @Inject
   private Cache<String, Integer> cache;
   
   @Test @DeploymentTarget("active-1-dep")
   public void callActive1() throws Exception 
   {
      int count = incrementCache(cache);
      Assert.assertEquals(1, count);
   }
   
   @Test @DeploymentTarget("active-2-dep")
   public void callActive2() throws Exception 
   {
      int count = incrementCache(cache);
      Assert.assertEquals(2, count);
   }

   private Integer incrementCache(Cache<String, Integer> cache)
   {
      String key = "counter";
      Integer counter = cache.get(key);
      Integer newCounter;
      if (counter != null)
      {
         newCounter = counter.intValue() + 1;
      }
      else
      {
         newCounter = 1;
      }
      cache.put(key, newCounter);
      return newCounter;
   }
}
