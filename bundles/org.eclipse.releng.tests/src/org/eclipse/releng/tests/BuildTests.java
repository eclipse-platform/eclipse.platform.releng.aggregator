/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;

public class BuildTests {

	private String           logFileName;
	private static final int HTML       = 0;
	private static final int PROPERTIES = 1;
	private static final int XML        = 2;

	private static FileTool.IZipFilter getTrueFilter() {
		return new FileTool.IZipFilter() {

			@Override
			public boolean shouldExtract(String fullEntryName, String entryName, int depth) {
				return true;
			}

			@Override
			public boolean shouldUnzip(String fullEntryName, String entryName, int depth) {
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

		try (BufferedReader aReader = new BufferedReader(new InputStreamReader(new FileInputStream(string)))){
			String aLine = aReader.readLine();
			while (aLine != null) {
				int aNumber = parseLine(aLine);
				if (aNumber > 0) {
					result = true;
				}
				aLine = aReader.readLine();
			}
		}
		catch (FileNotFoundException e) {
			System.out.println("Could not open log file: " + string);
			result = true;
		}
		catch (IOException e) {
			System.out.println("Error reading log file: " + string);
			result = true;
		}
		return result;
	}
	@Test
	public void testChkpii() {

		try {
			// test that chkpii is on path by printing chkpii help information
			Runtime aRuntime = Runtime.getRuntime();
			Process aProcess = aRuntime.exec(getExec() + " /?");
			BufferedReader aBufferedReader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
			while (aBufferedReader.readLine() != null) {
			}
			aProcess.waitFor();
		} catch (IOException e) {
			// skip chkpii test if chkpii cannot be run.
			System.out.println("testChkpii-NotInstalled");
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
			if (zipFile.isEmpty()) {
				FileTool.unzip(getTrueFilter(), new File(sniffFolder));
			} else {
				FileTool.unzip(getTrueFilter(), new ZipFile(zipFile), new File(sniffFolder));
			}
		}
		catch (IOException e) {
			fail(zipFile + ": " + sniffFolder + ": " + "IOException unzipping Eclipse for chkpii");
		}

		boolean result1 = testChkpii(HTML);
		boolean result2 = testChkpii(XML);
		boolean result3 = testChkpii(PROPERTIES);
		assertTrue("Translation errors in files.  See the chkpii logs linked from the test results page for details.",
				(result1 && result2 && result3));
	}

	private boolean testChkpii(int type) {
		Runtime aRuntime = Runtime.getRuntime();
		String chkpiiString = getChkpiiString(type);
		try {
			Process aProcess = aRuntime.exec(chkpiiString);
			BufferedReader aBufferedReader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
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
	 * @param type
	 * @return String
	 */
	private String getChkpiiString(int type) {
		return getExec() + " " + getFilesToTest(type) + " -E -O " + getOutputFile(type) + " -XM @" + getExcludeErrors() + " -X "
				+ getExcludeFile() + " -S /jsq /tex";
	}

	/**
	 * Method locateEclipseZip.
	 *
	 * @return String
	 */
	private String locateEclipseZip() {

		// String to use when running as an automated test.
		String installDir = Platform.getInstallLocation().getURL().getPath() + ".." + File.separator + "..";

		// String to use when running in Eclipse
		// String installDir = BootLoader.getInstallURL().getPath() + "..";
		File aFile = new File(installDir);

		for (File file : aFile.listFiles()) {
			String fileName = file.getName();
			if (fileName.startsWith("eclipse-SDK-") && fileName.endsWith(".zip")) {
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
	 * @param type
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
	 * @param type
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
	 * @return -1 if not an error or warning line or the number of errors or warnings.
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

		@Override
		public boolean accept(File dir, String name) {
			int lastDot = name.lastIndexOf('.');
			if (lastDot == -1) {
				return false;
			}
			return name.substring(lastDot).equals(suffix);
		}

	}

	public static final List<String> REQUIRED_FEATURE_FILES_EPL2 = List.of("epl-2.0.html", "feature.properties",
			"feature.xml", "license.html");
	public static final String REQUIRED_FEATURE_SUFFIX = "";

	public static final List<String> REQUIRED_PLUGIN_FILES = List.of("about.html", "plugin.properties", "plugin.xml");
	public static final String REQUIRED_PLUGIN_SUFFIX = ".jar";

	public static final List<String> REQUIRED_FEATURE_PLUGIN_FILES = List.of("about.html", "about.ini",
			"about.mappings", "about.properties", "plugin.properties", "plugin.xml");
	public static final String REQUIRED_FEATURE_PLUGIN_SUFFIX = ".gif";

	public static final List<String> REQUIRED_FRAGMENT_FILES = List.of("fragment.xml");
	public static final String REQUIRED_FRAGMENT_SUFFIX = "";

	public static final List<String> REQUIRED_SWT_FRAGMENT_FILES = List.of("fragment.properties");
	public static final String REQUIRED_SWT_FRAGMENT_SUFFIX = "";

	public static final List<String> REQUIRED_SOURCE_FILES = List.of("about.html");
	public static final String REQUIRED_SOURCE_SUFFIX = ".zip";

	public static final List<String> REQUIRED_BUNDLE_FILES = List.of("about.html");
	public static final String REQUIRED_BUNDLE_MANIFEST = "MANIFEST.MF";
	public static final String REQUIRED_BUNDLE_SUFFIX = ".jar";

	public static final List<String> SUFFIX_EXEMPT_LIST = List.of("org.eclipse.swt", "org.apache.ant");

	public static final int PLUGIN_COUNT  = 84; // - 20; // Note this number
	// must include non-shipping
	// test plugins
	public static final int FEATURE_COUNT = 9;  // - 1; // Note this number must

	// include non-shipping test
	// feature

	@Before
	public void setUp() {
		// Automated Test
		logFileName = Platform.getInstallLocation().getURL().getPath() + ".." + File.separator + ".." + File.separator + "results"
				+ File.separator + "chkpii"; // A tad bogus but this is where
											 // the build wants to copy the
											 // results from!

		// Runtime Workbench - TODO Put me back to Automated status
		// logFileName = "d:\\results";
		// sourceDirectoryName = "d:\\sourceFetch";
	}
	@Test
	public void testFeatureFiles() {
		assumeFalse(isMavenRun());
		List<String> result = new ArrayList<>();
		String installDir = Platform.getInstallLocation().getURL().getPath();

		File featureDir = new File(installDir, "features");
		for (File aFeature : featureDir.listFiles()) {
			List<String> testFiles = REQUIRED_FEATURE_FILES_EPL2;
			if (!testDirectory(aFeature, testFiles, REQUIRED_FEATURE_SUFFIX)) {
				result.add(aFeature.getPath());
			}
		}

		String aString = "";
		if (!result.isEmpty()) {
			for (String element: result) {
				aString = aString + element + "; ";
			}
		}
		assertTrue("Feature directory missing required files: " + aString, result.isEmpty());
	}

	/**
	 * Verifies all bundles starting with org.eclipse (with listed exceptions) to
	 * contain e.g. about.html and other recommended files.
	 */
	@Test
	public void testPluginFiles() {
		assumeFalse(isMavenRun());
		List<String> result = new ArrayList<>();
		String installDir = Platform.getInstallLocation().getURL().getPath();
		File pluginDir = new File(installDir, "plugins");
		for (File aPlugin : pluginDir.listFiles()) {
			if (!aPlugin.getName().contains("test") && aPlugin.getName().startsWith("org.eclipse")
					&& !aPlugin.getName().contains("org.eclipse.jetty")
					&& !aPlugin.getName().contains("org.eclipse.ecf")) {
				if (!testPluginFile(aPlugin)) {
					result.add(aPlugin.getPath());
				}
			}
		}

		String aString = "";
		if (!result.isEmpty()) {
			for (String element : result) {
				aString = aString + element + "; ";
			}
		}
		assertTrue("Plugin directory missing required files: " + aString, result.isEmpty());
	}

	private boolean isMavenRun() {
		return System.getenv("MAVEN_CMD_LINE_ARGS") != null;
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
		if ((testDirectory(aPlugin, REQUIRED_FRAGMENT_FILES, REQUIRED_FRAGMENT_SUFFIX))
				|| (testBundleDirectory(aPlugin, REQUIRED_BUNDLE_FILES, REQUIRED_BUNDLE_MANIFEST, REQUIRED_FRAGMENT_SUFFIX))) {
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

	private boolean testPluginJar(File aDirectory, List<String> requiredFiles) {
		ArrayList<String> list = new ArrayList<>();
		try (ZipFile jarredPlugin = new ZipFile(aDirectory)) {
			Enumeration<? extends ZipEntry> _enum = jarredPlugin.entries();
			while (_enum.hasMoreElements()) {
				list.add(_enum.nextElement().toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!list.containsAll(requiredFiles)) {
			return false;
		}
		return true;
	}

	private boolean testDirectory(File aDirectory, List<String> requiredFiles, String requiredSuffix) {
		if (aDirectory.getName().endsWith(".jar")) {
			return testPluginJar(aDirectory, requiredFiles);
		} else {
			if (!Arrays.asList(aDirectory.list()).containsAll(requiredFiles)) {
				return false;
			}

			int index = aDirectory.getName().indexOf('_');
			if (index == -1) {
				index = aDirectory.getName().length();
			}

			String plainName = aDirectory.getName().substring(0, index);

			if (requiredSuffix.isEmpty() || SUFFIX_EXEMPT_LIST.contains(plainName)) {
				return true;
			} else if (aDirectory.listFiles(new FileSuffixFilter(requiredSuffix)).length == 0) {
				return false;
			}
		}
		return true;
	}

	private boolean testBundleDirectory(File aDirectory, List<String> requiredFiles, String manifestFile, String requiredSuffix) {
		if (aDirectory.getName().endsWith(".jar")) {
			return testPluginJar(aDirectory, requiredFiles);
		} else {
			if (!Arrays.asList(aDirectory.list()).containsAll(requiredFiles)) {
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
				for (String filename : metaFiles) {
					if (filename == manifestFile) {
						return true;
					}
				}
			}

			if (!metaDir.exists()) {
				return false;
			}

			if (requiredSuffix.isEmpty() || SUFFIX_EXEMPT_LIST.contains(plainName)) {
				return true;
			} else if (aDirectory.listFiles(new FileSuffixFilter(requiredSuffix)).length == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return true if the receiver is a source plugin, false otherwise A separate method because this is a little tricky.
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

		for ( File aSourceDir : sourceDirs) {
			if (!testDirectory(aSourceDir, REQUIRED_SOURCE_FILES, REQUIRED_SOURCE_SUFFIX)) {
				return false;
			}
		}
		return true;
	}
	@Test
	public void testJavadocLogs() throws Exception {
		assumeFalse(isMavenRun());
		String javadocUrls = System.getProperty("RELENGTEST.JAVADOC.URLS");
		// Skip this test if there are no logs to check
		if (javadocUrls == null) {
			System.err.println("WARNING: no javadoc logs to test, since RELENGTEST.JAVADOC.URLS property was not set");
			return;
		} else {
			System.err.println("RELENGTEST.JAVADOC.URLS: " + javadocUrls);
		}

		String[] urls = javadocUrls.split(",");
		URL[] javadocLogs = new URL[urls.length];
		for (int i = 0; i < urls.length; i++) {
			javadocLogs[i] = new URL(urls[i]);
			System.err.println("javadocLogs[" + i + "]: " + javadocLogs[i]);
		}

		JavadocLog javadocLog = new JavadocLog(javadocLogs);
		String message = "javadoc errors and/or warnings in: \n";
		boolean problemLogsExist = !javadocLog.logs.isEmpty();
		if (problemLogsExist) {
			for (int i = 0; i < javadocLog.logs.size(); i++)
				message = message.concat(javadocLog.logs.get(i) + "\n");
		}
		message = message.concat("See the javadoc logs linked from the test results page for details");
		assertTrue(message, !problemLogsExist);
	}


	@Test
	public void testJarSign() throws Exception {
		assumeFalse(isMavenRun());
		String buildId = System.getProperty("buildId");
		assertNotNull("buildId property must be specified for testJarSign test", buildId);
		String downloadHost = "download.eclipse.org";
		String urlOfFile = "https://" + downloadHost + "/eclipse/downloads/drops4/" + buildId
				+ "/buildlogs/reporeports/reports/unsigned8.txt";
		URL logURL = new URL(urlOfFile);

		URLConnection urlConnection = logURL.openConnection();
		long nBytes = urlConnection.getContentLength();
		// nBytes will be -1 if the file doesn't exist
		// it will be more than 3 if there are unsigned jars. (atlear jar extention will be listed

		assertTrue("Some bundles are unsigned please refer  " + urlOfFile, (2 > nBytes));
	}

	private String getDownloadHost() {
		String downloadHost = System.getProperty("downloadHost");
		if (downloadHost == null) {
			downloadHost = "download.eclipse.org";
		}
		return downloadHost;
	}

	private void printHeaders(URLConnection urlConnection) {
		System.out.println("Debug: Headers for urlConnection to " + urlConnection.getURL());
		Map<String, List<String>> allFields = urlConnection.getHeaderFields();
		for (Entry<String, List<String>> entry : allFields.entrySet()) {
			for (String value : entry.getValue()) {
				System.out.printf("Debug: %-20s %-30s %n", "key: " + entry.getKey(), "value: " + value);
			}
		}
	}
	@Test
	public void testComparatorLogSize() throws Exception {
		assumeFalse(isMavenRun());
		final boolean DEBUG_TEST = true;
		// MAX_ALLOWED_BYTES will never be 'zero', even if no unexpected comparator warnings, because the
		// report always contains some information, such as identifying which build it was for.
		// The goal, here, is to
		// "hard code" nominal values, to make sure
		// there is no regressions, which would be implied by a report larger
		// than that nominal value.
		long MAX_ALLOWED_BYTES = 319;
		String buildId = System.getProperty("buildId");
		assertNotNull("buildId property must be specified for testComparatorLogSize test", buildId);
		// Comparator logs are not generated for N builds
		String downloadHost = getDownloadHost();
		String urlOfFile = "https://" + downloadHost + "/eclipse/downloads/drops4/" + buildId
				+ "/buildlogs/comparatorlogs/buildtimeComparatorUnanticipated.log.txt";
		URL logURL = new URL(urlOfFile);

		URLConnection urlConnection = logURL.openConnection();
		// urlConnection.connect();
		long nBytes = urlConnection.getContentLength();
		if (DEBUG_TEST) {
			System.out.println("Debug info for testComparatorLogSize");
			System.out.println("Debug: nBytes: " + nBytes);
			printHeaders(urlConnection);
		}
		// if find "response does not contain length, on a regular basis, for
		// some servers, will have to read contents.
		assertTrue("Either file (url) does not exist, or HTTP response does not contain content length. urlOfFile: "
				+ urlOfFile, (-1 != nBytes));
		assertTrue("Unanticipated comparator log file has increased in size, indicating a regression. See " + urlOfFile,
				nBytes <= MAX_ALLOWED_BYTES);
		if (MAX_ALLOWED_BYTES > (nBytes + 20)) {
			System.out.println(
					"WARNING: MAX_ALLOWED_BYTES was larger than bytes found, by " + (MAX_ALLOWED_BYTES - nBytes)
							+ ", which may indicate MAX_ALLOWED_BYTES needs to be lowered, to catch regressions.");
		}
	}

	private class JavadocLog {

		private ArrayList<String> logs = new ArrayList<>();

		private JavadocLog(URL[] logs) {
			findProblems(logs);
		}

		private void findProblems(URL[] javadocLogs) {
			String JAVADOC_WARNING = ": warning";
			String JAVADOC_ERROR = ": error";
			String JAVADOC_JAVA = ".java:";

			for (URL javadocLog : javadocLogs) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(javadocLog.openStream()))){
					String tmp;
					while ((tmp = in.readLine()) != null) {
						tmp = tmp.toLowerCase();
						if (tmp.contains(JAVADOC_ERROR) || tmp.contains(JAVADOC_WARNING)
								|| tmp.contains(JAVADOC_JAVA)) {
							String fileName = new File(javadocLog.getFile()).getName();
							if (!logs.contains(fileName))
								logs.add(fileName);
						}
					}
				}
				catch (FileNotFoundException e) {
					logs.add("Unable to find " + new File(javadocLog.getFile()).getName() + " to read.");
					e.printStackTrace();
				}
				catch (IOException e) {
					logs.add("Unable to read " + new File(javadocLog.getFile()).getName());
					e.printStackTrace();
				}
			}
		}
	}

}
