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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class FixCopyrightAction implements IObjectActionDelegate {

	public class MyInnerClass implements IResourceVisitor {
		public IProgressMonitor monitor;

		public boolean visit(IResource resource) throws CoreException {
			if (resource.getType() == IResource.FILE) {
				processFile((IFile) resource, monitor);
			}
			return true;
		}
	}

	private String propertiesCopyright;
	private String javaCopyright;
	private String newLine = System.getProperty("line.separator");
	private Map log = new HashMap();

	// The current selection
	protected IStructuredSelection selection;

	private static final int currentYear = new GregorianCalendar().get(Calendar.YEAR);

	/**
	 * Constructor for Action1.
	 */
	public FixCopyrightAction() {
		super();
	}

	/**
	 * Returns the selected resources.
	 * 
	 * @return the selected resources
	 */
	protected IFile[] getSelectedResources() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				IResource resource = (IResource) next;
				switch(resource.getType()) {
					case IResource.FILE :
						resources.add((IFile) resource);
						break;
					case IResource.FOLDER :
					case IResource.PROJECT :
						addMembers((IContainer) resource, resources);
						break;
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			IFile[] result = new IFile[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new IFile[0];
	}

	private void addMembers(IContainer container, List list) {
		try {
			IResource[] resources = container.members();
			for (int i = 0, max = resources.length; i < max; i++) {
				IResource resource = resources[i];
				switch(resource.getType()) {
					case IResource.FOLDER :
					case IResource.PROJECT :
						addMembers((IContainer) resource, list);
						break;
					case IResource.FILE :
						list.add((IFile) resource);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		log = new HashMap();
		try {
			final IResource[] results = getSelectedResources();
			PlatformUI.getWorkbench().getProgressService().run(true, /* fork */
			true, /* cancellable */
			new WorkspaceModifyOperation() {
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Fixing copyrights...", results.length);
						System.out.println("Start Fixing Copyrights");
						System.out.println("Resources selected: " + results.length);
						for (int i = 0; i < results.length; i++) {
							IResource resource = results[i];
							System.out.println("Resource selected: " + resource.getName());
							processFile((IFile) resource, monitor);
							monitor.worked(1);
						}

						writeLogs();
						//		displayLogs();
						System.out.println("Done Fixing Copyrights");

					} finally {
						monitor.done();
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Lookup and return the year in which the argument file was revised.  Return -1 if
	 * the revision year cannot be found.
	 */
	private int getCVSModificationYear(IFile file, IProgressMonitor monitor) {
		try {
			monitor.beginTask("Fetching logs from CVS", 100);

			try {
				ICVSRemoteResource cvsFile = CVSWorkspaceRoot.getRemoteResourceFor(file);
				if (cvsFile != null) {
					// get the log entry for the revision loaded in the workspace
					ILogEntry entry = ((ICVSRemoteFile) cvsFile).getLogEntry(new SubProgressMonitor(monitor, 100));
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(entry.getDate());
					return calendar.get(Calendar.YEAR);
				}
			} catch (TeamException e) {
				// do nothing
			}
		} finally {
			monitor.done();
		}

		return -1;
	}

	/**
	 *  
	 */
	private void writeLogs() {

		FileOutputStream aStream;
		try {
			File aFile = new File(Platform.getLocation().toFile(), "copyrightLog.txt");
			aStream = new FileOutputStream(aFile);
			Set aSet = log.entrySet();
			Iterator errorIterator = aSet.iterator();
			while (errorIterator.hasNext()) {
				Map.Entry anEntry = (Map.Entry) errorIterator.next();
				String errorDescription = (String) anEntry.getKey();
				aStream.write(errorDescription.getBytes());
				aStream.write(newLine.getBytes());
				List fileList = (List) anEntry.getValue();
				Iterator listIterator = fileList.iterator();
				while (listIterator.hasNext()) {
					String fileName = (String) listIterator.next();
					aStream.write("     ".getBytes());
					aStream.write(fileName.getBytes());
					aStream.write(newLine.getBytes());
				}
			}
			aStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void displayLogs() {

		Set aSet = log.entrySet();
		Iterator errorIterator = aSet.iterator();
		while (errorIterator.hasNext()) {
			Map.Entry anEntry = (Map.Entry) errorIterator.next();
			String errorDescription = (String) anEntry.getKey();
			System.out.println(errorDescription);
			List fileList = (List) anEntry.getValue();
			Iterator listIterator = fileList.iterator();
			while (listIterator.hasNext()) {
				String fileName = (String) listIterator.next();
				System.out.println("     " + fileName);
			}
		}
	}

	/**
	 * @param file
	 */
	private void processFile(IFile file, IProgressMonitor monitor) {
		SourceFile aSourceFile;

		String extension = file.getFileExtension();
		if (extension == null)
			return;
		monitor.subTask(file.getFullPath().toOSString());
		int fileType = IBMCopyrightComment.UNKNOWN_COMMENT;
		extension = extension.toLowerCase();
		if (extension.equals("java")) { //$NON-NLS-1$
			fileType = IBMCopyrightComment.JAVA_COMMENT;
			aSourceFile = new JavaFile(file);
		} else if (extension.equals("properties")) { //$NON-NLS-1$
			fileType = IBMCopyrightComment.PROPERTIES_COMMENT;
			aSourceFile = new PropertiesFile(file);
		} else
			return;

		if (aSourceFile.hasMultipleCopyrights()) {
			warn(file, null, "Multiple copyrights found.  File UNCHANGED."); //$NON-NLS-1$//$NON-NLS-2$
			return;
		}

		boolean hasCPL = true;

		BlockComment copyrightComment = aSourceFile.firstCopyrightComment();
		if (copyrightComment != null && (copyrightComment.getContents().indexOf("Common Public License") == -1)) {
			hasCPL = false;
		}

		IBMCopyrightComment ibmCopyright = IBMCopyrightComment.parse(copyrightComment, fileType);
		if (ibmCopyright == null) {
			warn(file, copyrightComment, "Could not interpret copyright comment"); //$NON-NLS-1$
			return;
		}

		// figure out if the comment should be updated by comparing the date range
		// in the comment to the last modification time provided by CVS

		int revised = ibmCopyright.getRevisionYear();
		int lastMod = revised;
		if (lastMod < currentYear)
			lastMod = getCVSModificationYear(file, new NullProgressMonitor());

		if (lastMod <= revised && !hasCPL) {
			return;
		}

		// either replace old copyright or put the new one at the top of the file
		ibmCopyright.setRevisionYear(lastMod);
		if (copyrightComment == null) {
			aSourceFile.insert(ibmCopyright.getCopyrightComment());
		} else {
			if (!copyrightComment.atTop())
				warn(file, copyrightComment, "Old copyright not at start of file, new copyright replaces old in same location"); //$NON-NLS-1$
			aSourceFile.replace(copyrightComment, ibmCopyright.getCopyrightComment());
		}
	}

	private void warn(IFile file, BlockComment firstBlockComment, String errorDescription) {
		List aList = (List) log.get(errorDescription);
		if (aList == null) {
			aList = new ArrayList();
			log.put(errorDescription, aList);
		}
		aList.add(file.getName());
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}

}
