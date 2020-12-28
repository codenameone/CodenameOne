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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * A Plugin for executing external programs.
 *
 * @author Jerome Lacoste (jerome@coffeebreaks.org)
 * @version $Id$
 * @since 1.0
 */
@Mojo( name = "exec", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class ExecMojo
    extends AbstractExecMojo
{
    /**
     * <p>
     * The executable. Can be a full path or the name of the executable. In the latter case, the executable must be in
     * the PATH for the execution to work. Omit when using <code>executableDependency</code>.
     * </p>
     * <p>
     * The plugin will search for the executable in the following order:
     * <ol>
     * <li>relative to the root of the project</li>
     * <li>as toolchain executable</li>
     * <li>relative to the working directory (Windows only)</li>
     * <li>relative to the directories specified in the system property PATH (Windows Only)</li>
     * </ol>
     * Otherwise use the executable as is.
     * </p>
     *
     * @since 1.0
     */
    @Parameter( property = "exec.executable" )
    private String executable;

    /**
     * <p>
     * Timeout in full milliseconds, default is {@code 0}.
     * <p>
     * <p>
     * When set to a value larger than zero, the executable is forcefully
     * terminated if it did not finish within this time, and the build will
     * fail.
     * </p>
     *
     * @since 3.0.0
     */
    @Parameter( property = "exec.timeout", defaultValue = "0" )
    private int timeout;

    /**
     * <p>
     * The toolchain. If omitted, <code>"jdk"</code> is assumed.
     * </p>
     */
    @Parameter( property = "exec.toolchain", defaultValue = "jdk" )
    private String toolchain;

    /**
     * The current working directory. Optional. If not specified, basedir will be used.
     *
     * @since 1.0
     */
    @Parameter( property = "exec.workingdir" )
    private File workingDirectory;

    /**
     * Program standard and error output will be redirected to the file specified by this optional field. If not
     * specified the standard Maven logging is used. <br/>
     * <strong>Note:</strong> Be aware that <code>System.out</code> and <code>System.err</code> use buffering, so don't
     * rely on the order!
     *
     * @since 1.1-beta-2
     * @see java.lang.System#err
     * @see java.lang.System#in
     */
    @Parameter( property = "exec.outputFile" )
    private File outputFile;

    /**
     * When enabled, program standard and error output will be redirected to the
     * Maven logger as <i>Info</i> and <i>Error</i> level logs, respectively. If not enabled the
     * traditional behavior of program output being directed to standard System.out
     * and System.err is used.<br>
     * <br>
     * NOTE: When enabled, to log the program standard out as Maven <i>Debug</i> level instead of
     * <i>Info</i> level use {@code exec.quietLogs=true}. <br>
     * <br>
     * This option can be extremely helpful when combined with multithreaded builds
     * for two reasons:<br>
     * <ul>
     * <li>Program output is suffixed with the owning thread name, making it easier
     * to trace execution of a specific projects build thread.</li>
     * <li>Program output will not get jumbled with other maven log messages.</li>
     * </ul>
     *
     * For Example, if using {@code exec:exec} to run a script to echo a count from
     * 1 to 100 as:
     *
     * <pre>
     * for i in {1..100}
     * do
     *   echo "${project.artifactId} - $i"
     * done
     * </pre>
     *
     * When this script is run multi-threaded on two modules, {@code module1} and
     * {@code module2}, you might get output such as:
     *
     * <pre>
     * [BuilderThread 1] [INFO] --- exec-maven-plugin:1.6.0:exec (test) @ module1 ---
     * [BuilderThread 2] [INFO] --- exec-maven-plugin:1.6.0:exec (test) @ module2 ---
     * ...
     * module2 - 98
     * modu
     * module1 - 97
     * module1 -
     * le2 - 9899
     * ...
     * </pre>
     *
     * With this flag enabled, the output will instead come something similar to:
     *
     * <pre>
     * ...
     * [Exec Stream Pumper] [INFO] [BuilderThread 2] module2 - 98
     * [Exec Stream Pumper] [INFO] [BuilderThread 1] module1 - 97
     * [Exec Stream Pumper] [INFO] [BuilderThread 1] module1 - 98
     * [Exec Stream Pumper] [INFO] [BuilderThread 2] module2 - 99
     * ...
     * </pre>
     *
     * NOTE 1: To show the thread in the Maven log, configure the Maven
     * installations <i>conf/logging/simplelogger.properties</i> option:
     * {@code org.slf4j.simpleLogger.showThreadName=true}<br>
     *
     * NOTE 2: This option is ignored when {@code exec.outputFile} is specified.
     *
     * @since 3.0.0
     * @see java.lang.System#err
     * @see java.lang.System#in
     */
    @Parameter( property = "exec.useMavenLogger", defaultValue = "false" )
    private boolean useMavenLogger;

    /**
     * When combined with {@code exec.useMavenLogger=true}, prints all executed
     * program output at debug level instead of the default info level to the Maven
     * logger.
     *
     * @since 3.0.0
     */
    @Parameter( property = "exec.quietLogs", defaultValue = "false" )
    private boolean quietLogs;

    /**
     * <p>
     * A list of arguments passed to the {@code executable}, which should be of type <code>&lt;argument&gt;</code> or
     * <code>&lt;classpath&gt;</code>. Can be overridden by using the <code>exec.args</code> environment variable.
     * </p>
     *
     * @since 1.0
     */
    @Parameter
    private List<?> arguments; // TODO: Change ? into something more meaningful

    /**
     * @since 1.0
     */
    @Parameter( readonly = true, required = true, defaultValue = "${basedir}" )
    private File basedir;

    /**
     * @since 3.0.0
     */
    @Parameter( readonly = true, required = true, defaultValue = "${project.build.directory}" )
    private File buildDirectory;

    /**
     * <p>Environment variables to pass to the executed program. For example if you want to set the LANG var:
     * <code>&lt;environmentVariables&gt;
     *     &lt;LANG&gt;en_US&lt;/LANG&gt;
     * &lt;/environmentVariables&gt;
     * </code>
     * </p>
     *
     * @since 1.1-beta-2
     */
    @Parameter
    private Map<String, String> environmentVariables = new HashMap<String, String>();

    /**
     * Environment script to be merged with <i>environmentVariables</i> This script is platform specifics, on Unix its
     * must be Bourne shell format. Use this feature if you have a need to create environment variable dynamically such
     * as invoking Visual Studio environment script file
     *
     * @since 1.4.0
     */
    @Parameter
    private File environmentScript = null;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     */
    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession session;

    /**
     * Exit codes to be resolved as successful execution for non-compliant applications (applications not returning 0
     * for success).
     *
     * @since 1.1.1
     */
    @Parameter
    private int[] successCodes;

    /**
     * If set to true the classpath and the main class will be written to a MANIFEST.MF file and wrapped into a jar.
     * Instead of '-classpath/-cp CLASSPATH mainClass' the exec plugin executes '-jar maven-exec.jar'.
     *
     * @since 1.1.2
     */
    @Parameter( property = "exec.longClasspath", defaultValue = "false" )
    private boolean longClasspath;

    /**
     * If set to true the modulepath and the main class will be written as an @arg file
     * Instead of '--module-path/-p MODULEPATH ' the exec plugin executes '@modulepath'.
     *
     * @since 1.1.2
     */
    @Parameter( property = "exec.longModulepath", defaultValue = "true" )
    private boolean longModulepath;

    /**
     * If set to true the child process executes asynchronously and build execution continues in parallel.
     */
    @Parameter( property = "exec.async", defaultValue = "false" )
    private boolean async;

    /**
     * If set to true, the asynchronous child process is destroyed upon JVM shutdown. If set to false, asynchronous
     * child process continues execution after JVM shutdown. Applies only to asynchronous processes; ignored for
     * synchronous processes.
     */
    @Parameter( property = "exec.asyncDestroyOnShutdown", defaultValue = "true" )
    private boolean asyncDestroyOnShutdown = true;

    public static final String CLASSPATH_TOKEN = "%classpath";

    public static final String MODULEPATH_TOKEN = "%modulepath";

    /**
     * priority in the execute method will be to use System properties arguments over the pom specification.
     *
     * @throws MojoExecutionException if a failure happens
     */
    public void executeImpl()
        throws MojoExecutionException
    {
        //exec.args=-agentlib:jdwp=transport=dt_socket,server=n,address=${jpda.address} -classpath %classpath ${packageClassName}
        
        if ( executable == null )
        {
            if (executableDependency == null)
            {
                throw new MojoExecutionException( "The parameter 'executable' is missing or invalid" );
            }

            executable = findExecutableArtifact().getFile().getAbsolutePath();
            getLog().debug( "using executable dependency " + executable);
        }

        if ( isSkip() )
        {
            getLog().info( "skipping execute as per configuration" );
            return;
        }

        if ( basedir == null )
        {
            throw new IllegalStateException( "basedir is null. Should not be possible." );
        }

        try
        {

            handleWorkingDirectory();

            String argsProp = getSystemProperty( "exec.args" );

            List<String> commandArguments = new ArrayList<String>();

            if ( hasCommandlineArgs() )
            {
                handleCommandLineArgs( commandArguments );
            }
            else if ( !StringUtils.isEmpty( argsProp ) )
            {
                handleSystemPropertyArguments( argsProp, commandArguments );
            }
            else
            {
                if ( arguments != null )
                {
                    handleArguments( commandArguments );
                }
            }

            Map<String, String> enviro = handleSystemEnvVariables();

            CommandLine commandLine = getExecutablePath( enviro, workingDirectory );

            String[] args = commandArguments.toArray( new String[commandArguments.size()] );

            commandLine.addArguments( args, false );

            Executor exec = new DefaultExecutor();
            if ( this.timeout > 0 )
            {
                exec.setWatchdog( new ExecuteWatchdog( this.timeout ) );
            }
            exec.setWorkingDirectory( workingDirectory );
            fillSuccessCodes( exec );

            getLog().debug( "Executing command line: " + commandLine );

            try
            {
                int resultCode;
                if ( outputFile != null )
                {
                    if ( !outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs() )
                    {
                        getLog().warn( "Could not create non existing parent directories for log file: " + outputFile );
                    }

                    FileOutputStream outputStream = null;
                    try
                    {
                        outputStream = new FileOutputStream( outputFile );

                        resultCode = executeCommandLine( exec, commandLine, enviro, outputStream );
                    }
                    finally
                    {
                        IOUtil.close( outputStream );
                    }
                }
                else if (useMavenLogger)
                {
                    getLog().debug("Will redirect program output to Maven logger");
                    final String parentThreadName = Thread.currentThread().getName();
                    final String logSuffix = "[" + parentThreadName + "] ";
                    Invokable<String> mavenOutRedirect = new Invokable<String>()
                    {

                        @Override
                        public void accept(String logMessage)
                        {
                            if (quietLogs)
                            {
                                getLog().debug(logSuffix + logMessage);
                            }
                            else
                            {
                                getLog().info(logSuffix + logMessage);
                            }
                        }
                    };
                    Invokable<String> mavenErrRedirect = new Invokable<String>()
                    {

                        @Override
                        public void accept(String logMessage)
                        {
                            getLog().error(logSuffix + logMessage);
                        }
                    };

                    try (OutputStream out = new LineRedirectOutputStream(mavenOutRedirect);
                            OutputStream err = new LineRedirectOutputStream(mavenErrRedirect)) {
                        resultCode = executeCommandLine(exec, commandLine, enviro, out, err);
                    }
                }
                else
                {
                    resultCode = executeCommandLine( exec, commandLine, enviro, System.out, System.err );
                }

                if ( isResultCodeAFailure( resultCode ) )
                {
                    String message = "Result of " + commandLine.toString() + " execution is: '" + resultCode + "'.";
                    getLog().error( message );
                    throw new MojoExecutionException( message );
                }
            }
            catch ( ExecuteException e )
            {
                if ( exec.getWatchdog() != null && exec.getWatchdog().killedProcess() )
                {
                    final String message = "Timeout. Process runs longer than " + this.timeout + " ms.";
                    getLog().error( message );
                    throw new MojoExecutionException( message, e );
                }
                else
                {
                    getLog().error( "Command execution failed.", e );
                    throw new MojoExecutionException( "Command execution failed.", e );
                }
            }
            catch ( IOException e )
            {
                getLog().error( "Command execution failed.", e );
                throw new MojoExecutionException( "Command execution failed.", e );
            }

            registerSourceRoots();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O Error", e );
        }
    }

    private Map<String, String> handleSystemEnvVariables()
        throws MojoExecutionException
    {

        Map<String, String> enviro = new HashMap<String, String>();
        try
        {
            Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();
            for ( Map.Entry<?, ?> entry : systemEnvVars.entrySet() )
            {
                enviro.put( (String) entry.getKey(), (String) entry.getValue() );
            }
        }
        catch ( IOException x )
        {
            getLog().error( "Could not assign default system enviroment variables.", x );
        }

        if ( environmentVariables != null )
        {
            enviro.putAll( environmentVariables );
        }

        if ( this.environmentScript != null )
        {
            getLog().info( "Pick up external environment script: " + this.environmentScript );
            Map<String, String> envVarsFromScript = this.createEnvs( this.environmentScript );
            if ( envVarsFromScript != null )
            {
                enviro.putAll( envVarsFromScript );
            }
        }

        if ( this.getLog().isDebugEnabled() )
        {
            Set<String> keys = new TreeSet<String>();
            keys.addAll( enviro.keySet() );
            for ( String key : keys )
            {
                this.getLog().debug( "env: " + key + "=" + enviro.get( key ) );
            }
        }

        return enviro;
    }

    /**
     * This is a convenient method to make the execute method a little bit more readable. It will define the
     * workingDirectory to be the baseDir in case of workingDirectory is null. If the workingDirectory does not exist it
     * will created.
     *
     * @throws MojoExecutionException
     */
    private void handleWorkingDirectory()
        throws MojoExecutionException
    {
        if ( workingDirectory == null )
        {
            workingDirectory = basedir;
        }

        if ( !workingDirectory.exists() )
        {
            getLog().debug( "Making working directory '" + workingDirectory.getAbsolutePath() + "'." );
            if ( !workingDirectory.mkdirs() )
            {
                throw new MojoExecutionException( "Could not make working directory: '"
                    + workingDirectory.getAbsolutePath() + "'" );
            }
        }
    }

    private void handleSystemPropertyArguments( String argsProp, List<String> commandArguments )
        throws MojoExecutionException
    {
        getLog().debug( "got arguments from system properties: " + argsProp );

        try
        {
            String[] args = CommandLineUtils.translateCommandline( argsProp );
            commandArguments.addAll( Arrays.asList( args ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Couldn't parse systemproperty 'exec.args'" );
        }
    }

    private void handleCommandLineArgs( List<String> commandArguments )
        throws MojoExecutionException, IOException
    {
        String[] args = parseCommandlineArgs();
        for ( int i = 0; i < args.length; i++ )
        {
            if ( isLongClassPathArgument( args[i] ) )
            {
                // it is assumed that starting from -cp or -classpath the arguments
                // are: -classpath/-cp %classpath mainClass
                // the arguments are replaced with: -jar $TMP/maven-exec.jar
                // NOTE: the jar will contain the classpath and the main class
                commandArguments.add( "-jar" );
                File tmpFile = createJar( computePath( null ), args[i + 2] );
                commandArguments.add( tmpFile.getAbsolutePath() );
                i += 2;
            }
            else if ( args[i].contains( CLASSPATH_TOKEN ) )
            {
                commandArguments.add( args[i].replace( CLASSPATH_TOKEN, computeClasspathString( null ) ) );
            }
            else
            {
                commandArguments.add( args[i] );
            }
        }
    }

    private void handleArguments( List<String> commandArguments )
        throws MojoExecutionException, IOException
    {
        String specialArg = null;

        for ( int i = 0; i < arguments.size(); i++ )
        {
            Object argument = arguments.get( i );

            if ( specialArg != null )
            {
                if ( isLongClassPathArgument( specialArg ) && argument instanceof Classpath )
                {
                    // it is assumed that starting from -cp or -classpath the arguments
                    // are: -classpath/-cp %classpath mainClass
                    // the arguments are replaced with: -jar $TMP/maven-exec.jar
                    // NOTE: the jar will contain the classpath and the main class
                    commandArguments.add( "-jar" );

                    File tmpFile = createJar( computePath( (Classpath) argument ),
                                              (String) arguments.get( ++i ) );
                    commandArguments.add( tmpFile.getAbsolutePath() );
                }
                else if ( isLongModulePathArgument( specialArg ) && argument instanceof Modulepath )
                {
                    String filePath = new File( buildDirectory, "modulepath" ).getAbsolutePath();

                    StringBuilder modulePath = new StringBuilder();
                    modulePath.append( '"' );

                    for ( Iterator<String> it = computePath( (Modulepath) argument ).iterator(); it.hasNext(); )
                    {
                        modulePath.append( it.next().replace( "\\", "\\\\" ) );
                        if ( it.hasNext() )
                        {
                            modulePath.append( File.pathSeparatorChar );
                        }
                    }

                    modulePath.append( '"' );

                    createArgFile( filePath, Arrays.asList( "-p", modulePath.toString() ) );
                    commandArguments.add( '@' + filePath );
                }
                else
                {
                    commandArguments.add( specialArg );
                }

                specialArg = null;

                continue;
            }

            if ( argument instanceof Classpath )
            {
                Classpath specifiedClasspath = (Classpath) argument;
                commandArguments.add( computeClasspathString( specifiedClasspath ) );
            }
            else if ( argument instanceof Modulepath )
            {
                Modulepath specifiedModulepath = (Modulepath) argument;
                commandArguments.add( computeClasspathString( specifiedModulepath ) );
            }
            else if ( isLongModulePathArgument( specialArg ) || isLongClassPathArgument( specialArg ) )
            {
                specialArg = (String) argument;
            }
            else if (argument == null)
            {
                commandArguments.add( "" );
            }
            else
            {
                commandArguments.add( (String) argument );
            }
        }
    }

    private void fillSuccessCodes( Executor exec )
    {
        if ( successCodes != null && successCodes.length > 0 )
        {
            exec.setExitValues( successCodes );
        }
    }

    boolean isResultCodeAFailure( int result )
    {
        if ( successCodes == null || successCodes.length == 0 )
        {
            return result != 0;
        }
        for ( int successCode : successCodes )
        {
            if ( successCode == result )
            {
                return false;
            }
        }
        return true;
    }

    private boolean isLongClassPathArgument( String arg )
    {
        return longClasspath && ( "-classpath".equals( arg ) || "-cp".equals( arg ) );
    }

    private boolean isLongModulePathArgument( String arg )
    {
        return longModulepath && ( "--module-path".equals( arg ) || "-p".equals( arg ) );
    }

    /**
     * Compute the classpath from the specified Classpath. The computed classpath is based on the classpathScope. The
     * plugin cannot know from maven the phase it is executed in. So we have to depend on the user to tell us he wants
     * the scope in which the plugin is expected to be executed.
     *
     * @param specifiedClasspath Non null when the user restricted the dependencies, <code>null</code> otherwise (the
     *            default classpath will be used)
     * @return a platform specific String representation of the classpath
     */
    private String computeClasspathString( AbstractPath specifiedClasspath )
    {
        List<String> resultList = computePath( specifiedClasspath );
        StringBuffer theClasspath = new StringBuffer();

        for ( String str : resultList )
        {
            addToClasspath( theClasspath, str );
        }

        return theClasspath.toString();
    }

    /**
     * Compute the classpath from the specified Classpath. The computed classpath is based on the classpathScope. The
     * plugin cannot know from maven the phase it is executed in. So we have to depend on the user to tell us he wants
     * the scope in which the plugin is expected to be executed.
     *
     * @param specifiedClasspath Non null when the user restricted the dependencies, <code>null</code> otherwise (the
     *            default classpath will be used)
     * @return a list of class path elements
     */
    private List<String> computePath( AbstractPath specifiedClasspath )
    {
        List<Artifact> artifacts = new ArrayList<>();
        List<Path> theClasspathFiles = new ArrayList<>();
        List<String> resultList = new ArrayList<>();

        collectProjectArtifactsAndClasspath( artifacts, theClasspathFiles );

        if ( ( specifiedClasspath != null ) && ( specifiedClasspath.getDependencies() != null ) )
        {
            artifacts = filterArtifacts( artifacts, specifiedClasspath.getDependencies() );
        }

        for ( Path f : theClasspathFiles )
        {
            resultList.add( f.toAbsolutePath().toString() );
        }

        for ( Artifact artifact : artifacts )
        {
            getLog().debug( "dealing with " + artifact );
            resultList.add( artifact.getFile().getAbsolutePath() );
        }

        return resultList;
    }

    private static void addToClasspath( StringBuffer theClasspath, String toAdd )
    {
        if ( theClasspath.length() > 0 )
        {
            theClasspath.append( File.pathSeparator );
        }
        theClasspath.append( toAdd );
    }

    private List<Artifact> filterArtifacts( List<Artifact> artifacts, Collection<String> dependencies )
    {
        AndArtifactFilter filter = new AndArtifactFilter();

        filter.add( new IncludesArtifactFilter( new ArrayList<String>( dependencies ) ) ); // gosh

        List<Artifact> filteredArtifacts = new ArrayList<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            if ( filter.include( artifact ) )
            {
                getLog().debug( "filtering in " + artifact );
                filteredArtifacts.add( artifact );
            }
        }
        return filteredArtifacts;
    }

    private ProcessDestroyer processDestroyer;

    CommandLine getExecutablePath( Map<String, String> enviro, File dir )
    {
        File execFile = new File( executable );
        String exec = null;
        if ( execFile.isFile() )
        {
            getLog().debug( "Toolchains are ignored, 'executable' parameter is set to " + executable );
            exec = execFile.getAbsolutePath();
        }

        if ( exec == null )
        {
            Toolchain tc = getToolchain();

            // if the file doesn't exist & toolchain is null, the exec is probably in the PATH...
            // we should probably also test for isFile and canExecute, but the second one is only
            // available in SDK 6.
            if ( tc != null )
            {
                getLog().info( "Toolchain in exec-maven-plugin: " + tc );
                exec = tc.findTool( executable );
            }
            else
            {
                if ( OS.isFamilyWindows() )
                {
                    List<String> paths = this.getExecutablePaths( enviro );
                    paths.add( 0, dir.getAbsolutePath() );

                    exec = findExecutable( executable, paths );
                }
            }
        }

        if ( exec == null )
        {
            exec = executable;
        }

        CommandLine toRet;
        if ( OS.isFamilyWindows() && !hasNativeExtension( exec ) && hasExecutableExtension( exec ) )
        {
            // run the windows batch script in isolation and exit at the end
            final String comSpec = System.getenv( "ComSpec" );
            toRet = new CommandLine( comSpec == null ? "cmd" : comSpec );
            toRet.addArgument( "/c" );
            toRet.addArgument( exec );
        }
        else
        {
            toRet = new CommandLine( exec );
        }

        return toRet;
    }

    static String findExecutable( final String executable, final List<String> paths )
    {
        File f = null;
        search: for ( final String path : paths )
        {
            f = new File( path, executable );
            if ( !OS.isFamilyWindows() && f.isFile() )
                break;
            else
                for ( final String extension : getExecutableExtensions() )
                {
                    f = new File( path, executable + extension );
                    if ( f.isFile() )
                        break search;
                }
        }

        if ( f == null || !f.exists() )
            return null;

        return f.getAbsolutePath();
    }

    private static boolean hasNativeExtension( final String exec )
    {
        final String lowerCase = exec.toLowerCase();
        return lowerCase.endsWith( ".exe" ) || lowerCase.endsWith( ".com" );
    }

    private static boolean hasExecutableExtension( final String exec )
    {
        final String lowerCase = exec.toLowerCase();
        for ( final String ext : getExecutableExtensions() )
            if ( lowerCase.endsWith( ext ) )
                return true;

        return false;
    }

    private static List<String> getExecutableExtensions()
    {
        final String pathExt = System.getenv( "PATHEXT" );
        return pathExt == null ? Arrays.asList( ".bat", ".cmd" )
                        : Arrays.asList( StringUtils.split( pathExt.toLowerCase(), File.pathSeparator ) );
    }

    private List<String> getExecutablePaths( Map<String, String> enviro )
    {
        List<String> paths = new ArrayList<String>();
        paths.add( "" );

        String path = enviro.get( "PATH" );
        if ( path != null )
        {
            paths.addAll( Arrays.asList( StringUtils.split( path, File.pathSeparator ) ) );
        }

        return paths;
    }

    protected int executeCommandLine( Executor exec, CommandLine commandLine, Map<String, String> enviro,
                                      OutputStream out, OutputStream err )
                                          throws ExecuteException, IOException
    {
        // note: don't use BufferedOutputStream here since it delays the outputs MEXEC-138
        PumpStreamHandler psh = new PumpStreamHandler( out, err, System.in );
        return executeCommandLine( exec, commandLine, enviro, psh );
    }

    protected int executeCommandLine( Executor exec, CommandLine commandLine, Map<String, String> enviro,
                                      FileOutputStream outputFile )
                                          throws ExecuteException, IOException
    {
        BufferedOutputStream bos = new BufferedOutputStream( outputFile );
        PumpStreamHandler psh = new PumpStreamHandler( bos );
        return executeCommandLine( exec, commandLine, enviro, psh );
    }

    protected int executeCommandLine( Executor exec, final CommandLine commandLine, Map<String, String> enviro,
                                      final PumpStreamHandler psh )
                                          throws ExecuteException, IOException
    {
        exec.setStreamHandler( psh );

        int result;
        try
        {
            psh.start();
            if ( async )
            {
                if ( asyncDestroyOnShutdown )
                {
                    exec.setProcessDestroyer( getProcessDestroyer() );
                }

                exec.execute( commandLine, enviro, new ExecuteResultHandler()
                {
                    public void onProcessFailed( ExecuteException e )
                    {
                        getLog().error( "Async process failed for: " + commandLine, e );
                    }

                    public void onProcessComplete( int exitValue )
                    {
                        getLog().info( "Async process complete, exit value = " + exitValue + " for: " + commandLine );
                        try
                        {
                            psh.stop();
                        }
                        catch ( IOException e )
                        {
                            getLog().error( "Error stopping async process stream handler for: " + commandLine, e );
                        }
                    }
                } );
                result = 0;
            }
            else
            {
                result = exec.execute( commandLine, enviro );
            }
        }
        finally
        {
            if ( !async )
            {
                psh.stop();
            }
        }
        return result;
    }

    //
    // methods used for tests purposes - allow mocking and simulate automatic setters
    //

    void setExecutable( String executable )
    {
        this.executable = executable;
    }

    String getExecutable()
    {
        return executable;
    }

    void setWorkingDirectory( String workingDir )
    {
        setWorkingDirectory( new File( workingDir ) );
    }

    void setWorkingDirectory( File workingDir )
    {
        this.workingDirectory = workingDir;
    }

    void setArguments( List<?> arguments )
    {
        this.arguments = arguments;
    }

    void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    void setProject( MavenProject project )
    {
        this.project = project;
    }

    protected String getSystemProperty( String key )
    {
        return System.getProperty( key );
    }

    public void setSuccessCodes( Integer... list )
    {
        this.successCodes = new int[list.length];
        for ( int index = 0; index < list.length; index++ )
        {
            successCodes[index] = list[index];
        }
    }

    public int[] getSuccessCodes()
    {
        return successCodes;
    }

    private Toolchain getToolchain()
    {
        Toolchain tc = null;

        try
        {
            if ( session != null ) // session is null in tests..
            {
                ToolchainManager toolchainManager =
                    (ToolchainManager) session.getContainer().lookup( ToolchainManager.ROLE );

                if ( toolchainManager != null )
                {
                    tc = toolchainManager.getToolchainFromBuildContext( toolchain, session );
                }
            }
        }
        catch ( ComponentLookupException componentLookupException )
        {
            // just ignore, could happen in pre-2.0.9 builds..
        }
        return tc;
    }

    /**
     * Create a jar with just a manifest containing a Main-Class entry for SurefireBooter and a Class-Path entry for all
     * classpath elements. Copied from surefire (ForkConfiguration#createJar())
     *
     * @param classPath List&lt;String> of all classpath elements.
     * @return
     * @throws IOException
     */
    private File createJar( List<String> classPath, String mainClass )
        throws IOException
    {
        File file = File.createTempFile( "maven-exec", ".jar" );
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream( file );
        JarOutputStream jos = new JarOutputStream( fos );
        jos.setLevel( JarOutputStream.STORED );
        JarEntry je = new JarEntry( "META-INF/MANIFEST.MF" );
        jos.putNextEntry( je );

        Manifest man = new Manifest();

        // we can't use StringUtils.join here since we need to add a '/' to
        // the end of directory entries - otherwise the jvm will ignore them.
        StringBuilder cp = new StringBuilder();
        for ( String el : classPath )
        {
            // NOTE: if File points to a directory, this entry MUST end in '/'.
            cp.append( new URL( new File( el ).toURI().toASCIIString() ).toExternalForm() + " " );
        }

        man.getMainAttributes().putValue( "Manifest-Version", "1.0" );
        man.getMainAttributes().putValue( "Class-Path", cp.toString().trim() );
        man.getMainAttributes().putValue( "Main-Class", mainClass );

        man.write( jos );
        jos.close();

        return file;
    }

    private void createArgFile( String filePath, List<String> lines )
        throws IOException
    {
        final String EOL = System.getProperty( "line.separator", "\\n" );

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( filePath );
            for ( String line : lines )
            {
                writer.append( line ).append( EOL );
            }

        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    protected Map<String, String> createEnvs( File envScriptFile )
        throws MojoExecutionException
    {
        Map<String, String> results = null;

        File tmpEnvExecFile = null;
        try
        {
            tmpEnvExecFile = this.createEnvWrapperFile( envScriptFile );

            Commandline cl = new Commandline();// commons-exec instead?
            cl.setExecutable( tmpEnvExecFile.getAbsolutePath() );
            if ( !OS.isFamilyWindows() )
            {
                cl.setExecutable( "sh" );
                cl.createArg().setFile( tmpEnvExecFile );
            }

            // pickup the initial env vars so that the env script can used if necessary
            if ( environmentVariables != null )
            {
                for ( Map.Entry<String, String> item : environmentVariables.entrySet() )
                {
                    cl.addEnvironment( item.getKey(), item.getValue() );
                }
            }

            EnvStreamConsumer stdout = new EnvStreamConsumer();
            StreamConsumer stderr = new DefaultConsumer();

            CommandLineUtils.executeCommandLine( cl, stdout, stderr );

            results = stdout.getParsedEnv();
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        finally
        {
            if ( tmpEnvExecFile != null )
            {
                tmpEnvExecFile.delete();
            }
        }

        return results;

    }

    protected File createEnvWrapperFile( File envScript )
        throws IOException
    {
        PrintWriter writer = null;
        File tmpFile = null;
        try
        {

            if ( OS.isFamilyWindows() )
            {
                tmpFile = File.createTempFile( "env", ".bat" );
                writer = new PrintWriter( tmpFile );
                writer.append( "@echo off" ).println();
                writer.append( "call \"" ).append( envScript.getCanonicalPath() ).append( "\"" ).println();
                writer.append( "echo " + EnvStreamConsumer.START_PARSING_INDICATOR ).println();
                writer.append( "set" ).println();
                writer.flush();
            }
            else
            {
                tmpFile = File.createTempFile( "env", ".sh" );
                // tmpFile.setExecutable( true );//java 6 only
                writer = new PrintWriter( tmpFile );
                writer.append( "#! /bin/sh" ).println();
                writer.append( ". " ).append( envScript.getCanonicalPath() ).println(); // works on all unix??
                writer.append( "echo " + EnvStreamConsumer.START_PARSING_INDICATOR ).println();
                writer.append( "env" ).println();
                writer.flush();
            }
        }
        finally
        {
            IOUtil.close( writer );
        }

        return tmpFile;

    }

    protected ProcessDestroyer getProcessDestroyer()
    {
        if ( processDestroyer == null )
        {
            processDestroyer = new ShutdownHookProcessDestroyer();
        }
        return processDestroyer;
    }
}
