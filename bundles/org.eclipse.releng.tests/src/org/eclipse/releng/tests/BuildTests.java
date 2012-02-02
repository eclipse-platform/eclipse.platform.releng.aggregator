/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import java.io.BufferedInputStream;
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
import java.util.Properties;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;


public class BuildTests extends TestCase {

	private String logFileName;
	private static final int HTML = 0;
	private static final int PROPERTIES = 1;
	private static final int XML = 2;

	private static FileTool.IZipFilter getTrueFilter() {
		return new FileTool.IZipFilter() {
			public boolean shouldExtract(String fullEntryName,
					String entryName, int depth) {
				return true;
			}

			public boolean shouldUnzip(String fullEntryName, String entryName,
					int depth) {
				return true;
			}
		};
	}

	/**
	 * Method hasErrors.
	 * 
	 * @param string
	 * @return boolean
	 */
	private boolean hasErrors(String string) {

		boolean result = false;
		BufferedReader aReader = null;

		try {
			aReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(string)));
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
			// test that chkpii is on path by printing chkpii help information
			Runtime aRuntime = Runtime.getRuntime();
			Process aProcess = aRuntime.exec(getExec() + " /?");
			BufferedReader aBufferedReader = new BufferedReader(
					new InputStreamReader(aProcess.getInputStream()));
			while (aBufferedReader.readLine() != null) {
			}
			aProcess.waitFor();
		} catch (IOException e) {
			// skip chkpii test if chkpii cannot be run.
			System.out.println(e.getMessage());
			System.out.println("Skipping chkpii test.");
			assertTrue(true);
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String zipFile = locateEclipseZip();

		// String sniffFolder = BootLoader.getInstallURL().getPath() +
		// "releng_sniff_folder";
		// String sniffFolder = "d:\\builds\\t";
		String sniffFolder = Platform.getLocation().toOSString();

		try {
			if (zipFile.equals("")) {
				FileTool.unzip(getTrueFilter(), new File(sniffFolder));
			} else {
				FileTool.unzip(getTrueFilter(), new ZipFile(zipFile), new File(
						sniffFolder));
			}
		} catch (IOException e) {
			fail(zipFile + ": " + sniffFolder + ": "
					+ "IOException unzipping Eclipse for chkpii");
		}

		boolean result1 = testChkpii(HTML);
		boolean result2 = testChkpii(XML);
		boolean result3 = testChkpii(PROPERTIES);
		assertTrue(
				"Translation errors in files.  See the chkpii logs linked from the test results page for details.",
				(result1 && result2 && result3));
	}

	private boolean testChkpii(int type) {
		Runtime aRuntime = Runtime.getRuntime();
		String chkpiiString = getChkpiiString(type);
		try {
			Process aProcess = aRuntime.exec(chkpiiString);
			BufferedReader aBufferedReader = new BufferedReader(
					new InputStreamReader(aProcess.getInputStream()));
			while (aBufferedReader.readLine() != null) {
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
	 * 
	 * @param HTML
	 * @return String
	 */
	private String getChkpiiString(int type) {
		return getExec() + " " + getFilesToTest(type) + " -E -O "
				+ getOutputFile(type) + " -XM @" + getExcludeErrors() + " -X "
				+ getExcludeFile() + " -S /jsq /tex";
	}

	/**
	 * Method locateEclipseZip.
	 * 
	 * @return String
	 */
	private String locateEclipseZip() {

		// String to use when running as an automated test.
		String installDir = Platform.getInstallLocation().getURL().getPath()
				+ ".." + File.separator + "..";

		// String to use when running in Eclipse
		// String installDir = BootLoader.getInstallURL().getPath() + "..";
		File aFile = new File(installDir);
		

		File[] files = aFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String fileName = file.getName();
			if (fileName.startsWith("eclipse-SDK-")
					&& fileName.endsWith(".zip")) {
				return file.getPath();
			}
		}

		return "";
	}

	/**
	 * Method getExcludeFiles.
	 * 
	 * @return String
	 */
	private String getExcludeFile() {
		String aString = System.getProperty("PLUGIN_PATH");
		return aString + File.separator + "ignoreFiles.txt";
	}

	/**
	 * Method getOutputFile.
	 * 
	 * @param HTML
	 * @return String
	 */

	private String getOutputFile(int type) {

		new File(logFileName).mkdirs();

		String aString = logFileName + File.separator + "org.eclipse.nls.";
		aString = new File(aString).getPath();

		switch (type) {
		case HTML:
			return aString + "html.txt";
		case PROPERTIES:
			return aString + "properties.txt";

		case XML:
			return aString + "xml.txt";

		default:
			return aString + "other.txt";
		}
	}

	/**
	 * Method getFilesToTest.
	 * 
	 * @param HTML
	 * @return String
	 */

	private String getFilesToTest(int type) {

		String sniffFolder = Platform.getLocation().toOSString();

		String aString = new File(sniffFolder).getPath() + File.separator;

		switch (type) {
		case HTML:
			return aString + "*.htm*";
		case PROPERTIES:
			return aString + "*.properties";

		case XML:
			return aString + "*.xml";

		default:
			return aString + "*.*";
		}
	}

	/**
	 * Method getExec.
	 * 
	 * @return String
	 */

	private String getExec() {

		return new File("chkpw1402.exe").getPath();
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
		return aString + File.separator + fileName;
	}

	/**
	 * Method parseLine.
	 * 
	 * @param aLine
	 * @return -1 if not an error or warning line or the number of errors or
	 *         warnings.
	 */
	private int parseLine(String aLine) {
		int index = aLine.indexOf("Files Could Not Be Processed: ");

		if (index == -1) {
			index = aLine.indexOf("Files Contain Error");
		}

		if (index == -1) {
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

	public static final String[] REQUIRED_FEATURE_FILES = { "epl-v10.html",
			"feature.properties", "feature.xml", "license.html" };
	public static final String REQUIRED_FEATURE_SUFFIX = "";

	public static final String[] REQUIRED_PLUGIN_FILES = { "about.html",
			"plugin.properties", "plugin.xml" };
	public static final String REQUIRED_PLUGIN_SUFFIX = ".jar";

	public static final String[] REQUIRED_FEATURE_PLUGIN_FILES = {
			"about.html", "about.ini", "about.mappings", "about.properties",
			"plugin.properties", "plugin.xml" };
	public static final String REQUIRED_FEATURE_PLUGIN_SUFFIX = ".gif";

	public static final String[] REQUIRED_FRAGMENT_FILES = { "fragment.xml" };
	public static final String REQUIRED_FRAGMENT_SUFFIX = "";

	public static final String[] REQUIRED_SWT_FRAGMENT_FILES = { "fragment.properties" };
	public static final String REQUIRED_SWT_FRAGMENT_SUFFIX = "";

	public static final String[] REQUIRED_SOURCE_FILES = { "about.html" };
	public static final String REQUIRED_SOURCE_SUFFIX = ".zip";

	public static final String[] REQUIRED_BUNDLE_FILES = { "about.html" };
	public static final String REQUIRED_BUNDLE_MANIFEST = "MANIFEST.MF";
	public static final String REQUIRED_BUNDLE_SUFFIX = ".jar";

	public static final String[] SUFFIX_EXEMPT_LIST = { "org.eclipse.swt",
			"org.apache.ant" };

	public static final int PLUGIN_COUNT = 84; // - 20; // Note this number
	// must include non-shipping
	// test plugins
	public static final int FEATURE_COUNT = 9; // - 1; // Note this number must

	// include non-shipping test
	// feature

	/**
	 * Constructor for EmptyDirectoriesTest.
	 * 
	 * @param arg0
	 */
	public BuildTests(String arg0) {
		super(arg0);
	}

	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		// Automated Test
		logFileName= Platform.getInstallLocation().getURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "chkpii"; // A tad bogus but this is where the build wants to copy the results from!

		// Runtime Workbench - TODO Put me back to Automated status
		// logFileName = "d:\\results";
		// sourceDirectoryName = "d:\\sourceFetch";
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
			if (!testDirectory(aFeature, REQUIRED_FEATURE_FILES,
					REQUIRED_FEATURE_SUFFIX)) {
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
		assertTrue("Feature directory missing required files: " + aString,
				result.size() == 0);
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
		assertTrue("Plugin directory missing required files: " + aString,
				result.size() == 0);
	}

	private boolean testPluginFile(File aPlugin) {

		// Are we a doc plugin?
		if (testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, ".zip")) {
			return true;
		}

		// Are we a feature plugin?
		if (testDirectory(aPlugin, REQUIRED_FEATURE_PLUGIN_FILES,
				REQUIRED_FEATURE_PLUGIN_SUFFIX)) {
			return true;
		}

		// Are we a regular plugin
		if (testDirectory(aPlugin, REQUIRED_PLUGIN_FILES,
				REQUIRED_PLUGIN_SUFFIX)) {
			return true;
		}

		// Are we a source plugin
		if (testSourcePlugin(aPlugin)) {
			return true;
		}

		// Are we a fragment
		if ((testDirectory(aPlugin, REQUIRED_FRAGMENT_FILES,
				REQUIRED_FRAGMENT_SUFFIX))
				|| (testBundleDirectory(aPlugin, REQUIRED_BUNDLE_FILES,
						REQUIRED_BUNDLE_MANIFEST, REQUIRED_FRAGMENT_SUFFIX))) {
			return true;
		}

		// Are we an swt fragment
		if (testDirectory(aPlugin, REQUIRED_SWT_FRAGMENT_FILES,
				REQUIRED_SWT_FRAGMENT_SUFFIX)) {
			return true;
		}

		// Are we a bundle?
		if (testBundleDirectory(aPlugin, REQUIRED_BUNDLE_FILES,
				REQUIRED_BUNDLE_MANIFEST, REQUIRED_BUNDLE_SUFFIX)) {
			return true;
		}

		// No then we are bad
		return false;
	}

	private boolean testPluginJar(File aDirectory, String[] requiredFiles) {
		ArrayList list = new ArrayList();
		try {
			ZipFile jarredPlugin = new ZipFile(aDirectory);
			Enumeration _enum = jarredPlugin.entries();
			while (_enum.hasMoreElements()) {
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

	private boolean testDirectory(File aDirectory, String[] requiredFiles,
			String requiredSuffix) {
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

	private boolean testBundleDirectory(File aDirectory,
			String[] requiredFiles, String manifestFile, String requiredSuffix) {
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
			if (!testDirectory(aSourceDir, REQUIRED_SOURCE_FILES,
					REQUIRED_SOURCE_SUFFIX)) {
				return false;
			}
		}
		return true;
	}

	public void testJavadocLogs() throws Exception {
		String javadocUrls= System.getProperty("RELENGTEST.JAVADOC.URLS");
		// Skip this test if there are no logs to check
		if (javadocUrls == null)
			return;

		String[] urls= javadocUrls.split(",");
		URL[] javadocLogs= new URL[urls.length];
		for (int i= 0; i < urls.length; i++) {
			javadocLogs[i]= new URL(urls[i]);
		}

		JavadocLog javadocLog= new JavadocLog(javadocLogs);
		String message= "javadoc errors and/or warnings in: \n";
		boolean problemLogsExist= javadocLog.logs.size() > 0;
		if (problemLogsExist) {
			for (int i= 0; i < javadocLog.logs.size(); i++)
				message= message.concat(javadocLog.logs.get(i).toString() + "\n");
		}
		message= message.concat("See the javadoc logs linked from the test results page for details");
			assertTrue(message, !problemLogsExist);
	}

	private class JavadocLog {
		private ArrayList logs = new ArrayList();

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
					in = new BufferedReader(new InputStreamReader(
							javadocLogs[i].openStream()));
					String tmp;
					while ((tmp = in.readLine()) != null) {
						tmp = tmp.toLowerCase();
						if (tmp.indexOf(JAVADOC_ERROR) != -1
								|| tmp.indexOf(JAVADOC_WARNING) != -1
								|| tmp.indexOf(JAVADOC_JAVA) != -1) {
							String fileName = new File(javadocLogs[i].getFile())
									.getName();
							if (!logs.contains(fileName))
								logs.add(fileName);
						}
					}
					in.close();

				} catch (FileNotFoundException e) {
					logs.add("Unable to find "
							+ new File(javadocLogs[i].getFile()).getName()
							+ " to read.");
					e.printStackTrace();
				} catch (IOException e) {
					logs.add("Unable to read "
							+ new File(javadocLogs[i].getFile()).getName());
					e.printStackTrace();
				}
			}
		}
	} 
 
	
	public void testComparatorLogs() throws Exception {
		String os = System.getProperty("os.name");
		// Only run compare tool on Linux to save time during tests 
		if (os == null || !os.equalsIgnoreCase("Linux")) {
			return;
		}

		// Load the configuration and ensure the mandatory parameters were specified
		Properties properties= loadCompareConfiguration();
		assertNotNull("could not load configuration", properties);

		String compareOldPath= properties.getProperty("compare.old");
		if (compareOldPath == null || compareOldPath.indexOf("N2") > 0) {
			return; // Nightly build, skip test
		}

		String comparatorUrl= System.getProperty("RELENGTEST.COMPARATOR.URL");
		if (comparatorUrl == null) {
			return; // No logs to check, skip test 
		}
		URL comparatorLogs= new URL(comparatorUrl);
		ComparatorLog comparatorLog= new ComparatorLog(comparatorLogs);
		String message= "comparator warnings in: \n";
		boolean problemLogsExist= comparatorLog.logs.size() > 0;
		message= message.concat("See the 'comparatorlog.txt' in 'Release engineering build logs' for details.");
		assertTrue(message, !problemLogsExist);
	}

	private class ComparatorLog {
		private ArrayList logs = new ArrayList();

		private ComparatorLog(URL comparatorLogs) {
			findProblems(comparatorLogs);
		}

		private void findProblems(URL comparatorLogs) {
		
			String COMPARATOR_ERROR = "difference found";		

			BufferedReader in = null;
		
				try {
					in = new BufferedReader(new InputStreamReader(
							comparatorLogs.openStream()));
					String tmp;
					while ((tmp = in.readLine()) != null) {
						tmp = tmp.toLowerCase();
						if (tmp.indexOf(COMPARATOR_ERROR) != -1) {										
							String fileName = new File(comparatorLogs.getFile())
									.getName();
							if (!logs.contains(fileName))
								logs.add(fileName);
						}
					}
					in.close();

				} catch (FileNotFoundException e) {
					logs.add("Unable to find "
							+ new File(comparatorLogs.getFile()).getName()
							+ " to read.");
					e.printStackTrace();
				} catch (IOException e) {
					logs.add("Unable to read "
							+ new File(comparatorLogs.getFile()).getName());
					e.printStackTrace();
				}
			}		
	}

	
	
	/*
	 * Load the configuration file which should be included in this bundle
	 */
	private Properties loadCompareConfiguration() {
		String aString = System.getProperty("PLUGIN_PATH");
		if (aString == null)
			return null;

		final String CONFIG_FILENAME= aString + File.separator + "compare.properties";
		Properties properties = new Properties();
		try {
			properties.load(new BufferedInputStream(new FileInputStream(
					CONFIG_FILENAME)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return properties;
	}

	// private String buildCommandLine(String source, String destination, String
	// options, String output) {
	private String buildCommandLine(String source, String destination,
			String output, String option) {

		Bundle bundle = Platform.getBundle("org.eclipse.equinox.launcher");
		URL u = null;
		String temp, t = null;
		try {
			u = FileLocator.resolve(bundle.getEntry("/"));
			temp = u.toString();
			// remove extraneous characters from string that specifies equinox
			// launcher filesystem location!
			t = temp.substring(temp.indexOf("/"), temp.lastIndexOf("/") - 1);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String javadir = System.getProperty("java.home");
		javadir += File.separator + "bin" + File.separator + "java";

		String command = javadir
				+ " -cp "
				+ t
				+ " org.eclipse.core.launcher.Main -application org.eclipse.pde.tools.versioning.application -clean";

		command += " -new " + source;
		command += " -old " + destination;
		command += " -output " + output;
		if (option != null) {
			command += " -option " + option;
		}
		
		System.out.println("commmand "+ command);
		System.out.println("option "+ option);

		return command;
	}

	private void verifyCompareResults(String source, String destination,
			String output) {
		Document doc = parseResultXML(new File(output));
		if (doc == null) {
			String msg = "output file is null";
			assertTrue(msg, msg == null);
			return;
		}
		NodeList list = doc.getElementsByTagName("Category");
		int errorNumber = getSubElementNumber("Error", list);
		int warningNumber = getSubElementNumber("Warning", list);
		if (errorNumber != 0 || warningNumber != 0) {
			String msg = "Features included in \"" + source
					+ "\" has been compared to those included in \""
					+ destination + "\".\n";
			msg += "There are " + errorNumber + " error messages and "
					+ warningNumber + " warning messages.\n";
			msg += "See the version compare logs linked from the test results page for details";
			assertTrue(msg, msg == null);
		}
	}

	/*
	 * Helper method to perform some action when we are unable to run the
	 * version compare mechanism.
	 * 
	 * For now just print out a message to the console and return.
	 */
	private void unableToRunCompare(String message) {
		System.out.println(message);
	}

	/**
	 * Compares the feature and plug-in versions contained in this Eclipse
	 * configuration, against a known previous Eclipse configuration.
	 */
	public void testVersionCompare() {

		String os = System.getProperty("os.name");

		/* Only run compare tool on Linux to save time during tests */
		if (!os.equalsIgnoreCase("Linux")) {
			return;
		}
		String msg = null;

		Bundle bundle = Platform.getBundle("org.eclipse.pde.tools.versioning");
		if (bundle == null) {
			msg = "Version comparison bundle (org.eclipse.pde.tools.versioning) not installed.";
			unableToRunCompare(msg);
			assertTrue(msg, msg == null);
			return;
		}
		// load the configuration and ensure the mandatory parameters were
		// specified
		Properties properties = loadCompareConfiguration();
		// assume that #load printed out error message and just return
		if (properties == null) {
			msg = "Properties file is null";
			assertTrue(msg, msg == null);
			return;
		}

		String compareOldPath = properties.getProperty("compare.old");
		File compareOldFile = compareOldPath == null ? null : new File(
				compareOldPath);
		if (compareOldFile == null) {
			msg = "Old directory not specified.";
			unableToRunCompare(msg);
			assertTrue(msg, msg == null);
			return;
		}

		/*
		 * Determine if the build is a nightly Nightly builds have qualifiers
		 * identical the buildId - for instance N200612080010 which means that
		 * they are lower than v20060921-1945 from an promoted integration build
		 * and thus cannot be compared 
		 */
		 

		// disable temporarily
		if (compareOldPath == null || compareOldPath.indexOf("N2") > 0) {
			// if nightly build, skip test
			return;
		}

		String compareNewPath = properties.getProperty("compare.new");
		File compareNewFile = compareNewPath == null ? null : new File(
				compareNewPath);
		if (compareNewFile == null) {
			msg = "New directory not specified.";
			unableToRunCompare(msg);
			assertTrue(msg, msg == null);
			return;
		}

		String outputFileName = properties.getProperty("compare.output");
		File compareOutputFile = outputFileName == null ? null : new File(
				outputFileName);
		if (compareOutputFile == null) {
			msg = "Output directory not specified.";
			unableToRunCompare(msg);
			assertTrue(msg, msg == null);
			return;
		}
		
		String compareOptions = properties.getProperty("compare.options");
				
	/*	
		 * String outputFileName =
		 * Platform.getInstallLocation().getURL().getPath() + ".." +
		 * File.separator + ".." + File.separator + "results" + File.separator +
		 * "results.xml"; // A tad bogus but this is where the build wants to
		 * copy the results from!
		 * 
		 * //create the output file try { File outputfile = new
		 * File(outputFileName); boolean created = outputfile.createNewFile();
		 * if (created) { } else { msg = "Output dir could not be created.";
		 * assertTrue(msg, msg == null); } } catch (IOException e) {
		 * e.printStackTrace(); } 
*/
		 

		String command = buildCommandLine(compareNewPath, compareOldPath,
				outputFileName, compareOptions);

		// System.out.println("command "+ command);

		try {
			Process aProcess = Runtime.getRuntime().exec(command);
			try {
				// wait until the comparison finishes
				aProcess.waitFor();
			} catch (InterruptedException e) {
				// ignore
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// analyze compare result
		verifyCompareResults(compareNewPath, compareOldPath, outputFileName);

	}

	/**
	 * parses the given XML file denoted by <code>file</code> and return
	 * Document instance of it
	 * 
	 * @param file
	 *            File instance which denoted an XML file
	 * @return Document instance
	 */
	private Document parseResultXML(File file) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			return docBuilder.parse(file);
		} catch (ParserConfigurationException e) {
			System.out.println("Unable to parse comparison result file.");
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			System.out.println("Unable to create XML parser.");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Unable to parse comparison result file.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out
					.println("Exception trying to parse comparison result file.");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * searches for element whose name attribute equals <code>elementName</code>,
	 * and return the number of its child nodes if it has been found
	 * 
	 * @param elementName
	 *            value of <code>name</code> attribute
	 * @param nodeList
	 *            NodeList instance
	 * @return number of child nodes of the element whose <code>name</code>
	 *         attribute equals <code>elementName</code>
	 */
	private int getSubElementNumber(String elementName, NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element element = ((Element) nodeList.item(i));
			if (element.getAttribute("Name").equals(elementName))
				return element.getChildNodes().getLength();
		}
		return 0;
	}

	/**
	 * check whether or not the given file potentially represents a platform
	 * configuration file on the file-system.
	 * 
	 * @return <code>true</code> if <code>file</code> is a configuration
	 *         file <code>false</code> otherwise
	 * 
	 * private boolean isConfiguration(File file) { IPath path = new
	 * Path(file.getAbsolutePath()); return file.isFile() &&
	 * "platform.xml".equalsIgnoreCase(path.lastSegment()); }
	 */
}
