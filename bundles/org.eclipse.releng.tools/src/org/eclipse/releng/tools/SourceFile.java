package org.eclipse.releng.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author droberts
 */
public abstract class SourceFile {
	
	IFile file;
	List comments = new ArrayList();
	StringWriter contents = new StringWriter();
	private String newLine = System.getProperty("line.separator");

	/**
	 * @param file
	 */
	public SourceFile(IFile file) {
		super();
		this.file = file;
		initialize();
	}

	public abstract String getCommentStart();
	public abstract String getCommentEnd();
	
	/**
	 * 
	 */
	private void initialize() {
		
		InputStream inputStream;
		try {
			inputStream = file.getContents(false);
			BufferedReader aReader = new BufferedReader(new InputStreamReader(inputStream));
			String aLine = aReader.readLine();
			String comment = "";
			BufferedWriter contentsWriter = new BufferedWriter(contents);
			int lineNumber = 0;
			int commentStart = 0;
			int commentEnd = 0;
			boolean inComment = false;
			
			while (aLine != null) {
				contentsWriter.write(aLine);
				contentsWriter.newLine();
				if (!inComment && aLine.trim().startsWith(getCommentStart())) {
					// start saving comment
					inComment = true;
					commentStart = lineNumber;
				}
				
				if (inComment) {
					comment = comment + aLine + newLine;
								
					if (aLine.trim().endsWith(getCommentEnd()) && commentStart != lineNumber) {
						// stop saving comment
						inComment = false;
						commentEnd = lineNumber;
						BlockComment aComment = new BlockComment(commentStart, commentEnd, comment.toString(), getCommentStart(), getCommentEnd());
						comments.add(aComment);
						comment = "";
						commentStart = 0;
						commentEnd = 0;
					}
				}
				
				aLine = aReader.readLine();
				lineNumber++;
			}
			
			aReader.close();
		} catch (CoreException e) {
			e.printStackTrace();
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

	/**
	 * @param string
	 */
	public void insert(String string) {
		
		InputStream fileStream;
		try {
			fileStream = file.getContents();
			ByteArrayOutputStream result = new ByteArrayOutputStream();
		
			result.write(string.getBytes());
			int aByte = fileStream.read();
			while (aByte != -1) {
				result.write(aByte);
				aByte = fileStream.read();
			}
		
			fileStream.close();
			ByteArrayInputStream writeMe = new ByteArrayInputStream(result.toByteArray());
			file.setContents(writeMe, IFile.KEEP_HISTORY, new NullProgressMonitor());
		
			result.close();
			writeMe.close();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	/**
	 * @return BlockComment
	 */
	public BlockComment firstCopyrightComment() {
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
			InputStream fileStream = file.getContents();
			ByteArrayOutputStream result = new ByteArrayOutputStream();
		
			result.write(string.getBytes());
			
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileStream));

			for (int i = 0; i < aComment.start; i++) {
				String aLine = fileReader.readLine();
				result.write(aLine.getBytes());
				result.write(newLine.getBytes());
			}
			
			for (int i = aComment.start; i < aComment.end + 1; i++) {
				String aLine = fileReader.readLine();
			}
			
			String aLine = fileReader.readLine();
			while (aLine != null) {
				result.write(aLine.getBytes());
				result.write(newLine.getBytes());
				aLine = fileReader.readLine();
			}
			
			fileStream.close();
			
			ByteArrayInputStream writeMe = new ByteArrayInputStream(result.toByteArray());
			file.setContents(writeMe, IFile.KEEP_HISTORY, new NullProgressMonitor());
		
			result.close();
			writeMe.close();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
}
