/******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.releng.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.Platform;

import junit.framework.TestCase;

public class BuildTests extends TestCase {
	
	private static final String CVS_KO = "-ko";
	private static final String CVS_KKV = "-kkv";
	private static final String CVS_KB = "-kb";
	private static final String CVS_BINARY = "CVS_BINARY";
	private static final int ENTRY_TYPE_INDEX = 3;
	private static final int ENTRY_NAME_INDEX = 0;
	private static final int ENTRY_FIELDS_SIZE = 5;
	private static final String DEFAULT_CVS_TYPE = "-kkv";
	
	private Map cvsTypes;
	private static final int HTML = 0;
	private static final int PROPERTIES = 1;
	private static final int XML = 2;
	
	private static FileTool.IZipFilter getTrueFilter() {
		return new FileTool.IZipFilter() {
			public boolean shouldExtract(String fullEntryName, String entryName, int depth) {return true;}
			public boolean shouldUnzip(String fullEntryName, String entryName, int depth) {
				return true;
			}
		};
	}
	/**
	 * Method hasErrors.
	 * @param string
	 * @return boolean
	 */
	private boolean hasErrors(String string) {
		
		boolean result = false;
		BufferedReader aReader = null;
		
		try {
			aReader = new BufferedReader(new InputStreamReader(new FileInputStream(string)));
			String aLine = aReader.readLine();
			while (aLine != null) {
				int aNumber = parseLine(aLine);
				if (aNumber > 0) {
					result = true;
					break;
				}
				aLine = aReader.readLine();
			}
		} catch (FileNotFoundException e) {
			System.out.println("Could not open log file: " + string);
			result = true;
		} catch (IOException e) {
			System.out.println("Error reading log file: " + string);
			result = true;
		} finally {
			if (aReader != null) {
				try {
					aReader.close();
				} catch (IOException e) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	public void testCVSKTag() {
		boolean result = false;
		
		initializeCVSTypes();
		
		// String to use when running as an automated test.
		String logFileName = BootLoader.getInstallURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "cvsTags";

		// String t use when running in an Eclipse runtime
		// String logFileName = BootLoader.getInstallURL().getPath() + "plugins" + File.separator + "org.eclipse.releng.tests_2.1.0" + File.separator + "results" + File.separator + "cvsTags";

		new File(logFileName).mkdirs();
		logFileName = logFileName + File.separator + "cvsTypesLog.txt";

		try {

	//		StringWriter aWriter = new StringWriter();
			BufferedWriter aLog = new BufferedWriter(new FileWriter(logFileName));
			String rootDirectoryName = "d:\\sourceFetch";
			File rootDirectory = new File(rootDirectoryName);
			result = scanCVSKTag(rootDirectory, aLog);
	//		System.out.println(aWriter.getBuffer().toString());
			aLog.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		assertFalse("CVS KTag type errors.  See the releng test logs linked from the test results page for details.", result);
	}
	
	private void initializeCVSTypes() {
		cvsTypes = new HashMap();
		cvsTypes.put("gif", CVS_BINARY);
		cvsTypes.put("jpg", CVS_BINARY);
		cvsTypes.put("zip", CVS_BINARY);
		cvsTypes.put("jar", CVS_BINARY);
		cvsTypes.put("bmp", CVS_BINARY);
		cvsTypes.put("class", CVS_BINARY);
		cvsTypes.put("dll", CVS_BINARY);
		cvsTypes.put("doc", CVS_BINARY);
		cvsTypes.put("exe", CVS_BINARY);
		cvsTypes.put("ico", CVS_BINARY);
		cvsTypes.put("jpeg", CVS_BINARY);
		cvsTypes.put("pdf", CVS_BINARY);
		cvsTypes.put("png", CVS_BINARY);
		cvsTypes.put("ppt", CVS_BINARY);
		cvsTypes.put("so", CVS_BINARY);
		cvsTypes.put("tiff", CVS_BINARY);
		cvsTypes.put("tif", CVS_BINARY);
		cvsTypes.put("xls", CVS_BINARY);
	}
	
	private boolean scanCVSKTag(File aDirectory, BufferedWriter aLog) {
		
		boolean result = false;
		
		File[] files = aDirectory.listFiles();
		for (int i = 0; i < files.length; i++) {
			File aFile = files[i];
			if (aFile.isDirectory()) {
				if (aFile.getName().equals("CVS")) {
					result = result | scanCVSDirectory(aFile, aLog);
				} else {
					result = result | scanCVSKTag(aFile, aLog);
				} 
			}
		}
		return result;
	}
	
	private boolean scanCVSDirectory(File aDirectory, BufferedWriter aLog) {
		
		boolean result = false;
		
		File entries = new File(aDirectory, "Entries");
		try {
			BufferedReader aReader = new BufferedReader(new FileReader(entries));
			String aLine = aReader.readLine();
			while (aLine != null) {
				result = result | validateEntry(aDirectory.getParentFile(), aLine, aLog);
				aLine = aReader.readLine();
			}
			
			aReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found reading Entries file");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException reading Entries file");
			e.printStackTrace();
		}
		
		return result;
		
	}
	
	private boolean validateEntry(File aDirectory, String aLine, BufferedWriter aLog) {

		boolean result = false;
		
		String[] fields = split(aLine, "/");
		if (fields.length < ENTRY_FIELDS_SIZE) {
			return result;
		}
		
		String entryName = aDirectory + File.separator + fields[ENTRY_NAME_INDEX];
		String expectedType  = (String) cvsTypes.get(entryName);

		// No type registered for exact file name.  Check for extension
		if (expectedType == null) {			
			
			String[] dotParts = split(fields[ENTRY_NAME_INDEX], ".");
			String entryExtension;
			if (dotParts.length == 0) {
				// File name has no extension.
				expectedType = DEFAULT_CVS_TYPE;
			} else {
				entryExtension = dotParts[dotParts.length - 1];
				expectedType = (String) cvsTypes.get(entryExtension);
				if (expectedType == null) {
					// Extension has no type registered
					expectedType = DEFAULT_CVS_TYPE;
				}
			}
		}
		
		// We know what type to expect for this file.  Are we the right one?
				
		String entryType = fields[ENTRY_TYPE_INDEX];
		
		try {
			if (expectedType.equals(CVS_BINARY)) {
				if (!entryType.equals(CVS_KB)) {
					// Fail Binary Test
					aLog.write("File: " + entryName + " was expected to be be binary");
//					System.out.println("File: " + entryName + " was expected to be be binary");
					aLog.newLine();
					result = true;
				}
			} else {
				if (!(entryType.equals(CVS_KKV) || entryType.equals(CVS_KO))) {
					// Fail
					aLog.write("File: " + entryName + " was expected to be be text");
//					System.out.println("File: " + entryName + " was expected to be be text");
					aLog.newLine();
					result = true;
				}
			}
		} catch (IOException e) {	
			System.out.println("IOException writing log");
			e.printStackTrace();	
		}
		return result;
	}
	
	/**
	 * @param aLine
	 * @param delimeter
	 * @return String[]
	 */
	private String[] split(String aLine, String delimeter) {
		StringTokenizer tokenizer = new StringTokenizer(aLine, delimeter);
		List list = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			list.add(tokenizer.nextToken());
		}
		return (String[]) list.toArray(new String[0]);
	}
	
	public void testChkpii() {
			
			String zipFile = locateEclipseZip();
//			String sniffFolder = BootLoader.getInstallURL().getPath() + "releng_sniff_folder";
//			String sniffFolder = "\\builds\\t";
			String sniffFolder = Platform.getLocation().toOSString();
		
			try {
				FileTool.unzip(getTrueFilter(), new ZipFile(zipFile), new File(sniffFolder));
			} catch (IOException e) {
				
				
				fail(zipFile + ": " + sniffFolder  + ": " + "IOException unzipping Eclipse for chkpii");
			}
		
			boolean result1  = testChkpii(HTML);
			boolean result2 = testChkpii(XML);
			boolean result3 = testChkpii(PROPERTIES);
			assertTrue("Translation errors in files.  See the chkpii logs linked from the test results page for details.", (result1 && result2 && result3));
		}
		
		private boolean testChkpii(int type) {
			Runtime aRuntime = Runtime.getRuntime();
			String chkpiiString = getChkpiiString(type);
			try {
				Process aProcess = aRuntime.exec(chkpiiString);
				BufferedReader aBufferedReader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
				String line = null;
				while ( (line = aBufferedReader.readLine()) != null) {
				}
				aProcess.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (InterruptedException e) {
				return false;
			}
			return !hasErrors(getOutputFile(type));
		}
		
		/**
		 * Method getChkpiiString.
		 * @param HTML
		 * @return String
		 */
		private String getChkpiiString(int type) {
			return getExec() + " " + getFilesToTest(type) + " -E -O " + getOutputFile(type) + " -XM @" + getExcludeErrors() + " -X " + getExcludeFile () + " -S";
		}
		/**
		 * Method locateEclipseZip.
		 * @return String
		 */
		private String locateEclipseZip() {

			// String to use when running as an automated test.			
			String installDir = BootLoader.getInstallURL().getPath() + ".." + File.separator + "..";
			
			// String to use when running in Eclipse
			// String installDir = BootLoader.getInstallURL().getPath() + "..";
			File aFile = new File(installDir);
			if (aFile == null) {
				System.out.println("File is null");
			}
			System.out.println(installDir);
			
			File[] files = aFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String fileName = file.getName();
				if (fileName.startsWith("eclipse-SDK-") && fileName.endsWith(".zip")) {
					return file.getPath();
				}
			}
			
			return "";
		}
		
		/**
		 * Method getExcludeFiles.
		 * @return String
		 */
		private String getExcludeFile() {
			String aString = BootLoader.getInstallURL().getPath() + "plugins" + File.separator + "org.eclipse.releng.tests_2.1.0" + File.separator + "ignoreFiles.txt";
			return new File(aString).getPath();
		}
		
		/**
		 * Method getOutputFile.
		 * @param HTML
		 * @return String
		 */
		
		private String getOutputFile(int type) {
		
			// String to use when running as an automated test.
			String aString = BootLoader.getInstallURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "chkpii";

			// String t use when running in an Eclipse runtime
			// String aString = BootLoader.getInstallURL().getPath() + "plugins" + File.separator + "org.eclipse.releng.tests_2.1.0" + File.separator + "results" + File.separator + "chkpii";

			new File(aString).mkdirs();
			aString = aString + File.separator + "org.eclipse.nls.";
			aString = new File(aString).getPath();

			switch (type) {
				case HTML :
					return aString + "html.txt";
				case PROPERTIES :
					return aString + "properties.txt";
				
				case XML : 
					return aString + "xml.txt";

				default :
					return aString + "other.txt";
			}
		}
		
		/**
		 * Method getFilesToTest.
		 * @param HTML
		 * @return String
		 */
		
		private String getFilesToTest(int type) {
			
//			String sniffFolder = BootLoader.getInstallURL().getPath() + "releng_sniff_folder" + File.separator;
//			String sniffFolder = File.separator + "builds" + File.separator + "t" + File.separator;
			String sniffFolder = Platform.getLocation().toOSString();

			String aString = new File(sniffFolder).getPath() + File.separator;
			
			switch (type) {
				case HTML :
					return aString + "*.htm*";
				case PROPERTIES :
					return aString + "*.properties";
							
				case XML : 
					return aString + "*.xml";
			
				default :
					return aString + "*.*";
			}
		}
		
		/**
		 * Method getExec.
		 * @return String
		 */
		
		private String getExec() {

			return new File("chkpw501.exe").getPath();
		}
		
		/**
		 * Method getExcludeErrors.
		 */
		private String getExcludeErrors() {
			
			String os = BootLoader.getOS();
			String fileName;
			
			if (os.equals(BootLoader.OS_WIN32)) {
				fileName = "ignoreErrorsWindows.txt";
			} else {
				fileName = "ignoreErrorsUnix.txt";
			}
			
			String aString = BootLoader.getInstallURL().getPath() + "plugins" + File.separator + "org.eclipse.releng.tests_2.1.0" + File.separator + fileName;
			return new File(aString).getPath();
		}
		
	/**
	 * Method parseLine.
	 * @param aLine
	 * @return -1 if not an error or warning line or the number of errors or
	 * warnings.
	 */
	private int parseLine(String aLine) {
		int index = aLine.indexOf("Files Contain Error");
		
		if (index == -1) {
			index = aLine.indexOf("Files Contain Warning");
		}
		
		if (index == -1) {
			index = aLine.indexOf("Files Could Not Be Processed");
		}
		
		if (index == -1) {
			return index;
		}
		
		String aString = aLine.substring(0, index).trim();
		return Integer.parseInt(aString);
	}
	
	public class FileSuffixFilter implements FilenameFilter {

		private String suffix;
		
		public FileSuffixFilter(String suffix) {
			this.suffix = suffix;
		}
		
		/**
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		public boolean accept(File dir, String name) {
			int lastDot = name.lastIndexOf('.');
			if (lastDot == -1) {
				return false;
			}
			return name.substring(lastDot).equals(suffix);
		}

	}

	public static final String[] REQUIRED_FEATURE_FILES = {"cpl-v10.html", "feature.properties", "feature.xml", "license.html"};
	public static final String REQUIRED_FEATURE_SUFFIX = ".jpg";

	public static final String[] REQUIRED_PLUGIN_FILES = {"about.html", "plugin.properties", "plugin.xml"};
	public static final String REQUIRED_PLUGIN_SUFFIX = ".jar";

	public static final String[] REQUIRED_FEATURE_PLUGIN_FILES = {"about.html", "about.ini", "about.mappings", "about.properties", "plugin.properties", "plugin.xml"};
	public static final String REQUIRED_FEATURE_PLUGIN_SUFFIX = ".gif";

	public static final String[] REQUIRED_FRAGMENT_FILES = {"fragment.xml"};
	public static final String REQUIRED_FRAGMENT_SUFFIX = "";
	
	public static final String[] REQUIRED_SOURCE_FILES = {"about.html"};
	public static final String REQUIRED_SOURCE_SUFFIX = ".zip";
	
	public static final String[] SUFFIX_EXEMPT_LIST = {"org.eclipse.swt"};
	
	public static final int PLUGIN_COUNT = 84; // - 20;	// Note this number must include non-shipping test plugins
	public static final int FEATURE_COUNT = 9; // - 1;	// Note this number must include non-shipping test feature

	/**
	 * Constructor for EmptyDirectoriesTest.
	 * @param arg0
	 */
	public BuildTests(String arg0) {
		super(arg0);
	}

	
	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFeatureFiles() {
		List result = new ArrayList();
		String installDir = BootLoader.getInstallURL().getPath();
		File featureDir = new File(installDir, "features");
		File[] features = featureDir.listFiles();
		for (int i = 0; i < features.length; i++) {
			File aFeature = features[i];
			if (!testDirectory(aFeature, REQUIRED_FEATURE_FILES, REQUIRED_FEATURE_SUFFIX)) {
				result.add(aFeature.getPath());
			}

		}
		
		String aString = "";
		if (result.size() > 0) {
			
			Iterator iter = result.iterator();
			while (iter.hasNext()) {
				String element = (String) iter.next();
				aString = aString + element + "; ";
			}
		}
		assertTrue("Feature directory missing required files: " + aString, result.size() == 0);
	}
	
//	public void testPluginCount() {
//		String installDir = BootLoader.getInstallURL().getPath();
//		File pluginDir = new File(installDir, "plugins");
//		File[] plugins = pluginDir.listFiles();
//
//		assertTrue("Plug-ins missing: " + (PLUGIN_COUNT - plugins.length), PLUGIN_COUNT == plugins.length);
//	}
	
	public void testFeatureCount() {
		String installDir = BootLoader.getInstallURL().getPath();
		File featureDir = new File(installDir, "features");
		File[] features = featureDir.listFiles();

		assertTrue("Features missing: " + (FEATURE_COUNT - features.length), FEATURE_COUNT == features.length);
	}
	
	
	public void testPluginFiles() {
		List result = new ArrayList();
		String installDir = BootLoader.getInstallURL().getPath();
		File pluginDir = new File(installDir, "plugins");
		File[] plugins = pluginDir.listFiles();
		for (int i = 0; i < plugins.length; i++) {
			File aPlugin = plugins[i];
			if (aPlugin.getName().indexOf("test") == -1) {
				if (!testPluginFile(aPlugin)) {
					result.add(aPlugin.getPath());
				}
			}
		}

		String aString = "";
		if (result.size() > 0) {

			Iterator iter = result.iterator();
			while (iter.hasNext()) {
				String element = (String) iter.next();
				aString = aString + element + "; ";
			}
		}
		assertTrue("Plugin directory missing required files: " + aString, result.size() == 0);
	}

	private boolean testPluginFile(File aPlugin) {
	
		// Are we a doc plugin?
		if (testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, ".zip")) {
			return true;
		}
		
		// Are we a feature plugin?
		if (testDirectory(aPlugin, REQUIRED_FEATURE_PLUGIN_FILES, REQUIRED_FEATURE_PLUGIN_SUFFIX)) {
			return true;
		}
	
		// Are we a regular plugin
		if (testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, REQUIRED_PLUGIN_SUFFIX)) {
			return true;
		}
		
		// Are we a source plugin
		if (testSourcePlugin(aPlugin)) {
			return true;
		}
		
		// Are we a fragment
		if (testDirectory(aPlugin, REQUIRED_FRAGMENT_FILES, REQUIRED_FRAGMENT_SUFFIX)) {
			return true;
		}
		
		// No then we are bad
		return false;
	}
	
	private boolean testDirectory(File aDirectory, String[] requiredFiles, String requiredSuffix) {
		if (!Arrays.asList(aDirectory.list()).containsAll(Arrays.asList(requiredFiles))) {
			return false;
		}
		
		int index = aDirectory.getName().indexOf('_');
		if (index == -1) {
			index = aDirectory.getName().length();
		}
		
		String plainName = aDirectory.getName().substring(0, index);
		
		if (requiredSuffix.equals("") || Arrays.asList(SUFFIX_EXEMPT_LIST).contains(plainName)) {
			return true;
		} else if (aDirectory.listFiles(new FileSuffixFilter(requiredSuffix)).length == 0) {
			return false;
		}
		
		return true;
	}

	/**
	 * Return true if the receiver is a source plugin, false otherwise
	 * A separate method because this is a little tricky.
	 * @param aPlugin
	 * @return boolean
	 */
	private boolean testSourcePlugin(File aPlugin) {
		if (!testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, "")) {
			return false;
		}
		
		File sourceDir = new File(aPlugin, "src");
		File[] sourceDirs = sourceDir.listFiles();
		if (sourceDirs == null) {
			return false;
		}
		
		for (int i = 0; i < sourceDirs.length; i++) {
			File aSourceDir = sourceDirs[i];
			if (!testDirectory(aSourceDir, REQUIRED_SOURCE_FILES, REQUIRED_SOURCE_SUFFIX)) {
				return false;
			}
		}
		return true;
	}	
	
}
