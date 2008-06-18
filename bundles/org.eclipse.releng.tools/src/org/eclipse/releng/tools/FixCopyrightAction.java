/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
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

	private String newLine = System.getProperty("line.separator");
	private Map log = new HashMap();
	private boolean swt = false;
	private String filterString;

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
			final String[] filterArray = new String[1];
			Dialog filterCriteria = new Dialog(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()) {
				
				Text text;
				
				protected void configureShell(Shell newShell) {
					super.configureShell(newShell);
					newShell.setText(Messages.getString("CopyrightDialog.1"));
				}

				protected Control createDialogArea(Composite parent) {
					Composite parentComposite = (Composite) super
							.createDialogArea(parent);
					Composite composite = new Composite(parentComposite,
							SWT.NONE);
					composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
							true, true));
					composite.setLayout(new GridLayout(2, false));

					Label label = new Label(composite, SWT.NONE);
					label.setText(Messages.getString("CopyrightDialog.2"));
					text = new Text(composite, SWT.BORDER | SWT.SINGLE);
					text.setText("copyright");
					text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
							false));
					
					Label header = new Label(composite, SWT.WRAP);
					header.setText(Messages.getString("CopyrightDialog.3"));
					GridData gridData = new GridData(SWT.FILL, SWT.FILL, true,
							false);
					gridData.horizontalSpan = 2;
					header.setLayoutData(gridData);
					return parentComposite;
				}

				protected void okPressed() {
					filterArray[0] = text.getText();
					super.okPressed();
				}
				
			};
			filterCriteria.setBlockOnOpen(true);
			int result = filterCriteria.open();
			if (result == Dialog.CANCEL)
				return;
			
			if (!filterArray[0].trim().equals("")) {
				filterString = filterArray[0];
			}
			
			final IResource[] results = getSelectedResources();
			PlatformUI.getWorkbench().getProgressService().run(true, /* fork */
			true, /* cancellable */
			new WorkspaceModifyOperation() {
				protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Fixing copyrights...", results.length);
						System.out.println("Start Fixing Copyrights");
						System.out.println("Resources selected: " + results.length);
						if (results.length > 0) {
							IProject project = results[0].getProject();
							if (project.getName().equals("org.eclipse.swt")) swt = true;
						}
						for (int i = 0; i < results.length; i++) {
							IResource resource = results[i];
							System.out.println("Resource selected: " + resource.getFullPath());
							processFile((IFile) resource, monitor);
							monitor.worked(1);
							if (monitor.isCanceled()) {
								monitor.setCanceled(true);
								break;
							}
						}

						writeLogs();
						if (swt) displayLogs();
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
					ILogEntry entry = ((ICVSRemoteFile) cvsFile)
							.getLogEntry(new SubProgressMonitor(monitor, 100));

					String logComment = entry.getComment();
					if (swt && logComment.indexOf("CPL") != -1
							&& logComment.indexOf("EPL") != -1) {
						// the last modification was the copyright comment
						// update for the transition from CPL to EPL, so
						// ignore
						return 0;

					}
					else if (filterString != null && logComment.indexOf(filterString) != -1) {
						//the last update was a copyright checkin - ignore
						return 0;
					}
					
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
		String extension = file.getFileExtension();
		if (extension == null) {
			warn(file, null, "File has no extension.  File UNCHANGED."); //$NON-NLS-1$
			return;
		}
		monitor.subTask(file.getFullPath().toOSString());
		SourceFile aSourceFile = SourceFile.createFor(file);
		if (aSourceFile == null) {
			return;
		}

		if (aSourceFile.hasMultipleCopyrights()) {
			warn(file, null, "Multiple copyrights found.  File UNCHANGED."); //$NON-NLS-1$//$NON-NLS-2$
			return;
		}

		boolean hasCPL = false, hasGPL = false, hasMPL = false, hasApple = false;

		BlockComment copyrightComment = aSourceFile.firstCopyrightComment();
		if (copyrightComment != null) {
			String copyrightString = copyrightComment.getContents();
			if (copyrightString.indexOf("Common Public License") != -1) hasCPL = true;
			if (swt) {
				if (copyrightString.indexOf("GPL") != -1) hasGPL = true;
				if (copyrightString.indexOf("MPL") != -1) hasMPL = true;
				if (copyrightString.indexOf("Apple Computer") != -1) hasApple = true; 
			}
		}

		IBMCopyrightComment ibmCopyright = IBMCopyrightComment.parse(copyrightComment, aSourceFile.getFileType());
		if (ibmCopyright == null) {
			warn(file, copyrightComment, "Could not interpret copyright comment.  File UNCHANGED."); //$NON-NLS-1$
			return;
		}

		// figure out if the comment should be updated by comparing the date range
		// in the comment to the last modification time provided by CVS

		int revised = ibmCopyright.getRevisionYear();
		int lastMod = revised;
		if (lastMod < currentYear)
			lastMod = getCVSModificationYear(file, new NullProgressMonitor());

		if (lastMod <= revised && !hasCPL) {
			return; // no update necessary
		}

		// either replace old copyright or put the new one at the top of the file
		ibmCopyright.setRevisionYear(lastMod);
		if (copyrightComment == null) {
			aSourceFile.insert(ibmCopyright.getCopyrightComment());
		} else {
			if (!copyrightComment.atTop()) {
				warn(file, copyrightComment, "Old copyright not at start of file, new copyright replaces old in same location."); //$NON-NLS-1$
			}
			if (hasGPL || hasMPL || hasApple) {
				warn(file, copyrightComment, "Old copyright contains GPL, MPL, or Apple. Copyright unchanged. Date updated if necessary."); //$NON-NLS-1$
				aSourceFile.replace(copyrightComment, ibmCopyright.getOriginalCopyrightComment());
			} else {
				aSourceFile.replace(copyrightComment, ibmCopyright.getCopyrightComment());
			}
		}
	}

	private void warn(IFile file, BlockComment firstBlockComment, String errorDescription) {
		List aList = (List) log.get(errorDescription);
		if (aList == null) {
			aList = new ArrayList();
			log.put(errorDescription, aList);
		}
		aList.add(file.getFullPath().toOSString());
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
