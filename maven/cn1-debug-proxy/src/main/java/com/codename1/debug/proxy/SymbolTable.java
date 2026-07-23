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
package com.codename1.debug.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * In-memory model of the symbol table the translator emits when
 * {@code -Dcn1.onDeviceDebug=true} is set. The table is gzip-compressed into
 * the iOS binary and streamed to this proxy over the wire on connect (there
 * is no local sidecar file). Resolves device-sent integer IDs back to
 * JVM-style class/method names so the proxy can answer JDWP queries like
 * AllClasses, ClassesBySignature, Method.LineTable, etc.
 *
 * The format intentionally trades cleverness for parseability — see
 * Parser.writeSymbolSidecar in the translator for the writer.
 */
public final class SymbolTable {

    public static final class ClassInfo {
        public final int classId;
        public final String name;        // e.g. "java_lang_String" (translator convention)
        public final String jvmName;     // e.g. "java/lang/String$CaseInsensitiveComparator"
        public final String sourceFile;
        /** Superclass classId, or -1 if java.lang.Object / unknown. */
        public int superId = -1;
        public final List<MethodInfo> methods = new ArrayList<>();
        /**
         * Instance fields physically stored in this class's struct (i.e. including
         * those inherited from parents). The translator emits all of them under
         * the storing class so the proxy can satisfy ObjectReference.GetValues
         * without walking a JDWP ReferenceType hierarchy.
         */
        public final List<FieldInfo> instanceFields = new ArrayList<>();

        ClassInfo(int classId, String name, String sourceFile, String jvmName) {
            this.classId = classId;
            this.name = name;
            this.sourceFile = sourceFile;
            this.jvmName = jvmName;
        }

        /** JVM-style signature retaining inner-class markers and literal underscores. */
        public String jvmSignature() {
            return "L" + jvmName + ";";
        }
    }

    public static final class FieldInfo {
        public final int fieldId;
        public final int classId;        // declaring class, not necessarily the holding class
        public final String name;
        public final String descriptor;  // JVM-style: "I", "Ljava/lang/String;", "[I", ...
        public final int accessFlags;    // JDWP modifier bits

        FieldInfo(int fieldId, int classId, String name, String descriptor, int accessFlags) {
            this.fieldId = fieldId;
            this.classId = classId;
            this.name = name;
            this.descriptor = descriptor;
            this.accessFlags = accessFlags;
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
        public final boolean isStatic;
        public final TreeSet<Integer> lines = new TreeSet<>();
        public final List<LocalVarInfo> locals = new ArrayList<>();

        MethodInfo(int methodId, int classId, String name, String descriptor, boolean isStatic) {
            this.methodId = methodId;
            this.classId = classId;
            this.name = name;
            this.descriptor = descriptor;
            this.isStatic = isStatic;
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
    private final Map<Integer, FieldInfo> fieldsById = new HashMap<>();
    private final Map<String, ClassInfo> classesByJvmSig = new HashMap<>();
    private final Map<String, ClassInfo> classesBySourceFile = new HashMap<>();

    /**
     * Parses the tab-delimited symbol table streamed off the device (already
     * gzip-inflated by the caller). The proxy no longer reads a local sidecar
     * file — the table travels over the wire via CMD_GET_SYMBOLS.
     */
    public static SymbolTable load(InputStream in) throws IOException {
        SymbolTable t = new SymbolTable();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
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
                        // New tables carry the original JVM internal name in
                        // column 6. Older tables only contain ParparVM's
                        // underscore-mangled identifier, so retain the legacy
                        // best-effort reconstruction for compatibility.
                        String jvmName = parts.length >= 6 && !parts[5].isEmpty()
                                ? parts[5] : parts[2].replace('_', '/');
                        ClassInfo c = new ClassInfo(id, parts[2], parts[3], jvmName);
                        if (parts.length >= 5) {
                            try { c.superId = Integer.parseInt(parts[4]); }
                            catch (NumberFormatException ignore) {}
                        }
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
                        boolean isStatic = parts.length >= 6 && "1".equals(parts[5]);
                        MethodInfo m = new MethodInfo(mid, cid, parts[3], parts[4], isStatic);
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
                    case "field": {
                        if (parts.length < 6) continue;
                        int classId = Integer.parseInt(parts[1]);
                        int fid = Integer.parseInt(parts[2]);
                        int access = Integer.parseInt(parts[5]);
                        FieldInfo fi = new FieldInfo(fid, classId, parts[3], parts[4], access);
                        t.fieldsById.put(fid, fi);
                        ClassInfo c = t.classesById.get(classId);
                        if (c != null) c.instanceFields.add(fi);
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
    public FieldInfo fieldById(int id) { return fieldsById.get(id); }
    public int fieldCount() { return fieldsById.size(); }
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
                .filter(ci -> ci.name.equals(className)
                        || ci.jvmName.equals(className)
                        || ci.jvmName.replace('/', '.').equals(className))
                .findFirst().orElse(null);
        if (c == null) return null;
        for (MethodInfo m : c.methods) {
            if (m.lines.contains(line)) return m;
        }
        return null;
    }
}
