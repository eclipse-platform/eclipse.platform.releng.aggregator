/*******************************************************************************
 * Copyright (c) 2010, 2019 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation of CVS adapter
 *     Gunnar Wagenknecht - initial API and implementation of Git adapter
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.releng.tools.git;

import java.io.IOException;
import java.util.Calendar;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.releng.tools.RepositoryProviderCopyrightAdapter;

public class GitCopyrightAdapter extends RepositoryProviderCopyrightAdapter {

	private static String filterString = "copyright"; // lowercase //$NON-NLS-1$

	/**
	 * Bugs to be filtered because they modified a lot of files in a trivial way
	 * and should not change the copyright year.
	 * <p>
	 * If the last commit message for a checked file starts with one of these
	 * strings the year will not be modified for this file.
	 * </p>
	 */
	private static final String[] FILTER_BUGS = new String[] {
			"Bug 535802", // license version update //$NON-NLS-1$
			"Bug 543933", // build javadocs with Java 11  //$NON-NLS-1$
	};

	public GitCopyrightAdapter(IResource[] resources) {
		super(resources);
	}

	@Override
	public int getLastModifiedYear(IFile file, IProgressMonitor monitor) throws CoreException {
			final RepositoryMapping mapping = RepositoryMapping.getMapping(file);
			if (mapping != null) {
				final Repository repo = mapping.getRepository();
				if (repo != null) {
					try (RevWalk walk = new RevWalk(repo)) {
						final ObjectId start = repo.resolve(Constants.HEAD);

						walk.setTreeFilter(AndTreeFilter.create(PathFilter.create(mapping.getRepoRelativePath(file)),
								TreeFilter.ANY_DIFF));
						walk.markStart(walk.lookupCommit(start));
						// dramatically increase performance for this use case
						// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=468850#c17
						walk.setRewriteParents(false);
						final RevCommit commit = walk.next();
						if (commit != null) {
							if (filterString != null
									&& commit.getFullMessage().toLowerCase().indexOf(filterString) != -1) {
								// the last update was a copyright check in -
								// ignore
								return 0;
							}

							for (String bugId : FILTER_BUGS) {
								if (commit.getShortMessage().startsWith(bugId)) {
									return 0;
								}
							}

							boolean isSWT = file.getProject().getName().startsWith("org.eclipse.swt"); //$NON-NLS-1$
							String logComment = commit.getFullMessage();
							if (isSWT && (logComment.indexOf("restore HEAD after accidental deletion") != -1 //$NON-NLS-1$
									|| logComment.indexOf("fix permission of files") != -1)) { //$NON-NLS-1$
								// ignore commits with above comments
								return 0;
							}

							boolean isPlatform = file.getProject().getName().equals("eclipse.platform"); //$NON-NLS-1$
							if (isPlatform && (logComment.indexOf("Merge in ant and update from origin/master") != -1 //$NON-NLS-1$
									|| logComment.indexOf(
											"Fixed bug 381684: Remove update from repository and map files") != -1)) { //$NON-NLS-1$
								// ignore commits with above comments
								return 0;
							}

							final Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(0);
							calendar.add(Calendar.SECOND, commit.getCommitTime());
							return calendar.get(Calendar.YEAR);
						}
					} catch (final IOException e) {
						throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0,
								NLS.bind("An error occured when processing {0}", file.getName()), e)); //$NON-NLS-1$
					}
				}
			}
		return -1;
	}

	@Override
	public void initialize(IProgressMonitor monitor) throws CoreException {
	}

}
