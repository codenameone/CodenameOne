/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The flutter-runtime plugin packs ({@code com.codename1.flutter.plugins.<pack>}), which
 * reach an application only through the transpiler.
 *
 * <p>The runtime jar carries each pack as Java SOURCE under {@link #ROOT}, never as
 * classes. A pack is copied into the transpiler's output when the generated code names
 * it, when the application's own Java does (the import-generated entry point installs
 * the firebase and app_links hooks), or when another copied pack does (share uses
 * crossfile). Everything else is simply never generated, so an app that does not import
 * geolocator contains no class naming {@code LocationManager}, and the builders -- which
 * derive permissions, manifest entries and native dependencies from the classes they
 * find -- have nothing to prune.</p>
 */
final class PluginPacks {
    /** Where a runtime jar (or class directory) keeps the pack sources. */
    static final String ROOT = "META-INF/dart/java/";
    static final String PACKAGE = "com.codename1.flutter.plugins.";
    static final String PATH = "com/codename1/flutter/plugins/";

    /** pack name -> (path under {@link #ROOT} -> source). */
    private final Map<String, Map<String, String>> packs = new TreeMap<String, Map<String, String>>();

    private PluginPacks() {
    }

    static PluginPacks load(List<File> stubClasspath) throws IOException {
        PluginPacks p = new PluginPacks();
        for (File entry : stubClasspath) {
            if (entry.isDirectory()) {
                File root = new File(entry, ROOT + PATH);
                List<File> files = new ArrayList<File>();
                collect(root, files);
                String base = new File(entry, ROOT).getPath() + File.separator;
                for (File f : files) {
                    String rel = f.getPath().substring(base.length()).replace(File.separatorChar, '/');
                    p.add(rel, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                }
            } else if (entry.isFile() && entry.getName().endsWith(".jar")) {
                ZipFile zip = new ZipFile(entry);
                try {
                    java.util.Enumeration<? extends ZipEntry> en = zip.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry ze = en.nextElement();
                        String n = ze.getName();
                        if (!ze.isDirectory() && n.startsWith(ROOT + PATH) && n.endsWith(".java")) {
                            p.add(n.substring(ROOT.length()), read(zip.getInputStream(ze)));
                        }
                    }
                } finally {
                    zip.close();
                }
            }
        }
        return p;
    }

    private void add(String rel, String source) {
        if (!rel.startsWith(PATH) || !rel.endsWith(".java")) {
            return;
        }
        int slash = rel.indexOf('/', PATH.length());
        if (slash < 0) {
            return;
        }
        String pack = rel.substring(PATH.length(), slash);
        Map<String, String> files = packs.get(pack);
        if (files == null) {
            files = new TreeMap<String, String>();
            packs.put(pack, files);
        }
        // First classpath entry wins, as it would for a class.
        if (!files.containsKey(rel)) {
            files.put(rel, source);
        }
    }

    /**
     * The packs {@code roots} name, plus every pack those name in turn. {@code roots}
     * is the generated code and the application's own Java.
     */
    Set<String> closure(Iterable<? extends CharSequence> roots) {
        Set<String> kept = new TreeSet<String>();
        List<String> todo = new ArrayList<String>();
        for (CharSequence text : roots) {
            for (String n : named(text)) {
                if (packs.containsKey(n) && kept.add(n)) {
                    todo.add(n);
                }
            }
        }
        while (!todo.isEmpty()) {
            String pack = todo.remove(todo.size() - 1);
            for (String src : packs.get(pack).values()) {
                for (String n : named(src)) {
                    if (packs.containsKey(n) && kept.add(n)) {
                        todo.add(n);
                    }
                }
            }
        }
        return kept;
    }

    /** The sources of {@code selected}, keyed by path relative to the output root. */
    Map<String, String> sources(Set<String> selected) {
        Map<String, String> out = new TreeMap<String, String>();
        for (String pack : selected) {
            out.putAll(packs.get(pack));
        }
        return out;
    }

    /**
     * Every pack name {@code text} spells as {@code com.codename1.flutter.plugins.<pack>},
     * qualified or in an import. A mention in a comment over-includes a pack, which costs
     * size, never a failed build.
     */
    static Set<String> named(CharSequence text) {
        Set<String> out = new TreeSet<String>();
        String s = text.toString();
        int at = s.indexOf(PACKAGE);
        while (at >= 0) {
            int start = at + PACKAGE.length();
            int end = start;
            while (end < s.length() && Character.isJavaIdentifierPart(s.charAt(end))) {
                end++;
            }
            if (end > start) {
                out.add(s.substring(start, end));
            }
            at = s.indexOf(PACKAGE, end);
        }
        return out;
    }

    /** The packs the Java sources under {@code roots} name. */
    static Set<String> namedByJava(List<File> roots) throws IOException {
        Set<String> out = new TreeSet<String>();
        for (File root : roots) {
            List<File> files = new ArrayList<File>();
            collect(root, files);
            for (File f : files) {
                out.addAll(named(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)));
            }
        }
        return out;
    }

    private static void collect(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        java.util.Arrays.sort(children);
        for (File c : children) {
            if (c.isDirectory()) {
                collect(c, out);
            } else if (c.getName().endsWith(".java")) {
                out.add(c);
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
