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
package org.eclipse.perfmsr.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.PerfMsrDimensions;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;


public abstract class PerformanceMonitor implements IPerformanceMonitor {
    
    protected HashMap fRunProperties;
    protected List fDataPoints;
    private String fHostName;
	private static DateFormat fDateFormat= DateFormat.getDateTimeInstance();
	private String fScenarioName;


    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#setLogFile(java.lang.String)
     */
    public void setLogFile(String logFile) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#setTestName(java.lang.String)
     */
    public void setTestName(String scenarioId) {
        fScenarioName= scenarioId;
    }

    private boolean writeable() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#snapshot(int)
     */
    public void snapshot(int step) {
		if (writeable()) {
		    
		    if (fRunProperties == null) {
		        fRunProperties= new HashMap();
		        writeRun(fRunProperties);
		    }
		    
		    if (fDataPoints == null)
		        fDataPoints= new ArrayList();
		    HashMap map= new HashMap();
		    writeOperatingSystemCounters(map);
		    DataPoint dp= new DataPoint(Integer.toString(step), map);
		    fDataPoints.add(dp);
		}
    }

	/**
	 * Write out the run element if it hasn't been written out yet.
	 */
	protected void writeRun(HashMap runProperties) {
	    
//		if (_unknownEnvironmentParms != null) {
//			PrintWriter pw = getPrintWriter();
//			pw.print("<!-- The following environment values were ignored (they probably are typo's): ");
//			pw.print(_unknownEnvironmentParms.toString());
//			pw.println(" -->");
//			_unknownEnvironmentParms = null;
//		}
		
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
	}
	
	private static String getUUID() {
//		if (!isLoaded())
		    return getRandomUUID();
//		try {
//			return nativeGetUUID();
//		} catch (Exception e) {
//			return getRandomUUID();
//		}
	}
	
	/**
	 * This is my poor man's UUID, which I use if I can't get one from the operating system.
	 */
	private static String getRandomUUID() {
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

    protected void writeOperatingSystemCounters(Map scalars) {
        String dimName= PerfMsrDimensions.USER_TIME.getName();
        scalars.put(dimName, new Scalar(dimName, System.currentTimeMillis()));
    }

	protected void writeGlobalPerformanceInfo(int step, boolean displayResults, StringBuffer b) {
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.perfmsr.core.IPerformanceMonitor#upload(java.lang.Object)
     */
    public void upload() {
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
	static PerformanceMonitor create()
	{
	    String os= System.getProperty("os.name");
		if (os.startsWith("Windows"))
			return new PerformanceMonitorWindows();
		
		if (os.startsWith("Mac OS X"))
		    return new PerformanceMonitorMac();
	
		return new PerformanceMonitorLinux();
	}	

    /**
     * @param string
     */
    public static void debug(String string) {
        // TODO Auto-generated method stub
    }
}
