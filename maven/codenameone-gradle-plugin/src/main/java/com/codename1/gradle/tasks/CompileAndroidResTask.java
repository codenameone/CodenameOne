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
import com.codename1.maven.AndroidResourceRunner;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Compiles `src/main/android` -- an Android application's manifest, `res/`
/// and `assets/` -- into R classes, the generated activity/view factory, and
/// the resource table the Android compatibility runtime reads. The same
/// [AndroidResourceRunner] as the Maven plugin's `compile-android-res` goal.
@CacheableTask
public abstract class CompileAndroidResTask extends Cn1Task {

    /// `src/main/android`, minus its Java and Kotlin, which javac compiles.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The `codenameone-android-compat` jar (its framework symbols).
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getCompatJar();

    @Input
    @Optional
    public abstract Property<String> getMainPackage();

    @Input
    @Optional
    public abstract Property<String> getMainClass();

    /// Source roots in which an existing main class suppresses the generated one.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceRoots();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getResourcesDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getStateDirectory();

    @TaskAction
    public void compile() {
        File androidDir = layout().androidSourceDir();
        File jar = null;
        for (File f : getCompatJar().getFiles()) {
            jar = f;
        }
        List<File> roots = new ArrayList<File>(getSourceRoots().getFiles());
        try {
            new AndroidResourceRunner(androidDir, getOutputDirectory().get().getAsFile(),
                    getResourcesDirectory().get().getAsFile(), getStateDirectory().get().getAsFile(), jar, null,
                    getMainPackage().getOrNull(), getMainClass().getOrNull(), roots, log()).run();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
