/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ui.UIProjectSetSerializationContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.actions.WorkspaceModifyOperation;


public class LoadMap extends CVSAction {

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		run(new WorkspaceModifyOperation(null) {
			public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					IResource[] resources = getSelectedResources();
					String[] referenceStrings = getReferenceStrings(resources);
					RepositoryProviderType type = RepositoryProviderType.getProviderType(CVSProviderPlugin.getTypeId());
					ProjectSetCapability c = type.getProjectSetCapability();
					c.addToWorkspace(referenceStrings, new UIProjectSetSerializationContext(getShell(), null), monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}

		}, true, PROGRESS_DIALOG);
	}

	/**
	 * Get the project set reference strings from the provider map files
	 * 
	 * @param mapFiles
	 * @return
	 * @throws CoreException
	 */
	protected String[] getReferenceStrings(IResource[] mapFiles) throws CoreException {
		List allStrings = new ArrayList();
		for (int i = 0; i < mapFiles.length; i++) {
			IResource resource = mapFiles[i];
			String[] referenceStrings = readReferenceStrings((IFile)resource);
			allStrings.addAll(Arrays.asList(referenceStrings));
		}
		return (String[]) allStrings.toArray(new String[allStrings.size()]);
	}
	
	/**
	 * Method readReferenceStrings.
	 * @param file
	 * @return String[]
	 */

	protected static String[] readReferenceStrings(IFile file) throws CoreException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
		try {
			try {
				String line = reader.readLine();
				List references = new ArrayList();
				while (line != null) {
					String referenceString = new MapEntry(line).getReferenceString();
					if (referenceString != null)  {
						references.add(referenceString);
					}
					line = reader.readLine();
				}
				return (String[]) references.toArray(new String[references.size()]);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0, "An I/O error occured", e));
		}
	}


	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		if (resources.length == 0) return false;
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getType() != IResource.FILE)
				return false;
			if (!MapFile.isMapFile((IFile)resource))
				return false;
		}
		return true;
	}

}
