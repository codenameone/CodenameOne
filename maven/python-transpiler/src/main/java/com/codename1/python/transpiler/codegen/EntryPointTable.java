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

import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Writes `_cn1_entry_points`, the module `EntryPoint.load` resolves an
/// entry point through: for every group the bundled distributions' metadata
/// declares, a function that imports each of its targets by a literal
/// import, and one statement `GROUPS["group"] = _gN` registering it. A
/// target is reached by name only through that table, so loading is plain
/// imports and attribute reads, never a computed `getattr`.
///
/// The registrations are not kept for their effect (Liveness): the one of a
/// group is kept only once live code spells that group's name, so a program
/// that loads its `llm` plugins does not keep every console script.
public final class EntryPointTable {
    /// The module's name, and the file it is compiled from.
    public static final String MODULE = "_cn1_entry_points";
    public static final String PATH = MODULE + ".py";
    /// The table's name in the module.
    public static final String TABLE = "GROUPS";

    private EntryPointTable() {
    }

    /// The module's source for the `entry_points.txt` texts in
    /// `distributions` (the parsed `_cn1_distributions`), naming only
    /// targets whose module is one of `modules`.
    public static String source(Object distributions, final Set<String> modules) {
        final Map<String, List<String>> groups = new LinkedHashMap<String, List<String>>();
        Infer.children(distributions, new Infer.Visit() {
            @Override
            public void on(Object o) {
                if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
                    read(((Constant) o).text, groups, modules);
                } else {
                    Infer.children(o, this);
                }
            }
        });
        StringBuilder b = new StringBuilder();
        b.append("\"\"\"Entry point targets by group, generated for importlib.metadata.\"\"\"\n");
        b.append(TABLE).append(" = {}\n");
        int n = 0;
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            String fn = "_g" + n++;
            b.append("\n\ndef ").append(fn).append("(target):\n");
            int i = 0;
            for (String t : e.getValue()) {
                int colon = t.indexOf(':');
                String mod = colon < 0 ? t : t.substring(0, colon);
                b.append("    if target == ").append(literal(t)).append(":\n");
                if (colon < 0) {
                    b.append("        import ").append(mod).append(" as _m").append(i).append('\n');
                    b.append("        return _m").append(i).append('\n');
                } else {
                    String attr = t.substring(colon + 1);
                    int dot = attr.indexOf('.');
                    String first = dot < 0 ? attr : attr.substring(0, dot);
                    b.append("        from ").append(mod).append(" import ").append(first).append(" as _a")
                            .append(i).append('\n');
                    b.append("        return _a").append(i).append(dot < 0 ? "" : attr.substring(dot)).append('\n');
                }
                i++;
            }
            b.append("    return None\n\n\n");
            b.append(TABLE).append('[').append(literal(e.getKey())).append("] = ").append(fn).append('\n');
        }
        return b.toString();
    }

    /// The group a statement of the module registers, or null when it is
    /// not a registration.
    public static String registers(Stmt s) {
        if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1
                || !(((Assign) s).targets.get(0) instanceof Subscript)) {
            return null;
        }
        Subscript t = (Subscript) ((Assign) s).targets.get(0);
        Expr k = t.index;
        if (!(t.value instanceof Name) || !TABLE.equals(((Name) t.value).id) || !(k instanceof Constant)
                || ((Constant) k).kind != Constant.Kind.STR) {
            return null;
        }
        return ((Constant) k).text;
    }

    private static void read(String text, Map<String, List<String>> groups, Set<String> modules) {
        String group = null;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                group = t.substring(1, t.length() - 1).trim();
            } else if (group != null && t.indexOf('=') > 0) {
                String target = t.substring(t.indexOf('=') + 1).trim();
                int cut = target.indexOf('[');
                target = (cut < 0 ? target : target.substring(0, cut)).replace(" ", "");
                int colon = target.indexOf(':');
                String mod = colon < 0 ? target : target.substring(0, colon);
                if (!dotted(mod) || colon >= 0 && !dotted(target.substring(colon + 1)) || !modules.contains(mod)) {
                    continue;
                }
                List<String> l = groups.get(group);
                if (l == null) {
                    l = new ArrayList<String>();
                    groups.put(group, l);
                }
                if (!l.contains(target)) {
                    l.add(target);
                }
            }
        }
    }

    private static boolean dotted(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (String p : s.split("\\.", -1)) {
            if (p.isEmpty() || !Character.isJavaIdentifierStart(p.charAt(0)) || p.charAt(0) == '$') {
                return false;
            }
            for (int i = 1; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c > 0x7e || !Character.isJavaIdentifierPart(c) || c == '$') {
                    return false;
                }
            }
        }
        return true;
    }

    private static String literal(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"') {
                b.append('\\');
            }
            b.append(c < 0x20 || c > 0x7e ? '_' : c);
        }
        return b.append('"').toString();
    }
}
