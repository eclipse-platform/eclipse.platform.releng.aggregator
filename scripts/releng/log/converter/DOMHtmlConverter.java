
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
 *     Hannes Wellmann - Convert to plain Java scripts
 *******************************************************************************/

package log.converter;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import log.converter.LogDocumentNode.ProblemSummaryNode;

public class DOMHtmlConverter {

	public static final String FORBIDDEN_REFERENCE = "ForbiddenReference"; //$NON-NLS-1$
	public static final String DISCOURAGED_REFERENCE = "DiscouragedReference"; //$NON-NLS-1$
	public static final Set<String> FILTERED_WARNINGS_IDS = Set.of(FORBIDDEN_REFERENCE, DISCOURAGED_REFERENCE);

	private final ResourceBundle messages = ResourceBundle.getBundle("log.converter.html_messages"); //$NON-NLS-1$

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

	private int globalErrorNumber;

	public void dump(Path inputFilename, Path outputFileName, LogDocumentNode documentNode) {
		final ProblemSummaryNode summaryNode = documentNode.getSummaryNode();
		if ((summaryNode == null) || (summaryNode.numberOfProblems() == 0)) {
			return;
		}
		try (Writer writer = Files.newBufferedWriter(outputFileName)) {
			String pluginName = outputFileName.getParent().getFileName().toString();
			if (pluginName == null) {
				writer.write(messages.getString("header")); //$NON-NLS-1$
			} else {
				String pattern = messages.getString("dom_header"); //$NON-NLS-1$
				writer.write(MessageFormat.format(pattern, pluginName, inputFilename.getFileName().toString()));
			}
			writer.write(messages.getString("problem.summary.title_anchor"));//$NON-NLS-1$
			writer.write(MessageFormat.format(messages.getString("problem.summary"), //
					summaryNode.numberOfProblems(), summaryNode.numberOfErrors(), summaryNode.numberOfWarnings(),
					summaryNode.numberOfInfos()));

			writer.write(messages.getString("anchors.references.no_top"));//$NON-NLS-1$
			List<ProblemsNode> problemsNodes = documentNode.getProblems();
			globalErrorNumber = 1;
			// dump errors
			writeIssueSection(writer, pluginName, problemsNodes, "error", "errors", ProblemsNode::getErrors,
					n -> n.numberOfErrors);
			// dump other warnings
			writeIssueSection(writer, pluginName, problemsNodes, "warning", "other_warnings",
					ProblemsNode::getOtherWarnings, n -> n.numberOfWarnings);
			// dump infos
			writeIssueSection(writer, pluginName, problemsNodes, "info", "infos", ProblemsNode::getInfos,
					n -> n.numberOfInfos);
			// dump forbidden accesses warnings
			writeIssueSection(writer, pluginName, problemsNodes, "warning", "forbidden_warnings",
					ProblemsNode::getForbiddenWarnings, n -> n.numberOfWarnings);
			// dump discouraged accesses warnings
			writeIssueSection(writer, pluginName, problemsNodes, "warning", "discouraged_warnings",
					ProblemsNode::getDiscouragedWarnings, n -> n.numberOfWarnings);

			writer.write(messages.getString("footer")); //$NON-NLS-1$
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeIssueSection(Writer writer, String pluginName, List<ProblemsNode> problemsNodes, String type,
			String titleType, Function<ProblemsNode, List<ProblemNode>> getNodes,
			ToIntFunction<ProblemsNode> getNumberOfIssues) throws IOException {
		writer.write(messages.getString(titleType + ".title_anchor"));//$NON-NLS-1$
		writer.write(messages.getString("anchors.references.no_" + titleType));//$NON-NLS-1$
		for (ProblemsNode problemsNode : problemsNodes) {
			List<ProblemNode> problemNodes = getNodes.apply(problemsNode);
			int numberOfIssues = getNumberOfIssues.applyAsInt(problemsNode);
			if (problemNodes.isEmpty()) {
				continue;
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
						problemNode.line, convertToHTML(problemNode.message),
						convertToHTML(problemNode.sourceCodeBefore), convertToHTML(problemNode.sourceCode),
						convertToHTML(problemNode.sourceCodeAfter), "", problemNode.charStart, problemNode.charEnd));
				globalErrorNumber++;
			}
			writer.write(messages.getString(titleType + ".footer")); //$NON-NLS-1$
		}
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

}
