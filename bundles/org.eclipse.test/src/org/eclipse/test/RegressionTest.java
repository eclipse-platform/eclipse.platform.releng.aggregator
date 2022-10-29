/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

/**
 * Check the output of several tests for regressions.
 */
public class RegressionTest {

	private final String oldFilename, newFilename, outFilename;

	/**
	 * Constructor for RegressionTest
	 */
	public RegressionTest(String oldFilename, String newFilename, String outFilename) {
		this.oldFilename = oldFilename;
		this.newFilename = newFilename;
		this.outFilename = outFilename;
	}

	public static void main(String[] argv) {
		if (argv.length < 3) {
			System.err.println("Error: too few arguments");
			System.err.println("Usage: (progname) oldfile newfile outfile");
		} else {
			// ASSERT: The program has at least the correct number of arguments
			RegressionTest rt = new RegressionTest(argv[0], argv[1], argv[2]);
			rt.testRegressions();
		}
	}

	/**
	 * Test for regressions in the test suite.
	 */
	public void testRegressions() {
		// Read the old and new files
		try (PrintStream output = new PrintStream(outFilename)) {
			String oldPass = Files.readString(Path.of(oldFilename));
			String newPass = Files.readString(Path.of(newFilename));
			parse(oldPass, newPass, output);
		} catch (Exception e) {
			System.err.println("Error opening file");
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	private void parse(String oldPass, String newPass, PrintStream output) {
		// Establish their relationship
		StringTokenizer oldst = new StringTokenizer(oldPass);
		StringTokenizer newst = new StringTokenizer(newPass);

		String[] oldTest = nextTest(oldst);
		String[] newTest = nextTest(newst);

		boolean nothingChanged = true;
		while (oldTest != null && newTest != null) {

			// Compare the two test names
			int compareName = oldTest[0].compareTo(newTest[0]);
			if (compareName == 0) {
				int compareStatus = oldTest[1].compareTo(newTest[1]);
				if (compareStatus != 0) {
					nothingChanged = false;
					output.println(testChanged(newTest));
				}
				oldTest = nextTest(oldst);
				newTest = nextTest(newst);
			} else if (compareName < 0) {
				// oldTestName comes first
				output.println(testNotRun(oldTest));
				oldTest = nextTest(oldst);
				nothingChanged = false;
			} else {
				// newTestName comes first
				output.println(testAdded(newTest));
				newTest = nextTest(newst);
				nothingChanged = false;
			}
		}
		// Make sure all tests are parsed
		while (oldTest != null) {
			// oldTestName comes first
			output.println(testNotRun(oldTest));
			oldTest = nextTest(oldst);
			nothingChanged = false;
		}
		while (newTest != null) {
			// newTestName comes first
			output.println(testAdded(newTest));
			newTest = nextTest(newst);
			nothingChanged = false;
		}
		// Make sure that there is always some output.
		if (nothingChanged) {
			output.println("All tests unchanged.");
		}
	}

	/**
	 * Get the message for when a test is not run.
	 */
	static String testNotRun(String[] test) {
		return "Not run: " + test[0];
	}

	/**
	 * Get the message for when a test's status changes.
	 */
	static String testChanged(String[] test) {
		return "Changed: " + test[0] + ", " + test[1];
	}

	/**
	 * Get the message for when a test is added.
	 */
	static String testAdded(String[] test) {
		return "New test: " + test[0] + ", Status: " + test[1];
	}

	/**
	 * Returns the next 2 tokens in st, if they exist. Returns null if they do not.
	 */
	static String[] nextTest(StringTokenizer st) {
		String[] test = new String[2];
		if (st.hasMoreTokens()) {
			test[0] = st.nextToken();
		} else {
			return null;
		}
		if (st.hasMoreTokens()) {
			test[1] = st.nextToken();
		} else {
			return null;
		}
		return test;
	}
}
