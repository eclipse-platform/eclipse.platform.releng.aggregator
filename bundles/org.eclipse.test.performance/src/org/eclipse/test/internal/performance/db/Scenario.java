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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.eval.StatisticsSession;

/**
 * A Scenario contains a series of data points for a single scenario.
 * The axis of the data points can be specified when creating a scenario.
 * Typical examples are:
 * - datapoints corresponding to different builds
 * - datapoints corresponding to diferent JVMs
 * @since 3.1
 */
public class Scenario {
    
    private final static boolean DEBUG= false;

    private String fScenarioName;
    private Variations fVariations;
    private String fSeriesKey;
    private String[] fSeriesNames;
    private StatisticsSession[] fSessions;
    private Dim[] fDimensions;
    private Set fQueryDimensions;
    private Map fSeries= new HashMap();

   
    /** 
     * @param scenario
     * @param variations
     * @param seriesKey
     * @param dimensions
     */
    public Scenario(String scenario, Variations variations, String seriesKey, Dim[] dimensions) {
        Assert.assertFalse(scenario.indexOf('%') >= 0);
        fScenarioName= scenario;
        fVariations= variations;
        fSeriesKey= seriesKey;
        if (dimensions != null && dimensions.length > 0) {
            fQueryDimensions= new HashSet();
            for (int i= 0; i < dimensions.length; i++)
                fQueryDimensions.add(dimensions[i]);
        }
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
        if (fSeriesNames == null)
        	return new String[0];
        return fSeriesNames;
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
            ts= new TimeSeries(fSeriesNames, ds, sd);
            fSeries.put(dim, ts);
        }
        return ts;
    }
    
    public void dump(PrintStream ps) {
	    ps.println("Scenario: " + getScenarioName()); //$NON-NLS-1$
	    Report r= new Report(2);
	    
	    String[] timeSeriesLabels= getTimeSeriesLabels();
	    r.addCell("Builds:"); //$NON-NLS-1$
	    for (int j= 0; j < timeSeriesLabels.length; j++)
	        r.addCellRight(timeSeriesLabels[j]);
	    r.nextRow();
	                
	    Dim[] dimensions= getDimensions();
	    for (int i= 0; i < dimensions.length; i++) {
	        Dim dim= dimensions[i];
	        r.addCell(dim.getName() + ':');
	        
	        TimeSeries ts= getTimeSeries(dim);
	        int n= ts.getLength();
	        for (int j= 0; j < n; j++) {
	            String stddev= ""; //$NON-NLS-1$
	            double stddev2= ts.getStddev(j);
	            if (stddev2 != 0.0)
	            	stddev= " [" + dim.getDisplayValue(stddev2) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	            r.addCellRight(dim.getDisplayValue(ts.getValue(j)) + stddev);
	        }
	        r.nextRow();
	    }
	    r.print(ps);
	    ps.println();
    }
    
    //---- private
        
    private void load() {
        if (fSeriesNames != null)
            return;
        //InternalDimensions.COMITTED.getId();	// trigger loading class InternalDimensions
        
        long start;
        if (DEBUG) start= System.currentTimeMillis();
        String[] names= DB.querySeriesValues(fScenarioName, fVariations, fSeriesKey);
        if (DEBUG) System.err.println("names: " + (System.currentTimeMillis()-start)); //$NON-NLS-1$

        ArrayList sessions= new ArrayList();
        ArrayList names2= new ArrayList();
        
        Variations v= (Variations) fVariations.clone();
        if (DEBUG) start= System.currentTimeMillis();
        Set dims= new HashSet();
        for (int t= 0; t < names.length; t++) {
            v.put(fSeriesKey, names[t]);
            DataPoint[] dps= DB.queryDataPoints(v, fScenarioName, fQueryDimensions);
            if (DEBUG) System.err.println("  dps length: " + dps.length); //$NON-NLS-1$
            if (dps.length > 0) {
            	dims.addAll(dps[0].getDimensions2());
            	sessions.add(new StatisticsSession(dps));
            	names2.add(names[t]);
            }
        }
        if (DEBUG) System.err.println("data: " + (System.currentTimeMillis()-start)); //$NON-NLS-1$

        fSessions= (StatisticsSession[]) sessions.toArray(new StatisticsSession[sessions.size()]);
        fSeriesNames= (String[]) names2.toArray(new String[sessions.size()]);
        
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
