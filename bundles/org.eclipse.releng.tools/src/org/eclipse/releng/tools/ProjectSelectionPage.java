/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;


/**
 * This class extends <code>WizardPage<code> class and use a <code>CheckboxTreeViewer<code> to 
 * display selectable items. 
 */
public class ProjectSelectionPage extends WizardPage {
	private CheckboxTreeViewer viewer;
	private IDialogSettings settings;
	private Button compareButton;
	private boolean compareButtonChecked;	
	private String SELECTED_ITEMS_KEY = Messages.getString("ProjectSelectionPage.0"); //$NON-NLS-1$
	private String COMPARE_BUTTON_KEY = Messages.getString("ProjectSelectionPage.1"); //$NON-NLS-1$
	private MapProject mapProject;

	private class MapFileLabelProvider extends LabelProvider {
		WorkbenchLabelProvider provider = new WorkbenchLabelProvider();
		public String getText(Object element) {
			if (element instanceof MapFile) {
				return ((MapFile)element).getName();
			}
			return provider.getText(element);
		}
		public Image getImage(Object element) {
			if (element instanceof MapFile) {
				return provider.getImage(((MapFile)element).getFile());
			}
			return provider.getImage(element);
		}
		public void dispose() {
			provider.dispose();
			super.dispose();
		}
	}
	
	public ProjectSelectionPage(String pageName, 
			String title, 
			IDialogSettings settings, 
			ImageDescriptor image) {
		super(pageName, title, image);
		this.settings = settings;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite topContainer = new Composite(parent, SWT.NONE);
		topContainer.setLayout(new GridLayout());
		topContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(topContainer, SWT.HORIZONTAL);
		label.setFont(font);
		label.setText(Messages.getString("ProjectSelectionPage.2")); //$NON-NLS-1$
		
		viewer = new ContainerCheckedTreeViewer(topContainer, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= viewer.getTree().getItemHeight() * 15;
		viewer.getTree().setLayoutData(gd);
		viewer.getTree().setFont(font);
		viewer.setLabelProvider(new MapFileLabelProvider());
		viewer.setContentProvider(getContentProvider());
		viewer.setComparator(new ResourceComparator(ResourceComparator.NAME) {
			public int compare(Viewer viewer, Object o1, Object o2) {
				if (o1 instanceof MapFile && o2 instanceof MapFile) {
					return super.compare(viewer, ((MapFile) o1).getFile(), ((MapFile) o2).getFile()); 
				}
				return super.compare(viewer, o1, o2);
			}});
		viewer.setInput(mapProject);
		viewer.expandAll();
		viewer.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				updatePageComplete();				
			}
		});
		
		compareButton = new Button(topContainer,SWT.CHECK);
		compareButton.setText(Messages.getString("ProjectSelectionPage.3")); //$NON-NLS-1$
		compareButton.setFont(font);
		compareButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				compareButtonChecked = compareButton.getSelection();
			}
		});
		
		initialize();
		setControl(topContainer);
	}
	/**
	 * Returns the content provider for the viewer
	 */
	private IContentProvider getContentProvider() {
		return new WorkbenchContentProvider() {
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof MapProject) {
					return mapProject.getValidMapFiles();
				}
				if (parentElement instanceof MapFile) {
					return ((MapFile)parentElement).getAccessibleProjects();
				}
				return null;
			}
			/*
			 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#getParent(java.lang.Object)
			 * @since 3.7
			 */
			public Object getParent(Object element) {
				if (mapProject == null)
					return null;

				if (element instanceof MapFile) {
					return mapProject;
				}
				if (element instanceof IProject) {
					MapFile[] mapFiles= mapProject.getValidMapFiles();
					for (int i= 0; i < mapFiles.length; i++) {
						if (mapFiles[i].contains((IProject)element))
							return mapFiles[i];
					}
				}
				return super.getParent(element);
			}
			public boolean hasChildren(Object element) {
				if (element instanceof MapFile) {
					return ((MapFile)element).getAccessibleProjects().length > 0;
				}
				return false;
			}
		};
	}

	/**
	 * Returns all the checked items if they are IProject 
	 */
	public IProject[] getCheckedProjects(){
		ArrayList projectsToRelease = new ArrayList();
		Object[] obj = viewer.getCheckedElements();
		if (obj == null)return null;
		for(int i = 0; i < obj.length; i++){
			if (obj[i] instanceof IProject)
				projectsToRelease.add(obj[i]); 
		}
		return (IProject[])projectsToRelease.toArray(new IProject[projectsToRelease.size()]);
	}	
	
	private void readProjectSettings(){
		if( settings == null) return;
		if(settings.getArray(SELECTED_ITEMS_KEY) != null){
			ArrayList nameList = new ArrayList(Arrays.asList(settings.getArray(SELECTED_ITEMS_KEY)));
			if(nameList != null){
				Iterator iter = nameList.iterator();
				while(iter.hasNext()){
					String name = (String)iter.next();
					IProject project = getProjectWithName(name);
					if(project != null){
						viewer.setChecked(project,true);
					}					
				}
			}
		}
	}
	
	private void initCompareEnablement(){
		if( settings == null || settings.get(COMPARE_BUTTON_KEY) == null) {
			compareButton.setSelection( true);
			compareButtonChecked = true;
			return;
		}else{
			boolean b = settings.getBoolean(COMPARE_BUTTON_KEY);
			compareButton.setSelection(b);
			compareButtonChecked = b;
		}
	}
	
	/**
	 * Save the checked items and the checkbox options to dialog settings
	 */
	public void saveSettings(){
		Object[] obj = viewer.getCheckedElements();
		ArrayList names = new ArrayList();
		for (int i = 0; i < obj.length; i++){
			if(obj[i] instanceof IProject){
				names.add(((IProject)obj[i]).getName());
			}
		}
		settings.put(SELECTED_ITEMS_KEY, (String[])names.toArray(new String[names.size()]));
		settings.put(COMPARE_BUTTON_KEY, compareButtonChecked);
	}

	/*
	 * @see org.eclipse.jface.wizard.WizardPage#setPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 * @since 3.7
	 */
	public void setPreviousPage(IWizardPage page) {
		super.setPreviousPage(page);
		updatePageComplete();
	}
	
	private void initialize(){
		initCheckedProjects();
		initCompareEnablement();
		updatePageComplete();
	}
	
	private void initCheckedProjects(){
		IProject[] p = ((ReleaseWizard)getWizard()).getPreSelectedProjects( );
		if(p != null){
			viewer.setCheckedElements(p);
		}else{
			readProjectSettings();
		}
	}

	/**
	 * Called by <code>readSettings()<code> to return the project associated with the given name
	 */
	private IProject getProjectWithName(String name){
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if(project.exists() && project.isAccessible())
			return project;
		return null;
	}

	public boolean isCompareButtonChecked(){
		return compareButtonChecked;
	}
	
	/**
	 *This page will not complete until at least one project is checked
	 */
	private void updatePageComplete(){
		Object[] obj = viewer.getCheckedElements();
		if(obj.length > 0){
			for(int i = 0; i < obj.length; i++){
				//Exclude the situation that an empty shown map file is selected
				if(obj[i] instanceof IProject){
					setPageComplete(true);
					break;
				}
			}
		}
		else{
			setPageComplete(false);
		}
	}
	
	private CheckboxTreeViewer getViewer(){
		return viewer;
	}
	
	public void setSelection(IProject[] projects) {
		if(projects != null && projects.length > 0){
			getViewer().setCheckedElements(projects);	
		}
	}
	public void updateMapProject(MapProject m){
		mapProject = m;
		if(viewer != null){
			Object[] checkedElements= null;
			if (m != null && mapProject != null && m.getProject().equals(mapProject.getProject()))
				checkedElements= viewer.getCheckedElements();
			viewer.setInput(mapProject);
			viewer.expandAll();
			if (checkedElements != null)
				viewer.setCheckedElements(checkedElements);
		}
	}
}
