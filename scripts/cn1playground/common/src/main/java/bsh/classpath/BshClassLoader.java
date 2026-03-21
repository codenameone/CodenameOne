/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh.classpath;

import java.net.URL;
import java.net.URLClassLoader;

import bsh.BshClassManager;

/**
    One of the things BshClassLoader does is to address a deficiency in
    URLClassLoader that prevents us from specifying individual classes
    via URLs.
*/
public class BshClassLoader extends URLClassLoader
{
    BshClassManager classManager;

    /**
        @param bases URLs JARClassLoader seems to require absolute paths
    */
    public BshClassLoader( BshClassManager classManager, URL [] bases ) {
        super( bases );
        this.classManager = classManager;
    }

    /**
        @param bases URLs JARClassLoader seems to require absolute paths
    */
    public BshClassLoader( BshClassManager classManager, BshClassPath bcp ) {
        this( classManager, bcp.getPathComponents() );
    }

    /**
        For use by children
        @param bases URLs JARClassLoader seems to require absolute paths
    */
    protected BshClassLoader( BshClassManager classManager ) {
        this( classManager, new URL [] { } );
    }

    // public version of addURL
    public void addURL( URL url ) {
        super.addURL( url );
    }

    /**
        This modification allows us to reload classes which are in the
        Java VM user classpath.  We search first rather than delegate to
        the parent classloader (or bootstrap path) first.

        An exception is for BeanShell core classes which are always loaded from
        the same classloader as the interpreter.
    */
    public Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve); // prevent SecurityException
        }

        /*
            Check first for classes loaded through this loader.
            The VM will not allow a class to be loaded twice.
        */
        Class c = findLoadedClass(name);
        if ( c != null ) {
            return c;
        }

        // This is copied from ClassManagerImpl
        // We should refactor this somehow if it sticks around
        if ( name.startsWith( ClassManagerImpl.BSH_PACKAGE ) ) {
            final ClassLoader loader = bsh.Interpreter.class
                    .getClassLoader();
            if ( null != loader ) try {
                return loader.loadClass( name );
            } catch ( ClassNotFoundException e ) { /* ignore */ }
        }

        /*
            Try to find the class using our classloading mechanism.
        */
        c = findClass( name );

        if ( resolve ) {
            resolveClass( c );
        }

        return c;
    }

    /**
        Find the correct source for the class...

        Try designated loader if any
        Try our URLClassLoader paths if any
        Try base loader if any
        Try system ???
    */
    // add some caching for not found classes?
    protected Class findClass( String name )
        throws ClassNotFoundException
    {
        // Deal with this cast somehow... maybe have this class use
        // ClassManagerImpl type directly.
        // Don't add the method to BshClassManager... it's really an impl thing
        ClassManagerImpl bcm = (ClassManagerImpl)getClassManager();

        // Should we try to load the class ourselves or delegate?
        // look for overlay loader

        ClassLoader cl = bcm.getLoaderForClass( name );

        // If there is a designated loader and it's not us delegate to it
        if ( cl != null && cl != this ) try {
            return cl.loadClass( name );
        } catch ( ClassNotFoundException e ) {
            throw new ClassNotFoundException(
                "Designated loader could not find class: "+e );
        }

        // Let URLClassLoader try any paths it may have
        if ( getURLs().length > 0 ) try {
            return super.findClass(name);
        } catch ( ClassNotFoundException e ) {
            //System.out.println(
            //  "base loader here caught class not found: "+name );
        }


        // If there is a baseLoader and it's not us delegate to it
        cl = bcm.getBaseLoader();

        if ( cl != null && cl != this ) try {
            return cl.loadClass( name );
        } catch ( ClassNotFoundException e ) { }

        // Try system loader
        return bcm.plainClassForName( name );
    }

    /*
        The superclass does something like this

        c = findLoadedClass(name);
        if null
            try
                if parent not null
                    c = parent.loadClass(name, false);
                else
                    c = findBootstrapClass(name);
            catch ClassNotFoundException
                c = findClass(name);
    */

    BshClassManager getClassManager() { return classManager; }

}
