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
package com.codename1.dart.transpiler.codegen;

import com.codename1.dart.transpiler.analyze.Program;
import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.ast.Ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Emits Java 17 source from the transpiler AST.
 *
 * <p>M1 emitter notes:
 * <ul>
 *   <li>Type resolution is folded into emission (each expression returns its
 *       code and static type); the standalone resolver pipeline arrives with
 *       M2 when inference needs grow.</li>
 *   <li>Stub-class constructors with named arguments emit as
 *       allocate-then-setter sequences (ANF); program-class constructors use
 *       canonical positional order with defaults inlined at call sites.</li>
 *   <li>Known M1 semantic divergence: for stub widgets, the constructor runs
 *       before named-argument expressions are evaluated (widgets are pure
 *       config objects, so this is unobservable in practice).</li>
 * </ul>
 */
public final class JavaEmitter {

    private final Program program;
    private final StubRegistry stubs;
    private final Diagnostics diags;
    private final String pkg;
    /** Distinct record shapes encountered during emission; one Java record class is generated per shape. */
    private final Map<String, RecordShape> recordShapes = new LinkedHashMap<String, RecordShape>();

    /** The structural shape of a Dart record: positional arity plus the sorted names of named fields. */
    private static final class RecordShape {
        int positional;
        List<String> named;
    }

    public JavaEmitter(Program program, StubRegistry stubs, Diagnostics diags, String pkg) {
        this.program = program;
        this.stubs = stubs;
        this.diags = diags;
        this.pkg = pkg;
        program.finishParts();
        superclassMixinsToMixins();
    }

    /**
     * {@code class X extends ListMixin<int>} (or {@code extends M} for any mixin, program or
     * stub, including an {@code abstract mixin class}) is {@code class X with M}: Object is
     * the superclass and M is applied to it. A mixin maps to a Java interface, which no
     * class can extend, so the superclass is moved to the front of the mixin list.
     */
    private void superclassMixinsToMixins() {
        for (Ast.Library lib : program.libraries) {
            for (Ast.ClassDecl c : lib.classes) {
                if (c.superclass == null || c.isMixin || c.extensionOn != null) {
                    continue;
                }
                Ast.ClassDecl sup = superDeclOf(c, c.superclass);
                if (sup == null) {
                    sup = stubs.classes.get(c.superclass.name);
                }
                if (sup != null && sup.isMixin) {
                    c.mixins.add(0, c.superclass);
                    c.superclass = null;
                }
            }
        }
    }

    // ==================================================================
    // Top level
    // ==================================================================

    // Codegen robustness: a resolver/emit gap on one declaration must not abort the whole build.
    // Record it (with the crash site) so a single pass yields the full gap inventory.
    private void emitCrash(Library lib, String what, Throwable ex) {
        StackTraceElement top = null;
        for (StackTraceElement s : ex.getStackTrace()) {
            if (s.getClassName().startsWith("com.codename1.dart.transpiler")) { top = s; break; }
        }
        String at = top == null ? "" : "  @ "
                + top.getClassName().substring(top.getClassName().lastIndexOf('.') + 1)
                + "." + top.getMethodName() + ":" + top.getLineNumber();
        diags.error(lib.fileName, 0, 0, "E0005",
                "Codegen crash on " + what + ": " + ex.getClass().getSimpleName()
                + (ex.getMessage() != null ? ": " + ex.getMessage() : "") + at);
    }

    /// Marks a case of the $dget/$dset/$dcall switches. The switches are emitted while the
    /// program's dynamic accesses are still being found, so each case is marked and
    /// pruneDynamicCases drops, after the whole program is emitted, every case whose
    /// name no `dynamic` access in the program uses. A case keeps its member alive
    /// against ParparVM's dead-code removal, and unpruned they were most of a 20%
    /// native size regression (one case per localized string, for instance).
    private static final String DYN_CASE = "/*dyn*/";

    /// Every member name a `dynamic` receiver is read, written or called with.
    private final java.util.Set<String> dynNames = new java.util.HashSet<String>();

    private List<GeneratedFile> pruneDynamicCases(List<GeneratedFile> files) {
        List<GeneratedFile> pruned = new ArrayList<GeneratedFile>(files.size());
        for (GeneratedFile g : files) {
            if (g == null || g.content.indexOf(DYN_CASE) < 0) {
                pruned.add(g);
                continue;
            }
            StringBuilder sb = new StringBuilder(g.content.length());
            for (String line : g.content.split("\n", -1)) {
                int at = line.indexOf(DYN_CASE + "case \"");
                if (at >= 0) {
                    int start = at + DYN_CASE.length() + 6;
                    String name = line.substring(start, line.indexOf('"', start));
                    // a case spanning lines (a closure default) is kept rather than cut in half
                    if (!dynNames.contains(name) && line.trim().endsWith(";")) {
                        continue;
                    }
                    line = line.substring(0, at) + line.substring(at + DYN_CASE.length());
                }
                sb.append(line).append('\n');
            }
            sb.setLength(sb.length() - 1);
            pruned.add(new GeneratedFile(g.relativePath, sb.toString()));
        }
        return pruned;
    }

    /// The Dart file each generated Java file was emitted from, by its relative path.
    private final Map<String, String> dartFileOf = new java.util.HashMap<String, String>();

    public List<GeneratedFile> emit() {
        List<GeneratedFile> pruned = pruneDynamicCases(emitAll());
        // After the pruning, which drops lines: the table pairs final Java lines.
        List<GeneratedFile> mapped = new ArrayList<GeneratedFile>(pruned.size());
        for (GeneratedFile g : pruned) {
            String dart = g == null ? null : dartFileOf.get(g.relativePath);
            if (g != null && g.relativePath.endsWith(".java")) {
                g = new GeneratedFile(g.relativePath, JavaKeywordEscaper.apply(g.content));
            }
            mapped.add(dart == null ? g : new GeneratedFile(g.relativePath, DartLineTable.apply(g.content, dart)));
        }
        return mapped;
    }

    /// The pub packages `lib` imports, `firebase_app_check` for
    /// `package:firebase_app_check/firebase_app_check.dart`.
    private static java.util.Set<String> importedPackages(Library lib) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        for (String uri : lib.imports) {
            if (uri != null && uri.startsWith("package:")) {
                int slash = uri.indexOf('/');
                out.add(slash < 0 ? uri.substring(8) : uri.substring(8, slash));
            }
        }
        return out;
    }

    /// Adds `g` to `out`, remembering the Dart file it came from for its line table.
    private void addMapped(List<GeneratedFile> out, GeneratedFile g, String dartFile) {
        out.add(g);
        if (g != null && dartFile != null) {
            dartFileOf.put(g.relativePath, dartFile);
        }
    }

    /// Writes the line mark ahead of what `n` lowers to (see DartLineTable).
    private void lineMark(Node n, Ctx ctx) {
        String mark = n == null ? null : DartLineTable.mark(n.file, n.line);
        if (mark != null) {
            ctx.writer().line(mark);
        }
    }

    private List<GeneratedFile> emitAll() {
        superclassesAsMixins();
        List<GeneratedFile> out = new ArrayList<GeneratedFile>();
        // Every library with a top-level main(), collected in source order (which is itself
        // sorted by path -- see DartTranspiler.transpile -- not necessarily the order a
        // human would expect). Resolved to a single entry point once the whole program has
        // been scanned, below, instead of overwriting a "last one wins" field as libraries
        // are visited: a tree with both lib/main.dart and lib/main_dev.dart (a common Flutter
        // flavor/dev-entry-point convention) would otherwise hand FlutterRegistry whichever
        // of the two sorts last, silently, with no diagnostic.
        List<Library> mainCandidates = new ArrayList<Library>();
        for (Library lib : program.libraries) {
            stubs.viewFrom(importedPackages(lib), program.classes.keySet());
            for (ClassDecl c : lib.classes) {
                try {
                    String dart = c.file != null ? c.file : lib.fileName;
                    if (c.extensionOn != null) {
                        addMapped(out, emitExtension(c), dart);
                    } else if (c.isMixin) {
                        addMapped(out, emitMixin(c), dart);
                    } else {
                        addMapped(out, emitClass(c), dart);
                        for (GeneratedFile extra : pendingFiles) {
                            addMapped(out, extra, dart);
                        }
                        pendingFiles.clear();
                    }
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "class " + c.name, ex);
                }
            }
            for (EnumDecl e : lib.enums) {
                try {
                    addMapped(out, emitEnum(e), e.file != null ? e.file : lib.fileName);
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "enum " + e.name, ex);
                }
            }
            if (!lib.functions.isEmpty() || !lib.topLevelVars.isEmpty()) {
                try {
                    addMapped(out, emitLibClass(lib), lib.fileName);
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "library " + lib.fileName, ex);
                }
                for (FunctionDecl f : lib.functions) {
                    if (f.name.equals("main")) {
                        mainCandidates.add(lib);
                    }
                }
            }
        }
        // What is emitted from here on belongs to no one library: the loaded-last view.
        stubs.viewFrom(java.util.Collections.<String>emptySet(), java.util.Collections.<String>emptySet());
        String mainLib = resolveMainLib(mainCandidates);
        if (mainLib != null) {
            out.add(emitRegistry(mainLib));
        }
        // record classes are discovered lazily while emitting bodies, so generate them last
        for (Map.Entry<String, RecordShape> e : recordShapes.entrySet()) {
            out.add(emitRecordClass(e.getKey(), e.getValue()));
        }
        return out;
    }

    /**
     * Picks the single library FlutterRegistry should invoke, out of every library that
     * declares a top-level main(). Deterministic rather than "last in source order wins":
     * with exactly one candidate it is used as-is; with several, the conventional Flutter
     * entry point (source-root {@code main.dart}, or {@code lib/main.dart} when the source
     * root is the Flutter project root rather than its {@code lib} directory) is preferred
     * when present; otherwise emission fails with a diagnostic naming every candidate rather
     * than silently picking one.
     */
    private String resolveMainLib(List<Library> mainCandidates) {
        if (mainCandidates.isEmpty()) {
            return null;
        }
        if (mainCandidates.size() == 1) {
            return Program.libClassName(mainCandidates.get(0).fileName);
        }
        for (Library lib : mainCandidates) {
            if (isConventionalMainEntry(lib.fileName)) {
                return Program.libClassName(lib.fileName);
            }
        }
        StringBuilder names = new StringBuilder();
        for (Library lib : mainCandidates) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(lib.fileName);
        }
        diags.error(mainCandidates.get(0).fileName, 0, 0, "E0006",
                "Multiple libraries declare a top-level main() and none is the conventional "
                + "entry point (lib/main.dart): " + names
                + ". Remove or rename the extra main() functions, or make the intended entry "
                + "point lib/main.dart.");
        return null;
    }

    /** Whether {@code fileName} is Flutter's default target, {@code lib/main.dart}. */
    private static boolean isConventionalMainEntry(String fileName) {
        String norm = fileName.replace('\\', '/');
        return norm.equals("main.dart") || norm.equals("lib/main.dart");
    }

    /** Registers a record shape (idempotent) and returns its generated class name. */
    private String registerRecordShape(int positional, List<String> namedSorted) {
        StringBuilder n = new StringBuilder("Rec$").append(positional);
        for (String nm : namedSorted) {
            n.append('$').append(nm);
        }
        String cn = n.toString();
        if (!recordShapes.containsKey(cn)) {
            RecordShape s = new RecordShape();
            s.positional = positional;
            s.named = namedSorted;
            recordShapes.put(cn, s);
        }
        return cn;
    }

    /** Registers the shape a record type name (built by the parser as Rec$N$names) denotes. */
    private void ensureRecordShape(String cn) {
        if (cn == null || !cn.startsWith("Rec$") || recordShapes.containsKey(cn)) {
            return;
        }
        String[] parts = cn.split("\\$", -1);
        if (parts.length < 2) {
            return;
        }
        int positional;
        try {
            positional = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        List<String> named = new ArrayList<String>();
        for (int i = 2; i < parts.length; i++) {
            named.add(parts[i]);
        }
        registerRecordShape(positional, named);
    }

    /** Emits a generic Java record class for a record shape (component types are the type parameters). */
    private GeneratedFile emitRecordClass(String cn, RecordShape s) {
        int total = s.positional + s.named.size();
        StringBuilder tp = new StringBuilder();
        StringBuilder comps = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i > 0) {
                tp.append(", ");
                comps.append(", ");
            }
            tp.append("T").append(i);
            String comp = i < s.positional ? "$" + (i + 1) : s.named.get(i - s.positional);
            comps.append("T").append(i).append(' ').append(comp);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/** Generated Dart record type (structural shape ").append(cn).append("). */\n");
        sb.append("public record ").append(cn);
        if (total > 0) {
            sb.append('<').append(tp).append('>');
        }
        sb.append('(').append(comps).append(") {\n");
        // Dart's record toString: `(1, a)`, `(a: 1, b: true)`, `(7, label: seven)`.
        sb.append("    @Override\n    public String toString() {\n");
        sb.append("        return \"(\"");
        for (int i = 0; i < total; i++) {
            if (i > 0) {
                sb.append(" + \", \"");
            }
            String comp = i < s.positional ? "$" + (i + 1) : s.named.get(i - s.positional);
            if (i >= s.positional) {
                sb.append(" + \"").append(comp).append(": \"");
            }
            sb.append(" + dart.runtime.DartRuntime.str(").append(comp).append(')');
        }
        sb.append(" + \")\";\n    }\n}\n");
        return new GeneratedFile(cn + ".java", sb.toString());
    }

    private Out emitRecordLit(RecordLit r, Ctx ctx) {
        List<RecordField> positional = new ArrayList<RecordField>();
        List<RecordField> named = new ArrayList<RecordField>();
        for (RecordField f : r.fields) {
            if (f.name == null) {
                positional.add(f);
            } else {
                named.add(f);
            }
        }
        named.sort((a, b) -> a.name.compareTo(b.name));
        List<String> namedNames = new ArrayList<String>();
        for (RecordField f : named) {
            namedNames.add(f.name);
        }
        String cn = registerRecordShape(positional.size(), namedNames);
        TypeRef t = new TypeRef(cn);
        StringBuilder args = new StringBuilder();
        boolean first = true;
        for (RecordField f : positional) {
            if (!first) {
                args.append(", ");
            }
            first = false;
            Out o = emitExpr(f.value, null, ctx);
            args.append(boxIfPrimitive(o, ctx));
            t.args.add(boxType(o.type));
        }
        for (RecordField f : named) {
            if (!first) {
                args.append(", ");
            }
            first = false;
            Out o = emitExpr(f.value, null, ctx);
            args.append(boxIfPrimitive(o, ctx));
            t.args.add(boxType(o.type));
        }
        return new Out("new " + cn + (r.fields.isEmpty() ? "(" : "<>(") + args + ")", t);
    }

    /** The static type of a record component accessed by name (`$1`, `$2`, or a named field). */
    private TypeRef recordComponentType(TypeRef recordType, String name) {
        ensureRecordShape(recordType.name);
        RecordShape s = recordShapes.get(recordType.name);
        if (s == null) {
            return TypeRef.DYNAMIC;
        }
        int idx = -1;
        if (name.length() > 1 && name.charAt(0) == '$') {
            try {
                idx = Integer.parseInt(name.substring(1)) - 1;
            } catch (NumberFormatException ignored) {
                idx = -1;
            }
        } else {
            int at = s.named.indexOf(name);
            if (at >= 0) {
                idx = s.positional + at;
            }
        }
        return idx >= 0 && idx < recordType.args.size() ? recordType.args.get(idx) : TypeRef.DYNAMIC;
    }

    private GeneratedFile emitRegistry(String mainLib) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("/** Generated entry-point registry for transpiled Flutter code. */\n");
        sb.append("public final class FlutterRegistry {\n");
        sb.append("    private FlutterRegistry() {\n    }\n\n");
        sb.append("    /** Invokes the Dart main() of the application's main library. */\n");
        sb.append("    public static void invokeMain() {\n");
        sb.append("        ").append(mainLib).append(".main$();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return new GeneratedFile("FlutterRegistry.java", sb.toString());
    }

    private GeneratedFile emitEnum(EnumDecl e) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        // Dart 2.17 enhanced enums carry a body (fields, methods, constructors). Emit the
        // members through the same machinery as a class, with a synthetic ClassDecl standing
        // in for `this`-typing and own-member resolution inside the bodies.
        boolean enhanced = !e.methods.isEmpty() || !e.fields.isEmpty() || !e.ctors.isEmpty();
        ClassDecl syn = new ClassDecl();
        syn.name = e.name;
        syn.ownerLibrary = e.ownerLibrary;
        syn.enumOf = e;
        String enumJava = javaEnumName(e);
        syn.fields = e.fields;
        syn.methods = e.methods;
        syn.ctors = e.ctors;
        Ctx ctx = new Ctx(syn);
        StringBuilder body = new StringBuilder();
        for (FieldDecl f : e.fields) {
            TypeRef ft = fieldType(f, ctx);
            String jt = javaType(ft, false, ctx);
            if (f.isStatic) {
                body.append(f.name.startsWith("_") ? "    static " : "    public static ");
            } else {
                body.append("    private ");
            }
            if ((f.isFinal || f.isConst)) {
                body.append("final ");
            }
            body.append(jt).append(' ').append(f.name).append(";\n");
            if (!f.name.startsWith("_") && !f.isStatic) {
                body.append("    public ").append(jt).append(" get$").append(f.name)
                        .append("() {\n        return ").append(f.name).append(";\n    }\n");
            }
        }
        CtorDecl entryCtor = null;
        java.util.Map<String, CtorDecl> namedEntryCtors = new java.util.HashMap<String, CtorDecl>();
        for (CtorDecl ct : e.ctors) {
            // A named generative constructor (`const E.named(...)`, used by an entry as
            // `x.named(...)`) cannot be the marker-constructor-plus-$init$ pair a class gets:
            // an enum's final fields must be assigned in a Java constructor. It is emitted as
            // an unnamed one instead, told apart from the others by a leading parameter of a
            // marker type of its own, which the entry passes as a typed null.
            String named = ct.isFactory ? null : ct.name;
            String ctor;
            if (named != null) {
                ct.name = null;
                try {
                    ctor = emitCtor(syn, ct, ctx);
                } finally {
                    ct.name = named;
                }
                namedEntryCtors.put(named, ct);
                body.append("    private static final class $Ctor_").append(named).append(" {\n    }\n");
            } else {
                ctor = emitCtor(syn, ct, ctx);
            }
            // A Java enum constructor may not be public; the constants are its only callers.
            String pub = "    public " + enumJava + "(";
            if (ctor.startsWith(pub)) {
                String rest = ctor.substring(pub.length());
                String marker = named == null ? ""
                        : "$Ctor_" + named + " $named" + (rest.startsWith(")") ? "" : ", ");
                ctor = "    private " + enumJava + "(" + marker + rest;
            }
            body.append(ctor);
            if (ct.name == null && !ct.isFactory) {
                entryCtor = ct;
            }
        }
        for (MethodDecl m : e.methods) {
            Method mm = new Method();
            mm.name = m.name;
            mm.isStatic = m.isStatic;
            mm.isGetter = m.isGetter;
            mm.isSetter = m.isSetter;
            // Enum methods may override Enum.toString etc.; @Override is optional in Java, so
            // omit it rather than risk annotating a method that overrides nothing.
            mm.isOverride = false;
            mm.isAbstract = m.isAbstract;
            mm.isAsync = m.isAsync;
            mm.isSyncStar = m.isSyncStar;
            mm.isAsyncStar = m.isAsyncStar;
            mm.returnType = m.returnType;
            mm.params = m.params;
            mm.typeParams = m.typeParams;
            mm.typeParamBounds = m.typeParamBounds;
            mm.body = m.body;
            mm.exprBody = m.exprBody;
            body.append(emitMethodLike(mm, ctx, false));
        }
        // `boundary('boundary')`: the entry's arguments go to the enum's constructor, in the
        // constructor's own (canonical) parameter order. Emitted before the imports are
        // written, since an argument may need one.
        String[] entryArgCode = new String[e.entries.size()];
        for (int i = 0; i < entryArgCode.length; i++) {
            Args entryArgs = i < e.entryArgs.size() ? e.entryArgs.get(i) : null;
            String entryNamed = i < e.entryCtors.size() ? e.entryCtors.get(i) : null;
            CtorDecl namedCtor = entryNamed != null ? namedEntryCtors.get(entryNamed) : null;
            if (entryArgs != null && namedCtor != null) {
                String canon = canonicalArgs(namedCtor, syn, entryArgs, ctx);
                entryArgCode[i] = "(($Ctor_" + entryNamed + ") null"
                        + (canon.isEmpty() ? "" : ", " + canon) + ")";
            } else if (entryNamed != null) {
                diags.error(e, "E0406", "Enum entry uses an unknown constructor: " + entryNamed);
            } else if (entryArgs != null && entryCtor != null) {
                entryArgCode[i] = "(" + canonicalArgs(entryCtor, syn, entryArgs, ctx) + ")";
            }
        }
        for (String imp : ctx.imports.values()) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!ctx.imports.isEmpty()) {
            sb.append('\n');
        }
        sb.append(dartRef(e)).append("\n");
        sb.append("public enum ").append(enumJava).append(" {\n    ");
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < e.entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
                names.append(", ");
            }
            sb.append(e.entries.get(i));
            if (entryArgCode[i] != null) {
                sb.append(entryArgCode[i]);
            }
            // the constant's simple name (enhanced enums carry `name(args)`)
            String entry = e.entries.get(i);
            int paren = entry.indexOf('(');
            names.append((paren >= 0 ? entry.substring(0, paren) : entry).trim());
        }
        // Dart's `EnumType.values` is a `List<EnumType>`; expose it as a DartList field
        // (coexisting with Java's implicit values() method) so `.values.idx(i)` resolves.
        sb.append(";\n\n");
        sb.append("    public static final dart.core.DartList<").append(enumJava)
                .append("> values = dart.core.DartList.<").append(enumJava).append(">of(")
                .append(names).append(");\n\n");
        // The Dart names, which a value whose name is a Java keyword (`native`)
        // does not keep as its Java constant (JavaKeywordEscaper renames it).
        StringBuilder dartNames = new StringBuilder();
        for (String nm : names.toString().split(", ")) {
            if (dartNames.length() > 0) {
                dartNames.append(", ");
            }
            dartNames.append('"').append(nm).append('"');
        }
        sb.append("    private static final String[] $dartNames = {").append(dartNames).append("};\n\n");
        sb.append("    public String name$() {\n        return $dartNames[ordinal()];\n    }\n\n");
        sb.append(body);
        // Dart's Enum.toString is "Mode.only" -- what print and string interpolation
        // show -- where Java's is the bare constant name; .name is still name().
        boolean ownToString = false;
        for (MethodDecl m : e.methods) {
            if ("toString".equals(m.name) && m.params.isEmpty() && !m.isStatic) {
                ownToString = true;
            }
        }
        if (!ownToString) {
            sb.append("\n    @Override\n    public String toString() {\n        return \"")
                    .append(e.name).append(".\" + name$();\n    }\n");
        }
        sb.append("}\n");
        return new GeneratedFile(enumJava + ".java", sb.toString());
    }

    /**
     * Whether a top-level variable must initialise LAZILY, on first read.
     *
     * <p>Dart initialises every top-level and static variable on first access,
     * so the order they are written in cannot matter. Java runs static
     * initialisers top to bottom, so the same source emitted as plain fields
     * silently reads a not-yet-assigned neighbour as null. Shrine's theme is
     * the shape of it:</p>
     *
     * <pre>
     *   final ThemeData shrineTheme = _buildShrineTheme();   // reads the scheme
     *   final ColorScheme _shrineColorScheme = ColorScheme(...);
     * </pre>
     *
     * <p>which threw {@code ExceptionInInitializerError} on the class's very
     * first use — the whole study failed to open. Only initialisers that
     * cannot depend on anything (a bare literal) stay plain fields, so the
     * common {@code const kPadding = 8.0} keeps reading as a constant.</p>
     */
    private static boolean isLazyTopLevel(FieldDecl v) {
        return v.initializer != null && !isSelfContainedLiteral(v.initializer);
    }

    /**
     * A class's static field initialised lazily, for the same reason as a top-level
     * one and more: Java initialises every static of a class the first time ANY of them
     * is touched, while Dart initialises each on its own first read. Reordering the
     * statics by dependency fixed the null-neighbour half and not this one -- reading
     * a harmless {@code C.ready} still ran an unrelated
     * {@code static final expensive = fail()} and threw, or ran side effects Dart would
     * have deferred until {@code expensive} was read.
     */
    /**
     * A {@code late} field with no initializer, which Dart checks: reading it before it is
     * assigned throws LateInitializationError, and so does assigning a {@code late final}
     * one twice. Emitted as a plain field it read as 0, false or null and could be
     * reassigned at will. Such a field is reached only through the get$/set$ pair
     * {@link #emitLateAccessors} writes, so every write form -- `=`, `op=`, `??=`, `++` --
     * goes through the existing accessor-pair lowering. Mixin fields already live behind
     * accessors of their own and are left as they are.
     */
    private static boolean isLateChecked(FieldDecl f, ClassDecl owner) {
        return f.isLate && f.initializer == null && !f.isConst && owner != null && !owner.isMixin;
    }

    private static String emitLateAccessors(FieldDecl f, String jt) {
        String st = f.isStatic ? "static " : "";
        String vis = f.isStatic && f.name.startsWith("_") ? "    " : "    public ";
        String flag = f.name + "$late";
        String self = f.isStatic ? "" : "this.";
        StringBuilder b = new StringBuilder();
        b.append("    private ").append(st).append("boolean ").append(flag).append(";\n");
        b.append(vis).append(st).append(jt).append(" get$").append(f.name).append("() {\n")
                .append("        if (!").append(flag).append(") {\n")
                .append("            throw dart.core.LateInitializationError.notInitialized(\"")
                .append(f.name).append("\");\n        }\n")
                .append("        return ").append(self).append(f.name).append(";\n    }\n");
        b.append(vis).append(st).append(jt).append(" set$").append(f.name).append("(").append(jt).append(" v) {\n");
        if (f.isFinal) {
            b.append("        if (").append(flag).append(") {\n")
                    .append("            throw dart.core.LateInitializationError.alreadyInitialized(\"")
                    .append(f.name).append("\");\n        }\n");
        }
        b.append("        ").append(flag).append(" = true;\n")
                .append("        ").append(self).append(f.name).append(" = v;\n")
                .append("        return v;\n    }\n");
        return b.toString();
    }

    /** A constructor's store into its own field: through set$ for a checked late field. */
    private static String fieldStore(ClassDecl c, String field, String value) {
        FieldDecl f = c.field(field);
        if (f != null && isLateChecked(f, c)) {
            return "this.set$" + field + "(" + value + ");";
        }
        return "this." + field + " = " + value + ";";
    }

    private static boolean isLazyStatic(FieldDecl f) {
        return f.isStatic && isLazyTopLevel(f);
    }

    /**
     * An initialiser that cannot throw when the getter runs it: a {@code const}
     * variable's (Dart evaluates it at compile time, where a throwing constant is a
     * compile error, and everything it reads is itself constant), or one
     * {@link #cannotThrow} accepts.
     */
    private static boolean initializerCannotThrow(FieldDecl v) {
        return v.isConst || cannotThrow(v.initializer);
    }

    /**
     * An expression that cannot throw: a constant, or a plain collection literal --
     * {@code <K, V>{}}, {@code [1, 2]} -- of such expressions. Anything that runs code
     * of its own (a call, a non-const constructor, a read that may be a lazy getter,
     * an operator) is assumed to be able to.
     */
    private static boolean cannotThrow(Expr e) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        if (e instanceof CtorCall) {
            return ((CtorCall) e).isConst;
        }
        if (e instanceof ListLit) {
            ListLit l = (ListLit) e;
            return l.isConst || allCannotThrow(l.elements);
        }
        if (e instanceof Ast.SetLit) {
            Ast.SetLit l = (Ast.SetLit) e;
            return l.isConst || allCannotThrow(l.elements);
        }
        if (e instanceof Ast.MapLit) {
            Ast.MapLit m = (Ast.MapLit) e;
            return m.isConst || (!m.structured && m.elements.isEmpty()
                    && allCannotThrow(m.keys) && allCannotThrow(m.values));
        }
        return isSelfContainedLiteral(e);
    }

    /** Every one of {@code es} is {@link #cannotThrow}; spreads, ifs and fors are not. */
    private static boolean allCannotThrow(List<Expr> es) {
        for (Expr x : es) {
            if (!cannotThrow(x)) {
                return false;
            }
        }
        return true;
    }

    /** A literal whose value cannot reference any other declaration. */
    private static boolean isSelfContainedLiteral(Expr e) {
        if (e instanceof Ast.IntLit || e instanceof Ast.DoubleLit
                || e instanceof Ast.BoolLit || e instanceof Ast.NullLit) {
            return true;
        }
        if (e instanceof Ast.StringLit) {
            for (Object part : ((Ast.StringLit) e).parts) {
                if (!(part instanceof String)) {
                    return false;   // interpolation can read anything
                }
            }
            return true;
        }
        if (e instanceof Ast.Unary) {
            return isSelfContainedLiteral(((Ast.Unary) e).operand);
        }
        return false;
    }

    /**
     * A top-level variable as a lazily-initialised accessor pair. Named
     * {@code get$x} / {@code set$x} so reads and writes route through the
     * emitter's existing accessor handling — {@code x = v} becomes
     * {@code Lib.set$x(v)} with no special case at the assignment site.
     */
    private String emitLazyTopLevel(FieldDecl v, TypeRef vt, String jt, Ctx ctx) {
        boolean cannotThrow = initializerCannotThrow(v);
        // Only the non-late throwing case wraps the initialiser in a try/finally (one
        // more indent level than the cannotThrow fast path); late re-entry runs the
        // initialiser inline with no guard at all, so it sits at that same, shallower
        // depth.
        boolean guardReentry = !cannotThrow && !v.isLate;
        ctx.pushWriter(guardReentry ? 4 : 3);
        Out init = emitExpr(v.initializer, vt, ctx);
        String lifted = ctx.popWriter();
        StringBuilder sb = new StringBuilder();
        sb.append("    private static ").append(jt).append(' ').append(v.name)
                .append("$value;\n");
        sb.append("    private static boolean ").append(v.name).append("$ready;\n");
        if (guardReentry) {
            // Non-late only: Dart treats a read of a top-level/static variable while its
            // own initialiser is still running as an error -- there is no `late` here to
            // fall back to re-running it. This flag is the only way to tell "never
            // started" apart from "currently running"; `ready` alone cannot, since it is
            // set only once the store below actually succeeds.
            sb.append("    private static boolean ").append(v.name).append("$initializing;\n");
        }
        sb.append('\n');
        sb.append("    /** Dart `").append(v.name)
                .append("` -- initialised on first read, as Dart does. */\n");
        sb.append("    public static ").append(jt).append(" get$").append(v.name)
                .append("() {\n");
        sb.append("        if (!").append(v.name).append("$ready) {\n");
        if (cannotThrow) {
            // A constant is evaluated by the Dart compiler, so it never throws at run
            // time and the reset below would be dead code. It is not free dead code:
            // ParparVM gives a method with a handler a full frame and an unwind
            // point, and the gallery's 190-odd constant getters grew the Linux
            // executable by 2% when every one carried it. A constant cannot read
            // itself either, so it needs no initializing flag.
            sb.append(lifted);
            sb.append("            ").append(v.name).append("$value = ")
                    .append(coerce(init, vt, ctx)).append(";\n");
            sb.append("            ").append(v.name).append("$ready = true;\n");
        } else if (v.isLate) {
            // `late`: Dart re-runs the initialiser on a re-entrant read instead of
            // throwing (spec), so every nested call stores into $value and marks $ready
            // on ITS OWN completion -- whichever call is outermost finishes last and
            // leaves the value that is actually kept. `late final` additionally forbids
            // a second store: if some inner call already bound the variable by the time
            // this one finishes evaluating, storing again would be a repeat assignment,
            // so it throws instead of silently overwriting.
            sb.append(lifted);
            sb.append("            ").append(jt).append(" $computed = ")
                    .append(coerce(init, vt, ctx)).append(";\n");
            if (v.isFinal) {
                sb.append("            if (").append(v.name).append("$ready) {\n");
                sb.append("                throw new dart.core.LateInitializationError(\"LateInitializationError: Field '")
                        .append(v.name).append("' has been assigned during initialization.\");\n");
                sb.append("            }\n");
            }
            sb.append("            ").append(v.name).append("$value = $computed;\n");
            sb.append("            ").append(v.name).append("$ready = true;\n");
        } else {
            // Non-late: a recursive read while $initializing is Dart's cyclic-initialiser
            // error, thrown BEFORE the initialiser runs again -- there is no zero value to
            // fall back to here, unlike the old (wrong) always-ready-first behaviour. An
            // initialiser that throws outright still leaves $ready false exactly as
            // before: nothing marks it until the store below runs, and $initializing is
            // reset in the finally so the next, unrelated read is free to retry.
            sb.append("            if (").append(v.name).append("$initializing) {\n");
            sb.append("                throw new dart.core.LateInitializationError(\"Reading static variable '")
                    .append(v.name).append("' during its initialization\");\n");
            sb.append("            }\n");
            sb.append("            ").append(v.name).append("$initializing = true;\n");
            sb.append("            try {\n");
            sb.append(lifted);
            sb.append("                ").append(v.name).append("$value = ")
                    .append(coerce(init, vt, ctx)).append(";\n");
            sb.append("                ").append(v.name).append("$ready = true;\n");
            sb.append("            } finally {\n");
            sb.append("                ").append(v.name).append("$initializing = false;\n");
            sb.append("            }\n");
        }
        sb.append("        }\n");
        sb.append("        return ").append(v.name).append("$value;\n");
        sb.append("    }\n\n");
        // Answers the value stored, as a Dart assignment expression does, so an
        // accessor write can stand where a value is needed (`x ??= v`, `y = x = v`).
        sb.append("    public static ").append(jt).append(" set$").append(v.name).append('(')
                .append(jt).append(" $v) {\n");
        sb.append("        ").append(v.name).append("$ready = true;\n");
        sb.append("        ").append(v.name).append("$value = $v;\n");
        sb.append("        return $v;\n");
        sb.append("    }\n\n");
        return sb.toString();
    }

    private GeneratedFile emitLibClass(Library lib) {
        Ctx ctx = new Ctx(null);
        ctx.currentLibrary = lib;
        String cls = Program.libClassName(lib.fileName);
        StringBuilder body = new StringBuilder();
        for (FieldDecl v : lib.topLevelVars) {
            TypeRef vt = fieldType(v, ctx);
            String jt = javaType(vt, false, ctx);
            if (isLazyTopLevel(v)) {
                body.append(emitLazyTopLevel(v, vt, jt, ctx));
                continue;
            }
            body.append("    public static ").append(jt).append(' ').append(v.name);
            if (v.initializer != null) {
                ctx.pushWriter(2);
                Out init = emitExpr(v.initializer, vt, ctx);
                String lifted = ctx.popWriter();
                if (lifted.isEmpty()) {
                    body.append(" = ").append(coerce(init, vt, ctx)).append(";\n");
                } else {
                    body.append(";\n\n    static {\n").append(lifted)
                            .append("        ").append(v.name).append(" = ")
                            .append(coerce(init, vt, ctx)).append(";\n    }\n");
                }
            } else {
                body.append(" = ").append(zeroValue(vt)).append(";\n");
            }
        }
        if (!lib.topLevelVars.isEmpty()) {
            body.append('\n');
        }
        for (FunctionDecl f : lib.functions) {
            Method m = new Method();
            m.isStatic = true;
            m.isAsync = f.isAsync;
            m.isSyncStar = f.isSyncStar;
            m.isAsyncStar = f.isAsyncStar;
            m.name = f.name.equals("main") ? "main$" : f.name;
            m.returnType = f.returnType;
            m.params = f.params;
            m.typeParams = f.typeParams;
            m.typeParamBounds = f.typeParamBounds;
            m.body = f.body;
            m.exprBody = f.exprBody;
            m.witnesses = methodWitnesses(f);
            body.append(emitMethodLike(m, ctx, false));
        }
        return finishClassFile(cls, "public final class " + cls, null, null,
                "    private " + cls + "() {\n    }\n\n" + body, ctx, lib.fileName);
    }

    /**
     * extension X on T { ... } — a final class of static methods whose first
     * parameter is the receiver ($self). Bodies see `this` as $self; bare
     * member CALLS are probed against the receiver type (intrinsics, stubs,
     * other extensions); bare PROPERTY access needs explicit `this.`.
     */
    private GeneratedFile emitExtension(ClassDecl ext) {
        Ctx ctx = new Ctx(null);
        ctx.currentLibrary = ext.ownerLibrary;
        ctx.extensionSelfType = ext.extensionOn;
        ctx.currentExtension = ext;
        StringBuilder body = new StringBuilder();
        body.append("    private ").append(ext.name).append("() {\n    }\n\n");
        for (MethodDecl m : ext.methods) {
            ctx.pushScope();
            TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
            StringBuilder sig = new StringBuilder();
            sig.append("    public static ");
            if (!ext.typeParams.isEmpty() || (m.typeParams != null && !m.typeParams.isEmpty())) {
                java.util.LinkedHashSet<String> tvs = new java.util.LinkedHashSet<String>(ext.typeParams);
                if (m.typeParams != null) {
                    tvs.addAll(m.typeParams);
                }
                sig.append('<').append(String.join(", ", tvs)).append("> ");
            }
            sig.append(m.isSetter ? "void" : javaType(rt, false, ctx))
                    .append(' ').append(m.name).append('(');
            if (!m.isStatic) {
                // A static extension member has no receiver: `Ext.helper(a)`.
                sig.append(javaType(ext.extensionOn, false, ctx)).append(" $self");
            }
            boolean firstParam = m.isStatic;
            for (Param pm : m.params) {
                TypeRef pt = pm.type == null || pm.type.is("var") ? TypeRef.DYNAMIC : pm.type;
                sig.append(firstParam ? "" : ", ");
                firstParam = false;
                sig.append(javaType(pt, false, ctx)).append(' ')
                        .append(ctx.declareShadowSafe(pm.name, pt));
            }
            sig.append(") {\n");
            body.append(sig);
            ctx.pushWriter(2);
            ctx.methodReturnType = rt;
            if (emitInlineGeneratorBody(m, rt, ctx)) {
                // generator: emitted as a lambda
            } else if (m.body != null) {
                emitStatements(m.body, ctx);
            } else if (m.exprBody != null) {
                if (rt.is("void")) {
                    // `void f() => expr;` runs expr for its effects, whatever its shape.
                    emitDiscardedValue(m.exprBody, ctx);
                } else {
                    Out o = emitExpr(m.exprBody, rt, ctx);
                    ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
                }
            }
            body.append(ctx.popWriter());
            ctx.methodReturnType = null;
            ctx.popScope();
            body.append("    }\n\n");
        }
        return finishClassFile(ext.name, "public final class " + ext.name, null, null, body, ctx, ext.file);
    }

    /**
     * The body of a {@code sync*} / {@code async*} member emitted where no private helper
     * method can sit beside it -- an extension's static method, a mixin's default method.
     * The generator body goes straight into the lambda DartIterable.syncStar /
     * AsyncStar.asyncStar run; parameters are captured, so the body must not assign them
     * (a Dart generator that reassigns a parameter is rare; javac reports it plainly).
     *
     * @return true when {@code m} is a generator and its body was emitted
     */
    private boolean emitInlineGeneratorBody(MethodDecl m, TypeRef rt, Ctx ctx) {
        if (m.isSetter || m.body == null || (!m.isSyncStar && !m.isAsyncStar)) {
            return false;
        }
        Ctx.Writer w = ctx.writer();
        String savedList = ctx.syncStarList;
        String savedSink = ctx.asyncStarSink;
        TypeRef savedElem = ctx.syncStarElem;
        boolean savedAsync = ctx.inAsyncBody;
        TypeRef savedRt = ctx.methodReturnType;
        String v = ctx.newTemp();
        if (m.isAsyncStar) {
            ctx.importClass("dart.async.AsyncStar");
            TypeRef elem = rt.is("Stream") && !rt.args.isEmpty() ? rt.arg(0) : TypeRef.DYNAMIC;
            String ej = javaType(elem, true, ctx);
            w.line("return AsyncStar.<" + ej + ">asyncStar((AsyncStar.Sink<" + ej + "> " + v + ") -> {");
            ctx.syncStarList = null;
            ctx.asyncStarSink = v;
            ctx.syncStarElem = elem;
            ctx.inAsyncBody = true;
        } else {
            ctx.importClass("dart.core.DartList");
            ctx.importClass("dart.core.DartIterable");
            TypeRef elem = rt.is("Iterable") && !rt.args.isEmpty() ? rt.arg(0) : TypeRef.DYNAMIC;
            String ej = javaType(elem, true, ctx);
            w.line("return DartIterable.syncStar((DartList<" + ej + "> " + v + ") -> {");
            ctx.syncStarList = v;
            ctx.asyncStarSink = null;
            ctx.syncStarElem = elem;
        }
        ctx.methodReturnType = TypeRef.VOID;
        ctx.indent(1);
        ctx.pushScope();
        emitStatements(m.body, ctx);
        ctx.popScope();
        ctx.indent(-1);
        w.line("});");
        ctx.syncStarList = savedList;
        ctx.asyncStarSink = savedSink;
        ctx.syncStarElem = savedElem;
        ctx.inAsyncBody = savedAsync;
        ctx.methodReturnType = savedRt;
        return true;
    }

    /**
     * mixin M { ... } — a Java interface with default methods; mixin fields
     * become abstract get$x/set$x accessor pairs that the applying class
     * synthesizes (Java interfaces hold no state).
     */
    private GeneratedFile emitMixin(ClassDecl mx) {
        Ctx ctx = new Ctx(mx);
        StringBuilder body = new StringBuilder();
        for (FieldDecl f : mx.fields) {
            TypeRef ft = fieldType(f, ctx);
            String jt = javaType(ft, false, ctx);
            body.append("    ").append(jt).append(" get$").append(f.name).append("();\n");
            body.append("    void set$").append(f.name).append('(').append(jt).append(" v);\n\n");
        }
        for (MethodDecl m : mx.methods) {
            if (m.isStatic) {
                // A Java interface takes static methods as they are (an `abstract mixin
                // class` with a private static helper is the common case).
                Method sm = new Method();
                sm.name = m.name;
                sm.isStatic = true;
                sm.isGetter = m.isGetter;
                sm.isSetter = m.isSetter;
                sm.isAsync = m.isAsync;
                sm.isSyncStar = m.isSyncStar;
                sm.isAsyncStar = m.isAsyncStar;
                sm.returnType = m.returnType;
                sm.params = m.params;
                sm.typeParams = m.typeParams;
                sm.typeParamBounds = m.typeParamBounds;
                sm.body = m.body;
                sm.exprBody = m.exprBody;
                sm.witnesses = methodWitnesses(m);
                body.append(emitMethodLike(sm, ctx, false));
                continue;
            }
            ctx.pushScope();
            TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
            // An abstract member the applying class implements -- possibly with a field,
            // through the bridge implementerBridges writes -- throws if nothing does.
            boolean abstractMember = m.body == null && m.exprBody == null;
            // A generic member keeps its own type parameters and witnesses here as it does
            // in a class: `whereType<T>() => inner.whereType<T>()` in a class used as a
            // mixin takes the runtime's (Class, boolean) pair and filters by T.
            java.util.Set<String> savedTps = ctx.methodTypeParams;
            java.util.Set<String> savedWl = ctx.witnessLocals;
            java.util.Map<String, TypeRef> savedBounds = ctx.methodTypeParamBounds;
            List<String> mws = m.typeParams == null || m.typeParams.isEmpty()
                    ? java.util.Collections.<String>emptyList() : methodWitnesses(m);
            boolean wtOverride = isWhereTypeOverride(m) && mws.isEmpty();
            StringBuilder lead = new StringBuilder();
            if (m.typeParams != null && !m.typeParams.isEmpty()) {
                ctx.methodTypeParams = new HashSet<String>(m.typeParams);
                ctx.methodTypeParamBounds = m.typeParamBounds;
                ctx.witnessLocals = mws.isEmpty() ? null : new HashSet<String>(mws);
                if (wtOverride) {
                    String tw = m.typeParams.get(0);
                    ctx.witnessLocals = new HashSet<String>(m.typeParams);
                    lead.append("java.lang.Class<").append(tw).append("> $twc$").append(tw)
                            .append(", boolean $twn$").append(tw);
                }
                for (String tw : mws) {
                    lead.append(lead.length() == 0 ? "" : ", ").append("dart.runtime.TypeWitness $tw$").append(tw);
                }
            }
            body.append("    default ");
            if (m.typeParams != null && !m.typeParams.isEmpty()) {
                body.append('<').append(String.join(", ", m.typeParams)).append("> ");
            }
            body.append(m.isSetter ? "void" : javaType(rt, false, ctx))
                    .append(' ').append(m.isGetter ? m.name : m.name).append('(').append(lead);
            for (int i = 0; i < m.params.size(); i++) {
                Param pm = m.params.get(i);
                TypeRef pt = pm.type == null || pm.type.is("var") ? TypeRef.DYNAMIC : pm.type;
                if (i > 0 || lead.length() > 0) {
                    body.append(", ");
                }
                body.append(javaType(pt, false, ctx)).append(' ')
                        .append(ctx.declareShadowSafe(pm.name, pt));
            }
            body.append(") {\n");
            if (wtOverride) {
                String tw = m.typeParams.get(0);
                body.append("        dart.runtime.TypeWitness $tw$").append(tw)
                        .append(" = dart.runtime.TypeWitness.of($twc$").append(tw).append(", $twn$")
                        .append(tw).append(");\n");
            }
            ctx.pushWriter(2);
            ctx.methodReturnType = rt;
            ctx.inAsyncBody = m.isAsync;
            if (abstractMember) {
                ctx.writer().line("throw new UnsupportedOperationException(\"" + mx.name + "." + m.name + "\");");
            } else if (emitInlineGeneratorBody(m, rt, ctx)) {
                // generator: emitted as a lambda
            } else if (m.body != null) {
                emitStatements(m.body, ctx);
            } else if (m.exprBody != null) {
                if (rt.is("void")) {
                    // `void f() => expr;` runs expr for its effects, whatever its shape.
                    emitDiscardedValue(m.exprBody, ctx);
                } else {
                    Out o = emitExpr(m.exprBody, rt, ctx);
                    ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
                }
            }
            body.append(ctx.popWriter());
            ctx.methodReturnType = null;
            ctx.inAsyncBody = false;
            ctx.methodTypeParams = savedTps;
            ctx.witnessLocals = savedWl;
            ctx.methodTypeParamBounds = savedBounds;
            ctx.popScope();
            body.append("    }\n\n");
        }
        for (java.util.Map.Entry<String, Integer> sb : onMixinSupers(mx).entrySet()) {
            body.append("    Object ").append(superBridgeSignature(mx, sb.getKey(), sb.getValue())).append(";\n\n");
        }
        String typeParams = mx.typeParams.isEmpty() ? "" : "<" + String.join(", ", mx.typeParams) + ">";
        StringBuilder mext = new StringBuilder();
        List<TypeRef> mxSupers = new ArrayList<TypeRef>(mx.interfaces);
        mxSupers.addAll(mx.mixinOn);
        for (TypeRef itf : mxSupers) {
            ClassDecl im = superDeclOf(mx, itf);
            if (im != null && interfacedClasses().contains(im)) {
                // `mixin M implements Shape`: M's values are Shapes
                mext.append(mext.length() > 0 ? ", " : "").append(javaType(itf, false, ctx));
            }
        }
        return finishClassFile(mx.name, "public interface " + mx.name + typeParams,
                mext.length() == 0 ? null : mext.toString(), null, body, ctx, mx.file);
    }

    private GeneratedFile emitClass(ClassDecl c) {
        Ctx ctx = new Ctx(c);
        StringBuilder body = new StringBuilder();

        // fields — Dart initializes statics lazily and order-independently, but Java runs
        // static field initializers top-to-bottom, so a static whose initializer reads a
        // later static reads null. Reorder statics so dependencies initialize first.
        for (FieldDecl f : orderStaticFieldsByDependency(c)) {
            TypeRef ft = fieldType(f, ctx);
            String jt = javaType(ft, false, ctx);
            if (isLazyStatic(f)) {
                body.append(emitLazyTopLevel(f, ft, jt, ctx));
                continue;
            }
            // Instance fields are private (accessed via get$/set$ accessors). Static fields
            // are read directly as ClassName.field with no accessor, so a public Dart static
            // (no leading underscore) must be public here; a library-private (_x) static must
            // be package-private so sibling classes in the same generated package can reach it.
            if (f.isStatic) {
                body.append(f.name.startsWith("_") ? "    static " : "    public static ");
            } else {
                body.append("    private ");
            }
            if ((f.isFinal || f.isConst) && f.initializer != null) {
                body.append("final ");
            }
            body.append(jt).append(' ').append(f.name);
            if (f.initializer != null) {
                ctx.pushWriter(2);
                Out init = emitExpr(f.initializer, ft, ctx);
                String lifted = ctx.popWriter();
                if (lifted.isEmpty()) {
                    body.append(" = ").append(coerce(init, ft, ctx)).append(";\n");
                } else {
                    // complex initializer (e.g. named-arg constructor): move it
                    // into an initializer block, which runs for every ctor
                    body.append(";\n\n    ").append(f.isStatic ? "static {" : "{").append('\n');
                    body.append(lifted);
                    body.append("        ").append(f.isStatic ? "" : "this.").append(f.name)
                            .append(" = ").append(coerce(init, ft, ctx)).append(";\n");
                    body.append("    }\n");
                }
            } else {
                body.append(";\n");
            }
            if (isLateChecked(f, c)) {
                body.append(emitLateAccessors(f, jt));
                body.append('\n');
                continue;
            }
            // Accessors for instance fields. A Dart library-private (`_x`) field is still
            // reachable from sibling classes in the same library, so emit its accessor
            // package-private (all generated classes share one package) rather than skip it;
            // cross-instance reads compile to `x.get$_field()`.
            if (!f.isStatic) {
                // Public even for a library-private `_x`: an implicit interface (X$I) carries
                // the accessor, so `other._x` reaches it through an interface-typed value,
                // and an implementer's own `_x` must not assign weaker access.
                String vis = "    public ";
                body.append(vis).append(jt).append(" get$").append(f.name).append("() {\n")
                        .append("        return ").append(f.name).append(";\n    }\n");
                if (!f.isFinal && !f.isConst) {
                    body.append(vis).append("void set$").append(f.name).append("(").append(jt).append(" v) {\n")
                            .append("        this.").append(f.name).append(" = v;\n    }\n");
                }
            }
            body.append('\n');
        }

        for (String tw : witnessParams(c)) {
            body.append("    public dart.runtime.TypeWitness ").append(witnessField(c, tw)).append(";\n");
        }
        ClassDecl twSuper = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        boolean implicitWitnessCtor = c.ctors.isEmpty()
                && (!witnessParams(c).isEmpty() || (twSuper != null && !witnessParams(twSuper).isEmpty()));
        // constructors
        if (needsMarker(c)) {
            body.append("    /** Marker distinguishing named-constructor instantiation. */\n");
            // Package-private, not private: a subclass's `super.named(..)` runs this pair.
            body.append("    static final class $NamedCtor {\n        private $NamedCtor() {\n        }\n    }\n\n");
            // A subclass's named constructor builds through this chain and then runs the
            // superclass constructor it names as an $init$ method, so the chain must not
            // run any constructor body itself.
            ClassDecl markerSuper = c.superclass != null ? superDeclOf(c, c.superclass) : null;
            body.append("    ").append(javaClassName(c)).append("($NamedCtor $marker) {\n");
            if (markerSuper != null) {
                body.append("        super((").append(javaClassName(markerSuper)).append(".$NamedCtor) null);\n");
            }
            body.append("    }\n\n");
            if (c.ctors.isEmpty() && !implicitWitnessCtor) {
                // Declaring the marker constructor removes Java's implicit default one,
                // which a class with no Dart constructor still needs.
                body.append("    public ").append(javaClassName(c)).append("() {\n    }\n\n");
            }
            if (isSuperOfMarker(c)) {
                body.append(emitUnnamedInit(c, ctx));
            }
        }
        for (CtorDecl ct : c.ctors) {
            body.append(emitCtor(c, ct, ctx));
        }
        if (implicitWitnessCtor) {
            // The implicit constructor, which takes (and passes on) the hidden witnesses.
            body.append("    public ").append(javaClassName(c)).append('(')
                    .append(stripTrailingComma(witnessParamDecls(c))).append(") {\n");
            java.util.Set<String> savedLocals = ctx.witnessLocals;
            ctx.witnessLocals = new HashSet<String>(witnessParams(c));
            Ctx.Writer iw = ctx.pushWriter(2);
            if (twSuper != null && !witnessParams(twSuper).isEmpty()) {
                iw.line("super(" + stripTrailingComma(witnessArgs(twSuper, c.superclass.args, c, ctx)) + ");");
            }
            storeWitnesses(c, "this", true, false, iw, ctx);
            body.append(ctx.popWriter());
            ctx.witnessLocals = savedLocals;
            body.append("    }\n\n");
        }

        // Dart operator== ($eq) overrides Java equals via a bridge
        MethodDecl eqOp = c.method("$eq");
        if (eqOp != null && eqOp.params.size() == 1) {
            String otherType = javaType(eqOp.params.get(0).type == null
                    ? TypeRef.DYNAMIC : eqOp.params.get(0).type, true, ctx);
            body.append("    @Override\n    public boolean equals(Object $o) {\n")
                    .append("        return $o instanceof ").append(otherType)
                    .append(" && $eq((").append(otherType).append(") $o);\n    }\n\n");
        }

        // methods
        for (MethodDecl m : c.methods) {
            Method mm = new Method();
            mm.name = m.name;
            mm.isStatic = m.isStatic;
            mm.isGetter = m.isGetter;
            mm.isSetter = m.isSetter;
            mm.isOverride = javaOverrides(c, m);
            mm.isAbstract = m.isAbstract;
            mm.isAsync = m.isAsync;
            mm.isSyncStar = m.isSyncStar;
            mm.isAsyncStar = m.isAsyncStar;
            mm.returnType = m.returnType;
            // Dart lets a value-returning method override a void one; Java forbids it, so pin the
            // override's return to void to keep it a valid override.
            if (overriddenReturnsVoid(c, m)) {
                mm.returnType = TypeRef.VOID;
            }
            mm.params = m.params;
            mm.typeParams = m.typeParams;
            mm.typeParamBounds = m.typeParamBounds;
            mm.body = m.body;
            mm.exprBody = m.exprBody;
            mm.witnesses = methodWitnesses(m);
            if (!c.isMixin) {
                mm.javaReturn = interfacePinnedReturn(c, m, ctx);
            }
            body.append(emitMethodLike(mm, ctx, c.isAbstract));
            if (!c.isMixin) {
                body.append(interfaceBridges(c, m, ctx));
            }
            if (m.isOverride && !mm.isOverride && !c.isMixin) {
                body.append(emitCovariantBridge(c, m, mm, ctx));
            }
        }

        if (!c.isMixin) {
            checkInterfaceCoverage(c);
        }
        // mixin applications: implement each mixin interface and synthesize
        // the state (field + accessors) the mixin's abstract accessors need
        StringBuilder impls = new StringBuilder();
        TypeRef mixinClassAsSuper = null;
        for (TypeRef mixRef : c.mixins) {
            ClassDecl mx = program.classes.get(mixRef.name);
            if (mx != null && stubs.isStubClass(mixRef.name) && !classVisibleFrom(mx, c.ownerLibrary)) {
                // An unrelated package class that shares a runtime mixin's name (a package's
                // own ChangeNotifier) is not the one this library mixes in.
                mx = null;
            }
            if (mx != null && !mx.isMixin && c.superclass == null && mixinClassAsSuper == null
                    && mixRef == c.mixins.get(0) && mx.superclass == null && mx.mixins.isEmpty()) {
                // `class S with M` where M is a `mixin class` (or any class with no
                // superclass): Object with M is exactly extending M, so it is the superclass.
                mixinClassAsSuper = mixRef;
                continue;
            }
            if (mx == null || !mx.isMixin) {
                // A mixin supplied by the hand-written runtime (a stub) maps to a
                // Java interface (its @JavaName) with default-method behaviour and no
                // synthesized state; the applying class simply implements it. Bare
                // calls to the mixin's members resolve through emitBareCall.
                if (stubs.isStubClass(mixRef.name)) {
                    if (impls.length() > 0) {
                        impls.append(", ");
                    }
                    impls.append(javaType(mixRef, false, ctx));
                    emitChangeNotifierState(mixRef, body);
                    emitMixinToString(c, mixRef, body);
                    continue;
                }
                diags.error(c, "E0402", "Unknown mixin: " + mixRef.name);
                continue;
            }
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append(javaType(mixRef, false, ctx));
            body.append(implementerBridges(c, mx, ctx));
            // The applying class holds the mixin's state, so a generic mixin's field is
            // declared with the type arguments it is applied with (`with Holder<String>`
            // holds a String); applied raw, with dynamic, as Dart instantiates to bounds.
            Map<String, TypeRef> mixSubst = new LinkedHashMap<String, TypeRef>();
            for (int i = 0; i < mx.typeParams.size(); i++) {
                mixSubst.put(mx.typeParams.get(i), mixRef.args.size() == mx.typeParams.size()
                        ? mixRef.args.get(i) : TypeRef.DYNAMIC);
            }
            for (FieldDecl f : mx.fields) {
                TypeRef ft = mixSubst.isEmpty() ? fieldType(f, ctx)
                        : substituteTypeParams(fieldType(f, ctx), mixSubst);
                String jt = javaType(ft, false, ctx);
                body.append("    private ").append(jt).append(' ').append(f.name);
                if (f.initializer != null) {
                    ctx.pushWriter(2);
                    Out init = emitExpr(f.initializer, ft, ctx);
                    String lifted = ctx.popWriter();
                    if (lifted.isEmpty()) {
                        body.append(" = ").append(coerce(init, ft, ctx));
                    } else {
                        diags.error(f, "E0403", "Complex mixin field initializers are not supported yet");
                    }
                }
                body.append(";\n");
                body.append("    public ").append(jt).append(" get$").append(f.name)
                        .append("() {\n        return ").append(f.name).append(";\n    }\n");
                body.append("    public void set$").append(f.name).append('(').append(jt)
                        .append(" v) {\n        this.").append(f.name).append(" = v;\n    }\n\n");
                if (!mixSubst.isEmpty()) {
                    // The mixin interface declares set$x over its type parameter, which the
                    // primitive storage classes (List<int> is a DartLongList) do not match:
                    // the interface's own spelling, with the arguments substituted, is
                    // implemented too.
                    String generic = javaType(fieldType(f, ctx), false, ctx);
                    for (Map.Entry<String, TypeRef> en : mixSubst.entrySet()) {
                        generic = generic.replaceAll("\\b" + java.util.regex.Pattern.quote(en.getKey()) + "\\b",
                                java.util.regex.Matcher.quoteReplacement(javaType(en.getValue(), true, ctx)));
                    }
                    if (!generic.equals(jt) && !generic.contains("?")) {
                        body.append("    public void set$").append(f.name).append('(').append(generic)
                                .append(" v) {\n        this.").append(f.name).append(" = (").append(jt)
                                .append(") v;\n    }\n\n");
                    }
                }
            }
        }
        // Dart `implements X` clauses (c.interfaces): the runtime interface the class satisfies
        // (e.g. `implements Iterator<T>` / `PreferredSizeWidget`). Emitted as Java `implements`.
        TypeRef implementedAsSuper = null;
        // `with A, B` where both mixins implement the same method: Java rejects a class that
        // inherits two default methods with one signature, and Dart takes the last mixin's.
        // The last one's declaration is emitted into the applying class itself.
        if (!c.isMixin) {
            Map<String, MethodDecl> lastImpl = new LinkedHashMap<String, MethodDecl>();
            Map<String, Integer> declaredBy = new LinkedHashMap<String, Integer>();
            for (TypeRef mixRef : c.mixins) {
                ClassDecl mx = program.classes.get(mixRef.name);
                if (mx == null || !mx.isMixin || !mixRef.args.isEmpty()) {
                    continue;
                }
                for (MethodDecl m : mx.methods) {
                    if (m.isStatic || m.isGetter || m.isSetter || m.name.startsWith("$")
                            || (m.body == null && m.exprBody == null)) {
                        continue;
                    }
                    Integer k = declaredBy.get(m.name);
                    declaredBy.put(m.name, k == null ? 1 : k + 1);
                    lastImpl.put(m.name, m);
                }
            }
            for (Map.Entry<String, MethodDecl> en : lastImpl.entrySet()) {
                if (declaredBy.get(en.getKey()) > 1 && c.method(en.getKey()) == null
                        && !usesSuper(en.getValue())) {
                    // a body calling super must stay in its mixin, where super reaches the
                    // mixin before it (emitMixinForwarders delegates to it instead)
                    MethodDecl m = en.getValue();
                    Method mm = new Method();
                    mm.name = m.name;
                    mm.isOverride = true;
                    mm.isAsync = m.isAsync;
                    mm.isSyncStar = m.isSyncStar;
                    mm.isAsyncStar = m.isAsyncStar;
                    mm.returnType = m.returnType == null ? TypeRef.DYNAMIC : m.returnType;
                    mm.params = m.params;
                    mm.typeParams = m.typeParams;
                    mm.typeParamBounds = m.typeParamBounds;
            mm.typeParamBounds = m.typeParamBounds;
                    mm.body = m.body;
                    mm.exprBody = m.exprBody;
                    mm.witnesses = methodWitnesses(m);
                    body.append(emitMethodLike(mm, ctx, false));
                }
            }
        }
        for (TypeRef itf : c.interfaces) {
            // `implements Exception` / `implements Error`: dart:core's are Java classes here.
            // With no superclass of its own the class extends it (which is what makes it
            // throwable); otherwise the clause adds nothing a Java class can express.
            // The same holds for every other dart:core error type implemented as an interface
            // (`implements FormatException`): extend it when the class has no superclass.
            if ((itf.is("Exception") || itf.is("Error") || CORE_ERRORS.containsKey(itf.name))
                    && !program.classes.containsKey(itf.name)) {
                String jc = CORE_ERRORS.get(itf.name);
                if (jc != null && c.superclass == null && implementedAsSuper == null
                        && mixinClassAsSuper == null) {
                    ctx.importClass(jc);
                    implementedAsSuper = new TypeRef(jc.substring(jc.lastIndexOf('.') + 1));
                }
                continue;
            }
            ClassDecl implemented = superDeclOf(c, itf);
            if (implemented != null && !implemented.isMixin && interfacedClasses().contains(implemented)) {
                // Every class used in an `implements` clause has a Java interface X$I
                // (see interfacedClasses); javaType names it for this type position.
                if (impls.length() > 0) {
                    impls.append(", ");
                }
                // `implements Result<Never>`: Never is the bottom type, so the Dart class is a
                // Result<T> for every T. Java generics are invariant and have no bottom, so the
                // raw interface is what stays assignable to each parameterization.
                impls.append(argsMentionNever(itf) ? javaType(new TypeRef(itf.name), false, ctx)
                        : javaType(itf, false, ctx));
                body.append(implementerBridges(c, implemented, ctx));
                continue;
            }
            if (implemented != null && !implemented.isMixin) {
                // A program class is a Java class, which `implements` cannot name. One
                // that is purely an interface -- abstract, no state, no implemented
                // members -- is extended instead, which inherits nothing Dart's
                // `implements` would not. Anything else is reported rather than emitted
                // as Java that does not compile.
                if (c.superclass == null && implementedAsSuper == null && mixinClassAsSuper == null
                        && isPureInterface(implemented)) {
                    implementedAsSuper = itf;
                } else {
                    diags.error(c, "E0404", "Implementing the class " + itf.name
                            + " is not supported yet; a class can implement one concrete class"
                            + " only when it has no superclass of its own and the implemented"
                            + " class has none either");
                }
                continue;
            }
            String itfJava = javaType(itf, false, ctx);
            String itfRaw = itfJava.indexOf('<') < 0 ? itfJava : itfJava.substring(0, itfJava.indexOf('<'));
            String itfSimple = itfRaw.substring(itfRaw.lastIndexOf('.') + 1);
            // The element-specialised forms too: List<int> is DartLongList, Map<int, V> DartLongMap.
            if (itfSimple.matches("Dart(Long|Double)?(List|Set|Map|Iterable)")) {
                // dart:core's List/Set/Map/Iterable are runtime *classes* here, which a Java
                // `implements` cannot name. A class applying the family's mixin
                // (ListMixin, ...) already has the protocol from it; one with no
                // superclass of its own extends the runtime class instead, which is the
                // type every List-typed parameter and field is declared with.
                if (!appliesCollectionMixin(c) && c.superclass == null && implementedAsSuper == null
                        && mixinClassAsSuper == null) {
                    implementedAsSuper = itf;
                }
                continue;
            }
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append(itfJava);
            emitChangeNotifierState(itf, body);
        }
        // `class C with M` where `mixin class M implements I` and C's field implements I's
        // abstract getter: M calls I's getter spelling (label()), C's field answers get$label,
        // so C needs the same bridge it would get from implementing I itself.
        if (!c.isMixin) {
            java.util.List<TypeRef> ancestors = new java.util.ArrayList<TypeRef>(c.mixins);
            if (c.superclass != null) {
                ancestors.add(c.superclass);
            }
            java.util.Set<String> visited = new java.util.HashSet<String>();
            for (int ai = 0; ai < ancestors.size(); ai++) {
                ClassDecl anc = program.classes.get(ancestors.get(ai).name);
                if (anc == null || !visited.add(anc.name)) {
                    continue;
                }
                if (anc.superclass != null) {
                    ancestors.add(anc.superclass);
                }
                ancestors.addAll(anc.mixins);
                for (TypeRef aitf : anc.interfaces) {
                    ClassDecl ai2 = superDeclOf(anc, aitf);
                    if (ai2 == null || ai2.isMixin) {
                        continue;
                    }
                    for (String chunk : implementerBridges(c, ai2, ctx).split("\n\n")) {
                        String head = chunk.trim();
                        if (head.isEmpty()) {
                            continue;
                        }
                        String sig = head.substring(0, head.indexOf('\n') < 0 ? head.length() : head.indexOf('\n'));
                        if (body.indexOf(sig) < 0) {
                            body.append(chunk).append("\n\n");
                        }
                    }
                }
            }
        }
        // jsonEncode calls toJson() on what it cannot encode itself; Dart does that
        // dynamically, the runtime through this interface.
        MethodDecl toJson = c.method("toJson");
        if (toJson != null && !toJson.isStatic && toJson.params.isEmpty() && !c.isMixin
                && (toJson.returnType == null || !(toJson.returnType.is("int") || toJson.returnType.is("double")
                    || toJson.returnType.is("bool") || toJson.returnType.is("void")))) {
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append("dart.convert.JsonEncodable");
        }
        if (!c.isMixin && emitDynamicMembers(c, body, ctx)) {
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append("dart.runtime.DynamicObject");
        }
        body.append(emitStubGetterBridges(c, ctx));
        body.append(emitSinkAddErrorBridges(c, ctx));
        // Dart 3 sealed → Java sealed: a sealed class with subtypes lists them in a permits clause and
        // its direct subtypes are marked non-sealed. Falls back to a plain abstract class when the
        // hierarchy has no subtypes (a permits-less sealed class is illegal in Java).
        List<String> subtypes = directSubtypes(c.name);
        boolean sealedSelf = c.isSealed && !subtypes.isEmpty();
        String modifier = "";
        if (!c.isSealed) {
            List<TypeRef> supers = new ArrayList<TypeRef>(c.interfaces);
            if (c.superclass != null) {
                supers.add(c.superclass);
            }
            for (TypeRef sr : supers) {
                ClassDecl sup = superDeclOf(c, sr);
                if (sup != null && sup.isSealed && !directSubtypes(sup.name).isEmpty()) {
                    modifier = "non-sealed ";
                    break;
                }
            }
        }
        StringBuilder typeParamsSb = new StringBuilder();
        if (c.typeParams != null && !c.typeParams.isEmpty()) {
            typeParamsSb.append('<');
            for (int i = 0; i < c.typeParams.size(); i++) {
                if (i > 0) {
                    typeParamsSb.append(", ");
                }
                typeParamsSb.append(c.typeParams.get(i));
            }
            typeParamsSb.append('>');
        }
        String jname = javaClassName(c);
        String decl = "public " + modifier + (sealedSelf ? "sealed " : "")
                + (c.isAbstract ? "abstract " : "") + "class " + jname + typeParamsSb;
        String ext = null;
        if (c.superclass != null) {
            ext = javaClassType(c.superclass, ctx);
        } else if (mixinClassAsSuper != null) {
            ext = javaClassType(mixinClassAsSuper, ctx);
        } else if (implementedAsSuper != null) {
            ext = javaClassType(implementedAsSuper, ctx);
        }
        if (interfacedClasses().contains(c)) {
            impls.append(impls.length() > 0 ? ", " : "").append(jname).append("$I").append(typeParamsSb);
        }
        String permits = null;
        if (sealedSelf) {
            StringBuilder pb = new StringBuilder();
            for (int i = 0; i < subtypes.size(); i++) {
                if (i > 0) {
                    pb.append(", ");
                }
                pb.append(subtypes.get(i));
            }
            permits = pb.toString();
        }
        emitOnMixinSuperBridges(c, body, ctx);
        emitMixinForwarders(c, body, ctx);
        if (interfacedClasses().contains(c)) {
            pendingFiles.add(emitImplicitInterface(c, jname, typeParamsSb.toString(), body, ctx));
        }
        return finishClassFile(jname, decl, ext, impls.length() == 0 ? null : impls.toString(),
                permits, body, ctx, c.file);
    }

    private final List<GeneratedFile> pendingFiles = new ArrayList<GeneratedFile>();

    /** True while javaType renders a class (extends, constructor) rather than a type. */
    private boolean classRefMode;

    /** The Java class -- never its implicit interface -- a type names. */
    private String javaClassType(TypeRef t, Ctx ctx) {
        boolean saved = classRefMode;
        classRefMode = true;
        try {
            // Only the outer name is the class: type arguments are type positions.
            if (t.args.isEmpty()) {
                return javaType(t, false, ctx);
            }
            String outer = javaType(new TypeRef(t.name), false, ctx);
            // A bare core collection name maps with default arguments (Iterable ->
            // DartIterable<Object>); only its class name is wanted before t's own.
            int lt = outer.indexOf('<');
            if (lt > 0) {
                outer = outer.substring(0, lt);
            }
            classRefMode = saved;
            return outer + genericSuffix(t, ctx);
        } finally {
            classRefMode = saved;
        }
    }

    private java.util.Set<ClassDecl> interfaced;

    /**
     * Program classes that serve as a Dart implicit interface: named in an {@code implements}
     * clause (of a class or a mixin), plus their program superclasses, since a Dart interface
     * carries every inherited member. Each gets a Java interface {@code X$I} the class itself
     * implements; type positions name the interface and only constructor calls, statics and
     * {@code extends} name the class, so an implementer can have a superclass of its own and
     * implement any number of program classes.
     */
    private java.util.Set<ClassDecl> interfacedClasses() {
        if (interfaced == null) {
            java.util.Set<ClassDecl> out = new java.util.LinkedHashSet<ClassDecl>();
            List<ClassDecl> work = new ArrayList<ClassDecl>();
            List<List<ClassDecl>> groups = new ArrayList<List<ClassDecl>>(program.classesByName.values());
            groups.add(new ArrayList<ClassDecl>(program.classes.values()));
            for (List<ClassDecl> same : groups) {
                for (ClassDecl k : same) {
                    if (k.extensionOn != null) {
                        continue;
                    }
                    List<TypeRef> named = new ArrayList<TypeRef>(k.interfaces);
                    if (k.isMixin) {
                        // `mixin M on S`: M's code sees `this` as an S, whatever class applies it.
                        named.addAll(k.mixinOn);
                    }
                    for (TypeRef itf : named) {
                        ClassDecl im = superDeclOf(k, itf);
                        if (im != null && !im.isMixin && im.extensionOn == null && im.enumOf == null) {
                            work.add(im);
                        }
                    }
                }
            }
            while (!work.isEmpty()) {
                ClassDecl im = work.remove(work.size() - 1);
                if (!out.add(im)) {
                    continue;
                }
                ClassDecl sup = im.superclass != null ? superDeclOf(im, im.superclass) : null;
                if (sup != null && !sup.isMixin && sup.extensionOn == null) {
                    work.add(sup);
                }
            }
            interfaced = out;
        }
        return interfaced;
    }

    private static final java.util.regex.Pattern PUBLIC_MEMBER = java.util.regex.Pattern.compile(
            "^    public (?!static )(?:abstract )?(?:final )?(?:synchronized )?(.*?)\\s*(\\{|;)\\s*$");

    /**
     * {@code X$I}: the implicit interface of {@code c}. Its members are the class's own public
     * instance members -- field accessors, getters, setters, methods -- read from the emitted
     * body, so every signature is exactly the one the class declares. They are default
     * methods that throw: Dart lets an abstract implementer, or one with noSuchMethod, leave
     * members out, and Java must still accept the class.
     */
    private GeneratedFile emitImplicitInterface(ClassDecl c, String jname, String typeParams,
                                                CharSequence classBody, Ctx ctx) {
        StringBuilder body = new StringBuilder();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (String line : classBody.toString().split("\n")) {
            java.util.regex.Matcher m = PUBLIC_MEMBER.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String head = m.group(1).trim();
            int paren = head.indexOf('(');
            if (paren <= 0 || head.indexOf('=') >= 0 && head.indexOf('=') < paren || !head.endsWith(")")
                    && !head.contains(") throws ")) {
                continue;
            }
            String before = head.substring(0, paren).trim();
            int sp = before.lastIndexOf(' ');
            if (sp < 0) {
                continue; // a constructor
            }
            String name = before.substring(sp + 1);
            if (name.equals(jname) || name.startsWith("$") && !name.startsWith("$op")
                    || "equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                // A default method may not override a member of Object.
                continue;
            }
            if (!seen.add(head.replaceAll("\\s+", " "))) {
                continue;
            }
            body.append("    default ").append(head).append(" {\n")
                    .append("        throw new UnsupportedOperationException(\"")
                    .append(c.name).append('.').append(name).append("\");\n    }\n\n");
        }
        // A Dart field and a getter/setter implement each other; the Java spellings differ
        // (get$x/set$x against x()/x(v)), so the interface carries both and an implementer
        // overrides whichever it declares (implementerBridges forwards to the other).
        for (FieldDecl f : c.fields) {
            if (f.isStatic || f.name.startsWith("_") || c.getter(f.name) != null || c.method(f.name) != null) {
                continue;
            }
            String jt = javaType(fieldType(f, ctx), false, ctx);
            String gh = jt + " " + f.name + "()";
            if (seen.add(gh)) {
                body.append("    default ").append(gh).append(" {\n        return get$").append(f.name)
                        .append("();\n    }\n\n");
            }
            String sh = "void " + f.name + "(" + jt + " v)";
            if (!f.isFinal && seen.add(sh)) {
                body.append("    default ").append(sh).append(" {\n        set$").append(f.name)
                        .append("(v);\n    }\n\n");
            }
        }
        StringBuilder ext = new StringBuilder();
        java.util.Set<ClassDecl> mine = interfacedClasses();
        ClassDecl sup = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        if (sup != null && mine.contains(sup)) {
            ext.append(javaType(c.superclass, false, ctx));
        }
        for (TypeRef itf : c.interfaces) {
            ClassDecl im = superDeclOf(c, itf);
            if (im != null && mine.contains(im)) {
                ext.append(ext.length() > 0 ? ", " : "").append(javaType(itf, false, ctx));
            }
        }
        return finishClassFile(jname + "$I", "public interface " + jname + "$I" + typeParams,
                ext.length() == 0 ? null : ext.toString(), null, body, ctx, c.file);
    }

    /** True when c (or a superclass in the program) applies a dart:collection mixin or base. */
    private boolean appliesCollectionMixin(ClassDecl c) {
        for (ClassDecl k = c; k != null;
             k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            java.util.List<TypeRef> refs = new java.util.ArrayList<TypeRef>(k.mixins);
            if (k.superclass != null) {
                refs.add(k.superclass);
            }
            for (TypeRef r : refs) {
                String n = r.name;
                if (n != null && (n.endsWith("Mixin") || n.endsWith("Base"))
                        && (n.startsWith("List") || n.startsWith("Set") || n.startsWith("Map")
                        || n.startsWith("Iterable")) && !program.classes.containsKey(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Where Dart lets a getter/setter implement a field of the implemented class (or a field a
     * getter), the Java names differ -- a field is get$x/set$x, a getter x() -- so the
     * implementer gets the other spelling as a bridge.
     */
    private String implementerBridges(ClassDecl c, ClassDecl implemented, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> done = new java.util.HashSet<String>();
        for (ClassDecl im = implemented; im != null;
             im = im.superclass != null ? superDeclOf(im, im.superclass) : null) {
            for (FieldDecl f : im.fields) {
                if (f.isStatic || !done.add(f.name) || hasFieldInChain(c, f.name)) {
                    continue;
                }
                MethodDecl g = findGetterInChain(c, f.name);
                if (g != null) {
                    TypeRef rt = g.returnType == null ? TypeRef.DYNAMIC : g.returnType;
                    sb.append("    public ").append(javaType(rt, false, ctx)).append(" get$").append(f.name)
                            .append("() {\n        return ").append(f.name).append("();\n    }\n\n");
                }
                MethodDecl st = findSetterInChain(c, f.name);
                if (st != null && st.params.size() == 1) {
                    Param p = st.params.get(0);
                    TypeRef pt = p.type == null ? TypeRef.DYNAMIC : p.type;
                    sb.append("    public void set$").append(f.name).append('(').append(javaType(pt, false, ctx))
                            .append(" v) {\n        ").append(f.name).append("(v);\n    }\n\n");
                }
            }
            for (MethodDecl g : im.methods) {
                if (!g.isGetter || g.isStatic || !done.add(g.name) || findGetterInChain(c, g.name) != null) {
                    continue;
                }
                FieldDecl f = findFieldInChain(c, g.name);
                if (f != null) {
                    sb.append("    public ").append(javaType(fieldType(f, ctx), false, ctx)).append(' ')
                            .append(g.name).append("() {\n        return get$").append(g.name)
                            .append("();\n    }\n\n");
                }
            }
        }
        return sb.toString();
    }

    /** One member of an implicit interface X$I, as Java sees it from an implementer. */
    private static final class IfaceSig {
        String owner;
        String ret;
        List<String> params = new ArrayList<String>();
    }

    private static String erase(String t) {
        int lt = t.indexOf('<');
        return lt < 0 ? t : t.substring(0, lt);
    }

    private static String erasedParams(List<String> ps) {
        StringBuilder sb = new StringBuilder();
        for (String p : ps) {
            sb.append(erase(p)).append(',');
        }
        return sb.toString();
    }

    private static boolean bridgeable(MethodDecl m) {
        return bridgeable(m, false);
    }

    private static boolean bridgeable(MethodDecl m, boolean allowTypeParams) {
        if (m.isStatic || m.isSetter || m.name.startsWith("$") || (!allowTypeParams && !m.typeParams.isEmpty())
                || "hashCode".equals(m.name) || "compareTo".equals(m.name) || "toString".equals(m.name)) {
            return false;
        }
        for (Param p : m.params) {
            if (p.named || p.defaultValue != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * The X$I members {@code m} of {@code c} implements, with their Java types as the
     * implementer's {@code implements} clause specializes them (a bare type variable boxed,
     * the way Java's generic substitution sees it).
     */
    private List<IfaceSig> interfaceSigs(ClassDecl c, MethodDecl m, Ctx ctx) {
        List<IfaceSig> out = new ArrayList<IfaceSig>();
        if (m.isStatic || m.isSetter || m.name.startsWith("$")) {
            return out;
        }
        java.util.Set<ClassDecl> seen = new java.util.HashSet<ClassDecl>();
        for (TypeRef itf : c.interfaces) {
            collectIfaceSigs(c, itf, java.util.Collections.<String>emptyList(),
                    java.util.Collections.<TypeRef>emptyList(), m, ctx, out, seen);
        }
        return out;
    }

    private void collectIfaceSigs(ClassDecl owner, TypeRef itf, List<String> outerNames, List<TypeRef> outerArgs,
                                  MethodDecl m, Ctx ctx, List<IfaceSig> out, java.util.Set<ClassDecl> seen) {
        // `implements impl.ShelfBase`: the reference may still carry its import prefix.
        ClassDecl im = superDeclOf(owner, itf);
        if (im == null || !interfacedClasses().contains(im) || !seen.add(im)) {
            return;
        }
        List<TypeRef> args = new ArrayList<TypeRef>();
        for (TypeRef a : itf.args) {
            args.add(substTypeParams(a, outerNames, outerArgs));
        }
        for (MethodDecl sm : im.methods) {
            if (sm.isStatic || sm.isSetter || sm.isGetter != m.isGetter || !sm.name.equals(m.name)
                    || sm.params.size() != m.params.size() || sm.typeParams.size() != m.typeParams.size()) {
                continue;
            }
            IfaceSig sig = new IfaceSig();
            sig.owner = im.name;
            for (Param p : sm.params) {
                sig.params.add(ifaceJava(p.type, im, args, ctx));
            }
            TypeRef rt = sm.returnType == null || sm.returnType.is("var") ? TypeRef.DYNAMIC : sm.returnType;
            sig.ret = rt.is("void") ? "void" : ifaceJava(rt, im, args, ctx);
            out.add(sig);
        }
        if (im.superclass != null) {
            collectIfaceSigs(im, im.superclass, im.typeParams, args, m, ctx, out, seen);
        }
        for (TypeRef sup : im.interfaces) {
            collectIfaceSigs(im, sup, im.typeParams, args, m, ctx, out, seen);
        }
    }

    private String ifaceJava(TypeRef t, ClassDecl im, List<TypeRef> args, Ctx ctx) {
        TypeRef tt = t == null ? TypeRef.DYNAMIC : t;
        boolean bareVar = im.typeParams.contains(tt.name) && tt.args.isEmpty();
        return javaType(substTypeParams(tt, im.typeParams, args), bareVar, ctx);
    }

    private List<String> implJavaParams(MethodDecl m, Ctx ctx) {
        List<String> ps = new ArrayList<String>();
        for (Param p : m.params) {
            ps.add(javaType(p.type == null ? TypeRef.DYNAMIC : p.type, false, ctx));
        }
        return ps;
    }

    /**
     * The Java return type {@code m} must declare to override an X$I member whose Java return
     * cannot be covariantly narrowed to the Dart one: Dart's {@code int weight()} implements
     * {@code num weight()}, but Java's {@code long} does not implement {@code Number}, nor
     * {@code long value()} a {@code T value()} specialized to {@code Long}. Null when the
     * declared type serves.
     */
    private String interfacePinnedReturn(ClassDecl c, MethodDecl m, Ctx ctx) {
        // A generic method is never bridged, but its return can still be pinned.
        if (!bridgeable(m, true)) {
            return null;
        }
        TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
        String implRet = rt.is("void") ? "void" : javaType(rt, false, ctx);
        String implParams = erasedParams(implJavaParams(m, ctx));
        for (IfaceSig sig : interfaceSigs(c, m, ctx)) {
            if (implParams.equals(erasedParams(sig.params)) && !sig.ret.equals(implRet)
                    && (isJavaPrimitive(sig.ret) || isJavaPrimitive(implRet)) && !"void".equals(sig.ret)) {
                return sig.ret;
            }
            // `Future<Crate2<V>> open<V>()` implementing `Future<CrateBase<V>> open<V>()`: Dart's
            // generics are covariant, Java's are not, so the same raw type with narrower
            // arguments is no override. The interface's return is declared; callers that know
            // the implementer cast back through Object (pinnedCallBox).
            if (implParams.equals(erasedParams(sig.params)) && !sig.ret.equals(implRet)
                    && sig.ret.indexOf('<') > 0 && implRet.indexOf('<') > 0
                    && sig.ret.substring(0, sig.ret.indexOf('<')).equals(implRet.substring(0, implRet.indexOf('<')))) {
                return sig.ret;
            }
        }
        return null;
    }

    /**
     * At a call site: when the called method's Java return was pinned to an X$I member's
     * (see interfacePinnedReturn), the boxed Java type of its Dart return, which the caller
     * casts the result back to; null otherwise.
     */
    private String pinnedCallBox(ClassDecl pc, MethodDecl m, Ctx ctx) {
        for (ClassDecl k = pc; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            if (k.methods.contains(m)) {
                if (k.isMixin || interfacePinnedReturn(k, m, ctx) == null) {
                    return null;
                }
                TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
                return javaType(rt, true, ctx);
            }
        }
        return null;
    }

    private final java.util.Set<String> interfaceBridgesDone = new java.util.HashSet<String>();

    /**
     * Where the Java signature of an implementer's method differs from the X$I member it
     * implements -- a covariant parameter, {@code num} against {@code int}, a type variable
     * specialized to a primitive -- the method is only an overload, and a call through the
     * interface would reach X$I's throwing default. This emits the member's own signature,
     * converting each argument and delegating, so the implementer's code is always reached.
     */
    private String interfaceBridges(ClassDecl c, MethodDecl m, Ctx ctx) {
        List<IfaceSig> sigs = interfaceSigs(c, m, ctx);
        if (sigs.isEmpty()) {
            return "";
        }
        List<String> implParams = implJavaParams(m, ctx);
        String implErased = erasedParams(implParams);
        StringBuilder sb = new StringBuilder();
        for (IfaceSig sig : sigs) {
            String erased = erasedParams(sig.params);
            if (erased.equals(implErased)) {
                continue;
            }
            if (!bridgeable(m)) {
                diags.error(m, "E0405", c.name + "." + m.name + " implements " + sig.owner + "." + m.name
                        + " with a different signature, which cannot be bridged when the method"
                        + " has optional, named or type parameters");
                continue;
            }
            if (!interfaceBridgesDone.add(c.name + "#" + m.name + "(" + erased)) {
                continue;
            }
            StringBuilder ps = new StringBuilder();
            StringBuilder call = new StringBuilder();
            for (int i = 0; i < sig.params.size(); i++) {
                if (i > 0) {
                    ps.append(", ");
                    call.append(", ");
                }
                ps.append(sig.params.get(i)).append(" $b").append(i);
                call.append("((").append(implParams.get(i)).append(") $b").append(i).append(')');
            }
            sb.append("    @SuppressWarnings({\"unchecked\", \"rawtypes\"})\n    public ").append(sig.ret)
                    .append(' ').append(m.name).append('(').append(ps).append(") {\n        ")
                    .append("void".equals(sig.ret) ? "" : "return ").append(m.name).append('(').append(call)
                    .append(");\n    }\n\n");
        }
        return sb.toString();
    }

    /**
     * Fails translation when a concrete class leaves a member of an implemented program class
     * to X$I's throwing default: nothing the class or its program superclasses and mixins
     * declare carries the name. A class with {@code noSuchMethod}, or a superclass outside the
     * program (whose members cannot be seen here), is not checked.
     */
    private void checkInterfaceCoverage(ClassDecl c) {
        if (c.isAbstract || c.isMixin || c.interfaces.isEmpty()) {
            return;
        }
        java.util.Set<String> have = new java.util.HashSet<String>();
        for (ClassDecl k = c; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            if (k.superclass != null && superDeclOf(k, k.superclass) == null) {
                return;
            }
            List<ClassDecl> parts = new ArrayList<ClassDecl>();
            parts.add(k);
            for (TypeRef mx : k.mixins) {
                ClassDecl md = program.classes.get(mx.name);
                if (md == null) {
                    return;
                }
                parts.add(md);
            }
            for (ClassDecl part : parts) {
                for (MethodDecl md : part.methods) {
                    if ("noSuchMethod".equals(md.name)) {
                        return;
                    }
                    have.add(md.name);
                }
                for (FieldDecl f : part.fields) {
                    have.add(f.name);
                }
            }
        }
        java.util.Set<ClassDecl> seen = new java.util.HashSet<ClassDecl>();
        List<ClassDecl> work = new ArrayList<ClassDecl>();
        for (TypeRef itf : c.interfaces) {
            ClassDecl im = superDeclOf(c, itf);
            if (im != null && interfacedClasses().contains(im)) {
                work.add(im);
            }
        }
        while (!work.isEmpty()) {
            ClassDecl im = work.remove(work.size() - 1);
            if (!seen.add(im)) {
                continue;
            }
            for (MethodDecl sm : im.methods) {
                // Object's members (hashCode, toString, ==, noSuchMethod) every class has.
                if (!sm.isStatic && !sm.name.startsWith("$") && !have.contains(sm.name)
                        && !"hashCode".equals(sm.name) && !"toString".equals(sm.name)
                        && !"noSuchMethod".equals(sm.name) && !"runtimeType".equals(sm.name)
                        // Another library's private member is not part of the interface Dart
                        // makes an implementer provide.
                        && !(sm.name.startsWith("_") && !java.util.Objects.equals(c.file, im.file))) {
                    diags.error(c, "E0405", c.name + " implements " + im.name + " but has no member "
                            + sm.name + "; a call through " + im.name + " would not reach it");
                }
            }
            List<TypeRef> ups = new ArrayList<TypeRef>(im.interfaces);
            if (im.superclass != null) {
                ups.add(im.superclass);
            }
            for (TypeRef up : ups) {
                ClassDecl u = program.classes.get(up.name);
                if (u != null && interfacedClasses().contains(u)) {
                    work.add(u);
                }
            }
        }
    }

    private FieldDecl findFieldInChain(ClassDecl c, String name) {
        for (ClassDecl k = c; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            FieldDecl f = k.field(name);
            if (f != null && !f.isStatic) {
                return f;
            }
        }
        return null;
    }

    private boolean hasFieldInChain(ClassDecl c, String name) {
        return findFieldInChain(c, name) != null;
    }

    private MethodDecl findGetterInChain(ClassDecl c, String name) {
        for (ClassDecl k = c; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            MethodDecl g = k.getter(name);
            if (g != null && !g.isStatic) {
                return g;
            }
        }
        return null;
    }

    private MethodDecl findSetterInChain(ClassDecl c, String name) {
        for (ClassDecl k = c; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            for (MethodDecl m : k.methods) {
                if (m.isSetter && !m.isStatic && m.name.equals(name)) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * The {@code $dget}/{@code $dset}/{@code $dcall} members through which dart.runtime.Dyn
     * reaches this class's fields, getters and methods from a {@code dynamic} receiver.
     * Each falls back to the superclass's when that is a program class, and answers
     * {@code Dyn.NO_MEMBER} otherwise. A method is reachable when it is an instance method
     * with no type parameters; named and optional parameters are filled from the call.
     *
     * @return whether this class declares the interface itself (no program superclass)
     */
    private boolean emitDynamicMembers(ClassDecl c, StringBuilder body, Ctx ctx) {
        boolean inherits = c.superclass != null && superDeclOf(c, c.superclass) != null
                && !superDeclOf(c, c.superclass).isMixin;
        // A class extending a framework or runtime class (a widget, a State, a
        // BoxPainter, a RestorableProperty) is not the model data dynamic code reaches
        // into, and its overrides of stub methods have no return type of their own to
        // dispatch with; leave it out. A pure value base -- Equatable -- is the
        // exception: `dynamic pet; pet.name` on a `Pet extends Equatable` is model data.
        if (c.superclass != null && !inherits && !MODEL_BASES.contains(c.superclass.name)) {
            return false;
        }
        if (inherits && !hasDynamicMembers(superDeclOf(c, c.superclass))) {
            return false;
        }
        TypeRef self = new TypeRef(c.name);
        for (String tp : c.typeParams) {
            self.args.add(new TypeRef(tp));
        }
        Out thisOut = new Out("this", self);
        java.util.Set<String> seen = new java.util.HashSet<String>();
        StringBuilder get = new StringBuilder();
        StringBuilder set = new StringBuilder();
        StringBuilder call = new StringBuilder();
        for (FieldDecl f : c.fields) {
            if (f.isStatic || !seen.add(f.name)) {
                continue;
            }
            get.append("            ").append(DYN_CASE).append("case \"").append(f.name).append("\": return this.get$").append(f.name)
                    .append("();\n");
            if (!f.isFinal && !f.isConst) {
                TypeRef ft = fieldType(f, ctx);
                set.append("            ").append(DYN_CASE).append("case \"").append(f.name).append("\": this.set$").append(f.name).append('(')
                        .append(dynCast("v", ft, ctx)).append("); return true;\n");
            }
        }
        for (MethodDecl m : c.methods) {
            if (m.isStatic || m.name.startsWith("$")) {
                continue;
            }
            if (m.isSetter) {
                // `dynamic a = A(); a.value = 3` reaches an explicit setter as it would a field.
                if (m.params.size() == 1 && seen.add("set " + m.name)) {
                    TypeRef pt = m.params.get(0).type == null || m.params.get(0).type.is("var")
                            ? TypeRef.DYNAMIC : m.params.get(0).type;
                    set.append("            ").append(DYN_CASE).append("case \"").append(m.name).append("\": this.")
                            .append(javaMethodName(m.name)).append('(').append(dynCast("v", pt, ctx))
                            .append("); return true;\n");
                }
                continue;
            }
            // A generic method called dynamically gets no type arguments (Dart infers
            // dynamic), so an argument typed by one of its type parameters is passed erased:
            // T as Object, List<T> as a raw list -- the parameters are not in scope here.
            java.util.Set<String> methodTypeParams = new java.util.HashSet<String>(m.typeParams);
            if (m.isGetter) {
                if (seen.add(m.name)) {
                    get.append("            ").append(DYN_CASE).append("case \"").append(m.name).append("\": return this.")
                            .append(javaMethodName(m.name)).append("();\n");
                }
                continue;
            }
            if (!seen.add("()" + m.name)) {
                continue;
            }
            StringBuilder args = new StringBuilder();
            int positional = 0;
            for (Param p : m.params) {
                if (!p.named) {
                    positional++;
                }
            }
            int pi = 0;
            for (Param p : m.params) {
                if (args.length() > 0) {
                    args.append(", ");
                }
                TypeRef pt = p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
                String raw = p.named ? "(n == null ? null : n.idx(\"" + p.name + "\"))"
                        : "(a.length > " + pi + " ? a[" + pi + "] : null)";
                String cast;
                if (mentionsAny(pt, methodTypeParams)) {
                    cast = pt.args.isEmpty() && methodTypeParams.contains(pt.name) ? raw
                            : "(" + erasedJavaType(pt, ctx) + ") dart.runtime.DartRuntime.as(" + raw + ", "
                                    + erasedJavaType(pt, ctx) + ".class, " + pt.nullable + ", "
                                    + quote(pt.toString()) + ")";
                } else {
                    cast = dynCast(raw, pt, ctx);
                }
                if (p.defaultValue != null && !mentionsAny(pt, methodTypeParams)) {
                    // An omitted argument takes the declared default, as a static call
                    // would: null would be the wrong value, and fails unboxing an int.
                    String present = p.named ? "(n != null && n.containsKey(\"" + p.name + "\"))"
                            : "(a.length > " + pi + ")";
                    cast = "(" + present + " ? " + cast + " : "
                            + coerce(emitDefaultValue(p, pt, c, ctx), pt, ctx) + ")";
                }
                if (!p.named) {
                    pi++;
                }
                args.append(cast);
            }
            TypeRef rt = m.returnType == null ? TypeRef.DYNAMIC : m.returnType;
            String invoke = "this." + javaMethodName(m.name) + "(" + args + ")";
            call.append("            ").append(DYN_CASE).append("case \"").append(m.name).append("\": ");
            // Dart checks a dynamic call's shape before running the method: a missing
            // required argument, an extra positional one or an unknown name is a
            // NoSuchMethodError, never a silent null or a later unboxing failure.
            int minPos = 0;
            StringBuilder named = new StringBuilder();
            StringBuilder requiredNamed = new StringBuilder();
            for (Param p : m.params) {
                if (p.named) {
                    named.append(named.length() > 0 ? ", " : "").append('"').append(p.name).append('"');
                    if (p.required) {
                        requiredNamed.append(requiredNamed.length() > 0 ? ", " : "").append('"').append(p.name).append('"');
                    }
                } else if (!p.optional && p.defaultValue == null) {
                    minPos++;
                }
            }
            call.append("dart.runtime.Dyn.checkArgs(this, \"").append(m.name).append("\", a, n, ").append(minPos).append(", ")
                    .append(positional).append(", ")
                    .append(named.length() == 0 ? "null" : "new String[] {" + named + "}").append(", ")
                    .append(requiredNamed.length() == 0 ? "null" : "new String[] {" + requiredNamed + "}").append("); ");
            if (rt.is("void") || overridesVoid(c, m.name)) {
                call.append(invoke).append("; return null;\n");
            } else {
                call.append("return ").append(invoke).append(";\n");
            }
        }
        String noMember = inherits ? null : "dart.runtime.Dyn.NO_MEMBER";
        body.append("    @Override\n    @SuppressWarnings(\"unchecked\")\n    public Object $dget(String name) {\n        switch (name) {\n").append(get)
                .append("            default: return ").append(noMember == null ? "super.$dget(name)" : noMember)
                .append(";\n        }\n    }\n\n");
        body.append("    @Override\n    @SuppressWarnings(\"unchecked\")\n    public boolean $dset(String name, Object v) {\n        switch (name) {\n")
                .append(set).append("            default: return ").append(inherits ? "super.$dset(name, v)" : "false")
                .append(";\n        }\n    }\n\n");
        body.append("    @Override\n    @SuppressWarnings(\"unchecked\")\n    public Object $dcall(String name, Object[] a, dart.core.DartMap<String, Object> n) {\n"
                + "        switch (name) {\n").append(call).append("            default: return ")
                .append(noMember == null ? "super.$dcall(name, a, n)" : noMember).append(";\n        }\n    }\n\n");
        return !inherits;
    }

    /** Whether {@code t} names any of {@code vars}, at any depth of its type arguments. */
    private static boolean mentionsAny(TypeRef t, java.util.Set<String> vars) {
        if (t == null || vars.isEmpty()) {
            return false;
        }
        if (t.name != null && vars.contains(t.name)) {
            return true;
        }
        for (TypeRef a : t.args) {
            if (mentionsAny(a, vars)) {
                return true;
            }
        }
        if (mentionsAny(t.funcReturn, vars)) {
            return true;
        }
        if (t.funcParams != null) {
            for (TypeRef a : t.funcParams) {
                if (mentionsAny(a, vars)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether {@link #emitDynamicMembers} gives this program class its dynamic members. */
    private boolean hasDynamicMembers(ClassDecl c) {
        java.util.Set<String> guard = new java.util.HashSet<String>();
        while (c != null && guard.add(c.name)) {
            if (c.isMixin) {
                return false;
            }
            if (c.superclass == null) {
                return true;
            }
            c = superDeclOf(c, c.superclass);
        }
        return false;
    }

    /// A dynamic value converted to the Java form of a parameter or field type, unboxing
    /// primitives. Checked through DartRuntime.as (an instanceof test that throws Dart's
    /// TypeError): a bare Java cast is unchecked on ParparVM, so `d.setCount("3")`
    /// against an int parameter called longValue() on a String -- a native crash no
    /// Dart catch could see. A type variable stays an unchecked cast, as `as T` does.
    private String dynCast(String raw, TypeRef t, Ctx ctx) {
        if (t == null || isDynamicType(t)) {
            return raw;
        }
        if (isTypeVariable(t, ctx)) {
            return "(" + javaType(boxType(t), true, ctx) + ") " + raw;
        }
        String checked;
        if (t.is("int") || t.is("double") || t.is("num")) {
            checked = "dart.runtime.DartRuntime.as(" + raw + ", Number.class, " + t.nullable + ", "
                    + quote(t.toString()) + ")";
            if (t.nullable || t.is("num")) {
                return t.is("num") ? checked
                        : "(" + javaType(boxType(t), true, ctx) + ") dart.runtime.Dyn.numAs(" + checked + ", "
                                + t.is("int") + ")";
            }
            return checked + "." + (t.is("int") ? "longValue()" : "doubleValue()");
        }
        String erased = erasedJavaType(t, ctx);
        checked = "dart.runtime.DartRuntime.as(" + raw + ", " + erased + ".class, " + t.nullable + ", "
                + quote(t.toString()) + ")";
        if (t.is("bool") && !t.nullable) {
            return checked + ".booleanValue()";
        }
        String jt = javaType(boxType(t), true, ctx);
        return jt.equals(erased) ? checked : "((" + jt + ") " + checked + ")";
    }

    /** Whether a program superclass declares {@code name} void, which a Java override keeps. */
    private boolean overridesVoid(ClassDecl c, String name) {
        java.util.Set<String> guard = new java.util.HashSet<String>();
        ClassDecl k = c.superclass == null ? null : superDeclOf(c, c.superclass);
        while (k != null && guard.add(k.name)) {
            MethodDecl sm = k.method(name);
            if (sm != null && sm.returnType != null && sm.returnType.is("void")) {
                return true;
            }
            k = k.superclass == null ? null : superDeclOf(k, k.superclass);
        }
        return false;
    }

    /**
     * A program class Java can stand in for an interface by extending it: abstract, with
     * no superclass or mixins of its own, no instance fields, no member with a body and
     * no constructor that takes or does anything.
     */
    private boolean isPureInterface(ClassDecl c) {
        if (!c.isAbstract || c.superclass != null || !c.mixins.isEmpty()) {
            return false;
        }
        for (TypeRef itf : c.interfaces) {
            ClassDecl ip = superDeclOf(c, itf);
            if (ip != null && !ip.isMixin) {
                return false;
            }
        }
        for (FieldDecl f : c.fields) {
            if (!f.isStatic) {
                return false;
            }
        }
        for (MethodDecl m : c.methods) {
            if (!m.isStatic && (m.body != null || m.exprBody != null)) {
                return false;
            }
        }
        for (CtorDecl ct : c.ctors) {
            // A factory is a static method, so extending the class inherits nothing from
            // it: `abstract class Shape { factory Shape.circle(..) = Circle; }` stays an
            // interface, which is the shape every freezed model has.
            if (ct.isFactory) {
                continue;
            }
            if (!ct.params.isEmpty() || !ct.fieldInits.isEmpty()
                    || ct.superInit != null || ct.redirect != null || ct.body != null) {
                return false;
            }
        }
        return true;
    }

    /** Names of the classes that directly extend or implement the named class (whole-program). */
    private List<String> directSubtypes(String name) {
        List<String> subs = new ArrayList<String>();
        for (ClassDecl c : program.classes.values()) {
            if (c.extensionOn != null || c.isMixin) {
                continue;
            }
            boolean extendsIt = c.superclass != null && name.equals(c.superclass.name);
            boolean implementsIt = false;
            for (TypeRef itf : c.interfaces) {
                if (name.equals(itf.name)) {
                    implementsIt = true;
                    break;
                }
            }
            if (extendsIt || implementsIt) {
                // The EMITTED name, not the Dart one. A subtype whose name collides with a
                // stub class is emitted under a library-qualified name, and a permits clause
                // naming the Dart name then refers to a class that does not exist - which is
                // not a niche case, since the Flutter stubs declare Rect, Size, Color and
                // plenty of other names an app will reasonably use for its own sealed types.
                subs.add(javaClassName(c));
            }
        }
        return subs;
    }

    private GeneratedFile finishClassFile(String name, String decl, String ext, String impls,
                                          CharSequence body, Ctx ctx, String dartFile) {
        return finishClassFile(name, decl, ext, impls, null, body, ctx, dartFile);
    }

    private GeneratedFile finishClassFile(String name, String decl, String ext, String impls,
                                          String permits, CharSequence body, Ctx ctx, String dartFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        for (String imp : ctx.imports.values()) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!ctx.imports.isEmpty()) {
            sb.append('\n');
        }
        sb.append("// Generated from ").append(dartFile).append(" -- do not edit.\n");
        sb.append(decl);
        if (ext != null) {
            sb.append(" extends ").append(ext);
        }
        if (impls != null && !impls.isEmpty()) {
            sb.append(" implements ").append(impls);
        }
        if (permits != null && !permits.isEmpty()) {
            sb.append(" permits ").append(permits);
        }
        sb.append(" {\n\n").append(body).append("}\n");
        return new GeneratedFile(name + ".java", sb.toString());
    }

    private String dartRef(Node n) {
        return "// Generated from " + n.file + " -- do not edit.";
    }

    // ==================================================================
    // Constructors
    // ==================================================================

    /** Canonical parameter order: positional as declared, then named as declared. */
    private String emitCtor(ClassDecl c, CtorDecl ct, Ctx ctx) {
        if (ct.isFactory) {
            return emitFactoryCtor(c, ct, ctx);
        }
        if (ct.name != null) {
            return emitNamedCtor(c, ct, ctx);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("    public ").append(javaClassName(c)).append('(');
        ctx.pushScope();
        List<Param> params = ct.params;
        String twDecls = witnessParamDecls(c);
        sb.append(params.isEmpty() ? stripTrailingComma(twDecls) : twDecls);
        java.util.Set<String> savedWitnessLocals = ctx.witnessLocals;
        ctx.witnessLocals = new HashSet<String>(witnessParams(c));
        for (int i = 0; i < params.size(); i++) {
            Param p = params.get(i);
            TypeRef pt = paramType(c, p, ctx);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(ctx.declareShadowSafe(p.name, pt));
        }
        sb.append(") {\n");
        Ctx.Writer w = ctx.pushWriter(2);
        String superNamedInit = null;

        // super(...) initializer for program superclasses
        ClassDecl progSuper = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        if (progSuper != null) {
            Args superArgs = superArgsOf(ct);
            CtorDecl superNamed = ct.superInit != null && ct.superInit.namedCtor != null
                    ? progSuper.namedCtor(ct.superInit.namedCtor) : null;
            if (superNamed != null && !superNamed.isFactory) {
                // `Child() : super.named(..)` runs the superclass's NAMED constructor; it
                // ran the unnamed one. A named constructor is the marker constructor plus
                // its $init$ method, so the same pair runs here. The call comes after this
                // class's initializer list, before its body, as Dart orders them.
                w.line("super((" + javaClassName(progSuper) + ".$NamedCtor) null);");
                superNamedInit = "super.$init$" + superNamed.name + "("
                        + canonicalArgs(superNamed, superArgs, ctx) + ");";
            } else {
                CtorDecl superCtor = progSuper.defaultCtor();
                // Nothing may precede super(...), so its arguments cannot be sequenced
                // through lifted temps; they keep the parameter order.
                inSuperInitializer = true;
                try {
                    String superCanon = canonicalArgs(superCtor, superArgs, ctx);
                    String superTw = witnessArgs(progSuper, c.superclass.args, ct, ctx);
                    w.line("super(" + (superCanon.isEmpty() ? stripTrailingComma(superTw)
                            : superTw + superCanon) + ");");
                } finally {
                    inSuperInitializer = false;
                }
            }
        } else if (ct.superInit != null && ct.superInit.args != null
                && stubClassOf(c.superclass) != null) {
            // Positional arguments go to the stub superclass's Java constructor --
            // `class C extends Cubit<int> { C() : super(0); }` -- which a stub declares when
            // its runtime class has no no-arg constructor. Named ones stay setters.
            // Only when the stub declares an unnamed constructor taking positionals: one
            // that declares none (the gallery's BoxPainter) has just Java's no-arg one,
            // and the Dart super arguments have nowhere to go.
            if (!ct.superInit.args.positional.isEmpty() && ct.superInit.namedCtor == null
                    && stubTakesPositionals(stubClassOf(c.superclass))) {
                StringBuilder sa = new StringBuilder();
                for (Expr e : ct.superInit.args.positional) {
                    if (sa.length() > 0) {
                        sa.append(", ");
                    }
                    sa.append(emitExpr(e, null, ctx).code);
                }
                inSuperInitializer = true;
                try {
                    w.line("super(" + sa + ");");
                } finally {
                    inSuperInitializer = false;
                }
            }
            for (NamedArg na : ct.superInit.args.named) {
                Out v = emitExpr(na.value, null, ctx);
                w.line("this." + na.name + "(" + v.code + ");");
            }
        }
        storeWitnesses(c, "this", true, superNamedInit != null, w, ctx);
        // super.x params against stub superclasses -> inherited setters
        if (progSuper == null) {
            for (Param p : params) {
                if (p.isSuper) {
                    w.line("this." + p.name + "(" + javaIdent(p.name) + ");");
                }
            }
        }
        // this.x params
        for (Param p : params) {
            if (p.isThis) {
                w.line(fieldStore(c, p.name, javaIdent(p.name)));
            }
        }
        // initializer list entries
        for (FieldInit fi : ct.fieldInits) {
            Out v = emitExpr(fi.value, typeOfField(c, fi.field, ctx), ctx);
            w.line(fieldStore(c, fi.field, v.code));
        }
        if (superNamedInit != null) {
            w.line(superNamedInit);
        }
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
        ctx.witnessLocals = savedWitnessLocals;
        sb.append(ctx.popWriter());
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * factory Foo(...) / factory Foo.name(...) -- a static method returning
     * the class; unnamed factories become {@code $create} and call sites
     * route through it.
     */
    /**
     * A generic class's static constructor method cannot see the class's type variables, so
     * it declares its own of the same names: {@code public static <T> Box<T> of(T v)}.
     */
    private static String staticTypeParams(ClassDecl c) {
        if (c.typeParams.isEmpty()) {
            return "";
        }
        return "<" + String.join(", ", c.typeParams) + "> ";
    }

    private static String staticTypeArgs(ClassDecl c) {
        if (c.typeParams.isEmpty()) {
            return "";
        }
        return "<" + String.join(", ", c.typeParams) + ">";
    }

    private String emitFactoryCtor(ClassDecl c, CtorDecl ct, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        String name = ct.name == null ? "$create" : javaIdent(ct.name);
        ctx.pushScope();
        // A factory may return any implementer, so an implicit interface's factory returns it.
        sb.append("    public static ").append(staticTypeParams(c)).append(javaClassName(c))
                .append(interfacedClasses().contains(c) ? "$I" : "")
                .append(staticTypeArgs(c)).append(' ').append(name).append('(');
        String twDecls = witnessParamDecls(c);
        sb.append(ct.params.isEmpty() ? stripTrailingComma(twDecls) : twDecls);
        appendParams(sb, c, ct.params, ctx);
        sb.append(") {\n");
        ctx.pushWriter(2);
        ctx.methodReturnType = new TypeRef(c.name);
        java.util.Set<String> savedWitnessLocals = ctx.witnessLocals;
        boolean savedStatic = ctx.staticMember;
        ctx.witnessLocals = new HashSet<String>(witnessParams(c));
        ctx.staticMember = true;
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
        ctx.witnessLocals = savedWitnessLocals;
        ctx.staticMember = savedStatic;
        sb.append(ctx.popWriter());
        ctx.methodReturnType = null;
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * Dart named constructor C.name(...) -- a private marker constructor
     * (field initializers still run), an instance $init$name carrying the
     * body with normal {@code this} semantics, and a public static factory
     * with the constructor's name that call sites invoke.
     */
    private String emitNamedCtor(ClassDecl c, CtorDecl ct, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        ctx.pushScope();
        StringBuilder paramSig = new StringBuilder();
        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < ct.params.size(); i++) {
            Param p = ct.params.get(i);
            TypeRef pt = paramType(c, p, ctx);
            if (i > 0) {
                paramSig.append(", ");
                argList.append(", ");
            }
            String jn = ctx.declareShadowSafe(p.name, pt);
            paramSig.append(javaType(pt, false, ctx)).append(' ').append(jn);
            argList.append(jn);
        }
        sb.append("    public static ").append(staticTypeParams(c)).append(javaClassName(c))
                .append(staticTypeArgs(c)).append(' ').append(javaIdent(ct.name))
                .append('(').append(paramSig.length() == 0 ? stripTrailingComma(witnessParamDecls(c))
                        : witnessParamDecls(c) + paramSig).append(") {\n");
        if (c.isAbstract) {
            // `abstract class A { A._(..); }` is reached only as a subclass's `super._(..)`
            // (the $init$ below); Java cannot instantiate the abstract class here.
            sb.append("        throw new UnsupportedOperationException(\"abstract ")
                    .append(c.name).append("\");\n    }\n\n");
        } else {
            sb.append("        ").append(javaClassName(c)).append(" $self = new ").append(javaClassName(c)).append("(($NamedCtor) null);\n");
            for (String tw : witnessParams(c)) {
                sb.append("        $self.").append(witnessField(c, tw)).append(" = $tw$").append(tw).append(";\n");
            }
            sb.append("        $self.$init$").append(ct.name).append('(').append(argList).append(");\n");
            sb.append("        return $self;\n    }\n\n");
        }
        sb.append("    void $init$").append(ct.name).append('(').append(paramSig).append(") {\n");
        ctx.pushWriter(2);
        emitInitBody(c, ct, ctx);
        sb.append(ctx.popWriter());
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * The unnamed constructor as an {@code $init$} method, for a subclass's named
     * constructor to run as its {@code super(..)}: that subclass is built through the
     * marker chain, so no Java constructor of this class runs for it. A class with no
     * Dart constructor gets the implicit one's: no parameters, only its own super call.
     */
    private String emitUnnamedInit(ClassDecl c, Ctx ctx) {
        CtorDecl ct = c.defaultCtor();
        if (ct == null) {
            if (!c.ctors.isEmpty()) {
                return "";   // only named constructors: Dart has no super() to call
            }
            ct = new CtorDecl();
        }
        StringBuilder sb = new StringBuilder();
        ctx.pushScope();
        sb.append("    void $init$(");
        for (int i = 0; i < ct.params.size(); i++) {
            Param p = ct.params.get(i);
            TypeRef pt = paramType(c, p, ctx);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(ctx.declareShadowSafe(p.name, pt));
        }
        sb.append(") {\n");
        ctx.pushWriter(2);
        emitInitBody(c, ct, ctx);
        sb.append(ctx.popWriter());
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * A constructor's work in method form, in Dart's order: this.x parameters and the
     * initializer list, then the superclass constructor it names -- an {@code $init$}
     * method of a program superclass, or setters for a stub superclass's named
     * arguments and super parameters, as the unnamed Java constructor applies them --
     * then the body.
     */
    private void emitInitBody(ClassDecl c, CtorDecl ct, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (ct.redirect != null) {
            // `C.a(x) : this.b(x, 0);` runs b's initialization in place of its own: b stores the
            // fields and calls the superclass, so this one does nothing else.
            String target = ct.redirect.namedCtor;
            CtorDecl targetCtor = target != null ? c.namedCtor(target) : c.defaultCtor();
            w.line("this.$init$" + (target != null ? target : "") + "("
                    + canonicalArgs(targetCtor, ct.redirect.args, ctx) + ");");
            return;
        }
        ClassDecl progSuper = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        for (Param p : ct.params) {
            if (p.isThis) {
                w.line(fieldStore(c, p.name, javaIdent(p.name)));
            }
        }
        for (FieldInit fi : ct.fieldInits) {
            Out v = emitExpr(fi.value, typeOfField(c, fi.field, ctx), ctx);
            w.line(fieldStore(c, fi.field, v.code));
        }
        if (progSuper != null) {
            Args superArgs = superArgsOf(ct);
            String target = ct.superInit != null ? ct.superInit.namedCtor : null;
            CtorDecl superCtor = target != null ? progSuper.namedCtor(target) : progSuper.defaultCtor();
            storeWitnesses(c, "this", false, true, w, ctx);
            w.line("super.$init$" + (target != null ? target : "") + "("
                    + canonicalArgs(superCtor, superArgs, ctx) + ");");
        } else if (stubClassOf(c.superclass) != null) {
            if (ct.superInit != null) {
                for (NamedArg na : ct.superInit.args.named) {
                    Out v = emitExpr(na.value, null, ctx);
                    w.line("this." + na.name + "(" + v.code + ");");
                }
            }
            for (Param p : ct.params) {
                if (p.isSuper) {
                    w.line("this." + p.name + "(" + javaIdent(p.name) + ");");
                }
            }
        }
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
    }

    /**
     * The arguments a constructor passes to its superclass constructor: the super
     * initializer's, then its super parameters -- a positional {@code super.x} after the
     * explicit positional arguments, a named one by name, as Dart passes them. Every
     * super parameter used to go by name, so a positional one reached nothing and the
     * superclass saw its default. A copy: the declaration is emitted more than once.
     */
    private static Args superArgsOf(CtorDecl ct) {
        Args superArgs = new Args();
        if (ct.superInit != null) {
            superArgs.positional.addAll(ct.superInit.args.positional);
            superArgs.named.addAll(ct.superInit.args.named);
        }
        for (Param p : ct.params) {
            if (p.isSuper) {
                Ident id = new Ident();
                id.name = p.name;
                if (p.named) {
                    NamedArg na = new NamedArg();
                    na.name = p.name;
                    na.value = id;
                    superArgs.named.add(na);
                } else {
                    superArgs.positional.add(id);
                }
            }
        }
        return superArgs;
    }

    /**
     * Whether a class carries the named-constructor marker chain: it declares a named
     * constructor, or a program subclass does (the chain runs through every ancestor).
     */
    private boolean needsMarker(ClassDecl c) {
        return markerClasses().contains(c);
    }

    /** Whether some program class with a marker chain extends {@code c} directly. */
    private boolean isSuperOfMarker(ClassDecl c) {
        for (ClassDecl m : markerClasses()) {
            if (m.superclass != null && superDeclOf(m, m.superclass) == c) {
                return true;
            }
        }
        return false;
    }

    private java.util.Set<ClassDecl> markerClasses;

    private java.util.Set<ClassDecl> markerClasses() {
        if (markerClasses == null) {
            java.util.Set<ClassDecl> out = new java.util.HashSet<ClassDecl>();
            for (List<ClassDecl> same : program.classesByName.values()) {
                for (ClassDecl c : same) {
                    if (!c.hasNamedNonFactoryCtor()) {
                        continue;
                    }
                    ClassDecl a = c;
                    while (a != null && out.add(a)) {
                        a = a.superclass != null ? superDeclOf(a, a.superclass) : null;
                    }
                }
            }
            markerClasses = out;
        }
        return markerClasses;
    }

    private void appendParams(StringBuilder sb, ClassDecl c, List<Param> params, Ctx ctx) {
        for (int i = 0; i < params.size(); i++) {
            Param p = params.get(i);
            TypeRef pt = paramType(c, p, ctx);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(ctx.declareShadowSafe(p.name, pt));
        }
    }

    // ==================================================================
    // Methods
    // ==================================================================

    private static class Method {
        String name;
        boolean isStatic;
        boolean isGetter;
        boolean isSetter;
        boolean isOverride;
        boolean isAbstract;
        boolean isAsync;
        boolean isSyncStar;
        boolean isAsyncStar;
        TypeRef returnType;
        /** The Java return type when it must differ from the Dart one's (see interfaceBridges). */
        String javaReturn;
        List<String> typeParams = new ArrayList<String>();
        java.util.Map<String, TypeRef> typeParamBounds = new java.util.HashMap<String, TypeRef>();
        List<Param> params = new ArrayList<Param>();
        Block body;
        Expr exprBody;
        /** The type parameters this method takes a hidden leading TypeWitness for. */
        List<String> witnesses = java.util.Collections.emptyList();
    }

    /**
     * Whether a plain method marked {@code @override} in Dart actually overrides
     * a Java-visible super/stub method with an IDENTICAL signature. Java rejects
     * {@code @Override} on a covariantly-narrowed parameter (e.g.
     * {@code updateShouldNotify(PageStatus)} against
     * {@code updateShouldNotify(InheritedWidget)}), which Dart allows via
     * {@code covariant}. Dropping the annotation when no identical-signature
     * target is found is always compile-safe ({@code @Override} is optional).
     * Getters/setters/static keep their prior behavior.
     */
    private boolean javaOverrides(ClassDecl c, MethodDecl m) {
        if (!m.isOverride) {
            return false;
        }
        if (isSinkAddErrorWithTrace(c, m)) {
            // Bridged by emitSinkAddErrorBridges: the runtime's addError takes an Object trace.
            return false;
        }
        if (m.isStatic || m.isGetter || m.isSetter) {
            return true;
        }
        // program super chain
        ClassDecl p = c;
        while (p != null) {
            for (TypeRef mix : p.mixins) {
                if (stubSigMatches(mix.name, m)) {
                    return true;
                }
            }
            for (TypeRef itf : p.interfaces) {
                if (stubSigMatches(itf.name, m)) {
                    return true;
                }
            }
            if (p.superclass == null) {
                break;
            }
            ClassDecl sp = superDeclOf(p, p.superclass);
            if (sp != null) {
                MethodDecl sm = sp.method(m.name);
                if (sm != null && sameParamTypes(sm.params, m.params)) {
                    return true;
                }
                p = sp;
                continue;
            }
            // superclass is a stub (or unknown): walk the stub chain
            return stubSigMatches(p.superclass.name, m);
        }
        return false;
    }

    /**
     * Whether the method {@code m} overrides a base method that returns {@code void}. Dart permits
     * overriding a {@code void} method with a value-returning one; Java does not, so such an
     * override must be emitted with a {@code void} return to stay a valid override.
     */
    private boolean overriddenReturnsVoid(ClassDecl c, MethodDecl m) {
        if (!m.isOverride || m.isStatic || m.isGetter || m.isSetter) {
            return false;
        }
        ClassDecl p = c;
        while (p != null) {
            if (p.superclass == null) {
                break;
            }
            ClassDecl sp = superDeclOf(p, p.superclass);
            if (sp != null) {
                MethodDecl sm = sp.method(m.name);
                // Match by name+arity: a base param typed with the class's type variable won't
                // name-match the override's concrete substitution.
                if (sm != null && sm.params.size() == m.params.size() && !sm.isGetter && !sm.isSetter) {
                    return sm.returnType != null && sm.returnType.is("void");
                }
                p = sp;
                continue;
            }
            return stubMethodReturnsVoid(p.superclass.name, m);
        }
        return false;
    }

    /**
     * Bridges a program class's field or getter onto the method name a stub
     * interface declares for it.
     *
     * <p>The two halves of the contract name the same property differently. A
     * stub's instance getter {@code E get current} is a Java method
     * {@code current()}; a program class's field {@code current} is a private
     * field with {@code get$current()} accessors. A class that satisfies the
     * stub interface with a field therefore does not implement the interface
     * method at all -- Java silently keeps the interface's default.
     *
     * <p>That is not a compile error and it does not throw. It just answers the
     * default forever: {@code Board with IterableMixin} iterated correctly and
     * handed every element back as null, so the 2D-transformations demo's
     * painter died on the first {@code boardPoint!} and the entire board -- the
     * only content on that screen -- never drew.
     */
    private String emitStubGetterBridges(ClassDecl c, Ctx ctx) {
        StringBuilder out = new StringBuilder();
        Set<String> done = new HashSet<String>();
        List<TypeRef> supers = new ArrayList<TypeRef>(c.interfaces);
        supers.addAll(c.mixins);
        for (TypeRef ref : supers) {
            Ast.ClassDecl sc = stubs.classes.get(ref.name);
            while (sc != null) {
                for (Ast.MethodDecl sm : sc.methods) {
                    if (!sm.isGetter || sm.isStatic || done.contains(sm.name)) {
                        continue;
                    }
                    // Only when the property is a FIELD. A Dart getter is already
                    // emitted under the interface's own name, so bridging it would
                    // declare the method twice; a class that supplies neither is a
                    // gap the interface's default is entitled to fill.
                    FieldDecl f = c.field(sm.name);
                    if (f == null) {
                        continue;
                    }
                    boolean declaresMethod = false;
                    for (MethodDecl m : c.methods) {
                        if (sm.name.equals(m.name) && (m.isGetter
                                || (!m.isSetter && m.params.isEmpty()))) {
                            declaresMethod = true;
                            break;
                        }
                    }
                    if (declaresMethod) {
                        continue;
                    }
                    done.add(sm.name);
                    String jt = javaType(fieldType(f, ctx), false, ctx);
                    out.append("    @Override\n");
                    out.append("    public ").append(jt).append(' ').append(sm.name)
                            .append("() {\n        return get$").append(sm.name)
                            .append("();\n    }\n\n");
                }
                sc = sc.superclass != null ? stubs.classes.get(sc.superclass.name) : null;
            }
        }
        return out.toString();
    }

    /** As {@link #stubSigMatches} but reports whether the matched stub method returns {@code void}. */
    private boolean stubMethodReturnsVoid(String stubClassName, MethodDecl m) {
        Ast.ClassDecl sc = stubs.classes.get(stubClassName);
        while (sc != null) {
            for (Ast.MethodDecl sm : sc.methods) {
                if (!sm.isGetter && !sm.isSetter && sm.name.equals(m.name)
                        && sm.params.size() == m.params.size()) {
                    return sm.returnType != null && sm.returnType.is("void");
                }
            }
            sc = sc.superclass != null ? stubs.classes.get(sc.superclass.name) : null;
        }
        return false;
    }

    /** A same-name, same-arity, identical-param-type method anywhere on a stub class's chain. */
    /**
     * Dart lets an override narrow a parameter ({@code covariant}, or implicitly on a
     * generated delegate's {@code shouldReload(_AppLocalizationsDelegate old)} and an
     * inherited widget's {@code updateShouldNotify(PageStatus old)}); Java does not, so
     * the narrowed method is only an OVERLOAD, and the runtime -- which calls the base
     * signature -- reached the base's default instead of the app's code. This emits the
     * override Java can see: the base's erased signature, casting each argument to the
     * narrowed type and delegating. Empty when no wider base method exists, when a
     * primitive, optional or type-variable parameter is involved, or when the erasures
     * coincide (the narrowed method then already overrides, or would clash).
     */
    private String emitCovariantBridge(ClassDecl c, MethodDecl m, Method mm, Ctx ctx) {
        if (m.isStatic || m.isGetter || m.isSetter || m.name.startsWith("$")
                || "hashCode".equals(m.name) || "compareTo".equals(m.name)) {
            return "";
        }
        for (Param p : m.params) {
            if (p.named || p.defaultValue != null) {
                return "";
            }
        }
        Object[] found = widerBaseMethod(c, m);
        if (found == null) {
            return "";
        }
        MethodDecl sm = (MethodDecl) found[0];
        List<String> ownerTypeParams = (List<String>) found[1];
        StringBuilder sig = new StringBuilder();
        StringBuilder call = new StringBuilder();
        boolean differs = false;
        for (int i = 0; i < m.params.size(); i++) {
            TypeRef wide = sm.params.get(i).type == null ? TypeRef.DYNAMIC : sm.params.get(i).type;
            TypeRef narrow = m.params.get(i).type == null ? TypeRef.DYNAMIC : m.params.get(i).type;
            if (ownerTypeParams.contains(wide.name) || sm.typeParams.contains(wide.name)) {
                // A type-variable parameter (RestorableProperty<T>.initWithValue(T)): Java's
                // own generic substitution already makes initWithValue(Set<X>) in a
                // RestorableProperty<Set<X>> an override, and a bridge taking Object
                // would clash with it (same erasure). Except when the argument is a
                // primitive: `implements EventSink<int>` substitutes add(Long) while the
                // Dart `add(int)` is emitted as add(long), so the boxed spelling is bridged.
                String prim = javaType(narrow, false, ctx);
                String boxed = "long".equals(prim) ? "Long" : "double".equals(prim) ? "Double"
                        : "boolean".equals(prim) ? "Boolean" : null;
                // A collection-mixin class already emits that element side boxed itself.
                if (boxed == null || appliesListMixin(c) || appliedCollectionMixin(c.name) != null) {
                    return "";
                }
                differs = true;
                if (i > 0) {
                    sig.append(", ");
                    call.append(", ");
                }
                sig.append(boxed).append(" $b").append(i);
                // Unboxed explicitly: a bare Long argument would pick this bridge again.
                call.append("$b").append(i).append('.').append(prim).append("Value()");
                continue;
            }
            String wideJava = javaType(TypeRef.of(wide.name), false, ctx);
            String narrowJava = javaType(narrow, false, ctx);
            if (isJavaPrimitive(wideJava) || isJavaPrimitive(narrowJava)) {
                return "";
            }
            String narrowErased = narrowJava.indexOf('<') >= 0
                    ? narrowJava.substring(0, narrowJava.indexOf('<')) : narrowJava;
            if (!narrowErased.equals(wideJava)) {
                differs = true;
            }
            if (i > 0) {
                sig.append(", ");
                call.append(", ");
            }
            sig.append(wideJava).append(" $b").append(i);
            call.append("((").append(narrowJava).append(") $b").append(i).append(')');
        }
        if (!differs) {
            return "";
        }
        TypeRef rt = mm.returnType == null || mm.returnType.is("var") ? TypeRef.DYNAMIC : mm.returnType;
        String rjt = javaType(rt, false, ctx);
        TypeRef brt = sm.returnType == null || sm.returnType.is("var") ? TypeRef.DYNAMIC : sm.returnType;
        String baseRjt = ownerTypeParams.contains(brt.name) || sm.typeParams.contains(brt.name)
                ? "Object" : javaType(brt, false, ctx);
        if ((isJavaPrimitive(rjt) || isJavaPrimitive(baseRjt) || "void".equals(rjt) || "void".equals(baseRjt))
                && !rjt.equals(baseRjt)) {
            // An override must return the base's primitive exactly; a covariant object
            // return is fine, a boolean against Object is not.
            return "";
        }
        // No @Override: the bridge's parameter types come from the Dart stub's names, and
        // should that mapping ever miss the Java signature the bridge is a harmless
        // overload -- exactly the state before it -- instead of a compile error.
        StringBuilder sb = new StringBuilder();
        sb.append("    @SuppressWarnings({\"unchecked\", \"rawtypes\"})\n    public ")
                .append(rjt).append(' ').append(m.name).append('(').append(sig).append(") {\n        ")
                .append("void".equals(rjt) ? "" : "return ").append(m.name).append('(').append(call)
                .append(");\n    }\n\n");
        return sb.toString();
    }

    private static boolean isJavaPrimitive(String t) {
        return "long".equals(t) || "double".equals(t) || "boolean".equals(t) || "int".equals(t)
                || "float".equals(t) || "char".equals(t) || "short".equals(t) || "byte".equals(t);
    }

    /**
     * The nearest base method -- program superclass chain first, then the stub chain,
     * mixins and interfaces -- with {@code m}'s name and arity but not its parameter
     * types, with the type parameters of the class declaring it; null when none.
     */
    private Object[] widerBaseMethod(ClassDecl c, MethodDecl m) {
        ClassDecl p = c;
        while (p != null && p.superclass != null) {
            ClassDecl sp = superDeclOf(p, p.superclass);
            if (sp == null) {
                Object[] s = widerStubMethod(p.superclass.name, m);
                if (s != null) {
                    return s;
                }
                break;
            }
            MethodDecl sm = sp.method(m.name);
            if (sm != null && !sm.isGetter && !sm.isSetter && !sm.isStatic) {
                if (sm.params.size() == m.params.size() && !sameParamTypes(sm.params, m.params)) {
                    return new Object[] {sm, sp.typeParams};
                }
                return null;
            }
            p = sp;
        }
        for (ClassDecl q = c; q != null; q = q.superclass == null ? null : superDeclOf(q, q.superclass)) {
            for (TypeRef t : q.mixins) {
                Object[] s = widerStubMethod(t.name, m);
                if (s != null) {
                    return s;
                }
            }
            for (TypeRef t : q.interfaces) {
                Object[] s = widerStubMethod(t.name, m);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private Object[] widerStubMethod(String stubClassName, MethodDecl m) {
        Ast.ClassDecl sc = stubs.classes.get(stubClassName);
        while (sc != null) {
            for (Ast.MethodDecl sm : sc.methods) {
                if (!sm.isGetter && !sm.isSetter && !sm.isStatic && sm.name.equals(m.name)) {
                    if (sm.params.size() == m.params.size() && !sameParamTypes(sm.params, m.params)) {
                        return new Object[] {sm, sc.typeParams};
                    }
                    return null;
                }
            }
            sc = sc.superclass != null ? stubs.classes.get(sc.superclass.name) : null;
        }
        return null;
    }

    /**
     * Whether m is a sink's {@code addError(Object error, [StackTrace? stackTrace])} in a class
     * that is a Dart EventSink or StreamSink -- both {@code dart.async.StreamSink} in Java, whose
     * addError is {@code (Object)} plus a default {@code (Object, Object)}. The emitted
     * {@code (Object, StackTrace)} is an overload of neither, so a caller holding the
     * interface would never reach it.
     */
    /** A program class's {@code void close()} that implements Dart's EventSink.close. */
    private boolean isEventSinkVoidClose(Method m, Ctx ctx) {
        if (m.isStatic || m.isGetter || m.isSetter || !"close".equals(m.name) || !m.params.isEmpty()
                || m.returnType == null || !m.returnType.is("void") || ctx.currentClass == null
                || ctx.currentClass.isMixin) {
            return false;
        }
        return reachesStubJavaName(ctx.currentClass, "dart.async.StreamSink");
    }

    private boolean isSinkAddErrorWithTrace(ClassDecl c, MethodDecl m) {
        if (m.isStatic || m.isGetter || m.isSetter || !"addError".equals(m.name) || m.params.size() != 2
                || m.params.get(1).type == null || !m.params.get(1).type.is("StackTrace")) {
            return false;
        }
        return reachesStubJavaName(c, "dart.async.StreamSink");
    }

    /** Whether program class c reaches, through supers, interfaces or mixins, a stub with that Java name. */
    private boolean reachesStubJavaName(ClassDecl c, String javaName) {
        java.util.ArrayDeque<TypeRef> todo = new java.util.ArrayDeque<TypeRef>();
        Set<String> seen = new HashSet<String>();
        ClassDecl cur = c;
        for (int guard = 0; guard < 256; guard++) {
            if (cur != null && seen.add("p:" + cur.name)) {
                if (cur.superclass != null) {
                    todo.add(cur.superclass);
                }
                todo.addAll(cur.interfaces);
                todo.addAll(cur.mixins);
            }
            cur = null;
            if (todo.isEmpty()) {
                return false;
            }
            TypeRef t = todo.poll();
            if (t == null || t.name == null) {
                continue;
            }
            ClassDecl pc = program.classes.get(t.name);
            if (pc != null) {
                cur = pc;
                continue;
            }
            Ast.ClassDecl sc = stubs.classes.get(t.name);
            if (sc != null && seen.add("s:" + t.name)) {
                if (javaName.equals(sc.javaName)) {
                    return true;
                }
                if (sc.superclass != null) {
                    todo.add(sc.superclass);
                }
                todo.addAll(sc.interfaces);
            }
        }
        return false;
    }

    /**
     * The interface members that reach a sink's {@code addError(Object, StackTrace)}: the
     * runtime's {@code addError(Object)} and {@code addError(Object, Object)}. Without them the
     * one-argument form stayed abstract, and a two-argument call through the interface took
     * the runtime default, which skipped the class's own handling entirely.
     */
    private String emitSinkAddErrorBridges(ClassDecl c, Ctx ctx) {
        if (c.isMixin) {
            return "";
        }
        for (MethodDecl m : c.methods) {
            if (m.isOverride && isSinkAddErrorWithTrace(c, m) && !m.isAbstract) {
                String st = "dart.core.StackTrace";
                return "    @Override\n    public void addError(Object error) {\n"
                        + "        this.addError(error, (" + st + ") null);\n    }\n\n"
                        + "    @Override\n    public void addError(Object error, Object stackTrace) {\n"
                        + "        this.addError(error, stackTrace instanceof " + st + " ? (" + st + ") stackTrace : null);\n"
                        + "    }\n\n";
            }
        }
        return "";
    }

    private boolean stubSigMatches(String stubClassName, MethodDecl m) {
        Ast.ClassDecl sc = stubs.classes.get(stubClassName);
        while (sc != null) {
            for (Ast.MethodDecl sm : sc.methods) {
                if (!sm.isGetter && !sm.isSetter && sm.name.equals(m.name)
                        && sameParamTypes(sm.params, m.params)) {
                    return true;
                }
            }
            sc = sc.superclass != null ? stubs.classes.get(sc.superclass.name) : null;
        }
        return false;
    }

    private boolean sameParamTypes(List<Param> a, List<Param> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!paramTypeName(a.get(i)).equals(paramTypeName(b.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The runtime interface ({@code dart.collection.ListMixin} or {@code IterableMixin})
     * that the program class {@code name} or a program superclass applies, as a mixin or as
     * a mixin-declared superclass ({@code extends ListBase<E>}); null when none. ListMixin
     * wins over IterableMixin, since it is the more specific protocol.
     */
    private String appliedCollectionMixin(String name) {
        String found = null;
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (ClassDecl mc = program.classes.get(name); mc != null && seen.add(mc.name);
                mc = mc.superclass != null ? superDeclOf(mc, mc.superclass) : null) {
            java.util.List<TypeRef> applied = new java.util.ArrayList<TypeRef>(mc.mixins);
            if (mc.superclass != null) {
                applied.add(mc.superclass);
            }
            for (TypeRef mix : applied) {
                Ast.ClassDecl sc = mix.name == null || program.classes.containsKey(mix.name)
                        ? null : stubs.classes.get(mix.name);
                String jn = sc != null ? sc.javaName : null;
                if ("dart.collection.ListMixin".equals(jn) || "dart.collection.SetMixin".equals(jn)
                        || "dart.collection.MapMixin".equals(jn)) {
                    return jn;
                }
                if ("dart.collection.IterableMixin".equals(jn)) {
                    found = jn;
                }
            }
        }
        return found;
    }

    /**
     * {@code o}'s code, bridged to a live {@code DartIterable} when it is a program object
     * applying a dart:collection mixin: a Java {@code Iterable} slot (addAll, a spread,
     * {@code yield*}) cannot take the object itself, which is no Java Iterable.
     */
    private String mixinIterableCode(Out o) {
        if (o.type != null && o.type.name != null && program.classes.containsKey(o.type.name)
                && appliedCollectionMixin(o.type.name) != null) {
            return "dart.core.DartIterable.ofMixin(" + o.code + ")";
        }
        return o.code;
    }

    /** Whether {@code c} or a program superclass applies the stub {@code ListMixin}. */
    private boolean appliesListMixin(ClassDecl c) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (ClassDecl mc = c; mc != null && seen.add(mc.name);
                mc = mc.superclass != null ? superDeclOf(mc, mc.superclass) : null) {
            for (TypeRef mix : mc.mixins) {
                if ("ListMixin".equals(mix.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A program StreamSubscription's {@code Future<E> asFuture<E>([E? futureValue])}: the
     * runtime interface declares the erased {@code Future<Object> asFuture(Object)}, and a
     * generic Java method with the same erasure neither overrides nor may coexist with it.
     * The method type parameter is dropped and read as dynamic throughout the signature.
     */
    /** Whether {@code c} directly implements or extends the runtime's StreamSubscription. */
    private static boolean implementsStreamSubscription(ClassDecl c) {
        if (c == null) {
            return false;
        }
        for (TypeRef i : c.interfaces) {
            if ("StreamSubscription".equals(i.name)) {
                return true;
            }
        }
        return c.superclass != null && "StreamSubscription".equals(c.superclass.name);
    }

    private List<String> eraseAsFutureOverride(Method m, Ctx ctx) {
        if (!"asFuture".equals(m.name) || m.isStatic || m.typeParams == null || m.typeParams.size() != 1
                || m.params.size() > 1 || ctx.currentClass == null) {
            return null;
        }
        if (!implementsStreamSubscription(ctx.currentClass)) {
            return null;
        }
        List<String> tps = m.typeParams;
        List<TypeRef> none = java.util.Collections.<TypeRef>emptyList();
        m.returnType = substTypeParams(m.returnType, tps, none);
        for (Param p : m.params) {
            p.type = substTypeParams(p.type, tps, none);
        }
        m.typeParams = new ArrayList<String>();
        m.witnesses = java.util.Collections.emptyList();
        return tps;
    }

    private String paramTypeName(Param p) {
        // A signature-typed function (`void Function(T)?`, a Funcs SAM in Java) is not the
        // bare `Function` a stub declares for an Object handler slot: the two need a bridge.
        if (p.type != null && (p.type.funcParams != null || p.type.funcReturn != null)) {
            return "Function()";
        }
        return p.type == null || p.type.is("var") ? "dynamic" : p.type.name;
    }

    private String emitMethodLike(Method m, Ctx ctx, boolean classIsAbstract) {
        java.util.Set<String> savedErased = ctx.erasedMethodTypeVars;
        List<String> erased = eraseAsFutureOverride(m, ctx);
        if (erased != null) {
            ctx.erasedMethodTypeVars = new HashSet<String>(erased);
        }
        try {
            String out = emitMethodLikeScoped(m, ctx, classIsAbstract);
            if (m.params.size() == 1 && m.params.get(0).optional && !m.isStatic
                    && ("asFuture".equals(m.name) || "pause".equals(m.name))
                    && implementsStreamSubscription(ctx.currentClass)) {
                // dart.async.StreamSubscription declares the omitted-argument form as its
                // own overload (asFuture(), pause()); the program class must implement it.
                out += "asFuture".equals(m.name)
                        ? "    public Future<Object> asFuture() {\n        return asFuture(null);\n    }\n\n"
                        : "    public void pause() {\n        pause(null);\n    }\n\n";
            }
            return out;
        } finally {
            ctx.erasedMethodTypeVars = savedErased;
        }
    }

    private String emitMethodLikeScoped(Method m, Ctx ctx, boolean classIsAbstract) {
        java.util.Map<String, TypeRef> savedBounds = ctx.methodTypeParamBounds;
        ctx.methodTypeParamBounds = m.typeParamBounds;
        java.util.Set<String> savedTps = ctx.methodTypeParams;
        boolean savedStatic = ctx.staticMember;
        ctx.methodTypeParams = m.typeParams == null ? null : new HashSet<String>(m.typeParams);
        ctx.staticMember = m.isStatic;
        java.util.Set<String> savedWl = ctx.witnessLocals;
        ctx.witnessLocals = m.witnesses.isEmpty() ? null : new HashSet<String>(m.witnesses);
        if (isWhereTypeOverride(m)) {
            ctx.witnessLocals = new HashSet<String>(m.typeParams);
        }
        try {
            return emitMethodLikeBody(m, ctx, classIsAbstract);
        } finally {
            ctx.witnessLocals = savedWl;
            ctx.methodTypeParamBounds = savedBounds;
            ctx.methodTypeParams = savedTps;
            ctx.staticMember = savedStatic;
        }
    }

    /**
     * Whether member n overrides a superclass or interface member whose declared return type is
     * one of that class's own type parameters (`E get defaultValue` in `Buf<E>`): erased to
     * Object there, so an override for `E = int` must return Long, never long.
     */
    private boolean overridesTypeVarReturn(ClassDecl c, String n, boolean getter, Ctx ctx) {
        if (c == null) {
            return false;
        }
        java.util.ArrayDeque<TypeRef> todo = new java.util.ArrayDeque<TypeRef>();
        java.util.Set<ClassDecl> seen = new HashSet<ClassDecl>();
        seen.add(c);
        ClassDecl cur = c;
        for (int guard = 0; guard < 64; guard++) {
            if (cur != c) {
                MethodDecl d = getter ? cur.getter(n) : cur.method(n);
                TypeRef t = d != null ? d.returnType : null;
                if (t == null && getter && cur.field(n) != null) {
                    t = cur.field(n).type;
                }
                if (t != null && t.name != null && t.args.isEmpty() && cur.typeParams.contains(t.name)) {
                    return true;
                }
            }
            if (cur.superclass != null) {
                todo.add(cur.superclass);
            }
            todo.addAll(cur.interfaces);
            todo.addAll(cur.mixins);
            cur = null;
            while (cur == null && !todo.isEmpty()) {
                ClassDecl k = program.resolveClass(todo.poll().name, ctx.library());
                cur = k != null && seen.add(k) ? k : null;
            }
            if (cur == null) {
                return false;
            }
        }
        return false;
    }

    private String emitMethodLikeBody(Method m, Ctx ctx, boolean classIsAbstract) {
        if (isEventSinkVoidClose(m, ctx)) {
            // Dart's EventSink.close() is void; in Java an EventSink is a dart.async.StreamSink,
            // whose close() answers a Future. The body goes into a method of this class's own
            // name -- never a shared one, or a subclass's super.close() would dispatch back
            // into the subclass -- and close() runs it and answers a completed future.
            String own = "close$" + ctx.currentClass.name;
            String saved = m.name;
            boolean savedOverride = m.isOverride;
            m.name = own;
            m.isOverride = false;
            String inner;
            try {
                inner = emitMethodLikeBody(m, ctx, classIsAbstract);
            } finally {
                m.name = saved;
                m.isOverride = savedOverride;
            }
            if (m.isAbstract) {
                return inner;
            }
            return "    @Override\n    public dart.async.Future<Object> close() {\n        this." + own
                    + "();\n        return dart.async.Future.value(null);\n    }\n\n" + inner;
        }
        StringBuilder sb = new StringBuilder();
        TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
        // Dart's `int get hashCode` / `int compareTo(...)` map to Java's Object.hashCode /
        // Comparable.compareTo, which return primitive `int` (not the `long` Dart int uses).
        // Emit a Java `int` return (not `long`) so the override is valid; the body's long
        // result is narrowed with an explicit cast.
        boolean forceIntReturn = ("hashCode".equals(m.name) && m.params.isEmpty() && !m.isSetter)
                || ("compareTo".equals(m.name) && m.params.size() == 1 && !m.isSetter);
        // A private member may override a private FIELD of an implemented class, whose
        // accessors no interface carries; @Override is optional, so it is left off.
        if (m.isOverride && !m.name.startsWith("_")) {
            sb.append("    @Override\n");
        }
        // Dart privacy is library-scoped, not class-scoped: a `_name` member is visible to
        // every other class in the same Dart library. All generated classes land in one Java
        // package, so emit `_`-prefixed members package-private (no modifier) rather than
        // `private`, so sibling classes can still reach them.
        sb.append("    ").append(m.name.startsWith("_") ? "" : "public ");
        if (m.isStatic) {
            sb.append("static ");
        }
        if (m.isAbstract) {
            sb.append("abstract ");
        }
        ctx.pushScope();
        if (m.typeParams != null && !m.typeParams.isEmpty()) {
            sb.append('<');
            for (int i = 0; i < m.typeParams.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(m.typeParams.get(i));
            }
            sb.append("> ");
        }
        // A class `with ListMixin<E>` implements dart.collection.ListMixin's generic
        // `E $index(long)` / `$indexSet(long, E)`: a primitive element type there would
        // not implement them, so the element side of both operators is emitted boxed.
        // `add(E)` too: an override typed `void add(int v)` emitted as add(long) was only an
        // overload, so a call through the List view (ListMixin.add) ran the mixin default.
        boolean boxListMixinOp = ("$index".equals(m.name) || "$indexSet".equals(m.name)
                || ("add".equals(m.name) && m.params.size() == 1))
                && !m.isStatic && appliesListMixin(ctx.currentClass);
        // The same for SetMixin/SetBase and MapMixin/MapBase: their abstract members are
        // generic in E or K/V, so an element, key or value side typed `int` is emitted boxed
        // or the class would only overload them (SetMixin.add(E), MapMixin.$indexSet(K, V)).
        String collFam = m.isStatic || ctx.currentClass == null ? null
                : appliedCollectionMixin(ctx.currentClass.name);
        boolean setFam = "dart.collection.SetMixin".equals(collFam);
        boolean mapFam = "dart.collection.MapMixin".equals(collFam);
        boolean boxCollReturn = (mapFam && ("$index".equals(m.name) || "remove".equals(m.name)))
                || (setFam && "lookup".equals(m.name));
        boolean boxCollAllParams = mapFam && "$indexSet".equals(m.name);
        boolean boxCollFirstParam = setFam && "add".equals(m.name) && m.params.size() == 1;
        String rjt = m.isSetter ? "void" : m.javaReturn != null ? m.javaReturn
                : (forceIntReturn ? "int" : javaType(rt, (boxListMixinOp && "$index".equals(m.name)) || boxCollReturn
                        || (!m.isStatic && overridesTypeVarReturn(ctx.currentClass, m.name, m.isGetter, ctx)), ctx));
        sb.append(rjt).append(' ').append(m.name).append('(');
        // The Java parameter list and names, kept for a sync* body's helper method.
        StringBuilder paramDecls = new StringBuilder();
        StringBuilder paramNames = new StringBuilder();
        boolean whereTypeOverride = isWhereTypeOverride(m);
        if (whereTypeOverride) {
            // The runtime's whereType(Class, boolean) shape (see isWhereTypeOverride); the
            // body reads T's witness from the local rebuilt from the two below.
            String tw = m.typeParams.get(0);
            sb.append("java.lang.Class<").append(tw).append("> $twc$").append(tw)
                    .append(", boolean $twn$").append(tw);
            paramDecls.append(", dart.runtime.TypeWitness $tw$").append(tw);
            paramNames.append(", $tw$").append(tw);
        }
        for (String tw : m.witnesses) {
            sb.append("dart.runtime.TypeWitness $tw$").append(tw).append(m.params.isEmpty() ? "" : ", ");
            paramDecls.append(", dart.runtime.TypeWitness $tw$").append(tw);
            paramNames.append(", $tw$").append(tw);
        }
        for (int i = 0; i < m.params.size(); i++) {
            Param p = m.params.get(i);
            TypeRef pt = p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
            if (i > 0) {
                sb.append(", ");
            }
            String jn = ctx.declareShadowSafe(p.name, pt);
            String decl = javaType(pt, (boxListMixinOp && i == ("add".equals(m.name) ? 0 : 1))
                    || boxCollAllParams || (boxCollFirstParam && i == 0), ctx) + ' ' + jn;
            sb.append(decl);
            paramDecls.append(", ").append(decl);
            paramNames.append(", ").append(jn);
        }
        sb.append(')');
        if (m.isAbstract) {
            sb.append(";\n\n");
            ctx.popScope();
            return sb.toString();
        }
        sb.append(" {\n");
        if (whereTypeOverride) {
            String tw = m.typeParams.get(0);
            sb.append("        dart.runtime.TypeWitness $tw$").append(tw)
                    .append(" = dart.runtime.TypeWitness.of($twc$").append(tw).append(", $twn$")
                    .append(tw).append(");\n");
        }
        ctx.pushWriter(2);
        ctx.methodReturnType = rt;
        ctx.inAsyncBody = m.isAsync;
        ctx.boxedLocals.clear();
        ctx.boxedLocals.addAll(m.body != null
                ? CaptureScan.boxedLocals(m.body) : CaptureScan.boxedLocals(m.exprBody));
        ctx.valueCalledLocals.clear();
        ctx.valueCalledLocals.addAll(CaptureScan.valueCalledNames(m.body, m.exprBody));
        if (m.isAsyncStar && !m.isSetter) {
            // async* generator. As for sync*, the body moves into a helper, here taking the
            // generator's AsyncStar.Sink: `yield x` -> sink.yield$(x), `yield* s` ->
            // sink.yieldAll(s). AsyncStar.asyncStar starts it when the stream is listened to,
            // on the event loop, where its awaits park like any async body's.
            ctx.importClass("dart.async.AsyncStar");
            TypeRef elem = rt.is("Stream") && !rt.args.isEmpty() ? rt.arg(0) : TypeRef.DYNAMIC;
            String ej = javaType(elem, true, ctx);
            String sink = ctx.newTemp();
            String helper = m.name + "$asyncStar";
            ctx.writer().line("return AsyncStar.<" + ej + ">asyncStar((AsyncStar.Sink<" + ej + "> " + sink
                    + ") -> " + helper + "(" + sink + paramNames + "));");
            sb.append(ctx.popWriter());
            sb.append("    }\n\n");
            StringBuilder hb = new StringBuilder("    private ");
            if (m.isStatic) {
                hb.append("static ");
            }
            if (m.typeParams != null && !m.typeParams.isEmpty()) {
                hb.append('<');
                for (int i = 0; i < m.typeParams.size(); i++) {
                    if (i > 0) {
                        hb.append(", ");
                    }
                    hb.append(m.typeParams.get(i));
                }
                hb.append("> ");
            }
            hb.append("void ").append(helper).append("(AsyncStar.Sink<").append(ej).append("> ").append(sink)
                    .append(paramDecls).append(") {\n");
            ctx.pushWriter(2);
            ctx.methodReturnType = TypeRef.VOID;
            ctx.inAsyncBody = true;
            String savedList = ctx.syncStarList;
            String savedSink = ctx.asyncStarSink;
            TypeRef savedElem = ctx.syncStarElem;
            ctx.syncStarList = null;
            ctx.asyncStarSink = sink;
            ctx.syncStarElem = elem;
            if (m.body != null) {
                emitStatements(m.body, ctx);
            }
            ctx.syncStarList = savedList;
            ctx.asyncStarSink = savedSink;
            ctx.syncStarElem = savedElem;
            hb.append(ctx.popWriter());
            hb.append("    }\n\n");
            sb.append(hb);
            ctx.popScope();
            ctx.methodReturnType = null;
            ctx.inAsyncBody = false;
            return sb.toString();
        }
        if (m.isSyncStar && !rt.is("List") && !m.isSetter) {
            // sync* generator. The body moves into a private helper that yields into the list
            // it is given (`yield x` -> out.add(x), `yield* xs` -> out.addAllIterable(xs), and a
            // bare `return;` simply ends it); the method returns DartIterable.syncStar over it,
            // which runs the body when an iteration BEGINS and again for every iteration, as
            // Dart runs a generator. Running it at the call -- as this did -- performed its side
            // effects before anyone iterated and made a second iteration replay the first.
            // (Suspension at each yield is not modelled; see DartIterable.syncStar.)
            ctx.importClass("dart.core.DartList");
            ctx.importClass("dart.core.DartIterable");
            TypeRef elem = rt.is("Iterable") && !rt.args.isEmpty() ? rt.arg(0) : TypeRef.DYNAMIC;
            String ej = javaType(elem, true, ctx);
            String out = ctx.newTemp();
            String helper = m.name + "$sync";
            ctx.writer().line("return DartIterable.syncStar((DartList<" + ej + "> " + out + ") -> "
                    + helper + "(" + out + paramNames + "));");
            sb.append(ctx.popWriter());
            sb.append("    }\n\n");
            StringBuilder hb = new StringBuilder("    private ");
            if (m.isStatic) {
                hb.append("static ");
            }
            if (m.typeParams != null && !m.typeParams.isEmpty()) {
                hb.append('<');
                for (int i = 0; i < m.typeParams.size(); i++) {
                    if (i > 0) {
                        hb.append(", ");
                    }
                    hb.append(m.typeParams.get(i));
                }
                hb.append("> ");
            }
            hb.append("void ").append(helper).append("(DartList<").append(ej).append("> ").append(out)
                    .append(paramDecls).append(") {\n");
            ctx.pushWriter(2);
            ctx.methodReturnType = TypeRef.VOID;
            String savedList = ctx.syncStarList;
            TypeRef savedElem = ctx.syncStarElem;
            ctx.syncStarList = out;
            ctx.syncStarElem = elem;
            if (m.body != null) {
                emitStatements(m.body, ctx);
            }
            ctx.syncStarList = savedList;
            ctx.syncStarElem = savedElem;
            hb.append(ctx.popWriter());
            hb.append("    }\n\n");
            sb.append(hb);
            ctx.popScope();
            ctx.methodReturnType = null;
            ctx.inAsyncBody = false;
            return sb.toString();
        }
        if (m.isSyncStar) {
            // sync* declared as a List (not valid Dart, kept as before): collect yielded values
            // into a DartList and return it.
            ctx.importClass("dart.core.DartList");
            TypeRef elem = (rt.is("Iterable") || rt.is("List") || rt.is("Set")) && !rt.args.isEmpty()
                    ? rt.arg(0) : TypeRef.DYNAMIC;
            String lst = ctx.newTemp();
            ctx.writer().line("DartList<" + javaType(elem, true, ctx) + "> " + lst + " = new DartList<>();");
            String savedList = ctx.syncStarList;
            TypeRef savedElem = ctx.syncStarElem;
            ctx.syncStarList = lst;
            ctx.syncStarElem = elem;
            if (m.body != null) {
                emitStatements(m.body, ctx);
            }
            // DartList is a java Iterable but not a DartIterable; wrap when the declared return type
            // maps to DartIterable (Dart `Iterable<E>`). A `List<E>` return can return the list directly.
            if (rt.is("List")) {
                ctx.writer().line("return " + lst + ";");
            } else {
                ctx.importClass("dart.core.DartIterable");
                ctx.writer().line("return DartIterable.wrap(" + lst + ");");
            }
            ctx.syncStarList = savedList;
            ctx.syncStarElem = savedElem;
            sb.append(ctx.popWriter());
            ctx.popScope();
            ctx.methodReturnType = null;
            ctx.inAsyncBody = false;
            sb.append("    }\n\n");
            return sb.toString();
        }
        boolean asyncFuture = m.isAsync && (rt.is("Future") || rt.is("FutureOr"));
        boolean savedNarrow = ctx.narrowReturnToInt;
        ctx.narrowReturnToInt = forceIntReturn;
        // An async function's exceptions belong to the Future it returns. Emitted
        // bare, a throw in the body -- or an await rethrowing a failed future --
        // left the Java method before any Future existed, so the caller got a
        // synchronous exception where Dart hands back a failed future to await
        // or catchError. The body is bracketed so a throw becomes Future.error.
        // RuntimeException, as an untyped Dart `catch` is emitted: every Dart
        // throw surfaces as one (DartRuntime.asError wraps non-exceptions), and
        // a VM error such as a stack overflow is not turned into a value.
        String asyncErr = null;
        if (asyncFuture && (m.body != null || m.exprBody != null)) {
            ctx.importClass("dart.async.Future");
            asyncErr = ctx.newTemp();
            ctx.writer().line("try {");
            ctx.indent(1);
        }
        if (m.body != null) {
            emitStatements(m.body, ctx);
            if (asyncFuture && !endsWithJump(m.body)) {
                ctx.importClass("dart.async.Future");
                ctx.writer().line("return Future.value(null);");
            }
        } else if (m.exprBody != null) {
            lineMark(m.exprBody, ctx);
            if (asyncFuture) {
                Out o = emitExpr(m.exprBody, null, ctx);
                ctx.importClass("dart.async.Future");
                ctx.writer().line("return " + (o.type.is("Future") ? o.code
                        : "Future.value(" + boxIfPrimitive(o, ctx) + ")") + ";");
            } else {
                if (rt.is("void")) {
                    // `void f() => expr;` -- and a value-returning arrow override narrowed
                    // to void -- runs expr for its effects, whatever its shape.
                    emitDiscardedValue(m.exprBody, ctx);
                } else {
                    Out o = emitExpr(m.exprBody, rt, ctx);
                    if (forceIntReturn) {
                        ctx.writer().line("return (int) (" + o.code + ");");
                    } else {
                        ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
                    }
                }
            }
        }
        if (asyncErr != null) {
            ctx.indent(-1);
            ctx.writer().line("} catch (RuntimeException " + asyncErr + ") {");
            ctx.indent(1);
            ctx.writer().line("return Future.error(" + asyncErr + ");");
            ctx.indent(-1);
            ctx.writer().line("}");
        }
        ctx.narrowReturnToInt = savedNarrow;
        sb.append(ctx.popWriter());
        ctx.popScope();
        ctx.methodReturnType = null;
        ctx.inAsyncBody = false;
        sb.append("    }\n\n");
        return sb.toString();
    }

    /** Does the block's last statement definitely leave the method? */
    private boolean endsWithJump(Block b) {
        if (b.statements.isEmpty()) {
            return false;
        }
        return leavesMethod(b.statements.get(b.statements.size() - 1));
    }

    /**
     * Whether a statement always returns or throws, by the rules javac applies to the
     * statement after it: an if/else when both branches do, a try when its finally does
     * or its body and every catch do. An `async` body that ends in such a try had a
     * trailing `return Future.value(null);` appended, which javac rejected as unreachable.
     */
    private boolean leavesMethod(Stmt last) {
        if (last instanceof ReturnStmt) {
            return true;
        }
        if (last instanceof ExprStmt) {
            return ((ExprStmt) last).expr instanceof ThrowExpr;
        }
        if (last instanceof Block) {
            List<Stmt> ss = ((Block) last).statements;
            return !ss.isEmpty() && leavesMethod(ss.get(ss.size() - 1));
        }
        if (last instanceof IfStmt) {
            IfStmt is = (IfStmt) last;
            return is.casePattern == null && is.elseStmt != null
                    && leavesMethod(is.thenStmt) && leavesMethod(is.elseStmt);
        }
        if (last instanceof TryStmt) {
            TryStmt ts = (TryStmt) last;
            if (ts.finallyBlock != null && leavesMethod(ts.finallyBlock)) {
                return true;
            }
            if (!leavesMethod(ts.tryBlock)) {
                return false;
            }
            for (CatchClause cc : ts.catches) {
                if (!leavesMethod(cc.body)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // ==================================================================
    // Statements
    // ==================================================================

    private void emitStatements(Block b, Ctx ctx) {
        for (Stmt s : b.statements) {
            emitStatement(s, ctx);
        }
    }

    private void emitStatement(Stmt s, Ctx ctx) {
        if (s != null && !(s instanceof Block)) {
            lineMark(s, ctx);
        }
        if (s != null && s.labels != null && !isLoop(s)) {
            // A labeled non-loop statement is a labeled Java block, which `break label;`
            // leaves. A loop carries its label on its own header (loopLabel), where
            // `continue label;` can reach it too.
            Ctx.Writer w = ctx.writer();
            w.line(loopLabel(s) + "{");
            ctx.indent(1);
            emitStatementBody(s, ctx);
            ctx.indent(-1);
            w.line("}");
            return;
        }
        emitStatementBody(s, ctx);
    }

    /**
     * A {@code late} local. Dart runs its initializer on the first read, not at the
     * declaration, and a read before any assignment (no initializer) throws
     * LateInitializationError; `late` used to be dropped, so the initializer ran eagerly
     * and an unassigned read answered 0. The local becomes a final runtime holder read
     * and written through get$v()/set$v(), the accessor shape every assignment form
     * already lowers (compound, ++, ??=), and that a closure captures as it is.
     */
    private void emitLateLocal(VarDeclStmt v, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        TypeRef declared = v.type != null && !v.type.is("var") ? v.type : null;
        String init = "null";
        TypeRef t = declared != null ? declared : TypeRef.DYNAMIC;
        if (v.initializer != null) {
            // Emitted into the initializer lambda, with whatever it lifts.
            ctx.pushWriter(ctx.currentIndent() + 1);
            Out o = emitExpr(v.initializer, declared, ctx);
            String lifted = ctx.popWriter();
            if (declared == null) {
                t = o.type != null ? o.type : TypeRef.DYNAMIC;
            }
            String value = coerce(o, t, ctx);
            init = lifted.isEmpty() ? "() -> " + value
                    : "() -> {\n" + lifted + indentStr(ctx.currentIndent() + 1) + "return " + value + ";\n"
                            + indentStr(ctx.currentIndent()) + "}";
        }
        String holder;
        if (t.is("int") && !t.nullable) {
            holder = "LateLong";
        } else if (t.is("double") && !t.nullable) {
            holder = "LateDouble";
        } else if (t.is("bool") && !t.nullable) {
            holder = "LateBool";
        } else {
            holder = "LateLocal";
        }
        ctx.importClass("dart.runtime." + holder);
        String jt = holder.equals("LateLocal") ? "LateLocal<" + javaType(t, true, ctx) + ">" : holder;
        String jn = ctx.declareLate(v.name, t);
        w.line("final " + jt + " " + jn + " = new " + holder + (holder.equals("LateLocal") ? "<>" : "")
                + "(" + quote(v.name) + ", " + v.isFinal + ", " + init + ");");
    }

    /**
     * Evaluates {@code e} for its effects only. What Java accepts as a statement (an
     * assignment, ++/--, a call, a throw) is emitted as one, exactly as an expression
     * statement is; anything else that may run code -- a getter, an operator, a lazy
     * top-level read -- is assigned to a discarded local. Only a literal, `this` or a
     * closure literal, which cannot, is dropped.
     */
    private void emitDiscardedValue(Expr e, Ctx ctx) {
        Expr core = stripParens(e);
        if (core instanceof IntLit || core instanceof DoubleLit || core instanceof BoolLit
                || core instanceof NullLit || core instanceof ThisExpr || core instanceof Lambda
                || (core instanceof StringLit && isConstantString((StringLit) core))
                || isPlainVariableRead(core, ctx)) {
            return;
        }
        if (core instanceof Assign || core instanceof IncDec || core instanceof Call
                || core instanceof CtorCall || core instanceof ThrowExpr || core instanceof Conditional
                || core instanceof Cascade || core instanceof AwaitExpr) {
            ExprStmt es = new ExprStmt();
            es.file = e.file;
            es.line = e.line;
            es.col = e.col;
            es.expr = core;
            emitStatementBody(es, ctx);
            return;
        }
        Out o = emitExpr(core, null, ctx);
        if (o.type != null && o.type.is("void")) {
            String code = statementize(o.code);
            if (!code.isEmpty()) {
                ctx.writer().line(code + ";");
            }
            return;
        }
        ctx.writer().line("Object $unused" + (unusedCounter++) + " = " + o.code + ";");
    }

    /**
     * A read of a local, or of a field of the class being emitted, that runs no code:
     * not a late local (its first read runs the initializer or throws), not a lazily
     * initialised or late-checked field (read through an accessor), not a mixin's field.
     */
    private boolean isPlainVariableRead(Expr e, Ctx ctx) {
        if (!(e instanceof Ident)) {
            return false;
        }
        String n = ((Ident) e).name;
        if (ctx.lookup(n) != null) {
            return !ctx.isLate(ctx.javaNameOf(n));
        }
        ClassDecl cc = ctx.currentClass;
        FieldDecl f = cc == null ? null : cc.field(n);
        return f != null && !cc.isMixin && !isLazyStatic(f) && !isLateChecked(f, cc);
    }

    private static Expr stripParens(Expr e) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        return e;
    }

    private static boolean isConstantString(StringLit s) {
        for (Object part : s.parts) {
            if (part instanceof Expr) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoop(Stmt s) {
        return s instanceof WhileStmt || s instanceof ForStmt || s instanceof ForInStmt;
    }

    /** The Java label prefix ({@code "L$outer: "}) a statement's header carries, or "". */
    private static String loopLabel(Stmt s) {
        if (s.labels == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String l : s.labels) {
            sb.append(javaLabel(l)).append(": ");
        }
        return sb.toString();
    }

    /** Java label for a Dart one; prefixed so a Dart label can never be a Java keyword. */
    private static String javaLabel(String dartLabel) {
        return "L$" + dartLabel;
    }

    private void emitStatementBody(Stmt s, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (s instanceof Block) {
            w.line("{");
            ctx.indent(1);
            ctx.pushScope();
            emitStatements((Block) s, ctx);
            ctx.popScope();
            ctx.indent(-1);
            w.line("}");
        } else if (s instanceof VarDeclStmt && ((VarDeclStmt) s).isLate) {
            emitLateLocal((VarDeclStmt) s, ctx);
        } else if (s instanceof VarDeclStmt) {
            VarDeclStmt v = (VarDeclStmt) s;
            TypeRef declared = v.type;
            Out init = null;
            TypeRef asyncFn = (declared == null || declared.is("var")) && v.initializer instanceof Lambda
                    ? asyncClosureType((Lambda) v.initializer, v.name, ctx) : null;
            if (v.initializer != null) {
                tearOffKeepAll = (declared == null || declared.is("var")) && v.initializer instanceof Ident;
                init = emitExpr(v.initializer, asyncFn != null ? asyncFn
                        : declared != null && !declared.is("var") ? declared : null, ctx);
            }
            TypeRef t = asyncFn != null ? asyncFn : declared == null || declared.is("var")
                    ? (init != null ? init.type : TypeRef.DYNAMIC) : declared;
            // untyped closure locals get a SAM type by arity. A declared signature
            // (`int Function() f = ...`) has its own SAM type below; it was forced to
            // VoidFunc0 here, which a value-returning closure does not compile against.
            if (t.is("Function") && t.funcReturn == null && v.initializer instanceof Lambda
                    && !((Lambda) v.initializer).isAsync) {
                // An untyped closure is typed from its body, as Dart infers it: a closure
                // that yields a value can be called for it (it was forced to VoidFunc0, and
                // one with parameters was an error).
                t = inferredClosureType((Lambda) v.initializer, v.name, ctx);
            }
            if (t.is("Function") && t.funcReturn == null && v.initializer instanceof Lambda) {
                Lambda l = (Lambda) v.initializer;
                if (l.params.isEmpty()) {
                    ctx.importClass("dart.runtime.Funcs");
                    String jn = ctx.declareShadowSafe(v.name, t);
                    w.line("Funcs.VoidFunc0 " + jn + " = " + init.code + ";");
                    return;
                }
                diags.error(v, "E0138", "Annotate this closure variable's type (only zero-arg closures are inferred in M1)");
            }
            String jn = ctx.declareShadowSafe(v.name, t);
            if (ctx.boxedLocals.contains(v.name)) {
                ctx.markBoxed(v.name);
                String holder = refHolder(t, ctx);
                String initCode = init != null ? coerce(init, t, ctx) : zeroValue(t);
                w.line("final " + holder + " " + jn + " = new " + holder.split("<")[0]
                        + (holder.startsWith("Ref<") ? "<>" : "") + "(" + initCode + ");");
                return;
            }
            if ((v.type == null || v.type.is("var")) && init != null && containsDynamic(t)
                    && !(v.initializer instanceof Lambda)) {
                // let javac infer generics the Dart-side inference doesn't track
                w.line("var " + jn + " = " + init.code + ";");
                return;
            }
            String jt = javaType(t, false, ctx);
            if (init != null) {
                w.line(jt + " " + jn + " = " + coerce(init, t, ctx) + ";");
            } else {
                w.line(jt + " " + jn + " = " + zeroValue(t) + ";");
            }
        } else if (s instanceof VarDeclGroup) {
            for (VarDeclStmt v : ((VarDeclGroup) s).decls) {
                emitStatement(v, ctx);
            }
        } else if (s instanceof ExprStmt) {
            Expr ex = ((ExprStmt) s).expr;
            if (ex instanceof ThrowExpr) {
                ctx.importClass("dart.runtime.DartRuntime");
                Out v = emitExpr(((ThrowExpr) ex).value, null, ctx);
                w.line("throw DartRuntime.asError(" + v.code + ");");
                return;
            }
            // A conditional used as a statement (`cond ? f() : g();`) -- when its arms are void
            // (e.g. controller.reverse()/forward()) the ternary is not a valid Java expression
            // statement, so lower it to an if/else.
            if (ex instanceof Conditional) {
                Conditional c = (Conditional) ex;
                // Each arm's lifted statements are kept apart and placed inside its own
                // branch; when the arms are not void they are discarded, since the
                // conditional is emitted again below as an ordinary expression.
                Lazy thenL = emitLazyOperand(c.thenExpr, null, ctx, 1);
                Lazy elseL = emitLazyOperand(c.elseExpr, null, ctx, 1);
                Out thenO = thenL.out;
                Out elseO = elseL.out;
                boolean voidArms = (thenO.type != null && thenO.type.is("void"))
                        || (elseO.type != null && elseO.type.is("void"));
                if (voidArms) {
                    Out cond = emitExpr(c.condition, TypeRef.BOOL, ctx);
                    w.line("if (" + cond.code + ") {");
                    w.raw(thenL.lifted);
                    ctx.indent(1);
                    String tc = statementize(thenO.code);
                    if (!tc.isEmpty()) {
                        w.line(tc + ";");
                    }
                    ctx.indent(-1);
                    w.line("} else {");
                    w.raw(elseL.lifted);
                    ctx.indent(1);
                    String ec = statementize(elseO.code);
                    if (!ec.isEmpty()) {
                        w.line(ec + ";");
                    }
                    ctx.indent(-1);
                    w.line("}");
                    return;
                }
            }
            if (ex instanceof Assign && isNullAwareMemberAssign((Assign) ex) && !needsShort((Assign) ex)) {
                emitNullAwareAssignStatement((Assign) ex, ctx);
                return;
            }
            Out o = emitStatementExpr(ex, ctx);
            String code = statementize(o.code);
            if (!code.isEmpty()) {
                w.line(code + ";");
            }
        } else if (s instanceof YieldStmt) {
            YieldStmt y = (YieldStmt) s;
            if (ctx.asyncStarSink != null) {
                if (y.star) {
                    Out o = emitExpr(y.value, null, ctx);
                    w.line(ctx.asyncStarSink + ".yieldAll(" + o.code + ");");
                } else {
                    Out o = emitExpr(y.value, ctx.syncStarElem, ctx);
                    w.line(ctx.asyncStarSink + ".yield$(" + coerce(o, ctx.syncStarElem, ctx) + ");");
                }
            } else if (ctx.syncStarList == null) {
                diags.error(y, "E0304", "yield outside a generator body");
            } else if (y.star) {
                Out o = emitExpr(y.value, null, ctx);
                w.line(ctx.syncStarList + ".addAllIterable(" + mixinIterableCode(o) + ");");
            } else {
                Out o = emitExpr(y.value, ctx.syncStarElem, ctx);
                w.line(ctx.syncStarList + ".add(" + coerce(o, ctx.syncStarElem, ctx) + ");");
            }
        } else if (s instanceof ReturnStmt) {
            ReturnStmt r = (ReturnStmt) s;
            TypeRef rt = ctx.methodReturnType;
            if (ctx.inAsyncBody && rt != null && (rt.is("Future") || rt.is("FutureOr"))) {
                // async body: returned values wrap into a completed Future
                ctx.importClass("dart.async.Future");
                TypeRef inner = rt.args.isEmpty() ? TypeRef.DYNAMIC : rt.arg(0);
                if (r.value == null) {
                    w.line("return Future.value(null);");
                } else {
                    Out o = emitExpr(r.value, inner, ctx);
                    // await already unwraps; returning a Future directly passes through
                    w.line("return " + asyncReturnValue(o, inner, ctx) + ";");
                }
                return;
            }
            if (r.value == null) {
                w.line(ctx.bareReturnIsNull ? "return null;" : "return;");
            } else if (rt != null && rt.is("void")) {
                // Dart allows `return expr;` from a method Java-typed void (it overrides a void
                // base). The value is discarded but the expression still runs: `return n++;`
                // and `return x = 5;` were dropped because their text had no '('.
                emitDiscardedValue(r.value, ctx);
                if (!(stripParens(r.value) instanceof ThrowExpr)) {
                    w.line("return;");
                }
            } else if (ctx.narrowReturnToInt) {
                Out o = emitExpr(r.value, null, ctx);
                w.line("return (int) (" + o.code + ");");
            } else {
                Out o = emitExpr(r.value, rt, ctx);
                if (rt == null && ctx.lambdaReturns != null) {
                    ctx.lambdaReturns.add(o.type);
                }
                w.line("return " + (rt != null ? coerce(o, rt, ctx) : o.code) + ";");
            }
        } else if (s instanceof PatternDeclStmt) {
            PatternDeclStmt pd = (PatternDeclStmt) s;
            Out init = emitExpr(pd.init, null, ctx);
            TypeRef it = init.type != null ? init.type : TypeRef.DYNAMIC;
            String tmp = ctx.newTemp();
            w.line(javaType(it, true, ctx) + " " + tmp + " = " + init.code + ";");
            ctx.declare(tmp, it);
            List<String> binds = new ArrayList<String>();
            patternMatch(pd.pattern, tmp, it, ctx, binds);
            for (String b : binds) {
                w.line(b);
            }
        } else if (s instanceof RethrowStmt) {
            if (ctx.rethrowTarget == null) {
                diags.error(s, "E0115", "rethrow is only allowed inside a catch clause");
            } else {
                ctx.importClass("dart.runtime.DartRuntime");
                w.line("throw DartRuntime.rethrow(" + ctx.rethrowTarget + ");");
            }
        } else if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            w.line("try {");
            ctx.indent(1);
            ctx.pushScope();
            emitStatements(t.tryBlock, ctx);
            ctx.popScope();
            ctx.indent(-1);
            if (!t.catches.isEmpty()) {
                emitCatchChain(t.catches, ctx);
            }
            if (t.finallyBlock != null) {
                w.line("} finally {");
                ctx.indent(1);
                ctx.pushScope();
                emitStatements(t.finallyBlock, ctx);
                ctx.popScope();
                ctx.indent(-1);
            }
            w.line("}");
        } else if (s instanceof IfStmt && ((IfStmt) s).casePattern != null) {
            emitIfCaseStmt((IfStmt) s, ctx);
        } else if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            Out c = emitExpr(i.condition, TypeRef.BOOL, ctx);
            w.line("if (" + c.code + ") {");
            ctx.indent(1);
            ctx.pushScope();
            // `if (x is T)` flow-promotes x to T inside the then-branch.
            List<Object[]> undo = applyGuardPromotions(i.condition, ctx);
            emitStatement(unwrapBlock(i.thenStmt), ctx);
            restorePromotions(undo, ctx);
            ctx.popScope();
            ctx.indent(-1);
            // `x is! T` false in the else branch, and after the if when the then branch
            // always leaves (`if (x is! T) return ...;`): x is a T from there on.
            boolean thenLeaves = stmtTerminates(i.thenStmt);
            if (i.elseStmt != null) {
                w.line("} else {");
                ctx.indent(1);
                ctx.pushScope();
                List<Object[]> neg = new ArrayList<Object[]>();
                collectNegativePromotions(i.condition, ctx, neg);
                emitStatement(unwrapBlock(i.elseStmt), ctx);
                restorePromotions(neg, ctx);
                ctx.popScope();
                ctx.indent(-1);
            }
            w.line("}");
            if (thenLeaves && i.elseStmt == null) {
                List<Object[]> neg = new ArrayList<Object[]>();
                collectNegativePromotions(i.condition, ctx, neg);
                if (!ctx.promoteForScope(neg)) {
                    restorePromotions(neg, ctx);
                }
            }
        } else if (s instanceof WhileStmt) {
            WhileStmt wh = (WhileStmt) s;
            Lazy c = emitLazyOperand(wh.condition, TypeRef.BOOL, ctx, 1);
            if (wh.isDo) {
                // do/while: skip the test on the first pass only. A flag rather than Java's
                // do/while keeps `continue` reaching the (possibly lifted) test and the
                // condition's lifted statements inside the loop.
                String first = ctx.newTemp();
                w.line("boolean " + first + " = true;");
                w.line(loopLabel(s) + "while (true) {");
                ctx.indent(1);
                w.line("if (!" + first + ") {");
                w.raw(c.lifted);
                w.line("    if (!(" + c.out.code + ")) {");
                w.line("        break;");
                w.line("    }");
                w.line("}");
                w.line(first + " = false;");
            } else if (!c.lifts()) {
                w.line(loopLabel(s) + "while (" + c.out.code + ") {");
                ctx.indent(1);
            } else {
                // The condition's lifted statements belong to every test, not to the
                // first one only: run them at the top of each pass, then test.
                w.line(loopLabel(s) + "while (true) {");
                w.raw(c.lifted);
                ctx.indent(1);
                w.line("if (!(" + c.out.code + ")) {");
                w.line("    break;");
                w.line("}");
            }
            ctx.pushScope();
            ctx.pushBreakTarget(null);
            emitStatement(unwrapBlock(wh.body), ctx);
            ctx.popBreakTarget();
            ctx.popScope();
            ctx.indent(-1);
            w.line("}");
        } else if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            ctx.pushScope();
            // lift the init before the loop; conditions/updates must be lift-free in M1
            String initCode = "";
            String forVarDart = null;
            String forVarJava = null;
            TypeRef forVarType = null;
            if (f.init instanceof VarDeclStmt) {
                VarDeclStmt v = (VarDeclStmt) f.init;
                Out init = v.initializer != null ? emitExpr(v.initializer, v.type, ctx) : null;
                TypeRef t = v.type == null || v.type.is("var")
                        ? (init != null ? init.type : TypeRef.DYNAMIC) : v.type;
                String loopVar = ctx.declareShadowSafe(v.name, t);
                forVarDart = v.name;
                forVarJava = loopVar;
                forVarType = t;
                initCode = javaType(t, false, ctx) + " " + loopVar + " = "
                        + (init != null ? coerce(init, t, ctx) : zeroValue(t));
            } else if (f.init instanceof ExprStmt) {
                initCode = statementize(emitStatementExpr(((ExprStmt) f.init).expr, ctx).code);
            }
            final String forVarDart$ = forVarDart;
            final String forVarJava$ = forVarJava;
            final TypeRef forVarType$ = forVarType;
            final ForStmt f$ = f;
            emitCForLoop(loopLabel(s), initCode, f.condition, f.updates, ctx, new Runnable() {
                public void run() {
                    // Dart binds the loop variable fresh each iteration, so a closure in the
                    // body captures a distinct value per pass. The Java loop variable is
                    // reassigned by the update clause (not effectively final), so emit a
                    // per-iteration final alias and route body references through it.
                    if (forVarDart$ != null && CaptureScan.readInLambda(f$.body, forVarDart$)) {
                        String alias = ctx.declareShadowSafe(forVarDart$, forVarType$);
                        ctx.writer().line("final " + javaType(forVarType$, false, ctx) + " " + alias
                                + " = " + forVarJava$ + ";");
                    }
                    ctx.pushBreakTarget(null);
                    emitStatement(unwrapBlock(f$.body), ctx);
                    ctx.popBreakTarget();
                }
            });
            ctx.popScope();
        } else if (s instanceof ForInStmt && ((ForInStmt) s).isAwait) {
            // `await for (x in stream) body`: a StreamIterator whose next() parks like an
            // await until the next event, and is cancelled however the loop is left.
            ForInStmt f = (ForInStmt) s;
            ctx.importClass("dart.async.StreamIterator");
            Out iter = emitExpr(f.iterable, null, ctx);
            TypeRef elem = f.varType != null && !f.varType.is("var")
                    ? f.varType
                    : (iter.type != null && iter.type.is("Stream") && !iter.type.args.isEmpty()
                        ? iter.type.arg(0) : TypeRef.DYNAMIC);
            String ej = javaType(elem, true, ctx);
            String it = ctx.newTemp();
            String src = iter.code;
            if (isDynamic(elem)) {
                // The element type is unknown here (a call through a local function, say)
                // while the Java value may be a Stream<String>; read it as Stream<Object>.
                ctx.importClass("dart.async.Stream");
                src = "(Stream<Object>) (Stream<?>) " + paren(src);
            }
            w.line("StreamIterator<" + ej + "> " + it + " = new StreamIterator<" + ej + ">(" + src + ");");
            w.line("try {");
            ctx.indent(1);
            ctx.pushScope();
            ctx.pushBreakTarget(null);
            w.line(loopLabel(s) + "while (" + it + ".next()) {");
            ctx.indent(1);
            if (f.pattern != null) {
                String loopVar = ctx.newTemp();
                w.line(ej + " " + loopVar + " = " + it + ".current();");
                ctx.declare(loopVar, elem);
                List<String> binds = new ArrayList<String>();
                patternMatch(f.pattern, loopVar, elem, ctx, binds);
                for (String bind : binds) {
                    w.line(bind);
                }
            } else {
                String loopVar = ctx.declareShadowSafe(f.varName, elem);
                w.line(ej + " " + loopVar + " = " + it + ".current();");
            }
            emitStatement(unwrapBlock(f.body), ctx);
            ctx.indent(-1);
            w.line("}");
            ctx.popBreakTarget();
            ctx.popScope();
            ctx.indent(-1);
            w.line("} finally {");
            w.line("    " + it + ".cancel();");
            w.line("}");
        } else if (s instanceof ForInStmt
                && isIndexedRecordFor(((ForInStmt) s).pattern, ((ForInStmt) s).iterable)) {
            // `for (final (int i, E e) in xs.indexed)` -- Dart's Iterable.indexed pairs each
            // element with its position. There is no runtime `indexed`, so lower to a counted
            // loop that binds the index and element subpatterns directly.
            final ForInStmt f = (ForInStmt) s;
            emitIndexedFor(loopLabel(s), f.pattern, f.iterable, ctx, new Runnable() {
                public void run() {
                    ctx.pushBreakTarget(null);
                    emitStatement(unwrapBlock(f.body), ctx);
                    ctx.popBreakTarget();
                }
            });
        } else if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            Out iter = emitExpr(f.iterable, null, ctx);
            if (f.iterable instanceof ThisExpr && ctx.currentClass != null && ctx.extensionSelfType == null) {
                // `for (e in this)` inside `mixin M on Iterable<E>`: the mixin is a Java
                // interface, so `this` is iterated as the core collection it is applied to.
                TypeRef coreOn = coreCollectionOn(ctx.currentClass);
                if (coreOn != null) {
                    iter = coreSelf(coreOn, ctx);
                }
            }
            TypeRef elem = f.varType != null && !f.varType.is("var")
                    ? f.varType
                    : (iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC);
            ctx.pushScope();
            ctx.pushBreakTarget(null);
            if (f.pattern != null) {
                // Dart 3 pattern for-in: bind a temp per element, then destructure into the pattern.
                String loopVar = ctx.newTemp();
                w.line(loopLabel(s) + "for (" + javaType(elem, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                ctx.declare(loopVar, elem);
                List<String> binds = new ArrayList<String>();
                patternMatch(f.pattern, loopVar, elem, ctx, binds);
                for (String b : binds) {
                    w.line(b);
                }
                emitStatement(unwrapBlock(f.body), ctx);
                ctx.indent(-1);
                w.line("}");
            } else {
                String loopVar = ctx.declareShadowSafe(f.varName, elem);
                w.line(loopLabel(s) + "for (" + javaType(elem, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                emitStatement(unwrapBlock(f.body), ctx);
                ctx.indent(-1);
                w.line("}");
            }
            ctx.popBreakTarget();
            ctx.popScope();
        } else if (s instanceof BreakStmt) {
            String bl = ((BreakStmt) s).label != null ? javaLabel(((BreakStmt) s).label)
                    : ctx.currentBreakLabel();
            w.line(bl != null ? "break " + bl + ";" : "break;");
        } else if (s instanceof ContinueStmt) {
            String cl = ((ContinueStmt) s).label;
            w.line(cl != null ? "continue " + javaLabel(cl) + ";" : "continue;");
        } else if (s instanceof SwitchStmt) {
            emitSwitchStmt((SwitchStmt) s, ctx);
        } else if (s instanceof Ast.LocalFunc) {
            emitLocalFunc((Ast.LocalFunc) s, ctx);
        } else if (s != null) {
            diags.error(s, "E0127", "Unsupported statement in emitter");
        }
    }

    /**
     * A nested function declaration, lowered to a local variable holding a lambda
     * bound to the matching {@code Funcs.*} functional interface. Registering the
     * local in scope lets later {@code name(args)} calls (via {@code emitBareCall})
     * and bare {@code name} tear-offs (via {@code emitIdent}) resolve against it.
     */
    private void emitLocalFunc(Ast.LocalFunc lf, Ctx ctx) {
        int arity = lf.params.size();
        if (arity > 5) {
            diags.error(lf, "E0139", "Nested functions with more than 5 parameters are not supported yet");
            return;
        }
        String samType = funcSamType(lf.returnType, lf.params, ctx);
        // Reuse the lambda machinery (capture/box handling, param typing) by
        // building an equivalent Lambda and emitting it against the SAM type.
        Lambda l = new Lambda();
        l.file = lf.file;
        l.line = lf.line;
        l.col = lf.col;
        l.isAsync = lf.isAsync;
        l.isSyncStar = lf.isSyncStar;
        l.isAsyncStar = lf.isAsyncStar;
        l.params = lf.params;
        l.body = lf.body;
        l.exprBody = lf.exprBody;
        // Declare the local first so a recursive body can reference the name.
        // Declared with its real signature, so a call of it is typed by its return type
        // (`stopAll().whenComplete(...)` binds statically instead of through Dyn).
        TypeRef expected = funcTypeRef(lf.returnType, lf.params);
        String jn = ctx.declareShadowSafe(lf.name, funcTypeRef(lf.returnType, lf.params));
        if (lf.isSyncStar || lf.isAsyncStar) {
            // A generator's element type comes from its declared Iterable<T>/Stream<T>,
            // which the generator lowering reads from the target's return type.
            expected.funcParams = new ArrayList<TypeRef>();
            for (Param p : lf.params) {
                expected.funcParams.add(paramValueType(p));
            }
            expected.funcReturn = lf.returnType;
        }
        Out init = emitExpr(l, expected, ctx);
        ctx.writer().line(samType + " " + jn + " = " + init.code + ";");
    }

    /** The {@code Funcs.*} functional-interface Java type for an inline function-type signature. */
    private String funcSamTypeFromRefs(List<TypeRef> params, TypeRef ret, Ctx ctx) {
        ctx.importClass("dart.runtime.Funcs");
        int arity = params == null ? 0 : params.size();
        boolean voidRet = ret == null || ret.is("void");
        if (voidRet) {
            if (arity == 0) {
                return "Funcs.VoidFunc0";
            }
            StringBuilder sb = new StringBuilder("Funcs.VoidFunc").append(arity).append('<');
            for (int i = 0; i < arity; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(javaType(params.get(i), true, ctx));
            }
            return sb.append('>').toString();
        }
        TypeRef r = ret.is("var") || ret.is("dynamic") ? TypeRef.DYNAMIC : ret;
        StringBuilder sb = new StringBuilder("Funcs.Func").append(arity).append('<');
        for (int i = 0; i < arity; i++) {
            sb.append(javaType(params.get(i), true, ctx)).append(", ");
        }
        sb.append(javaType(r, true, ctx));
        return sb.append('>').toString();
    }

    /** The {@code Funcs.*} functional-interface Java type for a function shape. */
    private String funcSamType(TypeRef ret, List<Param> params, Ctx ctx) {
        ctx.importClass("dart.runtime.Funcs");
        int arity = params.size();
        boolean voidRet = ret == null || ret.is("void");
        if (voidRet) {
            if (arity == 0) {
                return "Funcs.VoidFunc0";
            }
            StringBuilder sb = new StringBuilder("Funcs.VoidFunc").append(arity).append('<');
            for (int i = 0; i < arity; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(javaType(paramValueType(params.get(i)), true, ctx));
            }
            return sb.append('>').toString();
        }
        TypeRef r = ret.is("var") || ret.is("dynamic") ? TypeRef.DYNAMIC : ret;
        StringBuilder sb = new StringBuilder("Funcs.Func").append(arity).append('<');
        for (int i = 0; i < arity; i++) {
            sb.append(javaType(paramValueType(params.get(i)), true, ctx)).append(", ");
        }
        sb.append(javaType(r, true, ctx));
        return sb.append('>').toString();
    }

    /** A named typedef-shaped {@link TypeRef} used to give lambda params their real types. */
    private TypeRef funcTypeRef(TypeRef ret, List<Param> params) {
        // Not a registered typedef name, but emitLambda only reads typedefSig(expected.name);
        // an unregistered name yields null there, which is fine -- params carry their own types.
        // The signature is spelled out: a BARE Function target is an Object-erased slot to
        // emitLambda, which would bind the closure to an all-dynamic Func SAM that the local's
        // declared VoidFunc/Func type does not accept.
        TypeRef t = new TypeRef("Function");
        t.funcParams = new ArrayList<TypeRef>();
        for (Param p : params) {
            t.funcParams.add(paramValueType(p));
        }
        t.funcReturn = ret != null && !ret.is("var") ? ret : TypeRef.DYNAMIC;
        return t;
    }

    private static TypeRef paramValueType(Param p) {
        return p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
    }

    // ------------------------------------------------------------------
    // Dart 3: switch statements / expressions + pattern matching
    // ------------------------------------------------------------------

    /**
     * Lowers a switch statement to a labeled block of independent {@code if} tests. Cases do not fall
     * through (Dart semantics), so each match runs its body and breaks the label. A {@code when} guard
     * is a nested test inside the matched block, so a matched-but-guard-failed case falls through to
     * the following cases.
     */
    /**
     * Lowers a Dart 3 if-case statement {@code if (e case p [when g]) S1 else S2}. The scrutinee is
     * lifted into a temp, the pattern match becomes the condition (with its bindings in scope for the
     * guard and the then-branch), and a match-flag routes a failed match/guard to the else-branch.
     */
    private void emitIfCaseStmt(IfStmt i, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        w.line("{");
        ctx.indent(1);
        ctx.pushScope();
        Out subj = emitExpr(i.condition, null, ctx);
        String temp = ctx.newTemp();
        w.line(javaType(subj.type, true, ctx) + " " + temp + " = " + subj.code + ";");
        ctx.declare(temp, subj.type);
        List<String> binds = new ArrayList<String>();
        String cond = patternTest(i.casePattern, temp, subj.type, ctx, binds);
        boolean guarded = i.caseGuard != null;
        // Emit a structural if/else so javac's definite-return analysis holds. A guard that fails must
        // route to the else-branch, which requires emitting the else in two spots (pattern miss and
        // guard miss). The common unguarded case emits it once.
        w.line("if (" + cond + ") {");
        ctx.indent(1);
        ctx.pushScope();
        for (String b : binds) {
            w.line(b);
        }
        if (guarded) {
            Out g = emitExpr(i.caseGuard, TypeRef.BOOL, ctx);
            w.line("if (" + g.code + ") {");
            ctx.indent(1);
            ctx.pushScope();
            emitStatement(unwrapBlock(i.thenStmt), ctx);
            ctx.popScope();
            ctx.indent(-1);
            if (i.elseStmt != null) {
                w.line("} else {");
                ctx.indent(1);
                ctx.pushScope();
                emitStatement(unwrapBlock(i.elseStmt), ctx);
                ctx.popScope();
                ctx.indent(-1);
            }
            w.line("}");
        } else {
            emitStatement(unwrapBlock(i.thenStmt), ctx);
        }
        ctx.popScope();
        ctx.indent(-1);
        if (i.elseStmt != null) {
            w.line("} else {");
            ctx.indent(1);
            ctx.pushScope();
            emitStatement(unwrapBlock(i.elseStmt), ctx);
            ctx.popScope();
            ctx.indent(-1);
        }
        w.line("}");
        ctx.popScope();
        ctx.indent(-1);
        w.line("}");
    }

    private void emitSwitchStmt(SwitchStmt sw, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        Out subj = emitExpr(sw.subject, null, ctx);
        String s = ctx.newTemp();
        String label = "$sw" + s.substring(2);
        ctx.pushScope();
        w.line(javaType(subj.type, true, ctx) + " " + s + " = " + subj.code + ";");
        ctx.declare(s, subj.type);
        List<Pattern> casePatterns = new ArrayList<Pattern>();
        for (SwitchCase c : sw.cases) {
            if (!c.isDefault) {
                casePatterns.add(c.pattern);
            }
        }
        Map<String, String[]> savedCache = openSwitchFieldCache(casePatterns, s, ctx);
        w.line(label + ": {");
        ctx.indent(1);
        ctx.pushBreakTarget(label);
        SwitchCase defaultCase = null;
        // Tracks whether control can leave the switch block normally (a case that `break`s to
        // after the block rather than returning/throwing). When every case returns and there is
        // no default, a Dart-exhaustive switch leaves the block unreachable -- a trailing throw
        // then keeps a value-returning method/lambda definitely-assigned in Java.
        boolean anyFallThrough = false;
        boolean hasContentCase = false;
        // an empty non-default case falls through to the next case's body (Dart's only fallthrough)
        List<String> pending = new ArrayList<String>();
        List<SwitchCase> pendingCases = new ArrayList<SwitchCase>();
        for (SwitchCase c : sw.cases) {
            if (c.isDefault) {
                defaultCase = c;
                continue;
            }
            if (c.body.isEmpty()) {
                pendingCases.add(c);
                continue;
            }
            if (needsMatchFlag(pendingCases, c)) {
                // Each case keeps its own guard: `case 1: case 2 when g:` shares a body, and
                // g decides case 2 alone. Folding the patterns into one condition put g on
                // case 1 too, and an empty guarded case got a body of its own (a break).
                hasContentCase = true;
                if (emitSharedBodyCases(pendingCases, c, s, subj.type, label, ctx)) {
                    anyFallThrough = true;
                }
                pendingCases.clear();
                continue;
            }
            for (SwitchCase pc : pendingCases) {
                ctx.pushScope();
                List<String> pb = new ArrayList<String>();
                pending.add(patternTest(pc.pattern, s, subj.type, ctx, pb));
                ctx.popScope();
            }
            pendingCases.clear();
            ctx.pushScope();
            List<String> binds = new ArrayList<String>();
            String cond = patternTest(c.pattern, s, subj.type, ctx, binds);
            String full = cond;
            if (!pending.isEmpty()) {
                StringBuilder sb = new StringBuilder("(");
                for (String pc : pending) {
                    sb.append(pc).append(" || ");
                }
                full = sb.append(cond).append(")").toString();
                pending.clear();
            }
            w.line("if (" + full + ") {");
            ctx.indent(1);
            for (String b : binds) {
                w.line(b);
            }
            boolean guarded = c.guard != null;
            if (guarded) {
                Out g = emitExpr(c.guard, TypeRef.BOOL, ctx);
                w.line("if (" + g.code + ") {");
                ctx.indent(1);
            }
            hasContentCase = true;
            for (Stmt bs : c.body) {
                emitStatement(bs, ctx);
            }
            // only emit the implicit break when the body does not already jump (else Java flags it
            // as an unreachable statement)
            if (!endsWithTerminator(c.body)) {
                w.line("break " + label + ";");
                anyFallThrough = true;
            }
            if (guarded) {
                ctx.indent(-1);
                w.line("}");
            }
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        }
        if (defaultCase != null) {
            ctx.pushScope();
            for (Stmt bs : defaultCase.body) {
                emitStatement(bs, ctx);
            }
            ctx.popScope();
        }
        switchFieldCache = savedCache;
        ctx.popBreakTarget();
        ctx.indent(-1);
        w.line("}");
        // Dart-exhaustive switch (an enum subject, no default) where every arm returns/throws:
        // the post-block fall-through is unreachable in Dart, so emit an unreachable throw to
        // satisfy Java's definite-return analysis. Only for enum subjects -- a String/int switch is
        // never exhaustive and legitimately falls through to code after it.
        boolean enumSubject = subj.type != null
                && (program.enums.containsKey(subj.type.name) || stubs.isStubEnum(subj.type.name));
        if (defaultCase == null && hasContentCase && !anyFallThrough && enumSubject) {
            ctx.importClass("dart.runtime.DartRuntime");
            w.line("throw DartRuntime.asError(\"No matching switch case\");");
        }
        ctx.popScope();
    }

    /**
     * Whether a body shared by {@code pending} empty cases and {@code last} needs a match
     * flag: when any of them has a guard or binds, a single folded condition would apply
     * one case's guard (or bindings) to the others.
     */
    private boolean needsMatchFlag(List<SwitchCase> pending, SwitchCase last) {
        if (pending.isEmpty()) {
            return false;
        }
        if (last.guard != null || patternBinds(last.pattern)) {
            return true;
        }
        for (SwitchCase pc : pending) {
            if (pc.guard != null || patternBinds(pc.pattern)) {
                return true;
            }
        }
        return false;
    }

    /** True when a pattern declares a variable anywhere inside it. */
    private boolean patternBinds(Pattern p) {
        if (p instanceof VariablePattern) {
            return !((VariablePattern) p).wildcard;
        }
        if (p instanceof CastPattern) {
            return patternBinds(((CastPattern) p).inner);
        }
        if (p instanceof NullCheckPattern) {
            return patternBinds(((NullCheckPattern) p).inner);
        }
        if (p instanceof ObjectPattern) {
            for (PatternField f : ((ObjectPattern) p).fields) {
                if (patternBinds(f.pattern)) {
                    return true;
                }
            }
            return false;
        }
        if (p instanceof AndPattern) {
            for (Pattern part : ((AndPattern) p).parts) {
                if (patternBinds(part)) {
                    return true;
                }
            }
            return false;
        }
        if (p instanceof OrPattern) {
            for (Pattern part : ((OrPattern) p).alternatives) {
                if (patternBinds(part)) {
                    return true;
                }
            }
            return false;
        }
        if (p instanceof ListPattern) {
            for (Pattern part : ((ListPattern) p).elements) {
                if (patternBinds(part)) {
                    return true;
                }
            }
            return false;
        }
        if (p instanceof RecordPattern) {
            for (PatternField f : ((RecordPattern) p).fields) {
                if (patternBinds(f.pattern)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Emits cases that share one body, each tested with its own pattern, bindings and guard
     * into a match flag, then the body once under the flag. A case's variables are visible to
     * its own guard only: Dart lets the shared body use none of them unless every case binds
     * it, which is not supported here. Returns whether control can leave the switch normally.
     */
    private boolean emitSharedBodyCases(List<SwitchCase> pending, SwitchCase last, String s,
                                        TypeRef subjType, String label, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        String m = ctx.newTemp();
        w.line("boolean " + m + " = false;");
        List<SwitchCase> all = new ArrayList<SwitchCase>(pending);
        all.add(last);
        boolean lastBinds = patternBinds(last.pattern);
        // Dart lets the shared body use a variable only when every case declares it (with
        // one type). Such variables are hoisted: declared once before the tests, assigned
        // by whichever case matched, and visible to the body under their Dart name.
        Map<String, String> hoisted = new LinkedHashMap<String, String>();
        Map<String, TypeRef> hoistedTypes = new LinkedHashMap<String, TypeRef>();
        for (int i = 0; i < all.size(); i++) {
            SwitchCase c = all.get(i);
            ctx.pushScope();
            List<String> binds = new ArrayList<String>();
            String cond = patternTest(c.pattern, s, subjType, ctx, binds);
            Map<String, TypeRef> caseVars = new LinkedHashMap<String, TypeRef>(
                    ctx.scopes.get(ctx.scopes.size() - 1));
            if (i == 0 && lastBinds) {
                for (Map.Entry<String, TypeRef> v : caseVars.entrySet()) {
                    String hn = javaIdent(v.getKey()) + "$shared" + (ctx.shadowCounter++);
                    TypeRef vt = v.getValue() == null ? TypeRef.DYNAMIC : v.getValue();
                    w.line(javaType(vt, false, ctx) + " " + hn + " = " + zeroValue(vt) + ";");
                    hoisted.put(v.getKey(), hn);
                    hoistedTypes.put(v.getKey(), vt);
                }
            }
            // A variable some case lacks is not in scope in the body (Dart's analyzer
            // rejects a use of it), so a case without one simply leaves it unassigned.
            String test = "true".equals(cond) ? ""
                    : cond.indexOf("||") >= 0 || cond.indexOf('?') >= 0 ? "(" + cond + ")" : cond;
            if (i > 0) {
                test = test.isEmpty() ? "!" + m : "!" + m + " && " + test;
            } else if (test.isEmpty()) {
                test = "true";
            }
            w.line("if (" + test + ") {");
            ctx.indent(1);
            for (String b : binds) {
                w.line(b);
            }
            if (c.guard != null) {
                Out g = emitExpr(c.guard, TypeRef.BOOL, ctx);
                w.line(m + " = " + g.code + ";");
            } else {
                w.line(m + " = true;");
            }
            for (Map.Entry<String, String> h : hoisted.entrySet()) {
                if (caseVars.containsKey(h.getKey())) {
                    w.line(h.getValue() + " = " + ctx.javaNameOf(h.getKey()) + ";");
                }
            }
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        }
        w.line("if (" + m + ") {");
        ctx.indent(1);
        ctx.pushScope();
        for (Map.Entry<String, String> h : hoisted.entrySet()) {
            ctx.declare(h.getKey(), hoistedTypes.get(h.getKey()));
            ctx.renameScopes.get(ctx.renameScopes.size() - 1).put(h.getKey(), h.getValue());
        }
        for (Stmt bs : last.body) {
            emitStatement(bs, ctx);
        }
        boolean leaves = false;
        if (!endsWithTerminator(last.body)) {
            w.line("break " + label + ";");
            leaves = true;
        }
        ctx.popScope();
        ctx.indent(-1);
        w.line("}");
        return leaves;
    }

    /** True when a statement list definitely transfers control (so a trailing break is unreachable). */
    private boolean endsWithTerminator(List<Stmt> body) {
        if (body.isEmpty()) {
            return false;
        }
        return stmtTerminates(body.get(body.size() - 1));
    }

    /**
     * True when a single statement definitely transfers control. Recurses into a
     * trailing block so `case x: { ...; break; }` (the gen-l10n locale lookup shape)
     * is recognized as terminating and doesn't get a second, unreachable break.
     */
    private boolean stmtTerminates(Stmt last) {
        if (last instanceof ReturnStmt || last instanceof BreakStmt || last instanceof ContinueStmt
                || last instanceof RethrowStmt) {
            return true;
        }
        if (last instanceof ExprStmt && ((ExprStmt) last).expr instanceof ThrowExpr) {
            return true;
        }
        if (last instanceof Block) {
            List<Stmt> ss = ((Block) last).statements;
            return !ss.isEmpty() && stmtTerminates(ss.get(ss.size() - 1));
        }
        // Java's own rules, which decide whether a statement after these is unreachable:
        // an if/else ends when both branches do, and a try when its finally does, or when
        // its body and every catch do. A Dart `on T` clause with no catch-all still ends:
        // what it does not match is rethrown.
        if (last instanceof IfStmt) {
            IfStmt is = (IfStmt) last;
            return is.casePattern == null && is.elseStmt != null
                    && stmtTerminates(is.thenStmt) && stmtTerminates(is.elseStmt);
        }
        if (last instanceof TryStmt) {
            TryStmt ts = (TryStmt) last;
            if (ts.finallyBlock != null && stmtTerminates(ts.finallyBlock)) {
                return true;
            }
            if (!stmtTerminates(ts.tryBlock)) {
                return false;
            }
            for (CatchClause cc : ts.catches) {
                if (!stmtTerminates(cc.body)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Reorders a class's fields so that a static field whose initializer reads
     * another static field of the same class is emitted after it. Dart evaluates
     * static initializers lazily (order-independent); Java runs them top-to-bottom,
     * so source order can make a static read a not-yet-initialized sibling as null.
     * Stable: fields with no unmet dependency keep their original relative order,
     * and any cycle falls back to source order (its remaining fields appended).
     */
    private List<FieldDecl> orderStaticFieldsByDependency(ClassDecl c) {
        List<FieldDecl> fields = c.fields;
        java.util.Set<String> staticNames = new HashSet<String>();
        for (FieldDecl f : fields) {
            if (f.isStatic) {
                staticNames.add(f.name);
            }
        }
        if (staticNames.isEmpty()) {
            return fields;
        }
        // Static methods of this class: a static field whose initializer calls one of them
        // can transitively read any static field (the method body isn't analyzed here), so it
        // must initialize only after every other static field -- covers the common
        // `themeData(colorScheme)` builder that reads sibling `_textTheme`/`_colorScheme` statics.
        java.util.Set<String> staticMethods = new HashSet<String>();
        for (MethodDecl m : c.methods) {
            if (m.isStatic) {
                staticMethods.add(m.name);
            }
        }
        int staticCount = staticNames.size();
        List<FieldDecl> pending = new ArrayList<FieldDecl>(fields);
        List<FieldDecl> ordered = new ArrayList<FieldDecl>(fields.size());
        java.util.Set<String> placed = new HashSet<String>();
        int placedStatics = 0;
        boolean progress = true;
        while (!pending.isEmpty() && progress) {
            progress = false;
            for (int i = 0; i < pending.size(); i++) {
                FieldDecl f = pending.get(i);
                boolean ready = true;
                if (f.isStatic && f.initializer != null) {
                    java.util.Set<String> refs = CaptureScan.referencedNames(f.initializer);
                    // direct dependency on a same-class static that is not yet placed
                    for (String ref : refs) {
                        if (!ref.equals(f.name) && staticNames.contains(ref) && !placed.contains(ref)) {
                            ready = false;
                            break;
                        }
                    }
                    // calls a same-class static method -> wait for every other static field
                    if (ready) {
                        for (String ref : refs) {
                            if (staticMethods.contains(ref) && placedStatics < staticCount - 1) {
                                ready = false;
                                break;
                            }
                        }
                    }
                }
                if (ready) {
                    ordered.add(f);
                    placed.add(f.name);
                    if (f.isStatic) {
                        placedStatics++;
                    }
                    pending.remove(i);
                    progress = true;
                    break;
                }
            }
        }
        // cycle (or a dependency that never resolves): keep the rest in source order
        ordered.addAll(pending);
        return ordered;
    }

    /**
     * Lowers a switch expression to a lifted result temp assigned inside a labeled block (the same
     * shape as a switch statement). A non-exhaustive switch that matches nothing throws, mirroring
     * Dart's runtime behavior.
     */
    private Out emitSwitchExpr(SwitchExpr sw, TypeRef expected, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        Out subj = emitExpr(sw.subject, null, ctx);
        TypeRef resultType = expected != null && !expected.is("var") && !expected.is("dynamic")
                ? expected : TypeRef.DYNAMIC;
        // No context type (e.g. `switch (t) {...}.present()`): infer a common result type
        // from the arms so a member access on the switch value resolves against a real type.
        if (resultType.is("dynamic")) {
            TypeRef common = inferSwitchResultType(sw, ctx);
            if (common != null) {
                resultType = common;
            }
        }
        String s = ctx.newTemp();
        String res = ctx.newTemp();
        String label = "$sw" + s.substring(2);
        ctx.pushScope();
        w.line(javaType(subj.type, true, ctx) + " " + s + " = " + subj.code + ";");
        ctx.declare(s, subj.type);
        w.line(javaType(resultType, true, ctx) + " " + res + ";");
        List<Pattern> casePatterns = new ArrayList<Pattern>();
        for (SwitchExprCase c : sw.cases) {
            if (!(c.isDefault && c.guard == null)) {
                casePatterns.add(c.pattern);
            }
        }
        Map<String, String[]> savedCache = openSwitchFieldCache(casePatterns, s, ctx);
        w.line(label + ": {");
        ctx.indent(1);
        SwitchExprCase defaultCase = null;
        for (SwitchExprCase c : sw.cases) {
            if (c.isDefault && c.guard == null) {
                defaultCase = c;      // emitted unconditionally, last (Dart requires it last anyway)
                continue;
            }
            ctx.pushScope();
            List<String> binds = new ArrayList<String>();
            String cond = patternTest(c.pattern, s, subj.type, ctx, binds);
            w.line("if (" + cond + ") {");
            ctx.indent(1);
            for (String b : binds) {
                w.line(b);
            }
            // `switch (w) { T1() => w.span }`: an object pattern promotes a local subject
            // to the pattern's type in the guard and the arm, as `w is T1` would.
            String promoName = null;
            TypeRef promoPrev = null;
            if (c.pattern instanceof ObjectPattern && ((ObjectPattern) c.pattern).type != null
                    && sw.subject instanceof Ident && ctx.lookup(((Ident) sw.subject).name) != null) {
                promoName = ((Ident) sw.subject).name;
                promoPrev = ctx.pushPromotion(promoName, ((ObjectPattern) c.pattern).type);
            }
            boolean guarded = c.guard != null;
            if (guarded) {
                Out g = emitExpr(c.guard, TypeRef.BOOL, ctx);
                w.line("if (" + g.code + ") {");
                ctx.indent(1);
            }
            Out v = emitExpr(c.value, resultType, ctx);
            if (promoName != null) {
                ctx.restorePromotion(promoName, promoPrev);
            }
            w.line(res + " = " + coerce(v, resultType, ctx) + ";");
            w.line("break " + label + ";");
            if (guarded) {
                ctx.indent(-1);
                w.line("}");
            }
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        }
        switchFieldCache = savedCache;
        if (defaultCase != null) {
            ctx.pushScope();
            Out v = emitExpr(defaultCase.value, resultType, ctx);
            w.line(res + " = " + coerce(v, resultType, ctx) + ";");
            ctx.popScope();
        } else {
            ctx.importClass("dart.runtime.DartRuntime");
            w.line("throw DartRuntime.asError(\"No matching switch expression case\");");
        }
        ctx.indent(-1);
        w.line("}");
        ctx.popScope();
        return new Out(res, resultType);
    }

    /**
     * A common static type for every arm of a switch expression, or null when the arms
     * disagree or an arm's type can't be inferred without side effects. Used only when the
     * switch appears without a context type; keeps a member access on the switch value
     * (e.g. {@code switch (t) {...}.present()}) resolvable.
     */
    private TypeRef inferSwitchResultType(SwitchExpr sw, Ctx ctx) {
        TypeRef common = null;
        for (SwitchExprCase c : sw.cases) {
            TypeRef at = inferExprTypeQuiet(c.value, ctx);
            if (at == null || at.is("dynamic") || at.is("void")) {
                return null;
            }
            if (common == null) {
                common = at;
            } else if (!common.name.equals(at.name)) {
                return null;
            }
        }
        return common;
    }

    /**
     * Best-effort static type of an expression without emitting it (no side effects). Handles
     * the simple cases needed for switch-arm unification &mdash; a local variable or a field
     * (own, or inherited from a program superclass) referenced by a bare identifier. Returns
     * null for anything it can't resolve cheaply.
     */
    /** Whether an untyped set literal is only spreads of maps, which makes it a map. */
    private boolean spreadsOnlyMaps(SetLit sl, TypeRef expected, Ctx ctx) {
        if (sl.elementType != null || sl.elements.isEmpty()) {
            return false;
        }
        for (Expr el : sl.elements) {
            if (!(el instanceof SpreadElement)) {
                return false;
            }
        }
        if (expected != null && expected.is("Map")) {
            return true;
        }
        TypeRef t = inferExprTypeQuiet(((SpreadElement) sl.elements.get(0)).expr, ctx);
        return t != null && t.is("Map");
    }

    private TypeRef inferExprTypeQuiet(Expr e, Ctx ctx) {
        if (e instanceof Ident) {
            String nm = ((Ident) e).name;
            TypeRef local = ctx.lookup(nm);
            if (local != null) {
                return local;
            }
            ClassDecl cc = ctx.currentClass;
            if (cc != null) {
                FieldDecl f = cc.field(nm);
                if (f != null) {
                    return fieldType(f, ctx);
                }
                FieldDecl inhF = findInheritedField(cc, nm);
                if (inhF != null) {
                    return fieldType(inhF, ctx);
                }
            }
        }
        return null;
    }

    /**
     * True for a {@code (i, e) in xs.indexed} loop shape: the iterable is a plain {@code .indexed}
     * access and the pattern is a two-field positional record. Shared by the statement for-in and
     * the collection-literal for-element forms.
     */
    private boolean isIndexedRecordFor(Pattern pattern, Expr iterable) {
        if (!(pattern instanceof RecordPattern) || !(iterable instanceof PropertyGet)) {
            return false;
        }
        PropertyGet pg = (PropertyGet) iterable;
        if (!pg.name.equals("indexed") || pg.nullAware) {
            return false;
        }
        int positional = 0;
        for (PatternField pf : ((RecordPattern) pattern).fields) {
            if (pf.name != null) {
                return false;
            }
            positional++;
        }
        return positional == 2;
    }

    /**
     * Emits the header of an {@code xs.indexed} loop &mdash; a {@code long} counter and an
     * enhanced-for over the base iterable &mdash; binding the record's index and element
     * subpatterns, then runs {@code body} for the loop body and closes the loop. Dart's
     * {@code Iterable.indexed} has no runtime counterpart, so this counted lowering stands in.
     */
    private void emitIndexedFor(Pattern pattern, Expr iterable, Ctx ctx, Runnable body) {
        emitIndexedFor("", pattern, iterable, ctx, body);
    }

    private void emitIndexedFor(String label, Pattern pattern, Expr iterable, Ctx ctx, Runnable body) {
        Ctx.Writer w = ctx.writer();
        PropertyGet pg = (PropertyGet) iterable;
        Out base = emitExpr(pg.target, null, ctx);
        TypeRef bt = base.type;
        TypeRef elemT = bt != null && (bt.is("List") || bt.is("Iterable") || bt.is("Set"))
                && !bt.args.isEmpty() ? bt.arg(0) : TypeRef.DYNAMIC;
        RecordPattern rp = (RecordPattern) pattern;
        ctx.pushScope();
        String counter = ctx.newTemp();
        String el = ctx.newTemp();
        w.line("long " + counter + " = 0;");
        w.line(label + "for (" + javaType(elemT, true, ctx) + " " + el + " : " + base.code + ") {");
        ctx.indent(1);
        ctx.declare(el, elemT);
        List<String> binds = new ArrayList<String>();
        patternMatch(rp.fields.get(0).pattern, counter, TypeRef.INT, ctx, binds);
        patternMatch(rp.fields.get(1).pattern, el, elemT, ctx, binds);
        for (String b : binds) {
            w.line(b);
        }
        // Counted before the body, whose `continue` would otherwise skip the increment
        // and give every later element a stale index. The binds above copied it.
        w.line(counter + "++;");
        body.run();
        ctx.indent(-1);
        w.line("}");
        ctx.popScope();
    }

    /**
     * Emits the binding declarations for a pattern matched against {@code subj} (a temp holding the
     * scrutinee) into {@code binds}, and returns the boolean match condition. Bound variables are
     * declared into the current scope so the case body and guard can reference them.
     */
    /**
     * {@link #patternMatch} for a caller that evaluates the returned condition before running
     * the bindings (a switch case or an if-case), which lets a value read once in the test be
     * reused by the bindings. A destructuring for-in discards the condition, so it calls
     * patternMatch directly.
     */
    private String patternTest(Pattern p, String subj, TypeRef subjType, Ctx ctx, List<String> binds) {
        boolean saved = patternCondRuns;
        patternCondRuns = true;
        try {
            return patternMatch(p, subj, subjType, ctx, binds);
        } finally {
            patternCondRuns = saved;
        }
    }

    /** True while the condition patternMatch returns is evaluated before its bindings run. */
    private boolean patternCondRuns;
    private int patternSubjectTokens;
    /**
     * The getters several cases of the switch being emitted read off its subject, keyed
     * subject + "|" + type + "." + name, each to its {flag, value} temps. Null outside one.
     */
    private Map<String, String[]> switchFieldCache;

    /**
     * Declares the cross-case getter cache of a switch over {@code subj}. Dart reads a
     * getter of the matched value once per switch -- the first time a case needs it -- and
     * every later case reuses it (its invocation-key rule), so `case Box(value: 5): ...
     * case Box(value: 4):` calls value once. Only a getter of the subject itself that two or
     * more cases read is cached; one of a nested subpattern is read once per case. The key
     * is the pattern's type, so a getter read through a subtype's pattern in one case and
     * the supertype's in another is read twice. Returns the previous cache, to restore.
     */
    private Map<String, String[]> openSwitchFieldCache(List<Pattern> patterns, String subj, Ctx ctx) {
        Map<String, String[]> saved = switchFieldCache;
        Map<String, Integer> uses = new java.util.LinkedHashMap<String, Integer>();
        Map<String, TypeRef> types = new java.util.HashMap<String, TypeRef>();
        for (Pattern p : patterns) {
            java.util.Set<String> keys = new java.util.LinkedHashSet<String>();
            collectSubjectFields(p, subj, keys, types);
            for (String k : keys) {
                Integer n = uses.get(k);
                uses.put(k, n == null ? 1 : n + 1);
            }
        }
        Map<String, String[]> cache = null;
        for (Map.Entry<String, Integer> e : uses.entrySet()) {
            if (e.getValue() < 2) {
                continue;
            }
            if (cache == null) {
                cache = new java.util.HashMap<String, String[]>();
            }
            String flag = ctx.newTemp();
            String val = ctx.newTemp();
            ctx.writer().line("boolean " + flag + " = false;");
            ctx.writer().line(javaType(types.get(e.getKey()), true, ctx) + " " + val + " = null;");
            cache.put(e.getKey(), new String[] {flag, val});
        }
        switchFieldCache = cache;
        return saved;
    }

    /** The cache keys of the getters {@code p} reads directly off {@code subj}. */
    private void collectSubjectFields(Pattern p, String subj, java.util.Set<String> keys,
                                      Map<String, TypeRef> types) {
        if (p instanceof ObjectPattern) {
            ObjectPattern o = (ObjectPattern) p;
            for (PatternField f : o.fields) {
                String k = subj + "|" + o.type.name + "." + f.name;
                keys.add(k);
                types.put(k, cacheableFieldType(o.type, f.name));
            }
        } else if (p instanceof AndPattern) {
            for (Pattern part : ((AndPattern) p).parts) {
                collectSubjectFields(part, subj, keys, types);
            }
        } else if (p instanceof OrPattern) {
            for (Pattern part : ((OrPattern) p).alternatives) {
                collectSubjectFields(part, subj, keys, types);
            }
        }
    }

    /** A getter's type for a cache temp: dynamic when it names a class type parameter. */
    private TypeRef cacheableFieldType(TypeRef owner, String name) {
        TypeRef ft = fieldTypeOf(owner, name);
        ClassDecl cd = program.classes.get(owner.name);
        if (cd != null && cd.typeParams.contains(ft.name)) {
            return TypeRef.DYNAMIC;
        }
        return ft;
    }

    private String patternMatch(Pattern p, String subj, TypeRef subjType, Ctx ctx, List<String> binds) {
        if (p instanceof VariablePattern) {
            VariablePattern v = (VariablePattern) p;
            if (v.wildcard) {
                return "true";
            }
            // An unqualified identifier pattern whose name is a constant of the enum being
            // switched over is a CONSTANT pattern in Dart, not a variable binding (e.g.
            // `switch (this) { study => ..., material || cupertino => ... }`). Compare by
            // enum identity and bind nothing so it composes inside or-patterns.
            if (v.type == null && subjType != null && program.enums.containsKey(subjType.name)
                    && programEnum(subjType.name, ctx).hasEntry(v.name)) {
                return subj + " == " + simpleEnumName(subjType.name, ctx) + "." + v.name;
            }
            if (v.type != null && isReferenceType(v.type)) {
                String jt = javaType(v.type, true, ctx);
                String nm = ctx.declareShadowSafe(v.name, v.type);
                String seen = subj;
                if (v.type.name != null && program.classes.containsKey(v.type.name)
                        && appliedCollectionMixin(v.type.name) != null
                        && (subjType == null || !program.classes.containsKey(subjType.name))) {
                    // `Bag b` against a runtime collection: the subject may be the view a
                    // coercion made of a Bag, so test and bind the object behind the view.
                    seen = "dart.runtime.DartRuntime.unview(" + subj + ")";
                }
                binds.add(jt + " " + nm + " = (" + jt + ") " + seen + ";");
                // Tested erased: Java has no instanceof for a parameterized type.
                return seen + " instanceof " + erasedJavaType(v.type, ctx);
            }
            TypeRef bt = v.type != null ? v.type : subjType;
            String jt = javaType(bt, false, ctx);
            String nm = ctx.declareShadowSafe(v.name, bt);
            binds.add(jt + " " + nm + " = " + castSubject(subj, subjType, bt, ctx) + ";");
            return v.type != null ? instanceofCheck(subj, v.type, ctx) : "true";
        }
        if (p instanceof ConstantPattern) {
            ctx.importClass("dart.runtime.DartRuntime");
            Out val = emitExpr(((ConstantPattern) p).value, subjType, ctx);
            return "DartRuntime.eq(" + subj + ", " + val.code + ")";
        }
        if (p instanceof RelationalPattern) {
            RelationalPattern r = (RelationalPattern) p;
            Out operand = emitExpr(r.operand, subjType, ctx);
            if ("==".equals(r.op) || "!=".equals(r.op)) {
                ctx.importClass("dart.runtime.DartRuntime");
                String eq = "DartRuntime.eq(" + subj + ", " + operand.code + ")";
                return "==".equals(r.op) ? eq : "!(" + eq + ")";
            }
            String num = numericValue(subj, subjType);
            return "(" + num + " " + r.op + " " + operand.code + ")";
        }
        if (p instanceof NullCheckPattern) {
            NullCheckPattern nc = (NullCheckPattern) p;
            TypeRef inner = subjType;
            if (subjType != null && subjType.nullable) {
                inner = new TypeRef(subjType.name);
                inner.args.addAll(subjType.args);
                inner.funcParams = subjType.funcParams;
                inner.funcReturn = subjType.funcReturn;
            }
            String sub = patternMatch(nc.inner, subj, inner, ctx, binds);
            if (nc.assertNonNull) {
                ctx.importClass("java.util.Objects");
                return joinAnd("java.util.Objects.requireNonNull(" + subj + ") != null", sub);
            }
            return joinAnd(subj + " != null", sub);
        }
        if (p instanceof CastPattern) {
            CastPattern c = (CastPattern) p;
            String jt = javaType(c.type, true, ctx);
            String cast = "((" + jt + ") " + subj + ")";
            return joinAnd(subj + " instanceof " + erasedJavaType(c.type, ctx),
                    patternMatch(c.inner, cast, c.type, ctx, binds));
        }
        if (p instanceof ObjectPattern) {
            ObjectPattern o = (ObjectPattern) p;
            String jt = javaType(o.type, true, ctx);
            String cond = subj + " instanceof " + erasedJavaType(o.type, ctx);
            String cast = "((" + jt + ") " + subj + ")";
            boolean programType = program.classes.containsKey(o.type.name);
            for (PatternField f : o.fields) {
                String access;
                TypeRef ft;
                if (programType) {
                    access = cast + "." + fieldAccess(o.type, f.name) + "()";
                    ft = fieldTypeOf(o.type, f.name);
                } else {
                    // A core or stub type (`String(length: var n)`, `List(isEmpty: true)`):
                    // its getters are not get$x accessors, so read it the way `x.length`
                    // on that type is read anywhere else.
                    Out got = emitMemberGet(new Out(cast, o.type), f.name, p, ctx);
                    access = got.code;
                    ft = TypeRef.DYNAMIC;
                }
                String[] cached = switchFieldCache == null ? null
                        : switchFieldCache.get(subj + "|" + o.type.name + "." + f.name);
                cond = joinAnd(cond, fieldPatternMatch(f.pattern, access, ft, cached, ctx, binds));
            }
            return cond;
        }
        if (p instanceof AndPattern) {
            String cond = "true";
            for (Pattern part : ((AndPattern) p).parts) {
                cond = joinAnd(cond, patternMatch(part, subj, subjType, ctx, binds));
            }
            return cond;
        }
        if (p instanceof OrPattern) {
            // or-patterns must not bind (Dart requires identical bindings on every branch); the
            // common use is alternative constants, which bind nothing.
            List<String> throwaway = new ArrayList<String>();
            StringBuilder cond = new StringBuilder("(");
            List<Pattern> alts = ((OrPattern) p).alternatives;
            for (int i = 0; i < alts.size(); i++) {
                if (i > 0) {
                    cond.append(" || ");
                }
                cond.append(patternMatch(alts.get(i), subj, subjType, ctx, throwaway));
            }
            if (!throwaway.isEmpty()) {
                diags.error(p, "E0433", "Variable bindings inside an or-pattern are not supported");
            }
            return cond.append(")").toString();
        }
        if (p instanceof ListPattern) {
            ListPattern l = (ListPattern) p;
            ctx.importClass("java.util.List");
            String cast = "((List) " + subj + ")";
            String cond = subj + " instanceof List && " + cast + ".size() == " + l.elements.size();
            for (int i = 0; i < l.elements.size(); i++) {
                String access = cast + ".get(" + i + ")";
                cond = joinAnd(cond, patternMatch(l.elements.get(i), access, TypeRef.DYNAMIC, ctx, binds));
            }
            return cond;
        }
        if (p instanceof RecordPattern) {
            RecordPattern rp = (RecordPattern) p;
            List<PatternField> positional = new ArrayList<PatternField>();
            List<PatternField> named = new ArrayList<PatternField>();
            for (PatternField f : rp.fields) {
                if (f.name == null) {
                    positional.add(f);
                } else {
                    named.add(f);
                }
            }
            named.sort((a, b) -> a.name.compareTo(b.name));
            List<String> namedNames = new ArrayList<String>();
            for (PatternField f : named) {
                namedNames.add(f.name);
            }
            String cn = registerRecordShape(positional.size(), namedNames);
            String cast = "((" + cn + ") " + subj + ")";
            String cond = subj + " instanceof " + cn;
            for (int i = 0; i < positional.size(); i++) {
                cond = joinAnd(cond, patternMatch(positional.get(i).pattern,
                        cast + ".$" + (i + 1) + "()", TypeRef.DYNAMIC, ctx, binds));
            }
            for (PatternField f : named) {
                cond = joinAnd(cond, patternMatch(f.pattern,
                        cast + "." + f.name + "()", TypeRef.DYNAMIC, ctx, binds));
            }
            return cond;
        }
        diags.error(p, "E0434", "Unsupported pattern in emitter");
        return "false";
    }

    /**
     * Matches an object pattern's field subpattern against {@code access}, a getter call.
     * Dart reads the getter once per match, but a subpattern that both tests and binds
     * (`Box(value: int x)`) names its subject twice, so the getter ran twice. When it does,
     * the value goes into a temp: assigned where the condition first reads it, or by a
     * leading binding when the condition does not read it (or is never evaluated).
     */
    private String fieldPatternMatch(Pattern p, String access, TypeRef ft, String[] cached, Ctx ctx,
                                     List<String> binds) {
        String token = "$pfield" + (patternSubjectTokens++) + "$";
        int bindStart = binds.size();
        String cond = patternMatch(p, token, ft, ctx, binds);
        if (cached != null) {
            // Shared with other cases of the switch: every use reads through the switch's
            // cache, which calls the getter on first use only, whichever case that is.
            String flag = cached[0];
            String val = cached[1];
            String lazy = "((" + flag + " || !(" + flag + " = true)) ? " + val + " : (" + val + " = "
                    + access + "))";
            for (int i = bindStart; i < binds.size(); i++) {
                binds.set(i, binds.get(i).replace(token, lazy));
            }
            return cond.replace(token, lazy);
        }
        int inCond = countOccurrences(cond, token);
        int total = inCond;
        for (int i = bindStart; i < binds.size(); i++) {
            total += countOccurrences(binds.get(i), token);
        }
        String value = access;
        if (total > 1) {
            String t = ctx.newTemp();
            String tj = javaType(ft, true, ctx);
            int first = cond.indexOf(token);
            String before = first >= 0 ? cond.substring(0, first) : "";
            boolean condFirst = patternCondRuns && first >= 0 && before.indexOf("&&") < 0
                    && before.indexOf("||") < 0 && before.indexOf('?') < 0;
            if (condFirst) {
                // The first read sits where the condition always evaluates it, ahead of
                // every other read and of the bindings.
                ctx.writer().line(tj + " " + t + " = null;");
                cond = cond.substring(0, first) + "(" + t + " = " + access + ")"
                        + cond.substring(first + token.length());
                value = t;
            } else if (inCond == 0 || !patternCondRuns) {
                binds.add(bindStart, tj + " " + t + " = " + access + ";");
                bindStart++;
                value = t;
            }
        }
        cond = cond.replace(token, value);
        for (int i = bindStart; i < binds.size(); i++) {
            binds.set(i, binds.get(i).replace(token, value));
        }
        return cond;
    }

    private static int countOccurrences(String s, String token) {
        int n = 0;
        int at = s.indexOf(token);
        while (at >= 0) {
            n++;
            at = s.indexOf(token, at + token.length());
        }
        return n;
    }

    private static String joinAnd(String a, String b) {
        if ("true".equals(a)) {
            return b;
        }
        if ("true".equals(b)) {
            return a;
        }
        return a + " && " + b;
    }

    /** True for a type that maps to a Java reference type (so {@code instanceof} + cast is legal). */
    private boolean isReferenceType(TypeRef t) {
        String n = t.name;
        return !(n.equals("int") || n.equals("double") || n.equals("num") || n.equals("bool"));
    }

    private String instanceofCheck(String subj, TypeRef type, Ctx ctx) {
        if (isReferenceType(type)) {
            return subj + " instanceof " + erasedJavaType(type, ctx);
        }
        // primitive-typed variable pattern over a dynamic subject: check the boxed form
        String boxed = type.is("bool") ? "Boolean"
                : type.is("double") ? "Double"
                : type.is("num") ? "Number" : "Long";
        return subj + " instanceof " + boxed;
    }

    /** Casts/unboxes a scrutinee temp to the target bind type when they differ. */
    private String castSubject(String subj, TypeRef from, TypeRef to, Ctx ctx) {
        if (from != null && from.name.equals(to.name)) {
            return subj;
        }
        String jt = javaType(to, false, ctx);
        if (to.is("int")) {
            return "((Number) " + subj + ").longValue()";
        }
        if (to.is("double")) {
            return "((Number) " + subj + ").doubleValue()";
        }
        return "(" + jt + ") " + subj;
    }

    private String numericValue(String subj, TypeRef subjType) {
        if (subjType != null && (subjType.is("int") || subjType.is("double") || subjType.is("num"))) {
            return subj;
        }
        return "((Number) " + subj + ").doubleValue()";
    }

    /** The accessor call (without trailing {@code ()}) for a field/getter of a class in a pattern. */
    private String fieldAccess(TypeRef ownerType, String name) {
        ClassDecl cd = program.classes.get(ownerType.name);
        if (cd != null) {
            if (cd.field(name) != null) {
                return "get$" + name;
            }
            if (cd.getter(name) != null) {
                return name;
            }
        }
        return "get$" + name;
    }

    private TypeRef fieldTypeOf(TypeRef ownerType, String name) {
        ClassDecl cd = program.classes.get(ownerType.name);
        if (cd != null) {
            FieldDecl f = cd.field(name);
            if (f != null && f.type != null) {
                return f.type;
            }
            MethodDecl g = cd.getter(name);
            if (g != null && g.returnType != null) {
                return g.returnType;
            }
        }
        return TypeRef.DYNAMIC;
    }

    /** Blocks nested under if/while/for are emitted inline (the brace is already written). */
    private Stmt unwrapBlock(Stmt s) {
        return s;
    }

    // ==================================================================
    // Expressions
    // ==================================================================

    /** Emitted expression: Java code + inferred Dart static type. */
    private static final class Out {
        final String code;
        final TypeRef type;
        /**
         * True when this value's type fell to {@code dynamic} because a diagnostic was
         * already reported for it (an unresolved identifier/member/method/constructor).
         * Member/method access on such a receiver is suppressed from re-diagnosing, so a
         * single root cause is reported once instead of cascading down the whole chain.
         */
        final boolean fromError;
        /**
         * Dart null-shorting: when non-null, this names a temp whose nullness shorts the
         * WHOLE selector chain this Out belongs to. Set by a null-aware access (`a?.b`) and
         * propagated through trailing plain selectors (`.c()`, `.d`), then materialized into a
         * `(guard == null ? null : code)` conditional when the value is finally consumed.
         */
        final String shortGuard;

        Out(String code, TypeRef type) {
            this(code, type, false, null);
        }

        Out(String code, TypeRef type, boolean fromError) {
            this(code, type, fromError, null);
        }

        Out(String code, TypeRef type, boolean fromError, String shortGuard) {
            this.code = code;
            this.type = type == null ? TypeRef.DYNAMIC : type;
            this.fromError = fromError;
            this.shortGuard = shortGuard;
        }

        /** This Out re-tagged so its whole chain is shorted by {@code guard} being null. */
        Out withShort(String guard) {
            return guard == null ? this : new Out(code, type, fromError, guard);
        }
    }

    /**
     * Emits an expression as a consumed VALUE: any pending Dart null-short guard
     * (from a `?.` selector chain) is materialized into a conditional here. Selector
     * emitters that want to extend the chain call {@link #emitExprRaw} for their target
     * instead, so the guard propagates until the chain ends.
     */
    private Out emitExpr(Expr e, TypeRef expected, Ctx ctx) {
        return materializeShort(emitExprRaw(e, expected, ctx));
    }

    /** Wraps a guard-carrying Out in its `(guard == null ? null : code)` conditional. */
    private Out materializeShort(Out o) {
        if (o != null && o.shortGuard != null) {
            return new Out("(" + o.shortGuard + " == null ? null : " + o.code + ")",
                    boxType(o.type), o.fromError);
        }
        return o;
    }

    private Out emitExprRaw(Expr e, TypeRef expected, Ctx ctx) {
        if (e == null) {
            // The front end leaves a hole where it reported an error; the diagnostic is
            // already recorded, so emit a placeholder rather than crash the whole run.
            return new Out("null", TypeRef.DYNAMIC, true);
        }
        if (e instanceof IntLit) {
            long v = ((IntLit) e).value;
            if (expected != null && expected.is("double")) {
                return new Out(v + ".0", TypeRef.DOUBLE);
            }
            return new Out(v + "L", TypeRef.INT);
        }
        if (e instanceof DoubleLit) {
            double v = ((DoubleLit) e).value;
            String s = Double.toString(v);
            return new Out(s, TypeRef.DOUBLE);
        }
        if (e instanceof BoolLit) {
            return new Out(String.valueOf(((BoolLit) e).value), TypeRef.BOOL);
        }
        if (e instanceof NullLit) {
            return new Out("null", TypeRef.NULL);
        }
        if (e instanceof StringLit) {
            return emitString((StringLit) e, ctx);
        }
        if (e instanceof ListLit) {
            Out o = emitListLit((ListLit) e, expected, ctx);
            return ((ListLit) e).isConst ? constCollection(o, ctx) : o;
        }
        if (e instanceof MapLit) {
            MapLit ml = (MapLit) e;
            if (ml.keys.isEmpty() && ml.elements.isEmpty() && ml.keyType == null
                    && expected != null && expected.is("Set")) {
                // An empty `{}` is a map unless its context type is a Set, which is
                // how Dart reads `this.favorites = const {}` for a Set<int> field.
                SetLit sl = new SetLit();
                sl.isConst = ml.isConst;
                Out o = emitSetLit(sl, expected, ctx);
                return sl.isConst ? constCollection(o, ctx) : o;
            }
            Out o = emitMapLit(ml, expected, ctx);
            return ((MapLit) e).isConst ? constCollection(o, ctx) : o;
        }
        if (e instanceof SetLit && spreadsOnlyMaps((SetLit) e, expected, ctx)) {
            // `{...?headers}`: a body of spreads alone is a map when what it spreads is one
            // (Dart decides by the spread's static type); the parser, seeing no `k: v`, made
            // it a set, and `.containsKey` on the result was then E0137.
            SetLit sl = (SetLit) e;
            MapLit ml = new MapLit();
            ml.line = sl.line;
            ml.col = sl.col;
            ml.isConst = sl.isConst;
            ml.structured = true;
            ml.elements.addAll(sl.elements);
            Out o = emitMapLit(ml, expected, ctx);
            return ml.isConst ? constCollection(o, ctx) : o;
        }
        if (e instanceof SetLit) {
            Out o = emitSetLit((SetLit) e, expected, ctx);
            return ((SetLit) e).isConst ? constCollection(o, ctx) : o;
        }
        if (e instanceof Ident) {
            return emitIdent((Ident) e, expected, ctx);
        }
        if (e instanceof ThisExpr) {
            if (ctx.extensionSelfType != null) {
                return new Out("$self", ctx.extensionSelfType);
            }
            return new Out("this", ctx.currentClass != null ? new TypeRef(ctx.currentClass.name) : TypeRef.DYNAMIC);
        }
        if (e instanceof SuperExpr) {
            String member = ctx.superMember;
            ctx.superMember = null;
            Out viaMixin = superThroughMixin(member, ctx);
            if (viaMixin != null) {
                return viaMixin;
            }
            TypeRef sup = ctx.currentClass != null && ctx.currentClass.superclass != null
                    ? ctx.currentClass.superclass : TypeRef.DYNAMIC;
            return new Out("super", sup);
        }
        if (e instanceof CascadeTarget) {
            return ctx.cascadeTarget();
        }
        if (e instanceof Cascade) {
            Cascade cas = (Cascade) e;
            Out target = emitExpr(cas.target, expected, ctx);
            String tmp;
            if (target.code.equals("this")) {
                tmp = "this";
            } else {
                tmp = ctx.newTemp();
                ctx.writer().line("var " + tmp + " = " + target.code + ";");
            }
            if (cas.nullAware && !tmp.equals("this")) {
                // `a?..b()..c = v`: a null target skips every section, and the value is null.
                ctx.writer().line("if (" + tmp + " != null) {");
                ctx.indent(1);
            }
            ctx.pushCascadeTarget(new Out(tmp, cas.nullAware ? copyNonNull(target.type) : target.type));
            for (Expr section : cas.sections) {
                Out o = emitStatementExpr(section, ctx);
                String code = statementize(o.code);
                if (!code.isEmpty()) {
                    ctx.writer().line(code + ";");
                }
            }
            ctx.popCascadeTarget();
            if (cas.nullAware && !tmp.equals("this")) {
                ctx.indent(-1);
                ctx.writer().line("}");
            }
            return new Out(tmp, target.type);
        }
        if (e instanceof ThrowExpr) {
            // Expression position: a generic helper that never returns, typed as whatever the
            // context expects (Dart's Never), so it fits a ternary arm, `??` or an arrow body.
            ctx.importClass("dart.runtime.DartRuntime");
            Out v = emitExpr(((ThrowExpr) e).value, null, ctx);
            if (expected != null && !expected.is("void") && !expected.is("dynamic")
                    && !expected.is("var")) {
                return new Out("DartRuntime.<" + javaType(expected, true, ctx) + ">throwValue("
                        + v.code + ")", expected);
            }
            return new Out("DartRuntime.throwValue(" + v.code + ")", TypeRef.DYNAMIC);
        }
        if (e instanceof AwaitExpr) {
            Expr operand = ((AwaitExpr) e).operand;
            Call savedFor = ctx.callExpectedFor;
            TypeRef savedExp = ctx.callExpected;
            if (operand instanceof Call && expected != null) {
                // Only for a generic call's witness inference: `Box<int> b = await open(..)`.
                ctx.callExpectedFor = (Call) operand;
                ctx.callExpected = TypeRef.of("Future", expected);
            }
            Out o;
            try {
                o = emitExpr(operand, null, ctx);
            } finally {
                ctx.callExpectedFor = savedFor;
                ctx.callExpected = savedExp;
            }
            // Awaiting a void-typed operand (e.g. a `void` stub call) yields nothing; wrapping it
            // in Await.await$(...) is a "void not allowed here" error, so emit the bare call.
            if (o.type != null && o.type.is("void")) {
                return new Out(o.code, TypeRef.VOID);
            }
            ctx.importClass("dart.async.Await");
            if (o.type != null && o.type.is("Future") && o.type.nullable) {
                // `Future<int>? f = null; await f` is null in Dart; await$ rejects a null
                // Future with TypeError, so a nullable one goes through awaitAny.
                TypeRef inner = boxType(o.type.arg(0));
                String v = "Await.awaitAny(" + o.code + ")";
                return new Out(isDynamicType(inner) ? v : "((" + javaType(inner, true, ctx) + ") " + v + ")", inner);
            }
            if (o.type != null && o.type.is("Future")) {
                TypeRef awaited = boxType(o.type.arg(0));
                String pk = o.type.args.isEmpty() ? null : primitiveListKind(awaited);
                if (pk != null) {
                    // A Future<List<int>> from a generic runtime method completes with a
                    // boxed DartList; the List<int> it is typed as is a DartLongList.
                    ctx.importClass("dart.core.Dart" + pk + "List");
                    return new Out("Dart" + pk + "List.as" + pk + "s(Await.awaitAny(" + o.code + "))",
                            awaited);
                }
                return new Out("Await.await$(" + o.code + ")", awaited);
            }
            if (o.type == null || isDynamicType(o.type) || o.type.is("FutureOr")) {
                // Might be a Future, might be a value: decided at run time.
                TypeRef inner = o.type != null && o.type.is("FutureOr") && !o.type.args.isEmpty()
                        ? o.type.arg(0) : TypeRef.DYNAMIC;
                String v = "Await.awaitAny(" + o.code + ")";
                return new Out(isDynamicType(inner) ? v : "((" + javaType(boxType(inner), true, ctx) + ") " + v + ")",
                        boxType(inner));
            }
            // A statically non-Future value: `await 42` is 42.
            return new Out(o.code, o.type);
        }
        if (e instanceof ParenExpr) {
            Out inner = emitExpr(((ParenExpr) e).inner, expected, ctx);
            return new Out("(" + inner.code + ")", inner.type);
        }
        if (e instanceof SwitchExpr) {
            return emitSwitchExpr((SwitchExpr) e, expected, ctx);
        }
        if (e instanceof RecordLit) {
            return emitRecordLit((RecordLit) e, ctx);
        }
        if (e instanceof PropertyGet) {
            if (((PropertyGet) e).target instanceof SuperExpr) {
                Out bridged = onMixinSuperBridge(((PropertyGet) e).name, true, null, ctx);
                if (bridged != null) {
                    return bridged;
                }
                ctx.superMember = ((PropertyGet) e).name;
            }
            return emitPropertyGet((PropertyGet) e, ctx);
        }
        if (e instanceof Call) {
            if (((Call) e).target instanceof SuperExpr) {
                Out bridged = onMixinSuperBridge(((Call) e).name, false, ((Call) e).args, ctx);
                if (bridged != null) {
                    return bridged;
                }
                ctx.superMember = ((Call) e).name;
            }
            return emitCall((Call) e, expected, ctx);
        }
        if (e instanceof CtorCall) {
            CtorCall cc = (CtorCall) e;
            if (cc.ctorName != null) {
                // `List<E>.of(src)` parsed as a constructor call (explicit type arguments):
                // the same copy as List<E>.from, typed by E.
                if (cc.type.name.equals("List") && cc.ctorName.equals("of")) {
                    cc.ctorName = "from";
                }
                // List<E>.generate / .filled / .from -- dart:core intrinsic factories,
                // routed to the primitive Dart*List when E is a non-nullable int/double.
                if (cc.type.name.equals("List")
                        && (cc.ctorName.equals("generate") || cc.ctorName.equals("filled")
                            || cc.ctorName.equals("from") || cc.ctorName.equals("unmodifiable"))) {
                    return emitListFactory(cc, ctx);
                }
                // Map/Set/Iterable named factory constructors -- dart:core intrinsics
                // routed to the existing statics on Dart{Map,Set,Iterable}.
                Out coreFactory = emitCoreCollectionFactory(cc, ctx);
                if (coreFactory != null) {
                    return coreFactory;
                }
                // Future named constructors (Future.delayed / Future.value / Future.error),
                // e.g. `Future<void>.delayed(Duration(...), () { ... })`. Routed to the
                // dart.async.Future statics; the element type witness is dropped (erased).
                if (cc.type.name.equals("Future")) {
                    Out future = emitFutureNamedCtor(cc, ctx);
                    if (future != null) {
                        return future;
                    }
                }
                ClassDecl pc = program.classes.get(cc.type.name);
                if (pc != null && pc.namedCtor(cc.ctorName) != null) {
                    String tw = witnessArgs(pc, cc.type.args, cc, ctx);
                    String na = canonicalArgs(pc.namedCtor(cc.ctorName), cc.args, ctx);
                    return new Out(javaClassName(pc) + "." + javaIdent(cc.ctorName) + "("
                            + (na.isEmpty() ? stripTrailingComma(tw) : tw + na) + ")",
                            new TypeRef(cc.type.name));
                }
                Ast.ClassDecl sc = stubs.classes.get(cc.type.name);
                if (sc != null) {
                    // stub named ctors are declared as static methods
                    Ast.MethodDecl m = stubs.findMethod(cc.type.name, cc.ctorName, false);
                    if (m != null && m.isStatic) {
                        String call = stubSimpleName(cc.type.name, ctx) + "." + cc.ctorName + "("
                                + stubMethodArgs(m, cc.args, ctx) + ")";
                        // `Provider<T>.value(...)`: the T a null value cannot show.
                        String token = publishesValueByType(cc.type.name)
                                ? providedTypeToken(cc.type.args, ctx) : null;
                        if (token != null) {
                            call += ".providedType(" + token + ")";
                        }
                        // `Stream.fromIterable([1, 2])` is a Stream<int>, not the bare Stream<T>.
                        TypeRef bound = bindStaticClassTypeParams(m, sc, cc.type.args, cc.args, ctx);
                        return new Out(call, bound != null ? bound : m.returnType);
                    }
                }
                String en = cc.type.name;
                if ((en.equals("String") || en.equals("bool") || en.equals("int"))
                        && (cc.ctorName.equals("fromEnvironment") || cc.ctorName.equals("hasEnvironment"))) {
                    // `const bool.fromEnvironment('x')` arrives as a constructor call
                    // (the `const` keyword), not the static-call shape handled in
                    // emitCall. There is no compile-time environment: the default.
                    TypeRef t = en.equals("String") ? TypeRef.STRING : en.equals("bool") ? TypeRef.BOOL : TypeRef.INT;
                    if (cc.ctorName.equals("hasEnvironment")) {
                        return new Out("false", TypeRef.BOOL);
                    }
                    for (NamedArg na : cc.args.named) {
                        if (na.name.equals("defaultValue")) {
                            return emitExpr(na.value, t, ctx);
                        }
                    }
                    return new Out(en.equals("String") ? "\"\"" : en.equals("bool") ? "false" : "0L", t);
                }
                diags.error(e, "E0126", "Cannot resolve named constructor " + cc.type.name + "." + cc.ctorName);
                return new Out("null", TypeRef.DYNAMIC);
            }
            return emitCtorCall(cc.type.name, inferredWitnessTypeArgs(
                    program.classes.get(cc.type.name), cc.type.args, expected), cc.args, e, ctx);
        }
        if (e instanceof IndexGet) {
            if (((IndexGet) e).target instanceof SuperExpr) {
                ctx.superMember = "$index";
            }
            return emitIndexGet((IndexGet) e, ctx);
        }
        if (e instanceof Assign) {
            return emitAssign((Assign) e, ctx);
        }
        if (e instanceof Binary) {
            return emitBinary((Binary) e, ctx);
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Out o = emitExpr(u.operand, null, ctx);
            if (isDynamic(o.type)) {
                // Java's unary operators do not apply to Object; dispatch on the value.
                ctx.importClass("dart.runtime.DartRuntime");
                if (u.op.equals("!")) {
                    return new Out("!DartRuntime.dynBool(" + o.code + ")", TypeRef.BOOL);
                }
                String fn = u.op.equals("-") ? "dynNegate" : u.op.equals("~") ? "dynBitNot" : null;
                if (fn != null) {
                    return new Out("DartRuntime." + fn + "(" + o.code + ")", TypeRef.DYNAMIC);
                }
            }
            if (u.op.equals("-") && o.type != null && o.type.is("Duration")
                    && !program.classes.containsKey("Duration")) {
                // Duration's unary minus, under the mangled name (see emitBinary).
                return new Out(paren(o.code) + ".$minus()", o.type);
            }
            if ((u.op.equals("-") || u.op.equals("~")) && o.type != null && !isNumeric(o.type)
                    && !o.type.is("bool")) {
                // A class's own `operator -()` / `operator ~()`: declared under the mangled
                // binary name with no parameter (see AstBuilder.mangleOperator).
                String mangled = u.op.equals("-") ? "$minus" : "$bitNot";
                ClassDecl cls = program.classes.get(o.type.name);
                MethodDecl m = cls != null ? findMethodInHierarchy(cls, mangled) : null;
                boolean declares = cls != null ? m != null
                        : stubs.isStubClass(o.type.name) && stubs.findMethod(o.type.name, mangled, false) != null;
                if (declares) {
                    TypeRef rt = m != null && m.returnType != null && !m.returnType.is("var") ? m.returnType : o.type;
                    return new Out(paren(o.code) + "." + mangled + "()", rt);
                }
            }
            return new Out(u.op + paren(o.code), o.type);
        }
        if (e instanceof IncDec) {
            IncDec id = (IncDec) e;
            boolean statement = ctx.statementExpr == e;
            Assign inc = new Assign().at(id.file, id.line, id.col);
            inc.lhs = id.operand;
            inc.op = id.increment ? "+=" : "-=";
            IntLit one = new IntLit().at(id.file, id.line, id.col);
            one.value = 1;
            inc.rhs = one;
            Expr savedStmt = ctx.statementExpr;
            Assign savedPostfix = ctx.postfixAssign;
            if (statement) {
                ctx.statementExpr = inc;
            } else if (!id.prefix) {
                ctx.postfixAssign = inc;
            }
            try {
                if (needsShort(inc)
                        // `a[i]++`: the read is `a.get(i)`, a call `++` cannot apply to; the
                        // index assignment writes through `[]=` (the old value for a postfix).
                        || id.operand instanceof IndexGet
                        || (id.operand instanceof Ident && isTopLevelSetter(((Ident) id.operand).name, ctx))) {
                    // `a?.b++` is shorted like every assignment behind a `?.`; `top++` on a
                    // library-level getter/setter pair writes through the setter.
                    return emitAssign(inc, ctx);
                }
                ctx.pushWriter(ctx.currentIndent());
                Out target = emitExpr(id.operand, null, ctx);
                String targetLifted = ctx.popWriter();
                boolean accessor = target.code.endsWith("()") && target.code.contains(".get$");
                if ((id.operand instanceof PropertyGet
                        && target.code.endsWith("." + ((PropertyGet) id.operand).name + "()"))
                        || (accessor && !statement)) {
                    // `x.prop++` through a getter/setter pair (an app or stub property): the
                    // read is a call, and `x.prop()++` is not Java. Lowered as `x.prop += 1`,
                    // which writes through the setter, in its value form when the value is
                    // used (the old value for a postfix). The read emitted above is dropped
                    // with whatever it lifted, so its receiver is not evaluated twice.
                    ctx.pushWriter(ctx.currentIndent());
                    Out write = emitAssign(inc, ctx);
                    String writeLifted = ctx.popWriter();
                    // No setter found: emitAssign wrote the read back as an assignment
                    // target; keep the historical form rather than that.
                    if (!write.code.startsWith(target.code + " ")) {
                        ctx.writer().raw(writeLifted);
                        return write;
                    }
                }
                ctx.writer().raw(targetLifted);
                if (target.code.endsWith("()") && target.code.contains(".get$")) {
                    String base = target.code.substring(0, target.code.lastIndexOf(".get$"));
                    String prop = target.code.substring(target.code.lastIndexOf(".get$") + 5, target.code.length() - 2);
                    String delta = id.increment ? " + 1" : " - 1";
                    return new Out(base + ".set$" + prop + "(" + target.code + delta + ")", target.type);
                }
                if (isDynamic(target.type)) {
                    // `x++` on a dynamic x: Java's ++ does not apply to Object.
                    ctx.importClass("dart.runtime.DartRuntime");
                    String write = target.code + " = DartRuntime.dynBinary(\"" + (id.increment ? "+" : "-")
                            + "\", " + target.code + ", 1L)";
                    return new Out(id.prefix ? write
                            : "DartRuntime.dynPostfix(" + target.code + ", " + write + ")", target.type);
                }
                String op = id.increment ? "++" : "--";
                return new Out(id.prefix ? op + target.code : target.code + op, target.type);
            } finally {
                ctx.statementExpr = savedStmt;
                ctx.postfixAssign = savedPostfix;
            }
        }
        if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            Out cond = emitExpr(c.condition, TypeRef.BOOL, ctx);
            // `x is T ? x.member : ...` promotes x to T in the then-branch.
            List<Object[]> undo = applyGuardPromotions(c.condition, ctx);
            Lazy la = emitLazyOperand(c.thenExpr, expected, ctx, 1);
            restorePromotions(undo, ctx);
            Lazy lb = emitLazyOperand(c.elseExpr, expected, ctx, 1);
            Out a = la.out;
            Out b = lb.out;
            TypeRef t = conditionalType(a.type, b.type, expected);
            boolean mixedNumeric = a.type != null && b.type != null
                    && ((a.type.is("int") && b.type.is("double")) || (a.type.is("double") && b.type.is("int")))
                    && expected != null
                    && (expected.is("Object") || expected.is("num") || expected.is("dynamic"));
            if (la.lifts() || lb.lifts()) {
                // An arm lifted statements: they belong to that arm alone, so the
                // conditional becomes an if/else assigning a temp.
                String jt = mixedNumeric ? "Number" : declarableType(t, false, ctx);
                Ctx.Writer w = ctx.writer();
                if (jt == null) {
                    // No temp can hold the value; keep the old, eager form.
                    w.raw(la.lifted);
                    w.raw(lb.lifted);
                } else {
                    String r = ctx.newTemp();
                    w.line(jt + " " + r + ";");
                    w.line("if (" + cond.code + ") {");
                    w.raw(la.lifted);
                    ctx.indent(1);
                    w.line(r + " = " + (mixedNumeric ? "(Number) " + paren(a.code) : coerce(a, t, ctx)) + ";");
                    ctx.indent(-1);
                    w.line("} else {");
                    w.raw(lb.lifted);
                    ctx.indent(1);
                    w.line(r + " = " + (mixedNumeric ? "(Number) " + paren(b.code) : coerce(b, t, ctx)) + ";");
                    ctx.indent(-1);
                    w.line("}");
                    return new Out(r, t);
                }
            }
            if (mixedNumeric) {
                // An int arm and a double arm STORED as num or Object: Java's ternary
                // would promote the int to double even when that arm is chosen, so
                // `Object v = f ? 1 : 1.5` held 1.0 and `v is int` was false. Casting
                // each arm to Number keeps the selected value's own type -- boxing
                // alone is not enough, since a Long/Double pair is still promoted.
                // Only for a reference target: as an arithmetic operand or a double
                // return, the promotion gives the same number Dart does, and Number
                // arms would not compile there.
                return new Out("(" + cond.code + " ? (Number) " + paren(a.code) + " : (Number) "
                        + paren(b.code) + ")", t);
            }
            // `f == null ? sink.completeError : (Object e, StackTrace s) async {...}` handed
            // to an Object-erased Function slot: the tear-off arm carries its Funcs cast, the
            // lambda arm has no target type at all ("lambda expression not expected here").
            // The lambda takes the tear-off's functional interface.
            String aCode = a.code;
            String bCode = b.code;
            if (c.elseExpr instanceof Lambda && !(c.thenExpr instanceof Lambda)) {
                String sam = leadingFuncsCast(aCode);
                if (sam != null) {
                    bCode = "(" + sam + ") " + bCode;
                }
            } else if (c.thenExpr instanceof Lambda && !(c.elseExpr instanceof Lambda)) {
                String sam = leadingFuncsCast(bCode);
                if (sam != null) {
                    aCode = "(" + sam + ") " + aCode;
                }
            }
            // An arm Java types as Object (a dynamic read, an imprecise stub return, a
            // FutureOr) in a conditional whose slot is a concrete reference type: javac types
            // the ternary by its arms ("bad type in conditional expression"), so the arm takes
            // Dart's implicit downcast to the slot here.
            // Only a real slot: with none, Dart types `v is int ? v + 10 : v` as dynamic,
            // and narrowing the dynamic arm to the other arm's type would be wrong.
            aCode = downcastArm(a, aCode, expected, ctx);
            bCode = downcastArm(b, bCode, expected, ctx);
            return new Out("(" + cond.code + " ? " + aCode + " : " + bCode + ")", t);
        }
        if (e instanceof NotNullAssert) {
            Out o = emitExpr(((NotNullAssert) e).operand, null, ctx);
            ctx.importClass("dart.runtime.DartRuntime");
            TypeRef t = copyNonNull(o.type);
            return new Out("DartRuntime.nn(" + o.code + ")", t, o.fromError);
        }
        if (e instanceof IsTest) {
            IsTest t = (IsTest) e;
            Out o = emitExpr(t.operand, null, ctx);
            if (o.type != null && isClassRef(o.type)) {
                // `String is Type`: a type literal operand is the Type object, its class
                // literal; emitted bare it was the class name, which javac cannot test.
                o = new Out(classLiteral(o, ctx), new TypeRef("Type"));
            }
            String subject = o.code;
            if (o.type != null && !o.type.nullable
                    && (o.type.is("int") || o.type.is("double") || o.type.is("bool"))) {
                // A primitive operand cannot be tested with instanceof; the cast boxes it,
                // so `min(1, 2) is int` tests the Long it is.
                subject = "((Object) " + paren(o.code) + ")";
            } else if (o.type != null && o.type.is("Type")) {
                // `V is String` with V a type parameter or literal tests the Type object
                // itself (false, unless the test is `is Object`/`is Type`). The operand is a
                // java.lang.Class here, which is final, so javac refuses an instanceof
                // against any unrelated class; tested as an Object it gives Dart's answer.
                subject = "((Object) " + paren(o.code) + ")";
            } else if (o.type != null && o.type.name != null && t.type.name != null) {
                // `bin is LazyBin` where Bin is a class LazyBin is unrelated to: Dart allows the
                // test (a subclass of Bin may implement LazyBin), javac refuses an instanceof
                // between two unrelated classes. Testing the value as an Object is the same test.
                ClassDecl from = program.resolveClass(o.type.name, ctx.library());
                ClassDecl to = program.resolveClass(t.type.name, ctx.library());
                if (from != null && to != null && from != to && isProgramClassDecl(from) && isProgramClassDecl(to)
                        && !isProgramSubtype(from, to) && !isProgramSubtype(to, from)) {
                    subject = "((Object) " + paren(o.code) + ")";
                }
            }
            if (t.type.name != null && program.classes.containsKey(t.type.name)
                    && appliedCollectionMixin(t.type.name) != null
                    && (o.type == null || !program.classes.containsKey(o.type.name))) {
                // `s is Bag` where Bag applies a dart:collection mixin and s is typed as a
                // runtime collection: s may be the live view the coercion made of a Bag, which
                // is that Bag in Dart. The test looks through the view (DartRuntime.unview).
                subject = "dart.runtime.DartRuntime.unview(" + o.code + ")";
            } else if (iterableAsList(o.type, t.type)) {
                // `it is List<int>` on an Iterable: a List handed over as an Iterable travels
                // as its DartIterable view, which javac will not even test against DartList.
                subject = "dart.core.DartIterable.listBehind(" + o.code + ")";
            }
            String check;
            String tw = t.type.args.isEmpty() && t.type.funcReturn == null
                    ? witnessFor(t.type.name, ctx) : null;
            if (t.type.is("Null")) {
                // `x is Null` holds only for null; it mapped to Object, which is the opposite.
                check = subject + " == null";
            } else if (tw != null) {
                // `x is T` for a T the class keeps a witness of.
                check = (t.type.nullable ? "dart.runtime.TypeWitness.nullable(" + tw + ")" : tw)
                        + ".test(" + subject + ")";
            } else if (reportTypeVariableWitness(t, t.type, ctx,
                    "'" + (t.negated ? "is! " : "is ") + t.type.name + "'")) {
                // `instanceof T`/`DartRuntime.isOrNull(x, T.class)` for a bare type-variable
                // T is not merely wrong -- javac refuses both, since erasure leaves T with no
                // reifiable class to test against. Diagnosed above rather than emitted.
                check = "false";
            } else if (t.type.nullable) {
                // `x is int?` holds for null too. The subject is evaluated once, inside the
                // helper, so `next() is int?` does not call next() twice.
                ctx.importClass("dart.runtime.DartRuntime");
                check = "DartRuntime.isOrNull(" + subject + ", " + erasedJavaType(t.type, ctx) + ".class)";
            } else {
                // Erased: `x is List<String>` cannot name its arguments in a Java instanceof.
                //
                // DELIBERATE, and not "fixed" with runtime type witnesses. The transpiler's
                // generics model is Java's: type arguments are erased, and `is`/`as` check
                // the RAW type (and the storage class, so `is List<int>` does test for the
                // primitive-backed list). `(<int>[1] as dynamic) is List<String>` is
                // therefore true here where Dart says false. Reifying the arguments would
                // mean a witness carried by EVERY generic value -- each list, map, set,
                // future, stream and generic class instance, created and copied on every
                // path -- which is a pervasive time and size cost on device for a check
                // real Flutter code almost never depends on. A partial answer keyed on the
                // storage class (a DartLongList is no List<String>) would be worse than
                // none: whether it answered correctly would depend on how the list happened
                // to be built (a List<int> from map().toList() is a boxed DartList), so the
                // same test would flip between call sites. emitAsCast shares this model.
                check = subject + " instanceof " + erasedJavaType(t.type, ctx);
            }
            return new Out(t.negated ? "!(" + check + ")" : "(" + check + ")", TypeRef.BOOL);
        }
        if (e instanceof AsCast) {
            return emitAsCast((AsCast) e, ctx);
        }
        if (e instanceof Lambda) {
            return emitLambda((Lambda) e, expected, ctx);
        }
        diags.error(e, "E0128", "Unsupported expression in emitter: " + e.getClass().getSimpleName());
        return new Out("null", TypeRef.DYNAMIC);
    }

    /**
     * The Java class an {@code is}/{@code as} test checks against: the type's Java form
     * with its type arguments dropped, since Java can neither test nor cast-check a
     * parameterized type at runtime ({@code instanceof DartList<String>} does not
     * compile). The class itself is kept -- List&lt;int&gt; stays DartLongList -- because
     * that is the class a value of that type is held in, and the one a promoted use casts to.
     */
    private String erasedJavaType(TypeRef t, Ctx ctx) {
        String jt = javaType(t, true, ctx);
        int lt = jt.indexOf('<');
        return lt < 0 ? jt : jt.substring(0, lt);
    }

    /**
     * Whether {@code n} is plausibly a declared type parameter rather than a class name the
     * stubs lack: one of the enclosing class's type parameters, or the short upper-case
     * spelling ({@code T}, {@code E}, {@code K2}) Dart code uses for method type parameters,
     * which this emitter does not track per method.
     */
    private boolean looksLikeTypeParameter(String n, Ctx ctx) {
        if (ctx.currentClass != null && ctx.currentClass.typeParams != null
                && ctx.currentClass.typeParams.contains(n)) {
            return true;
        }
        return n.matches("[A-Z][A-Z0-9]?") || n.matches("T[A-Z][a-z]*");
    }

    /** A type that is a type VARIABLE here ({@code T}): it has no class to test against. */
    private boolean isTypeVariable(TypeRef t, Ctx ctx) {
        if (t == null || !t.args.isEmpty() || t.funcReturn != null || t.name == null
                || t.name.indexOf('.') >= 0) {
            return false;
        }
        String n = t.name;
        if (n.equals("int") || n.equals("double") || n.equals("num") || n.equals("bool")
                || n.equals("String") || n.equals("Object") || n.equals("dynamic") || n.equals("var")
                || n.equals("Null") || n.equals("void") || n.equals("Function")
                // Duration and Stopwatch are dart:core classes javaType maps by a bare-name
                // special case, same as CORE_ERRORS below -- neither is a stub or program
                // class, so without this they would read as unresolved type variables too.
                || n.equals("Duration") || n.equals("Stopwatch")) {
            return false;
        }
        if (program.resolveClass(n, ctx.library()) != null || program.classesByName.containsKey(n)
                || program.enums.containsKey(n) || program.typedefs.containsKey(n)
                || stubs.isStubClass(n) || stubs.isStubEnum(n) || typedefSig(n) != null) {
            return false;
        }
        // dart:core's error/exception hierarchy (StateError, ArgumentError, ...) resolves
        // through CORE_ERRORS rather than the stub/program registries javaType otherwise
        // falls through -- it is not a stub class, so without this `e is StateError` on a
        // caught Object (dart_thrown_values_round14 and friends) was misread as a type
        // variable, the same bare name javaType leaves unresolved either way. See
        // isThrowableType for the matching "Error"/"Exception" bare-name policy.
        if (CORE_ERRORS.containsKey(n) || n.equals("Error") || n.equals("Exception")) {
            return false;
        }
        String jt = javaType(t, true, ctx);
        return jt.equals(n);
    }

    // ---- reified type witnesses -------------------------------------------------------
    //
    // A program class whose body uses one of its type parameters as a value (`x is T`,
    // `T == int`, `Type get t => T`, `'$T'`) keeps a dart.runtime.TypeWitness per such
    // parameter in a field. Every constructor takes them as leading hidden parameters, from
    // the type arguments its caller wrote (or its own witnesses, for a subclass passing its
    // parameters on). Only those classes pay for it; a construction that cannot name its
    // type arguments is an error, never a guessed witness.

    private final java.util.Map<ClassDecl, List<String>> witnessCache =
            new java.util.IdentityHashMap<ClassDecl, List<String>>();

    /** The type parameters of {@code c} that need a witness, in declaration order. */
    List<String> witnessParams(ClassDecl c) {
        if (c == null || c.typeParams.isEmpty() || c.isMixin || c.extensionOn != null
                || c.enumOf != null || !isProgramClassDecl(c)) {
            return java.util.Collections.emptyList();
        }
        indexMethods();
        List<String> cached = witnessCache.get(c);
        if (cached != null) {
            return cached;
        }
        witnessCache.put(c, java.util.Collections.<String>emptyList());
        List<String> out = computeClassWitnesses(c);
        witnessCache.put(c, out);
        return out;
    }

    private List<String> computeClassWitnesses(ClassDecl c) {
        java.util.Set<String> used = new HashSet<String>();
        java.util.Set<String> tps = new HashSet<String>(c.typeParams);
        java.util.Set<Object> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<Object, Boolean>());
        for (FieldDecl f : c.fields) {
            scanTypeValueUses(f, tps, used, seen);
        }
        for (CtorDecl ct : c.ctors) {
            if (!ct.isFactory) {
                scanTypeValueUses(ct, tps, used, seen);
            }
        }
        for (MethodDecl m : c.methods) {
            if (!m.isStatic) {
                scanTypeValueUses(m, tps, used, seen);
            }
        }
        ClassDecl sup = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        if (sup != null) {
            List<String> sw = witnessParams(sup);
            for (String n : sw) {
                int i = sup.typeParams.indexOf(n);
                TypeRef a = i < c.superclass.args.size() ? c.superclass.args.get(i) : null;
                if (a != null && a.args.isEmpty() && tps.contains(a.name)) {
                    used.add(a.name);
                }
            }
        }
        List<String> out = new ArrayList<String>();
        for (String n : c.typeParams) {
            if (used.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * The type arguments of an unannotated construction of a class that keeps witnesses,
     * inferred from the type the context expects ({@code final Box<int> b = Box();}), as
     * Dart infers them. Anything else keeps what was written.
     */
    private List<TypeRef> inferredWitnessTypeArgs(ClassDecl pc, List<TypeRef> written, TypeRef expected) {
        if (pc == null || (written != null && !written.isEmpty()) || expected == null
                || witnessParams(pc).isEmpty() || !pc.name.equals(expected.name)
                || expected.args.size() != pc.typeParams.size()) {
            return written;
        }
        return expected.args;
    }

    private boolean isProgramClassDecl(ClassDecl c) {
        List<ClassDecl> same = program.classesByName.get(c.name);
        return program.classes.get(c.name) == c || (same != null && same.contains(c));
    }

    private void scanTypeValueUses(Object o, java.util.Set<String> tps, java.util.Set<String> used,
            java.util.Set<Object> seen) {
        if (o == null || !seen.add(o)) {
            return;
        }
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                scanTypeValueUses(e, tps, used, seen);
            }
            return;
        }
        if (o instanceof Args) {
            // Args and NamedArg are not Nodes, so the reflective walk below never reached a
            // call's arguments: `print('${xs.whereType<T>()}')` left T unwitnessed (E0142).
            scanTypeValueUses(((Args) o).positional, tps, used, seen);
            for (NamedArg na : ((Args) o).named) {
                scanTypeValueUses(na.value, tps, used, seen);
            }
            return;
        }
        if (!(o instanceof Node) || o instanceof ClassDecl || o instanceof Library) {
            return;
        }
        if (o instanceof Ident && tps.contains(((Ident) o).name)) {
            used.add(((Ident) o).name);
        }
        if (o instanceof Call && !((Call) o).typeArgs.isEmpty()) {
            Call call = (Call) o;
            for (int i = 0; i < call.typeArgs.size(); i++) {
                TypeRef a = call.typeArgs.get(i);
                if (a != null && a.args.isEmpty() && tps.contains(a.name) && callNeedsWitness(call, i)) {
                    used.add(a.name);
                }
            }
        }
        if (o instanceof Call && ((Call) o).typeArgs.isEmpty() && ((Call) o).name != null
                && calleeTakesWitness((Call) o)) {
            // `registerAdapter(Adapter<T>(...))`: the callee infers its witness from T.
            collectTypeNames(((Call) o).args, tps, used, 0);
            implicitWitnessCalls++;
        }
        if (o instanceof CtorCall && ((CtorCall) o).type != null) {
            TypeRef ct = ((CtorCall) o).type;
            ClassDecl pc = program.classes.get(ct.name);
            for (int i = 0; pc != null && i < ct.args.size() && i < pc.typeParams.size(); i++) {
                TypeRef a = ct.args.get(i);
                if (a.args.isEmpty() && tps.contains(a.name) && witnessParams(pc).contains(pc.typeParams.get(i))) {
                    used.add(a.name);
                }
            }
        }
        if (o instanceof IsTest) {
            TypeRef t = ((IsTest) o).type;
            if (t != null && t.args.isEmpty() && tps.contains(t.name)) {
                used.add(t.name);
            }
        }
        java.util.Set<String> inner = tps;
        List<String> shadow = o instanceof MethodDecl ? ((MethodDecl) o).typeParams
                : o instanceof FunctionDecl ? ((FunctionDecl) o).typeParams
                : o instanceof Lambda ? ((Lambda) o).typeParams : null;
        if (shadow != null && !shadow.isEmpty()) {
            inner = new HashSet<String>(tps);
            inner.removeAll(shadow);
        }
        for (java.lang.reflect.Field f : o.getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            Class<?> ft = f.getType();
            if (Node.class.isAssignableFrom(ft) || List.class.isAssignableFrom(ft) || ft == Args.class) {
                try {
                    scanTypeValueUses(f.get(o), inner, used, seen);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    // ---- generic METHOD witnesses ------------------------------------------------------
    //
    // A generic method, function or static whose body uses one of its OWN type parameters as
    // a value -- or hands it on to something that does (a witnessed class, another witnessed
    // generic, whereType<T>) -- takes a hidden leading dart.runtime.TypeWitness per such
    // parameter. Instance methods are decided per name and type-parameter count across every
    // program class, so an override always has its overridden method's Java signature; a
    // group with a member that overrides a RUNTIME method (List.cast, Iterable.whereType)
    // takes none, because the runtime's signature cannot change, and its uses stay E0142.
    // A call passes the written type arguments, else the ones inferred from the expected
    // type and the argument types; with neither it is E0142, never a guessed witness.

    private final java.util.Map<Object, List<String>> methodWitnessCache =
            new java.util.IdentityHashMap<Object, List<String>>();
    private java.util.Map<String, List<MethodDecl>> methodsByName;
    /** Counts calls to a witnessed generic that write no type arguments (see the scan). */
    private int implicitWitnessCalls;
    private java.util.Map<MethodDecl, ClassDecl> methodOwners;

    private void indexMethods() {
        if (methodsByName != null) {
            return;
        }
        methodsByName = new java.util.HashMap<String, List<MethodDecl>>();
        methodOwners = new java.util.IdentityHashMap<MethodDecl, ClassDecl>();
        java.util.Set<ClassDecl> all = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<ClassDecl, Boolean>());
        all.addAll(program.classes.values());
        for (List<ClassDecl> l : program.classesByName.values()) {
            all.addAll(l);
        }
        for (ClassDecl c : all) {
            for (MethodDecl m : c.methods) {
                methodOwners.put(m, c);
                List<MethodDecl> l = methodsByName.get(m.name);
                if (l == null) {
                    l = new ArrayList<MethodDecl>();
                    methodsByName.put(m.name, l);
                }
                l.add(m);
            }
        }
        // Witness needs propagate along calls and super chains, which can be cyclic: start
        // every class and generic method at "none" and recompute until nothing grows.
        List<Object> decls = new ArrayList<Object>();
        for (ClassDecl c : all) {
            if (!c.typeParams.isEmpty()) {
                witnessCache.remove(c);
            }
            for (MethodDecl m : c.methods) {
                if (!m.typeParams.isEmpty()) {
                    decls.add(m);
                }
            }
        }
        for (FunctionDecl f : program.functions.values()) {
            if (!f.typeParams.isEmpty()) {
                decls.add(f);
            }
        }
        List<ClassDecl> generic = new ArrayList<ClassDecl>();
        for (ClassDecl c : all) {
            if (!c.typeParams.isEmpty()) {
                witnessCache.put(c, java.util.Collections.<String>emptyList());
                generic.add(c);
            }
        }
        for (Object d : decls) {
            methodWitnessCache.put(d, java.util.Collections.<String>emptyList());
        }
        boolean changed = true;
        for (int round = 0; changed && round < 50; round++) {
            changed = false;
            for (ClassDecl c : generic) {
                List<String> out = witnessParams(c).isEmpty() && (c.isMixin || c.extensionOn != null
                        || c.enumOf != null || !isProgramClassDecl(c))
                        ? java.util.Collections.<String>emptyList() : computeClassWitnesses(c);
                if (!out.equals(witnessCache.put(c, out))) {
                    changed = true;
                }
            }
            for (Object d : decls) {
                changed |= computeMethodWitnesses(d);
            }
        }
    }

    /** The type parameters of a MethodDecl or FunctionDecl that take a hidden witness. */
    List<String> methodWitnesses(Object decl) {
        List<String> tps = decl instanceof MethodDecl ? ((MethodDecl) decl).typeParams
                : decl instanceof FunctionDecl ? ((FunctionDecl) decl).typeParams : null;
        if (tps == null || tps.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        indexMethods();
        List<String> cached = methodWitnessCache.get(decl);
        if (cached != null) {
            return cached;
        }
        computeMethodWitnesses(decl);
        return methodWitnessCache.get(decl);
    }

    /** Computes (and caches) the witnesses of decl's override group; true when any grew. */
    private boolean computeMethodWitnesses(Object decl) {
        List<String> tps = decl instanceof MethodDecl ? ((MethodDecl) decl).typeParams
                : ((FunctionDecl) decl).typeParams;
        List<Object> group = new ArrayList<Object>();
        boolean runtimeOverride = false;
        if (decl instanceof MethodDecl && !((MethodDecl) decl).isStatic && methodOwners.containsKey(decl)) {
            MethodDecl md = (MethodDecl) decl;
            // The override group: same-named methods of classes related to a member by
            // subtyping, closed transitively -- never an unrelated class's same name.
            List<MethodDecl> cands = new ArrayList<MethodDecl>();
            for (MethodDecl o : methodsByName.get(md.name)) {
                if (!o.isStatic && o.typeParams.size() == tps.size() && o != md) {
                    cands.add(o);
                }
            }
            group.add(md);
            boolean grew = true;
            while (grew) {
                grew = false;
                for (java.util.Iterator<MethodDecl> it = cands.iterator(); it.hasNext();) {
                    MethodDecl o = it.next();
                    for (Object g : group) {
                        ClassDecl a = methodOwners.get(g);
                        ClassDecl b = methodOwners.get(o);
                        if (isProgramSubtype(a, b) || isProgramSubtype(b, a)) {
                            group.add(o);
                            it.remove();
                            grew = true;
                            break;
                        }
                    }
                }
            }
            for (Object g : group) {
                MethodDecl o = (MethodDecl) g;
                runtimeOverride |= o.isOverride && !declaredInProgramSuper(methodOwners.get(o), o.name);
            }
        } else {
            group.add(decl);
        }
        for (Object g : group) {
            if (!methodWitnessCache.containsKey(g)) {
                methodWitnessCache.put(g, java.util.Collections.<String>emptyList());
            }
        }
        if (runtimeOverride) {
            return false;
        }
        boolean changed = false;
        boolean[] need = new boolean[tps.size()];
        for (Object g : group) {
            List<String> gt = g instanceof MethodDecl ? ((MethodDecl) g).typeParams : ((FunctionDecl) g).typeParams;
            java.util.Set<String> used = new HashSet<String>();
            java.util.Set<Object> seen = java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<Object, Boolean>());
            java.util.Set<String> set = new HashSet<String>(gt);
            int implicitBefore = implicitWitnessCalls;
            TypeRef gret = g instanceof MethodDecl ? ((MethodDecl) g).returnType : ((FunctionDecl) g).returnType;
            if (g instanceof MethodDecl) {
                MethodDecl m = (MethodDecl) g;
                scanTypeValueUses(m.params, set, used, seen);
                scanTypeValueUses(m.body, set, used, seen);
                scanTypeValueUses(m.exprBody, set, used, seen);
            } else {
                FunctionDecl f = (FunctionDecl) g;
                scanTypeValueUses(f.params, set, used, seen);
                scanTypeValueUses(f.body, set, used, seen);
                scanTypeValueUses(f.exprBody, set, used, seen);
            }
            if (implicitWitnessCalls != implicitBefore) {
                // `return box(name);`: the callee infers its witness from this method's
                // return type, so the type parameters that type names are needed too.
                collectTypeNames(gret, set, used, 0);
            }
            for (int i = 0; i < gt.size(); i++) {
                need[i] |= used.contains(gt.get(i));
            }
        }
        for (Object g : group) {
            List<String> gt = g instanceof MethodDecl ? ((MethodDecl) g).typeParams : ((FunctionDecl) g).typeParams;
            List<String> out = new ArrayList<String>();
            for (int i = 0; i < gt.size(); i++) {
                if (need[i]) {
                    out.add(gt.get(i));
                }
            }
            List<String> old = methodWitnessCache.put(g, out);
            changed |= old == null || !old.equals(out);
        }
        return changed;
    }

    /**
     * The declaration a supertype reference of {@code k} names. The type reference has lost
     * its import prefix, so {@code class BoxCollection implements impl.BoxCollection} would
     * otherwise resolve to {@code k} itself; a class never names itself as a supertype, so a
     * same-named declaration from another library is the one meant.
     */
    private ClassDecl superDeclOf(ClassDecl k, TypeRef t) {
        String tn = t.name;
        int dot = tn.lastIndexOf('.');
        ClassDecl sc;
        if (dot > 0) {
            // `implements impl.ShelfBase`: the prefix names the library to look in.
            tn = tn.substring(dot + 1);
            sc = program.resolveClass(tn, k.ownerLibrary, t.name.substring(0, dot));
            if (sc != null && sc != k && tn.equals(sc.name)) {
                return sc;
            }
        } else {
            sc = program.resolveClass(tn, k.ownerLibrary);
        }
        if (sc == k || (dot > 0 && sc == null)) {
            sc = null;
            List<ClassDecl> same = program.classesByName.get(tn);
            if (same != null) {
                for (ClassDecl o : same) {
                    if (o != k) {
                        sc = o;
                        break;
                    }
                }
            }
        }
        return sc;
    }

    /** Whether program class {@code sub} is {@code sup} or reaches it through program supertypes. */
    private boolean isProgramSubtype(ClassDecl sub, ClassDecl sup) {
        if (sub == null || sup == null) {
            return false;
        }
        java.util.Set<ClassDecl> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<ClassDecl, Boolean>());
        java.util.ArrayDeque<ClassDecl> work = new java.util.ArrayDeque<ClassDecl>();
        work.add(sub);
        while (!work.isEmpty()) {
            ClassDecl k = work.poll();
            if (k == sup) {
                return true;
            }
            if (!seen.add(k)) {
                continue;
            }
            List<TypeRef> sups = new ArrayList<TypeRef>(k.interfaces);
            sups.addAll(k.mixins);
            if (k.superclass != null) {
                sups.add(k.superclass);
            }
            for (TypeRef t : sups) {
                ClassDecl sc = superDeclOf(k, t);
                if (sc != null) {
                    work.add(sc);
                }
            }
        }
        return false;
    }

    private boolean declaredInProgramSuper(ClassDecl c, String name) {
        java.util.Set<ClassDecl> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<ClassDecl, Boolean>());
        java.util.ArrayDeque<ClassDecl> work = new java.util.ArrayDeque<ClassDecl>();
        if (c != null) {
            work.add(c);
        }
        boolean first = true;
        while (!work.isEmpty()) {
            ClassDecl k = work.poll();
            if (!seen.add(k)) {
                continue;
            }
            if (!first && k.method(name) != null) {
                return true;
            }
            first = false;
            List<TypeRef> sups = new ArrayList<TypeRef>(k.interfaces);
            sups.addAll(k.mixins);
            if (k.superclass != null) {
                sups.add(k.superclass);
            }
            for (TypeRef t : sups) {
                ClassDecl sc = superDeclOf(k, t);
                if (sc != null) {
                    work.add(sc);
                }
            }
        }
        return false;
    }

    /** Whether a call written with {@code <..., tp, ...>} at {@code idx} needs tp's witness. */
    private boolean callNeedsWitness(Call c, int idx) {
        if ("whereType".equals(c.name) && c.target != null) {
            return true;
        }
        if (c.name == null) {
            return false;
        }
        ClassDecl pc = c.target == null ? program.classes.get(c.name) : null;
        if (pc != null) {
            return idx < pc.typeParams.size() && witnessParams(pc).contains(pc.typeParams.get(idx));
        }
        FunctionDecl f = c.target == null ? program.functions.get(c.name) : null;
        if (f != null && idx < f.typeParams.size() && methodWitnesses(f).contains(f.typeParams.get(idx))) {
            return true;
        }
        indexMethods();
        List<MethodDecl> ms = methodsByName.get(c.name);
        if (ms != null) {
            for (MethodDecl m : ms) {
                if (idx < m.typeParams.size() && m.typeParams.size() == c.typeArgs.size()
                        && methodWitnesses(m).contains(m.typeParams.get(idx))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean calleeTakesWitness(Call c) {
        FunctionDecl f = c.target == null ? program.functions.get(c.name) : null;
        if (f != null) {
            return !methodWitnesses(f).isEmpty();
        }
        List<MethodDecl> ms = methodsByName.get(c.name);
        if (ms != null) {
            for (MethodDecl m : ms) {
                if (!methodWitnesses(m).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Adds every type-parameter name in tps written as a type argument inside o. */
    private void collectTypeNames(Object o, java.util.Set<String> tps, java.util.Set<String> used, int depth) {
        if (o == null || depth > 40) {
            return;
        }
        if (o instanceof Ast.Args) {
            Ast.Args a = (Ast.Args) o;
            for (Expr e : a.positional) {
                collectTypeNames(e, tps, used, depth + 1);
            }
            for (Ast.NamedArg na : a.named) {
                collectTypeNames(na.value, tps, used, depth + 1);
            }
            return;
        }
        if (o instanceof TypeRef) {
            TypeRef t = (TypeRef) o;
            if (t.args.isEmpty() && tps.contains(t.name)) {
                used.add(t.name);
            }
            for (TypeRef x : t.args) {
                collectTypeNames(x, tps, used, depth + 1);
            }
            return;
        }
        if (o instanceof Call) {
            for (TypeRef t : ((Call) o).typeArgs) {
                collectTypeNames(t, tps, used, depth + 1);
            }
            collectTypeNames(((Call) o).args, tps, used, depth + 1);
        } else if (o instanceof CtorCall) {
            collectTypeNames(((CtorCall) o).type, tps, used, depth + 1);
            collectTypeNames(((CtorCall) o).args, tps, used, depth + 1);
        }
    }

    private static String withWitnesses(String w, String args) {
        return args.isEmpty() ? stripTrailingComma(w) : w + args;
    }

    /**
     * The leading witness arguments (each followed by ", ") of a call to the generic
     * MethodDecl/FunctionDecl {@code decl}, or "" when it takes none.
     */
    private String methodWitnessCall(Object decl, Call c, TypeRef expected, Ctx ctx) {
        List<String> ws = methodWitnesses(decl);
        if (decl instanceof MethodDecl && ws.isEmpty() && isWhereTypeOverride((MethodDecl) decl)) {
            return whereTypeArgs(c.typeArgs.isEmpty() ? null : c.typeArgs.get(0), c, ctx) + ", ";
        }
        if (ws.isEmpty()) {
            return "";
        }
        List<String> tps = decl instanceof MethodDecl ? ((MethodDecl) decl).typeParams : ((FunctionDecl) decl).typeParams;
        List<Param> params = decl instanceof MethodDecl ? ((MethodDecl) decl).params : ((FunctionDecl) decl).params;
        TypeRef ret = decl instanceof MethodDecl ? ((MethodDecl) decl).returnType : ((FunctionDecl) decl).returnType;
        String name = decl instanceof MethodDecl ? ((MethodDecl) decl).name : ((FunctionDecl) decl).name;
        Map<String, TypeRef> m = new LinkedHashMap<String, TypeRef>();
        if (c.typeArgs.size() == tps.size()) {
            for (int i = 0; i < tps.size(); i++) {
                m.put(tps.get(i), c.typeArgs.get(i));
            }
        } else {
            int pos = 0;
            for (Param p : params) {
                Expr a = null;
                if (p.named) {
                    for (Ast.NamedArg na : c.args.named) {
                        if (na.name.equals(p.name)) {
                            a = na.value;
                        }
                    }
                } else if (pos < c.args.positional.size()) {
                    a = c.args.positional.get(pos++);
                }
                if (a != null && !(a instanceof Lambda)) {
                    TypeRef at = inferExprTypeQuiet(a, ctx);
                    if ((at == null || at.is("dynamic")) && a instanceof Call && ((Call) a).target == null
                            && ((Call) a).name != null && program.classes.containsKey(((Call) a).name)) {
                        // An unannotated construction: its class, with what was written.
                        at = new TypeRef(((Call) a).name);
                        at.args.addAll(((Call) a).typeArgs);
                    }
                    unifyThroughSupers(p.type, at, tps, m, 0);
                }
            }
            if (expected != null && ret != null) {
                TypeRef r = ret;
                TypeRef e = expected;
                if (r.is("Future") && r.args.size() == 1 && e.is("Future") && e.args.size() == 1) {
                    // `f = await open()`: the awaited types relate through their supertypes
                    // (Jar<E> to a Vessel field), which unifying Future<..> with Future<..> misses.
                    r = r.args.get(0);
                    e = e.args.get(0);
                } else if (r.is("Future") && r.args.size() == 1 && !e.is("Future")) {
                    r = r.args.get(0);
                } else if (e.is("Future") && e.args.size() == 1 && !r.is("Future")) {
                    e = e.args.get(0);
                }
                TypeRef sup = asSuper(r, e.name, 0);
                unifyTypeVars(sup, e, tps, m);
                if (sup != null && e.args.isEmpty() && sup.name != null && sup.name.equals(e.name)) {
                    // A raw generic context, `BoxBase? f; f = await open();`: the type is
                    // BoxBase<dynamic>, so Dart infers dynamic for what it leaves open.
                    for (TypeRef sa : sup.args) {
                        if (sa != null && sa.args.isEmpty() && tps.contains(sa.name) && !m.containsKey(sa.name)) {
                            m.put(sa.name, TypeRef.DYNAMIC);
                        }
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String n : ws) {
            TypeRef t = m.get(n);
            String w = t == null ? null : witnessOf(t, ctx);
            if (w == null) {
                diags.error(c, "E0142", "Calling " + name + " needs its type argument '" + n
                        + "' at run time (its body uses " + n + " as a value), and none is "
                        + (t == null ? "written or inferable here" : "available for '" + t.name + "'")
                        + ". Write the type argument explicitly at this call.");
                w = "null";
            }
            sb.append(w).append(", ");
        }
        return sb.toString();
    }

    /** {@code t} as its program supertype named {@code name}, or t itself. */
    private TypeRef asSuper(TypeRef t, String name, int depth) {
        if (t == null || name == null || name.equals(t.name) || depth > 8) {
            return t;
        }
        ClassDecl tc = program.classes.get(t.name);
        if (tc == null) {
            return t;
        }
        List<TypeRef> sups = new ArrayList<TypeRef>(tc.interfaces);
        sups.addAll(tc.mixins);
        if (tc.superclass != null) {
            sups.add(tc.superclass);
        }
        List<TypeRef> args = new ArrayList<TypeRef>();
        for (int i = 0; i < tc.typeParams.size(); i++) {
            args.add(i < t.args.size() ? t.args.get(i) : TypeRef.DYNAMIC);
        }
        for (TypeRef s : sups) {
            TypeRef r = asSuper(substTypeParams(s, tc.typeParams, args), name, depth + 1);
            if (r != null && name.equals(r.name)) {
                return r;
            }
        }
        return t;
    }

    /** unifyTypeVars that also reads a program class argument as each of its supertypes. */
    private void unifyThroughSupers(TypeRef p, TypeRef a, List<String> tps, Map<String, TypeRef> m, int depth) {
        if (p == null || a == null || depth > 8) {
            return;
        }
        if (p.name != null && !p.name.equals(a.name) && !tps.contains(p.name) && a.funcReturn == null) {
            ClassDecl ac = program.classes.get(a.name);
            if (ac != null) {
                List<TypeRef> sups = new ArrayList<TypeRef>(ac.interfaces);
                sups.addAll(ac.mixins);
                if (ac.superclass != null) {
                    sups.add(ac.superclass);
                }
                List<TypeRef> args = new ArrayList<TypeRef>();
                for (int i = 0; i < ac.typeParams.size(); i++) {
                    args.add(i < a.args.size() ? a.args.get(i) : TypeRef.DYNAMIC);
                }
                for (TypeRef s : sups) {
                    unifyThroughSupers(p, substTypeParams(s, ac.typeParams, args), tps, m, depth + 1);
                }
                return;
            }
        }
        unifyTypeVars(p, a, tps, m);
    }

    private static String witnessField(ClassDecl owner, String tp) {
        return "$tw$" + owner.name.replace('.', '_') + "$" + tp;
    }

    /** The Java expression of the witness of type parameter {@code n} here, or null. */
    /**
     * A program override of {@code Iterable.whereType<T>()} (a delegating wrapper's
     * {@code whereType<T>() => _base.whereType<T>()}). The runtime method is
     * {@code whereType(Class, boolean)} -- T's class and whether it admits null -- and every
     * call site passes those whatever the receiver, so the override takes the same two and
     * rebuilds T's witness from them: inside, T is a real type to filter by, not E0142.
     */
    private static boolean isWhereTypeOverride(MethodDecl m) {
        return "whereType".equals(m.name) && !m.isStatic && !m.isGetter && !m.isSetter
                && m.params.isEmpty() && m.typeParams != null && m.typeParams.size() == 1;
    }

    /**
     * The two arguments of the runtime's {@code whereType(Class, boolean)} for
     * {@code whereType<sub>()} called on a program override: T's class and whether it
     * admits null, from a witness when {@code sub} is a witnessed type parameter.
     */
    private String whereTypeArgs(TypeRef sub, Node pos, Ctx ctx) {
        String subW = sub != null && sub.args.isEmpty() ? witnessFor(sub.name, ctx) : null;
        if (subW != null) {
            return "(java.lang.Class) " + subW + ".type(), " + (sub.nullable ? "true" : subW + ".test(null)");
        }
        if (sub != null && reportTypeVariableWitness(pos, sub, ctx, "'whereType<" + sub.name + ">()'")) {
            return "null, true";
        }
        String token = sub != null ? javaType(sub, true, ctx) + ".class" : "null";
        boolean admitsNull = sub == null || sub.nullable || sub.is("dynamic") || sub.is("var");
        return "(java.lang.Class) " + token + ", " + admitsNull;
    }

    private static boolean isWhereTypeOverride(Method m) {
        return "whereType".equals(m.name) && !m.isStatic && !m.isGetter && !m.isSetter
                && m.params.isEmpty() && m.typeParams != null && m.typeParams.size() == 1
                && m.witnesses.isEmpty();
    }

    private String witnessFor(String n, Ctx ctx) {
        if (n == null) {
            return null;
        }
        if (ctx.witnessLocals != null && ctx.witnessLocals.contains(n)) {
            return "$tw$" + n;
        }
        if (ctx.methodTypeParams != null && ctx.methodTypeParams.contains(n)) {
            return null;
        }
        ClassDecl c = ctx.currentClass;
        if (c == null || ctx.staticMember || !witnessParams(c).contains(n)) {
            return null;
        }
        return "this." + witnessField(c, n);
    }

    /** The witness of the type {@code t} written at a construction site, or null. */
    private String witnessOf(TypeRef t, Ctx ctx) {
        if (t == null) {
            return null;
        }
        if (t.args.isEmpty() && t.funcReturn == null) {
            String w = witnessFor(t.name, ctx);
            if (w != null) {
                return t.nullable ? "dart.runtime.TypeWitness.nullable(" + w + ")" : w;
            }
        }
        if (isTypeVariable(t, ctx)) {
            return null;
        }
        boolean nullable = t.nullable || t.is("dynamic") || t.is("var") || t.is("Null")
                || t.is("void");
        return "dart.runtime.TypeWitness.of(" + erasedJavaType(t, ctx) + ".class, " + nullable + ")";
    }

    /**
     * The leading witness arguments for constructing {@code pc} with {@code typeArgs}, each
     * followed by ", " -- or "" when {@code pc} keeps none. Reports an error when a witness
     * cannot be formed.
     */
    private String witnessArgs(ClassDecl pc, List<TypeRef> typeArgs, Node pos, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (String w : witnessArgList(pc, typeArgs, pos, ctx)) {
            sb.append(w).append(", ");
        }
        return sb.toString();
    }

    private List<String> witnessArgList(ClassDecl pc, List<TypeRef> typeArgs, Node pos, Ctx ctx) {
        List<String> sb = new ArrayList<String>();
        for (String n : witnessParams(pc)) {
            int i = pc.typeParams.indexOf(n);
            TypeRef a = typeArgs != null && typeArgs.size() == pc.typeParams.size() ? typeArgs.get(i) : null;
            if (a == null && (typeArgs == null || typeArgs.isEmpty())) {
                // Nothing written and nothing inferred from context: Dart instantiates to
                // bound (`final g = FutureGroup();` is a FutureGroup<dynamic>), so the witness
                // is the bound's, or dynamic's for an unbounded parameter.
                TypeRef bound = pc.typeParamBounds.get(n);
                a = bound != null && bound.funcReturn == null && bound.args.isEmpty()
                        && !pc.typeParams.contains(bound.name) ? bound : new TypeRef("dynamic");
            }
            String w = witnessOf(a, ctx);
            if (w == null) {
                diags.error(pos, "E0142", "Constructing " + pc.name + " needs its type argument '" + n
                        + "' at run time (its body uses " + n + " as a value), and none is "
                        + (a == null ? "written here" : "available for '" + a.name + "'")
                        + ". Write the type argument explicitly at this construction.");
                w = "null";
            }
            sb.add(w);
        }
        return sb;
    }

    /** Hidden witness parameter declarations, each followed by ", ". */
    private String witnessParamDecls(ClassDecl c) {
        StringBuilder sb = new StringBuilder();
        for (String n : witnessParams(c)) {
            sb.append("dart.runtime.TypeWitness $tw$").append(n).append(", ");
        }
        return sb.toString();
    }

    private static String stripTrailingComma(String s) {
        return s.endsWith(", ") ? s.substring(0, s.length() - 2) : s;
    }

    /**
     * Stores this class's witnesses from the hidden parameters, and -- when
     * {@code parentToo} -- the program superclass's from this class's superclass arguments
     * (for a super chain that does not pass them through a Java constructor).
     */
    private void storeWitnesses(ClassDecl c, String self, boolean own, boolean parentToo, Ctx.Writer w,
            Ctx ctx) {
        for (String n : own ? witnessParams(c) : java.util.Collections.<String>emptyList()) {
            w.line(self + "." + witnessField(c, n) + " = $tw$" + n + ";");
        }
        if (parentToo) {
            ClassDecl sup = c.superclass != null ? superDeclOf(c, c.superclass) : null;
            if (sup != null && !witnessParams(sup).isEmpty()) {
                List<String> parts = witnessArgList(sup, c.superclass.args, c, ctx);
                List<String> sw = witnessParams(sup);
                for (int i = 0; i < sw.size(); i++) {
                    w.line(self + "." + witnessField(sup, sw.get(i)) + " = " + parts.get(i) + ";");
                }
            }
        }
    }

    /**
     * Reports E0142 when {@code t} is an unresolved type VARIABLE (an enclosing generic
     * method's or class's {@code <T>}, not a concrete class) at a site that needs a runtime
     * type to check or filter by. Type parameters are erased throughout this transpiler --
     * there is no reified {@code T} to hand the runtime, the way Dart itself can -- so a
     * construct that NEEDS one here cannot be lowered correctly. Without this check, each
     * such site either (a) silently drops the check (a null {@code Class} witness read as
     * "match everything", as {@code whereType} did), or (b) emits Java that mentions the type
     * variable where erasure forbids it ({@code instanceof T}, {@code T.class}), which is
     * always a compile error in the generated sources -- never a clean build with a wrong
     * answer, but just as opaque to a developer who does not expect generated-code errors.
     * Reified generics (a hidden {@code Class<T>} witness threaded through every generic
     * call) would fix this properly; that is a pervasive, deliberately unimplemented cost
     * (see the DELIBERATE note on erased type arguments in {@link #emitExpr}'s IsTest
     * branch), so the fix here is a clear diagnostic instead of a silent or opaque failure.
     *
     * @return true when {@code t} is a type variable and the error was reported; the caller
     *         should then emit a placeholder rather than the construct it was building
     */
    private boolean reportTypeVariableWitness(Node node, TypeRef t, Ctx ctx, String construct) {
        if (!isTypeVariable(t, ctx)) {
            return false;
        }
        if (!looksLikeTypeParameter(t.name, ctx)) {
            // Not a declared type variable: a class the stubs simply do not have yet.
            StringBuilder libs = new StringBuilder();
            Library lib = ctx.library();
            if (lib != null) {
                for (String imp : lib.imports) {
                    if (imp.startsWith("dart:") || imp.startsWith("package:")) {
                        if (libs.length() > 0) {
                            libs.append(", ");
                        }
                        libs.append(imp);
                    }
                }
            }
            diags.error(node, "E0142", construct + ": unknown type '" + t.name + "' (not declared in the "
                    + (libs.length() == 0 ? "dart:core" : libs.toString()) + " stub). Confirm the file "
                    + "passes `dart analyze`, or the API is not yet implemented in the Codename One runtime.");
            return true;
        }
        diags.error(node, "E0142", construct + ": '" + t.name + "' is a type parameter here, "
                + "not a concrete type. Generics are erased, so there is no runtime Class for "
                + "'" + t.name + "' to check or filter by, and using it silently matches "
                + "everything (or fails to compile) instead of doing what Dart does. Pass a "
                + "concrete type argument at this call site, or add an explicit "
                + "java.lang.Class<" + t.name + "> parameter to the enclosing method and "
                + "thread it through by hand.");
        return true;
    }

    /**
     * {@code value as T}. A failed cast is Dart's TypeError, so it goes through
     * DartRuntime.as, which tests with isInstance and throws one. A plain Java cast threw
     * ClassCastException, which {@code on TypeError} never caught -- and on iOS threw
     * nothing, because ParparVM does not check casts. {@code as T?} lets null through.
     */
    private Out emitAsCast(AsCast c, Ctx ctx) {
        Out o = emitExpr(c.operand, null, ctx);
        String jt = javaType(c.type, true, ctx);
        TypeRef target = c.type;
        // No class to test a type variable against: Dart reifies it, Java erases it. Unlike
        // `is T` or whereType<T>(), an `as T` is lowered anyway, as an unchecked cast: it
        // yields the right value whenever the value IS a T, which is what the common
        // `json['x'] as T` / `value as T` in generic code relies on, and rejecting it would
        // break code that works. What it cannot do is throw on a mismatch -- the failure
        // surfaces later, at the first use of the value as a T. A documented limitation
        // (reified generics are not modelled), not a silent wrong answer on the happy path.
        if (isTypeVariable(target, ctx)) {
            return new Out("((" + jt + ") " + o.code + ")", c.type, true);
        }
        boolean unchecked = target.is("dynamic") || target.is("var")
                || (target.is("Object") && target.nullable)
                // An upcast to the static type it already has cannot fail.
                || (o.type != null && !isDynamic(o.type) && o.type.args.isEmpty() && target.args.isEmpty()
                    && o.type.name != null && o.type.name.equals(target.name)
                    && (target.nullable || !o.type.nullable));
        if (unchecked) {
            return new Out("((" + jt + ") " + o.code + ")", c.type);
        }
        ctx.importClass("dart.runtime.DartRuntime");
        String erased = erasedJavaType(target, ctx);
        String operand = o.code;
        if (o.type != null && !o.type.nullable
                && (o.type.is("int") || o.type.is("double") || o.type.is("bool"))) {
            operand = "(Object) " + paren(o.code);
        }
        String call = "DartRuntime.as(" + operand + ", " + erased + ".class, " + target.nullable + ", "
                + quote(target.toString()) + ")";
        // The helper answers the erased class; a parameterized target narrows it unchecked:
        // type arguments are not reified, so `as List<String>` accepts any list (see the
        // IsTest branch of emitExpr for why that is the design rather than an oversight).
        return new Out(jt.equals(erased) ? call : "((" + jt + ") " + call + ")", c.type);
    }

    private Out emitString(StringLit s, Ctx ctx) {
        if (s.parts.size() == 1 && s.parts.get(0) instanceof String) {
            return new Out(quote((String) s.parts.get(0)), TypeRef.STRING);
        }
        if (s.parts.isEmpty()) {
            return new Out("\"\"", TypeRef.STRING);
        }
        ctx.importClass("dart.runtime.DartRuntime");
        if (s.parts.size() == 1) {
            Out o = emitExpr((Expr) s.parts.get(0), null, ctx);
            return new Out("DartRuntime.str(" + o.code + ")", TypeRef.STRING);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        boolean firstIsString = s.parts.get(0) instanceof String;
        for (Object part : s.parts) {
            if (!first) {
                sb.append(" + ");
            }
            if (part instanceof String) {
                sb.append(quote((String) part));
            } else {
                Out o = emitExpr((Expr) part, null, ctx);
                sb.append("DartRuntime.str(").append(o.code).append(')');
            }
            first = false;
        }
        String code = sb.toString();
        if (!firstIsString) {
            // DartRuntime.str returns String, so + concatenation is already string-typed
        }
        return new Out(code, TypeRef.STRING);
    }

    /**
     * A Dart {@code const} collection literal -- written {@code const [...]}, or a literal
     * inside a const context, which AstBuilder marks -- is unmodifiable: {@code const xs =
     * <int>[1]; xs.add(2);} throws UnsupportedError. Emitted growable, it accepted the add.
     * The freshly built collection is frozen in place (lists, sets, int maps) or copied
     * into the read-only map, keeping its static type.
     *
     * <p>NOT canonicalized: Dart makes {@code identical(const [1], const [1])} true, and
     * this still builds the value on each evaluation. That is the model the emitter
     * already uses for {@code const} constructor calls, which are not canonicalized either.
     * Hoisting each literal into a static constant would change when its elements are
     * evaluated relative to the lazily initialised statics they may name, and would cost
     * a field per literal across a whole application; mutability is what a program can
     * observe without calling identical().</p>
     */
    private Out constCollection(Out o, Ctx ctx) {
        TypeRef t = o.type;
        if (t == null) {
            return o;
        }
        if (t.is("List")) {
            ctx.importClass("dart.core.DartList");
            return new Out("DartList.constant(" + o.code + ")", t, o.fromError);
        }
        if (t.is("Set")) {
            ctx.importClass("dart.core.DartSet");
            return new Out("DartSet.constant(" + o.code + ")", t, o.fromError);
        }
        if (t.is("Map")) {
            // By what was built, not by the type: only the plain <int, int> literal is a
            // DartLongMap; a structured one (if/for/spread) is built as a DartMap.
            if (o.code.startsWith("DartLongMap.ofLongs(")) {
                ctx.importClass("dart.core.DartLongMap");
                return new Out("DartLongMap.constant(" + o.code + ")", t, o.fromError);
            }
            ctx.importClass("dart.core.DartMap");
            return new Out("DartMap.constant(" + o.code + ")", t, o.fromError);
        }
        return o;
    }

    private Out emitListLit(ListLit l, TypeRef expected, Ctx ctx) {
        ctx.importClass("dart.core.DartList");
        TypeRef elem = l.elementType;
        if (elem == null && expected != null && expected.is("List") && !expected.args.isEmpty()) {
            elem = expected.arg(0);
        }
        boolean structured = false;
        for (Expr e : l.elements) {
            if (e instanceof SpreadElement || e instanceof IfElement || e instanceof ForElement) {
                structured = true;
                break;
            }
        }
        if (structured) {
            if (elem == null) {
                // No type argument and no context: Dart infers the element type from
                // the elements. Built as List<dynamic> instead, `x == null ? <int>[] :
                // [...x]` held a DartList where its int type promised a DartLongList,
                // and the cast to it failed.
                elem = structuredElementTypeQuiet(l.elements, ctx);
            }
            if (elem == null) {
                elem = TypeRef.DYNAMIC;
            }
            String tmp = ctx.newTemp();
            String pk = primitiveListKind(TypeRef.of("List", elem));
            if (pk != null) {
                ctx.importClass("dart.core.Dart" + pk + "List");
                ctx.writer().line("Dart" + pk + "List " + tmp + " = new Dart" + pk + "List();");
            } else {
                ctx.writer().line("DartList<" + javaType(elem, true, ctx) + "> " + tmp + " = new DartList<>();");
            }
            for (Expr e : l.elements) {
                emitListElementInto(tmp, e, elem, ctx);
            }
            return new Out(tmp, TypeRef.of("List", elem));
        }
        StringBuilder sb = new StringBuilder();
        List<String> codes = new ArrayList<String>();
        TypeRef inferred = null;
        boolean mixed = false;
        for (Expr e : l.elements) {
            Out o = emitExpr(e, elem, ctx);
            codes.add(elementCode(o, elem, ctx));
            if (inferred == null) {
                inferred = o.type;
            } else if (o.type == null || inferred.name == null || !inferred.name.equals(o.type.name)) {
                // `[1, 2.5, 'x', null]`: elements that disagree make a List<dynamic>
                // (Dart would say List<Object?>), never a list of the first one's type.
                mixed = true;
            }
        }
        if (elem == null) {
            elem = mixed || inferred == null ? TypeRef.DYNAMIC : inferred;
        }
        String pk = primitiveListKind(TypeRef.of("List", elem));
        if (pk != null) {
            ctx.importClass("dart.core.Dart" + pk + "List");
            sb.append("Dart").append(pk).append("List.of").append("Long".equals(pk) ? "Longs(" : "Doubles(");
            for (int i = 0; i < codes.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(codes.get(i));
            }
            sb.append(')');
            return new Out(sb.toString(), TypeRef.of("List", elem));
        }
        sb.append("DartList.<").append(javaType(elem, true, ctx)).append(">of(");
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(codes.get(i));
        }
        sb.append(')');
        return new Out(sb.toString(), TypeRef.of("List", elem));
    }

    /**
     * The element type every element of an untyped collection literal agrees on --
     * a spread of a typed variable, a literal, a typed variable, an if-element whose
     * branches agree -- or null when any of them cannot be told without emitting it.
     */
    private TypeRef structuredElementTypeQuiet(List<Expr> elements, Ctx ctx) {
        TypeRef common = null;
        for (Expr e : elements) {
            TypeRef t = elementTypeQuiet(e, ctx);
            if (t == null || isDynamicType(t)) {
                return null;
            }
            if (common == null) {
                common = t;
            } else if (!common.toString().equals(t.toString())) {
                return null;
            }
        }
        return common;
    }

    /**
     * {@code p.items} where {@code p} is a local of a program class type written with type
     * arguments: the field's declared type with the class's type parameters bound to them
     * ({@code SimplePipe<int>.items} is a {@code List<int>}, not a {@code List<T>}). Null
     * for anything else.
     */
    private TypeRef fieldOfTypedReceiverQuiet(Expr x, Ctx ctx) {
        if (!(x instanceof PropertyGet) || !(((PropertyGet) x).target instanceof Ident)) {
            return null;
        }
        PropertyGet pg = (PropertyGet) x;
        TypeRef recv = ctx.lookup(((Ident) pg.target).name);
        if (recv == null || recv.args.isEmpty()) {
            return null;
        }
        ClassDecl pc = program.resolveClass(recv.name, ctx.library());
        if (pc == null || pc.typeParams.size() != recv.args.size()) {
            return null;
        }
        FieldDecl f = pc.field(pg.name);
        if (f == null) {
            f = findInheritedField(pc, pg.name);
        }
        if (f == null) {
            return null;
        }
        return substituteTypeParams(fieldType(f, ctx), paramMap(pc.typeParams, recv.args));
    }

    private TypeRef elementTypeQuiet(Expr e, Ctx ctx) {
        if (e instanceof SpreadElement) {
            TypeRef st = inferExprTypeQuiet(((SpreadElement) e).expr, ctx);
            if (st == null) {
                st = fieldOfTypedReceiverQuiet(((SpreadElement) e).expr, ctx);
            }
            if (st != null && (st.is("List") || st.is("Set") || st.is("Iterable")) && st.args.size() == 1) {
                return st.arg(0);
            }
            return null;
        }
        if (e instanceof IfElement) {
            IfElement i = (IfElement) e;
            TypeRef a = elementTypeQuiet(i.thenElement, ctx);
            if (i.elseElement == null || a == null) {
                return a;
            }
            TypeRef b = elementTypeQuiet(i.elseElement, ctx);
            return b != null && a.toString().equals(b.toString()) ? a : null;
        }
        if (e instanceof IntLit) {
            return TypeRef.INT;
        }
        if (e instanceof DoubleLit) {
            return TypeRef.DOUBLE;
        }
        if (e instanceof BoolLit) {
            return TypeRef.BOOL;
        }
        if (e instanceof StringLit) {
            return TypeRef.STRING;
        }
        if (e instanceof Ident) {
            return inferExprTypeQuiet(e, ctx);
        }
        return null;
    }

    /** Lowers one collection element (plain / spread / if / for) to adds on the builder list. */
    private void emitListElementInto(String list, Expr e, TypeRef elem, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (e instanceof SpreadElement) {
            SpreadElement s = (SpreadElement) e;
            Out src = emitExpr(s.expr, null, ctx);
            if (s.nullAware) {
                String tmp = ctx.newTemp();
                w.line("var " + tmp + " = " + src.code + ";");
                w.line("if (" + tmp + " != null) {");
                ctx.indent(1);
                w.line(list + ".addAllIterable(" + tmp + ");");
                ctx.indent(-1);
                w.line("}");
            } else {
                w.line(list + ".addAllIterable(" + mixinIterableCode(src) + ");");
            }
            return;
        }
        if (e instanceof IfElement) {
            IfElement i = (IfElement) e;
            Out cond = emitExpr(i.condition, TypeRef.BOOL, ctx);
            w.line("if (" + cond.code + ") {");
            ctx.indent(1);
            emitListElementInto(list, i.thenElement, elem, ctx);
            ctx.indent(-1);
            if (i.elseElement != null) {
                w.line("} else {");
                ctx.indent(1);
                emitListElementInto(list, i.elseElement, elem, ctx);
                ctx.indent(-1);
            }
            w.line("}");
            return;
        }
        if (e instanceof ForElement) {
            final ForElement f = (ForElement) e;
            final String list$ = list;
            final TypeRef elem$ = elem;
            if (isIndexedRecordFor(f.pattern, f.iterable)) {
                emitIndexedFor(f.pattern, f.iterable, ctx, new Runnable() {
                    public void run() {
                        emitListElementInto(list$, f.body, elem$, ctx);
                    }
                });
                return;
            }
            ctx.pushScope();
            if (f.pattern != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC;
                String loopVar = ctx.newTemp();
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                ctx.declare(loopVar, et);
                List<String> binds = new ArrayList<String>();
                patternMatch(f.pattern, loopVar, et, ctx, binds);
                for (String b : binds) {
                    w.line(b);
                }
                emitListElementInto(list, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
            } else if (f.varName != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = f.varType != null && !f.varType.is("var")
                        ? f.varType
                        : (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set")
                            ? iter.type.arg(0) : TypeRef.DYNAMIC);
                String loopVar = ctx.declareShadowSafe(f.varName, et);
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                emitListElementInto(list, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
            } else {
                String initCode = "";
                if (f.init instanceof VarDeclStmt) {
                    VarDeclStmt v = (VarDeclStmt) f.init;
                    Out init = v.initializer != null ? emitExpr(v.initializer, v.type, ctx) : null;
                    TypeRef t = v.type == null || v.type.is("var")
                            ? (init != null ? init.type : TypeRef.DYNAMIC) : v.type;
                    String loopVar2 = ctx.declareShadowSafe(v.name, t);
                    initCode = javaType(t, false, ctx) + " " + loopVar2 + " = "
                            + (init != null ? coerce(init, t, ctx) : zeroValue(t));
                } else if (f.init instanceof ExprStmt) {
                    initCode = statementize(emitStatementExpr(((ExprStmt) f.init).expr, ctx).code);
                }
                emitCForLoop(initCode, f.condition, f.updates, ctx, new Runnable() {
                    public void run() {
                        emitListElementInto(list$, f.body, elem$, ctx);
                    }
                });
            }
            ctx.popScope();
            return;
        }
        Out o = emitExpr(e, elem, ctx);
        w.line(list + ".add(" + coerce(o, elem, ctx) + ");");
    }

    private Out emitSetLit(SetLit s, TypeRef expected, Ctx ctx) {
        ctx.importClass("dart.core.DartSet");
        TypeRef elem = s.elementType;
        if (elem == null && expected != null && expected.is("Set") && !expected.args.isEmpty()) {
            elem = expected.arg(0);
        }
        boolean structured = false;
        for (Expr e : s.elements) {
            if (e instanceof SpreadElement || e instanceof IfElement || e instanceof ForElement) {
                structured = true;
                break;
            }
        }
        if (structured) {
            if (elem == null) {
                elem = TypeRef.DYNAMIC;
            }
            String tmp = ctx.newTemp();
            ctx.writer().line("DartSet<" + javaType(elem, true, ctx) + "> " + tmp + " = new DartSet<>();");
            for (Expr e : s.elements) {
                emitSetElementInto(tmp, e, elem, ctx);
            }
            return new Out(tmp, TypeRef.of("Set", elem));
        }
        List<String> codes = new ArrayList<String>();
        TypeRef inferred = null;
        for (Expr e : s.elements) {
            Out o = emitExpr(e, elem, ctx);
            codes.add(elementCode(o, elem, ctx));
            if (inferred == null) {
                inferred = o.type;
            }
        }
        if (elem == null) {
            elem = inferred != null ? inferred : TypeRef.DYNAMIC;
        }
        StringBuilder sb = new StringBuilder("DartSet.<").append(javaType(elem, true, ctx)).append(">of(");
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(codes.get(i));
        }
        sb.append(')');
        return new Out(sb.toString(), TypeRef.of("Set", elem));
    }

    /** Lowers one set-literal element (plain / spread / if / for) to adds on the builder set. */
    private void emitSetElementInto(String set, Expr e, TypeRef elem, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (e instanceof SpreadElement) {
            SpreadElement s = (SpreadElement) e;
            Out src = emitExpr(s.expr, null, ctx);
            if (s.nullAware) {
                String tmp = ctx.newTemp();
                w.line("var " + tmp + " = " + src.code + ";");
                w.line("if (" + tmp + " != null) {");
                ctx.indent(1);
                w.line(set + ".addAllIterable(" + tmp + ");");
                ctx.indent(-1);
                w.line("}");
            } else {
                w.line(set + ".addAllIterable(" + mixinIterableCode(src) + ");");
            }
            return;
        }
        if (e instanceof IfElement) {
            IfElement i = (IfElement) e;
            Out cond = emitExpr(i.condition, TypeRef.BOOL, ctx);
            w.line("if (" + cond.code + ") {");
            ctx.indent(1);
            emitSetElementInto(set, i.thenElement, elem, ctx);
            ctx.indent(-1);
            if (i.elseElement != null) {
                w.line("} else {");
                ctx.indent(1);
                emitSetElementInto(set, i.elseElement, elem, ctx);
                ctx.indent(-1);
            }
            w.line("}");
            return;
        }
        if (e instanceof ForElement) {
            final ForElement f = (ForElement) e;
            final String set$ = set;
            final TypeRef elem$ = elem;
            if (isIndexedRecordFor(f.pattern, f.iterable)) {
                emitIndexedFor(f.pattern, f.iterable, ctx, new Runnable() {
                    public void run() {
                        emitSetElementInto(set$, f.body, elem$, ctx);
                    }
                });
                return;
            }
            ctx.pushScope();
            if (f.pattern != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC;
                String loopVar = ctx.newTemp();
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                ctx.declare(loopVar, et);
                List<String> binds = new ArrayList<String>();
                patternMatch(f.pattern, loopVar, et, ctx, binds);
                for (String b : binds) {
                    w.line(b);
                }
                emitSetElementInto(set, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
            } else if (f.varName != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = f.varType != null && !f.varType.is("var")
                        ? f.varType
                        : (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set")
                            ? iter.type.arg(0) : TypeRef.DYNAMIC);
                String loopVar = ctx.declareShadowSafe(f.varName, et);
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                emitSetElementInto(set, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
            } else {
                String initCode = "";
                if (f.init instanceof VarDeclStmt) {
                    VarDeclStmt v = (VarDeclStmt) f.init;
                    Out init = v.initializer != null ? emitExpr(v.initializer, v.type, ctx) : null;
                    TypeRef t = v.type == null || v.type.is("var")
                            ? (init != null ? init.type : TypeRef.DYNAMIC) : v.type;
                    String loopVar2 = ctx.declareShadowSafe(v.name, t);
                    initCode = javaType(t, false, ctx) + " " + loopVar2 + " = "
                            + (init != null ? coerce(init, t, ctx) : zeroValue(t));
                } else if (f.init instanceof ExprStmt) {
                    initCode = statementize(emitStatementExpr(((ExprStmt) f.init).expr, ctx).code);
                }
                emitCForLoop(initCode, f.condition, f.updates, ctx, new Runnable() {
                    public void run() {
                        emitSetElementInto(set$, f.body, elem$, ctx);
                    }
                });
            }
            ctx.popScope();
            return;
        }
        Out o = emitExpr(e, elem, ctx);
        w.line(set + ".add(" + coerce(o, elem, ctx) + ");");
    }

    private Out emitMapLit(MapLit m, TypeRef expected, Ctx ctx) {
        TypeRef k = m.keyType;
        TypeRef v = m.valueType;
        if (k == null && expected != null && expected.is("Map") && expected.args.size() == 2) {
            k = expected.arg(0);
            v = expected.arg(1);
        }
        if (m.structured) {
            // Collection if/for/spread in a map literal: build into a DartMap with conditional/looped put().
            ctx.importClass("dart.core.DartMap");
            TypeRef kt = k == null ? TypeRef.DYNAMIC : k;
            TypeRef vt = v == null ? TypeRef.DYNAMIC : v;
            String tmp = ctx.newTemp();
            ctx.writer().line("DartMap<" + javaType(kt, true, ctx) + ", " + javaType(vt, true, ctx)
                    + "> " + tmp + " = new DartMap<>();");
            for (Expr e : m.elements) {
                emitMapElementInto(tmp, e, kt, vt, ctx);
            }
            return new Out(tmp, TypeRef.of("Map", kt, vt));
        }
        TypeRef mapType = TypeRef.of("Map",
                k == null ? TypeRef.DYNAMIC : k, v == null ? TypeRef.DYNAMIC : v);
        // Primitive long->long path: <int,int>{...} -> DartLongMap.ofLongs(k0,v0,...) (no boxing).
        if (isPrimitiveLongMap(mapType)) {
            ctx.importClass("dart.core.DartLongMap");
            StringBuilder sb = new StringBuilder("DartLongMap.ofLongs(");
            for (int i = 0; i < m.keys.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(emitExpr(m.keys.get(i), k, ctx).code).append(", ")
                  .append(emitExpr(m.values.get(i), v, ctx).code);
            }
            sb.append(')');
            return new Out(sb.toString(), mapType);
        }
        ctx.importClass("dart.core.DartMap");
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < m.keys.size(); i++) {
            Out ko = emitExpr(m.keys.get(i), k, ctx);
            Out vo = emitExpr(m.values.get(i), v, ctx);
            if (k == null) {
                k = ko.type;
            }
            if (v == null) {
                v = vo.type;
            }
            parts.add(funcCast(ko, k, ctx));
            parts.add(funcCast(vo, v, ctx));
        }
        // A type witness lets `of` infer K,V from the declared/inferred entry types (the
        // Object... varargs otherwise erase them to Object, breaking downstream `.idx` reads).
        String witness = "";
        if (k != null && v != null && isConcreteType(k) && isConcreteType(v)) {
            witness = ".<" + javaType(k, true, ctx) + ", " + javaType(v, true, ctx) + ">";
        }
        StringBuilder sb = new StringBuilder("DartMap").append(witness.isEmpty() ? ".of(" : witness + "of(");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parts.get(i));
        }
        sb.append(')');
        return new Out(sb.toString(), TypeRef.of("Map",
                k == null ? TypeRef.DYNAMIC : k, v == null ? TypeRef.DYNAMIC : v));
    }

    /** Lowers one map-literal element (entry / spread / if / for) to put()/putAll() on the builder map. */
    private void emitMapElementInto(String map, Expr e, TypeRef kt, TypeRef vt, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (e instanceof MapEntry) {
            MapEntry me = (MapEntry) e;
            Out ko = emitExpr(me.key, kt, ctx);
            Out vo = emitExpr(me.value, vt, ctx);
            w.line(map + ".put(" + boxIfPrimitive(ko, ctx) + ", " + boxIfPrimitive(vo, ctx) + ");");
            return;
        }
        if (e instanceof SpreadElement) {
            SpreadElement s = (SpreadElement) e;
            Out src = emitExpr(s.expr, null, ctx);
            if (s.nullAware) {
                String tmp = ctx.newTemp();
                w.line("var " + tmp + " = " + src.code + ";");
                w.line("if (" + tmp + " != null) {");
                ctx.indent(1);
                w.line(map + ".addAll(" + tmp + ");");
                ctx.indent(-1);
                w.line("}");
            } else {
                w.line(map + ".addAll(" + src.code + ");");
            }
            return;
        }
        if (e instanceof IfElement) {
            IfElement i = (IfElement) e;
            Out cond = emitExpr(i.condition, TypeRef.BOOL, ctx);
            w.line("if (" + cond.code + ") {");
            ctx.indent(1);
            emitMapElementInto(map, i.thenElement, kt, vt, ctx);
            ctx.indent(-1);
            if (i.elseElement != null) {
                w.line("} else {");
                ctx.indent(1);
                emitMapElementInto(map, i.elseElement, kt, vt, ctx);
                ctx.indent(-1);
            }
            w.line("}");
            return;
        }
        if (e instanceof ForElement) {
            final ForElement f = (ForElement) e;
            final String map$ = map;
            final TypeRef kt$ = kt;
            final TypeRef vt$ = vt;
            if (isIndexedRecordFor(f.pattern, f.iterable)) {
                emitIndexedFor(f.pattern, f.iterable, ctx, new Runnable() {
                    public void run() {
                        emitMapElementInto(map$, f.body, kt$, vt$, ctx);
                    }
                });
                return;
            }
            ctx.pushScope();
            if (f.pattern != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC;
                String loopVar = ctx.newTemp();
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                ctx.declare(loopVar, et);
                List<String> binds = new ArrayList<String>();
                patternMatch(f.pattern, loopVar, et, ctx, binds);
                for (String b : binds) {
                    w.line(b);
                }
                emitMapElementInto(map, f.body, kt, vt, ctx);
                ctx.indent(-1);
                w.line("}");
            } else if (f.varName != null) {
                Out iter = emitExpr(f.iterable, null, ctx);
                TypeRef et = f.varType != null && !f.varType.is("var")
                        ? f.varType
                        : (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set")
                            ? iter.type.arg(0) : TypeRef.DYNAMIC);
                String loopVar = ctx.declareShadowSafe(f.varName, et);
                w.line("for (" + javaType(et, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                emitMapElementInto(map, f.body, kt, vt, ctx);
                ctx.indent(-1);
                w.line("}");
            } else {
                String initCode = "";
                if (f.init instanceof VarDeclStmt) {
                    VarDeclStmt v2 = (VarDeclStmt) f.init;
                    Out init = v2.initializer != null ? emitExpr(v2.initializer, v2.type, ctx) : null;
                    TypeRef t = v2.type == null || v2.type.is("var")
                            ? (init != null ? init.type : TypeRef.DYNAMIC) : v2.type;
                    String loopVar2 = ctx.declareShadowSafe(v2.name, t);
                    initCode = javaType(t, false, ctx) + " " + loopVar2 + " = "
                            + (init != null ? coerce(init, t, ctx) : zeroValue(t));
                } else if (f.init instanceof ExprStmt) {
                    initCode = statementize(emitStatementExpr(((ExprStmt) f.init).expr, ctx).code);
                }
                emitCForLoop(initCode, f.condition, f.updates, ctx, new Runnable() {
                    public void run() {
                        emitMapElementInto(map$, f.body, kt$, vt$, ctx);
                    }
                });
            }
            ctx.popScope();
            return;
        }
        diags.error(e, "E0205", "Unsupported map-literal element in emitter");
    }

    /** An `is`/promotion from a declared Iterable to List, which the view must be looked through for. */
    private static boolean iterableAsList(TypeRef from, TypeRef to) {
        return from != null && to != null && from.name != null && to.name != null
                && from.name.equals("Iterable") && to.name.equals("List");
    }

    private Out emitIdent(Ident id, TypeRef expected, Ctx ctx) {
        String n = id.name;
        TypeRef local = ctx.lookup(n);
        if (local != null) {
            String jn = ctx.javaNameOf(n);
            String base = ctx.isLate(jn) ? jn + ".get$v()" : ctx.isBoxed(n) ? jn + ".v" : jn;
            // Flow-promoted by an `is` guard: read as the narrowed type via a cast.
            TypeRef promo = ctx.promotedType(n);
            if (promo != null) {
                if (promo.name != null && program.classes.containsKey(promo.name)
                        && appliedCollectionMixin(promo.name) != null
                        && (local.name == null || !program.classes.containsKey(local.name))) {
                    // Promoted to a class applying a dart:collection mixin from a runtime
                    // collection type: the value may be the view a coercion made of it, so the
                    // cast looks through the view, as the `is` test that promoted it did.
                    base = "dart.runtime.DartRuntime.unview(" + base + ")";
                }
                if (iterableAsList(local, promo)) {
                    // promoted by `is List` from an Iterable: read the list behind the view
                    base = "dart.core.DartIterable.listBehind(" + base + ")";
                }
                return new Out("((" + javaType(promo, true, ctx) + ") " + base + ")", promo);
            }
            return new Out(base, local);
        }
        ClassDecl cc = ctx.currentClass;
        if (cc != null) {
            FieldDecl f = cc.field(n);
            if (f != null) {
                if (cc.isMixin && !f.isStatic) {
                    // interface default methods reach mixin state via accessors
                    return new Out("this.get$" + n + "()", fieldType(f, ctx));
                }
                if (isLazyStatic(f) || isLateChecked(f, cc)) {
                    return new Out((f.isStatic ? javaClassName(cc) : "this") + ".get$" + n + "()", fieldType(f, ctx));
                }
                return new Out(f.isStatic ? javaClassName(cc) + "." + n : "this." + n, fieldType(f, ctx));
            }
            MethodDecl getter = cc.getter(n);
            if (getter != null) {
                return new Out("this." + n + "()", getter.returnType);
            }
            // tear-off of an own method when a function-ish value is expected
            // (a static method tears off through the class name, never `this`)
            MethodDecl md = cc.method(n);
            if (md != null) {
                Out ad = methodTearOffAdapter(md, md.isStatic ? cc.name : null, n, id, ctx);
                if (ad != null) {
                    return ad;
                }
                return new Out((md.isStatic ? cc.name : "this") + "::" + n, new TypeRef("Function"));
            }
            // field/getter inherited from a program superclass. The declared field is
            // private in the superclass, so reads route through its public get$ accessor;
            // a getter is a public zero-arg method.
            FieldDecl inhF = findInheritedField(cc, n);
            if (inhF != null && !inhF.isStatic && inheritedFieldVisible(cc, n)) {
                return new Out("this.get$" + n + "()", fieldType(inhF, ctx));
            }
            if (!cc.isMixin) {
                // state a mixin contributes: the applying class holds it behind get$/set$
                for (TypeRef mixRef : cc.mixins) {
                    ClassDecl mx = program.classes.get(mixRef.name);
                    FieldDecl mf = mx != null && mx.isMixin ? mx.field(n) : null;
                    if (mf != null && !mf.isStatic) {
                        TypeRef ft = fieldType(mf, ctx);
                        if (mixRef.args.size() == mx.typeParams.size() && !mx.typeParams.isEmpty()) {
                            Map<String, TypeRef> sub = new LinkedHashMap<String, TypeRef>();
                            for (int i = 0; i < mx.typeParams.size(); i++) {
                                sub.put(mx.typeParams.get(i), mixRef.args.get(i));
                            }
                            ft = substituteTypeParams(ft, sub);
                        }
                        return new Out("this.get$" + n + "()", ft);
                    }
                }
            }
            MethodDecl inhG = findInheritedGetter(cc, n);
            if (inhG != null) {
                return new Out("this." + n + "()", inhG.returnType);
            }
            MethodDecl inhM = findMethodInHierarchy(cc, n);
            if (inhM != null) {
                return new Out("this::" + n, new TypeRef("Function"));
            }
            // `abstract class M implements Loc { ... line ... }`: a field or getter of an
            // IMPLEMENTED program class is only an abstract member here, reached through
            // the accessor its Java interface declares, never as a field of this class.
            Object im = findImplementedMember(cc, n);
            if (im instanceof FieldDecl) {
                return new Out("this.get$" + n + "()", fieldType((FieldDecl) im, ctx));
            }
            if (im instanceof MethodDecl) {
                return new Out("this." + n + "()", ((MethodDecl) im).returnType);
            }
            // 'widget' inside a State<T> subclass
            if (n.equals("widget") && stateTypeArg(cc) != null) {
                return new Out("this.widget()", stateTypeArg(cc));
            }
            if (n.equals("context") && isStateSubclass(cc)) {
                return new Out("this.context()", new TypeRef("BuildContext"));
            }
            if (n.equals("mounted") && isStateSubclass(cc)) {
                return new Out("this.mounted()", TypeRef.BOOL);
            }
            // bare `runtimeType` — the receiver is the implicit `this`
            if (n.equals("runtimeType")) {
                return new Out("this.getClass()", new TypeRef("Type"));
            }
            // inside an enhanced-enum body: a bare enum-constant name resolves to the
            // constant, and the implicit name()/index() intrinsics are in scope.
            if (cc.enumOf != null) {
                Ast.EnumDecl selfEnum = cc.enumOf;
                if (selfEnum.hasEntry(n)) {
                    return new Out(javaEnumName(selfEnum) + "." + n, new TypeRef(cc.name));
                }
                if (n.equals("name")) {
                    return new Out("name()", TypeRef.STRING);
                }
                if (n.equals("index")) {
                    return new Out("ordinal()", TypeRef.INT);
                }
            }
            // inherited stub getters
            String stubSuper = nearestStubSuper(cc);
            if (stubSuper != null) {
                Ast.MethodDecl sg = stubs.findMethod(stubSuper, n, true);
                if (sg != null) {
                    // Narrow a generic getter (declared `T get value`) to the concrete
                    // type argument the class fixes for its stub super.
                    TypeRef rt = inheritedStubMemberReturnType(cc, n, true);
                    return new Out("this." + n + "()", rt != null ? rt : sg.returnType);
                }
            }
        }
        if (cc != null) {
            Out onSelf = mixinOnSelf(cc, n, true, ctx);
            if (onSelf != null) {
                return emitMemberGet(onSelf, n, id, ctx);
            }
            // An abstract getter only an implemented interface declares: a mixin
            // `implements Channel<T>` reading `stream` that the applying class provides.
            // The Java type implements the interface, so `this` reads it as that type.
            TypeRef iface = interfaceDeclaring(cc, n, ctx, new java.util.HashSet<String>());
            if (iface != null) {
                return emitMemberGet(new Out("this", iface), n, id, ctx);
            }
        }
        // A bare name in an extension body is first the extension's own getter: several
        // extensions on one type (`on Iterable<int>`, `on Iterable<num>`) may each declare
        // it, and only the enclosing one is meant.
        if (ctx.currentExtension != null && ctx.currentExtension.getter(n) != null) {
            MethodDecl eg = ctx.currentExtension.getter(n);
            return new Out(ctx.currentExtension.name + "." + n + "($self)",
                    eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
        }
        // Inside `extension on List<E>`, a bare `length` is `this.length` on the extended value.
        if (ctx.extensionSelfType != null && !lexicalTopLevel(n, ctx)
                && (hasMember(ctx.extensionSelfType.name, n, true, ctx)
                || EXTENDED_CORE_GETTERS.contains(ctx.extensionSelfType.name + "." + n))) {
            return emitMemberGet(new Out("$self", ctx.extensionSelfType), n, id, ctx);
        }
        // `extension on FileSpan` reading `length`, which only an interface FileSpan
        // implements (SourceSpan) declares: still a member of the extended value.
        if (ctx.extensionSelfType != null && !lexicalTopLevel(n, ctx)) {
            ClassDecl onCls = program.resolveClass(ctx.extensionSelfType.name, ctx.library());
            if (onCls != null && interfaceDeclaring(onCls, n, ctx, new java.util.HashSet<String>()) != null) {
                return emitMemberGet(new Out("$self", ctx.extensionSelfType), n, id, ctx);
            }
        }
        Out top = resolveTopLevel(n, ctx);
        tearOffKeepAll = false;
        if (top != null) {
            return top;
        }
        // A type parameter used as a value (`Type get type => T`, `T == int`): its witness.
        String tw = witnessFor(n, ctx);
        if (tw != null) {
            return new Out(tw + ".type()", new TypeRef("Type"));
        }
        // `dynamic` as a value (`T == dynamic`): the class a dynamic witness carries,
        // which is Object's -- erasure cannot tell `dynamic` from `Object` apart.
        if (n.equals("dynamic") && ctx.lookup(n) == null) {
            return new Out("Object.class", new TypeRef("Type"));
        }
        // dart:core `print` as a value (`list.forEach(print)`): a tear-off of the runtime's
        // print. Bare, so the call site's parameter supplies the functional interface; the
        // Object overload accepts any element type.
        if (n.equals("print") && ctx.lookup(n) == null) {
            ctx.importClass("dart.runtime.DartRuntime");
            TypeRef ft = new TypeRef("Function");
            ft.funcParams = new ArrayList<TypeRef>();
            ft.funcParams.add(TypeRef.DYNAMIC);
            ft.funcReturn = TypeRef.VOID;
            return new Out("DartRuntime::print", ft);
        }
        diags.error(id, "E0129", "Cannot resolve identifier '" + n
                + "'. Confirm the file passes `dart analyze`, or the API is not yet implemented in the Codename One runtime.");
        return new Out(n, TypeRef.DYNAMIC, true);
    }

    /**
     * Resolves a name against whole-program top-level scope: a user or stub class /
     * enum, a top-level const/var, a top-level function (as a method reference), or a
     * stub-contributed top-level value. Returns null when the name is not top-level.
     * Shared by bare-identifier resolution and import-prefix ({@code prefix.name})
     * member access — both denote the same global namespace in the single-package model.
     */
    /**
     * Set while an untyped local's initializer is a bare name (`final f = jsonEncode;`):
     * a stub function torn off there keeps every parameter, as its Dart type does.
     */
    private boolean tearOffKeepAll;

    /**
     * Whether a bare name in an extension body is in the library's lexical scope, which
     * Dart searches before the extended type's members: a program top-level declaration,
     * a runtime class, or a dart: library function this library imports. A runtime
     * library function that is NOT imported (dart:math's `log` with no import) is not in
     * scope, so `log.add(..)` reaches the extended class's `log` field, as in Dart.
     */
    private boolean lexicalTopLevel(String n, Ctx ctx) {
        if (resolveTopLevel(n, ctx) == null) {
            return false;
        }
        return program.classes.containsKey(n) || program.enums.containsKey(n)
                || program.topLevelVars.containsKey(n) || program.functions.containsKey(n)
                || stubs.isStubClass(n) || stubs.isStubEnum(n) || CORE_STATIC_RECEIVERS.contains(n)
                || n.equals("Future") || n.equals("Duration") || n.equals("Iterable")
                || stubs.topLevelVars.containsKey(n) || libraryStubFunction(n, ctx) != null;
    }

    private Out resolveTopLevel(String n, Ctx ctx) {
        return resolveTopLevel(n, null, ctx);
    }

    /**
     * @param prefix the import prefix the name was accessed through ({@code prefix.n}),
     *               or null for a bare identifier — used to disambiguate a top-level
     *               name declared in several libraries.
     */
    private Out resolveTopLevel(String n, String prefix, Ctx ctx) {
        if (program.classes.containsKey(n) || program.enums.containsKey(n)
                || stubs.isStubClass(n) || stubs.isStubEnum(n)
                || n.equals("Future") || n.equals("Duration") || n.equals("Iterable")
                || CORE_STATIC_RECEIVERS.contains(n)) {
            return new Out(n, classRef(n));
        }
        if (program.topLevelVars.containsKey(n)) {
            Library owner = program.resolveTopLevelVarOwner(n, ctx.library(), prefix);
            FieldDecl v = program.topLevelVars.get(n);
            // A lazily-initialised variable is an accessor pair, not a field —
            // see isLazyTopLevel for why it cannot be a field.
            String ref = Program.libClassName(owner.fileName)
                    + (isLazyTopLevel(v) ? ".get$" + n + "()" : "." + n);
            return new Out(ref, fieldType(v, ctx));
        }
        if (program.functions.containsKey(n)) {
            Library owner = program.resolveFunctionOwner(n, ctx.library(), prefix);
            FunctionDecl fn = program.functions.get(n);
            if (fn.isGetter) {
                // top-level getter access: `x` -> `OwnerLib.x()`
                TypeRef rt = fn.returnType == null || fn.returnType.is("var")
                        ? TypeRef.DYNAMIC : fn.returnType;
                return new Out(Program.libClassName(owner.fileName) + "." + n + "()", rt);
            }
            String ref = Program.libClassName(owner.fileName) + "::" + n;
            TypeRef ft = new TypeRef("Function");
            ft.funcParams = new ArrayList<TypeRef>();
            // A generic function's tear-off (`identity<int>` instantiates it, and Java
            // erases that) is typed with its type parameters as dynamic.
            boolean simple = fn.params.size() <= 5
                    && fn.returnType != null && !fn.returnType.is("var") && !fn.isAsync;
            boolean defaults = false;
            List<Param> ordered = new ArrayList<Param>();
            for (int pass = 0; pass < 3; pass++) {
                for (Param fp : fn.params) {
                    int kind = fp.named ? 2 : fp.optional || fp.defaultValue != null ? 1 : 0;
                    if (kind == pass) {
                        ordered.add(fp);
                    }
                }
                if (pass == 0 && ordered.size() < fn.params.size()) {
                    ft.funcRequired = ordered.size();
                }
            }
            for (Param fp : ordered) {
                if (fp.named) {
                    if (ft.funcNamed == null) {
                        ft.funcNamed = new ArrayList<String>();
                    }
                    ft.funcNamed.add(fp.name);
                }
                if (fp.defaultValue != null) {
                    defaults = true;
                }
                TypeRef pt = paramValueType(fp);
                ft.funcParams.add(fn.typeParams.isEmpty() ? pt
                        : substTypeParams(pt, fn.typeParams, java.util.Collections.<TypeRef>emptyList()));
            }
            if (simple) {
                ft.funcReturn = fn.typeParams.isEmpty() ? fn.returnType
                        : substTypeParams(fn.returnType, fn.typeParams, java.util.Collections.<TypeRef>emptyList());
                String sam = javaType(ft, false, ctx);
                if (ft.funcRequired >= 0 && ft.funcRequired < ordered.size()) {
                    // Optional parameters: an adapter restores defaults (see tearOffAdapter).
                    final String lib = Program.libClassName(owner.fileName) + "." + n;
                    Out ad = tearOffAdapter(ordered, ft.funcParams, ft.funcReturn,
                            names -> lib + "(" + String.join(", ", names) + ")", ctx);
                    if (ad != null) {
                        return ad;
                    }
                }
                // A typed SAM cast makes the tear-off a standalone expression: a list
                // element or a `??` operand gives a bare method reference no target type.
                if (!fn.typeParams.isEmpty()) {
                    // A cast would pin the erased Func<Object..>; a bare reference takes
                    // whatever instantiation the context names, and ft types a local.
                    return new Out(ref, ft);
                }
                return new Out("((" + sam + ") " + ref + ")", ft);
            }
            return new Out(ref, new TypeRef("Function"));
        }
        // Top-level library values contributed by a stub (@JavaName maps the
        // Dart top-level `pi` / `timeDilation` / `defaultTargetPlatform` to a
        // fully-qualified Java static field). Both reads and writes route here.
        FieldDecl stubVar = stubs.topLevelVars.get(n);
        // A stub top-level function torn off as a value (`stream.map(jsonDecode)`): a
        // lambda over every parameter, an adapter restoring the omitted optional ones.
        FunctionDecl sfn = stubVar != null ? null : libraryStubFunction(n, ctx);
        if (sfn == null && stubVar == null) {
            sfn = stubs.functions.get(n);
        }
        if (sfn != null && sfn.javaName != null && !sfn.isGetter && sfn.params.size() <= 5) {
            int dot = sfn.javaName.lastIndexOf('.');
            String cls = sfn.javaName.substring(0, dot);
            ctx.importClass(cls);
            final String target = cls.substring(cls.lastIndexOf('.') + 1) + "." + sfn.javaName.substring(dot + 1);
            List<TypeRef> ptypes = new ArrayList<TypeRef>();
            for (Param fp : sfn.params) {
                TypeRef pt = paramValueType(fp);
                ptypes.add(sfn.typeParams.isEmpty() ? pt
                        : substTypeParams(pt, sfn.typeParams, java.util.Collections.<TypeRef>emptyList()));
            }
            TypeRef ret = sfn.returnType == null || sfn.returnType.is("var") ? TypeRef.DYNAMIC
                    : sfn.typeParams.isEmpty() ? sfn.returnType
                    : substTypeParams(sfn.returnType, sfn.typeParams, java.util.Collections.<TypeRef>emptyList());
            // The value takes the required parameters; an optional one is passed as null,
            // which every stub default is (`jsonDecode(s, {reviver})`), so the value fits
            // the one-argument function a `map` or `where` expects.
            List<TypeRef> req = new ArrayList<TypeRef>();
            List<String> names = new ArrayList<String>();
            List<String> callArgs = new ArrayList<String>();
            for (int i = 0; i < sfn.params.size(); i++) {
                Param fp = sfn.params.get(i);
                if (fp.named || fp.optional || fp.defaultValue != null) {
                    callArgs.add("null");
                } else {
                    req.add(ptypes.get(i));
                    names.add("$a" + i);
                    callArgs.add("$a" + i);
                }
            }
            TypeRef ft = new TypeRef("Function");
            // Only an untyped local's value (`final f = jsonDecode;`) keeps them all: read
            // as an argument (`map(jsonDecode)`) it stays a function of the required
            // arity, the optional ones passed as null, so it fits a one-argument SAM.
            boolean keepAll = tearOffKeepAll;
            tearOffKeepAll = false;
            if (keepAll) {
                // Optional parameters: a full-arity value, as a user function's tear-off
                // is, so `f(x, 2)` through the value reaches the stub with its 2 (Dart
                // semantics); tearOffAdapter restores what a caller omits.
                List<Param> ordered = new ArrayList<Param>();
                List<TypeRef> otypes = new ArrayList<TypeRef>();
                for (int pass = 0; pass < 3; pass++) {
                    for (int i = 0; i < sfn.params.size(); i++) {
                        Param fp = sfn.params.get(i);
                        int kind = fp.named ? 2 : fp.optional || fp.defaultValue != null ? 1 : 0;
                        if (kind == pass) {
                            ordered.add(fp);
                            otypes.add(ptypes.get(i));
                        }
                    }
                }
                final List<Param> declared = sfn.params;
                final List<Param> inOrder = ordered;
                Out ad = tearOffAdapter(ordered, otypes, ret, nm -> {
                    // back to declaration order for the Java stub method
                    List<String> a = new ArrayList<String>();
                    for (Param dp : declared) {
                        a.add(nm.get(inOrder.indexOf(dp)));
                    }
                    return target + "(" + String.join(", ", a) + ")";
                }, ctx);
                if (ad != null) {
                    return ad;
                }
            }
            ft.funcParams = req;
            ft.funcReturn = ret;
            return new Out("((" + javaType(ft, false, ctx) + ") (" + String.join(", ", names) + ") -> "
                    + target + "(" + String.join(", ", callArgs) + "))", ft);
        }
        if (stubVar != null) {
            return new Out(stubVar.javaName, fieldType(stubVar, ctx));
        }
        return null;
    }

    /** Whether {@code target} is an {@code import '...' as name} prefix, not a value. */
    private boolean isImportPrefix(Expr target, Ctx ctx) {
        return target instanceof Ident
                && ctx.lookup(((Ident) target).name) == null
                && (ctx.currentClass == null || ctx.currentClass.field(((Ident) target).name) == null)
                && program.importPrefixes.contains(((Ident) target).name)
                && !program.topLevelVars.containsKey(((Ident) target).name)
                && !program.classes.containsKey(((Ident) target).name)
                && !program.enums.containsKey(((Ident) target).name);
    }

    private FieldDecl findInheritedField(ClassDecl c, String name) {
        ClassDecl s = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        while (s != null) {
            FieldDecl f = s.field(name);
            if (f != null) {
                return f;
            }
            s = s.superclass != null ? superDeclOf(s, s.superclass) : null;
        }
        return null;
    }

    /**
     * Whether an inherited field is in scope by its bare name. Every instance field has a
     * public get$/set$ pair, so a library-private `_x` is reachable too -- but only from the
     * library that declares it: Dart privacy is per library, and a subclass elsewhere that
     * says `_x` means something of its own.
     */
    /**
     * The instance field or getter {@code name} of a program class that {@code c} (or one
     * of its superclasses) lists under {@code implements}, searched transitively; null
     * when none declares it. Private names are never inherited through an interface.
     */
    private Object findImplementedMember(ClassDecl c, String name) {
        if (name.startsWith("_")) {
            return null;
        }
        java.util.Deque<ClassDecl> work = new java.util.ArrayDeque<ClassDecl>();
        java.util.Set<ClassDecl> seen = new java.util.HashSet<ClassDecl>();
        for (ClassDecl k = c; k != null && seen.add(k); k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            for (TypeRef up : k.interfaces) {
                ClassDecl u = program.classes.get(up.name);
                if (u != null) {
                    work.add(u);
                }
            }
        }
        seen.clear();
        while (!work.isEmpty()) {
            ClassDecl u = work.poll();
            if (!seen.add(u)) {
                continue;
            }
            FieldDecl f = u.field(name);
            if (f != null && !f.isStatic) {
                return f;
            }
            MethodDecl g = u.getter(name);
            if (g != null && !g.isStatic) {
                return g;
            }
            for (TypeRef up : u.interfaces) {
                ClassDecl x = program.classes.get(up.name);
                if (x != null) {
                    work.add(x);
                }
            }
            if (u.superclass != null) {
                ClassDecl x = superDeclOf(u, u.superclass);
                if (x != null) {
                    work.add(x);
                }
            }
        }
        return null;
    }

    private boolean inheritedFieldVisible(ClassDecl c, String name) {
        if (!name.startsWith("_")) {
            return true;
        }
        ClassDecl s = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        while (s != null) {
            if (s.field(name) != null) {
                return sameLibrary(s, c);
            }
            s = s.superclass != null ? superDeclOf(s, s.superclass) : null;
        }
        return false;
    }

    private static boolean sameLibrary(ClassDecl a, ClassDecl b) {
        if (a.ownerLibrary != null || b.ownerLibrary != null) {
            return a.ownerLibrary == b.ownerLibrary;
        }
        // Package libraries carry no owner: the code passed the analyzer, so a private
        // name it reaches through a superclass is that library's (its parts share it).
        return true;
    }

    private MethodDecl findInheritedGetter(ClassDecl c, String name) {
        ClassDecl s = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        while (s != null) {
            MethodDecl g = s.getter(name);
            if (g != null) {
                return g;
            }
            s = s.superclass != null ? superDeclOf(s, s.superclass) : null;
        }
        // declared (usually abstract) by an interface the class or a superclass implements:
        // `abstract class M implements Span` reads `start` before any subclass defines it
        return interfaceGetter(c, name, new java.util.HashSet<ClassDecl>());
    }

    private MethodDecl interfaceGetter(ClassDecl c, String name, java.util.Set<ClassDecl> seen) {
        for (ClassDecl k = c; k != null && seen.add(k);
                k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
            java.util.List<TypeRef> sups = new java.util.ArrayList<TypeRef>(k.interfaces);
            sups.addAll(k.mixins);
            for (TypeRef i : sups) {
                ClassDecl ic = program.classes.get(i.name);
                if (ic == null) {
                    continue;
                }
                MethodDecl g = ic.getter(name);
                if (g == null) {
                    g = interfaceGetter(ic, name, seen);
                }
                if (g != null) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * {@code SomeEnum.values} — Dart exposes an enum's members as a GETTER returning a
     * {@code List}, Java as a static method returning an ARRAY.
     *
     * <p>Both halves of that matter. Emitting the Dart spelling is a field access to
     * something that does not exist; emitting the bare {@code values()} hands back an array
     * to code that will go on to call Dart's list members on it. Wrapping restores the
     * declared type, so indexing, iteration and {@code length} all work as written.</p>
     */
    private Out enumValues(String enumName, Ctx ctx) {
        ctx.importClass("dart.core.DartList");
        return new Out("DartList.of(" + simpleEnumName(enumName, ctx) + ".values())",
                TypeRef.of("List", new TypeRef(enumName)));
    }

    private Out emitPropertyGet(PropertyGet pg, Ctx ctx) {
        // `ids.map(int.parse)`: a tear-off of the numeric parsers, as the method reference
        // the call form already lowers to (see emitPrimitiveStaticCall).
        if (pg.target instanceof Ident && (pg.name.equals("parse") || pg.name.equals("tryParse"))) {
            String tn = ((Ident) pg.target).name;
            if ((tn.equals("int") || tn.equals("double")) && ctx.lookup(tn) == null) {
                ctx.importClass("dart.core.DString");
                return new Out("DString::" + pg.name + (tn.equals("int") ? "Int" : "Double"),
                        new TypeRef("Function"));
            }
        }
        // `prefix.member` where prefix is an `import '...' as prefix` name: the member
        // is a top-level const/var/class/enum/function of another user (or stub) library.
        // In the whole-program single-package model that is just a global top-level lookup.
        if (isImportPrefix(pg.target, ctx)) {
            // deferred import: `m.loadLibrary` is a tear-off of type Future<void> Function().
            // We compile everything ahead-of-time, so the library is always loaded; the
            // tear-off is a supplier of an already-completed future.
            if (pg.name.equals("loadLibrary")) {
                ctx.importClass("dart.async.Future");
                return new Out("(() -> Future.value(null))", new TypeRef("Function"));
            }
            Out top = resolveTopLevel(pg.name,
                    pg.target instanceof Ident ? ((Ident) pg.target).name : null, ctx);
            if (top != null) {
                return top;
            }
        }
        // `SomeEnum.values` - Dart exposes an enum's members as a GETTER, Java as a static
        // METHOD. Intercepted here rather than in the class-reference path below because a
        // bare `Enum.values` (a for-in subject, say) arrives as a plain property get whose
        // target is a type name, not a class reference.
        if (pg.name.equals("values") && pg.target instanceof Ident
                && ctx.lookup(((Ident) pg.target).name) == null) {
            String en = ((Ident) pg.target).name;
            if (program.enums.containsKey(en) || stubs.isStubEnum(en)) {
                importEnum(en, ctx);
                return enumValues(en, ctx);
            }
        }
        // Named constants on the primitive numeric types (double.infinity, double.nan, ...).
        // These reach us as `<typeName>.<member>`; the type name is not a resolvable
        // expression on its own, so intercept before trying to emit it as a target.
        if (pg.target instanceof Ident && ctx.lookup(((Ident) pg.target).name) == null) {
            Out prim = emitPrimitiveTypeConstant(((Ident) pg.target).name, pg.name);
            if (prim != null) {
                return prim;
            }
        }
        Out target = emitExprRaw(pg.target, null, ctx);
        if (pg.nullAware) {
            // a?.b -> lift `$t = a` (materializing any guard a itself carries) and start a
            // new short: the member reads on the non-null $t, and $t being null shorts the
            // rest of the chain. The guard is NOT wrapped here so trailing plain selectors
            // (`.c()`) fold into the same conditional.
            Out mat = materializeShort(target);
            String tmp = ctx.newTemp();
            ctx.writer().line("var " + tmp + " = " + mat.code + ";");
            Out member = emitMemberGet(new Out(tmp, copyNonNull(mat.type), mat.fromError), pg.name, pg, ctx);
            return new Out(member.code, boxType(member.type), member.fromError, tmp);
        }
        Out member = emitMemberGet(target, pg.name, pg, ctx);
        // a plain selector after a `?.` stays inside the short (a?.b.c)
        return member.withShort(target.shortGuard);
    }

    /** dart:core named constants on the primitive numeric types, e.g. {@code double.infinity}. */
    private Out emitPrimitiveTypeConstant(String type, String member) {
        if (type.equals("double")) {
            if (member.equals("infinity")) {
                return new Out("Double.POSITIVE_INFINITY", TypeRef.DOUBLE);
            }
            if (member.equals("negativeInfinity")) {
                return new Out("Double.NEGATIVE_INFINITY", TypeRef.DOUBLE);
            }
            if (member.equals("nan")) {
                return new Out("Double.NaN", TypeRef.DOUBLE);
            }
            if (member.equals("maxFinite")) {
                return new Out("Double.MAX_VALUE", TypeRef.DOUBLE);
            }
            if (member.equals("minPositive")) {
                return new Out("Double.MIN_VALUE", TypeRef.DOUBLE);
            }
        }
        return null;
    }

    /** Property access driven by the target's static type. */
    /**
     * Inside {@code mixin M on S}, a bare member that M does not declare is S's: the mixin is
     * a Java interface, so it is reached through {@code ((S) this)}. Null when {@code cc} is
     * not such a mixin or no {@code on} type has the member.
     */
    /** Getters of core collection types a bare identifier in an extension on them may name. */
    private static final java.util.Set<String> EXTENDED_CORE_GETTERS = new java.util.HashSet<String>(
            java.util.Arrays.asList("List.length", "List.isEmpty", "List.isNotEmpty", "List.first",
                    "List.last", "List.reversed", "List.iterator", "Iterable.length", "Iterable.isEmpty",
                    "Iterable.isNotEmpty", "Iterable.first", "Iterable.last", "Iterable.iterator",
                    "Set.length", "Set.isEmpty", "Set.isNotEmpty", "Map.length", "Map.keys", "Map.values",
                    "Map.entries", "Map.isEmpty", "Map.isNotEmpty", "String.length", "String.isEmpty",
                    "String.isNotEmpty", "String.codeUnits"));

    /** The implemented interface (program class) of {@code cc} or its supertypes declaring getter or field n. */
    /** Getters every core List/Iterable/Set has, readable as a bare name in an implementor. */
    private static final java.util.Set<String> CORE_COLLECTION_GETTERS = new java.util.HashSet<String>(
            java.util.Arrays.asList("first", "last", "length", "isEmpty", "isNotEmpty", "single",
                    "iterator", "reversed"));

    private TypeRef interfaceDeclaring(ClassDecl cc, String n, Ctx ctx, java.util.Set<String> seen) {
        if (cc == null || !seen.add(cc.name)) {
            return null;
        }
        for (TypeRef i : cc.interfaces) {
            ClassDecl ic = program.resolveClass(i.name, ctx.library());
            if (ic == null) {
                // `abstract class C<E> implements List<E>` reading a bare `first`: the
                // getter is the core collection's, which the applying class provides.
                if ((i.is("List") || i.is("Iterable") || i.is("Set")) && CORE_COLLECTION_GETTERS.contains(n)) {
                    return i;
                }
                continue;
            }
            if (ic.getter(n) != null || ic.field(n) != null) {
                return i;
            }
            TypeRef deeper = interfaceDeclaring(ic, n, ctx, seen);
            if (deeper != null) {
                return deeper;
            }
        }
        ClassDecl sup = cc.superclass != null ? program.resolveClass(cc.superclass.name, ctx.library()) : null;
        return interfaceDeclaring(sup, n, ctx, seen);
    }

    /** Methods every core List/Iterable/Set has, callable as a bare name in a mixin on one. */
    private static final java.util.Set<String> CORE_COLLECTION_METHODS = new java.util.HashSet<String>(
            java.util.Arrays.asList("elementAt", "contains", "map", "where", "whereType", "expand",
                    "forEach", "toList", "toSet", "any", "every", "join", "fold", "reduce", "skip",
                    "take", "skipWhile", "takeWhile", "firstWhere", "lastWhere", "singleWhere",
                    "followedBy", "cast"));

    /** The core List/Iterable/Set a mixin is declared {@code on}, or null. */
    private TypeRef coreCollectionOn(ClassDecl cc) {
        if (cc == null) {
            return null;
        }
        if (cc.isMixin) {
            for (TypeRef on : cc.mixinOn) {
                TypeRef core = coreCollectionVia(on, new java.util.HashSet<ClassDecl>());
                if (core != null) {
                    return core;
                }
            }
        }
        // A class with no runtime superclass that only IMPLEMENTS a collection
        // through a program interface (`abstract class M<E> implements Coll<E>`, where
        // `Coll<E> implements List<E>`), used as a mixin by a class that also applies
        // ListMixin: its bare `first` and `for (x in this)` are the applier's, reached the
        // same way as in a mixin `on List<E>`.
        // The same holds for a class extending a program IterableBase/ListBase subclass,
        // whose Java type is the mixin interface and not the collection.
        if (nearestStubSuper(cc) == null) {
            TypeRef self = new TypeRef(cc.name);
            for (String tp : cc.typeParams) {
                self.args.add(new TypeRef(tp));
            }
            return coreCollectionVia(self, new java.util.HashSet<ClassDecl>());
        }
        return null;
    }

    /**
     * The core List/Iterable/Set that type t is or reaches through program supertypes, with
     * t's type arguments substituted along the way; null when it reaches none.
     */
    private TypeRef coreCollectionVia(TypeRef t, java.util.Set<ClassDecl> seen) {
        if (t == null) {
            return null;
        }
        ClassDecl pc = program.classes.get(t.name);
        if (pc == null) {
            if (!stubs.isStubClass(t.name) && (t.is("List") || t.is("Iterable") || t.is("Set"))) {
                return t;
            }
            // dart:collection's base classes and mixins are the collection they implement.
            String core = t.is("IterableBase") || t.is("IterableMixin") ? "Iterable"
                    : t.is("ListBase") || t.is("ListMixin") ? "List"
                    : t.is("SetBase") || t.is("SetMixin") ? "Set" : null;
            if (core == null) {
                return null;
            }
            TypeRef r = new TypeRef(core);
            r.args.addAll(t.args);
            return r;
        }
        if (!seen.add(pc)) {
            return null;
        }
        List<TypeRef> sups = new ArrayList<TypeRef>();
        if (pc.superclass != null) {
            sups.add(pc.superclass);
        }
        // A collection base class may be recorded as a mixin (`extends IterableBase<int>`).
        sups.addAll(pc.mixins);
        sups.addAll(pc.interfaces);
        sups.addAll(pc.mixinOn);
        for (TypeRef s : sups) {
            TypeRef core = coreCollectionVia(s, seen);
            if (core != null) {
                return substituteTypeParam(core, pc.typeParams, t.args);
            }
        }
        return null;
    }

    /**
     * `this` in a mixin on a core List/Iterable/Set, as that collection. The applying class
     * may be a runtime collection or a program ListBase/IterableBase/SetBase subclass that is
     * not one in Java, so dart.core.MixinSelf picks the object itself or its live view; a
     * plain cast would fail for the second kind. Typed lists (DartLongList) keep the cast.
     */
    private Out coreSelf(TypeRef coreOn, Ctx ctx) {
        String jt = javaType(coreOn, false, ctx);
        int lt = jt.indexOf('<');
        String raw = lt > 0 ? jt.substring(0, lt) : jt;
        String fn = "DartIterable".equals(raw) ? "iterable" : "DartList".equals(raw) ? "list"
                : "DartSet".equals(raw) ? "set" : null;
        if (fn == null) {
            return new Out("((" + jt + ") this)", coreOn);
        }
        String targ = lt > 0 ? "<" + jt.substring(lt + 1, jt.length() - 1) + ">" : "";
        return new Out("dart.core.MixinSelf." + targ + fn + "(this)", coreOn);
    }

    private Out mixinOnSelf(ClassDecl cc, String n, boolean getter, Ctx ctx) {
        TypeRef coreOn = coreCollectionOn(cc);
        if (!cc.isMixin || cc.mixinOn.isEmpty()) {
            if (coreOn != null && (getter ? CORE_COLLECTION_GETTERS : CORE_COLLECTION_METHODS).contains(n)) {
                return coreSelf(coreOn, ctx);
            }
            return null;
        }
        // A member the `on` program class declares itself wins over the core one.
        for (TypeRef on : cc.mixinOn) {
            if (program.classes.get(on.name) != null && hasMember(on.name, n, getter, ctx)) {
                return new Out("((" + javaType(on, false, ctx) + ") this)", on);
            }
        }
        if (coreOn != null && (getter ? CORE_COLLECTION_GETTERS : CORE_COLLECTION_METHODS).contains(n)) {
            // `mixin M on Iterable<E>` reading a bare `first`: the member is the core
            // collection's, reached through the type the mixin is applied to.
            return coreSelf(coreOn, ctx);
        }
        for (TypeRef on : cc.mixinOn) {
            if (hasMember(on.name, n, getter, ctx)) {
                return new Out("((" + javaType(on, false, ctx) + ") this)", on);
            }
        }
        return null;
    }

    /** Whether the named program or stub class, or one of its superclasses, has member n. */
    private boolean hasMember(String cls, String n, boolean getter, Ctx ctx) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String cur = cls;
        while (cur != null && seen.add(cur)) {
            ClassDecl pc = program.resolveClass(cur, ctx.library());
            if (pc == null) {
                return stubs.isStubClass(cur) && (stubs.findMethod(cur, n, getter) != null
                        || (getter && stubs.findMethod(cur, n, false) != null));
            }
            if (pc.method(n) != null || pc.field(n) != null || (getter && pc.getter(n) != null)) {
                return true;
            }
            for (TypeRef m : pc.mixins) {
                ClassDecl mx = program.resolveClass(m.name, ctx.library());
                if (mx != null && (mx.method(n) != null || mx.field(n) != null || mx.getter(n) != null)) {
                    return true;
                }
            }
            cur = pc.superclass != null ? pc.superclass.name : null;
        }
        return false;
    }

    /** The first non-program (core or stub) supertype of program class {@code c}, or null. */
    private TypeRef coreAncestor(ClassDecl c, java.util.Set<ClassDecl> seen) {
        if (c == null || !seen.add(c)) {
            return null;
        }
        List<TypeRef> sups = new ArrayList<TypeRef>();
        if (c.superclass != null) {
            sups.add(c.superclass);
        }
        sups.addAll(c.interfaces);
        for (TypeRef t : sups) {
            ClassDecl pc = program.classes.get(t.name);
            if (pc == null) {
                if (t.is("List") || t.is("Iterable") || t.is("Set") || t.is("Map")) {
                    return t;
                }
                continue;
            }
            TypeRef up = coreAncestor(pc, seen);
            if (up != null) {
                return up;
            }
        }
        return null;
    }

    private Out emitMemberGet(Out target, String name, Node posNode, Ctx ctx) {
        TypeRef tt = target.type;
        // A field/param typed with an import prefix (`intl.DateFormat`, `ui.Size`) keeps the
        // prefix in its type name; strip it so member resolution sees the real stub type.
        if (tt != null && tt.name != null && tt.name.indexOf('.') > 0) {
            tt.name = stripImportPrefix(tt.name);
        }
        // A receiver typed by a bounded type parameter (`B extends Base<T>`) has the
        // bound's members; Java erases B to Object, so view it as the bound.
        Out boundView = typeParamBoundView(target, ctx);
        if (boundView != null) {
            target = boundView;
            tt = boundView.type;
        }
        // Cascade suppression: the receiver already fell to `dynamic` from a reported
        // diagnostic upstream; a member read on a dynamic receiver is legal Dart, so
        // don't re-diagnose the same root cause on every link of the chain.
        if (target.fromError && tt.is("dynamic")) {
            return new Out(target.code + "." + name, TypeRef.DYNAMIC, true);
        }
        // Genuine `dynamic` receiver: a member read is legal Dart (dynamic dispatch,
        // resolved at runtime) — e.g. `(dynamic demo) => demo.slug` or a dynamic-typed
        // `platformDispatcher.platformBrightness`. Emit the access with a dynamic result.
        if (tt != null && tt.is("dynamic")) {
            ctx.importClass("dart.runtime.Dyn");
            dynNames.add(name);
            return new Out("Dyn.get(" + target.code + ", \"" + name + "\")", TypeRef.DYNAMIC);
        }
        // record component accessor: `r.$1`, `r.$2`, or `r.namedField`
        if (tt != null && tt.name.startsWith("Rec$")) {
            return new Out(target.code + "." + name + "()", recordComponentType(tt, name));
        }
        // Object protocol: `x.runtimeType` — every Dart object exposes it. Maps to the
        // Java class token, which compares by identity exactly like Dart's Type equality
        // (`other.runtimeType == runtimeType`).
        if (name.equals("runtimeType") && tt != null && !isClassRef(tt)) {
            return new Out(target.code + ".getClass()", new TypeRef("Type"));
        }
        // static access through a class reference
        if (isClassRef(tt)) {
            String cls = tt.arg(0).name;
            List<TypeRef> clsArgs = tt.arg(0).args;
            if (clsArgs.isEmpty() && posNode instanceof PropertyGet
                    && ((PropertyGet) posNode).target instanceof Ident
                    && ((Ident) ((PropertyGet) posNode).target).typeArgs != null) {
                clsArgs = ((Ident) ((PropertyGet) posNode).target).typeArgs;
            }
            Out tear = staticTearOff(cls, clsArgs, name, posNode, ctx);
            if (tear != null) {
                return tear;
            }
            // dart:core Duration's named constants: `zero` is the one non-int member (an
            // actual Duration instance, which the runtime class already declares), and the
            // rest are compile-time int constants. These are emitted as plain long literals
            // rather than routed through dart.core.Duration fields -- that class only models
            // elapsed-time values and does not declare most of them.
            if (cls.equals("Duration")) {
                if (name.equals("zero")) {
                    ctx.importClass("dart.core.Duration");
                    return new Out("Duration.zero", new TypeRef("Duration"));
                }
                Long lit = DURATION_INT_CONSTANTS.get(name);
                if (lit != null) {
                    return new Out(lit + "L", TypeRef.INT);
                }
            }
            if (program.enums.containsKey(cls) || stubs.isStubEnum(cls)) {
                importEnum(cls, ctx);
                if (name.equals("values")) {
                    return enumValues(cls, ctx);
                }
                return new Out(simpleEnumName(cls, ctx) + "." + name, new TypeRef(cls));
            }
            if (stubs.isStubClass(cls)) {
                Ast.MethodDecl g = stubs.findMethod(cls, name, true);
                if (g != null && g.isStatic) {
                    // Icons are methods in the runtime (see Icons.java: a field per icon
                    // kept all 2,176 alive), so an icon read is a call. So is
                    // StackTrace.current: it captures the reader's frames, which a field cannot.
                    boolean call = cls.equals("Icons") || (cls.equals("StackTrace") && name.equals("current"));
                    return new Out(stubSimpleName(cls, ctx) + "." + name
                            + (call ? "()" : ""), g.returnType);
                }
                // Material's style variants (`Icons.home_outlined`, `_rounded`, `_sharp`) are
                // separate glyphs in Flutter's font; the icon font here has one glyph per
                // icon, so a variant shows its base icon (an `_outlined` one the hollow
                // `_outline` glyph where the font has it) rather than failing the build.
                if (cls.equals("Icons")) {
                    for (String suffix : new String[] {"_outlined", "_rounded", "_sharp"}) {
                        if (!name.endsWith(suffix)) {
                            continue;
                        }
                        String base = name.substring(0, name.length() - suffix.length());
                        String[] cands = suffix.equals("_outlined")
                                ? new String[] {base + "_outline", base} : new String[] {base};
                        for (String cand : cands) {
                            Ast.MethodDecl bg = stubs.findMethod(cls, cand, true);
                            if (bg != null && bg.isStatic) {
                                return new Out(stubSimpleName(cls, ctx) + "." + cand + "()", bg.returnType);
                            }
                        }
                    }
                }
                Ast.MethodDecl m = stubs.findMethod(cls, name, false);
                if (m != null && m.isStatic) {
                    // static method tear-off — unsupported
                    diags.error(posNode, "E0130", "Static method tear-offs are not supported yet");
                    return new Out("null", TypeRef.DYNAMIC);
                }
            }
            // The class this library sees by that name: a conditional export can leave two
            // same-named classes in the program (a platform stub and the real one).
            ClassDecl pc = program.resolveClass(cls, ctx.library());
            if (pc == null) {
                pc = program.classes.get(cls);
            }
            if (pc != null) {
                FieldDecl f = pc.field(name);
                if (f != null && f.isStatic) {
                    return new Out(cls + (isLazyStatic(f) || isLateChecked(f, pc) ? ".get$" + name + "()" : "." + name),
                            fieldType(f, ctx));
                }
                // A static getter (`static String get mode => ...`) is a static no-arg method.
                MethodDecl sg = pc.getter(name);
                if (sg != null && sg.isStatic) {
                    return new Out(javaClassName(pc) + "." + javaMethodName(name) + "()",
                            sg.returnType == null ? TypeRef.DYNAMIC : sg.returnType);
                }
            }
            if (cls.equals("DateTime") && pc == null) {
                int dc = java.util.Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday",
                        "saturday", "sunday").indexOf(name);
                int mc = java.util.Arrays.asList("january", "february", "march", "april", "may", "june", "july",
                        "august", "september", "october", "november", "december").indexOf(name);
                if (name.equals("daysPerWeek") || name.equals("monthsPerYear")) {
                    return new Out(name.equals("daysPerWeek") ? "7L" : "12L", TypeRef.INT);
                }
                if (dc >= 0 || mc >= 0) {
                    return new Out((dc >= 0 ? dc + 1 : mc + 1) + "L", TypeRef.INT);
                }
            }
            if (cls.equals("Uri") && name.equals("base") && pc == null) {
                // An application has no working directory to resolve against; the
                // file-system root is the base every relative path then joins.
                ctx.importClass("dart.core.DartUri");
                return new Out("DartUri.parse(\"file:///\")", new TypeRef("Uri"));
            }
            if (CORE_STATIC_RECEIVERS.contains(cls) && pc == null) {
                diags.error(posNode, "E0131", "static member " + name + " of core type " + cls + " is not implemented");
                return new Out("null", TypeRef.DYNAMIC, true);
            }
            diags.error(posNode, "E0131", "Cannot resolve static member '" + name + "' on " + cls);
            return new Out("null", TypeRef.DYNAMIC, true);
        }
        // intrinsics
        if (tt.is("String")) {
            ctx.importClass("dart.core.DString");
            if (name.equals("length")) {
                return new Out("DString.length(" + target.code + ")", TypeRef.INT);
            }
            if (name.equals("isEmpty")) {
                return new Out("DString.isEmpty(" + target.code + ")", TypeRef.BOOL);
            }
            if (name.equals("isNotEmpty")) {
                return new Out("DString.isNotEmpty(" + target.code + ")", TypeRef.BOOL);
            }
        }
        if (tt.is("List") || tt.is("Iterable") || tt.is("Set")) {
            TypeRef elem = tt.arg(0);
            if (name.equals("length")) {
                return new Out(target.code + ".length()", TypeRef.INT);
            }
            if (name.equals("isEmpty")) {
                return new Out(target.code + ".isEmpty()", TypeRef.BOOL);
            }
            if (name.equals("isNotEmpty")) {
                return new Out(target.code + ".isNotEmpty()", TypeRef.BOOL);
            }
            if (name.equals("first")) {
                return new Out(target.code + ".first()", elem);
            }
            if (name.equals("last")) {
                return new Out(target.code + ".last()", elem);
            }
            if (name.equals("single")) {
                // Dart's `single`: the one element, StateError on none or several --
                // exactly singleWhere with an always-true test and no orElse.
                return new Out(target.code + ".singleWhere(e$ -> Boolean.TRUE, null)", elem);
            }
            if (name.equals("reversed")) {
                return new Out(target.code + ".reversedIterable()", TypeRef.of("Iterable", elem));
            }
        }
        if (tt.is("Map")) {
            if (name.equals("length")) {
                return new Out(target.code + ".length()", TypeRef.INT);
            }
            if (name.equals("keys")) {
                return new Out(target.code + ".keys()", TypeRef.of("Iterable", tt.arg(0)));
            }
            if (name.equals("values")) {
                return new Out(target.code + ".valuesIterable()", TypeRef.of("Iterable", tt.arg(1)));
            }
            if (name.equals("entries")) {
                return new Out(target.code + ".entries()",
                        TypeRef.of("Iterable", TypeRef.of("MapEntry", tt.arg(0), tt.arg(1))));
            }
            if (name.equals("isEmpty")) {
                return new Out(target.code + ".isEmpty()", TypeRef.BOOL);
            }
            if (name.equals("isNotEmpty")) {
                return new Out(target.code + ".isNotEmpty()", TypeRef.BOOL);
            }
        }
        if (tt.is("int") || tt.is("double")) {
            if (name.equals("isEven")) {
                return new Out("(" + target.code + " % 2 == 0)", TypeRef.BOOL);
            }
            if (name.equals("isOdd")) {
                return new Out("(" + target.code + " % 2 != 0)", TypeRef.BOOL);
            }
            // num's classification getters. Each evaluates the receiver once; an int
            // widens to double, which answers false/false/true for NaN/infinite/finite.
            if (name.equals("isNaN")) {
                return new Out("Double.isNaN(" + target.code + ")", TypeRef.BOOL);
            }
            if (name.equals("isInfinite")) {
                return new Out("Double.isInfinite(" + target.code + ")", TypeRef.BOOL);
            }
            if (name.equals("isFinite")) {
                return new Out("Double.isFinite(" + target.code + ")", TypeRef.BOOL);
            }
            if (name.equals("isNegative")) {
                // Dart: -0.0 is negative, NaN is not.
                return new Out("dart.core.CoreStatics.isNegative(" + target.code + ")", TypeRef.BOOL);
            }
        }
        // The same implicit members on a stub enum, which is a plain Java enum.
        if (!program.enums.containsKey(tt.name) && stubs.isStubEnum(tt.name)
                && (name.equals("index") || name.equals("name"))) {
            return name.equals("index") ? new Out("(long) " + paren(target.code) + ".ordinal()", TypeRef.INT)
                    : new Out(target.code + ".name()", TypeRef.STRING);
        }
        // implicit instance members on a program enum value: .index (int), .name (String)
        if (program.enums.containsKey(tt.name)) {
            if (name.equals("index")) {
                return new Out(target.code + ".ordinal()", TypeRef.INT);
            }
            if (name.equals("name")) {
                return new Out(target.code + ".name$()", TypeRef.STRING);
            }
            // enhanced-enum getter declared in the enum body
            Ast.EnumDecl ed = programEnum(tt.name, ctx);
            MethodDecl eg = ed.getter(name);
            if (eg != null) {
                return new Out(target.code + "." + name + "()",
                        eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
            }
            // enhanced-enum instance field (`final String label;`): emitEnum gives a public
            // one a get$ accessor and keeps a private one as the bare field.
            for (FieldDecl ef : ed.fields) {
                if (!ef.isStatic && ef.name.equals(name)) {
                    TypeRef eft = ef.type == null || ef.type.is("var") ? TypeRef.DYNAMIC : ef.type;
                    return new Out(ef.name.startsWith("_") ? target.code + "." + name
                            : target.code + ".get$" + name + "()", eft);
                }
            }
        }
        // program class member
        ClassDecl pc = programClass(tt.name, ctx);
        if (pc != null) {
            FieldDecl f = pc.field(name);
            if (f != null) {
                if (target.code.equals("this") && !isLateChecked(f, pc)) {
                    return new Out("this." + name, fieldType(f, ctx));
                }
                return new Out(target.code + ".get$" + name + "()",
                        asSeenFrom(fieldType(f, ctx), f, null, pc, tt));
            }
            MethodDecl g = pc.getter(name);
            if (g != null) {
                return new Out(target.code + "." + name + "()", asSeenFrom(g.returnType, g, null, pc, tt));
            }
            Object mixF = findMixinMember(pc, name, true);
            if (mixF instanceof FieldDecl) {
                return new Out(target.code + ".get$" + name + "()",
                        asSeenFrom(fieldType((FieldDecl) mixF, ctx), mixF, null, pc, tt));
            }
            Object mixG = findMixinMember(pc, name, false);
            if (mixG instanceof MethodDecl && ((MethodDecl) mixG).isGetter) {
                return new Out(target.code + "." + name + "()",
                        asSeenFrom(((MethodDecl) mixG).returnType, mixG, null, pc, tt));
            }
            // field/getter inherited from a program superclass
            FieldDecl inhF = findInheritedField(pc, name);
            if (inhF != null && inheritedFieldVisible(pc, name)) {
                return new Out(target.code + ".get$" + name + "()",
                        asSeenFrom(fieldType(inhF, ctx), inhF, null, pc, tt));
            }
            MethodDecl inhG = findInheritedGetter(pc, name);
            if (inhG != null) {
                return new Out(target.code + "." + name + "()", asSeenFrom(inhG.returnType, inhG, null, pc, tt));
            }
            // method tear-off on an instance: `obj.method` as a function value (method reference)
            MethodDecl tm = pc.method(name);
            if (tm == null) {
                tm = findMethodInHierarchy(pc, name);
            }
            if (tm != null && !tm.isStatic) {
                if (hasOptionalParam(tm)) {
                    // Dart evaluates the receiver once, at the tear-off; the adapter captures it.
                    String rt = ctx.newTemp();
                    ctx.writer().line("final " + javaType(tt, true, ctx) + " " + rt + " = " + target.code + ";");
                    ctx.declare(rt, tt);
                    Out ad = methodTearOffAdapter(tm, rt, name, posNode, ctx);
                    if (ad != null) {
                        return ad;
                    }
                    return new Out(rt + "::" + name, new TypeRef("Function"));
                }
                return new Out(target.code + "::" + name, new TypeRef("Function"));
            }
        }
        // stub class member (walk supers)
        if (stubs.isStubClass(tt.name) || tt.is("State")) {
            Ast.MethodDecl g = stubs.findMethod(tt.name, name, true);
            if (g != null) {
                TypeRef rt = g.returnType;
                // Narrow a generic getter (declared `V get value`) to the concrete
                // type argument the receiver instantiates — e.g.
                // MapEntry<Locale,DisplayOption>.value -> DisplayOption.
                TypeRef sub = stubMemberReturnType(tt, name, true);
                if (sub != null) {
                    rt = sub;
                }
                // State<T>.widget returns the type argument
                if (tt.is("State") && name.equals("widget") && !tt.args.isEmpty()) {
                    rt = tt.arg(0);
                }
                // GlobalKey<T>.currentState returns the type argument (a `!` null-assertion
                // at the use site strips the nullability the Dart getter declares)
                if (tt.is("GlobalKey") && name.equals("currentState") && !tt.args.isEmpty()) {
                    rt = tt.arg(0);
                }
                return new Out(target.code + "." + name + "()", rt);
            }
            // instance-method tear-off: `controller.reverse` / `controller.forward` used as a
            // callback value (e.g. `onTap: controller.reverse`). The member is a method on the
            // stub type, not a field/getter, so emit a Java method reference typed as a Function
            // — mirroring the own-method `this::name` tear-off. The stub already carries the
            // no-arg / optional-arg overloads the target functional interface needs.
            Ast.MethodDecl tearoff = stubs.findMethod(tt.name, name, false);
            if (tearoff != null && !tearoff.isStatic) {
                return new Out("(" + target.code + ")::" + name, new TypeRef("Function"));
            }
        }
        // getter/field inherited by a program class from its stub superclass or a stub mixin
        // (e.g. a RestorableProperty subclass reading `.value`, or a State-mixed accessor).
        if (pc != null) {
            String stubSuper = nearestStubSuper(pc);
            if (stubSuper != null) {
                Ast.MethodDecl sg = stubs.findMethod(stubSuper, name, true);
                if (sg != null) {
                    TypeRef rt = inheritedStubMemberReturnType(pc, name, true);
                    return new Out(target.code + "." + name + "()", rt != null ? rt : sg.returnType);
                }
            }
            for (TypeRef mixRef : pc.mixins) {
                if (stubs.isStubClass(mixRef.name)) {
                    Ast.MethodDecl sg = stubs.findMethod(mixRef.name, name, true);
                    if (sg != null) {
                        return new Out(target.code + "." + name + "()",
                                substMixinMember(sg.returnType, mixRef, pc, tt));
                    }
                }
            }
        }
        // `.mounted` (bool) — on a State receiver, or the BuildContext.mounted guard;
        // neither is part of the minimal State/BuildContext stub surface.
        if ((tt.is("State") || tt.is("BuildContext")) && name.equals("mounted")) {
            return new Out(target.code + ".mounted()", TypeRef.BOOL);
        }
        ClassDecl extCls = findUserExtension(tt.name, name, true, ctx);
        extCls = mostSpecificExtension(extCls, tt, name, true);
        if (extCls != null) {
            MethodDecl eg = extCls.getter(name);
            String self = extensionSelf(target, tt, extCls, ctx);
            return new Out(extCls.name + "." + name + "(" + self + ")",
                    eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
        }
        // An extension METHOD torn off (`f.catchError(completer.completeErrorIfPending)`):
        // the closure calls the extension's static form with the receiver. The receiver
        // expression is re-read per call, which is exact for the final fields and locals
        // this form is torn off from.
        ClassDecl extM = mostSpecificExtension(findUserExtension(tt.name, name, false, ctx), tt, name, false);
        if (extM != null && extM.method(name) != null && extM.method(name).typeParams.isEmpty()
                && extM.method(name).returnType != null) {
            final MethodDecl em = extM.method(name);
            final String self = extensionSelf(target, tt, extM, ctx);
            final String recvTemp = self;
            final String extName = extM.name;
            List<TypeRef> types = new ArrayList<TypeRef>();
            for (Param p : em.params) {
                types.add(paramValueType(p));
            }
            boolean anyOptional = false;
            for (Param p : em.params) {
                anyOptional |= p.named || p.optional || p.defaultValue != null;
            }
            if (!anyOptional) {
                // All required: a plain lambda; each argument is cast to its parameter's
                // erased type, since the target functional interface may take Objects.
                StringBuilder ps = new StringBuilder();
                StringBuilder as = new StringBuilder();
                for (int i = 0; i < em.params.size(); i++) {
                    String a = "$ta" + i;
                    ps.append(i > 0 ? ", " : "").append(a);
                    as.append(", (").append(javaType(types.get(i), true, ctx)).append(") ").append(a);
                }
                return new Out("(" + ps + ") -> " + extName + "." + name + "(" + recvTemp + as + ")",
                        new TypeRef("Function"));
            }
            return tearOffAdapter(em.params, types, em.returnType, names -> {
                StringBuilder sb = new StringBuilder(extName + "." + name + "(" + recvTemp);
                for (String a : names) {
                    sb.append(", ").append(a);
                }
                return sb.append(")").toString();
            }, ctx);
        }
        // extension getter contributed by a stub (`extension AnimationStatusExtensions on
        // AnimationStatus { bool get isAnimating; }`). The stub extension is emitted as a
        // static-method class (its @JavaName); dispatch `status.isAnimating` to
        // `AnimationStatusExtensions.isAnimating(status)`.
        Ast.ClassDecl stubExt = stubs.findExtension(tt.name, name, true);
        if (stubExt != null) {
            Ast.MethodDecl eg = extensionMember(stubExt, name, true);
            String extSimple = stubExtensionSimpleName(stubExt, ctx);
            TypeRef rt = eg == null || eg.returnType == null || eg.returnType.is("var")
                    ? TypeRef.DYNAMIC : eg.returnType;
            return new Out(extSimple + "." + name + "(" + target.code + ")", rt);
        }
        // `e.message` on dart:core's errors that declare one. Each maps to a Java exception
        // built from its message, so getMessage() is that field.
        if (name.equals("message") && CORE_ERRORS.containsKey(tt.name) && !tt.is("Exception")
                && !tt.is("TypeError") && !tt.is("ConcurrentModificationError")) {
            TypeRef str = new TypeRef("String");
            str.nullable = true;
            return new Out(target.code + ".getMessage()", str);
        }
        if (tt.is("Stopwatch")) {
            TypeRef rt = name.equals("isRunning") ? TypeRef.BOOL
                    : name.equals("elapsed") ? new TypeRef("Duration") : TypeRef.INT;
            return new Out(target.code + "." + name + "()", rt);
        }
        // Every Dart object has hashCode; a class whose superclass is a stub (Equatable)
        // reaches here with nothing declaring it.
        if (name.equals("hashCode") && tt != null && !isClassRef(tt)) {
            ctx.importClass("dart.runtime.DartRuntime");
            return new Out("DartRuntime.hashCodeOf(" + target.code + ")", TypeRef.INT);
        }
        // A collection method torn off as a callback -- `stream.listen(seen.add)`,
        // `ids.where(allowed.contains)`. The runtime collections declare these under the
        // same names, so a method reference has the shape the target interface wants.
        if ((tt.is("List") || tt.is("Set")) && COLLECTION_TEAR_OFFS.contains(name)
                || tt.is("Map") && (name.equals("containsKey") || name.equals("containsValue")
                        || name.equals("remove"))) {
            return new Out("(" + target.code + ")::" + name, new TypeRef("Function"));
        }
        TypeRef coreUp = programClass(tt.name, ctx) != null
                ? coreAncestor(programClass(tt.name, ctx), new HashSet<ClassDecl>()) : null;
        if (coreUp != null) {
            // `abstract class B implements A` with `abstract class A implements List<E>`: a
            // member only the core type declares (`length`) is reached through the
            // interface chain, however many program levels sit between.
            return emitMemberGet(new Out(target.code, coreUp), name, posNode, ctx);
        }
        diags.error(posNode, "E0132", "Cannot resolve member '" + name + "' on type " + tt
                + ". Confirm the file passes `dart analyze`, or the API is not yet implemented in the Codename One runtime.");
        return new Out(target.code + "." + name, TypeRef.DYNAMIC, true);
    }

    private static final Set<String> COLLECTION_TEAR_OFFS = new HashSet<String>(
            java.util.Arrays.asList("add", "addAll", "remove", "contains"));

    /**
     * For a Dart {@code List<int>}/{@code List<double>} with a non-nullable primitive element, the
     * primitive-list kind ("Long"/"Double") whose getLong/setLong/addLong methods avoid boxing;
     * null for any other list (which uses the boxed {@code DartList}).
     */
    private static String primitiveListKind(TypeRef tt) {
        if (tt == null || !tt.is("List") || tt.args.isEmpty()) {
            return null;
        }
        TypeRef e = tt.arg(0);
        if (e == null || e.nullable) {
            return null;
        }
        if (e.is("int")) {
            return "Long";
        }
        if (e.is("double")) {
            return "Double";
        }
        return null;
    }

    /**
     * True only for a non-nullable {@code Map<int, int>} &mdash; targets the
     * primitive {@code long}&rarr;{@code long} {@link dart.core.DartLongMap} so
     * puts/gets avoid Long boxing. Other key/value combinations keep the boxed
     * {@code DartMap}.
     */
    /** A side-effect-free constant literal — safe to evaluate eagerly (e.g. for a ?? default). */
    private static boolean isPureLiteral(Expr e) {
        return e instanceof IntLit || e instanceof DoubleLit || e instanceof BoolLit
                || e instanceof NullLit || e instanceof StringLit;
    }

    private static boolean isPrimitiveLongMap(TypeRef t) {
        if (t == null || !t.is("Map") || t.args.size() != 2) {
            return false;
        }
        TypeRef k = t.arg(0);
        TypeRef v = t.arg(1);
        return k != null && v != null && !k.nullable && !v.nullable && k.is("int") && v.is("int");
    }

    private Out emitIndexGet(IndexGet ig, Ctx ctx) {
        if (chainHasNullAware(ig.target)) {
            // `a?.b[i]`: a null `a` shorts the index too, which is not evaluated, and
            // anything after it. The target was materialized to `(a == null ? null :
            // a.b)` and indexed, which threw. Index the guarded read inside the short
            // instead, and keep what the index lifts behind the guard.
            Out raw = emitExprRaw(ig.target, null, ctx);
            if (raw.shortGuard != null) {
                ctx.pushWriter(ctx.currentIndent() + 1);
                Out r = emitIndexGetOn(new Out(raw.code, copyNonNull(raw.type), raw.fromError), ig, ctx);
                String lifted = ctx.popWriter();
                if (!lifted.isEmpty()) {
                    Out g = guardLifted(raw.shortGuard, lifted, r, ctx);
                    if (g != null) {
                        return g;
                    }
                }
                return new Out(r.code, boxType(r.type), r.fromError, raw.shortGuard);
            }
            return emitIndexGetOn(raw, ig, ctx);
        }
        if (ig.target instanceof ThisExpr && ctx.currentClass != null && ctx.extensionSelfType == null
                && ctx.currentClass.method("$index") == null) {
            // `this[i]` in a mixin on a core List (directly or through a program
            // interface): the Java type is not the list, the applier is.
            TypeRef coreOn = coreCollectionOn(ctx.currentClass);
            if (coreOn != null && coreOn.is("List")) {
                return emitIndexGetOn(coreSelf(coreOn, ctx), ig, ctx);
            }
        }
        return emitIndexGetOn(emitExpr(ig.target, null, ctx), ig, ctx);
    }

    /**
     * A receiver typed as a bounded type variable (`L extends List<E>`, `items[i]`): its
     * operators are the bound's, and the receiver is cast to the bound so the Java member
     * resolves even where the Java type variable carries no bound. Otherwise unchanged.
     */
    private Out throughTypeVarBound(Out target, Ctx ctx) {
        TypeRef tt = target.type;
        if (tt == null || tt.name == null || !tt.args.isEmpty() || tt.funcReturn != null) {
            return target;
        }
        TypeRef bound = ctx.methodTypeParamBounds != null ? ctx.methodTypeParamBounds.get(tt.name) : null;
        ClassDecl cc = ctx.currentClass;
        if (bound == null && cc != null && cc.typeParams.contains(tt.name)) {
            bound = cc.typeParamBounds.get(tt.name);
        }
        if (bound == null || bound.name == null || bound.name.equals(tt.name) || bound.funcReturn != null) {
            return target;
        }
        if (cc != null && cc.typeParams.contains(bound.name)) {
            return target;
        }
        return new Out("((" + javaType(bound, true, ctx) + ") " + target.code + ")", bound, target.fromError);
    }

    private Out emitIndexGetOn(Out target, IndexGet ig, Ctx ctx) {
        target = throughTypeVarBound(target, ctx);
        Out idx = emitExpr(ig.index, null, ctx);
        TypeRef tt = target.type;
        ClassDecl opClass = program.classes.get(tt.name);
        if (opClass != null) {
            MethodDecl om = findMethodInHierarchy(opClass, "$index");
            if (om != null) {
                TypeRef rt = om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType;
                if (opClass.methods.contains(om) && !opClass.typeParams.isEmpty()
                        && tt.args.size() == opClass.typeParams.size()) {
                    // `QueueList<Result<T>>[i]` reads a Result<T>, not the declared E
                    rt = substituteTypeParam(rt, opClass.typeParams, tt.args);
                }
                return new Out(target.code + ".$index(" + idx.code + ")", rt);
            }
        }
        if (tt.is("dynamic") && !target.fromError) {
            ctx.importClass("dart.runtime.Dyn");
            dynNames.add("[]");
            return new Out("Dyn.index(" + target.code + ", " + boxIfPrimitive(idx, ctx) + ")", TypeRef.DYNAMIC);
        }
        if (tt.is("String")) {
            ctx.importClass("dart.core.DString");
            return new Out("DString.idx(" + target.code + ", " + idx.code + ")", TypeRef.STRING);
        }
        if (tt.is("Map")) {
            if (isPrimitiveLongMap(tt)) {
                // Bare m[k] read: returns a nullable boxed Long (null when absent).
                // The common m[k] ?? default pattern is unboxed via the ?? peephole below.
                return new Out(target.code + ".idxLong(" + idx.code + ")", boxType(tt.arg(1)));
            }
            return new Out(target.code + ".idx(" + boxIfPrimitive(idx, ctx) + ")", boxType(tt.arg(1)));
        }
        String pk = primitiveListKind(tt);
        if (pk != null) {
            return new Out(target.code + ".get" + pk + "(" + idx.code + ")", tt.arg(0));
        }
        if (tt.args.isEmpty()) {
            // A non-generic stub type declaring `operator []` (Match, RegExpMatch,
            // HttpHeaders): the runtime class answers it as idx, and the stub's
            // declared return type is the value's type -- tt.arg(0) would be dynamic,
            // which made `m[1] ?? ''` an Object where a String was needed.
            TypeRef sr = stubMemberReturnType(tt, "$index", false);
            if (sr != null && !sr.is("var")) {
                return new Out(target.code + ".idx(" + idx.code + ")", sr);
            }
        }
        return new Out(target.code + ".idx(" + idx.code + ")", tt.arg(0), target.fromError && tt.is("dynamic"));
    }

    /** The element type of an indexable receiver (Map value / list-or-collection element). */
    private TypeRef indexElementType(TypeRef t) {
        if (t == null) {
            return TypeRef.DYNAMIC;
        }
        if (t.is("Map")) {
            return t.args.size() >= 2 ? t.arg(1) : TypeRef.DYNAMIC;
        }
        return t.args.isEmpty() ? TypeRef.DYNAMIC : t.arg(0);
    }

    /**
     * The value code for a compound index-assignment {@code x[i] op= v}: {@code read op v},
     * coerced to the element type. {@code read} is the already-emitted element read.
     */
    private String compoundValue(String readCode, TypeRef vt, Assign a, Ctx ctx) {
        Out rhs = emitExpr(a.rhs, vt, ctx);
        String baseOp = a.op.substring(0, a.op.length() - 1);
        String expr;
        if (isDynamic(vt) || isDynamic(rhs.type)) {
            ctx.importClass("dart.runtime.DartRuntime");
            // Dart downcasts the result to the slot's type (`int x; x += d`).
            return coerce(new Out("DartRuntime.dynBinary(\"" + baseOp + "\", " + readCode + ", "
                    + rhs.code + ")", TypeRef.DYNAMIC), vt, ctx);
        }
        if (vt != null && vt.is("String") && baseOp.equals("*")) {
            ctx.importClass("dart.core.DString");
            return "DString.repeat(" + readCode + ", " + rhs.code + ")";
        }
        if (vt != null && vt.is("num") && !baseOp.equals("/")) {
            // A num slot keeps an int an int, as the binary operator does.
            ctx.importClass("dart.runtime.DartRuntime");
            return "((Number) DartRuntime.dynBinary(\"" + baseOp + "\", " + readCode + ", " + rhs.code + "))";
        }
        Out opCall = operatorMethodValue(readCode, vt, baseOp, rhs);
        if (opCall != null) {
            return coerce(opCall, vt, ctx);
        }
        if (baseOp.equals("~/") || baseOp.equals("%")) {
            ctx.importClass("dart.runtime.DartRuntime");
            String fn = baseOp.equals("~/") ? "tdiv" : "mod";
            expr = "DartRuntime." + fn + "(" + readCode + ", " + rhs.code + ")";
        } else if ((baseOp.equals("<<") || baseOp.equals(">>") || baseOp.equals(">>>"))
                && !(a.rhs instanceof IntLit && ((IntLit) a.rhs).value >= 0 && ((IntLit) a.rhs).value < 64)) {
            // `x <<= n` with Dart's shift-count rules, as the binary operator has.
            ctx.importClass("dart.runtime.DartRuntime");
            String fn = baseOp.equals("<<") ? "shl" : baseOp.equals(">>") ? "shr" : "ushr";
            expr = "DartRuntime." + fn + "(" + readCode + ", " + rhs.code + ")";
        } else {
            expr = "(" + readCode + " " + baseOp + " " + rhs.code + ")";
        }
        return coerce(new Out(expr, vt), vt, ctx);
    }

    /** {@code x[i] ??= v} or {@code o.p ??= v}: a null-aware assignment whose target is not a plain variable. */
    private static boolean isNullAwareMemberAssign(Assign a) {
        return a.op.equals("??=") && (a.lhs instanceof IndexGet || a.lhs instanceof PropertyGet);
    }

    /**
     * The target of {@code x[i] ??= v} / {@code o.p ??= v} with its receiver and index
     * evaluated once, into temps, so the read and the write below can both name them.
     *
     * <p>Rewriting over temps is what lets the write go through the ordinary {@code =}
     * lowering -- idxSet, $indexSet, primitive lists, stub and app setters, accessors --
     * instead of emitting the read as an assignment target. The generic expansion this
     * replaces produced {@code map.idx(key) = value}, which javac rejects, and evaluated
     * the receiver and index up to three times.</p>
     */
    private Expr hoistNullAwareTarget(Assign a, Ctx ctx, List<String> inline) {
        if (a.lhs instanceof IndexGet) {
            IndexGet ig = (IndexGet) a.lhs;
            IndexGet copy = new IndexGet().at(a.file, a.line, a.col);
            copy.target = hoistOnce(ig.target, ctx, inline);
            copy.index = hoistOnce(ig.index, ctx, inline);
            return copy;
        }
        PropertyGet pg = (PropertyGet) a.lhs;
        PropertyGet copy = new PropertyGet().at(a.file, a.line, a.col);
        copy.target = hoistOnce(pg.target, ctx, inline);
        copy.name = pg.name;
        copy.nullAware = pg.nullAware;
        return copy;
    }

    /**
     * {@code e} evaluated into a fresh local, unless it is already {@code this} or a local.
     * With {@code inline} null the evaluation is a lifted statement, which is right only
     * where the enclosing statement runs unconditionally; otherwise only the declaration
     * is lifted and the assignment is appended to {@code inline}, for the caller to
     * sequence where the expression really is evaluated.
     */
    private Expr hoistOnce(Expr e, Ctx ctx, List<String> inline) {
        if (e instanceof ThisExpr || (e instanceof Ident && ctx.lookup(((Ident) e).name) != null)) {
            return e;
        }
        Out o = emitExpr(e, null, ctx);
        if (o.type != null && isClassRef(o.type)) {
            // `Config.label ??= v`: the receiver is a class, which has nothing to
            // evaluate and cannot be held in a variable.
            return e;
        }
        TypeRef t = o.type == null ? TypeRef.DYNAMIC : o.type;
        String tmp = ctx.newTemp();
        if (inline == null) {
            ctx.writer().line(javaType(t, false, ctx) + " " + tmp + " = " + o.code + ";");
        } else {
            ctx.writer().line(javaType(t, false, ctx) + " " + tmp + ";");
            inline.add(tmp + " = " + o.code);
        }
        ctx.declare(tmp, t);
        Ident id = new Ident().at(e.file, e.line, e.col);
        id.name = tmp;
        return id;
    }

    /**
     * The receiver of a read-modify-write through a getter/setter pair, which names it
     * twice: kept as written when naming it again has no effect (a local, {@code this}, a
     * field chain), otherwise evaluated once into a local ahead of the statement, as the
     * compound index assignment does with its receiver.
     */
    private String receiverOnce(Out tgt, Ctx ctx) {
        if (isRepeatable(tgt)) {
            return tgt.code;
        }
        String tmp = ctx.newTemp();
        TypeRef t = tgt.type == null ? TypeRef.DYNAMIC : tgt.type;
        ctx.writer().line(javaType(t, false, ctx) + " " + tmp + " = " + tgt.code + ";");
        return tmp;
    }

    /** A name that a bare assignment writes through a library-level {@code set name(v)}. */
    private boolean isTopLevelSetter(String nm, Ctx ctx) {
        return ctx.lookup(nm) == null
                && (ctx.currentClass == null || ctx.currentClass.field(nm) == null)
                && program.topLevelSetters.containsKey(nm);
    }

    /** A receiver naming it again has no effect on: a local, {@code this}, a field chain, a class. */
    private boolean isRepeatable(Out tgt) {
        return tgt.code.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
                || (tgt.type != null && isClassRef(tgt.type));
    }

    private static Assign plainAssign(Assign a, Expr lhs) {
        Assign plain = new Assign().at(a.file, a.line, a.col);
        plain.lhs = lhs;
        plain.op = "=";
        plain.rhs = a.rhs;
        return plain;
    }

    /**
     * {@code x[i] ??= v;} as a statement -- the common shape -- lowers to
     * {@code if (x[i] == null) { x[i] = v; }}: the value is evaluated only when the slot
     * is null, and the write is whatever {@code =} would emit, void setters and
     * {@code operator []=} included.
     */
    private void emitNullAwareAssignStatement(Assign a, Ctx ctx) {
        // A statement runs unconditionally, so lifting its receiver and index ahead of
        // it evaluates them exactly when Dart would.
        Expr lhs = hoistNullAwareTarget(a, ctx, null);
        Out read = emitExpr(lhs, null, ctx);
        Ctx.Writer w = ctx.writer();
        w.line("if (" + read.code + " == null) {");
        ctx.indent(1);
        String write = statementize(emitStatementExpr(plainAssign(a, lhs), ctx).code);
        if (!write.isEmpty()) {
            ctx.writer().line(write + ";");
        }
        ctx.indent(-1);
        ctx.writer().line("}");
    }

    /**
     * {@code x[i] ??= v} used as a value: the current value if non-null, otherwise the
     * written one. The write has to be a Java expression that yields the value, which
     * DartList/DartMap's idxSet and a plain field assignment are; an {@code operator []=}
     * or a setter is void in Java and is reported rather than emitted as invalid code.
     */
    private Out emitNullAwareAssignValue(Assign a, Ctx ctx) {
        // A value can sit where it is evaluated lazily -- a loop condition, a
        // short-circuited operand -- so nothing is evaluated ahead of it: the temps are
        // declared, and assigned in order inside the expression itself.
        List<String> inline = new ArrayList<String>();
        Expr lhs = hoistNullAwareTarget(a, ctx, inline);
        Out read = emitExpr(lhs, null, ctx);
        TypeRef rt = read.type == null ? TypeRef.DYNAMIC : read.type;
        String cur = ctx.newTemp();
        ctx.writer().line(javaType(rt, true, ctx) + " " + cur + ";");
        inline.add(cur + " = " + read.code);
        ctx.pushWriter(ctx.currentIndent() + 1);
        Out write = emitAssign(plainAssign(a, lhs), ctx);
        String writeLifted = ctx.popWriter();
        boolean yieldsValue = write.code.startsWith(read.code + " = ");
        if (lhs instanceof IndexGet) {
            String receiver = emitExpr(((IndexGet) lhs).target, null, ctx).code;
            yieldsValue = write.code.startsWith(receiver + ".idxSet(");
        }
        // A void setter or operator []= written in its value form lifts the write
        // and yields a temp.
        yieldsValue |= !writeLifted.isEmpty();
        if (!yieldsValue) {
            diags.error(a, "E0141", "'??=' on an operator []= or a setter is supported as a statement,"
                    + " not yet as a value. Assign first, then read the target.");
        }
        ctx.importClass("dart.runtime.DartRuntime");
        String chain = "true";
        for (int k = inline.size() - 1; k >= 0; k--) {
            chain = "DartRuntime.seq(" + inline.get(k) + ", " + chain + ")";
        }
        if (!writeLifted.isEmpty()) {
            // The written value lifted statements (`m[k] ??= [...load()]`), which must
            // run only when the slot is null: an if, which the enclosing operand's own
            // handling (emitLazyOperand) places wherever this value is evaluated.
            Ctx.Writer w = ctx.writer();
            w.line("if (!(" + chain + " && " + cur + " != null)) {");
            w.raw(writeLifted);
            ctx.indent(1);
            w.line(cur + " = " + write.code + ";");
            ctx.indent(-1);
            w.line("}");
            return new Out(cur, rt);
        }
        return new Out("(" + chain + " && " + cur + " != null ? " + cur + " : (" + write.code + "))", rt);
    }

    /**
     * {@code d.x = v}, {@code d[k] = v} and their compound forms on a {@code dynamic}
     * receiver, through dart.runtime.Dyn; null when the receiver is statically typed.
     * A compound write pins the receiver and key in temps so each is evaluated once.
     */
    private Out emitDynamicMemberAssign(Assign a, Ctx ctx) {
        return emitDynamicMemberAssign(a, null, ctx);
    }

    /**
     * As above; {@code pre} is the receiver when the caller has already emitted it (a
     * receiver whose type only emission reveals as dynamic, such as a call).
     */
    private Out emitDynamicMemberAssign(Assign a, Out pre, Ctx ctx) {
        Expr recv = a.lhs instanceof PropertyGet ? ((PropertyGet) a.lhs).target
                : a.lhs instanceof IndexGet ? ((IndexGet) a.lhs).target : null;
        if (recv == null || a.op.equals("??=")) {
            return null;
        }
        Out t = pre;
        if (t == null) {
            TypeRef rt = inferExprTypeQuiet(recv, ctx);
            if (rt == null || !rt.is("dynamic")) {
                return null;
            }
            t = emitExpr(recv, null, ctx);
        }
        if (!t.type.is("dynamic") || t.fromError) {
            return null;
        }
        ctx.importClass("dart.runtime.Dyn");
        String key = a.lhs instanceof PropertyGet ? "\"" + ((PropertyGet) a.lhs).name + "\""
                : boxIfPrimitive(emitExpr(((IndexGet) a.lhs).index, null, ctx), ctx);
        boolean prop = a.lhs instanceof PropertyGet;
        String recvCode = t.code;
        if (!a.op.equals("=")) {
            // `d[i++] += 1` / `f().x += 1`: the receiver and the key are read and then
            // written, so each is pinned in a temp and evaluated once, ahead of the value.
            recvCode = ctx.newTemp();
            ctx.writer().line("Object " + recvCode + " = " + t.code + ";");
            if (!prop) {
                String kTmp = ctx.newTemp();
                ctx.writer().line("Object " + kTmp + " = " + key + ";");
                key = kTmp;
            }
        }
        String value = boxIfPrimitive(emitExpr(a.rhs, null, ctx), ctx);
        if (prop) {
            dynNames.add(((PropertyGet) a.lhs).name);
        } else {
            dynNames.add("[]=");
            dynNames.add("[]");
        }
        if (!a.op.equals("=")) {
            ctx.importClass("dart.runtime.DartRuntime");
            String cur = prop ? "Dyn.get(" + recvCode + ", " + key + ")" : "Dyn.index(" + recvCode + ", " + key + ")";
            if (postfixValue(a, ctx)) {
                // `d[i]++` as a value: the old value.
                String oldTmp = ctx.newTemp();
                ctx.writer().line("Object " + oldTmp + " = " + cur + ";");
                cur = oldTmp;
                value = "DartRuntime.dynBinary(\"" + a.op.substring(0, a.op.length() - 1) + "\", " + cur + ", " + value + ")";
                ctx.writer().line((prop ? "Dyn.set(" : "Dyn.indexSet(") + recvCode + ", " + key + ", " + value + ");");
                return new Out(oldTmp, TypeRef.DYNAMIC);
            }
            value = "DartRuntime.dynBinary(\"" + a.op.substring(0, a.op.length() - 1) + "\", " + cur + ", " + value + ")";
        }
        return new Out((prop ? "Dyn.set(" : "Dyn.indexSet(") + recvCode + ", " + key + ", " + value + ")",
                TypeRef.DYNAMIC);
    }

    /** {@code a[i]++} used as a value: it yields the element read before the write. */
    private static boolean postfixValue(Assign a, Ctx ctx) {
        return ctx.postfixAssign == a && ctx.statementExpr != a;
    }

    /**
     * The value form of a postfix {@code a[i]++}: the old element is read once into a
     * temp, the incremented value written through {@code write}, and the temp returned.
     * The receiver and index are already temps (the compound path pins them).
     */
    private Out postfixIndex(String read, TypeRef t, Assign a, Ctx ctx,
            java.util.function.Function<String, String> write) {
        String jt = declarableType(t, false, ctx);
        String old = ctx.newTemp();
        ctx.writer().line((jt != null ? jt : "var") + " " + old + " = " + read + ";");
        ctx.writer().line(write.apply(compoundValue(old, t, a, ctx)) + ";");
        return new Out(old, t);
    }

    /**
     * The value of {@code slot ??= value}, coerced to the slot. A function value whose
     * Java parameterization differs from the slot's (a {@code Func2<Object, Object, Long>}
     * tear-off of {@code int f(Object?, Object?)} stored into an {@code int Function(E, E)?}
     * slot -- valid Dart, since parameters are contravariant) goes through the raw
     * interface, which Java converts unchecked; erasure makes them the same object.
     */
    private String nullAwareFuncValue(Out rhs, TypeRef slot, Ctx ctx) {
        String v = coerce(rhs, slot, ctx);
        if (slot == null || slot.funcReturn == null || rhs.type == null || rhs.type.funcReturn == null) {
            return v;
        }
        String jt = javaType(slot, false, ctx);
        int lt = jt.indexOf('<');
        if (lt <= 0 || jt.equals(javaType(rhs.type, false, ctx))) {
            return v;
        }
        return "(" + jt.substring(0, lt) + ") " + paren(v);
    }

    private Out emitAssign(Assign a, Ctx ctx) {
        Out shorted = emitShortedAssign(a, ctx);
        if (shorted != null) {
            return shorted;
        }
        if (isNullAwareMemberAssign(a)) {
            if (ctx.statementExpr == a) {
                emitNullAwareAssignStatement(a, ctx);
                return new Out("", TypeRef.VOID);
            }
            return emitNullAwareAssignValue(a, ctx);
        }
        Out superSet = onMixinSuperSet(a, ctx);
        if (superSet != null) {
            return superSet;
        }
        Out dynWrite = emitDynamicMemberAssign(a, ctx);
        if (dynWrite != null) {
            return dynWrite;
        }
        // ??= on a variable: a local or field is a valid Java assignment target.
        if (a.op.equals("??=")) {
            Out lhs = emitExpr(a.lhs, null, ctx);
            Lazy rl = emitLazyOperand(a.rhs, lhs.type, ctx, 1);
            Out rhs = rl.out;
            boolean accessor = lhs.code.endsWith("()") && lhs.code.contains(".get$");
            if (rl.lifts()) {
                // The value lifted statements (`cache ??= [...load()]`); they run only
                // when the variable is null, so the write becomes an if.
                String write;
                if (accessor) {
                    String base = lhs.code.substring(0, lhs.code.lastIndexOf(".get$"));
                    String prop = lhs.code.substring(lhs.code.lastIndexOf(".get$") + 5, lhs.code.length() - 2);
                    write = base + ".set$" + prop + "(" + coerce(rhs, lhs.type, ctx) + ")";
                } else {
                    write = lhs.code + " = " + coerce(rhs, lhs.type, ctx);
                }
                Ctx.Writer w = ctx.writer();
                w.line("if (" + lhs.code + " == null) {");
                w.raw(rl.lifted);
                ctx.indent(1);
                w.line(write + ";");
                ctx.indent(-1);
                w.line("}");
                return new Out("(" + lhs.code + ")", lhs.type);
            }
            if (accessor) {
                // A lazily initialised static or top-level is an accessor pair; its
                // setter answers the stored value.
                String base = lhs.code.substring(0, lhs.code.lastIndexOf(".get$"));
                String prop = lhs.code.substring(lhs.code.lastIndexOf(".get$") + 5, lhs.code.length() - 2);
                return new Out("(" + lhs.code + " == null ? " + base + ".set$" + prop + "("
                        + coerce(rhs, lhs.type, ctx) + ") : " + lhs.code + ")", lhs.type);
            }
            // Coerced like a plain write: `compare ??= defaultCompare` stores a
            // Func2<Object, Object, Long> tear-off into a Func2<E, E, Long> slot.
            return new Out("(" + lhs.code + " == null ? (" + lhs.code + " = " + nullAwareFuncValue(rhs, lhs.type, ctx)
                    + ") : " + lhs.code + ")", lhs.type);
        }
        if (a.lhs instanceof IndexGet) {
            IndexGet ig = (IndexGet) a.lhs;
            Out target = throughTypeVarBound(emitExpr(ig.target, null, ctx), ctx);
            if (target.type.is("dynamic") && !target.fromError) {
                // `f()[k] += 1` where f returns dynamic: inference did not see it, the
                // emitted receiver does. Index through Dyn, not a typed `.idx`.
                Out dyn = emitDynamicMemberAssign(a, target, ctx);
                if (dyn != null) {
                    return dyn;
                }
            }
            Out idx = emitExpr(ig.index, null, ctx);
            boolean compound = !a.op.equals("=");
            ClassDecl idxOpClass = program.classes.get(target.type.name);
            boolean voidIndexSet = idxOpClass != null && findMethodInHierarchy(idxOpClass, "$indexSet") != null
                    && ctx.statementExpr != a;
            if (compound || voidIndexSet) {
                // x[i] op= v -> x[i] = x[i] op v, evaluating x and i exactly once.
                String tTmp = ctx.newTemp();
                ctx.writer().line(javaType(target.type, false, ctx) + " " + tTmp + " = " + target.code + ";");
                String iTmp = ctx.newTemp();
                ctx.writer().line(javaType(idx.type, false, ctx) + " " + iTmp + " = " + idx.code + ";");
                target = new Out(tTmp, target.type);
                idx = new Out(iTmp, idx.type);
            }
            ClassDecl opClass = program.classes.get(target.type.name);
            if (opClass != null && findMethodInHierarchy(opClass, "$indexSet") != null) {
                TypeRef ivt = compound ? indexElementType(target.type) : null;
                MethodDecl readOp = findMethodInHierarchy(opClass, "$index");
                if (compound && readOp != null && readOp.returnType != null && !readOp.returnType.is("var")) {
                    // A class's own `operator []` says what it reads: the receiver type
                    // carries no type argument to take it from, and `grid[i] += 1` became
                    // dynamic arithmetic passed to an int parameter.
                    ivt = readOp.returnType;
                }
                if (postfixValue(a, ctx)) {
                    final String tc = target.code, ic = idx.code;
                    return postfixIndex(tc + ".$index(" + ic + ")", ivt, a, ctx,
                            v -> tc + ".$indexSet(" + ic + ", " + v + ")");
                }
                Out rhsOut = compound ? null : emitExpr(a.rhs, null, ctx);
                String rhsCode = compound
                        ? compoundValue(target.code + ".$index(" + idx.code + ")", ivt, a, ctx)
                        : rhsOut.code;
                TypeRef rt = ivt != null ? ivt : compound ? TypeRef.DYNAMIC : rhsOut.type;
                if (voidIndexSet) {
                    // `operator []=` is void in Java; as a value, the value is a temp,
                    // declared as the operator's value parameter.
                    MethodDecl ism = findMethodInHierarchy(opClass, "$indexSet");
                    if (ism.params.size() == 2 && ism.params.get(1).type != null
                            && !isDynamicType(ism.params.get(1).type)) {
                        rt = ism.params.get(1).type;
                    }
                    String jt = declarableType(rt, false, ctx);
                    String v = ctx.newTemp();
                    ctx.writer().line((jt != null ? jt : "var") + " " + v + " = " + rhsCode + ";");
                    ctx.writer().line(target.code + ".$indexSet(" + idx.code + ", " + v + ");");
                    return new Out(v, rt);
                }
                return new Out(target.code + ".$indexSet(" + idx.code + ", " + rhsCode + ")",
                        ivt != null ? ivt : TypeRef.DYNAMIC);
            }
            TypeRef vt = target.type.is("Map") ? target.type.arg(1) : target.type.arg(0);
            String pk = primitiveListKind(target.type);
            if (pk != null) {
                if (postfixValue(a, ctx)) {
                    final String tc = target.code, ic = idx.code;
                    return postfixIndex(tc + ".get" + pk + "(" + ic + ")", vt, a, ctx,
                            v -> tc + ".set" + pk + "(" + ic + ", " + v + ")");
                }
                String rhsCode = compound
                        ? compoundValue(target.code + ".get" + pk + "(" + idx.code + ")", vt, a, ctx)
                        : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
                return new Out(target.code + ".set" + pk + "(" + idx.code + ", " + rhsCode + ")", vt);
            }
            // Primitive long->long map: m[k] = v -> putLong(k, v), no boxing.
            if (isPrimitiveLongMap(target.type)) {
                // The key is coerced like the value: a dynamic one (an untyped forEach
                // parameter, say) did not compile against putLong(long, long).
                String key = coerce(idx, TypeRef.INT, ctx);
                if (postfixValue(a, ctx)) {
                    final String tc = target.code;
                    return postfixIndex(tc + ".idxLong(" + key + ")", vt, a, ctx,
                            v -> tc + ".putLong(" + key + ", " + v + ")");
                }
                String rhsCode = compound
                        ? compoundValue(target.code + ".idxLong(" + key + ")", vt, a, ctx)
                        : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
                return new Out(target.code + ".putLong(" + key + ", " + rhsCode + ")", vt);
            }
            String key = target.type.is("Map") ? boxIfPrimitive(idx, ctx) : idx.code;
            if (postfixValue(a, ctx)) {
                final String tc = target.code;
                return postfixIndex(tc + ".idx(" + key + ")", vt, a, ctx,
                        v -> tc + ".idxSet(" + key + ", " + v + ")");
            }
            String rhsCode = compound
                    ? compoundValue(target.code + ".idx(" + key + ")", vt, a, ctx)
                    : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
            return new Out(target.code + ".idxSet(" + key + ", " + rhsCode + ")", vt);
        }
        // assignment to a stub property that declares a Dart setter:
        // `x.value = v` -> the overloaded setter method `x.value(v)`. Compound forms
        // (`x.value -= d`) read through the getter: `x.value(x.value() - d)`.
        Out lhsOnce = null;
        if (a.lhs instanceof PropertyGet) {
            PropertyGet pg = (PropertyGet) a.lhs;
            Out tgt = emitExpr(pg.target, null, ctx);
            if (tgt.type != null && (stubs.isStubClass(tgt.type.name) || tgt.type.is("State"))) {
                Ast.MethodDecl setter = stubs.findSetter(tgt.type.name, pg.name);
                if (setter != null) {
                    TypeRef pt = setter.params.isEmpty() ? TypeRef.DYNAMIC : setter.params.get(0).type;
                    // Used as a value the receiver is named twice (temp, then call), so
                    // it is evaluated once first, ahead of the value, as Dart orders it.
                    String recv = a.op.equals("=") && ctx.statementExpr == a ? tgt.code : receiverOnce(tgt, ctx);
                    return voidSetterWrite(a, recv + "." + pg.name,
                            a.op.equals("=") ? null : recv + "." + pg.name + "()", pt, ctx);
                }
            }
            // App class (or its supers) declaring `set prop(v)` — emitted as the overloaded
            // instance method `prop(v)`, so `x.prop = v` becomes `x.prop(v)` (never `x.prop() = v`).
            // A compound `x.prop += v` reads through the getter, `x.prop(x.prop() + v)`, as the
            // stub branch above does; it fell through to `x.prop() += v`, which is not Java.
            // (`??=` never gets here: it is rewritten to a plain `=` under a null test.)
            if (tgt.type != null) {
                ClassDecl tc = program.resolveClass(tgt.type.name, ctx.library());
                Ast.MethodDecl setter = findAppSetter(tc, pg.name);
                if (setter != null) {
                    TypeRef pt = setter.params.isEmpty() ? TypeRef.DYNAMIC : setter.params.get(0).type;
                    String recv = a.op.equals("=") && ctx.statementExpr == a ? tgt.code : receiverOnce(tgt, ctx);
                    return voidSetterWrite(a, recv + "." + pg.name,
                            a.op.equals("=") ? null : recv + "." + pg.name + "()", pt, ctx);
                }
            }
            if ((ctx.statementExpr != a || !a.op.equals("=")) && !isRepeatable(tgt)) {
                // A field reached through accessors names the receiver in the read and
                // in the write (`make().v += 3` called make() twice), and a value form
                // evaluates the value before the call: evaluate the receiver once, first.
                String recv = receiverOnce(tgt, ctx);
                lhsOnce = emitMemberGet(new Out(recv, tgt.type, tgt.fromError), pg.name, pg, ctx);
            }
        }
        // assignment to a top-level setter: `x = v` where `set x(v)` is declared at
        // library scope -> the static setter method `OwnerLib.x(v)`.
        if (a.lhs instanceof Ident && !a.op.equals("??=") && isTopLevelSetter(((Ident) a.lhs).name, ctx)) {
            String nm = ((Ident) a.lhs).name;
            {
                Library owner = program.topLevelSetters.get(nm);
                String call = Program.libClassName(owner.fileName) + "." + nm;
                if (!a.op.equals("=")) {
                    // `top += 2` reads through the top-level getter and writes through the
                    // setter; it emitted `Lib.top() += 2`, which is not Java.
                    Out read = emitExpr(a.lhs, null, ctx);
                    return voidSetterWrite(a, call, read.code, read.type, ctx);
                }
                Out rhs = emitExpr(a.rhs, null, ctx);
                if (ctx.statementExpr == a) {
                    return new Out(call + "(" + rhs.code + ")", rhs.type);
                }
                // The setter is void in Java: as a value, the value goes into a temp.
                String jt = declarableType(rhs.type, false, ctx);
                String v = ctx.newTemp();
                ctx.writer().line((jt != null ? jt : "var") + " " + v + " = " + rhs.code + ";");
                ctx.writer().line(call + "(" + v + ");");
                return new Out(v, rhs.type);
            }
        }
        Out lhs = lhsOnce != null ? lhsOnce : emitExpr(a.lhs, null, ctx);
        String lcode = lhs.code;
        // A member read through an accessor method (`list.length()`, a runtime getter) is
        // written through its setter overload, `list.length(v)`: as an expression-bodied
        // setter (`set length(int v) => _items.length = v;`) it fell through to
        // `this._items.length() = v`, which is not Java.
        if (a.lhs instanceof PropertyGet && !a.op.equals("??=")) {
            String acc = "." + ((PropertyGet) a.lhs).name + "()";
            // Only the trailing accessor matters: `bag.get$items().length()` is the list's
            // length setter on whatever the accessor returns, not a `set$items` write.
            if (lcode.endsWith(acc) && !acc.startsWith(".get$")) {
                String recv = lcode.substring(0, lcode.length() - acc.length());
                return voidSetterWrite(a, recv + "." + ((PropertyGet) a.lhs).name,
                        a.op.equals("=") ? null : lcode, lhs.type, ctx);
            }
        }
        // `position = v` inside a subclass, where the inherited getter reads as the accessor
        // `this.position()` (an implemented interface declares the getter alone): the write
        // is the setter overload `this.position(v)`, never an assignment to a call.
        if (a.lhs instanceof Ident && !a.op.equals("??=")) {
            String nm = ((Ident) a.lhs).name;
            String acc = "." + nm + "()";
            if (lcode.equals("this" + acc) || lcode.equals(nm + "()")) {
                return voidSetterWrite(a, lcode.substring(0, lcode.length() - 2),
                        a.op.equals("=") ? null : lcode, lhs.type, ctx);
            }
        }
        // setters through accessors: x.get$f() as assignment target -> x.set$f(v)
        if (lcode.endsWith("()") && lcode.contains(".get$")) {
            String base = lcode.substring(0, lcode.lastIndexOf(".get$"));
            String prop = lcode.substring(lcode.lastIndexOf(".get$") + 5, lcode.length() - 2);
            // `x op= v` through an accessor pair -- a lazily initialised static or
            // top-level, or a mixin's field -- is set$x(get$x() op v).
            if (ctx.statementExpr != a && ctx.postfixAssign == a) {
                // `x.f++` as a value: the old value, read once.
                String jt = declarableType(lhs.type, false, ctx);
                String old = ctx.newTemp();
                ctx.writer().line((jt != null ? jt : "var") + " " + old + " = " + lcode + ";");
                ctx.writer().line(base + ".set$" + prop + "(" + compoundValue(old, lhs.type, a, ctx) + ");");
                return new Out(old, lhs.type);
            }
            String value = a.op.equals("=")
                    ? coerce(emitExpr(a.rhs, lhs.type, ctx), lhs.type, ctx)
                    : compoundValue(lcode, lhs.type, a, ctx);
            if (ctx.statementExpr != a) {
                // An instance field's set$ is void; as a value, the value is a temp.
                String jt = declarableType(lhs.type, false, ctx);
                String v = ctx.newTemp();
                ctx.writer().line((jt != null ? jt : "var") + " " + v + " = " + value + ";");
                ctx.writer().line(base + ".set$" + prop + "(" + v + ");");
                return new Out(v, lhs.type);
            }
            return new Out(base + ".set$" + prop + "(" + value + ")", lhs.type);
        }
        if (!a.op.equals("=") && (isDynamic(lhs.type)
                || (lhs.type != null && lhs.type.is("num") && !a.op.equals("/="))
                || (lhs.type != null && lhs.type.is("String") && a.op.equals("*=")))) {
            // `x += 1` on a dynamic x: Java's compound operators do not apply to Object.
            return new Out(lcode + " = " + compoundValue(lcode, lhs.type, a, ctx), lhs.type);
        }
        if ((a.op.equals("<<=") || a.op.equals(">>=") || a.op.equals(">>>="))
                && !(a.rhs instanceof IntLit && ((IntLit) a.rhs).value >= 0 && ((IntLit) a.rhs).value < 64)) {
            return new Out(lcode + " = " + compoundValue(lcode, lhs.type, a, ctx), lhs.type);
        }
        if (!a.op.equals("=") && !a.op.equals("??=") && lhs.type != null && !isNumeric(lhs.type)
                && !lhs.type.is("String") && !lhs.type.is("bool")) {
            // An operator the slot's type declares as a method: `total += d` is
            // `total = total.$plus(d)` (see operatorMethodValue).
            String baseOp = a.op.substring(0, a.op.length() - 1);
            String mangled = com.codename1.dart.transpiler.parser.AstBuilder.mangleOperator(baseOp);
            ClassDecl cls = program.classes.get(lhs.type.name);
            boolean declares = mangled != null
                    && (cls != null ? findMethodInHierarchy(cls, mangled) != null
                    : lhs.type.is("Duration")
                    || (stubs.isStubClass(lhs.type.name) && stubs.findMethod(lhs.type.name, mangled, false) != null));
            if (declares) {
                return new Out(lcode + " = " + compoundValue(lcode, lhs.type, a, ctx), lhs.type);
            }
        }
        String jop = a.op.equals("~/=") ? null : a.op;
        if (a.op.equals("~/=") || a.op.equals("%=")) {
            ctx.importClass("dart.runtime.DartRuntime");
            Out rhs = emitExpr(a.rhs, lhs.type, ctx);
            String fn = a.op.equals("~/=") ? "tdiv" : "mod";
            return new Out(lcode + " = DartRuntime." + fn + "(" + lcode + ", " + rhs.code + ")", lhs.type);
        }
        if (ctx.postfixAssign == a && a.rhs instanceof IntLit && (a.op.equals("+=") || a.op.equals("-="))) {
            // `a?.b++` reaches here as `$t.b += 1` under the short; it yields the old value.
            return new Out(lcode + (a.op.equals("+=") ? "++" : "--"), lhs.type);
        }
        Out rhs = emitExpr(a.rhs, lhs.type, ctx);
        return new Out(lcode + " " + jop + " " + coerce(rhs, lhs.type, ctx), lhs.type);
    }

    /**
     * Applies the flow promotions implied by a boolean guard {@code cond} holding true:
     * every {@code x is T} test (including those AND-ed together) where {@code x} is a
     * simple in-scope local narrows {@code x} to {@code T}. Returns an undo list of
     * {name, priorPromotion} pairs to pass to {@link #restorePromotions}.
     */
    private List<Object[]> applyGuardPromotions(Expr cond, Ctx ctx) {
        List<Object[]> undo = new ArrayList<Object[]>();
        collectPromotions(cond, ctx, undo);
        return undo;
    }

    /**
     * Whether {@code x is T} promotes x. Dart promotes only to a subtype of x's current type:
     * a BoxBaseImpl tested {@code is LazyBox} (an interface the class does not implement)
     * stays a BoxBaseImpl, and its members are still the ones read. Decided only when both
     * are program classes; otherwise the promotion is kept as before.
     */
    private boolean promotesTo(Ctx ctx, String name, TypeRef to) {
        TypeRef cur = ctx.promotedType(name);
        if (cur == null) {
            cur = ctx.lookup(name);
        }
        if (cur == null || to == null || cur.name == null || cur.name.equals(to.name)) {
            return true;
        }
        ClassDecl a = program.resolveClass(to.name, ctx.library());
        ClassDecl b = program.resolveClass(cur.name, ctx.library());
        if (a == null || b == null || !isProgramClassDecl(a) || !isProgramClassDecl(b)) {
            return true;
        }
        return isProgramSubtype(a, b);
    }

    private void collectPromotions(Expr cond, Ctx ctx, List<Object[]> undo) {
        if (cond instanceof Binary && "&&".equals(((Binary) cond).op)) {
            collectPromotions(((Binary) cond).left, ctx, undo);
            collectPromotions(((Binary) cond).right, ctx, undo);
            return;
        }
        if (cond instanceof IsTest) {
            IsTest t = (IsTest) cond;
            if (!t.negated && t.operand instanceof Ident && t.type != null
                    && ctx.lookup(((Ident) t.operand).name) != null
                    && promotesTo(ctx, ((Ident) t.operand).name, t.type)) {
                String name = ((Ident) t.operand).name;
                TypeRef prev = ctx.pushPromotion(name, t.type);
                undo.add(new Object[] {name, prev});
            }
        }
    }

    /**
     * Promotions implied by a boolean guard {@code cond} holding FALSE — used for the right
     * operand of {@code ||} (reached only when the left is false). A false {@code ||} means
     * every disjunct is false, so recurse both sides; a false {@code x is! T} narrows
     * {@code x} to {@code T}.
     */
    private void collectNegativePromotions(Expr cond, Ctx ctx, List<Object[]> undo) {
        if (cond instanceof Binary && "||".equals(((Binary) cond).op)) {
            collectNegativePromotions(((Binary) cond).left, ctx, undo);
            collectNegativePromotions(((Binary) cond).right, ctx, undo);
            return;
        }
        if (cond instanceof IsTest) {
            IsTest t = (IsTest) cond;
            if (t.negated && t.operand instanceof Ident && t.type != null
                    && ctx.lookup(((Ident) t.operand).name) != null) {
                String name = ((Ident) t.operand).name;
                TypeRef prev = ctx.pushPromotion(name, t.type);
                undo.add(new Object[] {name, prev});
            }
        }
    }

    private void restorePromotions(List<Object[]> undo, Ctx ctx) {
        for (int i = undo.size() - 1; i >= 0; i--) {
            ctx.restorePromotion((String) undo.get(i)[0], (TypeRef) undo.get(i)[1]);
        }
    }

    /**
     * Static type of a conditional/ternary expression: the two branch types' least upper
     * bound. Identical types win; a {@code null}/dynamic branch yields the other (boxed);
     * otherwise the nearest common ancestor, falling back to {@code Object} (never
     * {@code dynamic}, so ordinary members still resolve where the common type has them).
     */
    private TypeRef conditionalType(TypeRef a, TypeRef b, TypeRef expected) {
        if (a == null || a.is("dynamic") || a.is("Null")) {
            return b == null ? TypeRef.DYNAMIC : boxType(b);
        }
        if (b == null || b.is("dynamic") || b.is("Null")) {
            return boxType(a);
        }
        if (a.name.equals(b.name)) {
            return a;
        }
        if (expected != null && !expected.is("var") && !expected.is("dynamic")) {
            return expected;
        }
        TypeRef anc = commonAncestor(a.name, b.name);
        if (anc == null || anc.is("Object")) {
            // `cond ? this : other` in a mixin that implements other's type: the superclass
            // chains meet only at Object, but one arm already is the other's interface type.
            if (implementsName(a.name, b.name)) {
                return b;
            }
            if (implementsName(b.name, a.name)) {
                return a;
            }
        }
        return anc != null ? anc : new TypeRef("Object");
    }

    /** Whether {@code sub} reaches {@code sup} through superclasses, interfaces or mixins. */
    private boolean implementsName(String sub, String sup) {
        if (sub == null || sup == null) {
            return false;
        }
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<String>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        queue.add(sub);
        seen.add(sub);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur.equals(sup)) {
                return true;
            }
            ClassDecl pc = program.classes.get(cur);
            Ast.ClassDecl sc = pc != null ? pc : stubs.classes.get(cur);
            if (sc == null) {
                continue;
            }
            java.util.List<TypeRef> edges = new java.util.ArrayList<TypeRef>();
            if (sc.superclass != null) {
                edges.add(sc.superclass);
            }
            edges.addAll(sc.interfaces);
            edges.addAll(sc.mixins);
            for (TypeRef t : edges) {
                if (t != null && seen.add(t.name)) {
                    queue.add(t.name);
                }
            }
        }
        return false;
    }

    /** Nearest common ancestor class name of two types (program or stub), or null. */
    private TypeRef commonAncestor(String x, String y) {
        java.util.LinkedHashSet<String> xs = new java.util.LinkedHashSet<String>();
        for (String c = x; c != null; c = superName(c)) {
            xs.add(c);
        }
        for (String c = y; c != null; c = superName(c)) {
            if (xs.contains(c)) {
                return new TypeRef(c);
            }
        }
        return null;
    }

    /** Direct superclass name of a program or stub class, or null. */
    private String superName(String name) {
        ClassDecl pc = program.classes.get(name);
        if (pc != null && pc.superclass != null) {
            return pc.superclass.name;
        }
        Ast.ClassDecl sc = stubs.classes.get(name);
        if (sc != null && sc.superclass != null) {
            return sc.superclass.name;
        }
        return null;
    }

    /**
     * Emits {@code e} as a statement: its value is discarded, so an assignment through
     * a void setter keeps its plain call form ({@link #voidSetterWrite}).
     */
    private Out emitStatementExpr(Expr e, Ctx ctx) {
        Expr saved = ctx.statementExpr;
        ctx.statementExpr = e;
        try {
            return emitExpr(e, null, ctx);
        } finally {
            ctx.statementExpr = saved;
        }
    }

    /** True when some selector in the receiver chain of {@code e} is null-aware ({@code a?.b}). */
    private static boolean chainHasNullAware(Expr e) {
        while (e != null) {
            if (e instanceof PropertyGet) {
                if (((PropertyGet) e).nullAware) {
                    return true;
                }
                e = ((PropertyGet) e).target;
            } else if (e instanceof Call) {
                if (((Call) e).nullAware) {
                    return true;
                }
                e = ((Call) e).target;
            } else if (e instanceof IndexGet) {
                e = ((IndexGet) e).target;
            } else {
                return false;
            }
        }
        return false;
    }

    /** An assignment whose target sits behind a null-aware selector, which shorts it. */
    private static boolean needsShort(Assign a) {
        if (a.lhs instanceof PropertyGet) {
            PropertyGet pg = (PropertyGet) a.lhs;
            return pg.nullAware || chainHasNullAware(pg.target);
        }
        return a.lhs instanceof IndexGet && chainHasNullAware(((IndexGet) a.lhs).target);
    }

    /**
     * {@code a?.b = v}, {@code a?.b op= v}, {@code a?.b ??= v}, {@code a?.b[i] = v}: a
     * null receiver shorts the whole assignment -- neither the index nor the value is
     * evaluated, and its value is null. The assignment was emitted as though the
     * {@code ?.} were a plain {@code .}, and threw on the null. The write is emitted
     * against a temp holding the non-null receiver, inside {@code if (guard != null)}.
     * Returns null when the target is not behind a null-aware selector.
     */
    private Out emitShortedAssign(Assign a, Ctx ctx) {
        if (!needsShort(a)) {
            return null;
        }
        Expr recvExpr = a.lhs instanceof PropertyGet ? ((PropertyGet) a.lhs).target : ((IndexGet) a.lhs).target;
        boolean direct = a.lhs instanceof PropertyGet && ((PropertyGet) a.lhs).nullAware;
        String guard;
        String recvInit = null;
        TypeRef recvType;
        if (direct) {
            Out mat = emitExpr(recvExpr, null, ctx);
            guard = ctx.newTemp();
            ctx.writer().line("var " + guard + " = " + mat.code + ";");
            recvType = copyNonNull(mat.type);
        } else {
            Out raw = emitExprRaw(recvExpr, null, ctx);
            guard = raw.shortGuard;
            recvType = copyNonNull(raw.type);
            recvInit = raw.code;
        }
        ctx.pushWriter(ctx.currentIndent() + 1);
        String recv = guard;
        if (recvInit != null) {
            // The rest of the receiver chain, read where the guard says it may be.
            recv = ctx.newTemp();
            String jt = declarableType(recvType, false, ctx);
            ctx.writer().line((jt != null ? jt : "var") + " " + recv + " = " + recvInit + ";");
        }
        ctx.declare(recv, recvType);
        Ident id = new Ident().at(a.file, a.line, a.col);
        id.name = recv;
        Expr lhs;
        if (a.lhs instanceof PropertyGet) {
            PropertyGet copy = new PropertyGet().at(a.lhs.file, a.lhs.line, a.lhs.col);
            copy.target = id;
            copy.name = ((PropertyGet) a.lhs).name;
            lhs = copy;
        } else {
            IndexGet copy = new IndexGet().at(a.lhs.file, a.lhs.line, a.lhs.col);
            copy.target = id;
            copy.index = ((IndexGet) a.lhs).index;
            lhs = copy;
        }
        Assign inner = new Assign().at(a.file, a.line, a.col);
        inner.lhs = lhs;
        inner.op = a.op;
        inner.rhs = a.rhs;
        boolean statement = ctx.statementExpr == a;
        Expr savedStmt = ctx.statementExpr;
        Assign savedPostfix = ctx.postfixAssign;
        if (statement) {
            ctx.statementExpr = inner;
        }
        if (ctx.postfixAssign == a) {
            ctx.postfixAssign = inner;
        }
        Out w = emitAssign(inner, ctx);
        ctx.statementExpr = savedStmt;
        ctx.postfixAssign = savedPostfix;
        String lifted = ctx.popWriter();
        if (guard == null) {
            // No short reached the receiver after all (a parenthesized chain): plain.
            ctx.writer().raw(lifted);
            return w;
        }
        if (statement || w.type == null || w.type.is("void")) {
            return guardLifted(guard, lifted, new Out(w.code, TypeRef.VOID), ctx);
        }
        Out g = guardLifted(guard, lifted, w, ctx);
        return g != null ? g : w.withShort(guard);
    }

    /**
     * A write through a setter, which Java declares void. As a statement it is the
     * plain call; used as a value -- {@code (x.p += 3) > 0}, {@code print(x.p = 4)} --
     * the new value goes into a temp first, the setter is called with it, and the
     * value is the temp; for a postfix {@code x.p++} ({@link Ctx#postfixAssign}) the
     * old value is read into the temp instead. The call as a value was
     * {@code 'void' type not allowed here} from javac.
     *
     * @param recv  the receiver, already evaluated once when the write reads it too
     * @param call  the setter call up to its argument, e.g. {@code recv.p}
     * @param read  the getter read for a compound write, null for {@code =}
     */
    private Out voidSetterWrite(Assign a, String call, String read, TypeRef pt, Ctx ctx) {
        boolean statement = ctx.statementExpr == a;
        String jt = declarableType(pt, false, ctx);
        if (jt == null) {
            jt = "var";
        }
        Ctx.Writer w = ctx.writer();
        if (!statement && ctx.postfixAssign == a && read != null) {
            String old = ctx.newTemp();
            w.line(jt + " " + old + " = " + read + ";");
            w.line(call + "(" + compoundValue(old, pt, a, ctx) + ");");
            return new Out(old, pt);
        }
        String value = read == null
                ? coerce(emitExpr(a.rhs, pt, ctx), pt, ctx)
                : compoundValue(read, pt, a, ctx);
        if (statement) {
            return new Out(call + "(" + value + ")", pt);
        }
        String v = ctx.newTemp();
        w.line(jt + " " + v + " = " + value + ";");
        w.line(call + "(" + v + ");");
        return new Out(v, pt);
    }

    /** An operand emitted by {@link #emitLazyOperand}, and the statements it lifted. */
    private static final class Lazy {
        final Out out;
        /** Already indented; empty when the operand lifted nothing. */
        final String lifted;

        Lazy(Out out, String lifted) {
            this.out = out;
            this.lifted = lifted;
        }

        boolean lifts() {
            return !lifted.isEmpty();
        }
    }

    /**
     * Emits an operand Java evaluates only sometimes, or more than once -- a
     * conditional's arm, the right side of {@code && || ??}, a null-short's call, a
     * loop condition or update -- keeping apart whatever statements its emission
     * lifts (a compound index assignment's receiver temps, a spread list's builder,
     * a cascade, a named-argument constructor). Lifted into the enclosing statement
     * they ran once and unconditionally: {@code while ((xs[next()] += 1) < 10)} called
     * next() once, {@code false && (a[f()] += 1) > 0} still called f(), and
     * {@code xs == null ? <int>[] : [...xs]} spread a null list before the test.
     * The caller must run {@code lifted} exactly where the operand is evaluated; with
     * nothing lifted the operand is emitted exactly as it always was.
     *
     * @param depth how many blocks deeper than the current statement the caller will
     *              place the lifted lines, for their indentation
     */
    private Lazy emitLazyOperand(Expr e, TypeRef expected, Ctx ctx, int depth) {
        ctx.pushWriter(ctx.currentIndent() + depth);
        Out o = emitExpr(e, expected, ctx);
        String lifted = ctx.popWriter();
        if (!lifted.isEmpty() && onlyTempDeclarations(lifted)) {
            // A bare `T $t3;` runs nothing; it only gives the operand's expression a
            // temp to assign (`(($t3 = a) != null ? $t3 : b)`). It stays in front of
            // the enclosing statement, as it always was, so the operand keeps its
            // plain expression form instead of costing an if/else, an if or a
            // break-loop for no change in behaviour.
            Ctx.Writer w = ctx.writer();
            for (String line : lifted.split("\n")) {
                if (!line.trim().isEmpty() && !DartLineTable.isMark(line)) {
                    w.line(line.trim());
                }
            }
            lifted = "";
        }
        return new Lazy(o, lifted);
    }

    /** A temp declaration with no initialiser: {@code Widget $t3;}. */
    private static final java.util.regex.Pattern TEMP_DECLARATION = java.util.regex.Pattern.compile(
            "[A-Za-z_][\\w.$]*(<[^;=(){}]*>)?(\\[\\])* \\$t\\d+;");

    /** True when every line of {@code lifted} is a {@link #TEMP_DECLARATION}. */
    private static boolean onlyTempDeclarations(String lifted) {
        for (String line : lifted.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !DartLineTable.isMark(t) && !TEMP_DECLARATION.matcher(t).matches()) {
                return false;
            }
        }
        return true;
    }

    /** A Java type a temp can be declared with for {@code t}, or null when there is none. */
    private String declarableType(TypeRef t, boolean boxed, Ctx ctx) {
        if (t == null || t.is("var") || t.is("void") || t.funcParams != null || t.is("Function")) {
            return null;
        }
        return javaType(t, boxed, ctx);
    }

    /**
     * {@code called}, whose emission lifted {@code lifted}, evaluated only while
     * {@code guard} is non-null: the tail of a null-short ({@code a?.add([...xs])}),
     * whose arguments Dart never evaluates when {@code a} is null. Returns null --
     * after appending the lines unguarded, as before -- when no temp can hold the
     * result.
     */
    private Out guardLifted(String guard, String lifted, Out called, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        if (called.type == null || called.type.is("void") || called.code == null || called.code.isEmpty()) {
            w.line("if (" + guard + " != null) {");
            w.raw(lifted);
            String code = called.code == null ? "" : statementize(called.code);
            if (!code.isEmpty()) {
                ctx.indent(1);
                w.line(code + ";");
                ctx.indent(-1);
            }
            w.line("}");
            return new Out("", TypeRef.VOID);
        }
        TypeRef bt = boxType(called.type);
        String jt = declarableType(bt, true, ctx);
        if (jt == null) {
            w.raw(lifted);
            return null;
        }
        String r = ctx.newTemp();
        w.line(jt + " " + r + " = null;");
        w.line("if (" + guard + " != null) {");
        w.raw(lifted);
        ctx.indent(1);
        w.line(r + " = " + called.code + ";");
        ctx.indent(-1);
        w.line("}");
        return new Out(r, bt, called.fromError, guard);
    }

    /**
     * Writes a C-style {@code for} loop -- statement or collection element -- and runs
     * {@code body} inside it. A condition or update that lifts statements (see
     * {@link #emitLazyOperand}) has them run on every evaluation: the condition moves
     * into the body as {@code if (!(cond)) break;}, after its lines, and lifting
     * updates run at the top of every pass but the first. A {@code continue} still
     * reaches both, since each sits where the loop restarts.
     */
    private void emitCForLoop(String initCode, Expr condition, List<Expr> updates, Ctx ctx, Runnable body) {
        emitCForLoop("", initCode, condition, updates, ctx, body);
    }

    private void emitCForLoop(String label, String initCode, Expr condition, List<Expr> updates, Ctx ctx,
                              Runnable body) {
        Ctx.Writer w = ctx.writer();
        Lazy cond = condition != null ? emitLazyOperand(condition, TypeRef.BOOL, ctx, 1) : null;
        List<Lazy> ups = new ArrayList<Lazy>();
        boolean updatesLift = false;
        for (Expr u : updates) {
            Expr savedStmt = ctx.statementExpr;
            ctx.statementExpr = u;
            Lazy l = emitLazyOperand(u, null, ctx, 2);
            ctx.statementExpr = savedStmt;
            ups.add(l);
            updatesLift |= l.lifts();
        }
        boolean condLifts = cond != null && cond.lifts();
        if (!condLifts && !updatesLift) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ups.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(statementize(ups.get(i).out.code));
            }
            w.line(label + "for (" + initCode + "; " + (cond != null ? cond.out.code : "") + "; " + sb + ") {");
            ctx.indent(1);
        } else {
            if (updatesLift) {
                String first = ctx.newTemp();
                w.line("boolean " + first + " = true;");
                w.line(label + "for (" + initCode + "; ; ) {");
                ctx.indent(1);
                w.line("if (!" + first + ") {");
                for (Lazy l : ups) {
                    w.raw(l.lifted);
                    String code = statementize(l.out.code);
                    if (!code.isEmpty()) {
                        ctx.indent(1);
                        w.line(code + ";");
                        ctx.indent(-1);
                    }
                }
                w.line("}");
                w.line(first + " = false;");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ups.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(statementize(ups.get(i).out.code));
                }
                w.line(label + "for (" + initCode + "; ; " + sb + ") {");
                ctx.indent(1);
            }
            if (cond != null) {
                w.raw(cond.lifted);
                w.line("if (!(" + cond.out.code + ")) {");
                ctx.indent(1);
                w.line("break;");
                ctx.indent(-1);
                w.line("}");
            }
        }
        body.run();
        ctx.indent(-1);
        w.line("}");
    }

    /**
     * {@code a ?? b} whose default lifted statements: {@code tmp = a; if (tmp == null)
     * { <lifted>; tmp = b; }}. {@code jt} declares {@code tmp} here, or is null when
     * the caller already declared it.
     */
    private Out nullCoalesceLifted(String jt, String tmp, Out left, Lazy right, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        w.line((jt != null ? jt + " " : "") + tmp + " = " + left.code + ";");
        w.line("if (" + tmp + " == null) {");
        w.raw(right.lifted);
        ctx.indent(1);
        w.line(tmp + " = " + right.out.code + ";");
        ctx.indent(-1);
        w.line("}");
        return new Out(tmp, copyNonNull(left.type));
    }

    /**
     * {@code l && r} (or {@code l || r}) whose right operand lifted statements: they
     * run only when the left side does not decide the result, as Dart evaluates it.
     */
    private Out shortCircuitLifted(Out l, Lazy r, boolean and, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        String tmp = ctx.newTemp();
        w.line("boolean " + tmp + " = " + logicalOperand(l, ctx) + ";");
        w.line("if (" + (and ? tmp : "!" + tmp) + ") {");
        w.raw(r.lifted);
        ctx.indent(1);
        w.line(tmp + " = " + logicalOperand(r.out, ctx) + ";");
        ctx.indent(-1);
        w.line("}");
        return new Out(tmp, TypeRef.BOOL);
    }

    /**
     * {@code l op r} on a dart:core Duration, as a call to the runtime method carrying
     * the operator's mangled name; null when {@code op} is not one Duration declares for
     * that operand type.
     */
    private Out durationOperator(Out l, String op, Out r) {
        if (l.type == null || !l.type.is("Duration") || r.type == null
                || program.classes.containsKey("Duration")) {
            return null;
        }
        String mangled = com.codename1.dart.transpiler.parser.AstBuilder.mangleOperator(op);
        boolean durationArg = r.type.is("Duration");
        if ((("$plus".equals(mangled) || "$minus".equals(mangled)) && durationArg)
                || ("$times".equals(mangled) && (isNumeric(r.type) || r.type.is("num")))
                || ("$tdiv".equals(mangled) && r.type.is("int"))) {
            return new Out(l.code + "." + mangled + "(" + paren(r.code) + ")", new TypeRef("Duration"));
        }
        if (durationArg && ("$lt".equals(mangled) || "$le".equals(mangled)
                || "$gt".equals(mangled) || "$ge".equals(mangled))) {
            return new Out(l.code + "." + mangled + "(" + paren(r.code) + ")", TypeRef.BOOL);
        }
        return null;
    }

    /**
     * {@code read op rhs} where the slot's type declares {@code op} as a method -- an app
     * class, a stub value type or Duration -- for a compound assignment; null otherwise.
     * Java's {@code +=} does not apply to objects, so {@code total += d} on a Duration
     * (or an Offset, or an app class with operator +) did not compile.
     */
    private Out operatorMethodValue(String readCode, TypeRef vt, String baseOp, Out rhs) {
        if (vt == null || vt.name == null || isDynamic(vt)) {
            return null;
        }
        String mangled = com.codename1.dart.transpiler.parser.AstBuilder.mangleOperator(baseOp);
        if (mangled == null) {
            return null;
        }
        ClassDecl cls = program.classes.get(vt.name);
        if (cls != null) {
            MethodDecl om = findMethodInHierarchy(cls, mangled);
            return om == null ? null : new Out(readCode + "." + mangled + "(" + paren(rhs.code) + ")",
                    om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType);
        }
        Ast.MethodDecl om = stubs.isStubClass(vt.name) ? stubs.findMethod(vt.name, mangled, false) : null;
        if (om != null) {
            return new Out(readCode + "." + mangled + "(" + paren(rhs.code) + ")",
                    om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType);
        }
        // Duration is ALSO a stub class (flutter-runtime declares its getters), one
        // that declares no operators, so it is consulted after the stub.
        return durationOperator(new Out(readCode, vt), baseOp, rhs);
    }

    private Out emitBinary(Binary b, Ctx ctx) {
        if (b.left instanceof SuperExpr && (b.op.equals("==") || b.op.equals("!="))) {
            // `super == other` invokes the superclass's operator ==, which every class
            // emits as equals(Object); Object's own is identity, as Dart's is. Dart answers
            // false for a null operand without calling the operator, and so does equals.
            Out r = emitExpr(b.right, null, ctx);
            String eq = "super.equals(" + boxIfPrimitive(r, ctx) + ")";
            return new Out(b.op.equals("==") ? eq : "!" + eq, TypeRef.BOOL);
        }
        if (b.op.equals("??")) {
            // Peephole: (m[k] ?? literal) on a primitive Map<int,int> -> getLongOr(k, literal),
            // eliminating the boxed read. Only for a side-effect-free literal default, so eager
            // evaluation of the default matches ??'s short-circuit semantics.
            if (b.left instanceof IndexGet && isPureLiteral(b.right)) {
                IndexGet ig = (IndexGet) b.left;
                Out mt = emitExpr(ig.target, null, ctx);
                if (isPrimitiveLongMap(mt.type)) {
                    Out idx = emitExpr(ig.index, null, ctx);
                    Out def = emitExpr(b.right, TypeRef.of("int"), ctx);
                    return new Out(mt.code + ".getLongOr(" + idx.code + ", " + def.code + ")", TypeRef.of("int"));
                }
            }
            Out left = emitExpr(b.left, null, ctx);
            // `a ?? b` must evaluate a once and b only when a is null, WHERE the
            // expression is -- which may be a loop condition, a short-circuited operand
            // or a conditional's arm. Lifting `var t = a;` into a statement ran a once
            // before the enclosing statement instead: `while (next() ?? false)` tested
            // one stale value forever, and `false && (f() ?? true)` still called f().
            // Only the temp's declaration is lifted now; the assignment stays inline.
            // The default can lift statements of its own (`xs ?? [...fallback()]`); they
            // must run only when the left side is null, so then the whole operator
            // becomes `tmp = a; if (tmp == null) { <lifted>; tmp = b; }`.
            Out right;
            TypeRef lt = left.type;
            String jt = lt == null || lt.is("var") || lt.funcParams != null ? null : javaType(lt, true, ctx);
            if (b.left instanceof Ident || b.left instanceof ThisExpr) {
                // A variable read has no effect and nothing to cache.
                Lazy rl = emitLazyOperand(b.right, left.type, ctx, 1);
                if (rl.lifts() && jt != null) {
                    return nullCoalesceLifted(jt, ctx.newTemp(), left, rl, ctx);
                }
                ctx.writer().raw(rl.lifted);
                right = rl.out;
return new Out("(" + left.code + " != null ? " + left.code + " : " + right.code + ")",
                        ifNullType(left.type, right.type));
            }
            String tmp = ctx.newTemp();
            if (jt == null) {
                // A type javac infers but this emitter cannot name: no declaration can
                // hold it, so it keeps the eager form.
                ctx.writer().line("var " + tmp + " = " + left.code + ";");
                right = emitExpr(b.right, left.type, ctx);
                return new Out("(" + tmp + " != null ? " + tmp + " : " + right.code + ")",
                        copyNonNull(left.type));
            }
            ctx.writer().line(jt + " " + tmp + ";");
            Lazy rl = emitLazyOperand(b.right, left.type, ctx, 1);
            if (rl.lifts()) {
                return nullCoalesceLifted(null, tmp, left, rl, ctx);
            }
            right = rl.out;
            return new Out("((" + tmp + " = " + left.code + ") != null ? " + tmp + " : " + right.code + ")",
                    ifNullType(left.type, right.type));
        }
        if (b.op.equals("&&")) {
            // `x is T && x.member`: the left `is` guard flow-promotes x to T for the right operand.
            Out l = emitExpr(b.left, null, ctx);
            List<Object[]> undo = applyGuardPromotions(b.left, ctx);
            Lazy r = emitLazyOperand(b.right, null, ctx, 1);
            restorePromotions(undo, ctx);
            if (r.lifts()) {
                return shortCircuitLifted(l, r, true, ctx);
            }
            return new Out(logicalOperand(l, ctx) + " && " + logicalOperand(r.out, ctx), TypeRef.BOOL);
        }
        if (b.op.equals("||")) {
            // `x is! T || x.member`: reaching the right operand means the left was false,
            // i.e. `x is T` held — flow-promote x to T for the right operand.
            Out l = emitExpr(b.left, null, ctx);
            List<Object[]> undo = new ArrayList<Object[]>();
            collectNegativePromotions(b.left, ctx, undo);
            Lazy r = emitLazyOperand(b.right, null, ctx, 1);
            restorePromotions(undo, ctx);
            if (r.lifts()) {
                return shortCircuitLifted(l, r, false, ctx);
            }
            return new Out(logicalOperand(l, ctx) + " || " + logicalOperand(r.out, ctx), TypeRef.BOOL);
        }
        Out l = emitExpr(b.left, null, ctx);
        ctx.pushWriter(ctx.currentIndent());
        Out r = emitExpr(b.right, null, ctx);
        String rLifted = ctx.popWriter();
        if (!rLifted.isEmpty() && !(b.left instanceof IntLit || b.left instanceof StringLit
                || b.left instanceof BoolLit || b.left instanceof NullLit)) {
            // `f() + await g()`: the right operand lifted statements, which would run
            // before the left operand's expression. Dart evaluates left first, so the
            // left value is pinned in a temp ahead of them.
            String jt = declarableType(l.type, false, ctx);
            String lt = ctx.newTemp();
            ctx.writer().line((jt != null ? jt : "var") + " " + lt + " = " + l.code + ";");
            l = new Out(lt, l.type);
        }
        ctx.writer().raw(rLifted);
        boolean numeric = isNumeric(l.type) && isNumeric(r.type);
        // user-defined operators on program classes
        ClassDecl opClass = program.classes.get(l.type.name);
        if (opClass != null && !b.op.equals("==") && !b.op.equals("!=")
                && !b.op.equals("&&") && !b.op.equals("||") && !b.op.equals("??")) {
            String mangled = com.codename1.dart.transpiler.parser.AstBuilder.mangleOperator(b.op);
            MethodDecl om = mangled != null ? findMethodInHierarchy(opClass, mangled) : null;
            if (om != null) {
                return new Out(l.code + "." + mangled + "(" + r.code + ")",
                        om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType);
            }
        }
        // user-defined operators on stub value types (e.g. Offset + Offset, Radius * t)
        if (opClass == null && l.type != null && stubs.isStubClass(l.type.name)
                && !b.op.equals("==") && !b.op.equals("!=")
                && !b.op.equals("&&") && !b.op.equals("||") && !b.op.equals("??")) {
            String mangled = com.codename1.dart.transpiler.parser.AstBuilder.mangleOperator(b.op);
            Ast.MethodDecl om = mangled != null ? stubs.findMethod(l.type.name, mangled, false) : null;
            if (om != null) {
                return new Out(l.code + "." + mangled + "(" + paren(r.code) + ")",
                        om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType);
            }
        }
        if (b.op.equals("==") || b.op.equals("!=")) {
            // Java's primitive == on an int and a double widens the int, and that is Dart's
            // semantics too, not an approximation of them: measured on the Dart 3.9 VM,
            // 9007199254740993 == 9007199254740992.0 is TRUE and the matching > is false.
            // Only compareTo compares an int and a double exactly.
            //
            // That shortcut only holds for a non-nullable primitive on both sides. A
            // nullable operand (int?/double?/num?, emitted as boxed Long/Double) must not
            // take Java's ==: it auto-unboxes a null operand and throws NPE instead of
            // answering false, and two equal boxed values would compare by reference
            // rather than by value. Fall through to the null-safe, value-based helper below.
            if ((numeric || (l.type.is("bool") && r.type.is("bool"))) && !l.type.nullable && !r.type.nullable) {
                return new Out(paren(l.code) + " " + b.op + " " + paren(r.code), TypeRef.BOOL);
            }
            ctx.importClass("dart.runtime.DartRuntime");
            // `T == String`: a type literal operand compares as its class.
            if (l.type != null && isClassRef(l.type)) {
                l = new Out(classLiteral(l, ctx), new TypeRef("Type"));
            }
            if (r.type != null && isClassRef(r.type)) {
                r = new Out(classLiteral(r, ctx), new TypeRef("Type"));
            }
            String eq = "DartRuntime.eq(" + l.code + ", " + boxIfPrimitive(r, ctx) + ")";
            return new Out(b.op.equals("==") ? eq : "!" + eq, TypeRef.BOOL);
        }
        // A dynamic operand: Dart chooses the operator from the run-time value, and none
        // of the typed lowerings below apply -- the fallback emitted `Object + long`, which
        // javac rejects. A statically-String `+` keeps its concatenation.
        if ((isDynamic(l.type) || isDynamic(r.type))
                && !(b.op.equals("+") && l.type.is("String") && r.type.is("String"))) {
            ctx.importClass("dart.runtime.DartRuntime");
            if (b.op.equals("<") || b.op.equals(">") || b.op.equals("<=") || b.op.equals(">=")) {
                return new Out("DartRuntime.dynCompare(\"" + b.op + "\", " + l.code + ", " + r.code + ")",
                        TypeRef.BOOL);
            }
            return new Out("DartRuntime.dynBinary(\"" + b.op + "\", " + l.code + ", " + r.code + ")",
                    TypeRef.DYNAMIC);
        }
        // dart:core's Duration declares + - * ~/ and the relational operators; the runtime
        // class carries them under the mangled names. Without this they reached the
        // fallback below, which emitted `a + b` on two objects and javac rejected it.
        if (opClass == null) {
            Out dur = durationOperator(l, b.op, r);
            if (dur != null) {
                return dur;
            }
        }
        if (b.op.equals("~/")) {
            ctx.importClass("dart.runtime.DartRuntime");
            return new Out("DartRuntime.tdiv(" + l.code + ", " + r.code + ")", TypeRef.INT);
        }
        if (b.op.equals("%")) {
            if (numeric) {
                ctx.importClass("dart.runtime.DartRuntime");
                TypeRef t = l.type.is("double") || r.type.is("double") ? TypeRef.DOUBLE : TypeRef.INT;
                return new Out("DartRuntime.mod(" + l.code + ", " + r.code + ")", t);
            }
        }
        if (b.op.equals("/") && numeric) {
            if (l.type.is("int") && r.type.is("int")) {
                return new Out("((double) " + paren(l.code) + ") / " + paren(r.code), TypeRef.DOUBLE);
            }
            return new Out(paren(l.code) + " / " + paren(r.code), TypeRef.DOUBLE);
        }
        // Dart's String repetition, `'ab' * 3`. Only the dynamic path handled it, so the
        // typed form reached the fallback and emitted a Java string multiplication.
        if (b.op.equals("*") && l.type != null && l.type.is("String") && r.type != null && r.type.is("int")) {
            ctx.importClass("dart.core.DString");
            return new Out("DString.repeat(" + l.code + ", " + r.code + ")", TypeRef.STRING);
        }
        // Dart's `List + List` concatenation -> a new DartList.
        if (b.op.equals("+") && l.type != null && l.type.is("List") && r.type != null && r.type.is("List")) {
            ctx.importClass("dart.core.DartList");
            return new Out("DartList.concat(" + l.code + ", " + r.code + ")", l.type);
        }
        if (b.op.equals("+") && (l.type.is("String") || r.type.is("String"))) {
            // Keep string-concatenation chains flat so javac fuses them into ONE StringBuilder. A left
            // operand that is itself a `+` concatenation needs no parens (same precedence, left-assoc);
            // wrapping it (as paren() would) forces a separate builder + intermediate String per link,
            // which is pure GC churn. Non-concat left operands keep their precedence parens.
            String left = (b.left instanceof Binary && "+".equals(((Binary) b.left).op)) ? l.code : paren(l.code);
            return new Out(left + " + " + paren(r.code), TypeRef.STRING);
        }
        // `num` (Java Number) arithmetic/comparison: unbox the num operand(s) to double so Java's
        // numeric operators apply (Dart's `num` is the int|double supertype; a Number reference
        // cannot be used with +, -, *, / directly).
        if ((l.type != null && l.type.is("num")) || (r.type != null && r.type.is("num"))) {
            boolean lok = l.type != null && (isNumeric(l.type) || l.type.is("num"));
            boolean rok = r.type != null && (isNumeric(r.type) || r.type.is("num"));
            boolean arith = b.op.equals("+") || b.op.equals("-") || b.op.equals("*") || b.op.equals("/");
            boolean cmp = b.op.equals("<") || b.op.equals(">") || b.op.equals("<=") || b.op.equals(">=");
            if (lok && rok && (arith || cmp)) {
                // Dispatched on the VALUES, as Dart does: an int held in a num stays an
                // int (num n = 1; n + 1 is the int 2) and compares exactly above 2^53.
                // Converting every num to double lost both. Only / always yields a
                // double, so it keeps the double path.
                ctx.importClass("dart.runtime.DartRuntime");
                if (cmp) {
                    return new Out("DartRuntime.dynCompare(\"" + b.op + "\", " + l.code + ", " + r.code + ")",
                            TypeRef.BOOL);
                }
                if (!b.op.equals("/")) {
                    return new Out("((Number) DartRuntime.dynBinary(\"" + b.op + "\", " + l.code + ", "
                            + r.code + "))", new TypeRef("num"));
                }
                String lc = l.type.is("num") ? "((Number) " + paren(l.code) + ").doubleValue()" : l.code;
                String rc = r.type.is("num") ? "((Number) " + paren(r.code) + ").doubleValue()" : r.code;
                return new Out(paren(lc) + " " + b.op + " " + paren(rc), TypeRef.DOUBLE);
            }
        }
        // Integer shifts with Dart's count rules; a constant count of 0..63 is where
        // Java's operator already agrees, and keeps it.
        if ((b.op.equals("<<") || b.op.equals(">>") || b.op.equals(">>>"))
                && l.type != null && l.type.is("int") && r.type != null && r.type.is("int")
                && !(b.right instanceof IntLit && ((IntLit) b.right).value >= 0 && ((IntLit) b.right).value < 64)) {
            ctx.importClass("dart.runtime.DartRuntime");
            String fn = b.op.equals("<<") ? "shl" : b.op.equals(">>") ? "shr" : "ushr";
            return new Out("DartRuntime." + fn + "(" + l.code + ", " + r.code + ")", TypeRef.INT);
        }
        // Relational operators on enum operands compare by declaration order (Dart enum
        // semantics). Java enums expose that order as ordinal().
        if ((b.op.equals("<") || b.op.equals(">") || b.op.equals("<=") || b.op.equals(">="))
                && l.type != null
                && (program.enums.containsKey(l.type.name) || stubs.isStubEnum(l.type.name))) {
            return new Out(paren(l.code) + ".ordinal() " + b.op + " " + paren(r.code) + ".ordinal()",
                    TypeRef.BOOL);
        }
        TypeRef t;
        if (b.op.equals("<") || b.op.equals(">") || b.op.equals("<=") || b.op.equals(">=")) {
            t = TypeRef.BOOL;
        } else if (b.op.equals("&&") || b.op.equals("||")) {
            t = TypeRef.BOOL;
        } else if (numeric) {
            t = l.type.is("double") || r.type.is("double") ? TypeRef.DOUBLE : TypeRef.INT;
        } else {
            t = l.type;
        }
        return new Out(paren(l.code) + " " + b.op + " " + paren(r.code), t);
    }

    /**
     * The catch clauses of one try, as ONE {@code catch (Throwable)} whose body tests the
     * Dart clauses in order and rethrows what none of them takes. Separate Java catch
     * clauses cannot express Dart's rules: `on Error` and `on Exception` are tests, not
     * Java types (this runtime's Dart errors are RuntimeExceptions), a thrown value that is
     * not an exception -- `throw 'boom'`, `throw token` -- arrives carried in a DartThrown
     * and must be matched by ITS type and bound as itself, and a filtered clause that
     * rethrew from its own Java catch skipped every clause after it.
     */
    private void emitCatchChain(List<CatchClause> catches, Ctx ctx) {
        Ctx.Writer w = ctx.writer();
        ctx.importClass("dart.runtime.DartRuntime");
        String caught = ctx.newTemp();
        w.line("} catch (Throwable " + caught + ") {");
        ctx.indent(1);
        boolean first = true;
        boolean catchAll = false;
        for (CatchClause cc : catches) {
            TypeRef on = cc.onType;
            String test;
            String bindType;
            String bindValue;
            TypeRef varType;
            if (on == null || on.is("Object") || on.is("dynamic")) {
                test = null;
                bindType = "Object";
                bindValue = "DartRuntime.caught(" + caught + ")";
                varType = TypeRef.DYNAMIC;
            } else if (on.is("Error") && on.args.isEmpty()) {
                test = "DartRuntime.isDartError(" + caught + ")";
                bindType = "Object";
                bindValue = caught;
                varType = TypeRef.DYNAMIC;
            } else if (on.is("Exception") && on.args.isEmpty()) {
                test = "DartRuntime.isDartException(" + caught + ")";
                bindType = "Object";
                bindValue = caught;
                varType = TypeRef.DYNAMIC;
            } else if (isThrowableType(on)) {
                String jt;
                String coreError = CORE_ERRORS.get(on.name);
                if (coreError != null) {
                    ctx.importClass(coreError);
                    jt = coreError.substring(coreError.lastIndexOf('.') + 1);
                } else {
                    jt = javaType(on, true, ctx);
                }
                test = caught + " instanceof " + jt;
                bindType = jt;
                bindValue = "(" + jt + ") " + caught;
                varType = on;
                if (on.is("ConcurrentModificationError")) {
                    // Maps and sets iterate with Java's iterators, which report a change
                    // during iteration as java.util.ConcurrentModificationException; it is
                    // the same Dart error, so the clause takes both.
                    // Bound as the Dart error in both cases, so `e is ConcurrentModificationError`
                    // holds and e has that type's API.
                    test = "(" + test + " || " + caught + " instanceof java.util.ConcurrentModificationException)";
                    bindValue = "DartRuntime.asConcurrentModificationError(" + caught + ")";
                }
            } else {
                // A thrown value that is not an exception: matched by its own type.
                ctx.importClass("dart.core.DartThrown");
                String jt = javaType(on, true, ctx);
                String value = "((DartThrown) " + caught + ").value()";
                test = caught + " instanceof DartThrown && " + value + " instanceof " + jt;
                bindType = jt;
                bindValue = "(" + jt + ") " + value;
                varType = on;
            }
            if (test == null) {
                w.line(first ? "{" : "} else {");
                catchAll = true;
            } else {
                w.line((first ? "if (" : "} else if (") + test + ") {");
            }
            first = false;
            ctx.indent(1);
            ctx.pushScope();
            String var = ctx.declareShadowSafe(cc.exceptionVar != null ? cc.exceptionVar : "$e", varType);
            w.line(bindType + " " + var + " = " + bindValue + ";");
            if (cc.stackVar != null) {
                // The caught exception's own trace: the one an async error carried
                // (completeError/addError), else the Java frames of the throwable.
                String stackJn = ctx.declareShadowSafe(cc.stackVar, new TypeRef("StackTrace"));
                w.line("dart.core.StackTrace " + stackJn + " = dart.core.StackTrace.ofThrown(" + caught + ");");
            }
            String outerRethrow = ctx.rethrowTarget;
            ctx.rethrowTarget = caught;
            emitStatements(cc.body, ctx);
            ctx.rethrowTarget = outerRethrow;
            ctx.popScope();
            ctx.indent(-1);
            if (catchAll) {
                break;   // a catch-all takes everything; later clauses are unreachable
            }
        }
        if (!catchAll) {
            w.line("} else {");
            w.line("    throw DartRuntime.rethrow(" + caught + ");");
        }
        w.line("}");
        ctx.indent(-1);
    }

    /**
     * Whether values of {@code t} are Java throwables here: the dart:core errors, the
     * framework's own error types, and application classes whose superclass chain reaches
     * one. Anything else a Dart program throws travels in a DartThrown.
     */
    private boolean isThrowableType(TypeRef t) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String n = t.name;
        while (n != null && seen.add(n)) {
            if (CORE_ERRORS.containsKey(n) || n.equals("Error") || n.equals("Exception")
                    || n.endsWith("Error") && stubs.isStubClass(n)
                    || n.endsWith("Exception") && stubs.isStubClass(n)) {
                return true;
            }
            ClassDecl c = program.classes.get(n);
            if (c == null || c.superclass == null) {
                return false;
            }
            n = c.superclass.name;
        }
        return false;
    }

    /**
     * An operand of && or ||: a dynamic one is checked to be a bool at run time, as Dart
     * does. Emitted as is it was `Object && boolean`, which javac rejects. Java's && and
     * || still short-circuit, so the right operand is evaluated only when Dart would.
     */
    private String logicalOperand(Out o, Ctx ctx) {
        if (o.type == null || isDynamicType(o.type) || (o.type.is("bool") && o.type.nullable)) {
            ctx.importClass("dart.runtime.DartRuntime");
            return "DartRuntime.dynBool(" + o.code + ")";
        }
        return paren(o.code);
    }

    /**
     * A class mixing in ChangeNotifier holds its own listener list. The runtime's default
     * kept them in a shared map keyed weakly by the notifier, but a listener closing over
     * the notifier is held strongly there and leads back to the key, so a model with such a
     * listener could never be collected unless someone called dispose.
     */
    /**
     * Runtime types that Dart code may extend but that the runtime implements as a Java
     * interface, because Dart code also mixes them in. {@code class C extends ChangeNotifier}
     * is then exactly {@code class C with ChangeNotifier}: the stub declares no state a
     * superclass constructor could take, so rewriting the clause before emission lets the
     * mixin path (own listener list, interface super calls) handle both spellings.
     */
    private static final java.util.Set<String> INTERFACE_SUPERCLASSES =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "com.codename1.flutter.foundation.ChangeNotifier"));

    private void superclassesAsMixins() {
        for (Library lib : program.libraries) {
            stubs.viewFrom(importedPackages(lib), program.classes.keySet());
            for (ClassDecl c : lib.classes) {
                if (c.superclass == null || c.isMixin) {
                    continue;
                }
                ClassDecl app = program.resolveClass(c.superclass.name, lib);
                if (app != null && classVisibleFrom(app, lib)) {
                    continue;
                }
                Ast.ClassDecl sc = stubs.classes.get(c.superclass.name);
                if (sc != null && INTERFACE_SUPERCLASSES.contains(sc.javaName)) {
                    c.mixins.add(0, c.superclass);
                    c.superclass = null;
                }
            }
        }
    }

    private void emitChangeNotifierState(TypeRef mixin, StringBuilder body) {
        Ast.ClassDecl sc = stubs.classes.get(mixin.name);
        if (sc == null || !"com.codename1.flutter.foundation.ChangeNotifier".equals(sc.javaName)) {
            return;
        }
        body.append("    private final java.util.List<dart.runtime.Funcs.VoidFunc0> changeNotifierListeners =\n")
                .append("            new java.util.ArrayList<dart.runtime.Funcs.VoidFunc0>();\n\n")
                .append("    @Override\n")
                .append("    public java.util.List<dart.runtime.Funcs.VoidFunc0> changeNotifierListeners$() {\n")
                .append("        return changeNotifierListeners;\n    }\n\n");
    }

    /**
     * Dart's {@code ListMixin} and {@code IterableMixin} declare {@code toString()}
     * ({@code [1, 2]} and {@code (1, 2)}). The stubs map to Java interfaces, which cannot
     * supply it, so the applying class gets one unless it declares its own.
     */
    private void emitMixinToString(ClassDecl c, TypeRef mixin, StringBuilder body) {
        Ast.ClassDecl sc = stubs.classes.get(mixin.name);
        String open;
        String close;
        if (sc != null && "dart.collection.ListMixin".equals(sc.javaName)) {
            open = "[";
            close = "]";
        } else if (sc != null && "dart.collection.IterableMixin".equals(sc.javaName)) {
            open = "(";
            close = ")";
        } else {
            return;
        }
        for (MethodDecl m : c.methods) {
            if ("toString".equals(m.name) && !m.isStatic) {
                return;
            }
        }
        body.append("    @Override\n    public String toString() {\n        return $mixinToString(\"")
                .append(open).append("\", \"").append(close).append("\");\n    }\n\n");
    }

    /** dart:core error constructors -> dart-runtime classes. */
    private static final Map<String, String> CORE_ERRORS = new LinkedHashMap<String, String>();

    static {
        CORE_ERRORS.put("Exception", "dart.core.DartException");
        CORE_ERRORS.put("StateError", "dart.core.StateError");
        CORE_ERRORS.put("ArgumentError", "dart.core.ArgumentError");
        CORE_ERRORS.put("FormatException", "dart.core.FormatException");
        CORE_ERRORS.put("UnsupportedError", "dart.core.UnsupportedError");
        CORE_ERRORS.put("UnimplementedError", "dart.core.UnimplementedError");
        CORE_ERRORS.put("RangeError", "dart.core.RangeError");
        CORE_ERRORS.put("ConcurrentModificationError", "dart.core.ConcurrentModificationError");
        CORE_ERRORS.put("TypeError", "dart.core.TypeError");
        CORE_ERRORS.put("NoSuchMethodError", "dart.core.NoSuchMethodError");
        CORE_ERRORS.put("AssertionError", "dart.core.AssertionError");
    }

    /**
     * {@code State<T>} lifecycle methods that the framework overrides but that are not part
     * of the minimal {@code State} stub surface (initState/dispose/setState/build are). They
     * resolve as inherited {@code void} calls on any {@code State}-typed receiver (typically a
     * {@code super.<lifecycle>()} call), so a subclass can chain {@code super}.
     */
    private static final java.util.Set<String> STATE_LIFECYCLE = new java.util.HashSet<String>(java.util.Arrays.asList(
            "didChangeDependencies", "didUpdateWidget", "deactivate", "activate", "reassemble"));

    /** Known function typedefs: name -> [param types..., return type]. */
    private static final Map<String, TypeRef[]> TYPEDEFS = new LinkedHashMap<String, TypeRef[]>();

    static {
        TYPEDEFS.put("VoidCallback", new TypeRef[] {TypeRef.VOID});
        // (T value) -> void, the standard Flutter value-change callback
        TYPEDEFS.put("ValueChanged", new TypeRef[] {TypeRef.DYNAMIC, TypeRef.VOID});
        // (T value) -> void
        TYPEDEFS.put("ValueSetter", new TypeRef[] {TypeRef.DYNAMIC, TypeRef.VOID});
        // () -> T
        TYPEDEFS.put("ValueGetter", new TypeRef[] {TypeRef.DYNAMIC});
        // () -> void, the tap-gesture callback
        TYPEDEFS.put("GestureTapCallback", new TypeRef[] {TypeRef.VOID});
        TYPEDEFS.put("WidgetBuilder", new TypeRef[] {new TypeRef("BuildContext"), new TypeRef("Widget")});
        TYPEDEFS.put("IndexedWidgetBuilder", new TypeRef[] {new TypeRef("BuildContext"), TypeRef.INT, new TypeRef("Widget")});
        // (BuildContext, BoxConstraints) -> Widget, for LayoutBuilder
        TYPEDEFS.put("LayoutWidgetBuilder", new TypeRef[] {new TypeRef("BuildContext"), new TypeRef("BoxConstraints"), new TypeRef("Widget")});
        // (BuildContext) -> List<PopupMenuEntry<T>>, erased to Object return, for PopupMenuButton
        TYPEDEFS.put("PopupMenuItemBuilder", new TypeRef[] {new TypeRef("BuildContext"), TypeRef.DYNAMIC});
        // value-change callbacks (transpiler-internal typedef names used in stubs)
        TYPEDEFS.put("StringCallback", new TypeRef[] {TypeRef.STRING, TypeRef.VOID});
        TYPEDEFS.put("BoolCallback", new TypeRef[] {TypeRef.BOOL, TypeRef.VOID});
        TYPEDEFS.put("DoubleCallback", new TypeRef[] {TypeRef.DOUBLE, TypeRef.VOID});
        TYPEDEFS.put("IntCallback", new TypeRef[] {TypeRef.INT, TypeRef.VOID});
        TYPEDEFS.put("DynamicCallback", new TypeRef[] {TypeRef.DYNAMIC, TypeRef.VOID});
        // (int index) -> E, for List.generate's element generator
        TYPEDEFS.put("IndexedGenerator", new TypeRef[] {TypeRef.INT, TypeRef.DYNAMIC});
        // (NavigatorState, Object?) -> String, for RestorableRouteFuture.onPresent
        TYPEDEFS.put("RoutePresentationCallback", new TypeRef[] {new TypeRef("NavigatorState"), TypeRef.DYNAMIC, TypeRef.STRING});
        // (BuildContext, Widget?) -> Widget, for AnimatedBuilder.builder
        TYPEDEFS.put("TransitionBuilder", new TypeRef[] {new TypeRef("BuildContext"), new TypeRef("Widget"), new TypeRef("Widget")});
        // (AnimationStatus) -> void, for Animation.addStatusListener
        TYPEDEFS.put("AnimationStatusListener", new TypeRef[] {new TypeRef("AnimationStatus"), TypeRef.VOID});
        // (DateTime) -> void, for CupertinoDatePicker.onDateTimeChanged
        TYPEDEFS.put("DateTimeCallback", new TypeRef[] {new TypeRef("DateTime"), TypeRef.VOID});
        // (Duration) -> void, for CupertinoTimerPicker.onTimerDurationChanged
        // dart:core `typedef Comparator<T> = int Function(T a, T b)`
        TYPEDEFS.put("Comparator", new TypeRef[] {TypeRef.DYNAMIC, TypeRef.DYNAMIC, TypeRef.INT});
        TYPEDEFS.put("DurationCallback", new TypeRef[] {new TypeRef("Duration"), TypeRef.VOID});
        // (Set<MaterialState>) -> Color, for MaterialStateProperty/WidgetStateProperty.resolveWith
        TYPEDEFS.put("MaterialPropertyResolver",
                new TypeRef[] {TypeRef.of("Set", new TypeRef("MaterialState")), new TypeRef("Color")});
    }

    /**
     * The function-type signature ({@code [paramTypes..., returnType]}) of a typedef,
     * whether a built-in ({@link #TYPEDEFS}) or a user-declared function-type alias, or
     * {@code null} if {@code name} is not a (function-type) typedef.
     */
    /**
     * Fills a parameterized typedef signature's {@code dynamic} placeholders with the supplied
     * type arguments in order (built-in typedefs like ValueChanged use {@code dynamic} for their
     * type parameter). Returns a fresh array; the shared TYPEDEFS entries are never mutated.
     */
    private TypeRef[] substituteTypedefTypeArgs(TypeRef[] sig, List<TypeRef> args) {
        if (sig == null || args == null || args.isEmpty()) {
            return sig;
        }
        TypeRef[] out = new TypeRef[sig.length];
        int ai = 0;
        for (int i = 0; i < sig.length; i++) {
            if (sig[i] != null && sig[i].is("dynamic") && args.size() == 1) {
                // one type variable, used for every placeholder: Comparator<T> is (T, T) -> int
                out[i] = args.get(0);
            } else if (sig[i] != null && sig[i].is("dynamic") && ai < args.size()) {
                out[i] = args.get(ai++);
            } else {
                out[i] = sig[i];
            }
        }
        return out;
    }

    /** A user typedef by name, else a stub library's function typedef. */
    private Ast.TypedefDecl typedefDecl(String name) {
        Ast.TypedefDecl td = program.typedefs.get(name);
        return td != null || name == null ? td : stubs.typedefs.get(name);
    }

    private TypeRef[] typedefSig(String name) {
        Ast.TypedefDecl td = typedefDecl(name);
        if (td != null && td.returnType != null) {
            TypeRef[] sig = new TypeRef[td.paramTypes.size() + 1];
            for (int i = 0; i < td.paramTypes.size(); i++) {
                sig[i] = td.paramTypes.get(i);
            }
            sig[td.paramTypes.size()] = td.returnType;
            if (!td.typeParams.isEmpty()) {
                // a type parameter nested inside a component (`Future<List<T>>`) has no
                // binding here; it must not reach Java as an undeclared `T`
                for (int i = 0; i < sig.length; i++) {
                    if (sig[i] != null && !td.typeParams.contains(sig[i].name)) {
                        sig[i] = substTypeParams(sig[i], td.typeParams, java.util.Collections.<TypeRef>emptyList());
                    }
                }
            }
            return sig;
        }
        return TYPEDEFS.get(name);
    }

    /**
     * A generic user function typedef's signature with its type parameters replaced by
     * name, however deeply nested (`Future<({List<T> items})> Function(int)`); a type
     * parameter without an argument becomes dynamic. Null when not such a typedef.
     */
    private TypeRef[] userTypedefSig(String name, List<TypeRef> args) {
        Ast.TypedefDecl td = typedefDecl(name);
        if (td == null || td.returnType == null || td.typeParams.isEmpty()) {
            return null;
        }
        TypeRef[] sig = new TypeRef[td.paramTypes.size() + 1];
        for (int i = 0; i < td.paramTypes.size(); i++) {
            sig[i] = substTypeParams(td.paramTypes.get(i), td.typeParams, args);
        }
        sig[td.paramTypes.size()] = substTypeParams(td.returnType, td.typeParams, args);
        return sig;
    }

    private static TypeRef substTypeParams(TypeRef t, List<String> names, List<TypeRef> args) {
        if (t == null) {
            return null;
        }
        int at = names.indexOf(t.name);
        if (at >= 0 && t.args.isEmpty()) {
            TypeRef a = at < args.size() ? args.get(at) : TypeRef.DYNAMIC;
            if (t.nullable && !a.nullable) {
                TypeRef n = new TypeRef(a.name);
                n.args.addAll(a.args);
                n.funcParams = a.funcParams;
                n.funcReturn = a.funcReturn;
                n.nullable = true;
                return n;
            }
            return a;
        }
        TypeRef c = new TypeRef(t.name);
        c.nullable = t.nullable;
        c.line = t.line;
        c.col = t.col;
        for (TypeRef a : t.args) {
            c.args.add(substTypeParams(a, names, args));
        }
        if (t.funcParams != null) {
            c.funcParams = new ArrayList<TypeRef>();
            for (TypeRef a : t.funcParams) {
                c.funcParams.add(substTypeParams(a, names, args));
            }
        }
        c.funcReturn = substTypeParams(t.funcReturn, names, args);
        c.funcRequired = t.funcRequired;
        c.funcNamed = t.funcNamed;
        return c;
    }

    /**
     * The parameters of a call to a generic function with its type parameters bound -- to
     * the call's type arguments, else to what the arguments' static types imply, else
     * dynamic -- so a list literal or a closure argument is typed against the caller's
     * scope instead of naming the callee's type variables. {@code bound} receives the
     * bindings (null where nothing was inferred).
     */
    private List<Param> bindGenericParams(List<String> tps, List<Param> params, Call c, List<TypeRef> bound, Ctx ctx) {
        Map<String, TypeRef> m = new LinkedHashMap<String, TypeRef>();
        for (int i = 0; i < tps.size() && i < c.typeArgs.size(); i++) {
            m.put(tps.get(i), c.typeArgs.get(i));
        }
        if (m.isEmpty()) {
            int pos = 0;
            for (Param p : params) {
                if (p.named) {
                    continue;
                }
                if (pos >= c.args.positional.size()) {
                    break;
                }
                Expr a = c.args.positional.get(pos++);
                if (a instanceof Lambda && p.type != null && p.type.funcParams != null) {
                    Lambda l = (Lambda) a;
                    for (int i = 0; i < l.params.size() && i < p.type.funcParams.size(); i++) {
                        TypeRef lt = l.params.get(i).type;
                        if (lt != null && !lt.is("var")) {
                            unifyTypeVars(p.type.funcParams.get(i), lt, tps, m);
                        }
                    }
                } else if (a instanceof ListLit && ((ListLit) a).elementType == null) {
                    TypeRef el = ((ListLit) a).elements.isEmpty() ? null
                            : structuredElementTypeQuiet(((ListLit) a).elements, ctx);
                    if (el != null) {
                        unifyTypeVars(p.type, TypeRef.of("List", el), tps, m);
                    }
                } else if (!(a instanceof Lambda)) {
                    unifyTypeVars(p.type, inferExprTypeQuiet(a, ctx), tps, m);
                }
            }
        }
        List<TypeRef> args = new ArrayList<TypeRef>();
        for (String tp : tps) {
            bound.add(m.get(tp));
            args.add(m.containsKey(tp) ? m.get(tp) : TypeRef.DYNAMIC);
        }
        List<Param> out = new ArrayList<Param>();
        for (Param p : params) {
            Param q = new Param();
            q.name = p.name;
            q.named = p.named;
            q.optional = p.optional;
            q.required = p.required;
            q.defaultValue = p.defaultValue;
            q.isThis = p.isThis;
            q.isSuper = p.isSuper;
            q.type = substTypeParams(p.type, tps, args);
            out.add(q);
        }
        return out;
    }

    private static void unifyTypeVars(TypeRef p, TypeRef a, List<String> tps, Map<String, TypeRef> m) {
        if (p == null || a == null || a.is("dynamic") || a.is("Null") || a.is("var")) {
            return;
        }
        if (tps.contains(p.name) && p.args.isEmpty()) {
            if (!m.containsKey(p.name)) {
                TypeRef b = new TypeRef(a.name);
                b.args.addAll(a.args);
                b.funcParams = a.funcParams;
                b.funcReturn = a.funcReturn;
                m.put(p.name, b);
            }
            return;
        }
        if (p.name != null && p.name.equals(a.name) && p.args.size() == a.args.size()) {
            for (int i = 0; i < p.args.size(); i++) {
                unifyTypeVars(p.args.get(i), a.args.get(i), tps, m);
            }
        } else if ((p.is("Iterable") || p.is("List")) && p.args.size() == 1
                && (a.is("List") || a.is("Set") || a.is("Iterable")) && a.args.size() == 1) {
            unifyTypeVars(p.args.get(0), a.args.get(0), tps, m);
        }
    }

    /** Whether {@code name} is a function-type typedef (built-in or user-declared). */
    private boolean isFunctionTypedef(String name) {
        return typedefSig(name) != null;
    }

    /** Whether a value of this type is directly invocable (a bare {@code Function} or a function typedef). */
    private boolean isFunctionValued(TypeRef t) {
        return t != null && (t.is("Function") || typedefSig(t.name) != null);
    }

    /** The result type produced by invoking a function-valued {@link TypeRef} (VOID or DYNAMIC when unknown). */
    private TypeRef funcResultType(TypeRef t) {
        if (t != null && t.funcParams != null && t.funcReturn != null) {
            // An inline function type (`int Function(int)?`) carries its own signature.
            // Answered as dynamic, `print(o.f?.call(x))` took the void path and printed
            // nothing: the callback ran and its result was dropped.
            return t.funcReturn.is("void") ? TypeRef.VOID : t.funcReturn;
        }
        TypeRef[] sig = t == null ? null
                : t.args.isEmpty() ? null : userTypedefSig(t.name, t.args);
        if (sig == null && t != null) {
            sig = typedefSig(t.name);
        }
        if (sig != null) {
            TypeRef r = sig[sig.length - 1];
            return r.is("void") ? TypeRef.VOID : r;
        }
        return TypeRef.DYNAMIC;
    }

    /**
     * `super.m` inside {@code mixin M on Base}. The mixin is a Java interface, whose default
     * methods cannot name the applying class's superclass, so the call goes through a bridge,
     * {@code $super$m}, that M declares and every class applying M implements as
     * {@code super.m(...)}. Arguments and result travel untyped; the bridge body coerces them
     * exactly as a direct super call in the class would. Keyed by member name; the value is
     * the positional argument count, or -1 for a getter.
     */
    private final java.util.Map<ClassDecl, LinkedHashMap<String, Integer>> onMixinSuperCache =
            new java.util.HashMap<ClassDecl, LinkedHashMap<String, Integer>>();

    private LinkedHashMap<String, Integer> onMixinSupers(ClassDecl mx) {
        LinkedHashMap<String, Integer> found = onMixinSuperCache.get(mx);
        if (found == null) {
            found = new LinkedHashMap<String, Integer>();
            if (mx.isMixin && !mx.mixinOn.isEmpty()) {
                java.util.Set<Object> seen = java.util.Collections.newSetFromMap(
                        new java.util.IdentityHashMap<Object, Boolean>());
                for (MethodDecl m : mx.methods) {
                    collectSuperUses(m, found, seen);
                }
            }
            onMixinSuperCache.put(mx, found);
        }
        return found;
    }

    /** Walks AST nodes reflectively for `super.x` and positional `super.x(...)`. */
    private static void collectSuperUses(Object node, LinkedHashMap<String, Integer> out, java.util.Set<Object> seen) {
        if (node == null || !seen.add(node)) {
            return;
        }
        if (node instanceof java.util.Collection) {
            for (Object o : (java.util.Collection<?>) node) {
                collectSuperUses(o, out, seen);
            }
            return;
        }
        if (!node.getClass().getName().startsWith(Ast.class.getName())) {
            return;
        }
        if (node instanceof Assign && ((Assign) node).lhs instanceof PropertyGet
                && ((PropertyGet) ((Assign) node).lhs).target instanceof SuperExpr
                && !"??=".equals(((Assign) node).op)) {
            // `super.x = v` (or `super.x += v`): a setter bridge, `set$x`, taking the value.
            // Only a compound assignment reads the getter as well.
            Assign as = (Assign) node;
            out.put("set$" + ((PropertyGet) as.lhs).name, 1);
            if (!"=".equals(as.op)) {
                out.put("get$" + ((PropertyGet) as.lhs).name, -1);
            }
            collectSuperUses(as.rhs, out, seen);
            return;
        }
        if (node instanceof PropertyGet && ((PropertyGet) node).target instanceof SuperExpr) {
            out.put("get$" + ((PropertyGet) node).name, -1);
        } else if (node instanceof Call && ((Call) node).target instanceof SuperExpr
                && ((Call) node).args.named.isEmpty() && ((Call) node).name != null) {
            out.put(((Call) node).name, ((Call) node).args.positional.size());
        }
        for (Class<?> k = node.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()
                        || f.getType() == String.class || f.getType() == TypeRef.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    collectSuperUses(f.get(node), out, seen);
                } catch (IllegalAccessException | RuntimeException ex) {
                    // unreadable field: nothing to collect from it
                }
            }
        }
    }

    /**
     * Named per mixin ({@code $super$Loud$m}): with {@code extends B with M1, M2} both `on B`,
     * M2's super.m is M1.m while M1's is B.m, so one shared bridge could not answer both.
     */
    private static String superBridgeName(ClassDecl mx, String key) {
        return "$super$" + mx.name.replace('$', '_') + "$" + key.replace('$', '_');
    }

    private static boolean usesSuper(MethodDecl m) {
        LinkedHashMap<String, Integer> found = new LinkedHashMap<String, Integer>();
        collectSuperUses(m, found, java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<Object, Boolean>()));
        return !found.isEmpty();
    }

    private static String superBridgeSignature(ClassDecl mx, String key, int argc) {
        StringBuilder sb = new StringBuilder(superBridgeName(mx, key)).append('(');
        for (int i = 0; i < argc; i++) {
            sb.append(i > 0 ? ", " : "").append("Object $a").append(i);
        }
        return sb.append(')').toString();
    }

    private Out onMixinSuperBridge(String name, boolean getter, Args args, Ctx ctx) {
        ClassDecl mx = ctx.currentClass;
        if (name == null || mx == null || !mx.isMixin || mx.mixinOn.isEmpty() || ctx.extensionSelfType != null) {
            return null;
        }
        String key = getter ? "get$" + name : name;
        Integer argc = onMixinSupers(mx).get(key);
        if (argc == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("this.").append(superBridgeName(mx, key)).append('(');
        for (int i = 0; i < argc; i++) {
            Out a = emitExpr(args.positional.get(i), null, ctx);
            sb.append(i > 0 ? ", " : "").append(a.code);
        }
        Out raw = new Out(sb.append(')').toString(), TypeRef.DYNAMIC);
        TypeRef rt = onSuperMemberType(mx, name, getter, -1, ctx);
        if (rt != null && !rt.is("void") && !rt.is("dynamic") && !rt.is("var")) {
            return new Out("((" + javaType(rt, true, ctx) + ") " + raw.code + ")", rt);
        }
        return raw;
    }

    /**
     * `super.x = v` (or a compound `super.x += v`) inside {@code mixin M on Base}: the value
     * is computed here and handed to the {@code set$x} bridge, which the applying class
     * implements as the real super assignment. The expression's value is the value assigned.
     */
    private Out onMixinSuperSet(Assign a, Ctx ctx) {
        ClassDecl mx = ctx.currentClass;
        if (!(a.lhs instanceof PropertyGet) || !(((PropertyGet) a.lhs).target instanceof SuperExpr)
                || mx == null || !mx.isMixin || mx.mixinOn.isEmpty() || ctx.extensionSelfType != null
                || "??=".equals(a.op)) {
            return null;
        }
        String name = ((PropertyGet) a.lhs).name;
        String key = "set$" + name;
        if (onMixinSupers(mx).get(key) == null) {
            return null;
        }
        TypeRef vt = onSuperMemberType(mx, name, true, -1, ctx);
        Expr value = a.rhs;
        if (!"=".equals(a.op)) {
            Binary b = new Binary();
            b.left = a.lhs;
            b.op = a.op.substring(0, a.op.length() - 1);
            b.right = a.rhs;
            value = b;
        }
        Out v = emitExpr(value, vt, ctx);
        String code = "this." + superBridgeName(mx, key) + "(" + v.code + ")";
        if (vt != null && !vt.is("void") && !vt.is("dynamic") && !vt.is("var")) {
            return new Out("((" + javaType(vt, true, ctx) + ") " + code + ")", vt);
        }
        return new Out(code, v.type);
    }

    /**
     * The declared type of member n of the first program class an `on` mixin is declared
     * on: its return (getter) type, or with {@code param >= 0} that parameter's type. Null
     * when unknown or generic in the on-type's own type parameters.
     */
    private TypeRef onSuperMemberType(ClassDecl mx, String n, boolean getter, int param, Ctx ctx) {
        for (TypeRef on : mx.mixinOn) {
            ClassDecl pc = program.resolveClass(on.name, ctx.library());
            for (int guard = 0; pc != null && guard < 32; guard++) {
                TypeRef t = null;
                if (getter) {
                    MethodDecl g = pc.getter(n);
                    FieldDecl f = pc.field(n);
                    t = g != null ? g.returnType : f != null ? f.type : null;
                } else {
                    MethodDecl m = pc.method(n);
                    if (m != null) {
                        t = param < 0 ? m.returnType : param < m.params.size() ? m.params.get(param).type : null;
                    }
                }
                if (t != null) {
                    return t.name != null && pc.typeParams.contains(t.name) ? null : t;
                }
                pc = pc.superclass != null ? program.resolveClass(pc.superclass.name, ctx.library()) : null;
            }
        }
        return null;
    }

    /**
     * `class C extends B with M`: Dart's linearization puts M's members above B's, but a
     * Java class method always beats an interface default, so B.greet would win over
     * M.greet. For every member a mixin declares that the superclass chain (or an earlier
     * mixin) also has, and C does not declare itself, C gets an override forwarding to
     * {@code M.super.greet(...)}. Mixins are walked last first, so the last one wins, and
     * a pair of mixins declaring one member no longer leaves Java with conflicting defaults.
     * Named and optional parameters, generic methods and setters are forwarded too.
     */
    private void emitMixinForwarders(ClassDecl c, StringBuilder body, Ctx ctx) {
        if (c.isMixin || c.mixins.isEmpty()) {
            return;
        }
        java.util.Set<String> done = new HashSet<String>();
        for (MethodDecl own : c.methods) {
            done.add((own.isSetter ? "set$" : own.isGetter ? "get$" : "") + own.name);
        }
        for (FieldDecl f : c.fields) {
            done.add("get$" + f.name);
        }
        ClassDecl sup = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        for (int mi = c.mixins.size() - 1; mi >= 0; mi--) {
            TypeRef mref = c.mixins.get(mi);
            ClassDecl mx = program.resolveClass(mref.name, ctx.library());
            if (mx == null || !mx.isMixin) {
                continue;
            }
            for (MethodDecl m : mx.methods) {
                if (m.isStatic || m.isAbstract || m.name.startsWith("$") || !methodWitnesses(m).isEmpty()) {
                    continue;
                }
                String key = (m.isSetter ? "set$" : m.isGetter ? "get$" : "") + m.name;
                if (done.contains(key) || !(superChainHas(sup, m) || earlierMixinHas(c, mi, m, ctx))) {
                    continue;
                }
                done.add(key);
                Method mm = new Method();
                mm.name = m.name;
                mm.isGetter = m.isGetter;
                mm.isSetter = m.isSetter;
                mm.isOverride = true;
                mm.typeParams.addAll(m.typeParams);
                mm.typeParamBounds.putAll(m.typeParamBounds);
                mm.returnType = m.returnType == null ? null
                        : substTypeParams(m.returnType, mx.typeParams, mref.args);
                Expr call;
                if (m.isGetter) {
                    PropertyGet pg = new PropertyGet();
                    pg.target = new SuperExpr();
                    pg.name = m.name;
                    call = pg;
                } else {
                    Call cl = new Call();
                    cl.target = new SuperExpr();
                    cl.name = m.name;
                    for (Param p : m.params) {
                        // named and optional parameters keep the mixin's own defaults: the
                        // forwarder is the class's override, and in Dart the mixin's
                        // declaration (defaults included) is the one the class has
                        Param np = new Param();
                        np.name = p.name;
                        np.named = p.named;
                        np.optional = p.optional;
                        np.required = p.required;
                        np.defaultValue = p.defaultValue;
                        np.type = p.type == null ? null : substTypeParams(p.type, mx.typeParams, mref.args);
                        mm.params.add(np);
                        Ident id = new Ident();
                        id.name = p.name;
                        if (p.named) {
                            NamedArg na = new NamedArg();
                            na.name = p.name;
                            na.value = id;
                            cl.args.named.add(na);
                        } else {
                            cl.args.positional.add(id);
                        }
                    }
                    call = cl;
                    if (m.isSetter && m.params.size() == 1) {
                        // `set v(x)`: the forwarder assigns through the mixin's setter
                        PropertyGet pg = new PropertyGet();
                        pg.target = new SuperExpr();
                        pg.name = m.name;
                        Assign as = new Assign();
                        as.lhs = pg;
                        as.op = "=";
                        as.rhs = cl.args.positional.get(0);
                        call = as;
                    } else if (m.isSetter) {
                        continue;
                    }
                }
                mm.exprBody = call;
                String code = emitMethodLike(mm, ctx, false);
                // another pass (two plain mixins declaring one method) may already have copied
                // the last mixin's body into the class under the same Java signature
                int hs = code.indexOf("public ");
                int he = hs < 0 ? -1 : code.indexOf('{', hs);
                if (he > hs && body.indexOf(code.substring(hs, he)) >= 0) {
                    continue;
                }
                String raw = javaClassType(mref, ctx);
                int lt = raw.indexOf('<');
                raw = lt > 0 ? raw.substring(0, lt) : raw;
                // the body was emitted as a plain super access; aim it at the mixin's default
                int sp = he < 0 ? -1 : code.indexOf("super.", he);
                if (sp < 0) {
                    continue;
                }
                body.append(code, 0, sp).append(raw).append('.').append(code.substring(sp));
            }
        }
    }

    /** Whether class k declares a member of m's kind (method, getter or setter) and name. */
    private static boolean declaresLike(ClassDecl k, MethodDecl m) {
        if (m.isGetter || m.isSetter) {
            if (k.field(m.name) != null) {
                return true;
            }
            for (MethodDecl d : k.methods) {
                if (d.name.equals(m.name) && d.isGetter == m.isGetter && d.isSetter == m.isSetter) {
                    return true;
                }
            }
            return false;
        }
        return k.method(m.name) != null;
    }

    private boolean superChainHas(ClassDecl sup, MethodDecl m) {
        for (int guard = 0; sup != null && guard < 64; guard++) {
            if (declaresLike(sup, m)) {
                return true;
            }
            sup = sup.superclass != null ? superDeclOf(sup, sup.superclass) : null;
        }
        return false;
    }

    private boolean earlierMixinHas(ClassDecl c, int mi, MethodDecl m, Ctx ctx) {
        for (int i = 0; i < mi; i++) {
            ClassDecl o = program.resolveClass(c.mixins.get(i).name, ctx.library());
            if (o != null && o.isMixin && declaresLike(o, m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Java name of the nearest mixin before index mi in c's `with` list that declares a
     * concrete member for bridge key (`get$x` or a method name), or null when the super
     * access reaches the superclass.
     */
    private String previousMixinDeclaring(ClassDecl c, int mi, String key, Ctx ctx) {
        boolean getter = key.startsWith("get$");
        String n = getter ? key.substring(4) : key;
        for (int j = mi - 1; j >= 0; j--) {
            ClassDecl o = program.resolveClass(c.mixins.get(j).name, ctx.library());
            if (o == null || !o.isMixin) {
                continue;
            }
            MethodDecl d = getter ? o.getter(n) : o.method(n);
            if (d != null && !d.isAbstract && !d.isStatic) {
                String raw = javaClassType(c.mixins.get(j), ctx);
                int lt = raw.indexOf('<');
                return lt > 0 ? raw.substring(0, lt) : raw;
            }
        }
        return null;
    }

    /** Implements, in class c, the super bridges of every `on` mixin it applies. */
    private void emitOnMixinSuperBridges(ClassDecl c, StringBuilder body, Ctx ctx) {
        if (c.isMixin || c.superclass == null) {
            return;
        }
        java.util.Set<String> done = new HashSet<String>();
        for (int mi = 0; mi < c.mixins.size(); mi++) {
            ClassDecl mx = program.resolveClass(c.mixins.get(mi).name, ctx.library());
            if (mx == null || !mx.isMixin) {
                continue;
            }
            for (java.util.Map.Entry<String, Integer> e : onMixinSupers(mx).entrySet()) {
                String sig = superBridgeSignature(mx, e.getKey(), e.getValue());
                String prev = previousMixinDeclaring(c, mi, e.getKey(), ctx);
                if (!done.add(sig)) {
                    continue;
                }
                ctx.pushScope();
                ctx.pushWriter(2);
                Expr call;
                boolean setter = e.getKey().startsWith("set$") && e.getValue() == 1;
                if (e.getValue() < 0) {
                    PropertyGet pg = new PropertyGet();
                    pg.target = new SuperExpr();
                    pg.name = e.getKey().substring(4);
                    call = pg;
                } else if (setter) {
                    // `super.x = v`: the bridge assigns the superclass's setter and answers v
                    String sn = e.getKey().substring(4);
                    TypeRef pt = onSuperMemberType(mx, sn, true, -1, ctx);
                    Ident id = new Ident();
                    if (pt != null && !pt.is("dynamic") && !pt.is("var")) {
                        ctx.writer().line(javaType(pt, true, ctx) + " $b0 = (" + javaType(pt, true, ctx) + ") $a0;");
                        ctx.declare("$b0", pt);
                        id.name = "$b0";
                    } else {
                        ctx.declare("$a0", TypeRef.DYNAMIC);
                        id.name = "$a0";
                    }
                    PropertyGet pg = new PropertyGet();
                    pg.target = new SuperExpr();
                    pg.name = sn;
                    Assign as = new Assign();
                    as.lhs = pg;
                    as.op = "=";
                    as.rhs = id;
                    ExprStmt st = new ExprStmt();
                    st.expr = as;
                    emitStatement(st, ctx);
                    ctx.writer().line("return " + id.name + ";");
                    String inner = ctx.popWriter();
                    ctx.popScope();
                    body.append("    @Override\n    public Object ").append(sig).append(" {\n")
                            .append(inner).append("    }\n\n");
                    continue;
                } else {
                    Call cl = new Call();
                    cl.target = new SuperExpr();
                    cl.name = e.getKey();
                    for (int i = 0; i < e.getValue(); i++) {
                        TypeRef pt = onSuperMemberType(mx, e.getKey(), false, i, ctx);
                        Ident id = new Ident();
                        if (pt != null && !pt.is("dynamic") && !pt.is("var")) {
                            // the bridge takes Object; narrow to the declared parameter type
                            ctx.writer().line(javaType(pt, true, ctx) + " $b" + i + " = ("
                                    + javaType(pt, true, ctx) + ") $a" + i + ";");
                            ctx.declare("$b" + i, pt);
                            id.name = "$b" + i;
                        } else {
                            ctx.declare("$a" + i, TypeRef.DYNAMIC);
                            id.name = "$a" + i;
                        }
                        cl.args.positional.add(id);
                    }
                    call = cl;
                }
                Out o = emitExpr(call, null, ctx);
                int sp = prev == null ? -1 : o.code.indexOf("super.");
                if (sp >= 0) {
                    // Dart's linearization: the mixin applied before this one is the super
                    o = new Out(o.code.substring(0, sp) + prev + "." + o.code.substring(sp), o.type, o.fromError);
                }
                if (o.type != null && o.type.is("void")) {
                    ctx.writer().line(o.code + ";");
                    ctx.writer().line("return null;");
                } else {
                    ctx.writer().line("return " + o.code + ";");
                }
                String inner = ctx.popWriter();
                ctx.popScope();
                body.append("    @Override\n    public Object ").append(sig).append(" {\n")
                        .append(inner).append("    }\n\n");
            }
        }
    }

    /**
     * `super.m` in a class with no superclass but mixins (`class C with A, B`):
     * Dart's linearization sends it to the last mixin that declares `m`, which
     * Java reaches as that interface's default method, `B.super.m()`. A first
     * `with` entry that is a plain class is the Java superclass, so plain
     * `super` names it. Null when the class has a superclass or no mixin
     * declares the member.
     */
    private Out superThroughMixin(String member, Ctx ctx) {
        ClassDecl c = ctx.currentClass;
        if (member == null || c == null || c.superclass != null || c.mixins.isEmpty() || c.isMixin) {
            return null;
        }
        for (int i = c.mixins.size() - 1; i >= 0; i--) {
            TypeRef mixRef = c.mixins.get(i);
            ClassDecl mx = program.classes.get(mixRef.name);
            if (mx != null && stubs.isStubClass(mixRef.name) && !classVisibleFrom(mx, c.ownerLibrary)) {
                mx = null;
            }
            if (mx == null) {
                // A runtime mixin (ListMixin, MapMixin...) is a Java interface too.
                if (stubs.findMethod(mixRef.name, member, false) != null
                        || stubs.findMethod(mixRef.name, member, true) != null) {
                    return new Out(stubSimpleName(mixRef.name, ctx) + ".super", mixRef);
                }
                continue;
            }
            boolean declares = mx.method(member) != null || mx.getter(member) != null || mx.field(member) != null;
            if (!declares) {
                continue;
            }
            if (i == 0 && !mx.isMixin && mx.superclass == null && mx.mixins.isEmpty()) {
                return new Out("super", mixRef);
            }
            return new Out(javaClassName(mx) + ".super", mixRef);
        }
        return null;
    }

    /** Dart identifiers that are Java reserved words; escaped with a trailing underscore. */
    private static final java.util.Set<String> JAVA_KEYWORDS = new java.util.HashSet<String>(java.util.Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null"));

    /**
     * The Java method name for a Dart member/named-parameter identifier: a
     * Dart name that collides with a Java reserved word (e.g. {@code package})
     * is escaped with a trailing underscore. Hand-written runtime setters must
     * use the same escaped name.
     */
    static String javaMethodName(String dartName) {
        if (dartName == null) {
            return null;
        }
        if (JAVA_KEYWORDS.contains(dartName)) {
            return dartName + "_";
        }
        // A Java keyword that is NOT reserved in Dart (public, native, char, ...)
        // can be a Dart identifier both bare and with trailing underscores
        // (Flutter's Icons has public and public_). Bare maps to one underscore,
        // so each existing trailing underscore gains one more: the mapping is
        // injective and public/public_ never meet. Dart-reserved words (class,
        // try, ...) cannot appear bare, so their `_` forms keep their own name.
        int end = dartName.length();
        while (end > 0 && dartName.charAt(end - 1) == '_') {
            end--;
        }
        if (end > 0 && end < dartName.length()) {
            String base = dartName.substring(0, end);
            if (JAVA_KEYWORDS.contains(base) && !DART_RESERVED.contains(base)) {
                return dartName + "_";
            }
        }
        return dartName;
    }

    /** Dart reserved words: never a bare Dart identifier. */
    private static final java.util.Set<String> DART_RESERVED = new java.util.HashSet<String>(java.util.Arrays.asList(
            "assert", "break", "case", "catch", "class", "const", "continue", "default", "do", "else",
            "enum", "extends", "false", "final", "finally", "for", "if", "in", "is", "new", "null",
            "rethrow", "return", "super", "switch", "this", "throw", "true", "try", "var", "void",
            "while", "with"));

    /** True when the identifier is one or more underscores and nothing else. */
    private static boolean isAllUnderscores(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * The legal Java identifier for an arbitrary Dart name emitted as a Java
     * name. Folds in the reserved-word escape ({@link #javaMethodName}) and
     * additionally rewrites Dart's all-underscore wildcard/placeholder names
     * ({@code _}, {@code __}, ...) — a bare {@code _} is a reserved keyword in
     * Java 9+ — by appending a trailing underscore ({@code _}->{@code __},
     * {@code __}->{@code ___}), which is always legal. Deterministic: the same
     * Dart name always maps to the same Java name so declarations and
     * references stay consistent.
     */
    static String javaIdent(String dartName) {
        if (isAllUnderscores(dartName)) {
            return dartName + "_";
        }
        return javaMethodName(dartName);
    }

    /** Whether the most recently emitted lambda had a {@code void}-typed expression body. */
    private boolean lastLambdaVoid;
    /// The last block lambda was emitted with a `return null;` tail (see emitLambda), so
    /// it is value-shaped in Java whatever its Dart body returns; a caller choosing the
    /// lambda's SAM by its body shape must pick the value form.
    private boolean lastLambdaNullTail;
    /**
     * What the most recently emitted lambda yields: its arrow body's type, or the value
     * types of its block body's returns (empty when it returns nothing).
     */
    private TypeRef lastLambdaExprType;
    private List<TypeRef> lastLambdaReturns;
    /**
     * Whether the most recently emitted lambda is a single-expression arrow body
     * (no statements had to be lifted out of it) whose body, AS EMITTED JAVA, is
     * itself a legal statement -- a method call, assignment, {@code new X(...)} or
     * increment/decrement. Per JLS 15.27.3 such a body is compatible with both a
     * void-returning and a value-returning functional-interface descriptor
     * regardless of the body's own static type: a Dart binary expression with a
     * dynamic operand lowers to a call like {@code DartRuntime.dynBinary(...)},
     * which returns Object but is syntactically still a bare statement, so
     * {@code (v) -> DartRuntime.dynBinary(...)} is exactly as ambiguous, passed to
     * an overloaded method, as a body that is genuinely void. {@link #pinnedHandler}
     * reads this to decide whether a value-typed callback still needs a
     * disambiguating cast; a block body (one or more lifted statements, or any
     * body with an explicit {@code { ... }}) is never ambiguous this way, since it
     * is void-compatible only in the absence of any value-returning {@code return}.
     */
    private boolean lastLambdaExprIsStatementShaped;

    private Out emitLambda(Lambda l, TypeRef expected, Ctx ctx) {
        if (expected != null && expected.funcParams == null && expected.funcReturn == null
                && (expected.is("Object") || expected.is("Function") || isDynamicType(expected))) {
            // A bare `Function` slot (a tear-off adapter's optional parameter) erases to
            // Object too.
            // A closure handed to an Object/dynamic slot (a named argument through a
            // full-arity tear-off local, `dec(s, reviver: (k, v) => ..)`): Object is not a
            // functional interface, so bind it to its own Funcs SAM first.
            // A lambda's own Out type is a bare Function, so its SAM is built from its
            // parameters (declared or dynamic) and a dynamic result.
            TypeRef own = new TypeRef("Function");
            own.funcParams = new ArrayList<TypeRef>();
            for (Param p : l.params) {
                own.funcParams.add(p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type);
            }
            own.funcReturn = TypeRef.DYNAMIC;
            Out o = emitLambda(l, own, ctx);
            if (!o.code.startsWith("((")) {
                return new Out("((" + javaType(own, false, ctx) + ") " + o.code + ")", own);
            }
            return o;
        }
        Out adapted = optionalParamLambda(l, expected, ctx);
        if (adapted != null) {
            return adapted;
        }
        lastLambdaVoid = false;
        lastLambdaNullTail = false;
        lastLambdaExprIsStatementShaped = false;
        // typedef-typed target position gives untyped lambda params real types
        // `Comparator<int>` binds the typedef's placeholders, so `(a, b) => a.compareTo(b)`
        // has int parameters and an int result.
        // A generic user typedef (`Shaper<T> = String Function(int, T)`) binds its
        // parameters by name, so `Shaper<List<int>>` gives the lambda a List<int> T.
        TypeRef[] sigTypes = expected != null && !expected.args.isEmpty()
                ? userTypedefSig(expected.name, expected.args) : null;
        if (sigTypes == null && expected != null) {
            sigTypes = substituteTypedefTypeArgs(typedefSig(expected.name), expected.args);
        }
        // An inline function type target (`Widget Function(BuildContext, T, Widget?)`,
        // e.g. a generic stub builder whose element type was just substituted) likewise
        // supplies concrete param types: flatten funcParams + funcReturn into a sig.
        if (sigTypes == null && expected != null && expected.funcParams != null) {
            sigTypes = new TypeRef[expected.funcParams.size() + 1];
            for (int i = 0; i < expected.funcParams.size(); i++) {
                sigTypes[i] = expected.funcParams.get(i);
            }
            sigTypes[sigTypes.length - 1] =
                    expected.funcReturn != null ? expected.funcReturn : TypeRef.DYNAMIC;
        }
        boolean outerAsync = ctx.inAsyncBody;
        boolean outerBareNull = ctx.bareReturnIsNull;
        ctx.bareReturnIsNull = false;   // a nested lambda's `return;` is its own
        boolean outerAsyncLambda = ctx.asyncLambda;
        TypeRef outerReturn = ctx.methodReturnType;
        boolean outerNarrowInt = ctx.narrowReturnToInt;
        ctx.inAsyncBody = false;
        ctx.asyncLambda = false;
        // Thread the lambda's SAM return type so `return`/switch-expression arms inside the body
        // resolve against it (e.g. an onGenerateRoute builder whose switch arms are Route values).
        TypeRef lambdaReturn = null;
        if (sigTypes != null && sigTypes.length > 0) {
            TypeRef r = sigTypes[sigTypes.length - 1];
            if (r != null && !r.is("void") && !r.is("dynamic")) {
                lambdaReturn = r;
            }
        }
        // An async closure whose caller takes its Future is emitted as an async method is:
        // it returns a Future, and what its body throws completes that Future with the
        // error instead of escaping the call. One passed as a void callback (onPressed:
        // () async {...}) deliberately keeps the plain body: nobody can observe its Future,
        // and a handler per callback costs native code size in every app. The divergence
        // is accepted: a throw there escapes the call synchronously, where Dart would
        // report it as an uncaught async error.
        boolean asyncValue = l.isAsync && lambdaReturn != null
                && (lambdaReturn.is("Future") || lambdaReturn.is("FutureOr"));
        if (asyncValue) {
            TypeRef fut = new TypeRef("Future");
            fut.args.add(lambdaReturn.args.isEmpty() ? TypeRef.DYNAMIC : lambdaReturn.arg(0));
            lambdaReturn = fut;
            ctx.inAsyncBody = true;
            ctx.asyncLambda = true;
            ctx.importClass("dart.async.Future");
        }
        ctx.methodReturnType = lambdaReturn;
        ctx.narrowReturnToInt = false;
        // A break/continue cannot target a loop/switch outside the lambda body.
        List<String> savedBreaks = new ArrayList<String>(ctx.breakTargets);
        ctx.breakTargets.clear();
        ctx.pushScope();
        StringBuilder sig = new StringBuilder("(");
        for (int i = 0; i < l.params.size(); i++) {
            Param p = l.params.get(i);
            TypeRef pt = p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
            if (pt.is("dynamic") && sigTypes != null && i < sigTypes.length - 1) {
                pt = sigTypes[i];
            }
            if (i > 0) {
                sig.append(", ");
            }
            sig.append(ctx.declareShadowSafe(p.name, pt));
        }
        sig.append(')');
        String head = sig.toString();
        String code;
        List<TypeRef> outerReturns = ctx.lambdaReturns;
        List<TypeRef> returns = new ArrayList<TypeRef>();
        TypeRef exprType = null;
        if ((l.isAsyncStar || l.isSyncStar) && l.body != null) {
            // A generator closure: the lambda returns the generator, whose body is a nested
            // lambda fed the AsyncStar.Sink (async*) or the result list (sync*), as a
            // generator method's helper is.
            boolean async = l.isAsyncStar;
            TypeRef gen = lambdaReturn;
            TypeRef elem = gen != null && (gen.is(async ? "Stream" : "Iterable")) && !gen.args.isEmpty()
                    ? gen.arg(0) : TypeRef.DYNAMIC;
            String ej = javaType(elem, true, ctx);
            String target = ctx.newTemp();
            String savedSink = ctx.asyncStarSink;
            String savedList = ctx.syncStarList;
            TypeRef savedElem = ctx.syncStarElem;
            ctx.asyncStarSink = async ? target : null;
            ctx.syncStarList = async ? null : target;
            ctx.syncStarElem = elem;
            ctx.inAsyncBody = async;
            ctx.methodReturnType = TypeRef.VOID;
            ctx.pushWriter(ctx.currentIndent() + 2);
            ctx.lambdaReturns = returns;
            emitStatements(l.body, ctx);
            ctx.lambdaReturns = outerReturns;
            String body = ctx.popWriter();
            ctx.asyncStarSink = savedSink;
            ctx.syncStarList = savedList;
            ctx.syncStarElem = savedElem;
            String ind = indentStr(ctx.currentIndent());
            // What an untyped closure local infers its SAM from: the generator's type.
            exprType = TypeRef.of(async ? "Stream" : "Iterable", elem);
            if (async) {
                ctx.importClass("dart.async.AsyncStar");
                code = head + " -> AsyncStar.<" + ej + ">asyncStar((AsyncStar.Sink<" + ej + "> " + target
                        + ") -> {\n" + body + ind + "    })";
            } else {
                ctx.importClass("dart.core.DartList");
                ctx.importClass("dart.core.DartIterable");
                code = head + " -> DartIterable.syncStar((DartList<" + ej + "> " + target + ") -> {\n"
                        + body + ind + "    })";
            }
        } else if (asyncValue) {
            code = head + " -> {\n" + emitAsyncLambdaBody(l, lambdaReturn, ctx)
                    + indentStr(ctx.currentIndent()) + "}";
        } else if (l.body != null) {
            // `on<E>((event, emit) { emit(s); })`: a block that returns nothing, passed where
            // the function type returns dynamic -- a value-returning Funcs.FuncN in Java.
            // Its `return;` and its end both answer null. The body sits under `if (true)`,
            // which javac treats as able to complete normally, so the trailing return is
            // never "unreachable" after a body that always returns or throws.
            // The same holds for a generic return (`T Function()` of `T run<T>(...)`): Dart
            // infers T = void there, and the Java SAM is still a value-returning FuncN<T>.
            boolean nullTail = sigTypes != null && sigTypes.length > 0
                    && sigTypes[sigTypes.length - 1] != null
                    && (sigTypes[sigTypes.length - 1].is("dynamic")
                    || sigTypes[sigTypes.length - 1].is("FutureOr")
                    || sigTypes[sigTypes.length - 1].is("Object")
                    || isTypeVariable(sigTypes[sigTypes.length - 1], ctx))
                    && !returnsValue(l.body);
            boolean outerNullReturn = ctx.bareReturnIsNull;
            ctx.bareReturnIsNull = nullTail;
            Ctx.Writer w = ctx.pushWriter(ctx.currentIndent() + (nullTail ? 2 : 1));
            ctx.lambdaReturns = returns;
            emitStatements(l.body, ctx);
            ctx.lambdaReturns = outerReturns;
            String body = ctx.popWriter();
            ctx.bareReturnIsNull = outerNullReturn;
            lastLambdaNullTail = nullTail;
            if (nullTail) {
                String in = indentStr(ctx.currentIndent() + 1);
                body = in + "if (true) {\n" + body + in + "}\n" + in + "return null;\n";
            }
            code = head + " -> {\n" + body + indentStr(ctx.currentIndent()) + "}";
        } else {
            Ctx.Writer w = ctx.pushWriter(ctx.currentIndent() + 1);
            // Emit the arrow body against the lambda's SAM return type so a switch-expression /
            // conditional body unifies its arms to that type (e.g. an onGenerateRoute arrow whose
            // switch arms are Route values).
            // A void callback's arrow body is a statement: `() => model.count = 5` keeps
            // the plain setter call rather than a value form a void lambda cannot return.
            Expr savedStmt = ctx.statementExpr;
            if (lambdaReturn == null || lambdaReturn.is("void")) {
                ctx.statementExpr = l.exprBody;
            }
            Out o = emitExpr(l.exprBody, lambdaReturn, ctx);
            ctx.statementExpr = savedStmt;
            if (lambdaReturn != null && o.type != null && o.type.is("num")
                    && (lambdaReturn.is("double") || lambdaReturn.is("int"))) {
                // num arithmetic is a Number; a lambda typed to return a double or int
                // (fold's combiner, a double-returning builder) needs that primitive.
                o = new Out(coerce(o, lambdaReturn, ctx), lambdaReturn);
            }
            if (lambdaReturn != null && o.type != null && o.type.is("dynamic") && !lambdaReturn.nullable
                    && (lambdaReturn.is("int") || lambdaReturn.is("double") || lambdaReturn.is("bool")
                    || lambdaReturn.is("String"))) {
                // `(a, b) => a.compareTo(b)` on erased operands answers a dynamic value where
                // the SAM returns an int: Dart checks that at run time, and so does the cast.
                o = new Out("((" + javaType(lambdaReturn, true, ctx) + ") (Object) " + o.code + ")", lambdaReturn);
            }
            if (lambdaReturn != null && lambdaReturn.is("int") && !lambdaReturn.nullable && o.type != null
                    && o.type.is("int") && o.code.endsWith(")") && o.code.contains(".compareTo(")) {
                // A Comparable's compareTo is Java's int, which a Long-returning SAM
                // (List.sort's comparator) cannot box; widen it to Dart's int first.
                o = new Out("((long) " + o.code + ")", o.type);
            }
            String lifted = ctx.popWriter();
            exprType = o.type;
            lastLambdaVoid = o.type != null && o.type.is("void");
            lastLambdaExprIsStatementShaped = lifted.isEmpty() && isJavaStatementExpr(o.code);
            boolean genericVoid = o.type != null && o.type.is("void") && lambdaReturn != null
                    && isTypeVariable(lambdaReturn, ctx);
            if (genericVoid) {
                // `() => sink.write(x)` passed as `T Function()`: Dart infers T = void, but
                // the Java SAM returns a value, so the statement is followed by `return null`.
                code = head + " -> {\n" + lifted + indentStr(ctx.currentIndent() + 1)
                        + statementize(o.code) + ";\n" + indentStr(ctx.currentIndent() + 1)
                        + "return null;\n" + indentStr(ctx.currentIndent()) + "}";
            } else if (lifted.isEmpty()) {
                code = head + " -> " + o.code;
            } else if (o.type.is("void") || o.type.is("Null")) {
                code = head + " -> {\n" + lifted + indentStr(ctx.currentIndent() + 1)
                        + statementize(o.code) + ";\n" + indentStr(ctx.currentIndent()) + "}";
            } else {
                code = head + " -> {\n" + lifted + indentStr(ctx.currentIndent() + 1)
                        + "return " + o.code + ";\n" + indentStr(ctx.currentIndent()) + "}";
            }
        }
        ctx.popScope();
        ctx.breakTargets.clear();
        ctx.breakTargets.addAll(savedBreaks);
        ctx.inAsyncBody = outerAsync;
        ctx.bareReturnIsNull = outerBareNull;
        ctx.asyncLambda = outerAsyncLambda;
        ctx.methodReturnType = outerReturn;
        ctx.narrowReturnToInt = outerNarrowInt;
        // Last, so a lambda nested in this one cannot overwrite what this one yields.
        lastLambdaExprType = exprType;
        lastLambdaReturns = returns;
        return new Out(code, new TypeRef("Function"));
    }

    /**
     * The function type of an untyped async closure local, decided before its body is
     * emitted because it decides how the body is emitted: Future of dynamic when the local
     * is called for its value (`final r = f();`, `await f()`), so the body runs as an async
     * method does; void when every call discards the result, which keeps the closure
     * passable as a void callback. Null for a zero-arg closure nobody takes a value from,
     * which keeps its VoidFunc0 form, and for anything that is not an async closure.
     */
    private TypeRef asyncClosureType(Lambda l, String name, Ctx ctx) {
        if (!l.isAsync) {
            return null;
        }
        boolean valued = ctx.valueCalledLocals.contains(name);
        if (!valued && l.params.isEmpty()) {
            return null;
        }
        TypeRef fn = new TypeRef("Function");
        fn.funcParams = new ArrayList<TypeRef>();
        for (Param p : l.params) {
            fn.funcParams.add(p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type);
        }
        if (valued) {
            TypeRef fut = new TypeRef("Future");
            fut.args.add(TypeRef.DYNAMIC);
            fn.funcReturn = fut;
        } else {
            fn.funcReturn = TypeRef.VOID;
        }
        return fn;
    }

    /**
     * The body of an async closure whose Future is used, bracketed as an async method's is
     * so a throw becomes the Future's error. Returns the body text at one indent past the
     * current one.
     */
    private String emitAsyncLambdaBody(Lambda l, TypeRef futureType, Ctx ctx) {
        ctx.pushWriter(ctx.currentIndent() + 1);
        Ctx.Writer w = ctx.writer();
        String err = ctx.newTemp();
        w.line("try {");
        ctx.indent(1);
        if (l.body != null) {
            emitStatements(l.body, ctx);
            if (!endsWithJump(l.body)) {
                w.line("return Future.value(null);");
            }
        } else {
            TypeRef inner = futureType.arg(0);
            Out o = emitExpr(l.exprBody, isDynamicType(inner) ? null : inner, ctx);
            w.line("return " + asyncReturnValue(o, inner, ctx) + ";");
        }
        ctx.indent(-1);
        w.line("} catch (RuntimeException " + err + ") {");
        ctx.indent(1);
        w.line("return Future.error(" + err + ");");
        ctx.indent(-1);
        w.line("}");
        return ctx.popWriter();
    }

    /**
     * What an async body returns for {@code o}: a Future passes through, any other value is
     * wrapped. In an async closure typed Future of dynamic, a Future of a narrower type is
     * cast raw, as Java generics are invariant.
     */
    private String asyncReturnValue(Out o, TypeRef inner, Ctx ctx) {
        if (o.type != null && o.type.is("Future")) {
            if (ctx.asyncLambda && isDynamicType(inner)) {
                return "(Future) (" + o.code + ")";
            }
            return o.code;
        }
        return "Future.value(" + boxIfPrimitive(o, ctx) + ")";
    }

    /**
     * The function type of an untyped closure local, read off the lambda just emitted:
     * its declared parameter types (dynamic when untyped) and what its body yields.
     * Void only when the body yields nothing -- or when it is a call or assignment
     * whose result this body never uses: such a closure is a callback, and a value SAM
     * type would stop it being passed where a void callback is expected.
     */
    private TypeRef inferredClosureType(Lambda l, String name, Ctx ctx) {
        TypeRef fn = new TypeRef("Function");
        fn.funcParams = new ArrayList<TypeRef>();
        for (Param p : l.params) {
            fn.funcParams.add(p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type);
        }
        TypeRef ret;
        if (l.exprBody != null) {
            Expr core = stripParens(l.exprBody);
            boolean callback = core instanceof Call || core instanceof CtorCall || core instanceof Assign
                    || core instanceof IncDec || core instanceof Cascade || core instanceof AwaitExpr;
            ret = lastLambdaExprType;
            if (ret == null || ret.is("void") || (callback && !ctx.valueCalledLocals.contains(name))) {
                ret = TypeRef.VOID;
            }
        } else {
            ret = null;
            for (TypeRef r : lastLambdaReturns) {
                TypeRef rr = r == null ? TypeRef.DYNAMIC : r;
                if (ret == null) {
                    ret = rr;
                } else if (!ret.toString().equals(rr.toString())) {
                    ret = TypeRef.DYNAMIC;
                }
            }
            if (ret == null) {
                ret = TypeRef.VOID;
            }
        }
        if (ret.is("Null") || ret.is("var")) {
            ret = TypeRef.DYNAMIC;
        }
        fn.funcReturn = ret;
        return fn;
    }

    // ==================================================================
    // Calls
    // ==================================================================

    private Out emitCall(Call c, TypeRef expected, Ctx ctx) {
        Call savedFor = ctx.callExpectedFor;
        TypeRef savedExp = ctx.callExpected;
        TypeRef awaited = savedFor == c ? savedExp : null;
        ctx.callExpectedFor = c;
        ctx.callExpected = expected != null ? expected : awaited;
        try {
            return emitCallWithExpected(c, expected, ctx);
        } finally {
            ctx.callExpectedFor = savedFor;
            ctx.callExpected = savedExp;
        }
    }

    private TypeRef callExpected(Call c, Ctx ctx) {
        return ctx.callExpectedFor == c ? ctx.callExpected : null;
    }

    /** The program extension named {@code n}, preferring the current library's, or null. */
    private ClassDecl extensionNamed(String n, Ctx ctx) {
        if (n == null || ctx.lookup(n) != null || program.classes.containsKey(n)) {
            return null;
        }
        ClassDecl any = null;
        for (ClassDecl e : program.extensions) {
            if (n.equals(e.name)) {
                if (e.ownerLibrary == ctx.library()) {
                    return e;
                }
                if (any == null && !n.startsWith("_")) {
                    any = e;
                }
            }
        }
        return any;
    }

    /**
     * {@code Ext.staticMember(..)} and an explicit extension application
     * {@code Ext(receiver).member(..)}. An extension is not a class in the program tables, so
     * both fell through to "cannot resolve" (E0129/E0135). The application calls the
     * extension's static method directly, with the receiver as its {@code $self}, which is
     * also how Dart resolves it: the named extension wins even over a member of the receiver.
     */
    private Out emitExplicitExtensionCall(Call c, Ctx ctx) {
        if (c.name == null) {
            return null;
        }
        if (c.target instanceof Ident) {
            ClassDecl ext = extensionNamed(((Ident) c.target).name, ctx);
            MethodDecl m = ext == null ? null : ext.method(c.name);
            if (m != null && m.isStatic) {
                TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
                return new Out(ext.name + "." + c.name + "(" + methodArgs(m.params, c.args, ctx) + ")", rt);
            }
            return null;
        }
        if (!(c.target instanceof Call)) {
            return null;
        }
        Call app = (Call) c.target;
        if (app.target != null || app.args.positional.size() != 1 || !app.args.named.isEmpty()) {
            return null;
        }
        ClassDecl ext = extensionNamed(app.name, ctx);
        MethodDecl em = ext == null ? null : ext.method(c.name);
        if (em == null || em.isStatic || em.isGetter || em.isSetter) {
            return null;
        }
        Out recv = emitExpr(app.args.positional.get(0), null, ctx);
        TypeRef rtype = recv.type == null ? ext.extensionOn : recv.type;
        String rest = methodArgs(extensionCallbackParams(ext, em.params, rtype), c.args, ctx);
        String self = extensionSelf(recv, rtype, ext, ctx);
        return new Out(ext.name + "." + c.name + "(" + self + (rest.isEmpty() ? "" : ", " + rest) + ")",
                em.returnType == null || em.returnType.is("var") ? TypeRef.DYNAMIC : em.returnType);
    }

    private Out emitCallWithExpected(Call c, TypeRef expected, Ctx ctx) {
        // closure value invocation: f(...) where f is a local of Function type
        if (c.name == null && c.target != null) {
            Out target = emitExpr(c.target, null, ctx);
            if (target.type == null || target.type.is("dynamic") || target.type.is("Object")) {
                // `(f)()` on a dynamic value: no functional interface to call through.
                ctx.importClass("dart.runtime.Dyn");
                String a = plainArgs(c.args, ctx);
                return new Out("Dyn.invoke(" + target.code + (a.isEmpty() ? "" : ", " + a) + ")",
                        TypeRef.DYNAMIC);
            }
            return new Out(fnCall(target.code, target.type, c.args, ctx), TypeRef.DYNAMIC);
        }
        Out explicitExt = emitExplicitExtensionCall(c, ctx);
        if (explicitExt != null) {
            return explicitExt;
        }
        if (c.target == null) {
            return emitBareCall(c, expected, ctx);
        }
        // deferred import: `m.loadLibrary()` — AOT, so the library is already loaded;
        // hand back an already-completed future.
        if (c.name != null && c.name.equals("loadLibrary") && isImportPrefix(c.target, ctx)) {
            ctx.importClass("dart.async.Future");
            return new Out("Future.value(null)", TypeRef.of("Future", TypeRef.DYNAMIC));
        }
        // `prefix.fn(...)` / `prefix.Type(...)` through an `import '...' as prefix` name:
        // the invoked function or constructor lives in another user (or stub) library and
        // is globally addressable, so dispatch it as a bare (unqualified) call.
        if (c.name != null && isImportPrefix(c.target, ctx)
                && (program.functions.containsKey(c.name) || program.classes.containsKey(c.name)
                    || stubs.isStubClass(c.name) || stubs.functions.containsKey(c.name)
                    || prefixedLibraryFunction(((Ident) c.target).name, c.name, ctx))) {
            Call bare = new Call();
            bare.file = c.file;
            bare.line = c.line;
            bare.col = c.col;
            bare.name = c.name;
            bare.args = c.args;
            bare.typeArgs = c.typeArgs;
            // The prefix picks the library: a.Foo() and b.Foo() are different classes
            // when both libraries declare Foo. Dropping it let import order decide both.
            String outer = ctx.callPrefix;
            ctx.callPrefix = ((Ident) c.target).name;
            try {
                return emitBareCall(bare, expected, ctx);
            } finally {
                ctx.callPrefix = outer;
            }
        }
        // Static factory methods on the primitive numeric types — `double.parse(s)`,
        // `double.tryParse(s)`, `int.parse(s)`, `int.tryParse(s)`. The type name is not a
        // resolvable value expression, so intercept before emitting it as a target.
        if (c.name != null && c.target instanceof Ident
                && ctx.lookup(((Ident) c.target).name) == null
                && (ctx.currentClass == null || ctx.currentClass.field(((Ident) c.target).name) == null)) {
            Out prim = emitPrimitiveStaticCall(((Ident) c.target).name, c, ctx);
            if (prim != null) {
                return prim;
            }
        }
        Out target = emitExprRaw(c.target, null, ctx);
        if (c.nullAware) {
            // a?.m() - the SAME shorting a?.b gets, and it was missing here: only the
            // function-valued `call` case honoured the flag, so every other `?.m()` emitted
            // an unguarded invocation. The gallery's `_timeDilationTimer?.cancel()` is one,
            // and toggling slow motion threw a NullPointerException on the very first use
            // because that timer is null until something has been dilated.
            //
            // Lift the receiver into a temp and start a short: the call runs on the
            // non-null temp, and the temp being null shorts this and any trailing
            // selectors, exactly as Dart specifies.
            Out mat = materializeShort(target);
            String tmp = ctx.newTemp();
            ctx.writer().line("var " + tmp + " = " + mat.code + ";");
            // The arguments are evaluated only when the receiver is non-null, so any
            // statements they lift (`a?.addAll([...xs])`) go inside the guard.
            ctx.pushWriter(ctx.currentIndent() + 1);
            String outerChecked = ctx.nullCheckedTemp;
            ctx.nullCheckedTemp = tmp;
            Out called;
            try {
                called = emitMethodCallOn(
                        new Out(tmp, copyNonNull(mat.type), mat.fromError), c, ctx);
            } finally {
                ctx.nullCheckedTemp = outerChecked;
            }
            String lifted = ctx.popWriter();
            if (!lifted.isEmpty()) {
                Out guarded = guardLifted(tmp, lifted, called, ctx);
                if (guarded != null) {
                    return guarded;
                }
            }
            if (called.code == null || called.code.isEmpty()) {
                // The callee emitted its own guarded statement (the function-valued `call`
                // path does this) - there is no expression left to short.
                return called;
            }
            if (called.type != null && called.type.is("void")) {
                // A void call cannot be the value of a ternary. In statement position the
                // guard is an `if`, which is also what the result is used for: nothing.
                ctx.writer().line("if (" + tmp + " != null) { " + called.code + "; }");
                return new Out("", TypeRef.VOID);
            }
            return new Out(called.code, boxType(called.type), called.fromError, tmp);
        }
        if (target.shortGuard != null) {
            // a plain method call after a `?.` stays inside the short (a?.b.c()), and so
            // do any statements its arguments lift
            ctx.pushWriter(ctx.currentIndent() + 1);
            Out result = emitMethodCallOn(target, c, ctx);
            String lifted = ctx.popWriter();
            if (!lifted.isEmpty()) {
                Out guarded = guardLifted(target.shortGuard, lifted, result, ctx);
                if (guarded != null) {
                    return guarded;
                }
            }
            return result.withShort(target.shortGuard);
        }
        return emitMethodCallOn(target, c, ctx);
    }

    /**
     * Static numeric parse factories on {@code int} / {@code double}, whose receiver is a
     * bare type name rather than a value. Returns null for any other name.
     *
     * <p>Both go through the runtime's DString helpers. tryParse used to become the same
     * Long.parseLong as parse, so malformed input -- exactly what a tryParse guard is
     * written for -- threw NumberFormatException instead of answering null; and parse
     * threw Java's exception rather than Dart's FormatException, which an
     * {@code on FormatException} clause does not catch.</p>
     */
    private Out emitPrimitiveStaticCall(String typeName, Call c, Ctx ctx) {
        boolean isInt = typeName.equals("int");
        boolean isDouble = typeName.equals("double");
        if ((!isInt && !isDouble) || c.args.positional.isEmpty()) {
            return null;
        }
        if (!c.name.equals("parse") && !c.name.equals("tryParse")) {
            return null;
        }
        String arg = emitExpr(c.args.positional.get(0), TypeRef.STRING, ctx).code;
        boolean nullable = c.name.equals("tryParse");
        ctx.importClass("dart.core.DString");
        TypeRef t = new TypeRef(isInt ? "int" : "double");
        t.nullable = nullable;
        String fn = (nullable ? "tryParse" : "parse") + (isInt ? "Int" : "Double");
        if (isInt) {
            // `radix:` is the one named argument int.parse takes; it was dropped, so
            // int.parse('ff', radix: 16) failed as a decimal number.
            for (NamedArg na : c.args.named) {
                if (na.name.equals("radix")) {
                    arg += ", " + emitExpr(na.value, TypeRef.INT, ctx).code;
                }
            }
        }
        return new Out("DString." + fn + "(" + arg + ")", t);
    }

    private Out emitBareCall(Call c, TypeRef expected, Ctx ctx) {
        String n = c.name;
        // dart:core print
        if (n.equals("print")) {
            ctx.importClass("dart.runtime.DartRuntime");
            Expr arg = c.args.positional.isEmpty() ? null : c.args.positional.get(0);
            Out o = arg == null ? new Out("\"\"", TypeRef.STRING) : emitExpr(arg, null, ctx);
            return new Out("DartRuntime.print(" + o.code + ")", TypeRef.VOID);
        }
        // local closure variable
        TypeRef local = ctx.lookup(n);
        if (local != null) {
            // A function-typed local (bare `Function` or a function typedef such as
            // `LibraryLoader = Future<void> Function()`) invocation yields the typedef's
            // result type, so a chained `loader().then(...)` sees a real Future receiver.
            TypeRef ret = isFunctionValued(local) ? funcResultType(local) : TypeRef.DYNAMIC;
            // Through the local's own Java form: a shadow-renamed, boxed (captured and
            // reassigned) or late closure is not reachable by its Dart name.
            Ident callee = new Ident();
            callee.name = n;
            String recv = emitIdent(callee, null, ctx).code;
            return new Out(fnCall(recv, local, c.args, ctx), ret);
        }
        // inside an extension body, bare calls probe the receiver first
        if (ctx.extensionSelfType != null) {
            Out self = new Out("$self", ctx.extensionSelfType);
            Out probe = intrinsicCall(self, c, ctx);
            if (probe != null) {
                return probe;
            }
            Ast.MethodDecl sm = stubs.findMethod(ctx.extensionSelfType.name, n, false);
            if (sm != null) {
                return new Out("$self." + n + "(" + stubMethodArgs(sm, c.args, ctx) + ")", sm.returnType);
            }
            // an instance method of a program class (or interface) the extension is on
            ClassDecl onCls = programClass(ctx.extensionSelfType.name, ctx);
            if (onCls != null && resolveTopLevel(n, ctx) == null
                    && (ctx.currentExtension == null || ctx.currentExtension.method(n) == null)) {
                MethodDecl om = onCls.method(n);
                if (om == null) {
                    om = findMethodInHierarchy(onCls, n);
                }
                if (om != null && !om.isStatic && !om.isGetter) {
                    return emitMethodCallOn(self, c, ctx);
                }
            }
            ClassDecl extCls = ctx.currentExtension != null && ctx.currentExtension.method(n) != null
                    ? ctx.currentExtension : findUserExtension(ctx.extensionSelfType.name, n, false, ctx);
            if (extCls != null) {
                MethodDecl em = extCls.method(n);
                return new Out(extCls.name + "." + n + "($self"
                        + (c.args.positional.isEmpty() && c.args.named.isEmpty() ? "" : ", "
                        + methodArgs(em.params, c.args, ctx)) + ")",
                        em.returnType == null || em.returnType.is("var") ? TypeRef.DYNAMIC : em.returnType);
            }
        }
        // Stopwatch() — dart:core intrinsic (no-arg monotonic timer)
        if (n.equals("Stopwatch") && c.args.positional.isEmpty() && c.args.named.isEmpty()) {
            ctx.importClass("dart.core.Stopwatch");
            return new Out("new Stopwatch()", new TypeRef("Stopwatch"));
        }
        // Duration(seconds: 2, ...) — dart:core intrinsic with canonical named order
        if (n.equals("Duration")) {
            return emitDurationOf(c.args, ctx);
        }
        Out collection = emitCollectionClassCtor(c, n, ctx);
        if (collection != null) {
            return collection;
        }
        if (n.equals("identityHashCode") && c.args.positional.size() == 1 && resolveTopLevel(n, ctx) == null) {
            ctx.importClass("dart.core.CoreStatics");
            return new Out("CoreStatics.identityHashCode(" + emitExpr(c.args.positional.get(0), null, ctx).code + ")",
                    TypeRef.INT);
        }
        // dart:core exception constructors
        String coreError = CORE_ERRORS.get(n);
        if (coreError != null) {
            ctx.importClass(coreError);
            String simple = coreError.substring(coreError.lastIndexOf('.') + 1);
            String msg = c.args.positional.isEmpty() ? "\"\""
                    : emitExpr(c.args.positional.get(0), TypeRef.STRING, ctx).code;
            return new Out("new " + simple + "(" + msg + ")", new TypeRef("Exception"));
        }
        // constructor of program class
        ClassDecl pc = program.classes.get(n);
        if (pc != null) {
            return emitCtorCall(n, inferredWitnessTypeArgs(pc, c.typeArgs, expected), c.args, c, ctx);
        }
        // constructor of stub class
        if (stubs.isStubClass(n)) {
            return emitCtorCall(n, c.typeArgs, c.args, c, ctx);
        }
        // method of current class / inherited stub method
        ClassDecl cc = ctx.currentClass;
        if (cc != null) {
            MethodDecl m = cc.method(n);
            if (m == null && !cc.isMixin) {
                // a method the class inherits from a program superclass
                MethodDecl inh = findMethodInHierarchy(cc, n);
                if (inh != null && !inh.isGetter && !inh.isSetter) {
                    m = inh;
                }
            }
            if (m != null) {
                String recv = m.isStatic ? cc.name : "this";
                // An explicit `thenJob<R>(...)` keeps its type arguments: Java cannot infer
                // R from a lambda argument whose parameters name it.
                String targs = "";
                if (c.typeArgs != null && !c.typeArgs.isEmpty() && c.typeArgs.size() == m.typeParams.size()) {
                    StringBuilder tb = new StringBuilder("<");
                    for (int i = 0; i < c.typeArgs.size(); i++) {
                        if (i > 0) {
                            tb.append(", ");
                        }
                        tb.append(javaType(c.typeArgs.get(i), true, ctx));
                    }
                    targs = tb.append('>').toString();
                }
                String code = recv + "." + targs + n + "(" + withWitnesses(methodWitnessCall(m, c, callExpected(c, ctx), ctx),
                        methodArgs(m.params, c.args, ctx)) + ")";
                // `T run<T>(T Function() f)`: the result is the method's own T, bound from
                // the call (explicit type argument, an argument, or the closure's return),
                // never left as a type variable the caller's Java does not declare. Runs
                // after the arguments are emitted: the closure's return type is read from it.
                TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC
                        : bindMethodTypeParams(m.returnType, m, c, ctx);
                return new Out(code, rt);
            }
            String stubSuper = nearestStubSuper(cc);
            if (stubSuper != null) {
                Ast.MethodDecl sm = stubs.findMethod(stubSuper, n, false);
                if (sm != null && hasWitnessParam(sm)) {
                    return new Out("this." + n + "(" + witnessArgs(sm, c, ctx) + ")", sm.returnType);
                }
                if (sm != null) {
                    TypeRef rt = inheritedStubMemberReturnType(cc, n, false);
                    return new Out("this." + n + "(" + stubMethodArgs(sm, c.args, ctx) + ")",
                            rt != null ? rt : sm.returnType);
                }
            }
            // members contributed by an applied stub mixin (e.g. RestorationMixin's
            // registerForRestoration): resolved as an inherited default method. The mixin may
            // be applied by a program superclass (`class B extends A` where A extends, hence
            // mixes in, ChangeNotifier), so the superclass chain is walked too.
            for (ClassDecl k = cc; k != null; k = k.superclass != null ? superDeclOf(k, k.superclass) : null) {
                for (TypeRef mixRef : k.mixins) {
                    if (stubs.isStubClass(mixRef.name)) {
                        Ast.MethodDecl sm = stubs.findMethod(mixRef.name, n, false);
                        if (sm != null) {
                            return new Out("this." + n + "(" + stubMethodArgs(sm, c.args, ctx) + ")", sm.returnType);
                        }
                    }
                }
            }
            // bare invocation of an own (or inherited) function-typed field:
            // `onChanged(v)` where onChanged is a `void Function(...)` field.
            FieldDecl ff = cc.field(n);
            if (ff == null) {
                ff = findInheritedField(cc, n);
            }
            if (ff != null && isFunctionValued(ff.type)) {
                Out fieldRead = emitMemberGet(new Out(ff.isStatic ? cc.name : "this",
                        new TypeRef(cc.name)), n, c, ctx);
                return new Out(fnCall(fieldRead.code, ff.type, c.args, ctx),
                        funcResultType(ff.type));
            }
        }
        if (cc != null) {
            Out onSelf = mixinOnSelf(cc, n, false, ctx);
            if (onSelf != null) {
                return emitMethodCallOn(onSelf, c, ctx);
            }
        }
        // top-level function (user code)
        FunctionDecl fn = program.functions.get(n);
        if (fn != null) {
            Library owner = program.resolveFunctionOwner(n, ctx.library(), takeCallPrefix(ctx));
            // A private top-level function name can be declared in several libraries (Dart
            // privacy is library-scoped); use the RESOLVED owner's declaration so the parameter
            // list matches (e.g. crane's 2-arg `_customIconTheme` vs shrine's 1-arg one).
            FunctionDecl ownerFn = functionInLibrary(owner, n);
            if (ownerFn != null) {
                fn = ownerFn;
            }
            String cls = Program.libClassName(owner.fileName);
            String jn = n.equals("main") ? "main$" : n;
            TypeRef rt = fn.returnType == null || fn.returnType.is("var") ? TypeRef.DYNAMIC : fn.returnType;
            List<Param> ps = fn.params;
            if (!fn.typeParams.isEmpty()) {
                List<TypeRef> bound = new ArrayList<TypeRef>();
                ps = bindGenericParams(fn.typeParams, fn.params, c, bound, ctx);
                boolean all = true;
                for (TypeRef b : bound) {
                    all &= b != null;
                }
                if (all) {
                    rt = substTypeParams(rt, fn.typeParams, bound);
                }
            }
            String fcode = cls + "." + jn + "(" + withWitnesses(methodWitnessCall(fn, c, callExpected(c, ctx), ctx),
                    methodArgs(ps, c.args, ctx)) + ")";
            // Whatever of the function's own type parameters the arguments left unbound
            // (`twice(() => ++n)`) is taken from the closure, else erased: see the method case.
            return new Out(fcode, bindMethodTypeParams(rt, fn, c, ctx));
        }
        // A function value held by a top-level variable: `final Labeler fallback = _f;`
        // then `fallback(x)`. Read through the variable's own Java form, then invoke.
        if (program.topLevelVars.containsKey(n)) {
            Ident callee = new Ident();
            callee.name = n;
            callee.file = c.file;
            callee.line = c.line;
            callee.col = c.col;
            Out v = emitIdent(callee, null, ctx);
            if (v.type == null || v.type.is("dynamic") || v.type.is("Object")) {
                ctx.importClass("dart.runtime.Dyn");
                String a = plainArgs(c.args, ctx);
                return new Out("Dyn.invoke(" + v.code + (a.isEmpty() ? "" : ", " + a) + ")", TypeRef.DYNAMIC);
            }
            return new Out(fnCall(v.code, v.type, c.args, ctx),
                    isFunctionValued(v.type) ? funcResultType(v.type) : TypeRef.DYNAMIC);
        }
        // dart:core identical(a, b): unresolved before, so any use failed the build.
        if (n.equals("identical") && c.args.positional.size() == 2 && !program.functions.containsKey(n)) {
            ctx.importClass("dart.runtime.DartRuntime");
            Out a = emitExpr(c.args.positional.get(0), null, ctx);
            Out b = emitExpr(c.args.positional.get(1), null, ctx);
            // Object parameters: a primitive long/double/bool argument autoboxes.
            return new Out("DartRuntime.identical(" + a.code + ", " + b.code + ")", TypeRef.BOOL);
        }
        // stub top-level function (e.g. runApp); a dart: library's own, when its name
        // collides with another's (dart:developer's log is not dart:math's), only
        // through that library's import
        Ast.FunctionDecl sf = libraryStubFunction(n, ctx);
        if (sf == null) {
            sf = stubs.functions.get(n);
        }
        if (sf != null && sf.javaName != null) {
            int dot = sf.javaName.lastIndexOf('.');
            String cls = sf.javaName.substring(0, dot);
            String method = sf.javaName.substring(dot + 1);
            Out numeric = mathNumCall(cls, method, c, ctx);
            if (numeric != null) {
                return numeric;
            }
            ctx.importClass(cls);
            String simple = cls.substring(cls.lastIndexOf('.') + 1);
            String call = simple + "." + method + "(" + methodArgs(sf.params, c.args, ctx) + ")";
            if (!sf.typeParams.isEmpty() && sf.returnType != null) {
                // showDialog<bool>(...): bind the function's own type parameters -- to the
                // call's type arguments, else dynamic -- and cast the erased Java result to
                // match, as a generic stub method's call is.
                List<TypeRef> bound = new ArrayList<TypeRef>();
                for (int i = 0; i < sf.typeParams.size(); i++) {
                    bound.add(i < c.typeArgs.size() && !isTypeVariable(c.typeArgs.get(i), ctx)
                            ? c.typeArgs.get(i) : TypeRef.DYNAMIC);
                }
                TypeRef rt = substituteTypeParam(sf.returnType, sf.typeParams, bound);
                String jt = javaType(rt, true, ctx);
                if (jt != null && !jt.equals("Object")) {
                    return new Out("((" + jt + ") (Object) " + call + ")", rt);
                }
                return new Out(call, rt);
            }
            return new Out(call, sf.returnType);
        }
        diags.error(c, "E0135", "Cannot resolve function or constructor '" + n
                + "'. Confirm the file passes `dart analyze`, or the API is not yet implemented in the Codename One runtime.");
        return new Out("null", TypeRef.DYNAMIC, true);
    }

    /**
     * The {@code dartlib_<lib>.dart} stub function {@code n} this call reaches: through
     * its prefix when the call is {@code prefix.n(...)} and the prefix imports
     * {@code dart:<lib>}, else through an unprefixed import of {@code dart:<lib>} that
     * admits {@code n}. Null when the call reaches no such function.
     */
    private Ast.FunctionDecl libraryStubFunction(String n, Ctx ctx) {
        if (stubs.libraryFunctions.isEmpty()) {
            return null;
        }
        Ast.Library lib = ctx.library();
        if (lib == null) {
            return null;
        }
        String prefix = ctx.callPrefix;
        if (prefix != null) {
            String uri = lib.prefixImports.get(prefix);
            return uri != null && uri.startsWith("dart:") ? libraryFunction(uri.substring(5), n) : null;
        }
        for (Ast.Directive d : lib.importDirectives) {
            if (d.uri == null || !d.uri.startsWith("dart:") || !d.admits(n) || d.prefix != null) {
                continue;
            }
            Ast.FunctionDecl f = libraryFunction(d.uri.substring(5), n);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    private boolean prefixedLibraryFunction(String prefix, String n, Ctx ctx) {
        Ast.Library lib = ctx.library();
        String uri = lib == null ? null : lib.prefixImports.get(prefix);
        return uri != null && uri.startsWith("dart:") && libraryFunction(uri.substring(5), n) != null;
    }

    private Ast.FunctionDecl libraryFunction(String library, String n) {
        Map<String, Ast.FunctionDecl> m = stubs.libraryFunctions.get(library);
        return m == null ? null : m.get(n);
    }

    private Out emitMethodCallOn(Out target, Call c, Ctx ctx) {
        TypeRef tt = target.type;
        // A receiver typed with an import prefix (`intl.DateFormat`, `ui.Size`) keeps the
        // prefix in its type name; strip it so method resolution sees the real stub type.
        if (tt != null && tt.name != null && tt.name.indexOf('.') > 0) {
            tt.name = stripImportPrefix(tt.name);
        }
        // A receiver typed by a bounded type parameter (`B extends Base<T>`) has the
        // bound's members; Java erases B to Object, so view it as the bound.
        Out boundView = typeParamBoundView(target, ctx);
        if (boundView != null) {
            target = boundView;
            tt = boundView.type;
        }
        String n = c.name;
        // Cascade suppression: the receiver's type already fell to `dynamic` because a
        // diagnostic was reported for it upstream (unresolved identifier/member/etc.).
        // Accessing a method on a dynamic receiver is legal Dart (dynamic dispatch), so
        // re-diagnosing here would just spam the same root cause down the whole chain.
        if (target.fromError && tt.is("dynamic")) {
            return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")",
                    TypeRef.DYNAMIC, true);
        }
        // Genuine `dynamic` receiver: dispatch at runtime through dart.runtime.Dyn.
        if (tt != null && tt.is("dynamic")) {
            ctx.importClass("dart.runtime.Dyn");
            dynNames.add(n);
            String dc = "Dyn.call(" + target.code + ", \"" + n + "\", " + dynArgs(c.args, ctx) + ")";
            if (n.equals("compareTo") && c.args.positional.size() == 1 && c.args.named.isEmpty()) {
                // Comparable.compareTo answers an int whatever the receiver: a sort
                // comparator over erased elements still returns the SAM's Long.
                return new Out("((Long) " + dc + ")", TypeRef.INT);
            }
            return new Out(dc, TypeRef.DYNAMIC);
        }
        // static method on a class reference
        if (isClassRef(tt)) {
            String cls = tt.arg(0).name;
            // Dart's `Object.hash(a, b, ...)` -> java.util.Objects.hash(Object...).
            if (cls.equals("Object") && n.equals("hashAll") && c.args.positional.size() == 1) {
                ctx.importClass("dart.runtime.DartRuntime");
                return new Out("DartRuntime.hashAll(" + emitExpr(c.args.positional.get(0), null, ctx).code + ")",
                        TypeRef.INT);
            }
            if (cls.equals("Object") && n.equals("hash")) {
                // Through DartRuntime.hash, which hashes numbers as Dart's == compares
                // them; java.util.Objects.hash gave 1 and 1.0 different hashes.
                ctx.importClass("dart.runtime.DartRuntime");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.args.positional.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("(Object) ").append(paren(emitExpr(c.args.positional.get(i), null, ctx).code));
                }
                return new Out("DartRuntime.hash(" + sb + ")", TypeRef.INT);
            }
            // Dart's `Comparable.compare(a, b)` -> DartComparable.compare (delegates to compareTo).
            if (cls.equals("Comparable") && n.equals("compare")) {
                ctx.importClass("dart.core.DartComparable");
                String a = emitExpr(c.args.positional.get(0), null, ctx).code;
                String b = emitExpr(c.args.positional.get(1), null, ctx).code;
                return new Out("DartComparable.compare(" + a + ", " + b + ")", TypeRef.INT);
            }
            // Iterable.generate(count, [generator]) with no type arguments parses as a
            // static call on the name Iterable, which resolved to nothing; the typed form
            // (Iterable<int>.generate) is a constructor call handled with the collections.
            if (cls.equals("Iterable") && n.equals("generate") && !c.args.positional.isEmpty()) {
                ctx.importClass("dart.core.DartIterable");
                String count = emitExpr(c.args.positional.get(0), TypeRef.INT, ctx).code;
                String gen = c.args.positional.size() > 1
                        ? ", " + emitExpr(c.args.positional.get(1), new TypeRef("IndexedGenerator"), ctx).code : "";
                return new Out("DartIterable.<Object>generate(" + count + gen + ")",
                        TypeRef.of("Iterable", TypeRef.DYNAMIC));
            }
            Out coreStatic = emitCoreStaticCall(cls, c, ctx);
            if (coreStatic != null) {
                return coreStatic;
            }
            if (cls.equals("Future")) {
                ctx.importClass("dart.async.Future");
                if (n.equals("sync") || n.equals("microtask")) {
                    Out f = emitExpr(c.args.positional.get(0), inlineFuncType0(TypeRef.DYNAMIC), ctx);
                    if (n.equals("microtask")) {
                        ctx.importClass("dart.core.Duration");
                        return new Out("Future.delayed(Duration.zero, (dart.runtime.Funcs.Func0<Object>) "
                                + paren(f.code) + ")", TypeRef.of("Future", TypeRef.DYNAMIC));
                    }
                    ctx.importClass("dart.core.CoreStatics");
                    return new Out("CoreStatics.futureSync(" + f.code + ")", TypeRef.of("Future", TypeRef.DYNAMIC));
                }
                if (n.equals("error")) {
                    // Future.error(e, stackTrace): the trace travels with the error.
                    return new Out("Future.error(" + emitExpr(c.args.positional.get(0), null, ctx).code
                            + (c.args.positional.size() > 1
                                    ? ", " + emitExpr(c.args.positional.get(1), null, ctx).code : "") + ")",
                            TypeRef.of("Future", TypeRef.DYNAMIC));
                }
                if (n.equals("delayed")) {
                    String dur = emitExpr(c.args.positional.get(0), new TypeRef("Duration"), ctx).code;
                    String comp = c.args.positional.size() > 1
                            ? emitExpr(c.args.positional.get(1), null, ctx).code : null;
                    return new Out("Future.delayed(" + dur + (comp != null ? ", " + comp : "") + ")",
                            TypeRef.of("Future", TypeRef.DYNAMIC));
                }
                if (n.equals("value")) {
                    Out v = c.args.positional.isEmpty() ? new Out("null", TypeRef.NULL)
                            : emitExpr(c.args.positional.get(0), null, ctx);
                    return new Out("Future.value(" + boxIfPrimitive(v, ctx) + ")",
                            TypeRef.of("Future", v.type));
                }
                if (n.equals("wait")) {
                    Out l = emitExpr(c.args.positional.get(0), null, ctx);
                    // eagerError: and cleanUp: were dropped, so an eager wait still waited
                    // for every input and a cleanUp callback never ran.
                    String eager = null;
                    String cleanUp = null;
                    for (NamedArg na : c.args.named) {
                        if (na.name.equals("eagerError")) {
                            eager = emitExpr(na.value, TypeRef.BOOL, ctx).code;
                        } else if (na.name.equals("cleanUp")) {
                            cleanUp = pinnedHandler(na.value, emitExpr(na.value, null, ctx), true,
                                    "dart.runtime.Funcs.VoidFunc1<Object>", "dart.runtime.Funcs.VoidFunc1<Object>");
                        }
                    }
                    String args = eager == null && cleanUp == null ? l.code
                            : l.code + ", " + (eager == null ? "false" : eager) + ", "
                                    + (cleanUp == null ? "(dart.runtime.Funcs.VoidFunc1<Object>) null" : cleanUp);
                    return new Out("Future.wait(" + args + ")",
                            TypeRef.of("Future", TypeRef.of("List", TypeRef.DYNAMIC)));
                }
                diags.error(c, "E0304", "Unsupported Future member: " + n);
                return new Out("null", TypeRef.DYNAMIC);
            }
            if (stubs.isStubClass(cls)) {
                Ast.MethodDecl m = stubs.findMethod(cls, n, false);
                if (m != null && m.isStatic) {
                    Ast.ClassDecl sc = stubs.classes.get(cls);
                    Out numeric = sc == null ? null : mathNumCall(sc.javaName, n, c, ctx);
                    if (numeric != null) {
                        return numeric;
                    }
                    return stubCallOut(m, c, stubSimpleName(cls, ctx) + "." + n, ctx,
                            bindStaticClassTypeParams(m, sc, c, ctx));
                }
            }
            ClassDecl pc = program.classes.get(cls);
            if (pc != null) {
                // The Java name, which is library-prefixed when the class shares its name
                // with a stub (an app's own User beside firebase_auth's): the raw Dart name
                // then named the stub, or nothing at all.
                ClassDecl visible = program.resolveClass(cls, ctx.library());
                if (visible != null) {
                    pc = visible;
                }
                String jcls = javaClassName(pc);
                MethodDecl m = pc.method(n);
                if (m != null && m.isStatic) {
                    return new Out(jcls + "." + n + "(" + withWitnesses(methodWitnessCall(m, c,
                            callExpected(c, ctx), ctx), methodArgs(m.params, c.args, ctx)) + ")",
                            m.returnType == null ? TypeRef.DYNAMIC : m.returnType);
                }
                CtorDecl named = pc.namedCtor(n);
                if (named != null) {
                    // named (or named factory) constructor -> static factory
                    String tw = witnessArgs(pc, inferredWitnessTypeArgs(pc, c.typeArgs, callExpected(c, ctx)),
                            c, ctx);
                    String na = canonicalArgs(named, c.args, ctx);
                    return new Out(jcls + "." + javaIdent(n) + "(" + (na.isEmpty() ? stripTrailingComma(tw)
                            : tw + na) + ")", new TypeRef(cls));
                }
                if (n.equals("new")) {
                    // `Foo.new(args)`: the unnamed constructor called through its tear-off name
                    Call plain = new Call().at(c.file, c.line, c.col);
                    plain.name = cls;
                    plain.typeArgs.addAll(c.typeArgs);
                    plain.args = c.args;
                    return emitExpr(plain, null, ctx);
                }
                diags.error(c, "E0136", "Cannot resolve static member or constructor '" + cls + "." + n + "'");
                return new Out("null", TypeRef.DYNAMIC, true);
            }
            diags.error(c, "E0136", "Cannot resolve static method '" + n + "' on " + cls);
            return new Out("null", TypeRef.DYNAMIC, true);
        }
        // intrinsics
        Out intrinsic = intrinsicCall(target, c, ctx);
        if (intrinsic != null) {
            return intrinsic;
        }
        // enhanced-enum instance method: `category.displayTitle(loc)`
        Ast.EnumDecl ed = programEnum(tt.name, ctx);
        if (ed != null) {
            MethodDecl em = ed.method(n);
            if (em != null) {
                return new Out(target.code + "." + n + "(" + methodArgs(em.params, c.args, ctx) + ")",
                        em.returnType == null || em.returnType.is("var") ? TypeRef.DYNAMIC : em.returnType);
            }
        }
        // program class instance method
        ClassDecl pc = programClass(tt.name, ctx);
        if (pc != null) {
            MethodDecl m = pc.method(n);
            if (m == null) {
                m = findMethodInHierarchy(pc, n);
            }
            if (m == null) {
                Object mixM = findMixinMember(pc, n, false);
                if (mixM instanceof MethodDecl && !((MethodDecl) mixM).isGetter) {
                    m = (MethodDecl) mixM;
                }
            }
            if (m != null) {
                String callCode = target.code + "." + n + "(" + withWitnesses(methodWitnessCall(m, c,
                        callExpected(c, ctx), ctx), methodArgs(m.params, c.args, ctx)) + ")";
                TypeRef seen = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC
                        : asSeenFrom(m.returnType, m, m.typeParams, pc, tt);
                if (!m.typeParams.isEmpty() && c.typeArgs.size() == m.typeParams.size()) {
                    // `open<int>(..)`: the method's own type arguments, as written.
                    seen = substTypeParams(seen, m.typeParams, c.typeArgs);
                }
                if (pinnedCallBox(pc, m, ctx) != null) {
                    // The implementer's return as this call sees it: `shelf.open<int>()` is a
                    // Future<Crate2<Long>>, never the declaration's Future<Crate2<V>>, whose V
                    // is not in scope here. A type argument left to inference reads as dynamic.
                    TypeRef castTo = seen;
                    if (!m.typeParams.isEmpty() && c.typeArgs.size() != m.typeParams.size()) {
                        List<TypeRef> dyn = new ArrayList<TypeRef>();
                        for (int i = 0; i < m.typeParams.size(); i++) {
                            dyn.add(TypeRef.DYNAMIC);
                        }
                        castTo = substTypeParams(seen, m.typeParams, dyn);
                    }
                    callCode = "((" + javaType(castTo, true, ctx) + ") (Object) " + callCode + ")";
                }
                return new Out(callCode, seen);
            }
            // method inherited from the program class's stub superclass or a stub mixin
            // (e.g. a RestorableProperty subclass calling the inherited `dispose()`).
            String stubSuper = nearestStubSuper(pc);
            if (stubSuper != null) {
                Ast.MethodDecl sm = stubs.findMethod(stubSuper, n, false);
                if (sm != null) {
                    // `class V extends StreamView<int>`: the inherited `toList()` returns a
                    // Future<List<int>>, not the stub's bare Future<List<T>>.
                    return stubCallOut(sm, c, target.code + "." + n, ctx,
                            inheritedStubMemberReturnType(pc, n, false, tt));
                }
            }
            // a stub mixin applied by this class or by any program superclass
            // (`class B extends A` where `A with ListMixin<bool>` reaching `toList()`)
            java.util.Set<String> mixSeen = new java.util.HashSet<String>();
            for (ClassDecl mc = pc; mc != null && mixSeen.add(mc.name);
                    mc = mc.superclass != null ? superDeclOf(mc, mc.superclass) : null) {
                for (TypeRef mixRef : mc.mixins) {
                    if (stubs.isStubClass(mixRef.name)) {
                        Ast.MethodDecl sm = stubs.findMethod(mixRef.name, n, false);
                        if (sm != null) {
                            // QueueList<Result<T>> mixing ListMixin<E>: elementAt answers Result<T>
                            return stubCallOut(sm, c, target.code + "." + n, ctx,
                                    substMixinMember(sm.returnType, mixRef, mc, mc == pc ? tt : null));
                        }
                    }
                }
            }
            // invocation of a function-typed field: `obj.onTap(args)` where onTap is a
            // `void Function(...)`/callback field — read the field then invoke its SAM.
            FieldDecl ff = pc.field(n);
            if (ff == null) {
                ff = findInheritedField(pc, n);
            }
            if (ff != null && isFunctionValued(ff.type)) {
                Out fieldRead = emitMemberGet(target, n, c, ctx);
                return new Out(fnCall(fieldRead.code, ff.type, c.args, ctx),
                        funcResultType(ff.type));
            }
        }
        // SAM invocation on a function-typed value: `f.call(args)` / `f?.call(args)`
        if (n.equals("call") && isFunctionValued(tt)) {
            TypeRef ret = funcResultType(tt);
            // `f?.call()` reached through the `a?.m()` path above, which has already
            // put f in a temp and is guarding it: a second temp and test inside that
            // guard is dead code.
            if (c.nullAware && target.code.equals(ctx.nullCheckedTemp)) {
                // Typed as the guarded form below would be: a void (or erased-void)
                // callback stays a statement, which the caller's guard wraps.
                String call = fnCall(target.code, tt, c.args, ctx);
                return new Out(call, ret.is("void") || ret.is("dynamic") ? TypeRef.VOID : ret);
            }
            if (c.nullAware) {
                String tmp = ctx.newTemp();
                ctx.writer().line("var " + tmp + " = " + target.code + ";");
                // Arguments run only for a non-null callback; what they lift goes
                // inside the guard.
                ctx.pushWriter(ctx.currentIndent() + 1);
                String callExpr = fnCall(tmp, tt, c.args, ctx);
                String lifted = ctx.popWriter();
                // A void (or untyped-`Function`, whose void return was erased) callback is
                // fire-and-forget in statement position: guard with an `if` so a void SAM
                // isn't illegally used as a ternary value.
                if (ret.is("void") || ret.is("dynamic")) {
                    if (!lifted.isEmpty()) {
                        return guardLifted(tmp, lifted, new Out(callExpr, TypeRef.VOID), ctx);
                    }
                    ctx.writer().line("if (" + tmp + " != null) { " + callExpr + "; }");
                    return new Out("", TypeRef.VOID);
                }
                if (!lifted.isEmpty()) {
                    Out guarded = guardLifted(tmp, lifted, new Out(callExpr, ret), ctx);
                    if (guarded != null) {
                        return materializeShort(guarded);
                    }
                }
                return new Out("(" + tmp + " == null ? null : " + callExpr + ")", boxType(ret));
            }
            return new Out(fnCall(target.code, tt, c.args, ctx), ret);
        }
        // stub instance method
        String stubName = stubs.isStubClass(tt.name) ? tt.name : null;
        if (stubName != null || tt.is("State")) {
            Ast.MethodDecl m = stubs.findMethod(tt.name, n, false);
            if (m != null) {
                // Narrow a generic method result (declared `T evaluate(...)`) to the
                // concrete type argument the receiver instantiates.
                TypeRef sub = stubMemberReturnType(tt, n, false);
                return stubCallOut(withReceiverCallbackTypes(m, tt), c, target.code + "." + n, ctx, sub);
            }
        }
        // State<T> lifecycle overrides not present on the minimal State stub surface
        // (typically a `super.<lifecycle>()` chain); resolve as inherited void calls.
        if (tt.is("State") && STATE_LIFECYCLE.contains(n)) {
            return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")", TypeRef.VOID);
        }
        // extension methods
        ClassDecl extCls = findUserExtension(tt.name, n, false, ctx);
        extCls = mostSpecificExtension(extCls, tt, n, false);
        if (extCls != null) {
            MethodDecl em = extCls.method(n);
            String rest = methodArgs(extensionCallbackParams(extCls, em.params, tt), c.args, ctx);
            String self = extensionSelf(target, tt, extCls, ctx);
            return new Out(extCls.name + "." + n + "(" + self
                    + (rest.isEmpty() ? "" : ", " + rest) + ")",
                    em.returnType == null || em.returnType.is("var") ? TypeRef.DYNAMIC : em.returnType);
        }
        // extension method contributed by a stub (go_router's `extension GoRouterHelper on
        // BuildContext { void go(String location, {Object? extra}); }`): a static method of
        // the extension's @JavaName class taking the receiver first, then the call's
        // arguments laid out as for any stub method.
        Ast.ClassDecl stubExtM = stubs.findExtension(tt.name, n, false);
        if (stubExtM != null) {
            Ast.MethodDecl em = extensionMember(stubExtM, n, false);
            if (em != null) {
                String callee = stubExtensionSimpleName(stubExtM, ctx) + "." + n;
                // `extension X<K, V> on Map<K, V>`: the receiver's type arguments stand in
                // for the extension's type parameters while the arguments are emitted, so
                // `m.map((k, v) => ...)` types v as the map's value type, not a bare V.
                java.util.Map<String, TypeRef> extSubst = new java.util.HashMap<String, TypeRef>();
                TypeRef on = stubExtM.extensionOn;
                if (on != null && on.args.size() == tt.args.size()) {
                    for (int i = 0; i < on.args.size(); i++) {
                        TypeRef a = on.args.get(i);
                        if (a.args.isEmpty() && stubExtM.typeParams.contains(a.name)
                                && !em.typeParams.contains(a.name)) {
                            extSubst.put(a.name, tt.args.get(i));
                        }
                    }
                }
                List<TypeRef> savedParamTypes = new ArrayList<TypeRef>();
                for (Param ep : em.params) {
                    savedParamTypes.add(ep.type);
                    if (!extSubst.isEmpty()) {
                        ep.type = substFunctionType(ep.type, extSubst);
                    }
                }
                Out o;
                try {
                    o = stubCallOut(em, c, callee, ctx);
                } finally {
                    for (int i = 0; i < em.params.size(); i++) {
                        em.params.get(i).type = savedParamTypes.get(i);
                    }
                }
                String prefix = callee + "(";
                if (o.code.startsWith(prefix)) {
                    String rest = o.code.substring(prefix.length());
                    return new Out(prefix + target.code + (rest.startsWith(")") ? "" : ", ") + rest,
                            o.type, o.fromError);
                }
            }
        }
        if (tt.is("Stopwatch")) {
            // start / stop / reset are void no-arg controls
            return new Out(target.code + "." + n + "()", TypeRef.VOID);
        }
        // `.then(cb)` on a void receiver: some controller actions the runtime models as void
        // return Flutter's synchronously-completing TickerFuture (e.g.
        // `controller.reverse().then(...)`). Everything is AOT-synchronous, so run the receiver
        // for its effect, then continue on an already-completed future.
        if (n.equals("then") && tt.is("void") && !c.args.positional.isEmpty()) {
            ctx.importClass("dart.async.Future");
            if (target.code != null && !target.code.isEmpty()) {
                ctx.writer().line(statementize(target.code) + ";");
            }
            Out cb = emitExpr(c.args.positional.get(0), null, ctx);
            return new Out("Future.value(null).then(" + cb.code + ")",
                    TypeRef.of("Future", TypeRef.DYNAMIC));
        }
        // Object protocol
        if (n.equals("toString") && c.args.positional.isEmpty()) {
            ctx.importClass("dart.runtime.DartRuntime");
            return new Out("DartRuntime.str(" + target.code + ")", TypeRef.STRING);
        }
        if (n.equals("compareTo") && c.args.positional.size() == 1 && c.args.named.isEmpty()
                && isTypeVariable(tt, ctx)) {
            // `a.compareTo(b)` on a type variable bounded by Comparable (`E extends
            // Comparable<E>`): the erased receiver is an Object, so compare at run time.
            ctx.importClass("dart.core.DartComparable");
            return new Out("DartComparable.compare(" + target.code + ", "
                    + emitExpr(c.args.positional.get(0), null, ctx).code + ")", TypeRef.INT);
        }
        diags.error(c, "E0137", "Cannot resolve method '" + n + "' on type " + tt
                + ". Confirm the file passes `dart analyze`, or the API is not yet implemented in the Codename One runtime.");
        return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")", TypeRef.DYNAMIC, true);
    }

    /** Core-type method table (String / List / Map / int / double). */
    private Out intrinsicCall(Out target, Call c, Ctx ctx) {
        TypeRef tt = target.type;
        String n = c.name;
        List<Expr> pos = c.args.positional;
        if (tt.is("Stream") && n.equals("cast") && pos.isEmpty() && c.typeArgs.size() == 1) {
            // Stream.cast<R>() checks each event, so the runtime takes R as a witness the
            // way whereType does; an erased type variable has none and stays unchecked.
            TypeRef sub = c.typeArgs.get(0);
            // Generic code (a library's cast<T>()) is common, so a type variable is not
            // diagnosed as whereType's is: it keeps the old unchecked behaviour.
            boolean dyn = sub.is("dynamic") || sub.is("var") || sub.is("Object") || isTypeVariable(sub, ctx);
            String jt = dyn ? null : javaType(sub, true, ctx);
            if (jt == null) {
                return new Out(target.code + ".cast()", TypeRef.of("Stream", sub));
            }
            // A class literal names the raw type: List<int> is checked as a List.
            String raw = jt.indexOf('<') >= 0 ? jt.substring(0, jt.indexOf('<')) : jt;
            return new Out(target.code + ".<" + jt + ">cast(" + raw + ".class, " + sub.nullable + ")",
                    TypeRef.of("Stream", sub));
        }
        if (tt.is("String")) {
            ctx.importClass("dart.core.DString");
            if (n.equals("substring")) {
                String args = target.code;
                for (Expr e : pos) {
                    args += ", " + emitExpr(e, TypeRef.INT, ctx).code;
                }
                return new Out("DString.substring(" + args + ")", TypeRef.STRING);
            }
            if (n.equals("contains")) {
                // contains(pattern, startIndex): the start was dropped, so the search
                // always began at 0.
                String start = pos.size() > 1 ? ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code : "";
                return new Out("DString.contains(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + start + ")", TypeRef.BOOL);
            }
            if (n.equals("split")) {
                return new Out("DString.split(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.of("List", TypeRef.STRING));
            }
            if (n.equals("indexOf")) {
                String args = target.code;
                for (Expr e : pos) {
                    args += ", " + emitExpr(e, null, ctx).code;
                }
                return new Out("DString.indexOf(" + args + ")", TypeRef.INT);
            }
            if (n.equals("lastIndexOf")) {
                // Unlowered, it reached Java's String.lastIndexOf: an int result, no
                // RegExp pattern, and a long start that did not compile.
                String args = target.code + ", " + emitExpr(pos.get(0), null, ctx).code;
                if (pos.size() > 1) {
                    args += ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code;
                }
                return new Out("DString.lastIndexOf(" + args + ")", TypeRef.INT);
            }
            if (n.equals("replaceFirst")) {
                // Unresolved before: DString's overloads were never reached.
                String start = pos.size() > 2 ? ", " + emitExpr(pos.get(2), TypeRef.INT, ctx).code : "";
                return new Out("DString.replaceFirst(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ", "
                        + emitExpr(pos.get(1), null, ctx).code + start + ")", TypeRef.STRING);
            }
            if (n.equals("replaceAll")) {
                return new Out("DString.replaceAll(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ", "
                        + emitExpr(pos.get(1), null, ctx).code + ")", TypeRef.STRING);
            }
            if (n.equals("codeUnitAt")) {
                return new Out("DString.codeUnitAt(" + target.code + ", "
                        + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", TypeRef.INT);
            }
            if (n.equals("padLeft") || n.equals("padRight")) {
                String args = target.code;
                for (Expr e : pos) {
                    args += ", " + emitExpr(e, null, ctx).code;
                }
                return new Out("DString." + n + "(" + args + ")", TypeRef.STRING);
            }
            if (n.equals("compareTo")) {
                return new Out("DString.compareTo(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.INT);
            }
            if (n.equals("toUpperCase") || n.equals("toLowerCase")) {
                // Through the runtime, not Java's String methods: those follow the
                // device's default locale, so under Turkish 'i'.toUpperCase() was a
                // dotted capital and case-folded identifiers varied by device. Dart's
                // are locale independent.
                return new Out("DString." + n + "(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("trim")) {
                // Dart's whitespace set, not Java's <= U+0020.
                return new Out("DString.trim(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("startsWith")) {
                // A RegExp (any non-String Pattern) has no Java String.startsWith: it goes
                // through the runtime's prefix match. startsWith(pattern, index): the index
                // was once dropped, so 'abc'.startsWith('b', 1) answered false.
                Out pat = emitExpr(pos.get(0), null, ctx);
                boolean plain = pat.type != null && pat.type.is("String");
                if (plain && pos.size() == 1) {
                    return new Out(target.code + ".startsWith(" + pat.code + ")", TypeRef.BOOL);
                }
                ctx.importClass("dart.core.DString");
                String idx = pos.size() > 1 ? emitExpr(pos.get(1), TypeRef.INT, ctx).code : "0L";
                return new Out("DString.startsWith(" + target.code + ", " + (plain ? "" : "(Object) ")
                        + pat.code + ", " + idx + ")", TypeRef.BOOL);
            }
            if (n.equals("startsWith") || n.equals("endsWith")) {
                return new Out(target.code + "." + n + "("
                        + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.BOOL);
            }
            if (n.equals("toString")) {
                return new Out(target.code, TypeRef.STRING);
            }
        }
        if ((tt.is("int") || tt.is("double") || tt.is("num")) && n.equals("compareTo") && pos.size() == 1) {
            // num.compareTo across representations: an int against a double compares
            // exactly, -0.0 orders below 0, NaN above everything. Unresolved before.
            ctx.importClass("dart.core.DartComparable");
            Out other = emitExpr(pos.get(0), null, ctx);
            return new Out("DartComparable.compare(" + boxNumber(new Out(target.code, tt)) + ", "
                    + boxNumber(other) + ")", TypeRef.INT);
        }
        if (tt.is("int")) {
            if (n.equals("toString")) {
                return new Out("Long.toString(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("toStringAsFixed")) {
                ctx.importClass("dart.runtime.DartRuntime");
                return new Out("DartRuntime.toStringAsFixed(" + paren(target.code) + ", "
                        + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", TypeRef.STRING);
            }
            if (n.equals("toRadixString")) {
                ctx.importClass("dart.runtime.DartRuntime");
                return new Out("DartRuntime.toRadixString(" + target.code + ", "
                        + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", TypeRef.STRING);
            }
            if (n.equals("toDouble")) {
                return new Out("((double) " + paren(target.code) + ")", TypeRef.DOUBLE);
            }
            if (n.equals("toInt")) {
                return new Out(paren(target.code), TypeRef.INT);
            }
            if (n.equals("abs")) {
                return new Out("Math.abs(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("clamp")) {
                // ArgumentError for lower > upper, as Dart; nested min/max answered a value.
                ctx.importClass("dart.runtime.DartRuntime");
                String lo = emitExpr(pos.get(0), TypeRef.INT, ctx).code;
                String hi = emitExpr(pos.get(1), TypeRef.INT, ctx).code;
                return new Out("DartRuntime.clamp(" + target.code + ", " + lo + ", " + hi + ")", TypeRef.INT);
            }
        }
        if (tt.is("num") && pos.isEmpty()) {
            // A value typed only `num` -- `(json['id'] as num).toInt()`, the shape
            // json_serializable generates -- is a boxed Long or Double here.
            ctx.importClass("dart.runtime.DartRuntime");
            if (n.equals("toInt") || n.equals("truncate")) {
                return new Out("DartRuntime.numToInt(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("toDouble")) {
                return new Out("DartRuntime.numToDouble(" + target.code + ")", TypeRef.DOUBLE);
            }
        }
        if (tt.is("double")) {
            ctx.importClass("dart.runtime.DartRuntime");
            if (n.equals("toString")) {
                return new Out("DartRuntime.doubleStr(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("toStringAsFixed")) {
                return new Out("DartRuntime.toStringAsFixed(" + paren(target.code) + ", "
                        + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", TypeRef.STRING);
            }
            // toInt, floor and ceil refuse NaN and the infinities with UnsupportedError,
            // as Dart does; a Java cast made NaN 0 and saturated the infinities.
            if (n.equals("toInt") || n.equals("truncate")) {
                return new Out("DartRuntime.toInt(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("toDouble")) {
                return new Out("((double) " + paren(target.code) + ")", TypeRef.DOUBLE);
            }
            if (n.equals("floor")) {
                return new Out("DartRuntime.floor(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("ceil")) {
                return new Out("DartRuntime.ceil(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("round")) {
                // Half away from zero, as Dart rounds; Math.round rounds -1.5 to -1.
                ctx.importClass("dart.runtime.DartRuntime");
                return new Out("DartRuntime.round(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("floorToDouble")) {
                return new Out("Math.floor(" + target.code + ")", TypeRef.DOUBLE);
            }
            if (n.equals("ceilToDouble")) {
                return new Out("Math.ceil(" + target.code + ")", TypeRef.DOUBLE);
            }
            if (n.equals("roundToDouble")) {
                ctx.importClass("dart.runtime.DartRuntime");
                return new Out("DartRuntime.roundToDouble(" + target.code + ")", TypeRef.DOUBLE);
            }
            if (n.equals("abs")) {
                return new Out("Math.abs(" + target.code + ")", TypeRef.DOUBLE);
            }
            if (n.equals("clamp")) {
                String lo = emitExpr(pos.get(0), TypeRef.DOUBLE, ctx).code;
                String hi = emitExpr(pos.get(1), TypeRef.DOUBLE, ctx).code;
                return new Out("DartRuntime.clamp(((double) " + paren(target.code) + "), " + lo + ", "
                        + hi + ")", TypeRef.DOUBLE);
            }
        }
        if (tt.is("List") || tt.is("Iterable") || tt.is("Set")) {
            TypeRef elem = tt.arg(0);
            if (n.equals("add")) {
                Out v = emitExpr(pos.get(0), elem, ctx);
                String pk = primitiveListKind(tt);
                if (pk != null) {
                    return new Out(target.code + ".add" + pk + "(" + coerce(v, elem, ctx) + ")", TypeRef.VOID);
                }
                // Set.add answers whether the element was new, as Dart's does; List.add
                // is void. Typing both void made `var added = set.add(x)` declare a
                // void local.
                return new Out(target.code + ".add(" + boxIfPrimitive(v, ctx) + ")",
                        tt.is("Set") ? TypeRef.BOOL : TypeRef.VOID);
            }
            if (n.equals("addAll")) {
                return new Out(target.code + ".addAllIterable(" + mixinIterableCode(emitExpr(pos.get(0), null, ctx)) + ")", TypeRef.VOID);
            }
            // dart:collection's Queue is a List here (AstBuilder.collectionAlias); its own
            // end operations are the List ones at index 0 / the end.
            if (tt.is("List") && (n.equals("addLast") || n.equals("addFirst")) && pos.size() == 1) {
                if (n.equals("addFirst")) {
                    return new Out(target.code + ".insert(0L, " + coerce(emitExpr(pos.get(0), elem, ctx), elem, ctx) + ")",
                            TypeRef.VOID);
                }
                Call add = new Call();
                add.file = c.file;
                add.line = c.line;
                add.col = c.col;
                add.target = c.target;
                add.name = "add";
                add.args = c.args;
                return emitMethodCallOn(target, add, ctx);
            }
            if (tt.is("List") && n.equals("removeFirst") && pos.isEmpty()) {
                return new Out(target.code + ".removeAt(0L)", elem);
            }
            if (n.equals("insert")) {
                return new Out(target.code + ".insert(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code
                        + ", " + emitExpr(pos.get(1), elem, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("insertAll") && tt.is("List")) {
                return new Out(target.code + ".insertAll(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code
                        + ", " + emitExpr(pos.get(1), null, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("removeAt")) {
                return new Out(target.code + ".removeAt(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", elem);
            }
            if (n.equals("removeLast") && pos.isEmpty()) {
                // DartList has it; only this table lacked it, so the call was E0137.
                return new Out(target.code + ".removeLast()", elem);
            }
            if (n.equals("remove")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".removeValue(" + boxIfPrimitive(v, ctx) + ")", TypeRef.BOOL);
            }
            if (n.equals("contains")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".contains(" + boxIfPrimitive(v, ctx) + ")", TypeRef.BOOL);
            }
            if (n.equals("indexOf")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                // indexOf(element, start): the start was dropped, so a scan from an
                // offset began again at 0.
                String start = pos.size() > 1 ? ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code : "";
                return new Out(target.code + ".indexOfDart(" + boxIfPrimitive(v, ctx) + start + ")", TypeRef.INT);
            }
            if (n.equals("lastIndexOf")) {
                // Was unlowered: it reached Java's lastIndexOf, which compares with the
                // wrappers' equals (1 never matched 1.0) and has no start at all.
                Out v = emitExpr(pos.get(0), null, ctx);
                String start = pos.size() > 1 ? ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code : "";
                return new Out(target.code + ".lastIndexOfDart(" + boxIfPrimitive(v, ctx) + start + ")", TypeRef.INT);
            }
            if (n.equals("join")) {
                String sep = pos.isEmpty() ? "\"\"" : emitExpr(pos.get(0), null, ctx).code;
                return new Out(target.code + ".join(" + sep + ")", TypeRef.STRING);
            }
            if (n.equals("map")) {
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.DYNAMIC), ctx);
                return new Out(target.code + ".map(" + f.code + ")", TypeRef.of("Iterable", TypeRef.DYNAMIC));
            }
            if (n.equals("where")) {
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx);
                return new Out(target.code + ".where(" + f.code + ")", TypeRef.of("Iterable", elem));
            }
            if (n.equals("whereType")) {
                // Dart's whereType<T>() takes no predicate -- T can only be read from the
                // explicit <T> -- so the runtime's whereType(Class<T>) takes it as a
                // trailing type witness (a class literal for a user class, the boxed
                // class for a type the emitter maps to a Java primitive). No explicit
                // <T> passes a null witness: dynamic, which keeps every element, null
                // included, as Dart does. A nullable T? (and dynamic/Object? spelled out)
                // also admits null, so it says so: Class.isInstance(null) is false.
                //
                // An ENCLOSING generic method's <T> (only<T>(xs) => xs.whereType<T>()) is
                // neither: it is a real, specific type at the call site, but this emitter
                // erases it -- there is no Class to pass -- so it used to fall into the
                // null-witness branch above and silently filter nothing instead of only T.
                // Diagnosed instead of lowered: see reportTypeVariableWitness.
                TypeRef sub = c.typeArgs.isEmpty() ? null : c.typeArgs.get(0);
                String subW = sub != null && sub.args.isEmpty() ? witnessFor(sub.name, ctx) : null;
                if (subW != null) {
                    // A type parameter with a reified witness: filter by its class.
                    return new Out(target.code + ".whereType((java.lang.Class) " + subW + ".type(), "
                            + (sub.nullable ? "true" : subW + ".test(null)") + ")",
                            TypeRef.of("Iterable", sub));
                }
                if (sub != null && reportTypeVariableWitness(c, sub, ctx, "'whereType<" + sub.name + ">()'")) {
                    return new Out(target.code + ".whereType(null)",
                            TypeRef.of("Iterable", TypeRef.DYNAMIC), true);
                }
                String token = sub != null ? javaType(sub, true, ctx) + ".class" : "null";
                boolean admitsNull = sub == null || sub.nullable || sub.is("dynamic") || sub.is("var");
                return new Out(target.code + ".whereType(" + token + (admitsNull ? ", true" : "") + ")",
                        TypeRef.of("Iterable", sub != null ? sub : TypeRef.DYNAMIC));
            }
            if (n.equals("forEach")) {
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.VOID), ctx);
                return new Out(target.code + ".forEachDart(" + f.code + ")", TypeRef.VOID);
            }
            if (n.equals("toList")) {
                TypeRef listType = TypeRef.of("List", elem);
                // toList(growable: false) used to ignore the flag and hand back a growable list.
                String growable = null;
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("growable")) {
                        growable = emitExpr(na.value, TypeRef.BOOL, ctx).code;
                    }
                }
                // A Dart List<int>/List<double> variable has Java type Dart{Long,Double}List, but
                // the runtime toList() returns a boxed DartList<E>. Re-wrap into the primitive
                // list so the value matches its declared/target type.
                String pk = primitiveListKind(listType);
                if (pk != null) {
                    ctx.importClass("dart.core.Dart" + pk + "List");
                    return new Out("Dart" + pk + "List.from" + pk + "s(" + target.code + ".toList()"
                            + (growable != null ? ", " + growable : "") + ")", listType);
                }
                return new Out(target.code + ".toList(" + (growable != null ? growable : "") + ")", listType);
            }
            if (n.equals("sublist")) {
                String args = "";
                for (Expr e : pos) {
                    args += (args.isEmpty() ? "" : ", ")
                            + coerce(emitExpr(e, TypeRef.INT, ctx), TypeRef.INT, ctx);
                }
                return new Out(target.code + ".sublist(" + args + ")", tt);
            }
            if (n.equals("clear")) {
                return new Out(target.code + ".clear()", TypeRef.VOID);
            }
            if (n.equals("any") || n.equals("every")) {
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx);
                return new Out(target.code + "." + n + "(" + f.code + ")", TypeRef.BOOL);
            }
            if (n.equals("fold")) {
                Out init = emitExpr(pos.get(0), null, ctx);
                // The combiner is (R, E) -> R with R the seed's type, so its result is
                // coerced to R: `fold(0.0, (num sum, int e) => sum + e)` computes a num
                // and must hand back the double the seed made R.
                TypeRef seed = init.type != null && !init.type.is("dynamic") && !init.type.is("var")
                        ? init.type : null;
                TypeRef combineType = null;
                if (seed != null) {
                    combineType = new TypeRef("Function");
                    combineType.funcParams = new ArrayList<TypeRef>();
                    combineType.funcParams.add(seed);
                    combineType.funcParams.add(elem != null ? elem : TypeRef.DYNAMIC);
                    combineType.funcReturn = seed;
                }
                Out combine = emitExpr(pos.get(1), combineType, ctx);
                // The generic result R is inferred from the (boxed) seed; unbox it
                // back to a primitive when the seed is numeric so it flows straight
                // into arithmetic / a primitive-typed return.
                TypeRef r = init.type != null && (init.type.is("int") || init.type.is("double"))
                        ? init.type : TypeRef.DYNAMIC;
                String call = target.code + ".fold(" + boxIfPrimitive(init, ctx) + ", " + combine.code + ")";
                return new Out(unboxPrimitiveResult(call, r), r);
            }
            if (n.equals("firstWhere")) {
                Out test = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx);
                String orElse = "null";
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("orElse")) {
                        orElse = emitExpr(na.value, null, ctx).code;
                    }
                }
                String call = target.code + ".firstWhere(" + test.code + ", " + orElse + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("elementAt")) {
                String call = target.code + ".elementAt(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("sort")) {
                if (pos.isEmpty()) {
                    return new Out(target.code + ".sortDefault()", TypeRef.VOID);
                }
                // DartList inherits java.util.List.sort(Comparator) too, so a bare comparator
                // lambda is ambiguous. Pin it to DartList's Func2<E,E,int> overload.
                ctx.importClass("dart.runtime.Funcs");
                String et = elem != null ? javaType(boxType(elem), true, ctx) : "Object";
                TypeRef cmpType = null;
                if (elem != null && !elem.is("dynamic") && !elem.is("var")) {
                    // The comparator's parameters are the element type, so `(a, b) =>
                    // a.name.compareTo(b.name)` reads typed members, not dynamic ones.
                    cmpType = new TypeRef("Function");
                    cmpType.funcParams = new ArrayList<TypeRef>(java.util.Arrays.asList(elem, elem));
                    cmpType.funcRequired = 2;
                    cmpType.funcReturn = TypeRef.INT;
                }
                String cmp = emitExpr(pos.get(0), cmpType, ctx).code;
                return new Out(target.code + ".sort((Funcs.Func2<" + et + ", " + et + ", Long>) "
                        + paren(cmp) + ")", TypeRef.VOID);
            }
            if (n.equals("indexWhere")) {
                String args = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx).code;
                if (pos.size() > 1) {
                    args += ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code;
                }
                return new Out(target.code + ".indexWhere(" + args + ")", TypeRef.INT);
            }
            if (n.equals("lastIndexWhere")) {
                String start = pos.size() > 1 ? ", " + emitExpr(pos.get(1), TypeRef.INT, ctx).code : "";
                return new Out(target.code + ".lastIndexWhere("
                        + emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx).code + start + ")",
                        TypeRef.INT);
            }
            if (n.equals("removeWhere")) {
                return new Out(target.code + ".removeWhere("
                        + emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx).code + ")",
                        TypeRef.VOID);
            }
            if (n.equals("retainWhere")) {
                return new Out(target.code + ".retainWhere("
                        + emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx).code + ")",
                        TypeRef.VOID);
            }
            if (n.equals("lastWhere")) {
                Out test = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx);
                String orElse = "null";
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("orElse")) {
                        orElse = emitExpr(na.value, null, ctx).code;
                    }
                }
                String call = target.code + ".lastWhere(" + test.code + ", " + orElse + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("singleWhere")) {
                Out test = emitExpr(pos.get(0), null, ctx);
                String orElse = "null";
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("orElse")) {
                        orElse = emitExpr(na.value, null, ctx).code;
                    }
                }
                String call = target.code + ".singleWhere(" + test.code + ", " + orElse + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("reduce")) {
                // The combiner is (E, E) -> E: typed so `(a, b) => a + b` on a List<int> adds
                // longs rather than dispatching dynamically into an Object result.
                TypeRef combine = elem == null || isDynamicType(elem) ? null : inlineFuncType2(elem, elem, elem);
                String call = target.code + ".reduce(" + emitExpr(pos.get(0), combine, ctx).code + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("expand")) {
                // The callback's parameter is the element (`entries.expand((e) => ...)` reads
                // e.key, e.value typed); its result stays dynamic for the SAM, and the
                // element type of what it returns, when it says, types the expansion.
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.DYNAMIC), ctx);
                TypeRef out = TypeRef.DYNAMIC;
                if (pos.get(0) instanceof Lambda) {
                    TypeRef r = lastLambdaExprType;
                    if (r == null && lastLambdaReturns != null && lastLambdaReturns.size() == 1) {
                        r = lastLambdaReturns.get(0);
                    }
                    if (r != null && ITERABLE_LIKE.contains(r.name) && r.args.size() == 1
                            && !isTypeVariable(r.arg(0), ctx)) {
                        out = r.arg(0);
                    }
                }
                return new Out(target.code + ".expand(" + f.code + ")", TypeRef.of("Iterable", out));
            }
            if (n.equals("followedBy")) {
                return new Out(target.code + ".followedBy(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.of("Iterable", elem));
            }
            if (n.equals("take") || n.equals("skip")) {
                return new Out(target.code + "." + n + "(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")",
                        TypeRef.of("Iterable", elem));
            }
            if (n.equals("takeWhile") || n.equals("skipWhile")) {
                Out f = emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx);
                return new Out(target.code + "." + n + "(" + f.code + ")", TypeRef.of("Iterable", elem));
            }
            if (n.equals("getRange")) {
                String args = emitExpr(pos.get(0), TypeRef.INT, ctx).code + ", "
                        + emitExpr(pos.get(1), TypeRef.INT, ctx).code;
                return new Out(target.code + ".getRange(" + args + ")", TypeRef.of("Iterable", elem));
            }
            if (n.equals("asMap")) {
                // The runtime answers a boxed live view (DartMap<Long, E>), never the
                // primitive DartLongMap, so a List<int>'s view is typed Map<int, int?>:
                // the nullable value keeps isPrimitiveLongMap from claiming it, and
                // matches what the view's [] returns in Dart anyway.
                TypeRef viewValue = elem;
                if (elem != null && elem.is("int") && !elem.nullable) {
                    viewValue = TypeRef.of("int");
                    viewValue.nullable = true;
                }
                return new Out(target.code + ".asMap()", TypeRef.of("Map", TypeRef.INT, viewValue));
            }
            if (n.equals("toSet")) {
                return new Out(target.code + ".toSet()", TypeRef.of("Set", elem));
            }
            if (tt.is("Set") && n.equals("difference")) {
                return new Out(target.code + ".difference(" + emitExpr(pos.get(0), null, ctx).code + ")", tt);
            }
            if (tt.is("Set") && n.equals("intersection")) {
                return new Out(target.code + ".intersection(" + emitExpr(pos.get(0), null, ctx).code + ")", tt);
            }
            if (tt.is("Set") && n.equals("union")) {
                return new Out(target.code + ".union(" + emitExpr(pos.get(0), null, ctx).code + ")", tt);
            }
            if (tt.is("Set") && n.equals("containsAll")) {
                return new Out(target.code + ".containsAll(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.BOOL);
            }
            if (tt.is("Set") && n.equals("removeAll")) {
                // DartSet.removeAll(Iterable) already existed; only this table lacked a
                // case for it, so the call fell through to the generic E0137 error.
                return new Out(target.code + ".removeAll(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.VOID);
            }
            if (tt.is("Set") && n.equals("retainAll")) {
                return new Out(target.code + ".retainAll(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.VOID);
            }
        }
        if (tt.is("Map")) {
            if (n.equals("containsKey")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".containsKey(" + boxIfPrimitive(v, ctx) + ")", TypeRef.BOOL);
            }
            if (n.equals("containsValue")) {
                // Boxed through Object so a primitive argument picks containsValue(Object),
                // which compares with Dart's ==; unresolved before.
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".containsValue((Object) " + paren(v.code) + ")", TypeRef.BOOL);
            }
            if (n.equals("remove")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".removeDart(" + boxIfPrimitive(v, ctx) + ")", boxType(tt.arg(1)));
            }
            if (n.equals("forEach")) {
                Out f = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".forEachDart(" + f.code + ")", TypeRef.VOID);
            }
            if (n.equals("putIfAbsent")) {
                Out k = emitExpr(pos.get(0), null, ctx);
                // `V Function()`: the closure's literal builds the map's value type, so
                // `() => []` on a Map<K, List<int>> makes a DartLongList.
                TypeRef ifAbsentType = null;
                if (tt.args.size() == 2 && tt.arg(1) != null && !isTypeVariable(tt.arg(1), ctx)) {
                    ifAbsentType = new TypeRef("Function");
                    ifAbsentType.funcParams = new ArrayList<TypeRef>();
                    ifAbsentType.funcReturn = tt.arg(1);
                }
                Out f = emitExpr(pos.get(1), ifAbsentType, ctx);
                return new Out(target.code + ".putIfAbsentDart(" + boxIfPrimitive(k, ctx) + ", " + f.code + ")",
                        boxType(tt.arg(1)));
            }
            if (n.equals("addAll")) {
                return new Out(target.code + ".addAll(" + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("addEntries")) {
                return new Out(target.code + ".addEntries(" + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("removeWhere")) {
                return new Out(target.code + ".removeWhere(" + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("update")) {
                Out k = emitExpr(pos.get(0), null, ctx);
                Out upd = emitExpr(pos.get(1), null, ctx);
                String ifAbsent = "null";
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("ifAbsent")) {
                        ifAbsent = emitExpr(na.value, null, ctx).code;
                    }
                }
                return new Out(target.code + ".update(" + boxIfPrimitive(k, ctx) + ", " + upd.code
                        + ", " + ifAbsent + ")", boxType(tt.arg(1)));
            }
            if (n.equals("clear")) {
                return new Out(target.code + ".clear()", TypeRef.VOID);
            }
        }
        if (tt.is("Future")) {
            ctx.importClass("dart.async.Future");
            if (n.equals("then")) {
                // `getUrl(u).then((r) => r.close())`: an untyped callback parameter takes
                // the future's value type, as Dart infers it, so members of a runtime class
                // resolve statically -- through dynamic dispatch they could not resolve.
                TypeRef valueType = tt.args.size() == 1 ? tt.arg(0) : null;
                // A tear-off onValue (`then(completer.complete)`) is a Java method reference
                // both then overloads accept, which javac reports as ambiguous.
                Expr cbExpr = tearOffAsLambda(pos.get(0), 1, ctx);
                boolean typedCb = valueType != null && !isDynamicType(valueType)
                        && (isFullyConcrete(valueType) || classTypeVarInScope(valueType, ctx))
                        && !valueType.is("int") && !valueType.is("double") && !valueType.is("bool")
                        && !valueType.is("num") && !valueType.is("void") && !valueType.is("Null")
                        && cbExpr instanceof Lambda && ((Lambda) cbExpr).params.size() == 1
                        && ((Lambda) cbExpr).typeParams.isEmpty()
                        && (((Lambda) cbExpr).params.get(0).type == null || ((Lambda) cbExpr).params.get(0).type.is("var"));
                Out cb = emitExpr(cbExpr, typedCb ? inlineFuncType(valueType, TypeRef.DYNAMIC) : null, ctx);
                String vj = typedCb ? javaType(boxType(valueType), true, ctx) : null;
                // A lambda fits BOTH then overloads (Func1 / VoidFunc1) when it is void- or
                // throw-bodied, which javac reports as ambiguous; pin those to VoidFunc1.
                String cbCode = pinnedHandler(cbExpr, cb, typedCb,
                        typedCb ? "dart.runtime.Funcs.Func1<" + vj + ", Object>" : "dart.runtime.Funcs.Func1",
                        typedCb ? "dart.runtime.Funcs.VoidFunc1<" + vj + ">" : "dart.runtime.Funcs.VoidFunc1");
                // then(onValue, onError: handler): the handler was dropped, so a failure
                // skipped it and the chain stayed failed.
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("onError")) {
                        // A tear-off (`completer.completeError`) became a bare Java method
                        // reference, which an Object parameter cannot take: call it from a
                        // closure of the handler's own arity instead.
                        Expr onErr = tearOffAsErrorLambda(na.value, ctx);
                        // (error, stackTrace) handlers take the two-argument shapes.
                        boolean two = onErr instanceof Lambda && ((Lambda) onErr).params.size() == 2;
                        String handler = pinnedHandler(onErr, emitExpr(onErr, null, ctx), true,
                                two ? "dart.runtime.Funcs.Func2<Object, Object, Object>"
                                        : "dart.runtime.Funcs.Func1<Object, Object>",
                                two ? "dart.runtime.Funcs.VoidFunc2<Object, Object>"
                                        : "dart.runtime.Funcs.VoidFunc1<Object>");
                        return new Out(target.code + ".then(" + cbCode + ", (Object) " + paren(handler) + ")",
                                TypeRef.of("Future", TypeRef.DYNAMIC));
                    }
                }
                return new Out(target.code + ".then(" + cbCode + ")", TypeRef.of("Future", TypeRef.DYNAMIC));
            }
            if (n.equals("catchError")) {
                Out cb = emitExpr(pos.get(0), null, ctx);
                String cbCode = pinnedHandler(pos.get(0), cb, false, "dart.runtime.Funcs.Func1",
                        "dart.runtime.Funcs.VoidFunc1");
                boolean valueHandler = pos.get(0) instanceof Lambda
                        && (((Lambda) pos.get(0)).exprBody != null ? !lastLambdaVoid : lastLambdaNullTail || lambdaReturnsValue(pos.get(0)));
                // `test` is a NAMED parameter in Dart; reading it positionally meant it was
                // always null, so the handler caught failures its predicate rejected. The
                // runtime takes it as an Object, so a lambda is given its predicate type.
                String test = "null";
                Expr testExpr = pos.size() > 1 ? pos.get(1) : null;
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("test")) {
                        testExpr = na.value;
                    }
                }
                if (testExpr != null) {
                    test = pinnedHandler(testExpr, emitExpr(testExpr, null, ctx), true,
                            "dart.runtime.Funcs.Func1<Object, Boolean>", "dart.runtime.Funcs.Func1<Object, Boolean>");
                }
                // A handler that returns a value recovers with it, so the runtime answers
                // a Future<Object>: typed that way, an await of it coerces to the slot.
                return new Out(target.code + ".catchError(" + cbCode + ", " + test + ")",
                        valueHandler ? TypeRef.of("Future", TypeRef.DYNAMIC) : tt);
            }
            if (n.equals("whenComplete")) {
                // A cleanup that can produce a value -- `() => asyncCleanup()`, or a tear-off
                // of a function returning one -- goes to whenCompleteFuture, which waits for
                // a returned Future and surfaces its failure. The void form discarded it.
                Expr action = pos.get(0);
                Out cb = emitExpr(action, null, ctx);
                boolean value;
                if (action instanceof Lambda) {
                    value = ((Lambda) action).exprBody != null ? !lastLambdaVoid : lastLambdaNullTail || lambdaReturnsValue(action);
                } else {
                    // A tear-off is typed a bare Function, so its declared return is read
                    // from the method or function it names; unresolved stays on the void form.
                    TypeRef ret = cb.type != null ? cb.type.funcReturn : null;
                    if (ret == null) {
                        ret = tearOffReturn(action, ctx);
                    }
                    value = ret != null && !ret.is("void");
                }
                if (value) {
                    String fn = action instanceof Lambda
                            ? "(dart.runtime.Funcs.Func0<Object>) " + paren(cb.code) : cb.code;
                    return new Out(target.code + ".whenCompleteFuture(" + fn + ")", tt);
                }
                return new Out(target.code + ".whenComplete(" + cb.code + ")", tt);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Constructor calls
    // ------------------------------------------------------------------

    /**
     * Wraps a boxed generic-collection result (e.g. {@code Iterable<E>.fold}/{@code firstWhere})
     * in an unboxing cast when the Dart element/result type is a primitive {@code int}/{@code double},
     * so the value can flow into primitive arithmetic and primitive-typed returns. The intermediate
     * {@code (Long)}/{@code (Double)} cast keeps it valid even when the static type is erased to Object.
     */
    private static String unboxPrimitiveResult(String code, TypeRef t) {
        if (t != null && t.is("int")) {
            return "((long)(Long) (" + code + "))";
        }
        if (t != null && t.is("double")) {
            return "((double)(Double) (" + code + "))";
        }
        return code;
    }

    /**
     * Emits a Dart {@code List<E>} factory constructor ({@code generate}, {@code filled}
     * or {@code from}). Non-nullable {@code int}/{@code double} element types route to the
     * primitive {@link dart.core.DartLongList}/{@link dart.core.DartDoubleList} so the
     * result stays index/add consistent with how {@code List<int>}/{@code List<double>}
     * variables are typed elsewhere; everything else uses the boxed {@link dart.core.DartList}.
     */
    private Out emitListFactory(CtorCall cc, Ctx ctx) {
        TypeRef listType = cc.type;
        TypeRef elem = listType.args.isEmpty() ? TypeRef.DYNAMIC : listType.arg(0);
        String pk = primitiveListKind(listType);
        String cls;
        if (pk != null) {
            ctx.importClass("dart.core.Dart" + pk + "List");
            cls = "Dart" + pk + "List";
        } else {
            ctx.importClass("dart.core.DartList");
            cls = "DartList";
        }
        // Explicit type witness only for the generic boxed list; primitive lists are raw.
        String witness = pk == null ? ".<" + javaType(elem, true, ctx) + ">" : ".";
        // The primitive lists name generate/from with a *Longs/*Doubles suffix (their
        // unsuffixed forms would erasure-clash with the inherited DartList statics);
        // filled keeps its name since its primitive signature has a distinct erasure.
        String sfx = pk == null ? "" : ("Long".equals(pk) ? "Longs" : "Doubles");
        List<Expr> pos = cc.args.positional;
        String growable = null;
        for (NamedArg na : cc.args.named) {
            if (na.name.equals("growable")) {
                growable = emitExpr(na.value, TypeRef.BOOL, ctx).code;
            }
        }
        if (cc.ctorName.equals("unmodifiable")) {
            // A read-only copy; unresolved before, so any use failed the build.
            String src = emitExpr(pos.get(0), null, ctx).code;
            return new Out(cls + witness + (pk == null ? "unmodifiable" : "unmodifiable" + sfx) + "(" + src + ")",
                    listType);
        }
        if (cc.ctorName.equals("from")) {
            String src = emitExpr(pos.get(0), null, ctx).code;
            // Each element is checked against a concrete element type, as Dart does
            // (the primitive lists check their own); dynamic, Object and type variables
            // accept anything and keep the plain copy.
            if (pk == null && elem != null && isConcreteType(elem) && !isDynamicType(elem)) {
                String jt = javaType(copyNonNull(elem), true, ctx);
                int lt = jt.indexOf('<');
                String raw = lt >= 0 ? jt.substring(0, lt) : jt;
                return new Out(cls + witness + "fromChecked(" + src + ", " + raw + ".class, " + elem.nullable
                        + ", " + (growable != null ? growable : "true") + ")", listType);
            }
            // growable: false was parsed above and then dropped for from().
            return new Out(cls + witness + "from" + sfx + "(" + src
                    + (growable != null ? ", " + growable : "") + ")", listType);
        }
        String len = emitExpr(pos.get(0), TypeRef.INT, ctx).code;
        if (cc.ctorName.equals("filled")) {
            Out fillOut = emitExpr(pos.get(1), elem, ctx);
            String fill = pk == null ? boxIfPrimitive(fillOut, ctx) : coerce(fillOut, elem, ctx);
            String args = len + ", " + fill + (growable != null ? ", " + growable : "");
            return new Out(cls + witness + "filled(" + args + ")", listType);
        }
        // generate: the generator's index parameter is typed via the IndexedGenerator typedef
        String gen = emitExpr(pos.get(1), new TypeRef("IndexedGenerator"), ctx).code;
        String args = len + ", " + gen + (growable != null ? ", " + growable : "");
        return new Out(cls + witness + "generate" + sfx + "(" + args + ")", listType);
    }

    /**
     * dart:core Map/Set/Iterable named factory constructors, routed to the existing
     * statics on {@code DartMap}/{@code DartSet}/{@code DartIterable}. Returns null
     * when {@code cc} is not one of these core collection factories so the caller can
     * fall through to program-class / stub resolution.
     *
     * <p>Semantics match dart:core: {@code Map.of}/{@code Map.from} shallow-copy the
     * source map, {@code Map.fromIterable} applies optional {@code key}/{@code value}
     * transforms (element identity when a transform is absent), {@code Map.fromEntries}
     * copies key/value pairs, {@code Set.of}/{@code Set.from} copy an iterable, and
     * {@code Iterable.generate(count, [generator])} builds a lazy index sequence.
     */
    /**
     * dart:async {@code Future} named constructors used in ctor position
     * ({@code Future.delayed} / {@code Future.value} / {@code Future.error}),
     * routed to the {@link dart.async.Future} statics. Returns null for an
     * unrecognized name so the caller can fall through to the E0126 diagnostic.
     */
    private Out emitFutureNamedCtor(CtorCall cc, Ctx ctx) {
        ctx.importClass("dart.async.Future");
        String ctor = cc.ctorName;
        List<Expr> pos = cc.args.positional;
        if (ctor.equals("delayed")) {
            String dur = emitExpr(pos.get(0), new TypeRef("Duration"), ctx).code;
            String comp = pos.size() > 1 ? emitExpr(pos.get(1), null, ctx).code : null;
            return new Out("Future.delayed(" + dur + (comp != null ? ", " + comp : "") + ")",
                    TypeRef.of("Future", TypeRef.DYNAMIC));
        }
        if (ctor.equals("value")) {
            Out v = pos.isEmpty() ? new Out("null", TypeRef.NULL) : emitExpr(pos.get(0), null, ctx);
            return new Out("Future.value(" + boxIfPrimitive(v, ctx) + ")",
                    TypeRef.of("Future", v.type));
        }
        if (ctor.equals("error")) {
            return new Out("Future.error(" + emitExpr(pos.get(0), null, ctx).code
                    + (pos.size() > 1 ? ", " + emitExpr(pos.get(1), null, ctx).code : "") + ")",
                    TypeRef.of("Future", TypeRef.DYNAMIC));
        }
        return null;
    }

    /**
     * dart:collection's concrete collection constructors ({@code HashMap<K, V>()},
     * {@code SplayTreeSet<E>(compare)}, {@code Queue<E>()}, {@code UnmodifiableListView(src)})
     * built as the built-in Map/Set/List they alias (see AstBuilder.collectionAlias), so the
     * result has every built-in member. Null when {@code n} is not one of them, or names a
     * class of the program.
     */
    private Out emitCollectionClassCtor(Call c, String n, Ctx ctx) {
        String alias = com.codename1.dart.transpiler.parser.AstBuilder.collectionAlias(n);
        if (alias.equals(n) || program.classes.containsKey(n) || stubs.isStubClass(n)) {
            return null;
        }
        List<TypeRef> ta = c.typeArgs;
        boolean isMap = alias.equals("Map");
        TypeRef a0 = ta.isEmpty() ? TypeRef.DYNAMIC : ta.get(0);
        TypeRef a1 = ta.size() < 2 ? TypeRef.DYNAMIC : ta.get(1);
        TypeRef type = isMap ? TypeRef.of("Map", a0, a1) : TypeRef.of(alias, a0);
        List<Expr> pos = c.args.positional;
        if (n.startsWith("SplayTree")) {
            ctx.importClass("dart.collection." + n);
            String witness = isMap ? "<" + javaType(a0, true, ctx) + ", " + javaType(a1, true, ctx) + ">"
                    : "<" + javaType(a0, true, ctx) + ">";
            String cmp = "";
            Expr cmpE = pos.isEmpty() ? null : pos.get(0);
            for (NamedArg na : c.args.named) {
                if (na.name.equals("compare")) {
                    cmpE = na.value;
                }
            }
            if (cmpE != null) {
                String j = javaType(a0, true, ctx);
                cmp = "(dart.runtime.Funcs.Func2<" + j + ", " + j + ", Long>) "
                        + paren(emitExpr(cmpE, inlineFuncType2(a0, a0, TypeRef.INT), ctx).code);
            }
            return new Out("new " + n + witness + "(" + cmp + ")", type);
        }
        if (!pos.isEmpty()) {
            // UnmodifiableListView(src) / UnmodifiableMapView(map): a copy through the
            // existing factories (unmodifiable for the views).
            CtorCall cc = new CtorCall();
            cc.file = c.file;
            cc.line = c.line;
            cc.col = c.col;
            cc.type = type;
            cc.ctorName = n.startsWith("Unmodifiable") ? "unmodifiable" : "from";
            cc.args.positional.add(pos.get(0));
            return emitExpr(cc, type, ctx);
        }
        if (isMap) {
            MapLit m = new MapLit();
            m.keyType = a0;
            m.valueType = a1;
            m.file = c.file;
            m.line = c.line;
            m.col = c.col;
            return emitExpr(m, type, ctx);
        }
        if (alias.equals("Set")) {
            SetLit sl = new SetLit();
            sl.elementType = a0;
            sl.file = c.file;
            sl.line = c.line;
            sl.col = c.col;
            return emitExpr(sl, type, ctx);
        }
        ListLit l = new ListLit();
        l.elementType = a0;
        l.file = c.file;
        l.line = c.line;
        l.col = c.col;
        return emitExpr(l, type, ctx);
    }

    /** dart:core type names that only ever appear as the receiver of a static member here. */
    private static final java.util.Set<String> CORE_STATIC_RECEIVERS = new java.util.HashSet<String>(
            java.util.Arrays.asList("List", "Map", "Set", "String", "bool", "int", "double", "num",
                    "RangeError", "ArgumentError"));

    private String posArg(Call c, int i, TypeRef t, Ctx ctx) {
        return i < c.args.positional.size() ? emitExpr(c.args.positional.get(i), t, ctx).code : "null";
    }

    private Expr namedArg(Call c, String name) {
        for (NamedArg na : c.args.named) {
            if (na.name.equals(name)) {
                return na.value;
            }
        }
        return null;
    }

    /**
     * Static members of dart:core types called on the type name: {@code List.of(x)},
     * {@code String.fromCharCode(65)}, {@code RangeError.checkNotNegative(n)},
     * {@code Uri.encodeComponent(s)}. The collection factories route through the same
     * code as their constructor-call form ({@code List<int>.of(x)}). Null for any other
     * class; a core type with an unknown member is reported here, by name.
     */
    private Out emitCoreStaticCall(String cls, Call c, Ctx ctx) {
        if (program.classes.containsKey(cls)) {
            return null;
        }
        String n = c.name;
        List<Expr> pos = c.args.positional;
        if (cls.equals("List") || cls.equals("Map") || cls.equals("Set")
                || (cls.equals("Iterable") && n.equals("empty"))) {
            if (n.equals("empty")) {
                ListLit l = new ListLit();
                l.file = c.file;
                l.line = c.line;
                l.col = c.col;
                l.elementType = TypeRef.DYNAMIC;
                return emitExpr(l, null, ctx);
            }
            if (c.typeArgs.isEmpty() && (cls.equals("List") || cls.equals("Set"))
                    && (n.equals("of") || n.equals("from")) && pos.size() == 1 && c.args.named.isEmpty()) {
                // No type argument: a List<dynamic> copy, whatever the source's element type.
                String rt = cls.equals("List") ? "DartList" : "DartSet";
                ctx.importClass("dart.core." + rt);
                Out src = emitExpr(pos.get(0), null, ctx);
                String code = paren(mixinIterableCode(src));
                return new Out(rt + ".<Object>from((Iterable<Object>) (Iterable<?>) " + code + ")",
                        TypeRef.of(cls, TypeRef.DYNAMIC));
            }
            CtorCall cc = new CtorCall();
            cc.file = c.file;
            cc.line = c.line;
            cc.col = c.col;
            cc.type = new TypeRef(cls);
            cc.type.args.addAll(c.typeArgs);
            cc.ctorName = cls.equals("List") && n.equals("of") ? "from" : n;
            cc.args = c.args;

            Out o = emitExpr(cc, null, ctx);
            return o;
        }
        if (cls.equals("String") || cls.equals("bool") || cls.equals("int")) {
            if (n.equals("fromEnvironment")) {
                Expr dv = namedArg(c, "defaultValue");
                TypeRef t = cls.equals("String") ? TypeRef.STRING : cls.equals("bool") ? TypeRef.BOOL : TypeRef.INT;
                if (dv != null) {
                    return emitExpr(dv, t, ctx);
                }
                return new Out(cls.equals("String") ? "\"\"" : cls.equals("bool") ? "false" : "0L", t);
            }
            if (cls.equals("bool") && n.equals("hasEnvironment")) {
                return new Out("false", TypeRef.BOOL);
            }
        }
        if (cls.equals("String") && (n.equals("fromCharCode") || n.equals("fromCharCodes"))) {
            ctx.importClass("dart.core.CoreStatics");
            return new Out("CoreStatics." + n + "(" + posArg(c, 0, n.equals("fromCharCode") ? TypeRef.INT : null, ctx) + ")",
                    TypeRef.STRING);
        }
        if (cls.equals("Uri")) {
            if (n.equals("encodeComponent") || n.equals("encodeQueryComponent") || n.equals("decodeQueryComponent")) {
                ctx.importClass("dart.core.CoreStatics");
                return new Out("CoreStatics." + n + "(" + posArg(c, 0, TypeRef.STRING, ctx) + ")", TypeRef.STRING);
            }
            if (n.equals("decodeComponent")) {
                ctx.importClass("dart.core.DartUri");
                return new Out("DartUri.decodeComponent(" + posArg(c, 0, TypeRef.STRING, ctx) + ")", TypeRef.STRING);
            }
            if (n.equals("encodeFull") || n.equals("decodeFull")) {
                ctx.importClass("dart.core.DartUri");
                return new Out("DartUri." + n + "(" + posArg(c, 0, TypeRef.STRING, ctx) + ")", TypeRef.STRING);
            }
            if (n.equals("splitQueryString")) {
                ctx.importClass("dart.core.DartUri");
                TypeRef mt = new TypeRef("Map");
                mt.args.add(TypeRef.STRING);
                mt.args.add(TypeRef.STRING);
                return new Out("DartUri.splitQueryString(" + posArg(c, 0, TypeRef.STRING, ctx) + ")", mt);
            }
            return null;
        }
        if (cls.equals("RangeError")) {
            ctx.importClass("dart.core.RangeError");
            TypeRef nl = new TypeRef("int");
            nl.nullable = true;
            String a0 = posArg(c, 0, TypeRef.INT, ctx);
            if (n.equals("checkValidRange") && pos.size() >= 3) {
                return new Out("RangeError.checkValidRange(" + a0 + ", (Long) " + paren(posArg(c, 1, nl, ctx))
                        + ", " + posArg(c, 2, TypeRef.INT, ctx) + ")", TypeRef.INT);
            }
            if (n.equals("checkNotNegative")) {
                return new Out("RangeError.checkNotNegative(" + a0 + ", (String) " + paren(posArg(c, 1, TypeRef.STRING, ctx))
                        + ")", TypeRef.INT);
            }
            if (n.equals("checkValueInInterval")) {
                return new Out("RangeError.checkValueInInterval(" + a0 + ", " + posArg(c, 1, TypeRef.INT, ctx) + ", "
                        + posArg(c, 2, TypeRef.INT, ctx) + ", (String) " + paren(posArg(c, 3, TypeRef.STRING, ctx)) + ")",
                        TypeRef.INT);
            }
            if (n.equals("checkValidIndex")) {
                return new Out("RangeError.checkValidIndex(" + a0 + ", (Object) " + paren(posArg(c, 1, null, ctx))
                        + ", (String) " + paren(posArg(c, 2, TypeRef.STRING, ctx)) + ", (Long) "
                        + paren(posArg(c, 3, nl, ctx)) + ")", TypeRef.INT);
            }
            if (n.equals("range")) {
                return new Out("RangeError.range(" + a0 + ", (Long) " + paren(posArg(c, 1, nl, ctx)) + ", (Long) "
                        + paren(posArg(c, 2, nl, ctx)) + ", (String) " + paren(posArg(c, 3, TypeRef.STRING, ctx))
                        + ", (String) " + paren(posArg(c, 4, TypeRef.STRING, ctx)) + ")", new TypeRef("RangeError"));
            }
            if (n.equals("value")) {
                return new Out("RangeError.value(" + a0 + ", (String) " + paren(posArg(c, 1, TypeRef.STRING, ctx))
                        + ", (String) " + paren(posArg(c, 2, TypeRef.STRING, ctx)) + ")", new TypeRef("RangeError"));
            }
            if (n.equals("index")) {
                return new Out("RangeError.index(" + a0 + ", (Object) " + paren(posArg(c, 1, null, ctx))
                        + ", (String) " + paren(posArg(c, 2, TypeRef.STRING, ctx)) + ", (String) "
                        + paren(posArg(c, 3, TypeRef.STRING, ctx)) + ", (Long) " + paren(posArg(c, 4, nl, ctx)) + ")",
                        new TypeRef("RangeError"));
            }
        }
        if (cls.equals("ArgumentError")) {
            if (n.equals("checkNotNull")) {
                ctx.importClass("dart.core.CoreStatics");
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out("CoreStatics.checkNotNull(" + boxIfPrimitive(v, ctx) + ", (String) "
                        + paren(posArg(c, 1, TypeRef.STRING, ctx)) + ")", v.type);
            }
            if (n.equals("notNull")) {
                ctx.importClass("dart.core.CoreStatics");
                return new Out("CoreStatics.notNull(" + posArg(c, 0, TypeRef.STRING, ctx) + ")",
                        new TypeRef("ArgumentError"));
            }
            if (n.equals("value")) {
                ctx.importClass("dart.core.ArgumentError");
                Out v = pos.isEmpty() ? new Out("null", TypeRef.NULL) : emitExpr(pos.get(0), null, ctx);
                return new Out("ArgumentError.value(" + boxIfPrimitive(v, ctx) + ", (String) "
                        + paren(posArg(c, 1, TypeRef.STRING, ctx)) + ", (String) "
                        + paren(posArg(c, 2, TypeRef.STRING, ctx)) + ")", new TypeRef("ArgumentError"));
            }
        }
        if (CORE_STATIC_RECEIVERS.contains(cls) && !stubs.isStubClass(cls)) {
            diags.error(c, "E0129", "static member " + n + " of core type " + cls + " is not implemented");
            return new Out("null", TypeRef.DYNAMIC, true);
        }
        return null;
    }

    private Out emitCoreCollectionFactory(CtorCall cc, Ctx ctx) {
        String name = cc.type.name;
        String ctor = cc.ctorName;
        List<Expr> pos = cc.args.positional;
        if (name.equals("Map")) {
            ctx.importClass("dart.core.DartMap");
            TypeRef kt = cc.type.args.isEmpty() ? TypeRef.DYNAMIC : cc.type.arg(0);
            TypeRef vt = cc.type.args.size() < 2 ? TypeRef.DYNAMIC : cc.type.arg(1);
            String witness = ".<" + javaType(kt, true, ctx) + ", " + javaType(vt, true, ctx) + ">";
            if (ctor.equals("unmodifiable") && !isPrimitiveLongMap(cc.type)) {
                // Map.unmodifiable / UnmodifiableMapView: every write throws UnsupportedError.
                String src = emitExpr(pos.get(0), null, ctx).code;
                return new Out("DartMap" + witness + "unmodifiable(" + src + ")", cc.type);
            }
            if (ctor.equals("of") || ctor.equals("from") || ctor.equals("unmodifiable")) {
                String src = emitExpr(pos.get(0), null, ctx).code;
                if (isPrimitiveLongMap(cc.type)) {
                    ctx.importClass("dart.core.DartLongMap");
                    return new Out("DartLongMap.from(" + src + ")", cc.type);
                }
                return new Out("DartMap" + witness + "from(" + src + ")", cc.type);
            }
            if (ctor.equals("identity")) {
                return new Out("DartMap" + witness + "identity()", cc.type);
            }
            if (ctor.equals("fromEntries")) {
                // The entries are typed by the map's arguments, so a literal mixing a
                // `MapEntry(k, v)` with a call returning MapEntry<String, int> is one list.
                TypeRef entries = new TypeRef("List");
                TypeRef entry = new TypeRef("MapEntry");
                entry.args.add(kt);
                entry.args.add(vt);
                entries.args.add(entry);
                String src = emitExpr(pos.get(0), entries, ctx).code;
                return new Out("DartMap" + witness + "fromEntries(" + src + ")", cc.type);
            }
            if (ctor.equals("fromIterable")) {
                Out iterO = emitExpr(pos.get(0), null, ctx);
                String iter = iterO.code;
                // The key/value transforms take one element of the iterable; type their lambda
                // param from the element type so member access on it resolves.
                TypeRef elem = iterO.type != null && !iterO.type.args.isEmpty()
                        ? iterO.type.arg(0) : TypeRef.DYNAMIC;
                TypeRef keyFn = inlineFuncType(elem, kt);
                TypeRef valFn = inlineFuncType(elem, vt);
                String key = "null";
                String val = "null";
                for (NamedArg na : cc.args.named) {
                    if (na.name.equals("key")) {
                        key = emitExpr(na.value, keyFn, ctx).code;
                    } else if (na.name.equals("value")) {
                        val = emitExpr(na.value, valFn, ctx).code;
                    }
                }
                // fromIterable is generic in <E, K, V>; emit all three explicitly (a null key/value
                // otherwise leaves K/V uninferable).
                String fiWitness = ".<" + javaType(elem, true, ctx) + ", " + javaType(kt, true, ctx)
                        + ", " + javaType(vt, true, ctx) + ">";
                return new Out("DartMap" + fiWitness + "fromIterable(" + iter + ", " + key + ", " + val + ")", cc.type);
            }
            return null;
        }
        if (name.equals("Set")) {
            ctx.importClass("dart.core.DartSet");
            TypeRef et = cc.type.args.isEmpty() ? TypeRef.DYNAMIC : cc.type.arg(0);
            String witness = ".<" + javaType(et, true, ctx) + ">";
            if (ctor.equals("identity")) {
                return new Out("DartSet" + witness + "identity()", cc.type);
            }
            if (ctor.equals("of") || ctor.equals("from")) {
                String src = emitExpr(pos.get(0), null, ctx).code;
                return new Out("DartSet" + witness + "from(" + src + ")", cc.type);
            }
            if (ctor.equals("castFrom") && !pos.isEmpty()) {
                // Set.castFrom<S, T>(source) is a view of the same set; type arguments are
                // erased, so the view is the set itself (a copy would lose writes through it).
                Out src = emitExpr(pos.get(0), null, ctx);
                TypeRef target = cc.type.args.size() > 1 ? TypeRef.of("Set", cc.type.arg(1)) : cc.type;
                return new Out("((" + javaType(target, true, ctx) + ") (Object) " + src.code + ")", target);
            }
            return null;
        }
        if (name.equals("Iterable")) {
            if (ctor.equals("empty")) {
                // const Iterable<E>.empty(): an unmodifiable empty sequence.
                ctx.importClass("dart.core.DartIterable");
                TypeRef et = cc.type.args.isEmpty() ? TypeRef.DYNAMIC : cc.type.arg(0);
                String jt = javaType(et, true, ctx);
                return new Out("DartIterable.<" + jt + ">wrap(java.util.Collections.<" + jt + ">emptyList())",
                        TypeRef.of("Iterable", et));
            }
            if (ctor.equals("generate")) {
                ctx.importClass("dart.core.DartIterable");
                TypeRef et = cc.type.args.isEmpty() ? TypeRef.DYNAMIC : cc.type.arg(0);
                String witness = ".<" + javaType(et, true, ctx) + ">";
                String count = emitExpr(pos.get(0), TypeRef.INT, ctx).code;
                if (pos.size() > 1) {
                    String gen = emitExpr(pos.get(1), new TypeRef("IndexedGenerator"), ctx).code;
                    return new Out("DartIterable" + witness + "generate(" + count + ", " + gen + ")", cc.type);
                }
                return new Out("DartIterable" + witness + "generate(" + count + ")", cc.type);
            }
            return null;
        }
        return null;
    }

    /** Duration(days:..,hours:..,..) -> Duration.of(...) with canonical named order. */
    private Out emitDurationOf(Args args, Ctx ctx) {
        ctx.importClass("dart.core.Duration");
        String[] names = {"days", "hours", "minutes", "seconds", "milliseconds", "microseconds"};
        StringBuilder sb = new StringBuilder("Duration.of(");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Expr match = null;
            for (NamedArg na : args.named) {
                if (na.name.equals(names[i])) {
                    match = na.value;
                    break;
                }
            }
            sb.append(match == null ? "0L" : emitExpr(match, TypeRef.INT, ctx).code);
        }
        sb.append(')');
        return new Out(sb.toString(), new TypeRef("Duration"));
    }

    private Out emitCtorCall(String className, Args args, Node posNode, Ctx ctx) {
        return emitCtorCall(className, java.util.Collections.<TypeRef>emptyList(), args, posNode, ctx);
    }

    /**
     * Widgets that look a model up BY TYPE, and so need the Dart type argument at runtime.
     *
     * <p>Java erases {@code Selector<A, S>}, so the runtime cannot recover {@code A} and used
     * to ask the provider chain for the nearest value of ANY type. With more than one model
     * in scope that is right only by luck: Reply has its localizations and its EmailStore
     * above the same Selector, took the localizations, and failed — visibly as a cast error
     * on the desktop, and on iOS as a wrong object that flowed on until an unrelated switch
     * matched nothing and the page came up blank.</p>
     */
    private static boolean readsProvidedValueByType(String className) {
        return className.equals("Selector") || className.equals("Consumer")
                // flutter_bloc's widgets look their bloc up by B the same way.
                || className.equals("BlocBuilder") || className.equals("BlocListener")
                || className.equals("BlocConsumer") || className.equals("BlocSelector");
    }

    /**
     * Emits {@code tmp.providedType(A.class)} for those widgets.
     *
     * <p>Only for a plain class: a generic or dynamic argument has no class literal, and the
     * runtime's Object default (nearest provider) remains — no worse than before.</p>
     */
    private void emitProvidedTypeToken(String tmp, String className, List<TypeRef> typeArgs,
            Ctx ctx) {
        if (!readsProvidedValueByType(className) && !publishesValueByType(className)) {
            return;
        }
        String token = providedTypeToken(typeArgs, ctx);
        if (token != null) {
            ctx.writer().line(tmp + ".providedType(" + token + ");");
        }
    }

    /**
     * Providers that PUBLISH by type. Given their {@code T}, one holding null still
     * matches a lookup for T, so the reader subscribes and rebuilds when the value
     * arrives; without it the lookup went past the provider as if it were not there.
     */
    private static boolean publishesValueByType(String className) {
        return className.equals("Provider") || className.equals("ChangeNotifierProvider")
                || className.equals("BlocProvider") || className.equals("RepositoryProvider");
    }

    /**
     * Stub classes whose behaviour depends on their type argument and that are made with
     * positional arguments only: firebase_ui_auth's {@code AuthStateChangeAction<SignedIn>(cb)}
     * must run its callback for a SignedIn and never hand it a UserCreated, which the erased
     * callback would read as the wrong class. They get {@code .providedType(A.class)}, which
     * answers the object itself.
     */
    private static boolean actsOnTypeArgument(String className) {
        return className.equals("AuthStateChangeAction");
    }

    /** {@code A.class} for the first type argument when it is a plain class, else null. */
    private String providedTypeToken(List<TypeRef> typeArgs, Ctx ctx) {
        if (typeArgs == null || typeArgs.isEmpty() || typeArgs.get(0) == null) {
            return null;
        }
        TypeRef a = typeArgs.get(0);
        if (isTypeVariable(a, ctx)) {
            return null;
        }
        String javaName = javaType(a, true, ctx);
        if (javaName == null || javaName.indexOf('<') >= 0 || javaName.equals("Object")) {
            return null;
        }
        return javaName + ".class";
    }

    private Out emitCtorCall(String className, List<TypeRef> typeArgs, Args args, Node posNode, Ctx ctx) {
        // dart:core intrinsics whose Java stub has no matching named-arg constructor:
        // route to the canonical factory rather than the generic allocate-then-setters path.
        if (className.equals("Duration")) {
            return emitDurationOf(args, ctx);
        }
        if (className.equals("Stopwatch") && args.positional.isEmpty() && args.named.isEmpty()) {
            ctx.importClass("dart.core.Stopwatch");
            return new Out("new Stopwatch()", new TypeRef("Stopwatch"));
        }
        // Resolve against the referencing library's imports (not the flat simple-name map),
        // so a name shared by several files (`Backdrop` in pages/ vs studies/shrine/) binds to
        // the one this library actually imports.
        ClassDecl pc = program.resolveClass(className, ctx.library(), takeCallPrefix(ctx));
        // An app class may share a name with a stub type (new_gallery's routes.dart `Path`
        // vs dart:ui `Path`). Prefer the app class only when it is actually visible to the
        // current library (declared or imported); otherwise fall through to the stub.
        if (pc != null && stubs.isStubClass(className) && !classVisibleFrom(pc, ctx.library())) {
            pc = null;
        }
        if (pc != null) {
            String jn = javaClassName(pc);
            CtorDecl ct = pc.defaultCtor();
            // The written type arguments are part of the result's type (`Box<String>()`
            // is a Box<String>), so members read through it see them; a raw or
            // mismatched instantiation stays raw.
            TypeRef constructed = typeArgs != null && !typeArgs.isEmpty()
                    && typeArgs.size() == pc.typeParams.size()
                    ? TypeRef.of(className, typeArgs.toArray(new TypeRef[0])) : new TypeRef(className);
            String twArgs = witnessArgs(pc, typeArgs, posNode, ctx);
            if (ct != null && ct.isFactory) {
                String fa = canonicalArgs(ct, pc, args, ctx);
                return new Out(jn + ".$create(" + (fa.isEmpty() ? stripTrailingComma(twArgs) : twArgs + fa) + ")",
                        constructed);
            }
            // A generic program class constructed raw (`new Foo(...)`) erases the generics on ALL
            // its members — a constructor param typed Func1<BuildContext, Widget> becomes raw
            // Func1, so a lambda argument gets Object params. A diamond keeps the signatures.
            String diamond = pc.typeParams.isEmpty() ? "" : "<>";
            if (!constructed.args.isEmpty()) {
                // Written out rather than inferred: a diamond in receiver position
                // (`new Box<>().get()`) infers Object, not the String the Dart type says.
                StringBuilder ta = new StringBuilder("<");
                for (int i = 0; i < constructed.args.size(); i++) {
                    ta.append(i > 0 ? ", " : "").append(javaType(constructed.args.get(i), true, ctx));
                }
                diamond = ta.append('>').toString();
            }
            ClassDecl savedOwner = ctx.ctorTypeOwner;
            java.util.Map<String, TypeRef> savedSubst = ctx.ctorTypeSubst;
            if (!constructed.args.isEmpty()) {
                // `Sorted<int>((a, b) => a.compareTo(b))`: the closure's parameters are the
                // written type argument, not the class's unbound K.
                ctx.ctorTypeOwner = pc;
                ctx.ctorTypeSubst = new java.util.HashMap<String, TypeRef>();
                for (int i = 0; i < pc.typeParams.size(); i++) {
                    ctx.ctorTypeSubst.put(pc.typeParams.get(i), constructed.args.get(i));
                }
            }
            try {
                String ca = canonicalArgs(ct, pc, args, ctx);
                return new Out("new " + jn + diamond + "(" + (ca.isEmpty() ? stripTrailingComma(twArgs)
                        : twArgs + ca) + ")", constructed);
            } finally {
                ctx.ctorTypeOwner = savedOwner;
                ctx.ctorTypeSubst = savedSubst;
            }
        }
        Ast.ClassDecl sc = stubs.classes.get(className);
        if (sc == null) {
            diags.error(posNode, "E0135", "Cannot resolve constructor '" + className + "'");
            return new Out("null", TypeRef.DYNAMIC, true);
        }
        String simple = stubSimpleName(className, ctx);
        // A generic stub class constructed raw (`new Foo()`) erases the generics on ALL its
        // instance members — including builder/callback setters whose SAM types don't even
        // mention the type variable — so a lambda passed to such a setter gets Object params.
        // Emit an explicit type witness (diamond) to keep those signatures intact, filling any
        // missing Dart type argument with Object. For the provider/scoped_model builders we
        // also thread the argument into the setter param types so the lambda BODY resolves the
        // model's members (the deferred "constructor-type-argument threading", scoped here).
        Map<String, TypeRef> typeSubst = null;
        String diamond = "";
        List<TypeRef> resultArgs = new ArrayList<TypeRef>();
        if (!sc.typeParams.isEmpty()) {
            if (!typeArgs.isEmpty()) {
                typeSubst = new LinkedHashMap<String, TypeRef>();
            }
            StringBuilder d = new StringBuilder("<");
            for (int i = 0; i < sc.typeParams.size(); i++) {
                TypeRef arg = i < typeArgs.size() ? typeArgs.get(i) : null;
                if (typeSubst != null && arg != null) {
                    typeSubst.put(sc.typeParams.get(i), arg);
                }
                if (i > 0) {
                    d.append(", ");
                }
                d.append(arg != null ? javaType(arg, true, ctx) : "Object");
                resultArgs.add(arg != null ? arg : TypeRef.DYNAMIC);
            }
            d.append('>');
            diamond = d.toString();
        }
        // The instance type carries the (diamond-filled) type arguments so a later assignment into
        // a differently-parameterized target can bridge the Dart-covariance/Java-invariance gap.
        TypeRef stubResultType = resultArgs.isEmpty()
                ? new TypeRef(className)
                : TypeRef.of(className, resultArgs.toArray(new TypeRef[0]));
        Ast.CtorDecl ct = sc.defaultCtor();
        warnUnapplied(ct == null ? null : ct.unapplied, className, args);
        // positional args -> Java constructor arguments
        StringBuilder posArgs = new StringBuilder();
        List<Ast.Param> positionalParams = new ArrayList<Ast.Param>();
        List<Ast.Param> namedParams = new ArrayList<Ast.Param>();
        if (ct != null) {
            for (Ast.Param p : ct.params) {
                if (p.named) {
                    namedParams.add(p);
                } else {
                    positionalParams.add(p);
                }
            }
        }
        for (int i = 0; i < args.positional.size(); i++) {
            TypeRef pt = i < positionalParams.size() ? positionalParams.get(i).type : null;
            // Substitute the class's type parameters (e.g. AlwaysStoppedAnimation<double>'s T)
            // so a numeric literal argument coerces to the instantiated element type.
            if (typeSubst != null && pt != null) {
                pt = substituteTypeParams(pt, typeSubst);
            }
            Out o = emitExpr(args.positional.get(i), pt, ctx);
            if (i > 0) {
                posArgs.append(", ");
            }
            posArgs.append(coerce(o, pt, ctx));
        }
        // Fill omitted optional positional params (Dart's `[int month, int day, ...]`) so the
        // call matches the Java constructor, which takes them all (e.g. DateTime(year) needs
        // month/day/... supplied). Use the declared default, else the type's zero value.
        for (int i = args.positional.size(); i < positionalParams.size(); i++) {
            Ast.Param p = positionalParams.get(i);
            if (i > 0) {
                posArgs.append(", ");
            }
            if (p.defaultValue != null) {
                posArgs.append(coerce(emitExpr(p.defaultValue, p.type, ctx), p.type, ctx));
            } else {
                posArgs.append(zeroValue(p.type));
            }
        }
        if (args.named.isEmpty()) {
            String created = "new " + simple + diamond + "(" + posArgs + ")";
            String token = actsOnTypeArgument(className) ? providedTypeToken(typeArgs, ctx) : null;
            return new Out(token == null ? created : created + ".providedType(" + token + ")", stubResultType);
        }
        // allocate-then-setters (ANF)
        String tmp = ctx.newTemp();
        ctx.writer().line("var " + tmp + " = new " + simple + diamond + "(" + posArgs + ");");
        emitProvidedTypeToken(tmp, className, typeArgs, ctx);
        for (NamedArg na : args.named) {
            TypeRef pt = null;
            for (Ast.Param p : namedParams) {
                if (p.name.equals(na.name)) {
                    pt = p.type;
                    break;
                }
            }
            if (pt == null) {
                // inherited named param (e.g. key) — look up stub super chain
                Ast.ClassDecl cur = sc;
                outer:
                while (cur != null && cur.superclass != null) {
                    cur = stubs.classes.get(cur.superclass.name);
                    if (cur == null) {
                        break;
                    }
                    Ast.CtorDecl sct = cur.defaultCtor();
                    if (sct != null) {
                        for (Ast.Param p : sct.params) {
                            if (p.named && p.name.equals(na.name)) {
                                pt = p.type;
                                break outer;
                            }
                        }
                    }
                }
            }
            if (typeSubst != null && pt != null) {
                pt = substituteTypeParams(pt, typeSubst);
            }
            Out v = emitExpr(na.value, pt, ctx);
            ctx.writer().line(tmp + "." + javaMethodName(na.name) + "(" + coerce(v, pt, ctx) + ");");
        }
        return new Out(tmp, stubResultType);
    }

    /** Finds a Dart {@code set name(v)} declared on an app class or its app superclasses. */
    private Ast.MethodDecl findAppSetter(ClassDecl c, String name) {
        while (c != null) {
            for (Ast.MethodDecl m : c.methods) {
                if (m.isSetter && m.name.equals(name)) {
                    return m;
                }
            }
            c = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        }
        return null;
    }

    /** Whether an app class is declared in, or imported by, the given library. */
    private boolean classVisibleFrom(ClassDecl pc, Library from) {
        if (from == null || pc.ownerLibrary == null) {
            return true;   // no library context: keep the historical (program-preferred) behavior
        }
        if (pc.ownerLibrary == from) {
            return true;
        }
        for (String uri : from.imports) {
            if (program.resolveImportedLibrary(from, uri) == pc.ownerLibrary) {
                return true;
            }
        }
        return false;
    }

    /** Recursively replaces type-parameter names (e.g. {@code T}, {@code A}) with concrete args. */
    /**
     * The type of {@code member} -- a field, getter or method of a program class --
     * as seen through a receiver {@code receiver} of class {@code receiverDecl}. The
     * declaring class's type variables take the arguments the receiver's type carries
     * down to it through every superclass, mixin and interface on the way, each link
     * substituting its own: {@code T m()} on {@code Base<T>} is a String through a
     * {@code class Sub extends Base<String>}, and {@code X? f} on {@code A<X>} is an int?
     * through a {@code B<int>} of {@code class B<Y> extends A<Y>}. Left as the type
     * variable, {@code sub.m().length} did not resolve. A raw receiver, a raw link on the
     * way, or a variable a generic method declares itself ({@code shadowed}) keeps the
     * declared type.
     */
    private TypeRef asSeenFrom(TypeRef t, Object member, List<String> shadowed,
                               ClassDecl receiverDecl, TypeRef receiver) {
        if (t == null || receiver == null || receiver.args.size() != receiverDecl.typeParams.size()
                || program.classes.get(receiverDecl.name) != receiverDecl) {
            // The chain below is walked by name; a class sharing its name with another
            // library's (new_gallery's two Backdrops) cannot be told apart there.
            return t;
        }
        ClassDecl owner = memberOwner(member);
        if (owner == null || owner.typeParams.isEmpty()) {
            return t;
        }
        List<TypeRef> args = instantiationArgs(receiverDecl.name, receiver.args, owner.name,
                new java.util.HashSet<String>());
        if (args == null || args.size() != owner.typeParams.size()) {
            return t;
        }
        Map<String, TypeRef> subst = new LinkedHashMap<String, TypeRef>();
        for (int i = 0; i < owner.typeParams.size(); i++) {
            String p = owner.typeParams.get(i);
            if (shadowed == null || !shadowed.contains(p)) {
                subst.put(p, args.get(i));
            }
        }
        if (subst.isEmpty()) {
            return t;
        }
        TypeRef out = substituteTypeParams(t, subst);
        if (t.nullable && out != null && !out.nullable) {
            // `T?` with T := String is String?, which the substitution of the bare
            // name does not carry over.
            out = boxType(out);
        }
        return out;
    }

    /**
     * The type arguments {@code target} receives from {@code cls<args>}, searching its
     * superclass, mixins and interfaces depth first; null when no path reaches it.
     */
    private List<TypeRef> instantiationArgs(String cls, List<TypeRef> args, String target,
                                            java.util.Set<String> seen) {
        if (cls.equals(target)) {
            return args;
        }
        if (!seen.add(cls)) {
            return null;
        }
        Ast.ClassDecl decl = program.classes.get(cls);
        if (decl == null) {
            decl = stubs.classes.get(cls);
        }
        if (decl == null) {
            return null;
        }
        List<TypeRef> sups = new ArrayList<TypeRef>();
        if (decl.superclass != null) {
            sups.add(decl.superclass);
        }
        sups.addAll(decl.mixins);
        sups.addAll(decl.interfaces);
        for (TypeRef sup : sups) {
            List<TypeRef> found = instantiationArgs(sup.name,
                    substituteTypeParams(sup.args, decl.typeParams, args), target, seen);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Program class declaring each field and method, built on first use. */
    private java.util.IdentityHashMap<Object, ClassDecl> memberOwners;

    private java.util.IdentityHashMap<Param, ClassDecl> paramOwners;

    private ClassDecl paramOwner(Param p) {
        if (paramOwners == null) {
            paramOwners = new java.util.IdentityHashMap<Param, ClassDecl>();
            for (ClassDecl cd : program.classes.values()) {
                for (MethodDecl m : cd.methods) {
                    for (Param mp : m.params) {
                        paramOwners.put(mp, cd);
                    }
                }
                for (CtorDecl ct : cd.ctors) {
                    for (Param cp : ct.params) {
                        paramOwners.put(cp, cd);
                    }
                }
            }
        }
        return paramOwners.get(p);
    }

    private ClassDecl memberOwner(Object member) {
        if (memberOwners == null) {
            memberOwners = new java.util.IdentityHashMap<Object, ClassDecl>();
            for (ClassDecl cd : program.classes.values()) {
                for (FieldDecl f : cd.fields) {
                    memberOwners.put(f, cd);
                }
                for (MethodDecl m : cd.methods) {
                    memberOwners.put(m, cd);
                }
            }
        }
        return memberOwners.get(member);
    }

    private TypeRef substituteTypeParams(TypeRef t, Map<String, TypeRef> subst) {
        if (t == null) {
            return null;
        }
        if (t.funcParams == null && subst.containsKey(t.name) && t.args.isEmpty()) {
            return subst.get(t.name);
        }
        TypeRef out = new TypeRef(t.name);
        out.nullable = t.nullable;
        for (TypeRef a : t.args) {
            out.args.add(substituteTypeParams(a, subst));
        }
        if (t.funcParams != null) {
            out.funcParams = new ArrayList<TypeRef>();
            for (TypeRef p : t.funcParams) {
                out.funcParams.add(substituteTypeParams(p, subst));
            }
            out.funcReturn = substituteTypeParams(t.funcReturn, subst);
        }
        return out;
    }

    /**
     * A parameter default inlined at a call site. It is written in the declaring class's
     * scope, so a bare name there (`this.onEvent = _noOp`) is that class's static member,
     * which the caller's scope cannot see by its simple name.
     */
    private Out emitDefaultValue(Param p, TypeRef pt, ClassDecl owner, Ctx ctx) {
        // The class whose member declares p: a call through a subclass, or an inherited
        // or abstract method's default (`[decoder = utf8Decoder]` on an abstract reader),
        // names the declaring class's static, not the receiver's.
        ClassDecl declaring = paramOwner(p);
        if (declaring != null) {
            owner = declaring;
        }
        if (owner != null && p.defaultValue instanceof Ident && ctx.lookup(((Ident) p.defaultValue).name) == null) {
            String n = ((Ident) p.defaultValue).name;
            MethodDecl sm = owner.method(n);
            FieldDecl sf = owner.field(n);
            if ((sm != null && sm.isStatic) || (sf != null && sf.isStatic)) {
                ClassDecl saved = ctx.currentClass;
                ctx.currentClass = owner;
                try {
                    return castDefault(emitExpr(p.defaultValue, pt, ctx), pt, ctx);
                } finally {
                    ctx.currentClass = saved;
                }
            }
        }
        return castDefault(emitExpr(p.defaultValue, pt, ctx), pt, ctx);
    }

    /**
     * `Converter<List<int>, String> decoder = textDecoder`: a default whose class is a
     * different, generic-erased Java type (the runtime's Utf8Decoder is a Converter of
     * Object to Object) reaches the parameter's own type through Object.
     */
    private Out castDefault(Out d, TypeRef pt, Ctx ctx) {
        if (pt != null && !pt.args.isEmpty() && pt.funcParams == null && d.type != null
                && d.type.name != null && !d.type.is(pt.name) && !d.type.is("dynamic") && !d.type.is("Null")
                && stubs.isStubClass(d.type.name)) {
            String jt = javaType(pt, false, ctx);
            if (jt != null && !jt.equals("Object")) {
                return new Out("((" + jt + ") (Object) " + d.code + ")", pt);
            }
        }
        return d;
    }

    /** Program-class calls use canonical positional order with defaults inlined. */
    private String canonicalArgs(CtorDecl ct, Args args, Ctx ctx) {
        return canonicalArgs(ct, null, args, ctx);
    }

    private String canonicalArgs(CtorDecl ct, ClassDecl owner, Args args, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        if (ct == null) {
            for (int i = 0; i < args.positional.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(emitExpr(args.positional.get(i), null, ctx).code);
            }
            return sb.toString();
        }
        String reordered = sourceOrderedArgs(ct, owner, args, ctx);
        if (reordered != null) {
            return reordered;
        }
        int posIdx = 0;
        boolean first = true;
        for (Param p : ct.params) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            TypeRef pt = boundParamType(owner, p, ctx);
            if (!p.named) {
                if (posIdx < args.positional.size()) {
                    Expr arg = args.positional.get(posIdx++);
                    // A closure for an untyped parameter of a RUNTIME (stub) class method
                    // (`Stream.fold(0, (a, b) => a + b)`): the Java parameter is a typed
                    // Funcs SAM, so the lambda is left for javac to type. Binding it to an
                    // all-dynamic SAM (what an Object-erased program slot needs) does not
                    // convert to Func2<R, T, Object>.
                    boolean runtimeFnSlot = arg instanceof Lambda && ct.file == STUB_ARGS
                            && pt != null && pt.funcParams == null && pt.funcReturn == null
                            && (pt.is("Object") || isDynamicType(pt));
                    String handler = isStubFunctionSlot(ct, pt) ? objectHandlerSlot(arg, ctx) : null;
                    if (handler != null) {
                        sb.append(handler);
                    } else {
                        Out o = emitExpr(arg, runtimeFnSlot ? null : pt, ctx);
                        sb.append(coerce(o, pt, ctx));
                    }
                } else if (p.defaultValue != null) {
                    sb.append(coerce(emitDefaultValue(p, pt, owner, ctx), pt, ctx));
                } else {
                    sb.append(zeroValue(pt));
                }
            } else {
                NamedArg match = null;
                for (NamedArg na : args.named) {
                    if (na.name.equals(p.name)) {
                        match = na;
                        break;
                    }
                }
                // A program method's typed `onError` (`FutureOr<R> Function(Object,
                // StackTrace)?`) is not this case: its lambda takes the declared type below.
                if (match != null && p.name.equals("onError") && match.value instanceof Lambda
                        && (ct.file == STUB_ARGS || pt == null || pt.funcParams == null)) {
                    // dart.async.Stream.listen's onError (and any other stub parameter
                    // literally named onError): real Dart's onError takes either a
                    // one- or a two-argument handler, which no single concrete Java
                    // parameter type -- nor an overload, once a stub call pads every
                    // omitted trailing named parameter with a bare null -- can express.
                    // Pin the closure to the Funcs type its OWN arity needs, the same
                    // way Future.then's onError: is pinned (pinnedHandler), rather than
                    // leaving it a bare lambda a plain Object parameter cannot accept.
                    // See dart.async.Stream's class comment.
                    sb.append(pinnedErrorHandler((Lambda) match.value, emitExpr(match.value, null, ctx)));
                } else if (match != null) {
                    String handler = isStubFunctionSlot(ct, pt) ? objectHandlerSlot(match.value, ctx) : null;
                    if (handler != null) {
                        sb.append(handler);
                    } else {
                        Out o = emitExpr(match.value, pt, ctx);
                        sb.append(coerce(o, pt, ctx));
                    }
                } else if (p.defaultValue != null) {
                    sb.append(coerce(emitDefaultValue(p, pt, owner, ctx), pt, ctx));
                } else {
                    sb.append(zeroValue(pt));
                }
            }
        }
        reportUnknownNamedArgs(ct, owner, args);
        return sb.toString();
    }

    /**
     * Pins a transpiled {@code onError:} closure literal to the Funcs type its own
     * arity needs: {@code dart.runtime.Funcs.VoidFunc1<Object>} for a one-argument
     * handler ({@code (e) { ... }}), {@code dart.runtime.Funcs.VoidFunc2<Object,
     * Object>} for a two-argument one ({@code (e, st) { ... }}) -- see the call
     * site's comment and {@code dart.async.Stream}'s class comment for why this
     * cannot instead be an overload.
     */
    private String pinnedErrorHandler(Lambda src, Out o) {
        String type = src.params.size() >= 2
                ? "dart.runtime.Funcs.VoidFunc2<Object, Object>"
                : "dart.runtime.Funcs.VoidFunc1<Object>";
        return "(" + type + ") " + paren(o.code);
    }

    /**
     * A closure (or a tear-off) passed to a stub parameter the runtime declares as a
     * bare {@code Function} handler slot -- {@code StreamSubscription.onData/onError/onDone},
     * {@code listen}'s {@code onError}. Dart types these as an untyped
     * {@code Function}, which accepts a handler of any arity, and the runtime dispatches on
     * the Funcs type it is handed; a bare Java lambda or method reference has no target
     * type there ("Object is not a functional interface"). The closure is bound to the
     * Funcs SAM of its OWN arity, value- or void-returning as its body is. Answers null for
     * anything else, which keeps its default emission: other stub parameters declared
     * Object ({@code Stream.fold}'s combine) are typed Funcs SAMs in Java.
     */
    /**
     * {@code recv.m} / {@code m} naming an instance method of an application class, as
     * {@code (a, ..) => recv.m(a, ..)} over the method's required positional parameters
     * (zero to three); anything else is returned as it is. The receiver must be a name
     * or {@code this}, whose re-evaluation per call is free.
     */
    private Expr tearOffWithOwnArity(Expr e, Ctx ctx) {
        Expr recv;
        String name;
        ClassDecl cls;
        if (e instanceof PropertyGet && !((PropertyGet) e).nullAware
                && (((PropertyGet) e).target instanceof Ident || ((PropertyGet) e).target instanceof ThisExpr)) {
            recv = ((PropertyGet) e).target;
            name = ((PropertyGet) e).name;
            TypeRef rt = emitExpr(recv, null, ctx).type;
            cls = rt == null ? null : program.classes.get(rt.name);
        } else if (e instanceof Ident && ((Ident) e).typeArgs == null && ctx.currentClass != null
                && ctx.lookup(((Ident) e).name) == null) {
            recv = null;
            name = ((Ident) e).name;
            cls = ctx.currentClass;
        } else {
            return e;
        }
        MethodDecl m = cls == null ? null : findMethodInHierarchy(cls, name);
        if (m == null || m.isStatic || m.isGetter || m.isSetter) {
            return e;
        }
        int arity = 0;
        for (Param p : m.params) {
            if (p.named || p.optional) {
                break;
            }
            arity++;
        }
        if (arity > 3) {
            return e;
        }
        Lambda l = new Lambda();
        Call call = new Call();
        call.target = recv;
        call.name = name;
        for (int i = 0; i < arity; i++) {
            Param p = new Param();
            p.name = "$hArg" + i;
            p.type = TypeRef.DYNAMIC;
            l.params.add(p);
            Ident a = new Ident();
            a.name = p.name;
            call.args.positional.add(a);
        }
        l.exprBody = call;
        return l;
    }

    /**
     * A stub parameter declared as Dart's bare {@code Function} (no signature): the
     * runtime's Java slot for it is a plain Object that dispatches on the Funcs arity it
     * is handed. A stub parameter declared {@code Object} is a typed Funcs SAM in Java
     * ({@code Stream.fold}'s combine) and keeps the default emission.
     */
    private boolean isStubFunctionSlot(CtorDecl ct, TypeRef pt) {
        return ct.file == STUB_ARGS && pt != null && pt.is("Function")
                && pt.funcParams == null && pt.funcReturn == null;
    }

    private String objectHandlerSlot(Expr arg, Ctx ctx) {
        // A tear-off takes the arity of its own function type (tearOffAsErrorLambda reads
        // it); a method of an application class, the arity of its declaration.
        Expr e = arg;
        if (!(e instanceof Lambda)) {
            e = tearOffWithOwnArity(arg, ctx);
            // A method reference into a runtime class (`completer.completeError`): a
            // closure of the arity its own function type gives. A function-typed VALUE
            // (a variable or parameter) is already a Funcs object and passes as it is.
            if (!(e instanceof Lambda) && emitExpr(arg, null, ctx).code.contains("::")) {
                e = tearOffAsErrorLambda(arg, ctx);
            }
        }
        if (!(e instanceof Lambda)) {
            return null;
        }
        Lambda l = (Lambda) e;
        int n = l.params.size();
        if (n > 3) {
            return null;
        }
        Out o = emitExpr(l, null, ctx);
        StringBuilder objs = new StringBuilder();
        for (int i = 0; i < n; i++) {
            // A declared parameter type (`(Object e, StackTrace s) { .. }`) is kept: the
            // body reads it as that type. The runtime calls through the erased SAM.
            TypeRef pt = l.params.get(i).type;
            boolean typed = pt != null && !pt.is("var") && !isDynamicType(pt) && pt.funcParams == null
                    && pt.funcReturn == null && pt.args.isEmpty() && isFullyConcrete(pt);
            objs.append(i == 0 ? "" : ", ").append(typed ? javaType(boxType(pt), true, ctx) : "Object");
        }
        String asValue = "dart.runtime.Funcs.Func" + n + "<" + objs + (n == 0 ? "" : ", ") + "Object>";
        String asVoid = "dart.runtime.Funcs.VoidFunc" + n + (n == 0 ? "" : "<" + objs + ">");
        return pinnedHandler(l, o, true, asValue, asVoid);
    }

    /** Set while emitting a super(...) initializer, where no statement may come first. */
    private boolean inSuperInitializer;

    /**
     * Whether evaluating {@code e} can have an effect another argument could observe:
     * an assignment, an increment, an await, a cascade, any call -- a function, a
     * method on this object or on a value ({@code counter.next()}), a static method, a
     * constructor that is not {@code const} -- a getter on a value, and an index
     * operator. Any of those runs code a user class can give effects, and nothing here
     * knows which ones are pure: {@code Pair(b: counter.next(), a: counter.next())}
     * gave a and b each other's values when calls on a value were assumed pure.
     *
     * <p>A {@code const} constructor, a read through a class name
     * ({@code ReplyColors.white50}, {@code Axis.vertical}), literals and operators
     * over them are free. Treating every call as an effect costs little: sequencing
     * only happens for a program class's constructor or method whose order-sensitive
     * arguments are written out of parameter order, and then costs one temp per
     * argument -- a fraction of a percent of the gallery's compiled classes.</p>
     */
    private static boolean hasEffect(Expr e) {
        if (e == null || e instanceof IntLit || e instanceof DoubleLit || e instanceof BoolLit
                || e instanceof NullLit || e instanceof Ident || e instanceof ThisExpr
                || e instanceof SuperExpr || e instanceof Lambda) {
            return false;
        }
        if (e instanceof Assign || e instanceof IncDec || e instanceof AwaitExpr || e instanceof Cascade) {
            return true;
        }
        if (e instanceof StringLit) {
            for (Object part : ((StringLit) e).parts) {
                if (part instanceof Expr && hasEffect((Expr) part)) {
                    return true;
                }
            }
            return false;
        }
        if (e instanceof PropertyGet) {
            Expr target = ((PropertyGet) e).target;
            if (isClassReference(target)) {
                return false;
            }
            return true;
        }
        if (e instanceof IndexGet) {
            // operator [] is a method call, and a user class can give it effects.
            return true;
        }
        if (e instanceof ParenExpr) {
            return hasEffect(((ParenExpr) e).inner);
        }
        if (e instanceof NotNullAssert) {
            return hasEffect(((NotNullAssert) e).operand);
        }
        if (e instanceof Unary) {
            return hasEffect(((Unary) e).operand);
        }
        if (e instanceof Binary) {
            return hasEffect(((Binary) e).left) || hasEffect(((Binary) e).right);
        }
        if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            return hasEffect(c.condition) || hasEffect(c.thenExpr) || hasEffect(c.elseExpr);
        }
        if (e instanceof CtorCall) {
            CtorCall cc = (CtorCall) e;
            return !cc.isConst || argsHaveEffect(cc.args);
        }
        if (e instanceof Call) {
            return true;
        }
        if (e instanceof ListLit) {
            for (Expr x : ((ListLit) e).elements) {
                if (hasEffect(x)) {
                    return true;
                }
            }
            return false;
        }
        // Anything else is assumed to be able to: sequencing an argument that did
        // not need it costs a temp, while skipping one that did reorders effects.
        return true;
    }

    /** A bare class name used as a receiver: {@code ReplyColors.white50}, {@code Axis.vertical}. */
    private static boolean isClassReference(Expr e) {
        if (!(e instanceof Ident)) {
            return false;
        }
        String n = ((Ident) e).name;
        return n != null && !n.isEmpty() && Character.isUpperCase(n.charAt(0));
    }

    private static boolean argsHaveEffect(Args args) {
        for (Expr x : args.positional) {
            if (hasEffect(x)) {
                return true;
            }
        }
        for (NamedArg na : args.named) {
            if (hasEffect(na.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * An argument whose position relative to an effect matters: one that has an
     * effect, or one that reads a variable, which an effect can change --
     * {@code f(b: x, a: log())} must read x before log() runs. Constants, reads through
     * a class ({@code ReplyColors.white50}) and closures are left in place.
     */
    private static boolean orderSensitive(Expr e) {
        return hasEffect(e) || readsVariable(e);
    }

    /**
     * Whether {@code e} reads a variable anywhere in it, so that an effect evaluated
     * before it could change its value: {@code f(b: n + 1, a: bump())} must read n
     * before bump() runs, exactly as {@code f(b: n, a: bump())} must. A class name
     * used as a receiver is not a variable, and a closure reads nothing until it runs.
     */
    private static boolean readsVariable(Expr e) {
        if (e == null || e instanceof Lambda) {
            return false;
        }
        if (e instanceof Ident) {
            return !isClassReference(e);
        }
        if (e instanceof StringLit) {
            for (Object part : ((StringLit) e).parts) {
                if (part instanceof Expr && readsVariable((Expr) part)) {
                    return true;
                }
            }
            return false;
        }
        if (e instanceof ParenExpr) {
            return readsVariable(((ParenExpr) e).inner);
        }
        if (e instanceof NotNullAssert) {
            return readsVariable(((NotNullAssert) e).operand);
        }
        if (e instanceof Unary) {
            return readsVariable(((Unary) e).operand);
        }
        if (e instanceof Binary) {
            return readsVariable(((Binary) e).left) || readsVariable(((Binary) e).right);
        }
        if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            return readsVariable(c.condition) || readsVariable(c.thenExpr) || readsVariable(c.elseExpr);
        }
        if (e instanceof ListLit) {
            for (Expr x : ((ListLit) e).elements) {
                if (readsVariable(x)) {
                    return true;
                }
            }
            return false;
        }
        if (e instanceof CtorCall) {
            Args args = ((CtorCall) e).args;
            for (Expr x : args.positional) {
                if (readsVariable(x)) {
                    return true;
                }
            }
            for (NamedArg na : args.named) {
                if (readsVariable(na.value)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean sourceBefore(Expr a, Expr b) {
        return a.line < b.line || (a.line == b.line && a.col < b.col);
    }

    /**
     * The argument list of a program-class call whose arguments must be evaluated in a
     * different order from the parameters they fill, or null when the plain
     * canonical-order emission is already faithful.
     *
     * <p>Dart evaluates arguments left to right as written, and named arguments may be
     * written in any order: {@code f(b: log('b'), a: log('a'))} logs b first. Emitting
     * them in the callee's parameter order ran whichever parameter is declared first.
     * That only matters when an argument has an effect ({@link #hasEffect}) and the
     * written order of the {@linkplain #orderSensitive order-sensitive} arguments
     * differs from their parameter order; then each of those is assigned to a temp, in
     * source order, inside
     * the first slot that holds one: {@code f((DartRuntime.seq($t1 = log('b'),
     * DartRuntime.seq($t2 = log('a'), true)) ? $t2 : $t2), $t1)}. Java evaluates
     * arguments left to right, and a conditional over the same temp keeps that slot's
     * exact type. Only the temps' DECLARATIONS are lifted into statements; lifting the
     * evaluation would run an argument even on a branch not taken, as in
     * {@code x == null ? null : Foo(b: x.bar(), a: 1)}.</p>
     *
     * <p>Not applied inside a super(...) initializer, where no declaration may precede
     * the call, nor to an argument whose type a temp cannot name (a function value, or
     * one javac infers); those stay in place.</p>
     */
    private String sourceOrderedArgs(CtorDecl ct, ClassDecl owner, Args args, Ctx ctx) {
        if (args.named.isEmpty() || inSuperInitializer) {
            return null;
        }
        // Which parameter slot each call-site argument fills.
        Map<Expr, Integer> slotOf = new java.util.IdentityHashMap<Expr, Integer>();
        int posIdx = 0;
        boolean anyEffect = false;
        for (int i = 0; i < ct.params.size(); i++) {
            Param p = ct.params.get(i);
            Expr supplied = null;
            if (!p.named) {
                if (posIdx < args.positional.size()) {
                    supplied = args.positional.get(posIdx++);
                }
            } else {
                for (NamedArg na : args.named) {
                    if (na.name.equals(p.name)) {
                        supplied = na.value;
                        break;
                    }
                }
            }
            if (supplied != null) {
                slotOf.put(supplied, Integer.valueOf(i));
                anyEffect |= hasEffect(supplied);
            }
        }
        if (!anyEffect) {
            return null;
        }
        List<Expr> ordered = new ArrayList<Expr>();
        for (Expr e : slotOf.keySet()) {
            if (orderSensitive(e)) {
                ordered.add(e);
            }
        }
        java.util.Collections.sort(ordered, new java.util.Comparator<Expr>() {
            @Override
            public int compare(Expr a, Expr b) {
                return sourceBefore(a, b) ? -1 : sourceBefore(b, a) ? 1 : 0;
            }
        });
        boolean inOrder = true;
        for (int i = 1; i < ordered.size(); i++) {
            if (slotOf.get(ordered.get(i - 1)).intValue() > slotOf.get(ordered.get(i)).intValue()) {
                inOrder = false;
                break;
            }
        }
        if (inOrder) {
            return null;
        }
        String[] slotCode = new String[ct.params.size()];
        List<String> assigns = new ArrayList<String>();
        int anchor = -1;
        String anchorTemp = null;
        TypeRef anchorType = null;
        for (Expr e : ordered) {
            int slot = slotOf.get(e).intValue();
            TypeRef pt = paramType(owner, ct.params.get(slot), ctx);
            Out o = emitExpr(e, pt, ctx);
            TypeRef at = o.type;
            String jt = at == null || at.is("var") || at.funcParams != null || at.is("Function")
                    ? null : javaType(at, false, ctx);
            if (jt == null || (jt.equals("Object") && !isDynamicType(pt))) {
                // No temp can hold this value with the type the slot needs; it stays put.
                slotCode[slot] = coerce(o, pt, ctx);
                continue;
            }
            String tmp = ctx.newTemp();
            ctx.writer().line(jt + " " + tmp + ";");
            assigns.add(tmp + " = " + o.code);
            slotCode[slot] = coerce(new Out(tmp, at, o.fromError), pt, ctx);
            if (anchor < 0 || slot < anchor) {
                anchor = slot;
                anchorTemp = tmp;
                anchorType = at;
            }
        }
        if (anchor < 0) {
            return null;
        }
        ctx.importClass("dart.runtime.DartRuntime");
        String chain = "true";
        for (int k = assigns.size() - 1; k >= 0; k--) {
            chain = "DartRuntime.seq(" + assigns.get(k) + ", " + chain + ")";
        }
        slotCode[anchor] = coerce(new Out("(" + chain + " ? " + anchorTemp + " : " + anchorTemp + ")",
                anchorType), paramType(owner, ct.params.get(anchor), ctx), ctx);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ct.params.size(); i++) {
            Param p = ct.params.get(i);
            String code = slotCode[i];
            if (code == null) {
                TypeRef pt = boundParamType(owner, p, ctx);
                Expr supplied = null;
                for (Map.Entry<Expr, Integer> en : slotOf.entrySet()) {
                    if (en.getValue().intValue() == i) {
                        supplied = en.getKey();
                    }
                }
                if (supplied != null) {
                    code = coerce(emitExpr(supplied, pt, ctx), pt, ctx);
                } else if (p.defaultValue != null) {
                    code = coerce(emitDefaultValue(p, pt, owner, ctx), pt, ctx);
                } else {
                    code = zeroValue(pt);
                }
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(code);
        }
        reportUnknownNamedArgs(ct, owner, args);
        return sb.toString();
    }

    /**
     * Reports every named argument the callee does not declare.
     *
     * <p>Canonical expansion walks the PARAMETERS and looks up an argument for each, so an
     * argument nobody declared is simply never read — it vanishes with no diagnostic and no
     * runtime complaint. That is how {@code ListView.builder(shrinkWrap: true, ...)} came to
     * be built without shrink-wrapping: the value was written, transpiled, and dropped.</p>
     *
     * <p>Silently ignoring what the source asked for is the worst failure mode available
     * here, because the app looks like it works. Reporting turns each one into a line of a
     * to-do list instead: either the runtime grows the parameter, or the gap is a known one.</p>
     */
    private void reportUnknownNamedArgs(CtorDecl ct, ClassDecl owner, Args args) {
        if (ct == null || args == null || args.named == null || args.named.isEmpty()) {
            return;
        }
        for (NamedArg na : args.named) {
            boolean declared = false;
            for (Param p : ct.params) {
                // By NAME alone, deliberately. A parameter written `required T name` inside
                // the braces is not always flagged named by the parser, and requiring the
                // flag reported arguments that are in fact declared and passed correctly -
                // a false positive is fatal for a diagnostic that is meant to become an error.
                if (na.name.equals(p.name)) {
                    declared = true;
                    break;
                }
            }
            if (!declared) {
                String callee = owner != null ? owner.name : "<callee>";
                if (ct.name != null) {
                    callee = callee + "." + ct.name;
                }
                // An ERROR, not a warning. Dart itself rejects an undeclared named
                // argument, so accepting one is our divergence from the language, and the
                // way we diverged was the worst available: the value was dropped and the
                // app looked like it worked. Failing the build is what Flutter does.
                diags.error(na.value, "E0140",
                        callee + " does not declare named argument '" + na.name
                                + "'; add it to the runtime API and its Dart stub");
            }
        }
    }

    /** Program method calls: positional plus named-in-declared-order. */
    private String methodArgs(List<Param> params, Args args, Ctx ctx) {
        CtorDecl fake = new CtorDecl();
        fake.params = params;
        return canonicalArgs(fake, args, ctx);
    }

    /** Stub method calls: canonical positional per stub declaration order. */
    /**
     * A stub method whose LAST parameter is {@code Type? witness} wants the call's first
     * explicit type argument as a class literal there -- bloc's {@code on<E>(handler)},
     * which returns void and so cannot use the return-type witness above. The Dart caller
     * never passes it; a missing or type-variable {@code <E>} passes null (match anything).
     */
    private static boolean hasWitnessParam(Ast.MethodDecl m) {
        if (m.params.isEmpty()) {
            return false;
        }
        Param last = m.params.get(m.params.size() - 1);
        return "witness".equals(last.name) && last.type != null && last.type.is("Type");
    }

    private String witnessArgs(Ast.MethodDecl m, Call c, Ctx ctx) {
        CtorDecl fake = new CtorDecl();
        fake.params = new ArrayList<Param>(m.params.subList(0, m.params.size() - 1));
        String args = canonicalArgs(fake, c.args, ctx);
        String token = "null";
        TypeRef witness = c.typeArgs.isEmpty() ? null : c.typeArgs.get(0);
        if (witness == null && !c.args.positional.isEmpty()
                && c.args.positional.get(0) instanceof Ast.Lambda) {
            // `context.select((PetsCubit c) => c.state.pets)`: <T> is inferred from the
            // selector's declared parameter type, so take the witness from there.
            Ast.Lambda l = (Ast.Lambda) c.args.positional.get(0);
            if (!l.params.isEmpty() && l.params.get(0).type != null
                    && !l.params.get(0).type.is("var") && !l.params.get(0).type.is("dynamic")) {
                witness = l.params.get(0).type;
            }
        }
        if (witness != null && !isTypeVariable(witness, ctx)) {
            String javaName = javaType(witness, true, ctx);
            if (javaName != null) {
                int lt = javaName.indexOf('<');
                token = (lt >= 0 ? javaName.substring(0, lt) : javaName) + ".class";
            }
        }
        return args.isEmpty() ? token : args + ", " + token;
    }

    /**
     * W0002: the call passes a named argument the stub declares {@code @Unapplied} -- one
     * the runtime accepts so the program compiles, and then does nothing with
     * ({@code memCacheWidth}, a progress callback a transport cannot report). Reported at
     * the argument, so the build log names the line that expects more than it will get.
     */
    private void warnUnapplied(java.util.Set<String> unapplied, String owner, Args args) {
        if (unapplied == null || args == null) {
            return;
        }
        for (Ast.NamedArg a : args.named) {
            if (unapplied.contains(a.name)) {
                diags.warn(a.value, "W0002", "'" + a.name + "' of " + owner
                        + " is accepted but not applied on Codename One");
            }
        }
    }

    /** Marks the synthetic decl {@link #stubMethodArgs} builds, compared by identity. */
    private static final String STUB_ARGS = new String("<stub-method-args>");

    private String stubMethodArgs(Ast.MethodDecl m, Args args, Ctx ctx) {
        warnUnapplied(m.unapplied, m.name, args);
        CtorDecl fake = new CtorDecl();
        fake.params = m.params;
        fake.file = STUB_ARGS;
        return canonicalArgs(fake, args, ctx);
    }

    /**
     * Emits a stub method call, recovering the Dart generic {@code <T>} witness
     * the emitter otherwise drops. When the stub method returns one of its own
     * type parameters (a return type that resolves to no known class/enum/core
     * type, e.g. {@code Provider.of<T>} or
     * {@code context.dependOnInheritedWidgetOfExactType<T>}) and the call site
     * supplies a type argument, that argument is:
     * <ul>
     *   <li>passed to the Java method as a trailing {@code T.class} token, so
     *       the runtime can dispatch on the requested type; the Java runtime
     *       method must therefore accept a trailing {@code Class<T>} parameter;</li>
     *   <li>used as the call's static type and applied as a Java cast so member
     *       access on the result type-checks.</li>
     * </ul>
     * Methods that return a concrete type are emitted unchanged.
     */
    /**
     * dart:math's {@code min}, {@code max} and {@code pow} take and return {@code num}:
     * two ints give an int, and pow of an int by a non-negative int is an int. The stubs
     * declare them over double, which turned {@code min(1, 2)} into 1.0 and failed
     * {@code is int}. Two ints use the runtime's long overloads (pow by a non-negative
     * literal uses powInt); a double against anything but an int keeps the double form;
     * an int against a double, or two values typed only num or dynamic, go through the
     * Number versions, which answer one of the arguments unconverted. Null when the
     * call is not one of these.
     */
    private Out mathNumCall(String javaClass, String method, Call c, Ctx ctx) {
        if (!"dart.math.DartMath".equals(javaClass) || c.args.positional.size() != 2
                || !c.args.named.isEmpty()
                || !(method.equals("min") || method.equals("max") || method.equals("pow"))) {
            return null;
        }
        ctx.importClass("dart.math.DartMath");
        Expr ex = c.args.positional.get(0);
        Expr ey = c.args.positional.get(1);
        Out x = emitExpr(ex, null, ctx);
        Out y = emitExpr(ey, null, ctx);
        boolean xi = x.type != null && x.type.is("int") && !x.type.nullable;
        boolean yi = y.type != null && y.type.is("int") && !y.type.nullable;
        boolean xd = x.type != null && x.type.is("double") && !x.type.nullable;
        boolean yd = y.type != null && y.type.is("double") && !y.type.nullable;
        if (xi && yi) {
            if (!method.equals("pow")) {
                return new Out("DartMath." + method + "(" + x.code + ", " + y.code + ")", TypeRef.INT);
            }
            if (ey instanceof IntLit && ((IntLit) ey).value >= 0) {
                return new Out("DartMath.powInt(" + x.code + ", " + y.code + ")", TypeRef.INT);
            }
        }
        // pow with a double on either side is always a double in Dart; min and max
        // answer one of their arguments, so an int beside a double can come back.
        if ((xd && !yi) || (yd && !xi) || (method.equals("pow") && (xd || yd))) {
            // A double against another double -- or against a value this emitter only
            // types as num or dynamic, which in the gallery is a double whose type was
            // lost in arithmetic -- keeps the double form the stubs declare, so it still
            // fits the double slots it flows into.
            return new Out("DartMath." + method + "(" + coerce(x, TypeRef.DOUBLE, ctx) + ", "
                    + coerce(y, TypeRef.DOUBLE, ctx) + ")", TypeRef.DOUBLE);
        }
        // Boxed explicitly: a primitive long reaches a Number parameter only through boxing.
        return new Out("DartMath." + method + "Num(" + boxNumber(x) + ", " + boxNumber(y) + ")",
                TypeRef.of("num"));
    }

    private String boxNumber(Out o) {
        if (o.type != null && o.type.is("int") && !o.type.nullable) {
            return "Long.valueOf(" + o.code + ")";
        }
        if (o.type != null && o.type.is("double") && !o.type.nullable) {
            return "Double.valueOf(" + o.code + ")";
        }
        return "(Number) " + paren(o.code);
    }

    private Out stubCallOut(Ast.MethodDecl m, Call c, String callee, Ctx ctx) {
        return stubCallOut(m, c, callee, ctx, null);
    }

    /**
     * As {@link #stubCallOut(Ast.MethodDecl, Call, String, Ctx)} but with an already
     * type-argument-substituted return type ({@code substReturn}, e.g.
     * {@code ColorTween.evaluate(...)} narrowed from {@code T} to {@code Color}). The
     * explicit-{@code <T>}-witness path still keys off the raw declared return type.
     */
    private Out stubCallOut(Ast.MethodDecl m, Call c, String callee, Ctx ctx, TypeRef substReturn) {
        if (hasWitnessParam(m)) {
            return new Out(callee + "(" + witnessArgs(m, c, ctx) + ")",
                    bindMethodTypeParams(substReturn != null ? substReturn : m.returnType, m, c, ctx));
        }
        String args = stubMethodArgs(m, c.args, ctx);
        TypeRef rt = m.returnType;
        if (rt != null && !isConcreteType(rt)) {
            // The stub method returns one of its own type parameters, so the runtime
            // method takes a trailing Class<T> witness. Recover T from the explicit
            // <T> at the call site, or — when it was inferred and dropped (the common
            // `X.of(context) => context.dependOnInheritedWidgetOfExactType()` shape) —
            // from the enclosing method's return type.
            TypeRef sub = null;
            if (!c.typeArgs.isEmpty()) {
                sub = c.typeArgs.get(0);
                // The explicit <T> can itself be an ENCLOSING generic method's type
                // variable (e.g. `T lookup<T>(BuildContext ctx) => Provider.of<T>(ctx);`),
                // which is erased here same as whereType's <T> -- there is no Class<T> to
                // hand the runtime method, and `T.class` is a compile error besides.
                // Diagnosed rather than lowered; see reportTypeVariableWitness.
                if (reportTypeVariableWitness(c, sub, ctx, "'" + m.name + "<" + sub.name + ">'")) {
                    String placeholderArgs = args.isEmpty() ? "null" : args + ", null";
                    return new Out(callee + "(" + placeholderArgs + ")", sub, true);
                }
            } else if (INFERRED_WITNESS_METHODS.contains(m.name)
                    && ctx.methodReturnType != null && isConcreteType(ctx.methodReturnType)
                    && !ctx.methodReturnType.is("void") && !ctx.methodReturnType.is("dynamic")) {
                // A BuildContext ancestor lookup (`X.of(context) =>
                // context.dependOnInheritedWidgetOfExactType()`) whose <T> was inferred
                // from and dropped by the enclosing return type. Recover it from there.
                // ctx.methodReturnType is already vetted concrete by isConcreteType above,
                // so it is never a type variable here.
                sub = ctx.methodReturnType;
            }
            if (sub != null) {
                // The Java runtime method's Class<T> parameter lets javac infer the
                // return type, so no cast is needed (and a leading cast '(' would
                // trip statementize into wrapping a void setter call).
                String token = javaType(sub, true, ctx) + ".class";
                String all = args.isEmpty() ? token : args + ", " + token;
                return new Out(callee + "(" + all + ")", sub);
            }
        }
        TypeRef result = substReturn != null ? substReturn : rt;
        // isConcreteType above only vets the return type's own head: `Stream<R> map<R>(...)`
        // passes it because "Stream" is a known stub class, leaving the method's OWN type
        // parameter R dangling inside the type argument -- a name the generated Java never
        // declares (javac then rejects the local it types). Resolve any of the method's
        // own generics that still appear anywhere in the result.
        result = bindMethodTypeParams(result, m, c, ctx);
        String typed = explicitTypeArgs(callee, m, c, result, ctx);
        if (!typed.equals(callee)) {
            // javac ignores type arguments on a method that is not generic in Java --
            // Navigator's push<T> answers Future<Object> -- so the result is also cast
            // through Object to what the Dart signature promises. An erased cast: the
            // Java runtime checks only the raw class, which the method already returns.
            String jt = javaType(result, true, ctx);
            if (jt != null && !jt.equals("Object")) {
                return new Out("((" + jt + ") (Object) " + typed + "(" + args + "))", result);
            }
        }
        return new Out(typed + "(" + args + ")", result);
    }

    /**
     * {@code dio.get<List<dynamic>>(path)}: the call's explicit type arguments, handed
     * to the Java method as {@code dio.<DartList<Object>>get(path)}. Without them javac
     * infers Object for a type parameter that only appears in the result, and the
     * {@code var} the result lands in loses the type the Dart code relies on. Only when
     * the arguments line up one to one with the method's own and the result mentions
     * one of them; a non-generic Java method ignores explicit type arguments.
     */
    private String explicitTypeArgs(String callee, Ast.MethodDecl m, Call c, TypeRef result, Ctx ctx) {
        int dot = callee.lastIndexOf('.');
        if (dot <= 0 || c.typeArgs.isEmpty() || c.typeArgs.size() != m.typeParams.size()
                || m.returnType == null || !mentionsAnyTypeParam(m.returnType, m.typeParams)) {
            return callee;
        }
        StringBuilder sb = new StringBuilder();
        for (TypeRef t : c.typeArgs) {
            if (isTypeVariable(t, ctx)) {
                return callee;
            }
            String jt = javaType(t, true, ctx);
            if (jt == null) {
                return callee;
            }
            sb.append(sb.length() == 0 ? "" : ", ").append(jt);
        }
        return callee.substring(0, dot + 1) + "<" + sb + ">" + callee.substring(dot + 1);
    }

    /**
     * Binds a generic stub method's OWN type parameters -- the {@code <R>} of
     * {@code Stream<R> map<R>(...)} or the {@code <T>} of {@code NavigatorState}'s
     * {@code Future<T> push<T>(...)}, as opposed to the receiver class's own type
     * parameter that {@link #stubMemberReturnType} already substitutes from the
     * receiver's instantiation -- wherever one of them still appears inside {@code t}.
     * {@link #isConcreteType} cannot catch this: it only inspects a type's own head, so
     * a {@code Stream<R>} return passes it ("Stream" is a known stub class) and leaves
     * the nested {@code R} behind as a type-variable name with no Java declaration.
     *
     * <p>Resolves each of the method's type parameters, in order: an explicit type
     * argument at the call site ({@code stream.map<String>(...)}); failing that, the
     * inferred return type of a closure-literal argument ({@code (x) => x + 1} binds R
     * to {@code int} -- the {@code map}/{@code then}/{@code expand}/{@code fold}-style
     * idiom where the type parameter is never spelled out); or, failing both, erasure to
     * {@code dynamic}, which {@link #javaType} renders as the raw {@code Object} type
     * argument. The result therefore never names an unbound method type variable.</p>
     */
    private TypeRef bindMethodTypeParams(TypeRef t, Ast.MethodDecl m, Call c, Ctx ctx) {
        return bindMethodTypeParams(t, m.typeParams, m.params, c, ctx);
    }

    private TypeRef bindMethodTypeParams(TypeRef t, FunctionDecl f, Call c, Ctx ctx) {
        return bindMethodTypeParams(t, f.typeParams, f.params, c, ctx);
    }

    private TypeRef bindMethodTypeParams(TypeRef t, List<String> typeParams, List<Param> params, Call c,
                                         Ctx ctx) {
        if (t == null || typeParams.isEmpty() || !mentionsAnyTypeParam(t, typeParams)) {
            return t;
        }
        TypeRef fromClosure = null;
        boolean triedClosure = false;
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
        for (int i = 0; i < typeParams.size(); i++) {
            TypeRef bound = i < c.typeArgs.size() ? c.typeArgs.get(i) : null;
            if (bound == null) {
                // `Stream.fromIterable<T>(Iterable<T>)` called with a List<int>: T is
                // what the matching argument's static type carries in that position.
                bound = inferTypeParamFromArgs(typeParams.get(i), params, c.args, ctx);
            }
            if (bound == null) {
                if (!triedClosure) {
                    fromClosure = inferClosureArgReturnType(c);
                    triedClosure = true;
                }
                bound = fromClosure != null ? fromClosure : TypeRef.DYNAMIC;
            }
            subst.put(typeParams.get(i), bound);
        }
        return substTypeVars(t, subst);
    }

    /**
     * A generic method's type parameter {@code p} recovered from the positional
     * arguments: the first parameter typed {@code p} or {@code G<..p..>} whose argument's
     * static type has the same shape (an Iterable parameter also matches a List or Set
     * argument, whose first type argument is the element). Null when none says.
     */
    private TypeRef inferTypeParamFromArgs(String p, Ast.MethodDecl m, Args args, Ctx ctx) {
        return inferTypeParamFromArgs(p, m.params, args, ctx);
    }

    private TypeRef inferTypeParamFromArgs(String p, List<Param> params, Args args, Ctx ctx) {
        int pi = 0;
        for (Ast.Param prm : params) {
            if (prm.named) {
                continue;
            }
            if (pi >= args.positional.size()) {
                break;
            }
            Expr arg = args.positional.get(pi++);
            if (prm.type == null || !mentionsAnyTypeParam(prm.type, java.util.Collections.singletonList(p))
                    || arg instanceof Lambda) {
                continue;
            }
            Expr bare = arg;
            while (bare instanceof ParenExpr) {
                bare = ((ParenExpr) bare).inner;
            }
            TypeRef at = bare instanceof ListLit && ((ListLit) bare).elementType != null
                    ? TypeRef.of("List", ((ListLit) bare).elementType)
                    : bare instanceof ListLit || bare instanceof SetLit ? untypedLiteralType(bare, ctx)
                    : inferExprTypeQuiet(bare, ctx);
            TypeRef found = unifyTypeParam(p, prm.type, at);
            if (found != null && !isDynamicType(found) && !found.is("var")) {
                return found;
            }
        }
        return null;
    }

    /**
     * A static stub factory spelled with its class's type variable
     * ({@code Stream<T> fromIterable(Iterable<T>)} on {@code Stream<T>}): T is bound from
     * the call's explicit class arguments or its arguments; null leaves the return as is.
     */
    private TypeRef bindStaticClassTypeParams(Ast.MethodDecl m, Ast.ClassDecl sc, Call c, Ctx ctx) {
        return bindStaticClassTypeParams(m, sc, null, c.args, ctx);
    }

    private TypeRef cArgsTypeParam(String p, Ast.ClassDecl sc, List<TypeRef> explicit, Ast.MethodDecl m,
                                   Args args, Ctx ctx) {
        int i = sc.typeParams.indexOf(p);
        if (explicit != null && explicit.size() == sc.typeParams.size()) {
            return explicit.get(i);
        }
        return inferTypeParamFromArgs(p, m, args, ctx);
    }

    private TypeRef bindStaticClassTypeParams(Ast.MethodDecl m, Ast.ClassDecl sc, List<TypeRef> explicit,
                                              Args args, Ctx ctx) {
        if (sc == null || sc.typeParams.isEmpty() || m.returnType == null
                || !mentionsAnyTypeParam(m.returnType, sc.typeParams)) {
            return null;
        }
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
        for (String p : sc.typeParams) {
            if (m.typeParams.contains(p)) {
                continue;
            }
            TypeRef b = cArgsTypeParam(p, sc, explicit, m, args, ctx);
            // Nothing binds it (`Stream.periodic(d, f)`, `Stream.empty()`): dynamic, as
            // Dart instantiates to bounds, never the class's own T leaking into a local's
            // declared Java type.
            subst.put(p, b != null ? b : TypeRef.DYNAMIC);
        }
        return subst.isEmpty() ? null : substTypeVars(m.returnType, subst);
    }

    private static final java.util.Set<String> ITERABLE_LIKE =
            new java.util.HashSet<String>(java.util.Arrays.asList("Iterable", "List", "Set", "Queue"));

    private static TypeRef unifyTypeParam(String p, TypeRef formal, TypeRef actual) {
        if (formal == null || actual == null) {
            return null;
        }
        if (formal.args.isEmpty() && formal.funcParams == null && formal.name.equals(p)) {
            return actual;
        }
        boolean sameHead = formal.name.equals(actual.name)
                || (formal.is("Iterable") && ITERABLE_LIKE.contains(actual.name));
        if (!sameHead || formal.args.size() != actual.args.size()) {
            return null;
        }
        for (int i = 0; i < formal.args.size(); i++) {
            TypeRef r = unifyTypeParam(p, formal.args.get(i), actual.args.get(i));
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /** Whether {@code t} names one of {@code params} anywhere in its (possibly nested) type arguments. */
    private boolean mentionsAnyTypeParam(TypeRef t, List<String> params) {
        if (t == null) {
            return false;
        }
        if (t.args.isEmpty()) {
            return params.contains(t.name);
        }
        for (TypeRef a : t.args) {
            if (mentionsAnyTypeParam(a, params)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The inferred type of a single closure-literal argument's return value, for the
     * {@code map}/{@code then}/{@code expand}/{@code fold}-style idiom where a generic
     * stub method's own type parameter is inferred from the callback rather than
     * spelled out. Reads {@link #lastLambdaExprType}/{@link #lastLambdaReturns}, the
     * fields {@link #emitLambda} leaves behind describing the lambda it most recently
     * emitted -- the same fields {@code Future.catchError}/{@code whenComplete} already
     * consult immediately after emitting their own callback argument, above. That means
     * this must run right after the call's arguments are emitted (never cached across
     * calls), and only once a Lambda literal argument is confirmed present: a call
     * passing a tear-off or a stored function value emits no lambda at all, and would
     * otherwise read a stale value left by some earlier, unrelated closure.
     */
    private TypeRef inferClosureArgReturnType(Call c) {
        boolean hasLambdaArg = false;
        for (Expr e : c.args.positional) {
            if (e instanceof Lambda) {
                hasLambdaArg = true;
                break;
            }
        }
        if (!hasLambdaArg) {
            for (NamedArg na : c.args.named) {
                if (na.value instanceof Lambda) {
                    hasLambdaArg = true;
                    break;
                }
            }
        }
        if (!hasLambdaArg) {
            return null;
        }
        TypeRef t = lastLambdaExprType;
        if (t == null && lastLambdaReturns != null && !lastLambdaReturns.isEmpty()) {
            for (TypeRef r : lastLambdaReturns) {
                TypeRef rr = r == null ? TypeRef.DYNAMIC : r;
                if (t == null) {
                    t = rr;
                } else if (!t.toString().equals(rr.toString())) {
                    t = TypeRef.DYNAMIC;
                }
            }
        }
        if (t == null || t.is("void") || t.is("Null") || t.is("var") || t.is("dynamic")) {
            return null;
        }
        return t;
    }

    /**
     * BuildContext generic ancestor-lookup methods whose {@code <T>} is routinely inferred
     * from the enclosing {@code static X? of(context) => ...} return type rather than written
     * explicitly. For these, when no explicit type argument is present, the witness is recovered
     * from {@code ctx.methodReturnType}.
     */
    private static final java.util.Set<String> INFERRED_WITNESS_METHODS = new java.util.HashSet<String>(
            java.util.Arrays.asList("dependOnInheritedWidgetOfExactType",
                    "findAncestorWidgetOfExactType", "findAncestorStateOfType",
                    "findAncestorRenderObjectOfType", "getInheritedWidgetOfExactType"));

    /**
     * dart:core {@code Duration}'s named int constants (everything except {@code zero}), by
     * their Dart-specified values. The Dart SDK defines the "Per" constants from the four
     * base ratios (1000 micros/milli, 1000 millis/sec, 60 sec/min, 60 min/hour, 24 hour/day);
     * the derived constants are each other's products, laid out here the same way.
     */
    private static final java.util.Map<String, Long> DURATION_INT_CONSTANTS = new java.util.HashMap<String, Long>();

    static {
        DURATION_INT_CONSTANTS.put("microsecondsPerMillisecond", 1000L);
        DURATION_INT_CONSTANTS.put("millisecondsPerSecond", 1000L);
        DURATION_INT_CONSTANTS.put("secondsPerMinute", 60L);
        DURATION_INT_CONSTANTS.put("minutesPerHour", 60L);
        DURATION_INT_CONSTANTS.put("hoursPerDay", 24L);
        DURATION_INT_CONSTANTS.put("microsecondsPerSecond", 1000L * 1000L);
        DURATION_INT_CONSTANTS.put("millisecondsPerMinute", 1000L * 60L);
        DURATION_INT_CONSTANTS.put("secondsPerHour", 60L * 60L);
        DURATION_INT_CONSTANTS.put("microsecondsPerMinute", 1000L * 1000L * 60L);
        DURATION_INT_CONSTANTS.put("millisecondsPerHour", 1000L * 60L * 60L);
        DURATION_INT_CONSTANTS.put("secondsPerDay", 60L * 60L * 24L);
        DURATION_INT_CONSTANTS.put("minutesPerDay", 60L * 24L);
        DURATION_INT_CONSTANTS.put("microsecondsPerHour", 1000L * 1000L * 60L * 60L);
        DURATION_INT_CONSTANTS.put("millisecondsPerDay", 1000L * 60L * 60L * 24L);
        DURATION_INT_CONSTANTS.put("microsecondsPerDay", 1000L * 1000L * 60L * 60L * 24L);
    }

    private static final java.util.Set<String> CONCRETE_CORE = new java.util.HashSet<String>(
            java.util.Arrays.asList("int", "double", "bool", "String", "void", "num",
                    "dynamic", "var", "Object", "Null", "List", "Map", "Set", "Iterable",
                    "Future", "FutureOr", "Duration", "Stopwatch", "Function"));

    /**
     * Whether a type reference names a resolvable concrete type (stub, program
     * or core) rather than an unbound generic type parameter.
     */
    private boolean isConcreteType(TypeRef t) {
        if (t == null) {
            return false;
        }
        String n = t.name;
        return stubs.isStubClass(n) || stubs.isStubEnum(n)
                || program.classes.containsKey(n) || program.enums.containsKey(n)
                || TYPEDEFS.containsKey(n) || program.typedefs.containsKey(n)
                || CONCRETE_CORE.contains(n);
    }

    /**
     * Whether {@code sub} is (transitively) a subtype of {@code sup} across the program and stub
     * class hierarchies. Used to decide when a generic cross-type assignment needs an erasing cast.
     */
    /** Runtime superclasses whose subclasses are plain model data (see emitDynamicMembers). */
    private static final java.util.Set<String> MODEL_BASES =
            new java.util.HashSet<String>(java.util.Arrays.asList("Equatable"));

    /**
     * A user extension member applicable to a receiver of type {@code typeName}: one declared
     * on the type itself, or else on one of its supertypes, nearest first. Dart applies
     * {@code extension on List<int>} to a {@code Uint8List} and {@code extension on Iterable<T>}
     * to any list or set, so the lookup walks the superclass, interface and mixin edges of the
     * program and stub hierarchies, and the core collection edges the stubs leave implicit.
     */
    /**
     * The receiver as the extension's {@code $self} parameter takes it. An extension found on
     * a supertype takes the supertype: a list (a typed list among them) reaching an
     * {@code extension on Iterable<T>} is passed as its iterable view.
     */
    private String extensionSelf(Out target, TypeRef tt, ClassDecl ext, Ctx ctx) {
        if (ext.extensionOn == null || ext.extensionOn.name.equals(tt.name)) {
            return target.code;
        }
        if (ext.extensionOn.is("Iterable") && (tt.is("List") || findUserExtensionChainHasList(tt.name))) {
            return paren(target.code) + ".asIterable()";
        }
        return coerce(target, ext.extensionOn, ctx);
    }

    private boolean findUserExtensionChainHasList(String name) {
        Ast.ClassDecl sc = stubs.classes.get(name);
        if (sc == null) {
            return false;
        }
        for (TypeRef t : sc.interfaces) {
            if (t.is("List")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Of several extensions on one class declaring {@code member}, the one whose `on` type
     * matches the receiver's type arguments exactly: `extension on Iterable<int>` over
     * `extension on Iterable<num>` for a List<int>, as Dart's most-specific rule picks.
     */
    private ClassDecl mostSpecificExtension(ClassDecl found, TypeRef tt, String member, boolean getter) {
        if (found == null || tt == null || tt.args.isEmpty()) {
            return found;
        }
        for (ClassDecl e : program.extensions) {
            if (e == found || e.extensionOn == null || e.ownerLibrary != found.ownerLibrary
                    || !e.extensionOn.name.equals(found.extensionOn.name)
                    || e.extensionOn.args.size() != tt.args.size()
                    || (getter ? e.getter(member) == null : e.method(member) == null)) {
                continue;
            }
            boolean same = true;
            for (int i = 0; i < tt.args.size() && same; i++) {
                same = tt.args.get(i) != null && e.extensionOn.args.get(i).name.equals(tt.args.get(i).name);
            }
            if (same) {
                return e;
            }
        }
        return found;
    }

    private ClassDecl findUserExtension(String typeName, String member, boolean getter, Ctx ctx) {
        ClassDecl direct = program.findExtension(typeName, member, getter, ctx.library());
        if (direct != null || typeName == null) {
            return direct;
        }
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<String>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        seen.add(typeName);
        queue.add(typeName);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (!cur.equals(typeName)) {
                ClassDecl e = program.findExtension(cur, member, getter, ctx.library());
                if (e != null) {
                    return e;
                }
            }
            java.util.List<TypeRef> edges = new java.util.ArrayList<TypeRef>();
            ClassDecl pc = program.classes.get(cur);
            Ast.ClassDecl sc = pc != null ? pc : stubs.classes.get(cur);
            if (sc != null) {
                if (sc.superclass != null) {
                    edges.add(sc.superclass);
                }
                edges.addAll(sc.interfaces);
                edges.addAll(sc.mixins);
            }
            for (TypeRef t : edges) {
                if (t != null && seen.add(t.name)) {
                    queue.add(t.name);
                }
            }
            String implicit = "List".equals(cur) || "Set".equals(cur) || "Queue".equals(cur)
                    ? "Iterable" : null;
            if (implicit != null && seen.add(implicit)) {
                queue.add(implicit);
            }
        }
        return seen.add("Object") ? program.findExtension("Object", member, getter, ctx.library()) : null;
    }

    private static final java.util.Set<String> SEALED_CORE = new java.util.HashSet<String>(
            java.util.Arrays.asList("String", "int", "double", "bool"));

    /**
     * The static type of {@code a ?? b}: the left side made non-null, unless the default
     * is of a type the left can never be -- a String default for a {@code Uri?}
     * ({@code url ?? 'unknown'}) is an Object in Dart, and so in Java. Only the sealed
     * core types are compared, as nothing else can implement them; numbers keep the
     * left side's type.
     */
    private TypeRef ifNullType(TypeRef lt, TypeRef rt) {
        TypeRef base = copyNonNull(lt);
        if (lt == null || rt == null || rt.is("Null") || rt.is("dynamic") || rt.is("var")
                || lt.is("dynamic") || lt.is("var") || lt.is("Object") || lt.funcParams != null
                || rt.funcParams != null || lt.name == null || rt.name == null || lt.name.equals(rt.name)) {
            return base;
        }
        boolean ls = SEALED_CORE.contains(lt.name);
        boolean rs = SEALED_CORE.contains(rt.name);
        if (!ls && !rs) {
            return base;
        }
        if ((lt.is("int") || lt.is("double")) && (rt.is("int") || rt.is("double") || rt.is("num"))
                || (rt.is("int") || rt.is("double")) && lt.is("num")) {
            // Kept as the left's type: an int literal default of a double (`size ?? -1`)
            // is a double in Dart, and the numeric coercions already reconcile the rest.
            return base;
        }
        String other = ls ? rt.name : lt.name;
        if (other.equals("Object") || other.equals("Comparable") || other.equals("Pattern")
                || other.equals("num") || other.equals("FutureOr")) {
            return base;
        }
        return new TypeRef("Object");
    }

    private boolean isSubtypeName(String sub, String sup) {
        if (sub == null || sup == null) {
            return false;
        }
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String cur = sub;
        while (cur != null && seen.add(cur)) {
            if (cur.equals(sup)) {
                return true;
            }
            TypeRef next = null;
            ClassDecl pc = program.classes.get(cur);
            if (pc != null) {
                next = pc.superclass;
            } else {
                Ast.ClassDecl sc = stubs.classes.get(cur);
                if (sc != null) {
                    next = sc.superclass;
                }
            }
            cur = next != null ? next.name : null;
        }
        return false;
    }

    /**
     * Whether {@code value} already inherits exactly the instantiation {@code target} names, in
     * which case Java accepts the assignment as-is and an erasing cast would be pure noise.
     */
    private boolean inheritsSameInstantiation(TypeRef value, TypeRef target) {
        TypeRef inherited = supertypeInstantiation(value.name, value.args, target.name);
        return inherited != null
                && copyNonNull(inherited).toString().equals(copyNonNull(target).toString());
    }

    /**
     * The instantiation of {@code sup} that {@code sub<args>} already inherits, or null when the
     * superclass chain does not reach {@code sup}. From {@code class _MyHomePageState extends
     * State<MyHomePage>} the instantiation of {@code State} seen from {@code _MyHomePageState} is
     * {@code State<MyHomePage>} — which is why assigning one to the other needs no cast.
     *
     * <p>Walks the same chain as {@link #isSubtypeName}, but substitutes each class's type
     * parameters with the arguments carried down from the previous link.</p>
     */
    private TypeRef supertypeInstantiation(String sub, List<TypeRef> args, String sup) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String cur = sub;
        List<TypeRef> curArgs = args;
        while (cur != null && seen.add(cur)) {
            if (cur.equals(sup)) {
                TypeRef t = new TypeRef(cur);
                if (curArgs != null) {
                    t.args.addAll(curArgs);
                }
                return t;
            }
            ClassDecl decl = program.classes.get(cur);
            List<String> params;
            TypeRef next;
            if (decl != null) {
                params = decl.typeParams;
                next = decl.superclass;
            } else {
                Ast.ClassDecl sc = stubs.classes.get(cur);
                if (sc == null) {
                    return null;
                }
                params = sc.typeParams;
                next = sc.superclass;
            }
            if (next == null) {
                return null;
            }
            curArgs = substituteTypeParams(next.args, params, curArgs);
            cur = next.name;
        }
        return null;
    }

    /**
     * A stub mixin member's type seen through {@code class C<E> with M<E>} on a receiver
     * typed {@code C<X>}: the mixin's parameters become the with-clause arguments, and
     * those the receiver's (`QueueList<Result<T>>.first` is a Result<T>, not an E).
     */
    private TypeRef substMixinMember(TypeRef t, TypeRef mixRef, ClassDecl pc, TypeRef receiver) {
        if (t == null) {
            return null;
        }
        Ast.ClassDecl mx = stubs.classes.get(mixRef.name);
        if (mx == null || mx.typeParams.isEmpty() || mixRef.args.size() != mx.typeParams.size()) {
            return t;
        }
        TypeRef r = substituteTypeParam(t, mx.typeParams, mixRef.args);
        if (receiver != null && receiver.args.size() == pc.typeParams.size() && !pc.typeParams.isEmpty()) {
            r = substituteTypeParam(r, pc.typeParams, receiver.args);
        }
        return r;
    }

    /** {@code types} with each occurrence of {@code params[i]} replaced by {@code args[i]}. */
    private List<TypeRef> substituteTypeParams(List<TypeRef> types, List<String> params, List<TypeRef> args) {
        List<TypeRef> out = new ArrayList<TypeRef>();
        for (TypeRef t : types) {
            out.add(substituteTypeParam(t, params, args));
        }
        return out;
    }

    private TypeRef substituteTypeParam(TypeRef t, List<String> params, List<TypeRef> args) {
        if (t == null) {
            return null;
        }
        if (params != null && args != null) {
            int i = params.indexOf(t.name);
            if (i >= 0 && i < args.size()) {
                return args.get(i);
            }
        }
        if (t.args.isEmpty()) {
            return t;
        }
        TypeRef copy = new TypeRef(t.name);
        copy.nullable = t.nullable;
        copy.args.addAll(substituteTypeParams(t.args, params, args));
        return copy;
    }

    /** The top-level function named {@code n} declared in library {@code lib}, or null. */
    private FunctionDecl functionInLibrary(Library lib, String n) {
        if (lib == null) {
            return null;
        }
        for (FunctionDecl f : lib.functions) {
            if (f.name.equals(n)) {
                return f;
            }
        }
        return null;
    }

    /**
     * The expected type of a collection method's per-element lambda -- map, where,
     * forEach, any/every, the *Where searches -- so an untyped parameter gets the
     * element type. Passing nothing left `(e) => e.key` with a dynamic `e`: the member
     * read became a raw field access and the arithmetic a dynamic dispatch. Null (no
     * expectation) when the element type itself is unknown.
     */
    private TypeRef elementLambda(TypeRef elem, TypeRef ret) {
        if (elem == null || elem.is("dynamic") || elem.is("var")) {
            return null;
        }
        return inlineFuncType(elem, ret);
    }

    /**
     * Whether a callback can produce a value: an arrow lambda whose body is not void, or
     * a block lambda that returns a value somewhere outside a nested function. False
     * for anything that is not a lambda literal.
     */
    /**
     * The declared return type of a bare-name tear-off -- an own or inherited method,
     * or a top-level function -- or null when the expression is not one of those.
     */
    private TypeRef tearOffReturn(Expr e, Ctx ctx) {
        if (!(e instanceof Ident)) {
            return null;
        }
        String n = ((Ident) e).name;
        if (ctx.lookup(n) != null) {
            return null;   // a local shadows any method or function of that name
        }
        if (ctx.currentClass != null) {
            MethodDecl md = findMethodInHierarchy(ctx.currentClass, n);
            if (md != null) {
                return declaredReturn(md.returnType);
            }
        }
        FunctionDecl fn = program.functions.get(n);
        return fn != null && !fn.isGetter ? declaredReturn(fn.returnType) : null;
    }

    /** An omitted or inferred return type is emitted as Object, so it produces a value. */
    private static TypeRef declaredReturn(TypeRef t) {
        return t == null || t.is("var") ? TypeRef.DYNAMIC : t;
    }

    private boolean lambdaReturnsValue(Expr e) {
        if (!(e instanceof Lambda)) {
            return false;
        }
        Lambda l = (Lambda) e;
        if (l.exprBody != null) {
            return true;   // decided by the caller from lastLambdaVoid
        }
        return returnsValue(l.body);
    }

    private static boolean returnsValue(Stmt s) {
        if (s == null) {
            return false;
        }
        if (s instanceof ReturnStmt) {
            return ((ReturnStmt) s).value != null;
        }
        if (s instanceof Block) {
            for (Stmt x : ((Block) s).statements) {
                if (returnsValue(x)) {
                    return true;
                }
            }
            return false;
        }
        if (s instanceof IfStmt) {
            return returnsValue(((IfStmt) s).thenStmt) || returnsValue(((IfStmt) s).elseStmt);
        }
        if (s instanceof WhileStmt) {
            return returnsValue(((WhileStmt) s).body);
        }
        if (s instanceof ForStmt) {
            return returnsValue(((ForStmt) s).body);
        }
        if (s instanceof ForInStmt) {
            return returnsValue(((ForInStmt) s).body);
        }
        if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            if (returnsValue(t.tryBlock) || returnsValue(t.finallyBlock)) {
                return true;
            }
            for (CatchClause c : t.catches) {
                if (returnsValue(c.body)) {
                    return true;
                }
            }
            return false;
        }
        if (s instanceof SwitchStmt) {
            for (SwitchCase c : ((SwitchStmt) s).cases) {
                for (Stmt x : c.body) {
                    if (returnsValue(x)) {
                        return true;
                    }
                }
            }
        }
        return false;   // a nested function's returns are its own
    }

    /**
     * A callback lambda cast to the functional type its body fits. A lambda has no type
     * of its own, so it cannot go where the parameter is Object; and a void- or
     * throw-bodied one fits both the value and the void overload of then/catchError,
     * which javac reports as ambiguous. So does a value-bodied arrow lambda whose single
     * expression is itself a Java statement (a method call, assignment, {@code new X(...)}
     * or increment/decrement) -- {@code (v) -> list.add(v)} is ambiguous even though
     * {@code add} returns a value, and so is {@code (v) -> DartRuntime.dynBinary(...)},
     * which a dynamic-operand Dart binary expression lowers to; see
     * {@link #lastLambdaExprIsStatementShaped}. {@code forceCast} casts a value-returning
     * lambda too (an Object parameter); otherwise only an ambiguous one is cast and javac
     * resolves the rest as it always has. Anything that is not a lambda passes as is.
     */
    /**
     * An error-handler tear-off ({@code completer.completeError}, {@code sink.addError},
     * {@code _onError}) rewritten as the closure Dart calls it as: {@code (e, s) => f(e, s)}
     * when f takes a second positional argument, else {@code (e) => f(e)}. Only a receiver
     * whose re-evaluation is free (a name, {@code this}) is rewritten, since the closure
     * reads it per call where the tear-off read it once. Any other expression is returned
     * as it is.
     */
    private Expr tearOffAsErrorLambda(Expr e, Ctx ctx) {
        return tearOffAsLambda(e, -1, ctx);
    }

    /** As {@link #tearOffAsErrorLambda}, with a fixed arity when {@code fixedArity > 0}. */
    private Expr tearOffAsLambda(Expr e, int fixedArity, Ctx ctx) {
        Expr recv = null;
        String name;
        if (e instanceof PropertyGet && !((PropertyGet) e).nullAware) {
            recv = ((PropertyGet) e).target;
            name = ((PropertyGet) e).name;
            if (!(recv instanceof Ident || recv instanceof ThisExpr)) {
                return e;
            }
        } else if (e instanceof Ident && ((Ident) e).typeArgs == null) {
            name = ((Ident) e).name;
        } else {
            return e;
        }
        int arity;
        TypeRef ft = fixedArity > 0 ? null : emitExpr(e, null, ctx).type;
        if (fixedArity > 0 && !emitExpr(e, null, ctx).code.contains("::")) {
            return e;
        } else if (fixedArity > 0) {
            arity = fixedArity;
        } else if (ft != null && ft.funcParams != null) {
            arity = ft.funcParams.size();
        } else if ("completeError".equals(name) || "addError".equals(name)) {
            // Completer.completeError / EventSink.addError: (Object error, [StackTrace? st]).
            arity = 2;
        } else {
            return e;
        }
        if (arity < 1 || arity > 2) {
            return e;
        }
        Lambda l = new Lambda();
        Call call = new Call();
        call.target = recv;
        call.name = name;
        String[] names = {"$onErrE", "$onErrS"};
        for (int i = 0; i < arity; i++) {
            Param p = new Param();
            p.name = names[i];
            p.type = TypeRef.DYNAMIC;
            l.params.add(p);
            Ident arg = new Ident();
            arg.name = names[i];
            call.args.positional.add(arg);
        }
        l.exprBody = call;
        return l;
    }

    private String pinnedHandler(Expr src, Out o, boolean forceCast, String asValue, String asVoid) {
        if (!(src instanceof Lambda)) {
            return o.code;
        }
        boolean value = ((Lambda) src).exprBody != null ? !lastLambdaVoid : lastLambdaNullTail || lambdaReturnsValue(src);
        boolean ambiguous = ((Lambda) src).exprBody != null && lastLambdaExprIsStatementShaped;
        if (value && !forceCast && !ambiguous) {
            return o.code;
        }
        return "(" + (value ? asValue : asVoid) + ") " + paren(o.code);
    }

    /** An inline single-parameter function type {@code (param) -> ret}, for typing a lambda arg. */
    private TypeRef inlineFuncType2(TypeRef p1, TypeRef p2, TypeRef ret) {
        TypeRef t = new TypeRef("Function");
        t.funcParams = new ArrayList<TypeRef>();
        t.funcParams.add(p1 != null ? p1 : TypeRef.DYNAMIC);
        t.funcParams.add(p2 != null ? p2 : TypeRef.DYNAMIC);
        t.funcReturn = ret != null ? ret : TypeRef.DYNAMIC;
        return t;
    }

    private TypeRef inlineFuncType0(TypeRef ret) {
        TypeRef t = new TypeRef("Function");
        t.funcParams = new ArrayList<TypeRef>();
        t.funcReturn = ret;
        return t;
    }

    private TypeRef inlineFuncType(TypeRef param, TypeRef ret) {
        TypeRef t = new TypeRef("Function");
        t.funcParams = new ArrayList<TypeRef>();
        t.funcParams.add(param != null ? param : TypeRef.DYNAMIC);
        t.funcReturn = ret != null ? ret : TypeRef.DYNAMIC;
        return t;
    }

    /** {@link #isConcreteType} extended recursively through all type arguments. */
    private boolean isFullyConcrete(TypeRef t) {
        if (!isConcreteType(t)) {
            return false;
        }
        for (TypeRef a : t.args) {
            if (!isFullyConcrete(a)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Arguments of a call through a dynamic receiver: positional ones as an Object[], named
     * ones as a DartMap. A function literal has no target type there, so it is typed as a
     * function of its own arity over dynamic.
     */
    private String dynArgs(Args args, Ctx ctx) {
        StringBuilder sb = new StringBuilder("new Object[] {");
        for (int i = 0; i < args.positional.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Expr a = args.positional.get(i);
            TypeRef expect = null;
            if (a instanceof Lambda) {
                expect = new TypeRef("Function");
                expect.funcParams = new ArrayList<TypeRef>();
                for (int k = 0; k < ((Lambda) a).params.size(); k++) {
                    expect.funcParams.add(TypeRef.DYNAMIC);
                }
                expect.funcReturn = TypeRef.DYNAMIC;
            }
            Out o = emitExpr(a, expect, ctx);
            sb.append(expect != null ? funcCast(o, expect, ctx) : boxIfPrimitive(o, ctx));
        }
        sb.append("}");
        if (!args.named.isEmpty()) {
            ctx.importClass("dart.core.DartMap");
            sb.append(", DartMap.<String, Object>of(");
            boolean first = true;
            for (NamedArg na : args.named) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append("\"").append(na.name).append("\", ")
                        .append(boxIfPrimitive(emitExpr(na.value, null, ctx), ctx));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private static boolean hasOptionalParam(MethodDecl md) {
        for (Param p : md.params) {
            if (p.optional || p.named || p.defaultValue != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * A method tear-off with optional parameters: the adapter calls {@code recv.name(...)}
     * (a bare call when {@code recv} is null) through the ordinary call path, so named
     * arguments and defaults land where a direct call puts them.
     */
    private Out methodTearOffAdapter(MethodDecl md, final String recv, final String name, final Node pos, Ctx ctx) {
        if (!hasOptionalParam(md) || !md.typeParams.isEmpty() || md.returnType == null) {
            return null;
        }
        List<TypeRef> types = new ArrayList<TypeRef>();
        for (Param p : md.params) {
            types.add(paramValueType(p));
        }
        final List<Param> ordered = orderedParams(md.params);
        final Ctx c = ctx;
        return tearOffAdapter(md.params, types, md.returnType, names -> {
            Call call = new Call().at(pos.file, pos.line, pos.col);
            call.name = name;
            if (recv != null) {
                Ident t = new Ident();
                t.name = recv;
                t.line = pos.line;
                t.col = pos.col;
                call.target = t;
            }
            for (int i = 0; i < ordered.size(); i++) {
                Ident ref = new Ident();
                ref.name = names.get(i);
                ref.line = pos.line;
                ref.col = pos.col;
                if (ordered.get(i).named) {
                    NamedArg na = new NamedArg();
                    na.name = ordered.get(i).name;
                    na.value = ref;
                    call.args.named.add(na);
                } else {
                    call.args.positional.add(ref);
                }
            }
            return emitExpr(call, null, c).code;
        }, ctx);
    }

    /**
     * A closure declaring optional or named parameters ({@code ([x = 1]) => ...}): the
     * same closure with every parameter required, behind a {@link #tearOffAdapter} that
     * restores what the caller omitted. Null for a closure with none.
     */
    private Out optionalParamLambda(final Lambda l, TypeRef expected, final Ctx ctx) {
        boolean opt = false;
        for (Param p : l.params) {
            opt |= p.optional || p.named;
        }
        if (!opt || l.params.size() > 5 || l.isAsync || l.isAsyncStar || l.isSyncStar) {
            return null;
        }
        final List<Param> ordered = orderedParams(l.params);
        List<TypeRef> exp = expected != null && expected.funcParams != null
                && expected.funcParams.size() == ordered.size() ? expected.funcParams : null;
        final Lambda inner = new Lambda();
        inner.line = l.line;
        inner.col = l.col;
        inner.body = l.body;
        inner.exprBody = l.exprBody;
        final TypeRef innerFt = new TypeRef("Function");
        innerFt.funcParams = new ArrayList<TypeRef>();
        List<TypeRef> types = new ArrayList<TypeRef>();
        for (Param p : l.params) {
            int at = ordered.indexOf(p);
            TypeRef pt = p.type == null || p.type.is("var") ? (exp != null ? exp.get(at) : TypeRef.DYNAMIC) : p.type;
            types.add(pt);
        }
        for (Param p : ordered) {
            Param q = new Param();
            q.name = p.name;
            q.type = types.get(l.params.indexOf(p));
            inner.params.add(q);
            innerFt.funcParams.add(q.type);
        }
        TypeRef ret = expected != null && expected.funcReturn != null ? expected.funcReturn : TypeRef.DYNAMIC;
        innerFt.funcReturn = ret;
        return tearOffAdapter(l.params, types, ret, names -> {
            String fn = emitExpr(inner, innerFt, ctx).code;
            return "((" + javaType(innerFt, false, ctx) + ") " + fn + ").call(" + String.join(", ", names) + ")";
        }, ctx);
    }

    /** Parameters in SAM order: required positional, optional positional, then named. */
    private static List<Param> orderedParams(List<Param> params) {
        List<Param> ordered = new ArrayList<Param>();
        for (int pass = 0; pass < 3; pass++) {
            for (Param fp : params) {
                int kind = fp.named ? 2 : fp.optional || fp.defaultValue != null ? 1 : 0;
                if (kind == pass) {
                    ordered.add(fp);
                }
            }
        }
        return ordered;
    }

    /** Whether an omitted argument for a slot of this type is passed as the absent marker. */
    private static boolean absentSlot(TypeRef t) {
        return t == null || t.nullable || t.is("dynamic") || t.is("var") || t.is("Object") || t.is("Null");
    }

    /**
     * A tear-off (or closure) whose function type has optional or named parameters, as a
     * SAM of its full arity: required, optional positional, then named. A caller through a
     * function value fills no defaults, so it passes {@code DartRuntime.absent()} for an
     * omitted nullable slot (null for a non-nullable one) and the adapter restores the
     * declared default -- while an explicit null still arrives as null. The lambda takes
     * raw Object parameters (through the raw SAM) so the marker never meets a bridge cast.
     * {@code call} receives the Dart names of the adapted locals, in SAM order, already
     * declared in scope, and returns the Java code of the call. Null when nothing is
     * optional or the arity is past the Funcs shapes.
     */
    private Out tearOffAdapter(List<Param> params, List<TypeRef> types, TypeRef ret,
            java.util.function.Function<List<String>, String> call, Ctx ctx) {
        List<Param> ordered = new ArrayList<Param>();
        List<TypeRef> otypes = new ArrayList<TypeRef>();
        int required = 0;
        boolean optional = false;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < params.size(); i++) {
                Param fp = params.get(i);
                int kind = fp.named ? 2 : fp.optional || fp.defaultValue != null ? 1 : 0;
                if (kind == pass) {
                    ordered.add(fp);
                    otypes.add(types.get(i));
                }
            }
            if (pass == 0) {
                required = ordered.size();
            }
        }
        optional = required < ordered.size();
        if (!optional || ordered.size() > 5) {
            return null;
        }
        ctx.importClass("dart.runtime.DartRuntime");
        TypeRef ft = new TypeRef("Function");
        ft.funcParams = new ArrayList<TypeRef>(otypes);
        ft.funcRequired = required;
        for (Param fp : ordered) {
            if (fp.named) {
                if (ft.funcNamed == null) {
                    ft.funcNamed = new ArrayList<String>();
                }
                ft.funcNamed.add(fp.name);
            }
        }
        ft.funcReturn = ret != null && !ret.is("var") ? ret : TypeRef.DYNAMIC;
        String sam = javaType(ft, false, ctx);
        int lt = sam.indexOf('<');
        String raw = lt > 0 ? sam.substring(0, lt) : sam;
        boolean isVoid = raw.contains("VoidFunc");
        String tag = ctx.newTemp();
        StringBuilder lp = new StringBuilder();
        StringBuilder pro = new StringBuilder();
        List<String> names = new ArrayList<String>();
        ctx.pushScope();
        String ind = indentStr(ctx.currentIndent() + 1);
        for (int i = 0; i < ordered.size(); i++) {
            Param fp = ordered.get(i);
            TypeRef pt = otypes.get(i) != null ? otypes.get(i) : TypeRef.DYNAMIC;
            String rn = tag + "$r" + i;
            lp.append(i > 0 ? ", " : "").append(rn);
            String dn = tag + "$p" + i;
            String jn = ctx.declareShadowSafe(dn, pt);
            names.add(dn);
            String jt = javaType(pt, true, ctx);
            String cast = jt.equals("Object") ? rn : "(" + jt + ") " + rn;
            String v;
            if (i < required || (fp.named && fp.required)) {
                v = cast;
            } else {
                String def = fp.defaultValue != null ? emitExpr(fp.defaultValue, pt, ctx).code : "null";
                v = (absentSlot(pt) ? "DartRuntime.isAbsent(" : "DartRuntime.isAbsentOrNull(") + rn + ") ? "
                        + (jt.equals("Object") || def.equals("null") ? def : "(" + jt + ") " + def) + " : " + cast;
            }
            pro.append(ind).append(jt).append(' ').append(jn).append(" = ").append(v).append(";\n");
        }
        ctx.pushWriter(ctx.currentIndent() + 1);
        String body = call.apply(names);
        String lifted = ctx.popWriter();
        ctx.popScope();
        String code = "((" + sam + ") (" + raw + ") (" + lp + ") -> {\n" + pro + lifted + ind
                + (isVoid ? statementize(body) + ";" : "return " + body + ";") + "\n"
                + indentStr(ctx.currentIndent()) + "})";
        return new Out(code, ft);
    }

    /** An omitted optional argument to a function value: see {@link #tearOffAdapter}. */
    private String omittedArg(List<TypeRef> slotTypes, int i, Ctx ctx) {
        TypeRef t = slotTypes != null && i < slotTypes.size() ? slotTypes.get(i) : null;
        if (!absentSlot(t)) {
            return "null";
        }
        ctx.importClass("dart.runtime.DartRuntime");
        valueArgsAbsent = true;
        return "DartRuntime.absent()";
    }

    /** Set when the last {@link #valueArgs} passed the absent marker. */
    private boolean valueArgsAbsent;

    /**
     * {@code recv.call(args)} for a function value. When an argument is the absent marker
     * the call goes through the raw SAM: javac casts a generic argument to the slot's
     * instantiated type, and the marker is an Object. The result is cast back.
     */
    private String fnCall(String recv, TypeRef fnType, Args args, Ctx ctx) {
        valueArgsAbsent = false;
        String a = valueArgs(fnType, args, ctx);
        if (!valueArgsAbsent) {
            return recv + ".call(" + a + ")";
        }
        valueArgsAbsent = false;
        String sam = javaType(fnType, false, ctx);
        int lt = sam.indexOf('<');
        String raw = lt > 0 ? sam.substring(0, lt) : sam;
        String rawCall = "((" + raw + ") " + recv + ").call(" + a + ")";
        TypeRef ret = funcResultType(fnType);
        if (raw.contains("VoidFunc") || ret == null || ret.is("void")) {
            return rawCall;
        }
        String jt = javaType(ret, true, ctx);
        return jt.equals("Object") ? rawCall : "((" + jt + ") " + rawCall + ")";
    }

    /**
     * Arguments for invoking a function VALUE through its Funcs SAM. A function type with
     * optional or named parameters is a SAM of its full arity (required, optional
     * positional, then named), so an omitted optional is passed as null and a named
     * argument goes to its slot. Without such a shape this is {@link #plainArgs}.
     */
    private String valueArgs(TypeRef fnType, Args args, Ctx ctx) {
        List<TypeRef> slotTypes;
        int arity;
        int required;
        List<String> named;
        if (fnType == null) {
            return plainArgs(args, ctx);
        }
        if (fnType.funcParams != null) {
            arity = fnType.funcParams.size();
            slotTypes = fnType.funcParams;
            required = fnType.funcRequired;
            named = fnType.funcNamed;
        } else {
            Ast.TypedefDecl td = typedefDecl(fnType.name);
            if (td == null || td.paramTypes == null) {
                return plainArgs(args, ctx);
            }
            arity = td.paramTypes.size();
            slotTypes = td.paramTypes;
            required = td.funcRequired;
            named = td.funcNamed;
        }
        if (required < 0 && (named != null || slotTypes == null || args.positional.size() != arity
                || !args.named.isEmpty())) {
            return plainArgs(args, ctx);
        }
        int positionalSlots = arity - (named == null ? 0 : named.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < positionalSlots; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(i < args.positional.size()
                    ? slotArg(args.positional.get(i), lambdaSlot(args.positional.get(i), slotTypes, i), ctx)
                    : omittedArg(slotTypes, i, ctx));
        }
        if (named != null) {
            for (String nm : named) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                Expr v = null;
                for (int i = 0; i < args.named.size(); i++) {
                    if (args.named.get(i).name.equals(nm)) {
                        v = args.named.get(i).value;
                    }
                }
                sb.append(v == null ? omittedArg(slotTypes, positionalSlots + named.indexOf(nm), ctx)
                        : slotArg(v, lambdaSlot(v, slotTypes, positionalSlots + named.indexOf(nm)), ctx));
            }
        }
        return sb.toString();
    }

    /**
     * A closure argument's target type when invoking a function value: its slot's
     * function type, so the closure is a typed SAM rather than a bare Java lambda with
     * no functional interface to land in. Null for any other argument.
     */
    private String slotArg(Expr arg, TypeRef slot, Ctx ctx) {
        if (arg instanceof Lambda && slot != null && slot.funcParams == null && slot.funcReturn == null
                && (slot.is("Object") || isDynamicType(slot))) {
            // An Object slot (a stub's untyped callback parameter seen through a tear-off
            // value) is no functional interface: bind the closure to its own SAM.
            slot = null;
        }
        String code = emitExpr(arg, slot, ctx).code;
        if (slot == null || code.startsWith("((")) {
            return code;
        }
        // the SAM's own type arguments erase a function slot to Object
        return "((" + javaType(slot, false, ctx) + ") " + code + ")";
    }

    private TypeRef lambdaSlot(Expr arg, List<TypeRef> slotTypes, int i) {
        if (!(arg instanceof Lambda) || slotTypes == null || i < 0 || i >= slotTypes.size()) {
            return null;
        }
        TypeRef t = slotTypes.get(i);
        // an Object slot (a stub's `{Object? reviver}`) still needs a functional
        // interface for the closure: Function gives it the SAM of its arity
        return t != null && isFunctionValued(t) ? t : new TypeRef("Function");
    }

    private String plainArgs(Args args, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.positional.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(emitExpr(args.positional.get(i), null, ctx).code);
        }
        return sb.toString();
    }

    // ==================================================================
    // Types & helpers
    // ==================================================================

    private TypeRef fieldType(FieldDecl f, Ctx ctx) {
        if (f.type != null && !f.type.is("var")) {
            return f.type;
        }
        if (f.initializer != null) {
            // cheap literal-driven inference
            if (f.initializer instanceof IntLit) {
                return TypeRef.INT;
            }
            if (f.initializer instanceof DoubleLit) {
                return TypeRef.DOUBLE;
            }
            if (f.initializer instanceof BoolLit) {
                return TypeRef.BOOL;
            }
            if (f.initializer instanceof StringLit) {
                return TypeRef.STRING;
            }
            // A collection literal written with its type arguments names its own type:
            // `final log = <String>[];` was dynamic, and `log.add(...)` did not resolve.
            if (f.initializer instanceof ListLit && ((ListLit) f.initializer).elementType != null) {
                return TypeRef.of("List", ((ListLit) f.initializer).elementType);
            }
            if (f.initializer instanceof SetLit && ((SetLit) f.initializer).elementType != null) {
                return TypeRef.of("Set", ((SetLit) f.initializer).elementType);
            }
            if (f.initializer instanceof MapLit && ((MapLit) f.initializer).keyType != null
                    && ((MapLit) f.initializer).valueType != null) {
                MapLit m = (MapLit) f.initializer;
                return TypeRef.of("Map", m.keyType, m.valueType);
            }
            // An UNTYPED collection literal: Dart infers its type from the elements, and a
            // field declared from one must be that collection, not dynamic. Left dynamic,
            // `static const xs = [1, 2];` was a Java Object and `xs.add(3)` -- valid Dart,
            // which throws at runtime -- did not even compile.
            TypeRef literal = untypedLiteralType(f.initializer, ctx);
            if (literal != null) {
                return literal;
            }
            // `final x = Foo(1);`, `static const k = Foo.named();`, `var b = Box<int>();`:
            // the class constructed, as a local initialised the same way already gets.
            // Left dynamic, the field was declared Object and `x.member` did not compile.
            TypeRef constructed = constructedType(f.initializer, ctx);
            if (constructed != null) {
                return constructed;
            }
        }
        return TypeRef.DYNAMIC;
    }

    /**
     * The type Dart infers for an untyped collection literal with no context type:
     * {@code List<E>}, {@code Set<E>} or {@code Map<K, V>}, each argument the type every
     * element agrees on, else {@code dynamic} -- so {@code []} is List&lt;dynamic&gt;, as in
     * Dart. Null when {@code e} is not such a literal.
     *
     * <p>Narrower than Dart in one direction only: where the elements' types differ, Dart
     * takes their least upper bound ({@code [1, 2.5]} is List&lt;num&gt;) and this answers
     * dynamic. That is always a sound container for the values, and the emitter has no
     * LUB to compute. Element types come from the literals themselves and from
     * constructor calls, never from identifiers: a field's type is asked for while
     * emitting arbitrary code, whose local scope could shadow the name the initializer
     * means.</p>
     */
    private TypeRef untypedLiteralType(Expr e, Ctx ctx) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        if (e instanceof ListLit && ((ListLit) e).elementType == null) {
            return TypeRef.of("List", commonLiteralType(((ListLit) e).elements, ctx));
        }
        if (e instanceof SetLit && ((SetLit) e).elementType == null) {
            return TypeRef.of("Set", commonLiteralType(((SetLit) e).elements, ctx));
        }
        if (e instanceof MapLit && ((MapLit) e).keyType == null) {
            MapLit m = (MapLit) e;
            if (m.structured) {
                return TypeRef.of("Map", TypeRef.DYNAMIC, TypeRef.DYNAMIC);
            }
            return TypeRef.of("Map", commonLiteralType(m.keys, ctx), commonLiteralType(m.values, ctx));
        }
        return null;
    }

    private TypeRef commonLiteralType(List<Expr> elements, Ctx ctx) {
        TypeRef common = null;
        for (Expr x : elements) {
            TypeRef t = literalElementType(x, ctx);
            if (t == null || isDynamicType(t)) {
                return TypeRef.DYNAMIC;
            }
            if (common == null) {
                common = t;
            } else if (!common.toString().equals(t.toString())) {
                return TypeRef.DYNAMIC;
            }
        }
        return common == null ? TypeRef.DYNAMIC : common;
    }

    private TypeRef literalElementType(Expr x, Ctx ctx) {
        while (x instanceof ParenExpr) {
            x = ((ParenExpr) x).inner;
        }
        if (x instanceof Unary && "-".equals(((Unary) x).op)) {
            Expr operand = ((Unary) x).operand;
            return operand instanceof IntLit ? TypeRef.INT
                    : operand instanceof DoubleLit ? TypeRef.DOUBLE : null;
        }
        if (x instanceof IntLit) {
            return TypeRef.INT;
        }
        if (x instanceof DoubleLit) {
            return TypeRef.DOUBLE;
        }
        if (x instanceof BoolLit) {
            return TypeRef.BOOL;
        }
        if (x instanceof StringLit) {
            return TypeRef.STRING;
        }
        if (x instanceof ListLit && ((ListLit) x).elementType != null) {
            return TypeRef.of("List", ((ListLit) x).elementType);
        }
        if (x instanceof SetLit && ((SetLit) x).elementType != null) {
            return TypeRef.of("Set", ((SetLit) x).elementType);
        }
        if (x instanceof MapLit && ((MapLit) x).keyType != null && ((MapLit) x).valueType != null) {
            return TypeRef.of("Map", ((MapLit) x).keyType, ((MapLit) x).valueType);
        }
        TypeRef nested = untypedLiteralType(x, ctx);
        if (nested != null) {
            return nested;
        }
        return constructedType(x, ctx);
    }

    /**
     * dart:core and dart:async types whose constructors the emitter routes to intrinsic
     * factories with element-specialised results (a {@code List<int>.filled} is a
     * DartLongList); a field initialised by one keeps its old, dynamic type.
     */
    private static final java.util.Set<String> INTRINSIC_CONSTRUCTED = new java.util.HashSet<String>(
            java.util.Arrays.asList("List", "Map", "Set", "Iterable", "Future", "Stream",
                    "int", "double", "num", "String", "bool", "Object", "Function"));

    /**
     * The type a constructor invocation produces, when {@code e} is one the emitter
     * dispatches as such: {@code new}/{@code const} {@code C(...)} or {@code C.named(...)},
     * a bare {@code C(...)} or {@code C<T>(...)}, an import-prefixed {@code p.C(...)}, or
     * an unprefixed {@code C.named(...)} naming a declared constructor. Null otherwise.
     * Mirrors the result type the constructor emission reports, plus the written type
     * arguments.
     */
    private TypeRef constructedType(Expr e, Ctx ctx) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        String cls = null;
        List<TypeRef> typeArgs = null;
        if (e instanceof CtorCall) {
            CtorCall cc = (CtorCall) e;
            if (cc.type == null) {
                return null;
            }
            cls = cc.type.name;
            typeArgs = cc.type.args;
            if (cc.ctorName != null && !hasNamedCtor(cls, cc.ctorName)) {
                return null;
            }
        } else if (e instanceof Call) {
            Call c = (Call) e;
            if (c.name == null) {
                return null;
            }
            if (c.target == null || isImportPrefix(c.target, ctx)) {
                // A top-level function of the same name wins, as it does in emitBareCall.
                if (program.functions.containsKey(c.name) || stubs.functions.containsKey(c.name)) {
                    return null;
                }
                cls = c.name;
                typeArgs = c.typeArgs;
            } else if (c.target instanceof Ident && c.typeArgs.isEmpty()
                    && hasNamedCtor(((Ident) c.target).name, c.name)) {
                cls = ((Ident) c.target).name;
                typeArgs = java.util.Collections.<TypeRef>emptyList();
            } else {
                return null;
            }
        } else {
            return null;
        }
        // dart:collection's concrete maps and sets, `final _bins = HashMap<String, Bin>()`:
        // typed as the core interface they implement, so `_bins[n]` is a Bin? and not
        // dynamic. Only with written type arguments; inferring them is not attempted.
        if (cls != null && typeArgs != null && !program.classes.containsKey(cls)) {
            if (typeArgs.size() == 2 && (cls.equals("HashMap") || cls.equals("LinkedHashMap")
                    || cls.equals("SplayTreeMap"))) {
                return TypeRef.of("Map", typeArgs.get(0), typeArgs.get(1));
            }
            if (typeArgs.size() == 1 && (cls.equals("HashSet") || cls.equals("LinkedHashSet")
                    || cls.equals("SplayTreeSet"))) {
                return TypeRef.of("Set", typeArgs.get(0));
            }
        }
        if (cls == null || INTRINSIC_CONSTRUCTED.contains(cls)
                || (!program.classes.containsKey(cls) && !stubs.classes.containsKey(cls))) {
            return null;
        }
        return TypeRef.of(cls, typeArgs.toArray(new TypeRef[0]));
    }

    /** A program or stub class {@code cls} declaring a constructor named {@code name}. */
    private boolean hasNamedCtor(String cls, String name) {
        Ast.ClassDecl cd = program.classes.get(cls);
        if (cd == null) {
            cd = stubs.classes.get(cls);
        }
        if (cd == null) {
            return false;
        }
        for (CtorDecl ct : cd.ctors) {
            if (name.equals(ct.name)) {
                return true;
            }
        }
        return false;
    }

    private TypeRef typeOfField(ClassDecl c, String name, Ctx ctx) {
        FieldDecl f = c.field(name);
        return f == null ? TypeRef.DYNAMIC : fieldType(f, ctx);
    }

    /** Type of a constructor parameter, resolving this./super. against fields. */
    /** {@link #paramType}, with the constructed class's written type arguments bound. */
    private TypeRef boundParamType(ClassDecl owner, Param p, Ctx ctx) {
        TypeRef t = paramType(owner, p, ctx);
        boolean valueOfOwnTypeParam = owner != null && t != null && t.funcParams == null
                && !owner.typeParams.isEmpty() && !t.args.isEmpty()
                && mentionsAnyTypeParam(t, owner.typeParams);
        if (owner == null || owner != ctx.ctorTypeOwner || ctx.ctorTypeSubst == null || t == null) {
            // `SimplePipe('x', [1, 2])` with the type arguments inferred: a `List<T>`
            // parameter would hand the literal a T that is not in scope at the call site.
            // With no context the literal infers its own element type, which is what Dart
            // infers T from.
            if (valueOfOwnTypeParam && owner != ctx.currentClass) {
                return null;
            }
            return t;
        }
        if (valueOfOwnTypeParam) {
            // `SimplePipe<int>('x', [1, 2])`: the literal is a List<int>, not a List<T>.
            return substituteTypeParams(t, ctx.ctorTypeSubst);
        }
        // Only a callback is retyped: a value argument keeps its declared (erased) type.
        if (t.funcParams != null) {
            return substFunctionType(t, ctx.ctorTypeSubst);
        }
        if (typedefSig(t.name) != null && !t.args.isEmpty()) {
            return substituteTypeParams(t, ctx.ctorTypeSubst);
        }
        return t;
    }

    /** Index of {@code p} among its constructor's positional super parameters, or -1. */
    private static int positionalSuperIndex(ClassDecl c, Param p) {
        if (p.named) {
            return -1;
        }
        for (Ast.CtorDecl k : c.ctors) {
            int i = 0;
            for (Param q : k.params) {
                if (q == p) {
                    return i;
                }
                if (q.isSuper && !q.named) {
                    i++;
                }
            }
        }
        return -1;
    }

    /**
     * The binding of {@code decl}'s type variables to the arguments {@code ref} gives it
     * (`extends Base<int>`), each argument first rewritten through {@code outer} -- the
     * binding of the class that wrote {@code ref}. Null when there is nothing to bind.
     */
    private Map<String, TypeRef> superArgSubst(ClassDecl decl, TypeRef ref, Map<String, TypeRef> outer) {
        if (decl == null || ref == null || decl.typeParams.isEmpty()) {
            return null;
        }
        Map<String, TypeRef> m = new LinkedHashMap<String, TypeRef>();
        for (int i = 0; i < decl.typeParams.size(); i++) {
            TypeRef arg = i < ref.args.size() ? ref.args.get(i) : TypeRef.DYNAMIC;
            m.put(decl.typeParams.get(i), outer == null ? arg : substituteTypeParams(arg, outer));
        }
        return m;
    }

    private TypeRef paramType(ClassDecl c, Param p, Ctx ctx) {
        // A `this.x`/`super.x` initializing formal often carries no written type (parsed as
        // `dynamic`); fall through to resolve it from the field / super constructor instead of
        // returning the erased `dynamic` early.
        if (p.type != null && !p.type.is("var") && !p.type.is("dynamic")) {
            return p.type;
        }
        if (p.isThis && c != null) {
            FieldDecl f = c.field(p.name);
            if (f != null) {
                return fieldType(f, ctx);
            }
        }
        if (p.isSuper && c != null && c.superclass != null) {
            // super.x forwards to the super constructor's parameter named x; resolve its
            // type against the program (app) super chain first, then the stub super chain.
            ClassDecl progCur = superDeclOf(c, c.superclass);
            // The super constructor's parameter is written in the SUPER class's type
            // variables: `_IntBuffer(super.buffer)` over `TypedDataBuffer<E>(List<E> buffer)`
            // with `extends TypedDataBuffer<int>` is a List<int>, never a List<E> that names
            // a variable the subclass does not declare. Carry the substitution up the chain.
            Map<String, TypeRef> sub = superArgSubst(progCur, c.superclass, null);
            // A positional super parameter forwards by POSITION, not by name:
            // `B(super.count)` against `A(this._count)` is A's first positional.
            int superPos = positionalSuperIndex(c, p);
            if (progCur != null && superPos >= 0 && progCur.defaultCtor() != null) {
                int i = 0;
                for (Ast.Param sp : progCur.defaultCtor().params) {
                    if (!sp.named && i++ == superPos) {
                        TypeRef rt = paramType(progCur, sp, ctx);
                        if (rt != null && !rt.is("dynamic")) {
                            return sub == null ? rt : substituteTypeParams(rt, sub);
                        }
                    }
                }
            }
            while (progCur != null) {
                Ast.CtorDecl sct = progCur.defaultCtor();
                if (sct != null) {
                    for (Ast.Param sp : sct.params) {
                        if (sp.name.equals(p.name)) {
                            TypeRef rt = paramType(progCur, sp, ctx);
                            if (rt != null && !rt.is("dynamic")) {
                                return sub == null ? rt : substituteTypeParams(rt, sub);
                            }
                        }
                    }
                }
                // the super param may correspond directly to an inherited field
                FieldDecl sf = progCur.field(p.name);
                if (sf != null) {
                    TypeRef ft = fieldType(sf, ctx);
                    return sub == null || ft == null ? ft : substituteTypeParams(ft, sub);
                }
                ClassDecl next = progCur.superclass != null ? superDeclOf(progCur, progCur.superclass) : null;
                sub = next == null ? sub : superArgSubst(next, progCur.superclass, sub);
                progCur = next;
            }
            // look up the named param type on the stub super chain
            Ast.ClassDecl cur = stubs.classes.get(c.superclass.name);
            Map<String, TypeRef> ssub = superArgSubst(cur, c.superclass, null);
            while (cur != null) {
                Ast.CtorDecl sct = cur.defaultCtor();
                if (sct != null) {
                    for (Ast.Param sp : sct.params) {
                        if (sp.name.equals(p.name)) {
                            return ssub == null || sp.type == null ? sp.type : substituteTypeParams(sp.type, ssub);
                        }
                    }
                }
                Ast.ClassDecl nextStub = cur.superclass != null ? stubs.classes.get(cur.superclass.name) : null;
                ssub = nextStub == null ? ssub : superArgSubst(nextStub, cur.superclass, ssub);
                cur = nextStub;
            }
        }
        return TypeRef.DYNAMIC;
    }

    /** Maps a Dart type to Java source. boxed=true forces reference types. */
    /** True when a type argument of {@code t}, at any depth, is dart:core's Never. */
    private boolean argsMentionNever(TypeRef t) {
        for (TypeRef a : t.args) {
            if ((a.is("Never") && !program.classes.containsKey("Never")) || argsMentionNever(a)) {
                return true;
            }
        }
        return false;
    }

    private String javaType(TypeRef t, boolean boxed, Ctx ctx) {
        if (t == null || t.is("var") || t.is("dynamic") || t.is("Object") || t.is("Null")) {
            return "Object";
        }
        // A method type parameter erased away (see eraseAsFutureOverride) reads as Object.
        if (ctx != null && ctx.erasedMethodTypeVars != null && t.args.isEmpty()
                && ctx.erasedMethodTypeVars.contains(t.name)) {
            return "Object";
        }
        if (t.is("Never") && !program.classes.containsKey("Never")) {
            // dart:core Never, the bottom type: no value ever has it. A method declared to
            // return it always throws, so any reference return type is correct, and as a
            // type argument (`Result<Never>`, `Future<Never>`) Object is the erasure that
            // stays assignable wherever the Dart subtype is.
            return "Object";
        }
        if (t.is("Pattern") && !program.classes.containsKey("Pattern") && !stubs.classes.containsKey("Pattern")) {
            // dart:core Pattern: a String or a RegExp at run time, which share no
            // Java supertype; CoreExt dispatches on the receiver.
            return "Object";
        }
        // A type written with an import prefix (`intl.DateFormat`, `ui.Size`) carries the
        // prefix in its name; drop it so the base name resolves against a stub/program type.
        if (t.name != null && t.name.indexOf('.') > 0) {
            String stripped = stripImportPrefix(t.name);
            if (!stripped.equals(t.name)) {
                // The prefix names the library: `impl.ShelfBase` written in a library that
                // declares its own ShelfBase is the imported one, never the local one.
                String pfx = t.name.substring(0, t.name.indexOf('.'));
                Library from = ctx == null ? null : ctx.library();
                Library target = from == null ? null
                        : program.resolveImportedLibrary(from, from.prefixImports.get(pfx));
                List<ClassDecl> same = program.classesByName.get(stripped);
                t.name = stripped;
                if (target != null && same != null && same.size() > 1) {
                    for (ClassDecl pc : same) {
                        if (program.provides(target, stripped, pc.ownerLibrary)) {
                            String jn = javaClassName(pc);
                            if (!classRefMode && interfacedClasses().contains(pc)) {
                                jn = jn + "$I";
                            }
                            return jn + (t.args.isEmpty() ? "" : genericSuffix(t, ctx));
                        }
                    }
                }
            }
        }
        if (t.name != null && t.name.startsWith("Rec$")) {
            ensureRecordShape(t.name);
            if (t.args.isEmpty()) {
                return t.name;
            }
            StringBuilder rb = new StringBuilder(t.name).append('<');
            for (int i = 0; i < t.args.size(); i++) {
                if (i > 0) {
                    rb.append(", ");
                }
                rb.append(javaType(t.args.get(i), true, ctx));
            }
            return rb.append('>').toString();
        }
        boolean box = boxed || t.nullable;
        if (t.is("int")) {
            return box ? "Long" : "long";
        }
        if (t.is("double")) {
            return box ? "Double" : "double";
        }
        if (t.is("bool")) {
            return box ? "Boolean" : "boolean";
        }
        if (t.is("String")) {
            return "String";
        }
        if (t.is("void")) {
            // `void` as a type argument (e.g. Dart Route<void>) must box to Void;
            // Java has no `void` type argument.
            return box ? "Void" : "void";
        }
        if (t.is("num")) {
            return "Number";
        }
        if (t.is("Type") && !program.classes.containsKey("Type") && !stubs.classes.containsKey("Type")) {
            // dart:core Type: a reified type is its Java class at run time (`T` through its
            // witness, `x.runtimeType`, a type literal), so a declared Type is a Class<?>.
            return "Class<?>";
        }
        if (t.is("List")) {
            String pk = primitiveListKind(t);
            if (pk != null) {
                ctx.importClass("dart.core.Dart" + pk + "List");
                return "Dart" + pk + "List";
            }
            ctx.importClass("dart.core.DartList");
            return "DartList<" + javaType(t.arg(0), true, ctx) + ">";
        }
        if (t.is("Map")) {
            // By what was built, not by the type: only the plain <int, int> literal is a
            // DartLongMap; a structured one (if/for/spread) is built as a DartMap.
            if (isPrimitiveLongMap(t)) {
                ctx.importClass("dart.core.DartLongMap");
                return "DartLongMap";
            }
            ctx.importClass("dart.core.DartMap");
            return "DartMap<" + javaType(t.arg(0), true, ctx) + ", " + javaType(t.arg(1), true, ctx) + ">";
        }
        if (t.is("Set")) {
            ctx.importClass("dart.core.DartSet");
            return "DartSet<" + javaType(t.arg(0), true, ctx) + ">";
        }
        if (t.is("Iterable")) {
            ctx.importClass("dart.core.DartIterable");
            return "DartIterable<" + javaType(t.arg(0), true, ctx) + ">";
        }
        if (t.is("FutureOr")) {
            // FutureOr<T> is a union of a T and a Future<T>; Java has no union, so it is an
            // Object decided at run time (Await.awaitAny, an instanceof Future test), the way
            // a dynamic value is. Mapping it to Future<T> rejected every plain T passed in.
            return "Object";
        }
        if (t.is("Future")) {
            ctx.importClass("dart.async.Future");
            TypeRef a = t.args.isEmpty() ? TypeRef.DYNAMIC : t.arg(0);
            if (a.is("void") || a.is("Null")) {
                a = TypeRef.DYNAMIC;
            }
            return "Future<" + javaType(a, true, ctx) + ">";
        }
        if (t.is("Duration")) {
            ctx.importClass("dart.core.Duration");
            return "Duration";
        }
        if (t.is("Stopwatch")) {
            ctx.importClass("dart.core.Stopwatch");
            return "Stopwatch";
        }
        // user typedef that plainly aliases another type: resolve through to the target
        Ast.TypedefDecl userTd = typedefDecl(t.name);
        if (userTd != null && userTd.aliased != null) {
            return javaType(userTd.aliased, boxed, ctx);
        }
        if (typedefSig(t.name) != null) {
            ctx.importClass("dart.runtime.Funcs");
            // Substitute the typedef's type arguments (e.g. ValueChanged<int> => the `dynamic`
            // placeholder in `void Function(dynamic)` becomes `int`), so a callback of a
            // parameterized typedef renders VoidFunc1<Long> rather than VoidFunc1<Object>.
            TypeRef[] sig = userTypedefSig(t.name, t.args);
            if (sig == null) {
                sig = substituteTypedefTypeArgs(typedefSig(t.name), t.args);
            }
            int arity = sig.length - 1;
            TypeRef ret = sig[arity];
            if (ret.is("void")) {
                if (arity == 0) {
                    return "Funcs.VoidFunc0";
                }
                StringBuilder sb = new StringBuilder("Funcs.VoidFunc").append(arity).append('<');
                for (int i = 0; i < arity; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(javaType(sig[i], true, ctx));
                }
                return sb.append('>').toString();
            }
            StringBuilder sb = new StringBuilder("Funcs.Func").append(arity).append('<');
            for (int i = 0; i < arity; i++) {
                sb.append(javaType(sig[i], true, ctx)).append(", ");
            }
            sb.append(javaType(ret, true, ctx));
            return sb.append('>').toString();
        }
        if (t.is("Function")) {
            // An inline function type (`void Function(int)`) carries its parsed signature;
            // render the matching Funcs.* SAM so callbacks accept lambdas (not Object).
            if (t.funcReturn != null) {
                return funcSamTypeFromRefs(t.funcParams, t.funcReturn, ctx);
            }
            return "Object";
        }
        // app class (preferred when visible) — a disambiguated name resolves a collision with
        // another app class or a stub type of the same simple name.
        String appName = resolveAppClassName(t.name, ctx.library(), ctx);
        if (appName != null) {
            if (!classRefMode) {
                ClassDecl pcl = program.resolveClass(t.name, ctx.library());
                if (pcl != null && interfacedClasses().contains(pcl)) {
                    appName = appName + "$I";
                }
            }
            return appName + (t.args.isEmpty() ? "" : genericSuffix(t, ctx));
        }
        // stub class or enum
        Ast.ClassDecl sc = stubs.classes.get(t.name);
        if (sc != null && sc.javaName != null) {
            ctx.importClass(sc.javaName);
            String simple = sc.javaName.substring(sc.javaName.lastIndexOf('.') + 1);
            if (!t.args.isEmpty()) {
                StringBuilder sb = new StringBuilder(simple).append('<');
                for (int i = 0; i < t.args.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(javaType(t.args.get(i), true, ctx));
                }
                return sb.append('>').toString();
            }
            return simple;
        }
        Ast.EnumDecl own = visibleProgramEnum(t.name, ctx);
        if (own != null) {
            return javaEnumName(own);
        }
        Ast.EnumDecl se = stubs.enums.get(t.name);
        if (se != null && se.javaName != null) {
            ctx.importClass(se.javaName);
            return se.javaName.substring(se.javaName.lastIndexOf('.') + 1);
        }
        Ast.EnumDecl pe = programEnum(t.name, ctx);
        if (pe != null) {
            return javaEnumName(pe);
        }
        // program class / type parameter -- same package
        return t.name + (t.args.isEmpty() ? "" : genericSuffix(t, ctx));
    }

    private String genericSuffix(TypeRef t, Ctx ctx) {
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < t.args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(t.args.get(i), true, ctx));
        }
        return sb.append('>').toString();
    }

    /**
     * The Java class name for an app class. When its simple name collides — with another
     * app class of the same name in a different library (new_gallery has several
     * {@code _FrontLayer} / {@code HomePage} / {@code Backdrop}) or with a stub type (the
     * routes.dart {@code Path} vs dart:ui {@code Path}) — it is disambiguated with a
     * library-path prefix. Non-colliding names are returned unchanged to minimise churn.
     */
    private String javaClassName(ClassDecl c) {
        if (c == null) {
            return null;
        }
        if (c.enumOf != null) {
            return javaEnumName(c.enumOf);
        }
        boolean collides = stubs.isStubClass(c.name) || stubs.isStubEnum(c.name)
                || program.enumsByName.containsKey(c.name);
        List<ClassDecl> byName = program.classesByName.get(c.name);
        if (byName != null && byName.size() > 1) {
            collides = true;
        }
        if (!collides || c.ownerLibrary == null) {
            return c.name;
        }
        return libPathPrefix(c.ownerLibrary) + c.name;
    }

    /** Camel-cased library path (dir + basename, no extension) used to disambiguate class names. */
    private String libPathPrefix(Library lib) {
        String base = lib.fileName.replace('\\', '/');
        if (base.endsWith(".dart")) {
            base = base.substring(0, base.length() - 5);
        }
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < base.length(); i++) {
            char ch = base.charAt(i);
            if (ch == '_' || ch == '-' || ch == '.' || ch == '/') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(ch) : ch);
                up = false;
            }
        }
        return sb.toString();
    }

    /**
     * Resolves a user type name to its (possibly disambiguated) Java class name, or null when
     * no app class of that name is visible from {@code from} (so the caller falls back to a stub).
     */
    private String resolveAppClassName(String name, Library from, Ctx ctx) {
        ClassDecl c = program.resolveClass(name, from);
        if (c == null) {
            return null;
        }
        // When the name also denotes a stub type, only prefer the app class if it is actually
        // visible (declared/imported) from the referencing library.
        if ((stubs.isStubClass(name) || stubs.isStubEnum(name)) && !classVisibleFrom(c, from)) {
            return null;
        }
        return javaClassName(c);
    }

    private String stubSimpleName(String dartName, Ctx ctx) {
        Ast.ClassDecl sc = stubs.classes.get(dartName);
        if (sc != null && sc.javaName != null) {
            ctx.importClass(sc.javaName);
            return sc.javaName.substring(sc.javaName.lastIndexOf('.') + 1);
        }
        return dartName;
    }

    /** The getter/method declaration named {@code name} on an extension declaration, or null. */
    private Ast.MethodDecl extensionMember(Ast.ClassDecl ext, String name, boolean getter) {
        for (Ast.MethodDecl m : ext.methods) {
            if (m.name.equals(name) && m.isGetter == getter && !m.isSetter) {
                return m;
            }
        }
        return null;
    }

    /** Simple Java class name hosting a stub extension's static members; imports its @JavaName. */
    private String stubExtensionSimpleName(Ast.ClassDecl ext, Ctx ctx) {
        if (ext.javaName != null) {
            ctx.importClass(ext.javaName);
            return ext.javaName.substring(ext.javaName.lastIndexOf('.') + 1);
        }
        return ext.name;
    }

    private void importEnum(String dartName, Ctx ctx) {
        if (visibleProgramEnum(dartName, ctx) != null) {
            return;
        }
        Ast.EnumDecl se = stubs.enums.get(dartName);
        if (se != null && se.javaName != null) {
            ctx.importClass(se.javaName);
        }
    }

    private String simpleEnumName(String dartName, Ctx ctx) {
        Ast.EnumDecl own = visibleProgramEnum(dartName, ctx);
        if (own != null) {
            return javaEnumName(own);
        }
        Ast.EnumDecl se = stubs.enums.get(dartName);
        if (se != null && se.javaName != null) {
            return se.javaName.substring(se.javaName.lastIndexOf('.') + 1);
        }
        Ast.EnumDecl pe = programEnum(dartName, ctx);
        return pe != null ? javaEnumName(pe) : dartName;
    }

    /**
     * A user enum the emitting library declares or imports directly, which wins over a stub
     * enum of the same name: stubs are global, so an app's own {@code Priority} or
     * {@code ResponseType} used to be replaced by flutter_local_notifications' or dio's,
     * whatever it imported. Null when no such enum is in scope.
     */
    private Ast.EnumDecl visibleProgramEnum(String name, Ctx ctx) {
        Library from = ctx != null ? ctx.library() : null;
        if (from == null || !stubs.isStubEnum(name)) {
            return null;
        }
        Ast.EnumDecl pe = program.resolveEnum(name, from);
        if (pe == null || pe.ownerLibrary == null) {
            return null;
        }
        if (pe.ownerLibrary == from) {
            return pe;
        }
        for (String uri : from.imports) {
            if (program.resolveImportedLibrary(from, uri) == pe.ownerLibrary) {
                return pe;
            }
        }
        return null;
    }

    /** A user enum by simple name, preferring the one the code being emitted can see. */
    private Ast.EnumDecl programEnum(String name, Ctx ctx) {
        return program.resolveEnum(name, ctx != null ? ctx.library() : null);
    }

    /**
     * The Java name of a user enum: its own name, or library-path prefixed when another
     * user enum or class shares it. Two libraries each declaring a private {@code _Mode}
     * otherwise emitted the same {@code _Mode.java}, and the later one overwrote the first.
     */
    private String javaEnumName(Ast.EnumDecl e) {
        List<Ast.EnumDecl> same = program.enumsByName.get(e.name);
        boolean collides = (same != null && same.size() > 1) || program.classesByName.containsKey(e.name);
        if (!collides || e.ownerLibrary == null) {
            return e.name;
        }
        return libPathPrefix(e.ownerLibrary) + e.name;
    }

    /**
     * Strips a leading import-prefix segment from a type name &mdash; {@code intl.DateFormat}
     * &rarr; {@code DateFormat}, {@code ui.Size} &rarr; {@code Size} &mdash; when the segment
     * before the first dot is a known {@code import '...' as prefix} name (tracked
     * program-wide). In the single-package whole-program model the prefix is redundant once
     * the type is resolved, so the base name resolves against {@code program}/{@link StubRegistry}.
     * Returns the name unchanged when there is no such prefix.
     */
    private String stripImportPrefix(String name) {
        if (name != null) {
            int dot = name.indexOf('.');
            if (dot > 0 && program.importPrefixes.contains(name.substring(0, dot))) {
                String base = name.substring(dot + 1);
                // dart:ui's Image is UiImage in the stubs, since `Image` is the widget.
                if ("Image".equals(base) && dartUiPrefixes().contains(name.substring(0, dot))) {
                    return "UiImage";
                }
                return base;
            }
        }
        return name;
    }

    private java.util.Set<String> dartUiPrefixes;

    /// Every `import 'dart:ui' as p` prefix in the program.
    private java.util.Set<String> dartUiPrefixes() {
        if (dartUiPrefixes == null) {
            dartUiPrefixes = new java.util.HashSet<String>();
            for (Library l : program.libraries) {
                for (Ast.Directive d : l.importDirectives) {
                    if ("dart:ui".equals(d.uri) && d.prefix != null) {
                        dartUiPrefixes.add(d.prefix);
                    }
                }
            }
        }
        return dartUiPrefixes;
    }

    /** Pseudo-type marking a reference to a class itself (for static access). */
    /**
     * The Java class literal a Dart type literal denotes: `int` is the boxed Long the
     * witnesses and runtimeType answer, never the primitive `long.class`.
     */
    private String classLiteral(Out o, Ctx ctx) {
        String dartName = o.type != null && !o.type.args.isEmpty() ? o.type.arg(0).name : null;
        if (dartName == null) {
            return o.code + ".class";
        }
        String jt = javaType(new TypeRef(dartName), true, ctx);
        int lt = jt.indexOf('<');
        return (lt >= 0 ? jt.substring(0, lt) : jt) + ".class";
    }

    private TypeRef classRef(String className) {
        TypeRef t = new TypeRef("$class");
        t.args.add(new TypeRef(className));
        return t;
    }

    /**
     * A user class by simple name, resolved with same-library preference. Two libraries
     * may declare a class of the same name in the single-package model (e.g. the gallery's
     * {@code Backdrop} in pages/ and studies/crane/, or a private {@code _FrontLayer} in
     * two studies); the plain {@code program.classes} map keeps only one, so a
     * {@code widget.<field>} read from within a State resolves against the wrong sibling.
     * Prefer the declaration in the library that owns the code currently being emitted.
     */
    /// The pending call prefix, cleared so the call's own arguments resolve unprefixed.
    private static String takeCallPrefix(Ctx ctx) {
        String p = ctx.callPrefix;
        ctx.callPrefix = null;
        return p;
    }

    private ClassDecl programClass(String name, Ctx ctx) {
        Ast.Library lib = ctx != null && ctx.currentClass != null
                ? ctx.currentClass.ownerLibrary : null;
        return program.resolveClass(name, lib);
    }

    /**
     * A tear-off through a class name -- `Foo.new`, `Foo<T>.new`, `Foo.named`, a static
     * method `Fmt.up`, a stub static such as `DateTime.now` -- desugared into the closure
     * Dart defines it as, `(a, b) => Foo(a, b)`, so it is a typed function value in any
     * context (a list literal, a field initializer, a `??` operand). Only the required
     * positional parameters and the named ones are forwarded: an optional positional one
     * omitted keeps its default, which a forwarded null would not. Null when `name` is
     * not such a member, so the caller's resolution carries on.
     */
    private Out staticTearOff(String cls, List<TypeRef> typeArgs, String name, Node posNode, Ctx ctx) {
        ClassDecl pc = program.classes.get(cls);
        List<Param> params = null;
        Expr body = null;
        List<String> tps = pc != null ? pc.typeParams : java.util.Collections.<String>emptyList();
        if (pc != null && !program.enums.containsKey(cls)) {
            CtorDecl cd = null;
            boolean isCtor = false;
            if (name.equals("new")) {
                for (CtorDecl c : pc.ctors) {
                    if (c.name == null) {
                        cd = c;
                    }
                }
                isCtor = true;
            } else if (pc.namedCtor(name) != null) {
                cd = pc.namedCtor(name);
                isCtor = true;
            }
            if (isCtor) {
                params = cd != null ? cd.params : new ArrayList<Param>();
                CtorCall call = new CtorCall();
                call.type = new TypeRef(cls);
                call.type.args.addAll(typeArgs);
                call.ctorName = name.equals("new") ? null : name;
                body = call;
            } else {
                MethodDecl md = pc.method(name);
                if (md != null && md.isStatic && !md.isGetter && !md.isSetter) {
                    params = md.params;
                    body = staticCallNode(cls, name);
                    tearOffReturn = md.returnType;
                    tps = java.util.Collections.<String>emptyList();
                }
            }
            if (params != null) {
                return emitTearOffLambda(pc, params, tps, typeArgs, body, posNode, ctx);
            }
            return null;
        }
        if (stubs.isStubClass(cls)) {
            Ast.MethodDecl m = stubs.findMethod(cls, name, false);
            if (m != null && m.isStatic && !m.isGetter && !m.isSetter) {
                tearOffReturn = m.returnType;
                return emitTearOffLambda(null, m.params, tps, typeArgs, staticCallNode(cls, name), posNode, ctx);
            }
        }
        return null;
    }

    /** The declared return type of the static method the next tear-off lambda calls. */
    private TypeRef tearOffReturn;

    private Expr staticCallNode(String cls, String name) {
        Call c = new Call();
        Ident t = new Ident();
        t.name = cls;
        c.target = t;
        c.name = name;
        return c;
    }

    private Out emitTearOffLambda(ClassDecl owner, List<Param> params, List<String> tps, List<TypeRef> typeArgs,
            Expr body, Node posNode, Ctx ctx) {
        final Args args = body instanceof CtorCall ? ((CtorCall) body).args : ((Call) body).args;
        boolean hasOptional = false;
        List<TypeRef> ptypes = new ArrayList<TypeRef>();
        for (Param p : params) {
            hasOptional |= p.optional || p.named;
            TypeRef pt = paramValueType(p);
            if ((p.type == null || p.type.is("var")) && (p.isThis || p.isSuper) && owner != null) {
                FieldDecl f = owner.field(p.name);
                pt = f != null ? f.type : pt;
            }
            if (pt != null && tps.contains(pt.name)) {
                int at = tps.indexOf(pt.name);
                pt = at < typeArgs.size() ? typeArgs.get(at) : TypeRef.DYNAMIC;
            }
            ptypes.add(pt);
        }
        if (hasOptional && (tps.isEmpty() || !typeArgs.isEmpty())) {
            // Optional parameters of any kind: the adapter passes what the caller gave,
            // the declared default for what it omitted (see tearOffAdapter).
            final List<Param> ordered = orderedParams(params);
            final Expr callBody = body;
            final Node pos = posNode;
            TypeRef ret = body instanceof CtorCall ? ((CtorCall) body).type
                    : tearOffReturn != null ? tearOffReturn : TypeRef.DYNAMIC;
            Out ad = tearOffAdapter(params, ptypes, ret, names -> {
                for (int i = 0; i < ordered.size(); i++) {
                    Ident ref = new Ident();
                    ref.name = names.get(i);
                    ref.line = pos.line;
                    ref.col = pos.col;
                    if (ordered.get(i).named) {
                        NamedArg na = new NamedArg();
                        na.name = ordered.get(i).name;
                        na.value = ref;
                        args.named.add(na);
                    } else {
                        args.positional.add(ref);
                    }
                }
                callBody.line = pos.line;
                callBody.col = pos.col;
                return emitExpr(callBody, null, ctx).code;
            }, ctx);
            if (ad != null) {
                tearOffReturn = null;
                return ad;
            }
        }
        Lambda l = new Lambda();
        l.line = posNode.line;
        l.col = posNode.col;
        for (Param p : params) {
            if (p.optional) {
                continue;
            }
            Param lp = new Param();
            lp.name = "p$" + p.name;
            lp.named = p.named;
            lp.required = p.required;
            TypeRef pt = p.type;
            if ((pt == null || pt.is("var")) && (p.isThis || p.isSuper) && owner != null) {
                FieldDecl f = owner.field(p.name);
                pt = f != null ? f.type : null;
            }
            if (pt != null && tps.contains(pt.name)) {
                int at = tps.indexOf(pt.name);
                pt = at < typeArgs.size() ? typeArgs.get(at) : TypeRef.DYNAMIC;
            }
            lp.type = pt != null ? pt : TypeRef.DYNAMIC;
            if (p.named && !p.required) {
                // an omitted named argument must keep the callee's default
                lp.defaultValue = p.defaultValue;
            }
            l.params.add(lp);
            Ident ref = new Ident();
            ref.name = lp.name;
            ref.line = posNode.line;
            ref.col = posNode.col;
            if (p.named) {
                NamedArg na = new NamedArg();
                na.name = p.name;
                na.value = ref;
                args.named.add(na);
            } else {
                args.positional.add(ref);
            }
        }
        body.line = posNode.line;
        body.col = posNode.col;
        l.exprBody = body;
        TypeRef ft = new TypeRef("Function");
        ft.funcParams = new ArrayList<TypeRef>();
        boolean simple = true;
        for (Param lp : l.params) {
            if (lp.named) {
                simple = false;
            }
            ft.funcParams.add(lp.type);
        }
        if (body instanceof CtorCall) {
            ft.funcReturn = ((CtorCall) body).type;
        } else {
            ft.funcReturn = tearOffReturn != null ? tearOffReturn : TypeRef.DYNAMIC;
        }
        tearOffReturn = null;
        if (!simple || ft.funcParams.size() > 5 || (!tps.isEmpty() && typeArgs.isEmpty())) {
            // an uninstantiated generic constructor is left for the call site to target-type
            return emitExpr(l, null, ctx);
        }
        Out lo = emitExpr(l, ft, ctx);
        // Cast to the typed SAM so the closure is a standalone expression: a list
        // element, a `??` operand or an untyped local has no target type to bind it.
        return new Out("((" + javaType(ft, false, ctx) + ") " + lo.code + ")", ft);
    }

    private boolean isClassRef(TypeRef t) {
        return t != null && t.is("$class");
    }

    /**
     * Dart's {@code dynamic}: an operand whose operators are chosen at run time. Narrower
     * than {@link #isDynamicType} on purpose -- a {@code var} local is declared with Java's
     * {@code var} and keeps its inferred primitive type, so {@code i + 1} on it already
     * compiles and must stay a Java operator.
     */
    private static boolean isDynamic(TypeRef t) {
        return t != null && t.is("dynamic");
    }

    private boolean isNumeric(TypeRef t) {
        return t != null && (t.is("int") || t.is("double"));
    }

    private boolean containsDynamic(TypeRef t) {
        if (t == null || t.is("dynamic")) {
            return true;
        }
        for (TypeRef a : t.args) {
            if (containsDynamic(a)) {
                return true;
            }
        }
        return false;
    }

    private TypeRef boxType(TypeRef t) {
        if (t == null) {
            return TypeRef.DYNAMIC;
        }
        TypeRef c = TypeRef.of(t.name, t.args.toArray(new TypeRef[0]));
        c.nullable = true;
        c.funcParams = t.funcParams;
        c.funcReturn = t.funcReturn;
        return c;
    }

    private TypeRef copyNonNull(TypeRef t) {
        if (t == null) {
            return TypeRef.DYNAMIC;
        }
        TypeRef c = TypeRef.of(t.name, t.args.toArray(new TypeRef[0]));
        c.nullable = false;
        // An inline function type keeps its signature (in boxType too): without it a
        // callback read through `a?.f` lost its return type, and a value-returning
        // `?.call()` was emitted as a statement that dropped the value.
        c.funcParams = t.funcParams;
        c.funcReturn = t.funcReturn;
        return c;
    }

    /** Whether a Dart type erases to Java Object (untyped/dynamic value). */
    private boolean isDynamicType(TypeRef t) {
        return t == null || t.is("var") || t.is("dynamic") || t.is("Object") || t.is("FutureOr");
    }

    /**
     * Casts a bare lambda/function value to its target SAM type. Needed when the value flows
     * into an {@code Object...} varargs slot (e.g. DartMap.of / DartList.of), where a bare
     * lambda has no functional target and javac reports "Object is not a functional interface".
     */
    private String funcCast(Out o, TypeRef target, Ctx ctx) {
        if (target != null && isFunctionValued(target)) {
            return "(" + javaType(target, true, ctx) + ") " + paren(o.code);
        }
        return o.code;
    }

    /** Whether a parameterized type has a Dart-`dynamic` (Java Object) type argument. */
    private boolean hasDynamicArg(TypeRef t) {
        if (t == null || t.args == null) {
            return false;
        }
        for (TypeRef a : t.args) {
            if (isDynamicType(a)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Element/value coercion for collection literals: applies a functional-target cast for
     * callback elements, and a raw-type bridge when the element type is a `G&lt;dynamic&gt;`
     * (rendered `G&lt;Object&gt;`) but the concrete element is `G&lt;X&gt;` — which Java will not
     * convert to `G&lt;Object&gt;`. The unchecked raw cast mirrors Dart's `dynamic` covariance.
     */
    private String elementCode(Out o, TypeRef elem, Ctx ctx) {
        if (elem != null && isFunctionValued(elem)) {
            return funcCast(o, elem, ctx);
        }
        if (elem != null && hasDynamicArg(elem) && o.type != null && o.type.name != null
                && o.type.name.equals(elem.name) && !o.type.args.isEmpty()
                && !o.type.toString().equals(elem.toString())) {
            String raw = javaType(new TypeRef(elem.name), false, ctx);
            return "(" + raw + ") " + paren(o.code);
        }
        return coerce(o, elem, ctx);
    }

    private String coerce(Out o, TypeRef target, Ctx ctx) {
        if (target == null) {
            return o.code;
        }
        // A Dart type literal (a bare class name used as a value, e.g. `GalleryLocalizations`
        // passed as a `Type` argument) becomes a Java class literal.
        if (o.type != null && isClassRef(o.type) && !isClassRef(target)) {
            return classLiteral(o, ctx);
        }
        // dart:typed_data's TypedDataList is a Java interface beside the list classes, while
        // Dart has it implement List: every implementation is a typed list, so it casts.
        if (o.type != null && o.type.is("TypedDataList") && (target.is("List") || target.is("Iterable"))) {
            return "((" + javaType(target, false, ctx) + ") (Object) " + paren(o.code) + ")";
        }
        // A program class applying ListMixin/ListBase (or IterableMixin/IterableBase) is a
        // Dart List/Iterable, but the runtime list classes keep private storage, so the Java
        // class cannot be one: a live view that reads and writes through the object bridges
        // it at the boundary (dart.core.*ListMixinView, DartIterable.ofMixin).
        if (o.type != null && o.type.name != null && program.classes.containsKey(o.type.name)
                && (target.is("List") || target.is("Iterable"))) {
            String fam = appliedCollectionMixin(o.type.name);
            if ("dart.collection.ListMixin".equals(fam) && target.is("List")) {
                String jt = javaType(copyNonNull(target), false, ctx);
                String view = jt.startsWith("DartLongList") ? "dart.core.LongListMixinView"
                        : jt.startsWith("DartDoubleList") ? "dart.core.DoubleListMixinView"
                        : "dart.core.ListMixinView<>";
                return "new " + view + "(" + o.code + ")";
            }
            if (fam != null && target.is("Iterable") && !"dart.collection.MapMixin".equals(fam)) {
                return "dart.core.DartIterable.ofMixin(" + o.code + ")";
            }
        }
        // The same live bridge for SetMixin/SetBase (a Dart Set) and MapMixin/MapBase (a
        // Dart Map): dart.core.SetMixinView / MapMixinView read and write through the object.
        if (o.type != null && o.type.name != null && program.classes.containsKey(o.type.name)
                && (target.is("Set") || target.is("Map"))) {
            String fam = appliedCollectionMixin(o.type.name);
            if ("dart.collection.SetMixin".equals(fam) && target.is("Set")) {
                return "new dart.core.SetMixinView<>(" + o.code + ")";
            }
            if ("dart.collection.MapMixin".equals(fam) && target.is("Map")) {
                return "new dart.core.MapMixinView<>(" + o.code + ")";
            }
        }
        // Dart's StreamSink and EventSink are Sinks; in Java their close() answers a Future
        // where Sink's is void, so the interfaces cannot be related and Sink.of adapts.
        if (o.type != null && target.is("Sink") && (o.type.is("StreamSink") || o.type.is("EventSink")
                || o.type.is("IOSink") || o.type.is("StreamController"))) {
            ctx.importClass("dart.async.Sink");
            return "Sink.of(" + o.code + ")";
        }
        // A Dart `double` value reaching a Dart `int` target only happens through stub
        // imprecision (e.g. math.max/min typed to return double): Dart itself forbids the
        // implicit narrowing, so the value is known int-valued — truncate to match.
        if (target.is("int") && !target.nullable && o.type != null && o.type.is("double")) {
            return "(long) " + paren(o.code);
        }
        if (target.is("double") && o.type.is("int")) {
            if (o.code.endsWith("L")) {
                String digits = o.code.substring(0, o.code.length() - 1);
                try {
                    Long.parseLong(digits);
                    return digits + ".0";
                } catch (NumberFormatException ignore) {
                    // fall through
                }
            }
            return "((double) " + paren(o.code) + ")";
        }
        // Unbox an Object/dynamic value -- or a num, which is a Java Number -- flowing into
        // a Java primitive numeric target (e.g. an untyped lambda param assigned to a
        // `long`/`double` setter, or num arithmetic passed on as a double).
        if (!target.nullable && (isDynamicType(o.type) || (o.type != null && o.type.is("num")))) {
            if (target.is("int")) {
                return "((Number) " + paren(o.code) + ").longValue()";
            }
            if (target.is("double")) {
                return "((Number) " + paren(o.code) + ").doubleValue()";
            }
        }
        // A Dart List/Set flowing into an Iterable target: the runtime List/Set is not a
        // DartIterable, so bridge with asIterable().
        if (target.is("Iterable") && o.type != null && (o.type.is("List") || o.type.is("Set"))) {
            // `Iterable<int?> x = [1, null, 2]` built the list as List<Object>: the element
            // Java types differ, so the bridged view is retyped through Object.
            if (!target.args.isEmpty() && !o.type.args.isEmpty() && isFullyConcrete(target)
                    && !javaType(target.arg(0), true, ctx).equals(javaType(o.type.arg(0), true, ctx))) {
                return "((" + javaType(target, false, ctx) + ") (Object) " + paren(o.code) + ".asIterable())";
            }
            return paren(o.code) + ".asIterable()";
        }
        // A concrete generic target whose value is a proper subtype with different type arguments
        // (e.g. MaterialPageRoute<Void> -> Route<Object>): the subtyping holds but the type
        // argument mismatch makes Java reject it, so erase through Object.
        if (target.name != null && o.type != null && o.type.name != null
                && !target.name.equals(o.type.name)
                && !target.args.isEmpty()
                && isFullyConcrete(target)
                && isSubtypeName(o.type.name, target.name)
                && !inheritsSameInstantiation(o.type, target)) {
            return "(" + javaType(copyNonNull(target), false, ctx) + ") (Object) " + paren(o.code);
        }
        // Covariant generic assignment: Dart lists/maps/futures are covariant in their type
        // arguments, but Java generics are invariant. When value and target are the SAME generic
        // type with differing type arguments (e.g. DartList<RotatedBox> -> DartList<Widget>,
        // Future<List<Object>> -> Future<Object>), bridge with a RAW cast. A cast between two
        // distinct concrete parameterizations is illegal ("inconvertible types") in Java, so we
        // erase through the raw type — matching Dart's covariance (unchecked at runtime).
        if (target.name != null && o.type != null && o.type.name != null
                && target.name.equals(o.type.name)
                && !target.args.isEmpty() && !o.type.args.isEmpty()
                && !isDynamicType(target)
                && isFullyConcrete(target)
                && !target.toString().equals(o.type.toString())) {
            // Erase through Object so javac accepts the cross-parameterization cast (a direct cast
            // between two distinct concrete parameterizations is "inconvertible types"). Only when
            // the target is fully concrete — a type-variable arg (e.g. DartList<T>) is resolved by
            // Java's own inference and must not be pinned by a cast.
            return "(" + javaType(copyNonNull(target), false, ctx) + ") (Object) " + paren(o.code);
        }
        // A dynamic value (a Dyn.get member read) landing in a concrete slot -- a typed
        // local, a switch expression's result temp -- is Dart's implicit downcast. Only for
        // a fully concrete target: a type variable is left to Java's own inference.
        if (o.type != null && isDynamicType(o.type) && !isDynamicType(target) && isFullyConcrete(target)
                && !target.is("void") && o.code.startsWith("Dyn.")) {
            return "(" + javaType(copyNonNull(target), false, ctx) + ") " + paren(o.code);
        }
        // The same implicit downcast for a dynamic local or parameter (a closure's untyped
        // `(error, stackTrace)` handed on to `completeError(Object, StackTrace)`): its Java
        // type is Object, which javac will not narrow by itself. Only a bare name of an
        // Object-typed local, and only into a program or core class type with no type
        // arguments, so no primitive or generic slot is touched.
        if (o.type != null && (o.type.is("dynamic") || o.type.is("var") || o.type.is("Object"))
                && o.code.matches("[A-Za-z_$][A-Za-z0-9_$]*") && target.args.isEmpty()
                && target.funcParams == null && target.funcReturn == null && !isDynamicType(target)
                && !target.is("void") && !target.is("int") && !target.is("double") && !target.is("bool")
                && !target.is("num") && !target.is("Null") && isFullyConcrete(target)
                && !classTypeVarInScope(target, ctx)) {
            return "(" + javaType(copyNonNull(target), false, ctx) + ") " + o.code;
        }
        // A function value whose SAM differs from an Object-returning function slot of the
        // same arity (`Func0<Future<Object>>` into a `FutureOr Function()` slot, `Func0<Object>`):
        // Dart's function subtyping is covariant in the return, Java generics are not. The
        // Funcs interfaces erase identically, so the value is retyped through Object.
        if (o.type != null && o.type.funcParams != null && target.funcParams != null
                && o.type.funcParams.size() == target.funcParams.size()
                && o.type.funcReturn != null && !o.type.funcReturn.is("void")
                && !isTypeVariable(o.type.funcReturn, ctx) && isFullyConcrete(o.type.funcReturn)
                && target.funcReturn != null && !target.funcReturn.is("void")
                && (isDynamicType(target.funcReturn) || target.funcReturn.is("Object")
                        || target.funcReturn.is("FutureOr"))) {
            String ts = funcSamTypeFromRefs(target.funcParams, target.funcReturn, ctx);
            String os = funcSamTypeFromRefs(o.type.funcParams, o.type.funcReturn, ctx);
            // A method reference or lambda is not a value yet: Java types it by the slot.
            if (!ts.equals(os) && ts.indexOf('<') >= 0 && !o.code.contains("::") && !o.code.contains("->")) {
                return "(" + ts + ") (Object) " + paren(o.code);
            }
        }
        // A FutureOr<T> promoted to Future (`if (toReturn is Future) toReturn` into a
        // `<Future<Object?>>[...]` element): the Java value is an Object, cast to the slot.
        if (o.type != null && o.type.is("FutureOr") && target.is("Future") && isFullyConcrete(target)) {
            return "(" + javaType(copyNonNull(target), false, ctx) + ") (Object) " + paren(o.code);
        }
        // A FutureOr<T> is an Object in Java; reaching a T slot (after an `is! Future` test
        // returned early, or a value Dart already knows is not a Future) it is Dart's implicit
        // downcast. A class type variable is in scope only in an instance member.
        if (o.type != null && o.type.is("FutureOr") && !isDynamicType(target) && !target.is("void")
                && !target.is("Future") && target.funcReturn == null
                && (isFullyConcrete(target) || classTypeVarInScope(target, ctx))) {
            return "(" + javaType(boxType(copyNonNull(target)), true, ctx) + ") (Object) " + paren(o.code);
        }
        return o.code;
    }

    private String downcastArm(Out arm, String code, TypeRef slot, Ctx ctx) {
        if (slot == null || arm.type == null || isDynamicType(slot) || slot.is("void") || slot.is("Object")
                || slot.is("FutureOr") || slot.is("Null") || slot.is("Function") || slot.is("num")
                || slot.funcReturn != null || slot.funcParams != null
                || "null".equals(code) || code.contains("->") || code.contains("::")
                || !(isFullyConcrete(slot) || classTypeVarInScope(slot, ctx))) {
            return code;
        }
        boolean erased = isDynamicType(arm.type) || arm.type.is("Object") || arm.type.is("FutureOr");
        if (!erased) {
            return code;
        }
        return "(" + javaType(boxType(copyNonNull(slot)), true, ctx) + ") (Object) " + paren(code);
    }

    /**
     * The functional interface of a leading {@code ((Funcs.X<...>) ...)} cast, or null. Only a
     * cast whose operand is itself a lambda counts: that is how a tear-off is emitted.
     */
    private static String leadingFuncsCast(String code) {
        if (!code.startsWith("((Funcs.")) {
            return null;
        }
        int depth = 0;
        for (int i = 2; i < code.length(); i++) {
            char ch = code.charAt(i);
            if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                depth--;
            } else if (ch == ')' && depth == 0) {
                String sam = code.substring(2, i);
                return sam.matches("Funcs\\.[A-Za-z0-9]+(<.*>)?") ? sam : null;
            } else if (ch == '(' || ch == ' ' && depth == 0) {
                return null;
            }
        }
        return null;
    }

    /** {@code T} of the enclosing class, which Java has in scope only in an instance member. */
    private boolean classTypeVarInScope(TypeRef t, Ctx ctx) {
        return t != null && t.args.isEmpty() && t.funcReturn == null && !ctx.staticMember
                && ctx.currentClass != null && ctx.currentClass.typeParams != null
                && ctx.currentClass.typeParams.contains(t.name);
    }

    private String boxIfPrimitive(Out o, Ctx ctx) {
        // Java autoboxing covers long/double/boolean → Long/Double/Boolean
        return o.code;
    }

    private String zeroValue(TypeRef t) {
        if (t == null) {
            return "null";
        }
        if (t.nullable) {
            return "null";
        }
        if (t.is("int")) {
            return "0L";
        }
        if (t.is("double")) {
            return "0.0";
        }
        if (t.is("bool")) {
            return "false";
        }
        return "null";
    }

    /** Java holder class for a boxed captured local of the given type. */
    private String refHolder(TypeRef t, Ctx ctx) {
        if (t.is("int") && !t.nullable) {
            ctx.importClass("dart.runtime.RefLong");
            return "RefLong";
        }
        if (t.is("double") && !t.nullable) {
            ctx.importClass("dart.runtime.RefDouble");
            return "RefDouble";
        }
        if (t.is("bool") && !t.nullable) {
            ctx.importClass("dart.runtime.RefBool");
            return "RefBool";
        }
        ctx.importClass("dart.runtime.Ref");
        return "Ref<" + javaType(t, true, ctx) + ">";
    }

    private String paren(String code) {
        // parenthesize composite expressions to preserve precedence
        if (code.matches("[A-Za-z0-9_$.()\\[\\]\"]+") || code.startsWith("(")) {
            return code;
        }
        return "(" + code + ")";
    }

    /** Whether the '(' at index 0 of {@code code} closes at its last character. */
    private static boolean outerParenEnclosesAll(String code) {
        int depth = 0;
        boolean inStr = false;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inStr) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i == code.length() - 1;
                }
            }
        }
        return true;
    }

    /** Turns an expression emission into a valid Java statement expression. */
    private String statementize(String code) {
        if (code.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            // a bare temp/identifier (e.g. a cascade's receiver) — no-op statement
            return "";
        }
        if (code.startsWith("(") && code.endsWith(")") && outerParenEnclosesAll(code)) {
            // ternaries etc. are not valid statements; assign to a discard temp. A call on a
            // parenthesized receiver -- ((F) f).call(..) -- is already a statement, and may be
            // void, which `var` cannot hold.
            return "var $unused" + (unusedCounter++) + " = " + code;
        }
        return code;
    }

    /**
     * Whether {@code code}, as emitted Java, is itself a legal statement on its own (a
     * method call, assignment, {@code new X(...)}, or an increment/decrement) rather than
     * a value-only expression (a literal, an operator, a ternary, a bare identifier).
     * Mirrors {@link #statementize}'s own classification of what needs no further
     * wrapping to appear as a statement; used by {@link #pinnedHandler} to recognize an
     * arrow-bodied lambda that is ambiguous between a void- and a value-returning
     * functional-interface overload regardless of its body's own static type.
     */
    private boolean isJavaStatementExpr(String code) {
        if (code.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            return false;
        }
        return !(code.startsWith("(") && code.endsWith(")"));
    }

    private int unusedCounter;

    private boolean isStateSubclass(ClassDecl c) {
        return c.superclass != null && c.superclass.is("State");
    }

    private TypeRef stateTypeArg(ClassDecl c) {
        if (isStateSubclass(c) && !c.superclass.args.isEmpty()) {
            return c.superclass.arg(0);
        }
        return null;
    }

    /** Finds a mixin-contributed member (field or method) for a program class hierarchy. */
    private Object findMixinMember(ClassDecl c, String name, boolean wantField) {
        while (c != null) {
            for (TypeRef mixRef : c.mixins) {
                ClassDecl mx = program.classes.get(mixRef.name);
                if (mx == null) {
                    continue;
                }
                if (wantField) {
                    FieldDecl f = mx.field(name);
                    if (f != null) {
                        return f;
                    }
                } else {
                    MethodDecl m = mx.method(name);
                    if (m != null) {
                        return m;
                    }
                    MethodDecl g = mx.getter(name);
                    if (g != null) {
                        return g;
                    }
                }
            }
            c = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        }
        return null;
    }

    /** Finds a method walking the program-class superclass chain. */
    /**
     * The receiver viewed as its type parameter's declared bound, or null when its type
     * is not a bounded type parameter of the enclosing class (or the bound is not a class
     * this emitter can resolve members on).
     */
    private Out typeParamBoundView(Out target, Ctx ctx) {
        TypeRef tt = target.type;
        ClassDecl cc = ctx.currentClass;
        if (tt == null || tt.name == null || !tt.args.isEmpty() || tt.funcReturn != null) {
            return null;
        }
        TypeRef bound = null;
        java.util.Map<String, TypeRef> mb = ctx.methodTypeParamBounds;
        if (mb != null && mb.containsKey(tt.name)) {
            // a method's own type parameter shadows the class's
            bound = mb.get(tt.name);
            if (bound != null && (mb.containsKey(bound.name)
                    || (cc != null && cc.typeParams.contains(bound.name)))) {
                return null;
            }
        } else {
            if (cc == null || cc.typeParamBounds == null || !cc.typeParams.contains(tt.name)) {
                return null;
            }
            bound = cc.typeParamBounds.get(tt.name);
            if (bound != null && cc.typeParams.contains(bound.name)) {
                return null;
            }
        }
        // Comparable bounds keep the type-variable compareTo path (DartComparable.compare,
        // answering Dart's int), never java.lang.Comparable's int-returning compareTo.
        if (bound == null || bound.name == null || bound.name.equals(tt.name)
                || bound.name.equals("Comparable")) {
            return null;
        }
        // `L extends List<int>`: a core collection bound reads its members through the
        // runtime collection type, as a value of the bound type itself would.
        boolean coreBound = bound.is("List") || bound.is("Iterable") || bound.is("Set") || bound.is("Map");
        if (programClass(bound.name, ctx) == null && !stubs.isStubClass(bound.name) && !coreBound) {
            return null;
        }
        return new Out("((" + javaType(bound, true, ctx) + ") " + target.code + ")", bound);
    }

    private MethodDecl findMethodInHierarchy(ClassDecl c, String name) {
        return findMethodInHierarchy(c, name, new java.util.HashSet<ClassDecl>());
    }

    /**
     * Superclasses first (the concrete chain wins), then the interfaces each class
     * implements: an abstract `Sub<E> implements Base<E>` has Base's members too.
     */
    private MethodDecl findMethodInHierarchy(ClassDecl c, String name, java.util.Set<ClassDecl> seen) {
        java.util.List<ClassDecl> chain = new java.util.ArrayList<ClassDecl>();
        while (c != null && seen.add(c)) {
            MethodDecl m = c.method(name);
            if (m != null) {
                return m;
            }
            // Dart's linearization puts the mixins above the superclass, the last one first
            for (int i = c.mixins.size() - 1; i >= 0; i--) {
                ClassDecl mx = program.classes.get(c.mixins.get(i).name);
                MethodDecl mm = mx != null && mx.isMixin ? mx.method(name) : null;
                if (mm != null && !mm.isAbstract) {
                    return mm;
                }
            }
            chain.add(c);
            c = c.superclass != null ? superDeclOf(c, c.superclass) : null;
        }
        for (ClassDecl k : chain) {
            for (TypeRef i : k.interfaces) {
                ClassDecl ic = program.classes.get(i.name);
                MethodDecl m = ic == null ? null : findMethodInHierarchy(ic, name, seen);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }

    /** Nearest superclass of a program class that is a stub class. */
    private String nearestStubSuper(ClassDecl c) {
        TypeRef sup = c.superclass;
        while (sup != null) {
            if (stubs.isStubClass(sup.name)) {
                return sup.name;
            }
            ClassDecl pc = program.classes.get(sup.name);
            sup = pc != null ? pc.superclass : null;
        }
        return null;
    }

    private static boolean stubTakesPositionals(Ast.ClassDecl stub) {
        for (Ast.CtorDecl k : stub.ctors) {
            if (k.name == null) {
                for (Param p : k.params) {
                    if (!p.named) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Ast.ClassDecl stubClassOf(TypeRef t) {
        return t == null ? null : stubs.classes.get(t.name);
    }

    /**
     * Substitutes type variables in {@code t} using {@code subst}, preserving
     * nullability. A bare type variable (no type arguments of its own) maps
     * directly; otherwise the substitution recurses into the type arguments.
     */
    private TypeRef substTypeVars(TypeRef t, java.util.Map<String, TypeRef> subst) {
        if (t == null || subst.isEmpty()) {
            return t;
        }
        if (t.args.isEmpty()) {
            TypeRef mapped = subst.get(t.name);
            if (mapped == null) {
                return t;
            }
            if (t.nullable && !mapped.nullable) {
                TypeRef nn = new TypeRef(mapped.name);
                nn.args.addAll(mapped.args);
                nn.nullable = true;
                return nn;
            }
            return mapped;
        }
        java.util.List<TypeRef> newArgs = new java.util.ArrayList<TypeRef>();
        boolean changed = false;
        for (TypeRef a : t.args) {
            TypeRef na = substTypeVars(a, subst);
            newArgs.add(na);
            if (na != a) {
                changed = true;
            }
        }
        if (!changed) {
            return t;
        }
        TypeRef nt = new TypeRef(t.name);
        nt.args.addAll(newArgs);
        nt.nullable = t.nullable;
        return nt;
    }

    /** Maps a class's declared type-parameter names to concrete type arguments. */
    private static java.util.Map<String, TypeRef> paramMap(java.util.List<String> params,
            java.util.List<TypeRef> args) {
        java.util.Map<String, TypeRef> m = new java.util.HashMap<String, TypeRef>();
        if (params != null) {
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                m.put(params.get(i), args.get(i));
            }
        }
        return m;
    }

    /**
     * The concrete (type-argument-substituted) declared return type of a getter or
     * method {@code member} on a stub receiver {@code receiver}. Walks the stub
     * superclass chain composing the substitution from the receiver's own
     * instantiation, so e.g. {@code MapEntry<Locale,DisplayOption>.value} resolves
     * to {@code DisplayOption}, {@code FormFieldState<String>.value} to
     * {@code String?}, and {@code ColorTween.evaluate(...)} (via {@code Tween<Color>}
     * / {@code Animatable<T>}) to {@code Color}. Returns {@code null} when the member
     * is not found on the stub chain (leaving the caller's default type in place).
     */
    /**
     * {@code m} with its callback parameters' class type variables replaced by the
     * receiver's type arguments: {@code Stream<List<int>>.listen} expects a
     * {@code void Function(List<int>)}, so the closure passed to it types its
     * parameter and {@code xs.contains(1)} is a typed call. Without it the parameter
     * was dynamic and every member read went through {@code Dyn}, whose untyped
     * result javac then rejected. Unbound variables (a raw receiver) become dynamic.
     * Only callbacks change; value parameters keep their declared types.
     */
    /**
     * The parameters of a generic extension's method with the extension's type variables
     * bound from the receiver: `extension on List<E>` called on a `List<int>` hands
     * `(x) => x.isOdd` a parameter typed int instead of the unbound E. Only callback
     * parameters change, as for stub receivers.
     */
    private List<Param> extensionCallbackParams(ClassDecl ext, List<Param> params, TypeRef receiver) {
        if (ext.typeParams == null || ext.typeParams.isEmpty() || ext.extensionOn == null || receiver == null) {
            return params;
        }
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
        for (int i = 0; i < ext.extensionOn.args.size() && i < receiver.args.size(); i++) {
            TypeRef on = ext.extensionOn.args.get(i);
            TypeRef a = receiver.args.get(i);
            if (on.args.isEmpty() && on.funcParams == null && ext.typeParams.contains(on.name)
                    && a != null && isConcreteType(a)) {
                subst.put(on.name, a);
            }
        }
        if (subst.isEmpty()) {
            return params;
        }
        List<Param> out = new ArrayList<Param>();
        for (Param p : params) {
            TypeRef nt = p.type != null && p.type.funcParams != null ? substFunctionType(p.type, subst) : p.type;
            if (nt == p.type) {
                out.add(p);
                continue;
            }
            Param q = new Param();
            q.type = nt;
            q.name = p.name;
            q.named = p.named;
            q.required = p.required;
            q.optional = p.optional;
            q.isThis = p.isThis;
            q.isSuper = p.isSuper;
            q.defaultValue = p.defaultValue;
            out.add(q);
        }
        return out;
    }

    private Ast.MethodDecl withReceiverCallbackTypes(Ast.MethodDecl m, TypeRef receiver) {
        Ast.ClassDecl c = receiver == null ? null : stubs.classes.get(receiver.name);
        if (c == null || c.typeParams.isEmpty() || !c.methods.contains(m)) {
            return m;
        }
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
        for (int i = 0; i < c.typeParams.size(); i++) {
            // A raw receiver comes back as the stub's own `Stream<T>`; that T, like any
            // type variable, is no type the closure can declare, so it stays dynamic.
            TypeRef a = i < receiver.args.size() ? receiver.args.get(i) : null;
            subst.put(c.typeParams.get(i), a != null && isConcreteType(a) ? a : TypeRef.DYNAMIC);
        }
        List<Param> params = new ArrayList<Param>();
        boolean changed = false;
        for (Param p : m.params) {
            TypeRef nt = p.type != null && p.type.funcParams != null ? substFunctionType(p.type, subst) : p.type;
            if (nt != p.type) {
                Param q = new Param();
                q.type = nt;
                q.name = p.name;
                q.named = p.named;
                q.required = p.required;
                q.optional = p.optional;
                q.isThis = p.isThis;
                q.isSuper = p.isSuper;
                q.defaultValue = p.defaultValue;
                params.add(q);
                changed = true;
            } else {
                params.add(p);
            }
        }
        if (!changed) {
            return m;
        }
        Ast.MethodDecl copy = new Ast.MethodDecl();
        copy.returnType = m.returnType;
        copy.name = m.name;
        copy.typeParams = m.typeParams;
        copy.params = params;
        copy.isStatic = m.isStatic;
        copy.isGetter = m.isGetter;
        copy.isSetter = m.isSetter;
        copy.isAbstract = m.isAbstract;
        copy.isAsync = m.isAsync;
        return copy;
    }

    /** {@link #substTypeVars}, reaching into an inline function type's signature too. */
    private TypeRef substFunctionType(TypeRef t, java.util.Map<String, TypeRef> subst) {
        if (t == null || t.funcParams == null) {
            return substTypeVars(t, subst);
        }
        boolean changed = false;
        List<TypeRef> fps = new ArrayList<TypeRef>();
        for (TypeRef fp : t.funcParams) {
            TypeRef nfp = substFunctionType(fp, subst);
            changed |= nfp != fp;
            fps.add(nfp);
        }
        TypeRef ret = substFunctionType(t.funcReturn, subst);
        changed |= ret != t.funcReturn;
        if (!changed) {
            return t;
        }
        TypeRef nt = new TypeRef(t.name);
        nt.args.addAll(t.args);
        nt.nullable = t.nullable;
        nt.funcParams = fps;
        nt.funcReturn = ret;
        return nt;
    }

    private TypeRef stubMemberReturnType(TypeRef receiver, String member, boolean getter) {
        if (receiver == null) {
            return null;
        }
        Ast.ClassDecl c = stubs.classes.get(receiver.name);
        java.util.List<TypeRef> recvArgs = receiver.args;
        if (c != null && recvArgs.isEmpty() && !c.typeParams.isEmpty()) {
            // A raw receiver (a stub factory declared `static Stream fromIterable(..)`)
            // is the class instantiated to dynamic, as Dart reads it: its `toList()` is
            // a Future<List<dynamic>>, never one naming a T the Java code cannot see.
            recvArgs = new java.util.ArrayList<TypeRef>();
            for (int i = 0; i < c.typeParams.size(); i++) {
                recvArgs.add(TypeRef.DYNAMIC);
            }
        }
        java.util.Map<String, TypeRef> subst =
                paramMap(c == null ? null : c.typeParams, recvArgs);
        while (c != null) {
            for (Ast.MethodDecl m : c.methods) {
                if (m.name.equals(member) && m.isGetter == getter && !m.isSetter) {
                    return substTypeVars(m.returnType, subst);
                }
            }
            TypeRef sup = c.superclass;
            if (sup == null) {
                break;
            }
            Ast.ClassDecl sd = stubs.classes.get(sup.name);
            java.util.List<TypeRef> superArgs = new java.util.ArrayList<TypeRef>();
            for (TypeRef a : sup.args) {
                superArgs.add(substTypeVars(a, subst));
            }
            subst = paramMap(sd == null ? null : sd.typeParams, superArgs);
            c = sd;
        }
        return null;
    }

    /**
     * The concrete declared return type of a getter/method {@code member} inherited by
     * a program class {@code pc} from a generic stub superclass whose type argument the
     * program class fixes — e.g. {@code class _RestorableEmailState extends
     * RestorableListenable<EmailStore>} reading the inherited {@code value} getter
     * resolves the getter's {@code T} to {@code EmailStore}. Walks the program then stub
     * superclass chain composing the substitution. Returns {@code null} when the member
     * is not found on a stub ancestor.
     */
    private TypeRef inheritedStubMemberReturnType(ClassDecl pc, String member, boolean getter) {
        return inheritedStubMemberReturnType(pc, member, getter, null);
    }

    /**
     * As above, seen through a receiver typed {@code receiver}: the program class's own
     * type variables take the receiver's arguments first ({@code Wrap<int>} of
     * {@code class Wrap<E> extends StreamView<E>}).
     */
    private TypeRef inheritedStubMemberReturnType(ClassDecl pc, String member, boolean getter,
                                                  TypeRef receiver) {
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
        if (receiver != null && receiver.args.size() == pc.typeParams.size()) {
            subst.putAll(paramMap(pc.typeParams, receiver.args));
        }
        TypeRef sup = pc.superclass;
        while (sup != null) {
            java.util.List<TypeRef> superArgs = new java.util.ArrayList<TypeRef>();
            for (TypeRef a : sup.args) {
                superArgs.add(substTypeVars(a, subst));
            }
            ClassDecl superProg = program.classes.get(sup.name);
            Ast.ClassDecl superStub = stubs.classes.get(sup.name);
            java.util.List<String> params = superProg != null ? superProg.typeParams
                    : superStub != null ? superStub.typeParams
                    : java.util.Collections.<String>emptyList();
            java.util.Map<String, TypeRef> newSubst = paramMap(params, superArgs);
            if (superStub != null) {
                for (Ast.MethodDecl m : superStub.methods) {
                    if (m.name.equals(member) && m.isGetter == getter && !m.isSetter) {
                        return substTypeVars(m.returnType, newSubst);
                    }
                }
            }
            subst = newSubst;
            sup = superProg != null ? superProg.superclass
                    : superStub != null ? superStub.superclass : null;
        }
        return null;
    }

    private String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    private String indentStr(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    // ==================================================================
    // Emission context
    // ==================================================================

    private final class Ctx {
        /// The member a `super.x` about to be emitted names, for [#superThroughMixin].
        String superMember;
        /// Declared bounds of the type parameters of the method being emitted (`m<T extends B>`).
        java.util.Map<String, TypeRef> methodTypeParamBounds;
        /**
         * The Java temp holding what the innermost enclosing catch clause caught, which
         * {@code rethrow;} throws again as it is -- the same object, so its stack trace and
         * identity survive. Null outside a catch body.
         */
        String rethrowTarget;
        /// The import prefix of the `prefix.Name(...)` call being lowered to a bare call,
        /// until the class or function resolution consumes it (see takeCallPrefix).
        String callPrefix;

        ClassDecl currentClass;
        /// Type parameters of the enclosing generic method; they shadow the class's.
        java.util.Set<String> methodTypeParams;
        /// Type parameters whose witness is the local `$tw$<name>` here (a constructor's
        /// hidden parameter), rather than the instance field.
        java.util.Set<String> witnessLocals;
        /// True in a static member, where the class's witness fields are out of reach.
        boolean staticMember;
        /// The call being emitted and the type its context expects (see callExpected).
        Call callExpectedFor;
        TypeRef callExpected;
        /** The library whose top-level (lib-class) body is being emitted; set only when currentClass is null. */
        Library currentLibrary;
        final Map<String, String> imports = new TreeMap<String, String>();
        final List<Map<String, TypeRef>> scopes = new ArrayList<Map<String, TypeRef>>();
        final List<Writer> writers = new ArrayList<Writer>();
        final java.util.Set<String> boxedLocals = new HashSet<String>();
        /** Names called for their result in the current body (CaptureScan.valueCalledNames). */
        final java.util.Set<String> valueCalledLocals = new HashSet<String>();
        /** Value types of the `return`s of the lambda block body being emitted, or null. */
        List<TypeRef> lambdaReturns;
        private final java.util.Set<String> boxedActive = new HashSet<String>();
        TypeRef methodReturnType;
        TypeRef extensionSelfType;
        /** The extension whose body is being emitted; its own members resolve first. */
        ClassDecl currentExtension;
        /** The class being constructed with written type arguments, and their binding. */
        ClassDecl ctorTypeOwner;
        java.util.Map<String, TypeRef> ctorTypeSubst;
        /** Method type parameters dropped from the Java signature; each reads as Object. */
        java.util.Set<String> erasedMethodTypeVars;
        boolean inAsyncBody;
        /** Inside an async closure's body, whose Future may be typed Future of dynamic. */
        boolean asyncLambda;
        /**
         * The temp an enclosing {@code a?.m()} has already tested for null while it
         * emits the call on it, so {@code a?.call()} does not test the same temp again.
         */
        String nullCheckedTemp;
        /** Inside a hashCode/compareTo body: narrow each `return` value to Java int. */
        boolean narrowReturnToInt;
        /// A bare `return;` answers null: the enclosing block lambda targets a
        /// dynamic-returning (value) SAM but returns no value of its own.
        boolean bareReturnIsNull;
        String syncStarList;                  // non-null inside a sync* body: the result-list temp
        String asyncStarSink;                 // non-null inside an async* body: the AsyncStar.Sink temp
        /**
         * The expression being emitted as a statement, whose value nobody reads (see
         * {@link #emitStatementExpr}); an assignment through a void setter only needs a
         * value form when it is not this.
         */
        Expr statementExpr;
        /** An {@code x.p += 1} standing for {@code x.p++} used as a value: it yields the OLD value. */
        Assign postfixAssign;
        TypeRef syncStarElem;                 // element type yielded by the enclosing sync* body
        private int tempCounter;

        void markBoxed(String name) {
            boxedActive.add(name);
        }

        boolean isBoxed(String name) {
            return boxedActive.contains(name);
        }

        private final List<Out> cascadeTargets = new ArrayList<Out>();

        void pushCascadeTarget(Out t) {
            cascadeTargets.add(t);
        }

        void popCascadeTarget() {
            cascadeTargets.remove(cascadeTargets.size() - 1);
        }

        Out cascadeTarget() {
            return cascadeTargets.isEmpty() ? new Out("null", TypeRef.DYNAMIC)
                    : cascadeTargets.get(cascadeTargets.size() - 1);
        }

        // Break targets: a Dart `switch` is lowered to a labeled Java block (not a
        // Java switch), so a `break` targeting it needs the label; a `break` inside a
        // loop stays bare. Entries: the switch's label, or null for a loop.
        private final List<String> breakTargets = new ArrayList<String>();

        void pushBreakTarget(String label) {
            breakTargets.add(label);
        }

        void popBreakTarget() {
            breakTargets.remove(breakTargets.size() - 1);
        }

        /** The label a bare {@code break} must carry (a switch block), or null when a loop. */
        String currentBreakLabel() {
            return breakTargets.isEmpty() ? null : breakTargets.get(breakTargets.size() - 1);
        }

        Ctx(ClassDecl currentClass) {
            this.currentClass = currentClass;
        }

        /** The library this context is emitting within (class owner, or the lib class itself). */
        Library library() {
            if (currentClass != null) {
                return currentClass.ownerLibrary;
            }
            return currentLibrary;
        }

        void importClass(String fqcn) {
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            String existing = imports.get(simple);
            if (existing == null) {
                imports.put(simple, fqcn);
            }
        }

        final List<Map<String, String>> renameScopes = new ArrayList<Map<String, String>>();
        private int shadowCounter;

        /// Promotions that hold until the scope ends (`if (x is! T) return;`), one list per scope.
        final List<List<Object[]>> scopePromotions = new ArrayList<List<Object[]>>();

        void pushScope() {
            scopes.add(new LinkedHashMap<String, TypeRef>());
            renameScopes.add(new LinkedHashMap<String, String>());
            scopePromotions.add(new ArrayList<Object[]>());
        }

        void popScope() {
            scopes.remove(scopes.size() - 1);
            renameScopes.remove(renameScopes.size() - 1);
            if (!scopePromotions.isEmpty()) {
                List<Object[]> undo = scopePromotions.remove(scopePromotions.size() - 1);
                for (int i = undo.size() - 1; i >= 0; i--) {
                    restorePromotion((String) undo.get(i)[0], (TypeRef) undo.get(i)[1]);
                }
            }
        }

        /// Records promotions to undo when the innermost scope ends; false when there is none.
        boolean promoteForScope(List<Object[]> undo) {
            if (scopePromotions.isEmpty()) {
                return false;
            }
            scopePromotions.get(scopePromotions.size() - 1).addAll(undo);
            return true;
        }

        void declare(String name, TypeRef type) {
            if (!scopes.isEmpty()) {
                scopes.get(scopes.size() - 1).put(name, type);
            }
        }

        /**
         * Declares a local, renaming when the Dart name would illegally
         * shadow an enclosing Java local/param. Returns the Java name.
         */
        String declareShadowSafe(String name, TypeRef type) {
            String javaName = javaIdent(name);
            if (lookup(name) != null) {
                javaName = javaName + "$" + (shadowCounter++);
            }
            declare(name, type);
            if (!javaName.equals(name) && !renameScopes.isEmpty()) {
                renameScopes.get(renameScopes.size() - 1).put(name, javaName);
            }
            return javaName;
        }

        /** Java names of the late locals in this body; each is unique (see declareLate). */
        private final java.util.Set<String> lateLocals = new HashSet<String>();

        /** Declares a late local under a Java name no other local can have. */
        String declareLate(String name, TypeRef type) {
            declare(name, type);
            String javaName = javaIdent(name) + "$late" + (shadowCounter++);
            renameScopes.get(renameScopes.size() - 1).put(name, javaName);
            lateLocals.add(javaName);
            return javaName;
        }

        boolean isLate(String javaName) {
            return lateLocals.contains(javaName);
        }

        String javaNameOf(String name) {
            for (int i = renameScopes.size() - 1; i >= 0; i--) {
                if (scopes.get(i).containsKey(name)) {
                    String renamed = renameScopes.get(i).get(name);
                    return renamed != null ? renamed : name;
                }
            }
            return name;
        }

        TypeRef lookup(String name) {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                TypeRef t = scopes.get(i).get(name);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }

        /**
         * Flow-based type promotions from {@code x is T} guards, e.g. inside
         * {@code x is T && x.member} or an {@code if (x is T) { x.member }} then-branch.
         * Maps a promoted local's Dart name to the narrowed type; reads emit a cast.
         */
        private final Map<String, TypeRef> promotions = new java.util.HashMap<String, TypeRef>();

        TypeRef promotedType(String name) {
            return promotions.get(name);
        }

        /** Applies a promotion, returning the prior value (possibly null) for later restore. */
        TypeRef pushPromotion(String name, TypeRef type) {
            TypeRef prev = promotions.get(name);
            promotions.put(name, type);
            return prev;
        }

        void restorePromotion(String name, TypeRef prev) {
            if (prev == null) {
                promotions.remove(name);
            } else {
                promotions.put(name, prev);
            }
        }

        String newTemp() {
            return "$t" + (tempCounter++);
        }

        Writer pushWriter(int indent) {
            Writer w = new Writer(indent);
            writers.add(w);
            return w;
        }

        String popWriter() {
            Writer w = writers.remove(writers.size() - 1);
            return w.sb.toString();
        }

        Writer writer() {
            return writers.get(writers.size() - 1);
        }

        int currentIndent() {
            return writers.isEmpty() ? 1 : writer().indent;
        }

        void indent(int delta) {
            writer().indent += delta;
        }

        final class Writer {
            final StringBuilder sb = new StringBuilder();
            int indent;

            Writer(int indent) {
                this.indent = indent;
            }

            void line(String s) {
                sb.append(indentStr(indent)).append(s).append('\n');
            }

            /** Appends lines another writer already indented (see {@link #emitLazyOperand}). */
            void raw(String lines) {
                sb.append(lines);
            }
        }
    }
}
