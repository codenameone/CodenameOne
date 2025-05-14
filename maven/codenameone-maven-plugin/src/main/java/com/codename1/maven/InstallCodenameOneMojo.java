package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

@Mojo(name="install-codenameone")
public class InstallCodenameOneMojo extends AbstractCN1Mojo {
    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File cn1Dir = new File(System.getProperty("user.home"), ".codenameone");
        if (!cn1Dir.exists()) {
            getLog().info("Installing Codename One to "+cn1Dir);
            updateCodenameOne(true);
        }

    }
}
