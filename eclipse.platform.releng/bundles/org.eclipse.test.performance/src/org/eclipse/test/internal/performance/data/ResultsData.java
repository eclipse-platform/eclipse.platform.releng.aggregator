/*******************************************************************************
 * Copyright (c) 2022 Samantha Dawley and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Samantha Dawley - initial API and implementation
 *******************************************************************************/
 
package org.eclipse.test.internal.performance.data;

import java.io.EOFException;
//import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
//import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.test.internal.performance.db.Variations;

/**
 * @since 3.19
 */
public class ResultsData{
    //Static because there shouldn't be different groups of results
    private static Map<String, Sample> CURRENT_SCENARIO_DATA; //Map of scenarioID to Sample for the current build
	private static Map<String, Sample> BASELINE_SCENARIO_DATA; //Map of scenarioID to Sample for the baseline build

    private static String CURRENT_BUILD, BASELINE_BUILD = null;

    private final static String[] ECLIPSE_COMPONENTS = {
		"org.eclipse.ant", 
		"org.eclipse.compare", 
		"org.eclipse.core", 
		"org.eclipse.help", 
		"org.eclipse.jdt.core", 
		"org.eclipse.jdt.debug",
		"org.eclipse.jdt.text", 
		"org.eclipse.jdt.ui", 
		"org.eclipse.jface", 
		"org.eclipse.osgi", 
		"org.eclipse.pde.api.tools", 
		"org.eclipse.pde.ui", 
		"org.eclipse.swt", 
		"org.eclipse.team", 
		"org.eclipse.ua", 
		"org.eclipse.ui" 
	};

    public ResultsData(String current, String baseline) {
        CURRENT_BUILD = current;
    	BASELINE_BUILD = baseline;
        CURRENT_SCENARIO_DATA = new HashMap<>();
        BASELINE_SCENARIO_DATA = new HashMap<>();
    }

    public void importData(Path inputFile) {
    	System.out.println("INFO: Reading data from " + inputFile);
		Variations variations = null;
		try (InputStream is = Files.newInputStream(inputFile)) {
			while (true) { // loop will end on input stream EOF
        		ObjectInputStream ois = new ObjectInputStream(is);
        		String scenarioID = null;
        		Sample sample = null;
        		while (scenarioID == null || variations == null || sample == null) {
          			Object o = ois.readObject();
          			if (String.class.equals(o.getClass())) {
            			scenarioID = (String) o;
          			} else if (Variations.class.equals(o.getClass())) {
						if (variations == null) { //variations only needs to be captured once
              				variations = (Variations) o;
			            }
          			} else if (Sample.class.equals(o.getClass())) {
            			sample = (Sample) o;
          			} else {
            			System.err.println("WARN: Input contains unexpected object of type " + o.getClass().getCanonicalName());
          			}
        		}

        		//System.out.println("DEBUG: Store data for scenario " + scenarioID);
				String build = variations.getProperty("build");
				if ((build.equals(CURRENT_BUILD)) && (!CURRENT_SCENARIO_DATA.containsKey(scenarioID))) {
					CURRENT_SCENARIO_DATA.put(scenarioID, sample);
				} else if ((build.contains(BASELINE_BUILD)) && (!BASELINE_SCENARIO_DATA.containsKey(scenarioID))) {
					BASELINE_SCENARIO_DATA.put(scenarioID, sample);
				} else {
					System.err.println("WARN: Input contains Data from the wrong build or baseline");
				}		
    	    }
		} catch (EOFException ex) {
      		// EOFException is the intended way to end the loop
			System.out.println("Finished reading data from " + inputFile);
		}
		catch (Exception ex) {
            System.err.println("ERROR: IOException reading: " + inputFile);
            System.exit(1);
        }
	}

    public String[] getComponents() {
        return ECLIPSE_COMPONENTS;
    }

    public Set<String> getCurrentScenarios() {
        return CURRENT_SCENARIO_DATA.keySet();
    }

	public Set<String> getBaselineScenarios() {
		return BASELINE_SCENARIO_DATA.keySet();
	}

    public Double[] getData(String build, String scenarioID) {
        Sample sample = null;

        if (build == "current") {
            sample = CURRENT_SCENARIO_DATA.get(scenarioID);
        } else {
            sample = BASELINE_SCENARIO_DATA.get(scenarioID);
        }
        
        DataPoint[] data = sample.getDataPoints();

        Double elapsedProcess = 0.0;
        Double cpuTime = 0.0;

        for (DataPoint datum : data) {
            Dim[] dimensions = datum.getDimensions();
            for (Dim dim : dimensions) {
                if (dim.getName().contains("Elapsed Process")) {
                    Scalar scalar = datum.getScalar(dim);
					String value = dim.getDisplayValue(scalar);
					//check for big values (1.21k etc)
					if (value.substring(value.length() - 1).equals("K")){
						value = value.substring(0, value.length()-1);
						Double decimalValue = Double.parseDouble(value);
						decimalValue = decimalValue * 1000;
						elapsedProcess += decimalValue;
					} else {
						elapsedProcess += Double.parseDouble(value);
					}  
                }
                if (dim.getName().contains("CPU Time")) {
                    Scalar scalar = datum.getScalar(dim);
					String value = dim.getDisplayValue(scalar);
					//check for big values (1.21k etc)
					if (value.substring(value.length() - 1).equals("K")){
						value = value.substring(0, value.length()-1);
						Double decimalValue = Double.parseDouble(value);
						decimalValue = decimalValue * 1000;
						cpuTime += decimalValue;
					} else {
						cpuTime += Double.parseDouble(value);
					}  
                }
            }
        }

        Double[] currentData = {elapsedProcess, cpuTime};
        return currentData;
    }
}
