/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.releng.tools;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.releng.internal.tools.pomversion.IPomVersionConstants;
import org.eclipse.releng.internal.tools.pomversion.PomVersionErrorReporter;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * A Plugin for performing certain important RelEng tasks.
 * <p>
 * Currently this Plugin provides support for:
 *    <li>RelEng Map File Validator
 *    <li>Builder and associated project nature
 *    <li>Fix Copyright action
 */
public class RelEngPlugin extends AbstractUIPlugin {

	public static final String ID = "org.eclipse.releng.tools"; //$NON-NLS-1$
	public static final String MAP_PROJECT_NAME = Messages.getString("RelEngPlugin.1"); //$NON-NLS-1$
	public static final String MAP_FOLDER = Messages.getString("RelEngPlugin.2"); //$NON-NLS-1$
	private static final String BINARY_REPOSITORY_PROVIDER_CLASS_NAME= "org.eclipse.pde.internal.core.BinaryRepositoryProvider"; //$NON-NLS-1$

	private PomVersionErrorReporter fPomReporter = new PomVersionErrorReporter();


	//The shared instance.
	private static RelEngPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;

	/**
	 * The constructor.
	 */
	public RelEngPlugin() {
		plugin = this;
		try {
			resourceBundle= ResourceBundle.getBundle(ID + Messages.getString("RelEngPlugin.3")); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ID);
		if(node != null) {
			node.addPreferenceChangeListener(fPomReporter);
			String severity = getPreferenceStore().getString(IPomVersionConstants.POM_VERSION_ERROR_LEVEL);
			if(!IPomVersionConstants.VALUE_IGNORE.equals(severity)) {
				ResourcesPlugin.getWorkspace().addResourceChangeListener(fPomReporter, IResourceChangeEvent.POST_BUILD);
				int workspaceValidated= node.getInt(IPomVersionConstants.WORKSPACE_VALIDATED, 0);
				if (workspaceValidated < PomVersionErrorReporter.VERSION) {
					new WorkspaceJob(Messages.getString("RelEngPlugin.0")) { //$NON-NLS-1$
						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
							fPomReporter.validateWorkspace();
							return Status.OK_STATUS;
						}
					}.schedule();
				}
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ID);
		if(node != null) {
			node.removePreferenceChangeListener(fPomReporter);
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fPomReporter);
		}
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static RelEngPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= RelEngPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	/**
	 * Convenience method for logging CoreExceptions to the plugin log
	 */
	public static void log(CoreException e) {
		log(e.getStatus().getSeverity(), e.getMessage(), e);
	}

	/**
	 * Log the given exception along with the provided message and severity indicator
	 */
	public static void log(int severity, String message, Throwable e) {
		log(new Status(severity, ID, 0, message, e));
	}

	/**
	 * Log the given exception as an error.
	 *
	 * @param e exception to log
	 */
	public static void log(Throwable e){
		log(new Status(IStatus.ERROR, ID, 0, e.getMessage(), e));
	}

	/**
	 * Log the given status. Do not use this method for the IStatus from a CoreException.
	 * Use<code>log(CoreException)</code> instead so the stack trace is not lost.
	 */
	public static void log(IStatus status) {
		getPlugin().getLog().log(status);
	}
	/**
	 * Returns the singleton plug-in instance.
	 *
	 * @return the plugin instance
	 */
	public static RelEngPlugin getPlugin() {
		// If the instance has not been initialized, we will wait.
		// This can occur if multiple threads try to load the plugin at the same
		// time (see bug 33825: http://bugs.eclipse.org/bugs/show_bug.cgi?id=33825)
		while (plugin == null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore and keep trying
			}
		}
		return plugin;
	}

	/**
	 * Tells whether the given project is shared.
	 *
	 * @param project the project
	 * @return <code>true</code> if the project is shared
	 * @since 3.7
	 */
	static boolean isShared(IProject project) {
		Assert.isLegal(project != null);
		if (!RepositoryProvider.isShared(project))
			return false;

		// Check for PDE's binary projects that also connect a provider to the project
		RepositoryProvider provider= RepositoryProvider.getProvider(project);
		return provider != null && !BINARY_REPOSITORY_PROVIDER_CLASS_NAME.equals(provider.getClass().getName());
	}

}
