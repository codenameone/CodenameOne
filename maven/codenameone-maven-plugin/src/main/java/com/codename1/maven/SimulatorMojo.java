/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.types.Permissions.Permission;

/**
 *
 * @author shannah
 */
@Mojo( name="simulator", 
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, 
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, 
        defaultPhase = LifecyclePhase.COMPILE
        
        
        )
@Execute(phase = LifecyclePhase.COMPILE)
public class SimulatorMojo extends AbstractCN1Mojo {
    
 
    
private static final String GROUP_ID="com.codenameone";

    @Parameter(property = "cn1.exec.args")
    private String execArgs;
  
    
    @Override
    public void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("execArgs="+execArgs);
        getLog().info(""+System.getProperties());
        //String args = "-agentlib:jdwp=transport=dt_socket,server=n,address=${jpda.address} -classpath %classpath ${packageClassName}";
        
        Java java = createJava();
        
        System.out.println("Setting classpath for java: "+prepareClasspath(java));
        //java.setNewenvironment(true);
        java.setClasspath(prepareClasspath(java));
        
        
        Variable cn1Classpath = new Variable();
        cn1Classpath.setKey("cn1.class.path");
        cn1Classpath.setPath(prepareClasspath(java));
        
        java.addSysproperty(cn1Classpath);
        java.setClassname("com.codename1.impl.javase.Simulator");
        if (execArgs != null) {
            java.setJvmargs(execArgs);
        }
        java.setArgs(properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        java.setDir(getCN1ProjectDir());
        
        Permissions perms = java.createPermissions();
        
        Permission allPermissions = new Permission();
        allPermissions.setClass("java.security.AllPermission");
        perms.addConfiguredGrant(allPermissions);
        
        java.setFork(true);
        
        
        //java.setOutputproperty("stdout");
        //java.setErrorProperty("stderr");
        
        running = true;
        
        
        /*
        Thread redirectStdout = new Thread(()->{
            while (running) {
                
                String contents = antProject.getProperty("stdout");
                antProject.setProperty("stdout", "");
                if (contents != null && !contents.isEmpty()) {
                    getLog().info(contents);
                }
                contents = antProject.getProperty("stderr");
                antProject.setProperty("stderr", "");
                if (contents != null && !contents.isEmpty()) {
                    getLog().error(contents);
                }
                try {
                    Thread.sleep(100);
                } catch (Throwable t){}
            }
        });

        redirectStdout.start();
*/
        /*
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setIn(new DemuxInputStream(antProject));
        System.setOut(new PrintStream(new DemuxOutputStream(antProject, false) {
            @Override
            protected void processBuffer(ByteArrayOutputStream buffer) {
                super.processBuffer(buffer);
                try {
                    getLog().info(new String(buffer.toByteArray()));
                } catch (Throwable t){}
            }
            
        }));
        System.setErr(new PrintStream(new DemuxOutputStream(antProject, true)));
*/
        try {
            
        
            java.executeJava();
        } finally {
            running = false;
            /*
            System.setIn(oldIn);
            System.setOut(oldOut);
            System.setErr(oldErr);
*/
        }
       
        try {
            //Thread.sleep(10000);
        } catch (Throwable t){}
        getLog().info(antProject.getProperty("stdout"));
    }
    private boolean running;
    
    
   
    
    private Path prepareClasspath(Java java) {
        Log log = getLog();
        log.info("Preparing classpath for Simulator");
        List<String> paths = new ArrayList<>();
        //StringBuilder classpath = new StringBuilder();
        Path classpath = java.createClasspath();
        
        for (Artifact artifact : project.getDependencyArtifacts()) {
            
            log.info("Checking artifact "+artifact);
            //if (!filterByScope(artifact)) {
            //    continue;
            //}
            if (!filterByName(artifact)) {
                continue;
            }
            File file = artifact.getFile();
            //if (classpath.length() > 0) {
            //    classpath.append(':');
            //}
            classpath.add(new Path(antProject, file.getAbsolutePath()));
            if (Cn1libUtil.isCN1Lib(file)) {
                File nativeJar = Cn1libUtil.getNativeSEJar(artifact);
                if (nativeJar != null) {
                    //classpath.add(new Path(antProject, nativeJar.getAbsolutePath()));
                    
                    for (File nativeDepJar : Cn1libUtil.getNativeSEEmbeddedJars(artifact)) {
                        classpath.add(new Path(antProject, nativeDepJar.getAbsolutePath()));
                    }
                }
                        
            }
            if (getProjectInternalTmpJar() != null && getProjectInternalTmpJar().exists()) {
                classpath.add(new Path(antProject, getProjectInternalTmpJar().getAbsolutePath()));
            }
            
            //classpath.append(file.getPath());
            //paths.add(file.getAbsolutePath());
        }
        //if (classpath.length() > 0) {
        //    classpath.append(':');
        //}
        //classpath.append(classFiles.getPath());
        //paths.add(classFiles.getAbsolutePath());
        classpath.add(new Path(antProject, project.getBuild().getOutputDirectory()));
        log.info("Using the following classpath for Stubber: " + classpath);
        return classpath;
    }
    
     private boolean filterByScope(Artifact artifact) {
        return isSupportedScope(artifact.getScope());
                
    }
     
    private boolean filterByName(Artifact artifact) {
        if (GROUP_ID.equals(artifact.getGroupId())) {
            if ("java-runtime".equals(artifact.getArtifactId())) {
                return false;
            }
        }
        return true;
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
