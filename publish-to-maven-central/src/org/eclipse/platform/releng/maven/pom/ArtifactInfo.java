/********************************************************************************
 * Copyright (c) 2016, 2018 GK Software SE and others.
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
 ********************************************************************************/
package org.eclipse.platform.releng.maven.pom;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactInfo {

	private static final String SCM_TAG_START = ";tag=\""; // git tag inside Eclipse-SourceReference

	private static final String INDENT = "  ";

	private static final Map<String, String> MOVED_SCMS = new HashMap<>();

	private static final Pattern GITHUB_PATTERN = Pattern.compile("https://github.com/([^/]+)/.*");

	static {
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.releng",
				"https://github.com/eclipse-platform/eclipse.platform.releng");
		MOVED_SCMS.put("https://git.eclipse.org/r/platform/eclipse.platform.releng",
				"https://github.com/eclipse-platform/eclipse.platform.releng");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.ui",
				"https://github.com/eclipse-platform/eclipse.platform.ui");
		MOVED_SCMS.put("https://git.eclipse.org/r/platform/eclipse.platform.ui",
				"https://github.com/eclipse-platform/eclipse.platform.ui");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.text",
				"https://github.com/eclipse-platform/eclipse.platform.text");
		MOVED_SCMS.put("https://git.eclipse.org/r/platform/eclipse.platform.text",
				"https://github.com/eclipse-platform/eclipse.platform.text");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.debug",
				"https://github.com/eclipse-platform/eclipse.platform.debug");
		MOVED_SCMS.put("http://git.eclipse.org/gitroot/e4/org.eclipse.e4.tools",
				"https://github.com/eclipse-platform/eclipse.platform.ui.tools");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.ua",
				"https://github.com/eclipse-platform/eclipse.platform.ua");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/platform/eclipse.platform.swt",
				"https://github.com/eclipse-platform/eclipse.platform.swt");

		MOVED_SCMS.put("https://gihub.com/eclipse-platform/eclipse.platform.ua",
				"https://github.com/eclipse-platform/eclipse.platform.ua");
		MOVED_SCMS.put("https://github.com/eclipse-platform/org.eclipse.ui.tools",
				"https://github.com/eclipse-platform/eclipse.platform.ui.tools");

		MOVED_SCMS.put("https://git.eclipse.org/r/equinox/rt.equinox.bundles", "https://github.com/eclipse-equinox/p2");
		MOVED_SCMS.put("https://git.eclipse.org/r/equinox/rt.equinox.p2", "https://github.com/eclipse-equinox/p2");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/equinox/rt.equinox.p2", "https://github.com/eclipse-equinox/p2");
		MOVED_SCMS.put("https://git.eclipse.org/r/equinox/rt.equinox.framework",
				"https://github.com/eclipse-equinox/equinox");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/equinox/rt.equinox.bundles",
				"https://github.com/eclipse-equinox/equinox");

		MOVED_SCMS.put("git://git.eclipse.org/gitroot/jdt/eclipse.jdt.core.binaries",
				"https://github.com/eclipse-jdt/eclipse.jdt.core.binaries");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/jdt/eclipse.jdt.ui",
				"https://github.com/eclipse-jdt/eclipse.jdt.ui");
		MOVED_SCMS.put("git://git.eclipse.org/gitroot/jdt/eclipse.jdt.core",
				"https://github.com/eclipse-jdt/eclipse.jdt.core");
		MOVED_SCMS.put("https://git.eclipse.org/r/jdt/eclipse.jdt.core",
				"https://github.com/eclipse-jdt/eclipse.jdt.core");

		MOVED_SCMS.put("git://git.eclipse.org/gitroot/pde/eclipse.pde.ui",
				"https://github.com/eclipse-pde/eclipse.pde");
		MOVED_SCMS.put("https://git.eclipse.org/r/pde/eclipse.pde.ui", "https://github.com/eclipse-pde/eclipse.pde");
	}

	public String bsn;
	public String scmConnection;

	@Override
	public String toString() {
		return "ArtifactInfo [bsn=" + bsn + ", scmConnection=" + scmConnection + "]";
	}

	public String toPomFragment() {
		try {
			fixData();
			StringBuilder buf = new StringBuilder();
			String indent = INDENT;
			element("url", indent, buf,  getProjectURL());
			if (this.scmConnection == null) {
				System.err.println("No scm info for " + this.bsn);
			} else {
				String connectionUrl = extractScmConnection();
				String url = extractScmUrl(connectionUrl);
				buf.append(getFrontMatter(url));
				element("scm", indent, buf, subElement("connection", connectionUrl), subElement("tag", extractScmTag()),
						subElement("url", url));
				Developer.addUrlDevelopers(getProjectURL(), indent, buf);
			}
			return buf.toString();
		} catch (RuntimeException e) {
			System.err.println("Failed for " + this);
			throw e;
		}
	}

	private static String getFrontMatter(String scmURL) {
		String FRONT_MATTER = "" + //
				"  <licenses>\n" + //
				"    <license>\n" + //
				"      <name>Eclipse Public License - v 2.0</name>\n" + //
				"      <url>https://www.eclipse.org/legal/epl-2.0/</url>\n" + //
				"      <distribution>repo</distribution>\n" + //
				"    </license>\n" + //
				"  </licenses>\n" + //
				"  <organization>\n" + //
				"    <name>Eclipse Foundation</name>\n" + //
				"    <url>https://www.eclipse.org/</url>\n" + //
				"  </organization>\n" + //
				"  <issueManagement>\n" + //
				"    <system>Github</system>\n" + //
				"    <url>" + scmURL + "/issues</url>\n" + //
				"  </issueManagement>\n"; //
		return FRONT_MATTER;
	}

	private void fixData() {
		if (this.scmConnection == null) {
			if (this.bsn.equals("org.eclipse.jdt.core.compiler.batch")) {
				// not a regular OSGi bundle, scm info missing:
				this.scmConnection = "scm:git:https://github.com/eclipse-jdt/eclipse.jdt.core.git;path=\"org.eclipse.jdt.core\"";
				System.out.println("Fixed scmUrl for " + this.bsn);
			}
		} else {
			this.scmConnection = "scm:git:" + extractScmUrl(extractScmConnection()) + ".git";
		}
	}

	String getProjectURL() {
		String scmURL = extractScmUrl(extractScmConnection());
		Matcher matcher = GITHUB_PATTERN.matcher(scmURL);
		if (matcher.matches()) {
			String organization = matcher.group(1);
			return "https://projects.eclipse.org/projects/" + organization.replace('-', '.');
		}

		return "https://projects.eclipse.org/projects/";
	}

	String extractScmConnection() {
		int semi = this.scmConnection.indexOf(';');
		if (semi == -1)
			return this.scmConnection;
		return this.scmConnection.substring(0, semi);
	}

	String extractScmTag() {
		int tagStart = this.scmConnection.indexOf(SCM_TAG_START);
		if (tagStart == -1)
			return null;
		int next = this.scmConnection.indexOf("\"", tagStart + SCM_TAG_START.length());
		if (next == -1)
			next = this.scmConnection.length();
		return this.scmConnection.substring(tagStart + SCM_TAG_START.length(), next);
	}

	private static final Set<String> visitedSCMs = new HashSet<>();

	String extractScmUrl(String connection) {
		String scmURL = connection.replaceAll("^scm:git:", "").replaceAll("^scm:githttps:", "https:")
				.replaceAll("\\.git$", "");
		String movedSCMURL = MOVED_SCMS.get(scmURL);
		if (movedSCMURL != null) {
			scmURL = movedSCMURL;
		}
		if (EnrichPoms.test && visitedSCMs.add(scmURL)) {
			try (InputStream in = new URL(scmURL).openStream()) {
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return scmURL;
	}

	public static void element(String tag, String indent, StringBuilder buf, String... contents) {
		buf.append(indent).append('<').append(tag).append('>');
		if (contents.length == 1 && !contents[0].contains("\n")) {
			buf.append(contents[0]);
		} else {
			buf.append("\n");
			for (String content : contents)
				if (content != null)
					for (String line : content.split("\\n"))
						buf.append(indent).append(INDENT).append(line).append('\n');
			buf.append(indent);
		}
		buf.append("</").append(tag).append(">\n");
	}

	public static String subElement(String tag, String content) {
		if (content == null)
			return null;
		StringBuilder buf = new StringBuilder();
		element(tag, "", buf, content);
		return buf.toString();
	}
}
