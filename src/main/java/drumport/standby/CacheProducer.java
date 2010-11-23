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
package drumport.standby;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.Marshaller;
import org.infinispan.marshall.VersionAwareMarshaller;
import org.infinispan.server.hotrod.HotRodServer;

/**
 * Produces and Disposes EmbeddedCacheManager, Cache<Stirng, Integer> and HotRodServer
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@ApplicationScoped
public class CacheProducer
{
   //@Resource(name = "name")
   private String configName = "standby-infinispan.xml";
   
   //@Resource(name = "infinispan.server.host")
   private String serverHost = "127.0.0.1";

   //@Resource(name = "infinispan.server.port")
   private String serverPort = "11311";

   private HotRodServer server;
   
   private EmbeddedCacheManager cacheManager;
   
   @PostConstruct
   public void create() throws Exception
   {
      cacheManager = createCacheManager();
      server = createServer(cacheManager);
   }
   
   @PreDestroy
   public void destroy() 
   {
      distroyCacheManager(cacheManager);
      distroyServer(server);
   }
   
   public EmbeddedCacheManager createCacheManager() throws Exception
   {
      return new DefaultCacheManager(configName);
   }
   
   public void distroyCacheManager(EmbeddedCacheManager manager)
   {
      manager.stop();
   }
   
   public HotRodServer createServer(EmbeddedCacheManager manager) 
   {
      HotRodServer server = new HotRodServer();
      server.start(getProperties(), manager);
      return server;
   }
   
   public void distroyServer(HotRodServer server)
   {
      server.stop();
   }
   
   private Properties getProperties() {
      Properties p = new Properties();
      p.setProperty("infinispan.server.host", serverHost);
      p.setProperty("infinispan.server.port", serverPort);
      return p;
   }
   
   @Produces @ApplicationScoped
   public Marshaller createMarshaller()
   {
      VersionAwareMarshaller marshaller = new VersionAwareMarshaller();
      marshaller.inject(Thread.currentThread().getContextClassLoader(), null);
      marshaller.start();
      return marshaller;
   }
   
   public void distroyMarshaller(@Disposes Marshaller marshaller)
   {
      ((VersionAwareMarshaller)marshaller).stop();
   }
   
   @Produces @ApplicationScoped
   public Cache<String, Integer> getCache() 
   {
      return cacheManager.getCache();
   }
   
   public void distroyCache(@Disposes Cache<String, Integer> cache)
   {
      cache.stop();
   }

}
