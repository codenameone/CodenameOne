/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import static com.codename1.maven.ProjectUtil.wrap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.types.FileSet;

/**
 *
 * @author shannah
 */
public abstract class AbstractCN1Mojo extends AbstractMojo {
    
    protected static final String GROUP_ID="com.codenameone";
    protected static final String JAVA_RUNTIME_ARTIFACT_ID = "java-runtime";
    protected static final String ARTIFACT_ID="codenameone-maven-plugin";
    
    
    @Component
    protected MavenProjectHelper projectHelper;
    
    @Component
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
    
    protected Properties properties;

    
    private void setupAnt()  throws MojoExecutionException, MojoFailureException {
        
        antProject = new Project();
        antProject.setBaseDir(project.getBasedir());
        antProject.setDefaultInputStream(System.in);
        
        InputHandler handler = new DefaultInputHandler();
        antProject.setProjectReference(handler);
        antProject.setInputHandler(handler);
        
        
        antProject.init();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getCN1ProjectDir() != null) {
            properties = new Properties();
            try {
                properties.load(new FileInputStream(new File(getCN1ProjectDir(), "codenameone_settings.properties")));
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to find codenameone_settings.properties file.", ex);
            }
        } else {
            getLog().warn("Failed to find CN1 Project directory.  codenameone_settings.properties will not be loaded");
            getLog().warn("Checking from project root and source compile root: "+project.getCompileSourceRoots().get(0));
        }
        
        
        setupAnt();
        executeImpl();
    }
    
    protected abstract void executeImpl()  throws MojoExecutionException, MojoFailureException;
    
    
    protected File getCN1ProjectDir() {
        File f = getCN1ProjectDir(project.getBasedir());
        if (f != null) return f;
        f = getCN1ProjectDir(new File(project.getCompileSourceRoots().get(0)).getParentFile());
        return f;
        
    }
    
    private File getCN1ProjectDir(File start) {
        File f = new File(start, "codenameone_settings.properties");
        
        while (!f.exists() && f.getParentFile().getParentFile() != null) {
            f = new File(f.getParentFile().getParentFile(), "codenameone_settings.properties");
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
                            case Log.LEVEL_INFO:
                                getLog().info(output);
                                break;
                            case Log.LEVEL_WARN:
                                getLog().warn(output);
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
                    case Log.LEVEL_INFO:
                        getLog().info(output);
                        break;
                    case Log.LEVEL_WARN:
                        getLog().warn(output);
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
                    case Log.LEVEL_INFO:
                        getLog().info(output);
                        break;
                    case Log.LEVEL_WARN:
                        getLog().warn(output);
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
        
        
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                
        .setLocalRepository(localRepository)
        .setRemoteRepositories(new ArrayList<>(remoteRepositories))
        .setResolveTransitively(true)
        .setArtifact(artifact));

        if (result.isSuccess()) {
            out[0] = artifact.getFile().getAbsoluteFile();
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
    public static final String JPDATE_CODENAMEONE_JAR_PATH = System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "UpdateCodenameOne.jar";

    
    protected void installUpdater() throws IOException {
        String destinationPath = JPDATE_CODENAMEONE_JAR_PATH;
        FileOutputStream os = null;
        InputStream is = null;
        try {
            File re = new File(destinationPath);

            if (!re.exists()) {
                getLog().info("Installing Codename One Updater from "+UPDATE_CODENAMEONE_JAR_URL);
                is = new URL(UPDATE_CODENAMEONE_JAR_URL).openStream();
                os = new FileOutputStream(re);
                byte[] buf = new byte[65536];
                int len = 0;
                while ((len = is.read(buf)) > -1) {
                    os.write(buf, 0, len);
                }
            } else {
                getLog().debug("Designer is up to date");
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    getLog().error("Error closing output stream", ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    getLog().error("Error closing input stream", ex);
                }
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
        java.createArg().setFile(getCN1ProjectDir());
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
   
}
    
