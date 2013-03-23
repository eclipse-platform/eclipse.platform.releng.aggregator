/*******************************************************************************
 * Copyright (c) 2013 Tomasz Zarna and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tomasz Zarna <tzarna@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllRelengTests extends TestSuite {
	public static Test suite() {
		return new AllRelengTests();
	}

	public AllRelengTests() {
		addTestSuite(BuildTests.class);
		if (isJGitAvailable()) {
			addTestSuite(GitCopyrightAdapterTest.class);
		}
	}

	private boolean isJGitAvailable() {
		try {
			Class.forName("org.eclipse.jgit.api.Git");
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}
}
