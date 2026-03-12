package com.codename1.initializr.ui;

import com.codename1.components.ImageViewer;
import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.ProjectOptions.PreviewLanguage;
import com.codename1.initializr.model.Template;
import com.codename1.io.Log;
import com.codename1.io.Properties;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.InterFormContainer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.InputStream;
import java.util.Hashtable;

import static com.codename1.ui.CN.getResourceAsStream;

public class TemplatePreviewPanel {
    private final Container root;
    private final Container previewHolder;
    private final ImageViewer staticPreview;
    private InterFormContainer liveFormPreview;

    private Template template;
    private ProjectOptions options = ProjectOptions.defaults();
    private Form lastLiveForm;
    private Button lastLiveHelloButton;

    public TemplatePreviewPanel(Template template) {
        this.template = template;

        staticPreview = new ImageViewer();
        previewHolder = new Container(new BorderLayout());
        previewHolder.setUIID("InitializrPreviewHolder");

        root = new Container(new BorderLayout());
        root.setUIID("InitializrCard");
        root.add(BorderLayout.CENTER, previewHolder);
        updateMode();
    }

    public Component getComponent() {
        return root;
    }

    public void setTemplate(Template template) {
        this.template = template;
        updateMode();
    }

    public void setOptions(ProjectOptions options) {
        this.options = options == null ? ProjectOptions.defaults() : options;
        updateMode();
    }

    public void showUpdatedLivePreview() {
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            Form liveForm = createBarebonesPreviewForm(options);
            InterFormContainer next = new InterFormContainer(liveForm);
            next.setUIID("InitializrLiveFrame");
            liveFormPreview = next;
            previewHolder.removeAll();
            previewHolder.add(BorderLayout.CENTER, liveFormPreview);
            previewHolder.revalidate();
        }
    }

    private Form createBarebonesPreviewForm(ProjectOptions options) {
        installBundle(options);
        Form form = new Form("Hi World", BoxLayout.y());
        Button helloButton = new Button("Hello World");
        helloButton.setName("previewHelloButton");
        helloButton.setUIID("Button");
        helloButton.addActionListener(e -> Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null));
        form.add(helloButton);
        form.getToolbar().addMaterialCommandToSideMenu("Hello Command",
                FontImage.MATERIAL_CHECK, 4, e -> Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null));
        applyLivePreviewOptions(form, helloButton, null, options);
        validateCustomCss(options.customThemeCss);
        lastLiveForm = form;
        lastLiveHelloButton = helloButton;
        return form;
    }

    Form getLastLiveFormForTesting() {
        return lastLiveForm;
    }

    Button getLastLiveHelloButtonForTesting() {
        return lastLiveHelloButton;
    }

    private void installBundle(ProjectOptions options) {
        if (!options.includeLocalizationBundles) {
            UIManager.getInstance().setBundle(null);
            return;
        }
        Hashtable<String, String> bundle = findBundle(options.previewLanguage);
        UIManager.getInstance().setBundle(bundle);
    }

    private Hashtable<String, String> findBundle(PreviewLanguage language) {
        if (language == null) {
            return null;
        }
        Resources resources = Resources.getGlobalResources();
        if (resources != null && language != PreviewLanguage.ENGLISH) {
            try {
                Hashtable<String, String> exact = resources.getL10N("messages", language.bundleSuffix);
                if (exact != null) {
                    return exact;
                }
                int split = language.bundleSuffix.indexOf('_');
                if (split > 0) {
                    Hashtable<String, String> languageOnly = resources.getL10N("messages", language.bundleSuffix.substring(0, split));
                    if (languageOnly != null) {
                        return languageOnly;
                    }
                }
            } catch (RuntimeException err) {
                Log.e(err);
            }
        }

        String[] candidates = language.bundleSuffix.indexOf('_') > 0
                ? new String[]{"/messages_" + language.bundleSuffix + ".properties", "/messages_" + language.bundleSuffix.substring(0, language.bundleSuffix.indexOf('_')) + ".properties", "/messages.properties"}
                : new String[]{"/messages_" + language.bundleSuffix + ".properties", "/messages.properties"};

        for (String path : candidates) {
            Hashtable<String, String> loaded = loadBundleProperties(path);
            if (loaded != null) {
                return loaded;
            }
        }
        return null;
    }

    private Hashtable<String, String> loadBundleProperties(String resourcePath) {
        try (InputStream input = getResourceAsStream(resourcePath)) {
            if (input == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(input);
            Hashtable<String, String> out = new Hashtable<String, String>();
            for (Object key : props.keySet()) {
                String keyString = key.toString();
                out.put(keyString, props.getProperty(keyString));
            }
            return out;
        } catch (Exception err) {
            Log.e(err);
            return null;
        }
    }

    private void validateCustomCss(String rawCustomCss) {
        String customCss = normalizeCustomCss(rawCustomCss);
        if (customCss.length() > 0) {
            String wrappedCustomCss = "\n/* Initializr Appended Custom CSS */\n" + customCss + "\n";
            try {
                com.codename1.ui.css.CSSThemeCompiler compiler = new com.codename1.ui.css.CSSThemeCompiler();
                com.codename1.ui.util.MutableResource resource = new com.codename1.ui.util.MutableResource();
                compiler.compile(wrappedCustomCss, resource, "InitializrLiveThemeValidation");
            } catch (RuntimeException err) {
                throw new IllegalArgumentException(err.getMessage(), err);
            }
        }
    }

    private String normalizeCustomCss(String css) {
        if (css == null) {
            return "";
        }
        String trimmed = css.trim();
        return trimmed.length() == 0 ? "" : trimmed;
    }

    private void updateMode() {
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            Form liveForm = createBarebonesPreviewForm(options);
            InterFormContainer next = new InterFormContainer(liveForm);
            next.setUIID("InitializrLiveFrame");
            liveFormPreview = next;
            previewHolder.removeAll();
            previewHolder.add(BorderLayout.CENTER, liveFormPreview);
        } else {
            staticPreview.setImage(Resources.getGlobalResources().getImage(template.IMAGE_NAME));
            previewHolder.removeAll();
            previewHolder.add(BorderLayout.CENTER, staticPreview);
        }
        previewHolder.revalidate();
    }

    private void applyLivePreviewOptions(Form form, Button button, Button menuButton, ProjectOptions options) {
        String mode = options.themeMode == ProjectOptions.ThemeMode.DARK ? "Dark" : "Light";
        String accent = accentName(options.accent);
        boolean clean = options.accent == ProjectOptions.Accent.DEFAULT;

        if ("Light".equals(mode) && clean) {
            form.getContentPane().setUIID("Container");
            form.getToolbar().setUIID("Toolbar");
            form.getToolbar().getTitleComponent().setUIID("Title");
            if (menuButton != null) {
                menuButton.setUIID("Command");
            }
            button.setUIID("Button");
            return;
        }

        if ("Dark".equals(mode)) {
            form.getContentPane().setUIID("InitializrLiveContentDark");
            form.getToolbar().setUIID("InitializrLiveToolbarDark");
            form.getToolbar().getTitleComponent().setUIID("InitializrLiveTitleDark");
            if (menuButton != null) {
                menuButton.setUIID("InitializrLiveMenuDark");
            }
            if (clean) {
                button.setUIID("InitializrLiveButtonDarkClean");
                return;
            }
        } else {
            form.getContentPane().setUIID("Container");
            form.getToolbar().setUIID("Toolbar");
            form.getToolbar().getTitleComponent().setUIID("Title");
            if (menuButton != null) {
                menuButton.setUIID("Command");
            }
        }

        String shape = options.roundedButtons ? "Round" : "Square";
        button.setUIID("InitializrLiveButton" + mode + accent + shape);
    }

    private static String accentName(ProjectOptions.Accent accent) {
        if (accent == ProjectOptions.Accent.DEFAULT) {
            return "Clean";
        }
        if (accent == ProjectOptions.Accent.BLUE) {
            return "Blue";
        }
        if (accent == ProjectOptions.Accent.ORANGE) {
            return "Orange";
        }
        return "Teal";
    }
}
