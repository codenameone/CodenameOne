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
        // The class's REFERENCES, not every byte in it. A raw text search
        // matched a package name that merely appeared in a string constant --
        // a feature registry listing package names, a log message, a piece of
        // documentation -- and reported it as API usage. That is not a
        // cosmetic over-report on iOS: com/codename1/call/directory/ turns on
        // the Call Directory extension, and a signed build then aborts unless
        // a separate extension provisioning profile is supplied. A library
        // that only NAMES the package in a string broke the build of every
        // app that included it.
        //
        // Class names and descriptors are the two places a real reference
        // appears, and both are reachable from the constant pool without
        // asking what bytecode version this is.
        Set<String> references = classReferences(bytes);
        if (references == null) {
            // Unparseable. The raw scan is what this did before, and keeping
            // it here means a class shape this parser does not understand
            // still contributes rather than silently disabling a feature the
            // app really uses.
            String text;
            try {
                // ISO-8859-1 so every byte maps to a char and nothing is
                // lost; the prefixes are ASCII, so a match is a match.
                text = new String(bytes, "ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException never) {
                return;
            }
            for (int i = 0; i < prefixes.length; i++) {
                if (!found.contains(prefixes[i])
                        && text.indexOf(prefixes[i]) >= 0) {
                    found.add(prefixes[i]);
                }
            }
            return;
        }
        for (int i = 0; i < prefixes.length; i++) {
            if (found.contains(prefixes[i])) {
                continue;
            }
            for (String reference : references) {
                if (reference.indexOf(prefixes[i]) >= 0) {
                    found.add(prefixes[i]);
                    break;
                }
            }
        }
    }

    /// Every class name and type descriptor a class file refers to, or null
    /// when the bytes are not a class file this can read.
    ///
    /// Deliberately NOT through a bytecode library. The daemon's ASM is
    /// pinned to a version that refuses class files newer than it knows, and
    /// refusing to read a modern library is the same failure as misreading an
    /// old one. The constant pool's shape has not changed: only new TAGS have
    /// been added, and those are skipped by size.
    ///
    /// Utf8 entries reached from a CONSTANT_Class, a CONSTANT_NameAndType
    /// descriptor or a CONSTANT_MethodType are references. A Utf8 reached
    /// from a CONSTANT_String is a literal the code happens to carry, and
    /// that is exactly the difference this method exists to draw.
    static Set<String> classReferences(byte[] bytes) {
        java.io.DataInputStream in = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(bytes));
        try {
            if (in.readInt() != 0xCAFEBABE) {
                return null;
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            String[] utf8 = new String[count];
            int[] referenced = new int[count];
            int referencedCount = 0;
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:
                        utf8[i] = in.readUTF();
                        break;
                    case 7:
                    case 16:
                        referenced[referencedCount++] =
                                in.readUnsignedShort();
                        break;
                    case 12:
                        in.readUnsignedShort();
                        referenced[referencedCount++] =
                                in.readUnsignedShort();
                        break;
                    case 8:
                    case 19:
                    case 20:
                        in.readUnsignedShort();
                        break;
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 11:
                    case 17:
                    case 18:
                        in.readInt();
                        break;
                    case 5:
                    case 6:
                        in.readLong();
                        // A long or double takes TWO pool slots, and the
                        // second one is unusable. Miscounting here shifts
                        // every later index and turns the rest of the pool
                        // into noise.
                        i++;
                        break;
                    case 15:
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        break;
                    default:
                        // A tag this does not know means the rest cannot be
                        // walked, so say so rather than returning a partial
                        // answer that reads as "no references".
                        return null;
                }
            }
            Set<String> out = new HashSet<String>();
            for (int i = 0; i < referencedCount; i++) {
                int index = referenced[i];
                if (index > 0 && index < count && utf8[index] != null) {
                    out.add(utf8[index]);
                }
            }
            // The MEMBERS too, which the pool alone does not reach. A field
            // or method descriptor is referenced by field_info and
            // method_info directly, so a library that names a type only in a
            // signature it never calls -- an interface method returning
            // com.codename1.call.session.Call, an abstract declaration, a
            // native one -- carries that descriptor as a plain Utf8 that no
            // CONSTANT_Class, NameAndType or MethodType points at. It was
            // therefore invisible here, and the app built against that
            // library got no permissions, no services and no defines for a
            // package it plainly uses.
            //
            // A descriptor is the most direct kind of reference there is,
            // which is what makes this the right side of the line this
            // method draws: a CONSTANT_String is a literal the code happens
            // to carry, and a descriptor is the type system.
            in.readUnsignedShort();
            in.readUnsignedShort();
            in.readUnsignedShort();
            int interfaces = in.readUnsignedShort();
            for (int i = 0; i < interfaces; i++) {
                in.readUnsignedShort();
            }
            for (int members = 0; members < 2; members++) {
                int howMany = in.readUnsignedShort();
                for (int m = 0; m < howMany; m++) {
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                    int descriptor = in.readUnsignedShort();
                    if (descriptor > 0 && descriptor < count
                            && utf8[descriptor] != null) {
                        out.add(utf8[descriptor]);
                    }
                    // The SIGNATURE attribute too. javac erases a generic
                    // member -- List<com.codename1.call.session.Call>
                    // becomes ()Ljava/util/List; -- and puts the real type
                    // in Signature, so the descriptor collected above says
                    // nothing about it. Skipping every attribute therefore
                    // left a library whose only mention of a package is a
                    // generic declaration exactly as invisible as the
                    // descriptor case this walk was added for.
                    readAttributes(in, utf8, count, out);
                }
            }
            // And the CLASS's own attributes. A generic supertype lives
            // there and nowhere else: for
            // "class Calls extends ArrayList<...Call>" the super_class entry
            // names only ArrayList, and the argument is in the class-level
            // Signature. Stopping after the members left that case exactly as
            // invisible as the member-level one this walk was extended for.
            readAttributes(in, utf8, count, out);
            return out;
        } catch (Throwable unreadable) {
            return null;
        } finally {
            try {
                in.close();
            } catch (java.io.IOException ignored) {
                // Nothing useful to do about a failed close on a byte array.
            }
        }
    }

    /// Walks an `attributes` table, collecting a `Signature` and skipping
    /// the rest.
    ///
    /// Only the SHAPE is read -- a count, then a name index and a length per
    /// attribute -- so an attribute this has never heard of costs nothing.
    /// That is the same reason the constant pool is walked by hand rather
    /// than through a bytecode library. Signature is the one whose CONTENT
    /// matters: it carries the type a generic member erased away.
    private static void readAttributes(java.io.DataInputStream in,
            String[] utf8, int count, Set<String> out)
            throws java.io.IOException {
        int attributes = in.readUnsignedShort();
        for (int i = 0; i < attributes; i++) {
            int name = in.readUnsignedShort();
            long length = in.readInt() & 0xffffffffL;
            if (length == 2 && name > 0 && name < count
                    && "Signature".equals(utf8[name])) {
                int signature = in.readUnsignedShort();
                if (signature > 0 && signature < count
                        && utf8[signature] != null) {
                    out.add(utf8[signature]);
                }
                continue;
            }
            // ANNOTATIONS carry type references too, and only as pool
            // indices: "@Handler(com.codename1.call.session.Call.class)"
            // puts the descriptor in a class_info_index that nothing else
            // points at, so a library whose only mention of a package is an
            // annotation -- and which finds the annotated type reflectively
            // at runtime -- was invisible. Parsing succeeded, so the raw
            // text fallback never ran either.
            //
            // Read into memory rather than streamed, because the grammar
            // needs to look ahead and a malformed one must cost this
            // attribute rather than the class. Only these attributes; a Code
            // attribute is skipped as before.
            if (name > 0 && name < count && isAnnotationAttribute(utf8[name])
                    && length < 1 << 20) {
                byte[] body = new byte[(int) length];
                in.readFully(body);
                collectAnnotationTypes(body, utf8, count, out,
                        utf8[name].endsWith("ParameterAnnotations"),
                        "AnnotationDefault".equals(utf8[name]));
                continue;
            }
            while (length > 0) {
                long skipped = in.skip(length);
                if (skipped <= 0) {
                    // Nothing left to skip and the table says otherwise, so
                    // the file is not what it claims. The caller reads a
                    // throw here as "unreadable" and falls back to the text
                    // scan, which over-approximates rather than under.
                    throw new java.io.IOException("truncated attribute");
                }
                length -= skipped;
            }
        }
    }

    /// Whether an attribute name is one whose body is annotation data this
    /// can read.
    ///
    /// Deliberately NOT the type-annotation attributes: their target_info
    /// varies by target kind, and misreading it would desynchronise the
    /// walk. They are skipped whole, as anything else unknown is.
    private static boolean isAnnotationAttribute(String name) {
        return "RuntimeVisibleAnnotations".equals(name)
                || "RuntimeInvisibleAnnotations".equals(name)
                || "RuntimeVisibleParameterAnnotations".equals(name)
                || "RuntimeInvisibleParameterAnnotations".equals(name)
                || "AnnotationDefault".equals(name);
    }

    /// Collects every annotation type and class literal in an annotation
    /// attribute body.
    ///
    /// A malformed body costs this attribute and nothing else: the walk has
    /// already read past it, so a bad parse here cannot desynchronise the
    /// rest of the class.
    private static void collectAnnotationTypes(byte[] body, String[] utf8,
            int count, Set<String> out, boolean parameters,
            boolean defaultValue) {
        try {
            int[] at = new int[]{0};
            if (defaultValue) {
                readElementValue(body, at, utf8, count, out);
                return;
            }
            int groups = 1;
            if (parameters) {
                groups = body[at[0]++] & 0xff;
            }
            for (int g = 0; g < groups; g++) {
                int annotations = u2(body, at);
                for (int a = 0; a < annotations; a++) {
                    readAnnotation(body, at, utf8, count, out);
                }
            }
        } catch (RuntimeException malformed) { //NOPMD EmptyCatchBlock
            // Not this class's problem to report; the rest of the walk is
            // unaffected because the body was consumed by length.
        }
    }

    /// One `annotation` structure: its type, then its element pairs.
    private static void readAnnotation(byte[] body, int[] at, String[] utf8,
            int count, Set<String> out) {
        collect(u2(body, at), utf8, count, out);
        int pairs = u2(body, at);
        for (int i = 0; i < pairs; i++) {
            u2(body, at);
            readElementValue(body, at, utf8, count, out);
        }
    }

    /// One `element_value`, which is where a class literal lives.
    private static void readElementValue(byte[] body, int[] at, String[] utf8,
            int count, Set<String> out) {
        int tag = body[at[0]++] & 0xff;
        switch (tag) {
            case 'e':
                collect(u2(body, at), utf8, count, out);
                u2(body, at);
                break;
            case 'c':
                // The class literal itself.
                collect(u2(body, at), utf8, count, out);
                break;
            case '@':
                readAnnotation(body, at, utf8, count, out);
                break;
            case '[': {
                int values = u2(body, at);
                for (int i = 0; i < values; i++) {
                    readElementValue(body, at, utf8, count, out);
                }
                break;
            }
            default:
                // Every constant kind, including 's': a String element is a
                // literal, and the constant it names is not collected for
                // the same reason a CONSTANT_String is not.
                u2(body, at);
                break;
        }
    }

    /// Adds the Utf8 at `index`, when there is one.
    private static void collect(int index, String[] utf8, int count,
            Set<String> out) {
        if (index > 0 && index < count && utf8[index] != null) {
            out.add(utf8[index]);
        }
    }

    /// The next big-endian u2, advancing the cursor.
    private static int u2(byte[] body, int[] at) {
        int v = ((body[at[0]] & 0xff) << 8) | (body[at[0] + 1] & 0xff);
        at[0] += 2;
        return v;
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
