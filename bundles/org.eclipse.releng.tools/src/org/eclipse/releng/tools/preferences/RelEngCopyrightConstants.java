/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
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

/**
 * Contains all the constants used by the releng copyright tool
 */
public class RelEngCopyrightConstants {
	public final static String COPYRIGHT_TEMPLATE_KEY = "org.eclipse.releng.tools.copyrightTemplate"; //$NON-NLS-1$
	public final static String CREATION_YEAR_KEY = "org.eclipse.releng.tools.creationYear"; //$NON-NLS-1$
	public final static String REVISION_YEAR_KEY = "org.eclipse.releng.tools.revisionYear"; //$NON-NLS-1$
	public final static String USE_DEFAULT_REVISION_YEAR_KEY = "org.eclipse.releng.tools.useDefaultRevisionYear"; //$NON-NLS-1$
	// disable fix up existing copyright till it works better
//	public final static String FIX_UP_EXISTING_KEY = "org.eclipse.releng.tools.fixUpExisting"; //$NON-NLS-1$
	public final static String REPLACE_ALL_EXISTING_KEY = "org.eclipse.releng.tools.replaceAllExisting"; //$NON-NLS-1$
	public final static String IGNORE_PROPERTIES_KEY = "org.eclipse.releng.tools.ignoreProperties"; //$NON-NLS-1$
	public final static String IGNORE_XML_KEY = "org.eclipse.releng.tools.ignoreXml"; //$NON-NLS-1$
}
