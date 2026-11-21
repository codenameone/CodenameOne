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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Adds Hunspell dictionaries to an application that uses
 * {@code com.codename1.spellcheck.SpellChecker}, for every target: it runs in
 * {@link Executor#buildNoException} on the source jar, before any builder
 * unpacks it, so each target packages the files like any other resource.
 *
 * <p>Detection is a class reference, which is why spell checking is a class
 * of its own: an application that never names {@code SpellChecker} -- one
 * that only shows a {@code DocumentView} -- gets no dictionary and stays
 * small. The languages are the {@code spellcheck.languages} build hint, a
 * comma separated list ({@code en_US} by default); {@code
 * spellcheck.dictionaries=false} turns the step off for an application that
 * brings its own dictionaries.</p>
 *
 * <p>The dictionaries come from the {@code codenameone-spellcheck-dictionaries}
 * artifact, which the build tools resolve with the ports
 * ({@code AppBuilder.SPELLCHECK_DICTIONARIES}). They are written at the root of
 * the jar as {@code cn1dict_<lang>.aff} and {@code .dic}, with
 * {@code cn1dict_languages.txt} listing them, because the Apple ports flatten
 * resource folders. A file the application already carries under one of those
 * names is kept, never replaced.</p>
 */
final class SpellcheckDictionaries {
    static final String HINT_LANGUAGES = "spellcheck.languages";
    static final String HINT_ENABLED = "spellcheck.dictionaries";
    static final String DEFAULT_LANGUAGE = "en_US";
    private static final byte[] NEEDLE =
            "com/codename1/spellcheck/SpellChecker".getBytes(StandardCharsets.US_ASCII);
    private static final String OWN_PACKAGE = "com/codename1/spellcheck/";
    private static final String SOURCE_DIR = "/cn1dict/";
    private static final String PREFIX = "cn1dict_";
    private static final String INDEX = "cn1dict_languages.txt";

    private SpellcheckDictionaries() {
    }

    /**
     * The source jar to build: the given one, or a copy with the dictionaries
     * added when the application uses the spell checker.
     */
    static File inject(Executor exec, File jar, BuildRequest request)
            throws IOException, BuildException {
        if ("false".equalsIgnoreCase(request.getArg(HINT_ENABLED, "true").trim())
                || jar == null || !jar.isFile() || !usesSpellChecker(jar)) {
            return jar;
        }
        List<String> languages = languages(request.getArg(HINT_LANGUAGES, DEFAULT_LANGUAGE));
        Set<String> available = available(exec);
        for (String lang : languages) {
            if (!available.contains(lang)) {
                throw new BuildException("The " + HINT_LANGUAGES + " build hint names " + lang
                        + ", which has no bundled dictionary. Available: " + available
                        + ". Set " + HINT_ENABLED + "=false and supply your own Hunspell"
                        + " dictionary to SpellChecker.create for another language.");
            }
        }
        File out = File.createTempFile("cn1-spellcheck", ".jar", jar.getParentFile());
        Set<String> names = new HashSet<String>();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar));
             ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
            byte[] buf = new byte[65536];
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                if (!names.add(e.getName())) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(e.getName()));
                for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                    zip.write(buf, 0, n);
                }
                zip.closeEntry();
            }
            StringBuilder index = new StringBuilder();
            for (String lang : languages) {
                for (String ext : new String[] {".aff", ".dic"}) {
                    String name = PREFIX + lang + ext;
                    if (names.add(name)) {
                        zip.putNextEntry(new ZipEntry(name));
                        zip.write(read(exec, SOURCE_DIR + lang + ext));
                        zip.closeEntry();
                    }
                }
                index.append(lang).append('\n');
            }
            if (names.add(INDEX)) {
                zip.putNextEntry(new ZipEntry(INDEX));
                zip.write(index.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException | BuildException | RuntimeException err) {
            if (!out.delete()) {
                out.deleteOnExit();
            }
            throw err;
        }
        exec.debug("Added spell checking dictionaries " + languages);
        return out;
    }

    /** The languages of a hint value: comma separated, {@code en-US} read as {@code en_US}. */
    static List<String> languages(String hint) {
        Set<String> out = new LinkedHashSet<String>();
        for (String s : hint.split(",")) {
            String t = s.trim().replace('-', '_');
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            out.add(DEFAULT_LANGUAGE);
        }
        return new ArrayList<String>(out);
    }

    private static Set<String> available(Executor exec) throws IOException, BuildException {
        byte[] index = read(exec, SOURCE_DIR + "languages.txt");
        Set<String> out = new LinkedHashSet<String>();
        for (String line : new String(index, StandardCharsets.UTF_8).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && t.charAt(0) != '#') {
                out.add(t);
            }
        }
        return out;
    }

    private static byte[] read(Executor exec, String resource) throws IOException, BuildException {
        InputStream in = exec.getResourceAsStream(resource);
        if (in == null) {
            throw new BuildException("The application uses SpellChecker but the "
                    + "codenameone-spellcheck-dictionaries artifact, which holds " + resource
                    + ", was not resolved for this build.");
        }
        try (InputStream s = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            for (int n = s.read(buf); n >= 0; n = s.read(buf)) {
                b.write(buf, 0, n);
            }
            return b.toByteArray();
        }
    }

    /**
     * Does a class of the jar, other than the spell checker's own, name
     * {@code SpellChecker}? The class name sits in the constant pool as plain
     * modified UTF-8, which for an ASCII name is the name's bytes.
     */
    static boolean usesSpellChecker(File jar) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar))) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith(OWN_PACKAGE)) {
                    continue;
                }
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                for (int r = in.read(buf); r >= 0; r = in.read(buf)) {
                    b.write(buf, 0, r);
                }
                if (contains(b.toByteArray())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNameByte(byte b) {
        return b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9' || b == '_';
    }

    private static boolean contains(byte[] data) {
        outer:
        for (int i = 0; i + NEEDLE.length <= data.length; i++) {
            for (int j = 0; j < NEEDLE.length; j++) {
                if (data[i + j] != NEEDLE[j]) {
                    continue outer;
                }
            }
            int after = i + NEEDLE.length;
            // not a longer name such as SpellCheckerFactory; a '$' (an inner class) and
            // the ';' of a descriptor both still mean SpellChecker is used
            if (after == data.length || !isNameByte(data[after])) {
                return true;
            }
        }
        return false;
    }
}
