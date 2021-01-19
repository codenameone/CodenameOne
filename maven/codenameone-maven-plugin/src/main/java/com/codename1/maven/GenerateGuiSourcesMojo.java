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
 * Goal to generate java sources from the guibuilder files.
 * @author shannah
 */
@Mojo(name="generate-gui-sources", defaultPhase = LifecyclePhase.INITIALIZE)
public class GenerateGuiSourcesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        System.setProperty("javax.xml.bind.context.factory", "com.sun.xml.bind.v2.ContextFactory");
                
        GenerateGuiSources g = new GenerateGuiSources();
        g.setSrcDir(new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "java"));
        g.setGuiDir(new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "guibuilder"));
        g.execute();
    }
    
}
