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
package com.codename1.initializr;

import static com.codename1.ui.CN.*;

import com.codename1.components.InteractionDialog;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.initializr.model.GeneratorModel;
import com.codename1.initializr.model.IDE;
import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.Template;
import com.codename1.initializr.ui.AppIcon;
import com.codename1.initializr.ui.TemplatePreviewPanel;
import com.codename1.system.Lifecycle;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;
import com.codename1.util.StringUtil;

public class Initializr extends Lifecycle {
    private boolean darkMode;

    /** Rebuilds the live preview (and summary) for the current options. Held so a
     *  later light/dark toggle can re-theme the preview, not just the chrome. */
    private Runnable uiRefresh;

    /** Most recently built form. Exposed only so render/mockup tests can capture
     *  the exact form they triggered (the simulator's app-under-test lifecycle
     *  keeps an earlier form "current", so getCurrent() is not reliable in-test). */
    static Form lastBuiltForm;

    /** The primary "Generate Project" button. Held so the website-theme poll can
     *  nudge it left when the host page's Crisp chat launcher would cover it. */
    private Button generateButton;

    /** Last chat-launcher clearance (pixels) applied to the generate button's
     *  right margin. -1 forces the next poll to (re)apply it, e.g. after a theme
     *  refresh reloads the button style and drops the margin override. */
    private int lastChatClearance = -1;

    // UIIDs that have a "...Dark" variant in theme.css. Used to re-skin the whole
    // tree when the host website / system switches between light and dark.
    private static final String[] THEMEABLE = {
            "InitializrForm", "InitializrRoot", "InitializrColumn", "InitializrTopbar",
            "InitializrWordmark", "InitializrHero", "InitializrHeroTitle", "InitializrHeroSubtitle",
            "InitializrPill", "InitializrPillDot", "InitializrPillText",
            "InitializrPanel", "InitializrPanelHeader", "InitializrPanelTitle",
            "InitializrPanelSubtitle", "InitializrPanelChevron", "InitializrPanelBody",
            "InitializrSectionTitle", "InitializrFieldLabel", "InitializrField", "InitializrFieldHint",
            "InitializrChoicesGrid", "InitializrChoice", "InitializrSummary", "InitializrTip",
            "InitializrValidationError", "InitializrHelpButton", "InitializrGenerateBar",
            "InitializrGenerateInfo", "InitializrPrimaryButton", "InitializrPreviewWrap",
            "InitializrPreviewTitle", "InitializrLiveDot", "InitializrCard", "InitializrPreviewHolder",
            "InitializrSummaryText", "InitializrPhoneStage", "InitializrLiveFrame", "InitializrStep"
    };

    /** The post-download "next steps" panel at the top of the column, or null
     *  when it is not showing. Held so a second download replaces it. */
    private Container nextStepsPanel;

    @Override
    public void runApp() {
        setProperty("platformHint.javascript.beforeUnloadMessage", null);
        Boolean systemDarkMode = Display.getInstance().isDarkMode();
        darkMode = systemDarkMode != null && systemDarkMode.booleanValue();

        final Form form = new Form("", new BorderLayout());
        form.setUIID("InitializrForm");
        Toolbar topbar = form.getToolbar();
        topbar.setUIID("InitializrTopbar");
        topbar.setTitleCentered(false);
        Label wordmark = new Label("Initializr");
        wordmark.setUIID("InitializrWordmark");
        topbar.setTitleComponent(wordmark);

        final TextField appNameField = new TextField("MyAppName", "Main Class Name");
        final TextField packageField = new TextField("com.example.myapp", "Package Name");
        final Label appNameError = new Label("");
        final Label packageError = new Label("");
        final Template[] selectedTemplate = new Template[]{Template.BAREBONES};
        final IDE[] selectedIde = new IDE[]{IDE.INTELLIJ};
        final boolean[] includeLocalizationBundles = new boolean[]{false};
        final ProjectOptions.PreviewLanguage[] previewLanguage = new ProjectOptions.PreviewLanguage[]{ProjectOptions.PreviewLanguage.ENGLISH};
        final ProjectOptions.JavaVersion[] javaVersion = new ProjectOptions.JavaVersion[]{ProjectOptions.JavaVersion.JAVA_17};
        final ProjectOptions.BuildTool[] buildTool = new ProjectOptions.BuildTool[]{ProjectOptions.BuildTool.MAVEN};
        final ProjectOptions.ProjectType[] projectType = new ProjectOptions.ProjectType[]{ProjectOptions.ProjectType.APP};
        final boolean[] allPlatformModules = new boolean[]{false};
        // The colour scheme of a full-stack template: {0xRRGGBB, or -1 for its own}.
        final int[] brandColor = new int[]{-1};
        final boolean[] roundedCorners = new boolean[]{true};
        // The panels a full-stack template settles for itself, {build, localization,
        // java}, and the one only it has, {look}; filled in below, read by refresh.
        final Container[] fixedByTemplate = new Container[3];
        final Container[] lookPanelHolder = new Container[1];
        final Label lookIcon = new Label();
        // {Java 17, Java 8}: the build panel disables Java 8 while Gradle is selected.
        final RadioButton[] javaButtons = new RadioButton[2];
        final SpanLabel summaryLabel = new SpanLabel();
        final TemplatePreviewPanel previewPanel = new TemplatePreviewPanel(selectedTemplate[0]);

        // Mutable subtitle labels so panel headers reflect the current selection.
        final Label ideSubtitle = panelSubtitle(IDE.INTELLIJ.name());
        final Label localeSubtitle = panelSubtitle("No bundles");
        final Label javaSubtitle = panelSubtitle(ProjectOptions.JavaVersion.JAVA_17.label);
        final Label buildSubtitle = panelSubtitle(ProjectOptions.BuildTool.MAVEN.label);
        final Label lookSubtitle = panelSubtitle(SCHEME_LABELS[0]);

        appNameField.setUIID("InitializrField");
        packageField.setUIID("InitializrField");
        appNameError.setUIID("InitializrValidationError");
        packageError.setUIID("InitializrValidationError");
        appNameError.setHidden(true);
        appNameError.setVisible(false);
        packageError.setHidden(true);
        packageError.setVisible(false);
        summaryLabel.setUIID("InitializrSummary");
        summaryLabel.setTextUIID("InitializrSummaryText");

        final Label bundleInfo = new Label("Bundle: MyAppName.zip");
        bundleInfo.setUIID("InitializrGenerateInfo");

        final Runnable refresh = new Runnable() {
            public void run() {
                ProjectOptions options = currentOptions(includeLocalizationBundles, previewLanguage, javaVersion)
                        .withBuild(buildTool[0], projectType[0])
                        .withPlatformModules(allPlatformModules[0]);
                boolean fullStack = selectedTemplate[0].isFullStack();
                if (fullStack) {
                    options = options.forFullStack().withScheme(brandColor[0], roundedCorners[0]);
                }
                // A full-stack template is one build, one project type and one Java
                // level, so those panels have nothing to ask; it has a look instead.
                boolean buildChoices = GeneratorModel.isGradleOffered() || GeneratorModel.isMavenLayoutChoiceOffered();
                for (int i = 0; i < fixedByTemplate.length; i++) {
                    if (fixedByTemplate[i] != null) {
                        boolean shown = !fullStack && (i != 0 || buildChoices);
                        fixedByTemplate[i].setHidden(!shown);
                        fixedByTemplate[i].setVisible(shown);
                    }
                }
                if (lookPanelHolder[0] != null) {
                    lookPanelHolder[0].setHidden(!fullStack);
                    lookPanelHolder[0].setVisible(fullStack);
                }
                lookSubtitle.setText(schemeLabel(brandColor[0])
                        + (roundedCorners[0] ? "" : " . Square corners"));
                lookIcon.setIcon(AppIcon.create(appNameField.getText(), schemeColor(brandColor[0]),
                        convertToPixels(12)));
                previewPanel.setTemplate(selectedTemplate[0]);
                previewPanel.setOptions(options);

                ideSubtitle.setText(formatEnumLabel(selectedIde[0].name()));
                localeSubtitle.setText(includeLocalizationBundles[0]
                        ? "Bundles . " + previewLanguage[0].label
                        : "No bundles");
                javaSubtitle.setText(javaVersion[0].label);
                buildSubtitle.setText(buildTool[0] == ProjectOptions.BuildTool.GRADLE
                        || GeneratorModel.isMavenLayoutChoiceOffered()
                        ? buildTool[0].label + " . " + projectType[0].label
                        : buildTool[0].label);

                String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
                bundleInfo.setText("Bundle: " + (appName.length() == 0 ? "App" : appName) + ".zip");

                summaryLabel.setText(createSummary(
                        appNameField.getText(), packageField.getText(),
                        selectedTemplate[0], selectedIde[0], options));
                updateValidationErrorLabels(appNameField, packageField, appNameError, packageError);
                form.revalidate();
            }
        };

        // ----- left column -----
        Container hero = createHero();
        Container essentials = createEssentialsPanel(appNameField, packageField, appNameError, packageError,
                createLanguageSelector(selectedTemplate, refresh));
        Container idePanel = makePanel("IDE", ideSubtitle, true, false,
                createIdeSelectorPanel(selectedIde, refresh), form);
        Container localePanel = makePanel("Localization", localeSubtitle, true, false,
                createLocalizationPanel(includeLocalizationBundles, previewLanguage, refresh), form);
        Container javaPanel = makePanel("Java Version", javaSubtitle, true, false,
                createJavaOptionsPanel(javaVersion, javaButtons, refresh), form);
        Container buildPanel = makePanel("Build Tool", buildSubtitle, true, false,
                createBuildToolPanel(buildTool, projectType, allPlatformModules, javaVersion, javaButtons, refresh),
                form);
        Container lookPanel = makePanel("Look", lookSubtitle, true, true,
                createLookPanel(brandColor, roundedCorners, lookIcon, refresh), form);
        fixedByTemplate[0] = buildPanel;
        fixedByTemplate[1] = localePanel;
        fixedByTemplate[2] = javaPanel;
        lookPanelHolder[0] = lookPanel;
        Container settingsPanel = makePanel("Current Settings", panelSubtitle("Generated artifacts"), true, true,
                BoxLayout.encloseY(summaryLabel), form);

        // ----- live preview (stacked at the end of the single column) -----
        Container previewWrap = createPreviewWrap(previewPanel);

        // Gradle is offered once the plugin is published at the version the
        // generated projects use, and the Maven project types once that plugin can
        // build them; with neither, the Maven default stands alone.
        // Which of these panels show is refresh's to say: it depends on the template.
        Container column = BoxLayout.encloseY(hero, essentials, lookPanel, idePanel, buildPanel, localePanel,
                javaPanel, settingsPanel, previewWrap);
        column.setUIID("InitializrColumn");
        column.setScrollableY(true);
        column.setScrollVisible(true);

        // ----- generate bar -----
        final Button generateButton = new Button("Generate Project");
        generateButton.setUIID("InitializrPrimaryButton");
        // Derive the icon AFTER the UIID is applied. setMaterialIcon bakes the
        // component's current background into the FontImage; doing it while the
        // button still carried the default Button style is what painted a white
        // box behind the download glyph.
        FontImage.setMaterialIcon(generateButton, FontImage.MATERIAL_DOWNLOAD);
        this.generateButton = generateButton;
        generateButton.addActionListener(e -> {
            if (!validateInputs(appNameField, packageField)) {
                updateValidationErrorLabels(appNameField, packageField, appNameError, packageError);
                ToastBar.showErrorMessage("Please fix validation errors before generating.");
                form.revalidate();
                return;
            }
            String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
            String packageName = packageField.getText() == null ? "" : packageField.getText().trim();
            ProjectOptions options = downloadOptions(includeLocalizationBundles, previewLanguage, javaVersion)
                    .withBuild(buildTool[0], projectType[0])
                        .withPlatformModules(allPlatformModules[0]);
            if (selectedTemplate[0].isFullStack()) {
                options = options.forFullStack().withScheme(brandColor[0], roundedCorners[0])
                        .withIcon(AppIcon.png(appName, schemeColor(brandColor[0])));
            }
            GeneratorModel model = GeneratorModel.create(selectedIde[0], selectedTemplate[0], appName, packageName,
                    options);
            if (model.generate()) {
                showNextSteps(form, column, model);
            }
        });

        Container generateBar = new Container(new BorderLayout());
        generateBar.setUIID("InitializrGenerateBar");
        generateBar.add(BorderLayout.CENTER, bundleInfo);
        generateBar.add(BorderLayout.EAST, generateButton);

        Container body = new Container(new BorderLayout());
        body.add(BorderLayout.CENTER, column);
        body.setUIID("InitializrRoot");
        body.setScrollableY(false);
        form.getContentPane().setScrollableY(false);

        form.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.SOUTH, generateBar);
        appNameField.addDataChangedListener((type, index) -> refresh.run());
        packageField.addDataChangedListener((type, index) -> refresh.run());
        uiRefresh = refresh;
        refresh.run();
        if (darkMode) {
            applyTheme(form, true);
        }
        lastBuiltForm = form;
        initWebsiteThemeSync(form);
        form.show();
        notifyWebsiteUiReady();
        Display.getInstance().startThread(new Runnable() {
            public void run() {
                GeneratorModel.cleanupGeneratedZips();
            }
        }, "initializr-storage-cleanup").start();
    }

    // Options that drive the LIVE PREVIEW only. The preview mock-up reflects the
    // website's current light/dark chrome (via darkMode) so a dark-host visitor
    // still sees a dark, natively-styled preview. This is purely cosmetic: the
    // preview is styled with the initializr's own UIIDs and never compiles the
    // generated theme.css. Downloads use downloadOptions() instead.
    private ProjectOptions currentOptions(boolean[] includeLocalizationBundles,
                                          ProjectOptions.PreviewLanguage[] previewLanguage,
                                          ProjectOptions.JavaVersion[] javaVersion) {
        ProjectOptions.ThemeMode mode = darkMode ? ProjectOptions.ThemeMode.DARK : ProjectOptions.ThemeMode.LIGHT;
        return new ProjectOptions(mode, ProjectOptions.Accent.DEFAULT, true,
                includeLocalizationBundles[0], previewLanguage[0], javaVersion[0], "");
    }

    // Options for the generated DOWNLOAD. The UI no longer exposes a theme
    // picker, so every project ships the barebones default theme regardless of
    // the website's current dark/light chrome. Baking the host state in here is
    // what used to emit top-level "Initializr Theme Overrides" that broke light
    // mode; the default theme.css adapts to the device at runtime on its own.
    private ProjectOptions downloadOptions(boolean[] includeLocalizationBundles,
                                           ProjectOptions.PreviewLanguage[] previewLanguage,
                                           ProjectOptions.JavaVersion[] javaVersion) {
        return new ProjectOptions(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.DEFAULT, true,
                includeLocalizationBundles[0], previewLanguage[0], javaVersion[0], "");
    }

    private void notifyWebsiteUiReady() {
        WebsiteThemeNative nativeBridge = NativeLookup.create(WebsiteThemeNative.class);
        if (nativeBridge != null && nativeBridge.isSupported()) {
            nativeBridge.notifyUiReady();
        }
    }

    // ---------- builders ----------

    private Container createHero() {
        SpanLabel title = new SpanLabel("Scaffold a project in seconds");
        title.setUIID("InitializrHeroTitle");
        title.setTextUIID("InitializrHeroTitle");
        SpanLabel subtitle = new SpanLabel("Generate a ready-to-build Codename One application "
                + "- pick your IDE and options. We'll wire up the project for you.");
        subtitle.setUIID("InitializrHeroSubtitle");
        subtitle.setTextUIID("InitializrHeroSubtitle");

        Container text = BoxLayout.encloseY(title, subtitle);

        // Clean status dot: a circle glyph on its own transparent-background
        // label, in a mid-accent that reads on both pill backgrounds (so a
        // light/dark toggle does not need to recolour it).
        Label pillDot = new Label("");
        pillDot.setUIID("InitializrPillDot");
        FontImage.setMaterialIcon(pillDot, FontImage.MATERIAL_FIBER_MANUAL_RECORD, 1.7f);
        Label pillText = new Label("READY");
        pillText.setUIID("InitializrPillText");
        Container pill = new Container(new com.codename1.ui.layouts.FlowLayout(Component.LEFT, Component.CENTER));
        pill.setUIID("InitializrPill");
        pill.add(pillDot).add(pillText);

        Container pillHolder = new Container(new BorderLayout());
        pillHolder.add(BorderLayout.NORTH, FlowRight(pill));

        Container card = new Container(new BorderLayout());
        card.setUIID("InitializrHero");
        card.add(BorderLayout.CENTER, text);
        card.add(BorderLayout.EAST, pillHolder);
        return card;
    }

    private Container createEssentialsPanel(TextField appNameField, TextField packageField,
                                            Label appNameError, Label packageError, Container languageSelector) {
        Label title = new Label("Essentials");
        title.setUIID("InitializrPanelTitle");
        Label subtitle = panelSubtitle("Class, package, language");
        Container header = new Container(new BorderLayout());
        header.setUIID("InitializrPanelHeader");
        header.add(BorderLayout.CENTER, BoxLayout.encloseX(title, subtitle));

        Container fields = new Container(BoxLayout.y());
        fields.add(labeledFieldWithHelp("Main Class", appNameField, "Main Class",
                "This is your app's entry point class. It is used in generated source files and build "
                        + "configuration. Changing it later requires renaming code and updating references."));
        fields.add(appNameError);
        fields.add(labeledFieldWithHelp("Package", packageField, "Package Name",
                "Use reverse-domain format, e.g. com.yourcompany.myapp. This namespace should be globally "
                        + "unique. Unique package identifiers are critical for app store submissions because "
                        + "they distinguish your app from others and prevent install/update conflicts."));
        fields.add(packageError);
        fields.add(labeledField(GeneratorModel.isTemplateOffered(Template.WAYLINE) ? "Start from" : "Language",
                languageSelector));

        Container bodyWrap = new Container(new BorderLayout());
        bodyWrap.setUIID("InitializrPanelBody");
        bodyWrap.add(BorderLayout.CENTER, fields);

        Container panel = BoxLayout.encloseY(header, bodyWrap);
        panel.setUIID("InitializrPanel");
        return panel;
    }

    /// What the project starts as: an empty app in Java or in Kotlin, or -- once the
    /// plugin the Initializr generates against can build it -- the ride-hailing app
    /// with its server.
    private Container createLanguageSelector(Template[] selectedTemplate, Runnable onSelectionChanged) {
        boolean fullStack = GeneratorModel.isTemplateOffered(Template.WAYLINE);
        Container selector = new Container(new GridLayout(fullStack ? 3 : 1, fullStack ? 1 : 2));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        Template[] languages = fullStack
                ? new Template[] {Template.BAREBONES, Template.KOTLIN, Template.WAYLINE}
                : new Template[] {Template.BAREBONES, Template.KOTLIN};
        String[] labels = fullStack
                ? new String[] {"Empty app, Java", "Empty app, Kotlin", "Ride-hailing app with its server, Java"}
                : new String[] {"Java", "Kotlin"};
        for (int i = 0; i < languages.length; i++) {
            final Template template = languages[i];
            RadioButton button = new RadioButton(labels[i]);
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            group.add(button);
            selector.add(button);
            if (template == selectedTemplate[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (button.isSelected()) {
                    selectedTemplate[0] = template;
                    onSelectionChanged.run();
                }
            });
        }
        return selector;
    }

    /// The colour schemes a full-stack template is offered in. The first is the
    /// template's own, which leaves its stylesheet as it is.
    private static final String[] SCHEME_LABELS = {"Blue", "Teal", "Violet", "Orange", "Rose", "Graphite"};
    private static final int[] SCHEME_COLORS = {-1, 0x0f766e, 0x6d3fd1, 0xd9540b, 0xc8235d, 0x2f3a4a};

    private static String schemeLabel(int brandColor) {
        for (int i = 0; i < SCHEME_COLORS.length; i++) {
            if (SCHEME_COLORS[i] == brandColor) {
                return SCHEME_LABELS[i];
            }
        }
        return SCHEME_LABELS[0];
    }

    /// The colour a scheme paints with, which for the template's own is not -1.
    private static int schemeColor(int brandColor) {
        return brandColor < 0 ? GeneratorModel.FULL_STACK_BRAND : brandColor;
    }

    /// The look of a full-stack template: its brand colour, the corners, and the
    /// icon that follows from the colour and the name. The stylesheet takes every
    /// colour from a block of variables, so this is all it takes to re-skin it.
    private Container createLookPanel(int[] brandColor, boolean[] roundedCorners, Label icon,
                                      Runnable onSelectionChanged) {
        Container colors = new Container(new GridLayout(2, 3));
        colors.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        int dot = convertToPixels(3);
        for (int i = 0; i < SCHEME_COLORS.length; i++) {
            final int color = SCHEME_COLORS[i];
            RadioButton button = new RadioButton(SCHEME_LABELS[i]);
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            Image swatch = Image.createImage(dot, dot, 0);
            swatch.getGraphics().setAntiAliased(true);
            swatch.getGraphics().setColor(schemeColor(color));
            swatch.getGraphics().fillArc(0, 0, dot, dot, 0, 360);
            button.setIcon(swatch);
            group.add(button);
            colors.add(button);
            if (color == brandColor[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (button.isSelected()) {
                    brandColor[0] = color;
                    onSelectionChanged.run();
                }
            });
        }
        final CheckBox rounded = new CheckBox("Rounded corners");
        rounded.setUIID("InitializrChoice");
        rounded.setSelected(roundedCorners[0]);
        rounded.addActionListener(evt -> {
            roundedCorners[0] = rounded.isSelected();
            onSelectionChanged.run();
        });
        SpanLabel hint = new SpanLabel("The icon is the first letter of the main class on the color you choose. "
                + "Replace common/icon.png with your own artwork whenever you have it; the colors are the "
                + "variables theme.css opens with.");
        hint.setUIID("InitializrTip");
        hint.setTextUIID("InitializrTip");
        return BoxLayout.encloseY(labeledField("Color", colors), rounded, labeledField("Icon", icon), hint);
    }

    private Container createIdeSelectorPanel(IDE[] selectedIde, Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(2, 2));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        for (IDE ide : IDE.values()) {
            RadioButton button = new RadioButton(formatEnumLabel(ide.name()));
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            group.add(button);
            selector.add(button);
            if (ide == selectedIde[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (button.isSelected()) {
                    selectedIde[0] = ide;
                    onSelectionChanged.run();
                }
            });
        }
        return selector;
    }

    private Container createLocalizationPanel(boolean[] includeLocalizationBundles,
                                              ProjectOptions.PreviewLanguage[] previewLanguage,
                                              Runnable onSelectionChanged) {
        CheckBox includeBundles = new CheckBox("Include resource bundles");
        includeBundles.setUIID("InitializrChoice");
        includeBundles.setSelected(includeLocalizationBundles[0]);

        Picker languagePicker = new Picker();
        languagePicker.setUIID("InitializrField");
        String[] labels = new String[ProjectOptions.PreviewLanguage.values().length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = ProjectOptions.PreviewLanguage.values()[i].label;
        }
        languagePicker.setStrings(labels);
        languagePicker.setSelectedString(previewLanguage[0].label);
        languagePicker.setEnabled(includeBundles.isSelected());

        includeBundles.addActionListener(e -> {
            includeLocalizationBundles[0] = includeBundles.isSelected();
            languagePicker.setEnabled(includeBundles.isSelected());
            onSelectionChanged.run();
        });
        languagePicker.addActionListener(e -> {
            previewLanguage[0] = findLanguageByLabel(languagePicker.getSelectedString());
            onSelectionChanged.run();
        });

        return BoxLayout.encloseY(includeBundles, labeledField("Preview Language", languagePicker));
    }

    private Container createJavaOptionsPanel(ProjectOptions.JavaVersion[] javaVersion, RadioButton[] buttons,
                                             Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(2, 1));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        String[] labels = {"Java 17 (Recommended)", "Java 8"};
        ProjectOptions.JavaVersion[] versions = {ProjectOptions.JavaVersion.JAVA_17, ProjectOptions.JavaVersion.JAVA_8};
        for (int i = 0; i < versions.length; i++) {
            final ProjectOptions.JavaVersion version = versions[i];
            RadioButton button = new RadioButton(labels[i]);
            buttons[i] = button;
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            group.add(button);
            selector.add(button);
            if (version == javaVersion[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (button.isSelected()) {
                    javaVersion[0] = version;
                    onSelectionChanged.run();
                }
            });
        }
        return selector;
    }

    /// Maven or Gradle, and what the project holds. Gradle projects are Java 17
    /// only, so choosing Gradle selects Java 17 and disables Java 8. A Maven project
    /// gets the project type, and the choice of every platform module over the
    /// minimal layout, once the plugin it is generated against can build them
    /// ([GeneratorModel#isMavenLayoutChoiceOffered()]); before that it always
    /// carries the backend module behind a profile and the type is Gradle's alone.
    private Container createBuildToolPanel(ProjectOptions.BuildTool[] buildTool,
                                           ProjectOptions.ProjectType[] projectType,
                                           boolean[] allPlatformModules,
                                           ProjectOptions.JavaVersion[] javaVersion,
                                           RadioButton[] javaButtons, Runnable onSelectionChanged) {
        final boolean mavenLayouts = GeneratorModel.isMavenLayoutChoiceOffered();
        Container tools = new Container(new GridLayout(1, GeneratorModel.isGradleOffered() ? 2 : 1));
        tools.setUIID("InitializrChoicesGrid");
        ButtonGroup toolGroup = new ButtonGroup();

        final CheckBox allModules = new CheckBox("Include all platform modules (javase/, android/, ios/, ...)");
        allModules.setUIID("InitializrChoice");
        allModules.setSelected(allPlatformModules[0]);
        allModules.addActionListener(evt -> {
            allPlatformModules[0] = allModules.isSelected();
            onSelectionChanged.run();
        });
        final Container modulesSection = BoxLayout.encloseY(allModules);
        final Runnable updateModules = () -> {
            boolean show = mavenLayouts && buildTool[0] == ProjectOptions.BuildTool.MAVEN
                    && projectType[0] != ProjectOptions.ProjectType.BACKEND_ONLY;
            modulesSection.setHidden(!show);
            modulesSection.setVisible(show);
            if (!show && allPlatformModules[0] && projectType[0] == ProjectOptions.ProjectType.BACKEND_ONLY) {
                // A backend-only project has no platform modules to include.
                allPlatformModules[0] = false;
                allModules.setSelected(false);
            }
        };

        Container types = new Container(new GridLayout(3, 1));
        types.setUIID("InitializrChoicesGrid");
        ButtonGroup typeGroup = new ButtonGroup();
        final RadioButton[] typeButtons = new RadioButton[ProjectOptions.ProjectType.values().length];
        for (ProjectOptions.ProjectType type : ProjectOptions.ProjectType.values()) {
            RadioButton button = new RadioButton(type.label);
            typeButtons[type.ordinal()] = button;
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            typeGroup.add(button);
            types.add(button);
            if (type == projectType[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (button.isSelected()) {
                    projectType[0] = type;
                    updateModules.run();
                    onSelectionChanged.run();
                }
            });
        }
        SpanLabel hint = new SpanLabel(mavenLayouts
                ? "Gradle projects target Java 17. A Maven app builds every platform from common/ "
                        + "unless you include all platform modules."
                : "Gradle projects target Java 17. Every Maven project already "
                        + "includes an optional backend module.");
        hint.setUIID("InitializrTip");
        hint.setTextUIID("InitializrTip");
        final Container typeSection = BoxLayout.encloseY(labeledField("Project Type", types));
        boolean typeShown = buildTool[0] == ProjectOptions.BuildTool.GRADLE || mavenLayouts;
        typeSection.setHidden(!typeShown);
        typeSection.setVisible(typeShown);
        updateModules.run();

        for (ProjectOptions.BuildTool tool : ProjectOptions.BuildTool.values()) {
            if (tool == ProjectOptions.BuildTool.GRADLE && !GeneratorModel.isGradleOffered()) {
                continue;
            }
            RadioButton button = new RadioButton(tool.label);
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            toolGroup.add(button);
            tools.add(button);
            if (tool == buildTool[0]) {
                button.setSelected(true);
            }
            button.addActionListener(evt -> {
                if (!button.isSelected()) {
                    return;
                }
                buildTool[0] = tool;
                boolean gradle = tool == ProjectOptions.BuildTool.GRADLE;
                typeSection.setHidden(!gradle && !mavenLayouts);
                typeSection.setVisible(gradle || mavenLayouts);
                if (gradle) {
                    javaVersion[0] = ProjectOptions.JavaVersion.JAVA_17;
                    if (javaButtons[0] != null) {
                        javaButtons[0].setSelected(true);
                    }
                }
                if (javaButtons[1] != null) {
                    javaButtons[1].setEnabled(!gradle);
                }
                if (!gradle && !mavenLayouts && projectType[0] == ProjectOptions.ProjectType.BACKEND_ONLY) {
                    // Maven has no backend-only project, and the type is hidden
                    // now, so a stale choice would fail generation with no way to
                    // correct it. Every Maven project carries the backend anyway.
                    projectType[0] = ProjectOptions.ProjectType.APP;
                    typeGroup.setSelected(typeButtons[ProjectOptions.ProjectType.APP.ordinal()]);
                }
                updateModules.run();
                onSelectionChanged.run();
            });
        }
        return BoxLayout.encloseY(tools, typeSection, modulesSection, hint);
    }

    private Container createPreviewWrap(TemplatePreviewPanel previewPanel) {
        Label dot = new Label("");
        dot.setUIID("InitializrLiveDot");
        FontImage.setMaterialIcon(dot, FontImage.MATERIAL_FIBER_MANUAL_RECORD, 2f);
        Label title = new Label("Live preview");
        title.setUIID("InitializrPreviewTitle");
        Container head = BoxLayout.encloseX(dot, title);

        // Size the live preview like a phone (portrait ~1:2) and centre it, so it
        // reads as a device mockup instead of a shrunken strip.
        Component preview = previewPanel.getComponent();
        preview.setPreferredW(convertToPixels(60f));
        preview.setPreferredH(convertToPixels(120f));
        Container phoneStage = new Container(new com.codename1.ui.layouts.FlowLayout(Component.CENTER));
        phoneStage.setUIID("InitializrPhoneStage");
        phoneStage.add(preview);

        Container wrap = new Container(new BorderLayout());
        wrap.setUIID("InitializrPreviewWrap");
        wrap.add(BorderLayout.NORTH, head);
        wrap.add(BorderLayout.CENTER, phoneStage);
        return wrap;
    }

    // ---------- next steps after a download ----------

    // A visitor who downloads a project used to see nothing happen at all: no
    // hint of what to open, which command starts the first build, or that the
    // first cloud build asks for an account -- and most never built. This panel
    // says it at the moment they are looking. It sits at the TOP of the column,
    // not in a dialog: it never blocks the form, scrolls with it on a small
    // screen, and stays clear of the host page's Crisp widget, which floats
    // over the bottom-right (see applyChatLauncherClearance).
    void showNextSteps(Form form, Container column, GeneratorModel model) {
        if (nextStepsPanel != null && nextStepsPanel.getParent() != null) {
            nextStepsPanel.remove();
        }
        GeneratorModel.NextSteps next = model.nextSteps();

        Label title = new Label("Your project is downloading");
        title.setUIID("InitializrPanelTitle");
        final Button close = new Button();
        close.setUIID("InitializrPanelChevron");
        FontImage.setMaterialIcon(close, FontImage.MATERIAL_CLOSE, 3.4f);
        Container header = new Container(new BorderLayout());
        header.setUIID("InitializrPanelHeader");
        header.add(BorderLayout.CENTER, title);
        header.add(BorderLayout.EAST, close);

        Container body = new Container(BoxLayout.y());
        for (int i = 0; i < next.steps.length; i++) {
            body.add(stepText((i + 1) + ". " + next.steps[i], "InitializrStep"));
        }
        // Both command lines: guessing the visitor's OS from a browser is
        // fragile, and the wrong one is worse than two short blocks.
        body.add(labeledField("macOS / Linux", commandBlock(next.unixCommands)));
        body.add(labeledField("Windows (PowerShell or Command Prompt)", commandBlock(next.windowsCommands)));
        if (next.cloudBuild) {
            body.add(stepText("The first cloud build opens your browser and asks you to sign in or create a "
                    + "free account (100 cloud build credits a month). Local builds need no account.",
                    "InitializrStep"));
        }
        WebsiteThemeNative bridge = NativeLookup.create(WebsiteThemeNative.class);
        // Only on the Codename One website itself: anywhere else (a third-party
        // page framing /initializr-app/, localhost, a preview) the address would
        // go to a page we do not control, or nowhere.
        if (bridge != null && bridge.isSupported() && bridge.canRequestSteps()) {
            body.add(createEmailStepsRow(bridge, model));
        }

        Container bodyWrap = new Container(new BorderLayout());
        bodyWrap.setUIID("InitializrPanelBody");
        bodyWrap.add(BorderLayout.CENTER, body);

        final Container panel = BoxLayout.encloseY(header, bodyWrap);
        panel.setUIID("InitializrPanel");
        close.addActionListener(e -> {
            panel.remove();
            if (nextStepsPanel == panel) {
                nextStepsPanel = null;
            }
            form.revalidate();
        });
        nextStepsPanel = panel;
        if (darkMode) {
            applyTheme(panel, true);
        }
        column.addComponent(0, panel);
        form.revalidate();
        column.scrollComponentToVisible(panel);
    }

    /// "Email me these steps": optional, one email, and the button only lights up
    /// for something that looks like an address. The website page forwards the
    /// request (WebsiteThemeNative#requestSteps); nothing is stored here.
    private Container createEmailStepsRow(WebsiteThemeNative bridge, GeneratorModel model) {
        final TextField email = new TextField("", "you@example.com", 30, TextField.EMAILADDR);
        email.setUIID("InitializrField");
        final Button send = new Button("Email me these steps");
        send.setUIID("InitializrPrimaryButton");
        send.setEnabled(false);
        final SpanLabel note = stepText("We'll email the steps once and may follow up if you get stuck. "
                + "Nothing else.", "InitializrTip");
        final boolean[] requested = new boolean[1];
        email.addDataChangedListener((type, index) -> {
            if (!requested[0]) {
                send.setEnabled(isPlausibleEmail(email.getText()));
            }
        });
        send.addActionListener(e -> {
            final String address = email.getText() == null ? "" : email.getText().trim();
            if (!isPlausibleEmail(address) || requested[0]) {
                return;
            }
            // The bridge waits for the host page to confirm it really sent the
            // request (up to a few seconds), so ask off the EDT and keep the
            // form responsive; the outcome comes back through callSerially.
            requested[0] = true;
            send.setEnabled(false);
            email.setEnabled(false);
            note.setText("Sending...");
            revalidateForm(note);
            new Thread(() -> {
                boolean sent;
                try {
                    sent = bridge.requestSteps(address, model.getPackageName(), model.templateId(), model.ideId(),
                            model.buildKind());
                } catch (Throwable t) {
                    sent = false;
                }
                final boolean ok = sent;
                callSerially(() -> {
                    if (ok) {
                        note.setText("Check your inbox.");
                    } else {
                        // Let them try again, and say where the same steps are.
                        requested[0] = false;
                        send.setEnabled(isPlausibleEmail(email.getText()));
                        email.setEnabled(true);
                        note.setText("Couldn't send the email from here. The README in your project has the same steps.");
                    }
                    revalidateForm(note);
                });
            }, "initializr-steps").start();
        });
        Container row = new Container(new BorderLayout());
        row.add(BorderLayout.CENTER, email);
        row.add(BorderLayout.EAST, send);
        return BoxLayout.encloseY(row, note);
    }

    private static void revalidateForm(Component c) {
        Form f = c.getComponentForm();
        if (f != null) {
            f.revalidate();
        }
    }

    private SpanLabel stepText(String text, String uiid) {
        SpanLabel label = new SpanLabel(text);
        label.setUIID(uiid);
        label.setTextUIID(uiid);
        return label;
    }

    private SpanLabel commandBlock(String commands) {
        SpanLabel block = new SpanLabel(commands);
        block.setUIID("InitializrSummary");
        block.setTextUIID("InitializrSummaryText");
        return block;
    }

    /// Whether `value` looks enough like an email address to send to: one @, a
    /// non-empty local part, a dotted domain with a two-letter-or-longer last
    /// label, and nothing that cannot appear unquoted. Deliberately loose -- the
    /// server is the real check; this only keeps the button off for typos.
    static boolean isPlausibleEmail(String value) {
        if (value == null) {
            return false;
        }
        String s = value.trim();
        if (s.length() < 6 || s.length() > 254) {
            return false;
        }
        int at = s.indexOf('@');
        if (at < 1 || at != s.lastIndexOf('@')) {
            return false;
        }
        String domain = s.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        if (dot < 1 || dot > domain.length() - 3 || domain.startsWith(".") || domain.indexOf("..") >= 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= ' ' || c == 127 || ",;:<>()[]\\\"".indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    // ---------- panel helper ----------

    private Container makePanel(String title, Label subtitle, boolean collapsible,
                                boolean initiallyOpen, Component body, Form form) {
        Label titleLabel = new Label(title);
        titleLabel.setUIID("InitializrPanelTitle");

        Container header = new Container(new BorderLayout());
        header.setUIID("InitializrPanelHeader");
        header.add(BorderLayout.CENTER, BoxLayout.encloseX(titleLabel, subtitle));

        final Container bodyWrap = new Container(new BorderLayout());
        bodyWrap.setUIID("InitializrPanelBody");
        bodyWrap.add(BorderLayout.CENTER, body);

        if (collapsible) {
            final Button chevron = new Button();
            chevron.setUIID("InitializrPanelChevron");
            setChevron(chevron, initiallyOpen);
            header.add(BorderLayout.EAST, chevron);
            bodyWrap.setVisible(initiallyOpen);
            bodyWrap.setHidden(!initiallyOpen);
            ActionListener toggle = e -> {
                boolean nowOpen = !bodyWrap.isVisible();
                bodyWrap.setVisible(nowOpen);
                bodyWrap.setHidden(!nowOpen);
                setChevron(chevron, nowOpen);
                Form f = bodyWrap.getComponentForm();
                if (f != null) {
                    f.revalidate();
                }
            };
            chevron.addActionListener(toggle);
            header.setLeadComponent(chevron);
        }

        Container panel = BoxLayout.encloseY(header, bodyWrap);
        panel.setUIID("InitializrPanel");
        return panel;
    }

    private void setChevron(Button chevron, boolean open) {
        FontImage.setMaterialIcon(chevron, open
                ? FontImage.MATERIAL_KEYBOARD_ARROW_DOWN
                : FontImage.MATERIAL_CHEVRON_RIGHT, 3.4f);
    }

    private Label panelSubtitle(String text) {
        Label l = new Label("- " + text);
        l.setUIID("InitializrPanelSubtitle");
        return l;
    }

    private Container FlowRight(Component c) {
        Container row = new Container(new com.codename1.ui.layouts.FlowLayout(Component.RIGHT));
        row.add(c);
        return row;
    }

    private Container labeledField(String label, Component input) {
        Label l = new Label(label);
        l.setUIID("InitializrFieldLabel");
        return BoxLayout.encloseY(l, input);
    }

    private Container labeledFieldWithHelp(String label, Component input, String helpTitle, String helpBody) {
        Label l = new Label(label);
        l.setUIID("InitializrFieldLabel");
        Button help = new Button();
        help.setUIID("InitializrHelpButton");
        FontImage.setMaterialIcon(help, FontImage.MATERIAL_HELP_OUTLINE, 2.8f);
        help.addActionListener(e -> showHelpPopup(help, helpTitle, helpBody));
        Container header = new Container(new BorderLayout());
        header.add(BorderLayout.CENTER, l);
        header.add(BorderLayout.EAST, help);
        return BoxLayout.encloseY(header, input);
    }

    /** Shows the field help as a fluid InteractionDialog popup anchored to the
     *  help button, instead of a blocking modal Dialog. */
    private void showHelpPopup(Component source, String title, String body) {
        InteractionDialog dlg = new InteractionDialog(title);
        dlg.setUIID(darkMode ? "InitializrHelpPopupDark" : "InitializrHelpPopup");
        dlg.getTitleComponent().setUIID(darkMode ? "InitializrHelpPopupTitleDark" : "InitializrHelpPopupTitle");
        dlg.setLayout(new BorderLayout());
        SpanLabel content = new SpanLabel(body);
        content.setUIID(darkMode ? "InitializrHelpPopupBodyDark" : "InitializrHelpPopupBody");
        content.setTextUIID(darkMode ? "InitializrHelpPopupBodyDark" : "InitializrHelpPopupBody");
        content.setPreferredW(convertToPixels(58f));
        dlg.add(BorderLayout.CENTER, content);
        dlg.setDisposeWhenPointerOutOfBounds(true);
        dlg.showPopupDialog(source);
    }

    private ProjectOptions.PreviewLanguage findLanguageByLabel(String label) {
        for (ProjectOptions.PreviewLanguage language : ProjectOptions.PreviewLanguage.values()) {
            if (language.label.equals(label)) {
                return language;
            }
        }
        return ProjectOptions.PreviewLanguage.ENGLISH;
    }

    private String formatEnumLabel(String text) {
        if ("DEFAULT".equals(text)) {
            return "Clean";
        }
        return StringUtil.replaceAll(text, "_", " ");
    }

    // ---------- theme sync ----------

    private void initWebsiteThemeSync(Form form) {
        WebsiteThemeNative websiteThemeNative = NativeLookup.create(WebsiteThemeNative.class);
        if (websiteThemeNative == null || !websiteThemeNative.isSupported()) {
            return;
        }
        boolean dark = websiteThemeNative.isDarkMode();
        applyDarkMode(form, dark);
        UITimer.timer(900, true, form, () -> {
            boolean nowDark = websiteThemeNative.isDarkMode();
            if (nowDark != darkMode) {
                applyDarkMode(form, nowDark);
            }
            applyChatLauncherClearance(form, websiteThemeNative.chatLauncherClearance());
        });
    }

    // The Crisp chat launcher floats over the bottom-right of the host page,
    // directly on top of the generate button. The website reports how many
    // pixels of horizontal clearance the launcher needs (0 when it is hidden);
    // we add that as a right margin so the button slides left, clear of it. The
    // JS port works in CSS pixels, so the reported value maps 1:1.
    private void applyChatLauncherClearance(Form form, int clearancePx) {
        if (generateButton == null || clearancePx < 0 || clearancePx == lastChatClearance) {
            return;
        }
        lastChatClearance = clearancePx;
        Style all = generateButton.getAllStyles();
        all.setMarginUnitRight(Style.UNIT_TYPE_PIXELS);
        all.setMarginRight(clearancePx);
        form.revalidate();
    }

    private void applyDarkMode(Form form, boolean dark) {
        darkMode = dark;
        Display.getInstance().setDarkMode(dark);
        // Rebuild the preview for the new theme mode FIRST (it creates a fresh
        // InterFormContainer), then re-skin the whole tree so the new preview
        // frame is themed too. Otherwise a light->dark toggle leaves the preview
        // (and its frame) on the previous theme.
        if (uiRefresh != null) {
            uiRefresh.run();
        }
        applyTheme(form, dark);
        form.refreshTheme();
        // refreshTheme reloads the button style from the theme, dropping the
        // chat-launcher margin override; force the next poll to reapply it.
        lastChatClearance = -1;
    }

    private void applyTheme(Component component, boolean dark) {
        String uiid = component.getUIID();
        String themed = themedUiid(uiid, dark);
        if (!uiid.equals(themed)) {
            component.setUIID(themed);
        }
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyTheme(cnt.getComponentAt(i), dark);
            }
        }
    }

    private String themedUiid(String uiid, boolean dark) {
        if (uiid == null || uiid.length() == 0) {
            return uiid;
        }
        if (dark) {
            if (uiid.endsWith("Dark")) {
                return uiid;
            }
            return isThemeable(uiid) ? uiid + "Dark" : uiid;
        }
        if (!uiid.endsWith("Dark")) {
            return uiid;
        }
        String base = uiid.substring(0, uiid.length() - "Dark".length());
        return isThemeable(base) ? base : uiid;
    }

    private boolean isThemeable(String uiid) {
        for (String s : THEMEABLE) {
            if (s.equals(uiid)) {
                return true;
            }
        }
        return false;
    }

    // ---------- summary + validation ----------

    private String createSummary(String appName, String packageName, Template template, IDE ide, ProjectOptions options) {
        String safeApp = appName == null ? "" : appName.trim();
        String safePackage = packageName == null ? "" : packageName.trim();
        return "App      " + safeApp + "\n"
                + "Package  " + safePackage + "\n"
                + (template.isFullStack() ? "Template RIDE-HAILING APP + SERVER\n" : "")
                + "Language " + (template.IS_KOTLIN ? "KOTLIN" : "JAVA") + "\n"
                + "IDE      " + ide.name() + "\n"
                + "Build    " + options.buildTool.label
                + (options.isGradle() || GeneratorModel.isMavenLayoutChoiceOffered()
                        ? " (" + options.projectType.label
                                + (!options.isGradle() && options.allPlatformModules
                                        && options.projectType != ProjectOptions.ProjectType.BACKEND_ONLY
                                        ? ", all modules" : "") + ")"
                        : "") + "\n"
                + "Java     " + options.javaVersion.label + "\n"
                + "Bundles  " + (options.includeLocalizationBundles ? "INCLUDED" : "NONE") + "\n"
                + "Preview  " + options.previewLanguage.label;
    }

    private boolean validateInputs(TextField appNameField, TextField packageField) {
        String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
        String packageName = packageField.getText() == null ? "" : packageField.getText().trim();
        return isValidClassName(appName) && isValidPackageName(packageName);
    }

    private void updateValidationErrorLabels(TextField appNameField, TextField packageField, Label appNameError, Label packageError) {
        String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
        String packageName = packageField.getText() == null ? "" : packageField.getText().trim();

        String appNameMessage = isValidClassName(appName) || appName.length() == 0
                ? ""
                : "Main class must start with a letter and use only letters and digits.";
        String packageMessage = isValidPackageName(packageName) || packageName.length() == 0
                ? ""
                : "Package must use dot-separated identifiers with letters and digits only.";

        appNameError.setText(appNameMessage);
        appNameError.setHidden(appNameMessage.length() == 0);
        appNameError.setVisible(appNameMessage.length() > 0);

        packageError.setText(packageMessage);
        packageError.setHidden(packageMessage.length() == 0);
        packageError.setVisible(packageMessage.length() > 0);
    }

    private boolean isValidClassName(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        if (!isAsciiLetter(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isAsciiLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPackageName(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        int start = 0;
        int dots = 0;
        for (int i = 0; i <= value.length(); i++) {
            boolean atEnd = i == value.length();
            if (atEnd || value.charAt(i) == '.') {
                if (i == start) {
                    return false;
                }
                if (!isValidIdentifierPart(value, start, i)) {
                    return false;
                }
                if (!atEnd) {
                    dots++;
                }
                start = i + 1;
            }
        }
        return dots > 0;
    }

    private boolean isValidIdentifierPart(String value, int start, int end) {
        if (!isAsciiLetter(value.charAt(start))) {
            return false;
        }
        for (int i = start + 1; i < end; i++) {
            char c = value.charAt(i);
            if (!isAsciiLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isAsciiLetterOrDigit(char c) {
        return isAsciiLetter(c) || (c >= '0' && c <= '9');
    }
}
