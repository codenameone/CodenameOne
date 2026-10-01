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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.codename1.maven.PathUtil.path;

/// Compiles a project's `theme.css` (merged with the CSS of its cn1libs) into
/// `theme.res`, by forking the Codename One CSS compiler CLI.
///
/// The body of the Maven plugin's `css` goal, shared with the Gradle plugin's
/// `cn1Css` task. The build tool supplies the library CSS bundles it resolved
/// and the CLI's classpath; everything else -- the merge order, the up-to-date
/// check, the arguments -- is decided here.
public final class CssCompiler {
    /// The CSS compiler's entry point in `codenameone-css-cli`.
    public static final String CSS_CLI_MAIN_CLASS = "com.codename1.designer.css.CN1CSSCLI";

    /// Makes the `java` task the compiler is forked with. The Maven plugin
    /// passes its own so its tests can substitute a recording task.
    public interface JavaFactory {
        /// A fresh task.
        Java create();
    }

    /// A cn1lib's `cn1css` zip.
    public static final class LibraryCss {
        final String groupId;
        final String artifactId;
        final File zip;
        final boolean snapshot;
        final long lastModified;
        final File extractTo;

        /// @param extractTo where to unpack the zip; its CSS is then under
        ///        `META-INF/codenameone/<groupId>/<artifactId>/css`
        /// @param snapshot whether the bundle can change without its version
        ///        changing, in which case a stale extraction is replaced
        /// @param lastModified when the bundle last changed
        public LibraryCss(String groupId, String artifactId, File zip, File extractTo, boolean snapshot,
                          long lastModified) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.zip = zip;
            this.extractTo = extractTo;
            this.snapshot = snapshot;
            this.lastModified = lastModified;
        }
    }

    private final Log log;
    private final Project antProject;
    private final JavaFactory javaFactory;

    /// A compiler logging to `log`, unzipping through `antProject`, forking
    /// through tasks from `javaFactory`.
    private File buildDirectory;

    /// The project's build directory, when the build has moved it from the
    /// conventional one; passed to the compiler process. Null (the default)
    /// lets the compiler find it from the project layout.
    public CssCompiler buildDirectory(File dir) {
        this.buildDirectory = dir;
        return this;
    }

    public CssCompiler(Log log, Project antProject, JavaFactory javaFactory) {
        this.log = log;
        this.antProject = antProject;
        this.javaFactory = javaFactory;
    }

    /// Compiles `<themePrefix>theme.css`.
    ///
    /// @param cssDirectory the project's CSS directory
    /// @param classesOutputDir where `<themePrefix>theme.res` is written
    /// @param cssBuildDir where the merged stylesheet is written
    /// @param libraries the cn1libs' CSS bundles, merged BEFORE the project's
    ///        own stylesheet so the application can override them
    /// @param localizationDir the l10n directory bundled into the theme, or null
    /// @param cliClasspath the CSS compiler CLI's classpath
    /// @param workingDir the directory the compiler runs in (the one holding
    ///        `codenameone_settings.properties`)
    /// @param sourcesModified the newest modification time among the CSS and
    ///        localization sources; an output newer than it is left alone
    /// @return false when there was nothing to compile or it was up to date
    public boolean compile(String themePrefix, File cssDirectory, File classesOutputDir, File cssBuildDir,
                           List<LibraryCss> libraries, File localizationDir, String cliClasspath,
                           File workingDir, long sourcesModified) throws BuildExecutionException {
        File themeResOutput = new File(classesOutputDir, themePrefix + "theme.res");
        cssBuildDir.mkdirs();
        File mergeFile = new File(cssBuildDir, themePrefix + "theme.css");
        if (themeResOutput.exists() && sourcesModified < themeResOutput.lastModified()) {
            log.info("CSS sources unchanged since last compile.  Skipping CSS compilation");
            return false;
        }

        // A comma-delimited list of the CSS files the compiler merges: every
        // cn1lib's theme.css first, then the project's own last so the
        // application can override its libraries.
        StringBuilder inputs = new StringBuilder();
        for (LibraryCss lib : libraries) {
            File theme = extractedTheme(lib, themePrefix);
            if (theme != null) {
                if (inputs.length() > 0) {
                    inputs.append(",");
                }
                inputs.append(theme.getAbsolutePath());
            }
        }

        File cssTheme = new File(cssDirectory, themePrefix + "theme.css");
        if (cssTheme.exists()) {
            if (inputs.length() > 0) {
                inputs.append(",");
            }
            inputs.append(cssTheme.getAbsolutePath());
        } else {
            if (themePrefix.isEmpty() && inputs.length() > 0) {
                throw new BuildFailureException("Cannot compile CSS for this project.  The project does not include a "
                        + themePrefix + "-theme.css file in " + cssTheme + ", but it includes dependencies that require CSS.  "
                        + "Please add a CSS file at " + cssTheme);
            }
            log.info("Skipping CSS compilation for because " + themePrefix + cssTheme + " does not exist");
            return false;
        }

        // The CLI runs on a resolved classpath rather than `java -jar` against the
        // Designer's shaded artifact: it needs codenameone-javase for CEF
        // rasterization, but nothing needs a 43MB shaded copy of it.
        Java java = javaFactory.create();
        java.setDir(workingDir);
        java.setClasspath(new org.apache.tools.ant.types.Path(antProject, cliClasspath));
        java.setClassname(CSS_CLI_MAIN_CLASS);
        java.setFork(true);
        java.setFailonerror(true);
        java.createJvmarg().setValue("-Dcli=true");
        if (buildDirectory != null) {
            // Where the compiler keeps its checksums and other work files: the
            // build's own directory, which it cannot find for itself once moved.
            java.createJvmarg().setValue("-Dcn1.buildDir=" + buildDirectory.getAbsolutePath());
        }
        java.createArg().setValue("-css");
        java.createArg().setValue("-input");
        java.createArg().setValue(inputs.toString());
        java.createArg().setValue("-output");
        java.createArg().setFile(themeResOutput);
        java.createArg().setValue("-merge");
        java.createArg().setFile(mergeFile);
        if (localizationDir != null) {
            java.createArg().setValue("-l");
            java.createArg().setFile(localizationDir);
        }
        int res = java.executeJava();
        if (res != 0) {
            throw new BuildExecutionException("An error occurred while compiling the CSS files.  Inputs: " + inputs
                    + ", output: " + themeResOutput + ", merge file: " + mergeFile);
        }
        return true;
    }

    /// The comma-separated compiler inputs for `<themePrefix>theme.css`: every
    /// library's stylesheet, then `projectThemeCss` last when it exists. The
    /// simulator's live CSS reload is given the same list the build compiles.
    public String inputs(List<LibraryCss> libraries, String themePrefix, File projectThemeCss) {
        StringBuilder inputs = new StringBuilder();
        for (LibraryCss lib : libraries) {
            File theme = extractedTheme(lib, themePrefix);
            if (theme != null) {
                if (inputs.length() > 0) {
                    inputs.append(",");
                }
                inputs.append(theme.getAbsolutePath());
            }
        }
        if (projectThemeCss != null && projectThemeCss.exists()) {
            if (inputs.length() > 0) {
                inputs.append(",");
            }
            inputs.append(projectThemeCss.getAbsolutePath());
        }
        return inputs.toString();
    }

    private File extractedTheme(LibraryCss lib, String themePrefix) {
        if (lib.zip == null || !lib.zip.exists()) {
            return null;
        }
        File extracted = lib.extractTo;
        log.debug("Checking for extracted CSS bundle " + extracted);
        if (extracted.exists() && lib.snapshot && lib.lastModified > extracted.lastModified()) {
            try {
                FileUtils.deleteDirectory(extracted);
            } catch (IOException ex) {
                log.error(ex);
            }
        }
        if (!extracted.exists()) {
            log.debug("CSS bundle " + lib.zip + " not extracted yet.  Extracting to " + extracted);
            Expand expand = (Expand) antProject.createTask("unzip");
            expand.setSrc(lib.zip);
            expand.setDest(extracted);
            expand.execute();
            // An entry keeps the time recorded in the zip, which a reproducible
            // archive sets to 1980 -- Gradle's Zip does by default. The compiler
            // merges only inputs newer than the merged stylesheet, so a library
            // added to an existing project was silently left out. Freshly extracted
            // is what "changed" means here.
            touch(extracted, System.currentTimeMillis());
        }
        if (!extracted.exists()) {
            log.debug("CSS bundle extraction must have failed for " + lib.zip
                    + " because after extraction it still doesn't exist at " + extracted);
            return null;
        }
        File extractedCssDir = new File(extracted, path("META-INF", "codenameone", lib.groupId, lib.artifactId, "css"));
        File theme = new File(extractedCssDir, themePrefix + "theme.css");
        return theme.exists() ? theme : null;
    }

    private void touch(File f, long time) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    touch(c, time);
                }
            }
        }
        if (!f.setLastModified(time)) {
            // Best effort: only the merge's freshness check reads the time.
            log.debug("Could not set the modification time of " + f);
        }
    }

    /// The localization directory beside a source root's parent: `l10n`, else
    /// `i18n`, else null.
    public static File localizationSibling(File parent) {
        if (parent == null) {
            return null;
        }
        File l10n = new File(parent, "l10n");
        if (l10n.isDirectory()) {
            return l10n;
        }
        File i18n = new File(parent, "i18n");
        if (i18n.isDirectory()) {
            return i18n;
        }
        return null;
    }

    /// Every `*theme.css` prefix in `cssDirectory`: "" for `theme.css`, `dark`
    /// for `darktheme.css`, and so on.
    public static List<String> themePrefixes(File cssDirectory) {
        List<String> out = new ArrayList<String>();
        File[] files = cssDirectory == null ? null : cssDirectory.listFiles();
        if (files == null) {
            return out;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith("theme.css")) {
                out.add(name.substring(0, name.length() - "theme.css".length()));
            }
        }
        return out;
    }
}
