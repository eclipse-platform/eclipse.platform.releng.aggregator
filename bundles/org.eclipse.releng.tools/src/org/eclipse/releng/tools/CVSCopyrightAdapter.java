/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.Calendar;

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
	
	private static String filterString = "copyright"; // lowercase

	public CVSCopyrightAdapter(IResource[] resources) {
		super(resources);
	}

	public int getLastModifiedYear(IFile file, IProgressMonitor monitor) throws CoreException {
        try {
            monitor.beginTask("Fetching logs from CVS", 100);
            ICVSRemoteResource cvsFile = CVSWorkspaceRoot.getRemoteResourceFor(file);
            if (cvsFile != null && cvsFile.isManaged()) {
                // get the log entry for the revision loaded in the workspace
                ILogEntry entry = ((ICVSRemoteFile)cvsFile)
                        .getLogEntry(new SubProgressMonitor(monitor, 100));
                
                String logComment = entry.getComment();
				if (filterString != null && logComment.toLowerCase().indexOf(filterString) != -1) {
					//the last update was a copyright checkin - ignore
					return 0;
				}

				boolean isSWT= file.getProject().getName().startsWith("org.eclipse.swt"); //$NON-NLS-1$
				if (isSWT && logComment.indexOf("restore HEAD after accidental deletion") != -1) { //$NON-NLS-1$
					//the last update was the SWT accidental deletion of HEAD in 2009 - ignore
					return 0;
				}

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(entry.getDate());
				return calendar.get(Calendar.YEAR);
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
