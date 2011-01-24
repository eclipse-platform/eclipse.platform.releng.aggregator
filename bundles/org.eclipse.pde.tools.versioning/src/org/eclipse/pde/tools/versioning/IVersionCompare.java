/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.tools.versioning;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.util.IClassFileReader;

/**
 * This interface provides methods to clients for verification of version numbers
 * for features and plug-ins contained within an Eclipse configuration or under a directory.
 * <p>
 * Clients may implement this interface. However, in most cases clients will use the provided 
 * factory to acquire an implementation for use.
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IVersionCompare {

	/**
	 * This String type constant is a property name in compare option file; 
	 * Its value indicates exclusive feature ids.
	 */
	public static final String EXCLUDE_FEATURES_OPTION = "exclude.features"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive feature ids.
	 */
	public static final String INCLUDE_FEATURES_OPTION = "include.features"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates exclusive plugin ids.
	 */
	public static final String EXCLUDE_PLUGINS_OPTION = "exclude.plugins"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive plugin ids.
	 */
	public static final String INCLUDE_PLUGINS_OPTION = "include.plugins"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates exclusive operation systems.
	 */
	public static final String EXCLUDE_OS_OPTION = "exclude.os"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive operation systems.
	 */
	public static final String INCLUDE_OS_OPTION = "include.os"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates exclusive windows systems.
	 */
	public static final String EXCLUDE_WS_OPTION = "exclude.ws"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive windows systems.
	 */
	public static final String INCLUDE_WS_OPTION = "include.ws"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates exclusive system architecture specifications.
	 */
	public static final String EXCLUDE_ARCH_OPTION = "exclude.arc"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive system architecture specifications.
	 */
	public static final String INCLUDE_ARCH_OPTION = "include.arc"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file; 
	 * Its value indicates exclusive locale language specifications.
	 */
	public static final String EXCLUDE_NL_OPTION = "exclude.nl"; //$NON-NLS-1$

	/**
	 * This String type constant is a property name in compare option file;
	 * Its value indicates inclusive locale language specifications
	 */
	public static final String INCLUDE_NL_OPTION = "include.nl"; //$NON-NLS-1$

	/**
	 * This int type constant indicates overall compare result of a feature
	 */
	public static final int FEATURE_OVERALL_STATUS = 0x0001;

	/**
	 * This int type constant indicates detail compare messages of a feature
	 */
	public static final int FEATURE_DETAIL_STATUS = 0x0002;

	/**
	 * This int type constant indicates overall compare result of a plugin
	 */
	public static final int PLUGIN_OVERALL_STATUS = 0x0004;

	/**
	 * This int type constant indicates detail compare messages of a plugin
	 */
	public static final int PLUGIN_DETAIL_STATUS = 0x0008;

	/**
	 * This int type constant indicates overall compare result of a class
	 */
	public static final int CLASS_OVERALL_STATUS = 0x0010;

	/**
	 * This int type constant indicates detail compare messages of a class
	 */
	public static final int CLASS_DETAIL_STATUS = 0x0020;

	/**
	 * This int type constant indicates process error messages (e.g. IO error)
	 */
	public static final int PROCESS_ERROR_STATUS = 0x0040;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates some error occurred during comparing
	 */
	public final static int ERROR_OCCURRED = 0x0080;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates a major change of a feature, plugin, or class
	 */
	public final static int MAJOR_CHANGE = 0x0100;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates a minor change of a feature, plugin, or class
	 */
	public final static int MINOR_CHANGE = 0x0200;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates there is one or more new added attribute in a feature, plugin, or class
	 */
	public final static int NEW_ADDED = 0x0400;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates there is one or more attribute have been deleted from a feature, plugin, or class
	 */
	public final static int NO_LONGER_EXIST = 0x0800;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates a minor change of a feature, plugin, or class
	 */
	public final static int MICRO_CHANGE = 0x1000;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates a qualifier change of a feature, plugin, or class
	 */
	public final static int QUALIFIER_CHANGE = 0x2000;

	/**
	 * This int type constant is a compare result of feature, plugin, class;
	 * It indicates no change in a feature, plugin, or class
	 */
	public final static int NO_CHANGE = 0x4000;

	/**
	 * Verify two sets of features and return a status object indicating whether or not the version numbers
	 * of the features (an optionally of their included plug-ins) have been incremented appropriated based
	 * on the changes between the two sets.
	 * <p>
	 * Feature Version update priority:
	 * Versions have four parts: major, minor, micro, and qualifier. 
	 * There are four corresponding kinds of version update: major update, 
	 * minor update, micro update, qualifier update. Besides, there are two 
	 * other kinds of update: new_added update(if some features or plugins 
	 * have been new added to the feature), and no_longer_exist update(if 
	 * some features or plugins have been deleted from the feature). Consequently, 
	 * major update has the highest priority, then minor update (currently 
	 * new_added and no_longer_exist are considered as minor updates), then 
	 * micro update, and qualifier update has the lowest priority. If we talk
	 * about version update of a feature or a plugin, we mean the update with 
	 * the highest priority.
	 * </p><p>
	 * Feature Version update rule: 
	 * Usually, a feature includes other features and plug-ins, 
	 * e.g. : 
	 *  old version of "f1":
	 * <pre>
	 * 	<?xml version="1.0" encoding="UTF-8"?>
	 * 		<feature id="f1" label="aaa" version="1.0.0.v2005">
	 * 			<includes id="f2" version="1.0.0.v2005" name="bbb"/>
	 *          <includes id="f3" version="1.0.0.v2005" name="ccc"/>
	 *          <plugin id="p1" version="1.0.0.v2005"/>
	 *          <plugin id="p2" version="1.0.0.v2005"/>
	 *          <plugin id="p3" version="1.0.0.v2005"/>
	 *      </feature>
	 * </pre>
	 *  new version of "f1":
	 *  <pre>
	 * 	<?xml version="1.0" encoding="UTF-8"?>
	 * 		<feature id="f1" label="aaa" version="3.0.0">
	 *  		<includes id="f2" version="2.0.0.v2005" name="bbb"/>
	 *          <includes id="f3" version="1.1.0.v2005" name="ccc"/>
	 *          <plugin id="p1" version="1.0.1.v2005"/>
	 *          <plugin id="p2" version="1.0.0.v2006"/>
	 *          <plugin id="p4" version="1.0.0.v2006"/>
	 *      </feature>
	 * </pre></p><p>
	 * The basic rule of version update is that if we have updated versions of 
	 * any include features(f2,f3) or plugins(p1,p2), we need to update the version 
	 * of f1. Update of f1 should be at least the highest update of its included 
	 * features and plug-ins. For instance, "f2" has a major update, "f3" has a minor 
	 * update, "p1" has a micro update, and "p2" has a qualifier update, "p3" no 
	 * longer exists in "f1", "p4" has been new added to "f1". In conclusion, "f1" 
	 * should at least have a major change(it's ok if "f1" also has a minor, micro,
	 * or qualifier update). 
	 * </p><p>
	 * Feature Verification process:
	 * To verify the new version of "f1", we need to compare its included features 
	 * and plug-ins with those included in its old version and find out the update
	 * with the highest priority. For included features, such as "f2" and "f3", before
	 * comparing them with their corresponding ones in old "f1", we need to verify
	 * their new versions first through recursion, and then,
	 * <ul>
	 * <li>if the new versions of "f2" is correct, we use the new version of "f2" to 
	 * compare with the version of "f2" in old "f1".
	 * <li>if the new versions of "f2" is wrong, we try to generate a recommended new
	 * version for "f2", and then,
	 *<li>if we can generate a recommended new version for "f2", we use the recommended
	 * new version to compare with the version of "f2" in old "f1".
	 * <li>if we can not generate a recommended new version for "f2", there must be
	 * some error occurred. We will create a warning message for that error and
	 * we will also create a message to tell user that "f1" can not be verified 
	 * correctly, since we can't verify some of its included features. (The process
	 * will not stop if we find an error. It will continually to compare and try 
	 * to find problems as more as possible, and warning messages will be collected,
	 * and displayed at last.)
	 * </ul>
	 * </p><p>
	 * We must increment the major part if:
	 * <ul>
	 * <li>an included feature has incremented the major part of its version
	 * <li>an included plug-in has incremented the major part of its version
	 * </ul>
	 * </p><p>
	 * We must increment the minor part if:
	 * <ul>
	 * <li>an included feature has incremented the minor part of its version
	 * <li>an included plug-in has incremented the minor part of its version
	 * <li>if a new plug-in was added to the feature
	 * <li>if a plug-in was removed from the feature
	 * <li>if a new included feature was added to the feature
	 * <li>if an included feature was removed from the feature
	 * </ul>
	 * </p><p>
	 * We must increment the micro part if:
	 * <ul>
	 * <li>an included feature has incremented the micro part of its version
	 * <li>an included plug-in has incremented the micro part of its version
	 * </ul>
	 * </p><p>
	 * We must increment the qualifier if:
	 * <ul>
	 * <li>an included feature has incremented the qualifier part of its version
	 * <li>an included plug-in has incremented the qualifier part of its version
	 * </ul>
	 * </p>
	 * 
	 * @param file1 File instance which denotes a configuration file or a feature directory
	 * @param file2 File instance which denotes a configuration file or a feature directory
	 * @param needPluginCompare if <code>true</code>, the method will compare plugin objects; if <code>false</code>
	 * 		  it just compares plugin names and versions.
	 * @return IStatus instance which contains child status. Each child status indicates 
	 *         an error or warning of one included feature or plug-in
	 * @param compareOptionFile a property file which indicates exclusion and inclusion for feature compare
	 * @param monitor IProgressMonitor instance which will monitor the progress during feature comparing
	 * @throws CoreException
	 * @see #checkFeatureVersions(String, String, boolean, File, IProgressMonitor)
	 * @see #checkFeatureVersions(URL, URL, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(File file1, File file2, boolean needPluginCompare, File compareOptionFile, IProgressMonitor monitor) throws CoreException;

	/**
	 * As per {@link #checkFeatureVersions(File, File, boolean, File, IProgressMonitor)} except the given parameters are strings
	 * pointing to the configuration file or features directory.
	 * 
	 * @param path1 path which denotes an eclipse configuration XML file or a feature directory
	 * @param path2 path which denotes an eclipse configuration XML file or a feature directory
	 * @param needPluginCompare if <code>true</code>, the method will compare plugin objects; if <code>false</code>
	 * 		  it just compares plugin names and versions.
	 * @param compareOptionFile a property file which indicates exclusion and inclusion for feature compare
	 * @param monitor IProgressMonitor instance which will monitor the progress during feature comparing
	 * @return a status containing the comparison result
	 * @throws CoreException
	 * @see #checkFeatureVersions(File, File, boolean, File, IProgressMonitor)
	 * @see #checkFeatureVersions(URL, URL, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(String path1, String path2, boolean needPluginCompare, File compareOptionFile, IProgressMonitor monitor) throws CoreException;

	/**
	 * As per {@link #checkFeatureVersions(File, File, boolean, File, IProgressMonitor)} except the given parameters are urls
	 * pointing to configuration files.
	 * 
	 * @param configURL1 reference to an eclipse configuration XML file
	 * @param configURL2 reference to an eclipse configuration XML file 
	 * @param needPluginCompare if <code>true</code>, the method will compare plugin objects; if <code>false</code>
	 * 		  it just compares plugin names and versions.
	 * @param compareOptionFile a property file which indicates exclusion and inclusion for feature compare
	 * @param monitor IProgressMonitor instance which will monitor the progress during feature comparing
	 * @return a status containing the comparison result
	 * @throws CoreException
	 * @see #checkFeatureVersions(File, File, boolean, File, IProgressMonitor)
	 * @see #checkFeatureVersions(String, String, boolean, File, IProgressMonitor)
	 */
	public IStatus checkFeatureVersions(URL configURL1, URL configURL2, boolean needPluginCompare, File compareOptionFile, IProgressMonitor monitor) throws CoreException;

	/**
	 * <p>
	 * Compares the two given Java class files and returns a ICompareResult object containing a summary
	 * of the changes between the two files.
	 * </P><p>
	 * Steps to compare two classes:
	 * <ul>
	 * <li>1. compares class names. If two classes do not have the same class name, we don't do
	 *    further compare.</li>
	 * <li>2. compares class modifiers.
	 * 	  <ul> 
	 * 	  <li>A. If the modifier of a class has been changed from public to non-public</li>
	 *    or from protected to modifier other than public and protected, it is a major change.</li>
	 *    <li>B. If the modifier of a class has been changed from non-abstract to abstract, it is a major change.</li>
	 *    <li>C. If the modifier of a class has been changed from non-final to final, it is a major change.</li></ul>
	 * </li>
	 * <li>3. compares super classes. If the super class has been changed, it is a major change.</li>
	 * <li>4. compares implemented interfaces. If implemented interfaces have been changed, it is a major change.</li>
	 * <li>5. compares fields.
	 * 	  <ul>
	 * 	  <li>A. If the modifier of a field has been changed from public to non-public
	 *    or from protected to modifier other than public and protected, it is a major change.</li>
	 *    <li>B. If the modifier of a field has been changed from non-final to final, it is a major change.</li>
	 *    <li>C. If the modifier of a field has been changed from static to non-static, it is a major change.</li>
	 *    <li>D. If the modifier of a field has been changed from volatile to non-volatile or vis-verser, it is a major change.</li>
	 *    <li>E. If a public or protected field has been deprecated, it is a micro change.</li>
	 *    <li>F. If a public or protected field has been new added into the class, it is a minor change.</li>
	 *    <li>G. If a public or protected field has been deleted from the class, it is a major change.</li>
	 *    <li>H. If the type of a field has been changed, it is a major change.</li>
	 *    </ul>
	 * <li>6. compares methods.
	 *    <ul>
	 * 	  <li>A. If the modifier of a method has been changed from public to non-public
	 *    or from protected to modifier other than public and protected, it is a major change.</li>
	 *    <li>B. If the modifier of a method has been changed from non-abstract to abstract, it is a major change.</li>
	 *    <li>C. If the modifier of a method has been changed from non-final to final, it is a major change.</li>
	 *    <li>D. If the modifier of a method has been changed from static to non-static, it is a major change.</li>
	 *    <li>E. If the modifier of a method has been changed from volatile to non-volatile or vis-verser, it is a major change.</li>
	 *    <li>F. If a public or protected method has been deprecated, it is a micro change.</li>
	 *    <li>G. If a public or protected method has been new added into the class, it is a minor change.</li>
	 *    <li>H. If a public or protected method has been deleted from the class, it is a major change.</li>
	 *    <li>I. If return type, number of args, or any type of args has been changed, it is considered
	 *       as the combination of a method has been deleted and a new method has been added(major change).</li>
	 *    <li>J. If thrown exceptions of a method have been changed, it is a minor change.</li>
	 *    </ul>
	 * </li>
	 * </ul></p>
	 * 
	 * @param javaClass1 Sting which denotes the full path of a java class file
	 * @param javaClass2 Sting which denotes the full path of a java class file
	 * @param monitor IProgressMonitor instance which will monitor the progress during class comparing
	 * @return ICompareResult instance which contains change on the class and messages generated when class is compared
	 * @throws CoreException if there was an exception during the comparison
	 */
	public ICompareResult checkJavaClassVersions(String javaClass1, String javaClass2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given Java class files and returns a ICompareResult object containing a summary
	 * of the changes between the two files.{@link #checkJavaClassVersions(String, String, IProgressMonitor)}
	 * 
	 * @param javaClassURL1 URL which denotes a java class file
	 * @param javaClassURL2 URL which denotes a java class file
	 * @param monitor IProgressMonitor instance which will monitor the progress during class comparing
	 * @return ICompareResult instance which contains change on the class and messages generated when class is compared
	 * @throws CoreException if there was an exception during the comparison
	 */
	public ICompareResult checkJavaClassVersions(URL javaClassURL1, URL javaClassURL2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given Java class files and returns a ICompareResult object containing a summary
	 * of the changes between the two files.{@link #checkJavaClassVersions(String, String, IProgressMonitor)}
	 * 
	 * @param javaClassFile1 File which denotes a java class file
	 * @param javaClassFile2 File which denotes a java class file
	 * @param monitor IProgressMonitor instance which will monitor the progress during class comparing
	 * @return ICompareResult instance which contains change on the class and messages generated when class is compared
	 * @throws CoreException if there was an exception during the comparison
	 */
	public ICompareResult checkJavaClassVersions(File javaClassFile1, File javaClassFile2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given Java class files and returns a ICompareResult object containing a summary
	 * of the changes between the two files.{@link #checkJavaClassVersions(String, String, IProgressMonitor)}
	 * <p>
	 * This method does <em>not</em> close the InputStreams and users should handle stream closure in the calling code.
	 * </p>
	 * 
	 * @param javaClassInputStream1 InputStream which denotes a java class file
	 * @param javaClassInputStream2 InputStream which denotes a java class file
	 * @param monitor IProgressMonitor instance which will monitor the progress during class comparing
	 * @return ICompareResult instance which contains change on the class and messages generated when class is compared
	 * @throws CoreException if there was an exception during the comparison
	 */
	public ICompareResult checkJavaClassVersions(InputStream javaClassInputStream1, InputStream javaClassInputStream2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given Java class files and returns a ICompareResult object containing a summary
	 * of the changes between the two files.{@link #checkJavaClassVersions(String, String, IProgressMonitor)}
	 * 
	 * @param classFileReader1 IClassFileReader instance which denotes a java class file
	 * @param classFileReader2 IClassFileReader instance which denotes a java class file
	 * @param monitor IProgressMonitor instance which will monitor the progress during class comparing
	 * @return ICompareResult instance which contains change on the class and messages generated when class is compared
	 * @throws CoreException if there was an exception during the comparison
	 */
	public ICompareResult checkJavaClassVersions(IClassFileReader classFileReader1, IClassFileReader classFileReader2, IProgressMonitor monitor) throws CoreException;

	/**
	 * <p>
	 * Compares the two given plug-ins which each other and reports a ICompareResult object indicating
	 * whether or not the version number of the plug-ins have been incremented appropriately
	 * based on the relative changes.<p>
	 * <p>
	 * Steps to compare two plugins:
	 * <ul>
	 * <li>1. compares all classes in the class path of the plugin to check if they have any change.</li>
	 * <li>2. if a new class has been added into the plugin, it is a minor change.</li>
	 * <li>3. if a class has been deleted from the plugin, it is a major change.</li>
	 * </ul>
	 * </p>
	 * @param plugin1 String which denotes the plug-in's jar file name or directory
	 * @param plugin2 String which denotes the plug-in's jar file name or directory
	 * @param monitor IProgressMonitor instance which will monitor the progress during plugin comparing
	 * @return ICompareResult instance which contains change on the plugin and messages generated when plugin is compared
	 * @throws CoreException if an error occurred during the comparison
	 */
	public ICompareResult checkPluginVersions(String plugin1, String plugin2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given plug-ins which each other and reports a ICompareResult object indicating
	 * whether or not the version number of the plug-ins have been incremented appropriately
	 * based on the relative changes.{@link #checkPluginVersions(String, String, IProgressMonitor)}
	 * 
	 * @param pluginURL1 URL which denotes a plug-in's jar file name or directory
	 * @param pluginURL2 URL which denotes a plug-in's jar file name or directory
	 * @param monitor IProgressMonitor instance which will monitor the progress during plugin comparing
	 * @return ICompareResult instance which contains change on the plugin and messages generated when plugin is compared
	 * @throws CoreException if an error occurred during the comparison
	 */
	public ICompareResult checkPluginVersions(URL pluginURL1, URL pluginURL2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Compares the two given plug-ins which each other and reports a ICompareResult object indicating
	 * whether or not the version number of the plug-ins have been incremented appropriately
	 * based on the relative changes.{@link #checkPluginVersions(String, String, IProgressMonitor)}
	 * 
	 * @param pluginFile1 File which denotes a plug-in's jar file name or directory
	 * @param pluginFile2 File which denotes a plug-in's jar file name or directory
	 * @param monitor IProgressMonitor instance which will monitor the progress during plugin comparing
	 * @return ICompareResult instance which contains change on the plugin and messages generated when plugin is compared
	 * @throws CoreException if an error occurred during the comparison
	 */
	public ICompareResult checkPluginVersions(File pluginFile1, File pluginFile2, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return a status object whose contents are filtered based on the given bit masks.
	 * <p>
	 * The value for <code>infoChoice</code> is an integer constructor from any combination 
	 * of the following bit masks:
	 * <ul>
	 * <li>{@link #FEATURE_OVERALL_STATUS}</li>
	 * <li>{@link #FEATURE_DETAIL_STATUS}</li>
	 * <li>{@link #PLUGIN_OVERALL_STATUS}</li>
	 * <li>{@link #PLUGIN_DETAIL_STATUS}</li>
	 * <li>{@link #CLASS_OVERALL_STATUS}</li>
	 * <li>{@link #CLASS_DETAIL_STATUS}</li>
	 * <li>{@link #PROCESS_ERROR_STATUS}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param status IStatus instance of compare result
	 * @param infoChoice integer specifying the filter to use
	 * @see #FEATURE_OVERALL_STATUS
	 * @see #FEATURE_DETAIL_STATUS
	 * @see #PLUGIN_OVERALL_STATUS
	 * @see #PLUGIN_DETAIL_STATUS
	 * @see #CLASS_OVERALL_STATUS
	 * @see #CLASS_DETAIL_STATUS
	 * @see #PROCESS_ERROR_STATUS
	 * @return the filtered status object
	 */
	public IStatus processCompareResult(IStatus status, int infoChoice);

	/**
	 * writes out children statuses of <code>status</code> to XML file denoted by <code>fileName</code>
	 * <p>
	 * The format of the XML file is as following:
	 * <pre>
	 * 	<CompareResult Version="1.0">
	 *  	<Category Name="Error" SeverityCode="4"> 
	 *  		<Info Code="1" Message="The version of the feature "org.eclipse.rcp" should be at least "3.3.0"." />
	 *  		...
	 *  	</Category>
	 *  	<Category Name="Warning" SeverityCode="2">
	 *  		...
	 *  	</Category>
	 *  	<Category Name="Information" SeverityCode="1">
	 *  		...
	 *  	</Category>
	 *  </CompareResult>
	 * </pre>
	 * </p>
	 * @param status IStatus instance
	 * @param fileName String name of a XML file
	 * @throws CoreException <p>if any nested CoreException has been caught</p>
	 */
	public void writeToXML(IStatus status, String fileName) throws CoreException;
}