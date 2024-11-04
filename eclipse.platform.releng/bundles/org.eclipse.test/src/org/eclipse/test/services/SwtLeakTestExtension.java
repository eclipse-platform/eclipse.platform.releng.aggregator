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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/** logs start, stop, duration, error for all tests executed **/
public class SwtLeakTestExtension implements AfterTestExecutionCallback, BeforeTestExecutionCallback {
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		IWorkbench workbench = PlatformUI.getWorkbench();
		Set<Shell> preExistingShells = Set.of(workbench.getDisplay().getShells());

		getStore(context).put("workbench", workbench);
		getStore(context).put("preExistingShells", preExistingShells);
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		IWorkbench workbench = getStore(context).remove("workbench", IWorkbench.class);
		Set<Shell> preExistingShells = getStore(context).remove("preExistingShells", Set.class);
		// Check for shell leak.
		List<String> leakedModalShellTitles = new ArrayList<>();
		Shell[] shells = workbench.getDisplay().getShells();
		for (Shell shell : shells) {
			if (!shell.isDisposed() && !preExistingShells.contains(shell)) {
				leakedModalShellTitles.add(shell.getText());
				shell.close();
			}
		}
		assertEquals("Test leaked modal shell: [" + String.join(", ", leakedModalShellTitles) + "]", 0,
				leakedModalShellTitles.size());
	}

	private Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
	}

}
