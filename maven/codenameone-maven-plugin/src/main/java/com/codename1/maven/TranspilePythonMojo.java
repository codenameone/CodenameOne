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

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/// Transpiles the Python sources of an application module, `src/main/python`,
/// to Java under `target/generated-sources/python`, which is added as a source
/// root. From there the program is ordinary Codename One code and every target
/// builds it. A silent no-op for a project without Python sources, apart from
/// removing what an earlier build generated.
///
/// A construct a translated program cannot run -- `eval`, `exec`, a C
/// extension -- fails the build with the file and line.
@Mojo(name = "transpile-python", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class TranspilePythonMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.python.sourceDir", defaultValue = "${project.basedir}/src/main/python")
    private File pythonSourceDir;

    @Parameter(property = "cn1.python.outputDir", defaultValue = "${project.build.directory}/generated-sources/python")
    private File pythonOutputDir;

    /// The module run by `PythonModules.runMain()`. Unset, it is
    /// `codename1.python.mainModule` from the settings, which
    /// `cn1:import-python-project` writes, and then `main` when `main.py` exists.
    @Parameter(property = "cn1.python.mainModule")
    private String mainModule;

    @Parameter(property = "cn1.python.skip", defaultValue = "false")
    private boolean skip;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }
        String entry = mainModule;
        if (entry == null && properties != null) {
            entry = properties.getProperty(PythonProjectImporter.MAIN_MODULE_SETTING);
        }
        PythonProjectBuilder builder = new PythonProjectBuilder(pythonSourceDir, pythonOutputDir, entry,
                MavenLog.of(getLog())).classPath(compileClassPath())
                .javaSources(new File(project.getBuild().getSourceDirectory()),
                        new File(project.getBuild().getDirectory(), "python-java-imports"))
                .virtualThreadStack(properties == null ? null
                        : properties.getProperty(PythonProjectBuilder.VIRTUAL_THREAD_STACK_HINT));
        if (PythonProjectBuilder.isPythonProject(pythonSourceDir)) {
            checkJdk();
            checkRuntimeDependency();
        }
        boolean built;
        try {
            built = builder.run();
        } catch (com.codename1.builders.BuildException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        if (built) {
            String path = pythonOutputDir.getAbsolutePath();
            if (!project.getCompileSourceRoots().contains(path)) {
                project.addCompileSourceRoot(path);
            }
        }
    }

    private void checkJdk() throws MojoFailureException {
        String spec = System.getProperty("java.specification.version", "1.8");
        int major;
        try {
            major = Integer.parseInt(spec.startsWith("1.") ? spec.substring(2) : spec);
        } catch (NumberFormatException e) {
            major = 8;
        }
        if (major < 17) {
            throw new MojoFailureException("Python support generates Java 17 source, but this build is running on JDK "
                    + spec + ". Run Maven with JDK 17 or newer (e.g. from https://adoptium.net) to use src/main/python.");
        }
    }

    private void checkRuntimeDependency() throws MojoFailureException {
        for (Object o : project.getDependencies()) {
            if (PythonProjectBuilder.RUNTIME_ARTIFACT.equals(((Dependency) o).getArtifactId())) {
                return;
            }
        }
        throw new MojoFailureException("src/main/python contains Python sources but the project does not declare "
                + "the Python runtime dependency. Add this to common/pom.xml, or run cn1:import-python-project, "
                + "which adds it:\n\n"
                + "    <dependency>\n"
                + "        <groupId>com.codenameone</groupId>\n"
                + "        <artifactId>" + PythonProjectBuilder.RUNTIME_ARTIFACT + "</artifactId>\n"
                + "        <version>${cn1.version}</version>\n"
                + "    </dependency>\n");
    }

    private java.util.List<File> compileClassPath() {
        java.util.List<File> r = new java.util.ArrayList<File>();
        for (Object o : project.getArtifacts()) {
            org.apache.maven.artifact.Artifact a = (org.apache.maven.artifact.Artifact) o;
            if (a.getFile() != null && !"test".equals(a.getScope())) {
                r.add(a.getFile());
            }
        }
        return r;
    }
}
