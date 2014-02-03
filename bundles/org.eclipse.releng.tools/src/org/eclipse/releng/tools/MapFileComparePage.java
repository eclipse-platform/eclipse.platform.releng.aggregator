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
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;

public class MapFileComparePage extends WizardPage {
	
	private MapFileCompareEditorInput input = new MapFileCompareEditorInput();
	private String tag;
	
	public MapFileComparePage(String pageName, String title, ImageDescriptor image) {
		super(pageName, title, image);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		//Need to handle input and rebuild tree only when becoming visible
		if(visible){
			input.updateInput(((ReleaseWizard)getWizard()).getSelectedProjects(),tag);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(font);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		try {
			input.run(null);
		} catch (InterruptedException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		}
		
		Control c = input.createContents(composite);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);
	}
	
	public void setTag(String t) {
		this.tag = t;
	}
	public void updateMapProject(MapProject m){
		input.updateMapProject(m);
	}
}
