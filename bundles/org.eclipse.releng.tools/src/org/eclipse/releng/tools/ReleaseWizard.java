/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.preferences.MapProjectPreferencePage;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.CommitOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.dialogs.IPromptCondition;
import org.eclipse.team.internal.ui.dialogs.PromptingDialog;


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
	private BuildNotesPage buildNotesPage;

	private Dialog parentDialog;
	private IDialogSettings section;
	
	private MapProject mapProject;
	private IProject[] preSelectedProjects;
	private IProject[] selectedProjects;
	private IPreferenceStore preferenceStore;
	private boolean defaultBeingUsed;
	

	public ReleaseWizard() {
		setWindowTitle("Release"); //$NON-NLS-1$
		IDialogSettings settings = RelEngPlugin.getDefault().getDialogSettings();
		section = settings.getSection("ReleaseWizard");//$NON-NLS-1$
		if (section == null) {
			section = settings.addNewSection("ReleaseWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();
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
		defaultBeingUsed = false;
		if (preferenceStore.getBoolean(MapProjectPreferencePage.USE_DEFAULT_MAP_PROJECT) &&
				(preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH).length() > 1)) {
			String path = preferenceStore.getString(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH);
			
			try {
				mapProject = new MapProject(ResourcesPlugin.getWorkspace().getRoot().getProject(path));
				defaultBeingUsed = true;
			}
			catch (CoreException e) {
				//this is ok, we will use the mapSelectionPage instead
			}			
		}
		
		if (!defaultBeingUsed) addMapSelectionPage();
		
		projectSelectionPage = new ProjectSelectionPage(Messages.getString("ReleaseWizard.5"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.6"),  //$NON-NLS-1$
				section, 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		projectSelectionPage.setDescription(Messages.getString("ReleaseWizard.7")); //$NON-NLS-1$
		addPage(projectSelectionPage);
		
		tagPage = new TagPage(Messages.getString("ReleaseWizard.8"),  //$NON-NLS-1$
				Messages.getString("ReleaseWizard.9"),  //$NON-NLS-1$
				section, 
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		tagPage.setDescription(Messages.getString("ReleaseWizard.10")); //$NON-NLS-1$
		addPage(tagPage);
		
		projectComparePage = new ProjectComparePage(Messages.getString("ReleaseWizard.11"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.12"),  //$NON-NLS-1$
				section, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		projectComparePage.setDescription(Messages.getString("ReleaseWizard.13")); //$NON-NLS-1$
		addPage(projectComparePage);
		
		buildNotesPage = new BuildNotesPage(Messages.getString("ReleaseWizard.2"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.1"), section, TeamUIPlugin //$NON-NLS-1$
				.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		buildNotesPage.setDescription(Messages.getString("ReleaseWizard.0")); //$NON-NLS-1$
		addPage(buildNotesPage);
		
		mapComparePage = new MapFileComparePage(Messages.getString("ReleaseWizard.14"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.15"),  //$NON-NLS-1$
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		mapComparePage.setDescription(Messages.getString("ReleaseWizard.16")); //$NON-NLS-1$
		addPage(mapComparePage);
		
		commentPage = new CommitCommentPage(parentDialog, Messages.getString("ReleaseWizard.17"), //$NON-NLS-1$
				Messages.getString("ReleaseWizard.18"), TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE), Messages.getString("ReleaseWizard.19")); //$NON-NLS-1$ //$NON-NLS-2$
		addPage(commentPage);
		
		if (defaultBeingUsed) broadcastMapProjectChange(mapProject);
	}
	
	private void addMapSelectionPage() {
		mapSelectionPage = new MapProjectSelectionPage("MapProjectSelectionPage", //$NON-NLS-1$
				Messages.getString("ReleaseWizard.4"), //$NON-NLS-1$
				section,
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		mapSelectionPage.setDescription(Messages.getString("ReleaseWizard.3")); //$NON-NLS-1$
		addPage(mapSelectionPage);
	}

	/*
	 * commit buildnotes file if update option selected
	 */
    public boolean buildNotesOperation() {
		if (buildNotesPage.isUpdateNotesButtonChecked() && projectComparePage.isBuildNotesButtonChecked()) {
			buildNotesPage.updateNotesFile();
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						IFile iFile = buildNotesPage.getIFile();
						IProject iProject = iFile.getProject();
						monitor.beginTask(Messages.getString("ReleaseWizard.20"), 100); //$NON-NLS-1$
						new CommitOperation(
								null,
								RepositoryProviderOperation
										.asResourceMappers(new IResource[] { iProject }),
								new Command.LocalOption[0], commentPage
										.getComment()).run(monitor);
						monitor.done();
					}
				});
			} catch (InterruptedException e) {
				// Cancelled.
				return false;
			} catch (InvocationTargetException e) {
				CVSUIPlugin.openError(getShell(), null, null, e);
			}
			checkProjects();
		}
		return true;
	}

    /*
     * add project of build notes file if not already in selected projects
     */
	public void checkProjects() {
		IProject[] temp = new IProject[selectedProjects.length + 1];
		for (int i = 0; i < selectedProjects.length; i++) {
			if (selectedProjects[i] == buildNotesPage.getIFile().getProject()) {
				return;
			}
		}
		System.arraycopy(selectedProjects, 0, temp, 0, selectedProjects.length);
		temp[temp.length - 1] = buildNotesPage.getIFile().getProject();
		selectedProjects = temp;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		if(!isProjectSelected())return false;
		
		// Build notes file update cancelled. Close dialog.
		if (!buildNotesOperation()) {
			return true;
		}
		
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
					monitor.beginTask(Messages.getString("ReleaseWizard.21"), 100); //$NON-NLS-1$
					operation.run(new SubProgressMonitor(monitor, 90));
					if (operation.isMapFileUpdated()) {
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
					} else {
						// The map file update didn't occur and no exception was thrown.
						// Let the user know of the failure
						IStatus[] errors = operation.getErrors();
						IStatus status;
                        if (errors.length == 0) {
                            status = new Status(IStatus.ERROR, RelEngPlugin.ID, 0, Messages.getString("ReleaseWizard.22"), null); //$NON-NLS-1$
                        } else if (errors.length == 1) {
							status = errors[0];
						} else {
							status = new MultiStatus(RelEngPlugin.ID, 0, errors, Messages.getString("ReleaseWizard.23"), null); //$NON-NLS-1$
						}
						ErrorDialog.openError(getShell(), Messages.getString("ReleaseWizard.24"),  //$NON-NLS-1$
								Messages.getString("ReleaseWizard.25"),  //$NON-NLS-1$
								status, IStatus.ERROR | IStatus.WARNING);
					}
				}
			});
			if (!defaultBeingUsed) {
				mapSelectionPage.saveSettings();
				updatePreferenceStore();
			}
			projectSelectionPage.saveSettings();
			projectComparePage.saveSettings();
			buildNotesPage.saveSettings();
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
	
	private void updatePreferenceStore() {
		preferenceStore.setValue(MapProjectPreferencePage.USE_DEFAULT_MAP_PROJECT, mapSelectionPage.useDefaultMapProject());	
		String fullPath = mapSelectionPage.getSelectedMapProject().getProject().getFullPath().toString();
		preferenceStore.setValue(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH, fullPath);
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
		if (page == projectComparePage) {
			if (projectComparePage.isBuildNotesButtonChecked()) {
				buildNotesPage.setSyncInfoSet(projectComparePage
						.getSyncInfoSet());
				return buildNotesPage;
			} 
			return tagPage;
		}
		if (page == buildNotesPage) {
			return tagPage;
		}
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
			getPromptCondition(projects), CVSUIMessages.TagAction_uncommittedChangesTitle);
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
	

	protected IPromptCondition getPromptCondition(IResource[] resources) {
		return new IPromptCondition() {
			public boolean needsPrompt(IResource resource) {
				return CVSLightweightDecorator.isDirty(resource);
			}
			public String promptMessage(IResource resource) {
				return NLS.bind(CVSUIMessages.TagAction_uncommittedChanges, new String[] { resource.getName() });
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
	public void broadcastMapProjectChange(MapProject m){
		mapProject = m;
		projectSelectionPage.updateMapProject(m);
		projectComparePage.updateMapProject(m);
		mapComparePage.updateMapProject(m);
	}
}
