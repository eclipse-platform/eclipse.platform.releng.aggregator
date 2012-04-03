/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;


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

	/**
	 * Returns the shared instance.
	 */
	public static RelEngPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
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

	/**
	 * The following code is a sample of how to assign a
	 * RelEng nature to a project.  This only ever needed
	 * to be done once to set up org.eclipse.releng project
	 * with the desired nature
	 */
//	private void assignNature() {
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IWorkspaceRoot workspaceRoot = workspace.getRoot();
//		IProject aProject = workspaceRoot.getProject();
//		
//		
//		try {
//			IProjectDescription description;
//			description = aProject.getDescription();
//			String[] natures = description.getNatureIds();
//			String[] newNatures = new String[natures.length + 1];
//			System.arraycopy(natures, 0, newNatures, 0, natures.length);
//			newNatures[natures.length] = "org.eclipse.releng.tools.relEngNature";
//			description.setNatureIds(newNatures);
//			aProject.setDescription(description, null);
//		} catch (CoreException e) {
//			System.out.println("Failed to set nature");
//			e.printStackTrace();
//		}
//		
//	}
}
