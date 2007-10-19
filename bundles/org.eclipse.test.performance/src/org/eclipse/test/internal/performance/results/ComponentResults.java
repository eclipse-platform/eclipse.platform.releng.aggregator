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
import java.util.List;

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
	this.print = parent.print;
}

ComponentResults getComponent() {
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

/*
 * Read performance results information of the given scenarios.
 * First try to read local data if given directory is not null.
 */
void read(List scenarios, File dataDir) {
	println("Component '"+this.name+"':"); //$NON-NLS-1$ //$NON-NLS-2$
	long start = System.currentTimeMillis();
	boolean dirty = false;
	if (dataDir != null) {
		try {
	        dirty = readData(dataDir, scenarios);
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}
	int size = scenarios.size();
	long time = System.currentTimeMillis();
	for (int i=0; i<size; i++) {
		ScenarioResults scenarioResults= (ScenarioResults) scenarios.get(i);
		if (scenarioResults.parent == null) {
			scenarioResults.parent = this;
			scenarioResults.print = this.print;
			scenarioResults.read();
			dirty = true;
			addChild(scenarioResults, true);
		}
		if (dataDir != null && dirty && (System.currentTimeMillis() - time) > 300000) {
			writeData(dataDir);
			time = System.currentTimeMillis();
			dirty = false;
		}
	}
	if (dataDir != null && dirty) {
		writeData(dataDir);
	}
	printGlobalTime(start);
}

/*
 * Read the data stored locally in the given directory for all given scenarios.
 */
boolean readData(File dir, List scenarios) throws IOException {
	if (!dir.exists()) return true;
	File dataFile = new File(dir, getName()+".dat");	//$NON-NLS-1$
	if (!dataFile.exists()) return true;
	DB_Results.queryAllVariations(getPerformance().getConfigurationsPattern());
	DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
	boolean valid = false, dirty = false;
	int size = 0;
	try {
		String lastBuildName = stream.readUTF();
		size = stream.readInt();
		for (int i=0; i<size; i++) {
			int scenario_id = stream.readInt();
			ScenarioResults scenarioResults = getScenarioResults(scenarios, scenario_id);
			if (scenarioResults == null) {
				// scenario should always have been created while reading all scenarios from database
				return dirty;
			}
			scenarioResults.parent = this;
			scenarioResults.print = this.print;
			if (scenarioResults.readData(stream, lastBuildName)) {
				dirty = true;
			}
			addChild(scenarioResults, true);
		}
		valid = true;
	} finally {
		stream.close();
		if (valid) {
			println("		=> "+size+" scenarios data were read from file "+dataFile); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			dataFile.delete();
			println("		=> deleted file "+dataFile+" as it contained invalid data!!!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	return dirty;
}

/*
 * Write the component results data to the file '<component name>.dat' in the given directory.
 */
public void writeData(File dir) {
	if (!dir.exists() && !dir.mkdirs()) {
		System.err.println("can't create directory "+dir); //$NON-NLS-1$
	}
	File tmpFile = new File(dir, getName()+".tmp"); //$NON-NLS-1$
	File logFile = new File(dir, getName()+".dat");	//$NON-NLS-1$
	try {
		DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
		int size = this.children.size();
		stream.writeUTF(getPerformance().getName());
		stream.writeInt(size);
		for (int i=0; i<size; i++) {
			ScenarioResults scenarioResults = (ScenarioResults) this.children.get(i);
			scenarioResults.write(stream);
		}
		stream.close();
		if (logFile.exists()) logFile.delete();
		tmpFile.renameTo(logFile);
		println("		=> extracted data written in file "+logFile); //$NON-NLS-1$
	} catch (FileNotFoundException e) {
		System.err.println("can't create output file"+tmpFile); //$NON-NLS-1$
	} catch (IOException e) {
		e.printStackTrace();
	}
}

}
