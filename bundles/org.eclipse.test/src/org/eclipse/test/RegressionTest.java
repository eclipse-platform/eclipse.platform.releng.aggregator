/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.io.ByteArrayOutputStream;

/**
 * Check the output of several tests for regressions.
 */
public class RegressionTest {

	PrintStream output;
	String oldFilename, newFilename, outFilename;
	public static final String NOTHING_CHANGED_MSG
		= "All tests unchanged.";	
	/**
	 * Constructor for RegressionTest
	 */
	public RegressionTest(	String oldFilename,
							String newFilename,
							String outFilename) {
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
		String oldPass = "";
		String newPass = "";
		try {
			oldPass = readFile(oldFilename);
			newPass = readFile(newFilename);
		} catch (Exception e) {
			System.err.println("Error opening input file");
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try {
			output = new PrintStream(
						new BufferedOutputStream(
							new FileOutputStream(
								new File(outFilename))));
		} catch (Exception e) {
			System.err.println("Error opening output file");
			System.err.println(e.getMessage());
			System.exit(-1);
		}
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
			output.println(NOTHING_CHANGED_MSG);
		}
		output.close();
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
	 * Read the file given by s, and return its contents.
	 */
	static String readFile(String s) throws IOException {
		byte[] buf = new byte[8192];
		FileInputStream r = new FileInputStream(s);
		ByteArrayOutputStream aStream = new ByteArrayOutputStream();
		int n;
		while ((n = r.read(buf)) != -1) {
			aStream.write(buf, 0, n);
		}
		r.close();
		return aStream.toString();
	}
	
	
	/**
	 * Returns the next 2 tokens in st, if they exist.
	 * Returns null if they do not.
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

