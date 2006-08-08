/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;

public class BuildTests extends TestCase {
	
	private String logFileName;
	private static final int HTML = 0;
	private static final int PROPERTIES = 1;
	private static final int XML = 2;
	URL[] javadocLogs=null;
	
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

	public void testChkpii() {
		
		try {
				//test that chkpii is on path by printing chkpii help information
				Runtime aRuntime = Runtime.getRuntime();
				Process aProcess = aRuntime.exec(getExec()+" /?");
				BufferedReader aBufferedReader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
					while (aBufferedReader.readLine() != null) {}
				aProcess.waitFor();
			} catch (IOException e) {
				//skip chkpii test if chkpii cannot be run.
				System.out.println(e.getMessage());
				System.out.println("Skipping chkpii test.");
				assertTrue(true);
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			String zipFile = locateEclipseZip();

//			String sniffFolder = BootLoader.getInstallURL().getPath() + "releng_sniff_folder";
//			String sniffFolder = "\\builds\\t";
			String sniffFolder = Platform.getLocation().toOSString();

			try {
				if (zipFile.equals("")){
					FileTool.unzip(getTrueFilter(), new File(sniffFolder));
				} else{
					FileTool.unzip(getTrueFilter(), new ZipFile(zipFile), new File(sniffFolder));
				}
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
				while ( aBufferedReader.readLine() != null) {
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
			return getExec() + " " + getFilesToTest(type) + " -E -O " + getOutputFile(type) + " -XM @" + getExcludeErrors() + " -X " + getExcludeFile () + " -S /jsq /tex";
		}
		/**
		 * Method locateEclipseZip.
		 * @return String
		 */
		private String locateEclipseZip() {

			// String to use when running as an automated test.			
			String installDir = Platform.getInstallLocation().getURL().getPath()+ ".." + File.separator + "..";
			
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
			String aString = System.getProperty("PLUGIN_PATH");
			return aString+File.separator+"ignoreFiles.txt";
		}
		
		/**
		 * Method getOutputFile.
		 * @param HTML
		 * @return String
		 */
		
		private String getOutputFile(int type) {
		

			new File(logFileName).mkdirs();
			
			String aString = logFileName + File.separator + "org.eclipse.nls.";
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

			return new File("chkpw808.exe").getPath();
		}
		
		/**
		 * Method getExcludeErrors.
		 */
		private String getExcludeErrors() {
			
			String os = Platform.getOS();
			String fileName;
			
			if (os.equals("win32")) {
				fileName = "ignoreErrorsWindows.txt";
			} else {
				fileName = "ignoreErrorsUnix.txt";
			}
			
			String aString = System.getProperty("PLUGIN_PATH");
			return aString+ File.separator + fileName;
		}
		
	/**
	 * Method parseLine.
	 * @param aLine
	 * @return -1 if not an error or warning line or the number of errors or
	 * warnings.
	 */
	private int parseLine(String aLine) {
		int index = aLine.indexOf("Files Could Not Be Processed: ");
		
		if (index ==-1){
			index=aLine.indexOf("Files Contain Error");
		}
		
		if (index==-1){
			return -1;
		} else {		
			String aString = aLine.substring(0, index).trim();
			return Integer.parseInt(aString);
		}
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

	public static final String[] REQUIRED_FEATURE_FILES = {"epl-v10.html", "feature.properties", "feature.xml", "license.html"};
	public static final String REQUIRED_FEATURE_SUFFIX = ".jpg";

	public static final String[] REQUIRED_PLUGIN_FILES = {"about.html", "plugin.properties", "plugin.xml"};
	public static final String REQUIRED_PLUGIN_SUFFIX = ".jar";
		
	public static final String[] REQUIRED_FEATURE_PLUGIN_FILES = {"about.html", "about.ini", "about.mappings", "about.properties", "plugin.properties", "plugin.xml"};
	public static final String REQUIRED_FEATURE_PLUGIN_SUFFIX = ".gif";

	public static final String[] REQUIRED_FRAGMENT_FILES = {"fragment.xml"};
	public static final String REQUIRED_FRAGMENT_SUFFIX = "";
	
	public static final String[] REQUIRED_SWT_FRAGMENT_FILES = {"fragment.properties"};
	public static final String REQUIRED_SWT_FRAGMENT_SUFFIX = "";
	
	public static final String[] REQUIRED_SOURCE_FILES = {"about.html"};
	public static final String REQUIRED_SOURCE_SUFFIX = ".zip";
	
	public static final String[] REQUIRED_BUNDLE_FILES = {"about.html"};
	public static final String REQUIRED_BUNDLE_MANIFEST = "MANIFEST.MF";
	public static final String REQUIRED_BUNDLE_SUFFIX = ".jar";
	
	public static final String[] SUFFIX_EXEMPT_LIST = {"org.eclipse.swt","org.apache.ant"};
	
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
		
		// Autoamted Test
		logFileName = Platform.getInstallLocation().getURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "chkpii";  // A tad bogus but this is where the build wants to copy the results from!

		String javadocUrls=System.getProperty("RELENGTEST.JAVADOC.URLS");
		if (javadocUrls!=null){
			String [] urls=javadocUrls.split(",");
			javadocLogs=  new URL[urls.length];
			for (int i=0;i<urls.length;i++){
				javadocLogs[i]=new URL(urls[i]);
			}
		}
		
		// Runtime Workbench - TODI Put me back to Automated status
//		logFileName = "d:\\results";
//		sourceDirectoryName = "d:\\sourceFetch";
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFeatureFiles() {
		List result = new ArrayList();
		String installDir = Platform.getInstallLocation().getURL().getPath();
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
	
	public void testPluginFiles() {
		List result = new ArrayList();
		String installDir = Platform.getInstallLocation().getURL().getPath();
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
		if ((testDirectory(aPlugin, REQUIRED_FRAGMENT_FILES, REQUIRED_FRAGMENT_SUFFIX))||(testBundleDirectory(aPlugin, REQUIRED_BUNDLE_FILES, REQUIRED_BUNDLE_MANIFEST, REQUIRED_FRAGMENT_SUFFIX))) {
			return true;
		}				
		
		// Are we an swt fragment
		if (testDirectory(aPlugin, REQUIRED_SWT_FRAGMENT_FILES, REQUIRED_SWT_FRAGMENT_SUFFIX)) {
			return true;
		}		
		
		// Are we a bundle?
		if (testBundleDirectory(aPlugin, REQUIRED_BUNDLE_FILES, REQUIRED_BUNDLE_MANIFEST, REQUIRED_BUNDLE_SUFFIX)) {
			return true;
		}
		
		// No then we are bad
		return false;
	}
	
	private boolean testPluginJar(File aDirectory, String[] requiredFiles){
			ArrayList list = new ArrayList();
			try {
				ZipFile jarredPlugin=new ZipFile(aDirectory);
				Enumeration _enum=jarredPlugin.entries();
				while (_enum.hasMoreElements()){
					list.add(_enum.nextElement().toString());
				}				
			} catch (ZipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!list.containsAll(Arrays.asList(requiredFiles))) {
				return false;
			}
			return true;
	}
	private boolean testDirectory(File aDirectory, String[] requiredFiles, String requiredSuffix) {
		if (aDirectory.getName().endsWith(".jar")){
			return testPluginJar(aDirectory,requiredFiles);
		} else {
			if (!Arrays.asList(aDirectory.list()).containsAll(
					Arrays.asList(requiredFiles))) {
				return false;
			}

			int index = aDirectory.getName().indexOf('_');
			if (index == -1) {
				index = aDirectory.getName().length();
			}

			String plainName = aDirectory.getName().substring(0, index);

			if (requiredSuffix.equals("")
					|| Arrays.asList(SUFFIX_EXEMPT_LIST).contains(plainName)) {
				return true;
			} else if (aDirectory
					.listFiles(new FileSuffixFilter(requiredSuffix)).length == 0) {
				return false;
			}
		}
		return true;
	}
	

	private boolean testBundleDirectory(File aDirectory, String[] requiredFiles, String manifestFile, String requiredSuffix) {
		if (aDirectory.getName().endsWith(".jar")) {
			return testPluginJar(aDirectory, requiredFiles);
		} else {
			if (!Arrays.asList(aDirectory.list()).containsAll(
					Arrays.asList(requiredFiles))) {
				return false;
			}

			int index = aDirectory.getName().indexOf('_');
			if (index == -1) {
				index = aDirectory.getName().length();
			}

			String plainName = aDirectory.getName().substring(0, index);

			File metaDir = new File(aDirectory, "META-INF");

			String[] metaFiles = metaDir.list();
			if (metaFiles == null) {
				return (false);
			} else {
				for (int i = 0; i < metaFiles.length; i++) {
					String filename = metaFiles[i];
					if (filename == manifestFile) {
						return true;
					}
				}
			}

			if (!metaDir.exists()) {
				return false;
			}

			if (requiredSuffix.equals("")
					|| Arrays.asList(SUFFIX_EXEMPT_LIST).contains(plainName)) {
				return true;
			} else if (aDirectory
					.listFiles(new FileSuffixFilter(requiredSuffix)).length == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return true if the receiver is a source plugin, false otherwise A
	 * separate method because this is a little tricky.
	 * 
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

	public void testJavadocLogs() {
		//skip this test if there are no logs to check
		if (javadocLogs==null)
			assertTrue("", true);
		else{
			JavadocLog javadocLog = new JavadocLog(javadocLogs);
			String message="javadoc errors and/or warnings in: \n";
			boolean problemLogsExist=javadocLog.logs.size()>0;
			if (problemLogsExist){
				for (int i=0;i<javadocLog.logs.size();i++)
					message=message.concat(javadocLog.logs.get(i).toString()+"\n");
			}
			message=message.concat("See the javadoc logs linked from the test results page for details");
			assertTrue(message, !problemLogsExist);
		}
	}
		
	private class JavadocLog {
		private ArrayList logs=new ArrayList();
		private JavadocLog(URL[] logs) {
			findProblems(logs);
}

		private void findProblems(URL[] javadocLogs) {
			String JAVADOC_WARNING = ": warning";
			String JAVADOC_ERROR = ": error";
			String JAVADOC_JAVA = ".java:";

			BufferedReader in = null;
			for (int i = 0; i < javadocLogs.length; i++) {
				try {
					in = new BufferedReader(new InputStreamReader(javadocLogs[i].openStream()));
					String tmp;
					while ((tmp = in.readLine()) != null) {
						tmp = tmp.toLowerCase();
						if (tmp.indexOf(JAVADOC_ERROR) != -1 || tmp.indexOf(JAVADOC_WARNING) != -1|| tmp.indexOf(JAVADOC_JAVA) != -1) {
							String fileName=new File(javadocLogs[i].getFile()).getName();
							if (!logs.contains(fileName))
								logs.add(fileName);
						}
					}
					in.close();

				} catch (FileNotFoundException e) {
					logs.add("Unable to find "+new File(javadocLogs[i].getFile()).getName()+" to read.");
					e.printStackTrace();
				} catch (IOException e) {
					logs.add("Unable to read "+new File(javadocLogs[i].getFile()).getName());
					e.printStackTrace();
				}
			}
		}
	}
}


