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

import com.codename1.maven.SimulatorSupport;
import com.codename1.project.ProjectDescriptor;
import com.codename1.project.ProjectLayout;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/// Writes what the simulator reads at startup: `simulator.properties` (the
/// classpaths hot reload and live CSS reload fork with, and the command-line
/// build hints) and the project descriptor the desktop tools read.
@DisableCachingByDefault(because = "Writes two small files; not worth caching")
public abstract class PrepareSimulatorTask extends Cn1Task {
    /// The compile classpath hot reload recompiles against.
    @Internal
    public abstract ConfigurableFileCollection getCompileClasspath();

    /// The CSS compiler CLI classpath live CSS reload forks with.
    @Internal
    public abstract ConfigurableFileCollection getCssCompilerClasspath();

    /// The two classpaths as the path lists this task writes. The lists, not the
    /// files' contents, are what the output records, so these are the inputs:
    /// adding or upgrading a dependency changes them and regenerates the
    /// properties -- a stale list made hot reload recompile against the old
    /// classpath -- while an ordinary recompile of the project does not.
    @Input
    public String getClasspathPaths() {
        return getCompileClasspath().getAsPath() + "\n" + getCssCompilerClasspath().getAsPath();
    }

    /// `build/codenameone/simulator.properties`.
    @OutputFile
    public abstract RegularFileProperty getSimulatorProperties();

    /// `build/codenameone/project.properties`.
    @OutputFile
    public abstract RegularFileProperty getDescriptor();

    @TaskAction
    public void prepare() {
        ProjectLayout layout = layout();
        Properties effective = effectiveSettings();
        try {
            SimulatorSupport.writeSimulatorProperties(getSimulatorProperties().get().getAsFile(),
                    getCompileClasspath().getAsPath(), getCssCompilerClasspath().getAsPath(),
                    toProperties(getUserProperties().get()), effective);
            writeDescriptor(layout, getDescriptor().get().getAsFile(), effective, null);
        } catch (IOException ex) {
            throw new GradleException("Failed to write the simulator's properties", ex);
        }
    }

    static Properties toProperties(java.util.Map<String, String> map) {
        Properties p = new Properties();
        p.putAll(map);
        return p;
    }

    /// Writes the project descriptor, with the resolved main class and the
    /// source roots the build compiles.
    static void writeDescriptor(ProjectLayout layout, File file, Properties effective, List<String> extraRoots)
            throws IOException {
        ProjectDescriptor d = ProjectDescriptor.fromLayout(layout);
        d.set("sourceEncoding", "UTF-8");
        if (effective.getProperty("codename1.mainName") != null) {
            d.set("mainName", effective.getProperty("codename1.mainName").trim());
        }
        if (effective.getProperty("codename1.packageName") != null) {
            d.set("packageName", effective.getProperty("codename1.packageName").trim());
        }
        if (extraRoots != null) {
            for (String r : extraRoots) {
                d.add("sourceRoot", r);
            }
        }
        d.write(file);
    }
}
