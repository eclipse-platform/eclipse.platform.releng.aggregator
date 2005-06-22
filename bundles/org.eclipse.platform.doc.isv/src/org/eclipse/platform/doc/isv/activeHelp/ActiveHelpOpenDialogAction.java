/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.platform.doc.isv.activeHelp;

import org.eclipse.help.ILiveHelpAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Sample Active Help action.  This is the source code for the sample
 * active help action that is shown in the Platform Plug-in Developer Guide
 * in the Programmer's Guide.  This class is marked public because
 * it must be called from the active help servlet.  However, this class
 * is not intended to be used as API and should not be called by clients.
 */
public class ActiveHelpOpenDialogAction implements ILiveHelpAction {

     public void setInitializationString(String data) {
          // ignore the data.  We do not use any javascript parameters.
     }

     public void run() {
          // Active help does not run on the UI thread, so we must use syncExec
          Display.getDefault().syncExec(new Runnable() {
               public void run() {
                    IWorkbenchWindow window =
                         PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window != null) {
                         // Bring the Workbench window to the top of other windows;
                         // On some Windows systems, it will only flash the Workbench
                         // icon on the task bar
                         Shell shell = window.getShell();
                         shell.setMinimized(false);
                         shell.forceActive();
                         // Open a message dialog
                         MessageDialog.openInformation(
                              window.getShell(),
                              "Hello World.", //$NON-NLS-1$
                              "Hello World."); //$NON-NLS-1$
                    }
               }
          });
     }
}
