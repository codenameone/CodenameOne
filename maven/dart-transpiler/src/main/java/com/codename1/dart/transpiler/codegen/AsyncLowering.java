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
/// labels, break, continue, return and try/catch/finally around them (see lowerTry; a
/// try statement without an await stays a Java try inside one state). AsyncAnf has
/// already moved every other await out of its expression. Anything else -- a pattern
/// holding an await, `await for`, a local function, a late local or a pattern
/// declaration among the statements that are split into states -- keeps the existing
/// emission for the whole body.
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
        /// How many finally blocks enclose the target: a jump from inside more runs them.
        final int fins;

        Target(int fins, int kind, List<String> labels, int brk, int cont) {
            this.fins = fins;
            this.kind = kind;
            this.labels = labels == null ? Collections.<String>emptyList() : labels;
            this.brk = brk;
            this.cont = cont;
        }
    }

    /// A finally block being lowered: its state, and the fields of the pending completion
    /// the block ends with -- how the try was left ({@code c}: NEXT 0, RETURN 1 with the
    /// value in {@code v}, RETHROW 2 with the throwable in {@code v}, JUMP 3 to the state in
    /// {@code j}) -- and the jumps that left through it, each {state, enclosing finallies}.
    private static final class Fin {
        int state;
        String c;
        String v;
        String j;
        final List<int[]> jumps = new ArrayList<int[]>();
    }

    /// The frame being emitted, read by JavaEmitter's hooks (Ctx.frame).
    static final class Frame {
        /// The body's normalizer, for statements the lowering synthesizes (case guards).
        AsyncAnf anf;
        String name;
        String label;
        boolean isVoid;
        /// The local function being declared as a frame field (JavaEmitter.emitLocalFunc).
        LocalFunc localFunc;
        /// async*: the element type yields coerce to, else null.
        TypeRef starElem;
        /// sync*: the frame is a dart.async.SyncStarFrame, made afresh per iteration.
        boolean sync;
        /// The frame's Java element type argument; null when erased to Object.
        String starJava;
        TypeRef valueType;
        int states = 1;
        boolean closed;
        final List<String> fields = new ArrayList<String>();
        final Set<String> fieldNames = new HashSet<String>();
        final Map<Stmt, String> jumps = new IdentityHashMap<Stmt, String>();
        final Set<Stmt> returns = Collections.newSetFromMap(new IdentityHashMap<Stmt, Boolean>());
        final List<Target> targets = new ArrayList<Target>();
        VarDeclStmt fieldDecl;
        /// While a destructuring is emitted: its bindings are frame fields, not locals.
        boolean patternFields;
        /// What closures reference in each split loop being lowered, innermost last.
        final List<Set<String>> loopCaps = new ArrayList<Set<String>>();
        /// Per-iteration variables: Dart name to {field, type, holder class}.
        final Map<String, Object[]> cells = new HashMap<String, Object[]>();
        /// The finally blocks enclosing the statement being lowered, innermost last.
        final List<Fin> fins = new ArrayList<Fin>();
        /// Locals no later state reads, declared as Java locals (lowerList).
        final Set<Stmt> stateLocals = Collections.newSetFromMap(new IdentityHashMap<Stmt, Boolean>());
        /// What a throw in each state does: an index into handlers, absent for none.
        final Map<Integer, Integer> stateHandler = new HashMap<Integer, Integer>();
        /// Handler code, run with the throwable in `<frame>t`; it ends with a jump.
        final List<String> handlers = new ArrayList<String>();
        /// The handler of the states being opened, -1 for none.
        int handler = -1;
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
                    // A ClassDecl field is a back-reference, never a child: TypeRef.extensionType
                    // and Ident.pinnedClass reach a whole declaration, whose own types point back
                    // at it, and the walk recursed until the stack overflowed (async main
                    // mentioning an extension type).
                    if (!Modifier.isStatic(f.getModifiers()) && !"inferFrom".equals(f.getName())
                            && !ClassDecl.class.isAssignableFrom(f.getType())
                            && (Node.class.isAssignableFrom(f.getType())
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
        if (n instanceof AwaitExpr || n instanceof ForInStmt && ((ForInStmt) n).isAwait || n instanceof YieldStmt) {
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

    /// An arrow body `=> e` as the block `{ return e; }` (`{ e; }` for a void one), so an
    /// arrow that awaits is lowered through the frame like a block body instead of
    /// blocking its thread on the await.
    static Block arrowAsBlock(Expr e, boolean isVoid) {
        Block b = new Block();
        b.at(e.file, e.line, e.col);
        Stmt s;
        if (isVoid) {
            ExprStmt es = new ExprStmt();
            es.expr = e;
            s = es;
        } else {
            ReturnStmt r = new ReturnStmt();
            r.value = e;
            s = r;
        }
        s.at(e.file, e.line, e.col);
        b.statements.add(s);
        return b;
    }

    /// Whether the body can be lowered; see the class comment for the shapes.
    static boolean lowerable(Block body) {
        return listOk(body.statements);
    }

    private static boolean listOk(List<Stmt> list) {
        for (Stmt st : list) {
            boolean known = st instanceof ExprStmt
                    || st instanceof VarDeclStmt && !(((VarDeclStmt) st).isLate && hasAwait(st))
                    || st instanceof IfStmt || st instanceof WhileStmt || st instanceof ForStmt
                    || st instanceof ForInStmt || st instanceof ReturnStmt || st instanceof BreakStmt
                    || st instanceof ContinueStmt || st instanceof SwitchStmt || st instanceof TryStmt
                    || st instanceof Block || st instanceof RethrowStmt || st instanceof YieldStmt
                    || st instanceof LocalFunc
                    || st instanceof PatternDeclStmt && !hasAwait(((PatternDeclStmt) st).init);
            if (st instanceof VarDeclGroup) {
                known = true;
                for (VarDeclStmt d : ((VarDeclGroup) st).decls) {
                    known &= !(d.isLate && hasAwait(d));
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
        if (s instanceof YieldStmt) {
            return !hasAwait(((YieldStmt) s).value);
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
            return !hasAwait(i.condition) && !hasAwait(i.caseGuard) && branchOk(i.thenStmt) && branchOk(i.elseStmt);
        }
        if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            return !hasAwait(w.condition) && branchOk(w.body);
        }
        if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            return (f.init == null || !hasAwait(f.init) || listOk(Collections.singletonList(f.init)))
                    && !hasAwait(f.condition) && !hasAwait(f.updates)
                    && (f.conditionStmts == null || listOk(f.conditionStmts))
                    && (f.updateStmts == null || listOk(f.updateStmts)) && branchOk(f.body);
        }
        if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            if (!listOk(t.tryBlock.statements) || t.finallyBlock != null && !listOk(t.finallyBlock.statements)) {
                return false;
            }
            for (CatchClause c : t.catches) {
                if (!listOk(c.body.statements)) {
                    return false;
                }
            }
            return true;
        }
        if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            return !hasAwait(f.iterable) && branchOk(f.body);
        }
        if (s instanceof SwitchStmt) {
            SwitchStmt sw = (SwitchStmt) s;
            if (hasAwait(sw.subject)) {
                return false;
            }
            // A guard that awaits, or a case binding variables for a body that awaits, is
            // lowered case by case into states (lowerSwitchStates).
            for (SwitchCase c : sw.cases) {
                if (hasAwait(c.body) && !listOk(c.body)) {
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
        return emitFrame(params, body, isVoid, valueType, rt, null, null, ctx);
    }

    /// An async* body as an AsyncStarFrame, written as `return <its stream>;`.
    /// {@code witness} reifies the stream's element type (null: none). False, with
    /// nothing written, for a body shape this stage does not lower.
    boolean tryEmitStar(List<Param> params, Block body, TypeRef elem, String witness, JavaEmitter.Ctx ctx) {
        return body != null && emitFrame(params, body, true, null, null, elem == null ? TypeRef.DYNAMIC : elem,
                witness, ctx);
    }

    /// A sync* body as a SyncStarFrame, written as `return <its iterable>;`: the iterable
    /// builds a new frame for every iteration and each moveNext runs the body to its next
    /// yield. {@code witness} reifies the element type (null: none). False, with nothing
    /// written, for a body shape this stage does not lower (see lowerable).
    boolean tryEmitSyncStar(List<Param> params, Block body, TypeRef elem, String witness, JavaEmitter.Ctx ctx) {
        return body != null && emitFrame(params, body, true, null, null, elem == null ? TypeRef.DYNAMIC : elem,
                witness, true, ctx);
    }

    private boolean emitFrame(List<Param> params, Block body, boolean isVoid, TypeRef valueType, TypeRef rt,
            TypeRef starElem, String starWitness, JavaEmitter.Ctx ctx) {
        return emitFrame(params, body, isVoid, valueType, rt, starElem, starWitness, false, ctx);
    }

    private boolean emitFrame(List<Param> params, Block body, boolean isVoid, TypeRef valueType, TypeRef rt,
            TypeRef starElem, String starWitness, boolean sync, JavaEmitter.Ctx ctx) {
        if (body == null || starElem == null && !hasAwait(body.statements)) {
            return false;
        }
        final JavaEmitter.Ctx nctx = ctx;
        AsyncAnf anf = new AsyncAnf(params, body, new java.util.function.Predicate<String>() {
            @Override
            public boolean test(String n) {
                return em.namesNoValue(n, nctx);
            }
        });
        if (!isVoid && starElem == null && valueType != null && !em.isDynamicType(valueType)) {
            anf.returnContext = valueType;
        }
        body = anf.normalize(body);
        if (!lowerable(body)) {
            return false;
        }
        Frame f = new Frame();
        f.anf = anf;
        f.name = ctx.newTemp();
        f.label = f.name + "sm";
        f.isVoid = isVoid;
        f.starElem = starElem;
        f.sync = sync;
        if (starElem != null && !em.isDynamicType(starElem) && em.typeVarsInScope(starElem, ctx)) {
            f.starJava = em.javaType(em.boxType(starElem), true, ctx);
        }
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
        String cls = sync ? "dart.async.SyncStarFrame<" + (f.starJava == null ? "Object" : f.starJava) + ">"
                : starElem != null ? "dart.async.AsyncStarFrame<" + (f.starJava == null ? "Object" : f.starJava) + ">"
                : isVoid ? "dart.async.LambdaVoidFrame"
                : "dart.async.LambdaFrame<" + (erased ? "Object" : em.javaType(em.boxType(f.valueType), true, ctx)) + ">";
        if (sync) {
            // The factory lambda captures the arguments, which the method may have assigned
            // (a default, a coercion) before this point: copy each to an effectively final local.
            for (int i = 0; i < paramCopies.size(); i++) {
                String pc = paramCopies.get(i);
                int eq = pc.indexOf(" = ");
                String cap = f.name + "a" + i;
                w.line("var " + cap + " = " + outerNames.get(i) + ";");
                paramCopies.set(i, pc.substring(0, eq) + " = " + cap + ";");
            }
            // Every iteration runs the body afresh: the factory makes one frame per iterator.
            w.line("return dart.core.DartIterable.syncStarFrame(" + (starWitness == null ? "" : starWitness + ", ")
                    + "() -> {");
        }
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
        boolean guarded = !f.handlers.isEmpty();
        if (guarded) {
            // A throw in a state inside a lowered try goes where that state's region says.
            w.line("    try {");
        }
        w.line("        switch (" + f.name + ".state) {");
        w.raw(cases);
        w.line("            default:");
        w.line("                throw new IllegalStateException(\"async frame state \" + " + f.name + ".state);");
        w.line("        }");
        if (guarded) {
            String t = f.name + "t";
            w.line("    } catch (Throwable " + t + ") {");
            w.line("        switch (" + f.name + ".state) {");
            for (int h = 0; h < f.handlers.size(); h++) {
                StringBuilder labels = new StringBuilder();
                for (Map.Entry<Integer, Integer> e : f.stateHandler.entrySet()) {
                    if (e.getValue().intValue() == h) {
                        labels.append("case ").append(e.getKey()).append(": ");
                    }
                }
                if (labels.length() > 0) {
                    w.line("            " + labels + "{");
                    w.line("                " + f.handlers.get(h).replace("$T", t));
                    w.line("            }");
                }
            }
            w.line("            default:");
            w.line("                throw " + t + ";");
            w.line("        }");
            w.line("    }");
        }
        w.line("    }");
        w.line("};");
        if (sync) {
            w.line("return " + f.name + ";");
            w.line("});");
        } else if (starElem != null) {
            String stream = (f.starJava == null ? "(dart.async.Stream) " : "") + f.name + ".stream()";
            w.line("return " + (starWitness == null ? stream
                    : "dart.async.Stream.reify(" + stream + ", " + starWitness + ")") + ";");
        } else if (isVoid) {
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

    /// A frame field for the local function the frame is declaring, its reference returned.
    String frameField(String dartName, TypeRef t, String javaType, JavaEmitter.Ctx ctx) {
        return declareField(ctx.frame, ctx, dartName, t, javaType);
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
        if (f.handler >= 0) {
            f.stateHandler.put(Integer.valueOf(k), Integer.valueOf(f.handler));
        }
    }

    private void lowerList(List<Stmt> list, Frame f, JavaEmitter.Ctx ctx) {
        lowerList(list, null, f, ctx);
    }

    /// {@code after}: what reads the list's locals once it has run (a loop condition).
    private void lowerList(List<Stmt> list, Expr after, Frame f, JavaEmitter.Ctx ctx) {
        for (int i = 0; i < list.size(); i++) {
            Stmt s = list.get(i);
            if (s instanceof VarDeclStmt && !hasAwait(s) && javacTyped((VarDeclStmt) s)) {
                int split = i + 1;
                while (split < list.size() && !hasAwait(list.get(split))) {
                    split++;
                }
                String name = ((VarDeclStmt) s).name;
                if (!mentions(list.subList(split, list.size()), name)
                        && (split == list.size() || !mentions(after, name))) {
                    f.stateLocals.add(s);
                }
            }
            lower(s, f, ctx);
        }
    }

    /// An untyped local that outside a frame may be a Java `var`, typed by javac.
    private static boolean javacTyped(VarDeclStmt v) {
        return v.initializer != null && !(v.initializer instanceof Lambda) && (v.type == null || v.type.is("var"));
    }

    /// Whether {@code n} names {@code name}, closures included.
    private static boolean mentions(Object n, String name) {
        if (n instanceof List) {
            for (Object o : (List<?>) n) {
                if (mentions(o, name)) {
                    return true;
                }
            }
            return false;
        }
        if (n instanceof Args) {
            return mentions(((Args) n).positional, name) || mentions(((Args) n).named, name);
        }
        if (n instanceof NamedArg) {
            return mentions(((NamedArg) n).value, name);
        }
        if (!(n instanceof Node)) {
            return false;
        }
        if (n instanceof Ident && name.equals(((Ident) n).name)) {
            return true;
        }
        // `f(1)` calling a local function value names the local as the callee.
        if (n instanceof Call && ((Call) n).target == null && name.equals(((Call) n).name)) {
            return true;
        }
        try {
            for (Field f : nodeFields(n.getClass())) {
                if (mentions(f.get(n), name)) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return false;
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
        if ((s instanceof WhileStmt || s instanceof ForStmt || s instanceof ForInStmt || s instanceof TryStmt
                || s instanceof SwitchStmt || s.labels != null) && hasAwait(s)) {
            // Null-safety flow promotion across a suspending loop, try, switch or labelled
            // statement, conservatively: what it assigns is demoted before it, and only what
            // held at entry survives it.
            java.util.Set<String> pre = em.flowSnapshot(ctx);
            // A try is no loop: its body runs once, from the full entry state (what it
            // assigns is demoted where it is assigned); the catches and finally start from
            // tryEntryFlow, and only what held at entry and nothing assigned survives it.
            java.util.Set<String> entry = s instanceof TryStmt ? em.flowMinusAssigned(pre, s)
                    : em.flowEnterConservative(s, ctx);
            java.util.Set<String> savedTryEntry = tryEntryFlow;
            tryEntryFlow = s instanceof TryStmt ? pre : null;
            try {
                lowerStmt(s, f, ctx);
            } finally {
                tryEntryFlow = savedTryEntry;
            }
            em.flowRetain(entry, ctx);
            return;
        }
        lowerStmt(s, f, ctx);
    }

    private void lowerStmt(Stmt s, Frame f, JavaEmitter.Ctx ctx) {
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
        if (s instanceof VarDeclStmt && ((VarDeclStmt) s).isLate && !hasAwait(s) && !f.stateLocals.contains(s)) {
            // A late local living across states: its LateLocal holder is a frame field,
            // so reads keep their checks and a lazy initializer still runs on first read.
            final VarDeclStmt v = (VarDeclStmt) s;
            em.emitLateLocal(v, ctx, new java.util.function.BiFunction<String, TypeRef, String>() {
                @Override
                public String apply(String jt, TypeRef t) {
                    return declareField(f, ctx, v.name, t, jt);
                }
            });
            return;
        }
        if (s instanceof VarDeclStmt && !hasAwait(s) && f.stateLocals.contains(s)) {
            // Used only before the next state: a Java local, declared as outside a frame,
            // so javac still infers what the Dart-side inference left dynamic.
            em.emitStatement(s, ctx);
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
        if (s instanceof PatternDeclStmt) {
            // AsyncAnf has moved any await of the subject into a temp ahead of it.
            List<Object[]> snap = snapshot(s, f, ctx);
            f.patternFields = true;
            try {
                em.emitStatement(s, ctx);
            } finally {
                f.patternFields = false;
            }
            restore(snap, ctx);
            return;
        }
        if (s instanceof LocalFunc) {
            f.localFunc = (LocalFunc) s;
            List<Object[]> snap = snapshot(s, f, ctx);
            try {
                em.emitStatement(s, ctx);
            } finally {
                f.localFunc = null;
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
            f.targets.add(new Target(f.fins.size(), LABEL, s.labels, exit, -1));
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
                    && unparen(((Assign) unparen(((ExprStmt) s).expr)).lhs) instanceof Ident
                    && ctx.lookup(((Ident) unparen(((Assign) unparen(((ExprStmt) s).expr)).lhs)).name) != null) {
                // `x = await f()` to a local: the local's declared type, as for a declaration
                // (the operand's may name a same-named type of another library).
                TypeRef lt = ctx.lookup(((Ident) unparen(((Assign) unparen(((ExprStmt) s).expr)).lhs)).name);
                expected = em.isDynamicType(lt) ? null : lt;
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
        if (s instanceof IfStmt && ((IfStmt) s).casePattern != null) {
            lowerIfCase((IfStmt) s, f, ctx);
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
            // Null-safety flow promotion, as JavaEmitter's if: each branch sees what the
            // condition proves on its side, and the join keeps what every reaching branch has.
            java.util.Set<String> preFlow = new java.util.HashSet<String>(ctx.nonNull);
            em.flowGuard(i.condition, true, ctx);
            List<Object[]> undo = em.applyGuardPromotions(i.condition, ctx);
            lowerBranch(i.thenStmt, f, ctx);
            em.restorePromotions(undo, ctx);
            java.util.Set<String> thenFlow = em.flowTerminates(i.thenStmt) ? null
                    : new java.util.HashSet<String>(ctx.nonNull);
            ctx.nonNull.clear();
            ctx.nonNull.addAll(preFlow);
            em.flowGuard(i.condition, false, ctx);
            if (els >= 0) {
                jump(f, exit, ctx);
                open(f, els, ctx);
                List<Object[]> neg = new ArrayList<Object[]>();
                em.collectNegativePromotions(i.condition, ctx, neg);
                lowerBranch(i.elseStmt, f, ctx);
                em.restorePromotions(neg, ctx);
            }
            boolean elseLeaves = i.elseStmt != null && em.flowTerminates(i.elseStmt);
            if (elseLeaves && thenFlow != null) {
                ctx.nonNull.clear();
                ctx.nonNull.addAll(thenFlow);
            } else if (thenFlow != null) {
                ctx.nonNull.retainAll(thenFlow);
            } else if (elseLeaves) {
                ctx.nonNull.retainAll(preFlow);
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
            f.targets.add(new Target(f.fins.size(), LOOP, s.labels, exit, head));
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
            if (fs.conditionStmts != null) {
                lowerList(fs.conditionStmts, fs.condition, f, ctx);
            }
            if (fs.condition != null) {
                test(fs.condition, body, exit, f, ctx);
            }
            open(f, body, ctx);
            f.targets.add(new Target(f.fins.size(), LOOP, s.labels, exit, next));
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
            if (fs.updateStmts != null) {
                lowerList(fs.updateStmts, f, ctx);
            }
            jump(f, head, ctx);
            f.loopCaps.remove(f.loopCaps.size() - 1);
            ctx.popScope();
            open(f, exit, ctx);
            return;
        }
        if (s instanceof YieldStmt) {
            YieldStmt y = (YieldStmt) s;
            int k = newState(f);
            if (y.star) {
                JavaEmitter.Out o = em.emitExpr(y.value, null, ctx);
                em.noteYield(o, true, ctx);
                w.line(f.name + ".yieldAll((Object) (" + o.code + "), " + k + ");");
            } else {
                JavaEmitter.Out o = em.emitExpr(y.value, em.isDynamicType(f.starElem) ? null : f.starElem, ctx);
                em.noteYield(o, false, ctx);
                w.line(f.name + ".yield$(" + (f.starJava == null ? "(Object) (" + o.code + ")"
                        : em.coerce(o, f.starElem, ctx)) + ", " + k + ");");
            }
            w.line("return;");
            f.closed = true;
            open(f, k, ctx);
            // Cancelled while suspended here: leave as `return;` does, finally blocks first.
            w.line("if (" + f.name + ".cancelled()) {");
            ctx.indent(1);
            emitReturn(new ReturnStmt(), ctx);
            ctx.indent(-1);
            w.line("}");
            return;
        }
        if (s instanceof ForInStmt && ((ForInStmt) s).isAwait) {
            lowerAwaitFor((ForInStmt) s, f, ctx);
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
            String name = fi.pattern == null ? fi.varName : f.name + "pe" + f.states;
            if (isCell(f, name)) {
                declareCell(f, ctx, name, elem, "(" + jt + ") " + it + ".next()");
            } else {
                String v = declareField(f, ctx, name, elem, jt);
                w.line(v + " = (" + jt + ") " + it + ".next();");
            }
            if (fi.pattern != null) {
                destructureElement(fi, name, f, ctx);
            }
            f.targets.add(new Target(f.fins.size(), LOOP, s.labels, exit, head));
            lowerBranch(fi.body, f, ctx);
            f.targets.remove(f.targets.size() - 1);
            f.loopCaps.remove(f.loopCaps.size() - 1);
            ctx.popScope();
            jump(f, head, ctx);
            open(f, exit, ctx);
            return;
        }
        if (s instanceof SwitchStmt && needsStates((SwitchStmt) s)) {
            lowerSwitchStates((SwitchStmt) s, f, ctx);
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
            f.targets.add(new Target(f.fins.size(), SWITCH, null, exit, -1));
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
        if (s instanceof TryStmt) {
            lowerTry((TryStmt) s, f, ctx);
            return;
        }
        throw new IllegalStateException("not lowerable: " + s.getClass().getSimpleName());
    }

    private static int addHandler(Frame f, String code) {
        f.handlers.add(code);
        return f.handlers.size() - 1;
    }

    private static String field(Frame f, String javaType, String suffix) {
        String n = f.name + suffix;
        f.fields.add(javaType + " " + n + ";");
        return f.name + "." + n;
    }

    /// A try statement holding an await. The try block's states hand a throw to the catch
    /// dispatch, the dispatch's and the catch bodies' to the finally block (as a pending
    /// rethrow), and the finally block's to whatever encloses the statement. Every way out
    /// of the try and catch parts that has a finally block goes through it: falling off
    /// the end, return, break and continue record a pending completion and enter it, and
    /// it ends by carrying that completion out, through the next finally when there is one.
    /// The flow state before the suspending try being lowered (null when none): its catches
    /// start from it minus what the body assigns, its finally minus what body and catches
    /// assign, as the non-lowered try does.
    private java.util.Set<String> tryEntryFlow;

    private void lowerTry(TryStmt t, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        java.util.Set<String> pre = tryEntryFlow;
        tryEntryFlow = null;
        int exit = newState(f);
        int saved = f.handler;
        Fin fin = null;
        int outerHandler = saved;
        if (t.finallyBlock != null) {
            fin = new Fin();
            fin.state = newState(f);
            fin.c = field(f, "int", "c" + fin.state);
            fin.v = field(f, "Object", "v" + fin.state);
            fin.j = field(f, "int", "j" + fin.state);
            outerHandler = addHandler(f, fin.v + " = $T; " + fin.c + " = 2; " + jumpCode(f, fin.state));
        }
        int dispatch = -1;
        String ex = null;
        int tryHandler = outerHandler;
        if (!t.catches.isEmpty()) {
            dispatch = newState(f);
            ex = field(f, "Throwable", "x" + dispatch);
            tryHandler = addHandler(f, ex + " = $T; " + jumpCode(f, dispatch));
        }
        if (fin != null) {
            f.fins.add(fin);
        }
        f.handler = tryHandler;
        open(f, newState(f), ctx);
        lowerBranch(t.tryBlock, f, ctx);
        leaveTry(fin, exit, f, ctx);
        f.handler = outerHandler;
        if (dispatch >= 0) {
            open(f, dispatch, ctx);
            ctx.importClass("dart.runtime.DartRuntime");
            List<Object[]> clauses = new ArrayList<Object[]>();
            boolean catchAll = false;
            for (CatchClause cc : t.catches) {
                Object[] m = em.catchMatch(cc, ex, ctx);
                int k = newState(f);
                clauses.add(new Object[] {Integer.valueOf(k), cc, m});
                if (m[0] == null) {
                    jump(f, k, ctx);
                    catchAll = true;
                    break;
                }
                w.line("if (" + m[0] + ") {");
                w.line("    " + jumpCode(f, k));
                w.line("}");
            }
            if (!catchAll) {
                w.line("throw DartRuntime.rethrow(" + ex + ");");
                f.closed = true;
            }
            for (Object[] c : clauses) {
                CatchClause cc = (CatchClause) c[1];
                Object[] m = (Object[]) c[2];
                open(f, ((Integer) c[0]).intValue(), ctx);
                if (pre != null) {
                    em.flowResetMinusAssigned(pre, t.tryBlock, ctx);
                }
                ctx.pushScope();
                String var = declareField(f, ctx, cc.exceptionVar != null ? cc.exceptionVar : "$e", (TypeRef) m[3],
                        (String) m[1]);
                w.line(var + " = " + m[2] + ";");
                if (cc.stackVar != null) {
                    String st = declareField(f, ctx, cc.stackVar, new TypeRef("StackTrace"), "dart.core.StackTrace");
                    w.line(st + " = dart.core.StackTrace.ofThrown(" + ex + ");");
                }
                String outerRethrow = ctx.rethrowTarget;
                ctx.rethrowTarget = ex;
                try {
                    lowerList(cc.body.statements, f, ctx);
                } finally {
                    ctx.rethrowTarget = outerRethrow;
                }
                ctx.popScope();
                leaveTry(fin, exit, f, ctx);
            }
        }
        f.handler = saved;
        if (fin != null) {
            f.fins.remove(f.fins.size() - 1);
            open(f, fin.state, ctx);
            if (pre != null) {
                java.util.List<Object> ran = new ArrayList<Object>();
                ran.add(t.tryBlock);
                ran.add(t.catches);
                em.flowResetMinusAssigned(pre, ran, ctx);
            }
            ctx.pushScope();
            lowerList(t.finallyBlock.statements, f, ctx);
            ctx.popScope();
            finallyTail(fin, f, ctx);
        }
        open(f, exit, ctx);
    }

    /// `await for (v in s) body` as Dart desugars it: a StreamIterator stepped with
    /// non-blocking awaits on moveNext(), inside a finally that cancels the subscription
    /// when the loop is left early (break, return, a throw) and only then. A loop the
    /// stream ended has nothing to cancel and so no extra suspension, as in Dart.
    private void lowerAwaitFor(ForInStmt fi, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        JavaEmitter.Out iter = em.emitExpr(fi.iterable, null, ctx);
        TypeRef elem = fi.varType != null && !fi.varType.is("var") ? fi.varType
                : iter.type != null && iter.type.is("Stream") && iter.type.arg(0) != null ? iter.type.arg(0)
                : TypeRef.DYNAMIC;
        String itName = f.name + "it" + f.states;
        String it = f.name + "." + itName;
        f.fields.add("dart.async.StreamIterator<Object> " + itName + ";");
        w.line(it + " = new dart.async.StreamIterator<Object>((dart.async.Stream<Object>) (Object) ("
                + iter.code + "));");
        int exit = newState(f);
        int saved = f.handler;
        Fin fin = new Fin();
        fin.state = newState(f);
        fin.c = field(f, "int", "c" + fin.state);
        fin.v = field(f, "Object", "v" + fin.state);
        fin.j = field(f, "int", "j" + fin.state);
        int handler = addHandler(f, fin.v + " = $T; " + fin.c + " = 2; " + jumpCode(f, fin.state));
        f.fins.add(fin);
        f.handler = handler;
        int head = newState(f);
        int done = newState(f);
        open(f, head, ctx);
        int k = newState(f);
        w.line(f.name + ".await(" + it + ".moveNext(), " + k + ");");
        w.line("return;");
        f.closed = true;
        open(f, k, ctx);
        w.line("if (!((java.lang.Boolean) " + f.name + ".value()).booleanValue()) {");
        w.line("    " + jumpCode(f, done));
        w.line("}");
        ctx.pushScope();
        String jt = em.javaType(elem, true, ctx);
        f.loopCaps.add(CaptureScan.lambdaRefs(fi.body));
        String name = fi.pattern == null ? fi.varName : f.name + "pe" + f.states;
        if (isCell(f, name)) {
            declareCell(f, ctx, name, elem, "(" + jt + ") " + it + ".current()");
        } else {
            String v = declareField(f, ctx, name, elem, jt);
            w.line(v + " = (" + jt + ") " + it + ".current();");
        }
        if (fi.pattern != null) {
            destructureElement(fi, name, f, ctx);
        }
        // A break leaves through the finally; continue stays inside it.
        int brk = newState(f);
        f.targets.add(new Target(f.fins.size(), LOOP, fi.labels, brk, head));
        lowerBranch(fi.body, f, ctx);
        f.targets.remove(f.targets.size() - 1);
        f.loopCaps.remove(f.loopCaps.size() - 1);
        ctx.popScope();
        jump(f, head, ctx);
        open(f, brk, ctx);
        w.line(fin.c + " = 0;");
        jump(f, fin.state, ctx);
        // The stream ended: past the loop without the finally (nothing to cancel).
        open(f, done, ctx);
        jump(f, exit, ctx);
        f.handler = saved;
        f.fins.remove(f.fins.size() - 1);
        open(f, fin.state, ctx);
        int resumed = newState(f);
        w.line("if (" + it + ".isActive()) {");
        w.line("    " + f.name + ".await(" + it + ".cancel(), " + resumed + ");");
        w.line("    return;");
        w.line("}");
        int after = newState(f);
        jump(f, after, ctx);
        open(f, resumed, ctx);
        w.line(f.name + ".value();");
        jump(f, after, ctx);
        open(f, after, ctx);
        finallyTail(fin, f, ctx);
        open(f, exit, ctx);
    }

    /// An if-case (`if (e case p when g) S1 else S2`) whose branches hold an await. The
    /// subject, the match and the guard run in one state; the bindings are frame fields
    /// (patternBinding), so the then-branch reads them in whatever state it resumes in.
    /// AsyncAnf has already moved an await of the subject or the guard out of the test.
    /// Whether a switch statement holding an await must be lowered case by case: a guard
    /// awaits, a case binding variables has a body that awaits, or a case is the target of
    /// a labeled continue. The other shape stays a Java switch whose awaiting bodies move
    /// to states of their own.
    private static boolean needsStates(SwitchStmt sw) {
        for (SwitchCase c : sw.cases) {
            if (hasAwait(c.guard) || c.labels != null
                    || hasAwait(c.body) && c.pattern != null && !(c.pattern instanceof ConstantPattern)) {
                return true;
            }
        }
        return false;
    }

    /// A switch statement as states: every case is a test state, tried in order, whose
    /// pattern binds into frame fields and whose guard may suspend; a match jumps to the
    /// case's body state. An empty case shares the body of the next case that has one, as
    /// Dart's fall-through does. `break` leaves for the exit state and `continue l` jumps
    /// to the body of the case labeled `l`. A body that falls off its end leaves the switch.
    private void lowerSwitchStates(SwitchStmt sw, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        int exit = newState(f);
        int n = sw.cases.size();
        int[] tests = new int[n];
        int[] bodies = new int[n];
        for (int i = 0; i < n; i++) {
            tests[i] = newState(f);
            bodies[i] = newState(f);
        }
        int[] target = new int[n];
        int shared = exit;
        for (int i = n - 1; i >= 0; i--) {
            if (!sw.cases.get(i).body.isEmpty()) {
                shared = bodies[i];
            }
            target[i] = shared;
        }
        JavaEmitter.Out subj = em.emitExpr(sw.subject, null, ctx);
        TypeRef st = subj.type != null ? subj.type : TypeRef.DYNAMIC;
        String sf = field(f, em.javaType(st, true, ctx), "s" + exit);
        w.line(sf + " = " + subj.code + ";");
        int pushed = 0;
        for (int i = 0; i < n; i++) {
            SwitchCase c = sw.cases.get(i);
            if (c.labels != null) {
                f.targets.add(new Target(f.fins.size(), LABEL, c.labels, exit, target[i]));
                pushed++;
            }
        }
        f.targets.add(new Target(f.fins.size(), SWITCH, null, exit, -1));
        // A body shared by several cases reads the variables every one of them binds (Dart
        // requires each to bind it with the same type): one frame field per such variable,
        // assigned by whichever case matched, read by the body. sharedNames[i] holds the
        // names for every case of a group of two or more, the case with the body included.
        List<Set<String>> sharedNames = new ArrayList<Set<String>>();
        for (int i = 0; i < n; i++) {
            sharedNames.add(null);
        }
        for (int start = 0; start < n; ) {
            int end = start;
            while (end < n - 1 && sw.cases.get(end).body.isEmpty()) {
                end++;
            }
            if (end > start) {
                Set<String> common = null;
                for (int k = start; k <= end; k++) {
                    Set<String> names = new java.util.LinkedHashSet<String>();
                    if (sw.cases.get(k).pattern != null && !sw.cases.get(k).isDefault) {
                        patternNames(sw.cases.get(k).pattern, names);
                    }
                    if (common == null) {
                        common = names;
                    } else {
                        common.retainAll(names);
                    }
                }
                for (int k = start; k <= end; k++) {
                    sharedNames.set(k, common);
                }
            }
            start = end + 1;
        }
        Map<String, String> sharedRefs = new HashMap<String, String>();
        Map<String, TypeRef> sharedTypes = new HashMap<String, TypeRef>();
        for (int i = 0; i < n; i++) {
            SwitchCase c = sw.cases.get(i);
            int next = i + 1 < n ? tests[i + 1] : exit;
            open(f, tests[i], ctx);
            // A case's variables are visible to its guard and to its own body; a body
            // shared by several cases sees none of them.
            boolean own = !c.body.isEmpty() && (i == 0 || !sw.cases.get(i - 1).body.isEmpty());
            ctx.pushScope();
            if (c.isDefault || c.pattern == null) {
                jump(f, target[i], ctx);
            } else {
                int guard = hasAwait(c.guard) ? newState(f) : -1;
                w.line("{");
                ctx.indent(1);
                List<String> binds = new ArrayList<String>();
                String cond;
                f.patternFields = true;
                try {
                    cond = em.patternTest(c.pattern, sf, st, ctx, binds);
                } finally {
                    f.patternFields = false;
                }
                w.line("if (" + cond + ") {");
                ctx.indent(1);
                for (String b : binds) {
                    w.line(b);
                }
                if (sharedNames.get(i) != null) {
                    for (String name : sharedNames.get(i)) {
                        Ident vi = new Ident();
                        vi.at(c.file, c.line, c.col);
                        vi.name = name;
                        JavaEmitter.Out vo = em.emitExpr(vi, null, ctx);
                        String key = target[i] + "$" + name;
                        String ref = sharedRefs.get(key);
                        if (ref == null) {
                            TypeRef vt = vo.type != null ? vo.type : TypeRef.DYNAMIC;
                            ref = field(f, em.javaType(vt, true, ctx), "sv" + target[i] + "$" + JavaEmitter.javaIdent(name));
                            sharedRefs.put(key, ref);
                            sharedTypes.put(key, vt);
                        }
                        w.line(ref + " = " + vo.code + ";");
                    }
                }
                if (guard >= 0) {
                    w.line(jumpCode(f, guard));
                } else if (c.guard != null) {
                    JavaEmitter.Out g = em.dynCondition(em.emitExpr(c.guard, TypeRef.BOOL, ctx), ctx);
                    w.line("if (" + g.code + ") {");
                    w.line("    " + jumpCode(f, target[i]));
                    w.line("}");
                } else {
                    w.line(jumpCode(f, target[i]));
                }
                ctx.indent(-1);
                w.line("}");
                ctx.indent(-1);
                w.line("}");
                w.line(jumpCode(f, next));
                f.closed = true;
                if (guard >= 0) {
                    open(f, guard, ctx);
                    VarDeclStmt g = new VarDeclStmt();
                    g.at(c.guard.file, c.guard.line, c.guard.col);
                    g.name = "$guard" + guard;
                    g.type = TypeRef.BOOL;
                    g.isFinal = true;
                    g.initializer = c.guard;
                    lowerList(f.anf.normalizeStatements(Collections.<Stmt>singletonList(g)), f, ctx);
                    Ident gi = new Ident();
                    gi.at(c.guard.file, c.guard.line, c.guard.col);
                    gi.name = g.name;
                    test(gi, target[i], next, f, ctx);
                }
            }
            if (!own) {
                ctx.popScope();
                ctx.pushScope();
                if (!c.body.isEmpty() && sharedNames.get(i) != null) {
                    for (String name : sharedNames.get(i)) {
                        String key = target[i] + "$" + name;
                        if (sharedRefs.containsKey(key)) {
                            ctx.declareAs(name, sharedTypes.get(key), sharedRefs.get(key));
                        }
                    }
                }
            }
            if (!c.body.isEmpty()) {
                open(f, bodies[i], ctx);
                lowerList(c.body, f, ctx);
                jump(f, exit, ctx);
            }
            ctx.popScope();
        }
        f.targets.remove(f.targets.size() - 1);
        for (int k = 0; k < pushed; k++) {
            f.targets.remove(f.targets.size() - 1);
        }
        open(f, exit, ctx);
    }

    /// The variables a pattern binds (wildcards aside).
    private static void patternNames(Object n, Set<String> out) {
        if (n instanceof VariablePattern) {
            VariablePattern v = (VariablePattern) n;
            if (!v.wildcard && v.name != null && !"_".equals(v.name)) {
                out.add(v.name);
            }
        }
        if (n instanceof List) {
            for (Object x : (List<?>) n) {
                patternNames(x, out);
            }
            return;
        }
        if (n instanceof PatternField) {
            patternNames(((PatternField) n).pattern, out);
            return;
        }
        if (!(n instanceof Pattern)) {
            return;
        }
        for (Field fl : nodeFields(n.getClass())) {
            try {
                patternNames(fl.get(n), out);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void lowerIfCase(IfStmt i, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        int then = newState(f);
        int els = i.elseStmt != null ? newState(f) : -1;
        int exit = newState(f);
        ctx.pushScope();
        w.line("{");
        ctx.indent(1);
        JavaEmitter.Out subj = em.emitExpr(i.condition, null, ctx);
        String temp = ctx.newTemp();
        w.line(em.javaType(subj.type, true, ctx) + " " + temp + " = " + subj.code + ";");
        ctx.declare(temp, subj.type);
        List<String> binds = new ArrayList<String>();
        String cond;
        f.patternFields = true;
        try {
            cond = em.patternTest(i.casePattern, temp, subj.type, ctx, binds);
        } finally {
            f.patternFields = false;
        }
        w.line("if (" + cond + ") {");
        ctx.indent(1);
        for (String b : binds) {
            w.line(b);
        }
        if (i.caseGuard != null) {
            JavaEmitter.Out g = em.dynCondition(em.emitExpr(i.caseGuard, TypeRef.BOOL, ctx), ctx);
            w.line("if (" + g.code + ") {");
            w.line("    " + jumpCode(f, then));
            w.line("}");
        } else {
            w.line(jumpCode(f, then));
        }
        ctx.indent(-1);
        w.line("}");
        ctx.indent(-1);
        w.line("}");
        w.line(jumpCode(f, els >= 0 ? els : exit));
        f.closed = true;
        open(f, then, ctx);
        lowerBranch(i.thenStmt, f, ctx);
        ctx.popScope();
        if (els >= 0) {
            jump(f, exit, ctx);
            open(f, els, ctx);
            lowerBranch(i.elseStmt, f, ctx);
        }
        open(f, exit, ctx);
    }

    /// A pattern for-in (`for (var (a, b) in xs)`): the element went into the frame
    /// field {@code name}, and the pattern is then bound from it exactly as a pattern
    /// declaration is, so every binding is a frame field (patternBinding).
    private void destructureElement(ForInStmt fi, String name, Frame f, JavaEmitter.Ctx ctx) {
        Ident ref = new Ident();
        ref.file = fi.file;
        ref.line = fi.line;
        ref.col = fi.col;
        ref.name = name;
        PatternDeclStmt decl = new PatternDeclStmt();
        decl.file = fi.file;
        decl.line = fi.line;
        decl.col = fi.col;
        decl.pattern = fi.pattern;
        decl.init = ref;
        lower(decl, f, ctx);
    }

    /// After a finally block: the pending completion it interrupted carries on.
    private static void finallyTail(Fin fin, Frame f, JavaEmitter.Ctx ctx) {
        JavaEmitter.Ctx.Writer w = ctx.writer();
        if (!f.closed) {
            // A throw in the finally block replaced the pending completion; else it runs.
            w.line("switch (" + fin.c + ") {");
            w.line("    case 1: {");
            ctx.indent(2);
            if (f.fins.isEmpty()) {
                w.line(f.isVoid ? f.name + ".complete(null);" : f.name + ".resolve(" + fin.v + ");");
                w.line("return;");
            } else {
                Fin out = f.fins.get(f.fins.size() - 1);
                w.line(out.v + " = " + fin.v + "; " + out.c + " = 1; " + jumpCode(f, out.state));
            }
            ctx.indent(-2);
            w.line("    }");
            w.line("    case 2:");
            w.line("        throw (Throwable) " + fin.v + ";");
            if (!fin.jumps.isEmpty()) {
                w.line("    case 3:");
                w.line("        switch (" + fin.j + ") {");
                Set<Integer> seen = new HashSet<Integer>();
                for (int[] jmp : new ArrayList<int[]>(fin.jumps)) {
                    if (!seen.add(Integer.valueOf(jmp[0]))) {
                        continue;
                    }
                    w.line("            case " + jmp[0] + ":");
                    w.line("                " + leave(f, jmp[1], jmp[0]));
                }
                w.line("            default:");
                w.line("                throw new IllegalStateException(\"finally jump \" + " + fin.j + ");");
                w.line("        }");
            }
            w.line("    default:");
            w.line("        break;");
            w.line("}");
        }
    }

    /// The end of a try block or a catch body: to the finally block, else past the try.
    private static void leaveTry(Fin fin, int exit, Frame f, JavaEmitter.Ctx ctx) {
        if (f.closed) {
            return;
        }
        if (fin != null) {
            ctx.writer().line(fin.c + " = 0;");
            jump(f, fin.state, ctx);
        } else {
            jump(f, exit, ctx);
        }
    }

    /// A jump to state {@code k}, enclosed by {@code depth} finally blocks, from where the
    /// lowering is: through the innermost finally block it leaves, else straight there.
    private static String leave(Frame f, int depth, int k) {
        if (f.fins.size() > depth) {
            Fin fin = f.fins.get(f.fins.size() - 1);
            fin.jumps.add(new int[] {k, depth});
            return fin.c + " = 3; " + fin.j + " = " + k + "; " + jumpCode(f, fin.state);
        }
        return jumpCode(f, k);
    }

    private static String escape(Frame f, Target t, int k) {
        return leave(f, t.fins, k);
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
        if (ot != null && !ot.is("Future") && !ot.is("FutureOr")) {
            // A program class implementing Future<T> awaits to a T, as Future<T> does.
            TypeRef sup = em.asSuper(ot, "Future", 0);
            if (sup != null && sup.is("Future")) {
                ot = sup;
            }
        }
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
            return new JavaEmitter.Out("dart.core.Dart" + pk + "List.as" + pk + "s(" + v + ")", staticType(inner, ot == null || ot.nullable || !ot.is("Future") || f.resumeExpected != null));
        }
        return new JavaEmitter.Out("((" + em.javaType(boxed, true, ctx) + ") " + v + ")", staticType(inner, ot == null || ot.nullable || !ot.is("Future") || f.resumeExpected != null));
    }

    /// The static type of a resumed `await`: the boxed Java type the value is cast to, but
    /// with the Dart nullability of what the Future completes with -- `await f(1)` of a
    /// `Future<int>` is an `int`, and a literal holding it is a `List<int>`, not `List<int?>`.
    /// Nullable as before whenever the awaited operand may itself be null (`Future<int>?`),
    /// is not a plain Future, or the slot's declared type stands in for it.
    private TypeRef staticType(TypeRef inner, boolean keepBoxed) {
        TypeRef t = em.boxType(inner);
        t.nullable = keepBoxed || inner.nullable;
        return t;
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
        if (init != null && jt.indexOf('<') > 0 && !(v.initializer instanceof Lambda)
                && (v.type == null || v.type.is("var")) && em.containsDynamic(t)) {
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

    /// One binding of a destructuring in a lowered body: the variable is a frame field
    /// (a cell when a closure of a split loop captures it), and the answer is the Java
    /// statement that assigns it, emitted where the pattern's other bindings are.
    String patternBinding(String name, TypeRef t, String jt, String code, JavaEmitter.Ctx ctx) {
        Frame f = ctx.frame;
        if (isCell(f, name)) {
            String holder = em.refHolder(t, ctx);
            String raw = holder.startsWith("Ref<") ? "Ref<>" : holder;
            String ref = declareField(f, ctx, name, t, holder);
            ctx.markBoxed(ref);
            f.cells.put(name, new Object[] {ref, t, raw});
            return ref + " = new " + raw + "(" + code + ");";
        }
        return declareField(f, ctx, name, t, jt) + " = " + code + ";";
    }

    /// `return` in a lowered body: completes the frame's Future and leaves the step.
    void emitReturn(ReturnStmt r, JavaEmitter.Ctx ctx) {
        Frame f = ctx.frame;
        JavaEmitter.Ctx.Writer w = ctx.writer();
        String value = null;
        if (r.value != null && (f.isVoid || f.valueType.is("void") && unparen(r.value) == f.resumeAwait)) {
            // `return await x;` (`=> await x`) in a Future<void> body: the awaited value is
            // dropped, and a resumed void value is no Java statement of its own.
            em.emitDiscardedValue(r.value, ctx);
        } else if (r.value != null) {
            TypeRef inner = f.valueType;
            JavaEmitter.Out o = em.emitExpr(r.value, em.isDynamicType(inner) ? null : inner, ctx);
            if (ctx.asyncLambdaReturns != null) {
                // What an async closure's Future carries, for its inferred signature.
                ctx.asyncLambdaReturns.add(o.type);
            }
            if (o.type != null && o.type.is("void")) {
                w.line(o.code + ";");
            } else {
                boolean future = o.type != null && (o.type.is("Future") || o.type.is("FutureOr"))
                        || em.isDynamicType(inner) || em.isDynamicType(o.type) || inner.is("void");
                value = "(Object) (" + (future ? o.code : em.coerce(o, inner, ctx)) + ")";
            }
        }
        if (!f.fins.isEmpty()) {
            // Leaving a try that has a finally block: the block runs first, then returns.
            Fin fin = f.fins.get(f.fins.size() - 1);
            w.line(fin.v + " = " + (value == null ? "null" : value) + ";");
            w.line(fin.c + " = 1;");
            w.line(jumpCode(f, fin.state));
            return;
        }
        w.line(value == null ? f.name + ".complete(null);" : f.name + ".resolve(" + value + ");");
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
                    f.jumps.put(b, escape(f, t, t.brk));
                }
            }
        } else if (s instanceof ContinueStmt) {
            ContinueStmt c = (ContinueStmt) s;
            if (c.label == null ? loops == 0 : !labels.contains(c.label)) {
                Target t = find(f, c.label, true);
                if (t != null) {
                    f.jumps.put(c, escape(f, t, t.cont));
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
