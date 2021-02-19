/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.codehaus.plexus.util.FileUtils;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates a legacy .cn1lib file.
 * @author shannah
 */
@Mojo(name = "cn1lib", defaultPhase = LifecyclePhase.PACKAGE)
public final class Cn1libMojo extends AbstractCN1Mojo {


    private File getNativeDir() {
        return new File(getCN1LibProjectDir(), "native");
    }

    private File getBuildDir() {
        return new File(path(project.getBuild().getDirectory(), "codenameone", "cn1lib"));
    }

    private File getFinalCn1lib() {
        return new File(path(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".cn1lib"));
    }


    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        
        if (!isCN1ProjectDir()) {
            return;
        }

        try {
            buildCn1lib();
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to create CN1lib", ex);
        }

        projectHelper.attachArtifact(project, "cn1lib", getFinalCn1lib());
        
    }
    
    private String str(String str) {
        return str == null ? "" : str;
    }
    
    private boolean isSelf(Plugin plugin) {
        return plugin.getArtifactId().equals(ARTIFACT_ID) && plugin.getGroupId().equals(GROUP_ID);
    }
    
    private Plugin getSelf() {
        for (Plugin p : this.project.getBuildPlugins()) {
            if (isSelf(p)) {
                return p;
            }
        }
        return null;
    }
    
    
    
    /*
    private Dependency findPluginDependency(String groupId, String artifactId) {

        for (Dependency d : getSelf().getDependencies()) {
            getLog().info("Checking dep "+d);
            if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        return null;
    }
    
    private Dependency findStubberDependency() {
        return findPluginDependency(GROUP_ID, STUBBER_ARTIFACT_ID);
    }
    */


    /**
     * Finds the Stubber jar.
     * @return
     */
    private File findStubberJar() {
        File mavenPluginJar = getJar("com.codenameone", "codenameone-maven-plugin");
        if (mavenPluginJar == null || !mavenPluginJar.exists()) {
            throw new RuntimeException("Cannot find codenameone-maven-plugin jar");
        }

        File stubberJar = new File(mavenPluginJar.getParentFile(), "Stubber.jar");
        if (!stubberJar.exists() || mavenPluginJar.lastModified() > stubberJar.lastModified()) {
            // Stubber needs to be updated
            try {
                FileUtils.copyURLToFile(getClass().getResource("/Stubber.jar"), stubberJar);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to copy Stubber.jar from resource stream to file at "+stubberJar);
            }
        }
        return stubberJar;
    }
    

    private File getStubsBuildDir() {
        return new File(getBuildDir(), "stubs");
    }

    private File getStubsZip() {
        return new File(getBuildDir(), "stubs.zip");
    }



    private void buildStubs() throws IOException {
        getLog().debug("Found stubber "+findStubberJar());
        getStubsBuildDir().mkdirs();
        File javaSourcesDir = new File(project.getBasedir(), path("src", "main", "java"));
        if (getStubsZip().exists() && getStubsZip().lastModified() >= lastModifiedRecursive(javaSourcesDir)) {
            getLog().debug("Stubs have not changed.  Skipping stubber");
            return;
        }

        FileUtils.deleteDirectory(getStubsBuildDir());
        getStubsBuildDir().mkdir();
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", getBuildDir().getAbsolutePath());
        try {
            Javadoc javadoc = (Javadoc) antProject.createTask("javadoc");
            Path sourcePath = new Path(antProject, javaSourcesDir.getAbsolutePath());
            javadoc.setSourcepath(sourcePath);
            Path classPath = new Path(antProject);
            for (Artifact artifact : project.getArtifacts()) {
                classPath.add(new Path(antProject, artifact.getFile().getAbsolutePath()));
            }
            javadoc.setClasspath(classPath);
            javadoc.setDocletPath(new Path(antProject, findStubberJar().getAbsolutePath()));
            javadoc.setDoclet("com.codename1.build.client.StubGenerator");

            FileSet fileset = new FileSet();
            fileset.setProject(antProject);

            //getLog().info("dir="+project.getCompileSourceRoots().get(0));
            fileset.setDir(javaSourcesDir);

            getLog().debug("includes=" + stubberIncludes + "; excludes=" + stubberExcludes);

            fileset.setExcludes("*.java," + str(stubberExcludes));
            fileset.setIncludes(str(stubberIncludes));

            FilenameSelector javaFiles = new FilenameSelector();
            javaFiles.setName("**/*.java");

            fileset.addFilename(javaFiles);
            javadoc.addFileset(fileset);

            javadoc.execute();
        } finally {

            System.setProperty("user.dir", userDir);
        }

        Zip zip = (Zip)antProject.createTask("zip");
        zip.setDestFile(getStubsZip());
        FileSet files = new FileSet();
        files.setProject(antProject);
        files.setDir(getStubsBuildDir());
        files.setIncludes("**");
        zip.addFileset(files);
        zip.execute();

        FileUtils.deleteDirectory(getStubsBuildDir());

                        
    }
    
    private void buildMainZip() {
        getBuildDir().mkdirs();
        {
            Zip zip = (Zip)antProject.createTask("zip");
            File mainZip = new File(getBuildDir(), "main.zip");
            zip.setDestFile(mainZip);
            {
                FileSet files = new FileSet();
                files.setProject(antProject);
                files.setDir(new File(this.project.getBuild().getOutputDirectory()));
                files.setIncludes("**");
                zip.addFileset(files);
            }
            zip.execute();
        }
        
    }

    private File getModuleProject(String name) {
        return new File(project.getParent().getBasedir(), name);
    }

    private File[] getIOSPaths() {
        return new File[]{
                new File(getModuleProject("ios"), path("src", "main", "objectivec")),
                new File(getModuleProject("ios"), path("src", "main", "resources"))
        };
    }

    private static boolean isDirectoryEmpty(File directory) {
        String[] files = directory.list();
        return files.length == 0;
    }

    private void buildZip(String basename, File[] paths) {
        File zipFile = new File(getBuildDir(), basename + ".zip");
        Zip zip = (Zip)antProject.createTask("zip");
        zip.setDestFile(zipFile);
        //boolean empty = true;
        for (File dir : paths) {
            FileSet fs = new FileSet();
            fs.setProject(antProject);
            fs.setDir(dir);
            fs.setIncludes("**");
            //if (dir.exists() && !isDirectoryEmpty(dir)) {
            //    empty = false;
            //}
            zip.addFileset(fs);
        }
        //if (empty) {
        //    getLog().debug("No directories found for "+basename+" zip file.  Skipping");
        //}
        zip.execute();
    }

    private void buildIOS() {
        buildZip("nativeios", getIOSPaths());
    }

    private File[] getAndroidPaths() {
        return new File[]{
                new File(getModuleProject("android"), path("src", "main", "java")),
                new File(getModuleProject("android"), path("src", "main", "resources"))
        };
    }

    private void buildAndroid() {
        buildZip("nativeand", getAndroidPaths());
    }

    private File[] getJavascriptPaths() {
        return new File[]{
                new File(getModuleProject("javascript"), path("src", "main", "javascript")),
                new File(getModuleProject("javascript"), path("src", "main", "resources"))
        };
    }

    private void buildJavascript() {
        buildZip("nativejavascript", getJavascriptPaths());
    }

    private File[] getJavasePaths() {
        return new File[]{
                new File(getModuleProject("javase"), path("src", "main", "java")),
                new File(getModuleProject("javase"), path("src", "main", "resources"))
        };
    }

    private void buildJavase() {
        buildZip("nativese", getJavasePaths());
    }

    private File[] getWinPaths() {
        return new File[]{
                new File(getModuleProject("win"), path("src", "main", "csharp")),
                new File(getModuleProject("win"), path("src", "main", "resources"))
        };
    }

    private void buildWin() {
        buildZip("nativewin", getWinPaths());
    }


    private File[] getCSSPaths() {
        return new File[]{
                new File(project.getBasedir(), path("src", "main", "css"))
        };
    }

    private void buildCSS() {
        buildZip("css", getCSSPaths());
    }



    private void buildCn1lib() throws IOException {
        buildMainZip();
        buildCSS();
        buildAndroid();
        buildIOS();
        buildJavascript();
        buildJavase();
        buildStubs();
        buildWin();

        Zip zip = (Zip)antProject.createTask("zip");
        zip.setDestFile(getFinalCn1lib());
        {
            FileSet files = new FileSet();
            files.setProject(antProject);
            files.setDir(getBuildDir());
            files.setIncludes("**");
            zip.addFileset(files);
        }
        {
            FileSet files = new FileSet();
            files.setProject(antProject);
            files.setDir(project.getBasedir());
            files.setIncludes("*.properties");
            zip.addFileset(files);
        }
        zip.execute();
    }
    

    
    
    
    

}
