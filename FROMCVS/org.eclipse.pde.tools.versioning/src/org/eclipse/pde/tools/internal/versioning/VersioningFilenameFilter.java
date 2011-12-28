/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.internal.versioning;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * This class implements the FilenameFilter interface. Instance of this class
 * may be passed to the listFiles(FilenameFilter) method of the File class 
 * 
 */
public class VersioningFilenameFilter implements FilenameFilter {
	private Pattern pattern;

	/**
	 * Constructor for this class. Initialize it with the given regular expression.
	 * 
	 * @param featureRegex the regular expression to use for this filter
	 */
	public VersioningFilenameFilter(String featureRegex) {
		pattern = Pattern.compile(featureRegex);
	}

	/**
	 * Returns <code>true</code> if the given filename matches the regular
	 * expression for this filter, and <code>false</code> otherwise.
	 * 
	 * @param dir the directory in which the file was found
	 * @param name the name of the file to be tested
	 * @return <code>true</code> if the name should be included in the file list and<code>false</code> otherwise
	 */
	public boolean accept(File dir, String name) {
		// Creates a matcher and matches the given input against the pattern.
		return pattern.matcher(new File(name).getName()).matches();
	}
}
