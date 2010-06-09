/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.ArrayList;

import org.eclipse.releng.tools.preferences.MapProjectPreferencePage;
import org.eclipse.team.internal.ccvs.core.CVSTag;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This is a helper class used for obtaining CVSTags and checking for projects that have no corresponding
 * map entry.
 */
public class CVSTagHelper {

	private ArrayList defaultTags;

	//Returns an array of CVSTags for which no map entry could be found
	public CVSTag[] findMissingMapEntries(IResource[] resources) {
		defaultTags = new ArrayList();
		CVSTag[] tags = new CVSTag[resources.length];
		for (int i = 0; i < resources.length; i++) {
			tags[i] = getTag(resources[i]);
			if (CVSTag.DEFAULT == tags[i])
				defaultTags.add(resources[i]);
		}
		return tags;
	}

	//returns true if the user chooses to abort the current operation.
	public boolean warnAboutUnfoundMapEntries(String errorMessage) {
		if (defaultTags == null || defaultTags.size() <= 0)
			return false;
		String message = (errorMessage + "\n"); //$NON-NLS-1$
		for (int i = 0; i < defaultTags.size(); i++) {
			IResource aNotFoundResource = (IResource) defaultTags.get(i);
			message = message.concat("\n" + aNotFoundResource.getName()); //$NON-NLS-1$
		}
		message = message.concat("\n\n" + Messages.getString("CVSTagHelper.1")); //$NON-NLS-1$//$NON-NLS-2$
		return !MessageDialog.openConfirm(null, Messages.getString("CVSTagHelper.2"), message); //$NON-NLS-1$
	}

	/*
	 * Get the tag from the map files
	 * 
	 * @param resource
	 * @return
	 * @throws CVSException
	 */
	private CVSTag getTag(IResource resource) {
		MapProject selectedMapProject= null;
		try {
			selectedMapProject= getSelectedMapProject();
			if (selectedMapProject == null)
				return CVSTag.DEFAULT;
			MapEntry entry = selectedMapProject.getMapEntry(resource.getProject());
			if (entry == null)
				return CVSTag.DEFAULT;
			return entry.getTag();
		} finally {
			if (selectedMapProject != null)
				selectedMapProject.dispose();
		}
	}

	//returns the MapProject that was selected by the user in the MapProjectSelectionWizard
	private MapProject getSelectedMapProject() {
		IPreferenceStore preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();
		String path = preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH);
		if (!(path.length() > 0))
			return null;
		try {
			return new MapProject(ResourcesPlugin.getWorkspace().getRoot().getProject(path));
		} catch (CoreException e) {
			return null;
		}
	}

}
