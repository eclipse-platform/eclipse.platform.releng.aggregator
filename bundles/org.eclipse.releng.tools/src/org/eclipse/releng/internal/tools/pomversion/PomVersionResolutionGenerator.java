/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;


public class PomVersionResolutionGenerator implements IMarkerResolutionGenerator {

	private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	public IMarkerResolution[] getResolutions(IMarker marker) {

		// FIXME: Disabled because of bug 411100: POM version problem quick fix mangles the pom.xml file
//		try {
//			if (marker.getType().equals(IPomVersionConstants.PROBLEM_MARKER_TYPE)){
//				String correctedVersion = (String) marker.getAttribute(IPomVersionConstants.POM_CORRECT_VERSION);
//				return new IMarkerResolution[] {new PomVersionMarkerResolution(correctedVersion)};
//			}
//		} catch (CoreException e){
//			RelEngPlugin.log(e);
//		}

		return NO_RESOLUTIONS;
	}

}
