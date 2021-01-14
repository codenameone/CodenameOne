/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.project.MavenProject;

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
    
    
    
}
