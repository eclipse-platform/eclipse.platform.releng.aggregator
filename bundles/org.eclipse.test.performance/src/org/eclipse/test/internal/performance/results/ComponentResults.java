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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.SubMonitor;

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

String[] getAllSortedBuildNames() {
	Set allBuildNames = getAllBuildNames();
	String[] sortedNames = new String[allBuildNames.size()];
	allBuildNames.toArray(sortedNames);
	Arrays.sort(sortedNames, new Comparator() {
		public int compare(Object o1, Object o2) {
	        String s1 = (String) o1;
	        String s2 = (String) o2;
	        return getBuildDate(s1).compareTo(getBuildDate(s2));
	    }
	});
	return sortedNames;
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

private String lastBuildName(int kind) {
	String[] builds = getAllSortedBuildNames();
	int idx = builds.length-1;
	String lastBuildName = builds[idx--];
	switch (kind) {
		case 1: // no ref
			while (lastBuildName.startsWith(VERSION_REF)) {
				lastBuildName = builds[idx--];
			}
			break;
		case 2: // only I-build or M-build
			char ch = lastBuildName.charAt(0);
			while (ch != 'I' && ch != 'M') {
				lastBuildName = builds[idx--];
				ch = lastBuildName.charAt(0);
			}
			break;
		default:
			break;
	}
	return lastBuildName;
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
				/*
				scenarioResults = new ScenarioResults(-1, null, null);
				scenarioResults.parent = this;
				scenarioResults.readData(stream);
				*/
				// Should no longer occur as we get all scenarios from database now
				throw new RuntimeException("Unexpected unfound scenario!"); //$NON-NLS-1$
			}
			scenarioResults.parent = this;
			scenarioResults.printStream = this.printStream;
			scenarioResults.readData(stream);
			addChild(scenarioResults, true);
			if (this.printStream != null) this.printStream.print('.');
		}
		println();
		println("	=> "+size+" scenarios data were read from file "+dataFile); //$NON-NLS-1$ //$NON-NLS-2$
		
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
 * Read the database values for a build name and a list of scenarios.
 * The database is read only if the components does not already knows the
 * given build (i.e. if it has not been already read) or if the force arguments is set.
 */
void updateBuild(String buildName, List scenarios, boolean force, File dataDir, SubMonitor subMonitor, PerformanceResults.RemainingTimeGuess timeGuess) {
	
	// Read all variations
	println("Component '"+this.name+"':"); //$NON-NLS-1$ //$NON-NLS-2$
	PerformanceResults performanceResults = getPerformance();
	DB_Results.queryAllVariations(performanceResults.getConfigurationsPattern());

	// manage monitor
	int size = scenarios.size();
	subMonitor.setWorkRemaining(size+1);
	StringBuffer buffer = new StringBuffer("Component "); //$NON-NLS-1$
	buffer.append(this.name);
	buffer.append("..."); //$NON-NLS-1$
	String title = buffer.toString();
	subMonitor.subTask(title+timeGuess.display());
	timeGuess.count++;
	subMonitor.worked(1);
	if (subMonitor.isCanceled()) return;

	// Read new values for the local result
	boolean dirty = false;
	long readTime = System.currentTimeMillis();
	String log = " - read scenarios from DB:"; //$NON-NLS-1$
	if (size > 0) {
		for (int i=0; i<size; i++) {
	
			// manage monitor
			subMonitor.subTask(title+timeGuess.display());
			timeGuess.count++;
			if (log != null) {
				println(log);
				log = null;
			}
			
			// read results
			ScenarioResults nextScenarioResults= (ScenarioResults) scenarios.get(i);
			ScenarioResults scenarioResults = (ScenarioResults) getResults(nextScenarioResults.id);
			if (scenarioResults == null) {
				// Scenario is not known yet, force an update
				scenarioResults = nextScenarioResults;
				scenarioResults.parent = this;
				scenarioResults.printStream = this.printStream;
				scenarioResults.updateBuild(buildName, true);
				dirty = true;
				addChild(scenarioResults, true);
			} else {
				if (scenarioResults.updateBuild(buildName, force)) {
					dirty = true;
				}
			}
			if (dataDir != null && dirty && (System.currentTimeMillis() - readTime) > 300000) { // save every 5mn
				writeData(buildName, dataDir, true, true);
				dirty = false;
				readTime = System.currentTimeMillis();
			}
	
			// manage monitor
			subMonitor.worked(1);
			if (subMonitor.isCanceled()) return;
		}
	}
	
	// Write local files
	if (dataDir != null) {
		writeData(buildName, dataDir, false, dirty);
	}

	// Print global time
	printGlobalTime(readTime);

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
		stream.writeUTF(lastBuildName(0));
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
