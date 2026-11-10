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
package com.codename1.python.transpiler.analyze;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Assert;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.AugAssign;
import com.codename1.python.transpiler.ast.Node.BinOp;
import com.codename1.python.transpiler.ast.Node.BoolOp;
import com.codename1.python.transpiler.ast.Node.Break;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Comp;
import com.codename1.python.transpiler.ast.Node.Compare;
import com.codename1.python.transpiler.ast.Node.Comprehension;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Continue;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.For;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Global;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.IfExp;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.ListExpr;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Pass;
import com.codename1.python.transpiler.ast.Node.SetExpr;
import com.codename1.python.transpiler.ast.Node.Slice;
import com.codename1.python.transpiler.ast.Node.Starred;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import com.codename1.python.transpiler.ast.Node.While;
import com.codename1.python.transpiler.ast.Node.Yield;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Inlines a generator into the loop that consumes it, before scope
/// analysis, the way a tracing JIT does: `for x in g(a)` becomes the body of
/// `g` with its locals renamed apart and its `yield e` replaced by `x = e`
/// followed by the loop body. No generator object is made, nothing is
/// suspended, and the inlined locals are typed like any other.
///
/// The interleaving of the two bodies is exactly the generator's: `g` runs
/// up to its yield, the loop body runs, `g` resumes. It is only done where
/// nothing could tell the difference:
///
/// - `g` is a plain function at module level, bound by its `def` alone,
///   with positional parameters only and exactly one `yield`, which is a
///   statement of its own, so the loop body is not copied. It has no
///   `return`, `try`, `with`, `del`, `global`, `yield from`, nested scope or
///   anything the copy below does not know.
/// - The loop has no `else`, its target is a name, and its body has no
///   `break` or `continue` of its own: those would have to leave `g`'s loops.
/// - No name `g` reads from the module is bound by the consuming function or
///   one enclosing it, where the inlined read would land instead.
///
/// One difference remains, deliberately: a `StopIteration` raised inside a
/// call `g` makes is a `RuntimeError` out of a real generator (PEP 479) and
/// stays a `StopIteration` here. It is an error in the program either way.
///
/// `n = sum(e for x in it if c)` is first written as a list built by a loop
/// and summed, which is how the typed code already treats it (see
/// `TypedEmit.EAGER`), so that its loop can be inlined too.
public final class GenInline {
    /// How deep an inlined body is inlined into again.
    private static final int DEPTH = 3;

    private final Map<String, FunctionDef> gens = new HashMap<String, FunctionDef>();
    private final Set<String> moduleBound = new HashSet<String>();
    private int counter;

    private GenInline() {
    }

    public static void rewrite(List<Stmt> module) {
        GenInline g = new GenInline();
        Map<String, Integer> defs = new HashMap<String, Integer>();
        for (Stmt s : module) {
            g.bound(s, g.moduleBound);
            if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                defs.put(n, defs.containsKey(n) ? 2 : 1);
            }
        }
        Set<String> declaredGlobal = new HashSet<String>();
        for (Stmt s : module) {
            globalsIn(s, declaredGlobal);
        }
        Map<String, Integer> stores = new HashMap<String, Integer>();
        for (Stmt s : module) {
            Set<String> one = new HashSet<String>();
            g.bound(s, one);
            for (String n : one) {
                stores.merge(n, 1, Integer::sum);
            }
        }
        for (Stmt s : module) {
            if (s instanceof FunctionDef) {
                FunctionDef d = (FunctionDef) s;
                if (defs.get(d.name) == 1 && stores.get(d.name) == 1 && !declaredGlobal.contains(d.name)
                        && candidate(d)) {
                    g.gens.put(d.name, d);
                }
            }
        }
        boolean sumFree = !g.moduleBound.contains("sum") && !declaredGlobal.contains("sum")
                && !g.moduleBound.contains("*");
        for (Stmt s : module) {
            g.scopes(s, new ArrayDeque<Set<String>>(), sumFree);
        }
    }

    // ---- which functions qualify ----------------------------------------

    private static boolean candidate(FunctionDef d) {
        Node.Arguments a = d.args;
        if (d.isAsync || !d.decorators.isEmpty() || a.vararg != null || a.kwarg != null || !a.kwonly.isEmpty()
                || !a.defaults.isEmpty() || a.positionalOnly > 0) {
            return false;
        }
        int[] yields = {0};
        if (!plainStmts(d.body, yields)) {
            return false;
        }
        return yields[0] == 1;
    }

    /// Statements the copy knows, with every yield one of them.
    private static boolean plainStmts(List<Stmt> body, int[] yields) {
        for (Stmt s : body) {
            if (!plainStmt(s, yields)) {
                return false;
            }
        }
        return true;
    }

    private static boolean plainStmt(Stmt s, int[] yields) {
        if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Yield) {
            Yield y = (Yield) ((ExprStmt) s).value;
            yields[0]++;
            return !y.from && (y.value == null || plainExpr(y.value));
        }
        if (s instanceof ExprStmt) {
            return plainExpr(((ExprStmt) s).value);
        }
        if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                if (!plainTarget(t)) {
                    return false;
                }
            }
            return plainExpr(((Assign) s).value);
        }
        if (s instanceof AugAssign) {
            return plainTarget(((AugAssign) s).target) && plainExpr(((AugAssign) s).value);
        }
        if (s instanceof Pass || s instanceof Break || s instanceof Continue) {
            return true;
        }
        if (s instanceof Assert) {
            return plainExpr(((Assert) s).test) && (((Assert) s).msg == null || plainExpr(((Assert) s).msg));
        }
        if (s instanceof If) {
            If i = (If) s;
            return plainExpr(i.test) && plainStmts(i.body, yields) && plainStmts(i.orelse, yields);
        }
        if (s instanceof While) {
            While w = (While) s;
            return plainExpr(w.test) && plainStmts(w.body, yields) && plainStmts(w.orelse, yields);
        }
        if (s instanceof For) {
            For f = (For) s;
            return !f.isAsync && plainTarget(f.target) && plainExpr(f.iter) && plainStmts(f.body, yields)
                    && plainStmts(f.orelse, yields);
        }
        return false;
    }

    private static boolean plainTarget(Expr t) {
        if (t instanceof Name) {
            return true;
        }
        if (t instanceof TupleExpr || t instanceof ListExpr) {
            for (Expr e : elts(t)) {
                if (!plainTarget(e)) {
                    return false;
                }
            }
            return true;
        }
        if (t instanceof Starred) {
            return plainTarget(((Starred) t).value);
        }
        return (t instanceof Attribute || t instanceof Subscript) && plainExpr(t);
    }

    /// Expressions the copy knows: no nested scope, no yield, no walrus.
    private static boolean plainExpr(Expr e) {
        if (e == null || e instanceof Name || e instanceof Constant) {
            return true;
        }
        if (e instanceof Attribute) {
            return plainExpr(((Attribute) e).value);
        }
        if (e instanceof Subscript) {
            return plainExpr(((Subscript) e).value) && plainExpr(((Subscript) e).index);
        }
        if (e instanceof Slice) {
            Slice s = (Slice) e;
            return plainExpr(s.lower) && plainExpr(s.upper) && plainExpr(s.step);
        }
        if (e instanceof Starred) {
            return plainExpr(((Starred) e).value);
        }
        if (e instanceof BinOp) {
            return plainExpr(((BinOp) e).left) && plainExpr(((BinOp) e).right);
        }
        if (e instanceof UnaryOp) {
            return plainExpr(((UnaryOp) e).operand);
        }
        if (e instanceof BoolOp) {
            return allPlain(((BoolOp) e).values);
        }
        if (e instanceof Compare) {
            return plainExpr(((Compare) e).left) && allPlain(((Compare) e).comparators);
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            return plainExpr(i.test) && plainExpr(i.body) && plainExpr(i.orelse);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            if (!plainExpr(c.func) || !allPlain(c.args)) {
                return false;
            }
            for (Keyword k : c.keywords) {
                if (!plainExpr(k.value)) {
                    return false;
                }
            }
            return true;
        }
        if (e instanceof ListExpr || e instanceof TupleExpr || e instanceof SetExpr) {
            return allPlain(elts(e));
        }
        return false;
    }

    private static boolean allPlain(List<Expr> es) {
        for (Expr x : es) {
            if (!plainExpr(x)) {
                return false;
            }
        }
        return true;
    }

    private static List<Expr> elts(Expr e) {
        if (e instanceof ListExpr) {
            return ((ListExpr) e).elts;
        }
        if (e instanceof TupleExpr) {
            return ((TupleExpr) e).elts;
        }
        return ((SetExpr) e).elts;
    }

    // ---- names --------------------------------------------------------------

    /// Adds every name `s` binds in its own scope (`*` for a star import),
    /// without entering nested scopes beyond their own name.
    private static void bound(Node s, Set<String> out) {
        if (s instanceof FunctionDef) {
            out.add(((FunctionDef) s).name);
            return;
        }
        if (s instanceof ClassDef) {
            out.add(((ClassDef) s).name);
            return;
        }
        if (s instanceof Node.Lambda || s instanceof Comp) {
            if (s instanceof Comp) {
                // The first iterable is evaluated in the enclosing scope.
                bound(((Comp) s).generators.get(0).iter, out);
            }
            return;
        }
        if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                targetNames(t, out);
            }
        } else if (s instanceof AugAssign) {
            targetNames(((AugAssign) s).target, out);
        } else if (s instanceof Node.AnnAssign) {
            targetNames(((Node.AnnAssign) s).target, out);
        } else if (s instanceof For) {
            targetNames(((For) s).target, out);
        } else if (s instanceof Node.WithItem && ((Node.WithItem) s).vars != null) {
            targetNames(((Node.WithItem) s).vars, out);
        } else if (s instanceof Node.ExceptHandler && ((Node.ExceptHandler) s).name != null) {
            out.add(((Node.ExceptHandler) s).name);
        } else if (s instanceof Node.Import) {
            for (Node.Alias a : ((Node.Import) s).names) {
                String n = a.asname != null ? a.asname : a.name;
                int dot = n.indexOf('.');
                out.add(dot < 0 ? n : n.substring(0, dot));
            }
        } else if (s instanceof Node.ImportFrom) {
            for (Node.Alias a : ((Node.ImportFrom) s).names) {
                out.add(a.asname != null ? a.asname : a.name);
            }
        } else if (s instanceof Node.NamedExpr) {
            out.add(((Node.NamedExpr) s).target.id);
        } else if (s instanceof Node.Delete) {
            for (Expr t : ((Node.Delete) s).targets) {
                targetNames(t, out);
            }
        } else if (s instanceof Global) {
            out.addAll(((Global) s).names);
        }
        final Set<String> o = out;
        children(s, x -> bound(x, o));
    }

    private static void targetNames(Expr t, Set<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr || t instanceof ListExpr) {
            for (Expr e : elts(t)) {
                targetNames(e, out);
            }
        } else if (t instanceof Starred) {
            targetNames(((Starred) t).value, out);
        }
    }

    private static void globalsIn(Node s, Set<String> out) {
        if (s instanceof Global && !((Global) s).nonlocal) {
            out.addAll(((Global) s).names);
        }
        children(s, x -> globalsIn(x, out));
    }

    /// Every name `o` reads or writes, nested scopes included.
    private static void names(Node o, Set<String> out) {
        if (o instanceof Name) {
            out.add(((Name) o).id);
        }
        children(o, x -> names(x, out));
    }

    // ---- the walk -------------------------------------------------------------

    /// Rewrites the functions in `s`; `enclosing` holds what each enclosing
    /// function binds, innermost last.
    private void scopes(Node s, Deque<Set<String>> enclosing, boolean sumFree) {
        if (s instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) s;
            Set<String> mine = new HashSet<String>(d.args.names());
            for (Stmt x : d.body) {
                bound(x, mine);
            }
            enclosing.addLast(mine);
            if (sumFree) {
                sums(d.body, mine);
            }
            inline(d.body, enclosing, d.name, 0);
            for (Stmt x : d.body) {
                scopes(x, enclosing, sumFree);
            }
            enclosing.removeLast();
            return;
        }
        if (s instanceof ClassDef) {
            for (Stmt x : ((ClassDef) s).body) {
                scopes(x, enclosing, sumFree);
            }
            return;
        }
        children(s, x -> {
            if (x instanceof Stmt) {
                scopes(x, enclosing, sumFree);
            }
        });
    }

    /// `n = sum(e for x in it if c)` in `body` (and its nested blocks) as
    /// `t = []`, a loop appending `e`, and `n = sum(t)`.
    private void sums(List<Stmt> body, Set<String> mine) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            List<Stmt> out = sumLoop(s, mine);
            if (out != null) {
                body.remove(i);
                body.addAll(i, out);
                i += out.size() - 1;
                continue;
            }
            for (List<Stmt> b : blocks(s)) {
                sums(b, mine);
            }
        }
    }

    private List<Stmt> sumLoop(Stmt s, Set<String> mine) {
        Expr v;
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
            v = ((Assign) s).value;
        } else if (s instanceof AugAssign && ((AugAssign) s).target instanceof Name) {
            v = ((AugAssign) s).value;
        } else {
            return null;
        }
        if (!(v instanceof Call)) {
            return null;
        }
        Call c = (Call) v;
        if (!(c.func instanceof Name) || !"sum".equals(((Name) c.func).id) || mine.contains("sum")
                || c.args.size() != 1 || !c.keywords.isEmpty() || !(c.args.get(0) instanceof Comp)) {
            return null;
        }
        Comp g = (Comp) c.args.get(0);
        if (g.kind != Comp.Kind.GENERATOR || g.generators.size() != 1) {
            return null;
        }
        Comprehension q = g.generators.get(0);
        if (q.isAsync || !(q.target instanceof Name) || !plainExpr(g.elt) || !plainExpr(q.iter) || !allPlain(q.ifs)) {
            return null;
        }
        int k = ++counter;
        String list = "$sum" + k;
        Map<String, String> ren = new HashMap<String, String>();
        ren.put(((Name) q.target).id, "$sum" + k + "$" + ((Name) q.target).id);
        Copy cp = new Copy(ren);
        Call app = new Call(new Attribute(new Name(list).at(s.line, s.col), "append").at(s.line, s.col))
                .at(s.line, s.col);
        app.args.add(cp.expr(g.elt));
        List<Stmt> inner = new ArrayList<Stmt>();
        inner.add(new ExprStmt(app).at(s.line, s.col));
        for (int i = q.ifs.size() - 1; i >= 0; i--) {
            List<Stmt> wrapped = new ArrayList<Stmt>();
            wrapped.add(new If(cp.expr(q.ifs.get(i)), inner, new ArrayList<Stmt>()).at(s.line, s.col));
            inner = wrapped;
        }
        List<Stmt> out = new ArrayList<Stmt>();
        List<Expr> lt = new ArrayList<Expr>();
        lt.add(new Name(list).at(s.line, s.col));
        out.add(new Assign(lt, new ListExpr(new ArrayList<Expr>()).at(s.line, s.col)).at(s.line, s.col));
        out.add(new For(cp.expr(q.target), q.iter, inner, new ArrayList<Stmt>(), false).at(s.line, s.col));
        Call sum = new Call(c.func).at(c.line, c.col);
        sum.args.add(new Name(list).at(s.line, s.col));
        if (s instanceof Assign) {
            out.add(new Assign(((Assign) s).targets, sum).at(s.line, s.col));
        } else {
            out.add(new AugAssign(((AugAssign) s).target, ((AugAssign) s).op, sum).at(s.line, s.col));
        }
        mine.add(list);
        mine.add(ren.values().iterator().next());
        return out;
    }

    /// Inlines every qualifying loop in `body`, its nested blocks included.
    private void inline(List<Stmt> body, Deque<Set<String>> enclosing, String self, int depth) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            List<Stmt> out = s instanceof For ? inlineLoop((For) s, enclosing, self) : null;
            if (out != null) {
                body.remove(i);
                body.addAll(i, out);
                if (depth + 1 < DEPTH) {
                    // The inlined body may itself loop over a generator.
                    List<Stmt> again = new ArrayList<Stmt>(out);
                    inline(again, enclosing, self, depth + 1);
                    body.subList(i, i + out.size()).clear();
                    body.addAll(i, again);
                    i += again.size() - 1;
                } else {
                    i += out.size() - 1;
                }
                continue;
            }
            for (List<Stmt> b : blocks(s)) {
                inline(b, enclosing, self, depth);
            }
        }
    }

    private List<Stmt> inlineLoop(For f, Deque<Set<String>> enclosing, String self) {
        if (f.isAsync || !f.orelse.isEmpty() || !(f.target instanceof Name) || !(f.iter instanceof Call)) {
            return null;
        }
        Call c = (Call) f.iter;
        if (!(c.func instanceof Name) || !c.keywords.isEmpty()) {
            return null;
        }
        FunctionDef g = gens.get(((Name) c.func).id);
        if (g == null || g.name.equals(self) || c.args.size() != g.args.positional.size()) {
            return null;
        }
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                return null;
            }
        }
        for (Set<String> b : enclosing) {
            if (b.contains(g.name)) {
                return null;
            }
        }
        if (leaves(f.body)) {
            return null;
        }
        Set<String> local = new HashSet<String>(g.args.names());
        for (Stmt x : g.body) {
            bound(x, local);
        }
        Set<String> used = new HashSet<String>();
        for (Stmt x : g.body) {
            names(x, used);
        }
        used.removeAll(local);
        for (Set<String> b : enclosing) {
            for (String n : used) {
                if (b.contains(n)) {
                    return null;
                }
            }
        }
        int k = ++counter;
        Map<String, String> ren = new LinkedHashMap<String, String>();
        for (String n : local) {
            ren.put(n, g.name + "$" + k + "$" + n);
        }
        Copy cp = new Copy(ren);
        cp.target = f.target;
        cp.loopBody = f.body;
        List<Stmt> out = new ArrayList<Stmt>();
        List<String> params = g.args.names();
        // A parameter whose one use is the iterable of the body's first
        // loop takes the argument itself there, so that loop can be inlined
        // in its turn; the other arguments must be names or constants, so
        // that evaluating it there instead reorders nothing observable.
        int sub = substitutable(g, c);
        for (int i = 0; i < params.size(); i++) {
            if (i == sub) {
                cp.subst.put(params.get(i), c.args.get(i));
                continue;
            }
            List<Expr> t = new ArrayList<Expr>();
            t.add(new Name(ren.get(params.get(i))).at(f.line, f.col));
            out.add(new Assign(t, c.args.get(i)).at(f.line, f.col));
        }
        out.addAll(cp.stmts(g.body));
        Set<String> innermost = enclosing.peekLast();
        if (innermost != null) {
            innermost.addAll(ren.values());
        }
        return out;
    }

    private static int substitutable(FunctionDef g, Call c) {
        if (g.body.isEmpty() || !(g.body.get(0) instanceof For) || !(((For) g.body.get(0)).iter instanceof Name)) {
            return -1;
        }
        String p = ((Name) ((For) g.body.get(0)).iter).id;
        int i = g.args.names().indexOf(p);
        if (i < 0) {
            return -1;
        }
        Set<String> all = new HashSet<String>();
        int[] uses = {0};
        for (Stmt x : g.body) {
            count(x, p, uses);
            bound(x, all);
        }
        if (uses[0] != 1 || all.contains(p)) {
            return -1;
        }
        for (int j = 0; j < c.args.size(); j++) {
            Expr a = c.args.get(j);
            if (j != i && !(a instanceof Name || a instanceof Constant)) {
                return -1;
            }
        }
        return i;
    }

    private static void count(Node o, String n, int[] uses) {
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            uses[0]++;
        }
        children(o, x -> count(x, n, uses));
    }

    /// Whether `body` has a `break` or `continue` of its own loop.
    private static boolean leaves(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof Break || s instanceof Continue) {
                return true;
            }
            if (s instanceof For || s instanceof While) {
                List<Stmt> orelse = s instanceof For ? ((For) s).orelse : ((While) s).orelse;
                if (leaves(orelse)) {
                    return true;
                }
                continue;
            }
            if (s instanceof FunctionDef || s instanceof ClassDef) {
                continue;
            }
            for (List<Stmt> b : blocks(s)) {
                if (leaves(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// The statement lists directly inside `s`.
    private static List<List<Stmt>> blocks(Stmt s) {
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
        } else if (s instanceof Node.With) {
            r.add(((Node.With) s).body);
        } else if (s instanceof Node.Try) {
            Node.Try t = (Node.Try) s;
            r.add(t.body);
            for (Node.ExceptHandler h : t.handlers) {
                r.add(h.body);
            }
            r.add(t.orelse);
            r.add(t.finalbody);
        }
        return r;
    }

    /// Calls `v` for every AST node directly under `o`.
    private static void children(Node o, Visit v) {
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Class<?> t = f.getType();
                if (!Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t)) {
                    continue;
                }
                Object x;
                try {
                    x = f.get(o);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                if (x instanceof List) {
                    for (Object y : (List<?>) x) {
                        if (y instanceof Node) {
                            v.on((Node) y);
                        }
                    }
                } else if (x instanceof Node) {
                    v.on((Node) x);
                }
            }
        }
    }

    private interface Visit {
        void on(Node n);
    }

    // ---- the copy -------------------------------------------------------------

    /// A copy of a generator body with its locals renamed, its yield
    /// replaced by the loop's target assignment and body, and a substituted
    /// parameter replaced by its argument.
    private static final class Copy {
        private final Map<String, String> ren;
        final Map<String, Expr> subst = new HashMap<String, Expr>();
        Expr target;
        List<Stmt> loopBody;

        Copy(Map<String, String> ren) {
            this.ren = ren;
        }

        List<Stmt> stmts(List<Stmt> in) {
            List<Stmt> out = new ArrayList<Stmt>();
            for (Stmt s : in) {
                if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Yield) {
                    Yield y = (Yield) ((ExprStmt) s).value;
                    List<Expr> t = new ArrayList<Expr>();
                    t.add(target);
                    Expr v = y.value != null ? expr(y.value)
                            : new Constant(Constant.Kind.NONE, "None").at(s.line, s.col);
                    out.add(new Assign(t, v).at(s.line, s.col));
                    out.addAll(loopBody);
                    continue;
                }
                out.add(stmt(s));
            }
            return out;
        }

        private Stmt stmt(Stmt s) {
            Stmt r;
            if (s instanceof ExprStmt) {
                r = new ExprStmt(expr(((ExprStmt) s).value));
            } else if (s instanceof Assign) {
                r = new Assign(exprs(((Assign) s).targets), expr(((Assign) s).value));
            } else if (s instanceof AugAssign) {
                AugAssign a = (AugAssign) s;
                r = new AugAssign(expr(a.target), a.op, expr(a.value));
            } else if (s instanceof Pass) {
                r = new Pass();
            } else if (s instanceof Break) {
                r = new Break();
            } else if (s instanceof Continue) {
                r = new Continue();
            } else if (s instanceof Assert) {
                r = new Assert(expr(((Assert) s).test), expr(((Assert) s).msg));
            } else if (s instanceof If) {
                If i = (If) s;
                r = new If(expr(i.test), stmts(i.body), stmts(i.orelse));
            } else if (s instanceof While) {
                While w = (While) s;
                r = new While(expr(w.test), stmts(w.body), stmts(w.orelse));
            } else {
                For f = (For) s;
                r = new For(expr(f.target), expr(f.iter), stmts(f.body), stmts(f.orelse), false);
            }
            return r.at(s.line, s.col);
        }

        private List<Expr> exprs(List<Expr> in) {
            List<Expr> out = new ArrayList<Expr>();
            for (Expr e : in) {
                out.add(expr(e));
            }
            return out;
        }

        Expr expr(Expr e) {
            if (e == null) {
                return null;
            }
            Expr r;
            if (e instanceof Name) {
                String n = ((Name) e).id;
                Expr s = subst.get(n);
                if (s != null) {
                    return s;
                }
                String m = ren.get(n);
                r = new Name(m != null ? m : n);
            } else if (e instanceof Constant) {
                r = new Constant(((Constant) e).kind, ((Constant) e).text);
            } else if (e instanceof Attribute) {
                r = new Attribute(expr(((Attribute) e).value), ((Attribute) e).attr);
            } else if (e instanceof Subscript) {
                r = new Subscript(expr(((Subscript) e).value), expr(((Subscript) e).index));
            } else if (e instanceof Slice) {
                Slice s = (Slice) e;
                r = new Slice(expr(s.lower), expr(s.upper), expr(s.step));
            } else if (e instanceof Starred) {
                r = new Starred(expr(((Starred) e).value));
            } else if (e instanceof BinOp) {
                BinOp b = (BinOp) e;
                r = new BinOp(expr(b.left), b.op, expr(b.right));
            } else if (e instanceof UnaryOp) {
                r = new UnaryOp(((UnaryOp) e).op, expr(((UnaryOp) e).operand));
            } else if (e instanceof BoolOp) {
                r = new BoolOp(((BoolOp) e).isAnd, exprs(((BoolOp) e).values));
            } else if (e instanceof Compare) {
                Compare c = (Compare) e;
                Compare n = new Compare(expr(c.left));
                n.ops.addAll(c.ops);
                n.comparators.addAll(exprs(c.comparators));
                r = n;
            } else if (e instanceof IfExp) {
                IfExp i = (IfExp) e;
                r = new IfExp(expr(i.test), expr(i.body), expr(i.orelse));
            } else if (e instanceof Call) {
                Call c = (Call) e;
                Call n = new Call(expr(c.func));
                n.args.addAll(exprs(c.args));
                for (Keyword k : c.keywords) {
                    n.keywords.add(new Keyword(k.arg, expr(k.value)).at(k.line, k.col));
                }
                r = n;
            } else if (e instanceof ListExpr) {
                r = new ListExpr(exprs(((ListExpr) e).elts));
            } else if (e instanceof TupleExpr) {
                r = new TupleExpr(exprs(((TupleExpr) e).elts));
            } else {
                r = new SetExpr(exprs(((SetExpr) e).elts));
            }
            return r.at(e.line, e.col);
        }
    }
}
