/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class CodeFormatterExample {

	private static final int DEFAULT_READING_SIZE = 8192;
	private static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$
	private final static String UTF_8 = "UTF-8";	//$NON-NLS-1$

	/**
	 * Returns the given input stream's contents as a character array.
	 * If a length is specified (ie. if length != -1), this represents the number of bytes in the stream.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding)
		throws IOException {
		InputStreamReader reader = null;
		try {
			reader = encoding == null
						? new InputStreamReader(stream)
						: new InputStreamReader(stream, encoding);
		} catch (UnsupportedEncodingException e) {
			// encoding is not supported
			reader =  new InputStreamReader(stream);
		}
		char[] contents;
		int totalRead = 0;
		if (length == -1) {
			contents = CharOperation.NO_CHAR;
		} else {
			// length is a good guess when the encoding produces less or the same amount of characters than the file length
			contents = new char[length]; // best guess
		}

		while (true) {
			int amountRequested;
			if (totalRead < length) {
				// until known length is met, reuse same array sized eagerly
				amountRequested = length - totalRead;
			} else {
				// reading beyond known length
				int current = reader.read(); 
				if (current < 0) break;
				
				amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);  // read at least 8K
				
				// resize contents if needed
				if (totalRead + 1 + amountRequested > contents.length)
					System.arraycopy(contents, 	0, 	contents = new char[totalRead + 1 + amountRequested], 0, totalRead);
				
				// add current character
				contents[totalRead++] = (char) current; // coming from totalRead==length
			}
			// read as many chars as possible
			int amountRead = reader.read(contents, totalRead, amountRequested);
			if (amountRead < 0) break;
			totalRead += amountRead;
		}

		// Do not keep first character for UTF-8 BOM encoding
		int start = 0;
		if (totalRead > 0 && UTF_8.equals(encoding)) {
			if (contents[0] == 0xFEFF) { // if BOM char then skip
				totalRead--;
				start = 1;
			}
		}
		
		// resize contents if necessary
		if (totalRead < contents.length)
			System.arraycopy(contents, start, contents = new char[totalRead], 	0, 	totalRead);

		return contents;
	}

	private static char[] getFileCharContent(File file, String encoding) throws IOException {
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return getInputStreamAsCharArray(stream, (int) file.length(), encoding);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: CodeFormatterExample <path_to_file>"); //$NON-NLS-1$
			return;
		}
		// take default Eclipse formatting options
		Map options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		
		// initialize the compiler settings to be able to format 1.5 code
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		
		// change the option to wrap each enum constant on a new line
		options.put(
			DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
			DefaultCodeFormatterConstants.createAlignmentValue(
				true,
				DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
				DefaultCodeFormatterConstants.INDENT_ON_COLUMN));
		
		// instanciate the default code formatter with the given options
		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
		
		// retrieve the source to format
		String source = null;
		try {
			source = new String(getFileCharContent(new File(args[0]), null));
		} catch (IOException e) {
			System.err.println("Could not retrieve the source"); //$NON-NLS-1$
			e.printStackTrace();
			return;
		}
		final TextEdit edit = codeFormatter.format(
			CodeFormatter.K_COMPILATION_UNIT, // format a compilation unit
			source, // source to format
			0, // starting position
			source.length(), // length
			0, // initial indentation
			LINE_SEPARATOR // line separator
		);
		
		IDocument document = new Document(source);
		try {
			edit.apply(document);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		// display the formatted string on the System out
		System.out.println(document.get());
	}
}
