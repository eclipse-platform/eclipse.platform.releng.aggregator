/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.IOException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/**
 * XML File
 */
public class XmlFile extends SourceFile {

	public XmlFile(IFile file) {
		super(file);
	}

	public String getCommentStart() {
		return "<!--"; //$NON-NLS-1$
	}

	public String getCommentEnd() {
		return "-->"; //$NON-NLS-1$
	}
	
	public int getFileType() {
		return CopyrightComment.XML_COMMENT;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.releng.tools.SourceFile#doInsert(java.lang.String, org.eclipse.jface.text.IDocument)
	 */
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
			
			// ignore whitespace and new lines
			if(Character.isWhitespace(c)) {
				// we update the offset to ignore whitespaces 
				// after instruction ends 
				insertOffset = offset;
				continue;
			}

			// look at next char
			char c2 = ((offset+1) < document.getLength()) ? document.getChar(offset+1) : 0;
			
			// look for instruction ending
			if(inInstruction) {
				if(c == '?' && c2 == '>') {
					insertOffset = offset + 1;
					inInstruction = false;
					offset++; // don't need to analyse c2 again
					// we continue in case there are more instructions
					continue;
				} else {
					// look for ending
					continue;
				}
			}
			
			// next chars must start an instruction
			if(c == '<' && c2 =='?') {
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
