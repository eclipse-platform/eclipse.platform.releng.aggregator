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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.ui.synchronize.TreeViewerAdvisor;

/**
 *It extends <code>Dialog<code>. This dialog will show up if <code>isValidateButtonSelected()<code> 
 *in <code>TagPage<code> returns true
 */
public class ProjectValidationDialog extends Dialog {
	
	private ProjectComparePageTreeInput compareEditorInput;	
	private String title;
	
	public static void validateRelease(final IProject[] p, final CVSTag[] tags, IProgressMonitor monitor){
		CVSCompareSubscriber subscriber = new CVSCompareSubscriber(p, tags, Messages.getString("ReleaseWizard.20")); //$NON-NLS-1$
		SyncInfoSet set = new SyncInfoSet();
		subscriber.collectOutOfSync(p, IResource.DEPTH_INFINITE, set, monitor);
		if(!set.isEmpty()){
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					Shell shell = new Shell();	
					openValidationDialog(shell, p, tags);
				}
			});		
		}
	}

	private static void openValidationDialog(Shell shell, IProject[] p, CVSTag[] tags) {
		ProjectValidationDialog dialog = new ProjectValidationDialog(shell, Messages.getString("ProjectValidationDialog.2")); //$NON-NLS-1$
		if(dialog.getOutOfSyncProjects(p, tags) != null ){
			dialog.open();
		}
	}

	private ProjectValidationDialog(Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
		setShellStyle(SWT.RESIZE | SWT.MAX |SWT.APPLICATION_MODAL|SWT.DIALOG_TRIM);
		SyncInfoTree set = new SyncInfoTree();
		CompareConfiguration compareConfig = new CompareConfiguration();
		TreeViewerAdvisor viewerAdvisor = new TreeViewerAdvisor(null, null, set);
		compareEditorInput = new ProjectComparePageTreeInput(compareConfig, viewerAdvisor);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NONE); 
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(font);
		
		Label label = new Label(composite,SWT.NONE);
		label.setText(Messages.getString("ProjectCompareDialog.0")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setFont(font);
		
		try {
			// Preparing the input should be fast since we haven't started the collector
			compareEditorInput.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		}
		
		Control c = compareEditorInput.createContents(composite);
		GridData data = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(data);
		c.setFont(font);
		
		return super.createDialogArea(parent);
	}
	
	private IResource[] getOutOfSyncProjects(IProject[] projects, CVSTag[] tags){
		IResource[] r = null;
		if(projects == null) return null;
		try {
			r = compareEditorInput.getOutOfSyncProjects(projects, tags, new NullProgressMonitor());
		} catch (TeamException e1) {
			CVSUIPlugin.openError(getShell(), null, null, e1);
		}
		return r;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK button only, no Cancel button is needed
		createButton(
			parent,
			IDialogConstants.OK_ID,
			IDialogConstants.OK_LABEL,
			true);
	}
	
	/* (non-Javadoc)
	 * Method declared in Window.
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}
}
