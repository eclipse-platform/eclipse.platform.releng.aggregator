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
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
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
		//Start the release wizard
		ReleaseWizard wizard = new ReleaseWizard();
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
}
