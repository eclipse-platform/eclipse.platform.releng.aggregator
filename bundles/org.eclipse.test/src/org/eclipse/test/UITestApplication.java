/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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

