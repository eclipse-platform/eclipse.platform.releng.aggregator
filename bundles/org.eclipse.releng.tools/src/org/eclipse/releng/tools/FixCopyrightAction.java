/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.releng.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class FixCopyrightAction implements IObjectActionDelegate {

    public class MyInnerClass implements IResourceVisitor {
        public IProgressMonitor monitor;
        public boolean visit(IResource resource) throws CoreException {
            if (resource.getType() == IResource.FILE) {
                processFile((IFile) resource, monitor);
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

    private static final int currentYear = new GregorianCalendar().get(Calendar.YEAR);

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
            Iterator elements = selection.iterator();
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
        try {
            PlatformUI.getWorkbench().getProgressService().run(true, /* fork */
            true, /* cancellable */
            new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Fixing copyrights...",
                                IProgressMonitor.UNKNOWN);

                        System.out.println("Start Fixing Copyrights");
                        IResource[] results = getSelectedResources();
                        System.out.println("Resources selected: "
                                + results.length);
                        for (int i = 0; i < results.length; i++) {
                            IResource resource = results[i];
                            System.out.println(resource.getName());
                            try {
                                MyInnerClass myInnerClass = new MyInnerClass();
                                myInnerClass.monitor = monitor;
                                resource.accept(myInnerClass);
                            } catch (CoreException e1) {
                                e1.printStackTrace();
                            }
                        }

                        writeLogs();
                        //		displayLogs();
                        System.out.println("Done Fixing Copyrights");

                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Lookup and return the year in which the argument file was revised.  Return -1 if
     * the revision year cannot be found.
     */
    private int getCVSModificationYear(IFile file, IProgressMonitor monitor) {
        try {
            monitor.beginTask("Fetching logs from CVS", 100);

            try {
                ICVSRemoteResource cvsFile = CVSWorkspaceRoot.getRemoteResourceFor(file);
                if (cvsFile != null) {
	                // get the log entry for the revision loaded in the workspace
	                ILogEntry entry = ((ICVSRemoteFile)cvsFile)
	                        .getLogEntry(new SubProgressMonitor(monitor, 100));
	                return entry.getDate().getYear() + 1900;
                }
            } catch (TeamException e) {
                // do nothing
            }
        } finally {
            monitor.done();
        }

        return -1;
    }

    /**
     *  
     */
    private void writeLogs() {

        FileOutputStream aStream;
        try {
            File aFile = new File(Platform.getLocation().toFile(),
                    "copyrightLog.txt");
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
    private void processFile(IFile file, IProgressMonitor monitor) {
        SourceFile aSourceFile;

        String extension = file.getFileExtension();
        if (extension == null)
            return;
        monitor.subTask(file.getFullPath().toOSString());
        int fileType = IBMCopyrightComment.UNKNOWN_COMMENT;
        extension = extension.toLowerCase();
        if (extension.equals("java")) { //$NON-NLS-1$
        	fileType = IBMCopyrightComment.JAVA_COMMENT;
            aSourceFile = new JavaFile(file);
        } else if (extension.equals("properties")) { //$NON-NLS-1$
        	fileType = IBMCopyrightComment.PROPERTIES_COMMENT;
            aSourceFile = new PropertiesFile(file);
        } else
            return;

        if (aSourceFile.hasMultipleCopyrights()) {
            warn(file, null, "Multiple copyrights found.  File UNCHANGED."); //$NON-NLS-1$//$NON-NLS-2$
            return;
        }

        BlockComment copyrightComment = aSourceFile.firstCopyrightComment();
        IBMCopyrightComment ibmCopyright = IBMCopyrightComment.parse(copyrightComment, fileType);
        if (ibmCopyright == null) {
            warn(file, copyrightComment, "Could not interpret copyright comment"); //$NON-NLS-1$
            return;
        }

        // only check CVS if the copyright statement might be too old
        int revisionYear = ibmCopyright.getRevisionYear();
        if (revisionYear < currentYear) {
            int cvsYear = getCVSModificationYear(file, new NullProgressMonitor());
            ibmCopyright.setRevisionYear(Math.max(revisionYear, cvsYear));
        }

        // either replace old copyright or put the new one at the top of the file
        if (copyrightComment == null)
            aSourceFile.insert(ibmCopyright.getCopyrightComment());
        else {
            if (!copyrightComment.atTop())
                warn(file, copyrightComment, "Old copyright not at start of file, new copyright replaces old in same location"); //$NON-NLS-1$
            aSourceFile.replace(copyrightComment, ibmCopyright.getCopyrightComment());
        }
    }

    private void warn(IFile file, BlockComment firstBlockComment,
            String errorDescription) {
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

            aWriter
                    .write("/*******************************************************************************");
            aWriter.write(newLine);
            aWriter
                    .write(" * Copyright (c) 2000, 2004 IBM Corporation and others.");
            aWriter.write(newLine);
            aWriter
                    .write(" * All rights reserved. This program and the accompanying materials ");
            aWriter.write(newLine);
            aWriter
                    .write(" * are made available under the terms of the Common Public License v1.0");
            aWriter.write(newLine);
            aWriter
                    .write(" * which accompanies this distribution, and is available at");
            aWriter.write(newLine);
            aWriter.write(" * http://www.eclipse.org/legal/cpl-v10.html");
            aWriter.write(newLine);
            aWriter.write(" * ");
            aWriter.write(newLine);
            aWriter.write(" * Contributors:");
            aWriter.write(newLine);
            aWriter
                    .write(" *     IBM Corporation - initial API and implementation");
            aWriter.write(newLine);
            aWriter
                    .write(" *******************************************************************************/");
            aWriter.write(newLine);
            javaCopyright = aWriter.toString();
        }
        return javaCopyright.toString();
    }

    private String getPropertiesCopyright() {
        if (propertiesCopyright == null) {
            String newLine = System.getProperty("line.separator");
            StringWriter aWriter = new StringWriter();

            aWriter
                    .write("###############################################################################");
            aWriter.write(newLine);
            aWriter
                    .write("# Copyright (c) 2000, 2004 IBM Corporation and others.");
            aWriter.write(newLine);
            aWriter
                    .write("# All rights reserved. This program and the accompanying materials ");
            aWriter.write(newLine);
            aWriter
                    .write("# are made available under the terms of the Common Public License v1.0");
            aWriter.write(newLine);
            aWriter
                    .write("# which accompanies this distribution, and is available at");
            aWriter.write(newLine);
            aWriter.write("# http://www.eclipse.org/legal/cpl-v10.html");
            aWriter.write(newLine);
            aWriter.write("# ");
            aWriter.write(newLine);
            aWriter.write("# Contributors:");
            aWriter.write(newLine);
            aWriter
                    .write("#     IBM Corporation - initial API and implementation");
            aWriter.write(newLine);
            aWriter
                    .write("###############################################################################");
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