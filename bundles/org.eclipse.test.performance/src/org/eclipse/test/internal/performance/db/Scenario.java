/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.db;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.eval.StatisticsSession;

/**
 * @since 3.1
 */
public class Scenario {

	private String fConfigPattern;
	private String fBuildPattern;
    private String fScenarioName;
    private String[] fBuildNames;
    private StatisticsSession[] fSessions;
    private Dim[] fDimensions;
    private Dim[] fQueryDimensions;
    private Map fSeries= new HashMap();

    
    Scenario(String configPattern, String buildPattern, String scenario, Dim[] dimensions) {
    	fConfigPattern= configPattern;
    	fBuildPattern= buildPattern;
        fScenarioName= scenario;
        fQueryDimensions= dimensions;
    }
    
    public String getScenarioName() {
        return fScenarioName;
    }

    public Dim[] getDimensions() {
        load();
        if (fDimensions == null)
        	return new Dim[0];
        return fDimensions;
    }
    
    public String[] getTimeSeriesLabels() {
        load();
        if (fBuildNames == null)
        	return new String[0];
        return fBuildNames;
    }
    
    public TimeSeries getTimeSeries(Dim dim) {
        load();
        TimeSeries ts= (TimeSeries) fSeries.get(dim);
        if (ts == null) {
            double[] ds= new double[fSessions.length];
            double[] sd= new double[fSessions.length];
            for (int i= 0; i < ds.length; i++) {
                ds[i]= fSessions[i].getAverage(dim);
                sd[i]= fSessions[i].getStddev(dim);                
            }
            ts= new TimeSeries(fBuildNames, ds, sd);
            fSeries.put(dim, ts);
        }
        return ts;
    }
    
    //---- private
    
    private void load() {
        if (fBuildNames != null)
            return;
        InternalDimensions.COMITTED.getId();	// trigger loading class InternalDimensions
        
        fBuildNames= DB.queryBuildNames(fConfigPattern, fBuildPattern, fScenarioName);
        fSessions= new StatisticsSession[fBuildNames.length];
        
        Set dims= new HashSet();
        for (int t= 0; t < fBuildNames.length; t++) {
            DataPoint[] dps= DB.queryDataPoints(fConfigPattern, fBuildNames[t], fScenarioName, fQueryDimensions);
            if (dps.length > 0)
            	dims.addAll(dps[0].getDimensions2());
            fSessions[t]= new StatisticsSession(dps);
        }
        fDimensions= (Dim[]) dims.toArray(new Dim[dims.size()]);
        Arrays.sort(fDimensions,
            new Comparator() {
            	public int compare(Object o1, Object o2) {
            	    Dim d1= (Dim)o1;
            	    Dim d2= (Dim)o2;
            	    return d1.getName().compareTo(d2.getName());
            	}
        	}
        );
    }
}
