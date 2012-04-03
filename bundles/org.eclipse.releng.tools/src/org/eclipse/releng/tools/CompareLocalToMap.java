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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.releng.tools.preferences.MapProjectPreferencePage;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * This class compares the locally selected projects againsts the versions
 * found in the releng map files. The releng map files are searched for in the 
 * org.eclipse.releng project in the folder named maps
 */
public class CompareLocalToMap extends WorkspaceAction {
	

	/**
	 * Returns true if the super would enable the option *and*
	 * only projects are selected.  There is no concept of "releasing"
	 * anything but a project.
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		if (!super.isEnabled())
			return false;
		
		return hasProjectFromMapFile();
	}

	static boolean hasProjectFromMapFile() {
		final String FOUND= new String();
		try {
			final MapFile[] mapFiles= MapFile.findAllMapFiles(ResourcesPlugin.getWorkspace().getRoot());

			IResourceProxyVisitor visitor= new IResourceProxyVisitor() {
				public boolean visit(IResourceProxy resourceProxy) throws CoreException {
					if (resourceProxy.getType() == IResource.ROOT)
						return true;

					if (resourceProxy.getType() != IResource.PROJECT || !resourceProxy.isAccessible())
						return false;

					IProject project= (IProject)resourceProxy.requestResource();
					if (!RelEngPlugin.isShared(project))
						return false;

					if (contains(mapFiles, project))
						throw new OperationCanceledException(FOUND);

					return false;
				}
			};
			ResourcesPlugin.getWorkspace().getRoot().accept(visitor, IResource.NONE);
			return false;
		} catch (OperationCanceledException ex) {
			return ex.getMessage() == FOUND;
		} catch (CoreException e) {
			return false;
		}
	}

	private static boolean contains(MapFile[] mapFiles, IProject project) {
		for (int i= 0; i < mapFiles.length; i++)
			if (mapFiles[i].contains(project))
				return true;
		return false;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		//Start the MapProjectSelectionWizard
		IPreferenceStore preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();
		if (!(preferenceStore.getBoolean(MapProjectPreferencePage.USE_DEFAULT_MAP_PROJECT)) || 
				!(preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH).length() > 0)) {
			MapProjectSelectionWizard wizard = new MapProjectSelectionWizard(Messages.getString("CompareLocalToMap.0")); //$NON-NLS-1$
			wizard.execute(getShell());
			
			//check if the "cancel" button was used in the wizard dialog.  Return if so.
			if (wizard.operationCancelled())
				return;
		}
			
		IResource[] resources = getSelectedResources();
		if (resources.length == 0) return;
		
		//check for projects for which a map entry cannot be found
		CVSTagHelper tagHelper = new CVSTagHelper();
		CVSTag[] tags = tagHelper.findMissingMapEntries(resources);
		
		//warn the user if any projects were found to not have a corresponding map entry
		if (tags == null || tagHelper.warnAboutUnfoundMapEntries(Messages.getString("CompareLocalToMap.1"))) //$NON-NLS-1$
			return;
		
		// Create the synchronize view participant
		CVSCompareSubscriber s = new CVSCompareSubscriber(resources, tags, "Map Project"); //$NON-NLS-1$
		try {
			s.primeRemoteTree();
		} catch (CVSException e) {
			// Log and ignore
			RelEngPlugin.log(e);
		}
		CompareParticipant participant = new CompareParticipant(s);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
		participant.refresh(resources, "Refreshing", "Refreshing", getTargetPart().getSite()); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
