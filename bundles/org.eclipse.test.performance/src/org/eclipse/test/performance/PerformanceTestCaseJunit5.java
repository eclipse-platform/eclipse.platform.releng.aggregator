/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *     Copied and modified for Junit4 from PerformanceTestCase.java
 *******************************************************************************/

package org.eclipse.test.performance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * A PerformanceTestCaseJunit5 is a convenience class that takes care of managing a <code>PerformanceMeter</code>.
 * <p>
 * Here is an example:
 *
 * <blockquote>
 *
 * <pre>
 * public class MyPerformanceTestCase extends PerformanceTestCaseJunit% {
 *
 *     &#64;Test
 *     public void testMyOperation() {
 *         for (int i = 0; i < 10; i++) {
 *             // preparation
 *             startMeasuring();
 *             // my operation
 *             stopMeasuring();
 *             // clean up
 *         }
 *         commitMeasurements();
 *         assertPerformance();
 *     }
 * }
 * </pre>
 *
 * </blockquote>
 *
 * @since 3.16
 */
public class PerformanceTestCaseJunit5 extends AbstractPerformanceTestCase {

    /**
     * Create a default performance meter for this test case.
     *
     * @param testInfo
     *
     * @throws Exception
     */
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        Performance performance = Performance.getDefault();
        fPerformanceMeter = performance.createPerformanceMeter(performance.getDefaultScenarioId(this.getClass(), testInfo.getDisplayName()));
    }

    /**
     * Dispose of the performance meter.
     *
     * @throws Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        fPerformanceMeter.dispose();
    }
}
