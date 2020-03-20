/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 * Leo Ufimtsev adding fix of : Gunnar Wagenknecht  (wagenknecht) - [276253] detect '#!/bin/sh' header.
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


public class ShellMakeFile extends SourceFile {

	public ShellMakeFile(IFile file) {
		super(file);
	}

	//Optional Whitespace, #, optional whitespace, then at least 2 non-word chars repeated till EOL
	private static Pattern p = Pattern.compile("\\s*#\\s*\\W{2,}\\s*"); //$NON-NLS-1$

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
		return "#*"; //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	@Override
	public String getCommentEnd() {
		return "**"; //unused, Pattern matcher above will be used instead //$NON-NLS-1$
	}

	@Override
	public int getFileType() {
		return CopyrightComment.SHELL_MAKE_COMMENT;
	}

	@Override
	protected void doInsert(final String comment, IDocument document) throws BadLocationException, IOException {
		// find insert offset (we must skip instructions)
		int insertOffset = findInsertOffset(document);

		// insert comment
		document.replace(insertOffset, 0, comment);
	}

	private int findInsertOffset(IDocument document) throws BadLocationException {
		boolean inInstruction = false;
		int insertOffset = 0;

		for (int offset = 0; offset < document.getLength(); offset++) {
			char c = document.getChar(offset);

			// also look at next char
			char c2 = ((offset + 1) < document.getLength()) ? document.getChar(offset + 1) : 0;

			// look for line ending
			if (inInstruction) {
				if (c == '\n' && c2 == '\r' || c == '\r' && c2 == '\n') {
					insertOffset = offset + 2;
					break; // done
				} else if (c == '\n') {
					insertOffset = offset + 1;
					break; // done
				} else {
					// continue looking for ending
					continue;
				}
			}

			// next chars must start an instruction
			if (c == '#' && c2 == '!') {
				inInstruction = true;
				offset++; // don't need to analyse c2 again
				continue;
			} else {
				// if it's something else, we can stop seeking
				break;
			}
		}
		return insertOffset;
	}
}
