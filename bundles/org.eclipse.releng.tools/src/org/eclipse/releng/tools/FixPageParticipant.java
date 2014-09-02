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

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class FixPageParticipant implements IConsolePageParticipant {

	public Object getAdapter(Class adapter) {
		return null;
	}

	public void init(IPageBookViewPage page, IConsole console) {
		IActionBars actionBars = page.getSite().getActionBars();
		actionBars.getToolBarManager().appendToGroup(
				IConsoleConstants.LAUNCH_GROUP,
				new RemoveConsoleAction(console));
		actionBars.getToolBarManager().appendToGroup(
				IConsoleConstants.LAUNCH_GROUP, new RemoveAllConsolesAction());
	}

	public void dispose() {
	}

	public void activated() {
	}

	public void deactivated() {
	}

}
