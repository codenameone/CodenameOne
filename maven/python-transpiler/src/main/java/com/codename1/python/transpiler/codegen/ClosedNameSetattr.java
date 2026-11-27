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

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.Arg;
import com.codename1.python.transpiler.ast.Node.Arguments;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Compare;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.DictExpr;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.TupleExpr;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// `setattr(obj, prop[0], value)` where, in one function, `prop = table[k]`
/// is the only binding of `prop` and `table = {k1: ("name1", ...), ...}`
/// the only binding of `table`, and both names are only ever read by
/// subscript (or tested with `in`): the name stored is one of the literals
/// at that index of the table's tuples, a set closed at build time. Every
/// row of the table gets one more element, `lambda v: setattr(obj,
/// "name1", v)`, built from its own literal, and the statement becomes
/// `prop[<that index>](value)`: the row the data selects carries its own
/// store, so nothing compares attribute names at run time. openpyxl's
/// `WorkSheetParser.parse` stores each parsed property element under the
/// attribute its table names.
///
/// A dict literal cannot change while nothing stores into it, deletes from
/// it or passes it anywhere, which the "only read by subscript" rule
/// enforces; a tuple never changes. Any other use of either name, a row
/// read other than by an int index below the original length, a second
/// binding anywhere in the function (nested scopes included), a parameter,
/// `global`/`nonlocal`, an `obj` that is not an unrebound parameter, or a
/// module binding `setattr` leaves the call as it is.
public final class ClosedNameSetattr {
    private ClosedNameSetattr() {
    }

    public static void rewrite(Map<String, Node.Module> trees) {
        for (Node.Module m : trees.values()) {
            if (bindsName(m.body, "setattr")) {
                continue;
            }
            functions(m, new ArrayList<FunctionDef>()).forEach(ClosedNameSetattr::rewrite);
        }
    }

    private static List<FunctionDef> functions(Object o, List<FunctionDef> out) {
        if (o instanceof FunctionDef) {
            out.add((FunctionDef) o);
        }
        Infer.children(o, x -> functions(x, out));
        return out;
    }

    private static boolean bindsName(Object o, String n) {
        boolean[] r = new boolean[1];
        scanBindings(o, n, r);
        return r[0];
    }

    /// Whether anything in `o` other than a plain assignment binds `n`.
    private static void scanBindings(Object o, String n, boolean[] r) {
        if (r[0]) {
            return;
        }
        if (o instanceof FunctionDef && ((FunctionDef) o).name.equals(n)
                || o instanceof ClassDef && ((ClassDef) o).name.equals(n)
                || o instanceof Node.ExceptHandler && n.equals(((Node.ExceptHandler) o).name)
                || o instanceof Alias && n.equals(((Alias) o).asname != null ? ((Alias) o).asname : ((Alias) o).name)
                || o instanceof Node.Global && ((Node.Global) o).names.contains(n)
                || o instanceof Arguments && ((Arguments) o).names().contains(n)) {
            r[0] = true;
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (t instanceof Name && ((Name) t).id.equals(n)) {
                    r[0] = true;
                    return;
                }
            }
        }
        Infer.children(o, x -> scanBindings(x, n, r));
    }

    private static void rewrite(FunctionDef f) {
        List<ExprStmt> calls = new ArrayList<ExprStmt>();
        collectCalls(f.body, calls);
        Map<String, Integer> slots = new java.util.HashMap<String, Integer>();
        Map<ExprStmt, Stmt> done = new IdentityHashMap<ExprStmt, Stmt>();
        for (ExprStmt s : calls) {
            Call c = (Call) s.value;
            Subscript nameExpr = (Subscript) c.args.get(1);
            String prop = ((Name) nameExpr.value).id;
            int idx = Integer.parseInt(((Constant) nameExpr.index).text);
            if (!(c.args.get(0) instanceof Name)) {
                continue;
            }
            String obj = ((Name) c.args.get(0)).id;
            DictExpr d = table(f, prop, idx, obj);
            if (d == null) {
                continue;
            }
            String key = prop + "|" + idx + "|" + obj;
            Integer slot = slots.get(key);
            if (slot == null) {
                slot = ((TupleExpr) d.values.get(0)).elts.size();
                for (Expr v : d.values) {
                    String attr = ((Constant) ((TupleExpr) v).elts.get(idx)).text;
                    ((TupleExpr) v).elts.add(setter(obj, attr, v));
                }
                slots.put(key, slot);
            }
            Call call = new Call(new Subscript(new Name(prop).at(c.line, c.col),
                    new Constant(Constant.Kind.INT, String.valueOf(slot)).at(c.line, c.col))
                    .at(c.line, c.col)).at(c.line, c.col);
            call.args.add(c.args.get(2));
            done.put(s, new ExprStmt(call).at(s.line, s.col));
        }
        if (!done.isEmpty()) {
            replace(f.body, done);
        }
    }

    /// `lambda v: setattr(obj, "attr", v)`: the store of one table row,
    /// spelled with its literal name.
    private static Node.Lambda setter(String obj, String attr, Expr at) {
        Arguments a = new Arguments();
        a.positional.add(new Arg("v", null));
        Call store = new Call(new Name("setattr").at(at.line, at.col)).at(at.line, at.col);
        store.args.add(new Name(obj).at(at.line, at.col));
        store.args.add(new Constant(Constant.Kind.STR, attr).at(at.line, at.col));
        store.args.add(new Name("v").at(at.line, at.col));
        Node.Lambda l = new Node.Lambda(a, store);
        l.at(at.line, at.col);
        return l;
    }

    /// Replaces each statement of `done` in `l` and the statement lists
    /// nested in it (the calls were collected outside nested scopes).
    private static void replace(List<Stmt> l, Map<ExprStmt, Stmt> done) {
        for (int i = 0; i < l.size(); i++) {
            Stmt st = l.get(i);
            Stmt r = st instanceof ExprStmt ? done.get(st) : null;
            if (r != null) {
                l.set(i, r);
            } else if (st instanceof If) {
                replace(((If) st).body, done);
                replace(((If) st).orelse, done);
            } else if (st instanceof Node.For) {
                replace(((Node.For) st).body, done);
                replace(((Node.For) st).orelse, done);
            } else if (st instanceof Node.While) {
                replace(((Node.While) st).body, done);
                replace(((Node.While) st).orelse, done);
            } else if (st instanceof Node.With) {
                replace(((Node.With) st).body, done);
            } else if (st instanceof Node.Try) {
                replace(((Node.Try) st).body, done);
                for (Node.ExceptHandler h : ((Node.Try) st).handlers) {
                    replace(h.body, done);
                }
                replace(((Node.Try) st).orelse, done);
                replace(((Node.Try) st).finalbody, done);
            } else if (st instanceof Node.Match) {
                for (Node.MatchCase c : ((Node.Match) st).cases) {
                    replace(c.body, done);
                }
            }
        }
    }

    /// Every `setattr(a, p[<int>], b)` statement, skipping nested scopes.
    private static void collectCalls(Object o, List<ExprStmt> out) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Lambda) {
            return;
        }
        if (o instanceof ExprStmt && ((ExprStmt) o).value instanceof Call) {
            Call c = (Call) ((ExprStmt) o).value;
            if (c.func instanceof Name && "setattr".equals(((Name) c.func).id) && c.args.size() == 3
                    && c.keywords.isEmpty() && c.args.get(1) instanceof Subscript
                    && ((Subscript) c.args.get(1)).value instanceof Name
                    && ((Subscript) c.args.get(1)).index instanceof Constant
                    && ((Constant) ((Subscript) c.args.get(1)).index).kind == Constant.Kind.INT
                    && ((Constant) ((Subscript) c.args.get(1)).index).text.matches("[0-9]{1,3}")
                    && noStarred(c)) {
                out.add((ExprStmt) o);
            }
        }
        Infer.children(o, x -> collectCalls(x, out));
    }

    private static boolean noStarred(Call c) {
        for (Expr a : c.args) {
            if (a instanceof Node.Starred) {
                return false;
            }
        }
        return true;
    }

    /// The table `prop` is read from when `prop[idx]` is always one of the
    /// literal attribute names at `idx` of its rows, every row has the same
    /// length, `prop` is only ever subscripted by an int below that length,
    /// the table only by `prop`'s one binding (or tested with `in`), and
    /// `obj` is a parameter `f` never rebinds. Then a setter appended to
    /// every row cannot be seen by anything but the rewritten call, and
    /// `obj` names the same object when the table is built as when the
    /// call runs. Otherwise null.
    private static DictExpr table(FunctionDef f, String prop, int idx, String obj) {
        if (f.args.names().contains(prop) || !f.args.names().contains(obj)
                || obj.equals(prop) || rebinds(f, obj)) {
            return null;
        }
        Assign pa = soleAssign(f, prop);
        if (pa == null || !(pa.value instanceof Subscript) || !(((Subscript) pa.value).value instanceof Name)) {
            return null;
        }
        String table = ((Name) ((Subscript) pa.value).value).id;
        if (table.equals(prop) || table.equals(obj) || f.args.names().contains(table)) {
            return null;
        }
        Assign ta = soleAssign(f, table);
        if (ta == null || !(ta.value instanceof DictExpr) || !readOnly(f, prop, pa) || !readOnly(f, table, ta)) {
            return null;
        }
        DictExpr d = (DictExpr) ta.value;
        if (d.values.isEmpty()) {
            return null;
        }
        int len = -1;
        for (int i = 0; i < d.values.size(); i++) {
            Expr v = d.values.get(i);
            if (d.keys.get(i) == null || !(v instanceof TupleExpr) || ((TupleExpr) v).elts.size() <= idx) {
                return null;
            }
            List<Expr> elts = ((TupleExpr) v).elts;
            if (len >= 0 && elts.size() != len) {
                return null;
            }
            len = elts.size();
            for (Expr e : elts) {
                if (e instanceof Node.Starred) {
                    return null;
                }
            }
            Expr e = elts.get(idx);
            if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.STR
                    || !((Constant) e).text.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return null;
            }
        }
        List<Subscript> subs = new ArrayList<Subscript>();
        subscripts(f.body, prop, subs);
        for (Subscript sub : subs) {
            if (!(sub.index instanceof Constant) || ((Constant) sub.index).kind != Constant.Kind.INT
                    || !((Constant) sub.index).text.matches("[0-9]{1,3}")
                    || Integer.parseInt(((Constant) sub.index).text) >= len) {
                return null;
            }
        }
        subs.clear();
        subscripts(f.body, table, subs);
        if (subs.size() != 1 || subs.get(0) != pa.value) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        return d;
    }

    /// Whether anything in `f` binds `n` (its parameters aside).
    private static boolean rebinds(FunctionDef f, String n) {
        List<Assign> found = new ArrayList<Assign>();
        boolean[] other = new boolean[1];
        sole(f.body, n, found, other);
        return other[0] || !found.isEmpty();
    }

    /// Every subscript of the name `n` in `o`.
    private static void subscripts(Object o, String n, List<Subscript> out) {
        if (o instanceof Subscript && ((Subscript) o).value instanceof Name
                && ((Name) ((Subscript) o).value).id.equals(n)) {
            out.add((Subscript) o);
        }
        Infer.children(o, x -> subscripts(x, n, out));
    }

    /// The one `n = ...` of `f` when nothing else in it binds `n`.
    private static Assign soleAssign(FunctionDef f, String n) {
        List<Assign> found = new ArrayList<Assign>();
        boolean[] other = new boolean[1];
        sole(f.body, n, found, other);
        return other[0] || found.size() != 1 ? null : found.get(0);
    }

    private static void sole(Object o, String n, List<Assign> found, boolean[] other) {
        if (o instanceof Assign && ((Assign) o).targets.size() == 1 && ((Assign) o).targets.get(0) instanceof Name
                && ((Name) ((Assign) o).targets.get(0)).id.equals(n)) {
            found.add((Assign) o);
            Infer.children(((Assign) o).value, x -> sole(x, n, found, other));
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (!(t instanceof Name) && bindsTarget(t, n)) {
                    other[0] = true;
                }
            }
        }
        if (o instanceof FunctionDef && (((FunctionDef) o).name.equals(n) || ((FunctionDef) o).args.names().contains(n))
                || o instanceof ClassDef && ((ClassDef) o).name.equals(n)
                || o instanceof Node.ExceptHandler && n.equals(((Node.ExceptHandler) o).name)
                || o instanceof Alias && n.equals(((Alias) o).asname != null ? ((Alias) o).asname : ((Alias) o).name)
                || o instanceof Node.Global && ((Node.Global) o).names.contains(n)
                || o instanceof Node.Lambda && ((Node.Lambda) o).args.names().contains(n)
                || o instanceof Node.NamedExpr && ((Node.NamedExpr) o).target.id.equals(n)
                || o instanceof Node.AugAssign && bindsTarget(((Node.AugAssign) o).target, n)
                || o instanceof Node.AnnAssign && bindsTarget(((Node.AnnAssign) o).target, n)
                || o instanceof Node.Delete && ((Node.Delete) o).targets.stream().anyMatch(t -> bindsTarget(t, n))
                || o instanceof Node.For && bindsTarget(((Node.For) o).target, n)
                || o instanceof Node.Comprehension && bindsTarget(((Node.Comprehension) o).target, n)
                || o instanceof Node.WithItem && ((Node.WithItem) o).vars != null && bindsTarget(((Node.WithItem) o).vars, n)) {
            other[0] = true;
        }
        Infer.children(o, x -> sole(x, n, found, other));
    }

    private static boolean bindsTarget(Expr t, String n) {
        if (t instanceof Name) {
            return ((Name) t).id.equals(n);
        }
        if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                if (bindsTarget(e, n)) {
                    return true;
                }
            }
        }
        if (t instanceof Node.ListExpr) {
            for (Expr e : ((Node.ListExpr) t).elts) {
                if (bindsTarget(e, n)) {
                    return true;
                }
            }
        }
        return t instanceof Node.Starred && bindsTarget(((Node.Starred) t).value, n);
    }

    /// Whether every occurrence of `n` in `f` other than `def`'s target is
    /// the value of a subscript that is read, or the right side of `in`.
    private static boolean readOnly(FunctionDef f, String n, Assign def) {
        Set<Object> stores = java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        targets(f.body, stores);
        boolean[] bad = new boolean[1];
        uses(f.body, null, n, def.targets.get(0), stores, bad);
        return !bad[0];
    }

    private static void targets(Object o, Set<Object> stores) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                addTarget(t, stores);
            }
        } else if (o instanceof Node.AugAssign) {
            addTarget(((Node.AugAssign) o).target, stores);
        } else if (o instanceof Node.AnnAssign) {
            addTarget(((Node.AnnAssign) o).target, stores);
        } else if (o instanceof Node.Delete) {
            for (Expr t : ((Node.Delete) o).targets) {
                addTarget(t, stores);
            }
        } else if (o instanceof Node.For) {
            addTarget(((Node.For) o).target, stores);
        } else if (o instanceof Node.Comprehension) {
            addTarget(((Node.Comprehension) o).target, stores);
        } else if (o instanceof Node.WithItem && ((Node.WithItem) o).vars != null) {
            addTarget(((Node.WithItem) o).vars, stores);
        }
        Infer.children(o, x -> targets(x, stores));
    }

    private static void addTarget(Expr t, Set<Object> stores) {
        stores.add(t);
        if (t instanceof TupleExpr) {
            ((TupleExpr) t).elts.forEach(e -> addTarget(e, stores));
        } else if (t instanceof Node.ListExpr) {
            ((Node.ListExpr) t).elts.forEach(e -> addTarget(e, stores));
        } else if (t instanceof Node.Starred) {
            addTarget(((Node.Starred) t).value, stores);
        }
    }

    private static void uses(Object o, Object parent, String n, Expr def, Set<Object> stores, boolean[] bad) {
        if (bad[0]) {
            return;
        }
        if (o instanceof Name && ((Name) o).id.equals(n) && o != def) { //NOPMD CompareObjectsWithEquals - identity is meant
            boolean ok = parent instanceof Subscript && ((Subscript) parent).value == o //NOPMD CompareObjectsWithEquals - identity is meant
                    && !stores.contains(parent);
            if (!ok && parent instanceof Compare) {
                Compare c = (Compare) parent;
                int i = c.comparators.indexOf(o);
                ok = i >= 0 && c.comparators.get(i) == o //NOPMD CompareObjectsWithEquals - identity is meant
                        && ("in".equals(c.ops.get(i)) || "not in".equals(c.ops.get(i)));
            }
            if (!ok) {
                bad[0] = true;
            }
            return;
        }
        Object p = o instanceof List ? parent : o;
        Infer.children(o, x -> uses(x, p, n, def, stores, bad));
    }
}
