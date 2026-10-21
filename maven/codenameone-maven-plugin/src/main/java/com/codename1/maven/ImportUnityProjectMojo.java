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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/// Imports a Unity project into this Codename One application.
///
/// ```
/// mvn cn1:import-unity-project -Dsource=/path/to/MyUnityGame
/// ```
///
/// Copies the project's `Assets` and `ProjectSettings` into
/// `common/src/main/unity` -- never `Library`, `Temp` or `obj`, which the
/// Unity editor derives from them -- adds the runtime dependency to the common
/// module, and makes the Codename One main class run the project's first
/// scene. Build and run as usual afterwards. Importing again mirrors the Unity
/// project afresh.
@Mojo(name = "import-unity-project", requiresProject = true, aggregator = true)
public class ImportUnityProjectMojo extends AbstractCN1Mojo {

    /// The Unity project: the directory that holds `Assets`.
    @Parameter(property = "source")
    private File source;

    /// The same, under the name that matches `cn1.android.import`; it wins
    /// when both are given, being the one that cannot be meant for another
    /// plugin.
    @Parameter(property = "cn1.unity.import")
    private File unityImport;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File from = unityImport != null ? unityImport : source;
        if (from == null) {
            throw new MojoFailureException("Name the Unity project to import: -Dsource=/path/to/MyUnityGame");
        }
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        if (!UnityProjectBuilder.isUnityProject(from)) {
            // Before the pom is touched: a mistyped path must change nothing.
            throw new MojoFailureException(from + " is not a Unity project: it needs both an Assets and a"
                    + " ProjectSettings directory");
        }
        wireUnityCompat(common);
        String pkg = properties == null ? null : properties.getProperty("codename1.packageName");
        String main = properties == null ? null : properties.getProperty("codename1.mainName");
        UnityProjectImporter.Result r;
        try {
            r = new UnityProjectImporter(MavenLog.of(getLog())).importProject(from, common, pkg, main);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.copiedFiles + " files (" + r.scripts + " scripts, " + r.scenes
                + " scenes) into " + new File(common, "src/main/unity")
                + (r.removedFiles == 0 ? "" : "; removed " + r.removedFiles + " the Unity project no longer has"));
        for (String i : r.ignored) {
            getLog().warn("  not built: " + i);
        }
        if (r.productName != null) {
            getLog().info("The Unity project is named \"" + r.productName + "\"; set codename1.displayName in"
                    + " codenameone_settings.properties to show the same name.");
        }
    }

    /// A project generated before Unity compatibility existed has none of its
    /// wiring, and the imported project would not build. Adds it to the
    /// common pom, or stops before copying anything, naming what to add.
    private void wireUnityCompat(File common) throws MojoExecutionException, MojoFailureException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        UnityPomUpdater u = new UnityPomUpdater(pom);
        if (!u.manual.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nothing was imported: " + pomFile
                    + " needs Unity compatibility wired in by hand. Add:\n");
            for (String m : u.manual) {
                sb.append("\n- ").append(m);
            }
            throw new MojoFailureException(sb.toString());
        }
        if (u.changed) {
            try {
                Files.write(pomFile.toPath(), u.pom.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write " + pomFile, e);
            }
            getLog().info("Added Unity compatibility to " + pomFile);
        }
    }
}
