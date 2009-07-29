/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.common.internal;

//$Id$

import java.net.URL;

import org.jboss.osgi.spi.service.DeployerService;
import org.jboss.osgi.spi.util.BundleDeployment;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * A {@link DeployerService} that delegates to the service that is tracked by the given {@link DeployerServiceTracker}
 * 
 * This delegate is registered as an MBean
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-May-2009
 */
public class DeployerServiceDelegate implements DeployerService
{
   private BundleContext context;

   public DeployerServiceDelegate(BundleContext context)
   {
      this.context = context;
   }

   public void deploy(BundleDeployment[] bundles) throws BundleException
   {
      DeployerService service = getDefaultDeployerService();
      service.deploy(bundles);
   }

   public void deploy(URL url) throws BundleException
   {
      DeployerService service = getDefaultDeployerService();
      service.deploy(url);
   }

   public void undeploy(BundleDeployment[] bundles) throws BundleException
   {
      for (BundleDeployment info : bundles)
         undeploy(info.getLocation());
   }

   public boolean undeploy(URL url) throws BundleException
   {
      boolean undeployed = false;
      
      DeployerService service = getMicrocontainerDeployerService();
      if (service != null)
         undeployed = service.undeploy(url);
      
      if (undeployed == false)
      {
         service = getSystemDeployerService();
         undeployed = service.undeploy(url);
      }
      
      return undeployed;
   }

   private DeployerService getDefaultDeployerService()
   {
      // First try the MC provider
      DeployerService service = getMicrocontainerDeployerService();

      // Fall back to the system provider
      if (service == null)
         service = getSystemDeployerService();

      return service;
   }

   private DeployerService getMicrocontainerDeployerService()
   {
      DeployerService service = null;
      try
      {
         String filter = "(provider=microcontainer)";
         String serviceName = DeployerService.class.getName();
         ServiceReference[] srefs = context.getServiceReferences(serviceName, filter);
         if (srefs != null)
            service = (DeployerService)context.getService(srefs[0]);
      }
      catch (InvalidSyntaxException ex)
      {
         throw new IllegalArgumentException(ex);
      }
      return service;
   }

   private DeployerService getSystemDeployerService()
   {
      DeployerService service = null;
      try
      {
         String filter = "(provider=system)";
         String serviceName = DeployerService.class.getName();
         ServiceReference[] srefs = context.getServiceReferences(serviceName, filter);
         if (srefs != null)
            service = (DeployerService)context.getService(srefs[0]);
      }
      catch (InvalidSyntaxException ex)
      {
         throw new IllegalArgumentException(ex);
      }
      
      if (service == null)
         throw new IllegalStateException("Cannot obtain system DeployerService");
      return service;
   }
}