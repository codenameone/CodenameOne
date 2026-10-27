/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.AugAssign;
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Await;
import com.codename1.python.transpiler.ast.Node.Comprehension;
import com.codename1.python.transpiler.ast.Node.BinOp;
import com.codename1.python.transpiler.ast.Node.BoolOp;
import com.codename1.python.transpiler.ast.Node.Break;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Comp;
import com.codename1.python.transpiler.ast.Node.Compare;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.DictExpr;
import com.codename1.python.transpiler.ast.Node.ExceptHandler;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.For;
import com.codename1.python.transpiler.ast.Node.FormattedValue;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.IfExp;
import com.codename1.python.transpiler.ast.Node.JoinedStr;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Lambda;
import com.codename1.python.transpiler.ast.Node.ListExpr;
import com.codename1.python.transpiler.ast.Node.Match;
import com.codename1.python.transpiler.ast.Node.MatchCase;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.NamedExpr;
import com.codename1.python.transpiler.ast.Node.Raise;
import com.codename1.python.transpiler.ast.Node.Return;
import com.codename1.python.transpiler.ast.Node.SetExpr;
import com.codename1.python.transpiler.ast.Node.Slice;
import com.codename1.python.transpiler.ast.Node.Starred;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.Try;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import com.codename1.python.transpiler.ast.Node.While;
import com.codename1.python.transpiler.ast.Node.With;
import com.codename1.python.transpiler.ast.Node.WithItem;
import com.codename1.python.transpiler.ast.Node.Yield;
import com.codename1.python.transpiler.ast.Scope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Moves every `yield` and `await` of a generator or coroutine body to the
/// top of a statement, where [GenLowering] can split the body at it.
///
/// `x = f(a, await b)` becomes `$y1 = f; $y2 = a; $y3 = await b;
/// x = $y1($y2, $y3)`: everything evaluated before the suspension is kept in
/// a hidden local, so Python's left-to-right order survives, and `and`, `or`
/// and `if`-expressions become `if` statements so a suspension in their
/// right-hand side still happens only when Python would evaluate it. Only
/// the headers of a statement are rewritten; [GenLowering] lowers the bodies
/// statement by statement and comes back here for each.
final class Linearize {
    private final String file;
    private final Scope scope;
    private int uid;

    Linearize(String file, Scope scope) {
        this.file = file;
        this.scope = scope;
    }

    // ---- where the suspensions are -----------------------------------------

    /// Whether a comprehension is asynchronous: an `async for` clause, or an
    /// `await` in its element, its conditions or an inner iterable.
    static boolean isAsync(Comp c) {
        if (suspends(c.elt) || suspends(c.value)) {
            return true;
        }
        for (int i = 0; i < c.generators.size(); i++) {
            Comprehension g = c.generators.get(i);
            if (g.isAsync || i > 0 && suspends(g.iter) || any(g.ifs)) {
                return true;
            }
        }
        return false;
    }

    /// An async comprehension evaluated by awaiting it: all but a generator
    /// expression, which is an async generator instead.
    static boolean awaited(Comp c) {
        return c.kind != Comp.Kind.GENERATOR && isAsync(c);
    }

    /// Whether evaluating `e` can suspend: a yield or await outside any
    /// nested function or comprehension body.
    static boolean suspends(Expr e) {
        if (e == null) {
            return false;
        }
        if (e instanceof Yield || e instanceof Await) {
            return true;
        }
        if (e instanceof Lambda) {
            return false;
        }
        if (e instanceof Comp) {
            // Only the first iterable is evaluated in the enclosing scope,
            // but an async list, set or dict comprehension is awaited.
            return suspends(((Comp) e).generators.get(0).iter) || awaited((Comp) e);
        }
        if (e instanceof NamedExpr) {
            return suspends(((NamedExpr) e).value);
        }
        if (e instanceof BoolOp) {
            return any(((BoolOp) e).values);
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            return suspends(i.test) || suspends(i.body) || suspends(i.orelse);
        }
        if (e instanceof FormattedValue) {
            FormattedValue f = (FormattedValue) e;
            return suspends(f.value) || suspends(f.spec);
        }
        return any(children(e));
    }

    private static boolean any(List<Expr> l) {
        for (Expr x : l) {
            if (suspends(x)) {
                return true;
            }
        }
        return false;
    }

    /// Whether running `s` can suspend, its nested statements included.
    static boolean suspends(Stmt s) {
        if (s instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) s;
            return any(f.decorators) || any(f.args.defaults) || anyNullable(f.args.kwDefaults);
        }
        if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            return any(c.decorators) || any(c.bases);
        }
        for (Expr e : headers(s)) {
            if (suspends(e)) {
                return true;
            }
        }
        for (List<Stmt> b : bodies(s)) {
            if (suspends(b)) {
                return true;
            }
        }
        if (s instanceof For && ((For) s).isAsync) {
            return true;
        }
        return s instanceof With && ((With) s).isAsync;
    }

    static boolean suspends(List<Stmt> body) {
        for (Stmt s : body) {
            if (suspends(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyNullable(List<Expr> l) {
        for (Expr x : l) {
            if (x != null && suspends(x)) {
                return true;
            }
        }
        return false;
    }

    /// The expressions a statement evaluates itself, not counting its body.
    static List<Expr> headers(Stmt s) {
        List<Expr> r = new ArrayList<Expr>();
        if (s instanceof ExprStmt) {
            r.add(((ExprStmt) s).value);
        } else if (s instanceof Assign) {
            r.addAll(((Assign) s).targets);
            r.add(((Assign) s).value);
        } else if (s instanceof AugAssign) {
            r.add(((AugAssign) s).target);
            r.add(((AugAssign) s).value);
        } else if (s instanceof AnnAssign) {
            r.add(((AnnAssign) s).target);
            if (((AnnAssign) s).value != null) {
                r.add(((AnnAssign) s).value);
            }
        } else if (s instanceof Return) {
            if (((Return) s).value != null) {
                r.add(((Return) s).value);
            }
        } else if (s instanceof If) {
            r.add(((If) s).test);
        } else if (s instanceof While) {
            r.add(((While) s).test);
        } else if (s instanceof For) {
            r.add(((For) s).target);
            r.add(((For) s).iter);
        } else if (s instanceof With) {
            for (WithItem w : ((With) s).items) {
                r.add(w.context);
                if (w.vars != null) {
                    r.add(w.vars);
                }
            }
        } else if (s instanceof Raise) {
            if (((Raise) s).exc != null) {
                r.add(((Raise) s).exc);
            }
            if (((Raise) s).cause != null) {
                r.add(((Raise) s).cause);
            }
        } else if (s instanceof Node.Assert) {
            r.add(((Node.Assert) s).test);
            if (((Node.Assert) s).msg != null) {
                r.add(((Node.Assert) s).msg);
            }
        } else if (s instanceof Node.Delete) {
            r.addAll(((Node.Delete) s).targets);
        } else if (s instanceof Match) {
            r.add(((Match) s).subject);
            for (MatchCase c : ((Match) s).cases) {
                if (c.guard != null) {
                    r.add(c.guard);
                }
            }
        } else if (s instanceof Try) {
            for (ExceptHandler h : ((Try) s).handlers) {
                if (h.type != null) {
                    r.add(h.type);
                }
            }
        } else if (s instanceof Node.TypeAlias) {
            r.add(((Node.TypeAlias) s).value);
        }
        return r;
    }

    /// The statement lists nested in `s`.
    static List<List<Stmt>> bodies(Stmt s) {
        List<List<Stmt>> r = new ArrayList<List<Stmt>>();
        if (s instanceof If) {
            r.add(((If) s).body);
            r.add(((If) s).orelse);
        } else if (s instanceof While) {
            r.add(((While) s).body);
            r.add(((While) s).orelse);
        } else if (s instanceof For) {
            r.add(((For) s).body);
            r.add(((For) s).orelse);
        } else if (s instanceof With) {
            r.add(((With) s).body);
        } else if (s instanceof Try) {
            Try t = (Try) s;
            r.add(t.body);
            for (ExceptHandler h : t.handlers) {
                r.add(h.body);
            }
            r.add(t.orelse);
            r.add(t.finalbody);
        } else if (s instanceof Match) {
            for (MatchCase c : ((Match) s).cases) {
                r.add(c.body);
            }
        }
        return r;
    }

    /// The operands of `e` in evaluation order (nulls left out).
    static List<Expr> children(Expr e) {
        List<Expr> r = new ArrayList<Expr>();
        if (e instanceof Attribute) {
            r.add(((Attribute) e).value);
        } else if (e instanceof Subscript) {
            r.add(((Subscript) e).value);
            r.add(((Subscript) e).index);
        } else if (e instanceof Slice) {
            Slice s = (Slice) e;
            addIf(r, s.lower);
            addIf(r, s.upper);
            addIf(r, s.step);
        } else if (e instanceof Starred) {
            r.add(((Starred) e).value);
        } else if (e instanceof BinOp) {
            r.add(((BinOp) e).left);
            r.add(((BinOp) e).right);
        } else if (e instanceof UnaryOp) {
            r.add(((UnaryOp) e).operand);
        } else if (e instanceof Compare) {
            r.add(((Compare) e).left);
            r.addAll(((Compare) e).comparators);
        } else if (e instanceof Call) {
            Call c = (Call) e;
            r.add(c.func);
            r.addAll(c.args);
            for (Keyword k : c.keywords) {
                r.add(k.value);
            }
        } else if (e instanceof ListExpr) {
            r.addAll(((ListExpr) e).elts);
        } else if (e instanceof TupleExpr) {
            r.addAll(((TupleExpr) e).elts);
        } else if (e instanceof SetExpr) {
            r.addAll(((SetExpr) e).elts);
        } else if (e instanceof DictExpr) {
            DictExpr d = (DictExpr) e;
            for (int i = 0; i < d.keys.size(); i++) {
                addIf(r, d.keys.get(i));
                r.add(d.values.get(i));
            }
        } else if (e instanceof JoinedStr) {
            r.addAll(((JoinedStr) e).parts);
        } else if (e instanceof Yield) {
            addIf(r, ((Yield) e).value);
        } else if (e instanceof Await) {
            r.add(((Await) e).value);
        }
        return r;
    }

    private static void addIf(List<Expr> r, Expr e) {
        if (e != null) {
            r.add(e);
        }
    }

    // ---- statements --------------------------------------------------------

    /// The statements that replace `s`, so that no header of the last one
    /// suspends except as the whole value of an expression statement or an
    /// assignment; null when `s` is already in that form.
    List<Stmt> stmt(Stmt s) {
        if (s instanceof ExprStmt) {
            Expr v = ((ExprStmt) s).value;
            if (!suspends(v) || topOnly(v)) {
                return null;
            }
            List<Stmt> pre = new ArrayList<Stmt>();
            Expr n = top(v, pre);
            pre.add(at(new ExprStmt(n), s));
            return pre;
        }
        if (s instanceof Assign) {
            Assign a = (Assign) s;
            if (any(a.targets)) {
                throw err(s, "await or yield inside an assignment target");
            }
            if (!suspends(a.value) || topOnly(a.value)) {
                return null;
            }
            List<Stmt> pre = new ArrayList<Stmt>();
            Expr n = top(a.value, pre);
            pre.add(at(new Assign(a.targets, n), s));
            return pre;
        }
        if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (a.value == null || scope.kind == Scope.Kind.CLASS) {
                return null;
            }
            // Inside a function an annotation is never evaluated.
            List<Stmt> r = new ArrayList<Stmt>();
            r.add(at(new Assign(Collections.singletonList(a.target), a.value), s));
            return r;
        }
        List<Stmt> pre = new ArrayList<Stmt>();
        if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            if (suspends(a.target)) {
                throw err(s, "await or yield inside an assignment target");
            }
            if (!suspends(a.value)) {
                return null;
            }
            pre.add(at(new AugAssign(a.target, a.op, lin(a.value, pre)), s));
            return pre;
        }
        if (s instanceof Return) {
            Return r = (Return) s;
            if (r.value == null || !suspends(r.value)) {
                return null;
            }
            pre.add(at(new Return(lin(r.value, pre)), s));
            return pre;
        }
        if (s instanceof If) {
            If i = (If) s;
            if (!suspends(i.test)) {
                return null;
            }
            pre.add(at(new If(lin(i.test, pre), i.body, i.orelse), s));
            return pre;
        }
        if (s instanceof While) {
            While w = (While) s;
            if (!suspends(w.test)) {
                return null;
            }
            if (!w.orelse.isEmpty()) {
                throw err(s, "await or yield in the condition of a while loop with an else clause");
            }
            // while T: B  ->  while True: (T's pre); if not T: break; B
            List<Stmt> body = new ArrayList<Stmt>();
            Expr t = lin(w.test, body);
            List<Stmt> brk = new ArrayList<Stmt>();
            brk.add(at(new Break(), s));
            body.add(at(new If(at(new UnaryOp("not", t), s), brk, new ArrayList<Stmt>()), s));
            body.addAll(w.body);
            pre.add(at(new While(at(new Constant(Constant.Kind.TRUE, "True"), s), body, w.orelse), s));
            return pre;
        }
        if (s instanceof For) {
            For f = (For) s;
            if (suspends(f.target)) {
                throw err(s, "await or yield inside a for target");
            }
            if (!suspends(f.iter)) {
                return null;
            }
            pre.add(at(new For(f.target, lin(f.iter, pre), f.body, f.orelse, f.isAsync), s));
            return pre;
        }
        if (s instanceof With) {
            With w = (With) s;
            if (w.items.size() > 1) {
                // `with a, b: B` is `with a: with b: B`.
                List<Stmt> inner = new ArrayList<Stmt>();
                inner.add(at(new With(new ArrayList<WithItem>(w.items.subList(1, w.items.size())), w.body,
                        w.isAsync), s));
                pre.add(at(new With(Collections.singletonList(w.items.get(0)), inner, w.isAsync), s));
                return pre;
            }
            WithItem item = w.items.get(0);
            if (item.vars != null && suspends(item.vars)) {
                throw err(s, "await or yield inside a with target");
            }
            if (!suspends(item.context)) {
                return null;
            }
            WithItem n = new WithItem(lin(item.context, pre), item.vars);
            n.line = item.line;
            n.col = item.col;
            pre.add(at(new With(Collections.singletonList(n), w.body, w.isAsync), s));
            return pre;
        }
        if (s instanceof Raise) {
            Raise r = (Raise) s;
            if (!suspends(r.exc) && !suspends(r.cause)) {
                return null;
            }
            List<Expr> parts = new ArrayList<Expr>();
            addIf(parts, r.exc);
            addIf(parts, r.cause);
            List<Expr> n = seq(parts, pre);
            pre.add(at(new Raise(r.exc == null ? null : n.get(0), r.cause == null ? null : n.get(n.size() - 1)), s));
            return pre;
        }
        if (s instanceof Node.Assert) {
            Node.Assert a = (Node.Assert) s;
            if (a.msg != null && suspends(a.msg)) {
                throw err(s, "await or yield inside an assert message");
            }
            if (!suspends(a.test)) {
                return null;
            }
            pre.add(at(new Node.Assert(lin(a.test, pre), a.msg), s));
            return pre;
        }
        if (s instanceof Match) {
            Match m = (Match) s;
            for (MatchCase c : m.cases) {
                if (c.guard != null && suspends(c.guard)) {
                    throw err(s, "await or yield inside a case guard");
                }
            }
            if (!suspends(m.subject)) {
                return null;
            }
            pre.add(at(new Match(lin(m.subject, pre), m.cases), s));
            return pre;
        }
        if (s instanceof Try) {
            for (ExceptHandler h : ((Try) s).handlers) {
                if (h.type != null && suspends(h.type)) {
                    throw err(h, "await or yield inside an except clause's type");
                }
            }
            return null;
        }
        for (Expr e : headers(s)) {
            if (suspends(e)) {
                throw err(s, "await or yield here");
            }
        }
        if ((s instanceof FunctionDef || s instanceof ClassDef) && suspends(s)) {
            throw err(s, "await or yield in a decorator, default or base class");
        }
        return null;
    }

    /// A suspension whose operand does not suspend itself.
    private static boolean topOnly(Expr v) {
        if (v instanceof Yield) {
            return !suspends(((Yield) v).value);
        }
        if (!(v instanceof Await)) {
            return false;
        }
        Expr x = ((Await) v).value;
        if (x instanceof Comp && awaited((Comp) x)) {
            return !suspends(((Comp) x).generators.get(0).iter);
        }
        return !suspends(x);
    }

    /// `v` with its operands linearized, keeping a suspension at its top.
    private Expr top(Expr v, List<Stmt> pre) {
        if (v instanceof Yield) {
            Yield y = (Yield) v;
            return at(new Yield(lin(y.value, pre), y.from), v);
        }
        if (v instanceof Await) {
            Expr x = ((Await) v).value;
            if (x instanceof Comp && awaited((Comp) x)) {
                // An async comprehension, awaited where lin put it.
                return v;
            }
            return at(new Await(lin(x, pre)), v);
        }
        return lin(v, pre);
    }

    // ---- expressions -------------------------------------------------------

    /// `e` rewritten to suspend nowhere, with what it must evaluate first
    /// appended to `pre`.
    Expr lin(Expr e, List<Stmt> pre) {
        if (e == null || !suspends(e)) {
            return e;
        }
        if (e instanceof Yield || e instanceof Await) {
            Name t = hidden(e);
            pre.add(at(new Assign(Collections.<Expr>singletonList(t), top(e, pre)), e));
            return name(t);
        }
        if (e instanceof BoolOp) {
            return boolOp((BoolOp) e, pre);
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            Expr test = lin(i.test, pre);
            Name r = hidden(e);
            List<Stmt> a = new ArrayList<Stmt>();
            a.add(assign(r, lin(i.body, a), e));
            List<Stmt> b = new ArrayList<Stmt>();
            b.add(assign(r, lin(i.orelse, b), e));
            pre.add(at(new If(test, a, b), e));
            return name(r);
        }
        if (e instanceof NamedExpr) {
            NamedExpr n = (NamedExpr) e;
            return at(new NamedExpr(n.target, lin(n.value, pre)), e);
        }
        if (e instanceof Comp) {
            Comp c = (Comp) e;
            if (suspends(c.generators.get(0).iter)) {
                throw err(e, "await in a comprehension's iterable");
            }
            // An async comprehension is a coroutine the enclosing one awaits.
            Await w = at(new Await(c), e);
            Name t = hidden(e);
            pre.add(at(new Assign(Collections.<Expr>singletonList(t), w), e));
            return name(t);
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            for (int i = 1; i < c.comparators.size(); i++) {
                if (suspends(c.comparators.get(i))) {
                    throw err(e, "await or yield inside a chained comparison");
                }
            }
        }
        if (e instanceof FormattedValue) {
            FormattedValue f = (FormattedValue) e;
            if (suspends(f.spec)) {
                throw err(e, "await or yield inside a format spec");
            }
            return at(new FormattedValue(lin(f.value, pre), f.conversion, f.spec), e);
        }
        return rebuild(e, seq(children(e), pre));
    }

    /// Linearizes operands evaluated in order: each one before the last that
    /// suspends is kept in a hidden local, so it is evaluated first.
    private List<Expr> seq(List<Expr> kids, List<Stmt> pre) {
        int last = -1;
        for (int i = 0; i < kids.size(); i++) {
            if (suspends(kids.get(i))) {
                last = i;
            }
        }
        List<Expr> r = new ArrayList<Expr>();
        for (int i = 0; i < kids.size(); i++) {
            Expr k = kids.get(i);
            if (i < last) {
                r.add(suspends(k) ? spill(lin(k, pre), pre) : spill(k, pre));
            } else if (i == last) {
                r.add(lin(k, pre));
            } else {
                r.add(k);
            }
        }
        return r;
    }

    /// `k`'s value in a hidden local, or `k` when evaluating it later is the
    /// same as evaluating it now.
    private Expr spill(Expr k, List<Stmt> pre) {
        if (k instanceof Constant) {
            return k;
        }
        if (k instanceof Starred) {
            return at(new Starred(spill(((Starred) k).value, pre)), k);
        }
        if (k instanceof Slice) {
            Slice s = (Slice) k;
            return at(new Slice(s.lower == null ? null : spill(s.lower, pre), s.upper == null ? null
                    : spill(s.upper, pre), s.step == null ? null : spill(s.step, pre)), k);
        }
        Name t = hidden(k);
        pre.add(assign(t, k, k));
        return name(t);
    }

    private Expr boolOp(BoolOp b, List<Stmt> pre) {
        Name r = hidden(b);
        pre.add(assign(r, lin(b.values.get(0), pre), b));
        List<Stmt> into = pre;
        for (int i = 1; i < b.values.size(); i++) {
            List<Stmt> body = new ArrayList<Stmt>();
            body.add(assign(r, lin(b.values.get(i), body), b));
            Expr test = b.isAnd ? name(r) : at(new UnaryOp("not", name(r)), b);
            into.add(at(new If(test, body, new ArrayList<Stmt>()), b));
            into = body;
        }
        return name(r);
    }

    /// `e` with its operands (in [#children] order) replaced.
    private Expr rebuild(Expr e, List<Expr> k) {
        int i = 0;
        Expr r;
        if (e instanceof Attribute) {
            r = new Attribute(k.get(0), ((Attribute) e).attr);
        } else if (e instanceof Subscript) {
            r = new Subscript(k.get(0), k.get(1));
        } else if (e instanceof Slice) {
            Slice s = (Slice) e;
            Expr lo = s.lower == null ? null : k.get(i++);
            Expr up = s.upper == null ? null : k.get(i++);
            Expr st = s.step == null ? null : k.get(i++);
            r = new Slice(lo, up, st);
        } else if (e instanceof Starred) {
            r = new Starred(k.get(0));
        } else if (e instanceof BinOp) {
            r = new BinOp(k.get(0), ((BinOp) e).op, k.get(1));
        } else if (e instanceof UnaryOp) {
            r = new UnaryOp(((UnaryOp) e).op, k.get(0));
        } else if (e instanceof Compare) {
            Compare c = new Compare(k.get(0));
            c.ops.addAll(((Compare) e).ops);
            c.comparators.addAll(k.subList(1, k.size()));
            r = c;
        } else if (e instanceof Call) {
            Call o = (Call) e;
            Call c = new Call(k.get(i++));
            for (int j = 0; j < o.args.size(); j++) {
                c.args.add(k.get(i++));
            }
            for (Keyword kw : o.keywords) {
                Keyword n = new Keyword(kw.arg, k.get(i++));
                n.line = kw.line;
                n.col = kw.col;
                c.keywords.add(n);
            }
            r = c;
        } else if (e instanceof ListExpr) {
            r = new ListExpr(k);
        } else if (e instanceof TupleExpr) {
            r = new TupleExpr(k);
        } else if (e instanceof SetExpr) {
            r = new SetExpr(k);
        } else if (e instanceof DictExpr) {
            DictExpr o = (DictExpr) e;
            DictExpr d = new DictExpr();
            for (int j = 0; j < o.keys.size(); j++) {
                d.keys.add(o.keys.get(j) == null ? null : k.get(i++));
                d.values.add(k.get(i++));
            }
            r = d;
        } else if (e instanceof JoinedStr) {
            JoinedStr j = new JoinedStr();
            j.parts.addAll(k);
            r = j;
        } else {
            throw err(e, "await or yield inside " + e.getClass().getSimpleName());
        }
        return at(r, e);
    }

    // ---- helpers -----------------------------------------------------------

    private Name hidden(Node at) {
        String n = "$y" + (++uid);
        scope.assigned.add(n);
        return at(new Name(n), at);
    }

    private static Name name(Name n) {
        return at(new Name(n.id), n);
    }

    private static Assign assign(Name t, Expr v, Node at) {
        return at(new Assign(Collections.<Expr>singletonList(name(t)), v), at);
    }

    private static <T extends Node> T at(T n, Node from) {
        n.line = from.line;
        n.col = from.col;
        return n;
    }

    private PythonSyntaxError err(Node n, String what) {
        return new PythonSyntaxError(file, n.line, n.col, what + " is not supported yet");
    }
}
