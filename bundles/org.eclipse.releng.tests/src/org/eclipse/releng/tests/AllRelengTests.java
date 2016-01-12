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
