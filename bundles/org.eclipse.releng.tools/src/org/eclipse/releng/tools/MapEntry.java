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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

/**
 * This class provides access to information stored in RelEng map files
 */
public class MapEntry {
	
	private static final String FRAGMENT = "fragment";
	private static final String FEATURE = "feature";
	private static final String PLUGIN = "plugin";
	private boolean valid = false;
	private String type = "";
	private String id = "";
	private String tagName = "";
	private String repo = "";
	private String password = "";
	private String cvsModule = "";

	public static void main (String[] args)  {
		// For testing only
		
		String[] strings = {
			"",
			" ",
			"type",
			"type@",
			"type@id",
			"type@id=",
			"type@id=tag,",
			"type@id=tag, connectString",
			"type@id=tag, connectString,",
			"type@id=tag, connectString,password",
			"type@id=tag, connectString,password,",
			"type@id=tag, connectString,password,moduleName",
			"type@id=tag, connectString,,moduleName",
			"!***************  FEATURE CONTRIBUTION  ******************************************************",
			"@",
			"=",
			",,,",
			"@=,,,,",
		};
		
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i];
			MapEntry anEntry = new MapEntry(string);

			System.out.println("");
			System.out.println(string);
			System.out.println(anEntry.getMapString());
//			System.out.println(anEntry.getReferenceString());
			System.out.println("");
			anEntry.display();
		}
		
	}
	
	/**
	 * 
	 */
	private void display() {
		// For testing only
		System.out.println("Is Valid: " + isValid());
		System.out.println("Type: " + getType());
		System.out.println("Project Name: " + getId());
		System.out.println("Version: " + getTagName());
		System.out.println("Connect: " + getRepo());
		System.out.println("Password: " + getPassword());
		System.out.println("CVS Module: " + getCVSModule());
	}
	
	public MapEntry(String entryLine) {
		init(entryLine);
	}

	/**
	 * Create a map entry for the given project and tag
	 * @param project
	 * @param tag
	 */
	public MapEntry(IProject project, CVSTag tag) throws CVSException {
		type = internalGetType(project);
		id = project.getName();
		tagName = tag.getName();
		FolderSyncInfo info = CVSWorkspaceRoot.getCVSFolderFor(project).getFolderSyncInfo();
		repo = info.getRoot();
		password = "";
		cvsModule = info.getRepository();
		valid = true;
	}
	
	private String internalGetType(IProject project) {
		if (project.getFile("plugin.xml").exists()) return PLUGIN;
		if (project.getFile("feature.xml").exists()) return FEATURE;
		if (project.getFile("fragment.xml").exists()) return FRAGMENT;
		return PLUGIN;
	}

	/**
	 * Parse a map file entry line
	 * @param entryLine
	 */	
	private void init(String entryLine) {

		valid = false;
	
		// Type	
		int start = 0;
		int end = entryLine.indexOf('@');
		if (end == -1)  return;
		type = entryLine.substring(start, end).trim();
		
		// Project Name
		start = end + 1;
		end = entryLine.indexOf('=', start);
		if (end == -1) return;
		id = entryLine.substring(start, end).trim();
		
		// Version
		start = end + 1;
		end = entryLine.indexOf(',', start);
		if (end == -1) return;
		tagName = entryLine.substring(start, end).trim();
	
		// Repo Connect String
		start = end + 1;
		if (start == entryLine.length()) return; // No connect string - invalid
		end = entryLine.indexOf(',', start);
		if (end == -1)  {
			// Tailing , not required if connect string is last entry on line.
			repo = entryLine.substring(start).trim();
			valid = true;
			return;
		} else  {
			repo = entryLine.substring(start, end).trim();
		}
	
		// All required fields met.
		valid = true;
		
		// Optional Password.
		start = end + 1;
		if (start == entryLine.length()) return;  // End of line reached.  No password
		end = entryLine.indexOf(',', start);
		if (end == -1)  {
			// No trailing , but password present.
			password = entryLine.substring(start).trim();
			return;
		}
		// , after password
		password = entryLine.substring(start, end).trim();
		
		// Optional CVS Module Name
		start = end + 1;
		if (start == entryLine.length())  return;  // No module name
		cvsModule = entryLine.substring(start).trim();
	}

	public String getTagName() {
		return tagName;
	}
	
	public CVSTag getTag() {
		if (getTagName().equals("HEAD")) return CVSTag.DEFAULT;
		return new CVSTag(getTagName(), CVSTag.VERSION);
	}
	
	public String getPassword() {
		return password;
	}

	public String getId() {
		return id;
	}

	private String internalGetCVSModule()  {
		if (cvsModule.equals(""))  {
			return id;
		} else  {
			return cvsModule;
		}
	}
	
	public String getCVSModule() {
		return cvsModule;
	}

	public String getRepo() {
		return repo;
	}

	public String getType() {
		return type;
	}

	public boolean isValid() {
		return valid;
	}
	
	public String getReferenceString()  {
		if (!isValid()) return null;
		// This is the format used by the CVS IProjectSerializer
		String projectName = new Path(internalGetCVSModule()).lastSegment();
		return "1.0," + getRepo() + "," + internalGetCVSModule() + "," + projectName + "," + getTagName();
	}

	public String getMapString() {
		String result = getType() + "@" + getId() + "=" + getTagName() + "," + getRepo() + "," + getPassword();
		if (!getCVSModule().equals("") || !getPassword().equals(""))  {
			result = result + ",";
		}
		return result + getCVSModule();		
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public void setId(String projectID) {
		this.id = projectID;
	}

	public void setCVSModule(String path) {
		this.cvsModule = path;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
	
	public String toString() {
		return "Entry: " + getMapString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof MapEntry) {
			return ((MapEntry)obj).getMapString().equals(getMapString());
		}
		return super.equals(obj);
	}
	
	/**
	 * Return true if this map entry is mapped to the given CVS module
	 * @param moduleName
	 * @return
	 */
	public boolean isMappedTo(String moduleName) {
		IPath entryPath = new Path(internalGetCVSModule());
		IPath modulePath = new Path(moduleName);
		if (entryPath.segmentCount() != modulePath.segmentCount()) return false;
		for (int i = 0; i < entryPath.segmentCount(); i++) {
			if (!entryPath.segment(i).equals(modulePath.segment(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return <code>true</code> if the entry is mapped to the given project
	 * and <code>false</code> otherwise.
	 */
	public boolean isMappedTo(IProject project) {
		String moduleName;
		try {
			moduleName = getCVSModule(project);
			if (moduleName == null) return false;
			return isMappedTo(moduleName);
		} catch (CVSException e) {
			RelEngPlugin.getDefault().getLog().log(e.getStatus());
			return false;
		}
	}
	
	/**
	 * Get the remote CVS module for the project or <code>null</code>
	 * if the project is not a CVS project.
	 */
	private String getCVSModule(IProject project) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
		FolderSyncInfo info = folder.getFolderSyncInfo();
		if (info == null) {
			return null;
		}
		return info.getRepository();
	}

}
