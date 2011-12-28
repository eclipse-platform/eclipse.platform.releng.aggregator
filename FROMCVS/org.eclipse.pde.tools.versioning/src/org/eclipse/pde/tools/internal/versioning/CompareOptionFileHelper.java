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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.PluginEntryModel;

/**
 * This class provides methods to process compare option property file, store the inclusive and exclusive
 * information, check if a feature of plugin is need to be compared.
 * <p>
 * The properties in the compare option file are as following:
 * <ul>
 * <li>"exclude.os" - which indicates exclusive Operation Systems
 * <li>"include.os" - which indicates inclusive Operation Systems
 * <li>"exclude.ws" - which indicates exclusive windows system architecture specifications
 * <li>"include.ws" - which indicates inclusive windows system architecture specifications
 * <li>"exclude.arch" - which indicates exclusive optional system architecture specifications
 * <li>"include.arch" - which indicates inclusive optional system architecture specifications
 * <li>"exclude.nl" - which indicates exclusive optional locale language specifications
 * <li>"include.nl" - which indicates inclusive optional locale language specifications
 * <li>"exclude.features" - which indicates exclusive feature ids
 * <li>"include.features" - which indicates inclusive feature ids
 * <li>"exclude.plugins" - which indicates exclusive plugin ids
 * <li>"include.plugins" - which indicates inclusive plugin ids
 * </ul>
 * </p>
 *
 */
public class CompareOptionFileHelper implements VersionCompareConstants {

	private Map optionTable = null;

	/**
	 * constructor
	 * @param file compare option file
	 * @throws CoreException <p>if nested CoreException has been thrown 
	 */
	public CompareOptionFileHelper(File file) throws CoreException {
		if (file == null)
			return;
		processVersioningOptionFile(file);
	}

	/**
	 * processes the compare option file denoted by <code>file</code>, and put 
	 * property key as key, List of property values of the property key as value
	 * into a map.
	 * 
	 * 
	 * @param file compare option file
	 * @throws CoreException if <code>file</code> does not exist or any IOException has been thrown 
	 */
	private void processVersioningOptionFile(File file) throws CoreException {
		Map table = new Hashtable();
		InputStream inputStream = null;
		Properties properties = new Properties();
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file));
			properties.load(inputStream);
		} catch (FileNotFoundException fnfe) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_fileNotFoundMsg, file.getAbsolutePath()), fnfe));
		} catch (IOException ioe) {
			throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_readPropertyFailedMsg, file.getAbsolutePath()), ioe));
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ioe) {
					throw new CoreException(new Status(IStatus.ERROR, VersionCompareConstants.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.VersionCompareDispatcher_closeFileFailedMsg, file.getAbsolutePath()), ioe));
				}
			}
		}
		for (Iterator iterator = properties.keySet().iterator(); iterator.hasNext();) {
			Object key = iterator.next();
			String property = properties.getProperty((String) key);
			if (property == null || property.trim().equals(VersionCompareConstants.EMPTY_STRING))
				continue;
			List propertyValueList;
			// for feature or plugin inclusion, exclusion values, we convert them into Patterns
			if (key.equals(IVersionCompare.INCLUDE_FEATURES_OPTION) || key.equals(IVersionCompare.EXCLUDE_FEATURES_OPTION) || key.equals(IVersionCompare.INCLUDE_PLUGINS_OPTION) || key.equals(IVersionCompare.EXCLUDE_PLUGINS_OPTION))
				propertyValueList = generatePatternList(Arrays.asList(property.split(VersionCompareConstants.COMMA_MARK)));
			else
				propertyValueList = Arrays.asList(property.split(VersionCompareConstants.COMMA_MARK));
			table.put(key, propertyValueList);
		}
		this.optionTable = table;
	}

	/*
	 * check the includes/excludes filters based on operating system
	 */
	private boolean filterOS(Object object) {
		List inclusionValueList = (List) optionTable.get(IVersionCompare.INCLUDE_OS_OPTION);
		String[] propertyValue;
		if (object instanceof IIncludedFeatureReference)
			propertyValue = ((IIncludedFeatureReference) object).getOS() == null ? null : ((IIncludedFeatureReference) object).getOS().split(COMMA_MARK);
		else if (object instanceof PluginEntryModel)
			propertyValue = ((PluginEntryModel) object).getOS() == null ? null : ((PluginEntryModel) object).getOS().split(COMMA_MARK);
		else
			propertyValue = ((FeatureModel) object).getOS() == null ? null : ((FeatureModel) object).getOS().split(COMMA_MARK);
		if (!inclusionMatch(propertyValue, inclusionValueList))
			return false;
		// inclusionMatch is true, do exclusionMatch
		return !exclusionMatch(propertyValue, (List) optionTable.get(IVersionCompare.EXCLUDE_OS_OPTION));
	}

	/*
	 * check the includes/excludes filters based on windowing system
	 */
	private boolean filterWS(Object object) {
		List inclusionValueList = (List) optionTable.get(IVersionCompare.INCLUDE_WS_OPTION);
		String[] propertyValue;
		if (object instanceof IIncludedFeatureReference)
			propertyValue = ((IIncludedFeatureReference) object).getWS() == null ? null : ((IIncludedFeatureReference) object).getWS().split(COMMA_MARK);
		else if (object instanceof PluginEntryModel)
			propertyValue = ((PluginEntryModel) object).getWS() == null ? null : ((PluginEntryModel) object).getWS().split(COMMA_MARK);
		else
			propertyValue = ((FeatureModel) object).getWS() == null ? null : ((FeatureModel) object).getWS().split(COMMA_MARK);
		if (!inclusionMatch(propertyValue, inclusionValueList))
			return false;
		// inclusionMatch is true, do exclusionMatch
		return !exclusionMatch(propertyValue, (List) optionTable.get(IVersionCompare.EXCLUDE_WS_OPTION));
	}

	/*
	 * check the includes/excludes filters based on system architecture
	 */
	private boolean filterArch(Object object) {
		List inclusionValueList = (List) optionTable.get(IVersionCompare.INCLUDE_ARCH_OPTION);
		String[] propertyValue;
		if (object instanceof IIncludedFeatureReference)
			propertyValue = ((IIncludedFeatureReference) object).getOSArch() == null ? null : ((IIncludedFeatureReference) object).getOSArch().split(COMMA_MARK);
		else if (object instanceof PluginEntryModel)
			propertyValue = ((PluginEntryModel) object).getOSArch() == null ? null : ((PluginEntryModel) object).getOSArch().split(COMMA_MARK);
		else
			propertyValue = ((FeatureModel) object).getOSArch() == null ? null : ((FeatureModel) object).getOSArch().split(COMMA_MARK);
		if (!inclusionMatch(propertyValue, inclusionValueList))
			return false;
		// inclusionMatch is true, do exclusionMatch
		return !exclusionMatch(propertyValue, (List) optionTable.get(IVersionCompare.EXCLUDE_ARCH_OPTION));
	}

	/*
	 * check the includes/excludes filters based on the language
	 */
	private boolean filterNL(Object object) {
		List inclusionValueList = (List) optionTable.get(IVersionCompare.INCLUDE_NL_OPTION);
		String[] propertyValue;
		if (object instanceof IIncludedFeatureReference)
			propertyValue = ((IIncludedFeatureReference) object).getNL() == null ? null : ((IIncludedFeatureReference) object).getNL().split(COMMA_MARK);
		else if (object instanceof PluginEntryModel)
			propertyValue = ((PluginEntryModel) object).getNL() == null ? null : ((PluginEntryModel) object).getNL().split(COMMA_MARK);
		else
			propertyValue = ((FeatureModel) object).getNL() == null ? null : ((FeatureModel) object).getNL().split(COMMA_MARK);
		if (!inclusionMatch(propertyValue, inclusionValueList))
			return false;
		// inclusionMatch is true, do exclusionMatch
		return !exclusionMatch(propertyValue, (List) optionTable.get(IVersionCompare.EXCLUDE_NL_OPTION));
	}

	/*
	 * check the includes/excludes filters based on feature identifiers
	 */
	private boolean filterFeatures(Object object) throws CoreException {
		List inclusionPatternList = (List) optionTable.get(IVersionCompare.INCLUDE_FEATURES_OPTION);
		String id;
		if (object instanceof IIncludedFeatureReference)
			id = ((IIncludedFeatureReference) object).getVersionedIdentifier().getIdentifier();
		else
			id = ((FeatureModel) object).getFeatureIdentifier();
		if (inclusionPatternList != null) {
			if (isMatch(id, inclusionPatternList)) {
				// inclusion match, do exclusion match
				List exclusionPatternList = (List) optionTable.get(IVersionCompare.EXCLUDE_FEATURES_OPTION);
				if (exclusionPatternList != null && isMatch(id, exclusionPatternList))
					return false;
			} else
				return false;
		} else {
			// do exclusion match
			List exclusionPatternList = (List) optionTable.get(IVersionCompare.EXCLUDE_FEATURES_OPTION);
			if (exclusionPatternList != null && isMatch(id, exclusionPatternList))
				return false;
		}
		return true;
	}

	/*
	 * check the includes/excludes filters based on plugin identifiers
	 */
	private boolean filterPlugins(Object object) {
		List inclusionPatternList = (List) optionTable.get(IVersionCompare.INCLUDE_PLUGINS_OPTION);
		String id = ((PluginEntryModel) object).getPluginIdentifier();
		if (inclusionPatternList != null) {
			if (isMatch(id, inclusionPatternList)) {
				// inclusion match, do exclusion match
				List exclusionPatternList = (List) optionTable.get(IVersionCompare.EXCLUDE_PLUGINS_OPTION);
				if (exclusionPatternList != null && isMatch(id, exclusionPatternList))
					return false;
			} else
				return false;
		} else {
			// do exclusion match
			List exclusionPatternList = (List) optionTable.get(IVersionCompare.EXCLUDE_PLUGINS_OPTION);
			if (exclusionPatternList != null && isMatch(id, exclusionPatternList))
				return false;
		}
		return true;
	}

	/**
	 * Returns a boolean value indicating whether or not the given object should be compared
	 * based on the includes and excludes filters.
	 * <p>
	 * The parameter can be of any of the following types:
	 * <ul>
	 * <li><code>IIncludedFeatureReference</code></li>
	 * <li><code>FeatureModel</code></li>
	 * <li><code>PluginEntryModel</code></li>
	 * </ul>
	 * </p>
	 * 
	 * @param object the object to check
	 * @return <code>true</code> if we should compare and <code>false</code> otherwise
	 * @throws CoreException <p> if any nested CoreException has been thrown</p>
	 */
	public boolean shouldCompare(Object object) throws CoreException {
		if (!((object instanceof IIncludedFeatureReference) || (object instanceof PluginEntryModel) || (object instanceof FeatureModel)))
			return false;
		if (optionTable == null || optionTable.size() == 0)
			// if inclusionTable is null, means everything need to be compared
			return true;

		// check the includes/excludes filters based on operating system
		if (!filterOS(object))
			return false;

		// check the includes/excludes filters based on windowing system
		if (!filterWS(object))
			return false;

		// check the includes/excludes filters based on system architecture
		if (!filterArch(object))
			return false;

		// check the includes/excludes filters based on the language
		if (!filterNL(object))
			return false;

		// check the includes/excludes filters based on feature or plugin identifiers
		if ((object instanceof IIncludedFeatureReference) || (object instanceof FeatureModel))
			return filterFeatures(object);
		else
			return filterPlugins(object);
	}

	/**
	 * checks whether <code>list1</code> contains at least one element of <code>list2</code>
	 * @param list1 List
	 * @param list2 List
	 * @return <code>true</code>if <code>list1</code> contains at least one element of <code>list2</code>,
	 * 	       <code>false</code> otherwise
	 */
	private boolean isIntersecting(List list1, List list2) {
		for (Iterator iterator = list2.iterator(); iterator.hasNext();) {
			Object key = iterator.next();
			if (list1.contains(key))
				return true;
		}
		return false;
	}

	/**
	 * check whether given <code>values</code> and <code>inclusionValueList</code> has an intersection
	 * @param values String array
	 * @param inclusionValueList inclusion value list
	 * @return <code>true</code> if given <code>values</code> and <code>inclusionValueList</code> has an intersection
	 * 		   <code>false</code> otherwise
	 */
	private boolean inclusionMatch(String[] values, List inclusionValueList) {
		if (values == null || inclusionValueList == null)
			return true;
		List propertyValue = Arrays.asList(values);
		if (!isIntersecting(inclusionValueList, propertyValue))
			// if propertyValue and inclusionValueList does not an intersection, return false 
			return false;
		return true;
	}

	/**
	 * check whether given <code>values</code> are included the <code>exclusionValueList</code>
	 * @param values String array
	 * @param exclusionValueList exclusion value list
	 * @return <code>true</code> if given <code>values</code> are included the <code>exclusionValueList</code>
	 * 		   <code>false</code> otherwise
	 */
	private boolean exclusionMatch(String[] values, List exclusionValueList) {
		if (values == null || exclusionValueList == null)
			return false;
		List propertyValue = Arrays.asList(values);
		if (!isIncluded(exclusionValueList, propertyValue))
			// if propertyValue are not included in exclusionValuList, return false
			return false;
		return true;
	}

	/**
	 * checks whether <code>list1</code> contains all the elements of <code>list2</code>
	 * @param list1 List
	 * @param list2 List
	 * @return <code>true</code>if <code>list1</code> contains all the elements of <code>list2</code>,
	 * 	       <code>false</code> otherwise
	 */
	private boolean isIncluded(List list1, List list2) {
		for (Iterator iterator = list2.iterator(); iterator.hasNext();) {
			Object key = iterator.next();
			if (!list1.contains(key))
				return false;
		}
		return true;
	}

	/**
	 * checks whether the <code>string</code> matches the <code>regex</code>
	 * @param string String need to be matched
	 * @param patternList List contains Patterns
	 * @return <code>true</code>if the <code>string</code> matches the <code>regex</code>
	 * 		   <code>false</code>otherwise
	 */
	private boolean isMatch(String string, List patternList) {
		for (Iterator iterator = patternList.iterator(); iterator.hasNext();) {
			Pattern pattern = (Pattern) iterator.next();
			Matcher matcher = pattern.matcher(string);
			if (matcher.matches())
				return true;
		}
		return false;
	}

	/**
	 * generates Pattern instances from each String in <code>list</code>
	 * @param list List contains Strings
	 * @return List contains Pattern instances
	 */
	private List generatePatternList(List list) {
		List newList = new ArrayList();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			newList.add(Pattern.compile(generateRegex((String) iterator.next())));
		}
		return newList;
	}

	/**
	 * generates regular expression from <code>string</code>
	 * @param string
	 * @return regular expression
	 */
	private String generateRegex(String string) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char singleChar = string.charAt(i);
			if (singleChar == START_CHAR)
				// convert '*' to ".*"
				buffer.append(WILD_CAST_STRING);
			else if (singleChar == DOT_CHAR)
				// convert '.' to "\."
				buffer.append(DOT_QUOTE_STRING);
			else
				buffer.append(singleChar);
		}
		return buffer.toString();
	}
}
