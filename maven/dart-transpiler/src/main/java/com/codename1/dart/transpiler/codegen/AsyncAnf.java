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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A-normal form for an async body, ahead of AsyncLowering: every await nested in an
/// expression is hoisted into a local of its own (`$anfN`), declared just before the
/// statement, so that each await left is one of the statement-level shapes the lowering
/// suspends on. Evaluation order is kept: an operand evaluated before an await is
/// evaluated into a local before it, an operand after it stays where it was.
/// `&&`, `||`, `??`, `?:`, `?.`, `??=` and null-aware cascades become if statements, so
/// a right-hand side runs only when Dart would run it. Conditions of while, do and for
/// that await become `while (true)` loops that test at the top (do and for through a
/// first-pass flag, so `continue` still reaches the test and the update); a for loop whose
/// header awaits stays a for loop, its condition and updates as statements
/// (ForStmt.conditionStmts / updateStmts), so each pass keeps its own loop variable.
/// `a?.b.c(await x)` skips the whole chain, the await included, when `a` is null.
///
/// The pass reads the AST only and never changes it: what it rewrites is copied, and a
/// statement it does not handle is returned as it was, for lowerable() to judge. Shapes
/// left alone: an await inside a collection if/for element (or after one), a null-aware
/// assignment target, a switch expression, a record literal, a `when` guard, an if-case
/// subject and `await for`. Arguments are evaluated in source order, named ones among
/// the positional. A receiver is copied ahead of a later await unless reading it
/// evaluates nothing: `this`, a type or an import prefix (JavaEmitter.namesNoValue).
final class AsyncAnf {
    private final Set<String> locals = new HashSet<String>();
    private final Set<String> tempNames = new HashSet<String>();
    private int temps;
    /// Whether a name, unqualified, reads no value: a type or an import prefix.
    private final java.util.function.Predicate<String> noValue;

    AsyncAnf(List<Param> params, Block body, java.util.function.Predicate<String> noValue) {
        this.noValue = noValue;
        for (Param p : params) {
            locals.add(p.name);
        }
        collectLocals(body);
    }

    private void collectLocals(Object n) {
        if (n instanceof List) {
            for (Object o : (List<?>) n) {
                collectLocals(o);
            }
            return;
        }
        if (!(n instanceof Node) || n instanceof Lambda || n instanceof LocalFunc) {
            return;
        }
        if (n instanceof VarDeclStmt) {
            locals.add(((VarDeclStmt) n).name);
        } else if (n instanceof ForInStmt && ((ForInStmt) n).varName != null) {
            locals.add(((ForInStmt) n).varName);
        }
        for (Field f : fields(n.getClass())) {
            collectLocals(get(f, n));
        }
    }

    Block normalize(Block b) {
        Block c = copy(b);
        c.statements = list(b.statements);
        return c;
    }

    // ------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------

    private List<Stmt> list(List<Stmt> in) {
        List<Stmt> out = new ArrayList<Stmt>();
        for (Stmt s : in) {
            stmt(s, out);
        }
        return out;
    }

    private void stmt(Stmt s, List<Stmt> out) {
        if (s == null || !AsyncLowering.hasAwait(s)) {
            out.add(s);
            return;
        }
        List<Stmt> pre = new ArrayList<Stmt>();
        Stmt r = rewrite(s, pre);
        if (r == null) {
            out.add(s);
            return;
        }
        out.addAll(pre);
        out.add(r);
    }

    private Stmt branch(Stmt b) {
        if (b == null || !AsyncLowering.hasAwait(b)) {
            return b;
        }
        List<Stmt> l = new ArrayList<Stmt>();
        stmt(b, l);
        return l.size() == 1 ? l.get(0) : block(l, b);
    }

    private Stmt rewrite(Stmt s, List<Stmt> pre) {
        if (s instanceof Block) {
            return normalize((Block) s);
        }
        if (s instanceof ExprStmt) {
            if (AsyncLowering.statementAwait(s) != null) {
                return s;
            }
            Expr whole = ((ExprStmt) s).expr;
            while (whole instanceof ParenExpr) {
                whole = ((ParenExpr) whole).inner;
            }
            Expr e = whole instanceof Assign && "=".equals(((Assign) whole).op)
                    && unparenAwait(((Assign) whole).rhs) ? awaitedStore((Assign) whole, pre)
                    : expr(((ExprStmt) s).expr, pre);
            if (e == null) {
                return null;
            }
            if (isTemp(e)) {
                return block(new ArrayList<Stmt>(), s);
            }
            ExprStmt c = copy((ExprStmt) s);
            c.expr = e;
            return c;
        }
        if (s instanceof VarDeclStmt) {
            VarDeclStmt v = (VarDeclStmt) s;
            if (v.isLate) {
                return null;
            }
            if (AsyncLowering.statementAwait(s) != null) {
                return s;
            }
            Expr e = expr(v.initializer, pre);
            if (e == null) {
                return null;
            }
            VarDeclStmt c = copy(v);
            c.initializer = e;
            return c;
        }
        if (s instanceof VarDeclGroup) {
            for (VarDeclStmt d : ((VarDeclGroup) s).decls) {
                if (d.isLate && AsyncLowering.hasAwait(d)) {
                    return null;
                }
                stmt(d, pre);
            }
            return block(new ArrayList<Stmt>(), s);
        }
        if (s instanceof YieldStmt) {
            // An async* yield is a suspension point of its own: its value is computed,
            // awaits and all, before the yield.
            YieldStmt y = (YieldStmt) s;
            Expr e = expr(y.value, pre);
            if (e == null) {
                return null;
            }
            if (AsyncLowering.hasAwait(e)) {
                e = force(e, pre, s);
            }
            YieldStmt c = copy(y);
            c.value = e;
            return c;
        }
        if (s instanceof ReturnStmt) {
            if (AsyncLowering.statementAwait(s) != null) {
                return s;
            }
            Expr e = expr(((ReturnStmt) s).value, pre);
            if (e == null) {
                return null;
            }
            ReturnStmt c = copy((ReturnStmt) s);
            c.value = e;
            return c;
        }
        if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            IfStmt c = copy(i);
            if (i.casePattern != null) {
                if (AsyncLowering.hasAwait(i.condition) || AsyncLowering.hasAwait(i.caseGuard)) {
                    return null;
                }
            } else {
                c.condition = expr(i.condition, pre);
                if (c.condition == null) {
                    return null;
                }
            }
            c.thenStmt = branch(i.thenStmt);
            c.elseStmt = branch(i.elseStmt);
            return c;
        }
        if (s instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) s;
            WhileStmt c = copy(w);
            if (!AsyncLowering.hasAwait(w.condition)) {
                c.body = branch(w.body);
                return c;
            }
            c.isDo = false;
            c.condition = bool(true, w);
            List<Stmt> body = new ArrayList<Stmt>();
            String first = null;
            List<Stmt> test = body;
            if (w.isDo) {
                first = temp();
                pre.add(decl(first, TypeRef.of("bool"), bool(false, w), w));
                test = new ArrayList<Stmt>();
            }
            Expr ce = expr(w.condition, test);
            if (ce == null) {
                return null;
            }
            test.add(breakUnless(ce, w));
            if (w.isDo) {
                body.add(ifStmt(ident(first, w), block(test, w), null, w));
                body.add(assign(ident(first, w), bool(true, w), w));
            }
            body.add(blockOf(branch(w.body), w));
            c.body = block(body, w);
            return c;
        }
        if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            boolean header = f.init != null && AsyncLowering.hasAwait(f.init)
                    || AsyncLowering.hasAwait(f.condition) || AsyncLowering.hasAwait(f.updates);
            if (!header) {
                ForStmt c = copy(f);
                c.body = branch(f.body);
                return c;
            }
            // The loop stays a for loop, so every pass has its own loop variable; the
            // lowering runs conditionStmts at the head and updateStmts after the pass.
            List<Stmt> outer = new ArrayList<Stmt>();
            ForStmt c = copy(f);
            if (f.init != null && AsyncLowering.hasAwait(f.init)) {
                List<Stmt> l = new ArrayList<Stmt>();
                stmt(f.init, l);
                c.init = l.remove(l.size() - 1);
                outer.addAll(l);
            }
            if (AsyncLowering.hasAwait(f.condition)) {
                List<Stmt> cp = new ArrayList<Stmt>();
                c.condition = expr(f.condition, cp);
                if (c.condition == null) {
                    return null;
                }
                c.conditionStmts = cp;
            }
            if (AsyncLowering.hasAwait(f.updates)) {
                List<Stmt> up = new ArrayList<Stmt>();
                for (Expr u : f.updates) {
                    stmt(exprStmt(u, f), up);
                }
                c.updates = new ArrayList<Expr>();
                c.updateStmts = up;
            }
            c.body = branch(f.body);
            if (outer.isEmpty()) {
                return c;
            }
            outer.add(c);
            return block(outer, f);
        }
        if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            if (f.isAwait) {
                return null;
            }
            ForInStmt c = copy(f);
            c.iterable = expr(f.iterable, pre);
            if (c.iterable == null) {
                return null;
            }
            c.body = branch(f.body);
            return c;
        }
        if (s instanceof TryStmt) {
            TryStmt t = (TryStmt) s;
            TryStmt c = copy(t);
            c.tryBlock = normalize(t.tryBlock);
            c.catches = new ArrayList<CatchClause>();
            for (CatchClause k : t.catches) {
                CatchClause kc = copy(k);
                kc.body = normalize(k.body);
                c.catches.add(kc);
            }
            c.finallyBlock = t.finallyBlock == null ? null : normalize(t.finallyBlock);
            return c;
        }
        if (s instanceof SwitchStmt) {
            SwitchStmt sw = (SwitchStmt) s;
            SwitchStmt c = copy(sw);
            c.subject = expr(sw.subject, pre);
            if (c.subject == null) {
                return null;
            }
            c.cases = new ArrayList<SwitchCase>();
            for (SwitchCase k : sw.cases) {
                if (AsyncLowering.hasAwait(k.guard)) {
                    return null;
                }
                SwitchCase kc = copy(k);
                kc.body = list(k.body);
                c.cases.add(kc);
            }
            return c;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Expressions: the result holds no await, `pre` holds what runs first
    // ------------------------------------------------------------------

    private Expr expr(Expr e, List<Stmt> pre) {
        if (e == null || !AsyncLowering.hasAwait(e)) {
            return e;
        }
        if (e instanceof ParenExpr) {
            ParenExpr c = copy((ParenExpr) e);
            c.inner = expr(c.inner, pre);
            return c.inner == null ? null : c;
        }
        if (e instanceof AwaitExpr) {
            AwaitExpr c = copy((AwaitExpr) e);
            c.operand = expr(c.operand, pre);
            if (c.operand == null) {
                return null;
            }
            String t = temp();
            pre.add(decl(t, null, c, e));
            return ident(t, e);
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            boolean lazy = "&&".equals(b.op) || "||".equals(b.op) || "??".equals(b.op);
            if (lazy && AsyncLowering.hasAwait(b.right)) {
                Expr l = expr(b.left, pre);
                if (l == null) {
                    return null;
                }
                String t = temp();
                pre.add(decl(t, TypeRef.of("??".equals(b.op) ? "dynamic" : "bool"), l, e));
                List<Stmt> inner = new ArrayList<Stmt>();
                Expr r = expr(b.right, inner);
                if (r == null) {
                    return null;
                }
                inner.add(assign(ident(t, e), r, e));
                Expr test = "&&".equals(b.op) ? ident(t, e)
                        : "||".equals(b.op) ? not(ident(t, e), e) : binary(ident(t, e), "==", nul(e), e);
                pre.add(ifStmt(test, block(inner, e), null, e));
                return ident(t, e);
            }
            Expr[] k = kids(pre, null, b.left, b.right);
            if (k == null) {
                return null;
            }
            Binary c = copy(b);
            c.left = k[0];
            c.right = k[1];
            return c;
        }
        if (e instanceof Conditional) {
            Conditional q = (Conditional) e;
            Expr cond = expr(q.condition, pre);
            if (cond == null) {
                return null;
            }
            if (!AsyncLowering.hasAwait(q.thenExpr) && !AsyncLowering.hasAwait(q.elseExpr)) {
                Conditional c = copy(q);
                c.condition = cond;
                return c;
            }
            String t = temp();
            pre.add(decl(t, TypeRef.of("dynamic"), null, e));
            List<Stmt> a = new ArrayList<Stmt>();
            Expr ta = expr(q.thenExpr, a);
            List<Stmt> b = new ArrayList<Stmt>();
            Expr tb = expr(q.elseExpr, b);
            if (ta == null || tb == null) {
                return null;
            }
            a.add(assign(ident(t, e), ta, e));
            b.add(assign(ident(t, e), tb, e));
            pre.add(ifStmt(cond, block(a, e), block(b, e), e));
            return ident(t, e);
        }
        Expr link = shortLink(e);
        if (link != null) {
            return shorted(e, link, pre);
        }
        if (e instanceof Call) {
            return call((Call) e, pre);
        }
        if (e instanceof CtorCall) {
            CtorCall c = copy((CtorCall) e);
            c.args = args(c.args, pre, new ArrayList<Expr>(), false);
            return c.args == null ? null : c;
        }
        if (e instanceof IndexGet) {
            IndexGet g = (IndexGet) e;
            Expr[] k = kids(pre, new boolean[] {staticRef(g.target), false}, g.target, g.index);
            if (k == null) {
                return null;
            }
            IndexGet c = copy(g);
            c.target = k[0];
            c.index = k[1];
            return c;
        }
        if (e instanceof PropertyGet) {
            PropertyGet c = copy((PropertyGet) e);
            c.target = expr(c.target, pre);
            return c.target == null ? null : c;
        }
        if (e instanceof Unary) {
            Unary c = copy((Unary) e);
            c.operand = expr(c.operand, pre);
            return c.operand == null ? null : c;
        }
        if (e instanceof NotNullAssert) {
            NotNullAssert c = copy((NotNullAssert) e);
            c.operand = expr(c.operand, pre);
            return c.operand == null ? null : c;
        }
        if (e instanceof IsTest) {
            IsTest c = copy((IsTest) e);
            c.operand = expr(c.operand, pre);
            return c.operand == null ? null : c;
        }
        if (e instanceof AsCast) {
            AsCast c = copy((AsCast) e);
            c.operand = expr(c.operand, pre);
            return c.operand == null ? null : c;
        }
        if (e instanceof ThrowExpr) {
            ThrowExpr c = copy((ThrowExpr) e);
            c.value = expr(c.value, pre);
            return c.value == null ? null : c;
        }
        if (e instanceof IncDec) {
            IncDec c = copy((IncDec) e);
            c.operand = expr(c.operand, pre);
            return c.operand == null ? null : c;
        }
        if (e instanceof StringLit) {
            StringLit s = (StringLit) e;
            List<Expr> parts = new ArrayList<Expr>();
            for (Object o : s.parts) {
                if (o instanceof Expr) {
                    parts.add((Expr) o);
                }
            }
            Expr[] k = kids(pre, null, parts.toArray(new Expr[0]));
            if (k == null) {
                return null;
            }
            StringLit c = copy(s);
            c.parts = new ArrayList<Object>();
            int j = 0;
            for (Object o : s.parts) {
                c.parts.add(o instanceof Expr ? k[j++] : o);
            }
            return c;
        }
        if (e instanceof ListLit) {
            ListLit c = copy((ListLit) e);
            c.elements = elements(c.elements, pre);
            return c.elements == null ? null : c;
        }
        if (e instanceof SetLit) {
            SetLit c = copy((SetLit) e);
            c.elements = elements(c.elements, pre);
            return c.elements == null ? null : c;
        }
        if (e instanceof MapLit) {
            return map((MapLit) e, pre);
        }
        if (e instanceof Assign) {
            return assignExpr((Assign) e, pre);
        }
        if (e instanceof Cascade) {
            return cascade((Cascade) e, pre);
        }
        return null;
    }

    private Expr call(Call call, List<Stmt> pre) {
        boolean argsAwait = false;
        for (Expr a : call.args.positional) {
            argsAwait |= AsyncLowering.hasAwait(a);
        }
        for (NamedArg a : call.args.named) {
            argsAwait |= AsyncLowering.hasAwait(a.value);
        }
        if (call.nullAware && argsAwait) {
            // `r?.m(await x)`: the arguments run only when the receiver is not null.
            Expr tg = expr(call.target, pre);
            if (tg == null) {
                return null;
            }
            String r = temp();
            pre.add(decl(r, null, tg, call));
            String t = temp();
            pre.add(decl(t, TypeRef.of("dynamic"), null, call));
            Call c = copy(call);
            c.nullAware = false;
            NotNullAssert nn = new NotNullAssert();
            nn.at(call.file, call.line, call.col);
            nn.operand = ident(r, call);
            c.target = nn;
            List<Stmt> inner = new ArrayList<Stmt>();
            Expr v = expr(c, inner);
            if (v == null) {
                return null;
            }
            inner.add(assign(ident(t, call), v, call));
            pre.add(ifStmt(binary(ident(r, call), "!=", nul(call), call), block(inner, call), null, call));
            return ident(t, call);
        }
        List<Expr> lead = new ArrayList<Expr>();
        boolean keepTarget = call.target == null || staticRef(call.target);
        if (call.target != null) {
            lead.add(call.target);
        }
        Call c = copy(call);
        List<Expr> leadOut = new ArrayList<Expr>();
        c.args = args(call.args, pre, lead, keepTarget, leadOut);
        if (c.args == null) {
            return null;
        }
        if (call.target != null) {
            c.target = leadOut.get(0);
        }
        return c;
    }

    // ------------------------------------------------------------------
    // Null-shorting: `a?.b.c(await x)` skips the whole chain, the await too
    // ------------------------------------------------------------------

    private static Expr receiver(Expr e) {
        if (e instanceof Call) {
            return ((Call) e).target;
        }
        if (e instanceof PropertyGet) {
            return ((PropertyGet) e).target;
        }
        if (e instanceof IndexGet) {
            return ((IndexGet) e).target;
        }
        if (e instanceof NotNullAssert) {
            // `a?.b!.c()`: a `!` is a selector of the chain the null shorts.
            return ((NotNullAssert) e).operand;
        }
        return null;
    }

    private static boolean nullAwareLink(Expr e) {
        return e instanceof Call && ((Call) e).nullAware && ((Call) e).target != null
                || e instanceof PropertyGet && ((PropertyGet) e).nullAware;
    }

    /// Whether the links of {@code e}'s selector chain from {@code e} down to {@code x},
    /// x's receiver excluded, await.
    private static boolean awaitAbove(Expr e, Expr x) {
        boolean own = e instanceof Call && AsyncLowering.hasAwait(((Call) e).args)
                || e instanceof IndexGet && AsyncLowering.hasAwait(((IndexGet) e).index);
        if (e == x) {
            return own;
        }
        return own || awaitAbove(receiver(e), x);
    }

    /// The null-aware link below {@code e} in its selector chain whose null skips an await
    /// after it, else null. A null-aware call that is {@code e} itself is call()'s.
    private static Expr shortLink(Expr e) {
        for (Expr x = receiver(e); x != null; x = receiver(x)) {
            if (nullAwareLink(x) && awaitAbove(e, x)) {
                return x;
            }
        }
        return null;
    }

    private Expr replaceLink(Expr e, Expr x, Expr rep) {
        if (e == x) {
            return rep;
        }
        if (e instanceof Call) {
            Call c = copy((Call) e);
            c.target = replaceLink(c.target, x, rep);
            return c;
        }
        if (e instanceof PropertyGet) {
            PropertyGet c = copy((PropertyGet) e);
            c.target = replaceLink(c.target, x, rep);
            return c;
        }
        if (e instanceof NotNullAssert) {
            NotNullAssert c = copy((NotNullAssert) e);
            c.operand = replaceLink(c.operand, x, rep);
            return c;
        }
        IndexGet c = copy((IndexGet) e);
        c.target = replaceLink(c.target, x, rep);
        return c;
    }

    private Expr shorted(Expr e, Expr x, List<Stmt> pre) {
        Expr tg = expr(receiver(x), pre);
        if (tg == null) {
            return null;
        }
        String r = temp();
        pre.add(decl(r, null, tg, e));
        String t = temp();
        pre.add(decl(t, TypeRef.of("dynamic"), null, e));
        NotNullAssert nn = new NotNullAssert();
        nn.at(e.file, e.line, e.col);
        nn.operand = ident(r, e);
        Expr xc;
        if (x instanceof Call) {
            Call c = copy((Call) x);
            c.nullAware = false;
            c.target = nn;
            xc = c;
        } else {
            PropertyGet c = copy((PropertyGet) x);
            c.nullAware = false;
            c.target = nn;
            xc = c;
        }
        List<Stmt> inner = new ArrayList<Stmt>();
        Expr v = expr(replaceLink(e, x, xc), inner);
        if (v == null) {
            return null;
        }
        inner.add(assign(ident(t, e), v, e));
        pre.add(ifStmt(binary(ident(r, e), "!=", nul(e), e), block(inner, e), null, e));
        return ident(t, e);
    }

    private Args args(Args a, List<Stmt> pre, List<Expr> lead, boolean keepLead) {
        return args(a, pre, lead, keepLead, new ArrayList<Expr>());
    }

    /// The arguments, after the receiver(s) in {@code lead}, whose results go to {@code leadOut}.
    private Args args(Args a, List<Stmt> pre, List<Expr> lead, boolean keepLead, List<Expr> leadOut) {
        List<Expr> all = new ArrayList<Expr>(lead);
        all.addAll(a.positional);
        for (NamedArg n : a.named) {
            all.add(n.value);
        }
        // Dart evaluates the arguments in source order, named ones among the positional.
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < all.size(); i++) {
            order.add(Integer.valueOf(i));
        }
        final List<Expr> src = all;
        final int leads = lead.size();
        Collections.sort(order.subList(leads, order.size()), new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer x, Integer y) {
                Expr ex = src.get(x.intValue());
                Expr ey = src.get(y.intValue());
                if (ex == null || ey == null || ex.line != ey.line) {
                    return ex == null || ey == null ? 0 : Integer.compare(ex.line, ey.line);
                }
                return Integer.compare(ex.col, ey.col);
            }
        });
        Expr[] sorted = new Expr[all.size()];
        boolean[] keep = new boolean[all.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = all.get(order.get(i).intValue());
            keep[i] = i < leads && keepLead;
        }
        Expr[] ks = kids(pre, keep, sorted);
        if (ks == null) {
            return null;
        }
        Expr[] k = new Expr[ks.length];
        for (int i = 0; i < ks.length; i++) {
            k[order.get(i).intValue()] = ks[i];
        }
        int j = 0;
        for (int i = 0; i < lead.size(); i++) {
            leadOut.add(k[j++]);
        }
        Args out = new Args();
        for (int i = 0; i < a.positional.size(); i++) {
            out.positional.add(k[j++]);
        }
        for (NamedArg n : a.named) {
            NamedArg nc = new NamedArg();
            nc.name = n.name;
            nc.value = k[j++];
            out.named.add(nc);
        }
        return out;
    }

    private List<Expr> elements(List<Expr> els, List<Stmt> pre) {
        int last = -1;
        for (int i = 0; i < els.size(); i++) {
            if (AsyncLowering.hasAwait(els.get(i))) {
                last = i;
            }
        }
        List<Expr> vals = new ArrayList<Expr>();
        for (int i = 0; i < els.size(); i++) {
            Expr x = els.get(i);
            if (x instanceof IfElement || x instanceof ForElement) {
                if (i <= last) {
                    return null;
                }
                vals.add(null);
            } else if (x instanceof SpreadElement) {
                vals.add(((SpreadElement) x).expr);
            } else if (x instanceof MapEntry) {
                return null;
            } else {
                vals.add(x);
            }
        }
        Expr[] k = kids(pre, null, vals.toArray(new Expr[0]));
        if (k == null) {
            return null;
        }
        List<Expr> out = new ArrayList<Expr>();
        for (int i = 0; i < els.size(); i++) {
            Expr x = els.get(i);
            if (x instanceof SpreadElement) {
                SpreadElement sc = copy((SpreadElement) x);
                sc.expr = k[i];
                out.add(sc);
            } else if (x instanceof IfElement || x instanceof ForElement) {
                out.add(x);
            } else {
                out.add(k[i]);
            }
        }
        return out;
    }

    private Expr map(MapLit m, List<Stmt> pre) {
        MapLit c = copy(m);
        if (!m.structured && m.elements.isEmpty()) {
            List<Expr> all = new ArrayList<Expr>();
            for (int i = 0; i < m.keys.size(); i++) {
                all.add(m.keys.get(i));
                all.add(m.values.get(i));
            }
            Expr[] k = kids(pre, null, all.toArray(new Expr[0]));
            if (k == null) {
                return null;
            }
            c.keys = new ArrayList<Expr>();
            c.values = new ArrayList<Expr>();
            for (int i = 0; i < k.length; i += 2) {
                c.keys.add(k[i]);
                c.values.add(k[i + 1]);
            }
            return c;
        }
        List<Expr> all = new ArrayList<Expr>();
        for (Expr x : m.elements) {
            if (x instanceof MapEntry) {
                all.add(((MapEntry) x).key);
                all.add(((MapEntry) x).value);
            } else {
                // A spread, if or for element among awaits keeps the body unlowered.
                if (AsyncLowering.hasAwait(m.elements)) {
                    return null;
                }
            }
        }
        Expr[] k = kids(pre, null, all.toArray(new Expr[0]));
        if (k == null) {
            return null;
        }
        c.elements = new ArrayList<Expr>();
        int j = 0;
        for (Expr x : m.elements) {
            MapEntry me = copy((MapEntry) x);
            me.key = k[j++];
            me.value = k[j++];
            c.elements.add(me);
        }
        return c;
    }

    private Expr assignExpr(Assign a, List<Stmt> pre) {
        Expr lhs = a.lhs;
        while (lhs instanceof ParenExpr) {
            lhs = ((ParenExpr) lhs).inner;
        }
        Expr target;
        if (lhs instanceof Ident) {
            target = lhs;
        } else if (lhs instanceof PropertyGet && !((PropertyGet) lhs).nullAware) {
            PropertyGet p = (PropertyGet) lhs;
            PropertyGet c = copy(p);
            c.target = staticRef(p.target) ? p.target : force(expr(p.target, pre), pre, p);
            if (c.target == null) {
                return null;
            }
            target = c;
        } else if (lhs instanceof IndexGet) {
            IndexGet g = (IndexGet) lhs;
            IndexGet c = copy(g);
            c.target = staticRef(g.target) ? g.target : force(expr(g.target, pre), pre, g);
            if (c.target == null) {
                return null;
            }
            c.index = force(expr(g.index, pre), pre, g);
            if (c.index == null) {
                return null;
            }
            target = c;
        } else {
            return null;
        }
        if ("=".equals(a.op)) {
            Expr r = expr(a.rhs, pre);
            return r == null ? null : assignOf(target, "=", r, a);
        }
        if ("??=".equals(a.op)) {
            List<Stmt> inner = new ArrayList<Stmt>();
            Expr r = expr(a.rhs, inner);
            if (r == null) {
                return null;
            }
            inner.add(exprStmt(assignOf(target, "=", r, a), a));
            pre.add(ifStmt(binary(target, "==", nul(a), a), block(inner, a), null, a));
            return target;
        }
        if (!a.op.endsWith("=")) {
            return null;
        }
        String old = temp();
        pre.add(decl(old, null, target, a));
        Expr r = expr(a.rhs, pre);
        if (r == null) {
            return null;
        }
        return assignOf(target, "=", binary(ident(old, a), a.op.substring(0, a.op.length() - 1), r, a), a);
    }

    private static boolean unparenAwait(Expr e) {
        while (e instanceof ParenExpr) {
            e = ((ParenExpr) e).inner;
        }
        return e instanceof AwaitExpr;
    }

    /// `o.p = await f()` as a statement: the target's parts go into locals and the await
    /// stays the assignment's value, so the slot's type still infers f's type arguments.
    private Expr awaitedStore(Assign a, List<Stmt> pre) {
        Expr rhs = a.rhs;
        while (rhs instanceof ParenExpr) {
            rhs = ((ParenExpr) rhs).inner;
        }
        Assign probe = copy(a);
        Ident hole = ident(temp(), a);
        probe.rhs = hole;
        Expr lhsDone = assignExpr(probe, pre);
        if (!(lhsDone instanceof Assign) || ((Assign) lhsDone).rhs != hole) {
            return null;
        }
        AwaitExpr aw = copy((AwaitExpr) rhs);
        aw.operand = expr(aw.operand, pre);
        if (aw.operand == null) {
            return null;
        }
        Assign out = copy((Assign) lhsDone);
        out.rhs = aw;
        return out;
    }

    private Expr cascade(Cascade k, List<Stmt> pre) {
        Expr tg = expr(k.target, pre);
        if (tg == null) {
            return null;
        }
        String r = temp();
        pre.add(decl(r, null, tg, k));
        List<Stmt> body = k.nullAware ? new ArrayList<Stmt>() : pre;
        for (Expr sec : k.sections) {
            Expr recv = ident(r, k);
            if (k.nullAware) {
                NotNullAssert nn = new NotNullAssert();
                nn.at(k.file, k.line, k.col);
                nn.operand = recv;
                recv = nn;
            }
            boolean[] found = new boolean[1];
            Expr s = subst(sec, recv, found);
            if (!found[0]) {
                return null;
            }
            stmt(exprStmt(s, k), body);
        }
        if (k.nullAware) {
            pre.add(ifStmt(binary(ident(r, k), "!=", nul(k), k), block(body, k), null, k));
        }
        return ident(r, k);
    }

    /// The section with its implicit receiver replaced; nested cascades keep their own.
    private Expr subst(Expr e, Expr rep, boolean[] found) {
        if (e instanceof CascadeTarget) {
            found[0] = true;
            return rep;
        }
        if (e instanceof Call && ((Call) e).target != null) {
            Call c = copy((Call) e);
            c.target = subst(c.target, rep, found);
            return c;
        }
        if (e instanceof PropertyGet) {
            PropertyGet c = copy((PropertyGet) e);
            c.target = subst(c.target, rep, found);
            return c;
        }
        if (e instanceof IndexGet) {
            IndexGet c = copy((IndexGet) e);
            c.target = subst(c.target, rep, found);
            return c;
        }
        if (e instanceof Assign) {
            Assign c = copy((Assign) e);
            c.lhs = subst(c.lhs, rep, found);
            return c;
        }
        if (e instanceof IncDec) {
            IncDec c = copy((IncDec) e);
            c.operand = subst(c.operand, rep, found);
            return c;
        }
        if (e instanceof NotNullAssert) {
            NotNullAssert c = copy((NotNullAssert) e);
            c.operand = subst(c.operand, rep, found);
            return c;
        }
        if (e instanceof Cascade) {
            Cascade c = copy((Cascade) e);
            c.target = subst(c.target, rep, found);
            return c;
        }
        return e;
    }

    /// Children evaluated left to right: each one before the last that awaits is
    /// evaluated into a local (unless {@code keep} says it is a static reference), the
    /// one that awaits last is rewritten, and those after it stay as they are.
    private Expr[] kids(List<Stmt> pre, boolean[] keep, Expr... in) {
        int last = -1;
        for (int i = 0; i < in.length; i++) {
            if (AsyncLowering.hasAwait(in[i])) {
                last = i;
            }
        }
        Expr[] out = in.clone();
        for (int i = 0; i < last; i++) {
            if (in[i] == null) {
                continue;
            }
            Expr x = expr(in[i], pre);
            if (x == null) {
                return null;
            }
            out[i] = keep != null && keep[i] ? x : spill(x, pre, in[i]);
        }
        if (last >= 0) {
            out[last] = expr(in[last], pre);
            if (out[last] == null) {
                return null;
            }
        }
        return out;
    }

    private Expr spill(Expr x, List<Stmt> pre, Node at) {
        if (x instanceof IntLit || x instanceof DoubleLit || x instanceof BoolLit || x instanceof NullLit
                || x instanceof ThisExpr || x instanceof SuperExpr || x instanceof Lambda || isTemp(x)) {
            return x;
        }
        if (x instanceof StringLit) {
            boolean constant = true;
            for (Object o : ((StringLit) x).parts) {
                constant &= !(o instanceof Expr);
            }
            if (constant) {
                return x;
            }
        }
        return force(x, pre, at);
    }

    private Expr force(Expr x, List<Stmt> pre, Node at) {
        if (x == null) {
            return null;
        }
        if (isTemp(x) || x instanceof ThisExpr) {
            return x;
        }
        String t = temp();
        pre.add(decl(t, null, x, at));
        return ident(t, at);
    }

    /// A receiver naming a type or an import prefix: reading it evaluates nothing, so it
    /// is not copied ahead. A field, a getter or a top-level variable is a value read
    /// before the awaits that follow it, so it is.
    private boolean staticRef(Expr e) {
        if (e instanceof ThisExpr || e instanceof SuperExpr) {
            return true;
        }
        if (e instanceof Ident) {
            return !locals.contains(((Ident) e).name) && !isTemp(e) && noValue.test(((Ident) e).name);
        }
        if (e instanceof PropertyGet && !((PropertyGet) e).nullAware && ((PropertyGet) e).target instanceof Ident) {
            // `prefix.Type`: both names read no value. `Type.field` and `prefix.topLevel` do.
            return staticRef(((PropertyGet) e).target) && noValue.test(((PropertyGet) e).name);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Node helpers
    // ------------------------------------------------------------------

    private String temp() {
        String t = "$anf" + (temps++);
        tempNames.add(t);
        return t;
    }

    private boolean isTemp(Expr e) {
        return e instanceof Ident && tempNames.contains(((Ident) e).name);
    }

    private static <T extends Node> T at(T n, Node src) {
        n.at(src.file, src.line, src.col);
        return n;
    }

    private static Ident ident(String name, Node at) {
        Ident i = at(new Ident(), at);
        i.name = name;
        return i;
    }

    private static BoolLit bool(boolean v, Node at) {
        BoolLit b = at(new BoolLit(), at);
        b.value = v;
        return b;
    }

    private static NullLit nul(Node at) {
        return at(new NullLit(), at);
    }

    private static Unary not(Expr e, Node at) {
        Unary u = at(new Unary(), at);
        u.op = "!";
        u.operand = e;
        return u;
    }

    private static Binary binary(Expr l, String op, Expr r, Node at) {
        Binary b = at(new Binary(), at);
        b.left = l;
        b.op = op;
        b.right = r;
        return b;
    }

    private static Assign assignOf(Expr lhs, String op, Expr rhs, Node at) {
        Assign a = at(new Assign(), at);
        a.lhs = lhs;
        a.op = op;
        a.rhs = rhs;
        return a;
    }

    private static ExprStmt assign(Expr lhs, Expr rhs, Node at) {
        return exprStmt(assignOf(lhs, "=", rhs, at), at);
    }

    private static ExprStmt exprStmt(Expr e, Node at) {
        ExprStmt s = at(new ExprStmt(), at);
        s.expr = e;
        return s;
    }

    private static VarDeclStmt decl(String name, TypeRef t, Expr init, Node at) {
        VarDeclStmt v = at(new VarDeclStmt(), at);
        v.name = name;
        v.type = t == null ? TypeRef.of("var") : t;
        v.initializer = init;
        return v;
    }

    private static IfStmt ifStmt(Expr c, Stmt then, Stmt els, Node at) {
        IfStmt i = at(new IfStmt(), at);
        i.condition = c;
        i.thenStmt = then;
        i.elseStmt = els;
        return i;
    }

    private static Stmt breakUnless(Expr c, Node at) {
        BreakStmt b = at(new BreakStmt(), at);
        return ifStmt(not(paren(c, at), at), b, null, at);
    }

    private static Expr paren(Expr e, Node at) {
        ParenExpr p = at(new ParenExpr(), at);
        p.inner = e;
        return p;
    }

    private static Block block(List<Stmt> l, Node at) {
        Block b = at(new Block(), at);
        b.statements = l;
        return b;
    }

    private static Block blockOf(Stmt s, Node at) {
        if (s instanceof Block && s.labels == null) {
            Block c = copy((Block) s);
            return c;
        }
        List<Stmt> l = new ArrayList<Stmt>();
        if (s != null) {
            l.add(s);
        }
        return block(l, at);
    }

    private static List<Field> fields(Class<?> c) {
        List<Field> fs = new ArrayList<Field>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && (Node.class.isAssignableFrom(f.getType())
                        || List.class.isAssignableFrom(f.getType()))) {
                    f.setAccessible(true);
                    fs.add(f);
                }
            }
        }
        return fs;
    }

    private static Object get(Field f, Object n) {
        try {
            return f.get(n);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /// A shallow copy: the same fields, lists shared (callers replace what they change).
    @SuppressWarnings("unchecked")
    static <T> T copy(T n) {
        try {
            Class<?> c = n.getClass();
            java.lang.reflect.Constructor<?> k = c.getDeclaredConstructor();
            k.setAccessible(true);
            T r = (T) k.newInstance();
            for (Class<?> x = c; x != null && x != Object.class; x = x.getSuperclass()) {
                for (Field f : x.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        f.set(r, f.get(n));
                    }
                }
            }
            return r;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
