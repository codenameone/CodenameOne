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
package com.codename1.gradle.tasks;

import com.codename1.build.AntSupport;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import com.codename1.maven.CssCompiler;
import org.apache.tools.ant.Project;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Compiles `src/main/css/*theme.css`, merged with every cn1lib's CSS, into
/// `theme.res`. The same [CssCompiler] as the Maven plugin's `css` goal.
@CacheableTask
public abstract class Cn1CssTask extends Cn1Task {
    /// The project's CSS sources, and the l10n bundles compiled into the theme.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The cn1libs' `cn1css` bundles, encoded like [Cn1BuildTask#getArtifacts()].
    @Input
    public abstract ListProperty<String> getLibraryCss();

    /// The bundle files themselves, so a changed bundle recompiles.
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract ConfigurableFileCollection getLibraryCssFiles();

    /// The CSS compiler CLI's classpath.
    @Classpath
    public abstract ConfigurableFileCollection getCompilerClasspath();

    /// Where `theme.res` goes; added to the main resources.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// Where the merged stylesheet, the extracted cn1lib bundles and the
    /// simulator's input list ([#SIMULATOR_INPUTS]) go. An output, so a build
    /// cache hit restores the extracted bundles that list names.
    @OutputDirectory
    public abstract DirectoryProperty getWorkDirectory();

    /// The file in [#getWorkDirectory()] holding the comma-separated stylesheets
    /// the main theme is compiled from, cn1libs first. The simulator's live CSS
    /// reload recompiles from the same list, so a library's styles survive an
    /// edit to the application's own theme.css.
    public static final String SIMULATOR_INPUTS = "simulator-css-inputs.txt";

    /// Empties `out`. It is a main resource directory, so a theme.res left from
    /// an earlier run would still be packaged after CSS was switched off.
    private static void clear(File out) {
        File[] children = out.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            try {
                org.apache.commons.io.FileUtils.forceDelete(c);
            } catch (java.io.IOException ex) {
                throw new GradleException("Could not delete the stale " + c, ex);
            }
        }
    }

    /// `inputs` (comma separated) with each path under `projectDir` written
    /// relative to it. The file is a cached output: absolute paths would send a
    /// checkout restored from the build cache somewhere else to the old one's
    /// stylesheets. The simulator resolves them against the project again.
    public static String relative(String inputs, File projectDir) {
        String base = projectDir.getAbsolutePath() + File.separator;
        StringBuilder sb = new StringBuilder();
        for (String path : inputs.split(",")) {
            if (path.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(path.startsWith(base) ? path.substring(base.length()) : path);
        }
        return sb.toString();
    }

    private static void writeInputs(File to, String inputs) {
        try {
            java.nio.file.Files.write(to.toPath(), inputs.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException ex) {
            throw new GradleException("Could not write " + to, ex);
        }
    }

    @TaskAction
    public void compile() {
        Log log = log();
        File out = getOutputDirectory().get().getAsFile();
        File cssDir = layout().cssDir();
        File work = getWorkDirectory().get().getAsFile();
        // Gone before any early return: with CSS switched off or theme.css removed,
        // a list from an earlier run would keep the simulator's live reload merging
        // stylesheets the build no longer uses.
        File simulatorInputs = new File(work, SIMULATOR_INPUTS);
        if (simulatorInputs.exists() && !simulatorInputs.delete()) {
            throw new GradleException("Could not delete " + simulatorInputs);
        }
        // Effective, not the file alone: codename1.cssTheme set in gradle.properties,
        // with -P/-D or in codenameone { buildHints } counts as it does for the
        // simulator and the native builds.
        if (effectiveSettings().getProperty("codename1.cssTheme") == null) {
            log.info("CSS themes not activated for this project (codename1.cssTheme). Skipping CSS compilation");
            clear(out);
            return;
        }
        if (!layout().themeCss().isFile()) {
            log.warn("CSS compilation skipped because " + layout().themeCss() + " does not exist");
            clear(out);
            return;
        }
        List<CssCompiler.LibraryCss> libraries = new ArrayList<CssCompiler.LibraryCss>();
        for (String encoded : getLibraryCss().get()) {
            com.codename1.build.BuildArtifact a = com.codename1.gradle.GradleHostFactory.decode(encoded);
            if (a == null) {
                continue;
            }
            File extractTo = new File(work, "libs" + File.separator + a.getGroupId() + File.separator
                    + a.getArtifactId() + File.separator + a.getVersion());
            libraries.add(new CssCompiler.LibraryCss(a.getGroupId(), a.getArtifactId(), a.getFile(), extractTo,
                    a.getVersion() != null && a.getVersion().endsWith("-SNAPSHOT"), a.getFile().lastModified()));
        }
        final Project ant = AntSupport.newProject(layout().projectDir());
        CssCompiler compiler = new CssCompiler(log, ant,
                () -> AntSupport.createJava(ant, log, AntSupport.LEVEL_INFO));
        File l10n = CssCompiler.localizationSibling(layout().l10nDir().getParentFile());
        // Every current theme is compiled below, so start empty: a deleted
        // darktheme.css would otherwise leave darktheme.res in a directory that is
        // packaged as a main resource.
        clear(out);
        try {
            for (String prefix : CssCompiler.themePrefixes(cssDir)) {
                // Gradle has already decided this task must run; a merged stylesheet
                // left from the last run would make the compiler decide otherwise.
                File merged = new File(work, prefix + "theme.css");
                if (merged.exists() && !merged.delete()) {
                    throw new GradleException("Could not delete " + merged);
                }
                // Gradle decides whether this task is up to date, so the
                // compiler's own timestamp check is told everything changed.
                compiler.compile(prefix, cssDir, out, work, libraries, l10n != null && l10n.isDirectory() ? l10n : null,
                        getCompilerClasspath().getAsPath(), layout().projectDir(), Long.MAX_VALUE);
            }
            // After the compile, which extracted every bundle the list names.
            writeInputs(simulatorInputs, relative(compiler.inputs(libraries, "", layout().themeCss()),
                    layout().projectDir()));
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex);
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
