/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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


public class PerformanceTestCase extends TestCase {

	protected PerformanceMeter fPerformanceMeter;

	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
	}

	protected void tearDown() throws Exception {
		fPerformanceMeter.dispose();
	}

	protected void assertPerformance() {
		Performance.getDefault().assertPerformance(fPerformanceMeter);
	}

	protected void commitMeasurements() {
		fPerformanceMeter.commit(); 
	}

	protected void stopMeasuring() {
		fPerformanceMeter.stop();
	}

	protected void startMeasuring() {
		fPerformanceMeter.start();
	}
}
