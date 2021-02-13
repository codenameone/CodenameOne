/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

/**
 *
 * @author shannah
 */
public class ProjectUtil {
    private final MavenProject project;
    
    public ProjectUtil(MavenProject project) {
        this.project = project;
    }
    
    public static ProjectUtil wrap(MavenProject proj) {
        return new ProjectUtil(proj);
    }
    
    public File getCN1LibProjectDir() {
        File f = new File(project.getBasedir(), "codenameone_library_appended.properties");
        if (f.exists()) {
            return project.getBasedir();
        }
        
        for (String srcRoot : project.getCompileSourceRoots()) {
            f = new File(srcRoot);
            if (f.exists() && f.getParentFile().exists()) {
                f = new File(f.getParentFile(), "codenameone_library_appended.properties");
                if (f.exists()) {
                    return f.getParentFile();
                }
            }
        }
        return null;
    }
    
    public File getCN1ProjectDir() {
        File f = new File(project.getBasedir(), "codenameone_settings.properties");
        if (f.exists()) {
            return project.getBasedir();
        }
        
        for (String srcRoot : project.getCompileSourceRoots()) {
            f = new File(srcRoot);
            if (f.exists() && f.getParentFile().exists()) {
                f = new File(f.getParentFile(), "codenameone_settings.properties");
                if (f.exists()) {
                    return f.getParentFile();
                }
                f = new File(f.getParentFile(), "codenameone_library_appended.properties");
                if (f.exists()) {
                    return f.getParentFile();
                }
            }
        }
        return null;
    }

    /**
     * Generates a hello world project from the cn1app-archetype at the given location.
     * @param destDir The destination directory where the project should be created.  The name of this file will be used as the archetypeId, and the mainName
     * @param groupId The groupId for the project
     * @throws MojoExecutionException If it fails to generate the project.
     * @throws IllegalStateException If The codename1.version system property is not set.  This property should be set to the current version of the codename one maven plugin
     *  As this method is intended primarly to be used in unit tests.
     */
    public static void generateHelloWorldProject(File destDir, String groupId) throws MojoExecutionException {
        InvocationRequest req = new DefaultInvocationRequest();

        req.setGoals(Collections.singletonList("archetype:generate"));
        Properties props = new Properties();
        props.setProperty("archetypeGroupId", "com.codenameone.archetypes");
        props.setProperty("archetypeArtifactId", "cn1app-archetype");
        String version = System.getProperty("codename1.version");
        if (version == null) {
            throw new IllegalStateException("generateHelloProject requires that the codename1.version System property be set.");
        }
        props.setProperty("archetypeVersion", version);
        props.setProperty("artifactId", destDir.getName());
        props.setProperty("groupId", groupId);
        props.setProperty("version", version);
        props.setProperty("mainName", destDir.getName());
        props.setProperty("interactiveMode", "false");
        req.setProperties(props);
        req.setBaseDirectory(destDir.getParentFile());

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(req);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to generate HelloWorld project at " + destDir, result.getExecutionException());
            }
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException("Failed to generate HelloWorld project at "+destDir, ex);
        }
    }


    public static void executeGoal(File projectDir, String goalName, String... properties) throws MojoExecutionException {
        InvocationRequest req = new DefaultInvocationRequest();

        req.setGoals(Collections.singletonList(goalName));
        Properties props = new Properties();
        props.setProperty("interactiveMode", "false");
        for (String p : properties) {
            int pos;
            if ((pos = p.indexOf("=")) > 0) {
                props.setProperty(p.substring(0, pos), p.substring(pos+1));
            } else {
                props.setProperty(p, "true");
            }
        }
        req.setProperties(props);
        req.setBaseDirectory(projectDir);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(req);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to run goal "+goalName+" on project project at " + projectDir, result.getExecutionException());
            }
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException("Failed to run goal "+goalName+" on project at "+projectDir, ex);
        }
    }


    
    
}
