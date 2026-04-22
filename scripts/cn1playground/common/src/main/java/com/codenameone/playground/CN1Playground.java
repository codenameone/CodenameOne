package com.codenameone.playground;

import com.codename1.components.MultiButton;
import com.codename1.components.SplitPane;
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Util;
import com.codename1.system.Lifecycle;
import com.codename1.system.NativeLookup;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import com.codename1.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class CN1Playground extends Lifecycle {
    private static final boolean DEFAULT_DARK_MODE = true;
    private static final String THEME_ROLE = "playgroundThemeRole";
    private static final String ROLE_EMBEDDED_FORM = "embeddedForm";
    private static final String ROLE_EMBEDDED_TITLE_AREA = "embeddedTitleArea";
    private static final String SHARE_BUTTON_LABEL = "Copy Shareable Playground URL";
    private final PlaygroundRunner runner = new PlaygroundRunner();
    private final PlaygroundProjectExporter projectExporter = new PlaygroundProjectExporter();

    private Form appForm;
    private PlaygroundBrowserEditor editor;
    private PlaygroundBrowserEditor cssEditor;
    private PlaygroundInspector inspector;
    private Container previewRoot;
    private Container historyMenu;
    private final List<Component> sideMenuComponents = new ArrayList<>();
    private Resources theme;
    private boolean websiteDarkMode = DEFAULT_DARK_MODE;
    private String currentScript;
    private String currentCss;
    private List<PlaygroundRunner.InlineMessage> currentMessages = new ArrayList<>();
    private List<PlaygroundRunner.InlineMessage> currentCssMessages = new ArrayList<>();
    private int editSequence;
    private int autoRunSequence;
    private Tabs editorTabs;
    private WebsiteThemeNative websiteThemeNative;
    private boolean websiteThemeInitialized;

    @Override
    public void runApp() {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        theme = Resources.getGlobalResources();
        currentScript = resolveInitialScript();
        currentCss = resolveInitialCss();

        appForm = new Form("Playground", new BorderLayout());
        appForm.setUIID("PlaygroundForm");

        Toolbar toolbar = appForm.getToolbar();
        toolbar.setUIID("PlaygroundToolbar");
        toolbar.setTitleCentered(false);
        if (toolbar.getTitleComponent() != null) {
            toolbar.getTitleComponent().setUIID("PlaygroundTitle");
        }

        toolbar.addMaterialCommandToRightBar("Download", com.codename1.ui.FontImage.MATERIAL_DOWNLOAD, e -> projectExporter.export(currentScript, currentCss));

        editor = new PlaygroundBrowserEditor(PlaygroundBrowserEditor.Mode.JAVA, currentScript, websiteDarkMode, this::handleSourceChanged);
        cssEditor = new PlaygroundBrowserEditor(PlaygroundBrowserEditor.Mode.CSS, currentCss, websiteDarkMode, this::handleCssChanged);
        inspector = new PlaygroundInspector(websiteDarkMode, (component, property, value) -> handlePropertyChanged(component));

        previewRoot = createPreviewRoot();
        historyMenu = new Container(BoxLayout.y());
        historyMenu.setUIID("PlaygroundMenuContainer");

        Container editorPanel = wrapPanel(editor.getComponent());
        Container cssEditorPanel = wrapPanel(cssEditor.getComponent());
        Container inspectorPanel = wrapPanel(inspector.getComponent());

        editorTabs = new Tabs();
        editorTabs.setUIID("PlaygroundEditorTabs");
        editorTabs.addTab("Code", editorPanel);
        editorTabs.addTab("CSS", cssEditorPanel);
        editorTabs.addTab("Inspector", inspectorPanel);
        applyTabsTheme(websiteDarkMode);

        Container previewPanel = wrapPanel(previewRoot);

        appForm.add(BorderLayout.CENTER, createMainContent(editorTabs, previewPanel));

        installSideMenu(toolbar);
        applyWebsiteTheme(appForm, websiteDarkMode);

        runScript(appForm);
        initWebsiteThemeSync(appForm);

        appForm.show();
        notifyWebsiteUiReady();
    }

    @Override
    protected void handleNetworkError(NetworkEvent err) {
        Log.p("Networking error: " + err);
    }

    private Container wrapPanel(Component content) {
        Container panel = new Container(new BorderLayout());
        panel.setUIID("PlaygroundPanel");
        panel.add(BorderLayout.CENTER, content);
        return panel;
    }

    private void handlePropertyChanged(Component component) {
        if (component == null) {
            return;
        }
        Container parent = component.getParent();
        if (parent != null) {
            parent.revalidate();
        } else {
            component.repaint();
        }
        persistCurrentState();
    }

    private void handleSourceChanged(String source, int version) {
        currentScript = source == null ? "" : source;
        persistCurrentState();
        scheduleHistorySnapshot();
        scheduleAutoRun();
    }

    private void handleCssChanged(String source, int version) {
        currentCss = source == null ? "" : source;
        persistCurrentState();
        applyCurrentCss();
    }

    private Container createPreviewRoot() {
        Container root = new Container(new BorderLayout());
        root.setScrollableY(true);
        root.setUIID("PlaygroundPreview");
        return root;
    }

    private Component createMainContent(Tabs tabs, Container previewPanel) {
        tabs.setSwipeActivated(false);
        if (!Display.getInstance().isPortrait()) {
            return new SplitPane(SplitPane.HORIZONTAL_SPLIT, tabs, previewPanel, "25%", "50%", "75%");
        }
        tabs.addTab("Preview", previewPanel);
        return tabs;
    }

    private void runScript(Form form) {
        UITimer.timer(1, false, form, () -> executeRunScript(form));
    }

    private void executeRunScript(Form form) {
        CN.callSerially(() -> {
            List<PlaygroundStateStore.HistoryEntry> history = PlaygroundStateStore.pushHistory(currentScript);
            refreshHistoryMenu(form.getToolbar(), history);

            List<PlaygroundRunner.InlineMessage> loggedMessages = new ArrayList<>();
            PlaygroundContext context = new PlaygroundContext(
                    form,
                    previewRoot,
                    theme,
                    message -> loggedMessages.add(new PlaygroundRunner.InlineMessage(0, message, "info"))
            );

            PlaygroundRunner.RunResult result = runner.run(currentScript, context);

            currentMessages = new ArrayList<>(loggedMessages);
            currentMessages.addAll(result.getMessages());

            replacePreview(result.getComponent());
            editor.setMarkers(result.getDiagnostics());
            editor.setInlineMessages(currentMessages);
            editor.setUiidCompletions(PlaygroundCssSupport.collectVisibleUiids(previewRoot));
            applyCurrentCss();
            persistCurrentState();
        });
    }

    private void replacePreview(Component component) {
        previewRoot.removeAll();

        if (component == null) {
            inspector.setPreviewRoot(null);
            previewRoot.revalidate();
            return;
        }

        detachForPreview(component);
        markEmbeddedPreviewRoles(component);
        applyWebsiteTheme(component, websiteDarkMode);

        previewRoot.add(BorderLayout.CENTER, component);
        inspector.setPreviewRoot(previewRoot);
        previewRoot.revalidate();
    }

    private void detachForPreview(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.removeComponent(component);
        }
    }

    /**
     * Small special-case hook:
     * when a Form or its title area is rendered inside the preview,
     * keep its semantic appearance without introducing a whole second theme system.
     */
    private void markEmbeddedPreviewRoles(Component component) {
        if (component instanceof Form) {
            component.putClientProperty(THEME_ROLE, ROLE_EMBEDDED_FORM);
            Form f = (Form) component;
            if (f.getTitleArea() != null) {
                f.getTitleArea().putClientProperty(THEME_ROLE, ROLE_EMBEDDED_TITLE_AREA);
            }
        }

        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                markEmbeddedPreviewRoles(cnt.getComponentAt(i));
            }
        }
    }

    private void installSideMenu(Toolbar toolbar) {
        Toolbar.setEnableSideMenuSwipe(false);
        PlaygroundMenuSection shareSection = new PlaygroundMenuSection("Share");
        addSideMenuComponent(toolbar, shareSection);
        addSideMenuComponent(toolbar, createSideMenuButton(SHARE_BUTTON_LABEL, () -> {
            copyCurrentSourceUrl();
            toolbar.closeSideMenu();
        }));

        PlaygroundMenuSection samplesSection = new PlaygroundMenuSection("Samples");
        addSideMenuComponent(toolbar, samplesSection);
        for (PlaygroundExamples.Sample sample : PlaygroundExamples.SAMPLES) {
            addSideMenuComponent(toolbar, createSideMenuButton(sample.title, () -> {
                setScript(sample.script, true);
                toolbar.closeSideMenu();
            }));
        }

        PlaygroundMenuSection historySection = new PlaygroundMenuSection("History");
        addSideMenuComponent(toolbar, historySection);
        addSideMenuComponent(toolbar, historyMenu);

        refreshHistoryMenu(toolbar, PlaygroundStateStore.loadHistory());
    }

    private void addSideMenuComponent(Toolbar toolbar, Component component) {
        applyWebsiteTheme(component, websiteDarkMode);
        sideMenuComponents.add(component);
        toolbar.addComponentToSideMenu(component);
    }

    private void refreshHistoryMenu(Toolbar toolbar, List<PlaygroundStateStore.HistoryEntry> history) {
        historyMenu.removeAll();

        if (history.isEmpty()) {
            Label empty = new Label("No saved runs yet");
            empty.setUIID("PlaygroundMenuEmpty");
            historyMenu.add(empty);
        } else {
            for (PlaygroundStateStore.HistoryEntry entry : history) {
                historyMenu.add(createHistoryButton(entry, history, toolbar));
            }
        }

        applyWebsiteTheme(historyMenu, websiteDarkMode);
        historyMenu.revalidate();
    }

    private MultiButton createHistoryButton(PlaygroundStateStore.HistoryEntry entry,
                                            List<PlaygroundStateStore.HistoryEntry> history,
                                            Toolbar toolbar) {
        MultiButton button = new MultiButton(entry.title());
        button.setTextLine2(entry.detail(history));
        button.setUIID("PlaygroundSideCommand");
        button.setUIIDLine1("PlaygroundSideCommandLine1");
        button.setUIIDLine2("PlaygroundSideCommandLine2");
        button.addActionListener(e -> {
            setScript(entry.script, true);
            toolbar.closeSideMenu();
        });
        applyWebsiteTheme(button, websiteDarkMode);
        return button;
    }

    private Button createSideMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setUIID("PlaygroundSideCommand");
        button.addActionListener(e -> action.run());
        applyWebsiteTheme(button, websiteDarkMode);
        return button;
    }

    private String resolveInitialScript() {
        String sharedScript = scriptFromUrl();
        if (sharedScript != null) {
            PlaygroundStateStore.saveCurrentState(sharedScript, resolveInitialCss(), PlaygroundStateStore.loadCurrentOutput());
            return sharedScript;
        }
        return PlaygroundStateStore.loadCurrentScript();
    }

    private String resolveInitialCss() {
        String sharedCss = cssFromUrl();
        return sharedCss == null ? PlaygroundStateStore.loadCurrentCss() : sharedCss;
    }

    private String scriptFromUrl() {
        String href = CN.getProperty("browser.window.location.href", null);
        if (href == null || href.isEmpty()) {
            return null;
        }

        String code = queryParam(href, "code");
        if (code != null && !code.isEmpty()) {
            String decoded = decodeSharedScript(code);
            if (decoded != null && !decoded.trim().isEmpty()) {
                return decoded;
            }
        }

        String sample = queryParam(href, "sample");
        PlaygroundExamples.Sample found = PlaygroundExamples.findBySlug(sample);
        return found == null ? null : found.script;
    }

    private String cssFromUrl() {
        String href = CN.getProperty("browser.window.location.href", null);
        if (href == null || href.isEmpty()) {
            return null;
        }

        String css = queryParam(href, "css");
        if (css != null && !css.isEmpty()) {
            String decoded = decodeSharedScript(css);
            if (decoded != null && !decoded.trim().isEmpty()) {
                return decoded;
            }
        }
        return null;
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
        for (String pair : pairs) {
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
        String base = CN.getProperty("browser.window.location.href", null);
        if (base == null || base.isEmpty()) {
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
        if (encoded.isEmpty()) {
            return;
        }

        StringBuilder shareUrl = new StringBuilder(base).append("?code=").append(encoded);
        if (currentCss != null && !currentCss.isEmpty()) {
            String encodedCss = encodeSharedScript(currentCss);
            shareUrl.append("&css=").append(encodedCss);
        }

        Display.getInstance().copyToClipboard(shareUrl.toString());
    }

    private String encodeSharedScript(String script) {
        if (script == null || script.isEmpty()) {
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

    private void applyCurrentCss() {
        if (appForm == null) {
            return;
        }
        restoreThemeDefaults();
        applySideMenuPalette(websiteDarkMode);
        List<PlaygroundRunner.Diagnostic> diagnostics = new ArrayList<PlaygroundRunner.Diagnostic>();
        List<PlaygroundRunner.InlineMessage> messages = new ArrayList<PlaygroundRunner.InlineMessage>();
        try {
            applyCssToPreview(appForm, currentCss);
            if (currentCss != null && currentCss.trim().length() > 0) {
                messages.add(new PlaygroundRunner.InlineMessage(0, "Custom CSS applied.", "success"));
            }
        } catch (RuntimeException ex) {
            String error = ex.getMessage() == null ? "Invalid CSS" : ex.getMessage();
            diagnostics.add(new PlaygroundRunner.Diagnostic(1, 1, 1, 2, error, "error"));
            messages.add(new PlaygroundRunner.InlineMessage(1, error, "error"));
        }
        currentCssMessages = messages;
        cssEditor.setMarkers(diagnostics);
        cssEditor.setInlineMessages(messages);
        cssEditor.setUiidCompletions(PlaygroundCssSupport.collectVisibleUiids(previewRoot));
        applyTabsTheme(websiteDarkMode);
        appForm.refreshTheme();
    }

    private void restoreThemeDefaults() {
        if (theme == null) {
            return;
        }
        String[] names = theme.getThemeResourceNames();
        if (names == null || names.length == 0) {
            return;
        }
        Hashtable baseTheme = theme.getTheme(names[0]);
        if (baseTheme != null) {
            UIManager.getInstance().setThemeProps(baseTheme);
        }
    }

    private void applyCssToPreview(Form form, String css) {
        String normalized = PlaygroundCssSupport.normalizeCustomCss(css);
        if (normalized.length() == 0) {
            return;
        }
        String wrappedCss = "\n/* Playground custom CSS */\n" + normalized + "\n";
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();
        compiler.compile(wrappedCss, resource, "PlaygroundCustomTheme");
        Hashtable customTheme = resource.getTheme("PlaygroundCustomTheme");
        if (customTheme != null && !customTheme.isEmpty()) {
            UIManager.getInstance().addThemeProps(customTheme);
            if (previewRoot != null) {
                previewRoot.refreshTheme();
                previewRoot.revalidate();
            } else {
                form.refreshTheme();
            }
        }
    }

    private void persistCurrentState() {
        PlaygroundStateStore.saveCurrentState(currentScript, currentCss, joinMessages(currentMessages, currentCssMessages));
    }

    private String joinMessages(List<PlaygroundRunner.InlineMessage> scriptMessages, List<PlaygroundRunner.InlineMessage> cssMessages) {
        List<PlaygroundRunner.InlineMessage> messages = new ArrayList<PlaygroundRunner.InlineMessage>();
        if (scriptMessages != null) {
            messages.addAll(scriptMessages);
        }
        if (cssMessages != null) {
            messages.addAll(cssMessages);
        }
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(messages.get(i).text);
        }
        return out.toString();
    }

    private void scheduleHistorySnapshot() {
        if (appForm == null) {
            return;
        }

        final int snapshot = ++editSequence;
        UITimer.timer(1200, false, appForm, () -> {
            if (snapshot == editSequence) {
                refreshHistoryMenu(appForm.getToolbar(), PlaygroundStateStore.pushHistory(currentScript));
            }
        });
    }

    private void scheduleAutoRun() {
        if (appForm == null) {
            return;
        }

        final int runTicket = ++autoRunSequence;
        UITimer.timer(850, false, appForm, () -> {
            if (runTicket == autoRunSequence) {
                runScript(appForm);
            }
        });
    }

    private void initWebsiteThemeSync(Form form) {
        websiteThemeNative = NativeLookup.create(WebsiteThemeNative.class);
        refreshWebsiteTheme(form);
        UITimer.timer(900, true, form, () -> refreshWebsiteTheme(form));
        UITimer.timer(250, true, form, this::syncOpenSideMenuTheme);
    }

    private void notifyWebsiteUiReady() {
        if (websiteThemeNative != null && websiteThemeNative.isSupported()) {
            websiteThemeNative.notifyUiReady();
            return;
        }

        BrowserComponent js = CN.getSharedJavascriptContext();
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
                res -> {}
        );
    }

    private void refreshWebsiteTheme(Form form) {
        if (Display.getInstance().isSimulator()) {
            applyDarkMode(form, DEFAULT_DARK_MODE);
            return;
        }

        if (websiteThemeNative != null && websiteThemeNative.isSupported()) {
            applyDarkMode(form, websiteThemeNative.isDarkMode());
            return;
        }

        BrowserComponent js = CN.getSharedJavascriptContext();
        if (js == null) {
            applyDarkMode(form, websiteDarkMode);
            return;
        }

        js.execute(
                "callback.onSuccess((function(){"
                        + "var dark = false;"
                        + "try {"
                        + "var parentWindow = (window.parent && window.parent !== window) ? window.parent : null;"
                        + "var parentDoc = parentWindow && parentWindow.document ? parentWindow.document : null;"
                        + "var parentBody = parentDoc && parentDoc.body ? parentDoc.body : null;"
                        + "var classes = parentBody && parentBody.classList ? parentBody.classList : null;"
                        + "if (classes) {"
                        + "if (classes.contains('dark') || classes.contains('cn1-initializr-dark')) { return 'true'; }"
                        + "if (classes.contains('light') || classes.contains('cn1-initializr-light')) { return 'false'; }"
                        + "}"
                        + "if (parentWindow && parentWindow.localStorage) {"
                        + "var pref = parentWindow.localStorage.getItem('pref-theme');"
                        + "if (pref === 'dark') { return 'true'; }"
                        + "if (pref === 'light') { return 'false'; }"
                        + "}"
                        + "var mediaWindow = parentWindow || window;"
                        + "if (mediaWindow.matchMedia) { dark = mediaWindow.matchMedia('(prefers-color-scheme: dark)').matches; }"
                        + "} catch (e) {}"
                        + "if (!dark && window.matchMedia) { dark = window.matchMedia('(prefers-color-scheme: dark)').matches; }"
                        + "return dark ? 'true' : 'false';"
                        + "})())",
                res -> applyDarkMode(form, "true".equals(String.valueOf(res)))
        );
    }

    private void applyDarkMode(Form form, boolean dark) {
        Display.getInstance().setDarkMode(dark);
        if (!websiteThemeInitialized || dark != websiteDarkMode) {
            websiteDarkMode = dark;
            websiteThemeInitialized = true;
            applySideMenuPalette(dark);
            applyWebsiteTheme(form, dark);
            applyTabsTheme(dark);
            form.refreshTheme();

            if (editor != null) {
                editor.applyTheme(dark);
            }
            if (cssEditor != null) {
                cssEditor.applyTheme(dark);
            }
            if (inspector != null) {
                inspector.applyTheme(dark);
            }
            for (Component cmp : sideMenuComponents) {
                applyWebsiteTheme(cmp, dark);
            }
        }
    }

    private void applySideMenuPalette(boolean dark) {
        Hashtable sideMenuPalette = new Hashtable();
        int bgColor = dark ? 0x0f172a : 0xffffff;
        int borderColor = dark ? 0x1f2937 : 0xcccccc;

        sideMenuPalette.put("SideNavigationPanel.bgColor", bgColor);
        sideMenuPalette.put("SideNavigationPanel.bgTransparency", 255);
        sideMenuPalette.put("SideNavigationPanelDark.bgColor", bgColor);
        sideMenuPalette.put("SideNavigationPanelDark.bgTransparency", 255);
        sideMenuPalette.put("RightSideNavigationPanel.bgColor", bgColor);
        sideMenuPalette.put("RightSideNavigationPanel.bgTransparency", 255);

        sideMenuPalette.put("StatusBarSideMenu.bgColor", bgColor);
        sideMenuPalette.put("StatusBarSideMenu.bgTransparency", 255);
        sideMenuPalette.put("StatusBarSideMenuDark.bgColor", bgColor);
        sideMenuPalette.put("StatusBarSideMenuDark.bgTransparency", 255);

        sideMenuPalette.put("SideCommand.bgColor", bgColor);
        sideMenuPalette.put("SideCommand.bgTransparency", 255);
        sideMenuPalette.put("SideCommand.border", com.codename1.ui.plaf.Border.createLineBorder(2, borderColor));
        UIManager.getInstance().addThemeProps(sideMenuPalette);
    }

    private void syncOpenSideMenuTheme() {
        Form current = Display.getInstance().getCurrent();
        if (current == null) {
            return;
        }
        applySideMenuContainerTheme(current);
    }

    private void applySideMenuContainerTheme(Component component) {
        if (component == null) {
            return;
        }
        String uiid = component.getUIID();
        if ("SideNavigationPanel".equals(uiid)
                || "SideNavigationPanelDark".equals(uiid)
                || "RightSideNavigationPanel".equals(uiid)
                || "StatusBarSideMenu".equals(uiid)
                || "StatusBarSideMenuDark".equals(uiid)) {
            applyWebsiteTheme(component, websiteDarkMode);
            component.getAllStyles().setBgTransparency(255);
            component.getAllStyles().setBgColor(websiteDarkMode ? 0x0f172a : 0xffffff);
        }
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applySideMenuContainerTheme(cnt.getComponentAt(i));
            }
        }
    }

    private void applyWebsiteTheme(Component component, boolean dark) {
        if (component == null) {
            return;
        }

        String role = getThemeRole(component);
        String newUiid = themedUiid(resolveBaseUiid(component, role), dark);
        if (newUiid != null && !newUiid.equals(component.getUIID())) {
            component.setUIID(newUiid);
        }

        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyWebsiteTheme(cnt.getComponentAt(i), dark);
            }
        }
    }

    private String resolveBaseUiid(Component component, String role) {
        if (ROLE_EMBEDDED_FORM.equals(role)) {
            return "PlaygroundEmbeddedForm";
        }
        if (ROLE_EMBEDDED_TITLE_AREA.equals(role)) {
            return "PlaygroundEmbeddedTitleArea";
        }

        String uiid = component.getUIID();
        if (uiid != null && uiid.endsWith("Dark")) {
            return uiid.substring(0, uiid.length() - 4);
        }
        return uiid;
    }

    private String themedUiid(String uiid, boolean dark) {
        if (uiid == null || uiid.isEmpty()) {
            return uiid;
        }

        if (!supportsDarkVariant(uiid)) {
            return uiid;
        }

        if (dark) {
            return uiid.endsWith("Dark") ? uiid : uiid + "Dark";
        }

        return uiid.endsWith("Dark") ? uiid.substring(0, uiid.length() - 4) : uiid;
    }

    private boolean supportsDarkVariant(String uiid) {
        switch (uiid) {
            case "PlaygroundForm":
            case "PlaygroundContent":
            case "PlaygroundToolbar":
            case "PlaygroundTitle":
            case "PlaygroundPanel":
            case "PlaygroundPreview":
            case "SideNavigationPanel":
            case "RightSideNavigationPanel":
            case "StatusBarSideMenu":
            case "PlaygroundSideCommand":
            case "PlaygroundSideCommandLine1":
            case "PlaygroundSideCommandLine2":
            case "PlaygroundMenuSection":
            case "PlaygroundMenuSectionTitle":
            case "PlaygroundMenuEmpty":
            case "PlaygroundMenuContainer":
            case "PlaygroundEmbeddedForm":
            case "PlaygroundEmbeddedTitleArea":
            case "PlaygroundInspectorRoot":
            case "PlaygroundInspectorTree":
            case "PlaygroundInspectorProps":
            case "PlaygroundPropName":
            case "PlaygroundPropValue":
            case "PlaygroundPropSmall":
            case "PlaygroundPropUnit":
            case "PlaygroundPropEmpty":
            case "PlaygroundColorPreview":
            case "PlaygroundInspectorTreeNode":
                return true;
            default:
                return false;
        }
    }

    private void applyTabsTheme(boolean dark) {
        if (editorTabs != null) {
            String tabsUiid = dark ? "PlaygroundEditorTabsDark" : "PlaygroundEditorTabs";
            String tabUiid = dark ? "TabDark" : "Tab";
            editorTabs.setUIID(tabsUiid);
            editorTabs.setTabUIID(tabUiid);
            Container tabsContainer = editorTabs.getTabsContainer();
            for (int i = 0; i < tabsContainer.getComponentCount(); i++) {
                tabsContainer.getComponentAt(i).setUIID(tabUiid);
            }
            editorTabs.refreshTheme();
            editorTabs.revalidate();
        }
    }

    private String getThemeRole(Component component) {
        Object val = component.getClientProperty(THEME_ROLE);
        return val instanceof String ? (String) val : null;
    }
}
