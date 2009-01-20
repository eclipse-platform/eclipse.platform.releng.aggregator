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

import junit.framework.JUnit4TestAdapter;

import junit.framework.Test;
import org.eclipse.test.EclipseTestRunner.TestFailedException;

/**
 * Helper class for EclipseTestRunner.  Returns at Test object that is compatible with JUnit 4.x.
 */
public class EclipseJunit4TestHelper {
	/**
	 * Returns the Test corresponding to the given test class. 
	 */
	protected static Test getTest(EclipseTestRunner runner, String testClassName) throws TestFailedException {
		if (testClassName.length() <= 0) {
			return null;
		}
		Class testClass = null;
		try {
			testClass = runner.loadSuiteClass(testClassName);
		} catch (ClassNotFoundException e) {
			if (e.getCause() != null) {
				runner.runFailed(e.getCause());
			}
			String clazz = e.getMessage();
			if (clazz == null)
				clazz = testClassName;
			runner.runFailed("Class not found \"" + clazz + "\"");
			return null;
		} catch (Exception e) {
			runner.runFailed(e);
			return null;
		}
		return new JUnit4TestAdapter(testClass);
	}

}
