/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.test.internal.performance.PerformanceMonitorLinux;
import org.eclipse.test.internal.performance.PerformanceMonitorMac;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;


class PerformanceMonitor {
    
	private HashMap fRunProperties;
	private List fDataPoints;
    private String fHostName;
	private String fScenarioName;

	private static DateFormat fDateFormat= DateFormat.getDateTimeInstance();
    private static PerformanceMonitor fgPerformanceMonitor;

    
    public static PerformanceMonitor getPerformanceMonitor(boolean shared) {
		PerformanceMonitor pm;
		if (!shared)
		    pm= create();
		else {
			if (fgPerformanceMonitor == null)
				fgPerformanceMonitor= create();
			pm= fgPerformanceMonitor;
		}
		return pm;
    }

    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#setTestName(java.lang.String)
     */
    public void setTestName(String scenarioId) {
        fScenarioName= scenarioId;
    }

    private boolean isEnabled() {
    	// we are not filtering anything yet...
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#snapshot(int)
     */
    public void snapshot(int step) {
		if (isEnabled()) {
		    
		    if (fRunProperties == null) {
		        fRunProperties= new HashMap();
		        collectRunInfo(fRunProperties);
		    }
		    
		    if (fDataPoints == null)
		        fDataPoints= new ArrayList();
		    HashMap map= new HashMap();
		    collectOperatingSystemCounters(map);
		    DataPoint dp= new DataPoint(Integer.toString(step), map);
		    fDataPoints.add(dp);
		}
    }

	/**
	 * Write out the run element if it hasn't been written out yet.
	 */
	private void collectRunInfo(HashMap runProperties) {
	    
	    runProperties.put("uuid", getUUID());
	    runProperties.put("driver", "driver?");
	    runProperties.put("driverdate", "driverdate?");
	    runProperties.put("driverlabel", "driverlabel?");
	    runProperties.put("driverStream", "driverStream?");
	    
		String version= System.getProperty("java.fullversion");
		if (version == null)
		    version= System.getProperty("java.runtime.version");
	    runProperties.put("jvm", version);

	    runProperties.put("host", getHost());

		long now= System.currentTimeMillis();
	    runProperties.put("runTS", String.valueOf(now));
	    runProperties.put("displayRunTS", fDateFormat.format(new Date(now)));
	    		
//		writeAttribute(TimerXML.var, getVarInEffect(), pw);
//		writeAttribute(TimerXML.version, GK_VERSION, pw);
		
	    if (fScenarioName != null)
		    runProperties.put("testname", fScenarioName);
	
		StringBuffer b= new StringBuffer(400);
		b.append("eclipse.vmargs=");
		b.append(System.getProperty("eclipse.vmargs"));
		b.append(" eclipse.commands=");
		b.append(System.getProperty("eclipse.commands"));
		runProperties.put("cmdArgs", b.toString());
		
		collectGlobalPerformanceInfo(runProperties);
	}
	
	protected String getUUID() {
		// This is my poor man's UUID, which I use if I can't get one from the operating system.
		Random r= new Random();
		String s1= Long.toHexString(r.nextLong());
		String s2= Long.toHexString(r.nextLong());
		return s1.substring(0,8) + "-" + s1.substring(8,12) + "-" + s1.substring(12) +
								"-" + s2.substring(0,4) + "-" + s2.substring(4);
	}

	/**
	 * Answer the name of the host that we are running on.
	 */
	private String getHost() {
	    if (fHostName == null) {
			try {
				InetAddress addr= InetAddress.getLocalHost();
				fHostName= addr.getHostName().toLowerCase();
			} catch (UnknownHostException e) {
				String possibleHost= e.getMessage();
				if (possibleHost != null)
				    fHostName= possibleHost;
			} catch (Exception e) {
				// PerfMsrCorePlugin.writeLog(e);
			}
	    }
		return fHostName;
	}

    public Sample getSample() {
        if (fDataPoints != null)
            return new Sample(fRunProperties, (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
        return null;
    }

    protected void collectOperatingSystemCounters(Map scalars) {
        // default implementation just writes currentTimeMillis
        addScalar(scalars, Dimensions.USER_TIME, System.currentTimeMillis());
    }

	protected void collectGlobalPerformanceInfo(Map scalars) {
		// no default implementation
	}
	
    void addScalar(Map scalars, Dimension dimension, long value) {
        // System.out.println(dimension.getName() + ": " + value);
        scalars.put(dimension, new Scalar(dimension, value));
    }
    
	/**
	 * In the normal case, only a single PerformanceMonitor would be allocated, so
	 * plug-ins need to be careful that any one plugin doesn't allocated
	 * more than one timer.
	 *
	 * Use the getPerformanceMonitor method to get instances of this class.
	 * 
	 * @see PerfMsrCorePlugin#getPerformanceMonitor(boolean) 
	 */
	private static PerformanceMonitor create() {
	    String os= System.getProperty("os.name");
		if (os.startsWith("Windows"))
		    return new PerformanceMonitorWindows();
		if (os.startsWith("Mac OS X"))
		    return new PerformanceMonitorMac();
		return new PerformanceMonitorLinux();
	}
}
