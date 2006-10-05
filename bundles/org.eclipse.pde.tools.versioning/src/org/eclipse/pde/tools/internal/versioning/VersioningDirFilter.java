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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * This class implements the FileFilter interface. Instance of this class may be
 * passed to the listFiles(FileFilter) method of the File class
 * 
 */
public class VersioningDirFilter implements FileFilter {
	private String dirName;

	/**
	 * Constructor for the class.
	 */
	public VersioningDirFilter(String dirName) {
		super();
		this.dirName = dirName;
	}

	/**
	 * Constructor for the class.
	 */
	public VersioningDirFilter() {
		super();
		this.dirName = null;
	}

	/**
	 * Returns <code>true</code> if the given file is a directory and <code>false</code> otherwise.
	 * 
	 * @param file the abstract pathname to be tested
	 * @return <code>true</code> if the file is a directory and <code>false</code> otherwise
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) {
			if (dirName != null) {
				IPath path = new Path(file.getAbsolutePath());
				if (path.lastSegment().equals(dirName))
					return true;
				return false;
			}
			return true;
		}
		return false;
	}
}