/*******************************************************************************
 * Copyright (c) 2024 Joerg Kubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.services;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** verifies the Extension is used **/
public class LoggingTestExtensionTest {
	private static final String SOMETHING = "something";
	private static final ByteArrayOutputStream OUT = new ByteArrayOutputStream();
	private static PrintStream oldStdOut;

	@BeforeAll
	public static void beforeAll() {
		oldStdOut = System.out;
		System.setOut(new PrintStream(OUT));
	}
	@Test
	public void testWritten() {
		System.out.println(SOMETHING);
	}

	@AfterAll
	public static void afterAll() {
		String output = new String(OUT.toByteArray());
		System.setOut(oldStdOut);
		assertTrue(output, output.contains("testWritten"));
		assertTrue(output, output.contains(SOMETHING));
		assertTrue(output, output.contains(org.eclipse.test.services.LoggingTestExtension.BEFORE_TEST_START));
		assertTrue(output, output.contains(org.eclipse.test.services.LoggingTestExtension.AFTER_TEST_PASSED));
	}
}
