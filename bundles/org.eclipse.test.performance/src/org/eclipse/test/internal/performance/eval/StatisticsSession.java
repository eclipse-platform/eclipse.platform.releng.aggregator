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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Scalar;

import junit.framework.Assert;

/**
 * @since 3.1
 */
public class StatisticsSession {

	static final class Statistics {
		public long count;
		public long sum;
		public double average;
		public double stddev;
	}
	
	private final DataPoint[] fDataPoints;
	private final Map fStatistics= new HashMap();

	public StatisticsSession(DataPoint[] datapoints) {
	    fDataPoints= datapoints;
	}
	
	public double getAverage(Dim dimension) {
		return getStats(dimension).average;
	}
	
	public long getSum(Dim dimension) {
		return getStats(dimension).sum;
	}
	
	public long getCount(Dim dimension) {
		return getStats(dimension).count;
	}
	
	public double getStddev(Dim dimension) {
		return getStats(dimension).stddev;
	}
	
	private Statistics getStats(Dim dimension) {
		Statistics stats= (Statistics) fStatistics.get(dimension);
		if (stats == null) {
			stats= computeStats(dimension);
			fStatistics.put(dimension, stats);
		}
		return stats;
	}

	private Statistics computeStats(Dim dimension) {
	    		
		Statistics stats= new Statistics();
		
		Set set= new HashSet();
		for (int j= 0; j < fDataPoints.length; j++) {
		    DataPoint dp= fDataPoints[j];
		    set.add(new Integer(dp.getStep()));
		}
		
		long mags[]= new long[fDataPoints.length];
		switch (set.size()) {
		case 1:
			// if there is only one Step, we don't calculate the delta.
			for (int i= 0; i < fDataPoints.length; i++) {
				Scalar sc= fDataPoints[i].getScalar(dimension);
				if (sc != null) {
					long magnitude= sc.getMagnitude();
					mags[i]= magnitude;
					stats.sum += magnitude;
					stats.count++;
				}
			}
			if (stats.count > 0) {
				stats.average= stats.sum / stats.count;
				for (int i= 0; i < fDataPoints.length; i++) {
					stats.stddev+= (stats.average - mags[i]) * (stats.average - mags[i]);
				}
			}
			break;
		case 2:
			for (int i= 0; i < fDataPoints.length-1; i += 2) {
				DataPoint before= fDataPoints[i];
				Assert.assertTrue("wrong order of steps", before.getStep() == InternalPerformanceMeter.BEFORE); //$NON-NLS-1$
				DataPoint after= fDataPoints[i + 1];
				Assert.assertTrue("wrong order of steps", after.getStep() == InternalPerformanceMeter.AFTER); //$NON-NLS-1$
				
				Scalar delta= getDelta(before, after, dimension);
				long magnitude= delta.getMagnitude();
				mags[i]= magnitude;
				stats.sum += magnitude;
				stats.count++;
			}
			if (stats.count > 0) {
				stats.average= stats.sum / stats.count;
				for (int i= 0; i < fDataPoints.length-1; i += 2) {
					stats.stddev+= (stats.average - mags[i]) * (stats.average - mags[i]);
				}
			}
			break;
		default:
			Assert.assertTrue("cannot handle more than two steps", false); //$NON-NLS-1$
		    break;
		}
		
		stats.stddev= Math.sqrt(stats.stddev / stats.count);

		return stats;
	}

	private Scalar getDelta(DataPoint before, DataPoint after, Dim dimension) {
		Scalar one= before.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, one != null); //$NON-NLS-1$

		Scalar two= after.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, two != null); //$NON-NLS-1$
		
		return new Scalar(one.getDimension(), two.getMagnitude() - one.getMagnitude());
	}

	public boolean contains(Dim dimension) {
		if (fDataPoints.length > 0)
			return fDataPoints[0].contains(dimension);
		return false;
	}
}
