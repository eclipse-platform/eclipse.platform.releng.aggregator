/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [235572] detect existing comments in bat files
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;


public class BatFile extends SourceFile {

	public BatFile(IFile file) {
		super(file);
	}

	//Optional Whitespace, #, optional whitespace, then at least 2 non-word chars repeated till EOL
	private static Pattern p = Pattern.compile("\\s*@?[rR][eE][mM]\\s+\\W{2,}\\s*"); //$NON-NLS-1$

	@Override
	public boolean isCommentStart(String aLine) {
		return p.matcher(aLine).matches();
	}

	@Override
	public boolean isCommentEnd(String aLine, String commentStartString) {
		String s = commentStartString.trim();
		s = s.substring(s.length()-2);
		return aLine.trim().endsWith(s);
	}

	@Override
	public String getCommentStart() {
		return "@rem **";  //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	@Override
	public String getCommentEnd() {
		return "**";  //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	@Override
	public int getFileType() {
		return CopyrightComment.BAT_COMMENT;
	}

}
