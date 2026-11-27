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
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/// Third-party helpers whose behaviour Codename One provides itself, at
/// compile time, wherever a package uses them.
///
/// Some packages route ordinary calls through plumbing that does nothing a
/// program can observe and that defeats typing: a decorator that wraps a
/// method in a `(*args, **kwargs)` closure to emit a warning nobody sees, an
/// alias whose body is `getattr(self, new_name)`. Every caller of such a
/// method then reaches a function of unknown type, and every class it is on
/// is loose. The helpers are rewritten where they are used, right after
/// parsing, into what they mean:
///
/// - bs4's `@_deprecated(...)` (bs4._deprecation) leaves the method as it
///   is, with the warning the wrapper issued as its first statement.
/// - `X = _deprecated_alias("X", "y", v)` in a class body is a property that
///   warns and reads or writes `self.y`.
/// - `X = _deprecated_function_alias("X", "y", v)` is a method that warns and
///   calls `self.y` with what it was given.
///
/// Each warning is the helper's own `DeprecationWarning`, issued with the
/// same message and stack level, so where Python shows one (a call from
/// `__main__`) it is shown here too. The method keeps its own `__name__`,
/// as `functools.wraps` made the wrapper report; an alias reports its own
/// name where bs4's said `alias`.
///
/// A use that does not have this exact shape -- names that are not string
/// literals, the helper called anywhere but a class body -- is left alone
/// and compiled as the package wrote it.
public final class KnownHelpers {
    private enum Kind { DROP_DECORATOR, PROPERTY_ALIAS, METHOD_ALIAS }

    /// Module and function name to what the helper is.
    private static final Map<String, Kind> HELPERS = new HashMap<String, Kind>();

    static {
        HELPERS.put("bs4._deprecation._deprecated", Kind.DROP_DECORATOR);
        HELPERS.put("bs4._deprecation._deprecated_alias", Kind.PROPERTY_ALIAS);
        HELPERS.put("bs4._deprecation._deprecated_function_alias", Kind.METHOD_ALIAS);
    }

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private KnownHelpers() {
    }

    /// Rewrites the uses in `tree`, the module at relative path `path`.
    public static void rewrite(Node.Module tree, String path) {
        Map<String, Kind> local = imported(tree, path);
        if (local.isEmpty()) {
            return;
        }
        boolean[] used = {false};
        for (Stmt s : tree.body) {
            visit(s, local, path, used);
        }
        if (used[0]) {
            // The module the rewritten members warn through, after its
            // docstring and `from __future__` imports.
            int at = 0;
            while (at < tree.body.size() && (docstring(tree.body.get(at))
                    || tree.body.get(at) instanceof ImportFrom && "__future__".equals(((ImportFrom) tree.body.get(at)).module))) {
                at++;
            }
            Stmt imp = Parser.parse("import warnings as " + WARNINGS + "\n", path).body.get(0);
            at(imp, tree.body.isEmpty() ? 1 : tree.body.get(Math.min(at, tree.body.size() - 1)).line);
            tree.body.add(at, imp);
        }
        dropImports(tree, path, local.keySet());
    }

    /// Takes the helpers out of the imports that bind them once nothing in
    /// the module names them any more: a package `__init__` that imported
    /// one would otherwise keep it, and everything it does, alive as one of
    /// the package's attributes.
    private static void dropImports(Node.Module tree, String path, java.util.Set<String> helpers) {
        java.util.Set<String> named = new java.util.HashSet<String>();
        for (Stmt s : tree.body) {
            if (!(s instanceof ImportFrom)) {
                names(s, named);
            }
        }
        for (int i = tree.body.size() - 1; i >= 0; i--) {
            Stmt s = tree.body.get(i);
            if (!(s instanceof ImportFrom) || absolute((ImportFrom) s, path) == null) {
                continue;
            }
            List<Alias> keep = new ArrayList<Alias>();
            for (Alias a : ((ImportFrom) s).names) {
                String bound = a.asname != null ? a.asname : a.name;
                if (!helpers.contains(bound) || named.contains(bound)) {
                    keep.add(a);
                }
            }
            if (keep.size() == ((ImportFrom) s).names.size()) {
                continue;
            }
            if (keep.isEmpty()) {
                tree.body.remove(i);
            } else {
                ImportFrom f = new ImportFrom(((ImportFrom) s).module, ((ImportFrom) s).level, keep);
                f.line = s.line;
                tree.body.set(i, f);
            }
        }
    }

    /// Every name `o` reads, and every string under it (an `__all__` entry
    /// names an attribute too).
    private static void names(Object o, java.util.Set<String> out) {
        if (o instanceof Name) {
            out.add(((Name) o).id);
        } else if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            out.add(((Constant) o).text);
        }
        Infer.children(o, x -> names(x, out));
    }

    /// The name the rewritten members reach `warnings` by: one no package
    /// spells.
    private static final String WARNINGS = "_cn1_warnings";

    private static boolean docstring(Stmt s) {
        return s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant
                && ((Constant) ((ExprStmt) s).value).kind == Constant.Kind.STR;
    }

    /// The helpers `tree` imports at module level, by the name it binds.
    private static Map<String, Kind> imported(Node.Module tree, String path) {
        Map<String, Kind> r = new HashMap<String, Kind>();
        for (Stmt s : tree.body) {
            if (!(s instanceof ImportFrom)) {
                continue;
            }
            String module = absolute((ImportFrom) s, path);
            if (module == null) {
                continue;
            }
            for (Alias a : ((ImportFrom) s).names) {
                Kind k = HELPERS.get(module + "." + a.name);
                if (k != null) {
                    r.put(a.asname != null ? a.asname : a.name, k);
                }
            }
        }
        // A module that binds one of those names again (a def, an
        // assignment) means something else by it.
        for (Stmt s : tree.body) {
            if (s instanceof FunctionDef) {
                r.remove(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                r.remove(((ClassDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name) {
                        r.remove(((Name) t).id);
                    }
                }
            }
        }
        return r;
    }

    /// The dotted module an import names, relative ones resolved against
    /// `path`; null when it climbs out of the tree.
    private static String absolute(ImportFrom f, String path) {
        if (f.level == 0) {
            return f.module;
        }
        String[] parts = path.replace('\\', '/').split("/");
        // The package a module is in is its directory, `__init__.py` too.
        int keep = parts.length - 1 - (f.level - 1);
        if (keep < 0) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < keep; i++) {
            b.append(b.length() == 0 ? "" : ".").append(parts[i]);
        }
        if (f.module != null) {
            b.append(b.length() == 0 ? "" : ".").append(f.module);
        }
        return b.toString();
    }

    private static void visit(Stmt s, Map<String, Kind> local, String path, boolean[] used) {
        if (s instanceof FunctionDef) {
            unwrap((FunctionDef) s, local, path, used);
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            for (int i = 0; i < c.body.size(); i++) {
                Stmt m = c.body.get(i);
                List<Stmt> with = alias(m, local, path);
                if (with != null) {
                    used[0] = true;
                    c.body.remove(i);
                    c.body.addAll(i, with);
                    i += with.size() - 1;
                } else {
                    visit(m, local, path, used);
                }
            }
        }
    }

    /// Takes a `@_deprecated(replaced_by, version)` off `f`, issuing its
    /// warning as `f`'s first statement (after a docstring) instead. Only
    /// with literal arguments: the message is built from them.
    private static void unwrap(FunctionDef f, Map<String, Kind> local, String path, boolean[] used) {
        for (int i = f.decorators.size() - 1; i >= 0; i--) {
            Expr d = f.decorators.get(i);
            if (!(d instanceof Call) || !(((Call) d).func instanceof Name)
                    || local.get(((Name) ((Call) d).func).id) != Kind.DROP_DECORATOR) {
                continue;
            }
            Call c = (Call) d;
            String by = argument(c, 0, "replaced_by");
            String version = by != null ? argument(c, 1, "version") : null;
            if (version == null) {
                continue;
            }
            Stmt warn = Parser.parse(warn("Call to deprecated method " + f.name + ". (Replaced by " + by
                    + ") -- Deprecated since version " + version + ".") + "\n", path).body.get(0);
            int line = f.body.isEmpty() ? f.line : f.body.get(0).line;
            at(warn, line);
            f.body.add(!f.body.isEmpty() && docstring(f.body.get(0)) ? 1 : 0, warn);
            f.decorators.remove(i);
            used[0] = true;
        }
    }

    /// A statement issuing a `DeprecationWarning` with message `m`, at the
    /// stack level bs4's helpers use: the caller of the deprecated member.
    private static String warn(String m) {
        return WARNINGS + ".warn(" + pyStr(m) + ", DeprecationWarning, stacklevel=2)";
    }

    /// `s` as a Python string literal.
    private static String pyStr(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' || ch == '"') {
                b.append('\\').append(ch);
            } else if (ch < 0x20 || ch > 0x7e) {
                b.append(String.format("\\U%08x", (int) ch));
            } else {
                b.append(ch);
            }
        }
        return b.append('"').toString();
    }

    /// What class-body statement `s` means when it binds an alias, or null.
    private static List<Stmt> alias(Stmt s, Map<String, Kind> local, String path) {
        if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1
                || !(((Assign) s).targets.get(0) instanceof Name) || !(((Assign) s).value instanceof Call)) {
            return null;
        }
        Call c = (Call) ((Assign) s).value;
        if (!(c.func instanceof Name) || !c.keywords.isEmpty() || c.args.size() < 2) {
            return null;
        }
        Kind k = local.get(((Name) c.func).id);
        String name = ((Name) ((Assign) s).targets.get(0)).id;
        String old = literal(c.args.get(0));
        String to = literal(c.args.get(1));
        String version = c.args.size() == 3 ? literal(c.args.get(2)) : null;
        if (k == null || k == Kind.DROP_DECORATOR || old == null || to == null || version == null
                || !IDENTIFIER.matcher(to).matches()) {
            return null;
        }
        String tail = ". (Replaced by " + to + ") -- Deprecated since version " + version + ".";
        String src;
        if (k == Kind.PROPERTY_ALIAS) {
            src = "class _Alias:\n"
                    + "    @property\n"
                    + "    def " + name + "(self):\n"
                    + "        " + warn("Access to deprecated property " + old + tail) + "\n"
                    + "        return self." + to + "\n"
                    + "    @" + name + ".setter\n"
                    + "    def " + name + "(self, value):\n"
                    + "        " + warn("Write to deprecated property " + old + tail) + "\n"
                    + "        self." + to + " = value\n";
        } else {
            src = "class _Alias:\n"
                    + "    def " + name + "(self, *args, **kwargs):\n"
                    + "        " + warn("Call to deprecated method " + old + tail) + "\n"
                    + "        return self." + to + "(*args, **kwargs)\n";
        }
        Node.Module m = Parser.parse(src, path);
        List<Stmt> r = new ArrayList<Stmt>(((ClassDef) m.body.get(0)).body);
        for (Stmt x : r) {
            at(x, s.line);
        }
        return r;
    }

    /// Puts `o` and everything under it on `line`: a message about the
    /// alias names the package's own line.
    private static void at(Object o, int line) {
        if (o instanceof Node) {
            ((Node) o).line = line;
        }
        Infer.children(o, x -> at(x, line));
    }

    /// The string literal passed as parameter `name`, at position `pos`
    /// or by keyword, of a call taking exactly (replaced_by, version);
    /// null for any other shape.
    private static String argument(Call c, int pos, String name) {
        if (c.args.size() + c.keywords.size() != 2) {
            return null;
        }
        if (pos < c.args.size()) {
            return literal(c.args.get(pos));
        }
        for (Node.Keyword k : c.keywords) {
            if (name.equals(k.arg)) {
                return literal(k.value);
            }
        }
        return null;
    }

    private static String literal(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR ? ((Constant) e).text : null;
    }
}
