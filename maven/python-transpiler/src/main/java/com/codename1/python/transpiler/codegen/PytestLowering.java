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
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Gives the bundled pytest a test table written at build time.
///
/// pytest finds tests and fixtures by name -- `dir(module)`, then
/// `getattr(module, name)` for every `test_*`, and a fixture by the name of
/// the parameter asking for it -- which is reflection. Here every module
/// that imports pytest gets one more statement, after its last test or
/// fixture definition,
///
/// ```python
/// import pytest as _cn1_pytest_mod
/// __cn1_pytest__ = _cn1_pytest_mod._Table([
///     ("f", "total", total, ("numbers",),
///      lambda _cn1_r: total(numbers=_cn1_r("numbers"))),
///     ("c", "TestGroup", TestGroup, [
///         ("test_m", TestGroup.test_m, ("k",),
///          lambda _cn1_s, _cn1_r: _cn1_s.test_m(k=_cn1_r("k")),
///          lambda _cn1_s: _cn1_s.test_m)]),
/// ])
/// ```
///
/// listing, in `dir` order, each module-level `test*` function and fixture,
/// each `Test*` class with its `test*` methods and fixtures, the fixture
/// names each one asks for (its parameters without defaults, read from the
/// signature) and a call that passes each of them literally. The runner
/// iterates the table and resolves a name through dictionaries of fixtures,
/// never through attributes.
///
/// `monkeypatch.setattr(obj, "name", value)`, `delattr(obj, "name")` and
/// `setattr("pkg.mod.attr", value)` get their accessors as one more keyword,
/// `_cn1_acc=(name, get, set, delete, owner)`, each lambda naming the
/// attribute literally; `owner` resolves a dotted target by trying each
/// module prefix of the path, longest first, with the rest as literal
/// attribute loads. No receiver is trusted by its name: a call statement
/// `r.setattr(...)` with a readable name becomes
/// `_cn1_pytest_mod._cn1_mp(r, delete, acc)(...)`, which hands the accessors
/// to `r` only when it is a MonkeyPatch at run time and otherwise calls
/// `r.setattr` unchanged, so any other object with such a method is
/// unaffected. A call the build cannot lower is refused in strict mode,
/// naming its line, when its receiver may be a MonkeyPatch: a parameter
/// named `monkeypatch` (the fixture) or a name bound from `MonkeyPatch()` or
/// `.context()`. A fixture defined inside a function, and
/// `run_module(sys.modules[__name__])` in a module that gets no table, are
/// refused the same way.
public final class PytestLowering {

    private PytestLowering() {
    }

    /// Adds the test table and the monkeypatch accessors to `tree`, the module at `path`.
    public static void rewrite(Node.Module tree, String path) {
        if (path.endsWith("pytest/__init__.py") || "pytest.py".equals(path) || !importsPytest(tree)) {
            return;
        }
        // One alias of pytest the generated code names, bound right after
        // the module's own import of it.
        tree.body.add(pytestImport(tree) + 1, Parser.parse("import pytest as _cn1_pytest_mod\n", path).body.get(0));
        List<String> known = new ArrayList<String>();
        Infer.children(tree, x -> knownMonkeypatches(x, known));
        monkeypatch(tree.body, path, known);
        nestedFixtures(tree.body, false);
        Map<String, String> entries = new TreeMap<String, String>();
        int last = -1;
        for (int i = 0; i < tree.body.size(); i++) {
            Node.Stmt s = tree.body.get(i);
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                if (f.name.startsWith("test") || isFixture(f)) {
                    entries.put(f.name, "(\"f\", \"" + f.name + "\", " + f.name + ", " + deps(f, false)
                            + ", lambda _cn1_r: " + call(f.name, f, false) + ")");
                    last = i;
                } else {
                    entries.remove(f.name);
                }
            } else if (s instanceof ClassDef) {
                ClassDef c = (ClassDef) s;
                if (c.name.startsWith("Test")) {
                    entries.put(c.name, classEntry(c));
                    last = i;
                } else {
                    entries.remove(c.name);
                }
            }
        }
        if (last < 0) {
            Infer.children(tree, PytestLowering::selfRunWithoutTable);
            return;
        }
        StringBuilder src = new StringBuilder("__cn1_pytest__ = _cn1_pytest_mod._Table([");
        for (String e : entries.values()) {
            src.append(e).append(", ");
        }
        src.append("])\n");
        Node.Module gen = Parser.parse(src.toString(), path);
        tree.body.addAll(last + 1, gen.body);
    }

    private static String classEntry(ClassDef c) {
        Map<String, String> members = new TreeMap<String, String>();
        for (Node.Stmt s : c.body) {
            if (!(s instanceof FunctionDef)) {
                continue;
            }
            FunctionDef f = (FunctionDef) s;
            if (f.name.startsWith("__") || !(f.name.startsWith("test") || isFixture(f))) {
                members.remove(f.name);
                continue;
            }
            boolean bound = !decorated(f, "staticmethod");
            members.put(f.name, "(\"" + f.name + "\", " + c.name + "." + f.name + ", " + deps(f, bound)
                    + ", lambda _cn1_s, _cn1_r: " + call("_cn1_s." + f.name, f, bound)
                    + ", lambda _cn1_s: _cn1_s." + f.name + ")");
        }
        StringBuilder b = new StringBuilder("(\"c\", \"" + c.name + "\", " + c.name + ", [");
        for (String m : members.values()) {
            b.append(m).append(", ");
        }
        return b.append("])").toString();
    }

    /// The parameters pytest fills with fixtures: those without a default,
    /// past `self`/`cls` for a method.
    private static List<Node.Arg> fixtureParams(FunctionDef f, boolean method) {
        Node.Arguments a = f.args;
        List<Node.Arg> r = new ArrayList<Node.Arg>();
        int required = a.positional.size() - a.defaults.size();
        for (int i = method ? 1 : 0; i < required; i++) {
            r.add(a.positional.get(i));
        }
        for (int i = 0; i < a.kwonly.size(); i++) {
            if (a.kwDefaults.get(i) == null) {
                r.add(a.kwonly.get(i));
            }
        }
        return r;
    }

    private static String deps(FunctionDef f, boolean method) {
        StringBuilder b = new StringBuilder("(");
        for (Node.Arg a : fixtureParams(f, method)) {
            b.append('"').append(a.name).append("\", ");
        }
        return b.append(')').toString();
    }

    private static String call(String callee, FunctionDef f, boolean method) {
        StringBuilder b = new StringBuilder(callee).append('(');
        for (Node.Arg a : fixtureParams(f, method)) {
            int at = f.args.positional.indexOf(a);
            if (at >= 0 && at < f.args.positionalOnly) {
                b.append("_cn1_r(\"").append(a.name).append("\"), ");
            } else {
                b.append(a.name).append("=_cn1_r(\"").append(a.name).append("\"), ");
            }
        }
        return b.append(')').toString();
    }

    private static boolean isFixture(FunctionDef f) {
        return decorated(f, "fixture");
    }

    private static boolean decorated(FunctionDef f, String name) {
        for (Expr d : f.decorators) {
            Expr e = d instanceof Call ? ((Call) d).func : d;
            if (e instanceof Node.Name && name.equals(((Node.Name) e).id)
                    || e instanceof Attribute && name.equals(((Attribute) e).attr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean importsPytest(Node.Module tree) {
        return pytestImport(tree) >= 0;
    }

    /// The index of the first top-level statement importing pytest, or -1.
    private static int pytestImport(Node.Module tree) {
        for (int i = 0; i < tree.body.size(); i++) {
            Node.Stmt s = tree.body.get(i);
            if (s instanceof ImportFrom && "pytest".equals(((ImportFrom) s).module)) {
                return i;
            }
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("pytest".equals(a.name)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /// Collects the names that may hold a MonkeyPatch: parameters named
    /// `monkeypatch` and names bound from `MonkeyPatch(...)` or `.context()`.
    private static void knownMonkeypatches(Object o, List<String> known) {
        if (o instanceof FunctionDef) {
            Node.Arguments a = ((FunctionDef) o).args;
            for (Node.Arg p : a.positional) {
                if ("monkeypatch".equals(p.name)) {
                    known.add(p.name);
                }
            }
            for (Node.Arg p : a.kwonly) {
                if ("monkeypatch".equals(p.name)) {
                    known.add(p.name);
                }
            }
        } else if (o instanceof Node.Assign && makesMonkeypatch(((Node.Assign) o).value)) {
            for (Expr t : ((Node.Assign) o).targets) {
                if (t instanceof Node.Name) {
                    known.add(((Node.Name) t).id);
                }
            }
        } else if (o instanceof Node.WithItem && makesMonkeypatch(((Node.WithItem) o).context)
                && ((Node.WithItem) o).vars instanceof Node.Name) {
            known.add(((Node.Name) ((Node.WithItem) o).vars).id);
        }
        Infer.children(o, x -> knownMonkeypatches(x, known));
    }

    private static boolean makesMonkeypatch(Expr e) {
        if (!(e instanceof Call)) {
            return false;
        }
        Expr f = ((Call) e).func;
        String n = f instanceof Node.Name ? ((Node.Name) f).id : f instanceof Attribute ? ((Attribute) f).attr : null;
        return "MonkeyPatch".equals(n) || "context".equals(n);
    }

    private static boolean mayBeMonkeypatch(Expr recv, List<String> known) {
        return recv instanceof Node.Name && known.contains(((Node.Name) recv).id);
    }

    private static Attribute patchCall(Object o) {
        if (o instanceof Call && ((Call) o).func instanceof Attribute) {
            Attribute fn = (Attribute) ((Call) o).func;
            if ("setattr".equals(fn.attr) || "delattr".equals(fn.attr)) {
                return fn;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void monkeypatch(Object o, String path, List<String> known) {
        if (o instanceof List) {
            List<Object> l = (List<Object>) o;
            for (int i = 0; i < l.size(); i++) {
                Object x = l.get(i);
                if (x instanceof ExprStmt && patchCall(((ExprStmt) x).value) != null) {
                    Call c = (Call) ((ExprStmt) x).value;
                    Attribute fn = (Attribute) c.func;
                    boolean set = "setattr".equals(fn.attr);
                    String acc = accessors(c, set);
                    if (acc != null) {
                        l.set(i, viaHelper(c, fn, set, acc, path));
                        continue;
                    }
                    if (mayBeMonkeypatch(fn.value, known)) {
                        c.refusal = refusalOf(c, set);
                    }
                }
                monkeypatch(x, path, known);
            }
            return;
        }
        Attribute fn = patchCall(o);
        if (fn != null && mayBeMonkeypatch(fn.value, known)) {
            // An expression position: the receiver is a MonkeyPatch by what
            // bound it, so the accessors are passed directly.
            Call c = (Call) o;
            boolean set = "setattr".equals(fn.attr);
            String acc = accessors(c, set);
            if (acc == null) {
                c.refusal = refusalOf(c, set);
            } else {
                c.keywords.add(new Keyword("_cn1_acc", ((ExprStmt) Parser.parse(acc + "\n", path).body.get(0)).value));
            }
        }
        if (!(o instanceof Node)) {
            return;
        }
        // Statement lists are walked as lists, so a call statement in any
        // block can be replaced in place.
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        || !java.lang.reflect.Modifier.isPublic(f.getModifiers())) {
                    continue;
                }
                try {
                    Object x = f.get(o);
                    if (x instanceof List || x instanceof Node) {
                        monkeypatch(x, path, known);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static ExprStmt viaHelper(Call c, Attribute fn, boolean set, String acc, String path) {
        ExprStmt s = (ExprStmt) Parser.parse("_cn1_pytest_mod._cn1_mp(_cn1_x, " + (set ? "False" : "True") + ", "
                + acc + ")()\n", path).body.get(0);
        Call outer = (Call) s.value;
        Call inner = (Call) outer.func;
        inner.args.set(0, fn.value);
        outer.args.addAll(c.args);
        outer.keywords.addAll(c.keywords);
        s.line = c.line;
        s.col = c.col;
        outer.line = c.line;
        outer.col = c.col;
        inner.line = c.line;
        inner.col = c.col;
        return s;
    }

    private static String refusalOf(Call c, boolean set) {
        String what = "monkeypatch." + (set ? "setattr" : "delattr") + "(...)";
        for (Keyword k : c.keywords) {
            if (k.arg == null || "name".equals(k.arg) || "target".equals(k.arg) || "value".equals(k.arg)) {
                return what + " with keyword or **mapping arguments the build cannot read; pass the target"
                        + " and a literal name positionally";
            }
        }
        return what + " with an attribute name the build cannot read stores by computed name, which is"
                + " reflection; write the name as a literal";
    }

    /// The `_cn1_acc` tuple's source for `c`, or null when the build cannot read the name.
    private static String accessors(Call c, boolean set) {
        for (Keyword k : c.keywords) {
            if (k.arg == null || "name".equals(k.arg) || "target".equals(k.arg) || "value".equals(k.arg)
                    || "_cn1_acc".equals(k.arg)) {
                return null;
            }
        }
        for (Expr a : c.args) {
            if (a instanceof Node.Starred) {
                return null;
            }
        }
        String dotted = set && c.args.size() == 2 ? str(c.args.get(0)) : null;
        String owner = "None";
        String name;
        if (dotted != null && dotted.indexOf('.') > 0) {
            name = dotted.substring(dotted.lastIndexOf('.') + 1);
            owner = owner(dotted.substring(0, dotted.lastIndexOf('.')));
        } else {
            name = c.args.size() >= 2 ? str(c.args.get(1)) : null;
        }
        if (name == null || !ident(name)) {
            return null;
        }
        return "(\"" + name + "\", lambda _cn1_o, _cn1_d: getattr(_cn1_o, \"" + name + "\", _cn1_d), "
                + "lambda _cn1_o, _cn1_v: setattr(_cn1_o, \"" + name + "\", _cn1_v), "
                + "lambda _cn1_o: delattr(_cn1_o, \"" + name + "\"), " + owner + ")";
    }

    /// Refuses a `@fixture` defined inside a function: the runner's table
    /// lists top-level and class fixtures only.
    private static void nestedFixtures(Object o, boolean inFunction) {
        if (o instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) o;
            if (inFunction && isFixture(f)) {
                f.refusal = "@pytest.fixture '" + f.name + "' defined inside a function is registered only when"
                        + " that function runs; define fixtures at module or class level";
            }
            for (Node.Stmt s : f.body) {
                nestedFixtures(s, true);
            }
            return;
        }
        Infer.children(o, x -> nestedFixtures(x, inFunction));
    }

    /// Refuses `run_module(sys.modules[__name__])` in a module that defines no tests.
    private static void selfRunWithoutTable(Object o) {
        if (o instanceof Call && ((Call) o).args.size() >= 1) {
            Call c = (Call) o;
            String n = c.func instanceof Node.Name ? ((Node.Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            Expr a = c.args.get(0);
            if ("run_module".equals(n) && a instanceof Node.Subscript
                    && ((Node.Subscript) a).index instanceof Node.Name
                    && "__name__".equals(((Node.Name) ((Node.Subscript) a).index).id)) {
                c.refusal = "pytest.run_module on this module, which defines no top-level test_* function or"
                        + " Test* class, so the build wrote it no test table";
            }
        }
        Infer.children(o, PytestLowering::selfRunWithoutTable);
    }

    /// `lambda: _cn1_pytest_mod._dotted(path, ((module, load), ...))`, longest module prefix first.
    private static String owner(String path) {
        String[] parts = path.split("\\.");
        StringBuilder b = new StringBuilder("lambda: _cn1_pytest_mod._dotted(\"" + path + "\", (");
        for (int i = parts.length; i > 0; i--) {
            StringBuilder mod = new StringBuilder();
            StringBuilder load = new StringBuilder("_cn1_m");
            for (int j = 0; j < parts.length; j++) {
                if (j < i) {
                    mod.append(j == 0 ? "" : ".").append(parts[j]);
                } else {
                    load.append('.').append(parts[j]);
                }
            }
            b.append("(\"").append(mod).append("\", lambda _cn1_m: ").append(load).append("), ");
        }
        return b.append("))").toString();
    }

    private static String str(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR ? ((Constant) e).text : null;
    }

    private static boolean ident(String s) {
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0)) || s.charAt(0) == '$') {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!(ch == '_' || ch < 128 && Character.isLetterOrDigit(ch))) {
                return false;
            }
        }
        return true;
    }
}
