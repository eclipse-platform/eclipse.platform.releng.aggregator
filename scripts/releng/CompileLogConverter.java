
/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.releng.build.tools.convert.dom.AbstractDOMConverter;
import org.eclipse.releng.build.tools.convert.dom.DOMHtmlConverter;
import org.eclipse.releng.build.tools.convert.dom.DOMTxtConverter;
import org.eclipse.releng.build.tools.convert.dom.IDOMConverter;
import org.eclipse.releng.build.tools.convert.dom.LogDocumentNode;
import org.eclipse.releng.build.tools.convert.dom.LogDocumentNode.ProblemSummaryNode;
import org.eclipse.releng.build.tools.convert.dom.ProblemNode;
import org.eclipse.releng.build.tools.convert.dom.ProblemNode.SeverityType;
import org.eclipse.releng.build.tools.convert.dom.ProblemsNode;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class CompileLogConverter {

	private static final String HTML_EXTENSION = ".html"; //$NON-NLS-1$

	private static final String TXT_EXTENSION = ".txt"; //$NON-NLS-1$

	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	public static final String INPUT_SOURCE = "inputSource"; //$NON-NLS-1$

	public static final String CONVERTER_ID = "converterID"; //$NON-NLS-1$

	public static final String OUTPUT_FILE_NAME = "outputFileName"; //$NON-NLS-1$

	public static final String ENABLE_VALIDATION = "enableValidation"; //$NON-NLS-1$

	public static final String RECURSIVE = "recurse"; //$NON-NLS-1$

	public static final int FORMAT_VERSION_2 = 2;

	public static final int CURRENT_FORMAT_VERSION = FORMAT_VERSION_2;

	private static final FileFilter XML_FILTER = pathname -> {
		final String path = pathname.getAbsolutePath().toLowerCase();
		return path.endsWith(XML_EXTENSION) || pathname.isDirectory();
	};

	private static void collectAllFiles(final File root, final ArrayList<File> collector, final FileFilter fileFilter) {
		final File[] files = root.listFiles(fileFilter);
		for (final File currentFile : files) {
			if (currentFile.isDirectory()) {
				collectAllFiles(currentFile, collector, fileFilter);
			} else {
				collector.add(currentFile);
			}
		}
	}

	public static File[] getAllFiles(final File root, final FileFilter fileFilter) {
		final ArrayList<File> files = new ArrayList<>();
		if (root.isDirectory()) {
			collectAllFiles(root, files, fileFilter);
			final File[] result = new File[files.size()];
			files.toArray(result);
			return result;
		}
		return null;
	}

	public static void main(final String[] args) {
		try {
			run(args);
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static void run(final String... args) throws ParserConfigurationException {
		final CompileLogConverter converter = new CompileLogConverter();
		converter.configure(args);
		converter.parse2();
	}

	private final HashMap<String, String> options = new HashMap<>();

	public void configure(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			throw new IllegalArgumentException("Arguments cannot be empty");//$NON-NLS-1$
		}

		// set default options
		options.put(ENABLE_VALIDATION, "false"); //$NON-NLS-1$
		int converterID = CompileLogConverter.HTML;

		final int argCount = args.length;
		int index = 0;
		boolean setOutputFile = false;
		boolean setValidation = false;
		boolean setInputFile = false;

		final int DEFAULT_MODE = 0;
		final int OUTPUT_FILE_MODE = 1;
		final int INPUT_FILE_MODE = 2;

		int mode = DEFAULT_MODE;

		loop: while (index < argCount) {
			final String currentArg = args[index++];

			switch (mode) {
			case INPUT_FILE_MODE:
				options.put(INPUT_SOURCE, currentArg);
				mode = DEFAULT_MODE;
				break;
			case OUTPUT_FILE_MODE:
				if (currentArg.toLowerCase().endsWith(TXT_EXTENSION)) {
					converterID = CompileLogConverter.TXT;
				}
				options.put(OUTPUT_FILE_NAME, currentArg);
				mode = DEFAULT_MODE;
				continue loop;
			case DEFAULT_MODE:
				if (currentArg.equals("-v")) { //$NON-NLS-1$
					if (setValidation) {
						throw new IllegalArgumentException("Duplicate validation flag"); //$NON-NLS-1$
					}
					setValidation = true;
					options.put(ENABLE_VALIDATION, "true"); //$NON-NLS-1$
					mode = DEFAULT_MODE;
					continue loop;
				}
				if (currentArg.equals("-o")) { //$NON-NLS-1$
					if (setOutputFile) {
						throw new IllegalArgumentException("Duplicate output file"); //$NON-NLS-1$
					}
					setOutputFile = true;
					mode = OUTPUT_FILE_MODE;
					continue loop;
				}
				if (currentArg.equals("-i")) { //$NON-NLS-1$
					if (setInputFile) {
						throw new IllegalArgumentException("Duplicate input file"); //$NON-NLS-1$
					}
					setInputFile = true;
					mode = INPUT_FILE_MODE;
					continue loop;
				}
				if (currentArg.equals("-r")) { //$NON-NLS-1$
					options.put(RECURSIVE, "true"); //$NON-NLS-1$
					mode = DEFAULT_MODE;
				}
			}
		}
		final String input = options.get(INPUT_SOURCE);
		if (input == null) {
			throw new IllegalArgumentException("An input file or directorty is required"); //$NON-NLS-1$
		}
		if (options.get(RECURSIVE) == null) {
			if (!input.toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
				throw new IllegalArgumentException("Input file must be an xml file"); //$NON-NLS-1$
			}
		}
		options.put(CONVERTER_ID, String.valueOf(converterID));
	}

	private void dump(final LogDocumentNode documentNode) {
		IDOMConverter converter = createDOMConverter(Integer.parseInt(options.get(CompileLogConverter.CONVERTER_ID)));
		converter.dump(CURRENT_FORMAT_VERSION, options, documentNode);
	}

	public static final int TXT = 0;
	public static final int HTML = 1;

	public static IDOMConverter createDOMConverter(final int id) {
		return switch (id) {
		case TXT -> new DOMTxtConverter();
		case HTML -> new DOMHtmlConverter();
		default -> new DOMHtmlConverter();
		};
	}

	private String extractNameFrom(final String inputFileName) {
		final int index = inputFileName.lastIndexOf('.');
		return switch (Integer.parseInt(options.get(CompileLogConverter.CONVERTER_ID))) {
		case CompileLogConverter.TXT -> inputFileName.substring(0, index) + TXT_EXTENSION;
		case CompileLogConverter.HTML -> inputFileName.substring(0, index) + HTML_EXTENSION;
		default -> inputFileName.substring(0, index) + HTML_EXTENSION;
		};
	}

	public void parse2() throws ParserConfigurationException {
		@SuppressWarnings("restriction")
		final DocumentBuilderFactory factory = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createDocumentBuilderFactoryIgnoringDOCTYPE();
		final boolean validation = Boolean.parseBoolean(options.get(ENABLE_VALIDATION));
		factory.setValidating(validation);
		factory.setIgnoringElementContentWhitespace(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		// Commented due to
		// https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/1943
		// builder.setEntityResolver((publicId, systemId) -> new InputSource(new
		// ByteArrayInputStream(new byte[0])));

		final String inputSourceOption = options.get(INPUT_SOURCE);
		if (options.get(RECURSIVE) != null) {
			// collect all xml files and iterate over them
			final File sourceDir = new File(inputSourceOption);
			if (!sourceDir.exists()) {
				throw new IllegalArgumentException("Directory " + inputSourceOption + " doesn't exist");//$NON-NLS-1$//$NON-NLS-2$
			}
			if (!sourceDir.isDirectory()) {
				throw new IllegalArgumentException(inputSourceOption + " must be a directory in recursive mode");//$NON-NLS-1$
			}
			final File[] xmlFiles = getAllFiles(sourceDir, XML_FILTER);
			for (File xmlFile : xmlFiles) {
				final String inputFileName = xmlFile.getAbsolutePath();
				final InputSource inputSource = new InputSource(inputFileName);
				final String outputFileName = extractNameFrom(inputFileName);
				options.put(INPUT_SOURCE, inputFileName);
				options.put(OUTPUT_FILE_NAME, outputFileName);
				try {
					builder.setErrorHandler(new DefaultHandler() {
						@Override
						public void error(final SAXParseException e) throws SAXException {
							reportError(inputFileName, e);
							throw e;
						}
					});
					final Document document = builder.parse(inputSource);
					final LogDocumentNode documentNode = process(document);
					dump(documentNode);
				} catch (final SAXException e) {
					System.out.println(e);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			// Parse
			if (inputSourceOption != null) {
				final InputSource inputSource = new InputSource(inputSourceOption);
				if (options.get(OUTPUT_FILE_NAME) == null) {
					options.put(OUTPUT_FILE_NAME, extractNameFrom(inputSourceOption));
				}
				try {
					builder.setErrorHandler(new DefaultHandler() {

						@Override
						public void error(final SAXParseException e) throws SAXException {
							reportError(inputSourceOption, e);
							throw e;
						}
					});
					final Document document = builder.parse(inputSource);
					final LogDocumentNode documentNode = process(document);
					dump(documentNode);
				} catch (SAXException | IOException e) {
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
					if (AbstractDOMConverter.FILTERED_WARNINGS_IDS.contains(problem.id)) {
						if (AbstractDOMConverter.FORBIDDEN_REFERENCE.equals(problem.id)) {
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

	void reportError(final String inputFileName, final SAXParseException e) {
		System.err.println(
				"Error in " + inputFileName + " at line " + e.getLineNumber() + " and column " + e.getColumnNumber()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		System.err.println(e.getMessage());
	}
}
