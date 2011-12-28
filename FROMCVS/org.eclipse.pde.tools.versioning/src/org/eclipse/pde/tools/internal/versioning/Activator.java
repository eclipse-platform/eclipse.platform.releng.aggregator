/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.internal.versioning;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 *	 org.eclipse.pde.tools.versioning bundle activator
 *
 */
public class Activator implements BundleActivator {
	public static final String PLUGIN_ID = "org.eclipse.pde.tools.versioning"; //$NON-NLS-1$
	private static PluginConverter pluginConverter = null;
	private static BundleContext context = null;
	private static ServiceTracker bundleTracker = null;
	private static ServiceTracker debugTracker = null;

	/**
	 * Log the given debug message.
	 */
	public static void debug(String message) {
		if (message != null)
			System.out.println(message);
	}

	/**
	 * Return a boolean value indicating whether or not debug options are
	 * turned on for the given key.
	 */
	public static boolean getBooleanDebugOption(String key) {
		if (context == null)
			return false;
		if (debugTracker == null) {
			debugTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		DebugOptions debug = (DebugOptions) debugTracker.getService();
		return debug == null ? false : debug.getBooleanOption(key, false);
	}

	/*
	 * Constructor for the class.
	 */
	public Activator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext runtimeContext) throws Exception {
		context = runtimeContext;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext runtimeContext) {
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		context = null;
		pluginConverter = null;
	}

	/**
	 * Return the plug-in converter class or <code>null</code> if it
	 * is not available.
	 * 
	 * @return the plug-in converter or <code>null</code>
	 */
	public static PluginConverter getPluginConverter() {
		if (pluginConverter == null) {
			if (bundleTracker == null) {
				if (context == null)
					return null;
				bundleTracker = new ServiceTracker(context, PluginConverter.class.getName(), null);
				bundleTracker.open();
		}
			pluginConverter = (PluginConverter) bundleTracker.getService();
		}
		return pluginConverter;
	}
}
