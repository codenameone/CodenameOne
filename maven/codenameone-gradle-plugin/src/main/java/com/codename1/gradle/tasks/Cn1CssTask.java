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
import org.gradle.api.tasks.Internal;
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

    /// Where the merged stylesheet and the extracted cn1lib bundles go.
    @Internal
    public abstract DirectoryProperty getWorkDirectory();

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

    @TaskAction
    public void compile() {
        Log log = log();
        File out = getOutputDirectory().get().getAsFile();
        File cssDir = layout().cssDir();
        if (rawSettings().getProperty("codename1.cssTheme") == null) {
            log.info("CSS themes not activated for this project (codename1.cssTheme). Skipping CSS compilation");
            clear(out);
            return;
        }
        if (!layout().themeCss().isFile()) {
            log.warn("CSS compilation skipped because " + layout().themeCss() + " does not exist");
            clear(out);
            return;
        }
        File work = getWorkDirectory().get().getAsFile();
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
        File l10n = layout().l10nDir();
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
                compiler.compile(prefix, cssDir, out, work, libraries, l10n.isDirectory() ? l10n : null,
                        getCompilerClasspath().getAsPath(), layout().projectDir(), Long.MAX_VALUE);
            }
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex);
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
