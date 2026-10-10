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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Imports a Swing or JavaFX project (a Maven or Gradle project, or one
/// module of it) into a Codename One application: copies its `src/main`
/// sources and resources into `src/main/desktop`, records how the application
/// starts ([DesktopSources#ENTRY_RECORD]), makes room for the main class the
/// build generates from that record ([DesktopEntryPoints]), and reports what
/// becomes of each of its dependencies.
///
/// Nothing is asked. The entry point is found by reading the sources, and
/// where they leave a choice the source project's own main-class setting
/// decides it; when that does not either, the import stops before copying
/// anything and lists the candidates.
public final class DesktopProjectImporter {

    /// The record an import leaves in `src/main/desktop`; see [ImportedFiles].
    static final String IMPORT_RECORD = ".desktop-import-files";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /// Coordinates a desktop layer implements, by exact `group:artifact`.
    static final Map<String, String> COVERED = new LinkedHashMap<String, String>();

    /// Coordinates a desktop layer implements, by `group:artifact` prefix.
    static final String[][] COVERED_PREFIXES = {
        {"org.swinglabs.swingx:swingx-", "SwingX"},
    };

    static {
        COVERED.put("org.openjfx:javafx-controls", "JavaFX controls");
        COVERED.put("org.openjfx:javafx-fxml", "FXML");
        COVERED.put("org.openjfx:javafx-graphics", "JavaFX graphics");
        COVERED.put("org.openjfx:javafx-base", "JavaFX base");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "Kotlin standard library");
    }

    /// What an import says about a dependency no layer implements, when it
    /// could not read the jar and say more ([DesktopImportReport]). Such a
    /// library is application code: the build bundles the classes the
    /// application uses and relocates them with it
    /// ([CompatRemapper#withApplicationLibraries]).
    public static final String BUNDLED_NOTE = "the classes the application uses are bundled and relocated; "
            + "unsupported API they use will be reported at build time";

    /// Whether `coordinate` (`group:artifact`) is a module of a toolkit a
    /// layer stands in for. One the layer does not implement cannot be added
    /// as a library: its classes are the toolkit's own.
    public static boolean isToolkitModule(String coordinate) {
        return coordinate.startsWith("org.openjfx:");
    }

    /// The suffix a main class source is set aside under.
    static final String SET_ASIDE_SUFFIX = ".pre-desktop-import";

    /// Dependencies that only matter to tests.
    static final String[] IGNORED = {"junit:", "org.junit", "org.mockito", "org.testfx", "org.hamcrest",
        "org.assertj"};

    public static final class Result {
        public final List<String> covered = new ArrayList<String>();
        /// Dependencies no layer implements, by `group:artifact`. They are
        /// not copied: add each to the application's own build, where it is
        /// handled as [#BUNDLED_NOTE] says.
        public final List<String> uncovered = new ArrayList<String>();
        /// The class the build generates to start the application
        /// (`codename1.packageName` and `codename1.mainName`), or null when
        /// the import was not told the project's.
        /// What the import could not read from the project's build, as
        /// sentences.
        public final List<String> unresolved = new ArrayList<String>();
        /// The dependencies the application's own build has to declare for
        /// the imported sources to compile, each with the scope it needs
        /// there; see [Library]. A plugin adds them to the build it is
        /// importing into.
        public final List<Library> libraries = new ArrayList<Library>();
        public String generatedMain;
        /// The source of the project's previous main class, where the import
        /// set it aside to make room for the generated one; null when there
        /// was none to move.
        public File setAside;
        /// The `module-info.java` files that were left out, relative to
        /// `src/main/desktop`. A Codename One application is not a module.
        public final List<String> droppedModuleInfo = new ArrayList<String>();
        /// The recorded entry point; null when the import kept an existing
        /// record it could not improve on.
        public String mainClass;
        /// [DesktopSources#KIND_SWING] or [DesktopSources#KIND_JAVAFX].
        public String kind;
        public boolean kotlin;
        public int copiedFiles;
        /// The Java release the project's build compiles for, or 0 when its
        /// build files do not say. See [DesktopImportReport#javaLevelWarning].
        public int javaLevel;
    }

    /// A dependency of the imported project that the application's build has
    /// to declare too.
    ///
    /// Nothing a layer authors under the toolkit's own names is one: the
    /// JavaFX layer's jar already holds `javafx.*`. SwingX is different, and
    /// for the reason Swing is -- the layer's classes live under the names
    /// they ship with, so the sources can only be compiled against the real
    /// library. It is therefore declared `provided`: there to compile
    /// against, never bundled, with the layer's own classes shipping in its
    /// place. A library no layer implements is application code and is
    /// declared `compile`, which is what makes the build bundle and
    /// relocate it ([CompatRemapper#withApplicationLibraries]).
    public static final class Library {
        public final String groupId;
        public final String artifactId;
        /// As the project's build spells it, with a property of the same
        /// build file resolved; null when the build file does not say (a
        /// managed version, a version catalog), and the dependency then has
        /// to be added by hand.
        public final String version;
        /// True for `provided`, false for `compile`.
        public final boolean provided;

        Library(String groupId, String artifactId, String version, boolean provided) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.provided = provided;
        }

        /// `group:artifact`.
        public String coordinate() {
            return groupId + ":" + artifactId;
        }
    }

    /// A class the application could be started through.
    static final class Candidate {
        final String name;
        boolean javafx;

        Candidate(String name) {
            this.name = name;
        }

        String kind() {
            return javafx ? DesktopSources.KIND_JAVAFX : DesktopSources.KIND_SWING;
        }
    }

    private final Log log;

    public DesktopProjectImporter(Log log) {
        this.log = log;
    }

    private static boolean hasSources(File dir) {
        return new File(dir, "src/main/java").isDirectory() || new File(dir, "src/main/kotlin").isDirectory();
    }

    /// The directory whose `src/main` is imported: `source/<module>` when a
    /// module is named, else `source` itself, else the one directory directly
    /// inside it that has sources.
    public static File moduleDir(File source, String module) throws BuildException {
        if (module != null && module.length() > 0) {
            File m = new File(source, module);
            if (!hasSources(m)) {
                throw new BuildException("No src/main/java or src/main/kotlin under " + m);
            }
            return m;
        }
        if (hasSources(source)) {
            return source;
        }
        List<File> found = new ArrayList<File>();
        File[] children = source.listFiles();
        if (children != null) {
            Arrays.sort(children);
            for (File c : children) {
                if (c.isDirectory() && hasSources(c)) {
                    found.add(c);
                }
            }
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new BuildException("No src/main/java or src/main/kotlin under " + source
                    + " or the directories directly inside it");
        }
        StringBuilder names = new StringBuilder();
        for (File f : found) {
            names.append(names.length() == 0 ? "" : ", ").append(f.getName());
        }
        throw new BuildException(source + " has several modules with sources (" + names
                + "); name the one to import with -Dcn1.desktop.module=");
    }

    /// Imports `source` (or its `module`) into the application whose common
    /// module is `commonDir`. `mainClassOverride`, when not null, names the
    /// entry point and no other is considered.
    public Result importProject(File source, String module, File commonDir, String mainClassOverride)
            throws BuildException {
        return importProject(source, module, commonDir, mainClassOverride, null, null);
    }

    /// As [#importProject(File, String, File, String)], for an application
    /// whose main class is `mainPackage`.`mainName` (its
    /// `codename1.packageName` and `codename1.mainName`). The build generates
    /// that class from the entry record, so a source of the same name -- the
    /// one the project template wrote -- is set aside as
    /// `<name>.java.pre-desktop-import`; one that already extends a desktop
    /// lifecycle is the developer's own and stays.
    public Result importProject(File source, String module, File commonDir, String mainClassOverride,
                                String mainPackage, String mainName) throws BuildException {
        File moduleDir = moduleDir(source, module);
        File main = new File(moduleDir, "src/main");
        File target = new File(commonDir, "src/main/desktop");
        Result r = new Result();
        try {
            // Decided from the source project, before anything is copied: an
            // import that cannot tell how the application starts leaves the
            // application as it found it.
            Map<String, Candidate> candidates = new TreeMap<String, Candidate>();
            scan(new File(main, "java"), candidates);
            scan(new File(main, "kotlin"), candidates);
            Properties existing = DesktopSources.readEntryRecord(target);
            Candidate entry = chooseEntry(candidates, mainClassOverride, declaredMainClasses(source, moduleDir),
                    existing == null ? null : existing.getProperty("mainClass"), existing != null);

            String generated = mainName == null || mainName.trim().length() == 0 ? null
                    : (mainPackage == null || mainPackage.trim().length() == 0 ? "" : mainPackage.trim() + ".")
                    + mainName.trim();
            if (generated != null && declares(main, generated)) {
                throw new BuildException("Nothing was imported: the desktop project has a class named " + generated
                        + ", which is the name of this application's main class (codename1.packageName and "
                        + "codename1.mainName). The build generates the main class, so it needs a name the "
                        + "imported sources do not use: change codename1.mainName in "
                        + "codenameone_settings.properties and import again.");
            }

            ImportedFiles files = new ImportedFiles(target, IMPORT_RECORD, "the desktop project", log)
                    .skipping("module-info.java")
                    // The application is packaged by the Codename One build,
                    // which writes the manifest of whatever it produces.
                    .skipping("MANIFEST.MF");
            for (String name : new String[] {"java", "kotlin", "resources"}) {
                File src = new File(main, name);
                if (src.isDirectory()) {
                    r.copiedFiles += files.copy(src, new File(target, name), name);
                }
            }
            files.removeStale();
            files.write();
            for (String skipped : files.skipped()) {
                if (skipped.endsWith("module-info.java")) {
                    r.droppedModuleInfo.add(skipped);
                    log.info("Left out " + skipped + ": a Codename One application is not a Java module");
                }
            }
            if (entry != null) {
                r.mainClass = entry.name;
                r.kind = entry.kind();
                writeEntryRecord(target, existing, entry);
            } else if (existing != null) {
                r.mainClass = existing.getProperty("mainClass");
                r.kind = existing.getProperty("kind");
            }
            if (generated != null && r.mainClass != null) {
                r.generatedMain = generated;
                r.setAside = setAsideMainClass(commonDir, generated);
            }
            readDependencies(moduleDir, r);
            r.javaLevel = javaLevel(source, moduleDir);
            if (ImportedFiles.hasKotlin(target)) {
                // The application's Kotlin build switches on when
                // src/main/kotlin exists; it compiles the desktop sources too.
                File kotlinDir = new File(commonDir, "src/main/kotlin");
                if (!kotlinDir.isDirectory()) {
                    if (!kotlinDir.mkdirs()) {
                        throw new IOException("Cannot create " + kotlinDir);
                    }
                    Files.write(new File(kotlinDir, ".keep").toPath(), new byte[0]);
                    log.info("The desktop sources include Kotlin: created " + kotlinDir
                            + " so the application compiles Kotlin");
                }
                r.kotlin = true;
            }
        } catch (IOException e) {
            throw new BuildException("Import failed: " + e.getMessage(), e);
        }
        return r;
    }

    /// Whether the sources under `main` declare the top-level class `name`.
    private static boolean declares(File main, String name) {
        String path = name.replace('.', '/');
        return new File(main, "java/" + path + ".java").isFile() || new File(main, "kotlin/" + path + ".kt").isFile();
    }

    /// Moves the application's own main class source out of the compiler's
    /// way, so that the class the build generates under that name is the
    /// only one. Answers where it went, or null when nothing was moved.
    private File setAsideMainClass(File commonDir, String className) throws IOException {
        String path = className.replace('.', '/');
        File[] sources = {new File(commonDir, "src/main/java/" + path + ".java"),
            new File(commonDir, "src/main/kotlin/" + path + ".kt")};
        File moved = null;
        for (File f : sources) {
            if (!f.isFile()) {
                continue;
            }
            String text = code(new String(Files.readAllBytes(f.toPath()), UTF8));
            if (text.indexOf("DesktopLifecycle") >= 0 || text.indexOf("FxLifecycle") >= 0) {
                log.info(f.getName() + " already extends a desktop lifecycle and was kept; the build generates no "
                        + "main class beside it");
                continue;
            }
            File backup = new File(f.getPath() + SET_ASIDE_SUFFIX);
            if (backup.exists()) {
                // An earlier import's copy of the original, which is the one
                // worth keeping; this file was written since.
                log.warn(f.getName() + " was written after an earlier import set the original aside ("
                        + backup.getName() + "). The build fails while both it and " + DesktopSources.ENTRY_RECORD
                        + " ask to start the application: delete one of them.");
                continue;
            }
            Files.move(f.toPath(), backup.toPath());
            log.info("Set " + f.getName() + " aside as " + backup.getName() + ": the build generates " + className
                    + " to start the imported application");
            moved = backup;
        }
        return moved;
    }

    /// Picks the entry point. Answers null only when there is no candidate
    /// and an existing record to keep.
    ///
    /// In order: the override; the only candidate; the candidate the source
    /// project's build names as its main class; the candidate an earlier
    /// import recorded. A build that names a plain launcher class, in a
    /// project with exactly one `javafx.application.Application` subclass,
    /// means that subclass: the launcher only exists to call it, and the
    /// JavaFX layer starts the application class itself.
    Candidate chooseEntry(Map<String, Candidate> candidates, String override, List<String> declared,
                          String recorded, boolean hasRecord) throws BuildException {
        if (override != null && override.length() > 0) {
            Candidate c = candidates.get(override);
            if (c == null) {
                throw new BuildException("-Dcn1.desktop.mainClass=" + override + " names no class that extends "
                        + "javafx.application.Application or declares public static void main(String[]). "
                        + describe(candidates));
            }
            return c;
        }
        if (candidates.isEmpty()) {
            if (hasRecord) {
                return null;
            }
            throw new BuildException("Nothing was imported: no class extends javafx.application.Application or "
                    + "declares public static void main(String[]), so there is nothing to start.");
        }
        if (candidates.size() == 1) {
            return candidates.values().iterator().next();
        }
        for (String name : declared) {
            Candidate c = candidates.get(name);
            if (c == null) {
                continue;
            }
            if (!c.javafx) {
                Candidate onlyApplication = null;
                int applications = 0;
                for (Candidate other : candidates.values()) {
                    if (other.javafx) {
                        onlyApplication = other;
                        applications++;
                    }
                }
                if (applications == 1) {
                    log.info("The project's main class " + name + " is a launcher; the application starts through "
                            + onlyApplication.name);
                    return onlyApplication;
                }
            }
            return c;
        }
        if (recorded != null && candidates.containsKey(recorded)) {
            return candidates.get(recorded);
        }
        throw new BuildException("Nothing was imported: the project has several classes it could start through, "
                + "and its build names none of them as the main class. " + describe(candidates)
                + " Name one with -Dcn1.desktop.mainClass=");
    }

    private static String describe(Map<String, Candidate> candidates) {
        if (candidates.isEmpty()) {
            return "The sources have no such class.";
        }
        StringBuilder b = new StringBuilder("Candidates:");
        for (Candidate c : candidates.values()) {
            b.append("\n  ").append(c.name).append(" (").append(c.kind()).append(')');
        }
        b.append('\n');
        return b.toString();
    }

    private void writeEntryRecord(File target, Properties existing, Candidate entry) throws IOException {
        if (existing != null && entry.name.equals(existing.getProperty("mainClass"))
                && entry.kind().equals(existing.getProperty("kind"))) {
            // Already says so; whatever else the developer wrote in it stays.
            return;
        }
        String text = "# How the desktop application in this directory starts; read by the Codename One build.\n"
                + "# mainClass: the class that starts it, as the sources declare it.\n"
                + "# kind: " + DesktopSources.KIND_JAVAFX + " when that class extends javafx.application.Application, "
                + DesktopSources.KIND_SWING + " when it is started through its main method.\n"
                + "mainClass=" + entry.name + "\n"
                + "kind=" + entry.kind() + "\n";
        if (!target.isDirectory() && !target.mkdirs()) {
            throw new IOException("Cannot create " + target);
        }
        Files.write(DesktopSources.entryRecord(target).toPath(), text.getBytes(UTF8));
        log.info("Recorded the entry point " + entry.name + " (" + entry.kind() + ") in "
                + DesktopSources.ENTRY_RECORD);
    }

    // ---- entry point detection -------------------------------------------

    private static final Pattern NOISE = Pattern.compile(
            "\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|/\\*.*?\\*/|//[^\\n]*",
            Pattern.DOTALL);
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)");
    private static final Pattern FX_IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+javafx\\.application\\.(?:Application|\\*)\\s*;?\\s*$");
    private static final Pattern JAVA_EXTENDS = Pattern.compile(
            "\\bclass\\s+(\\w+)\\s*(?:<[^{]*?>)?\\s*extends\\s+((?:javafx\\.application\\.)?Application)\\b");
    private static final Pattern JAVA_MAIN = Pattern.compile(
            "\\b(?:public\\s+static|static\\s+public)\\s+(?:final\\s+)?void\\s+main\\s*\\(\\s*(?:final\\s+)?String\\s*"
            + "(?:\\[\\s*\\]\\s*\\w+|\\.\\.\\.\\s*\\w+|\\s+\\w+\\s*\\[\\s*\\])\\s*\\)");
    private static final Pattern KOTLIN_EXTENDS = Pattern.compile(
            "\\bclass\\s+(\\w+)[^{\\n]*?:[^{\\n]*?((?:javafx\\.application\\.)?Application)\\s*\\(");
    private static final Pattern KOTLIN_STATIC_MAIN = Pattern.compile("@JvmStatic\\s+fun\\s+main\\s*\\(");
    private static final Pattern KOTLIN_TOP_MAIN = Pattern.compile("(?m)^fun\\s+main\\s*\\(");
    private static final Pattern KOTLIN_FILE_NAME = Pattern.compile("@file\\s*:\\s*JvmName\\s*\\(\\s*\"(\\w+)\"");

    /// Source text with its comments and string literals blanked, so a class
    /// name in a comment is not read as a declaration.
    static String code(String source) {
        return NOISE.matcher(source).replaceAll(" ");
    }

    private void scan(File dir, Map<String, Candidate> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) {
                scan(f, out);
            } else if (f.getName().endsWith(".java") && !"module-info.java".equals(f.getName())) {
                String name = f.getName().substring(0, f.getName().length() - ".java".length());
                scanJava(name, new String(Files.readAllBytes(f.toPath()), UTF8), out);
            } else if (f.getName().endsWith(".kt")) {
                String name = f.getName().substring(0, f.getName().length() - ".kt".length());
                scanKotlin(name, new String(Files.readAllBytes(f.toPath()), UTF8), out);
            }
        }
    }

    private static Candidate candidate(Map<String, Candidate> out, String name) {
        Candidate c = out.get(name);
        if (c == null) {
            c = new Candidate(name);
            out.put(name, c);
        }
        return c;
    }

    /// `fileClass` is the file's name without its extension, which is the
    /// name of the public class a Java file declares.
    static void scanJava(String fileClass, String source, Map<String, Candidate> out) {
        String code = code(source);
        Matcher pkg = PACKAGE.matcher(code);
        String prefix = pkg.find() ? pkg.group(1) + "." : "";
        boolean imported = FX_IMPORT.matcher(code).find();
        Matcher ext = JAVA_EXTENDS.matcher(code);
        while (ext.find()) {
            if (ext.group(2).indexOf('.') < 0 && !imported) {
                // Some other class called Application.
                continue;
            }
            String cls = ext.group(1);
            candidate(out, prefix + (cls.equals(fileClass) ? cls : fileClass + "$" + cls)).javafx = true;
        }
        if (JAVA_MAIN.matcher(code).find()) {
            candidate(out, prefix + fileClass);
        }
    }

    private static final Pattern KOTLIN_DECLARATION = Pattern.compile(
            "(?<!:)\\b(companion\\s+object|object|class|interface)\\b(?:\\s+(\\w+))?");

    /// The binary name, without its package, of the class a `@JvmStatic`
    /// function at `at` is a static method of: the `object` it is declared
    /// in, or the class whose `companion object` it is declared in (the
    /// static method is generated on the class, not on the companion).
    /// Nested declarations are joined with `$`. Null when the function is
    /// inside no declaration this can read, and the caller falls back on the
    /// file's name.
    ///
    /// `code` has its comments and string literals blanked, so every brace
    /// in it is a block's.
    static String kotlinDeclaration(String code, int at) {
        // What each open block is: a declaration's name, "" for a companion
        // object, null for anything else (a function body, a lambda).
        List<String> open = new ArrayList<String>();
        int header = 0;
        for (int i = 0; i < at; i++) {
            char c = code.charAt(i);
            if (c == '{') {
                String name = null;
                Matcher m = KOTLIN_DECLARATION.matcher(code.substring(header, i));
                // The last one before the brace: a property or a function
                // without a body can stand between two blocks. `X::class`
                // is not a declaration.
                while (m.find()) {
                    name = m.group(1).startsWith("companion") ? "" : m.group(2);
                }
                open.add(name);
                header = i + 1;
            } else if (c == '}') {
                if (!open.isEmpty()) {
                    open.remove(open.size() - 1);
                }
                header = i + 1;
            } else if (c == ';') {
                header = i + 1;
            }
        }
        StringBuilder out = new StringBuilder();
        for (String name : open) {
            if (name == null) {
                // Inside a function: a local declaration, which has no
                // name a launcher could use.
                return null;
            }
            if (name.length() > 0) {
                out.append(out.length() == 0 ? "" : "$").append(name);
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    static void scanKotlin(String fileClass, String source, Map<String, Candidate> out) {
        // The annotation's argument is a string literal, which code() blanks.
        Matcher named = KOTLIN_FILE_NAME.matcher(source);
        String code = code(source);
        Matcher pkg = PACKAGE.matcher(code);
        String prefix = pkg.find() ? pkg.group(1) + "." : "";
        boolean imported = FX_IMPORT.matcher(code).find();
        Matcher ext = KOTLIN_EXTENDS.matcher(code);
        while (ext.find()) {
            if (ext.group(2).indexOf('.') < 0 && !imported) {
                continue;
            }
            candidate(out, prefix + ext.group(1)).javafx = true;
        }
        Matcher staticMain = KOTLIN_STATIC_MAIN.matcher(code);
        while (staticMain.find()) {
            // The class is the declaration the function is written in, which
            // Kotlin does not tie to the file's name.
            String declared = kotlinDeclaration(code, staticMain.start());
            candidate(out, prefix + (declared == null ? fileClass : declared));
        }
        if (KOTLIN_TOP_MAIN.matcher(code).find()) {
            // A top-level function compiles into the file's facade class.
            String facade = named.find() ? named.group(1)
                    : Character.toUpperCase(fileClass.charAt(0)) + fileClass.substring(1) + "Kt";
            candidate(out, prefix + facade);
        }
    }

    // ---- the source project's build --------------------------------------

    private static String read(File f) throws IOException {
        return f.isFile() ? new String(Files.readAllBytes(f.toPath()), UTF8) : "";
    }

    /// The build files that describe the module: its own, then the ones of the
    /// project it is a module of.
    private static List<String> buildFiles(File source, File moduleDir, String... names) throws IOException {
        List<String> out = new ArrayList<String>();
        for (String name : names) {
            String text = read(new File(moduleDir, name));
            if (text.length() > 0) {
                out.add(text);
            }
        }
        if (!source.equals(moduleDir)) {
            for (String name : names) {
                String text = read(new File(source, name));
                if (text.length() > 0) {
                    out.add(text);
                }
            }
        }
        return out;
    }

    private static final Pattern POM_MAIN = Pattern.compile(
            "<(exec\\.mainClass|mainClass|main\\.class|Main-Class)>\\s*([^<\\s]+)\\s*</\\1>");
    private static final Pattern GRADLE_MAIN = Pattern.compile(
            "\\bmainClass(?:Name)?\\s*(?:=|\\.set\\s*\\(|\\()?\\s*[\"']([^\"']+)[\"']");
    private static final Pattern GRADLE_MANIFEST_MAIN = Pattern.compile(
            "[\"']Main-Class[\"']\\s*(?::|to|,)\\s*[\"']([^\"']+)[\"']");
    private static final Pattern MANIFEST_MAIN = Pattern.compile("(?m)^Main-Class:\\s*(\\S+)");

    /// The main classes the source project's build names, most specific
    /// first: `exec.mainClass` and `mainClass` in a pom, `mainClass` in a
    /// Gradle `application` or `javafx` block, and a manifest's `Main-Class`.
    static List<String> declaredMainClasses(File source, File moduleDir) throws IOException {
        Set<String> out = new LinkedHashSet<String>();
        for (String pom : buildFiles(source, moduleDir, "pom.xml")) {
            Matcher m = POM_MAIN.matcher(pom);
            while (m.find()) {
                add(out, resolve(m.group(2), pom));
            }
        }
        for (String gradle : buildFiles(source, moduleDir, "build.gradle.kts", "build.gradle")) {
            Matcher m = GRADLE_MAIN.matcher(gradle);
            while (m.find()) {
                add(out, m.group(1));
            }
            m = GRADLE_MANIFEST_MAIN.matcher(gradle);
            while (m.find()) {
                add(out, m.group(1));
            }
        }
        Matcher m = MANIFEST_MAIN.matcher(read(new File(moduleDir, "src/main/resources/META-INF/MANIFEST.MF")));
        if (m.find()) {
            add(out, m.group(1));
        }
        return new ArrayList<String>(out);
    }

    /// A pom value, with a `${property}` it consists of replaced by that
    /// property's value where the same pom defines it.
    private static String resolve(String value, String pom) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String name = value.substring(2, value.length() - 1);
            Matcher m = Pattern.compile("<" + Pattern.quote(name) + ">\\s*([^<\\s]+)\\s*</" + Pattern.quote(name) + ">")
                    .matcher(pom);
            return m.find() ? m.group(1) : null;
        }
        return value;
    }

    /// [#resolve] against `pom`, and when the property is not defined there,
    /// against each of `poms` in turn: a property is inherited too.
    private static String resolveIn(String value, String pom, List<String> poms) {
        String v = resolve(value, pom);
        for (int i = 0; v == null && i < poms.size(); i++) {
            v = resolve(value, poms.get(i));
        }
        return v;
    }

    /// A compiler plugin's own configuration: it wins over the properties.
    private static final Pattern POM_COMPILER = Pattern.compile(
            "<artifactId>\\s*maven-compiler-plugin\\s*</artifactId>(.*?)</plugin>", Pattern.DOTALL);
    private static final String[] POM_LEVEL_ELEMENTS = {"release", "source", "target"};
    private static final String[] POM_LEVEL_PROPERTIES = {"maven.compiler.release", "maven.compiler.source",
        "maven.compiler.target"};
    private static final Pattern[] GRADLE_LEVELS = {
        Pattern.compile("JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)"),
        Pattern.compile("jvmToolchain\\(\\s*(\\d+)\\s*\\)"),
        Pattern.compile("(?:sourceCompatibility|targetCompatibility|options\\.release)\\s*(?:=|\\.set\\()?\\s*"
                + "(?:JavaVersion\\.VERSION_|JavaVersion\\.toVersion\\(\\s*)?[\"']?(1[._]\\d|\\d+)"),
    };

    /// The Java release the build of `moduleDir` compiles for: what its own
    /// build file says, else what the project it is a module of says, else 0.
    ///
    /// In a pom the compiler plugin's configuration is read before the
    /// `maven.compiler.*` properties, as Maven does, and `release` before
    /// `source` before `target`. A value spelled through a property is
    /// resolved in the same file; one that cannot be is passed over.
    static int javaLevel(File source, File moduleDir) throws IOException {
        for (String pom : buildFiles(source, moduleDir, "pom.xml")) {
            Matcher plugin = POM_COMPILER.matcher(pom);
            while (plugin.find()) {
                for (String name : POM_LEVEL_ELEMENTS) {
                    int level = level(element(plugin.group(1), name), pom);
                    if (level > 0) {
                        return level;
                    }
                }
            }
            for (String name : POM_LEVEL_PROPERTIES) {
                int level = level(element(pom, name), pom);
                if (level > 0) {
                    return level;
                }
            }
        }
        for (String gradle : buildFiles(source, moduleDir, "build.gradle.kts", "build.gradle")) {
            for (Pattern p : GRADLE_LEVELS) {
                Matcher m = p.matcher(gradle);
                if (m.find()) {
                    int level = level(m.group(1), "");
                    if (level > 0) {
                        return level;
                    }
                }
            }
        }
        return 0;
    }

    /// `17`, `1.8` or `1_8` as a release number; 0 for anything else.
    private static int level(String value, String pom) {
        String v = value == null ? null : resolve(value, pom);
        if (v == null) {
            return 0;
        }
        if (v.startsWith("1.") || v.startsWith("1_")) {
            v = v.substring(2);
        }
        if (v.length() == 0 || v.length() > 3) {
            return 0;
        }
        for (int i = 0; i < v.length(); i++) {
            if (v.charAt(i) < '0' || v.charAt(i) > '9') {
                return 0;
            }
        }
        return Integer.parseInt(v);
    }

    private static void add(Set<String> out, String name) {
        if (name == null || name.indexOf("${") >= 0) {
            // A property this build file does not define.
            return;
        }
        // A modular build writes module/class.
        int slash = name.lastIndexOf('/');
        out.add(slash < 0 ? name : name.substring(slash + 1));
    }

    private static final Pattern POM_DEPENDENCY = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    private static final Pattern GRADLE_DEPENDENCY = Pattern.compile(
            "(?:implementation|api|compileOnly|runtimeOnly)\\s*\\(?\\s*[\"']([^\"':]+:[^\"':]+)(?::([^\"':@]*)[^\"']*)?[\"']");
    private static final Pattern GRADLE_FX_MODULES = Pattern.compile("\\bmodules\\s*(?:=|\\()([^\\n]*)");
    private static final Pattern GRADLE_FX_MODULE = Pattern.compile("[\"']javafx\\.(\\w+)[\"']");

    private static String element(String xml, String name) {
        Matcher m = Pattern.compile("<" + name + ">\\s*([^<\\s]+)\\s*</" + name + ">").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    /// The dependencies of the project in `moduleDir` that the application
    /// importing it has to declare: what [Result#libraries] of an import of
    /// it holds, for a plugin that edits the application's build before
    /// anything is copied.
    public static List<Library> librariesOf(File moduleDir) throws BuildException {
        Result r = new Result();
        try {
            readDependencies(moduleDir, r);
        } catch (IOException e) {
            throw new BuildException("Could not read the build of " + moduleDir + ": " + e.getMessage(), e);
        }
        return r.libraries;
    }

    private static final Pattern POM_PARENT = Pattern.compile("<parent>(.*?)</parent>", Pattern.DOTALL);
    private static final Pattern POM_NO_RELATIVE_PATH = Pattern.compile(
            "<relativePath\\s*/>|<relativePath>\\s*</relativePath>");
    /// How many parents up a module's POM is followed: deeper than any real
    /// build, and an end to two POMs that name each other.
    private static final int MAX_PARENTS = 12;

    /// The POM of the module in `moduleDir`, then its parent's, then that
    /// one's, as far as they are files of the source tree: a parent is read
    /// from where the module says it is (`relativePath`, `../pom.xml` when
    /// it says nothing), and only when the POM found there is the one the
    /// module names.
    ///
    /// A parent that is not in the tree -- one resolved from a repository,
    /// as `spring-boot-starter-parent` is -- is not fetched: an import reads
    /// the files it was given and does not run Maven. It is named in
    /// [Result#unresolved], since what it declares for its modules is then
    /// unknown here. Neither are profiles activated or imported BOMs
    /// expanded; this is the dependencies a POM spells out, not the
    /// effective model.
    static List<String> pomChain(File moduleDir, Result r) throws IOException {
        List<String> chain = new ArrayList<String>();
        File dir = moduleDir.getAbsoluteFile();
        String pom = read(new File(dir, "pom.xml"));
        if (pom.length() == 0) {
            return chain;
        }
        chain.add(pom);
        for (int depth = 0; depth < MAX_PARENTS; depth++) {
            Matcher parent = POM_PARENT.matcher(pom);
            if (!parent.find()) {
                break;
            }
            String named = element(parent.group(1), "groupId") + ":" + element(parent.group(1), "artifactId");
            String relative = element(parent.group(1), "relativePath");
            File file = null;
            // An empty relativePath is how a POM says its parent is not on disk.
            if (!POM_NO_RELATIVE_PATH.matcher(parent.group(1)).find()) {
                file = new File(dir, relative == null ? "../pom.xml" : relative);
                if (file.isDirectory()) {
                    file = new File(file, "pom.xml");
                }
            }
            String text = file == null ? "" : read(file);
            // Maven takes the file only when it is the POM asked for.
            String own = element(POM_PARENT.matcher(text).replaceAll(" "), "artifactId");
            if (text.length() == 0 || own == null || !own.equals(element(parent.group(1), "artifactId"))) {
                r.unresolved.add("the parent POM " + named + " is not among the project's files; dependencies it "
                        + "declares for its modules were not read: add the ones the application needs to its "
                        + "build by hand");
                break;
            }
            chain.add(text);
            pom = text;
            dir = file.getAbsoluteFile().getParentFile();
        }
        return chain;
    }

    static void readDependencies(File moduleDir, Result r) throws IOException {
        Set<String> coords = new LinkedHashSet<String>();
        Map<String, String> versions = new java.util.HashMap<String, String>();
        boolean catalog = false;
        // The module's own POM, then its parents': Maven hands a parent's
        // <dependencies> down to every module, so they are this module's as
        // much as the ones it spells out. (Its dependencyManagement is not:
        // that only says which version a module would get if it asked.)
        List<String> poms = pomChain(moduleDir, r);
        for (String text : poms) {
            // Managed versions and plugin dependencies are not the module's.
            String pom = text.replaceAll("(?s)<dependencyManagement>.*?</dependencyManagement>", " ")
                    .replaceAll("(?s)<build>.*?</build>", " ");
            Matcher dep = POM_DEPENDENCY.matcher(pom);
            while (dep.find()) {
                String group = element(dep.group(1), "groupId");
                String artifact = element(dep.group(1), "artifactId");
                if (group != null && artifact != null && !"test".equals(element(dep.group(1), "scope"))
                        && !coords.contains(group + ":" + artifact)) {
                    // The nearest declaration wins, as it does in Maven.
                    coords.add(group + ":" + artifact);
                    String version = element(dep.group(1), "version");
                    version = version == null ? null : resolveIn(version, pom, poms);
                    if (version != null && version.indexOf("${") < 0) {
                        versions.put(group + ":" + artifact, version);
                    }
                }
            }
        }
        for (String name : new String[] {"build.gradle.kts", "build.gradle"}) {
            String gradle = read(new File(moduleDir, name));
            Matcher g = GRADLE_DEPENDENCY.matcher(gradle);
            while (g.find()) {
                coords.add(g.group(1));
                String version = g.group(2);
                if (version != null && version.length() > 0 && version.indexOf('$') < 0) {
                    versions.put(g.group(1), version);
                }
            }
            // The JavaFX Gradle plugin's javafx { modules = [...] }.
            Matcher modules = GRADLE_FX_MODULES.matcher(gradle);
            while (modules.find()) {
                Matcher module = GRADLE_FX_MODULE.matcher(modules.group(1));
                while (module.find()) {
                    coords.add("org.openjfx:javafx-" + module.group(1));
                }
            }
            catalog |= gradle.contains("libs.");
        }
        for (String coord : coords) {
            boolean ignored = false;
            for (String i : IGNORED) {
                if (coord.startsWith(i)) {
                    ignored = true;
                }
            }
            if (ignored) {
                continue;
            }
            String covered = COVERED.get(coord);
            // Covered by a prefix: implemented by a layer under the names it
            // ships with, so still needed to compile against.
            boolean compiledAgainst = false;
            if (covered == null) {
                for (String[] prefix : COVERED_PREFIXES) {
                    if (coord.startsWith(prefix[0])) {
                        covered = prefix[1];
                        compiledAgainst = true;
                    }
                }
            }
            if (covered != null) {
                r.covered.add(coord + " (" + covered + ")");
            } else {
                r.uncovered.add(coord);
            }
            if (compiledAgainst || (covered == null && !isToolkitModule(coord))) {
                int colon = coord.indexOf(':');
                r.libraries.add(new Library(coord.substring(0, colon), coord.substring(colon + 1), versions.get(coord),
                        compiledAgainst));
            }
        }
        if (catalog) {
            r.unresolved.add("dependencies declared through a version catalog (libs.*) were not resolved; "
                    + "add the ones the application needs to its build by hand");
        }
    }
}
