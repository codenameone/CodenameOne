/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * A mojo that updates Codename One.
 * @author shannah
 */
@Mojo(name = "update")
public class UpdateCodenameOneMojo extends AbstractCN1Mojo {
    
    
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        updateCodenameOne(true);
 
    }
    
}
