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
public class VersionClassDirFilter implements FileFilter {
	private String fileExtension;

	/**
	 * Constructor
	 * 
	 * @param fileExtension the file extension to filter on
	 */
	public VersionClassDirFilter(String fileExtension) {
		super();
		this.fileExtension = fileExtension;
	}

	/**
	 * Returns <code>true</code> if the given file is a directory or its extension matches the file extension for this
	 * filter and <code>false</code> otherwise.
	 * 
	 * @param file the abstract pathname to be tested
	 * @return <code>true</code> if the file is a directory , or its extension matches this filter's file extension 
	 * 		   <code>false</code> otherwise
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}

		IPath path = new Path(file.getAbsolutePath());
		String extension = path.getFileExtension();
		if (extension == null) {
			if (fileExtension == null || fileExtension.trim().equals("")) //$NON-NLS-1$
				return true;
			else
				return false;
		} else {
			if (path.getFileExtension().equals(fileExtension))
				return true;
			else
				return false;
		}
	}
}
