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
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Lowers every openpyxl `Alias` that [DeclarativeMap] left in place.
///
/// `x = Alias("y")` in a class body is a descriptor whose `__get__` and
/// `__set__` reach the target through `getattr`/`setattr` with the name it
/// stored: reflection the strict build refuses. The name is a literal at
/// every use, so each such entry becomes a property reading and writing
/// `self.y`. The model metaclass skips an `Alias` (it adds no field), and a
/// property is no descriptor of its, so serialisation is unchanged. When no
/// `Alias(...)` call is left anywhere in the program, the two computed
/// methods of the class are removed. A module that imports `Alias` from
/// elsewhere, or a call with anything but one identifier string, is left
/// alone (and keeps the methods alive).
public final class AliasLowering {

    private static final String BASE_PATH = "openpyxl/descriptors/base.py";

    private AliasLowering() {
    }

    /// Lowers the remaining aliases among `trees`.
    public static void rewrite(Map<String, Node.Module> trees) {
        Node.Module base = null;
        boolean left = false;
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(BASE_PATH)) {
                base = e.getValue();
            }
            boolean imported = importsAlias(e.getValue());
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    List<String> names = new ArrayList<String>();
                    left |= lower((ClassDef) s, imported, e.getKey(), names);
                    if (!names.isEmpty()) {
                        overBaseFields(trees, e.getKey(), (ClassDef) s, names);
                    }
                }
            }
        }
        if (base == null || left) {
            return;
        }
        for (Stmt s : base.body) {
            if (s instanceof ClassDef && "Alias".equals(((ClassDef) s).name)) {
                List<Stmt> keep = new ArrayList<Stmt>();
                for (Stmt m : ((ClassDef) s).body) {
                    if (!(m instanceof FunctionDef) || !("__get__".equals(((FunctionDef) m).name)
                            || "__set__".equals(((FunctionDef) m).name))) {
                        keep.add(m);
                    }
                }
                ((ClassDef) s).body.clear();
                ((ClassDef) s).body.addAll(keep);
            }
        }
    }

    /// An alias property `n` of `c` over a field a base class stores on
    /// `self` (openpyxl's `Chartsheet.HeaderFooter` over the
    /// `self.HeaderFooter = HeaderFooter()` of `_WorkbookChild.__init__`):
    /// in Python every such store on a `Chartsheet` goes through the alias.
    /// The base gets a property `n` of its own, over `self._cn1_al_n`, so its
    /// stores and reads are calls the subclass's property overrides, exactly
    /// as the data descriptor shadows the instance attribute; the base is
    /// left with no field of that name for the subclass body to rebind.
    private static void overBaseFields(Map<String, Node.Module> trees, String file, ClassDef c, List<String> names) {
        java.util.Set<ClassDef> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<ClassDef, Boolean>());
        List<Map.Entry<String, ClassDef>> work = new ArrayList<Map.Entry<String, ClassDef>>();
        work.add(new java.util.AbstractMap.SimpleEntry<String, ClassDef>(file, c));
        while (!work.isEmpty()) {
            Map.Entry<String, ClassDef> w = work.remove(work.size() - 1);
            ClassDef k = w.getValue();
            for (Node.Expr b : k.bases) {
                if (!(b instanceof Name)) {
                    continue;
                }
                Map.Entry<String, ClassDef> r = resolve(trees, w.getKey(), ((Name) b).id);
                if (r == null || !seen.add(r.getValue())) {
                    continue;
                }
                ClassDef bc = r.getValue();
                for (String n : names) {
                    if (storesOnSelf(bc, n) && !bodyBinds(bc, n)) {
                        String src = "@property\ndef " + n + "(self):\n    return self._cn1_al_" + n + "\n@" + n
                                + ".setter\ndef " + n + "(self, value):\n    self._cn1_al_" + n + " = value\n";
                        Node.Module parsed = Parser.parse("class _al:\n" + src.replaceAll("(?m)^(?=.)", "    "),
                                r.getKey());
                        DataclassInit.atLine(parsed, bc.line);
                        bc.body.addAll(((ClassDef) parsed.body.get(0)).body);
                    }
                }
                work.add(r);
            }
        }
    }

    /// The module path and class `n` names in module `file`: a class of
    /// that module, or one an absolute `from m import n` brings in.
    private static Map.Entry<String, ClassDef> resolve(Map<String, Node.Module> trees, String file, String n) {
        Node.Module m = trees.get(file);
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && n.equals(((ClassDef) s).name)) {
                return new java.util.AbstractMap.SimpleEntry<String, ClassDef>(file, (ClassDef) s);
            }
        }
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom && ((ImportFrom) s).level == 0 && ((ImportFrom) s).module != null) {
                for (Node.Alias a : ((ImportFrom) s).names) {
                    if (n.equals(a.asname != null ? a.asname : a.name)) {
                        String p = ((ImportFrom) s).module.replace('.', '/');
                        for (String f : new String[] {p + ".py", p + "/__init__.py"}) {
                            Node.Module t = trees.get(f);
                            if (t != null) {
                                for (Stmt x : t.body) {
                                    if (x instanceof ClassDef && a.name.equals(((ClassDef) x).name)) {
                                        return new java.util.AbstractMap.SimpleEntry<String, ClassDef>(f, (ClassDef) x);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean bodyBinds(ClassDef c, String n) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && n.equals(((FunctionDef) s).name)
                    || s instanceof ClassDef && n.equals(((ClassDef) s).name)) {
                return true;
            }
            if (s instanceof Assign) {
                for (Node.Expr t : ((Assign) s).targets) {
                    if (t instanceof Name && n.equals(((Name) t).id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean storesOnSelf(ClassDef c, String n) {
        boolean[] hit = {false};
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && !((FunctionDef) s).args.names().isEmpty()) {
                String self = ((FunctionDef) s).args.names().get(0);
                Infer.children(s, x -> selfStore(x, self, n, hit));
            }
        }
        return hit[0];
    }

    private static void selfStore(Object o, String self, String n, boolean[] hit) {
        if (o instanceof Assign) {
            for (Node.Expr t : ((Assign) o).targets) {
                hit[0] |= t instanceof Node.Attribute && n.equals(((Node.Attribute) t).attr)
                        && ((Node.Attribute) t).value instanceof Name
                        && self.equals(((Name) ((Node.Attribute) t).value).id);
            }
        }
        Infer.children(o, x -> selfStore(x, self, n, hit));
    }

    private static boolean importsAlias(Node.Module m) {
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                String mod = f.module == null ? "" : f.module;
                boolean ours = f.level == 0 ? "openpyxl.descriptors".equals(mod)
                        || "openpyxl.descriptors.base".equals(mod) : "base".equals(mod) || "descriptors".equals(mod);
                for (Node.Alias a : f.names) {
                    if ("Alias".equals(a.name) && a.asname == null && ours) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Lowers the aliases of `c`; true when one is left as it was.
    private static boolean lower(ClassDef c, boolean imported, String file, List<String> names) {
        boolean left = false;
        List<Stmt> body = new ArrayList<Stmt>();
        for (Stmt s : c.body) {
            if (s instanceof ClassDef) {
                left |= lower((ClassDef) s, imported, file, new ArrayList<String>());
            }
            if (!isAliasCall(s)) {
                body.add(s);
                continue;
            }
            Assign a = (Assign) s;
            Call v = (Call) a.value;
            if (!imported || a.targets.size() != 1 || !(a.targets.get(0) instanceof Name) || v.args.size() != 1
                    || !v.keywords.isEmpty() || !(v.args.get(0) instanceof Constant)
                    || ((Constant) v.args.get(0)).kind != Constant.Kind.STR
                    || !ident(((Constant) v.args.get(0)).text)) {
                left = true;
                body.add(s);
                continue;
            }
            String n = ((Name) a.targets.get(0)).id;
            String t = ((Constant) v.args.get(0)).text;
            names.add(n);
            String src = "@property\ndef " + n + "(self):\n    return self." + t + "\n@" + n + ".setter\ndef " + n
                    + "(self, value):\n    self." + t + " = value\n";
            Node.Module parsed = Parser.parse("class _al:\n" + src.replaceAll("(?m)^(?=.)", "    "), file);
            DataclassInit.atLine(parsed, s.line);
            body.addAll(((ClassDef) parsed.body.get(0)).body);
        }
        c.body.clear();
        c.body.addAll(body);
        return left;
    }

    private static boolean isAliasCall(Stmt s) {
        return s instanceof Assign && ((Assign) s).value instanceof Call
                && ((Call) ((Assign) s).value).func instanceof Name
                && "Alias".equals(((Name) ((Call) ((Assign) s).value).func).id);
    }

    private static boolean ident(String s) {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0x80 || !(Character.isLetterOrDigit(ch) || ch == '_')) {
                return false;
            }
        }
        return true;
    }
}
