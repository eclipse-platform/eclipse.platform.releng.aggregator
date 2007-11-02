/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class MapProjectSelectionPage extends WizardPage {
	
	private MapProject selectedMapProject;
	private IDialogSettings settings;
	private TreeViewer projectTree;
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
		
		projectTree = createTree(topContainer); 
		projectTree.setInput(RelEngPlugin.getWorkspace().getRoot());
		
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
		result.setComparator((new ResourceComparator(ResourceComparator.NAME)));
		return result;
	}	

	private void updateMapProject(){
		selectedMapProject = null;	
		
		IStructuredSelection selection = (IStructuredSelection)projectTree.getSelection();
		if( !selection.isEmpty()){
			Object obj = selection.getFirstElement();
			if(obj instanceof IProject){
				try {
					selectedMapProject = new MapProject((IProject)obj);
					if(selectedMapProject.getValidMapFiles().length == 0){
						selectedMapProject = null;
					}
				} catch (CoreException e) {
					selectedMapProject = null;
				}
			}
		}
		setPageComplete(isValid(selectedMapProject));
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
			projectTree.setSelection(selection);
		}
		projectTree.getTree().setFocus();
	}
	
	public void saveSettings(){
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
