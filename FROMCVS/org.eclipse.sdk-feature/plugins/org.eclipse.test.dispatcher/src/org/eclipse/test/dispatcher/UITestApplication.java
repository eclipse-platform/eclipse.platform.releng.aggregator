/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.dispatcher;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.testing.ITestHarness;

/**
 * This is a wrapper application for org.eclipse.test.UITestApplication.
 * Its primary purpose is to call JUnitBundleConfigurer before org.eclipse.test.UITestApplication
 * is loaded and called.
 */
public class UITestApplication implements IApplication, ITestHarness {		
	
	/**
	 * Application entry point.  Calls JUnitBundleConfigurer to check if org.junit
	 * version 4.x needs to be uninstalled, then calls org.eclipse.test.UITestApplication.
	 *
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get("application.args");
		if (args == null)
			args = new String[0];
		JUnitBundleConfigurer configurer = new JUnitBundleConfigurer();
		configurer.configureJUnit(args);
		return JUnitBundleConfigurer.run("org.eclipse.test.UITestApplication");
	}

	public void stop() {
		//do nothing	
	}

	public void runTests() {
		try {
			JUnitBundleConfigurer.runTests("org.eclipse.test.UITestApplication");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}

