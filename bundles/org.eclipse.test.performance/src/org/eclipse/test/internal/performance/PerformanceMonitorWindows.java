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

import org.eclipse.test.internal.performance.PerformanceMonitor;
import org.eclipse.test.internal.performance.LoadValueConstants;

/**
 * The PerformanceMonitor for Windows.
 */
class PerformanceMonitorWindows extends PerformanceMonitor {
    		
	
	private boolean fgNativeGetPerformanceInfoNotAvailable;

 	/**
	 * Write out operating system counters for Windows.
	 * Answer some performance counters.
	 * 
	 * @param counters the results are returned in this array.
	 * <ol>
	 * <li>working set in bytes for this process
	 * <li>peak working set in bytes for this process
	 * <li>elapsed time in milliseconds
	 * <li>user time in milliseconds
	 * <li>kernel time in milliseconds
	 * <li>page faults for the process
	 * <li>commit charge total in bytes (working set for the entire machine). On some 
	 * machines we have problems getting this value so we return -1 in that case.
	 * <li>number of GDI objects in the process
	 * <li>number of USER objects in the process
	 * <li>number of open handles in the process. returns -1 if this information is not available
	 * <li>Number of read operations
	 * <li>Number of write operations
	 * <li>Number of bytes read
	 * <li>Number of bytes written
	 * </ol>
	 * 
	 * @return true if the function returned valid results and false if there was
	 * some sort of problem. This method can also throw a Runtime exception if
	 * any of the windows API calls returns an error.
	 */
    protected void collectOperatingSystemCounters(Map scalars) {
		synchronized(this) {
			if (!org.eclipse.perfmsr.core.PerformanceMonitor.isLoaded())
			    return;
			
			long[] counters= new long[14];
			if (org.eclipse.perfmsr.core.PerformanceMonitor.nativeGetPerformanceCounters(counters)) {
				addScalar(scalars, 4, "Working Set", counters[0]);
				addScalar(scalars, 8, "Working Set Peak", counters[1]);
				addScalar(scalars, 9, "Elapsed Process", counters[2]);
				addScalar(scalars, 10, "User time", counters[3]);
				addScalar(scalars, 11, "Kernel time", counters[4]);
				addScalar(scalars, 19, "Page Faults", counters[5]);
				if (counters[6] != -1)
					addScalar(scalars, 7, "Committed", counters[6]);
				addScalar(scalars, 34, "GDI Objects", counters[7]);
				addScalar(scalars, 35, "USER Objects", counters[8]);
				if (counters[9] != -1)
					addScalar(scalars, 36, "Open Handles", counters[9]);
				addScalar(scalars, 38, "Read Count", counters[10]);
				addScalar(scalars, 39, "Write Count", counters[11]);
				addScalar(scalars, 40, "Bytes Read", counters[12]);
				addScalar(scalars, 41, "Bytes Written", counters[13]);
                addScalar(scalars, LoadValueConstants.What.cpuTime, "CPU Time", counters[3] + counters[4]);
			}
		}
    }

	/**
	 * Write out the global performance info. This includes things like the total
	 * committed memory for the entire system.
	 * 
	 * This function depends on the GetPerformanceInfo() function being available in
	 * the Windows psapi.dll. This is available in XP but is usually not available
	 * in Win/2000. If it is not available then this function throws an UnsupportedOperationException.
	 */
	protected void collectGlobalPerformanceInfo(Map scalars) {
		if (fgNativeGetPerformanceInfoNotAvailable)
			return;
		synchronized(this) {
			if (!org.eclipse.perfmsr.core.PerformanceMonitor.isLoaded())
				return;
			
	    	long[] counters= new long[13];
			try {
				org.eclipse.perfmsr.core.PerformanceMonitor.nativeGetPerformanceInfo(counters);
				long pageSize= counters[9];
				addScalar(scalars, 37, "Commit Total", counters[0]*pageSize);
				addScalar(scalars, 22, "Commit Limit", counters[1]*pageSize); 
				addScalar(scalars, 23, "Commit Peak", counters[2]*pageSize);
				addScalar(scalars, 24, "Physical Total", counters[3]*pageSize); 
				addScalar(scalars, 25, "Physical Available", counters[4]*pageSize); 
				addScalar(scalars, 26, "System Cache", counters[5]*pageSize);
				addScalar(scalars, 27, "Kernel Total", counters[6]*pageSize); 
				addScalar(scalars, 28, "Kernel Paged", counters[7]*pageSize); 
				addScalar(scalars, 29, "Kernel Nonpaged", counters[8]*pageSize); 
				addScalar(scalars, 30, "Page Size", counters[9]);
				addScalar(scalars, 31, "Handle Count", counters[10]); 
				addScalar(scalars, 32, "Process Count", counters[11]); 
				addScalar(scalars, 33, "Thread Count", counters[12]);
			} catch (Exception e) {
			    System.err.println("Exception in collectGlobalPerformanceInfo: " + e.toString());
				fgNativeGetPerformanceInfoNotAvailable= true;
			}
		}
	}
	
	protected String getUUID() {
		if (org.eclipse.perfmsr.core.PerformanceMonitor.isLoaded()) {
			try {
				return org.eclipse.perfmsr.core.PerformanceMonitor.nativeGetUUID();
			} catch (Exception e) {
			}
		}
		return super.getUUID();
	}
}
