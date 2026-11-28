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
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers openpyxl's `NamedStyle.__setattr__` (`styles/named_styles.py`),
/// which forwards every store and then recalculates the style when one of
/// a literal tuple of fields changed on a style bound to a workbook.
///
/// A class that binds `__setattr__` has no layout. The hook only matters
/// for the listed fields, and each of them is a descriptor whose `__set__`
/// every store of it runs, whatever the receiver's static type: so each such
/// field's descriptor is constructed from a subclass of its own descriptor
/// class (`_cn1_Hooked_Typed(Typed)`), written next to the class, whose
/// `__set__` calls the original and then the hook. The descriptors stay
/// descriptors of the same kind, so [DeclarativeMap] and
/// `Serialisable.from_tree` see what they saw before, and `__setattr__` is
/// dropped. No attribute name is compared at run time.
///
/// If `__setattr__` or any listed field has another shape, nothing is
/// changed.
public final class NamedStyleLowering {

    private static final String PATH = "openpyxl/styles/named_styles.py";

    private NamedStyleLowering() {
    }

    public static void rewrite(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(PATH)) {
                List<Stmt> body = e.getValue().body;
                for (int i = 0; i < body.size(); i++) {
                    if (body.get(i) instanceof ClassDef && "NamedStyle".equals(((ClassDef) body.get(i)).name)) {
                        lower(body, i, e.getKey());
                        return;
                    }
                }
            }
        }
    }

    private static void lower(List<Stmt> module, int at, String file) {
        ClassDef c = (ClassDef) module.get(at);
        int sa = -1;
        for (int i = 0; i < c.body.size(); i++) {
            if (c.body.get(i) instanceof FunctionDef && "__setattr__".equals(((FunctionDef) c.body.get(i)).name)) {
                if (sa >= 0) {
                    return;
                }
                sa = i;
            }
        }
        if (sa < 0) {
            return;
        }
        List<String> names = hooked((FunctionDef) c.body.get(sa));
        if (names == null) {
            return;
        }
        List<Assign> ctors = new ArrayList<Assign>();
        Set<String> kinds = new LinkedHashSet<String>();
        for (String n : names) {
            Assign k = null;
            int seen = 0;
            for (Stmt s : c.body) {
                if (s instanceof Assign && ((Assign) s).targets.size() == 1
                        && ((Assign) s).targets.get(0) instanceof Name
                        && n.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                    seen++;
                    Expr v = ((Assign) s).value;
                    if (v instanceof Call && ((Call) v).func instanceof Name) {
                        k = (Assign) s;
                    }
                } else if (s instanceof FunctionDef && n.equals(((FunctionDef) s).name)) {
                    seen++;
                }
            }
            if (k == null || seen != 1) {
                return;
            }
            ctors.add(k);
            kinds.add(((Name) ((Call) k.value).func).id);
        }
        StringBuilder b = new StringBuilder();
        for (String k : kinds) {
            b.append("class _cn1_Hooked_").append(k).append('(').append(k).append("):\n")
                    .append("    def __set__(self, instance, value):\n")
                    .append("        super().__set__(instance, value)\n")
                    .append("        if instance._wb:\n")
                    .append("            instance._recalculate()\n");
        }
        Node.Module parsed = Parser.parse(b.toString(), file);
        DataclassInit.atLine(parsed, c.line);
        for (Assign k : ctors) {
            Call old = (Call) k.value;
            Name f = new Name("_cn1_Hooked_" + ((Name) old.func).id);
            f.line = old.func.line;
            Call nc = new Call(f);
            nc.line = old.line;
            nc.args.addAll(old.args);
            nc.keywords.addAll(old.keywords);
            Assign na = new Assign(k.targets, nc);
            na.line = k.line;
            c.body.set(c.body.indexOf(k), na);
        }
        c.body.remove(sa);
        module.addAll(at, parsed.body);
    }

    /// The fields of `def __setattr__(self, attr, value):
    /// super().__setattr__(attr, value); if getattr(self, '_wb', None) and
    /// attr in (<literals>): self._recalculate()`, or null.
    private static List<String> hooked(FunctionDef f) {
        if (f.body.size() != 2 || f.args.positional.size() != 3 || !f.decorators.isEmpty()
                || !(f.body.get(1) instanceof Node.If)) {
            return null;
        }
        Node.If i = (Node.If) f.body.get(1);
        if (!i.orelse.isEmpty() || i.body.size() != 1 || !(i.test instanceof Node.BoolOp)) {
            return null;
        }
        Node.BoolOp t = (Node.BoolOp) i.test;
        if (t.values.size() != 2 || !(t.values.get(1) instanceof Node.Compare)) {
            return null;
        }
        Node.Compare cmp = (Node.Compare) t.values.get(1);
        if (cmp.comparators.size() != 1 || !(cmp.comparators.get(0) instanceof Node.TupleExpr)) {
            return null;
        }
        if (!(i.body.get(0) instanceof Node.ExprStmt)) {
            return null;
        }
        Expr call = ((Node.ExprStmt) i.body.get(0)).value;
        if (!(call instanceof Call) || !(((Call) call).func instanceof Attribute)
                || !"_recalculate".equals(((Attribute) ((Call) call).func).attr)) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        for (Expr e : ((Node.TupleExpr) cmp.comparators.get(0)).elts) {
            if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.STR) {
                return null;
            }
            out.add(((Constant) e).text);
        }
        return out;
    }
}
