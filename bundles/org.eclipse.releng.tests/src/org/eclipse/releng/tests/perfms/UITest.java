/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests.perfms;
import junit.framework.TestCase;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
//import org.eclipse.perfmsr.core.PerfMsrCorePlugin;
//import org.eclipse.perfmsr.core.Upload;

public class UITest extends TestCase {
	public void runTest() {
		testPerfms();
	}
	protected void tearDown() {
	//	Upload.Status status = PerfMsrCorePlugin.getPerformanceMonitor(true).upload(null);
	//	System.out.println(status.message);
	}
	public void testPerfms() {
		/*
		 * this test takes snapshots before (1) and after (2) opening the Java
		 * Perspective. The delta between snapshots can be used to calculate the time required to open
		 * the Java perspective.  Disabled for now since the EclipseTestRunner is instrumented for performance
		 * monitoring.
		 */
		try {
	 	//	PerfMsrCorePlugin.getPerformanceMonitor(true).snapshot(1);
			PlatformUI.getWorkbench().openWorkbenchWindow(
					"org.eclipse.jdt.ui.JavaPerspective", null);
		//	PerfMsrCorePlugin.getPerformanceMonitor(true).snapshot(2);
		} catch (WorkbenchException e) {
			e.printStackTrace();
		} 
	}
}
