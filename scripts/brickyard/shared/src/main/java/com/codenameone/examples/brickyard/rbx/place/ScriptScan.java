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
package com.codenameone.examples.brickyard.rbx.place;

import com.codenameone.examples.brickyard.rbx.ClassDb;

/// A quick static look at a script's source for things that cannot work
/// here: services the engine does not have, `Instance.new` of an unknown
/// class, and the sandbox-breaking globals Luau itself removed. It reads
/// text only; nothing runs.
final class ScriptScan {
    private static final String[] REMOVED = {"loadstring", "getfenv", "setfenv", "newproxy", "debug.sethook"};

    private ScriptScan() {
    }

    static void scan(String script, String src, CompatReport r) {
        quoted(src, "GetService(", script, r, true);
        quoted(src, "Instance.new(", script, r, false);
        for (String g : REMOVED) {
            int at = src.indexOf(g);
            while (at >= 0) {
                boolean start = at == 0 || !ident(src.charAt(at - 1));
                int e = at + g.length();
                boolean end = e >= src.length() || !ident(src.charAt(e));
                if (start && end) {
                    CompatReport.add(r.scriptFindings, script, g + " is not available");
                    break;
                }
                at = src.indexOf(g, at + 1);
            }
        }
    }

    private static boolean ident(char c) {
        return c == '_' || c == '.' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }

    private static void quoted(String src, String call, String script, CompatReport r, boolean service) {
        int at = src.indexOf(call);
        while (at >= 0) {
            int p = at + call.length();
            while (p < src.length() && src.charAt(p) == ' ') {
                p++;
            }
            if (p < src.length() && (src.charAt(p) == '"' || src.charAt(p) == '\'')) {
                int e = src.indexOf(src.charAt(p), p + 1);
                if (e > p) {
                    String name = src.substring(p + 1, e);
                    ClassDb.Desc d = ClassDb.get(name);
                    if (service && (d == null || !d.isService())) {
                        CompatReport.add(r.scriptFindings, script, "service " + name + " is not available");
                    } else if (!service && (d == null || !d.isCreatable())) {
                        CompatReport.add(r.scriptFindings, script, "Instance.new(\"" + name + "\") is not available");
                    }
                }
            }
            at = src.indexOf(call, at + 1);
        }
    }
}
