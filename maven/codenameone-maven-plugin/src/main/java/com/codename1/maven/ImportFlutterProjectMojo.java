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

/// Imports a Flutter project into this Codename One application.
///
/// ```
/// mvn cn1:import-flutter-project -Dcn1.flutter.import=/path/to/flutter_app
/// ```
///
/// Copies the project's `lib` tree into `common/src/main/flutter`, where
/// `cn1:transcode-flutter` transpiles it at build time, copies the assets and
/// fonts its `pubspec.yaml` declares, makes the Codename One main class start
/// the Dart `main()`, and lists the project's pub dependencies the Flutter
/// runtime's stubs do and do not cover. Build and run as usual afterwards --
/// no Dart SDK is needed, but the build must run on JDK 17+.
@Mojo(name = "import-flutter-project", requiresProject = true, aggregator = true)
public class ImportFlutterProjectMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.flutter.import", required = true)
    private File source;

    /// The package `cn1:transcode-flutter` (`TranscodeFlutterMojo`) generates
    /// `FlutterRegistry` into -- same property and default as that Mojo's own
    /// `@Parameter`, so a project that sets `cn1.flutter.package` (as a
    /// project property, since the two goals never share one `<execution>`'s
    /// `<configuration>`) gets an entry point that calls into the package the
    /// transpile actually used, instead of the hardcoded default.
    @Parameter(property = "cn1.flutter.package", defaultValue = "com.codename1.generated.flutter")
    private String flutterPackage;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        wireFlutterRuntime(common);
        String pkg = properties == null ? null : properties.getProperty("codename1.packageName");
        String main = properties == null ? null : properties.getProperty("codename1.mainName");
        FlutterProjectImporter.Result r;
        try {
            r = new FlutterProjectImporter(MavenLog.of(getLog())).importProject(source, common, pkg, main,
                    flutterPackage);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.copiedFiles + " Dart file(s) and " + r.copiedAssets + " asset(s)/font(s) into "
                + new File(common, "src/main/flutter"));
        if (r.appName != null) {
            getLog().info("pubspec.yaml: " + r.appName + (r.appVersion != null ? " " + r.appVersion : ""));
        }
        for (String c : r.covered) {
            getLog().info("  supported: " + c);
        }
        for (String u : r.uncovered) {
            getLog().warn("  NOT supported by the Flutter runtime: " + u);
        }
        for (String a : r.missingAssets) {
            getLog().warn("  asset declared in pubspec.yaml but not found: " + a);
        }
        if (!r.unresolvedPackages.isEmpty()) {
            // Each one was already warned individually, with the remedy, as it was hit.
            getLog().warn("  " + r.unresolvedPackages.size() + " pub package(s) could not be resolved: "
                    + r.unresolvedPackages);
        }
    }

    /// A project generated before Flutter support existed, or whose archetype
    /// pom still has the runtime dependency commented out, has no working
    /// wiring for the imported sources. Adds it to the common pom, or stops
    /// before copying anything, naming what to add.
    private void wireFlutterRuntime(File common) throws MojoExecutionException, MojoFailureException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()), "UTF-8");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        FlutterPomUpdater u = new FlutterPomUpdater(pom);
        if (!u.manual.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nothing was imported: " + pomFile
                    + " needs the Flutter runtime wired in by hand. Add:\n");
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
            getLog().info("Wired the Flutter runtime into " + pomFile);
        }
    }
}
