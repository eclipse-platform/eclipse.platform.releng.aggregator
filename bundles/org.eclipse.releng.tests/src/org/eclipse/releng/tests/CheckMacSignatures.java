/*******************************************************************************
 * Copyright (c) 2016, 2018 David Williams and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Williams - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tests;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class CheckMacSignatures {

	private boolean runningOnMac;
	private String  eclipseInstall;

	public CheckMacSignatures() {

	}

	@Before
	public void checkIfOnMac() {
		String os = System.getProperty("osgi.os");
		if ("macosx".equals(os)) {
			runningOnMac = true;
			eclipseInstall = System.getProperty("eclipse.install.location");
		}
		// temp
		System.out.println("eclipse.home: " + System.getProperty("eclipse.home"));
		System.out.println("eclipse.home.location: " + System.getProperty("eclipse.home.location"));
		System.out.println("All properties");
		Properties allProperties = System.getProperties();
		allProperties.list(System.out);
	}

	@Test
	public void checkSignature() {
		if (!runningOnMac) {
			System.out.println("Not running on Mac. No need to check Mac signature");
		} else {
			System.out.println("Eclipse Install location: " + eclipseInstall);
		}
	}
}
