/*******************************************************************************
 * Copyright (c) 2013 Tomasz Zarna and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tomasz Zarna <tzarna@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class AllRelengTests {

	public static TestSuite suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new JUnit4TestAdapter(BuildTests.class));
		if (isJGitAvailable())
			suite.addTest(new JUnit4TestAdapter(GitCopyrightAdapterTest.class));
		return suite;
	}

	private static boolean isJGitAvailable() {
		try {
			Class.forName("org.eclipse.jgit.api.Git");
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}
}
