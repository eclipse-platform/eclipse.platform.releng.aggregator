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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

class PerformanceMonitorLinux extends PerformanceMonitor {
    
    /**
     * The status values for a Linux process, that is the values that come from /proc/self/stat.
     * The names of the variables match the man proc page.
     */
    static class LinuxProcStat {
    	
    	public int pid;			/** Process id. */
    	public String comm;		/** The command name. */
    	public String state;
    	public int ppid;		/** Parent process id. */
    	public int pgrp;		/** Process group. */    	
    	public int session;
    	public int ttry_nr;
    	public int tpgid;
    	public long flags;
    	public long minflt;		/** Minor page faults (didn't need to load a page from disk). */
    	public long cminflt;	/** Minor page faults for the process and it's children. */
    	public long majflt;		/** Major page faults. */
    	public long	cmajflt;	/** Major page faults for the process and it's children. */
    	public long utime;		/** User time in jiffies. */
    	public long stime;		/** System time in jiffies. */ 	
    	public long cutime;		/** User time for the process and it's children. */
    	public long cstime;		/** System time for the process and it's children. */
    	// I didn't bother with the rest (there are more)
    }

    /**
     * The status memory values values for a Linux process, that is the values that come from /proc/self/statm.
     * The names of the variables match the man proc page.
     */
    static class LinuxProcStatM {
    	
    	public int size;		/** Size of the process in pages. */
    	public int resident;	/** Resident size in pages. */
    	public int shared;		/** Shared size in pages. */
    	public int trs;			/** Text (code) size in pages. */
    	public int drs;			/** Data/Stack size in pages. */
    	public int lrs;			/** Library size in pages. */
    	public int dt;			/** Dirty pages. */
    }

    /**
     * The meminfo values for a Linux machine, that is the values that come from /proc/meminfo.
     */
    static class LinuxMemInfo {
    	public long total;
    	public long used;
    	public long free;
    	public long shared;
    	public long buffers;
    	public long cache;
    }
    
	/**
	 * Write out operating system counters for Linux.
	 */
	protected void collectOperatingSystemCounters(Map scalars) {
		synchronized(this) {
    		LinuxProcStat stat= new LinuxProcStat();
    		BufferedReader rdr= null;
    		try {
    			rdr= new BufferedReader(new FileReader("/proc/self/stat"));
    			String line= rdr.readLine();
    			StringTokenizer st= new StringTokenizer(line);
    			stat.pid = Integer.parseInt(st.nextToken());
    			stat.comm = st.nextToken();
    			stat.state = st.nextToken();
    			stat.ppid = Integer.parseInt(st.nextToken());
    			stat.pgrp = Integer.parseInt(st.nextToken());
    			stat.session = Integer.parseInt(st.nextToken());
    			stat.ttry_nr = Integer.parseInt(st.nextToken());
    			stat.tpgid = Integer.parseInt(st.nextToken());
    			stat.flags = Long.parseLong(st.nextToken());
    			stat.minflt = Long.parseLong(st.nextToken());
    			stat.cminflt = Long.parseLong(st.nextToken());
    			stat.majflt = Long.parseLong(st.nextToken());
    			stat.cmajflt = Long.parseLong(st.nextToken());
    			stat.utime = Long.parseLong(st.nextToken());
    			stat.stime = Long.parseLong(st.nextToken());
    			stat.cutime = Long.parseLong(st.nextToken());
    			stat.cstime = Long.parseLong(st.nextToken());
    		} catch (IOException e) {
    		} finally {
    			try {
    			    rdr.close();
    			} catch (IOException e) {
    			}
    		}

    		LinuxProcStatM statm= new LinuxProcStatM();
    		try {
    			rdr= new BufferedReader(new FileReader("/proc/self/statm"));
    			String line= rdr.readLine();
    			StringTokenizer st= new StringTokenizer(line);
    			
    			statm.size= Integer.parseInt(st.nextToken());
    			statm.resident= Integer.parseInt(st.nextToken());
    			statm.shared= Integer.parseInt(st.nextToken());
    			statm.trs= Integer.parseInt(st.nextToken());
    			statm.drs= Integer.parseInt(st.nextToken());
    			statm.lrs= Integer.parseInt(st.nextToken());
    			statm.dt= Integer.parseInt(st.nextToken());
    		} catch (IOException e) {
    		} finally {
    			try {
    			    rdr.close();
    			} catch (IOException e) {
    			}
    		}
 			
    		long pageSize= 4096;
    		long jiffies= 10L;
    		
			addScalar(scalars, LoadValueConstants.What.userTime, "User time", stat.utime*jiffies);			
			addScalar(scalars, LoadValueConstants.What.kernelTime, "Kernel time", stat.stime*jiffies);			
			addScalar(scalars, LoadValueConstants.What.cpuTime, "CPU time", (stat.utime+stat.stime)*jiffies);			
			addScalar(scalars, LoadValueConstants.What.workingSet, "Working Set", statm.resident*pageSize);		
			addScalar(scalars, LoadValueConstants.What.softPageFaults, "Soft Page Faults", stat.minflt);			
			addScalar(scalars, LoadValueConstants.What.hardPageFaults, "Hard Page Faults", stat.majflt);			
			addScalar(scalars, LoadValueConstants.What.trs, "Text Size", statm.trs*pageSize);			
			addScalar(scalars, LoadValueConstants.What.drs, "Data Size", statm.drs*pageSize);			
			addScalar(scalars, LoadValueConstants.What.lrs, "Library Size", statm.lrs*pageSize);
		}
	}
	
	/**
	 * Write out the global machine counters for Linux.
	 */
	protected void collectGlobalPerformanceInfo(Map scalars) {
		LinuxMemInfo mi= new LinuxMemInfo();
		BufferedReader rdr= null;
		try {
			rdr= new BufferedReader(new FileReader("/proc/meminfo"));
			String line= rdr.readLine();	// throw away the heading line
			line= rdr.readLine();
			StringTokenizer st= new StringTokenizer(line);
			st.nextToken();	// throw away label
			mi.total = Long.parseLong(st.nextToken());
			mi.used = Long.parseLong(st.nextToken());
			mi.free = Long.parseLong(st.nextToken());
			mi.shared = Long.parseLong(st.nextToken());
			mi.buffers = Long.parseLong(st.nextToken());
			mi.cache = Long.parseLong(st.nextToken());
		} catch (IOException e) {
		} finally {
			try {
			    rdr.close();
			} catch (IOException e) {
			}
		}
		addScalar(scalars, LoadValueConstants.What.physicalMemory, "Physical Memory", mi.total);
		addScalar(scalars, LoadValueConstants.What.usedLinuxMemory, "Used Memory", mi.used);
		addScalar(scalars, LoadValueConstants.What.freeLinuxMemory, "Free Memory", mi.free);
		addScalar(scalars, LoadValueConstants.What.buffersLinux, "Buffers Memory", mi.buffers);
		addScalar(scalars, LoadValueConstants.What.systemCache, "System Cache", mi.cache);
	}
}
