/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
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
 *     Copied and modified from PerformanceTestCase.java
 *******************************************************************************/

package org.eclipse.test.performance;

/**
 * Common parts of Junit4 and Junit5
 *
 * @since 3.16
 */
public class AbstractPerformanceTestCase {

    protected PerformanceMeter fPerformanceMeter;

    /**
     * Mark the scenario of this test case to be included into the global and the component performance summary. The summary shows
     * the given dimension of the scenario and labels the scenario with the short name.
     *
     * @param shortName
     *            a short (shorter than 40 characters) descritive name of the scenario
     * @param dimension
     *            the dimension to show in the summary
     */
    public void tagAsGlobalSummary(String shortName, Dimension dimension) {
        Performance performance = Performance.getDefault();
        performance.tagAsGlobalSummary(fPerformanceMeter, shortName, new Dimension[] { dimension });
    }

    /**
     * Mark the scenario represented by the given PerformanceMeter to be included into the global and the component performance
     * summary. The summary shows the given dimensions of the scenario and labels the scenario with the short name.
     *
     * @param shortName
     *            a short (shorter than 40 characters) descritive name of the scenario
     * @param dimensions
     *            an array of dimensions to show in the summary
     */
    public void tagAsGlobalSummary(String shortName, Dimension[] dimensions) {
        Performance performance = Performance.getDefault();
        performance.tagAsGlobalSummary(fPerformanceMeter, shortName, dimensions);
    }

    /**
     * Mark the scenario of this test case to be included into the component performance summary. The summary shows the given
     * dimension of the scenario and labels the scenario with the short name.
     *
     * @param shortName
     *            a short (shorter than 40 characters) descritive name of the scenario
     * @param dimension
     *            the dimension to show in the summary
     */
    public void tagAsSummary(String shortName, Dimension dimension) {
        Performance performance = Performance.getDefault();
        performance.tagAsSummary(fPerformanceMeter, shortName, new Dimension[] { dimension });
    }

    /**
     * Mark the scenario represented by the given PerformanceMeter to be included into the component performance summary. The
     * summary shows the given dimensions of the scenario and labels the scenario with the short name.
     *
     * @param shortName
     *            a short (shorter than 40 characters) descritive name of the scenario
     * @param dimensions
     *            an array of dimensions to show in the summary
     */
    public void tagAsSummary(String shortName, Dimension[] dimensions) {
        Performance performance = Performance.getDefault();
        performance.tagAsSummary(fPerformanceMeter, shortName, dimensions);
    }

    /**
     * Set a comment for the scenario represented by this TestCase. Currently only comments with a commentKind of
     * EXPLAINS_DEGRADATION_COMMENT are used. Their commentText is shown in a hover of the performance summaries graph if a
     * performance degradation exists.
     *
     * @param commentKind
     *            kind of comment. Must be EXPLAINS_DEGRADATION_COMMENT to have an effect.
     * @param commentText
     *            the comment (shorter than 400 characters)
     */
    public void setComment(int commentKind, String commentText) {
        Performance performance = Performance.getDefault();
        performance.setComment(fPerformanceMeter, commentKind, commentText);
    }

    /**
     * Called from within a test case immediately before the code to measure is run. It starts capturing of performance data. Must
     * be followed by a call to {@link AbstractPerformanceTestCase#stopMeasuring()} before subsequent calls to this method or
     * {@link AbstractPerformanceTestCase#commitMeasurements()}.
     *
     * @see PerformanceMeter#start()
     */
    protected void startMeasuring() {
        fPerformanceMeter.start();
    }

    /**
     * Called from within a test case immediately after the operation to measure. Must be preceded by a call to
     * {@link AbstractPerformanceTestCase#startMeasuring()}, that follows any previous call to this method.
     *
     * @see PerformanceMeter#stop()
     */
    protected void stopMeasuring() {
        fPerformanceMeter.stop();
    }

    /**
     * Called exactly once after repeated measurements are done and before their analysis. Afterwards
     * {@link AbstractPerformanceTestCase#startMeasuring()} and {@link AbstractPerformanceTestCase#stopMeasuring()} must not be called.
     *
     * @see PerformanceMeter#commit()
     */
    protected void commitMeasurements() {
        fPerformanceMeter.commit();
    }

    /**
     * Asserts default properties of the measurements captured for this test case.
     *
     * @throws RuntimeException
     *             if the properties do not hold
     */
    protected void assertPerformance() {
        Performance.getDefault().assertPerformance(fPerformanceMeter);
    }

    /**
     * Asserts that the measurement specified by the given dimension is within a certain range with respect to some reference value.
     * If the specified dimension isn't available, the call has no effect.
     *
     * @param dim
     *            the Dimension to check
     * @param lowerPercentage
     *            a negative number indicating the percentage the measured value is allowed to be smaller than some reference value
     * @param upperPercentage
     *            a positive number indicating the percentage the measured value is allowed to be greater than some reference value
     * @throws RuntimeException
     *             if the properties do not hold
     */
    protected void assertPerformanceInRelativeBand(Dimension dim, int lowerPercentage, int upperPercentage) {
        Performance.getDefault().assertPerformanceInRelativeBand(fPerformanceMeter, dim, lowerPercentage, upperPercentage);
    }
}
