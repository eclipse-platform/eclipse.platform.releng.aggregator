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

import org.eclipse.perfmsr.core.IPerformanceMonitor;
import org.eclipse.perfmsr.core.PerfMsrCorePlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.PerfMsrDimensions;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;


/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

	/**
	 * The perfmsr plug-in's performance monitor
	 */
	private IPerformanceMonitor fPerformanceMonitor;
	
	private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults";

	private String fScenarioId;
	
	/**
	 * @param scenarioId the scenario id
	 */
	public OSPerformanceMeter(String scenarioId) {
		fPerformanceMonitor= PerfMsrCorePlugin.getPerformanceMonitor(false);
		fPerformanceMonitor.setTestName(scenarioId);
		fScenarioId= scenarioId;
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#start()
	 */
	public void start() {
		fPerformanceMonitor.snapshot(1);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	public void stop() {
		fPerformanceMonitor.snapshot(2);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#commit()
	 */
	public void commit() {
		fPerformanceMonitor.upload();
	    
		if (System.getProperty(VERBOSE_PERFORMANCE_METER_PROPERTY) != null) {
			System.out.println(fScenarioId + ":");
			Sample sample= getSample();
			if (sample != null) {
				DataPoint[] dataPoints= sample.getDataPoints();
				for (int i= 0, n= dataPoints.length; i < n - 1; i += 2) {
					System.out.println("Iteration " + (i / 2 + 1) + ":");
					Scalar[] before= dataPoints[i].getScalars();
					Scalar[] after= dataPoints[i + 1].getScalars();
					for (int j= 0, m= Math.min(before.length, after.length); j < m; j++) {
						long valueBefore= before[j].getMagnitude();
						long valueAfter= after[j].getMagnitude();
						String dimensionId= before[j].getDimension();
						Dimension dimension= PerfMsrDimensions.getDimension(dimensionId);
						String name= dimension != null ? dimension.getName() + " [" + dimension.getUnit().getShortName() + "]" : dimensionId;
//						System.out.println(name + ":\t" + valueBefore + "\t" + valueAfter + "\t" + (valueAfter - valueBefore));
						System.out.println(name + ":\t" + (valueAfter - valueBefore));
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
	 */
	public void dispose() {
		fPerformanceMonitor= null;
	}

	/*
	 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#getSample()
	 */
	public Sample getSample() {
	    if (fPerformanceMonitor != null)
	        return fPerformanceMonitor.getSample();
	    return null;
	}
}
