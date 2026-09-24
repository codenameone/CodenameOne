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

    public List<GeneratedFile> emit() {
        List<GeneratedFile> out = new ArrayList<GeneratedFile>();
        String mainLib = null;
        for (Library lib : program.libraries) {
            for (ClassDecl c : lib.classes) {
                try {
                    if (c.extensionOn != null) {
                        out.add(emitExtension(c));
                    } else if (c.isMixin) {
                        out.add(emitMixin(c));
                    } else {
                        out.add(emitClass(c));
                    }
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "class " + c.name, ex);
                }
            }
            for (EnumDecl e : lib.enums) {
                try {
                    out.add(emitEnum(e));
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "enum " + e.name, ex);
                }
            }
            if (!lib.functions.isEmpty() || !lib.topLevelVars.isEmpty()) {
                try {
                    out.add(emitLibClass(lib));
                } catch (RuntimeException | StackOverflowError ex) {
                    emitCrash(lib, "library " + lib.fileName, ex);
                }
                for (FunctionDecl f : lib.functions) {
                    if (f.name.equals("main")) {
                        mainLib = Program.libClassName(lib.fileName);
                    }
                }
            }
        }
        if (mainLib != null) {
            out.add(emitRegistry(mainLib));
        }
        // record classes are discovered lazily while emitting bodies, so generate them last
        for (Map.Entry<String, RecordShape> e : recordShapes.entrySet()) {
            out.add(emitRecordClass(e.getKey(), e.getValue()));
        }
        return out;
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
        sb.append('(').append(comps).append(") {\n}\n");
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
        return new Out("new " + cn + "<>(" + args + ")", t);
    }

    /** The static type of a record component accessed by name (`$1`, `$2`, or a named field). */
    private TypeRef recordComponentType(TypeRef recordType, String name) {
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
        for (CtorDecl ct : e.ctors) {
            body.append(emitCtor(syn, ct, ctx));
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
            mm.returnType = m.returnType;
            mm.params = m.params;
            mm.typeParams = m.typeParams;
            mm.body = m.body;
            mm.exprBody = m.exprBody;
            body.append(emitMethodLike(mm, ctx, false));
        }
        for (String imp : ctx.imports.values()) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!ctx.imports.isEmpty()) {
            sb.append('\n');
        }
        sb.append(dartRef(e)).append("\n");
        sb.append("public enum ").append(e.name).append(" {\n    ");
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < e.entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
                names.append(", ");
            }
            sb.append(e.entries.get(i));
            // the constant's simple name (enhanced enums carry `name(args)`)
            String entry = e.entries.get(i);
            int paren = entry.indexOf('(');
            names.append((paren >= 0 ? entry.substring(0, paren) : entry).trim());
        }
        // Dart's `EnumType.values` is a `List<EnumType>`; expose it as a DartList field
        // (coexisting with Java's implicit values() method) so `.values.idx(i)` resolves.
        sb.append(";\n\n");
        sb.append("    public static final dart.core.DartList<").append(e.name)
                .append("> values = dart.core.DartList.<").append(e.name).append(">of(")
                .append(names).append(");\n\n");
        sb.append(body);
        sb.append("}\n");
        return new GeneratedFile(e.name + ".java", sb.toString());
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
    private static boolean isLazyStatic(FieldDecl f) {
        return f.isStatic && isLazyTopLevel(f);
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
        ctx.pushWriter(3);
        Out init = emitExpr(v.initializer, vt, ctx);
        String lifted = ctx.popWriter();
        StringBuilder sb = new StringBuilder();
        sb.append("    private static ").append(jt).append(' ').append(v.name)
                .append("$value;\n");
        sb.append("    private static boolean ").append(v.name).append("$ready;\n\n");
        sb.append("    /** Dart `").append(v.name)
                .append("` -- initialised on first read, as Dart does. */\n");
        sb.append("    public static ").append(jt).append(" get$").append(v.name)
                .append("() {\n");
        sb.append("        if (!").append(v.name).append("$ready) {\n");
        // Marked ready BEFORE the initialiser runs: a variable whose own
        // initialiser reads it back is a cycle, and returning the zero value
        // beats recursing until the stack goes.
        sb.append("            ").append(v.name).append("$ready = true;\n");
        sb.append(lifted);
        sb.append("            ").append(v.name).append("$value = ")
                .append(coerce(init, vt, ctx)).append(";\n");
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
            m.name = f.name.equals("main") ? "main$" : f.name;
            m.returnType = f.returnType;
            m.params = f.params;
            m.typeParams = f.typeParams;
            m.body = f.body;
            m.exprBody = f.exprBody;
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
        StringBuilder body = new StringBuilder();
        body.append("    private ").append(ext.name).append("() {\n    }\n\n");
        for (MethodDecl m : ext.methods) {
            ctx.pushScope();
            TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
            StringBuilder sig = new StringBuilder();
            sig.append("    public static ").append(m.isSetter ? "void" : javaType(rt, false, ctx))
                    .append(' ').append(m.name).append('(')
                    .append(javaType(ext.extensionOn, false, ctx)).append(" $self");
            for (Param pm : m.params) {
                TypeRef pt = pm.type == null || pm.type.is("var") ? TypeRef.DYNAMIC : pm.type;
                sig.append(", ").append(javaType(pt, false, ctx)).append(' ')
                        .append(ctx.declareShadowSafe(pm.name, pt));
            }
            sig.append(") {\n");
            body.append(sig);
            ctx.pushWriter(2);
            ctx.methodReturnType = rt;
            if (m.body != null) {
                emitStatements(m.body, ctx);
            } else if (m.exprBody != null) {
                Out o = emitExpr(m.exprBody, rt.is("void") ? null : rt, ctx);
                if (rt.is("void")) {
                    ctx.writer().line(statementize(o.code) + ";");
                } else {
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
                diags.error(m, "E0401", "Static mixin members are not supported yet");
                continue;
            }
            ctx.pushScope();
            TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
            body.append("    default ").append(m.isSetter ? "void" : javaType(rt, false, ctx))
                    .append(' ').append(m.isGetter ? m.name : m.name).append('(');
            for (int i = 0; i < m.params.size(); i++) {
                Param pm = m.params.get(i);
                TypeRef pt = pm.type == null || pm.type.is("var") ? TypeRef.DYNAMIC : pm.type;
                if (i > 0) {
                    body.append(", ");
                }
                body.append(javaType(pt, false, ctx)).append(' ')
                        .append(ctx.declareShadowSafe(pm.name, pt));
            }
            body.append(") {\n");
            ctx.pushWriter(2);
            ctx.methodReturnType = rt;
            ctx.inAsyncBody = m.isAsync;
            if (m.body != null) {
                emitStatements(m.body, ctx);
            } else if (m.exprBody != null) {
                Out o = emitExpr(m.exprBody, rt.is("void") ? null : rt, ctx);
                if (rt.is("void")) {
                    ctx.writer().line(statementize(o.code) + ";");
                } else {
                    ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
                }
            }
            body.append(ctx.popWriter());
            ctx.methodReturnType = null;
            ctx.inAsyncBody = false;
            ctx.popScope();
            body.append("    }\n\n");
        }
        return finishClassFile(mx.name, "public interface " + mx.name, null, null, body, ctx, mx.file);
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
            // Accessors for instance fields. A Dart library-private (`_x`) field is still
            // reachable from sibling classes in the same library, so emit its accessor
            // package-private (all generated classes share one package) rather than skip it;
            // cross-instance reads compile to `x.get$_field()`.
            if (!f.isStatic) {
                boolean priv = f.name.startsWith("_");
                String vis = priv ? "    " : "    public ";
                body.append(vis).append(jt).append(" get$").append(f.name).append("() {\n")
                        .append("        return ").append(f.name).append(";\n    }\n");
                if (!f.isFinal && !f.isConst) {
                    body.append(vis).append("void set$").append(f.name).append("(").append(jt).append(" v) {\n")
                            .append("        this.").append(f.name).append(" = v;\n    }\n");
                }
            }
            body.append('\n');
        }

        // constructors
        if (c.hasNamedNonFactoryCtor()) {
            body.append("    /** Marker distinguishing named-constructor instantiation. */\n");
            body.append("    private static final class $NamedCtor {\n        private $NamedCtor() {\n        }\n    }\n\n");
            body.append("    private ").append(javaClassName(c)).append("($NamedCtor $marker) {\n    }\n\n");
        }
        for (CtorDecl ct : c.ctors) {
            body.append(emitCtor(c, ct, ctx));
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
            mm.returnType = m.returnType;
            // Dart lets a value-returning method override a void one; Java forbids it, so pin the
            // override's return to void to keep it a valid override.
            if (overriddenReturnsVoid(c, m)) {
                mm.returnType = TypeRef.VOID;
            }
            mm.params = m.params;
            mm.typeParams = m.typeParams;
            mm.body = m.body;
            mm.exprBody = m.exprBody;
            body.append(emitMethodLike(mm, ctx, c.isAbstract));
        }

        // mixin applications: implement each mixin interface and synthesize
        // the state (field + accessors) the mixin's abstract accessors need
        StringBuilder impls = new StringBuilder();
        for (TypeRef mixRef : c.mixins) {
            ClassDecl mx = program.classes.get(mixRef.name);
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
                    continue;
                }
                diags.error(c, "E0402", "Unknown mixin: " + mixRef.name);
                continue;
            }
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append(mixRef.name);
            for (FieldDecl f : mx.fields) {
                TypeRef ft = fieldType(f, ctx);
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
            }
        }
        // Dart `implements X` clauses (c.interfaces): the runtime interface the class satisfies
        // (e.g. `implements Iterator<T>` / `PreferredSizeWidget`). Emitted as Java `implements`.
        for (TypeRef itf : c.interfaces) {
            if (impls.length() > 0) {
                impls.append(", ");
            }
            impls.append(javaType(itf, false, ctx));
        }
        body.append(emitStubGetterBridges(c, ctx));
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
                ClassDecl sup = program.classes.get(sr.name);
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
            ext = javaType(c.superclass, false, ctx);
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
        return finishClassFile(jname, decl, ext, impls.length() == 0 ? null : impls.toString(),
                permits, body, ctx, c.file);
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
        sb.append("// Generated from ").append(dartFile).append(" — do not edit.\n");
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
        return "// Generated from " + n.file + " — do not edit.";
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

        // super(...) initializer for program superclasses
        ClassDecl progSuper = c.superclass != null ? program.classes.get(c.superclass.name) : null;
        if (progSuper != null) {
            Args superArgs = ct.superInit != null ? ct.superInit.args : new Args();
            // super.x params contribute as named args
            for (Param p : params) {
                if (p.isSuper) {
                    NamedArg na = new NamedArg();
                    na.name = p.name;
                    Ident id = new Ident();
                    id.name = p.name;
                    na.value = id;
                    superArgs.named.add(na);
                }
            }
            CtorDecl superCtor = progSuper.defaultCtor();
            // Nothing may precede super(...), so its arguments cannot be sequenced
            // through lifted temps; they keep the parameter order.
            inSuperInitializer = true;
            try {
                w.line("super(" + canonicalArgs(superCtor, superArgs, ctx) + ");");
            } finally {
                inSuperInitializer = false;
            }
        } else if (ct.superInit != null && ct.superInit.args != null
                && stubClassOf(c.superclass) != null) {
            for (NamedArg na : ct.superInit.args.named) {
                Out v = emitExpr(na.value, null, ctx);
                w.line("this." + na.name + "(" + v.code + ");");
            }
        }
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
                w.line("this." + p.name + " = " + javaIdent(p.name) + ";");
            }
        }
        // initializer list entries
        for (FieldInit fi : ct.fieldInits) {
            Out v = emitExpr(fi.value, typeOfField(c, fi.field, ctx), ctx);
            w.line("this." + fi.field + " = " + v.code + ";");
        }
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
        sb.append(ctx.popWriter());
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * factory Foo(...) / factory Foo.name(...) — a static method returning
     * the class; unnamed factories become {@code $create} and call sites
     * route through it.
     */
    private String emitFactoryCtor(ClassDecl c, CtorDecl ct, Ctx ctx) {
        StringBuilder sb = new StringBuilder();
        String name = ct.name == null ? "$create" : javaIdent(ct.name);
        ctx.pushScope();
        sb.append("    public static ").append(javaClassName(c)).append(' ').append(name).append('(');
        appendParams(sb, c, ct.params, ctx);
        sb.append(") {\n");
        ctx.pushWriter(2);
        ctx.methodReturnType = new TypeRef(c.name);
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
        sb.append(ctx.popWriter());
        ctx.methodReturnType = null;
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
    }

    /**
     * Dart named constructor C.name(...) — a private marker constructor
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
        sb.append("    public static ").append(javaClassName(c)).append(' ').append(javaIdent(ct.name))
                .append('(').append(paramSig).append(") {\n");
        sb.append("        ").append(javaClassName(c)).append(" $self = new ").append(javaClassName(c)).append("(($NamedCtor) null);\n");
        sb.append("        $self.$init$").append(ct.name).append('(').append(argList).append(");\n");
        sb.append("        return $self;\n    }\n\n");
        sb.append("    private void $init$").append(ct.name).append('(').append(paramSig).append(") {\n");
        ctx.pushWriter(2);
        Ctx.Writer w = ctx.writer();
        for (Param p : ct.params) {
            if (p.isThis) {
                w.line("this." + p.name + " = " + javaIdent(p.name) + ";");
            }
            if (p.isSuper) {
                diags.error(p, "E0206", "super parameters are not supported on named constructors yet");
            }
        }
        for (FieldInit fi : ct.fieldInits) {
            Out v = emitExpr(fi.value, typeOfField(c, fi.field, ctx), ctx);
            w.line("this." + fi.field + " = " + v.code + ";");
        }
        if (ct.body != null) {
            emitStatements(ct.body, ctx);
        }
        sb.append(ctx.popWriter());
        ctx.popScope();
        sb.append("    }\n\n");
        return sb.toString();
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
        TypeRef returnType;
        List<String> typeParams = new ArrayList<String>();
        List<Param> params = new ArrayList<Param>();
        Block body;
        Expr exprBody;
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
            ClassDecl sp = program.classes.get(p.superclass.name);
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
            ClassDecl sp = program.classes.get(p.superclass.name);
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
     * method at all — Java silently keeps the interface's default.
     *
     * <p>That is not a compile error and it does not throw. It just answers the
     * default forever: {@code Board with IterableMixin} iterated correctly and
     * handed every element back as null, so the 2D-transformations demo's
     * painter died on the first {@code boardPoint!} and the entire board — the
     * only content on that screen — never drew.
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

    private String paramTypeName(Param p) {
        return p.type == null || p.type.is("var") ? "dynamic" : p.type.name;
    }

    private String emitMethodLike(Method m, Ctx ctx, boolean classIsAbstract) {
        StringBuilder sb = new StringBuilder();
        TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
        // Dart's `int get hashCode` / `int compareTo(...)` map to Java's Object.hashCode /
        // Comparable.compareTo, which return primitive `int` (not the `long` Dart int uses).
        // Emit a Java `int` return (not `long`) so the override is valid; the body's long
        // result is narrowed with an explicit cast.
        boolean forceIntReturn = ("hashCode".equals(m.name) && m.params.isEmpty() && !m.isSetter)
                || ("compareTo".equals(m.name) && m.params.size() == 1 && !m.isSetter);
        if (m.isOverride) {
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
        String rjt = m.isSetter ? "void" : (forceIntReturn ? "int" : javaType(rt, false, ctx));
        sb.append(rjt).append(' ').append(m.name).append('(');
        for (int i = 0; i < m.params.size(); i++) {
            Param p = m.params.get(i);
            TypeRef pt = p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(ctx.declareShadowSafe(p.name, pt));
        }
        sb.append(')');
        if (m.isAbstract) {
            sb.append(";\n\n");
            ctx.popScope();
            return sb.toString();
        }
        sb.append(" {\n");
        ctx.pushWriter(2);
        ctx.methodReturnType = rt;
        ctx.inAsyncBody = m.isAsync;
        ctx.boxedLocals.clear();
        ctx.boxedLocals.addAll(m.body != null
                ? CaptureScan.boxedLocals(m.body) : CaptureScan.boxedLocals(m.exprBody));
        if (m.isSyncStar) {
            // sync* generator: collect yielded values into a DartList and return it (DartList is an
            // Iterable). `yield x` -> list.add(x); `yield* xs` -> list.addAllIterable(xs).
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
            if (asyncFuture) {
                Out o = emitExpr(m.exprBody, null, ctx);
                ctx.importClass("dart.async.Future");
                ctx.writer().line("return " + (o.type.is("Future") ? o.code
                        : "Future.value(" + boxIfPrimitive(o, ctx) + ")") + ";");
            } else {
                Out o = emitExpr(m.exprBody, rt.is("void") ? null : rt, ctx);
                if (rt.is("void")) {
                    ctx.writer().line(statementize(o.code) + ";");
                } else if (forceIntReturn) {
                    ctx.writer().line("return (int) (" + o.code + ");");
                } else {
                    ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
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

    /** Shallow check: does the block's last statement definitely leave the method? */
    private boolean endsWithJump(Block b) {
        if (b.statements.isEmpty()) {
            return false;
        }
        Stmt last = b.statements.get(b.statements.size() - 1);
        if (last instanceof ReturnStmt) {
            return true;
        }
        return last instanceof ExprStmt && ((ExprStmt) last).expr instanceof ThrowExpr;
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
        Ctx.Writer w = ctx.writer();
        if (s instanceof Block) {
            w.line("{");
            ctx.indent(1);
            ctx.pushScope();
            emitStatements((Block) s, ctx);
            ctx.popScope();
            ctx.indent(-1);
            w.line("}");
        } else if (s instanceof VarDeclStmt) {
            VarDeclStmt v = (VarDeclStmt) s;
            TypeRef declared = v.type;
            Out init = null;
            if (v.initializer != null) {
                init = emitExpr(v.initializer, declared != null && !declared.is("var") ? declared : null, ctx);
            }
            TypeRef t = declared == null || declared.is("var")
                    ? (init != null ? init.type : TypeRef.DYNAMIC) : declared;
            // untyped closure locals get a SAM type by arity
            if (t.is("Function") && v.initializer instanceof Lambda) {
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
            if ((v.type == null || v.type.is("var")) && init != null && containsDynamic(t)) {
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
            // A conditional used as a statement (`cond ? f() : g();`) — when its arms are void
            // (e.g. controller.reverse()/forward()) the ternary is not a valid Java expression
            // statement, so lower it to an if/else.
            if (ex instanceof Conditional) {
                Conditional c = (Conditional) ex;
                Out thenO = emitExpr(c.thenExpr, null, ctx);
                Out elseO = emitExpr(c.elseExpr, null, ctx);
                boolean voidArms = (thenO.type != null && thenO.type.is("void"))
                        || (elseO.type != null && elseO.type.is("void"));
                if (voidArms) {
                    Out cond = emitExpr(c.condition, TypeRef.BOOL, ctx);
                    w.line("if (" + cond.code + ") {");
                    ctx.indent(1);
                    String tc = statementize(thenO.code);
                    if (!tc.isEmpty()) {
                        w.line(tc + ";");
                    }
                    ctx.indent(-1);
                    w.line("} else {");
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
            if (ex instanceof Assign && isNullAwareMemberAssign((Assign) ex)) {
                emitNullAwareAssignStatement((Assign) ex, ctx);
                return;
            }
            Out o = emitExpr(ex, null, ctx);
            String code = statementize(o.code);
            if (!code.isEmpty()) {
                w.line(code + ";");
            }
        } else if (s instanceof YieldStmt) {
            YieldStmt y = (YieldStmt) s;
            if (ctx.syncStarList == null) {
                diags.error(y, "E0304", "yield outside a sync* generator body");
            } else if (y.star) {
                Out o = emitExpr(y.value, null, ctx);
                w.line(ctx.syncStarList + ".addAllIterable(" + o.code + ");");
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
                    String code = o.code;
                    // await already unwraps; returning a Future directly passes through
                    if (o.type.is("Future")) {
                        w.line("return " + code + ";");
                    } else {
                        w.line("return Future.value(" + boxIfPrimitive(o, ctx) + ");");
                    }
                }
                return;
            }
            if (r.value == null) {
                w.line("return;");
            } else if (rt != null && rt.is("void")) {
                // Dart allows `return expr;` from a method Java-typed void (it overrides a void
                // base). Keep a side-effecting call; drop a pure read (not a valid Java statement).
                Out o = emitExpr(r.value, null, ctx);
                String code = statementize(o.code);
                if (!code.isEmpty() && code.contains("(")) {
                    w.line(code + ";");
                }
                w.line("return;");
            } else if (ctx.narrowReturnToInt) {
                Out o = emitExpr(r.value, null, ctx);
                w.line("return (int) (" + o.code + ");");
            } else {
                Out o = emitExpr(r.value, rt, ctx);
                w.line("return " + (rt != null ? coerce(o, rt, ctx) : o.code) + ";");
            }
        } else if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            w.line("try {");
            ctx.indent(1);
            ctx.pushScope();
            emitStatements(t.tryBlock, ctx);
            ctx.popScope();
            ctx.indent(-1);
            for (CatchClause cc : t.catches) {
                String exType = cc.onType != null
                        ? javaType(cc.onType, true, ctx) : "RuntimeException";
                // A dart:core error named only in an `on` clause still needs its
                // import: `on FormatException` compiled only in a file that happened
                // to construct one somewhere else.
                String coreError = cc.onType != null ? CORE_ERRORS.get(cc.onType.name) : null;
                if (coreError != null && !cc.onType.name.equals("Exception")) {
                    ctx.importClass(coreError);
                    exType = coreError.substring(coreError.lastIndexOf('.') + 1);
                }
                ctx.pushScope();
                String var = ctx.declareShadowSafe(cc.exceptionVar != null ? cc.exceptionVar : "$e",
                        cc.onType != null ? cc.onType : TypeRef.DYNAMIC);
                w.line("} catch (" + exType + " " + var + ") {");
                ctx.indent(1);
                if (cc.stackVar != null) {
                    // stack traces are not modeled; bind the name for compilation
                    String stackJn = ctx.declareShadowSafe(cc.stackVar, TypeRef.DYNAMIC);
                    w.line("Object " + stackJn + " = null;");
                }
                emitStatements(cc.body, ctx);
                ctx.popScope();
                ctx.indent(-1);
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
            if (i.elseStmt != null) {
                w.line("} else {");
                ctx.indent(1);
                ctx.pushScope();
                emitStatement(unwrapBlock(i.elseStmt), ctx);
                ctx.popScope();
                ctx.indent(-1);
            }
            w.line("}");
        } else if (s instanceof WhileStmt) {
            WhileStmt wh = (WhileStmt) s;
            Out c = emitExpr(wh.condition, TypeRef.BOOL, ctx);
            w.line("while (" + c.code + ") {");
            ctx.indent(1);
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
                initCode = statementize(emitExpr(((ExprStmt) f.init).expr, null, ctx).code);
            }
            String cond = f.condition != null ? emitExpr(f.condition, TypeRef.BOOL, ctx).code : "";
            StringBuilder updates = new StringBuilder();
            for (int i = 0; i < f.updates.size(); i++) {
                if (i > 0) {
                    updates.append(", ");
                }
                updates.append(statementize(emitExpr(f.updates.get(i), null, ctx).code));
            }
            w.line("for (" + initCode + "; " + cond + "; " + updates + ") {");
            ctx.indent(1);
            // Dart binds the loop variable fresh each iteration, so a closure in the
            // body captures a distinct value per pass. The Java loop variable is
            // reassigned by the update clause (not effectively final), so emit a
            // per-iteration final alias and route body references through it.
            if (forVarDart != null && CaptureScan.readInLambda(f.body, forVarDart)) {
                String alias = ctx.declareShadowSafe(forVarDart, forVarType);
                w.line("final " + javaType(forVarType, false, ctx) + " " + alias
                        + " = " + forVarJava + ";");
            }
            ctx.pushBreakTarget(null);
            emitStatement(unwrapBlock(f.body), ctx);
            ctx.popBreakTarget();
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        } else if (s instanceof ForInStmt
                && isIndexedRecordFor(((ForInStmt) s).pattern, ((ForInStmt) s).iterable)) {
            // `for (final (int i, E e) in xs.indexed)` — Dart's Iterable.indexed pairs each
            // element with its position. There is no runtime `indexed`, so lower to a counted
            // loop that binds the index and element subpatterns directly.
            final ForInStmt f = (ForInStmt) s;
            emitIndexedFor(f.pattern, f.iterable, ctx, new Runnable() {
                public void run() {
                    ctx.pushBreakTarget(null);
                    emitStatement(unwrapBlock(f.body), ctx);
                    ctx.popBreakTarget();
                }
            });
        } else if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            Out iter = emitExpr(f.iterable, null, ctx);
            TypeRef elem = f.varType != null && !f.varType.is("var")
                    ? f.varType
                    : (iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC);
            ctx.pushScope();
            ctx.pushBreakTarget(null);
            if (f.pattern != null) {
                // Dart 3 pattern for-in: bind a temp per element, then destructure into the pattern.
                String loopVar = ctx.newTemp();
                w.line("for (" + javaType(elem, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
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
                w.line("for (" + javaType(elem, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
                ctx.indent(1);
                emitStatement(unwrapBlock(f.body), ctx);
                ctx.indent(-1);
                w.line("}");
            }
            ctx.popBreakTarget();
            ctx.popScope();
        } else if (s instanceof BreakStmt) {
            String bl = ctx.currentBreakLabel();
            w.line(bl != null ? "break " + bl + ";" : "break;");
        } else if (s instanceof ContinueStmt) {
            w.line("continue;");
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
        l.params = lf.params;
        l.body = lf.body;
        l.exprBody = lf.exprBody;
        // Declare the local first so a recursive body can reference the name.
        String jn = ctx.declareShadowSafe(lf.name, new TypeRef("Function"));
        Out init = emitExpr(l, funcTypeRef(lf.returnType, lf.params), ctx);
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
        // an unregistered name yields null there, which is fine — params carry their own types.
        return new TypeRef("Function");
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
        String cond = patternMatch(i.casePattern, temp, subj.type, ctx, binds);
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
        w.line(label + ": {");
        ctx.indent(1);
        ctx.pushBreakTarget(label);
        SwitchCase defaultCase = null;
        // Tracks whether control can leave the switch block normally (a case that `break`s to
        // after the block rather than returning/throwing). When every case returns and there is
        // no default, a Dart-exhaustive switch leaves the block unreachable — a trailing throw
        // then keeps a value-returning method/lambda definitely-assigned in Java.
        boolean anyFallThrough = false;
        boolean hasContentCase = false;
        // an empty non-default case falls through to the next case's body (Dart's only fallthrough)
        List<String> pending = new ArrayList<String>();
        for (SwitchCase c : sw.cases) {
            if (c.isDefault) {
                defaultCase = c;
                continue;
            }
            ctx.pushScope();
            List<String> binds = new ArrayList<String>();
            String cond = patternMatch(c.pattern, s, subj.type, ctx, binds);
            if (c.body.isEmpty() && c.guard == null) {
                if (!binds.isEmpty()) {
                    diags.error(c, "E0436", "An empty fall-through case cannot bind variables");
                }
                pending.add(cond);
                ctx.popScope();
                continue;
            }
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
        ctx.popBreakTarget();
        ctx.indent(-1);
        w.line("}");
        // Dart-exhaustive switch (an enum subject, no default) where every arm returns/throws:
        // the post-block fall-through is unreachable in Dart, so emit an unreachable throw to
        // satisfy Java's definite-return analysis. Only for enum subjects — a String/int switch is
        // never exhaustive and legitimately falls through to code after it.
        boolean enumSubject = subj.type != null
                && (program.enums.containsKey(subj.type.name) || stubs.isStubEnum(subj.type.name));
        if (defaultCase == null && hasContentCase && !anyFallThrough && enumSubject) {
            ctx.importClass("dart.runtime.DartRuntime");
            w.line("throw DartRuntime.asError(\"No matching switch case\");");
        }
        ctx.popScope();
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
        if (last instanceof ReturnStmt || last instanceof BreakStmt || last instanceof ContinueStmt) {
            return true;
        }
        if (last instanceof ExprStmt && ((ExprStmt) last).expr instanceof ThrowExpr) {
            return true;
        }
        if (last instanceof Block) {
            List<Stmt> ss = ((Block) last).statements;
            return !ss.isEmpty() && stmtTerminates(ss.get(ss.size() - 1));
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
        // must initialize only after every other static field — covers the common
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
            String cond = patternMatch(c.pattern, s, subj.type, ctx, binds);
            w.line("if (" + cond + ") {");
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
            Out v = emitExpr(c.value, resultType, ctx);
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
        w.line("for (" + javaType(elemT, true, ctx) + " " + el + " : " + base.code + ") {");
        ctx.indent(1);
        ctx.declare(el, elemT);
        List<String> binds = new ArrayList<String>();
        patternMatch(rp.fields.get(0).pattern, counter, TypeRef.INT, ctx, binds);
        patternMatch(rp.fields.get(1).pattern, el, elemT, ctx, binds);
        for (String b : binds) {
            w.line(b);
        }
        body.run();
        w.line(counter + "++;");
        ctx.indent(-1);
        w.line("}");
        ctx.popScope();
    }

    /**
     * Emits the binding declarations for a pattern matched against {@code subj} (a temp holding the
     * scrutinee) into {@code binds}, and returns the boolean match condition. Bound variables are
     * declared into the current scope so the case body and guard can reference them.
     */
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
                    && program.enums.get(subjType.name).hasEntry(v.name)) {
                return subj + " == " + subjType.name + "." + v.name;
            }
            if (v.type != null && isReferenceType(v.type)) {
                String jt = javaType(v.type, true, ctx);
                String nm = ctx.declareShadowSafe(v.name, v.type);
                binds.add(jt + " " + nm + " = (" + jt + ") " + subj + ";");
                return subj + " instanceof " + jt;
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
        if (p instanceof CastPattern) {
            CastPattern c = (CastPattern) p;
            String jt = javaType(c.type, true, ctx);
            String cast = "((" + jt + ") " + subj + ")";
            return joinAnd(subj + " instanceof " + jt, patternMatch(c.inner, cast, c.type, ctx, binds));
        }
        if (p instanceof ObjectPattern) {
            ObjectPattern o = (ObjectPattern) p;
            String jt = javaType(o.type, true, ctx);
            String cond = subj + " instanceof " + jt;
            String cast = "((" + jt + ") " + subj + ")";
            for (PatternField f : o.fields) {
                String access = cast + "." + fieldAccess(o.type, f.name) + "()";
                TypeRef ft = fieldTypeOf(o.type, f.name);
                cond = joinAnd(cond, patternMatch(f.pattern, access, ft, ctx, binds));
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
            return subj + " instanceof " + javaType(type, true, ctx);
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
            return emitListLit((ListLit) e, expected, ctx);
        }
        if (e instanceof MapLit) {
            return emitMapLit((MapLit) e, expected, ctx);
        }
        if (e instanceof SetLit) {
            return emitSetLit((SetLit) e, expected, ctx);
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
            ctx.pushCascadeTarget(new Out(tmp, target.type));
            for (Expr section : cas.sections) {
                Out o = emitExpr(section, null, ctx);
                String code = statementize(o.code);
                if (!code.isEmpty()) {
                    ctx.writer().line(code + ";");
                }
            }
            ctx.popCascadeTarget();
            return new Out(tmp, target.type);
        }
        if (e instanceof ThrowExpr) {
            diags.error(e, "E0205", "throw is only supported in statement position in M2");
            return new Out("null", TypeRef.DYNAMIC);
        }
        if (e instanceof AwaitExpr) {
            Out o = emitExpr(((AwaitExpr) e).operand, null, ctx);
            // Awaiting a void-typed operand (e.g. a `void` stub call) yields nothing; wrapping it
            // in Await.await$(...) is a "void not allowed here" error, so emit the bare call.
            if (o.type != null && o.type.is("void")) {
                return new Out(o.code, TypeRef.VOID);
            }
            ctx.importClass("dart.async.Await");
            TypeRef inner = o.type.is("Future") ? o.type.arg(0) : TypeRef.DYNAMIC;
            return new Out("Await.await$(" + o.code + ")", boxType(inner));
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
            return emitPropertyGet((PropertyGet) e, ctx);
        }
        if (e instanceof Call) {
            return emitCall((Call) e, expected, ctx);
        }
        if (e instanceof CtorCall) {
            CtorCall cc = (CtorCall) e;
            if (cc.ctorName != null) {
                // List<E>.generate / .filled / .from — dart:core intrinsic factories,
                // routed to the primitive Dart*List when E is a non-nullable int/double.
                if (cc.type.name.equals("List")
                        && (cc.ctorName.equals("generate") || cc.ctorName.equals("filled")
                            || cc.ctorName.equals("from"))) {
                    return emitListFactory(cc, ctx);
                }
                // Map/Set/Iterable named factory constructors — dart:core intrinsics
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
                    return new Out(javaClassName(pc) + "." + javaIdent(cc.ctorName) + "("
                            + canonicalArgs(pc.namedCtor(cc.ctorName), cc.args, ctx) + ")",
                            new TypeRef(cc.type.name));
                }
                Ast.ClassDecl sc = stubs.classes.get(cc.type.name);
                if (sc != null) {
                    // stub named ctors are declared as static methods
                    Ast.MethodDecl m = stubs.findMethod(cc.type.name, cc.ctorName, false);
                    if (m != null && m.isStatic) {
                        return new Out(stubSimpleName(cc.type.name, ctx) + "." + cc.ctorName + "("
                                + stubMethodArgs(m, cc.args, ctx) + ")", m.returnType);
                    }
                }
                diags.error(e, "E0126", "Cannot resolve named constructor " + cc.type.name + "." + cc.ctorName);
                return new Out("null", TypeRef.DYNAMIC);
            }
            return emitCtorCall(cc.type.name, cc.type.args, cc.args, e, ctx);
        }
        if (e instanceof IndexGet) {
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
            return new Out(u.op + paren(o.code), o.type);
        }
        if (e instanceof IncDec) {
            IncDec id = (IncDec) e;
            Out target = emitExpr(id.operand, null, ctx);
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
        }
        if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            Out cond = emitExpr(c.condition, TypeRef.BOOL, ctx);
            // `x is T ? x.member : ...` promotes x to T in the then-branch.
            List<Object[]> undo = applyGuardPromotions(c.condition, ctx);
            Out a = emitExpr(c.thenExpr, expected, ctx);
            restorePromotions(undo, ctx);
            Out b = emitExpr(c.elseExpr, expected, ctx);
            TypeRef t = conditionalType(a.type, b.type, expected);
            boolean mixedNumeric = a.type != null && b.type != null
                    && ((a.type.is("int") && b.type.is("double")) || (a.type.is("double") && b.type.is("int")))
                    && expected != null
                    && (expected.is("Object") || expected.is("num") || expected.is("dynamic"));
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
            return new Out("(" + cond.code + " ? " + a.code + " : " + b.code + ")", t);
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
            String subject = o.code;
            if (o.type != null && !o.type.nullable
                    && (o.type.is("int") || o.type.is("double") || o.type.is("bool"))) {
                // A primitive operand cannot be tested with instanceof; the cast boxes it,
                // so `min(1, 2) is int` tests the Long it is.
                subject = "((Object) " + paren(o.code) + ")";
            }
            String check = subject + " instanceof " + javaType(t.type, true, ctx);
            return new Out(t.negated ? "!(" + check + ")" : "(" + check + ")", TypeRef.BOOL);
        }
        if (e instanceof AsCast) {
            AsCast c = (AsCast) e;
            Out o = emitExpr(c.operand, null, ctx);
            return new Out("((" + javaType(c.type, true, ctx) + ") " + o.code + ")", c.type);
        }
        if (e instanceof Lambda) {
            return emitLambda((Lambda) e, expected, ctx);
        }
        diags.error(e, "E0128", "Unsupported expression in emitter: " + e.getClass().getSimpleName());
        return new Out("null", TypeRef.DYNAMIC);
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
        for (Expr e : l.elements) {
            Out o = emitExpr(e, elem, ctx);
            codes.add(elementCode(o, elem, ctx));
            if (inferred == null) {
                inferred = o.type;
            }
        }
        if (elem == null) {
            elem = inferred != null ? inferred : TypeRef.DYNAMIC;
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
                w.line(list + ".addAllIterable(" + src.code + ");");
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
                    initCode = statementize(emitExpr(((ExprStmt) f.init).expr, null, ctx).code);
                }
                String cond = f.condition != null ? emitExpr(f.condition, TypeRef.BOOL, ctx).code : "";
                StringBuilder updates = new StringBuilder();
                for (int i = 0; i < f.updates.size(); i++) {
                    if (i > 0) {
                        updates.append(", ");
                    }
                    updates.append(statementize(emitExpr(f.updates.get(i), null, ctx).code));
                }
                w.line("for (" + initCode + "; " + cond + "; " + updates + ") {");
                ctx.indent(1);
                emitListElementInto(list, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
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
                w.line(set + ".addAllIterable(" + src.code + ");");
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
                    initCode = statementize(emitExpr(((ExprStmt) f.init).expr, null, ctx).code);
                }
                String cond = f.condition != null ? emitExpr(f.condition, TypeRef.BOOL, ctx).code : "";
                StringBuilder updates = new StringBuilder();
                for (int i = 0; i < f.updates.size(); i++) {
                    if (i > 0) {
                        updates.append(", ");
                    }
                    updates.append(statementize(emitExpr(f.updates.get(i), null, ctx).code));
                }
                w.line("for (" + initCode + "; " + cond + "; " + updates + ") {");
                ctx.indent(1);
                emitSetElementInto(set, f.body, elem, ctx);
                ctx.indent(-1);
                w.line("}");
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
                    initCode = statementize(emitExpr(((ExprStmt) f.init).expr, null, ctx).code);
                }
                String cond = f.condition != null ? emitExpr(f.condition, TypeRef.BOOL, ctx).code : "";
                StringBuilder updates = new StringBuilder();
                for (int i = 0; i < f.updates.size(); i++) {
                    if (i > 0) {
                        updates.append(", ");
                    }
                    updates.append(statementize(emitExpr(f.updates.get(i), null, ctx).code));
                }
                w.line("for (" + initCode + "; " + cond + "; " + updates + ") {");
                ctx.indent(1);
                emitMapElementInto(map, f.body, kt, vt, ctx);
                ctx.indent(-1);
                w.line("}");
            }
            ctx.popScope();
            return;
        }
        diags.error(e, "E0205", "Unsupported map-literal element in emitter");
    }

    private Out emitIdent(Ident id, TypeRef expected, Ctx ctx) {
        String n = id.name;
        TypeRef local = ctx.lookup(n);
        if (local != null) {
            String jn = ctx.javaNameOf(n);
            String base = ctx.isBoxed(n) ? jn + ".v" : jn;
            // Flow-promoted by an `is` guard: read as the narrowed type via a cast.
            TypeRef promo = ctx.promotedType(n);
            if (promo != null) {
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
                if (isLazyStatic(f)) {
                    return new Out(javaClassName(cc) + ".get$" + n + "()", fieldType(f, ctx));
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
                return new Out((md.isStatic ? cc.name : "this") + "::" + n, new TypeRef("Function"));
            }
            // field/getter inherited from a program superclass. The declared field is
            // private in the superclass, so reads route through its public get$ accessor;
            // a getter is a public zero-arg method.
            FieldDecl inhF = findInheritedField(cc, n);
            if (inhF != null && !inhF.isStatic && !inhF.name.startsWith("_")) {
                return new Out("this.get$" + n + "()", fieldType(inhF, ctx));
            }
            MethodDecl inhG = findInheritedGetter(cc, n);
            if (inhG != null) {
                return new Out("this." + n + "()", inhG.returnType);
            }
            MethodDecl inhM = findMethodInHierarchy(cc, n);
            if (inhM != null) {
                return new Out("this::" + n, new TypeRef("Function"));
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
            if (program.enums.containsKey(cc.name)) {
                Ast.EnumDecl selfEnum = program.enums.get(cc.name);
                if (selfEnum.hasEntry(n)) {
                    return new Out(cc.name + "." + n, new TypeRef(cc.name));
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
        Out top = resolveTopLevel(n, ctx);
        if (top != null) {
            return top;
        }
        diags.error(id, "E0129", "Cannot resolve identifier '" + n
                + "'. Confirm the file passes `dart analyze`.");
        return new Out(n, TypeRef.DYNAMIC, true);
    }

    /**
     * Resolves a name against whole-program top-level scope: a user or stub class /
     * enum, a top-level const/var, a top-level function (as a method reference), or a
     * stub-contributed top-level value. Returns null when the name is not top-level.
     * Shared by bare-identifier resolution and import-prefix ({@code prefix.name})
     * member access — both denote the same global namespace in the single-package model.
     */
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
                || n.equals("Future") || n.equals("Duration")) {
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
            return new Out(Program.libClassName(owner.fileName) + "::" + n, new TypeRef("Function"));
        }
        // Top-level library values contributed by a stub (@JavaName maps the
        // Dart top-level `pi` / `timeDilation` / `defaultTargetPlatform` to a
        // fully-qualified Java static field). Both reads and writes route here.
        FieldDecl stubVar = stubs.topLevelVars.get(n);
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
        ClassDecl s = c.superclass != null ? program.classes.get(c.superclass.name) : null;
        while (s != null) {
            FieldDecl f = s.field(name);
            if (f != null) {
                return f;
            }
            s = s.superclass != null ? program.classes.get(s.superclass.name) : null;
        }
        return null;
    }

    private MethodDecl findInheritedGetter(ClassDecl c, String name) {
        ClassDecl s = c.superclass != null ? program.classes.get(c.superclass.name) : null;
        while (s != null) {
            MethodDecl g = s.getter(name);
            if (g != null) {
                return g;
            }
            s = s.superclass != null ? program.classes.get(s.superclass.name) : null;
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
    private Out emitMemberGet(Out target, String name, Node posNode, Ctx ctx) {
        TypeRef tt = target.type;
        // A field/param typed with an import prefix (`intl.DateFormat`, `ui.Size`) keeps the
        // prefix in its type name; strip it so member resolution sees the real stub type.
        if (tt != null && tt.name != null && tt.name.indexOf('.') > 0) {
            tt.name = stripImportPrefix(tt.name);
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
            return new Out(target.code + "." + name, TypeRef.DYNAMIC);
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
                    return new Out(stubSimpleName(cls, ctx) + "." + name, g.returnType);
                }
                Ast.MethodDecl m = stubs.findMethod(cls, name, false);
                if (m != null && m.isStatic) {
                    // static method tear-off — unsupported
                    diags.error(posNode, "E0130", "Static method tear-offs are not supported yet");
                    return new Out("null", TypeRef.DYNAMIC);
                }
            }
            ClassDecl pc = program.classes.get(cls);
            if (pc != null) {
                FieldDecl f = pc.field(name);
                if (f != null && f.isStatic) {
                    return new Out(cls + (isLazyStatic(f) ? ".get$" + name + "()" : "." + name),
                            fieldType(f, ctx));
                }
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
            if (name.equals("reversed")) {
                return new Out(target.code + ".reversed()", TypeRef.of("Iterable", elem));
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
        }
        // implicit instance members on a program enum value: .index (int), .name (String)
        if (program.enums.containsKey(tt.name)) {
            if (name.equals("index")) {
                return new Out(target.code + ".ordinal()", TypeRef.INT);
            }
            if (name.equals("name")) {
                return new Out(target.code + ".name()", TypeRef.STRING);
            }
            // enhanced-enum getter declared in the enum body
            Ast.EnumDecl ed = program.enums.get(tt.name);
            MethodDecl eg = ed.getter(name);
            if (eg != null) {
                return new Out(target.code + "." + name + "()",
                        eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
            }
        }
        // program class member
        ClassDecl pc = programClass(tt.name, ctx);
        if (pc != null) {
            FieldDecl f = pc.field(name);
            if (f != null) {
                if (target.code.equals("this")) {
                    return new Out("this." + name, fieldType(f, ctx));
                }
                return new Out(target.code + ".get$" + name + "()", fieldType(f, ctx));
            }
            MethodDecl g = pc.getter(name);
            if (g != null) {
                return new Out(target.code + "." + name + "()", g.returnType);
            }
            Object mixF = findMixinMember(pc, name, true);
            if (mixF instanceof FieldDecl) {
                return new Out(target.code + ".get$" + name + "()", fieldType((FieldDecl) mixF, ctx));
            }
            Object mixG = findMixinMember(pc, name, false);
            if (mixG instanceof MethodDecl && ((MethodDecl) mixG).isGetter) {
                return new Out(target.code + "." + name + "()", ((MethodDecl) mixG).returnType);
            }
            // field/getter inherited from a program superclass
            FieldDecl inhF = findInheritedField(pc, name);
            if (inhF != null && !inhF.name.startsWith("_")) {
                return new Out(target.code + ".get$" + name + "()", fieldType(inhF, ctx));
            }
            MethodDecl inhG = findInheritedGetter(pc, name);
            if (inhG != null) {
                return new Out(target.code + "." + name + "()", inhG.returnType);
            }
            // method tear-off on an instance: `obj.method` as a function value (method reference)
            MethodDecl tm = pc.method(name);
            if (tm == null) {
                tm = findMethodInHierarchy(pc, name);
            }
            if (tm != null && !tm.isStatic) {
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
                        return new Out(target.code + "." + name + "()", sg.returnType);
                    }
                }
            }
        }
        // `.mounted` (bool) — on a State receiver, or the BuildContext.mounted guard;
        // neither is part of the minimal State/BuildContext stub surface.
        if ((tt.is("State") || tt.is("BuildContext")) && name.equals("mounted")) {
            return new Out(target.code + ".mounted()", TypeRef.BOOL);
        }
        ClassDecl extCls = program.findExtension(tt.name, name, true);
        if (extCls != null) {
            MethodDecl eg = extCls.getter(name);
            return new Out(extCls.name + "." + name + "(" + target.code + ")",
                    eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
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
        if (tt.is("Stopwatch")) {
            TypeRef rt = name.equals("isRunning") ? TypeRef.BOOL
                    : name.equals("elapsed") ? new TypeRef("Duration") : TypeRef.INT;
            return new Out(target.code + "." + name + "()", rt);
        }
        diags.error(posNode, "E0132", "Cannot resolve member '" + name + "' on type " + tt
                + ". Confirm the file passes `dart analyze`.");
        return new Out(target.code + "." + name, TypeRef.DYNAMIC, true);
    }

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
        Out target = emitExpr(ig.target, null, ctx);
        Out idx = emitExpr(ig.index, null, ctx);
        TypeRef tt = target.type;
        ClassDecl opClass = program.classes.get(tt.name);
        if (opClass != null) {
            MethodDecl om = findMethodInHierarchy(opClass, "$index");
            if (om != null) {
                return new Out(target.code + ".$index(" + idx.code + ")",
                        om.returnType == null || om.returnType.is("var") ? TypeRef.DYNAMIC : om.returnType);
            }
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
        String write = statementize(emitAssign(plainAssign(a, lhs), ctx).code);
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
        Out write = emitAssign(plainAssign(a, lhs), ctx);
        boolean yieldsValue = write.code.startsWith(read.code + " = ");
        if (lhs instanceof IndexGet) {
            String receiver = emitExpr(((IndexGet) lhs).target, null, ctx).code;
            yieldsValue = write.code.startsWith(receiver + ".idxSet(");
        }
        if (!yieldsValue) {
            diags.error(a, "E0141", "'??=' on an operator []= or a setter is supported as a statement,"
                    + " not yet as a value. Assign first, then read the target.");
        }
        ctx.importClass("dart.runtime.DartRuntime");
        String chain = "true";
        for (int k = inline.size() - 1; k >= 0; k--) {
            chain = "DartRuntime.seq(" + inline.get(k) + ", " + chain + ")";
        }
        return new Out("(" + chain + " && " + cur + " != null ? " + cur + " : (" + write.code + "))", rt);
    }

    private Out emitAssign(Assign a, Ctx ctx) {
        if (isNullAwareMemberAssign(a)) {
            return emitNullAwareAssignValue(a, ctx);
        }
        // ??= on a variable: a local or field is a valid Java assignment target.
        if (a.op.equals("??=")) {
            Out lhs = emitExpr(a.lhs, null, ctx);
            Out rhs = emitExpr(a.rhs, lhs.type, ctx);
            if (lhs.code.endsWith("()") && lhs.code.contains(".get$")) {
                // A lazily initialised static or top-level is an accessor pair; its
                // setter answers the stored value.
                String base = lhs.code.substring(0, lhs.code.lastIndexOf(".get$"));
                String prop = lhs.code.substring(lhs.code.lastIndexOf(".get$") + 5, lhs.code.length() - 2);
                return new Out("(" + lhs.code + " == null ? " + base + ".set$" + prop + "("
                        + coerce(rhs, lhs.type, ctx) + ") : " + lhs.code + ")", lhs.type);
            }
            return new Out("(" + lhs.code + " == null ? (" + lhs.code + " = " + rhs.code + ") : " + lhs.code + ")",
                    lhs.type);
        }
        if (a.lhs instanceof IndexGet) {
            IndexGet ig = (IndexGet) a.lhs;
            Out target = emitExpr(ig.target, null, ctx);
            Out idx = emitExpr(ig.index, null, ctx);
            boolean compound = !a.op.equals("=");
            if (compound) {
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
                String rhsCode = compound
                        ? compoundValue(target.code + ".$index(" + idx.code + ")", ivt, a, ctx)
                        : emitExpr(a.rhs, null, ctx).code;
                return new Out(target.code + ".$indexSet(" + idx.code + ", " + rhsCode + ")",
                        ivt != null ? ivt : TypeRef.DYNAMIC);
            }
            TypeRef vt = target.type.is("Map") ? target.type.arg(1) : target.type.arg(0);
            String pk = primitiveListKind(target.type);
            if (pk != null) {
                String rhsCode = compound
                        ? compoundValue(target.code + ".get" + pk + "(" + idx.code + ")", vt, a, ctx)
                        : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
                return new Out(target.code + ".set" + pk + "(" + idx.code + ", " + rhsCode + ")", vt);
            }
            // Primitive long->long map: m[k] = v -> putLong(k, v), no boxing.
            if (isPrimitiveLongMap(target.type)) {
                String rhsCode = compound
                        ? compoundValue(target.code + ".idxLong(" + idx.code + ")", vt, a, ctx)
                        : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
                return new Out(target.code + ".putLong(" + idx.code + ", " + rhsCode + ")", vt);
            }
            String key = target.type.is("Map") ? boxIfPrimitive(idx, ctx) : idx.code;
            String rhsCode = compound
                    ? compoundValue(target.code + ".idx(" + key + ")", vt, a, ctx)
                    : coerce(emitExpr(a.rhs, vt, ctx), vt, ctx);
            return new Out(target.code + ".idxSet(" + key + ", " + rhsCode + ")", vt);
        }
        // assignment to a stub property that declares a Dart setter:
        // `x.value = v` -> the overloaded setter method `x.value(v)`. Compound forms
        // (`x.value -= d`) read through the getter: `x.value(x.value() - d)`.
        if (a.lhs instanceof PropertyGet) {
            PropertyGet pg = (PropertyGet) a.lhs;
            Out tgt = emitExpr(pg.target, null, ctx);
            if (tgt.type != null && (stubs.isStubClass(tgt.type.name) || tgt.type.is("State"))) {
                Ast.MethodDecl setter = stubs.findSetter(tgt.type.name, pg.name);
                if (setter != null) {
                    TypeRef pt = setter.params.isEmpty() ? TypeRef.DYNAMIC : setter.params.get(0).type;
                    String val = a.op.equals("=")
                            ? coerce(emitExpr(a.rhs, pt, ctx), pt, ctx)
                            : compoundValue(tgt.code + "." + pg.name + "()", pt, a, ctx);
                    return new Out(tgt.code + "." + pg.name + "(" + val + ")", pt);
                }
            }
            // App class (or its supers) declaring `set prop(v)` — emitted as the overloaded
            // instance method `prop(v)`, so `x.prop = v` becomes `x.prop(v)` (never `x.prop() = v`).
            if (a.op.equals("=") && tgt.type != null) {
                ClassDecl tc = program.resolveClass(tgt.type.name, ctx.library());
                Ast.MethodDecl setter = findAppSetter(tc, pg.name);
                if (setter != null) {
                    TypeRef pt = setter.params.isEmpty() ? TypeRef.DYNAMIC : setter.params.get(0).type;
                    Out rhs = emitExpr(a.rhs, pt, ctx);
                    return new Out(tgt.code + "." + pg.name + "(" + coerce(rhs, pt, ctx) + ")", pt);
                }
            }
        }
        // assignment to a top-level setter: `x = v` where `set x(v)` is declared at
        // library scope -> the static setter method `OwnerLib.x(v)`.
        if (a.op.equals("=") && a.lhs instanceof Ident) {
            String nm = ((Ident) a.lhs).name;
            if (ctx.lookup(nm) == null
                    && (ctx.currentClass == null || ctx.currentClass.field(nm) == null)
                    && program.topLevelSetters.containsKey(nm)) {
                Library owner = program.topLevelSetters.get(nm);
                Out rhs = emitExpr(a.rhs, null, ctx);
                return new Out(Program.libClassName(owner.fileName) + "." + nm
                        + "(" + rhs.code + ")", rhs.type);
            }
        }
        Out lhs = emitExpr(a.lhs, null, ctx);
        String lcode = lhs.code;
        // setters through accessors: x.get$f() as assignment target -> x.set$f(v)
        if (lcode.endsWith("()") && lcode.contains(".get$")) {
            String base = lcode.substring(0, lcode.lastIndexOf(".get$"));
            String prop = lcode.substring(lcode.lastIndexOf(".get$") + 5, lcode.length() - 2);
            // `x op= v` through an accessor pair -- a lazily initialised static or
            // top-level, or a mixin's field -- is set$x(get$x() op v).
            String value = a.op.equals("=")
                    ? coerce(emitExpr(a.rhs, lhs.type, ctx), lhs.type, ctx)
                    : compoundValue(lcode, lhs.type, a, ctx);
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
        String jop = a.op.equals("~/=") ? null : a.op;
        if (a.op.equals("~/=") || a.op.equals("%=")) {
            ctx.importClass("dart.runtime.DartRuntime");
            Out rhs = emitExpr(a.rhs, lhs.type, ctx);
            String fn = a.op.equals("~/=") ? "tdiv" : "mod";
            return new Out(lcode + " = DartRuntime." + fn + "(" + lcode + ", " + rhs.code + ")", lhs.type);
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

    private void collectPromotions(Expr cond, Ctx ctx, List<Object[]> undo) {
        if (cond instanceof Binary && "&&".equals(((Binary) cond).op)) {
            collectPromotions(((Binary) cond).left, ctx, undo);
            collectPromotions(((Binary) cond).right, ctx, undo);
            return;
        }
        if (cond instanceof IsTest) {
            IsTest t = (IsTest) cond;
            if (!t.negated && t.operand instanceof Ident && t.type != null
                    && ctx.lookup(((Ident) t.operand).name) != null) {
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
        return anc != null ? anc : new TypeRef("Object");
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

    private Out emitBinary(Binary b, Ctx ctx) {
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
            Out right;
            if (b.left instanceof Ident || b.left instanceof ThisExpr) {
                // A variable read has no effect and nothing to cache.
                right = emitExpr(b.right, left.type, ctx);
                return new Out("(" + left.code + " != null ? " + left.code + " : " + right.code + ")",
                        copyNonNull(left.type));
            }
            TypeRef lt = left.type;
            String jt = lt == null || lt.is("var") || lt.funcParams != null ? null : javaType(lt, true, ctx);
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
            right = emitExpr(b.right, left.type, ctx);
            return new Out("((" + tmp + " = " + left.code + ") != null ? " + tmp + " : " + right.code + ")",
                    copyNonNull(left.type));
        }
        if (b.op.equals("&&")) {
            // `x is T && x.member`: the left `is` guard flow-promotes x to T for the right operand.
            Out l = emitExpr(b.left, null, ctx);
            List<Object[]> undo = applyGuardPromotions(b.left, ctx);
            Out r = emitExpr(b.right, null, ctx);
            restorePromotions(undo, ctx);
            return new Out(paren(l.code) + " && " + paren(r.code), TypeRef.BOOL);
        }
        if (b.op.equals("||")) {
            // `x is! T || x.member`: reaching the right operand means the left was false,
            // i.e. `x is T` held — flow-promote x to T for the right operand.
            Out l = emitExpr(b.left, null, ctx);
            List<Object[]> undo = new ArrayList<Object[]>();
            collectNegativePromotions(b.left, ctx, undo);
            Out r = emitExpr(b.right, null, ctx);
            restorePromotions(undo, ctx);
            return new Out(paren(l.code) + " || " + paren(r.code), TypeRef.BOOL);
        }
        Out l = emitExpr(b.left, null, ctx);
        Out r = emitExpr(b.right, null, ctx);
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
            if (numeric || (l.type.is("bool") && r.type.is("bool"))) {
                return new Out(paren(l.code) + " " + b.op + " " + paren(r.code), TypeRef.BOOL);
            }
            ctx.importClass("dart.runtime.DartRuntime");
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
            if (sig[i] != null && sig[i].is("dynamic") && ai < args.size()) {
                out[i] = args.get(ai++);
            } else {
                out[i] = sig[i];
            }
        }
        return out;
    }

    private TypeRef[] typedefSig(String name) {
        Ast.TypedefDecl td = program.typedefs.get(name);
        if (td != null && td.returnType != null) {
            TypeRef[] sig = new TypeRef[td.paramTypes.size() + 1];
            for (int i = 0; i < td.paramTypes.size(); i++) {
                sig[i] = td.paramTypes.get(i);
            }
            sig[td.paramTypes.size()] = td.returnType;
            return sig;
        }
        return TYPEDEFS.get(name);
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
        TypeRef[] sig = t == null ? null : typedefSig(t.name);
        if (sig != null) {
            TypeRef r = sig[sig.length - 1];
            return r.is("void") ? TypeRef.VOID : r;
        }
        return TypeRef.DYNAMIC;
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
        return JAVA_KEYWORDS.contains(dartName) ? dartName + "_" : dartName;
    }

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

    private Out emitLambda(Lambda l, TypeRef expected, Ctx ctx) {
        lastLambdaVoid = false;
        // typedef-typed target position gives untyped lambda params real types
        TypeRef[] sigTypes = expected != null ? typedefSig(expected.name) : null;
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
        TypeRef outerReturn = ctx.methodReturnType;
        boolean outerNarrowInt = ctx.narrowReturnToInt;
        ctx.inAsyncBody = false;
        // Thread the lambda's SAM return type so `return`/switch-expression arms inside the body
        // resolve against it (e.g. an onGenerateRoute builder whose switch arms are Route values).
        TypeRef lambdaReturn = null;
        if (sigTypes != null && sigTypes.length > 0) {
            TypeRef r = sigTypes[sigTypes.length - 1];
            if (r != null && !r.is("void") && !r.is("dynamic")) {
                lambdaReturn = r;
            }
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
        if (l.body != null) {
            Ctx.Writer w = ctx.pushWriter(ctx.currentIndent() + 1);
            emitStatements(l.body, ctx);
            String body = ctx.popWriter();
            code = head + " -> {\n" + body + indentStr(ctx.currentIndent()) + "}";
        } else {
            Ctx.Writer w = ctx.pushWriter(ctx.currentIndent() + 1);
            // Emit the arrow body against the lambda's SAM return type so a switch-expression /
            // conditional body unifies its arms to that type (e.g. an onGenerateRoute arrow whose
            // switch arms are Route values).
            Out o = emitExpr(l.exprBody, lambdaReturn, ctx);
            if (lambdaReturn != null && o.type != null && o.type.is("num")
                    && (lambdaReturn.is("double") || lambdaReturn.is("int"))) {
                // num arithmetic is a Number; a lambda typed to return a double or int
                // (fold's combiner, a double-returning builder) needs that primitive.
                o = new Out(coerce(o, lambdaReturn, ctx), lambdaReturn);
            }
            String lifted = ctx.popWriter();
            lastLambdaVoid = o.type != null && o.type.is("void");
            if (lifted.isEmpty()) {
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
        ctx.methodReturnType = outerReturn;
        ctx.narrowReturnToInt = outerNarrowInt;
        return new Out(code, new TypeRef("Function"));
    }

    // ==================================================================
    // Calls
    // ==================================================================

    private Out emitCall(Call c, TypeRef expected, Ctx ctx) {
        // closure value invocation: f(...) where f is a local of Function type
        if (c.name == null && c.target != null) {
            Out target = emitExpr(c.target, null, ctx);
            return new Out(target.code + ".call(" + plainArgs(c.args, ctx) + ")", TypeRef.DYNAMIC);
        }
        if (c.target == null) {
            return emitBareCall(c, ctx);
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
                    || stubs.isStubClass(c.name) || stubs.functions.containsKey(c.name))) {
            Call bare = new Call();
            bare.file = c.file;
            bare.line = c.line;
            bare.col = c.col;
            bare.name = c.name;
            bare.args = c.args;
            return emitBareCall(bare, ctx);
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
            Out called = emitMethodCallOn(
                    new Out(tmp, copyNonNull(mat.type), mat.fromError), c, ctx);
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
        Out result = emitMethodCallOn(target, c, ctx);
        // a plain method call after a `?.` stays inside the short (a?.b.c())
        return result.withShort(target.shortGuard);
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

    private Out emitBareCall(Call c, Ctx ctx) {
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
            return new Out(n + ".call(" + plainArgs(c.args, ctx) + ")", ret);
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
            ClassDecl extCls = program.findExtension(ctx.extensionSelfType.name, n, false);
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
            return emitCtorCall(n, c.typeArgs, c.args, c, ctx);
        }
        // constructor of stub class
        if (stubs.isStubClass(n)) {
            return emitCtorCall(n, c.typeArgs, c.args, c, ctx);
        }
        // method of current class / inherited stub method
        ClassDecl cc = ctx.currentClass;
        if (cc != null) {
            MethodDecl m = cc.method(n);
            if (m != null) {
                String recv = m.isStatic ? cc.name : "this";
                return new Out(recv + "." + n + "("
                        + methodArgs(m.params, c.args, ctx) + ")",
                        m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType);
            }
            String stubSuper = nearestStubSuper(cc);
            if (stubSuper != null) {
                Ast.MethodDecl sm = stubs.findMethod(stubSuper, n, false);
                if (sm != null) {
                    return new Out("this." + n + "(" + stubMethodArgs(sm, c.args, ctx) + ")", sm.returnType);
                }
            }
            // members contributed by an applied stub mixin (e.g. RestorationMixin's
            // registerForRestoration): resolved as an inherited default method.
            for (TypeRef mixRef : cc.mixins) {
                if (stubs.isStubClass(mixRef.name)) {
                    Ast.MethodDecl sm = stubs.findMethod(mixRef.name, n, false);
                    if (sm != null) {
                        return new Out("this." + n + "(" + stubMethodArgs(sm, c.args, ctx) + ")", sm.returnType);
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
                return new Out(fieldRead.code + ".call(" + plainArgs(c.args, ctx) + ")",
                        funcResultType(ff.type));
            }
        }
        // top-level function (user code)
        FunctionDecl fn = program.functions.get(n);
        if (fn != null) {
            Library owner = program.resolveFunctionOwner(n, ctx.library(), null);
            // A private top-level function name can be declared in several libraries (Dart
            // privacy is library-scoped); use the RESOLVED owner's declaration so the parameter
            // list matches (e.g. crane's 2-arg `_customIconTheme` vs shrine's 1-arg one).
            FunctionDecl ownerFn = functionInLibrary(owner, n);
            if (ownerFn != null) {
                fn = ownerFn;
            }
            String cls = Program.libClassName(owner.fileName);
            String jn = n.equals("main") ? "main$" : n;
            return new Out(cls + "." + jn + "(" + methodArgs(fn.params, c.args, ctx) + ")",
                    fn.returnType == null || fn.returnType.is("var") ? TypeRef.DYNAMIC : fn.returnType);
        }
        // stub top-level function (e.g. runApp)
        Ast.FunctionDecl sf = stubs.functions.get(n);
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
            return new Out(simple + "." + method + "(" + methodArgs(sf.params, c.args, ctx) + ")",
                    sf.returnType);
        }
        diags.error(c, "E0135", "Cannot resolve function or constructor '" + n
                + "'. Confirm the file passes `dart analyze`, or the API may be unsupported in M1.");
        return new Out("null", TypeRef.DYNAMIC, true);
    }

    private Out emitMethodCallOn(Out target, Call c, Ctx ctx) {
        TypeRef tt = target.type;
        // A receiver typed with an import prefix (`intl.DateFormat`, `ui.Size`) keeps the
        // prefix in its type name; strip it so method resolution sees the real stub type.
        if (tt != null && tt.name != null && tt.name.indexOf('.') > 0) {
            tt.name = stripImportPrefix(tt.name);
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
        // static method on a class reference
        if (isClassRef(tt)) {
            String cls = tt.arg(0).name;
            // Dart's `Object.hash(a, b, ...)` -> java.util.Objects.hash(Object...).
            if (cls.equals("Object") && (n.equals("hash") || n.equals("hashAll"))) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.args.positional.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(boxIfPrimitive(emitExpr(c.args.positional.get(i), null, ctx), ctx));
                }
                return new Out("java.util.Objects.hash(" + sb + ")", TypeRef.INT);
            }
            // Dart's `Comparable.compare(a, b)` -> DartComparable.compare (delegates to compareTo).
            if (cls.equals("Comparable") && n.equals("compare")) {
                ctx.importClass("dart.core.DartComparable");
                String a = emitExpr(c.args.positional.get(0), null, ctx).code;
                String b = emitExpr(c.args.positional.get(1), null, ctx).code;
                return new Out("DartComparable.compare(" + a + ", " + b + ")", TypeRef.INT);
            }
            if (cls.equals("Future")) {
                ctx.importClass("dart.async.Future");
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
                    return new Out("Future.wait(" + l.code + ")",
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
                    return stubCallOut(m, c, stubSimpleName(cls, ctx) + "." + n, ctx);
                }
            }
            ClassDecl pc = program.classes.get(cls);
            if (pc != null) {
                MethodDecl m = pc.method(n);
                if (m != null && m.isStatic) {
                    return new Out(cls + "." + n + "(" + methodArgs(m.params, c.args, ctx) + ")",
                            m.returnType == null ? TypeRef.DYNAMIC : m.returnType);
                }
                CtorDecl named = pc.namedCtor(n);
                if (named != null) {
                    // named (or named factory) constructor -> static factory
                    return new Out(cls + "." + n + "(" + canonicalArgs(named, c.args, ctx) + ")",
                            new TypeRef(cls));
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
        Ast.EnumDecl ed = program.enums.get(tt.name);
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
                return new Out(target.code + "." + n + "(" + methodArgs(m.params, c.args, ctx) + ")",
                        m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType);
            }
            // method inherited from the program class's stub superclass or a stub mixin
            // (e.g. a RestorableProperty subclass calling the inherited `dispose()`).
            String stubSuper = nearestStubSuper(pc);
            if (stubSuper != null) {
                Ast.MethodDecl sm = stubs.findMethod(stubSuper, n, false);
                if (sm != null) {
                    return stubCallOut(sm, c, target.code + "." + n, ctx);
                }
            }
            for (TypeRef mixRef : pc.mixins) {
                if (stubs.isStubClass(mixRef.name)) {
                    Ast.MethodDecl sm = stubs.findMethod(mixRef.name, n, false);
                    if (sm != null) {
                        return stubCallOut(sm, c, target.code + "." + n, ctx);
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
                return new Out(fieldRead.code + ".call(" + plainArgs(c.args, ctx) + ")",
                        funcResultType(ff.type));
            }
        }
        // SAM invocation on a function-typed value: `f.call(args)` / `f?.call(args)`
        if (n.equals("call") && isFunctionValued(tt)) {
            TypeRef ret = funcResultType(tt);
            if (c.nullAware) {
                String tmp = ctx.newTemp();
                ctx.writer().line("var " + tmp + " = " + target.code + ";");
                // A void (or untyped-`Function`, whose void return was erased) callback is
                // fire-and-forget in statement position: guard with an `if` so a void SAM
                // isn't illegally used as a ternary value.
                if (ret.is("void") || ret.is("dynamic")) {
                    ctx.writer().line("if (" + tmp + " != null) { " + tmp + ".call("
                            + plainArgs(c.args, ctx) + "); }");
                    return new Out("", TypeRef.VOID);
                }
                return new Out("(" + tmp + " == null ? null : " + tmp + ".call("
                        + plainArgs(c.args, ctx) + "))", boxType(ret));
            }
            return new Out(target.code + ".call(" + plainArgs(c.args, ctx) + ")", ret);
        }
        // stub instance method
        String stubName = stubs.isStubClass(tt.name) ? tt.name : null;
        if (stubName != null || tt.is("State")) {
            Ast.MethodDecl m = stubs.findMethod(tt.name, n, false);
            if (m != null) {
                // Narrow a generic method result (declared `T evaluate(...)`) to the
                // concrete type argument the receiver instantiates.
                TypeRef sub = stubMemberReturnType(tt, n, false);
                return stubCallOut(m, c, target.code + "." + n, ctx, sub);
            }
        }
        // State<T> lifecycle overrides not present on the minimal State stub surface
        // (typically a `super.<lifecycle>()` chain); resolve as inherited void calls.
        if (tt.is("State") && STATE_LIFECYCLE.contains(n)) {
            return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")", TypeRef.VOID);
        }
        // extension methods
        ClassDecl extCls = program.findExtension(tt.name, n, false);
        if (extCls != null) {
            MethodDecl em = extCls.method(n);
            String rest = methodArgs(em.params, c.args, ctx);
            return new Out(extCls.name + "." + n + "(" + target.code
                    + (rest.isEmpty() ? "" : ", " + rest) + ")",
                    em.returnType == null || em.returnType.is("var") ? TypeRef.DYNAMIC : em.returnType);
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
        diags.error(c, "E0137", "Cannot resolve method '" + n + "' on type " + tt
                + ". Confirm the file passes `dart analyze`, or the API may be unsupported in M1.");
        return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")", TypeRef.DYNAMIC, true);
    }

    /** Core-type method table (String / List / Map / int / double). */
    private Out intrinsicCall(Out target, Call c, Ctx ctx) {
        TypeRef tt = target.type;
        String n = c.name;
        List<Expr> pos = c.args.positional;
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
                return new Out("DString.contains(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.BOOL);
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
            if (n.equals("startsWith") && pos.size() > 1) {
                // startsWith(pattern, index): the index was dropped, so
                // 'abc'.startsWith('b', 1) answered false.
                return new Out("DString.startsWith(" + target.code + ", "
                        + emitExpr(pos.get(0), null, ctx).code + ", "
                        + emitExpr(pos.get(1), TypeRef.INT, ctx).code + ")", TypeRef.BOOL);
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
                return new Out(target.code + ".addAllIterable(" + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("insert")) {
                return new Out(target.code + ".insert(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code
                        + ", " + emitExpr(pos.get(1), elem, ctx).code + ")", TypeRef.VOID);
            }
            if (n.equals("removeAt")) {
                return new Out(target.code + ".removeAt(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")", elem);
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
                String cmp = emitExpr(pos.get(0), null, ctx).code;
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
                return new Out(target.code + ".lastIndexWhere("
                        + emitExpr(pos.get(0), elementLambda(elem, TypeRef.BOOL), ctx).code + ")",
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
                String call = target.code + ".reduce(" + emitExpr(pos.get(0), null, ctx).code + ")";
                return new Out(unboxPrimitiveResult(call, elem), elem);
            }
            if (n.equals("expand")) {
                return new Out(target.code + ".expand(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.of("Iterable", TypeRef.DYNAMIC));
            }
            if (n.equals("followedBy")) {
                return new Out(target.code + ".followedBy(" + emitExpr(pos.get(0), null, ctx).code + ")",
                        TypeRef.of("Iterable", elem));
            }
            if (n.equals("take") || n.equals("skip")) {
                return new Out(target.code + "." + n + "(" + emitExpr(pos.get(0), TypeRef.INT, ctx).code + ")",
                        TypeRef.of("Iterable", elem));
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
                Out f = emitExpr(pos.get(1), null, ctx);
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
                Out cb = emitExpr(pos.get(0), null, ctx);
                // A lambda fits BOTH then overloads (Func1 / VoidFunc1) when it is void- or
                // throw-bodied, which javac reports as ambiguous; pin those to VoidFunc1.
                String cbCode = pinnedHandler(pos.get(0), cb, false, "dart.runtime.Funcs.Func1",
                        "dart.runtime.Funcs.VoidFunc1");
                // then(onValue, onError: handler): the handler was dropped, so a failure
                // skipped it and the chain stayed failed.
                for (NamedArg na : c.args.named) {
                    if (na.name.equals("onError")) {
                        String handler = pinnedHandler(na.value, emitExpr(na.value, null, ctx), true,
                                "dart.runtime.Funcs.Func1<Object, Object>", "dart.runtime.Funcs.VoidFunc1<Object>");
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
                        && (((Lambda) pos.get(0)).exprBody != null ? !lastLambdaVoid : lambdaReturnsValue(pos.get(0)));
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
                    value = ((Lambda) action).exprBody != null ? !lastLambdaVoid : lambdaReturnsValue(action);
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
        if (cc.ctorName.equals("from")) {
            String src = emitExpr(pos.get(0), null, ctx).code;
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
            return new Out("Future.error(" + emitExpr(pos.get(0), null, ctx).code + ")",
                    TypeRef.of("Future", TypeRef.DYNAMIC));
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
            if (ctor.equals("of") || ctor.equals("from")) {
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
                String src = emitExpr(pos.get(0), null, ctx).code;
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
            return null;
        }
        if (name.equals("Iterable")) {
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
        return className.equals("Selector") || className.equals("Consumer");
    }

    /**
     * Emits {@code tmp.providedType(A.class)} for those widgets.
     *
     * <p>Only for a plain class: a generic or dynamic argument has no class literal, and the
     * runtime's Object default (nearest provider) remains — no worse than before.</p>
     */
    private void emitProvidedTypeToken(String tmp, String className, List<TypeRef> typeArgs,
            Ctx ctx) {
        if (!readsProvidedValueByType(className) || typeArgs.isEmpty()) {
            return;
        }
        TypeRef a = typeArgs.get(0);
        if (a == null) {
            return;
        }
        String javaName = javaType(a, false, ctx);
        if (javaName == null || javaName.indexOf('<') >= 0 || javaName.equals("Object")) {
            return;
        }
        ctx.writer().line(tmp + ".providedType(" + javaName + ".class);");
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
        ClassDecl pc = program.resolveClass(className, ctx.library());
        // An app class may share a name with a stub type (new_gallery's routes.dart `Path`
        // vs dart:ui `Path`). Prefer the app class only when it is actually visible to the
        // current library (declared or imported); otherwise fall through to the stub.
        if (pc != null && stubs.isStubClass(className) && !classVisibleFrom(pc, ctx.library())) {
            pc = null;
        }
        if (pc != null) {
            String jn = javaClassName(pc);
            CtorDecl ct = pc.defaultCtor();
            if (ct != null && ct.isFactory) {
                return new Out(jn + ".$create(" + canonicalArgs(ct, pc, args, ctx) + ")",
                        new TypeRef(className));
            }
            // A generic program class constructed raw (`new Foo(...)`) erases the generics on ALL
            // its members — a constructor param typed Func1<BuildContext, Widget> becomes raw
            // Func1, so a lambda argument gets Object params. A diamond keeps the signatures.
            String diamond = pc.typeParams.isEmpty() ? "" : "<>";
            return new Out("new " + jn + diamond + "(" + canonicalArgs(ct, pc, args, ctx) + ")",
                    new TypeRef(className));
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
            return new Out("new " + simple + diamond + "(" + posArgs + ")", stubResultType);
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
            c = c.superclass != null ? program.classes.get(c.superclass.name) : null;
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
            TypeRef pt = paramType(owner, p, ctx);
            if (!p.named) {
                if (posIdx < args.positional.size()) {
                    Out o = emitExpr(args.positional.get(posIdx++), pt, ctx);
                    sb.append(coerce(o, pt, ctx));
                } else if (p.defaultValue != null) {
                    sb.append(coerce(emitExpr(p.defaultValue, pt, ctx), pt, ctx));
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
                if (match != null) {
                    Out o = emitExpr(match.value, pt, ctx);
                    sb.append(coerce(o, pt, ctx));
                } else if (p.defaultValue != null) {
                    sb.append(coerce(emitExpr(p.defaultValue, pt, ctx), pt, ctx));
                } else {
                    sb.append(zeroValue(pt));
                }
            }
        }
        reportUnknownNamedArgs(ct, owner, args);
        return sb.toString();
    }

    /** Set while emitting a super(...) initializer, where no statement may come first. */
    private boolean inSuperInitializer;

    /**
     * Whether evaluating {@code e} can have an effect another argument could observe:
     * an assignment, an increment, an await, a cascade, or a call to a function or to
     * a method of this object -- {@code log('b')}, {@code _next()}. Calls on a value or
     * a class ({@code color.withOpacity(.5)}, {@code EdgeInsets.all(8)},
     * {@code Theme.of(context)}) and constructor calls are taken to have none. That is
     * the line Flutter code sits on: every widget constructor has named arguments in
     * whatever order the author wrote them, and treating all of them as effects would
     * sequence nearly every call in a build method for no observable difference.
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
            return hasEffect(((PropertyGet) e).target);
        }
        if (e instanceof IndexGet) {
            return hasEffect(((IndexGet) e).target) || hasEffect(((IndexGet) e).index);
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
            return argsHaveEffect(((CtorCall) e).args);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            boolean ownOrFunction = c.target == null || c.target instanceof ThisExpr;
            boolean onAClass = c.target == null && c.name != null && !c.name.isEmpty()
                    && Character.isUpperCase(c.name.charAt(0));
            if (ownOrFunction && !onAClass) {
                return true;
            }
            return hasEffect(c.target) || argsHaveEffect(c.args);
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
     * effect, or a bare variable read, which an effect can change --
     * {@code f(b: x, a: log())} must read x before log() runs. Constants, reads through
     * a class ({@code ReplyColors.white50}) and constructor calls are left in place.
     */
    private static boolean orderSensitive(Expr e) {
        return e instanceof Ident || hasEffect(e);
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
                TypeRef pt = paramType(owner, p, ctx);
                Expr supplied = null;
                for (Map.Entry<Expr, Integer> en : slotOf.entrySet()) {
                    if (en.getValue().intValue() == i) {
                        supplied = en.getKey();
                    }
                }
                if (supplied != null) {
                    code = coerce(emitExpr(supplied, pt, ctx), pt, ctx);
                } else if (p.defaultValue != null) {
                    code = coerce(emitExpr(p.defaultValue, pt, ctx), pt, ctx);
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
    private String stubMethodArgs(Ast.MethodDecl m, Args args, Ctx ctx) {
        CtorDecl fake = new CtorDecl();
        fake.params = m.params;
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
            } else if (INFERRED_WITNESS_METHODS.contains(m.name)
                    && ctx.methodReturnType != null && isConcreteType(ctx.methodReturnType)
                    && !ctx.methodReturnType.is("void") && !ctx.methodReturnType.is("dynamic")) {
                // A BuildContext ancestor lookup (`X.of(context) =>
                // context.dependOnInheritedWidgetOfExactType()`) whose <T> was inferred
                // from and dropped by the enclosing return type. Recover it from there.
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
        return new Out(callee + "(" + args + ")", substReturn != null ? substReturn : rt);
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
     * which javac reports as ambiguous. {@code forceCast} casts a value-returning lambda
     * too (an Object parameter); otherwise only a void one is cast and javac resolves the
     * rest as it always has. Anything that is not a lambda passes as is.
     */
    private String pinnedHandler(Expr src, Out o, boolean forceCast, String asValue, String asVoid) {
        if (!(src instanceof Lambda)) {
            return o.code;
        }
        boolean value = ((Lambda) src).exprBody != null ? !lastLambdaVoid : lambdaReturnsValue(src);
        if (value && !forceCast) {
            return o.code;
        }
        return "(" + (value ? asValue : asVoid) + ") " + paren(o.code);
    }

    /** An inline single-parameter function type {@code (param) -> ret}, for typing a lambda arg. */
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
        }
        return TypeRef.DYNAMIC;
    }

    private TypeRef typeOfField(ClassDecl c, String name, Ctx ctx) {
        FieldDecl f = c.field(name);
        return f == null ? TypeRef.DYNAMIC : fieldType(f, ctx);
    }

    /** Type of a constructor parameter, resolving this./super. against fields. */
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
            ClassDecl progCur = program.classes.get(c.superclass.name);
            while (progCur != null) {
                Ast.CtorDecl sct = progCur.defaultCtor();
                if (sct != null) {
                    for (Ast.Param sp : sct.params) {
                        if (sp.name.equals(p.name)) {
                            TypeRef rt = paramType(progCur, sp, ctx);
                            if (rt != null && !rt.is("dynamic")) {
                                return rt;
                            }
                        }
                    }
                }
                // the super param may correspond directly to an inherited field
                FieldDecl sf = progCur.field(p.name);
                if (sf != null) {
                    return fieldType(sf, ctx);
                }
                progCur = progCur.superclass != null ? program.classes.get(progCur.superclass.name) : null;
            }
            // look up the named param type on the stub super chain
            Ast.ClassDecl cur = stubs.classes.get(c.superclass.name);
            while (cur != null) {
                Ast.CtorDecl sct = cur.defaultCtor();
                if (sct != null) {
                    for (Ast.Param sp : sct.params) {
                        if (sp.name.equals(p.name)) {
                            return sp.type;
                        }
                    }
                }
                cur = cur.superclass != null ? stubs.classes.get(cur.superclass.name) : null;
            }
        }
        return TypeRef.DYNAMIC;
    }

    /** Maps a Dart type to Java source. boxed=true forces reference types. */
    private String javaType(TypeRef t, boolean boxed, Ctx ctx) {
        if (t == null || t.is("var") || t.is("dynamic") || t.is("Object") || t.is("Null")) {
            return "Object";
        }
        // A type written with an import prefix (`intl.DateFormat`, `ui.Size`) carries the
        // prefix in its name; drop it so the base name resolves against a stub/program type.
        if (t.name != null && t.name.indexOf('.') > 0) {
            String stripped = stripImportPrefix(t.name);
            if (!stripped.equals(t.name)) {
                t.name = stripped;
            }
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
        if (t.is("Future") || t.is("FutureOr")) {
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
        Ast.TypedefDecl userTd = program.typedefs.get(t.name);
        if (userTd != null && userTd.aliased != null) {
            return javaType(userTd.aliased, boxed, ctx);
        }
        if (typedefSig(t.name) != null) {
            ctx.importClass("dart.runtime.Funcs");
            // Substitute the typedef's type arguments (e.g. ValueChanged<int> => the `dynamic`
            // placeholder in `void Function(dynamic)` becomes `int`), so a callback of a
            // parameterized typedef renders VoidFunc1<Long> rather than VoidFunc1<Object>.
            TypeRef[] sig = substituteTypedefTypeArgs(typedefSig(t.name), t.args);
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
        Ast.EnumDecl se = stubs.enums.get(t.name);
        if (se != null && se.javaName != null) {
            ctx.importClass(se.javaName);
            return se.javaName.substring(se.javaName.lastIndexOf('.') + 1);
        }
        // program class / enum / type parameter — same package
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
        boolean collides = stubs.isStubClass(c.name) || stubs.isStubEnum(c.name);
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
        Ast.EnumDecl se = stubs.enums.get(dartName);
        if (se != null && se.javaName != null) {
            ctx.importClass(se.javaName);
        }
    }

    private String simpleEnumName(String dartName, Ctx ctx) {
        Ast.EnumDecl se = stubs.enums.get(dartName);
        if (se != null && se.javaName != null) {
            return se.javaName.substring(se.javaName.lastIndexOf('.') + 1);
        }
        return dartName;
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
                return name.substring(dot + 1);
            }
        }
        return name;
    }

    /** Pseudo-type marking a reference to a class itself (for static access). */
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
    private ClassDecl programClass(String name, Ctx ctx) {
        Ast.Library lib = ctx != null && ctx.currentClass != null
                ? ctx.currentClass.ownerLibrary : null;
        return program.resolveClass(name, lib);
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
        return c;
    }

    private TypeRef copyNonNull(TypeRef t) {
        if (t == null) {
            return TypeRef.DYNAMIC;
        }
        TypeRef c = TypeRef.of(t.name, t.args.toArray(new TypeRef[0]));
        c.nullable = false;
        return c;
    }

    /** Whether a Dart type erases to Java Object (untyped/dynamic value). */
    private boolean isDynamicType(TypeRef t) {
        return t == null || t.is("var") || t.is("dynamic") || t.is("Object");
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
            return o.code + ".class";
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
        return o.code;
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

    /** Turns an expression emission into a valid Java statement expression. */
    private String statementize(String code) {
        if (code.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            // a bare temp/identifier (e.g. a cascade's receiver) — no-op statement
            return "";
        }
        if (code.startsWith("(") && code.endsWith(")")) {
            // ternaries etc. are not valid statements; assign to a discard temp
            return "var $unused" + (unusedCounter++) + " = " + code;
        }
        return code;
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
            c = c.superclass != null ? program.classes.get(c.superclass.name) : null;
        }
        return null;
    }

    /** Finds a method walking the program-class superclass chain. */
    private MethodDecl findMethodInHierarchy(ClassDecl c, String name) {
        while (c != null) {
            MethodDecl m = c.method(name);
            if (m != null) {
                return m;
            }
            c = c.superclass != null ? program.classes.get(c.superclass.name) : null;
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
    private TypeRef stubMemberReturnType(TypeRef receiver, String member, boolean getter) {
        if (receiver == null) {
            return null;
        }
        Ast.ClassDecl c = stubs.classes.get(receiver.name);
        java.util.Map<String, TypeRef> subst =
                paramMap(c == null ? null : c.typeParams, receiver.args);
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
        java.util.Map<String, TypeRef> subst = new java.util.HashMap<String, TypeRef>();
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
        final ClassDecl currentClass;
        /** The library whose top-level (lib-class) body is being emitted; set only when currentClass is null. */
        Library currentLibrary;
        final Map<String, String> imports = new TreeMap<String, String>();
        final List<Map<String, TypeRef>> scopes = new ArrayList<Map<String, TypeRef>>();
        final List<Writer> writers = new ArrayList<Writer>();
        final java.util.Set<String> boxedLocals = new HashSet<String>();
        private final java.util.Set<String> boxedActive = new HashSet<String>();
        TypeRef methodReturnType;
        TypeRef extensionSelfType;
        boolean inAsyncBody;
        /** Inside a hashCode/compareTo body: narrow each `return` value to Java int. */
        boolean narrowReturnToInt;
        String syncStarList;                  // non-null inside a sync* body: the result-list temp
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

        void pushScope() {
            scopes.add(new LinkedHashMap<String, TypeRef>());
            renameScopes.add(new LinkedHashMap<String, String>());
        }

        void popScope() {
            scopes.remove(scopes.size() - 1);
            renameScopes.remove(renameScopes.size() - 1);
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
        }
    }
}
