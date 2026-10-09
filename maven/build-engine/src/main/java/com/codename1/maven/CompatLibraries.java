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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// The third-party libraries of a desktop application, as they ship on a
/// device.
///
/// A device build has no class path: what the application uses of a library
/// is part of the application. So before the classes directory is
/// relocated, every dependency jar that is part of the application (Maven's
/// `compile` scope, Gradle's `implementation`) is classified from its
/// classes ([DependencyClassifier]) and handled by what it is:
///
/// - **a library a layer implements itself** (its classes live in packages
///   the layer owns) is never unpacked. The layer's classes ship in its
///   place, and the application's calls are relocated onto them.
/// - **a library written against Swing or JavaFX** -- a layout manager, a
///   component set -- is unpacked whole when the application uses it at all:
///   its classes name `javax/swing/JPanel`, and what ships is the relocated
///   runtime.
/// - **a pure-Java library** is unpacked as far as the application reaches
///   it and no further: the classes its own classes refer to, transitively,
///   through every other library. One stray `java.beans` reference in a JSON
///   library does not make its 800 classes application code. The reached
///   classes are relocated like the application's -- their descriptors name
///   `java.io.File`, which the application's relocated calls spell
///   differently -- and checked like them, so a library that cannot work on
///   a device fails the build instead of the device.
/// - **a library the application never reaches** is left out altogether. A
///   class only a service loader would find is found by nothing on a device.
///
/// Three kinds of jar are not touched: one written against the Codename One
/// API, the Kotlin runtime -- the build handles both by itself -- and a jar
/// with no classes. A `provided` jar is never a candidate: it says the
/// classes come from somewhere else.
///
/// #### The record
///
/// What was decided is written to [#RECORD] inside the classes directory:
/// each jar, what it was classified as, and the files that came from it.
/// Four things read it:
///
/// - the next run, which unpacks only what is new -- a class already there
///   is relocated already, and unpacking it again would undo that -- and
///   removes what the application no longer reaches;
/// - the compliance check, to say which jar a class with an unsupported
///   reference came from, since the developer did not write it;
/// - the port report, for its table of dependencies;
/// - the step that assembles the application, which must not merge a
///   recorded jar: its classes are in the application already, relocated,
///   or are not meant to be in it at all.
///
/// The record travels with the classes: a module that depends on this one
/// finds it inside this one's jar.
public final class CompatLibraries {

    /// The record's path inside a classes directory or a jar.
    public static final String RECORD = "META-INF/codenameone/compat-libraries.txt";

    /// The size from which what a library adds to the application is worth
    /// a warning.
    static final long LARGE = 4L * 1024 * 1024;

    private static final String JAR = "jar\t";
    private static final String FILE = "file\t";
    private static final String INFO = "info\t";

    private CompatLibraries() {
    }

    /// Classifies `candidates` against the desktop `layers`, unpacks into
    /// `classesDir` what the application ships of each, and answers the jars
    /// something was unpacked from. `skip` are jars that are never libraries
    /// whatever they refer to: the layers' own runtimes.
    static List<File> bundle(File classesDir, List<File> candidates, List<Relocation> layers, Set<File> skip,
                             Log log) throws IOException {
        List<File> bundled = new ArrayList<File>();
        if (candidates == null || layers.isEmpty()) {
            return bundled;
        }
        Map<String, Entry> before = read(classesDir);
        if (candidates.isEmpty() && before.isEmpty()) {
            return bundled;
        }
        List<File> jars = new ArrayList<File>();
        for (File jar : candidates) {
            if (jar != null && !skip.contains(jar)) {
                jars.add(jar);
            }
        }
        List<DependencyClassifier.Library> libraries = DependencyClassifier.classify(jars, layers);
        Set<String> unpackedBefore = new HashSet<String>();
        for (Entry e : before.values()) {
            unpackedBefore.addAll(e.files);
        }
        DependencyClassifier.reach(DependencyClassifier.applicationReferences(classesDir, unpackedBefore, layers),
                libraries);
        Map<String, Entry> after = new LinkedHashMap<String, Entry>();
        for (DependencyClassifier.Library lib : libraries) {
            if (!lib.managed()) {
                continue;
            }
            File jar = lib.file();
            Entry old = before.get(jar.getName());
            Entry entry = new Entry(jar.length() + "\t" + jar.lastModified());
            entry.describe(lib);
            Set<String> shipped = lib.shipped();
            boolean same = old != null && old.stamp.equals(entry.stamp);
            Set<String> mine = old == null ? Collections.<String>emptySet() : old.files;
            long[] written = new long[2];
            if (!shipped.isEmpty()) {
                unpack(jar, classesDir, mine, same ? mine : Collections.<String>emptySet(),
                        lib.kind() == DependencyClassifier.Kind.UI_LIBRARY ? null : shipped, entry, written, log);
                bundled.add(jar);
            }
            delete(classesDir, mine, entry.files);
            after.put(jar.getName(), entry);
            if (entry.equals(old)) {
                log.debug(jar.getName() + " is handled already and has not changed");
                continue;
            }
            announce(lib, entry, written, layers, log);
        }
        // A jar that is no longer a dependency takes its classes with it.
        for (Map.Entry<String, Entry> e : before.entrySet()) {
            if (!after.containsKey(e.getKey())) {
                delete(classesDir, e.getValue().files, allFiles(after));
            }
        }
        if (!after.equals(before)) {
            write(classesDir, after);
        }
        return bundled;
    }

    private static Set<String> allFiles(Map<String, Entry> entries) {
        Set<String> out = new HashSet<String>();
        for (Entry e : entries.values()) {
            out.addAll(e.files);
        }
        return out;
    }

    private static void delete(File classesDir, Set<String> files, Set<String> keep) throws IOException {
        for (String stale : files) {
            if (!keep.contains(stale)) {
                File f = new File(classesDir, stale);
                if (f.isFile() && !f.delete()) {
                    throw new IOException("Could not delete " + f);
                }
            }
        }
    }

    /// Says once, when it is decided or changes, what becomes of a library.
    private static void announce(DependencyClassifier.Library lib, Entry entry, long[] written,
                                 List<Relocation> layers, Log log) {
        String jar = lib.jar();
        if (lib.kind() == DependencyClassifier.Kind.LAYER_PROVIDED) {
            log.info(jar + " is provided by the " + lib.layer() + " compatibility layer: the layer's classes ship "
                    + "in its place");
            return;
        }
        if (entry.files.isEmpty()) {
            log.info(jar + " is not used by the application's classes and is left out of it");
            return;
        }
        if (written[0] == 0) {
            return;
        }
        if (lib.nativeCode() != null) {
            log.warn(jar + " " + lib.nativeCode() + ", which cannot run on a device; the classes of it that the "
                    + "application uses are checked like its own");
        }
        long kb = (lib.file().length() + 1023) / 1024;
        if (lib.kind() == DependencyClassifier.Kind.UI_LIBRARY) {
            String line = "Bundling " + jar + " with the application (" + written[0] + " classes, " + kb
                    + " KB): it is written against " + names(layers) + " and is relocated with it";
            if (lib.file().length() >= LARGE) {
                log.warn(line + ". That is a large library to ship whole; every class of it goes into the"
                        + " application");
            } else {
                log.info(line);
            }
            return;
        }
        String line = "Bundling " + written[0] + " of the " + lib.classCount() + " classes of " + jar
                + " with the application: the ones it uses, relocated and checked with it";
        if (written[1] >= LARGE) {
            log.warn(line + ". That is " + ((written[1] + 1023) / 1024) + " KB of library code in the application");
        } else {
            log.info(line);
        }
    }

    private static String names(List<Relocation> layers) {
        StringBuilder out = new StringBuilder();
        for (Relocation r : layers) {
            out.append(out.length() == 0 ? "" : " or ").append(r.name());
        }
        return out.toString();
    }

    /// Unpacks `jar` -- all of it when `classes` is null, else those classes
    /// and every resource -- leaving alone whatever the application has
    /// under the same name: its own class wins, as it does on a classpath,
    /// unless `mine` says an earlier run put the file there from this jar.
    /// A file among `current` is one that run unpacked from this very build
    /// of the jar; it is relocated by now and is kept as it is. `written`
    /// receives the number of classes written and their size.
    private static void unpack(File jar, File classesDir, Set<String> mine, Set<String> current,
                               Set<String> classes, Entry entry, long[] written, Log log) throws IOException {
        ZipFile zip = new ZipFile(jar);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                // The manifest, signatures and the Java 9 variants of a
                // multi-release jar describe the jar, not the application;
                // a desktop's native libraries are nothing a device loads.
                if (e.isDirectory() || name.startsWith("META-INF/") || name.endsWith("module-info.class")
                        || name.contains("..") || name.startsWith("/") || isNativeLibrary(name)) {
                    continue;
                }
                boolean isClass = name.endsWith(".class");
                if (isClass && classes != null
                        && !classes.contains(name.substring(0, name.length() - ".class".length()))) {
                    continue;
                }
                File out = new File(classesDir, name);
                if (out.exists() && !mine.contains(name)) {
                    log.debug(name + " of " + jar.getName() + " is the application's own; kept");
                    continue;
                }
                entry.files.add(name);
                if (current.contains(name) && out.isFile()) {
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Could not create " + parent);
                }
                InputStream in = zip.getInputStream(e);
                try {
                    byte[] bytes = readAll(in);
                    Files.write(out.toPath(), bytes);
                    if (isClass) {
                        written[0]++;
                        written[1] += bytes.length;
                    }
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
    }

    private static boolean isNativeLibrary(String name) {
        return name.endsWith(".so") || name.endsWith(".dll") || name.endsWith(".dylib") || name.endsWith(".jnilib");
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n >= 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
        return out.toByteArray();
    }

    private static final class Entry {
        private final String stamp;
        private final Set<String> files = new TreeSet<String>();
        /// What the jar was classified as, by key: see [Library].
        private final Map<String, String> info = new TreeMap<String, String>();

        Entry(String stamp) {
            this.stamp = stamp;
        }

        void describe(DependencyClassifier.Library lib) {
            info.put("kind", lib.kind().name());
            info.put("classes", String.valueOf(lib.classCount()));
            info.put("toolkit", String.valueOf(lib.toolkitClassCount()));
            info.put("reached", String.valueOf(lib.reached().size()));
            if (lib.layer() != null) {
                info.put("layer", lib.layer());
            }
            if (lib.nativeCode() != null) {
                info.put("native", lib.nativeCode());
            }
            if (lib.mainPackage() != null) {
                info.put("package", lib.mainPackage());
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Entry && ((Entry) o).stamp.equals(stamp) && ((Entry) o).files.equals(files)
                    && ((Entry) o).info.equals(info);
        }

        @Override
        public int hashCode() {
            return stamp.hashCode();
        }
    }

    /// One recorded jar: what the build decided about it.
    public static final class Library {
        private final String jar;
        private final Entry entry;

        Library(String jar, Entry entry) {
            this.jar = jar;
            this.entry = entry;
        }

        /// The jar's file name.
        public String jar() {
            return jar;
        }

        /// What it was classified as; null for a record written before
        /// jars were classified.
        public DependencyClassifier.Kind kind() {
            String kind = entry.info.get("kind");
            for (DependencyClassifier.Kind k : DependencyClassifier.Kind.values()) {
                if (k.name().equals(kind)) {
                    return k;
                }
            }
            return null;
        }

        /// How many classes the jar holds.
        public int classCount() {
            return number("classes");
        }

        /// How many of the jar's classes the application reaches.
        public int reachedCount() {
            return number("reached");
        }

        /// How many of its classes are in the application.
        public int shippedCount() {
            int n = 0;
            for (String f : entry.files) {
                if (f.endsWith(".class")) {
                    n++;
                }
            }
            return n;
        }

        /// The layer that stands in for the jar, or null.
        public String layer() {
            return entry.info.get("layer");
        }

        /// How the jar depends on native code, or null; see
        /// [DependencyClassifier.Library#nativeCode].
        public String nativeCode() {
            return entry.info.get("native");
        }

        /// The package most of the jar's classes are in, dotted and at most
        /// three parts deep, or null.
        public String mainPackage() {
            return entry.info.get("package");
        }

        private int number(String key) {
            String value = entry.info.get(key);
            if (value == null) {
                return 0;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /// Every jar `classesDir`'s record holds, by file name in the order
    /// recorded; empty when there is no record.
    public static Map<String, Library> libraries(File classesDir) {
        Map<String, Library> out = new LinkedHashMap<String, Library>();
        try {
            for (Map.Entry<String, Entry> e : read(classesDir).entrySet()) {
                out.put(e.getKey(), new Library(e.getKey(), e.getValue()));
            }
        } catch (IOException e) {
            return out;
        }
        return out;
    }

    private static Map<String, Entry> parse(String text) {
        Map<String, Entry> out = new LinkedHashMap<String, Entry>();
        Entry current = null;
        for (String line : text.split("\n")) {
            if (line.startsWith(JAR)) {
                int tab = line.indexOf('\t', JAR.length());
                if (tab > 0) {
                    current = new Entry(line.substring(tab + 1));
                    out.put(line.substring(JAR.length(), tab), current);
                }
            } else if (line.startsWith(FILE) && current != null) {
                current.files.add(line.substring(FILE.length()));
            } else if (line.startsWith(INFO) && current != null) {
                int eq = line.indexOf('=');
                if (eq > INFO.length()) {
                    current.info.put(line.substring(INFO.length(), eq), line.substring(eq + 1));
                }
            }
        }
        return out;
    }

    private static Map<String, Entry> read(File root) throws IOException {
        if (root == null) {
            return new LinkedHashMap<String, Entry>();
        }
        if (root.isDirectory()) {
            File record = new File(root, RECORD);
            return record.isFile() ? parse(new String(Files.readAllBytes(record.toPath()), StandardCharsets.UTF_8))
                    : new LinkedHashMap<String, Entry>();
        }
        if (root.isFile() && root.getName().endsWith(".jar")) {
            ZipFile zip = new ZipFile(root);
            try {
                ZipEntry e = zip.getEntry(RECORD);
                if (e != null) {
                    InputStream in = zip.getInputStream(e);
                    try {
                        return parse(new String(readAll(in), StandardCharsets.UTF_8));
                    } finally {
                        in.close();
                    }
                }
            } finally {
                zip.close();
            }
        }
        return new LinkedHashMap<String, Entry>();
    }

    private static void write(File classesDir, Map<String, Entry> entries) throws IOException {
        File record = new File(classesDir, RECORD);
        if (entries.isEmpty()) {
            if (record.isFile() && !record.delete()) {
                throw new IOException("Could not delete " + record);
            }
            return;
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            text.append(JAR).append(e.getKey()).append('\t').append(e.getValue().stamp).append('\n');
            for (Map.Entry<String, String> i : e.getValue().info.entrySet()) {
                text.append(INFO).append(i.getKey()).append('=').append(i.getValue()).append('\n');
            }
            for (String f : e.getValue().files) {
                text.append(FILE).append(f).append('\n');
            }
        }
        File parent = record.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        Files.write(record.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    }

    /// The file names of the jars recorded in any of `roots`, each a classes
    /// directory or a jar built from one: the jars whose classes are in the
    /// application already as far as they belong there, and which must not
    /// be merged into it again. A root that cannot be read has recorded
    /// nothing.
    public static Set<String> bundledJarNames(Iterable<File> roots) {
        Set<String> out = new HashSet<String>();
        if (roots != null) {
            for (File root : roots) {
                try {
                    out.addAll(read(root).keySet());
                } catch (IOException e) {
                    // Not a readable jar: then not one this step produced.
                    continue;
                }
            }
        }
        return out;
    }

    /// The jar each bundled class of `classesDir` came from, by the class's
    /// internal name; empty when nothing was bundled.
    public static Map<String, String> classOrigins(File classesDir) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        Map<String, Entry> entries;
        try {
            entries = read(classesDir);
        } catch (IOException e) {
            return out;
        }
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            for (String f : e.getValue().files) {
                if (f.endsWith(".class")) {
                    out.put(f.substring(0, f.length() - ".class".length()), e.getKey());
                }
            }
        }
        return out;
    }

    /// What a build message adds after naming a class that came from `jar`.
    public static String origin(String jar) {
        return "in the library " + jar + ", which is bundled and relocated with the application";
    }
}
