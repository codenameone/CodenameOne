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
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Delete;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.Global;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/// `threading.local` stores every attribute by name in a per-thread dict,
/// through `__getattr__`/`__setattr__`, which a closed world cannot compile.
/// Here a module-level `x = threading.local()` (or `local()` imported from
/// `threading`), bound once, whose every use in the program is `x.<name>`
/// (read, store or `del`) or `hasattr`/`getattr`/`setattr`/`delattr` of `x`
/// with a literal name, becomes an instance of a generated holder class:
/// one property per name the program uses, each reading or writing that
/// field of a generated slot object kept per thread ident. Names are
/// literal, so nothing is looked up by name at run time.
///
/// Anything else -- `x` passed, returned or rebound, `global x`, any other
/// module importing a name `x` (or `*`) or reading an attribute named `x`,
/// a subclass of `local` -- leaves the statement as it is, and the bundled
/// `threading.local` is refused in strict mode as before.
public final class ThreadLocalLowering {
    private ThreadLocalLowering() {
    }

    /// Lowers the thread-local objects of every module of `trees`.
    public static void rewrite(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String p = e.getKey();
            if (p.matches("(.*/)?threading\\.py")) {
                continue;
            }
            rewrite(e.getValue(), p, trees);
        }
    }

    private static void rewrite(Node.Module tree, String path, Map<String, Node.Module> trees) {
        Set<String> mods = new HashSet<String>();
        Set<String> locals = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("threading".equals(a.name)) {
                        mods.add(a.asname != null ? a.asname : a.name);
                    }
                }
            } else if (s instanceof ImportFrom && ((ImportFrom) s).level == 0
                    && "threading".equals(((ImportFrom) s).module)) {
                for (Alias a : ((ImportFrom) s).names) {
                    if ("local".equals(a.name)) {
                        locals.add(a.asname != null ? a.asname : a.name);
                    }
                }
            }
        }
        if (mods.isEmpty() && locals.isEmpty()) {
            return;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        for (String m : mods) {
            if (bindings.getOrDefault(m, 0) != 1) {
                return;
            }
        }
        for (String l : locals) {
            if (bindings.getOrDefault(l, 0) != 1) {
                return;
            }
        }
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1
                    || !(((Assign) s).targets.get(0) instanceof Name) || !isLocalCall(((Assign) s).value, mods, locals)) {
                continue;
            }
            String x = ((Name) ((Assign) s).targets.get(0)).id;
            if (bindings.getOrDefault(x, 0) != 1 || escapes(x, tree, trees)) {
                continue;
            }
            Set<String> names = new TreeSet<String>();
            boolean[] bad = {false};
            boolean[] deletes = {false};
            collect(tree.body, null, x, s, names, bad, deletes);
            if (bad[0]) {
                continue;
            }
            String holder = "_cn1_tl_" + x;
            String slots = "_cn1_tls_" + x;
            StringBuilder src = new StringBuilder();
            src.append("from threading import get_ident as _cn1_tl_ident\n\n\n");
            src.append("class ").append(slots).append(":\n    def __init__(self):\n");
            if (names.isEmpty()) {
                src.append("        pass\n");
            }
            for (String n : names) {
                src.append("        self.").append(n).append(" = None\n");
                src.append("        self.").append(n).append("_cn1_set = False\n");
            }
            src.append("\n\nclass ").append(holder).append(":\n    def __init__(self):\n")
                    .append("        self._cn1_d = {}\n\n")
                    .append("    def _cn1_ns(self):\n        me = _cn1_tl_ident()\n")
                    .append("        ns = self._cn1_d.get(me)\n        if ns is None:\n")
                    .append("            ns = ").append(slots).append("()\n            self._cn1_d[me] = ns\n")
                    .append("        return ns\n");
            for (String n : names) {
                src.append("\n    @property\n    def ").append(n).append("(self):\n")
                        .append("        ns = self._cn1_ns()\n        if not ns.").append(n).append("_cn1_set:\n")
                        .append("            raise AttributeError(\"'_thread._local' object has no attribute '")
                        .append(n).append("'\")\n        return ns.").append(n).append("\n");
                src.append("\n    @").append(n).append(".setter\n    def ").append(n).append("(self, v):\n")
                        .append("        ns = self._cn1_ns()\n        ns.").append(n).append(" = v\n")
                        .append("        ns.").append(n).append("_cn1_set = True\n");
                if (deletes[0]) {
                    src.append("\n    @").append(n).append(".deleter\n    def ").append(n).append("(self):\n")
                            .append("        ns = self._cn1_ns()\n        if not ns.").append(n)
                            .append("_cn1_set:\n            raise AttributeError(\"").append(n).append("\")\n")
                            .append("        ns.").append(n).append(" = None\n")
                            .append("        ns.").append(n).append("_cn1_set = False\n");
                }
            }
            List<Stmt> gen = Parser.parse(src.toString(), path).body;
            for (Stmt g : gen) {
                DataclassInit.atLine(g, s.line);
            }
            Call c = new Call(new Name(holder));
            c.line = s.line;
            c.func.line = s.line;
            Assign a = new Assign(((Assign) s).targets, c);
            a.line = s.line;
            tree.body.set(i, a);
            tree.body.addAll(i, gen);
            i += gen.size();
        }
    }

    private static boolean isLocalCall(Expr e, Set<String> mods, Set<String> locals) {
        if (!(e instanceof Call) || !((Call) e).args.isEmpty() || !((Call) e).keywords.isEmpty()) {
            return false;
        }
        Expr f = ((Call) e).func;
        if (f instanceof Name) {
            return locals.contains(((Name) f).id);
        }
        return f instanceof Attribute && "local".equals(((Attribute) f).attr) && ((Attribute) f).value instanceof Name
                && mods.contains(((Name) ((Attribute) f).value).id);
    }

    /// Whether another module could reach `x`: it imports a name `x` or
    /// `*`, or reads an attribute named `x` of anything.
    private static boolean escapes(String x, Node.Module self, Map<String, Node.Module> trees) {
        boolean[] hit = {false};
        for (Node.Module t : trees.values()) {
            if (t != self) { //NOPMD CompareObjectsWithEquals - identity is meant
                reaches(t, x, hit);
                if (hit[0]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void reaches(Object o, String x, boolean[] hit) {
        if (hit[0]) {
            return;
        }
        if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                if (x.equals(a.name) || "*".equals(a.name)) {
                    hit[0] = true;
                }
            }
        } else if (o instanceof Attribute && x.equals(((Attribute) o).attr)) {
            hit[0] = true;
        }
        Infer.children(o, c -> reaches(c, x, hit));
    }

    private static final Set<String> REFLECT = new HashSet<String>(java.util.Arrays.asList("hasattr", "getattr",
            "setattr", "delattr"));

    /// Collects the literal names used on `x`, setting `bad` for any other
    /// use of it.
    private static void collect(Object o, Object parent, String x, Stmt def, Set<String> names, boolean[] bad,
            boolean[] deletes) {
        if (bad[0]) {
            return;
        }
        if (o instanceof Global && ((Global) o).names.contains(x)) {
            bad[0] = true;
            return;
        }
        if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                        && x.equals(((Name) ((Attribute) t).value).id)) {
                    deletes[0] = true;
                }
            }
        }
        if (o instanceof Attribute && ((Attribute) o).value instanceof Name
                && x.equals(((Name) ((Attribute) o).value).id)) {
            String a = ((Attribute) o).attr;
            if (a.startsWith("__")) {
                bad[0] = true;
            }
            names.add(a);
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && REFLECT.contains(((Name) ((Call) o).func).id)) {
            Call c = (Call) o;
            if (!c.args.isEmpty() && c.args.get(0) instanceof Name && x.equals(((Name) c.args.get(0)).id)) {
                if (c.args.size() < 2 || !(c.args.get(1) instanceof Constant)
                        || ((Constant) c.args.get(1)).kind != Constant.Kind.STR || !c.keywords.isEmpty()
                        || !((Constant) c.args.get(1)).text.matches("[A-Za-z_][A-Za-z0-9_]*")
                        || ((Constant) c.args.get(1)).text.startsWith("__")) {
                    bad[0] = true;
                    return;
                }
                if ("delattr".equals(((Name) c.func).id)) {
                    deletes[0] = true;
                }
                names.add(((Constant) c.args.get(1)).text);
                for (int i = 1; i < c.args.size(); i++) {
                    collect(c.args.get(i), c, x, def, names, bad, deletes);
                }
                return;
            }
        }
        if (o instanceof Name && x.equals(((Name) o).id)) {
            if (parent != def) { //NOPMD CompareObjectsWithEquals - identity is meant
                bad[0] = true;
            }
            return;
        }
        if (o instanceof Node.FunctionDef) {
            // A parameter named `x` shadows it; uses of the parameter would
            // then be miscounted, so the whole object is left alone.
            if (paramNamed((Node.FunctionDef) o, x)) {
                bad[0] = true;
                return;
            }
        }
        Infer.children(o, c -> collect(c, o, x, def, names, bad, deletes));
    }

    private static boolean paramNamed(Node.FunctionDef f, String x) {
        boolean[] hit = {false};
        Infer.children(f.args, c -> {
            if (c instanceof Node && x.equals(argName(c))) {
                hit[0] = true;
            }
        });
        return hit[0];
    }

    private static String argName(Object o) {
        return o instanceof Node.Arg ? ((Node.Arg) o).name : null;
    }
}
