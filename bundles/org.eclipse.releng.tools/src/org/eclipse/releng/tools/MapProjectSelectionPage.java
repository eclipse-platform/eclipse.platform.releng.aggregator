/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.releng.tools;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class MapProjectSelectionPage extends WizardPage {

	private IDialogSettings settings;
	private MapProject mapProject;
	private TreeViewer projectTree;
	private Button useDefaultButton;
	private Button useOtherButton;
	private boolean useDefaults;
	private String DEFAULT_BUTTON_KEY;
	private String SELECTED_PROJECT_KEY;
	
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
				
		useDefaultButton = new Button(topContainer, SWT.RADIO);
		useDefaultButton.setText("Use default map project (org.eclipse.releng)"); 
		useDefaultButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaults = useDefaultButton.getSelection();
				updateOthers();
			}
		});
		
		useOtherButton = new Button(topContainer, SWT.RADIO);
		useOtherButton.setText("Specify the map project you want to use");
		useOtherButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaults = !useOtherButton.getSelection();
				updateOthers();
			}
		});
		
		projectTree = createTree(topContainer);
		projectTree.setInput(RelEngPlugin.getWorkspace().getRoot());
		Runnable refresh = new Runnable() {
			public void run() {
				getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						projectTree.refresh();
					}
				});
			}
		};

        Dialog.applyDialogFont(parent);
        initializedViewer();
        setControl(topContainer);
	}

	protected TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.SINGLE | SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= tree.getItemHeight() * 15;
		tree.setLayoutData(gd);
		TreeViewer result = new TreeViewer(tree);
		result.setContentProvider(new WorkbenchContentProvider());
		result.setLabelProvider(new WorkbenchLabelProvider());
		result.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {				
				updateOthers();
			}
		});
		result.setSorter(new ResourceSorter(ResourceSorter.NAME));
		return result;
	}	

	private void updateMapProject(){
		if(useDefaults){
			mapProject = MapProject.getDefaultMapProject();		
		}else{
			IStructuredSelection selection = (IStructuredSelection)projectTree.getSelection();
			if( !selection.isEmpty()){
				Object obj = selection.getFirstElement();
				if(obj instanceof IProject){
					try {
						mapProject = new MapProject((IProject)obj);
						if(mapProject.getValidMapFiles().length == 0){
							mapProject = null;
						}
					} catch (CoreException e) {
						mapProject = null;
					}
				}
			}else{
				mapProject = null;
			}
		}
		if(isValid(mapProject)){
			((ReleaseWizard)getWizard()).boadcastMapProjectChange(mapProject);
			setPageComplete( true);
		}else{
			setPageComplete(false);
		}

	}

	private void initializedViewer(){
		if(settings == null){
			useDefaults = true;
		}else{
			readSettings();
		}
		useDefaultButton.setSelection(useDefaults);
		useOtherButton.setSelection(!useDefaults);
		updateOthers();
	}
	
	private void updateOthers(){
		projectTree.getTree().setEnabled(!useDefaults);
		updateMapProject();
	}
	
	private void readSettings(){
		if(settings.get(DEFAULT_BUTTON_KEY) != null){
			useDefaults = settings.getBoolean(DEFAULT_BUTTON_KEY);
		}else{
			useDefaults = true;
		}
		String name = settings.get(SELECTED_PROJECT_KEY);
		if(!useDefaults && name != null){
			ISelection selection = new StructuredSelection(RelEngPlugin.getWorkspace().getRoot().getProject(name));
			projectTree.setSelection(selection);
			projectTree.getTree().setFocus();
		}
	}
	public void saveSettings(){
		settings.put(DEFAULT_BUTTON_KEY, useDefaults);
		IStructuredSelection selection = (IStructuredSelection)projectTree.getSelection();
		if(!selection.isEmpty()){
			Object obj = selection.getFirstElement();
			if(obj instanceof IProject){
				settings.put(SELECTED_PROJECT_KEY, ((IProject)obj).getName());			
			}
		}
	}
	
	private boolean isValid(final MapProject mapProject){
		//Check if map project is accessible
		if(mapProject==null || (!mapProject.getProject().isAccessible())){
			setErrorMessage("Invalid map project selected");
			return false;
		}

		//Check if the map project is shared
		if(RepositoryProvider.getProvider(mapProject.getProject()) == null){
			setErrorMessage("Project " + mapProject.getProject().getName()+" is not shared");	
			return false;
		}
		setErrorMessage(null);
		return true;
	}
}
