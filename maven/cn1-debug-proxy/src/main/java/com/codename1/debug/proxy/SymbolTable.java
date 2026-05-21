/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.debug.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * In-memory model of the cn1-symbols.txt sidecar emitted by the translator
 * when {@code -Dcn1.onDeviceDebug=true} is set. Resolves device-sent
 * integer IDs back to JVM-style class/method names so the proxy can answer
 * JDWP queries like AllClasses, ClassesBySignature, Method.LineTable, etc.
 *
 * The format intentionally trades cleverness for parseability — see
 * Parser.writeSymbolSidecar in the translator for the writer.
 */
public final class SymbolTable {

    public static final class ClassInfo {
        public final int classId;
        public final String name;        // e.g. "java_lang_String" (translator convention)
        public final String sourceFile;
        public final List<MethodInfo> methods = new ArrayList<>();

        ClassInfo(int classId, String name, String sourceFile) {
            this.classId = classId;
            this.name = name;
            this.sourceFile = sourceFile;
        }

        /** JVM-style signature: "Ljava/lang/String;" derived from the underscore-separated name. */
        public String jvmSignature() {
            return "L" + name.replace('_', '/') + ";";
        }
    }

    public static final class LocalVarInfo {
        public final int slot;
        public final String name;
        public final String descriptor;

        LocalVarInfo(int slot, String name, String descriptor) {
            this.slot = slot;
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    public static final class MethodInfo {
        public final int methodId;
        public final int classId;
        public final String name;
        public final String descriptor;
        public final TreeSet<Integer> lines = new TreeSet<>();
        public final List<LocalVarInfo> locals = new ArrayList<>();

        MethodInfo(int methodId, int classId, String name, String descriptor) {
            this.methodId = methodId;
            this.classId = classId;
            this.name = name;
            this.descriptor = descriptor;
        }

        /**
         * argSlots counts JVM local slots consumed by method args (used by
         * JDWP VariableTable's argCnt field). Instance methods get an extra
         * slot for {@code this}; longs/doubles take two slots each.
         */
        public int argSlots(boolean isStatic) {
            int n = isStatic ? 0 : 1;
            int i = descriptor.indexOf('(');
            int end = descriptor.indexOf(')');
            if (i < 0 || end < 0) return n;
            int j = i + 1;
            while (j < end) {
                char c = descriptor.charAt(j);
                switch (c) {
                    case 'J': case 'D': n += 2; j++; break;
                    case 'L':
                        j = descriptor.indexOf(';', j) + 1; n++; break;
                    case '[':
                        while (descriptor.charAt(j) == '[') j++;
                        if (descriptor.charAt(j) == 'L') j = descriptor.indexOf(';', j) + 1;
                        else j++;
                        n++; break;
                    default: n++; j++;
                }
            }
            return n;
        }
    }

    private final Map<Integer, ClassInfo> classesById = new HashMap<>();
    private final Map<Integer, MethodInfo> methodsById = new HashMap<>();
    private final Map<String, ClassInfo> classesByJvmSig = new HashMap<>();
    private final Map<String, ClassInfo> classesBySourceFile = new HashMap<>();

    public static SymbolTable load(Path file) throws IOException {
        SymbolTable t = new SymbolTable();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                switch (parts[0]) {
                    case "version":
                        // 1 is the only known version; future versions can branch here.
                        break;
                    case "class": {
                        if (parts.length < 4) continue;
                        int id = Integer.parseInt(parts[1]);
                        ClassInfo c = new ClassInfo(id, parts[2], parts[3]);
                        t.classesById.put(id, c);
                        t.classesByJvmSig.put(c.jvmSignature(), c);
                        if (!parts[3].isEmpty()) {
                            t.classesBySourceFile.put(parts[3], c);
                        }
                        break;
                    }
                    case "method": {
                        if (parts.length < 5) continue;
                        int mid = Integer.parseInt(parts[1]);
                        int cid = Integer.parseInt(parts[2]);
                        MethodInfo m = new MethodInfo(mid, cid, parts[3], parts[4]);
                        t.methodsById.put(mid, m);
                        ClassInfo c = t.classesById.get(cid);
                        if (c != null) c.methods.add(m);
                        break;
                    }
                    case "line": {
                        if (parts.length < 3) continue;
                        int mid = Integer.parseInt(parts[1]);
                        int ln = Integer.parseInt(parts[2]);
                        MethodInfo m = t.methodsById.get(mid);
                        if (m != null) m.lines.add(ln);
                        break;
                    }
                    case "var": {
                        if (parts.length < 5) continue;
                        int mid = Integer.parseInt(parts[1]);
                        int slot = Integer.parseInt(parts[2]);
                        MethodInfo m = t.methodsById.get(mid);
                        if (m != null) m.locals.add(new LocalVarInfo(slot, parts[3], parts[4]));
                        break;
                    }
                    default:
                        // unknown directives are ignored to allow forward-compat
                }
            }
        }
        return t;
    }

    public ClassInfo classById(int id) { return classesById.get(id); }
    public MethodInfo methodById(int id) { return methodsById.get(id); }
    public ClassInfo classByJvmSignature(String sig) { return classesByJvmSig.get(sig); }
    public ClassInfo classBySourceFile(String name) { return classesBySourceFile.get(name); }
    public java.util.Collection<ClassInfo> allClasses() { return classesById.values(); }
    public java.util.Collection<MethodInfo> allMethods() { return methodsById.values(); }

    /**
     * Resolves "ClassName:42" style breakpoint references the way jdb states
     * them. Walks the class's methods and returns the one whose line set
     * contains the requested source line, or null if no method covers it.
     */
    public MethodInfo methodCoveringLine(String className, int line) {
        ClassInfo c = classesById.values().stream()
                .filter(ci -> ci.name.equals(className) || ci.name.replace('_', '.').equals(className))
                .findFirst().orElse(null);
        if (c == null) return null;
        for (MethodInfo m : c.methods) {
            if (m.lines.contains(line)) return m;
        }
        return null;
    }
}
