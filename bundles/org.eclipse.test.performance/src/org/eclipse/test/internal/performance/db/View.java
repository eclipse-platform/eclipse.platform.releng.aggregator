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
package org.eclipse.test.internal.performance.db;

import org.eclipse.test.internal.performance.data.Dim;

public class View {

    public static void main(String[] args) {

        Scenario[] scenarios= DB.queryScenarios("%", "%", "%"); // get all Scenarios //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        for (int s= 0; s < scenarios.length; s++) {
            Scenario t= scenarios[s];
            String scenarioName= t.getScenarioName();
            System.out.println("Scenario: " + scenarioName); //$NON-NLS-1$
            
            Dim[] dimensions= t.getDimensions();
            if (dimensions.length > 0) {
	            TimeSeries ts= t.getTimeSeries(dimensions[0]);
	           
	            System.out.print("Builds:"); //$NON-NLS-1$
	            for (int j= 0; j < ts.getLength(); j++)
	                System.out.print('\t' + ts.getLabel(j));
	            System.out.println();
	                        
	            for (int i= 0; i < dimensions.length; i++) {
	                Dim dim= dimensions[i];
	                System.out.print(dim.getName() + ": "); //$NON-NLS-1$
	                
	                ts= t.getTimeSeries(dim);
	                int n= ts.getLength();
	                for (int j= 0; j < n; j++)
	                    System.out.print('\t' + dim.getDisplayValue(ts.getValue(j)));
	                System.out.println();
	            }
            }
            System.out.println();
        }
     }
}
