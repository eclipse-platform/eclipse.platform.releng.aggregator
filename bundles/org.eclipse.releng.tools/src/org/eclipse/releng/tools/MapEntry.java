/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

/**
 * This class provides access to information stored in RelEng map files
 */
public class MapEntry {
	
	private static final String HEAD = "HEAD";
	private static final String KEY_TAG = "tag"; //$NON-NLS-1$
	private static final String KEY_PATH = "path"; //$NON-NLS-1$
	private static final String KEY_CVSROOT = "cvsRoot"; //$NON-NLS-1$
	private static final String KEY_PASSWORD = "password"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private boolean valid = false;
	private String type = EMPTY_STRING;
	private String id = EMPTY_STRING;
	private OrderedMap arguments = new OrderedMap();
	private boolean legacy = false;
	private String version;

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
			"type@id,version=CVS,tag=myTag,cvsRoot=myCvsRoot,password=password,path=myPath",
		};
		
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i];
			MapEntry anEntry = new MapEntry(string);

			System.out.println("-----------------------------------------------");
			System.out.println("input: " + string);
			System.out.println("map string: " + anEntry.getMapString());
//			System.out.println(anEntry.getReferenceString());
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
		System.out.println("Tag: " + getTagName());
		if (version != null)
			System.out.println("Version: " + version);
		System.out.println("Connect: " + getRepo());
		System.out.println("Password: " + getPassword());
		System.out.println("CVS Module: " + getCVSModule());
	}
	
	public MapEntry(String entryLine) {
		init(entryLine);
	}

	/**
	 * Parse a map file entry line
	 * @param entryLine
	 */	
	private void init(String entryLine) {
		valid = false;
	
		// check for commented out entry
		if (entryLine.startsWith("#") || entryLine.startsWith("!"))
			return;

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
		// we have a version that we have to strip off
		int comma = id.indexOf(',');
		if (comma != -1) {
			version = id.substring(comma + 1);
			id = id.substring(0, comma);
		}

		String[] args = getArrayFromStringWithBlank(entryLine.substring(end + 1), ",");
		this.arguments = populate(args);
		String tag = (String) arguments.get(KEY_TAG);
		String repo = (String) arguments.get(KEY_CVSROOT);
		if (tag == null || tag.length() == 0 || repo == null || repo.length() == 0)
			return;
		valid = true;
	}

	/*
	 * Build a table from the given array. In the new format,the array contains
	 * key=value elements. Otherwise we fill in the key based on the old format.
	 */
	private OrderedMap populate(String[] entries) {
		OrderedMap result = new OrderedMap();
		for (int i=0; i<entries.length; i++) {
			String entry = entries[i];
			int index = entry.indexOf('=');
			if (index == -1) {
				// we only handle CVS entries
				if (i == 0 && "CVS".equalsIgnoreCase(entry)) 
					continue;
				// legacy story...
				return legacyPopulate(entries);
			}
			String key = entry.substring(0, index);
			String value = entry.substring(index + 1);
			result.put(key, value);
		}
		result.toString();
		return result;
	}
	
	private OrderedMap legacyPopulate(String[] entries) {
		legacy = true;
		OrderedMap result = new OrderedMap();
		// must have at least tag and connect string
		if (entries.length >= 2) {
			// Version
			result.put(KEY_TAG, entries[0]);
			// Repo Connect String
			result.put(KEY_CVSROOT, entries[1]);
			
			// Optional Password.
			if (entries.length >= 3)
				result.put(KEY_PASSWORD, entries[2]);
			
			// Optional CVS Module Name
			if (entries.length >= 4)
				result.put(KEY_PATH, entries[3]);
		}
		return result;
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified. The specificity of this method is that it returns an empty
	 * element when to same separators are following each others. For example
	 * the string a,,b returns the following array [a, ,b]
	 *  
	 */
	public static String[] getArrayFromStringWithBlank(String list, String separator) {
		if (list == null || list.trim().length() == 0)
			return new String[0];
		List result = new ArrayList();
		boolean previousWasSeparator = true;
		for (StringTokenizer tokens = new StringTokenizer(list, separator, true); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (token.equals(separator)) {
				if (previousWasSeparator)
					result.add(""); //$NON-NLS-1$
				previousWasSeparator = true;
			} else {
				result.add(token);
				previousWasSeparator = false;
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public String getTagName() {
		String value = (String) arguments.get(KEY_TAG);
		return value == null  || HEAD.equals(value) ? EMPTY_STRING : value;
	}
	
	public CVSTag getTag() {
		if (getTagName().equals(HEAD) || getTagName().equals("")) return CVSTag.DEFAULT; //$NON-NLS-1$
		return new CVSTag(getTagName(), CVSTag.VERSION);
	}
	
	public String getPassword() {
		String value = (String) arguments.get(KEY_PASSWORD);
		return value == null ? EMPTY_STRING : value;
	}

	public String getId() {
		return id;
	}

	private String internalGetCVSModule()  {
		String module = (String) arguments.get(KEY_PATH);
		return module == null ? id : module;
	}
	
	public String getCVSModule() {
		String value = (String) arguments.get(KEY_PATH);
		return value == null ? EMPTY_STRING : value;
	}

	public String getRepo() {
		String value = (String) arguments.get(KEY_CVSROOT);
		return value == null ? EMPTY_STRING : value;
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
		StringBuffer result = new StringBuffer();
		if (legacy) {
			result.append(getType());
			result.append('@');
			result.append(getId());
			if (version != null) {
				result.append(',');
				result.append(version);
			}
			result.append('=');
			result.append(getTagName());
			result.append(',');
			result.append(getRepo());
			result.append(',');
			result.append(getPassword());
			if (!getCVSModule().equals("") || !getPassword().equals(""))
				result.append(',');
			result.append(getCVSModule());
			return result.toString();
		}
		result.append(getType());
		result.append('@');
		result.append(getId());
		if (version != null) {
			result.append(',');
			result.append(version);
		}
		result.append('=');
		result.append("CVS");
		for (Iterator iter = arguments.keys().iterator(); iter.hasNext(); ) {
			String key = (String) iter.next();
			String value = (String) arguments.get(key);
			if (value != null && value.length() > 0)
				result.append(',' + key + '=' + value);
		}
		return result.toString();
	}

	/*
	 * Return the version specified for this entry. Can be null.
	 */
	public String getVersion() {
		return version;
	}
	
	public void setPassword(String password) {
		arguments.put(KEY_PASSWORD, password);
	}

	public void setId(String projectID) {
		this.id = projectID;
	}

	public void setCVSModule(String path) {
		arguments.put(KEY_PATH, path);
	}

	public void setRepo(String repo) {
		arguments.put(KEY_CVSROOT, repo);
	}

	public void setTagName(String tagName) {
		arguments.put(KEY_TAG, tagName);
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
