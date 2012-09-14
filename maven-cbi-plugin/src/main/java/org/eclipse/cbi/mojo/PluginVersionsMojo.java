/*******************************************************************************
 * Copyright (c) 2012 Eclipse Foundation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cbi.mojo;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.eclipse.tycho.core.osgitools.OsgiManifest;

/**
 * @goal plugin-versions
 */
public class PluginVersionsMojo
    extends AbstractPluginScannerMojo
{
    /** @parameter default-value="${project.build.directory}/plugin-versions.properties" */
    protected File destination;

    @Override
    protected void processPlugins( Properties properties, Map<File, OsgiManifest> plugins )
    {
        for ( OsgiManifest manifest : plugins.values() )
        {
            properties.put( manifest.getBundleSymbolicName(), manifest.getBundleVersion() );
        }
    }

    @Override
    protected File getDestination()
    {
        return destination;
    }
}
