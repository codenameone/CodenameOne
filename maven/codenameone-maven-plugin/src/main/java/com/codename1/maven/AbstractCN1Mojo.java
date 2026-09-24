/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import com.codename1.ant.SortedProperties;

import static com.codename1.maven.PathUtil.path;
import static com.codename1.maven.ProjectUtil.wrap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.doxia.logging.Log;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author shannah
 */
public abstract class AbstractCN1Mojo extends AbstractMojo {
    
    protected static final String GROUP_ID="com.codenameone";
    protected static final String JAVA_RUNTIME_ARTIFACT_ID = "java-runtime";
    protected static final String ARTIFACT_ID="codenameone-maven-plugin";

    /**
     * Main class of the headless CSS compiler CLI, resolved via
     * {@link #getCssCliClasspath()}. Kept here so the mojo and the simulator's
     * CSSWatcher agree on one spelling.
     */
    protected static final String CSS_CLI_MAIN_CLASS = "com.codename1.designer.css.CN1CSSCLI";

    /**
     * simulator.properties key carrying {@link #getCssCliClasspath()} through to the
     * simulator, where CSSWatcher forks the CSS compiler for live reload.
     */
    protected static final String CSS_CLI_CLASSPATH_PROPERTY = "cn1.css.cli.classpath";


    @Component
    protected MavenProjectHelper projectHelper;

    @Parameter( defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "project.build.directory", readonly = true)
    protected String outputDirectory;

    
    @Parameter(property = "cn1lib.stubber.excludes", defaultValue="")
    protected String stubberExcludes;
    
    @Parameter(property = "cn1lib.stubber.includes", defaultValue="**")
    protected String stubberIncludes;
    
    @Parameter(property = "project.build.finalName", readonly = true)
    protected String finalName;
    
    protected Project antProject;
    
    @Parameter(property = "plugin.artifacts", required = true, readonly = true)
    protected List<Artifact> pluginArtifacts;
    
    @Component 
    protected RepositorySystem repositorySystem;
    
    @Parameter(required = true, readonly = true, defaultValue = "${localRepository}")
    protected MavenArtifactRepository localRepository;
    
    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    protected List<MavenArtifactRepository> remoteRepositories;

    /**
     * The legacy resolver used by the goals below does not read the session's offline flag by
     * itself, so {@code mvn -o} still let it reach the network and, worse, install what it found
     * over an artifact the same build had just produced locally. Every resolution request in this
     * class passes this through.
     */
    @Parameter(required = true, readonly = true, defaultValue = "${settings.offline}")
    protected boolean offline;

    /**
     * Version of the deprecated Resource Editor that {@code cn1:designer} resolves.
     * It is frozen rather than tracking the framework version, so it does not follow
     * ${cn1.version}. Override with -Dcn1.designer.version to run a different build.
     */
    @Parameter(property = "cn1.designer.version", defaultValue = "7.0.263")
    protected String designerVersion;
    
    protected Properties properties;

    protected long getSourcesModificationTime() throws IOException {
        return getSourcesModificationTime(false);
    }

    protected long getCSSSourcesModificationTime() throws IOException {
        long mTime = 0;
        File root = getCN1ProjectDir().getCanonicalFile().getParentFile();
        File commonSources = new File(root, path("common", "src", "main", "css"));
        if (commonSources.exists()) {
            mTime = Math.max(mTime, lastModifiedRecursive(commonSources, ALL_FILES_FILTER));
        }


        File codenameOneSettings = new File(root, "common" + File.separator + "codenameone_settings.properties");
        if (codenameOneSettings.exists()) {
            mTime = Math.max(mTime, codenameOneSettings.lastModified());
        }

        File pomFile = new File(root, "common" + File.separator + "pom.xml");
        if (pomFile.exists()) {
            mTime = Math.max(mTime, pomFile.lastModified());
        }

        return mTime;
    }

    protected long getSourcesModificationTime(boolean commonOnly) throws IOException {
        long mTime = 0;
        File root = getCN1ProjectDir().getCanonicalFile().getParentFile();
        File commonSources = new File(root, "common" + File.separator + "src");
        if (commonSources.exists()) {
            mTime = Math.max(mTime, lastModifiedRecursive(commonSources, ALL_FILES_FILTER));
        }
        if (!commonOnly) {
            String platform = project.getProperties().getProperty("codename1.platform");
            if (platform != null) {
                File platformSourcesDir = new File(root, platform + File.separator + "src");
                if (platformSourcesDir.exists()) {
                    mTime = Math.max(mTime, lastModifiedRecursive(platformSourcesDir, ALL_FILES_FILTER));
                }
            }
        }

        File codenameOneSettings = new File(root, "common" + File.separator + "codenameone_settings.properties");
        if (codenameOneSettings.exists()) {
            mTime = Math.max(mTime, codenameOneSettings.lastModified());
        }

        File pomFile = new File(root, "common" + File.separator + "pom.xml");
        if (pomFile.exists()) {
            mTime = Math.max(mTime, pomFile.lastModified());
        }

        if (!commonOnly) {
            String platform = project.getProperties().getProperty("codename1.platform");
            pomFile = new File(root, platform + File.separator + "pom.xml");
            if (pomFile.exists()) {
                mTime = Math.max(mTime, pomFile.lastModified());
            }
        }
        return mTime;
    }


    private void setupAnt()  throws MojoExecutionException, MojoFailureException {
        
        antProject = new Project();
        if (project.getBasedir() != null) {
            antProject.setBaseDir(project.getBasedir());
        } else {
            antProject.setBaseDir(new File("."));
        }
        antProject.setDefaultInputStream(System.in);
        
        InputHandler handler = new DefaultInputHandler();
        antProject.setProjectReference(handler);
        antProject.setInputHandler(handler);
        
        
        antProject.init();
    }

    /**
     * The properties Maven was invoked with, so a build hint given on the
     * command line reaches the build.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /// The modules this build is running over, for resolving which one produced
    /// a classpath element.
    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    /**
     * The reactor module whose compiled output is `element`, or null.
     *
     * <p>In the generated layout the application's classes come from `common`
     * while a platform module is what runs: asking the RUNNING project where its
     * sources are answers for the wrong module, and a class compiled from
     * `common` then has no backing source and reads as stale.</p>
     */
    protected MavenProject moduleProducing(File element) {
        if (reactorProjects == null || element == null) {
            return null;
        }
        String wanted = canonicalPath(element);
        for (MavenProject candidate : reactorProjects) {
            if (candidate.getBuild() != null
                    && candidate.getBuild().getOutputDirectory() != null
                    && wanted.equals(
                        canonicalPath(new File(candidate.getBuild().getOutputDirectory())))) {
                return candidate;
            }
            // A reactor `package` build hands over the dependency module's JAR
            // rather than its output directory, which is the ordinary shape for
            // the generated layout -- matching directories alone sent every
            // class in that jar back to the running module's source roots, where
            // it has none, so a live misplaced annotation read as stale.
            if (candidate.getArtifact() != null && candidate.getArtifact().getFile() != null
                    && wanted.equals(canonicalPath(candidate.getArtifact().getFile()))) {
                return candidate;
            }
            if (wanted.equals(canonicalPath(packagedFileOf(candidate)))) {
                return candidate;
            }
        }
        return null;
    }

    /// Where `candidate` writes its jar, or null when it does not say.
    ///
    /// The artifact's own file is the better answer and is checked first, but it
    /// is only set once the module has been packaged in this session.
    private static File packagedFileOf(MavenProject candidate) {
        if (candidate.getBuild() == null || candidate.getBuild().getDirectory() == null
                || candidate.getBuild().getFinalName() == null) {
            return null;
        }
        return new File(candidate.getBuild().getDirectory(),
                candidate.getBuild().getFinalName() + ".jar");
    }

    private static String canonicalPath(File f) {
        if (f == null) {
            return null;
        }
        try {
            return f.getCanonicalPath();
        } catch (IOException ex) {
            return f.getAbsolutePath();
        }
    }

    /**
     * The `-D` properties this build was invoked with, or null when there is no
     * session -- which is what the tests run without.
     */
    protected Properties userProperties() {
        return session == null ? null : session.getUserProperties();
    }

    /**
     * What the nested build is invoked with.
     *
     * <p>cn1:run is a nested Maven build, and nothing of the outer command line
     * reached it. So {@code mvn cn1:run -Dcodename1.arg.desktop.titleBar=NATIVE}
     * -- or {@code -Dcodename1.mainName} -- was accepted, printed, and then
     * dropped: the inner build overlaid nothing, process-annotations stamped the
     * manifest for the file's entry point, and the simulator ran on values the
     * same command line would have changed for a device build.</p>
     *
     * <p>Everything in the {@code codename1} namespace, which is the rule
     * {@code overlayCommandLineBuildHints} applies -- except the platform, which
     * is set last because this goal IS the javase simulator and a stray
     * {@code -Dcodename1.platform} must not send the nested build elsewhere.</p>
     */
    protected static Properties nestedBuildProperties(Properties userProperties) {
        Properties props = new Properties();
        if (userProperties != null) {
            for (String key : userProperties.stringPropertyNames()) {
                if (key.startsWith("codename1.")) {
                    props.setProperty(key, userProperties.getProperty(key));
                }
            }
        }
        props.setProperty("codename1.platform", "javase");
        return props;
    }

    /**
     * The {@code codename1.arg.*} entries of {@code userProperties}, which is
     * what {@code -D} actually passed.
     *
     * <p>Only those, never every hint in the settings file. The simulator reads
     * a system property before the file, so publishing the file's own hints that
     * way would outrank the file itself and hide the both-declared conflict the
     * simulator is supposed to report.</p>
     */
    protected static Properties commandLineBuildHints(Properties userProperties) {
        Properties out = new Properties();
        if (userProperties == null) {
            return out;
        }
        for (String key : userProperties.stringPropertyNames()) {
            if (key.startsWith("codename1.arg.")) {
                out.setProperty(key, userProperties.getProperty(key));
            }
        }
        return out;
    }

    /** The session this mojo is running in, for a subclass that has to reproduce it. */
    protected MavenSession getSession() {
        return session;
    }

    /**
     * Lets {@code -Dcodename1.arg.x=y} override the settings file.
     *
     * Build hints were read only out of {@code codenameone_settings.properties},
     * so a hint passed on the command line was accepted by Maven, printed in
     * the build's own property dump, and then silently ignored — the builder
     * asked {@code request.getArg(...)} and got the file's value or the
     * default. That is what made {@code -Dcodename1.arg.ios.onDeviceDebug=true}
     * produce a build with no symbol table and no debug listener, and it is why
     * {@code cn1:buildIosOnDeviceDebug} could not turn the hint on for one
     * build the way it says it does.
     *
     * Only user properties are overlaid — the values actually passed with -D or
     * set by an invoking build — not the whole system property table, so
     * unrelated JVM properties cannot become build hints.
     */
    protected void overlayCommandLineBuildHints(Properties target) {
        if (session == null || target == null) {
            return;
        }
        Properties userProperties = session.getUserProperties();
        if (userProperties == null) {
            return;
        }
        for (String key : userProperties.stringPropertyNames()) {
            if (key.startsWith("codename1.")) {
                target.setProperty(key, userProperties.getProperty(key));
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getCN1ProjectDir() != null) {
            properties = new Properties();
            File cn1Properties = new File(getCN1ProjectDir(), "codenameone_settings.properties");
            if (cn1Properties.exists()) {
                try {
                    properties.load(new FileInputStream(new File(getCN1ProjectDir(), "codenameone_settings.properties")));
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to find codenameone_settings.properties file.", ex);
                }
            }
            overlayCommandLineBuildHints(properties);
        } else {
            getLog().warn("Failed to find CN1 Project directory.  codenameone_settings.properties will not be loaded");
            if (project.getCompileSourceRoots() != null && !project.getCompileSourceRoots().isEmpty()) {
                getLog().warn("Checking from project root and source compile root: " + project.getCompileSourceRoots().get(0));
            }
        }
        
        
        setupAnt();
        try {
            executeImpl();
        } catch (MojoExecutionException | MojoFailureException e) {
            offerHelp(e);
            throw e;
        } catch (RuntimeException e) {
            offerHelp(e);
            throw e;
        }
    }

    /**
     * On any local failure, record the context and print the "Get help" affordance
     * (see {@link com.codename1.maven.help.ToolingHelp}). This never sends anything and
     * never masks the original failure &mdash; it just tells the user how to reach support.
     */
    private void offerHelp(Throwable failure) {
        com.codename1.maven.help.ToolingHelp.offerAfterFailure(
                getLog(),
                com.codename1.maven.help.ToolingHelp.COMPONENT_MAVEN_PLUGIN,
                helpStep(),
                helpAction(),
                com.codename1.maven.help.ToolingHelp.pluginVersion(),
                failure);
    }

    /**
     * The wire-contract {@code step} this goal maps to (install | create_project |
     * configure | local_run | build_submit | other). Subclasses override to classify
     * their failures; defaults to {@code other}.
     */
    protected String helpStep() {
        return "other";
    }

    /**
     * The exact command/action that failed, folded into the reproduction so support can
     * re-run it (e.g. {@code mvn cn1:build -Dcodename1.platform=ios}). Null when unknown.
     */
    protected String helpAction() {
        return null;
    }

    protected abstract void executeImpl()  throws MojoExecutionException, MojoFailureException;


    protected static boolean contains(String needle, String... haystack) {
        for (String s : haystack) {
            if (s.equals(needle)) {
                return true;
            }
        }
        return false;
    }
    
    protected File getCN1ProjectDir() {
        if (project == null || project.getBasedir() == null) {
            return null;
        }
        if (contains(project.getBasedir().getName(), "javase", "javascript", "android", "ios", "win", "linux", "mac")) {
            File commonSettings = new File(project.getBasedir(), ".." + File.separator + "common" + File.separator + "codenameone_settings.properties");
            if (commonSettings.exists()) {
                return commonSettings.getParentFile();
            }
            commonSettings = new File(project.getBasedir(), ".." + File.separator + "common" + File.separator + "codenameone_library_appended.properties");
            if (commonSettings.exists()) {
                return commonSettings.getParentFile();
            }
            
        }
        File commonSubdir = new File(project.getBasedir(), "common");
        if (!new File("codenameone_settings.properties").exists() && commonSubdir.exists()) {
            if (new File(commonSubdir, "codenameone_settings.properties").exists()) {
                return commonSubdir;
            }
        }
        
        File f = getCN1ProjectDir(project.getBasedir());
        if (f != null) return f;
        f = getCN1ProjectDir(new File(project.getCompileSourceRoots().get(0)).getParentFile());
        return f;
        
    }
    
    private File getCN1ProjectDir(File start) {
        File f = new File(start, "codenameone_settings.properties");
        
        while (!f.exists() && f.getParentFile() != null && f.getParentFile().getParentFile() != null) {
            f = new File(f.getParentFile().getParentFile(), "codenameone_settings.properties");
            if (f.exists()) {
                return f.getParentFile();
            }
            f = new File(f.getParentFile().getParentFile(), "codenameone_library_appended.properties");
            if (f.exists()) {
                return f.getParentFile();
            }
            
            
        }
        return f.exists() ? f.getParentFile() : null;
        
    }
    
    public Java createJava() {
        return createJava(Log.LEVEL_DEBUG);
    }
    
    public Java createJava(final int logLevel) {
        
        Java java = new Java() {
            {
               redirector = new Redirector(this) {
                   @Override
                   protected void handleOutput(String output) {
                       switch (logLevel) {
                            case Log.LEVEL_DEBUG:
                                getLog().debug(output);
                                break;
                            case Log.LEVEL_DISABLED:
                                break;
                            case Log.LEVEL_ERROR:
                                getLog().error(output);
                                break;
                            case Log.LEVEL_WARN:
                                getLog().warn(output);
                                break;
                            case Log.LEVEL_INFO:
                            default:
                                getLog().info(output);
                                break;
                        }
                   }

                   @Override
                   protected void handleErrorOutput(String output) {
                       getLog().error(output);
                   }
                    
                   
               };
               
            }
            @Override
            protected void handleOutput(String output) {
                switch (logLevel) {
                    case Log.LEVEL_DEBUG:
                        getLog().debug(output);
                        break;
                    case Log.LEVEL_DISABLED:
                        break;
                    case Log.LEVEL_ERROR:
                        getLog().error(output);
                        break;
                    case Log.LEVEL_WARN:
                        getLog().warn(output);
                        break;
                    case Log.LEVEL_INFO:
                    default:
                        getLog().info(output);
                        break;
                }

            }

            @Override
            protected void handleErrorOutput(String output) {
                getLog().error(output);
            }

            @Override
            protected void handleFlush(String output) {
                switch (logLevel) {
                    case Log.LEVEL_DEBUG:
                        getLog().debug(output);
                        break;
                    case Log.LEVEL_DISABLED:
                        break;
                    case Log.LEVEL_ERROR:
                        getLog().error(output);
                        break;
                    case Log.LEVEL_WARN:
                        getLog().warn(output);
                        break;
                    case Log.LEVEL_INFO:
                    default:
                        getLog().info(output);
                        break;
                }
            }

            @Override
            public void log(String msg) {
                getLog().info(msg);
            }

            @Override
            public void log(String msg, int msgLevel) {
                getLog().info(msg);
                
            }
            
            
            
            
            
        };
        java.setProject(antProject);
        return java;
    }
    
    protected File getJar(String groupId, String artifactId) {
        Artifact art = getArtifact(groupId, artifactId);
        if (art == null) return null;
        return getJar(art);
    }
    
   
    
    protected Artifact getArtifact(String groupId, String artifactId) {
        Artifact out = project.getArtifacts().stream().filter(art->art.getArtifactId().equals(artifactId) && art.getGroupId().equals(groupId)).findFirst().orElse(null);
        if (out != null) return out;
        out = pluginArtifacts.stream().filter(
                art->art.getArtifactId().equals(artifactId) && 
                        art.getGroupId().equals(groupId)).findFirst().orElse(null);
        return out;
    }
    
    protected File getJar(String groupId, String artifactId, String classifier) {
        Artifact art = getArtifact(groupId, artifactId, classifier);
        if (art == null) return null;
        return getJar(art);
    }
    
    protected Artifact getArtifact(String groupId, String artifactId, String classifier) {
        Artifact out =  project.getArtifacts().stream().filter(
                art->art.getArtifactId().equals(artifactId) && 
                        art.getGroupId().equals(groupId) &&
                        Objects.equals(art.getClassifier(), classifier)).findFirst().orElse(null);
        if (out != null) return out;
        out = pluginArtifacts.stream().filter(
                art->art.getArtifactId().equals(artifactId) && 
                        art.getGroupId().equals(groupId) &&
                        Objects.equals(art.getClassifier(), classifier)).findFirst().orElse(null);
        return out;
    }
    
    protected File getJar(Artifact artifact) {
        File[] out = new File[1];
        out[0] = artifact.getFile();

        if (out[0] != null && !"pom.xml".equals(out[0].getName()) && !out[0].getName().endsWith(".pom")) {
            return out[0];
        }
        
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                
        .setOffline(offline)
        .setLocalRepository(localRepository)
        .setRemoteRepositories(new ArrayList<>(remoteRepositories))
        .setResolveTransitively(true)
        .setArtifact(artifact));

        if (result.isSuccess()) {
            out[0] = artifact.getFile().getAbsoluteFile();
        }
        if (out[0] == null || "pom.xml".equals(out[0].getName()) || out[0].getName().endsWith(".pom")){
            return null;
        }
        return out[0];
    }


    
    private File cn1libProjectDir;
   
    protected File getCN1LibProjectDir() {
        if (cn1libProjectDir == null) {
            cn1libProjectDir = wrap(project).getCN1LibProjectDir();
        }
        return cn1libProjectDir;
        
    }
    
    
    protected static long lastModifiedRecursive(File file) {
        return lastModifiedRecursive(file, ALL_FILES_FILTER);
    }

    protected static long lastModifiedRecursive(File file, FilenameFilter filter) {
        long lastModified = 0L;
        if (file.isDirectory()) {
            
            for (File child : file.listFiles()) {
                lastModified = Math.max(lastModifiedRecursive(child, filter), lastModified);
            }
        }
        if (filter.accept(file.getParentFile(), file.getName())) {
            lastModified = Math.max(file.lastModified(), lastModified);
        }
        return lastModified;
    }
    
    protected static final FilenameFilter ALL_FILES_FILTER = (dir, name) -> {
        return true;
    };
    
    protected static final FilenameFilter NO_FILES_FILTER = (dir, name) -> {
        return false;
    };
    
    protected Dependency createSystemScopeDependency(String artifactId, String groupId, String version, File location) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId+"-jar");
        dependency.setVersion(version);
        dependency.setScope(Artifact.SCOPE_SYSTEM);
        
        dependency.setSystemPath(location.getAbsolutePath());
        dependency.setType("jar");
        dependency.setClassifier("jar");
        
        return dependency;
    }
    
    
    
    /**
     * Project's css directory.
     * @return 
     */
    protected File getProjectCSSDir() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File cssSibling = new File(dirFile.getParentFile(), "css");
            File themeCss = new File(cssSibling, "theme.css");
            if (themeCss.exists()) {
                return cssSibling;
            }
            
        }
        return new File(project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "css");
    }
    
    /**
     * The codenameone_maven properties file, which keeps track of dependencies so that we know when to update the project.
     * @return 
     */
    protected File getMavenPropertiesFile() {
        return new File(getCN1ProjectDir(), "codenameone_maven.properties");
    }
    

    /**
     * Delete directory
     * @param file 
     */
    protected static void delTree(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delTree(child);
            }
        }
        file.delete();
    }

    
     /**
     * The maven properties that keep track of dependencies to that we know when to update the project.
     * @return
     * @throws IOException 
     */
    protected Properties getMavenProperties() throws IOException {
        if (mavenProperties == null) {
            File mavenPropertiesFile = getMavenPropertiesFile();
            mavenProperties = new Properties();
            if (mavenPropertiesFile.exists()) {
                try (FileInputStream fis = new FileInputStream(mavenPropertiesFile)) {
                    mavenProperties.load(fis);
                }
            }
        }
        return mavenProperties;
        
    }
    
    /**
     * Persist maven properties to disk.
     * @throws IOException 
     */
    protected void saveMavenProperties() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getMavenPropertiesFile())) {
            getMavenProperties().store(fos, "Updated dependencies");
        }
    }
    
    /**
     * @see #getMavenProperties() 
     * @see #getMavenPropertiesFile() 
     */
    private Properties mavenProperties;
    
    protected void saveProjectProperties() throws IOException {
        if (projectProperties != null) {
            try (FileOutputStream fos = new FileOutputStream(getProjectPropertiesFile())) {
                projectProperties.store(fos, "saved project properties from installCn1libsMojo");
            }
        }
    }
    
    private SortedProperties projectProperties;
    
    
    /**
     * The project's codenameone_settings.properties file
     * @return 
     */
    protected File getProjectPropertiesFile() {
        return new File(getCN1ProjectDir(), "codenameone_settings.properties");
    }
    
    /**
     * The project's codenameone_settings.properties
     * 
     * @return
     * @throws IOException 
     */
    protected SortedProperties getProjectProperties() throws IOException {
        if (projectProperties == null) {
            projectProperties = new SortedProperties();
            File propertiesFile = getProjectPropertiesFile();
            if (propertiesFile.exists()) {
                try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                    projectProperties.load(fis);
                }
            }
            
        }
        return projectProperties;
    }
    
    protected long getLastModified(Artifact artifact) {
        File f = findArtifactFile(artifact);
        if (f != null) {
            return f.lastModified();
        }
        return 0;
    }
    
    /**
     * Gets directory inside local repository that cn1lib artifact is extracted into.
     * @param artifact
     * @return 
     */
    protected File getLibDirFor(Artifact artifact) {
        File artifactFile = findArtifactFile(artifact);
        File artifactDir = new File(artifactFile.getParentFile(), artifactFile.getName()+"-extracted");
        return artifactDir;
    }
    
    
    /**
     * Gets file in local repository associated with artifact.
     * @param artifact
     * @return 
     */
    /**
     * Forwards the effective local repository to a forked Maven invocation.
     * Goals like cn1:run re-invoke Maven through the Invoker API; without
     * this, a build executed with -Dmaven.repo.local=... forks a child that
     * silently resolves from the settings-default repository and runs stale
     * artifacts.
     */
    protected void forwardLocalRepository(java.util.Properties props) {
        if (localRepository != null && localRepository.getBasedir() != null) {
            props.setProperty("maven.repo.local", localRepository.getBasedir());
        }
    }

    protected File findArtifactFile(Artifact artifact) {
        File[] out = new File[1];
        
        
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                
        .setOffline(offline)
        .setLocalRepository(localRepository)
        .setRemoteRepositories(new ArrayList<>(remoteRepositories))
        .setResolveTransitively(true)
        .setArtifact(artifact));

        if (result.isSuccess()) {
            out[0] = artifact.getFile().getAbsoluteFile();
        }
        
        return out[0];
    }
    
    protected List<File> getLibsNativeJarsForPlatform(String platform) {
        getLog().debug("Getting nativese source jars");
        List<File> out = new ArrayList<File>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            File artifactFile = artifact.getFile();
            getLog().debug("Checking "+artifactFile);
            if (!Cn1libUtil.isCN1Lib(artifactFile)) {
                getLog().debug("Not a cn1lib");
                continue;
            }
            File nativeSejar = Cn1libUtil.getNativeJar(artifact, platform);
            if (nativeSejar != null) {
                out.add(nativeSejar);
            }
            //out.addAll(Cn1libUtil.getNativeSEEmbeddedJars(artifact));
        }
        return out;
    }
    
    protected List<File> getLibsNativeSESourceJars() {
        getLog().debug("Getting nativese source jars");
        List<File> out = new ArrayList<File>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            File artifactFile = artifact.getFile();
            getLog().debug("Checking "+artifactFile);
            if (!Cn1libUtil.isCN1Lib(artifactFile)) {
                getLog().debug("Not a cn1lib");
                continue;
            }
            File nativeSejar = Cn1libUtil.getNativeSEJar(artifact);
            if (nativeSejar != null) {
                out.add(nativeSejar);
            }
            //out.addAll(Cn1libUtil.getNativeSEEmbeddedJars(artifact));
        }
        return out;
    }
    
    protected List<File> getLibsNativeSEDependencyJars() {
        List<File> out = new ArrayList<File>();
        for (Artifact artifact : project.getDependencyArtifacts()) {
            File artifactFile = artifact.getFile();
            if (!Cn1libUtil.isCN1Lib(artifactFile)) {
                continue;
            }
            //File nativeSejar = Cn1libUtil.getNativeSEJar(artifact);
            //if (nativeSejar != null) {
            //    out.add(nativeSejar);
            //}
            out.addAll(Cn1libUtil.getNativeSEEmbeddedJars(artifact));
        }
        return out;
    }
    
    protected File getProjectNativeSEDir() {
        return new File(getProjectNativeDir(), "javase");
    }
    
    
    protected File getProjectInternalTmpJar() {
        return new File(new File(project.getBuild().getOutputDirectory()).getParentFile(), "javase-classes.jar");
    }
    
    protected File getProjectInternalTmpDir() {
        return new File(new File(project.getBuild().getOutputDirectory()).getParentFile(), "javase-classes");
    }
    
    protected File getProjectNativeDir() {
        return new File(getCN1ProjectDir(), "native");
    }
    
    public static final String UPDATE_CODENAMEONE_JAR_URL = "https://www.codenameone.com/files/updates/UpdateCodenameOne.jar";
    public static final String UPDATE_CODENAMEONE_JAR_FALLBACK_URL = "https://github.com/codenameone/CodenameOne/raw/refs/heads/master/maven/UpdateCodenameOne.jar";
    public static final String UPDATE_CODENAMEONE_JAR_RESOURCE = "/com/codename1/maven/UpdateCodenameOne.jar";
    public static final String JPDATE_CODENAMEONE_JAR_PATH = System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "UpdateCodenameOne.jar";


    protected void installUpdater() throws IOException {
        File re = new File(JPDATE_CODENAMEONE_JAR_PATH);
        if (re.exists()) {
            getLog().debug("Designer is up to date");
            return;
        }
        re.getParentFile().mkdirs();

        try (InputStream bundled = AbstractCN1Mojo.class.getResourceAsStream(UPDATE_CODENAMEONE_JAR_RESOURCE)) {
            if (bundled != null) {
                getLog().info("Installing Codename One Updater from bundled plugin resource");
                copyToFile(bundled, re);
                return;
            }
        }

        IOException lastFailure = null;
        for (String url : new String[] { UPDATE_CODENAMEONE_JAR_URL, UPDATE_CODENAMEONE_JAR_FALLBACK_URL }) {
            getLog().info("Installing Codename One Updater from " + url);
            try (InputStream is = new URL(url).openStream()) {
                copyToFile(is, re);
                return;
            } catch (IOException ex) {
                lastFailure = ex;
                getLog().warn("Failed to download Codename One Updater from " + url + ": " + ex.getMessage());
            }
        }
        throw lastFailure != null ? lastFailure : new IOException("Failed to install Codename One updater");
    }

    private static void copyToFile(InputStream is, File dest) throws IOException {
        try (FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int len;
            while ((len = is.read(buf)) > -1) {
                os.write(buf, 0, len);
            }
        }
    }
    
    protected void updateCodenameOne(boolean force, File... files) throws MojoExecutionException {
        try {
            installUpdater();
        } catch (Exception ex) {
            getLog().error("Failed to install Codename One updater");
            throw new MojoExecutionException("Failed to install codenameone updater", ex);
        }
        if (!force) {
            // If we're not forcing an update, and there are no missing files being requested,
            // then we'll call it a day.
            boolean missing = false;
            for (File f : files) {
                if (!f.exists()) {
                    missing = true;
                    break;
                }
            }
            if (!missing) {
                return;
            }
        }
        
        Java java = createJava();
        java.setFork(true);
        java.setJar(new File(JPDATE_CODENAMEONE_JAR_PATH));
        File dummyProject = new File(project.getBuild().getDirectory(), path("codenameone", "update-dummy"));
        File dummyProjectLib = new File(dummyProject, "lib");
        dummyProjectLib.mkdirs();
        File cn1Properties = new File(getCN1ProjectDir(), "codenameone_settings.properties");
        if (cn1Properties.exists()) {
            try {
                FileUtils.copyFile(cn1Properties, new File(dummyProject, cn1Properties.getName()));
            } catch (IOException ex) {
                getLog().warn("Failed to copy "+cn1Properties+" into dummy project", ex);
            }
        }
        //java.createArg().setFile(getCN1ProjectDir());
        java.createArg().setFile(dummyProject);
        java.createArg().setValue("force");
        java.executeJava();
    }
    
    
    protected void copyKotlinIncrementalCompileOutputToOutputDir() {
        if ("true".equals(project.getProperties().getProperty("kotlin.compiler.incremental"))) {
            File kotlinIncrementalOutputDir = new File(project.getBuild().getDirectory() + File.separator + "kotlin-ic" + File.separator + "compile" + File.separator + "classes");
            File outputDir = new File(project.getBuild().getOutputDirectory());
            if (kotlinIncrementalOutputDir.exists()) {
                Copy copy = (Copy)antProject.createTask("copy");
                copy.setTodir(outputDir);
                FileSet files = new FileSet();
                files.setProject(antProject);
                files.setDir(kotlinIncrementalOutputDir);
                files.setIncludes("**");
                copy.addFileset(files);
                copy.setOverwrite(true);
                copy.execute();
            }

        }
    }
    protected static String OS = System.getProperty("os.name").toLowerCase();
    protected static boolean isWindows = (OS.indexOf("win") >= 0);
    protected static boolean isMac = (OS.indexOf("mac") >= 0);

    protected File getFFmpegDir() {
        String path = System.getProperty("ffmpeg.dir", null);
        if (path == null || path.isEmpty()) return null;
        return new File(path);
    }

    protected boolean isFFmpegSetup() {
        File dir = getFFmpegDir();
        if (dir == null || !dir.exists()) {
            return false;
        }
        return findExecutable(dir, "ffmpeg") != null && findExecutable(dir, "ffprobe") != null;
    }

    private File findExecutable(File root, String name) {
        if (root == null || !root.exists()) {
            return null;
        }
        String alt = isWindows ? name + ".exe" : name;
        if (root.isFile()) {
            return root.getName().equals(alt) ? root : null;
        }
        for (File child : root.listFiles()) {
            File found = findExecutable(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    protected void setupFFmpeg() {
        // The simulator resolves ffmpeg/ffprobe from the bundled
        // org.bytedeco:ffmpeg-platform binaries at runtime (see FFMPEGMedia), so
        // we no longer stage an externally installed ffmpeg here. Staging a copy
        // of a PATH executable is fragile -- e.g. on Windows it picks up a
        // chocolatey shimgen shim that does not work once copied out of place,
        // which silently produced zero decoded frames. We only honor an explicit
        // ffmpeg.dir override when one is already configured.
        if (isFFmpegSetup()) {
            project.getProperties().setProperty("ffmpeg.dir", getFFmpegDir().getAbsolutePath());
            System.setProperty("ffmpeg.dir", getFFmpegDir().getAbsolutePath());
        }
    }

    /**
     * Builds the classpath for the headless CSS compiler CLI
     * (com.codenameone:codenameone-css-cli, main class
     * {@link #CSS_CLI_MAIN_CLASS}), resolved transitively so it picks up
     * codenameone-css-compiler, codenameone-javase and their dependencies.
     *
     * <p>CSS compilation used to run {@code java -jar} against the
     * codenameone-designer jar-with-dependencies, a ~43MB shaded artifact that
     * bundled the whole Swing resource editor just to reach CN1CSSCLI. The CLI
     * now lives in its own thin module, so we resolve it as an ordinary
     * dependency and launch it with {@code -cp} instead. Nothing has to publish
     * a shaded copy of the JavaSE port.</p>
     *
     * @return classpath string suitable for {@code java -cp} / Ant's {@code createClasspath}.
     * @throws MojoExecutionException if the CLI could not be resolved. This might occur if
     * calling this method before dependencies have been resolved.
     */
    protected String getCssCliClasspath() throws MojoExecutionException {
        Artifact artifact = getArtifact("com.codenameone", "codenameone-css-cli");
        if (artifact == null) {
            throw new MojoExecutionException("Could not find the Codename One CSS compiler CLI "
                    + "(com.codenameone:codenameone-css-cli). It ships as a dependency of the "
                    + "codenameone-maven-plugin, so this usually means the plugin's own dependencies "
                    + "have not been resolved yet.");
        }
        List<File> files = new ArrayList<File>();
        addCssCliJar(files, artifact);
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setOffline(offline)
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<>(remoteRepositories))
                .setResolveTransitively(true)
                .setArtifact(artifact));
        if (result != null && result.getArtifacts() != null) {
            for (Artifact resolved : result.getArtifacts()) {
                addCssCliJar(files, resolved);
            }
        }
        if (files.isEmpty()) {
            throw new MojoExecutionException("Resolved com.codenameone:codenameone-css-cli but it "
                    + "produced an empty classpath.");
        }
        StringBuilder classpath = new StringBuilder();
        for (File file : files) {
            if (classpath.length() > 0) {
                classpath.append(File.pathSeparator);
            }
            classpath.append(file.getAbsolutePath());
        }
        return classpath.toString();
    }

    private static void addCssCliJar(List<File> files, Artifact artifact) {
        if (artifact == null || artifact.getFile() == null || !"jar".equals(artifact.getType())) {
            return;
        }
        File file = artifact.getFile().getAbsoluteFile();
        if (!file.exists() || files.contains(file)) {
            return;
        }
        files.add(file);
    }

    /**
     * Get the designer jar, equivalent to the designer_1.jar in the user's home directory
     * but resolved through Maven so the version is explicit.
     *
     * <p>The Resource Editor is deprecated and frozen at {@link #designerVersion}, so it is
     * resolved <em>on demand</em> rather than declared as a plugin dependency: only
     * {@code cn1:designer} needs it, and an ordinary build should not download ~43MB of
     * Swing editor. CSS compilation, which used to come along for the ride, moved to
     * {@link #getCssCliClasspath()}.</p>
     *
     * @return The Codename One designer jar with all dependencies.
     * @throws MojoExecutionException If the designer jar could not be resolved.
     */
    protected File getDesignerJar() throws MojoExecutionException{
        Artifact artifact = getArtifact("com.codenameone", "codenameone-designer", "jar-with-dependencies");
        if (artifact == null) {
            artifact = repositorySystem.createArtifactWithClassifier(
                    "com.codenameone", "codenameone-designer", designerVersion, "jar", "jar-with-dependencies");
        }
        File file = findArtifactFile(artifact);
        if (file == null) {
            throw new MojoExecutionException("Could not resolve the Codename One Resource Editor "
                    + "(com.codenameone:codenameone-designer:" + designerVersion + ":jar-with-dependencies).\n"
                    + "The editor is deprecated and frozen at that version; override it with "
                    + "-Dcn1.designer.version=<version> if you need a different one.");
        }

        File extracted = new File(file.getParentFile(), file.getName()+"-extracted");
        File designerJar = new File(extracted, "designer_1.jar");

        if (!designerJar.exists() || designerJar.lastModified() < file.lastModified()) {
            Expand expand = (Expand)antProject.createTask("unzip");
            expand.setSrc(file);
            expand.setDest(extracted);
            expand.execute();
        }


        if (!designerJar.exists()) {
            throw new MojoExecutionException("Failed to extract designer_1.jar from artifact "+artifact);
        }
        return designerJar;
    }

    protected boolean isCN1ProjectDir() {
        if (getCN1ProjectDir() == null) {
            getLog().debug("Skipping guibuilder because this is not a CN1 project");
            return false;
        }
        try {
            if (!getCN1ProjectDir().getCanonicalFile().equals(project.getBasedir().getCanonicalFile())) {
                getLog().debug("Skipping guibuilder because this is not a CN1 project");
                return false;
            }
        } catch (IOException ex) {
            getLog().error("Failed to get canonical paths for project dir", ex);
            return false;
        }
        return true;
    }
   

    /**
     * Every directory a source could be compiled from, not only the ones
     * {@code getCompileSourceRoots} lists.
     *
     * <p>build-helper and the generated-source plugins do add their roots there,
     * but the Kotlin plugin compiles its own {@code <sourceDirs>} without adding
     * them back -- so in a module that configures them, a Kotlin class could
     * have a perfectly good source and still look deleted. The orphan filter
     * would then drop it silently and its misplaced annotation would produce
     * neither its hint nor the placement error.</p>
     *
     * <p>The conventional {@code src/main/kotlin} is included when it exists for
     * the same reason: this list is used to decide that a source is ABSENT, and
     * a list that is merely incomplete must not be read as that.</p>
     */
    protected static List<String> compileSourceRoots(MavenProject project) {
        return compileSourceRoots(project, null);
    }

    /**
     * The same list, resolving `${...}` against the `-D` properties the build
     * was invoked with as well as the module's own.
     *
     * @param userProperties the session's user properties, or null when there
     *                       is no session to read them from
     */
    protected static List<String> compileSourceRoots(MavenProject project,
                                                     Properties userProperties) {
        if (project == null) {
            return null;
        }
        Interpolation expressions = new Interpolation(project, userProperties);
        List<String> roots = new ArrayList<String>();
        List<String> configured = project.getCompileSourceRoots();
        if (configured != null && compilesJava(project)) {
            roots.addAll(configured);
        }
        addKotlinSourceDirs(expressions, roots);
        // The conventional Kotlin root, but only where the Kotlin plugin has not
        // said where its sources are. A configured <sourceDirs> REPLACES the
        // default, so an existing src/main/kotlin beside one is a tree the build
        // does not compile -- and a stale class whose source still sits there
        // then looked live, so the orphan filter kept it and the placement error
        // it carries fired on every build.
        File basedir = project.getBasedir();
        // ...and only where the build compiles Kotlin at all. A module that
        // never had the plugin, or had it removed, does not compile
        // src/main/kotlin however many .kt files are sitting in it. Adding it
        // anyway made a stale class in target/classes look LIVE because its old
        // source was still on disk, and a build hint annotation on that class
        // then failed the placement check on every incremental build -- a hard
        // error nothing in the project could clear except deleting files.
        if (basedir != null && compilesTheConventionalKotlinRoot(project)) {
            File kotlin = new File(basedir, "src" + File.separator + "main"
                    + File.separator + "kotlin");
            if (kotlin.isDirectory() && !roots.contains(kotlin.getAbsolutePath())) {
                roots.add(kotlin.getAbsolutePath());
            }
        }
        addBuildHelperSources(expressions, roots);
        return roots;
    }

    /**
     * The configurations that apply to {@code goal}, most specific first: each
     * execution bound to it, then the plugin-level one.
     *
     * <p>Maven merges plugin-level configuration into every execution, so a
     * parameter written once outside them applies to this goal too -- and an
     * execution's own value overrides it. Reading only one of the two got both
     * halves wrong in turn: the build-helper roots missed a plugin-level
     * {@code <sources>}, and the compiler encoding reported the plugin-level
     * value over an execution that overrides it.</p>
     *
     * <p>An execution that names no goal but carries Maven's own id for one --
     * {@code default-compile} -- is bound to it: that is how a POM overrides a
     * lifecycle-injected execution.</p>
     */
    private static List<Object> configurationsFor(org.apache.maven.model.Plugin plugin,
                                                  String goal, String element) {
        return configurationsFor(plugin, goal, element, true);
    }

    /**
     * The same, with {@code runsWithoutExecution} saying whether the goal is
     * bound when the POM writes no execution for it.
     *
     * <p>It is for {@code maven-compiler-plugin}, which the default lifecycle
     * binds, and for a Kotlin plugin with {@code <extensions>true</extensions>}.
     * It is NOT for {@code build-helper-maven-plugin}, whose {@code add-source}
     * runs only where an execution says so: plugin-level {@code <sources>} with
     * no execution is dormant configuration, and treating it as a compiled root
     * made a stale class in target/classes look live because its source sits
     * there -- failing the placement check on every incremental build over a
     * directory Maven never reads.</p>
     */
    private static List<Object> configurationsFor(org.apache.maven.model.Plugin plugin,
                                                  String goal, String element,
                                                  boolean runsWithoutExecution) {
        List<Object> out = new ArrayList<Object>();
        boolean bound = false;
        if (plugin.getExecutions() != null) {
            for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                boolean thisOne = execution.getGoals() != null
                        && execution.getGoals().contains(goal);
                // The id alone binds the goal only where the LIFECYCLE provides
                // an execution for it -- maven-compiler-plugin, and a Kotlin
                // plugin with <extensions>true</extensions>. build-helper never
                // gets one, so `default-add-source` with no <goal> is an
                // execution with an odd name that runs nothing, and counting it
                // pulled in sources Maven does not compile. That is the same
                // question runsWithoutExecution answers.
                if (!thisOne && runsWithoutExecution
                        && ("default-" + goal).equals(execution.getId())) {
                    thisOne = true;
                }
                if (!thisOne || isDisabled(execution)) {
                    // A disabled execution contributes no configuration and does
                    // not count as binding the goal -- with `bound` set, an
                    // execution switched off with <phase>none</phase> would hide
                    // the plugin-level configuration that still applies.
                    continue;
                }
                bound = true;
                // The execution's own value REPLACES the plugin-level one --
                // Maven merges by element, and a repeated list is not appended
                // unless the POM says so. Taking both would have added a
                // directory the build does not compile, which is a phantom root
                // for everything downstream to scan.
                //
                // Unless the POM DOES say so: `combine.children="append"` is how
                // it asks for both, and then both are in effect.
                if (!has(execution.getConfiguration(), element)) {
                    // `combine.self="override"` discards the inherited
                    // configuration wholesale, so an execution that says it and
                    // omits the element is not falling back to the plugin's --
                    // it has none. Reporting the plugin-level value there named
                    // a setting the build does not use.
                    if (!overrides(execution.getConfiguration())) {
                        out.add(plugin.getConfiguration());
                    }
                    continue;
                }
                out.add(execution.getConfiguration());
                if (appends(execution.getConfiguration(), element)
                        && plugin.getConfiguration() != null) {
                    out.add(plugin.getConfiguration());
                }
            }
        }
        if (!bound && runsWithoutExecution && plugin.getConfiguration() != null) {
            out.add(plugin.getConfiguration());
        }
        return out;
    }

    /// Whether the POM asked for this element's children to be appended to the
    /// inherited ones rather than to replace them.
    ///
    /// Maven reads the attribute on the element itself or on the configuration
    /// it sits in, so both are checked.
    private static boolean appends(Object configuration, String element) {
        if (!(configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom)) {
            return false;
        }
        org.codehaus.plexus.util.xml.Xpp3Dom root =
                (org.codehaus.plexus.util.xml.Xpp3Dom) configuration;
        return "append".equals(root.getAttribute("combine.children"))
                || (root.getChild(element) != null
                    && "append".equals(root.getChild(element).getAttribute("combine.children")));
    }

    /// Whether the POM asked for this configuration to replace the inherited
    /// one entirely rather than merge with it.
    private static boolean overrides(Object configuration) {
        return configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom
                && "override".equals(((org.codehaus.plexus.util.xml.Xpp3Dom) configuration)
                        .getAttribute("combine.self"));
    }

    private static boolean has(Object configuration, String element) {
        return configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom
                && ((org.codehaus.plexus.util.xml.Xpp3Dom) configuration).getChild(element) != null;
    }

    /**
     * build-helper's {@code add-source} directories.
     *
     * <p>That goal runs at {@code generate-sources} and adds them to the project
     * itself, so a mojo bound after it sees them already. A goal invoked
     * DIRECTLY -- {@code mvn cn1:settings} -- runs no lifecycle at all, so the
     * list it reads is missing them, and a main class living only in an added
     * root looked absent.</p>
     *
     * <p>{@code add-test-source} uses the same element and is passed over, the
     * same distinction the Kotlin plugin's compile and test-compile executions
     * need.</p>
     */
    private static void addBuildHelperSources(Interpolation expressions, List<String> roots) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = expressions.project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return;
        }
        if (plugins == null) {
            return;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"build-helper-maven-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            // Once per execution bound to add-source, taking the configuration
            // that actually supplies its <sources>. Every such execution adds
            // its own roots, so these accumulate -- but within one execution the
            // levels do not, they override.
            for (Object configuration
                    : configurationsFor(plugin, "add-source", "sources", false)) {
                addSourcesFrom(expressions, configuration, roots);
            }
        }
    }

    private static void addSourcesFrom(Interpolation expressions, Object configuration,
                                       List<String> roots) {
        if (!(configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom)) {
            return;
        }
        org.codehaus.plexus.util.xml.Xpp3Dom sources =
                ((org.codehaus.plexus.util.xml.Xpp3Dom) configuration).getChild("sources");
        if (sources == null) {
            return;
        }
        for (org.codehaus.plexus.util.xml.Xpp3Dom source : sources.getChildren()) {
            addRoot(expressions, source.getValue(), roots);
        }
    }

    /**
     * A configured path with every expression this can resolve applied, or null
     * when it is empty or still holds one it could not.
     *
     * <p>Maven usually interpolates these while building the model, so this is
     * normally a no-op -- but a value that arrives unexpanded was being dropped
     * outright, and `${project.basedir}/appsrc` is an ordinary way to write a
     * root. A root dropped here is a main class the migration cannot find.</p>
     *
     * <p>A `$` that opens nothing is an ordinary character in a path and is
     * left alone; only an unresolved `${...}` makes the value unusable.</p>
     */
    private static String expandProjectExpressions(Interpolation expressions, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String out = expressions.resolve(value.trim());
        return out.indexOf("${") >= 0 ? null : out;
    }

    /**
     * Resolves `${...}` the way Maven does when it hands a plugin its
     * configuration: the project's own expressions, then the `-D` properties
     * the build was invoked with, then the module's properties -- which is
     * where a profile Maven activated has already put its own.
     *
     * <p>Only the project expressions used to be resolved, so a root or an
     * encoding written as `${generated.sources}` or `${source.charset}` was
     * discarded even though Maven compiles with it. Discarding a root loses a
     * main class; discarding an encoding decodes a non-ASCII name with the
     * wrong charset, or reports an inherited encoding that is not the one in
     * force.</p>
     */
    private static final class Interpolation {

        /** Long enough for a property defined in terms of another; a cycle stops here. */
        private static final int PASSES = 8;

        private final MavenProject project;
        private final Properties user;

        Interpolation(MavenProject project, Properties user) {
            this.project = project;
            this.user = user;
        }

        String resolve(String value) {
            if (value == null) {
                return null;
            }
            String out = value;
            for (int pass = 0; pass < PASSES && out.indexOf("${") >= 0; pass++) {
                StringBuilder expanded = new StringBuilder();
                boolean changed = false;
                int at = 0;
                while (true) {
                    int open = out.indexOf("${", at);
                    if (open < 0) {
                        expanded.append(out.substring(at));
                        break;
                    }
                    int close = out.indexOf('}', open + 2);
                    if (close < 0) {
                        // Not an expression, just a stray `${`.
                        expanded.append(out.substring(at));
                        break;
                    }
                    expanded.append(out, at, open);
                    String resolved = valueOf(out.substring(open + 2, close));
                    if (resolved == null) {
                        expanded.append(out, open, close + 1);
                    } else {
                        expanded.append(resolved);
                        changed = true;
                    }
                    at = close + 1;
                }
                out = expanded.toString();
                if (!changed) {
                    // Everything left is a name nothing defines; another pass
                    // would produce the same string.
                    break;
                }
            }
            return out;
        }

        private String valueOf(String key) {
            if (key.isEmpty()) {
                return null;
            }
            // The project's own expressions first: a property named
            // `project.basedir` does not shadow the real basedir in Maven
            // either.
            String standard = standard(key);
            if (standard != null) {
                return standard;
            }
            if (user != null) {
                String value = user.getProperty(key);
                if (value != null) {
                    return value;
                }
            }
            if (project != null && project.getProperties() != null) {
                String value = project.getProperties().getProperty(key);
                if (value != null) {
                    return value;
                }
            }
            return System.getProperty(key);
        }

        private String standard(String key) {
            if (project == null) {
                return null;
            }
            File basedir = project.getBasedir();
            if (basedir != null
                    && ("project.basedir".equals(key) || "project.baseDir".equals(key)
                        || "basedir".equals(key) || "pom.basedir".equals(key))) {
                return basedir.getAbsolutePath();
            }
            if (project.getBuild() != null) {
                if ("project.build.directory".equals(key)) {
                    return project.getBuild().getDirectory();
                }
                if ("project.build.outputDirectory".equals(key)) {
                    return project.getBuild().getOutputDirectory();
                }
                if ("project.build.sourceDirectory".equals(key)) {
                    return project.getBuild().getSourceDirectory();
                }
            }
            if ("project.groupId".equals(key)) {
                return project.getGroupId();
            }
            if ("project.artifactId".equals(key)) {
                return project.getArtifactId();
            }
            if ("project.version".equals(key)) {
                return project.getVersion();
            }
            return null;
        }
    }

    /**
     * Whether {@code src/main/kotlin} is a root this module actually compiles.
     *
     * <p>Two conditions, and both are about what Maven does rather than what is
     * on disk: the Kotlin plugin has to be bound to the {@code compile} goal --
     * a {@code test-compile} execution compiles the test tree, not this one --
     * and it must not say where its sources are, because a configured
     * {@code <sourceDirs>} REPLACES the default.</p>
     */
    private static boolean compilesTheConventionalKotlinRoot(MavenProject project) {
        return hasKotlinCompileExecution(project) && !declaresKotlinSourceDirs(project);
    }

    /**
     * Whether `execution` was switched off with `<phase>none</phase>`.
     *
     * <p>That is the conventional way to disable an execution inherited from a
     * parent while leaving its goal in place, so the goal alone does not mean
     * the build runs it. Treating a disabled one as live made a stale class in
     * target/classes look current because its source is still on disk, and a
     * build hint annotation on that class then failed the placement check on
     * every incremental build.</p>
     *
     * <p>Only `none`. Maven ignores any phase outside the lifecycle, but naming
     * the others needs a lifecycle model this does not have, and `none` is the
     * spelling every POM uses.</p>
     */
    private static boolean isDisabled(org.apache.maven.model.PluginExecution execution) {
        String phase = execution == null ? null : execution.getPhase();
        return phase != null && "none".equalsIgnoreCase(phase.trim());
    }

    /**
     * Whether the Kotlin plugin compiles this module's main sources.
     *
     * <p>An execution bound to {@code compile}, or {@code <extensions>true</extensions>}
     * -- which is the documented way to let the plugin contribute its own
     * lifecycle, and then it compiles with no execution written at all. Reading
     * only the executions called such a module Kotlin-less, so an existing
     * {@code src/main/kotlin} was left out of the roots and a Kotlin main class
     * living there could not be found.</p>
     */
    private static boolean hasKotlinCompileExecution(MavenProject project) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return false;
        }
        if (plugins == null) {
            return false;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"kotlin-maven-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            if (kotlinRunsWithoutExecution(plugin)) {
                return true;
            }
            if (plugin.getExecutions() == null) {
                continue;
            }
            {
                for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                    if (bindsCompile(execution, plugin.isExtensions()) && !isDisabled(execution)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether anything compiles the roots {@code getCompileSourceRoots} lists.
     *
     * <p>That list comes from the model, not from what the build runs, so it is
     * still populated when a POM switches {@code default-compile} off with
     * {@code <phase>none</phase>}. A source sitting in a tree nothing compiles
     * would then vouch for a stale class in {@code target/classes}, and a
     * misplaced annotation on that class fails every incremental build.</p>
     *
     * <p>Only a JAVA compiler execution counts. An earlier version of this also
     * accepted a Kotlin compile execution, on the belief that a module could
     * leave its Java sources to the Kotlin plugin -- which is not what happens.
     * kotlinc does JOINT compilation: it reads Java sources to resolve against
     * them and emits no class files for them, which is exactly why Kotlin's own
     * documented Maven setup disables {@code default-compile} and then adds a
     * {@code java-compile} execution back. That replacement is what the second
     * condition sees, and it is the only thing that makes those roots
     * compiled.</p>
     */
    private static boolean compilesJava(MavenProject project) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return true;
        }
        if (plugins == null) {
            return true;
        }
        boolean defaultCompileDisabled = false;
        boolean replacementBound = false;
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"maven-compiler-plugin".equals(plugin.getArtifactId())
                    || plugin.getExecutions() == null) {
                continue;
            }
            for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                boolean compiles = execution.getGoals() != null
                        && execution.getGoals().contains("compile");
                if ("default-compile".equals(execution.getId())) {
                    if (isDisabled(execution)) {
                        defaultCompileDisabled = true;
                    } else {
                        replacementBound = true;
                    }
                } else if (compiles && !isDisabled(execution)) {
                    replacementBound = true;
                }
            }
        }
        return !defaultCompileDisabled || replacementBound;
    }

    /**
     * Whether this Kotlin plugin compiles with no execution written for it.
     *
     * <p>That is what {@code <extensions>true</extensions>} buys -- the plugin
     * contributes its own lifecycle. A POM switches THAT off the way it switches
     * off any inherited execution, {@code <id>default-compile</id>} with
     * {@code <phase>none</phase>}, and then nothing is bound: not the
     * conventional root, and not the plugin-level {@code <sourceDirs>} either,
     * which is a directory Maven does not compile and a place a stale annotated
     * class can keep a source.</p>
     *
     * <p>Only {@code default-compile} cancels it. The execution the extension
     * contributes is that one, so disabling a DIFFERENTLY named execution that
     * happens to bind {@code compile} switches off only that execution and
     * leaves the lifecycle running. Reading any disabled compile execution as
     * cancellation dropped the Kotlin roots of a module that still compiles
     * Kotlin, which classifies a live class as stale -- the opposite mistake,
     * and the one that loses a real annotation.</p>
     */
    private static boolean kotlinRunsWithoutExecution(org.apache.maven.model.Plugin plugin) {
        if (!plugin.isExtensions()) {
            return false;
        }
        if (plugin.getExecutions() != null) {
            for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                if ("default-compile".equals(execution.getId()) && isDisabled(execution)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Whether `execution` binds the `compile` goal, disabled or not.
    ///
    /// `lifecycleProvided` says whether the plugin has a lifecycle-injected
    /// execution at all: without `<extensions>true</extensions>` the Kotlin
    /// plugin has none, so `default-compile` with no `<goal>` is a name and
    /// nothing more.
    private static boolean bindsCompile(org.apache.maven.model.PluginExecution execution,
                                        boolean lifecycleProvided) {
        if (execution.getGoals() != null && execution.getGoals().contains("compile")) {
            return true;
        }
        return lifecycleProvided && "default-compile".equals(execution.getId());
    }

    /** Whether the Kotlin plugin says where its main sources are. */
    private static boolean declaresKotlinSourceDirs(MavenProject project) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return false;
        }
        if (plugins == null) {
            return false;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"kotlin-maven-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            for (Object configuration
                    : configurationsFor(plugin, "compile", "sourceDirs",
                            kotlinRunsWithoutExecution(plugin))) {
                if (has(configuration, "sourceDirs")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The Kotlin plugin's {@code <sourceDirs>}, wherever they are configured. */
    private static void addKotlinSourceDirs(Interpolation expressions, List<String> roots) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = expressions.project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return;
        }
        if (plugins == null) {
            return;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"kotlin-maven-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            // The same selection build-helper uses: the `compile` goal only --
            // a `test-compile` execution's sourceDirs are src/test/kotlin and
            // friends, and adding them made a deleted production class look
            // like it still had a source -- and within an execution the
            // configuration levels override rather than accumulate.
            for (Object configuration
                    : configurationsFor(plugin, "compile", "sourceDirs",
                            kotlinRunsWithoutExecution(plugin))) {
                addSourceDirsFrom(expressions, configuration, roots);
            }
        }
    }

    private static void addSourceDirsFrom(Interpolation expressions, Object configuration,
                                          List<String> roots) {
        if (!(configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom)) {
            return;
        }
        org.codehaus.plexus.util.xml.Xpp3Dom dirs =
                ((org.codehaus.plexus.util.xml.Xpp3Dom) configuration).getChild("sourceDirs");
        if (dirs == null) {
            return;
        }
        for (org.codehaus.plexus.util.xml.Xpp3Dom dir : dirs.getChildren()) {
            addRoot(expressions, dir.getValue(), roots);
        }
    }

    private static void addRoot(Interpolation expressions, String value, List<String> roots) {
        String path = expandProjectExpressions(expressions, value);
        if (path == null) {
            return;
        }
        File f = new File(path);
        if (!f.isAbsolute() && expressions.project.getBasedir() != null) {
            f = new File(expressions.project.getBasedir(), path);
        }
        if (!roots.contains(f.getAbsolutePath())) {
            roots.add(f.getAbsolutePath());
        }
    }


    /**
     * The source encoding Maven compiles `module` with, or null when nothing
     * says.
     *
     * <p>The plugin's own {@code <encoding>} first. The parameter DEFAULTS to
     * {@code ${project.build.sourceEncoding}}, so an explicit one overrides the
     * property -- reading the property first meant a module that sets the
     * plugin parameter got its parent's value instead of its own.</p>
     *
     * <p>Read from the effective model, so a profile Maven activated is already
     * folded in -- which is the part a tool reading POM text cannot do.</p>
     */
    protected static String sourceEncodingOf(MavenProject module) {
        return sourceEncodingOf(module, null);
    }

    /**
     * The same answer, resolving `${...}` against the `-D` properties the build
     * was invoked with as well as the module's own.
     *
     * @param userProperties the session's user properties, or null when there
     *                       is no session to read them from
     */
    protected static String sourceEncodingOf(MavenProject module, Properties userProperties) {
        if (module == null) {
            return null;
        }
        Interpolation expressions = new Interpolation(module, userProperties);
        String encoding = compilerPluginEncoding(expressions);
        if (encoding == null || encoding.trim().isEmpty()) {
            encoding = expressions.valueOf("project.build.sourceEncoding");
            if (encoding == null) {
                encoding = expressions.valueOf("maven.compiler.encoding");
            }
            // A property can be written in terms of another one.
            encoding = expressions.resolve(encoding);
            if (encoding != null && encoding.indexOf("${") >= 0) {
                encoding = null;
            }
        }
        return encoding == null || encoding.trim().isEmpty() ? null : encoding.trim();
    }

    /// The `<encoding>` maven-compiler-plugin is configured with, from the
    /// EFFECTIVE model -- so a profile that Maven activated is already folded in.
    private static String compilerPluginEncoding(Interpolation expressions) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = expressions.project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return null;
        }
        if (plugins == null) {
            return null;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"maven-compiler-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            // Most specific first: an execution bound to `compile` overrides
            // the plugin-level value, and testCompile's is not this one.
            for (Object configuration : configurationsFor(plugin, "compile", "encoding")) {
                String encoding = encodingIn(expressions, configuration);
                if (encoding != null) {
                    return encoding;
                }
            }
        }
        return null;
    }

    private static String encodingIn(Interpolation expressions, Object configuration) {
        if (!(configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom)) {
            return null;
        }
        org.codehaus.plexus.util.xml.Xpp3Dom encoding =
                ((org.codehaus.plexus.util.xml.Xpp3Dom) configuration).getChild("encoding");
        if (encoding == null || encoding.getValue() == null) {
            return null;
        }
        String value = expressions.resolve(encoding.getValue().trim());
        // An expression nothing defines is not an encoding -- but a `$` that
        // opens nothing is just a character, and a resolved one is the charset
        // Maven really compiles with.
        return value.isEmpty() || value.indexOf("${") >= 0 ? null : value;
    }


    // ------------------------------------------------------------------
    // SVG transcoder configuration
    // ------------------------------------------------------------------
    //
    // Declared here rather than on TranscodeSVGMojo alone so that Maven injects
    // them into every goal of this plugin. Two goals other than the transcoder
    // need the same answers -- the self-repair below, and the placeholder
    // diagnostic in CompileCSSMojo -- and both were reading defaults while the
    // project had configured something else. A repair that transcodes from a
    // directory the next build will not use, into an output directory the next
    // build will not read, is worse than no repair. Maven resolves plugin-level
    // <configuration> and the cn1.svg.* properties for any goal that declares
    // the parameter, so declaring it once here covers both without anyone
    // parsing Xpp3Dom by hand.

    @Parameter(property = "cn1.svg.sourceDirs")
    protected List<String> svgSourceDirs;

    @Parameter(property = "cn1.svg.outputDir",
            defaultValue = "${project.build.directory}/generated-sources/svg")
    protected File svgOutputDir;

    @Parameter(property = "cn1.svg.placeholderDir",
            defaultValue = "${project.build.directory}/css-resources")
    protected File svgPlaceholderDir;

    @Parameter(property = "cn1.svg.package", defaultValue = SvgTranscodeRunner.DEFAULT_PACKAGE)
    protected String svgPackage;

    /** Where generated vector sources go, falling back to the standard location
     *  when the parameter was not injected (a directly constructed mojo). */
    protected File svgOutputDir() {
        return svgOutputDir != null ? svgOutputDir
                : new File(project.getBuild().getDirectory(),
                        "generated-sources" + File.separator + "svg");
    }

    protected String svgPackage() {
        return svgPackage != null && !svgPackage.isEmpty()
                ? svgPackage : SvgTranscodeRunner.DEFAULT_PACKAGE;
    }

    /** A transcoder configured exactly as the bound goal would be. */
    protected SvgTranscodeRunner newSvgTranscodeRunner() {
        File placeholders = svgPlaceholderDir != null ? svgPlaceholderDir
                : new File(project.getBuild().getDirectory(), "css-resources");
        return new SvgTranscodeRunner(project.getBasedir(), svgSourceDirs,
                svgOutputDir(), placeholders, svgPackage(), getLog());
    }

    // ------------------------------------------------------------------
    // SVG transcoder self-repair
    // ------------------------------------------------------------------

    /**
     * Adds {@code dir} to the module's compile source roots if it is not
     * already there. Called by the SVG transcoder and by the self-repair
     * below, both of which generate Java sources that javac has to see.
     */
    protected void registerSourceRoot(File dir) {
        String path = dir.getAbsolutePath();
        if (!project.getCompileSourceRoots().contains(path)) {
            project.addCompileSourceRoot(path);
            getLog().debug("Added compile source root " + path);
        }
    }

    /**
     * Brings a project created before the build-time SVG transcoder existed up
     * to date, if and only if it actually has vector assets.
     *
     * <p>The {@code transcode-svg} goal is not bound by any lifecycle mapping
     * an application module uses -- {@code components.xml} only maps the
     * {@code cn1lib} packaging, and an app's {@code common} module is
     * {@code jar} -- so it runs only when the pom declares the execution
     * explicitly. That execution was added to the archetype well after the
     * Maven project format shipped, which leaves every project generated before
     * it silently without a transcoder.</p>
     *
     * <p>"Silently" is the problem worth fixing. The CSS compiler writes a 1x1
     * transparent PNG into the theme for every {@code url(*.svg)} it sees and
     * relies on the generated {@code SVGRegistry} to replace those entries at
     * startup. With no registry nothing replaces them, so
     * {@code theme.getImage("logo.svg")} returns a perfectly valid fully
     * transparent 1x1 image: no exception, no warning, no null -- just a screen
     * with nothing on it. The simulator hides this, because JavaSEPort finds
     * the registry reflectively by class name and therefore tolerates any
     * classpath layout, while the device builders look for the compiled class
     * at one fixed path and emit no {@code installGlobal()} call when it is
     * absent. The result is an app that looks correct in the simulator and
     * renders blank on the device.</p>
     *
     * <p>So this does both halves: it transcodes now, into the current build,
     * and then rewrites the pom so subsequent builds bind the goal the ordinary
     * way. Doing only the second half would leave this build shipping the blank
     * placeholders it just diagnosed.</p>
     *
     * <p>Must be called before {@code compile} for the first half to have any
     * effect -- the generated sources are handed to javac through
     * {@link #registerSourceRoot}.</p>
     */
    protected void ensureSvgTranscoderWired() throws MojoExecutionException {
        if (!isCN1ProjectDir()) {
            return;
        }
        if (isTranscodeSvgBound()) {
            return;
        }
        if (!isCN1ApplicationModule()) {
            // Applications only. getCN1ProjectDir() also recognizes a cn1lib
            // (codenameone_library_appended.properties) and the cn1lib archetype
            // binds generate-gui-sources too, so without this a library holding
            // any .svg would be repaired as well -- and every registry is emitted
            // under the one fixed name com.codename1.generated.svg.SVGRegistry,
            // which the per-platform builders look for at that literal path. Two
            // of them on one classpath means one wins and the other's images stay
            // placeholders.
            //
            // That collision is a property of the transcoder's fixed-name
            // registry, not of this repair: a library author who adds the
            // execution by hand hits it exactly the same way. Aggregating
            // multiple registries is a change to the transcoder's design and does
            // not belong in a repair path. What does belong here is not creating
            // the situation silently, in a module whose author never asked for a
            // registry, while also rewriting their pom.
            getLog().debug("Skipping the SVG transcoder repair: not an application module.");
            return;
        }
        // Screen first, then run the transcoder exactly as the bound goal
        // will run it. The repair used to have its own tolerant mode, which
        // meant the run that decided the pom was safe to edit was not the run
        // the pom then installed -- a file could transcode here and fail
        // forever afterwards. One behaviour, decided before anything happens:
        // either every input is something the goal can be relied on to read,
        // in which case transcode and record it, or none of this happens.
        SvgTranscodeRunner runner = newSvgTranscodeRunner();
        List<String> unreadable;
        try {
            if (!runner.hasVectorSources()) {
                // Nothing to transcode. A project with no vector assets is not
                // out of date in any way that matters, so leave its pom alone.
                return;
            }
            unreadable = runner.unreadableSources();
        } catch (Exception ex) {
            getLog().warn("Could not examine this project's vector sources (" + ex
                    + "). The project has been left exactly as it was.");
            return;
        }
        if (!unreadable.isEmpty()) {
            // A .lottie is a ZIP the JSON parser cannot read, and an unrelated
            // .json in a vector directory is claimed by extension alone. Running
            // the goal over either aborts the build -- correct for a goal the
            // developer bound, unacceptable for a repair that nobody asked for.
            getLog().warn("Not running the build-time vector transcoder: these file(s) sit in "
                    + "a vector source directory but are not animations the goal can read:");
            for (String name : unreadable) {
                getLog().warn("    " + name);
            }
            getLog().warn("Move them elsewhere and rebuild, or add the transcode-svg "
                    + "execution yourself if they really are animations.");
            return;
        }

        getLog().info("This project has SVG/Lottie assets but its pom does not run the "
                + "build-time vector transcoder. Transcoding them now.");
        try {
            runner.run();
            registerSourceRoot(svgOutputDir());
        } catch (Exception ex) {
            // Screened and still unhappy. Whatever it is, this build was
            // passing before the repair touched it and must still pass.
            getLog().warn("The build-time vector transcoder could not run over this project ("
                    + ex + "). The project has been left exactly as it was.");
            return;
        }
        addTranscodeSvgExecutionToPom();
    }

    /**
     * True when this module is a Codename One <em>application</em>, as opposed
     * to a cn1lib. The two are told apart by which marker file the project dir
     * holds: an app has {@code codenameone_settings.properties}, a library has
     * {@code codenameone_library_appended.properties} and no settings file.
     */
    private boolean isCN1ApplicationModule() {
        File dir = getCN1ProjectDir();
        return dir != null && new File(dir, "codenameone_settings.properties").isFile();
    }

    /** True when some execution of this plugin binds the transcode-svg goal. */
    private boolean isTranscodeSvgBound() {
        List<org.apache.maven.model.Plugin> plugins = project.getBuildPlugins();
        if (plugins == null) {
            return false;
        }
        for (org.apache.maven.model.Plugin p : plugins) {
            if (!GROUP_ID.equals(p.getGroupId())
                    || !"codenameone-maven-plugin".equals(p.getArtifactId())) {
                continue;
            }
            for (org.apache.maven.model.PluginExecution e : p.getExecutions()) {
                if (e.getGoals() != null && e.getGoals().contains("transcode-svg")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Inserts the {@code transcode-svg} execution into this module's pom.
     *
     * <p>Edited as text rather than through the Maven model: a pom is a file a
     * developer owns and reads, and round-tripping it through a model writer
     * reflows whitespace and can drop comments across the whole document to add
     * eight lines. A located insert changes exactly the bytes being added. The
     * result is parsed before it is written, and the original is kept as
     * {@code pom.xml.bak}, so a pom this does not understand is left alone
     * rather than damaged.</p>
     */
    private void addTranscodeSvgExecutionToPom() {
        File pomFile = project.getFile();
        if (pomFile == null || !pomFile.isFile()) {
            warnCouldNotEditPom("the module has no pom file on disk");
            return;
        }
        // Read and write in the encoding the document declares, not in UTF-8.
        // A pom that says ISO-8859-1 and carries a non-ASCII developer name or
        // description would otherwise be decoded wrongly, re-encoded as UTF-8
        // and written back under an unchanged declaration -- silent corruption
        // of a file this build does not own. The model-parser check below
        // cannot see it either, because it is handed a String.
        byte[] pomBytes;
        try {
            pomBytes = FileUtils.readFileToByteArray(pomFile);
        } catch (IOException ex) {
            warnCouldNotEditPom("it could not be read: " + ex.getMessage());
            return;
        }
        Charset charset = declaredXmlEncoding(pomBytes);
        if (charset == null) {
            warnCouldNotEditPom("its declared XML encoding is not supported by this JVM");
            return;
        }
        String pom = new String(pomBytes, charset);
        if (pomDeclaresTranscodeSvg(pom)) {
            // The file already declares it even though the resolved model did
            // not report it -- an inactive profile is the usual reason. Adding
            // a second copy would be worse than doing nothing. Asked of the
            // parsed model rather than the raw text: a substring search also
            // matched the goal named in a comment or in some unrelated value,
            // and then silently refused to repair a project that had never
            // declared it at all.
            return;
        }
        String updated = insertTranscodeSvgExecution(pom);
        if (updated == null) {
            warnCouldNotEditPom("its codenameone-maven-plugin element could not be located");
            return;
        }
        if (!parsesAndBindsTranscodeSvg(updated)) {
            // The edit produced something Maven would not accept, or would
            // accept without actually binding the goal. Never write it.
            warnCouldNotEditPom("the edit did not produce a pom that Maven can read"
                    + " with the goal bound");
            return;
        }
        File backup = unusedBackupFile(pomFile);
        if (backup == null) {
            warnCouldNotEditPom("no free name was available for a backup of it");
            return;
        }
        try {
            FileUtils.copyFile(pomFile, backup);
            writeInPlace(pomFile, updated, charset, backup, getLog());
        } catch (IOException ex) {
            warnCouldNotEditPom("it could not be written: " + ex.getMessage());
            return;
        }
        getLog().info("Added the transcode-svg execution to " + pomFile
                + " (previous contents saved as " + backup.getName() + ").");
    }

    /**
     * Writes {@code content} over {@code target} in place, restoring
     * {@code backup} if the write fails part way through.
     *
     * <p>This used to write a sibling temporary file and move it into place,
     * for atomicity: a failure mid-write would otherwise leave a truncated pom
     * and a build that no longer starts. The trouble is that a move replaces
     * the inode, and the inode is what carries everything about the file that
     * is not its content. Each attribute had to be cloned back by hand, and
     * each one missed was a silent change to a file this build does not own --
     * the mode (a group-writable pom came back rw-r--r--), the symlink (a
     * shared pom quietly became an independent copy), then the owner and group
     * (a container running as root leaving the developer a root-owned pom),
     * with ACLs and extended attributes behind them. That list has no end, and
     * a miss is invisible until it matters.</p>
     *
     * <p>Writing in place preserves all of it by construction: same inode, so
     * same owner, group, mode, ACLs, extended attributes and hard links, and a
     * symlink is followed rather than replaced. What it gives up is atomicity,
     * and the backup taken moments earlier already covers that -- restored here
     * automatically, and named in the error if even that fails. A truncated
     * write needs the process to die between two syscalls on a file of a few
     * kilobytes; losing a pom's ownership happens on every successful repair in
     * a container.</p>
     */
    /** Test seam for {@link #writeInPlace}. */
    static void writeInPlaceForTest(File target, String content, Charset charset, File backup)
            throws IOException {
        writeInPlace(target, content, charset, backup,
                new org.apache.maven.plugin.logging.SystemStreamLog());
    }

    private static void writeInPlace(File target, String content, Charset charset,
            File backup, org.apache.maven.plugin.logging.Log log) throws IOException {
        try {
            FileUtils.writeStringToFile(target, content, charset);
        } catch (IOException ex) {
            try {
                FileUtils.copyFile(backup, target);
                log.warn("Writing " + target.getName() + " failed part way through; it has been "
                        + "restored from " + backup.getName() + ".");
            } catch (IOException restoreFailed) {
                log.error(target + " is incomplete and could not be restored automatically. "
                        + "Its previous contents are in " + backup.getName() + ".");
            }
            throw ex;
        }
    }

    /**
     * A backup path that does not already exist: {@code pom.xml.bak}, else
     * {@code pom.xml.bak.1} and upward. Null when none is free.
     *
     * <p>Copying onto {@code pom.xml.bak} unconditionally would destroy a
     * developer's own backup, or the only copy left by an earlier repair, and
     * an ordinary build is not allowed to do that. Never overwrite a file whose
     * whole purpose is to be the copy of last resort.</p>
     */
    static File unusedBackupFile(File pomFile) {
        File dir = pomFile.getParentFile();
        File candidate = new File(dir, pomFile.getName() + ".bak");
        if (!candidate.exists()) {
            return candidate;
        }
        for (int i = 1; i <= 100; i++) {
            candidate = new File(dir, pomFile.getName() + ".bak." + i);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The charset named in the document's XML declaration, {@code UTF-8} when
     * it names none, or null when it names one this JVM cannot provide.
     *
     * <p>The declaration is probed as ISO-8859-1, which maps every byte and so
     * never throws, and is ASCII-compatible with every encoding a pom is
     * realistically written in -- enough to read the declaration itself
     * regardless of what it turns out to say.</p>
     */
    static Charset declaredXmlEncoding(byte[] bytes) {
        int probe = Math.min(bytes.length, 256);
        String head = new String(bytes, 0, probe, StandardCharsets.ISO_8859_1);
        Matcher m = XML_DECL_ENCODING.matcher(head);
        if (!m.find()) {
            return StandardCharsets.UTF_8;
        }
        String name = m.group(1).trim();
        try {
            return Charset.isSupported(name) ? Charset.forName(name) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final Pattern XML_DECL_ENCODING = Pattern.compile(
            "<\\?xml[^>]*?encoding\\s*=\\s*[\"']([^\"']+)[\"']");

    /**
     * Whether this pom declares the transcode-svg goal anywhere Maven would
     * read it: the build, or any profile's build, active or not.
     */
    static boolean pomDeclaresTranscodeSvg(String pom) {
        Model model;
        try {
            model = new MavenXpp3Reader().read(new StringReader(pom));
        } catch (Exception ex) {
            // Unparseable: insertTranscodeSvgExecution declines anyway, and the
            // edited document is validated before it is written.
            return false;
        }
        if (buildBindsTranscodeSvg(model.getBuild())) {
            return true;
        }
        if (model.getProfiles() != null) {
            for (org.apache.maven.model.Profile profile : model.getProfiles()) {
                if (buildBindsTranscodeSvg(profile.getBuild())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean buildBindsTranscodeSvg(org.apache.maven.model.BuildBase build) {
        if (build == null || build.getPlugins() == null) {
            return false;
        }
        for (org.apache.maven.model.Plugin p : build.getPlugins()) {
            if (!"codenameone-maven-plugin".equals(p.getArtifactId())) {
                continue;
            }
            for (org.apache.maven.model.PluginExecution e : p.getExecutions()) {
                if (e.getGoals() != null && e.getGoals().contains("transcode-svg")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The text edit itself, kept pure so it can be tested against real poms.
     *
     * <p>Returns the updated document, or null when this pom's shape is not
     * understood well enough to edit safely -- the caller then leaves the file
     * alone and tells the developer what to add by hand. The caller is also
     * responsible for checking that the execution is not already present and
     * for re-parsing the result before writing it.</p>
     */
    static String insertTranscodeSvgExecution(String pom) {
        int marker = pom.indexOf("<artifactId>codenameone-maven-plugin</artifactId>");
        if (marker < 0) {
            return null;
        }
        int pluginStart = pom.lastIndexOf("<plugin>", marker);
        int pluginEnd = pom.indexOf("</plugin>", marker);
        if (pluginStart < 0 || pluginEnd < 0) {
            return null;
        }
        String eol = pom.indexOf("\r\n") >= 0 ? "\r\n" : "\n";
        int[] executions = findExecutionsTag(pom, pluginStart, pluginEnd);
        if (executions != null) {
            int tagStart = executions[0];
            int tagClose = executions[1];
            boolean selfClosing = executions[2] == 1;
            String outerIndent = indentOfLineAt(pom, tagStart);
            String indent = outerIndent + "    ";
            if (selfClosing) {
                // <executions/> has no content to insert into, so expand it into
                // a real element. Appending a second <executions> sibling instead
                // is well-formed XML that Maven rejects outright with
                // "Duplicated tag: 'executions'", which would leave the project
                // unbuildable until its backup was restored.
                String openTag = trimTrailingWhitespace(pom.substring(tagStart, tagClose - 1)) + ">";
                return pom.substring(0, tagStart)
                        + openTag + eol
                        + executionBlock(indent, eol) + eol
                        + outerIndent + "</executions>"
                        + pom.substring(tagClose + 1);
            }
            // The text right after <executions> already starts with a line
            // break, so the block must not carry a trailing one of its own.
            int insertAt = tagClose + 1;
            return pom.substring(0, insertAt)
                    + eol + executionBlock(indent, eol)
                    + pom.substring(insertAt);
        }
        // The plugin is declared without any executions -- wrap ours in a
        // new <executions> element just before </plugin>.
        String indent = indentOfLineAt(pom, pluginEnd);
        return pom.substring(0, pluginEnd)
                + "<executions>" + eol
                + executionBlock(indent + "        ", eol) + eol
                + indent + "</executions>" + eol
                + indent
                + pom.substring(pluginEnd);
    }

    /**
     * Locates the plugin's {@code executions} element between {@code from} and
     * {@code to}, in any of the spellings a pom may legally use:
     * {@code <executions>}, {@code <executions/>}, {@code <executions />} and
     * any of those carrying attributes such as
     * {@code combine.children="append"}.
     *
     * <p>Returns {@code {tagStart, indexOfClosingAngleBracket, selfClosing}} or
     * null when there is none. An exact search for the literal
     * {@code "<executions>"} missed every form but the first and fell through to
     * appending a second element, which Maven refuses to parse. A {@code >}
     * inside a quoted attribute value would still fool this scan; that is what
     * the model-parser check on the finished document is for.</p>
     */
    private static int[] findExecutionsTag(String pom, int from, int to) {
        int i = from;
        while (true) {
            int tagStart = pom.indexOf("<executions", i);
            if (tagStart < 0 || tagStart >= to) {
                return null;
            }
            int afterName = tagStart + "<executions".length();
            char next = afterName < pom.length() ? pom.charAt(afterName) : '\0';
            // Only a complete tag name counts -- not <executionsSomething>.
            if (next == '>' || next == '/' || next == ' ' || next == '\t'
                    || next == '\r' || next == '\n') {
                int close = pom.indexOf('>', afterName);
                if (close < 0 || close >= to) {
                    return null;
                }
                return new int[]{tagStart, close, pom.charAt(close - 1) == '/' ? 1 : 0};
            }
            i = afterName;
        }
    }

    private static String trimTrailingWhitespace(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * Reads the edited document with Maven's own model parser and confirms the
     * goal is really bound.
     *
     * <p>A DOM well-formedness check is not enough, which is the whole lesson
     * here: two sibling {@code <executions>} elements are perfectly well-formed
     * XML and Maven still rejects the file with "Duplicated tag". Parsing with
     * the parser that will actually read this pom catches that and every other
     * structural mistake, and asking whether the goal came out bound catches an
     * edit that parsed but landed somewhere useless.</p>
     */
    private static boolean parsesAndBindsTranscodeSvg(String pom) {
        Model model;
        try {
            model = new MavenXpp3Reader().read(new StringReader(pom));
        } catch (Exception ex) {
            return false;
        }
        return buildBindsTranscodeSvg(model.getBuild());
    }

    private static String executionBlock(String indent, String eol) {
        return indent + "<execution>" + eol
                + indent + "    <!-- Added automatically: this project has SVG/Lottie" + eol
                + indent + "         assets, and without this execution they compile to" + eol
                + indent + "         blank 1x1 placeholders on the device. -->" + eol
                + indent + "    <id>transcode-svg</id>" + eol
                + indent + "    <phase>generate-sources</phase>" + eol
                + indent + "    <goals>" + eol
                + indent + "        <goal>transcode-svg</goal>" + eol
                + indent + "    </goals>" + eol
                + indent + "</execution>";
    }

    /** The leading whitespace of the line containing {@code pos}. */
    private static String indentOfLineAt(String text, int pos) {
        int lineStart = text.lastIndexOf('\n', pos) + 1;
        int i = lineStart;
        while (i < pos && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return text.substring(lineStart, i);
    }

    private void warnCouldNotEditPom(String why) {
        getLog().warn("Could not add the transcode-svg execution to this project's pom because "
                + why + ". This build transcoded the SVGs anyway, but add the execution "
                + "yourself so later builds do the same:");
        getLog().warn("    <execution>");
        getLog().warn("        <id>transcode-svg</id>");
        getLog().warn("        <phase>generate-sources</phase>");
        getLog().warn("        <goals><goal>transcode-svg</goal></goals>");
        getLog().warn("    </execution>");
    }

    /**
     * Name of the resource {@code BuildHintAnnotationProcessor} emits into
     * {@code target/classes} at PROCESS_CLASSES.
     */
    private static final String ANNOTATION_HINTS_RESOURCE =
            "META-INF/codenameone/build-hints.properties";

    /**
     * Overlays the build hints that came from annotations onto the settings the
     * build request is assembled from.
     *
     * <p>Read from the compile classpath rather than from the staged fat jar.
     * {@code mergeJars} builds that jar with Ant's {@code Zip} in update mode,
     * which adds and overwrites entries but never removes one, and the jar is
     * reused when it is not stale &mdash; so a project that had its annotations
     * deleted would keep shipping yesterday's hints. The classpath directory is
     * written by the annotation processor on every build and deleted by it when
     * the last annotation goes away, so it always reflects the current source.</p>
     */
    protected void mergeAnnotationBuildHints(Properties target, List<String> classpathElements)
            throws MojoFailureException {
        if (target == null || classpathElements == null) {
            return;
        }
        String expectedMain = null;
        if (properties != null) {
            String main = properties.getProperty("codename1.mainName");
            String pkg = properties.getProperty("codename1.packageName");
            if (main != null && main.trim().length() > 0) {
                expectedMain = (pkg == null || pkg.trim().length() == 0)
                        ? main.trim() : pkg.trim() + "." + main.trim();
            }
        }
        // The manifest's presence, not its contents, is what proves the processor
        // ran. An annotation with every member left at its default -- @Ios() after
        // the last attribute was deleted -- is legal Java, and the processor emits
        // a manifest carrying only the main-class stamp for it. Judging by the hint
        // count alone would read that as "never processed" and refuse a build that
        // is in fact perfectly configured.
        String stale = null;
        // The processor writes the manifest into the SAME directory it scanned,
        // so a manifest stamped for a main class was written beside that class.
        // An element carrying the stamp without the class is therefore left over
        // -- the shape is moving the application class from a platform module
        // into common without cleaning -- and project output comes before
        // dependencies on the classpath, so the leftover was found first, applied
        // its old values and returned before the real one was ever read.
        //
        // Ordered rather than refused. Refusing assumes the two can never be
        // packaged apart, and an element that assembles resources from another
        // module would then lose its hints entirely; putting the colocated ones
        // first fixes the stale-wins bug and still falls back to a manifest that
        // is all there is.
        for (String element : colocatedFirst(classpathElements, expectedMain)) {
            Properties found = readAnnotationHints(new File(element));
            if (found == null) {
                continue;
            }
            String stamped = found.getProperty("cn1.buildHints.mainClass");
            if (expectedMain != null && !expectedMain.equals(stamped)) {
                // A cn1lib that bound the goal itself, a stale artifact, or a
                // file a project keeps in src/main/resources -- anything on the
                // classpath can carry this name. It has to say it was generated
                // for THIS main class: an unstamped one used to be accepted, and
                // then it both applied somebody else's hints and counted as
                // proof that the processor ran, which is what suppresses the
                // refusal below when the goal is not bound at all.
                getLog().debug("cn1: ignoring build hints from " + element
                        + " -- they were generated for " + stamped);
                continue;
            }
            if (found.getProperty(com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .SOURCE_DIGEST_KEY) == null) {
                // The processor always records one. A manifest without it was
                // written by something else.
                getLog().debug("cn1: ignoring build hints from " + element
                        + " -- no processor fingerprint");
                continue;
            }
            // The stamp says which class produced this file, not when. Nothing
            // clears target/classes between builds, so a project that ran the
            // processor once and then stopped -- goal unbound, skipped, or bound
            // to a phase that no longer runs -- keeps a manifest naming the right
            // class while the annotations beside it have changed. Comparing the
            // recorded fingerprint against the compiled class is what tells those
            // apart; without it the build silently ships the older values and the
            // guard below never runs.
            String mismatch = digestMismatch(new File(element), expectedMain, found, classpathElements);
            if (mismatch != null) {
                getLog().debug("cn1: ignoring build hints from " + element + " -- " + mismatch);
                stale = mismatch;
                continue;
            }
            int applied = 0;
            for (String key : found.stringPropertyNames()) {
                if (!key.startsWith("codename1.arg.")) {
                    continue;
                }
                // The processor refuses a hint declared as an annotation and as a
                // properties line, but it can only refuse the builds it runs in.
                // The fingerprint above covers the annotations and nothing else,
                // so with processing skipped a line added to the properties file
                // afterwards leaves a manifest that still matches -- and this
                // overlay would quietly replace the value the developer just
                // wrote, until the next clean build regenerated the manifest and
                // failed. Same declaration, same answer, whether or not
                // target/classes happened to be cleaned.
                String conflict = conflictingPropertiesDeclaration(target, key, found);
                if (conflict != null) {
                    throw new MojoFailureException(conflict);
                }
                if (overriddenOnTheCommandLine(key)) {
                    // -D wins, and it wins whichever SPELLING it used. The
                    // overlay below only replaces the same key, so
                    // -Dcodename1.arg.cn1.androidTheme against an annotated
                    // and.themeMode left both set -- and the two readers then
                    // disagreed with each other: JavaSEPort takes the canonical
                    // and falls back to the alias, so the annotation won in the
                    // simulator, while AndroidGradleBuilder writes both and the
                    // alias landed last, so -D won on the device. Same command
                    // line, opposite results.
                    getLog().debug("cn1: " + key + " comes from the command line, "
                            + "so the annotation value is not applied");
                    continue;
                }
                target.setProperty(key, found.getProperty(key));
                applied++;
            }
            // The FIRST accepted manifest is the answer, whether or not it set
            // anything. Continuing when it happened to apply nothing let a later
            // element decide instead: `@Ios()` with no members -- legal Java, and
            // what is left after the last attribute is deleted -- produces a
            // current manifest with no hint keys, and an older copy of the same
            // main class further down the classpath carries a manifest that
            // passes its own digest check, because it is fingerprinted against
            // the stale class sitting beside it. Its obsolete hints were then
            // applied over the authoritative empty one. The same is true of a
            // manifest whose every key is overridden on the command line.
            if (applied > 0) {
                getLog().info("cn1: applied " + applied + " build hint(s) from annotations");
            } else {
                getLog().debug("cn1: annotations were processed and set no build hint");
            }
            failOnMisplacedAnnotations(classpathElements, expectedMain);
            return;
        }
        // No manifest at all. If the compiled classes carry build hint
        // annotations anyway, the processor never ran -- a mojo's defaultPhase
        // does not add an execution to a project's POM, so an app that adopts the
        // annotations without binding process-annotations compiles cleanly and
        // ships with every annotated hint missing. Refuse rather than build that.
        String annotated = classCarryingBuildHintAnnotations(classpathElements, expectedMain);
        if (annotated != null) {
            throw new MojoFailureException(annotated + " carries build hint annotations, but "
                    + (stale == null
                        ? "no " + ANNOTATION_HINTS_RESOURCE + " was produced"
                        : "the only " + ANNOTATION_HINTS_RESOURCE + " on the classpath is left "
                          + "over from an earlier build (" + stale + ")")
                    + ", so none of them reached this "
                    + "build.\n\nThe cn1 process-annotations goal has to run on the module that "
                    + "compiles it:\n"
                    + "    <execution>\n"
                    + "      <id>cn1-process-classes</id>\n"
                    + "      <phase>process-classes</phase>\n"
                    + "      <goals>\n"
                    + "        <goal>process-annotations</goal>\n"
                    + "      </goals>\n"
                    + "    </execution>");
        }
    }

    /**
     * Refuses a build where a class other than the main one carries build hint
     * annotations.
     *
     * <p>Run even when a manifest was accepted, because accepting one does not
     * mean the processor ran THIS build: the fingerprint covers the main class,
     * so an annotation added to a live class beside it leaves the manifest
     * looking entirely current. With the goal unbound or skipped, the hints from
     * that class reach nothing and the build succeeds having neither applied
     * them nor said the annotation is in the wrong place -- the silent failure
     * this whole feature exists to remove. Had the processor run, it would have
     * refused the build for the same class.</p>
     */
    private void failOnMisplacedAnnotations(List<String> classpathElements, String expectedMain)
            throws MojoFailureException {
        if (expectedMain == null) {
            return;
        }
        java.util.Collection<String> descriptors = hintAnnotationDescriptors(classpathElements);
        for (String element : classpathElements) {
            String live = liveAnnotatedClass(new File(element), descriptors, expectedMain);
            if (live != null) {
                throw new MojoFailureException(live + " carries build hint annotations, but they "
                        + "are only read from the application's main class (" + expectedMain
                        + "), so none of them reached this build.\n\nMove them onto "
                        + expectedMain + ", or set those hints in "
                        + "codenameone_settings.properties.");
            }
        }
    }

    /**
     * The duplicate-declaration message for a hint set by an annotation and by a
     * properties line, or null when there is no clash.
     *
     * <p>Only reached when the processor did not run this build; when it did, it
     * has already failed for the same reason and with more to say -- it can point
     * at the offending line. An alias counts as the same setting, matching what
     * the processor checks, so declaring {@code and.captureRecord} in the file
     * still collides with {@code @Android(captureRecord)}.</p>
     */
    private String conflictingPropertiesDeclaration(Properties settings, String key,
                                                    Properties manifest) {
        String name = key.substring("codename1.arg.".length());
        java.util.Set<String> names = new java.util.LinkedHashSet<String>();
        names.add(name);
        for (com.codename1.build.shared.BuildHints.Hint h
                : com.codename1.build.shared.BuildHints.entries()) {
            if (name.equals(h.aliasOf())
                    || name.equals(com.codename1.build.shared.BuildHints.canonicalName(h.name()))) {
                names.add(h.name());
            }
        }
        for (String candidate : names) {
            String candidateKey = "codename1.arg." + candidate;
            String fromFile = settings.getProperty(candidateKey);
            if (fromFile == null) {
                continue;
            }
            String origin = manifest.getProperty("cn1.buildHints.origin." + name);
            return candidateKey + " is declared twice.\n"
                    + "    annotation : " + (origin == null ? "on the main class" : origin)
                    + " = " + manifest.getProperty(key) + "\n"
                    + "    properties : codenameone_settings.properties\n"
                    + "                 " + candidateKey + "=" + fromFile + "\n"
                    + "    A build hint has one source of truth. Delete the properties line and "
                    + "keep the annotation, or delete the annotation attribute and keep the line. "
                    + "(-D" + candidateKey + "=... overrides either and is not a conflict.)";
        }
        return null;
    }

    /**
     * {@code classpathElements}, with those containing {@code expectedMain}'s
     * class file first and the relative order otherwise preserved.
     *
     * @param classpathElements the compile classpath, in Maven's order
     * @param expectedMain the binary name of the app's main class, or null
     * @return the elements to search, colocated ones first
     */
    private List<String> colocatedFirst(List<String> classpathElements, String expectedMain) {
        if (expectedMain == null) {
            return classpathElements;
        }
        List<String> beside = new ArrayList<String>();
        List<String> rest = new ArrayList<String>();
        for (String element : classpathElements) {
            boolean here;
            try {
                here = readClass(new File(element), expectedMain) != null;
            } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
                // Unreadable is not evidence either way; leave it where it was.
                getLog().debug("cn1: could not look for " + expectedMain + " in " + element, ex);
                here = false;
            }
            (here ? beside : rest).add(element);
        }
        beside.addAll(rest);
        return beside;
    }

    /**
     * Why a manifest cannot have come from the class beside it, or null when it can.
     *
     * <p>Answered only when both halves are actually available: with no
     * {@code codename1.mainName}, no class file for it ANYWHERE on the classpath,
     * or no recorded fingerprint, there is nothing to compare and the manifest is
     * taken at face value -- the same as before this check existed. It refuses
     * only on positive evidence of a mismatch.</p>
     */
    private String digestMismatch(File element, String expectedMain, Properties manifest,
                                  List<String> classpathElements) {
        String recorded = manifest.getProperty(
                com.codename1.maven.processors.BuildHintAnnotationProcessor.SOURCE_DIGEST_KEY);
        if (expectedMain == null || recorded == null || recorded.length() == 0) {
            return null;
        }
        // Beside the manifest first, then anywhere on the classpath. Looking only
        // beside it answers "no evidence" for the layout that packages resources
        // apart from classes, and a manifest with no evidence against it is taken
        // at face value -- so a manifest left by a build that no longer runs the
        // processor applied its obsolete hints, and counted as proof the processor
        // ran, while the recompiled class sat in another element.
        com.codename1.maven.annotations.AnnotatedClass cls = classOnClasspath(element, expectedMain);
        if (cls == null) {
            for (String other : classpathElements) {
                cls = classOnClasspath(new File(other), expectedMain);
                if (cls != null) {
                    break;
                }
            }
        }
        if (cls == null) {
            return null;
        }
        try {
            String actual = com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .sourceDigest(cls);
            if (recorded.equals(actual)) {
                return null;
            }
            return "it was generated from a different set of annotations on "
                    + expectedMain + " than the one compiled onto the classpath";
        } catch (com.codename1.maven.annotations.ProcessingException ex) {
            // Unreadable is not evidence of staleness.
            getLog().debug("cn1: could not fingerprint " + expectedMain, ex);
            return null;
        }
    }

    /** {@code expectedMain} read out of {@code element}, or null when it is not there. */
    private com.codename1.maven.annotations.AnnotatedClass classOnClasspath(File element,
                                                                           String expectedMain) {
        try {
            return readClass(element, expectedMain);
        } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
            getLog().debug("cn1: could not look for " + expectedMain + " in " + element, ex);
            return null;
        }
    }

    /** Reads one compiled class out of a classpath directory or jar. */
    private com.codename1.maven.annotations.AnnotatedClass readClass(File element, String binaryName)
            throws IOException, com.codename1.maven.annotations.ProcessingException {
        String path = binaryName.replace('.', '/') + ".class";
        if (element.isDirectory()) {
            File f = new File(element, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                return null;
            }
            try (InputStream in = new FileInputStream(f)) {
                return com.codename1.maven.annotations.ClassScanner.readClass(in, f);
            }
        }
        if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    return com.codename1.maven.annotations.ClassScanner.readClass(in, element);
                }
            }
        }
        return null;
    }

    /**
     * The application class carrying a build hint annotation, or null.
     *
     * <p>Read straight out of the class file's annotation table rather than from
     * source, so it sees exactly what the compiler emitted.</p>
     *
     * <p>The MAIN class alone when the project names one. The processor honours
     * no other class, so no other class is evidence that annotations went
     * unprocessed &mdash; and scanning them all meant a stale annotated
     * {@code .class}, left behind by a rename without a clean, failed every build
     * with "no manifest was produced" for a class the developer had already
     * deleted. The processor ignores that orphan; so does this. Only when the
     * project names no main class at all does this fall back to scanning
     * everything, since then there is nothing more specific to ask about.</p>
     */
    private String classCarryingBuildHintAnnotations(List<String> classpathElements,
                                                     String expectedMain) {
        java.util.Collection<String> descriptors = hintAnnotationDescriptors(classpathElements);
        if (expectedMain != null) {
            for (String element : classpathElements) {
                try {
                    if (mainClassCarriesAnnotation(new File(element), expectedMain, descriptors)) {
                        return expectedMain;
                    }
                } catch (IOException | RuntimeException ex) {
                    getLog().debug("cn1: could not read " + expectedMain + " from " + element, ex);
                }
            }
            // The main class carries none. A LIVE class elsewhere still counts:
            // @Target(TYPE) accepts the placement, so without this the build
            // succeeds having neither applied the hint nor said the annotation is
            // in the wrong place -- which is the silent failure the whole feature
            // removes. It is only stale output that must not count, and that is a
            // question about the source, not about which class it is.
            //
            // Reported as a misplacement, because that is what it is: had the
            // processor run it would have refused the build for this class.
            for (String element : classpathElements) {
                String live = liveAnnotatedClass(new File(element), descriptors);
                if (live != null) {
                    return live;
                }
            }
            return null;
        }
        // A reactor `package` build hands us the dependency module's jar rather
        // than its output directory, which findAnnotatedClasses handles alongside
        // a directory -- that is exactly the shape this check has to work in.
        for (String element : classpathElements) {
            String hit = findAnnotatedClass(new File(element), descriptors);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * The first annotated class in this element whose source still exists, or null.
     *
     * <p>Stale output is excluded the same way the processor excludes it &mdash;
     * by asking whether the module's configured source roots still declare the
     * class &mdash; so an orphan left by a rename cannot fail the build, while a
     * class the developer actually wrote does.</p>
     */
    private String liveAnnotatedClass(File element, java.util.Collection<String> descriptors) {
        return liveAnnotatedClass(element, descriptors, null);
    }

    /// As above, ignoring `exclude` -- the class the manifest was generated for,
    /// which carrying annotations is the whole point of.
    private String liveAnnotatedClass(File element, java.util.Collection<String> descriptors,
                                      String exclude) {
        List<String> roots;
        String encoding;
        try {
            // The module that PRODUCED this element, not the one running. In the
            // generated layout the application's classes come from `common`
            // while a platform module runs the build, so asking the running
            // project where its sources are answered for the wrong module: every
            // class compiled from `common` had no backing source, read as stale,
            // and its misplaced annotation went unreported -- which is exactly
            // the silence this check exists to break.
            //
            // The complete set, not only what getCompileSourceRoots lists: the
            // Kotlin plugin compiles its own sourceDirs without adding them
            // back, and this list is used to decide that a source is ABSENT.
            org.apache.maven.project.MavenProject owner = moduleProducing(element);
            org.apache.maven.project.MavenProject module = owner == null ? project : owner;
            roots = compileSourceRoots(module, userProperties());
            // The charset that module compiles with, for the same reason
            // ProcessAnnotationsMojo is given it: the scan below decides a source
            // is ABSENT by reading it, and the single-byte encodings all decode
            // every byte into DIFFERENT characters. Without it a name outside
            // ASCII is unjudgeable and the class is kept -- which is the safe
            // direction, but it keeps a genuinely deleted class forever and fails
            // its placement check on every incremental build. This caller knows
            // the module, so there is no reason for it to be the one guessing.
            encoding = sourceEncodingOf(module, userProperties());
        } catch (RuntimeException ex) {
            return null;
        }
        if (roots == null || roots.isEmpty()) {
            // Not told where the sources are, so staleness cannot be judged and
            // an orphan would fail the build. Silence is the lesser harm: the
            // processor still refuses this placement whenever it runs.
            return null;
        }
        // Every candidate, not the first: rejecting one stale class must not end
        // the search, or whether a live misplacement is reported depends on the
        // order the directory happened to be listed in.
        for (String hit : findAnnotatedClasses(element, descriptors)) {
            if (exclude != null && exclude.equals(hit)) {
                continue;
            }
            try {
                com.codename1.maven.annotations.AnnotatedClass cls = readClass(element, hit);
                if (cls != null && com.codename1.maven.processors.BuildHintAnnotationProcessor
                        .hasBackingSource(cls, roots, encoding)) {
                    return hit;
                }
            } catch (IOException | com.codename1.maven.annotations.ProcessingException ex) {
                getLog().debug("cn1: could not read " + hit + " from " + element, ex);
            }
        }
        return null;
    }

    /**
     * Whether {@code -D} already sets this hint, under any spelling.
     *
     * <p>An alias and its target are ONE effective setting -- the builder reads
     * {@code android.captureRecord} and then lets {@code and.captureRecord}
     * override it -- so a command line that names either of them is setting the
     * hint an annotation would otherwise supply. Comparing the keys literally
     * missed that, and the documented rule is that {@code -D} overrides both the
     * annotation and the properties file.</p>
     */
    private boolean overriddenOnTheCommandLine(String key) {
        return overriddenOnTheCommandLine(key,
                getSession() == null ? null : getSession().getUserProperties());
    }

    static boolean overriddenOnTheCommandLine(String key, Properties user) {
        if (user == null) {
            return false;
        }
        String canonical = com.codename1.build.shared.BuildHints.canonicalName(
                com.codename1.build.shared.BuildHints.strip(key));
        for (String name : user.stringPropertyNames()) {
            if (!name.startsWith(com.codename1.build.shared.BuildHints.ARG_PREFIX)) {
                continue;
            }
            String other = com.codename1.build.shared.BuildHints.canonicalName(
                    com.codename1.build.shared.BuildHints.strip(name));
            if (canonical.equals(other)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the named class, read from this classpath element, is annotated. */
    private boolean mainClassCarriesAnnotation(File element, String binaryName,
                                               java.util.Collection<String> descriptors)
            throws IOException {
        String path = binaryName.replace('.', '/') + ".class";
        if (element.isDirectory()) {
            File f = new File(element, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                return false;
            }
            try (InputStream in = new FileInputStream(f)) {
                return carriesBuildHintAnnotation(in, descriptors);
            }
        }
        if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    return false;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    return carriesBuildHintAnnotation(in, descriptors);
                }
            }
        }
        return false;
    }


    private boolean carriesBuildHintAnnotation(InputStream in,
                                               java.util.Collection<String> descriptors)
            throws IOException {
        final boolean[] seen = {false};
        new org.objectweb.asm.ClassReader(in).accept(
                new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(
                            String desc, boolean visible) {
                        if (descriptors.contains(desc)) {
                            seen[0] = true;
                        }
                        return null;
                    }
                },
                org.objectweb.asm.ClassReader.SKIP_CODE
                        | org.objectweb.asm.ClassReader.SKIP_DEBUG
                        | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        return seen[0];
    }

    private String findAnnotatedClass(File dir, java.util.Collection<String> descriptors) {
        List<String> all = findAnnotatedClasses(dir, descriptors);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Every annotated class under this element, by BINARY name.
     *
     * <p>The binary name, not the file's own: returning {@code Wrong} for
     * {@code com/example/Wrong.class} made the message name a class that does not
     * exist, and made re-reading it by name fail, so the guard saw nothing.</p>
     *
     * <p>All of them, not the first: an incremental output directory can hold a
     * stale annotated class and a live one at once, and stopping at whichever
     * {@code File.listFiles} returned first made the answer depend on directory
     * order.</p>
     */
    private List<String> findAnnotatedClasses(File element, java.util.Collection<String> descriptors) {
        List<String> out = new ArrayList<String>();
        if (element.isDirectory()) {
            collectAnnotatedClasses(element, element, descriptors, out);
        } else if (element.isFile() && element.getName().endsWith(".jar")) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        if (carriesBuildHintAnnotation(in, descriptors)) {
                            out.add(entry.getName()
                                    .substring(0, entry.getName().length() - ".class".length())
                                    .replace('/', '.'));
                        }
                    }
                }
            } catch (IOException | RuntimeException ex) {
                getLog().debug("cn1: could not scan " + element + ": " + ex.getMessage());
            }
        }
        return out;
    }

    private void collectAnnotatedClasses(File root, File dir,
                                         java.util.Collection<String> descriptors,
                                         List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectAnnotatedClasses(root, f, descriptors, out);
                continue;
            }
            if (!f.getName().endsWith(".class")) {
                continue;
            }
            try (InputStream in = new FileInputStream(f)) {
                if (carriesBuildHintAnnotation(in, descriptors)) {
                    String rel = f.getAbsolutePath()
                            .substring(root.getAbsolutePath().length())
                            .replace(File.separatorChar, '/');
                    while (rel.startsWith("/")) {
                        rel = rel.substring(1);
                    }
                    out.add(rel.substring(0, rel.length() - ".class".length()).replace('/', '.'));
                }
            } catch (IOException | RuntimeException ex) {
                getLog().debug("cn1: could not scan " + f + ": " + ex.getMessage());
            }
        }
    }

    /// The build hint annotation types, read off the classpath they live on.
    ///
    /// The package is enumerated rather than listed: a generated table naming
    /// each annotation was a second statement of which ones exist, and it went
    /// stale the moment one was added without regenerating it.
    private java.util.Collection<String> hintAnnotationDescriptors(
            List<String> classpathElements) {
        try {
            return com.codename1.build.shared.BuildHintAnnotationReader
                    .bindingsFromClasspath(classpathElements).descriptors();
        } catch (IOException ex) {
            getLog().debug("cn1: could not read the build hint annotations", ex);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Reads the emitted hints out of a classpath element, which is either the
     * module's output directory or a jar.
     *
     * @return the properties, or null when this element carries none
     */
    private Properties readAnnotationHints(File element) {
        if (element == null || !element.exists()) {
            return null;
        }
        try {
            if (element.isDirectory()) {
                File f = new File(element, ANNOTATION_HINTS_RESOURCE);
                if (!f.isFile()) {
                    return null;
                }
                try (FileInputStream in = new FileInputStream(f)) {
                    Properties p = new Properties();
                    p.load(in);
                    return p;
                }
            }
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element)) {
                java.util.zip.ZipEntry entry = zip.getEntry(ANNOTATION_HINTS_RESOURCE);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Properties p = new Properties();
                    p.load(in);
                    return p;
                }
            }
        } catch (IOException ex) {
            getLog().warn("cn1: could not read build hints from " + element + ": "
                    + ex.getMessage());
            return null;
        }
    }
}
