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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.internal.ui.synchronize.ChangeSetModelManager;
import org.eclipse.team.ui.SaveablePartAdapter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ParticipantPageDialog;
import org.eclipse.team.ui.synchronize.ParticipantPageSaveablePart;

/**
 *It extends <code>Dialog<code>. This dialog will show up if <code>isValidateButtonSelected()<code> 
 *in <code>TagPage<code> returns true
 */
public class ProjectValidationDialog extends ParticipantPageDialog {
	
	private String title = Messages.getString("ProjectValidationDialog.2"); //$NON-NLS-1$
	
	public static void validateRelease(final Shell shell, final IProject[] p, final CVSTag[] tags, IProgressMonitor monitor) throws TeamException{
		final CompareParticipant participant = new CompareParticipant(new CVSCompareSubscriber(p, tags, Messages.getString("ReleaseWizard.20"))); //$NON-NLS-1$
		try {
			participant.refreshNow(p, Messages.getString("ReleaseWizard.20"), monitor); //$NON-NLS-1$
			if (!participant.getSyncInfoSet().isEmpty()) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						openValidationFailedDialog(shell, participant);
					}
				});	
			}
		} finally {
			participant.dispose();
		}
	}

	private static void openValidationFailedDialog(Shell shell, CompareParticipant participant) {
		ISynchronizePageConfiguration configuration = participant.createPageConfiguration();
		configuration.setMenuGroups(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] { 
				ISynchronizePageConfiguration.NAVIGATE_GROUP,  
				ISynchronizePageConfiguration.LAYOUT_GROUP,
				ChangeSetModelManager.CHANGE_SET_GROUP});
		configuration.setMenuGroups(ISynchronizePageConfiguration.P_CONTEXT_MENU, new String[0]);
		
		CompareConfiguration cc = new CompareConfiguration();
		cc.setLeftEditable(false);
		cc.setRightEditable(false);
		ParticipantPageSaveablePart part = new ParticipantPageSaveablePart(shell, cc, configuration, participant);
		
		ProjectValidationDialog dialog = new ProjectValidationDialog(shell, part, participant); //$NON-NLS-1$
		dialog.open();
	}

	public ProjectValidationDialog(Shell shell, SaveablePartAdapter input,
			ISynchronizeParticipant participant) {
		super(shell, input, participant);
		shell.setText(title);
	}
}
