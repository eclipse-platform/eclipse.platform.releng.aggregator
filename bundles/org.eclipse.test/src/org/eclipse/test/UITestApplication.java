/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;

/**
 * A Workbench that runs a test suite specified in the command line arguments.
 */
public class UITestApplication  implements ITestHarness, IApplication {

	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	private TestableObject fTestableObject;
	int fTestRunnerResult = -1;
	private IApplicationContext appContext;

	public Object run(final Object args) throws Exception {
		// Get the application to test
		Object application = getApplication((String[])args);
		assertNotNull(application);

		fTestableObject = PlatformUI.getTestableObject();
		fTestableObject.setTestHarness(this);
		((IApplication) application).start(appContext);
		return Integer.valueOf(fTestRunnerResult);
	}

	/*
	 * return the application to run, or null if not even the default application
	 * is found.
	 */
	private Object getApplication(String[] args) throws CoreException {
		// Assume we are in 3.0 mode.
		// Find the name of the application as specified by the PDE JUnit launcher.
		// If no application is specified, the 3.0 default workbench application
		// is returned.
		IExtension extension =
				Platform.getExtensionRegistry().getExtension(
						Platform.PI_RUNTIME,
						Platform.PT_APPLICATIONS,
						getApplicationToRun(args));

		assertNotNull(extension);

		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (runnable instanceof IApplication)
					return runnable;
			}
		}
		return null;
	}

	/**
	 * The -testApplication argument specifies the application to be run.
	 * If the PDE JUnit launcher did not set this argument, then return
	 * the name of the default application.
	 * In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
	 *
	 */
	private String getApplicationToRun(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-testApplication") && i < args.length -1) //$NON-NLS-1$
				return args[i+1];
		}
		return DEFAULT_APP_3_0;
	}

	@Override
	public void runTests() {
		fTestableObject.testingStarting();
		fTestableObject.runTest(() -> {
			try {
				fTestRunnerResult = EclipseTestRunner.run(Platform.getCommandLineArgs());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		fTestableObject.testingFinished();
	}


	@Override
	public Object start(IApplicationContext context) throws Exception {
		this.appContext = context;
		String[] args = (String[]) appContext.getArguments().get("application.args");
		if (args == null)
			args = new String[0];
		return run(args);
	}

	@Override
	public void stop() {

	}

}

