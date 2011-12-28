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

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.osgi.framework.Version;

/**
 * VersionCompare
 * 
 * @see org.eclipse.pde.tools.versioning.IVersionCompare
 */
public class FeatureVersionCompare implements VersionCompareConstants {
	// MultiStatus instance used to store error or warning messages of features and plugin entries
	private MultiStatus finalResult;
	// Map used to store ICompreResult instances of verified features
	private Map verifiedFeatureTable;
	// Map used to store ICompreResult instances of verified plugins
	private Map verifiedPluginTable;
	// FeatureModelTables used to store FeatureModels
	private FeatureModelTable featureModelTable1;
	private FeatureModelTable featureModelTable2;
	//
	private PluginVersionCompare pluginVersionCompare;
	//
	private CompareOptionFileHelper compareOptionFileHelper;
	// monitor related variables
	private VersioningProgressMonitorWrapper monitorWrapper;
	//
	private boolean needPluginCompare;
	//
	private long startTime;
	private boolean DEBUG = false;
	private static final String DEBUG_OPTION = VersionCompareConstants.PLUGIN_ID + "/debug/features"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	public FeatureVersionCompare() {
		pluginVersionCompare = new PluginVersionCompare();
		DEBUG = Activator.getBooleanDebugOption(DEBUG_OPTION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(URL, URL, boolean, Map)
	 */
	public IStatus checkFeatureVersions(URL configURL1, URL configURL2, boolean comparePlugins, File optionFile, IProgressMonitor monitor) throws CoreException {
		return compareMain(configURL1, configURL2, comparePlugins, optionFile, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(java.lang.String, java.lang.String, boolean, Map)
	 */
	public IStatus checkFeatureVersions(String path1, String path2, boolean comparePlugins, File optionFile, IProgressMonitor monitor) throws CoreException {
		return checkFeatureVersions(new File(path1), new File(path2), comparePlugins, optionFile, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.tools.versioning.IVersionCompare#checkFeatureVersions(File, File, boolean, Map) 
	 */
	public IStatus checkFeatureVersions(File file1, File file2, boolean comparePlugins, File optionFile, IProgressMonitor monitor) throws CoreException {
		// check to see if we are pointing to two configuration files or two directories
		if ((isConfiguration(file1) && isConfiguration(file2)) || (isFeaturesDirectory(file1) && isFeaturesDirectory(file2))) {
			return compareMain(file1, file2, comparePlugins, optionFile, monitor);
		}
		return resultStatusHandler(IStatus.WARNING, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.FeatureVersionCompare_inputErrorMsg, file1, file2), null);
	}

	/**
	 * compare main method
	 * @param object1 URL, String, File instance which denotes a directory or a configuration file
	 * @param object2 URL, String, File instance which denotes a directory or a configuration file
	 * @param comparePlugins <code>true</code>plugins need to be compared as object, <code>false</code> just to flat comparison
	 * @param optionFile File instance which denotes a compare option file
	 * @param monitor IProgressMonitor instance
	 * @return IStatus instance which contains message generated during comparing
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private IStatus compareMain(Object object1, Object object2, boolean comparePlugins, File optionFile, IProgressMonitor monitor) throws CoreException {
		try {
			monitor = VersioningProgressMonitorWrapper.monitorFor(monitor);
			monitorWrapper = new VersioningProgressMonitorWrapper(monitor);
			if (optionFile != null)
				// generate CompareOptionFileHelper instance
				compareOptionFileHelper = new CompareOptionFileHelper(optionFile);
			// generate a MultiStatus instance to store compare result
			finalResult = new MultiStatus(PLUGIN_ID, IStatus.OK, Messages.FeatureVersionCompare_errorReasonMsg, null);
			// generate verified features and plugins maps
			verifiedFeatureTable = new Hashtable(0);
			verifiedPluginTable = new Hashtable(0);
			this.needPluginCompare = comparePlugins;
			// begin task
			monitorWrapper.beginTask(Messages.FeatureVersionCompare_verifyingFeatureMsg, 100);
			// generate feature model maps
			featureModelTable1 = new FeatureModelTable(object1, compareOptionFileHelper);
			monitorWrapper.worked(comparePlugins ? 5 : 30);
			featureModelTable2 = new FeatureModelTable(object2, compareOptionFileHelper);
			monitorWrapper.worked(comparePlugins ? 5 : 30);
			if (!featureModelTable1.isOK()) {
				finalResult.merge(featureModelTable1.getStatus());
			}
			if (!featureModelTable2.isOK()) {
				finalResult.merge(featureModelTable2.getStatus());
			}
			// if featureModelTable1 or featureModelTable2 is empty, we return here
			if (featureModelTable1.size() == 0 || featureModelTable2.size() == 0) {
				return finalResult;
			}
			compareFeatures();
			monitorWrapper.worked(comparePlugins ? 90 : 40);
			return finalResult;
		} finally {
			monitorWrapper.done();
		}
	}

	/**
	 * check whether or not the given file potentially represents a platform configuration file on the file-system.
	 * 
	 * @return <code>true</code> if <code>file</code> is a configuration file
	 * 		   <code>false</code> otherwise
	 */
	private boolean isConfiguration(File file) {
		return file.isFile() && CONFIGURATION_FILE_NAME.equalsIgnoreCase(new Path(file.getAbsolutePath()).lastSegment());
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
	 * Add the given compare result to the list of already processed features.
	 * 
	 * @param result the compare result
	 */
	private void featureProcessed(CheckedItem result) {
		verifiedFeatureTable.put(result.getSourceKey(), result);
	}

	/**
	 * Add the given compare result to the list of already processed plugins.
	 * 
	 * @param result the compare result
	 */
	private void pluginProcessed(CheckedItem result) {
		verifiedPluginTable.put(result.getSourceKey()+KEY_SEPARATOR+ result.getDestinationKey(), result);
	}

	/**
	 * Verifies every version of feature included in featureModelTable1 based on the feature which has the same feature 
	 * id in featureModelTable2, and generates an overall result as an VersioningMultiStatus instance
	 * 
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private void compareFeatures() throws CoreException {
		startTime = System.currentTimeMillis();
		// compare features
		for (Iterator keys = featureModelTable1.getKeySet().iterator(); keys.hasNext();) {
			if (monitorWrapper.isCanceled())
				throw new OperationCanceledException();
			// get a key (feature ID) from the new features map
			Object key = keys.next();
			FeatureModel newFeatureModel = (FeatureModel) (featureModelTable1.getFeatureModel(key));
			// check if the feature has been checked or not
			if (searchFeatureResult(newFeatureModel.getFeatureIdentifier() + KEY_SEPARATOR + newFeatureModel.getFeatureVersion()) == null) {
				// if the feature has not been checked, we check it and add the result to status list
				featureProcessed(verifyNewFeatureVersion(newFeatureModel, (FeatureModel) featureModelTable2.getFeatureModel(key), monitorWrapper.getSubMonitor((needPluginCompare ? 90 : 40) / featureModelTable1.size())));
			}
		}
		debug("Feature Compare Time: " + (System.currentTimeMillis() - startTime) + " milliseconds"); //$NON-NLS-1$//$NON-NLS-2$
		debug("Total message number: " + finalResult.getChildren().length); //$NON-NLS-1$
	}

	/*
	 * Log the given debug message.
	 */
	private void debug(String message) {
		if (DEBUG)
			Activator.debug(message);
	}

	/**
	 * Compares <code>featureModel1</code> to <code>featureModel2</code> to check if the version of <code>featureModel1</code> is correct or not;
	 * <code>featureModel1</code> and <code>featureModel2</code> have the same feature ID
	 * 
	 * @param featureModel1 the FeatureModel instance 
	 * @param featureModel2 the FeatureModel instance 
	 * @return result of verification encapsulated in an IStatus instance
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private CheckedItem verifyNewFeatureVersion(FeatureModel featureModel1, FeatureModel featureModel2, IProgressMonitor monitor) throws CoreException {
		// get feature id
		String id = featureModel1.getFeatureIdentifier();
		// get versions of featureModel1
		Version version1 = new Version(featureModel1.getFeatureVersion());
		// check if featureModel2 is null, if it's null, we consider it as new added
		if (featureModel2 == null) {
			Object[] msg = {id, version1, featureModelTable1.getLocation(id)};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_newIntroducedFeatureMsg, msg), null));
			return new CheckedItem(id + KEY_SEPARATOR + featureModel1.getFeatureVersion(), null, new Version(featureModel1.getFeatureVersion()), IVersionCompare.NEW_ADDED);
		}
		// get versions of featureModel2
		Version version2 = new Version(featureModel2.getFeatureVersion());
		// compare featureModel1 to featureModel2
		int result = checkFeatureChange(featureModel1, featureModel2, monitor);
		// some versions of included features or plugins are wrong 
		if (result == IVersionCompare.ERROR_OCCURRED) {
			Object[] msg = {id, version1, featureModelTable1.getLocation(id)};
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_nestedErrorOccurredMsg, msg), null));
			return new CheckedItem(id + KEY_SEPARATOR + featureModel1.getFeatureVersion(), null, null, result);
		}
		// get the recommended version
		Version recommendedVersion = recommendVersion(version2, result);
		// check if version1 is correct or not
		if (!isNewVersionValid(version1, recommendedVersion, result)) {
			Object[] msg = new Object[] {id, version1, featureModelTable1.getLocation(id), recommendedVersion};
			if (result == IVersionCompare.QUALIFIER_CHANGE) {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_incorrectNewVersionMsg2, msg), null));
			} else {
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_incorrectNewVersionMsg, msg), null));
			}
			return new CheckedItem(id + KEY_SEPARATOR + featureModel1.getFeatureVersion(), null, recommendedVersion, result);
		}else{
			// check if version has been increased properly
			int change = checkVersionChange(version1, version2);
			if (change < result) {			
				Object[] msg = new Object[] {id, version1, featureModelTable1.getLocation(id), getVersionName(result), getVersionName(change)};
				finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_versionChangeIncorrectMsg, msg), null));
				return new CheckedItem(id + KEY_SEPARATOR + version1.toString(), null, version1, change);				
			}
			return new CheckedItem(id + KEY_SEPARATOR + version1.toString(), null, version1, result);
		}		
	}
	
	/**
	 * gets String which represent type of version denoted by <code>versionType</code>
	 * @param versionType int number
	 * @return String
	 */
	private String getVersionName(int versionType){
		switch (versionType){
			case IVersionCompare.MAJOR_CHANGE: return "Major"; //$NON-NLS-1$
			case IVersionCompare.MINOR_CHANGE: return "Minor"; //$NON-NLS-1$
			case IVersionCompare.MICRO_CHANGE: return "Micro"; //$NON-NLS-1$
			case IVersionCompare.QUALIFIER_CHANGE: return "Qualify"; //$NON-NLS-1$
			case IVersionCompare.NO_CHANGE: return "NoChange"; //$NON-NLS-1$			
			default: return EMPTY_STRING;
		}
	}

	/**
	 * Compares included features and plugins in <code>featureModel1</code> to those in <code>featureModel2</code>. 
	 * Check if there is any change from <code>featureModel2</code> to <code>feaureModel1</code>
	 * 
	 * @param featureModel1 FeatureModel instance
	 * @param featureModel2 FeatureModel instance
	 * @return change with the highest priority from <code>featureModel2</code> to <code>featureModel1</code>
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private int checkFeatureChange(FeatureModel featureModel1, FeatureModel featureModel2, IProgressMonitor monitor) throws CoreException {
		int overallChange = IVersionCompare.NO_CHANGE;
		String id = featureModel1.getFeatureIdentifier();
		Version version1 = new Version(featureModel1.getFeatureVersion());
		Version version2 = new Version(featureModel2.getFeatureVersion());
		// compare feature version first
		if (version1.compareTo(version2) < 0) {
			Object[] msg = {id, version1, featureModelTable1.getLocation(id), version2, featureModelTable2.getLocation(id)};
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_newVersionLowerThanOldMsg, msg), null));
		}
		// get IncludedFeatureReference, PluginEntries of featureModel1
		IIncludedFeatureReference[] includedFeatures1 = featureModel1.getFeatureIncluded();
		PluginEntryModel[] pluginEntries1 = featureModel1.getPluginEntryModels();
		// get IncludedFeatureReference, PluginEntries of featureModel2
		IIncludedFeatureReference[] includedFeatures2 = featureModel2.getFeatureIncluded();
		PluginEntryModel[] pluginEntries2 = featureModel2.getPluginEntryModels();
		try {
			monitor.beginTask(NLS.bind(Messages.FeatureVersionCompare_comparingFeatureMsg, id + UNDERSCORE_MARK + version1.toString()), 10);
			// compare IncludedFeatureReferences in featureModel1 with those in featureModel2
			overallChange = Math.min(overallChange, compareReferences(id, includedFeatures1, includedFeatures2, monitorWrapper.getSubMonitor(5)));
			// compare PluginEntries in featureModel1 with those in featureModel2
			overallChange = Math.min(overallChange, compareReferences(id, pluginEntries1, pluginEntries2, monitorWrapper.getSubMonitor(5)));
			// return the version change with the highest priority
			return overallChange;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Generates and returns a recommended version based on the given version and change.
	 * 
	 * @param version the current version
	 * @param change the degree of change
	 * @return the recommended new version or <code>null</code> if the change is IVersionCompare.ERROR_OCCURRED 
	 * 		   or the change is not an expected value
	 */
	private Version recommendVersion(Version version, int change) {
		// get major, minor, service of version
		int major = version.getMajor();
		int minor = version.getMinor();
		int micro = version.getMicro();
		String qualifier = version.getQualifier();
		// update major, minor, service based on the compare result
		switch (change) {
			case IVersionCompare.ERROR_OCCURRED :
				return null;
			case IVersionCompare.MAJOR_CHANGE : {
				major++;
				minor = 0;
				micro = 0;
				qualifier = EMPTY_STRING;
				break;
			}
				// NOT_EXIST, NEW_ADDED are currently treated as minor change
			case IVersionCompare.NO_LONGER_EXIST :
			case IVersionCompare.NEW_ADDED :
			case IVersionCompare.MINOR_CHANGE : {
				minor++;
				micro = 0;
				qualifier = EMPTY_STRING;
				break;
			}
			case IVersionCompare.MICRO_CHANGE : {
				micro++;
				qualifier = EMPTY_STRING;
				break;
			}
			case IVersionCompare.QUALIFIER_CHANGE : {
				// TODO increase qualifier
				break;
			}
			case IVersionCompare.NO_CHANGE : {
				// if there is no change, just return the version
				return version;
			}
			default : {
				return null;
			}
		}
		// generate recommended version
		return new Version(major, minor, micro, qualifier);
	}

	/**
	 * Checks if the <code>version1</code> is valid, based on <code>version2</code> and <code>change</code>
	 * from <code>version2</code> to <code>version1</code>
	 * 
	 * @param version1 feature version
	 * @param version2 feature version
	 * @param change change from <code>version2</code> to <code>version1</code>
	 * @return <code>true</code> if version1 is correct and <code>false</code> otherwise
	 */
	private boolean isNewVersionValid(Version version1, Version version2, int change) {
		if (change == IVersionCompare.QUALIFIER_CHANGE) {
			// compare qualifier version. We currently don't increase qualifier version if there is 
			// QUALIFIER_CHANGE,we need the version1 be big than version2
			return version1.compareTo(version2) > 0;
		}
		// compare qualifier version
		return version1.compareTo(version2) >= 0;
	}

	/**
	 * Compares feature version(or plugin entry versions) in <code>references1</code> to those in <code>references2</code>.
	 * Check if there is any version change from <code>references2</code> to <code>references1</code>. The change with
	 * the highest priority will be returned
	 * 
	 * @param parentID id of the feature which these features(or plugin entries) belong to
	 * @param references1 array of included plugins or features 
	 * @param references2 array of included plugins or features 
	 * @return change with the highest priority from <code>references2</code> to <code>references1</code>
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private int compareReferences(String parentID, Object[] references1, Object[] references2, IProgressMonitor monitor) throws CoreException {
		try {
			// generate Maps for references1 and references2
			Map referenceMap1 = generateTable(references1);
			Map referenceMap2 = generateTable(references2);
			//
			if (referenceMap1 == null && referenceMap2 == null) {
				return IVersionCompare.NO_CHANGE;
			}
			monitor.beginTask(Messages.FeatureVersionCompare_comparingReferenceMsg, referenceMap1 == null? 1 : referenceMap1.size() + 1);
			// initialization before compare
			int overallChange = IVersionCompare.NO_CHANGE;
			if (referenceMap1 != null) {
				// compares features(or plugin entries) in referenceMap1 to those in referenceMap2
				for (Iterator keys = referenceMap1.keySet().iterator(); keys.hasNext();) {
					if (monitorWrapper.isCanceled())
						throw new OperationCanceledException();
					// get the next key (feature id)
					Object id = keys.next();
					// get corresponding version from referenceMap1
					Version version1 = referenceMap1 == null ? null : (Version) (referenceMap1.get(id));
					// get the version of corresponding reference from referenceMap2
					Version version2 = referenceMap2 == null ? null : (Version) referenceMap2.get(id);
					int currentChange = IVersionCompare.NO_CHANGE;
					if (references1[0] instanceof IIncludedFeatureReference) {
						currentChange = compareIncludedFeatureReference(parentID, (String) id, version1, version2, new SubProgressMonitor(monitor, 1));
					} else {
						currentChange = compareIncludedPluginReference(parentID, (String) id, version1, version2, new SubProgressMonitor(monitor, 1));
					}
					// we save the version change with higher priority
					overallChange = Math.min(overallChange, currentChange);
					// delete the reference from referenceMap2
					if (referenceMap2 != null)
						referenceMap2.remove(id);
				}
			}
			// new features or plugins added into the feature
			if (referenceMap2 != null) {
				for (Iterator keys = referenceMap2.keySet().iterator(); keys.hasNext();) {
					Object id = keys.next();
					Version version = (Version) referenceMap2.get(id);
					if (references2[0] instanceof IIncludedFeatureReference) {
						Object[] msg = {FEATURE_TITLE, (String) id + UNDERSCORE_MARK + version.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_deletedFeaturePluginMsg, msg), null));
					} else {
						Object[] msg = {PLUGIN_TITLE, (String) id + UNDERSCORE_MARK + version.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
						finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_deletedFeaturePluginMsg, msg), null));
					}
				}
				if (!referenceMap2.isEmpty()) {
					overallChange = Math.min(overallChange, IVersionCompare.NO_LONGER_EXIST);
				}
			}
			monitor.worked(1);
			return overallChange;
		} finally {
			monitor.done();
		}
	}

	/**
	 * compares two versions of an included plugins
	 * @param parentID ID of the feature to which the plugin denoted by <code>pluginID</code> belongs
	 * @param pluginID plugin id
	 * @param version1 new version of plugin denoted by <code>pluginID</code>
	 * @param version2 old version of plugin denoted by <code>pluginID</code>
	 * @return change happened on plugin denoted by <code>pluginID</code>
	 */
	private int compareIncludedPluginReference(String parentID, String pluginID, Version version1, Version version2, IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 10); //$NON-NLS-1$
			int pluginChange = IVersionCompare.NO_CHANGE;
			if (version2 == null) {
				// if version2 is null, the plugin is a new added one
				Object[] msg = {PLUGIN_TITLE, pluginID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
				finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_newAddedFeaturePlingMsg, msg), null));
				//pluginChange = IVersionCompare.NEW_ADDED;				
				// the return was changed to a MINOR_CHANGE because adding a new plugin is considering a minor change
				// and the value associated with NEW_ADDED indicates that it is a greater than minor change
				pluginChange = IVersionCompare.MINOR_CHANGE;
			} else {
				if (version1.compareTo(version2) < 0) {
					// if version1 is lower than version2, it is wrong
					Object[] msg = new Object[] {version1, PLUGIN_TITLE, pluginID, parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID), version2, featureModelTable2.getVersion(parentID), featureModelTable1.getLocation(parentID)};
					finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_lowerNewVersion, msg), null));
					if (!needPluginCompare)
						return IVersionCompare.ERROR_OCCURRED;
				}
				if (needPluginCompare) {
					// Object level compare
					String pluginKey1 = pluginID + KEY_SEPARATOR + version1.toString();
					String pluginKey2 = pluginID + KEY_SEPARATOR + version2.toString();
					// check if the plugin has been compared
					CheckedItem pluginResult = searchPluginResult(pluginKey1 + pluginKey2);
					monitor.worked(1);
					if (pluginResult != null) {
						pluginChange = pluginResult.getChange();
					} else {
						// compare plugins as objects
						IPath pluginDirPath1 = getPluginDirPath(featureModelTable1.getLocation(parentID));
						IPath pluginDirPath2 = getPluginDirPath(featureModelTable2.getLocation(parentID));
						Map pluginInfo1 = searchPlugin(pluginDirPath1, pluginID, version1.toString());
						Map pluginInfo2 = searchPlugin(pluginDirPath2, pluginID, version2.toString());
						monitor.worked(2);
						// do plugin compare if both pluginPath1 and pluginPath2 are not null
						if (pluginInfo1 != null && pluginInfo2 != null) {
							try {
								// compare two plugins
								pluginChange = Math.min(pluginChange, pluginVersionCompare.checkPluginVersions(finalResult, ((IPath) pluginInfo1.get(BUNDLE_LOCATION)).toFile(), (ManifestElement[]) pluginInfo1.get(BUNDLE_CLASSPATH), ((IPath) pluginInfo2.get(BUNDLE_LOCATION)).toFile(), (ManifestElement[]) pluginInfo2.get(BUNDLE_CLASSPATH), new SubProgressMonitor(monitor, 7)));
							} catch (CoreException ce) {
								pluginChange = IVersionCompare.ERROR_OCCURRED;
							}
							if (pluginChange == IVersionCompare.ERROR_OCCURRED) {
								// put the compare result into table, we don't need to compare it again if it is also included in another feature
								pluginProcessed(new CheckedItem(pluginKey1, pluginKey2, null, pluginChange));
								// set message
								Object[] msg = {pluginID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
								finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_errorInVerifyPluginMsg, msg), null));
								// if result is error, set overallChange as error and continue to check next plugin
								pluginChange = IVersionCompare.ERROR_OCCURRED;
							} else {
								// get recommended version based on compare result
								Version recommendedVersion = recommendVersion(version2, pluginChange);
								if (!isNewVersionValid(version1, recommendedVersion, pluginChange)) {
									// if version1 is not correct, set message
									Object[] msg = {pluginID, version1.toString(), ((IPath) pluginInfo1.get(BUNDLE_LOCATION)).toOSString(), recommendedVersion};
									finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_incorrectPluginVersionMsg, msg), null));
								}else{
									// check if version has been increased properly
									int actualChange = checkVersionChange(version1, version2);
									if (actualChange < pluginChange){
										Object[] msg = new Object[] {pluginID, version1, getVersionName(pluginChange), getVersionName(actualChange)};
										finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.FEATURE_OVERALL_STATUS, NLS.bind(Messages.FeatureVersionCompare_incorrectPluginVersionChange, msg), null));
										pluginChange = actualChange;
									}
								}	
								// put the compare result into table, we don't need to compare it again if it is also included in another feature
								pluginProcessed(new CheckedItem(pluginKey1, pluginKey2,recommendedVersion, pluginChange));
							}
						} else {
							// if pluginPath1 or pluginPath2 is null, set messages
							if (pluginInfo1 == null) {
								Object[] msg = {pluginID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID), pluginDirPath1};
								finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_pluginNotFoundMsg, msg), null));
							}
							if (pluginInfo2 == null) {
								Object[] msg = {pluginID + UNDERSCORE_MARK + version2.toString(), parentID, featureModelTable2.getVersion(parentID), featureModelTable2.getLocation(parentID), pluginDirPath2};
								finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_pluginNotFoundMsg, msg), null));
							}
							pluginChange = IVersionCompare.ERROR_OCCURRED;
							// put the compare into table, we don't need to compare it again if it is also included in another feature
							pluginProcessed(new CheckedItem(pluginKey1, pluginKey2, null, pluginChange));
						}
					}
				} else {
					// String level compare
					pluginChange = checkVersionChange(version1, version2);
				}
			}
			return pluginChange;
		} finally {
			monitor.done();
		}
	}

	/**
	 * compares two versions of an included features
	 * @param parentID ID of the feature to which the feature denoted by <code>featureID</code> belongs
	 * @param featureID feature id
	 * @param version1 new version of feature denoted by <code>featureID</code>
	 * @param version2 old version of feature denoted by <code>featureID</code>
	 * @return change happened on feature denoted by <code>featureID</code>
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private int compareIncludedFeatureReference(String parentID, String featureID, Version version1, Version version2, IProgressMonitor monitor) throws CoreException {
		String key1 = featureID + KEY_SEPARATOR + version1.toString();
		int change = IVersionCompare.NO_CHANGE;
		// if version2 is null, the feature is a new added one
		if (version2 == null) {
			Object[] msg = {FEATURE_TITLE, featureID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
			finalResult.merge(resultStatusHandler(IStatus.INFO, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_newAddedFeaturePlingMsg, msg), null));
			change = IVersionCompare.NEW_ADDED;
		}
		// get the corresponding FeatureModel from featureModelTable1
		FeatureModel featureModel1 = (FeatureModel) featureModelTable1.getFeatureModel(featureID);
		FeatureModel featureModel2 = (FeatureModel) featureModelTable2.getFeatureModel(featureID);
		if (featureModel1 == null) {
			// if the new FeatureModel has not been found, it's an error
			Object[] msg = {featureID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_sourceIncludedFeatureNotFoundMsg, msg), null));
			change = IVersionCompare.ERROR_OCCURRED;
		} else if (!featureModel1.getFeatureVersion().equals(version1.toString())) {
			//if the version of the FeatureModel we got does not match the version of the reference, it is an error
			// e.g. feature f1 includes feature f2-1.0.0.v2006, but the corresponding FeatureModel we found is feature f2-1.0.1, it is wrong
			Object[] msg = {featureID + UNDERSCORE_MARK + version1.toString(), parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID)};
			finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_sourceIncludedFeatureNotFoundMsg, msg), null));
			change = IVersionCompare.ERROR_OCCURRED;
		}
		// if the feature is a new added one, we don't do further check
		if (version2 != null) {
			if (featureModel2 == null) {
				// if featureModel2 is null, it is an error
				Object[] msg = {featureID + UNDERSCORE_MARK + version2.toString(), parentID, featureModelTable2.getVersion(parentID), featureModelTable2.getLocation(parentID)};
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_destIncludedFeatureNotFoundMsg, msg), null));
				change = IVersionCompare.ERROR_OCCURRED;
			} else if (!featureModel2.getFeatureVersion().equals(version2.toString())) {
				//if the version of the FeatureModel we got does not match the version of the reference, it is an error
				// e.g. feature f1 includes feature f2-1.0.0.v2006, but the corresponding FeatureModel we found is feature f2-1.0.1, it is wrong
				Object[] msg = {featureID + UNDERSCORE_MARK + version2.toString(), parentID, featureModelTable2.getVersion(parentID), featureModelTable2.getLocation(parentID)};
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_destIncludedFeatureNotFoundMsg, msg), null));
				change = IVersionCompare.ERROR_OCCURRED;
			}
			// String level compare
			if (version1.compareTo(version2) < 0) {
				Object[] msg = new Object[] {version1, FEATURE_TITLE, featureID, parentID, featureModelTable1.getVersion(parentID), featureModelTable1.getLocation(parentID), version2, featureModelTable2.getVersion(parentID), featureModelTable1.getLocation(parentID)};
				finalResult.merge(resultStatusHandler(IStatus.ERROR, IVersionCompare.FEATURE_DETAIL_STATUS, NLS.bind(Messages.FeatureVersionCompare_lowerNewVersion, msg), null));
			}
			if (change == IVersionCompare.ERROR_OCCURRED) {
				featureProcessed(new CheckedItem(key1, null, null, IVersionCompare.ERROR_OCCURRED));
				return IVersionCompare.ERROR_OCCURRED;
			}
			// check if the current feature version has been checked
			CheckedItem compareResult = searchFeatureResult(key1);
			// Object level compare
			if (compareResult == null) {
				// check the included feature models and get result status
				compareResult = verifyNewFeatureVersion(featureModel1, featureModel2, monitorWrapper.getSubMonitor(needPluginCompare ? 90 : 40 / featureModelTable1.size()));
				// add the result status into result list
				featureProcessed(compareResult);
			}
			change = Math.min(change, compareResult.getChange());
		}
		return change;
	}

	/*
	 * Return a Map contains bundle name, bundle version, bundle classpath, and bundle location
	 * if the location of the specific plug-in version that we are looking for. Otherwise return
	 * <code>null</code>
	 */
	private Map matchesPlugin(File file, String pluginName, String pluginVersion) throws CoreException {
		if (!file.exists())
			return null;
		Map manifest = ManifestHelper.getElementsFromManifest(file, new String[] {BUNDLE_SYMBOLICNAME, BUNDLE_VERSION, BUNDLE_CLASSPATH});
		ManifestElement[] elements = (ManifestElement[]) manifest.get(BUNDLE_SYMBOLICNAME);
		if (elements == null)
			return null;
		String name = elements[0].getValue();
		if (name == null || !name.equals(pluginName))
			return null;
		elements = (ManifestElement[]) manifest.get(BUNDLE_VERSION);
		if (elements == null)
			return null;
		String version = elements[0].getValue();
		if (version == null || !version.equals(pluginVersion))
			return null;
		manifest.put(BUNDLE_LOCATION, new Path(file.getAbsolutePath()));
		return manifest;
	}

	/**
	 * searches for plugin denoted by <code>pluginName</code> and <code>pluginVersion</code> under
	 * plugin directory indicated by <code>plugDir</code>, and return a Map contains bundle name, 
	 * bundle version, bundle classpath, and bundle location if we find, return <code>null</code> 
	 * if we don't find
	 * 
	 * @param plugDir plugin directory
	 * @param pluginName plugin name
	 * @param pluginVersion plugin version
	 * @return full path of plugin
	 */
	private Map searchPlugin(IPath plugDir, String pluginName, String pluginVersion) {
		// as an optimization first check to see if the plug-in directory name is the Eclipse-format
		try {
			// check to see if we can guess the JAR name
			File jar = plugDir.append(pluginName + UNDERSCORE_MARK + pluginVersion + DOT_MARK + JAR_FILE_EXTENSION).toFile();
			Map manifest = matchesPlugin(jar, pluginName, pluginVersion);
			if (manifest != null)
				return manifest;
			// didn't match the JAR so try and match the directory name
			File dir = plugDir.append(pluginName + UNDERSCORE_MARK + pluginVersion).toFile();
			manifest = matchesPlugin(dir, pluginName, pluginVersion);
			if (manifest != null)
				return manifest;
		} catch (CoreException e) {
			// ignore and catch below if re-thrown
		}
		File[] plugins = plugDir.toFile().listFiles();
		if (plugins == null)
			return null;
		for (int i = 0; i < plugins.length; i++) {
			try {
				Map manifest = matchesPlugin(plugins[i], pluginName, pluginVersion);
				if (manifest != null)
					return manifest;
			} catch (CoreException ce) {
				finalResult.merge(resultStatusHandler(IStatus.WARNING, IVersionCompare.PROCESS_ERROR_STATUS, NLS.bind(Messages.PluginVersionCompare_couldNotReadManifestMsg, plugins[i].getAbsolutePath()), null));
			}
		}
		return null;
	}

	/**
	 * generates a map which stores the key-value pairs; Feature id(or plugin id) is key and version is value;
	 * or <code>null</code> when <code>objects</code> is empty
	 * 
	 * @param objects array of feature or plugin entry objects
	 * @return Map instance contains id-version pairs or <code>null</code>
	 * @throws CoreException <p>if any nested CoreException has been thrown</p>
	 */
	private Map generateTable(Object[] objects) throws CoreException {
		if (objects == null || objects.length == 0) {
			return null;
		}
		Hashtable hashTable = new Hashtable();
		// set the ids and versions of plugins into hashtable, id is key, version is value
		if (objects[0] instanceof IIncludedFeatureReference) {
			for (int i = 0; i < objects.length; i++)
				if (objects[i] != null && (compareOptionFileHelper == null ? true : compareOptionFileHelper.shouldCompare(objects[i])))
					hashTable.put(((IIncludedFeatureReference) objects[i]).getVersionedIdentifier().getIdentifier(), new Version(((IIncludedFeatureReference) objects[i]).getVersionedIdentifier().getVersion().toString()));
		} else if (objects[0] instanceof PluginEntryModel) {
			for (int i = 0; i < objects.length; i++)
				if (objects[i] != null && (compareOptionFileHelper == null ? true : compareOptionFileHelper.shouldCompare(objects[i])))
					hashTable.put(((PluginEntryModel) objects[i]).getPluginIdentifier(), new Version(((PluginEntryModel) objects[i]).getPluginVersion()));
		}
		return hashTable;
	}

	/**
	 * Checks if there is any change from <version2> to <version1>
	 * 
	 * @param version1 feature version
	 * @param version2 feature version
	 * @return compare result indicates change in major, minor, micro, qualifier ,no change or error if <code>version1</code> is lower than <code>version2</code>
	 */
	private int checkVersionChange(Version version1, Version version2) {
		// overall compare
		if (version1.compareTo(version2) < 0) {
			return IVersionCompare.ERROR_OCCURRED;
		}		
		// compare major version
		if (version1.getMajor() > version2.getMajor()) {
			return IVersionCompare.MAJOR_CHANGE;
		}	
		// compare minor version
		if (version1.getMinor() > version2.getMinor()) {
			return IVersionCompare.MINOR_CHANGE;
		}		
		// compare micro version
		if (version1.getMicro() > version2.getMicro()) {
			return IVersionCompare.MICRO_CHANGE;
		}
		// compare qualifier version
		if (version1.getQualifier().compareTo(version2.getQualifier()) > 0) {
			return IVersionCompare.QUALIFIER_CHANGE;
		}		
		// if we got to this point, it means no change
		return IVersionCompare.NO_CHANGE;
	}

	/**
	 * Returns the compare result for the feature with the specified key. Returns <code>null</code>
	 * if it is not found.
	 * 
	 * @param key FeatureKey instance (feature id + "#" + feature version)
	 * @return the compare result or <code>null</code>
	 */
	private CheckedItem searchFeatureResult(Object key) {
		return (CheckedItem) verifiedFeatureTable.get(key);
	}

	/**
	 * Returns the compare result for the plugin with the specified key. Returns <code>null</code>
	 * if it is not found.
	 * 
	 * @param key compare source plugin key instance (plugin id + "#" + plugin version) + "#" + 
	 * 		  compare destination plugin key instance (plugin id + "#" + plugin version)
	 * @return the compare result or <code>null</code> if not found
	 */
	private CheckedItem searchPluginResult(Object key) {
		return (CheckedItem) verifiedPluginTable.get(key);
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
	 * get path of "plugins/" based on the given feature file path
	 * @param featurePath path of a "feature.xml" file
	 * @return path of "plugins/"
	 */
	private IPath getPluginDirPath(IPath featurePath) {
		IPath path = featurePath.removeLastSegments(3);
		return path.append(PLUGIN_DIR_NAME);
	}
}