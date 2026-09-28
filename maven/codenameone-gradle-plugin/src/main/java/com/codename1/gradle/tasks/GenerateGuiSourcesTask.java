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

import com.codename1.build.AntSupport;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.CodenameOneUpdater;
import com.codename1.maven.GuiSourcesGenerator;
import com.codename1.project.ProjectLayout;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;

/// Generates Java sources from the legacy GUI builder's XML
/// (`src/main/guibuilder`) and from CodeRAD view templates
/// (`src/main/rad/views`), as the Maven plugin's `generate-gui-sources` goal
/// does. A project with neither does nothing here.
@DisableCachingByDefault(because = "The legacy step writes into src/main/java")
public abstract class GenerateGuiSourcesTask extends Cn1Task {
    /// The GUI builder XML and the view templates.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// Where the generated view classes go; a source root of the main source set.
    @OutputDirectory
    public abstract DirectoryProperty getRadOutputDirectory();

    @TaskAction
    public void generate() {
        ProjectLayout layout = layout();
        GuiSourcesGenerator generator = new GuiSourcesGenerator(log(), layout.projectDir(), layout.radViewsDir(),
                getRadOutputDirectory().get().getAsFile(), new File(layout.buildDir(), "generated-sources"));
        try {
            if (layout.guiBuilderDir().isDirectory()) {
                // The legacy generator ships in the build client, which a cloud build
                // installs anyway; fetch it now if this is the first thing to need it.
                File client = CodenameOneUpdater.buildClientJar();
                new CodenameOneUpdater(log(), AntSupport.newProject(layout.projectDir())).update(false,
                        new File(layout.buildDir(), "codenameone"), layout.projectDir(), client);
                generator.generateLegacyGui(client, layout.javaSourceDir(), layout.guiBuilderDir());
            }
            generator.generateRadViews();
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
