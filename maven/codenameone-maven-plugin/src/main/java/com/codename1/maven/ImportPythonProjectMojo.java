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

/// Imports a Python project into this Codename One application.
///
/// ```
/// mvn cn1:import-python-project -Dsource=/path/to/my-python-app
/// ```
///
/// Copies the project's `.py` sources into `common/src/main/python`, adds the
/// runtime dependency and the `transpile-python` goal to the common module,
/// records the entry module in `codenameone_settings.properties` and makes the
/// Codename One main class run it. Build and run as usual afterwards.
/// Importing again mirrors the Python project afresh.
@Mojo(name = "import-python-project", requiresProject = true, aggregator = true)
public class ImportPythonProjectMojo extends AbstractCN1Mojo {

    /// The Python project directory.
    @Parameter(property = "source")
    private File source;

    /// The same, under a name no other plugin reads; it wins when both are given.
    @Parameter(property = "cn1.python.import")
    private File pythonImport;

    /// Leaves PyPI alone: the requirements are listed, not downloaded.
    @Parameter(property = "cn1.python.offline", defaultValue = "false")
    private boolean offline;

    /// An index that speaks PyPI's JSON API, in place of pypi.org.
    @Parameter(property = "cn1.python.index")
    private String index;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File from = pythonImport != null ? pythonImport : source;
        if (from == null) {
            throw new MojoFailureException("Name the Python project to import: -Dsource=/path/to/my-python-app");
        }
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        if (!PythonProjectBuilder.isPythonProject(PythonProjectImporter.sourceRoot(from))) {
            // Before the pom is touched: a mistyped path must change nothing.
            throw new MojoFailureException(from + " has no Python sources");
        }
        wirePython(common);
        String pkg = properties == null ? null : properties.getProperty("codename1.packageName");
        String main = properties == null ? null : properties.getProperty("codename1.mainName");
        PythonProjectImporter.Result r;
        try {
            r = new PythonProjectImporter(MavenLog.of(getLog())).importProject(from, common, pkg, main);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Imported " + r.copiedFiles + " Python files into " + new File(common, "src/main/python")
                + (r.removedFiles == 0 ? "" : "; removed " + r.removedFiles + " the project no longer has"));
        getLog().info(r.mainModule == null ? "No entry module was found: the sources are imported as a library"
                : "Entry module: " + r.mainModule);
        if (r.requirementSpecs.isEmpty()) {
            return;
        }
        if (offline) {
            getLog().warn("The project depends on " + r.requirements + "; they were not downloaded "
                    + "(-Dcn1.python.offline). Import again without it to vendor them.");
            return;
        }
        PythonPackageResolver.Result p;
        try {
            PythonPackageResolver resolver = new PythonPackageResolver(MavenLog.of(getLog()));
            if (index != null && !index.isEmpty()) {
                resolver.index(index);
            }
            p = resolver.resolve(r.requirementSpecs, common);
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        if (!p.provided.isEmpty()) {
            getLog().info("Provided by the Codename One Python runtime: " + p.provided);
        }
        if (!p.installed.isEmpty()) {
            getLog().info("Installed into src/main/" + PythonPackageResolver.PACKAGES_DIR + ": " + p.installed);
        }
        for (java.util.Map.Entry<String, String> e : p.nativeModules.entrySet()) {
            getLog().warn("C extension module " + e.getKey() + " is written in Java by " + e.getValue()
                    + (p.stubsWritten.contains(e.getValue()) ? " (stub generated: fill in its functions)" : ""));
        }
        if (!p.optionalNativeModules.isEmpty()) {
            getLog().info("C accelerator modules with a pure-Python fallback, used instead: "
                    + p.optionalNativeModules);
        }
    }

    private void wirePython(File common) throws MojoExecutionException, MojoFailureException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        PythonPomUpdater u = new PythonPomUpdater(pom);
        if (!u.manual.isEmpty()) {
            StringBuilder sb = new StringBuilder("Nothing was imported: " + pomFile
                    + " needs Python support wired in by hand. Add:\n");
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
            getLog().info("Added Python support to " + pomFile);
        }
    }
}
