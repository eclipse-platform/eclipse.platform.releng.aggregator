/********************************************************************************
 * Copyright (c) 2016, 2022 GK Software SE and others.
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

import static org.eclipse.platform.releng.maven.pom.ArtifactInfo.element;
import static org.eclipse.platform.releng.maven.pom.ArtifactInfo.subElement;

import java.util.Collections;
import java.util.List;

public class Developer {

	private final String url;

	public Developer(String url) {
		this.url = url;
	}

	public static void addUrlDevelopers(String projectURL, String indent, StringBuilder buf) {
		element("developers", indent, buf,
				Developer.pomSubElements(Collections.singletonList(new Developer(projectURL + "/who"))));
	}

	private static String pomSubElements(List<Developer> devs) {
		StringBuilder buf = new StringBuilder();
		for (Developer developer : devs)
			developer.toPom(buf, "");
		return buf.toString();
	}

	protected void toPom(StringBuilder buf, String indent) {
		element("developer", indent, buf, subElement("url", this.url));
	}
}
