/********************************************************************************
 * Copyright (c) 2016, 2025 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial implementation
 *     Hannes Wellmann - Simplify and remove now unncessary elements
 ********************************************************************************/
package org.eclipse.platform.releng.maven.pom;

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ArtifactInfo(String bsn, String scmConnection) {

	private static final String SCM_TAG_START = ";tag=\""; // git tag inside Eclipse-SourceReference
	private static final String INDENT = "  ";
	private static final Pattern GITHUB_PATTERN = Pattern.compile("https://github.com/([^/]+)/.*");

	public ArtifactInfo {
		Objects.requireNonNull(bsn);
		Objects.requireNonNull(scmConnection);
	}

	String toPomFragment() {
		try {
			StringBuilder buf = new StringBuilder();
			String projectURL = getProjectURL();
			element("url", INDENT, buf, projectURL);
			String url = extractScmUrl();
			buf.append(getFrontMatter(url));
			element("scm", INDENT, buf, subElement("connection", extractScmConnection()),
					subElement("tag", extractScmTag()), subElement("url", url));
			addUrlDevelopers(projectURL, INDENT, buf);
			return buf.toString();
		} catch (RuntimeException e) {
			throw new IllegalStateException("Failed for " + this, e);
		}
	}

	private static String getFrontMatter(String scmURL) {
		return String.format("""
				  <licenses>
				    <license>
				      <name>Eclipse Public License - v 2.0</name>
				      <url>https://www.eclipse.org/legal/epl-2.0/</url>
				      <distribution>repo</distribution>
				    </license>
				  </licenses>
				  <organization>
				    <name>Eclipse Foundation</name>
				    <url>https://www.eclipse.org/</url>
				  </organization>
				  <issueManagement>
				    <system>Github</system>
				    <url>%s/issues</url>
				  </issueManagement>
				""", scmURL);
	}

	private String getProjectURL() {
		String scmURL = extractScmUrl();
		Matcher matcher = GITHUB_PATTERN.matcher(scmURL);
		if (matcher.matches()) {
			String organization = matcher.group(1);
			return "https://projects.eclipse.org/projects/" + organization.replace('-', '.');
		}
		throw new IllegalArgumentException("Unexpected scm-URL: " + scmURL);
	}

	private String extractScmConnection() {
		return substringTo(this.scmConnection, 0, ';');
	}

	private String extractScmTag() {
		int tagStart = this.scmConnection.indexOf(SCM_TAG_START);
		if (tagStart == -1) {
			throw new IllegalArgumentException("scm-tag not found in " + this.scmConnection);
		}
		return substringTo(this.scmConnection, tagStart + SCM_TAG_START.length(), '"');
	}

	private static void addUrlDevelopers(String projectURL, String indent, StringBuilder buf) {
		StringBuilder subElements = new StringBuilder();
		element("developer", "", subElements, subElement("url", projectURL + "/who"));
		element("developers", indent, buf, subElements.toString());
	}

	private static final Set<String> visitedSCMs = new HashSet<>();

	private String extractScmUrl() {
		String connection = extractScmConnection();
		String scmURL = connection.replaceFirst("^scm:git:", "").replaceFirst("^scm:githttps:", "https:")
				.replaceFirst("\\.git$", "");
		if (EnrichPoms.test && visitedSCMs.add(scmURL)) {
			try (InputStream in = new URI(scmURL).toURL().openStream()) {
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return scmURL;
	}

	private static String substringTo(String string, int beginIndex, char endChar) {
		int endIndex = string.indexOf(endChar, beginIndex);
		return endIndex == -1 ? string.substring(beginIndex) : string.substring(beginIndex, endIndex);
	}

	private static void element(String tag, String indent, StringBuilder buf, String... contents) {
		buf.append(indent).append('<').append(tag).append('>');
		if (contents.length == 1 && !contents[0].contains("\n")) {
			buf.append(contents[0]);
		} else {
			buf.append("\n");
			for (String content : contents) {
				content.lines().forEach(line -> buf.append(indent).append(INDENT).append(line).append('\n'));
			}
			buf.append(indent);
		}
		buf.append("</").append(tag).append(">\n");
	}

	private static String subElement(String tag, String content) {
		Objects.requireNonNull(content);
		StringBuilder buf = new StringBuilder();
		element(tag, "", buf, content);
		return buf.toString();
	}
}
