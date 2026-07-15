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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Playground source editor built directly on the opt-in lightweight editing API in {@link TextArea}.
 */
final class PlaygroundLightweightEditor {
    private static final int MAX_SOURCE_LENGTH = 1_000_000;

    interface Listener {
        void onSourceChanged(String source, int version);
    }

    enum Mode {
        JAVA,
        CSS
    }

    private final TextArea editor;
    private final TextArea messages;
    private final Container component;
    private final Listener listener;
    private List<PlaygroundRunner.Diagnostic> diagnostics = new ArrayList<PlaygroundRunner.Diagnostic>();
    private List<PlaygroundRunner.InlineMessage> inlineMessages = new ArrayList<PlaygroundRunner.InlineMessage>();
    private String source;
    private int version;

    PlaygroundLightweightEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        Mode actualMode = mode == null ? Mode.JAVA : mode;
        this.source = source == null ? "" : source;
        this.listener = listener;

        editor = new TextArea(this.source, 20, 80, TextArea.ANY);
        editor.setName(actualMode == Mode.CSS ? "PlaygroundCssSourceEditor" : "PlaygroundJavaSourceEditor");
        editor.setMaxSize(Math.max(MAX_SOURCE_LENGTH, this.source.length() + 1));
        editor.setSingleLineTextArea(false);
        editor.setGrowByContent(false);
        editor.setSmoothScrolling(true);
        editor.setLightweightEditingEnabled(true);

        messages = new TextArea("", 4, 80, TextArea.ANY);
        messages.setEditable(false);
        messages.setSingleLineTextArea(false);
        messages.setGrowByContent(true);
        messages.setUIID("Label");
        messages.setVisible(false);

        component = new Container(new BorderLayout());
        component.add(BorderLayout.CENTER, editor);
        component.add(BorderLayout.SOUTH, messages);

        editor.addDataChangedListener((type, index) -> {
            String updated = editor.getText();
            if (updated == null) {
                updated = "";
            }
            if (updated.equals(PlaygroundLightweightEditor.this.source)) {
                return;
            }
            PlaygroundLightweightEditor.this.source = updated;
            if (PlaygroundLightweightEditor.this.listener != null) {
                PlaygroundLightweightEditor.this.listener.onSourceChanged(updated, ++version);
            }
        });
        applyTheme(darkMode);
    }

    Component getComponent() {
        return component;
    }

    void setSource(String source) {
        this.source = source == null ? "" : source;
        if (this.source.length() >= editor.getMaxSize()) {
            editor.setMaxSize(this.source.length() + 1);
        }
        editor.setText(this.source);
    }

    void setMarkers(List<PlaygroundRunner.Diagnostic> diagnostics) {
        this.diagnostics = diagnostics == null
                ? new ArrayList<PlaygroundRunner.Diagnostic>()
                : new ArrayList<PlaygroundRunner.Diagnostic>(diagnostics);
        renderMessages();
    }

    void setInlineMessages(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        this.inlineMessages = inlineMessages == null
                ? new ArrayList<PlaygroundRunner.InlineMessage>()
                : new ArrayList<PlaygroundRunner.InlineMessage>(inlineMessages);
        renderMessages();
    }

    void applyTheme(boolean darkMode) {
        editor.setUIID(darkMode ? "PlaygroundSourceEditorDark" : "PlaygroundSourceEditor");
        Style editorStyle = editor.getAllStyles();
        editorStyle.setBgColor(darkMode ? 0x0f172a : 0xffffff);
        editorStyle.setFgColor(darkMode ? 0xe2e8f0 : 0x0f172a);
        editorStyle.setBgTransparency(255);
        editorStyle.setBorder(Border.createEmpty());
        editorStyle.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        editorStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        editorStyle.setPadding(1.5f, 1.5f, 1.5f, 1.5f);

        Style messageStyle = messages.getAllStyles();
        messageStyle.setBgColor(darkMode ? 0x0f172a : 0xffffff);
        messageStyle.setFgColor(darkMode ? 0xe2e8f0 : 0x0f172a);
        messageStyle.setBgTransparency(255);
        messageStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        messageStyle.setPadding(1f, 1f, 1.5f, 1.5f);
        editor.repaint();
        messages.repaint();
    }

    private void renderMessages() {
        StringBuilder out = new StringBuilder();
        for (PlaygroundRunner.Diagnostic diagnostic : diagnostics) {
            appendLine(out, diagnostic.line > 0
                    ? "Line " + diagnostic.line + ": " + diagnostic.message
                    : diagnostic.message);
        }
        for (PlaygroundRunner.InlineMessage message : inlineMessages) {
            appendLine(out, message.text);
        }
        messages.setText(out.toString());
        messages.setVisible(out.length() > 0);
        if (component.getComponentForm() != null) {
            component.revalidate();
        }
    }

    private static void appendLine(StringBuilder out, String value) {
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(value == null ? "" : value);
    }
}
