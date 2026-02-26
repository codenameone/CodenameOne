package com.codename1.initializr.ui;

import com.codename1.components.ImageViewer;
import com.codename1.components.ToastBar;
import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.Template;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.InterFormContainer;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.Hashtable;

public class TemplatePreviewPanel {
    private final Container root;
    private final Container previewHolder;
    private final ImageViewer staticPreview;
    private final Label staticPreviewFallback;
    private InterFormContainer liveFormPreview;

    private Template template;
    private ProjectOptions options = ProjectOptions.defaults();

    public TemplatePreviewPanel(Template template) {
        this.template = template;

        staticPreview = new ImageViewer();
        staticPreviewFallback = new Label("Preview unavailable");
        staticPreviewFallback.setUIID("InitializrTip");

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

    private Form createBarebonesPreviewForm(ProjectOptions options) {
        installBundle(options);
        Form form = new Form("Hi World", BoxLayout.y());
        Button helloButton = new Button("Hello World");
        helloButton.setUIID("Button");
        helloButton.addActionListener(e -> Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null));
        form.add(helloButton);
        Command menuCommand = form.getToolbar().addMaterialCommandToSideMenu("Hello Command",
                FontImage.MATERIAL_CHECK, 4, e -> Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null));
        form.getToolbar().addMaterialCommandToLeftBar("", FontImage.MATERIAL_MENU, e ->
                ToastBar.showInfoMessage("Side menu is not available in embedded preview mode."));
        Button menuButton = form.getToolbar().findCommandComponent(menuCommand);
        applyLivePreviewOptions(form, helloButton, menuButton, options);
        return form;
    }

    private void installBundle(ProjectOptions options) {
        if (!options.includeLocalizationBundles) {
            UIManager.getInstance().setBundle(null);
            return;
        }
        Hashtable<String, String> bundle = findBundle(options.previewLanguage);
        UIManager.getInstance().setBundle(bundle);
    }

    private Hashtable<String, String> findBundle(ProjectOptions.PreviewLanguage language) {
        Resources resources = Resources.getGlobalResources();
        if (resources == null || language == null) {
            return null;
        }
        Hashtable<String, String> exact = resources.getL10N("messages", language.bundleSuffix);
        if (exact != null) {
            return exact;
        }
        int split = language.bundleSuffix.indexOf('_');
        if (split > 0) {
            return resources.getL10N("messages", language.bundleSuffix.substring(0, split));
        }
        return null;
    }

    private void updateMode() {
        previewHolder.removeAll();
        if (template == Template.BAREBONES || template == Template.KOTLIN) {
            Form liveForm = createBarebonesPreviewForm(options);
            liveFormPreview = new InterFormContainer(liveForm);
            liveFormPreview.setUIID("InitializrLiveFrame");
            previewHolder.add(BorderLayout.CENTER, liveFormPreview);
        } else {
            staticPreview.setImage(Resources.getGlobalResources().getImage(template.IMAGE_NAME));
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
