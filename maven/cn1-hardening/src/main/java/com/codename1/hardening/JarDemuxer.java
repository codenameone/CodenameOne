/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Splits the merged application jar into a class-only jar (the only thing ProGuard
 * and the ASM transforms ever see) and an ordered set of every non-class entry.
 * The non-class entries -- {@code .aar}, {@code .a}, {@code .res} theme files,
 * tarred native bundles, resources -- are carried across byte-for-byte and
 * re-emitted into the hardened jar. Running ProGuard over the whole jar would
 * recompress and mangle those, which is why the split exists.
 */
public final class JarDemuxer {

    /** The non-class entries of an input jar, kept in their original order and bytes. */
    public static final class NonClassEntries {
        private final Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();

        void put(String name, byte[] data) {
            entries.put(name, data);
        }

        public int size() {
            return entries.size();
        }

        public Map<String, byte[]> asMap() {
            return entries;
        }
    }

    private JarDemuxer() {
    }

    /**
     * Reads {@code input}, writes every {@code .class} entry into {@code classesJarOut}, and
     * returns the remaining entries. Directory entries are dropped (the rebuild recreates the
     * container).
     *
     * @return the non-class entries plus, via {@link #classCount}, how many classes were split
     */
    public static NonClassEntries split(File input, File classesJarOut) throws IOException {
        NonClassEntries nonClass = new NonClassEntries();
        FileInputStream fi = new FileInputStream(input);
        try {
            ZipInputStream zis = new ZipInputStream(fi);
            FileOutputStream fo = new FileOutputStream(classesJarOut);
            try {
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fo));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    byte[] data = readAll(zis);
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        ZipEntry out = new ZipEntry(name);
                        zos.putNextEntry(out);
                        zos.write(data);
                        zos.closeEntry();
                    } else if (isJarSignature(name)) {
                        // Renaming/transforming classes invalidates any bundled jar signature, so
                        // carrying the .SF/.RSA/.DSA/.EC blocks across would make a verifying JarFile
                        // throw SecurityException: Invalid signature file digest. Drop them; without
                        // the .SF the JVM no longer verifies, which is correct for a rewritten jar.
                        continue;
                    } else {
                        // Every non-class entry -- resources, native bundles, AND any .java/.kt/.swift/
                        // .m/.h/.cs source a CN1Lib bundles -- is carried byte-for-byte. Carrying a
                        // .java/.kt source unchanged while the engine renames classes is SAFE and cannot
                        // produce a "cannot find symbol" against the renamed jar, because no builder ever
                        // javac-compiles an unzip'd source against an engine-renamed classpath:
                        //   - Android: the engine does NOT rename (AndroidGradleBuilder.hardeningRename-
                        //     Supported()==false -> renameEnabled=false). R8 is the sole renamer and runs
                        //     AFTER javac has compiled the app classes together with any bundled native
                        //     sources, so R8 renames the whole set consistently -- imports still resolve.
                        //   - iOS/mac/watch/tv: unzip routes these sources into the RESOURCE tree
                        //     (IPhoneBuilder passes resDir as unzip's sourceDir); the only javac compiles
                        //     the generated stub dir, never the carried sources. Native impls are Obj-C.
                        //   - win/linux: javac compiles only the ParparVM translator-GENERATED .java,
                        //     emitted from already-renamed bytecode, so it is internally consistent.
                        //   - javascript: javac targets the JS port's own sources against staged classes;
                        //     bundled app native sources are .js, and string encryption is off on JS.
                        //   - javase/desktop: runs the bytecode directly and recompiles no sources.
                        // So there is no rename-vs-source mismatch to guard; a source-text keep scanner
                        // would be dead code. Revisit only if a builder starts compiling unzip's sourceDir
                        // against an engine-renamed classpath.
                        //
                        // A resource entry is copied under its ORIGINAL path, so a PACKAGE-RELATIVE
                        // Class.getResourceAsStream("x.properties") from an engine-renamed class -- which the
                        // runtime resolves under the class's now-obfuscated package -- would miss it. This is
                        // a DELIBERATE design choice, not an oversight: Codename One's own resource model
                        // uses absolute paths (Display.getResourceAsStream("/x"), the theme .res), which are
                        // unaffected by package renaming, so package names are obfuscated on purpose (see the
                        // BuiltinKeepRules packageNamesAreNotKept test). The only exposure is a bundled
                        // third-party dependency that loads a package-relative resource; adapting resource
                        // paths by the class mapping cannot run here (the mapping does not exist until after
                        // ProGuard) and would forfeit package obfuscation. Such a dependency uses
                        // harden.keep to keep its package-relative-resource classes (which keeps their
                        // package path); this limitation is documented in App-Hardening.asciidoc.
                        nonClass.put(name, data);
                    }
                }
                zos.finish();
                zos.flush();
            } finally {
                fo.close();
            }
        } finally {
            fi.close();
        }
        return nonClass;
    }

    /**
     * Writes {@code outJar} from the transformed classes (keyed by internal name, e.g.
     * {@code a/b/C}) plus the preserved non-class entries, each copied byte-for-byte.
     */
    public static void rebuild(File outJar, Map<String, byte[]> classesByInternalName,
                               NonClassEntries nonClass) throws IOException {
        FileOutputStream fo = new FileOutputStream(outJar);
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fo));
            for (Map.Entry<String, byte[]> e : classesByInternalName.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey() + ".class");
                zos.putNextEntry(entry);
                zos.write(e.getValue());
                zos.closeEntry();
            }
            for (Map.Entry<String, byte[]> e : nonClass.asMap().entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                zos.putNextEntry(entry);
                zos.write(e.getValue());
                zos.closeEntry();
            }
            zos.finish();
            zos.flush();
        } finally {
            fo.close();
        }
    }

    /** Reads every {@code .class} entry of a jar into a map keyed by internal name. */
    public static Map<String, byte[]> readClasses(File jar) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
        FileInputStream fi = new FileInputStream(jar);
        try {
            ZipInputStream zis = new ZipInputStream(fi);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] data = readAll(zis);
                String internal = entry.getName().substring(0, entry.getName().length() - ".class".length());
                classes.put(internal, data);
            }
        } finally {
            fi.close();
        }
        return classes;
    }

    /** True for a jar signature block under META-INF that a class rewrite invalidates. */
    static boolean isJarSignature(String name) {
        String upper = name.toUpperCase();
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        // Only the signature blocks in META-INF's top level, not nested paths.
        if (upper.indexOf('/', "META-INF/".length()) >= 0) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC");
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(Math.max(1024, in.available()));
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) >= 0) {
            bout.write(buf, 0, r);
        }
        return bout.toByteArray();
    }
}
