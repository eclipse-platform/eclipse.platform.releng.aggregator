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
package org.eclipse.test.internal.performance.data;

import java.util.Map;


/**
 * @since 3.1
 */
public class DataPoint {
	private int fStep;
	private Map fScalars;
	
	public DataPoint(int step, Map values) {
		fStep= step;
		fScalars= values;
	}
	
	public int getStep() {
		return fStep;
	}
	
	public Scalar[] getScalars() {
		return (Scalar[]) fScalars.values().toArray(new Scalar[fScalars.size()]);
	}
	
	public Scalar getScalar(Dimension dimension) {
		return (Scalar) fScalars.get(dimension);
	}
	
	public String toString() {
		return "DataPoint [step= " + fStep + ", #dimensions: " + fScalars.size() + "]";
	}
}
