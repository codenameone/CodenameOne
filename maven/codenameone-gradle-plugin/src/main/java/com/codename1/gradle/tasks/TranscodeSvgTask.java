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
import com.codename1.maven.SvgTranscodeRunner;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/// Transcodes the SVG and Lottie assets under `src/main/css` and
/// `src/main/svg` into Java sources, which the main source set compiles. The
/// same [SvgTranscodeRunner] as the Maven plugin's `transcode-svg` goal.
@CacheableTask
public abstract class TranscodeSvgTask extends Cn1Task {
    /// The vector sources and the CSS that references them.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The generated Java sources.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// The 1x1 placeholders the CSS compiler sees for referenced images.
    @OutputDirectory
    public abstract DirectoryProperty getPlaceholderDirectory();

    @TaskAction
    public void transcode() {
        // Every run regenerates everything, so start empty: the runner neither
        // deletes the class of an asset that was removed nor rewrites SVGRegistry
        // when it looks newer than what is left, and the deleted image stayed
        // compiled and registered until a clean build.
        for (DirectoryProperty dir : java.util.Arrays.asList(getOutputDirectory(), getPlaceholderDirectory())) {
            java.io.File f = dir.get().getAsFile();
            try {
                if (f.isDirectory()) {
                    org.apache.commons.io.FileUtils.cleanDirectory(f);
                }
            } catch (java.io.IOException ex) {
                throw new GradleException("Could not clear " + f, ex);
            }
        }
        try {
            new SvgTranscodeRunner(layout().projectDir(), null, getOutputDirectory().get().getAsFile(),
                    getPlaceholderDirectory().get().getAsFile(), null, log()).run();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
