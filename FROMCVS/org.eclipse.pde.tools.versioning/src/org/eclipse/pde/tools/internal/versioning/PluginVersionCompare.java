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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;

/**
 * PluginVersionCompare
 */
public class PluginVersionCompare implements VersionCompareConstants {
	private final static String CLASS_NAME = "PluginVersionCompare"; //$NON-NLS-1$
	private final static int NEW_CLASS = 0;
	private final static int DELETED_CLASS = 1;
	private final static int CREATE_FILE_TIMES = 5;
	// MultiStatus instance used to store error or warning messages
	private MultiStatus finalResult;
	// JavaClassVersionCompare instance
	JavaClassVersionCompare classCompare;
	//	
	private boolean hasMajorChange;
	private boolean hasMinorChange;
	private boolean hasMicroChange;
	private boolean hasError;
	// for debug
	private long startTime;
	private boolean DEBUG = false;
	private static final String DEBUG_OPTION = VersionCompareConstants.PLUGIN_ID + "/debug/plugins"; //$NON-NLS-1$

	/**
	 * constructor
	 */
	public PluginVersionCompare() {
		super();
		classCompare = new JavaClassVersionCompare();
		DEBUG = Activator.getBooleanDebugOption(DEBUG_OPTION);
	}

	/*
	 * Log the given debug message.
	 */
	private void debug(String message) {
		if (DEBUG) {
			Activator.debug(message);
		}
	}

	/**
	 * Compares the two given plug-ins which each other and reports a status object indicating
	 * whether or not the version number of the plug-ins have been incremented appropriately
	 * based on the relative changes.
	 * 
	 * <p>
	 * The plug-in parameters can be instances of any of the following types:
	 * <ul>
	 * <li>{@link java.lang.String} - which denotes the plug-in's jar file name or directory
	 * <li>{@link java.net.URL} - which denotes the plug-in's jar file name or directory
	 * <li>{@link ManifestElement}[] - which denotes the manifest elements of Bundle_ClassPath of <code>plugin1</code>
	 * <li>{@link java.io.File} - which denotes the plug-in's jar file name or directory
	 * <li>{@link ManifestElement}[] - which denotes the manifest elements of Bundle_ClassPath of <code>plugin2</code> 
	 * </ul>
	 * </p>
	 * 
	 * @param plugin1 a plug-in reference
	 * @param elements1 Bundle_ClassPath of <code>plugin1</code>
	 * @param plugin2 a plug-in reference
	 * @param elements2 Bundle_ClassPath of <code>plugin2</code>
	 * @param status a MultiStatus instance to carry out compare information 
	 * @param monitor IProgressMonitor instance which will monitor the progress during plugin comparing
	 * @return int number which indicates compare result, it could be MAJOR_CHANGE,MINOR_CHANGE,MICRO_CHANGE,or NO_CHANGE
	 * @throws CoreException if the parameters are of an incorrect type or if an error occurred during the comparison
	 */
	protected int checkPluginVersions(MultiStatus status, Object plugin1, ManifestElement[] elements1, Object plugin2, ManifestElement[] elements2, IProgressMonitor monitor) throws CoreException {
		try {
			monitor = VersioningProgressMonitorWrapper.monitorFor(monitor);
			monitor.beginTask(Messages.PluginVersionCompare_comparingPluginMsg, 100);
			finalResult = status;
			// convert input objects into Files
			File file1 = convertInputObject(plugin1);
			File file2 = convertInputObject(plugin2);
			startTime = System.currentTimeMillis();
			// initialize flags	
			hasError=false;
			hasMajorChange = false;
			hasMinorChange = false;
			hasMicroChange = false;
			// get class URL Tables
			Map classFileURLTable1 = null;
			Map classFileURLTable2 = null;
			classFileURLTable1 = generateClassFileURLTableFromFile(file1, elements1);
			// worked 1%
			monitor.worked(1);
			classFileURLTable2 = generateClassFileURLTableFromFile(file2, elements2);
			// worked 1%
			monitor.worked(1);
			//
			return checkPluginVersions(file1.getName(), classFileURLTable1, classFileURLTable2, new SubProgressMonitor(monitor, 98));
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkPluginVersions(String, String, IProgressMonitor)
	 */
	public int checkPluginVersions(MultiStatus status, Object plugin1, Object plugin2, IProgressMonitor monitor) throws CoreException {
		try {
			monitor = VersioningProgressMonitorWrapper.monitorFor(monitor);
			monitor.beginTask(Messages.PluginVersionCompare_comparingPluginMsg, 100);
			finalResult = status;
			// convert input objects into Files
			File file1 = convertInputObject(plugin1);
			File file2 = convertInputObject(plugin2);
			startTime = System.currentTimeMillis();
			// initialize flags	
			hasError=false;
			hasMajorChange = false;
			hasMinorChange = false;
			hasMicroChange = false;
			// get class URL Tables
			Map classFileURLTable1 = null;
			Map classFileURLTable2 = null;
			classFileURLTable1 = generateClassFileURLTable(file1);
			// worked 2%
			monitor.worked(2);
			classFileURLTable2 = generateClassFileURLTable(file2);
			// worked 2%
			monitor.worked(2);
			// check plugin version ( 96% workload)
			return checkPluginVersions(file1.getName(), classFileURLTable1, classFileURLTable2, new SubProgressMonitor(monitor, 96));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Compares the corresponding classes denoted by URLS in <code>classFileURLTable1</code> to those in <code>classFileURLTable2</code>
	 * @param pluginName name of plugin
	 * @param classFileURLTable1 contains URL lists
	 * @param classFileURLTable2 contains URL lists
	 * @param monitor IProgressMonitor instance
	 * @return change with the highest priority
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private int checkPluginVersions(String pluginName, Map classFileURLTable1, Map classFileURLTable2, IProgressMonitor monitor) {
		// if both tables are empty, we treat it as no change
		if (classFileURLTable1.size() == 0 && classFileURLTable2.size() == 0) {
			debug(NLS.bind(Messages.PluginVersionCompare_finishedProcessPluginMsg, pluginName, String.valueOf(System.currentTimeMillis() - startTime)));
			return IVersionCompare.NO_CHANGE;
		}
		// if size of table1 is 0, size of table2 is not 0, we treat it as major change (same as some class has been deleted)
		if (classFileURLTable1.size() == 0) {
			// check if there is any class no longer exists
			processChangedClasseLists(classFileURLTable1, DELETED_CLASS);
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginMajorChangeMsg, pluginName), null));
			debug(NLS.bind(Messages.PluginVersionCompare_finishedProcessPluginMsg, pluginName, String.valueOf(System.currentTimeMillis() - startTime)));
			return IVersionCompare.MAJOR_CHANGE;
		}
		// if size of table2 is 0, size of table1 is not 0, we treat it as minor change (same as some class has been new added)
		if (classFileURLTable2.size() == 0) {
			// check if there is any class new added
			processChangedClasseLists(classFileURLTable1, NEW_CLASS);
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginMinorChangeMsg, pluginName), null));
			debug(NLS.bind(Messages.PluginVersionCompare_finishedProcessPluginMsg, pluginName, String.valueOf(System.currentTimeMillis() - startTime)));
			return IVersionCompare.MINOR_CHANGE;
		}
		// compare classes
		compareClasses(classFileURLTable1, classFileURLTable2, monitor);
		// delete temporary directory
		deleteTmpDirectory();
		//
		debug(NLS.bind(Messages.PluginVersionCompare_finishedProcessPluginMsg, pluginName, String.valueOf(System.currentTimeMillis() - startTime)));
		// analysis result
		if (hasError) {
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginErrorOccurredMsg, pluginName), null));
			return IVersionCompare.ERROR_OCCURRED;
		}
		if (hasMajorChange) {
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginMajorChangeMsg, pluginName), null));
			return IVersionCompare.MAJOR_CHANGE;
		}
		if (hasMinorChange) {
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginMinorChangeMsg, pluginName), null));
			return IVersionCompare.MINOR_CHANGE;
		}
		if (hasMicroChange) {
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_OVERALL_STATUS, NLS.bind(Messages.PluginVersionCompare_pluginMicroChangeMsg, pluginName), null));
			return IVersionCompare.MICRO_CHANGE;
		}
		return IVersionCompare.NO_CHANGE;
	}

	/**
	 * compares corresponding classes indicated by the URLs in two maps
	 * @param classFileURLTable1 
	 * @param classFileURLTable2
	 * @param monitor IProgressMonitor instance
	 */
	private void compareClasses(Map classFileURLTable1, Map classFileURLTable2, IProgressMonitor monitor) {
		try {
			monitor.beginTask("", classFileURLTable1.size() + 1); //$NON-NLS-1$
			for (Iterator iterator1 = classFileURLTable1.keySet().iterator(); iterator1.hasNext();) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				Object key = iterator1.next();
				// get URL lists of the same class file name
				List value1 = (List) classFileURLTable1.get(key);
				List value2 = (List) classFileURLTable2.get(key);
				if (value2 == null) {
					processChangedClasses(value1.toArray(), NEW_CLASS);
					continue;
				}
				List classFileReaderList1 = generateClassFileReaderList(value1);
				List classFileReaderList2 = generateClassFileReaderList(value2);
				compareClasses(classFileReaderList1, classFileReaderList2, new SubProgressMonitor(monitor, 1));
				// delete the value from classFileURLTable2
				classFileURLTable2.remove(key);
			}
			// check if there is any class no longer exists
			processChangedClasseLists(classFileURLTable2, DELETED_CLASS);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	/**
	 * compares IClassFileReader instances in <code>list1</code> to the corresponding one
	 * in <code>list2</code>
	 * @param list1 contains IClassFileReader instances
	 * @param list2 contains IClassFileReader instances
	 * @param monitor IProgressMonitor instance
	 */
	private void compareClasses(List list1, List list2, IProgressMonitor monitor) {
		try {
			monitor.beginTask("", list1.size() + 1); //$NON-NLS-1$
			for (Iterator classIterator1 = list1.iterator(); classIterator1.hasNext();) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				IClassFileReader classFileReader1 = (IClassFileReader) classIterator1.next();
				String className1 = charsToString(classFileReader1.getClassName());
				Iterator classIterator2;
				boolean beCompared = false;
				for (classIterator2 = list2.iterator(); classIterator2.hasNext();) {
					IClassFileReader classFileReader2 = (IClassFileReader) classIterator2.next();
					if (className1.equals(charsToString(classFileReader2.getClassName()))) {
						// compare two IClassFileReader instances and merge the result into finalResult
						try {
							processed(classCompare.checkJavaClassVersions(finalResult, classFileReader1, classFileReader2, new SubProgressMonitor(monitor, 1)));
						} catch (CoreException ce) {
							finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_errorWhenCompareClassesMsg, charsToString(classFileReader1.getClassName())), null));
						}
						list2.remove(classFileReader2);
						beCompared = true;
						break;
					}
				}
				if (!beCompared && shouldProcess(classFileReader1)) {
					// new added class
					finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_DETAIL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.PluginVersionCompare_destinationClassNotFoundMsg, charsToString(classFileReader1.getClassName())), null));
				}
			}
			// no longer exist classes
			for (Iterator iterator2 = list2.iterator(); iterator2.hasNext();) {
				IClassFileReader classFileReader = (IClassFileReader) iterator2.next();
				if (shouldProcess(classFileReader))
					finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_DETAIL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.PluginVersionCompare_sourceClassNotFoundMsg, charsToString(classFileReader.getClassName())), null));
			}
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	/**
	 * processes no long existed classe lists contained in <code>map</code>
	 * @param map Map which contains class name and URL list pairs which no longer exist in the new plugin
	 */
	private void processChangedClasseLists(Map map, int flag){
		for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
			List list = (List) iterator.next();
			processChangedClasses(list.toArray(), flag);
		}
	}
	
	/**
	 * process classes indicated by URL instances in <code>classArray</code> depending on <code>flag</code>
	 * if <code>flag</code> is NEW_CLASS, all the IClassFileReader instances in <code>classArray</code> are new added classes
	 * if <code>flag</code> is DELETED_CLASS, all the IClassFileReader instances in <code>classArray</code> are classes no longer exist
	 * @param classArray contains URL instances
	 * @param flag could be NEW_CLASS or DELETED_CLASS
	 */
	private void processChangedClasses(Object[] classArray, int flag) {
		if (classArray.length == 0)
			return;
		for (int i = 0; i < classArray.length; i++) {
			if (!(classArray[i] instanceof URL))
				continue;
			try {
				IClassFileReader classFileReader = ClassFileHelper.getReader(classArray[i]);
				if (classFileReader == null) {
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_couldNotReadClassMsg, ((URL) classArray[i]).getFile()), null));
					continue;
				}
				if (shouldProcess(classFileReader) && shouldProcess(charsToString(classFileReader.getClassName())))
					if (flag == NEW_CLASS)
						// new added class
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_OVERALL_STATUS | IVersionCompare.MINOR_CHANGE, NLS.bind(Messages.PluginVersionCompare_destinationClassNotFoundMsg, charsToString(classFileReader.getClassName())), null));
					else if (flag == DELETED_CLASS)
						// deleted class
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.CLASS_OVERALL_STATUS | IVersionCompare.MAJOR_CHANGE, NLS.bind(Messages.PluginVersionCompare_sourceClassNotFoundMsg, charsToString(classFileReader.getClassName())), null));
			} catch (CoreException e) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_couldNotReadClassMsg, ((URL) classArray[i]).getFile()), e));
			}
		}
	}

	/**
	 * generates a List which stores IClassFileReader instances indicated by URLs in <code>list</code>
	 * 
	 * @param list contains URL instances
	 * @return List contains IClassFileReader instances
	 */
	private List generateClassFileReaderList(List list) {
		ArrayList newList = new ArrayList(0);
		if (list == null || list.size() == 0)
			return newList;
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			try {
				IClassFileReader classFileReader = ClassFileHelper.getReader(iterator.next());
				if (classFileReader == null)
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_couldNotReadClassMsg, ((URL) iterator.next()).getFile()), null));
				else
					newList.add(classFileReader);
			} catch (CoreException e) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_couldNotReadClassMsg, ((URL) iterator.next()).getFile()), null));
			}
		}
		return newList;
	}

	/**
	 * generates Map containing lists of URL instances indicates java classes
	 * which are under a plugin directory or included in a plugin jar file denoted by <code>file</code>.
	 * key is simple name of class file e.g. Foo.class
	 * value is a List which contains URLs pointing to classes which have the same simple file name 
	 * 
	 * @param file File instance which denotes a plugin directory or a plugin jar file
	 * @return Map containing URL instances
	 * @throws CoreException <p>if any nested CoreException has been caught</p>				
	 */
	private Map generateClassFileURLTable(File file) throws CoreException {
		// get bundle class path
		ManifestElement[] elements = null;
		try {
			elements = (ManifestElement[]) ManifestHelper.getElementsFromManifest(file, new String[] {BUNDLE_CLASSPATH}).get(BUNDLE_CLASSPATH);
		} catch (CoreException ce) {
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_couldNotReadClassPathMsg, file.getAbsolutePath()), null));
		}
		return generateClassFileURLTableFromFile(file, elements);
	}

	/**
	 * converts input Object into File instance
	 * @param object input object
	 * @return File which represents <code>object</code>
	 * @throws CoreException <p>if type of <code>object</code> is unexpected</p> 
	 * 					     <p>or <code>object</code> does not represent an exist file or directory
	 */
	private File convertInputObject(Object object) throws CoreException {
		File file;
		if (object instanceof String) {
			// if object is a String
			file = new File((String) object);
		} else if (object instanceof URL)
			// if object is an URL
			file = new File(((URL) object).getFile());
		else if (object instanceof File)
			// if object is a File
			file = (File) object;
		else
			// otherwise throw CoreException
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.JavaClassVersionCompare_unexpectedTypeMsg, object.getClass().getName()), null));
		if (file.exists())
			return file;
		throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.PluginVersionCompare_inputNotExistMsg, file.getAbsolutePath()), null));
	}

	/**
	 * generates Map containing lists of URL instances indicates java classes
	 * which are under a plugin directory or included in a plugin jar file denoted by <code>file</code>.
	 * key is simple name of class file e.g. Foo.class
	 * value is a List which contains URLs pointing to classes which have the same simple file name 
	 * 
	 * @param file File instance which denotes a plugin directory or a plugin jar file
	 * @param elements ManifestElement array of class path attribute
	 * @return Map containing URL instances
	 * @throws CoreException <p>if any nested CoreException has been caught</p>						
	 */
	private Map generateClassFileURLTableFromFile(File file, ManifestElement[] elements) throws CoreException {
		Map table = new Hashtable(0);
		if (file.isFile())
			// if file is a file
			getClassURLsFromJar(table, file, elements);
		else if (file.isDirectory())
			// if file is a directory
			getClassURLsFromDir(table, file, elements);
		if (table.size() == 0)
			finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.PluginVersionCompare_noValidClassFoundMsg, file.getAbsolutePath()), null));
		return table;
	}

	/**
	 * gets URL instances indicates java classes which are included in a plugin jar file denoted by <code>file</code>.
	 * and set them into <code>map</code>
	 * key is simple name of class file e.g. Foo.class
	 * value is a List which contains URLs pointing to classes which have the same simple file name 
	 * 
	 * @param map Map used to store URL instances
	 * @param file File instance which denotes a jar file
	 * @param elements ManifestElement array of class path attribute
	 * @throws CoreException <p>if jar file could not be loaded successfully</p>
	 * 						 <p>or any nested CoreException has been caught</p>
	 * 						
	 */
	private void getClassURLsFromJar(Map map, File file, ManifestElement[] elements) throws CoreException {
		// check if file is a jar file
		if (!isGivenTypeFile(file, JAR_FILE_EXTENSION))
			return;
		JarFile jarFile;
		try {
			jarFile = new JarFile(file);
		} catch (IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.PluginVersionCompare_couldNotOpenJarMsg, file.getAbsolutePath()), ioe));
		}
		if (elements == null)
			// if elements is null, we consider the class path as "."
			getAllClassURLsInJar(map, jarFile);
		else {
			// check elements
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getValue().equals(DOT_MARK))
					getAllClassURLsInJar(map, jarFile);
				else {
					// class path element is supposed to be "." or "*.jar"
					if (!new Path(elements[i].getValue()).getFileExtension().equals(JAR_FILE_EXTENSION)) {
						finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PLUGIN_DETAIL_STATUS  | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_inValidClassPathMsg, elements[i].getValue(), file.getAbsolutePath()), null));
						continue;
					}
					// if the jar entry indicates a nested jar file, we get IClassFileReaders of all the classes in it
					JarEntry entry = jarFile.getJarEntry(elements[i].getValue());
					if (entry != null) {
						JarFile tempJarFile = null;
						// get tmp file indicated by entry
						File tmpFile = getTmpFile(jarFile, entry);
						if (tmpFile == null)
							// if failed to get tmp file, continue to check next element
							continue;
						try {
							// generate a JarFile of the tmp file
							tempJarFile = new JarFile(tmpFile);
						} catch (IOException ioe) {
							Object[] msg = {tmpFile.getAbsoluteFile(), entry.getName(), file.getAbsolutePath()};
							finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failOpenTmpJarMsg, msg), ioe));
							// continue to check next element
							continue;
						}
						// get URLs of all classes in the jar
						getAllClassURLsInJar(map, tempJarFile);
					} else
						finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.PLUGIN_DETAIL_STATUS, NLS.bind(Messages.PluginVersionCompare_classPathJarNotFoundMsg, elements[i].getValue(), file.getAbsolutePath()), null));
				}
			}
		}
		// close jar file
		try {
			jarFile.close();
		} catch (IOException ioe) {
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failCloseJarMsg, jarFile.getName()), ioe));
		}
	}

	/**
	 * delete temporary directory used by this class
	 *
	 */
	private void deleteTmpDirectory() {
		// get java tmp directory
		IPath tmpPath = getJavaTmpPath();
		// find the directory for this plugin
		tmpPath = tmpPath.append(PLUGIN_ID);
		// find the directory for this class
		tmpPath = tmpPath.append(CLASS_NAME);
		// delete all the files and directories
		File tmpDir = tmpPath.toFile();
		deleteTmpFile(tmpDir);
	}

	/**
	 * delete all the files and directories under <code>dir</code>
	 * @param file
	 */
	private void deleteTmpFile(File dir) {
		if (!dir.exists())
			return;
		if (dir.isFile())
			dir.delete();
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				files[i].delete();
			} else
				deleteTmpFile(files[i]);
		}
		dir.delete();
	}

	/**
	 * get URLs of all the classes which is in <code>jar</code>, and
	 * put them into <code>map</code>
	 * @param map map used to store URLs of all the classes
	 * @param jar jar file
	 */
	private void getAllClassURLsInJar(Map map, JarFile jar) {
		// get JarEntrys
		Enumeration enumeration = jar.entries();
		String jarURLString = jar.getName() + JAR_URL_SEPARATOR;
		String classURLString;
		for (; enumeration.hasMoreElements();) {
			JarEntry jarEntry = (JarEntry) enumeration.nextElement();
			// get URL for JarEntry which represents a java class file
			if (!isValidClassJarEntry(jarEntry))
				continue;
			if (!shouldProcess(jarEntry.getName()))
				continue;
			classURLString = jarURLString + jarEntry.getName();
			URL classFileURL = null;
			try {
				classFileURL = new URL(JAR_URL_HEAD + classURLString);
			} catch (MalformedURLException e) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.FeatureModelTable_urlConvertErrorMsg, classURLString), e));
				// do next
				continue;
			}
			// try to get URL list
			IPath entryPath = new Path(jarEntry.getName());
			List urls = (List) map.get(entryPath.lastSegment());
			if (urls == null) {
				urls = new ArrayList(0);
				urls.add(classFileURL);
			} else
				urls.add(classFileURL);
			// put file name and list into map
			map.put(entryPath.lastSegment(), urls);
		}
	}

	/**
	 * get File instance which indicates the full path of the temporary jar file 
	 * which is denoted by <code>jarEntry</code>.
	 * jar file denoted by <code>jarEntry</code> will be extracted to the java
	 * tmp directory and then be returned as a File instance
	 *  
	 * @param jarFile
	 * @param jarEntry
	 * @return File indicates the full path of the temporary jar file or <code>null</code> 
	 * 		   if could not get the tmp file successfully
	 */
	private File getTmpFile(JarFile jarFile, JarEntry jarEntry) {
		// get java tmp directory
		IPath tmpPath = getJavaTmpPath();
		// create a directory for this plugin
		tmpPath = tmpPath.append(PLUGIN_ID);
		// create a directory for this plugin
		tmpPath = tmpPath.append(CLASS_NAME);
		// get path represented by jarEntry
		IPath entryPath = new Path(jarEntry.getName());
		// generate tmp directory
		tmpPath = tmpPath.append(entryPath.removeLastSegments(1));
		File tmpDir = tmpPath.toFile();
		// if the tmp directory does not exist, make it
		if (!tmpDir.exists())
			tmpDir.mkdirs();
		// get OutputStream of the tmp file
		IPath tmpFile = null;
		OutputStream out = null;
		int i = 0;
		while (out == null) {
			// generate the full path of the tmp file(directory/file name)
			tmpFile = tmpPath.append(generateTmpFileName(entryPath.lastSegment()));
			try {
				out = new BufferedOutputStream(new FileOutputStream(tmpFile.toFile()));
			} catch (IOException ioe) {
				i++;
				//we try to create the tmp file for 5 times if any IOException has been caught
				if (i == CREATE_FILE_TIMES) {
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failCreatTmpJarMsg, tmpFile.toOSString()), ioe));
					return null;
				}
			}
		}
		InputStream in = null;
		try {
			try {
				// get InputStream of jarEntry
				in = new BufferedInputStream(jarFile.getInputStream(jarEntry));
			} catch (IOException ioe) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failOpenJarEntryMsg, jarEntry.getName()), ioe));
				return null;
			}
			// extract the file from jar to the tmp file
			int readResult;
			try {
				while ((readResult = in.read()) != -1)
					out.write(readResult);
			} catch (IOException ioe) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failExtractJarEntryMsg, jarEntry.getName()), ioe));
				return null;
			} finally {
				try {
					// ensure the input stream is closed
					if (in != null) {
						in.close();
						in = null;
					}
				} catch (IOException ioe) {
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failCloseJarEntryAfterExtractMsg, jarEntry.getName()), ioe));
				}
			}
		} finally {
			try {
				// ensure the output stream is closed
				if (out != null) {
					out.close();
					out = null;
				}
			} catch (IOException ioe) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.PluginVersionCompare_failCloseTmpAfterCreateMsg, tmpFile.toOSString()), ioe));
			}
		}
		return tmpFile == null ? null : tmpFile.toFile();
	}

	/**
	 * generates file name with current time stamp in the front
	 * e.g.: file.jar --> 1152797697531.jar
	 * @param fileName
	 * @return generated file name
	 */
	private String generateTmpFileName(String fileName) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(System.currentTimeMillis());
		int index = fileName.lastIndexOf(DOT_MARK);
		if (index == -1)
			return buffer.toString();
		buffer.append(fileName.substring(index));
		return buffer.toString();
	}

	/**
	 * gets URL instances indicates java classes which are under in a plugin directory denoted by <code>dir</code>.
	 * and set them into <code>map</code>
	 * key is simple name of class file e.g. Foo.class
	 * value is a List which contains URLs pointing to classes which have the same simple file name 
	 * 
	 * @param map Map used to store URL instances
	 * @param dir File instance which denotes a directory on file-system
	 * @param elements ManifestElement array of class path attribute
	 * @throws CoreException <p>if nested CoreException has been caught</p>
	 */
	private void getClassURLsFromDir(Map map, File dir, ManifestElement[] elements) throws CoreException {
		if (elements == null)
			// if elements is null, we consider the class path as "."
			getAllClassURLsUnderDir(map, dir);
		else {
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getValue().equals(DOT_MARK))
					getAllClassURLsUnderDir(map, dir);
				else {
					IPath path = new Path(dir.getAbsolutePath());
					path = path.append(elements[i].getValue());
					// class path element is supposed to be "." or "*.jar"
					if (!path.getFileExtension().equals(JAR_FILE_EXTENSION))
						finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.PluginVersionCompare_inValidClassPathMsg, elements[i].getValue(), dir.getAbsolutePath()), null));
					File jarFile = path.toFile();
					if (jarFile.exists())
						getClassURLsFromJar(map, jarFile, null);
					else
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PLUGIN_DETAIL_STATUS, NLS.bind(Messages.PluginVersionCompare_classPathJarNotFoundMsg, elements[i].getValue(), dir.getAbsolutePath()), null));
				}
			}
		}
	}

	/**
	 * checks if the class denoted by <code>className</code> need to be processed.
	 * @param className class name or class file name
	 * @return <code>true</code> if className does not include "/internal/", "\internal\", and ".internal."
	 * 	       <code>false</code> otherwise
	 */
	private boolean shouldProcess(String className) {
		return className.indexOf("/internal/") == -1 && className.indexOf("\\internal\\") == -1 && className.indexOf("\\.internal\\.") == -1; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * checks if the class denoted by <code>classFileReader</code> need to be processed.
	 * @param classFileReader IClassFileReader instance
	 * @return <code>true</code> if access flag of the class is public or protected
	 * 	       <code>false</code> otherwise
	 */
	private boolean shouldProcess(IClassFileReader classFileReader) {
		return Flags.isPublic(classFileReader.getAccessFlags()) || Flags.isProtected(classFileReader.getAccessFlags());
	}

	/**
	 * gets URLs of all "*.class" file under directory <code>dir</code> and its sub-directories
	 * and put them into <code>map</code>
	 * @param map Map used to store IClassFileReader instance
	 * @param dir directory
	 */
	private void getAllClassURLsUnderDir(Map map, File dir) {
		// if file is a directory, get URLs of "*.class" files under the directory and its sub-directories
		File[] classFiles = dir.listFiles(new VersionClassDirFilter(CLASS_FILE_EXTENSION));
		if (classFiles == null) {
			return;
		}
		for (int i = 0; i < classFiles.length; i++) {
			File file = classFiles[i];
			if (classFiles[i].isDirectory())
				// get URLs of class files under sub directory
				getAllClassURLsUnderDir(map, classFiles[i]);
			else if (isGivenTypeFile(file, CLASS_FILE_EXTENSION)) {
				if (!shouldProcess(file.getAbsolutePath()))
					continue;
				// get URL of class file
				URL classFileURL = null;
				try {
					classFileURL = classFiles[i].toURL();
				} catch (MalformedURLException e) {
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.FeatureModelTable_urlConvertErrorMsg, classFiles[i].getAbsolutePath()), e));
					// do next
					continue;
				}
				if (classFileURL == null)
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS | IVersionCompare.ERROR_OCCURRED, NLS.bind(Messages.FeatureModelTable_urlConvertErrorMsg, classFiles[i].getAbsolutePath()), null));
				else {
					// try to get URL list
					List urls = (List) map.get(file.getName());
					if (urls == null) {
						urls = new ArrayList(0);
						urls.add(classFileURL);
					} else
						urls.add(classFileURL);
					// put file name and list into map
					map.put(file.getName(), urls);
				}
			}
		}
	}

	/**
	 * converts char array to String
	 * @param chars array of char
	 * @return String which represents the content of <code>chars</code>
	 */
	private String charsToString(char[] chars) {
		return new String(chars);
	}

	/**
	 * Checks whether or not the given file potentially represents a given type file on the file-system.
	 * 
	 * @param file File instance which denotes to file on the file-system
	 * @param type file type(e.g. "java","class", "jar")
	 * @return <code>true</code> <code>file</code> exists and is a given type file,
	 *         <code>false</code> otherwise
	 */
	private boolean isGivenTypeFile(File file, String type) {
		IPath path = new Path(file.getAbsolutePath());
		return file.isFile() && path.getFileExtension().equals(type);
	}

	/**
	 * Checks whether or not the given JarEntry potentially represents a valid java class file,
	 * we filter out class file like "Foo$0.class".
	 * 
	 * @param entry JarEntry instance 
	 * @param type JarEntry type(e.g. "class")
	 * @return <code>true</code> <code>entry</code> exists and is a valid java class JarEntry,
	 *         <code>false</code> otherwise
	 */
	private boolean isValidClassJarEntry(JarEntry entry) {
		IPath path = new Path(entry.toString());
		// get file extension
		String extension = path.getFileExtension();
		if (extension == null)
			return false;
		if (!extension.equals(CLASS_FILE_EXTENSION))
			return false;
		int extensionIndex = path.lastSegment().lastIndexOf(extension);
		if (extensionIndex == -1)
			return false;
		// get name of the file (without ".class")
		String fileName = path.lastSegment().substring(0, extensionIndex - 1);
		try {
			int dollarIndex = fileName.lastIndexOf(DOLLAR_MARK);
			// if no "$" in the file name, it is what we want
			if (dollarIndex == -1)
				return true;
			// if the sub-String after "$" is all number, it is not what we want
			Long.parseLong(fileName.substring(dollarIndex + 1));
			return false;
		} catch (NumberFormatException nfe) {
			// if the sub-String after "$" is not all number, it is what we want
			return true;
		}
	}

	/**
	 * get java tmp directory IPath from system properties
	 * @return IPath instance which indicates java tmp directory
	 */
	private IPath getJavaTmpPath() {
		Properties properties = System.getProperties();
		return new Path(properties.getProperty(JAVA_TMP_DIR_PROPERTY));
	}

	/**
	 * Return a new status object populated with the given information.
	 * 
	 * @param severity severity of status
	 * @param code indicates type of this IStatus instance, it could be one of: FEATURE_OVERALL_STATUS, 
	 * 		 FEATURE_DETAIL_STATUS, PLUGIN_OVERALL_STATUS, PLUGIN_DETAIL_STATUS, PROCESS_ERROR_STATUS,
	 * 		 CLASS_OVERALL_STATUS, CLASS_DETAIL_STATUS
	 * @param message the status message
	 * @param exception exception which has been caught, or <code>null</code>
	 * @return the new status object
	 */
	private IStatus resultStatusHandler(int severity, int code, String message, Exception exception) {
		processed(code);
		if (message == null) {
			if (exception != null)
				message = exception.getMessage();
			// extra check because the exception message can be null
			if (message == null)
				message = EMPTY_STRING;
		}
		return new Status(severity, PLUGIN_ID, code, message, exception);
	}

	/**
	 * checks what kind of change does <code>result</code> represent
	 * @param result compare result
	 */
	private void processed(int result) {
		if ((result & IVersionCompare.ERROR_OCCURRED) != 0){
			hasError = true;
			return;
		}
		if ((result & IVersionCompare.MAJOR_CHANGE) != 0){
			hasMajorChange = true;
			return;
		}
		if ((result & IVersionCompare.MINOR_CHANGE) != 0){
			hasMinorChange = true;
			return;
		}
		if ((result & IVersionCompare.MICRO_CHANGE) != 0)
			hasMicroChange = true;
	}
}
