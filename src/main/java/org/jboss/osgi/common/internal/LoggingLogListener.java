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

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LogListener that logs LogEntrys to SLF4J.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Mar-2009
 */
public class LoggingLogListener implements LogListener
{
   public void logged(LogEntry entry)
   {
      Bundle bundle = entry.getBundle();
      int level = entry.getLevel();
      Throwable throwable = entry.getException();

      String loggerName = bundle.getSymbolicName();
      Logger log = LoggerFactory.getLogger(loggerName);

      if (level == LogService.LOG_DEBUG)
         log.debug(entry.getMessage(), throwable);

      else if (level == LogService.LOG_INFO)
         log.info(entry.getMessage(), throwable);

      else if (level == LogService.LOG_WARNING)
         log.warn(entry.getMessage(), throwable);

      else if (level == LogService.LOG_ERROR)
         log.error(entry.getMessage(), throwable);
   }
}