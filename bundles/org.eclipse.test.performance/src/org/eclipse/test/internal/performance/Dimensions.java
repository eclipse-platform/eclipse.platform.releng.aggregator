package org.eclipse.test.internal.performance;

import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Unit;

/**
 * Some hard coded measurement id's.
 */
public interface Dimensions {
    
//  Common:
//		OS Counters:
    		Dimension
				SYSTEM_TIME= new Dimension(2, Unit.SECOND, 1000), 		// System.currentTimeMillis. "System Time"
				USED_JAVA_HEAP= new Dimension(3, Unit.BYTE), 			// Runtime.totalMemory() - Runtime.freeMemory()
    			WORKING_SET= new Dimension(4, Unit.BYTE), 				// the working set (or on Linux the resident set). "Working Set"	
    			USER_TIME= new Dimension(10, Unit.SECOND, 1000),		// the amount of elapsed user time. "User time"
    			KERNEL_TIME= new Dimension(11, Unit.SECOND, 1000),		// the amount of elapsed kernel time. "Kernel time"
    			CPU_TIME= new Dimension(20, Unit.SECOND, 1000); 		// the amount of CPU time we have used so far. "CPU Time"

//  	OS Info:
    		Dimension
    			PHYSICAL_TOTAL= new Dimension(24, Unit.BYTE),	// the amount of physical memory in bytes. "Physical Memory"
				SYSTEM_CACHE= new Dimension(26, Unit.BYTE);		// the amount of system cache memory in bytes. "System Cache"

//	Windows:
//		OS Counters:
    		Dimension
				COMITTED= new Dimension(7, Unit.BYTE),			// "Committed"
    			WORKING_SET_PEAK= new Dimension(8, Unit.BYTE),	// "Working Set Peak"
    			ELAPSED_PROCESS= new Dimension(9, Unit.SECOND),	// "Elapsed Process"
    			PAGE_FAULTS= new Dimension(19), 				// "Page Faults"
    			GDI_OBJECTS= new Dimension(34), 				// "GDI Objects"
    			USER_OBJECTS= new Dimension(35), 				// "USER Objects"
    			OPEN_HANDLES= new Dimension(36), 				// "Open Handles"
    			READ_COUNT= new Dimension(38, Unit.BYTE), 		// "Read Count"
    			WRITE_COUNT= new Dimension(39, Unit.BYTE),		// "Write Count"
    			BYTES_READ= new Dimension(40, Unit.BYTE), 		// "Bytes Read"
    			BYTES_WRITTEN= new Dimension(41, Unit.BYTE); 	// "Bytes Written"
    			
//    	OS Info:
    		Dimension
    			COMMIT_LIMIT= new Dimension(22),		// "Commit Limit"
    			COMMIT_PEAK= new Dimension(23),			// "Commit Peak"
    			PHYSICAL_AVAIL= new Dimension(25, Unit.BYTE), 	// "Physical Available"
    			KERNEL_TOTAL= new Dimension(27),		// "Kernel Total"
    			KERNEL_PAGED= new Dimension(28),		// "Kernel Paged"
    			KERNEL_NONPAGED= new Dimension(29), 	// "Kernel Nonpaged"
    			PAGE_SIZE= new Dimension(30, Unit.BYTE),// "Page Size"
    			HANDLE_COUNT= new Dimension(31),		// "Handle Count"
    			PROCESS_COUNT= new Dimension(32),		// "Process Count"
    			THREAD_COUNT= new Dimension(33),		// "Thread Count"
				COMMIT_TOTAL= new Dimension(37);		// "Commit Total"
    		
// Linux:
//    	OS Counters:
    		Dimension
				HARD_PAGE_FAULTS= new Dimension(42),	// the number of hard page faults. A page had to be fetched from disk. "Hard Page Faults"
    			SOFT_PAGE_FAULTS= new Dimension(43),	// the number of soft page faults. A page was not fetched from disk. "Soft Page Faults"		
    			TRS= new Dimension(44, Unit.BYTE),		// the amount of memory in bytes occupied by text (i.e. code). "Text Size"	
    			DRS= new Dimension(45, Unit.BYTE),		// the amount of memory in bytes occupied by data or stack. "Data Size"
    			LRS= new Dimension(46, Unit.BYTE);		// the amount of memory in bytes occupied by shared code. "Library Size"
    			
//  	OS Info:
    		Dimension
    			USED_LINUX_MEM= new Dimension(48, Unit.BYTE),	// the amount of memory that Linux reports is used. From /proc/meminfo. "Used Memory"
    			FREE_LINUX_MEM= new Dimension(49, Unit.BYTE),	// the amount of memory that Linux reports is free. From /proc/meminfo. "Free Memory"
    			BUFFERS_LINUX= new Dimension(50, Unit.BYTE);	// the amount of memory that Linux reports is used by buffers. From /proc/meminfo. "Buffers Memory"
    	
// Mac:
//		OS Counters:
//		OS Info:
}
