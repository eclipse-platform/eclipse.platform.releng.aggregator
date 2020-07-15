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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * A PerformanceTestCaseJunit4 is a convenience class that takes care of managing a <code>PerformanceMeter</code>.
 * <p>
 * Here is an example:
 *
 * <blockquote>
 *
 * <pre>
 * public class MyPerformanceTestCase extends PerformanceTestCaseJunit4 {
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
public class PerformanceTestCaseJunit4 extends AbstractPerformanceTestCase {

    @Rule
    public TestName tn = new TestName();

    /**
     * Create a default performance meter for this test case.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Performance performance = Performance.getDefault();
        fPerformanceMeter = performance.createPerformanceMeter(performance.getDefaultScenarioId(this.getClass(), tn.getMethodName()));
    }

    /**
     * Dispose of the performance meter.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        fPerformanceMeter.dispose();
    }
}
