/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 *  This class overrides the basic tag operation in order to update and possibly commit
 * the map file used by the Eclipse RelEng builder.
 */
public class TagAndReleaseOperation extends TagOperation {
	
	private IResource[] selectedProjects;
	private CVSTag tag;
	private String comment;
	private MapProject mapProject;
	private boolean mapFileUpdated;
	
	public TagAndReleaseOperation(IWorkbenchPart part, MapProject mapProject, IResource[] resources, CVSTag t, String c) {
 		super(part, asResourceMappers(resources));
		selectedProjects = new IResource[resources.length];
		System.arraycopy(resources,0,selectedProjects,0,resources.length);
		this.tag = t;
		setTag(tag);
		this.comment = c;
		this.mapProject = mapProject;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.TagOperation#tag(org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus tag(
		CVSTeamProvider provider,
		IResource[] resources,
        boolean recurse,
		IProgressMonitor progress)
		throws CVSException {
		
		// Tag the resource
		progress.beginTask("Releasing project " + provider.getProject().getName(), 100);
		IStatus status = super.tag(provider, resources, recurse, new SubProgressMonitor(progress, 95));
		if (status.getSeverity() == IStatus.ERROR) return status;
		progress.done();
		return status;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		monitor.beginTask("Tagging with " + getTag().getName(), 100);
		super.execute(new SubProgressMonitor(monitor, 95));
		
		monitor.subTask("Updating and committing map files");
        // Always update the map file even if tagging failed
		updateMapFile();
        // Only commit if there are no errors
		if (!errorsOccurred()) {
			try {
				mapProject.commitMapProject(comment,monitor);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
			mapFileUpdated = true;
		}
		monitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getSchedulingRule(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected ISchedulingRule getSchedulingRule(CVSTeamProvider provider) {
		// We need a rule on both the provider and the releng map project
		ISchedulingRule rule = super.getSchedulingRule(provider);
		return new MultiRule(new ISchedulingRule[] {rule, mapProject.getProject()});
	}

	/**
	 * Update the tag for the given project in the appropriate map file.
	 */
	private void updateTagsInMapFile(IProject project, String t) throws CVSException {
		try {
			mapProject.updateFile(project, t);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private void updateMapFile() throws CVSException{
		for (int i = 0; i < selectedProjects.length; i++) {
			if(selectedProjects[i] instanceof IProject){
				updateTagsInMapFile((IProject)selectedProjects[i], tag.getName());
			}
		}
	}

	public boolean isMapFileUpdated() {
		return mapFileUpdated;
	}
	
	protected IStatus[] getErrors() {
		return super.getErrors();
	}
	
}
