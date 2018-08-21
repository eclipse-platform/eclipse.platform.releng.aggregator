/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.internal.tools.pomversion;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;


public class PomVersionResolutionGenerator implements IMarkerResolutionGenerator {

	private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		int charstart = marker.getAttribute(IMarker.CHAR_START, -1);
		int charend = marker.getAttribute(IMarker.CHAR_END, -1);
		if(charstart > -1 && charend > -1) {
			try {
				if (marker.getType().equals(IPomVersionConstants.PROBLEM_MARKER_TYPE)){
					String correctedVersion = (String) marker.getAttribute(IPomVersionConstants.POM_CORRECT_VERSION);
					if (correctedVersion != null && correctedVersion.length() > 0){
						return new IMarkerResolution[] {new PomVersionMarkerResolution(marker,correctedVersion)};
					}
				}
			} catch (CoreException e){
				RelEngPlugin.log(e);
			}
		}
		return NO_RESOLUTIONS;
	}
}
