/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FilenameSelector;

/**
 *
 * @author shannah
 */
@Mojo(name = "cn1lib", defaultPhase = LifecyclePhase.PACKAGE)
public final class Cn1libMojo extends AbstractCN1Mojo {
    
    public static final String CN1LIB_VERSION = "1.0";

    
    private static final String STUBBER_ARTIFACT_ID = "stubber";

    private static final String[] NATIVE_TYPES = new String[]{"rim", "j2me", "javase", "javascript", "ios", "android", "win"};
   
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        
        
        
        File artifact = new File(this.outputDirectory +"/"+ this.finalName + ".jar");
        
        
        nativeDir = new File(getCN1LibProjectDir(), "native");
        buildDir = new File(this.project.getBuild().getDirectory(), "tmp");
        //File stubsDir = new File(buildDir, "stubs");
        File lib = new File(buildDir, "lib");
        File metaInf = new File(lib, "META-INF");
        File metaCn1lib = new File(metaInf, "cn1lib");
        metaCn1lib.mkdirs();
        buildMain(lib);
        for (String file : new String[]{"manifest.properties", "codenameone_library_appended.properties", "codenameone_library_required.properties"}) {
            Copy copy = new Copy();
            copy.setFile(new File(getCN1LibProjectDir(), file));
            copy.setTodir(metaCn1lib);
            copy.execute();
        }
        
        
        
        
        
        buildNatives(metaCn1lib);
        nativeDir = getCN1LibProjectDir();
        buildNative(metaCn1lib, "css", "");
        
        
        stubber();
        
        
        Manifest manifest = new Manifest();
        Attributes cn1Attributes = manifest.getAttributes("cn1lib");
        if (cn1Attributes == null) {
            cn1Attributes = new Attributes();
            manifest.getEntries().put("cn1lib", cn1Attributes);
        }
        cn1Attributes.putValue("Version", CN1LIB_VERSION);
        
        File manifestFile = new File(metaInf, "MANIFEST.MF");
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            manifest.write(fos);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to create manifest file", ex);
        }
        
        Zip zip = (Zip)antProject.createTask("zip");
        
        zip.setBasedir(lib);
        zip.setCompress(true);
        zip.setDestFile(artifact);
        zip.execute();
        this.project.getArtifact().setFile(artifact);
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
    
    
    private Artifact findStubberArtifact() {
        //Dependency dep = findStubberDependency();
        //getLog().info("Stubber dep: "+dep);
        Artifact art = repositorySystem.createPluginArtifact(getSelf());
        art.setArtifactId(STUBBER_ARTIFACT_ID);
        art.setGroupId(GROUP_ID);
        art.setVersion("6.0.0");
        return art;
        
        
    }
    
    private File findStubberJar() {
        File[] out = new File[1];
        Artifact artifact = findStubberArtifact();
        
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
    
   private Path prepareStubberClassPath() {
        Log log = getLog();
        log.info("Preparing classpath for Stubber");
        List<String> paths = new ArrayList<>();
        //StringBuilder classpath = new StringBuilder();
        Path classpath = new Path(antProject);
        
        for (Artifact artifact : project.getDependencyArtifacts()) {
            log.info("Checking artifact "+artifact);
            if (!filterByScope(artifact)) {
                continue;
            }
            File file = artifact.getFile();
            //if (classpath.length() > 0) {
            //    classpath.append(':');
            //}
            classpath.add(new Path(antProject, file.getAbsolutePath()));
            //classpath.append(file.getPath());
            //paths.add(file.getAbsolutePath());
        }
        //if (classpath.length() > 0) {
        //    classpath.append(':');
        //}
        //classpath.append(classFiles.getPath());
        //paths.add(classFiles.getAbsolutePath());
        log.info("Using the following classpath for Stubber: " + classpath);
        return classpath;
    }
   private boolean filterByScope(Artifact artifact) {
        return isSupportedScope(artifact.getScope());
                
    }

    protected boolean isSupportedScope(String scope) {
        switch (scope) {
            case Artifact.SCOPE_COMPILE:
            case Artifact.SCOPE_PROVIDED:
            case Artifact.SCOPE_SYSTEM:
                return true;
            default:
                return false;
        }
    }
    
    private void stubber() {
        getLog().debug("Found stubber "+findStubberJar());
        
        File srcDir = new File(project.getCompileSourceRoots().get(0));
        File antBuildDir = new File(srcDir.getParentFile(), "build");
        boolean antBuildDirExists = antBuildDir.exists();
        File stubDir = new File(antBuildDir, "stubs");
        try {
            //File stubDir = new File(new File(project.getCmo), "stubs");
            //stubDir.mkdirs();
            Delete delete = (Delete)antProject.createTask("delete");
            delete.setDir(stubDir);
            delete.execute();


            Javadoc javadoc = (Javadoc)antProject.createTask("javadoc");
            
            Path sourcePath = new Path(antProject, srcDir.getAbsolutePath());
            
            
            javadoc.setSourcepath(sourcePath);
            javadoc.setClasspath(prepareStubberClassPath());
            javadoc.setDocletPath(new Path(antProject, findStubberJar().getAbsolutePath()));
            javadoc.setDoclet("com.codename1.build.client.StubGenerator");

            FileSet fileset = new FileSet();
            fileset.setProject(antProject);

            //getLog().info("dir="+project.getCompileSourceRoots().get(0));
            fileset.setDir(new File(project.getCompileSourceRoots().get(0)));
            getLog().debug("includes="+stubberIncludes+"; excludes="+stubberExcludes);

            fileset.setExcludes("*.java,"+str(stubberExcludes));
            fileset.setIncludes(str(stubberIncludes));

            FilenameSelector javaFiles = new FilenameSelector();
            javaFiles.setName("**/*.java");

            fileset.addFilename(javaFiles);
            //javadoc.addFileset(fileset);

            javadoc.execute();

            //<zip basedir="build/stubs" compress="false" destfile="build/lib/stubs.zip" />
            Zip zip = (Zip)antProject.createTask("zip");

            zip.setBasedir(stubDir);
            zip.setCompress(false);

            File libDir = new File(buildDir, "lib");
            libDir.mkdirs();
            //File stubsZip = new File(new File(project.getBuild().getOutputDirectory()), ""
            File stubsZip = new File(this.outputDirectory +"/"+ this.finalName + "-sources.jar");

            zip.setDestFile(stubsZip);
            zip.execute();

            projectHelper.attachArtifact(project, "java-source", "sources", stubsZip);


    //        <delete dir="build/stubs"/>
    //        <javadoc sourcepath="src"
    //            classpath="lib/CodenameOne.jar:lib/CLDC11.jar"
    //            docletpath="Stubber.jar"
    //            doclet="com.codename1.build.client.StubGenerator"> 
    //            <fileset dir="${src.dir}" excludes="*.java,${excludes}" includes="${includes}">
    //                <filename name="**/*.java"/>
    //            </fileset>
    //         </javadoc>
    //            
        } finally {
            if (!antBuildDirExists && antBuildDir.exists()) {
                Delete delete = (Delete)antProject.createTask("delete");
                delete.setDir(antBuildDir);
                delete.execute();
            }
            if (stubDir.exists()) {
                Delete delete = (Delete)antProject.createTask("delete");
                delete.setDir(stubDir);
                delete.execute();
            }
        }
                        
    }
    
    private void buildMain(File libJarRoot) {
        
        //Zip zip = (Zip)antProject.createTask("zip");
        
        //zip.setCompress(false);
        getLog().debug("Zipping baseDir "+new File(this.project.getBuild().getOutputDirectory()));
        //zip.setBasedir(new File(this.project.getBuild().getOutputDirectory()));
        //File destFile = new File(new File(buildDir, "lib"), "main.zip");
        //getLog().debug("Into zip file "+destFile);
        //destFile.getParentFile().mkdirs();
        //zip.setDestFile(destFile);
        //zip.execute();
        libJarRoot.mkdirs();
        Copy copy = (Copy)antProject.createTask("copy");
        copy.setTodir(libJarRoot);
        FileSet files = new FileSet();
        files.setProject(antProject);
        files.setDir(new File(this.project.getBuild().getOutputDirectory()));
        files.setIncludes("**");
        copy.addFileset(files);
        copy.execute();
        
        
    }
    
    private File nativeDir;
    private File buildDir;
    
    private void buildNative(File libRoot, String type, String prefix) {
        File dir = new File(nativeDir, type);
        dir.mkdirs();
        Zip zip = (Zip)antProject.createTask("zip");
        zip.setBasedir(dir);
        zip.setCompress(false);
        
        libRoot.mkdirs();
        
        File destFile = new File(libRoot, prefix+type+".zip");
        zip.setDestFile(destFile);
        zip.execute();
        
        
    }
    
    private void buildNatives(File libRoot) {
        
        for (String type : NATIVE_TYPES) {
            buildNative(libRoot, type, "native");
        }
    }
    
    
    
    
    

}
