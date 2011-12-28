/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.versioning;

import org.eclipse.pde.tools.internal.versioning.VersionCompareDispatcher;

/**
 * Factory class to create objects used for comparing versions.
 * <p>
 * This class is not intended to be sub-classed by clients.
 * </p>
 */
public final class VersionCompareFactory {

	/**
	 * Constructor
	 */
	public VersionCompareFactory() {
		super();
	}

	/**
	 * Return an instance of a class which can be used for version comparison.
	 * 
	 * @return version compare class
	 */
	public IVersionCompare getVersionCompare() {
		return new VersionCompareDispatcher();
	}

}
