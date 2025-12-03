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

public class ProblemsNode {

	public String sourceFileName;
	public int numberOfProblems;
	public int numberOfErrors;
	public int numberOfWarnings;
	public int numberOfInfos;

	private final List<ProblemNode> errorNodes = new ArrayList<>();
	private final List<ProblemNode> otherWarningNodes = new ArrayList<>();
	private final List<ProblemNode> discouragedWarningsNodes = new ArrayList<>();
	private final List<ProblemNode> forbiddenWarningsNodes = new ArrayList<>();
	private final List<ProblemNode> infoNodes = new ArrayList<>();

	public void addDiscouragedWarning(final ProblemNode node) {
		discouragedWarningsNodes.add(node);
	}

	public void addError(final ProblemNode node) {
		errorNodes.add(node);
	}

	public void addForbiddenWarning(final ProblemNode node) {
		forbiddenWarningsNodes.add(node);
	}

	public void addOtherWarning(final ProblemNode node) {
		otherWarningNodes.add(node);
	}

	public void addInfo(final ProblemNode node) {
		infoNodes.add(node);
	}

	public List<ProblemNode> getDiscouragedWarnings() {
		return Collections.unmodifiableList(discouragedWarningsNodes);
	}

	public List<ProblemNode> getErrors() {
		return Collections.unmodifiableList(errorNodes);
	}

	public List<ProblemNode> getForbiddenWarnings() {
		return Collections.unmodifiableList(forbiddenWarningsNodes);
	}

	public List<ProblemNode> getOtherWarnings() {
		return Collections.unmodifiableList(otherWarningNodes);
	}

	public List<ProblemNode> getInfos() {
		return Collections.unmodifiableList(infoNodes);
	}
}
