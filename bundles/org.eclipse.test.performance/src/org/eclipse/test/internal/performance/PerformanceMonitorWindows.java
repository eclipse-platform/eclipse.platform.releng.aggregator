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

import org.eclipse.perfmsr.core.PerformanceMonitor;
import org.eclipse.test.internal.performance.BasePerformanceMonitor;
import org.eclipse.test.internal.performance.LoadValueConstants;
import org.eclipse.test.internal.performance.data.Scalar;

/**
 * The PerformanceMonitor for Windows.
 * (It does not have the suffix "Windows" in its name because it relies on natives
 * which we cannot change right now). 
 */
public class PerformanceMonitorWindows extends BasePerformanceMonitor {
    		
	
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
			if (!PerformanceMonitor.isLoaded())
			    return;
			
			long[] counters= new long[14];
			if (PerformanceMonitor.nativeGetPerformanceCounters(counters)) {
				writeValue(scalars, 4, "Working Set", counters[0]);
				writeValue(scalars, 8, "Working Set Peak", counters[1]);
				writeValue(scalars, 9, "Elapsed Process", counters[2]);
				writeValue(scalars, 10, "User time", counters[3]);
				writeValue(scalars, 11, "Kernel time", counters[4]);
				writeValue(scalars, 19, "Page Faults", counters[5]);
				if (counters[6] != -1)
					writeValue(scalars, 7, "Committed", counters[6]);
				writeValue(scalars, 34, "GDI Objects", counters[7]);
				writeValue(scalars, 35, "USER Objects", counters[8]);
				if (counters[9] != -1)
					writeValue(scalars, 36, "Open Handles", counters[9]);
				writeValue(scalars, 38, "Read Count", counters[10]);
				writeValue(scalars, 39, "Write Count", counters[11]);
				writeValue(scalars, 40, "Bytes Read", counters[12]);
				writeValue(scalars, 41, "Bytes Written", counters[13]);
                writeValue(scalars, LoadValueConstants.What.cpuTime, "CPU Time", counters[3] + counters[4]);
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
			if (!PerformanceMonitor.isLoaded())
				return;
			
	    	long[] counters= new long[13];
			try {
				PerformanceMonitor.nativeGetPerformanceInfo(counters);
				long pageSize= counters[9];
				writeValue(scalars, 37, "Commit Total", counters[0]*pageSize);
				writeValue(scalars, 22, "Commit Limit", counters[1]*pageSize); 
				writeValue(scalars, 23, "Commit Peak", counters[2]*pageSize);
				writeValue(scalars, 24, "Physical Total", counters[3]*pageSize); 
				writeValue(scalars, 25, "Physical Available", counters[4]*pageSize); 
				writeValue(scalars, 26, "System Cache", counters[5]*pageSize);
				writeValue(scalars, 27, "Kernel Total", counters[6]*pageSize); 
				writeValue(scalars, 28, "Kernel Paged", counters[7]*pageSize); 
				writeValue(scalars, 29, "Kernel Nonpaged", counters[8]*pageSize); 
				writeValue(scalars, 30, "Page Size", counters[9]);
				writeValue(scalars, 31, "Handle Count", counters[10]); 
				writeValue(scalars, 32, "Process Count", counters[11]); 
				writeValue(scalars, 33, "Thread Count", counters[12]);
			} catch (Exception e) {
			    System.err.println("Exception in collectGlobalPerformanceInfo: " + e.toString());
				fgNativeGetPerformanceInfoNotAvailable= true;
			}
		}
	}
	
	protected String getUUID() {
		if (PerformanceMonitor.isLoaded()) {
			try {
				return PerformanceMonitor.nativeGetUUID();
			} catch (Exception e) {
			}
		}
		return super.getUUID();
	}
	
    private void writeValue(Map scalars, int id, String string, long value) {
        System.out.println(id + ": " + string + " " + value);
        scalars.put("" + id, new Scalar("" + id, value));
    }
}
