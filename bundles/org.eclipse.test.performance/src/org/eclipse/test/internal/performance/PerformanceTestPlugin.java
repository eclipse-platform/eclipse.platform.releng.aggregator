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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.test.internal.performance.db.DB;
import org.osgi.framework.BundleContext;


/**
 * @since 3.1
 */
public class PerformanceTestPlugin extends Plugin {
    
	/** 
	 * perf_ctrl - Environment variable name that controls the performance instrumentation.
	 * It is a semicolon separated set of values.
	 */
	public static final String ENV_PERF_CTRL= "perf_ctrl"; //$NON-NLS-1$

	/**
	 * The plug-in ID
	 */
    public static final String PLUGIN_ID= "org.eclipse.test.performance"; //$NON-NLS-1$
    

	/**
	 * The shared instance.
	 */
	private static PerformanceTestPlugin fgPlugin;
	
	private static Properties fgEnvironmentVariables;
	
	/**
	 * The constructor.
	 */
	public PerformanceTestPlugin() {
	    super();
		fgPlugin= this;
	}
	
	public void stop(BundleContext context) throws Exception {
		DB.shutdown();
		super.stop(context);
	}
		
	/*
	 * Returns the shared instance.
	 */
	public static PerformanceTestPlugin getDefault() {
		return fgPlugin;
	}
	
    public static Properties getEnvironmentVariables() {
        if (fgEnvironmentVariables == null) {
            fgEnvironmentVariables= new Properties();
            PerformanceTestPlugin.getEnvironmentVariables(fgEnvironmentVariables);
        }
        return fgEnvironmentVariables;
     }
    
    public static String getEnvironment(String key) {
        return getEnvironmentVariables().getProperty(key);
    }

	/*
	 * Process your environment variables.
	 */
	public static void getEnvironmentVariables(Properties properties) {
		String ctrl= System.getProperty(ENV_PERF_CTRL);
		if (ctrl != null) {
			StringTokenizer st= new StringTokenizer(ctrl, ";"); //$NON-NLS-1$
			while (st.hasMoreTokens()) {
				String token= st.nextToken();
				int i= token.indexOf('=');
				if (i < 1)
				    throw new IllegalArgumentException("Key '" + token + "' in the environment variable " + ENV_PERF_CTRL + " is illformed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				String value= token.substring(i+1);
				token= token.substring(0,i);
				//System.out.println(token + ": <" + value + ">");
				properties.put(token, value);
			}
		}
	}
	
	private static final String VERSION_SUFFIX= "-runtime"; //$NON-NLS-1$
	private static final String BUILDID_PROPERTY= "eclipse.buildId"; //$NON-NLS-1$
	private static final String SDK_BUNDLE_GROUP_IDENTIFIER= "org.eclipse.sdk"; //$NON-NLS-1$

	public static String getBuildId() {
		String buildId= System.getProperty(BUILDID_PROPERTY);
		if (buildId != null)
			return buildId;
		IBundleGroupProvider[] providers= Platform.getBundleGroupProviders();
		for (int i= 0; i < providers.length; i++) {
			IBundleGroup[] groups= providers[i].getBundleGroups();
			for (int j= 0; j < groups.length; j++)
				if (SDK_BUNDLE_GROUP_IDENTIFIER.equals(groups[j].getIdentifier()))
					return groups[j].getVersion() + VERSION_SUFFIX;
		}
		return null;
	}

	private static final String LOCALHOST= "localhost"; //$NON-NLS-1$

	/**
	 * Answer the name of the host that we are running on.
	 * @return the hostname
	 */
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return LOCALHOST;
		}
	}
}
