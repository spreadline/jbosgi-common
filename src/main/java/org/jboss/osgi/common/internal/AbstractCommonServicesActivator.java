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

import java.util.Properties;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.StandardMBean;

import org.jboss.osgi.spi.service.DeployerService;
import org.jboss.osgi.spi.service.DeploymentRegistryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The common services activator
 * 
 * @author thomas.diesler@jboss.com
 * @since 23-Jan-2009
 */
public abstract class AbstractCommonServicesActivator
{
   protected ServiceTracker trackLogReaderService(BundleContext context)
   {
      ServiceTracker logTracker = new ServiceTracker(context, LogReaderService.class.getName(), null)
      {
         @Override
         public Object addingService(ServiceReference reference)
         {
            LogReaderService logReader = (LogReaderService)super.addingService(reference);
            logReader.addLogListener(new LogListenerBridge());
            return logReader;
         }
      };
      return logTracker;
   }

   protected DeployerService registerDeployerServices(BundleContext context)
   {
      // Register the DeploymentRegistryService
      DeploymentRegistryService registry = new DeploymentRegistryServiceImpl(context);
      context.registerService(DeploymentRegistryService.class.getName(), registry, null);
      
      // Register the SystemDeployerService
      Properties props = new Properties();
      props.put("provider", "system");
      SystemDeployerService systemDeployer = new SystemDeployerService(context);
      context.registerService(DeployerService.class.getName(), systemDeployer, props);
      
      // Register the DeployerServiceDelegate
      props = new Properties();
      props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
      DeployerService deployerDelegate = new DeployerServiceDelegate(context);
      context.registerService(DeployerService.class.getName(), deployerDelegate, props);
      return deployerDelegate;
   }

   protected void registerDeployerServiceMBean(MBeanServer mbeanServer, DeployerService delegate)
   {
      try
      {
         StandardMBean mbean = new StandardMBean(delegate, DeployerService.class);
         mbeanServer.registerMBean(mbean, DeployerService.MBEAN_DEPLOYER_SERVICE);
      }
      catch (JMException ex)
      {
         throw new IllegalStateException("Cannot register DeployerService MBean", ex);
      }
   }

   protected void unregisterDeployerServiceMBean(MBeanServer mbeanServer)
   {
      try
      {
         if (mbeanServer.isRegistered(DeployerService.MBEAN_DEPLOYER_SERVICE))
            mbeanServer.unregisterMBean(DeployerService.MBEAN_DEPLOYER_SERVICE);
      }
      catch (JMException ex)
      {
         logError("Cannot unregister DeployerService MBean", ex);
      }
   }

   protected abstract void logError(String message, Exception ex);
}