/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;

public class GetBugsOperation {
	private static final CVSTag TAG_1_1 = new CVSTag("1.1", CVSTag.VERSION);

	private ReleaseWizard wizard;

	private SyncInfoSet syncInfoSet;

	private SyncInfo[] syncInfos;

	private Pattern bugPattern;

	protected GetBugsOperation(ReleaseWizard wizard, SyncInfoSet syncInfoSet) {
		this.wizard = wizard;
		this.syncInfoSet = syncInfoSet;
		bugPattern = Pattern.compile("bug (\\d+)", Pattern.CASE_INSENSITIVE
				| Pattern.UNICODE_CASE);
	}

	protected void run(final BuildNotesPage page) {
		try {
			wizard.getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					final int totalWork = 101;
					monitor
							.beginTask(
									Messages.getString("GetBugsOperation.0"), totalWork); //$NON-NLS-1$

					// task 1 -- get bug number from comments
					syncInfos = syncInfoSet.getSyncInfos();
					Set bugTree = getBugNumbersFromComments(syncInfos,
							new SubProgressMonitor(monitor, 85,
									SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

					// task 2 -- create map of bugs and summaries
					Integer[] bugs = (Integer[]) bugTree
							.toArray(new Integer[0]);
					final TreeMap map = (TreeMap) getBugzillaSummaries(bugs,
							new SubProgressMonitor(monitor, 15,
									SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
					page.getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							page.setMap(map);
						}
					});
					monitor.done();
				}
			});
		} catch (InterruptedException e) {
			CVSUIPlugin.openError(wizard.getShell(), null, null, e);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(wizard.getShell(), null, null, e);
		}
	}

	protected Set getBugNumbersFromComments(SyncInfo[] syncInfos,
			IProgressMonitor monitor) {
		monitor.beginTask("Scanning comments for bug numbers", syncInfos.length);
		TreeSet set = new TreeSet();
		for (int i = 0; i < syncInfos.length; i++) {
			SyncInfo info = syncInfos[i];
			getBugNumbersForSyncInfo(info, monitor, set);
			monitor.worked(1);
		}
		monitor.done();
		return set;
	}

	private void getBugNumbersForSyncInfo(SyncInfo info,
			IProgressMonitor monitor, Set set) {
		try {
			CVSTag remoteTag = null;
			CVSTag localTag = null;
			IResource localResource = info.getLocal();
			if (localResource.exists()) {
				ICVSResource cvsLocal = CVSWorkspaceRoot
						.getCVSResourceFor(localResource);
				ResourceSyncInfo rsync = cvsLocal.getSyncInfo();
				if (rsync != null) {
					localTag = new CVSTag(rsync.getRevision(), CVSTag.VERSION);
				}
			}
			if (localTag == null) {
				localTag = getProjectTag(localResource.getProject());
			}

			ICVSRemoteResource cvsRemoteResource = (ICVSRemoteResource) info
					.getRemote();
			if (cvsRemoteResource == null) {
				cvsRemoteResource = CVSWorkspaceRoot
						.getRemoteResourceFor(localResource);
				remoteTag = TAG_1_1;
			} else {
				ResourceSyncInfo rsync = cvsRemoteResource.getSyncInfo();
				if (rsync != null) {
					remoteTag = new CVSTag(rsync.getRevision(), CVSTag.VERSION);
				}
			}
			if (remoteTag == null) {
				remoteTag = TAG_1_1;
			}

			LogEntryCache cache = new RemoteLogOperation.LogEntryCache();

			RemoteLogOperation logOp = new RemoteLogOperation(null,
					new ICVSRemoteResource[] { cvsRemoteResource }, localTag,
					remoteTag, cache);
			logOp.run(monitor);
			ILogEntry[] logEntries = cache.getLogEntries(cvsRemoteResource);
			for (int i = 0; i < logEntries.length; i++) {
				ILogEntry entry = logEntries[i];
				if (!entry.getRevision().equals(remoteTag.getName())
						|| remoteTag == TAG_1_1) {
					findBugNumber(entry.getComment(), set);
				}
			}
		} catch (CVSException e) {
			CVSUIPlugin.openError(wizard.getShell(), null, null, e);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(wizard.getShell(), null, null, e);
		} catch (InterruptedException e) {
			CVSUIPlugin.openError(wizard.getShell(), null, null, e);
		}
	}

	private CVSTag getProjectTag(IProject project) {
		IFile dotProject = project.getFile(".project");
		if (dotProject.exists()) {
			ICVSResource cvsResource = CVSWorkspaceRoot
					.getCVSResourceFor(dotProject);
			try {
				if (cvsResource != null && cvsResource.getSyncInfo() != null) {
					CVSTag tag = cvsResource.getSyncInfo().getTag();
					if (tag != null) {
						return tag;
					}
				}
			} catch (CVSException e) {
				CVSUIPlugin.openError(wizard.getShell(), null, null, e);
			}
		}
		return new CVSTag();
	}

	protected void findBugNumber(String comment, Set set) {
		if (comment == null) {
			return;
		}
		Matcher matcher = bugPattern.matcher(comment);
		while (matcher.find()) {
			Integer bugNumber = new Integer(matcher.group(1));
			set.add(bugNumber);
		}
	}

	/*
	 * Method uses set of bug numbers to query bugzilla and get summary of each
	 * bug
	 */
	protected Map getBugzillaSummaries(Integer[] bugs, IProgressMonitor monitor) {
		monitor.beginTask(
				Messages.getString("GetBugsOperation.1"), bugs.length + 1); //$NON-NLS-1$
		final String BUG_DATABASE_PREFIX = "https://bugs.eclipse.org/bugs/show_bug.cgi?id=";
		final String BUG_DATABASE_POSTFIX = "&ctype=xml";
		int summaryStartIndex = 0;
		int summaryEndIndex = 0;
		HttpURLConnection hURL;
		DataInputStream in;
		URLConnection url;
		StringBuffer buffer;
		TreeMap map = new TreeMap();
		for (int i = 0; i < bugs.length; i++) {
			try {
				url = (new URL(BUG_DATABASE_PREFIX + bugs[i]
						+ BUG_DATABASE_POSTFIX).openConnection());
				if (url instanceof HttpURLConnection) {
					hURL = (HttpURLConnection) url;
					hURL.setAllowUserInteraction(true);
					hURL.connect();
					in = new DataInputStream(hURL.getInputStream());
					buffer = new StringBuffer();
					try {
						if (hURL.getResponseCode() != HttpURLConnection.HTTP_OK) {
							throw new IOException("Bad response code");
						}
						while (true) {
							buffer.append((char) in.readUnsignedByte());
						}
					} catch (EOFException e) {
						hURL.disconnect();
					}
					String webPage = buffer.toString();
					summaryStartIndex = webPage.indexOf("<short_desc>") + 12;
					summaryEndIndex = webPage.indexOf("</short_desc>",
							summaryStartIndex);
					if (summaryStartIndex != -1 & summaryEndIndex != -1) {
						String summary = webPage.substring(summaryStartIndex,
								summaryEndIndex);
						summary = summary.replaceAll("&quot;", "\"");
						summary = summary.replaceAll("&lt;", "<");
						summary = summary.replaceAll("&gt;", ">");
						summary = summary.replaceAll("&amp;", "&");
						summary = summary.replaceAll("&apos;", "'");
						map.put(bugs[i], summary);
					}
				}
			} catch (IOException e) {
				CVSUIPlugin.openError(wizard.getShell(), null, null, e);
			}
			monitor.worked(1);
		}
		monitor.done();
		return map;
	}
}
