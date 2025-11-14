
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

import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import utilities.JSON;
import utilities.OS;
import utilities.XmlProcessorFactoryRelEng;

/** @author Dean Roberts (circa 2000!) and David Williams (circa 2016) */

final String HTML_EXTENSION = ".html";
final String XML_EXTENSION = ".xml";
final String COMPILER_SUMMARY_FILENAME = "compilerSummary.json";

Path dropDirectory;
Path compileLogsDirectory; // Location of compile logs base directory

void main() throws Exception {
	dropDirectory = Path.of(OS.readProperty("dropDirectory")).toRealPath();
	compileLogsDirectory = dropDirectory.resolve("compilelogs/plugins");

	IO.println("INFO: Processing compiler logs in");
	IO.println("\t" + compileLogsDirectory);

	parseCompileLogs();
}

String computeShortName(String name) {
	// further shortening (may need to "back out", so left as separate step)
	// we always expect the name to start with "org.eclipse." .. but,
	// just in case that changes, we'll check and handle if not.
	String commonnamespace = "org.eclipse.";
	int start = name.startsWith(commonnamespace) ? commonnamespace.length() : 0;
	// Similarly, we always expect the name to end with '_version',
	// but just in case not.
	int last = name.indexOf('_');
	return name.substring(start, last == -1 ? name.length() : last);
}

final Pattern XML_EXTENSION_PATTERN = Pattern.compile(XML_EXTENSION + "$");

void parseCompileLog(Path file, Map<String, JSON.Object> compilerLog)
		throws IOException, SAXException, ParserConfigurationException {
	Document aDocument;
	try (BufferedReader reader = Files.newBufferedReader(file)) {
		DocumentBuilder builder = XmlProcessorFactoryRelEng.createDocumentBuilderIgnoringDOCTYPE();
		aDocument = builder.parse(new InputSource(reader));
	}
	if (aDocument == null) {
		return;
	}
	// Get summary of problems
	final NodeList nodeList = aDocument.getElementsByTagName("problem");
	if (nodeList == null || nodeList.getLength() == 0) {
		return;
	}
	int errorCount = 0;
	int warningCount = 0;
	int forbiddenWarningCount = 0;
	int discouragedWarningCount = 0;
	int infoCount = 0;

	final int length = nodeList.getLength();
	for (int i = 0; i < length; i++) {
		final Node problemNode = nodeList.item(i);
		final NamedNodeMap aNamedNodeMap = problemNode.getAttributes();
		final Node severityNode = aNamedNodeMap.getNamedItem("severity");
		final Node idNode = aNamedNodeMap.getNamedItem("id");
		if (severityNode != null) {
			switch (severityNode.getNodeValue()) {
			case "WARNING" -> {
				// this is a warning need to check the id
				String value = idNode == null ? "" : idNode.getNodeValue();
				switch (value) {
				case "ForbiddenReference" -> forbiddenWarningCount++;
				case "DiscouragedReference" -> discouragedWarningCount++;
				default -> warningCount++;
				}
			}
			case "ERROR" -> errorCount++; // this is an error
			case "INFO" -> infoCount++; // this is an info warning
			}
		}
	}
	if (errorCount == 0 && warningCount == 0 && forbiddenWarningCount == 0 && discouragedWarningCount == 0
			&& infoCount == 0) {
		return;
	}

	String relativePath = compileLogsDirectory.relativize(file).toString().replace(File.separatorChar, '/');
	// make sure '.xml' extension is "last thing" in string. (bug 490320)
	String relativePathToHTML = XML_EXTENSION_PATTERN.matcher(relativePath).replaceAll(HTML_EXTENSION);
	String pluginDirectoryName = file.getParent().getFileName().toString();
	String shortName = computeShortName(pluginDirectoryName);

	JSON.Object issues = JSON.Object.create();
	issues.add("path", JSON.String.create(relativePathToHTML));
	addIfNonZero(issues, "errors", errorCount);
	addIfNonZero(issues, "warnings", warningCount);
	addIfNonZero(issues, "infos", infoCount);
	addIfNonZero(issues, "forbidden", forbiddenWarningCount);
	addIfNonZero(issues, "discouraged", discouragedWarningCount);
	if (compilerLog.putIfAbsent(shortName, issues) != null) {
		throw new IllegalStateException("Plugin already set: " + shortName + "\n" + compilerLog.get(shortName));
	}
}

void addIfNonZero(JSON.Object object, String key, int value) {
	if (value != 0) {
		object.add(key, JSON.Integer.create(value));
	}
}

void parseCompileLogs() throws Exception {

	Map<String, JSON.Object> compilerLog = new HashMap<>();
	try (var xmlFiles = Files.walk(compileLogsDirectory).filter(f -> f.toString().endsWith(XML_EXTENSION))
			.filter(Files::isRegularFile)) {
		for (Path file : (Iterable<Path>) xmlFiles::iterator) {
			parseCompileLog(file, compilerLog);
		}
	}

	JSON.Object compilerSummary = JSON.Object.create();
	String note = "This file created by the CompilerSummaryGenerator Java script";
	compilerSummary.add("note", JSON.String.create(note));
	compilerLog.entrySet().stream().sorted(Comparator.comparing(Entry::getKey))
			.forEach(e -> compilerSummary.add(e.getKey(), e.getValue()));

	IO.println("INFO: Plug-ins containing compiler issues: " + compilerLog.size());
	Path file = dropDirectory.resolve(COMPILER_SUMMARY_FILENAME);
	IO.println("Write Compile log summary data to: " + file);
	JSON.write(compilerSummary, file);
}
