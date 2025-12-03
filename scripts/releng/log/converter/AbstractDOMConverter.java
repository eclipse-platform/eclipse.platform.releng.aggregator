/*******************************************************************************
 *  Copyright (c) 2006, 2025 IBM Corporation and others.
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

package log.converter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import log.converter.LogDocumentNode.ProblemSummaryNode;

public abstract class AbstractDOMConverter implements IDOMConverter {

	public static final String FORBIDDEN_REFERENCE = "ForbiddenReference"; //$NON-NLS-1$
	public static final String DISCOURAGED_REFERENCE = "DiscouragedReference"; //$NON-NLS-1$
	public static final Set<String> FILTERED_WARNINGS_IDS = Set.of(FORBIDDEN_REFERENCE, DISCOURAGED_REFERENCE);

	protected ResourceBundle messages;

	private String convertToHTML(final String s) {
		final StringBuilder buffer = new StringBuilder();
		for (int i = 0, max = s.length(); i < max; i++) {
			final char c = s.charAt(i);
			switch (c) {
			case '<' -> buffer.append("&lt;"); //$NON-NLS-1$
			case '>' -> buffer.append("&gt;"); //$NON-NLS-1$
			case '\"' -> buffer.append("&quot;"); //$NON-NLS-1$
			case '&' -> buffer.append("&amp;"); //$NON-NLS-1$
			case '^' -> buffer.append("&and;"); //$NON-NLS-1$
			default -> buffer.append(c);
			}
		}
		return buffer.toString();
	}

	@Override
	public void dump(final int formatVersion, final Map<String, String> options, final LogDocumentNode documentNode) {
		switch (formatVersion) {
		case CompileLogConverter.FORMAT_VERSION_2:
			dumpVersion2(options, documentNode);
		}
	}

	private void dumpVersion2(final Map<String, String> options, final LogDocumentNode documentNode) {
		final String fileName = options.get(CompileLogConverter.OUTPUT_FILE_NAME);
		final ProblemSummaryNode summaryNode = documentNode.getSummaryNode();
		if ((summaryNode == null) || (summaryNode.numberOfProblems() == 0)) {
			return;
		}
		try (final Writer writer = new BufferedWriter(new FileWriter(fileName))) {
			final String pluginName = extractPluginName(fileName);
			if (pluginName == null) {
				writer.write(messages.getString("header")); //$NON-NLS-1$
			} else {
				final String pattern = messages.getString("dom_header"); //$NON-NLS-1$
				writer.write(MessageFormat.format(pattern, pluginName,
						extractXmlFileName(options.get(CompileLogConverter.INPUT_SOURCE))));
			}
			writeTopAnchor(writer);
			writer.write(MessageFormat.format(messages.getString("problem.summary"), //
					summaryNode.numberOfProblems(), summaryNode.numberOfErrors(), summaryNode.numberOfWarnings(),
					summaryNode.numberOfInfos()));

			writeAnchorsReferences(writer);
			List<ProblemsNode> problemsNodes = documentNode.getProblems();
			int globalErrorNumber = 1;

			writeErrorAnchor(writer);
			writeAnchorsReferencesErrors(writer);
			// dump errors
			for (final ProblemsNode problemsNode : problemsNodes) {
				globalErrorNumber = writeNodes(writer, pluginName, globalErrorNumber, problemsNode,
						problemsNode.getErrors(), "error", "errors", problemsNode.numberOfErrors);
			}

			writeOtherWarningsAnchor(writer);
			writeAnchorsReferencesOtherWarnings(writer);
			// dump other warnings
			for (final ProblemsNode problemsNode : problemsNodes) {
				globalErrorNumber = writeNodes(writer, pluginName, globalErrorNumber, problemsNode,
						problemsNode.getOtherWarnings(), "warning", "other_warnings", problemsNode.numberOfWarnings);
			}

			// dump infos
			writeInfosAnchor(writer);
			writeAnchorsReferencesInfos(writer);
			for (final ProblemsNode problemsNode : problemsNodes) {
				globalErrorNumber = writeNodes(writer, pluginName, globalErrorNumber, problemsNode,
						problemsNode.getInfos(), "info", "infos", problemsNode.numberOfInfos);
			}

			// dump forbidden accesses warnings
			writeForbiddenRulesWarningsAnchor(writer);
			writeAnchorsReferencesForbiddenRulesWarnings(writer);
			for (final ProblemsNode problemsNode : problemsNodes) {
				globalErrorNumber = writeNodes(writer, pluginName, globalErrorNumber, problemsNode,
						problemsNode.getForbiddenWarnings(), "warning", "forbidden_warnings",
						problemsNode.numberOfWarnings);
			}

			// dump discouraged accesses warnings
			writeDiscouragedRulesWarningsAnchor(writer);
			writeAnchorsReferencesDiscouragedRulesWarnings(writer);
			for (final ProblemsNode problemsNode : problemsNodes) {
				globalErrorNumber = writeNodes(writer, pluginName, globalErrorNumber, problemsNode,
						problemsNode.getDiscouragedWarnings(), "warning", "discouraged_warnings",
						problemsNode.numberOfWarnings);
			}

			writer.write(messages.getString("footer")); //$NON-NLS-1$
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private int writeNodes(Writer writer, String pluginName, int globalErrorNumber, ProblemsNode problemsNode,
			List<ProblemNode> problemNodes, String type, String titleType, int numberOfIssues) throws IOException {
		if (problemNodes.isEmpty()) {
			return globalErrorNumber;
		}
		MessageFormat form = new MessageFormat(messages.getString(titleType + ".header"));
		double[] warningsLimits = { 1, 2 };
		String[] warningParts = { messages.getString("one_" + type), //$NON-NLS-1$
				messages.getString("multiple_" + type + "s") //$NON-NLS-1$
		};
		ChoiceFormat warningForm = new ChoiceFormat(warningsLimits, warningParts);
		String sourceFileName = extractRelativePath(problemsNode.sourceFileName, pluginName);
		form.setFormatByArgumentIndex(1, warningForm);
		Object[] arguments = new Object[] { sourceFileName, numberOfIssues };
		writer.write(form.format(arguments));
		for (int j = 0; j < problemNodes.size(); j++) {
			ProblemNode problemNode = problemNodes.get(j);
			String pattern = messages.getString(type + "s.entry." + ((j & 1) != 0 ? "odd" : "even"));
			problemNode.setSources();
			writer.write(MessageFormat.format(pattern, sourceFileName, globalErrorNumber, j + 1, problemNode.id,
					problemNode.line, convertToHTML(problemNode.message), convertToHTML(problemNode.sourceCodeBefore),
					convertToHTML(problemNode.sourceCode), convertToHTML(problemNode.sourceCodeAfter),
					getUnderLine(problemNode.sourceCodeBefore, problemNode.sourceCodeAfter), problemNode.charStart,
					problemNode.charEnd));
			globalErrorNumber++;
		}
		writer.write(messages.getString(titleType + ".footer")); //$NON-NLS-1$
		return globalErrorNumber;
	}

	private String extractPluginName(final String fileName) {
		// fileName is fully qualified and we want to extract the segment before
		// the log file name
		// file name contains only '/'
		final String logName = fileName.replace('\\', '/');
		final int index = logName.lastIndexOf('/');
		if (index == -1) {
			return null;
		}
		final int index2 = logName.lastIndexOf('/', index - 1);
		if (index2 == -1) {
			return null;
		}
		return logName.substring(index2 + 1, index);
	}

	private String extractRelativePath(final String sourceFileName, final String pluginName) {
		if (pluginName == null) {
			return sourceFileName;
		}
		final int index = pluginName.indexOf('_');
		if (index == -1) {
			return sourceFileName;
		}
		final String pluginShortName = pluginName.substring(0, index);
		final int index2 = sourceFileName.indexOf(pluginShortName);
		if (index2 == -1) {
			return sourceFileName;
		}
		return sourceFileName.substring(index2 + pluginShortName.length(), sourceFileName.length());
	}

	private String extractXmlFileName(final String fileName) {
		// fileName is fully qualified and we want to extract the segment before
		// the log file name
		// file name contains only '/'
		final String logName = fileName.replace('\\', '/');
		final int index = logName.lastIndexOf('/');
		if (index == -1) {
			return null;
		}
		return logName.substring(index + 1, logName.length());
	}
}
