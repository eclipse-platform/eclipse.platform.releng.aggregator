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

import java.util.HashSet;

import junit.framework.Assert;

import org.eclipse.test.internal.performance.Constants;
import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * @since 3.1
 */
public class Evaluator extends EmptyEvaluator {
	
	private AssertChecker[] fCheckers;


	public void setAssertCheckers(AssertChecker[] asserts) {
		fCheckers= asserts;
	}

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#evaluate(org.eclipse.jdt.ui.tests.performance.PerformanceMeter)
	 */
	public void evaluate(PerformanceMeter performanceMeter) throws RuntimeException {
	    
		if (fCheckers == null)
		    return;	// nothing to do
		
		// get reference build tag
		String refBuild= PerformanceTestPlugin.getEnvironment(Constants.ASSERT_AGAINST);
		if (refBuild == null)
		    return;	// nothing to do

	    if (!(performanceMeter instanceof InternalPerformanceMeter))
	        return;
        InternalPerformanceMeter ipm= (InternalPerformanceMeter) performanceMeter;
	    Sample session= ipm.getSample();
		Assert.assertTrue("metering session is null", session != null); //$NON-NLS-1$
	    String scenarioName= session.getScenarioID();
		
		// determine all dimensions we need
		HashSet allDimensions= new HashSet();
		for (int i= 0; i < fCheckers.length; i++) {
			AssertChecker chk= fCheckers[i];
			Dim[] dims= chk.getDimensions();
			for (int j= 0; j < dims.length; j++)
				allDimensions.add(dims[j]);
		}
		Dim[] allDims= (Dim[]) allDimensions.toArray(new Dim[allDimensions.size()]);
		
		// get data for this session
		DataPoint[] sessionDatapoints= null;
		String build= PerformanceTestPlugin.getEnvironment(Constants.BUILD);
		if (build != null)
		    sessionDatapoints= DB.queryDataPoints(null, build, scenarioName, allDims);
		else
			sessionDatapoints= session.getDataPoints();
	    if (sessionDatapoints == null || sessionDatapoints.length == 0) {
	        System.out.println("Warning: no session data for build '" + build + "' found"); //$NON-NLS-1$ //$NON-NLS-2$
	    	return;
	    }

		DataPoint[] datapoints= DB.queryDataPoints(null, refBuild, scenarioName, allDims);
	    if (datapoints == null || datapoints.length == 0) {
	        System.out.println("no reference data for build '" + refBuild + "' found"); //$NON-NLS-1$ //$NON-NLS-2$
	        return;
	    }
				
		StatisticsSession referenceStats= new StatisticsSession(datapoints);
		StatisticsSession measuredStats= new StatisticsSession(sessionDatapoints);

		StringBuffer failMesg= new StringBuffer("Performance criteria not met when compared to '" + refBuild + "':"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean pass= true;
		for (int i= 0; i < fCheckers.length; i++) {
			AssertChecker chk= fCheckers[i];
			pass &= chk.test(referenceStats, measuredStats, failMesg);
		}
		Assert.assertTrue(failMesg.toString(), pass);
	}
}
