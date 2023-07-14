/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.eval;

import java.util.HashSet;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.db.Variations;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.Assert;

/**
 * The default implementation of an evaluator backed by a database.
 *
 * @since 3.1
 */
public class Evaluator extends EmptyEvaluator {

    private AssertChecker[] fCheckers;

    @Override
    public void setAssertCheckers(AssertChecker[] asserts) {
        fCheckers = asserts;
    }

    @Override
    public void evaluate(PerformanceMeter performanceMeter) throws RuntimeException {

        if (fCheckers == null)
            return; // nothing to do

        // get reference build tag
        Variations refKeys = PerformanceTestPlugin.getAssertAgainst();
        String assertKey = System.getProperty(PerformanceTestPlugin.ECLIPSE_PERF_ASSERTAGAINST);
        if (refKeys == null) {
            PerformanceTestPlugin.logWarning("refkeys was null. " + PerformanceTestPlugin.ECLIPSE_PERF_ASSERTAGAINST + " was " + assertKey); //$NON-NLS-1$ //$NON-NLS-2$
            return; // nothing to do
        }
        // else
        PerformanceTestPlugin.logInfo("refkeys was: " + refKeys.toString() + " \n\t based on " + PerformanceTestPlugin.ECLIPSE_PERF_ASSERTAGAINST + " being set to " + assertKey); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


        if (!(performanceMeter instanceof InternalPerformanceMeter))
            return; // we cannot handle this.

        InternalPerformanceMeter ipm = (InternalPerformanceMeter) performanceMeter;
        Sample session = ipm.getSample();
        Assert.assertTrue("metering session is null", session != null); //$NON-NLS-1$

        // determine all dimensions we need
        HashSet<Dim> allDimensions = new HashSet<>();
        for (AssertChecker chk : fCheckers) {
            for (Dim dim : chk.getDimensions())
                allDimensions.add(dim);
        }

        // get data for this session
        DataPoint[] sessionDatapoints = session.getDataPoints();
        Variations config = PerformanceTestPlugin.getVariations();
        if (sessionDatapoints == null || sessionDatapoints.length == 0) {
            PerformanceTestPlugin.logWarning("no session data named '" + config + "' found"); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

    }
}
