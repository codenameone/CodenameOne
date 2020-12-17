/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.util.Properties;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author shannah
 */
@Mojo(name = "prepare-simulator-classpath", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PrepareSimulatorClasspathMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing Simulator Classpath");
        Properties props = project.getModel().getProperties();
        if (props == null) {
            props = new Properties();
            project.getModel().setProperties(props);
        }
        props.setProperty("exec.args", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));

        
    }
    
}
