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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.test.internal.performance.InternalDimensions;
import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.data.Dim;

/**
 * Abstract class to store performance results.
 * 
 * Each results gives access to specific children depending on model.
 */
public abstract class AbstractResults implements Comparable {

	/**
	 * The lis of supported dimensions.
	 * <p>
	 * Currently only {@link InternalDimensions#ELAPSED_PROCESS}
	 * and {@link InternalDimensions#CPU_TIME}.
	 */
	public final static Dim[] SUPPORTED_DIMS = {
		InternalDimensions.ELAPSED_PROCESS,
		InternalDimensions.CPU_TIME
	};

	/**
	 * The list of possible configurations.
	 * <p>
	 * Only used if no specific configurations are specified
	 * (see {@link PerformanceResults#read(String[][], String)}.
	 */
	public final static String[] CONFIGS;

	/**
	 * The list of possible test boxes.
	 * <p>
	 * Only used if no specific configurations are specified
	 * (see {@link PerformanceResults#read(String[][], String)}.
	 */
	public final static String[] BOXES = {
		"RHEL 4.0 Sun 1.4.2_10 (3 GHz 2.5 GB)", //$NON-NLS-1$
		"Win XP Sun 1.4.2_10 (3 GHz 2 GB)", //$NON-NLS-1$
		"RHEL 3.0 Sun 1.4.2_10 (3 GHz 2 GB)", //$NON-NLS-1$
		"Win XP Sun 1.4.2_10 (2 GHz 512 MB)", //$NON-NLS-1$
		"RHEL 3.0 Sun 1.4.2_10 (2 GHz 512 MB)", //$NON-NLS-1$
	};

	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm"); //$NON-NLS-1$

	// Initialize constant fields depending on DB version
	public final static String VERSION_REF, VERSION;
	static {
		String dbName = PerformanceTestPlugin.getDBName();
		String version;
	
		// Initialize reference version and database directory
		char mainVersion;
		char minorVersion;
		if (dbName.equals("perfDB")) { //$NON-NLS-1$
			version = "_R3.3"; //$NON-NLS-1$
			VERSION_REF = "3.2"; //$NON-NLS-1$
			mainVersion = '3';
			minorVersion = '3';
			VERSION = "3.3"; //$NON-NLS-1$
		} else {
			mainVersion = dbName.charAt(dbName.length()-2);
			minorVersion = dbName.charAt(dbName.length()-1);
			version = ""; //$NON-NLS-1$
			VERSION_REF = mainVersion+"."+(char)(minorVersion-1); //$NON-NLS-1$
			VERSION = mainVersion+"."+minorVersion; //$NON-NLS-1$
		}
	
		// Initialize configuration names
		CONFIGS = new String[] {
			"eclipseperflnx3"+version, //$NON-NLS-1$
			"eclipseperfwin2"+version, //$NON-NLS-1$
			"eclipseperflnx2"+version, //$NON-NLS-1$
			"eclipseperfwin1"+version, //$NON-NLS-1$
			"eclipseperflnx1"+version, //$NON-NLS-1$
		};
	}

	AbstractResults parent;
	int id = -1;
	String name;
	List children;
	private boolean newLine = true;
	boolean print = false;

/*
 * Return the build date as yyyyMMddHHmm
 */
static String getBuildDate(String buildName, String baselinePrefix) {

	// Baseline name
	if (buildName.startsWith(baselinePrefix)) {
		int length = buildName.length();
		return buildName.substring(length-12, length);
	}
	
	// Build name
	char first = buildName.charAt(0);
	if (first == 'N' || first == 'I' || first == 'M') { // TODO (frederic) should be buildIdPrefixes...
		return buildName.substring(1, 9)+buildName.substring(10, 14);
	}
	
	// Try with date format
	int length = buildName.length() - 12 /* length of date */;
	for (int i=0; i<=length; i++) {
		try {
			String substring = i == 0 ? buildName : buildName.substring(i);
			DATE_FORMAT.parse(substring);
			return substring; // if no exception is raised then the substring has a correct date format => return it
		} catch(ParseException ex) {
			// skip
		}
	}
	return null;
}

static Dim getDimension(int id) {
	int length = SUPPORTED_DIMS.length;
	for (int i=0; i<length; i++) {
		if (SUPPORTED_DIMS[i].getId() == id) {
			return SUPPORTED_DIMS[i];
		}
	}
	return null;
}

AbstractResults(AbstractResults parent, String name) {
	this.parent = parent;
	this.children = new ArrayList();
	this.name = name;
}

AbstractResults(AbstractResults parent, int id) {
	this.parent = parent;
	this.children = new ArrayList();
	this.id = id;
}

/*
 * Add a child to current results, using specific sort
 * order if specified.
 */
void addChild(Comparable child, boolean sort) {
	if (sort) {
		int size = this.children.size();
		for (int i=0; i<size; i++) {
			Object results = this.children.get(i);
			if (child.compareTo(results) < 0) {
				this.children.add(i, child);
				return;
			}
		}
	}
	this.children.add(child);
}

/**
 * Compare the results to the given one using the name.
 * 
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
public int compareTo(Object obj) {
	if (obj instanceof AbstractResults) {
		AbstractResults res = (AbstractResults) obj;
		return getName().compareTo(res.getName());
	}
	return -1;
}

/**
 * Returns whether two results are equals using the name
 * to compare them.
 * 
 * @param obj  The results to compare with
 * @return <code>true</code> if the name are equals,
 * 	<code>false</code> otherwise
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
public boolean equals(Object obj) {
	if (obj instanceof AbstractResults) {
		return this.name.equals(((AbstractResults)obj).getName());
	}
	return super.equals(obj);
}

int getId() {
	return this.id;
}

/**
 * Returns the name of the results object.
 * 
 * @return The name of the results
 */
public String getName() {
	return this.name;
}

PerformanceResults getPerformance() {
	if (this.parent != null) {
		return this.parent.getPerformance();
	}
	return null;
}

String getPath() {
	String path = this.parent==null || this.parent.parent==null ? "" : this.parent.getPath() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
	return path+this.name;
}

/**
 * Return the children list of the current results.
 * 
 * @return An iterator on the children list
 */
public Iterator getResults() {
	return this.children.iterator();
}

AbstractResults getResults(String resultName) {
	int size = this.children.size();
	for (int i=0; i<size; i++) {
		AbstractResults searchedResults = (AbstractResults) this.children.get(i);
		if (searchedResults.getName().equals(resultName)) {
			return searchedResults;
		}
	}
	return null;
}

AbstractResults getResults(int searchedId) {
	int size = this.children.size();
	for (int i=0; i<size; i++) {
		AbstractResults searchedResults = (AbstractResults) this.children.get(i);
		if (searchedResults.id == searchedId) {
			return searchedResults;
		}
	}
	return null;
}

public int hashCode() {
	return this.name.hashCode();
}

void printTab() {
	if (this.parent != null) {
		if (this.print) System.out.print("\t"); //$NON-NLS-1$
		this.parent.printTab();
	}
}
void print(String text) {
	if (this.print) {
		if (this.newLine) printTab();
		System.out.print(text);
		this.newLine = false;
	}
}

void printGlobalTime(long start) {
	printGlobalTime(start, null);
}

void printGlobalTime(long start, String end) {
	StringBuffer buffer = new StringBuffer("=> time spent in '"); //$NON-NLS-1$
	buffer.append(this.name);
	buffer.append("' was "); //$NON-NLS-1$
	buffer.append((System.currentTimeMillis()-start)/1000.0);
	if (end == null) {
		buffer.append('s');
	} else {
		buffer.append("s. "); //$NON-NLS-1$
		buffer.append(end.trim());
	}
	println(buffer);
}

void println(String text) {
	if (this.print) {
		if (this.newLine) printTab();
		System.out.println(text);
		this.newLine = true;
	}
}

void println(StringBuffer buffer) {
	println(buffer.toString());
}

public int size() {
	return this.children == null ? 0 : this.children.size();
}

public String toString() {
	return getPath();
}

}
