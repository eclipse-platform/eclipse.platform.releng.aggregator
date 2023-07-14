/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.osgi.framework.Version;

public class BuildTests {

	private static final String PLATFORM_FEATURE = "org.eclipse.platform_";

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

	@Test
	public void testProductFileVersion() throws Exception {
		assumeFalse(isMavenRun());
		String installDir = Platform.getInstallLocation().getURL().getPath();
		File productFile = new File(installDir, ".eclipseproduct");
		Properties props = new Properties();
		try (InputStream stream = Files.newInputStream(productFile.toPath())) {
			props.load(stream);
		}
		String versionProp = props.getProperty("version");
		assertNotNull("'version' property not found in " + productFile, versionProp);

		File featureDir = new File(installDir, "features");
		String versionStr = null;
		for (File feature : featureDir.listFiles()) {
			String featureName = feature.getName();
			if (!featureName.startsWith(PLATFORM_FEATURE)) {
				continue;
			}
			// something like org.eclipse.platform_4.28.0.v20230328-1800
			versionStr = featureName.substring(PLATFORM_FEATURE.length(), featureName.length());
			break;
		}
		assertNotNull("Failed to find platform feature at " + featureDir, versionStr);
		assertTrue("Failed to find product file at " + productFile, productFile.isFile());
		Version featureVersion = new Version(versionStr);
		featureVersion = new Version(featureVersion.getMajor(), featureVersion.getMinor(), featureVersion.getMicro());
		Version productVersion = new Version(versionProp);
		assertEquals("Product version doesn't match version of the platform", featureVersion, productVersion);
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
