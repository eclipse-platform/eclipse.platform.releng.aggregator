/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.RepositoryProviderType;

public class CVSCopyrightAdapterFactory implements IAdapterFactory,
		IRepositoryProviderCopyrightAdapterFactory {

	private static final Class[] ADAPTER_LIST = new Class[] { IRepositoryProviderCopyrightAdapterFactory.class };

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (IRepositoryProviderCopyrightAdapterFactory.class
				.equals(adapterType)) {
			return getCVSCopyrightAdapter(adaptableObject);
		}
		return null;
	}

	private Object getCVSCopyrightAdapter(Object adaptableObject) {
		if (!(adaptableObject instanceof RepositoryProviderType))
			return null;
		return this;
	}

	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

	public RepositoryProviderCopyrightAdapter createAdapater(
			IResource[] resources) {
		return new CVSCopyrightAdapter(resources);
	}
}
