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

    private final CodeEditor editor;
    private final TextArea messages;
    private final Container component;
    private final Listener listener;
    private List<PlaygroundRunner.InlineMessage> inlineMessages = new ArrayList<PlaygroundRunner.InlineMessage>();
    private final List<String> uiidCompletions = new ArrayList<String>();
    private boolean completionProviderInstalled;
    private String source;
    private int version;
    private int pendingTextQuery;

    PlaygroundCodeEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        Mode actualMode = mode == null ? Mode.JAVA : mode;
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
            editor.setCompletionProvider((ed, code, cursor, results) -> {
                List<CodeCompletion> out = new ArrayList<CodeCompletion>();
                java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();
                for (String uiid : uiidCompletions) {
                    if (seen.add(uiid)) {
                        out.add(new CodeCompletion(uiid).setType("uiid"));
                    }
                }
                // Offer identifiers already present in the source so partially typed variable / method
                // names (e.g. "varia" -> "variableName") complete. The editor filters by the typed
                // prefix, so returning the whole set here is fine.
                for (String word : collectIdentifiers(code, cursor)) {
                    if (seen.add(word)) {
                        out.add(new CodeCompletion(word).setType("text"));
                    }
                }
                results.onSucess(out);
            });
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
