/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;


import java.io.IOException;

/**
 * An application that launches tests using the EclipseTestRunner.
 */
public class CoreTestApplication {
	
	/**
	 * Runs a set of tests as defined by the given command line args.
	 */
	public Object run(String[] args) throws Exception {
		return new Integer(runTests(args));
	}

	protected int runTests(String[] args) throws IOException {
		return EclipseTestRunner.run(args);
	}
}
