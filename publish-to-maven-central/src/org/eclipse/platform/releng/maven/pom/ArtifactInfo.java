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

public class ArtifactInfo {

	private static final String SCM_GITROOT = "scm:git:git://git.eclipse.org/gitroot/";
	private static final String SCM_CGIT = "https://git.eclipse.org/c/";

	private static final String SCM_TAG_START = ";tag=\""; // git tag inside Eclipse-SourceReference

	private static final String INDENT = "  ";
	
	private static final String FRONT_MATTER =
			"  <licenses>\n" + 
			"    <license>\n" + 
			"      <name>Eclipse Public License - v 2.0</name>\n" + 
			"      <url>https://www.eclipse.org/legal/epl-2.0/</url>\n" + 
			"      <distribution>repo</distribution>\n" + 
			"    </license>\n" + 
			"  </licenses>\n" + 
			"  <organization>\n" + 
			"    <name>Eclipse Foundation</name>\n" + 
			"    <url>http://www.eclipse.org/</url>\n" + 
			"  </organization>\n" + 
			"  <issueManagement>\n" + 
			"    <system>Bugzilla</system>\n" + 
			"    <url>https://bugs.eclipse.org/</url>\n" + 
			"  </issueManagement>\n";
	
	public static final String COPYRIGHT =
			"<!--\n" +
			"  Copyright (c) 2016, 2018 GK Software SE and others.\n" +
			"\n" + 
			"  This program and the accompanying materials\n" + 
			"  are made available under the terms of the Eclipse Public License 2.0\n" + 
			"  which accompanies this distribution, and is available at\n" + 
			"  https://www.eclipse.org/legal/epl-2.0/\n" + 
			"\n" + 
			"  SPDX-License-Identifier: EPL-2.0\n" + 
			"\n" + 
			"  Contributors:\n" + 
			"      Stephan Herrmann - initial implementation\n" + 
			"-->\n";

	public String bsn;
	public String name;
	public String scmConnection;
	
	@Override
	public String toString() {
		return "ArtifactInfo [bsn=" + bsn + ", name=" + name + ", scmConnection=" + scmConnection + "]";
	}

	public String toPomFragment() {
		try {
			fixData();
			StringBuilder buf = new StringBuilder();
			String indent = INDENT;
			element("name", indent, buf, this.name);
			element("url", indent, buf, "http://www.eclipse.org/"+getProject());
			buf.append(FRONT_MATTER);
			if (this.scmConnection == null) {
				System.err.println("No scm info for "+this.bsn);
			} else {
				String connectionUrl = extractScmConnection();
				String url = extractScmUrl(connectionUrl);
				element("scm", indent, buf,
					subElement("connection", connectionUrl),
					subElement("tag", extractScmTag()),
					subElement("url", url));
				Developer.addUrlDevelopers(url, this.bsn, indent, buf);
//				String projRepo = extractProjectRepo(url);
//				if (projRepo != null) {
//					Developer.addDevelopers(projRepo, this.bsn, indent, buf);
//				} else {
//					System.err.println("Could not determine git repo for "+this.bsn+" from "+url);
//				}
			}
			return buf.toString();
		} catch (RuntimeException e) {
			System.err.println("Failed for "+this);
			throw e;
		}
	}
	
	private void fixData() {
		if (this.scmConnection == null) {
			if (this.bsn.equals("org.eclipse.jdt.core.compiler.batch")) {
				// not a regular OSGi bundle, scm info missing:
				this.scmConnection = "scm:git:git://git.eclipse.org/gitroot/jdt/eclipse.jdt.core.git;path=\"org.eclipse.jdt.core\"";
				System.out.println("Fixed scmUrl for "+this.bsn);
			} else if (this.bsn.startsWith("org.eclipse.emf")) {
				this.scmConnection = "scm:git:https://git.eclipse.org/r/emf/org.eclipse.emf";
				System.out.println("Fixed scmUrl for "+this.bsn);
			} else if (this.bsn.startsWith("org.eclipse.ecf")) {
				this.scmConnection = "scm:git:https://git.eclipse.org/r/ecf/org.eclipse.ecf;tag=\"R-Release_HEAD-sdk_feature-279_279\"";
				System.out.println("Fixed scmUrl for "+this.bsn);
			}
		}
		if (this.name == null || this.name.charAt(0) == '%') {
			if (this.bsn.equals("org.eclipse.core.resources.win32.x86")
					|| this.bsn.equals("org.eclipse.core.resources.win32.x86_64")) {
				this.name = "Core Resource Management Win32 Fragment";
				System.out.println("Fixed name for "+this.bsn);
			}
		}
	}

	String getProject() {
		if (this.bsn.startsWith("org.eclipse.jdt"))
			return "jdt";
		if (this.bsn.startsWith("org.eclipse.pde"))
			return "pde";
		if (this.bsn.startsWith("org.eclipse.ecf"))
			return "ecf";
		return "platform";
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
		int next = this.scmConnection.indexOf("\"", tagStart+SCM_TAG_START.length());
		if (next == -1)
			next = this.scmConnection.length();
		return this.scmConnection.substring(tagStart+SCM_TAG_START.length(), next);
	}

	String extractScmUrl(String connection) {
		if (connection.startsWith(SCM_GITROOT))
			return SCM_CGIT+connection.substring(SCM_GITROOT.length());
		return connection.replace("eclipse.org/r", "eclipse.org/c");
	}

	String extractProjectRepo(String url) {
		int pos = 0;
		for (int i=0; i<5; i++) {
			pos = url.indexOf('/', pos+1);
			if (pos == -1)
				return null;
		}
		return url.substring(0, pos);
	}
	
	public static void element(String tag, String indent, StringBuilder buf, String... contents) {
		buf.append(indent).append('<').append(tag).append('>');
		if (contents.length == 1 && !contents[0].contains("\n")) {
			buf.append(contents[0]);
		} else {
			buf.append("\n");
			for (String content : contents)
				if (content != null)
					for (String line: content.split("\\n")) 
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
