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
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// The third-party libraries of a desktop application that are themselves
/// written against Swing or JavaFX -- a layout manager, a look and feel, a
/// component set.
///
/// Such a jar cannot stay a dependency. Its classes name `javax/swing/JPanel`,
/// and what ships is the relocated runtime: left alone the library would
/// refer to classes that are not in the application. So it is treated as
/// application code. Before the classes directory is relocated, every
/// dependency jar that is part of the application (Maven's `compile` scope,
/// Gradle's `implementation`) and that refers to a desktop layer is unpacked
/// into it; from there the relocation, the compliance check and everything
/// after see its classes exactly as they see the application's own.
///
/// A `provided` jar is never unpacked: it says the classes come from
/// somewhere else, which for a desktop layer is the runtime the build ships.
/// Neither is a jar that names no desktop layer, which the build goes on
/// merging into the application as it always has.
///
/// #### The record
///
/// What was unpacked is written to [#RECORD] inside the classes directory,
/// one jar and the files that came from it. Three things read it:
///
/// - the next run, which leaves a jar that has not changed alone -- its
///   classes are relocated already, and unpacking them again would undo
///   that -- and which knows the files it may overwrite when one has;
/// - the compliance check, to say which jar a class with an unsupported
///   reference came from, since the developer did not write it;
/// - the step that assembles the application, which must not merge the jar
///   a second time under the very names it was relocated in place.
///
/// The record travels with the classes: a module that depends on this one
/// finds it inside this one's jar.
public final class CompatLibraries {

    /// The record's path inside a classes directory or a jar.
    public static final String RECORD = "META-INF/codenameone/compat-libraries.txt";

    /// The size from which a bundled jar is worth a warning: every class of
    /// it ships, used or not, until the build strips what is unreachable.
    static final long LARGE = 4L * 1024 * 1024;

    private static final String JAR = "jar\t";
    private static final String FILE = "file\t";

    private CompatLibraries() {
    }

    /// Unpacks into `classesDir` each of `candidates` that refers to one of
    /// `layers` and answers those jars. `skip` are jars that are never
    /// libraries whatever they refer to: the layers' own runtimes.
    static List<File> bundle(File classesDir, List<File> candidates, List<Relocation> layers, Set<File> skip,
                             Log log) throws IOException {
        List<File> bundled = new ArrayList<File>();
        if (candidates == null || candidates.isEmpty() || layers.isEmpty()) {
            return bundled;
        }
        Map<String, Entry> before = read(classesDir);
        Map<String, Entry> after = new LinkedHashMap<String, Entry>();
        Set<String> seen = new HashSet<String>();
        for (File jar : candidates) {
            if (jar == null || !jar.isFile() || !jar.getName().endsWith(".jar") || skip.contains(jar)
                    || !seen.add(jar.getName()) || !CompatLayers.jarRefersTo(jar, layers)) {
                continue;
            }
            bundled.add(jar);
            Entry old = before.get(jar.getName());
            String stamp = jar.length() + "\t" + jar.lastModified();
            if (old != null && old.stamp.equals(stamp) && old.allPresent(classesDir)) {
                after.put(jar.getName(), old);
                log.debug(jar.getName() + " is bundled already and has not changed");
                continue;
            }
            Entry entry = new Entry(stamp);
            int classes = unpack(jar, classesDir, old == null ? Collections.<String>emptySet() : old.files, entry,
                    log);
            after.put(jar.getName(), entry);
            long kb = (jar.length() + 1023) / 1024;
            String line = "Bundling " + jar.getName() + " with the application (" + classes + " classes, " + kb
                    + " KB): it is written against " + names(layers) + " and is relocated with it";
            if (jar.length() >= LARGE) {
                log.warn(line + ". That is a large library to ship whole; every class of it goes into the"
                        + " application");
            } else {
                log.info(line);
            }
        }
        if (!after.equals(before)) {
            write(classesDir, after);
        }
        return bundled;
    }

    private static String names(List<Relocation> layers) {
        StringBuilder out = new StringBuilder();
        for (Relocation r : layers) {
            out.append(out.length() == 0 ? "" : " or ").append(r.name());
        }
        return out.toString();
    }

    /// Unpacks `jar`, leaving alone whatever the application has under the
    /// same name -- its own class wins, as it does on a classpath -- unless
    /// `mine` says an earlier run put the file there from this jar.
    private static int unpack(File jar, File classesDir, Set<String> mine, Entry entry, Log log)
            throws IOException {
        int classes = 0;
        ZipFile zip = new ZipFile(jar);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                // The manifest, signatures and the Java 9 variants of a
                // multi-release jar describe the jar, not the application.
                if (e.isDirectory() || name.startsWith("META-INF/") || name.endsWith("module-info.class")
                        || name.contains("..") || name.startsWith("/")) {
                    continue;
                }
                File out = new File(classesDir, name);
                if (out.exists() && !mine.contains(name)) {
                    log.debug(name + " of " + jar.getName() + " is the application's own; kept");
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Could not create " + parent);
                }
                InputStream in = zip.getInputStream(e);
                try {
                    Files.write(out.toPath(), readAll(in));
                } finally {
                    in.close();
                }
                entry.files.add(name);
                if (name.endsWith(".class")) {
                    classes++;
                }
            }
        } finally {
            zip.close();
        }
        return classes;
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

        Entry(String stamp) {
            this.stamp = stamp;
        }

        boolean allPresent(File classesDir) {
            for (String f : files) {
                if (!new File(classesDir, f).isFile()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Entry && ((Entry) o).stamp.equals(stamp) && ((Entry) o).files.equals(files);
        }

        @Override
        public int hashCode() {
            return stamp.hashCode();
        }
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

    /// The file names of the jars bundled into any of `roots`, each a
    /// classes directory or a jar built from one. A root that cannot be read
    /// has bundled nothing.
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
