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
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * @author shannah
 */
@Mojo(name="add-nativese-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE)
public class AddNativeSEDependenciesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if ("javase".equals(project.getProperties().getProperty("codename1.targetPlatform"))) {
            File javase = new File(project.getBasedir(), "native" + File.separator + "javase");
            if (javase.exists()) {
                project.addCompileSourceRoot(javase.getAbsolutePath());
            }
        }
    }
    
}
