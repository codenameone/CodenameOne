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

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/// Definite assignment for a function's locals: the loads of a name that
/// every path to them has bound, and nothing since unbound. Such a load
/// needs no unbound-local check.
///
/// Only ever an under-approximation: a construct this does not model leaves
/// the loads in it unmarked (still checked) and binds nothing after it.
/// Walrus targets, comprehensions and lambdas are not followed.
final class DefAssign {
    final Set<Name> safe = Collections.newSetFromMap(new IdentityHashMap<Name, Boolean>());

    /// Marks the safe loads of `body`, a function whose parameters `params`
    /// are bound on entry.
    void function(List<Stmt> body, Set<String> params) {
        block(body, new HashSet<String>(params));
    }

    /// The names bound after `b`, or null when it cannot complete normally.
    private Set<String> block(List<Stmt> b, Set<String> in) {
        Set<String> cur = in;
        for (Stmt s : b) {
            if (cur == null) {
                return null;
            }
            cur = stmt(s, cur);
        }
        return cur;
    }

    private static Set<String> join(Set<String> a, Set<String> b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        Set<String> r = new HashSet<String>(a);
        r.retainAll(b);
        return r;
    }

    private Set<String> stmt(Stmt s, Set<String> in) {
        if (s instanceof ExprStmt) {
            expr(((ExprStmt) s).value, in);
            return in;
        }
        if (s instanceof Assign) {
            Assign a = (Assign) s;
            expr(a.value, in);
            Set<String> out = new HashSet<String>(in);
            for (Expr t : a.targets) {
                bind(t, in, out);
            }
            return out;
        }
        if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            Set<String> out = new HashSet<String>(in);
            if (a.target instanceof Name) {
                expr(a.target, in);
            }
            expr(a.value, in);
            bind(a.target, in, out);
            return out;
        }
        if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (a.value == null) {
                return in;
            }
            expr(a.value, in);
            Set<String> out = new HashSet<String>(in);
            bind(a.target, in, out);
            return out;
        }
        if (s instanceof Return) {
            expr(((Return) s).value, in);
            return null;
        }
        if (s instanceof Raise) {
            expr(((Raise) s).exc, in);
            expr(((Raise) s).cause, in);
            return null;
        }
        if (s instanceof Break || s instanceof Continue) {
            return null;
        }
        if (s instanceof Pass) {
            return in;
        }
        if (s instanceof Assert) {
            expr(((Assert) s).test, in);
            expr(((Assert) s).msg, in);
            return in;
        }
        if (s instanceof Delete) {
            Set<String> out = new HashSet<String>(in);
            for (Expr t : ((Delete) s).targets) {
                if (t instanceof Name || t instanceof TupleExpr || t instanceof ListExpr) {
                    Set<String> gone = new HashSet<String>();
                    delNames(t, gone);
                    out.removeAll(gone);
                } else {
                    parts(t, in);
                }
            }
            return out;
        }
        if (s instanceof If) {
            If i = (If) s;
            expr(i.test, in);
            return join(block(i.body, in), block(i.orelse, in));
        }
        if (s instanceof While) {
            While w = (While) s;
            // A later iteration sees what an earlier one unbound.
            Set<String> loop = new HashSet<String>(in);
            loop.removeAll(unbinds(w.body));
            expr(w.test, loop);
            block(w.body, loop);
            block(w.orelse, loop);
            // The body may run no time at all, and a break skips the else.
            return loop;
        }
        if (s instanceof For) {
            For f = (For) s;
            expr(f.iter, in);
            Set<String> loop = new HashSet<String>(in);
            loop.removeAll(unbinds(f.body));
            Set<String> body = new HashSet<String>(loop);
            bind(f.target, loop, body);
            block(f.body, body);
            block(f.orelse, loop);
            return loop;
        }
        if (s instanceof With) {
            With w = (With) s;
            Set<String> cur = new HashSet<String>(in);
            for (WithItem it : w.items) {
                expr(it.context, cur);
                if (it.vars != null) {
                    bind(it.vars, cur, cur);
                }
            }
            // A context manager may swallow an exception from the body.
            block(w.body, cur);
            return cur;
        }
        if (s instanceof Try) {
            Try t = (Try) s;
            Set<String> body = block(t.body, in);
            Set<String> res = body == null ? null : block(t.orelse, body);
            for (ExceptHandler h : t.handlers) {
                expr(h.type, in);
                Set<String> hin = new HashSet<String>(in);
                if (h.name != null) {
                    hin.add(h.name);
                }
                Set<String> hout = block(h.body, hin);
                if (hout != null && h.name != null) {
                    // The name is unbound when the handler ends.
                    hout = new HashSet<String>(hout);
                    hout.remove(h.name);
                }
                res = join(res, hout);
            }
            if (!t.finalbody.isEmpty()) {
                Set<String> fin = block(t.finalbody, in);
                if (fin == null) {
                    return null;
                }
                if (res != null) {
                    res = new HashSet<String>(res);
                    res.addAll(fin);
                }
            }
            return res;
        }
        if (s instanceof FunctionDef) {
            Set<String> out = new HashSet<String>(in);
            out.add(((FunctionDef) s).name);
            return out;
        }
        if (s instanceof ClassDef) {
            Set<String> out = new HashSet<String>(in);
            out.add(((ClassDef) s).name);
            return out;
        }
        if (s instanceof Import) {
            Set<String> out = new HashSet<String>(in);
            for (Alias a : ((Import) s).names) {
                out.add(a.asname != null ? a.asname : a.name.split("\\.")[0]);
            }
            return out;
        }
        if (s instanceof ImportFrom) {
            Set<String> out = new HashSet<String>(in);
            for (Alias a : ((ImportFrom) s).names) {
                if (!"*".equals(a.name)) {
                    out.add(a.asname != null ? a.asname : a.name);
                }
            }
            return out;
        }
        // Global, Match, TypeAlias: nothing marked, nothing bound.
        return in;
    }

    /// The names `del` or an `except ... as` clause in `b` can unbind,
    /// outside nested functions and classes.
    private static Set<String> unbinds(List<Stmt> b) {
        Set<String> out = new HashSet<String>();
        unbinds(b, out);
        return out;
    }

    private static void unbinds(List<Stmt> b, Set<String> out) {
        for (Stmt s : b) {
            if (s instanceof Delete) {
                for (Expr t : ((Delete) s).targets) {
                    delNames(t, out);
                }
            } else if (s instanceof If) {
                unbinds(((If) s).body, out);
                unbinds(((If) s).orelse, out);
            } else if (s instanceof While) {
                unbinds(((While) s).body, out);
                unbinds(((While) s).orelse, out);
            } else if (s instanceof For) {
                unbinds(((For) s).body, out);
                unbinds(((For) s).orelse, out);
            } else if (s instanceof With) {
                unbinds(((With) s).body, out);
            } else if (s instanceof Try) {
                Try t = (Try) s;
                unbinds(t.body, out);
                for (ExceptHandler h : t.handlers) {
                    if (h.name != null) {
                        out.add(h.name);
                    }
                    unbinds(h.body, out);
                }
                unbinds(t.orelse, out);
                unbinds(t.finalbody, out);
            } else if (s instanceof Match) {
                for (MatchCase c : ((Match) s).cases) {
                    unbinds(c.body, out);
                }
            }
        }
    }

    private static void delNames(Expr t, Set<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                delNames(e, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                delNames(e, out);
            }
        }
    }

    /// Binds the names of target `t`; the parts of attribute and subscript
    /// targets are loads.
    private void bind(Expr t, Set<String> in, Set<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                bind(e, in, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                bind(e, in, out);
            }
        } else if (t instanceof Starred) {
            bind(((Starred) t).value, in, out);
        } else {
            parts(t, in);
        }
    }

    private void parts(Expr t, Set<String> in) {
        if (t instanceof Attribute) {
            expr(((Attribute) t).value, in);
        } else if (t instanceof Subscript) {
            expr(((Subscript) t).value, in);
            expr(((Subscript) t).index, in);
        }
    }

    private void exprs(List<Expr> es, Set<String> in) {
        for (Expr e : es) {
            expr(e, in);
        }
    }

    private void expr(Expr e, Set<String> in) {
        if (e == null) {
            return;
        }
        if (e instanceof Name) {
            if (in.contains(((Name) e).id)) {
                safe.add((Name) e);
            }
        } else if (e instanceof Attribute || e instanceof Subscript) {
            parts(e, in);
        } else if (e instanceof Slice) {
            expr(((Slice) e).lower, in);
            expr(((Slice) e).upper, in);
            expr(((Slice) e).step, in);
        } else if (e instanceof Starred) {
            expr(((Starred) e).value, in);
        } else if (e instanceof BinOp) {
            expr(((BinOp) e).left, in);
            expr(((BinOp) e).right, in);
        } else if (e instanceof UnaryOp) {
            expr(((UnaryOp) e).operand, in);
        } else if (e instanceof BoolOp) {
            exprs(((BoolOp) e).values, in);
        } else if (e instanceof Compare) {
            expr(((Compare) e).left, in);
            exprs(((Compare) e).comparators, in);
        } else if (e instanceof IfExp) {
            expr(((IfExp) e).test, in);
            expr(((IfExp) e).body, in);
            expr(((IfExp) e).orelse, in);
        } else if (e instanceof NamedExpr) {
            expr(((NamedExpr) e).value, in);
        } else if (e instanceof Call) {
            Call c = (Call) e;
            expr(c.func, in);
            exprs(c.args, in);
            for (Keyword k : c.keywords) {
                expr(k.value, in);
            }
        } else if (e instanceof ListExpr) {
            exprs(((ListExpr) e).elts, in);
        } else if (e instanceof TupleExpr) {
            exprs(((TupleExpr) e).elts, in);
        } else if (e instanceof SetExpr) {
            exprs(((SetExpr) e).elts, in);
        } else if (e instanceof DictExpr) {
            for (Expr k : ((DictExpr) e).keys) {
                expr(k, in);
            }
            exprs(((DictExpr) e).values, in);
        }
        // Comprehensions, lambdas, f-strings, await, yield: left checked.
    }
}
