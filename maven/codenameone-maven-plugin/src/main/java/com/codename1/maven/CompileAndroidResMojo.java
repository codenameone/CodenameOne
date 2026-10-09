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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Compiles the Android sources of an application module: `src/main/android`
/// laid out like an Android Studio module's `src/main` (`AndroidManifest.xml`,
/// `res/`, `assets/`, `java/`).
///
/// Generates the R classes and the application factory under
/// `target/generated-sources/android`, writes the resource table, images and
/// assets into the classes directory, and adds `src/main/android/java` as a
/// source root so the Android code compiles unchanged against
/// `codenameone-android-compat`. A silent no-op for a project without
/// `src/main/android`. See `remap-android` for the step that follows
/// compilation.
@Mojo(name = "compile-android-res", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileAndroidResMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.android.sourceDir", defaultValue = "${project.basedir}/src/main/android")
    private File androidSourceDir;

    @Parameter(property = "cn1.android.outputDir", defaultValue = "${project.build.directory}/generated-sources/android")
    private File androidOutputDir;

    /// Overrides the manifest's `package`, like a Gradle `namespace`.
    @Parameter(property = "cn1.android.namespace")
    private String androidNamespace;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!AndroidResourceRunner.isAndroidProject(androidSourceDir)) {
            return;
        }
        // The last build relocated the classes in the output directory, and
        // javac compiles against that directory: a Kotlin class the
        // incremental compiler does not recompile would be seen relocated, so
        // the Java code calling it would not compile. Its unrelocated original
        // is in Kotlin's own output tree; put it back before anything compiles.
        copyKotlinIncrementalCompileOutputToOutputDir(true);
        File javaDir = new File(androidSourceDir, "java");
        List<File> roots = new ArrayList<File>();
        for (Object o : project.getCompileSourceRoots()) {
            roots.add(new File(o.toString()));
        }
        roots.add(javaDir);
        // Kotlin sources are not a Maven compile source root at this point (the
        // Kotlin plugin is configured with its own source directories), but a
        // Kotlin main class is still the application's: look there too, or a
        // second main class would be generated beside it.
        roots.add(new File(project.getBasedir(), "src/main/kotlin"));
        String pkg = null;
        String main = null;
        if (isCN1ProjectDir()) {
            pkg = getCN1ProjectProperty("codename1.packageName");
            main = getCN1ProjectProperty("codename1.mainName");
        }
        AndroidResourceRunner runner = new AndroidResourceRunner(androidSourceDir, androidOutputDir,
                new File(project.getBuild().getOutputDirectory()), new File(project.getBuild().getDirectory()),
                compatJar(project.getArtifacts()), androidNamespace, pkg, main, roots, MavenLog.of(getLog()));
        try {
            runner.run();
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        registerSourceRoot(androidOutputDir);
        if (javaDir.isDirectory()) {
            registerSourceRoot(javaDir);
        }
    }

    private String getCN1ProjectProperty(String key) {
        return properties == null ? null : properties.getProperty(key);
    }

    static File compatJar(Iterable<?> artifacts) {
        for (Object o : artifacts) {
            Artifact a = (Artifact) o;
            if (AndroidResourceRunner.COMPAT_ARTIFACT.equals(a.getArtifactId()) && a.getFile() != null) {
                return a.getFile();
            }
        }
        return null;
    }
}
