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

import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

    private PerformanceMonitor fPerformanceMonitor;
    private long               fStartTime;
    private List<DataPoint>               fDataPoints = new ArrayList<>();

    /**
     * @param scenarioId
     *            the scenario id
     */
    public OSPerformanceMeter(String scenarioId) {
        super(scenarioId);
        fPerformanceMonitor = PerformanceMonitor.getPerformanceMonitor();
        fStartTime = System.currentTimeMillis();
    }

    @Override
    public void dispose() {
        fPerformanceMonitor = null;
        fDataPoints = null;
        super.dispose();
    }

    @Override
    public void start() {
        snapshot(BEFORE);
    }

    @Override
    public void stop() {
        snapshot(AFTER);
    }

    @Override
    public Sample getSample() {
        if (fDataPoints != null) {
            HashMap<String, String> runProperties = new HashMap<>();
            collectRunInfo(runProperties);
            return new Sample(getScenarioName(), fStartTime, runProperties,
                    fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
        }
        return null;
    }

    // ---- private stuff ------

    private void snapshot(int step) {
        HashMap<Dim, Scalar> map = new HashMap<>();
        fPerformanceMonitor.collectOperatingSystemCounters(map);
        fDataPoints.add(new DataPoint(step, map));
    }

    /**
     * Write out the run element if it hasn't been written out yet.
     *
     * @param runProperties
     */
    private void collectRunInfo(HashMap runProperties) {
        fPerformanceMonitor.collectGlobalPerformanceInfo(runProperties);
    }
}
