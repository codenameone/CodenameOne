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

import com.codename1.python.transpiler.ast.Node.*;
import com.codename1.python.transpiler.ast.Scope;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Finds the locals whose list a new slice copy may be written into, in
/// place of a new list: `perm = perm1[:]` in a loop then copies into the list
/// `perm` held before, which nothing else can still see.
///
/// A local qualifies when every assignment to it is a slice copy
/// (`x = y[a:b]`) or `x = []`, each of which makes a list nobody else holds,
/// and every read of it is the object of a subscript (`x[i]`, `x[i] = v`,
/// `x[a:b]`), the receiver of `x.append(v)` or the argument of a literal's
/// `"sep".join(x)`, none of which hands the list itself anywhere. Its list is then reachable
/// through the local alone, so overwriting it is unobservable. Parameters
/// never qualify (the caller holds their first value), nor does a local a
/// nested scope captures. Any construct the walk does not know makes it give
/// up on the whole function.
final class ListRecycling {
    private final Set<String> bad = new HashSet<String>();
    private final Set<String> fresh = new HashSet<String>();
    private boolean unknown;

    private ListRecycling() {
    }

    static Set<String> find(List<Stmt> body, List<String> params, Scope fs) {
        ListRecycling r = new ListRecycling();
        r.stmts(body, false);
        Set<String> out = new HashSet<String>();
        if (r.unknown) {
            return out;
        }
        for (String n : r.fresh) {
            if (!r.bad.contains(n) && !params.contains(n) && fs.isLocal(n) && !fs.cells.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    static boolean isSliceCopy(Expr e) {
        return e instanceof Subscript && ((Subscript) e).index instanceof Slice;
    }

    static boolean isEmptyList(Expr e) {
        return e instanceof ListExpr && ((ListExpr) e).elts.isEmpty();
    }

    /// Whether `x` in this call is used without being handed anywhere:
    /// `x.append(v)`, or `"sep".join(x)` on a literal, which is always str's
    /// own join and keeps nothing.
    private static boolean harmlessCall(Call c) {
        if (!c.keywords.isEmpty() || c.args.size() != 1 || !(c.func instanceof Attribute)) {
            return false;
        }
        Attribute f = (Attribute) c.func;
        if ("append".equals(f.attr) && f.value instanceof Name) {
            return true;
        }
        return "join".equals(f.attr) && f.value instanceof Constant
                && ((Constant) f.value).kind == Constant.Kind.STR && c.args.get(0) instanceof Name;
    }

    private void stmts(List<Stmt> body, boolean esc) {
        if (body == null) {
            return;
        }
        for (Stmt s : body) {
            stmt(s, esc);
        }
    }

    private void stmt(Stmt s, boolean esc) {
        if (s instanceof ExprStmt) {
            expr(((ExprStmt) s).value, esc);
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            expr(a.value, esc);
            if (!esc && a.targets.size() == 1 && a.targets.get(0) instanceof Name && (isSliceCopy(a.value) || isEmptyList(a.value))) {
                fresh.add(((Name) a.targets.get(0)).id);
                return;
            }
            for (Expr t : a.targets) {
                target(t, esc);
            }
        } else if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            expr(a.value, esc);
            target(a.target, esc);
        } else if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            expr(a.value, esc);
            target(a.target, esc);
        } else if (s instanceof Return) {
            expr(((Return) s).value, esc);
        } else if (s instanceof Delete) {
            for (Expr t : ((Delete) s).targets) {
                target(t, esc);
            }
        } else if (s instanceof Raise) {
            expr(((Raise) s).exc, esc);
            expr(((Raise) s).cause, esc);
        } else if (s instanceof Assert) {
            expr(((Assert) s).test, esc);
            expr(((Assert) s).msg, esc);
        } else if (s instanceof Global) {
            bad.addAll(((Global) s).names);
        } else if (s instanceof If) {
            expr(((If) s).test, esc);
            stmts(((If) s).body, esc);
            stmts(((If) s).orelse, esc);
        } else if (s instanceof While) {
            expr(((While) s).test, esc);
            stmts(((While) s).body, esc);
            stmts(((While) s).orelse, esc);
        } else if (s instanceof For) {
            For f = (For) s;
            expr(f.iter, esc);
            target(f.target, esc);
            stmts(f.body, esc);
            stmts(f.orelse, esc);
        } else if (s instanceof With) {
            for (WithItem w : ((With) s).items) {
                expr(w.context, esc);
                if (w.vars != null) {
                    target(w.vars, esc);
                }
            }
            stmts(((With) s).body, esc);
        } else if (s instanceof Try) {
            Try t = (Try) s;
            stmts(t.body, esc);
            for (ExceptHandler h : t.handlers) {
                expr(h.type, esc);
                if (h.name != null) {
                    bad.add(h.name);
                }
                stmts(h.body, esc);
            }
            stmts(t.orelse, esc);
            stmts(t.finalbody, esc);
        } else if (s instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) s;
            bad.add(f.name);
            exprs(f.decorators, true);
            exprs(f.args.defaults, true);
            exprs(f.args.kwDefaults, true);
            stmts(f.body, true);
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            bad.add(c.name);
            exprs(c.decorators, true);
            exprs(c.bases, true);
            for (Keyword k : c.keywords) {
                expr(k.value, true);
            }
            stmts(c.body, true);
        } else if (!(s instanceof Pass || s instanceof Break || s instanceof Continue)) {
            unknown = true;
        }
    }

    /// A store target: a name stored by anything but a slice copy is out; a
    /// subscript stores into its object without handing it anywhere.
    private void target(Expr t, boolean esc) {
        if (t instanceof Name) {
            bad.add(((Name) t).id);
        } else if (t instanceof Subscript) {
            expr(t, esc);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                target(e, esc);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                target(e, esc);
            }
        } else if (t instanceof Starred) {
            target(((Starred) t).value, esc);
        } else {
            expr(t, esc);
        }
    }

    private void exprs(List<Expr> es, boolean esc) {
        if (es == null) {
            return;
        }
        for (Expr e : es) {
            expr(e, esc);
        }
    }

    private void expr(Expr e, boolean esc) {
        if (e == null || e instanceof Constant) {
            return;
        }
        if (e instanceof Name) {
            bad.add(((Name) e).id);
        } else if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            if (esc || !(s.value instanceof Name)) {
                expr(s.value, esc);
            }
            expr(s.index, esc);
        } else if (e instanceof Slice) {
            expr(((Slice) e).lower, esc);
            expr(((Slice) e).upper, esc);
            expr(((Slice) e).step, esc);
        } else if (e instanceof BinOp) {
            expr(((BinOp) e).left, esc);
            expr(((BinOp) e).right, esc);
        } else if (e instanceof UnaryOp) {
            expr(((UnaryOp) e).operand, esc);
        } else if (e instanceof BoolOp) {
            exprs(((BoolOp) e).values, esc);
        } else if (e instanceof Compare) {
            expr(((Compare) e).left, esc);
            exprs(((Compare) e).comparators, esc);
        } else if (e instanceof IfExp) {
            expr(((IfExp) e).test, esc);
            expr(((IfExp) e).body, esc);
            expr(((IfExp) e).orelse, esc);
        } else if (e instanceof Attribute) {
            expr(((Attribute) e).value, esc);
        } else if (e instanceof Call) {
            Call c = (Call) e;
            if (!esc && harmlessCall(c)) {
                Attribute f = (Attribute) c.func;
                expr(f.value instanceof Name ? null : f.value, esc);
                expr(f.value instanceof Name ? c.args.get(0) : null, esc);
                return;
            }
            expr(c.func, esc);
            exprs(c.args, esc);
            for (Keyword k : c.keywords) {
                expr(k.value, esc);
            }
        } else if (e instanceof Starred) {
            expr(((Starred) e).value, esc);
        } else if (e instanceof NamedExpr) {
            bad.add(((NamedExpr) e).target.id);
            expr(((NamedExpr) e).value, esc);
        } else if (e instanceof ListExpr) {
            exprs(((ListExpr) e).elts, esc);
        } else if (e instanceof TupleExpr) {
            exprs(((TupleExpr) e).elts, esc);
        } else if (e instanceof SetExpr) {
            exprs(((SetExpr) e).elts, esc);
        } else if (e instanceof DictExpr) {
            exprs(((DictExpr) e).keys, esc);
            exprs(((DictExpr) e).values, esc);
        } else if (e instanceof JoinedStr) {
            exprs(((JoinedStr) e).parts, esc);
        } else if (e instanceof FormattedValue) {
            expr(((FormattedValue) e).value, esc);
            expr(((FormattedValue) e).spec, esc);
        } else if (e instanceof Await) {
            expr(((Await) e).value, esc);
        } else if (e instanceof Yield) {
            expr(((Yield) e).value, esc);
        } else if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            exprs(l.args.defaults, true);
            exprs(l.args.kwDefaults, true);
            expr(l.body, true);
        } else if (e instanceof Comp) {
            Comp c = (Comp) e;
            expr(c.elt, true);
            expr(c.value, true);
            for (Comprehension g : c.generators) {
                expr(g.target, true);
                expr(g.iter, true);
                exprs(g.ifs, true);
            }
        } else {
            unknown = true;
        }
    }
}
