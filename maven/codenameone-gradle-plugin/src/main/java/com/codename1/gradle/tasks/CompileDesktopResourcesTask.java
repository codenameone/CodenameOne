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

import com.codename1.fxml.DesktopResourceCompiler;
import com.codename1.maven.CompatLayers;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Compiles the FXML documents and style sheets of `src/main/desktop/resources`
/// for the JavaFX compatibility layer: each document into Java source that
/// builds its tree, each sheet into the binary table the layer's style
/// engine reads. The same [DesktopResourceCompiler] as the Maven plugin's
/// `prepare-desktop-sources` goal.
///
/// A document or a sheet the compiler rejects fails the task with every
/// error it found, each as `file:line:column: message`; what it only warns
/// about is logged.
@CacheableTask
public abstract class CompileDesktopResourcesTask extends Cn1Task {

    /// The `**/*.fxml` and `**/*.css` files under the desktop resources.
    /// The task is skipped when there are none.
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The directory the sources are in, which their resource paths are
    /// relative to. Not an input of its own: [#getSources()] is what is
    /// read from it.
    @Internal
    public abstract DirectoryProperty getResourcesRoot();

    /// The application's compile class path, where the classes the
    /// documents name are read from.
    @Classpath
    public abstract ConfigurableFileCollection getCompileClasspath();

    /// The generated Java sources.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// The compiled style sheets.
    @OutputDirectory
    public abstract DirectoryProperty getResourcesDirectory();

    @TaskAction
    public void compile() {
        List<File> classpath = new ArrayList<File>(getCompileClasspath().getFiles());
        if (CompatLayers.runtimeJar(CompatLayers.JAVAFX, classpath) == null) {
            getLogger().debug("The JavaFX layer is not on the class path; no FXML or style sheet is compiled");
            return;
        }
        File root = getResourcesRoot().get().getAsFile();
        DesktopResourceCompiler compiler = new DesktopResourceCompiler(Collections.singletonList(root), classpath,
                getOutputDirectory().get().getAsFile(), getResourcesDirectory().get().getAsFile(),
                new DesktopResourceCompiler.Log() {
                    @Override
                    public void info(String message) {
                        getLogger().lifecycle(message);
                    }

                    @Override
                    public void warn(String message) {
                        getLogger().warn(message);
                    }
                });
        List<String> errors;
        try {
            errors = compiler.run();
        } catch (IOException ex) {
            throw new GradleException("Could not compile the FXML documents and style sheets of " + root + ": "
                    + ex.getMessage(), ex);
        }
        if (!errors.isEmpty()) {
            StringBuilder all = new StringBuilder();
            for (String error : errors) {
                all.append('\n').append(error);
            }
            throw new GradleException(errors.size() + " error(s) in the FXML documents and style sheets of " + root
                    + ":" + all);
        }
    }
}
