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
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.Variations;
import org.eclipse.test.internal.performance.eval.StatisticsSession;
import org.eclipse.test.performance.PerformanceMeter;


public abstract class InternalPerformanceMeter extends PerformanceMeter {

    public static final int AVERAGE= -3;
    public static final int BEFORE= 0;
    public static final int AFTER= 1;
    
    private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults"; //$NON-NLS-1$

	private String fScenarioId;

	
	public InternalPerformanceMeter(String scenarioId) {
	    fScenarioId= scenarioId;
    }

	public void dispose() {
	    fScenarioId= null;
	}

    public abstract Sample getSample();

	/**
	 * Answer the scenario ID.
	 * @return the scenario ID
	 */
	public String getScenarioName() {
		return fScenarioId;
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#commit()
	 */
	public void commit() {
	    Sample sample= getSample();
	    if (sample != null) {
	        Variations variations= PerformanceTestPlugin.getVariations();
	        if (variations != null)
	            DB.store(variations, sample);
	        if (!DB.isActive() || System.getProperty(VERBOSE_PERFORMANCE_METER_PROPERTY) != null)
	        	printSample(System.out, sample);
	    }
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
}
