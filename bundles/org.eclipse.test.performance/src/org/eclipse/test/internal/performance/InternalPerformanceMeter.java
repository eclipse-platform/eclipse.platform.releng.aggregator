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

package org.eclipse.test.internal.performance;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.performance.PerformanceMeter;

public abstract class InternalPerformanceMeter extends PerformanceMeter {

	private static final String LOCALHOST= "localhost";

	private static final String VERSION_SUFFIX= "-runtime";
	private static final String BUILDID_PROPERTY= "eclipse.buildId";
	private static final String SDK_BUNDLE_GROUP_IDENTIFIER= "org.eclipse.sdk";

	public static final String DRIVER_PROPERTY= "driver"; //$NON-NLS-1$
	public static final String HOSTNAME_PROPERTY= "host"; //$NON-NLS-1$
	public static final String RUN_TS_PROPERTY= "runTS"; //$NON-NLS-1$
	public static final String TESTNAME_PROPERTY= "testname"; //$NON-NLS-1$
	
	public static final String BEFORE= "1"; //$NON-NLS-1$
	public static final String AFTER= "2"; //$NON-NLS-1$

	private String fScenarioId;

	
	public InternalPerformanceMeter(String scenarioId) {
	    fScenarioId= scenarioId;
    }

	public void dispose() {
	    fScenarioId= null;
	}

    public abstract Sample getSample();


	/**
	 * Answer the scenario id.
	 */
	String getScenarioName() {
		return fScenarioId;
	}

	/**
	 * Answer the name of the host that we are running on.
	 */
	String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return LOCALHOST;
		}
	}

	String getBuildId() {
		String buildId= System.getProperty(BUILDID_PROPERTY);
		if (buildId != null)
			return buildId;
		IBundleGroupProvider[] providers= Platform.getBundleGroupProviders();
		for (int i= 0; i < providers.length; i++) {
			IBundleGroup[] groups= providers[i].getBundleGroups();
			for (int j= 0; j < groups.length; j++)
				if (SDK_BUNDLE_GROUP_IDENTIFIER.equals(groups[j].getIdentifier()))
					return groups[j].getVersion() + VERSION_SUFFIX;
		}
		return null;
	}
}
