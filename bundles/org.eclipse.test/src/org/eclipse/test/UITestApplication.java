/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.io.IOException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.internal.Workbench;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */ 
public class UITestApplication extends Workbench {
	/**
	 * Run an event loop for the workbench.
	 */
	protected void runEventLoop(Window.IExceptionHandler handler) {
		// Dispatch all events.
		Display display = Display.getCurrent();
		while (true) {
			try {
				if (!display.readAndDispatch())
					break;
			} catch (Throwable e) {
				break;
			}
		}

		String[] arguments= getCommandLineArgs();
		runTests(arguments);
		close();		
	}
	
	protected void runTests(String[] commandLineArgs) {
		try {
			EclipseTestRunner.main(commandLineArgs);	
		} catch(IOException e) {
			e.printStackTrace();
		}	
	}
}

