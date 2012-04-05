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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * This code copied from CVSProjectPropertiesPage
 */
public class RepositorySelectionDialog extends Dialog {
	ICVSRepositoryLocation[] locations;
	ICVSRepositoryLocation location;
	
	private static final int TABLE_HEIGHT_HINT = 150;
	private static final int TABLE_WIDTH_HINT = 300;

	TableViewer viewer;
	Button okButton;
	public RepositorySelectionDialog(Shell shell) {
		super(shell);
	}
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText(Messages.getString("RepositorySelectionDialog.0")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createLabel(composite, Messages.getString("RepositorySelectionDialog.1"), 1);  //$NON-NLS-1$
		Table table = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData();
		data.widthHint = TABLE_WIDTH_HINT;
		data.heightHint = TABLE_HEIGHT_HINT;
		table.setLayoutData(data);
		viewer = new TableViewer(table);
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setContentProvider(new WorkbenchContentProvider() {
			public Object[] getElements(Object inputElement) {
				return locations;
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection.isEmpty()) {
					location = null;
					okButton.setEnabled(false);
				} else {
					location = (ICVSRepositoryLocation)selection.getFirstElement();
					okButton.setEnabled(true);
				}
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		viewer.setInput(locations);
		return composite;
	}
	protected Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	protected void cancelPressed() {
		location = null;
		super.cancelPressed();
	}
	public void setLocations(ICVSRepositoryLocation[] locations) {
		this.locations = locations;
	}
	public ICVSRepositoryLocation getLocation() {
		return location;
	}
}
