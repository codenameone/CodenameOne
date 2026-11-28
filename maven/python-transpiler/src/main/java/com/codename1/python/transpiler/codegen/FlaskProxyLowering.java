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
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/// Flask's `request`, `g`, `session` and `current_app` are proxies that
/// forward any attribute, by name, to the current request context's
/// object. The bundled `flask._Proxy` keeps only the item, container and
/// truth protocols; here the attribute names the program uses on each
/// proxy -- `request.<name>` read, stored or deleted, and
/// `hasattr`/`getattr`/`setattr`/`delattr` with a literal name -- become
/// properties of a generated subclass, one module `_cn1_flask_proxies`
/// for the program, and each `from flask import request` imports that
/// module's instance instead. Every name is literal, so nothing is looked
/// up by name at run time.
///
/// A proxy that escapes where the build cannot follow its attribute uses
/// -- passed to a call, stored, returned, read as `flask.request`, or read
/// with a computed name -- is refused in strict mode, naming the line.
public final class FlaskProxyLowering {
    /// The path of the generated module.
    public static final String PATH = "_cn1_flask_proxies.py";

    private static final String[] PROXIES = {"request", "g", "session", "current_app"};
    private static final Map<String, String> WHAT = new HashMap<String, String>();
    private static final Set<String> REFLECT = new HashSet<String>(Arrays.asList("hasattr", "getattr", "setattr",
            "delattr"));
    /// Builtins whose argument the proxy answers through a protocol it keeps.
    private static final Set<String> PROTOCOL = new HashSet<String>(Arrays.asList("len", "bool", "iter", "repr",
            "str", "list", "dict", "sorted", "tuple", "set"));

    static {
        WHAT.put("request", "request");
        WHAT.put("g", "g");
        WHAT.put("session", "session");
        WHAT.put("current_app", "app");
    }

    private FlaskProxyLowering() {
    }

    private static final class Use {
        final Set<String> names = new TreeSet<String>();
        final Set<String> stored = new HashSet<String>();
        final Set<String> deleted = new HashSet<String>();
    }

    /// Lowers the flask proxies of the program's modules; the source of the
    /// generated module, or null when no module imports a proxy.
    public static String rewrite(Map<String, Node.Module> trees) {
        Map<String, Use> uses = new LinkedHashMap<String, Use>();
        for (String p : PROXIES) {
            uses.put(p, new Use());
        }
        boolean any = false;
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String path = e.getKey().replace('\\', '/');
            if (path.startsWith("flask/") || PATH.equals(path)) {
                continue;
            }
            any |= module(e.getValue(), uses);
        }
        if (!any) {
            return null;
        }
        StringBuilder src = new StringBuilder("import flask as _cn1_f\n");
        for (String p : PROXIES) {
            Use u = uses.get(p);
            String k = "_cn1_" + p + "_proxy";
            src.append("\n\nclass ").append(k).append("(_cn1_f._Proxy):\n    pass\n");
            for (String n : u.names) {
                src.append("\n    @property\n    def ").append(n).append("(self):\n        return self._get().")
                        .append(n).append("\n");
                if (u.stored.contains(n)) {
                    src.append("\n    @").append(n).append(".setter\n    def ").append(n)
                            .append("(self, v):\n        self._get().").append(n).append(" = v\n");
                }
                if (u.deleted.contains(n)) {
                    src.append("\n    @").append(n).append(".deleter\n    def ").append(n)
                            .append("(self):\n        del self._get().").append(n).append("\n");
                }
            }
            src.append("\n\n").append(p).append(" = ").append(k).append("(\"").append(WHAT.get(p)).append("\")\n");
        }
        return src.toString();
    }

    /// Redirects the proxy imports of one module and collects their uses.
    private static boolean module(Node.Module tree, Map<String, Use> uses) {
        Map<String, String> bound = new HashMap<String, String>();
        Set<String> flaskMods = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("flask".equals(a.name)) {
                        flaskMods.add(a.asname != null ? a.asname : a.name);
                    }
                }
            }
        }
        boolean any = false;
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (!(s instanceof ImportFrom) || ((ImportFrom) s).level != 0
                    || !"flask".equals(((ImportFrom) s).module)) {
                continue;
            }
            List<Alias> keep = new ArrayList<Alias>();
            List<Alias> moved = new ArrayList<Alias>();
            for (Alias a : ((ImportFrom) s).names) {
                if (WHAT.containsKey(a.name)) {
                    moved.add(a);
                    bound.put(a.asname != null ? a.asname : a.name, a.name);
                } else {
                    keep.add(a);
                }
            }
            if (moved.isEmpty()) {
                continue;
            }
            any = true;
            ImportFrom gen = new ImportFrom(PATH.substring(0, PATH.length() - 3), 0, moved);
            DataclassInit.atLine(gen, s.line);
            if (keep.isEmpty()) {
                tree.body.set(i, gen);
            } else {
                ImportFrom rest = new ImportFrom("flask", 0, keep);
                DataclassInit.atLine(rest, s.line);
                tree.body.set(i, rest);
                tree.body.add(++i, gen);
            }
        }
        if (bound.isEmpty() && flaskMods.isEmpty()) {
            return false;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        for (Map.Entry<String, String> b : bound.entrySet()) {
            if (bindings.getOrDefault(b.getKey(), 0) != 1) {
                // Rebound somewhere: refuse every use rather than guess.
                walk(tree.body, null, b.getKey(), null, null);
            } else {
                walk(tree.body, null, b.getKey(), uses.get(b.getValue()), null);
            }
        }
        for (String m : flaskMods) {
            walk(tree.body, null, m, null, m);
        }
        return any;
    }

    private static final String ESCAPE = "a flask proxy used other than by a literal attribute, an item, a"
            + " container test or its truth cannot be lowered at build time; read the attribute you need here"
            + " (request.<name>) instead of passing the proxy on";

    /// Walks `o` for uses of the proxy bound to `name` (`use` null refuses
    /// every one), or, with `module` set, for `module.<proxy>` reads.
    private static void walk(Object o, Object parent, String name, Use use, String module) {
        if (o instanceof Node.FunctionDef && binds(((Node.FunctionDef) o).args, name)
                || o instanceof Node.Lambda && binds(((Node.Lambda) o).args, name)) {
            // A parameter of that name shadows the proxy.
            return;
        }
        if (module != null) {
            if (o instanceof Attribute && ((Attribute) o).value instanceof Name
                    && module.equals(((Name) ((Attribute) o).value).id) && WHAT.containsKey(((Attribute) o).attr)) {
                ((Node) o).refusal = module + "." + ((Attribute) o).attr + " cannot be lowered at build time;"
                        + " import it with from flask import " + ((Attribute) o).attr;
                return;
            }
            Infer.children(o, c -> walk(c, o, name, use, module));
            return;
        }
        if (o instanceof Attribute && isName(((Attribute) o).value, name)) {
            String a = ((Attribute) o).attr;
            if (use == null || a.startsWith("_")) {
                ((Node) o).refusal = ESCAPE;
            } else {
                use.names.add(a);
                if (parent instanceof Assign && ((Assign) parent).targets.contains(o)
                        || parent instanceof Node.AugAssign) {
                    use.stored.add(a);
                }
                if (parent instanceof Delete) {
                    use.deleted.add(a);
                }
            }
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && REFLECT.contains(((Name) ((Call) o).func).id)
                && !((Call) o).args.isEmpty() && isName(((Call) o).args.get(0), name)) {
            Call c = (Call) o;
            String f = ((Name) c.func).id;
            if (use == null || c.args.size() < 2 || !(c.args.get(1) instanceof Constant)
                    || ((Constant) c.args.get(1)).kind != Constant.Kind.STR
                    || !((Constant) c.args.get(1)).text.matches("[A-Za-z][A-Za-z0-9_]*")) {
                c.refusal = f + " of a flask proxy with a computed name is reflection; use a literal name";
                return;
            }
            String a = ((Constant) c.args.get(1)).text;
            use.names.add(a);
            if ("setattr".equals(f)) {
                use.stored.add(a);
            } else if ("delattr".equals(f)) {
                use.deleted.add(a);
            }
            for (int i = 2; i < c.args.size(); i++) {
                walk(c.args.get(i), c, name, use, null);
            }
            return;
        }
        if (o instanceof Name && name.equals(((Name) o).id)) {
            boolean ok = use != null && (parent instanceof Node.Subscript && ((Node.Subscript) parent).value == o //NOPMD CompareObjectsWithEquals - identity is meant
                    || parent instanceof Node.Compare
                    || parent instanceof Node.If || parent instanceof Node.While || parent instanceof Node.IfExp
                    || parent instanceof Node.BoolOp || parent instanceof Node.UnaryOp
                    || parent instanceof Call && ((Call) parent).func instanceof Name
                    && PROTOCOL.contains(((Name) ((Call) parent).func).id) && ((Call) parent).args.contains(o));
            if (!ok) {
                ((Node) o).refusal = ESCAPE;
            }
            return;
        }
        Infer.children(o, c -> walk(c, o, name, use, null));
    }

    private static boolean isName(Expr e, String name) {
        return e instanceof Name && name.equals(((Name) e).id);
    }

    private static boolean binds(Node.Arguments args, String name) {
        boolean[] hit = {false};
        Infer.children(args, c -> {
            if (c instanceof Node.Arg && name.equals(((Node.Arg) c).name)) {
                hit[0] = true;
            }
        });
        return hit[0];
    }
}
