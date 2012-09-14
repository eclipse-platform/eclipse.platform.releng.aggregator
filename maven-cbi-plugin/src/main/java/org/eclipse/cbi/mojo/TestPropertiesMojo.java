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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

/**
 * @goal test-properties
 */
public class TestPropertiesMojo
    extends AbstractPluginScannerMojo
{
    private static enum TestType
    {
        NONE, TEST, PERFTEST
    };

    private static class Plugin
    {
        private final OsgiManifest manifest;

        private final TestType testType;

        public Plugin( OsgiManifest manifest, TestType testType )
        {
            this.manifest = manifest;
            this.testType = testType;
        }

        public OsgiManifest getManifest()
        {
            return manifest;
        }

        public TestType getTestType()
        {
            return testType;
        }
    }

    private static XMLParser parser = new XMLParser();

    /** @parameter default-value="${project.build.directory}/test.properties" */
    protected File destination;

    @Override
    protected File getDestination()
    {
        return destination;
    }

    @Override
    protected void processPlugins( Properties properties, Map<File, OsgiManifest> plugins )
        throws Exception
    {
        Map<String, Plugin> model = new HashMap<String, Plugin>();

        for ( Map.Entry<File, OsgiManifest> entry : plugins.entrySet() )
        {
            TestType type = getTestType( entry.getKey() );
            OsgiManifest manifest = entry.getValue();
            model.put( manifest.getBundleSymbolicName(), new Plugin( manifest, type ) );
        }

        for ( Plugin plugin : model.values() )
        {
            if ( TestType.NONE == plugin.getTestType() )
            {
                continue;
            }

            OsgiManifest manifest = plugin.getManifest();

            properties.put( manifest.getBundleSymbolicName(),
                            manifest.getBundleSymbolicName() + "_" + manifest.getBundleVersion() );

            if ( TestType.PERFTEST == plugin.getTestType() )
            {
                properties.put( manifest.getBundleSymbolicName() + ".has.performance.target", "true" );
            }

            List<Plugin> dependencies = new ArrayList<Plugin>();
            collectRequiredBundles( plugin, model, dependencies, new HashSet<ArtifactKey>() );
            StringBuilder sb = new StringBuilder();
            for ( Plugin dependency : dependencies )
            {
                if ( plugin == dependency )
                {
                    continue;
                }

                if ( TestType.NONE == dependency.getTestType() )
                {
                    continue;
                }

                if ( sb.length() > 0 )
                {
                    sb.append( " " );
                }

                sb.append( "**/${" ).append( dependency.getManifest().getBundleSymbolicName() ).append( "}**" );
            }

            properties.put( manifest.getBundleSymbolicName() + ".prerequisite.testplugins", sb.toString() );
        }
    }

    private void collectRequiredBundles( Plugin plugin, Map<String, Plugin> model, Collection<Plugin> required,
                                         Set<ArtifactKey> visited )
        throws BundleException
    {
        ArtifactKey key = newArtifactKey( plugin );
        if ( visited.add( key ) )
        {
            required.add( plugin );

            OsgiManifest manifest = plugin.getManifest();

            String value = manifest.getValue( Constants.REQUIRE_BUNDLE );
            if ( value == null )
            {
                return;
            }
            ManifestElement[] elements = ManifestElement.parseHeader( Constants.REQUIRE_BUNDLE, value );
            if ( elements == null )
            {
                return;
            }
            for ( ManifestElement element : elements )
            {
                Plugin other = model.get( element.getValue() );
                if ( other != null )
                {
                    collectRequiredBundles( other, model, required, visited );
                }
            }
        }
    }

    private ArtifactKey newArtifactKey( Plugin plugin )
    {
        OsgiManifest m = plugin.getManifest();
        return new DefaultArtifactKey( ArtifactKey.TYPE_ECLIPSE_PLUGIN, m.getBundleSymbolicName(), m.getBundleVersion() );
    }

    private TestType getTestType( File plugin )
        throws IOException
    {
        JarFile jar = new JarFile( plugin );
        try
        {
            ZipEntry entry = jar.getEntry( "test.xml" );
            if ( entry == null )
            {
                return TestType.NONE;
            }
            Document document;
            InputStream is = jar.getInputStream( entry );
            try
            {
                document = parser.parse( new XMLIOSource( is ) );
            }
            finally
            {
                IOUtil.close( is );
            }

            for ( Element element : document.getRootElement().getChildren( "target" ) )
            {
                if ( "performance".equals( element.getAttributeValue( "name" ) ) )
                {
                    return TestType.PERFTEST;
                }
            }

            return TestType.TEST;
        }
        finally
        {
            jar.close();
        }
    }
}
