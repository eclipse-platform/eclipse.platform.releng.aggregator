/*******************************************************************************
 * Copyright (c) 2014 IBH SYSTEMS GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.releng.tools.AdvancedFixCopyrightAction.FixConsole;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

public class RemoveAllConsolesAction extends Action {
	public RemoveAllConsolesAction() {
		super(
				Messages.getString("RemoveAllConsolesAction.Text"), RelEngPlugin.imageDescriptorFromPlugin(RelEngPlugin.ID, "icons/full/elcl16/console_remall.png")); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public void run() {
		List consolesList = new ArrayList();

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
			consoles[i] = (IConsole) consolesList.get(i);
		}

		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(consoles);
	}
}
