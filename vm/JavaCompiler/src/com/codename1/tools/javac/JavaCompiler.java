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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Compiles Java source to class files. Written against the Codename One
 * JavaAPI subset so it can itself be translated by ParparVM and run in the
 * browser: the classes it compiles against come from a {@link ClassLibrary}
 * (the running VM's own class set), and the result is plain class-file bytes
 * the translator consumes.
 *
 * <p>Usage: add sources, call {@link #compile()}, read the class files and the
 * diagnostics from the {@link Result}.
 */
public final class JavaCompiler {
    private final ClassLibrary library;
    private final List<Source> sources = new ArrayList<Source>();

    public JavaCompiler(ClassLibrary library) {
        this.library = library;
    }

    /** Adds a compilation unit; {@code name} is used in diagnostics (for example {@code Main.java}). */
    public JavaCompiler addSource(String name, String text) {
        sources.add(new Source(name, text));
        return this;
    }

    /**
     * Adds a script: Java source whose top level may hold statements and methods
     * besides type declarations (see {@link ScriptSpec} for how it is wrapped).
     */
    public JavaCompiler addScript(String name, String text, ScriptSpec spec) {
        Source s = new Source(name, text);
        sources.add(s);
        scripts.put(s, spec);
        return this;
    }

    /**
     * An import every source gets after its own, as {@code a.b.*}, {@code a.b.C},
     * {@code static a.b.C.member} or {@code static a.b.C.*}.
     * The user's imports win: a class they import by name shadows an on-demand default.
     */
    public JavaCompiler addDefaultImport(String importName) {
        defaultImports.add(importName);
        return this;
    }

    /**
     * Compiles every call to {@code owner.name desc} (an instance method, resolved to
     * exactly that declaration) as {@code invokestatic toOwner.toName}, which receives
     * the receiver as its first argument followed by the original arguments. The
     * Playground uses it to keep {@code form.show()} inside its preview.
     */
    public JavaCompiler redirectCall(String owner, String name, String desc, String toOwner, String toName) {
        redirects.add(new String[]{owner, name, desc, toOwner, toName});
        return this;
    }

    private final List<String[]> redirects = new ArrayList<String[]>();
    private final List<String> defaultImports = new ArrayList<String>();
    private final Map<Source, ScriptSpec> scripts = new java.util.IdentityHashMap<Source, ScriptSpec>();

    public Result compile() {
        Compiler c = new Compiler(library);
        c.defaultImports.addAll(defaultImports);
        c.redirects.addAll(redirects);
        c.scripts.putAll(scripts);
        Map<String, byte[]> classes = c.compile(sources);
        List<ClassInfo> infos = new ArrayList<ClassInfo>();
        for (ClassSymbol cs : c.enter.sourceClasses) {
            if (classes.containsKey(cs.internalName)) {
                infos.add(new ClassInfo(cs));
            }
        }
        return new Result(classes, c.log.diagnostics, infos, c.syntaxErrors == 0);
    }

    /** What the compiled classes declare: enough to find an entry point without reflection. */
    public static final class ClassInfo {
        private final String name;
        private final boolean isPublic;
        private final boolean isTopLevel;
        private final boolean isAbstract;
        private final List<String> methods = new ArrayList<String>();
        /** Per method (as in {@link #methods}): its return type and every supertype of it, by internal name. */
        private final Map<String, List<String>> returnSupertypes = new java.util.HashMap<String, List<String>>();
        private final List<String> interfaces = new ArrayList<String>();
        private final String superclass;

        ClassInfo(ClassSymbol c) {
            name = c.internalName;
            isPublic = c.isPublic();
            isTopLevel = c.outer == null;
            isAbstract = c.isAbstract() || c.isInterface();
            for (MethodSymbol m : c.methods) {
                // Non-private: a launcher compiled into the same package can call it.
                if (!m.isPrivate()) {
                    String key = (m.isStatic() ? "static " : "") + m.name + descriptorOf(m);
                    methods.add(key);
                    Type r = m.returnType == null ? null : Types.erasure(m.returnType);
                    if (r != null && r.tag == Type.Tag.CLASS) {
                        List<String> sups = new ArrayList<String>();
                        collectSupertypes(((Type.ClassType) r).sym, sups);
                        returnSupertypes.put(key, sups);
                    }
                }
            }
            for (Type i : c.interfaces()) {
                if (i.tag == Type.Tag.CLASS) {
                    interfaces.add(((Type.ClassType) i).sym.internalName);
                }
            }
            Type sup = c.superclass();
            superclass = sup != null && sup.tag == Type.Tag.CLASS ? ((Type.ClassType) sup).sym.internalName : null;
        }

        private static void collectSupertypes(ClassSymbol c, List<String> out) {
            if (c == null || out.contains(c.internalName)) {
                return;
            }
            out.add(c.internalName);
            Type sup = c.superclass();
            if (sup != null && sup.tag == Type.Tag.CLASS) {
                collectSupertypes(((Type.ClassType) sup).sym, out);
            }
            for (Type i : c.interfaces()) {
                if (i.tag == Type.Tag.CLASS) {
                    collectSupertypes(((Type.ClassType) i).sym, out);
                }
            }
        }

        private static String descriptorOf(MethodSymbol m) {
            StringBuilder b = new StringBuilder("(");
            for (Type p : m.params) {
                b.append(Types.descriptor(Types.erasure(p)));
            }
            return b.append(')').append(Types.descriptor(Types.erasure(m.returnType))).toString();
        }

        /** Internal name ({@code pkg/Outer$Inner}). */
        public String getName() {
            return name;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public boolean isTopLevel() {
            return isTopLevel;
        }

        public boolean isAbstract() {
            return isAbstract;
        }

        public String getSuperclass() {
            return superclass;
        }

        public List<String> getInterfaces() {
            return Collections.unmodifiableList(interfaces);
        }

        /** Does the class declare a non-private method {@code name} with this descriptor ({@code (I)V})? */
        public boolean declares(String methodName, String descriptor, boolean isStatic) {
            return methods.contains((isStatic ? "static " : "") + methodName + descriptor);
        }

        /**
         * Does the non-private method {@code name} taking these parameters ({@code (I)})
         * return {@code internalName} or a subtype of it ({@code com/codename1/ui/Component})?
         */
        public boolean returnsSubtypeOf(String methodName, String paramsDescriptor, boolean isStatic, String internalName) {
            String prefix = (isStatic ? "static " : "") + methodName + paramsDescriptor;
            for (Map.Entry<String, List<String>> e : returnSupertypes.entrySet()) {
                if (e.getKey().startsWith(prefix) && e.getKey().charAt(prefix.length() - 1) == ')'
                        && e.getValue().contains(internalName)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The return descriptor of the non-private method {@code name} taking these
         * parameters ({@code (I)}), whatever it returns; null when there is none.
         */
        public String returnDescriptor(String methodName, String paramsDescriptor, boolean isStatic) {
            String prefix = (isStatic ? "static " : "") + methodName + paramsDescriptor;
            for (String m : methods) {
                if (m.startsWith(prefix) && m.length() > prefix.length() && m.charAt(prefix.length() - 1) == ')') {
                    return m.substring(prefix.length());
                }
            }
            return null;
        }
    }

    /** The outcome: class files keyed by internal name, and the diagnostics in report order. */
    public static final class Result {
        private final Map<String, byte[]> classes;
        private final List<Diagnostic> diagnostics;
        private final List<ClassInfo> classInfos;
        private final boolean wellFormed;

        Result(Map<String, byte[]> classes, List<Diagnostic> diagnostics, List<ClassInfo> classInfos, boolean wellFormed) {
            this.classes = classes;
            this.diagnostics = diagnostics;
            this.classInfos = classInfos;
            this.wellFormed = wellFormed;
        }

        /**
         * True when every source parsed: any errors are semantic (an unknown name, a
         * type mismatch), not syntax. A tool checking that a fragment is Java at all --
         * a documentation snippet that names classes it never imports -- asks this.
         */
        public boolean isWellFormed() {
            return wellFormed;
        }

        /** The compiled classes' declarations, in source order. */
        public List<ClassInfo> getClassInfos() {
            return Collections.unmodifiableList(classInfos);
        }

        public Map<String, byte[]> getClasses() {
            return Collections.unmodifiableMap(classes);
        }

        public List<Diagnostic> getDiagnostics() {
            return Collections.unmodifiableList(diagnostics);
        }

        public boolean isSuccess() {
            for (Diagnostic d : diagnostics) {
                if (d.error) {
                    return false;
                }
            }
            return true;
        }
    }
}
