
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import log.converter.DOMHtmlConverter;
import log.converter.LogDocumentNode;
import log.converter.LogDocumentNode.ProblemSummaryNode;
import log.converter.ProblemNode;
import log.converter.ProblemNode.SeverityType;
import log.converter.ProblemsNode;
import utilities.OS;
import utilities.XmlProcessorFactoryRelEng;

public class CompileLogConverter {

	private static final String HTML_EXTENSION = ".html"; //$NON-NLS-1$

	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	private static List<Path> getAllFiles(Path root) throws IOException {
		try (var files = Files.walk(root).filter(Files::isRegularFile)) {
			return files.filter(f -> f.toString().toLowerCase(Locale.ROOT).endsWith(XML_EXTENSION)).toList();
		}
	}

	public static void main(String[] args) throws IOException, ParserConfigurationException {
		Path source = Path.of(OS.readProperty("input"));
		CompileLogConverter converter = new CompileLogConverter();
		converter.parse2(source);
	}

	private Path extractNameFrom(Path file) {
		String inputFileName = file.getFileName().toString();
		final int index = inputFileName.lastIndexOf('.');
		return file.resolveSibling(inputFileName.substring(0, index) + HTML_EXTENSION);
	}

	private void parse2(Path sourceDir) throws ParserConfigurationException, IOException {
		DOMHtmlConverter converter = new DOMHtmlConverter();
		final DocumentBuilderFactory factory = XmlProcessorFactoryRelEng.createDocumentBuilderFactoryIgnoringDOCTYPE();
		factory.setValidating(true);
		factory.setIgnoringElementContentWhitespace(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		// Commented due to
		// https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/1943
		// builder.setEntityResolver((publicId, systemId) -> new InputSource(new
		// ByteArrayInputStream(new byte[0])));
		if (true) {
			// collect all xml files and iterate over them
			if (!Files.exists(sourceDir)) {
				throw new IllegalArgumentException("Directory " + sourceDir + " doesn't exist");//$NON-NLS-1$//$NON-NLS-2$
			}
			if (!Files.isDirectory(sourceDir)) {
				throw new IllegalArgumentException(sourceDir + " must be a directory in recursive mode");//$NON-NLS-1$
			}
			List<Path> xmlFiles = getAllFiles(sourceDir);
			for (Path xmlFile : xmlFiles) {
				Path inputFile = xmlFile.toAbsolutePath();
				Path outputFile = extractNameFrom(inputFile);
				try {
					builder.setErrorHandler(new DefaultHandler() {
						@Override
						public void error(final SAXParseException e) throws SAXException {
							reportError(inputFile, e);
							throw e;
						}
					});
					Document document = builder.parse(inputFile.toFile());
					final LogDocumentNode documentNode = process(document);
					converter.dump(inputFile, outputFile, documentNode);
				} catch (final SAXException e) {
					System.out.println(e);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private LogDocumentNode process(final Document document) {
		final LogDocumentNode documentNode = new LogDocumentNode();
		NodeList nodeList = document.getElementsByTagName("problem_summary"); //$NON-NLS-1$
		if (nodeList.getLength() == 1) {
			final Node problemSummaryNode = nodeList.item(0);
			final NamedNodeMap problemSummaryMap = problemSummaryNode.getAttributes();
			final ProblemSummaryNode summaryNode = new ProblemSummaryNode(
					Integer.parseInt(problemSummaryMap.getNamedItem("problems").getNodeValue()),
					Integer.parseInt(problemSummaryMap.getNamedItem("errors").getNodeValue()),
					Integer.parseInt(problemSummaryMap.getNamedItem("warnings").getNodeValue()),
					Integer.parseInt(problemSummaryMap.getNamedItem("infos").getNodeValue()));
			documentNode.setProblemSummary(summaryNode);
		}

		nodeList = document.getElementsByTagName("problems"); //$NON-NLS-1$
		if (nodeList == null) {
			return null;
		}

		final int length = nodeList.getLength();
		int globalErrorNumber = 1;
		for (int i = 0; i < length; i++) {
			final Node problemsNode = nodeList.item(i);
			final ProblemsNode node = new ProblemsNode();
			documentNode.addProblemsNode(node);
			final Node sourceNode = problemsNode.getParentNode();
			final NamedNodeMap sourceNodeMap = sourceNode.getAttributes();
			final String sourceFileName = sourceNodeMap.getNamedItem("path").getNodeValue();//$NON-NLS-1$
			node.sourceFileName = sourceFileName;
			final NamedNodeMap problemsNodeMap = problemsNode.getAttributes();
			node.numberOfErrors = Integer.parseInt(problemsNodeMap.getNamedItem("errors").getNodeValue());//$NON-NLS-1$
			node.numberOfWarnings = Integer.parseInt(problemsNodeMap.getNamedItem("warnings").getNodeValue());//$NON-NLS-1$
			node.numberOfProblems = Integer.parseInt(problemsNodeMap.getNamedItem("problems").getNodeValue());//$NON-NLS-1$
			node.numberOfInfos = Integer.parseInt(problemsNodeMap.getNamedItem("infos").getNodeValue());//$NON-NLS-1$

			final NodeList children = problemsNode.getChildNodes();
			final int childrenLength = children.getLength();
			for (int j = 0; j < childrenLength; j++) {
				final Node problemNode = children.item(j);
				final NamedNodeMap problemNodeMap = problemNode.getAttributes();
				final String severity = problemNodeMap.getNamedItem("severity").getNodeValue();//$NON-NLS-1$
				final ProblemNode problem = new ProblemNode();
				problem.id = problemNodeMap.getNamedItem("id").getNodeValue();//$NON-NLS-1$
				switch (severity) {
				case "ERROR":
					problem.severityType = SeverityType.ERROR;
					node.addError(problem);
					break;
				case "INFO":
					problem.severityType = SeverityType.INFO;
					node.addInfo(problem);
					break;
				case "WARNING":
					problem.severityType = SeverityType.WARNING;
					if (DOMHtmlConverter.FILTERED_WARNINGS_IDS.contains(problem.id)) {
						if (DOMHtmlConverter.FORBIDDEN_REFERENCE.equals(problem.id)) {
							node.addForbiddenWarning(problem);
						} else {
							node.addDiscouragedWarning(problem);
						}
					} else {
						node.addOtherWarning(problem);
					}
					break;
				}
				problem.charStart = Integer.parseInt(problemNodeMap.getNamedItem("charStart").getNodeValue());//$NON-NLS-1$
				problem.charEnd = Integer.parseInt(problemNodeMap.getNamedItem("charEnd").getNodeValue());//$NON-NLS-1$
				problem.line = Integer.parseInt(problemNodeMap.getNamedItem("line").getNodeValue());//$NON-NLS-1$
				problem.globalProblemNumber = globalErrorNumber;
				problem.problemNumber = j;
				problem.sourceFileName = sourceFileName;
				globalErrorNumber++;
				final NodeList problemChildren = problemNode.getChildNodes();
				final int problemChildrenLength = problemChildren.getLength();
				for (int n = 0; n < problemChildrenLength; n++) {
					final Node child = problemChildren.item(n);
					final String nodeName = child.getNodeName();
					if ("message".equals(nodeName)) {//$NON-NLS-1$
						final NamedNodeMap childNodeMap = child.getAttributes();
						problem.message = childNodeMap.getNamedItem("value").getNodeValue();//$NON-NLS-1$
					} else if ("source_context".equals(nodeName)) {//$NON-NLS-1$
						final NamedNodeMap childNodeMap = child.getAttributes();
						problem.sourceStart = Integer.parseInt(childNodeMap.getNamedItem("sourceStart").getNodeValue());//$NON-NLS-1$
						problem.sourceEnd = Integer.parseInt(childNodeMap.getNamedItem("sourceEnd").getNodeValue());//$NON-NLS-1$
						problem.contextValue = childNodeMap.getNamedItem("value").getNodeValue();//$NON-NLS-1$
					}
				}
			}
		}
		return documentNode;
	}

	private void reportError(final Path inputFileName, final SAXParseException e) {
		System.err.println(
				"Error in " + inputFileName + " at line " + e.getLineNumber() + " and column " + e.getColumnNumber()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		System.err.println(e.getMessage());
	}

}
