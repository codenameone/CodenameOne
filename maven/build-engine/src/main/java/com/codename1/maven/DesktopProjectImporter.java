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
/// starts ([DesktopSources#ENTRY_RECORD]), and reports which of its
/// dependencies the desktop layers cover.
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
        COVERED.put("com.miglayout:miglayout-swing", "covered by the built-in MiG layout support");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "Kotlin standard library");
    }

    /// Dependencies that only matter to tests.
    static final String[] IGNORED = {"junit:", "org.junit", "org.mockito", "org.testfx", "org.hamcrest",
        "org.assertj"};

    public static final class Result {
        public final List<String> covered = new ArrayList<String>();
        public final List<String> uncovered = new ArrayList<String>();
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
            readDependencies(moduleDir, r);
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
        if (KOTLIN_STATIC_MAIN.matcher(code).find()) {
            candidate(out, prefix + fileClass);
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
            "(?:implementation|api|compileOnly|runtimeOnly)\\s*\\(?\\s*[\"']([^\"':]+:[^\"':]+)(?::[^\"']*)?[\"']");
    private static final Pattern GRADLE_FX_MODULES = Pattern.compile("\\bmodules\\s*(?:=|\\()([^\\n]*)");
    private static final Pattern GRADLE_FX_MODULE = Pattern.compile("[\"']javafx\\.(\\w+)[\"']");

    private static String element(String xml, String name) {
        Matcher m = Pattern.compile("<" + name + ">\\s*([^<\\s]+)\\s*</" + name + ">").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    static void readDependencies(File moduleDir, Result r) throws IOException {
        Set<String> coords = new LinkedHashSet<String>();
        boolean catalog = false;
        // The module's own build only: what a parent declares for every module
        // says nothing about what this one uses.
        String pom = read(new File(moduleDir, "pom.xml"));
        // Managed versions and plugin dependencies are not the module's.
        pom = pom.replaceAll("(?s)<dependencyManagement>.*?</dependencyManagement>", " ")
                .replaceAll("(?s)<build>.*?</build>", " ");
        Matcher dep = POM_DEPENDENCY.matcher(pom);
        while (dep.find()) {
            String group = element(dep.group(1), "groupId");
            String artifact = element(dep.group(1), "artifactId");
            if (group != null && artifact != null && !"test".equals(element(dep.group(1), "scope"))) {
                coords.add(group + ":" + artifact);
            }
        }
        for (String name : new String[] {"build.gradle.kts", "build.gradle"}) {
            String gradle = read(new File(moduleDir, name));
            Matcher g = GRADLE_DEPENDENCY.matcher(gradle);
            while (g.find()) {
                coords.add(g.group(1));
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
            if (covered == null) {
                for (String[] prefix : COVERED_PREFIXES) {
                    if (coord.startsWith(prefix[0])) {
                        covered = prefix[1];
                    }
                }
            }
            if (covered != null) {
                r.covered.add(coord + " (" + covered + ")");
            } else {
                r.uncovered.add(coord);
            }
        }
        if (catalog) {
            r.uncovered.add("dependencies declared through a version catalog (libs.*) were not resolved; "
                    + "check them against the supported list by hand");
        }
    }
}
