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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;

abstract class AbstractPluginScannerMojo
    extends AbstractMojo
{
    /**
     * igorf: as of 2012-01-05, generated repository location is hardcoded to target/repository in tycho
     * 
     * @parameter default-value="${project.build.directory}/repository"
     **/
    protected File repository;

    /** @component */
    protected BundleReader bundleReader;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            Properties properties = new Properties();

            File[] plugins = new File( repository, "plugins" ).listFiles();

            if ( plugins != null )
            {
                Map<File, OsgiManifest> manifests = new HashMap<File, OsgiManifest>();
                for ( File plugin : plugins )
                {
                    OsgiManifest manifest = bundleReader.loadManifest( plugin );
                    manifests.put( plugin, manifest );
                }

                processPlugins( properties, manifests );
            }

            OutputStream os = new BufferedOutputStream( new FileOutputStream( getDestination() ) );

            try
            {
                properties.store( os, null );
            }
            finally
            {
                IOUtil.close( os );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not write plugin versions", e );
        }
    }

    protected abstract void processPlugins( Properties properties, Map<File, OsgiManifest> plugins )
        throws Exception;

    protected abstract File getDestination();

}
