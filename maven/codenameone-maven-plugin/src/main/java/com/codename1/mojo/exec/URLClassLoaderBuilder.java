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

import org.apache.maven.plugin.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
class URLClassLoaderBuilder
{
    private Log logger;
    private Collection<Path> paths;
    private Collection<String> exclusions;

    private URLClassLoaderBuilder()
    {
    }

    static URLClassLoaderBuilder builder()
    {
        return new URLClassLoaderBuilder();
    }

    URLClassLoaderBuilder setLogger(Log logger )
    {
        this.logger = logger;
        return this;
    }

    URLClassLoaderBuilder setExclusions(Collection<String> exclusions )
    {
        this.exclusions = exclusions;
        return this;
    }

    URLClassLoaderBuilder setPaths(Collection<Path> paths )
    {
        this.paths = paths;
        return this;
    }

    URLClassLoader build() throws IOException
    {
        List<URL> urls = new ArrayList<>( paths.size() );

        for ( Path dependency : paths )
        {
            if ( exclusions != null && exclusions.contains( dependency.getFileName().toString() ) )
            {
                if ( logger != null )
                {
                    logger.debug("Excluding as requested '" + dependency + "'" );
                }
                continue;
            }
            try
            {
                urls.add( dependency.toUri().toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new IOException( "Error during setting up classpath", e );
            }
        }

        return new ExecJavaClassLoader( urls.toArray( new URL[0] ) );
    }

    // child first strategy
    private static class ExecJavaClassLoader extends URLClassLoader
    {
        static
        {
            try
            {
                registerAsParallelCapable();
            }
            catch ( Exception e )
            {
                // no-op, not that important
            }
        }

        private final String jre;

        public ExecJavaClassLoader(URL[] urls )
        {
            super(urls);
            jre = getJre();
        }

        @Override
        public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (name == null) {
                throw new ClassNotFoundException();
            }

            synchronized ( getClassLoadingLock( name ) )
            {
                Class<?> clazz;

                // if in the JVM, never override them
                if ( isDirectJvmClass( name ) )
                {
                    try
                    {
                        clazz = getSystemClassLoader().loadClass( name );
                        if ( postLoad( resolve, clazz ) )
                        {
                            return clazz;
                        }
                    }
                    catch ( NoClassDefFoundError | ClassNotFoundException ignored )
                    {
                        // no-op
                    }
                }

                // already loaded?
                clazz = findLoadedClass( name );
                if ( postLoad( resolve, clazz ) )
                {
                    return clazz;
                }

                // look for it in this classloader
                try
                {
                    clazz = super.findClass( name );
                    if (clazz != null)
                    {
                        if ( postLoad( resolve, clazz ) )
                        {
                            return clazz;
                        }
                        return clazz;
                    }
                }
                catch ( ClassNotFoundException | NoClassDefFoundError ignored )
                {
                    // try next
                }

                // load from parent - todo: exclude some classes?
                ClassLoader parent = getParent();
                if (parent == null) {
                    parent = getSystemClassLoader();
                }
                try
                {
                    clazz = Class.forName( name, false, parent );
                    if ( postLoad( resolve, clazz ) )
                    {
                        return clazz;
                    }
                }
                catch ( ClassNotFoundException ignored )
                {
                    // no-op
                }

                throw new ClassNotFoundException(name);
            }
        }

        @Override
        public Enumeration<URL> getResources( String name ) throws IOException
        {
            final Enumeration<URL> selfResources = findResources(name);
            final Enumeration<URL> parentResources = getParent().getResources(name);
            if (!parentResources.hasMoreElements())
            {
                return selfResources;
            }
            if (!selfResources.hasMoreElements())
            {
                return new FilteringUrlEnum(parentResources);
            }
            return new ChainedEnumerations(
                    asList( selfResources, new FilteringUrlEnum(parentResources) ).iterator());
        }

        private boolean isInJvm( URL resource )
        {
            final Path path = toPath(resource);
            if (path == null)
            {
                return false;
            }
            return path.normalize().toAbsolutePath().toString().startsWith(jre);
        }

        private String getJre()
        {
            final Path home = new File( System.getProperty("java.home", "") ).toPath();
            if ( "jre".equals( home.getFileName().toString() ) && home.getParent() != null &&
                    Files.exists(home.getParent().resolve( "lib/tools.jar" ) ) )
            {
                return home.getParent().toAbsolutePath().toString();
            }
            return home.toAbsolutePath().toString();
        }

        private Path toPath(final URL url)
        {
            if ( "jar".equals( url.getProtocol()) )
            {
                try
                {
                    String spec = url.getFile();
                    int separator = spec.indexOf( '!' );
                    if ( separator == -1 ) {
                        return null;
                    }
                    return toPath( new URL( spec.substring( 0, separator + 1 ) ) ) ;
                }
                catch ( MalformedURLException e )
                {
                    // no-op
                }
            }
            else if ( "file".equals( url.getProtocol() ) )
            {
                String path = decode( url.getFile() );
                if (path.endsWith( "!" ))
                {
                    path = path.substring( 0, path.length() - 1 );
                }
                return new File(path).getAbsoluteFile().toPath();
            }
            return null;
        }

        private String decode( String fileName )
        {
            if ( fileName.indexOf( '%' ) == -1 )
            {
                return fileName;
            }

            final StringBuilder result = new StringBuilder( fileName.length() );
            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            for ( int i = 0; i < fileName.length(); )
            {
                final char c = fileName.charAt( i );

                if ( c == '%' )
                {
                    out.reset();
                    do
                    {
                        if ( i + 2 >= fileName.length() )
                        {
                            throw new IllegalArgumentException( "Incomplete % sequence at: " + i );
                        }

                        final int d1 = Character.digit( fileName.charAt( i + 1 ), 16 );
                        final int d2 = Character.digit (fileName.charAt( i + 2 ), 16 );

                        if ( d1 == -1 || d2 == -1 )
                        {
                            throw new IllegalArgumentException(
                                    "Invalid % sequence (" + fileName.substring(i, i + 3) + ") at: " + i );
                        }

                        out.write( (byte) ( ( d1 << 4 ) + d2 ) );

                        i += 3;

                    }
                    while ( i < fileName.length() && fileName.charAt(i) == '%' );

                    result.append( out.toString() );

                    continue;
                }
                else
                {
                    result.append( c );
                }

                i++;
            }
            return result.toString();
        }

        private boolean postLoad( boolean resolve, Class<?> clazz )
        {
            if ( clazz != null )
            {
                if (resolve)
                {
                    resolveClass( clazz );
                }
                return true;
            }
            return false;
        }

        // not all jvm classes, for ex "javax" can be overriden so don't list it here
        private boolean isDirectJvmClass(final String name) {
            if (name.startsWith( "java." ) )
            {
                return true;
            }
            if ( name.startsWith( "sun." ) )
            {
                return true;
            }
            if ( name.startsWith( "jdk." ) )
            {
                return true;
            }
            if ( name.startsWith( "oracle." ) )
            {
                return true;
            }
            if ( name.startsWith( "javafx." ) )
            {
                return true;
            }
            if ( name.startsWith( "netscape." ) )
            {
                return true;
            }
            if ( name.startsWith( "org." ) )
            {
                final String sub = name.substring( "org.".length() );
                if ( sub.startsWith( "w3c.dom." ) )
                {
                    return true;
                }
                if ( sub.startsWith( "omg." ) )
                {
                    return true;
                }
                if ( sub.startsWith( "xml.sax." ) )
                {
                    return true;
                }
                if ( sub.startsWith( "ietf.jgss." ) )
                {
                    return true;
                }
                if ( sub.startsWith( "jcp.xml.dsig.internal." ) )
                {
                    return true;
                }
            }
            if ( name.startsWith( "com." ) )
            {
                final String sub = name.substring( "com.".length() );
                if ( sub.startsWith( "oracle." ) )
                {
                    return true;
                }
                if ( sub.startsWith( "sun." ) )
                {
                    final String subSun = sub.substring( "sun.".length() );
                    if ( subSun.startsWith( "accessibility." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "activation." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "awt." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "beans." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "corba.se." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "demo.jvmti." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "image.codec.jpeg." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "imageio." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "istack.internal." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "java." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "java_cup." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "jmx." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "jndi." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "management." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "media.sound." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "naming.internal." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "net." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "nio." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "org." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "rmi.rmid." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "rowset." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "security." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "swing." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "tracing." ) )
                    {
                        return true;
                    }
                    if ( subSun.startsWith( "xml.internal." ) )
                    {
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        private class FilteringUrlEnum implements Enumeration<URL>
        {
            private final Enumeration<URL> delegate;
            private URL next;

            private FilteringUrlEnum( Enumeration<URL> delegate )
            {
                this.delegate = delegate;
            }

            @Override
            public boolean hasMoreElements()
            {
                while (delegate.hasMoreElements())
                {
                    next = delegate.nextElement();
                    if (isInJvm(next))
                    {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public URL nextElement()
            {
                return next;
            }
        }

        private static class ChainedEnumerations implements Enumeration<URL>
        {
            private final Iterator<Enumeration<URL>> enumerations;
            private Enumeration<URL> current;

            private ChainedEnumerations( Iterator<Enumeration<URL>> enumerations )
            {
                this.enumerations = enumerations;
            }

            @Override
            public boolean hasMoreElements()
            {
                if (current == null || !current.hasMoreElements())
                {
                    if (!enumerations.hasNext())
                    {
                        return false;
                    }
                    current = enumerations.next();
                }
                return current.hasMoreElements();
            }

            @Override
            public URL nextElement()
            {
                return current.nextElement();
            }
        }
    }
}
