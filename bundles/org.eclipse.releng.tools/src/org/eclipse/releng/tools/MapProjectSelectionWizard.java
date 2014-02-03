/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.releng.tools.preferences.MapProjectPreferencePage;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * This class provides a Wizard implementation for selecting a MapProject 
 */
public class MapProjectSelectionWizard extends Wizard {

	// Dialog store constants
	private static final String BOUNDS_HEIGHT = "bounds.height"; //$NON-NLS-1$
	private static final String BOUNDS_WIDTH = "bounds.width"; //$NON-NLS-1$
	private static final String BOUNDS_Y = "bounds.y"; //$NON-NLS-1$
	private static final String BOUNDS_X = "bounds.x"; //$NON-NLS-1$

	private boolean operationCancelled;
	private IPreferenceStore preferenceStore;
	private IDialogSettings section;
	private MapProjectSelectionPage mapProjectSelectionPage;

	public MapProjectSelectionWizard(String aTitle) {
		setWindowTitle(aTitle);
		IDialogSettings settings = RelEngPlugin.getDefault().getDialogSettings();
		section = settings.getSection("MapProjectSelectionWizard");//$NON-NLS-1$
		if (section == null) {
			section = settings.addNewSection("MapProjectSelectionWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		operationCancelled = false;
		preferenceStore = RelEngPlugin.getDefault().getPreferenceStore();
	}

	/*
	 * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 * @since 3.1
	 */
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		if (getDialogSettings().get(BOUNDS_X) != null) {
			int x = getDialogSettings().getInt(BOUNDS_X);
			int y = getDialogSettings().getInt(BOUNDS_Y);
			int width = getDialogSettings().getInt(BOUNDS_WIDTH);
			int height = getDialogSettings().getInt(BOUNDS_HEIGHT);
			getShell().setBounds(x, y, width, height);
		}

		getShell().addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
				storeBounds(e);
			}

			public void controlResized(ControlEvent e) {
				storeBounds(e);
			}

			private void storeBounds(ControlEvent e) {
				Rectangle bounds = getShell().getBounds();
				getDialogSettings().put(BOUNDS_X, bounds.x);
				getDialogSettings().put(BOUNDS_Y, bounds.y);
				getDialogSettings().put(BOUNDS_WIDTH, bounds.width);
				getDialogSettings().put(BOUNDS_HEIGHT, bounds.height);
			}
		});
	}

	public boolean execute(Shell shell) {
		setNeedsProgressMonitor(true);
		WizardDialog dialog = new WizardDialog(shell, this);
		return (dialog.open() == Window.OK);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		mapProjectSelectionPage = new MapProjectSelectionPage("MapProjectSelectionPage", //$NON-NLS-1$
				Messages.getString("MapProjectSelectionWizard.1"), //$NON-NLS-1$
				section, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		mapProjectSelectionPage.setDescription(Messages.getString("MapProjectSelectionWizard.2")); //$NON-NLS-1$
		addPage(mapProjectSelectionPage);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		mapProjectSelectionPage.saveSettings();
		preferenceStore.setValue(MapProjectPreferencePage.USE_DEFAULT_MAP_PROJECT, mapProjectSelectionPage.useDefaultMapProject());
		String fullPath = mapProjectSelectionPage.getSelectedMapProject().getProject().getFullPath().toString();
		preferenceStore.setValue(MapProjectPreferencePage.SELECTED_MAP_PROJECT_PATH, fullPath);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performCancel()
	 */
	public boolean performCancel() {
		operationCancelled = true;
		return true;
	}

	//Used by classes calling MapProjectSelectionWizard to determine if their intended action should proceed
	//once this wizard has terminated.
	public boolean operationCancelled() {
		return operationCancelled;
	}
}
