
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
 *     Hannes Wellmann - split TestResultsGenerator and CompilerSummaryGenerator
 *******************************************************************************/

import static utilities.XmlProcessorFactoryRelEng.elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utilities.JSON;
import utilities.OS;
import utilities.XmlProcessorFactoryRelEng;

/**
 * @author Dean Roberts (circa 2000!) and David Williams (circa 2016)
 */
public class TestResultsGenerator {

	//
	// NOTE: KEEP THIS AT Java-21 until all tests are executed with Java-25!
	//

	private static final String XML_EXTENSION = ".xml";
	private static final String elementName = "testsuite";
	private List<String> expectedTestConfigs;
	private Set<String> expectedTestLogs;

	// Parameters
	// Location of the xml files
	private Path xmlDirectory;
	// Location and name of test manifest file
	private Path testManifestFile;

	private final Set<String> missingManifestFiles = new TreeSet<>();

	/*
	 * returns number of errors plus number of failures. returns a negative number
	 * if the file is missing or something is wrong with the file (such as is
	 * incomplete).
	 */
	private void collectErrors(Path fileName, JSON.Object test)
			throws ParserConfigurationException, SAXException, IOException {
		int errorCount = -99;
		// File should exists, since we are "driving" this based on file list
		// ... but, just in case.
		if (!Files.exists(fileName)) {
			errorCount = -1;
		} else {
			if (Files.size(fileName) == 0) {
				errorCount = -2;
			} else {
				try {
					DocumentBuilder builder = XmlProcessorFactoryRelEng.createDocumentBuilderWithErrorOnDOCTYPE();
					Document document = builder.parse(fileName.toFile());
					final NodeList elements = document.getElementsByTagName(elementName);

					final int elementCount = elements.getLength();
					if (elementCount == 0) {
						errorCount = -3;
					} else {
						// There can be multiple "testSuites" per file so we
						// need to
						// loop through each to count all errors and failures.
						errorCount = 0;
						JSON.Array suites = JSON.Array.create();
						for (Element element : elements(elements)) {
							String errorsValue = element.getAttribute("errors");
							String failuresValue = element.getAttribute("failures");
							errorCount += errorsValue.isEmpty() ? 0 : Integer.parseInt(errorsValue);
							errorCount += failuresValue.isEmpty() ? 0 : Integer.parseInt(failuresValue);
							String testSuiteFQN = element.getAttribute("package") + "." + element.getAttribute("name");
							suites.add(JSON.String.create(testSuiteFQN));
						}
						test.add("suites", suites);
					}
				} catch (final IOException e) {
					println("ERROR: IOException: " + fileName);
					e.printStackTrace();
					errorCount = -4;
				} catch (final SAXException e) {
					println("ERROR: SAXException: " + fileName);
					e.printStackTrace();
					errorCount = -5;
				} catch (final ParserConfigurationException e) {
					e.printStackTrace();
					errorCount = -6;
				}
			}
		}
		test.add("errors", JSON.Integer.create(errorCount));
	}

	public static void main(String[] args) throws Exception {
		TestResultsGenerator generator = new TestResultsGenerator();
		generator.testManifestFile = Path.of(OS.readProperty("testManifestFile")).toRealPath();
		generator.xmlDirectory = Path.of(OS.readProperty("xmlDirectory")).toRealPath();
		generator.expectedTestConfigs = List.of(OS.readProperty("testsConfigExpected").split(","));

		println("INFO: Processing test results in");
		println("\t" + generator.xmlDirectory);
		println("INFO: For configurations");
		generator.expectedTestConfigs.forEach(c -> println("\t- " + c));

		generator.parseJUnitTestsXml();
	}

	private void parseJUnitTestsXml() throws Exception {
		println("DEBUG: Begin: Parsing XML JUnit results files");
		List<String> foundConfigs = new ArrayList<>();
		List<Path> allFiles = new ArrayList<>();
		for (String expectedConfig : expectedTestConfigs) {
			expectedConfig = computeLongConfig(expectedConfig);

			int sizeBefore = allFiles.size();
			String expectedConfigEnding = "_" + expectedConfig + XML_EXTENSION;
			try (var list = Files.list(xmlDirectory)) {
				list.filter(f -> f.toString().endsWith(expectedConfigEnding)).forEach(allFiles::add);
			}
			if (sizeBefore < allFiles.size()) {
				foundConfigs.add(expectedConfig);
			} else {
				println("WARNING: No tests found for configuration: " + expectedConfig);
			}
		}
		expectedTestLogs = loadExpectedTests(foundConfigs);
		// files MUST be alphabetical, for now?
		allFiles.sort(null);
		Map<String, JSON.Object> summaries = new HashMap<>();
		for (Path junitResultsFile : allFiles) {
			checkIfMissingFromTestManifestFile(junitResultsFile);
			String filename = junitResultsFile.getFileName().toString();
			JSON.Object test = JSON.Object.create();
			collectErrors(junitResultsFile, test);
			String testPluginName = computeCoreName(filename);
			String configuration = computeConfig(filename);
			JSON.Object summary = summaries.computeIfAbsent(configuration, c -> JSON.Object.create());
			summary.add(testPluginName, test);
		}

		println("DEBUG: End: Parsing XML JUnit results files");
		// above is all "compute data". Now it is time to "display" it.
		if (foundConfigs.size() > 0) {
			println("DEBUG: Begin: Generating test results index");
			for (Entry<String, JSON.Object> entry : summaries.entrySet()) {
				String configName = entry.getKey();
				JSON.Object summary = entry.getValue();
				String shortConfig = computeShortConfigurationName(configName);
				Path file = xmlDirectory.resolveSibling(shortConfig + "-summary.json");
				println("DEBUG: Write Eclipse drop main data to: " + file);
				JSON.write(summary, file);
			}
			verifyAllTestsRan(xmlDirectory);
			listMissingManifestFiles();
			println("DEBUG: End: Generating test results index tables");
		} else {
			println("WARNING: Test results not found in " + xmlDirectory);
		}
		// TODO: It seems that the DNF/-1 was (just) set if a test result was not
		// available for one config, but not for another one (i.e. a missing cell).
	}

	private Set<String> loadExpectedTests(List<String> foundConfigs)
			throws ParserConfigurationException, SAXException, IOException {

		Set<String> testLogsSet = new TreeSet<>();

		DocumentBuilder parser = XmlProcessorFactoryRelEng.createDocumentBuilderWithErrorOnDOCTYPE();
		Document document = parser.parse(testManifestFile.toFile());

		// store a list of the test logs expected after testing
		NodeList testLogList = document.getElementsByTagName("logFile");
		int testLogCount = testLogList.getLength();
		for (int i = 0; i < testLogCount; i++) {
			Node testLog = testLogList.item(i);
			String testLogName = testLog.getAttributes().getNamedItem("name").getNodeValue();
			Node typeNode = testLog.getAttributes().getNamedItem("type");
			if (typeNode == null || typeNode.getNodeValue().equals("test")) {
				int underscore = testLogName.indexOf('_');
				String initialTestName = underscore == -1 ? testLogName : testLogName.substring(0, underscore);
				testLogsSet.add(initialTestName);
				// System.out.println("Debug: initialTestName: " + initialTestName);
			}
		}
		// List of test logs expected at end of build
		// We depend on both test logs and configs being sorted
		return testLogsSet.stream()
				.flatMap(initialLogName -> foundConfigs.stream().map(config -> initialLogName + "_" + config + ".xml"))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/*
	 * This is the reverse of checking that all expected logs were found. If logs
	 * were found that are NOT in the test manifest, we write the list below missing
	 * files, so that they can be added to testManifest.xml. This allows them to be
	 * detected as missing, in future.
	 */
	private void checkIfMissingFromTestManifestFile(Path junitResultsFile) {
		String filename = junitResultsFile.getFileName().toString();
		if (!expectedTestLogs.contains(filename)) {
			String corename = computeCoreName(filename);
			missingManifestFiles.add(corename);
		}
	}

	private static String computeCoreName(String fname) {
		// corename is all that needs to be listed in testManifest.xml
		int firstUnderscorepos = fname.indexOf('_');
		// should not occur, but if it does, we will take whole name
		return firstUnderscorepos == -1 ? fname : fname.substring(0, firstUnderscorepos);
	}

	private static String computeConfig(String fname) {
		int firstUnderscorepos = fname.indexOf('_');
		if (firstUnderscorepos == -1) {
			// should not occur, but if it does, we will set to null
			// and let calling program decide what to do.
			return null;
		} else {
			int lastPos = fname.lastIndexOf(XML_EXTENSION);
			return lastPos == -1 ? null : fname.substring(firstUnderscorepos + 1, lastPos);
		}
	}

	private static String computeShortConfigurationName(String config) {
		int javaIndex = config.indexOf("-java");
		return config.substring(0, config.indexOf('_', javaIndex));
	}

	private static String computeLongConfig(String config) {
		String[] elements = config.split("-");
		String os = elements[2];
		String arch = elements[3];
		int javaVersion = Integer.parseInt(elements[4].substring(4)); // number after -java
		String ws = switch (os) { // TODO: better derive this from something
		case "linux" -> "gtk";
		case "macosx" -> "cocoa";
		case "win32" -> "win32";
		default -> throw new IllegalArgumentException("Unsupported OS: " + os);
		};
		return config + "_" + os + "." + ws + "." + arch + "_" + javaVersion;
	}

	private void verifyAllTestsRan(Path directory) {
		List<String> missingFiles = new ArrayList<>();
		for (String testLogName : expectedTestLogs) {
			if (!Files.exists(directory.resolve(testLogName))) {
				missingFiles.add(testLogName);
			}
		}
		if (missingFiles.size() > 0) {
			println("WARNING: Results of test(s) listed in testManifest.xml are missing: " + missingFiles.size());
			for (String testLogName : missingFiles) {
				println("WARNING:\t- " + testLogName);
			}
		}
	}

	private void listMissingManifestFiles() throws IOException {
		if (missingManifestFiles.size() > 0) {
			// TODO: Make this visible on the download page somehow?
			Path filename = xmlDirectory.resolve("addToTestManifest.xml");
			println("WARNING: Found test(s) missing in testManifest.xml: " + missingManifestFiles.size());
			println("WARNING:\tFor details see: " + filename);
			StringBuilder xmlFragment = new StringBuilder("""
					<?xml version=\"1.0\" encoding=\"UTF-8\"?>
					<topLevel>
					""");
			for (String testLogName : missingManifestFiles) {
				println("WARNING:\t- " + testLogName);
				xmlFragment.append("<logFile\n");
				xmlFragment.append("  name=\"").append(testLogName).append("\"\n");
				xmlFragment.append("  type=\"test\" />\n");
			}
			xmlFragment.append("</topLevel>");
			Files.writeString(filename, xmlFragment);
		}
	}

	private static void println(String msg) {
		System.out.println(msg);
	}

}
