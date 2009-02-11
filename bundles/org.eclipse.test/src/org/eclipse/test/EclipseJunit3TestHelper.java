/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.lang.reflect.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.test.EclipseTestRunner.TestFailedException;

/**
 * Helper class for EclipseTestRunner.  Returns at Test object that is compatible with JUnit 3.x.
 */
public class EclipseJunit3TestHelper {
	
	/**
	 * Returns the Test corresponding to the given suite. 
	 */
	protected static Test getTest(EclipseTestRunner runner, String suiteClassName) throws TestFailedException {
		if (suiteClassName.length() <= 0) {
			return null;
		}
		Class testClass = null;
		try {
			testClass = runner.loadSuiteClass(suiteClassName);
		} catch (ClassNotFoundException e) {
			if (e.getCause() != null) {
				runner.runFailed(e.getCause());
			}
			String clazz = e.getMessage();
			if (clazz == null)
				clazz = suiteClassName;
			runner.runFailed("Class not found \"" + clazz + "\"");
			return null;
		} catch (Exception e) {
			runner.runFailed(e);
			return null;
		}
		Method suiteMethod = null;
		try {
			suiteMethod = testClass.getMethod(EclipseTestRunner.SUITE_METHODNAME, new Class[0]);
		} catch (Exception e) {
			// try to extract a test suite automatically
			return new TestSuite(testClass);
		}
		if (!Modifier.isStatic(suiteMethod.getModifiers())) {
			runner.runFailed("suite() method must be static");
			return null;
		}
		Test test = null;
		try {
			test = (Test) suiteMethod.invoke(null, new Class[0]);
			if (test == null)
				return test;
		} catch (InvocationTargetException e) {
			runner.runFailed("Failed to invoke suite():" + e.getTargetException().toString());
			return null;
		} catch (IllegalAccessException e) {
			runner.runFailed("Failed to invoke suite():" + e.toString());
			return null;
		}
		return test;
	}
}
