package com.codenameone.playground;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.CallbackAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PlaygroundBrowserEditor {
    interface Listener {
        void onSourceChanged(String source, int version);
    }

    enum Mode {
        JAVA("java"),
        CSS("css");

        private final String monacoLanguage;

        Mode(String monacoLanguage) {
            this.monacoLanguage = monacoLanguage;
        }

        String monacoLanguage() {
            return monacoLanguage;
        }
    }

    private final Component component;
    private final Listener listener;
    private final String metadataJson;
    private final Mode mode;
    private final BrowserComponent browser;
    private final TextArea fallbackEditor;
    private final TextArea fallbackMessages;
    private String pendingSource = "";
    private String pendingMarkersJson = "[]";
    private String pendingMessagesJson = "[]";
    private String pendingUiidsJson = "[]";
    private boolean pendingDarkMode;
    private boolean ready;

    PlaygroundBrowserEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        this.mode = mode == null ? Mode.JAVA : mode;
        this.listener = listener;
        this.metadataJson = PlaygroundEditorMetadata.json();
        this.pendingSource = source == null ? "" : source;
        this.pendingDarkMode = darkMode;
        if (shouldUseBrowserEditor()) {
            browser = new BrowserComponent();
            browser.putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.FALSE);
            fallbackEditor = null;
            fallbackMessages = null;
            component = browser;
            browser.addWebEventListener(BrowserComponent.onMessage, this::handleMessage);
            try {
                browser.setURLHierarchy("/playground-editor/index.html");
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load playground editor resources", ex);
            }
            browser.ready(new CallbackAdapter<BrowserComponent>() {
                @Override
                public void onSucess(BrowserComponent value) {
                    flush();
                }
            });
        } else {
            browser = null;
            fallbackEditor = new TextArea(pendingSource, 20, 80, TextArea.ANY);
            fallbackEditor.setUIID("TextArea");
            fallbackEditor.setEditable(true);
            fallbackEditor.setSingleLineTextArea(false);
            fallbackEditor.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            fallbackEditor.getAllStyles().setPadding(1.5f, 1.5f, 1.5f, 1.5f);
            fallbackEditor.addDataChangedListener((type, index) -> {
                pendingSource = fallbackEditor.getText();
                listener.onSourceChanged(pendingSource, ++readyVersion);
            });
            fallbackMessages = new TextArea("", 4, 80, TextArea.ANY);
            fallbackMessages.setEditable(false);
            fallbackMessages.setSingleLineTextArea(false);
            fallbackMessages.setUIID("Label");
            Container wrapper = new Container(new BorderLayout());
            wrapper.add(BorderLayout.CENTER, fallbackEditor);
            wrapper.add(BorderLayout.SOUTH, fallbackMessages);
            component = wrapper;
            applyThemeToFallback(darkMode);
        }
    }

    private int readyVersion;

    Component getComponent() {
        return component;
    }

    void setSource(String source) {
        pendingSource = source == null ? "" : source;
        if (browser == null) {
            if (!pendingSource.equals(fallbackEditor.getText())) {
                fallbackEditor.setText(pendingSource);
            }
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setSource(" + asJsString(pendingSource) + ");");
        }
    }

    void setMarkers(List<PlaygroundRunner.Diagnostic> diagnostics) {
        pendingMarkersJson = toMarkersJson(diagnostics);
        if (browser == null) {
            renderFallbackMessages(diagnostics, null);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setMarkers(" + pendingMarkersJson + ");");
        }
    }

    void setInlineMessages(List<PlaygroundRunner.InlineMessage> messages) {
        pendingMessagesJson = toMessagesJson(messages);
        if (browser == null) {
            renderFallbackMessages(null, messages);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setInlineMessages(" + pendingMessagesJson + ");");
        }
    }

    void applyTheme(boolean darkMode) {
        pendingDarkMode = darkMode;
        if (browser == null) {
            applyThemeToFallback(darkMode);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.applyTheme(" + (darkMode ? "true" : "false") + ");");
        }
    }

    void setUiidCompletions(List<String> uiids) {
        pendingUiidsJson = toUiidJson(uiids);
        if (browser != null && ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setUiids(" + pendingUiidsJson + ");");
        }
    }

    private void flush() {
        if (browser == null) {
            return;
        }
        browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.bootstrap("
                + asJsString(metadataJson) + ", "
                + asJsString(pendingSource) + ", "
                + asJsString(mode.monacoLanguage()) + ", "
                + (pendingDarkMode ? "true" : "false") + ", "
                + pendingMarkersJson + ", "
                + pendingMessagesJson + ", "
                + pendingUiidsJson + ");");
    }

    private boolean shouldUseBrowserEditor() {
        return "HTML5".equals(CN.getPlatformName());
    }

    private void applyThemeToFallback(boolean darkMode) {
        if (fallbackEditor == null || fallbackMessages == null) {
            return;
        }
        int bg = darkMode ? 0x0f172a : 0xffffff;
        int fg = darkMode ? 0xe2e8f0 : 0x0f172a;
        fallbackEditor.getAllStyles().setBgColor(bg);
        fallbackEditor.getAllStyles().setFgColor(fg);
        fallbackEditor.getAllStyles().setBgTransparency(255);
        fallbackMessages.getAllStyles().setBgColor(bg);
        fallbackMessages.getAllStyles().setFgColor(fg);
        fallbackMessages.getAllStyles().setBgTransparency(255);
        fallbackMessages.setVisible(fallbackMessages.getText() != null && fallbackMessages.getText().length() > 0);
        fallbackEditor.repaint();
        fallbackMessages.repaint();
    }

    private void renderFallbackMessages(List<PlaygroundRunner.Diagnostic> diagnostics, List<PlaygroundRunner.InlineMessage> messages) {
        if (fallbackMessages == null) {
            return;
        }
        StringBuilder out = new StringBuilder();
        if (diagnostics != null) {
            for (int i = 0; i < diagnostics.size(); i++) {
                PlaygroundRunner.Diagnostic diagnostic = diagnostics.get(i);
                if (out.length() > 0) {
                    out.append('\n');
                }
                if (diagnostic.line > 0) {
                    out.append("Line ").append(diagnostic.line).append(": ");
                }
                out.append(diagnostic.message);
            }
        }
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                PlaygroundRunner.InlineMessage message = messages.get(i);
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(message.text);
            }
        }
        fallbackMessages.setText(out.toString());
        fallbackMessages.setVisible(out.length() > 0);
        ((Container) component).revalidate();
    }

    private void handleMessage(ActionEvent event) {
        Object source = event.getSource();
        if (!(source instanceof String)) {
            return;
        }
        Map<String, Object> payload = parseMessage((String) source);
        if (payload.isEmpty()) {
            return;
        }
        String type = asString(payload.get("type"));
        if ("ready".equals(type)) {
            ready = true;
            flush();
            return;
        }
        if ("change".equals(type)) {
            String text = asString(payload.get("text"));
            int version = asInt(payload.get("version"));
            listener.onSourceChanged(text, version);
        }
    }

    private Map<String, Object> parseMessage(String message) {
        try {
            return new JSONParser().parseJSON(new CharArrayReader(message.toCharArray()));
        } catch (IOException ex) {
            return Collections.emptyMap();
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String toMarkersJson(List<PlaygroundRunner.Diagnostic> diagnostics) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int i = 0; i < diagnostics.size(); i++) {
            PlaygroundRunner.Diagnostic diagnostic = diagnostics.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append('{');
            appendJsonField(out, "message", diagnostic.message);
            out.append(',');
            appendJsonField(out, "severity", diagnostic.severity);
            out.append(',');
            appendJsonField(out, "line", diagnostic.line);
            out.append(',');
            appendJsonField(out, "column", diagnostic.column);
            out.append(',');
            appendJsonField(out, "endLine", diagnostic.endLine);
            out.append(',');
            appendJsonField(out, "endColumn", diagnostic.endColumn);
            out.append('}');
        }
        out.append(']');
        return out.toString();
    }

    private String toMessagesJson(List<PlaygroundRunner.InlineMessage> messages) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int i = 0; i < messages.size(); i++) {
            PlaygroundRunner.InlineMessage message = messages.get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append('{');
            appendJsonField(out, "kind", message.kind);
            out.append(',');
            appendJsonField(out, "text", message.text);
            out.append(',');
            appendJsonField(out, "line", message.line);
            out.append('}');
        }
        out.append(']');
        return out.toString();
    }

    private String toUiidJson(List<String> uiids) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        if (uiids != null) {
            int j = 0;
            for (int i = 0; i < uiids.size(); i++) {
                String uiid = uiids.get(i);
                if (uiid == null || uiid.trim().length() == 0) {
                    continue;
                }
                if (j++ > 0) {
                    out.append(',');
                }
                appendJsonString(out, uiid.trim());
            }
        }
        out.append(']');
        return out.toString();
    }

    private void appendJsonField(StringBuilder out, String name, String value) {
        appendJsonString(out, name);
        out.append(':');
        appendJsonString(out, value == null ? "" : value);
    }

    private void appendJsonField(StringBuilder out, String name, int value) {
        appendJsonString(out, name);
        out.append(':').append(value);
    }

    private void appendJsonString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        String hex = Integer.toHexString(ch);
                        out.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
                    break;
            }
        }
        out.append('"');
    }

    private String asJsString(String value) {
        StringBuilder out = new StringBuilder();
        appendJsonString(out, value == null ? "" : value);
        return out.toString();
    }
}
