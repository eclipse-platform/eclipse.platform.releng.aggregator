/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class MapFile {
	
	public static final String MAP_FILE_EXTENSION = "map";
	
	protected IFile file;
	protected MapEntry[] entries;
	
	public MapFile(IFile aFile) throws CoreException {
		file = aFile;
		loadEntries();
	}
	
	public IFile getFile(){
		return file;
	}
	
	protected void loadEntries() throws CoreException {
		InputStream inputStream = null;
		List list = new ArrayList();		

		try {
			inputStream = file.getContents();
			BufferedReader aReader = new BufferedReader(new InputStreamReader(
					inputStream));
			String aLine = aReader.readLine();
			while (aLine != null) {
				if (isMapLine(aLine)) {
					list.add(new MapEntry(aLine));
				}
				aLine = aReader.readLine();
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0, "An I/O Error occurred process map file " + file.getFullPath().toString(), e));
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Ignore close exceptions so we don't mask another exception
				}
			}
		}

		this.entries = (MapEntry[]) list.toArray(new MapEntry[list.size()]);
	}
	
	private boolean isMapLine(String line) {
		if (line.trim().length() == 0) return false;
		if (line.startsWith("!")) return false;
		return true;
	}
	
	public boolean contains(IProject project){
		for(int j = 0;j < entries.length; j++){
			if (entries[j].isMappedTo(project)){
				return true;
			}
		}
		return false;
	}

	public MapEntry getMapEntry(IProject project) {
		for(int j = 0;j < entries.length; j++){
			if (entries[j].isMappedTo(project)){
				return entries[j];
			}
		}
		return null;
	}

	public IProject[] getAccessibleProjects() {
		List list = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if(entries == null || entries.length ==0) return null;
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.isAccessible()) {
				for (int j = 0; j < entries.length; j++){
					if (entries[j].isMappedTo(project)) {
						list.add(project);
					}
				}
			}
		}
		return (IProject[])list.toArray(new IProject[list.size()]);
	}

	public String getName() {
		return file.getName();
	}

}
