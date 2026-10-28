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
import com.codename1.maven.PythonProjectBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/// Transpiles `src/main/python` to Java sources, which the main source set
/// compiles. The same [PythonProjectBuilder] as the Maven plugin's
/// `transpile-python` goal. Runs without Python sources too, to empty its
/// output of what an earlier build generated.
@CacheableTask
public abstract class TranspilePythonTask extends Cn1Task {
    /// `src/main/python`.
    @Internal
    public abstract DirectoryProperty getPythonDirectory();

    /// The `.py` files under it.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The module run as `__main__`, or unset for `main`/a library.
    @Input
    @Optional
    public abstract Property<String> getMainModule();

    /// The compile class path `from java.<package> import <Class>` resolves
    /// against.
    @Classpath
    public abstract ConfigurableFileCollection getJavaClassPath();

    /// The `codename1.arg.python.virtualThreadStack` build hint, or unset for
    /// the transpiler's default.
    @Input
    @Optional
    public abstract Property<String> getVirtualThreadStack();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void transpile() {
        try {
            new PythonProjectBuilder(getPythonDirectory().get().getAsFile(), getOutputDirectory().get().getAsFile(),
                    getMainModule().getOrNull(), log())
                    .classPath(new java.util.ArrayList<java.io.File>(getJavaClassPath().getFiles()))
                    .virtualThreadStack(getVirtualThreadStack().getOrNull()).run();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
