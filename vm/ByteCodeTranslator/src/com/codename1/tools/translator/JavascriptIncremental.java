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
package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates a handful of classes to JavaScript that links against a host bundle
 * already running (an open-world bundle, see {@link JavascriptOpenWorld}): the
 * Playground compiles user code in the browser and loads the result into its own VM.
 *
 * <p>Only the user classes are generated. Host classes are read (signatures are
 * enough, so stub class files without code work) for what the generator needs to
 * know about them: the hierarchy, where fields are declared, which classes have a
 * static initializer. What it cannot know -- whether a host function is a plain
 * function or a generator -- is settled at run time: every user method is emitted
 * as a generator, and calls into the host go through {@code _G_<function>}
 * adapters (declared in the chunk's preamble) that drive a generator result and
 * pass a plain one through. Virtual calls use the generator-aware {@code _v*}
 * dispatchers, which already accept either kind of target.
 *
 * <p>The output is self-contained JavaScript: evaluate it in the host's global
 * scope (the worker's, via a blob {@code importScripts}) and the classes are
 * registered, supertypes first.
 */
public final class JavascriptIncremental {
    /** Supplies host class files by internal name ({@code java/lang/String}), or null when absent. */
    public interface ClassSource {
        byte[] classBytes(String internalName);
    }

    private static boolean active;
    private static final Set<String> external = new HashSet<String>();
    private static final Set<String> adapters = new LinkedHashSet<String>();

    private JavascriptIncremental() {
    }

    static boolean isActive() {
        return active;
    }

    /** Is this (sanitized) class part of the host rather than the code being translated? */
    static boolean isExternal(String sanitizedClass) {
        return active && sanitizedClass != null && external.contains(sanitizedClass);
    }

    /** The adapter alias a call to host function {@code fn} goes through; recorded for the preamble. */
    static String adapterFor(String fn) {
        adapters.add(fn);
        return "_G_" + fn;
    }

    /**
     * Translates {@code userClasses} (internal name to class-file bytes) against the
     * host. Returns the JavaScript; throws on a class the host does not have.
     */
    public static synchronized String translate(Map<String, byte[]> userClasses, ClassSource host) throws Exception {
        ByteCodeTranslator.OutputType savedOutput = ByteCodeTranslator.output;
        Parser.cleanup();
        external.clear();
        adapters.clear();
        active = true;
        ByteCodeTranslator.output = ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT;
        try {
            List<ByteCodeClass> all = Parser.parsedClasses();
            for (byte[] bytes : userClasses.values()) {
                Parser.parseBytes(bytes);
            }
            List<ByteCodeClass> user = new ArrayList<ByteCodeClass>(all);
            Set<String> loaded = new HashSet<String>();
            for (ByteCodeClass c : user) {
                loaded.add(c.getClsName());
            }
            // The host classes the user classes mention (their constant pools), and the
            // supertypes of those, transitively.
            List<String> work = new ArrayList<String>();
            for (byte[] bytes : userClasses.values()) {
                collectClassReferences(bytes, work);
            }
            while (!work.isEmpty()) {
                String name = work.remove(work.size() - 1);
                String sanitized = name.replace('/', '_').replace('$', '_');
                if (loaded.contains(sanitized)) {
                    continue;
                }
                loaded.add(sanitized);
                byte[] bytes = host.classBytes(name);
                if (bytes == null) {
                    continue;
                }
                int before = all.size();
                ByteCodeClass hc = Parser.parseBytes(bytes);
                for (int i = before; i < all.size(); i++) {
                    external.add(all.get(i).getClsName());
                }
                if (hc.getBaseClass() != null) {
                    work.add(hc.getBaseClass());
                }
                for (String i : hc.getBaseInterfaces()) {
                    work.add(i);
                }
            }
            Parser.linkHierarchy();
            for (ByteCodeClass c : user) {
                for (BytecodeMethod m : c.getMethods()) {
                    m.setJavascriptSuspending(true);
                }
            }
            JavascriptReachability.resetExportedFacts();
            JavascriptSuspensionAnalysis.exportedDispatchModel = null;
            JavascriptSuspensionAnalysis.exportedSuspendingSigs = null;
            JavascriptMethodGenerator.setClassIndex(all);
            StringBuilder body = new StringBuilder();
            for (ByteCodeClass c : supertypesFirst(user)) {
                body.append(c.generateJavascriptCode(all)).append('\n');
            }
            StringBuilder out = new StringBuilder();
            out.append("// incremental translation: ").append(user.size()).append(" classes\n");
            for (String fn : adapters) {
                out.append("var _G_").append(fn).append(" = _GW(\"").append(fn).append("\");\n");
            }
            out.append(body);
            return out.toString();
        } finally {
            active = false;
            external.clear();
            JavascriptMethodGenerator.setClassIndex(null);
            Parser.cleanup();
            ByteCodeTranslator.output = savedOutput;
        }
    }

    /**
     * Every class a class file names: CONSTANT_Class entries (array element classes
     * included) and the classes inside field/method descriptors. Read straight from
     * the constant pool so the names keep their '/' and '$' (the translator's
     * sanitized names cannot be turned back into class-file names reliably).
     */
    static void collectClassReferences(byte[] b, List<String> out) {
        int count = u2(b, 8);
        String[] utf8 = new String[count];
        int[] classIndex = new int[count];
        int p = 10;
        for (int i = 1; i < count; i++) {
            int tag = b[p] & 0xFF;
            switch (tag) {
                case 1: {
                    int len = u2(b, p + 1);
                    utf8[i] = decodeUtf8(b, p + 3, len);
                    p += 3 + len;
                    break;
                }
                case 7:
                    classIndex[i] = u2(b, p + 1);
                    p += 3;
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    p += 5;
                    break;
                case 5: case 6:
                    p += 9;
                    i++;
                    break;
                case 8: case 16: case 19: case 20:
                    p += 3;
                    break;
                case 15:
                    p += 4;
                    break;
                default:
                    throw new IllegalArgumentException("bad constant pool tag " + tag);
            }
        }
        for (int i = 1; i < count; i++) {
            if (classIndex[i] != 0 && utf8[classIndex[i]] != null) {
                addDescriptorClasses(utf8[classIndex[i]].startsWith("[") ? utf8[classIndex[i]] : "L" + utf8[classIndex[i]] + ";", out);
            } else if (utf8[i] != null && (utf8[i].startsWith("(") || utf8[i].startsWith("L") && utf8[i].endsWith(";")
                    || utf8[i].startsWith("["))) {
                addDescriptorClasses(utf8[i], out);
            }
        }
    }

    private static void addDescriptorClasses(String d, List<String> out) {
        int i = 0;
        while (i < d.length()) {
            char c = d.charAt(i);
            if (c == 'L') {
                int end = d.indexOf(';', i);
                if (end < 0) {
                    return;
                }
                String n = d.substring(i + 1, end);
                if (n.indexOf('<') < 0 && n.indexOf('.') < 0 && n.length() > 0) {
                    out.add(n);
                }
                i = end + 1;
            } else {
                i++;
            }
        }
    }

    private static int u2(byte[] b, int p) {
        return (b[p] & 0xFF) << 8 | b[p + 1] & 0xFF;
    }

    private static String decodeUtf8(byte[] b, int p, int len) {
        StringBuilder s = new StringBuilder(len);
        int end = p + len;
        while (p < end) {
            int c = b[p++] & 0xFF;
            if (c < 0x80) {
                s.append((char) c);
            } else if ((c & 0xE0) == 0xC0 && p < end) {
                s.append((char) ((c & 0x1F) << 6 | b[p++] & 0x3F));
            } else if (p + 1 < end) {
                s.append((char) ((c & 0x0F) << 12 | (b[p] & 0x3F) << 6 | b[p + 1] & 0x3F));
                p += 2;
            } else {
                p++;
            }
        }
        return s.toString();
    }

    private static List<ByteCodeClass> supertypesFirst(List<ByteCodeClass> user) {
        Map<String, ByteCodeClass> byName = new LinkedHashMap<String, ByteCodeClass>();
        for (ByteCodeClass c : user) {
            byName.put(c.getClsName(), c);
        }
        List<ByteCodeClass> out = new ArrayList<ByteCodeClass>();
        Set<String> done = new HashSet<String>();
        for (ByteCodeClass c : user) {
            visit(c, byName, done, out);
        }
        return out;
    }

    private static void visit(ByteCodeClass c, Map<String, ByteCodeClass> byName, Set<String> done, List<ByteCodeClass> out) {
        if (!done.add(c.getClsName())) {
            return;
        }
        if (c.getBaseClass() != null) {
            ByteCodeClass b = byName.get(c.getBaseClass().replace('/', '_').replace('$', '_'));
            if (b != null) {
                visit(b, byName, done, out);
            }
        }
        for (String i : c.getBaseInterfaces()) {
            ByteCodeClass b = byName.get(i.replace('/', '_').replace('$', '_'));
            if (b != null) {
                visit(b, byName, done, out);
            }
        }
        out.add(c);
    }
}
