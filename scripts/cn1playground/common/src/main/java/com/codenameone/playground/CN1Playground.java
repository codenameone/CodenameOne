package com.codenameone.playground;

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
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
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

    private static final String PANEL_SAMPLES = "samples";
    private static final String PANEL_INSPECTOR = "inspector";
    private static final String PANEL_HISTORY = "history";

    private static final String MOBILE_TAB_CODE = "code";
    private static final String MOBILE_TAB_CSS = "css";
    private static final String MOBILE_TAB_PREVIEW = "preview";

    private final PlaygroundRunner runner = new PlaygroundRunner();
    private final PlaygroundProjectExporter projectExporter = new PlaygroundProjectExporter();

    private Form appForm;
    private PlaygroundBrowserEditor editor;
    private PlaygroundBrowserEditor cssEditor;
    private PlaygroundInspector inspector;
    private PlaygroundTopBar topBar;
    private PlaygroundActivityBar activityBar;
    private PlaygroundSamplesPanel samplesPanel;
    private PlaygroundHistoryPanel historyPanel;
    private Container inspectorWrapper;
    private PlaygroundPreviewColumn previewColumn;
    private PlaygroundSegmented mobileTopTabs;
    private Container bottomNav;

    private Container bodyContainer;
    private Container editorHost;
    private Container previewContainer;
    private Container leftSidePanelSlot;
    private Container rightSidePanelSlot;

    private Resources theme;
    private Resources androidTheme;
    private Resources iosTheme;
    private String activeDeviceThemeKey;
    private boolean websiteDarkMode = DEFAULT_DARK_MODE;
    private String currentScript;
    private String currentCss;
    private String currentMode = PlaygroundTopBar.MODE_CODE;
    private String currentActivity = PlaygroundActivityBar.NONE;
    private String currentMobileTab = MOBILE_TAB_CODE;
    private List<PlaygroundRunner.InlineMessage> currentMessages = new ArrayList<>();
    private List<PlaygroundRunner.InlineMessage> currentCssMessages = new ArrayList<>();
    private int editSequence;
    private int autoRunSequence;
    private int historySequence;
    private WebsiteThemeNative websiteThemeNative;
    private boolean websiteThemeInitialized;

    static final int LAYOUT_NONE = -1;
    static final int LAYOUT_MOBILE = 0;
    static final int LAYOUT_TABLET = 1;
    static final int LAYOUT_DESKTOP = 2;

    /// Test-only: when set to one of the LAYOUT_* constants, applyLayoutForCurrentSize
    /// bypasses Display.isDesktop / isTablet and uses this value. The harness can't
    /// spoof CN1's platform-capability checks from outside so this hook gives it a
    /// direct way to exercise each shell.
    static int testOnlyForceLayout = LAYOUT_NONE;

    private int lastLayout = LAYOUT_NONE;

    private static void detach(Component c) {
        if (c == null) {
            return;
        }
        Container parent = c.getParent();
        if (parent != null) {
            parent.removeComponent(c);
        }
    }

    private static void safeAddCenter(Container parent, Component child) {
        if (parent == null || child == null) {
            return;
        }
        detach(child);
        parent.add(BorderLayout.CENTER, child);
    }

    @Override
    public void runApp() {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        theme = Resources.getGlobalResources();
        currentScript = resolveInitialScript();
        currentCss = resolveInitialCss();
        currentMode = PlaygroundStateStore.loadMode(PlaygroundTopBar.MODE_CODE);

        appForm = new Form("Playground", new BorderLayout());
        appForm.setUIID("PlaygroundForm");

        // getToolbar can return null in a headless Display.init(null) context
        // (e.g. the layout harness) - guard so we don't NPE before the new
        // chrome is constructed.
        Toolbar toolbar = appForm.getToolbar();
        if (toolbar != null) {
            toolbar.setUIID("PlaygroundToolbar");
            toolbar.setTitleCentered(false);
            toolbar.hideToolbar();
        }

        editor = new PlaygroundBrowserEditor(PlaygroundBrowserEditor.Mode.JAVA, currentScript, websiteDarkMode, this::handleSourceChanged);
        cssEditor = new PlaygroundBrowserEditor(PlaygroundBrowserEditor.Mode.CSS, currentCss, websiteDarkMode, this::handleCssChanged);
        inspector = new PlaygroundInspector(websiteDarkMode, (component, property, value) -> handlePropertyChanged(component));

        topBar = new PlaygroundTopBar(currentMode, websiteDarkMode, new PlaygroundTopBar.Actions() {
            @Override
            public void onModeChanged(String key) {
                switchMode(key);
            }

            @Override
            public void onShare() {
                copyCurrentSourceUrl();
            }

            @Override
            public void onDownload() {
                projectExporter.export(currentScript, currentCss);
            }
        });

        PlaygroundActivityBar.Item[] activityItems = new PlaygroundActivityBar.Item[]{
                new PlaygroundActivityBar.Item(PANEL_SAMPLES, "Samples", FontImage.MATERIAL_DASHBOARD),
                new PlaygroundActivityBar.Item(PANEL_INSPECTOR, "Inspector", FontImage.MATERIAL_ACCOUNT_TREE),
                new PlaygroundActivityBar.Item(PANEL_HISTORY, "History", FontImage.MATERIAL_HISTORY)
        };
        currentActivity = PlaygroundStateStore.loadPanel(PlaygroundActivityBar.NONE);
        activityBar = new PlaygroundActivityBar(activityItems, currentActivity, websiteDarkMode, this::handleActivitySelected);

        samplesPanel = new PlaygroundSamplesPanel(websiteDarkMode, () -> handleActivitySelected(PlaygroundActivityBar.NONE), this::handleSampleSelected);
        historyPanel = new PlaygroundHistoryPanel(websiteDarkMode, () -> handleActivitySelected(PlaygroundActivityBar.NONE), this::handleHistorySelected);
        historyPanel.setEntries(PlaygroundStateStore.loadHistory());

        inspectorWrapper = buildInspectorWrapper();

        String device = PlaygroundStateStore.loadDevice(PlaygroundPreviewColumn.DEVICE_IPHONE);
        String orientation = PlaygroundStateStore.loadOrientation(PlaygroundPreviewColumn.ORIENTATION_PORTRAIT);
        previewColumn = new PlaygroundPreviewColumn(device, orientation, websiteDarkMode, () -> {
            PlaygroundStateStore.saveDevice(previewColumn.getDevice());
            PlaygroundStateStore.saveOrientation(previewColumn.getOrientation());
            applyDeviceTheme(previewColumn.getDevice());
        });

        mobileTopTabs = buildMobileTopTabs();
        bottomNav = buildBottomNav();

        editorHost = new Container(new BorderLayout());
        editorHost.setUIID(websiteDarkMode ? "PlaygroundPanelDark" : "PlaygroundPanel");

        previewContainer = new Container(new BorderLayout());
        previewContainer.setUIID(websiteDarkMode ? "PlaygroundPanelDark" : "PlaygroundPanel");
        previewContainer.add(BorderLayout.CENTER, previewColumn);

        leftSidePanelSlot = new Container(new BorderLayout());
        leftSidePanelSlot.getAllStyles().setBgTransparency(0);

        rightSidePanelSlot = new Container(new BorderLayout());
        rightSidePanelSlot.getAllStyles().setBgTransparency(0);

        bodyContainer = new Container(new BorderLayout());
        bodyContainer.setUIID(websiteDarkMode ? "PlaygroundBodyDark" : "PlaygroundBody");

        Container topBarStack = new Container(BoxLayout.y());
        topBarStack.getAllStyles().setBgTransparency(0);
        topBarStack.add(topBar);

        appForm.add(BorderLayout.NORTH, topBarStack);
        appForm.add(BorderLayout.CENTER, bodyContainer);

        applyLayoutForCurrentSize();
        applyMode(currentMode, false);
        applyActivity(currentActivity);

        applyWebsiteTheme(appForm, websiteDarkMode);
        applyDeviceTheme(previewColumn.getDevice());
        runScript(appForm);
        initWebsiteThemeSync(appForm);

        appForm.addOrientationListener(e -> applyLayoutForCurrentSize());
        appForm.addSizeChangedListener(e -> applyLayoutForCurrentSize());

        appForm.show();
        notifyWebsiteUiReady();

        // CN1's first paint on the HTML5 port sometimes settles before the
        // canvas has its final CSS size. Force one more layout pass shortly
        // after show() so the mobile shell lands at the correct breakpoint.
        UITimer.timer(150, false, appForm, () -> {
            lastLayout = LAYOUT_NONE;
            applyLayoutForCurrentSize();
            appForm.revalidate();
        });
    }

    @Override
    protected void handleNetworkError(NetworkEvent err) {
        Log.p("Networking error: " + err);
    }

    private Container buildInspectorWrapper() {
        Container wrapper = new Container(new BorderLayout());
        wrapper.setUIID(websiteDarkMode ? "PlaygroundSidePanelDark" : "PlaygroundSidePanel");

        Label headerLabel = new Label("INSPECTOR");
        headerLabel.setUIID(websiteDarkMode ? "PlaygroundSidePanelHeaderDark" : "PlaygroundSidePanelHeader");

        Button closeButton = new Button();
        closeButton.setUIID(websiteDarkMode ? "PlaygroundSidePanelCloseDark" : "PlaygroundSidePanelClose");
        FontImage.setMaterialIcon(closeButton, FontImage.MATERIAL_CLOSE, 3f);
        closeButton.addActionListener(e -> handleActivitySelected(PlaygroundActivityBar.NONE));

        Container header = new Container(new BorderLayout());
        header.getAllStyles().setBgTransparency(0);
        header.add(BorderLayout.CENTER, headerLabel);
        header.add(BorderLayout.EAST, closeButton);

        wrapper.add(BorderLayout.NORTH, header);
        wrapper.add(BorderLayout.CENTER, inspector.getComponent());
        wrapper.setPreferredW(Display.getInstance().convertToPixels(60f));
        return wrapper;
    }

    private PlaygroundSegmented buildMobileTopTabs() {
        PlaygroundSegmented.Option[] opts = new PlaygroundSegmented.Option[]{
                new PlaygroundSegmented.Option(MOBILE_TAB_CODE, "Code", FontImage.MATERIAL_CODE),
                new PlaygroundSegmented.Option(MOBILE_TAB_CSS, "CSS", FontImage.MATERIAL_BRUSH),
                new PlaygroundSegmented.Option(MOBILE_TAB_PREVIEW, "Preview", FontImage.MATERIAL_VISIBILITY)
        };
        return new PlaygroundSegmented(opts, MOBILE_TAB_CODE, websiteDarkMode, key -> {
            currentMobileTab = key;
            refreshMobileTabContent();
        });
    }

    private Container buildBottomNav() {
        Container nav = new Container(new GridLayout(1, 3));
        nav.setUIID(websiteDarkMode ? "PlaygroundBottomNavDark" : "PlaygroundBottomNav");
        nav.add(createBottomNavButton(PANEL_SAMPLES, "Samples", FontImage.MATERIAL_DASHBOARD));
        nav.add(createBottomNavButton(PANEL_INSPECTOR, "Inspector", FontImage.MATERIAL_ACCOUNT_TREE));
        nav.add(createBottomNavButton(PANEL_HISTORY, "History", FontImage.MATERIAL_HISTORY));
        return nav;
    }

    private Button createBottomNavButton(String key, String label, char icon) {
        Button btn = new Button(label);
        boolean active = key.equals(currentActivity);
        btn.setUIID(uiidForBottomNavItem(active));
        btn.setTextPosition(Component.BOTTOM);
        // Centre the icon+label stack inside the button cell. Button alignment
        // defaults to LEFT, which leaves the glyph hugging the left padding on
        // the HTML5 port instead of sitting above the label.
        btn.setAlignment(Component.CENTER);
        // FontImage.setMaterialIcon bakes the glyph using whatever foreground
        // colour the style has at the moment of the call. UIID application is
        // sometimes deferred (seen on the HTML5 port), leaving the FG at its
        // default for long enough that the baked glyph ends up invisible on
        // top of the nav's navy background. Force the UIID's foreground inline
        // so the bake always uses the intended colour. Same pattern as the
        // top bar app icon in PlaygroundTopBar.applyAppIconStyle.
        btn.getAllStyles().setFgColor(bottomNavItemFgColor(active, websiteDarkMode));
        FontImage.setMaterialIcon(btn, icon, 4f);
        btn.putClientProperty("navIcon", Character.valueOf(icon));
        btn.putClientProperty("navKey", key);
        btn.addActionListener(e -> handleActivitySelected(key));
        return btn;
    }

    private String uiidForBottomNavItem(boolean active) {
        if (active) {
            return websiteDarkMode ? "PlaygroundBottomNavItemActiveDark" : "PlaygroundBottomNavItemActive";
        }
        return websiteDarkMode ? "PlaygroundBottomNavItemDark" : "PlaygroundBottomNavItem";
    }

    /// Mirrors the `color:` values on the bottom-nav item UIIDs in theme.css.
    /// Kept in sync by hand so `FontImage.setMaterialIcon` bakes the glyph
    /// against the exact colour the stylesheet claims, regardless of when the
    /// UIID is actually applied to the component.
    private static int bottomNavItemFgColor(boolean active, boolean dark) {
        if (active) {
            return dark ? 0x4D86FF : 0x2F6BFF;
        }
        return dark ? 0xA8B8DA : 0x7F8AA3;
    }

    private void refreshBottomNav() {
        if (bottomNav == null) {
            return;
        }
        for (int i = 0; i < bottomNav.getComponentCount(); i++) {
            Component c = bottomNav.getComponentAt(i);
            if (!(c instanceof Button)) {
                continue;
            }
            Button btn = (Button) c;
            Object keyObj = btn.getClientProperty("navKey");
            if (!(keyObj instanceof String)) {
                continue;
            }
            boolean active = keyObj.equals(currentActivity);
            btn.setUIID(uiidForBottomNavItem(active));
            // Re-force the FG and re-bake the Material icon so the glyph
            // colour follows the new active/inactive state. Same rationale as
            // in createBottomNavButton - the UIID alone is unreliable because
            // setMaterialIcon captures the FG at call time, and the icon
            // image baked during construction would otherwise keep its old
            // (inactive) colour forever.
            btn.getAllStyles().setFgColor(bottomNavItemFgColor(active, websiteDarkMode));
            Object iconKey = btn.getClientProperty("navIcon");
            if (iconKey instanceof Character) {
                FontImage.setMaterialIcon(btn, ((Character) iconKey).charValue(), 4f);
            }
        }
        bottomNav.setUIID(websiteDarkMode ? "PlaygroundBottomNavDark" : "PlaygroundBottomNav");
        if (bottomNav.getComponentForm() != null) {
            bottomNav.revalidate();
        }
    }

    private void applyLayoutForCurrentSize() {
        int layout;
        if (testOnlyForceLayout != LAYOUT_NONE) {
            layout = testOnlyForceLayout;
        } else {
            Display d = Display.getInstance();
            float pxPerMm = d.convertToPixels(1f);
            if (pxPerMm < 0.1f) {
                pxPerMm = 4f;
            }
            int displayW = d.getDisplayWidth();
            float widthMm = displayW / pxPerMm;
            if (widthMm < 190f) {
                layout = LAYOUT_MOBILE;
            } else if (widthMm < 291f || d.isTablet()) {
                layout = LAYOUT_TABLET;
            } else {
                layout = LAYOUT_DESKTOP;
            }
        }
        if (layout == lastLayout) {
            return;
        }
        lastLayout = layout;

        bodyContainer.removeAll();
        if (layout == LAYOUT_MOBILE) {
            assembleMobileLayout();
        } else {
            assembleDesktopLayout(layout == LAYOUT_TABLET);
        }

        if (bodyContainer.getComponentForm() != null) {
            bodyContainer.revalidate();
        }
    }

    private void assembleDesktopLayout(boolean compact) {
        topBar.setCompact(compact);
        topBar.setMobile(false);
        previewColumn.setCompact(compact);
        previewColumn.setMobile(false);

        // Mobile shell plants bottomNav at appForm.SOUTH. Clear it so the
        // desktop shell doesn't end up with both an activity bar and a bottom
        // nav stacked at the form's south edge.
        detach(bottomNav);

        detach(activityBar);
        detach(leftSidePanelSlot);
        detach(rightSidePanelSlot);
        detach(editorHost);
        detach(previewContainer);
        detach(previewColumn);

        if (previewColumn.getParent() == null) {
            previewContainer.removeAll();
            previewContainer.add(BorderLayout.CENTER, previewColumn);
        }

        // Hint the preview with a minimum width equal to the iPhone skin in portrait
        // (72 mm body + ~20 mm breathing room so the bezel + corner mask don't get
        // squeezed flush against the surrounding chrome).
        previewContainer.setPreferredW(Display.getInstance().convertToPixels(92f));

        // SplitPane only between editor and preview. Samples/History and Inspector
        // are siblings OUTSIDE the split so opening them doesn't squash the skin.
        SplitPane.Settings splitSettings = new SplitPane.Settings(
                SplitPane.HORIZONTAL_SPLIT, "25%", "50%", "75%")
                .showExpandCollapseButtons(false)
                .showDragHandle(false)
                .dividerThicknessMM(0.8f)
                .dividerUIID("PlaygroundSplitDivider");
        SplitPane split = new SplitPane(splitSettings, editorHost, previewContainer);

        Container center = new Container(new BorderLayout());
        center.getAllStyles().setBgTransparency(0);
        center.add(BorderLayout.WEST, leftSidePanelSlot);
        center.add(BorderLayout.CENTER, split);
        center.add(BorderLayout.EAST, rightSidePanelSlot);

        Container bodyInner = new Container(new BorderLayout());
        bodyInner.getAllStyles().setBgTransparency(0);
        bodyInner.add(BorderLayout.WEST, activityBar);
        bodyInner.add(BorderLayout.CENTER, center);

        bodyContainer.add(BorderLayout.CENTER, bodyInner);

        attachEditorsToHost();
    }

    private void assembleMobileLayout() {
        topBar.setCompact(true);
        topBar.setMobile(true);
        previewColumn.setCompact(true);
        previewColumn.setMobile(true);

        detach(mobileTopTabs);
        detach(bottomNav);
        detach(editorHost);
        detach(previewContainer);
        detach(previewColumn);

        // Defensive: earlier flows may have called setHidden on bottomNav (the
        // brief calls for hiding it when the keyboard is up). Ensure it is
        // visible and has a real preferred height when mobile assembles so the
        // Samples / Inspector / History row is never silently missing.
        bottomNav.setHidden(false);
        bottomNav.setVisible(true);
        bottomNav.setPreferredH(Display.getInstance().convertToPixels(12f));

        if (previewColumn.getParent() == null) {
            previewContainer.removeAll();
            previewContainer.add(BorderLayout.CENTER, previewColumn);
        }

        Container stack = new Container(BoxLayout.y());
        stack.getAllStyles().setBgTransparency(0);
        stack.add(mobileTopTabs);

        Container tabContent = new Container(new BorderLayout());
        tabContent.getAllStyles().setBgTransparency(0);
        tabContent.putClientProperty("mobileTabContent", Boolean.TRUE);

        Container mobileLayout = new Container(new BorderLayout());
        mobileLayout.getAllStyles().setBgTransparency(0);
        mobileLayout.add(BorderLayout.NORTH, stack);
        mobileLayout.add(BorderLayout.CENTER, tabContent);

        bodyContainer.add(BorderLayout.CENTER, mobileLayout);

        // Put bottomNav at the FORM's SOUTH slot (not inside bodyContainer) so
        // the Form-level BorderLayout carves out its height BEFORE allocating
        // bodyContainer to the editor + tab stack. Keeps the nav height out
        // of the tab content region, so the Monaco iframe peers can't shrink
        // or reflow it when the editor reports its own preferred size.
        detach(bottomNav);
        appForm.add(BorderLayout.SOUTH, bottomNav);

        refreshMobileTabContent();
    }

    private void refreshMobileTabContent() {
        Container tabContent = findMobileTabContent(bodyContainer);
        if (tabContent == null) {
            return;
        }
        tabContent.removeAll();

        // If a bottom-nav panel is active, it replaces the tab content while
        // the top bar + bottom nav stay in place. Tapping the panel's close
        // button (or tapping the same nav item again) clears the activity and
        // restores the tab content. Samples / Inspector / History all behave
        // the same way - the richer "bottom sheet" variant is reserved for
        // later, but this keeps the mobile flow functional.
        Component mobilePanel = mobilePanelFor(currentActivity);
        if (mobilePanel != null) {
            safeAddCenter(tabContent, mobilePanel);
            if (tabContent.getComponentForm() != null) {
                tabContent.revalidate();
            }
            return;
        }

        switch (currentMobileTab) {
            case MOBILE_TAB_CSS:
                currentMode = PlaygroundTopBar.MODE_CSS;
                attachEditorsToHost();
                safeAddCenter(tabContent, editorHost);
                break;
            case MOBILE_TAB_PREVIEW:
                if (previewColumn.getParent() != previewContainer) {
                    detach(previewColumn);
                    previewContainer.removeAll();
                    previewContainer.add(BorderLayout.CENTER, previewColumn);
                }
                safeAddCenter(tabContent, previewContainer);
                break;
            default:
                currentMode = PlaygroundTopBar.MODE_CODE;
                attachEditorsToHost();
                safeAddCenter(tabContent, editorHost);
                break;
        }
        if (tabContent.getComponentForm() != null) {
            tabContent.revalidate();
        }
    }

    private Container findMobileTabContent(Container root) {
        if (root == null) {
            return null;
        }
        Object flag = root.getClientProperty("mobileTabContent");
        if (Boolean.TRUE.equals(flag)) {
            return root;
        }
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Container) {
                Container found = findMobileTabContent((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void attachEditorsToHost() {
        Component active = PlaygroundTopBar.MODE_CSS.equals(currentMode) ? cssEditor.getComponent() : editor.getComponent();
        Component other = PlaygroundTopBar.MODE_CSS.equals(currentMode) ? editor.getComponent() : cssEditor.getComponent();
        editorHost.removeAll();
        detach(active);
        detach(other);
        editorHost.add(BorderLayout.CENTER, active);
        if (editorHost.getComponentForm() != null) {
            editorHost.revalidate();
        }
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

    private void handleActivitySelected(String key) {
        currentActivity = key == null ? PlaygroundActivityBar.NONE : key;
        if (activityBar != null) {
            activityBar.setActive(currentActivity);
        }
        PlaygroundStateStore.savePanel(currentActivity);
        applyActivity(currentActivity);
        refreshBottomNav();
    }

    private Component mobilePanelFor(String key) {
        if (PANEL_SAMPLES.equals(key)) {
            return samplesPanel.getComponent();
        }
        if (PANEL_HISTORY.equals(key)) {
            historyPanel.setEntries(PlaygroundStateStore.loadHistory());
            return historyPanel.getComponent();
        }
        if (PANEL_INSPECTOR.equals(key)) {
            return inspectorWrapper;
        }
        return null;
    }

    private boolean isMobileLayout() {
        return lastLayout == LAYOUT_MOBILE;
    }

    private void applyActivity(String key) {
        if (isMobileLayout()) {
            // Mobile: bottom nav swaps the tab content with the selected
            // panel. No left/right slot involvement.
            detach(samplesPanel.getComponent());
            detach(historyPanel.getComponent());
            detach(inspectorWrapper);
            refreshMobileTabContent();
            return;
        }
        if (leftSidePanelSlot == null || rightSidePanelSlot == null) {
            return;
        }
        leftSidePanelSlot.removeAll();
        rightSidePanelSlot.removeAll();

        if (PANEL_SAMPLES.equals(key)) {
            safeAddCenter(leftSidePanelSlot, samplesPanel.getComponent());
        } else if (PANEL_HISTORY.equals(key)) {
            historyPanel.setEntries(PlaygroundStateStore.loadHistory());
            safeAddCenter(leftSidePanelSlot, historyPanel.getComponent());
        } else if (PANEL_INSPECTOR.equals(key)) {
            safeAddCenter(rightSidePanelSlot, inspectorWrapper);
        }

        // Empty Containers naturally collapse to ~0 preferred size so adjacent
        // slots (editorHost, previewContainer) fill the remaining space on their own.
        // Animate the shared ancestor (center) when the form is displayed -- this
        // is the BorderLayout that hosts both slots plus the SplitPane, so the
        // animation captures the layout transition as a single coordinated pass.
        // Do NOT call revalidate() here: animateLayout captures current positions,
        // applies the new layout, and interpolates between them, and a preceding
        // revalidate would skip the animation by snapping to the final layout.
        Form current = Display.getInstance().getCurrent();
        boolean formShowing = appForm != null && current == appForm;
        Container animationRoot = leftSidePanelSlot.getParent();
        if (formShowing && animationRoot != null) {
            animationRoot.animateLayout(220);
        }
    }

    private void handleSampleSelected(PlaygroundExamples.Sample sample) {
        setScript(sample.script, true);
    }

    private void handleHistorySelected(PlaygroundStateStore.HistoryEntry entry) {
        setScript(entry.script, true);
    }

    private void switchMode(String mode) {
        applyMode(mode, true);
    }

    private void applyMode(String mode, boolean userInitiated) {
        if (mode == null) {
            mode = PlaygroundTopBar.MODE_CODE;
        }
        currentMode = mode;
        if (topBar != null) {
            topBar.setMode(mode);
        }
        PlaygroundStateStore.saveMode(mode);
        attachEditorsToHost();
        if (userInitiated) {
            refreshMobileTabContent();
        }
    }

    private void runScript(Form form) {
        UITimer.timer(1, false, form, () -> executeRunScript(form));
    }

    private void executeRunScript(Form form) {
        CN.callSerially(() -> {
            List<PlaygroundRunner.InlineMessage> loggedMessages = new ArrayList<>();
            PlaygroundContext context = new PlaygroundContext(
                    form,
                    previewColumn.getContentHost(),
                    theme,
                    message -> loggedMessages.add(new PlaygroundRunner.InlineMessage(0, message, "info"))
            );

            PlaygroundRunner.RunResult result = runner.run(currentScript, context);

            currentMessages = new ArrayList<>(loggedMessages);
            currentMessages.addAll(result.getMessages());

            boolean hasErrors = false;
            List<PlaygroundRunner.Diagnostic> diagnostics = result.getDiagnostics();
            for (int i = 0; i < diagnostics.size(); i++) {
                if ("error".equalsIgnoreCase(diagnostics.get(i).severity)) {
                    hasErrors = true;
                    break;
                }
            }

            if (hasErrors) {
                topBar.showFailed();
                previewColumn.setStale(true);
            } else {
                topBar.showLive();
                previewColumn.setStale(false);
                replacePreview(result.getComponent());
            }

            editor.setMarkers(diagnostics);
            editor.setInlineMessages(currentMessages);
            editor.setUiidCompletions(PlaygroundCssSupport.collectVisibleUiids(previewColumn.getContentHost()));
            applyCurrentCss();
            persistCurrentState();
        });
    }

    private void replacePreview(Component component) {
        if (component == null) {
            previewColumn.setPreview(null);
            inspector.setPreviewRoot(null);
            return;
        }

        detachForPreview(component);
        markEmbeddedPreviewRoles(component);
        applyWebsiteTheme(component, websiteDarkMode);

        previewColumn.setPreview(component);
        // Feed the Inspector the user's script result directly rather than the
        // preview wrapper chain (stage / bezel / screen / mask) - otherwise the
        // tree's root shows inspector-internal skin chrome rather than the
        // user's actual root Component.
        inspector.setPreviewRoot(component);
    }

    private void detachForPreview(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.removeComponent(component);
        }
    }

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
        applyMode(PlaygroundTopBar.MODE_CODE, true);
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
        cssEditor.setUiidCompletions(PlaygroundCssSupport.collectVisibleUiids(previewColumn.getContentHost()));
        // Refresh only the user's preview content. Walking the bezel/screen/mask would
        // wipe programmatic borders that have no theme.css counterpart.
        if (previewColumn != null) {
            previewColumn.refreshPreviewTheme();
        }
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
        if (PlaygroundPreviewColumn.DEVICE_PIXEL.equals(activeDeviceThemeKey)) {
            layerAndroidTheme();
        } else {
            // iPhone AND no-skin both default to iOS base theme - no-skin just
            // means "don't render a bezel", not "don't theme the content".
            layerIosTheme();
        }
    }

    private void applyDeviceTheme(String device) {
        if (device == null) {
            device = PlaygroundPreviewColumn.DEVICE_IPHONE;
        }
        if (device.equals(activeDeviceThemeKey)) {
            return;
        }
        activeDeviceThemeKey = device;
        restoreThemeDefaults();
        applyCurrentCss();
        // Only refresh the user's preview content subtree. refreshTheme() walks by UIID
        // and would overwrite the bezel's programmatic RoundRectBorder and the
        // corner-mask overlay's transparent background since those UIIDs have no
        // matching declarations in theme.css.
        if (previewColumn != null) {
            previewColumn.refreshPreviewTheme();
        }
    }

    private void layerAndroidTheme() {
        if (androidTheme == null) {
            try {
                androidTheme = Resources.open("/androidTheme.res");
            } catch (java.io.IOException ex) {
                Log.p("Android theme unavailable: " + ex);
                return;
            }
        }
        applyThemeOverlay(androidTheme);
    }

    /// Layers the builtin iPhone native theme on top of the playground base.
    /// `iOS7Theme.res` ships with the Codename One distribution; we load it from the
    /// classpath instead of copying a local copy into the project resources.
    /// If the resource isn't on the runtime classpath (falls back silently) the
    /// iPhone skin renders with the playground base theme only.
    private void layerIosTheme() {
        if (iosTheme == null) {
            try {
                iosTheme = Resources.openLayered("/iOS7Theme");
            } catch (java.io.IOException ex) {
                try {
                    iosTheme = Resources.openLayered("/iPhoneTheme");
                } catch (java.io.IOException ex2) {
                    Log.p("iOS theme unavailable: " + ex2);
                    return;
                }
            }
        }
        applyThemeOverlay(iosTheme);
    }

    private void applyThemeOverlay(Resources res) {
        if (res == null) {
            return;
        }
        String[] names = res.getThemeResourceNames();
        if (names == null || names.length == 0) {
            return;
        }
        Hashtable props = res.getTheme(names[0]);
        if (props != null && !props.isEmpty()) {
            UIManager.getInstance().addThemeProps(props);
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
            if (previewColumn != null) {
                previewColumn.refreshPreviewTheme();
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
        final int snapshot = ++historySequence;
        UITimer.timer(2000, false, appForm, () -> {
            if (snapshot == historySequence) {
                List<PlaygroundStateStore.HistoryEntry> updated = PlaygroundStateStore.pushHistory(currentScript);
                if (historyPanel != null) {
                    historyPanel.setEntries(updated);
                }
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

            applyWebsiteTheme(form, dark);

            if (editor != null) {
                editor.applyTheme(dark);
            }
            if (cssEditor != null) {
                cssEditor.applyTheme(dark);
            }
            if (inspector != null) {
                inspector.applyTheme(dark);
            }
            if (topBar != null) {
                topBar.applyTheme(dark);
            }
            if (activityBar != null) {
                activityBar.applyTheme(dark);
            }
            if (samplesPanel != null) {
                samplesPanel.applyTheme(dark);
            }
            if (historyPanel != null) {
                historyPanel.applyTheme(dark);
            }
            if (previewColumn != null) {
                previewColumn.applyTheme(dark);
            }
            if (mobileTopTabs != null) {
                mobileTopTabs.applyTheme(dark);
            }

            if (inspectorWrapper != null) {
                inspectorWrapper.setUIID(dark ? "PlaygroundSidePanelDark" : "PlaygroundSidePanel");
            }
            if (editorHost != null) {
                editorHost.setUIID(dark ? "PlaygroundPanelDark" : "PlaygroundPanel");
            }
            if (previewContainer != null) {
                previewContainer.setUIID(dark ? "PlaygroundPanelDark" : "PlaygroundPanel");
            }
            if (bodyContainer != null) {
                bodyContainer.setUIID(dark ? "PlaygroundBodyDark" : "PlaygroundBody");
            }
            refreshBottomNav();
            form.refreshTheme();
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
            case "PlaygroundBody":
            case "PlaygroundToolbar":
            case "PlaygroundTitle":
            case "PlaygroundTopBar":
            case "PlaygroundAppIcon":
            case "PlaygroundWordmark":
            case "PlaygroundSegment":
            case "PlaygroundSegmentOption":
            case "PlaygroundSegmentOptionSelected":
            case "PlaygroundStatusPill":
            case "PlaygroundStatusPillError":
            case "PlaygroundStatusLabel":
            case "PlaygroundStatusLabelError":
            case "PlaygroundShareButton":
            case "PlaygroundDownloadButton":
            case "PlaygroundActivityBar":
            case "PlaygroundActivityButton":
            case "PlaygroundActivityButtonActive":
            case "PlaygroundSidePanel":
            case "PlaygroundSidePanelHeader":
            case "PlaygroundSidePanelClose":
            case "PlaygroundSearchField":
            case "PlaygroundSampleItem":
            case "PlaygroundSampleItemSelected":
            case "PlaygroundHistoryItem":
            case "PlaygroundHistoryLine1":
            case "PlaygroundHistoryLine2":
            case "PlaygroundPreviewToolbar":
            case "PlaygroundDimensions":
            case "PlaygroundDeviceStage":
            case "PlaygroundNoSkinStage":
            case "PlaygroundDeviceBezel":
            case "PlaygroundDeviceScreen":
            case "PlaygroundStaleChip":
            case "PlaygroundTopTabs":
            case "PlaygroundTopTab":
            case "PlaygroundTopTabSelected":
            case "PlaygroundBottomNav":
            case "PlaygroundBottomNavItem":
            case "PlaygroundBottomNavItemActive":
            case "PlaygroundPanel":
            case "PlaygroundSplitDivider":
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
            case "PlaygroundTreeRow":
            case "PlaygroundTreeRowActive":
            case "PlaygroundTreeChevron":
            case "PlaygroundTreeTypeIcon":
            case "PlaygroundTreeType":
            case "PlaygroundTreeTypeActive":
            case "PlaygroundTreeBracket":
            case "PlaygroundTreeBracketActive":
            case "PlaygroundInspectorSection":
            case "PlaygroundInspectorDivider":
            case "PlaygroundInspectorTreeDivider":
            case "PlaygroundFieldRow":
            case "PlaygroundFieldLabel":
            case "PlaygroundFieldInput":
            case "PlaygroundFieldReadOnly":
            case "PlaygroundFieldMicro":
            case "PlaygroundInspectorSwatch":
            case "PlaygroundInspectorSegment":
            case "PlaygroundInspectorSegmentActive":
            case "PlaygroundInspectorSegmentInactive":
            case "PlaygroundInspectorCheckbox":
            case "PlaygroundColorPreview":
            case "PlaygroundInspectorTreeNode":
                return true;
            default:
                return false;
        }
    }

    private String getThemeRole(Component component) {
        Object val = component.getClientProperty(THEME_ROLE);
        return val instanceof String ? (String) val : null;
    }
}
