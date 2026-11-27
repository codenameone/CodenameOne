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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.IfExp;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/// Folds, at build time, class bases that Python computes when the module
/// runs. The program is closed: which modules exist is known here, so
///
/// - `try: import gzip` / `except ImportError: gzip = None` at module level
///   is the import alone when every module it names is in the program or the
///   bundled library (an import the build resolves cannot fail that way);
/// - a base `A if m else B`, `m` bound once in the module and by an import,
///   is `A` -- a module is always true;
/// - a base `f(...)` or `mod.f(...)` calling a module-level function whose
///   whole body returns one class defined once at module level, or a
///   `cobble.visitor(...)` (every visitor is `cobble._Visitor`,
///   [CobbleLowering]), with constant arguments, is that class.
///
/// Anything else is left as written, and strict mode refuses it as before.
public final class BuildTimeBases {
    private BuildTimeBases() {
    }

    /// Rewrites `trees` (path to module); `exists` answers whether a dotted
    /// module outside them is bundled.
    public static void rewrite(Map<String, Node.Module> trees, Predicate<String> exists) {
        Predicate<String> any = m -> path(trees, m) != null || exists.test(m);
        for (Node.Module t : trees.values()) {
            foldImports(t, any);
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            Node.Module t = e.getValue();
            Map<String, Integer> binds = bindings(t);
            // A copy: a folded base may add an import to the module.
            for (Stmt s : new ArrayList<Stmt>(t.body)) {
                if (!(s instanceof ClassDef)) {
                    continue;
                }
                List<Expr> bases = ((ClassDef) s).bases;
                for (int i = 0; i < bases.size(); i++) {
                    Expr b = bases.get(i);
                    if (b instanceof IfExp && ((IfExp) b).test instanceof Name
                            && importedOnce(t, ((Name) ((IfExp) b).test).id, binds)) {
                        bases.set(i, ((IfExp) b).body);
                    } else if (b instanceof Name) {
                        Expr r = freshImportBase(any, t, ((Name) b).id);
                        if (r != null) {
                            bases.set(i, r);
                        }
                    } else if (b instanceof Call) {
                        Expr r = callBase(trees, e.getKey(), t, binds, (Call) b);
                        if (r != null) {
                            DataclassInit.atLine(r, s.line);
                            bases.set(i, r);
                        }
                    }
                }
            }
        }
    }

    private static void foldImports(Node.Module t, Predicate<String> exists) {
        for (int i = 0; i < t.body.size(); i++) {
            if (!(t.body.get(i) instanceof Node.Try)) {
                continue;
            }
            Node.Try y = (Node.Try) t.body.get(i);
            if (y.star || !y.finalbody.isEmpty() || y.body.isEmpty() || y.handlers.isEmpty()) {
                continue;
            }
            boolean ok = true;
            for (Stmt s : y.body) {
                if (!(s instanceof Import)) {
                    ok = false;
                    break;
                }
                for (Alias a : ((Import) s).names) {
                    ok &= exists.test(a.name);
                }
            }
            for (Node.ExceptHandler h : y.handlers) {
                ok &= h.type instanceof Name && ("ImportError".equals(((Name) h.type).id)
                        || "ModuleNotFoundError".equals(((Name) h.type).id));
            }
            if (ok) {
                List<Stmt> repl = new ArrayList<Stmt>(y.body);
                repl.addAll(y.orelse);
                t.body.remove(i);
                t.body.addAll(i, repl);
                i += repl.size() - 1;
            }
        }
    }

    /// A base `n` bound at module level (directly or under a top-level `if`)
    /// only by `..., n, ... = f()`, where `f` is a module-level function that
    /// ends in `return (..., r, ...)` with `r = p.attr` and
    /// `p = importlib.import_module("mod")` its one binding of each, and `mod`
    /// in the program or the bundled library: defusedxml's `_get_py3_cls`, which re-imports
    /// `xml.etree.ElementTree` for its pure-Python `XMLParser`. The program has
    /// one copy of each module, so the class is `mod.attr`, bound by an import
    /// added at the top of the module. The call itself still runs as written.
    private static Expr freshImportBase(Predicate<String> any, Node.Module t, String n) {
        Assign site = null;
        int sites = 0;
        for (Stmt s : t.body) {
            List<Stmt> scope = java.util.Collections.singletonList(s);
            if (s instanceof Node.If) {
                scope = ((Node.If) s).body;
                if (((Node.If) s).orelse.size() > 0) {
                    scope = new ArrayList<Stmt>(scope);
                    scope.addAll(((Node.If) s).orelse);
                }
            }
            for (Stmt x : scope) {
                if (x instanceof Assign) {
                    for (Expr tg : ((Assign) x).targets) {
                        if (tg instanceof Name && n.equals(((Name) tg).id)) {
                            sites += 2;
                        } else if (tg instanceof Node.TupleExpr) {
                            for (Expr el : ((Node.TupleExpr) tg).elts) {
                                if (el instanceof Name && n.equals(((Name) el).id)) {
                                    sites++;
                                    site = (Assign) x;
                                }
                            }
                        }
                    }
                } else if (x instanceof Node.If) {
                    // Only one level of `if` is read; deeper is refused.
                    if (s instanceof Node.If) {
                        return null;
                    }
                } else {
                    Map<String, Integer> one = new HashMap<String, Integer>();
                    if (!(x instanceof FunctionDef) && !(x instanceof ClassDef)) {
                        walk(x, one);
                    } else {
                        bump(x instanceof FunctionDef ? ((FunctionDef) x).name : ((ClassDef) x).name, one);
                    }
                    if (one.containsKey(n) && !(x instanceof ImportFrom && s instanceof Node.If)) {
                        return null;
                    }
                }
            }
        }
        if (sites != 1 || site.targets.size() != 1 || !(site.value instanceof Call)
                || !(((Call) site.value).func instanceof Name)) {
            return null;
        }
        int k = -1;
        List<Expr> elts = ((Node.TupleExpr) site.targets.get(0)).elts;
        for (int j = 0; j < elts.size(); j++) {
            if (elts.get(j) instanceof Name && n.equals(((Name) elts.get(j)).id)) {
                k = j;
            }
        }
        String fn = ((Name) ((Call) site.value).func).id;
        Map<String, Integer> binds = bindings(t);
        FunctionDef f = null;
        for (Stmt s : t.body) {
            if (s instanceof FunctionDef && fn.equals(((FunctionDef) s).name)) {
                f = (FunctionDef) s;
            }
        }
        if (f == null || binds.get(fn) != 1 || !f.decorators.isEmpty() || f.isAsync || f.body.isEmpty()
                || !(f.body.get(f.body.size() - 1) instanceof Node.Return)) {
            return null;
        }
        Expr rv = ((Node.Return) f.body.get(f.body.size() - 1)).value;
        if (!(rv instanceof Node.TupleExpr) || ((Node.TupleExpr) rv).elts.size() != elts.size()
                || !(((Node.TupleExpr) rv).elts.get(k) instanceof Name)) {
            return null;
        }
        Expr attr = soleLocal(f, ((Name) ((Node.TupleExpr) rv).elts.get(k)).id);
        if (!(attr instanceof Attribute) || !(((Attribute) attr).value instanceof Name)) {
            return null;
        }
        Expr imp = soleLocal(f, ((Name) ((Attribute) attr).value).id);
        if (imp instanceof Call && ((Call) imp).args.size() == 1 && ((Call) imp).args.get(0) instanceof Name) {
            // `name = "mod"` then `import_module(name)`, `name` bound once:
            // the call names the module as a literal (which Liveness reads
            // as one import, not as a lookup by computed name).
            Expr lit = soleLocal(f, ((Name) ((Call) imp).args.get(0)).id);
            if (lit instanceof Constant && ((Constant) lit).kind == Constant.Kind.STR) {
                Constant k2 = new Constant(Constant.Kind.STR, ((Constant) lit).text);
                k2.line = lit.line;
                ((Call) imp).args.set(0, k2);
            }
        }
        if (!(imp instanceof Call) || ((Call) imp).args.size() != 1 || !((Call) imp).keywords.isEmpty()
                || !(((Call) imp).func instanceof Attribute)
                || !"import_module".equals(((Attribute) ((Call) imp).func).attr)
                || !(((Attribute) ((Call) imp).func).value instanceof Name)
                || !(((Call) imp).args.get(0) instanceof Constant)
                || ((Constant) ((Call) imp).args.get(0)).kind != Constant.Kind.STR) {
            return null;
        }
        String lib = ((Name) ((Attribute) ((Call) imp).func).value).id;
        boolean importlib = false;
        for (Stmt s : t.body) {
            List<Stmt> scope = s instanceof Node.If ? ((Node.If) s).body : java.util.Collections.singletonList(s);
            for (Stmt x : scope) {
                if (x instanceof Import) {
                    for (Alias a : ((Import) x).names) {
                        importlib |= "importlib".equals(a.name) && lib.equals(a.asname == null ? a.name : a.asname);
                    }
                }
            }
        }
        String mod = ((Constant) ((Call) imp).args.get(0)).text;
        if (!importlib || binds.get(lib) != 1 || !any.test(mod)) {
            return null;
        }
        String local = "_cn1_base_" + n;
        if (binds.containsKey(local)) {
            return null;
        }
        List<Alias> names = new ArrayList<Alias>();
        names.add(new Alias(((Attribute) attr).attr, local));
        int at = 0;
        while (at < t.body.size() && t.body.get(at) instanceof ImportFrom
                && "__future__".equals(((ImportFrom) t.body.get(at)).module)) {
            at++;
        }
        ImportFrom add = new ImportFrom(mod, 0, names);
        add.line = t.body.get(Math.min(at, t.body.size() - 1)).line;
        t.body.add(at, add);
        return new Name(local);
    }

    /// The value of the one assignment to local `v` in `f`, or null when
    /// `v` is bound any other way or more than once.
    private static Expr soleLocal(FunctionDef f, String v) {
        Map<String, Integer> b = new HashMap<String, Integer>();
        for (Stmt s : f.body) {
            walk(s, b);
        }
        // walk() also counts a name used inside a subscript or attribute
        // target (`d[v] = x`), which binds nothing; over-counting only
        // refuses, so take those back out where they are the reason.
        Map<String, Integer> uses = new HashMap<String, Integer>();
        for (Stmt s : f.body) {
            targetUses(s, uses);
        }
        if (b.containsKey(v) && uses.containsKey(v)) {
            b.put(v, b.get(v) - uses.get(v));
        }
        Integer c = b.get(v);
        if (c == null || c != 1) {
            return null;
        }
        Expr[] out = new Expr[1];
        for (Stmt s : f.body) {
            soleAssign(s, v, out);
        }
        return out[0];
    }

    /// Counts the names under the subscripts and attributes of assignment
    /// targets, which [#targets] counts as bindings.
    private static void targetUses(Object o, Map<String, Integer> out) {
        List<Expr> ts = o instanceof Assign ? ((Assign) o).targets
                : o instanceof Node.AugAssign ? java.util.Collections.singletonList(((Node.AugAssign) o).target)
                : o instanceof Node.Delete ? ((Node.Delete) o).targets : java.util.Collections.<Expr>emptyList();
        for (Expr t : ts) {
            if (t instanceof Node.Subscript || t instanceof Attribute) {
                Infer.children(t, x -> targets(x, out));
            }
        }
        Infer.children(o, x -> targetUses(x, out));
    }

    private static void soleAssign(Object o, String v, Expr[] out) {
        if (o instanceof Assign && ((Assign) o).targets.size() == 1 && ((Assign) o).targets.get(0) instanceof Name
                && v.equals(((Name) ((Assign) o).targets.get(0)).id)) {
            out[0] = ((Assign) o).value;
        }
        if (!(o instanceof FunctionDef) && !(o instanceof ClassDef) && !(o instanceof Node.Lambda)) {
            Infer.children(o, x -> soleAssign(x, v, out));
        }
    }

    /// Whether `n` is bound exactly once in `t`, by a top-level `import`.
    private static boolean importedOnce(Node.Module t, String n, Map<String, Integer> binds) {
        Integer c = binds.get(n);
        if (c == null || c != 1) {
            return false;
        }
        for (Stmt s : t.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if (n.equals(a.asname) || a.asname == null && n.equals(a.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Expr callBase(Map<String, Node.Module> trees, String path, Node.Module t,
            Map<String, Integer> binds, Call c) {
        for (Expr a : c.args) {
            if (!(a instanceof Constant)) {
                return null;
            }
        }
        for (Keyword k : c.keywords) {
            if (k.arg == null || !(k.value instanceof Constant)) {
                return null;
            }
        }
        String fn;
        String owner;
        String qual;
        if (c.func instanceof Name) {
            fn = ((Name) c.func).id;
            owner = path;
            qual = null;
        } else if (c.func instanceof Attribute && ((Attribute) c.func).value instanceof Name) {
            qual = ((Name) ((Attribute) c.func).value).id;
            fn = ((Attribute) c.func).attr;
            owner = importedModule(trees, path, t, qual, binds);
            if (owner == null) {
                return null;
            }
        } else {
            return null;
        }
        Node.Module m = trees.get(owner);
        Map<String, Integer> mb = owner.equals(path) ? binds : bindings(m);
        Integer fc = mb.get(fn);
        if (fc == null || fc != 1) {
            return null;
        }
        FunctionDef f = null;
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef && fn.equals(((FunctionDef) s).name)) {
                f = (FunctionDef) s;
            }
        }
        if (f == null || !f.decorators.isEmpty() || f.isAsync) {
            return null;
        }
        List<Stmt> body = f.body;
        int at = 0;
        if (!body.isEmpty() && body.get(0) instanceof Node.ExprStmt
                && ((Node.ExprStmt) body.get(0)).value instanceof Constant) {
            at = 1;
        }
        if (body.size() != at + 1 || !(body.get(at) instanceof Node.Return)) {
            return null;
        }
        Expr v = ((Node.Return) body.get(at)).value;
        if (v instanceof Name) {
            String cls = ((Name) v).id;
            Integer cc = mb.get(cls);
            boolean isClass = false;
            for (Stmt s : m.body) {
                isClass |= s instanceof ClassDef && cls.equals(((ClassDef) s).name);
            }
            if (!isClass || cc == null || cc != 1) {
                return null;
            }
            return qual == null ? new Name(cls) : new Attribute(new Name(qual), cls);
        }
        if (v instanceof Call && ((Call) v).func instanceof Attribute
                && "visitor".equals(((Attribute) ((Call) v).func).attr)
                && ((Attribute) ((Call) v).func).value instanceof Name
                && "cobble".equals(importName(m, ((Name) ((Attribute) ((Call) v).func).value).id, mb))) {
            for (Stmt s : t.body) {
                if (s instanceof Import) {
                    for (Alias a : ((Import) s).names) {
                        String local = a.asname != null ? a.asname : a.name;
                        if ("cobble".equals(a.name) && "cobble".equals(importName(t, local, binds))) {
                            return new Attribute(new Name(local), "_Visitor");
                        }
                    }
                }
            }
        }
        return null;
    }

    /// The module a top-level `import x` binds `n` to, when that is its one
    /// binding; null otherwise.
    private static String importName(Node.Module t, String n, Map<String, Integer> binds) {
        Integer c = binds.get(n);
        if (c == null || c != 1) {
            return null;
        }
        for (Stmt s : t.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if (n.equals(a.asname) || a.asname == null && n.equals(a.name) && a.name.indexOf('.') < 0) {
                        return a.name;
                    }
                }
            }
        }
        return null;
    }

    /// The program path of the module `n` names in `t` (one binding, by
    /// `import a.b as n`, `import n` or `from pkg import n`), or null.
    private static String importedModule(Map<String, Node.Module> trees, String path, Node.Module t, String n,
            Map<String, Integer> binds) {
        Integer c = binds.get(n);
        if (c == null || c != 1) {
            return null;
        }
        String pkg = path.endsWith("/__init__.py") ? path.substring(0, path.length() - 12)
                : path.lastIndexOf('/') < 0 ? "" : path.substring(0, path.lastIndexOf('/'));
        pkg = pkg.replace('/', '.');
        for (Stmt s : t.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if (n.equals(a.asname) || a.asname == null && n.equals(a.name)) {
                        return path(trees, a.name);
                    }
                }
            } else if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    if (n.equals(a.asname != null ? a.asname : a.name)) {
                        String base = pkg;
                        for (int l = 1; l < f.level && base != null; l++) {
                            int d = base.lastIndexOf('.');
                            base = d < 0 ? (base.isEmpty() ? null : "") : base.substring(0, d);
                        }
                        if (base == null) {
                            return null;
                        }
                        String mod = f.level == 0 ? f.module
                                : f.module == null ? base : base.isEmpty() ? f.module : base + "." + f.module;
                        if (mod == null) {
                            return null;
                        }
                        return path(trees, mod.isEmpty() ? a.name : mod + "." + a.name);
                    }
                }
            }
        }
        return null;
    }

    private static String path(Map<String, Node.Module> trees, String dotted) {
        String p = dotted.replace('.', '/');
        if (trees.containsKey(p + ".py")) {
            return p + ".py";
        }
        return trees.containsKey(p + "/__init__.py") ? p + "/__init__.py" : null;
    }

    /// How many times each name is bound anywhere in `t` (any scope, so a
    /// count of one is conservative).
    static Map<String, Integer> bindings(Node.Module t) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        walk(t, out);
        return out;
    }

    private static void walk(Object o, Map<String, Integer> out) {
        if (o instanceof Assign) {
            for (Expr x : ((Assign) o).targets) {
                targets(x, out);
            }
        } else if (o instanceof Node.AugAssign) {
            targets(((Node.AugAssign) o).target, out);
        } else if (o instanceof Node.AnnAssign) {
            targets(((Node.AnnAssign) o).target, out);
        } else if (o instanceof Node.For) {
            targets(((Node.For) o).target, out);
        } else if (o instanceof Node.WithItem && ((Node.WithItem) o).vars != null) {
            targets(((Node.WithItem) o).vars, out);
        } else if (o instanceof Node.Delete) {
            for (Expr x : ((Node.Delete) o).targets) {
                targets(x, out);
            }
        } else if (o instanceof Node.NamedExpr) {
            bump(((Node.NamedExpr) o).target.id, out);
        } else if (o instanceof Node.ExceptHandler && ((Node.ExceptHandler) o).name != null) {
            bump(((Node.ExceptHandler) o).name, out);
        } else if (o instanceof Node.Global) {
            for (String n : ((Node.Global) o).names) {
                bump(n, out);
            }
        } else if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                String n = a.asname != null ? a.asname : a.name;
                int d = n.indexOf('.');
                bump(d < 0 ? n : n.substring(0, d), out);
            }
        } else if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                bump(a.asname != null ? a.asname : a.name, out);
            }
        } else if (o instanceof FunctionDef) {
            bump(((FunctionDef) o).name, out);
        } else if (o instanceof ClassDef) {
            bump(((ClassDef) o).name, out);
        }
        Infer.children(o, x -> walk(x, out));
    }

    private static void targets(Object o, Map<String, Integer> out) {
        if (o instanceof Name) {
            bump(((Name) o).id, out);
        }
        Infer.children(o, x -> targets(x, out));
    }

    private static void bump(String n, Map<String, Integer> out) {
        Integer c = out.get(n);
        out.put(n, c == null ? 1 : c + 1);
    }
}
