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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/// Copies a Python project's sources into an application's
/// `common/src/main/python`, for `cn1:import-python-project` and the Gradle
/// plugin's `importPythonProject`.
///
/// The source root is the project's `src` directory when it has the
/// `src/<package>` layout, else the project directory itself. Only `.py`
/// files are copied, never a virtual environment, a cache or a build
/// directory. The entry module is found the way `python -m` and the
/// project's metadata name it, and a Codename One main class that runs it is
/// written. Importing again mirrors the project afresh: a file the project no
/// longer has is removed, through a record of what the last import wrote.
public final class PythonProjectImporter {
    /// Directories that are never part of a program's source.
    static final List<String> SKIPPED = Collections.unmodifiableList(Arrays.asList(
            ".git", ".hg", ".svn", ".venv", "venv", "env", ".env", "__pycache__", ".tox", ".nox", ".mypy_cache",
            ".pytest_cache", ".ruff_cache", "build", "dist", "node_modules", ".idea", ".vscode", "site-packages"));

    /// The settings key naming the entry module, read by the build.
    public static final String MAIN_MODULE_SETTING = "codename1.python.mainModule";

    static final String RECORD = ".cn1-python-import";

    private final Log log;

    public static final class Result {
        public int copiedFiles;
        public int removedFiles;
        /// The module run as `__main__` (or `module:function`), or null for a library.
        public String mainModule;
        /// Requirements the project declares, which have to be provided as
        /// source under `src/main/python` or by the runtime.
        public final List<String> requirements = new ArrayList<String>();
    }

    public PythonProjectImporter(Log log) {
        this.log = log;
    }

    /// @param source      the Python project directory
    /// @param commonDir   the application's common module
    /// @param mainPackage `codename1.packageName`, or null to leave the main class alone
    /// @param mainClass   `codename1.mainName`, or null to leave the main class alone
    public Result importProject(File source, File commonDir, String mainPackage, String mainClass)
            throws BuildException {
        if (source == null || !source.isDirectory()) {
            throw new BuildException(source + " is not a directory; name the Python project to import");
        }
        File root = sourceRoot(source);
        if (!PythonProjectBuilder.isPythonProject(root)) {
            throw new BuildException(source + " has no Python sources");
        }
        File target = new File(commonDir, "src/main/python");
        Result r = new Result();
        try {
            if (target.getCanonicalFile().equals(root.getCanonicalFile())) {
                throw new BuildException(source + " is already this application's src/main/python");
            }
            Set<String> previous = readRecord(target);
            Set<String> imported = new TreeSet<String>();
            copy(root, target, "", imported, r);
            for (String path : previous) {
                if (!imported.contains(path)) {
                    File stale = new File(target, path);
                    if (stale.isFile() && !Files.isSymbolicLink(stale.toPath()) && stale.delete()) {
                        r.removedFiles++;
                    }
                }
            }
            writeRecord(target, imported);
            String pyproject = read(new File(source, "pyproject.toml"));
            r.mainModule = mainModule(root, pyproject);
            r.requirements.addAll(requirements(pyproject, read(new File(source, "requirements.txt"))));
            if (r.mainModule != null) {
                updateSetting(new File(commonDir, "codenameone_settings.properties"), r.mainModule);
                if (mainPackage != null && mainClass != null && mainPackage.length() > 0 && mainClass.length() > 0) {
                    writeEntryPoint(new File(commonDir, "src/main/java"), mainPackage, mainClass);
                }
            }
        } catch (IOException e) {
            throw new BuildException("Cannot import " + source + ": " + e.getMessage(), e);
        }
        return r;
    }

    /// `src` in the `src/<package>` layout, else the project itself.
    static File sourceRoot(File project) {
        File src = new File(project, "src");
        if (src.isDirectory() && PythonProjectBuilder.isPythonProject(src)) {
            return src;
        }
        return project;
    }

    /// The entry point: `[project.scripts]`'s first target (`module:function`), else a
    /// package's `__main__`, else `main`, `app` or `__main__` at the root.
    static String mainModule(File root, String pyproject) {
        if (pyproject != null) {
            String scripts = section(pyproject, "project.scripts");
            if (scripts != null) {
                for (String line : scripts.split("\n")) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String v = line.substring(eq + 1).trim().replace("\"", "").replace("'", "");
                        int colon = v.indexOf(':');
                        String mod = colon >= 0 ? v.substring(0, colon).trim() : v;
                        if (new File(root, mod.replace('.', '/') + ".py").isFile()
                                || new File(root, mod.replace('.', '/') + "/__init__.py").isFile()) {
                            return v.trim();
                        }
                    }
                }
            }
        }
        File[] children = root.listFiles();
        if (children != null) {
            Arrays.sort(children);
            for (File c : children) {
                if (c.isDirectory() && new File(c, "__main__.py").isFile() && new File(c, "__init__.py").isFile()) {
                    return c.getName() + ".__main__";
                }
            }
        }
        for (String m : new String[] {"main", "app", "__main__"}) {
            if (new File(root, m + ".py").isFile()) {
                return m;
            }
        }
        return null;
    }

    /// The body of a TOML table, up to the next header, or null.
    static String section(String toml, String name) {
        String header = "[" + name + "]";
        int at = toml.indexOf(header);
        if (at < 0) {
            return null;
        }
        int start = at + header.length();
        int end = toml.indexOf("\n[", start);
        return end < 0 ? toml.substring(start) : toml.substring(start, end);
    }

    /// The distribution names a project depends on: `[project] dependencies`
    /// and `requirements.txt`, without versions, markers or comments.
    static List<String> requirements(String pyproject, String requirementsTxt) {
        Set<String> names = new TreeSet<String>();
        if (pyproject != null) {
            String project = section(pyproject, "project");
            if (project != null) {
                int at = project.indexOf("dependencies");
                int open = at < 0 ? -1 : project.indexOf('[', at);
                int close = open < 0 ? -1 : project.indexOf(']', open);
                if (close > open) {
                    for (String item : project.substring(open + 1, close).split(",")) {
                        add(names, item.trim().replace("\"", "").replace("'", ""));
                    }
                }
            }
        }
        if (requirementsTxt != null) {
            for (String line : requirementsTxt.split("\n")) {
                String l = line.trim();
                if (!l.startsWith("-")) {
                    add(names, l);
                }
            }
        }
        return new ArrayList<String>(names);
    }

    private static void add(Set<String> names, String spec) {
        int hash = spec.indexOf('#');
        String s = hash >= 0 ? spec.substring(0, hash) : spec;
        int end = 0;
        while (end < s.length()) {
            char c = s.charAt(end);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')) {
                break;
            }
            end++;
        }
        if (end > 0) {
            names.add(s.substring(0, end));
        }
    }

    private void copy(File src, File dest, String path, Set<String> imported, Result r) throws IOException {
        if (path.length() > 0 && Files.isSymbolicLink(src.toPath())) {
            log.warn("Skipped " + path + ": a symbolic link");
            return;
        }
        if (src.isDirectory()) {
            File[] children = src.listFiles();
            if (children == null) {
                return;
            }
            Arrays.sort(children);
            for (File f : children) {
                String n = f.getName();
                if (SKIPPED.contains(n) || n.endsWith(".egg-info") || n.endsWith(".dist-info")) {
                    continue;
                }
                if (f.isDirectory() || n.endsWith(".py")) {
                    copy(f, new File(dest, n), path.length() == 0 ? n : path + "/" + n, imported, r);
                }
            }
            return;
        }
        if (Files.isSymbolicLink(dest.toPath()) || dest.isDirectory()) {
            throw new IOException(dest + " is in the way of " + path);
        }
        File parent = dest.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        imported.add(path);
        r.copiedFiles++;
    }

    private static String read(File f) throws IOException {
        return f.isFile() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : null;
    }

    private static Set<String> readRecord(File target) throws IOException {
        Set<String> paths = new TreeSet<String>();
        String text = read(new File(target, RECORD));
        if (text != null) {
            for (String line : text.split("\n")) {
                String p = line.trim();
                // Never a path that climbs out of the import root.
                if (p.length() > 0 && !p.startsWith("/") && !("/" + p + "/").contains("/../")) {
                    paths.add(p);
                }
            }
        }
        return paths;
    }

    private static void writeRecord(File target, Set<String> imported) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String p : imported) {
            sb.append(p).append('\n');
        }
        Files.createDirectories(target.toPath());
        Files.write(new File(target, RECORD).toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /// Sets [#MAIN_MODULE_SETTING], keeping every other line of the file as it is.
    static void updateSetting(File settings, String module) throws IOException {
        String line = MAIN_MODULE_SETTING + "=" + module;
        List<String> lines = new ArrayList<String>();
        boolean found = false;
        String text = read(settings);
        if (text != null) {
            for (String l : text.split("\n", -1)) {
                if (l.startsWith(MAIN_MODULE_SETTING + "=")) {
                    lines.add(line);
                    found = true;
                } else {
                    lines.add(l);
                }
            }
            if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
                lines.remove(lines.size() - 1);
            }
        }
        if (!found) {
            lines.add(line);
        }
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        Files.write(settings.toPath(), sb.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    private void writeEntryPoint(File javaDir, String pkg, String cls) throws IOException {
        File f = new File(javaDir, pkg.replace('.', File.separatorChar) + File.separator + cls + ".java");
        Files.createDirectories(f.getParentFile().toPath());
        String src = "package " + pkg + ";\n\n"
                + "import com.codename1.system.Lifecycle;\n\n"
                + "/// The Codename One entry point: runs the imported Python program's main module.\n"
                + "public class " + cls + " extends Lifecycle {\n"
                + "    @Override\n"
                + "    public void runApp() {\n"
                + "        com.codename1.generated.python.PythonModules.runMain();\n"
                + "    }\n"
                + "}\n";
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        if (f.isFile()) {
            File backup = new File(f.getPath() + ".pre-python-import");
            boolean unchanged = Arrays.equals(bytes, Files.readAllBytes(f.toPath()));
            if (backup.exists()) {
                if (!unchanged) {
                    log.warn(f.getName() + " was changed after an earlier import and was left as it is; "
                            + "delete it and import again to regenerate it");
                    return;
                }
            } else if (!unchanged) {
                Files.copy(f.toPath(), backup.toPath());
                log.info("Saved the previous " + f.getName() + " as " + backup.getName());
            }
        }
        Files.write(f.toPath(), bytes);
    }
}
