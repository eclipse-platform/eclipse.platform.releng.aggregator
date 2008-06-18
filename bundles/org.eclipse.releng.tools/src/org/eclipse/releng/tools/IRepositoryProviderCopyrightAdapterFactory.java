/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterManager;

/**
 * This interface supports the creation of a {@link RepositoryProviderCopyrightAdapter}
 * that can be used by the {@link AdvancedFixCopyrightAction} to determine the last modified year
 * for a set of files. It should be obtained by adapting the repository provider type to an instance
 * of this interface using the {@link IAdapterManager}.
 *
 */
public interface IRepositoryProviderCopyrightAdapterFactory {

	/**
	 * Create an adapter for the given set of resources
	 * @param resources the resources
	 * @return an adapter
	 */
	public RepositoryProviderCopyrightAdapter createAdapater(IResource[] resources);
}
