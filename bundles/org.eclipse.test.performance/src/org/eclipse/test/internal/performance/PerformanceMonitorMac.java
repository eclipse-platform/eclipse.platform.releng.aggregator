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

/**
 * The Mac OS X version of the performance monitor.
 * (Uses default implementation for now).
 */
public class PerformanceMonitorMac extends BasePerformanceMonitor {
    
	/**
	 * Write out operating system counters for Mac OS X.
	 */
	protected void collectOperatingSystemCounters(Map scalars) {
	    super.collectOperatingSystemCounters(scalars);
	}

	/**
	 * Write out the global machine counters for Mac OS X.
	 */
	protected void collectGlobalPerformanceInfo(Map scalars) {
	    super.collectGlobalPerformanceInfo(scalars);
	}
}
