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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.internal.ui.synchronize.ChangeSetModelManager;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.part.PageBook;

/**
 *This class extends <code>WizardPage<code>. It utilizes a <code>PageBook<code> to show user
 *whether there are any changed projects or not since last release. It also shows a compare 
 *editor if there is any changed project detected  
 */
public class ProjectComparePage extends WizardPage{

	private PageBook pageBook;
	private Control compareView;
	private Label noneChangeMessage;
	private MapProject mapProject;
	private ISynchronizePageConfiguration configuration;
	private ParticipantPageSaveablePart input;

	public ProjectComparePage(String pageName, 
			String title, 
			ImageDescriptor image) {
		super(pageName, title, image);
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
		
		input = createCompareInput();
		input.createPartControl(pageBook);
		compareView = input.getControl();
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
								r = getOutOfSyncProjects(projects, tags, monitor);
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
	public void updateMapProject(MapProject m){
		mapProject = m;
	}
	
	private ParticipantPageSaveablePart createCompareInput() {	
		ISynchronizeParticipant participant = new CompareParticipant(new CVSCompareSubscriber(new IResource[0], new CVSTag[0], "RelEng Release"));
		configuration = participant.createPageConfiguration();
		configuration.setMenuGroups(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] { 
				ISynchronizePageConfiguration.NAVIGATE_GROUP,  
				ISynchronizePageConfiguration.LAYOUT_GROUP,
				ChangeSetModelManager.CHANGE_SET_GROUP});
		configuration.setMenuGroups(ISynchronizePageConfiguration.P_CONTEXT_MENU, new String[0]);
		
		CompareConfiguration cc = new CompareConfiguration();
		cc.setLeftEditable(false);
		cc.setRightEditable(false);
		ParticipantPageSaveablePart part = new ParticipantPageSaveablePart(getShell(), cc, configuration, participant);
		
		// will be reset when setVisible is called
		setPageComplete(true);
		
		return part;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		super.dispose();
		input.getParticipant().dispose();
		input.dispose();
	}
	
	/**
	 * Return the list of projects that are out-of-sync
	 */
	public IResource[] getOutOfSyncProjects(IProject[] projects, CVSTag[] tags, IProgressMonitor monitor) throws TeamException {
		CompareParticipant participant = (CompareParticipant)input.getParticipant();
		CVSCompareSubscriber subscriber = (CVSCompareSubscriber)participant.getSubscriber();
		subscriber.resetRoots(projects, tags);
		try {
			subscriber.primeRemoteTree();
		} catch (CVSException e) {
			// Log and ignore
			RelEngPlugin.log(e);
		}
		participant.refreshNow(projects, "", monitor);
		IResource[] r = participant.getSyncInfoSet().members(ResourcesPlugin.getWorkspace().getRoot());
		return r;
	}
}

