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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.actions.TagInRepositoryAction;

/**
 * Tags the versions in a map file with another tag
 */
public class TagMap extends TagInRepositoryAction {
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getSelectedCVSResources()
	 */
	protected ICVSRemoteResource[] getSelectedRemoteResources() {
		IResource[] resources = getSelectedResources();
		List identifiers = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			try {
				String[] strings = LoadMap.readReferenceStrings((IFile)resource);
				identifiers.addAll(Arrays.asList(strings));
			} catch (CoreException e) {
				RelEngPlugin.log(e);
			}
		}
		return getCVSResourcesFor((String[]) identifiers.toArray(new String[identifiers.size()]));
	}

	private ICVSRemoteResource[] getCVSResourcesFor(String[] referenceStrings) {
		Map previouslySelectedRepositories = new HashMap();
		int size = referenceStrings.length;
		List result = new ArrayList(size);
		for (int i = 0; i < size; i++) {
			StringTokenizer tokenizer = new StringTokenizer(referenceStrings[i], ","); //$NON-NLS-1$
			String version = tokenizer.nextToken();
			if (!version.equals("1.0")) { //$NON-NLS-1$
				// Bail out, this is a newer version
				return null;
			}
			try {
				String repo = tokenizer.nextToken();
				ICVSRepositoryLocation storedlocation = getLocationFromString(repo);
				ICVSRepositoryLocation location = (ICVSRepositoryLocation)previouslySelectedRepositories.get(storedlocation);
				if (location == null) {
					location = getWritableRepositoryLocation(storedlocation);
					previouslySelectedRepositories.put(storedlocation, location);
					if (location == null) return new ICVSRemoteResource[0];
				}
				String module = tokenizer.nextToken();
				String projectName = tokenizer.nextToken();
				CVSTag tag = CVSTag.DEFAULT;
				if (tokenizer.hasMoreTokens()) {
					String tagName = tokenizer.nextToken();
					tag = new CVSTag(tagName, CVSTag.VERSION);
				}
				result.add(location.getRemoteFolder(module, tag));
			} catch (CVSException e) {
			    RelEngPlugin.log(e);
			}
		}
		return (ICVSRemoteResource[]) result.toArray(new ICVSRemoteResource[result.size()]);
	}
	
	private ICVSRepositoryLocation getLocationFromString(String repo) throws CVSException {
		// create the new location
		ICVSRepositoryLocation newLocation = CVSRepositoryLocation.fromString(repo);
		if (newLocation.getUsername() == null || newLocation.getUsername().length() == 0) {
			// look for an existing location that matched
			ICVSRepositoryLocation[] locations = CVSProviderPlugin.getPlugin().getKnownRepositories();
			for (int i = 0; i < locations.length; i++) {
				ICVSRepositoryLocation location = locations[i];
				if (location.getMethod() == newLocation.getMethod()
					&& location.getHost().equals(newLocation.getHost())
					&& location.getPort() == newLocation.getPort()
					&& location.getRootDirectory().equals(newLocation.getRootDirectory()))
						return location;
			}
		}
		return newLocation;
	}
	
	private ICVSRepositoryLocation getWritableRepositoryLocation(ICVSRepositoryLocation storedLocation) {
		// Find out which repo locations are appropriate
		ICVSRepositoryLocation[] locations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
		List compatibleLocations = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			ICVSRepositoryLocation location = locations[i];
			// Only locations with the same host and root are eligible
			if (!location.getHost().equals(storedLocation.getHost())) continue;
			if (!location.getRootDirectory().equals(storedLocation.getRootDirectory())) continue;
			compatibleLocations.add(location);
		}
		RepositorySelectionDialog dialog = new RepositorySelectionDialog(getShell());
		dialog.setLocations((ICVSRepositoryLocation[])compatibleLocations.toArray(new ICVSRepositoryLocation[compatibleLocations.size()]));
		dialog.open();
		ICVSRepositoryLocation location = dialog.getLocation();
		return location;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		IResource[] resources = getSelectedResources();
		return (resources.length >= 0  && MapProject.getDefaultMapProject().mapsAreLoaded());
	}

}
