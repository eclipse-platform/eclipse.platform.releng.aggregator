/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


/**
 * Validate page which is shown while the release is being validated.
 * 
 * @since 3.6
 */
public class ValidatePage extends WizardPage {

	public ValidatePage(Dialog parentDialog, String pageName, String title, ImageDescriptor image) {
		super(pageName, title, image);
		setDescription(Messages.getString("ValidatePage.description")); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {
		setControl(new Label(parent, SWT.NONE));
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return false;
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		return null;
	}
}
