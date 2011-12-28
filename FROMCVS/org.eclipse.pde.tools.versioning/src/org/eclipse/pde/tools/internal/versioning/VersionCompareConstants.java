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

public interface VersionCompareConstants {
	public static final String PLUGIN_ID = "org.eclipse.pde.tools.versioning"; //$NON-NLS-1$
	public final static String BUNDLE_MANIFEST = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	public final static String BUNDLE_CLASSPATH = "Bundle-ClassPath"; //$NON-NLS-1$
	public final static String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"; //$NON-NLS-1$
	public final static String BUNDLE_VERSION = "Bundle-Version"; //$NON-NLS-1$
	public final static String BUNDLE_LOCATION = "Bundle-Location"; //$NON-NLS-1$
	public final static String JAVA_TMP_DIR_PROPERTY = "java.io.tmpdir";//$NON-NLS-1$
	public final static String PLUGIN_DIR_NAME = "plugins"; //$NON-NLS-1$
	public static final String FEATURES_FILE_NAME = "feature.xml"; //$NON-NLS-1$
	public static final String CONFIGURATION_FILE_NAME = "platform.xml"; //$NON-NLS-1$
	public static final String JAVA_FILE_EXTENSION = "java"; //$NON-NLS-1$
	public static final String CLASS_FILE_EXTENSION = "class"; //$NON-NLS-1$
	public static final String JAR_FILE_EXTENSION = "jar"; //$NON-NLS-1$
	public static final String PLATFORM_PROTOCOL = "platform"; //$NON-NLS-1$
	public static final String FEATURE_TITLE = "feature"; //$NON-NLS-1$
	public static final String PLUGIN_TITLE = "plugin"; //$NON-NLS-1$
	public static final String FIELD_TITLE = "field"; //$NON-NLS-1$
	public static final String METHOD_TITLE = "method"; //$NON-NLS-1$
	public static final String JAR_URL_HEAD = "jar:file:"; //$NON-NLS-1$
	public static final String JAR_URL_SEPARATOR = "!/"; //$NON-NLS-1$
	public static final String WILD_CAST_STRING = ".*"; //$NON-NLS-1$
	public static final String DOT_QUOTE_STRING = "\\."; //$NON-NLS-1$
	public static final int DEFAULT_MODIFIER_TESTER = 0x0007;
	public static final char START_CHAR = '*';
	public static final char DOT_CHAR = '.';
	public static final String ENCODING_TYPE = "UTF-8"; //$NON-NLS-1$
	public static final String ROOT_ELEMENT_NAME = "CompareResult"; //$NON-NLS-1$
	public static final String SEVERITY_ELEMENT_NAME = "Category"; //$NON-NLS-1$
	public static final String CHILDREN_ELEMENT_NAME = "Info"; //$NON-NLS-1$
	public static final String CODE_ATTRIBUTE_NAME = "Code"; //$NON-NLS-1$
	public static final String MESSAGE_ATTRIBUTE_NAME = "Message"; //$NON-NLS-1$
	public static final String XML_FILE_EXTENSION = "xml"; //$NON-NLS-1$
	// marks
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	public final static String DOLLAR_MARK = "$";//$NON-NLS-1$
	public final static String DOT_MARK = ".";//$NON-NLS-1$
	public static final String COMMA_MARK = ","; //$NON-NLS-1$
	public static final String UNDERSCORE_MARK = "_"; //$NON-NLS-1$
	public static final String KEY_SEPARATOR = "#"; //$NON-NLS-1$
	public static final String SPACE = " "; //$NON-NLS-1$
	public static final String START_MARK = "*"; //$NON-NLS-1$
	// modifier String
	public static final String ABSTRACT_STRING = "abstract"; //$NON-NLS-1$
	public static final String PUBLIC_STRING = "public"; //$NON-NLS-1$
	public static final String PRIVATE_STRING = "private"; //$NON-NLS-1$
	public static final String FINAL_STRING = "final"; //$NON-NLS-1$
	public static final String DEFAULT_STRING = "default"; //$NON-NLS-1$
	public static final String STATIC_STRING = "static"; //$NON-NLS-1$
	public static final String PROTECTED_STRING = "protected"; //$NON-NLS-1$
	public static final String VOLATILE_STRING = "volatile"; //$NON-NLS-1$
	public static final String NON_ABSTRACT_STRING = "non-abstract"; //$NON-NLS-1$
	public static final String NON_STATIC_STRING = "non-static"; //$NON-NLS-1$
	public static final String NON_FINAL_STRING = "non-final"; //$NON-NLS-1$
	public static final String NON_VOLATILE_STRING = "non-volatile"; //$NON-NLS-1$
}
