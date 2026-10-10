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
package com.codename1.maven.annotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Reads the classes a module's dependencies contribute, for the processors
/// that generate code from a library the application shares between its halves.
///
/// Three kinds of entry are left out:
///
/// - The module's own output. It is first on Maven's compile classpath and has
///   already been scanned as the module itself.
/// - The Codename One runtimes. They are thousands of classes, none of them
///   the application's, and reading them would cost every build for nothing.
/// - `java/`, `javax/` and `kotlin/`, for the same reason.
///
/// A class file is parsed only when its bytes hold one of the wanted annotation
/// descriptors. A descriptor an annotation is applied with is a constant pool
/// entry, so it appears in the file verbatim; a class that does not contain the
/// text cannot carry the annotation, and most classes are dismissed on that test
/// without being parsed at all.
public final class DependencyClasses {

    /// Marks an entry as the client runtime.
    private static final String CORE_MARKER = "com/codename1/ui/Display.class";

    /// Marks an entry as the server runtime.
    private static final String BACKEND_MARKER = "com/codename1/backend/Backend.class";

    private DependencyClasses() {
    }

    /// Every dependency class carrying one of `descriptors`, keyed by internal
    /// name. The first definition of a name wins, as it does on a classpath.
    ///
    /// @param compileClasspath the module's compile classpath
    /// @param ownOutput the module's output directory, or null
    /// @param descriptors annotation descriptors, in `Lcom/example/Name;` form
    public static Map<String, AnnotatedClass> scan(List<String> compileClasspath,
            File ownOutput, Iterable<String> descriptors) throws ProcessingException {
        Map<String, AnnotatedClass> out = new LinkedHashMap<String, AnnotatedClass>();
        if (compileClasspath == null) {
            return out;
        }
        java.util.List<byte[]> needles = new java.util.ArrayList<byte[]>();
        for (String descriptor : descriptors) {
            needles.add(ascii(descriptor));
        }
        if (needles.isEmpty()) {
            return out;
        }
        File own = canonical(ownOutput);
        for (String element : compileClasspath) {
            File file = new File(element);
            if (file.isDirectory()) {
                if (own != null && own.equals(canonical(file))) {
                    continue;
                }
                if (new File(file, CORE_MARKER).isFile() || new File(file, BACKEND_MARKER).isFile()) {
                    continue;
                }
                scanDirectory(file, file, needles, out);
            } else if (file.isFile()) {
                scanArchive(file, needles, out);
            }
        }
        return out;
    }

    /// Whether `entryName` is on the classpath, as a file in a directory entry
    /// or an entry of an archive. Nothing is loaded.
    public static boolean onClasspath(List<String> compileClasspath, String entryName) {
        return onClasspath(compileClasspath, null, entryName);
    }

    /// Whether a DEPENDENCY supplies `entryName`: the same question with the
    /// module's own output left out of it.
    ///
    /// The output directory is on the module's compile classpath, and it holds
    /// what the previous build generated. Asked without this exclusion, "has a
    /// dependency already generated this class?" is answered yes by the
    /// module's own last build, so the second build of a module generated
    /// nothing, left the stale class in place and dropped it from the
    /// bootstrap that registers it.
    ///
    /// @param ownOutput the module's output directory, or null to consider every entry
    public static boolean onClasspath(List<String> compileClasspath, File ownOutput,
            String entryName) {
        if (compileClasspath == null) {
            return false;
        }
        File own = canonical(ownOutput);
        for (String element : compileClasspath) {
            File file = new File(element);
            if (file.isDirectory()) {
                if (own != null && own.equals(canonical(file))) {
                    continue;
                }
                if (new File(file, entryName.replace('/', File.separatorChar)).isFile()) {
                    return true;
                }
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            try {
                ZipFile zip = new ZipFile(file);
                try {
                    if (zip.getEntry(entryName) != null) {
                        return true;
                    }
                } finally {
                    zip.close();
                }
            } catch (IOException unreadable) {
                // Not an archive, which most of what Maven lists legitimately is
                // not. It simply does not answer the question.
                continue;
            }
        }
        return false;
    }

    /// Reads one class off the classpath by internal name, or null when no
    /// entry defines it. For the types an annotated dependency class refers to
    /// -- an enum a transfer object holds, say -- which carry no annotation of
    /// their own and so are not part of a [#scan].
    public static AnnotatedClass read(List<String> compileClasspath, String internalName)
            throws ProcessingException {
        if (compileClasspath == null || internalName == null || skipped(internalName + ".class")) {
            return null;
        }
        String entryName = internalName + ".class";
        for (String element : compileClasspath) {
            File file = new File(element);
            if (file.isDirectory()) {
                File candidate = new File(file, entryName.replace('/', File.separatorChar));
                if (candidate.isFile()) {
                    return ClassScanner.readClass(candidate);
                }
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            ZipFile zip;
            try {
                zip = new ZipFile(file);
            } catch (IOException notAnArchive) {
                continue;
            }
            try {
                ZipEntry entry = zip.getEntry(entryName);
                if (entry == null) {
                    continue;
                }
                InputStream in = zip.getInputStream(entry);
                try {
                    return ClassScanner.readClass(new ByteArrayInputStream(readAll(in)), file);
                } finally {
                    in.close();
                }
            } catch (IOException unreadable) {
                throw new ProcessingException("Could not read " + entryName + " from " + file
                        + ": " + unreadable.getMessage(), unreadable);
            } finally {
                try {
                    zip.close();
                } catch (IOException ignored) {
                    // Read-only; nothing a failed close can lose.
                }
            }
        }
        return null;
    }

    private static void scanDirectory(File root, File dir, List<byte[]> needles,
            Map<String, AnnotatedClass> out) throws ProcessingException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        java.util.Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                scanDirectory(root, child, needles, out);
                continue;
            }
            String entryName = root.toPath().relativize(child.toPath()).toString()
                    .replace(File.separatorChar, '/');
            if (!entryName.endsWith(".class") || skipped(entryName)) {
                continue;
            }
            byte[] bytes;
            try {
                InputStream in = new FileInputStream(child);
                try {
                    bytes = readAll(in);
                } finally {
                    in.close();
                }
            } catch (IOException unreadable) {
                throw new ProcessingException("Could not read " + child + ": "
                        + unreadable.getMessage(), unreadable);
            }
            consider(bytes, child, needles, out);
        }
    }

    private static void scanArchive(File archive, List<byte[]> needles,
            Map<String, AnnotatedClass> out) throws ProcessingException {
        ZipFile zip;
        try {
            zip = new ZipFile(archive);
        } catch (IOException notAnArchive) {
            // A pom-typed or otherwise non-archive dependency holds no classes.
            return;
        }
        try {
            if (zip.getEntry(CORE_MARKER) != null || zip.getEntry(BACKEND_MARKER) != null) {
                return;
            }
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class") || skipped(name)) {
                    continue;
                }
                byte[] bytes;
                InputStream in = zip.getInputStream(entry);
                try {
                    bytes = readAll(in);
                } finally {
                    in.close();
                }
                consider(bytes, archive, needles, out);
            }
        } catch (IOException unreadable) {
            throw new ProcessingException("Could not read " + archive + ": "
                    + unreadable.getMessage(), unreadable);
        } finally {
            try {
                zip.close();
            } catch (IOException ignored) {
                // Read-only; nothing a failed close can lose.
            }
        }
    }

    private static void consider(byte[] bytes, File source, List<byte[]> needles,
            Map<String, AnnotatedClass> out) throws ProcessingException {
        boolean wanted = false;
        for (byte[] needle : needles) {
            if (indexOf(bytes, needle) >= 0) {
                wanted = true;
                break;
            }
        }
        if (!wanted) {
            return;
        }
        AnnotatedClass cls = ClassScanner.readClass(new ByteArrayInputStream(bytes), source);
        if (cls != null && !out.containsKey(cls.getInternalName())) {
            out.put(cls.getInternalName(), cls);
        }
    }

    private static boolean skipped(String entryName) {
        return entryName.startsWith("java/") || entryName.startsWith("javax/")
                || entryName.startsWith("kotlin/") || entryName.startsWith("META-INF/")
                || entryName.endsWith("module-info.class");
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) text.charAt(i);
        }
        return out;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        int last = haystack.length - needle.length;
        outer:
        for (int i = 0; i <= last; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static File canonical(File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalFile();
        } catch (IOException unreadable) {
            return file.getAbsoluteFile();
        }
    }
}
