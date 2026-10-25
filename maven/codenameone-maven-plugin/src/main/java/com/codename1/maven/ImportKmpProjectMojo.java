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
package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/// Imports a Kotlin Multiplatform / Compose Multiplatform module into this
/// Codename One application.
///
/// ```
/// mvn cn1:import-kmp-project -Dcn1.kmp.import=/path/to/MyKmpApp
///     [-Dcn1.kmp.module=composeApp] [-Dcn1.kmp.entry=com.example.App]
/// ```
///
/// Copies the module's `commonMain` and JVM source set into
/// `common/src/main/kmp`, where the archetype's `compose` profile compiles
/// them, and writes the application's main class so that it shows the root
/// composable. See [KmpProjectImporter].
@Mojo(name = "import-kmp-project", requiresProject = true, aggregator = true)
public class ImportKmpProjectMojo extends AbstractCN1Mojo {

    /// A Kotlin Multiplatform project, or one module of it.
    @Parameter(property = "cn1.kmp.import", required = true)
    private File source;

    /// The module to import, when more than one has a `src/commonMain`.
    @Parameter(property = "cn1.kmp.module")
    private String module;

    /// The fully qualified root composable, when it is not the only `App()`.
    @Parameter(property = "cn1.kmp.entry")
    private String entry;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        KmpProjectImporter.Result r;
        try {
            r = new KmpProjectImporter().importProject(source, module, common, entry,
                    properties == null ? null : properties.getProperty("codename1.packageName"),
                    properties == null ? null : properties.getProperty("codename1.mainName"));
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.getCopiedFiles() + " files from " + r.getModule() + " into "
                + new File(common, "src/main/kmp"));
        getLog().info("The application shows " + r.getEntry() + "() from " + r.getMainClass());
        if (r.getSetAside() != null) {
            getLog().info("The previous main class is kept as " + r.getSetAside().getName());
        }
        for (java.io.File f : r.getSkippedEntryPoints()) {
            getLog().info("  left out: " + f + " (it opens a desktop window; " + r.getMainClass().getName()
                    + " starts the application)");
        }
        for (String set : r.getSkippedSourceSets()) {
            getLog().info("  left out: " + set + " (Codename One provides the platforms itself)");
        }
    }
}
