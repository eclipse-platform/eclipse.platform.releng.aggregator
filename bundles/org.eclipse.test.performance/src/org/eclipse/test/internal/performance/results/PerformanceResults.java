/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.results;

import java.io.File;
import java.util.*;


/**
 * Root class to handle performance results.
 * 
 * Usually performance results are built for a current build vs. a baseline build.
 * 
 * This class allow to read all data from releng performance database for given
 * configurations and scenario pattern.
 * 
 * Then it provides easy and speedy access to all stored results.
 */
public class PerformanceResults extends AbstractResults {

	String baselineName; // Name of the baseline build used for comparison
	String baselinePrefix;
	private String scenarioPattern;
	private List components;
	String[] configNames, sortedConfigNames;
	private String[] configBoxes, sortedConfigBoxes;
	private String configPattern;

public static PerformanceResults createPerformanceResults(String scenarioPattern, File dataDir, boolean print) {
	DB_Results.getBuilds(); // Init build names
	if (DB_Results.LAST_CURRENT_BUILD == null || DB_Results.LAST_BASELINE_BUILD == null) return null;
	PerformanceResults performanceResults = new PerformanceResults(DB_Results.LAST_CURRENT_BUILD, DB_Results.LAST_BASELINE_BUILD, print);
	performanceResults.read(null, scenarioPattern, dataDir, DEFAULT_FAILURE_THRESHOLD);
	return performanceResults;
}

	// Failure threshold
	public static final int DEFAULT_FAILURE_THRESHOLD = 10;
	int failure_threshold = DEFAULT_FAILURE_THRESHOLD;

public PerformanceResults(String name, String baseline, boolean print) {
	super(null, name);
	this.baselineName = baseline;
	if (baseline != null) {
		this.baselinePrefix = baseline.substring(0, baseline.lastIndexOf('_'));
	}
	this.print = print;
}

/**
 * Returns the name of the baseline used for extracted results
 * 
 * @return The build name of the baseline of <code>null</code>
 * 	if no specific baseline is used for the extracted results.
 */
public String getBaselineName() {
	return this.baselineName;
}

/*
 * Get the baseline prefix (computed from #baselineName).
 */
String getBaselinePrefix() {
	return this.baselinePrefix;
}

/*
 * Get the build date (see #getBuildDate(String, String)).
 */
public String getBuildDate() {
	return getBuildDate(this.name, this.baselinePrefix);
}

/**
 * Return the list of components concerned by performance results.
 * 
 * @return The list of the components
 */
public List getComponents() {
	return components;
}

/**
 * Get the scenarios of a given component.
 *
 * @param componentName The component name. Should not be <code>null</code>
 * @return A list of {@link ScenarioResults scenario results}
 */
public List getComponentScenarios(String componentName) {
	ComponentResults componentResults = (ComponentResults) getResults(componentName);
	if (componentResults == null) return null;
	return Collections.unmodifiableList(componentResults.children);
}

/**
 * Get the scenarios which have a summary for a given component.
 *
 * @param componentName The component name
 * @param config Configuration name
 * @return A list of {@link ScenarioResults scenario results} which have a summary
 */
public List getComponentSummaryScenarios(String componentName, String config) {
	if (componentName == null) {
		int size = size();
		List scenarios = new ArrayList();
		for (int i=0; i< size; i++) {
			ComponentResults componentResults = (ComponentResults) this.children.get(i);
			scenarios.addAll(componentResults.getSummaryScenarios(true, config));
		}
		return scenarios;
	}
	ComponentResults componentResults = (ComponentResults) getResults(componentName);
	return componentResults.getSummaryScenarios(false, config);
}

/**
 * Return the configuration boxes considered for this performance results
 * sorted or not depending on the given flag.
 * 
 * @param sort Indicates whether the list must be sorted or not.
 * 	The order is defined by the configuration names, not by the box names
 * @return The list of configuration boxes sorted by configuration names
 */
public String[] getConfigBoxes(boolean sort) {
	return sort ? this.sortedConfigBoxes : this.configBoxes;
}

/**
 * Return the configuration names considered for this performance results
 * sorted or not depending on the given flag.
 * 
 * @param sort Indicates whether the list must be sorted or not
 * @return The list of configuration names
 */
public String[] getConfigNames(boolean sort) {
	return sort ?this.sortedConfigNames : this.configNames;
}

/*
 * Compute a SQL pattern from all stored configuration names.
 * For example 'eclipseperflnx1', 'eclipseperflnx2' and 'eclipseperflnx3'
 * will return 'eclipseperflnx_'.
 */
String getConfigurationsPattern() {
	if (this.configPattern == null) {
		int length = this.sortedConfigNames == null ? 0 : this.sortedConfigNames.length;
		if (length == 0) return null;
		this.configPattern = this.sortedConfigNames[0];
		int refLength = this.configPattern.length();
		for (int i=1; i<length; i++) {
			String config = this.sortedConfigNames[i];
			StringBuffer newConfig = null;
			if (refLength != config.length()) return null; // strings have not the same length => cannot find a pattern
			for (int j=0; j<refLength; j++) {
				char c = this.configPattern.charAt(j);
				if (config.charAt(j) != c) {
					if (newConfig == null) {
						newConfig = new StringBuffer(refLength);
						if (j == 0) return null; // first char is already different => cannot find a pattern
						newConfig.append(this.configPattern.substring(0, j));
					}
					newConfig.append('_');
				} else if (newConfig != null) {
					newConfig.append(c);
				}
			}
			if (newConfig != null) {
				this.configPattern = newConfig.toString();
			}
		}
	}
	return this.configPattern;
}

/*
 * (non-Javadoc)
 * @see org.eclipse.test.internal.performance.results.AbstractResults#getPerformance()
 */
PerformanceResults getPerformance() {
	return this;
}

/**
 * Get the results of a given scenario.
 *
 * @param scenarioName The scenario name
 * @return The {@link ScenarioResults scenario results}
 */
public ScenarioResults getScenarioResults(String scenarioName) {
	ComponentResults componentResults = (ComponentResults) getResults(DB_Results.getComponentNameFromScenario(scenarioName));
	return componentResults == null ? null : (ScenarioResults) componentResults.getResults(scenarioName);
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * @param dataDir The directory where data will be stored locally
 * 	if <code>null</code>, then storage will be performed
 */
public void read(File dataDir) {
	read(null, null, dataDir, DEFAULT_FAILURE_THRESHOLD);
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * @param configs All configurations to extract results. If <code>null</code>,
 * 	then all known configurations ({@link #CONFIGS})  are read.
 * @param pattern The pattern of the concerned scenarios
 * @param dataDir The directory where data will be stored locally
 * 	if <code>null</code>, then storage will be performed
 * @param threshold The failure percentage threshold over which a build result
 * 	value compared to the baseline is considered as failing.
 */
public void read(String[][] configs, String pattern, File dataDir, int threshold) {

	this.scenarioPattern = pattern;
	this.failure_threshold = threshold;

	// Print title
	StringBuffer buffer = new StringBuffer("Read performance results until build '"); //$NON-NLS-1$
	buffer.append(this.name);
	if (scenarioPattern == null) {
		buffer.append("':"); //$NON-NLS-1$
	} else {
		buffer.append("' using scenario pattern '"); //$NON-NLS-1$
		buffer.append(scenarioPattern);
		buffer.append("':"); //$NON-NLS-1$
	}
	println(buffer);

	// Store given configs
	if (configs == null) {
		int length=CONFIGS.length;
		this.configNames = new String[length];
		this.sortedConfigNames = new String[length];
		this.configBoxes = new String[length];
		for (int i=0; i<length; i++) {
			this.configNames[i] = this.sortedConfigNames[i] = CONFIGS[i];
			this.configBoxes[i] = BOXES[i];
		}
	} else {
		int length = configs.length;
		this.configNames = new String[length];
		this.sortedConfigNames = new String[length];
		this.configBoxes = new String[length];
		for (int i=0; i<length; i++) {
			this.configNames[i] = this.sortedConfigNames[i] = configs[i][0];
			this.configBoxes[i] = configs[i][1];
		}
	}
	Arrays.sort(this.sortedConfigNames);
	int length = this.sortedConfigNames.length;
	this.sortedConfigBoxes = new String[length];
	for (int i=0; i<length; i++) {
		for (int j=0; j<length; j++) {
			if (this.sortedConfigNames[i] == this.configNames[j]) { // == is intentional!
				this.sortedConfigBoxes[i] = this.configBoxes[j];
				break;
			}
		}
	}

	// Get scenarios from the given pattern
	print("	+ get corresponding scenarios for build: "+this.name); //$NON-NLS-1$
	long start = System.currentTimeMillis();
	Map allScenarios = DB_Results.queryAllScenarios(this.scenarioPattern, this.name);
	println(" -> "+(System.currentTimeMillis()-start)+"ms"); //$NON-NLS-1$ //$NON-NLS-2$

	// Create corresponding children
	List allComponents = DB_Results.getComponents();
	int size = allComponents.size();
	this.components = new ArrayList(size);
	for (int i=0; i<size; i++) {
		String componentName = (String) allComponents.get(i);
		List scenarios = (List) allScenarios.get(componentName);
		if (scenarios == null) continue;
		this.components.add(componentName);
		ComponentResults componentResults = new ComponentResults(this, componentName);
		addChild(componentResults, true);
		componentResults.read(scenarios, dataDir);
	}

	// Print time
	printGlobalTime(start);
}
}
