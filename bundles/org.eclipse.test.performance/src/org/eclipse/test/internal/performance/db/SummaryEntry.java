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
package org.eclipse.test.internal.performance.db;

import org.eclipse.test.internal.performance.data.Dim;


public class SummaryEntry {
    public String scenarioName;
    public String shortName;
    public Dim dimension;
    
    SummaryEntry(String scenarioName, String shortName, Dim dimension) {
        this.scenarioName= scenarioName;
        this.shortName= shortName;
        this.dimension= dimension;
    }
    
    public String toString() {
        return "SummaryEntry: " + scenarioName + ' ' + shortName + ' ' + dimension; //$NON-NLS-1$
    }
}
