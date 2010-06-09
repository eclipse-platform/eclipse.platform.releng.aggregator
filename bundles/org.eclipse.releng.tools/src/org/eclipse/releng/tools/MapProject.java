/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.ui.operations.CommitOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;


public class MapProject implements IResourceChangeListener {
	
	private static MapProject mapProject = null;
	private IProject project;
	private MapFile[] mapFiles;
	
	/**
	 * Return the default map project (org.eclipse.releng) or
	 * <code>null</code> if the project does not exist or there
	 * is an error processing it. If there is an error, it
	 * will be logged.
	 * @return the default map project
	 */
	public static MapProject getDefaultMapProject(){		
		if (mapProject == null) {
			IProject project = getProjectFromWorkspace();
			try {
				mapProject = new MapProject(project);
			} catch (CoreException e) {
				RelEngPlugin.log(e);
			}
		}
		return mapProject;
	}
	
	private static IProject getProjectFromWorkspace() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(RelEngPlugin.MAP_PROJECT_NAME);
		return project;
	}

	public MapProject(IProject p) throws CoreException {
		this.project = p;
		loadMapFiles();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public IProject getProject() {
		return project;
	}
	
	public void setProject(IProject p){
		this.project = p;
	}
	
	private MapFile getMapFile(IProject p){
		for (int i = 0; i< mapFiles.length; i++){
			if (mapFiles[i].contains(p)) {
				return mapFiles[i];
			}
		}		
		return null;
	}
	
	/**
	 * Return the MapEntry for the given project
	 * @param string
	 * @param string1
	 */
	public MapEntry getMapEntry(IProject p) {
		MapFile file = getMapFile(p);
		if (file != null) {
			return file.getMapEntry(p);
		}
		return null;
	}

	public boolean mapsAreLoaded() {
		return project.exists();
	}
 
	public MapFile[] getValidMapFiles(){
		List list = new ArrayList();
		for (int i = 0; i <mapFiles.length; i++){
			IProject[] projects = mapFiles[i].getAccessibleProjects(); 
			if( projects!= null && projects.length > 0){
				list.add(mapFiles[i]);
			}
		}
		return (MapFile[])list.toArray(new MapFile[list.size()]);
	}
	
	/**
	 * @param aProject The map entry of the specified project will be changed to the specified tag
	 * @param tag The specified tag
	 * @return returns if no map file having such a map entry is found 
	 */
	public void updateFile(IProject aProject, String tag) throws CoreException {
		MapFile aFile = getMapFile(aProject);
		if (aFile == null)return;
		MapContentDocument changed = new MapContentDocument(aFile);
		changed.updateTag(aProject, tag);
		if (changed.isChanged()) {
			aFile.getFile().setContents(changed.getContents(), IFile.KEEP_HISTORY, null);
		}
	}
	
	public void commitMapProject(String comment, IProgressMonitor  monitor) throws CoreException{
		try {
			new CommitOperation(null, RepositoryProviderOperation.asResourceMappers(new IResource[] { project }), new Command.LocalOption[0], comment).run(monitor);
		} catch (InvocationTargetException e) {
			throw TeamException.asTeamException(e);
		} catch (InterruptedException e) {
			// Ignore;
			e.printStackTrace();
		}
	}
	
	public MapFile[] getMapFilesFor(IProject[] projects){
		Set alist = new HashSet();		
		for(int i = 0; i<projects.length; i++){
			MapFile aMapFile = getMapFile(projects[i]);
			alist.add(aMapFile);
		}
		return (MapFile[])alist.toArray(new MapFile[alist.size()]);
	}

	/**
	 * Get the tags for the given projects. If no tag is found
	 * for whatever reason, HEAD is used.
	 * @param projects
	 * @return
	 */
	public CVSTag[] getTagsFor(IProject[] projects){
		if(projects == null || projects.length == 0)return null;
		CVSTag[] tags = new CVSTag[projects.length];
		for (int i = 0; i < tags.length; i++){
			MapEntry entry = getMapEntry(projects[i]);
			if (entry == null) tags[i] = CVSTag.DEFAULT;
			else tags[i] = entry.getTag();		
		}
		return tags;
	}
	/**
	 * Deregister the IResourceChangeListner. It is reserved for use in the future. It is never called
	 * for now
	 */
	public void dispose(){
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		mapFiles= null;
	}
	
	/**
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta root = event.getDelta();
		
		//TODO: Need to add code to handle map project deletion, addition and rename			
		IResourceDelta folderDelta = root.findMember(getMapFolder().getFullPath());
		if (folderDelta == null) return;
		
		//Handle map files deletion, addition and rename
		IResourceDelta[] deltas = folderDelta.getAffectedChildren();
		if(deltas == null || deltas.length == 0) return;
		for (int i = 0; i < deltas.length; i++) {
			IResourceDelta delta = deltas[i];
			if(delta.getResource().getType() == IResource.FILE){				
				try{
					IFile aFile = (IFile)(delta.getResource());
					MapFile mFile = null;
					if(MapFile.isMapFile(aFile)){
						// Handle content change
						if(delta.getKind() == IResourceDelta.CHANGED){	
							mFile = getMapFileFor(aFile);
							mFile.loadEntries();	
						}
						//Handle deletion. We cannot simply remove the map file directly bacause we have to call
						//getMapFileFor(IFile) in order to do so. But the IFile is already deleted. So we have to 
						//reconstuct the map files.
						if(delta.getKind() == IResourceDelta.REMOVED ){
							loadMapFiles();
						}
						// Handle addition
						if(delta.getKind() == IResourceDelta.ADDED ){
							mFile = getMapFileFor(aFile);
							addMapFile(mFile);
						}
					}
				} catch (CoreException e) {
					RelEngPlugin.log(e);
				}
			}
		}
	}

	private IFolder getMapFolder(){
		return getProject().getFolder(RelEngPlugin.MAP_FOLDER);
	}

	private void loadMapFiles() throws CoreException {
		mapFiles = MapFile.findAllMapFiles(project);
	}
	
	private MapFile getMapFileFor(IFile file) throws CoreException{
		for(int i = 0; i < mapFiles.length; i++){
			if (mapFiles[i].getFile().equals(file))
				return mapFiles[i];
		}
		return new MapFile(file);
	}
	private void addMapFile(MapFile aFile){
		Set set = new HashSet(Arrays.asList(mapFiles));
		set.add(aFile);
		mapFiles = (MapFile[])set.toArray(new MapFile[set.size()]); 
	}
}
