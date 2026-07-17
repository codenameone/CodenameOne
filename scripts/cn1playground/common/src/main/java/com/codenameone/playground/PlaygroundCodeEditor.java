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

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Playground source pane built on the framework code editor API. */
final class PlaygroundCodeEditor {
    interface Listener {
        void onSourceChanged(String source, int version);
    }

    enum Mode {
        JAVA("java"),
        CSS("css");

        private final String language;

        Mode(String language) {
            this.language = language;
        }
    }

    /// CSS property names Codename One's CSS compiler understands, offered as completions in the CSS
    /// editor (the editor filters by the typed prefix).
    private static final String[] CSS_PROPERTIES = {
        "background", "background-color", "background-image", "background-repeat", "background-size",
        "backdrop-filter", "border", "border-top", "border-right", "border-bottom", "border-left",
        "border-color", "border-width", "border-style", "border-radius", "border-image", "box-shadow",
        "color", "cursor", "elevation", "filter", "font", "font-family", "font-size", "font-stretch",
        "font-style", "font-weight", "height", "icon-gap", "letter-spacing", "line-height", "margin",
        "margin-top", "margin-right", "margin-bottom", "margin-left", "max-height", "max-width",
        "min-height", "min-width", "opacity", "padding", "padding-top", "padding-right",
        "padding-bottom", "padding-left", "text-align", "text-decoration", "text-transform",
        "width"
    };

    private final CodeEditor editor;
    private final TextArea messages;
    private final Container component;
    private final Listener listener;
    private final Mode mode;
    private List<PlaygroundRunner.InlineMessage> inlineMessages = new ArrayList<PlaygroundRunner.InlineMessage>();
    private final List<String> uiidCompletions = new ArrayList<String>();
    private boolean completionProviderInstalled;
    private String source;
    private int version;
    private int pendingTextQuery;

    PlaygroundCodeEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        Mode actualMode = mode == null ? Mode.JAVA : mode;
        this.mode = actualMode;
        this.source = source == null ? "" : source;
        this.listener = listener;

        editor = new CodeEditor(actualMode.language, this.source);
        editor.setName(actualMode == Mode.CSS ? "PlaygroundCssSourceEditor" : "PlaygroundJavaSourceEditor");
        editor.setShowLineNumbers(true);
        editor.setTabSize(4);

        messages = new TextArea("", 4, 80, TextArea.ANY);
        messages.setEditable(false);
        messages.setSingleLineTextArea(false);
        messages.setGrowByContent(true);
        messages.setUIID("Label");
        // setHidden collapses the preferred size; setVisible(false) would keep reserving the
        // empty strip's rows under the editor
        messages.setHidden(true);

        component = new Container(new BorderLayout());
        component.add(BorderLayout.CENTER, editor);
        component.add(BorderLayout.SOUTH, messages);

        editor.addChangeListener(evt -> readChangedSource());
        applyTheme(darkMode);
    }

    Component getComponent() {
        return component;
    }

    void setSource(String source) {
        this.source = source == null ? "" : source;
        pendingTextQuery++;
        editor.setText(this.source);
    }

    void setMarkers(List<PlaygroundRunner.Diagnostic> diagnostics) {
        List<CodeDiagnostic> converted = new ArrayList<CodeDiagnostic>();
        if (diagnostics != null) {
            for (PlaygroundRunner.Diagnostic diagnostic : diagnostics) {
                if (diagnostic == null) {
                    continue;
                }
                int line = Math.max(1, diagnostic.line);
                int column = Math.max(1, diagnostic.column);
                int endLine = Math.max(line, diagnostic.endLine);
                int endColumn = diagnostic.endColumn;
                if (endLine == line) {
                    endColumn = Math.max(column + 1, endColumn);
                } else {
                    endColumn = Math.max(1, endColumn);
                }
                converted.add(new CodeDiagnostic(line, column, endLine, endColumn, diagnostic.message)
                        .setSeverity(diagnostic.severity));
            }
        }
        editor.setDiagnostics(converted);
    }

    void setUiidCompletions(List<String> uiids) {
        uiidCompletions.clear();
        if (uiids != null) {
            for (String uiid : uiids) {
                if (uiid != null && uiid.trim().length() > 0) {
                    uiidCompletions.add(uiid.trim());
                }
            }
        }
        if (!completionProviderInstalled) {
            completionProviderInstalled = true;
            editor.setCompletionProvider((ed, code, cursor, results)
                    -> results.onSucess(buildCompletions(code == null ? "" : code, cursor)));
        }
    }

    private List<CodeCompletion> buildCompletions(String code, int cursor) {
        List<CodeCompletion> out = new ArrayList<CodeCompletion>();
        Set<String> seen = new java.util.LinkedHashSet<String>();
        // The editor filters proposals by the typed prefix too, but the index holds ~1500 types, so
        // filter here as well to keep the returned payload small on every keystroke.
        String prefix = currentPrefix(code, cursor);
        if (mode == Mode.CSS) {
            // CSS pane: property names plus the UIID selectors visible in the preview.
            for (String property : CSS_PROPERTIES) {
                add(out, seen, property, prefix, "property", null);
            }
            for (String uiid : uiidCompletions) {
                add(out, seen, uiid, prefix, "uiid", null);
            }
            addBufferIdentifiers(out, seen, code, cursor, prefix);
            return out;
        }

        // Java pane. After `receiver.` offer that type's members (faux reflection over the CN1
        // access index) and nothing else -- global names are irrelevant in a member position.
        PlaygroundCompletionModel model = PlaygroundCompletionModel.get();
        String receiver = model.findReceiver(code, cursor);
        if (receiver.length() > 0) {
            String type = model.inferType(receiver, code, model.visibleTypes(code));
            for (String member : model.memberSignatures(type)) {
                add(out, seen, member, prefix, "member", insertFor(member));
            }
            return out;
        }

        // No receiver: UIIDs, in-scope globals, visible type names, buffer identifiers and keywords.
        for (String uiid : uiidCompletions) {
            add(out, seen, uiid, prefix, "uiid", null);
        }
        for (String global : model.globals().keySet()) {
            add(out, seen, global, prefix, "variable", null);
        }
        for (String simple : model.typeSimpleNames()) {
            add(out, seen, simple, prefix, "type", null);
        }
        addBufferIdentifiers(out, seen, code, cursor, prefix);
        for (String keyword : model.keywords()) {
            add(out, seen, keyword, prefix, "keyword", null);
        }
        return out;
    }

    private static void add(List<CodeCompletion> out, Set<String> seen, String display, String prefix,
            String type, String insert) {
        if (display == null || display.length() == 0 || !matchesPrefix(display, prefix) || !seen.add(display)) {
            return;
        }
        CodeCompletion cc = insert == null ? new CodeCompletion(display) : new CodeCompletion(display, insert);
        out.add(cc.setType(type));
    }

    private static boolean matchesPrefix(String candidate, String prefix) {
        if (prefix.length() == 0) {
            return true;
        }
        if (candidate.length() < prefix.length()) {
            return false;
        }
        return candidate.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /// The identifier characters immediately before the cursor (the part after a member-access dot, or
    /// the word being typed) -- the prefix the proposals are filtered by.
    private static String currentPrefix(String code, int cursor) {
        int end = Math.max(0, Math.min(cursor, code.length()));
        int start = end;
        while (start > 0 && isIdentPart(code.charAt(start - 1))) {
            start--;
        }
        return code.substring(start, end);
    }

    /// Insert text for a member proposal: `name(` for a method with arguments (so the caret lands
    /// inside the parentheses), `name()` for a no-arg method, and the field name verbatim.
    private static String insertFor(String signature) {
        int paren = signature.indexOf('(');
        if (paren < 0) {
            return signature;
        }
        String name = signature.substring(0, paren);
        return signature.endsWith("()") ? name + "()" : name + "(";
    }

    private void addBufferIdentifiers(List<CodeCompletion> out, Set<String> seen, String code, int cursor,
            String prefix) {
        // Offer identifiers already present in the source so partially typed variable / method
        // names (e.g. "varia" -> "variableName") complete.
        for (String word : collectIdentifiers(code, cursor)) {
            add(out, seen, word, prefix, "text", null);
        }
    }

    /// Extracts distinct Java-style identifiers from the source, excluding the token currently being
    /// typed at {@code cursor} (so a half-typed word doesn't propose itself) and single-character names.
    private static List<String> collectIdentifiers(String code, int cursor) {
        List<String> words = new ArrayList<String>();
        if (code == null || code.length() == 0) {
            return words;
        }
        int typedStart = cursor;
        while (typedStart > 0 && isIdentPart(code.charAt(typedStart - 1))) {
            typedStart--;
        }
        int typedEnd = cursor;
        while (typedEnd < code.length() && isIdentPart(code.charAt(typedEnd))) {
            typedEnd++;
        }
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<String>();
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (isIdentStart(c)) {
                int start = i;
                i++;
                while (i < n && isIdentPart(code.charAt(i))) {
                    i++;
                }
                // skip the token straddling the caret and trivial one-character names
                if (start == typedStart && i == typedEnd) {
                    continue;
                }
                if (i - start > 1) {
                    set.add(code.substring(start, i));
                }
            } else {
                i++;
            }
        }
        words.addAll(set);
        return words;
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || (c >= '0' && c <= '9');
    }

    void setInlineMessages(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        this.inlineMessages = inlineMessages == null
                ? new ArrayList<PlaygroundRunner.InlineMessage>()
                : new ArrayList<PlaygroundRunner.InlineMessage>(inlineMessages);
        renderMessages();
    }

    void applyTheme(boolean darkMode) {
        editor.setUIID(darkMode ? "PlaygroundSourceEditorDark" : "PlaygroundSourceEditor");
        editor.setTheme(darkMode ? "dark" : "light");

        Style messageStyle = messages.getAllStyles();
        messageStyle.setBgColor(darkMode ? 0x0f172a : 0xffffff);
        messageStyle.setFgColor(darkMode ? 0xe2e8f0 : 0x0f172a);
        messageStyle.setBgTransparency(255);
        messageStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        messageStyle.setPadding(1f, 1f, 1.5f, 1.5f);
        editor.repaint();
        messages.repaint();
    }

    private void readChangedSource() {
        final int query = ++pendingTextQuery;
        editor.getText(new SuccessCallback<String>() {
            @Override
            public void onSucess(String updated) {
                if (query != pendingTextQuery) {
                    return;
                }
                if (updated == null) {
                    updated = "";
                }
                if (updated.equals(PlaygroundCodeEditor.this.source)) {
                    return;
                }
                PlaygroundCodeEditor.this.source = updated;
                if (PlaygroundCodeEditor.this.listener != null) {
                    PlaygroundCodeEditor.this.listener.onSourceChanged(updated, ++version);
                }
            }
        });
    }

    private void renderMessages() {
        StringBuilder out = new StringBuilder();
        for (PlaygroundRunner.InlineMessage message : inlineMessages) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(message.text == null ? "" : message.text);
        }
        messages.setText(out.toString());
        messages.setHidden(out.length() == 0);
        if (component.getComponentForm() != null) {
            component.revalidate();
        }
    }
}
