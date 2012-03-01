/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Test suite with user-specified test order. Fails if not all test methods are
 * listed.
 * 
 * <p>
 * <b>Background:</b> {@link java.lang.Class#getDeclaredMethods()} does not
 * specify the order of the methods. Up to JavaSE 6, the methods were usually
 * sorted in declaration order, but in JavaSE 7, the order is random. This class
 * guarantees reliable test execution order.
 * </p>
 * 
 * @since 3.8
 */
public class OrderedTestSuite extends TestSuite {

	/**
	 * Creates a new ordered test suite that runs tests in the specified execution order.
	 * 
	 * @param testClass the JUnit-3-style test class 
	 * @param testMethods the names of all test methods in the expected execution order
	 */
	public OrderedTestSuite(final Class testClass, String[] testMethods) {
		super(testClass.getName());

		Set existingMethods= new HashSet();
		Method[] methods= testClass.getMethods(); // just public member methods
		for (int i= 0; i < methods.length; i++) {
			Method method= methods[i];
			existingMethods.add(method.getName());
		}

		for (int i= 0; i < testMethods.length; i++) {
			final String testMethod= testMethods[i];
			if (existingMethods.remove(testMethod)) {
				addTest(createTest(testClass, testMethod));
			} else {
				addTest(error(testClass, testMethod, new IllegalArgumentException(
						"Class '" + testClass.getName() + " misses test method '" + testMethod + "'."))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		for (Iterator iter= existingMethods.iterator(); iter.hasNext();) {
			String existingMethod= (String) iter.next();
			if (existingMethod.startsWith("test")) { //$NON-NLS-1$
				addTest(error(testClass, existingMethod, new IllegalArgumentException(
						"Test method '" + existingMethod + "' not listed in OrderedTestSuite of class '" + testClass.getName() + "'."))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

	}

	private static Test error(Class testClass, String testMethod, Exception exception) {
		final Throwable e2= exception.fillInStackTrace();
		return new TestCase(testMethod + "(" + testClass.getName() + ")") { //$NON-NLS-1$ //$NON-NLS-2$
			protected void runTest() throws Throwable {
				throw e2;
			}
		};
	}
}
