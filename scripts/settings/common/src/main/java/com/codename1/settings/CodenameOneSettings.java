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
import com.codename1.annotations.buildhints.*;

@Android(themeMode = AndroidThemeMode.MODERN)
@Build(nativeTheme = NativeThemeMode.MODERN)
@Desktop(height = 820, interactiveScrollbars = true, titleBar = DesktopTitleBar.NATIVE, width = 1260)
@Ios(themeMode = IosThemeMode.MODERN)
public class CodenameOneSettings extends Lifecycle {
    public enum Section { BASIC, BUILD_HINTS, EXTENSIONS, ADVANCED }

    private static final String PREF_DARK_MODE = "settings.darkMode";
    private static final String PREF_FONT_DELTA = "settings.fontDeltaPx";
    private static final String EXTENSIONS_URL = "https://www.codenameone.com/files/CN1Libs.xml";

    private ProjectBinding binding;
    private SettingsProperties settings;
    private BuildHintCatalog buildHints = BuildHintCatalog.load();
    /// hint name -> the annotation attribute that declares it, e.g. "@Ios(pods)".
    /// Empty when the project has not been built, or declares none.
    private java.util.Map<String, String> annotationOwnedHints = new java.util.HashMap<>();
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
            buildHints = BuildHintCatalog.load();
            annotationOwnedHints = loadAnnotationOwnedHints();
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
        // FIVE rows for the nine fields below, not four. GridLayout sizes itself from the declared
        // row count, so a fifth row forced at layout time contributes nothing to the preferred
        // height -- adding the watch main, the standalone switch and the TV main to a four-row
        // grid left the last row compressed or clipped.
        Container grid = new Container(new GridLayout(5, 2));
        grid.setUIID(uiid("SettingsFieldGrid"));
        grid.add(textFieldGroup("Title", "codename1.displayName", false));
        grid.add(textFieldGroup("Description", "codename1.description", false));
        grid.add(textFieldGroup("Version", "codename1.version", false));
        grid.add(textFieldGroup("Vendor", "codename1.vendor", false));
        grid.add(textFieldGroup("Package Name", "codename1.packageName", false));
        grid.add(textFieldGroup("Main Class", "codename1.mainName", false));
        // Secondary entry points. Declaring a watch lifecycle class is the whole
        // opt-in for the Apple Watch and Wear OS apps -- there are no wearable
        // build hints. Both take a fully-qualified class name, unlike the phone
        // main class which is a simple name resolved against the package.
        grid.add(textFieldGroup("Watch Main Class", "codename1.watchMain", false));
        // The one thing about the watch build that cannot be derived from the project, so it needs
        // a control. Without it this page could only ever produce the companion configuration --
        // and since this change also retired the old wearable distribution/standalone hints, the
        // standalone Wear product and standalone Apple distribution were unreachable from the UI
        // and had to be set by hand in the properties file.
        grid.add(switchGroup("Standalone Watch App", "codename1.watchStandalone"));
        grid.add(textFieldGroup("TV Main Class", "codename1.tvMain", false));
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
            // The same ownership rule as the catalog rows. Withholding the row's
            // controls and leaving this form open is no protection at all: typing
            // ios.teamId here -- or any alias of it -- writes exactly the second
            // declaration the row was hiding, and the next build refuses the
            // project. Canonical, so an alias of an annotation-owned hint is
            // caught too.
            String ownedBy = annotationOwnedHints.get(
                    com.codename1.build.shared.BuildHints.canonicalName(k));
            if (ownedBy != null) {
                ToastBar.showErrorMessage(k + " is set by " + ownedBy
                        + " on the main class. Change it there -- declaring it here as well "
                        + "fails the build.");
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
        // Look the hint up by its canonical name: a deprecated alias configures the
        // same effective setting, so cn1.androidTheme is owned whenever an
        // annotation owns and.themeMode. Matching on the exact name left the alias
        // row offering Add, which would create the second declaration the next
        // build refuses through the alias conflict check.
        String ownedBy = annotationOwnedHints.get(
                com.codename1.build.shared.BuildHints.canonicalName(meta.name()));
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
        if (ownedBy != null) {
            metaLine.add(new Label(ownedBy, uiid("SettingsRowMeta")));
        }
        text.add(name).add(metaLine);
        if (ownedBy != null) {
            // Set by an annotation on the main class. Editing it here would write a
            // second declaration and the next build would refuse the project, so the
            // value is shown and the editor is withheld.
            boolean duplicate = active;
            TextArea owned = new TextArea(duplicate
                    ? "Declared BOTH here and by " + ownedBy + " on the main class. The next "
                      + "build refuses that. Remove the properties declaration with the button "
                      + "on the right, or delete the annotation attribute."
                    : "Set by " + ownedBy + " on the main class. "
                      + "Change it there -- declaring it here as well fails the build.");
            owned.setUIID(uiid("SettingsRowText"));
            owned.setEditable(false);
            owned.setFocusable(false);
            text.add(owned);
            if (duplicate) {
                // The properties declaration exists AND an annotation owns the
                // hint, so this row is the build failure. Withholding every
                // control left the tool able to report the problem and unable to
                // fix it; the value stays uneditable -- editing it would only
                // move the conflict -- but removing it is exactly the resolution,
                // so that button stays.
                Container header = new Container(new BorderLayout());
                header.add(BorderLayout.CENTER, text);
                header.add(BorderLayout.EAST, removeHintButton(meta));
                row.add(header);
            } else {
                row.add(text);
            }
        } else if (active) {
            text.add(activeHintEditor(meta, value, effectiveType));
        } else {
            Container controls = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
            controls.setUIID(uiid("SettingsHintEditor"));
            Button add = new Button("Add", uiid("SettingsOutline"));
            add.setMaterialIcon(FontImage.MATERIAL_ADD, 1.2f);
            String seed = defaultHintValue(meta);
            if (seed != null) {
                add.addActionListener(e -> {
                    settings.setBuildHint(meta.name(), seed);
                    renderBuildHintsList();
                    animatePage();
                });
                controls.add(add);
            } else {
                // Nothing safe to seed. The build decides this hint's value
                // itself when the line is ABSENT -- android.targetSDKVersion is
                // computed from the installed platforms -- so writing a
                // placeholder does not create an unset hint, it overrides the
                // computation with a value nobody chose. `0` there selects the
                // legacy android-14 target and emits targetSdkVersion="0"; an
                // empty string is no better for a hint whose presence is the
                // switch, which is how facebook.appId=\"\" would enable Facebook
                // with no ID.
                //
                // So the row asks for a value and writes nothing until it has
                // one. Add is what reveals the field, not what saves.
                TextField pending = new TextField("", "value");
                pending.setUIID(uiid("SettingsField"));
                configureHintField(pending, meta);
                Button save = new Button("Save", uiid("SettingsOutline"));
                save.addActionListener(e -> {
                    String typed = pending.getText() == null ? "" : pending.getText().trim();
                    if (typed.length() == 0) {
                        ToastBar.showErrorMessage(meta.name()
                                + " has no default -- enter the value you want.");
                        return;
                    }
                    if (!isValidHintValue(meta, typed)) {
                        ToastBar.showErrorMessage(typed + " is not a valid value for "
                                + meta.name() + ".");
                        return;
                    }
                    settings.setBuildHint(meta.name(), canonicalHintValue(meta, typed));
                    renderBuildHintsList();
                    animatePage();
                });
                add.addActionListener(e -> {
                    controls.removeAll();
                    controls.add(pending).add(save);
                    controls.getComponentForm().revalidate();
                    pending.startEditingAsync();
                });
                controls.add(add);
            }
            Container header = new Container(new BorderLayout());
            header.add(BorderLayout.CENTER, text);
            header.add(BorderLayout.EAST, controls);
            row.add(header);
        }
        if (active && ownedBy == null) {
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
                    // Stored in the domain's own spelling. Accepting `INTERNALONLY`
                    // and writing it back verbatim marked the value valid and then
                    // failed the Android build, because the builder copies it into
                    // the case-sensitive android:installLocation attribute. What
                    // the developer meant is unambiguous, and only one spelling of
                    // it works everywhere.
                    settings.setBuildHint(meta.name(), canonicalHintValue(meta, next));
                    valueField.setUIID(uiid("SettingsField"));
                } else {
                    valueField.setUIID(uiid("SettingsFieldError"));
                }
                valueField.repaint();
            });
            controls.add(BorderLayout.CENTER, valueField);
        }
        controls.add(BorderLayout.EAST, removeHintButton(meta));
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

    /// Deletes this hint's properties declaration.
    ///
    /// One implementation, used by the ordinary editor and by the conflict row:
    /// removing the declaration is the resolution in both, and the second copy
    /// this replaced was the reason the conflict row had no way out at all.
    private Button removeHintButton(BuildHintMetadata meta) {
        Button remove = new Button("", uiid("SettingsSmallIconButton"));
        remove.setMaterialIcon(FontImage.MATERIAL_DELETE, 2.2f);
        remove.addActionListener(e -> {
            settings.removeBuildHint(meta.name());
            renderBuildHintsList();
            animatePage();
        });
        return remove;
    }

    /// The value Add should write, or null when there is nothing safe to write.
    ///
    /// The builder's OWN default when the catalog records one: seeding a
    /// type-wide placeholder instead writes a value the project did not have --
    /// android.NotificationChannel.importance defaults to 2, and Add persisting 0
    /// silences the channel before the user has typed anything. Adding a hint
    /// should start from what the build already does.
    ///
    /// Null when it records none, because for those the ABSENCE of the line is
    /// itself the configuration -- android.targetSDKVersion is computed from the
    /// installed platforms when unset, and a placeholder overrides that
    /// computation rather than leaving it alone. A boolean is the one exception:
    /// its two values are the whole domain, so `true` is a real choice and is
    /// what adding the hint means.
    private String defaultHintValue(BuildHintMetadata meta) {
        String catalogDefault = meta.defaultValue();
        if (catalogDefault != null && catalogDefault.length() > 0) {
            return catalogDefault;
        }
        if (meta.type() == BuildHintType.BOOLEAN) {
            return "true";
        }
        return null;
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

    /// `value` in the spelling the catalog declares, or `value` when the domain
    /// is open or does not recognise it.
    ///
    /// Only the spelling changes, never the choice: an accepted alias resolves to
    /// the canonical value it names, which is the one every reader accepts.
    private String canonicalHintValue(BuildHintMetadata meta, String value) {
        if (value == null || value.length() == 0 || meta.values().isEmpty()) {
            return value;
        }
        com.codename1.build.shared.BuildHints.Hint hint =
                com.codename1.build.shared.BuildHints.byName(meta.name());
        if (hint == null || hint.values().isEmpty()) {
            return value;
        }
        String canonical = hint.canonicalValue(value);
        return canonical == null ? value : canonical;
    }

    private boolean isValidHintValue(BuildHintMetadata meta, String value) {
        if (value == null || value.trim().length() == 0) {
            return true;
        }
        String v = value.trim();
        // A closed value domain is the one case where a wrong value is certain to
        // be wrong: the builder compares against these strings and silently uses
        // its default when it recognises none of them.
        //
        // Through the catalog's own canonicalisation rather than the picklist,
        // because a domain can accept spellings that are not offered as choices:
        // ios.themeMode=flat and and.themeMode=material are what the runtime
        // compares against, and rejecting them told a developer that a working
        // configuration was invalid and then refused to save the edit.
        if (!meta.values().isEmpty()) {
            com.codename1.build.shared.BuildHints.Hint hint =
                    com.codename1.build.shared.BuildHints.byName(meta.name());
            if (hint != null && !hint.values().isEmpty()) {
                return hint.canonicalValue(v) != null;
            }
            for (String allowed : meta.values()) {
                if (allowed.equalsIgnoreCase(v)) {
                    return true;
                }
            }
            return false;
        }
        if (meta.type() == BuildHintType.BOOLEAN) {
            return "true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v);
        }
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

    /**
     * A boolean setting, stored as the string {@code "true"}/{@code "false"} the build hints use.
     *
     * <p>Shaped like {@link #textFieldGroup} so it sits in the same grid: label above, control
     * below. Anything other than {@code "true"} reads as off, which matches how the builders parse
     * these -- they compare against {@code "true"} rather than parsing a boolean.</p>
     */
    private Component switchGroup(String label, String key) {
        Container fieldGroup = new Container(BoxLayout.y());
        fieldGroup.setUIID(uiid("SettingsFieldGroup"));
        Switch sw = new Switch(uiid("SettingsSwitch"));
        sw.setValue("true".equals(settings.get(key)));
        sw.addActionListener(e -> settings.set(key, sw.isValue() ? "true" : "false"));
        fieldGroup.add(new Label(label, uiid("SettingsFieldLabel"))).add(sw);
        return fieldGroup;
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

    /// Reads the hints the main class's annotations declare.
    ///
    /// The annotation processor writes this file into `target/classes` on every
    /// build and deletes it when the last annotation goes away, so it is the
    /// authoritative statement of what the annotations currently declare -- and
    /// it carries `cn1.buildHints.origin.<name>`, which names the attribute.
    ///
    /// This matters because a hint declared by an annotation must not also be
    /// written into `codenameone_settings.properties`: the next build fails with
    /// the duplicate-declaration error. Without this the Add button would create
    /// exactly that, silently, for any hint the generated project ships as an
    /// annotation.
    ///
    /// An unbuilt project has no file and no annotation-owned hints, which is the
    /// same conservative answer this tool gave before.
    private java.util.Map<String, String> loadAnnotationOwnedHints() {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        if (binding == null || binding.projectDir() == null) {
            return out;
        }
        String path = binding.projectDir() + "/target/classes/META-INF/codenameone/build-hints.properties";
        InputStream in = null;
        try {
            // Always start from the source, because it is the only current
            // statement of what the annotations declare. The manifest is a build
            // artifact and goes stale in both directions: absent right after
            // cn1:migrate-build-hints, and out of date the moment an attribute is
            // added to a project that was built earlier. Trusting it alone left
            // the newly annotated hint looking unowned, and Add then wrote the
            // duplicate declaration the next build refuses.
            //
            // The manifest supplies origins, but it cannot ADD ownership the
            // source does not show. An attribute deleted from the main class and
            // not yet rebuilt is exactly that case: the source is right, the
            // manifest is a build old, and taking the union kept Add and the
            // editor hidden for a hint nothing owns any more -- until the user
            // happened to rebuild, with nothing to suggest that was the fix.
            java.util.Map<String, String> fromSource = annotationOwnedHintsFromSource();
            if (fromSource != null) {
                out.putAll(fromSource);
            }

            String url = ProjectIO.fsUrl(path);
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (!fs.exists(url)) {
                return out;
            }
            in = fs.openInputStream(url);
            String text = Util.readToString(in, "ISO-8859-1");
            String originPrefix = "cn1.buildHints.origin.";
            for (String line : com.codename1.util.StringUtil.tokenize(text, "\n")) {
                String t = line.trim();
                if (!t.startsWith(originPrefix)) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq > originPrefix.length()) {
                    String hint = t.substring(originPrefix.length(), eq).trim();
                    String canonical =
                            com.codename1.build.shared.BuildHints.canonicalName(hint);
                    // Only for a hint the source still declares -- unless there
                    // was no source to read, where the manifest is all there is
                    // and is better than nothing.
                    if (fromSource == null || fromSource.containsKey(canonical)) {
                        out.put(canonical, t.substring(eq + 1).trim());
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(ex);
        } finally {
            Util.cleanup(in);
        }
        return out;
    }

    /// Reads the build hint annotations straight off the main class.
    ///
    /// Only the attribute *names* are needed -- what each hint is set to does not
    /// matter, only that an annotation owns it -- so this scans the annotation
    /// list above the class declaration for `name =` at the top level of each
    /// annotation's parentheses and maps those to hint names through the catalog.
    /// Values are skipped wholesale, so a comma or bracket inside a string cannot
    /// confuse it.
    /// The hints the main class's SOURCE declares, or null when no source file
    /// for it could be read at all.
    private java.util.Map<String, String> annotationOwnedHintsFromSource() {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        String main = settings == null ? null : settings.get("codename1.mainName");
        String pkg = settings == null ? null : settings.get("codename1.packageName");
        if (binding == null || binding.projectDir() == null || main == null || main.isEmpty()) {
            return out;
        }
        String rel = (pkg == null || pkg.isEmpty() ? "" : pkg.replace('.', '/') + "/") + main;
        for (String ext : new String[]{".java", ".kt"}) {
            for (String root : new String[]{"/src/main/java/", "/src/main/kotlin/", "/src/"}) {
                String path = binding.projectDir() + root + rel + ext;
                String text = readIfPresent(path);
                if (text != null && ext.equals(".java")) {
                    text = decodeUnicodeEscapes(text);
                }
                // The conventional path has to DECLARE the class, not merely
                // exist. Moving a Kotlin main class into a differently named file
                // can leave the old Main.kt behind holding something else, and
                // returning on its mere existence skipped the search below --
                // reporting the annotated hints as unowned, which is the state
                // that lets Add write the duplicate.
                if (text == null || !declaresClass(text, main, pkg, ext.equals(".kt"))) {
                    continue;
                }
                collectOwnedHints(text, out, ext.equals(".kt"),
                        otherProjectSources(binding.projectDir(), path));
                return out;
            }
        }
        // Those three roots are a convention, not the truth: a module can add a
        // source root, and Kotlin does not require a file to be named after the
        // class it declares. Falling straight through to null here let the caller
        // trust a stale manifest -- which is the bug the source scan was added to
        // fix, reappearing for anyone whose layout is merely unusual. So look
        // properly before giving up.
        String found = findMainClassSource(binding.projectDir(), main, pkg);
        if (found != null) {
            collectOwnedHints(found, out, lastSourceWasKotlin,
                    otherProjectSources(binding.projectDir(), lastSourcePath));
            return out;
        }
        // Genuinely no source. Distinct from "found and declares nothing", and
        // the caller has to tell them apart before letting the source overrule
        // the manifest.
        return null;
    }

    /// Set by findMainClassSource, since it decides the language by what it finds.
    private boolean lastSourceWasKotlin;

    /// The path findMainClassSource read, so it can be left out of the sweep for
    /// declarations made elsewhere.
    private String lastSourcePath;

    /// The directories a module's MAIN sources are compiled from, by
    /// convention, as candidates to be filtered by what exists.
    ///
    /// An allow-list rather than a walk of the whole project with exclusions.
    /// The exclusions were never going to be complete -- `src/test` was
    /// followed by `src/testFixtures`, then `src/main/resources`, then
    /// `src/main/templates` and `src/main/proto` -- because "not a source root"
    /// is not a property of a directory's name. Naming the roots instead makes
    /// every one of those wrong by construction.
    ///
    /// `target/generated-sources` is a root: Maven plugins add it, so a
    /// declaration there is one the compiler sees. Nothing else under an output
    /// directory is reachable, which also retires the `build` special case --
    /// starting inside a source root means com.codename1.build.shared is walked
    /// as the package it is, with no rule needed to tell it from an output
    /// directory.
    ///
    /// The flat `src` is a candidate only where there is no `src/main`, since
    /// that is the layout it belongs to; in a Maven layout it would drag the
    /// test sets back in.
    ///
    /// KNOWN LIMIT, and the reason it is acceptable: a module that configures a
    /// custom root in its POM is not covered, because this tool has no resolved
    /// project to ask and its POM handling is deliberately string surgery rather
    /// than an XML model. Missing a peer means a shadowing type goes unseen, so
    /// a hint reads as annotation-owned and its editor stays hidden -- annoying,
    /// but it cannot write the duplicate declaration that fails the next build,
    /// which is what including a non-source directory could.
    static java.util.List<String> candidateSourceRoots(String projectDir, boolean hasSrcMain) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (projectDir == null) {
            return out;
        }
        out.add(projectDir + "/src/main/java");
        out.add(projectDir + "/src/main/kotlin");
        if (!hasSrcMain) {
            out.add(projectDir + "/src");
        }
        out.add(projectDir + "/target/generated-sources");
        out.add(projectDir + "/build/generated-sources");
        return out;
    }

    /// The roots a POM declares: `<sourceDirectory>`, and the source lists the
    /// Kotlin and build-helper plugins take.
    ///
    /// A string read, like the rest of this tool's POM handling. It is looking
    /// for plain elements, and what it cannot resolve -- a `${property}` path --
    /// it leaves alone rather than guessing.
    ///
    /// A declared root under a test tree is dropped: those are configured
    /// through the same elements, and one of them shadowing a production type is
    /// the failure this list exists to avoid.
    static java.util.List<String> declaredSourceRoots(String pomText) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (pomText == null) {
            return out;
        }
        for (String element : new String[] {"sourceDirectory", "sourceDir", "source"}) {
            for (String value : elementValues(pomText, element)) {
                String path = value.trim().replace('\\', '/');
                if (path.isEmpty() || path.indexOf('$') >= 0 || looksLikeATestRoot(path)) {
                    continue;
                }
                if (!out.contains(path)) {
                    out.add(path);
                }
            }
        }
        return out;
    }

    /// Whether a declared path is a test tree, by the same convention the source
    /// sets follow: a `test` segment, or one that says test the way
    /// `testFixtures` and `integrationTest` do.
    private static boolean looksLikeATestRoot(String path) {
        for (String segment : com.codename1.util.StringUtil.tokenize(path, "/")) {
            if ("test".equals(segment)
                    || (segment.length() > 4 && segment.startsWith("test")
                        && Character.isUpperCase(segment.charAt(4)))
                    || segment.endsWith("Test") || segment.endsWith("Tests")) {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<String> elementValues(String xml, String name) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String open = "<" + name + ">";
        String shut = "</" + name + ">";
        int at = xml.indexOf(open);
        while (at >= 0) {
            int close = xml.indexOf(shut, at + open.length());
            if (close < 0) {
                break;
            }
            out.add(xml.substring(at + open.length(), close));
            at = xml.indexOf(open, close + shut.length());
        }
        return out;
    }

    /// Those of them that are there, plus whatever the POM declares.
    private java.util.List<String> mainSourceRoots(String projectDir) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        boolean hasSrcMain = projectDir != null
                && fs.isDirectory(ProjectIO.fsUrl(projectDir + "/src/main"));
        java.util.List<String> candidates =
                new java.util.ArrayList<>(candidateSourceRoots(projectDir, hasSrcMain));
        // A module may put its sources somewhere else entirely, and the main
        // class is the one file this list cannot afford to miss: without it
        // nothing knows which hints an annotation owns, and Add writes the
        // duplicate the next build refuses.
        for (String declared : declaredSourceRoots(pomText())) {
            String path = declared.startsWith("/") || declared.indexOf(':') == 1
                    ? declared : projectDir + "/" + declared;
            if (!candidates.contains(path)) {
                candidates.add(path);
            }
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String candidate : candidates) {
            if (fs.isDirectory(ProjectIO.fsUrl(candidate))) {
                out.add(candidate);
            }
        }
        return out;
    }

    /// The bound POM's text, read once per session.
    private String pomText() {
        if (!pomTextRead) {
            pomTextRead = true;
            if (binding != null && binding.pom() != null && !binding.pom().isEmpty()) {
                pomText = readIfPresentRaw(binding.pom());
            }
        }
        return pomText;
    }

    private boolean pomTextRead;
    private String pomText;

    /// The text of every OTHER source in the project, bounded.
    ///
    /// For the declarations that are not file-scoped and so can decide what a
    /// name in the main source means: a `typealias` naming one of our
    /// annotations, and a type whose name shadows an on-demand import of one.
    /// Java as well as Kotlin, because the second of those is a Java rule too --
    /// a `p/Ios.java` beside the main class is what `@Ios` means there,
    /// whatever the wildcard import says.
    ///
    /// Bounded in files read and in directories walked, because this runs when
    /// the tool opens a project and a source tree is not a search index; a
    /// project past the bound simply keeps the earlier behaviour for whatever
    /// was declared in a file nobody reached.
    private java.util.List<PeerSource> otherProjectSources(String projectDir, String exclude) {
        java.util.List<PeerSource> out = new java.util.ArrayList<>();
        if (projectDir == null) {
            return out;
        }
        java.util.List<String> queue = new java.util.ArrayList<>(mainSourceRoots(projectDir));
        for (int i = 0; i < queue.size() && i < 4000 && out.size() < 200; i++) {
            String dir = queue.get(i);
            String[] children;
            try {
                children = FileSystemStorage.getInstance().listFiles(ProjectIO.fsUrl(dir));
            } catch (Exception ex) {
                continue;
            }
            if (children == null) {
                continue;
            }
            for (String child : children) {
                String name = child.endsWith("/") ? child.substring(0, child.length() - 1) : child;
                String path = dir + "/" + name;
                if (FileSystemStorage.getInstance().isDirectory(ProjectIO.fsUrl(path))) {
                    if (!name.startsWith(".")) {
                        queue.add(path);
                    }
                    continue;
                }
                if (!name.endsWith(".kt") && !name.endsWith(".java")) {
                    continue;
                }
                if (path.equals(exclude) || out.size() >= 200) {
                    continue;
                }
                String text = readIfPresent(path);
                if (text != null) {
                    boolean kotlinPeer = name.endsWith(".kt");
                    // Java's escapes are translated before anything is
                    // tokenized, so a peer declaring an escaped package name is
                    // in the package it decodes to -- and reading it literally
                    // put it in another one, where it shadows nothing.
                    out.add(new PeerSource(
                            kotlinPeer ? text : decodeUnicodeEscapes(text), kotlinPeer));
                }
            }
        }
        return out;
    }

    /// The text of the file declaring `main` in `pkg`, found by searching, or null.
    ///
    /// Walks the project for a `.java` or `.kt` file that declares the class, so
    /// a configured source root or a Kotlin file whose name differs from its
    /// class is found anyway. Bounded in depth and in how many files it will
    /// open, because this runs when Settings opens a project and a source tree is
    /// not a search index.
    private String findMainClassSource(String projectDir, String main, String pkg) {
        if (projectDir == null || main == null || main.isEmpty()) {
            return null;
        }
        // Collected first, then examined in two passes. Deciding as we walk made
        // the answer depend on directory order: the budget could be spent on
        // unrelated files before reaching the one Kotlin source whose name
        // differs from its class -- which is the only layout this fallback exists
        // for, so exactly the case it would drop.
        java.util.List<String> named = new java.util.ArrayList<>();
        java.util.List<String> others = new java.util.ArrayList<>();
        java.util.List<String> queue = new java.util.ArrayList<>(mainSourceRoots(projectDir));
        for (int i = 0; i < queue.size() && i < 4000; i++) {
            String dir = queue.get(i);
            String[] children;
            try {
                children = FileSystemStorage.getInstance().listFiles(ProjectIO.fsUrl(dir));
            } catch (Exception ex) {
                continue;
            }
            if (children == null) {
                continue;
            }
            for (String child : children) {
                String name = child.endsWith("/") ? child.substring(0, child.length() - 1) : child;
                String path = dir + "/" + name;
                if (FileSystemStorage.getInstance().isDirectory(ProjectIO.fsUrl(path))) {
                    // The same roots as the peer sweep, so the two cannot
                    // disagree about where this module's sources are.
                    if (!name.startsWith(".")) {
                        queue.add(path);
                    }
                    continue;
                }
                if (name.equals(main + ".java") || name.equals(main + ".kt")) {
                    named.add(path);
                } else if (name.endsWith(".kt")) {
                    // Only Kotlin: Java requires a public type to be named after
                    // its file, so a differently named .java cannot declare the
                    // main class of an application.
                    others.add(path);
                }
            }
        }
        String hit = firstDeclaring(named, main, pkg, named.size());
        return hit != null ? hit : firstDeclaring(others, main, pkg, 400);
    }


    /// The text of the first of `paths` that declares `main` in `pkg`, opening at
    /// most `budget` of them.
    private String firstDeclaring(java.util.List<String> paths, String main, String pkg,
                                  int budget) {
        int opened = 0;
        for (String path : paths) {
            if (opened++ >= budget) {
                return null;
            }
            String text = readIfPresent(path);
            if (text != null && !path.endsWith(".kt")) {
                text = decodeUnicodeEscapes(text);
            }
            if (text == null || !declaresClass(text, main, pkg, path.endsWith(".kt"))) {
                continue;
            }
            lastSourceWasKotlin = path.endsWith(".kt");
            lastSourcePath = path;
            return text;
        }
        return null;
    }

    /// Whether `text` declares `main` in `pkg`, judged by the package statement
    /// and a `class`/`object` declaration rather than by where the file sits.
    static boolean declaresClass(String text, String main, String pkg) {
        return declaresClass(text, main, pkg, true);
    }

    /// Whether `text` declares `main` in `pkg`, judged by the package statement
    /// and a `class`/`object` declaration rather than by where the file sits.
    ///
    /// Found in CODE: a `// class Main` left over from an edit, or those words
    /// inside a string, would otherwise make an unrelated file answer for the
    /// main class -- ownership then reads as empty and Settings offers Add for a
    /// hint the real main class already annotates.
    /// The package `source` declares, or "" for the default package.
    static String declaredPackageIn(String text, boolean kotlin) {
        int pkgAt = nextMarker(text, "package", 0, kotlin);
        while (pkgAt >= 0) {
            int after = pkgAt + "package".length();
            if (after < text.length() && !continuesAName(text.charAt(after))
                    && (pkgAt == 0 || !continuesAName(text.charAt(pkgAt - 1)))) {
                return qualifiedNameAt(text, after, kotlin);
            }
            pkgAt = nextMarker(text, "package", after, kotlin);
        }
        return "";
    }

    static boolean declaresClass(String text, String main, String pkg, boolean kotlin) {
        String declaredPkg = "";
        int pkgAt = nextMarker(text, "package", 0, kotlin);
        while (pkgAt >= 0) {
            int after = pkgAt + "package".length();
            if (after < text.length() && !continuesAName(text.charAt(after))
                    && (pkgAt == 0 || !continuesAName(text.charAt(pkgAt - 1)))) {
                // Live tokens, as the processor-side helper reads it.
                // `package /* generated */ com.example;` is legal, and taking the
                // remainder of the text and trimming it started the name at the
                // comment -- so the real main source was rejected by both the
                // conventional lookup and the fallback search.
                // Component by component, exactly as the import reader does.
                // `package com /* generated */ . example;` is legal, and reading
                // the name as one contiguous run recorded `com` and rejected the
                // real main source.
                declaredPkg = qualifiedNameAt(text, after, kotlin);
                break;
            }
            pkgAt = nextMarker(text, "package", after, kotlin);
        }
        if (!(pkg == null || pkg.isEmpty() ? "" : pkg).equals(declaredPkg)) {
            return false;
        }
        // At the TOP level. An application's main class is not nested, and
        // accepting a nested one let an unrelated `class Outer { class Main }`
        // in the same package end the search on the wrong file -- so the
        // annotations on the real main class were never read, and Settings
        // offered Add for a hint that is already annotated.
        int depth = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'' || c == '/' || c == '`') {
                int skipped = skipNonCode(text, i, kotlin);
                if (skipped > i) {
                    i = skipped;
                    continue;
                }
            }
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            if (depth != 0 || !continuesAName(c)
                    || (i > 0 && continuesAName(text.charAt(i - 1)))) {
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < text.length() && continuesAName(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(i, wordEnd);
            if ("class".equals(word) || "object".equals(word)) {
                // Every legal separator, not just a space: `class\nMain` and
                // `class /* why */ Main` are both valid Java and Kotlin, and
                // stopping at a newline read the declaration as unnamed.
                int n = nextLiveChar(text, wordEnd, kotlin);
                if (n >= 0) {
                    // Kotlin lets the name be ESCAPED in backticks, and the
                    // binary name -- which is what codename1.mainName holds --
                    // is the text between them. Reading it with the identifier
                    // rule recorded an empty name, so the real main source was
                    // rejected, nothing knew which hints an annotation already
                    // owns, and Settings offered Add for one of them.
                    int end = n;
                    String declared;
                    if (kotlin && text.charAt(n) == '`') {
                        int close = text.indexOf('`', n + 1);
                        declared = close < 0 ? null : text.substring(n + 1, close);
                    } else {
                        while (end < text.length() && continuesAName(text.charAt(end))) {
                            end++;
                        }
                        declared = text.substring(n, end);
                    }
                    if (main.equals(declared)) {
                        return true;
                    }
                }
            }
            i = wordEnd;
        }
        return false;
    }

    /// The POM as bytes-to-ISO-8859-1, used only to find the encoding
    /// declaration -- which is ASCII wherever it appears.
    private String readIfPresentRaw(String path) {
        InputStream in = null;
        try {
            String url = ProjectIO.fsUrl(path);
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            return new String(Util.readInputStream(in), "ISO-8859-1");
        } catch (Exception ex) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    private String readIfPresent(String path) {
        InputStream in = null;
        try {
            String url = ProjectIO.fsUrl(path);
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            byte[] bytes = Util.readInputStream(in);
            // What the project SAYS it is written in, when it says. The guess
            // below can only tell UTF-8 from a single-byte encoding, so a
            // multibyte one such as Shift_JIS came back as mojibake and its
            // non-ASCII names never matched.
            String declared = declaredSourceEncoding();
            if (declared != null) {
                try {
                    return new String(bytes, declared);
                } catch (Exception unsupported) {
                    // Named an encoding this runtime does not have. Guessing is
                    // better than failing to read the file at all.
                }
            }
            // UTF-8 where the file is UTF-8, which is the overwhelming case;
            // ISO-8859-1 where it is not, since that never fails to decode. The
            // compiler's source encoding is a project setting this tool does not
            // have, and decoding a single-byte source as UTF-8 produced
            // replacement characters -- so a non-ASCII package or class name
            // never matched codename1.packageName, the real main source was
            // rejected, and a hint an annotation owns read as editable.
            return new String(bytes, isValidUtf8(bytes) ? "UTF-8" : "ISO-8859-1");
        } catch (Exception ex) {
            Log.e(ex);
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    /// The index just past the dotted name starting at or after `from`, or
    /// `from` when there is none. The same walk as `qualifiedNameAt`, so the two
    /// cannot disagree about where a name ends.
    static int qualifiedNameEnd(String source, int from, boolean kotlin) {
        int i = nextLiveChar(source, from, kotlin);
        int end = from;
        while (i >= 0 && i < source.length()) {
            int stop = componentEnd(source, i, kotlin);
            if (stop == i) {
                return end;
            }
            end = stop;
            int dot = nextLiveChar(source, stop, kotlin);
            if (dot < 0 || source.charAt(dot) != '.') {
                return end;
            }
            i = nextLiveChar(source, dot + 1, kotlin);
        }
        return end;
    }

    /// The dotted name starting at or after `from`, stepping over whitespace and
    /// comments around each dot.
    /// The end of the name component at `i`, or `i` when there is none.
    ///
    /// A Kotlin component may be ESCAPED in backticks -- `package com.`when``
    /// is legal and the class belongs to com.when. Reading only identifier
    /// characters stopped at the backtick and recorded `com.`, so the real main
    /// source was rejected: nothing then knew which hints an annotation already
    /// owns, and Settings could write the duplicate properties declaration that
    /// the next build rejects.
    private static int componentEnd(String source, int i, boolean kotlin) {
        if (kotlin && i < source.length() && source.charAt(i) == '`') {
            int close = source.indexOf('`', i + 1);
            return close < 0 ? i : close + 1;
        }
        int end = i;
        while (end < source.length() && continuesAName(source.charAt(end))) {
            end++;
        }
        return end;
    }

    /// That component's text, which is what the backticks quote rather than
    /// include.
    private static String componentText(String source, int i, int end, boolean kotlin) {
        if (kotlin && i < end && source.charAt(i) == '`') {
            return source.substring(i + 1, end - 1);
        }
        return source.substring(i, end);
    }

    static String qualifiedNameAt(String source, int from, boolean kotlin) {
        int i = nextLiveChar(source, from, kotlin);
        StringBuilder name = new StringBuilder();
        while (i >= 0 && i < source.length()) {
            int end = componentEnd(source, i, kotlin);
            if (end == i) {
                break;
            }
            name.append(componentText(source, i, end, kotlin));
            int dot = nextLiveChar(source, end, kotlin);
            if (dot < 0 || source.charAt(dot) != '.') {
                break;
            }
            name.append('.');
            i = nextLiveChar(source, dot + 1, kotlin);
        }
        return name.toString();
    }

    /// One import directive: the dotted name it introduces and its alias, if any.
    static final class Imported {
        final String name;
        final String alias;

        Imported(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }
    }

    /// Every live `import` in `source`, read FORWARDS.
    ///
    /// Forwards rather than by backing up from a name, because backing up has to
    /// step over comments in reverse -- and
    /// `import /* build hints */ com.codename1.annotations.buildhints.Ios;` is
    /// legal, so a backward walk that skipped only spaces missed the import and
    /// the live @Ios was read as somebody else's.
    static java.util.List<Imported> importsIn(String source, boolean kotlin) {
        java.util.List<Imported> out = new java.util.ArrayList<>();
        int at = nextMarker(source, "import", 0, kotlin);
        while (at >= 0) {
            int after = at + "import".length();
            boolean whole = (at == 0 || !continuesAName(source.charAt(at - 1)))
                    && after < source.length() && !continuesAName(source.charAt(after));
            if (!whole) {
                at = nextMarker(source, "import", after, kotlin);
                continue;
            }
            int i = nextLiveChar(source, after, kotlin);
            if (i < 0) {
                return out;
            }
            // Java's optional `static`, which is a modifier and not the imported
            // name. Reading it as the name recorded an import called `static`,
            // so `import static com.example.Types.Ios;` never registered as
            // giving `Ios` away -- a wildcard import of ours was trusted instead
            // and the editor was hidden for a hint the processor never emits.
            if (!kotlin && source.startsWith("static", i)
                    && i + 6 < source.length() && !continuesAName(source.charAt(i + 6))) {
                int afterStatic = nextLiveChar(source, i + 6, kotlin);
                if (afterStatic >= 0) {
                    i = afterStatic;
                }
            }
            // Component by component, stepping over whitespace and comments
            // around each dot. `import com.codename1.annotations. /* x */
            // buildhints.Ios;` is legal, and reading the name as one contiguous
            // run stopped at the separator and recorded only the prefix -- so the
            // import was not recognised and the live @Ios read as somebody
            // else's.
            StringBuilder name = new StringBuilder();
            while (i >= 0 && i < source.length()) {
                if (source.charAt(i) == '*') {
                    name.append('*');
                    i++;
                    break;
                }
                // A COMPONENT may be escaped -- `import
                // com.codename1.annotations.`buildhints`.Ios` is legal Kotlin.
                // Reading only identifier characters recorded `annotations.`, so
                // the import went unrecognised and a live @Ios was read as
                // somebody else's: Settings then offered the hint as unowned and
                // could write the duplicate the next build refuses.
                int end = componentEnd(source, i, kotlin);
                if (end == i) {
                    break;
                }
                name.append(componentText(source, i, end, kotlin));
                int dot = nextLiveChar(source, end, kotlin);
                if (dot < 0 || source.charAt(dot) != '.') {
                    i = end;
                    break;
                }
                name.append('.');
                i = nextLiveChar(source, dot + 1, kotlin);
            }
            if (i < 0) {
                i = source.length();
            }
            String alias = null;
            int a = nextLiveChar(source, i, kotlin);
            if (a >= 0 && source.regionMatches(a, "as", 0, 2)
                    && a + 2 < source.length() && !continuesAName(source.charAt(a + 2))) {
                int n = nextLiveChar(source, a + 2, kotlin);
                if (n >= 0) {
                    // The alias may be escaped too: `import a.B as `when``.
                    int nameEnd = componentEnd(source, n, kotlin);
                    if (nameEnd > n) {
                        alias = componentText(source, n, nameEnd, kotlin);
                    }
                }
            }
            if (name.length() > 0) {
                out.add(new Imported(name.toString(), alias));
            }
            at = nextMarker(source, "import", i, kotlin);
        }
        return out;
    }

    /// Whether a live import brings `simple` in from the build hints package.
    ///
    /// Either the type by name or the package on demand, and neither if some
    /// other library's type of that name is imported explicitly: a single-type
    /// import shadows an on-demand one, so their `Ios` beats our wildcard. That
    /// is the language's rule, not a preference.
    static boolean importsAnnotation(String source, String simple, boolean kotlin) {
        return importsAnnotation(source, simple, kotlin, false);
    }

    /// As above; `shadowed` says the same package declares a type of that name.
    ///
    /// A same-package type beats an ON-DEMAND import in both languages, so a
    /// project with its own `Ios` and a wildcard import of ours writes its own
    /// -- and reading that as ours hid the editor for a hint the processor never
    /// emits. A NAMED import still wins, since it is the more specific statement
    /// and a file may not both import a name and declare it.
    static boolean importsAnnotation(String source, String simple, boolean kotlin,
                                     boolean shadowed) {
        String pkg = "com.codename1.annotations.buildhints.";
        boolean ours = false;
        for (Imported imported : importsIn(source, kotlin)) {
            if (imported.alias != null) {
                // Introduces its ALIAS rather than its own name -- so it neither
                // grants nor shadows the simple spelling, unless the alias IS
                // that spelling. `import com.example.Other as Ios` makes `@Ios`
                // mean Other, and ignoring it let a wildcard import of ours be
                // trusted instead, hiding the editor for a hint the processor
                // never emits.
                if (imported.alias.equals(simple)) {
                    return imported.name.startsWith(pkg);
                }
                continue;
            }
            if (imported.name.equals(pkg + simple)) {
                return true;
            }
            if (imported.name.equals(pkg + "*")) {
                ours = !shadowed;
            } else if (imported.name.endsWith("." + simple)) {
                return false;
            }
        }
        return ours;
    }

    /// Whether a top-level type named `simple` is declared in `text`.
    ///
    /// Wider than the main-class lookup on purpose: an annotation is declared
    /// with `annotation class` in Kotlin and `@interface` in Java, and any of
    /// those shadows an on-demand import of the same name.
    static boolean declaresTypeNamed(String text, String simple, boolean kotlin) {
        return declaresTypeNamed(text, simple, kotlin, true);
    }

    /// As above; `includePrivate` is false for a peer, where a file-private
    /// declaration is not a name the main source can see.
    ///
    /// On a top-level Kotlin declaration `private` means this FILE only, so
    /// another file's `private annotation class Ios` shadows nothing -- counting
    /// it made a real `@Ios` read as somebody else's, so the hint looked unowned
    /// and Add wrote the duplicate the next build refuses. In the main file
    /// itself a private type does shadow, because that is the file it belongs
    /// to.
    static boolean declaresTypeNamed(String text, String simple, boolean kotlin,
                                     boolean includePrivate) {
        String modifiers = !includePrivate && kotlin ? blanked(text, kotlin) : null;
        int depth = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'' || c == '/' || c == '`') {
                int skipped = skipNonCode(text, i, kotlin);
                if (skipped > i) {
                    i = skipped;
                    continue;
                }
            }
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            if (depth != 0 || !continuesAName(c)
                    || (i > 0 && continuesAName(text.charAt(i - 1)))) {
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < text.length() && continuesAName(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(i, wordEnd);
            if ("class".equals(word) || "object".equals(word) || "interface".equals(word)
                    || "enum".equals(word) || "record".equals(word)) {
                int n = nextLiveChar(text, wordEnd, kotlin);
                if (n >= 0) {
                    int end = componentEnd(text, n, kotlin);
                    if (end > n && componentText(text, n, end, kotlin).equals(simple)
                            && (modifiers == null || !declaredPrivate(modifiers, i))) {
                        return true;
                    }
                }
            }
            i = wordEnd;
        }
        return false;
    }


    /// The name a Kotlin `typealias Alias = Ios` gives an annotation, or null.
    ///
    /// Unlike an import alias this renames the type in the file itself, so the
    /// annotation never appears under its own name and no import mentions the
    /// alias at all. The right-hand side may be the simple name -- which only
    /// counts when an import makes it ours -- or the fully qualified one, which
    /// needs no import.
    ///
    /// One level: an alias of an alias is not followed, because the first is
    /// what a file that renames our annotation actually writes.
    static String kotlinTypeAlias(String source, String simple, boolean kotlin) {
        java.util.List<String> all = kotlinTypeAliases(source, simple, kotlin);
        return all.isEmpty() ? null : all.get(0);
    }

    /// EVERY such name, for the same reason the import form collects them all:
    /// a file may declare `typealias First = Ios` and `typealias AppIos = Ios`
    /// and use only the second.
    static java.util.List<String> kotlinTypeAliases(String source, String simple,
                                                    boolean kotlin) {
        return kotlinTypeAliases(visibleTypeAliases(source, null), simple, kotlin);
    }

    /// Every name that resolves to `simple`, across all of `sources`, following
    /// a CHAIN of aliases.
    ///
    /// `typealias AppIos = Ios` then `typealias CustomIos = AppIos` is legal,
    /// and `@CustomIos(...)` still compiles to our annotation. Accepting only a
    /// right-hand side that names the annotation directly left the hint reading
    /// as unowned, so Add wrote the duplicate declaration the next build
    /// refuses. Resolved by closure rather than by recursion so that a cycle --
    /// which the compiler rejects, but this reader must not hang on -- simply
    /// stops adding names.
    /// A `typealias`, with where it was written and the name the main file sees
    /// it under.
    ///
    /// The two names differ because an import may rename it: `import
    /// com.other.AppIos as Custom` makes `com.other`'s `AppIos` usable only as
    /// `Custom`, so the file that declares it and the file that writes the
    /// annotation disagree about what it is called.
    static final class AliasDeclaration {
        /// The name in the file that declares it, which its own chain uses.
        final String local;
        /// The name the main file writes, or null when it cannot see this one.
        final String visible;
        final String target;
        /// The package it is declared in, which is the scope its chain resolves
        /// in -- a chain may span files, but only within one package.
        final String scope;
        /// The text that declares it, whose imports decide what its target names.
        final String owner;

        AliasDeclaration(String local, String visible, String target, String scope, String owner) {
            this.local = local;
            this.visible = visible;
            this.target = target;
            this.scope = scope;
            this.owner = owner;
        }
    }

    /// Whether `word` is a modifier that may sit between `private` and the
    /// keyword it belongs to.
    ///
    /// A class carries more of them than a typealias does -- `private
    /// annotation class Ios` is the shape that matters here -- and stopping at
    /// the first one not on this list is what keeps a `private` belonging to an
    /// earlier declaration from being read as this one's.
    private static boolean isDeclarationModifier(String word) {
        return "public".equals(word) || "internal".equals(word) || "protected".equals(word)
                || "actual".equals(word) || "expect".equals(word)
                || "annotation".equals(word) || "data".equals(word) || "enum".equals(word)
                || "sealed".equals(word) || "open".equals(word) || "abstract".equals(word)
                || "final".equals(word) || "inner".equals(word) || "value".equals(word)
                || "inline".equals(word) || "external".equals(word);
    }

    /// The source encoding the POM declares, or null when it declares none.
    ///
    /// Read once per session: this is asked for every source file the sweeps
    /// open, and the answer cannot change while the project is bound.
    private String declaredSourceEncoding() {
        if (!sourceEncodingRead) {
            sourceEncodingRead = true;
            sourceEncoding = declaredSourceEncoding(pomText());
        }
        return sourceEncoding;
    }

    private boolean sourceEncodingRead;
    private String sourceEncoding;

    /// The encoding `pomText` declares: the conventional property first, then
    /// the compiler plugin's own setting.
    ///
    /// A string read rather than an XML model, which is how this tool handles
    /// POMs everywhere else. It is looking for one value that is written as a
    /// plain element in both places.
    static String declaredSourceEncoding(String pomText) {
        if (pomText == null) {
            return null;
        }
        String value = elementValue(pomText, "project.build.sourceEncoding");
        if (value == null) {
            value = elementValue(pomText, "maven.compiler.encoding");
        }
        if (value == null) {
            // Inside the COMPILER plugin. maven-resources-plugin declares an
            // <encoding> of its own, and taking the first one in the file
            // adopted the resource charset for every source -- so a UTF-8 source
            // with a differently encoded resources block was read as neither.
            value = elementValue(pluginBlock(pomText, "maven-compiler-plugin"), "encoding");
        }
        if (value == null || value.trim().isEmpty() || value.indexOf('$') >= 0) {
            // An unresolved ${property} is not an encoding, and this reader has
            // no model to resolve it against.
            return null;
        }
        return value.trim();
    }

    /// The `<plugin>` element declaring `artifactId`, or null.
    static String pluginBlock(String pomText, String artifactId) {
        if (pomText == null) {
            return null;
        }
        int at = pomText.indexOf("<artifactId>" + artifactId + "</artifactId>");
        if (at < 0) {
            return null;
        }
        int open = pomText.lastIndexOf("<plugin>", at);
        int close = pomText.indexOf("</plugin>", at);
        if (open < 0 || close < 0) {
            return null;
        }
        return pomText.substring(open, close);
    }

    private static String elementValue(String xml, String name) {
        if (xml == null) {
            return null;
        }
        String open = "<" + name + ">";
        int at = xml.indexOf(open);
        if (at < 0) {
            return null;
        }
        int close = xml.indexOf("</" + name + ">", at + open.length());
        return close < 0 ? null : xml.substring(at + open.length(), close);
    }

    /// Whether `bytes` decode as UTF-8.
    ///
    /// Hand-rolled because CharsetDecoder is outside the Codename One API
    /// subset this class compiles against, the same reason the name predicate
    /// and the hex reader are.
    static boolean isValidUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            int following;
            int lowest;
            int payload;
            if (b < 0x80) {
                i++;
                continue;
            } else if (b >= 0xC2 && b <= 0xDF) {
                following = 1;
                lowest = 0x80;
                payload = 0x1F;
            } else if (b >= 0xE0 && b <= 0xEF) {
                following = 2;
                lowest = 0x800;
                payload = 0x0F;
            } else if (b >= 0xF0 && b <= 0xF4) {
                following = 3;
                lowest = 0x10000;
                payload = 0x07;
            } else {
                return false;
            }
            if (i + following >= bytes.length) {
                return false;
            }
            int value = b & payload;
            for (int n = 1; n <= following; n++) {
                int next = bytes[i + n] & 0xFF;
                if (next < 0x80 || next > 0xBF) {
                    return false;
                }
                value = (value << 6) | (next & 0x3F);
            }
            // Overlong, and the surrogate range, which UTF-8 does not encode.
            if (value < lowest || value > 0x10FFFF || (value >= 0xD800 && value <= 0xDFFF)) {
                return false;
            }
            i += following + 1;
        }
        return true;
    }

    /// `source` with its comments and literals replaced by spaces, offsets and
    /// line breaks preserved.
    ///
    /// For reading BACKWARDS, which the forward scanner cannot help with:
    /// `private /* note */ typealias AppIos = Ios` is legal, and a backward walk
    /// that skips only whitespace stops at the comment and reports the
    /// declaration as public.
    private static String blanked(String source, boolean kotlin) {
        char[] out = source.toCharArray();
        int i = 0;
        while (i < out.length) {
            char c = out[i];
            if (c != '"' && c != '\'' && c != '/' && c != '`') {
                i++;
                continue;
            }
            int end = skipNonCode(source, i, kotlin);
            if (end <= i) {
                i++;
                continue;
            }
            while (i < end) {
                if (out[i] != '\n' && out[i] != '\r') {
                    out[i] = ' ';
                }
                i++;
            }
        }
        return new String(out);
    }

    /// Whether the declaration at `at` carries the `private` modifier.
    ///
    /// Read backwards over the modifiers that may precede the keyword, stopping
    /// at anything that is not one -- so a `private` belonging to whatever came
    /// before this declaration is not read as this one's.
    private static boolean declaredPrivate(String source, int at) {
        int i = at;
        for (int word = 0; word < 8; word++) {
            int end = i;
            while (end > 0 && (source.charAt(end - 1) == ' ' || source.charAt(end - 1) == '\t'
                    || source.charAt(end - 1) == '\n' || source.charAt(end - 1) == '\r')) {
                end--;
            }
            int start = end;
            while (start > 0 && continuesAName(source.charAt(start - 1))) {
                start--;
            }
            if (start == end) {
                return false;
            }
            String modifier = source.substring(start, end);
            if ("private".equals(modifier)) {
                return true;
            }
            if (!isDeclarationModifier(modifier)) {
                return false;
            }
            i = start;
        }
        return false;
    }

    /// Another source file, with the language it is written in.
    ///
    /// The language travels with the text because it changes what the text
    /// MEANS: raw strings close differently, block comments nest in one and not
    /// the other, and Java translates unicode escapes before it tokenizes -- so
    /// a Java peer declaring `package \u0070;` is in package p, which a
    /// Kotlin-mode read cannot see.
    static final class PeerSource {
        final String text;
        final boolean kotlin;

        PeerSource(String text, boolean kotlin) {
            this.text = text;
            this.kotlin = kotlin;
        }
    }

    /// Peers written in one language, which is what a caller that has plain
    /// texts means by them.
    static java.util.List<PeerSource> peers(java.util.List<String> texts, boolean kotlin) {
        java.util.List<PeerSource> out = new java.util.ArrayList<>();
        if (texts != null) {
            for (String text : texts) {
                if (text != null) {
                    out.add(new PeerSource(text, kotlin));
                }
            }
        }
        return out;
    }

    /// Every `typealias` `mainSource` can see, with the name it sees it under.
    ///
    /// Visibility is per SYMBOL, not per package: `import com.other.Unrelated`
    /// exposes nothing else from `com.other`, and `import com.other.AppIos as
    /// Custom` exposes that one under `Custom`. Reducing this to "does the main
    /// file import anything from that package" was wrong in both directions --
    /// it let an unrelated import expose an alias that hides the editor for a
    /// hint nothing owns, and it lost the local name of a renamed one so a real
    /// annotation went unrecognised and Add wrote the duplicate.
    static java.util.List<AliasDeclaration> visibleTypeAliases(String mainSource,
                                                               java.util.List<String> others) {
        return visibleTypeAliases(mainSource, peers(others, true), true);
    }

    static java.util.List<AliasDeclaration> visibleTypeAliases(String mainSource,
                                                               java.util.List<PeerSource> others,
                                                               boolean kotlin) {
        java.util.List<AliasDeclaration> out = new java.util.ArrayList<>();
        if (mainSource == null) {
            return out;
        }
        String mainPkg = declaredPackageIn(mainSource, kotlin);
        for (String[] declared : typeAliasDeclarations(mainSource, kotlin)) {
            out.add(new AliasDeclaration(declared[0], declared[0], declared[1], mainPkg,
                    mainSource));
        }
        if (others == null) {
            return out;
        }
        java.util.List<Imported> imports = importsIn(mainSource, kotlin);
        for (PeerSource peer : others) {
            String other = peer == null ? null : peer.text;
            if (other == null) {
                continue;
            }
            String pkg = declaredPackageIn(other, peer.kotlin);
            boolean samePackage = pkg.equals(mainPkg);
            for (String[] declared : typeAliasDeclarations(other, peer.kotlin)) {
                if ("private".equals(declared[2])) {
                    // On a top-level Kotlin declaration `private` means this FILE
                    // only, not this package -- so another file's is not a name
                    // the main source can write, and treating it as one let it
                    // vouch for an unrelated annotation of the same name and hide
                    // the editor for a hint nothing owns.
                    continue;
                }
                String visible = samePackage ? declared[0]
                        : importedNameOf(imports, pkg, declared[0]);
                // Kept even when invisible: it may still be a LINK in a chain
                // whose visible end is imported, and that chain resolves in the
                // package it is written in.
                out.add(new AliasDeclaration(declared[0], visible, declared[1], pkg, other));
            }
        }
        return out;
    }

    /// The name `imports` gives `pkg`.`simple`, or null when none of them does.
    ///
    /// A named import wins over an on-demand one, since it is the more specific
    /// statement about that symbol and may rename it.
    private static String importedNameOf(java.util.List<Imported> imports, String pkg,
                                         String simple) {
        String qualified = pkg == null || pkg.isEmpty() ? simple : pkg + "." + simple;
        String onDemand = null;
        for (Imported imported : imports) {
            if (imported.name.equals(qualified)) {
                return imported.alias != null ? imported.alias : simple;
            }
            if (imported.name.equals(pkg + ".*")) {
                onDemand = simple;
            }
        }
        return onDemand;
    }

    /// Every name that resolves to `simple`, following a CHAIN of aliases.
    ///
    /// `typealias AppIos = Ios` then `typealias CustomIos = AppIos` is legal,
    /// and `@CustomIos(...)` still compiles to our annotation. Accepting only a
    /// right-hand side that names the annotation directly left the hint reading
    /// as unowned, so Add wrote the duplicate declaration the next build
    /// refuses. Resolved by closure rather than by recursion so that a cycle --
    /// which the compiler rejects, but this reader must not hang on -- simply
    /// stops adding names.
    ///
    /// The chain is followed by the LOCAL name within one package, which is the
    /// scope a top-level declaration resolves in, and only names the main file
    /// can actually see are returned.
    static java.util.List<String> kotlinTypeAliases(java.util.List<AliasDeclaration> declarations,
                                                    String simple, boolean kotlin) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (!kotlin || declarations == null) {
            return out;
        }
        String qualified = "com.codename1.annotations.buildhints." + simple;
        java.util.List<String> resolved = new java.util.ArrayList<String>();
        java.util.List<AliasDeclaration> pending = new java.util.ArrayList<>();
        // The key each pending declaration's target might name, worked out once
        // rather than on every pass.
        java.util.List<java.util.List<String>> pendingTargets = new java.util.ArrayList<>();
        for (AliasDeclaration declared : declarations) {
            // The bare name counts only where an import makes it ours, and that
            // import is file-scoped -- so it is decided per owner, here, rather
            // than once for the whole sweep. `import ...Ios as Base` then
            // `typealias AppIos = Base` is the same point one step along.
            boolean imported = importsAnnotation(declared.owner, simple, kotlin);
            java.util.List<String> importedAs = kotlinImportAliases(declared.owner, simple, kotlin);
            if (declared.target.equals(qualified)
                    || (imported && declared.target.equals(simple))
                    || importedAs.contains(declared.target)) {
                add(resolved, declared.scope + "\u0000" + declared.local);
                add(out, declared.visible);
            } else {
                pending.add(declared);
                pendingTargets.add(targetKeys(declared));
            }
        }
        // Each pass can only resolve one more link, so the number of passes is
        // bounded by the number of declarations left over.
        for (int pass = 0; pass < pending.size(); pass++) {
            boolean grew = false;
            for (int i = 0; i < pending.size(); i++) {
                AliasDeclaration declared = pending.get(i);
                String key = declared.scope + "\u0000" + declared.local;
                if (resolved.contains(key)) {
                    continue;
                }
                for (String candidate : pendingTargets.get(i)) {
                    if (resolved.contains(candidate)) {
                        resolved.add(key);
                        add(out, declared.visible);
                        grew = true;
                        break;
                    }
                }
            }
            if (!grew) {
                break;
            }
        }
        return out;
    }

    /// The chain links `declared`'s target might name, most specific first.
    ///
    /// A link may cross a package boundary: package `a` declares
    /// `typealias Base = Ios`, package `b` imports `a.Base` and declares
    /// `typealias AppIos = Base`. Looking only in the declaring file's own
    /// package missed that, so the chain stopped there, the hint read as unowned
    /// and Add wrote the duplicate declaration the next build refuses.
    ///
    /// A qualified target names its package outright. Otherwise a named import
    /// -- under its own name or an `as` name -- says where it comes from, and
    /// failing that it is the declaring package's own, or any package imported
    /// on demand.
    private static java.util.List<String> targetKeys(AliasDeclaration declared) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        String target = declared.target;
        int dot = target.lastIndexOf('.');
        if (dot > 0) {
            out.add(target.substring(0, dot) + "\u0000" + target.substring(dot + 1));
            return out;
        }
        java.util.List<String> onDemand = new java.util.ArrayList<String>();
        for (Imported imported : importsIn(declared.owner, true)) {
            int at = imported.name.lastIndexOf('.');
            if (at <= 0) {
                continue;
            }
            String pkg = imported.name.substring(0, at);
            String simpleName = imported.name.substring(at + 1);
            if ("*".equals(simpleName)) {
                onDemand.add(pkg + "\u0000" + target);
                continue;
            }
            String visibleAs = imported.alias != null ? imported.alias : simpleName;
            if (visibleAs.equals(target)) {
                out.add(pkg + "\u0000" + simpleName);
                return out;
            }
        }
        out.add(declared.scope + "\u0000" + target);
        out.addAll(onDemand);
        return out;
    }

    private static void add(java.util.List<String> out, String value) {
        if (value != null && !out.contains(value)) {
            out.add(value);
        }
    }

    /// Every `typealias Name = Target` in `source`, as {name, target, private}.
    ///
    /// The third element is "private" when the declaration carries that
    /// modifier, which on a top-level Kotlin declaration means visible in this
    /// FILE only -- not in the package.
    static java.util.List<String[]> typeAliasDeclarations(String source, boolean kotlin) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        if (!kotlin || source == null) {
            return out;
        }
        int at = nextMarker(source, "typealias", 0, kotlin);
        while (at >= 0) {
            int after = at + "typealias".length();
            boolean whole = (at == 0 || !continuesAName(source.charAt(at - 1)))
                    && after < source.length() && !continuesAName(source.charAt(after));
            if (whole) {
                int n = nextLiveChar(source, after, kotlin);
                if (n >= 0) {
                    int end = componentEnd(source, n, kotlin);
                    if (end > n) {
                        String name = componentText(source, n, end, kotlin);
                        int eq = nextLiveChar(source, end, kotlin);
                        if (eq >= 0 && source.charAt(eq) == '=') {
                            out.add(new String[] {name, qualifiedNameAt(source, eq + 1, kotlin),
                                    declaredPrivate(blanked(source, kotlin), at)
                                            ? "private" : ""});
                        }
                    }
                }
            }
            at = nextMarker(source, "typealias", after, kotlin);
        }
        return out;
    }

    /// The name a Kotlin `import ... as Alias` gives an annotation, or null.
    ///
    /// Kotlin lets a file rename what it imports, and then the annotation never
    /// appears under its own name anywhere in the source. Missing that reads the
    /// hint as unowned, so Settings offers it for Add, writes the properties
    /// line, and the next `process-annotations` fails on the duplicate the tool
    /// itself created.
    static String kotlinImportAlias(String source, String simple, boolean kotlin) {
        java.util.List<String> all = kotlinImportAliases(source, simple, kotlin);
        return all.isEmpty() ? null : all.get(0);
    }

    /// EVERY such name. A file may import the same annotation twice under
    /// different aliases, and answering with the first left the other
    /// unrecognised -- so the hint read as unowned, Settings offered Add, and
    /// the next build failed on the duplicate the tool had just written.
    static java.util.List<String> kotlinImportAliases(String source, String simple,
                                                      boolean kotlin) {
        String needle = "com.codename1.annotations.buildhints." + simple;
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Imported imported : importsIn(source, kotlin)) {
            if (imported.alias != null && needle.equals(imported.name)) {
                out.add(imported.alias);
            }
        }
        return out;
    }

    /// Maps every `@Group(attr = ...)` on the main class to the hints it sets.
    /// Java rules for the source text; see the three-argument form.
    static void collectAnnotationOwnedHints(String source, java.util.Map<String, String> out) {
        collectAnnotationOwnedHints(source, out, false);
    }

    static void collectAnnotationOwnedHints(String source, java.util.Map<String, String> out,
                                            boolean kotlin) {
        collectAnnotationOwnedHints(source, out, kotlin, null);
    }

    /// As above, also resolving `typealias` declarations made in OTHER files.
    ///
    /// A typealias is a top-level declaration, not a file-scoped one: a project
    /// may declare `typealias AppIos = Ios` in one file and write `@AppIos(...)`
    /// on the main class in another. Looking only at the main source read the
    /// hint as unowned, so Add wrote the duplicate declaration the next build
    /// refuses. An import alias is NOT collected this way -- that one applies
    /// only to the file that writes it.
    static void collectAnnotationOwnedHints(String source, java.util.Map<String, String> out,
                                            boolean kotlin,
                                            java.util.List<String> otherSources) {
        collectOwnedHints(source, out, kotlin, peers(otherSources, kotlin));
    }

    static void collectOwnedHints(String source, java.util.Map<String, String> out,
                                  boolean kotlin, java.util.List<PeerSource> otherSources) {
        // Once, not once per hint: which aliases exist and what the main file
        // calls them does not depend on which hint is being asked about.
        java.util.List<AliasDeclaration> declaredAliases =
                kotlin ? visibleTypeAliases(source, otherSources, kotlin)
                       : new java.util.ArrayList<AliasDeclaration>();
        // The sources whose top-level types could shadow an on-demand import:
        // this file, and the rest of its package. Each is read in the language
        // it is written in, since that decides what its text means -- a Java
        // peer's unicode escapes among other things.
        java.util.List<PeerSource> samePackage = new java.util.ArrayList<>();
        samePackage.add(new PeerSource(source, kotlin));
        if (otherSources != null) {
            String mainPkg = declaredPackageIn(source, kotlin);
            for (PeerSource peer : otherSources) {
                if (peer != null && peer.text != null
                        && declaredPackageIn(peer.text, peer.kotlin).equals(mainPkg)) {
                    samePackage.add(peer);
                }
            }
        }
        for (com.codename1.build.shared.BuildHints.Hint h : com.codename1.build.shared.BuildHints.entries()) {
            if (!h.isAnnotated()) {
                continue;
            }
            String simple = h.group().annotationSimpleName();
            // Three spellings are valid: the imported simple name, the fully
            // qualified one, which needs no import, and a Kotlin alias, under
            // which the annotation's own name appears nowhere. Missing any of
            // them leaves the hint editable and Add writes the duplicate.
            java.util.List<String> aliases = kotlinImportAliases(source, simple, kotlin);
            // A fourth: Kotlin can rename a type in the FILE, with no import
            // involved -- `typealias AppIos = Ios` and then `@AppIos(...)`. The
            // compiled annotation is still ours, so missing it left the hint
            // editable and Add wrote the duplicate the next build refuses.
            // One closure over every source, not one per file: a chain may cross
            // files, with the link that names our annotation in one and the link
            // that the main class writes in another.
            aliases.addAll(kotlinTypeAliases(declaredAliases, simple, kotlin));
            // The simple name only counts when an import makes it OUR annotation.
            // @Build and @Android are ordinary enough names that another library's
            // annotation with a matching attribute would otherwise be read as
            // ownership -- and Settings would hide the editor for a hint the
            // processor never emits, which is indistinguishable from the tool
            // being broken.
            boolean shadowed = false;
            boolean first = true;
            for (PeerSource peer : samePackage) {
                // The first entry is the main source itself, where a private
                // type is in the file it belongs to and does shadow.
                boolean own = first;
                first = false;
                if (declaresTypeNamed(peer.text, simple, peer.kotlin, own)) {
                    shadowed = true;
                    break;
                }
            }
            boolean imported = importsAnnotation(source, simple, kotlin, shadowed);
            String qualified = "com.codename1.annotations.buildhints." + simple;

            // Every `@` that is real code, with the name after it read component
            // by component. Matching literal strings could not see
            // `@com.codename1.annotations. /* generated */ buildhints.Ios`, which
            // is legal -- ownership then read as empty and Add wrote the
            // duplicate.
            int at = nextMarker(source, "@", 0, kotlin);
            boolean found = false;
            while (at >= 0 && !found) {
                String name = qualifiedNameAt(source, at + 1, kotlin);
                int after = qualifiedNameEnd(source, at + 1, kotlin);
                boolean ours = (imported && name.equals(simple))
                        || name.equals(qualified)
                        || aliases.contains(name);
                if (ours) {
                    int open = nextLiveChar(source, after, kotlin);
                    if (open >= 0 && source.charAt(open) == '(') {
                        String args = balancedArgs(source, open, kotlin);
                        if (args != null && declaresAttribute(args, h.attr(), kotlin)) {
                            out.put(com.codename1.build.shared.BuildHints.canonicalName(h.name()),
                                    "@" + simple + "(" + h.attr() + ")");
                            found = true;
                            break;
                        }
                    }
                }
                at = nextMarker(source, "@", at + 1, kotlin);
            }
        }
    }

    /// The text inside the parentheses starting at `open`, or null when unbalanced.
    ///
    /// Skips strings, character literals and comments. A comment inside an
    /// annotation can carry an unmatched delimiter --
    /// `@Ios(/* required for issue ( */ teamId = "x")` -- and counting it as
    /// syntax loses the annotation's boundary, leaving an owned hint editable.
    private static String balancedArgs(String source, int open, boolean kotlin) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            int skipped = skipNonCode(source, i, kotlin);
            if (skipped > i) {
                i = skipped - 1;
                continue;
            }
            char c = source.charAt(i);
            if (c == '(' || c == '{' || c == '[') {
                depth++;
            } else if (c == ')' || c == '}' || c == ']') {
                depth--;
                if (depth == 0) {
                    return source.substring(open + 1, i);
                }
            }
        }
        return null;
    }

    /// Whether `args` assigns `attr` at the top level, ignoring anything inside a
    /// nested value, a string, a character literal or a comment.
    private static boolean declaresAttribute(String args, String attr, boolean kotlin) {
        int depth = 0;
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < args.length(); i++) {
            int skipped = skipNonCode(args, i, kotlin);
            if (skipped > i) {
                i = skipped - 1;
                continue;
            }
            char c = args.charAt(i);
            if (c == '(' || c == '{' || c == '[') {
                depth++;
            } else if (c == ')' || c == '}' || c == ']') {
                depth--;
            } else if (depth == 0 && c == '='
                    && (i + 1 >= args.length() || args.charAt(i + 1) != '=')) {
                if (word.toString().trim().equals(attr)) {
                    return true;
                }
                word.setLength(0);
            } else if (depth == 0 && c == ',') {
                word.setLength(0);
            } else if (depth == 0) {
                word.append(c);
            }
        }
        return false;
    }

    /// Whether `c` could continue a Java identifier.
    ///
    /// Hand-rolled because Character.isJavaIdentifierPart is outside the
    /// Codename One API subset, and this class is compiled as app code.
    /// Java's unicode escapes, applied.
    ///
    /// javac processes `\\uXXXX` in the LEXICAL TRANSLATION step, before it
    /// tokenizes anything, so `package com.ex\\u0061mple;` really declares
    /// com.example and an escape works inside an identifier. Reading the text
    /// literally recorded `com.ex`, so the real main source was rejected,
    /// nothing knew which hints an annotation already owns, and Add could write
    /// the duplicate declaration the next build refuses.
    ///
    /// A backslash only opens an escape when an EVEN number of backslashes
    /// precedes it, which is what keeps a string literal spelling one. Kotlin
    /// has no such step, so this is applied to Java only.
    ///
    /// Safe here because this tool never writes a source file back -- it edits
    /// codenameone_settings.properties and the POM -- so nothing depends on an
    /// offset into the text as it is on disk.
    static String decodeUnicodeEscapes(String text) {
        if (text == null || text.indexOf('\\') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '\\') {
                out.append(c);
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) == '\\') {
                j++;
            }
            int run = j - i;
            for (int pair = 0; pair < run / 2; pair++) {
                out.append('\\').append('\\');
            }
            if (run % 2 == 0) {
                i = j;
                continue;
            }
            int u = j;
            while (u < text.length() && text.charAt(u) == 'u') {
                u++;
            }
            int value = u > j ? hexQuad(text, u) : -1;
            if (value < 0) {
                out.append('\\');
                i = j;
                continue;
            }
            out.append((char) value);
            i = u + 4;
        }
        return out.toString();
    }

    /// The four hex digits at `from`, or -1. Hand-rolled for the same reason
    /// [#continuesAName] is: this class compiles against the Codename One API
    /// subset.
    private static int hexQuad(String text, int from) {
        if (from + 4 > text.length()) {
            return -1;
        }
        int value = 0;
        for (int i = from; i < from + 4; i++) {
            char c = text.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else {
                return -1;
            }
            value = value * 16 + digit;
        }
        return value;
    }

    private static boolean continuesAName(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_' || c == '$') {
            return true;
        }
        // Both languages allow a non-ASCII identifier -- `package com.应用` is
        // valid Java and Kotlin -- and stopping at the first such character read
        // a short name, so the real main source was rejected and Settings could
        // offer a hint an annotation already owns.
        //
        // Everything outside ASCII that is not whitespace counts, since
        // Character.isJavaIdentifierPart is outside the Codename One API subset
        // this class is compiled against. That is wider than the language rule,
        // but only by characters that cannot legally sit next to an identifier
        // in source the compiler has already accepted -- and the alternative,
        // rejecting all of them, is wrong for every name that has one.
        return c >= 0x80 && !Character.isWhitespace(c);
    }

    /// The index of the next character that is neither whitespace nor part of a
    /// comment, starting at `from`; -1 when the source ends first.
    static int nextLiveChar(String source, int from, boolean kotlin) {
        int i = from;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                i++;
                continue;
            }
            if (c == '/') {
                int skipped = skipNonCode(source, i, kotlin);
                if (skipped > i) {
                    i = skipped;
                    continue;
                }
            }
            return i;
        }
        return -1;
    }

    /// The next occurrence of `marker` that is real code, or -1.
    ///
    /// Comments and string literals are stepped over with the same scanner the
    /// argument reader uses, so what counts as code is one answer rather than
    /// two that can disagree.
    static int nextMarker(String source, String marker, int from, boolean kotlin) {
        int i = from;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"' || c == '\'' || c == '/' || c == '`') {
                int skipped = skipNonCode(source, i, kotlin);
                if (skipped > i) {
                    i = skipped;
                    continue;
                }
            }
            if (source.startsWith(marker, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /// If a string, character literal or comment starts at `i`, the index just
    /// past it; otherwise `i`.
    /// Index just past a Java text block opening at `i`. Escapes apply, so a
    /// backslash consumes the character after it and cannot start a delimiter.
    private static int endOfJavaTextBlock(String s, int i) {
        int j = i + 3;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == '\\') {
                j += 2;
                continue;
            }
            if (c == '"' && s.startsWith("\"\"\"", j)) {
                return j + 3;
            }
            j++;
        }
        return s.length();
    }

    /// Index just past a Kotlin raw string opening at `i`. No escapes, and a run
    /// of quotes closes at its last three, so the extra ones belong to the value.
    /// The offset just past a `${ ... }` template expression at `i`, or -1 when
    /// one does not start there.
    ///
    /// Braces are matched, and a nested literal inside the expression is stepped
    /// over so that a `}` inside it does not close the expression early.
    private static int endOfKotlinTemplate(String s, int i) {
        if (i + 1 >= s.length() || s.charAt(i) != '$' || s.charAt(i + 1) != '{') {
            return -1;
        }
        int depth = 0;
        int j = i + 1;
        while (j < s.length()) {
            char ch = s.charAt(j);
            if (ch == '"') {
                j = s.startsWith("\"\"\"", j) ? endOfKotlinRawString(s, j) : endOfKotlinString(s, j);
                continue;
            }
            // The expression is ordinary code, so it holds ordinary comments and
            // char literals -- and a quote inside one of those is not a nested
            // string. Reading `${ /* " */ 1 }` as if it were swallowed the rest
            // of the file, hiding every annotation after it.
            // An escaped identifier belongs here too: everything inside it is
            // part of the name, so a quote there does not open a string and a
            // brace does not close the expression.
            if (ch == '\'' || ch == '/' || ch == '`') {
                int skipped = skipNonCode(s, j, true);
                if (skipped > j) {
                    j = skipped;
                    continue;
                }
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return j + 1;
                }
            }
            j++;
        }
        return -1;
    }

    /// The offset just past an ordinary Kotlin string starting at `i`.
    private static int endOfKotlinString(String s, int i) {
        int j = i + 1;
        while (j < s.length()) {
            if (s.charAt(j) == '\\') {
                j += 2;
                continue;
            }
            int template = endOfKotlinTemplate(s, j);
            if (template > j) {
                j = template;
                continue;
            }
            if (s.charAt(j) == '"') {
                return j + 1;
            }
            j++;
        }
        return s.length();
    }

    private static int endOfKotlinRawString(String s, int i) {
        int j = i + 3;
        while (j < s.length()) {
            // A template expression here too: a `"""` inside one is a nested
            // literal, not this string's terminator.
            int template = endOfKotlinTemplate(s, j);
            if (template > j) {
                j = template;
                continue;
            }
            if (s.charAt(j) != '"') {
                j++;
                continue;
            }
            int run = j;
            while (run < s.length() && s.charAt(run) == '"') {
                run++;
            }
            if (run - j >= 3) {
                return run;
            }
            j = run;
        }
        return s.length();
    }

    private static int skipNonCode(String s, int i, boolean kotlin) {
        char c = s.charAt(i);
        // A Kotlin raw string or a Java text block, which the ordinary rule reads
        // as an empty string followed by a new one -- and then an embedded quote
        // inside it opens a literal that swallows the annotation after it.
        //
        // The two languages close it differently, and taking the shorter reading
        // in either direction over-consumes past a live annotation:
        //
        //   Java   escape sequences DO apply, so \" is one quote and the run
        //          \""" is an escaped quote followed by two, not a delimiter.
        //   Kotlin escapes do NOT apply, and a run of four or more quotes ends
        //          the literal at its LAST three -- """a"""" holds a" .
        if (c == '"' && s.startsWith("\"\"\"", i)) {
            return kotlin ? endOfKotlinRawString(s, i) : endOfJavaTextBlock(s, i);
        }
        if (c == '"') {
            for (int j = i + 1; j < s.length(); j++) {
                if (s.charAt(j) == '\\') {
                    j++;
                    continue;
                }
                // A Kotlin template expression opens a fresh nesting level, and
                // the first quote inside it starts a NEW literal rather than
                // closing this one -- so `"${"@Ios(teamId = x)"}"` ended the
                // string early and exposed its contents as live code, which read
                // as an annotation nobody wrote and hid the editor for a hint
                // nothing owns.
                int template = kotlin ? endOfKotlinTemplate(s, j) : -1;
                if (template > j) {
                    j = template - 1;
                    continue;
                }
                if (s.charAt(j) == '"') {
                    return j + 1;
                }
            }
            return s.length();
        }
        if (c == '\'') {
            for (int j = i + 1; j < s.length(); j++) {
                if (s.charAt(j) == '\\') {
                    j++;
                } else if (s.charAt(j) == '\'') {
                    return j + 1;
                }
            }
            return s.length();
        }
        // A Kotlin escaped identifier -- `class `when``. It is code, not a
        // literal, but it is stepped over whole because a quote inside it
        // (`say"hi` is a legal name) would otherwise open a literal that
        // swallows every annotation after it, leaving an owned hint editable in
        // Settings and letting the user add the duplicate that fails the build.
        if (kotlin && c == '`') {
            int close = s.indexOf('`', i + 1);
            int nl = s.indexOf('\n', i + 1);
            if (close >= 0 && (nl < 0 || close < nl)) {
                return close + 1;
            }
        }
        if (c == '/' && i + 1 < s.length()) {
            char n = s.charAt(i + 1);
            if (n == '/') {
                int nl = s.indexOf('\n', i);
                return nl < 0 ? s.length() : nl;
            }
            if (n == '*') {
                // Kotlin block comments NEST; Java's do not. Stopping at the
                // first */ in Kotlin ends the comment early and the rest of it is
                // then read as live code.
                if (!kotlin) {
                    int close = s.indexOf("*/", i + 2);
                    return close < 0 ? s.length() : close + 2;
                }
                int depth = 0;
                int j = i;
                while (j < s.length()) {
                    if (s.charAt(j) == '/' && j + 1 < s.length() && s.charAt(j + 1) == '*') {
                        depth++;
                        j += 2;
                        continue;
                    }
                    if (s.charAt(j) == '*' && j + 1 < s.length() && s.charAt(j + 1) == '/') {
                        depth--;
                        j += 2;
                        if (depth == 0) {
                            return j;
                        }
                        continue;
                    }
                    j++;
                }
                return s.length();
            }
        }
        return i;
    }
}
