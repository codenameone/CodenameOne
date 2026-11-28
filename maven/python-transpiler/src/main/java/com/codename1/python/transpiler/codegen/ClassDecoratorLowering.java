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
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Lowers the standard class decorators whose effect is known, so the class
/// keeps its layout: each hands back the class it was given.
///
/// - `six.add_metaclass(M)` becomes the keyword `metaclass=M`, and a base
///   `six.with_metaclass(M, B...)` becomes the bases `B...` with that keyword.
///   Both are what the two helpers build at run time, a class of metaclass `M`.
/// - `typing_extensions.deprecated(msg, category=None)` (or `warnings.`) only
///   sets `__deprecated__`, so it becomes that class attribute.
/// - `typing.runtime_checkable` (or `typing_extensions.`) on a protocol
///   becomes the class attribute `_is_runtime_protocol = True`.
/// - `functools.total_ordering` becomes the comparison methods it would have
///   set, written for the root it picks (the stdlib's own `functools`), when
///   the roots are the class body's own and the class has no base.
///
/// A name is lowered only when the module imports it from that module and
/// binds it nowhere else; anything else keeps its decorator, and strict mode
/// still refuses the class.
public final class ClassDecoratorLowering {
    private static final String[] ORDER = {"__lt__", "__le__", "__gt__", "__ge__"};

    private ClassDecoratorLowering() {
    }

    /// Rewrites the decorated classes at module level of `tree` (path `path`).
    public static void rewrite(Node.Module tree, String path) {
        // bound name -> "module.name", for names imported from the modules here.
        Map<String, String> known = new HashMap<String, String>();
        // bound name -> module, for `import six`.
        Map<String, String> mods = new HashMap<String, String>();
        for (Stmt s : tree.body) {
            if (s instanceof ImportFrom && ((ImportFrom) s).level == 0 && relevant(((ImportFrom) s).module)) {
                for (Alias a : ((ImportFrom) s).names) {
                    known.put(a.asname != null ? a.asname : a.name, ((ImportFrom) s).module + "." + a.name);
                }
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if (relevant(a.name) && a.name.indexOf('.') < 0) {
                        mods.put(a.asname != null ? a.asname : a.name, a.name);
                    }
                }
            }
        }
        if (known.isEmpty() && mods.isEmpty()) {
            return;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        for (Stmt s : tree.body) {
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            withMetaclassBase(c, known, mods, bindings);
            for (int i = c.decorators.size() - 1; i >= 0; i--) {
                Expr d = c.decorators.get(i);
                String q = resolve(d instanceof Call ? ((Call) d).func : d, known, mods, bindings);
                if (q == null || !lower(c, d, q, path)) {
                    break;
                }
                c.decorators.remove(i);
            }
        }
    }

    private static boolean relevant(String module) {
        return "six".equals(module) || "typing_extensions".equals(module) || "typing".equals(module) || "warnings".equals(module)
                || "functools".equals(module);
    }

    /// "module.name" for a reference to one of the imported helpers.
    private static String resolve(Expr e, Map<String, String> known, Map<String, String> mods,
            Map<String, Integer> bindings) {
        if (e instanceof Name) {
            String id = ((Name) e).id;
            return known.containsKey(id) && bindings.getOrDefault(id, 0) == 1 ? known.get(id) : null;
        }
        if (e instanceof Attribute && ((Attribute) e).value instanceof Name) {
            String id = ((Name) ((Attribute) e).value).id;
            return mods.containsKey(id) && bindings.getOrDefault(id, 0) == 1
                    ? mods.get(id) + "." + ((Attribute) e).attr : null;
        }
        return null;
    }

    private static boolean hasMetaclass(ClassDef c) {
        for (Keyword k : c.keywords) {
            if (k.arg == null || "metaclass".equals(k.arg)) {
                return true;
            }
        }
        return false;
    }

    private static void withMetaclassBase(ClassDef c, Map<String, String> known, Map<String, String> mods,
            Map<String, Integer> bindings) {
        if (c.bases.size() != 1 || !(c.bases.get(0) instanceof Call) || hasMetaclass(c)) {
            return;
        }
        Call b = (Call) c.bases.get(0);
        if (!"six.with_metaclass".equals(resolve(b.func, known, mods, bindings)) || b.args.isEmpty()
                || !b.keywords.isEmpty()) {
            return;
        }
        for (Expr a : b.args) {
            if (a instanceof Node.Starred) {
                return;
            }
        }
        c.bases.clear();
        c.bases.addAll(b.args.subList(1, b.args.size()));
        c.keywords.add(new Keyword("metaclass", b.args.get(0)));
    }

    private static boolean lower(ClassDef c, Expr d, String q, String path) {
        if ("six.add_metaclass".equals(q)) {
            if (!(d instanceof Call) || ((Call) d).args.size() != 1 || !((Call) d).keywords.isEmpty()
                    || hasMetaclass(c) || ((Call) d).args.get(0) instanceof Node.Starred) {
                return false;
            }
            c.keywords.add(new Keyword("metaclass", ((Call) d).args.get(0)));
            return true;
        }
        if ("typing_extensions.deprecated".equals(q) || "warnings.deprecated".equals(q)) {
            return deprecated(c, d);
        }
        if ("typing.runtime_checkable".equals(q) || "typing_extensions.runtime_checkable".equals(q)) {
            return !(d instanceof Call) && runtimeCheckable(c);
        }
        if ("functools.total_ordering".equals(q)) {
            return !(d instanceof Call) && totalOrdering(c, path);
        }
        return false;
    }

    /// `@runtime_checkable` on a protocol only sets `_is_runtime_protocol`
    /// (the structural check itself is ProtocolChecks'). On any other class
    /// CPython raises, so the decorator stays.
    private static boolean runtimeCheckable(ClassDef c) {
        if (!ProtocolChecks.isProtocol(c)) {
            return false;
        }
        for (Stmt s : c.body) {
            if (binds(s, "_is_runtime_protocol")) {
                return false;
            }
        }
        Assign a = new Assign(new ArrayList<Expr>(Collections.singletonList((Expr) new Name("_is_runtime_protocol"))),
                new Constant(Constant.Kind.TRUE, "True"));
        DataclassInit.atLine(a, c.line);
        c.body.add(a);
        return true;
    }

    /// `@deprecated(msg, category=None)`: the decorator only stores `msg`.
    private static boolean deprecated(ClassDef c, Expr d) {
        if (!(d instanceof Call) || ((Call) d).args.size() != 1) {
            return false;
        }
        Call k = (Call) d;
        Expr msg = k.args.get(0);
        if (!(msg instanceof Constant) || ((Constant) msg).kind != Constant.Kind.STR) {
            return false;
        }
        boolean noCategory = false;
        for (Keyword kw : k.keywords) {
            if ("category".equals(kw.arg) && kw.value instanceof Constant
                    && ((Constant) kw.value).kind == Constant.Kind.NONE) {
                noCategory = true;
            } else if (!"stacklevel".equals(kw.arg) || !(kw.value instanceof Constant)) {
                return false;
            }
        }
        if (!noCategory) {
            return false;
        }
        for (Stmt s : c.body) {
            if (binds(s, "__deprecated__")) {
                return false;
            }
        }
        Assign a = new Assign(new ArrayList<Expr>(Collections.singletonList((Expr) new Name("__deprecated__"))), msg);
        DataclassInit.atLine(a, c.line);
        c.body.add(a);
        return true;
    }

    private static boolean binds(Stmt s, String name) {
        if (s instanceof FunctionDef) {
            return name.equals(((FunctionDef) s).name);
        }
        if (s instanceof ClassDef) {
            return name.equals(((ClassDef) s).name);
        }
        if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                if (!(t instanceof Name) || name.equals(((Name) t).id)) {
                    return true;
                }
            }
            return false;
        }
        // Anything else that could bind a name in the body: be safe.
        return !(s instanceof Node.ExprStmt || s instanceof Node.Pass || s instanceof Node.AnnAssign);
    }

    /// `@total_ordering` on a class with no base whose body defines its roots
    /// as plain methods: the stdlib's choice of root and its formulas.
    private static boolean totalOrdering(ClassDef c, String path) {
        if (!c.bases.isEmpty() || !c.keywords.isEmpty()) {
            return false;
        }
        Set<String> roots = new HashSet<String>();
        for (Stmt s : c.body) {
            for (String op : ORDER) {
                if (binds(s, op)) {
                    if (!(s instanceof FunctionDef) || !((FunctionDef) s).decorators.isEmpty()) {
                        return false;
                    }
                    roots.add(op);
                }
            }
        }
        String root = null;
        for (String op : ORDER) {
            if (roots.contains(op)) {
                root = op;
                break;
            }
        }
        if (root == null) {
            return false;
        }
        String lt;
        if ("__lt__".equals(root)) {
            lt = "r";
        } else if ("__le__".equals(root)) {
            lt = "r and self != other";
        } else if ("__gt__".equals(root)) {
            lt = "not r and self != other";
        } else {
            lt = "not r";
        }
        for (String op : ORDER) {
            if (roots.contains(op)) {
                continue;
            }
            String ret;
            if ("__lt__".equals(op)) {
                ret = "lt";
            } else if ("__le__".equals(op)) {
                ret = "lt or self == other";
            } else if ("__gt__".equals(op)) {
                ret = "not lt and self != other";
            } else {
                ret = "not lt";
            }
            String src = "def " + op + "(self, other):\n"
                    + "    r = self." + root + "(other)\n"
                    + "    if r is NotImplemented:\n"
                    + "        return r\n"
                    + "    lt = " + lt + "\n"
                    + "    return " + ret + "\n";
            FunctionDef f = ConstArgSpecialize.parseMethod(src, c.line, path);
            DataclassInit.atLine(f, c.line);
            c.body.add(f);
        }
        return true;
    }
}
