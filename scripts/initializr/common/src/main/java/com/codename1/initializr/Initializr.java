package com.codename1.initializr;

import static com.codename1.ui.CN.*;

import com.codename1.components.Accordion;
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
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.StringUtil;

public class Initializr extends Lifecycle {
    private boolean websiteDarkMode;

    @Override
    public void runApp() {
        setProperty("platformHint.javascript.beforeUnloadMessage", null);
        final Form form = new Form("", new BorderLayout());
        form.setUIID("InitializrForm");

        final TextField appNameField = new TextField("MyAppName", "Main Class Name");
        final TextField packageField = new TextField("com.example.myapp", "Package Name");
        final Label appNameError = new Label("");
        final Label packageError = new Label("");
        final Template[] selectedTemplate = new Template[]{Template.BAREBONES};
        final IDE[] selectedIde = new IDE[]{IDE.INTELLIJ};
        final ProjectOptions.ThemeMode[] selectedThemeMode = new ProjectOptions.ThemeMode[]{ProjectOptions.ThemeMode.LIGHT};
        final ProjectOptions.Accent[] selectedAccent = new ProjectOptions.Accent[]{ProjectOptions.Accent.DEFAULT};
        final boolean[] roundedButtons = new boolean[]{true};
        final boolean[] includeLocalizationBundles = new boolean[]{false};
        final ProjectOptions.PreviewLanguage[] previewLanguage = new ProjectOptions.PreviewLanguage[]{ProjectOptions.PreviewLanguage.ENGLISH};
        final ProjectOptions.JavaVersion[] javaVersion = new ProjectOptions.JavaVersion[]{ProjectOptions.JavaVersion.JAVA_8};
        final String[] customThemeCss = new String[]{""};
        final RadioButton[] templateButtons = new RadioButton[Template.values().length];
        final SpanLabel summaryLabel = new SpanLabel();
        final TemplatePreviewPanel previewPanel = new TemplatePreviewPanel(selectedTemplate[0]);
        final Container[] themePanelRef = new Container[1];
        final Label customCssError = new Label("");
        final boolean[] customCssValid = new boolean[]{true};

        appNameField.setUIID("InitializrField");
        packageField.setUIID("InitializrField");
        appNameError.setUIID("InitializrValidationError");
        packageError.setUIID("InitializrValidationError");
        appNameError.setHidden(true);
        appNameError.setVisible(false);
        packageError.setHidden(true);
        packageError.setVisible(false);
        summaryLabel.setUIID("InitializrSummary");
        customCssError.setUIID("InitializrValidationError");
        customCssError.setHidden(true);
        customCssError.setVisible(false);

        final Runnable refresh = new Runnable() {
            public void run() {
                ProjectOptions options = new ProjectOptions(
                        selectedThemeMode[0], selectedAccent[0], roundedButtons[0],
                        includeLocalizationBundles[0], previewLanguage[0], javaVersion[0],
                        customThemeCss[0]
                );
                customCssValid[0] = true;
                customCssError.setText("");
                customCssError.setHidden(true);
                customCssError.setVisible(false);
                try {
                    previewPanel.setTemplate(selectedTemplate[0]);
                    previewPanel.setOptions(options);
                } catch (IllegalArgumentException cssErr) {
                    customCssValid[0] = false;
                    customCssError.setText("Custom CSS Error: " + cssErr.getMessage());
                    customCssError.setHidden(false);
                    customCssError.setVisible(true);
                }
                boolean canCustomizeTheme = supportsLivePreview(selectedTemplate[0]);
                if (themePanelRef[0] != null) {
                    setEnabledRecursive(themePanelRef[0], canCustomizeTheme);
                }
                summaryLabel.setText(createSummary(
                        appNameField.getText(),
                        packageField.getText(),
                        selectedTemplate[0],
                        selectedIde[0],
                        options
                ));
                updateValidationErrorLabels(appNameField, packageField, appNameError, packageError);
                form.revalidate();
            }
        };

        final Container essentialsCard = createEssentialsCard(
                appNameField,
                packageField,
                appNameError,
                packageError,
                createTemplateSelector(selectedTemplate, templateButtons, refresh)
        );
        final Container idePanel = createIdeSelectorPanel(selectedIde, refresh);
        final Container themePanel = createThemeOptionsPanel(selectedThemeMode, selectedAccent, roundedButtons, customThemeCss, customCssError, refresh);
        final Container localizationPanel = createLocalizationPanel(includeLocalizationBundles, previewLanguage, refresh, previewPanel);
        final Container javaPanel = createJavaOptionsPanel(javaVersion, refresh);
        themePanelRef[0] = themePanel;
        final Container settingsPanel = BoxLayout.encloseY(summaryLabel);

        Accordion advancedAccordion = new Accordion();
        advancedAccordion.addContent("IDE", idePanel);
        advancedAccordion.addContent("Theme Customization", themePanel);
        advancedAccordion.addContent("Localization", localizationPanel);
        advancedAccordion.addContent("Java Version", javaPanel);
        advancedAccordion.addContent("Current Settings", settingsPanel);
        advancedAccordion.setAutoClose(false);
        advancedAccordion.setScrollable(false);

        Container leftColumn = BoxLayout.encloseY(createHeader(), essentialsCard, advancedAccordion);
        leftColumn.setUIID("InitializrColumn");
        leftColumn.setScrollableY(true);

        Container rightColumn = new Container(new BorderLayout());
        rightColumn.add(BorderLayout.CENTER, previewPanel.getComponent());
        rightColumn.setUIID("InitializrColumn");

        final Button generateButton = new Button("Generate Project");
        FontImage.setMaterialIcon(generateButton, FontImage.MATERIAL_DOWNLOAD);
        generateButton.setUIID("InitializrPrimaryButton");

        generateButton.addActionListener(e -> {
            if (!validateInputs(appNameField, packageField) || !customCssValid[0]) {
                updateValidationErrorLabels(appNameField, packageField, appNameError, packageError);
                ToastBar.showErrorMessage(customCssValid[0] ? "Please fix validation errors before generating." : "Please fix custom CSS errors before generating.");
                form.revalidate();
                return;
            }
            String appName = appNameField.getText() == null ? "" : appNameField.getText().trim();
            String packageName = packageField.getText() == null ? "" : packageField.getText().trim();
            ProjectOptions options = new ProjectOptions(
                    selectedThemeMode[0], selectedAccent[0], roundedButtons[0],
                    includeLocalizationBundles[0], previewLanguage[0], javaVersion[0],
                    customThemeCss[0]
            );
            GeneratorModel.create(selectedIde[0], selectedTemplate[0], appName, packageName, options).generate();
        });

        Container body = createResponsiveBody(leftColumn, rightColumn);
        body.setUIID("InitializrRoot");
        body.setScrollableY(false);
        form.getContentPane().setScrollableY(false);

        form.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.SOUTH, generateButton);
        appNameField.addDataChangedListener((type, index) -> refresh.run());
        packageField.addDataChangedListener((type, index) -> refresh.run());
        refresh.run();
        initWebsiteThemeSync(form);
        form.show();
        notifyWebsiteUiReady();
    }

    private void notifyWebsiteUiReady() {
        WebsiteThemeNative nativeBridge = NativeLookup.create(WebsiteThemeNative.class);
        if (nativeBridge != null && nativeBridge.isSupported()) {
            nativeBridge.notifyUiReady();
        }
    }

    private Container createLocalizationPanel(boolean[] includeLocalizationBundles,
                                              ProjectOptions.PreviewLanguage[] previewLanguage,
                                              Runnable onSelectionChanged,
                                              TemplatePreviewPanel previewPanel) {
        CheckBox includeBundles = new CheckBox("Include Resource Bundles");
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

        includeBundles.addActionListener(e -> {
            includeLocalizationBundles[0] = includeBundles.isSelected();
            languagePicker.setEnabled(includeBundles.isSelected());
            onSelectionChanged.run();
        });
        languagePicker.setEnabled(includeBundles.isSelected());
        languagePicker.addActionListener(e -> {
            String selected = languagePicker.getSelectedString();
            previewLanguage[0] = findLanguageByLabel(selected);
            onSelectionChanged.run();
        });

        return BoxLayout.encloseY(includeBundles, labeledField("Preview Language", languagePicker));
    }

    private Container createHeader() {
        Label title = new Label("Initializr - Scaffold a Project in Seconds");
        title.setUIID("InitializrHeroTitle");
        Label subtitle = new Label("Generate a \"getting started\" Codename One application.");
        subtitle.setUIID("InitializrHeroSubtitle");
        Container card = BoxLayout.encloseY(title, subtitle);
        card.setUIID("InitializrHeaderCard");
        return card;
    }

    private Container createEssentialsCard(TextField appNameField, TextField packageField,
                                           Label appNameError, Label packageError, Container templateSelector) {
        Label title = new Label("Essentials");
        title.setUIID("InitializrSectionTitle");
        Container fields = new Container(BoxLayout.y());
        fields.add(labeledFieldWithHelp(
                "Main Class",
                appNameField,
                "Main Class",
                "This is your app's entry point class. It is used in generated source files and build configuration. "
                        + "Changing it later requires renaming code and updating references."
        ));
        fields.add(appNameError);
        fields.add(labeledFieldWithHelp(
                "Package",
                packageField,
                "Package Name",
                "Use reverse-domain format, e.g. com.yourcompany.myapp. "
                        + "This namespace should be globally unique. "
                        + "Unique package identifiers are critical for app store submissions because they distinguish your app "
                        + "from others and prevent install/update conflicts."
        ));
        fields.add(packageError);
        fields.add(labeledField("Template", templateSelector));
        Container card = BoxLayout.encloseY(title, fields);
        card.setUIID("InitializrCard");
        return card;
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

    private Container createThemeOptionsPanel(ProjectOptions.ThemeMode[] selectedThemeMode,
                                              ProjectOptions.Accent[] selectedAccent,
                                              boolean[] roundedButtons,
                                              String[] customThemeCss,
                                              Label customCssError,
                                              Runnable onSelectionChanged) {
        Container modeRow = new Container(new GridLayout(1, 2));
        modeRow.setUIID("InitializrChoicesGrid");
        ButtonGroup modeGroup = new ButtonGroup();
        for (ProjectOptions.ThemeMode mode : ProjectOptions.ThemeMode.values()) {
            RadioButton rb = new RadioButton(formatEnumLabel(mode.name()));
            rb.setToggle(true);
            rb.setUIID("InitializrChoice");
            modeGroup.add(rb);
            modeRow.add(rb);
            if (mode == selectedThemeMode[0]) {
                rb.setSelected(true);
            }
            rb.addActionListener(e -> {
                if (rb.isSelected()) {
                    selectedThemeMode[0] = mode;
                    onSelectionChanged.run();
                }
            });
        }

        Container accentRow = new Container(new GridLayout(2, 2));
        accentRow.setUIID("InitializrChoicesGrid");
        ButtonGroup accentGroup = new ButtonGroup();
        for (ProjectOptions.Accent value : ProjectOptions.Accent.values()) {
            RadioButton rb = new RadioButton(formatEnumLabel(value.name()));
            rb.setToggle(true);
            rb.setUIID("InitializrChoice");
            accentGroup.add(rb);
            accentRow.add(rb);
            if (value == selectedAccent[0]) {
                rb.setSelected(true);
            }
            rb.addActionListener(e -> {
                if (rb.isSelected()) {
                    selectedAccent[0] = value;
                    onSelectionChanged.run();
                }
            });
        }

        CheckBox rounded = new CheckBox("Rounded Buttons");
        rounded.setUIID("InitializrChoice");
        rounded.setSelected(roundedButtons[0]);
        rounded.addActionListener(e -> {
            roundedButtons[0] = rounded.isSelected();
            onSelectionChanged.run();
        });

        TextArea cssEditor = new TextArea(customThemeCss[0], 8, 30);
        cssEditor.setName("appendCustomCssEditor");
        cssEditor.setUIID("InitializrField");
        cssEditor.setHint("/* Appended to generated theme.css */\nButton {\n    border-radius: 0;\n}");
        cssEditor.setGrowByContent(true);
        cssEditor.addDataChangedListener((type, index) -> {
            customThemeCss[0] = cssEditor.getText();
            onSelectionChanged.run();
        });

        return BoxLayout.encloseY(
                labeledField("Mode", modeRow),
                labeledField("Accent", accentRow),
                rounded,
                labeledField("Append Custom CSS", cssEditor),
                customCssError
        );
    }


    private Container createJavaOptionsPanel(ProjectOptions.JavaVersion[] javaVersion, Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(2, 1));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();

        for (ProjectOptions.JavaVersion version : ProjectOptions.JavaVersion.values()) {
            RadioButton button = new RadioButton(version.label);
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

    private Container createTemplateSelector(Template[] selectedTemplate, RadioButton[] templateButtons, Runnable onSelectionChanged) {
        Container selector = new Container(new GridLayout(2, 2));
        selector.setUIID("InitializrChoicesGrid");
        ButtonGroup group = new ButtonGroup();
        for (Template template : Template.values()) {
            RadioButton button = new RadioButton(formatEnumLabel(template.name()));
            button.setToggle(true);
            button.setUIID("InitializrChoice");
            group.add(button);
            templateButtons[template.ordinal()] = button;
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

    private Container createResponsiveBody(Container leftColumn, Container rightColumn) {
        boolean wide = getDisplayWidth() >= convertToPixels(80f);
        if (wide) {
            Container grid = new Container(new GridLayout(1, 2));
            grid.add(leftColumn);
            grid.add(rightColumn);
            return grid;
        }
        Container stacked = new Container(new BorderLayout());
        stacked.add(BorderLayout.NORTH, leftColumn);
        stacked.add(BorderLayout.CENTER, rightColumn);
        return stacked;
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
        FontImage.setMaterialIcon(help, FontImage.MATERIAL_HELP_OUTLINE);
        help.addActionListener(e -> Dialog.show(helpTitle, helpBody, "OK", null));
        Container header = new Container(new BorderLayout());
        header.add(BorderLayout.CENTER, l);
        header.add(BorderLayout.EAST, help);
        return BoxLayout.encloseY(header, input);
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

    private boolean supportsLivePreview(Template template) {
        return template == Template.BAREBONES || template == Template.KOTLIN;
    }

    private void initWebsiteThemeSync(Form form) {
        WebsiteThemeNative websiteThemeNative = NativeLookup.create(WebsiteThemeNative.class);
        if (websiteThemeNative == null || !websiteThemeNative.isSupported()) {
            return;
        }
        websiteDarkMode = websiteThemeNative.isDarkMode();
        applyWebsiteTheme(form, websiteDarkMode);
        form.refreshTheme();
        UITimer.timer(900, true, form, () -> {
            boolean dark = websiteThemeNative.isDarkMode();
            if (dark != websiteDarkMode) {
                websiteDarkMode = dark;
                applyWebsiteTheme(form, dark);
                form.refreshTheme();
            }
        });
    }

    private void applyWebsiteTheme(Component component, boolean dark) {
        String uiid = component.getUIID();
        String themed = themedUiid(uiid, dark);
        if (!uiid.equals(themed)) {
            component.setUIID(themed);
        }
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyWebsiteTheme(cnt.getComponentAt(i), dark);
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
            switch (uiid) {
                case "InitializrForm":
                case "InitializrRoot":
                case "InitializrHeaderCard":
                case "InitializrCard":
                case "InitializrHeroTitle":
                case "InitializrHeroSubtitle":
                case "InitializrSectionTitle":
                case "InitializrFieldLabel":
                case "InitializrField":
                case "InitializrFieldHint":
                case "InitializrChoice":
                case "InitializrSummary":
                case "InitializrTip":
                case "InitializrValidationError":
                case "InitializrHelpButton":
                case "AccordionItem":
                case "AccordionContent":
                case "AccordionArrow":
                case "AccordionOpenCloseIcon":
                    return uiid + "Dark";
                default:
                    return uiid;
            }
        }
        if (!uiid.endsWith("Dark")) {
            return uiid;
        }
        String base = uiid.substring(0, uiid.length() - "Dark".length());
        switch (base) {
            case "InitializrForm":
            case "InitializrRoot":
            case "InitializrHeaderCard":
            case "InitializrCard":
            case "InitializrHeroTitle":
            case "InitializrHeroSubtitle":
            case "InitializrSectionTitle":
            case "InitializrFieldLabel":
            case "InitializrField":
            case "InitializrFieldHint":
            case "InitializrChoice":
            case "InitializrSummary":
            case "InitializrTip":
            case "InitializrValidationError":
            case "InitializrHelpButton":
            case "AccordionItem":
            case "AccordionContent":
            case "AccordionArrow":
            case "AccordionOpenCloseIcon":
                return base;
            default:
                return uiid;
        }
    }

    private void setEnabledRecursive(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                setEnabledRecursive(cnt.getComponentAt(i), enabled);
            }
        }
    }

    private String createSummary(String appName, String packageName, Template template, IDE ide, ProjectOptions options) {
        String safeApp = appName == null ? "" : appName.trim();
        String safePackage = packageName == null ? "" : packageName.trim();
        return "App: " + safeApp + "\n"
                + "Package: " + safePackage + "\n"
                + "Template: " + template.name() + "\n"
                + "IDE: " + ide.name() + "\n"
                + "Theme: " + options.themeMode.name() + "\n"
                + "Accent: " + options.accent.name() + "\n"
                + "Rounded Buttons: " + (options.roundedButtons ? "Yes" : "No") + "\n"
                + "Localization Bundles: " + (options.includeLocalizationBundles ? "Yes" : "No") + "\n"
                + "Preview Language: " + options.previewLanguage.label + "\n"
                + "Java: " + options.javaVersion.label + "\n"
                + "Append Custom CSS: " + (options.customThemeCss == null || options.customThemeCss.trim().length() == 0 ? "No" : "Yes") + "\n"
                + "Kotlin: " + (template.IS_KOTLIN ? "Yes" : "No");
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
