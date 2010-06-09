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
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;
import org.eclipse.team.internal.core.InfiniteSubProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * This action replaces one or more projects in the local workspace
 * with the versions released to the RelEng map file.
 */
public class ReplaceLocalFromMap extends WorkspaceAction {

	private CVSTag[] tags;
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForNonExistantResources()
	 */
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		if (super.isEnabledForCVSResource(cvsResource)) {
			// Don't enable if there are sticky file revisions in the lineup
			if (!cvsResource.isFolder()) {
				ResourceSyncInfo info = cvsResource.getSyncInfo();
				if (info != null && info.getTag() != null) {
					String revision = info.getRevision();
					String tag = info.getTag().getName();
						if (revision.equals(tag)) return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns true if the super would enable the option *and*
	 * only projects are selected.  There is no concept of "releasing"
	 * anything but a project.
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	public boolean isEnabled() {
		if (!(super.isEnabled()))
			return false;

		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.getType() != IResource.PROJECT) {
				return false;
			}
		}

		return CompareLocalToMap.hasProjectFromMapFile();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.ReplaceWithRemoteAction#performReplace(org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void performReplace(IResource[] resources, IProgressMonitor monitor) throws TeamException, InvocationTargetException, InterruptedException {
		monitor.beginTask(null, 100 * resources.length);
		try {
			if (tags.length != resources.length)
				return;
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				new ReplaceOperation(getTargetPart(), new IResource[] { resource }, tags[i], true).run(new SubProgressMonitor(monitor, 100));
			}
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		//Start the MapProjectSelectionWizard
		IPreferenceStore preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();
		if (!(preferenceStore.getBoolean(MapProjectPreferencePage.USE_DEFAULT_MAP_PROJECT)) || 
				!(preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH).length() > 0)) {
			MapProjectSelectionWizard wizard = new MapProjectSelectionWizard(Messages.getString("ReplaceLocalFromMap.0")); //$NON-NLS-1$
			wizard.execute(getShell());
			
			//check if the "cancel" button was used in the wizard dialog.  Return if so.
			if (wizard.operationCancelled())
				return;
		}
		
		final IResource[][] resources = new IResource[][] {null};
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
				try {
					monitor.beginTask(null, 100);					
					resources[0] = checkOverwriteOfDirtyResources(getSelectedResources(), new InfiniteSubProgressMonitor(monitor, 100));
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
		
		if (resources[0] == null || resources[0].length == 0) return;
		
		//check for projects for which a map entry cannot be found
		CVSTagHelper tagHelper = new CVSTagHelper();
		tags = tagHelper.findMissingMapEntries(resources[0]);
		
		//warn the user if any projects were found to not have a corresponding map entry
		boolean operationCancelled = tagHelper.warnAboutUnfoundMapEntries(Messages.getString("ReplaceLocalFromMap.1")); //$NON-NLS-1$
		if (operationCancelled || tags == null)
			return;
		
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					performReplace(resources[0], monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		}, true, PROGRESS_DIALOG);
		
	}
}
