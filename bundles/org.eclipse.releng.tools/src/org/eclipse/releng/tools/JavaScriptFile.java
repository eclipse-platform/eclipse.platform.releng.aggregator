/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IFile;


public class JavaScriptFile extends SourceFile {

	public JavaScriptFile(IFile file) {
		super(file);
	}

	@Override
	public String getCommentStart() {
		return "/*"; //$NON-NLS-1$
	}

	@Override
	public String getCommentEnd() {
		return "*/"; //$NON-NLS-1$
	}
	@Override
	public int getFileType() {
		return CopyrightComment.JAVASCRIPT_COMMENT;
	}

}
