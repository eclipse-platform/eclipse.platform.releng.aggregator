/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class FixCopyrightAction implements IObjectActionDelegate {

	public class MyInnerClass
		implements IResourceVisitor {

			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE) {
					processFile((IFile) resource);
				}
				return true;
			}
	}
	
	private String propertiesCopyright;
	private String javaCopyright;
	private String newLine = System.getProperty("line.separator");
	private Map log = new HashMap();

	
	// The current selection
	protected IStructuredSelection selection;

	/**
	 * Constructor for Action1.
	 */
	public FixCopyrightAction() {
		super();
	}

	/**
	 * Returns the selected resources.
	 * 
	 * @return the selected resources
	 */
	protected IResource[] getSelectedResources() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof IResource) {
					resources.add(next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(IResource.class);
					if (adapter instanceof IResource) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			IResource[] result = new IResource[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new IResource[0];
	}
	
	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		
		log = new HashMap();
		
		System.out.println("Start Fixing Copyrights");
		IResource[] results = getSelectedResources();
		System.out.println("Resources selected: " + results.length);
		for (int i = 0; i < results.length; i++) {
			IResource resource = results[i];
			System.out.println(resource.getName());
			try {
				resource.accept(new MyInnerClass());
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
		}
	
		writeLogs();
//		displayLogs();
		System.out.println("Done Fixing Copyrights");

	}

	/**
	 * 
	 */
	private void writeLogs() {
		
		FileOutputStream aStream;
		try {
			File aFile = new File(Platform.getLocation().toFile(), "copyrightLog.txt");
			aStream = new FileOutputStream(aFile);
			Set aSet = log.entrySet();
			Iterator errorIterator = aSet.iterator();
			while (errorIterator.hasNext()) {
				Map.Entry anEntry = (Map.Entry) errorIterator.next();
				String errorDescription = (String) anEntry.getKey();
				aStream.write(errorDescription.getBytes());
				aStream.write(newLine.getBytes());
				List fileList = (List) anEntry.getValue();
				Iterator listIterator = fileList.iterator();
				while (listIterator.hasNext()) {
					String fileName = (String) listIterator.next();
					aStream.write("     ".getBytes());
					aStream.write(fileName.getBytes());
					aStream.write(newLine.getBytes());
				}
			}
			aStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void displayLogs() {
		
		Set aSet = log.entrySet();
		Iterator errorIterator = aSet.iterator();
		while (errorIterator.hasNext()) {
			Map.Entry anEntry = (Map.Entry) errorIterator.next();
			String errorDescription = (String) anEntry.getKey();
			System.out.println(errorDescription);
			List fileList = (List) anEntry.getValue();
			Iterator listIterator = fileList.iterator();
			while (listIterator.hasNext()) {
				String fileName = (String) listIterator.next();
				System.out.println("     " + fileName);
			}
		}
	}

	/**
	 * @param file
	 */
	private void processFile(IFile file) {
		
		SourceFile aSourceFile;
		String copyright;
		
		String extension = file.getFileExtension();
		if (extension == null) {
			return;
		}
		
		extension = extension.toLowerCase();
		if (extension.equals("java")) {
			aSourceFile = new JavaFile(file);
			copyright = getJavaCopyright();
		} else if (extension.equals("properties")) {
			aSourceFile = new PropertiesFile(file);
			copyright=getPropertiesCopyright();
		} else {
			return;
		}
		
		SourceFile aJavaFile = new JavaFile(file);
		
		if (aSourceFile.hasMultipleCopyrights()) {
			warn(file, null, "Multiple copyrights found.  File UNCHANGED.");
			return;
		}
		
		BlockComment firstBlockComment = aSourceFile.firstBlockComment();
		
		// No Block Comments
		if (firstBlockComment == null) {
			// No block comments so just insert new copyright at top of file.
			aSourceFile.insert(copyright);
			return;
		}
		
		BlockComment firstCopyrightComment = aSourceFile.firstCopyrightComment();
		if (firstCopyrightComment == null) {
			// No copy right comments.  Insert new copyright at top of file.
			aSourceFile.insert(copyright);
			if (firstBlockComment.atTop()) {
				// If the first thing in the file was a non copyrght block comment raise a warning.
				warn(file, firstBlockComment, "Non-copyright block comment at BOF.  New copyright inserted above it.");
			}
			return;
		}
		
		if (firstCopyrightComment.notIBM()) {
			warn(file, firstCopyrightComment, "Not an IBM Copyright.  File UNCHANGED: " + firstCopyrightComment.getCopyrightHolder());
			return;
		}
		
		if (!firstCopyrightComment.nonIBMContributors().isEmpty()) {
			warn(file, firstCopyrightComment, "Had non IBM Contributors.  File UNCHANGED");
			return;
		}

		// Remove old copyright and place new copyright at top of file.
		aSourceFile.replace(firstCopyrightComment, copyright);
		
		if (!firstCopyrightComment.atTop()) {
			// The old copyright was NOT at the top of the file.  Give a warning.
			warn(file, firstCopyrightComment, "Old copyright not at BOF.  New copyright inserted at BOF.  Old copyright removed.");
		}
	}

	private void warn(IFile file, BlockComment firstBlockComment, String errorDescription) {
		List aList = (List) log.get(errorDescription);
		if (aList == null) {
			aList = new ArrayList();
			log.put(errorDescription, aList);
		}
		aList.add(file.getName());
	}

	/**
	 * 
	 */
	private String getJavaCopyright() {
		if (javaCopyright == null) {
			String newLine = System.getProperty("line.separator");
			StringWriter aWriter = new StringWriter();
			
			aWriter.write("/*******************************************************************************");
			aWriter.write(newLine);
			aWriter.write(" * Copyright (c) 2000, 2004 IBM Corporation and others.");
			aWriter.write(newLine);
			aWriter.write(" * All rights reserved. This program and the accompanying materials ");
			aWriter.write(newLine);
			aWriter.write(" * are made available under the terms of the Common Public License v1.0");
			aWriter.write(newLine); 
			aWriter.write(" * which accompanies this distribution, and is available at");
			aWriter.write(newLine);
			aWriter.write(" * http://www.eclipse.org/legal/cpl-v10.html");
			aWriter.write(newLine);
			aWriter.write(" * ");
			aWriter.write(newLine);
			aWriter.write(" * Contributors:");
			aWriter.write(newLine);
			aWriter.write(" *     IBM Corporation - initial API and implementation");
			aWriter.write(newLine);
			aWriter.write(" *******************************************************************************/");
			aWriter.write(newLine);
			javaCopyright = aWriter.toString();
		}
		return javaCopyright.toString();
	}

	private String getPropertiesCopyright() {
		if (propertiesCopyright == null) {
			String newLine = System.getProperty("line.separator");
			StringWriter aWriter = new StringWriter();
			
			aWriter.write("###############################################################################");
			aWriter.write(newLine);
			aWriter.write("# Copyright (c) 2000, 2004 IBM Corporation and others.");
			aWriter.write(newLine);
			aWriter.write("# All rights reserved. This program and the accompanying materials ");
			aWriter.write(newLine);
			aWriter.write("# are made available under the terms of the Common Public License v1.0");
			aWriter.write(newLine); 
			aWriter.write("# which accompanies this distribution, and is available at");
			aWriter.write(newLine);
			aWriter.write("# http://www.eclipse.org/legal/cpl-v10.html");
			aWriter.write(newLine);
			aWriter.write("# ");
			aWriter.write(newLine);
			aWriter.write("# Contributors:");
			aWriter.write(newLine);
			aWriter.write("#     IBM Corporation - initial API and implementation");
			aWriter.write(newLine);
			aWriter.write("###############################################################################");
			aWriter.write(newLine);
			propertiesCopyright = aWriter.toString();
		}
		return propertiesCopyright.toString();
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}
	
}
