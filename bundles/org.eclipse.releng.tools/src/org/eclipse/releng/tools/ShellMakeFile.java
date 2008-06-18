/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

public class ShellMakeFile extends SourceFile {

	public ShellMakeFile(IFile file) {
		super(file);
	}

	public String getCommentStart() {
		return "#*";
	}

	public String getCommentEnd() {
		return "**";
	}

	public int getFileType() {
		return CopyrightComment.SHELL_MAKE_COMMENT;
	}

}
