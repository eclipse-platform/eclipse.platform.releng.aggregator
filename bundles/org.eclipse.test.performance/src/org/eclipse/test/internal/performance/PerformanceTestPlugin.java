/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Plugin;


/**
 * @since 3.1
 */
public class PerformanceTestPlugin extends Plugin {

	/**
	 * The plug-in id
	 */
    public static final String PLUGIN_ID= "org.eclipse.test.performance"; //$NON-NLS-1$
    
	/**
	 * The shared instance.
	 */
	private static PerformanceTestPlugin fgPlugin;

	/**
	 * The constructor.
	 */
	public PerformanceTestPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static PerformanceTestPlugin getDefault() {
		return fgPlugin;
	}
}
