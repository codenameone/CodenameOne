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
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers openpyxl's `Color.value` (`styles/colors.py`), which reads and
/// writes whichever field `self.type` names: `getattr(self, self.type)`.
///
/// Every store of a literal `self.type = 'rgb'` in `__init__` is followed by
/// a reader and a writer closure over that one field (`lambda o: o.rgb`, and
/// a `setattr` with the literal name), and `value` calls the pair. Any other
/// store of `type` -- from anywhere in the program, on a receiver of any
/// static type -- goes through the property setter [DeclarativeMap] wrote
/// for the `String()` field, so that setter first clears the pair: `value`
/// then raises `TypeError` instead of reading a field the data no longer
/// selects. No attribute name is compared at run time.
///
/// Runs after [DeclarativeMap]. If `type` was not lowered to a property
/// (a store could then bypass the hook), or anything has another shape,
/// nothing is changed.
public final class ColorValueLowering {

    private static final String PATH = "openpyxl/styles/colors.py";
    private static final String GET = "_cn1_value_get";
    private static final String SET = "_cn1_value_set";

    private ColorValueLowering() {
    }

    public static void rewrite(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(PATH)) {
                List<Stmt> body = e.getValue().body;
                for (int i = 0; i < body.size(); i++) {
                    if (body.get(i) instanceof ClassDef && "Color".equals(((ClassDef) body.get(i)).name)) {
                        lower(body, i, e.getKey());
                        return;
                    }
                }
            }
        }
    }

    private static boolean decoratedWith(FunctionDef f, String owner, String attr) {
        if (f.decorators.size() != 1) {
            return false;
        }
        Expr d = f.decorators.get(0);
        if (attr == null) {
            return d instanceof Name && ("property".equals(((Name) d).id)
                    || Infer.PROPERTY_ALIAS.equals(((Name) d).id));
        }
        return d instanceof Attribute && attr.equals(((Attribute) d).attr)
                && ((Attribute) d).value instanceof Name && owner.equals(((Name) ((Attribute) d).value).id);
    }

    private static void lower(List<Stmt> module, int at, String file) {
        ClassDef c = (ClassDef) module.get(at);
        FunctionDef init = null;
        Assign typeField = null;
        int typeBinds = 0;
        FunctionDef typeSetter = null;
        int valueGet = -1;
        int valueSet = -1;
        Set<String> fields = new HashSet<String>();
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            if (s instanceof Assign && ((Assign) s).targets.size() == 1
                    && ((Assign) s).targets.get(0) instanceof Name) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                fields.add(n);
                if ("type".equals(n)) {
                    typeBinds++;
                    Expr v = ((Assign) s).value;
                    if (v instanceof Node.Call && ((Node.Call) v).func instanceof Name) {
                        typeField = (Assign) s;
                    }
                }
            }
            if (s instanceof FunctionDef && "type".equals(((FunctionDef) s).name)) {
                typeBinds++;
            }
            if (!(s instanceof FunctionDef)) {
                continue;
            }
            FunctionDef f = (FunctionDef) s;
            if ("__init__".equals(f.name)) {
                init = f;
            } else if ("type".equals(f.name) && decoratedWith(f, "type", "setter")) {
                typeSetter = f;
            } else if ("value".equals(f.name) && decoratedWith(f, null, null)) {
                valueGet = i;
            } else if ("value".equals(f.name) && decoratedWith(f, "value", "setter")) {
                valueSet = i;
            } else if (decoratedWith(f, null, null)) {
                fields.add(f.name);
            }
        }
        // The one binding of `type` in the class: a lowered property (its
        // setter) or a descriptor construction (its class's __set__).
        boolean viaSetter = typeSetter != null && typeField == null && typeBinds == 2;
        boolean viaDescriptor = typeSetter == null && typeField != null && typeBinds == 1;
        if (init == null || !viaSetter && !viaDescriptor || valueGet < 0 || valueSet < 0 || mentions(c)) {
            return;
        }
        List<Object[]> stores = new ArrayList<Object[]>();
        if (!stores(init.body, fields, stores) || stores.isEmpty()) {
            return;
        }
        if (viaSetter) {
            typeSetter.body.addAll(0, ((FunctionDef) members("def _f(self):\n    self." + GET + " = None\n    self."
                    + SET + " = None\n", file, typeSetter.line).get(0)).body);
        } else {
            Node.Call old = (Node.Call) typeField.value;
            String kind = ((Name) old.func).id;
            String hooked = "_cn1_ColorType_" + kind;
            Node.Module parsed = Parser.parse("class " + hooked + "(" + kind + "):\n"
                    + "    def __set__(self, instance, value):\n"
                    + "        instance." + GET + " = None\n        instance." + SET + " = None\n"
                    + "        super().__set__(instance, value)\n", file);
            DataclassInit.atLine(parsed, c.line);
            Name f = new Name(hooked);
            f.line = old.func.line;
            Node.Call nc = new Node.Call(f);
            nc.line = old.line;
            nc.args.addAll(old.args);
            nc.keywords.addAll(old.keywords);
            Assign na = new Assign(typeField.targets, nc);
            na.line = typeField.line;
            c.body.set(c.body.indexOf(typeField), na);
            module.addAll(at, parsed.body);
        }
        for (Object[] st : stores) {
            @SuppressWarnings("unchecked")
            List<Stmt> body = (List<Stmt>) st[0];
            Stmt s = (Stmt) st[1];
            String f = (String) st[2];
            List<Stmt> pair = ((FunctionDef) members("def _f(self):\n    self." + GET + " = lambda o: o." + f
                    + "\n    self." + SET + " = lambda o, v: setattr(o, '" + f + "', v)\n", file, s.line)
                    .get(0)).body;
            body.addAll(body.indexOf(s) + 1, pair);
        }
        String getSrc = "@property\ndef value(self):\n    g = self." + GET + "\n    if g is None:\n"
                + "        raise TypeError(\"Color.type does not name one of its value fields\")\n"
                + "    return g(self)\n";
        String setSrc = "@value.setter\ndef value(self, value):\n    s = self." + SET + "\n    if s is None:\n"
                + "        raise TypeError(\"Color.type does not name one of its value fields\")\n"
                + "    s(self, value)\n";
        int line = c.body.get(valueGet).line;
        c.body.set(valueGet, members(getSrc, file, line).get(0));
        c.body.set(valueSet, members(setSrc, file, c.body.get(valueSet).line).get(0));
        c.body.addAll(members(GET + " = None\n" + SET + " = None\n", file, c.body.get(c.body.size() - 1).line));
    }

    /// Whether the class already mentions the generated names.
    private static boolean mentions(Object o) {
        if (o instanceof Attribute && (GET.equals(((Attribute) o).attr) || SET.equals(((Attribute) o).attr))) {
            return true;
        }
        final boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= mentions(x));
        return hit[0];
    }

    /// Every `self.type = ...` in `body` (into nested ifs), which must all
    /// be literal names of the class's lowered fields.
    private static boolean stores(List<Stmt> body, Set<String> fields, List<Object[]> out) {
        for (Stmt s : body) {
            if (s instanceof Node.If) {
                if (!stores(((Node.If) s).body, fields, out) || !stores(((Node.If) s).orelse, fields, out)) {
                    return false;
                }
                continue;
            }
            if (s instanceof Assign && ((Assign) s).targets.size() == 1
                    && ((Assign) s).targets.get(0) instanceof Attribute
                    && "type".equals(((Attribute) ((Assign) s).targets.get(0)).attr)) {
                Attribute a = (Attribute) ((Assign) s).targets.get(0);
                Expr v = ((Assign) s).value;
                if (!(a.value instanceof Name) || !"self".equals(((Name) a.value).id) || !(v instanceof Constant)
                        || ((Constant) v).kind != Constant.Kind.STR || !fields.contains(((Constant) v).text)) {
                    return false;
                }
                out.add(new Object[] {body, s, ((Constant) v).text});
                continue;
            }
            if (typeStore(s)) {
                return false;
            }
        }
        return true;
    }

    private static boolean typeStore(Object o) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (t instanceof Attribute && "type".equals(((Attribute) t).attr)) {
                    return true;
                }
            }
        }
        final boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= typeStore(x));
        return hit[0];
    }

    private static List<Stmt> members(String src, String file, int line) {
        Node.Module parsed = Parser.parse("class _cv:\n" + src.replaceAll("(?m)^(?=.)", "    "), file);
        DataclassInit.atLine(parsed, line);
        return ((ClassDef) parsed.body.get(0)).body;
    }
}
