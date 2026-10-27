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

import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Compare;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.ExceptHandler;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.For;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.Match;
import com.codename1.python.transpiler.ast.Node.MatchCase;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Pattern;
import com.codename1.python.transpiler.ast.Node.Raise;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.Try;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import com.codename1.python.transpiler.ast.Node.While;
import com.codename1.python.transpiler.ast.Node.With;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Rewrites `match` statements and `try` statements with `except*` clauses
/// into ordinary statements, before scope analysis, so that every later pass
/// (and the generator lowering) sees nothing new.
///
/// Each `match` case becomes a call of the runtime matcher with a
/// description of its pattern (see `PyMatch` in the runtime), which answers
/// the captured names or None:
///
/// ```
/// $mS = subject
/// $mD = False
/// $mB = __cn1_match__(("q", ("c", "x"), ("_",)), $mS)
/// if $mB is not None:
///     x = $mB["x"]
///     if guard:
///         $mD = True
///         body
/// if not $mD:
///     ...the next case...
/// ```
///
/// Value and class expressions in a pattern are evaluated in the
/// description, so they are looked up each time the case is tried, as in
/// CPython; the names a case binds are assigned only once it matched.
///
/// A `try` with `except*` clauses keeps its body, `else` and `finally`, and
/// gets one `except BaseException` handler that splits the group clause by
/// clause through the runtime (see `ExceptionGroups` in the runtime):
///
/// ```
/// except BaseException as $xN:
///     $sN = __cn1_except_star__($xN)
///     $mN = __cn1_except_star_take__($sN, T1)
///     if $mN is not None:
///         e = $mN
///         try:
///             handler          # a bare `raise` here re-raises $mN
///         except BaseException as $rN:
///             __cn1_except_star_raised__($sN, $rN)
///     ...
///     __cn1_except_star_finish__($sN)
/// ```
public final class Desugar {
    private int counter;

    private Desugar() {
    }

    public static void rewrite(List<Stmt> body) {
        new Desugar().block(body);
    }

    private void block(List<Stmt> body) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            children(s);
            if (s instanceof Match) {
                List<Stmt> r = lower((Match) s);
                body.remove(i);
                body.addAll(i, r);
                i += r.size() - 1;
            } else if (s instanceof Try && ((Try) s).star) {
                body.set(i, exceptStar((Try) s));
            }
        }
    }

    private void children(Stmt s) {
        if (s instanceof If) {
            block(((If) s).body);
            block(((If) s).orelse);
        } else if (s instanceof While) {
            block(((While) s).body);
            block(((While) s).orelse);
        } else if (s instanceof For) {
            block(((For) s).body);
            block(((For) s).orelse);
        } else if (s instanceof With) {
            block(((With) s).body);
        } else if (s instanceof Try) {
            Try t = (Try) s;
            block(t.body);
            for (ExceptHandler h : t.handlers) {
                block(h.body);
            }
            block(t.orelse);
            block(t.finalbody);
        } else if (s instanceof FunctionDef) {
            block(((FunctionDef) s).body);
        } else if (s instanceof ClassDef) {
            block(((ClassDef) s).body);
        } else if (s instanceof Match) {
            for (MatchCase c : ((Match) s).cases) {
                block(c.body);
            }
        }
    }

    private List<Stmt> lower(Match m) {
        int n = ++counter;
        String subject = "$mS" + n;
        String done = "$mD" + n;
        String bound = "$mB" + n;
        List<Stmt> out = new ArrayList<Stmt>();
        out.add(assign(name(subject, m), m.subject, m));
        boolean flag = m.cases.size() > 1;
        if (flag) {
            out.add(assign(name(done, m), new Constant(Constant.Kind.FALSE, null).at(m.line, m.col), m));
        }
        List<Stmt> into = out;
        for (int i = 0; i < m.cases.size(); i++) {
            MatchCase c = m.cases.get(i);
            List<Stmt> cs = new ArrayList<Stmt>();
            Call call = new Call(name("__cn1_match__", c)).at(c.line, c.col);
            call.args.add(describe(c.pattern));
            call.args.add(name(subject, c));
            cs.add(assign(name(bound, c), call, c));
            Compare test = new Compare(name(bound, c)).at(c.line, c.col);
            test.ops.add("is not");
            test.comparators.add(new Constant(Constant.Kind.NONE, null).at(c.line, c.col));
            List<Stmt> matched = new ArrayList<Stmt>();
            List<String> names = new ArrayList<String>();
            captures(c.pattern, names);
            for (String x : names) {
                Expr v = new Subscript(name(bound, c), new Constant(Constant.Kind.STR, x).at(c.line, c.col))
                        .at(c.line, c.col);
                matched.add(assign(name(x, c), v, c));
            }
            List<Stmt> run = new ArrayList<Stmt>();
            if (flag) {
                run.add(assign(name(done, c), new Constant(Constant.Kind.TRUE, null).at(c.line, c.col), c));
            }
            run.addAll(c.body);
            if (c.guard != null) {
                matched.add(new If(c.guard, run, new ArrayList<Stmt>()).at(c.line, c.col));
            } else {
                matched.addAll(run);
            }
            cs.add(new If(test, matched, new ArrayList<Stmt>()).at(c.line, c.col));
            if (i == 0) {
                into.addAll(cs);
            } else {
                Expr notDone = new UnaryOp("not", name(done, c)).at(c.line, c.col);
                into.add(new If(notDone, cs, new ArrayList<Stmt>()).at(c.line, c.col));
            }
        }
        return out;
    }

    private Stmt exceptStar(Try t) {
        int n = ++counter;
        String x = "$x" + n;
        String st = "$s" + n;
        String m = "$m" + n;
        List<Stmt> h = new ArrayList<Stmt>();
        Call begin = new Call(name("__cn1_except_star__", t)).at(t.line, t.col);
        begin.args.add(name(x, t));
        h.add(assign(name(st, t), begin, t));
        for (ExceptHandler c : t.handlers) {
            Call take = new Call(name("__cn1_except_star_take__", c)).at(c.line, c.col);
            take.args.add(name(st, c));
            take.args.add(c.type);
            h.add(assign(name(m, c), take, c));
            Compare test = new Compare(name(m, c)).at(c.line, c.col);
            test.ops.add("is not");
            test.comparators.add(new Constant(Constant.Kind.NONE, null).at(c.line, c.col));
            List<Stmt> run = new ArrayList<Stmt>();
            if (c.name != null) {
                run.add(assign(name(c.name, c), name(m, c), c));
            }
            reraise(c.body, m);
            String r = "$r" + n;
            Call rec = new Call(name("__cn1_except_star_raised__", c)).at(c.line, c.col);
            rec.args.add(name(st, c));
            rec.args.add(name(r, c));
            List<Stmt> recBody = new ArrayList<Stmt>();
            recBody.add(new ExprStmt(rec).at(c.line, c.col));
            List<ExceptHandler> hs = new ArrayList<ExceptHandler>();
            hs.add(new ExceptHandler(name("BaseException", c), r, recBody).at(c.line, c.col));
            run.add(new Try(c.body, hs, new ArrayList<Stmt>(), new ArrayList<Stmt>(), false).at(c.line, c.col));
            h.add(new If(test, run, new ArrayList<Stmt>()).at(c.line, c.col));
        }
        Call fin = new Call(name("__cn1_except_star_finish__", t)).at(t.line, t.col);
        fin.args.add(name(st, t));
        h.add(new ExprStmt(fin).at(t.line, t.col));
        List<ExceptHandler> one = new ArrayList<ExceptHandler>();
        one.add(new ExceptHandler(name("BaseException", t), x, h).at(t.line, t.col));
        return new Try(t.body, one, t.orelse, t.finalbody, false).at(t.line, t.col);
    }

    /// Points each bare `raise` of an `except*` clause at the part of the
    /// group the clause took, rather than the whole exception being handled.
    private static void reraise(List<Stmt> body, String m) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            if (s instanceof Raise && ((Raise) s).exc == null) {
                body.set(i, new Raise(name(m, s), null).at(s.line, s.col));
            } else if (s instanceof If) {
                reraise(((If) s).body, m);
                reraise(((If) s).orelse, m);
            } else if (s instanceof While) {
                reraise(((While) s).body, m);
                reraise(((While) s).orelse, m);
            } else if (s instanceof For) {
                reraise(((For) s).body, m);
                reraise(((For) s).orelse, m);
            } else if (s instanceof With) {
                reraise(((With) s).body, m);
            } else if (s instanceof Try) {
                // Its own handlers re-raise what they caught.
                reraise(((Try) s).body, m);
                reraise(((Try) s).orelse, m);
                reraise(((Try) s).finalbody, m);
            }
        }
    }

    /// The names `p` binds, in the order they first appear.
    private static void captures(Pattern p, List<String> names) {
        if (p.name != null && p.kind != Pattern.Kind.VALUE && p.kind != Pattern.Kind.SINGLETON
                && p.kind != Pattern.Kind.CLASS && !names.contains(p.name)) {
            if (p.kind == Pattern.Kind.AS) {
                for (Pattern q : p.patterns) {
                    captures(q, names);
                }
                names.add(p.name);
                return;
            }
            names.add(p.name);
        }
        if (p.kind == Pattern.Kind.OR) {
            if (!p.patterns.isEmpty()) {
                captures(p.patterns.get(0), names);
            }
            return;
        }
        for (Pattern q : p.patterns) {
            captures(q, names);
        }
        for (Pattern q : p.kwdPatterns) {
            captures(q, names);
        }
    }

    private Expr describe(Pattern p) {
        List<Expr> t = new ArrayList<Expr>();
        switch (p.kind) {
            case WILDCARD:
                t.add(str("_", p));
                break;
            case CAPTURE:
                t.add(str("c", p));
                t.add(str(p.name, p));
                break;
            case VALUE:
                t.add(str("v", p));
                t.add(p.value);
                break;
            case SINGLETON:
                t.add(str("s", p));
                t.add(p.value);
                break;
            case STAR:
                t.add(str("*", p));
                t.add(p.name == null ? none(p) : str(p.name, p));
                break;
            case SEQUENCE:
                t.add(str("q", p));
                for (Pattern q : p.patterns) {
                    t.add(describe(q));
                }
                break;
            case MAPPING:
                t.add(str("m", p));
                t.add(tuple(new ArrayList<Expr>(p.keys), p));
                List<Expr> vs = new ArrayList<Expr>();
                for (Pattern q : p.patterns) {
                    vs.add(describe(q));
                }
                t.add(tuple(vs, p));
                t.add(p.name == null ? none(p) : str(p.name, p));
                break;
            case CLASS:
                t.add(str("k", p));
                t.add(p.value);
                List<Expr> pos = new ArrayList<Expr>();
                for (Pattern q : p.patterns) {
                    pos.add(describe(q));
                }
                t.add(tuple(pos, p));
                List<Expr> kn = new ArrayList<Expr>();
                List<Expr> kp = new ArrayList<Expr>();
                for (int i = 0; i < p.kwdAttrs.size(); i++) {
                    kn.add(str(p.kwdAttrs.get(i), p));
                    kp.add(describe(p.kwdPatterns.get(i)));
                }
                t.add(tuple(kn, p));
                t.add(tuple(kp, p));
                break;
            case OR:
                t.add(str("|", p));
                for (Pattern q : p.patterns) {
                    t.add(describe(q));
                }
                break;
            case AS:
                t.add(str("as", p));
                t.add(p.patterns.isEmpty() ? tuple(Collections.<Expr>singletonList(str("_", p)), p)
                        : describe(p.patterns.get(0)));
                t.add(str(p.name, p));
                break;
            default:
                throw new IllegalStateException(String.valueOf(p.kind));
        }
        return tuple(t, p);
    }

    private static Expr tuple(List<Expr> items, Pattern at) {
        return new TupleExpr(items).at(at.line, at.col);
    }

    private static Expr str(String s, Pattern at) {
        return new Constant(Constant.Kind.STR, s).at(at.line, at.col);
    }

    private static Expr none(Pattern at) {
        return new Constant(Constant.Kind.NONE, null).at(at.line, at.col);
    }

    private static Name name(String id, com.codename1.python.transpiler.ast.Node at) {
        return new Name(id).at(at.line, at.col);
    }

    private static Stmt assign(Name target, Expr value, com.codename1.python.transpiler.ast.Node at) {
        return new Assign(Collections.<Expr>singletonList(target), value).at(at.line, at.col);
    }
}
