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

package org.eclipse.test.internal.performance.eval;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Scalar;

/**
 * @since 3.1
 */
public class AbsoluteBandChecker extends AssertChecker {

    private long fLowerBand;
    private long fUpperBand;

    public AbsoluteBandChecker(Dim dimension, long lowerBand, long upperBand) {
        super(dimension);
        fLowerBand = lowerBand;
        fUpperBand = upperBand;
    }

    @Override
    public boolean test(StatisticsSession reference, StatisticsSession measured, StringBuffer message) {
        Dim dimension = getDimension();

        if (!measured.contains(dimension)) {
            PerformanceTestPlugin.logWarning("collected data provides no dimension '" + dimension.getName() + '\''); //$NON-NLS-1$
            return true;
        }
        if (!reference.contains(dimension)) {
            PerformanceTestPlugin.logWarning("reference data provides no dimension '" + dimension.getName() + '\''); //$NON-NLS-1$
            return true;
        }

        double actual = measured.getAverage(dimension);
        double test = reference.getAverage(dimension);

        if (actual > fUpperBand + test || actual < test - fLowerBand) {
            message.append('\n'
                    + dimension.getName()
                    + ": " + dimension.getDisplayValue(actual) + " is not within [-" + dimension.getDisplayValue(new Scalar(null, fLowerBand)) + ", +" + dimension.getDisplayValue(new Scalar(null, fUpperBand)) + "] of " + dimension.getDisplayValue(test)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return false;
        }

        return true;
    }
}
