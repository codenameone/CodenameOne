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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bsh.BshClassManager;
import bsh.ClassPathException;
import bsh.Interpreter;  // for debug()
import bsh.UtilEvalError;
import bsh.classpath.BshClassPath.ClassSource;
import bsh.classpath.BshClassPath.GeneratedClassSource;
import bsh.classpath.BshClassPath.JarClassSource;

/**
    <pre>
    Manage all classloading in BeanShell.
    Allows classpath extension and class file reloading.

    This class holds the implementation of the BshClassManager so that it
    can be separated from the core package.

    This class currently relies on 1.2 for BshClassLoader and weak references.
    Is there a workaround for weak refs?  If so we could make this work
    with 1.1 by supplying our own classloader code...

    See "http://www.beanshell.org/manual/classloading.html" for details
    on the bsh classloader architecture.

    Bsh has a multi-tiered class loading architecture.  No class loader is
    created unless/until a class is generated, the classpath is modified,
    or a class is reloaded.

    Note: we may need some synchronization in here

    Note on jdk1.2 dependency:

    We are forced to use weak references here to accommodate all of the
    fleeting namespace listeners.  (NameSpaces must be informed if the class
    space changes so that they can un-cache names).  I had the interesting
    thought that a way around this would be to implement BeanShell's own
    garbage collector...  Then I came to my senses and said - screw it,
    class re-loading will require 1.2.

    ---------------------

    Classloading precedence:

    in-script evaluated class (scripted class)
    in-script added / modified classpath

    optionally, external classloader
    optionally, thread context classloader

    plain Class.forName()
    source class (.java file in classpath)

    </pre>

*/
public class ClassManagerImpl extends BshClassManager
{
    static final String BSH_PACKAGE = "bsh";
    /**
        The classpath of the base loader.  Initially and upon reset() this is
        an empty instance of BshClassPath.  It grows as paths are added or is
        reset when the classpath is explicitly set.  This could also be called
        the "extension" class path, but is not strictly confined to added path
        (could be set arbitrarily by setClassPath())
    */
    private BshClassPath baseClassPath;
    private boolean superImport;

    /**
        This is the full blown classpath including baseClassPath (extensions),
        user path, and java bootstrap path (rt.jar)

        This is lazily constructed and further (and more importantly) lazily
        intialized in components because mapping the full path could be
        expensive.

        The full class path is a composite of:
            baseClassPath (user extension) : userClassPath : bootClassPath
        in that order.
    */
    private BshClassPath fullClassPath;

    // ClassPath Change listeners
    private final Set<WeakReference<Listener>> listeners = ConcurrentHashMap.newKeySet();
    private final ReferenceQueue<Listener> refQueue = new ReferenceQueue<>();

    /**
        This handles extension / modification of the base classpath
        The loader to use where no mapping of reloaded classes exists.

        The baseLoader is initially null meaning no class loader is used.
    */
    private BshClassLoader baseLoader;

    /**
        Map by classname of loaders to use for reloaded classes
    */
    private final Map<String, DiscreteFilesClassLoader> loaderMap = new ConcurrentHashMap<>();

    /**
        Used by BshClassManager singleton constructor
    */
    public ClassManagerImpl() {
        reset();
    }

    /**
        @return the class or null
    */
    @Override
    public Class<?> classForName( String name )
    {
        // check positive cache
        Class<?> c = absoluteClassCache.get(name);
        if (c != null )
            return c;

        // check negative cache
        if ( absoluteNonClasses.contains(name) ) {
            Interpreter.debug("absoluteNonClass list hit: ", name);
            return null;
        }

        Interpreter.debug("Trying to load class: ", name);

        // Check explicitly mapped (reloaded) class...
        final ClassLoader overlayLoader = getLoaderForClass( name );
        if ( overlayLoader != null ) {
            try {
                c = overlayLoader.loadClass(name);
            } catch ( Exception e ) {
                Interpreter.debug("overlay loader failed for '", "' - ", e);
            }
            // Should be there since it was explicitly mapped
            // throw an error?
        }

        // insure that core classes are loaded from the same loader
        if ( c == null && name.startsWith(BSH_PACKAGE) ) {
            final ClassLoader myClassLoader = Interpreter.class.getClassLoader(); // is null if located in bootclasspath
            if (myClassLoader != null) {
                try {
                    c = myClassLoader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // fall through
                } catch (NoClassDefFoundError e) {
                    // fall through
                }
            } else {
                try {
                    c = Class.forName( name );
                } catch ( ClassNotFoundException e ) {
                    // fall through
                } catch ( NoClassDefFoundError e ) {
                    // fall through
                }
            }
        }

        // Check classpath extension / reloaded classes
        if ( c == null && baseLoader != null ) {
            try {
                c = baseLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // fall through
            }
        }

        // Optionally try external classloader
        if ( c == null && externalClassLoader != null ) {
            try {
                c = externalClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // fall through
            }
        }

        // Optionally try context classloader
        // Note that this might be a security violation
        // is catching the SecurityException sufficient for all environments?
        // or do we need a way to turn this off completely?
        if ( c ==  null ) {
            try {
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if ( contextClassLoader != null ) {
                    c = Class.forName( name, true, contextClassLoader );
                }
            } catch ( ClassNotFoundException e ) {
                // fall through
            } catch ( NoClassDefFoundError e ) {
                // fall through
            } catch ( SecurityException e ) {
                // fall through
            }
        }

        // try plain class forName()
        if ( c == null )
            try {
                c = Class.forName( name );
            } catch ( ClassNotFoundException e ) {
                // fall through
/* I disagree with letting this fall through  -fschmidt
            } catch ( NoClassDefFoundError e ) {
                // fall through
*/
            } catch ( NoClassDefFoundError nce ) {

               /*
                * This error can happen when the internal class name has a different
                * spelling or capitalization than the external name of the resource
                * that was loaded. This can happen in a number or ways, but a common
                * situation is case insensitive files systems like Windows, where a
                * request to load a class named 'a' may load a resource 'A.class'
                * where the internal class name of 'A' does not match 'a'.
                *
                * This can also happen on Windows OS when trying to reference a non-existing
                * variable that has similar spelling but different capitalization than
                * an imported class.  For example if a class named 'Foo' has been imported,
                * and a script references 'foo', then this loader will try to load foo.class,
                * which due to the case-insensitive file system will load Foo.class, and then
                * the loader will throw the error because class in the file is Foo but it
                * was looking for foo.
                *
                * What to do about this?  It is fundamentally a Java/Windows mismatch
                * issue and BeanShell should not have to do anything.  However not
                * catching the Error causes the Interpreter to blow up which is unfriendly.
                * I can think of three options of what to do:
                *
                * 1) Catch the error and silently fall through.  The next step after this
                * will try to load the class by name from a Java source file, and maybe this
                * is what was intended.  If a matching java source file is not found then
                * ultimately an EvalError is thrown.
                *
                * 2) Same as above, but write a line to System.err indicating that a class
                * resource had an mismatching internal name.  This might help to shed light
                * when things are mysteriously not workig as expected.
                *
                * 3) Wrap the exception in an EvalError.  This wont do any magic and will
                * put the burden on the developer to fix their problem if it is a Windows
                * case insensitivty problem.  However if there is a java source file that
                * could be loaded then it wont happen, so this breaks things.
                */

               System.err.println("The class named '"+name+"' could not be found, either because it no longer exists or it is not contained in the class file of the same name. Caused by: "+nce.getMessage());
            }

        // Try scripted class
        if ( c == null )
            c = loadSourceClass( name );

        // Cache result (or null for not found)
        cacheClassInfo( name, c );

        return c;
    }

    /**
        Get a resource URL using the BeanShell classpath
        @param path should be an absolute path
    */
    @Override
    public URL getResource( String path )
    {
        URL url = null;
        if ( baseLoader != null )
            // classloader wants no leading slash
            url = baseLoader.getResource( path.substring(1) );
        if ( url == null )
            url = super.getResource( path );
        return url;
    }

    /**
        Get a resource stream using the BeanShell classpath
        @param path should be an absolute path
    */
    @Override
    public InputStream getResourceAsStream( String path )
    {
        Object in = null;
        if ( null != baseLoader )
            // classloader wants no leading slash
            in = baseLoader.getResourceAsStream( path.substring(1) );
        if ( null == in )
            return super.getResourceAsStream( path );
        return (InputStream) in;
    }

    ClassLoader getLoaderForClass( String name ) {
        return (ClassLoader)loaderMap.get( name );
    }

    // Classpath mutators

    /**
    */
    @Override
    public void addClassPath( URL path )
        throws IOException
    {
        if ( baseLoader == null )
            setClassPath( new URL [] { path } );
        else {
            // opportunity here for listener in classpath
            baseLoader.addURL( path );
            baseClassPath.add( path );
            classLoaderChanged();
        }
    }

    /**
        Clear all classloading behavior and class caches and reset to
        initial state.
    */
    @Override
    public void reset()
    {
        baseClassPath = new BshClassPath("baseClassPath");
        baseLoader = null;
        loaderMap.clear();
        classLoaderChanged(); // calls clearCaches() for us.
    }

    /**
        Set a new base classpath and create a new base classloader.
        This means all types change.
    */
    @Override
    public void setClassPath( URL [] cp ) {
        baseClassPath.setPath( cp );
        initBaseLoader();
        loaderMap.clear();
        classLoaderChanged();
    }

    /**
        Overlay the entire path with a new class loader.
        Set the base path to the user path + base path.

        No point in including the boot class path (can't reload thos).
    */
    @Override
    public void reloadAllClasses() throws ClassPathException
    {
        BshClassPath bcp = new BshClassPath("temp");
        bcp.addComponent( baseClassPath );
        bcp.addComponent( BshClassPath.getUserClassPath() );
        setClassPath( bcp.getPathComponents() );
    }

    /**
        init the baseLoader from the baseClassPath
    */
    private void initBaseLoader() {
        baseLoader = new BshClassLoader( this, baseClassPath );
    }

    // class reloading

    /**
        Reloading classes means creating a new classloader and using it
        whenever we are asked for classes in the appropriate space.
        For this we use a DiscreteFilesClassLoader
    */
    @Override
    public void reloadClasses( String [] classNames )
        throws ClassPathException
    {
        clearCaches();

        // validate that it is a class here?

        // init base class loader if there is none...
        if ( baseLoader == null )
            initBaseLoader();

        DiscreteFilesClassLoader.ClassSourceMap map =
            new DiscreteFilesClassLoader.ClassSourceMap();

        for (int i=0; i< classNames.length; i++) {
            String name = classNames[i];

            // look in baseLoader class path
            ClassSource classSource = baseClassPath.getClassSource( name );

            // look in user class path
            if ( classSource == null ) {
                BshClassPath.getUserClassPath().insureInitialized();
                classSource = BshClassPath.getUserClassPath().getClassSource(
                    name );
            }

            // No point in checking boot class path, can't reload those.
            // else we could have used fullClassPath above.

            if ( classSource == null )
                throw new ClassPathException("Nothing known about class: "
                    +name );

            // JarClassSource is not working... just need to implement it's
            // getCode() method or, if we decide to, allow the BshClassManager
            // to handle it... since it is a URLClassLoader and can handle JARs
            if ( classSource instanceof JarClassSource )
                throw new ClassPathException("Cannot reload class: "+name+
                    " from source: "+ classSource );

            map.put( name, classSource );
        }

        // Create classloader for the set of classes
        DiscreteFilesClassLoader.newInstance( this, map );

        // map those classes the loader in the overlay map
        Iterator<String> it = map.keySet().iterator();
        while ( it.hasNext() )
            loaderMap.put( it.next(), DiscreteFilesClassLoader.instance() );
        classLoaderChanged();
    }

    /**
        Reload all classes in the specified package: e.g. "com.sun.tools"

        The special package name "<unpackaged>" can be used to refer
        to unpackaged classes.
    */
    @Override
    public void reloadPackage( String pack )
        throws ClassPathException
    {
        Collection<String> classes =
            baseClassPath.getClassesForPackage( pack );

        if ( classes == null )
            classes =
                BshClassPath.getUserClassPath().getClassesForPackage( pack );

        // no point in checking boot class path, can't reload those

        if ( classes == null )
            throw new ClassPathException("No classes found for package: "+pack);

        reloadClasses( classes.toArray( new String[classes.size()] ) );
    }

    /**
        Unimplemented
        For this we'd have to store a map by location as well as name...

    public void reloadPathComponent( URL pc ) throws ClassPathException {
        throw new ClassPathException("Unimplemented!");
    }
    */

    // end reloading

    /**
        Get the full blown classpath.
    */
    public BshClassPath getClassPath() throws ClassPathException
    {
        if ( fullClassPath != null )
            return fullClassPath;

        fullClassPath = new BshClassPath("BeanShell Full Class Path");
        fullClassPath.addComponent( BshClassPath.getUserClassPath() );
        try {
            fullClassPath.addComponent( BshClassPath.getBootClassPath() );
        } catch ( ClassPathException e ) {
            System.err.println("Warning: can't get boot class path");
        }
        fullClassPath.addComponent( baseClassPath );

        return fullClassPath;
    }

    /**
        Support for "import *;"
        Hide details in here as opposed to NameSpace.
    */
    @Override
    public void doSuperImport()
        throws UtilEvalError
    {
        // Should we prevent it from happening twice?

        try {
            getClassPath().insureInitialized();
            // prime the lookup table
            getClassNameByUnqName( "" ) ;

            // always true now
            //getClassPath().setNameCompletionIncludeUnqNames(true);

        } catch ( ClassPathException e ) {
            throw new UtilEvalError("Error importing classpath "+ e, e);
        }

        superImport = true;
    }

    @Override
    protected boolean hasSuperImport() { return superImport; }

    /**
        Return the name or null if none is found,
        Throw an ClassPathException containing detail if name is ambigous.
    */
    @Override
    public String getClassNameByUnqName( String name )
        throws ClassPathException
    {
        return getClassPath().getClassNameByUnqName( name );
    }

    @Override
    public void addListener(Listener l) {
        listeners.add(new WeakReference<Listener>(l, refQueue));

        // clean up old listeners
        Reference<? extends Listener> deadref;
        while ((deadref = refQueue.poll()) != null) {
            if (!listeners.remove(deadref))
                Interpreter.debug("tried to remove non-existent weak ref: ", deadref);
        }
    }

    @Override
    public void removeListener( Listener l ) {
        throw new Error("unimplemented");
    }

    public ClassLoader getBaseLoader() {
        return baseLoader;
    }

    /*
        Impl Notes:
        We add the bytecode source and the "reload" the class, which causes the
        BshClassLoader to be initialized and create a DiscreteFilesClassLoader
        for the bytecode.

        @exception ClassPathException can be thrown by reloadClasses
    */
    @Override
    public Class<?> defineClass( String name, byte [] code )
    {
        baseClassPath.setClassSource( name, new GeneratedClassSource( code ) );
        try {
             reloadClasses( new String [] { name } );
        } catch ( ClassPathException e ) {
            throw new bsh.InterpreterError("defineClass: "+e, e);
        }
        return classForName( name );
    }

    /**
        Clear global class cache and notify namespaces to clear their
        class caches.

        The listener list is implemented with weak references so that we
        will not keep every namespace in existence forever.
    */
    @Override
    protected void classLoaderChanged() {
        List<WeakReference<Listener>> toRemove = new ArrayList<>(); // safely remove
        for (WeakReference<Listener> wr : listeners) {
            Listener l = wr.get();
            if (l == null) // garbage collected
                toRemove.add(wr);
            else
                l.classLoaderChanged();
        }

        for (WeakReference<Listener> wr : toRemove)
            listeners.remove(wr);
    }

    @Override
    public void dump( PrintWriter i )
    {
        i.println("Bsh Class Manager Dump: ");
        i.println("----------------------- ");
        i.println("baseLoader = "+baseLoader);
        i.println("loaderMap= "+loaderMap);
        i.println("----------------------- ");
        i.println("baseClassPath = "+baseClassPath);
    }

}
