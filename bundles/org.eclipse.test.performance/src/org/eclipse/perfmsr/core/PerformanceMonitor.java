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

import java.util.Date;
import java.util.Map;


/**
 * The PerformanceMonitor for Windows.
 * (It does not have the suffix "Windows"  in its name because it relies on natives
 * and we could not change the class name). 
 */
public class PerformanceMonitor extends BasePerformanceMonitor {
    
	private static CounterMap[]	_map = new CounterMap[] {
	        new CounterMap(4, "Working Set", false),
	        new CounterMap(8, "Working Set Peak", false),
	        new CounterMap(9, "Elapsed Process", true),
	        new CounterMap(10, "User time", true),
	        new CounterMap(11, "Kernel time", true),
	        new CounterMap(19, "Page Faults", false),
	        new CounterMap(7, "Committed", false),
	        new CounterMap(34, "GDI Objects", false),
	        new CounterMap(35, "USER Objects", false),
	        new CounterMap(36, "Open Handles", false),
	        new CounterMap(38, "Read Count", false),
	        new CounterMap(39, "Write Count", false),
	        new CounterMap(40, "Bytes Read", false),
	        new CounterMap(41, "Bytes Written", false)
	};
	
	private interface WhatLabel {
		String elapsed 			= "Elapsed";
		String totalJavaHeap 	= "Total Java Heap";
		String usedJavaHeap 	= "Used Java Heap";
		String cpuTime 			= "CPU Time";
	}
	
	/**
	 * Index into the counters array.
	 */
	private interface WhatIdx {
	
		/** (3) The index into the counters array that holds the user time value. */
		int userTime 	= 3;
	
		/** (4) The index into the counters array that holds the kernel time value. */
		int kernelTime 	= 4;
		
		/** (6) The index into the counters array that holds the total committed number. */
		int committed 	= 6;
		
		/** (9) The index into the counters array that holds the open handles number. */
		int openHandles = 9;
	}
			

    /**
     * Map a native counter to a measurement.
     */
    private static class CounterMap {
    	public int 		what;		// dimension
    	public String	label;
    	public boolean	isTime;
    	
    	public CounterMap(int what, String label, boolean isTime) {
    		this.what = what;
    		this.label = label;
    		this.isTime = isTime;
    	}
    }
    
    /**
     * Hold some Window's global performance counters. This essentially wraps the
     * PERFORMANCE_INFORMATION structure.
     */
    static class PerformanceInfo {
        
    	/** Index of the counter that holds the page size. */
    	private static final int PAGE_SIZE_INDEX = 9;
    	
    	/** 
    	 * Represent an individual performance value.
    	 */
    	public class PerformanceValue {
    		/** 
    		 * 0 - an invalid value that indicates that we could not get the real value for the counter. 
    		 * We have to use zero rather than -1 because the C type is unsigned. 
    		 */
    		public final static long NO_COUNTER_VALUE = 0;
    		
    		/** index into the _counters array. */
    		private int		_index;
    		
    		/** The corresponding whatid in the performance data base. */
    		private int		_what;
    		
    		/** A description of the measurement. */
    		private String	_label;
    		
    		private PerformanceValue(int index, int what, String label) {
    			_index = index;
    			_what = what;
    			_label = label;
    		}
    		
    		public String getLabel() {
    			return _label;
    		}
    		
    		public int getWhat() {
    			return _what;
    		}
    		
    		/**
    		 * Answer the number of bytes. Since some of the counters hold pages instead of
    		 * bytes they need to be multiplied by the page size to get the number of bytes.
    		 * 
    		 * For normal counters (like the number of threads) the counter is returned 
    		 * without being modified.
    		 * 
    		 * The convention is to return NO_COUNTER_VALUE if the counter could not be determined.
    		 */
    		public long rawValue() {
    			if (_counters[_index] == NO_COUNTER_VALUE)
    			    return NO_COUNTER_VALUE;
    			
    			return _index < 9
    						? _counters[_index] * _counters[PAGE_SIZE_INDEX]
    						: _counters[_index];
    		}
    				
    	}
    	
    	private long[]	_counters = new long[13];
    	
    	    	
    	public final PerformanceValue COMMIT_TOTAL = 	new PerformanceValue(0, 37, "Commit Total"); 
    	public final PerformanceValue COMMIT_LIMIT = 	new PerformanceValue(1, 22, "Commit Limit"); 
    	public final PerformanceValue COMMIT_PEEK = 	new PerformanceValue(2, 23, "Commit Peak"); 
    	public final PerformanceValue PHYSICAL_TOTAL = 	new PerformanceValue(3, 24, "Physical Total"); 
    	public final PerformanceValue PHYSICAL_AVAILABLE = new PerformanceValue(4, 25, "Physical Available"); 
    	public final PerformanceValue SYSTEM_CACHE = 	new PerformanceValue(5, 26, "System Cache"); 
    	public final PerformanceValue KERNEL_TOTAL = 	new PerformanceValue(6, 27, "Kernel Total"); 
    	public final PerformanceValue KERNEL_PAGED = 	new PerformanceValue(7, 28, "Kernel Paged"); 
    	public final PerformanceValue KERNEL_NON_PAGED =new PerformanceValue(8, 29, "Kernel Nonpaged"); 
    	public final PerformanceValue PAGE_SIZE = 		new PerformanceValue(9, 30, "page Size"); 
    	public final PerformanceValue HANDLE_COUNT = 	new PerformanceValue(10, 31, "Handle Count"); 
    	public final PerformanceValue PROCESS_COUNT = 	new PerformanceValue(11, 32, "Process Count"); 
    	public final PerformanceValue THREAD_COUNT = 	new PerformanceValue(12, 33, "Thread Count"); 

    	PerformanceValue[] values = {COMMIT_TOTAL, COMMIT_LIMIT, COMMIT_PEEK, PHYSICAL_TOTAL, PHYSICAL_AVAILABLE,
    		SYSTEM_CACHE, KERNEL_TOTAL, KERNEL_PAGED, KERNEL_NON_PAGED, PAGE_SIZE, HANDLE_COUNT, PROCESS_COUNT,
    		THREAD_COUNT};
    }

	//-----------------------------------


	/** 
	 * ivjperf - name of the library that implements the native methods.
	 */
	private static final String NATIVE_LIBRARY_NAME= "ivjperf";
	
	/**
	 * Is the native library loaded? 0-don't know, 1-no, 2-yes
	 */
	private static int fgIsLoaded= 0;

	/**
	 * Answer true if the native library for this class has been successfully
	 * loaded. If the load has not been attempted yet, try to load it.
	 */
	private static boolean isLoaded() {
		if (fgIsLoaded == 0) {
			try {
				System.loadLibrary(NATIVE_LIBRARY_NAME);
				fgIsLoaded= 2;
			} catch (Throwable e) {
			    fgIsLoaded= 1;
			}
		}
		return fgIsLoaded == 2;
	}
	
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
    protected void writeOperatingSystemCounters(Map scalars) {
		long[] counters = new long[_map.length];
		synchronized(this) {
			if (!isLoaded()) {
			    return;
			}
			if (nativeGetPerformanceCounters(counters)) {
				for (int i = 0; i < _map.length; i++) {
                	if ((i ==  WhatIdx.committed || i == WhatIdx.openHandles) && counters[i] == -1)
                	    continue;
                	
                	writeValue(_map[i].what, counters[i], _map[i].label, 
                		_map[i].isTime ? formatedTime(counters[i])
                		        		: formatEng(counters[i]));
                }
                
                long cpu = counters[WhatIdx.userTime] + counters[WhatIdx.kernelTime];
                writeValue(LoadValueConstants.What.cpuTime, cpu, WhatLabel.cpuTime, formatedTime(cpu));
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
	protected void writeGlobalPerformanceInfo(Map scalars) {
		synchronized(this) {
		    PerformanceInfo pi= new PerformanceInfo();
			if (!isLoaded()) {
			    System.err.println("The DLL " + NATIVE_LIBRARY_NAME + " could not be loaded");
				return;
			}
			try {
				nativeGetPerformanceInfo(pi._counters);
			} catch (Exception e) {
			    System.err.println("Exception in writeGlobalPerformanceInfo: " + e.toString());
				return;
			}
			for (int i= 0; i < pi.values.length; i++) {
				PerformanceInfo.PerformanceValue v= pi.values[i];
				long result = v.rawValue();
				writeValue(v.getWhat(), result, v.getLabel(), formatEng(result));
			}
		}
	}
	
	protected String getUUID() {
		if (isLoaded()) {
			try {
				return nativeGetUUID();
			} catch (Exception e) {
			}
		}
		return super.getUUID();
	}
	
	//----- natives
	
	/**
	 * Calls the Windows GetPerformanceInfo function
	 * @param counters any array of counters that corresponds to the Windows
	 * PERFORMANCE_INFORMATION structure.
	 */
	private static native void nativeGetPerformanceInfo(long[] counters);
	
	private static native boolean nativeGetPerformanceCounters(long[] counters);
	
	private static native String nativeGetUUID();
	
	//---- dummies
	
    private String formatedTime(long l) {
        return new Date(l).toString();
    }

    private String formatEng(long l) {
        return Long.toString(l);
    }

    private void writeValue(int i, long l, String string, String object) {
        System.out.println(i + ": " + string + " " + object);
    }
}
