/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Java;

/**
 *
 * @author shannah
 */
@Mojo(name = "css", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CompileCSSMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        /*
        <java jar="${user.home}/.codenameone/designer_1.jar" fork="true" failonerror="true">
            <jvmarg value="-Dcli=true"/>
            <arg value="-css"/>
            <arg file="css/theme.css"/>
            <arg file="src/theme.res"/>
        </java>
        */
        
        if (properties.getProperty("codename1.cssTheme", null) == null) {
            getLog().info("CSS themes not activated for this project.  Skipping CSS compilation");
            return;
        }
        
        File cssDirectory = findCSSDirectory();
        if (cssDirectory == null || !cssDirectory.exists()) {
            getLog().warn("CSS compilation skipped because no CSS theme was found");
            return;
        }
        
        
        File cssTheme = new File(cssDirectory, "theme.css");
        
        File cssImplDir = new File(project.getBuild().getDirectory() + File.separator + "css");
        cssImplDir.mkdirs();
        Java java = createJava();
        java.setDir(getCN1ProjectDir());
        java.setJar(getDesignerJar());
        java.setFork(true);
        java.setFailonerror(true);
        String cefDir = System.getProperty("cef.dir", System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "cef");
        java.createJvmarg().setValue("-Dcli=true");
        java.createJvmarg().setValue("-Dcef.dir="+cefDir);
        java.createJvmarg().setValue("-Dcn1.libCSSDir="+cssImplDir.getAbsolutePath());
        java.createArg().setValue("-css");
        java.createArg().setFile(cssTheme);
        java.createArg().setFile(new File(project.getBuild().getOutputDirectory() + File.separator + "theme.res"));
        java.executeJava();
    }
    
    protected File findCSSDirectory() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File cssSibling = new File(dirFile.getParentFile(), "css");
            File themeCss = new File(cssSibling, "theme.css");
            if (themeCss.exists()) {
                return cssSibling;
            }
            
        }
        return null;
    }
    
    protected File findThemeRes() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File resources = new File(dirFile.getParentFile(), "resources");
            File themeRes = new File(resources, "theme.res");
            if (themeRes.exists()) {
                return themeRes;
            }
            themeRes = new File(dirFile, "theme.res");
            if (themeRes.exists()) {
                return themeRes;
            }
        }
        return null;
    }
    
    protected File getDesignerJar() throws MojoExecutionException{
        File cn1Home = new File(System.getProperty("user.home") + File.separator + ".codenameone");
        File designerJar = new File(cn1Home, "designer_1.jar");
        if (!designerJar.exists()) {
            updateCodenameOne(true);
        }
        
        if (!designerJar.exists()) {
            throw new MojoExecutionException("Failed to find designer_1.jar even after running codename one update.");
        }
        return designerJar;
    }
    
}
