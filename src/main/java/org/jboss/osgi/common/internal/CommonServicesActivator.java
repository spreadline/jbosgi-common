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


import javax.management.MBeanServer;

import org.jboss.osgi.common.log.LogServiceTracker;
import org.jboss.osgi.spi.service.DeployerService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The common services activator
 * 
 * @author thomas.diesler@jboss.com
 * @since 23-Jan-2009
 */
public class CommonServicesActivator extends AbstractCommonServicesActivator implements BundleActivator
{
   private LogServiceTracker logServiceTracker;
   private ServiceTracker logReaderTracker;
   
   @Override
   protected void logError(String message, Exception ex)
   {
      logServiceTracker.log(LogService.LOG_ERROR, message, ex);
   }

   public void start(BundleContext context)
   {
      logServiceTracker = new LogServiceTracker(context);
      
      // Track LogReaderService and add/remove LogListener
      logReaderTracker = trackLogReaderService(context);
      logReaderTracker.open();
      
      // Register the system SystemDeployerService and DeployerServiceDelegate
      DeployerService deployer = registerDeployerServices(context);
      
      // Track the MBeanServer and register the DeployerServiceDelegate
      trackMBeanServer(context, deployer);
   }

   public void stop(BundleContext context)
   {
      if (logServiceTracker != null)
         logServiceTracker.close();
      
      if (logReaderTracker != null)
         logReaderTracker.close();
      
      ServiceReference sref = context.getServiceReference(MBeanServer.class.getName());
      if (sref != null)
      {
         MBeanServer mbeanServer = (MBeanServer)context.getService(sref);
         unregisterDeployerServiceMBean(mbeanServer);
      }
   }
   
   private void trackMBeanServer(BundleContext context, final DeployerService deployer)
   {
      ServiceTracker jmxTracker = new ServiceTracker(context, MBeanServer.class.getName(), null)
      {
         @Override
         public Object addingService(ServiceReference reference)
         {
            MBeanServer mbeanServer = (MBeanServer)super.addingService(reference);
            registerDeployerServiceMBean(mbeanServer, deployer);
            return mbeanServer;
         }

         @Override
         public void removedService(ServiceReference reference, Object service)
         {
            MBeanServer mbeanServer = (MBeanServer)service;
            unregisterDeployerServiceMBean(mbeanServer);
            super.removedService(reference, service);
         }
      };
      jmxTracker.open();
   }
}