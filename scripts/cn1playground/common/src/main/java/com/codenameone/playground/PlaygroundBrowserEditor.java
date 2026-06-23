package com.codenameone.playground;

import com.codename1.io.CharArrayReader;
import com.codename1.io.JSONParser;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.CallbackAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Playground code/CSS editor.
 *
 * On the HTML5 (web) port it hosts the Monaco editor in a {@link BrowserComponent} exactly as before
 * (unchanged, so the web Playground is not regressed). On every other platform - the JavaSE simulator,
 * desktop, iOS and Android - it now uses the native {@link CodeEditor} component (syntax highlighting,
 * line numbers, themes, code completion driven by the same Codename One API metadata, and diagnostics)
 * instead of the previous bare {@link TextArea} fallback.
 */
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
    private final CodeEditor codeEditor;
    private final TextArea messages;
    private final PlaygroundCompletion completion;
    private TextArea fallbackEditor;
    private boolean usingFallback;
    private String pendingSource = "";
    private String pendingMarkersJson = "[]";
    private String pendingMessagesJson = "[]";
    private String pendingUiidsJson = "[]";
    private boolean pendingDarkMode;
    private boolean ready;
    private int readyVersion;

    PlaygroundBrowserEditor(Mode mode, String source, boolean darkMode, Listener listener) {
        this.mode = mode == null ? Mode.JAVA : mode;
        this.listener = listener;
        this.metadataJson = PlaygroundEditorMetadata.json();
        this.pendingSource = source == null ? "" : source;
        this.pendingDarkMode = darkMode;
        if (shouldUseBrowserEditor()) {
            browser = new BrowserComponent();
            browser.putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.FALSE);
            codeEditor = null;
            messages = null;
            completion = null;
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
            completion = new PlaygroundCompletion(
                    this.mode == Mode.CSS ? PlaygroundCompletion.Mode.CSS : PlaygroundCompletion.Mode.JAVA,
                    metadataJson, Collections.<String>emptyList());
            codeEditor = new CodeEditor();
            codeEditor.setLanguage(this.mode.monacoLanguage());
            codeEditor.setShowLineNumbers(true);
            codeEditor.setTheme(darkMode ? "dark" : "light");
            codeEditor.setText(pendingSource);
            codeEditor.setCompletionProvider(completion);
            codeEditor.addChangeListener(evt -> codeEditor.getText(text -> {
                pendingSource = text == null ? "" : text;
                listener.onSourceChanged(pendingSource, ++readyVersion);
            }));
            messages = new TextArea("", 4, 80, TextArea.ANY);
            messages.setEditable(false);
            messages.setSingleLineTextArea(false);
            messages.setUIID("Label");
            messages.setVisible(false);
            Container wrapper = new Container(new BorderLayout());
            wrapper.add(BorderLayout.CENTER, codeEditor);
            wrapper.add(BorderLayout.SOUTH, messages);
            component = wrapper;
            applyThemeToMessages(darkMode);
            // CodeEditor renders inside the platform web widget. On platforms/JDKs where that widget
            // cannot initialise (e.g. CEF unavailable in a desktop simulator) the editor never becomes
            // ready; in that case we transparently fall back to a plain TextArea so the Playground is
            // never left without a usable editor - guaranteeing no regression versus the old behavior.
            startFallbackWatchdog();
        }
    }

    private void startFallbackWatchdog() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                CN.callSerially(new Runnable() {
                    public void run() {
                        if (!usingFallback && (codeEditor == null || !codeEditor.isEditorReady())) {
                            activateFallback();
                        }
                    }
                });
            }
        });
        t.start();
    }

    private void activateFallback() {
        if (usingFallback || component == null) {
            return;
        }
        usingFallback = true;
        fallbackEditor = new TextArea(pendingSource, 20, 80, TextArea.ANY);
        fallbackEditor.setUIID("TextArea");
        fallbackEditor.setSingleLineTextArea(false);
        fallbackEditor.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        fallbackEditor.getAllStyles().setPadding(1.5f, 1.5f, 1.5f, 1.5f);
        fallbackEditor.addDataChangedListener((type, index) -> {
            pendingSource = fallbackEditor.getText();
            listener.onSourceChanged(pendingSource, ++readyVersion);
        });
        Container wrapper = (Container) component;
        if (codeEditor != null) {
            wrapper.removeComponent(codeEditor);
        }
        wrapper.add(BorderLayout.CENTER, fallbackEditor);
        applyThemeToFallback(pendingDarkMode);
        wrapper.revalidate();
    }

    private void applyThemeToFallback(boolean darkMode) {
        if (fallbackEditor == null) {
            return;
        }
        int bg = darkMode ? 0x0f172a : 0xffffff;
        int fg = darkMode ? 0xe2e8f0 : 0x0f172a;
        fallbackEditor.getAllStyles().setBgColor(bg);
        fallbackEditor.getAllStyles().setFgColor(fg);
        fallbackEditor.getAllStyles().setBgTransparency(255);
        fallbackEditor.repaint();
    }

    Component getComponent() {
        return component;
    }

    void setSource(String source) {
        pendingSource = source == null ? "" : source;
        if (browser == null) {
            if (usingFallback) {
                if (!pendingSource.equals(fallbackEditor.getText())) {
                    fallbackEditor.setText(pendingSource);
                }
            } else {
                codeEditor.setText(pendingSource);
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
            if (!usingFallback) {
                codeEditor.setDiagnostics(toCodeDiagnostics(diagnostics));
            }
            renderMessages(diagnostics, null);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setMarkers(" + pendingMarkersJson + ");");
        }
    }

    void setInlineMessages(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        pendingMessagesJson = toMessagesJson(inlineMessages);
        if (browser == null) {
            renderMessages(null, inlineMessages);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setInlineMessages(" + pendingMessagesJson + ");");
        }
    }

    void applyTheme(boolean darkMode) {
        pendingDarkMode = darkMode;
        if (browser == null) {
            if (usingFallback) {
                applyThemeToFallback(darkMode);
            } else {
                codeEditor.setTheme(darkMode ? "dark" : "light");
            }
            applyThemeToMessages(darkMode);
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.applyTheme(" + (darkMode ? "true" : "false") + ");");
        }
    }

    void setUiidCompletions(List<String> uiids) {
        pendingUiidsJson = toUiidJson(uiids);
        if (browser == null) {
            if (completion != null) {
                completion.setUiids(uiids);
            }
            return;
        }
        if (ready) {
            browser.execute("window.PlaygroundEditor && window.PlaygroundEditor.setUiids(" + pendingUiidsJson + ");");
        }
    }

    private List<CodeDiagnostic> toCodeDiagnostics(List<PlaygroundRunner.Diagnostic> diagnostics) {
        List<CodeDiagnostic> out = new ArrayList<CodeDiagnostic>();
        if (diagnostics == null) {
            return out;
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            PlaygroundRunner.Diagnostic d = diagnostics.get(i);
            int line = d.line > 0 ? d.line : 1;
            int col = d.column > 0 ? d.column : 1;
            int endLine = d.endLine > 0 ? d.endLine : line;
            int endCol = d.endColumn > 0 ? d.endColumn : col + 1;
            out.add(new CodeDiagnostic(line, col, endLine, endCol, d.message).setSeverity(mapSeverity(d.severity)));
        }
        return out;
    }

    private String mapSeverity(String severity) {
        if (severity == null) {
            return CodeDiagnostic.ERROR;
        }
        String s = severity.toLowerCase();
        if (s.indexOf("warn") >= 0) {
            return CodeDiagnostic.WARNING;
        }
        if (s.indexOf("info") >= 0 || s.indexOf("hint") >= 0) {
            return CodeDiagnostic.INFO;
        }
        return CodeDiagnostic.ERROR;
    }

    private void flush() {
        if (browser == null) {
            return;
        }
        // The BrowserComponent fires ready as soon as the iframe document loads,
        // which can be BEFORE editor.js has run and defined window.PlaygroundEditor
        // (loader.js + editor.js load asynchronously). A plain
        // "PlaygroundEditor && PlaygroundEditor.bootstrap(...)" then silently
        // no-ops and the editor never initialises -- and the iframe->host message
        // channel is one-way here, so we can't rely on the editor signalling back.
        // Inject a self-retrying bootstrap that waits inside the iframe until
        // PlaygroundEditor exists, so bootstrap runs regardless of load ordering.
        String bootstrapArgs = asJsString(metadataJson) + ", "
                + asJsString(pendingSource) + ", "
                + asJsString(mode.monacoLanguage()) + ", "
                + (pendingDarkMode ? "true" : "false") + ", "
                + pendingMarkersJson + ", "
                + pendingMessagesJson + ", "
                + pendingUiidsJson;
        browser.execute("(function(){var n=0;function go(){"
                + "if(window.PlaygroundEditor){window.PlaygroundEditor.bootstrap(" + bootstrapArgs + ");}"
                + "else if(n++<200){setTimeout(go,25);}}go();})();");
    }

    private boolean shouldUseBrowserEditor() {
        return "HTML5".equals(CN.getPlatformName());
    }

    private void applyThemeToMessages(boolean darkMode) {
        if (messages == null) {
            return;
        }
        int bg = darkMode ? 0x0f172a : 0xffffff;
        int fg = darkMode ? 0xe2e8f0 : 0x0f172a;
        messages.getAllStyles().setBgColor(bg);
        messages.getAllStyles().setFgColor(fg);
        messages.getAllStyles().setBgTransparency(255);
        messages.setVisible(messages.getText() != null && messages.getText().length() > 0);
        messages.repaint();
    }

    private void renderMessages(List<PlaygroundRunner.Diagnostic> diagnostics, List<PlaygroundRunner.InlineMessage> inlineMessages) {
        if (messages == null) {
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
        if (inlineMessages != null) {
            for (int i = 0; i < inlineMessages.size(); i++) {
                PlaygroundRunner.InlineMessage message = inlineMessages.get(i);
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(message.text);
            }
        }
        messages.setText(out.toString());
        messages.setVisible(out.length() > 0);
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
            // The editor re-signals "ready" every 400ms until it's bootstrapped, and
            // on the JS port a ready from any editor iframe reaches every editor's
            // handler. Only bootstrap once -- re-flushing re-runs setSource in the
            // editor and resets the caret / re-pushes stale text while the user types.
            if (ready) {
                return;
            }
            ready = true;
            flush();
            return;
        }
        if ("change".equals(type)) {
            // On the JS port onMessage is delivered to every editor's handler
            // (the port can't match a message to a specific iframe), so each
            // editor only acts on changes tagged with its own language.
            String language = asString(payload.get("language"));
            if (language.length() > 0 && !language.equals(mode.monacoLanguage())) {
                return;
            }
            String text = asString(payload.get("text"));
            int version = asInt(payload.get("version"));
            pendingSource = text == null ? "" : text;
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

    private String toMessagesJson(List<PlaygroundRunner.InlineMessage> inlineMessages) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int i = 0; i < inlineMessages.size(); i++) {
            PlaygroundRunner.InlineMessage message = inlineMessages.get(i);
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
