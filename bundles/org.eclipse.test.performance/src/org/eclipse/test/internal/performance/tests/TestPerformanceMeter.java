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

package org.eclipse.test.internal.performance.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.internal.performance.data.Unit;


/**
 * Performance meter that generates deterministic values.
 */
class TestPerformanceMeter extends InternalPerformanceMeter {
    
    static Dim TESTDIM1= new Dim(98, Unit.SECOND, 1000);
    static Dim TESTDIM2= new Dim(99, Unit.BYTE);
	
	private long fStartTime;
	private List fDataPoints= new ArrayList();    
	
	/**
	 * @param scenarioId the scenario id
	 */
	TestPerformanceMeter(String scenarioId) {
	    super(scenarioId);
		fStartTime= System.currentTimeMillis();
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
	 */
	public void dispose() {
	    fDataPoints= null;
	    super.dispose();
	}

	/*
	 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#getSample()
	 */
	public Sample getSample() {
	    if (fDataPoints != null)
	        return new Sample(getScenarioName(), fStartTime, new HashMap(), (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
	    return null;
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#start()
	 */
	public void start() {
	    HashMap scalars= new HashMap();
	    scalars.put(TESTDIM1, new Scalar(TESTDIM1, 100));
	    scalars.put(TESTDIM2, new Scalar(TESTDIM2, 1000));
	    fDataPoints.add(new DataPoint(BEFORE, scalars));
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	public void stop() {
	    HashMap scalars= new HashMap();
	    scalars.put(TESTDIM1, new Scalar(TESTDIM1, 1000));
	    scalars.put(TESTDIM2, new Scalar(TESTDIM2, 2000));
	    fDataPoints.add(new DataPoint(AFTER, scalars));
	}
}
