/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.perfmsr.core;

public class PerfMsrCorePlugin {
    
    static PerformanceMonitor fgPerformanceMonitor;

    public static IPerformanceMonitor getPerformanceMonitor(boolean shared) {
		PerformanceMonitor pm= null;
		
		if (!shared)
		    pm= PerformanceMonitor.create();
		else {
			if (fgPerformanceMonitor == null)
			    fgPerformanceMonitor= PerformanceMonitor.create();
			pm= fgPerformanceMonitor;
		}
		PerformanceMonitor.debug("PerfMsrCorePlugin.getPerformanceMonitor() returning");
		return pm;
    }
}
