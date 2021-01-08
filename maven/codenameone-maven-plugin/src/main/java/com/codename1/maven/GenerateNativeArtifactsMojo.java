/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import static com.codename1.maven.AbstractCN1Mojo.ALL_FILES_FILTER;
import static com.codename1.maven.AbstractCN1Mojo.GROUP_ID;
import static com.codename1.maven.AbstractCN1Mojo.delTree;
import static com.codename1.maven.AbstractCN1Mojo.lastModifiedRecursive;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
@Mojo(name = "generate-native-artifacts", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateNativeArtifactsMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        List<String> types = Arrays.asList("ios", "android", "javascript", "win");
        for (String type : types) {
            generateNativeArtifact(type);
        }
        compileNativeSE();
    }
    
    private void generateNativeArtifact(String type) {
        File dir = new File(getCN1ProjectDir(), "native" + File.separator + type);
        if (!dir.exists()) {
            
            dir.mkdirs();
        }
        
        Zip zip = (Zip)antProject.createTask("zip");
        File artifact = new File(this.outputDirectory +"/"+ this.finalName + "-" + type + ".jar");
        zip.setDestFile(artifact);
        zip.setBasedir(dir);
        zip.execute();
        
        projectHelper.attachArtifact(project, "jar", type, artifact);
        
    }
    
    
   
    
    
    
    public void compileNativeSE() throws MojoExecutionException, MojoFailureException {
        /*
        <mkdir dir="native/javase" />
        <mkdir dir="native/internal_tmp" />
        <mkdir dir="lib/impl/native/javase" />
        <javac destdir="native/internal_tmp"
            encoding="${source.encoding}"
            source="1.8"
            target="1.8"
            classpath="${run.classpath}:${build.classes.dir}">
            <src path="native/javase"/>
            <src path="lib/impl/native/javase"/>
        </javac>
        <copy todir="native/internal_tmp">
            <fileset dir="native/javase" excludes="*.java,*.jar"/>
            <fileset dir="lib/impl/native/javase" excludes="*.java,*.jar"/>
        </copy>       
        */
        
        
        File destJar = new File(this.outputDirectory +"/"+ this.finalName + "-javase.jar");
        File nativeSE = getProjectNativeSEDir();
        nativeSE.mkdirs();
        boolean requiresCompile = true;
        if (!requiresCompile) {
            getLog().debug("javase native sources unchanged.  No compile necesary");
            //projectHelper.attachArtifact(project, "java", "javase", destJar);
            return;
        }
        
        getLog().info("Compiling JavaSE native interfaces to "+destJar);
        File destDir = getProjectInternalTmpDir();
        delTree(destDir);
        destDir.mkdirs();
        
        Javac javac = (Javac)antProject.createTask("javac");
        getLog().debug("destdir="+destDir);
        javac.setDestdir(destDir);
        javac.setEncoding("UTF-8");
        javac.setSource("1.8");
        javac.setTarget("1.8");
        
        Path classpath = javac.createClasspath();
        classpath.add(new Path(antProject, project.getBuild().getOutputDirectory()));
        classpath.add(new Path(antProject, destDir.getAbsolutePath()));
        for (File jar : getLibsNativeSEDependencyJars()) {
            classpath.add(new Path(antProject, jar.getAbsolutePath()));
        }
        project.getArtifacts().forEach(artifact -> {
            if (isJavaSEDep(artifact) || "compile".equals(artifact.getScope()) || "system".equals(artifact.getScope()) || "provided".equals(artifact.getScope())) {
                classpath.add(new Path(antProject, findArtifactFile(artifact).getAbsolutePath()));
            }
        });
        getLog().info("Classpath="+classpath);
        javac.setClasspath(classpath);
        
        Path src = javac.createSrc();
        
        src.add(new Path(antProject, nativeSE.getAbsolutePath()));
        for (File jar : getLibsNativeSESourceJars()) {
            File extractedJar = new File(jar.getParentFile(), jar.getName()+"-extracted");
            getLog().info("Processing nativese jar "+jar);
            if (!extractedJar.exists()) {
                getLog().info("Trying to extract "+jar+" to "+extractedJar);
                Expand expand = (Expand)antProject.createTask("unzip");
                
                expand.setSrc(jar);
                expand.setDest(extractedJar);
                expand.execute();
            }
            
            src.add(new Path(antProject, extractedJar.getAbsolutePath()));
        }
        getLog().debug("src="+src);
        javac.setSrcdir(src);
        javac.execute();
        
        getLog().debug("Copying resources from native sources directories to "+destDir);
        Copy copy = (Copy)antProject.createTask("copy");
        copy.setTodir(destDir);
        FileSet fileSet = new FileSet();
        fileSet.setProject(antProject);
        fileSet.setDir(nativeSE);
        //project.addCompileSourceRoot(nativeSE.getAbsolutePath());
        fileSet.setExcludes("*.java,*.jar");
        getLog().debug("Copying fileset "+fileSet);
        copy.addFileset(fileSet);
        /*
        for (File jar : getLibsNativeSESourceJars()) {
            getLog().info("Copying javase source jar "+jar+"");
            File extractedJar = new File(jar.getParentFile(), jar.getName()+"-extracted");
            fileSet = new FileSet();
            fileSet.setProject(antProject);
            fileSet.setDir(extractedJar);
            //project.addCompileSourceRoot(extractedJar.getAbsolutePath());
            fileSet.setExcludes("*.java,*.jar");
            getLog().debug("Copying fileset "+fileSet);
            copy.addFileset(fileSet);
        }
        */
        copy.execute();
        
        
        getLog().info("Zipping "+destDir+" to "+destJar);
        Zip zip = (Zip)antProject.createTask("zip");
        destJar.delete();
        zip.setDestFile(destJar);
        zip.setBasedir(destDir);
        zip.execute();
        getLog().debug("Deleting "+destDir);
        projectHelper.attachArtifact(project, "jar", "javase", destJar);
        
        //delTree(destDir);
        
        
        
    }
    
    private boolean isJavaSEDep(Artifact artifact) {
        return artifact.getGroupId().equals(GROUP_ID) && artifact.getArtifactId().equals("javase");
    }
    
    
    
}
