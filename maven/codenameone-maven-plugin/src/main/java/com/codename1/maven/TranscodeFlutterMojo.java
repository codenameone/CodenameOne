/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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


import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@link FlutterProjectBuilder}: transpiles Flutter/Dart sources under {@code src/main/flutter} into Java
 * source targeting the Codename One Flutter runtime
 * ({@code codenameone-flutter-runtime}), so Flutter UI code runs as plain
 * Codename One components at native speed -- no Dart VM or Flutter engine.
 *
 * <h3>Source layout</h3>
 * <ul>
 *   <li>{@code src/main/flutter/**&#47;*.dart} -- Dart sources (whole-program
 *       transpile; subdirectories allowed)</li>
 *   <li>{@code src/main/flutter/assets/**} and {@code src/main/flutter/packages/**} --
 *       bundled assets, flattened into the
 *       build output (Codename One resources are flat on every port) so
 *       {@code Image.asset(...)} resolves</li>
 *   <li>Any other pubspec {@code assets:}/{@code fonts:} key --
 *       {@code fonts/MyFont.ttf}, {@code images/logo.png} -- is flattened the
 *       same way, but only when {@code cn1:import-flutter-project} recorded it
 *       declared (see {@code FlutterProjectImporter.ASSET_KEYS_RECORD}); a
 *       hand-wired tree with no such record relies on the two roots above
 *       alone</li>
 * </ul>
 *
 * <p>When the directory does not exist the goal is a silent no-op. When it
 * exists, generated sources land in {@code target/generated-sources/flutter}
 * (registered as a compile source root). Generated code is Java 17 source,
 * so the build must run on JDK 17+ -- checked here with a friendly error.
 * The transpile is whole-program with a digest-based fast skip; unchanged
 * outputs are not rewritten, keeping incremental javac warm.</p>
 */
@Mojo(name = "transcode-flutter", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class TranscodeFlutterMojo extends AbstractCN1Mojo {

    private static final String RUNTIME_ARTIFACT = FlutterProjectBuilder.RUNTIME_ARTIFACT;

    @Parameter(property = "cn1.flutter.sourceDir", defaultValue = "${project.basedir}/src/main/flutter")
    private File flutterSourceDir;

    @Parameter(property = "cn1.flutter.outputDir", defaultValue = "${project.build.directory}/generated-sources/flutter")
    private File flutterOutputDir;

    @Parameter(property = "cn1.flutter.package", defaultValue = FlutterProjectBuilder.DEFAULT_PACKAGE)
    private String flutterPackage;

    @Parameter(property = "cn1.flutter.stateFile", defaultValue = "${project.build.directory}/flutter-transpiler/state.txt")
    private File stateFile;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        boolean flutter = FlutterProjectBuilder.isFlutterProject(flutterSourceDir);
        if (flutter) {
            checkRuntimeDependency();
        }
        List<File> classpath = new ArrayList<File>();
        // runtime dependencies contribute API stubs via META-INF/dart/*.dart
        for (Object o : project.getArtifacts()) {
            org.apache.maven.artifact.Artifact a = (org.apache.maven.artifact.Artifact) o;
            classpath.add(a.getFile());
        }
        List<File> javaRoots = new ArrayList<File>();
        for (Object root : project.getCompileSourceRoots()) {
            javaRoots.add(new File(String.valueOf(root)));
        }
        File classes = new File(project.getBuild().getOutputDirectory());
        try {
            new FlutterProjectBuilder(flutterSourceDir, flutterOutputDir, classes,
                    new File(project.getBuild().getDirectory(), "cn1-flutter-assets.lst"), stateFile,
                    flutterPackage, MavenLog.of(getLog()))
                    .stubClasspath(classpath)
                    .javaSourceRoots(javaRoots)
                    .classesDir(classes)
                    .build();
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        if (flutter) {
            registerSourceRoot();
        }
    }

    private void checkRuntimeDependency() throws MojoFailureException {
        for (Object o : project.getDependencies()) {
            Dependency d = (Dependency) o;
            if (RUNTIME_ARTIFACT.equals(d.getArtifactId())) {
                return;
            }
        }
        throw new MojoFailureException("src/main/flutter contains Dart sources but the project "
                + "does not declare the Flutter runtime dependency. Add this to common/pom.xml:\n\n"
                + "    <dependency>\n"
                + "        <groupId>com.codenameone</groupId>\n"
                + "        <artifactId>" + RUNTIME_ARTIFACT + "</artifactId>\n"
                + "        <version>${cn1.version}</version>\n"
                + "    </dependency>\n");
    }

    private void registerSourceRoot() {
        String path = flutterOutputDir.getAbsolutePath();
        if (!project.getCompileSourceRoots().contains(path)) {
            project.addCompileSourceRoot(path);
            getLog().debug("Added compile source root " + path);
        }
    }
}
