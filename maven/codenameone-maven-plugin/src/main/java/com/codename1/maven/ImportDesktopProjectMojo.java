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
///     [-Dcn1.desktop.library=true]
/// ```
///
/// Copies the project's (or the named module's) `src/main/java`,
/// `src/main/kotlin` and `src/main/resources` into
/// `common/src/main/desktop`, writes the application's Codename One main
/// class (`codename1.packageName`.`codename1.mainName`, in
/// `common/src/main/java`) so that it starts the imported application, and
/// lists what becomes of the project's dependencies. The main class is an
/// ordinary source file, the application's own from then on. With
/// `cn1.desktop.library` the main class is left alone and the desktop
/// classes are a library the application's own code uses. Build and run as
/// usual afterwards.
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

    /// Imports the sources as a library: the application's main class is not
    /// written, and the application starts as it did.
    @Parameter(property = "cn1.desktop.library", defaultValue = "false")
    private boolean library;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File common = getCN1ProjectDir();
        if (common == null) {
            throw new MojoExecutionException("Run this from a Codename One application project");
        }
        DesktopProjectImporter.Result r;
        String declared;
        // The POM is wired first, since a POM that cannot be wired is a
        // reason to import nothing. An import that then fails -- several
        // entry points and none chosen -- has imported nothing either, so
        // the POM goes back to what it was.
        File pomFile = new File(common, "pom.xml");
        byte[] original = readOrNull(pomFile);
        boolean imported = false;
        try {
            declared = wireDesktopCompat(common, DesktopProjectImporter.moduleDir(source, module));
            r = new DesktopProjectImporter(MavenLog.of(getLog())).importProject(source, module, common, mainClass,
                    properties == null ? null : properties.getProperty("codename1.packageName"),
                    properties == null ? null : properties.getProperty("codename1.mainName"), library);
            imported = true;
        } catch (com.codename1.builders.BuildException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            if (!imported) {
                restore(pomFile, original);
            }
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
        if (r.generatedMain != null) {
            getLog().info("Wrote this application's main class, " + r.generatedMain + " (" + r.mainSource
                    + "), to start it on every target" + (r.setAside == null ? ""
                    : "; the previous source is kept as " + r.setAside.getName()));
        } else if (r.library) {
            getLog().info("Imported as a library: the application's main class was left as it is");
        }
        for (String u : r.uncovered) {
            if (DesktopProjectImporter.isToolkitModule(u)) {
                getLog().warn("  NOT implemented by the desktop compatibility layers: " + u
                        + ". Code that uses it is reported at build time.");
                continue;
            }
            if (declares(declared, u)) {
                // The table below says what becomes of it.
                continue;
            }
            getLog().warn("  not part of the desktop compatibility layers: " + u + ". Add it to the common "
                    + "module's dependencies (scope compile); it is then "
                    + DesktopProjectImporter.BUNDLED_NOTE + ".");
        }
        for (DesktopProjectImporter.Library lib : r.libraries) {
            if (lib.provided && !declares(declared, lib.coordinate())) {
                getLog().warn("  " + lib.coordinate() + " is what the imported sources are compiled against, and "
                        + "the project's build does not spell out its version. Add it to the common module's "
                        + "dependencies (scope provided).");
            }
        }
        for (String u : r.unresolved) {
            getLog().warn("  " + u);
        }
        reportDependencies(r, common);
        String level = DesktopImportReport.javaLevelWarning(r.javaLevel);
        if (level != null) {
            getLog().warn(level);
        }
    }

    private static byte[] readOrNull(File file) {
        try {
            return file.isFile() ? java.nio.file.Files.readAllBytes(file.toPath()) : null;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /// Writes `original` back to `file` if the file has changed since it
    /// was read.
    private void restore(File file, byte[] original) {
        if (original == null) {
            return;
        }
        try {
            if (!java.util.Arrays.equals(original, java.nio.file.Files.readAllBytes(file.toPath()))) {
                java.nio.file.Files.write(file.toPath(), original);
                getLog().info("Restored " + file + ": nothing was imported");
            }
        } catch (java.io.IOException e) {
            getLog().warn("Could not restore " + file + ": " + e.getMessage());
        }
    }

    /// Prints what the build will do with each jar the project depends on,
    /// its own dependencies included: [DesktopImportReport]. A dependency
    /// whose jar cannot be had -- no version in the build file, or nothing to
    /// download it from -- is named and left to the build, which classifies
    /// it the same way.
    private void reportDependencies(DesktopProjectImporter.Result r, File common) {
        java.util.Set<File> jars = new java.util.LinkedHashSet<File>();
        for (DesktopProjectImporter.Library lib : r.libraries) {
            if (lib.version == null) {
                continue;
            }
            int before = jars.size();
            try {
                org.apache.maven.artifact.Artifact artifact = repositorySystem.createArtifact(lib.groupId,
                        lib.artifactId, lib.version, "jar");
                org.apache.maven.artifact.resolver.ArtifactResolutionResult result = repositorySystem.resolve(
                        new org.apache.maven.artifact.resolver.ArtifactResolutionRequest()
                                .setOffline(offline)
                                .setLocalRepository(localRepository)
                                .setRemoteRepositories(
                                        new java.util.ArrayList<org.apache.maven.artifact.repository.ArtifactRepository>(
                                                remoteRepositories))
                                .setResolveTransitively(true)
                                .setArtifact(artifact));
                addJar(jars, artifact);
                if (result != null && result.getArtifacts() != null) {
                    java.util.List<org.apache.maven.artifact.Artifact> resolved =
                            new java.util.ArrayList<org.apache.maven.artifact.Artifact>(result.getArtifacts());
                    java.util.Collections.sort(resolved);
                    for (org.apache.maven.artifact.Artifact a : resolved) {
                        if (!"test".equals(a.getScope())) {
                            addJar(jars, a);
                        }
                    }
                }
            } catch (RuntimeException e) {
                getLog().debug("Could not resolve " + lib.coordinate(), e);
            }
            if (jars.size() == before) {
                getLog().info("  " + lib.coordinate() + " could not be read here; at build time "
                        + DesktopProjectImporter.BUNDLED_NOTE + ".");
            }
        }
        if (jars.isEmpty()) {
            return;
        }
        try {
            java.util.List<DesktopImportReport.Row> rows = DesktopImportReport.rows(jars,
                    new File(common, "src/main/desktop"));
            getLog().info("What the build does with the project's dependencies (" + rows.size() + " jars, "
                    + "their own dependencies included):");
            for (String line : DesktopImportReport.table(rows)) {
                getLog().info("  " + line);
            }
        } catch (java.io.IOException e) {
            getLog().warn("Could not read the project's dependencies: " + e.getMessage());
        }
    }

    private static void addJar(java.util.Set<File> jars, org.apache.maven.artifact.Artifact a) {
        File f = a.getFile();
        if (f != null && f.isFile() && f.getName().endsWith(".jar")) {
            jars.add(f);
        }
    }

    /// A project generated before the desktop layers existed has none of
    /// their wiring, and the imported sources would not build. Adds it to the
    /// common pom, or stops before copying anything, naming what to add.
    ///
    /// The libraries the project is compiled against are declared in the same
    /// edit ([DesktopProjectImporter.Library]): an import that leaves sources
    /// which cannot compile has not imported the project. Answers the pom as
    /// it is afterwards.
    private String wireDesktopCompat(File common, File moduleDir)
            throws MojoExecutionException, MojoFailureException, com.codename1.builders.BuildException {
        File pomFile = new File(common, "pom.xml");
        String pom;
        try {
            pom = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()), "UTF-8");
        } catch (java.io.IOException e) {
            throw new MojoExecutionException("Cannot read " + pomFile, e);
        }
        DesktopPomUpdater u = new DesktopPomUpdater(pom, hasKotlin(new File(moduleDir, "src/main")),
                DesktopProjectImporter.librariesOf(moduleDir));
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
            getLog().info("Updated " + pomFile + " for the desktop compatibility layers");
            for (String lib : u.addedLibraries) {
                getLog().info("  added the dependency " + lib);
            }
        }
        return u.pom;
    }

    /// Whether `pom` declares `coordinate` (`group:artifact`), by its
    /// artifact: the same test the update itself applies.
    private static boolean declares(String pom, String coordinate) {
        return pom.indexOf("<artifactId>" + coordinate.substring(coordinate.indexOf(':') + 1) + "</artifactId>") >= 0;
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
