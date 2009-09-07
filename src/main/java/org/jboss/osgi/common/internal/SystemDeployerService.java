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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.osgi.common.log.LogServiceTracker;
import org.jboss.osgi.spi.logging.ExportedPackageHelper;
import org.jboss.osgi.spi.management.ManagedBundleService;
import org.jboss.osgi.spi.service.DeployerService;
import org.jboss.osgi.spi.service.DeploymentRegistryService;
import org.jboss.osgi.spi.util.BundleDeployment;
import org.jboss.osgi.spi.util.BundleDeploymentFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A {@link DeployerService} that installs/uninstalls the bundles directly on the OSGi framework without going through the MC registered deployers.
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-May-2009
 */
public class SystemDeployerService implements DeployerService
{
   private LogServiceTracker log;
   private BundleContext context;
   private ServiceTracker registryTracker;
   private ServiceTracker startLevelTracker;

   public SystemDeployerService(BundleContext context)
   {
      this.log = new LogServiceTracker(context);
      this.context = context;
   }

   public void deploy(BundleDeployment[] depArr) throws BundleException
   {
      DeploymentRegistryService registry = getDeploymentRegistry();

      // Install the bundles
      Map<BundleDeployment, Bundle> bundleMap = new HashMap<BundleDeployment, Bundle>();
      for (BundleDeployment dep : depArr)
      {
         try
         {
            String location = dep.getLocation().toExternalForm();
            Bundle bundle = context.installBundle(location);
            log.log(LogService.LOG_INFO, "Installed: " + bundle);

            registerManagedBundle(bundle);
            bundleMap.put(dep, bundle);

            registry.registerBundleDeployment(dep);
         }
         catch (BundleException ex)
         {
            log.log(LogService.LOG_ERROR, "Cannot install bundle: " + dep, ex);
         }
      }

      // Resolve the bundles through the PackageAdmin
      PackageAdmin packageAdmin = null;
      ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
      if (sref != null)
      {
         packageAdmin = (PackageAdmin)context.getService(sref);
         
         Collection<Bundle> bundles = bundleMap.values();
         Bundle[] bundleArr = new Bundle[bundles.size()];
         bundles.toArray(bundleArr);
         
         packageAdmin.resolveBundles(bundleArr);
      }
      
      // Start the installed bundles
      for (BundleDeployment dep : depArr)
      {
         Bundle bundle = bundleMap.get(dep);

         StartLevel startLevel = getStartLevel();
         if (dep.getStartLevel() > 0)
         {
            startLevel.setBundleStartLevel(bundle, dep.getStartLevel());
         }

         if (dep.isAutoStart())
         {
            int state = bundle.getState();
            if (state == Bundle.RESOLVED || packageAdmin == null)
            {
               try
               {
                  log.log(LogService.LOG_DEBUG, "Start: " + bundle);

                  // Added support for Bundle.START_ACTIVATION_POLICY on start
                  // http://issues.apache.org/jira/browse/FELIX-1317
                  // bundle.start(Bundle.START_ACTIVATION_POLICY);

                  bundle.start();

                  log.log(LogService.LOG_INFO, "Started: " + bundle);
                  ExportedPackageHelper packageHelper = new ExportedPackageHelper(context);
                  packageHelper.logExportedPackages(bundle);
               }
               catch (BundleException ex)
               {
                  log.log(LogService.LOG_ERROR, "Cannot start bundle: " + bundle, ex);
               }
            }
            else
            {
               log.log(LogService.LOG_INFO, "Cannot start unresolved bundle: " + bundle);
            }
         }
      }
   }

   public void undeploy(BundleDeployment[] depArr) throws BundleException
   {
      DeploymentRegistryService registry = getDeploymentRegistry();

      for (BundleDeployment dep : depArr)
      {
         Bundle bundle = getBundle(dep);
         if (bundle != null)
         {
            registry.unregisterBundleDeployment(dep);

            unregisterManagedBundle(bundle);
            bundle.uninstall();
            log.log(LogService.LOG_INFO, "Uninstalled: " + bundle);
         }
         else
         {
            log.log(LogService.LOG_WARNING, "Cannot obtain bundle for: " + dep);
         }
      }
   }

   public void deploy(URL url) throws BundleException
   {
      BundleDeployment dep = BundleDeploymentFactory.createBundleDeployment(url);
      deploy(new BundleDeployment[] { dep });
   }

   public boolean undeploy(URL url) throws BundleException
   {
      DeploymentRegistryService registry = getDeploymentRegistry();
      BundleDeployment dep = registry.getBundleDeployment(url);
      if (dep != null)
      {
         undeploy(new BundleDeployment[] { dep });
         return true;
      }
      else
      {
         log.log(LogService.LOG_WARNING, "Cannot find deployment for: " + url);
         return false;
      }
   }

   private Bundle getBundle(BundleDeployment dep)
   {
      String symbolicName = dep.getSymbolicName();
      Version version = dep.getVersion();

      Bundle bundle = null;
      for (Bundle aux : context.getBundles())
      {
         if (aux.getSymbolicName().equals(symbolicName))
         {
            Version auxVersion = aux.getVersion();
            if (version.equals(auxVersion))
            {
               bundle = aux;
               break;
            }
         }
      }
      return bundle;
   }

   private void registerManagedBundle(Bundle bundle)
   {
      ServiceReference sref = context.getServiceReference(ManagedBundleService.class.getName());
      if (sref != null)
      {
         ManagedBundleService service = (ManagedBundleService)context.getService(sref);
         service.register(bundle);
      }
      else
      {
         log.log(LogService.LOG_DEBUG, "No ManagedBundleService. Cannot register managed bundle: " + bundle);
      }
   }

   private void unregisterManagedBundle(Bundle bundle)
   {
      ServiceReference sref = context.getServiceReference(ManagedBundleService.class.getName());
      if (sref != null)
      {
         ManagedBundleService service = (ManagedBundleService)context.getService(sref);
         service.unregister(bundle);
      }
      else
      {
         log.log(LogService.LOG_DEBUG, "No ManagedBundleService. Cannot unregister managed bundle: " + bundle);
      }
   }

   private DeploymentRegistryService getDeploymentRegistry()
   {
      if (registryTracker == null)
      {
         registryTracker = new ServiceTracker(context, DeploymentRegistryService.class.getName(), null);
         registryTracker.open();
      }
      return (DeploymentRegistryService)registryTracker.getService();
   }

   private StartLevel getStartLevel()
   {
      if (startLevelTracker == null)
      {
         startLevelTracker = new ServiceTracker(context, StartLevel.class.getName(), null);
         startLevelTracker.open();
      }
      return (StartLevel)startLevelTracker.getService();
   }
}