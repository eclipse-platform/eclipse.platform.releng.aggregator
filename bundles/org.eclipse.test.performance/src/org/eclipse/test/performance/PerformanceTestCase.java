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
package org.eclipse.test.performance;

import junit.framework.TestCase;

/**
 * A PerformanceTestCase is a convenience class that takes care of managing
 * a <code>PerformanceMeter</code>.
 * <p>
 * Here is an example:
 * <pre>
 * public class MyPerformanceTestCase extends PeformanceTestCase {
 * 		
 *   public void testMyOperation() {
 *     for (int i= 0; i < 10; i++) {
 *       startMeasuring();
 *       // my operation
 *       stopMeasuring();
 *     }
 *     commitMeasurements();
 *     assertPerformance();
 *   }	
 * }
 */
public class PerformanceTestCase extends TestCase {

	protected PerformanceMeter fPerformanceMeter;

	/**
	 * Constructs a performance test case.
	 */
	public PerformanceTestCase() {
		super();
	}

	/**
	 * Constructs a performance test case with the given name.
	 */
	public PerformanceTestCase(String name) {
		super(name);
	}
	
	/**
	 * Overidden to create a default performance meter for this test case.
	 */
	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
	}

	/**
	 * Overidden to disposee of the performance meter.
	 */
	protected void tearDown() throws Exception {
		fPerformanceMeter.dispose();
	}

	/**
	 * Called from within a test case immediately before the code to measure is run.
	 * It starts capturing of performance data.
	 * Must be followed by a call to {@link PerformanceTestCase#stopMeasuring()} before subsequent calls
	 * to this method or {@link PerformanceTestCase#commitMeasurements()}.
	 */
	protected void startMeasuring() {
		fPerformanceMeter.start();
	}
	
	protected void stopMeasuring() {
		fPerformanceMeter.stop();
	}
	
	protected void commitMeasurements() {
		fPerformanceMeter.commit(); 
	}

	/**
	 * Asserts default properties of the measurements captured for this test case.
	 * 
	 * @throws RuntimeException if the properties do not hold
	 */
	protected void assertPerformance() {
		Performance.getDefault().assertPerformance(fPerformanceMeter);
	}

	/**
	 * Asserts that the measurement specified by the given dimension
	 * is within a certain range with respect to some reference value.
	 * If the specified dimension isn't available, the call has no effect.
	 * 
	 * @param dim the Dimension to check
	 * @param lowerPercentage a negative number indicating the percentage the measured value is allowed to be smaller than some reference value
	 * @param upperPercentage a positive number indicating the percentage the measured value is allowed to be greater than some reference value
	 * @throws RuntimeException if the properties do not hold
	 */
	protected void assertPerformanceInRelativeBand(Dimension dim, int lowerPercentage, int upperPercentage) {
		Performance.getDefault().assertPerformanceInRelativeBand(fPerformanceMeter, dim, lowerPercentage, upperPercentage);
	}
}
