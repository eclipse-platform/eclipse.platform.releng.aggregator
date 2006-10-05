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

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

/**
 * Helper class to manipulate manifest files.
 */
public class ManifestHelper implements VersionCompareConstants {

	/**
	 * gets attributes from plugin.xml file in a plugin jar or directory denoted by <code>file</code>
	 * @param file File instance which denotes a plugin jar or directory
	 * @return Map which contains attributes of plugin.xml
	 */
	private static Map getAttributesFromPluginManifest(File file) {
		PluginConverter pluginConverter = Activator.getPluginConverter();
		if (pluginConverter == null) {
			// TODO log and return
			// throw new CoreException(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING, Messages.PluginVersionCompare_noPluginConverterInstanceMsg, null));
			return null;
		}
		Dictionary pluginXML = null;
		try {
			pluginXML = pluginConverter.convertManifest(file, true, null, true, null);
		} catch (PluginConversionException pce) {
			// TODO log return
			// throw new CoreException(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING, NLS.bind(Messages.PluginVersionCompare_couldNotConvertManifestMsg, file.getAbsoluteFile()), pce));
			return null;
		}
		// we need to convert the dictionary into a map. Too bad.
		Map result = new HashMap();
		for (Enumeration e = pluginXML.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			result.put(key, pluginXML.get(key));
		}
		return result;
	}

	/**
	 * get ManifestElement array of element denoted by <code>elementName</code>
	 * from manifest file of a jar or a  plugin directory
	 * 
	 * @param file File instance which represents a bundle directory(or jar file)
	 * @param keys the names of the elements we want to retrieve
	 * @return ManifestElement array of element denoted by <code>elementName</code> or <code>null</code> if
	 * 		   element has not been found
	 * @throws CoreException <p>if {@link BundleException} has been thrown</p>
	 */
	public static Map getElementsFromManifest(File file, String[] keys) throws CoreException {
		Map result = new HashMap();
		// try to get attributes from /META-INF/MANIFEST.MF file first 
		Attributes attributes = getAttributes(file);
		// if we didn't get information, try to get information from plugin.xml file
		Map map = attributes == null ? getAttributesFromPluginManifest(file) : convertAttributes(attributes);
		// TODO log and return
		if (map == null)
			return result;
		for (int i = 0; i < keys.length; i++) {
			String value = (String) map.get(keys[i]);
			if (value == null) {
				// TODO log? we couldn't find the key we were looking for
				result.put(keys[i], null);
				continue;
			}
			try {
				// get elements of attribute denoted by elementName
				ManifestElement[] elements = ManifestElement.parseHeader(keys[i], value);
				// we got a result so add it to the list and move to the next key
				result.put(keys[i], elements);
			} catch (BundleException be) {
				throw new CoreException(new Status(IStatus.WARNING, PLUGIN_ID, IStatus.WARNING, NLS.bind(Messages.PluginVersionCompare_couldNotParseHeaderMsg, value), be));
			}
		}
		return result;
	}

	/*
	 * Convert the given Attributes object into a Map so we can use simple Map APIs on
	 * it later.
	 */
	private static Map convertAttributes(Attributes attributes) {
		Map result = new HashMap();
		for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
			Attributes.Name key = (Name) iter.next();
			result.put(key.toString(), attributes.getValue(key));
		}
		return result;
	}

	/**
	 * Return the attributes that are in the given manifest file.
	 * 
	 * @param file manifest file
	 * @return the attributes of the manifest file, or <code>null</code>
	 * @throws IOException <p>if any IOException has been thrown</p>
	 */
	private static Attributes getAttributes(File file) {
		// if file is a directory, we try to get /META-INF/MANIFEST.MF file under it
		if (file.isDirectory()) {
			File manifestFile = new File(file, BUNDLE_MANIFEST);
			if (!manifestFile.exists())
				return null;
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(manifestFile));
				return new Manifest(input).getMainAttributes();
			} catch (IOException e) {
				return null;
			} finally {
				if (input != null)
					try {
						input.close();
					} catch (IOException e) {
						// ignore
					}
			}
		}
		// if file is a jar, we try to get /META-INF/MANIFEST.MF file in it
		if (file.getName().endsWith(JAR_FILE_EXTENSION)) {
			try {
				return new JarFile(file).getManifest().getMainAttributes();
			} catch (IOException e) {
				return null;
			}
		}
		// we don't have a directory or JAR file
		return null;
	}

}
