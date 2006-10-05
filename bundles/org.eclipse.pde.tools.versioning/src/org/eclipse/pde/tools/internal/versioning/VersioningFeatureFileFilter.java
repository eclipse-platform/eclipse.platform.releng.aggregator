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
import java.io.FileFilter;

/**
 * This class implements the FileFilter interface. Instance of this class may be
 * passed to the listFiles(FileFilter) method of the File class
 * 
 */
public class VersioningFeatureFileFilter implements FileFilter {
	private String fileName;

	/**
	 * Constructor for the class.
	 * 
	 * @param fileName the filename to filter on
	 */
	public VersioningFeatureFileFilter(String fileName) {
		super();
		this.fileName = fileName;
	}

	/**
	 * Returns <code>true</code> if the given file matches the name for this
	 * filter and <code>false</code> otherwise.
	 * 
	 * @param file the abstract pathname to be tested
	 * @return <code>true</code> if the file matches this filter's name and<code>false</code> otherwise
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return false;
		}

		String name = file.getName();
		if (name.equals(fileName)) {
			return true;
		} else {
			return false;
		}
	}
}
