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

import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Imports a Kotlin Multiplatform module into a Codename One application.
///
/// The module's `commonMain` and its JVM source set (`jvmMain`, else
/// `desktopMain`) are copied into `common/src/main/kmp`, which the
/// archetype's `compose` profile compiles with the Compose compiler. The
/// Android and iOS source sets are not copied: they call the platforms
/// directly, and Codename One supplies both platforms itself.
///
/// The application's main class is rewritten to show the module's root
/// composable in a `ComposeForm`. The composable is the one named, or the
/// only `@Composable fun App()` the common sources declare.
public final class KmpProjectImporter {
    /// What an import did.
    public static final class Result {
        private File module;
        private int copiedFiles;
        private String entry;
        private File mainClass;
        private File setAside;
        private final List<String> skippedSourceSets = new ArrayList<String>();
        private final List<File> skippedEntryPoints = new ArrayList<File>();

        /// The module whose sources were imported.
        public File getModule() {
            return module;
        }

        /// How many source files were copied.
        public int getCopiedFiles() {
            return copiedFiles;
        }

        /// The fully qualified root composable the application shows.
        public String getEntry() {
            return entry;
        }

        /// The main class the import wrote.
        public File getMainClass() {
            return mainClass;
        }

        /// The previous main class, renamed out of the build, or null.
        public File getSetAside() {
            return setAside;
        }

        /// Platform source sets that were not imported.
        public List<String> getSkippedSourceSets() {
            return skippedSourceSets;
        }

        /// JVM sources left out because they start the desktop application.
        public List<File> getSkippedEntryPoints() {
            return skippedEntryPoints;
        }
    }

    /// The artifact a project with Kotlin Multiplatform sources depends on.
    public static final String COMPOSE_ARTIFACT = "codenameone-compose-compat";
    /// The Kotlin compiler plugin that makes a function composable, applied by
    /// a Gradle build under this id.
    public static final String COMPOSE_GRADLE_PLUGIN = "org.jetbrains.kotlin.plugin.compose";

    /// `src/main/kmp` of a common module: where the shared sources are imported.
    public static File kmpDir(File common) {
        return new File(common, "src/main/kmp");
    }

    /// The Kotlin source roots under [#kmpDir] a build compiles.
    public static File[] kotlinDirs(File kmpDir) {
        return new File[] {new File(kmpDir, "commonMain/kotlin"), new File(kmpDir, "jvmMain/kotlin")};
    }

    /// Whether the module has imported Kotlin Multiplatform sources.
    public static boolean isKmpProject(File kmpDir) {
        return kmpDir.isDirectory();
    }

    private static final String[] JVM_SETS = {"jvmMain", "desktopMain"};
    private static final String[] SKIPPED_SETS = {"androidMain", "iosMain", "appleMain", "nativeMain", "jsMain",
        "wasmJsMain"};
    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)", Pattern.MULTILINE);
    private static final Pattern WINDOW = Pattern.compile("^\\s*import\\s+androidx\\.compose\\.ui\\.window\\.",
            Pattern.MULTILINE);
    private static final Pattern APP = Pattern.compile("@Composable\\s+(?:public\\s+)?fun\\s+App\\s*\\(\\s*\\)");

    /// The directory of the module to import: `source` itself when it has a
    /// `src/commonMain`, else its only child that has one, else `module`.
    public static File moduleDir(File source, String module) throws BuildException {
        if (module != null && module.length() > 0) {
            File m = new File(source, module);
            if (!new File(m, "src/commonMain").isDirectory()) {
                throw new BuildException("Module " + module + " of " + source + " has no src/commonMain");
            }
            return m;
        }
        if (new File(source, "src/commonMain").isDirectory()) {
            return source;
        }
        List<File> found = new ArrayList<File>();
        File[] kids = source.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (new File(k, "src/commonMain").isDirectory()) {
                    found.add(k);
                }
            }
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new BuildException("No Kotlin Multiplatform module (a directory with src/commonMain) in " + source);
        }
        StringBuilder names = new StringBuilder();
        for (File f : found) {
            names.append(' ').append(f.getName());
        }
        throw new BuildException("Several modules have a src/commonMain; choose one with -Dcn1.kmp.module:" + names);
    }

    /// Copies the module's sources into `common` and writes the main class
    /// `packageName`.`mainName`. `entry` is the fully qualified root
    /// composable, or null to find `App()`.
    public Result importProject(File source, String module, File common, String entry, String packageName,
                                String mainName) throws BuildException {
        if (packageName == null || mainName == null) {
            throw new BuildException("codename1.packageName and codename1.mainName must be set in "
                    + "codenameone_settings.properties");
        }
        Result r = new Result();
        r.module = moduleDir(source, module);
        File kmp = kmpDir(common);
        File commonSrc = new File(r.module, "src/commonMain/kotlin");
        if (!commonSrc.isDirectory()) {
            throw new BuildException(r.module + " has no src/commonMain/kotlin");
        }
        try {
            r.copiedFiles += copyTree(commonSrc, new File(kmp, "commonMain/kotlin"));
            for (String set : JVM_SETS) {
                File d = new File(r.module, "src/" + set + "/kotlin");
                if (d.isDirectory()) {
                    r.copiedFiles += copyJvmTree(d, new File(kmp, "jvmMain/kotlin"), r);
                    break;
                }
            }
            for (String set : SKIPPED_SETS) {
                if (new File(r.module, "src/" + set).isDirectory()) {
                    r.skippedSourceSets.add(set);
                }
            }
            r.entry = entry != null && entry.length() > 0 ? entry : findApp(commonSrc);
            writeMain(common, packageName, mainName, r);
        } catch (IOException e) {
            throw new BuildException("Importing " + r.module + " failed: " + e.getMessage(), e);
        }
        return r;
    }

    private static String findApp(File dir) throws IOException, BuildException {
        List<String> found = new ArrayList<String>();
        collectApps(dir, found);
        if (found.size() == 1) {
            return found.get(0);
        }
        throw new BuildException((found.isEmpty() ? "No" : "More than one") + " @Composable fun App() in the common "
                + "sources" + (found.isEmpty() ? "" : " (" + found + ")")
                + "; name the root composable with -Dcn1.kmp.entry=com.example.App");
    }

    private static void collectApps(File dir, List<String> out) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File k : kids) {
            if (k.isDirectory()) {
                collectApps(k, out);
            } else if (k.getName().endsWith(".kt")) {
                String text = new String(Files.readAllBytes(k.toPath()), StandardCharsets.UTF_8);
                if (APP.matcher(text).find()) {
                    Matcher p = PACKAGE.matcher(text);
                    out.add(p.find() ? p.group(1) + ".App" : "App");
                }
            }
        }
    }

    private static void writeMain(File common, String pkg, String name, Result r) throws IOException {
        String path = pkg.replace('.', '/');
        File kotlin = new File(common, "src/main/kotlin/" + path + "/" + name + "Compose.kt");
        kotlin.getParentFile().mkdirs();
        write(kotlin, "package " + pkg + "\n\n"
                + "import com.codename1.compose.ComposeForm\n\n"
                + "/// Shows the imported application's root composable. Called from " + name + ".runApp.\n"
                + "fun startCompose() {\n"
                + "    ComposeForm { " + r.entry + "() }.show()\n"
                + "}\n");
        File java = new File(common, "src/main/java/" + path + "/" + name + ".java");
        String source = "package " + pkg + ";\n\n"
                + "import com.codename1.system.Lifecycle;\n\n"
                + "/// Starts the imported Compose Multiplatform application, whose root\n"
                + "/// composable " + name + "Compose.kt shows.\n"
                + "public class " + name + " extends Lifecycle {\n"
                + "    @Override\n"
                + "    public void runApp() {\n"
                + "        " + name + "ComposeKt.startCompose();\n"
                + "    }\n"
                + "}\n";
        if (java.isFile()) {
            String old = new String(Files.readAllBytes(java.toPath()), StandardCharsets.UTF_8);
            if (!old.equals(source)) {
                File aside = new File(java.getParentFile(), name + ".java.before-kmp-import");
                Files.move(java.toPath(), aside.toPath(), StandardCopyOption.REPLACE_EXISTING);
                r.setAside = aside;
            }
        }
        java.getParentFile().mkdirs();
        write(java, source);
        r.mainClass = java;
    }

    private static void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    /// Like [#copyTree], minus the files that open a desktop window. A desktop
    /// target's `main` is `application { Window { App() } }`, which is Compose
    /// for Desktop's windowing API; here the generated main class starts the
    /// application instead, and that API does not exist.
    private static int copyJvmTree(File from, File to, Result r) throws IOException {
        int n = 0;
        File[] kids = from.listFiles();
        if (kids == null) {
            return 0;
        }
        to.mkdirs();
        for (File k : kids) {
            File dest = new File(to, k.getName());
            if (k.isDirectory()) {
                n += copyJvmTree(k, dest, r);
            } else if (k.getName().endsWith(".kt") && WINDOW.matcher(new String(Files.readAllBytes(k.toPath()),
                    StandardCharsets.UTF_8)).find()) {
                r.skippedEntryPoints.add(k);
            } else {
                Files.copy(k.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                n++;
            }
        }
        return n;
    }

    private static int copyTree(File from, File to) throws IOException {
        int n = 0;
        File[] kids = from.listFiles();
        if (kids == null) {
            return 0;
        }
        to.mkdirs();
        for (File k : kids) {
            File dest = new File(to, k.getName());
            if (k.isDirectory()) {
                n += copyTree(k, dest);
            } else {
                Files.copy(k.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                n++;
            }
        }
        return n;
    }
}
