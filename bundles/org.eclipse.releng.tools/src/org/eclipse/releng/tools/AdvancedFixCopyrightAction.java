/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     IBH SYSTEMS GmbH - allow removing the consoles
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
import org.eclipse.core.runtime.SubMonitor;
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

	final class FixConsole extends MessageConsole {
		private FixConsole() {
			super(Messages.getString("AdvancedFixCopyrightAction.0"), null); //$NON-NLS-1$
		}
	}


	public class FixCopyrightVisitor implements IResourceVisitor {
		private final IProgressMonitor monitor;
		private final RepositoryProviderCopyrightAdapter adapter;

		public FixCopyrightVisitor(RepositoryProviderCopyrightAdapter adapter, IProgressMonitor monitor) {
			this.adapter = adapter;
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (!monitor.isCanceled()) {
				if (resource.getType() == IResource.FILE) {
					monitor.subTask(((IFile) resource).getFullPath().toOSString());
					processFile((IFile) resource, adapter, monitor);
					monitor.worked(1);
				}
			}
			return true;
		}
	}

	/**
	 * Visit each file to count total number of files that we will traverse.
	 * This is used to show the progress correctly.
	 */
	private class FileCountVisitor implements IResourceVisitor {
		private int fileCount;

		public FileCountVisitor() {
			this.fileCount = 0;
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (resource.getType() == IResource.FILE) {
				fileCount += 1;
			}
			return true;
		}

		public int getfileCount() {
			return this.fileCount;
		}
	}

	private String newLine = System.lineSeparator();
	private Map<String, List<String>> log = new HashMap<>();
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
		ArrayList<IResource> resources = new ArrayList<>();
		if (!selection.isEmpty()) {

			Iterator<?> elements = selection.iterator();
			while (elements.hasNext()) {
				addResource(elements.next(), resources);
			}
		}
		IResource[] result = new IResource[resources.size()];
		resources.toArray(result);
		return result;
	}

	private void addResource(Object element, ArrayList<IResource> resources) {
		if (element instanceof IResource) {
			resources.add((IResource) element);
		} else if (element instanceof IWorkingSet) {
			IWorkingSet ws = (IWorkingSet) element;
			IAdaptable[] elements = ws.getElements();
			for (int i = 0; i < elements.length; i++)
				addResource(elements[i], resources);
		} else if (element instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) element;
			addResource(a.getAdapter(IResource.class), resources);
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {
		log = new HashMap<>();
		console = new FixConsole();
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(IConsoleConstants.ID_CONSOLE_VIEW);
		} catch (PartInitException e) {
			// Don't fail if we can't show the console
			RelEngPlugin.log(e);
		}
		final MessageConsoleStream stream = console.newMessageStream();

		WorkspaceJob wJob = new WorkspaceJob(Messages.getString("AdvancedFixCopyrightAction.1")) { //$NON-NLS-1$
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					long start = System.currentTimeMillis();
					stream.println(Messages.getString("AdvancedFixCopyrightAction.2")); //$NON-NLS-1$
					IResource[] results = getSelectedResources();
					stream.println(NLS.bind(Messages.getString("AdvancedFixCopyrightAction.3"), //$NON-NLS-1$
							Integer.toString(results.length)));

					int totalFileCount = countFiles(results);
					SubMonitor subMonitor = SubMonitor.convert(monitor, totalFileCount);
					subMonitor.beginTask(Messages.getString("AdvancedFixCopyrightAction.4"), totalFileCount); //$NON-NLS-1$

					RepositoryProviderCopyrightAdapter adapter = createCopyrightAdapter(results);
					if (adapter == null) {
						if (!RelEngPlugin.getDefault().getPreferenceStore()
								.getBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY)) {
							throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0,
									Messages.getString("AdvancedFixCopyrightAction.5"), null)); //$NON-NLS-1$
						}
					} else {
						adapter.initialize(subMonitor);
					}
					List<CoreException> exceptions = new ArrayList<>();
					for (IResource resource : results) {
						stream.println(
								NLS.bind(Messages.getString("AdvancedFixCopyrightAction.6"), resource.getName())); //$NON-NLS-1$
						try {
							resource.accept(new FixCopyrightVisitor(adapter, subMonitor));
						} catch (CoreException e1) {
							exceptions.add(e1);
						}
					}

					writeLogs();
					displayLogs(stream);
					stream.println(Messages.getString("AdvancedFixCopyrightAction.7")); //$NON-NLS-1$
					long end = System.currentTimeMillis();
					stream.println(
							NLS.bind(Messages.getString("AdvancedFixCopyrightAction.8"), Long.toString(end - start))); //$NON-NLS-1$
					if (!exceptions.isEmpty()) {
						stream.println(Messages.getString("AdvancedFixCopyrightAction.9")); //$NON-NLS-1$
						if (exceptions.size() == 1) {
							throw exceptions.get(0);
						} else {
							List<Status> status = new ArrayList<>();
							for (CoreException ce : exceptions) {
								status.add(new Status(ce.getStatus().getSeverity(), ce.getStatus().getPlugin(),
										ce.getStatus().getCode(), ce.getStatus().getMessage(), ce));
							}
							throw new CoreException(
									new MultiStatus(RelEngPlugin.ID, 0, status.toArray(new IStatus[status.size()]),
											Messages.getString("AdvancedFixCopyrightAction.10"), //$NON-NLS-1$
											null));
						}
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			private int countFiles(IResource[] results) {
				int sum = 0;
				for (IResource file : results) {
					FileCountVisitor fileCountVisitor = new FileCountVisitor();
					try {
						file.accept(fileCountVisitor);
					} catch (CoreException e) {
						// This exception can be ignored.
						// Here we are only counting files.
						// It will be handled when files are actually
						// proccessed.
					}
					sum += fileCountVisitor.getfileCount();
				}
				return sum;
			}
		};

		wJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		wJob.setUser(true);
		wJob.schedule();
	}

	protected RepositoryProviderCopyrightAdapter createCopyrightAdapter(IResource[] results) throws CoreException {
		RepositoryProviderType providerType = null;
		for (IResource resource : results) {
			RepositoryProvider p = RepositoryProvider.getProvider(resource.getProject());
			if (p != null) {
				if (providerType == null) {
					providerType = RepositoryProviderType.getProviderType(p.getID());
				} else if (!providerType.getID().equals(p.getID())) {
					throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0,
							Messages.getString("AdvancedFixCopyrightAction.11"), null)); //$NON-NLS-1$
				}
			}
		}
		if (providerType == null) {
			return null;
		}
		IRepositoryProviderCopyrightAdapterFactory factory = providerType
				.getAdapter(IRepositoryProviderCopyrightAdapterFactory.class);
		if (factory == null) {
			factory = (IRepositoryProviderCopyrightAdapterFactory) Platform.getAdapterManager()
					.loadAdapter(providerType, IRepositoryProviderCopyrightAdapterFactory.class.getName());
			if (factory == null) {
				throw new CoreException(new Status(IStatus.ERROR, RelEngPlugin.ID, 0,
						NLS.bind(Messages.getString("AdvancedFixCopyrightAction.12"), providerType.getID()), null)); //$NON-NLS-1$
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
			File aFile = new File(Platform.getLocation().toFile(), "copyrightLog.txt"); //$NON-NLS-1$
			aStream = new FileOutputStream(aFile);
			Set<Map.Entry<String, List<String>>> aSet = log.entrySet();
			Iterator<Map.Entry<String, List<String>>> errorIterator = aSet.iterator();
			while (errorIterator.hasNext()) {
				Map.Entry<String, List<String>> anEntry = errorIterator.next();
				String errorDescription = anEntry.getKey();
				aStream.write(errorDescription.getBytes());
				aStream.write(newLine.getBytes());
				List<String> fileList = anEntry.getValue();
				Iterator<String> listIterator = fileList.iterator();
				while (listIterator.hasNext()) {
					String fileName = listIterator.next();
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

		Set<Map.Entry<String, List<String>>> aSet = log.entrySet();
		Iterator<Map.Entry<String, List<String>>> errorIterator = aSet.iterator();
		while (errorIterator.hasNext()) {
			Map.Entry<String, List<String>> anEntry = errorIterator.next();
			String errorDescription = anEntry.getKey();
			stream.println(errorDescription);
			List<String> fileList = anEntry.getValue();
			Iterator<String> listIterator = fileList.iterator();
			while (listIterator.hasNext()) {
				String fileName = listIterator.next();
				stream.println("     " + fileName); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Handle an induvidual file at a time
	 *
	 * <p>
	 * Functions:
	 * <ul>
	 * -
	 * <li>get user defined default date</li> -
	 * <li>identify if multiple comments inserted.</li>
	 * <li>update existing comment or insert a new one</li>
	 * </ul>
	 *
	 * @param file
	 *            - file to be proccessed (.java, .bat, .xml etc...)
	 * @param adapter
	 * @param monitor
	 */
	private void processFile(IFile file, RepositoryProviderCopyrightAdapter adapter, IProgressMonitor monitor) {

		// Missign file Extension
		if (!checkFileExtension(file)) {
			return;
		}

		// Create an instance of the appropriate Source container.
		// (xml/java/bash etc..)
		SourceFile aSourceFile = SourceFile.createFor(file);
		if (!checkSourceCreatedOk(aSourceFile, file)) {
			return;
		}

		// Aquire user settings
		IPreferenceStore prefStore = RelEngPlugin.getDefault().getPreferenceStore();

		// Check if user wants to skip over this file
		if (!checkUserFileIgnoreSettings(prefStore, aSourceFile)) {
			return;
		}

		// Skip over source files that have multiple copy-right notes
		if (!checkMultipleCopyright(file, aSourceFile)) {
			return;
		}

		// Extract 'current' 'raw' comment from the document.
		BlockComment copyrightComment = aSourceFile.getFirstCopyrightComment();

		CopyrightComment ibmCopyright = null;

		// if replacing all comments, don't even parse, just use default
		// copyright comment
		if (prefStore.getBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY)) {

			// Aquire user default comments from settings.
			ibmCopyright = AdvancedCopyrightComment.defaultComment(aSourceFile.getFileType());
		} else {

			// Parse the raw comment and update the last revision year.
			// (inserting a revision year if neccessary).
			ibmCopyright = AdvancedCopyrightComment.parse(copyrightComment, aSourceFile.getFileType());

			// Check that the newly created comment was constructed correctly.
			if (ibmCopyright == null) {

				// Check against a standard IBM copyright header.
				// Let's see if the file is EPL
				ibmCopyright = IBMCopyrightComment.parse(copyrightComment, aSourceFile.getFileType());
				if (ibmCopyright != null) {
					// Could not proccess file at all.
					warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.15")); //$NON-NLS-1$
				}
			}
		}

		// Could not determine the 'new' 'copyright' header. Do not procces
		// file.
		if (ibmCopyright == null) {
			warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.16")); //$NON-NLS-1$
			return;
		}

		ibmCopyright.setLineDelimiter(aSourceFile.getLineDelimiter());

		// year last revised as listed in the copyright header.
		int revised = ibmCopyright.getRevisionYear();

		// lasdMod = last touched by user. (e.g as defined 'default' in
		// options).
		int lastMod = revised;

		// Read user defined year from options.
		if (prefStore.getBoolean(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY) || adapter == null)
			lastMod = prefStore.getInt(RelEngCopyrightConstants.REVISION_YEAR_KEY);
		else {
			// figure out if the comment should be updated by comparing the date
			// range
			// in the comment to the last modification time provided by adapter
			if (lastMod < currentYear) {
				try {
					lastMod = adapter.getLastModifiedYear(file, monitor);
				} catch (CoreException e) {
					// Let's log the exception and continue
					RelEngPlugin.log(IStatus.ERROR,
							NLS.bind(Messages.getString("AdvancedFixCopyrightAction.17"), file.getFullPath()), e); //$NON-NLS-1$
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
		if (lastMod <= revised && (copyrightComment != null)
				&& (!prefStore.getBoolean(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY)))
			return;

		// either replace old copyright or put the new one at the top of the
		// file
		ibmCopyright.setRevisionYear(lastMod);
		if (copyrightComment == null) // do this on files without comments
			aSourceFile.insert(ibmCopyright.getCopyrightComment());
		else { // do this with files that have a copy-right comment already.

			// Verify that the comment is at the top of the file. Warn
			// otherwise.
			// [276257] XML file is a special case because it can have an
			// xml-header and other headers, thus it can start at an arbirary
			// position.
			if (!copyrightComment.atTop() && (aSourceFile.getFileType() != CopyrightComment.XML_COMMENT)) {
				warn(file, copyrightComment, Messages.getString("AdvancedFixCopyrightAction.19")); //$NON-NLS-1$
			}
			aSourceFile.replace(copyrightComment, ibmCopyright.getCopyrightComment());
		}
	}

	private boolean checkFileExtension(IFile file) {
		if (file.getFileExtension() == null) {
			warn(file, null, Messages.getString("AdvancedFixCopyrightAction.13")); //$NON-NLS-1$
			return false;
		} else {
			return true;
		}
	}

	private boolean checkSourceCreatedOk(SourceFile sourceFile, IFile file) {
		if (sourceFile == null) {
			// Warn if source creation failed.
			warn(file, null, Messages.getString("AdvancedFixCopyrightAction.20")); //$NON-NLS-1$
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Check if user chose to skip files of this kind.
	 *
	 * @param prefStore
	 *            Copyright preference store
	 * @param aSourceFile
	 *            Instance of the file to be checked.
	 * @return false if user wishes to skip over the file.
	 */
	private boolean checkUserFileIgnoreSettings(IPreferenceStore prefStore, SourceFile aSourceFile) {

		// -- Skip file if it's a property file and user chose to ignore
		// property files.
		if (aSourceFile.getFileType() == CopyrightComment.PROPERTIES_COMMENT
				&& prefStore.getBoolean(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY)) {
			return false;
		}
		// -- Skip over xml file if the user selected to skip xml files.
		if (aSourceFile.getFileType() == CopyrightComment.XML_COMMENT
				&& prefStore.getBoolean(RelEngCopyrightConstants.IGNORE_XML_KEY)) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the file has multiple copyright notices. Skip such files.
	 *
	 * @param file
	 * @param aSourceFile
	 * @return true if it has a single notice.
	 */
	private boolean checkMultipleCopyright(IFile file, SourceFile aSourceFile) {
		if (aSourceFile.hasMultipleCopyrights()) {
			warn(file, null, Messages.getString("AdvancedFixCopyrightAction.14")); //$NON-NLS-1$
			return false;
		} else {
			return true;
		}
	}

	private void warn(IFile file, BlockComment firstBlockComment, String errorDescription) {
		List<String> aList = log.get(errorDescription);
		if (aList == null) {
			aList = new ArrayList<>();
			log.put(errorDescription, aList);
		}
		aList.add(file.getFullPath().toString());
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}

}
