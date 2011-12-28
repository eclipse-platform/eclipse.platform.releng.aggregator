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
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.tools.versioning.IVersionCompare;
import org.eclipse.pde.tools.versioning.VersionCompareFactory;

/**
 * This is the headless version compare application.
 */
public class VersionVerifier implements IPlatformRunnable {
	// path of new configuration file or feature directory
	File newPath;
	// path of old configuration file or feature directory
	File oldPath;
	// if need to compare plugins as objects
	boolean needPluginCompare = false;
	// path of the option file
	String optionFilePath;
	// path of the result file
	String resultPath;
	boolean isConfiguration = false;
	boolean isDir = false;
	boolean consoleOutput = false;

	/*
	 * Prints the results using the given print writer. Closes the writer
	 * after processing.
	 */
	private void printResult(IStatus status, PrintWriter writer) {
		try {
			// write out compare results to result file and also display them on screen
			writer.println(Messages.VersionVerifier_summaryMsg);
			if (status.isOK()) {
				writer.println(Messages.VersionVerifier_compareOKMsg);
			} else {
				IStatus[] childStatus = status.getChildren();
				writer.println();
				if (childStatus.length == 0) {
					writer.println(status.getMessage());
				} else {
					for (int i = 0; i < childStatus.length; i++) {
						if (childStatus[i].isOK()) {
							continue;
						}
						if (childStatus[i].getException() != null) {
							String msg = childStatus[i].getMessage();
							if (!msg.equals("")) { //$NON-NLS-1$
								writer.println(msg);
							}
							writer.println(childStatus[i].getException().getMessage());
						} else {
							writer.println(childStatus[i].getMessage());
						}
					}
					writer.println();
					writer.println(NLS.bind(Messages.VersionVerifier_messageNumberMsg, String.valueOf(childStatus.length)));
				}
			}
		} finally {
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}
	}

	/*
	 * Print out the given status object.
	 */
	private void printResult(IStatus status, IVersionCompare versionCompare) {
		// create a File instance for the result file
		File resultFilePath = new File(resultPath);
		File parentFilePath = null;
		try {
			if (!resultFilePath.exists()) {
				// if the result file is not exist, we need to check its parent file
				parentFilePath = resultFilePath.getParentFile();
				if (parentFilePath.exists()) {
					// if the parent file exists, we create the result file directly
					resultFilePath.createNewFile();
				} else {
					// if the parent file does not exist, we create the parent file(directory) first
					parentFilePath.mkdirs();
					// and then create the result file
					resultFilePath.createNewFile();
				}
			} else {
				if (!resultFilePath.isFile()) {
					System.out.println(NLS.bind(Messages.VersionVerifier_createFileErrorMsg, Messages.VersionVerifier_pathIsNotFileMsg));
					return;
				}
			}
		} catch (IOException ioe) {
			System.out.println(NLS.bind(Messages.VersionVerifier_createFileErrorMsg, resultFilePath));
			return;
		}
		// write the results to a XML file
		try {
			versionCompare.writeToXML(status, resultFilePath.getAbsolutePath());
		} catch (CoreException ce) {
			ce.printStackTrace();
		}
		// check to see if the user requested console output as well
		if (consoleOutput)
			printResult(status, new PrintWriter(System.out));
	}

	/*
	 * Print out a message describing how to run the application. 
	 */
	private void printUsage() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Usage: java -cp startup.jar org.eclipse.core.launcher.Main -application org.eclipse.pde.tools.versioning.application -clean -new [path] -old [path] -option [path] -output [path] [-pluginCompare] [-consoleOutput]"); //$NON-NLS-1$
		buffer.append("\n-new: path of new configuration file or features directory"); //$NON-NLS-1$
		buffer.append("\n-old: path of old configuration file or features directory"); //$NON-NLS-1$
		buffer.append("\n-option: path of compare option file (optional)"); //$NON-NLS-1$
		buffer.append("\n-output: path of result XML file"); //$NON-NLS-1$
		buffer.append("\n-pluginCompare: if need to compare plugins as objects (optional)"); //$NON-NLS-1$
		buffer.append("\n-consoleOutput: print results to the system console (optional)"); //$NON-NLS-1$
		System.out.println(buffer.toString());
	}

	/*
	 * Look at the given file path and determine if it is a configuration file
	 * or a features directory. Return the associated File object.
	 */
	private File processPath(String pathString) {
		IPath path = new Path(pathString);
		File file = path.toFile();
		// check to see if we are pointing to a configuration file
		if ("platform.xml".equalsIgnoreCase(path.lastSegment())) //$NON-NLS-1$
			isConfiguration = true;
		// check if its a directory
		isDir = file.isDirectory();
		return file;
	}

	/*
	 * Process the command-line arguments and set up the local variables
	 * for the application.
	 */
	private boolean processCommandLine(String[] parameters) {
		// get parameters
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equalsIgnoreCase("-new")) { //$NON-NLS-1$
				if ((i + 1 < parameters.length) && !parameters[i + 1].startsWith("-")) { //$NON-NLS-1$
					i++;
					newPath = processPath(parameters[i]);
					if (isConfiguration && isDir) {
						System.out.println(Messages.VersionVerifier_mixedInputMsg);
						return false;
					}
				}
			} else if (parameters[i].equalsIgnoreCase("-old")) { //$NON-NLS-1$
				if ((i + 1 < parameters.length) && !parameters[i + 1].startsWith("-")) { //$NON-NLS-1$
					i++;
					oldPath = processPath(parameters[i]);
					if (isConfiguration && isDir) {
						System.out.println(Messages.VersionVerifier_mixedInputMsg);
						return false;
					}
				}
			} else if (parameters[i].equalsIgnoreCase("-option")) { //$NON-NLS-1$
				if ((i + 1 < parameters.length) && !parameters[i + 1].startsWith("-")) { //$NON-NLS-1$
					i++;
					optionFilePath = parameters[i];
				}
			} else if (parameters[i].equalsIgnoreCase("-output")) { //$NON-NLS-1$
				if ((i + 1 < parameters.length) && !parameters[i + 1].startsWith("-")) { //$NON-NLS-1$
					i++;
					resultPath = parameters[i];
				}
			} else if (parameters[i].equalsIgnoreCase("-pluginCompare")) { //$NON-NLS-1$
				needPluginCompare = true;
			} else if (parameters[i].equalsIgnoreCase("-consoleOutput")) { //$NON-NLS-1$
				consoleOutput = true;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) {
		// cast the args object into String array
		boolean ok = processCommandLine((String[]) args);
		// if any necessary parameter is missed, we display help message to tell
		// user how to run this application in command line
		if (!ok || newPath == null || oldPath == null || resultPath == null) {
			printUsage();
			return null;
		}
		// do the work, compare features included in new configuration(or feature directory) 
		// with those included in old configuration(or feature directory)
		IVersionCompare ivc = new VersionCompareFactory().getVersionCompare();
		IStatus status = null;
		try {
			status = ivc.checkFeatureVersions(newPath, oldPath, needPluginCompare, optionFilePath == null || optionFilePath.trim().equals("") ? null : new File(optionFilePath), null); //$NON-NLS-1$
		} catch (CoreException ce) {
			System.out.print(Messages.VersionVerifier_coreExceptionMsg);
			System.out.println(ce.getMessage());
			return null;
		} catch (Exception e){
			e.printStackTrace();
		}
		// print out the results
		printResult(status, ivc);
		return null;
	}
}
