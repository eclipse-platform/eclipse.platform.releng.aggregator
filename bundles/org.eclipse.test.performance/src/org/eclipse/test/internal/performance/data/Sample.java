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
package org.eclipse.test.internal.performance.data;

import java.util.Map;

import junit.framework.Assert;

/**
 * @since 3.1
 */
public class Sample {
	String fScenarioID;
	long fStartTime;
	Map fProperties;
	DataPoint[] fDataPoints;
	
	public Sample(String scenarioID, long starttime, Map properties, DataPoint[] dataPoints) {
		Assert.assertTrue("scenarioID is null", scenarioID != null); //$NON-NLS-1$
		fScenarioID= scenarioID;
		fStartTime= starttime;
		fProperties= properties;
		fDataPoints= dataPoints;
	}
	
    public Sample(DataPoint[] dataPoints) {
        fDataPoints= dataPoints;
    }
    
	public String getScenarioID() {
	    return fScenarioID;
	}
	
    public long getStartTime() {
         return fStartTime;
    }
    
	public String getProperty(String name) {
		return (String) fProperties.get(name);
	}
	
	public DataPoint[] getDataPoints() {
		DataPoint[] dataPoints= new DataPoint[fDataPoints.length];
		System.arraycopy(fDataPoints, 0, dataPoints, 0, fDataPoints.length);
		return dataPoints;
	}
	
	public String toString() {
	    return "MeteringSession [scenarioID= " + fScenarioID + ", #datapoints: " + fDataPoints.length + "]";
	}
}
