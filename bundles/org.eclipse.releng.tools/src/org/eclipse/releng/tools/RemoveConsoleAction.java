/*******************************************************************************
 * Copyright (c) 2014, 2016 IBH SYSTEMS GmbH.
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
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class RemoveConsoleAction extends Action {
	
	private final IConsole console;
	
	public RemoveConsoleAction(IConsole console) {
		super(Messages.getString("RemoveConsoleAction.Text"), AbstractUIPlugin.imageDescriptorFromPlugin(RelEngPlugin.ID, "icons/full/elcl16/console_rem.png"));  //$NON-NLS-1$//$NON-NLS-2$
		this.console = console;
	}
	
	@Override
	public void run() {
		ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{console});
	}
}
