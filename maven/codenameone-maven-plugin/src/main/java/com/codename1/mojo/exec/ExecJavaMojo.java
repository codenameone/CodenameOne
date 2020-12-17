package com.codename1.mojo.exec;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;

/**
 * Executes the supplied java class in the current VM with the enclosing project's dependencies as classpath.
 * 
 * @author Kaare Nilsen (kaare.nilsen@gmail.com), David Smiley (dsmiley@mitre.org)
 * @since 1.0
 */
@Mojo( name = "java", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
@Execute(phase = LifecyclePhase.COMPILE)
public class ExecJavaMojo
    extends AbstractExecMojo
{
    @Component
    private DependencyResolver dependencyResolver;


    /**
     * @since 1.0
     */
    @Component
    private ProjectBuilder projectBuilder;

    /**
     * @since 1.1-beta-1
     */
    @Parameter( readonly = true, defaultValue = "${plugin.artifacts}" )
    private List<Artifact> pluginDependencies;

    /**
     * The main class to execute.<br>
     * With Java 9 and above you can prefix it with the modulename, e.g. <code>com.greetings/com.greetings.Main</code>
     * Without modulename the classpath will be used, with modulename a new modulelayer will be created.
     * 
     * @since 1.0
     */
    @Parameter( required = true, property = "exec.mainClass" )
    private String mainClass;

    /**
     * The class arguments.
     * 
     * @since 1.0
     */
    @Parameter( property = "exec.arguments" )
    private String[] arguments;

    /**
     * A list of system properties to be passed. Note: as the execution is not forked, some system properties required
     * by the JVM cannot be passed here. Use MAVEN_OPTS or the exec:exec instead. See the user guide for more
     * information.
     * 
     * @since 1.0
     */
    @Parameter
    private Property[] systemProperties;

    /**
     * Indicates if mojo should be kept running after the mainclass terminates. Use full for server like apps with
     * daemon threads.
     * 
     * @deprecated since 1.1-alpha-1
     * @since 1.0
     */
    @Parameter( property = "exec.keepAlive", defaultValue = "false" )
    @Deprecated
    private boolean keepAlive;

    /**
     * Indicates if the project dependencies should be used when executing the main class.
     * 
     * @since 1.1-beta-1
     */
    @Parameter( property = "exec.includeProjectDependencies", defaultValue = "true" )
    private boolean includeProjectDependencies;

    /**
     * Indicates if this plugin's dependencies should be used when executing the main class.
     * <p/>
     * This is useful when project dependencies are not appropriate. Using only the plugin dependencies can be
     * particularly useful when the project is not a java project. For example a mvn project using the csharp plugins
     * only expects to see dotnet libraries as dependencies.
     * 
     * @since 1.1-beta-1
     */
    @Parameter( property = "exec.includePluginsDependencies", defaultValue = "false" )
    private boolean includePluginDependencies;

    /**
     * Whether to interrupt/join and possibly stop the daemon threads upon quitting. <br/>
     * If this is <code>false</code>, maven does nothing about the daemon threads. When maven has no more work to do,
     * the VM will normally terminate any remaining daemon threads.
     * <p>
     * In certain cases (in particular if maven is embedded), you might need to keep this enabled to make sure threads
     * are properly cleaned up to ensure they don't interfere with subsequent activity. In that case, see
     * {@link #daemonThreadJoinTimeout} and {@link #stopUnresponsiveDaemonThreads} for further tuning.
     * </p>
     * 
     * @since 1.1-beta-1
     */
    @Parameter( property = "exec.cleanupDaemonThreads", defaultValue = "true" )
    private boolean cleanupDaemonThreads;

    /**
     * This defines the number of milliseconds to wait for daemon threads to quit following their interruption.<br/>
     * This is only taken into account if {@link #cleanupDaemonThreads} is <code>true</code>. A value &lt;=0 means to
     * not timeout (i.e. wait indefinitely for threads to finish). Following a timeout, a warning will be logged.
     * <p>
     * Note: properly coded threads <i>should</i> terminate upon interruption but some threads may prove problematic: as
     * the VM does interrupt daemon threads, some code may not have been written to handle interruption properly. For
     * example java.util.Timer is known to not handle interruptions in JDK &lt;= 1.6. So it is not possible for us to
     * infinitely wait by default otherwise maven could hang. A sensible default value has been chosen, but this default
     * value <i>may change</i> in the future based on user feedback.
     * </p>
     * 
     * @since 1.1-beta-1
     */
    @Parameter( property = "exec.daemonThreadJoinTimeout", defaultValue = "15000" )
    private long daemonThreadJoinTimeout;

    /**
     * Wether to call {@link Thread#stop()} following a timing out of waiting for an interrupted thread to finish. This
     * is only taken into account if {@link #cleanupDaemonThreads} is <code>true</code> and the
     * {@link #daemonThreadJoinTimeout} threshold has been reached for an uncooperative thread. If this is
     * <code>false</code>, or if {@link Thread#stop()} fails to get the thread to stop, then a warning is logged and
     * Maven will continue on while the affected threads (and related objects in memory) linger on. Consider setting
     * this to <code>true</code> if you are invoking problematic code that you can't fix. An example is
     * {@link java.util.Timer} which doesn't respond to interruption. To have <code>Timer</code> fixed, vote for
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6336543">this bug</a>.
     * 
     * @since 1.1-beta-1
     */
    @Parameter( property = "exec.stopUnresponsiveDaemonThreads", defaultValue = "false" )
    private boolean stopUnresponsiveDaemonThreads;

    /**
     * Deprecated this is not needed anymore.
     * 
     * @deprecated since 1.1-alpha-1
     * @since 1.0
     */
    @Parameter( property = "exec.killAfter", defaultValue = "-1" )
    @Deprecated
    private long killAfter;

    private Properties originalSystemProperties;

    /**
     * Additional elements to be appended to the classpath.
     * 
     * @since 1.3
     */
    @Parameter
    private List<String> additionalClasspathElements;

    /**
     * List of file to exclude from the classpath.
     * It matches the jar name, for example {@code slf4j-simple-1.7.30.jar}.
     *
     * @since 3.0.1
     */
    @Parameter
    private List<String> classpathFilenameExclusions;

    /**
     * Execute goal.
     * 
     * @throws MojoExecutionException execution of the main class or one of the threads it generated failed.
     * @throws MojoFailureException something bad happened...
     */
    @Override
    public void executeImpl()
        throws MojoExecutionException, MojoFailureException
    {
        mainClass = "com.codename1.impl.javase.Simulator";
        arguments = new String[]{properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName")};
        additionalClasspathElements = new ArrayList<String>();
        File seclasses = this.getProjectInternalTmpJar();
        if (seclasses != null && seclasses.exists()) {
            additionalClasspathElements.add(seclasses.getAbsolutePath());
        }
        
        if ( isSkip() )
        {
            getLog().info( "skipping execute as per configuration" );
            return;
        }
        if ( killAfter != -1 )
        {
            getLog().warn( "Warning: killAfter is now deprecated. Do you need it ? Please comment on MEXEC-6." );
        }

        if ( null == arguments )
        {
            arguments = new String[0];
        }

        if ( getLog().isDebugEnabled() )
        {
            StringBuffer msg = new StringBuffer( "Invoking : " );
            msg.append( mainClass );
            msg.append( ".main(" );
            for ( int i = 0; i < arguments.length; i++ )
            {
                if ( i > 0 )
                {
                    msg.append( ", " );
                }
                msg.append( arguments[i] );
            }
            msg.append( ")" );
            getLog().debug( msg );
        }

        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup( mainClass /* name */ );
        Thread bootstrapThread = new Thread( threadGroup, new Runnable()
        {
            public void run()
            {
                int sepIndex = mainClass.indexOf( '/' );

                final String bootClassName;
                if ( sepIndex >= 0 )
                {
                    bootClassName = mainClass.substring( sepIndex + 1 );
                }
                else 
                {
                    bootClassName = mainClass;
                }
                
                try
                {
                    Class<?> bootClass = Thread.currentThread().getContextClassLoader().loadClass( bootClassName );
                    
                    MethodHandles.Lookup lookup = MethodHandles.lookup();

                    MethodHandle mainHandle =
                        lookup.findStatic( bootClass, "main",
                                                 MethodType.methodType( void.class, String[].class ) );
                    
                    mainHandle.invoke( arguments );
                }
                catch ( IllegalAccessException e )
                { // just pass it on
                    Thread.currentThread().getThreadGroup().uncaughtException( Thread.currentThread(),
                                                                               new Exception( "The specified mainClass doesn't contain a main method with appropriate signature.",
                                                                                              e ) );
                }
                catch ( InvocationTargetException e )
                { // use the cause if available to improve the plugin execution output
                   Throwable exceptionToReport = e.getCause() != null ? e.getCause() : e;
                   Thread.currentThread().getThreadGroup().uncaughtException( Thread.currentThread(), exceptionToReport );
                }
                catch ( Throwable e )
                { // just pass it on
                    Thread.currentThread().getThreadGroup().uncaughtException( Thread.currentThread(), e );
                }
            }
        }, mainClass + ".main()" );
        URLClassLoader classLoader = getClassLoader();
        bootstrapThread.setContextClassLoader( classLoader );
        setSystemProperties();

        bootstrapThread.start();
        joinNonDaemonThreads( threadGroup );
        // It's plausible that spontaneously a non-daemon thread might be created as we try and shut down,
        // but it's too late since the termination condition (only daemon threads) has been triggered.
        if ( keepAlive )
        {
            getLog().warn( "Warning: keepAlive is now deprecated and obsolete. Do you need it? Please comment on MEXEC-6." );
            waitFor( 0 );
        }

        if ( cleanupDaemonThreads )
        {

            terminateThreads( threadGroup );

            try
            {
                threadGroup.destroy();
            }
            catch ( IllegalThreadStateException e )
            {
                getLog().warn( "Couldn't destroy threadgroup " + threadGroup, e );
            }
        }

        if ( classLoader != null )
        {
            try
            {
                classLoader.close();
            }
            catch ( IOException e )
            {
                getLog().error(e.getMessage(), e);
            }
        }

        if ( originalSystemProperties != null )
        {
            System.setProperties( originalSystemProperties );
        }

        synchronized ( threadGroup )
        {
            if ( threadGroup.uncaughtException != null )
            {
                throw new MojoExecutionException( "An exception occured while executing the Java class. "
                    + threadGroup.uncaughtException.getMessage(), threadGroup.uncaughtException );
            }
        }

        registerSourceRoots();
    }

    /**
     * a ThreadGroup to isolate execution and collect exceptions.
     */
    class IsolatedThreadGroup
        extends ThreadGroup
    {
        private Throwable uncaughtException; // synchronize access to this

        public IsolatedThreadGroup( String name )
        {
            super( name );
        }

        public void uncaughtException( Thread thread, Throwable throwable )
        {
            if ( throwable instanceof ThreadDeath )
            {
                return; // harmless
            }
            synchronized ( this )
            {
                if ( uncaughtException == null ) // only remember the first one
                {
                    uncaughtException = throwable; // will be reported eventually
                }
            }
            getLog().warn( throwable );
        }
    }

    private void joinNonDaemonThreads( ThreadGroup threadGroup )
    {
        boolean foundNonDaemon;
        do
        {
            foundNonDaemon = false;
            Collection<Thread> threads = getActiveThreads( threadGroup );
            for ( Thread thread : threads )
            {
                if ( thread.isDaemon() )
                {
                    continue;
                }
                foundNonDaemon = true; // try again; maybe more threads were created while we were busy
                joinThread( thread, 0 );
            }
        }
        while ( foundNonDaemon );
    }

    private void joinThread( Thread thread, long timeoutMsecs )
    {
        try
        {
            getLog().debug( "joining on thread " + thread );
            thread.join( timeoutMsecs );
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt(); // good practice if don't throw
            getLog().warn( "interrupted while joining against thread " + thread, e ); // not expected!
        }
        if ( thread.isAlive() ) // generally abnormal
        {
            getLog().warn( "thread " + thread + " was interrupted but is still alive after waiting at least "
                + timeoutMsecs + "msecs" );
        }
    }

    private void terminateThreads( ThreadGroup threadGroup )
    {
        long startTime = System.currentTimeMillis();
        Set<Thread> uncooperativeThreads = new HashSet<Thread>(); // these were not responsive to interruption
        for ( Collection<Thread> threads = getActiveThreads( threadGroup ); !threads.isEmpty(); threads =
            getActiveThreads( threadGroup ), threads.removeAll( uncooperativeThreads ) )
        {
            // Interrupt all threads we know about as of this instant (harmless if spuriously went dead (! isAlive())
            // or if something else interrupted it ( isInterrupted() ).
            for ( Thread thread : threads )
            {
                getLog().debug( "interrupting thread " + thread );
                thread.interrupt();
            }
            // Now join with a timeout and call stop() (assuming flags are set right)
            for ( Thread thread : threads )
            {
                if ( !thread.isAlive() )
                {
                    continue; // and, presumably it won't show up in getActiveThreads() next iteration
                }
                if ( daemonThreadJoinTimeout <= 0 )
                {
                    joinThread( thread, 0 ); // waits until not alive; no timeout
                    continue;
                }
                long timeout = daemonThreadJoinTimeout - ( System.currentTimeMillis() - startTime );
                if ( timeout > 0 )
                {
                    joinThread( thread, timeout );
                }
                if ( !thread.isAlive() )
                {
                    continue;
                }
                uncooperativeThreads.add( thread ); // ensure we don't process again
                if ( stopUnresponsiveDaemonThreads )
                {
                    getLog().warn( "thread " + thread + " will be Thread.stop()'ed" );
                    thread.stop();
                }
                else
                {
                    getLog().warn( "thread " + thread + " will linger despite being asked to die via interruption" );
                }
            }
        }
        if ( !uncooperativeThreads.isEmpty() )
        {
            getLog().warn( "NOTE: " + uncooperativeThreads.size() + " thread(s) did not finish despite being asked to "
                + " via interruption. This is not a problem with exec:java, it is a problem with the running code."
                + " Although not serious, it should be remedied." );
        }
        else
        {
            int activeCount = threadGroup.activeCount();
            if ( activeCount != 0 )
            {
                // TODO this may be nothing; continue on anyway; perhaps don't even log in future
                Thread[] threadsArray = new Thread[1];
                threadGroup.enumerate( threadsArray );
                getLog().debug( "strange; " + activeCount + " thread(s) still active in the group " + threadGroup
                    + " such as " + threadsArray[0] );
            }
        }
    }

    private Collection<Thread> getActiveThreads( ThreadGroup threadGroup )
    {
        Thread[] threads = new Thread[threadGroup.activeCount()];
        int numThreads = threadGroup.enumerate( threads );
        Collection<Thread> result = new ArrayList<Thread>( numThreads );
        for ( int i = 0; i < threads.length && threads[i] != null; i++ )
        {
            result.add( threads[i] );
        }
        return result; // note: result should be modifiable
    }

    /**
     * Pass any given system properties to the java system properties.
     */
    private void setSystemProperties()
    {
        if ( systemProperties != null )
        {
            originalSystemProperties = System.getProperties();
            for ( Property systemProperty : systemProperties )
            {
                String value = systemProperty.getValue();
                System.setProperty( systemProperty.getKey(), value == null ? "" : value );
            }
        }
    }

    /**
     * Set up a classloader for the execution of the main class.
     * 
     * @return the classloader
     * @throws MojoExecutionException if a problem happens
     */
    private URLClassLoader getClassLoader()
        throws MojoExecutionException
    {
        List<Path> classpathURLs = new ArrayList<>();
        this.addRelevantPluginDependenciesToClasspath( classpathURLs );
        this.addRelevantProjectDependenciesToClasspath( classpathURLs );
        this.addAdditionalClasspathElements( classpathURLs );
        
        try
        {
            return URLClassLoaderBuilder.builder()
                    .setLogger( getLog() )
                    .setPaths( classpathURLs )
                    .setExclusions( classpathFilenameExclusions )
                    .build();
        }
        catch ( NullPointerException | IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }

    private void addAdditionalClasspathElements( List<Path> path )
    {
        if ( additionalClasspathElements != null )
        {
            for ( String classPathElement : additionalClasspathElements )
            {
                Path file = Paths.get( classPathElement );
                if ( !file.isAbsolute() )
                {
                    file = project.getBasedir().toPath().resolve( file );
                }
                getLog().debug( "Adding additional classpath element: " + file + " to classpath" );
                path.add( file );
            }
        }
    }

    /**
     * Add any relevant project dependencies to the classpath. Indirectly takes includePluginDependencies and
     * ExecutableDependency into consideration.
     * 
     * @param path classpath of {@link java.net.URL} objects
     * @throws MojoExecutionException if a problem happens
     */
    private void addRelevantPluginDependenciesToClasspath( List<Path> path )
        throws MojoExecutionException
    {
        if ( hasCommandlineArgs() )
        {
            arguments = parseCommandlineArgs();
        }

        for ( Artifact classPathElement : this.determineRelevantPluginDependencies() )
        {
            getLog().debug( "Adding plugin dependency artifact: " + classPathElement.getArtifactId()
                + " to classpath" );
            path.add( classPathElement.getFile().toPath() );
        }
    }

    /**
     * Add any relevant project dependencies to the classpath. Takes includeProjectDependencies into consideration.
     * 
     * @param path classpath of {@link java.net.URL} objects
     * @throws MojoExecutionException if a problem happens
     */
    private void addRelevantProjectDependenciesToClasspath( List<Path> path )
        throws MojoExecutionException
    {
        if ( this.includeProjectDependencies )
        {
            getLog().debug( "Project Dependencies will be included." );

            List<Artifact> artifacts = new ArrayList<>();
            List<Path> theClasspathFiles = new ArrayList<>();

            collectProjectArtifactsAndClasspath( artifacts, theClasspathFiles );

            for ( Path classpathFile : theClasspathFiles )
            {
                getLog().debug( "Adding to classpath : " + classpathFile );
                path.add( classpathFile );
            }

            for ( Artifact classPathElement : artifacts )
            {
                getLog().debug( "Adding project dependency artifact: " + classPathElement.getArtifactId()
                    + " to classpath" );
                path.add( classPathElement.getFile().toPath() );
            }
        }
        else
        {
            getLog().debug( "Project Dependencies will be excluded." );
        }

    }

    /**
     * Determine all plugin dependencies relevant to the executable. Takes includePlugins, and the executableDependency
     * into consideration.
     * 
     * @return a set of Artifact objects. (Empty set is returned if there are no relevant plugin dependencies.)
     * @throws MojoExecutionException if a problem happens resolving the plufin dependencies
     */
    private Set<Artifact> determineRelevantPluginDependencies()
        throws MojoExecutionException
    {
        Set<Artifact> relevantDependencies;
        if ( this.includePluginDependencies )
        {
            if ( this.executableDependency == null )
            {
                getLog().debug( "All Plugin Dependencies will be included." );
                relevantDependencies = new HashSet<Artifact>( this.pluginDependencies );
            }
            else
            {
                getLog().debug( "Selected plugin Dependencies will be included." );
                Artifact executableArtifact = this.findExecutableArtifact();
                relevantDependencies = this.resolveExecutableDependencies( executableArtifact );
            }
        }
        else
        {
            relevantDependencies = Collections.emptySet();
            getLog().debug( "Plugin Dependencies will be excluded." );
        }
        return relevantDependencies;
    }

    /**
     * Resolve the executable dependencies for the specified project
     * 
     * @param executablePomArtifact the project's POM
     * @return a set of Artifacts
     * @throws MojoExecutionException if a failure happens
     */
    private Set<Artifact> resolveExecutableDependencies( Artifact executablePomArtifact )
        throws MojoExecutionException
    {

        Set<Artifact> executableDependencies = new LinkedHashSet<>();
        try
        {
            ProjectBuildingRequest buildingRequest = getSession().getProjectBuildingRequest();
            
            MavenProject executableProject =
                this.projectBuilder.build( executablePomArtifact, buildingRequest ).getProject();

            for ( ArtifactResult artifactResult : dependencyResolver.resolveDependencies( buildingRequest, executableProject.getModel(), null ) )
            {
                executableDependencies.add( artifactResult.getArtifact() );
            }
        }
        catch ( Exception ex )
        {
            throw new MojoExecutionException( "Encountered problems resolving dependencies of the executable "
                + "in preparation for its execution.", ex );
        }

        return executableDependencies;
    }

    /**
     * Stop program execution for nn millis.
     * 
     * @param millis the number of millis-seconds to wait for, <code>0</code> stops program forever.
     */
    private void waitFor( long millis )
    {
        Object lock = new Object();
        synchronized ( lock )
        {
            try
            {
                lock.wait( millis );
            }
            catch ( InterruptedException e )
            {
                Thread.currentThread().interrupt(); // good practice if don't throw
                getLog().warn( "Spuriously interrupted while waiting for " + millis + "ms", e );
            }
        }
    }

}
