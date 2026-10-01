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
package com.codename1.gradle;

import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import com.codename1.project.ProjectLayout;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/// A [ProjectHost] that answers from another, for a task that needs to change
/// one answer.
public class DelegatingProjectHost implements ProjectHost {
    private final ProjectHost delegate;

    /// Answers from `delegate`.
    public DelegatingProjectHost(ProjectHost delegate) {
        this.delegate = delegate;
    }

    @Override
    public Log log() {
        return delegate.log();
    }

    @Override
    public ProjectLayout layout() {
        return delegate.layout();
    }

    @Override
    public File cn1ProjectDir() {
        return delegate.cn1ProjectDir();
    }

    @Override
    public File baseDir() {
        return delegate.baseDir();
    }

    @Override
    public File buildDirectory() {
        return delegate.buildDirectory();
    }

    @Override
    public File outputDirectory() {
        return delegate.outputDirectory();
    }

    @Override
    public String finalName() {
        return delegate.finalName();
    }

    @Override
    public String groupId() {
        return delegate.groupId();
    }

    @Override
    public Properties projectProperties() {
        return delegate.projectProperties();
    }

    @Override
    public Properties userProperties() {
        return delegate.userProperties();
    }

    @Override
    public List<String> compileClasspathElements() throws BuildExecutionException {
        return delegate.compileClasspathElements();
    }

    @Override
    public List<String> runtimeClasspathElements() throws BuildExecutionException {
        return delegate.runtimeClasspathElements();
    }

    @Override
    public List<String> compileSourceRoots() {
        return delegate.compileSourceRoots();
    }

    @Override
    public Collection<BuildArtifact> artifacts() {
        return delegate.artifacts();
    }

    @Override
    public Set<String> neededWithoutDesktopRuntime() {
        return delegate.neededWithoutDesktopRuntime();
    }

    @Override
    public File getJar(BuildArtifact artifact) {
        return delegate.getJar(artifact);
    }

    @Override
    public File getJar(String groupId, String artifactId, String classifier) {
        return delegate.getJar(groupId, artifactId, classifier);
    }

    @Override
    public long sessionStartTime() {
        return delegate.sessionStartTime();
    }

    @Override
    public long sourcesModificationTime() throws IOException {
        return delegate.sourcesModificationTime();
    }

    @Override
    public void attachArtifact(String type, String classifier, File file) {
        delegate.attachArtifact(type, classifier, file);
    }

    @Override
    public String codenameOneVersion() {
        return delegate.codenameOneVersion();
    }

    @Override
    public String pluginVersion() {
        return delegate.pluginVersion();
    }
}
