/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;


final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.releng.tools.preferences.messages"; //$NON-NLS-1$

	public static String PomErrorLevelBlock_mismatched_pom_versions_pref;
	public static String PomVersionPreferencePage_pom_pref_message;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}
}
