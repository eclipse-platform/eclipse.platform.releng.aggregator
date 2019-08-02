/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.test.internal.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

public class SystemTimePerformanceMeter extends InternalPerformanceMeter {

    private static final int DEFAULT_INITIAL_CAPACITY = 3;

    private long             fStartDate;
    private List<Long>             fStartTime;
    private List<Long>             fStopTime;

    /**
     * @param scenarioId
     *            the scenario id
     */
    public SystemTimePerformanceMeter(String scenarioId) {
        this(scenarioId, DEFAULT_INITIAL_CAPACITY);
        fStartDate = System.currentTimeMillis();
    }

    /**
     * @param scenarioId
     *            the scenario id
     * @param initalCapacity
     *            the initial capacity in the number of measurments
     */
    public SystemTimePerformanceMeter(String scenarioId, int initalCapacity) {
        super(scenarioId);
        fStartTime = new ArrayList<>(initalCapacity);
        fStopTime = new ArrayList<>(initalCapacity);
    }

    @Override
    public void start() {
        fStartTime.add(Long.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void stop() {
        fStopTime.add(Long.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void commit() {
        Assert.isTrue(fStartTime.size() == fStopTime.size());
        System.out.println("Scenario: " + getScenarioName()); //$NON-NLS-1$
        int maxOccurenceLength = String.valueOf(fStartTime.size()).length();
        for (int i = 0; i < fStartTime.size(); i++) {
            String occurence = String.valueOf(i + 1);
            System.out
                    .println("Occurence " + replicate(" ", maxOccurenceLength - occurence.length()) + occurence + ": " + (fStopTime.get(i).longValue() - fStartTime.get(i).longValue())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private String replicate(String s, int n) {
        StringBuffer buf = new StringBuffer(n * s.length());
        for (int i = 0; i < n; i++)
            buf.append(s);
        return buf.toString();
    }

    @Override
    public void dispose() {
        fStartTime = null;
        fStopTime = null;
        super.dispose();
    }

    @Override
    public Sample getSample() {
        Assert.isTrue(fStartTime.size() == fStopTime.size());

        Map<String, String> properties = new HashMap<>();
        /*
         * properties.put(DRIVER_PROPERTY, PerformanceTestPlugin.getBuildId()); properties.put(HOSTNAME_PROPERTY, getHostName());
         */

        DataPoint[] data = new DataPoint[2 * fStartTime.size()];
        for (int i = 0; i < fStartTime.size(); i++) {
            data[2 * i] = createDataPoint(BEFORE, InternalDimensions.SYSTEM_TIME, fStartTime.get(i).longValue());
            data[2 * i + 1] = createDataPoint(AFTER, InternalDimensions.SYSTEM_TIME, fStopTime.get(i).longValue());
        }

        return new Sample(getScenarioName(), fStartDate, properties, data);
    }

    private DataPoint createDataPoint(int step, Dim dimension, long value) {
        Map<Dim, Scalar> scalars = new HashMap<>();
        scalars.put(dimension, new Scalar(dimension, value));
        return new DataPoint(step, scalars);
    }
}
