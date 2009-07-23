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

//$Id: SystemDeployerService.java 90894 2009-07-07 11:58:40Z thomas.diesler@jboss.com $

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.osgi.spi.service.DeployerService;
import org.jboss.osgi.spi.service.DeploymentRegistryService;
import org.jboss.osgi.spi.util.BundleDeployment;
import org.osgi.framework.BundleContext;

/**
 * A {@link DeployerService} that installs/uninstalls the bundles directly on the OSGi framework without going through the MC registered deployers.
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-May-2009
 */
public class DeploymentRegistryServiceImpl implements DeploymentRegistryService
{
   private Set<BundleDeployment> deployments = new HashSet<BundleDeployment>();

   public DeploymentRegistryServiceImpl(BundleContext context)
   {
   }

   public void registerBundleDeployment(BundleDeployment dep)
   {
      deployments.add(dep);
   }

   public void unregisterBundleDeployment(BundleDeployment dep)
   {
      deployments.remove(dep);
   }
   
   public BundleDeployment getBundleDeployment(String symbolicName, String version)
   {
      if (symbolicName == null)
         throw new IllegalArgumentException("Cannot obtain bundle deployment for null symbolic name");

      BundleDeployment dep = null;
      for (BundleDeployment auxDep : deployments)
      {
         String auxName = auxDep.getSymbolicName();
         String auxVersion = auxDep.getVersion();
         if (symbolicName.equals(auxName) && (version == null || version.equals(auxVersion)))
         {
            dep = auxDep;
            break;
         }
      }

      return dep;
   }

   public BundleDeployment getBundleDeployment(URL url)
   {
      if (url == null)
         throw new IllegalArgumentException("Cannot obtain bundle deployment for: null");

      BundleDeployment dep = null;
      for (BundleDeployment auxDep : deployments)
      {
         if (url.equals(auxDep.getLocation()))
         {
            dep = auxDep;
            break;
         }
      }

      return dep;
   }
}