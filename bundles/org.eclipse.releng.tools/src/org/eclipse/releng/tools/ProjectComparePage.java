/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;
import org.eclipse.ui.part.PageBook;

/**
 *This class extends <code>WizardPage<code>. It utilizes a <code>PageBook<code> to show user
 *whether there are any changed projects or not since last release. It also shows a compare 
 *editor if there is any changed project detected  
 */
public class ProjectComparePage extends WizardPage{

	private ProjectComparePageTreeInput compareEditorInput;
	private PageBook pageBook;
	private Control compareView;
	private Label noneChangeMessage;
	private MapProject mapProject;

	public ProjectComparePage(String pageName, 
			String title, 
			ImageDescriptor image,
			MapProject mProject) {
		super(pageName, title, image);
		mapProject = mProject;

	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		GridData data = new GridData(GridData.FILL_BOTH);
		
		Composite composite = new Composite(parent, SWT.NONE); 
		composite.setLayout(new GridLayout());
		composite.setLayoutData(data);	
		composite.setFont(font);
		
		pageBook = new PageBook(composite, SWT.NONE);
		composite.setLayout(new GridLayout());
		pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));
		pageBook.setFont(font);
					
		CompareConfiguration compareConfig = new CompareConfiguration();
		SyncInfoTree set = new SyncInfoTree();
		TreeViewerAdvisor viewerAdvisor = new TreeViewerAdvisor(null, null, set);
		compareEditorInput = new ProjectComparePageTreeInput(compareConfig, viewerAdvisor){
			public String getTitle() {
				return Messages.getString("ProjectComparePage.0"); //$NON-NLS-1$
			}
		};
		try {
			// Preparing the input should be fast since we haven't started the collector
			compareEditorInput.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		}
		compareView = compareEditorInput.createContents(pageBook);
		compareView.setFont(font);
		compareView.setLayoutData(data);
		
		noneChangeMessage = new Label(pageBook,SWT.WRAP);
		noneChangeMessage.setText(Messages.getString("ProjectComparePage.1")); //$NON-NLS-1$
		noneChangeMessage.setLayoutData(data);
		noneChangeMessage.setFont(font);
		
		setControl(composite);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			final ReleaseWizard wizard = (ReleaseWizard)getWizard();
			try {
				//Get updated selected project in case that some projects have outgoing changes and excluded by user
				wizard.updateSelectedProject();
				
				//Get the finalized selected projects and the associated tags from existing map files
				final IProject[] projects = wizard.getSelectedProjects();
				final CVSTag[] tags = mapProject.getTagsFor(projects);			
				
				//Collect all the out-of-sync projects from selected projects and update the selected project again 
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException, InterruptedException {
						IResource[] r = null;
						if(projects != null && projects.length != 0){
							try {
								r = compareEditorInput.getOutOfSyncProjects(projects, tags, monitor);
								wizard.setSelectedProjects(r);
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							}						
						}
					}
				});
				
				//Show no-project-changed information on the page book if condition satisfied
				if( wizard.getSelectedProjects()== null||wizard.getSelectedProjects().length == 0){
					setPageComplete(false);
					pageBook.showPage(noneChangeMessage);
				}
				//Open a compare editor otherwise
				else{
					setPageComplete(true);
					pageBook.showPage(compareView);
				}
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
			} catch (InterruptedException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
			}
		}
		//When the page is invisible, it should not affect the wizard work flow by anyway
		else {
			setPageComplete(true);
		}
	}
	
	
}

