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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Writes the Java that stands in for a package's compiled modules.
///
/// A C extension is supported by replacing its compiled part with Java: the
/// package's Python is transpiled as it is, and each compiled module it
/// imports is a Java class that registers the same names through
/// `com.codename1.python.runtime.NativeModule`. Most compiled modules are
/// small accelerators over the platform's own facilities, so the class is
/// usually a few calls into Codename One.
///
/// This class finds the names the package's Python takes from each compiled
/// module -- `from ._speedups import escape`, `_speedups.escape(...)` -- and
/// writes a class with a stub for each, raising `NotImplementedError` until
/// it is filled in. A stub that exists is never overwritten. A module every
/// import of which sits in a `try` that falls back on `ImportError` needs
/// nothing at all: the package's pure-Python path runs instead.
final class PythonExtensionStubs {
    /// The file, beside the packages, that maps each compiled module to its
    /// Java class for the build.
    static final String INDEX = "cn1-native-modules.properties";
    static final String PACKAGE = "com.codename1.python.ext";

    // Possessive: the module part may be empty, so with greedy quantifiers the
    // two blank runs around it could split a long run of blanks every way
    // there is, which is quadratic on a hostile line.
    private static final Pattern FROM = Pattern.compile(
            "^([ \\t]*+)from[ \\t]++(\\.*+)([\\w.]*+)[ \\t]+import[ \\t]+\\(?([^#)\\n]*)", Pattern.MULTILINE);
    private static final Pattern IMPORT = Pattern.compile(
            "^([ \\t]*)import[ \\t]+([\\w.]+)(?:[ \\t]+as[ \\t]+(\\w+))?", Pattern.MULTILINE);

    private final Map<String, String> sources;

    /// `sources` maps each Python file's path to its text.
    PythonExtensionStubs(Map<String, String> sources) {
        this.sources = sources;
    }

    /// `pkg.sub._speedups` becomes `PkgSubSpeedups`.
    String className(String module) {
        return PACKAGE + "." + simpleName(module);
    }

    static String simpleName(String module) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < module.length(); i++) {
            char c = module.charAt(i);
            if (c == '.' || c == '_') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0 || !Character.isJavaIdentifierStart(sb.charAt(0))) {
            sb.insert(0, 'M');
        }
        return sb.toString();
    }

    /// Whether every import of `module` falls back when it is missing.
    boolean isOptional(String module) {
        boolean any = false;
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String pkg = packageOf(e.getKey());
            String text = e.getValue();
            for (int[] at : imports(text, pkg, module, null)) {
                any = true;
                if (!guarded(text, at[0], at[1])) {
                    return false;
                }
            }
        }
        return any;
    }

    /// The names the package takes from `module`.
    TreeSet<String> usedNames(String module) {
        TreeSet<String> names = new TreeSet<String>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            imports(e.getValue(), packageOf(e.getKey()), module, names);
        }
        names.remove("*");
        return names;
    }

    /// Writes the stub for `module` under `javaDir`; false when one exists.
    boolean write(String module, String distribution, File javaDir) throws IOException {
        String simple = simpleName(module);
        File f = new File(javaDir, PACKAGE.replace('.', '/') + "/" + simple + ".java");
        if (f.exists()) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PACKAGE).append(";\n\n");
        sb.append("import com.codename1.python.runtime.NativeModule;\n\n");
        sb.append("/// `").append(module).append("`, which ").append(distribution)
                .append(" ships compiled, written in Java.\n");
        sb.append("///\n");
        sb.append("/// Generated with a stub for each name the package's Python takes from the\n");
        sb.append("/// module. Replace each stub's body with Java -- usually a call into Codename\n");
        sb.append("/// One -- over plain Java values: a `Number` for an int or float, `String`,\n");
        sb.append("/// `byte[]`, `java.util.List`, `java.util.Map`, `null` for `None`. This file\n");
        sb.append("/// is yours: importing the project again does not overwrite it.\n");
        sb.append("public final class ").append(simple).append(" {\n");
        sb.append("    private ").append(simple).append("() {\n    }\n\n");
        sb.append("    /// Registers the module; the generated program calls this at start.\n");
        sb.append("    public static void install() {\n");
        sb.append("        new NativeModule(\"").append(module).append("\")\n");
        for (String n : usedNames(module)) {
            sb.append("                .function(\"").append(n).append("\", args -> {\n");
            sb.append("                    throw NativeModule.notImplemented(\"").append(module).append('.')
                    .append(n).append("\");\n");
            sb.append("                })\n");
        }
        sb.append("                .install();\n");
        sb.append("    }\n}\n");
        Files.createDirectories(f.getParentFile().toPath());
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return true;
    }

    static void writeIndex(File file, Map<String, String> modules) throws IOException {
        if (modules.isEmpty()) {
            if (file.isFile()) {
                Files.delete(file.toPath());
            }
            return;
        }
        StringBuilder sb = new StringBuilder("# Compiled Python modules and the Java classes that replace them\n");
        for (Map.Entry<String, String> e : new TreeMap<String, String>(modules).entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /// Reads [#INDEX] back.
    static Map<String, String> readIndex(File file) throws IOException {
        Map<String, String> out = new TreeMap<String, String>();
        if (!file.isFile()) {
            return out;
        }
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            int eq = line.indexOf('=');
            if (!line.startsWith("#") && eq > 0) {
                out.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return out;
    }

    private static String packageOf(String path) {
        String p = path.endsWith("/__init__.py") ? path.substring(0, path.length() - 12)
                : path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
        return p.replace('/', '.');
    }

    /// Resolves `from .x import` relative to the importing file's package.
    private static String absolute(String pkg, String dots, String rest) {
        if (dots.isEmpty()) {
            return rest;
        }
        String base = pkg;
        for (int i = 1; i < dots.length(); i++) {
            int d = base.lastIndexOf('.');
            base = d < 0 ? "" : base.substring(0, d);
        }
        return rest.isEmpty() ? base : base.isEmpty() ? rest : base + "." + rest;
    }

    /// The start offset and indent of every import of `module` in `text`,
    /// adding the names taken from it to `names` when that is given.
    static java.util.List<int[]> imports(String text, String pkg, String module, TreeSet<String> names) {
        java.util.List<int[]> at = new java.util.ArrayList<int[]>();
        String leaf = module.substring(module.lastIndexOf('.') + 1);
        Matcher m = FROM.matcher(text);
        while (m.find()) {
            String from = absolute(pkg, m.group(2), m.group(3));
            if (from.equals(module)) {
                at.add(new int[] {m.start(), m.group(1).length()});
                if (names != null) {
                    for (String part : m.group(4).split(",")) {
                        String n = part.trim().split("\\s+")[0];
                        if (!n.isEmpty()) {
                            names.add(n);
                        }
                    }
                }
            } else if (module.equals(from.isEmpty() ? leaf : from + "." + leaf)) {
                // from pkg import _speedups [as alias]
                for (String part : m.group(4).split(",")) {
                    String[] w = part.trim().split("\\s+");
                    if (w[0].equals(leaf)) {
                        at.add(new int[] {m.start(), m.group(1).length()});
                        if (names != null) {
                            attributes(text, w.length == 3 ? w[2] : leaf, names);
                        }
                    }
                }
            }
        }
        m = IMPORT.matcher(text);
        while (m.find()) {
            if (m.group(2).equals(module)) {
                at.add(new int[] {m.start(), m.group(1).length()});
                if (names != null) {
                    attributes(text, m.group(3) != null ? m.group(3) : module, names);
                }
            }
        }
        return at;
    }

    private static void attributes(String text, String alias, TreeSet<String> names) {
        Matcher m = Pattern.compile("(?<![\\w.])" + Pattern.quote(alias) + "\\.(\\w+)").matcher(text);
        while (m.find()) {
            names.add(m.group(1));
        }
    }

    /// Whether the line at `offset` sits directly in a `try:` whose handlers
    /// include one for `ImportError` (or a broader one).
    private static boolean guarded(String text, int offset, int indent) {
        String[] before = text.substring(0, offset).split("\n", -1);
        int tryIndent = -1;
        int tryLine = -1;
        for (int i = before.length - 2; i >= 0; i--) {
            String l = before[i];
            if (l.trim().isEmpty() || l.trim().startsWith("#")) {
                continue;
            }
            int ind = indentOf(l);
            if (ind < indent) {
                if (l.trim().startsWith("try:")) {
                    tryIndent = ind;
                    tryLine = i;
                }
                break;
            }
        }
        if (tryLine < 0) {
            return false;
        }
        String[] after = text.substring(offset).split("\n");
        for (String l : after) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("#") || indentOf(l) > tryIndent) {
                continue;
            }
            if (!t.startsWith("except")) {
                return false;
            }
            if (t.startsWith("except:") || t.contains("ImportError") || t.contains("ModuleNotFoundError")
                    || t.matches("except\\s*\\(?\\s*(Base)?Exception\\b.*")) {
                return true;
            }
        }
        return false;
    }

    private static int indentOf(String l) {
        int i = 0;
        while (i < l.length() && (l.charAt(i) == ' ' || l.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }
}
