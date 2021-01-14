/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import com.codename1.build.client.GenerateGuiSources;
import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author shannah
 */
@Mojo(name="generate-gui-sources", defaultPhase = LifecyclePhase.INITIALIZE)
public class GenerateGuiSourcesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        System.setProperty("javax.xml.bind.context.factory", "com.sun.xml.bind.v2.ContextFactory");
                
        GenerateGuiSources g = new GenerateGuiSources();
        g.setSrcDir(new File(project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "java"));
        g.setGuiDir(new File(project.getBasedir() + File.separator + "src" + File.separator + "main" + File.separator + "guibuilder"));
        g.execute();
    }
    
}
