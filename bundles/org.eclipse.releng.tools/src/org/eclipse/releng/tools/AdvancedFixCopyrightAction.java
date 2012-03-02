/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.preferences.RelEngCopyrightConstants;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class AdvancedFixCopyrightAction implements IObjectActionDelegate {

	public class FixCopyrightVisitor implements IResourceVisitor {
		private final IProgressMonitor monitor;
		private final RepositoryProviderCopyrightAdapter adapter;

		public FixCopyrightVisitor(RepositoryProviderCopyrightAdapter adapter,
				IProgressMonitor monitor) {
			this.adapter = adapter;
			this.monitor = monitor;
		}

		public boolean visit(IResource resource) throws CoreException {
			if (!monitor.isCanceled()) {
				if (resource.getType() == IResource.FILE) {
					processFile((IFile) resource, adapter, monitor);
				}
			}
			return true;
		}
	}

	private String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
	private Map log = new HashMap();
	private MessageConsole console;

	// The current selection
	protected IStructuredSelection selection;

	private static final int currentYear = new GregorianCalendar().get(Calendar.YEAR);

	/**
	 * Returns the selected resources.
	 * 
	 * @return the selected resources
	 */
	protected IResource[] getSelectedResources() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				addResource(elements.next(), resources);
			}
		}
		if (resources != null && !resources.isEmpty()) {
			IResource[] result = new IResource[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new IResource[0];
	}

	private void addResource(Object element, ArrayList resources) {
		if (element instanceof IResource) {
			resources.add(element);
		} else if (element instanceof IWorkingSet) {
			IWorkingSet ws = (IWorkingSet) element;
			IAdaptable[] elements= ws.getElements();
			for (int i= 0; i < elements.length; i++)
				addResource(elements[i], resources);
		} else if (element instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) element;
			addResource((IResource)a.getAdapter(IResource.class), resources);
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
		console = new MessageConsole(Messages.getString("AdvancedFixCopyrightAction.0"), null); //$NON-NLS-1$
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] {console});
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IConsoleConstants.ID_CONSOLE_VIEW);
		} catch (PartInitException e) {
			// Don't fail if we can't show the console
			RelEngPlugin.log(e);
		}
		final MessageConsoleStream stream = console.newMessageStream();

		WorkspaceJob wJob = new WorkspaceJob(Messages.getString("AdvancedFixCopyrightAction.1")) { //$NON-NLS-1$
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					long start = System.currentTimeMillis();
					stream.println(Messages.getString("AdvancedFixCopyrightAction.2")); //$NON-NLS-1$
					IResource[] results = getSelectedResources();
					stream.println(NLS.bind(
							Messages.getString("AdvancedFixCopyrightAction.3"), Integer.toString(results.length))); //$NON-NLS-1$
					monitor.beginTask(Messages.getString("AdvancedFixCopyrightAction.4"), results.length * 100 + 100); //$NON-NLS-1$

					RepositoryProviderCopyrightAdapter adapter = createCopyrightAdapter(results);
					if(adapter == null) {
						if(!RelEngPlugin.getDefault().getPreferenceStore().getBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY)) {
							throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0, Messages.getString("AdvancedFixCopyrightAction.5"), null)); //$NON-NLS-1$
						}
					} else {
						adapter.initialize(new SubProgressMonitor(monitor, 100));
					}
					List exceptions = new ArrayList();
					for (int i = 0; i < results.length; i++) {
						IResource resource = results[i];
						stream.println(NLS.bind(
								Messages.getString("AdvancedFixCopyrightAction.6"), resource.getName())); //$NON-NLS-1$
						try {
							resource.accept(new FixCopyrightVisitor(adapter, monitor));

							monitor.worked(100);
						} catch (CoreException e1) {
							exceptions.add(e1);
						}
					}

					writeLogs();
					displayLogs(stream);
					stream.println(Messages.getString("AdvancedFixCopyrightAction.7")); //$NON-NLS-1$
					long end = System.currentTimeMillis();
					stream.println(NLS.bind(
							Messages.getString("AdvancedFixCopyrightAction.8"), Long.toString(end - start))); //$NON-NLS-1$
					if (!exceptions.isEmpty()) {
						stream.println(Messages.getString("AdvancedFixCopyrightAction.9")); //$NON-NLS-1$
						if (exceptions.size() == 1) {
							throw (CoreException)exceptions.get(0);
						} else {
							List status = new ArrayList();
							for (Iterator iterator = exceptions.iterator(); iterator
									.hasNext();) {
								CoreException ce = (CoreException) iterator.next();
								status.add(new Status(
										ce.getStatus().getSeverity(), 
										ce.getStatus().getPlugin(),
										ce.getStatus().getCode(),
										ce.getStatus().getMessage(),
										ce));
							}
							throw new CoreException(new MultiStatus(RelEngPlugin.ID,
									0, (IStatus[]) status.toArray(new IStatus[status.size()]),
									Messages.getString("AdvancedFixCopyrightAction.10"), //$NON-NLS-1$
									null));
						}
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		wJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		wJob.setUser(true);
		wJob.schedule();
	}

	protected RepositoryProviderCopyrightAdapter createCopyrightAdapter(
			IResource[] results) throws CoreException {
		RepositoryProviderType providerType = null;
		for (int i = 0; i < results.length; i++) {
			IResource resource = results[i];
			RepositoryProvider p = RepositoryProvider.getProvider(resource.getProject());
			if (p != null) {
				if (providerType == null) {
					providerType = RepositoryProviderType.getProviderType(p.getID());
				} else if (!providerType.getID().equals(p.getID())) {
					throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0, Messages.getString("AdvancedFixCopyrightAction.11"), null)); //$NON-NLS-1$
				}
			}
		}
		if(providerType == null) {
			return null;
		}
		IRepositoryProviderCopyrightAdapterFactory factory = (IRepositoryProviderCopyrightAdapterFactory)providerType.getAdapter(IRepositoryProviderCopyrightAdapterFactory.class);
		if (factory == null) {
			factory = (IRepositoryProviderCopyrightAdapterFactory)Platform.getAdapterManager().loadAdapter(providerType, IRepositoryProviderCopyrightAdapterFactory.class.getName());
			if (factory == null) {
				throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0, NLS.bind(Messages.getString("AdvancedFixCopyrightAction.12"), providerType.getID()), null)); //$NON-NLS-1$
			}
		}
		return factory.createAdapater(results);
	}

	/**
	 *  
	 */
	private void writeLogs() {

		FileOutputStream aStream;
		try {
			File aFile = new File(Platform.getLocation().toFile(),
					"copyrightLog.txt"); //$NON-NLS-1$
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
					aStream.write("     ".getBytes()); //$NON-NLS-1$
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

	private void displayLogs(MessageConsoleStream stream) {

		Set aSet = log.entrySet();
		Iterator errorIterator = aSet.iterator();
		while (errorIterator.hasNext()) {
			Map.Entry anEntry = (Map.Entry) errorIterator.next();
			String errorDescription = (String) anEntry.getKey();
			stream.println(errorDescription);
			List fileList = (List) anEntry.getValue();
			Iterator listIterator = fileList.iterator();
			while (listIterator.hasNext()) {
				String fileName = (String) listIterator.next();
				stream.println("     " + fileName); //$NON-NLS-1$
			}
		}
	}

	private void processFile(IFile file, RepositoryProviderCopyrightAdapter adapter, IProgressMonitor monitor) {
		monitor.subTask(file.getFullPath().toOSString());

		if (file.getFileExtension() == null) {
			warn(file, null, Messages.getString("AdvancedFixCopyrightAction.13")); //$NON-NLS-1$
			return;
		}

		SourceFile aSourceFile = SourceFile.createFor(file);
		if (aSourceFile == null)
			return;

		IPreferenceStore prefStore = RelEngPlugin.getDefault().getPreferenceStore();
		if ((aSourceFile.getFileType() == CopyrightComment.PROPERTIES_COMMENT && prefStore
				.getBoolean(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY))
				|| (aSourceFile.getFileType() == CopyrightComment.XML_COMMENT && prefStore
				.getBoolean(RelEngCopyrightConstants.IGNORE_XML_KEY))) {
			return;
		}
		if (aSourceFile.hasMultipleCopyrights()) {
			warn(file, null, Messages.getString("AdvancedFixCopyrightAction.14")); //$NON-NLS-1$
			return;
		}

		BlockComment copyrightComment = aSourceFile.getFirstCopyrightComment();
		CopyrightComment ibmCopyright = null;
		// if replacing all comments, don't even parse, just use default copyright comment
		if (prefStore.getBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY)) {
			ibmCopyright = AdvancedCopyrightComment.defaultComment(aSourceFile.getFileType());
		} else {
			ibmCopyright = AdvancedCopyrightComment.parse(copyrightComment, aSourceFile.getFileType());  
			if (ibmCopyright == null) {
				// Let's see if the file is EPL
				ibmCopyright = IBMCopyrightComment.parse(copyrightComment, aSourceFile.getFileType());
				if (ibmCopyright != null) {
					warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.15")); //$NON-NLS-1$
				}
			}
		}

		if (ibmCopyright == null) {
			warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.16")); //$NON-NLS-1$
			return;
		}

		// figure out revision year
		int revised = ibmCopyright.getRevisionYear();
		int lastMod = revised;
		if (prefStore.getBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY) || adapter == null)
			lastMod = prefStore.getInt(RelEngCopyrightConstants.REVISION_YEAR_KEY);
		else {
			// figure out if the comment should be updated by comparing the date range
			// in the comment to the last modification time provided by adapter
			if (lastMod < currentYear) {
				try {
					lastMod = adapter.getLastModifiedYear(file, new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					// Let's log the exception and continue
					RelEngPlugin
					.log(IStatus.ERROR,
							NLS.bind(
									Messages.getString("AdvancedFixCopyrightAction.17"), file.getFullPath()), e); //$NON-NLS-1$
				}
				if (lastMod > currentYear) {
					// Don't allow future years to be used in the copyright
					lastMod = currentYear;
				}
				if (lastMod == 0) {
					warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.18")); //$NON-NLS-1$
					return;
				}
				// use default revision year
				if (lastMod == -1) {
					lastMod = prefStore.getInt(RelEngCopyrightConstants.REVISION_YEAR_KEY);
				}
				if (lastMod < revised) {
					// Don't let the copyright date go backwards
					lastMod = revised;
				}
			}
		}

		// only exit if existing copyright comment already contains the year
		// of last modification and not overwriting all comments
		if (lastMod <= revised && (copyrightComment != null) && (!prefStore.getBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY)))
			return;

		// either replace old copyright or put the new one at the top of the file
		ibmCopyright.setRevisionYear(lastMod);
		if (copyrightComment == null)
			aSourceFile.insert(ibmCopyright.getCopyrightComment());
		else {
			if (!copyrightComment.atTop())
				warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.19")); //$NON-NLS-1$
			aSourceFile.replace(copyrightComment, ibmCopyright.getCopyrightComment());
		}
	}

	private void warn(IFile file, BlockComment firstBlockComment,
			String errorDescription) {
		List aList = (List) log.get(errorDescription);
		if (aList == null) {
			aList = new ArrayList();
			log.put(errorDescription, aList);
		}
		aList.add(file.getFullPath().toString());
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
