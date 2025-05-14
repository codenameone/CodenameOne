/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;

/**
 * Opens the designer.
 * @author shannah
 */
@Mojo(name = "designer")
public class OpenDesignerMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        updateCodenameOne(false, getDesignerJar());
        Java java = createJava();
        java.setFork(true);
        java.setSpawn(true);
        java.setJar(getDesignerJar());
        java.createArg().setFile(getResourceFile());
        java.executeJava();
    }
 
    
    
     
    private File getResourceFile() {
        return new File(project.getProperties().getProperty("cn1.resourceFile", project.getCompileSourceRoots().get(0) + File.separator + "theme.res"));
        
        
    }
}
