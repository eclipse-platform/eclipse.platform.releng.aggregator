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

package org.eclipse.test.internal.performance;

import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.performance.PerformanceMeter;


public abstract class InternalPerformanceMeter extends PerformanceMeter {

    public static final int BEFORE= 0;
    public static final int AFTER= 1;
    
	private String fScenarioId;

	
	public InternalPerformanceMeter(String scenarioId) {
	    fScenarioId= scenarioId;
    }

	public void dispose() {
	    fScenarioId= null;
	}

    public abstract Sample getSample();


	/**
	 * Answer the scenario ID.
	 * @return the scenario ID
	 */
	public String getScenarioName() {
		return fScenarioId;
	}
}
