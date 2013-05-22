/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.releng.tools.pomversion.IPomVersionConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This block is used to add error/warning combos to the {@link PomVersionPreferencePage}
 * 
 */
public class PomErrorLevelBlock extends ConfigurationBlock {
	/**
	 * Provides data information for created controls
	 */
	protected static class ControlData {
		Key key;
		private String[] values;
		
		/**
		 * Constructor
		 * @param key
		 * @param values
		 */
		public ControlData(Key key, String[] values) {
			this.key = key;
			this.values = values;
		}
		
		public Key getKey() {
			return key;
		}
		
		public String getValue(boolean selection) {
			int index= selection ? 0 : 1;
			return values[index];
		}
		
		public String getValue(int index) {
			return values[index];
		}		
		
		public int getSelection(String value) {
			if (value != null) {
				for (int i= 0; i < values.length; i++) {
					if (value.equals(values[i])) {
						return i;
					}
				}
			}
			return values.length -1; // assume the last option is the least severe
		}
	}
	
	/**
	 * Provides management for changed/stored values for a given preference key
	 */
	protected static class Key {
		
		private String qualifier;
		private String key;
		
		/**
		 * Constructor
		 * @param qualifier
		 * @param key
		 */
		public Key(String qualifier, String key) {
			this.qualifier= qualifier;
			this.key= key;
		}
		
		/**
		 * Returns the {@link IEclipsePreferences} node for the given context and {@link IWorkingCopyManager}
		 * @param context
		 * @param manager
		 * @return the {@link IEclipsePreferences} node or <code>null</code>
		 */
		private IEclipsePreferences getNode(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = context.getNode(qualifier);
			if (manager != null) {
				return manager.getWorkingCopy(node);
			}
			return node;
		}
		
		/**
		 * Returns the value stored in the {@link IEclipsePreferences} node from the given context and working copy manager
		 * @param context
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or <code>null</code>
		 */
		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if(node != null) {
				return node.get(key, null);
			}
			return null;
		}
		
		/**
		 * Returns the stored value of this {@link IEclipsePreferences} node using a given lookup order, and allowing the
		 * top scope to be ignored
		 * @param lookupOrder
		 * @param ignoreTopScope
		 * @param manager
		 * @return the value from the {@link IEclipsePreferences} node or <code>null</code>
		 */
		public String getStoredValue(IScopeContext[] lookupOrder, boolean ignoreTopScope, IWorkingCopyManager manager) {
			for (int i = ignoreTopScope ? 1 : 0; i < lookupOrder.length; i++) {
				String value = getStoredValue(lookupOrder[i], manager);
				if (value != null) {
					return value;
				}
			}
			return null;
		}
		
		/**
		 * Sets the value of this key
		 * @param context
		 * @param value
		 * @param manager
		 */
		public void setStoredValue(IScopeContext context, String value, IWorkingCopyManager manager) {
			IEclipsePreferences node = getNode(context, manager);
			if (value != null) {
				node.put(key, value);
			} else {
				node.remove(key);
			}
		}
			
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return qualifier + '/' + key;
		}
	}

	private static final Key KEY_POM_VERSION_ERROR_LEVEL = new Key(RelEngPlugin.ID, IPomVersionConstants.POM_VERSION_ERROR_LEVEL);

	/**
	 * An array of all of the keys for the page
	 */
	private static Key[] fgAllKeys = {
		KEY_POM_VERSION_ERROR_LEVEL
	};

	/**
	 * Constant representing the severity values presented in the combo boxes for each option
	 */
	private static final String[] SEVERITIES_LABELS = {
		"Error", //$NON-NLS-1$
		"Warning", //$NON-NLS-1$
		"Ignore" //$NON-NLS-1$
	};
	
	/**
	 * Constant representing the severity values presented in the combo boxes for each option
	 */
	private static final String[] SEVERITIES = {
		IPomVersionConstants.VALUE_ERROR,
		IPomVersionConstants.VALUE_WARNING,
		IPomVersionConstants.VALUE_IGNORE,
	};
	
	/**
	 * Default selection listener for controls on the page
	 */
	private SelectionListener selectionlistener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if(e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				ControlData data = (ControlData) combo.getData();
				data.key.setStoredValue(fLookupOrder[0], combo.getText(), fManager);
				fDirty = true;
			}
		}
	};

	/**
	 * Listing of all of the {@link Combo}s added to the block
	 */
	private Combo fCombo = null;
	
	/**
	 * The context of settings locations to search for values in
	 */
	IScopeContext[] fLookupOrder = null;
	
	/**
	 * the working copy manager to work with settings
	 */
	IWorkingCopyManager fManager = null;
	
	/**
	 * The main composite for the configuration block, used for enabling/disabling the block 
	 */
	private Composite fMainComp = null;
	
	
	/**
	 * Stored old fProject specific settings. 
	 */
	private IdentityHashMap fOldProjectSettings = null;
	
	/**
	 * Flag used to know if the page needs saving or not
	 */
	boolean fDirty = false;
	
	/**
	 * Constructor
	 * @param project
	 */
	public PomErrorLevelBlock(IWorkbenchPreferenceContainer container) {
		fLookupOrder = new IScopeContext[] {
			InstanceScope.INSTANCE,
			DefaultScope.INSTANCE
		};
		if(container == null) {
			fManager = new WorkingCopyManager();
		}
		else {
			fManager = container.getWorkingCopyManager();
		}
		fOldProjectSettings = null;
	}
	
	/**
	 * Creates the control in the parent control
	 * 
	 * @param parent the parent control
	 */
	public Control createControl(Composite parent) {
		fMainComp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		fMainComp.setLayout(gl);
		fMainComp.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		fMainComp.setFont(parent.getFont());
		this.fCombo = createComboControl(fMainComp, Messages.PomErrorLevelBlock_mismatched_pom_versions_pref, KEY_POM_VERSION_ERROR_LEVEL);
		Dialog.applyDialogFont(fMainComp);
		return fMainComp;
	}

	/**
	 * Saves all of the changes on the page
	 */
	public void performOK()  {
		save();
	}
	
	/**
	 * Directly applies all of the changes on the page
	 */
	public void performApply() {
		save();
	}
	
	/**
	 * Performs the save operation on the working copy manager
	 */
	private void save() {
		if(fDirty) {
			try {
				ArrayList changes = new ArrayList();
				collectChanges(fLookupOrder[0], changes);
				if(changes.size() > 0) {
					fManager.applyChanges();
				}
				fDirty = false;
			}
			catch(BackingStoreException bse) {
				RelEngPlugin.log(new Status(IStatus.ERROR, RelEngPlugin.ID, bse.getMessage(), bse));
			}
		}
	}
	
	/**
	 * Cancels all of the changes on the page
	 */
	public void performCancel() {}
	
	/**
	 * Reverts all of the settings back to their defaults
	 */
	public void performDefaults() {
		String defval = null;
		for(int i = 0; i < fgAllKeys.length; i++) {
			defval = fgAllKeys[i].getStoredValue(fLookupOrder, true, fManager);
			fgAllKeys[i].setStoredValue(fLookupOrder[0], defval, fManager);
		}
		updateCombos();
		fDirty = true;
	}
	
	/**
	 * Updates all of the registered {@link Combo}s on the page.
	 * Registration implies that the {@link Combo} control was added to the listing 
	 * of fCombos
	 */
	private void updateCombos() {
		if (this.fCombo != null) {
			ControlData data = (ControlData) fCombo.getData();
			this.fCombo.select(data.getSelection(data.getKey().getStoredValue(fLookupOrder, false, fManager)));
		}
	}

	/**
	 * Disposes the controls from this page
	 */
	public void dispose() {
		fMainComp.getParent().dispose();
	}
	
	/**
	 * Creates a {@link Label} | {@link Combo} control. The combo is initialised from the given {@link Key}
	 * @param parent
	 * @param label
	 * @param key
	 */
	protected Combo createComboControl(Composite parent, String label, Key key) {
		Label lbl = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
		lbl.setLayoutData(gd);
		lbl.setText(label);
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.END, GridData.CENTER, false, false);
		combo.setLayoutData(gd);
		ControlData data = new ControlData(key, SEVERITIES); 
		combo.setData(data);
		combo.setItems(SEVERITIES_LABELS);
		combo.addSelectionListener(selectionlistener);
		combo.select(data.getSelection(key.getStoredValue(fLookupOrder, false, fManager)));
		addHighlight(parent, lbl, combo);
		return combo;
	}

	/**
	 * Collects the keys that have changed on the page into the specified list
	 * @param changes the {@link List} to collect changed keys into
	 */
	private void collectChanges(IScopeContext context, List changes) {
		Key key = null;
		String origval = null,
			   newval = null;
		boolean complete = fOldProjectSettings == null;
		for(int i = 0; i < fgAllKeys.length; i++) {
			key = fgAllKeys[i];
			origval = key.getStoredValue(context, null);
			newval = key.getStoredValue(context, fManager);
			if(newval == null) {
				if(origval != null) {
					changes.add(key);
				}
				else if(complete) {
					key.setStoredValue(context, key.getStoredValue(fLookupOrder, true, fManager), fManager);
					changes.add(key);
				}
			}
			else if(!newval.equals(origval)) {
				changes.add(key);
			}
		}
	}
	
	public static Key[] getAllKeys() {
		return fgAllKeys;
	}
}
