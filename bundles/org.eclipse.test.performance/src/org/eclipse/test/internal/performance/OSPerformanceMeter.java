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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.eval.StatisticsSession;


/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

	private PerformanceMonitor fPerformanceMonitor;
	private long fStartTime;
	private List fDataPoints= new ArrayList();
	
    private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults";
    
	
	/**
	 * @param scenarioId the scenario id
	 */
	public OSPerformanceMeter(String scenarioId) {
	    super(scenarioId);
		fPerformanceMonitor= PerformanceMonitor.getPerformanceMonitor();
		fStartTime= System.currentTimeMillis();
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
	    Sample sample= getSample();
	    if (sample != null) {
	        DB.store(sample);
	        if (!DB.isActive() || System.getProperty(VERBOSE_PERFORMANCE_METER_PROPERTY) != null)
    				printSample(System.out, sample);
	    }
	}

	/*
	 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#getSample()
	 */
	public Sample getSample() {
	    
	    if (fDataPoints != null) {
	        HashMap runProperties= new HashMap();
	        collectRunInfo(runProperties);
	        fPerformanceMonitor.collectGlobalPerformanceInfo(runProperties);
	        return new Sample(getScenarioName(), fStartTime, runProperties, (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
	    }
	    return null;
	}
	
	//---- private stuff ------
	
    private void snapshot(int step) {
	    HashMap map= new HashMap();
	    
	    if (true) {
	    		Runtime runtime= Runtime.getRuntime();
			runtime.gc();
			long used= runtime.totalMemory() - runtime.freeMemory();
			addScalar(map, Dimensions.USED_JAVA_HEAP, used);
		}

	    fPerformanceMonitor.collectOperatingSystemCounters(map);
	    fDataPoints.add(new DataPoint(step, map));
    }

    private void addScalar(Map scalars, Dimension dimension, long value) {
        scalars.put(dimension, new Scalar(dimension, value));
    }    

	private void printSample(PrintStream ps, Sample sample) {
		ps.print("Scenario '" + getScenarioName() + "' ");
		DataPoint[] dataPoints= sample.getDataPoints();
		if (dataPoints.length > 0) {
			StatisticsSession s= new StatisticsSession(dataPoints);
			Dimension[] dimensions= dataPoints[0].getDimensions();
			if (dimensions.length > 0) {
				ps.println("(average over " + s.getCount(dimensions[0]) + " samples):");
				for (int i= 0; i < dimensions.length; i++) {
				    Dimension dimension= dimensions[i];
				    ps.println("  " + dimension.getName() + ": " + dimension.getDisplayValue(s.getAverage(dimension)));
				}
			}
		}
		ps.println();
	}

	/**
	 * Write out the run element if it hasn't been written out yet.
	 */
	private void collectRunInfo(HashMap runProperties) {
	    
        runProperties.put(DRIVER_PROPERTY, PerformanceTestPlugin.getBuildId());
        runProperties.put(HOSTNAME_PROPERTY, getHostName());

        /*
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
		*/
	}
}
