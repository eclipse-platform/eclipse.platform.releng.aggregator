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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.ui.synchronize.SynchronizeCompareInput;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;


public class ProjectComparePageTreeInput extends SynchronizeCompareInput {
	
	private SyncInfoTree set;
	
	/**
	 * @param configuration
	 * @param diffViewerConfiguration
	 */
	public ProjectComparePageTreeInput(CompareConfiguration configuration,
			TreeViewerAdvisor diffViewerConfiguration) {
		super(configuration, diffViewerConfiguration);
		configuration.setLeftEditable(false);
		configuration.setRightEditable(false);
		set = (SyncInfoTree) diffViewerConfiguration.getSyncInfoSet();
	}

	/**
	 * @param projects the projects selected by the wizard
	 * @param tags the tags in map files associated with the given projects
	 * @param monitor
	 */
	public IResource[] getOutOfSyncProjects(IProject[] projects, CVSTag[] tags, IProgressMonitor monitor) throws TeamException {
		CVSCompareSubscriber subscriber = new CVSCompareSubscriber(projects, tags, "Releng Release");
		IResource[] r = null;
		try {
			subscriber.refresh( projects, IResource.DEPTH_INFINITE, monitor);
			set.beginInput();
			set.clear();
			subscriber.collectOutOfSync(projects, IResource.DEPTH_INFINITE, set, monitor);
			if (!set.isEmpty()) {
				// All the projects that have differences
				r = set.members(ResourcesPlugin.getWorkspace().getRoot());	
			}
		} finally {
			set.endInput(monitor);
		}
		return r;
	}

	
}
