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
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Evaluates openpyxl's style descriptors at build time.
///
/// `openpyxl/styles/styleable.py` declares the style fields of every cell
/// as class attributes of `StyleableObject` holding descriptor objects:
///
/// ```python
/// class StyleableObject:
///     font = StyleDescriptor('_fonts', "fontId")
///     quotePrefix = StyleArrayDescriptor('quotePrefix')
/// ```
///
/// whose `__get__` and `__set__` reach the workbook collection and the
/// style array slot through `getattr`/`setattr` with the names stored in
/// the descriptor: reflection the strict build refuses, on the hottest
/// path of a spreadsheet read. Every argument is a literal, so this pass
/// writes each field as a property whose getter and setter are the
/// descriptor's methods with those names spelled out
/// (`self.parent.parent._fonts`, `self._style.fontId`), and empties the
/// four descriptor classes, which nothing else uses. The upstream source is
/// not edited. If the module does not have exactly the shape expected, it
/// is left as it is.
public final class StyleableLowering {

    private static final String PATH = "openpyxl/styles/styleable.py";
    private static final String[] DESCRIPTORS = {"StyleDescriptor", "NumberFormatDescriptor",
        "NamedStyleDescriptor", "StyleArrayDescriptor"};
    private static final String INIT = "    if not self._style:\n        self._style = StyleArray()\n";

    private StyleableLowering() {
    }

    /// Lowers the style descriptors of `StyleableObject` among `trees`.
    public static void rewrite(Map<String, Node.Module> trees) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(PATH)) {
                lower(e.getValue(), e.getKey());
            }
        }
    }

    private static void lower(Node.Module m, String file) {
        Map<String, ClassDef> classes = new HashMap<String, ClassDef>();
        for (Stmt s : m.body) {
            if (s instanceof ClassDef) {
                classes.put(((ClassDef) s).name, (ClassDef) s);
            }
        }
        ClassDef owner = classes.get("StyleableObject");
        if (owner == null) {
            return;
        }
        for (String d : DESCRIPTORS) {
            if (!classes.containsKey(d)) {
                return;
            }
        }
        List<Stmt> body = new ArrayList<Stmt>();
        for (Stmt s : owner.body) {
            String src = s instanceof Assign ? property((Assign) s) : null;
            if (src == null && s instanceof Assign && ((Assign) s).value instanceof Call
                    && ((Call) ((Assign) s).value).func instanceof Name
                    && isDescriptor(((Name) ((Call) ((Assign) s).value).func).id)) {
                // A descriptor this pass cannot spell out: leave it all.
                return;
            }
            if (src == null) {
                body.add(s);
                continue;
            }
            Node.Module parsed = Parser.parse("class _st:\n" + src.replaceAll("(?m)^(?=.)", "    "), file);
            DataclassInit.atLine(parsed, s.line);
            body.addAll(((ClassDef) parsed.body.get(0)).body);
        }
        owner.body.clear();
        owner.body.addAll(body);
        for (String d : DESCRIPTORS) {
            ClassDef c = classes.get(d);
            Node.Module p = Parser.parse("pass\n", file);
            DataclassInit.atLine(p, c.line);
            c.body.clear();
            c.body.addAll(p.body);
        }
    }

    private static boolean isDescriptor(String n) {
        for (String d : DESCRIPTORS) {
            if (d.equals(n)) {
                return true;
            }
        }
        return false;
    }

    /// The property source replacing `s`, or null when it is not a
    /// descriptor field with literal arguments.
    private static String property(Assign s) {
        if (s.targets.size() != 1 || !(s.targets.get(0) instanceof Name) || !(s.value instanceof Call)) {
            return null;
        }
        Call c = (Call) s.value;
        if (!(c.func instanceof Name) || !c.keywords.isEmpty()) {
            return null;
        }
        List<String> args = new ArrayList<String>();
        for (Expr a : c.args) {
            if (!(a instanceof Constant) || ((Constant) a).kind != Constant.Kind.STR || !ident(((Constant) a).text)) {
                return null;
            }
            args.add(((Constant) a).text);
        }
        String n = ((Name) s.targets.get(0)).id;
        String kind = ((Name) c.func).id;
        String head = "@property\ndef " + n + "(self):\n";
        String set = "@" + n + ".setter\ndef " + n + "(self, value):\n";
        if ("StyleDescriptor".equals(kind) && args.size() == 2) {
            String coll = "    coll = self.parent.parent." + args.get(0) + "\n";
            return head + coll + INIT + "    idx = self._style." + args.get(1) + "\n    return StyleProxy(coll[idx])\n"
                    + set + coll + INIT + "    self._style." + args.get(1) + " = coll.add(value)\n";
        }
        if ("StyleArrayDescriptor".equals(kind) && args.size() == 1) {
            return head + "    if self._style is None:\n        return False\n    return bool(self._style."
                    + args.get(0) + ")\n" + set + "    if self._style is None:\n        self._style = StyleArray()\n"
                    + "    self._style." + args.get(0) + " = value\n";
        }
        if ("NumberFormatDescriptor".equals(kind) && args.isEmpty()) {
            return head + INIT + "    idx = self._style.numFmtId\n    if idx < BUILTIN_FORMATS_MAX_SIZE:\n"
                    + "        return BUILTIN_FORMATS.get(idx, \"General\")\n"
                    + "    coll = self.parent.parent._number_formats\n    return coll[idx - BUILTIN_FORMATS_MAX_SIZE]\n"
                    + set + "    coll = self.parent.parent._number_formats\n    if value in BUILTIN_FORMATS_REVERSE:\n"
                    + "        idx = BUILTIN_FORMATS_REVERSE[value]\n    else:\n"
                    + "        idx = coll.add(value) + BUILTIN_FORMATS_MAX_SIZE\n" + INIT
                    + "    self._style.numFmtId = idx\n";
        }
        if ("NamedStyleDescriptor".equals(kind) && args.isEmpty()) {
            return head + INIT + "    idx = self._style.xfId\n    coll = self.parent.parent._named_styles\n"
                    + "    return coll.names[idx]\n"
                    + set + INIT + "    coll = self.parent.parent._named_styles\n    if isinstance(value, NamedStyle):\n"
                    + "        style = value\n        if style not in coll:\n"
                    + "            self.parent.parent.add_named_style(style)\n    elif value not in coll.names:\n"
                    + "        if value in styles:\n            style = styles[value]\n            if style not in coll:\n"
                    + "                self.parent.parent.add_named_style(style)\n        else:\n"
                    + "            raise ValueError(\"{0} is not a known style\".format(value))\n    else:\n"
                    + "        style = coll[value]\n    self._style = copy(style.as_tuple())\n";
        }
        return null;
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
