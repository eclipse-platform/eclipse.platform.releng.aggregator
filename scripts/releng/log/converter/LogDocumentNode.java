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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogDocumentNode {

	public record ProblemSummaryNode(int numberOfProblems, int numberOfErrors, int numberOfWarnings,
			int numberOfInfos) {
		@Override
		public String toString() {
			return "problems : " + numberOfProblems //
					+ " errors : " + numberOfErrors //
					+ " warnings : " + numberOfWarnings //
					+ " infos : " + numberOfInfos;
		}
	}

	private final List<ProblemsNode> problems = new ArrayList<>();
	private ProblemSummaryNode summaryNode;

	public void addProblemsNode(final ProblemsNode node) {
		problems.add(node);
	}

	public List<ProblemsNode> getProblems() {
		return Collections.unmodifiableList(problems);
	}

	public ProblemSummaryNode getSummaryNode() {
		return summaryNode;
	}

	public void setProblemSummary(final ProblemSummaryNode node) {
		summaryNode = node;
	}
}
