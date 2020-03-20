/*******************************************************************************
 * Copyright (c) 2008, 2014 Gunnar Wagenknecht and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Leo Ufimtsev lufimtse@redhat.com - fixed xml header issues.
 *      https://bugs.eclipse.org/381147
 *      https://bugs.eclipse.org/bugs/show_bug.cgi?id=276257  //used.
 *
 *
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/*
 * Test notes:
 * [x] Empty Document
 * [x] Empty Document with XML header
 * [x] Document with content, no XML header.
 * [x] Document with XML header and content on 2nd line
 * [x] Document with XML header, copyright on first line with content on first line.
 * [x] Document with XML header, content on the first line that doesn't close properly.
 * Example:
 *      <?xml version="1.0" encoding="UTF-8"?><fragment><extension
 *     //Copy-right comment is executed correctly, '<fragment ..' is put onto new line.
 *
 * [x] Document with XML header, copyright on 2nd line, stuff.
 * [x] test with non-IBM header.
 * 2014.07.15 tested.
 *
 */

/**
 * <h2> XML File handler. </h2>
 *
 * <p> This class deals with the special case of 'xml' files.</p>
 *
 * <p> * If an xml header exists for example: {@code <?xml version="1.0" encoding="UTF-8"?> } <br>
 * then the copyright comment is inserted exactly at the end of it, on a new line, <br>
 * moving any content down below the xml header. </p>
 *
 * <p> It does take into account multiple headers also. e.g:<br>
 * {@code <?xml version="1.0" encoding="UTF-8"?>}<br>
   {@code <?eclipse version="3.2"?> }<br>
   In this case, the copy right comment is inserted below the last header. </p>

 * <p> If no xml header exists, then the copyright comment is inserted at the top of the xml file.<p>
 */
public class XmlFile extends SourceFile {

	public XmlFile(IFile file) {
		super(file);
	}

	/**
	 * Deals with the fact that XML files can start with a header and the copy <br>
	 * right comment can start at the end of the header. <br>
	 *
	 * <p> For example: {@code <?xml?> <!-- } </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCommentStart(String aLine) {
		return aLine.trim().contains(getCommentStart());

		//Note, above is a bit different from parent, contains/startswithd:
		//Parent:
		//return aLine.trim().STARTSWITH(getCommentStart());

	}

	/**
	 * Deals with the fact that XML files can end with a header and the copy <br>
	 * right comment can start at the end of the header. <br>
	 *
	 * <p> For exaple: {@code <?xml?> <!-- } </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCommentEnd(String aLine, String commentStartString) {
		return aLine.trim().contains(getCommentEnd());
		//Similarly, uses 'contains' instead of 'starts with'
	}

	@Override
	public String getCommentStart() {
		return "<!--"; //$NON-NLS-1$
	}

	@Override
	public String getCommentEnd() {
		return "-->"; //$NON-NLS-1$
	}

	@Override
	public int getFileType() {
		return CopyrightComment.XML_COMMENT;
	}

	/**
	 * Given the new constructed copyright comment, it inserts it into the the document.
	 *
	 * <p> Note, this is only called if inserting an actual comment.<br>
	 *  If only updating the year, this method is not called. </p>
	 * @see org.eclipse.releng.tools.SourceFile#doInsert(java.lang.String, org.eclipse.jface.text.IDocument)
	 */
	@Override
	protected void doInsert(final String comment, IDocument document) throws BadLocationException, IOException {

		//----------------- XML COMMENT CLEAN UP
		// XML comments need extra-tidy up because we need to consider the existance of an XML header
		String tidyComment = comment.trim();

		//Append new-line at the end for cleaner look.
		tidyComment += '\n';

		// check for existance of an xml header (<?xml)
		// If so, put the comment 'below' it.
		// example:
		//<?xml .... ?>
		//<--
		//    comment start....
		if (containsXmlEncoding(document)) {
			// If encoding is present, pre-append a new line.
			tidyComment = '\n' + tidyComment;
		}

		//------------------ COMMENT INSERT
		// find insert offset (we must skip instructions)
		int insertOffset = findInsertOffset(document);

		// insert comment
		document.replace(insertOffset, 0, tidyComment);
	}

	/**
	 * Given the document, find the place after the xml header to insert the comment.
	 * @param document
	 * @return
	 * @throws BadLocationException
	 */
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

					//Offset is '+2' not '+1' because of '?' in '?>'
					insertOffset = offset + 2;
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

	/**
	 * Find out if an XML document contains an XML meta header.
	 *
	 * <p> XML documents <b> sometimes </b> contain a header specifying various attributes such as  <br>
	 * version, encoding etc... </p>
	 *
	 * <p> Examples include: <br>
	 * {@literal <?xml version="1.0" encoding="UTF-8"?> }<br>
	 * {@literal<?xml version="1.0" encoding="UTF-8" standalone="no"?> } <br>
	 * {@literal <?xml version="1.0" ?> } </p>
	 *
	 * @param xmlDoc
	 * @return             True if it contains a header.
	 * @throws BadLocationException
	 */
	public boolean containsXmlEncoding(IDocument xmlDoc) throws BadLocationException {

		//XML attribute headers *must* reside on the first line.
		//We identify if the xml document contains a header by checking the first tag and see if it starts with: <?xml

		//-- Check to see if the document is long enough to contain the minimum '<?xml?>' tag
		if (xmlDoc.getLength() < 7) {
			return false;
		}

		for (int offset = 0; offset < xmlDoc.getLength(); offset++) {

			//Read Char.
			char c = xmlDoc.getChar(offset);

				// ignore whitespace and new lines
				if(Character.isWhitespace(c)) {
						continue;
				}

				//Once we've found the first '<', check that it's a '<?xml'
				if (c == '<') {

					//check that document is long enough to close a header if we are to read it: '<?xml
					if ((offset + 4) < xmlDoc.getLength()) {

						//Read "<?xml" equivalent.
						String xmlTag = "" + c + xmlDoc.getChar(offset+1) + xmlDoc.getChar(offset+2) + //$NON-NLS-1$
												 xmlDoc.getChar(offset+3) + xmlDoc.getChar(offset+4);

						if ( xmlTag.compareToIgnoreCase("<?xml") == 0) { //$NON-NLS-1$
							return true;
						} else {
							return false;
						}
					}
				}
		}

		//if parsing an empty xml document, return false.
		return false;
	}

}
