/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.actions.ReplaceWithRemoteAction;

/**
 * This action replaces one or more projects in the local workspace
 * with the versions released to the RelEng map file.
 */
public class ReplaceLocalFromMap extends ReplaceWithRemoteAction {

	private String actionName;

	/*
	 * Get the tag from the map files in the org.eclipse.releng project
	 * 
	 * @param resource
	 * @return
	 * @throws CVSException
	 */
	protected CVSTag getTag(IResource resource) {
		MapEntry entry = MapProject.getDefaultMapProject().getMapEntry((IProject)resource);
		if (entry == null) return CVSTag.DEFAULT;
		return entry.getTag();
	}
	
	/**
	 * Returns true if the super would enable the option *and*
	 * only projects are selected.  There is no concept of "releasing"
	 * anything but a project.
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		
		boolean result = super.isEnabled();
		if (!result) {
			return false;
		}
		
		IResource[] resources = super.getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getType() != IResource.PROJECT) {
				return false;
			}
		}
		
		return (getMapProject() != null && getMapProject().mapsAreLoaded());
	}

	/**
	 * Undo action rename of superclass
	 */ 
	protected void setActionEnablement(IAction action) {
		actionName = action.getText();
		super.setActionEnablement(action);
	}
	
	protected String calculateActionTagValue() {
		return actionName;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.ReplaceWithRemoteAction#performReplace(org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void performReplace(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask("Replacing projects", 100 * resources.length);
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
			if (provider == null) continue;
			provider.update(
				new IResource[] {resource}, 
				new LocalOption[] {Update.IGNORE_LOCAL_CHANGES},
				getTag(resource),
				false, 
				new SubProgressMonitor(monitor, 100));
		}
		monitor.done();
	}
	private MapProject getMapProject(){
		return MapProject.getDefaultMapProject();
	}
}
