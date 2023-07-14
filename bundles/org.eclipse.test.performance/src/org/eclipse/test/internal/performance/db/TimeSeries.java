/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.db;

public class TimeSeries {

    private String[] fBuildNames;
    private double[] fAverages;
    private double[] fStddev;
    private long[]   fCount;

    TimeSeries(String[] tags, double[] averages, double[] stddev, long[] sizes) {
        fBuildNames = tags;
        fAverages = averages;
        fStddev = stddev;
        fCount = sizes;
    }

    /**
     * Returns length of series.
     *
     * @return length of series
     */
    public int getLength() {
        return fBuildNames.length;
    }

    /**
     * Returns value at given index.
     *
     * @param ix
     * @return value at given index
     */
    public double getValue(int ix) {
        return fAverages[ix];
    }

    /**
     * Returns std dev at given index.
     *
     * @param ix
     * @return std dev at given index
     */
    public double getStddev(int ix) {
        return fStddev[ix];
    }

    /**
     * Returns label at given index.
     *
     * @param ix
     * @return label at given index
     */
    public String getLabel(int ix) {
        return fBuildNames[ix];
    }

    /**
     * Returns the sample size at the given index.
     *
     * @param ix
     * @return the sample size at the given index
     */
    public long getCount(int ix) {
        return fCount[ix];
    }
}
