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

/// Imports a Swing or JavaFX project into this Codename One application.
///
/// ```
/// mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp
///     [-Dcn1.desktop.module=app] [-Dcn1.desktop.mainClass=com.example.Main]
/// ```
///
/// Copies the project's (or the named module's) `src/main/java`,
/// `src/main/kotlin` and `src/main/resources` into
/// `common/src/main/desktop`, records how the application starts in
/// `cn1-desktop.properties` there, and lists the project's dependencies the
/// desktop layers do and do not cover. Build and run as usual afterwards.
/// See [DesktopProjectImporter].
@Mojo(name = "import-desktop-project", requiresProject = true, aggregator = true)
public class ImportDesktopProjectMojo extends AbstractCN1Mojo {

    /// A Maven or Gradle desktop project, or one module of it.
    @Parameter(property = "cn1.desktop.import", required = true)
    private File source;

    /// The module to import, for a project whose root has no sources and
    /// more than one module that does.
    @Parameter(property = "cn1.desktop.module")
    private String module;

    /// The class the application starts through, when the sources and the
    /// project's build leave more than one to choose from.
    @Parameter(property = "cn1.desktop.mainClass")
    private String mainClass;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        DesktopProjectImporter.Result r;
        try {
            wireDesktopCompat(common, DesktopProjectImporter.moduleDir(source, module));
            r = new DesktopProjectImporter(MavenLog.of(getLog())).importProject(source, module, common, mainClass);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.copiedFiles + " files into " + new File(common, "src/main/desktop"));
        if (r.mainClass != null) {
            getLog().info("The application starts through " + r.mainClass + " (" + r.kind + ")");
        }
        for (String dropped : r.droppedModuleInfo) {
            getLog().info("  left out: " + dropped + " (a Codename One application is not a Java module)");
        }
        for (String c : r.covered) {
            getLog().info("  supported: " + c);
        }
        for (String u : r.uncovered) {
            getLog().warn("  NOT supplied by the desktop compatibility layers, and not copied: " + u);
        }
    }

    /// A project generated before the desktop layers existed has none of
    /// their wiring, and the imported sources would not build. Adds it to the
    /// common pom, or stops before copying anything, naming what to add.
    private void wireDesktopCompat(File common, File moduleDir) throws MojoExecutionException, MojoFailureException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()), "UTF-8");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        DesktopPomUpdater u = new DesktopPomUpdater(pom, hasKotlin(new File(moduleDir, "src/main")));
        if (!u.manual.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nothing was imported: " + pomFile
                    + " needs the desktop compatibility layers wired in by hand. Add:\n");
            for (String m : u.manual) {
                sb.append("\n- ").append(m);
            }
            throw new MojoFailureException(sb.toString());
        }
        if (u.changed) {
            try {
                java.nio.file.Files.write(pomFile.toPath(), u.pom.getBytes("UTF-8"));
            } catch (java.io.IOException e) {
                throw new MojoExecutionException("Cannot write " + pomFile, e);
            }
            getLog().info("Added the desktop compatibility layers to " + pomFile);
        }
    }

    private static boolean hasKotlin(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (f.isDirectory() ? hasKotlin(f) : f.getName().endsWith(".kt")) {
                return true;
            }
        }
        return false;
    }
}
