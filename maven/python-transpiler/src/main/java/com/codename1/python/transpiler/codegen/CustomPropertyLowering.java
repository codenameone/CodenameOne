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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers openpyxl's custom document property (`packaging/custom.py`),
/// whose value lives in whichever one of its child elements is set, and
/// which finds that element by name: `setattr(self, k, v)` over its
/// `**kw`, `getattr(self, a)` over `__elements__`, and
/// `getattr(self, self._typ)` in `to_tree` and in
/// `CustomPropertyList.from_tree`.
///
/// The element names are closed at build time: the class body's nested
/// descriptors, sorted as openpyxl's metaclass sorts `__elements__`. This
/// pass writes, per element, a literal store in `__init__`, a literal read
/// in `type`, and a reader and writer lambda over that one element; every store of a
/// literal `_typ` also stores that element's pair in `_typ_get` and
/// `_typ_set`, so `to_tree` and `from_tree` call the pair the data selected
/// instead of comparing names. Two differences from the source, both on
/// input openpyxl never writes: an unknown keyword raises `TypeError`
/// (upstream stored an attribute no layout has), and more than one element
/// keyword raises `ValueError` (upstream kept the last in keyword order).
/// If any part of the module, or any store of `_typ` elsewhere in the
/// program, does not have the expected shape, nothing is changed.
public final class CustomPropertyLowering {

    private static final String PATH = "openpyxl/packaging/custom.py";
    private static final String CLS = "_CustomDocumentProperty";
    private static final Set<String> NESTED = new HashSet<String>(
            Arrays.asList("NestedText", "NestedDateTime", "NestedBoolText"));
    private static final Set<String> OWN = new HashSet<String>(
            Arrays.asList("_typ", "_typ_get", "_typ_set", "_type_value"));

    private CustomPropertyLowering() {
    }

    public static void rewrite(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(PATH)) {
                for (Map.Entry<String, Node.Module> o : trees.entrySet()) {
                    if (o.getValue() != e.getValue() && mentions(o.getValue())) { //NOPMD CompareObjectsWithEquals - identity
                        return;
                    }
                }
                lower(e.getValue(), e.getKey());
            }
        }
    }

    private static boolean mentions(Object o) {
        if (o instanceof Attribute && OWN.contains(((Attribute) o).attr)) {
            return true;
        }
        final boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= mentions(x));
        return hit[0];
    }

    private static ClassDef cls(Node.Module m, String n) {
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(n)) {
                return (ClassDef) s;
            }
        }
        return null;
    }

    private static int method(ClassDef c, String n) {
        int found = -1;
        for (int i = 0; i < c.body.size(); i++) {
            if (c.body.get(i) instanceof FunctionDef && ((FunctionDef) c.body.get(i)).name.equals(n)) {
                if (found >= 0) {
                    return -2;
                }
                found = i;
            }
        }
        return found;
    }

    private static String assignedName(Stmt s) {
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
            return ((Name) ((Assign) s).targets.get(0)).id;
        }
        return null;
    }

    private static void lower(Node.Module m, String file) {
        ClassDef c = cls(m, CLS);
        ClassDef list = cls(m, "CustomPropertyList");
        if (c == null || list == null || !c.decorators.isEmpty()) {
            return;
        }
        List<String> elements = new ArrayList<String>();
        int typAt = -1;
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            String n = assignedName(s);
            if (n == null) {
                continue;
            }
            Expr v = ((Assign) s).value;
            if ("_typ".equals(n) && v instanceof Constant && ((Constant) v).kind == Constant.Kind.NONE) {
                typAt = i;
            } else if (v instanceof Call && ((Call) v).func instanceof Name
                    && NESTED.contains(((Name) ((Call) v).func).id)) {
                elements.add(n);
            } else if ("__elements__".equals(n) || OWN.contains(n)) {
                return;
            }
        }
        Collections.sort(elements);
        int init = method(c, "__init__");
        int type = method(c, "type");
        int toTree = method(c, "to_tree");
        int fromTree = method(list, "from_tree");
        if (elements.isEmpty() || typAt < 0 || init < 0 || type < 0 || toTree < 0 || fromTree < 0
                || ((FunctionDef) c.body.get(init)).body.size() != 7
                || ((FunctionDef) c.body.get(type)).body.size() != 3
                || ((FunctionDef) c.body.get(toTree)).body.size() != 3) {
            return;
        }
        if (!signature((FunctionDef) c.body.get(init))) {
            return;
        }
        // CustomPropertyList.from_tree: value = getattr(prop, attr)
        Stmt getattrStmt = null;
        List<Stmt> holder = null;
        for (Stmt s : ((FunctionDef) list.body.get(fromTree)).body) {
            if (s instanceof Node.For) {
                for (Stmt t : ((Node.For) s).body) {
                    if ("value".equals(assignedName(t)) && isCall(((Assign) t).value, "getattr", "prop", "attr")) {
                        getattrStmt = t;
                        holder = ((Node.For) s).body;
                    }
                }
            }
        }
        // CustomPropertyList.to_tree: np._typ = "lpwstr"
        List<Object[]> typStores = new ArrayList<Object[]>();
        if (getattrStmt == null || !typStores(m, c, typStores)) {
            return;
        }
        for (Object[] ts : typStores) {
            if (!elements.contains((String) ts[3])) {
                return;
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("def __init__(self, name=None, pid=0, fmtid=CPROPS_FMTID, linkTarget=None, **kw):\n")
                .append("    self.fmtid = fmtid\n    self.pid = pid\n    self.name = name\n    self._typ = None\n")
                .append("    self._typ_get = None\n    self._typ_set = None\n    self.linkTarget = linkTarget\n")
                .append("    n = 0\n");
        for (String e : elements) {
            b.append("    if '").append(e).append("' in kw:\n        self.").append(e).append(" = kw['").append(e)
                    .append("']\n        n += 1\n").append(typ("self", e, "        ")).append("    else:\n        self.")
                    .append(e).append(" = None\n");
        }
        b.append("    if n != len(kw):\n        raise TypeError(\"unexpected keyword argument\")\n")
                .append("    if n > 1:\n        raise ValueError(\"a custom property holds one value\")\n");
        String initSrc = b.toString();
        b.setLength(0);
        b.append("@property\ndef type(self):\n    if self._typ is not None:\n        return self._typ\n");
        for (String e : elements) {
            b.append("    if self.").append(e).append(" is not None:\n        return '").append(e).append("'\n");
        }
        b.append("    if self.linkTarget is not None:\n        return \"linkTarget\"\n");
        String typeSrc = b.toString();
        b.setLength(0);
        b.append("def _type_value(self):\n    if self._typ is not None:\n        return self._typ_get(self)\n");
        for (String e : elements) {
            b.append("    if self.").append(e).append(" is not None:\n        return self.").append(e).append('\n');
        }
        b.append("    if self.linkTarget is not None:\n        return self.linkTarget\n")
                .append("    raise TypeError(\"attribute name must be string\")\n");
        String valueSrc = b.toString();
        String treeSrc = "def to_tree(self, tagname=None, idx=None, namespace=None):\n"
                + "    if self._typ_get is None:\n        raise TypeError(\"attribute name must be string\")\n"
                + "    child = self._typ_get(self)\n    if child is None:\n        self._typ_set(self, \"\")\n\n"
                + "    return super().to_tree(tagname=None, idx=None, namespace=None)\n";
        replace(c, init, initSrc, file);
        replace(c, type, typeSrc, file);
        replace(c, toTree, treeSrc, file);
        c.body.addAll(typAt + 1, members("_typ_get = None\n_typ_set = None\n", file, c.body.get(typAt).line));
        c.body.addAll(members(valueSrc, file, c.body.get(c.body.size() - 1).line));
        int at = holder.indexOf(getattrStmt);
        holder.set(at, members("value = prop._type_value()\n", file, getattrStmt.line).get(0));
        for (Object[] ts : typStores) {
            @SuppressWarnings("unchecked")
            List<Stmt> body = (List<Stmt>) ts[0];
            Stmt s = (Stmt) ts[1];
            String recv = (String) ts[2];
            String e = (String) ts[3];
            body.addAll(body.indexOf(s) + 1, members(typ(recv, e, "").replaceFirst("^[^\n]*\n", ""), file, s.line));
        }
    }

    /// `__init__(self, name=None, pid=0, fmtid=CPROPS_FMTID,
    /// linkTarget=None, **kw)` whose sixth and seventh statements are loops
    /// (the ones this pass replaces), exactly as the template spells it.
    private static boolean signature(FunctionDef f) {
        Node.Arguments a = f.args;
        String[] names = {"self", "name", "pid", "fmtid", "linkTarget"};
        if (a.positional.size() != names.length || a.kwarg == null || !"kw".equals(a.kwarg.name)
                || a.vararg != null || !a.kwonly.isEmpty() || a.defaults.size() != 4) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if (!names[i].equals(a.positional.get(i).name)) {
                return false;
            }
        }
        Expr d0 = a.defaults.get(0);
        Expr d1 = a.defaults.get(1);
        Expr d2 = a.defaults.get(2);
        Expr d3 = a.defaults.get(3);
        return d0 instanceof Constant && ((Constant) d0).kind == Constant.Kind.NONE
                && d1 instanceof Constant && "0".equals(((Constant) d1).text)
                && d2 instanceof Name && "CPROPS_FMTID".equals(((Name) d2).id)
                && d3 instanceof Constant && ((Constant) d3).kind == Constant.Kind.NONE
                && f.body.get(5) instanceof Node.For && f.body.get(6) instanceof Node.For;
    }

    private static String typ(String recv, String e, String ind) {
        return ind + recv + "._typ = '" + e + "'\n" + ind + recv + "._typ_get = lambda o: o." + e + "\n"
                + ind + recv + "._typ_set = lambda o, v: setattr(o, '" + e + "', v)\n";
    }

    /// Every `x._typ = <str>` in the module outside the class, which must
    /// all be `name._typ = "literal"` statements directly in a block.
    private static boolean typStores(Object o, ClassDef c, List<Object[]> out) {
        if (o == c) { //NOPMD CompareObjectsWithEquals - identity
            return true;
        }
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (x instanceof Assign && ((Assign) x).targets.size() == 1
                        && ((Assign) x).targets.get(0) instanceof Attribute
                        && "_typ".equals(((Attribute) ((Assign) x).targets.get(0)).attr)) {
                    Attribute a = (Attribute) ((Assign) x).targets.get(0);
                    Expr v = ((Assign) x).value;
                    if (!(a.value instanceof Name) || !(v instanceof Constant)
                            || ((Constant) v).kind != Constant.Kind.STR) {
                        return false;
                    }
                    out.add(new Object[] {o, x, ((Name) a.value).id, ((Constant) v).text});
                    continue;
                }
                if (!typStores(x, c, out)) {
                    return false;
                }
            }
            return true;
        }
        if (o instanceof Attribute && OWN.contains(((Attribute) o).attr)) {
            return false;
        }
        if (o instanceof Node.If) {
            return typStores(((Node.If) o).test, c, out) && typStores(((Node.If) o).body, c, out)
                    && typStores(((Node.If) o).orelse, c, out);
        }
        if (o instanceof Node.For) {
            return typStores(((Node.For) o).target, c, out) && typStores(((Node.For) o).iter, c, out)
                    && typStores(((Node.For) o).body, c, out) && typStores(((Node.For) o).orelse, c, out);
        }
        if (o instanceof FunctionDef) {
            return typStores(((FunctionDef) o).decorators, c, out) && typStores(((FunctionDef) o).args, c, out)
                    && typStores(((FunctionDef) o).body, c, out);
        }
        if (o instanceof ClassDef) {
            return typStores(((ClassDef) o).bases, c, out) && typStores(((ClassDef) o).body, c, out);
        }
        final boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= typStores(x, c, out));
        return ok[0];
    }

    private static boolean isCall(Expr v, String f, String a0, String a1) {
        if (!(v instanceof Call) || !(((Call) v).func instanceof Name) || !f.equals(((Name) ((Call) v).func).id)) {
            return false;
        }
        Call c = (Call) v;
        return c.keywords.isEmpty() && c.args.size() == 2 && c.args.get(0) instanceof Name
                && a0.equals(((Name) c.args.get(0)).id) && c.args.get(1) instanceof Name
                && a1.equals(((Name) c.args.get(1)).id);
    }

    private static List<Stmt> members(String src, String file, int line) {
        Node.Module parsed = Parser.parse("class _cp:\n" + src.replaceAll("(?m)^(?=.)", "    "), file);
        DataclassInit.atLine(parsed, line);
        return ((ClassDef) parsed.body.get(0)).body;
    }

    private static void replace(ClassDef c, int at, String src, String file) {
        List<Stmt> body = members(src, file, c.body.get(at).line);
        c.body.remove(at);
        c.body.addAll(at, body);
    }
}
