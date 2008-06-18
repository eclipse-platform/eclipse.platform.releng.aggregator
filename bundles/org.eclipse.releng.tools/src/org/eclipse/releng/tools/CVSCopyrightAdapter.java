/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;

public class CVSCopyrightAdapter extends RepositoryProviderCopyrightAdapter {
	
	public CVSCopyrightAdapter(IResource[] resources) {
		super(resources);
	}

	public int getLastModifiedYear(IFile file, IProgressMonitor monitor) throws CoreException {
        try {
            monitor.beginTask("Fetching logs from CVS", 100); //$NON-NLS-1$
            ICVSRemoteResource cvsFile = CVSWorkspaceRoot.getRemoteResourceFor(file);
            if (cvsFile != null && cvsFile.isManaged()) {
                // get the log entry for the revision loaded in the workspace
                ILogEntry entry = ((ICVSRemoteFile)cvsFile)
                        .getLogEntry(new SubProgressMonitor(monitor, 100));
                return entry.getDate().getYear() + 1900;
            }
        } finally {
            monitor.done();
        }

        return -1;
	}

	public void initialize(IProgressMonitor monitor) throws CoreException {
		// TODO We should perform a bulk "log" command to get the last modified year
	}

}
