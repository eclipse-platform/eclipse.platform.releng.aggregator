package org.eclipse.releng.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.boot.BootLoader;

import junit.framework.TestCase;

public class BuildTests extends TestCase {
	
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
	
	public void testPluginFiles() {
		List result = new ArrayList();
		String installDir = BootLoader.getInstallURL().getPath();
		File pluginDir = new File(installDir, "plugins");
		File[] plugins = pluginDir.listFiles();
		for (int i = 0; i < plugins.length; i++) {
			File aPlugin = plugins[i];
			if (!testPluginFile(aPlugin)) {
				result.add(aPlugin.getPath());
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
