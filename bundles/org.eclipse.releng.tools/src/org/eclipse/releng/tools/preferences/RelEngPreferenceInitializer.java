/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import java.util.Calendar;

import org.eclipse.releng.tools.Messages;
import org.eclipse.releng.tools.RelEngPlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initializes default preferences for release engineering tool
 */
public class RelEngPreferenceInitializer extends AbstractPreferenceInitializer {
	private final String LEGAL_LINE = Messages.getString("RelEngPreferenceInitializer.0"); //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
        IPreferenceStore store = RelEngPlugin.getDefault().getPreferenceStore();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        store.setDefault(RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY, LEGAL_LINE);
        store.setDefault(RelEngCopyrightConstants.CREATION_YEAR_KEY, year);
		store.setDefault(RelEngCopyrightConstants.REVISION_YEAR_KEY, year);
        store.setDefault(RelEngCopyrightConstants.USE_DEFAULT_REVISION_YEAR_KEY, false);
    	// disable fix up existing copyright till it works better
//        store.setDefault(RelEngCopyrightConstants.FIX_UP_EXISTING_KEY, false);
        store.setDefault(RelEngCopyrightConstants.REPLACE_ALL_EXISTING_KEY, false);
        store.setDefault(RelEngCopyrightConstants.IGNORE_PROPERTIES_KEY, false);
        store.setDefault(RelEngCopyrightConstants.IGNORE_XML_KEY, false);
	}

}
