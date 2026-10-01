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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/// PROCESS_CLASSES Mojo. ASM-scans the project's compiled `.class` files,
/// dispatches each annotated class to the registered `AnnotationProcessor`s,
/// and writes the emitted bytecode back into `target/classes` so it lives in
/// the same tree as the rest of the compile output.
///
/// **Fail-fast**: any processor-reported error (e.g. `@Route` on a class that
/// doesn't extend `Form`) aborts the build with a `MojoFailureException`
/// listing every offender. The Mojo never overwrites generated files when a
/// validation error is pending — invalid input cannot leak past this Mojo.
///
/// Generated classes are emitted under `${project.build.outputDirectory}` so:
///   1. The maven build's normal jar-packaging copies them.
///   2. ParparVM's iOS class scan and the JavaSE simulator both see them.
///   3. The project's `target/classes` takes precedence over any cn1-core
///      JAR stub of the same internal name on the classpath at runtime.
/// COMPILE resolution is required, not optional. A processor discovers which
/// annotations are build hint annotations by reading the annotation package off
/// this classpath, and without a declared scope Maven does not resolve it:
/// getCompileClasspathElements() throws, the package is not found, and every
/// annotated hint is silently skipped.
@Mojo(name = "process-annotations",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class ProcessAnnotationsMojo extends AbstractCN1Mojo {

    // The MavenProject reference is inherited from AbstractCN1Mojo.

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/cn1-annotations",
               required = true)
    protected File stubSourceDirectory;

    @Parameter(defaultValue = "false")
    protected boolean skip;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("cn1: process-annotations skipped by configuration");
            return;
        }
        if (!outputDirectory.isDirectory()) {
            getLog().debug("cn1: nothing compiled at " + outputDirectory + " — skipping process-annotations");
            return;
        }

        try {
            new AnnotationProcessing(MavenLog.of(getLog()), outputDirectory, stubSourceDirectory,
                    getCN1ProjectDir(), rawProjectSettings(), mainClassBinaryName(),
                    // The roots Maven is actually compiling, and the charset javac is
                    // given; see AnnotationProcessing.
                    compileSourceRoots(project, userProperties()),
                    sourceEncodingOf(project, userProperties()),
                    compileClasspathOf(project)).run();
        } catch (com.codename1.build.BuildFailureException e) {
            throw new MojoFailureException(e.getMessage(), e.getCause() == null ? e : e.getCause());
        } catch (com.codename1.build.BuildExecutionException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause() == null ? e : e.getCause());
        }
    }

    /// Loads `codenameone_settings.properties` exactly as it sits on disk.
    ///
    /// Deliberately not the inherited `properties` field: that one has the
    /// `-D` command line overlaid on top of it, and a hint passed with `-D` is
    /// the documented way to override one for a single build. A processor that
    /// compared annotations against the overlaid view would report a conflict
    /// for the one case that is supposed to win.
    private Properties rawProjectSettings() {
        File f = getProjectPropertiesFile();
        if (f == null || !f.exists()) {
            return null;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } catch (IOException ex) {
            getLog().warn("cn1: could not read " + f + ": " + ex.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // nothing useful to do on close failure of a read-only stream
                }
            }
        }
        return p;
    }

    /// `codename1.packageName` + `codename1.mainName`, or null when the project
    /// declares no main class.
    ///
    /// From the EFFECTIVE settings, not the file: `execute()` has already
    /// overlaid `-D codename1.mainName` / `codename1.packageName` onto
    /// `properties`, and `CN1BuildMojo` computes the main class it expects from
    /// that same overlaid table. Reading the file here stamped the manifest for
    /// a class the merge was not looking for, so it refused the manifest and
    /// the annotated hints were silently dropped -- while
    /// `failOnMisplacedAnnotations` reported the entry point the build had
    /// actually selected as the wrong place to put them.
    ///
    /// The RAW file is still what the duplicate-hint check reads, which is the
    /// one thing `-D` must NOT feed: a hint passed on the command line is an
    /// override of the file's value, not a second declaration of it.
    String mainClassBinaryName() {
        Properties p = properties != null ? properties : rawProjectSettings();
        if (p == null) {
            return null;
        }
        String main = p.getProperty("codename1.mainName");
        String pkg = p.getProperty("codename1.packageName");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        main = main.trim();
        if (pkg == null || pkg.trim().length() == 0) {
            return main;
        }
        return pkg.trim() + "." + main;
    }

    /// The module's compile classpath, or an empty list when Maven cannot
    /// resolve it.
    ///
    /// Not fatal: a processor that cannot find the annotations says so itself,
    /// and failing the build here would break goals that do not need them.
    private java.util.List<String> compileClasspathOf(
            org.apache.maven.project.MavenProject module) {
        if (module == null) {
            return java.util.Collections.emptyList();
        }
        try {
            return module.getCompileClasspathElements();
        } catch (org.apache.maven.artifact.DependencyResolutionRequiredException ex) {
            // Loud. An unresolved classpath means the annotation package cannot
            // be found, and a processor that reads it then finds nothing to do
            // -- so every annotated hint would be skipped with no other sign. The
            // mojo declares COMPILE resolution precisely so this cannot happen;
            // reaching it means that declaration was lost.
            getLog().warn("cn1: the compile classpath for " + module.getArtifactId()
                    + " is unresolved, so build hint annotations cannot be read: "
                    + ex.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
