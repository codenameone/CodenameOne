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

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Arg;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Return;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/// What a build is told about functions of its Python sources, its own or
/// any package's, in `cn1-python.properties` beside the sources:
///
/// ```
/// # reached in a way the build cannot see: compile it anyway
/// keep = pkg.plugins.load_all, pkg.models.Model.from_row
/// # never needed here: leave it out, and raise if it is called after all
/// remove = pkg.cli.run_python
/// # needed, and it cannot be translated: its body is Java
/// replace = pkg.utils.compile_code
/// ```
///
/// A name is the module's dotted name followed by the function's, or by a
/// class's and a method's. `remove` and `replace` are how code that cannot
/// run in a translated program (`eval`, `exec`) stops failing the build when
/// the program does reach the function it is in: the function's body is not
/// compiled at all. A replaced function calls the Java registered for it
/// with `NativeModule.replace`; the build writes a class with a stub for
/// each, per module, for the developer to fill in (see [#shims]).
public final class Directives {
    /// The file, in the root of the Python sources.
    public static final String FILE = "cn1-python.properties";
    /// The package of the generated classes that hold the replacements.
    public static final String SHIM_PACKAGE = "com.codename1.python.ext";

    private final Set<String> keep = new TreeSet<String>();
    private final Set<String> remove = new TreeSet<String>();
    private final Set<String> replace = new TreeSet<String>();
    /// Module to its replaced functions, each with the comment that
    /// describes its parameters.
    private final Map<String, Map<String, String>> replaced = new TreeMap<String, Map<String, String>>();

    /// Reads the file's text; null or empty gives no directive.
    public static Directives parse(String text) {
        Directives d = new Directives();
        if (text == null) {
            return d;
        }
        int n = 0;
        for (String raw : text.split("\n")) {
            n++;
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            int eq = line.indexOf('=');
            String key = eq < 0 ? line : line.substring(0, eq).trim();
            Set<String> into = "keep".equals(key) ? d.keep : "remove".equals(key) ? d.remove
                    : "replace".equals(key) ? d.replace : null;
            if (eq < 0 || into == null) {
                throw new PythonSyntaxError(FILE, n, 1, "expected keep=, remove= or replace= followed by the dotted"
                        + " names of functions (module.function or module.Class.method), separated by commas");
            }
            for (String name : line.substring(eq + 1).split(",")) {
                if (!name.trim().isEmpty()) {
                    into.add(name.trim());
                }
            }
        }
        return d;
    }

    /// The functions to compile although nothing is seen to reach them.
    public Set<String> keep() {
        return keep;
    }

    /// Whether the file names `function` under `remove` or `replace`.
    boolean names(String function) {
        return remove.contains(function) || replace.contains(function);
    }

    /// Rewrites the body of every removed and replaced function in
    /// `modules` (dotted module name to tree), before anything reads them.
    /// A name that is no function of the program is an error: a directive
    /// that silently does nothing would leave the code it was written for
    /// in the build.
    public void apply(Map<String, Node.Module> modules) {
        for (String name : remove) {
            for (FunctionDef f : resolve(name, modules)) {
                Call c = at(new Call(at(new Name("__cn1_removed__"), f)), f);
                c.args.add(at(new Constant(Constant.Kind.STR, name + " was left out of this build (remove= in "
                        + FILE + ")"), f));
                f.body.clear();
                f.body.add(at(new ExprStmt(c), f));
            }
        }
        for (String name : replace) {
            List<FunctionDef> fs = resolve(name, modules);
            String module = moduleOf(name, modules);
            for (FunctionDef f : fs) {
                Call c = at(new Call(at(new Name("__cn1_replaced__"), f)), f);
                c.args.add(at(new Constant(Constant.Kind.STR, name), f));
                StringBuilder sig = new StringBuilder();
                for (Arg a : f.args.positional) {
                    param(c, sig, a.name, a.name, f);
                }
                if (f.args.vararg != null) {
                    param(c, sig, f.args.vararg.name, f.args.vararg.name + " (a tuple)", f);
                }
                for (Arg a : f.args.kwonly) {
                    param(c, sig, a.name, a.name, f);
                }
                if (f.args.kwarg != null) {
                    param(c, sig, f.args.kwarg.name, f.args.kwarg.name + " (a dict)", f);
                }
                f.body.clear();
                f.body.add(at(new Return(c), f));
                Map<String, String> m = replaced.get(module);
                if (m == null) {
                    m = new TreeMap<String, String>();
                    replaced.put(module, m);
                }
                m.put(name, sig.toString());
            }
        }
    }

    private static void param(Call c, StringBuilder sig, String name, String shown, FunctionDef f) {
        c.args.add(at(new Name(name), f));
        sig.append(sig.length() == 0 ? "" : ", ").append(shown);
    }

    private static <T extends Node> T at(T n, FunctionDef f) {
        n.line = f.line;
        n.col = f.col;
        return n;
    }

    private static String moduleOf(String name, Map<String, Node.Module> modules) {
        for (int dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.', dot - 1)) {
            if (modules.containsKey(name.substring(0, dot))) {
                return name.substring(0, dot);
            }
        }
        return null;
    }

    private static List<FunctionDef> resolve(String name, Map<String, Node.Module> modules) {
        String module = moduleOf(name, modules);
        List<Object> in = new ArrayList<Object>();
        if (module != null) {
            in.add(modules.get(module).body);
            String[] path = name.substring(module.length() + 1).split("\\.");
            for (int i = 0; i < path.length; i++) {
                List<Object> next = new ArrayList<Object>();
                for (Object o : in) {
                    find(o, path[i], i == path.length - 1, next);
                }
                in = next;
            }
        }
        List<FunctionDef> r = new ArrayList<FunctionDef>();
        for (Object o : in) {
            if (o instanceof FunctionDef) {
                r.add((FunctionDef) o);
            }
        }
        if (r.isEmpty()) {
            throw new PythonSyntaxError(FILE, 1, 1, name + " is not a function of this program: a name is the"
                    + " module's dotted name followed by the function's (pkg.module.function) or by a class's and"
                    + " a method's (pkg.module.Class.method)");
        }
        return r;
    }

    /// Every definition of `name` that runs as part of `in`: inside `if`,
    /// `try` and `with` too, where a module picks one of two versions.
    private static void find(Object in, String name, boolean function, List<Object> out) {
        Infer.children(in, x -> {
            if (x instanceof FunctionDef) {
                if (function && ((FunctionDef) x).name.equals(name)) {
                    out.add(x);
                }
            } else if (x instanceof ClassDef) {
                if (!function && ((ClassDef) x).name.equals(name)) {
                    out.add(((ClassDef) x).body);
                }
            } else if (!(x instanceof Expr)) {
                find(x, name, function, out);
            }
        });
    }

    /// The class that holds the replacements of `module`'s functions:
    /// `pkg.sub.utils` is `PkgSubUtilsShims`.
    public static String shimClass(String module) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < module.length(); i++) {
            char c = module.charAt(i);
            boolean letter = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
            if (!letter) {
                up = true;
            } else if (up) {
                sb.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.append("Shims").toString();
    }

    /// The Java source of each class of replacements, by its path under a
    /// source root: a stub per replaced function, raising until it is
    /// filled in. The file is the developer's once written; a build only
    /// writes one that does not exist.
    public Map<String, String> shims() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Map<String, String>> e : replaced.entrySet()) {
            String cls = shimClass(e.getKey());
            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(SHIM_PACKAGE).append(";\n\n");
            sb.append("import com.codename1.python.runtime.NativeModule;\n\n");
            sb.append("/// The functions of `").append(e.getKey()).append("` this build replaces with Java\n");
            sb.append("/// (`replace=` in `").append(FILE).append("`).\n");
            sb.append("///\n");
            sb.append("/// Generated with a stub for each. A stub receives the values of the function's\n");
            sb.append("/// parameters, in the order listed above it, as the Python runtime holds them, and\n");
            sb.append("/// returns what the call answers (`com.codename1.python.runtime.Py.NONE` for\n");
            sb.append("/// `None`). This file is yours: a build does not overwrite it.\n");
            sb.append("public final class ").append(cls).append(" {\n");
            sb.append("    private ").append(cls).append("() {\n    }\n\n");
            sb.append("    /// Registers the replacements; the generated program calls this at start.\n");
            sb.append("    public static void install() {\n");
            for (Map.Entry<String, String> f : e.getValue().entrySet()) {
                sb.append("        // args: ").append(f.getValue().isEmpty() ? "none" : f.getValue()).append('\n');
                sb.append("        NativeModule.replace(\"").append(f.getKey()).append("\", args -> {\n");
                sb.append("            throw NativeModule.notReplaced(\"").append(f.getKey()).append("\");\n");
                sb.append("        });\n");
            }
            sb.append("    }\n}\n");
            out.put(SHIM_PACKAGE.replace('.', '/') + "/" + cls + ".java", sb.toString());
        }
        return out;
    }

    /// The classes of [#shims], by qualified name, for the program to
    /// install at start.
    public List<String> shimClasses() {
        List<String> r = new ArrayList<String>();
        for (String module : replaced.keySet()) {
            r.add(SHIM_PACKAGE + "." + shimClass(module));
        }
        return r;
    }
}
