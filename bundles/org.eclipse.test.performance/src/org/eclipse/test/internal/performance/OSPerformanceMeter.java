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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.db.DB;


/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

	private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults";

	private PerformanceMonitor fPerformanceMonitor;
	private List fDataPoints= new ArrayList();
    
	private static Runtime fgRuntime= Runtime.getRuntime();

	
	/**
	 * @param scenarioId the scenario id
	 */
	public OSPerformanceMeter(String scenarioId) {
	    super(scenarioId);
		fPerformanceMonitor= PerformanceMonitor.getPerformanceMonitor();
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
	 */
	public void dispose() {
	    fPerformanceMonitor= null;
	    fDataPoints= null;
		super.dispose();
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#start()
	 */
	public void start() {
		snapshot(BEFORE);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	public void stop() {
		snapshot(AFTER);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#commit()
	 */
	public void commit() {
	    
	    if (false) {
		    DB.store(getScenarioName(), getSample());
	    }
	    
		if (System.getProperty(VERBOSE_PERFORMANCE_METER_PROPERTY) != null)
			printSample();
	}

	/*
	 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#getSample()
	 */
	public Sample getSample() {
	    
	    PerformanceTestPlugin.processEnvironmentVariables();

	    if (fDataPoints != null) {
	            
	        HashMap runProperties= new HashMap();
	        
	        collectRunInfo(runProperties);
	        
			fPerformanceMonitor.collectGlobalPerformanceInfo(runProperties);
			    
	        return new Sample(runProperties, (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
	    }
	    return null;
	}
	
	//---- private stuff ------
	
    private void snapshot(String step) {
	    HashMap map= new HashMap();
	    
	    if (true) {
			fgRuntime.gc();
			long used= fgRuntime.totalMemory() - fgRuntime.freeMemory();
			addScalar(map, Dimensions.USED_JAVA_HEAP, used);
		}

	    fPerformanceMonitor.collectOperatingSystemCounters(map);
	    fDataPoints.add(new DataPoint(step, map));
    }

    private void addScalar(Map scalars, Dimension dimension, long value) {
        scalars.put(dimension, new Scalar(dimension, value));
    }    

	private void printSample() {
		Sample sample= getSample();
		if (sample != null) {
			Map averages= new HashMap();
			DataPoint[] dataPoints= sample.getDataPoints();
			for (int i= 0, n= dataPoints.length; i < n - 1; i += 2) {
				Scalar[] start= dataPoints[i].getScalars();
				Scalar[] stop= dataPoints[i + 1].getScalars();
				for (int j= 0, m= Math.min(start.length, stop.length); j < m; j++) {
					Dimension dimension= start[j].getDimension();
					if (dimension.equals(stop[j].getDimension())) {
						long delta= stop[j].getMagnitude() - start[j].getMagnitude();
						Double value= (Double) averages.get(dimension);
						double oldAvg= value != null ? value.doubleValue() : 0.0;
						double newAvg= oldAvg + (delta - oldAvg)/(i/2 + 1);
						averages.put(dimension, new Double(newAvg));
					} else
						System.out.println("OSPerformanceMeter.toDisplayString(): Dimensions do not match");
				}
			}
			System.out.println(getScenarioName() + ":");
			for (Iterator iter= averages.keySet().iterator(); iter.hasNext();) {
				Dimension dimension= (Dimension) iter.next();
				double avgDelta= ((Double) averages.get(dimension)).doubleValue();
				String name= dimension.getName() + " [" + dimension.getUnit().getShortName() + "]";
				System.out.println(name + ":\t" + avgDelta);
			}
		}
	}

	/**
	 * Write out the run element if it hasn't been written out yet.
	 */
	private void collectRunInfo(HashMap runProperties) {
	    
        runProperties.put(DRIVER_PROPERTY, getBuildId());
        runProperties.put(HOSTNAME_PROPERTY, getHostName());
        runProperties.put(RUN_TS_PROPERTY, new Long(System.currentTimeMillis()));
        runProperties.put(TESTNAME_PROPERTY, getScenarioName());
	    	    
		String version= System.getProperty("java.fullversion");
		if (version == null)
		    version= System.getProperty("java.runtime.version");
	    runProperties.put("jvm", version);
	    				
		StringBuffer b= new StringBuffer(400);
		b.append("eclipse.vmargs=");
			b.append(System.getProperty("eclipse.vmargs"));
		b.append(" eclipse.commands=");
			b.append(System.getProperty("eclipse.commands"));
		runProperties.put("cmdArgs", b.toString());		
	}	
}
