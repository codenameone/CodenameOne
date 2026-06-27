package com.codename1.initializr;

import static com.codename1.ui.CN.*;

import com.codename1.components.InteractionDialog;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.initializr.model.GeneratorModel;
import com.codename1.initializr.model.IDE;
import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.Template;
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
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
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
            "InitializrSummaryText", "InitializrPhoneStage", "InitializrLiveFrame"
    };

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
        final SpanLabel summaryLabel = new SpanLabel();
        final TemplatePreviewPanel previewPanel = new TemplatePreviewPanel(selectedTemplate[0]);

        // Mutable subtitle labels so panel headers reflect the current selection.
        final Label ideSubtitle = panelSubtitle(IDE.INTELLIJ.name());
        final Label localeSubtitle = panelSubtitle("No bundles");
        final Label javaSubtitle = panelSubtitle(ProjectOptions.JavaVersion.JAVA_17.label);

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
                ProjectOptions options = currentOptions(includeLocalizationBundles, previewLanguage, javaVersion);
                previewPanel.setTemplate(selectedTemplate[0]);
                previewPanel.setOptions(options);

                ideSubtitle.setText(formatEnumLabel(selectedIde[0].name()));
                localeSubtitle.setText(includeLocalizationBundles[0]
                        ? "Bundles . " + previewLanguage[0].label
                        : "No bundles");
                javaSubtitle.setText(javaVersion[0].label);

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
                createJavaOptionsPanel(javaVersion, refresh), form);
        Container settingsPanel = makePanel("Current Settings", panelSubtitle("Generated artifacts"), true, true,
                BoxLayout.encloseY(summaryLabel), form);

        // ----- live preview (stacked at the end of the single column) -----
        Container previewWrap = createPreviewWrap(previewPanel);

        Container column = BoxLayout.encloseY(hero, essentials, idePanel, localePanel,
                javaPanel, settingsPanel, previewWrap);
        column.setUIID("InitializrColumn");
        column.setScrollableY(true);
        column.setScrollVisible(true);

        // ----- generate bar -----
        final Button generateButton = new Button("Generate Project");
        FontImage.setMaterialIcon(generateButton, FontImage.MATERIAL_DOWNLOAD);
        generateButton.setUIID("InitializrPrimaryButton");
        generateButton.addActionListener(e -> {
            if (!validateInputs(appNameField, packageField)) {
                updateValidationErrorLabels(appNameField, packageField, appNameError, packageError);
                ToastBar.showErrorMessage("Please fix validation errors before generating.");
                form.revalidate();
                return;
            }
            String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
            String packageName = packageField.getText() == null ? "" : packageField.getText().trim();
            ProjectOptions options = currentOptions(includeLocalizationBundles, previewLanguage, javaVersion);
            GeneratorModel.create(selectedIde[0], selectedTemplate[0], appName, packageName, options).generate();
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

    private ProjectOptions currentOptions(boolean[] includeLocalizationBundles,
                                          ProjectOptions.PreviewLanguage[] previewLanguage,
                                          ProjectOptions.JavaVersion[] javaVersion) {
        // The initializr UI no longer exposes a theme picker, so every generated
        // project ships the barebones default theme. That theme.css already adapts
        // to the device's light/dark setting at runtime via an
        // @media (prefers-color-scheme: dark) block, so we must NOT bake the
        // website's current dark/light state into the download -- doing so used to
        // emit hard-coded dark colors at top-level scope that then broke light mode
        // for anyone who opened the project.
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
        fields.add(labeledField("Language", languageSelector));

        Container bodyWrap = new Container(new BorderLayout());
        bodyWrap.setUIID("InitializrPanelBody");
        bodyWrap.add(BorderLayout.CENTER, fields);

        Container panel = BoxLayout.encloseY(header, bodyWrap);
        panel.setUIID("InitializrPanel");
        return panel;
    }

    private Container createLanguageSelector(Template[] selectedTemplate, Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(1, 2));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        Template[] languages = {Template.BAREBONES, Template.KOTLIN};
        String[] labels = {"Java", "Kotlin"};
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

    private Container createJavaOptionsPanel(ProjectOptions.JavaVersion[] javaVersion, Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(2, 1));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        String[] labels = {"Java 17 (Recommended)", "Java 8"};
        ProjectOptions.JavaVersion[] versions = {ProjectOptions.JavaVersion.JAVA_17, ProjectOptions.JavaVersion.JAVA_8};
        for (int i = 0; i < versions.length; i++) {
            final ProjectOptions.JavaVersion version = versions[i];
            RadioButton button = new RadioButton(labels[i]);
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
        });
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
                + "Language " + (template.IS_KOTLIN ? "KOTLIN" : "JAVA") + "\n"
                + "IDE      " + ide.name() + "\n"
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
