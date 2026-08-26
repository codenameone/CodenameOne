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
package com.codename1.builders;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Answers which class-name prefixes appear anywhere inside a folder of
 * submitted libraries.
 *
 * <p>The builders' own scanner reads the application's compiled classes and
 * never opens a submitted jar, so a feature used <em>only</em> by a cn1lib is
 * invisible to it: the app gets no permissions, no services, no native
 * defines, and the library calls an API that was never switched on. Nearby
 * solved this with its own private tree-and-archive walker; this is that
 * walker with the feature-specific parts taken out, so the next family does
 * not need a third copy.</p>
 *
 * <p><b>Keep this file in sync with
 * {@code com.codename1.build.daemon.LibraryClassPrefixScan}.</b></p>
 *
 * <p>Detection is by raw byte search rather than by parsing the constant
 * pool. That is deliberately crude and deliberately generous: a false
 * positive costs an unused permission or a few kilobytes of native code, and
 * a false negative costs a feature that silently does not work. Obfuscated
 * and shaded archives are exactly where the parse would fail and the search
 * still succeeds.</p>
 */
final class LibraryClassPrefixScan {

    /** How deep a nested archive is followed. */
    private static final int MAX_DEPTH = 3;

    private LibraryClassPrefixScan() {
    }

    /**
     * Returns the subset of {@code prefixes} found under {@code root}.
     *
     * @param root     the submitted-libraries folder, may be null or absent
     * @param prefixes slash-separated class-name prefixes to look for
     * @return the prefixes that were found, never null
     */
    static Set<String> prefixesFound(File root, String[] prefixes) {
        Set<String> found = new HashSet<String>();
        if (root == null || !root.isDirectory() || prefixes == null
                || prefixes.length == 0) {
            return found;
        }
        scanTree(root, prefixes, found, 0);
        return found;
    }

    private static void scanTree(File dir, String[] prefixes, Set<String> found,
            int depth) {
        if (found.size() == prefixes.length) {
            // Everything asked about has been seen; nothing later can change
            // the answer.
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String name = child.getName().toLowerCase(Locale.ROOT);
            if (child.isDirectory()) {
                scanTree(child, prefixes, found, depth);
            } else if (name.endsWith(".jar") || name.endsWith(".aar")
                    || name.endsWith(".zip")) {
                scanArchive(child, prefixes, found, depth);
            } else if (name.endsWith(".class")) {
                // The same skip the archive branch makes: a loose tree can
                // carry unpacked API definitions, and a definition's own
                // constant pool names itself. Round 30 fixed only the packed
                // case.
                if (isFrameworkClass(child.getPath(), prefixes)) {
                    continue;
                }
                inspect(readAll(child), prefixes, found);
            }
        }
    }

    private static void scanArchive(File archive, String[] prefixes,
            Set<String> found, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        ZipFile zip = null;
        try {
            zip = new ZipFile(archive);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (found.size() == prefixes.length) {
                    return;
                }
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = entry.getName().toLowerCase(Locale.ROOT);
                if (lower.endsWith(".class")) {
                    if (isFrameworkClass(entry.getName(), prefixes)) {
                        // The API definition itself, bundled into a fat jar.
                        // Its own constant pool necessarily names its own
                        // class, so inspecting it reports usage that nothing
                        // in the app or the library actually has -- and on
                        // iOS that false hit generates the directory
                        // extension and then demands a provisioning profile
                        // for it, failing an otherwise valid build. The
                        // nearby scanner skips these for the same reason.
                        continue;
                    }
                    try {
                        inspect(readAll(zip.getInputStream(entry)), prefixes,
                                found);
                    } catch (Throwable unreadable) {
                        // One bad entry says nothing about the next.
                        continue;
                    }
                } else if (lower.endsWith(".jar")) {
                    // An Android archive keeps its bytecode in a nested
                    // classes.jar, so the entries that matter are one level in.
                    try {
                        inspectNested(readAll(zip.getInputStream(entry)),
                                prefixes, found, depth + 1);
                    } catch (Throwable unreadable) {
                        continue;
                    }
                }
            }
        } catch (Throwable unreadable) {
            // Not an archive, or a broken one. Guessing that it uses
            // everything would charge the whole apparatus to any app that
            // ships a stray file.
            return;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Throwable ignored) {
                    // Nothing useful to do about a failed close.
                }
            }
        }
    }

    private static void inspectNested(byte[] archiveBytes, String[] prefixes,
            Set<String> found, int depth) {
        if (archiveBytes == null || archiveBytes.length == 0
                || depth > MAX_DEPTH) {
            return;
        }
        File temp = null;
        try {
            temp = File.createTempFile("cn1scan", ".jar");
            java.io.FileOutputStream out = new java.io.FileOutputStream(temp);
            try {
                out.write(archiveBytes);
            } finally {
                out.close();
            }
            scanArchive(temp, prefixes, found, depth);
        } catch (Throwable unreadable) {
            return;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    private static void inspect(byte[] bytes, String[] prefixes,
            Set<String> found) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        String text;
        try {
            // ISO-8859-1 so every byte maps to a char and nothing is lost;
            // the prefixes are ASCII, so a match is a match.
            text = new String(bytes, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException never) {
            return;
        }
        for (int i = 0; i < prefixes.length; i++) {
            if (!found.contains(prefixes[i]) && text.indexOf(prefixes[i]) >= 0) {
                found.add(prefixes[i]);
            }
        }
    }

    /// Whether this entry IS one of the classes being looked for, rather
    /// than something that references one.
    ///
    /// A fat jar that bundles the Codename One API carries
    /// com/codename1/call/directory/CallDirectory.class, whose constant pool
    /// contains its own name; counting that as usage turns every such jar
    /// into a directory-enabled build.
    static boolean isFrameworkClass(String entryName, String[] prefixes) {
        String path = entryName.replace('\\', '/');
        int at = path.indexOf("com/codename1/");
        if (at < 0) {
            return false;
        }
        String fromRoot = path.substring(at);
        for (String prefix : prefixes) {
            if (fromRoot.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readAll(File file) {
        try {
            InputStream in = new java.io.FileInputStream(file);
            return readAll(in);
        } catch (java.io.IOException unreadable) {
            return null;
        }
    }

    private static byte[] readAll(InputStream in) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        try {
            int read = in.read(buffer);
            while (read > 0) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
        } catch (java.io.IOException unreadable) {
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (java.io.IOException ignored) {
                // Nothing useful to do about a failed close.
            }
        }
        return out.toByteArray();
    }
}
