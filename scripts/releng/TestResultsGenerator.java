
/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Convert to plain Java scripts
 *******************************************************************************/

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Dean Roberts (circa 2000!) and David Williams (circa 2016)
 */
public class TestResultsGenerator {

	public class ResultsTable implements Iterable<String> {

		private final Map<String, Row> rows = new TreeMap<>();
		private final List<String> columns;

		public ResultsTable(List<String> columns) {
			this.columns = columns;
		}

		public record Cell(int errorCount, File resultsFile) {
		}

		private class Row {

			Map<String, Cell> row = new TreeMap<>();

			public Row(List<String> columns) {
				for (String column : columns) {
					row.put(column, null);
				}
			}

			public Cell getCell(String column) {
				return row.get(column);
			}

			public void putCell(String columnName, Integer cellValue, File file) {
				row.put(columnName, new Cell(cellValue, file));
			}
		}

		private Row getRow(String rowname) {
			Row row = rows.get(rowname);
			if (row == null) {
				row = new Row(columns);
				rows.put(rowname, row);
			}
			return row;
		}

		public Cell getCell(String rowName, String columnName) {
			return getRow(rowName).getCell(columnName);
		}

		public void putCell(String rowName, String columnName, Integer cellValue, File file) {
			getRow(rowName).putCell(columnName, cellValue, file);
		}

		@Override
		public Iterator<String> iterator() {
			return rows.keySet().iterator();
		}
	}

	private static final String HTML_EXTENSION = ".html";
	private static final String XML_EXTENSION = ".xml";
	private static final String WARNING_SEVERITY = "WARNING";
	private static final String ERROR_SEVERITY = "ERROR";
	private static final String INFO_SEVERITY = "INFO";
	private static final String ForbiddenReferenceID = "ForbiddenReference";
	private static final String DiscouragedReferenceID = "DiscouragedReference";

	private static final String elementName = "testsuite";

	private List<String> expectedConfigs = null;
	private static final String EOL = System.lineSeparator();
	private static boolean DEBUG = false;

	private final String expected_config_type = "expected";
	private final String expectedConfigFilename = "testConfigs.php";
	private final String foundConfigFilename = "testConfigsFound.php";

	private ErrorTracker anErrorTracker;

	// Parameters
	// build runs JUnit automated tests
	private boolean isBuildTested;

	// buildType, I, N
	private String buildType;

	// Comma separated list of drop tokens
	private String dropTokenList;

	// Location of the xml files
	private String xmlDirectoryName;

	// Location of the resulting index.php file.
	private String dropDirectoryName;

	// Location and name of the template drop index.php file.
	private String dropTemplateFileName;

	// Name of the HTML fragment file that any testResults.php file will
	// "include".
	// setting to common default.
	private final String testResultsHtmlFileName = "testResultsTables.html";

	// Name of the generated drop index php file;
	private String dropHtmlFileName;

	// Arbitrary path used in the index.php page to href the
	// generated .html files.
	private final String hrefTestResultsTargetPath = "testresults";
	// Arbitrary path used in the index.php page to reference the compileLogs
	private final String hrefCompileLogsTargetPath = "compilelogs/plugins/";
	// Location of compile logs base directory
	private String compileLogsDirectoryName;
	// Location and name of test manifest file
	private String testManifestFileName;
	// private static String testsConstant = ".tests";
	// private static int testsConstantLength = testsConstant.length();
	// temporary way to force "missing" list not to be printed (until complete
	// solution found)
	private final boolean doMissingList = true;

	private final Set<String> missingManifestFiles = Collections.checkedSortedSet(new TreeSet<>(), String.class);

	class ExpectedConfigFiler implements FilenameFilter {

		String configEnding;

		public ExpectedConfigFiler(String expectedConfigEnding) {
			configEnding = expectedConfigEnding;
		}

		@Override
		public boolean accept(File dir, String name) {
			return (name.endsWith(configEnding));
		}

	}

	private void logException(final Throwable e) {
		log(EOL + "ERROR: " + e.getMessage());
		StackTraceElement[] stackTrace = e.getStackTrace();
		for (StackTraceElement stackTraceElement : stackTrace) {
			log(stackTraceElement.toString());
		}
	}

	// Configuration of test machines.
	// Add or change new configurations here
	// and update titles in testResults.template.php.
	// These are the suffixes used for JUnit's XML output files.
	// On each invocation, all files in results directory are
	// scanned, to see if they end with suffixes, and if so,
	// are processed for summary row. The column order is determined by
	// the order listed here.
	// This suffix is determined, at test time, when the files junit files are
	// generated, by the setting of a variable named "platform" in test.xml
	// and associated property files.

	// no defaults set since adds to confusion or errors
	// private String[] testsConfigDefaults = { "ep4" + getTestedBuildType() +
	// "-unit-cen64-gtk2_linux.gtk.x86_64_8.0.xml",
	// "ep4" + getTestedBuildType() + "-unit-mac64_macosx.cocoa.x86_64_8.0.xml",
	// "ep4" + getTestedBuildType() + "-unit-win32_win32.win32.x86_8.0.xml",
	// "ep4" + getTestedBuildType() + "-unit-cen64-gtk3_linux.gtk.x86_64_8.0.xml" };
	private String testsConfigExpected;
	private final String compilerSummaryFilename = "compilerSummary.html";
	/*
	 * Default for "regenerate" is FALSE, but during development, is handy to set to
	 * TRUE. If TRUE, the "index.php" file and "compilerSummary.html" files are
	 * regenerated. In production that should seldom be required. The
	 * testResultsTables.html file, however, is regenerated each call (when
	 * 'isTested" is set) since the purpose is usually to include an additional
	 * tested platform.
	 */
	private final boolean regenerate = false;

	private int countCompileErrors(final String aString) {
		return extractNumber(aString, "error");
	}

	private int countCompileWarnings(final String aString) {
		return extractNumber(aString, "warning");
	}

	private int countDiscouragedWarnings(final String aString) {
		return extractNumber(aString, "Discouraged access:");
	}

	private int countInfos(final String aString) {
		return extractNumber(aString, "info");
	}

	/*
	 * returns number of errors plus number of failures. returns a negative number
	 * if the file is missing or something is wrong with the file (such as is
	 * incomplete).
	 */
	private int countErrors(final String fileName) {
		int errorCount = -99;
		// File should exists, since we are "driving" this based on file list
		// ... but, just in case.
		if (!new File(fileName).exists()) {
			errorCount = -1;
		} else {

			if (new File(fileName).length() == 0) {
				errorCount = -2;
			} else {

				try {
					DocumentBuilder parser = createDocumentBuilderWithErrorOnDOCTYPE();
					final Document document = parser.parse(fileName);
					final NodeList elements = document.getElementsByTagName(elementName);

					final int elementCount = elements.getLength();
					if (elementCount == 0) {
						errorCount = -3;
					} else {
						// There can be multiple "testSuites" per file so we
						// need to
						// loop through each to count all errors and failures.
						errorCount = 0;
						for (int i = 0; i < elementCount; i++) {
							final Element element = (Element) elements.item(i);
							final NamedNodeMap attributes = element.getAttributes();
							Node aNode = attributes.getNamedItem("errors");
							if (aNode != null) {
								errorCount = errorCount + Integer.parseInt(aNode.getNodeValue());
							}
							aNode = attributes.getNamedItem("failures");
							errorCount = errorCount + Integer.parseInt(aNode.getNodeValue());
						}
					}

				} catch (final IOException e) {
					log(EOL + "ERROR: IOException: " + fileName);
					logException(e);
					errorCount = -4;
				} catch (final SAXException e) {
					log(EOL + "ERROR: SAXException: " + fileName);
					logException(e);
					errorCount = -5;
				} catch (final ParserConfigurationException e) {
					logException(e);
					errorCount = -6;
				}
			}
		}
		return errorCount;
	}

	private int countForbiddenWarnings(final String aString) {
		return extractNumber(aString, "Access restriction:");
	}

	public static void main(String[] args) {
		TestResultsGenerator generator = new TestResultsGenerator();
		generator.isBuildTested = Boolean.parseBoolean(System.getProperty("isBuildTested", "false"));

		generator.buildType = Objects.requireNonNull(System.getProperty("buildType"), "Not set: buildType");
		generator.compileLogsDirectoryName = System.getProperty("compileLogsDirectoryName");
		generator.dropDirectoryName = Objects.requireNonNull(System.getProperty("dropDirectoryName"),
				"Not set: dropDirectoryName");
		generator.dropHtmlFileName = System.getProperty("dropHtmlFileName");
		generator.dropTemplateFileName = System.getProperty("dropTemplateFileName");
		generator.dropTokenList = System.getProperty("dropTokenList");
		generator.testManifestFileName = Objects.requireNonNull(System.getProperty("testManifestFileName"),
				"Not set: testManifestFileName");

		generator.xmlDirectoryName = System.getProperty("xmlDirectoryName");
		generator.testsConfigExpected = System.getProperty("testsConfigExpected");

		generator.execute();
	}

	public void execute() {

		log(EOL + "INFO: Processing test and build results for ");
		log("\t" + dropDirectoryName);
		anErrorTracker = new ErrorTracker();
		anErrorTracker.loadFile(testManifestFileName);

		writeDropIndexFile();

		try {
			parseCompileLogs();
		} catch (IOException e) {
			throw new IllegalStateException("Error while parsing Compiler Results File ", e);
		}

		if (isBuildTested) {

			try {
				parseJUnitTestsXml();

			} catch (IOException e) {
				throw new IllegalStateException("Error while parsing JUnit Tests Results Files", e);
			}

		} else {
			log(EOL + "INFO: isBuildTested value was not true, so did no processing for test files");
		}
		log(EOL + "INFO: Completed processing test and build results");
	}

	private int extractNumber(final String aString, final String endToken) {
		final int endIndex = aString.lastIndexOf(endToken);
		if (endIndex == -1) {
			return 0;
		}

		int startIndex = endIndex;
		while ((startIndex >= 0) && (aString.charAt(startIndex) != '(') && (aString.charAt(startIndex) != ',')) {
			startIndex--;
		}

		final String count = aString.substring(startIndex + 1, endIndex).trim();
		try {
			return Integer.parseInt(count);
		} catch (final NumberFormatException e) {
			return 0;
		}

	}

	private void formatAccessesErrorRow(final String fileName, final int forbiddenAccessesWarningsCount,
			final int discouragedAccessesWarningsCount, final int infoCount, final StringBuilder buffer) {

		if ((forbiddenAccessesWarningsCount == 0) && (discouragedAccessesWarningsCount == 0) && (infoCount == 0)) {
			return;
		}

		String relativeName = computeRelativeName(fileName);
		String shortName = computeShortName(relativeName);

		buffer.append("<tr>").append(EOL).append("<td class='namecell'>").append(EOL).append("<a href=").append("\"")
		.append(relativeName).append("\">").append(shortName).append("</a>").append("</td>\n")
		.append("<td class=\"cell\" >").append("<a href=").append("\"").append(relativeName)
		.append("#FORBIDDEN_WARNINGS").append("\">").append(forbiddenAccessesWarningsCount).append("</a>")
		.append("</td>").append(EOL).append("<td class=\"cell\" >").append("<a href=").append("\"")
		.append(relativeName).append("#DISCOURAGED_WARNINGS").append("\">")
		.append(discouragedAccessesWarningsCount).append("</a>").append("</td>").append(EOL)
		.append("<td class=\"cell\" >").append("<a href=").append("\"").append(relativeName)
		.append("#INFO_WARNINGS").append("\">").append(infoCount).append("</a>").append("</td>").append(EOL)
		.append("</tr>").append(EOL);
	}

	private String computeRelativeName(final String fileName) {
		String relativeName;
		final int i = fileName.indexOf(hrefCompileLogsTargetPath);
		relativeName = fileName.substring(i);
		return relativeName;
	}

	private String computeShortName(final String relativeName) {
		String shortName;

		int start = hrefCompileLogsTargetPath.length();
		int last = relativeName.lastIndexOf("/");
		// if there is no "last slash", that's a pretty weird case, but we'll
		// just
		// take the whole rest of string in that case.
		if (last == -1) {
			shortName = relativeName.substring(start);
		} else {
			shortName = relativeName.substring(start, last);
		}
		// further shortening (may need to "back out", so left as separate step)
		// we always expect the name to start with "org.eclipse." .. but, just
		// in case that changes, we'll check and handle if not.
		String commonnamespace = "org.eclipse.";
		if (shortName.startsWith(commonnamespace)) {
			start = commonnamespace.length();
		} else {
			start = 0;
		}
		// Similarly, we alwasy expect the name to end with '_version', but just
		// in case not.
		last = shortName.indexOf('_');
		if (last == -1) {
			shortName = shortName.substring(start);
		} else {
			shortName = shortName.substring(start, last);
		}
		return shortName;
	}

	private void formatCompileErrorRow(final String fileName, final int errorCount, final int warningCount,
			final StringBuilder buffer) {

		if ((errorCount == 0) && (warningCount == 0)) {
			return;
		}

		String relativeName = computeRelativeName(fileName);
		String shortName = computeShortName(relativeName);

		buffer.append("<tr>" + EOL + "<td class='cellname'>" + EOL).append("<a href=").append("\"").append(relativeName)
		.append("\">").append(shortName).append("</a>").append("</td>\n").append("<td class=\"cell\" >")
		.append("<a href=").append("\"").append(relativeName).append("#ERRORS").append("\">").append(errorCount)
		.append("</a>").append("</td>\n").append("<td class=\"cell\" >").append("<a href=").append("\"")
		.append(relativeName).append("#OTHER_WARNINGS").append("\">").append(warningCount).append("</a>")
		.append("</td>\n").append("</tr>\n");
	}

	private void parseCompileLog(final String log, final StringBuilder compilerLog, final StringBuilder accessesLog) {
		int errorCount = 0;
		int warningCount = 0;
		int forbiddenWarningCount = 0;
		int discouragedWarningCount = 0;
		int infoCount = 0;

		final File file = new File(log);
		Document aDocument = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			final InputSource inputSource = new InputSource(reader);
			final DocumentBuilder builder = createDocumentBuilderIgnoringDOCTYPE();

			aDocument = builder.parse(inputSource);
		} catch (final ParserConfigurationException | IOException | SAXException e) {
			logException(e);
		}

		if (aDocument == null) {
			return;
		}
		// Get summary of problems
		final NodeList nodeList = aDocument.getElementsByTagName("problem");
		if ((nodeList == null) || (nodeList.getLength() == 0)) {
			return;
		}

		final int length = nodeList.getLength();
		for (int i = 0; i < length; i++) {
			final Node problemNode = nodeList.item(i);
			final NamedNodeMap aNamedNodeMap = problemNode.getAttributes();
			final Node severityNode = aNamedNodeMap.getNamedItem("severity");
			final Node idNode = aNamedNodeMap.getNamedItem("id");
			if (severityNode != null) {
				final String severityNodeValue = severityNode.getNodeValue();
				if (WARNING_SEVERITY.equals(severityNodeValue)) {
					// this is a warning
					// need to check the id
					final String nodeValue = idNode == null ? "" : idNode.getNodeValue();
					if (ForbiddenReferenceID.equals(nodeValue)) {
						forbiddenWarningCount++;
					} else if (DiscouragedReferenceID.equals(nodeValue)) {
						discouragedWarningCount++;
					} else {
						warningCount++;
					}
				} else if (ERROR_SEVERITY.equals(severityNodeValue)) {
					// this is an error
					errorCount++;
				} else if (INFO_SEVERITY.equals(severityNodeValue)) {
					// this is an info warning
					infoCount++;
				}
			}
		}
		if (errorCount != 0) {
			// use wildcard in place of version number on directory names
			// log(log + "/n");
			String logName = log.substring(compileLogsDirectoryName.length() + 1);
			final StringBuilder buffer = new StringBuilder(logName);
			buffer.replace(logName.indexOf("_") + 1, logName.indexOf(File.separator, logName.indexOf("_") + 1), "*");
			logName = new String(buffer);

			anErrorTracker.registerError(logName);
		}
		// make sure '.xml' extension is "last thing" in string. (bug 490320)
		final String logName = log.replaceAll(XML_EXTENSION + "$", HTML_EXTENSION);
		formatCompileErrorRow(logName, errorCount, warningCount, compilerLog);
		formatAccessesErrorRow(logName, forbiddenWarningCount, discouragedWarningCount, infoCount, accessesLog);
	}

	private void parseCompileLogs() throws IOException {
		String compileLogsDirectory = compileLogsDirectoryName;
		if (compileLogsDirectory == null || compileLogsDirectory.isBlank()) {
			log(EOL + "INFO: Skip generating the compile logs summary page.");
			return;
		}
		File sourceDirectory = new File(compileLogsDirectory);
		File mainDir = new File(dropDirectoryName);
		File compilerSummaryFile = new File(mainDir, compilerSummaryFilename);
		// we do not recompute compiler summary each time, since it is
		// fairly time consuming -- and no reason it would not be "complete",
		// if it exists.
		if (compilerSummaryFile.exists() && !regenerate) {
			log(EOL + "INFO: Compile logs summary page, " + compilerSummaryFilename
					+ ", was found to exist already and not regenerated.");
		} else {
			if (compilerSummaryFile.exists()) {
				log(EOL + "INFO: Compile logs summary page, " + compilerSummaryFilename
						+ ", was found to exist already and is being regenerated.");
			}
			log("DEBUG: BEGIN: Parsing compile logs and generating summary table.");
			final StringBuilder compilerString = new StringBuilder();
			final StringBuilder accessesString = new StringBuilder();
			processCompileLogsDirectory(compileLogsDirectory, compilerString, accessesString);
			if (compilerString.length() == 0) {
				compilerString.append(
						"<tr><td class='namecell'>None</td><td class='cell'>&nbsp;</td><td class='cell'>&nbsp;</td></tr>"
								+ EOL);
			}
			if (accessesString.length() == 0) {
				accessesString.append(
						"<tr><td class='namecell'>None</td><td class='cell'>&nbsp;</td><td class='cell'>&nbsp;</td></tr>"
								+ EOL);
			}
			String prefix = """
					<h3 id="PluginsErrors">Plugins containing compile errors or warnings</h3>

					<p>The table below shows the plugins in which errors or warnings were encountered. Click on the jar file link to view its
					detailed report.</p>

					<table>
					  <tr>
					    <th class='cell'>Compile Logs (Jar Files)</th>
					    <th class='cell'>Errors</th>
					<th class='cell'>Warnings</th>
					  </tr>
					""";
			String suffix = """
							  </table>

					<h3 id="AcessErrors">Plugins containing access errors or warnings</h3>
					<table>
					 <tr>
					   <th class='cell'>Compile Logs (Jar Files)</th>
					   <th class='cell'>Forbidden Access</th>
					   <th class='cell'>Discouraged Access</th>
					   <th class='cell'>Info Warnings</th>
					</tr>
					""";
			StringBuilder compileLogResults = new StringBuilder(prefix).append(compilerString).append(suffix);
			compileLogResults.append(accessesString.toString());
			compileLogResults.append("</table>").append(EOL);
			// write the include file. The name of this file must match what is
			// in testResults.template.php
			writePhpIncludeCompilerResultsFile(sourceDirectory, compileLogResults.toString());
			log("DEBUG: End: Parsing compile logs and generating summary table.");
		}
	}

	private void parseJUnitTestsXml() throws IOException {
		log("DEBUG: Begin: Parsing XML JUnit results files");
		List<String> foundConfigs = new ArrayList<>();
		final File xmlResultsDirectory = new File(xmlDirectoryName);
		ResultsTable resultsTable = new ResultsTable(getTestsConfig());
		if (xmlResultsDirectory.exists()) {
			// reinitialize each time.
			// We currently "re do" all of tests, but can improve in the future
			// where the "found configs" are remembered, but then have to keep
			// track of original order (not "found" order which has to with when
			// tests completed).
			foundConfigs.clear();

			List<File> allFileNames = new ArrayList<>();

			for (String expectedConfig : getTestsConfig()) {

				FilenameFilter configfilter = new ExpectedConfigFiler("_" + expectedConfig + XML_EXTENSION);
				// we end with "full" list of files, sorted by configfilter, and
				// then alphabetical.
				File[] xmlFileNamesForConfig = xmlResultsDirectory.listFiles(configfilter);

				if (xmlFileNamesForConfig.length > 0) {
					// log("DEBUG: For " + expectedConfig + " found " +
					// xmlFileNamesForConfig.length + " XML results files");
					foundConfigs.add(expectedConfig);
					// sort by name, for each 'config' found.
					Arrays.sort(xmlFileNamesForConfig);
					Collections.addAll(allFileNames, xmlFileNamesForConfig);
				}
			}
			File[] xmlFileNames = new File[allFileNames.size()];
			allFileNames.toArray(xmlFileNames);
			// files MUST be alphabetical, for now?
			Arrays.sort(xmlFileNames);
			String sourceDirectoryCanonicalPath = dropDirectoryName;
			for (File junitResultsFile : xmlFileNames) {
				checkIfMissingFromTestManifestFile(junitResultsFile, foundConfigs);
				String fullName = junitResultsFile.getPath();
				int errorCount = countErrors(fullName);
				resultsTable.putCell(computeCoreName(junitResultsFile), computeConfig(junitResultsFile), errorCount,
						junitResultsFile);
				if (errorCount != 0) {
					trackDataForMail(sourceDirectoryCanonicalPath, junitResultsFile, fullName);
				}
			}
		} else {
			// error? Or, just too early?
			log(EOL + "WARNING: sourceDirectory did not exist at \n\t" + xmlResultsDirectory);
			log("     either incorrect call to 'generate index' or called too early (tests not done yet)?");
		}
		log("DEBUG: End: Parsing XML JUnit results files");
		// above is all "compute data". Now it is time to "display" it.
		if (foundConfigs.size() > 0) {
			log("DEBUG: Begin: Generating test results index tables in " + testResultsHtmlFileName);
			writeHTMLResultsTable(foundConfigs, resultsTable);
			log("DEBUG: End: Generating test results index tables");
		} else {
			log(EOL + "WARNING: Test results not found in " + xmlResultsDirectory.getAbsolutePath());
		}

	}

	private void writeHTMLResultsTable(List<String> foundConfigs, ResultsTable resultsTable) throws IOException {
		// These first files reflect what we expected, and what we found.
		String found_config_type = "found";
		writePhpConfigFile(found_config_type, foundConfigs, foundConfigFilename);
		// write the table to main output directory in testResultsTables.html,
		// which in turn is included by the testResults.php file.

		StringBuilder htmlString = new StringBuilder();
		// first we right a bit of "static" part. That comes before the table.
		htmlString.append(EOL).append("<h3 id=\"UnitTest\">Unit Test Results</h3>").append(EOL);

		if (buildType.equals("Y")) {
			htmlString.append(
					"<p>The unit tests are run on the <a href=\"https://ci.eclipse.org/releng/job/YBuilds/\">releng ci instance</a>.</p>");
		} else {
			htmlString.append(
					"<p>The unit tests are run on the <a href=\"https://ci.eclipse.org/releng/job/AutomatedTests/\">releng ci instance</a>.</p>");
		}
		String tableDescription = """
				<p>The table shows the unit test results for this build on the platforms
				tested. You may access the test results page specific to each
				component on a specific platform by clicking the cell link.
				Normally, the number of errors is indicated in the cell.</p>
				<p>A negative number or \"DNF\" means the test \"Did Not Finish\" for unknown reasons
				and hence no results page is available. In that case,
				more information can sometimes be found in
				the <a href=\"logs.php#console\">console logs</a>.</p>
				<?php
				if (file_exists(\"testNotes.html\")) {
				  $my_file = file_get_contents(\"testNotes.html\");
				  echo $my_file;
				}
				?>
				""";
		htmlString.append(EOL).append(tableDescription);

		htmlString.append(startTableOfUnitResults());
		for (String row : resultsTable) {
			htmlString.append(formatJUnitRow(row, resultsTable, foundConfigs));
		}
		// Once we are done with the Unit tests rows, we must add end table
		// tag, since the following methods may or may not add a table of
		// their own.
		htmlString.append(EOL).append("</table>").append(EOL);
		// check for missing test logs
		// TODO put styling on these tables
		htmlString.append(verifyAllTestsRan(xmlDirectoryName, foundConfigs));
		htmlString.append(listMissingManifestFiles());
		writeTestResultsFile(htmlString.toString());
	}

	private String startTableOfUnitResults() throws IOException {
		StringBuilder result = new StringBuilder();
		int ncolumns = getTestsConfig().size();
		result.append("<table>").append(EOL);
		// table header
		result.append("<tr>").append(EOL);
		result.append("<th class='cell' ").append(" rowspan='2' > org.eclipse <br /> Test Bundles </th>").append(EOL);
		result.append("<th class='cell' colspan='").append(ncolumns)
		.append("'> Test Configurations (Hudson Job/os.ws.arch/VM) </th>").append(EOL);
		result.append("</tr>\n");

		result.append("<tr>").append(EOL);

		for (String column : getTestsConfig()) {
			result.append("<th class='cell'>").append(computeDisplayConfig(column)).append("</th>\n");
		}
		result.append("</tr>").append(EOL);
		// end table header
		return result.toString();
	}

	/*
	 * This function "breaks" the full config string at meaningful underscores, for
	 * improved display in tables and similar. Remember, some config values can have
	 * more than two underscores, such as ep46I-unit-lin64_linux.gtk.x86_64_8.0,
	 * which should be split as ep46I-unit-lin64 lin64_linux.gtk.x86_64 8.0
	 */
	private String computeDisplayConfig(String config) {
		int lastUnderscore = config.lastIndexOf("_");
		int firstUnderscore = config.indexOf('_', config.indexOf("x86_64") + 6);
		// echo "<br/>DEBUG: config: config firstUnderscore: firstUnderscore
		// lastUnderscore: lastUnderscore lastMinusFirst: platformLength"
		String jobname = config.substring(0, firstUnderscore);
		String platformconfig = config.substring(firstUnderscore + 1, lastUnderscore);
		String vmused = config.substring(lastUnderscore + 1);
		// echo "DEBUG: jobname: ".jobname."<br/>";
		// echo "DEBUG: platformconfig: ".platformconfig."<br/>";
		// echo "DEBUG: vmused: ".vmused."<br/>";
		return jobname + "<br/>" + platformconfig + "<br/>" + vmused;

	}

	/*
	 * As far as I know, this "work" was done to track data send out in an email.
	 */
	private void trackDataForMail(String sourceDirectoryCanonicalPath, File junitResultsFile, final String fullName) {
		anErrorTracker.registerError(fullName.substring(xmlDirectoryName.length() + 1));
	}

	/*
	 * This is the "reverse" of checking for "missing test results". It is simple
	 * sanity check to see if all "known" test results are listed in in the
	 * testManifest.xml file. We only do this check if we also are checking for
	 * missing logs which depends on an accurate testManifest.xml file.
	 */
	private void checkIfMissingFromTestManifestFile(File junitResultsFile, List<String> foundConfigs) {
		if (doMissingList) {
			if (!verifyLogInManifest(junitResultsFile.getName(), foundConfigs)) {
				String corename = computeCoreName(junitResultsFile);
				missingManifestFiles.add(corename);
			}
		}
	}

	private String computeCoreName(File junitResultsFile) {
		String fname = junitResultsFile.getName();
		// corename is all that needs to be listed in testManifest.xml
		String corename = null;
		int firstUnderscorepos = fname.indexOf('_');
		if (firstUnderscorepos == -1) {
			// should not occur, but if it does, we will take whole name
			corename = fname;
		} else {
			corename = fname.substring(0, firstUnderscorepos);
		}
		return corename;
	}

	private String computeConfig(File junitResultsFile) {
		String fname = junitResultsFile.getName();
		String configName = null;
		int firstUnderscorepos = fname.indexOf('_');
		if (firstUnderscorepos == -1) {
			// should not occur, but if it does, we will set to null
			// and let calling program decide what to do.
			configName = null;
		} else {
			int lastPos = fname.lastIndexOf(XML_EXTENSION);
			if (lastPos == -1) {
				configName = null;
			} else {
				configName = fname.substring(firstUnderscorepos + 1, lastPos);
			}
		}
		return configName;
	}

	private void writePhpConfigFile(String config_type, List<String> configs, String phpfilename) throws IOException {
		File mainDir = new File(dropDirectoryName);
		File testConfigsFile = new File(mainDir, phpfilename);
		try (Writer testconfigsPHP = new FileWriter(testConfigsFile)) {
			testconfigsPHP.write("<?php" + EOL);
			testconfigsPHP.write("//This file created by 'generateIndex' ant task, while parsing test results" + EOL);
			testconfigsPHP.write("// It is based on " + config_type + " testConfigs" + EOL);
			String phpArrayVariableName = "$" + config_type + "TestConfigs";
			testconfigsPHP.write(phpArrayVariableName + " = array();" + EOL);
			for (String fConfig : configs) {
				testconfigsPHP.write(phpArrayVariableName + "[]=\"" + fConfig + "\";" + EOL);
			}
		}
	}

	private void writePhpIncludeCompilerResultsFile(final File sourceDirectory, String compilerSummary)
			throws IOException {
		File mainDir = new File(dropDirectoryName);
		File compilerSummaryFile = new File(mainDir, compilerSummaryFilename);
		try (Writer compilerSummaryPHP = new FileWriter(compilerSummaryFile)) {
			compilerSummaryPHP.write("<!--" + EOL);
			compilerSummaryPHP.write(
					"  This file created by 'generateIndex' ant task, while parsing build and tests results" + EOL);
			compilerSummaryPHP.write("-->" + EOL);
			compilerSummaryPHP.write(compilerSummary);
		}
	}

	private void processCompileLogsDirectory(final String directoryName, final StringBuilder compilerLog,
			final StringBuilder accessesLog) {
		final File sourceDirectory = new File(directoryName);
		if (sourceDirectory.isFile()) {
			if (sourceDirectory.getName().endsWith(".log")) {
				readCompileLog(sourceDirectory.getAbsolutePath(), compilerLog, accessesLog);
			}
			if (sourceDirectory.getName().endsWith(XML_EXTENSION)) {
				parseCompileLog(sourceDirectory.getAbsolutePath(), compilerLog, accessesLog);
			}
		}
		if (sourceDirectory.isDirectory()) {
			final File[] logFiles = sourceDirectory.listFiles();
			Arrays.sort(logFiles);
			for (File logFile : logFiles) {
				processCompileLogsDirectory(logFile.getAbsolutePath(), compilerLog, accessesLog);
			}
		}
	}

	private String processDropRow(final PlatformStatus aPlatform) {
		if ("equinox".equalsIgnoreCase(aPlatform.format())) {
			return processEquinoxDropRow(aPlatform);
		} else {
			return processEclipseDropRow(aPlatform);
		}

	}

	private String processEclipseDropRow(PlatformStatus aPlatform) {
		StringBuilder result = new StringBuilder("<tr>\n<td>").append(aPlatform.name()).append("</td>\n");
		// generate file link, size and checksums in the php template
		result.append("<?php genLinks(\"").append(aPlatform.fileName()).append("\"); ?>\n");
		result.append("</tr>\n");
		return result.toString();
	}

	private String processDropRows(final PlatformStatus[] platforms) {
		StringBuilder result = new StringBuilder();
		for (PlatformStatus platform : platforms) {
			result.append(processDropRow(platform));
		}
		return result.toString();
	}

	/*
	 * Generate and return the HTML mark-up for a single row for an Equinox JAR on
	 * the downloads page.
	 */
	private String processEquinoxDropRow(final PlatformStatus aPlatform) {
		StringBuilder result = new StringBuilder("<tr>");
		result.append("<td>");
		final String filename = aPlatform.fileName();
		// if there are images, put them in the same table column as the name of
		// the file
		final List<String> images = aPlatform.images();
		if ((images != null) && !images.isEmpty()) {
			for (String image : images) {
				result.append("<img src=\"").append(image).append("\"/>&nbsp;");
			}
		}
		result.append("<a href=\"download.php?dropFile=").append(filename).append("\">").append(filename)
		.append("</a></td>\n");
		result.append("{$generateDropSize(\"").append(filename).append("\")}\n");
		result.append("{$generateChecksumLinks(\"").append(filename).append("\", $buildlabel)}\n");
		result.append("</tr>\n");
		return result.toString();
	}

	private void readCompileLog(final String log, final StringBuilder compilerLog, final StringBuilder accessesLog) {
		final String fileContents = readFile(log);

		final int errorCount = countCompileErrors(fileContents);
		final int warningCount = countCompileWarnings(fileContents);
		final int forbiddenWarningCount = countForbiddenWarnings(fileContents);
		final int discouragedWarningCount = countDiscouragedWarnings(fileContents);
		final int infoCount = countInfos(fileContents);
		if (errorCount != 0) {
			// use wildcard in place of version number on directory names
			String logName = log.substring(compileLogsDirectoryName.length() + 1);
			final StringBuilder stringBuilder = new StringBuilder(logName);
			stringBuilder.replace(logName.indexOf("_") + 1, logName.indexOf(File.separator, logName.indexOf("_") + 1),
					"*");
			logName = new String(stringBuilder);

			anErrorTracker.registerError(logName);
		}
		formatCompileErrorRow(log, errorCount, warningCount, compilerLog);
		formatAccessesErrorRow(log, forbiddenWarningCount, discouragedWarningCount, infoCount, accessesLog);
	}

	private String readFile(final String fileName) {
		try {
			return Files.readString(Path.of(fileName), Charset.defaultCharset());
		} catch (final IOException e) {
			logException(e);
			return "";
		}
	}

	private String replace(final String source, final String original, final String replacement) {

		final int replaceIndex = source.indexOf(original);
		if (replaceIndex > -1) {
			StringBuilder resultString = new StringBuilder().append(source.substring(0, replaceIndex));
			resultString.append(replacement);
			resultString.append(source.substring(replaceIndex + original.length()));
			return resultString.toString();
		} else {
			log(EOL + "WARNING: Could not find token: " + original);
			return source;
		}

	}

	private String verifyAllTestsRan(final String directory, List<String> foundConfigs) {
		StringBuilder replaceString = new StringBuilder();
		List<String> missingFiles = new ArrayList<>();
		if (doMissingList) {
			for (String testLogName : anErrorTracker.getTestLogs(foundConfigs)) {

				if (new File(directory + File.separator + testLogName).exists()) {
					// log("DEBUG: found log existed: " + testLogName);
					continue;
				}
				// log("DEBUG: found log DID NOT exist: " + testLogName);
				anErrorTracker.registerError(testLogName);
				// replaceString = replaceString + tmp;
				missingFiles.add(testLogName);
			}
		} else {
			// Note: we intentionally do not deal with missing file for perf.
			// tests yet.
			// (though, probably could, once fixed with "expected configs").
			replaceString
			.append("""
					<tbody>
					<tr><td colspan=\"0\"><p><span class=\"footnote\">NOTE: </span>
					Remember that for performance unit test tables, there are never any \"missing files\" listed, if there are any.
					This is expected to be a temporary solution, until an exact fix can be implemented. For more details, see
					<a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id=451890\">bug 451890</a>.</p>
					</td></tr>
					</tbody>
					""");
		}
		// TODO: we need lots more of separating "data" from "formating"
		if (doMissingList && missingFiles.size() > 0) {
			String ordinalWord = "File";
			if (missingFiles.size() > 1) {
				ordinalWord = "Files";
			}

			replaceString.append("</table>").append(EOL).append("<table>").append("<tr> <th class='cell'>Missing ")
			.append(ordinalWord).append("</th></tr>");
			for (String testLogName : missingFiles) {
				replaceString.append(EOL).append("<tr><td class='namecell'>").append(testLogName).append("</td></tr>");
			}
			replaceString.append(EOL).append("</table>");
		}
		return replaceString.toString();
	}

	private void writeDropIndexFile() {
		String dropHtmlFile = dropHtmlFileName;
		if (dropHtmlFile == null || dropHtmlFile.isBlank()) {
			log(EOL + "INFO: Skip generating the drop index file.");
			return;
		}
		List<String> dropTokens = List.of(dropTokenList.split(","));
		final String outputFileName = dropDirectoryName + File.separator + dropHtmlFile;
		File outputIndexFile = new File(outputFileName);
		// we assume if "eclipse" has been done, then "equinox" has been as
		// well.
		if (outputIndexFile.exists() && !regenerate) {
			log(EOL + "INFO: The drop index file, " + dropHtmlFile
					+ ", was found to exist already and not regenerated.");
		} else {
			String dropTemplateString = readFile(dropTemplateFileName);
			if (outputIndexFile.exists()) {
				log(EOL + "INFO: The drop index file, " + dropHtmlFile
						+ ", was found to exist already and is being regenerated.");
			}
			log("DEBUG: Begin: Generating drop index page");
			final String[] types = anErrorTracker.getTypes();
			for (int i = 0; i < types.length; i++) {
				final PlatformStatus[] platforms = anErrorTracker.getPlatforms(types[i]);
				final String replaceString = processDropRows(platforms);
				dropTemplateString = replace(dropTemplateString, dropTokens.get(i).toString(), replaceString);
			}
			writeFile(outputIndexFile, dropTemplateString);
			log("DEBUG: End: Generating drop index page");
		}
	}

	private void writeFile(File outputFile, final String contents) {
		try {
			Files.writeString(outputFile.toPath(), contents);
		} catch (final FileNotFoundException e) {
			log(EOL + "ERROR: File not found exception while writing: " + outputFile.getPath());
		} catch (final IOException e) {
			log(EOL + "ERROR: IOException writing: " + outputFile.getPath());
		}
	}

	/*
	 * This method writes the computed HTML to the file specified by caller in
	 * testResultsHtmlFileName. There must be an appropriate file on Download site
	 * that "includes" the file.
	 */
	private void writeTestResultsFile(String contents) {
		final String outputFileName = dropDirectoryName + File.separator + testResultsHtmlFileName;
		File outputFile = new File(outputFileName);
		writeFile(outputFile, contents);

	}

	private List<String> getTestsConfig() throws IOException {
		if (expectedConfigs == null) {
			expectedConfigs = new ArrayList<>();
			String expectedConfigParam = testsConfigExpected;
			if (expectedConfigParam != null) {
				StringTokenizer tokenizer = new StringTokenizer(expectedConfigParam, " ,\t");
				while (tokenizer.hasMoreTokens()) {
					expectedConfigs.add(tokenizer.nextToken());
				}
			} else {
				throw new IllegalStateException("test configurations were not found. One or more must be set.");
			}
			if (DEBUG) {
				// log("DEBUG: testsConfig array ");
				for (String expected : expectedConfigs) {
					log("\tDEBUG: expectedTestConfig: " + expected);
				}
			}
			// write expected test config file here. This file is later used by
			// the PHP file so the name passed in must match what was put in PHP
			// file.
			writePhpConfigFile(expected_config_type, expectedConfigs, expectedConfigFilename);
		}
		return expectedConfigs;
	}

	/*
	 * This is the reverse of checking that all expected logs were found. If logs
	 * were found that are NOT in the test manifest, we write the list below missing
	 * files, so that they can be added to testManifest.xml. This allows them to be
	 * detected as missing, in future. We only do this check if "doMissingList" is
	 * true.
	 */
	private boolean verifyLogInManifest(String filename, List<String> foundConfigs) {
		boolean result = false;
		if (doMissingList) {
			for (String testLogName : anErrorTracker.getTestLogs(foundConfigs)) {
				if (filename.equals(testLogName)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	private String listMissingManifestFiles() throws IOException {
		StringBuilder results = new StringBuilder();
		if (doMissingList) {
			StringBuilder xmlFragment = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ").append(EOL)
					.append("<topLevel>").append(EOL);

			if (doMissingList && missingManifestFiles.size() > 0) {
				String ordinalWord = "File";
				if (missingManifestFiles.size() > 1) {
					ordinalWord = "Files";
				}

				results.append(EOL).append("<table>").append(
						"<tr> <th class='cell'>Releng: <a href=\"addToTestManifest.xml\">Missing testManifest.xml ")
				.append(ordinalWord).append("</a></th></tr>");
				for (String testLogName : missingManifestFiles) {
					results.append(EOL).append("<tr><td class='namecell'>").append(testLogName).append("</td></tr>");
					xmlFragment.append("<logFile ").append(EOL).append("  name=\"").append(testLogName).append("\"")
					.append(EOL).append("  type=\"test\" />").append(EOL);
				}
				results.append(EOL).append("</table>");
				xmlFragment.append("</topLevel>");
				try (FileWriter xmlOutput = new FileWriter(dropDirectoryName + "/addToTestManifest.xml")) {
					xmlOutput.write(xmlFragment.toString());
				}
			}
		}
		return results.toString();
	}

	// Specific to the RelEng test results page
	private String formatJUnitRow(String corename, ResultsTable resultsTable, List<String> foundConfigs)
			throws IOException {

		StringBuilder results = new StringBuilder();
		int orgEclipseLength = "org.eclipse.".length();
		// indexOf('_') assumes never part of file name?
		final String displayName = corename.substring(orgEclipseLength);

		results.append(EOL).append("<tr><td class=\"namecell\">").append(displayName).append("</td>");

		for (String config : getTestsConfig()) {
			TestResultsGenerator.ResultsTable.Cell cell = resultsTable.getCell(corename, config);
			if (cell == null && foundConfigs.contains(config)) {
				cell = new TestResultsGenerator.ResultsTable.Cell(-1, null);
			}
			results.append(printCell(cell));
		}
		results.append("</tr>").append(EOL);
		return results.toString();
	}

	private String printCell(TestResultsGenerator.ResultsTable.Cell cell) {
		String result = null;
		String displayName = null;
		if (cell == null) {
			displayName = "<td class=\"cell\">&nbsp;</td>";
			result = displayName;
		} else {
			int cellErrorCount = cell.errorCount();
			File cellResultsFile = cell.resultsFile();
			String filename = null;
			int beginFilename = 0;
			String rawfilename = null;
			if (cellResultsFile != null) {
				filename = cellResultsFile.getName();
				beginFilename = filename.lastIndexOf(File.separatorChar);
				rawfilename = filename.substring(beginFilename + 1, filename.length() - XML_EXTENSION.length());
			}
			String startCell = null;
			if (cellErrorCount == -999) {
				displayName = "<td class=\"cell\">&nbsp;</td>";
				result = displayName;
			} else if (cellErrorCount == 0) {
				startCell = "<td class=\"cell\">";
				displayName = "(0)";
				result = addLinks(startCell, displayName, rawfilename);
			} else if (cellErrorCount < 0) {
				startCell = "<td class=\"errorcell\">";
				displayName = "(" + Integer.toString(cellErrorCount) + ") DNF ";
				result = startCell + displayName + "</td>";
			} else if (cellErrorCount > 0) {
				startCell = "<td class=\"errorcell\">";
				displayName = "(" + Integer.toString(cellErrorCount) + ")";
				result = addLinks(startCell, displayName, rawfilename);
			} else {
				// should never occur
				displayName = "<td class='errorcell'>?" + Integer.toString(cellErrorCount) + "?</td>";
				result = displayName;
			}
		}
		return result;

	}

	private String addLinks(String startCell, String displayName, String rawfilename) {
		return startCell + "<a style=\"color:inherit\" title=\"Detailed Unit Test Results Table\" href=\""
				+ hrefTestResultsTargetPath + "/html/" + rawfilename + HTML_EXTENSION + "\">" + displayName + "</a>"
				+ "<a style=\"color:#555555\" title=\"XML Test Result (e.g. for importing into the Eclipse JUnit view)\" href=\""
				+ hrefTestResultsTargetPath + "/xml/" + rawfilename + XML_EXTENSION + "\">&nbsp;(XML)</a></td>";
	}

	// --- utility methods ---

	private static void log(String msg) {
		System.out.println(msg);
	}

	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE = createDocumentBuilderFactoryIgnoringDOCTYPE();
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE = createDocumentBuilderFactoryWithErrorOnDOCTYPE();

	private static synchronized DocumentBuilderFactory createDocumentBuilderFactoryIgnoringDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			// completely disable external entities declarations:
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	private static synchronized DocumentBuilderFactory createDocumentBuilderFactoryWithErrorOnDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// completely disable DOCTYPE declaration:
		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}

	static synchronized DocumentBuilder createDocumentBuilderIgnoringDOCTYPE() throws ParserConfigurationException {
		DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY_IGNORING_DOCTYPE.newDocumentBuilder();
		builder.setEntityResolver((__, ___) -> new InputSource(new ByteArrayInputStream(new byte[0])));
		return builder;
	}

	static synchronized DocumentBuilder createDocumentBuilderWithErrorOnDOCTYPE() throws ParserConfigurationException {
		return DOCUMENT_BUILDER_FACTORY_ERROR_ON_DOCTYPE.newDocumentBuilder();
	}

}
