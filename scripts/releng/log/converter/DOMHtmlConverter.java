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

import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;

public class DOMHtmlConverter extends AbstractDOMConverter {

	public DOMHtmlConverter() {
		messages = ResourceBundle.getBundle("org.eclipse.releng.build.tools.convert.ant.html_messages"); //$NON-NLS-1$
	}

	@Override
	public String getUnderLine(final String sourceBefore, final String sourceOfError) {
		return "";
	}

	@Override
	public void writeAnchorsReferences(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_top"));//$NON-NLS-1$
	}

	@Override
	public void writeAnchorsReferencesDiscouragedRulesWarnings(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_discouraged_warnings"));//$NON-NLS-1$
	}

	@Override
	public void writeAnchorsReferencesErrors(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_errors"));//$NON-NLS-1$
	}

	@Override
	public void writeAnchorsReferencesForbiddenRulesWarnings(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_forbidden_warnings"));//$NON-NLS-1$
	}

	@Override
	public void writeAnchorsReferencesOtherWarnings(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_other_warnings"));//$NON-NLS-1$
	}

	@Override
	public void writeAnchorsReferencesInfos(final Writer writer) throws IOException {
		writer.write(messages.getString("anchors.references.no_infos"));//$NON-NLS-1$
	}

	@Override
	public void writeDiscouragedRulesWarningsAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("discouraged_warnings.title_anchor"));//$NON-NLS-1$
	}

	@Override
	public void writeErrorAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("errors.title_anchor"));//$NON-NLS-1$
	}

	@Override
	public void writeForbiddenRulesWarningsAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("forbidden_warnings.title_anchor"));//$NON-NLS-1$
	}

	@Override
	public void writeOtherWarningsAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("other_warnings.title_anchor"));//$NON-NLS-1$
	}

	@Override
	public void writeInfosAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("infos.title_anchor"));//$NON-NLS-1$
	}

	@Override
	public void writeTopAnchor(final Writer writer) throws IOException {
		writer.write(messages.getString("problem.summary.title_anchor"));//$NON-NLS-1$
	}
}
