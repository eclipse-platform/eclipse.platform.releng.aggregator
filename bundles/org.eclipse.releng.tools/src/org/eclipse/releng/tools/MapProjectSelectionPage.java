/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.team.core.RepositoryProvider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;


public class MapProjectSelectionPage extends WizardPage {
	
	private MapProject selectedMapProject;
	private IDialogSettings settings;
	private ListViewer mapProjectListViewer;
	protected Button useDefaultProjectButton;
	protected boolean useDefaultMapProject;
	private final String SELECTED_PROJECT_KEY = "Selected Project"; //$NON-NLS-1$
	
	public MapProjectSelectionPage(String pageName, 
			String title, 
			IDialogSettings settings, 
			ImageDescriptor image) {
		super(pageName, title, image);
		this.settings = settings;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite topContainer = new Composite(parent, SWT.NONE);
		topContainer.setLayout(new GridLayout());
		topContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		mapProjectListViewer = createListViewer(topContainer); 
		mapProjectListViewer.setInput(getMapFileProjects());
		useDefaultProjectButton = new Button(topContainer, SWT.CHECK);
		useDefaultProjectButton.setText(Messages.getString("MapProjectSelectionPage.0")); //$NON-NLS-1$
		useDefaultProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultMapProject = (useDefaultProjectButton.getSelection());
				updateOthers();
			}
		});
	
        Dialog.applyDialogFont(parent);
        initializedViewer();
        setControl(topContainer);
	}

	protected ListViewer createListViewer(Composite parent) {
		List tree = new List(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= tree.getItemHeight() * 15;
		tree.setLayoutData(gd);
		ListViewer result = new ListViewer(tree);
		result.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				Set projects=(Set)inputElement;
				return ((IProject[]) projects.toArray(new IProject[projects.size()]));
			}
			public void dispose() {	
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		result.setLabelProvider(new WorkbenchLabelProvider());
		result.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {				
				updateOthers();
			}
		});
		result.setComparator((new ResourceComparator(ResourceComparator.NAME)));
		return result;
	}	

	private static Set getMapFileProjects() {
		Set projects = new HashSet();
		MapFile[] mapFiles;
		try {
			mapFiles = MapFile.findAllMapFiles(RelEngPlugin.getWorkspace().getRoot());
		} catch (CoreException ex) {
			return Collections.EMPTY_SET;
		}
		for (int i = 0; i < mapFiles.length; i++)
			projects.add(mapFiles[i].getFile().getProject());
		for (Iterator iterator= projects.iterator(); iterator.hasNext();) {
			MapProject mapProject= null;
			try {
				mapProject= new MapProject((IProject)iterator.next());
				if (mapProject.getValidMapFiles().length == 0)
					iterator.remove();
			} catch (CoreException e) {
				iterator.remove();
			} finally {
				if (mapProject != null)
					mapProject.dispose();
			}
			
		}
		return projects;
	}

	private void updateMapProject(){
		if (selectedMapProject != null) {
			selectedMapProject.dispose();	
			selectedMapProject = null;
		}

		IStructuredSelection selection = (IStructuredSelection)mapProjectListViewer.getSelection();
		if( !selection.isEmpty()){
			Object obj = selection.getFirstElement();
			if(obj instanceof IProject){
				try {
					selectedMapProject = new MapProject((IProject)obj);
				} catch (CoreException e) {
					selectedMapProject = null;
				}
			}
		}
		setPageComplete(isValid(selectedMapProject));
	}

	/*
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 * @since 3.7
	 */
	public void dispose() {
		if (selectedMapProject != null) {
			selectedMapProject.dispose();	
			selectedMapProject = null;
		}
		super.dispose();
	}

	private void initializedViewer(){
		readSettings();
		updateOthers();
	}
	
	protected void updateOthers(){
		updateMapProject();
	}
	
	private void readSettings(){
		String name = settings.get(SELECTED_PROJECT_KEY);
		if (name != null) {
			ISelection selection = new StructuredSelection(RelEngPlugin.getWorkspace().getRoot().getProject(name));
			mapProjectListViewer.setSelection(selection);
		}
		mapProjectListViewer.getList().setFocus();
	}
	
	public void saveSettings(){
		IStructuredSelection selection = (IStructuredSelection)mapProjectListViewer.getSelection();
		if(!selection.isEmpty()){
			Object obj = selection.getFirstElement();
			if(obj instanceof IProject){
				settings.put(SELECTED_PROJECT_KEY, ((IProject)obj).getName());			
			}
		}
	}
	
	private boolean isValid(final MapProject mapProject){
		//Check if map project is accessible
		if (mapProject==null || (!mapProject.getProject().isAccessible())){
			setErrorMessage(Messages.getString("MapProjectSelectionPage.1")); //$NON-NLS-1$
			return false;
		}

		//Check if the map project is shared
		if (RepositoryProvider.getProvider(mapProject.getProject()) == null){
			setErrorMessage(Messages.getString("MapProjectSelectionPage.2") + mapProject.getProject().getName()+Messages.getString("MapProjectSelectionPage.3"));	 //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		setErrorMessage(null);
		if (getWizard() instanceof ReleaseWizard)
			((ReleaseWizard)getWizard()).broadcastMapProjectChange(mapProject);
		return true;
	}
	
	//Used by MapProjectSelectionWizard for storing preferences
	protected MapProject getSelectedMapProject() {
		return selectedMapProject;
	}
	
	//Used by MapProjectSelectionWizard for storing preferences
	protected boolean useDefaultMapProject() {
		return useDefaultMapProject;
	}
}
