package org.eclipse.test.internal.performance;

/**
 * Some hard coded measurement id's. The server and the plug need to agreee onwhat these mean.
 */
public interface Dimensions {
    
//  Common:
//	OS Counters:
    		int
    			WORKING_SET= 4, 		// the working set (or on Linux the resident set). "Working Set"	
    			USER_TIME= 10,		// the amount of elapsed user time. "User time"
    			KERNEL_TIME= 11,		// the amount of elapsed kernel time. "Kernel time"
    			CPU_TIME= 20; 		// the amount of CPU time we have used so far. "CPU Time"

//  OS Info:
    		int
    			PHYSICAL_TOTAL= 24, // the amount of physical memory in bytes. "Physical Memory"
			SYSTEM_CACHE= 26; 	// the amount of system cache memory in bytes. "System Cache"

//	Windows:
//	OS Counters:
    		int
			COMITTED= 7, 		// "Committed"
    			WORKING_SET_PEAK= 8,	// "Working Set Peak"
    			ELAPSED_PROCESS= 9,	// "Elapsed Process"
    			PAGE_FAULTS= 19, 	// "Page Faults"
    			GDI_OBJECTS= 34, 	// "GDI Objects"
    			USER_OBJECTS= 35, 	// "USER Objects"
    			OPEN_HANDLES= 36, 	// "Open Handles"
    			READ_COUNT= 38, 		// "Read Count"
    			WRITE_COUNT= 39, 	// "Write Count"
    			BYTES_READ= 40, 		// "Bytes Read"
    			BYTES_WRITTEN= 41; 	// "Bytes Written"
    			
//    	OS Info:
    		int
    			COMMIT_LIMIT= 22, 	// "Commit Limit"
    			COMMIT_PEAK= 23, 	// "Commit Peak"
    			PHYSICAL_AVAIL= 25, 	// "Physical Available"
    			KERNEL_TOTAL= 27, 	// "Kernel Total"
    			KERNEL_PAGED= 28, 	// "Kernel Paged"
    			KERNEL_NONPAGED= 29, 	// "Kernel Nonpaged"
    			PAGE_SIZE= 30, 		// "Page Size"
    			HANDLE_COUNT= 31, 	// "Handle Count"
    			PROCESS_COUNT= 32, 	// "Process Count"
    			THREAD_COUNT= 33, 	// "Thread Count"
			COMMIT_TOTAL= 37; 	// "Commit Total"
    		
// Linux:
//    	OS Counters:
    		int
			HARD_PAGE_FAULTS= 42, // the number of hard page faults. A page had to be fetched from disk. "Hard Page Faults"
    			SOFT_PAGE_FAULTS= 43, // the number of soft page faults. A page was not fetched from disk. "Soft Page Faults"		
    			TRS= 44, // the amount of memory in bytes occupied by text (i.e. code). "Text Size"	
    			DRS= 45, // the amount of memory in bytes occupied by data or stack. "Data Size"
    			LRS= 46; // the amount of memory in bytes occupied by shared code. "Library Size"
    			
//  OS Info:
    		int
    			USED_LINUX_MEM= 48, // the amount of memory that Linux reports is used. From /proc/meminfo. "Used Memory"
    			FREE_LINUX_MEM= 49, // the amount of memory that Linux reports is free. From /proc/meminfo. "Free Memory"
    			BUFFERS_LINUX= 50; // the amount of memory that Linux reports is used by buffers. From /proc/meminfo. "Buffers Memory"
    	
// Mac:
//	OS Counters:
//	OS Info:
}
