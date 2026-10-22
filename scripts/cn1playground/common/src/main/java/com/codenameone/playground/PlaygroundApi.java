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
package com.codenameone.playground;

import com.codename1.tools.javac.StubLibrary;
import com.codename1.tools.translator.classfile.ClassReader;
import com.codename1.tools.translator.classfile.ClassVisitor;
import com.codename1.tools.translator.classfile.FieldVisitor;
import com.codename1.tools.translator.classfile.MethodVisitor;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The API user code is compiled against: a stub library of every class the
 * Playground's VM contains (JavaAPI, Codename One core, the Playground's own
 * helpers), generated at build time into {@value #RESOURCE}. The compiler and the
 * in-browser translator read class files from it; the editor's completion lists
 * classes and members from it.
 */
final class PlaygroundApi {
    static final String RESOURCE = "/playground-api.cn1stubs";

    private static StubLibrary library;
    private static String[] classNames;
    private static final Map<String, String[]> methodCache = new HashMap<String, String[]>();
    private static final Map<String, String[]> fieldCache = new HashMap<String, String[]>();
    private static final Map<String, Map<String, String>> memberTypeCache = new HashMap<String, Map<String, String>>();

    private PlaygroundApi() {
    }

    /** The stub library; loaded on first use. */
    static synchronized StubLibrary library() throws IOException {
        if (library == null) {
            // Through Display where there is one (the browser serves resources itself); the
            // class loader where there is not -- headless tools compile without a Display.
            InputStream in = Display.isInitialized()
                    ? Display.getInstance().getResourceAsStream(PlaygroundApi.class, RESOURCE) : null;
            if (in == null) {
                in = PlaygroundApi.class.getResourceAsStream(RESOURCE);
            }
            if (in == null) {
                throw new IOException("Missing " + RESOURCE + " (the Playground build generates it)");
            }
            // Plain streams rather than Util: Util reaches the implementation, which a
            // headless tool does not have.
            try {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bytes.write(buf, 0, n);
                }
                library = new StubLibrary(bytes.toByteArray());
            } finally {
                in.close();
            }
        }
        return library;
    }

    /** Like {@link #library()} but null when unavailable (completion degrades quietly). */
    static StubLibrary libraryOrNull() {
        try {
            return library();
        } catch (IOException e) {
            return null;
        }
    }

    /** Public classes (and public member classes) as dotted names: {@code java.util.Map.Entry}. */
    static synchronized String[] classNames() {
        if (classNames == null) {
            StubLibrary lib = libraryOrNull();
            List<String> out = new ArrayList<String>();
            if (lib != null) {
                for (String n : lib.classNames()) {
                    if (isListed(lib, n)) {
                        out.add(n.replace('/', '.').replace('$', '.'));
                    }
                }
            }
            classNames = out.toArray(new String[out.size()]);
        }
        return classNames;
    }

    private static Map<String, String> uniqueBySimpleName;

    /// The one public top-level class with this simple name, as a dotted name, or
    /// null when there is none or more than one (one Codename One class wins
    /// over a JDK class of the same name). Drives the auto-import of names
    /// a snippet uses without importing them (``PlaygroundRunner.compile``).
    static synchronized String uniqueClassForSimpleName(String simpleName) {
        if (uniqueBySimpleName == null) {
            StubLibrary lib = libraryOrNull();
            Map<String, String> unique = new HashMap<String, String>();
            Map<String, String> codenameOne = new HashMap<String, String>();
            if (lib != null) {
                for (String n : lib.classNames()) {
                    if (n.indexOf('$') >= 0 || !isListed(lib, n)) {
                        continue;
                    }
                    String simple = n.substring(n.lastIndexOf('/') + 1);
                    String dotted = n.replace('/', '.');
                    // An empty value marks a name two packages share.
                    unique.put(simple, unique.containsKey(simple) ? "" : dotted);
                    if (n.startsWith("com/codename1/")) {
                        codenameOne.put(simple, codenameOne.containsKey(simple) ? "" : dotted);
                    }
                }
            }
            // A name the device's class library shares with one Codename One
            // class (SecureRandom, Temporal) means the Codename One one: the
            // snippets are written against the framework, and the java.* twin
            // only exists so ported JDK code compiles.
            for (Map.Entry<String, String> e : codenameOne.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    unique.put(e.getKey(), e.getValue());
                }
            }
            uniqueBySimpleName = unique;
        }
        String cls = uniqueBySimpleName.get(simpleName);
        return cls == null || cls.isEmpty() ? null : cls;
    }

    private static boolean isListed(StubLibrary lib, String internalName) {
        int dollar = internalName.lastIndexOf('$');
        if (dollar >= 0 && dollar + 1 < internalName.length() && Character.isDigit(internalName.charAt(dollar + 1))) {
            return false;
        }
        if (internalName.startsWith("com/codename1/impl/") || internalName.startsWith("com/codename1/tools/")
                || internalName.startsWith("org/teavm/")) {
            return false;
        }
        final int[] access = new int[1];
        byte[] b = lib.classBytes(internalName);
        if (b == null) {
            return false;
        }
        new ClassReader(b).accept(new ClassVisitor() {
            @Override
            public void visit(int version, int acc, String name, String signature, String superName, String[] interfaces) {
                access[0] = acc;
            }

        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return (access[0] & 0x0001) != 0;
    }

    /** Public method display strings ({@code setText(String)}) of a class and its supertypes. */
    static synchronized String[] methodSignatures(String dottedName) {
        String[] cached = methodCache.get(dottedName);
        if (cached == null) {
            Set<String> out = new LinkedHashSet<String>();
            collectMembers(internalName(dottedName), out, null, null, new LinkedHashSet<String>());
            cached = out.toArray(new String[out.size()]);
            methodCache.put(dottedName, cached);
        }
        return cached;
    }

    /** Public field names of a class and its supertypes. */
    static synchronized String[] fieldNames(String dottedName) {
        String[] cached = fieldCache.get(dottedName);
        if (cached == null) {
            Set<String> out = new LinkedHashSet<String>();
            collectMembers(internalName(dottedName), null, out, null, new LinkedHashSet<String>());
            cached = out.toArray(new String[out.size()]);
            fieldCache.put(dottedName, cached);
        }
        return cached;
    }

    /**
     * The type of a member of a class or its supertypes, for completing after a
     * chained expression ({@code b.getAllStyles().}): {@code name/argc} keys a
     * method (its return type), {@code name} a field. Answers the dotted class name,
     * or "" when the member is unknown or its type is a primitive or an array --
     * neither has members to offer. A subclass is read before its supertypes, so a
     * covariant override's narrower return type wins.
     */
    static synchronized String memberType(String dottedName, String key) {
        Map<String, String> types = memberTypeCache.get(dottedName);
        if (types == null) {
            types = new HashMap<String, String>();
            collectMembers(internalName(dottedName), null, null, types, new LinkedHashSet<String>());
            memberTypeCache.put(dottedName, types);
        }
        String t = types.get(key);
        return t == null ? "" : t;
    }

    /** {@code Lcom/codename1/ui/plaf/Style;} to {@code com.codename1.ui.plaf.Style}; "" for anything else. */
    private static String referenceType(String desc) {
        if (desc.length() < 3 || desc.charAt(0) != 'L') {
            return "";
        }
        return desc.substring(1, desc.length() - 1).replace('/', '.').replace('$', '.');
    }

    private static int paramCount(String desc) {
        String params = simpleParams(desc);
        if (params.length() == 0) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < params.length(); i++) {
            if (params.charAt(i) == ',') {
                n++;
            }
        }
        return n;
    }

    /** {@code java.util.Map.Entry} to the internal name the library has ({@code java/util/Map$Entry}). */
    private static String internalName(String dotted) {
        StubLibrary lib = libraryOrNull();
        String n = dotted.replace('.', '/');
        if (lib == null) {
            return n;
        }
        while (lib.classBytes(n) == null) {
            int slash = n.lastIndexOf('/');
            if (slash < 0) {
                return dotted.replace('.', '/');
            }
            n = n.substring(0, slash) + "$" + n.substring(slash + 1);
        }
        return n;
    }

    private static void collectMembers(String internalName, final Set<String> methods, final Set<String> fields,
            final Map<String, String> types, Set<String> seen) {
        StubLibrary lib = libraryOrNull();
        if (lib == null || internalName == null || !seen.add(internalName)) {
            return;
        }
        byte[] b = lib.classBytes(internalName);
        if (b == null) {
            return;
        }
        final List<String> supers = new ArrayList<String>();
        new ClassReader(b).accept(new ClassVisitor() {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (superName != null) {
                    supers.add(superName);
                }
                if (interfaces != null) {
                    for (String i : interfaces) {
                        supers.add(i);
                    }
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (fields != null && (access & 0x0001) != 0) {
                    fields.add(name);
                }
                if (types != null && (access & 0x0001) != 0 && !types.containsKey(name)) {
                    types.put(name, referenceType(desc));
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (methods != null && (access & 0x0001) != 0 && !name.startsWith("<")) {
                    methods.add(name + "(" + simpleParams(desc) + ")");
                }
                if (types != null && (access & 0x0001) != 0 && !name.startsWith("<")) {
                    String key = name + "/" + paramCount(desc);
                    if (!types.containsKey(key)) {
                        types.put(key, referenceType(desc.substring(desc.indexOf(')') + 1)));
                    }
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        for (String s : supers) {
            collectMembers(s, methods, fields, types, seen);
        }
    }

    /** {@code (ILjava/lang/String;[B)V} to {@code int,String,byte[]}. */
    static String simpleParams(String desc) {
        StringBuilder b = new StringBuilder();
        int i = 1;
        while (i < desc.length() && desc.charAt(i) != ')') {
            int dims = 0;
            while (desc.charAt(i) == '[') {
                dims++;
                i++;
            }
            String t;
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String full = desc.substring(i + 1, end);
                int cut = Math.max(full.lastIndexOf('/'), full.lastIndexOf('$'));
                t = full.substring(cut + 1);
                i = end + 1;
            } else {
                t = primitiveName(c);
                i++;
            }
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(t);
            for (int d = 0; d < dims; d++) {
                b.append("[]");
            }
        }
        return b.toString();
    }

    private static String primitiveName(char c) {
        switch (c) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            default: return "void";
        }
    }
}
