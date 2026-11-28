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
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers cobble's class decorators at build time.
///
/// `@cobble.data` finds the class's `cobble.field()` attributes and writes
/// `__init__`, `__repr__`, `__eq__`, `__ne__` and `__hash__` over them, and
/// `@cobble.visitable` (which `data` implies) gives the class the
/// `_accept0`/`_accept1` methods a `cobble.visitor` dispatches through. The
/// real package makes them all at run time from the field names, with
/// computed `getattr`/`setattr`; here every name is static, the methods are
/// ordinary members of the class body, the decorator is dropped and the class
/// keeps a layout. `cobble.copy` calls the generated `_cobble_copy`, which
/// takes one keyword per field and reads every field not passed off `self`.
///
/// A class this cannot read exactly -- a base defined in another module, a
/// field call with arguments other than `default=None`, a required field
/// after an optional one -- keeps its decorator, so strict mode still refuses
/// it rather than compiling a guess.
public final class CobbleLowering {
    private static final String UNDEFINED = "_cn1_cobble_undefined";

    private CobbleLowering() {
    }

    /// Rewrites the cobble classes of `tree`, the module at path `path`.
    public static void rewrite(Node.Module tree, String path) {
        Set<String> modules = new HashSet<String>();
        Set<String> dataNames = new HashSet<String>();
        Set<String> visitableNames = new HashSet<String>();
        Set<String> fieldNames = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("cobble".equals(a.name)) {
                        modules.add(a.asname != null ? a.asname : a.name);
                    }
                }
            } else if (s instanceof ImportFrom && "cobble".equals(((ImportFrom) s).module)
                    && ((ImportFrom) s).level == 0) {
                for (Alias a : ((ImportFrom) s).names) {
                    String bound = a.asname != null ? a.asname : a.name;
                    if ("data".equals(a.name)) {
                        dataNames.add(bound);
                    } else if ("visitable".equals(a.name)) {
                        visitableNames.add(bound);
                    } else if ("field".equals(a.name)) {
                        fieldNames.add(bound);
                    }
                }
            }
        }
        if (modules.isEmpty() && dataNames.isEmpty() && visitableNames.isEmpty()) {
            return;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        List<Set<String>> all = new ArrayList<Set<String>>();
        all.add(modules);
        all.add(dataNames);
        all.add(visitableNames);
        all.add(fieldNames);
        for (Set<String> names : all) {
            for (String n : names) {
                if (bindings.getOrDefault(n, 0) != 1) {
                    return;
                }
            }
        }
        Map<String, ClassDef> classes = new HashMap<String, ClassDef>();
        for (Stmt s : tree.body) {
            if (s instanceof ClassDef) {
                classes.put(((ClassDef) s).name, (ClassDef) s);
            }
        }
        if (!modules.isEmpty()) {
            visitorBases(tree, path, modules.iterator().next());
        }
        Map<String, List<String[]>> fieldsOf = new HashMap<String, List<String[]>>();
        Stmt marker = null;
        for (Stmt s : tree.body) {
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            if (c.decorators.size() != 1 || !c.keywords.isEmpty()) {
                continue;
            }
            Expr d = c.decorators.get(0);
            boolean data = is(d, "data", dataNames, modules);
            boolean visitable = is(d, "visitable", visitableNames, modules);
            if (!data && !visitable) {
                continue;
            }
            List<String[]> fields = null;
            if (data) {
                fields = fields(c, classes, fieldsOf, fieldNames, modules, new HashSet<String>());
                if (fields == null || !trailingDefaults(fields)) {
                    continue;
                }
            }
            lower(c, path, data, fields, fieldNames, modules);
            if (data && marker == null) {
                marker = (Stmt) Parser.parse("from cobble import _undefined as " + UNDEFINED + "\n", path).body.get(0);
                DataclassInit.atLine(marker, c.line);
            }
        }
        if (marker != null) {
            int i = 0;
            while (i < tree.body.size() && (tree.body.get(i) instanceof ImportFrom
                    && "__future__".equals(((ImportFrom) tree.body.get(i)).module)
                    || i == 0 && tree.body.get(i) instanceof Node.ExprStmt)) {
                i++;
            }
            tree.body.add(i, marker);
        }
    }

    /// `X = cobble.visitor(Base, ...)` at module level becomes
    /// `class X(cobble._Visitor): pass`, and a class base written as that
    /// call becomes `cobble._Visitor`: every visitor is the one runtime class,
    /// so a class built on one has a base with a layout instead of a call
    /// result.
    private static void visitorBases(Node.Module tree, String path, String module) {
        Set<String> none = new HashSet<String>();
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (s instanceof Assign && ((Assign) s).targets.size() == 1
                    && ((Assign) s).targets.get(0) instanceof Name && ((Assign) s).value instanceof Call
                    && is(((Call) ((Assign) s).value).func, "visitor", none, java.util.Collections.singleton(module))) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                Stmt c = Parser.parse("class " + n + "(" + module + "._Visitor):\n    pass\n", path).body.get(0);
                DataclassInit.atLine(c, s.line);
                tree.body.set(i, c);
            } else if (s instanceof ClassDef) {
                List<Expr> bases = ((ClassDef) s).bases;
                for (int b = 0; b < bases.size(); b++) {
                    if (bases.get(b) instanceof Call
                            && is(((Call) bases.get(b)).func, "visitor", none, java.util.Collections.singleton(module))) {
                        Expr e = ((Assign) Parser.parse("x = " + module + "._Visitor\n", path).body.get(0)).value;
                        DataclassInit.atLine(e, s.line);
                        bases.set(b, e);
                    }
                }
            }
        }
    }

    private static boolean is(Expr e, String attr, Set<String> names, Set<String> modules) {
        return e instanceof Name && names.contains(((Name) e).id)
                || e instanceof Attribute && attr.equals(((Attribute) e).attr)
                && ((Attribute) e).value instanceof Name && modules.contains(((Name) ((Attribute) e).value).id);
    }

    /// The `{name, "1" when it defaults to None}` pairs of `c` in cobble's
    /// order: inherited fields (from bases defined earlier) first, then the
    /// class's own in source order. Null when they cannot be known.
    private static List<String[]> fields(ClassDef c, Map<String, ClassDef> classes,
            Map<String, List<String[]>> cache, Set<String> fieldNames, Set<String> modules, Set<String> seen) {
        List<String[]> known = cache.get(c.name);
        if (known != null) {
            return known;
        }
        if (!seen.add(c.name)) {
            return null;
        }
        List<String[]> out = new ArrayList<String[]>();
        Set<String> names = new HashSet<String>();
        for (Expr b : c.bases) {
            if (b instanceof Name && "object".equals(((Name) b).id)) {
                continue;
            }
            if (!(b instanceof Name) || !classes.containsKey(((Name) b).id)) {
                return null;
            }
            List<String[]> inherited = fields(classes.get(((Name) b).id), classes, cache, fieldNames, modules, seen);
            if (inherited == null) {
                return null;
            }
            for (String[] f : inherited) {
                if (names.add(f[0])) {
                    out.add(f);
                }
            }
        }
        for (Stmt s : c.body) {
            if (!(s instanceof Assign)) {
                continue;
            }
            Assign a = (Assign) s;
            if (!(a.value instanceof Call) || !is(((Call) a.value).func, "field", fieldNames, modules)) {
                continue;
            }
            Call call = (Call) a.value;
            if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Name) || !call.args.isEmpty()) {
                return null;
            }
            boolean optional = false;
            for (Keyword k : call.keywords) {
                if (!"default".equals(k.arg) || !(k.value instanceof Constant)
                        || ((Constant) k.value).kind != Constant.Kind.NONE) {
                    return null;
                }
                optional = true;
            }
            String n = ((Name) a.targets.get(0)).id;
            if (!names.add(n)) {
                return null;
            }
            out.add(new String[] {n, optional ? "1" : null});
        }
        cache.put(c.name, out);
        return out;
    }

    private static boolean trailingDefaults(List<String[]> fields) {
        boolean optional = false;
        for (String[] f : fields) {
            if (f[1] != null) {
                optional = true;
            } else if (optional) {
                return false;
            }
        }
        return true;
    }

    private static void lower(ClassDef c, String path, boolean data, List<String[]> fields,
            Set<String> fieldNames, Set<String> modules) {
        Set<String> defined = new HashSet<String>();
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef) {
                defined.add(((FunctionDef) s).name);
            }
        }
        StringBuilder src = new StringBuilder();
        if (data) {
            List<Stmt> keep = new ArrayList<Stmt>();
            for (Stmt s : c.body) {
                if (s instanceof Assign && ((Assign) s).value instanceof Call
                        && is(((Call) ((Assign) s).value).func, "field", fieldNames, modules)) {
                    continue;
                }
                keep.add(s);
            }
            c.body.clear();
            c.body.addAll(keep);
            src.append("def __init__(self");
            for (String[] f : fields) {
                src.append(", ").append(f[0]).append(f[1] != null ? "=None" : "");
            }
            src.append("):\n");
            for (String[] f : fields) {
                src.append("    self.").append(f[0]).append(" = ").append(f[0]).append('\n');
            }
            if (fields.isEmpty()) {
                src.append("    pass\n");
            }
            src.append("def __repr__(self):\n    return \"").append(c.name).append("(\"");
            for (int i = 0; i < fields.size(); i++) {
                String n = fields.get(i)[0];
                src.append(" + \"").append(i == 0 ? "" : ", ").append(n).append("=\" + repr(self.").append(n)
                        .append(')');
            }
            src.append(" + \")\"\n");
            src.append("def __eq__(self, other):\n    if not isinstance(other, ").append(c.name)
                    .append("):\n        return False\n");
            for (String[] f : fields) {
                src.append("    if not (self.").append(f[0]).append(" == other.").append(f[0])
                        .append("):\n        return False\n");
            }
            src.append("    return True\n");
            src.append("def __ne__(self, other):\n    return not (self == other)\n");
            src.append("def __hash__(self):\n    return hash((").append(c.name);
            for (String[] f : fields) {
                src.append(", self.").append(f[0]);
            }
            src.append("))\n");
            // One keyword per field, defaulting to a marker: a field not
            // passed is read straight off self, with no dict of the
            // attributes built, updated and splatted back.
            src.append("def _cobble_copy(self");
            if (!fields.isEmpty()) {
                src.append(", *");
            }
            for (String[] f : fields) {
                src.append(", ").append(f[0]).append('=').append(UNDEFINED);
            }
            src.append("):\n    return type(self)(");
            for (int i = 0; i < fields.size(); i++) {
                String n = fields.get(i)[0];
                src.append(i == 0 ? "" : ", ").append(n).append("=self.").append(n).append(" if ").append(n)
                        .append(" is ").append(UNDEFINED).append(" else ").append(n);
            }
            src.append(")\n");
        }
        String visit = "visit_" + underscore(c.name);
        if (!defined.contains("_accept0")) {
            src.append("def _accept0(self, visitor):\n    return visitor.").append(visit).append("(self)\n");
        }
        if (!defined.contains("_accept1")) {
            src.append("def _accept1(self, visitor, arg0):\n    return visitor.").append(visit)
                    .append("(self, arg0)\n");
        }
        int line = c.body.isEmpty() ? c.line : c.body.get(c.body.size() - 1).line;
        for (Stmt s : Parser.parse(src.toString(), path).body) {
            DataclassInit.atLine(s, line);
            c.body.add(s);
        }
        c.decorators.clear();
    }

    /// cobble's `_underscore`: CamelCase to snake_case for visitor names.
    static String underscore(String word) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                if (i > 0) {
                    char prev = word.charAt(i - 1);
                    boolean prevLower = prev >= 'a' && prev <= 'z';
                    boolean prevDigit = prev >= '0' && prev <= '9';
                    boolean prevUpper = prev >= 'A' && prev <= 'Z';
                    boolean nextLower = i + 1 < word.length() && word.charAt(i + 1) >= 'a'
                            && word.charAt(i + 1) <= 'z';
                    if (prevLower || prevDigit || nextLower && prevUpper) {
                        out.append('_');
                    }
                }
                out.append((char) (ch - 'A' + 'a'));
            } else if (ch == '-') {
                out.append('_');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
