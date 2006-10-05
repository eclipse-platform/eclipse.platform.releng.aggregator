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

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.pde.tools.internal.versioning.messages";//$NON-NLS-1$

	public static String FeatureModelTable_couldNotReadConfigFileMsg;
	public static String FeatureModelTable_featureFileParseErrorMsg;

	public static String FeatureModelTable_featureSourceErrorMsg;
	public static String FeatureModelTable_urlConvertErrorMsg;
	public static String FeatureVersionCompare_comparingFeatureMsg;
	public static String FeatureVersionCompare_comparingReferenceMsg;
	public static String FeatureVersionCompare_deletedFeaturePluginMsg;
	public static String FeatureVersionCompare_destIncludedFeatureNotFoundMsg;
	public static String FeatureVersionCompare_errorInVerifyPluginMsg;
	public static String FeatureVersionCompare_errorReasonMsg;
	public static String FeatureVersionCompare_featureFileErrorMsg;
	public static String FeatureVersionCompare_incorrectNewVersionMsg;
	public static String FeatureVersionCompare_incorrectNewVersionMsg2;

	public static String FeatureVersionCompare_incorrectPluginVersionChange;
	public static String FeatureVersionCompare_incorrectPluginVersionMsg;
	public static String FeatureVersionCompare_inputErrorMsg;
	public static String FeatureVersionCompare_lowerNewVersion;
	public static String FeatureVersionCompare_nestedErrorOccurredMsg;
	public static String FeatureVersionCompare_newAddedFeaturePlingMsg;
	public static String FeatureVersionCompare_newIntroducedFeatureMsg;
	public static String FeatureVersionCompare_newVersionLowerThanOldMsg;
	public static String FeatureVersionCompare_noFeatureFoundMsg;
	public static String FeatureVersionCompare_noFeaturesFoundInConfigMsg;
	public static String FeatureVersionCompare_versionChangeIncorrectMsg;
	public static String FeatureVersionCompare_pluginNotFoundMsg;
	public static String FeatureVersionCompare_sourceIncludedFeatureNotFoundMsg;
	public static String FeatureVersionCompare_verifyingFeatureMsg;
	public static String JavaClassVersionCompare_classErrorOccurredMsg;
	public static String JavaClassVersionCompare_classFileNotLoadedMsg;
	public static String JavaClassVersionCompare_classMajorChangeMsg;
	public static String JavaClassVersionCompare_classMicroChange;
	public static String JavaClassVersionCompare_classMinorChangeMsg;
	public static String JavaClassVersionCompare_classModifierChangedMsg;
	public static String JavaClassVersionCompare_comparingClassMsg;
	public static String JavaClassVersionCompare_deletedInterfaceMsg;
	public static String JavaClassVersionCompare_deprecatedChangedMsg;
	public static String JavaClassVersionCompare_descriptorChangedMsg;
	public static String JavaClassVersionCompare_differentClassNameMsg;
	public static String JavaClassVersionCompare_differentSuperClassMsg;
	public static String JavaClassVersionCompare_inputStreamErrMsg;
	public static String JavaClassVersionCompare_ModifierChangedMsg;
	public static String JavaClassVersionCompare_newAddedExceptionMsg;
	public static String JavaClassVersionCompare_newAddedInterfaceMsg;
	public static String JavaClassVersionCompare_newAddedMsg;
	public static String JavaClassVersionCompare_noLongerExistExceptionMsg;
	public static String JavaClassVersionCompare_noLongerExistMsg;
	public static String JavaClassVersionCompare_unexpectedTypeMsg;

	public static String PluginVersionCompare_classPathJarNotFoundMsg;
	public static String PluginVersionCompare_comparingPluginMsg;
	public static String PluginVersionCompare_couldNotConvertManifestMsg;
	public static String PluginVersionCompare_couldNotOpenJarMsg;
	public static String PluginVersionCompare_couldNotParseHeaderMsg;
	public static String PluginVersionCompare_couldNotReadAttributesMsg;
	public static String PluginVersionCompare_couldNotReadClassMsg;
	public static String PluginVersionCompare_couldNotReadClassPathMsg;
	public static String PluginVersionCompare_couldNotReadManifestMsg;
	public static String PluginVersionCompare_destinationClassNotFoundMsg;
	public static String PluginVersionCompare_errorWhenCompareClassesMsg;
	public static String PluginVersionCompare_failCloseJarEntryAfterExtractMsg;
	public static String PluginVersionCompare_failCloseJarMsg;
	public static String PluginVersionCompare_failCloseTmpAfterCreateMsg;
	public static String PluginVersionCompare_failCreatTmpJarMsg;
	public static String PluginVersionCompare_failExtractJarEntryMsg;
	public static String PluginVersionCompare_failOpenJarEntryMsg;
	public static String PluginVersionCompare_failOpenTmpJarMsg;
	public static String PluginVersionCompare_finishedProcessPluginMsg;
	public static String PluginVersionCompare_inputNotExistMsg;
	public static String PluginVersionCompare_inValidClassPathMsg;
	public static String PluginVersionCompare_noPluginConverterInstanceMsg;
	public static String PluginVersionCompare_noValidClassFoundMsg;

	public static String PluginVersionCompare_pluginErrorOccurredMsg;
	public static String PluginVersionCompare_pluginMajorChangeMsg;
	public static String PluginVersionCompare_pluginMicroChangeMsg;
	public static String PluginVersionCompare_pluginMinorChangeMsg;
	public static String PluginVersionCompare_sourceClassNotFoundMsg;

	public static String VersionCompareDispatcher_closeFileFailedMsg;

	public static String VersionCompareDispatcher_failedCreateDocMsg;

	public static String VersionCompareDispatcher_failedWriteXMLFileMsg;
	public static String VersionCompareDispatcher_fileNotFoundMsg;

	public static String VersionCompareDispatcher_invalidXMLFileNameMsg;
	public static String VersionCompareDispatcher_readPropertyFailedMsg;
	public static String VersionVerifier_compareOKMsg;
	public static String VersionVerifier_coreExceptionMsg;
	public static String VersionVerifier_createFileErrorMsg;
	public static String VersionVerifier_messageNumberMsg;
	public static String VersionVerifier_mixedInputMsg;
	public static String VersionVerifier_pathIsNotFileMsg;
	public static String VersionVerifier_summaryMsg;
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Do not instantiate
	}
}