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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Class to handle performance results of an eclipse component
 * (for example 'org.eclipse.jdt.core').
 * 
 * It gives access to results for each scenario run for this component.
 * 
 * @see ScenarioResults
 */
public class ComponentResults extends AbstractResults {

public ComponentResults(AbstractResults parent, String name) {
	super(parent, name);
	this.printStream = parent.printStream;
}

Set getAllBuildNames() {
	Set buildNames = new HashSet();
	int size = size();
	for (int i=0; i<size; i++) {
		ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
		Set builds = scenarioResults.getAllBuildNames();
		buildNames.addAll(builds);
	}
	return buildNames;
}

ComponentResults getComponentResults() {
	return this;
}

private ScenarioResults getScenarioResults(List scenarios, int searchedId) {
	int size = scenarios.size();
	for (int i=0; i<size; i++) {
		ScenarioResults scenarioResults = (ScenarioResults) scenarios.get(i);
		if (scenarioResults.id == searchedId) {
			return scenarioResults;
		}
	}
	return null;
}

/**
 * Returns a list of scenario results which have a summary
 * 
 * @param global Indicates whether the summary must be global or not.
 * @param config Configuration name
 * @return A list of {@link ScenarioResults scenario results} which have a summary
 */
public List getSummaryScenarios(boolean global, String config) {
	int size= size();
	List scenarios = new ArrayList(size);
	for (int i=0; i<size; i++) {
		ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
		ConfigResults configResults = scenarioResults.getConfigResults(config);
		if (configResults != null) {
			BuildResults buildResults = configResults.getCurrentBuildResults();
			if ((global && buildResults.summaryKind == 1) || (!global && buildResults.summaryKind >= 0)) {
				scenarios.add(scenarioResults);
			}
		}
	}
	return scenarios;
}

private boolean hasLocalData(File dataDir) {
	boolean hasData = dataDir != null && dataDir.exists() && (new File(dataDir, getName()+".dat")).exists(); //$NON-NLS-1$
	return hasData;
}

/*
 * Read performance results information of the given scenarios.
 * First try to read data for existing scenarios and complete possible new ones
 * by specific request to the database.
 */
void read(List scenarios, File dataDir, IProgressMonitor monitor, PerformanceResults.RemainingTimeGuess timeGuess) {
	println("Component '"+this.name+"':"); //$NON-NLS-1$ //$NON-NLS-2$
	long start = System.currentTimeMillis();
	if (hasLocalData(dataDir)) {
        readData(dataDir, scenarios, monitor, timeGuess);
	}
	try {
		if (getPerformance().canUpdateName()) {
			readNewScenarios(null, scenarios, dataDir, monitor, timeGuess);
		}
	}
	catch (OperationCanceledException oce) {
		return;
	}
	printGlobalTime(start);
}

/*
 * Read the data stored locally in the given directory for all given scenarios.
 * Update the missing information from the database.
 */
void readData(File dataDir, List scenarios, IProgressMonitor monitor, PerformanceResults.RemainingTimeGuess timeGuess) {
	DB_Results.queryAllVariations(getPerformance().getConfigurationsPattern());
	boolean dirty = false;

	// read local file and update last build name if local data file has a more recent one
	String lastBuildName = readLocalFile(dataDir, scenarios);

	// manage monitor
	if (monitor != null) {
		StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
		subTaskBuffer.append(this.name);
		subTaskBuffer.append("..."); //$NON-NLS-1$
		subTaskBuffer.append(timeGuess.display());
		timeGuess.count++;
		monitor.subTask(subTaskBuffer.toString());
		monitor.worked(100);
		if (monitor.isCanceled()) return;
	}

	// Read new values for the local result
	boolean first = true;
	long readTime = System.currentTimeMillis();
	int size = size();
	int step = 900 / size;
	for (int i=0; i<size; i++) {

		// manage monitor
		if (monitor != null) {
			StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
			subTaskBuffer.append(this.name);
			subTaskBuffer.append("..."); //$NON-NLS-1$
			subTaskBuffer.append(timeGuess.display());
			timeGuess.count++;
			monitor.subTask(subTaskBuffer.toString());
		}
		
		// read results
		ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
		if (first) {
			println(" - read DB contents:"); //$NON-NLS-1$
			first = false;
		}
		long start = System.currentTimeMillis();
		boolean newData = scenarioResults.readNewData(lastBuildName, false);
		long time = System.currentTimeMillis()-start;
		if (newData) {
			dirty = true;
			print(", infos..."); //$NON-NLS-1$
			start = System.currentTimeMillis();
			scenarioResults.completeResults(lastBuildName);
			time = System.currentTimeMillis()-start;
			println(timeString(time));
		}
		if (dataDir != null && dirty && (System.currentTimeMillis() - readTime) > 300000) { // save every 5mn
			writeData(null, dataDir, true, true);
			readTime = System.currentTimeMillis();
			dirty = false;
		}

		// manage monitor
		if (monitor != null) {
			monitor.worked(step);
			if (monitor.isCanceled()) return;
		}
	}
	
	// Write local files
	if (dataDir != null) {
		writeData(null, dataDir, false, dirty);
	}
}

/*
 * Read local file contents and populate the results model with the collected
 * information.
 */
String readLocalFile(File dir, List scenarios) {
	if (!dir.exists()) return null;
	File dataFile = new File(dir, getName()+".dat");	//$NON-NLS-1$
	if (!dataFile.exists()) return null;
	DataInputStream stream = null;
	try {
		// Read local file info
		stream = new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
		print(" - read local files info"); //$NON-NLS-1$
		String lastBuildName = stream.readUTF(); // first string is the build name
	
		// Next field is the number of scenarios for the component
		int size = stream.readInt();
	
		// Then follows all the scenario information
		for (int i=0; i<size; i++) {
			// ... which starts with the scenario id
			int scenario_id = stream.readInt();
			ScenarioResults scenarioResults = getScenarioResults(scenarios, scenario_id);
			if (scenarioResults == null) {
				// this can happen if scenario pattern does not cover all those stored in local data file
				// hence, creates a fake scenario to read the numbers and skip to the next scenario
				scenarioResults = new ScenarioResults(-1, null, null);
				scenarioResults.parent = this;
				scenarioResults.readData(stream);
			} else {
				scenarioResults.parent = this;
				scenarioResults.printStream = this.printStream;
				scenarioResults.readData(stream);
				addChild(scenarioResults, true);
			}
			if (this.printStream != null) this.printStream.print('.');
		}
		println();
		println("	=> "+size+" scenarios data were read from file "+dataFile); //$NON-NLS-1$ //$NON-NLS-2$

		// Update performance name
		String lastBuildDate = getBuildDate(lastBuildName);
		PerformanceResults performanceResults = getPerformance();
		if (performanceResults.name == null ) {
			performanceResults.name = lastBuildName;
		}
		else if (performanceResults.canUpdateName() && lastBuildDate.compareTo(performanceResults.getBuildDate()) > 0) {
			performanceResults.updatedName = lastBuildName;
		}
		
		// Return last build name stored in the local files
		return lastBuildName;
	} catch (IOException ioe) {
		println("	!!! "+dataFile+" should be deleted as it contained invalid data !!!"); //$NON-NLS-1$ //$NON-NLS-2$
	} finally {
		try {
	        stream.close();
        } catch (IOException e) {
	        // nothing else to do!
        }
	}
	return null;
}

/*
 * Read the database values for the given build name.
 * Compare with the given scenarios list to see if new ones has been created
 * since the last read operation.
 */
void readNewData(String buildName, List scenarios, File dataDir, IProgressMonitor monitor, PerformanceResults.RemainingTimeGuess timeGuess) {
	println("Component '"+this.name+"':"); //$NON-NLS-1$ //$NON-NLS-2$
	PerformanceResults performanceResults = getPerformance();
	DB_Results.queryAllVariations(performanceResults.getConfigurationsPattern());

	// manage monitor
	if (monitor != null) {
		StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
		subTaskBuffer.append(this.name);
		subTaskBuffer.append("..."); //$NON-NLS-1$
		subTaskBuffer.append(timeGuess.display());
		timeGuess.count++;
		monitor.subTask(subTaskBuffer.toString());
		monitor.worked(100);
		if (monitor.isCanceled()) return;
	}

	// Read new values for the local result
	boolean dirty = false;
	boolean first = true;
	long readTime = System.currentTimeMillis();
	int size = size();
	if (size > 0) {
		int step = 900 / size;
		for (int i=0; i<size; i++) {
	
			// manage monitor
			if (monitor != null) {
				StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
				subTaskBuffer.append(this.name);
				subTaskBuffer.append("..."); //$NON-NLS-1$
				subTaskBuffer.append(timeGuess.display());
				timeGuess.count++;
				monitor.subTask(subTaskBuffer.toString());
			}
			
			// read results
			ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
			if (first) {
				println(" - read DB contents:"); //$NON-NLS-1$
				first = false;
			}
			if (buildName == null) {
				scenarioResults.read(null, -1);
				dirty = true;
			} else if (scenarioResults.readNewData(buildName, true)) {
				dirty = true;
			}
			if (dataDir != null && dirty && (System.currentTimeMillis() - readTime) > 300000) { // save every 5mn
				writeData(buildName, dataDir, true, true);
				dirty = false;
				readTime = System.currentTimeMillis();
			}
	
			// manage monitor
			if (monitor != null) {
				monitor.worked(step);
				if (monitor.isCanceled()) return;
			}
		}
	}
	
	// Write local files
	if (dataDir != null) {
		writeData(buildName, dataDir, false, dirty);
	}

	// Identify unknown scenarios
	List unknownScenarios = new ArrayList();
	size = scenarios.size();
	for (int i=0; i<size; i++) {
		ScenarioResults scenarioResults= (ScenarioResults) scenarios.get(i);
		if (getResults(scenarioResults.id) == null) {
			unknownScenarios.add(scenarioResults);
		}
	}
	
	// Read new scenarios
	try {
		readNewScenarios(buildName, unknownScenarios, dataDir, monitor, timeGuess);
	}
	catch (OperationCanceledException oce) {
		return;
	}

	// Update performance name
	if (buildName != null) {
		String lastBuildDate = getBuildDate(buildName);
		if (performanceResults.canUpdateName() && lastBuildDate.compareTo(performanceResults.getBuildDate()) > 0) {
			performanceResults.updatedName = buildName;
		}
	}

	// Print global time
	printGlobalTime(readTime);

}

/*
 * Read new scenarios values from database.
 * New scenarios are those in the list without parent, hence not populated
 * with a previous read operation.
 */
void readNewScenarios(String buildName, List scenarios, File dataDir, IProgressMonitor monitor, PerformanceResults.RemainingTimeGuess timeGuess) {
	int size = scenarios.size();
	if (size == 0) return;
	long time = System.currentTimeMillis();
	boolean dirty = false;
	boolean first = true;
	boolean hasData = hasLocalData(dataDir);
	int step = hasData ? 0 : 1000 / size;
	for (int i=0; i<size; i++) {

		// manage monitor
		if (!hasData && monitor != null) {
			StringBuffer subTaskBuffer = new StringBuffer("Component "); //$NON-NLS-1$
			subTaskBuffer.append(this.name);
			subTaskBuffer.append("..."); //$NON-NLS-1$
			subTaskBuffer.append(timeGuess.display());
			timeGuess.count++;
			monitor.subTask(subTaskBuffer.toString());
		}

		// read results
		ScenarioResults scenarioResults= (ScenarioResults) scenarios.get(i);
		if (scenarioResults.parent == null) {
			if (first) {
				println(" - read new scenarios:"); //$NON-NLS-1$
				first = false;
			}
			scenarioResults.parent = this;
			scenarioResults.printStream = this.printStream;
			scenarioResults.readNewData(buildName, true);
			dirty = true;
			addChild(scenarioResults, true);
		}
		if (dataDir != null && dirty && (System.currentTimeMillis() - time) > 300000) { // save every 5mn
			writeData(buildName, dataDir, true, true);
			time = System.currentTimeMillis();
			dirty = false;
		}

		// manage monitor
		if (monitor != null) {
			if (!hasData) monitor.worked(step);
			if (monitor.isCanceled()) throw new OperationCanceledException();
		}
	}
	
	// Write new local files
	if (dataDir != null) {
		writeData(buildName, dataDir, false, dirty);
	}
}

/*
 * Write the component results data to the file '<component name>.dat' in the given directory.
 */
void writeData(String buildName, File dir, boolean temp, boolean dirty) {
	if (!dir.exists() && !dir.mkdirs()) {
		System.err.println("can't create directory "+dir); //$NON-NLS-1$
	}
	File tmpFile = new File(dir, getName()+".tmp"); //$NON-NLS-1$
	File dataFile = new File(dir, getName()+".dat"); //$NON-NLS-1$
	if (!dirty) { // only possible on final write
		if (tmpFile.exists()) {
			if (dataFile.exists()) dataFile.delete();
			tmpFile.renameTo(dataFile);
			println("	=> rename temporary file to "+dataFile); //$NON-NLS-1$
		}
		return;
	}
	if (tmpFile.exists()) {
		tmpFile.delete();
	}
	File file;
	if (temp) {
		file = tmpFile;
	} else {
		if (dataFile.exists()) {
			dataFile.delete();
		}
		file = dataFile;
	}
	try {
		DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		int size = this.children.size();
		stream.writeUTF(buildName != null ? buildName : getPerformance().getName());
		stream.writeInt(size);
		for (int i=0; i<size; i++) {
			ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
			scenarioResults.write(stream);
		}
		stream.close();
		println("	=> extracted data "+(temp?"temporarily ":"")+"written in file "+file); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	} catch (FileNotFoundException e) {
		System.err.println("can't create output file"+file); //$NON-NLS-1$
	} catch (IOException e) {
		e.printStackTrace();
	}
}

}
