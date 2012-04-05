/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class allows the repository provider to plug into the Fix Copyright action
 * in order to provide the last modified year for one or more files.
 */
public abstract class RepositoryProviderCopyrightAdapter {

	private final IResource[] resources;

	public RepositoryProviderCopyrightAdapter(IResource[] resources) {
		this.resources = resources;
	}

	/**
	 * Initialize the adapter. This call is provided to support the batch fetching
	 * of the last modifies year for all the files of interest.
	 * @param monitor a progress monitor
	 */
	public abstract void initialize(IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Return the last modified year for the given file or -1 if the
	 * last modified year could not be determined.
	 * @param file the file
	 * @param monitor a progress monitor
	 * @return the last modified year or -1
	 * @throws CoreException
	 */
	public abstract int getLastModifiedYear(IFile file, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return the resources that are involved in this operation
	 * @return the resources
	 */
	public IResource[] getResources() {
		return resources;
	}
}
