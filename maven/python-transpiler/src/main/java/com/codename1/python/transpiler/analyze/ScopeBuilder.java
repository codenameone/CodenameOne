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
package com.codename1.python.transpiler.analyze;

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.*;
import com.codename1.python.transpiler.ast.Scope;

import java.util.ArrayList;
import java.util.List;

/// Builds the [Scope] tree of a module with CPython's binding rules, then
/// decides which locals are cells (captured by an inner scope).
///
/// Every function, lambda, class and comprehension node gets its scope
/// attached; the module scope is returned.
public final class ScopeBuilder {
    private final String file;
    private final List<Scope> all = new ArrayList<Scope>();
    private Scope cur;

    private ScopeBuilder(String file) {
        this.file = file;
    }

    public static Scope build(Node.Module m, String file) {
        ScopeBuilder b = new ScopeBuilder(file);
        Scope s = new Scope(Scope.Kind.MODULE, null, "<module>");
        b.all.add(s);
        b.cur = s;
        Desugar.rewrite(m.body);
        b.stmts(m.body);
        b.resolveCells();
        return s;
    }

    private PythonSyntaxError err(Node n, String msg) {
        return new PythonSyntaxError(file, n.line, n.col, msg);
    }

    private void bind(String n) {
        cur.assigned.add(n);
    }

    private Scope push(Scope.Kind k, String name) {
        Scope s = new Scope(k, cur, name);
        if (cur.kind == Scope.Kind.CLASS) {
            s.enclosingClass = cur;
        } else {
            s.enclosingClass = cur.enclosingClass;
        }
        all.add(s);
        cur = s;
        return s;
    }

    private void pop() {
        cur = cur.parent;
    }

    // ---- statements ------------------------------------------------------

    private void stmts(List<Stmt> body) {
        for (Stmt s : body) {
            stmt(s);
        }
    }

    private void stmt(Stmt s) {
        if (s instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) s;
            exprs(f.decorators);
            argDefaults(f.args);
            bind(f.name);
            f.scope = push(Scope.Kind.FUNCTION, f.name);
            params(f.args);
            stmts(f.body);
            pop();
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            exprs(c.decorators);
            exprs(c.bases);
            for (Keyword k : c.keywords) {
                expr(k.value);
            }
            bind(c.name);
            c.scope = push(Scope.Kind.CLASS, c.name);
            stmts(c.body);
            pop();
        } else if (s instanceof ExprStmt) {
            expr(((ExprStmt) s).value);
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            expr(a.value);
            for (Expr t : a.targets) {
                target(t);
            }
        } else if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            expr(a.value);
            expr(a.target);
            target(a.target);
        } else if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (a.value != null) {
                expr(a.value);
                target(a.target);
            } else if (a.target instanceof Name && cur.kind != Scope.Kind.MODULE) {
                // An annotation alone still makes the name local to a function.
                bind(((Name) a.target).id);
            } else if (!(a.target instanceof Name)) {
                expr(a.target);
            }
        } else if (s instanceof Return) {
            expr(((Return) s).value);
        } else if (s instanceof Delete) {
            for (Expr t : ((Delete) s).targets) {
                if (t instanceof Name) {
                    cur.used.add(((Name) t).id);
                    cur.deleted.add(((Name) t).id);
                }
                target(t);
            }
        } else if (s instanceof Raise) {
            expr(((Raise) s).exc);
            expr(((Raise) s).cause);
        } else if (s instanceof Global) {
            Global g = (Global) s;
            for (String n : g.names) {
                if (cur.kind == Scope.Kind.MODULE) {
                    if (g.nonlocal) {
                        throw err(s, "nonlocal declaration not allowed at module level");
                    }
                    continue;
                }
                if (g.nonlocal) {
                    cur.nonlocals.add(n);
                } else {
                    cur.globals.add(n);
                    cur.module().assigned.add(n);
                }
            }
        } else if (s instanceof Assert) {
            expr(((Assert) s).test);
            expr(((Assert) s).msg);
        } else if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                if (a.asname != null) {
                    bind(a.asname);
                } else {
                    int dot = a.name.indexOf('.');
                    bind(dot < 0 ? a.name : a.name.substring(0, dot));
                }
            }
        } else if (s instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) s).names) {
                if ("*".equals(a.name)) {
                    if (cur.kind != Scope.Kind.MODULE) {
                        throw err(s, "import * only allowed at module level");
                    }
                    cur.hasStarImport = true;
                } else {
                    bind(a.asname != null ? a.asname : a.name);
                }
            }
        } else if (s instanceof If) {
            If i = (If) s;
            expr(i.test);
            stmts(i.body);
            stmts(i.orelse);
        } else if (s instanceof While) {
            While w = (While) s;
            expr(w.test);
            stmts(w.body);
            stmts(w.orelse);
        } else if (s instanceof For) {
            For f = (For) s;
            expr(f.iter);
            target(f.target);
            stmts(f.body);
            stmts(f.orelse);
        } else if (s instanceof With) {
            With w = (With) s;
            for (WithItem it : w.items) {
                expr(it.context);
                if (it.vars != null) {
                    target(it.vars);
                }
            }
            stmts(w.body);
        } else if (s instanceof Try) {
            Try t = (Try) s;
            stmts(t.body);
            for (ExceptHandler h : t.handlers) {
                expr(h.type);
                if (h.name != null) {
                    bind(h.name);
                    cur.deleted.add(h.name);
                }
                stmts(h.body);
            }
            stmts(t.orelse);
            stmts(t.finalbody);
        } else if (s instanceof Match) {
            Match m = (Match) s;
            expr(m.subject);
            for (MatchCase c : m.cases) {
                pattern(c.pattern);
                expr(c.guard);
                stmts(c.body);
            }
        } else if (s instanceof TypeAlias) {
            bind(((TypeAlias) s).name);
            expr(((TypeAlias) s).value);
        }
    }

    private void pattern(Pattern p) {
        if (p == null) {
            return;
        }
        if (p.name != null && p.kind != Pattern.Kind.VALUE) {
            bind(p.name);
        }
        if (p.value != null) {
            expr(p.value);
        }
        exprs(p.keys);
        for (Pattern q : p.patterns) {
            pattern(q);
        }
        for (Pattern q : p.kwdPatterns) {
            pattern(q);
        }
    }

    private void argDefaults(Arguments a) {
        exprs(a.defaults);
        for (Expr e : a.kwDefaults) {
            expr(e);
        }
    }

    private void params(Arguments a) {
        for (String n : a.names()) {
            cur.params.add(n);
        }
    }

    private void target(Expr t) {
        if (t instanceof Name) {
            bind(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                target(e);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                target(e);
            }
        } else if (t instanceof Starred) {
            target(((Starred) t).value);
        } else {
            // Attribute or subscript: its parts are reads.
            if (t instanceof Attribute && ((Attribute) t).value instanceof Name) {
                root().attrStores.add(((Name) ((Attribute) t).value).id + "." + ((Attribute) t).attr);
            }
            expr(t);
        }
    }

    private Scope root() {
        Scope s = cur;
        while (s.parent != null) {
            s = s.parent;
        }
        return s;
    }

    // ---- expressions -----------------------------------------------------

    private void exprs(List<Expr> es) {
        for (Expr e : es) {
            expr(e);
        }
    }

    private void expr(Expr e) {
        if (e == null) {
            return;
        }
        if (e instanceof Name) {
            cur.used.add(((Name) e).id);
        } else if (e instanceof Constant) {
            return;
        } else if (e instanceof JoinedStr) {
            exprs(((JoinedStr) e).parts);
        } else if (e instanceof FormattedValue) {
            expr(((FormattedValue) e).value);
            expr(((FormattedValue) e).spec);
        } else if (e instanceof Attribute) {
            expr(((Attribute) e).value);
        } else if (e instanceof Subscript) {
            expr(((Subscript) e).value);
            expr(((Subscript) e).index);
        } else if (e instanceof Slice) {
            Slice s = (Slice) e;
            expr(s.lower);
            expr(s.upper);
            expr(s.step);
        } else if (e instanceof Starred) {
            expr(((Starred) e).value);
        } else if (e instanceof BinOp) {
            expr(((BinOp) e).left);
            expr(((BinOp) e).right);
        } else if (e instanceof UnaryOp) {
            expr(((UnaryOp) e).operand);
        } else if (e instanceof BoolOp) {
            exprs(((BoolOp) e).values);
        } else if (e instanceof Compare) {
            expr(((Compare) e).left);
            exprs(((Compare) e).comparators);
        } else if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            expr(i.test);
            expr(i.body);
            expr(i.orelse);
        } else if (e instanceof NamedExpr) {
            NamedExpr n = (NamedExpr) e;
            expr(n.value);
            walrus(n);
        } else if (e instanceof Call) {
            Call c = (Call) e;
            if (c.func instanceof Name && ("setattr".equals(((Name) c.func).id) || "delattr".equals(((Name) c.func).id))
                    && !c.args.isEmpty() && c.args.get(0) instanceof Name) {
                Expr a = c.args.size() > 1 ? c.args.get(1) : null;
                String attr = a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR
                        ? ((Constant) a).text : null;
                root().attrStores.add(((Name) c.args.get(0)).id + "." + (attr != null ? attr : "*"));
            }
            expr(c.func);
            exprs(c.args);
            for (Keyword k : c.keywords) {
                expr(k.value);
            }
            if (c.func instanceof Name && "super".equals(((Name) c.func).id) && c.args.isEmpty()) {
                // Zero-argument super() reads the first parameter, which
                // must therefore be visible as a cell from nested scopes.
                Scope f = cur;
                while (f != null && f.kind == Scope.Kind.COMPREHENSION) {
                    f = f.parent;
                }
                if (f != null && f != cur && f.isFunctionLike() && !f.params.isEmpty()) { //NOPMD CompareObjectsWithEquals - identity is meant
                    cur.used.add(f.params.iterator().next());
                }
            }
        } else if (e instanceof ListExpr) {
            exprs(((ListExpr) e).elts);
        } else if (e instanceof TupleExpr) {
            exprs(((TupleExpr) e).elts);
        } else if (e instanceof SetExpr) {
            exprs(((SetExpr) e).elts);
        } else if (e instanceof DictExpr) {
            DictExpr d = (DictExpr) e;
            exprs(d.keys);
            exprs(d.values);
        } else if (e instanceof Comp) {
            Comp c = (Comp) e;
            // The first iterable is evaluated in the enclosing scope.
            expr(c.generators.get(0).iter);
            c.scope = push(Scope.Kind.COMPREHENSION, "<comp>");
            for (int i = 0; i < c.generators.size(); i++) {
                Comprehension g = c.generators.get(i);
                if (i > 0) {
                    expr(g.iter);
                }
                target(g.target);
                exprs(g.ifs);
            }
            expr(c.elt);
            expr(c.value);
            pop();
        } else if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            argDefaults(l.args);
            l.scope = push(Scope.Kind.LAMBDA, "<lambda>");
            params(l.args);
            expr(l.body);
            pop();
        } else if (e instanceof Await) {
            cur.hasYield = true;
            expr(((Await) e).value);
        } else if (e instanceof Yield) {
            cur.hasYield = true;
            cur.yields = true;
            expr(((Yield) e).value);
        }
    }

    /// PEP 572: a walrus inside a comprehension binds in the nearest
    /// enclosing scope that is not a comprehension.
    private void walrus(NamedExpr n) {
        String id = n.target.id;
        if (cur.kind != Scope.Kind.COMPREHENSION) {
            bind(id);
            return;
        }
        Scope t = cur;
        while (t.kind == Scope.Kind.COMPREHENSION) {
            t = t.parent;
        }
        if (t.kind == Scope.Kind.CLASS) {
            throw err(n, "assignment expression within a comprehension cannot be used in a class body");
        }
        for (Scope s = cur; s != t; s = s.parent) { //NOPMD CompareObjectsWithEquals - identity is meant
            if (t.kind == Scope.Kind.MODULE) {
                s.globals.add(id);
            } else {
                s.nonlocals.add(id);
            }
        }
        if (t.kind == Scope.Kind.MODULE || t.globals.contains(id)) {
            t.module().assigned.add(id);
        } else if (!t.nonlocals.contains(id)) {
            t.assigned.add(id);
        }
    }

    // ---- cells -----------------------------------------------------------

    private void resolveCells() {
        for (Scope s : all) {
            List<String> names = new ArrayList<String>(s.used);
            names.addAll(s.nonlocals);
            for (String n : names) {
                if (s.resolve(n) != Scope.Binding.FREE) {
                    continue;
                }
                Scope owner = s.owner(n);
                if (owner == null) {
                    throw new PythonSyntaxError(file, 0, 0, "no binding for nonlocal '" + n + "' found");
                }
                owner.cells.add(n);
            }
        }
    }
}
