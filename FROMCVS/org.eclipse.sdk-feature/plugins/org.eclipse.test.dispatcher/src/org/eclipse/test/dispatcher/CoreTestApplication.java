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

/**
 * This is a wrapper application for org.eclipse.test.CoreTestApplication.
 * Its primary purpose is to call JUnitBundleConfigurer before org.eclipse.test.CoreTestApplication is
 * loaded and called.
 */
public class CoreTestApplication implements IApplication {
	
	/**
	 * Application entry point.  Calls JUnitBundleConfigurer to check if org.junit
	 * version 4.x needs to be uninstalled, then org.eclipse.test.CoreTestApplication is called.
	 *
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get("application.args");
		if (args == null)
			args = new String[0];
		JUnitBundleConfigurer configurer = new JUnitBundleConfigurer();
		configurer.configureJUnit(args);
		return JUnitBundleConfigurer.run("org.eclipse.test.CoreTestApplication");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		//do nothing
	}
}
