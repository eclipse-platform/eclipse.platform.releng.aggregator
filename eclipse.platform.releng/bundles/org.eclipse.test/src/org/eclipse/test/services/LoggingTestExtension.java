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

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/** logs start, stop, duration, error for all tests executed **/
public class LoggingTestExtension implements AfterTestExecutionCallback, BeforeTestExecutionCallback {
	public static String BEFORE_TEST_START = "Test Before: ";
	public static String AFTER_TEST_PASSED = "Test Passed: ";
	public static String AFTER_TEST_FAILED = "Test Failed: ";

	public LoggingTestExtension() {
		System.out.println("LoggingTestService");
	}
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		long n0 = System.nanoTime();
		getStore(context).put("TIME", n0);
		System.out.println(BEFORE_TEST_START + context.getDisplayName());
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		long t1 = System.nanoTime();
		long t0 = getStore(context).remove("TIME", long.class);
		String took = " after: " + (t1 - t0) / 1_000_000 + "ms";
		Throwable t = context.getExecutionException().orElse(null);
		if (t == null) {
			System.out.println(AFTER_TEST_PASSED + context.getDisplayName() + took);
		} else {
			System.out.println(AFTER_TEST_FAILED + context.getDisplayName() + took);
			t.printStackTrace(System.out);
		}
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
	}

}
