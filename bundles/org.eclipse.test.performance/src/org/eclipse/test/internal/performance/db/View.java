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

import org.eclipse.test.internal.performance.data.Dim;

public class View {
    
    public static void main(String[] args) {
    
        // get all Scenarios 
        Scenario[] scenarios= DB.queryScenarios("%", "%", "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
        for (int s= 0; s < scenarios.length; s++) {
            Scenario t= scenarios[s];
            System.out.println("Scenario: " + t.getScenarioName()); //$NON-NLS-1$
            Report r= new Report(2);
            
            String[] timeSeriesLabels= t.getTimeSeriesLabels();
            r.addCell("Builds:"); //$NON-NLS-1$
            for (int j= 0; j < timeSeriesLabels.length; j++)
                r.addCellRight(timeSeriesLabels[j]);
            r.nextRow();
                        
            Dim[] dimensions= t.getDimensions();
            for (int i= 0; i < dimensions.length; i++) {
                Dim dim= dimensions[i];
                r.addCell(dim.getName() + ':');
                
                TimeSeries ts= t.getTimeSeries(dim);
                int n= ts.getLength();
                for (int j= 0; j < n; j++)
                    r.addCellRight(dim.getDisplayValue(ts.getValue(j)));
                r.nextRow();
            }
            r.print(System.out);
            System.out.println();
        }
    }
}
