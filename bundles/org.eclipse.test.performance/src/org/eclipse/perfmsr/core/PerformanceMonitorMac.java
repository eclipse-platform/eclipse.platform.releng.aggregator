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

import java.util.Map;

/**
 * The Mac OS X version of the performance monitor.
 */
public class PerformanceMonitorMac extends BasePerformanceMonitor {
    
	/**
	 * Write out operating system counters for Mac OS X.
	 */
	protected void writeOperatingSystemCounters(Map scalars) {
	    super.writeOperatingSystemCounters(scalars);
	}

	/**
	 * Write out the global machine counters for Mac OS X.
	 */
	protected void writeGlobalPerformanceInfo(Map scalars) {
	    super.writeGlobalPerformanceInfo(scalars);
	}
}
