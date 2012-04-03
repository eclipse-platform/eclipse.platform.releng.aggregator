/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IFile;


/**
 * @author droberts
 */
public class PropertiesFile extends SourceFile {

	/**
	 * @param file
	 */
	public PropertiesFile(IFile file) {
		super(file);
	}

	/* (non-Javadoc)
	 * @see Test.popup.actions.SourceFile#getCommentStart()
	 */
	public String getCommentStart() {
		return "##"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see Test.popup.actions.SourceFile#getCommentEnd()
	 */
	public String getCommentEnd() {
		return "##"; //$NON-NLS-1$
	}
	
	public int getFileType() {
		return CopyrightComment.PROPERTIES_COMMENT;
	}

}
