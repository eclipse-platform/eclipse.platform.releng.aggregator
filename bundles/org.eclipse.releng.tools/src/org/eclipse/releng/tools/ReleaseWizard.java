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

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSLightweightDecorator;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.dialogs.IPromptCondition;
import org.eclipse.team.internal.ui.dialogs.PromptingDialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;


public class ReleaseWizard extends Wizard {

	// Dialog store constants
	private static final String BOUNDS_HEIGHT= "bounds.height"; //$NON-NLS-1$
	private static final String BOUNDS_WIDTH= "bounds.width"; //$NON-NLS-1$
	private static final String BOUNDS_Y= "bounds.y"; //$NON-NLS-1$
	private static final String BOUNDS_X= "bounds.x"; //$NON-NLS-1$
	
	private MapProjectSelectionPage mapSelectionPage;
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
	

	public ReleaseWizard() {
		setWindowTitle("Release"); //$NON-NLS-1$
		IDialogSettings settings = RelEngPlugin.getDefault().getDialogSettings();
		section = settings.getSection("ReleaseWizard");//$NON-NLS-1$
		if (section == null) {
			section = settings.addNewSection("ReleaseWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
	}
	
	/*
	 * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 * @since 3.1
	 */
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		if (getDialogSettings().get(BOUNDS_X) != null) {
			int x= getDialogSettings().getInt(BOUNDS_X);
			int y= getDialogSettings().getInt(BOUNDS_Y);
			int width= getDialogSettings().getInt(BOUNDS_WIDTH);
			int height= getDialogSettings().getInt(BOUNDS_HEIGHT);
			getShell().setBounds(x, y, width, height);
		}
		
		getShell().addControlListener(new ControlListener() {

			public void controlMoved(ControlEvent e) {
				storeBounds(e);
			}

			public void controlResized(ControlEvent e) {
				storeBounds(e);
			}
			
			private void storeBounds(ControlEvent e) {
				Rectangle bounds= getShell().getBounds();
				getDialogSettings().put(BOUNDS_X, bounds.x);
				getDialogSettings().put(BOUNDS_Y, bounds.y);
				getDialogSettings().put(BOUNDS_WIDTH, bounds.width);
				getDialogSettings().put(BOUNDS_HEIGHT, bounds.height);
			}
		});
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
		mapSelectionPage = new MapProjectSelectionPage("MapProjectSelectionPage",
				"Map Project Selection",
				section,
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		mapSelectionPage.setDescription("Specify a map project to release projects");
		addPage(mapSelectionPage);
		
		projectSelectionPage = new ProjectSelectionPage(Messages.getString("ReleaseWizard.5"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.6"), 
				section, 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		projectSelectionPage.setDescription(Messages.getString("ReleaseWizard.7")); //$NON-NLS-1$
		addPage(projectSelectionPage);
		
		tagPage = new TagPage(Messages.getString("ReleaseWizard.8"), 
				Messages.getString("ReleaseWizard.9"), 
				section, 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE)); //$NON-NLS-1$ //$NON-NLS-2$
		tagPage.setDescription(Messages.getString("ReleaseWizard.10")); //$NON-NLS-1$
		addPage(tagPage);
		
		projectComparePage = new ProjectComparePage(Messages.getString("ReleaseWizard.11"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.12"), 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE)); //$NON-NLS-1$
		projectComparePage.setDescription(Messages.getString("ReleaseWizard.13")); //$NON-NLS-1$
		addPage(projectComparePage);
		
		mapComparePage = new MapFileComparePage(Messages.getString("ReleaseWizard.14"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.15"), 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE)); //$NON-NLS-1$
		mapComparePage.setDescription(Messages.getString("ReleaseWizard.16")); //$NON-NLS-1$
		addPage(mapComparePage);
		
		commentPage = new CommitCommentPage(parentDialog, Messages.getString("ReleaseWizard.17"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.18"), TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE), Messages.getString("ReleaseWizard.19")); //$NON-NLS-1$ //$NON-NLS-2$
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
					TagAndReleaseOperation operation = new TagAndReleaseOperation(null, mapProject, 
							selectedProjects, tag,commentPage.getComment() );
					if (tagPage.isMoveButtonSelected()) {
						operation.moveTag();
					}
					monitor.beginTask("Releasing", 100);
					operation.run(new SubProgressMonitor(monitor, 90));
					try {						
						if(tagPage.isValidateButtonSelected()){
							try {
								validateRelease(new SubProgressMonitor(monitor, 10));
							} catch (TeamException e) {
								throw new InvocationTargetException(e);
							}
						}
					} finally {
						monitor.done();
					}
				}
			});
			mapSelectionPage.saveSettings();
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
		if (page == mapSelectionPage){
			return projectSelectionPage;
		}
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
	private void validateRelease(IProgressMonitor  monitor) throws TeamException{
		ProjectValidationDialog.validateRelease(getShell(), selectedProjects, mapProject.getTagsFor(selectedProjects), monitor);
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
		// There must be projects selected
		if (!isProjectSelected()) {
			return false;
		}
		// There must be a tag
		if (!tagPage.isPageComplete()) {
			return false;
		}
		// Force map comparison if option set by user
		IWizardPage currentPage = getContainer().getCurrentPage();
		if(currentPage == tagPage){
			if(tagPage.compareButtonSelected()){
				return false;
			}
		}
		return true;
	}

	public MapProject getMapProject(){
		return mapProject;
	}
	public void boadcastMapProjectChange(MapProject m){
		mapProject = m;
		projectSelectionPage.updateMapProject(m);
		projectComparePage.updateMapProject(m);
		mapComparePage.updateMapProject(m);
	}
}
