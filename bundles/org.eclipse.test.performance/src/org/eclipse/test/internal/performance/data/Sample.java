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
import java.util.Set;

import org.eclipse.test.performance.Dimension;

import junit.framework.Assert;

/**
 * @since 3.1
 */
public class Sample {
	String fScenarioID;
	long fStartTime;
	Map fProperties;
	DataPoint[] fDataPoints;
	boolean fIsSummary;
	String fShortName;
	Dimension[] fSummaryDimensions;
	
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
    
    public void tagAsSummary(String shortName, Dimension[] summaryDimensions) {
        fIsSummary= true;
        fShortName= shortName;
        fSummaryDimensions= summaryDimensions;
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
	
	public String[] getPropertyKeys() {
	    if (fProperties == null)
	        return new String[0];
	    Set set= fProperties.keySet();
	    return (String[]) set.toArray(new String[set.size()]);
	}
	
	public DataPoint[] getDataPoints() {
		DataPoint[] dataPoints= new DataPoint[fDataPoints.length];
		System.arraycopy(fDataPoints, 0, dataPoints, 0, fDataPoints.length);
		return dataPoints;
	}
	
	public String toString() {
	    return "MeteringSession [scenarioID= " + fScenarioID + ", #datapoints: " + fDataPoints.length + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

    public boolean isSummary() {
        return fIsSummary;
    }
    
    public boolean isGlobal() {
        return true;
    }
    
    public String getShortname() {
        return fShortName;
    }
    
    public Dimension[] getSummaryDimensions() {
        return fSummaryDimensions;
    }
}
