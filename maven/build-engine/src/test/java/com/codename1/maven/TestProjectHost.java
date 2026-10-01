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
import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import com.codename1.build.SystemStreamLog;
import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/// A [ProjectHost] for engine tests: every answer is a public field.
public class TestProjectHost implements ProjectHost {
    public Log log = new SystemStreamLog();
    public File projectDir = new File(".");
    public File buildDir = new File("build");
    public File outputDir = new File("build/classes");
    public String finalName = "test";
    public String groupId = "com.example";
    public Properties projectProperties = new Properties();
    public Properties userProperties = new Properties();
    public List<String> classpath = new ArrayList<String>();
    public List<String> sourceRoots = new ArrayList<String>();
    public List<BuildArtifact> artifacts = new ArrayList<BuildArtifact>();
    public long sourcesModified;

    /// A dependency with the given scope, as a resolved build artifact.
    public static BuildArtifact artifact(String artifactId, String scope, File file) {
        return new BuildArtifact("com.example", artifactId, "1.0", null, "jar", scope, file, null);
    }

    @Override
    public Log log() {
        return log;
    }

    @Override
    public ProjectLayout layout() {
        return ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.APP, projectDir, projectDir);
    }

    @Override
    public File cn1ProjectDir() {
        return projectDir;
    }

    @Override
    public File baseDir() {
        return projectDir;
    }

    @Override
    public File buildDirectory() {
        return buildDir;
    }

    @Override
    public File outputDirectory() {
        return outputDir;
    }

    @Override
    public String finalName() {
        return finalName;
    }

    @Override
    public String groupId() {
        return groupId;
    }

    @Override
    public Properties projectProperties() {
        return projectProperties;
    }

    @Override
    public Properties userProperties() {
        return userProperties;
    }

    @Override
    public List<String> compileClasspathElements() {
        return classpath;
    }

    @Override
    public List<String> runtimeClasspathElements() {
        return classpath;
    }

    @Override
    public List<String> compileSourceRoots() {
        return sourceRoots;
    }

    @Override
    public Collection<BuildArtifact> artifacts() {
        return artifacts;
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
        for (BuildArtifact a : artifacts) {
            if (a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)) {
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
        return sourcesModified;
    }

    @Override
    public void attachArtifact(String type, String classifier, File file) {
    }

    @Override
    public String codenameOneVersion() {
        return "8.0-SNAPSHOT";
    }

    @Override
    public String pluginVersion() {
        return "8.0-SNAPSHOT";
    }

    /// An empty host.
    public static TestProjectHost empty() {
        return new TestProjectHost();
    }
}
