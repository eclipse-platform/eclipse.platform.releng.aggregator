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

package org.eclipse.test.internal.performance.eval;

import junit.framework.Assert;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * The DB evaluator. Does nothing.
 */
public class DBEvaluator implements IEvaluator {

	private AssertChecker[] fCheckers;

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#evaluate(org.eclipse.jdt.ui.tests.performance.PerformanceMeter)
	 */
	public void evaluate(PerformanceMeter performanceMeter) throws RuntimeException {
	    
		if (fCheckers == null)
		    return;	// nothing to do

	    if (!(performanceMeter instanceof InternalPerformanceMeter))
	        return;
        InternalPerformanceMeter ipm= (InternalPerformanceMeter) performanceMeter;
        
	    Sample session= ipm.getSample();
		Assert.assertTrue("metering session is null", session != null); //$NON-NLS-1$

		Sample reference= getReferenceSession(ipm.getScenarioName(), "this");
		
		
        String refTag= "3.1"; // reference.getProperty(InternalPerformanceMeter.DRIVER_PROPERTY);
        String refDate= "0824"; // reference.getProperty(InternalPerformanceMeter.RUN_TS_PROPERTY);
	    DB db= DB.acquire();
	    DataPoint[] datapoints= db.query(ipm.getScenarioName(), refTag);
	    DB.release(db);
	    
		StatisticsSession referenceStats= new StatisticsSession(datapoints);
		StatisticsSession measuredStats= new StatisticsSession(session.getDataPoints());
	    
		StringBuffer failMesg= new StringBuffer("Performance criteria not met when compared to '" + refTag + "' from " + refDate + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean pass= true;
		for (int i= 0; i < fCheckers.length; i++) {
			pass &= fCheckers[i].test(referenceStats, measuredStats, failMesg);
		}
		Assert.assertTrue(failMesg.toString(), pass);
	}

	private Sample getReferenceSession(String scenarioName, String hostname) {
        String refTag= "3.1"; // reference.getProperty(InternalPerformanceMeter.DRIVER_PROPERTY);
        String refDate= "0824"; // reference.getProperty(InternalPerformanceMeter.RUN_TS_PROPERTY);
	    DB db= DB.acquire();
	    DataPoint[] datapoints= db.query(scenarioName, refTag);
	    DB.release(db);
	    return new Sample(datapoints);
	}
	
	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#setAssertCheckers(org.eclipse.test.internal.performance.eval.AssertChecker[])
	 */
	public void setAssertCheckers(AssertChecker[] asserts) {
		fCheckers= asserts;
	}

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#setReferenceFilterProperties(java.lang.String, java.lang.String)
	 */
	public void setReferenceFilterProperties(String driver, String timestamp) {
	}
}
