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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.eclipse.test.internal.performance.data.Dim;

public class View {
    
    public static void main(String[] args) {
		
		String outFile= null;
		// outfile= "/tmp/dbdump";	//$NON-NLS-1$
		PrintStream ps= null;
		if (outFile != null) {
		    try {
                ps= new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            } catch (FileNotFoundException e) {
                System.err.println("can't create output file"); //$NON-NLS-1$
            }
		}
		if (ps == null)
		    ps= System.out;
		
        // get all Scenarios 
        Dim[] qd= null; // new Dim[] { InternalDimensions.CPU_TIME };
        Scenario[] scenarios= DB.queryScenarios("%", "%", "%", qd); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        ps.println(scenarios.length + " Scenarios"); //$NON-NLS-1$
        ps.println();
        
        for (int s= 0; s < scenarios.length; s++) {
            Scenario t= scenarios[s];
            ps.println("Scenario " + s + ": " + t.getScenarioName()); //$NON-NLS-1$ //$NON-NLS-2$
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
                for (int j= 0; j < n; j++) {
                    String stddev= " [" + dim.getDisplayValue(ts.getStddev(j)) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                    r.addCellRight(dim.getDisplayValue(ts.getValue(j)) + stddev);
                }
                r.nextRow();
            }
            r.print(ps);
            ps.println();
        }
        if (ps != System.out)
            ps.close();
    }
}
