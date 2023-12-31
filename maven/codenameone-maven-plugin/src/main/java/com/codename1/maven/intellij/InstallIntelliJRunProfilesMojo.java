package com.codename1.maven.intellij;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Goal to install the IntelliJ run profiles for the project.
 */
@Mojo(name="configure-intellij", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class InstallIntelliJRunProfilesMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File workspaceFile = new File(".idea/workspace.xml");
        if (!workspaceFile.exists()) {
            throw new MojoFailureException("No workspace.xml file found.  Please run this goal from the root of your IntelliJ project.");
        }

        InputStream in = getClass().getResourceAsStream("workspace.xml");
        if (in == null) {
            throw new MojoFailureException("Failed to find workspace.xml template in resources");
        }
        try {
            FileUtils.copyURLToFile(getClass().getResource("workspace.xml"), workspaceFile);
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to copy workspace.xml template to " + workspaceFile.getAbsolutePath(), ex);
        }
    }
}
