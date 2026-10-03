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

/// Imports an Android Studio project into this Codename One application.
///
/// ```
/// mvn cn1:import-android-project -Dcn1.android.import=/path/to/MyAndroidApp [-Dcn1.android.module=app]
/// ```
///
/// Copies the module's `src/main` into `common/src/main/android`, makes the
/// Codename One main class start the Android application, and lists the
/// module's Gradle dependencies the compatibility runtime does and does not
/// cover. Build and run as usual afterwards.
@Mojo(name = "import-android-project", requiresProject = true, aggregator = true)
public class ImportAndroidProjectMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.android.import", required = true)
    private File source;

    @Parameter(property = "cn1.android.module", defaultValue = "app")
    private String module;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        wireAndroidCompat(common);
        String pkg = properties == null ? null : properties.getProperty("codename1.packageName");
        String main = properties == null ? null : properties.getProperty("codename1.mainName");
        AndroidProjectImporter.Result r;
        try {
            r = new AndroidProjectImporter(MavenLog.of(getLog())).importProject(source, module, common, pkg, main);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.copiedFiles + " files into " + new File(common, "src/main/android"));
        for (String c : r.covered) {
            getLog().info("  supported: " + c);
        }
        for (String u : r.uncovered) {
            getLog().warn("  NOT supported by the Android compatibility runtime: " + u);
        }
        if (r.applicationId != null && pkg != null && !r.applicationId.equals(pkg)) {
            getLog().info("The Android applicationId is " + r.applicationId + "; codename1.packageName is " + pkg
                    + ". Change codename1.packageName to keep the same store identity.");
        }
    }

    /// A project generated before Android compatibility existed has none of
    /// its wiring, and the imported sources would not build. Adds it to the
    /// common pom, or stops before copying anything, naming what to add.
    private void wireAndroidCompat(File common) throws MojoExecutionException, MojoFailureException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()), "UTF-8");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        File main = AndroidProjectImporter.mainDir(source, module);
        AndroidPomUpdater u = new AndroidPomUpdater(pom, main != null && hasKotlin(main));
        if (!u.manual.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nothing was imported: " + pomFile
                    + " needs Android compatibility wired in by hand. Add:\n");
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
            getLog().info("Added Android compatibility to " + pomFile);
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
