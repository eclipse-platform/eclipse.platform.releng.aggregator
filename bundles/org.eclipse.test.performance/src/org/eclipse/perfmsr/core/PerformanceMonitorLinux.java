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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

public class PerformanceMonitorLinux extends BasePerformanceMonitor {
    
    /**
     * The status values for a Linux process, that is the values that come from /proc/self/stat.
     * 
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
     * 
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
	protected void writeOperatingSystemCounters(Map scalars) {
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
 			
			long v = jiffiesToMilliseconds(stat.utime);
			//writeValue(LoadValueConstants.What.userTime, v, "User time", formatedTime(v), displayResults, b);
			
			v = jiffiesToMilliseconds(stat.stime);
			//writeValue(LoadValueConstants.What.kernelTime, v, "Kernel time", formatedTime(v), displayResults, b);
			
			v = jiffiesToMilliseconds(stat.utime+stat.stime);
			//writeValue(LoadValueConstants.What.cpuTime, v, "CPU time", formatedTime(v), displayResults, b);
			
			int i = pagesToBytes(statm.resident);
			//writeValue(LoadValueConstants.What.workingSet, i, "Working Set", formatEng(i), displayResults, b);
			
			//writeValue(LoadValueConstants.What.softPageFaults, stat.minflt, "Soft Page Faults", formatEng(stat.minflt), displayResults, b);
			
			//writeValue(LoadValueConstants.What.hardPageFaults, stat.majflt, "Hard Page Faults", formatEng(stat.majflt), displayResults, b);
			
			i = pagesToBytes(statm.trs);
			//writeValue(LoadValueConstants.What.trs, i, "Text Size", formatEng(i), displayResults, b);
			
			i = pagesToBytes(statm.drs);
			//writeValue(LoadValueConstants.What.drs, i, "Data Size", formatEng(i), displayResults, b);
			
			i = pagesToBytes(statm.lrs);
			//writeValue(LoadValueConstants.What.lrs, i, "Library Size", formatEng(i), displayResults, b);
		}
	}
	
	/**
	 * Write out the global machine counters for Linux.
	 */
	protected void writeGlobalPerformanceInfo(Map scalars) {
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
//		writeValue(LoadValueConstants.What.physicalMemory, mi.total, "Physical Memory", formatEng(mi.total), displayResults, b);
//		writeValue(LoadValueConstants.What.usedLinuxMemory, mi.used, "Used Memory", formatEng(mi.used), displayResults, b);
//		writeValue(LoadValueConstants.What.freeLinuxMemory, mi.free, "Free Memory", formatEng(mi.free), displayResults, b);
//		writeValue(LoadValueConstants.What.buffersLinux, mi.buffers, "Buffers Memory", formatEng(mi.buffers), displayResults, b);
//		writeValue(LoadValueConstants.What.systemCache, mi.cache, "System Cache", formatEng(mi.cache), displayResults, b);
	}
	
	/**
	 * Convert jiffies to milliseconds. For pre 2.5 kernels on Intel it seems like this is 10ms per
	 * jiffy. 
	 * 
	 * @param jiffies
	 * @return
	 */
	private static long jiffiesToMilliseconds(long jiffies) {
		//TODO for newer kernels you should call the jiffies_to_clock_t function
		return jiffies * 10L;
	}
	
	/**
	 * Convert a value that is the number of pages into bytes.
	 * @param pages
	 * @return
	 */
	private static int pagesToBytes(int pages) {
		return pages * 4096;
	}
	
	/**
	 * Write out a value element.
	 * 
	 * @param what the measurement type
	 * @param result the value of the measurement
	 * @param displayWhat the measurement type in a human readable form
	 * @param displayResult the measurement in a human readable form
	 * @param displayResults are the results going to be displayed?
	 * @param b the buffer where the results will be displayed
	 */
//	private void writeValue(int what, long result, String displayWhat, String displayResult, 
//			boolean displayResults, StringBuffer b) {
//		PrintWriter pw = getPrintWriter();
//		pw.print("<"+TimerXML.value+" "+TimerXML.what+"='");
//		pw.print(what);
//		pw.print("' "+TimerXML.result+"='");
//		pw.print(result);
//		pw.print("' "+TimerXML.displayWhat+"='");
//		pw.print(displayWhat);
//		pw.print("' "+TimerXML.displayResult+"='");
//		pw.print(displayResult);
//		pw.println("' />");
//		
//		if (displayResults) {
//			b.append(displayWhat);
//			b.append(" = ");
//			b.append(displayResult);
//			b.append("\n");
//		}
//	}
}
