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
public class PerformanceMonitorMac extends PerformanceMonitor {
    
	/**
	 * Give your subclasses a chance to override this method.
	 */
	protected void writeOperatingSystemCounters(Map scalars) {
	    super.writeOperatingSystemCounters(scalars);
	}

	/**
	 * Write out the global machine counters for Linux.
	 * 
	 * @param step
	 * @param displayResults
	 * @param b a buffer that is being built up when the results are to be displayed.
	 */
	protected void writeGlobalPerformanceInfo(int step, boolean displayResults, StringBuffer b)
	{
		if (1==2)step=step+0;	// get rid of the unused warning
//		LinuxMemInfo mi = LinuxMemInfo.create();
//		writeValue(LoadValueConstants.What.physicalMemory, mi.total, "Physical Memory", formatEng(mi.total), displayResults, b);
//		writeValue(LoadValueConstants.What.usedLinuxMemory, mi.used, "Used Memory", formatEng(mi.used), displayResults, b);
//		writeValue(LoadValueConstants.What.freeLinuxMemory, mi.free, "Free Memory", formatEng(mi.free), displayResults, b);
//		writeValue(LoadValueConstants.What.buffersLinux, mi.buffers, "Buffers Memory", formatEng(mi.buffers), displayResults, b);
//		writeValue(LoadValueConstants.What.systemCache, mi.cache, "System Cache", formatEng(mi.cache), displayResults, b);
	}
}
