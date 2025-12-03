
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
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @version 1.0
 */
public class ErrorTracker {

	private final Set<String> testLogsSet = Collections.checkedSortedSet(new TreeSet<>(), String.class);
	// Platforms keyed on
	private final Map<String, PlatformStatus> platforms = new HashMap<>();
	private final Map<String, List<PlatformStatus>> logFiles = new HashMap<>();
	private final Map<String, List<String>> typesMap = new HashMap<>();

	private final List<String> typesList = new ArrayList<>();

	private String convertPathDelimiters(final String path) {
		return new File(path).getPath();
	}

	public PlatformStatus[] getPlatforms(final String type) {
		final List<String> platformIDs = typesMap.get(type);
		final PlatformStatus[] result = new PlatformStatus[platformIDs.size()];
		for (int i = 0; i < platformIDs.size(); i++) {
			result[i] = platforms.get(platformIDs.get(i));
		}
		return result;
	}

	/**
	 * Returns the testLogs.
	 *
	 * @return Vector
	 */
	public List<String> getTestLogs(List<String> foundConfigs) {
		// List of test logs expected at end of build
		// We depend on both test logs and configs being sorted
		ArrayList<String> testLogs = new ArrayList<>();
		for (String initialLogName : testLogsSet) {
			for (String config : foundConfigs) {
				testLogs.add(initialLogName + "_" + config + ".xml");
			}
		}
		return testLogs;
	}

	// Answer a string array of the zip type names in the order they appear in
	// the .xml file.
	public String[] getTypes() {
		return typesList.toArray(new String[typesList.size()]);
	}

	// Answer an array of PlatformStatus objects for a given type.

	@SuppressWarnings("restriction")
	public void loadFile(final String fileName) {
		try {
			DocumentBuilder parser = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createDocumentBuilderWithErrorOnDOCTYPE();
			final Document document = parser.parse(fileName);
			final NodeList elements = document.getElementsByTagName("platform");
			final int elementCount = elements.getLength();
			for (int i = 0; i < elementCount; i++) {
				final PlatformStatus aPlatform = PlatformStatus.create((Element) elements.item(i));
				// System.out.println("ID: " + aPlatform.getId());
				platforms.put(aPlatform.id(), aPlatform);

				final Node zipType = elements.item(i).getParentNode();
				final String zipTypeName = zipType.getAttributes().getNamedItem("name").getNodeValue();

				List<String> aVector = typesMap.get(zipTypeName);
				if (aVector == null) {
					typesList.add(zipTypeName);
					aVector = new ArrayList<>();
					typesMap.put(zipTypeName, aVector);
				}
				aVector.add(aPlatform.id());

			}

			final NodeList effectedFiles = document.getElementsByTagName("effectedFile");
			final int effectedFilesCount = effectedFiles.getLength();
			for (int i = 0; i < effectedFilesCount; i++) {
				final Node anEffectedFile = effectedFiles.item(i);
				final Node logFile = anEffectedFile.getParentNode();
				String logFileName = logFile.getAttributes().getNamedItem("name").getNodeValue();
				logFileName = convertPathDelimiters(logFileName);
				final String effectedFileID = anEffectedFile.getAttributes().getNamedItem("id").getNodeValue();
				// System.out.println(logFileName);
				List<PlatformStatus> aVector = logFiles.get(logFileName);
				if (aVector == null) {
					aVector = new ArrayList<>();
					logFiles.put(logFileName, aVector);

				}
				final PlatformStatus ps = platforms.get(effectedFileID);
				if (ps != null) {
					aVector.add(ps);
				}
			}

			// store a list of the test logs expected after testing
			final NodeList testLogList = document.getElementsByTagName("logFile");
			final int testLogCount = testLogList.getLength();
			for (int i = 0; i < testLogCount; i++) {

				final Node testLog = testLogList.item(i);
				final String testLogName = testLog.getAttributes().getNamedItem("name").getNodeValue();
				final Node typeNode = testLog.getAttributes().getNamedItem("type");
				// String type = "test";
				// if (typeNode != null) {
				// type = typeNode.getNodeValue();
				// }
				// if (testLogName.endsWith(".xml") && type.equals("test")) {
				// above is how it used to be checked, prior to 4/4/2016, but
				// test logs are only log file in testManifest.xml without a "type" attribute
				// -- I test for either/or, so that new versions of testManifest.xml
				// can more correctly use "test" attribute, if desired.
				if (typeNode == null || typeNode.getNodeValue().equals("test")) {
					int firstUnderscore = testLogName.indexOf('_');
					String initialTestName = null;
					if (firstUnderscore == -1) {
						// no underscore found. Assume testManifest xml has been updated
						// to mention minimal name.
						initialTestName = testLogName;
					} else {
						initialTestName = testLogName.substring(0, firstUnderscore);
					}
					testLogsSet.add(initialTestName);
					// System.out.println("Debug: initialTestName: " + initialTestName);
				}

			}

		} catch (final IOException e) {
			System.out.println("IOException: " + fileName);
			// e.printStackTrace();

		} catch (final SAXException e) {
			System.out.println("SAXException: " + fileName);
			e.printStackTrace();

		} catch (final ParserConfigurationException e1) {
			e1.printStackTrace();
		}
	}

	public void registerError(final String fileName) {
		// System.out.println("Found an error in: " + fileName);
		if (logFiles.containsKey(fileName)) {
			List<PlatformStatus> aVector = logFiles.get(fileName);
			for (PlatformStatus element : aVector) {
				element.registerError();
			}
		} else {

			// If a log file is not specified explicitly it effects
			// all "platforms" except JDT

			for (PlatformStatus aValue : platforms.values()) {
				if (!aValue.id().equals("JA") && !aValue.id().equals("EW") && !aValue.id().equals("EA")) {
					aValue.registerError();
				}
			}
		}
	}

}
