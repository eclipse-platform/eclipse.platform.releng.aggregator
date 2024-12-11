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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

// Manual demonstration:
public class SwtLeakTestExtensionTest {
	static Shell leakedShell;
	@Test
	public void testNoLeak() {
		System.out.println("noleak");
		IWorkbench workbench = PlatformUI.getWorkbench();
		Display display = workbench.getDisplay();
		Shell shell = new Shell(display);
		shell.dispose();
	}

	@Test
	public void testLeak() { // fails
		System.out.println("leak");
		IWorkbench workbench = PlatformUI.getWorkbench();
		Display display = workbench.getDisplay();
		leakedShell = new Shell(display);
	}

	@AfterAll
	public static void afterAll() {
		leakedShell.dispose();
	}

}
