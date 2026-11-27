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
import com.codename1.python.transpiler.ast.Node.Arg;
import com.codename1.python.transpiler.ast.Node.Arguments;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.DictExpr;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Specializes a function whose whole body stores a mapping argument as
/// attributes,
///
/// ```python
/// def fill(obj, attrs):
///     for k, v in attrs.items():
///         setattr(obj, k, v)
///
/// @classmethod
/// def configure(cls, **kw):
///     for k, v in kw.items():
///         setattr(cls, k, v)
/// ```
///
/// on the keys each call site spells out: a dict literal with string
/// keys for a positional mapping, the literal keywords for `**kw`. Every
/// distinct key sequence gets a clone whose body is one literal store per
/// key, in that order, from a parameter of its own (`obj.label = _cn1_kv0`).
/// Each call passes the values in place of the mapping, in the position
/// they were evaluated in, so evaluation order is unchanged and no mapping
/// is built; a dict iterates in insertion order, so the stores happen in
/// the order the loop made them. Each value reaches its store with its own
/// type, so the inference sees every type a field can hold.
///
/// Only module-level functions, `@classmethod` and `@staticmethod` methods
/// qualify (the implicit argument of a plain method depends on how it is
/// reached). It refuses a function with defaults, `*args` or keyword-only
/// parameters, a name defined twice or used other than by being called, a
/// call with starred arguments, `**` entries, a non-identifier or repeated
/// key, a private (mangled) key, and a module-level function called other
/// than as a statement. No name-dispatch code is generated.
public final class DictItemsSetattr {
    private static final String SUFFIX = "__cn1_d";
    private static final int[] NONE = new int[0];

    private DictItemsSetattr() {
    }

    public static void rewrite(Map<String, Node.Module> trees, Iterable<Node.Module> libTrees) {
        List<Node.Module> all = new ArrayList<Node.Module>(trees.values());
        for (Node.Module m : libTrees) {
            all.add(m);
        }
        for (Node.Module m : trees.values()) {
            for (int i = 0; i < m.body.size(); i++) {
                Stmt s = m.body.get(i);
                if (s instanceof FunctionDef && moduleFn(m, (FunctionDef) s, all)) {
                    i--;
                } else if (s instanceof ClassDef) {
                    ClassDef c = (ClassDef) s;
                    for (int j = 0; j < c.body.size(); j++) {
                        if (c.body.get(j) instanceof FunctionDef && method(c, (FunctionDef) c.body.get(j), all)) {
                            j--;
                        }
                    }
                }
            }
        }
    }

    /// The parameter positions of the loop: [object, mapping], mapping -1
    /// meaning `**kw`; empty when `f` is not exactly the loop.
    private static int[] shape(FunctionDef f) {
        Arguments a = f.args;
        if (f.isAsync || a.vararg != null || !a.kwonly.isEmpty() || !a.defaults.isEmpty()
                || a.positionalOnly != 0 || f.returns != null) {
            return NONE;
        }
        List<Stmt> body = f.body;
        int start = 0;
        if (body.size() == 2 && body.get(0) instanceof Node.ExprStmt
                && ((Node.ExprStmt) body.get(0)).value instanceof Constant) {
            start = 1;
        }
        if (body.size() != start + 1 || !(body.get(start) instanceof Node.For)) {
            return NONE;
        }
        Node.For loop = (Node.For) body.get(start);
        if (loop.isAsync || !loop.orelse.isEmpty() || loop.body.size() != 1
                || !(loop.target instanceof Node.TupleExpr) || !(loop.iter instanceof Call)) {
            return NONE;
        }
        List<Expr> t = ((Node.TupleExpr) loop.target).elts;
        Call it = (Call) loop.iter;
        if (t.size() != 2 || !(t.get(0) instanceof Name) || !(t.get(1) instanceof Name)
                || !it.args.isEmpty() || !it.keywords.isEmpty() || !(it.func instanceof Attribute)
                || !"items".equals(((Attribute) it.func).attr) || !(((Attribute) it.func).value instanceof Name)) {
            return NONE;
        }
        String k = ((Name) t.get(0)).id;
        String v = ((Name) t.get(1)).id;
        String p = ((Name) ((Attribute) it.func).value).id;
        Stmt st = loop.body.get(0);
        if (k.equals(v) || !(st instanceof Node.ExprStmt) || !(((Node.ExprStmt) st).value instanceof Call)) {
            return NONE;
        }
        Call sc = (Call) ((Node.ExprStmt) st).value;
        if (!(sc.func instanceof Name) || !"setattr".equals(((Name) sc.func).id) || sc.args.size() != 3
                || !sc.keywords.isEmpty() || !(sc.args.get(0) instanceof Name)
                || !isName(sc.args.get(1), k) || !isName(sc.args.get(2), v)) {
            return NONE;
        }
        String o = ((Name) sc.args.get(0)).id;
        int oi = -1;
        int pi = -2;
        for (int i = 0; i < a.positional.size(); i++) {
            Arg x = a.positional.get(i);
            if (x.annotation != null) {
                return NONE;
            }
            if (x.name.equals(o)) {
                oi = i;
            }
            if (x.name.equals(p)) {
                pi = i;
            }
        }
        if (a.kwarg != null) {
            if (a.kwarg.annotation != null) {
                return NONE;
            }
            if (a.kwarg.name.equals(p)) {
                pi = -1;
            }
        }
        for (String n : a.names()) {
            if (n.startsWith("_cn1_kv")) {
                return NONE;
            }
        }
        if (oi < 0 || pi == -2 || pi == oi || o.equals(k) || o.equals(v) || p.equals(k) || p.equals(v)) {
            return NONE;
        }
        return new int[] {oi, pi};
    }

    private static boolean isName(Expr e, String id) {
        return e instanceof Name && ((Name) e).id.equals(id);
    }

    /// The keys `c` passes to the mapping, in order, or null.
    private static List<String> keys(FunctionDef f, int[] sh, int implicit, Call c) {
        Arguments a = f.args;
        for (Expr x : c.args) {
            if (x instanceof Node.Starred) {
                return null;
            }
        }
        if (c.args.size() != a.positional.size() - implicit) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        if (sh[1] == -1) {
            for (Node.Keyword k : c.keywords) {
                if (k.arg == null || a.names().contains(k.arg) || !okKey(k.arg) || out.contains(k.arg)) {
                    return null;
                }
                out.add(k.arg);
            }
            return out;
        }
        if (!c.keywords.isEmpty() || sh[1] < implicit || !(c.args.get(sh[1] - implicit) instanceof DictExpr)) {
            return null;
        }
        DictExpr d = (DictExpr) c.args.get(sh[1] - implicit);
        for (Expr k : d.keys) {
            if (!(k instanceof Constant) || ((Constant) k).kind != Constant.Kind.STR) {
                return null;
            }
            String s = ((Constant) k).text;
            if (!okKey(s) || out.contains(s)) {
                return null;
            }
            out.add(s);
        }
        return out;
    }

    private static boolean okKey(String s) {
        return ConstArgSpecialize.ident(s) && !(s.startsWith("__") && !s.endsWith("__"));
    }

    private static String param(int i) {
        return "_cn1_kv" + i;
    }

    private static FunctionDef clone(FunctionDef f, int[] sh, String name, List<String> keys) {
        Arguments na = new Arguments();
        List<Arg> vs = new ArrayList<Arg>();
        for (int i = 0; i < keys.size(); i++) {
            vs.add(new Arg(param(i), null).at(f.args.line, f.args.col));
        }
        for (int i = 0; i < f.args.positional.size(); i++) {
            Arg x = f.args.positional.get(i);
            if (i == sh[1]) {
                na.positional.addAll(vs);
            } else {
                na.positional.add(new Arg(x.name, null).at(x.line, x.col));
            }
        }
        if (sh[1] == -1) {
            na.positional.addAll(vs);
        }
        na.at(f.args.line, f.args.col);
        String o = f.args.positional.get(sh[0]).name;
        List<Stmt> body = new ArrayList<Stmt>();
        int l = f.body.get(f.body.size() - 1).line;
        int col = f.body.get(f.body.size() - 1).col;
        for (int i = 0; i < keys.size(); i++) {
            List<Expr> tg = new ArrayList<Expr>();
            tg.add(new Attribute(new Name(o).at(l, col), keys.get(i)).at(l, col));
            body.add(new Node.Assign(tg, new Name(param(i)).at(l, col)).at(l, col));
        }
        if (body.isEmpty()) {
            body.add(new Node.Pass().at(l, col));
        }
        List<Expr> decs = new ArrayList<Expr>();
        for (Expr d : f.decorators) {
            decs.add(new Name(((Name) d).id).at(d.line, d.col));
        }
        return new FunctionDef(name, na, body, decs, null, false).at(f.line, f.col);
    }

    /// The arguments of the clone's call: the mapping's values in place of
    /// the mapping, each in the position it was evaluated in.
    private static List<Expr> args(int[] sh, int implicit, Call c) {
        List<Expr> out = new ArrayList<Expr>();
        for (int i = 0; i < c.args.size(); i++) {
            if (i == sh[1] - implicit) {
                out.addAll(((DictExpr) c.args.get(i)).values);
            } else {
                out.add(c.args.get(i));
            }
        }
        if (sh[1] == -1) {
            for (Node.Keyword k : c.keywords) {
                out.add(k.value);
            }
        }
        return out;
    }

    /// Counts in `o`: [FunctionDefs named n, Names n, Attributes .n, string constants n].
    private static void census(Object o, final String n, final int[] r, final List<Call> attrCalls,
            final IdentityHashMap<Object, Boolean> attrSeen) {
        if (o instanceof FunctionDef && ((FunctionDef) o).name.equals(n)) {
            r[0]++;
        } else if (o instanceof Name && ((Name) o).id.equals(n)) {
            r[1]++;
        } else if (o instanceof Attribute && n.equals(((Attribute) o).attr)) {
            r[2]++;
        } else if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR && n.equals(((Constant) o).text)) {
            r[3]++;
        } else if (o instanceof Node.Alias && (n.equals(((Node.Alias) o).name) || n.equals(((Node.Alias) o).asname))) {
            r[3]++;
        } else if (o instanceof Node.Global && ((Node.Global) o).names.contains(n)) {
            r[3]++;
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute && n.equals(((Attribute) ((Call) o).func).attr)) {
            attrCalls.add((Call) o);
        }
        Infer.children(o, x -> census(x, n, r, attrCalls, attrSeen));
    }

    private static boolean moduleFn(Node.Module m, FunctionDef f, List<Node.Module> all) {
        if (!f.decorators.isEmpty()) {
            return false;
        }
        int[] sh = shape(f);
        if (sh.length == 0) {
            return false;
        }
        int[] r = new int[4];
        List<Call> ac = new ArrayList<Call>();
        census(m, f.name, r, ac, null);
        if (r[0] != 1 || r[2] != 0 || r[3] != 0) {
            return false;
        }
        for (Node.Module o : all) {
            if (o != m) { //NOPMD CompareObjectsWithEquals - identity is meant
                int[] r2 = new int[4];
                census(o, f.name, r2, new ArrayList<Call>(), null);
                if (r2[0] + r2[1] + r2[2] + r2[3] != 0) {
                    return false;
                }
            }
        }
        Map<Call, List<Stmt>> holder = new IdentityHashMap<Call, List<Stmt>>();
        ConstArgSpecialize.stmtCalls(m.body, holder);
        List<Call> cs = new ArrayList<Call>();
        for (Call c : holder.keySet()) {
            if (isName(c.func, f.name)) {
                cs.add(c);
            }
        }
        if (cs.size() != r[1] || cs.isEmpty()) {
            return false;
        }
        Map<String, List<String>> seqs = new LinkedHashMap<String, List<String>>();
        List<String> of = new ArrayList<String>();
        for (Call c : cs) {
            List<String> ks = keys(f, sh, 0, c);
            if (ks == null) {
                return false;
            }
            String id = String.join(",", ks);
            seqs.put(id, ks);
            of.add(id);
        }
        Map<String, String> names = names(f.name, seqs, all);
        if (names == null) {
            return false;
        }
        for (int j = 0; j < cs.size(); j++) {
            Call c = cs.get(j);
            List<Stmt> l = holder.get(c);
            for (int q = 0; q < l.size(); q++) {
                Stmt sq = l.get(q);
                boolean es = sq instanceof Node.ExprStmt && ((Node.ExprStmt) sq).value == c; //NOPMD CompareObjectsWithEquals - identity is meant
                boolean as = sq instanceof Node.Assign && ((Node.Assign) sq).value == c; //NOPMD CompareObjectsWithEquals - identity is meant
                if (es || as) {
                    Call nc = new Call(new Name(names.get(of.get(j))).at(c.func.line, c.func.col)).at(c.line, c.col);
                    nc.args.addAll(args(sh, 0, c));
                    Stmt rep = as ? new Node.Assign(((Node.Assign) sq).targets, nc) : new Node.ExprStmt(nc);
                    l.set(q, rep.at(sq.line, sq.col));
                    break;
                }
            }
        }
        replace(m.body, f, sh, seqs, names);
        return true;
    }

    private static boolean method(ClassDef cls, FunctionDef f, List<Node.Module> all) {
        if (f.decorators.size() != 1 || !(f.decorators.get(0) instanceof Name)) {
            return false;
        }
        String d = ((Name) f.decorators.get(0)).id;
        int implicit;
        if ("classmethod".equals(d)) {
            implicit = 1;
        } else if ("staticmethod".equals(d)) {
            implicit = 0;
        } else {
            return false;
        }
        int[] sh = shape(f);
        if (sh.length == 0 || f.name.startsWith("__")) {
            return false;
        }
        int[] r = new int[4];
        List<Call> cs = new ArrayList<Call>();
        for (Node.Module o : all) {
            census(o, f.name, r, cs, null);
        }
        if (r[0] != 1 || r[1] != 0 || r[3] != 0 || r[2] != cs.size() || cs.isEmpty()) {
            return false;
        }
        Map<String, List<String>> seqs = new LinkedHashMap<String, List<String>>();
        List<String> of = new ArrayList<String>();
        for (Call c : cs) {
            List<String> ks = keys(f, sh, implicit, c);
            if (ks == null) {
                return false;
            }
            String id = String.join(",", ks);
            seqs.put(id, ks);
            of.add(id);
        }
        Map<String, String> names = names(f.name, seqs, all);
        if (names == null) {
            return false;
        }
        for (int j = 0; j < cs.size(); j++) {
            Call c = cs.get(j);
            List<Expr> na = args(sh, implicit, c);
            ((Attribute) c.func).attr = names.get(of.get(j));
            c.args.clear();
            c.args.addAll(na);
            c.keywords.clear();
        }
        replace(cls.body, f, sh, seqs, names);
        return true;
    }

    private static Map<String, String> names(String base, Map<String, List<String>> seqs, List<Node.Module> all) {
        Map<String, String> names = new LinkedHashMap<String, String>();
        int n = 0;
        for (String id : seqs.keySet()) {
            String nm = base + SUFFIX + n++;
            int[] r = new int[4];
            for (Node.Module o : all) {
                census(o, nm, r, new ArrayList<Call>(), null);
            }
            if (r[0] + r[1] + r[2] + r[3] != 0) {
                return null;
            }
            names.put(id, nm);
        }
        return names;
    }

    private static void replace(List<Stmt> body, FunctionDef f, int[] sh, Map<String, List<String>> seqs,
            Map<String, String> names) {
        int at = body.indexOf(f);
        body.remove(at);
        List<FunctionDef> clones = new ArrayList<FunctionDef>();
        for (Map.Entry<String, List<String>> e : seqs.entrySet()) {
            clones.add(clone(f, sh, names.get(e.getKey()), e.getValue()));
        }
        body.addAll(at, clones);
    }
}
