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

import org.eclipse.core.runtime.*;

/**
 *	a wrapper class of IProgressMonitor which provides methods to
 *	get access to the wrapped IProgressMonitor instance
 */
public class VersioningProgressMonitorWrapper extends ProgressMonitorWrapper {
	// the wrapped monitor is not supposed to be used more than once, 
	// this flag will be set as true when the first time it is started, and
	// will not be set as false when the monitor's task is done.
	private boolean monitorStarted;

	/**
	 * constructor
	 * @param monitor
	 */
	public VersioningProgressMonitorWrapper(IProgressMonitor monitor) {
		super(monitor);
		monitorStarted = false;
	}

	/**
	 * get a new sub monitor of wrapped progress monitor
	 *
	 * @return IProgressMonitor instance
	 */
	public IProgressMonitor getSubMonitor(int ticks) {
		return new SubProgressMonitor(this.getWrappedProgressMonitor(), ticks);
	}

	/**
	 * starts the wrapped monitor
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(String name, int totalWork) {
		if (!monitorStarted) {
			this.getWrappedProgressMonitor().beginTask(name, totalWork);
			monitorStarted = true;
		}
	}

	/**
	 * if <code>monitor</code> is <code>null</code> return a new NullProgressMonitor instance,
	 * otherwise return <code>monitor</code>
	 * @param monitor IProgressMonitor instance
	 * @return IProgressMonitor instance
	 */
	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		return monitor == null ? new NullProgressMonitor() : monitor;
	}
}