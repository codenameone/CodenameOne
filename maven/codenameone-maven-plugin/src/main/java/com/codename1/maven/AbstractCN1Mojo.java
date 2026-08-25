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
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.doxia.logging.Log;
import org.apache.maven.model.Dependency;
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
        if (contains(project.getBasedir().getName(), "javase", "javascript", "android", "ios", "win", "linux")) {
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
        if (configured != null) {
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
                if (!thisOne && ("default-" + goal).equals(execution.getId())) {
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
            boolean disabled = false;
            if (plugin.getExecutions() != null) {
                for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                    if (!bindsCompile(execution)) {
                        continue;
                    }
                    if (isDisabled(execution)) {
                        disabled = true;
                    } else {
                        return true;
                    }
                }
            }
            // The lifecycle binding is what <extensions>true</extensions> buys,
            // and a POM switches THAT off the same way it switches off any
            // inherited execution -- <id>default-compile</id><phase>none</phase>.
            // Returning true on extensions alone claimed src/main/kotlin for a
            // module whose Kotlin compilation is explicitly disabled.
            if (plugin.isExtensions() && !disabled) {
                return true;
            }
        }
        return false;
    }

    /// Whether `execution` binds the `compile` goal, disabled or not.
    private static boolean bindsCompile(org.apache.maven.model.PluginExecution execution) {
        return (execution.getGoals() != null && execution.getGoals().contains("compile"))
                || "default-compile".equals(execution.getId());
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
                    : configurationsFor(plugin, "compile", "sourceDirs", plugin.isExtensions())) {
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
                    : configurationsFor(plugin, "compile", "sourceDirs", plugin.isExtensions())) {
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

}
    
