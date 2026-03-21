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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import bsh.ClassPathException;
import bsh.NameSource;

/**
    A BshClassPath encapsulates knowledge about a class path of URLs.
    It can maps all classes the path which may include:
        jar/zip files and base dirs

    A BshClassPath may composite other BshClassPaths as components of its
    path and will reflect changes in those components through its methods
    and listener interface.

    Classpath traversal is done lazily when a call is made to
        getClassesForPackage() or getClassSource()
    or can be done explicitily through insureInitialized().
    Feedback on mapping progress is provided through the MappingFeedback
    interface.

    Design notes:
    Several times here we traverse ourselves and our component paths to
    produce a composite view of some thing relating to the path.  This would
    be an opportunity for a visitor pattern.
*/
public class BshClassPath
    implements ClassPathListener, NameSource
{
    String name;

    /** The URL path components */
    private final Set<URL> path = ConcurrentHashMap.newKeySet();
    /** Ordered list of components BshClassPaths */
    private final Set<BshClassPath> compPaths = ConcurrentHashMap.newKeySet();

    /** Set of classes in a package mapped by package name */
    private final Map<String, Set<String>> packageMap = new ConcurrentHashMap<>();
    /** Map of source (URL or File dir) of every clas */
    private final Map<String, ClassSource> classSource = new ConcurrentHashMap<>();
    /**  The packageMap and classSource maps have been built. */
    private boolean mapsInitialized;

    private UnqualifiedNameTable unqNameTable;

    /**
        This used to be configurable, but now we always include them.
    */
    private boolean nameCompletionIncludesUnqNames = true;

    Vector<WeakReference<ClassPathListener>> listeners = new Vector<>();

    // constructors

    public BshClassPath( String name ) {
        this.name = name;
        reset();
    }

    public BshClassPath(  String name, URL[] urls ) {
        this( name );
        add( urls );
    }

    // end constructors

    // mutators

    public void setPath( URL[] urls ) {
        reset();
        add( urls );
    }

    /**
        Add the specified BshClassPath as a component of our path.
        Changes in the bcp will be reflected through us.
    */
    public void addComponent( BshClassPath bcp ) {
        if (bcp == null) return;
        compPaths.add( bcp );
        bcp.addListener( this );
    }

    public void add( URL[] urls ) {
        path.addAll( Arrays.asList(urls) );
        if ( mapsInitialized )
            map( urls );
    }

    public void add( URL url ) throws IOException {
        path.add(url);
        if ( mapsInitialized )
            map( url );
    }

    /**
        Get the path components including any component paths.
    */
    public URL[] getPathComponents() {
        return getFullPath().toArray( new URL[0] );
    }

    /**
        Return the set of class names in the specified package
        including all component paths.
    */
    public Set<String> getClassesForPackage( String pack ) {
        insureInitialized();
        Set<String> set = new HashSet<>();
        Collection<String> c = packageMap.get( pack );
        if ( c != null )
            set.addAll( c );

        compPaths.forEach((cp) -> {
            Collection<String> cf = cp.getClassesForPackage(pack);
            if (cf != null)
                set.addAll(cf);
        });

        return set;
    }

    /**
        Return the source of the specified class which may lie in component
        path.
    */
    public ClassSource getClassSource( String className )
    {
        // Before triggering classpath mapping (initialization) check for
        // explicitly set class sources (e.g. generated classes).  These would
        // take priority over any found in the classpath anyway.
        ClassSource cs = classSource.get( className );
        if ( cs != null )
            return cs;

        insureInitialized(); // trigger possible mapping

        Iterator<BshClassPath> it = compPaths.iterator();
        cs = classSource.get( className );
        while (cs == null && it.hasNext())
            cs = it.next().getClassSource(className);
        return cs;
    }

    /**
        Explicitly set a class source.  This is used for generated classes, but
        could potentially be used to allow a user to override which version of
        a class from the classpath is located.
    */
    public void setClassSource( String className, ClassSource cs )
    {
        classSource.put( className, cs );
    }

    /**
        If the claspath map is not initialized, do it now.
        If component maps are not do them as well...

        Random note:
        Should this be "insure" or "ensure".  I know I've seen "ensure" used
        in the JDK source.  Here's what Webster has to say:

            Main Entry:ensure Pronunciation:in-'shur
            Function:transitive verb Inflected
            Form(s):ensured; ensuring : to make sure,
            certain, or safe : GUARANTEE synonyms ENSURE,
            INSURE, ASSURE, SECURE mean to make a thing or
            person sure. ENSURE, INSURE, and ASSURE are
            interchangeable in many contexts where they
            indicate the making certain or inevitable of an
            outcome, but INSURE sometimes stresses the
            taking of necessary measures beforehand, and
            ASSURE distinctively implies the removal of
            doubt and suspense from a person's mind. SECURE
            implies action taken to guard against attack or
            loss.
    */
    public void insureInitialized()
    {
        insureInitialized( true );
    }

     /**
     * This is the top level classpath component.
     * @param topPath the start Class Mapping message
     */
    protected void insureInitialized( boolean topPath )
    {
        // If we are the top path and haven't been initialized before
        // inform the listeners we are going to do expensive map
        if ( topPath && !mapsInitialized )
            startClassMapping();

        // initialize components
        compPaths.forEach((cp) -> cp.insureInitialized(false));

        // initialize ourself
        if ( !mapsInitialized )
            map( path.toArray( new URL[0] ) );

        if ( topPath && !mapsInitialized )
            endClassMapping();

        mapsInitialized = true;
    }

    /**
        Get the full path including component paths.
        (component paths listed first, in order)
        Duplicate path components are removed.
    */
    protected List<URL> getFullPath()
    {
        List<URL> list = new ArrayList<>();
        compPaths.forEach((cp) -> {
            List<URL> l = cp.getFullPath();
            // take care to remove dups
            // wish we had an ordered set collection
            Iterator<URL> it = l.iterator();
            while ( it.hasNext() ) {
                URL o = it.next();
                if ( !list.contains(o) )
                    list.add( o );
            }
        });
        list.addAll( path );
        return list;
    }


    /**
        Support for super import "*";
        Get the full name associated with the unqualified name in this
        classpath.  Returns either the String name or an AmbiguousName object
        encapsulating the various names.
    */
    public String getClassNameByUnqName( String name )
        throws ClassPathException
    {
        insureInitialized();
        AmbiguousName aName = getUnqualifiedNameTable().get( name );

        if (null == aName) return null;

        List<String> names = aName.get();
        if (names.size() != 1)
          throw new ClassPathException("Ambiguous class names: "+ names);

        return names.get(0);
    }

    /*
        Note: we could probably do away with the unqualified name table
        in favor of a second name source
    */
    private UnqualifiedNameTable getUnqualifiedNameTable() {
        if ( unqNameTable == null )
            unqNameTable = buildUnqualifiedNameTable();
        return unqNameTable;
    }

    private UnqualifiedNameTable buildUnqualifiedNameTable()
    {
        final UnqualifiedNameTable unqNameTable = new UnqualifiedNameTable();

        // add component names
        compPaths.forEach((cp) -> {
            Set<String> s = cp.classSource.keySet();
            s.forEach((nt) -> unqNameTable.add( nt ));
        });

        // add ours
        classSource.keySet().forEach((nt) -> unqNameTable.add( nt ));

        return unqNameTable;
    }

    public String[] getAllNames()
    {
        insureInitialized();

        final List<String> names = new ArrayList<>();
        getPackagesSet().forEach((pack) -> names.addAll(
            removeInnerClassNames( getClassesForPackage( pack ))));

        if ( nameCompletionIncludesUnqNames )
            names.addAll( getUnqualifiedNameTable().keySet() );

        return names.toArray(new String[names.size()]);
    }

    /**
        call map(url) for each url in the array
    */
    void map( URL[] urls )
    {
        for (int i=0; i< urls.length; i++) try{
            map( urls[i] );
        } catch ( Exception e ) {
            String s = "Error constructing classpath: " +urls[i]+": "+e;
            errorWhileMapping( s );
            throw new RuntimeException("Failed to map class path "+i, e);
        }
    }

    void map( URL url ) throws IOException
    {
        if ("jrt".equals(url.getProtocol())) {
            classMapping("FileSystem: "+url );
            map( searchJrtFSForClasses( url ), new JrtClassSource(url) );
        } else  if ("jar".equals(url.getProtocol())) {
            classMapping("FileSystem: "+url );
            map( searchJarFSForClasses( url ), new JarClassSource(url) );
        } else {
            String name = url.getFile();
            File f = new File( name );

            if ( f.isDirectory() ) {
                classMapping( "Directory "+ f.toString() );
                map( traverseDirForClasses( f ), new DirClassSource(f) );
            } else if ( isArchiveFileName( name ) ) {
                classMapping("Archive: "+url );
                map( searchArchiveForClasses( url ), new JarClassSource(url) );
            } else {
                String s = "Not a classpath component: "+ name ;
                errorWhileMapping( s );
            }
        }
    }

    private void map( String[] classes, ClassSource source ) {
        for (int i=0; i< classes.length; i++) {
            //System.out.println( classes[i] +": "+ source );
            mapClass( classes[i], source );
        }
    }

    private void mapClass( String className, ClassSource source )
    {
        // add to package map
        String[] sa = splitClassname( className );
        String pack = sa[0];
        Set<String> set = packageMap.get( pack );
        if ( set == null ) {
            set = new HashSet<>();
            packageMap.put( pack, set );
        }
        set.add( className );

        // Add to classSource map
        Object obj = classSource.get( className );
        // don't replace previously set (found earlier in classpath or
        // explicitly set via setClassSource() )
        if ( obj == null )
            classSource.put( className, source );
    }

    /**
        Clear everything and reset the path to empty.
    */
    private void reset() {
        path.clear();
        compPaths.clear();
        clearCachedStructures();
    }

    /**
        Clear anything cached.  All will be reconstructed as necessary.
    */
    private void clearCachedStructures() {
        mapsInitialized = false;
        packageMap.clear();
        classSource.clear();
        unqNameTable = null;
        nameSpaceChanged();
    }

    public void classPathChanged() {
        clearCachedStructures();
        notifyListeners();
    }

    // Begin Static stuff

    static String[] traverseDirForClasses( File dir )
        throws IOException
    {
        List<String> list = traverseDirForClassesAux( dir, dir );
        return list.toArray(new String[list.size()]);
    }

    static List<String> traverseDirForClassesAux( File topDir, File dir )
        throws IOException
    {
        List<String> list = new ArrayList<>();
        String top = topDir.getAbsolutePath();

        File[] children = dir.listFiles();
        if ( null == children )
            children = new File[0];
        for (int i=0; i< children.length; i++)  {
            File child = children[i];
            if ( child.isDirectory() )
                list.addAll( traverseDirForClassesAux( topDir, child ) );
            else {
                String name = child.getAbsolutePath();
                if ( isClassFileName( name ) ) {
                    /*
                        Remove absolute (topdir) portion of path and leave
                        package-class part
                    */
                    if ( name.startsWith( top ) )
                        name = name.substring( top.length()+1 );
                    else
                        throw new IOException( "problem parsing paths" );

                    name = canonicalizeClassName(name);
                    list.add( name );
                }
            }
        }


        return list;
    }

    /** Search jrt file system for module classes.
     * @param url the jrt file system url
     * @return array of class names found
     * @throws IOException of any reading problems  */
    static String[] searchJrtFSForClasses( URL url ) throws IOException {
        try {
            Path path = FileSystems.getFileSystem(new URI("jrt:/")).getPath("modules", url.getPath());
            try (Stream<Path> stream = Files.walk(path)) {
                return stream.map(Path::toString)
                        .filter(BshClassPath::isClassFileName)
                        .map(BshClassPath::canonicalizeClassName)
                        .toArray(String[]::new);
            } catch (Exception e) { throw e; }
        } catch (URISyntaxException e) { /* ignore */ }
        return new String[0];
    }

    /** Search jar file system for classes.
     * @param url the jar file system url
     * @return array of class names found
     * @throws IOException of any reading problems  */
    static String[] searchJarFSForClasses( URL url ) throws IOException {
        try {
            try {
                FileSystems.newFileSystem(url.toURI(), new HashMap<>());
            } catch (FileSystemAlreadyExistsException e) { /* ignore */ }

            Path path = FileSystems.getFileSystem(url.toURI()).getPath("/");
            try (Stream<Path> stream = Files.walk(path)) {
                return stream.map(Path::toString)
                        .filter(BshClassPath::isClassFileName)
                        .map(BshClassPath::canonicalizeClassName)
                        .toArray(String[]::new);
            } catch (Exception e) { throw e; }
        } catch (URISyntaxException e) { /* ignore */ }
        return new String[0];
    }

    /** Search Archive for classes.
     * @param url the archive file location
     * @return array of class names found
     * @throws IOException of any reading problems  */
    static String[] searchArchiveForClasses( URL url ) throws IOException {
        List<String> list = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(url.openStream());

        ZipEntry ze;
        while( zip.available() == 1 )
            if ( (ze = zip.getNextEntry()) != null
                    && isClassFileName( ze.getName() ) )
                list.add( canonicalizeClassName( ze.getName() ) );
        zip.close();

        return list.toArray( new String[list.size()] );
    }

    public static boolean isClassFileName( String name ){
        return name.toLowerCase().endsWith(".class");
    }

    public static boolean isArchiveFileName( String name ){
        name = name.toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".jmod");
    }

    /**
        Create a proper class name from a messy thing.
        Turn / or \ into .,  remove leading class and trailing .class

        Only makes strings if necessary
    */
    private static final Pattern slashDot = Pattern.compile("[/\\\\]");
    private static final Pattern moduleName = Pattern.compile("^modules/[^/]+/");
    private static final Pattern dotClass = Pattern.compile("\\.[^\\.]+$");
    public static String canonicalizeClassName( String name )
    {
        String classname = name;
        if ( classname.startsWith("modules/") )
            classname = moduleName.matcher(classname).replaceFirst("");
        if (classname.indexOf('/') >= 0 || classname.indexOf('\\') >= 0)
            classname = slashDot.matcher(classname).replaceAll(".");
        if ( classname.startsWith(".") )
            classname = classname.substring(1);
        if ( classname.startsWith("class ") )
            classname = classname.substring(6);
        if ( classname.startsWith("classes.") )
            classname = classname.substring(8);
        if ( classname.endsWith(".class") )
            classname = dotClass.matcher(classname).replaceFirst("");
        return classname;
    }

    /**
        Split class name into package and name
    */
    private static final Pattern splitClass = Pattern.compile("\\.(?=[^.]+$)");
    public static String[] splitClassname ( String classname ) {
        classname = canonicalizeClassName( classname );

        if (classname.indexOf('.') == -1)
            return new String[] { "<unpackaged>", classname };
        return splitClass.split(classname);
    }

    /**
        Return a new collection without any inner class names
    */
    public static Collection<String> removeInnerClassNames( Collection<String> col ) {
        List<String> list = new ArrayList<>();
        list.addAll(col);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
             if (it.next().indexOf("$") != -1 )
                it.remove();
        }
        return list;
    }

    /**
        The user classpath from system property
            java.class.path
    */
    private static URL[] userClassPathComp;
    public static URL[] getUserClassPathComponents()
        throws ClassPathException
    {
        if ( userClassPathComp != null )
            return userClassPathComp;

        String cp = System.getProperty("java.class.path");
        String[] paths = null == cp ? new String[0] : cp.split(File.pathSeparator);

        URL[] urls = new URL[ paths.length ];
        try {
            for ( int i=0; i<paths.length; i++)
                // We take care to get the canonical path first.
                // Java deals with relative paths for it's bootstrap loader
                // but JARClassLoader doesn't.
                urls[i] = new File(
                    new File(paths[i]).getCanonicalPath() ).toURI().toURL();
        } catch ( IOException e ) {
            throw new ClassPathException("can't parse class path: "+e, e);
        }

        userClassPathComp = urls;
        return urls;
    }

    /**
        Get a list of all of the known packages
    */
    public Set<String> getPackagesSet()
    {
        insureInitialized();
        final Set<String> set = new HashSet<>();
        set.addAll( packageMap.keySet() );

        compPaths.forEach((cp) -> set.addAll( cp.packageMap.keySet() ));
        return set;
    }

    public void addListener( ClassPathListener l ) {
        listeners.addElement( new WeakReference<ClassPathListener>(l) );
    }
    public void removeListener( ClassPathListener l ) {
        for ( Iterator<WeakReference<ClassPathListener>> it = listeners.iterator() ;
                it.hasNext() ; )
            if (it.next().get() == l)
                it.remove();
    }
    void notifyListeners() {
        ClassPathListener l;
        for ( Iterator<WeakReference<ClassPathListener>> it = listeners.iterator() ;
                it.hasNext() ; )
            if ((l = it.next().get()) == null)
                it.remove();
            else
                l.classPathChanged();
    }

    /**
        A BshClassPath initialized to the user path
        from java.class.path
    */
    private static BshClassPath userClassPath;
    public static BshClassPath getUserClassPath()
        throws ClassPathException
    {
        if ( userClassPath == null )
            userClassPath = new BshClassPath(
                "User Class Path", getUserClassPathComponents() );
        return userClassPath;
    }

    /**
        Get the boot path including the lib/rt.jar if possible.
    */
    private static BshClassPath bootClassPath;
    public static BshClassPath getBootClassPath()
        throws ClassPathException
    {
        if ( bootClassPath == null ) try {
            bootClassPath = new BshClassPath(
                    "Boot Class Path", new URL[] { getRTJarPath() } );
        } catch ( MalformedURLException e ) {
            throw new ClassPathException(" can't find boot jar: "+e, e);
        }
        return bootClassPath;
    }


    private static URL getRTJarPath() throws MalformedURLException
    {
        String urlString =
            Class.class.getResource("/java/lang/String.class").toExternalForm();

        if ( urlString.startsWith("jrt:/") )
            return new URL(urlString.substring(0, urlString.indexOf('/', 5)));

        return new URL(urlString.replaceFirst("[^!]*$", "/"));
    }

    public abstract static class ClassSource {
        Object source;
        abstract byte[] getCode( String className );
    }

    public static class JarClassSource extends ClassSource {
        JarClassSource( URL url ) { source = url; }
        public URL getURL() { return (URL)source; }
        public byte[] getCode( String className ) {
            String n = '/' + className.replace( '.', '/' ) + ".class";
            try (URLClassLoader urlc = new URLClassLoader(new URL[] { getURL() });
                DataInputStream in = new DataInputStream(
                    urlc.loadClass(className).getResourceAsStream(n))) {
                byte[] bytes = new byte[in.available()];
                in.readFully(bytes);
                return bytes;
            } catch (IOException | ClassNotFoundException e) { /* ignore */ }
            return new byte[0];
        }
        public String toString() { return "Jar: "+source; }
    }

    public static class DirClassSource extends ClassSource
    {
        DirClassSource( File dir ) { source = dir; }
        public File getDir() { return (File)source; }
        public String toString() { return "Dir: "+source; }

        public byte[] getCode( String className ) {
            return readBytesFromFile( getDir(), className );
        }

        public static byte[] readBytesFromFile( File base, String className )
        {
            String n = className.replace( '.', File.separatorChar ) + ".class";
            File file = new File( base, n );

            if ( !file.exists() )
                return null;

            byte[] bytes;
            try ( FileInputStream fis = new FileInputStream(file);
                    DataInputStream dis = new DataInputStream( fis ) ) {

                bytes = new byte [ (int)file.length() ];

                dis.readFully( bytes );
                dis.close();
            } catch(IOException ie ) {
                throw new RuntimeException("Couldn't load file: "+file, ie);
            }

            return bytes;
        }

    }

    public static class JrtClassSource extends ClassSource {

        JrtClassSource( URL url ) { source = url; }

        public URL getURL() { return (URL) source; }

        public byte[] getCode( String className ) {
            String n = '/' + className.replace( '.', '/' ) + ".class";
            try (DataInputStream in = new DataInputStream(
                    (InputStream) new URL(source + n).getContent())) {
                byte[] bytes = new byte[in.available()];
                in.readFully(bytes);
                return bytes;
            } catch (IOException e) { /* ignore */ }
            return new byte[0];
        }

        public String toString() { return "Jrt: "+source; }
    }

    public static class GeneratedClassSource extends ClassSource
    {
        GeneratedClassSource( byte[] bytecode ) { source = bytecode; }
        public byte[] getCode( String className ) {
            return (byte[])source;
        }
    }

    public String toString() {
        return "BshClassPath "+name+"("+super.toString()+") path= "+path +"\n"
            + "compPaths = {" + compPaths +" }";
    }


    /*
        Note: we could probably do away with the unqualified name table
        in favor of a second name source
    */
    static class UnqualifiedNameTable extends HashMap<String, AmbiguousName> {
        private static final long serialVersionUID = 1L;

        void add( String fullname ) {
            String name = splitClassname( fullname )[1];

            if ( !super.containsKey(name))
                super.put( name, new AmbiguousName(fullname) );
            else
                super.get(name).add(fullname);
        }
    }

    public static class AmbiguousName {
        List<String> list = new ArrayList<>();
        public AmbiguousName(String name) {
            list.add(name);
        }
        public void add( String name ) {
            list.add( name );
        }
        public List<String> get() {
            //return (String[])list.toArray(new String[0]);
            return list;
        }
    }

    /**
        Fire the NameSourceListeners
    */
    private List<NameSource.Listener> nameSourceListeners;
    void nameSpaceChanged()
    {
        if ( nameSourceListeners == null )
            return;

        for(int i=0; i<nameSourceListeners.size(); i++)
            nameSourceListeners.get(i).nameSourceChanged( this );
    }

    /**
        Implements NameSource
        Add a listener who is notified upon changes to names in this space.
    */
    public void addNameSourceListener( NameSource.Listener listener ) {
        if ( nameSourceListeners == null )
            nameSourceListeners = new ArrayList<>();
        nameSourceListeners.add( listener );
    }

    /** only allow one for now */
    private static MappingFeedback mappingFeedbackListener;

    /**
     * Add a mapping feedback callback class. If one already exists a runtime
     * exception is thrown.
     * @param mf the mapping feedback callback listener.
     */
    public static void addMappingFeedback( MappingFeedback mf )
    {
        if ( mappingFeedbackListener != null )
            throw new RuntimeException("Unimplemented: already a listener");
        mappingFeedbackListener = mf;
    }

    void startClassMapping() {
        if ( mappingFeedbackListener != null )
            mappingFeedbackListener.startClassMapping();
        else
            System.err.println( "Start ClassPath Mapping" );
    }

    void classMapping( String msg ) {
        if ( mappingFeedbackListener != null ) {
            mappingFeedbackListener.classMapping( msg );
        } else
            System.err.println( "Mapping: "+msg );
    }

    void errorWhileMapping( String s ) {
        if ( mappingFeedbackListener != null )
            mappingFeedbackListener.errorWhileMapping( s );
        else
            System.err.println( s );
    }

    void endClassMapping() {
        if ( mappingFeedbackListener != null )
            mappingFeedbackListener.endClassMapping();
        else
            System.err.println( "End ClassPath Mapping" );
    }

    static interface MappingFeedback
    {
        void startClassMapping();

        /**
            Provide feedback on the progress of mapping the classpath
            @param msg is a message about the path component being mapped
       */
        void classMapping( String msg );

        void errorWhileMapping( String msg );

        void endClassMapping();
    }


    /**
     * Whether map is initialized.
     * @return boolean return the mapsInitialized
     */
    public boolean isMapsInitialized() {
        return mapsInitialized;
    }

    /**
     * Change whether map is initialized.
     * @param mapsInitialized the mapsInitialized to set
     */
    public void setMapsInitialized(boolean mapsInitialized) {
        this.mapsInitialized = mapsInitialized;
    }

    /**
     * Get the unqualified name table.
     * @return UnqualifiedNameTable return the unqNameTable
     */
    public UnqualifiedNameTable getUnqNameTable() {
        return unqNameTable;
    }

    /**
     * Change the unqualified name table.
     * @param unqNameTable the unqNameTable to set
     */
    public void setUnqNameTable(UnqualifiedNameTable unqNameTable) {
        this.unqNameTable = unqNameTable;
    }

    /**
     * Whether name completion includes unqualified names.
     * @return boolean return the nameCompletionIncludesUnqNames
     */
    public boolean isNameCompletionIncludesUnqNames() {
        return nameCompletionIncludesUnqNames;
    }

    /**
     * Change whether name completion includes unqualified names.
     * @param nameCompletionIncludesUnqNames the nameCompletionIncludesUnqNames to set
     */
    public void setNameCompletionIncludesUnqNames(boolean nameCompletionIncludesUnqNames) {
        this.nameCompletionIncludesUnqNames = nameCompletionIncludesUnqNames;
    }

    /**
     * Get the name source listeners list.
     * @return {@code List<NameSource.Listener>} return the nameSourceListeners
     */
    public List<NameSource.Listener> getNameSourceListeners() {
        return nameSourceListeners;
    }

    /**
     * Change the name source listeners list.
     * @param nameSourceListeners the nameSourceListeners to set
     */
    public void setNameSourceListeners(List<NameSource.Listener> nameSourceListeners) {
        this.nameSourceListeners = nameSourceListeners;
    }

}
