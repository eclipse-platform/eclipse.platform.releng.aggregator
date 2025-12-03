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

public class ProblemNode {

	public enum SeverityType {
		ERROR, WARNING, INFO;
	}

	public SeverityType severityType;
	public int charStart;
	public int charEnd;
	public int line;
	public String id;
	public String message;
	public int sourceStart;
	public int sourceEnd;
	public String contextValue;
	public int globalProblemNumber;
	public int problemNumber;
	public String sourceFileName;

	public String sourceCodeBefore;
	public String sourceCodeAfter;
	public String sourceCode;

	public void setSources() {
		if ((sourceStart == -1) || (sourceEnd == -1)) {
			sourceCodeBefore = "";
			sourceCode = contextValue;
			sourceCodeAfter = "";
		} else {
			final int length = contextValue.length();
			if (sourceStart < length) {
				sourceCodeBefore = contextValue.substring(0, sourceStart);
				final int end = sourceEnd + 1;
				if (end < length) {
					sourceCode = contextValue.substring(sourceStart, end);
					sourceCodeAfter = contextValue.substring(end, length);
				} else {
					sourceCode = contextValue.substring(sourceStart, length);
					sourceCodeAfter = "";
				}
			} else {
				sourceCodeBefore = "";
				sourceCode = "";
				sourceCodeAfter = "";
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		switch (severityType) {
		case ERROR -> buffer.append("ERROR ");//$NON-NLS-1$
		case WARNING -> buffer.append("WARNING ");//$NON-NLS-1$
		case INFO -> buffer.append("INFO ");//$NON-NLS-1$
		}
		buffer.append("line : ").append(line).append(" message = ").append(message);//$NON-NLS-1$//$NON-NLS-2$
		return buffer.toString();
	}
}
