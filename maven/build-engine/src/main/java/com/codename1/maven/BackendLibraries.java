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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Backend libraries: jars whose classes a server module builds as its own.
///
/// A server is wired at build time. Its routers, its bean wiring and the dao of
/// every entity are generated from the annotated classes the build scans, and
/// the build scans the module's own output. A library's `@RestController`,
/// `@Service` or `@Entity` was therefore compiled, shipped and never wired: no
/// route, no bean, no table.
///
/// A jar that carries [#MARKER] says it is meant to be wired by the server that
/// uses it -- the backend part of a cn1lib (`backend.zip` inside the `.cn1lib`)
/// or a backend-only library. Before anything scans the module, its classes and
/// resources are copied into the module's output directory, so every processor,
/// every bytecode rewrite and the native translation see one application and
/// nothing has to know where a class came from. The library is not processed
/// when it is built: generated wiring has fixed names (`cn1app.DaoBootstrap`),
/// and a library's copy would collide with the application's.
///
/// The module's own classes always win. What was copied is listed in
/// [#RECORD] with its size and checksum, so the next build replaces or removes
/// exactly those files -- a library upgraded or dropped leaves nothing stale
/// behind. A listed file whose content changed since is no longer the
/// library's: the module compiled a class of that name over it, and it stays.
public final class BackendLibraries {

    /// The entry that marks a jar as a backend library. Its content is ignored.
    public static final String MARKER = "META-INF/codenameone/backend-library";

    /// What the last merge copied: one line per file, the output-relative path,
    /// its size and its CRC-32, separated by tabs.
    static final String RECORD = "META-INF/codenameone/backend-libraries.merged";

    private BackendLibraries() {
    }

    /// Whether `element`, a classpath entry, is a backend library.
    public static boolean isLibrary(File element) {
        if (element.isDirectory()) {
            return new File(element, MARKER).isFile();
        }
        if (!element.isFile()) {
            return false;
        }
        try (ZipFile zip = new ZipFile(element)) {
            return zip.getEntry(MARKER) != null;
        } catch (IOException notAnArchive) {
            return false;
        }
    }

    /// Copies every backend library on `classpath` into `output`.
    ///
    /// @param classpath classpath elements, in classpath order; the first
    ///        library to define a path wins, as on a classpath
    /// @param output the module's compiled classes
    /// @return the number of libraries merged
    public static int merge(List<String> classpath, File output) throws IOException {
        File record = new File(output, RECORD);
        Map<String, String> previous = read(record);
        // Undo the last merge first: a file still listed there, with the
        // content the merge wrote, is a library's, and whatever the libraries
        // hold now is copied back below. One whose content changed was written
        // since by the module's own compile, and is the module's.
        for (Map.Entry<String, String> e : previous.entrySet()) {
            File stale = new File(output, e.getKey());
            if (stale.isFile() && (e.getValue() == null || e.getValue().equals(stamp(stale)))
                    && !stale.delete()) {
                throw new IOException("Could not remove " + stale);
            }
        }
        Set<String> merged = new LinkedHashSet<String>();
        int libraries = 0;
        if (classpath != null) {
            File own = canonical(output);
            for (String path : classpath) {
                File element = new File(path);
                if (canonical(element).equals(own) || !isLibrary(element)) {
                    continue;
                }
                libraries++;
                if (element.isDirectory()) {
                    copyDirectory(element, element, output, merged);
                } else {
                    copyArchive(element, output, merged);
                }
            }
        }
        if (merged.isEmpty()) {
            if (record.isFile() && !record.delete()) {
                throw new IOException("Could not remove " + record);
            }
            return libraries;
        }
        mkdirs(record.getParentFile());
        try (Writer out = new OutputStreamWriter(new FileOutputStream(record), StandardCharsets.UTF_8)) {
            for (String path : merged) {
                out.write(path);
                out.write('\t');
                out.write(stamp(new File(output, path)));
                out.write('\n');
            }
        }
        return libraries;
    }

    /// Whether a library entry is copied: class files and resources, but not the
    /// jar's own metadata, which describes the library and not the application.
    private static boolean wanted(String name) {
        if (name.startsWith("META-INF/")) {
            // A service registration is copied like any resource; when the
            // module registers the same service, the module's file wins.
            return !name.equals("META-INF/MANIFEST.MF") && !name.equals(MARKER)
                    && !name.startsWith("META-INF/maven/") && !name.endsWith(".SF")
                    && !name.endsWith(".RSA") && !name.endsWith(".DSA")
                    && !name.equals(RECORD);
        }
        return !name.equals("module-info.class");
    }

    private static void copyArchive(File jar, File output, Set<String> merged) throws IOException {
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !wanted(name) || name.contains("..")) {
                    continue;
                }
                File target = new File(output, name);
                if (target.exists()) {
                    // The module's own file, or an earlier library's.
                    continue;
                }
                mkdirs(target.getParentFile());
                try (InputStream in = zip.getInputStream(entry)) {
                    copy(in, target);
                }
                merged.add(name);
            }
        }
    }

    private static void copyDirectory(File root, File dir, File output, Set<String> merged)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                copyDirectory(root, child, output, merged);
                continue;
            }
            String name = root.toURI().relativize(child.toURI()).getPath();
            if (!wanted(name)) {
                continue;
            }
            File target = new File(output, name);
            if (target.exists()) {
                continue;
            }
            mkdirs(target.getParentFile());
            try (InputStream in = new FileInputStream(child)) {
                copy(in, target);
            }
            merged.add(name);
        }
    }

    /// A file's size and CRC-32, as the record stores them.
    private static String stamp(File file) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                crc.update(buffer, 0, n);
            }
        }
        return file.length() + "\t" + Long.toHexString(crc.getValue());
    }

    /// The record's paths, each with its stamp; null for a line written
    /// without one.
    private static Map<String, String> read(File record) throws IOException {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (!record.isFile()) {
            return out;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(record), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                int tab = line.indexOf('\t');
                String path = tab < 0 ? line : line.substring(0, tab);
                if (!path.isEmpty() && !path.contains("..")) {
                    out.put(path, tab < 0 ? null : line.substring(tab + 1));
                }
            }
        }
        return out;
    }

    /// Stamps every file the last merge into `output` copied with its current
    /// content. The build calls this once it has finished rewriting classes --
    /// bean weaving and the ORM enhancer change merged classes in place -- so
    /// the next merge recognizes them as the library's. A file the stamp at
    /// merge time no longer matched before this runs would be the module's, but
    /// nothing in a build writes a class over a merged one between the merge
    /// and here except those rewrites.
    public static void restamp(File output) throws IOException {
        File record = new File(output, RECORD);
        Map<String, String> listed = read(record);
        if (listed.isEmpty()) {
            return;
        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(record), StandardCharsets.UTF_8)) {
            for (String path : listed.keySet()) {
                File file = new File(output, path);
                if (!file.isFile()) {
                    continue;
                }
                out.write(path);
                out.write('\t');
                out.write(stamp(file));
                out.write('\n');
            }
        }
    }

    /// What the last merge into `output` copied, as output-relative paths.
    public static Set<String> mergedPaths(File output) throws IOException {
        return new LinkedHashSet<String>(read(new File(output, RECORD)).keySet());
    }

    /// The classpath entries that are backend libraries.
    public static List<String> libraries(List<String> classpath) {
        List<String> out = new ArrayList<String>();
        if (classpath != null) {
            for (String path : classpath) {
                if (isLibrary(new File(path))) {
                    out.add(path);
                }
            }
        }
        return out;
    }

    private static void copy(InputStream in, File target) throws IOException {
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static void mkdirs(File dir) throws IOException {
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Could not create " + dir);
        }
    }

    private static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }
}
