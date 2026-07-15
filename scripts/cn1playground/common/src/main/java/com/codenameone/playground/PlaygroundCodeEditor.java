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
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Playground adapter around the framework {@link CodeEditor}. The framework editor owns text input,
 * selection, scrolling, diagnostics, syntax highlighting and the pure/browser/native backend choice.
 * The playground's Monaco page is retained as a custom browser engine so the web deployment can add
 * its generated Codename One metadata and UIID completions without bypassing the editor API.
 */
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

        String language() {
            return language;
        }
    }

    private static final class EditorBridge extends CodeEditor {
        EditorBridge(String language, String source) {
            super(language, source);
        }

        void playgroundCommand(String name, String value) {
            command(name, value);
        }
    }

    private final EditorBridge editor;
    private final TextArea messages;
    private final Container component;
    private final Listener listener;
    private String source;
    private String messageText = "";
    private int version;

    PlaygroundCodeEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        Mode actualMode = mode == null ? Mode.JAVA : mode;
        this.source = source == null ? "" : source;
        this.listener = listener;

        editor = new EditorBridge(actualMode.language(), this.source);
        editor.setEngineURL("/playground-editor/index.html");
        editor.setTheme(darkMode ? "dark" : "light");
        editor.setShowLineNumbers(true);
        editor.setTabSize(4);
        editor.playgroundCommand("playgroundMetadata", PlaygroundEditorMetadata.json());

        messages = new TextArea("", 4, 80, TextArea.ANY);
        messages.setEditable(false);
        messages.setSingleLineTextArea(false);
        messages.setUIID("Label");
        messages.setVisible(false);

        component = new Container(new BorderLayout());
        component.add(BorderLayout.CENTER, editor);
        component.add(BorderLayout.SOUTH, messages);

        editor.addChangeListener(event -> editor.getText(new SuccessCallback<String>() {
            @Override
            public void onSucess(String value) {
                String updated = value == null ? "" : value;
                if (updated.equals(PlaygroundCodeEditor.this.source)) {
                    return;
                }
                PlaygroundCodeEditor.this.source = updated;
                PlaygroundCodeEditor.this.listener.onSourceChanged(updated, ++version);
            }
        }));
        editor.addReadyListener(event -> refreshMessageVisibility());
        applyTheme(darkMode);
    }

    Component getComponent() {
        return component;
    }

    void setSource(String source) {
        this.source = source == null ? "" : source;
        editor.setText(this.source);
    }

    void setMarkers(List<PlaygroundRunner.Diagnostic> diagnostics) {
        List<CodeDiagnostic> converted = new ArrayList<CodeDiagnostic>();
        if (diagnostics != null) {
            for (PlaygroundRunner.Diagnostic diagnostic : diagnostics) {
                converted.add(new CodeDiagnostic(
                        Math.max(1, diagnostic.line),
                        Math.max(1, diagnostic.column),
                        Math.max(1, diagnostic.endLine),
                        Math.max(1, diagnostic.endColumn),
                        diagnostic.message).setSeverity(diagnostic.severity));
            }
        }
        editor.setDiagnostics(converted);
        renderMessages(diagnostics, null);
    }

    void setInlineMessages(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        editor.playgroundCommand("setInlineMessages", toMessagesJson(inlineMessages));
        renderMessages(null, inlineMessages);
    }

    void applyTheme(boolean darkMode) {
        editor.setTheme(darkMode ? "dark" : "light");
        int bg = darkMode ? 0x0f172a : 0xffffff;
        int fg = darkMode ? 0xe2e8f0 : 0x0f172a;
        messages.getAllStyles().setBgColor(bg);
        messages.getAllStyles().setFgColor(fg);
        messages.getAllStyles().setBgTransparency(255);
        messages.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        messages.getAllStyles().setPadding(1f, 1f, 1.5f, 1.5f);
        messages.repaint();
    }

    void setUiidCompletions(List<String> uiids) {
        editor.playgroundCommand("setUiids", toUiidJson(uiids));
    }

    private void renderMessages(List<PlaygroundRunner.Diagnostic> diagnostics,
                                List<PlaygroundRunner.InlineMessage> inlineMessages) {
        StringBuilder out = new StringBuilder();
        if (diagnostics != null) {
            for (PlaygroundRunner.Diagnostic diagnostic : diagnostics) {
                appendLine(out, diagnostic.line > 0
                        ? "Line " + diagnostic.line + ": " + diagnostic.message
                        : diagnostic.message);
            }
        }
        if (inlineMessages != null) {
            for (PlaygroundRunner.InlineMessage message : inlineMessages) {
                appendLine(out, message.text);
            }
        }
        messageText = out.toString();
        messages.setText(messageText);
        refreshMessageVisibility();
    }

    private static void appendLine(StringBuilder out, String value) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(value == null ? "" : value);
    }

    private void refreshMessageVisibility() {
        boolean browserEngine = editor.isEditorReady() && editor.getInternalBrowser() != null;
        messages.setVisible(!browserEngine && messageText.length() > 0);
        if (component.getComponentForm() != null) {
            component.revalidate();
        }
    }

    private static String toMessagesJson(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        StringBuilder out = new StringBuilder("[");
        if (inlineMessages != null) {
            for (PlaygroundRunner.InlineMessage message : inlineMessages) {
                if (out.length() > 1) {
                    out.append(',');
                }
                out.append('{');
                appendJsonField(out, "kind", message.kind);
                out.append(',');
                appendJsonField(out, "text", message.text);
                out.append(',').append("\"line\":").append(message.line).append('}');
            }
        }
        return out.append(']').toString();
    }

    private static String toUiidJson(List<String> uiids) {
        StringBuilder out = new StringBuilder("[");
        if (uiids != null) {
            for (String uiid : uiids) {
                if (uiid == null || uiid.trim().length() == 0) {
                    continue;
                }
                if (out.length() > 1) {
                    out.append(',');
                }
                appendJsonString(out, uiid);
            }
        }
        return out.append(']').toString();
    }

    private static void appendJsonField(StringBuilder out, String name, String value) {
        appendJsonString(out, name);
        out.append(':');
        appendJsonString(out, value == null ? "" : value);
    }

    private static void appendJsonString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') {
                out.append('\\').append(ch);
            } else if (ch == '\n') {
                out.append("\\n");
            } else if (ch == '\r') {
                out.append("\\r");
            } else if (ch == '\t') {
                out.append("\\t");
            } else if (ch < 0x20) {
                String hex = Integer.toHexString(ch);
                out.append("\\u");
                for (int j = hex.length(); j < 4; j++) {
                    out.append('0');
                }
                out.append(hex);
            } else {
                out.append(ch);
            }
        }
        out.append('"');
    }
}
