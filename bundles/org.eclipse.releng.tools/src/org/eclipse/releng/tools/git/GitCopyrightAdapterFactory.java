/*******************************************************************************
 * Copyright (c) 2010, 2016 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.releng.tools.git;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.releng.tools.IRepositoryProviderCopyrightAdapterFactory;
import org.eclipse.releng.tools.RepositoryProviderCopyrightAdapter;
import org.eclipse.team.core.RepositoryProviderType;

public class GitCopyrightAdapterFactory implements IAdapterFactory,
		IRepositoryProviderCopyrightAdapterFactory {

	private static final Class<?>[] ADAPTER_LIST = new Class[] { IRepositoryProviderCopyrightAdapterFactory.class };

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IRepositoryProviderCopyrightAdapterFactory.class
				.equals(adapterType)) {
			return adapterType.cast(getGitCopyrightAdapter(adaptableObject));
		}
		return null;
	}

	private Object getGitCopyrightAdapter(Object adaptableObject) {
		if (!(adaptableObject instanceof RepositoryProviderType))
			return null;
		return this;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTER_LIST;
	}

	@Override
	public RepositoryProviderCopyrightAdapter createAdapater(
			IResource[] resources) {
		return new GitCopyrightAdapter(resources);
	}
}
