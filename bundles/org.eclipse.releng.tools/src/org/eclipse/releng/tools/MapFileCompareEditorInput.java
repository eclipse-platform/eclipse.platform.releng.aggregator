/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;

/**
 * This class extends<code>CompareEditorInput<code>. It defines the input for map file compare editor, which is showed by
 * <code>MapFileComparePage<code>
 */
public class MapFileCompareEditorInput extends CompareEditorInput {
	
	private MapContentDocument[] documents;
	private DiffNode root;
	private Viewer viewer;
	private MapProject mapProject;
	
	private IProject[] selectedProjects;
	private String tag;	//The proposed tag

	
	public MapFileCompareEditorInput() {
		super(new CompareConfiguration());
		documents = new MapContentDocument[0];
		root = new DiffNode(Differencer.NO_CHANGE) {
			public boolean hasChildren() {
				return true;
			}
		};
		mapProject = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		CompareConfiguration config = getCompareConfiguration();
		config.setRightEditable(false);
		config.setLeftEditable(false);
		config.setRightLabel("Proposed change");
		config.setLeftLabel("Current content");
		return root;
	}

	public void updateInput(IProject[] projects, String tag) {
		setSelectedProjects(projects);
		setTag(tag);		
		MapContentDocument[] docs = constructDocuments();
		setDocuments(docs);
		buildTree();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#createDiffViewer(org.eclipse.swt.widgets.Composite)
	 */
	public Viewer createDiffViewer(Composite parent) {
		viewer =  super.createDiffViewer(parent);
		viewer.setInput(this);
		return viewer;
	}
	
	public void updateMapProject(MapProject m){
		mapProject = m;
	}

	private void setDocuments(MapContentDocument[] docs) {
		this.documents = docs;
	}
	
	

	private void buildTree() {
		if(documents  == null || documents.length == 0)return;
		//Empty the tree
		if(root.hasChildren()){
			IDiffElement[] children = root.getChildren();
			for(int i = 0; i < children.length; i++){
				root.remove(children[i]);
			}
		}

		//rebuild the tree
		DiffNode[] diffNodes = new DiffNode[documents.length];
		for (int i = 0; i < diffNodes.length; i++){
			ResourceNode resourceNode = new ResourceNode(documents[i].getMapFile().getFile()) {
				public boolean isEditable() {
					return false;
				} 
			};
						
			diffNodes[i] = new DiffNode(root, Differencer.CHANGE, null, resourceNode, documents[i]) {
				public Image getImage() {
					return getLeft().getImage();
				}
			};
		}
		viewer.refresh();
	} 
	
	/*
	 * Returns the map files that will change due to the projects being released
	 */
	private MapFile[] getChangedMapFiles() {
		if (selectedProjects == null || selectedProjects.length == 0)
			return null;
		List projectList = new ArrayList();
		CVSTag[] tags = mapProject.getTagsFor(selectedProjects );
		for(int i = 0; i < selectedProjects.length; i++){
			if(!tags[i].getName().equals(tag)){
				projectList.add(selectedProjects[i]);
			}
		}
		IProject [] projects = (IProject[])projectList.toArray(new IProject[projectList.size()]);
		return mapProject.getMapFilesFor(projects);
	}
	

	//Construct the document as the diffNode. 
	private MapContentDocument [] constructDocuments(){
		MapFile[] mapFiles = getChangedMapFiles();
		if(mapFiles == null || mapFiles.length ==0) return null;
		MapContentDocument [] docs = new MapContentDocument[mapFiles.length];
		for(int i = 0; i < mapFiles.length; i++){
			docs[i] = new MapContentDocument(mapFiles[i]);
			for(int j = 0; j < selectedProjects.length; j++){
				//update the new content of each selected projects
				if(mapFiles[i].contains(selectedProjects[j])){
					try {
						docs[i]. updateTag(selectedProjects[j],tag);
					} catch (CVSException e) {
						CVSUIPlugin.openError(null, null, null, e);
					}
				}
			}
		}
		return docs;
	}
	
	private void setSelectedProjects(IProject[] projects) {
		if (projects != null && projects.length != 0) {
			selectedProjects = new IProject[projects.length ];
			System.arraycopy(projects, 0, selectedProjects, 0, projects.length);
		}
	}
	
	private void setTag(String t) {
		this.tag = t;
	}
}
