/*******************************************************************************
 *  Copyright (c) 2013, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import java.util.HashSet;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;


/**
 * Marker resolution for when version in pom.xml does not match the plug-in version.
 * Replaces the version string to one based on the version in the manifest.  The corrected
 * version must have been stored on the marker at creation time.
 */
public class PomVersionMarkerResolution extends WorkbenchMarkerResolution {

	private IMarker marker;
	private String correctedVersion;

	/**
	 * New marker resolution that will offer to replace the current POM version with corrected version
	 * @param correctedVersion new version to insert
	 * @deprecated Use PomVersionMarkerResolution(IMarker marker,String correctedVersion) instead
	 */
	@Deprecated
	public PomVersionMarkerResolution(String correctedVersion) {
		this.correctedVersion = correctedVersion;
	}

	public PomVersionMarkerResolution(IMarker marker,String correctedVersion) {
		this.marker = marker;
		this.correctedVersion = correctedVersion;
	}

	@Override
	public String getLabel() {
		return NLS.bind(Messages.PomVersionMarkerResolution_label, correctedVersion);
	}

	@Override
	public void run(IMarker marker) {
		try {
			correctedVersion = (String) marker.getAttribute(IPomVersionConstants.POM_CORRECT_VERSION);
		} catch (CoreException e1) {
			RelEngPlugin.log(e1);
		}
		if (correctedVersion == null || correctedVersion.trim().length() == 0) {
			return;
		}
		int charstart = marker.getAttribute(IMarker.CHAR_START, -1);
		int charend = marker.getAttribute(IMarker.CHAR_END, -1);
		if(charstart < 0 || charend < 0) {
			return;
		}
		IResource resource = marker.getResource();
		if (resource.exists() && resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			if (!file.isReadOnly()) {
				NullProgressMonitor monitor = new NullProgressMonitor();
				ITextFileBufferManager fbm = FileBuffers.getTextFileBufferManager();
				IPath location = file.getFullPath();
				ITextFileBuffer buff = null;
				try {
					fbm.connect(location, LocationKind.IFILE, monitor);
					buff = fbm.getTextFileBuffer(location, LocationKind.IFILE);
					if(buff != null) {
						IDocument doc = buff.getDocument();
						try {
							if(charstart > -1 && charend > -1) {
								doc.replace(charstart, charend-charstart, correctedVersion);
								buff.commit(monitor, true);
							}
						} catch(BadLocationException ble) {
							RelEngPlugin.log(ble);
						}
					}
				} catch (CoreException e) {
					RelEngPlugin.log(e);
				} finally {
					try {
						if (buff != null)
							fbm.disconnect(location, LocationKind.IFILE, monitor);
					} catch (CoreException e) {
						RelEngPlugin.log(e);
					}
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public Image getImage() {
		return null ;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (IMarker iMarker : markers) {
			if (iMarker.equals(marker))
				continue;
			try {
				if (iMarker.getType().equals(IPomVersionConstants.PROBLEM_MARKER_TYPE))
					mset.add(iMarker);
			} catch (CoreException e) {

			}
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}
}
