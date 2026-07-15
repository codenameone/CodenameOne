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
package com.codename1.ui;

import com.codename1.ui.editor.CodePureEditor;
import com.codename1.ui.editor.PureEditor;
import com.codename1.ui.editor.SyntaxHighlighter;
import com.codename1.ui.editor.SyntaxHighlightResult;
import com.codename1.ui.editor.SyntaxToken;
import com.codename1.util.SuccessCallback;

import java.util.Hashtable;
import java.util.List;

/// An IDE style source code editor with syntax highlighting, a line-number gutter and asynchronous
/// code completion.
///
/// `CodeEditor` is built for editing source code on both touch devices (with the virtual keyboard) and
/// desktops (with a physical keyboard). Code completion is driven by a `CodeCompletionProvider`, which
/// can resolve proposals locally or from a remote language server.
///
/// Like `RichTextArea`, ports with low-level text input use the pure Codename One text engine
/// (`com.codename1.ui.editor`) with a built-in incremental syntax highlighter. Ports without that input
/// contract use an editable `BrowserComponent` fallback, and a port can transparently supply a native
/// code editor instead.
///
/// #### Example
///
/// ```java
/// CodeEditor editor = new CodeEditor();
/// editor.setLanguage("java");
/// editor.setText("public class Main {\n\n}");
/// editor.setCompletionProvider((ed, code, cursor, results) -> {
///     List<CodeCompletion> out = new ArrayList<>();
///     out.add(new CodeCompletion("System.out.println(", "System.out.println()").setType("method"));
///     results.onSucess(out);
/// });
/// form.add(BorderLayout.CENTER, editor);
/// ```
///
/// @author Shai Almog
public class CodeEditor extends AbstractEditorComponent {
    private static final Hashtable<String, SyntaxHighlighter> syntaxHighlighters =
            new Hashtable<String, SyntaxHighlighter>();
    private String language = "text";
    private String theme = "light";
    private boolean showLineNumbers = true;
    private int tabSize = 4;
    private String engineUrl;
    private CodeCompletionProvider completionProvider;

    /// Creates an empty code editor.
    public CodeEditor() {
        super("CodeEditor");
    }

    /// Creates a code editor initialized with the supplied source and language.
    ///
    /// #### Parameters
    ///
    /// - `language`: the language id used for syntax highlighting (e.g. `"java"`, `"javascript"`,
    ///   `"kotlin"`, `"css"`, `"xml"`, `"json"`, `"python"`)
    ///
    /// - `text`: the initial source code
    public CodeEditor(String language, String text) {
        this();
        setLanguage(language);
        setText(text);
    }

    @Override
    String getEditorType() {
        return "code";
    }

    @Override
    PureEditor createPureEditor() {
        return new CodePureEditor(this, getEditorType());
    }

    /// Replaces the entire editor content.
    ///
    /// #### Parameters
    ///
    /// - `text`: the source code
    public void setText(String text) {
        command("setText", text == null ? "" : text);
    }

    /// Retrieves the current source code. The callback is invoked on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the source code
    public void getText(SuccessCallback<String> callback) {
        query("getText", null, callback);
    }

    /// Sets the language used for syntax highlighting.
    ///
    /// #### Parameters
    ///
    /// - `language`: the language id (e.g. `"java"`, `"javascript"`, `"kotlin"`, `"css"`, `"xml"`,
    ///   `"json"`, `"python"`)
    public void setLanguage(String language) {
        this.language = language == null ? "text" : language;
        command("setLanguage", this.language);
        command("setCustomHighlighter", getRegisteredSyntaxHighlighter(this.language) == null ? "0" : "1");
    }

    /// Returns the current highlighting language id.
    public String getLanguage() {
        return language;
    }

    /// Registers a syntax highlighter for a language id. Registration is global and affects existing
    /// editors the next time their language is set. Passing null removes a previous registration.
    public static void registerSyntaxHighlighter(String language, SyntaxHighlighter highlighter) {
        String id = normalizeLanguage(language);
        if (highlighter == null) {
            syntaxHighlighters.remove(id);
        } else {
            syntaxHighlighters.put(id, highlighter);
        }
    }

    /// Returns the third-party syntax highlighter registered for a language, or null.
    public static SyntaxHighlighter getRegisteredSyntaxHighlighter(String language) {
        return syntaxHighlighters.get(normalizeLanguage(language));
    }

    private static String normalizeLanguage(String language) {
        return language == null ? "text" : language.trim().toLowerCase();
    }

    /// Sets the color theme. Currently `"light"` and `"dark"` are supported.
    ///
    /// #### Parameters
    ///
    /// - `theme`: the theme id
    public void setTheme(String theme) {
        this.theme = theme == null ? "light" : theme;
        command("setTheme", this.theme);
    }

    /// Returns the current theme id.
    public String getTheme() {
        return theme;
    }

    /// Shows or hides the line-number gutter.
    ///
    /// #### Parameters
    ///
    /// - `show`: true to show line numbers
    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
        command("setLineNumbers", show ? "1" : "0");
    }

    /// Returns true if the line-number gutter is shown.
    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    /// Sets the number of spaces inserted for a tab / used for indentation.
    ///
    /// #### Parameters
    ///
    /// - `tabSize`: the indentation width in spaces
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
        command("setTabSize", String.valueOf(tabSize));
    }

    /// Returns the indentation width in spaces.
    public int getTabSize() {
        return tabSize;
    }

    /// Makes the editor read-only or editable. This is a convenience around
    /// `AbstractEditorComponent#setEditable(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `readOnly`: true to prevent editing
    public void setReadOnly(boolean readOnly) {
        setEditable(!readOnly);
    }

    /// Returns true when the editor is read-only.
    public boolean isReadOnly() {
        return !isEditable();
    }

    /// Inserts text at the current caret position, replacing any active selection.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text to insert
    public void insertAtCursor(String text) {
        command("insertText", text);
    }

    /// Retrieves the current caret character offset. The callback is invoked on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the caret offset as an Integer
    public void getCursorPosition(final SuccessCallback<Integer> callback) {
        query("getCursor", null, new StringToIntCallback(callback));
    }

    private static final class StringToIntCallback implements SuccessCallback<String> {
        private final SuccessCallback<Integer> delegate;

        StringToIntCallback(SuccessCallback<Integer> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSucess(String value) {
            int v = 0;
            try {
                if (value != null && value.length() > 0) {
                    v = Integer.parseInt(value.trim());
                }
            } catch (NumberFormatException err) {
                v = 0;
            }
            delegate.onSucess(Integer.valueOf(v));
        }
    }

    /// Sets the diagnostics (errors / warnings / hints) displayed in the editor as squiggly underlines,
    /// gutter markers and tooltips. Pass an empty list (or null) to clear all diagnostics.
    ///
    /// #### Parameters
    ///
    /// - `diagnostics`: the diagnostics to display
    public void setDiagnostics(List<CodeDiagnostic> diagnostics) {
        command("setDiagnostics", diagnosticsJson(diagnostics));
    }

    private static String diagnosticsJson(List<CodeDiagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder("[");
        if (diagnostics != null) {
            for (CodeDiagnostic d : diagnostics) {
                if (d == null) {
                    continue;
                }
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append("{\"l\":").append(d.getLine())
                    .append(",\"c\":").append(d.getColumn())
                    .append(",\"el\":").append(d.getEndLine())
                    .append(",\"ec\":").append(d.getEndColumn())
                    .append(",\"s\":\"").append(jsonEscape(d.getSeverity()))
                    .append("\",\"m\":\"").append(jsonEscape(d.getMessage()))
                    .append("\"}");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /// Points the browser fallback at a custom editor page in the app hierarchy. Ports with low-level
    /// text input continue to use the pure editor; this URL is used only where the browser fallback is
    /// required.
    ///
    /// #### Parameters
    ///
    /// - `url`: an app-hierarchy URL, or null for the built-in page
    public void setEngineURL(String url) {
        this.engineUrl = url;
    }

    /// Returns the custom browser-engine URL, or null for the built-in page.
    @Override
    public String getEngineURL() {
        return engineUrl;
    }

    /// Sets the provider that supplies code completion proposals. Passing null disables completion.
    ///
    /// #### Parameters
    ///
    /// - `provider`: the completion provider, or null to disable completion
    public void setCompletionProvider(CodeCompletionProvider provider) {
        this.completionProvider = provider;
        command("setCompletionEnabled", provider != null ? "1" : "0");
    }

    /// Returns the current completion provider, or null if none is set.
    public CodeCompletionProvider getCompletionProvider() {
        return completionProvider;
    }

    @Override
    void onEditorEvent(String type, String value) {
        if ("highlight".equals(type)) {
            handleHighlightRequest(value);
            return;
        }
        if ("complete".equals(type)) {
            handleCompletionRequest(value);
            return;
        }
        super.onEditorEvent(type, value);
    }

    private void handleHighlightRequest(String value) {
        SyntaxHighlighter highlighter = getRegisteredSyntaxHighlighter(language);
        if (highlighter == null || value == null) {
            return;
        }
        int colon = value.indexOf(':');
        if (colon < 0) {
            return;
        }
        String request = value.substring(0, colon);
        String source = value.substring(colon + 1);
        StringBuilder html = new StringBuilder();
        int state = 0;
        int lineStart = 0;
        while (lineStart <= source.length()) {
            int lineEnd = source.indexOf('\n', lineStart);
            boolean lastLine = lineEnd < 0;
            if (lastLine) {
                lineEnd = source.length();
            }
            if (lineStart > 0) {
                html.append('\n');
            }
            String line = source.substring(lineStart, lineEnd);
            SyntaxHighlightResult result = highlighter.tokenize(line, state);
            if (result == null) {
                appendHtml(html, line, 0, line.length());
                state = 0;
                if (lastLine) {
                    break;
                }
                lineStart = lineEnd + 1;
                continue;
            }
            state = result.endState;
            int position = 0;
            for (int i = 0; i < result.tokens.size(); i++) {
                SyntaxToken token = result.tokens.get(i);
                if (token == null) {
                    continue;
                }
                int start = Math.max(position, Math.min(line.length(), token.start));
                int end = Math.max(start, Math.min(line.length(), token.start + token.length));
                appendHtml(html, line, position, start);
                if (end > start) {
                    appendTokenStart(html, token);
                    appendHtml(html, line, start, end);
                    html.append("</span>");
                }
                position = end;
            }
            appendHtml(html, line, position, line.length());
            if (lastLine) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        command("applyCustomHighlight", request + ":" + html.toString());
    }

    private static void appendTokenStart(StringBuilder out, SyntaxToken token) {
        if (token.lightColor >= 0 || token.darkColor >= 0) {
            int light = token.lightColor >= 0 ? token.lightColor : token.darkColor;
            int dark = token.darkColor >= 0 ? token.darkColor : token.lightColor;
            out.append("<span class=\"cx\" style=\"--cl:#").append(rgb(light))
                    .append(";--cd:#").append(rgb(dark)).append("\">");
            return;
        }
        String css = token.kind == SyntaxToken.KEYWORD || token.kind == SyntaxToken.TYPE ? "kw"
                : (token.kind == SyntaxToken.STRING || token.kind == SyntaxToken.PROPERTY ? "st"
                : (token.kind == SyntaxToken.COMMENT ? "cm"
                : (token.kind == SyntaxToken.NUMBER ? "nu" : "")));
        out.append("<span");
        if (css.length() > 0) {
            out.append(" class=\"").append(css).append('"');
        }
        out.append('>');
    }

    private static String rgb(int color) {
        String value = Integer.toHexString(color & 0xffffff);
        while (value.length() < 6) {
            value = "0" + value;
        }
        return value;
    }

    private static void appendHtml(StringBuilder out, String value, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
        }
    }

    private void handleCompletionRequest(String value) {
        final CodeCompletionProvider provider = completionProvider;
        if (provider == null || value == null) {
            return;
        }
        int colon = value.indexOf(':');
        if (colon < 0) {
            return;
        }
        final String reqId = value.substring(0, colon);
        final int cursor;
        int c;
        try {
            c = Integer.parseInt(value.substring(colon + 1).trim());
        } catch (NumberFormatException err) {
            c = 0;
        }
        cursor = c;
        getText(new SuccessCallback<String>() {
            @Override
            public void onSucess(String code) {
                final String safeCode = code == null ? "" : code;
                provider.getCompletions(CodeEditor.this, safeCode, cursor, new SuccessCallback<List<CodeCompletion>>() {
                    @Override
                    public void onSucess(List<CodeCompletion> results) {
                        command("showCompletions", reqId + ":" + toJson(results));
                    }
                });
            }
        });
    }

    private static String toJson(List<CodeCompletion> items) {
        StringBuilder sb = new StringBuilder("[");
        if (items != null) {
            for (CodeCompletion cc : items) {
                if (cc == null) {
                    continue;
                }
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append("{\"d\":\"").append(jsonEscape(cc.getDisplayText()))
                    .append("\",\"i\":\"").append(jsonEscape(cc.getInsertText()))
                    .append("\",\"t\":\"").append(jsonEscape(cc.getType()))
                    .append("\",\"x\":\"").append(jsonEscape(cc.getDetail()))
                    .append("\"}");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        String hex = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    String createEditorHtml() {
        return CodeEditorHtml.PAGE;
    }

}
