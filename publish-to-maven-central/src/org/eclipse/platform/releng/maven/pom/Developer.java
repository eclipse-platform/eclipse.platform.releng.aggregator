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

import static org.eclipse.platform.releng.maven.pom.ArtifactInfo.element;
import static org.eclipse.platform.releng.maven.pom.ArtifactInfo.subElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Developer {

	static final String PLATFORM_GIT_REPO = "https://git.eclipse.org/c/platform";

	static final String[] ROLE_LEAD = { "Project Lead" };
	
	public static final HashMap<String, List<Developer>> developersPerRepo = new HashMap<>();
	public static final Set<String> projects = new HashSet<>();
	static {
		// currently unused:
		Developer dani = new IndividualDeveloper("Dani Megert");
		developersPerRepo.put(PLATFORM_GIT_REPO,
				Arrays.asList(dani));
		developersPerRepo.put("https://git.eclipse.org/c/equinox",
				Arrays.asList(new IndividualDeveloper("Ian Bull"), new IndividualDeveloper("Pascal Rapicault"), new IndividualDeveloper("Thomas Watson")));
		developersPerRepo.put("https://git.eclipse.org/c/jdt",
				Arrays.asList(dani));
		developersPerRepo.put("https://git.eclipse.org/c/pde",
				Arrays.asList(new IndividualDeveloper("Curtis Windatt"), new IndividualDeveloper("Vikas Chandra")));
		//

		projects.add("eclipse.platform");
		projects.add("eclipse.jdt");
		projects.add("eclipse.pde");
		projects.add("rt.equinox");
	}

	public static void addIndividualDevelopers(String projRepo, String bsn, String indent, StringBuilder buf) {
		List<Developer> devs = getDevelopers(projRepo, bsn);
		if (devs == null)
			System.err.println("No developers for project repo "+projRepo+" ("+bsn+")");
		else
			element("developers", indent, buf, Developer.pomSubElements(devs));
		
	}

	public static void addUrlDevelopers(String gitUrl, String bsn, String indent, StringBuilder buf) {
		String whoSInvolved = gitRepoToWhoSInvolved(gitUrl);
		if (whoSInvolved == null)
			System.err.println("No developers for project repo "+gitUrl+" ("+bsn+")");
		else
			element("developers", indent, buf, Developer.pomSubElements(Collections.singletonList(new UrlDeveloper(whoSInvolved))));
	}

	private static String gitRepoToWhoSInvolved(String gitUrl) {
		String[] tokens = gitUrl.split("/");
		if (tokens.length >= 6) {
			String token = tokens[5]; // https://git.eclipse.org/c/equinox/rt.equinox.framework.git => start with rt.equinox.framework
			int end = token.endsWith(".git") ? token.length()-".git".length() : token.length();
			String project = token.substring(0, end);
			while (!projects.contains(project)) {
				end = project.lastIndexOf('.');
				if (end == -1)
					return null;
				project = project.substring(0, end); // cut off non-matching tail segment
			}
			return "https://projects.eclipse.org/projects/"+project+"/who";
		}
		return null;
	}

	private static List<Developer> getDevelopers(String projRepo, String bsn) {
		// "platform" artifacts in pde repos:
		if ("org.eclipse.ui.views.log".equals(bsn) || "org.eclipse.ui.trace".equals(bsn))
			return developersPerRepo.get(PLATFORM_GIT_REPO);
		// "platform" artifacts in jdt repos:
		if ("org.eclipse.ltk.core.refactoring".equals(bsn) || "org.eclipse.ltk.ui.refactoring".equals(bsn))
			return developersPerRepo.get(PLATFORM_GIT_REPO);
		return developersPerRepo.get(projRepo);
	}

	private static String pomSubElements(List<Developer> devs) {
		StringBuilder buf = new StringBuilder();
		for (Developer developer : devs)
			developer.toPom(buf, "");
		return buf.toString();
	}
	
	protected abstract void toPom(StringBuilder buf, String string);

	/** Represent project leads as individual developers. */
	static class IndividualDeveloper extends Developer {
		String name;
		String[] roles;
		public IndividualDeveloper(String name) {
			this.name = name;
			this.roles = ROLE_LEAD;
		}
		
		protected void toPom(StringBuilder buf, String indent) {
			element("developer", indent, buf,
					subElement("name", this.name),
					getRolesElement());
		}

		String getRolesElement() {
			StringBuilder rolesElement = new StringBuilder();
			element("roles", "", rolesElement, String.join("\n", getRoleElements()));
			return rolesElement.toString();
		}

		String[] getRoleElements() {
			String[] roleElements = new String[this.roles.length];
			for (int i = 0; i < this.roles.length; i++) {
				StringBuilder subBuf = new StringBuilder();
				element("role", "", subBuf, this.roles[i]);
				roleElements[i] = subBuf.toString();
			}
			return roleElements;
		}
	}

	/** Represent developers using the "Who's Involved" web page. */
	static class UrlDeveloper extends Developer {
		String url;
		public UrlDeveloper(String url) {
			this.url = url;
		}
		
		protected void toPom(StringBuilder buf, String indent) {
			element("developer", indent, buf,
					subElement("url", this.url));
		}
	}
}
