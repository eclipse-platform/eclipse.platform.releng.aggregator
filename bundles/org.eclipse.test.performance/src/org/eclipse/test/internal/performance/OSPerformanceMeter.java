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
import java.util.Properties;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.eval.StatisticsSession;


/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

	private PerformanceMonitor fPerformanceMonitor;
	private long fStartTime;
	private List fDataPoints= new ArrayList();
	
    private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults"; //$NON-NLS-1$
    
	
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
	        Properties config= PerformanceTestPlugin.getConfig();
	        if (config != null)
	            DB.store(config, sample);
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
	        return new Sample(getScenarioName(), fStartTime, runProperties, (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
	    }
	    return null;
	}
	
	//---- private stuff ------
	
    private void snapshot(int step) {
	    HashMap map= new HashMap();
	    fPerformanceMonitor.collectOperatingSystemCounters(map);
	    fDataPoints.add(new DataPoint(step, map));
    }

	private void printSample(PrintStream ps, Sample sample) {
		ps.print("Scenario '" + getScenarioName() + "' "); //$NON-NLS-1$ //$NON-NLS-2$
		DataPoint[] dataPoints= sample.getDataPoints();
		if (dataPoints.length > 0) {
			StatisticsSession s= new StatisticsSession(dataPoints);
			Dim[] dimensions= dataPoints[0].getDimensions();
			if (dimensions.length > 0) {
				ps.println("(average over " + s.getCount(dimensions[0]) + " samples):"); //$NON-NLS-1$ //$NON-NLS-2$
				for (int i= 0; i < dimensions.length; i++) {
				    Dim dimension= dimensions[i];
				    ps.println("  " + dimension.getName() + ": " + dimension.getDisplayValue(s.getAverage(dimension))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		ps.println();
	}

	/**
	 * Write out the run element if it hasn't been written out yet.
	 * @param runProperties
	 */
	private void collectRunInfo(HashMap runProperties) {
        fPerformanceMonitor.collectGlobalPerformanceInfo(runProperties);
	}
}
