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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.internal.resources.WorkspaceRoot;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class BuildNotesPage extends WizardPage {

	private String FILE_PATH_KEY = "BuildNotesPage.filePath";
	private String UPDATE_FILE_KEY = "BuildNotesPage.updateNotesButton";
	private Button updateNotesButton;
	private boolean updateNotesButtonChecked;
	private SyncInfoSet syncInfoSet;
	private IDialogSettings settings;
	private Text text;
	private Map map;
	private boolean validPath;
	private Text filePath;
	private Button browse;
	private IFile iFile;

	protected BuildNotesPage(String pageName, String title,
			IDialogSettings settings, ImageDescriptor image) {
		super(pageName, title, image);
		this.settings = settings;
	}

	public void createControl(Composite parent) {
		GridData data = new GridData(GridData.FILL_BOTH);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		composite.setLayoutData(data);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		updateNotesButton = new Button(composite, SWT.CHECK);
		updateNotesButton.setText("Update Build Notes File");
		updateNotesButton.setLayoutData(data);
		updateNotesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateNotesButtonChecked = updateNotesButton.getSelection();
				if (updateNotesButtonChecked) {
					filePath.setEnabled(true);
					filePath.setText(filePath.getText());
					browse.setEnabled(true);
				} else {
					filePath.setEnabled(false);
					setErrorMessage(null);
					browse.setEnabled(false);
				}
				updateButtons();
			}
		});

		Label label = new Label(composite, SWT.LEFT);
		label.setText("Build Notes File:");

		data = new GridData(GridData.FILL_HORIZONTAL);
		filePath = new Text(composite, SWT.BORDER);
		filePath.setLayoutData(data);
		filePath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Path path = new Path(filePath.getText());
				validPath = false;
				if (!path.isEmpty()) {
					IFile file = ((WorkspaceRoot) ResourcesPlugin
							.getWorkspace().getRoot()).getFile(path);
					if (path.isValidPath(filePath.getText())
							&& file.getParent().exists()) {
						if (path.getFileExtension().equals("html")) {
							setErrorMessage(null);
							validPath = true;
							iFile = file;
						} else {
							setErrorMessage("Invalid file extension.");
						}
					} else {
						setErrorMessage("Invalid path.");
					}
				} else {
					// path is empty
					setErrorMessage("Input a file path.");
				}
				updateButtons();
			}
		});

		browse = new Button(composite, SWT.PUSH);
		browse.setText("Browse");
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IResource iResource = buildNotesFileDialog();
				if (iResource instanceof IFile) {
					IFile iFile = (IFile) iResource;
					filePath.setText(iFile.getFullPath().toString());
				} else if (iResource instanceof IFolder) {
					IFolder iFolder = (IFolder) iResource;
					filePath.setText(iFolder.getFullPath().toString()
							+ "/build_notes.html");
				} else if (iResource instanceof IProject) {
					IProject iProject = (IProject) iResource;
					filePath.setText(iProject.getFullPath().toString()
							+ "/build_notes.html");
				}
			}
		});

		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		text = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		text.setLayoutData(data);
	
		initialize();
		setControl(composite);
	}

	private void initialize() {
		initSelections();		
	}

	/*
	 * initialize the controls on the page
	 */
	private void initSelections() {
		if( settings == null || settings.get(UPDATE_FILE_KEY) == null || settings.get(FILE_PATH_KEY) == null) {
			updateNotesButton.setSelection(false);
			updateNotesButtonChecked = false;
			browse.setEnabled(false);
			filePath.setEnabled(false);
			return;
		}else{
			boolean b = settings.getBoolean(UPDATE_FILE_KEY);
			updateNotesButton.setSelection(b);
			updateNotesButtonChecked = b;
			filePath.setText(settings.get(FILE_PATH_KEY));
			browse.setEnabled(true);
			filePath.setEnabled(true);
		}		
	}

	/*
	 * enable or disable wizard buttons
	 */
	public void updateButtons() {
		if (isUpdateNotesButtonChecked() && !getValidPath()) {
			setPageComplete(false);
		} else {
			setPageComplete(true);
		}
	}

	/*
	 * if file doesn't already exist, prepare new one
	 * otherwise call method to write to existing file
	 */
	public void updateNotesFile() {
		if (!filePath.isDisposed()) {
			Path path = new Path(filePath.getText());
			WorkspaceRoot root = (WorkspaceRoot) ResourcesPlugin.getWorkspace()
					.getRoot();
			final IFile file = root.getFile(path);
			if (file.exists()) {
				writeUpdate(file);
			} else {
				if (file.getParent().exists()) {
					try {
						getContainer().run(true, true,
								new IRunnableWithProgress() {
									public void run(IProgressMonitor monitor)
											throws InvocationTargetException,
											InterruptedException {
										monitor.beginTask("Creating file...",
												100);
										StringBuffer buffer = new StringBuffer();
										buffer
												.append("<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n");
										buffer.append("<html>\n\n");
										buffer.append("<head>\n");
										buffer
												.append("   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n");
										buffer
												.append("   <meta name=\"Build\" content=\"Build\">\n");
										buffer
												.append("   <title>Eclipse Platform Release Notes (3.2) - JFace and Workbench</title>\n");
										buffer.append("</head>\n\n");
										buffer.append("<body>\n\n");
										buffer
												.append("<h1>Eclipse Platform Build Notes (3.2)<br>\n");
										buffer
												.append("JFace and Workbench</h1>");

										ByteArrayInputStream c = new ByteArrayInputStream(
												buffer.toString().getBytes());
										try {
											file.create(c, true, monitor);
										} catch (CoreException e) {
											e.printStackTrace();
										}

										try {
											c.close();
										} catch (IOException e) {
											e.printStackTrace();
										}
										monitor.done();
									}
								});
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					writeUpdate(file);
				}
			}
		}
	}

	/*
	 * write update to build notes file
	 */
	public void writeUpdate(final IFile file) {
		BufferedInputStream originalContents = null;
		try {
			originalContents = new BufferedInputStream(file.getContents());
			final StringBuffer buffer = new StringBuffer();

			int character;
			while ((character = originalContents.read()) != -1) {
				buffer.append((char) character);
			}

			String marker = "</h1>";
			SimpleDateFormat formatter = new SimpleDateFormat(
					"MMMM dd, yyyy, h:mm a");
			Date currentTime = new Date();
			String dateString = formatter.format(currentTime);
			dateString = dateString.replaceAll("AM", "a.m.");
			dateString = dateString.replaceAll("PM", "p.m.");

			int index = buffer.indexOf(marker) + marker.length();
			if (index != -1) {
				StringBuffer insertBuffer = new StringBuffer();
				insertBuffer.append("\n<p>Integration Build (" + dateString
						+ ")</p>\n");
				insertBuffer.append("  <p>Problem reports updated</p>\n");
				insertBuffer.append("  <p>\n");

				Iterator i = map.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry entry = (Map.Entry) i.next();
					Integer bug = (Integer) entry.getKey();
					String summary = (String) entry.getValue();
					insertBuffer
							.append("<a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id=");
					insertBuffer.append(bug);
					insertBuffer.append("\">Bug ");
					insertBuffer.append(bug);
					insertBuffer.append("</a>. ");
					insertBuffer.append(summary + "<br>\n");
				}
				insertBuffer.append("  </p>");
				buffer.insert(index, "\n" + insertBuffer.toString());

				try {
					getContainer().run(true, true, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask("Updating File...", 100);
							ByteArrayInputStream c = new ByteArrayInputStream(
									buffer.toString().getBytes());
							try {
								file.setContents(c, true, true, monitor);
								c.close();
							} catch (CoreException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							monitor.done();
						}
					});
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			text.setText("");
			GetBugsOperation getBugsOperation = new GetBugsOperation(
					(ReleaseWizard) getWizard(), syncInfoSet);
			getBugsOperation.run(this);
			String tempText = outputReport();
			if(tempText != null) {
				text.setText(tempText);
			}
			System.out.println(text.getText());
			System.out.println();
			System.out.println();
		}
	}

	public IResource buildNotesFileDialog() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());

		// filter for .html files only and exclude bin folders
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IFile) {
					IFile file = (IFile) element;
					IPath path = file.getFullPath();
					if (path.getFileExtension().equals("html")) {
						return true;
					}
				} else if (element instanceof IFolder) {
					IFolder folder = (IFolder) element;
					if (folder.getName().equals("bin")) {
						return false;
					}
					return true;
				} else if (element instanceof IProject) {
					return true;
				}
				return false;
			}
		});

		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setAllowMultiple(false);
		dialog.setTitle("Select Build Notes File");
		dialog
				.setMessage("Select the file to update with the new build notes.");
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			if (elements != null && elements.length > 0) {
				if (elements[0] instanceof IFile) {
					IFile iFile = (IFile) elements[0];
					return iFile;
				} else if (elements[0] instanceof IFolder) {
					IFolder iFolder = (IFolder) elements[0];
					return iFolder;
				} else if (elements[0] instanceof IProject) {
					IProject iProject = (IProject) elements[0];
					return iProject;
				}
			}
		}
		return null;
	}
	
	public boolean isUpdateNotesButtonChecked() {
		return updateNotesButtonChecked;
	}

	public void setSyncInfoSet(SyncInfoSet syncInfoSet) {
		this.syncInfoSet = syncInfoSet;
	}

	/**
	 *  return string of report
	 */
	public String outputReport() {
		StringBuffer buffer = new StringBuffer();
		if (map.size() < 1) {
			buffer.append("The map file has been updated.\n");
		} else {
			buffer
					.append("The map file has been updated for the following fixes:\n");
			Iterator i = map.entrySet().iterator();

			while (i.hasNext()) {
				Map.Entry entry = (Map.Entry) i.next();
				Integer bug = (Integer) entry.getKey();
				String summary = (String) entry.getValue();
				buffer.append("+ Bug " + bug + ". " + summary + "\n");
			}
		}
		buffer.append("\nThe following projects have changed:\n");
		IProject[] iProjects = ((ReleaseWizard) getWizard())
				.getSelectedProjects();
		for (int j = 0; j < iProjects.length; j++) {
			buffer.append(iProjects[j].getName() + "\n");
		}
		return buffer.toString();
	}

	public void setMap(Map map) {
		this.map = map;
	}

	public boolean getValidPath() {
		return validPath;
	}

	public IFile getIFile() {
		return iFile;
	}

	public void saveSettings() {
		settings.put(UPDATE_FILE_KEY, updateNotesButtonChecked);
		settings.put(FILE_PATH_KEY, filePath.getText());
	}
}
