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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfiguration.ISiteEntry;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.xml.sax.SAXException;

/**
 * FeatureModels use two hashtables to store FeatureModels and
 * feature.xml file locations. It provides methods to access
 * information of FeatureModels
 * 
 *
 */
public class FeatureModelTable implements VersionCompareConstants {

	class TableEntry {
		FeatureModel model;
		IPath location;

		TableEntry(FeatureModel model, IPath location) {
			super();
			this.model = model;
			this.location = location;
		}
	}

	// indicate the FeatureModels is from a configuration file or a directory
	private boolean isConfiguration;
	// table for storing features. key is a string which is the feature identifier. values are instances
	// of the TableEntry inner class as defined by this type.
	private Map featureTable;
	// MultiStatus which stores error or warning message
	private MultiStatus status;
	//
	private CompareOptionFileHelper compareOptionFileHelper;

	/**
	 * constructor
	 * 
	 * @param object it could be a String, File, or URL which denotes to a eclipse configuration file or a feature directory
	 * @param helper CompareOptionFileHelper instance
	 * @throws CoreException <p>if any nested CoreException has been thrown
	 */
	public FeatureModelTable(Object object, CompareOptionFileHelper helper) throws CoreException {
		if (object instanceof URL)
			initialize((URL) object, helper);
		else if (object instanceof File)
			initialize((File) object, helper);
		else if (object instanceof String)
			initialize(new File((String) object), helper);
	}

	/**
	 * initialization
	 * 
	 * @param configURL URL instance which denotes an eclipse configuration XML file
	 * @throws CoreException
	 */
	private void initialize(URL configURL, CompareOptionFileHelper helper) throws CoreException {
		compareOptionFileHelper = helper;
		status = new MultiStatus(PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		featureTable = new HashMap(0);
		IPlatformConfiguration config;
		// get configuration
		try {
			config = ConfiguratorUtils.getPlatformConfiguration(configURL);
		} catch (IOException ioe) {
			throw new CoreException(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureModelTable_couldNotReadConfigFileMsg, configURL.getFile()), ioe));
		}
		// generate Lists which store URLs of feature entries included in configuration1 and configuration2
		List featureList = generateFeatureEntryList(configURL, config);
		if (featureList == null) {
			status.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureVersionCompare_noFeaturesFoundInConfigMsg, configURL), null));
		}
		// generate feature model maps and feature address maps
		generateFeatureModelTable(config, featureList);
		// this is a configuration file
		isConfiguration = true;
	}

	/**
	 * initialization
	 * 
	 * @param file File instance which denotes an directory of FileSystem
	 * @param map inclusive map
	 * @throws CoreException
	 */
	private void initialize(File file, CompareOptionFileHelper helper) throws CoreException {
		// check to see if we are pointing to configuration files
		if (isConfiguration(file)) {
			try {
				initialize(file.toURL(), helper);
				return;
			} catch (MalformedURLException e) {
				throw new CoreException(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureModelTable_urlConvertErrorMsg, file.getAbsolutePath()), e));
			}
		}
		compareOptionFileHelper = helper;
		status = new MultiStatus(PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
		featureTable = new HashMap(0);
		// check to see if we have feature directories
		if (isFeaturesDirectory(file)) {
			// if file are directory, get sub feature directories under it
			File[] subFeatures = file.listFiles(new VersioningDirFilter());
			// check if there is any feature directory under directory1 and directory2
			if (subFeatures == null || subFeatures.length == 0) {
				status.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureVersionCompare_noFeatureFoundMsg, file), null));
				return;
			}
			// generate feature model maps
			generateFeatureModelTable(subFeatures);
			if (featureTable.size() == 0)
				status.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureVersionCompare_noFeatureFoundMsg, file), null));
			// this is a directory
			isConfiguration = false;
			return;
		}
		status.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureModelTable_featureSourceErrorMsg, file), null));		
	}

	/**
	 * check whether or not the given file potentially represents a platform configuration file on the file-system.
	 * 
	 * @return <code>true</code> if <code>file</code> is a configuration file
	 * 		   <code>false</code> otherwise
	 */
	private boolean isConfiguration(File file) {
		IPath path = new Path(file.getAbsolutePath());
		return file.isFile() && CONFIGURATION_FILE_NAME.equalsIgnoreCase(path.lastSegment());
	}

	/**
	 * check whether or not the given file represents a features directory.
	 * 
	 * @return <code>true</code> if <code>file</code> is a directory
	 * 		   <code>false</code> otherwise
	 */
	private boolean isFeaturesDirectory(File file) {
		return file.isDirectory();
	}

	/**
	 * extracts FeatureModels of features in <code>featureFiles</code>, and
	 * put them into hashtable
	 * Key is feature id 
	 * Value is FeatureModel instance of the feature
	 * 
	 * @param featureFiles array of File instances 
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private void generateFeatureModelTable(File[] featureFiles) throws CoreException {
		File[] featureXMLFile;
		for (int i = 0; i < featureFiles.length; i++) {
			// get "feature.xml" file under the feature directory
			featureXMLFile = featureFiles[i].listFiles(new VersioningFeatureFileFilter(FEATURES_FILE_NAME));
			if (featureXMLFile == null || featureXMLFile.length == 0) {
				// there is no "feature.xml" file found
				continue;
			}
			// parse the "feature.xml" file and get its FeatureModel object
			FeatureModel fm = parseFeature(featureXMLFile[0].getAbsolutePath());
			if (compareOptionFileHelper == null ? true : compareOptionFileHelper.shouldCompare(fm))
				// add the feature and its information to the table
				featureTable.put(fm.getFeatureIdentifier(), new TableEntry(fm, new Path(featureXMLFile[0].getAbsolutePath())));
		}
	}

	/**
	 * Generates a list which contains locations of feature entries included in <code>config</code>
	 * represented by <code>IPath</code> objects.
	 * 
	 * @param config configuration
	 * @return List instance contains IPaths of feature entries included in <code>config</code>; return is <code>null</code>
	 *  	   if there is no feature entry has been found in <code>config</code>
	 */
	private List generateFeatureEntryList(URL configURL, IPlatformConfiguration config) {
		// get site entries from the configuration
		ISiteEntry[] sites = config.getConfiguredSites();
		// create a new List
		ArrayList list = new ArrayList(0);
		for (int i = 0; i < sites.length; i++) {
			IPath sitePath;
			// get URL of site
			URL siteURL = sites[i].getURL();
			if (siteURL.getProtocol().equals(PLATFORM_PROTOCOL)) {
				sitePath = new Path(configURL.getPath());
				// path points to the platform.xml file in the update bundle's
				// location in the configuration area so work our way back
				// to the root of the Eclipse install.
				sitePath = sitePath.removeLastSegments(3);
			} else {
				sitePath = new Path(siteURL.getPath());
			}
			String[] features = sites[i].getFeatures();
			// create a path for each feature entry included in the site
			for (int j = 0; j < features.length; j++)
				list.add(sitePath.append(features[j]).append(FEATURES_FILE_NAME));
		}
		return list.size() == 0 ? null : list;
	}

	/**
	 * extracts FeatureModels of features in configuration file denoted by <code>config</code>,
	 * Key is feature id 
	 * Value is FeatureModel object
	 * 
	 * @param config configuration
	 * @param list list of feature locations (IPath)
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private void generateFeatureModelTable(IPlatformConfiguration config, List list) throws CoreException {
		// create a map which contains the feature id and version of the feature entries in the configuration
		for (Iterator features = list.iterator(); features.hasNext();) {
			IPath location = null;
			try {
				// get location of feature
				location = (IPath) features.next();
				// get FeatureModel 
				FeatureModel fm = parseFeature(location.toFile().toURL().openStream());
				if (compareOptionFileHelper == null ? true : compareOptionFileHelper.shouldCompare(fm))
					featureTable.put(fm.getFeatureIdentifier(), new TableEntry(fm, location));
			} catch (IOException ioe) {
				status.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureVersionCompare_featureFileErrorMsg, location.toOSString()), ioe));
				continue;
			}
		}
	}

	/**
	 * Return a new status object populated with the given information.
	 * 
	 * @param severity severity of status
	 * @param code indicates type of this IStatus instance, it could be one of: FEATURE_OVERALL_STATUS, 
	 * 		 FEATURE_DETAIL_STATUS, PLUGIN_OVERALL_STATUS, PLUGIN_DETAIL_STATUS, PROCESS_ERROR_STATUS,
	 * 		 CLASS_OVERALL_STATUS, CLASS_DETAIL_STATUS
	 * @param message the status message
	 * @param exception exception which has been caught, or <code>null</code>
	 * @return the new status object
	 */
	private IStatus resultStatusHandler(int severity, int code, String message, Exception exception) {
		if (message == null) {
			if (exception != null)
				message = exception.getMessage();
			// extra check because the exception message can be null
			if (message == null)
				message = EMPTY_STRING;
		}
		return new Status(severity, PLUGIN_ID, code, message, exception);
	}

	/**
	 * Parses the feature manifest file denoted by the given input stream and returns
	 * the resulting feature model object. The stream is closed when this method is returned.
	 * 
	 * @param input the stream pointing to the feature manifest
	 * @return resulting feature model object
	 * @throws CoreException if there was an error parsing the input stream
	 */
	private FeatureModel parseFeature(InputStream input) throws CoreException {
		try {
			FeatureModelFactory factory = new FeatureModelFactory();
			return factory.parseFeature(input);
		} catch (SAXException saxe) {
			throw new CoreException(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, null, saxe));
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Parses the feature manifest file located in the file-system at the given path. Return
	 * the resulting feature model object.
	 * 
	 * @param path the location of the feature manifest
	 * @return the resulting feature model
	 * @throws CoreException if there was an error parsing the feature manifest
	 */
	private FeatureModel parseFeature(String path) throws CoreException {
		try {
			InputStream fis = new BufferedInputStream(new FileInputStream(path));
			// return FeatureModel of feature.xml the FielInputStream passed to parseFeature(InputStream)
			// will be closed in parseFeature(InputStream featureInputStream) method.
			return parseFeature(fis);
		} catch (FileNotFoundException fnfe) {
			throw new CoreException(resultStatusHandler(IStatus.ERROR, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureModelTable_featureFileParseErrorMsg, path), fnfe));
		}
	}

	/**
	 * checks if the FeatureModels included in the current object are from a configuration file
	 * @return <code>true</code> if the current object contains FeatureModels from a configuration file
	 * 		   <code>false</code> otherwise
	 */
	public boolean isConfiguration() {
		return isConfiguration;
	}

	/**
	 * checks if there is any error occurred when generate the current object
	 * @return <code>true</code> if there is no error occurred
	 * 		   <code>false</code> otherwise
	 */
	public boolean isOK() {
		return status.isOK();
	}

	/**
	 * gets location of the feature which is denoted by <code>id</code>
	 * @param id feature id
	 * @return location of feature <code>id</code>
	 */
	public IPath getLocation(String id) {
		TableEntry entry = (TableEntry) featureTable.get(id);
		return entry == null ? null : entry.location;
	}

	/**
	 * gets version of the feature which is denoted by <code>id</code>
	 * @param id feature id
	 * @return feature version(String)
	 */
	public String getVersion(String id) {
		TableEntry entry = (TableEntry) featureTable.get(id);
		return entry == null ? null : entry.model.getFeatureVersion();
	}

	/**
	 * gets status of the current FeatureModelTable object
	 * @return status
	 */
	public IStatus getStatus() {
		return status;
	}

	/**
	 * gets FeatureModel instance of the feature denoted by <code>id</code>
	 * @param id feature id
	 * @return Object instance which presents a FeatureModel instance
	 */
	public Object getFeatureModel(Object id) {
		TableEntry entry = (TableEntry) featureTable.get(id);
		return entry == null ? null : entry.model;
	}

	/**
	 * gets number of FeatureModel instance included in the current FeatureModelTable instance
	 * @return number of FeatureModel instance
	 */
	public int size() {
		return featureTable.size();
	}

	/**
	 * gets a Set view of the keys contained in the current FeatureModelTable instance
	 * @return a set view of keys contained in the current FeatureModelTable instance
	 */
	public Set getKeySet() {
		return featureTable.keySet();
	}
}
