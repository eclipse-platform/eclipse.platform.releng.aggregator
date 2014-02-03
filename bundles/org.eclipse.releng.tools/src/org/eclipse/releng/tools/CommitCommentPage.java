/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.ui.CommitCommentArea;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;

public class CommitCommentPage extends CVSWizardPage {

	private CommitCommentArea commitCommentArea;

	public CommitCommentPage(
		Dialog parentDialog,
		String pageName,
		String title,
		ImageDescriptor image,
		String description) {
			
		super(pageName, title, image, description);
		commitCommentArea = new CommitCommentArea();
	}

	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout());
		setControl(top);
		commitCommentArea.createArea(top);
        Dialog.applyDialogFont(parent);
	}

	public String getComment() {
		return commitCommentArea.getComment(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			commitCommentArea.setFocus();
		}
	}
}

