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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

public class RemoveConsoleAction extends Action {

	private final IConsole console;

	public RemoveConsoleAction(IConsole console) {
		super(Messages.getString("RemoveConsoleAction.Text")); //$NON-NLS-1$
		ResourceLocator.imageDescriptorFromBundle(RelEngPlugin.ID, "icons/full/elcl16/console_rem.png").ifPresent(d-> setImageDescriptor(d)); //$NON-NLS-1$
		this.console = console;
	}

	@Override
	public void run() {
		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{console});
	}
}
