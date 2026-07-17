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

package com.codenameone.playground;

import com.codename1.ui.CodeDiagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Client-side heuristic linter for the playground Java editor, ported from the previous
 * browser-based editor. It surfaces the common mistakes that the interpreter's own diagnostics miss on
 * the fly (before a run completes, and regardless of how leniently the interpreter recovers):
 * unbalanced brackets and references to unknown types. Reported as {@link CodeDiagnostic}s the editor
 * draws as squiggles.
 */
final class PlaygroundHeuristicLint {

    private static final List<String> ALWAYS_CHECK = Arrays.asList(
            "Component", "Object", "String", "Integer", "Long", "Boolean", "Double", "Float");

    private PlaygroundHeuristicLint() {
    }

    private static final class Brace {
        final char ch;
        final int line;
        final int column;

        Brace(char ch, int line, int column) {
            this.ch = ch;
            this.line = line;
            this.column = column;
        }
    }

    static List<CodeDiagnostic> compute(String text) {
        List<CodeDiagnostic> markers = new ArrayList<CodeDiagnostic>();
        if (text == null || text.length() == 0) {
            return markers;
        }
        PlaygroundCompletionModel model = PlaygroundCompletionModel.get();
        Map<String, String> visible = model.visibleTypes(text);
        List<Brace> braceStack = new ArrayList<Brace>();
        String[] lines = split(text, '\n');
        boolean inBlockComment = false;
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            char inString = 0;
            StringBuilder codeLine = new StringBuilder();
            for (int column = 0; column < line.length(); column++) {
                char ch = line.charAt(column);
                char next = column + 1 < line.length() ? line.charAt(column + 1) : 0;
                if (inBlockComment) {
                    codeLine.append(' ');
                    if (ch == '*' && next == '/') {
                        inBlockComment = false;
                        codeLine.append(' ');
                        column++;
                    }
                    continue;
                }
                if (inString == 0 && ch == '/' && next == '/') {
                    while (codeLine.length() < line.length()) {
                        codeLine.append(' ');
                    }
                    break;
                }
                if (inString == 0 && ch == '/' && next == '*') {
                    inBlockComment = true;
                    codeLine.append("  ");
                    column++;
                    continue;
                }
                if (inString == 0 && (ch == '"' || ch == '\'')) {
                    inString = ch;
                    codeLine.append(' ');
                    continue;
                }
                if (inString != 0) {
                    codeLine.append(' ');
                    if (ch == '\\') {
                        if (column + 1 < line.length()) {
                            codeLine.append(' ');
                        }
                        column++;
                        continue;
                    }
                    if (ch == inString) {
                        inString = 0;
                    }
                    continue;
                }
                codeLine.append(ch);
                if (ch == '{' || ch == '(' || ch == '[') {
                    braceStack.add(new Brace(ch, lineIndex + 1, column + 1));
                } else if (ch == '}' || ch == ')' || ch == ']') {
                    Brace open = braceStack.isEmpty() ? null : braceStack.remove(braceStack.size() - 1);
                    if (open == null || !matchesBrace(open.ch, ch)) {
                        markers.add(error(lineIndex + 1, column + 1, lineIndex + 1, column + 2,
                                "Unmatched " + ch));
                    }
                }
            }
            collectUnknownTypeMarkers(markers, codeLine.toString(), lineIndex + 1, model, visible);
        }
        for (Brace b : braceStack) {
            markers.add(error(b.line, b.column, b.line, b.column + 1, "Missing closing match for " + b.ch));
        }
        return markers;
    }

    private static void collectUnknownTypeMarkers(List<CodeDiagnostic> markers, String line, int lineNumber,
            PlaygroundCompletionModel model, Map<String, String> visible) {
        // Scan for capitalized identifiers ([A-Z][A-Za-z0-9_]*) manually -- java.util.regex is not
        // CN1-safe. Only whole tokens (not preceded/followed by an identifier char) count.
        int i = 0;
        int n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (c >= 'A' && c <= 'Z' && (i == 0 || !isTokenChar(line.charAt(i - 1)))) {
                int start = i;
                i++;
                while (i < n && isTokenChar(line.charAt(i))) {
                    i++;
                }
                String token = line.substring(start, i);
                if (!isIgnoredTypeToken(line, token, start) && !model.isKnownType(token, visible)) {
                    markers.add(warning(lineNumber, start + 1, lineNumber, i + 1, "Unknown type " + token));
                }
            } else {
                i++;
            }
        }
    }

    private static boolean isTokenChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static boolean isIgnoredTypeToken(String line, String token, int index) {
        if (ALWAYS_CHECK.contains(token)) {
            return false;
        }
        if (line.indexOf("class " + token) >= 0 || line.indexOf("interface " + token) >= 0
                || line.indexOf("enum " + token) >= 0) {
            return true;
        }
        return index > 0 && line.charAt(index - 1) == '.';
    }

    private static boolean matchesBrace(char open, char close) {
        return (open == '{' && close == '}') || (open == '(' && close == ')') || (open == '[' && close == ']');
    }

    private static CodeDiagnostic error(int sl, int sc, int el, int ec, String message) {
        return new CodeDiagnostic(sl, sc, el, ec, message).setSeverity(CodeDiagnostic.ERROR);
    }

    private static CodeDiagnostic warning(int sl, int sc, int el, int ec, String message) {
        return new CodeDiagnostic(sl, sc, el, ec, message).setSeverity(CodeDiagnostic.WARNING);
    }

    /// String.split on the JS runtime treats its argument as a regex and drops trailing empties;
    /// this keeps every line (including blank trailing ones) so line numbers stay aligned.
    private static String[] split(String text, char delimiter) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == delimiter) {
                out.add(text.substring(start, i));
                start = i + 1;
            }
        }
        out.add(text.substring(start));
        return out.toArray(new String[out.size()]);
    }
}
