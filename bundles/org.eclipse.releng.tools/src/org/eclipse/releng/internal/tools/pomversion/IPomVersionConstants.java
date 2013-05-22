/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import org.eclipse.releng.tools.RelEngPlugin;


/**
 * Contains all the constants used by the POM version tool.
 */
public interface IPomVersionConstants {
	
	/**
	 * The marker type id for POM version problems specified in the markers extension.
	 * Value is: <code>org.eclipse.releng.tools.pomVersionProblem</code>
	 */
	public final static String PROBLEM_MARKER_TYPE = RelEngPlugin.ID + ".pomVersionProblem"; //$NON-NLS-1$
	
	/**
	 * String attribute stored in the problem marker for the correct version that should be in the POM file
	 */
	public static final String POM_CORRECT_VERSION = "pom.CorrectVersion"; //$NON-NLS-1$
	
	/**
	 * Preference setting that stores the severity level for pom version problem markers.
	 * Preference value must be a string and one of {@link #VALUE_ERROR}, {@link #VALUE_WARNING} or {@link #VALUE_IGNORE}. 
	 */
	public final static String POM_VERSION_ERROR_LEVEL = RelEngPlugin.ID + ".invalidPomVersionErrorLevel"; //$NON-NLS-1$

	/**
	 * Constant representing the preference value 'ignore'.
	 * Value is: <code>Ignore</code>
	 */
	public static final String VALUE_IGNORE = "Ignore"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'warning'.
	 * Value is: <code>Warning</code>
	 */
	public static final String VALUE_WARNING = "Warning"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'error'.
	 * Value is: <code>Error</code>
	 */
	public static final String VALUE_ERROR = "Error"; //$NON-NLS-1$
}
