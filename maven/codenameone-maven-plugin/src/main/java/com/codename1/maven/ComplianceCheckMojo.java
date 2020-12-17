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
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
@Mojo(name = "compliance-check", defaultPhase = LifecyclePhase.COMPILE)
public class ComplianceCheckMojo extends AbstractCN1Mojo {
    
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running compliance check against Codename One Java Runtime API");
        getLog().info("See https://www.codenameone.com/javadoc/ for supported Classes and Methods");
        
        File javaRuntimeJar = getJar(GROUP_ID, JAVA_RUNTIME_ARTIFACT_ID);
        if (javaRuntimeJar == null) {
            getLog().info("Skipping compliance check because "+JAVA_RUNTIME_ARTIFACT_ID+" is not listed in dependencies");
            return;
        }
        
//        <mkdir dir="build/tmp"/>
//        <javac destdir="build/tmp"
//            source="1.8"
//            target="1.8"
//            bootclasspath="lib/CLDC11.jar"
//            classpath="${javac.classpath}:${build.classes.dir}">
//            <src path="${src.dir}"/>
//        </javac>    

        File build = new File(project.getBuild().getDirectory());
        File tmp = new File(build, "tmp");
        tmp.mkdirs();
        
        Javac javac = (Javac)antProject.createTask("javac");
        javac.setDestdir(tmp);
        javac.setSource("1.8");
        javac.setTarget("1.8");
        javac.setBootclasspath(new Path(antProject, javaRuntimeJar.getAbsolutePath()));
        Path path = new Path(antProject);
        project.getDependencyArtifacts().forEach(artifact -> {
            if (isSupportedScope(artifact.getScope())) {
                path.add(new Path(antProject, getJar(artifact).getAbsolutePath()));
            }
        });
        
        javac.setClasspath(path);
        Path srcPath = javac.createSourcepath();
        srcPath.setPath(project.getCompileSourceRoots().get(0));
        javac.setSrcdir(srcPath);
        javac.setErrorProperty("complianceLog");
        try {
            javac.execute();
        } catch (Throwable t) {
            getLog().error("API compliance check failed.  This project uses APIs that are not currently available in Codename One's java runtime.");
            getLog().error("See https://www.codenameone.com/javadoc/ for available APIs");
            getLog().error(antProject.getProperty("complianceLog"));
            throw new MojoExecutionException("API Compliance check failed", t);
        }
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
}
