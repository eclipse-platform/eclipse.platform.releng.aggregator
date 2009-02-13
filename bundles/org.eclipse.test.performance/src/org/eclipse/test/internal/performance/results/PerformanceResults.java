/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.io.PrintStream;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;


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
	private String[] components;
	String[] configNames, sortedConfigNames;
	String[] configBoxes, sortedConfigBoxes;
	private String configPattern;
	String updatedName = null;
	boolean nameCanBeUpdated = false;

	/*
	 * Local class helping to guess remaining time while reading results from DB
	 */
	class RemainingTimeGuess {
		int all, count;
		long start;
		double remaining;
		RemainingTimeGuess(int all, long start) {
			this.all = all;
			this.start = start;
		}
		String display() {
			StringBuffer buffer = new StringBuffer(" [elapsed: "); //$NON-NLS-1$
			long elapsed = getElapsed();
			buffer.append(timeChrono(elapsed));
			if (this.count > 0) {
				buffer.append(" | left: "); //$NON-NLS-1$
				long remainingTime = getRemainingTime(elapsed);
				buffer.append(timeChrono(remainingTime));
				buffer.append(" | end: "); //$NON-NLS-1$
				buffer.append(timeEnd(remainingTime));
			}
			buffer.append(']');
			return buffer.toString();
		}
		private long getRemainingTime(long elapsed) {
			return (long) ((((double)elapsed) / this.count) * (this.all - this.count));
	    }
		private long getElapsed() {
			return System.currentTimeMillis() - this.start;
	    }
	}


	// Failure threshold
	public static final int DEFAULT_FAILURE_THRESHOLD = 10;
	int failure_threshold = DEFAULT_FAILURE_THRESHOLD;
	private int allScenariosSize;

/*
public static PerformanceResults createPerformanceResults(String scenarioPattern, File dataDir, PrintStream stream, IProgressMonitor monitor) {
	String[] builds = DB_Results.getBuilds(); // Init build names
	if (DB_Results.LAST_CURRENT_BUILD == null) {
		System.err.println("Could not find the last current build amongst the following builds list:");	//$NON-NLS-1$
		int length = builds.length;
		for (int i=0; i<length; i++) {
			System.err.println("	- "+builds[i]); //$NON-NLS-1$
		}
		return null;
	}
	if (DB_Results.LAST_BASELINE_BUILD == null) {
		System.err.println("Could not find the last baseline build amongst the following builds list:");	//$NON-NLS-1$
		int length = builds.length;
		for (int i=0; i<length; i++) {
			System.err.println("	- "+builds[i]); //$NON-NLS-1$
		}
		return null;
	}
	PerformanceResults performanceResults = new PerformanceResults(DB_Results.LAST_CURRENT_BUILD, DB_Results.LAST_BASELINE_BUILD, stream);
	performanceResults.updatedName = DB_Results.LAST_CURRENT_BUILD; // allow name to be updated
	performanceResults.read(null, scenarioPattern, dataDir, DEFAULT_FAILURE_THRESHOLD, monitor);
	return performanceResults;
}
*/
public PerformanceResults(String name, String baseline, PrintStream stream) {
	super(null, name);
	this.baselineName = baseline;
	if (baseline != null) {
		this.baselinePrefix = baseline.substring(0, baseline.lastIndexOf('_'));
	}
	this.printStream = stream;
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
	if (this.baselinePrefix != null) return this.baselinePrefix;
	return super.getBaselinePrefix();
}

/*
 * Get the build date (see #getBuildDate(String, String)).
 */
public String getBuildDate() {
	String buildName = getName();
	if (buildName == null) return ""; //$NON-NLS-1$
	return getBuildDate(getName(), getBaselinePrefix());
}

/**
 * Returns the list of all builds currently read.
 * 
 * @return The names list of all currently known builds
 */
public List getAllBuildNames() {
	SortedSet buildNames = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
	        String s1 = (String) o1;
	        String s2 = (String) o2;
	        return getBuildDate(s1).compareTo(getBuildDate(s2));
	    }
	});
	int size = size();
	for (int i=0; i<size; i++) {
		ComponentResults componentResults = (ComponentResults) this.children.get(i);
		Set builds = componentResults.getAllBuildNames();
		buildNames.addAll(builds);
	}
	return new ArrayList(buildNames);
}

/**
 * Return the list of components concerned by performance results.
 * 
 * @return The list of the components
 */
public String[] getComponents() {
	return this.components;
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

public String getName() {
	if (this.nameCanBeUpdated && this.updatedName != null) return this.updatedName;
	if (this.name == null) {
		this.name = DB_Results.LAST_CURRENT_BUILD;
	}
	return this.name;
}

/*
 * (non-Javadoc)
 * @see org.eclipse.test.internal.performance.results.AbstractResults#getPerformance()
 */
PerformanceResults getPerformance() {
	return this;
}

private Map getScenarios(String pattern, IProgressMonitor monitor, long start) throws OperationCanceledException {
	// Get scenarios from the given pattern
	print("	+ get all database scenarios..."); //$NON-NLS-1$
	if (monitor != null) monitor.subTask("Get all database scenarios..."); //$NON-NLS-1$
	Map allScenarios = DB_Results.queryAllScenarios(pattern);
	this.allScenariosSize = 0;
	List componentsSet = new ArrayList(allScenarios.keySet());
	Collections.sort(componentsSet);
	int componentsSize = componentsSet.size();
	componentsSet.toArray(this.components = new String[componentsSize]);
	for (int i=0; i<componentsSize; i++) {
		String componentName = this.components[i];
		List scenarios = (List) allScenarios.get(componentName);
		this.allScenariosSize += scenarios.size();
	}
	println(" -> "+this.allScenariosSize+" found in "+(System.currentTimeMillis()-start)+"ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	if (monitor != null) {
		monitor.worked(900);
		if (monitor.isCanceled()) throw new OperationCanceledException();
	}
	return allScenarios;
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

boolean canUpdateName() {
	return this.nameCanBeUpdated;
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * @param dataDir The directory where local files are located
 * @param monitor  The progress monitor
 */
public void readLocal(File dataDir, IProgressMonitor monitor) {
	if (dataDir == null) {
		throw new IllegalArgumentException("Must specify a directory to read local files!"); //$NON-NLS-1$
	}

	// Print title
	StringBuffer buffer = new StringBuffer("Read local performance results"); //$NON-NLS-1$
	String taskName = buffer.toString();
	if (monitor != null) {
		monitor.setTaskName(taskName);
		monitor.worked(100);
		if (monitor.isCanceled()) return;
	}
	println(buffer);

	// Update infos
	long start = System.currentTimeMillis();
	storeConfigInfo(null);
	Map allScenarios;
    try {
        allScenarios = getScenarios(null, monitor, start);
    } catch (OperationCanceledException e) {
        return;
    }
	this.nameCanBeUpdated = true;

	// Create corresponding children
	int componentsLength = this.components.length;
	for (int i=0; i<componentsLength; i++) {
		String componentName = this.components[i];
		List scenarios = (List) allScenarios.get(componentName);
		if (monitor != null) {
			int percentage = (int) ((((double)(i+1)) / (componentsLength+1)) * 100);
			monitor.setTaskName(taskName+" ("+percentage+"%)"); //$NON-NLS-1$ //$NON-NLS-2$
			StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
			subTaskBuffer.append(componentName);
			subTaskBuffer.append("..."); //$NON-NLS-1$
			monitor.subTask(subTaskBuffer.toString());
		}
		if (scenarios == null) continue;
		ComponentResults componentResults = new ComponentResults(this, componentName);
		addChild(componentResults, true);
		componentResults.readLocalFile(dataDir, scenarios);
		if (monitor != null) {
			if (monitor.isCanceled()) return;
		}
	}

	// Print time
	printGlobalTime(start);
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * @param configs All configurations to extract results. If <code>null</code>,
 * 	then all known configurations ({@link #CONFIGS})  are read.
 * @param pattern The pattern of the concerned scenarios
 * @param dataDir The directory where data will be read/stored locally.
 * 	If <code>null</code>, then database will be read instead and no storage
 * 	will be performed
 * @param threshold The failure percentage threshold over which a build result
 * 	value compared to the baseline is considered as failing.
 * @param monitor The progress monitor
 */
public void read(String[][] configs, String pattern, File dataDir, int threshold, IProgressMonitor monitor) {

	// Init
	this.scenarioPattern = pattern == null ? DB_Results.DEFAULT_SCENARIO_PATTERN : pattern;
	this.failure_threshold = threshold;

	// Print title
	StringBuffer buffer = new StringBuffer("Read performance results until build '"); //$NON-NLS-1$
	buffer.append(this.name);
	String taskName = buffer.toString();
	if (monitor != null) {
		monitor.setTaskName(taskName);
		monitor.worked(100);
		if (monitor.isCanceled()) return;
	}
	if (scenarioPattern == null) {
		buffer.append("':"); //$NON-NLS-1$
	} else {
		buffer.append("' using scenario pattern '"); //$NON-NLS-1$
		buffer.append(scenarioPattern);
		buffer.append("':"); //$NON-NLS-1$
	}
	println(buffer);

	// Update configs
	long start = System.currentTimeMillis();
	storeConfigInfo(configs);
	Map allScenarios;
    try {
        allScenarios = getScenarios(null, monitor, start);
    } catch (OperationCanceledException e) {
        return;
    }
	this.nameCanBeUpdated = false;

	// Create corresponding children
	int componentsLength = this.components.length;
	RemainingTimeGuess timeGuess = new RemainingTimeGuess(1+componentsLength+this.allScenariosSize, start);
	for (int i=0; i<componentsLength; i++) {
		String componentName = this.components[i];
		List scenarios = (List) allScenarios.get(componentName);
		if (monitor != null) {
			int percentage = (int) ((((double)(i+1)) / (componentsLength+1)) * 100);
			monitor.setTaskName(taskName+" ("+percentage+"%)"); //$NON-NLS-1$ //$NON-NLS-2$
			StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
			subTaskBuffer.append(componentName);
			subTaskBuffer.append("..."); //$NON-NLS-1$
			monitor.subTask(subTaskBuffer.toString());
		}
		if (scenarios == null) continue;
		ComponentResults componentResults = new ComponentResults(this, componentName);
		addChild(componentResults, true);
		componentResults.read(scenarios, dataDir, monitor, timeGuess);
		if (monitor != null) {
			if (monitor.isCanceled()) return;
		}
	}

	// Print time
	printGlobalTime(start);
}

private void storeConfigInfo(String[][] configs) {
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
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * @param buildName The build name to read new data
 * @param dataDir The directory where data will be stored locally
 * @param monitor 
 */
public void updateBuild(String buildName, File dataDir, IProgressMonitor monitor) {

	// Print title
	StringBuffer buffer = new StringBuffer("Read new data for "); //$NON-NLS-1$
	if (buildName == null) {
		buffer.append("all builds"); //$NON-NLS-1$
	} else {
		buffer.append(buildName);
		buffer.append(" build"); //$NON-NLS-1$
	}
	String taskName = buffer.toString();
	if (monitor != null) {
		monitor.setTaskName(taskName);
		monitor.worked(100);
		if (monitor.isCanceled()) return;
	}
	println(buffer);

	// Update info
	storeConfigInfo(null);
	long start = System.currentTimeMillis();
	Map allScenarios;
    try {
        allScenarios = getScenarios(null, monitor, start);
    } catch (OperationCanceledException e) {
        return;
    }
	this.nameCanBeUpdated = true;

	// Create corresponding children
	int componentsLength = this.components.length;
	RemainingTimeGuess timeGuess = new RemainingTimeGuess(1+componentsLength+this.allScenariosSize, start);
	for (int i=0; i<componentsLength; i++) {
		String componentName = this.components[i];
		List scenarios = (List) allScenarios.get(componentName);
		if (monitor != null) {
			int percentage = (int) ((((double)(i+1)) / (componentsLength+1)) * 100);
			monitor.setTaskName(taskName+" ("+percentage+"%)"); //$NON-NLS-1$ //$NON-NLS-2$
			StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
			subTaskBuffer.append(componentName);
			subTaskBuffer.append("..."); //$NON-NLS-1$
			monitor.subTask(subTaskBuffer.toString());
		}
		if (scenarios == null) continue;
		ComponentResults componentResults = (ComponentResults) getResults(componentName);
		componentResults.readNewData(buildName, scenarios, dataDir, monitor, timeGuess);
		if (monitor != null) {
			if (monitor.isCanceled()) return;
		}
	}
	
	// Update name
	String lastBuildDate = getBuildDate(buildName);
	if (this.name == null || lastBuildDate.compareTo(getBuildDate()) > 0) {
		this.name = buildName;
		this.updatedName = null;
	}

	// Print time
	printGlobalTime(start);
}
}
