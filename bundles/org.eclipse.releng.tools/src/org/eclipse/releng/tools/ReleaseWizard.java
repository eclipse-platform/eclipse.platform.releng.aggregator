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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.dialogs.IPromptCondition;
import org.eclipse.team.internal.ui.dialogs.PromptingDialog;
import org.eclipse.team.ui.ISharedImages;


public class ReleaseWizard extends Wizard {

	private ProjectSelectionPage projectSelectionPage;
	private TagPage tagPage;
	private ProjectComparePage projectComparePage;
	private MapFileComparePage mapComparePage;
	private CommitCommentPage commentPage;

	private Dialog parentDialog;
	private IDialogSettings section;
	
	private MapProject mapProject;
	private IProject[] preSelectedProjects;
	private IProject[] selectedProjects;
	

	public ReleaseWizard(MapProject mProject) {
		setWindowTitle("Release"); //$NON-NLS-1$
		IDialogSettings settings = RelEngPlugin.getDefault().getDialogSettings();
		section = settings.getSection("ReleaseWizard");//$NON-NLS-1$
		if (section == null) {
			section = settings.addNewSection("ReleaseWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		mapProject = mProject;
	}	

	public boolean execute(Shell shell) {
		setNeedsProgressMonitor(true);
		WizardDialog dialog = new WizardDialog(shell, this);
		setParentDialog(dialog);
		return (dialog.open() == Window.OK);
	}
	
	/**
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		projectSelectionPage = new ProjectSelectionPage(Messages.getString("ReleaseWizard.5"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.6"), 
				section, 
				TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE),
				mapProject);
		projectSelectionPage.setDescription(Messages.getString("ReleaseWizard.7")); //$NON-NLS-1$
		addPage(projectSelectionPage);
		
		tagPage = new TagPage(Messages.getString("ReleaseWizard.8"), 
				Messages.getString("ReleaseWizard.9"), 
				section, 
				TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE)); //$NON-NLS-1$ //$NON-NLS-2$
		tagPage.setDescription(Messages.getString("ReleaseWizard.10")); //$NON-NLS-1$
		addPage(tagPage);
		
		projectComparePage = new ProjectComparePage(Messages.getString("ReleaseWizard.11"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.12"), 
				TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE),
				mapProject); //$NON-NLS-1$
		projectComparePage.setDescription(Messages.getString("ReleaseWizard.13")); //$NON-NLS-1$
		addPage(projectComparePage);
		
		mapComparePage = new MapFileComparePage(Messages.getString("ReleaseWizard.14"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.15"), 
				TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE)); //$NON-NLS-1$
		mapComparePage.setDescription(Messages.getString("ReleaseWizard.16")); //$NON-NLS-1$
		addPage(mapComparePage);
		
		commentPage = new CommitCommentPage(parentDialog, Messages.getString("ReleaseWizard.17"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.18"), TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_WIZBAN_SHARE), Messages.getString("ReleaseWizard.19")); //$NON-NLS-1$ //$NON-NLS-2$
		addPage(commentPage);
	}
	/**
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		if(!isProjectSelected())return false;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					CVSTag tag = new CVSTag(tagPage.getTagString(), CVSTag.VERSION);
					TagAndReleaseOperation operation = new TagAndReleaseOperation(getShell(), mapProject, 
							selectedProjects, tag,commentPage.getComment() );
					if (tagPage.isMoveButtonSelected()) {
						operation.moveTag();
					}
					monitor.beginTask("Releasing", 100);
					operation.run(new SubProgressMonitor(monitor, 90));
					try {						
						if(tagPage.isValidateButtonSelected()){
							validateRelease(new SubProgressMonitor(monitor, 10));
						}
					} finally {
						monitor.done();
					}
				}
			});
			projectSelectionPage.saveSettings();
			tagPage.saveSettings();
			return true;
		} catch (InterruptedException e) {
			// Cancelled. Ignore and close dialog
			return true;
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		}
		return false;
	}
	
	public void setParentDialog(Dialog p) {
		this.parentDialog = p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == projectSelectionPage) {
			IProject[] projects = projectSelectionPage.getCheckedProjects();
			if (projects != null && projects.length > 0){
				selectedProjects = projects;
			}

			if (projectSelectionPage.isCompareButtonChecked()){
				return projectComparePage;
			}			
			else
				return tagPage;
		}
		if (page == tagPage) {
			if (tagPage.compareButtonSelected()){
				mapComparePage.setTag(tagPage.getTagString());
				return mapComparePage;
			}
			if (tagPage.commitButtonSelected())
				return commentPage;
		}
		if (page == mapComparePage)
			return commentPage;
		if (page == projectComparePage)
			return tagPage;
		return null;
	}

	public void setPreSelectedProjects(IResource[] resources) {
		if (resources.length < 1) {
			preSelectedProjects = null;
		} else {
			Set list = new HashSet();
			for (int i = 0; i < resources.length; i++) {
				list.add(resources[i].getProject());
			}
			preSelectedProjects = (IProject[]) list.toArray(new IProject[list.size()]);
		}
	}

	// Prompt for any uncommitted changes
	protected IProject[] performPrompting(IProject[] projects)  {
		IResource[] resources;
		PromptingDialog prompt = new PromptingDialog(getShell(), projects,
			getPromptCondition(projects), Policy.bind("TagAction.uncommittedChangesTitle"));//$NON-NLS-1$
		try {
			 resources = prompt.promptForMultiple();
		} catch(InterruptedException e) {
			return null;
		}
		if(resources.length == 0) {
			return null;					
		}
		projects = new IProject[resources.length ];
		for(int i = 0; i < resources.length; i++){
			if(resources[i] instanceof IProject)
				projects[i] = (IProject)resources[i];
		}
		return projects;
	}
	

	protected IPromptCondition getPromptCondition(IResource[] resource) {
		return new IPromptCondition() {
			public boolean needsPrompt(IResource resource) {
				return CVSLightweightDecorator.isDirty(resource);
			}
			public String promptMessage(IResource resource) {
				return Policy.bind("TagAction.uncommittedChanges", resource.getName());//$NON-NLS-1$
			}
		};
	}

	public IProject[] getSelectedProjects(){
		return selectedProjects;
	}

	//the update will happen when (1)from project selection page to compare project page or (2)from
	//project selection page to Enter Tag page. It calls shouldRemove() to determine the projects to keep
	public void updateSelectedProject(){
		selectedProjects = projectSelectionPage.getCheckedProjects();
		selectedProjects = performPrompting(selectedProjects);
		projectSelectionPage.setSelection(selectedProjects);
	}
	
	public void setSelectedProjects(IResource[] projects){
		if(projects == null) selectedProjects = null;
		else {
			selectedProjects =  new IProject[projects.length];
			for(int i = 0; i < projects.length; i++){
				selectedProjects[i] = (IProject)projects[i];
			}
		}
	}
	protected ProjectSelectionPage getProjectSelectionPage(){
		return projectSelectionPage;
	}
	
	//This method is called if validate button in TagPage is checked
	private void validateRelease(IProgressMonitor  monitor){
		ProjectValidationDialog.validateRelease(selectedProjects, mapProject.getTagsFor(selectedProjects), monitor);
	}


	private boolean isProjectSelected(){
		if (selectedProjects == null || selectedProjects.length == 0){
			return false;
		}
		return true;
	}
	
	public IProject[] getPreSelectedProjects(){
		return preSelectedProjects;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	public boolean canFinish() {
		IWizardPage currentPage = getContainer().getCurrentPage();
		if(currentPage == tagPage){
			if(tagPage.compareButtonSelected()){
				return false;
			}
		}
		return super.canFinish();
	}
}
