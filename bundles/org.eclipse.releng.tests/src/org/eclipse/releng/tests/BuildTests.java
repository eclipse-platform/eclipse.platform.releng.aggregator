package org.eclipse.releng.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.boot.BootLoader;

import junit.framework.TestCase;

public class BuildTests extends TestCase {
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
		public void testChkpii() {
			
			String zipFile = locateEclipseZip();
			String sniffFolder = BootLoader.getInstallURL().getPath() + "releng_sniff_folder";
		
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
			System.out.println(chkpiiString);
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
			
			String installDir = BootLoader.getInstallURL().getPath() + ".." + File.separator + "..";  // Only one .. for debug
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
			String aString = BootLoader.getInstallURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "chkpii";
//			String aString = BootLoader.getInstallURL().getPath() + "..\\results\\chkpii";
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
			
			String sniffFolder = BootLoader.getInstallURL().getPath() + "releng_sniff_folder" + File.separator;
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
//			String os = BootLoader.getOS();
//			if (os == null) {
//				System.out.println("OS IS NULL");
//			} else {
//				System.out.println("OS: " + os);
//			}
			
//			String exeName;
//			if (os.equals(BootLoader.OS_UNKNOWN)) {
//				exeName = "chkpw501.exe";
//			} else {
//				exeName = "chkpl501.exe";
//			}
//			String aString = BootLoader.getInstallURL().getPath() + "plugins" + File.separator + "org.eclipse.releng.tests_2.1.0" + File.separator + exeName;
			
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
