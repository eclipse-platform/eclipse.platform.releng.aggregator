/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


/**
 *This class respond the "release..." menu action and try to open a wizard
 */
public class TagAndReleaseAction extends CVSAction implements IWorkbenchWindowActionDelegate {

	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.TagAction#execute(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		MapProject mapProject= MapProject.getDefaultMapProject();
		if(!isValid(mapProject)){
			return;
		}
		//Start the release wizard
		ReleaseWizard wizard = new ReleaseWizard(mapProject);
		IResource[] preSelection = getSelectedResources();
		if(preSelection != null && preSelection.length != 0){
			wizard.setPreSelectedProjects(preSelection);
		}
		wizard.execute(getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		this.shell = window.getShell();	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getSelectedResources()
	 * Overwrite the method to eliminate null ponter exception when first time start the wizard
	 */
	protected IResource[] getSelectedResources() {
		if (selection == null) return new IResource[0];
		return super.getSelectedResources();
	}
	private boolean isValid(final MapProject mapProject){
		//Check if map project is accessible
		if(mapProject==null || (!mapProject.getProject().isAccessible())){
			MessageDialog.openError(getShell(),"Error Occured","There is no map project found or it is not accessible.");
			return false;
		}
		//Check if there is at least one map file with valid entries exists in map project
		MapFile[] files = mapProject.getValidMapFiles();
		if(files == null || files.length == 0){
			MessageDialog.openError(getShell(),"Error Occured","There is no valid map file or map entry found.");
			return false;
		}
		//Check if the map project is shared
		if(RepositoryProvider.getProvider(mapProject.getProject()) == null){
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(new Shell(),"Error Occured", "Project " + mapProject.getProject().getName()+" is not shared");
				}
			});
			return false;
		}
		return true;
	}
}
