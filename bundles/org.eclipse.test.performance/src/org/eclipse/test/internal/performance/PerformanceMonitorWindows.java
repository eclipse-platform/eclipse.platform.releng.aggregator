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
				addScalar(scalars, Dimensions.WORKING_SET, counters[0]);
				addScalar(scalars, Dimensions.WORKING_SET_PEAK, counters[1]);
				addScalar(scalars, Dimensions.ELAPSED_PROCESS, counters[2]);
				addScalar(scalars, Dimensions.USER_TIME, counters[3]);
				addScalar(scalars, Dimensions.KERNEL_TIME, counters[4]);
				addScalar(scalars, Dimensions.PAGE_FAULTS, counters[5]);
				if (counters[6] != -1)
					addScalar(scalars, Dimensions.COMITTED, counters[6]);
				addScalar(scalars, Dimensions.GDI_OBJECTS, counters[7]);
				addScalar(scalars, Dimensions.USER_OBJECTS, counters[8]);
				if (counters[9] != -1)
					addScalar(scalars, Dimensions.OPEN_HANDLES, counters[9]);
				addScalar(scalars, Dimensions.READ_COUNT, counters[10]);
				addScalar(scalars, Dimensions.WRITE_COUNT, counters[11]);
				addScalar(scalars, Dimensions.BYTES_READ, counters[12]);
				addScalar(scalars, Dimensions.BYTES_WRITTEN, counters[13]);
                addScalar(scalars, Dimensions.CPU_TIME, counters[3] + counters[4]);
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
				addScalar(scalars, Dimensions.COMMIT_TOTAL, counters[0]*pageSize);
				addScalar(scalars, Dimensions.COMMIT_LIMIT, counters[1]*pageSize); 
				addScalar(scalars, Dimensions.COMMIT_PEAK, counters[2]*pageSize);
				addScalar(scalars, Dimensions.PHYSICAL_TOTAL, counters[3]*pageSize); 
				addScalar(scalars, Dimensions.PHYSICAL_AVAIL, counters[4]*pageSize); 
				addScalar(scalars, Dimensions.SYSTEM_CACHE, counters[5]*pageSize);
				addScalar(scalars, Dimensions.KERNEL_TOTAL, counters[6]*pageSize); 
				addScalar(scalars, Dimensions.KERNEL_PAGED, counters[7]*pageSize); 
				addScalar(scalars, Dimensions.KERNEL_NONPAGED, counters[8]*pageSize); 
				addScalar(scalars, Dimensions.PAGE_SIZE, counters[9]);
				addScalar(scalars, Dimensions.HANDLE_COUNT, counters[10]); 
				addScalar(scalars, Dimensions.PROCESS_COUNT, counters[11]); 
				addScalar(scalars, Dimensions.THREAD_COUNT, counters[12]);
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
