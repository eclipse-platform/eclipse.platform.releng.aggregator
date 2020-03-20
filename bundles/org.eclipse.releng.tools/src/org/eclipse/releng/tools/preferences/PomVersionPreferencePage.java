/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * This preference page allows the error level of the POM version tool to be set
 */
public class PomVersionPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = RelEngPlugin.ID + "PomVersionPreferencePage"; //$NON-NLS-1$

	/**
	 * The main configuration block for the page
	 */
	private PomErrorLevelBlock block = null;

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		comp.setLayout(gl);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label label = new Label(comp, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint = 400;
		label.setLayoutData(gd);
		label.setText(Messages.PomVersionPreferencePage_pom_pref_message);
		label.setFont(comp.getFont());

		block = new PomErrorLevelBlock((IWorkbenchPreferenceContainer)getContainer());
		block.createControl(comp);
		Dialog.applyDialogFont(comp);
		return comp;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performCancel() {
		if (this.block != null) {
			this.block.performCancel();
		}
		return super.performCancel();
	}

	@Override
	public boolean performOk() {
		this.block.performOK();
		return true;
	}

	@Override
	protected void performApply() {
		this.block.performApply();
	}

	@Override
	protected void performDefaults() {
		this.block.performDefaults();
	}
}
