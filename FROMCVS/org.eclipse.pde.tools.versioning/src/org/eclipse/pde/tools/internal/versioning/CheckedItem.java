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

import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.osgi.framework.Version;

/**
 * This class represents version change of a checked feature(or plugin)
 */
public class CheckedItem {
	/**
	 * compare source feature or plugin key(id + "#" + version)
	 */
	private String sourceKey;
	/**
	 * compare destination feature or plugin key(id + "#" + version)
	 * for plugin, this value must be set when create a new instance of this class, 
	 * since it is possible there are two plugins have the same id but different versions exist
	 * in an Eclipse installation;
	 * for feature, this value can be set as <code>null</code>
	 */
	private String destinationKey;
	private Version version;
	private int change;

	/**
	 * Constructor for the class. Set the key, version, and change to be the given values.
	 * 
	 * @param sourceKey compare source feature id(or plugin id) + "#" + feature version(or plugin version)
	 * @param destinationKey compare destination feature id(or plugin id) + "#" + feature version(or plugin version)
	 * @param version if the new version is correct, it is the new version; if the new version
	 *                is incorrect, it is the recommended version; if some error happened, it is <code>null</code>
	 * @param change change happened on the feature or plugin
	 */
	public CheckedItem(String sourceKey, String destinationKey, Version version, int change) {
		this.sourceKey = sourceKey;
		this.destinationKey = destinationKey;
		this.version = version;
		this.change = change;
	}

	/**
	 * Return the compare source feature(or plugin) key.
	 * 
	 * @return feature(or plugin) key, feature id(or plugin id) + "#" + feature version(or plugin version)
	 */
	public String getSourceKey() {
		return this.sourceKey;
	}
	
	/**
	 * Return the compare destination feature(or plugin) key.
	 * 
	 * @return feature(or plugin) key, feature id(or plugin id) + "#" + feature version(or plugin version)
	 * 	       it can be <code>null</code> if the instance of this class represents a compare result of features
	 */
	public String getDestinationKey() {
		return this.destinationKey;
	}

	/**
	 * Return the compare result's version. If the new version is correct, it is the 
	 * new version; if the new version is incorrect, it is the recommended 
	 * version; if some error happened, it is <code>null</code>.
	 * 
	 * @return version the version or <code>null</code>
	 */
	public Version getVersion() {
		return this.version;
	}

	/**
	 * returns change on the feature or plugin
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
	public int getChange() {
		return this.change;
	}
}
