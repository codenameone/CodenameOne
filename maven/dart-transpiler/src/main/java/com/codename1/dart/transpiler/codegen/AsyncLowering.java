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

import com.codename1.dart.transpiler.ast.Ast.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers an async body to a resumable frame: a state machine that returns at every
/// await and is resumed, from the microtask queue, with the awaited value. Nothing
/// blocks and no thread is involved.
///
/// The body becomes an anonymous `dart.async.LambdaFrame` (or `LambdaVoidFrame` when
/// nobody sees the Future) created where the body was, whose fields are the body's
/// parameters and the locals declared at the level of the statements that contain an
/// await, and whose states are one lambda: a `switch` on the frame's state inside a
/// labeled `for (;;)`. A statement without an await is emitted by JavaEmitter as it
/// always is, inside one state. Closures created in the body read and write the fields
/// through the frame, so they see what later states assign.
///
/// The shapes lowered are chosen from the AST alone (lowerable): an await that is a
/// whole expression statement, a local's initializer, the right-hand side of a plain
/// assignment to a name, or a returned value; and if, while, do, for, for-in, switch,
/// labels, break, continue and return around them. Anything else -- an await inside a
/// larger expression, a try statement or a pattern holding an await, `await for`, a
/// local function, a late local or a pattern declaration among the statements that are
/// split into states -- keeps the existing emission for the whole body.
final class AsyncLowering {
    private final JavaEmitter em;

    AsyncLowering(JavaEmitter em) {
        this.em = em;
    }

    private static final int LOOP = 0;
    private static final int SWITCH = 1;
    private static final int LABEL = 2;

    /// A break/continue target the lowering split into states.
    private static final class Target {
        final int kind;
        final List<String> labels;
        final int brk;
        final int cont;

        Target(int kind, List<String> labels, int brk, int cont) {
            this.kind = kind;
            this.labels = labels == null ? Collections.<String>emptyList() : labels;
            this.brk = brk;
            this.cont = cont;
        }
    }

    /// The frame being emitted, read by JavaEmitter's hooks (Ctx.frame).
    static final class Frame {
        String name;
        String label;
        boolean isVoid;
        TypeRef valueType;
        int states = 1;
        boolean closed;
        final List<String> fields = new ArrayList<String>();
        final Set<String> fieldNames = new HashSet<String>();
        final Map<Stmt, String> jumps = new IdentityHashMap<Stmt, String>();
        final Set<Stmt> returns = Collections.newSetFromMap(new IdentityHashMap<Stmt, Boolean>());
        final List<Target> targets = new ArrayList<Target>();
        VarDeclStmt fieldDecl;
        /// What closures reference in each split loop being lowered, innermost last.
        final List<Set<String>> loopCaps = new ArrayList<Set<String>>();
        /// Per-iteration variables: Dart name to {field, type, holder class}.
        final Map<String, Object[]> cells = new HashMap<String, Object[]>();
        AwaitExpr resumeAwait;
        JavaEmitter.Out resumeOperand;
        TypeRef resumeExpected;
    }

    // ------------------------------------------------------------------
    // AST queries
    // ------------------------------------------------------------------

    private static final Map<Class<?>, List<Field>> FIELDS = new HashMap<Class<?>, List<Field>>();

    private static synchronized List<Field> nodeFields(Class<?> c) {
        List<Field> fs = FIELDS.get(c);
        if (fs == null) {
            fs = new ArrayList<Field>();
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    // Args is no Node: a call's arguments hold awaits too.
                    if (!Modifier.isStatic(f.getModifiers()) && (Node.class.isAssignableFrom(f.getType())
                            || List.class.isAssignableFrom(f.getType()) || f.getType() == Args.class)) {
                        f.setAccessible(true);
                        fs.add(f);
                    }
                }
            }
            FIELDS.put(c, fs);
        }
        return fs;
    }

    /// Whether {@code n} holds an await of its own body: nested functions are not entered.
    static boolean hasAwait(Object n) {
        if (n == null) {
            return false;
        }
        if (n instanceof List) {
            for (Object o : (List<?>) n) {
                if (hasAwait(o)) {
                    return true;
                }
            }
            return false;
        }
        if (n instanceof Args) {
            return hasAwait(((Args) n).positional) || hasAwait(((Args) n).named);
        }
        if (n instanceof NamedArg) {
            return hasAwait(((NamedArg) n).value);
        }
        if (!(n instanceof Node) || n instanceof Lambda || n instanceof LocalFunc) {
            return false;
        }
        if (n instanceof AwaitExpr || n instanceof ForInStmt && ((ForInStmt) n).isAwait) {
            return true;
        }
        try {
            for (Field f : nodeFields(n.getClass())) {
                if (hasAwait(f.get(n))) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return false;
    }

    private static Expr unparen(Expr e) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        return e;
    }

    /// An await whose operand holds no await, else null.
    private static AwaitExpr plainAwait(Expr e) {
        e = unparen(e);
        if (e instanceof AwaitExpr && !hasAwait(((AwaitExpr) e).operand)) {
            return (AwaitExpr) e;
        }
        return null;
    }

    /// The await a statement-level shape suspends on, else null.
    static AwaitExpr statementAwait(Stmt s) {
        if (s instanceof ExprStmt) {
            Expr e = unparen(((ExprStmt) s).expr);
            if (e instanceof Assign && "=".equals(((Assign) e).op) && (unparen(((Assign) e).lhs) instanceof Ident
                    || simpleTarget(unparen(((Assign) e).lhs)))) {
                return plainAwait(((Assign) e).rhs);
            }
            return plainAwait(e);
        }
        if (s instanceof VarDeclStmt && !((VarDeclStmt) s).isLate) {
            return plainAwait(((VarDeclStmt) s).initializer);
        }
        if (s instanceof ReturnStmt) {
            return plainAwait(((ReturnStmt) s).value);
        }
        return null;
    }

    /// `a.b` or `a[i]` over names and literals: what AsyncAnf leaves ahead of an awaited
    /// assignment, its parts already evaluated into locals.
    private static boolean simpleTarget(Expr lhs) {
        if (lhs instanceof PropertyGet && !((PropertyGet) lhs).nullAware) {
            return simplePart(((PropertyGet) lhs).target);
        }
        if (lhs instanceof IndexGet) {
            return simplePart(((IndexGet) lhs).target) && simplePart(((IndexGet) lhs).index);
        }
        return false;
    }

    private static boolean simplePart(Expr e) {
        return e instanceof Ident || e instanceof ThisExpr || e instanceof IntLit || e instanceof StringLit
                && ((StringLit) e).parts.size() <= 1 && !hasAwait(e);
    }

    /// Whether the body can be lowered; see the class comment for the shapes.
    static boolean lowerable(Block body) {
        return listOk(body.statements);
    }

    private static boolean listOk(List<Stmt> list) {
        for (Stmt st : list) {
            boolean known = st instanceof ExprStmt || st instanceof VarDeclStmt && !((VarDeclStmt) st).isLate
                    || st instanceof IfStmt || st instanceof WhileStmt || st instanceof ForStmt
                    || st instanceof ForInStmt || st instanceof ReturnStmt || st instanceof BreakStmt
                    || st instanceof ContinueStmt || st instanceof SwitchStmt || st instanceof TryStmt
                    || st instanceof Block || st instanceof RethrowStmt;
            if (st instanceof VarDeclGroup) {
                known = true;
                for (VarDeclStmt d : ((VarDeclGroup) st).decls) {
                    known &= !d.isLate;
                }
            }
            if (!known || !stmtOk(st)) {
                return false;
            }
        }
        return true;
    }

    private static boolean stmtOk(Stmt s) {
        if (s == null || !hasAwait(s)) {
            return true;
        }
        if (s instanceof Block) {
            return listOk(((Block) s).statements);
        }
        if (s instanceof ExprStmt || s instanceof VarDeclStmt || s instanceof ReturnStmt) {
            return statementAwait(s) != null;
        }
        if (s instanceof VarDeclGroup) {
            for (VarDeclStmt d : ((VarDeclGroup) s).decls) {
                if (!stmtOk(d)) {
                    return false;
                }
            }
            return true;
        }
        if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            return i.casePattern == null && !hasAwait(i.condition) && branchOk(i.thenStmt) && branchOk(i.elseStmt);
        }
        if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            return !hasAwait(w.condition) && branchOk(w.body);
        }
        if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            return (f.init == null || !hasAwait(f.init)) && !hasAwait(f.condition)
                    && !hasAwait(f.updates) && branchOk(f.body);
        }
        if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            return !f.isAwait && f.pattern == null && !hasAwait(f.iterable) && branchOk(f.body);
        }
        if (s instanceof SwitchStmt) {
            SwitchStmt sw = (SwitchStmt) s;
            if (hasAwait(sw.subject)) {
                return false;
            }
            for (SwitchCase c : sw.cases) {
                if (hasAwait(c.guard)) {
                    return false;
                }
                if (hasAwait(c.body) && (c.pattern != null && !(c.pattern instanceof ConstantPattern)
                        || !listOk(c.body))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean branchOk(Stmt s) {
        if (s == null || !hasAwait(s)) {
            return true;
        }
        if (s instanceof Block) {
            return listOk(((Block) s).statements);
        }
        return listOk(Collections.singletonList(s));
    }

    private static void collectReturns(Object n, Set<Stmt> into) {
        if (n == null) {
            return;
        }
        if (n instanceof List) {
            for (Object o : (List<?>) n) {
                collectReturns(o, into);
            }
            return;
        }
        if (!(n instanceof Node) || n instanceof Lambda || n instanceof LocalFunc) {
            return;
        }
        if (n instanceof ReturnStmt) {
            into.add((Stmt) n);
        }
        if (n instanceof Expr) {
            return;
        }
        try {
            for (Field f : nodeFields(n.getClass())) {
                collectReturns(f.get(n), into);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------
    // Entry
    // ------------------------------------------------------------------

    /// Emits {@code body} as a frame and the statement that starts it: `return` of the
    /// frame's Future for a Future-valued body ({@code rt} its declared type, for the
    /// witness), the bare start for a void one. False, with nothing written, when the
    /// body has no await or a shape this stage does not lower.
    boolean tryEmit(List<Param> params, Block body, boolean isVoid, TypeRef valueType, TypeRef rt,
            JavaEmitter.Ctx ctx) {
        if (body == null || !hasAwait(body.statements)) {
            return false;
        }
        body = new AsyncAnf(params, body).normalize(body);
        if (!lowerable(body)) {
            return false;
        }
        Frame f = new Frame();
        f.name = ctx.newTemp();
        f.label = f.name + "sm";
        f.isVoid = isVoid;
        f.valueType = valueType == null ? TypeRef.DYNAMIC : valueType;
        collectReturns(body.statements, f.returns);
        List<String> paramCopies = new ArrayList<String>();
        List<String> outerNames = new ArrayList<String>();
        for (Param p : params) {
            outerNames.add(ctx.javaNameOf(p.name));
        }
        Frame saved = ctx.frame;
        int base = ctx.currentIndent();
        ctx.pushScope();
        ctx.frame = f;
        String cases;
        try {
            for (int i = 0; i < params.size(); i++) {
                Param p = params.get(i);
                TypeRef pt = ctx.lookup(p.name);
                if (pt == null) {
                    pt = p.type != null && !p.type.is("var") ? p.type : TypeRef.DYNAMIC;
                }
                String field = declareField(f, ctx, p.name, pt, em.javaType(pt, false, ctx));
                paramCopies.add(field + " = " + outerNames.get(i) + ";");
            }
            ctx.pushWriter(base + 4);
            JavaEmitter.Ctx.Writer w = ctx.writer();
            ctx.indent(-1);
            w.line("case 0: {");
            ctx.indent(1);
            lowerList(body.statements, f, ctx);
            if (!f.closed) {
                w.line(f.name + ".complete(null);");
                w.line("return;");
            }
            ctx.indent(-1);
            w.line("}");
            ctx.indent(1);
            cases = ctx.popWriter();
        } finally {
            ctx.frame = saved;
            ctx.popScope();
        }
        JavaEmitter.Ctx.Writer w = ctx.writer();
        // A closure's Future type can name the callee's type variables, which are not in
        // scope here; the frame is then a Future<Object>, handed over raw.
        // So is a body whose Java signature is wider than its Dart Future (a pinned or
        // erased return) or declared FutureOr: the Future converts to it unchecked.
        boolean erased = !isVoid && (!em.typeVarsInScope(f.valueType, ctx) || ctx.asyncReturnWidened
                || rt != null && !rt.is("Future"));
        String cls = isVoid ? "dart.async.LambdaVoidFrame"
                : "dart.async.LambdaFrame<" + (erased ? "Object" : em.javaType(em.boxType(f.valueType), true, ctx)) + ">";
        w.line("var " + f.name + " = new " + cls + "() {");
        for (String fd : f.fields) {
            w.line("    " + fd);
        }
        w.line("};");
        for (String pc : paramCopies) {
            w.line(pc);
        }
        w.line(f.name + ".body = () -> {");
        w.line("    " + f.label + ": for (;;) {");
        w.line("        switch (" + f.name + ".state) {");
        w.raw(cases);
        w.line("            default:");
        w.line("                throw new IllegalStateException(\"async frame state \" + " + f.name + ".state);");
        w.line("        }");
        w.line("    }");
        w.line("};");
        if (isVoid) {
            w.line(f.name + ".start();");
        } else {
            String start = (erased ? "(dart.async.Future) " : "") + f.name + ".start()";
            w.line("return " + (rt != null && !erased ? em.reifyAsyncResult(start, rt, ctx) : start) + ";");
        }
        return true;
    }

    private static boolean isCell(Frame f, String name) {
        for (Set<String> caps : f.loopCaps) {
            if (caps.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /// A local of a split loop that a closure captures: the field holds a cell, and every
    /// pass declares a new one, so a closure keeps the variable of the pass that made it.
    private void declareCell(Frame f, JavaEmitter.Ctx ctx, String name, TypeRef t, String init) {
        String holder = em.refHolder(t, ctx);
        String raw = holder.startsWith("Ref<") ? "Ref<>" : holder;
        String ref = declareField(f, ctx, name, t, holder);
        ctx.markBoxed(ref);
        f.cells.put(name, new Object[] {ref, t, raw});
        ctx.writer().line(ref + " = new " + raw + "(" + init + ");");
    }

    private static List<VarDeclStmt> initDecls(Stmt init) {
        List<VarDeclStmt> out = new ArrayList<VarDeclStmt>();
        if (init instanceof VarDeclStmt) {
            out.add((VarDeclStmt) init);
        } else if (init instanceof VarDeclGroup) {
            out.addAll(((VarDeclGroup) init).decls);
        }
        return out;
    }

    /// Before a statement whose closures capture per-iteration variables: each cell is
    /// copied to a Java local the closures capture, so a later pass's cell is not theirs.
    private static List<Object[]> snapshot(Stmt s, Frame f, JavaEmitter.Ctx ctx) {
        List<Object[]> undo = new ArrayList<Object[]>();
        if (f.cells.isEmpty()) {
            return undo;
        }
        for (String name : CaptureScan.lambdaRefs(s)) {
            Object[] c = f.cells.get(name);
            if (c == null || !((String) c[0]).equals(ctx.javaNameOf(name))) {
                continue;
            }
            String local = ctx.newTemp();
            ctx.writer().line("var " + local + " = " + c[0] + ";");
            ctx.declareAs(name, (TypeRef) c[1], local);
            ctx.markBoxed(local);
            undo.add(c);
            undo.add(new Object[] {name});
        }
        return undo;
    }

    private static void restore(List<Object[]> undo, JavaEmitter.Ctx ctx) {
        for (int i = 0; i < undo.size(); i += 2) {
            Object[] c = undo.get(i);
            ctx.declareAs((String) undo.get(i + 1)[0], (TypeRef) c[1], (String) c[0]);
            ctx.markBoxed((String) c[0]);
        }
    }

    private static String declareField(Frame f, JavaEmitter.Ctx ctx, String dartName, TypeRef t, String javaType) {
        // Prefixed: a bare `state`, `future` or `body` would hide the frame's own members.
        String base = "l$" + JavaEmitter.javaIdent(dartName);
        String name = base;
        for (int i = 1; f.fieldNames.contains(name); i++) {
            name = base + "$" + i;
        }
        f.fieldNames.add(name);
        f.fields.add(javaType + " " + name + ";");
        String ref = f.name + "." + name;
        ctx.declareAs(dartName, t, ref);
        return ref;
    }

    // ------------------------------------------------------------------
    // States
    // ------------------------------------------------------------------

    private static int newState(Frame f) {
        return f.states++;
    }

    private static String jumpCode(Frame f, int k) {
        return f.name + ".state = " + k + "; continue " + f.label + ";";
    }

    private static void jump(Frame f, int k, JavaEmitter.Ctx ctx) {
        if (!f.closed) {
            ctx.writer().line(jumpCode(f, k));
            f.closed = true;
        }
    }

    /// Ends the current state (falling into {@code k}) and opens state {@code k}.
    private static void open(Frame f, int k, JavaEmitter.Ctx ctx) {
        jump(f, k, ctx);
        JavaEmitter.Ctx.Writer w = ctx.writer();
        ctx.indent(-1);
        w.line("}");
        w.line("case " + k + ": {");
        ctx.indent(1);
        f.closed = false;
    }

    private void lowerList(List<Stmt> list, Frame f, JavaEmitter.Ctx ctx) {
        for (Stmt s : list) {
            lower(s, f, ctx);
        }
    }

    private void lowerBranch(Stmt s, Frame f, JavaEmitter.Ctx ctx) {
        ctx.pushScope();
        if (s instanceof Block && s.labels == null) {
            lowerList(((Block) s).statements, f, ctx);
        } else if (s != null) {
            lower(s, f, ctx);
        }
        ctx.popScope();
    }

    private void lower(Stmt s, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        if (f.closed) {
            // Dart allows statements after a return; they never run, and Java rejects them.
            int dead = newState(f);
            open(f, dead, ctx);
        }
        if (s instanceof VarDeclGroup) {
            for (VarDeclStmt d : ((VarDeclGroup) s).decls) {
                lower(d, f, ctx);
            }
            return;
        }
        if (s instanceof VarDeclStmt && !hasAwait(s)) {
            f.fieldDecl = (VarDeclStmt) s;
            List<Object[]> snap = snapshot(s, f, ctx);
            try {
                em.emitStatement(s, ctx);
            } finally {
                f.fieldDecl = null;
            }
            restore(snap, ctx);
            return;
        }
        if (!hasAwait(s)) {
            registerEscapes(s, 0, 0, new HashSet<String>(), f);
            w.line("if (true) {");
            ctx.indent(1);
            List<Object[]> snap = snapshot(s, f, ctx);
            em.emitStatement(s, ctx);
            restore(snap, ctx);
            ctx.indent(-1);
            w.line("}");
            return;
        }
        boolean loop = s instanceof WhileStmt || s instanceof ForStmt || s instanceof ForInStmt;
        if (s.labels != null && !loop) {
            int exit = newState(f);
            f.targets.add(new Target(LABEL, s.labels, exit, -1));
            List<String> labels = s.labels;
            s.labels = null;
            try {
                lower(s, f, ctx);
            } finally {
                s.labels = labels;
            }
            f.targets.remove(f.targets.size() - 1);
            open(f, exit, ctx);
            return;
        }
        if (s instanceof Block) {
            lowerBranch(s, f, ctx);
            return;
        }
        AwaitExpr aw = statementAwait(s);
        if (aw != null) {
            TypeRef expected = null;
            if (s instanceof VarDeclStmt && ((VarDeclStmt) s).type != null && !((VarDeclStmt) s).type.is("var")) {
                expected = ((VarDeclStmt) s).type;
            } else if (s instanceof ReturnStmt && !f.isVoid && !em.isDynamicType(f.valueType)) {
                expected = f.valueType;
            } else if (s instanceof ExprStmt && unparen(((ExprStmt) s).expr) instanceof Assign
                    && !(unparen(((Assign) unparen(((ExprStmt) s).expr)).lhs) instanceof Ident)) {
                // `o.p = await open()`: the slot's type infers the callee's witness.
                TypeRef lt = em.emitExpr(unparen(((Assign) unparen(((ExprStmt) s).expr)).lhs), null, ctx).type;
                expected = lt == null || em.isDynamicType(lt) ? null : lt;
            }
            List<Object[]> snap = snapshot(s, f, ctx);
            suspend(aw, expected, f, ctx);
            restore(snap, ctx);
            f.resumeAwait = aw;
            f.resumeExpected = expected;
            f.fieldDecl = s instanceof VarDeclStmt ? (VarDeclStmt) s : null;
            try {
                em.emitStatement(s, ctx);
            } finally {
                f.resumeAwait = null;
                f.resumeOperand = null;
                f.resumeExpected = null;
                f.fieldDecl = null;
            }
            if (s instanceof ReturnStmt) {
                f.closed = true;
            }
            return;
        }
        if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            JavaEmitter.Out c = em.dynCondition(em.emitExpr(i.condition, TypeRef.BOOL, ctx), ctx);
            int then = newState(f);
            int els = i.elseStmt != null ? newState(f) : -1;
            int exit = newState(f);
            w.line("if (" + c.code + ") {");
            w.line("    " + jumpCode(f, then));
            w.line("}");
            w.line(jumpCode(f, els >= 0 ? els : exit));
            f.closed = true;
            open(f, then, ctx);
            List<Object[]> undo = em.applyGuardPromotions(i.condition, ctx);
            lowerBranch(i.thenStmt, f, ctx);
            em.restorePromotions(undo, ctx);
            if (els >= 0) {
                jump(f, exit, ctx);
                open(f, els, ctx);
                List<Object[]> neg = new ArrayList<Object[]>();
                em.collectNegativePromotions(i.condition, ctx, neg);
                lowerBranch(i.elseStmt, f, ctx);
                em.restorePromotions(neg, ctx);
            }
            open(f, exit, ctx);
            return;
        }
        if (s instanceof WhileStmt) {
            WhileStmt wh = (WhileStmt) s;
            int head = newState(f);
            int body = newState(f);
            int exit = newState(f);
            if (wh.isDo) {
                open(f, body, ctx);
            } else {
                open(f, head, ctx);
                test(wh.condition, body, exit, f, ctx);
                open(f, body, ctx);
            }
            f.targets.add(new Target(LOOP, s.labels, exit, head));
            f.loopCaps.add(CaptureScan.lambdaRefs(wh.body));
            lowerBranch(wh.body, f, ctx);
            f.loopCaps.remove(f.loopCaps.size() - 1);
            f.targets.remove(f.targets.size() - 1);
            if (wh.isDo) {
                open(f, head, ctx);
                test(wh.condition, body, exit, f, ctx);
            } else {
                jump(f, head, ctx);
            }
            open(f, exit, ctx);
            return;
        }
        if (s instanceof ForStmt) {
            ForStmt fs = (ForStmt) s;
            ctx.pushScope();
            f.loopCaps.add(CaptureScan.lambdaRefs(s));
            if (fs.init != null) {
                lower(fs.init, f, ctx);
            }
            int head = newState(f);
            int body = newState(f);
            int next = newState(f);
            int exit = newState(f);
            open(f, head, ctx);
            if (fs.condition != null) {
                test(fs.condition, body, exit, f, ctx);
            }
            open(f, body, ctx);
            f.targets.add(new Target(LOOP, s.labels, exit, next));
            lowerBranch(fs.body, f, ctx);
            f.targets.remove(f.targets.size() - 1);
            open(f, next, ctx);
            // Dart's next pass gets its own variable, holding the value this one ended with.
            for (VarDeclStmt d : initDecls(fs.init)) {
                Object[] c = f.cells.get(d.name);
                if (c != null && ((String) c[0]).equals(ctx.javaNameOf(d.name))) {
                    w.line(c[0] + " = new " + c[2] + "(" + c[0] + ".v);");
                }
            }
            for (Expr u : fs.updates) {
                em.emitDiscardedValue(u, ctx);
            }
            jump(f, head, ctx);
            f.loopCaps.remove(f.loopCaps.size() - 1);
            ctx.popScope();
            open(f, exit, ctx);
            return;
        }
        if (s instanceof ForInStmt) {
            ForInStmt fi = (ForInStmt) s;
            JavaEmitter.Out iter = em.emitExpr(fi.iterable, null, ctx);
            TypeRef elem = fi.varType != null && !fi.varType.is("var") ? fi.varType
                    : iter.type != null && (iter.type.is("List") || iter.type.is("Iterable") || iter.type.is("Set"))
                    && iter.type.arg(0) != null ? iter.type.arg(0) : TypeRef.DYNAMIC;
            String it = f.name + "." + f.name + "it" + f.states;
            f.fields.add("java.util.Iterator<?> " + f.name + "it" + f.states + ";");
            w.line(it + " = ((java.lang.Iterable<?>) (Object) (" + iter.code + ")).iterator();");
            int head = newState(f);
            int exit = newState(f);
            open(f, head, ctx);
            w.line("if (!" + it + ".hasNext()) {");
            w.line("    " + jumpCode(f, exit));
            w.line("}");
            ctx.pushScope();
            String jt = em.javaType(elem, true, ctx);
            f.loopCaps.add(CaptureScan.lambdaRefs(fi.body));
            if (isCell(f, fi.varName)) {
                declareCell(f, ctx, fi.varName, elem, "(" + jt + ") " + it + ".next()");
            } else {
                String v = declareField(f, ctx, fi.varName, elem, jt);
                w.line(v + " = (" + jt + ") " + it + ".next();");
            }
            f.targets.add(new Target(LOOP, s.labels, exit, head));
            lowerBranch(fi.body, f, ctx);
            f.targets.remove(f.targets.size() - 1);
            f.loopCaps.remove(f.loopCaps.size() - 1);
            ctx.popScope();
            jump(f, head, ctx);
            open(f, exit, ctx);
            return;
        }
        if (s instanceof SwitchStmt) {
            SwitchStmt sw = (SwitchStmt) s;
            int exit = newState(f);
            SwitchStmt copy = new SwitchStmt();
            copy.file = sw.file;
            copy.line = sw.line;
            copy.col = sw.col;
            copy.subject = sw.subject;
            List<Object[]> moved = new ArrayList<Object[]>();
            for (SwitchCase c : sw.cases) {
                if (!hasAwait(c.body)) {
                    copy.cases.add(c);
                    continue;
                }
                SwitchCase cc = new SwitchCase();
                cc.file = c.file;
                cc.line = c.line;
                cc.col = c.col;
                cc.pattern = c.pattern;
                cc.guard = c.guard;
                cc.isDefault = c.isDefault;
                BreakStmt go = new BreakStmt();
                go.label = f.name + "case" + moved.size();
                int k = newState(f);
                f.jumps.put(go, jumpCode(f, k));
                cc.body.add(go);
                copy.cases.add(cc);
                moved.add(new Object[] {Integer.valueOf(k), c});
            }
            f.targets.add(new Target(SWITCH, null, exit, -1));
            registerEscapes(copy, 0, 0, new HashSet<String>(), f);
            w.line("if (true) {");
            ctx.indent(1);
            em.emitStatement(copy, ctx);
            ctx.indent(-1);
            w.line("}");
            jump(f, exit, ctx);
            for (Object[] m : moved) {
                open(f, ((Integer) m[0]).intValue(), ctx);
                ctx.pushScope();
                lowerList(((SwitchCase) m[1]).body, f, ctx);
                ctx.popScope();
                jump(f, exit, ctx);
            }
            f.targets.remove(f.targets.size() - 1);
            open(f, exit, ctx);
            return;
        }
        throw new IllegalStateException("not lowerable: " + s.getClass().getSimpleName());
    }

    private void test(Expr cond, int yes, int no, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Out c = em.dynCondition(em.emitExpr(cond, TypeRef.BOOL, ctx), ctx);
        JavaEmitter.Ctx.Writer w = ctx.writer();
        w.line("if (" + c.code + ") {");
        w.line("    " + jumpCode(f, yes));
        w.line("}");
        w.line(jumpCode(f, no));
        f.closed = true;
    }

    /// Evaluates the operand, suspends on it and opens the state that resumes.
    private void suspend(AwaitExpr aw, TypeRef expected, Frame f, JavaEmitter.Ctx ctx) {
        Call savedFor = ctx.callExpectedFor;
        TypeRef savedExp = ctx.callExpected;
        if (aw.operand instanceof Call && expected != null) {
            // A generic call's witness inference, as the blocking await does it:
            // `Box<int> b = await open(..)`.
            ctx.callExpectedFor = (Call) aw.operand;
            ctx.callExpected = TypeRef.of("Future", expected);
        }
        JavaEmitter.Out o;
        try {
            o = em.emitExpr(aw.operand, null, ctx);
        } finally {
            ctx.callExpectedFor = savedFor;
            ctx.callExpected = savedExp;
        }
        JavaEmitter.Ctx.Writer w = ctx.writer();
        String arg;
        if (o.type != null && o.type.is("void")) {
            w.line(o.code + ";");
            arg = "null";
        } else {
            arg = "(Object) (" + o.code + ")";
        }
        int k = newState(f);
        w.line(f.name + ".await(" + arg + ", " + k + ");");
        w.line("return;");
        f.closed = true;
        open(f, k, ctx);
        f.resumeOperand = o;
    }

    /// The awaited value in the resuming state, typed as Dart types the await.
    JavaEmitter.Out resumeValue(JavaEmitter.Ctx ctx) {
        Frame f = ctx.frame;
        TypeRef ot = f.resumeOperand.type;
        String v = f.name + ".value()";
        if (ot != null && ot.is("void")) {
            return new JavaEmitter.Out(v, TypeRef.VOID);
        }
        TypeRef inner;
        if (ot != null && (ot.is("Future") || ot.is("FutureOr"))) {
            inner = ot.args.isEmpty() || ot.arg(0) == null ? TypeRef.DYNAMIC : ot.arg(0);
        } else {
            inner = ot == null ? TypeRef.DYNAMIC : ot;
        }
        if (inner.is("void")) {
            return new JavaEmitter.Out(v, TypeRef.DYNAMIC);
        }
        if (f.resumeExpected != null && !em.isDynamicType(f.resumeExpected)) {
            // The slot's declared type: the operand's may name the callee's own type
            // variables (`Box<String> s = await open(5)` of `Future<Box<E>> open<E>`).
            inner = f.resumeExpected;
        }
        TypeRef boxed = em.boxType(inner);
        if (em.isDynamicType(boxed) || !em.typeVarsInScope(boxed, ctx)) {
            return new JavaEmitter.Out(v, TypeRef.DYNAMIC);
        }
        String pk = JavaEmitter.primitiveListKind(boxed);
        if (pk != null) {
            return new JavaEmitter.Out("dart.core.Dart" + pk + "List.as" + pk + "s(" + v + ")", boxed);
        }
        return new JavaEmitter.Out("((" + em.javaType(boxed, true, ctx) + ") " + v + ")", boxed);
    }

    /// A local declared among the statements split into states: a field of the frame.
    void emitFieldLocal(VarDeclStmt v, TypeRef t, JavaEmitter.Out init, JavaEmitter.Ctx ctx) {
        Frame f = ctx.frame;
        if (!em.typeVarsInScope(t, ctx)) {
            // Inferred from a callee's signature, its type variables are not in scope here.
            t = TypeRef.DYNAMIC;
        }
        String jt;
        if (t.is("Function") && t.funcReturn == null && v.initializer instanceof Lambda
                && ((Lambda) v.initializer).params.isEmpty()) {
            jt = "dart.runtime.Funcs.VoidFunc0";
        } else {
            jt = em.javaType(t, false, ctx);
        }
        String code = init != null ? (jt.equals("dart.runtime.Funcs.VoidFunc0") ? init.code : em.coerce(init, t, ctx))
                : em.zeroValue(t);
        if (init != null && jt.indexOf('<') > 0 && !(v.initializer instanceof Lambda)) {
            // A `var` inferred as Stream<Object> holding a Stream<Long>: generics are erased.
            code = "(" + jt + ") (Object) (" + code + ")";
        }
        if (!jt.equals("dart.runtime.Funcs.VoidFunc0") && isCell(f, v.name)) {
            declareCell(f, ctx, v.name, t, code);
            return;
        }
        String ref = declareField(f, ctx, v.name, t, jt);
        ctx.writer().line(ref + " = " + code + ";");
    }

    /// `return` in a lowered body: completes the frame's Future and leaves the step.
    void emitReturn(ReturnStmt r, JavaEmitter.Ctx ctx) {
        Frame f = ctx.frame;
        JavaEmitter.Ctx.Writer w = ctx.writer();
        if (r.value == null) {
            w.line(f.name + ".complete(null);");
        } else if (f.isVoid) {
            em.emitDiscardedValue(r.value, ctx);
            w.line(f.name + ".complete(null);");
        } else {
            TypeRef inner = f.valueType;
            JavaEmitter.Out o = em.emitExpr(r.value, em.isDynamicType(inner) ? null : inner, ctx);
            if (o.type != null && o.type.is("void")) {
                w.line(o.code + ";");
                w.line(f.name + ".complete(null);");
            } else {
                boolean future = o.type != null && (o.type.is("Future") || o.type.is("FutureOr"))
                        || em.isDynamicType(inner) || em.isDynamicType(o.type) || inner.is("void");
                String code = future ? o.code : em.coerce(o, inner, ctx);
                w.line(f.name + ".resolve((Object) (" + code + "));");
            }
        }
        w.line("return;");
    }

    // ------------------------------------------------------------------
    // break / continue leaving a statement emitted whole
    // ------------------------------------------------------------------

    private static void registerEscapes(Stmt s, int loops, int breakables, Set<String> labels, Frame f) {
        if (s == null) {
            return;
        }
        Set<String> inner = labels;
        if (s.labels != null) {
            inner = new HashSet<String>(labels);
            inner.addAll(s.labels);
        }
        if (s instanceof BreakStmt) {
            BreakStmt b = (BreakStmt) s;
            if (!f.jumps.containsKey(b) && (b.label == null ? breakables == 0 : !labels.contains(b.label))) {
                Target t = find(f, b.label, false);
                if (t != null) {
                    f.jumps.put(b, jumpCode(f, t.brk));
                }
            }
        } else if (s instanceof ContinueStmt) {
            ContinueStmt c = (ContinueStmt) s;
            if (c.label == null ? loops == 0 : !labels.contains(c.label)) {
                Target t = find(f, c.label, true);
                if (t != null) {
                    f.jumps.put(c, jumpCode(f, t.cont));
                }
            }
        } else if (s instanceof Block) {
            for (Stmt st : ((Block) s).statements) {
                registerEscapes(st, loops, breakables, inner, f);
            }
        } else if (s instanceof IfStmt) {
            registerEscapes(((IfStmt) s).thenStmt, loops, breakables, inner, f);
            registerEscapes(((IfStmt) s).elseStmt, loops, breakables, inner, f);
        } else if (s instanceof WhileStmt) {
            registerEscapes(((WhileStmt) s).body, loops + 1, breakables + 1, inner, f);
        } else if (s instanceof ForStmt) {
            registerEscapes(((ForStmt) s).body, loops + 1, breakables + 1, inner, f);
        } else if (s instanceof ForInStmt) {
            registerEscapes(((ForInStmt) s).body, loops + 1, breakables + 1, inner, f);
        } else if (s instanceof SwitchStmt) {
            for (SwitchCase c : ((SwitchStmt) s).cases) {
                for (Stmt st : c.body) {
                    registerEscapes(st, loops, breakables + 1, inner, f);
                }
            }
        } else if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            registerEscapes(t.tryBlock, loops, breakables, inner, f);
            for (CatchClause c : t.catches) {
                registerEscapes(c.body, loops, breakables, inner, f);
            }
            registerEscapes(t.finallyBlock, loops, breakables, inner, f);
        }
    }

    private static Target find(Frame f, String label, boolean isContinue) {
        for (int i = f.targets.size() - 1; i >= 0; i--) {
            Target t = f.targets.get(i);
            if (label != null ? t.labels.contains(label) : isContinue ? t.kind == LOOP : t.kind != LABEL) {
                return t;
            }
        }
        return null;
    }
}
