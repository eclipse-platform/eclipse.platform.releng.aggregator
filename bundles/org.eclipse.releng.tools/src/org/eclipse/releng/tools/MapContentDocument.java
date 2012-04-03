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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;


public class MapContentDocument implements ITypedElement, IStreamContentAccessor{
	private MapFile mapFile;
	private String oldContents = ""; //$NON-NLS-1$
	private String newContents = ""; //$NON-NLS-1$

	public MapContentDocument(MapFile aMapFile) {
		mapFile = aMapFile;	
		initialize();
	}

	/**
	 * Update the tag associated with the given project in the new contents.
	 */
	public void updateTag(IProject project, String tag) throws CVSException {
		InputStream inputStream = new BufferedInputStream(
				new ByteArrayInputStream(newContents.getBytes()));
		boolean match = false;
		StringBuffer buffer = new StringBuffer();
		try {
			BufferedReader aReader = new BufferedReader(new InputStreamReader(
					inputStream));
			String aLine = aReader.readLine();
			while (aLine != null) {
				if (aLine.trim().length() != 0 && !aLine.startsWith("!") && !aLine.startsWith("#")) {
					// Found a possible match
					MapEntry entry = new MapEntry(aLine);
					if (!entry.isValid()) {
						throw new CVSException("Malformed map file line: "
								+ aLine);
					}
					if (entry.isMappedTo(project)) {
						// Now for sure we have a match. Replace the line.
						entry.setTagName(tag);
						aLine = entry.getMapString();
						match = true;
					}
				}
				buffer.append(aLine);
				aLine = aReader.readLine();
				if (aLine != null) {
					buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (match) {
			newContents = buffer.toString();
		}
	}
	public boolean isChanged() {
		return !(oldContents.equals(newContents));
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		return mapFile.getFile().getName();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.compare.ITypedElement#getImage()
	 */
	public Image getImage() {
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		return mapFile.getFile().getFileExtension();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.compare.IStreamContentAccessor#getContents()
	 */
	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(getNewContent().getBytes());
	}
	public MapFile getMapFile() {
		return mapFile;
	}
	
	private String getNewContent() {
		return newContents;
	}
	private void initialize() {
		InputStream inputStream;
		StringBuffer buffer = new StringBuffer();
		try {
			inputStream = mapFile.getFile().getContents();
			BufferedReader aReader = new BufferedReader(new InputStreamReader(
					inputStream));
			String aLine = aReader.readLine();
			while (aLine != null) {
				buffer.append(aLine);
				aLine = aReader.readLine();
				if (aLine != null) {
					buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
				}
			}
			oldContents = buffer.toString();
			newContents = new String(oldContents);
		} catch (CoreException e) {
			CVSUIPlugin.openError(null, null, null, e);
		} catch (IOException e) {
			CVSUIPlugin.openError(null, null, null, e);
		}
	}
}
