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
import org.apache.tools.ant.types.Commandline.Argument;

/**
 *
 * @author shannah
 */
@Mojo(name = "settings")
public class OpenSettingsMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        updateCodenameOne(false, getGuiBuilderJar());
        Java java = createJava();
        java.setFork(true);
        java.setSpawn(true);
        java.setJar(getGuiBuilderJar());
        Argument arg = java.createArg();
        arg.setValue("-settings");
        arg = java.createArg();
        arg.setFile(new File(getCN1ProjectDir(), "codenameone_settings.properties"));
        java.executeJava();
        
    }
    
    private File getGuiBuilderJar() {
        File home = new File(System.getProperty("user.home"));
        File codenameone = new File(home, ".codenameone");
        File settingsJar = new File(codenameone, "guibuilder.jar");
        
        return settingsJar;
    }
    
}
