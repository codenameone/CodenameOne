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
package com.codename1.gradle.tasks;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.gradle.GradleHostFactory;
import com.codename1.maven.AppBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Sends a build to the Codename One build server, or runs one of the local
/// build targets.
///
/// The same [AppBuilder] the Maven plugin's `cn1:build` runs: the staged
/// jar-with-dependencies, the class closure check, the settings merge
/// (annotations, command-line hints, cn1lib properties) and both preflights are
/// shared code. `-Pcodename1.stageOnly=true` stops once the upload jar is
/// assembled, exactly as `-Dcodename1.stageOnly` does for Maven.
@DisableCachingByDefault(because = "A build submission or a local native build is never cacheable")
public abstract class Cn1BuildTask extends Cn1Task {
    /// `codename1.platform`: ios, android, javase, javascript, win, linux.
    @Input
    public abstract Property<String> getPlatform();

    /// `codename1.buildTarget`: ios-device, android-source, ...
    @Input
    public abstract Property<String> getBuildTarget();

    /// Wait for a cloud build and download its result.
    @Input
    public abstract Property<Boolean> getAutomated();

    /// Stop once the upload jar is staged and checked.
    @Input
    public abstract Property<Boolean> getStageOnly();

    /// Open a generated Xcode or Android Studio project.
    @Input
    public abstract Property<Boolean> getOpen();

    /// What is uploaded, in order: the compiled classes and resources, the
    /// platform's native sources, then the dependencies.
    @Classpath
    public abstract ConfigurableFileCollection getUploadClasspath();

    /// The resolved dependencies behind [getUploadClasspath()], encoded, so the
    /// engine can tell the server-supplied ones from the application's own.
    @Input
    public abstract ListProperty<String> getArtifacts();

    /// `codenameone-core` and `java-runtime`, which the server supplies and a
    /// local build needs to translate against.
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getFrameworkJars();

    /// The source roots the project compiles.
    @Input
    public abstract ListProperty<String> getSourceRoots();

    /// The base name outputs are written under.
    @Input
    public abstract Property<String> getFinalName();

    /// The project's group; a dependency sharing it is part of the application.
    @Input
    @Optional
    public abstract Property<String> getGroupId();

    /// Project-level `codename1.*` properties.
    @Input
    public abstract MapProperty<String, String> getProjectProperties();

    @TaskAction
    public void build() {
        final java.io.File work = new java.io.File(layout().buildDir(), "codenameone" + java.io.File.separator
                + getName());
        AppBuilder builder = new TaskAppBuilder(GradleHostFactory.create(this, log(), layout(),
                getFinalName().get(), getGroupId().getOrElse(""), getProjectProperties().get(),
                getUserProperties().get(), getUploadClasspath(), getSourceRoots().get(), getArtifacts().get(),
                getFrameworkJars(), getCodenameOneVersion().get()), work);
        builder.platform(getPlatform().get())
                .buildTarget(getBuildTarget().get())
                .automated(getAutomated().get())
                .stageOnly(getStageOnly().get())
                .open(getOpen().get());
        try {
            builder.execute();
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// All platforms share this project's build directory, so each build task
    /// keeps its own Ant project; see AppBuilder.workDirectory.
    private static final class TaskAppBuilder extends AppBuilder {
        private final java.io.File work;

        TaskAppBuilder(com.codename1.build.ProjectHost host, java.io.File work) {
            super(host);
            this.work = work;
        }

        @Override
        protected java.io.File workDirectory() {
            return work;
        }
    }
}
