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

public class TimeSeries {
    
    private String[] fBuildNames;
    private double[] fAverages;
    private double[] fStddev;
    
    TimeSeries(String[] tags, double[] averages, double[] stddev) {
        fBuildNames= tags;
        fAverages= averages;
        fStddev= stddev;
    }
    
    /**
     * Returns length of series.
     * @return length of series
     */
    public int getLength() {
        return fBuildNames.length;
    }
    
    /**
     * Returns value at given index.
     * @param ix 
     * @return value at given index
     */
    public double getValue(int ix) {
        return fAverages[ix];
    }
    
    /**
     * Returns std dev at given index.
     * @param ix 
     * @return std dev at given index
     */
    public double getStddev(int ix) {
        return fStddev[ix];
    }
    
    /**
     * Returns label at given index.
     * @param ix
     * @return label at given index
     */
    public String getLabel(int ix) {
        return fBuildNames[ix];
    }
}
