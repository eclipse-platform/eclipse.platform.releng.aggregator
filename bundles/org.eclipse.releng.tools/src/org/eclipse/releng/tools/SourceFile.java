/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Oberhuber (Wind River) - [235572] detect existing comments in bat files
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.osgi.util.NLS;


/**
 * @author droberts
 */
public abstract class SourceFile {
	
	IFile file;
	List comments = new ArrayList();
	StringWriter contents = new StringWriter();
	private ITextFileBufferManager textFileBufferManager;

	public static SourceFile createFor(IFile file) {
        String extension = file.getFileExtension();
        if (extension != null) {
	        extension = extension.toLowerCase();
			if (extension.equals("java")) { //$NON-NLS-1$
				return new JavaFile(file);
	        } else if (extension.equals("c") || extension.equals("h") || extension.equals("rc") || extension.equals("cc") || extension.equals("cpp")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	            return new CFile(file);
			} else if (extension.equals("properties")) { //$NON-NLS-1$
				return new PropertiesFile(file);
	        } else if (extension.equals("sh") || extension.equals("csh") || extension.equals("mak") || extension.equals("pl") || extension.equals("tcl")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	            return new ShellMakeFile(file);
	        } else if (extension.equals("bat")) { //$NON-NLS-1$
	            return new BatFile(file);
			} else if (extension.equals("js")) { //$NON-NLS-1$
	            return new JavaScriptFile(file);
//			} else if (extension.equals("xml")) { //$NON-NLS-1$
//	            return new XmlFile(file);
			}
        }
		return null;
	}
	
	public SourceFile(IFile file) {
		super();
		this.file = file;
		initialize();
	}

	/**
	 * Test if the given line marks the start of a potential Copyright comment.
	 * Can be overridden in subclasses to perform advanced detection.
	 * @param aLine a line of text to check
	 * @return <code>true</code> if the line can mark a copyright comment start.
	 * @since 3.5
	 */
	public boolean isCommentStart(String aLine) {
		return aLine.trim().startsWith(getCommentStart());
	}
	/**
	 * Test if the given line marks the end of a potential Copyright comment.
	 * Can be overridden in subclasses to perform advanced detection.
	 * @param aLine a line of text to check
	 * @param commentStartString the line which started the block comment
	 * @return <code>true</code> if the line can mark a copyright comment end.
	 * @since 3.5
	 */
	public boolean isCommentEnd(String aLine, String commentStartString) {
		return aLine.trim().endsWith(getCommentEnd());
	}
	public abstract String getCommentStart();
	public abstract String getCommentEnd();
	

	private void initialize() {
		textFileBufferManager= FileBuffers.createTextFileBufferManager();
		try {
			ITextFileBuffer fileBuffer= getFileBuffer();
			if (fileBuffer == null)
				return;
			
			IDocument document;
			try {
				document= fileBuffer.getDocument();
			} finally {
				try {
					textFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, null);
				} catch (CoreException e) {
					e.printStackTrace();
					// continue as we were able to get the file buffer and its document
				}
			}
			
			String newLine= TextUtilities.getDefaultLineDelimiter(document);
			BufferedReader aReader = new BufferedReader(new StringReader(document.get()));
			String aLine = aReader.readLine();
			String comment = ""; //$NON-NLS-1$
			BufferedWriter contentsWriter = new BufferedWriter(contents);
			int lineNumber = 0;
			int commentStart = 0;
			int commentEnd = 0;
			boolean inComment = false;
			String commentStartString = ""; //$NON-NLS-1$
			
			while (aLine != null) {
				contentsWriter.write(aLine);
				contentsWriter.newLine();
				if (!inComment && isCommentStart(aLine)) {
					// start saving comment
					inComment = true;
					commentStart = lineNumber;
					commentStartString = aLine;
				}
				
				if (inComment) {
					comment = comment + aLine + newLine;
								
					if (isCommentEnd(aLine, commentStartString) && commentStart != lineNumber) {
						// stop saving comment
						inComment = false;
						commentEnd = lineNumber;
						String commentEndString = aLine.trim();
						commentEndString = commentEndString.substring(commentEndString.length()-2);
						BlockComment aComment = new BlockComment(commentStart, commentEnd, comment.toString(), commentStartString, commentEndString);
						comments.add(aComment);
						comment = ""; //$NON-NLS-1$
						commentStart = 0;
						commentEnd = 0;
						commentStartString = ""; //$NON-NLS-1$
					}
				}
				
				aLine = aReader.readLine();
				lineNumber++;
			}
			
			aReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return BlockComment
	 */
	public BlockComment firstBlockComment() {
		if (comments.isEmpty()) {
			return null;
		} else {
			return (BlockComment) comments.get(0);
		}
	}

	private ITextFileBuffer getFileBuffer() {
		try {
			textFileBufferManager.connect(file.getFullPath(), LocationKind.IFILE, null);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}

		ITextFileBuffer fileBuffer= textFileBufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
		if (fileBuffer != null)
			return fileBuffer;

		System.err.println(NLS.bind(Messages.getString("SourceFile.0"), file.getFullPath())); //$NON-NLS-1$
			return null;
	}
	
	/**
	 * @param string
	 */
	public void insert(String string) {
		ITextFileBuffer fileBuffer= getFileBuffer();
		if (fileBuffer == null)
			return;

		try {
			IDocument document= fileBuffer.getDocument();
			doInsert(string, document);
			fileBuffer.commit(null, false);
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				textFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	protected void doInsert(String comment, IDocument document) throws BadLocationException, CoreException, IOException {
		document.replace(0, 0, comment);
	}

	/**
	 * @return BlockComment
	 */
	public BlockComment getFirstCopyrightComment() {
		Iterator anIterator = comments.iterator();
		while (anIterator.hasNext()) {
			BlockComment aComment = (BlockComment) anIterator.next();
			if (aComment.isCopyright()) {
				return aComment;
			}
		}
		
		return null;
	}

	/**
	 * @param firstCopyrightComment
	 * @param string
	 */
	public void replace(BlockComment aComment, String string) {
		
	
		try {
			ITextFileBuffer fileBuffer= getFileBuffer();
			if (fileBuffer == null)
				return;
			
			IDocument document= fileBuffer.getDocument();

			IRegion startLine= document.getLineInformation(aComment.start);
			IRegion endLine= document.getLineInformation(aComment.end + 1);
			document.replace(startLine.getOffset(), endLine.getOffset() - startLine.getOffset(), string);
			
			fileBuffer.commit(null, false);
		
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} finally  {
			try {
				FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.IFILE, null);
			} catch (CoreException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * @return boolean
	 */
	public boolean hasMultipleCopyrights() {
		int count = 0;
		Iterator anIterator = comments.iterator();
		while (anIterator.hasNext()) {
			BlockComment aComment = (BlockComment) anIterator.next();
			if (aComment.isCopyright()) {
				count++;
			}
			if (count > 1) {
				return true;
			}
		}
		return false;
	}

	public abstract int getFileType();
}
