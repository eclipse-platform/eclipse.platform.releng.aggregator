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
package org.eclipse.test.internal.performance;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Plugin;


/**
 * @since 3.1
 */
public class PerformanceTestPlugin extends Plugin {
    
	/** 
	 * etools_perf_ctrl - Environment variable name that controls the performance instrumentation.
	 * It is a semicolon separated set of values. See the class level of
	 * documentation for TimerStep for it's valid values.
	 */
	public static final String ENV_PERF_CTRL= "etools_perf_ctrl";

	static {
        processEnvironmentVariables();
    }

	/**
	 * The plug-in id
	 */
    public static final String PLUGIN_ID= "org.eclipse.test.performance"; //$NON-NLS-1$
    
	/**
	 * The shared instance.
	 */
	private static PerformanceTestPlugin fgPlugin;

	
	/**
	 * The constructor.
	 */
	public PerformanceTestPlugin() {
	    super();
		fgPlugin= this;
	}
	
	/**
	 * The constructor.
	 */
	public PerformanceTestPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static PerformanceTestPlugin getDefault() {
		return fgPlugin;
	}
	
	/**
	 * Process your environment variables.
	 */
	public static void processEnvironmentVariables() {
		String ctrl= System.getProperty(ENV_PERF_CTRL);
		if (ctrl != null) {
			StringTokenizer st= new StringTokenizer(ctrl, ";");
			while (st.hasMoreTokens()) {
				String token= st.nextToken();
				int i= token.indexOf('=');
				if (i < 1)
				    throw new IllegalArgumentException("Key '" + token + "' in the environment variable " + ENV_PERF_CTRL + " is illformed");
				String value= token.substring(i+1);
				token= token.substring(0,i);
				System.out.println(token + ": " + value);
			}
		}
	}
}
