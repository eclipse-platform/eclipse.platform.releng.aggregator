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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;


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

	String[] allBuildNames = null;
	Map allScenarios;
	String baselineName; // Name of the baseline build used for comparison
	String baselinePrefix;
	private String scenarioPattern = "%"; //$NON-NLS-1$
	private String[] components;
	String[] configNames, sortedConfigNames;
	String[] configBoxes, sortedConfigBoxes;
	private String configPattern;

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

public PerformanceResults(String name, String baseline, String baselinePrefix, PrintStream stream) {
	super(null, name);
	this.baselineName = baseline;
	this.baselinePrefix = baselinePrefix;
	this.printStream = stream;
	setDefaults();
}

/**
 * Returns the list of all builds currently read.
 * 
 * @return The names list of all currently known builds
 */
public String[] getAllBuildNames() {
	if (this.allBuildNames == null) {
		setAllBuildNames();
	}
	return this.allBuildNames;
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
	String buildName = getName();
	if (buildName == null) return ""; //$NON-NLS-1$
	return getBuildDate(getName(), getBaselinePrefix());
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

/**
 * Return the name of the last build name except baselines.
 * 
 * @return the name of the last build
 */
public String getLastBuildName() {
	return getLastBuildName(1/*all except baselines*/);
}
/**
 * Return the name of the last build name
 * 
 * @param kind Decide what kind of build is taken into account
 * 	0: all kind of build
 * 	1: all except baseline builds
 * 	2: all except baseline and nightly builds
 * 	3: only integration builds
 * @return the name of the last build of the selected kind
 */
public String getLastBuildName(int kind) {
	if (this.name == null) {
		getAllBuildNames(); // init build names if necessary
		int idx = this.allBuildNames.length-1;
		this.name = this.allBuildNames[idx];
		if (kind > 0) {
			loop: while (idx-- >= 0) {
				switch (this.name.charAt(0)) {
					case 'N':
						if (kind < 2) break loop;
						break;
					case 'M':
						if (kind < 3) break loop;
						break;
					case 'I':
						if (kind < 4) break loop;
						break;
				}
				this.name = this.allBuildNames[idx];
			}
		}
	}
	return this.name;
}

public String getName() {
	if (this.name == null) {
		setAllBuildNames();
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

private String[] read(boolean local, String buildName, String[][] configs, boolean force, File dataDir, String taskName, SubMonitor subMonitor) {
	if (local && dataDir == null) {
		throw new IllegalArgumentException("Must specify a directory to read local files!"); //$NON-NLS-1$
	}
	subMonitor.setWorkRemaining(100);
	
	// Reset
	boolean reset = local || (buildName == null && force);
	if (reset) reset();

	// Update info
	setConfigInfo(configs);
	long start = System.currentTimeMillis();
	int allScenariosSize;
    try {
        allScenariosSize = readScenarios(buildName, subMonitor.newChild(10));
    } catch (OperationCanceledException e) {
        return null;
    }

	// Create corresponding children
	int componentsLength = this.components.length;
	subMonitor.setWorkRemaining(componentsLength);
	RemainingTimeGuess timeGuess = null;
	for (int i=0; i<componentsLength; i++) {
		String componentName = this.components[i];
		List scenarios = (List) allScenarios.get(componentName);
		
		// Manage monitor
		int percentage = (int) ((((double)(i+1)) / (componentsLength+1)) * 100);
		subMonitor.setTaskName(taskName+" ("+percentage+"%)"); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
		subTaskBuffer.append(componentName);
		subTaskBuffer.append("..."); //$NON-NLS-1$
		subMonitor.subTask(subTaskBuffer.toString());
		
		// Get component results
		if (scenarios == null) continue;
		ComponentResults componentResults;
		if (reset) {
			componentResults = new ComponentResults(this, componentName);
			addChild(componentResults, true);
		} else {
			componentResults = (ComponentResults) getResults(componentName);
		}
		
		// Read the component results
		if (local) {
			try {
				componentResults.readLocalFile(dataDir, scenarios);
			}
			catch (FileNotFoundException ex) {
				return null;
			}
			subMonitor.worked(1);
		} else {
			if (timeGuess == null) {
				timeGuess = new RemainingTimeGuess(1+componentsLength+allScenariosSize, start);
			}
			componentResults.updateBuild(buildName, scenarios, force, dataDir, subMonitor.newChild(1), timeGuess);
		}
		if (subMonitor.isCanceled()) return null;
	}

	// Update names
	setAllBuildNames();

	// Print time
	printGlobalTime(start);
	
	return this.allBuildNames;
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * @param buildName The name of the build
 * @param configs All configurations to extract results. If <code>null</code>,
 * 	then all known configurations ({@link #CONFIGS})  are read.
 * @param pattern The pattern of the concerned scenarios
 * @param dataDir The directory where data will be read/stored locally.
 * 	If <code>null</code>, then database will be read instead and no storage
 * 	will be performed
 * @param threshold The failure percentage threshold over which a build result
 * 	value compared to the baseline is considered as failing.
 * @param monitor The progress monitor
 * 
 * @return All known builds
 */
public String[] readAll(String buildName, String[][] configs, String pattern, File dataDir, int threshold, IProgressMonitor monitor) {

	// Init
	this.scenarioPattern = pattern == null ? "%" : pattern; //$NON-NLS-1$
	this.failure_threshold = threshold;
	SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);

	// Set default names
	setDefaults();

	// Read local file first
	String[] names = read(true, null, configs, true, dataDir, null, subMonitor.newChild(100));
	
	// Read database contents after
	boolean force = names==null;
	return read(false, force ? null : buildName, configs, force, dataDir, null, subMonitor.newChild(900));
}

/**
 * Read all data from performance database for the given configurations
 * and scenario pattern.
 * 
 * Note that calling this method flush all previous read data.
 * 
 * @param dataDir The directory where local files are located
 * @param monitor  The progress monitor
 * @return The list of build names read in local files
 */
public String[] readLocal(File dataDir, IProgressMonitor monitor) {

	// Print title
	String taskName = "Read local performance results"; //$NON-NLS-1$
	println(taskName);
	
	// Create monitor
	SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);
	subMonitor.setTaskName(taskName);
	
	// Read
	return read(true, null, null, true, dataDir, taskName, subMonitor);
}

private int readScenarios(String buildName, SubMonitor subMonitor) throws OperationCanceledException {
	subMonitor.setWorkRemaining(10);
	long start = System.currentTimeMillis();
	String titleSuffix;
	if (buildName == null) {
		titleSuffix = "all database scenarios..."; //$NON-NLS-1$
	} else {
		titleSuffix = "all database scenarios for "+buildName+" build..."; //$NON-NLS-1$ //$NON-NLS-2$
	}
	print("	+ get "+titleSuffix); //$NON-NLS-1$
	subMonitor.subTask("Get "+titleSuffix); //$NON-NLS-1$
	this.allScenarios = DB_Results.queryAllScenarios(this.scenarioPattern, buildName);
	int allScenariosSize = 0;
	List componentsSet = new ArrayList(this.allScenarios.keySet());
	Collections.sort(componentsSet);
	int componentsSize = componentsSet.size();
	componentsSet.toArray(this.components = new String[componentsSize]);
	for (int i=0; i<componentsSize; i++) {
		String componentName = this.components[i];
		List scenarios = (List) this.allScenarios.get(componentName);
		allScenariosSize += scenarios.size();
	}
	println(" -> "+allScenariosSize+" found in "+(System.currentTimeMillis()-start)+"ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	subMonitor.worked(10);
	if (subMonitor.isCanceled()) throw new OperationCanceledException();
	return allScenariosSize;
}

private void reset() {
	this.allBuildNames = null;
	this.children = new ArrayList();
	this.name = null;
	this.components = null;
}

private void setAllBuildNames() {
	SortedSet builds = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
	        String s1 = (String) o1;
	        String s2 = (String) o2;
	        return getBuildDate(s1).compareTo(getBuildDate(s2));
	    }
	});
	int size = size();
	for (int i=0; i<size; i++) {
		ComponentResults componentResults = (ComponentResults) this.children.get(i);
		Set names = componentResults.getAllBuildNames();
		builds.addAll(names);
	}
	int buildsSize = builds.size();
	this.allBuildNames = new String[buildsSize];
	if (buildsSize > 0) {
		builds.toArray(this.allBuildNames);
		int idx = this.allBuildNames.length-1;
		this.name = this.allBuildNames[idx--];
		while (this.name.startsWith(VERSION_REF)) {
			this.name = this.allBuildNames[idx--];
		}
	}
}

private void setConfigInfo(String[][] configs) {
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

private void setDefaults() {
	
	// Set name if null
	if (this.name == null) {
		setAllBuildNames();
		if (this.name == null) { // does not know any build
			this.name = DB_Results.getLastCurrentBuild();
			if (this.name == null) {
				throw new RuntimeException("Cannot find any current build!"); //$NON-NLS-1$
			}
			if (this.printStream != null) {
				this.printStream.println("	+ no build specified => use last one: "+this.name); //$NON-NLS-1$
			}
		}
	}

	// Init baseline name if not set
	if (this.baselineName == null) {
		String buildDate = AbstractResults.getBuildDate(getName());
		this.baselineName = DB_Results.getLastBaselineBuild(buildDate);
		if (this.baselineName == null) {
			throw new RuntimeException("Cannot find any baseline to refer!"); //$NON-NLS-1$
		}
		if (this.printStream != null) {
			this.printStream.println("	+ no baseline specified => use last one: "+this.baselineName); //$NON-NLS-1$
		}
	}

	// Init baseline prefix if not set
	if (this.baselinePrefix == null) {
		// Assume that baseline name format is *always* x.y_yyyyMMddhhmm_yyyyMMddhhmm
		this.baselinePrefix = this.baselineName.substring(0, this.baselineName.lastIndexOf('_'));
	}
	
	// Set scenario pattern default
	if (this.scenarioPattern == null) {
		this.scenarioPattern = "%"; //$NON-NLS-1$
	}

	// Flush print stream
	if (this.printStream != null) {
		this.printStream.println();
		this.printStream.flush();
	}
}

/**
 * Update a given build information with database contents.
 * 
 * @param builds The builds to read new data
 * @param force Force the update from the database, even if the build is
 * 	already known.
 * @param dataDir The directory where data should be stored locally if necessary.
 * 	If <code>null</code>, then information changes won't be persisted.
 * @param monitor The progress monitor
 * @return All known builds
 */
public String[] updateBuilds(String[] builds, boolean force, File dataDir, IProgressMonitor monitor) {

	// Print title
	StringBuffer buffer = new StringBuffer("Update data for "); //$NON-NLS-1$
	int length = builds == null ? 0 : builds.length;
	switch (length) {
		case 0:
			buffer.append("all builds"); //$NON-NLS-1$
			reset();
			break;
		case 1:
			buffer.append(builds[0]);
			buffer.append(" build"); //$NON-NLS-1$
			break;
		default:
			buffer.append("several builds"); //$NON-NLS-1$
			break;
	}
	String taskName = buffer.toString();
	println(buffer);

	// Create sub-monitor
	SubMonitor subMonitor = SubMonitor.convert(monitor, 1000*length);
	subMonitor.setTaskName(taskName);
	
	// Read
	for (int i=0; i<length;  i++) {
		read(false, builds[i], null, force, dataDir, taskName, subMonitor.newChild(1000));
	}
	
	// Return new builds list
	return this.allBuildNames;
}

/**
 * Update a given build information with database contents.
 * 
 * @param buildName The build name to read new data
 * @param force Force the update from the database, even if the build is
 * 	already known.
 * @param dataDir The directory where data should be stored locally if necessary.
 * 	If <code>null</code>, then information changes won't be persisted.
 * @param monitor The progress monitor
 * @return All known builds
 */
public String[] updateBuild(String buildName, boolean force, File dataDir, IProgressMonitor monitor) {

	// Print title
	StringBuffer buffer = new StringBuffer("Update data for "); //$NON-NLS-1$
	if (buildName == null) {
		buffer.append("all builds"); //$NON-NLS-1$
		reset();
	} else {
		buffer.append(buildName);
		buffer.append(" build"); //$NON-NLS-1$
	}
	String taskName = buffer.toString();
	println(buffer);

	// Create sub-monitor
	SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);
	subMonitor.setTaskName(taskName);
	
	// Read
	read(false, buildName, null, force, dataDir, taskName, subMonitor);

	// Refresh name
	if (buildName != null && !buildName.startsWith(VERSION_REF)) {
		this.name = buildName;
	}
	
	// Return new list all build names
	return this.allBuildNames;
}

}
