/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * Martin Oberhuber (Wind River) - [235572] detect existing comments in bat files
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;


public class ShellMakeFile extends SourceFile {

	public ShellMakeFile(IFile file) {
		super(file);
	}

	//Optional Whitespace, #, optional whitespace, then at least 2 non-word chars repeated till EOL 
	private static Pattern p = Pattern.compile("\\s*#\\s*\\W{2,}\\s*"); //$NON-NLS-1$
	
	public boolean isCommentStart(String aLine) {
		return p.matcher(aLine).matches();
	}

	public boolean isCommentEnd(String aLine, String commentStartString) {
		String s = commentStartString.trim();
		s = s.substring(s.length()-2);
		return aLine.trim().endsWith(s);
	}

	public String getCommentStart() {
		return "#*"; //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	public String getCommentEnd() {
		return "**"; //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	public int getFileType() {
		return CopyrightComment.SHELL_MAKE_COMMENT;
	}

}
