/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/// A [ProjectHost] over one Maven module, for the goals that are not
/// `AbstractCN1Mojo`s (the backend goals). Answers only from the module itself.
final class MavenModuleHost implements ProjectHost {
    private final MavenProject project;
    private final Log log;

    MavenModuleHost(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }

    @Override
    public Log log() {
        return log;
    }

    @Override
    public ProjectLayout layout() {
        File dir = project == null ? new File(".") : project.getBasedir();
        ProjectLayout l = ProjectLayouts.detect(dir);
        return l != null ? l : ProjectLayouts.of(BuildSystem.MAVEN, ProjectKind.BACKEND, dir, dir);
    }

    @Override
    public File cn1ProjectDir() {
        return project.getBasedir();
    }

    @Override
    public File baseDir() {
        return project.getBasedir();
    }

    @Override
    public File buildDirectory() {
        return new File(project.getBuild().getDirectory());
    }

    @Override
    public File outputDirectory() {
        return new File(project.getBuild().getOutputDirectory());
    }

    @Override
    public String finalName() {
        return project.getArtifactId();
    }

    @Override
    public String groupId() {
        return project.getGroupId();
    }

    @Override
    public Properties projectProperties() {
        return project.getProperties();
    }

    @Override
    public Properties userProperties() {
        return new Properties();
    }

    @Override
    public List<String> compileClasspathElements() throws BuildExecutionException {
        try {
            return project.getCompileClasspathElements();
        } catch (Exception ex) {
            throw new BuildExecutionException("Could not resolve the compile classpath", ex);
        }
    }

    @Override
    public List<String> runtimeClasspathElements() throws BuildExecutionException {
        try {
            return project.getRuntimeClasspathElements();
        } catch (Exception ex) {
            throw new BuildExecutionException("Could not resolve the runtime classpath", ex);
        }
    }

    @Override
    public List<String> compileSourceRoots() {
        List<String> out = new ArrayList<String>();
        for (Object root : project.getCompileSourceRoots()) {
            out.add(String.valueOf(root));
        }
        return out;
    }

    @Override
    public Collection<BuildArtifact> artifacts() {
        List<BuildArtifact> out = new ArrayList<BuildArtifact>();
        if (project.getArtifacts() != null) {
            for (Artifact a : project.getArtifacts()) {
                out.add(AbstractCN1Mojo.toBuildArtifact(a));
            }
        }
        return out;
    }

    @Override
    public Set<String> neededWithoutDesktopRuntime() {
        return null;
    }

    @Override
    public File getJar(BuildArtifact artifact) {
        return artifact.getFile();
    }

    @Override
    public File getJar(String groupId, String artifactId, String classifier) {
        for (BuildArtifact a : artifacts()) {
            if (a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)
                    && (classifier == null ? a.getClassifier() == null : classifier.equals(a.getClassifier()))) {
                return a.getFile();
            }
        }
        return null;
    }

    @Override
    public long sessionStartTime() {
        return Long.MAX_VALUE;
    }

    @Override
    public long sourcesModificationTime() {
        return Long.MAX_VALUE;
    }

    @Override
    public void attachArtifact(String type, String classifier, File file) {
        log.info("Build output: " + file.getAbsolutePath());
    }

    @Override
    public String codenameOneVersion() {
        return project.getProperties().getProperty("cn1.version", "");
    }

    @Override
    public String pluginVersion() {
        return project.getProperties().getProperty("cn1.plugin.version", "");
    }
}
