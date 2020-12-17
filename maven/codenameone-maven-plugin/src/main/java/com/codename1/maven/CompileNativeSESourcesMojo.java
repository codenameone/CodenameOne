/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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
@Mojo(name="compile-javase-sources", defaultPhase = LifecyclePhase.COMPILE)
public class CompileNativeSESourcesMojo extends AbstractCN1Mojo {
  
    private long getSourcesLastModified() {
        long lastModified = lastModifiedRecursive(getProjectNativeSEDir(), ALL_FILES_FILTER);
        for (File f : getLibsNativeSESourceJars()) {
            lastModified = Math.max(f.lastModified(), lastModified);
        }
        return lastModified;
    }
    
    
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
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
        
        
        File destJar = getProjectInternalTmpJar();
        File nativeSE = getProjectNativeSEDir();
        nativeSE.mkdirs();
        boolean requiresCompile = !destJar.exists() || getSourcesLastModified() > destJar.lastModified();
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
        project.getDependencyArtifacts().forEach(artifact -> {
            if (isJavaSEDep(artifact) || "compile".equals(artifact.getScope()) || "system".equals(artifact.getScope()) || "runtime".equals(artifact.getScope())) {
                classpath.add(new Path(antProject, findArtifactFile(artifact).getAbsolutePath()));
            }
        });
        getLog().debug("Classpath="+classpath);
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
        copy.execute();
        
        
        getLog().debug("Zipping "+destDir+" to "+destJar);
        Zip zip = (Zip)antProject.createTask("zip");
        destJar.delete();
        zip.setDestFile(destJar);
        zip.setBasedir(destDir);
        zip.execute();
        getLog().debug("Deleting "+destDir);
        //projectHelper.attachArtifact(project, "java", "javase", destJar);
        
        //delTree(destDir);
        
        
        
    }
    
    private boolean isJavaSEDep(Artifact artifact) {
        return artifact.getGroupId().equals(GROUP_ID) && artifact.getArtifactId().equals("javase");
    }
    
    
    
    
    
    
    
    
}
