/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.releng.tools.MapFile;
import org.eclipse.releng.tools.MapProject;
import org.eclipse.releng.tools.Messages;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.team.internal.ui.SWTUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class MapProjectPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	//used as key in the preference store
	public static final String USE_DEFAULT_MAP_PROJECT = "useDefaultMapProject"; //$NON-NLS-1$
	public static final String SELECTED_MAP_PROJECT_PATH = "defaultMapProject"; //$NON-NLS-1$

	//when this box is checked, the user will be prompted to selected a MapProject
	//when using tools that require the use of MapProjects
	private Button alwaysPromptButton;
	private List projectList;
	private Label mapProjectSelectionDescription;
	private IProject[] workspaceMapProjects;
	private IPreferenceStore preferenceStore;

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();

		Composite topContainer = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		topContainer.setLayout(layout);
		topContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		Label pageDescription = SWTUtils.createLabel(topContainer, Messages.getString("MapProjectPreferencePage.1")); //$NON-NLS-1$
		pageDescription.setLayoutData(data);

		//radio button to toggle remembering the selected map project
		alwaysPromptButton = new Button(topContainer, SWT.CHECK);
		alwaysPromptButton.setText(Messages.getString("MapProjectPreferencePage.0")); //$NON-NLS-1$
		alwaysPromptButton.setLayoutData(data);

		SelectionAdapter selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				update();
			}
		};
		alwaysPromptButton.addSelectionListener(selectionListener);

		mapProjectSelectionDescription = SWTUtils.createLabel(topContainer, Messages.getString("MapProjectPreferencePage.2")); //$NON-NLS-1$
		createMapProjectList(topContainer);
		initialButtonSetup();
		//TODO: Update this
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EXT_PREFERENCE_PAGE);
		Dialog.applyDialogFont(parent);
		return topContainer;
	}

	/*
	 * Updates any interested parties when the alwaysPromptButton is either checked or
	 * unchecked.
	 */
	protected void update() {
		projectList.setEnabled((!alwaysPromptButton.getSelection()));
		if (projectList.isEnabled()) {
			projectSelected();
			projectList.setFocus();
		} else {
			setValid(alwaysPromptButton.getSelection());
			setErrorMessage(null);
		}
		mapProjectSelectionDescription.setEnabled((!alwaysPromptButton.getSelection()));
	}

	/**
	 * Populates a List of available projects in the current workspace.
	 * @param aComposite The Composite to which the List being populated will be attached.
	 */
	private void createMapProjectList(Composite aComposite) {
		projectList = new List(aComposite, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData();
		//Set heightHint with a small value so the list size will be defined by 
		//the space available in the dialog instead of resizing the dialog to
		//fit all the items in the list.
		data.heightHint = projectList.getItemHeight();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		projectList.setLayoutData(data);

		Set projects = new HashSet();
		MapFile[] mapFiles;
		try {
			mapFiles = MapFile.findAllMapFiles(RelEngPlugin.getWorkspace().getRoot());
		} catch (CoreException ex) {
			mapFiles = new MapFile[0];
		}
		for (int i = 0; i < mapFiles.length; i++)
			projects.add(mapFiles[i].getFile().getProject());
		workspaceMapProjects = ((IProject[]) projects.toArray(new IProject[projects.size()]));

		String[] projectNames = new String[workspaceMapProjects.length];
		for (int i = 0; i < workspaceMapProjects.length; i++) {
			projectNames[i] = workspaceMapProjects[i].getName();
		}
		projectList.setItems(projectNames);
		projectList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				projectSelected();
			}
		});
	}

	//The handler method for a selection in the List
	protected void projectSelected() {
		int selectedIndex = projectList.getSelectionIndex();
		if (selectedIndex == -1) {
			setValid(false);
			return;
		}
		IProject selectedProject = workspaceMapProjects[selectedIndex];
		boolean isValid = false;
		MapProject mapProject = null;
		try {
			mapProject = new MapProject(selectedProject);
			isValid = true;
			if (mapProject.getValidMapFiles().length == 0)
				setMessage(Messages.getString("MapProjectPreferencePage.3"), WARNING); //$NON-NLS-1$
		} catch (CoreException e) {
			isValid = false;
		} finally {
			if (mapProject != null)
				mapProject.dispose();
		}
		setValid(isValid);
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// do nothing
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		preferenceStore.setValue(USE_DEFAULT_MAP_PROJECT, !(alwaysPromptButton.getSelection()));
		if (projectList.getSelectionIndex() != -1) {
			String fullPath = workspaceMapProjects[projectList.getSelectionIndex()].getFullPath().toString();
			preferenceStore.setValue(SELECTED_MAP_PROJECT_PATH, fullPath);
		}
		return super.performOk();
	}

	/* 
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		alwaysPromptButton.setSelection(true);
		update();
		super.performDefaults();
	}

	/*
	 * @see PreferencePage#doGetPreferenceStore()
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return preferenceStore;
	}

	//Initializes the widgets to reflect the user's current map project settings
	private void initialButtonSetup() {
		//if there is an illegitimate default MapProject set, prompt for a new one
		int selectedMapProjectPathLength = preferenceStore.getString(SELECTED_MAP_PROJECT_PATH).length();
		if (!(selectedMapProjectPathLength > 0))
			preferenceStore.setValue(USE_DEFAULT_MAP_PROJECT, false);

		//if there is a legitimate default MapProject, highlight it
		if ((preferenceStore.getBoolean(USE_DEFAULT_MAP_PROJECT)) && (selectedMapProjectPathLength > 0))
			highlightDefaultMapProject();

		alwaysPromptButton.setSelection(!(preferenceStore.getBoolean(USE_DEFAULT_MAP_PROJECT)));
		update();
	}

	private void highlightDefaultMapProject() {
		String path = preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH);
		boolean isMapProjectSelected = false;
		if (path.length() > 0) {
			MapProject selectedMapProject= null;
			try {
				selectedMapProject = new MapProject(ResourcesPlugin.getWorkspace().getRoot().getProject(path));
				isMapProjectSelected = true;
			} catch (CoreException e) {
				isMapProjectSelected = false;
			} finally {
				if (selectedMapProject != null)
					selectedMapProject.dispose();
			}
		}

		if (!isMapProjectSelected) {
			alwaysPromptButton.setEnabled(true);
			preferenceStore.setValue(USE_DEFAULT_MAP_PROJECT, false);
		} else {
			//highlight the current default map project if applicable
			for (int i = 0; i < workspaceMapProjects.length; i++) {
				if (path.equals(workspaceMapProjects[i].getFullPath().toString())) {
					projectList.setSelection(i);
					projectSelected();
					return;
				}
			}
		}
	}

}
