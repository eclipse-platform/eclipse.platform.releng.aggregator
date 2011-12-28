/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.internal.versioning;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.osgi.util.NLS;

/**
 * Helper class for dealing with class files.
 */
public class ClassFileHelper implements VersionCompareConstants {

	/**
	 * Return a class file reader based on the given object or <code>null</code>.
	 */
	public static IClassFileReader getReader(Object object) throws CoreException {
		if (object instanceof String)
			return getClassReaderFromFile(new File((String) object));
		if (object instanceof File)
			return getClassReaderFromFile((File) object);
		if (object instanceof URL)
			return getClassReaderFromURL((URL) object);
		if (object instanceof InputStream) {
			IClassFileReader fileReader = ToolFactory.createDefaultClassFileReader((InputStream) object, IClassFileReader.ALL);
			return fileReader;
		} else if (object instanceof IClassFileReader)
			return (IClassFileReader) object;
		else
			// otherwise throw CoreException
			throw new CoreException(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING, NLS.bind(Messages.JavaClassVersionCompare_unexpectedTypeMsg, object.getClass().getName()), null));
	}

	/**
	 * gets IClassFileReader of the java class denoted by <code>file</code>
	 * @param file java class file
	 * @return IClassFileReader instance or <code>null</code>if file type is wrong or any error occurred
	 */
	private static IClassFileReader getClassReaderFromFile(File file) {
		if (isGivenTypeFile(file, CLASS_FILE_EXTENSION))
			return ToolFactory.createDefaultClassFileReader(file.getAbsolutePath(), IClassFileReader.ALL_BUT_METHOD_BODIES);
		if (isGivenTypeFile(file, JAVA_FILE_EXTENSION))
			return null;
		return null;
	}

	/**
	 * Checks whether or not the given file potentially represents a given type file on the file-system.
	 * 
	 * @param file File instance which denotes to a file on the file-system
	 * @param type file type(e.g. "java","class")
	 * @return <code>true</code> <code>file</code> exists and is a configuration file,
	 *         <code>false</code> otherwise
	 */
	private static boolean isGivenTypeFile(File file, String type) {
		return file.isFile() && file.getName().endsWith(type);
	}

	/**
	 * Return a class file reader based on the contents of the given URL's input stream.
	 * 
	 * @param url location of the class file
	 * @return the class file reader or <code>null</code>if any error occurred
	 */
	private static IClassFileReader getClassReaderFromURL(URL url) {
		try {
			InputStream input = url.openStream();
			try {
				return ToolFactory.createDefaultClassFileReader(input, IClassFileReader.ALL_BUT_METHOD_BODIES);
			} finally {
				if (input != null) {
					input.close();
					input = null;
				}
			}
		} catch (IOException e) {
			//ignore, result will be checked outside
		}
		return null;
	}

}
