package com.codename1.mojo.exec;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Factory for {@link PathsToolchain}.
 *
 * @author Markus KARG (markus@headcrashing.eu)
 */
@Component( role = ToolchainFactory.class, hint = "paths" )
class PathsToolchainFactory
    implements ToolchainFactory
{

    @Requirement
    private Logger logger;

    public ToolchainPrivate createToolchain( final ToolchainModel model )
        throws MisconfiguredToolchainException
    {
        if ( model == null )
            return null;

        final PathsToolchain pathsToolchain = new PathsToolchain( model, this.logger );
        final Properties provides = this.getProvidesProperties( model );
        for ( final Map.Entry<Object, Object> provide : provides.entrySet() )
        {
            final String key = (String) provide.getKey();
            final String value = (String) provide.getValue();
            if ( value == null )
                throw new MisconfiguredToolchainException( "Provides token '" + key
                    + "' doesn't have any value configured." );

            pathsToolchain.addProvideToken( key, RequirementMatcherFactory.createExactMatcher( value ) );
        }

        final Xpp3Dom config = (Xpp3Dom) model.getConfiguration();
        if ( config == null )
            return pathsToolchain;

        final Xpp3Dom pathDom = config.getChild( "paths" );
        if ( pathDom == null )
            return pathsToolchain;

        final Xpp3Dom[] pathDoms = pathDom.getChildren( "path" );
        if ( pathDoms == null || pathDoms.length == 0 )
            return pathsToolchain;

        final List<String> paths = new ArrayList<String>( pathDoms.length );
        for ( final Xpp3Dom pathdom : pathDoms )
        {
            final String pathString = pathdom.getValue();

            if ( pathString == null )
                throw new MisconfiguredToolchainException( "path element is empty" );

            final String normalizedPath = FileUtils.normalize( pathString );
            final File file = new File( normalizedPath );
            if ( !file.exists() )
                throw new MisconfiguredToolchainException( "Non-existing path '" + file.getAbsolutePath() + "'" );

            paths.add( normalizedPath );
        }

        pathsToolchain.setPaths( paths );

        return pathsToolchain;
    }

    public ToolchainPrivate createDefaultToolchain()
    {
        return null;
    }

    protected Properties getProvidesProperties( final ToolchainModel model )
    {
        final Object value = this.getBeanProperty( model, "provides" );

        return value instanceof Properties ? (Properties) value : toProperties( (Xpp3Dom) value );
    }

    protected static Properties toProperties( final Xpp3Dom dom )
    {
        final Properties props = new Properties();

        final Xpp3Dom[] children = dom.getChildren();
        for ( final Xpp3Dom child : children )
            props.put( child.getName(), child.getValue() );

        return props;
    }

    protected Object getBeanProperty( final Object obj, final String property )
    {
        try
        {
            final Method method = new PropertyDescriptor( property, obj.getClass() ).getReadMethod();

            return method.invoke( obj );
        }
        catch ( final IntrospectionException e )
        {
            throw new RuntimeException( "Incompatible toolchain API", e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new RuntimeException( "Incompatible toolchain API", e );
        }
        catch ( final InvocationTargetException e )
        {
            final Throwable cause = e.getCause();

            if ( cause instanceof RuntimeException )
                throw (RuntimeException) cause;

            throw new RuntimeException( "Incompatible toolchain API", e );
        }
    }
}
