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

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.test.internal.performance.db.DB;
import org.osgi.framework.BundleContext;


/**
 * @since 3.1
 */
public class PerformanceTestPlugin extends Plugin {
    
    public static final String CONFIG= "config"; //$NON-NLS-1$
	public static final String BUILD= "build"; //$NON-NLS-1$

	private static final String DEFAULT_DB_NAME= "perfDB"; //$NON-NLS-1$
	private static final String DEFAULT_DB_USER= "guest"; //$NON-NLS-1$
	private static final String DEFAULT_DB_PASSWORD= "guest"; //$NON-NLS-1$
	
	private static final String DB_LOCATION= "dbloc"; //$NON-NLS-1$
	private static final String DB_NAME= "dbname"; //$NON-NLS-1$
	private static final String DB_USER= "dbuser"; //$NON-NLS-1$
	private static final String DB_PASSWD= "dbpasswd"; //$NON-NLS-1$

    /*
	 * New properties
	 */
    private static final String ECLIPSE_PERF_DBLOC= "eclipse.perf.dbloc"; //$NON-NLS-1$
    private static final String ECLIPSE_PERF_ASSERTAGAINST= "eclipse.perf.assertAgainst"; //$NON-NLS-1$
    private static final String ECLIPSE_PERF_CONFIG= "eclipse.perf.config"; //$NON-NLS-1$

    /*
	 * Old property subkeys
	 */
	private static final String OLD_ASSERT_AGAINST= "assertAgainst"; //$NON-NLS-1$
	private static final String OLD_BUILD= "build"; //$NON-NLS-1$
	private static final String OLD_CONFIG= "config"; //$NON-NLS-1$

    /** 
	 * perf_ctrl - Environment variable name that controls the performance instrumentation.
	 * It is a semicolon separated set of values.
	 */
	public static final String ENV_PERF_CTRL= "perf_ctrl"; //$NON-NLS-1$

	/**
	 * The plug-in ID
	 */
    public static final String PLUGIN_ID= "org.eclipse.test.performance"; //$NON-NLS-1$
    
	/** Status code describing an internal error */
	public static final int INTERNAL_ERROR= 1;

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
	
	/*
	 * returns the "perf_ctrl" property split into separate key/value pairs
	 */
    private static Properties getOldStyleEnvironmentVariables() {
        if (fgEnvironmentVariables == null) {
            fgEnvironmentVariables= new Properties();
            String ctrl= System.getProperty(ENV_PERF_CTRL);
            if (ctrl != null)
                parsePairs(fgEnvironmentVariables, ctrl);
        }
        return fgEnvironmentVariables;
     }
    
	/*
	 * -Declipse.perf.dbloc=net://localhost
	 */
	public static String getDBLocation() {
		String dbloc= System.getProperty(ECLIPSE_PERF_DBLOC);
		if (dbloc != null) {
		    Properties keys= new Properties();
		    parsePairs(keys, ECLIPSE_PERF_DBLOC + '=' + dbloc);
		    dbloc= keys.getProperty(ECLIPSE_PERF_DBLOC);
		} else {
		    dbloc= getOldStyleEnvironmentVariables().getProperty(DB_LOCATION);
		}
        //dbloc= "net://localhost";
		//dbloc= "/Users/weinand/Eclipse/cloudscape2";
        return dbloc;
	}

	public static String getDBName() {
		String dbloc= System.getProperty(ECLIPSE_PERF_DBLOC);
		if (dbloc != null) {
		    Properties keys= new Properties();
		    parsePairs(keys, ECLIPSE_PERF_DBLOC + '=' + dbloc);
		    return keys.getProperty(DB_NAME, DEFAULT_DB_NAME);
		} 
	    return getOldStyleEnvironmentVariables().getProperty(DB_NAME, DEFAULT_DB_NAME);
	}

	public static String getDBUser() {
		String dbloc= System.getProperty(ECLIPSE_PERF_DBLOC);
		if (dbloc != null) {
		    Properties keys= new Properties();
		    parsePairs(keys, ECLIPSE_PERF_DBLOC + '=' + dbloc);
		    return keys.getProperty(DB_USER, DEFAULT_DB_USER);
		} 
	    return getOldStyleEnvironmentVariables().getProperty(DB_USER, DEFAULT_DB_USER);
	}

	public static String getDBPassword() {
		String dbloc= System.getProperty(ECLIPSE_PERF_DBLOC);
		if (dbloc != null) {
		    Properties keys= new Properties();
		    parsePairs(keys, ECLIPSE_PERF_DBLOC + '=' + dbloc);
		    return keys.getProperty(DB_PASSWD, DEFAULT_DB_PASSWORD);
		} 
	    return getOldStyleEnvironmentVariables().getProperty(DB_PASSWD, DEFAULT_DB_PASSWORD);
	}
	
	/*
	 * -Declipse.perf.config=<varname1>=<varval1>;<varname2>=<varval2>;...;<varnameN>=<varvalN>
	 */
	public static Properties getConfig() {
	    Properties keys= new Properties();
		String configKey= System.getProperty(ECLIPSE_PERF_CONFIG);
		if (configKey != null) {
		    parsePairs(keys, configKey);
		} else {
			String build= getOldStyleEnvironmentVariables().getProperty(OLD_BUILD);
		    if (build == null)
		        return null;
		    keys.put(BUILD, build);

		    String configName= getOldStyleEnvironmentVariables().getProperty(OLD_CONFIG);
		    if (configName != null)
		        keys.put(CONFIG, configName);
		}
	    return keys;
	}

	/*
	 * -Declipse.perf.assertAgainst=<varname1>=<varval1>;<varname2>=<varval2>;...;<varnameN>=<varvalN>
	 * Returns null if assertAgainst property isn't defined.
	 */
	public static Properties getAssertAgainst() {
	    Properties keys= getConfig();
	    if (keys == null)
	        keys= new Properties();
		String assertKey= System.getProperty(ECLIPSE_PERF_ASSERTAGAINST);
		if (assertKey != null) {
		    parsePairs(keys, assertKey);
		} else {
		    String refBuild= getOldStyleEnvironmentVariables().getProperty(OLD_ASSERT_AGAINST);
		    if (refBuild == null)
		        return null;
		    keys.put(BUILD, refBuild);
		}
	    return keys;
	}
	
	private static void parsePairs(Properties keys, String s) {
		StringTokenizer st= new StringTokenizer(s, ";"); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token= st.nextToken();
			int i= token.indexOf('=');
			if (i < 1)
			    throw new IllegalArgumentException("Key '" + token + "' in the environment variable " + ENV_PERF_CTRL + " is illformed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String value= token.substring(i+1);
			token= token.substring(0,i);
			//System.out.println(token + ": <" + value + ">");
			keys.put(token, value);
		}	    
	}
	
	public static void parseVariations(Properties keys, String s) {
		StringTokenizer st= new StringTokenizer(s, "|"); //$NON-NLS-1$
		while (st.hasMoreTokens()) {
			String token= st.nextToken();
			int i= token.indexOf('=');
			if (i < 1)
			    throw new IllegalArgumentException("Key '" + token + "' in the environment variable " + ENV_PERF_CTRL + " is illformed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String value= token.substring(i+1);
			token= token.substring(0,i);
			//System.out.println(token + ": <" + value + ">");
			keys.put(token, value);
		}	    
	}
	
	
	/*
	 * TODO: we need to escape '=' and ';' characters in key/values.
	 */
    public static String toVariations(Properties keyValues) {
        Set set= keyValues.keySet();
        String[] keys= (String[]) set.toArray(new String[set.size()]);
        Arrays.sort(keys);
        StringBuffer sb= new StringBuffer();
        
        for (int i= 0; i < keys.length; i++) {
            String key= keys[i];
            String value= keyValues.getProperty(key);
            sb.append('|');
            sb.append(key);
            sb.append('=');
            if (value != null)
                sb.append(value);
            sb.append('|');
        }
	    return sb.toString();
    }

	// logging
		
	public static void logError(String message) {
		if (message == null)
			message= ""; //$NON-NLS-1$
		log(new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, null));
	}

	public static void logWarning(String message) {
		if (message == null)
			message= ""; //$NON-NLS-1$
		log(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.OK, message, null));
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, "Internal Error", e)); //$NON-NLS-1$
	}
	
	public static void log(IStatus status) {
	    if (fgPlugin != null) {
	        fgPlugin.getLog().log(status);
	    } else {
	        switch (status.getSeverity()) {
	        case IStatus.ERROR:
		        System.err.println("Error: " + status.getMessage()); //$NON-NLS-1$
	            break;
	        case IStatus.WARNING:
		        System.err.println("Warning: " + status.getMessage()); //$NON-NLS-1$
	            break;
	        }
	        Throwable exception= status.getException();
	        if (exception != null)
	            exception.printStackTrace(System.err);
	    }
	}
}
