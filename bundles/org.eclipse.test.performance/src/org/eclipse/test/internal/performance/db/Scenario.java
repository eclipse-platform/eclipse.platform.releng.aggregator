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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.eval.StatisticsSession;

/**
 * @since 3.1
 */
public class Scenario {

    private String fScenarioName;
    private String[] fTags;
    private StatisticsSession[] fSessions;
    private Dim[] fDimensions;
    private Map fSeries= new HashMap();

    
    Scenario(String scenario) {
        fScenarioName= scenario;
    }
    
    public String getScenarioName() {
        return fScenarioName;
    }

    public Dim[] getDimensions() {
        load();
        return fDimensions;
    }
    
    public TimeSeries getTimeSeries(Dim dim) {
        load();
        TimeSeries ts= (TimeSeries) fSeries.get(dim);
        if (ts == null) {
            double[] ds= new double[fSessions.length];
            for (int i= 0; i < ds.length; i++)
                ds[i]= fSessions[i].getAverage(dim);
            ts= new TimeSeries(fTags, ds);
            fSeries.put(dim, ts);
        }
        return ts;
    }
    
    //---- private
    
    private void load() {
        if (fTags != null)
            return;
        int xxxx= InternalDimensions.COMITTED.getId();	// trigger loading class InternalDimensions
        
        fTags= DB.queryTags(fScenarioName);
        fSessions= new StatisticsSession[fTags.length];

        for (int t= 0; t < fTags.length; t++) {
            String tag= fTags[t];
            DataPoint[] dps= DB.query(tag, fScenarioName, null);
            if (fDimensions == null && dps.length > 0)
                fDimensions= dps[0].getDimensions();

            fSessions[t]= new StatisticsSession(dps);
        }        
    }
}
