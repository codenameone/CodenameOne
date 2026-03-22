package com.codenameone.playground;

import com.codename1.io.Util;
import com.codename1.components.MultiButton;
import com.codename1.components.SplitPane;
import com.codename1.system.Lifecycle;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CN1Playground extends Lifecycle {
    private static final String THEME_ROLE = "playgroundThemeRole";

    private final PlaygroundRunner runner = new PlaygroundRunner();
    private Form appForm;
    private PlaygroundBrowserEditor editor;
    private Container previewRoot;
    private Container historyMenu;
    private Resources theme;
    private boolean websiteDarkMode;
    private String currentScript;
    private List<PlaygroundRunner.InlineMessage> currentMessages = new ArrayList<PlaygroundRunner.InlineMessage>();
    private int editSequence;
    private int autoRunSequence;

    @Override
    public void runApp() {
        com.codename1.ui.CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        theme = Resources.getGlobalResources();
        currentScript = resolveInitialScript();

        Form form = new Form("CN1 Playground", new BorderLayout());
        appForm = form;
        Toolbar toolbar = form.getToolbar();
        toolbar.setTitleCentered(false);
        setThemeRole(form.getContentPane(), "form");
        setThemeRole(form.getTitleArea(), "header");
        setThemeRole(toolbar.getTitleComponent(), "headerTitle");

        editor = new PlaygroundBrowserEditor(currentScript, websiteDarkMode, this::handleSourceChanged);
        previewRoot = createPreviewRoot();
        historyMenu = new Container(BoxLayout.y());

        Container editorPanel = new Container(new BorderLayout());
        setThemeRole(editorPanel, "panel");
        editorPanel.add(BorderLayout.CENTER, editor.getComponent());

        Container previewPanel = new Container(new BorderLayout());
        setThemeRole(previewPanel, "panel");
        previewPanel.add(BorderLayout.CENTER, previewRoot);

        Component content = createMainContent(editorPanel, previewPanel);
        setThemeRole(content, "content");
        form.add(BorderLayout.CENTER, content);

        installSideMenu(toolbar, form);
        runScript(form);
        initWebsiteThemeSync(form);
        form.show();
        notifyWebsiteUiReady();
    }

    private void handleSourceChanged(String source, int version) {
        currentScript = source == null ? "" : source;
        persistCurrentState();
        scheduleHistorySnapshot();
        scheduleAutoRun();
    }

    private Container createPreviewRoot() {
        Container root = new Container(new BorderLayout());
        root.setScrollableY(true);
        root.getAllStyles().setBgColor(0xffffff);
        root.getAllStyles().setBgTransparency(255);
        root.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        root.getAllStyles().setPadding(2, 2, 2, 2);
        setThemeRole(root, "preview");
        return root;
    }

    private Component createMainContent(Container editorPanel, Container previewPanel) {
        if (com.codename1.ui.CN.getDisplayWidth() >= 900) {
            return new SplitPane(SplitPane.HORIZONTAL_SPLIT, editorPanel, previewPanel, "45%", "25%", "75%");
        }
        Container stacked = new Container(new GridLayout(2, 1));
        stacked.addAll(editorPanel, previewPanel);
        return stacked;
    }

    private void runScript(Form form) {
        com.codename1.ui.CN.callSerially(() -> executeRunScript(form));
    }

    private void executeRunScript(Form form) {
        List<PlaygroundStateStore.HistoryEntry> history = PlaygroundStateStore.pushHistory(currentScript);
        refreshHistoryMenu(form.getToolbar(), history);
        previewRoot.removeAll();

        List<PlaygroundRunner.InlineMessage> loggedMessages = new ArrayList<PlaygroundRunner.InlineMessage>();
        PlaygroundContext context = new PlaygroundContext(form, previewRoot, theme,
                message -> loggedMessages.add(new PlaygroundRunner.InlineMessage(0, message, "info")));
        PlaygroundRunner.RunResult result = runner.run(currentScript, context);

        currentMessages = new ArrayList<PlaygroundRunner.InlineMessage>(loggedMessages);
        currentMessages.addAll(result.getMessages());

        replacePreview(result.getComponent());
        editor.setMarkers(result.getDiagnostics());
        editor.setInlineMessages(currentMessages);
        persistCurrentState();
    }

    private void replacePreview(Component component) {
        previewRoot.removeAll();
        if (component == null) {
            previewRoot.revalidate();
            return;
        }
        detachForPreview(component);
        previewRoot.add(BorderLayout.CENTER, component);
        previewRoot.revalidate();
    }

    private void detachForPreview(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.removeComponent(component);
        }
    }

    private void installSideMenu(Toolbar toolbar, Form form) {
        toolbar.addComponentToSideMenu(new PlaygroundMenuSection("Share"));
        toolbar.addComponentToSideMenu(createSideMenuButton("Copy Current URL", () -> {
            copyCurrentSourceUrl();
            toolbar.closeSideMenu();
        }));
        toolbar.addComponentToSideMenu(new PlaygroundMenuSection("Samples"));
        for (PlaygroundExamples.Sample sample : PlaygroundExamples.SAMPLES) {
            toolbar.addComponentToSideMenu(createSideMenuButton(sample.title, () -> {
                setScript(sample.script, true);
                toolbar.closeSideMenu();
            }));
        }
        toolbar.addComponentToSideMenu(new PlaygroundMenuSection("History"));
        toolbar.addComponentToSideMenu(historyMenu);
        refreshHistoryMenu(toolbar, PlaygroundStateStore.loadHistory());
    }

    private void refreshHistoryMenu(Toolbar toolbar, List<PlaygroundStateStore.HistoryEntry> history) {
        historyMenu.removeAll();
        if (history.isEmpty()) {
            Label empty = new Label("No saved runs yet");
            empty.setUIID("PlaygroundMenuEmpty");
            historyMenu.add(empty);
        } else {
            for (int i = 0; i < history.size(); i++) {
                PlaygroundStateStore.HistoryEntry entry = history.get(i);
                historyMenu.add(createHistoryButton(entry, history, toolbar));
            }
        }
        historyMenu.revalidate();
    }

    private MultiButton createHistoryButton(PlaygroundStateStore.HistoryEntry entry,
            List<PlaygroundStateStore.HistoryEntry> history, Toolbar toolbar) {
        MultiButton button = new MultiButton(entry.title());
        button.setTextLine2(entry.detail(history));
        button.setUIID("SideCommand");
        button.addActionListener(e -> {
            setScript(entry.script, true);
            toolbar.closeSideMenu();
        });
        return button;
    }

    private Button createSideMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setUIID("SideCommand");
        setThemeRole(button, "sideCommand");
        button.addActionListener(e -> action.run());
        return button;
    }

    private String resolveInitialScript() {
        String sharedScript = scriptFromUrl();
        if (sharedScript != null) {
            PlaygroundStateStore.saveCurrentState(sharedScript, PlaygroundStateStore.loadCurrentOutput());
            return sharedScript;
        }
        return PlaygroundStateStore.loadCurrentScript();
    }

    private String scriptFromUrl() {
        String href = com.codename1.ui.CN.getProperty("browser.window.location.href", null);
        if (href == null || href.length() == 0) {
            return null;
        }
        String code = queryParam(href, "code");
        if (code != null && code.length() > 0) {
            String decoded = decodeSharedScript(code);
            if (decoded != null && decoded.trim().length() > 0) {
                return decoded;
            }
        }
        String sample = queryParam(href, "sample");
        PlaygroundExamples.Sample found = PlaygroundExamples.findBySlug(sample);
        return found == null ? null : found.script;
    }

    private String queryParam(String href, String name) {
        int queryStart = href.indexOf('?');
        if (queryStart < 0 || queryStart == href.length() - 1) {
            return null;
        }
        String query = href.substring(queryStart + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) {
            query = query.substring(0, hash);
        }
        String prefix = name + "=";
        String[] pairs = Util.split(query, "&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            if (pair.startsWith(prefix)) {
                return pair.substring(prefix.length());
            }
        }
        return null;
    }

    private String decodeSharedScript(String encoded) {
        try {
            String normalized = encoded.replace('-', '+').replace('_', '/');
            int pad = normalized.length() % 4;
            if (pad > 0) {
                normalized = normalized + "====".substring(pad);
            }
            byte[] data = Base64.decode(normalized.getBytes("UTF-8"));
            return new String(data, "UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    private void copyCurrentSourceUrl() {
        String base = com.codename1.ui.CN.getProperty("browser.window.location.href", null);
        if (base == null || base.length() == 0) {
            return;
        }
        int query = base.indexOf('?');
        if (query >= 0) {
            base = base.substring(0, query);
        }
        int hash = base.indexOf('#');
        if (hash >= 0) {
            base = base.substring(0, hash);
        }
        String encoded = encodeSharedScript(currentScript);
        if (encoded.length() == 0) {
            return;
        }
        com.codename1.ui.Display.getInstance().copyToClipboard(base + "?code=" + encoded);
    }

    private String encodeSharedScript(String script) {
        if (script == null || script.length() == 0) {
            return "";
        }
        try {
            String encoded = Base64.encodeNoNewline(script.getBytes("UTF-8"))
                    .replace('+', '-')
                    .replace('/', '_');
            while (encoded.endsWith("=")) {
                encoded = encoded.substring(0, encoded.length() - 1);
            }
            return encoded;
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

    private void setScript(String script, boolean runNow) {
        currentScript = script == null ? "" : script;
        if (editor != null) {
            editor.setSource(currentScript);
        }
        persistCurrentState();
        if (runNow && appForm != null) {
            runScript(appForm);
        }
    }

    private void persistCurrentState() {
        PlaygroundStateStore.saveCurrentState(currentScript, joinMessages(currentMessages));
    }

    private String joinMessages(List<PlaygroundRunner.InlineMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            PlaygroundRunner.InlineMessage message = messages.get(i);
            if (i > 0) {
                out.append('\n');
            }
            out.append(message.text);
        }
        return out.toString();
    }

    private void scheduleHistorySnapshot() {
        if (appForm == null) {
            return;
        }
        final int snapshot = ++editSequence;
        UITimer.timer(1200, false, appForm, () -> {
            if (snapshot != editSequence) {
                return;
            }
            refreshHistoryMenu(appForm.getToolbar(), PlaygroundStateStore.pushHistory(currentScript));
        });
    }

    private void scheduleAutoRun() {
        if (appForm == null) {
            return;
        }
        final int runTicket = ++autoRunSequence;
        UITimer.timer(850, false, appForm, () -> {
            if (runTicket != autoRunSequence) {
                return;
            }
            runScript(appForm);
        });
    }

    private void initWebsiteThemeSync(Form form) {
        refreshWebsiteTheme(form);
        UITimer.timer(900, true, form, () -> refreshWebsiteTheme(form));
    }

    private void notifyWebsiteUiReady() {
        BrowserComponent js = com.codename1.ui.CN.getSharedJavascriptContext();
        if (js == null) {
            return;
        }
        js.execute(
                "callback.onSuccess((function(){"
                        + "try {"
                        + "if (window.parent && window.parent !== window && window.parent.postMessage) {"
                        + "window.parent.postMessage({ type: 'cn1-playground-ui-ready' }, '*');"
                        + "}"
                        + "} catch (e) {}"
                        + "return true;"
                        + "})())",
                res -> {
                });
    }

    private void refreshWebsiteTheme(Form form) {
        BrowserComponent js = com.codename1.ui.CN.getSharedJavascriptContext();
        if (js == null) {
            return;
        }
        js.execute(
                "callback.onSuccess((function(){"
                        + "var dark = false;"
                        + "var explicit = false;"
                        + "try {"
                        + "var parentDoc = (window.parent && window.parent.document) ? window.parent.document : null;"
                        + "if (parentDoc && parentDoc.body && parentDoc.body.classList) {"
                        + "dark = parentDoc.body.classList.contains('dark') || parentDoc.body.classList.contains('cn1-initializr-dark');"
                        + "}"
                        + "if (!dark && window.parent && window.parent.localStorage) {"
                        + "var pref = window.parent.localStorage.getItem('pref-theme');"
                        + "if (pref === 'dark') { dark = true; explicit = true; }"
                        + "else if (pref === 'light') { dark = false; explicit = true; }"
                        + "}"
                        + "} catch (e) {}"
                        + "if (!explicit && !dark && window.matchMedia) {"
                        + "dark = window.matchMedia('(prefers-color-scheme: dark)').matches;"
                        + "}"
                        + "return dark ? 'true' : 'false';"
                        + "})())",
                res -> {
                    boolean dark = "true".equals(String.valueOf(res));
                    if (dark != websiteDarkMode) {
                        websiteDarkMode = dark;
                        applyWebsiteTheme(form, dark);
                        if (editor != null) {
                            editor.applyTheme(dark);
                        }
                        form.refreshTheme();
                    }
                });
    }

    private void applyWebsiteTheme(Component component, boolean dark) {
        applyRoleStyle(component, getThemeRole(component), dark);
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyWebsiteTheme(cnt.getComponentAt(i), dark);
            }
        }
        applyGlobalThemeStyles(dark);
    }

    private void applyRoleStyle(Component component, String role, boolean dark) {
        if (role == null) {
            return;
        }
        Style style = component.getAllStyles();
        style.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        switch (role) {
            case "form":
            case "content":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x111827 : 0xf5f7fb);
                break;
            case "panel":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x1f2937 : 0xffffff);
                style.setMarginUnit(Style.UNIT_TYPE_DIPS);
                style.setMargin(1, 1, 1, 1);
                style.setPadding(1, 1, 1, 1);
                break;
            case "preview":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x0f172a : 0xffffff);
                style.setPadding(2, 2, 2, 2);
                break;
            case "header":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x1f2937 : 0xe5e7eb);
                style.setPadding(1, 1, 2, 2);
                break;
            case "headerTitle":
                style.setFgColor(dark ? 0xf8fafc : 0x111827);
                break;
            case "sideCommand":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x111827 : 0xffffff);
                style.setFgColor(dark ? 0xe5e7eb : 0x111827);
                break;
            default:
                break;
        }
    }

    private void applyGlobalThemeStyles(boolean dark) {
        tintUiid("TitleArea", dark ? 0x111827 : 0xe5e7eb, 0, true);
        tintUiid("Title", dark ? 0x111827 : 0xe5e7eb, dark ? 0xf8fafc : 0x111827, true);
        tintUiid("SideNavigationPanel", dark ? 0x0f172a : 0xffffff, dark ? 0xe5e7eb : 0x111827, true);
        tintUiid("SideCommand", dark ? 0x0f172a : 0xffffff, dark ? 0xe5e7eb : 0x111827, false);
        tintUiid("PlaygroundMenuSection", dark ? 0x0f172a : 0xffffff, 0, true);
        tintUiid("PlaygroundMenuSectionTitle", dark ? 0x94a3b8 : 0x6b7280, dark ? 0x94a3b8 : 0x6b7280, false);
        tintUiid("PlaygroundMenuEmpty", dark ? 0x0f172a : 0xffffff, dark ? 0x94a3b8 : 0x6b7280, false);
    }

    private void tintUiid(String uiid, int bgColor, int fgColor, boolean updateBackground) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        if (style == null) {
            return;
        }
        if (updateBackground) {
            style.setBgTransparency(255);
            style.setBgColor(bgColor);
        }
        if (fgColor != 0) {
            style.setFgColor(fgColor);
        }
    }

    private void setThemeRole(Component component, String role) {
        component.putClientProperty(THEME_ROLE, role);
    }

    private String getThemeRole(Component component) {
        Object role = component.getClientProperty(THEME_ROLE);
        return role instanceof String ? (String) role : null;
    }
}
