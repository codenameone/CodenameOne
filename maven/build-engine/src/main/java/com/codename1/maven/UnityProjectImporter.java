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

/// Copies a Unity project into a Codename One application
/// (`common/src/main/unity`) and makes the application's main class run it.
/// Shared by the Maven goal `import-unity-project` and the Gradle task.
///
/// Only `Assets` and `ProjectSettings` are copied. They are the project;
/// `Library`, `Temp`, `obj` and `Logs` are what the Unity editor derives from
/// them, are routinely gigabytes, and hold the editor's own compiled
/// assemblies, which must never reach a build of ours.
///
/// The copy is a mirror of the Unity project, not a fork of it: importing
/// again replaces every file an earlier import copied and removes the ones
/// the project no longer has. The game is edited in Unity and imported again.
public final class UnityProjectImporter {

    /// The directories of a Unity project that are its source.
    static final String[] COPIED = {"Assets", "ProjectSettings"};

    /// Names never copied, wherever they are: version-control metadata and
    /// what an operating system leaves in a directory it has shown.
    static final List<String> SKIPPED = Collections.unmodifiableList(Arrays.asList(
            ".git", ".svn", ".hg", ".vs", ".idea", ".DS_Store", "Thumbs.db", "obj", "Temp", "Library"));

    /// Compiled code and native libraries a project carries as files. None of
    /// it is translated: there is C# source for none of it.
    private static final String[] BINARY_PLUGINS = {".dll", ".so", ".a", ".dylib", ".bundle", ".aar", ".jar"};

    /// The list of files the last import copied, kept in the target so the
    /// next import can remove what the project dropped.
    static final String IMPORT_RECORD = ".unity-import-files";

    public static final class Result {
        public int copiedFiles;
        public int removedFiles;
        public int scripts;
        public int scenes;
        /// `productName` of `ProjectSettings/ProjectSettings.asset`, or null.
        public String productName;
        /// Files that will be ignored by the build, each with the reason.
        public final List<String> ignored = new ArrayList<String>();
    }

    private final Log log;

    public UnityProjectImporter(Log log) {
        this.log = log;
    }

    /// @param source      the Unity project: the directory that holds `Assets`
    /// @param commonDir   the application's common module
    /// @param mainPackage `codename1.packageName`, or null to leave the main class alone
    /// @param mainClass   `codename1.mainName`, or null to leave the main class alone
    public Result importProject(File source, File commonDir, String mainPackage, String mainClass)
            throws BuildException {
        if (source == null || !source.isDirectory()) {
            throw new BuildException(source + " is not a directory; name the Unity project to import, the directory"
                    + " that holds Assets and ProjectSettings");
        }
        if (!UnityProjectBuilder.isUnityProject(source)) {
            throw new BuildException(source + " is not a Unity project: it needs both an Assets and a"
                    + " ProjectSettings directory");
        }
        File target = new File(commonDir, "src/main/unity");
        Result r = new Result();
        try {
            if (target.getCanonicalFile().equals(source.getCanonicalFile())) {
                throw new BuildException(source + " is already this application's src/main/unity");
            }
            Set<String> previous = readImportRecord(target);
            Set<String> imported = new TreeSet<String>();
            for (String name : COPIED) {
                copy(new File(source, name), new File(target, name), name, imported, r);
            }
            for (String path : previous) {
                if (!imported.contains(path)) {
                    File stale = new File(target, path);
                    if (stale.isFile() && stale.delete()) {
                        r.removedFiles++;
                    }
                }
            }
            writeImportRecord(target, imported);
            r.productName = productName(new File(source, "ProjectSettings/ProjectSettings.asset"));
            if (mainPackage != null && mainClass != null && mainPackage.length() > 0 && mainClass.length() > 0) {
                writeEntryPoint(new File(commonDir, "src/main/java"), mainPackage, mainClass);
            }
        } catch (IOException e) {
            throw new BuildException("Cannot import " + source + ": " + e.getMessage(), e);
        }
        return r;
    }

    private void copy(File src, File dest, String path, Set<String> imported, Result r) throws IOException {
        if (src.isDirectory()) {
            File[] children = src.listFiles();
            if (children == null) {
                return;
            }
            for (File f : children) {
                if (SKIPPED.contains(f.getName())) {
                    continue;
                }
                copy(f, new File(dest, f.getName()), path + "/" + f.getName(), imported, r);
            }
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("cannot create " + parent);
        }
        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        imported.add(path);
        r.copiedFiles++;
        String name = src.getName();
        if (name.endsWith(".cs")) {
            r.scripts++;
        } else if (name.endsWith(".unity")) {
            r.scenes++;
        } else {
            for (String suffix : BINARY_PLUGINS) {
                if (name.endsWith(suffix)) {
                    r.ignored.add(path + " (compiled code: only C# source is translated)");
                    break;
                }
            }
        }
    }

    /// The value of the `productName:` line of a project settings file, or
    /// null. The file is YAML when the project serializes assets as text,
    /// which is the only form the build reads either.
    static String productName(File settings) {
        if (!settings.isFile()) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(settings.toPath(), StandardCharsets.ISO_8859_1)) {
                String t = line.trim();
                if (t.startsWith("productName:")) {
                    String value = t.substring("productName:".length()).trim();
                    return value.length() == 0 ? null : value;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private void writeEntryPoint(File javaDir, String pkg, String cls) throws IOException {
        File f = new File(javaDir, pkg.replace('.', File.separatorChar) + File.separator + cls + ".java");
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("cannot create " + parent);
        }
        String src = UnityProjectBuilder.mainClassSource(pkg, cls, "",
                "/// The Codename One entry point: runs the first scene of the imported Unity project.\n");
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        if (f.isFile()) {
            File backup = new File(f.getPath() + ".pre-unity-import");
            boolean unchanged = Arrays.equals(bytes, Files.readAllBytes(f.toPath()));
            if (backup.exists()) {
                // A repeated import: the backup already holds the application's
                // original class, and overwriting it would lose the only copy.
                // The file itself is what an earlier import wrote -- unless the
                // developer has customized it since, and then it is kept.
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

    private static Set<String> readImportRecord(File target) throws IOException {
        Set<String> out = new TreeSet<String>();
        File record = new File(target, IMPORT_RECORD);
        if (record.isFile()) {
            for (String line : Files.readAllLines(record.toPath(), StandardCharsets.UTF_8)) {
                // Only paths under the two copied directories: the record is
                // a file in the project, and a line edited into it must not
                // be able to name a file elsewhere for deletion.
                if (line.indexOf("..") < 0 && (line.startsWith("Assets/") || line.startsWith("ProjectSettings/"))) {
                    out.add(line);
                }
            }
        }
        return out;
    }

    private static void writeImportRecord(File target, Set<String> imported) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String path : imported) {
            sb.append(path).append('\n');
        }
        Files.write(new File(target, IMPORT_RECORD).toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
