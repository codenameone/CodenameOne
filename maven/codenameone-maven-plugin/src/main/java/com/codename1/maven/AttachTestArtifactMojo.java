/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.tools.ant.taskdefs.Zip;

/**
 *
 * @author shannah
 */
@Mojo(name="attach-test-artifact")
public class AttachTestArtifactMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        Zip zip = (Zip)antProject.createTask("zip");
        File testOutputs = new File(project.getBuild().getTestOutputDirectory());
        if (!testOutputs.exists()) {
            testOutputs.mkdir();
        }
        File dummy = new File(testOutputs, project.getGroupId()+"__"+project.getArtifactId()+"__cn1tests");
        try {
            dummy.createNewFile();
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to create dummy test file", ex);
        }
        zip.setBasedir(testOutputs);
        File dest = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName()+"-tests.jar");
        zip.setDestFile(dest);
        zip.setCompress(false);
        zip.execute();
        projectHelper.attachArtifact(project, "jar", "tests", dest);
        
        
    }
    
}
