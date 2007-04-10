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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;


/**
 *This class respond the "release..." menu action and try to open a wizard
 */
public class TagAndReleaseAction extends CVSAction {
	
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
	public boolean isEnabled() {
		return true;
	}
}
