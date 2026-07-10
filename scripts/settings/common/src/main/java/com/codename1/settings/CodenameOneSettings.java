package com.codename1.settings;

import com.codename1.components.InteractionDialog;
import com.codename1.components.Switch;
import com.codename1.components.ToastBar;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.settings.extensions.ExtensionDescriptor;
import com.codename1.settings.extensions.ExtensionCatalogMerger;
import com.codename1.settings.extensions.MavenCentralSearch;
import com.codename1.settings.extensions.MavenDependency;
import com.codename1.settings.extensions.PomEditor;
import com.codename1.settings.hints.BuildHintCatalog;
import com.codename1.settings.hints.BuildHintMetadata;
import com.codename1.settings.hints.BuildHintType;
import com.codename1.settings.project.ProjectBinding;
import com.codename1.settings.project.ProjectIO;
import com.codename1.settings.project.SettingsProperties;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.TableLayout;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodenameOneSettings extends Lifecycle {
    public enum Section { BASIC, BUILD_HINTS, EXTENSIONS, ADVANCED }

    private static final String PREF_DARK_MODE = "settings.darkMode";
    private static final String PREF_FONT_DELTA = "settings.fontDeltaPx";
    private static final String EXTENSIONS_URL = "https://www.codenameone.com/files/CN1Libs.xml";

    private ProjectBinding binding;
    private SettingsProperties settings;
    private BuildHintCatalog buildHints = BuildHintCatalog.fallback();
    private Section section = Section.BASIC;
    private Form form;
    private Container page;
    private Container pageViewport;
    private boolean darkMode;
    private int fontDeltaPx;
    private float fontPinchAccumulator = 1f;
    private String hintFilter = "";
    private String extensionFilter = "";
    private List<ExtensionDescriptor> extensionCatalog;
    private final Map<String, Boolean> expandedExtensions = new LinkedHashMap<String, Boolean>();
    private Button toolbarMenuButton;
    private static CodenameOneSettings activeSettings;

    public static void adjustActiveFontSizeForDesktopShortcut(int deltaPx) {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(() -> s.adjustFontSize(deltaPx));
        }
    }

    public static void resetActiveFontSizeForDesktopShortcut() {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(s::resetFontSize);
        }
    }

    public static void saveActiveSettingsForDesktopMenu() {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(s::saveSettings);
        }
    }

    public static void openActiveProjectFolderForDesktopMenu() {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(s::openProjectFolder);
        }
    }

    public static void toggleActiveDarkModeForDesktopMenu() {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(s::toggleDarkMode);
        }
    }

    public static void goActiveSectionForDesktopMenu(Section section) {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(() -> s.go(section));
        }
    }

    public static void showActiveAboutForDesktopMenu() {
        CodenameOneSettings s = activeSettings;
        if (s != null) {
            CN.callSerially(s::showAboutDialog);
        }
    }

    @Override
    public void runApp() {
        activeSettings = this;
        Toolbar.setGlobalToolbar(true);
        darkMode = Preferences.get(PREF_DARK_MODE, Boolean.TRUE.equals(CN.isDarkMode()));
        String forcedDark = System.getProperty("settings.darkMode");
        if ("true".equals(forcedDark) || "false".equals(forcedDark)) {
            darkMode = "true".equals(forcedDark);
        }
        String forcedSection = System.getProperty("settings.section");
        if (forcedSection != null) {
            for (Section candidate : Section.values()) {
                if (candidate.name().equalsIgnoreCase(forcedSection.replace('-', '_'))) {
                    section = candidate;
                    break;
                }
            }
        }
        String forcedHintFilter = System.getProperty("settings.hintFilter");
        if (forcedHintFilter != null) {
            hintFilter = forcedHintFilter;
        }
        fontDeltaPx = Preferences.get(PREF_FONT_DELTA, 0);
        CN.setDarkMode(Boolean.valueOf(darkMode));
        loadProject();
        installErrorHandlers();

        form = new Form("Codename One Settings", new BorderLayout()) {
            @Override
            public void keyPressed(int keyCode) {
                if (handleFontShortcut(keyCode)) {
                    return;
                }
                super.keyPressed(keyCode);
            }

            @Override
            protected boolean pinch(float scale) {
                handleFontPinch(scale);
                return true;
            }

            @Override
            protected void pinchReleased(int x, int y) {
                fontPinchAccumulator = 1f;
                super.pinchReleased(x, y);
            }
        };
        form.setUIID(uiid("SettingsForm"));
        form.getTextSelection().setEnabled(true);
        installMenuCommands();
        buildShell();
        form.show();
        if ("true".equals(System.getProperty("settings.openMenu")) && toolbarMenuButton != null) {
            CN.callSerially(() -> showAppMenu(toolbarMenuButton));
        }
    }

    private void loadProject() {
        binding = ProjectIO.loadBinding();
        if (binding != null) {
            settings = new SettingsProperties(binding.settings());
            try {
                settings.load();
            } catch (Exception ex) {
                Log.e(ex);
            }
            buildHints = loadBuildHints(binding.buildHintsDoc());
        }
    }

    private BuildHintCatalog loadBuildHints(String docPath) {
        InputStream in = null;
        if (docPath != null && docPath.length() > 0) {
            try {
                String url = ProjectIO.fsUrl(docPath);
                FileSystemStorage fs = FileSystemStorage.getInstance();
                if (fs.exists(url)) {
                    in = fs.openInputStream(url);
                    return BuildHintCatalog.fromAsciiDoc(Util.readToString(in, "UTF-8"));
                }
            } catch (Exception ex) {
                Log.e(ex);
            } finally {
                Util.cleanup(in);
                in = null;
            }
        }
        try {
            in = getClass().getResourceAsStream("/com/codename1/settings/hints/Advanced-Topics-Under-The-Hood.asciidoc");
            if (in == null) {
                return BuildHintCatalog.fallback();
            }
            return BuildHintCatalog.fromAsciiDoc(Util.readToString(in, "UTF-8"));
        } catch (Exception ex) {
            Log.e(ex);
            return BuildHintCatalog.fallback();
        } finally {
            Util.cleanup(in);
        }
    }

    private void installErrorHandlers() {
        CN.addEdtErrorHandler(evt -> {
            evt.consume();
            Throwable err = evt.getSource() instanceof Throwable ? (Throwable) evt.getSource() : null;
            if (err != null) {
                Log.e(err);
            }
            ToastBar.showErrorMessage(err == null ? "An internal Settings error occurred." : err.getMessage());
        });
        CN.addNetworkErrorListener(evt -> {
            evt.consume();
            if (evt.getError() != null) {
                Log.e(evt.getError());
            }
            ToastBar.showErrorMessage("Network request failed"
                    + (evt.getResponseCode() > 0 ? " (HTTP " + evt.getResponseCode() + ")" : ""));
        });
    }

    private void buildShell() {
        form.getContentPane().removeAll();
        page = new Container(BoxLayout.y());
        renderPage();
        pageViewport = new Container(BoxLayout.y());
        pageViewport.setScrollableY(true);
        pageViewport.setUIID(uiid("SettingsPage"));
        TableLayout contentLayout = new TableLayout(1, 2);
        Container pageRow = new Container(contentLayout);
        int contentPercent = Display.getInstance().getDisplayWidth() < 1100 || section == Section.EXTENSIONS ? 100 : 72;
        pageRow.add(contentLayout.createConstraint(0, 0).widthPercentage(contentPercent), page);
        if (contentPercent < 100) {
            pageRow.add(contentLayout.createConstraint(0, 1).widthPercentage(100 - contentPercent), new Container());
        }
        pageViewport.add(pageRow);
        form.add(BorderLayout.NORTH, configureToolbar());
        form.add(BorderLayout.CENTER, BorderLayout.center(pageViewport).add(BorderLayout.WEST, rail()));
        applyFontScale(form);
        form.revalidate();
    }

    private Button toolbarIcon(char icon) {
        Button b = new Button("", uiid("SettingsToolbarButton"));
        b.setMaterialIcon(icon, 3.8f);
        return b;
    }

    private Image toolbarMarkImage() {
        try {
            Image image = Image.createImage("/icon.png");
            int size = CN.convertToPixels(2.05f);
            return image == null ? null : image.scaled(size, size);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toolbarAppName() {
        if (settings == null) {
            return "";
        }
        String name = settings.get("codename1.displayName", "");
        return name == null || name.length() == 0 ? "" : name;
    }

    private Container configureToolbar() {
        Container tb = new Container();
        tb.setLayout(new BorderLayout());
        tb.setUIID(uiid("SettingsChrome"));

        Container left = new Container(BoxLayout.x());
        Button brand = new Button("Settings", uiid("SettingsToolbarBrand"));
        brand.setMaterialIcon(FontImage.MATERIAL_SETTINGS, 3.8f);
        left.add(brand);
        String appName = toolbarAppName();
        if (appName.length() > 0) {
            Label appNameLabel = new Label(appName, uiid("SettingsAppName"));
            left.add(appNameLabel);
        }

        Container path = new Container(new BorderLayout());
        path.setUIID(uiid("SettingsPathChip"));
        Label pathText = new Label(toolbarPathText(), uiid("SettingsPathText"));
        pathText.setEndsWith3Points(true);
        Label pathIcon = new Label("", uiid("SettingsPathIcon"));
        pathIcon.setMaterialIcon(FontImage.MATERIAL_FOLDER_OPEN, 3.2f);
        path.add(BorderLayout.WEST, pathIcon);
        path.add(BorderLayout.CENTER, pathText);

        Container right = new Container(BoxLayout.x());
        Button open = toolbarIcon(FontImage.MATERIAL_FOLDER_OPEN);
        open.addActionListener(e -> openProjectFolder());
        Button save = new Button("Save", uiid("SettingsSave"));
        save.setMaterialIcon(FontImage.MATERIAL_SAVE, 3.2f);
        save.addActionListener(e -> saveSettings());
        Button theme = toolbarIcon(darkMode ? FontImage.MATERIAL_BRIGHTNESS_5 : FontImage.MATERIAL_BRIGHTNESS_3);
        theme.addActionListener(e -> toggleDarkMode());
        Button menu = toolbarIcon(FontImage.MATERIAL_MENU);
        menu.addActionListener(e -> showAppMenu(menu));
        toolbarMenuButton = menu;
        right.add(open).add(save).add(theme).add(menu);

        tb.add(BorderLayout.WEST, left);
        tb.add(BorderLayout.CENTER, path);
        tb.add(BorderLayout.EAST, right);
        return tb;
    }

    private String toolbarPathText() {
        String path = binding == null || binding.projectDir() == null ? "No project selected" : binding.projectDir();
        if (!path.startsWith("file:") && binding != null && binding.projectDir() != null) {
            path = "file:" + path;
        }
        if (path.length() <= 86) {
            return path;
        }
        return path.substring(0, 48) + "..." + path.substring(path.length() - 30);
    }

    private void showAppMenu(Component anchor) {
        InteractionDialog d = new InteractionDialog();
        d.setLayout(new BorderLayout());
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setAnimateShow(true);
        Container menu = new Container(BoxLayout.y());
        menu.setUIID(uiid("SettingsPopupMenu"));
        popupAction(menu, "Update", FontImage.MATERIAL_REFRESH, () -> {
            d.dispose();
            extensionCatalog = null;
            if (section == Section.EXTENSIONS) {
                renderPage();
                animatePage();
            } else {
                ToastBar.showMessage("Extension catalog will refresh when opened.", FontImage.MATERIAL_REFRESH);
            }
        });
        popupAction(menu, "Save", FontImage.MATERIAL_SAVE, () -> {
            d.dispose();
            saveSettings();
        });
        popupToggle(menu, "Dark Mode", darkMode, () -> {
                    d.dispose();
                    toggleDarkMode();
                });
        popupAction(menu, "Close", FontImage.MATERIAL_CANCEL, () -> {
            d.dispose();
            Display.getInstance().exitApplication();
        });
        d.add(BorderLayout.CENTER, menu);
        d.setUIID(uiid("SettingsPopupMenu"));
        d.getContentPane().setUIID(uiid("SettingsPopupMenu"));
        int right = CN.convertToPixels(2f);
        int top = anchor.getAbsoluteY() + anchor.getHeight() - CN.convertToPixels(0.5f);
        int menuWidth = Display.getInstance().getDisplayWidth() * 18 / 100;
        int menuHeight = menu.getStyle().getVerticalPadding() + d.getStyle().getVerticalPadding();
        for (int i = 0; i < menu.getComponentCount(); i++) {
            menuHeight += menu.getComponentAt(i).getPreferredH();
        }
        int left = Math.max(CN.convertToPixels(1f), Display.getInstance().getDisplayWidth() - menuWidth - right);
        int bottom = Math.max(CN.convertToPixels(1f), Display.getInstance().getDisplayHeight() - top - menuHeight);
        d.show(top, bottom, left, right);
    }

    private void showAboutDialog() {
        Dialog d = new Dialog("About Codename One Settings", new BorderLayout());
        Container content = new Container(BoxLayout.y());
        content.setUIID(uiid("SettingsDialogContent"));
        content.add(new Label("Codename One Settings", uiid("SettingsCardTitle")));
        content.add(new Label("Version " + appVersion(), uiid("SettingsRowMeta")));
        content.add(new Label("Java " + prop("java.version", "unknown"), uiid("SettingsRowMeta")));
        content.add(new Label(prop("java.vm.name", "JVM"), uiid("SettingsRowMeta")));
        content.add(new Label(prop("os.name", "OS") + " " + prop("os.version", ""), uiid("SettingsRowMeta")));
        if (binding != null && binding.projectDir() != null) {
            Label project = new Label(binding.projectDir(), uiid("SettingsRowMeta"));
            project.setEndsWith3Points(true);
            content.add(project);
        }
        Button close = new Button("Close", uiid("SettingsPrimary"));
        close.addActionListener(e -> d.dispose());
        content.add(FlowLayout.encloseRight(close));
        d.add(BorderLayout.CENTER, content);
        d.showPopupDialog(toolbarMenuButton == null ? form : toolbarMenuButton);
    }

    private String appVersion() {
        return prop("settings.version", "development");
    }

    private String prop(String key, String fallback) {
        String value = System.getProperty(key);
        return value == null || value.length() == 0 ? fallback : value;
    }

    private void popupAction(Container menu, String text, char icon, Runnable action) {
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid("SettingsPopupItem"));
        Button b = new Button(text, uiid("SettingsPopupLabel"));
        b.addActionListener(e -> action.run());
        Label iconLabel = new Label("", uiid("SettingsPopupIcon"));
        iconLabel.setMaterialIcon(icon, 2.8f);
        row.add(BorderLayout.CENTER, b);
        row.add(BorderLayout.EAST, iconLabel);
        row.setLeadComponent(b);
        menu.add(row);
    }

    private void popupToggle(Container menu, String text, boolean on, Runnable action) {
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid("SettingsPopupItem"));
        Label label = new Label(text, uiid("SettingsPopupToggleLabel"));
        Switch sw = new Switch(uiid("SettingsSwitch"));
        sw.setValue(on);
        sw.addActionListener(e -> action.run());
        row.add(BorderLayout.CENTER, label).add(BorderLayout.EAST, sw);
        menu.add(row);
    }

    private void openProjectFolder() {
        if (binding != null && binding.projectDir() != null) {
            Display.getInstance().execute(ProjectIO.fsUrl(binding.projectDir()));
        }
    }

    private Container rail() {
        Container side = new Container(BoxLayout.y());
        side.setUIID(uiid("SettingsRail"));
        nav(side, Section.BASIC, FontImage.MATERIAL_TUNE, "Basic");
        nav(side, Section.BUILD_HINTS, FontImage.MATERIAL_TUNE, "Hints");
        nav(side, Section.EXTENSIONS, FontImage.MATERIAL_EXTENSION, "Ext");
        return side;
    }

    private void nav(Container side, Section target, char icon, String text) {
        Button item = new Button(text, uiid(section == target ? "SettingsRailItemSelected" : "SettingsRailItem"));
        item.setTextPosition(Component.BOTTOM);
        item.setMaterialIcon(icon, 3.8f);
        item.addActionListener(e -> go(target));
        side.add(item);
    }

    private void renderPage() {
        page.removeAll();
        if (binding == null || settings == null) {
            page.add(pageTitle("No Maven Project", "Run this from a Codename One Maven project using mvn cn1:settings."));
            return;
        }
        switch (section) {
            case BASIC -> renderBasic();
            case BUILD_HINTS -> renderBuildHints();
            case EXTENSIONS -> renderExtensions();
            case ADVANCED -> renderAdvanced();
        }
        page.revalidate();
    }

    private void renderBasic() {
        page.add(pageTitle("Basic", "Core application settings - title, version, package and icon."));
        Container grid = new Container(new GridLayout(3, 2));
        grid.setUIID(uiid("SettingsFieldGrid"));
        grid.add(textFieldGroup("Title", "codename1.displayName", false));
        grid.add(textFieldGroup("Description", "codename1.description", false));
        grid.add(textFieldGroup("Version", "codename1.version", false));
        grid.add(textFieldGroup("Vendor", "codename1.vendor", false));
        grid.add(textFieldGroup("Package Name", "codename1.packageName", false));
        grid.add(textFieldGroup("Main Class", "codename1.mainName", false));
        page.add(grid);
        page.add(iconDrop());
        page.add(divider());
        Label premiumTitle = new Label("PREMIUM FEATURES", uiid("SettingsSectionTag"));
        page.add(premiumTitle);
        Container premium = new Container(new GridLayout(1, 2));
        premium.setUIID(uiid("SettingsFieldGrid"));
        premium.add(versionedBuildField());
        premium.add(includeSourceField());
        page.add(premium);
    }

    private Component versionedBuildField() {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        Label fieldLabel = new Label("Versioned Build", uiid("SettingsFieldLabel"));
        Picker version = new Picker();
        version.setType(Display.PICKER_TYPE_STRINGS);
        version.setStrings(versionChoices());
        version.setUIID(uiid("SettingsField"));
        String current = settings.getBuildHint("build.version");
        version.setSelectedString(current == null || current.length() == 0 ? "none" : current);
        version.addActionListener(e -> {
            String selected = version.getSelectedString();
            if (selected == null || "none".equals(selected)) {
                settings.removeBuildHint("build.version");
            } else {
                settings.setBuildHint("build.version", selected);
            }
        });
        fieldGroup.add(fieldLabel).add(version);
        return fieldGroup;
    }

    private String[] versionChoices() {
        return new String[]{"none", "master", "7.0.250", "7.0.249", "7.0.248", "7.0.247", "7.0.246", "7.0.245"};
    }

    private Component includeSourceField() {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        Label fieldLabel = new Label("Include Source", uiid("SettingsFieldLabel"));
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid("SettingsToggleRow"));
        Label label = new Label("Bundle project source", uiid("SettingsRowMeta"));
        row.add(BorderLayout.CENTER, label);
        Switch includeSource = new Switch(uiid("SettingsSwitch"));
        includeSource.setValue("1".equals(settings.getBuildHint("build.incSources")));
        includeSource.addActionListener(e -> {
            if (includeSource.isValue()) {
                settings.setBuildHint("build.incSources", "1");
            } else {
                settings.removeBuildHint("build.incSources");
            }
        });
        row.add(BorderLayout.EAST, includeSource);
        fieldGroup.add(fieldLabel).add(row);
        return fieldGroup;
    }

    private void renderBuildHints() {
        page.add(pageTitle("Build Hints", "Search known hints from the developer guide, or add arbitrary build arguments."));
        Container filter = new Container(new BorderLayout());
        filter.setUIID(uiid("SettingsFilterRow"));
        TextField search = new TextField(hintFilter, "Search build hints");
        search.setUIID(uiid("SettingsField"));
        search.addDataChangedListener((type, index) -> {
            hintFilter = search.getText() == null ? "" : search.getText();
            renderBuildHintsList();
        });
        filter.add(BorderLayout.CENTER, search);
        page.add(filter);
        page.add(customHintRow());
        Container list = new Container(BoxLayout.y());
        list.setName("buildHintsList");
        list.setUIID(uiid("SettingsList"));
        page.add(list);
        renderBuildHintsList();
    }

    private Component customHintRow() {
        TableLayout layout = new TableLayout(1, 3);
        Container row = new Container(layout);
        row.setUIID(uiid("SettingsRow"));
        TextField key = new TextField("", "custom.hint.name");
        key.setUIID(uiid("SettingsField"));
        TextField value = new TextField("", "value");
        value.setUIID(uiid("SettingsField"));
        Container keyCell = BorderLayout.center(key);
        keyCell.setUIID(uiid("SettingsHintKeyCell"));
        Container valueCell = BorderLayout.center(value);
        valueCell.setUIID(uiid("SettingsHintValueCell"));
        Button add = new Button("Add", uiid("SettingsOutline"));
        add.addActionListener(e -> {
            String k = key.getText() == null ? "" : key.getText().trim();
            if (k.startsWith(SettingsProperties.BUILD_HINT_PREFIX)) {
                k = k.substring(SettingsProperties.BUILD_HINT_PREFIX.length());
            }
            if (k.length() == 0) {
                ToastBar.showErrorMessage("Enter a build hint name.");
                return;
            }
            settings.setBuildHint(k, value.getText() == null ? "" : value.getText());
            key.setText("");
            value.setText("");
            renderBuildHintsList();
            animatePage();
        });
        row.add(layout.createConstraint(0, 0).widthPercentage(43), keyCell);
        row.add(layout.createConstraint(0, 1).widthPercentage(43), valueCell);
        row.add(layout.createConstraint(0, 2).widthPercentage(14), add);
        return row;
    }

    private void renderBuildHintsList() {
        Container list = (Container) page.getComponentAt(page.getComponentCount() - 1);
        list.removeAll();
        Map<String, BuildHintMetadata> rows = new LinkedHashMap<String, BuildHintMetadata>();
        for (String key : settings.buildHintKeys()) {
            BuildHintMetadata meta = buildHints.get(key);
            rows.put(key, meta == null
                    ? new BuildHintMetadata(key, "Custom build hint.", null, "custom")
                    : meta);
        }
        for (BuildHintMetadata meta : buildHints.search(hintFilter)) {
            rows.put(meta.name(), meta);
        }
        for (BuildHintMetadata meta : rows.values()) {
            if (!meta.matches(hintFilter)) {
                continue;
            }
            list.add(hintRow(meta));
        }
        list.revalidate();
    }

    private void animatePage() {
        if (form != null) {
            form.animateLayout(180);
        }
    }

    private Component hintRow(BuildHintMetadata meta) {
        Container row = new Container(BoxLayout.y());
        row.setUIID(uiid("SettingsRow"));
        boolean active = hasBuildHint(meta.name());
        String value = active ? settings.getBuildHint(meta.name()) : "";
        BuildHintType effectiveType = effectiveHintType(meta, value);
        Container text = new Container(BoxLayout.y());
        Label name = new Label(meta.name(), uiid("SettingsRowTitle"));
        name.setEndsWith3Points(true);
        Container metaLine = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        Label desc = new Label(meta.platform() + " / " + effectiveType, uiid("SettingsRowMeta"));
        metaLine.add(desc);
        if (active) {
            metaLine.add(new Label("Active", uiid("SettingsActiveBadge")));
        }
        text.add(name).add(metaLine);
        if (active) {
            text.add(activeHintEditor(meta, value, effectiveType));
        } else {
            Container controls = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
            controls.setUIID(uiid("SettingsHintEditor"));
            Button add = new Button("Add", uiid("SettingsOutline"));
            add.setMaterialIcon(FontImage.MATERIAL_ADD, 1.2f);
            add.addActionListener(e -> {
                settings.setBuildHint(meta.name(), defaultHintValue(meta));
                renderBuildHintsList();
                animatePage();
            });
            controls.add(add);
            Container header = new Container(new BorderLayout());
            header.add(BorderLayout.CENTER, text);
            header.add(BorderLayout.EAST, controls);
            row.add(header);
        }
        if (active) {
            row.add(text);
        }
        TextArea details = new TextArea(meta.description());
        details.setUIID(uiid("SettingsRowText"));
        details.setEditable(false);
        details.setFocusable(false);
        details.setRows(descriptionRows(meta.description()));
        details.setGrowByContent(true);
        row.add(details);
        return row;
    }

    private Component activeHintEditor(BuildHintMetadata meta, String value, BuildHintType effectiveType) {
        TableLayout editorLayout = new TableLayout(1, 2);
        Container editor = new Container(editorLayout);
        editor.setUIID(uiid("SettingsHintEditor"));
        Container controls = new Container(new BorderLayout());
        if (effectiveType == BuildHintType.BOOLEAN) {
            Switch toggle = new Switch(uiid("SettingsSwitch"));
            toggle.setValue("true".equalsIgnoreCase(value));
            toggle.addActionListener(e -> settings.setBuildHint(meta.name(), toggle.isValue() ? "true" : "false"));
            controls.add(BorderLayout.CENTER,
                    new Container(new FlowLayout(Component.RIGHT, Component.CENTER)).add(toggle));
        } else {
            TextField valueField = new TextField(value, "value");
            valueField.setUIID(uiid(isValidHintValue(meta, value) ? "SettingsField" : "SettingsFieldError"));
            configureHintField(valueField, meta);
            valueField.addDataChangedListener((type, index) -> {
                String next = valueField.getText() == null ? "" : valueField.getText().trim();
                if (isValidHintValue(meta, next)) {
                    settings.setBuildHint(meta.name(), next);
                    valueField.setUIID(uiid("SettingsField"));
                } else {
                    valueField.setUIID(uiid("SettingsFieldError"));
                }
                valueField.repaint();
            });
            controls.add(BorderLayout.CENTER, valueField);
        }
        Button remove = new Button("", uiid("SettingsSmallIconButton"));
        remove.setMaterialIcon(FontImage.MATERIAL_DELETE, 2.2f);
        remove.addActionListener(e -> {
            settings.removeBuildHint(meta.name());
            renderBuildHintsList();
            animatePage();
        });
        controls.add(BorderLayout.EAST, remove);
        editor.add(editorLayout.createConstraint(0, 0).widthPercentage(72), new Container());
        editor.add(editorLayout.createConstraint(0, 1).widthPercentage(28), controls);
        return editor;
    }

    private BuildHintType effectiveHintType(BuildHintMetadata meta, String value) {
        if (meta.type() == BuildHintType.BOOLEAN
                || "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value)) {
            return BuildHintType.BOOLEAN;
        }
        return meta.type();
    }

    private boolean hasBuildHint(String key) {
        return settings.keys().contains(SettingsProperties.fullBuildHintKey(key));
    }

    private String defaultHintValue(BuildHintMetadata meta) {
        if (meta.type() == BuildHintType.BOOLEAN) {
            return "true";
        }
        if (meta.type() == BuildHintType.INTEGER) {
            return "0";
        }
        return "";
    }

    private int descriptionRows(String text) {
        int len = text == null ? 0 : text.length();
        if (len > 260) {
            return 5;
        }
        if (len > 170) {
            return 4;
        }
        if (len > 95) {
            return 3;
        }
        return 2;
    }

    private void configureHintField(TextField field, BuildHintMetadata meta) {
        if (meta.type() == BuildHintType.INTEGER) {
            field.setConstraint(TextArea.NUMERIC);
        } else if (meta.type() == BuildHintType.URL) {
            field.setConstraint(TextArea.URL);
        } else if (meta.type() == BuildHintType.SECRET) {
            field.setConstraint(TextArea.PASSWORD);
        }
    }

    private boolean isValidHintValue(BuildHintMetadata meta, String value) {
        if (value == null || value.trim().length() == 0) {
            return true;
        }
        String v = value.trim();
        if (meta.type() == BuildHintType.INTEGER) {
            return isDigits(v);
        }
        if (meta.type() == BuildHintType.VERSION) {
            return isVersion(v) || "master".equals(v);
        }
        if (meta.type() == BuildHintType.URL) {
            return (v.startsWith("http://") || v.startsWith("https://")) && v.indexOf('.', v.indexOf("://") + 3) > 0;
        }
        return true;
    }

    private boolean isDigits(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private boolean isVersion(String text) {
        if (text == null || text.length() == 0 || !isDigit(text.charAt(0))) {
            return false;
        }
        boolean lastWasSeparator = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isDigit(c) || isAsciiLetter(c)) {
                lastWasSeparator = false;
                continue;
            }
            if (c == '.' || c == '_' || c == '-') {
                if (lastWasSeparator) {
                    return false;
                }
                lastWasSeparator = true;
                continue;
            }
            return false;
        }
        return !lastWasSeparator;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private void editHint(BuildHintMetadata meta) {
        Dialog d = new Dialog(meta == null ? "Add Build Hint" : "Edit Build Hint", new BorderLayout());
        d.setDisposeWhenPointerOutOfBounds(true);
        Container content = new Container(BoxLayout.y());
        content.setUIID(uiid("SettingsDialogContent"));
        TextField key = new TextField(meta == null ? "" : meta.name(), "hint.name");
        key.setUIID(uiid("SettingsField"));
        TextArea value = new TextArea(meta == null ? "" : settings.getBuildHint(meta.name()));
        value.setHint("value");
        value.setUIID(uiid("SettingsArea"));
        value.setRows(5);
        content.add(new Label("Key", uiid("SettingsFieldLabel"))).add(key);
        content.add(new Label("Value", uiid("SettingsFieldLabel"))).add(value);
        Button ok = new Button(new Command("Apply"));
        ok.setUIID(uiid("SettingsPrimary"));
        Button cancel = new Button(new Command("Cancel"));
        cancel.setUIID(uiid("SettingsOutline"));
        content.add(FlowLayout.encloseRight(cancel, ok));
        d.add(BorderLayout.CENTER, content);
        if (ok.getCommand() == d.showDialog()) {
            String k = key.getText() == null ? "" : key.getText().trim();
            if (k.startsWith(SettingsProperties.BUILD_HINT_PREFIX)) {
                k = k.substring(SettingsProperties.BUILD_HINT_PREFIX.length());
            }
            if (k.length() > 0) {
                settings.setBuildHint(k, value.getText() == null ? "" : value.getText());
                renderBuildHints();
                buildShell();
            }
        }
    }

    private void renderExtensions() {
        page.add(pageTitle("Extensions", "Install & update 3rd-party libraries (cn1libs) and native extensions."));
        Container searchCard = new Container(new BorderLayout());
        searchCard.setUIID(uiid("SettingsSearchBox"));
        Label searchIcon = new Label("", uiid("SettingsSearchIcon"));
        searchIcon.setMaterialIcon(FontImage.MATERIAL_SEARCH, 2.8f);
        TextField query = new TextField(extensionFilter, "Search extensions...");
        query.setUIID(uiid("SettingsSearchField"));
        query.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(Component cmp) {
                searchCard.setUIID(uiid("SettingsSearchBoxFocused"));
                searchCard.repaint();
            }

            @Override
            public void focusLost(Component cmp) {
                searchCard.setUIID(uiid("SettingsSearchBox"));
                searchCard.repaint();
            }
        });
        Container results = new Container(BoxLayout.y());
        query.addDataChangedListener((type, index) -> {
            extensionFilter = query.getText() == null ? "" : query.getText();
            renderExtensionList(results);
        });
        searchCard.add(BorderLayout.WEST, searchIcon);
        searchCard.add(BorderLayout.CENTER, query);
        page.add(searchCard);
        page.add(results);
        renderExtensionList(results);
        if (extensionCatalog == null) {
            loadExtensions(results);
        }
    }

    private void renderExtensionList(Container results) {
        results.removeAll();
        List<ExtensionDescriptor> found = extensionCatalog == null ? MavenCentralSearch.curated() : extensionCatalog;
        Container grid = new Container(BoxLayout.y());
        grid.setUIID(uiid("SettingsExtensionGrid"));
        Container row = null;
        int count = 0;
        int columns = extensionColumns();
        for (ExtensionDescriptor d : found) {
            if (matchesExtension(d, extensionFilter)) {
                if (count % columns == 0) {
                    row = new Container(new GridLayout(1, columns));
                    row.setUIID(uiid("SettingsExtensionRow"));
                    grid.add(row);
                }
                row.add(extensionRow(d));
                count++;
            }
        }
        if (count == 0) {
            results.add(new Label(extensionCatalog == null ? "Loading extension catalog..." : "No extensions match the current filter.",
                    uiid("SettingsRowMeta")));
        } else {
            results.add(grid);
        }
        results.revalidate();
    }

    private int extensionColumns() {
        int width = Display.getInstance().getDisplayWidth();
        if (width < 900) {
            return 2;
        }
        return 3;
    }

    private void loadExtensions(Container results) {
        Display.getInstance().startThread(() -> {
            List<ExtensionDescriptor> loaded = null;
            try {
                loaded = loadBundledCn1LibCatalog();
            } catch (Exception ex) {
                Log.e(ex);
            }
            final List<ExtensionDescriptor> catalog = mergeCatalogs(loaded);
            CN.callSerially(() -> {
                extensionCatalog = catalog;
                renderExtensionList(results);
            });

            try {
                List<ExtensionDescriptor> refreshed = ExtensionCatalogMerger.preserveCompatibilityMetadata(
                        fetchCn1LibCatalog(), loaded);
                final List<ExtensionDescriptor> refreshedCatalog = mergeCatalogs(refreshed);
                CN.callSerially(() -> {
                    extensionCatalog = refreshedCatalog;
                    renderExtensionList(results);
                });
            } catch (Exception ex) {
                Log.e(ex);
            }
        }, "SettingsExtensionCatalog").start();
    }

    private List<ExtensionDescriptor> fetchCn1LibCatalog() throws Exception {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(EXTENSIONS_URL);
        req.setPost(false);
        req.setContentType("application/xml");
        NetworkManager.getInstance().addToQueueAndWait(req);
        if (req.getResponseCode() >= 400 || req.getResponseData() == null) {
            throw new java.io.IOException("CN1Libs.xml returned HTTP " + req.getResponseCode());
        }
        Element root = parseExtensionXml(new InputStreamReader(new ByteArrayInputStream(req.getResponseData()), "UTF-8"));
        return parseExtensionRoot(root);
    }

    private List<ExtensionDescriptor> loadBundledCn1LibCatalog() throws Exception {
        InputStream in = getClass().getResourceAsStream("/com/codename1/settings/extensions/CN1Libs.xml");
        if (in == null) {
            return new ArrayList<ExtensionDescriptor>();
        }
        try {
            Element root = parseExtensionXml(new InputStreamReader(in, "UTF-8"));
            return parseExtensionRoot(root);
        } finally {
            Util.cleanup(in);
        }
    }

    private Element parseExtensionXml(InputStreamReader reader) {
        XMLParser parser = new XMLParser();
        parser.setCaseSensitive(true);
        parser.setIncludeWhitespacesBetweenTags(false);
        return parser.parse(reader);
    }

    private List<ExtensionDescriptor> parseExtensionRoot(Element root) {
        ArrayList<ExtensionDescriptor> out = new ArrayList<ExtensionDescriptor>();
        for (int i = 0; i < root.getNumChildren(); i++) {
            ExtensionDescriptor descriptor = parseExtension(root.getChildAt(i));
            if (descriptor != null && descriptor.name().length() > 0) {
                out.add(descriptor);
            }
        }
        Collections.sort(out, new Comparator<ExtensionDescriptor>() {
            @Override
            public int compare(ExtensionDescriptor a, ExtensionDescriptor b) {
                return a.name().compareToIgnoreCase(b.name());
            }
        });
        return out;
    }

    private List<ExtensionDescriptor> mergeCatalogs(List<ExtensionDescriptor> xml) {
        LinkedHashMap<String, ExtensionDescriptor> out = new LinkedHashMap<String, ExtensionDescriptor>();
        for (ExtensionDescriptor d : MavenCentralSearch.curated()) {
            out.put(extensionCatalogKey(d), d);
        }
        if (xml != null) {
            for (ExtensionDescriptor d : xml) {
                String key = extensionCatalogKey(d);
                ExtensionDescriptor existing = out.get(key);
                if (existing == null
                        || d.dependency() != null
                        || existing.fileName().length() == 0 && d.fileName().length() > 0) {
                    out.put(key, d);
                }
            }
        }
        ArrayList<ExtensionDescriptor> merged = new ArrayList<ExtensionDescriptor>(out.values());
        Collections.sort(merged, new Comparator<ExtensionDescriptor>() {
            @Override
            public int compare(ExtensionDescriptor a, ExtensionDescriptor b) {
                return displayExtensionName(a).compareToIgnoreCase(displayExtensionName(b));
            }
        });
        return merged;
    }

    private String extensionCatalogKey(ExtensionDescriptor descriptor) {
        String name = displayExtensionName(descriptor).toLowerCase();
        StringBuilder key = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
                key.append(ch);
            }
        }
        return key.toString();
    }

    private ExtensionDescriptor parseExtension(Element extension) {
        if (extension == null) {
            return null;
        }
        String fileName = extension.getAttribute("fileName");
        String name = normalizedExtensionName(childText(extension, "name"));
        String desc = childText(extension, "description");
        String link = childText(extension, "link");
        String license = childText(extension, "license");
        String platforms = childText(extension, "platforms");
        String author = childText(extension, "contributed");
        String tags = childText(extension, "tags");
        String dependencies = childText(extension, "dependencies");
        String version = childText(extension, "version");
        String status = childText(extension, "status");
        String warning = childText(extension, "warning");
        MavenDependency dep = parseMavenDependency(extension.getFirstChildByTagName("maven"));
        return new ExtensionDescriptor(name, desc, dep, dep != null, fileName, link, license, platforms,
                author, tags, dependencies, version, status, warning);
    }

    private String childText(Element parent, String tag) {
        Element child = parent.getFirstChildByTagName(tag);
        if (child == null || child.getNumChildren() == 0) {
            return "";
        }
        Element text = child.getChildAt(0);
        return text == null || text.getText() == null ? "" : text.getText().trim();
    }

    private MavenDependency parseMavenDependency(Element maven) {
        if (maven == null) {
            return null;
        }
        Element dependency = maven.getFirstChildByTagName("dependency");
        Element source = dependency == null ? maven : dependency;
        String group = childText(source, "groupId");
        String artifact = childText(source, "artifactId");
        String version = childText(source, "version");
        String type = childText(source, "type");
        if (group.length() == 0 || artifact.length() == 0 || version.length() == 0) {
            return null;
        }
        return new MavenDependency(group, artifact, version, type);
    }

    private boolean matchesExtension(ExtensionDescriptor d, String filter) {
        if (filter == null || filter.trim().length() == 0) {
            return true;
        }
        String q = filter.toLowerCase();
        return d.name().toLowerCase().contains(q)
                || d.description().toLowerCase().contains(q)
                || d.fileName().toLowerCase().contains(q)
                || d.license().toLowerCase().contains(q)
                || d.platforms().toLowerCase().contains(q)
                || d.author().toLowerCase().contains(q)
                || d.tags().toLowerCase().contains(q)
                || d.dependencies().toLowerCase().contains(q)
                || (d.dependency() != null && d.dependency().coordinates().toLowerCase().contains(q));
    }

    private Component extensionRow(ExtensionDescriptor descriptor) {
        boolean expanded = Boolean.TRUE.equals(expandedExtensions.get(extensionKey(descriptor)));
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid("SettingsExtensionCard"));
        Container text = new Container(BoxLayout.y());
        text.setUIID(uiid("SettingsExtensionBody"));
        Label title = new Label(displayExtensionName(descriptor), uiid("SettingsExtensionTitle"));
        title.setEndsWith3Points(true);
        title.addPointerReleasedListener(e -> {
            expandedExtensions.put(extensionKey(descriptor), !expanded);
            renderPage();
            animatePage();
        });
        text.add(title);
        Container description = new Container(BoxLayout.y());
        addExtensionDescription(description, descriptor.description(), expanded ? 4 : 2);
        text.add(description);
        Container meta = new Container(new GridLayout(1, 2));
        meta.setUIID(uiid("SettingsExtensionMetaGrid"));
        meta.add(extensionMeta("License", displayLicense(descriptor.license())));
        meta.add(extensionMeta("Platforms", descriptor.platforms()));
        text.add(meta);
        Container tagRow = extensionTags(descriptor);
        if (tagRow.getComponentCount() > 0) {
            text.add(tagRow);
        }
        if (expanded) {
            if (descriptor.warning().length() > 0) {
                text.add(new Label(descriptor.warning(), uiid("SettingsExtensionWarning")));
            }
            if (descriptor.dependency() != null) {
                Label dependency = new Label(displayDependency(descriptor.dependency()), uiid("SettingsExtensionMeta"));
                dependency.setEndsWith3Points(true);
                text.add(dependency);
            } else if (descriptor.fileName().length() > 0) {
                Label file = new Label(descriptor.fileName(), uiid("SettingsExtensionMeta"));
                file.setEndsWith3Points(true);
                text.add(file);
                text.add(new Label("Legacy cn1lib - may be out of date", uiid("SettingsExtensionWarning")));
            }
            if (descriptor.author().length() > 0) {
                Label author = new Label("By " + descriptor.author(), uiid("SettingsExtensionMeta"));
                author.setEndsWith3Points(true);
                text.add(author);
            }
        }
        row.add(BorderLayout.CENTER, text);
        Container actions = new Container(new BorderLayout());
        if (descriptor.dependency() != null) {
            boolean installed = isDependencyInstalled(descriptor.dependency());
            Button add = new Button(installed ? "Installed ✓" : "Download",
                    uiid(installed ? "SettingsSave" : "SettingsExtensionPrimary"));
            add.addActionListener(e -> {
                if (installed) {
                    offerUninstall(descriptor);
                } else {
                    installMavenExtension(descriptor);
                }
            });
            actions.add(BorderLayout.CENTER, add);
        } else if (descriptor.fileName().length() > 0) {
            boolean installed = isLegacyCn1LibInstalled(descriptor);
            Button install = new Button(installed ? "Installed ✓" : "Install",
                    uiid(installed ? "SettingsSave" : "SettingsExtensionPrimary"));
            install.addActionListener(e -> {
                if (installed) {
                    offerUninstall(descriptor);
                } else {
                    installLegacyCn1Lib(descriptor);
                }
            });
            actions.add(BorderLayout.CENTER, install);
        }
        row.add(BorderLayout.SOUTH, actions);
        return row;
    }

    private String normalizedExtensionName(String name) {
        if ("Apple AppTrackingTransparency library".equals(name)) {
            return "Apple AppTrackingTransparency";
        }
        if ("BouncyCastle SDK".equals(name) || "Bouncy Castle SDK".equals(name)) {
            return "BouncyCastle SDK";
        }
        return name == null ? "" : name.trim();
    }

    private String displayExtensionName(ExtensionDescriptor descriptor) {
        return normalizedExtensionName(descriptor.name());
    }

    private String displayLicense(String license) {
        if (license == null) {
            return "";
        }
        String value = license.trim();
        if ("MIT License".equalsIgnoreCase(value)) {
            return "MIT";
        }
        if ("GPL+Classpath Exception".equalsIgnoreCase(value)) {
            return "MIT";
        }
        return value;
    }

    private boolean isDependencyInstalled(MavenDependency dependency) {
        if (binding == null || binding.pom() == null || binding.pom().length() == 0 || dependency == null) {
            return false;
        }
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(ProjectIO.fsUrl(binding.pom()));
            return PomEditor.containsDependency(Util.readToString(in, "UTF-8"), dependency);
        } catch (Exception ex) {
            return false;
        } finally {
            Util.cleanup(in);
        }
    }

    private void addExtensionDescription(Container text, String description, int maxLines) {
        List<String> lines = wrapExtensionText(description, 38, maxLines);
        for (String line : lines) {
            Label l = new Label(line, uiid("SettingsExtensionText"));
            l.setEndsWith3Points(true);
            text.add(l);
        }
    }

    private List<String> wrapExtensionText(String text, int maxChars, int maxLines) {
        ArrayList<String> lines = new ArrayList<String>();
        String remaining = text == null ? "" : text.trim();
        while (remaining.length() > 0 && lines.size() < maxLines) {
            if (remaining.length() <= maxChars) {
                lines.add(remaining);
                break;
            }
            int split = remaining.lastIndexOf(' ', maxChars);
            if (split < maxChars / 2) {
                split = maxChars;
            }
            lines.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        if (lines.size() == 0) {
            lines.add("");
        }
        return lines;
    }

    private String extensionKey(ExtensionDescriptor descriptor) {
        if (descriptor.dependency() != null) {
            return descriptor.dependency().coordinates();
        }
        return descriptor.name() + "|" + descriptor.fileName();
    }

    private void addExtensionMeta(Container text, String label, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        text.add(new Label(label + " " + value.trim(), uiid("SettingsExtensionMeta")));
    }

    private Component extensionMeta(String label, String value) {
        Container c = new Container(BoxLayout.y());
        c.setUIID(uiid("SettingsExtensionMetaColumn"));
        Label l = new Label(label, uiid("SettingsExtensionMetaLabel"));
        Label v = new Label(value == null || value.trim().length() == 0 ? "-" : value.trim(), uiid("SettingsExtensionMetaValue"));
        v.setEndsWith3Points(true);
        c.add(l).add(v);
        return c;
    }

    private Container extensionTags(ExtensionDescriptor descriptor) {
        Container tags = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        tags.setUIID(uiid("SettingsExtensionTagRow"));
        String raw = descriptor.tags();
        if (raw != null && raw.trim().length() > 0) {
            String[] pieces = raw.split("[,;]");
            int added = 0;
            for (String piece : pieces) {
                String tag = piece.trim();
                if (tag.length() > 0) {
                    tags.add(new Label(displayTag(tag), uiid("SettingsExtensionTags")));
                    added++;
                    if (added >= 2) {
                        break;
                    }
                }
            }
        }
        return tags;
    }

    private String displayTag(String tag) {
        String value = tag == null ? "" : tag.trim();
        if ("payment".equalsIgnoreCase(value)) {
            return "PAYMENTS";
        }
        if ("networking".equalsIgnoreCase(value)) {
            return "HARDWARE";
        }
        if ("security".equalsIgnoreCase(value)) {
            return "CRYPTO";
        }
        return value.toUpperCase();
    }

    private String displayDependency(MavenDependency dependency) {
        String version = dependency.version();
        if ("${cn1.version}".equals(version)) {
            version = "current CN1";
        }
        return dependency.groupId() + ":" + dependency.artifactId() + ":" + version
                + (dependency.type().length() == 0 ? "" : ":" + dependency.type());
    }

    private void addDependency(MavenDependency dependency) {
        if (binding.pom() == null || binding.pom().length() == 0) {
            ToastBar.showErrorMessage("No common/pom.xml was bound to this Settings session.");
            return;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            String url = ProjectIO.fsUrl(binding.pom());
            in = FileSystemStorage.getInstance().openInputStream(url);
            String pom = Util.readToString(in, "UTF-8");
            Util.cleanup(in);
            in = null;
            String updated = PomEditor.addDependency(pom, dependency);
            if (updated.equals(pom)) {
                ToastBar.showInfoMessage("Dependency already exists: " + dependency.coordinates());
                return;
            }
            out = FileSystemStorage.getInstance().openOutputStream(url);
            out.write(updated.getBytes("UTF-8"));
            out.flush();
            ToastBar.showInfoMessage("Added " + dependency.coordinates() + " to common/pom.xml");
            renderPage();
            animatePage();
        } catch (Exception ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Failed to update common/pom.xml: " + ex.getMessage());
        } finally {
            Util.cleanup(in);
            Util.cleanup(out);
        }
    }

    private void installMavenExtension(ExtensionDescriptor descriptor) {
        if (confirmCompatibility(descriptor)) {
            addDependency(descriptor.dependency());
        }
    }

    private boolean confirmCompatibility(ExtensionDescriptor descriptor) {
        String warning = descriptor.warning();
        if (warning.length() == 0 && descriptor.hasCompatibilityWarning()) {
            warning = "This extension is marked " + descriptor.status()
                    + " and may not work with current Codename One versions.";
        }
        if (warning.length() == 0 && descriptor.dependency() == null) {
            warning = "This is a legacy cn1lib package. It may be out of date or unsupported by current Codename One versions.";
        }
        return warning.length() == 0 || Dialog.show("Compatibility warning", warning, "Continue", "Cancel");
    }

    private void installLegacyCn1Lib(ExtensionDescriptor descriptor) {
        if (descriptor.fileName().length() == 0) {
            ToastBar.showErrorMessage("This legacy cn1lib entry does not include a downloadable file.");
            return;
        }
        if (!confirmCompatibility(descriptor)) {
            return;
        }
        Display.getInstance().startThread(() -> {
            try {
                ConnectionRequest req = new ConnectionRequest();
                req.setUrl("https://www.codenameone.com/files/" + encodeUrlPath(descriptor.fileName()));
                req.setPost(false);
                NetworkManager.getInstance().addToQueueAndWait(req);
                if (req.getResponseCode() >= 400 || req.getResponseData() == null) {
                    throw new java.io.IOException("Download failed with HTTP " + req.getResponseCode());
                }
                String dir = legacyCn1LibDir();
                FileSystemStorage fs = FileSystemStorage.getInstance();
                fs.mkdir(ProjectIO.fsUrl(dir));
                String dest = dir + "/" + descriptor.fileName();
                OutputStream out = null;
                try {
                    out = fs.openOutputStream(ProjectIO.fsUrl(dest));
                    out.write(req.getResponseData());
                    out.flush();
                } finally {
                    Util.cleanup(out);
                }
                CN.callSerially(() -> {
                    ToastBar.showMessage("Installed " + descriptor.fileName()
                            + " into cn1libs/", FontImage.MATERIAL_CHECK);
                    renderPage();
                    animatePage();
                });
            } catch (Exception ex) {
                Log.e(ex);
                CN.callSerially(() -> ToastBar.showErrorMessage("Failed to install cn1lib: " + ex.getMessage()));
            }
        }, "SettingsLegacyCn1LibInstall").start();
    }

    private boolean isLegacyCn1LibInstalled(ExtensionDescriptor descriptor) {
        return descriptor.fileName().length() > 0
                && FileSystemStorage.getInstance().exists(ProjectIO.fsUrl(legacyCn1LibPath(descriptor)));
    }

    private String legacyCn1LibPath(ExtensionDescriptor descriptor) {
        return legacyCn1LibDir() + "/" + descriptor.fileName();
    }

    private void offerUninstall(ExtensionDescriptor descriptor) {
        if (!Dialog.show("Uninstall extension",
                "Remove " + displayExtensionName(descriptor) + " from this project?",
                "Uninstall", "Cancel")) {
            return;
        }
        if (descriptor.dependency() != null) {
            removeDependency(descriptor.dependency());
        } else {
            String path = legacyCn1LibPath(descriptor);
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (fs.exists(ProjectIO.fsUrl(path))) {
                fs.delete(ProjectIO.fsUrl(path));
            }
            ToastBar.showInfoMessage("Removed " + descriptor.fileName() + " from cn1libs/");
            renderPage();
            animatePage();
        }
    }

    private void removeDependency(MavenDependency dependency) {
        InputStream in = null;
        OutputStream out = null;
        try {
            String url = ProjectIO.fsUrl(binding.pom());
            in = FileSystemStorage.getInstance().openInputStream(url);
            String pom = Util.readToString(in, "UTF-8");
            Util.cleanup(in);
            in = null;
            String updated = PomEditor.removeDependency(pom, dependency);
            if (updated.equals(pom)) {
                ToastBar.showInfoMessage("Dependency is not installed: " + dependency.coordinates());
                return;
            }
            out = FileSystemStorage.getInstance().openOutputStream(url);
            out.write(updated.getBytes("UTF-8"));
            out.flush();
            ToastBar.showInfoMessage("Removed " + dependency.coordinates() + " from common/pom.xml");
            renderPage();
            animatePage();
        } catch (Exception ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Failed to update common/pom.xml: " + ex.getMessage());
        } finally {
            Util.cleanup(in);
            Util.cleanup(out);
        }
    }

    private String legacyCn1LibDir() {
        if (binding.multimoduleRoot() != null && binding.multimoduleRoot().length() > 0) {
            return binding.multimoduleRoot() + "/cn1libs";
        }
        String projectDir = binding.projectDir();
        if (projectDir != null && projectDir.endsWith("/common")) {
            return projectDir.substring(0, projectDir.length() - "/common".length()) + "/cn1libs";
        }
        return projectDir + "/cn1libs";
    }

    private String encodeUrlPath(String path) {
        return path.replace(" ", "%20");
    }

    private void renderAdvanced() {
        page.add(pageTitle("Advanced", "Open project files directly when the structured editors are not enough."));
        Container c = card("Files");
        actionRow(c, "Settings file", binding.settings(), () -> Display.getInstance().execute(ProjectIO.fsUrl(binding.settings())));
        actionRow(c, "Common POM", binding.pom(), () -> Display.getInstance().execute(ProjectIO.fsUrl(binding.pom())));
        if (binding.buildHintsDoc() != null && binding.buildHintsDoc().length() > 0) {
            actionRow(c, "Build-hints source", binding.buildHintsDoc(), () -> Display.getInstance().execute(ProjectIO.fsUrl(binding.buildHintsDoc())));
        }
        page.add(c);
    }

    private Container pageTitle(String title, String sub) {
        Container c = new Container(BoxLayout.y());
        Label heading = new Label(title, uiid("SettingsPageTitle"));
        Label subtitle = new Label(sub, uiid("SettingsSub"));
        c.add(heading);
        c.add(subtitle);
        return c;
    }

    private Component iconDrop() {
        Container wrap = new Container(BoxLayout.y());
        Label iconLabel = new Label("Icon", uiid("SettingsFieldLabel"));
        wrap.add(iconLabel);
        Container drop = new Container(new BorderLayout());
        drop.setUIID(uiid("SettingsIconDrop"));
        Label icon = new Label("", uiid("SettingsIconPreview"));
        Image preview = loadProjectIconPreview();
        if (preview != null) {
            int imageSize = CN.convertToPixels(12f);
            icon.setIcon(preview.scaled(imageSize, imageSize));
        } else {
            icon.setText("M");
        }
        Container text = new Container(BoxLayout.y());
        text.add(new Label(projectIconName(), uiid("SettingsRowTitle")));
        text.add(new Label("Opaque square PNG, 512x512 or 1024x1024.", uiid("SettingsRowMeta")));
        Button replace = new Button("Replace", uiid("SettingsOutline"));
        replace.addActionListener(e -> replaceIcon());
        Container replaceCell = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        replaceCell.setUIID(uiid("SettingsIconAction"));
        replaceCell.add(replace);
        drop.add(BorderLayout.WEST, icon).add(BorderLayout.CENTER, text).add(BorderLayout.EAST, replaceCell);
        wrap.add(drop);
        return wrap;
    }

    private void replaceIcon() {
        CN.openFileChooser(e -> {
            if (e == null || e.getSource() == null) {
                return;
            }
            String source = (String) e.getSource();
            String dest = projectIconPath();
            if (dest == null || dest.length() == 0) {
                ToastBar.showErrorMessage("No Maven project icon path is available.");
                return;
            }
            InputStream in = null;
            OutputStream out = null;
            try {
                FileSystemStorage fs = FileSystemStorage.getInstance();
                String validation = validateReplacementIcon(source);
                if (validation != null) {
                    ToastBar.showErrorMessage(validation);
                    return;
                }
                String dir = dest.substring(0, dest.lastIndexOf('/'));
                fs.mkdir(ProjectIO.fsUrl(dir));
                in = fs.openInputStream(source);
                out = fs.openOutputStream(ProjectIO.fsUrl(dest));
                Util.copy(in, out);
                ToastBar.showMessage("Icon replaced", FontImage.MATERIAL_CHECK);
                buildShell();
            } catch (Exception ex) {
                Log.e(ex);
                ToastBar.showErrorMessage("Failed to replace icon: " + ex.getMessage());
            } finally {
                Util.cleanup(in);
                Util.cleanup(out);
            }
        }, "png");
    }

    private String validateReplacementIcon(String source) throws Exception {
        if (!source.toLowerCase().endsWith(".png")) {
            return "Icon must be a PNG file.";
        }
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(source);
            byte[] sig = new byte[8];
            int read = in.read(sig);
            if (read != 8 || sig[0] != (byte) 0x89 || sig[1] != 0x50 || sig[2] != 0x4e || sig[3] != 0x47
                    || sig[4] != 0x0d || sig[5] != 0x0a || sig[6] != 0x1a || sig[7] != 0x0a) {
                return "Icon must be a valid PNG file.";
            }
        } finally {
            Util.cleanup(in);
        }
        Image img = loadImage(source);
        if (img == null) {
            return "Icon PNG could not be decoded.";
        }
        if (img.getWidth() != img.getHeight()) {
            return "Icon must be square.";
        }
        if (img.getWidth() != 512 && img.getWidth() != 1024) {
            return "Icon must be 512x512 or 1024x1024.";
        }
        int[] rgb = img.getRGB();
        for (int i = 0; i < rgb.length; i++) {
            if ((rgb[i] & 0xff000000) != 0xff000000) {
                return "Icon must be fully opaque.";
            }
        }
        return null;
    }

    private Image loadProjectIconPreview() {
        String path = projectIconPath();
        if (path == null || path.length() == 0) {
            return null;
        }
        return loadImage(ProjectIO.fsUrl(path));
    }

    private Image loadImage(String url) {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(url);
            return Image.createImage(in);
        } catch (Exception ex) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    private String projectIconPath() {
        if (binding == null || binding.projectDir() == null || binding.projectDir().length() == 0) {
            return null;
        }
        String icon = settings == null ? "" : settings.get("codename1.icon", "icon.png");
        if (icon == null || icon.length() == 0) {
            icon = "icon.png";
        }
        if (icon.indexOf("://") > 0 || icon.startsWith("/")) {
            return icon;
        }
        String projectDir = binding.projectDir();
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String commonIcon = projectDir + "/" + icon;
        if (fs.exists(ProjectIO.fsUrl(commonIcon))) {
            return commonIcon;
        }
        String nestedCommonIcon = projectDir + "/common/" + icon;
        if (fs.exists(ProjectIO.fsUrl(nestedCommonIcon))) {
            return nestedCommonIcon;
        }
        return commonIcon;
    }

    private String projectIconName() {
        String path = projectIconPath();
        if (path == null || path.length() == 0) {
            return "icon.png";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private Component divider() {
        Container c = new Container();
        c.setUIID(uiid("SettingsDivider"));
        return c;
    }

    private Component fieldPair(Component left, Component right) {
        Container row = new Container(new GridLayout(1, 2));
        row.setUIID(uiid("SettingsFieldPair"));
        row.add(left).add(right);
        return row;
    }

    private Component staticField(String label, String value) {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        Label fieldLabel = new Label(label, uiid("SettingsFieldLabel"));
        Label val = new Label(value, uiid("SettingsField"));
        val.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL)
                .derive(CN.convertToPixels(1.75f), Font.STYLE_PLAIN));
        fieldGroup.add(fieldLabel).add(val);
        return fieldGroup;
    }

    private Component switchField(String label, String value) {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        Label fieldLabel = new Label(label, uiid("SettingsFieldLabel"));
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid("SettingsToggleRow"));
        row.add(BorderLayout.CENTER, new Label(value, uiid("SettingsRowMeta")));
        Container sw = new Container();
        sw.setUIID(uiid("SettingsSwitch"));
        row.add(BorderLayout.EAST, sw);
        fieldGroup.add(fieldLabel).add(row);
        return fieldGroup;
    }

    private Container card(String title) {
        Container c = new Container(BoxLayout.y());
        c.setUIID(uiid("SettingsCard"));
        c.add(new Label(title, uiid("SettingsCardTitle")));
        return c;
    }

    private void row(Container parent, String label, String value) {
        Container r = new Container(new BorderLayout());
        r.setUIID(uiid("SettingsCardRow"));
        r.add(BorderLayout.WEST, new Label(label, uiid("SettingsRowTitle")));
        r.add(BorderLayout.CENTER, new Label(value == null || value.length() == 0 ? "[not set]" : value, uiid("SettingsRowMeta")));
        parent.add(r);
    }

    private void textRow(Container parent, String label, String key, boolean secret) {
        parent.add(textFieldGroup(label, key, secret));
    }

    private Component textFieldGroup(String label, String key, boolean secret) {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        TextField field = new SettingsTextField(settings.get(key));
        field.setUIID(uiid("SettingsField"));
        field.setEnableInputScroll(false);
        field.setScrollVisible(false);
        if (secret) {
            field.setConstraint(TextField.PASSWORD);
        }
        field.addDataChangedListener((type, index) -> settings.set(key, field.getText()));
        Label fieldLabel = new Label(label, uiid("SettingsFieldLabel"));
        fieldGroup.add(fieldLabel).add(field);
        return fieldGroup;
    }

    private void actionRow(Container parent, String label, String value, Runnable action) {
        Container r = new Container(new BorderLayout());
        r.setUIID(uiid("SettingsCardRow"));
        r.add(BorderLayout.CENTER, new Label(label + ": " + value, uiid("SettingsRowMeta")));
        Button open = new Button("Open", uiid("SettingsOutline"));
        open.setMaterialIcon(FontImage.MATERIAL_OPEN_IN_NEW);
        open.addActionListener(e -> action.run());
        r.add(BorderLayout.EAST, open);
        parent.add(r);
    }

    private void saveSettings() {
        if (settings == null || !settings.isModified()) {
            return;
        }
        try {
            settings.save();
            ToastBar.showInfoMessage("Settings saved");
            buildShell();
        } catch (Exception ex) {
            Log.e(ex);
            ToastBar.showErrorMessage("Failed to save settings: " + ex.getMessage());
        }
    }

    private void go(Section s) {
        section = s;
        buildShell();
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        Preferences.set(PREF_DARK_MODE, darkMode);
        CN.setDarkMode(Boolean.valueOf(darkMode));
        UIManager.getInstance().refreshTheme();
        buildShell();
        form.refreshTheme(false);
        applyFontScale(form);
        form.revalidate();
    }

    private void adjustFontSize(int deltaPx) {
        fontDeltaPx += deltaPx;
        if (fontDeltaPx < -4) {
            fontDeltaPx = -4;
        }
        if (fontDeltaPx > 12) {
            fontDeltaPx = 12;
        }
        Preferences.set(PREF_FONT_DELTA, fontDeltaPx);
        buildShell();
    }

    private void resetFontSize() {
        fontDeltaPx = 0;
        Preferences.set(PREF_FONT_DELTA, fontDeltaPx);
        buildShell();
    }

    private boolean handleFontShortcut(int keyCode) {
        Display display = Display.getInstance();
        if (!display.isControlKeyDown() && !display.isMetaKeyDown()) {
            return false;
        }
        if (keyCode == '+' || keyCode == '=') {
            adjustFontSize(2);
            return true;
        }
        if (keyCode == '-' || keyCode == '_') {
            adjustFontSize(-2);
            return true;
        }
        if (keyCode == '0') {
            resetFontSize();
            return true;
        }
        return false;
    }

    private void handleFontPinch(float scale) {
        if (scale <= 0) {
            return;
        }
        fontPinchAccumulator *= scale;
        while (fontPinchAccumulator >= 1.14f) {
            adjustFontSize(2);
            fontPinchAccumulator /= 1.14f;
        }
        while (fontPinchAccumulator <= 0.88f) {
            adjustFontSize(-2);
            fontPinchAccumulator /= 0.88f;
        }
    }

    private void applyFontScale(Component c) {
        if (fontDeltaPx == 0 || c == null) {
            return;
        }
        applyScaledFont(c);
        if (c instanceof TextArea) {
            applyScaledFont(((TextArea)c).getHintLabel());
        }
        if (c instanceof Container) {
            Container cnt = (Container)c;
            for (int iter = 0; iter < cnt.getComponentCount(); iter++) {
                applyFontScale(cnt.getComponentAt(iter));
            }
        }
    }

    private void applyScaledFont(Component c) {
        if (c == null) {
            return;
        }
        int size = Display.getInstance().convertToPixels(baseFontMm(c.getUIID())) + fontDeltaPx;
        if (size < 8) {
            size = 8;
        }
        boolean bold = isBoldUiid(c.getUIID());
        Font font = nativeFont(bold ? CN.NATIVE_MAIN_BOLD : CN.NATIVE_MAIN_REGULAR,
                size, bold ? Font.STYLE_BOLD : Font.STYLE_PLAIN);
        if (font != null) {
            c.getAllStyles().setFont(font);
        }
    }

    private Font nativeFont(String nativeName, int sizePx, int style) {
        try {
            Font base = Font.createTrueTypeFont(nativeName, nativeName);
            if (base != null) {
                return base.derive(sizePx, style);
            }
        } catch (Exception ex) {
            Log.e(ex);
        }
        Font fallback = Font.getDefaultFont();
        if (fallback != null && fallback.isTTFNativeFont()) {
            try {
                return fallback.derive(sizePx, style);
            } catch (Exception ex) {
                Log.e(ex);
            }
        }
        return null;
    }

    private float baseFontMm(String uiid) {
        String id = stripDark(uiid);
        if ("SettingsPageTitle".equals(id)) {
            return 6f;
        }
        if ("SettingsTitle".equals(id) || "SettingsToolbarBrand".equals(id)) {
            return 3.6f;
        }
        if ("SettingsMark".equals(id)) {
            return 3.8f;
        }
        if ("SettingsAppName".equals(id) || "SettingsPathText".equals(id)) {
            return 3f;
        }
        if ("SettingsSub".equals(id)) {
            return 3.2f;
        }
        if ("SettingsRowMeta".equals(id) || "SettingsRowText".equals(id)) {
            return 2.8f;
        }
        if ("SettingsExtensionMeta".equals(id) || "SettingsExtensionWarning".equals(id)) {
            return 2.6f;
        }
        if ("SettingsFieldLabel".equals(id)) {
            return 2.7f;
        }
        if ("SettingsSectionTag".equals(id)) {
            return 2.3f;
        }
        if ("SettingsRailItem".equals(id) || "SettingsRailItemSelected".equals(id)) {
            return 2.4f;
        }
        if ("SettingsActiveBadge".equals(id) || "SettingsExtensionTags".equals(id)) {
            return 2.2f;
        }
        if ("SettingsCardTitle".equals(id) || "SettingsRowTitle".equals(id)) {
            return 3.3f;
        }
        if ("SettingsPrimary".equals(id) || "SettingsSave".equals(id)
                || "SettingsOutline".equals(id) || "SettingsIconButton".equals(id)
                || "SettingsSmallIconButton".equals(id) || "SettingsPopupLabel".equals(id)) {
            return 3.1f;
        }
        if ("SettingsExtensionTitle".equals(id)) {
            return 4f;
        }
        if ("SettingsExtensionText".equals(id)) {
            return 3.1f;
        }
        if (id.indexOf("Field") >= 0 || "SettingsSearchField".equals(id)) {
            return 3.3f;
        }
        return 3.4f;
    }

    private boolean isBoldUiid(String uiid) {
        String id = stripDark(uiid);
        return id.indexOf("Title") >= 0 || id.indexOf("Primary") >= 0 || id.indexOf("Save") >= 0
                || id.indexOf("Outline") >= 0 || id.indexOf("SectionTag") >= 0;
    }

    private String stripDark(String uiid) {
        if (uiid == null) {
            return "";
        }
        return uiid.endsWith("Dark") ? uiid.substring(0, uiid.length() - 4) : uiid;
    }

    private void installMenuCommands() {
        if (form.getToolbar() == null) {
            return;
        }
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Update", 'U', () -> {
            extensionCatalog = null;
            if (section == Section.EXTENSIONS) {
                renderPage();
                animatePage();
            }
        }));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Save", 'S', () -> saveSettings()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Open Project Folder", 'O', () -> openProjectFolder()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Basic", '1', () -> go(Section.BASIC)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Build Hints", '2', () -> go(Section.BUILD_HINTS)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Extensions", '3', () -> go(Section.EXTENSIONS)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Toggle Dark Mode", 'D', () -> toggleDarkMode()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Increase Font Size", '+', () -> adjustFontSize(2)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Decrease Font Size", '-', () -> adjustFontSize(-2)));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Reset Font Size", '0', () -> resetFontSize()));
        form.getToolbar().addCommandToOverflowMenu(menuCommand("Close", 'Q', () -> Display.getInstance().exitApplication()));
    }

    private Command menuCommand(String name, char shortcut, Runnable action) {
        Command cmd = new Command(name) {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                action.run();
            }
        };
        cmd.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        cmd.setDesktopShortcut(shortcut);
        return cmd;
    }

    private String uiid(String base) {
        return darkMode ? base + "Dark" : base;
    }

    private static final class SettingsTextField extends TextField {
        SettingsTextField(String text) {
            super(text);
        }

        @Override
        public boolean isScrollableY() {
            return false;
        }
    }
}
