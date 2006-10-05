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

import org.eclipse.core.runtime.MultiStatus;

/**
 * This interface provides methods to access to compare result information of a plugin or class
 * <p>
 * <b>Note:</b> This interface should not be implemented by clients.
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface ICompareResult {
	/**
	 * gets MuliStatus instance which contains messages created when verify a plugin or class
	 * @return MultiStatus instance 
	 */
	public MultiStatus getResultStatus();

	/**
	 * gets overall change happened on a plugin or class
	 * <p>
	 * The value of change is an int number of the following:
	 * <ul>
	 * <li>{@link IVersionCompare#ERROR_OCCURRED}</li>
	 * <li>{@link IVersionCompare#MAJOR_CHANGE}</li>
	 * <li>{@link IVersionCompare#MINOR_CHANGE}</li>
	 * <li>{@link IVersionCompare#NEW_ADDED}</li>
	 * <li>{@link IVersionCompare#NO_LONGER_EXIST}</li>
	 * <li>{@link IVersionCompare#MICRO_CHANGE}</li>
	 * <li>{@link IVersionCompare#QUALIFIER_CHANGE}</li>
	 * <li>{@link IVersionCompare#NO_CHANGE}</li>
	 * </ul>
	 * </p>
	 * @return change int number which indicates the overall change happened on a plugin or class
	 * @see IVersionCompare#ERROR_OCCURRED
	 * @see IVersionCompare#MAJOR_CHANGE
	 * @see IVersionCompare#MINOR_CHANGE
	 * @see IVersionCompare#NEW_ADDED
	 * @see IVersionCompare#NO_LONGER_EXIST
	 * @see IVersionCompare#MICRO_CHANGE
	 * @see IVersionCompare#QUALIFIER_CHANGE
	 * @see IVersionCompare#NO_CHANGE
	 */
	public int getChange();
}
