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
package org.eclipse.test.internal.performance;

import java.util.Map;

import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Scalar;


class PerformanceMonitor {
    
    private static PerformanceMonitor fgPerformanceMonitor;

    public static PerformanceMonitor getPerformanceMonitor() {
		if (fgPerformanceMonitor == null) {
		    String os= System.getProperty("os.name"); //$NON-NLS-1$
		    if (os.startsWith("Windows")) //$NON-NLS-1$
		        fgPerformanceMonitor= new PerformanceMonitorWindows();
		    else if (os.startsWith("Mac OS X")) //$NON-NLS-1$
                fgPerformanceMonitor= new PerformanceMonitorMac();
            else
                fgPerformanceMonitor= new PerformanceMonitorLinux();
		}
		return fgPerformanceMonitor;
    }

    protected void collectOperatingSystemCounters(Map scalars) {
        // default implementation
        addScalar(scalars, InternalDimensions.SYSTEM_TIME, System.currentTimeMillis());
        /*
    	Runtime runtime= Runtime.getRuntime();
		runtime.gc();
		long used= runtime.totalMemory() - runtime.freeMemory();
		addScalar(scalars, Dimensions.USED_JAVA_HEAP, used);
		*/
    }

	protected void collectGlobalPerformanceInfo(Map scalars) {
		// no default implementation
	}
	
    void addScalar(Map scalars, Dim dimension, long value) {
        scalars.put(dimension, new Scalar(dimension, value));
    }
}
