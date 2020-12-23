/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
@Mojo(name = "compliance-check", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class ComplianceCheckMojo extends AbstractCN1Mojo {

    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running compliance check against Codename One Java Runtime API");
        getLog().info("See https://www.codenameone.com/javadoc/ for supported Classes and Methods");
        
        if (true) {
            // Using the proguard check because it works even for code that has already been compiled
            // such as kotlin, or other java libraries.
            copyKotlinIncrementalCompileOutputToOutputDir();
            runProguard();
            return;
        }

        File javaRuntimeJar = getJar(GROUP_ID, JAVA_RUNTIME_ARTIFACT_ID);
        if (javaRuntimeJar == null) {
            getLog().info("Skipping compliance check because " + JAVA_RUNTIME_ARTIFACT_ID + " is not listed in dependencies");
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

        Javac javac = (Javac) antProject.createTask("javac");
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
    
    private void runProguard() throws MojoExecutionException {
        runProguard(0); // First pass strips code without warnings
        runProguard(1); // Second pass checks the stripped jar generated in the first pass to make sure there are 
                        // no warnings - which would result from missing classes
    }

    private void runProguard(int passNum) throws MojoExecutionException{
        Java java = createJava();
        Path classPath = java.createClasspath();
        classPath.setProject(antProject);
        for (File jar : getProguardJars()) {
            classPath.add(new Path(antProject, jar.getAbsolutePath()));
        }
        getLog().info("Proguard classpath: "+classPath);
        java.setClasspath(classPath);
        java.setClassname(proguardMainClass);
        java.setFailonerror(true);
        java.setFork(true);
        
        //List<String> args = new ArrayList<String>();
        /*
        -verbose
            -dontobfuscate
            -libraryjars lib/CLDC11.jar:lib/CodenameOne.jar
            -injars build/tmp:lib/impl/cls
            -outjars build/tmp.jar
            -keep class ${codename1.packageName}.${codename1.mainName} {
            *;
            }
            -dontwarn **

        */
        java.createArg().setValue("-dontobfuscate");
        if (passNum == 1) {
            //java.createArg().setValue("-verbose");
            java.createArg().setValue("-dontnote");
        } else {
            java.createArg().setValue("-dontnote");
            java.createArg().setValue("-dontwarn");
        }
        java.createArg().setValue("-libraryjars");
        Path libraryJarsPath = new Path(antProject, getJavaRuntimeJar().getAbsolutePath());
        libraryJarsPath.add(new Path(antProject, getCodenameOneJar().getAbsolutePath()));
        java.createArg().setPath(libraryJarsPath);
        File complianceCheckJar =  new File(project.getBuild().getDirectory() + File.separator + "compliance-check.jar");
        java.createArg().setValue("-keepattributes");
        java.createArg().setValue("Signature");
        if (passNum == 0) {
            Path inJars = new Path(antProject, project.getBuild().getOutputDirectory());
            
            project.getDependencyArtifacts().forEach(artifact -> {
                if (artifact.getGroupId().equals("com.codenameone") && artifact.getArtifactId().equals("codenameone-core")) {
                    return;
                }

                if (artifact.getScope().equals("compile") || artifact.getScope().equals("system")) {
                    inJars.add(new Path(antProject, getJar(artifact).getAbsolutePath()+"(!META-INF/**)"));
                }
            });
            java.createArg().setValue("-injars");
            java.createArg().setPath(inJars);
            java.createArg().setValue("-outjars");

            
            java.createArg().setPath(new Path(antProject, complianceCheckJar.getAbsolutePath()));
            
        } else if (passNum == 1) {
            java.createArg().setValue("-injars");
            java.createArg().setPath(new Path(antProject, complianceCheckJar.getAbsolutePath()));
            
        }
        java.createArg().setValue("-keep");
        String keep = "class "+properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName")+" {\n" +
"            *;\n" +
"            }";
        //getLog().info("Addin -keep directive "+keep);
        java.createArg().setValue(keep);
        if (passNum == 0) {
            java.createArg().setValue("-dontwarn");
            java.createArg().setValue("**");
            java.createArg().setValue("-ignorewarnings");
        }
        
        int result = java.executeJava();
        if (result != 0) {
            throw new MojoExecutionException("Compliance check failed");
        }

    }
    
    

    private File getJavaRuntimeJar() {
        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(JAVA_RUNTIME_ARTIFACT_ID + " not found in dependencies");
        
    }
    
    private File getCodenameOneJar() {
        String codenameOneCoreId = "codenameone-core";
        for (Artifact artifact : project.getDependencyArtifacts()) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(codenameOneCoreId + " not found in dependencies");
    }
    
    private List<File> getProguardJars() throws MojoExecutionException {

        List<Artifact> proguardArtifacts = new ArrayList<Artifact>();
        int proguardArtifactDistance = -1;
        // This should be solved in Maven 2.1
        //Starting in v. 7.0.0., proguard got split up in proguard-base and proguard-core,
        //both of which need to be on the classpath.
        for (Artifact artifact : pluginArtifacts) {
            getLog().debug("pluginArtifact: " + artifact.getFile());

            final String artifactId = artifact.getArtifactId();
            if (artifactId.startsWith("proguard")) {
                int distance = artifact.getDependencyTrail().size();
                getLog().debug("proguard DependencyTrail: " + distance);

                /*
				 *  Check if artifact has been defined twice - eg. no proguardVersion given but dependency for proguard
				 *  defined in plugin config
                 */
                for (Artifact existingArtifact : proguardArtifacts) {
                    if (existingArtifact.getArtifactId().equals(artifactId)) {
                        getLog().warn("Dependency for proguard defined twice! This may lead to unexpected results: "
                                + existingArtifact.getArtifactId() + ":" + existingArtifact.getVersion()
                                + " | "
                                + artifactId + ":" + artifact.getVersion());
                        break;
                    }
                }

                proguardArtifacts.add(artifact);

            }
        }
        if (!proguardArtifacts.isEmpty()) {
            List<File> resList = new ArrayList<File>(proguardArtifacts.size());
            for (Artifact p : proguardArtifacts) {
                getLog().debug("proguardArtifact: " + p.getFile());
                resList.add(p.getFile().getAbsoluteFile());
            }
            return resList;
        }
        getLog().info("proguard jar not found in pluginArtifacts");

        ClassLoader cl;
        cl = getClass().getClassLoader();
        // cl = Thread.currentThread().getContextClassLoader();
        String classResource = "/" + proguardMainClass.replace('.', '/') + ".class";
        URL url = cl.getResource(classResource);
        if (url == null) {
            throw new MojoExecutionException(
                    "Obfuscation failed ProGuard (" + proguardMainClass + ") not found in classpath");
        }
        String proguardJar = url.toExternalForm();
        if (proguardJar.startsWith("jar:file:")) {
            proguardJar = proguardJar.substring("jar:file:".length());
            proguardJar = proguardJar.substring(0, proguardJar.indexOf('!'));
        } else {
            throw new MojoExecutionException("Unrecognized location (" + proguardJar + ") in classpath");
        }
        return Collections.singletonList(new File(proguardJar));
    }

    private String proguardMainClass = "proguard.ProGuard";
}
