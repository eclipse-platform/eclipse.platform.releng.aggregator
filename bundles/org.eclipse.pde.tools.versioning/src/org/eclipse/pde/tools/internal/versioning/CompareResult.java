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
package org.eclipse.pde.tools.internal.versioning;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.pde.tools.versioning.ICompareResult;

/**
 *	This class represents compare result of a plugin or class
 */
public class CompareResult implements ICompareResult {
	int change;
	MultiStatus status;

	/**
	 * constructor
	 * 
	 * @param change change on a feature or plugin
	 * @param status contains messages created when verify a plugin or class
	 */
	public CompareResult(int change, MultiStatus status) {
		this.change = change;
		this.status = status;
	}

	/**
	 * gets status which contains messages created when verify a plugin or class
	 * @return MultiStatus instance 
	 */
	public MultiStatus getResultStatus() {
		return this.status;
	}

	/**
	 * get change on a plugin or class
	 * @return change
	 */
	public int getChange() {
		return this.change;
	}
}
