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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * This class compares the locally selected projects againsts the versions
 * found in the releng map files. The releng map files are searched for in the 
 * org.eclipse.releng project in the folder named maps
 */
public class CompareLocalToMap extends WorkspaceAction {

	/*
	 * Get the tag from the map files in the org.eclipse.releng project
	 * 
	 * @param resource
	 * @return
	 * @throws CVSException
	 */
	protected CVSTag getTag(IResource resource) {
		MapEntry entry = getMapProject().getMapEntry(resource.getProject());
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
		return (getMapProject() != null && getMapProject().mapsAreLoaded());
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {		
		IResource[] resources = getSelectedResources();
		if (resources.length == 0) return;
		CVSTag[] tags = new CVSTag[resources.length];		
		for (int i = 0; i < resources.length; i++) {
			tags[i] = getTag(resources[i]);
		}

		// Create the synchronize view participant
		CVSCompareSubscriber s = new CVSCompareSubscriber(resources, tags, "RelEng Map"); //$NON-NLS-1$
		CompareParticipant participant = new CompareParticipant(s);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
		participant.refresh(resources, "Refreshing", "Refreshing", getTargetPart().getSite());
	}
	private MapProject getMapProject(){
		return MapProject.getDefaultMapProject();
	}
}
