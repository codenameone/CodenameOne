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

import com.codename1.builders.BuildException;
import com.codename1.maven.FlutterProjectBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;

/// Transpiles `src/main/flutter` -- Flutter/Dart sources and their assets --
/// into Java on the Codename One Flutter runtime. The same
/// [FlutterProjectBuilder] as the Maven plugin's `transcode-flutter` goal.
///
/// The flattened assets go to a directory of this task's own, an output
/// directory of the main source set, never into `compileJava`'s destination:
/// javac empties that on a full recompile. For the same reason no plugin-pack
/// class sweep is needed here -- Gradle's incremental compile removes the
/// class of a source the transpiler deleted.
///
/// Without a Flutter project the task is skipped, unless an earlier build of
/// one left output behind: then it runs once more to remove it.
///
/// Not cacheable: the transpiler keeps its own digest of everything it read,
/// and skips the work itself when nothing changed.
@DisableCachingByDefault(because = "The transpiler keeps its own incremental state")
public abstract class TranscodeFlutterTask extends Cn1Task {

    /// `src/main/flutter`.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getFlutterDirectory();

    /// The compile class path: its `META-INF/dart/*.dart` stubs are the Dart
    /// API the program compiles against.
    @Classpath
    public abstract ConfigurableFileCollection getStubClasspath();

    /// The application's own Java, which can name a plugin pack.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getJavaSourceRoots();

    /// `cn1.flutter.package`.
    @Input
    public abstract Property<String> getPackageName();

    /// The generated Java source.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// The flattened assets, a resources root of the main source set.
    @OutputDirectory
    public abstract DirectoryProperty getAssetsDirectory();

    /// The transpiler's state and the record of the assets written.
    @LocalState
    public abstract DirectoryProperty getStateDirectory();

    @TaskAction
    public void transcode() {
        File state = getStateDirectory().get().getAsFile();
        try {
            new FlutterProjectBuilder(getFlutterDirectory().get().getAsFile(),
                    getOutputDirectory().get().getAsFile(), getAssetsDirectory().get().getAsFile(),
                    new File(state, "assets.lst"), new File(state, "state.txt"),
                    getPackageName().get(), log())
                    .stubClasspath(getStubClasspath().getFiles())
                    .javaSourceRoots(getJavaSourceRoots().getFiles())
                    .build();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// Whether there is a Flutter project to build, or the output of one that
    /// is no longer there to remove: the generated sources are a source root
    /// and the assets part of the main output, so what a deleted project left
    /// in them would ship until `clean`.
    public boolean hasWork() {
        if (FlutterProjectBuilder.isFlutterProject(getFlutterDirectory().get().getAsFile())) {
            return true;
        }
        for (DirectoryProperty d : new DirectoryProperty[] {getOutputDirectory(), getAssetsDirectory()}) {
            String[] children = d.get().getAsFile().list();
            if (children != null && children.length > 0) {
                return true;
            }
        }
        return false;
    }
}
