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

    public JavaEmitter(Program program, StubRegistry stubs, Diagnostics diags, String pkg) {
        this.program = program;
        this.stubs = stubs;
        this.diags = diags;
        this.pkg = pkg;
    }

    // ==================================================================
    // Top level
    // ==================================================================

    public List<GeneratedFile> emit() {
        List<GeneratedFile> out = new ArrayList<GeneratedFile>();
        String mainLib = null;
        for (Library lib : program.libraries) {
            for (ClassDecl c : lib.classes) {
                if (c.extensionOn != null) {
                    out.add(emitExtension(c));
                } else if (c.isMixin) {
                    out.add(emitMixin(c));
                } else {
                    out.add(emitClass(c));
                }
            }
            for (EnumDecl e : lib.enums) {
                out.add(emitEnum(e));
            }
            if (!lib.functions.isEmpty() || !lib.topLevelVars.isEmpty()) {
                out.add(emitLibClass(lib));
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
        return out;
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
        sb.append(dartRef(e)).append("\n");
        sb.append("public enum ").append(e.name).append(" {\n    ");
        for (int i = 0; i < e.entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(e.entries.get(i));
        }
        sb.append("\n}\n");
        return new GeneratedFile(e.name + ".java", sb.toString());
    }

    private GeneratedFile emitLibClass(Library lib) {
        Ctx ctx = new Ctx(null);
        String cls = Program.libClassName(lib.fileName);
        StringBuilder body = new StringBuilder();
        for (FieldDecl v : lib.topLevelVars) {
            TypeRef vt = fieldType(v, ctx);
            String jt = javaType(vt, false, ctx);
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
            m.name = f.name.equals("main") ? "main$" : f.name;
            m.returnType = f.returnType;
            m.params = f.params;
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
                sig.append(", ").append(javaType(pt, false, ctx)).append(' ').append(pm.name);
                ctx.declare(pm.name, pt);
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
                body.append(javaType(pt, false, ctx)).append(' ').append(pm.name);
                ctx.declare(pm.name, pt);
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

        // fields
        for (FieldDecl f : c.fields) {
            TypeRef ft = fieldType(f, ctx);
            String jt = javaType(ft, false, ctx);
            body.append("    private ");
            if (f.isStatic) {
                body.append("static ");
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
            // public accessors for non-library-private instance fields
            if (!f.name.startsWith("_") && !f.isStatic) {
                body.append("    public ").append(jt).append(" get$").append(f.name).append("() {\n")
                        .append("        return ").append(f.name).append(";\n    }\n");
                if (!f.isFinal && !f.isConst) {
                    body.append("    public void set$").append(f.name).append("(").append(jt).append(" v) {\n")
                            .append("        this.").append(f.name).append(" = v;\n    }\n");
                }
            }
            body.append('\n');
        }

        // constructors
        if (c.hasNamedNonFactoryCtor()) {
            body.append("    /** Marker distinguishing named-constructor instantiation. */\n");
            body.append("    private static final class $NamedCtor {\n        private $NamedCtor() {\n        }\n    }\n\n");
            body.append("    private ").append(c.name).append("($NamedCtor $marker) {\n    }\n\n");
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
            mm.isOverride = m.isOverride;
            mm.isAbstract = m.isAbstract;
            mm.isAsync = m.isAsync;
            mm.returnType = m.returnType;
            mm.params = m.params;
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
        String decl = "public " + (c.isAbstract ? "abstract " : "") + "class " + c.name;
        String ext = null;
        if (c.superclass != null) {
            ext = javaType(c.superclass, false, ctx);
        }
        return finishClassFile(c.name, decl, ext, impls.length() == 0 ? null : impls.toString(), body, ctx, c.file);
    }

    private GeneratedFile finishClassFile(String name, String decl, String ext, String impls,
                                          CharSequence body, Ctx ctx, String dartFile) {
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
        sb.append("    public ").append(c.name).append('(');
        ctx.pushScope();
        List<Param> params = ct.params;
        for (int i = 0; i < params.size(); i++) {
            Param p = params.get(i);
            TypeRef pt = paramType(c, p, ctx);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(p.name);
            ctx.declare(p.name, pt);
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
            w.line("super(" + canonicalArgs(superCtor, superArgs, ctx) + ");");
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
                    w.line("this." + p.name + "(" + p.name + ");");
                }
            }
        }
        // this.x params
        for (Param p : params) {
            if (p.isThis) {
                w.line("this." + p.name + " = " + p.name + ";");
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
        String name = ct.name == null ? "$create" : ct.name;
        ctx.pushScope();
        sb.append("    public static ").append(c.name).append(' ').append(name).append('(');
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
            paramSig.append(javaType(pt, false, ctx)).append(' ').append(p.name);
            argList.append(p.name);
            ctx.declare(p.name, pt);
        }
        sb.append("    public static ").append(c.name).append(' ').append(ct.name)
                .append('(').append(paramSig).append(") {\n");
        sb.append("        ").append(c.name).append(" $self = new ").append(c.name).append("(($NamedCtor) null);\n");
        sb.append("        $self.$init$").append(ct.name).append('(').append(argList).append(");\n");
        sb.append("        return $self;\n    }\n\n");
        sb.append("    private void $init$").append(ct.name).append('(').append(paramSig).append(") {\n");
        ctx.pushWriter(2);
        Ctx.Writer w = ctx.writer();
        for (Param p : ct.params) {
            if (p.isThis) {
                w.line("this." + p.name + " = " + p.name + ";");
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
            sb.append(javaType(pt, false, ctx)).append(' ').append(p.name);
            ctx.declare(p.name, pt);
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
        TypeRef returnType;
        List<Param> params = new ArrayList<Param>();
        Block body;
        Expr exprBody;
    }

    private String emitMethodLike(Method m, Ctx ctx, boolean classIsAbstract) {
        StringBuilder sb = new StringBuilder();
        TypeRef rt = m.returnType == null || m.returnType.is("var") ? TypeRef.DYNAMIC : m.returnType;
        if (m.isOverride) {
            sb.append("    @Override\n");
        }
        sb.append("    ").append(m.name.startsWith("_") ? "private " : "public ");
        if (m.isStatic) {
            sb.append("static ");
        }
        if (m.isAbstract) {
            sb.append("abstract ");
        }
        ctx.pushScope();
        String rjt = m.isSetter ? "void" : javaType(rt, false, ctx);
        sb.append(rjt).append(' ').append(m.name).append('(');
        for (int i = 0; i < m.params.size(); i++) {
            Param p = m.params.get(i);
            TypeRef pt = p.type == null || p.type.is("var") ? TypeRef.DYNAMIC : p.type;
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(javaType(pt, false, ctx)).append(' ').append(p.name);
            ctx.declare(p.name, pt);
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
        boolean asyncFuture = m.isAsync && (rt.is("Future") || rt.is("FutureOr"));
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
                } else {
                    ctx.writer().line("return " + coerce(o, rt, ctx) + ";");
                }
            }
        }
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
            Out o = emitExpr(ex, null, ctx);
            String code = statementize(o.code);
            if (!code.isEmpty()) {
                w.line(code + ";");
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
                String var = cc.exceptionVar != null ? cc.exceptionVar : "$e";
                w.line("} catch (" + exType + " " + var + ") {");
                ctx.indent(1);
                ctx.pushScope();
                ctx.declare(var, cc.onType != null ? cc.onType : TypeRef.DYNAMIC);
                if (cc.stackVar != null) {
                    // stack traces are not modeled; bind the name for compilation
                    w.line("Object " + cc.stackVar + " = null;");
                    ctx.declare(cc.stackVar, TypeRef.DYNAMIC);
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
        } else if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            Out c = emitExpr(i.condition, TypeRef.BOOL, ctx);
            w.line("if (" + c.code + ") {");
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
        } else if (s instanceof WhileStmt) {
            WhileStmt wh = (WhileStmt) s;
            Out c = emitExpr(wh.condition, TypeRef.BOOL, ctx);
            w.line("while (" + c.code + ") {");
            ctx.indent(1);
            ctx.pushScope();
            emitStatement(unwrapBlock(wh.body), ctx);
            ctx.popScope();
            ctx.indent(-1);
            w.line("}");
        } else if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            ctx.pushScope();
            // lift the init before the loop; conditions/updates must be lift-free in M1
            String initCode = "";
            if (f.init instanceof VarDeclStmt) {
                VarDeclStmt v = (VarDeclStmt) f.init;
                Out init = v.initializer != null ? emitExpr(v.initializer, v.type, ctx) : null;
                TypeRef t = v.type == null || v.type.is("var")
                        ? (init != null ? init.type : TypeRef.DYNAMIC) : v.type;
                String loopVar = ctx.declareShadowSafe(v.name, t);
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
            emitStatement(unwrapBlock(f.body), ctx);
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        } else if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            Out iter = emitExpr(f.iterable, null, ctx);
            TypeRef elem = f.varType != null && !f.varType.is("var")
                    ? f.varType
                    : (iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                        ? iter.type.arg(0) : TypeRef.DYNAMIC);
            ctx.pushScope();
            String loopVar = ctx.declareShadowSafe(f.varName, elem);
            w.line("for (" + javaType(elem, true, ctx) + " " + loopVar + " : " + iter.code + ") {");
            ctx.indent(1);
            emitStatement(unwrapBlock(f.body), ctx);
            ctx.indent(-1);
            w.line("}");
            ctx.popScope();
        } else if (s instanceof BreakStmt) {
            w.line("break;");
        } else if (s instanceof ContinueStmt) {
            w.line("continue;");
        } else if (s != null) {
            diags.error(s, "E0127", "Unsupported statement in emitter");
        }
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

        Out(String code, TypeRef type) {
            this.code = code;
            this.type = type == null ? TypeRef.DYNAMIC : type;
        }
    }

    private Out emitExpr(Expr e, TypeRef expected, Ctx ctx) {
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
            ctx.importClass("dart.async.Await");
            TypeRef inner = o.type.is("Future") ? o.type.arg(0) : TypeRef.DYNAMIC;
            return new Out("Await.await$(" + o.code + ")", boxType(inner));
        }
        if (e instanceof ParenExpr) {
            Out inner = emitExpr(((ParenExpr) e).inner, expected, ctx);
            return new Out("(" + inner.code + ")", inner.type);
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
                ClassDecl pc = program.classes.get(cc.type.name);
                if (pc != null && pc.namedCtor(cc.ctorName) != null) {
                    return new Out(cc.type.name + "." + cc.ctorName + "("
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
            return emitCtorCall(cc.type.name, cc.args, e, ctx);
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
            String op = id.increment ? "++" : "--";
            return new Out(id.prefix ? op + target.code : target.code + op, target.type);
        }
        if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            Out cond = emitExpr(c.condition, TypeRef.BOOL, ctx);
            Out a = emitExpr(c.thenExpr, expected, ctx);
            Out b = emitExpr(c.elseExpr, expected, ctx);
            TypeRef t = a.type.name.equals(b.type.name) ? a.type
                    : (expected != null ? expected : TypeRef.DYNAMIC);
            return new Out("(" + cond.code + " ? " + a.code + " : " + b.code + ")", t);
        }
        if (e instanceof NotNullAssert) {
            Out o = emitExpr(((NotNullAssert) e).operand, null, ctx);
            ctx.importClass("dart.runtime.DartRuntime");
            TypeRef t = copyNonNull(o.type);
            return new Out("DartRuntime.nn(" + o.code + ")", t);
        }
        if (e instanceof IsTest) {
            IsTest t = (IsTest) e;
            Out o = emitExpr(t.operand, null, ctx);
            String check = o.code + " instanceof " + javaType(t.type, true, ctx);
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
            ctx.writer().line("DartList<" + javaType(elem, true, ctx) + "> " + tmp + " = new DartList<>();");
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
            codes.add(coerce(o, elem, ctx));
            if (inferred == null) {
                inferred = o.type;
            }
        }
        if (elem == null) {
            elem = inferred != null ? inferred : TypeRef.DYNAMIC;
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
            ForElement f = (ForElement) e;
            ctx.pushScope();
            if (f.varName != null) {
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

    private Out emitMapLit(MapLit m, TypeRef expected, Ctx ctx) {
        ctx.importClass("dart.core.DartMap");
        TypeRef k = m.keyType;
        TypeRef v = m.valueType;
        if (k == null && expected != null && expected.is("Map") && expected.args.size() == 2) {
            k = expected.arg(0);
            v = expected.arg(1);
        }
        StringBuilder sb = new StringBuilder("DartMap.of(");
        for (int i = 0; i < m.keys.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Out ko = emitExpr(m.keys.get(i), k, ctx);
            Out vo = emitExpr(m.values.get(i), v, ctx);
            if (k == null) {
                k = ko.type;
            }
            if (v == null) {
                v = vo.type;
            }
            sb.append(boxIfPrimitive(ko, ctx)).append(", ").append(boxIfPrimitive(vo, ctx));
        }
        sb.append(')');
        return new Out(sb.toString(), TypeRef.of("Map",
                k == null ? TypeRef.DYNAMIC : k, v == null ? TypeRef.DYNAMIC : v));
    }

    private Out emitIdent(Ident id, TypeRef expected, Ctx ctx) {
        String n = id.name;
        TypeRef local = ctx.lookup(n);
        if (local != null) {
            String jn = ctx.javaNameOf(n);
            return new Out(ctx.isBoxed(n) ? jn + ".v" : jn, local);
        }
        ClassDecl cc = ctx.currentClass;
        if (cc != null) {
            FieldDecl f = cc.field(n);
            if (f != null) {
                if (cc.isMixin && !f.isStatic) {
                    // interface default methods reach mixin state via accessors
                    return new Out("this.get$" + n + "()", fieldType(f, ctx));
                }
                return new Out(f.isStatic ? cc.name + "." + n : "this." + n, fieldType(f, ctx));
            }
            MethodDecl getter = cc.getter(n);
            if (getter != null) {
                return new Out("this." + n + "()", getter.returnType);
            }
            // tear-off of an own method when a function-ish value is expected
            MethodDecl md = cc.method(n);
            if (md != null) {
                return new Out("this::" + n, new TypeRef("Function"));
            }
            // 'widget' inside a State<T> subclass
            if (n.equals("widget") && stateTypeArg(cc) != null) {
                return new Out("this.widget()", stateTypeArg(cc));
            }
            if (n.equals("context") && isStateSubclass(cc)) {
                return new Out("this.context()", new TypeRef("BuildContext"));
            }
            // inherited stub getters
            String stubSuper = nearestStubSuper(cc);
            if (stubSuper != null) {
                Ast.MethodDecl sg = stubs.findMethod(stubSuper, n, true);
                if (sg != null) {
                    return new Out("this." + n + "()", sg.returnType);
                }
            }
        }
        if (program.classes.containsKey(n) || program.enums.containsKey(n)
                || stubs.isStubClass(n) || stubs.isStubEnum(n)
                || n.equals("Future") || n.equals("Duration")) {
            return new Out(n, classRef(n));
        }
        if (program.topLevelVars.containsKey(n)) {
            Library owner = program.topLevelVarOwners.get(n);
            return new Out(Program.libClassName(owner.fileName) + "." + n,
                    fieldType(program.topLevelVars.get(n), ctx));
        }
        if (program.functions.containsKey(n)) {
            Library owner = program.functionOwners.get(n);
            return new Out(Program.libClassName(owner.fileName) + "::" + n, new TypeRef("Function"));
        }
        diags.error(id, "E0129", "Cannot resolve identifier '" + n
                + "'. Confirm the file passes `dart analyze`.");
        return new Out(n, TypeRef.DYNAMIC);
    }

    private Out emitPropertyGet(PropertyGet pg, Ctx ctx) {
        Out target = emitExpr(pg.target, null, ctx);
        TypeRef tt = target.type;
        if (pg.nullAware) {
            // a?.b -> lift: T $t = a; ($t == null ? null : $t.b)
            String tmp = ctx.newTemp();
            ctx.writer().line("var " + tmp + " = " + target.code + ";");
            Out member = emitMemberGet(new Out(tmp, copyNonNull(tt)), pg.name, pg, ctx);
            TypeRef mt = boxType(member.type);
            return new Out("(" + tmp + " == null ? null : " + member.code + ")", mt);
        }
        return emitMemberGet(target, pg.name, pg, ctx);
    }

    /** Property access driven by the target's static type. */
    private Out emitMemberGet(Out target, String name, Node posNode, Ctx ctx) {
        TypeRef tt = target.type;
        // static access through a class reference
        if (isClassRef(tt)) {
            String cls = tt.arg(0).name;
            if (program.enums.containsKey(cls) || stubs.isStubEnum(cls)) {
                importEnum(cls, ctx);
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
                    return new Out(cls + "." + name, fieldType(f, ctx));
                }
            }
            diags.error(posNode, "E0131", "Cannot resolve static member '" + name + "' on " + cls);
            return new Out("null", TypeRef.DYNAMIC);
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
        // program class member
        ClassDecl pc = program.classes.get(tt.name);
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
        }
        // stub class member (walk supers)
        if (stubs.isStubClass(tt.name) || tt.is("State")) {
            Ast.MethodDecl g = stubs.findMethod(tt.name, name, true);
            if (g != null) {
                TypeRef rt = g.returnType;
                // State<T>.widget returns the type argument
                if (tt.is("State") && name.equals("widget") && !tt.args.isEmpty()) {
                    rt = tt.arg(0);
                }
                return new Out(target.code + "." + name + "()", rt);
            }
        }
        ClassDecl extCls = program.findExtension(tt.name, name, true);
        if (extCls != null) {
            MethodDecl eg = extCls.getter(name);
            return new Out(extCls.name + "." + name + "(" + target.code + ")",
                    eg.returnType == null || eg.returnType.is("var") ? TypeRef.DYNAMIC : eg.returnType);
        }
        diags.error(posNode, "E0132", "Cannot resolve member '" + name + "' on type " + tt
                + ". Confirm the file passes `dart analyze`.");
        return new Out(target.code + "." + name, TypeRef.DYNAMIC);
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
            return new Out(target.code + ".idx(" + boxIfPrimitive(idx, ctx) + ")", boxType(tt.arg(1)));
        }
        return new Out(target.code + ".idx(" + idx.code + ")", tt.arg(0));
    }

    private Out emitAssign(Assign a, Ctx ctx) {
        // ??= special
        if (a.op.equals("??=")) {
            Out lhs = emitExpr(a.lhs, null, ctx);
            Out rhs = emitExpr(a.rhs, lhs.type, ctx);
            return new Out("(" + lhs.code + " == null ? (" + lhs.code + " = " + rhs.code + ") : " + lhs.code + ")",
                    lhs.type);
        }
        if (a.lhs instanceof IndexGet) {
            IndexGet ig = (IndexGet) a.lhs;
            Out target = emitExpr(ig.target, null, ctx);
            Out idx = emitExpr(ig.index, null, ctx);
            if (!a.op.equals("=")) {
                diags.error(a, "E0133", "Compound assignment to an index is not supported yet");
            }
            ClassDecl opClass = program.classes.get(target.type.name);
            if (opClass != null && findMethodInHierarchy(opClass, "$indexSet") != null) {
                Out rhs = emitExpr(a.rhs, null, ctx);
                return new Out(target.code + ".$indexSet(" + idx.code + ", " + rhs.code + ")", rhs.type);
            }
            TypeRef vt = target.type.is("Map") ? target.type.arg(1) : target.type.arg(0);
            Out rhs = emitExpr(a.rhs, vt, ctx);
            String key = target.type.is("Map") ? boxIfPrimitive(idx, ctx) : idx.code;
            return new Out(target.code + ".idxSet(" + key + ", " + coerce(rhs, vt, ctx) + ")", vt);
        }
        Out lhs = emitExpr(a.lhs, null, ctx);
        String lcode = lhs.code;
        // setters through accessors: x.get$f() as assignment target -> x.set$f(v)
        if (lcode.endsWith("()") && lcode.contains(".get$")) {
            if (!a.op.equals("=")) {
                diags.error(a, "E0134", "Compound assignment through accessors is not supported yet");
            }
            Out rhs = emitExpr(a.rhs, lhs.type, ctx);
            String base = lcode.substring(0, lcode.lastIndexOf(".get$"));
            String prop = lcode.substring(lcode.lastIndexOf(".get$") + 5, lcode.length() - 2);
            return new Out(base + ".set$" + prop + "(" + coerce(rhs, lhs.type, ctx) + ")", lhs.type);
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

    private Out emitBinary(Binary b, Ctx ctx) {
        if (b.op.equals("??")) {
            Out left = emitExpr(b.left, null, ctx);
            String tmp = ctx.newTemp();
            ctx.writer().line("var " + tmp + " = " + left.code + ";");
            Out right = emitExpr(b.right, left.type, ctx);
            return new Out("(" + tmp + " != null ? " + tmp + " : " + right.code + ")",
                    copyNonNull(left.type));
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
        if (b.op.equals("==") || b.op.equals("!=")) {
            if (numeric || (l.type.is("bool") && r.type.is("bool"))) {
                return new Out(paren(l.code) + " " + b.op + " " + paren(r.code), TypeRef.BOOL);
            }
            ctx.importClass("dart.runtime.DartRuntime");
            String eq = "DartRuntime.eq(" + l.code + ", " + boxIfPrimitive(r, ctx) + ")";
            return new Out(b.op.equals("==") ? eq : "!" + eq, TypeRef.BOOL);
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
        if (b.op.equals("+") && (l.type.is("String") || r.type.is("String"))) {
            return new Out(paren(l.code) + " + " + paren(r.code), TypeRef.STRING);
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

    /** Known function typedefs: name -> [param types..., return type]. */
    private static final Map<String, TypeRef[]> TYPEDEFS = new LinkedHashMap<String, TypeRef[]>();

    static {
        TYPEDEFS.put("VoidCallback", new TypeRef[] {TypeRef.VOID});
        TYPEDEFS.put("WidgetBuilder", new TypeRef[] {new TypeRef("BuildContext"), new TypeRef("Widget")});
        TYPEDEFS.put("IndexedWidgetBuilder", new TypeRef[] {new TypeRef("BuildContext"), TypeRef.INT, new TypeRef("Widget")});
        // value-change callbacks (transpiler-internal typedef names used in stubs)
        TYPEDEFS.put("StringCallback", new TypeRef[] {TypeRef.STRING, TypeRef.VOID});
        TYPEDEFS.put("BoolCallback", new TypeRef[] {TypeRef.BOOL, TypeRef.VOID});
        TYPEDEFS.put("DoubleCallback", new TypeRef[] {TypeRef.DOUBLE, TypeRef.VOID});
        TYPEDEFS.put("IntCallback", new TypeRef[] {TypeRef.INT, TypeRef.VOID});
        TYPEDEFS.put("DynamicCallback", new TypeRef[] {TypeRef.DYNAMIC, TypeRef.VOID});
    }

    private Out emitLambda(Lambda l, TypeRef expected, Ctx ctx) {
        // typedef-typed target position gives untyped lambda params real types
        TypeRef[] sigTypes = expected != null ? TYPEDEFS.get(expected.name) : null;
        boolean outerAsync = ctx.inAsyncBody;
        TypeRef outerReturn = ctx.methodReturnType;
        ctx.inAsyncBody = false;
        ctx.methodReturnType = null;
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
            Out o = emitExpr(l.exprBody, null, ctx);
            String lifted = ctx.popWriter();
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
        ctx.inAsyncBody = outerAsync;
        ctx.methodReturnType = outerReturn;
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
        Out target = emitExpr(c.target, null, ctx);
        return emitMethodCallOn(target, c, ctx);
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
            return new Out(n + ".call(" + plainArgs(c.args, ctx) + ")", TypeRef.DYNAMIC);
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
        // Duration(seconds: 2, ...) — dart:core intrinsic with canonical named order
        if (n.equals("Duration")) {
            ctx.importClass("dart.core.Duration");
            String[] names = {"days", "hours", "minutes", "seconds", "milliseconds", "microseconds"};
            StringBuilder sb = new StringBuilder("Duration.of(");
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Expr match = null;
                for (NamedArg na : c.args.named) {
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
            return emitCtorCall(n, c.args, c, ctx);
        }
        // constructor of stub class
        if (stubs.isStubClass(n)) {
            return emitCtorCall(n, c.args, c, ctx);
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
        }
        // top-level function (user code)
        FunctionDecl fn = program.functions.get(n);
        if (fn != null) {
            Library owner = program.functionOwners.get(n);
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
            ctx.importClass(cls);
            String simple = cls.substring(cls.lastIndexOf('.') + 1);
            return new Out(simple + "." + method + "(" + methodArgs(sf.params, c.args, ctx) + ")",
                    sf.returnType);
        }
        diags.error(c, "E0135", "Cannot resolve function or constructor '" + n
                + "'. Confirm the file passes `dart analyze`, or the API may be unsupported in M1.");
        return new Out("null", TypeRef.DYNAMIC);
    }

    private Out emitMethodCallOn(Out target, Call c, Ctx ctx) {
        TypeRef tt = target.type;
        String n = c.name;
        // static method on a class reference
        if (isClassRef(tt)) {
            String cls = tt.arg(0).name;
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
                    return new Out(stubSimpleName(cls, ctx) + "." + n + "("
                            + stubMethodArgs(m, c.args, ctx) + ")", m.returnType);
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
                return new Out("null", TypeRef.DYNAMIC);
            }
            diags.error(c, "E0136", "Cannot resolve static method '" + n + "' on " + cls);
            return new Out("null", TypeRef.DYNAMIC);
        }
        // intrinsics
        Out intrinsic = intrinsicCall(target, c, ctx);
        if (intrinsic != null) {
            return intrinsic;
        }
        // program class instance method
        ClassDecl pc = program.classes.get(tt.name);
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
        }
        // stub instance method
        String stubName = stubs.isStubClass(tt.name) ? tt.name : null;
        if (stubName != null || tt.is("State")) {
            Ast.MethodDecl m = stubs.findMethod(tt.name, n, false);
            if (m != null) {
                return new Out(target.code + "." + n + "(" + stubMethodArgs(m, c.args, ctx) + ")",
                        m.returnType);
            }
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
        // Object protocol
        if (n.equals("toString") && c.args.positional.isEmpty()) {
            ctx.importClass("dart.runtime.DartRuntime");
            return new Out("DartRuntime.str(" + target.code + ")", TypeRef.STRING);
        }
        diags.error(c, "E0137", "Cannot resolve method '" + n + "' on type " + tt
                + ". Confirm the file passes `dart analyze`, or the API may be unsupported in M1.");
        return new Out(target.code + "." + n + "(" + plainArgs(c.args, ctx) + ")", TypeRef.DYNAMIC);
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
            if (n.equals("toUpperCase") || n.equals("toLowerCase") || n.equals("trim")) {
                return new Out(target.code + "." + n + "()", TypeRef.STRING);
            }
            if (n.equals("startsWith") || n.equals("endsWith")) {
                return new Out(target.code + "." + n + "("
                        + emitExpr(pos.get(0), null, ctx).code + ")", TypeRef.BOOL);
            }
            if (n.equals("toString")) {
                return new Out(target.code, TypeRef.STRING);
            }
        }
        if (tt.is("int")) {
            if (n.equals("toString")) {
                return new Out("Long.toString(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("toDouble")) {
                return new Out("((double) " + paren(target.code) + ")", TypeRef.DOUBLE);
            }
            if (n.equals("abs")) {
                return new Out("Math.abs(" + target.code + ")", TypeRef.INT);
            }
        }
        if (tt.is("double")) {
            ctx.importClass("dart.runtime.DartRuntime");
            if (n.equals("toString")) {
                return new Out("DartRuntime.doubleStr(" + target.code + ")", TypeRef.STRING);
            }
            if (n.equals("toInt")) {
                return new Out("((long) " + paren(target.code) + ")", TypeRef.INT);
            }
            if (n.equals("floor")) {
                return new Out("((long) Math.floor(" + target.code + "))", TypeRef.INT);
            }
            if (n.equals("ceil")) {
                return new Out("((long) Math.ceil(" + target.code + "))", TypeRef.INT);
            }
            if (n.equals("round")) {
                return new Out("Math.round(" + target.code + ")", TypeRef.INT);
            }
            if (n.equals("abs")) {
                return new Out("Math.abs(" + target.code + ")", TypeRef.DOUBLE);
            }
        }
        if (tt.is("List") || tt.is("Iterable") || tt.is("Set")) {
            TypeRef elem = tt.arg(0);
            if (n.equals("add")) {
                Out v = emitExpr(pos.get(0), elem, ctx);
                return new Out(target.code + ".add(" + boxIfPrimitive(v, ctx) + ")", TypeRef.VOID);
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
                return new Out(target.code + ".indexOfDart(" + boxIfPrimitive(v, ctx) + ")", TypeRef.INT);
            }
            if (n.equals("join")) {
                String sep = pos.isEmpty() ? "\"\"" : emitExpr(pos.get(0), null, ctx).code;
                return new Out(target.code + ".join(" + sep + ")", TypeRef.STRING);
            }
            if (n.equals("map")) {
                Out f = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".map(" + f.code + ")", TypeRef.of("Iterable", TypeRef.DYNAMIC));
            }
            if (n.equals("where")) {
                Out f = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".where(" + f.code + ")", TypeRef.of("Iterable", elem));
            }
            if (n.equals("forEach")) {
                Out f = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".forEachDart(" + f.code + ")", TypeRef.VOID);
            }
            if (n.equals("toList")) {
                return new Out(target.code + ".toList()", TypeRef.of("List", elem));
            }
            if (n.equals("sublist")) {
                String args = "";
                for (Expr e : pos) {
                    args += (args.isEmpty() ? "" : ", ") + emitExpr(e, TypeRef.INT, ctx).code;
                }
                return new Out(target.code + ".sublist(" + args + ")", tt);
            }
            if (n.equals("clear")) {
                return new Out(target.code + ".clear()", TypeRef.VOID);
            }
            if (n.equals("any") || n.equals("every")) {
                Out f = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + "." + n + "(" + f.code + ")", TypeRef.BOOL);
            }
        }
        if (tt.is("Map")) {
            if (n.equals("containsKey")) {
                Out v = emitExpr(pos.get(0), null, ctx);
                return new Out(target.code + ".containsKey(" + boxIfPrimitive(v, ctx) + ")", TypeRef.BOOL);
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
            if (n.equals("clear")) {
                return new Out(target.code + ".clear()", TypeRef.VOID);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Constructor calls
    // ------------------------------------------------------------------

    private Out emitCtorCall(String className, Args args, Node posNode, Ctx ctx) {
        ClassDecl pc = program.classes.get(className);
        if (pc != null) {
            CtorDecl ct = pc.defaultCtor();
            if (ct != null && ct.isFactory) {
                return new Out(className + ".$create(" + canonicalArgs(ct, args, ctx) + ")",
                        new TypeRef(className));
            }
            return new Out("new " + className + "(" + canonicalArgs(ct, args, ctx) + ")",
                    new TypeRef(className));
        }
        Ast.ClassDecl sc = stubs.classes.get(className);
        if (sc == null) {
            diags.error(posNode, "E0135", "Cannot resolve constructor '" + className + "'");
            return new Out("null", TypeRef.DYNAMIC);
        }
        String simple = stubSimpleName(className, ctx);
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
            Out o = emitExpr(args.positional.get(i), pt, ctx);
            if (i > 0) {
                posArgs.append(", ");
            }
            posArgs.append(coerce(o, pt, ctx));
        }
        if (args.named.isEmpty()) {
            return new Out("new " + simple + "(" + posArgs + ")", new TypeRef(className));
        }
        // allocate-then-setters (ANF)
        String tmp = ctx.newTemp();
        ctx.writer().line("var " + tmp + " = new " + simple + "(" + posArgs + ");");
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
            Out v = emitExpr(na.value, pt, ctx);
            ctx.writer().line(tmp + "." + na.name + "(" + coerce(v, pt, ctx) + ");");
        }
        return new Out(tmp, new TypeRef(className));
    }

    /** Program-class calls use canonical positional order with defaults inlined. */
    private String canonicalArgs(CtorDecl ct, Args args, Ctx ctx) {
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
        int posIdx = 0;
        boolean first = true;
        for (Param p : ct.params) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            TypeRef pt = paramType(null, p, ctx);
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
        return sb.toString();
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
        if (p.type != null && !p.type.is("var")) {
            return p.type;
        }
        if (p.isThis && c != null) {
            FieldDecl f = c.field(p.name);
            if (f != null) {
                return fieldType(f, ctx);
            }
        }
        if (p.isSuper && c != null && c.superclass != null) {
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
            return "void";
        }
        if (t.is("num")) {
            return "Number";
        }
        if (t.is("List")) {
            ctx.importClass("dart.core.DartList");
            return "DartList<" + javaType(t.arg(0), true, ctx) + ">";
        }
        if (t.is("Map")) {
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
        if (TYPEDEFS.containsKey(t.name)) {
            ctx.importClass("dart.runtime.Funcs");
            TypeRef[] sig = TYPEDEFS.get(t.name);
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
            return "Object";
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

    private String stubSimpleName(String dartName, Ctx ctx) {
        Ast.ClassDecl sc = stubs.classes.get(dartName);
        if (sc != null && sc.javaName != null) {
            ctx.importClass(sc.javaName);
            return sc.javaName.substring(sc.javaName.lastIndexOf('.') + 1);
        }
        return dartName;
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

    /** Pseudo-type marking a reference to a class itself (for static access). */
    private TypeRef classRef(String className) {
        TypeRef t = new TypeRef("$class");
        t.args.add(new TypeRef(className));
        return t;
    }

    private boolean isClassRef(TypeRef t) {
        return t != null && t.is("$class");
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

    private String coerce(Out o, TypeRef target, Ctx ctx) {
        if (target == null) {
            return o.code;
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
        final Map<String, String> imports = new TreeMap<String, String>();
        final List<Map<String, TypeRef>> scopes = new ArrayList<Map<String, TypeRef>>();
        final List<Writer> writers = new ArrayList<Writer>();
        final java.util.Set<String> boxedLocals = new HashSet<String>();
        private final java.util.Set<String> boxedActive = new HashSet<String>();
        TypeRef methodReturnType;
        TypeRef extensionSelfType;
        boolean inAsyncBody;
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

        Ctx(ClassDecl currentClass) {
            this.currentClass = currentClass;
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
            String javaName = name;
            if (lookup(name) != null) {
                javaName = name + "$" + (shadowCounter++);
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
