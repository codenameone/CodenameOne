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
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.UnaryOp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Replaces a parameter default that names a module constant with the
/// constant: `def f(x, *, priority=PRIORITY_GENERIC)` after
/// `PRIORITY_GENERIC = 10.0` becomes `def f(x, *, priority=10.0)`.
///
/// Python evaluates a default once, when the `def` runs, so the parameter
/// holds whatever the name was bound to then. When the module binds the
/// name exactly once, at its top level, to a literal, and that binding
/// comes before the `def` in the module's body, the value is that literal
/// whatever happens later. A call that omits the argument can then be bound
/// at build time (Infer#staticArgs), which takes only constant defaults.
///
/// Only top-level functions and the methods of top-level classes are
/// rewritten; a method's default is left alone when its class body binds
/// the name first, since the class namespace is searched before the
/// module's.
public final class ConstantDefaults {
    private ConstantDefaults() {
    }

    public static void rewrite(Node.Module tree) {
        for (Stmt s : tree.body) {
            if (s instanceof ImportFrom) {
                for (Node.Alias a : ((ImportFrom) s).names) {
                    if ("*".equals(a.name)) {
                        // May bind any name, at any point.
                        return;
                    }
                }
            }
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        Map<String, Expr> constants = new HashMap<String, Expr>();
        for (Stmt s : tree.body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                Expr v = ((Assign) s).value;
                if (literal(v) && bindings.getOrDefault(n, 0) == 1) {
                    constants.put(n, v);
                }
            } else if (s instanceof FunctionDef) {
                replace((FunctionDef) s, constants, null);
            } else if (s instanceof ClassDef) {
                Map<String, Integer> local = new HashMap<String, Integer>();
                for (Stmt m : ((ClassDef) s).body) {
                    if (m instanceof FunctionDef) {
                        replace((FunctionDef) m, constants, local);
                        local.merge(((FunctionDef) m).name, 1, Integer::sum);
                    } else {
                        DataclassInit.count(m, local);
                    }
                }
            }
        }
    }

    private static void replace(FunctionDef f, Map<String, Expr> constants, Map<String, Integer> classBound) {
        replace(f.args.defaults, constants, classBound);
        replace(f.args.kwDefaults, constants, classBound);
    }

    private static void replace(List<Expr> defaults, Map<String, Expr> constants, Map<String, Integer> classBound) {
        for (int i = 0; i < defaults.size(); i++) {
            Expr d = defaults.get(i);
            if (d instanceof Name && constants.containsKey(((Name) d).id)
                    && (classBound == null || !classBound.containsKey(((Name) d).id))) {
                Expr c = copy(constants.get(((Name) d).id));
                c.line = d.line;
                c.col = d.col;
                defaults.set(i, c);
            }
        }
    }

    /// A literal whose value does not depend on when it is evaluated: a
    /// number, string, bytes, bool or None, or a signed number.
    private static boolean literal(Expr v) {
        if (v instanceof Constant) {
            return true;
        }
        return v instanceof UnaryOp && ("-".equals(((UnaryOp) v).op) || "+".equals(((UnaryOp) v).op))
                && ((UnaryOp) v).operand instanceof Constant;
    }

    private static Expr copy(Expr v) {
        if (v instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) v;
            UnaryOp r = new UnaryOp(u.op, copy(u.operand));
            r.line = u.line;
            r.col = u.col;
            return r;
        }
        Constant c = (Constant) v;
        Constant r = new Constant(c.kind, c.text);
        r.line = c.line;
        r.col = c.col;
        return r;
    }
}
