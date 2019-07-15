/*******************************************************************************
 * Copyright (c) 2014, 2019 IBH SYSTEMS GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 548799
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.releng.tools.AdvancedFixCopyrightAction.FixConsole;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

public class RemoveAllConsolesAction extends Action {
	public RemoveAllConsolesAction() {
		super(Messages.getString("RemoveAllConsolesAction.Text")); //$NON-NLS-1$
		ResourceLocator.imageDescriptorFromBundle(RelEngPlugin.ID, "icons/full/elcl16/console_remall.png").ifPresent(d-> setImageDescriptor(d)); //$NON-NLS-1$
	}

	@Override
	public void run() {
		List<IConsole> consolesList = new ArrayList<>();

		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager()
				.getConsoles()) {
			if (console instanceof FixConsole) {
				consolesList.add(console);
			}
		}

		if (consolesList.isEmpty())
			return;

		IConsole[] consoles = new IConsole[consolesList.size()];
		for (int i = 0; i < consoles.length; i++) {
			consoles[i] = consolesList.get(i);
		}

		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(consoles);
	}
}
