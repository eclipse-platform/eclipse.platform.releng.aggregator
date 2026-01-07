
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

import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utilities.JSON;
import utilities.OS;
import utilities.XmlProcessorFactoryRelEng;

/** @author Dean Roberts (circa 2000!) and David Williams (circa 2016) */

final String XML_EXTENSION = ".xml";
final String COMPILER_SUMMARY_FILENAME = "compilerSummary.json";

Path compileLogsDirectory; // Location of compile logs base directory

void main() throws Exception {
	Path dropDirectory = Path.of(OS.readProperty("dropDirectory")).toRealPath();
	compileLogsDirectory = dropDirectory.resolve("compilelogs");

	IO.println("INFO: Processing compiler logs in");
	IO.println("\t" + compileLogsDirectory);

	parseCompileLogs();
}

void parseCompileLog(Path file, Map<String, JSON.Object> compilerLog)
		throws IOException, SAXException, ParserConfigurationException {
	Document aDocument = XmlProcessorFactoryRelEng.parseDocumentIgnoringDOCTYPE(file);
	int warnings = 0;
	int accessWarnings = 0;
	int infos = 0;
	// Get summary of problems
	NodeList problemList = aDocument.getElementsByTagName("problem");
	for (Element problem : elements(problemList)) {
		String severity = problem.getAttribute("severity");
		switch (severity) {
		case "WARNING" -> {
			// this is a warning need to check the id
			String value = problem.getAttribute("id");
			switch (value) {
			case "ForbiddenReference", "DiscouragedReference" -> accessWarnings++;
			default -> warnings++;
			}
		}
		case "INFO" -> infos++; // this is an info warning
		}
	}
	if (warnings == 0 && accessWarnings == 0 && infos == 0) {
		return;
	}
	String path = compileLogsDirectory.relativize(file).toString().replace(File.separatorChar, '/');
	JSON.Object issues = JSON.Object.create();
	addIfNonZero(issues, "warnings", warnings);
	addIfNonZero(issues, "access", accessWarnings);
	addIfNonZero(issues, "infos", infos);
	if (compilerLog.putIfAbsent(path, issues) != null) {
		throw new IllegalStateException("Plugin already set: " + path + "\n" + compilerLog.get(path));
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
	compilerLog.entrySet().stream().sorted(Comparator.comparing(Entry::getKey))
			.forEach(e -> compilerSummary.add(e.getKey(), e.getValue()));

	IO.println("INFO: Plug-ins containing compiler issues: " + compilerLog.size());
	Path file = compileLogsDirectory.resolve(COMPILER_SUMMARY_FILENAME);
	IO.println("Write Compile log summary data to: " + file);
	JSON.write(compilerSummary, file);
}
