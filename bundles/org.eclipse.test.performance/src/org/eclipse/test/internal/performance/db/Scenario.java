/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.db;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.eval.StatisticsSession;
import org.junit.Assert;

/**
 * A Scenario contains a series of data points for a single scenario. The axis of the data points can be specified when creating a
 * scenario. Typical examples are: - datapoints corresponding to different builds - datapoints corresponding to different OSes -
 * datapoints corresponding to different JVMs
 *
 * @since 3.1
 */
public class Scenario {

    private static final boolean DEBUG = false;

    public static class SharedState {

        private Variations fVariations;
        private String     fSeriesKey;
        private Set<Dim>        fQueryDimensions;
        private String     fScenarioPattern;
        private Map<String, Map<String, String>>        fMessages;

        SharedState(Variations variations, String scenarioPattern, String seriesKey, Dim[] dimensions) {
            fVariations = variations;
            fScenarioPattern = scenarioPattern;
            fSeriesKey = seriesKey;
            if (dimensions != null && dimensions.length > 0) {
                fQueryDimensions = new HashSet<>();
                for (Dim dimension : dimensions)
                    fQueryDimensions.add(dimension);
            }
        }

        String[] getFailures(String[] names, String scenarioId) {
            if (fMessages == null) {
                fMessages = new HashMap<>();
                Variations v = (Variations) fVariations.clone();
                for (String name : names) {
                    v.put(fSeriesKey, name);
                    Map<String, String> map = DB.queryFailure(fScenarioPattern, v);
                    fMessages.put(name, map);
                }
            }
            String[] result = new String[names.length];
            for (int i = 0; i < names.length; i++) {
                Map<String, String> messages = fMessages.get(names[i]);
                if (messages != null)
                    result[i] = messages.get(scenarioId);
            }
            return result;
        }
    }

    private SharedState         fSharedState;
    private String              fScenarioName;
    private String[]            fSeriesNames;
    private StatisticsSession[] fSessions;
    private Map<Dim, TimeSeries>                 fSeries = new HashMap<>();
    private Dim[]               fDimensions;

    /**
     * @param scenario
     * @param variations
     * @param seriesKey
     * @param dimensions
     * @deprecated
     */
    @Deprecated
    public Scenario(String scenario, Variations variations, String seriesKey, Dim[] dimensions) {
        Assert.assertFalse(scenario.indexOf('%') >= 0);
        fScenarioName = scenario;
        fSharedState = new SharedState(variations, scenario, seriesKey, dimensions);
    }

    /**
     * @param scenario
     * @param sharedState
     */
    public Scenario(String scenario, SharedState sharedState) {
        Assert.assertFalse(scenario.indexOf('%') >= 0);
        fScenarioName = scenario;
        fSharedState = sharedState;
    }

    public String getScenarioName() {
        return fScenarioName;
    }

    public Dim[] getDimensions() {
        loadSessions();
        if (fDimensions == null)
            return new Dim[0];
        return fDimensions;
    }

    public String[] getTimeSeriesLabels() {
        loadSeriesNames();
        if (fSeriesNames == null)
            return new String[0];
        return fSeriesNames;
    }

    public String[] getFailureMessages() {
        loadSeriesNames();
        return fSharedState.getFailures(fSeriesNames, fScenarioName);
    }

    public TimeSeries getTimeSeries(Dim dim) {
        loadSessions();
        TimeSeries ts = fSeries.get(dim);
        if (ts == null) {
            double[] ds = new double[fSessions.length];
            double[] sd = new double[fSessions.length];
            long[] sizes = new long[fSessions.length];
            for (int i = 0; i < ds.length; i++) {
                ds[i] = fSessions[i].getAverage(dim);
                sd[i] = fSessions[i].getStddev(dim);
                sizes[i] = fSessions[i].getCount(dim);
            }
            ts = new TimeSeries(fSeriesNames, ds, sd, sizes);
            fSeries.put(dim, ts);
        }
        return ts;
    }

    public void dump(PrintStream ps, String key) {
        ps.println("Scenario: " + getScenarioName()); //$NON-NLS-1$
        Report r = new Report(2);

        String[] timeSeriesLabels = getTimeSeriesLabels();
        r.addCell(key + ":"); //$NON-NLS-1$
        for (String timeSeriesLabel : timeSeriesLabels)
            r.addCellRight(timeSeriesLabel);
        r.nextRow();

        Dim[] dimensions = getDimensions();
        for (Dim dim : dimensions) {
            r.addCell(dim.getName() + ':');

            TimeSeries ts = getTimeSeries(dim);
            int n = ts.getLength();
            for (int j = 0; j < n; j++) {
                String stddev = ""; //$NON-NLS-1$
                double stddev2 = ts.getStddev(j);
                if (stddev2 != 0.0)
                    stddev = " [" + dim.getDisplayValue(stddev2) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                r.addCellRight(dim.getDisplayValue(ts.getValue(j)) + stddev);
            }
            r.nextRow();
        }
        r.print(ps);
        ps.println();
    }

    // ---- private

    private void loadSeriesNames() {
        if (fSeriesNames == null) {
            long start;
            if (DEBUG)
                start = System.currentTimeMillis();
            fSeriesNames = DB.querySeriesValues(fScenarioName, fSharedState.fVariations, fSharedState.fSeriesKey);
            if (DEBUG)
                System.err.println("names: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
        }
    }

    private void loadSessions() {
        if (fSessions != null)
            return;

        loadSeriesNames();

        long start;
        Variations v = (Variations) fSharedState.fVariations.clone();
        if (DEBUG)
            start = System.currentTimeMillis();
        ArrayList<StatisticsSession> sessions = new ArrayList<>();
        ArrayList<String> names2 = new ArrayList<>();
        Set<Dim> dims = new HashSet<>();
        for (String fSeriesName : fSeriesNames) {
            v.put(fSharedState.fSeriesKey, fSeriesName);
            DataPoint[] dps = DB.queryDataPoints(v, fScenarioName, fSharedState.fQueryDimensions);
            if (DEBUG)
                System.err.println("  dps length: " + dps.length); //$NON-NLS-1$
            if (dps.length > 0) {
                dims.addAll(dps[0].getDimensions2());
                sessions.add(new StatisticsSession(dps));
                names2.add(fSeriesName);
            }
        }
        if (DEBUG)
            System.err.println("data: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$

        fSessions = sessions.toArray(new StatisticsSession[sessions.size()]);
        fSeriesNames = names2.toArray(new String[sessions.size()]);

        fDimensions = dims.toArray(new Dim[dims.size()]);
        Arrays.sort(fDimensions, (o1, o2) -> {
            Dim d1 = o1;
            Dim d2 = o2;
            return d1.getName().compareTo(d2.getName());
        });
    }
}
