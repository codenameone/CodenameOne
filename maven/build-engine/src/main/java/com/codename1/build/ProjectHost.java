/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.build;

import com.codename1.project.ProjectLayout;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/// What the build engine needs to know about the project being built, answered
/// by the build tool running it.
///
/// The Maven plugin answers from its `MavenProject` and session, the Gradle
/// plugin from its project model and resolved configurations. Everything
/// behind this interface is the build tool's business; everything the engine
/// decides from these answers is shared, so Maven and Gradle builds of the
/// same application make the same decisions.
public interface ProjectHost {
    /// The logger.
    Log log();

    /// The project's layout.
    ProjectLayout layout();

    /// The directory holding `codenameone_settings.properties`.
    File cn1ProjectDir();

    /// The module or project directory the build runs in.
    File baseDir();

    /// The build output directory (`target` under Maven, `build` under Gradle).
    File buildDirectory();

    /// Where the project's compiled classes are.
    File outputDirectory();

    /// The base name outputs are written under.
    String finalName();

    /// The project's group id. A dependency that shares it is part of the
    /// application rather than a library.
    String groupId();

    /// Project-level properties (Maven's `project.getProperties()`; Gradle's
    /// `codename1.*` project properties).
    Properties projectProperties();

    /// The properties given on the command line (`-D` for Maven, `-P` or
    /// `-D` for Gradle). `codename1.*` entries override the settings file.
    Properties userProperties();

    /// The compile classpath, the project's own output first.
    List<String> compileClasspathElements() throws BuildExecutionException;

    /// The runtime classpath, the project's own output first.
    List<String> runtimeClasspathElements() throws BuildExecutionException;

    /// The source roots the project compiles.
    List<String> compileSourceRoots();

    /// Every resolved dependency.
    Collection<BuildArtifact> artifacts();

    /// The dependency keys (`groupId:artifactId:classifier`) the application's
    /// compile dependencies need once the desktop runtime aggregator is
    /// removed, or null when that cannot be determined.
    Set<String> neededWithoutDesktopRuntime();

    /// The file of a resolved dependency, resolving it if the tool must.
    File getJar(BuildArtifact artifact);

    /// The jar of `groupId:artifactId[:classifier]` among the project's
    /// dependencies or the build tool's own, or null.
    File getJar(String groupId, String artifactId, String classifier);

    /// When the current build invocation started, in epoch milliseconds, or
    /// `Long.MAX_VALUE` when unknown.
    long sessionStartTime();

    /// The newest modification time among the project's sources.
    long sourcesModificationTime() throws IOException;

    /// Records a build output beside the project's primary artifact.
    ///
    /// @param classifier null for none
    void attachArtifact(String type, String classifier, File file);

    /// The Codename One version being built against.
    String codenameOneVersion();

    /// The build plugin's own version.
    String pluginVersion();
}
