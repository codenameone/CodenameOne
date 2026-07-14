/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.io.JSONParser;
import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The pure code editor backend. It maps the code editor command / query vocabulary onto a `CodeView`,
/// parsing the diagnostics and completion payloads that arrive over the string command channel back into
/// `CodeDiagnostic` / `CodeCompletion` objects.
public class CodePureEditor extends PureEditor {
    private CodeView codeView;

    /// Creates a code editor backend.
    public CodePureEditor(EditorHost host, String editorType) {
        super(host, editorType);
        codeView = (CodeView) view();
    }

    @Override
    protected EditorView createView(EditorHost host, boolean codeMode) {
        return new CodeView(host);
    }

    @Override
    public void cmd(String name, String arg) {
        if ("setLanguage".equals(name)) {
            codeView.setLanguage(arg);
            return;
        }
        if ("setTheme".equals(name)) {
            codeView.setTheme(arg);
            return;
        }
        if ("setLineNumbers".equals(name)) {
            codeView.setShowLineNumbers("1".equals(arg));
            return;
        }
        if ("setTabSize".equals(name)) {
            codeView.setTabSize(parseInt(arg, 4));
            return;
        }
        if ("setCompletionEnabled".equals(name)) {
            codeView.setCompletionEnabled("1".equals(arg));
            return;
        }
        if ("setDiagnostics".equals(name)) {
            codeView.setDiagnostics(parseDiagnostics(arg));
            return;
        }
        if ("showCompletions".equals(name)) {
            handleShowCompletions(arg);
            return;
        }
        super.cmd(name, arg);
    }

    private void handleShowCompletions(String arg) {
        if (arg == null) {
            return;
        }
        int colon = arg.indexOf(':');
        if (colon < 0) {
            return;
        }
        int reqId = parseInt(arg.substring(0, colon), -1);
        List<CodeCompletion> items = parseCompletions(arg.substring(colon + 1));
        codeView.showCompletions(reqId, items);
    }

    private static List<Object> parseArray(String json) {
        if (json == null || json.length() == 0) {
            return new ArrayList<Object>();
        }
        try {
            Map<String, Object> root = JSONParser.parseJSON(json);
            Object list = root.get("root");
            if (list instanceof List) {
                return (List<Object>) list;
            }
        } catch (Throwable t) {
            // malformed payload, treat as empty
        }
        return new ArrayList<Object>();
    }

    private List<CodeDiagnostic> parseDiagnostics(String json) {
        List<CodeDiagnostic> out = new ArrayList<CodeDiagnostic>();
        List<Object> arr = parseArray(json);
        for (int i = 0; i < arr.size(); i++) {
            Object o = arr.get(i);
            if (!(o instanceof Map)) {
                continue;
            }
            Map m = (Map) o;
            int line = intVal(m.get("l"), 1);
            int col = intVal(m.get("c"), 1);
            int el = intVal(m.get("el"), line);
            int ec = intVal(m.get("ec"), col + 1);
            String sev = strVal(m.get("s"), CodeDiagnostic.ERROR);
            String msg = strVal(m.get("m"), "");
            out.add(new CodeDiagnostic(line, col, el, ec, msg).setSeverity(sev));
        }
        return out;
    }

    private List<CodeCompletion> parseCompletions(String json) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        List<Object> arr = parseArray(json);
        for (int i = 0; i < arr.size(); i++) {
            Object o = arr.get(i);
            if (!(o instanceof Map)) {
                continue;
            }
            Map m = (Map) o;
            String disp = strVal(m.get("d"), "");
            String ins = strVal(m.get("i"), disp);
            CodeCompletion cc = new CodeCompletion(disp, ins);
            cc.setType(strVal(m.get("t"), null));
            cc.setDetail(strVal(m.get("x"), null));
            out.add(cc);
        }
        return out;
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o instanceof String) {
            return parseInt((String) o, def);
        }
        return def;
    }

    private static String strVal(Object o, String def) {
        if (o instanceof String) {
            return (String) o;
        }
        return def;
    }

    private static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException err) {
            return def;
        }
    }
}
