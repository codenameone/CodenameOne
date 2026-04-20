package com.codename1.tools.skindesigner;

import com.codename1.system.Lifecycle;
import com.codename1.components.OnOffSwitch;
import com.codename1.components.ScaleImageLabel;
import com.codename1.components.SplitPane;
import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Properties;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.validation.NumericConstraint;
import com.codename1.ui.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipOutputStream;

public class SkinDesigner extends Lifecycle {
    private static final String[] NATIVE_THEMES = {"iOS", "Android", "Windows"};
    private static final String[] NATIVE_THEME_FILES = {"iOS7Theme.res", "android_holo_light.res", "winTheme.res"};
    private boolean websiteDarkMode;

    @Override
    public void runApp() {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        Form skinDesignerForm = new Form("Skin Designer", new BorderLayout());
        skinDesignerForm.setTitle("");
        skinDesignerForm.setUIID("SkinDesignerForm");
        Validator vl = new Validator();
        final Tabs details = new Tabs();
        details.getTabsContainer().setUIID("SkinDesignerTabsContainer");
        details.getTabsContainer().setScrollableX(false);
        final ImageSettings[] imPortraitRef = new ImageSettings[1];
        final ImageSettings[] imLandscapeRef = new ImageSettings[1];

        skinDesignerForm.add(BorderLayout.CENTER, details);

        Picker nativeTheme = new Picker();
        nativeTheme.setUIID("SkinDesignerField");
        nativeTheme.setStrings(NATIVE_THEMES);
        nativeTheme.setSelectedString(NATIVE_THEMES[0]);
        nativeTheme.setRenderingPrototype("XXXXXXXXXXXXXXXXXXX");
        autoSave(nativeTheme, "nativeTheme");

        Picker platformName = new Picker();
        platformName.setUIID("SkinDesignerField");
        platformName.setStrings("ios", "and", "win","rim", "se");
        platformName.setSelectedString("ios");
        platformName.setRenderingPrototype("XXXX");
        autoSave(platformName, "platformName");

        OnOffSwitch tablet = new OnOffSwitch();
        tablet.setUIID("SkinDesignerField");
        tablet.setValue(false);
        autoSave(tablet, "tablet");

        TextField systemFontFamily = new TextField("Helvetica", "System Font Family", 20, TextField.ANY);
        TextField proportionalFontFamily = new TextField("Helvetica", "Proportional Font Family", 20, TextField.ANY);
        TextField monospaceFontFamily = new TextField("Courier", "Monospace Font Family", 20, TextField.ANY);
        styleFields(systemFontFamily, proportionalFontFamily, monospaceFontFamily);
        autoSave(systemFontFamily, "systemFontFamily");
        autoSave(proportionalFontFamily, "proportionalFontFamily");
        autoSave(monospaceFontFamily, "monospaceFontFamily");

        TextField smallFontSize = new TextField("11", "Small Font Size", 20, TextField.NUMERIC);
        TextField mediumFontSize = new TextField("14", "Medium Font Size", 20, TextField.NUMERIC);
        TextField largeFontSize = new TextField("20", "Large Font Size", 20, TextField.NUMERIC);
        styleFields(smallFontSize, mediumFontSize, largeFontSize);
        autoSave(smallFontSize, "smallFontSize");
        autoSave(mediumFontSize, "mediumFontSize");
        autoSave(largeFontSize, "largeFontSize");

        TextField pixelRatio = new TextField("6.4173236936575", "Pixel Ratio - pixels per millimeter", 20, TextField.DECIMAL);
        pixelRatio.setUIID("SkinDesignerField");
        autoSave(pixelRatio, "pixelRatio");

        Picker overrideNamePrimary = new Picker();
        overrideNamePrimary.setUIID("SkinDesignerField");
        overrideNamePrimary.setStrings("phone", "tablet", "desktop");
        overrideNamePrimary.setSelectedString("phone");
        overrideNamePrimary.setRenderingPrototype("XXXXXXXX");
        autoSave(overrideNamePrimary, "overrideNamePrimary");

        Picker overrideNameSecondary = new Picker();
        overrideNameSecondary.setUIID("SkinDesignerField");
        overrideNameSecondary.setStrings("ios", "android", "windows");
        overrideNameSecondary.setSelectedString("ios");
        overrideNameSecondary.setRenderingPrototype("XXXXXXXX");
        autoSave(overrideNameSecondary, "overrideNameSecondary");

        Picker overrideNameLast = new Picker();
        overrideNameLast.setUIID("SkinDesignerField");
        overrideNameLast.setStrings("iphone", "ipad", "android-phone", "android-tablet", "desktop");
        overrideNameLast.setSelectedString("iphone");
        overrideNameLast.setRenderingPrototype("XXXXXXXX");
        autoSave(overrideNameLast, "overrideNameLast");

        Container settingsContainer = BoxLayout.encloseY(
                labeledFieldTitle("Native Theme"),
                nativeTheme,
                labeledFieldTitle("Platform Name"),
                platformName,
                BorderLayout.center(labeledFieldTitle("Tablet")).add(BorderLayout.EAST, tablet),
                labeledFieldTitle(systemFontFamily.getHint()),
                systemFontFamily,
                labeledFieldTitle(proportionalFontFamily.getHint()),
                proportionalFontFamily,
                labeledFieldTitle(monospaceFontFamily.getHint()),
                monospaceFontFamily,
                labeledFieldTitle(smallFontSize.getHint()),
                smallFontSize,
                labeledFieldTitle(mediumFontSize.getHint()),
                mediumFontSize,
                labeledFieldTitle(largeFontSize.getHint()),
                largeFontSize,
                labeledFieldTitle(pixelRatio.getHint()),
                pixelRatio,
                labeledFieldTitle("Platform Overrides"),
                BoxLayout.encloseX(overrideNamePrimary, overrideNameSecondary, overrideNameLast)
        );
        settingsContainer.setUIID("SkinDesignerCard");
        settingsContainer.setScrollableY(true);

        vl.addConstraint(smallFontSize, new NumericConstraint(false, 5, 400, "Font size must be a valid integer in the 5-400 range")).
                addConstraint(mediumFontSize, new NumericConstraint(false, 5, 400, "Font size must be a valid integer in the 5-400 range")).
                addConstraint(largeFontSize, new NumericConstraint(false, 5, 400, "Font size must be a valid integer in the 5-400 range")).
                addConstraint(pixelRatio, new NumericConstraint(true, 0.1, 60, "PixelRatio is a positive decimal size in the range of 0.1 to 60")).
                setShowErrorMessageForFocusedComponent(true);

        Runnable saveAction = () -> {
            byte[] data = createSkinFile(imPortraitRef[0], imLandscapeRef[0], nativeTheme, platformName, tablet, systemFontFamily,
                    proportionalFontFamily, monospaceFontFamily, smallFontSize, mediumFontSize, largeFontSize,
                    pixelRatio, overrideNamePrimary, overrideNameSecondary, overrideNameLast);
            if(data != null) {
                FileSystemStorage fs = FileSystemStorage.getInstance();
                try(OutputStream os = fs.openOutputStream(fs.getAppHomePath() + "skin-file.skin")) {
                    os.write(data);
                } catch(IOException err) {
                    Log.e(err);
                    ToastBar.showErrorMessage("Error wring skin file " + err);
                }
                // in the JavaScript port this will trigger the download dialog
                Display.getInstance().execute(fs.getAppHomePath() + "skin-file.skin");
            }
        };

        imPortraitRef[0] = createImageSettings("/skin.png", "port", vl, () -> showHelpForm(skinDesignerForm), saveAction);
        imLandscapeRef[0] = createImageSettings("/skin_l.png", "lan", vl, () -> showHelpForm(skinDesignerForm), saveAction);
        ImageSettings imPortrait = imPortraitRef[0];
        ImageSettings imLandscape = imLandscapeRef[0];
        details.addTab("Portrait", FontImage.MATERIAL_STAY_CURRENT_PORTRAIT, 4.5f, imPortrait.getContainer());
        details.addTab("Landscape", FontImage.MATERIAL_STAY_CURRENT_LANDSCAPE, 4.5f, imLandscape.getContainer());
        details.addTab("Settings", FontImage.MATERIAL_SETTINGS, 3.5f, settingsContainer);

        skinDesignerForm.show();
        initThemeFromUrl(skinDesignerForm, details);
    }

    private Label labeledFieldTitle(String text) {
        Label label = new Label(text);
        label.setUIID("SkinDesignerFieldLabel");
        return label;
    }

    private void styleFields(TextField... fields) {
        for (TextField field : fields) {
            field.setUIID("SkinDesignerField");
        }
    }

    private void styleActionButton(Button button, char materialIcon) {
        button.setUIID("SkinDesignerActionButton");
        FontImage.setMaterialIcon(button, materialIcon);
        button.getAllStyles().setAlignment(Component.CENTER);
        button.setGap(CN.convertToPixels(0.7f));
    }

    private void styleIconActionButton(Button button, char materialIcon) {
        styleActionButton(button, materialIcon);
        button.setText("");
        button.setGap(0);
    }

    private void initThemeFromUrl(Form form, Tabs details) {
        websiteDarkMode = readThemeFromUrl();
        Display.getInstance().setDarkMode(websiteDarkMode);
        applyWebsiteTheme(form, websiteDarkMode);
        applyTabsTheme(details, websiteDarkMode);
        form.refreshTheme();
        UITimer.timer(900, true, form, () -> {
            boolean dark = readThemeFromUrl();
            if (dark != websiteDarkMode) {
                websiteDarkMode = dark;
                Display.getInstance().setDarkMode(dark);
                applyWebsiteTheme(form, dark);
                applyTabsTheme(details, dark);
                form.refreshTheme();
            }
        });
    }

    private void applyTabsTheme(Tabs tabs, boolean dark) {
        if (tabs == null) {
            return;
        }
        String tabsUiid = dark ? "SkinDesignerTabsContainerDark" : "SkinDesignerTabsContainer";
        String tabUiid = dark ? "TabDark" : "Tab";
        tabs.setUIID(tabsUiid);
        tabs.setTabUIID(tabUiid);
        Container tabsContainer = tabs.getTabsContainer();
        for (int i = 0; i < tabsContainer.getComponentCount(); i++) {
            tabsContainer.getComponentAt(i).setUIID(tabUiid);
        }
        tabs.refreshTheme();
        tabs.revalidate();
    }

    private boolean readThemeFromUrl() {
        String href = CN.getProperty("browser.window.location.href", "");
        String theme = queryParam(href, "theme");
        if ("dark".equalsIgnoreCase(theme)) {
            return true;
        }
        if ("light".equalsIgnoreCase(theme)) {
            return false;
        }
        return Display.getInstance().isDarkMode();
    }

    private String queryParam(String href, String name) {
        if (href == null || href.length() == 0) {
            return null;
        }
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
            if (pair.startsWith(prefix) && pair.length() > prefix.length()) {
                return pair.substring(prefix.length());
            }
        }
        return null;
    }

    private void applyWebsiteTheme(Container component, boolean dark) {
        for (int i = 0; i < component.getComponentCount(); i++) {
            Component child = component.getComponentAt(i);
            String uiid = child.getUIID();
            String themed = themedUiid(uiid, dark);
            if (uiid != null && !uiid.equals(themed)) {
                child.setUIID(themed);
            }
            if (child instanceof Container) {
                applyWebsiteTheme((Container) child, dark);
            }
        }
        String containerUiid = component.getUIID();
        String themedContainer = themedUiid(containerUiid, dark);
        if (containerUiid != null && !containerUiid.equals(themedContainer)) {
            component.setUIID(themedContainer);
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
                case "SkinDesignerForm":
                case "SkinDesignerTabsContainer":
                case "SkinDesignerCard":
                case "SkinDesignerField":
                case "SkinDesignerFieldLabel":
                case "SkinDesignerTabBar":
                case "SkinDesignerTabButton":
                case "SkinDesignerTabButtonSelected":
                case "Toolbar":
                case "Title":
                case "Command":
                case "Tab":
                case "TabSelected":
                case "TabsContainer":
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
            case "SkinDesignerForm":
            case "SkinDesignerTabsContainer":
            case "SkinDesignerCard":
            case "SkinDesignerField":
            case "SkinDesignerFieldLabel":
            case "SkinDesignerTabBar":
            case "SkinDesignerTabButton":
            case "SkinDesignerTabButtonSelected":
            case "Toolbar":
            case "Title":
            case "Command":
            case "Tab":
            case "TabSelected":
            case "TabsContainer":
                return base;
            default:
                return uiid;
        }
    }

    interface ImageSettings {
        Container getContainer();
        Image createSkinOverlay();
        Image getSkinImage();
        int getScreenX();
        int getScreenY();
        int getScreenWidth();
        int getScreenHeight();
        int getSafeX();
        int getSafeY();
        int getSafeWidth();
        int getSafeHeight();
    }

    private void autoSave(TextArea ta, String preferencesKey) {
        String val = Preferences.get(preferencesKey, null);
        if(val != null) {
            ta.setText(val);
        }
        ta.addActionListener(e -> {
            Preferences.set(preferencesKey, ta.getText());
        });
    }

    private void autoSave(OnOffSwitch o, String preferencesKey) {
        boolean val = Preferences.get(preferencesKey, o.isValue());
        if(val != o.isValue()) {
            o.setValue(val);
        }
        o.addActionListener(e -> {
            Preferences.set(preferencesKey, o.isValue());
        });
    }

    private void autoSave(Picker p, String preferencesKey) {
        String val = Preferences.get(preferencesKey, null);
        if(val != null) {
            p.setSelectedString(val);
        }
        p.addActionListener(e -> {
            Preferences.set(preferencesKey, p.getSelectedString());
        });
    }

    private ImageSettings createImageSettings(String imageFile, String prefix, Validator vl, Runnable helpCallback, Runnable saveCallback) {
        Image img = null;
        try {
            img = Image.createImage(Display.getInstance().getResourceAsStream(getClass(), imageFile));
        } catch(IOException err) {
            Log.e(err);
        }
        ScaleImageLabel sl = new ScaleImageLabel(img);
        Button imagePicker = new Button("Select Image");
        imagePicker.setUIID("SkinDesignerActionButton");
        imagePicker.setTooltip("Choose a skin image from your gallery");
        imagePicker.addActionListener((e) -> {
            Display.getInstance().openGallery((ee) -> {
                if(ee != null && ee.getSource() != null) {
                    try {
                        String fileName = (String)ee.getSource();
                        sl.setIcon(Image.createImage(fileName));
                        sl.getParent().revalidate();
                        Util.copy(FileSystemStorage.getInstance().openInputStream(fileName),
                                Storage.getInstance().createOutputStream(prefix + ".png"));
                    } catch(IOException err) {
                        ToastBar.showErrorMessage("Error Loading Image: " + err);
                    }
                }
            }, Display.GALLERY_IMAGE);
        });
        if(Storage.getInstance().exists(prefix + ".png")) {
            try(InputStream is = Storage.getInstance().createInputStream(prefix + ".png")) {
                sl.setIcon(Image.createImage(is));
            } catch(IOException err) {
                Log.e(err);
            }
        }

        final TextField screenWidthPixels = new TextField("320", "Width", 8, TextField.NUMERIC);
        final TextField screenHeightPixels = new TextField("480", "Height", 8, TextField.NUMERIC);
        final TextField screenPositionX = new TextField("40", "X", 8, TextField.NUMERIC);
        final TextField screenPositionY = new TextField("40", "Y", 8, TextField.NUMERIC);
        final TextField safeX = new TextField("40", "Safe X", 8, TextField.NUMERIC);
        final TextField safeY = new TextField("40", "Safe Y", 8, TextField.NUMERIC);
        final TextField safeWidth = new TextField("320", "Safe Width", 8, TextField.NUMERIC);
        final TextField safeHeight = new TextField("480", "Safe Height", 8, TextField.NUMERIC);
        final TextField floodTolerance = new TextField("24", "Color Tol (0-441)", 4, TextField.NUMERIC);
        if("lan".equals(prefix) && Preferences.get("lanX", null) == null) {
            String portraitX = Preferences.get("portX", null);
            String portraitY = Preferences.get("portY", null);
            String portraitWidth = Preferences.get("portWidth", null);
            String portraitHeight = Preferences.get("portHeight", null);
            if(portraitX != null && portraitY != null && portraitWidth != null && portraitHeight != null) {
                screenPositionX.setText(portraitY);
                screenPositionY.setText(portraitX);
                screenWidthPixels.setText(portraitHeight);
                screenHeightPixels.setText(portraitWidth);
                safeX.setText(portraitY);
                safeY.setText(portraitX);
                safeWidth.setText(portraitHeight);
                safeHeight.setText(portraitWidth);
            }
        }
        styleFields(screenWidthPixels, screenHeightPixels, screenPositionX, screenPositionY);
        styleFields(safeX, safeY, safeWidth, safeHeight, floodTolerance);
        autoSave(screenWidthPixels, prefix + "Width");
        autoSave(screenHeightPixels, prefix + "Height");
        autoSave(screenPositionX, prefix + "X");
        autoSave(screenPositionY, prefix + "Y");
        autoSave(safeX, prefix + "SafeX");
        autoSave(safeY, prefix + "SafeY");
        autoSave(safeWidth, prefix + "SafeWidth");
        autoSave(safeHeight, prefix + "SafeHeight");
        autoSave(floodTolerance, prefix + "FloodTolerance");
        vl.addConstraint(screenWidthPixels, new NumericConstraint(false, 20, 5000, "Screen size must be a valid integer in the 20-5000 range")).
                addConstraint(screenHeightPixels, new NumericConstraint(false, 20, 5000, "Screen size must be a valid integer in the 20-5000 range")).
                addConstraint(screenPositionX, new NumericConstraint(false, 0, 5000, "Screen position must be a valid integer in the 0-5000 range")).
                addConstraint(screenPositionY, new NumericConstraint(false, 0, 5000, "Screen position must be a valid integer in the 0-5000 range")).
                addConstraint(safeX, new NumericConstraint(false, 0, 5000, "Safe area X must be a valid integer in the 0-5000 range")).
                addConstraint(safeY, new NumericConstraint(false, 0, 5000, "Safe area Y must be a valid integer in the 0-5000 range")).
                addConstraint(safeWidth, new NumericConstraint(false, 1, 5000, "Safe area width must be a valid integer in the 1-5000 range")).
                addConstraint(safeHeight, new NumericConstraint(false, 1, 5000, "Safe area height must be a valid integer in the 1-5000 range")).
                addConstraint(floodTolerance, new NumericConstraint(false, 0, 441, "Tolerance must be 0-441"));
        if("lan".equals(prefix)) {
            String portraitX = Preferences.get("portX", null);
            String portraitY = Preferences.get("portY", null);
            String portraitWidth = Preferences.get("portWidth", null);
            String portraitHeight = Preferences.get("portHeight", null);
            if(portraitX != null && portraitY != null && portraitWidth != null && portraitHeight != null &&
                    portraitX.equals(screenPositionX.getText()) && portraitY.equals(screenPositionY.getText()) &&
                    portraitWidth.equals(screenWidthPixels.getText()) && portraitHeight.equals(screenHeightPixels.getText())) {
                screenPositionX.setText(portraitY);
                screenPositionY.setText(portraitX);
                screenWidthPixels.setText(portraitHeight);
                screenHeightPixels.setText(portraitWidth);
                safeX.setText(portraitY);
                safeY.setText(portraitX);
                safeWidth.setText(portraitHeight);
                safeHeight.setText(portraitWidth);
            }
        }

        final OnOffSwitch useSafeArea = new OnOffSwitch();
        useSafeArea.setUIID("SkinDesignerField");
        useSafeArea.setValue(false);
        useSafeArea.setTooltip("Enable a separate safe area inside the screen");
        autoSave(useSafeArea, prefix + "UseSafeArea");
        Runnable applySafeEnabled = () -> {
            boolean enabled = useSafeArea.isValue();
            safeX.setEnabled(enabled);
            safeY.setEnabled(enabled);
            safeWidth.setEnabled(enabled);
            safeHeight.setEnabled(enabled);
        };
        applySafeEnabled.run();
        useSafeArea.addActionListener(e -> {
            boolean enabled = useSafeArea.isValue();
            if (enabled) {
                int sx = screenPositionX.getAsInt(0);
                int sy = screenPositionY.getAsInt(0);
                int sw = screenWidthPixels.getAsInt(0);
                int sh = screenHeightPixels.getAsInt(0);
                int curX = safeX.getAsInt(sx);
                int curY = safeY.getAsInt(sy);
                int curW = safeWidth.getAsInt(sw);
                int curH = safeHeight.getAsInt(sh);
                if (curW < 1 || curW > sw) { curW = sw; }
                if (curH < 1 || curH > sh) { curH = sh; }
                if (curX < sx) { curX = sx; }
                if (curY < sy) { curY = sy; }
                if (curX + curW > sx + sw) { curX = sx + sw - curW; }
                if (curY + curH > sy + sh) { curY = sy + sh - curH; }
                safeX.setText("" + curX);
                safeY.setText("" + curY);
                safeWidth.setText("" + curW);
                safeHeight.setText("" + curH);
            }
            applySafeEnabled.run();
        });

        Button aim = new Button();
        styleIconActionButton(aim, FontImage.MATERIAL_PAN_TOOL);
        aim.setTooltip("Visually position the screen and safe area");

        aim.addActionListener(e ->
                aimPosition(sl.getIcon(),
                        screenPositionX,
                        screenPositionY,
                        safeX,
                        safeY,
                        safeWidth,
                        safeHeight,
                        screenWidthPixels.getAsInt(768),
                        screenHeightPixels.getAsInt(1024),
                        useSafeArea));

        Button helpButton = new Button();
        styleIconActionButton(helpButton, FontImage.MATERIAL_HELP);
        helpButton.setTooltip("Open help");
        helpButton.addActionListener(e -> helpCallback.run());

        Button saveButton = new Button();
        styleIconActionButton(saveButton, FontImage.MATERIAL_SAVE);
        saveButton.setTooltip("Save the skin file");
        saveButton.addActionListener(e -> saveCallback.run());

        ScaleImageLabel maskLabel = new ScaleImageLabel();
        OnOffSwitch useMask = new OnOffSwitch();
        useMask.setValue(false);
        useMask.setUIID("SkinDesignerField");
        useMask.setTooltip("Use the detected mask instead of a simple rectangle");
        autoSave(useMask, prefix + "UseMask");
        Button detectScreenButton = new Button("Detect Screen by Color");
        detectScreenButton.setUIID("SkinDesignerActionButton");
        detectScreenButton.setTooltip("Auto-detect the screen area via flood-fill from the center pixel");
        detectScreenButton.addActionListener(e -> {
            Image source = sl.getIcon();
            if(source == null) {
                ToastBar.showErrorMessage("Please select a skin image first");
                return;
            }
            int seedX = screenPositionX.getAsInt(0) + (screenWidthPixels.getAsInt(0) / 2);
            int seedY = screenPositionY.getAsInt(0) + (screenHeightPixels.getAsInt(0) / 2);
            Image generatedMask = createFloodFillMask(source,
                    seedX,
                    seedY,
                    floodTolerance.getAsInt(24));
            if(generatedMask != null) {
                maskLabel.setIcon(generatedMask);
                useMask.setValue(true);
                maskLabel.getParent().revalidate();
                try(OutputStream os = Storage.getInstance().createOutputStream(prefix + ".mask.png")) {
                    ImageIO.getImageIO().save(generatedMask, os, ImageIO.FORMAT_PNG, 1);
                } catch(IOException err) {
                    Log.e(err);
                    ToastBar.showErrorMessage("Error saving generated mask: " + err.getMessage());
                }
            }
        });

        if(Storage.getInstance().exists(prefix + ".mask.png")) {
            try(InputStream is = Storage.getInstance().createInputStream(prefix + ".mask.png")) {
                maskLabel.setIcon(Image.createImage(is));
            } catch(IOException err) {
                Log.e(err);
            }
        }

        Container actionButtons = FlowLayout.encloseCenter(aim, helpButton, saveButton);
        Container detectionButtons = FlowLayout.encloseCenter(detectScreenButton, floodTolerance, useMask);
        Container safeAreaHeader = BorderLayout.center(labeledFieldTitle("Safe Area (X/Y/Width/Height)"))
                .add(BorderLayout.EAST, useSafeArea);
        Container controls = BoxLayout.encloseY(
                imagePicker,
                labeledFieldTitle("Screen Position (X/Y/Width/Height)"),
                GridLayout.encloseIn(4, screenPositionX, screenPositionY, screenWidthPixels, screenHeightPixels),
                safeAreaHeader,
                GridLayout.encloseIn(4, safeX, safeY, safeWidth, safeHeight),
                labeledFieldTitle("Screen Mask Detection"),
                detectionButtons,
                actionButtons
        );
        controls.setUIID("SkinDesignerCard");
        controls.setScrollableY(true);
        Container preview = BoxLayout.encloseY(sl);
        preview.setUIID("SkinDesignerCard");
        preview.setScrollableY(true);

        int splitType = "lan".equals(prefix) ? SplitPane.VERTICAL_SPLIT : SplitPane.HORIZONTAL_SPLIT;
        Component split = new SplitPane(splitType, controls, preview, "35%", "45%", "55%");
        final Container cnt = BorderLayout.center(split);
        cnt.setUIID("SkinDesignerCard");
        return new ImageSettings() {
            @Override
            public Container getContainer() {
                return cnt;
            }

            @Override
            public Image getSkinImage() {
                Image img = sl.getIcon();
                int[] data = img.getRGB();
                int width = img.getWidth();
                int height = img.getHeight();
                Image mask = maskLabel.getIcon();
                if(useMask.isValue() && mask != null && mask.getWidth() == width && mask.getHeight() == height) {
                    int[] maskRgb = mask.getRGB();
                    for(int i = 0 ; i < maskRgb.length ; i++) {
                        if(maskRgb[i] != 0xff000000) {
                            data[i] = 0;
                        }
                    }
                } else {
                    Rectangle screen = new Rectangle(screenPositionX.getAsInt(0), screenPositionY.getAsInt(0),
                            screenWidthPixels.getAsInt(50), screenHeightPixels.getAsInt(50));
                    for(int x = 0 ; x < width ; x++) {
                        for(int y = 0 ; y < height ; y++) {
                            if(screen.contains(x, y, 1, 1)) {
                                data[y * width + x] = 0;
                            }
                        }
                    }
                }

                return Image.createImage(data, width, height);
            }

            @Override
            public Image createSkinOverlay() {
                Image skinImage = getSkinImage();
                if(skinImage == null) {
                    return null;
                }
                Image m = Image.createImage(skinImage.getWidth(), skinImage.getHeight(), 0);
                Graphics g = m.getGraphics();
                g.setColor(0);
                Image mask = maskLabel.getIcon();
                if(useMask.isValue() && mask != null && mask.getWidth() == skinImage.getWidth() && mask.getHeight() == skinImage.getHeight()) {
                    int[] maskRgb = mask.getRGB();
                    int w = mask.getWidth();
                    int h = mask.getHeight();
                    for(int x = 0; x < w; x++) {
                        for(int y = 0; y < h; y++) {
                            if(maskRgb[y * w + x] == 0xff000000) {
                                g.drawLine(x, y, x, y);
                            }
                        }
                    }
                } else {
                    g.fillRect(screenPositionX.getAsInt(0), screenPositionY.getAsInt(0),
                            screenWidthPixels.getAsInt(50), screenHeightPixels.getAsInt(50));
                }
                return m;
            }

            @Override
            public int getScreenX() {
                return screenPositionX.getAsInt(0);
            }

            @Override
            public int getScreenY() {
                return screenPositionY.getAsInt(0);
            }

            @Override
            public int getScreenWidth() {
                return screenWidthPixels.getAsInt(50);
            }

            @Override
            public int getScreenHeight() {
                return screenHeightPixels.getAsInt(50);
            }

            @Override
            public int getSafeX() {
                return useSafeArea.isValue() ? safeX.getAsInt(getScreenX()) : getScreenX();
            }

            @Override
            public int getSafeY() {
                return useSafeArea.isValue() ? safeY.getAsInt(getScreenY()) : getScreenY();
            }

            @Override
            public int getSafeWidth() {
                return useSafeArea.isValue() ? safeWidth.getAsInt(getScreenWidth()) : getScreenWidth();
            }

            @Override
            public int getSafeHeight() {
                return useSafeArea.isValue() ? safeHeight.getAsInt(getScreenHeight()) : getScreenHeight();
            }
        };
    }

    private Image createFloodFillMask(Image source, int seedX, int seedY, int tolerance) {
        int width = source.getWidth();
        int height = source.getHeight();
        if(seedX < 0 || seedY < 0 || seedX >= width || seedY >= height) {
            ToastBar.showErrorMessage("Seed point is outside image bounds");
            return null;
        }
        int[] src = source.getRGB();
        int[] mask = new int[src.length];
        for(int i = 0; i < mask.length; i++) {
            mask[i] = 0xffffffff;
        }
        boolean[] visited = new boolean[src.length];
        int[] queue = new int[src.length];
        int head = 0;
        int tail = 0;
        int seedIdx = seedY * width + seedX;
        int seedColor = src[seedIdx];
        queue[tail++] = seedIdx;
        visited[seedIdx] = true;
        while(head < tail) {
            int idx = queue[head++];
            int px = idx % width;
            int py = idx / width;
            if(colorMatch(src[idx], seedColor, tolerance)) {
                mask[idx] = 0xff000000;
                if(px > 0) {
                    int n = idx - 1;
                    if(!visited[n]) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if(px < width - 1) {
                    int n = idx + 1;
                    if(!visited[n]) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if(py > 0) {
                    int n = idx - width;
                    if(!visited[n]) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
                if(py < height - 1) {
                    int n = idx + width;
                    if(!visited[n]) {
                        visited[n] = true;
                        queue[tail++] = n;
                    }
                }
            }
        }
        return Image.createImage(mask, width, height);
    }

    private boolean colorMatch(int c1, int c2, int tolerance) {
        int a1 = (c1 >>> 24) & 0xff;
        int a2 = (c2 >>> 24) & 0xff;
        if(a2 < 16) {
            return a1 < 16;
        }
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = c1 & 0xff;
        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = c2 & 0xff;
        return Math.abs(a1 - a2) <= tolerance &&
                Math.abs(r1 - r2) <= tolerance &&
                Math.abs(g1 - g2) <= tolerance &&
                Math.abs(b1 - b2) <= tolerance;
    }

    private void showHelpForm(Form backForm) {
        BrowserComponent help = new BrowserComponent();
        help.setURL("jar:///help.html?theme=" + (websiteDarkMode ? "dark" : "light"));
        Form helpForm = new Form("Help", new BorderLayout());
        helpForm.setUIID("SkinDesignerForm");
        helpForm.add(BorderLayout.CENTER, help);
        Button back = new Button();
        styleIconActionButton(back, FontImage.MATERIAL_ARROW_BACK);
        back.addActionListener(ee -> backForm.showBack());
        Container helpActions = FlowLayout.encloseRight(back);
        helpActions.setUIID("SkinDesignerTabBar");
        helpForm.add(BorderLayout.NORTH, helpActions);
        applyWebsiteTheme(helpForm, websiteDarkMode);
        helpForm.show();
    }

    static final int AIM_MODE_SCREEN = 0;
    static final int AIM_MODE_SAFE = 1;
    static final int AIM_MODE_PAN = 2;

    final class AimView extends Component {
        private final Image img;
        private final int imgW;
        private final int imgH;
        private final int screenW;
        private final int screenH;
        private final TextField xField;
        private final TextField yField;
        private final TextField safeXField;
        private final TextField safeYField;
        private final TextField safeWField;
        private final TextField safeHField;
        private final OnOffSwitch useSafeArea;
        private int mode = AIM_MODE_SCREEN;
        private float zoomMul = 1f;
        private float panNX = 0.5f;
        private float panNY = 0.5f;
        private int lastPX = -1;
        private int lastPY = -1;

        AimView(Image img, TextField xField, TextField yField, TextField safeXField, TextField safeYField,
                TextField safeWField, TextField safeHField, int screenW, int screenH, OnOffSwitch useSafeArea) {
            this.img = img;
            this.imgW = img.getWidth();
            this.imgH = img.getHeight();
            this.xField = xField;
            this.yField = yField;
            this.safeXField = safeXField;
            this.safeYField = safeYField;
            this.safeWField = safeWField;
            this.safeHField = safeHField;
            this.screenW = screenW;
            this.screenH = screenH;
            this.useSafeArea = useSafeArea;
            setUIID("SkinDesignerCard");
            setFocusable(true);
        }

        boolean isSafeAreaEnabled() {
            return useSafeArea != null && useSafeArea.isValue();
        }

        void setMode(int mode) {
            this.mode = mode;
        }

        int getMode() {
            return mode;
        }

        void zoomIn() {
            float nz = Math.min(8f, zoomMul * 1.5f);
            if (nz != zoomMul) {
                zoomMul = nz;
                repaint();
            }
        }

        void zoomOut() {
            float nz = Math.max(1f, zoomMul / 1.5f);
            if (nz != zoomMul) {
                zoomMul = nz;
                if (zoomMul <= 1f) {
                    panNX = 0.5f;
                    panNY = 0.5f;
                }
                repaint();
            }
        }

        void nudge(int dx, int dy) {
            applyDelta(dx, dy);
            repaint();
        }

        private float currentScale() {
            int vw = getWidth();
            int vh = getHeight();
            if (vw <= 0 || vh <= 0 || imgW <= 0 || imgH <= 0) {
                return 0f;
            }
            float fit = Math.min(((float) vw) / imgW, ((float) vh) / imgH);
            return fit * zoomMul;
        }

        private int drawOriginX(float scale) {
            int vw = getWidth();
            int drawW = Math.max(1, Math.round(imgW * scale));
            if (drawW <= vw) {
                return getX() + (vw - drawW) / 2;
            }
            int raw = getX() + vw / 2 - Math.round(panNX * drawW);
            int min = getX() + vw - drawW;
            int max = getX();
            return Math.max(min, Math.min(max, raw));
        }

        private int drawOriginY(float scale) {
            int vh = getHeight();
            int drawH = Math.max(1, Math.round(imgH * scale));
            if (drawH <= vh) {
                return getY() + (vh - drawH) / 2;
            }
            int raw = getY() + vh / 2 - Math.round(panNY * drawH);
            int min = getY() + vh - drawH;
            int max = getY();
            return Math.max(min, Math.min(max, raw));
        }

        private int clamp(int v, int lo, int hi) {
            if (hi < lo) {
                hi = lo;
            }
            return Math.max(lo, Math.min(hi, v));
        }

        private void applyDelta(int dImgX, int dImgY) {
            if (dImgX == 0 && dImgY == 0) {
                return;
            }
            if (mode == AIM_MODE_PAN) {
                if (zoomMul > 1f) {
                    float scale = currentScale();
                    if (scale > 0f) {
                        float drawW = imgW * scale;
                        float drawH = imgH * scale;
                        float vw = getWidth();
                        float vh = getHeight();
                        if (drawW > vw) {
                            panNX = Math.max(0f, Math.min(1f, panNX - (dImgX * scale) / (drawW - vw)));
                        }
                        if (drawH > vh) {
                            panNY = Math.max(0f, Math.min(1f, panNY - (dImgY * scale) / (drawH - vh)));
                        }
                    }
                }
                return;
            }
            if (mode == AIM_MODE_SAFE) {
                if (!isSafeAreaEnabled()) {
                    return;
                }
                int sx = xField.getAsInt(0);
                int sy = yField.getAsInt(0);
                int saw = Math.min(safeWField.getAsInt(screenW), screenW);
                int sah = Math.min(safeHField.getAsInt(screenH), screenH);
                int curX = safeXField.getAsInt(sx);
                int curY = safeYField.getAsInt(sy);
                int newX = clamp(curX + dImgX, sx, sx + screenW - saw);
                int newY = clamp(curY + dImgY, sy, sy + screenH - sah);
                if (newX != curX) {
                    safeXField.setText("" + newX);
                }
                if (newY != curY) {
                    safeYField.setText("" + newY);
                }
                return;
            }
            int curX = xField.getAsInt(0);
            int curY = yField.getAsInt(0);
            int newX = clamp(curX + dImgX, 0, imgW - screenW);
            int newY = clamp(curY + dImgY, 0, imgH - screenH);
            int actualDx = newX - curX;
            int actualDy = newY - curY;
            if (actualDx != 0) {
                xField.setText("" + newX);
            }
            if (actualDy != 0) {
                yField.setText("" + newY);
            }
            if ((actualDx != 0 || actualDy != 0) && isSafeAreaEnabled()) {
                int saw = Math.min(safeWField.getAsInt(screenW), screenW);
                int sah = Math.min(safeHField.getAsInt(screenH), screenH);
                int curSafeX = safeXField.getAsInt(curX);
                int curSafeY = safeYField.getAsInt(curY);
                int newSafeX = clamp(curSafeX + actualDx, newX, newX + screenW - saw);
                int newSafeY = clamp(curSafeY + actualDy, newY, newY + screenH - sah);
                if (newSafeX != curSafeX) {
                    safeXField.setText("" + newSafeX);
                }
                if (newSafeY != curSafeY) {
                    safeYField.setText("" + newSafeY);
                }
            }
        }

        @Override
        public void pointerPressed(int x, int y) {
            lastPX = x;
            lastPY = y;
        }

        @Override
        public void pointerReleased(int x, int y) {
            lastPX = -1;
            lastPY = -1;
        }

        @Override
        public void pointerDragged(int x, int y) {
            if (lastPX < 0 || lastPY < 0) {
                lastPX = x;
                lastPY = y;
                return;
            }
            int dpx = x - lastPX;
            int dpy = y - lastPY;
            float scale = currentScale();
            if (scale <= 0f) {
                lastPX = x;
                lastPY = y;
                return;
            }
            int dImgX = Math.round(dpx / scale);
            int dImgY = Math.round(dpy / scale);
            if (dImgX == 0 && dImgY == 0) {
                return;
            }
            applyDelta(dImgX, dImgY);
            lastPX = x;
            lastPY = y;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            float scale = currentScale();
            if (scale <= 0f) {
                return;
            }
            int drawW = Math.max(1, Math.round(imgW * scale));
            int drawH = Math.max(1, Math.round(imgH * scale));
            int drawX = drawOriginX(scale);
            int drawY = drawOriginY(scale);

            int[] clip = g.getClip();
            g.pushClip();
            g.clipRect(getX(), getY(), getWidth(), getHeight());

            g.drawImage(img, drawX, drawY, drawW, drawH);

            int screenImgX = xField.getAsInt(0);
            int screenImgY = yField.getAsInt(0);
            boolean safeOn = isSafeAreaEnabled();
            int safeImgX = safeOn ? safeXField.getAsInt(screenImgX) : screenImgX;
            int safeImgY = safeOn ? safeYField.getAsInt(screenImgY) : screenImgY;
            int safeImgW = safeOn ? safeWField.getAsInt(screenW) : screenW;
            int safeImgH = safeOn ? safeHField.getAsInt(screenH) : screenH;

            int sx = drawX + Math.round(screenImgX * scale);
            int sy = drawY + Math.round(screenImgY * scale);
            int sw = Math.max(1, Math.round(screenW * scale));
            int sh = Math.max(1, Math.round(screenH * scale));
            int fx = drawX + Math.round(safeImgX * scale);
            int fy = drawY + Math.round(safeImgY * scale);
            int fw = Math.max(1, Math.round(safeImgW * scale));
            int fh = Math.max(1, Math.round(safeImgH * scale));

            int oldAlpha = g.getAlpha();
            int oldColor = g.getColor();

            g.setAlpha(150);
            g.setColor(0);
            g.fillRect(sx, sy, sw, sh);

            g.setAlpha(80);
            int checker = Math.max(4, Math.round(12f * scale));
            for (int yy = sy; yy < sy + sh; yy += checker) {
                for (int xx = sx; xx < sx + sw; xx += checker) {
                    boolean dark = (((xx - sx) / checker) + ((yy - sy) / checker)) % 2 == 0;
                    g.setColor(dark ? 0x999999 : 0xcccccc);
                    int cw = Math.min(checker, sx + sw - xx);
                    int ch = Math.min(checker, sy + sh - yy);
                    g.fillRect(xx, yy, cw, ch);
                }
            }

            if (safeOn) {
                g.setAlpha(150);
                g.setColor(0xff0000);
                int safeRight = fx + fw;
                int safeBottom = fy + fh;
                int screenRight = sx + sw;
                int screenBottom = sy + sh;
                if (fy > sy) {
                    g.fillRect(sx, sy, sw, Math.max(0, fy - sy));
                }
                if (fx > sx) {
                    g.fillRect(sx, fy, Math.max(0, fx - sx), Math.max(0, fh));
                }
                if (safeRight < screenRight) {
                    g.fillRect(safeRight, fy, Math.max(0, screenRight - safeRight), Math.max(0, fh));
                }
                if (safeBottom < screenBottom) {
                    g.fillRect(sx, safeBottom, sw, Math.max(0, screenBottom - safeBottom));
                }
            }

            g.setAlpha(255);
            g.setColor(mode == AIM_MODE_SCREEN ? 0x2f6bff : 0x00ffff);
            g.drawRect(sx, sy, sw, sh);
            if (safeOn) {
                g.setColor(mode == AIM_MODE_SAFE ? 0xffaa00 : 0xff0000);
                g.drawRect(fx, fy, fw, fh);
            }

            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
            g.popClip();
            g.setClip(clip);
        }
    }

    void aimPosition(final Image img, final TextField x, final TextField y, final TextField safeX, final TextField safeY, final TextField safeW, final TextField safeH, final int w, final int h, final OnOffSwitch useSafeArea) {
        if(img == null) {
            ToastBar.showErrorMessage("You need to pick a skin image first");
            return;
        }
        final String originalX = x.getText();
        final String originalY = y.getText();
        final String originalSafeX = safeX.getText();
        final String originalSafeY = safeY.getText();
        final Form editPosition = new Form("Positioning", new BorderLayout());
        editPosition.setUIID("SkinDesignerForm");

        final AimView view = new AimView(img, x, y, safeX, safeY, safeW, safeH, w, h, useSafeArea);

        Button done = new Button("Done");
        styleActionButton(done, FontImage.MATERIAL_CHECK);
        done.setTooltip("Keep changes and return");
        done.addActionListener(e -> editPosition.showBack());
        Button cancel = new Button("Cancel");
        styleActionButton(cancel, FontImage.MATERIAL_CANCEL);
        cancel.setTooltip("Discard changes and return");
        Runnable cancelAction = () -> {
            x.setText(originalX);
            y.setText(originalY);
            safeX.setText(originalSafeX);
            safeY.setText(originalSafeY);
            editPosition.showBack();
        };
        cancel.addActionListener(e -> cancelAction.run());
        editPosition.setBackCommand(new com.codename1.ui.Command("Cancel") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                cancelAction.run();
            }
        });

        Container topActions = GridLayout.encloseIn(2, cancel, done);
        topActions.setUIID("SkinDesignerTabBar");
        editPosition.add(BorderLayout.NORTH, topActions);

        final boolean safeEnabled = useSafeArea != null && useSafeArea.isValue();
        Button modeScreen = new Button("Screen");
        Button modeSafe = new Button("Safe");
        Button modePan = new Button("Pan");
        modeScreen.setTooltip("Drag to reposition the screen area");
        modeSafe.setTooltip(safeEnabled
                ? "Drag to adjust the safe area within the screen"
                : "Enable 'Use Safe Area' on the previous screen to edit safe area");
        modePan.setTooltip("Drag to scroll the zoomed view");
        if (!safeEnabled) {
            modeSafe.setEnabled(false);
        }
        final Button[] modeButtons = { modeScreen, modeSafe, modePan };
        final int[] modeValues = { AIM_MODE_SCREEN, AIM_MODE_SAFE, AIM_MODE_PAN };
        updateModeButtons(modeButtons, view.getMode());
        for (int i = 0; i < modeButtons.length; i++) {
            final int target = modeValues[i];
            modeButtons[i].addActionListener(e -> {
                view.setMode(target);
                updateModeButtons(modeButtons, target);
                if (target == AIM_MODE_PAN) {
                    ToastBar.showMessage("Pan mode: zoom in and drag to scroll the view",
                            FontImage.MATERIAL_PAN_TOOL, 2500);
                } else if (target == AIM_MODE_SAFE) {
                    ToastBar.showMessage("Safe mode: drag to adjust the safe area inside the screen",
                            FontImage.MATERIAL_CROP_FREE, 2500);
                } else if (target == AIM_MODE_SCREEN) {
                    ToastBar.showMessage("Screen mode: drag to reposition the screen area",
                            FontImage.MATERIAL_STAY_CURRENT_PORTRAIT, 2000);
                }
                view.repaint();
            });
        }
        Container modeBar = GridLayout.encloseIn(3, modeScreen, modeSafe, modePan);
        modeBar.setUIID("SkinDesignerTabBar");

        Button zoomIn = new Button();
        Button zoomOut = new Button();
        Button left = new Button();
        Button right = new Button();
        Button up = new Button();
        Button down = new Button();
        styleIconActionButton(zoomIn, FontImage.MATERIAL_ZOOM_IN);
        styleIconActionButton(zoomOut, FontImage.MATERIAL_ZOOM_OUT);
        styleIconActionButton(left, FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
        styleIconActionButton(right, FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT);
        styleIconActionButton(up, FontImage.MATERIAL_KEYBOARD_ARROW_UP);
        styleIconActionButton(down, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
        zoomIn.setTooltip("Zoom in");
        zoomOut.setTooltip("Zoom out");
        left.setTooltip("Nudge left (1 px)");
        right.setTooltip("Nudge right (1 px)");
        up.setTooltip("Nudge up (1 px)");
        down.setTooltip("Nudge down (1 px)");

        zoomIn.addActionListener(e -> view.zoomIn());
        zoomOut.addActionListener(e -> view.zoomOut());
        left.addActionListener(e -> view.nudge(-1, 0));
        right.addActionListener(e -> view.nudge(1, 0));
        up.addActionListener(e -> view.nudge(0, -1));
        down.addActionListener(e -> view.nudge(0, 1));

        Container navBar = GridLayout.encloseIn(6, zoomIn, zoomOut, left, right, up, down);
        Container south = BoxLayout.encloseY(modeBar, navBar);
        editPosition.add(BorderLayout.CENTER, view);
        editPosition.add(BorderLayout.SOUTH, south);
        applyWebsiteTheme(editPosition, websiteDarkMode);
        editPosition.show();
    }

    private void updateModeButtons(Button[] buttons, int selectedMode) {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = i == selectedMode;
            String base = selected ? "SkinDesignerTabButtonSelected" : "SkinDesignerTabButton";
            buttons[i].setUIID(websiteDarkMode ? base + "Dark" : base);
        }
    }

    private byte[] imageToByteArray(Image img) throws IOException {
        ByteArrayOutputStream bo  = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(img, bo, ImageIO.FORMAT_PNG, 1);
        bo.close();
        return bo.toByteArray();
    }

    byte[] createSkinFile(ImageSettings imPortrait, ImageSettings imLandscape, Picker nativeTheme, Picker platformName, OnOffSwitch tablet, TextField systemFontFamily, TextField proportionalFontFamily, TextField monospaceFontFamily, TextField smallFontSize, TextField mediumFontSize, TextField largeFontSize, TextField pixelRatio, Picker overrideNamePrimary, Picker overrideNameSecondary, Picker overrideNameLast) {
        Image portrait = imPortrait.getSkinImage();
        Image landscape = imLandscape.getSkinImage();
        if (portrait == null) {
            ToastBar.showErrorMessage("Missing portrait skin image");
            return null;
        }
        if (landscape == null) {
            ToastBar.showErrorMessage("Missing landscape skin image");
            return null;
        }
        Image overlayPortrait = imPortrait.createSkinOverlay();
        Image overlayLandscape = imLandscape.createSkinOverlay();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try(ZipOutputStream zos = new ZipOutputStream(bos)) {
            ZipEntry ze = new ZipEntry("skin.png");
            zos.putNextEntry(ze);
            zos.write(imageToByteArray(portrait));

            ze = new ZipEntry("skin_l.png");
            zos.putNextEntry(ze);
            zos.write(imageToByteArray(landscape));

            ze = new ZipEntry("skin_map.png");
            zos.putNextEntry(ze);
            zos.write(imageToByteArray(overlayPortrait));

            ze = new ZipEntry("skin_map_l.png");
            zos.putNextEntry(ze);
            zos.write(imageToByteArray(overlayLandscape));

            String theme = nativeTheme.getSelectedString();
            for(int iter = 0 ; iter < NATIVE_THEMES.length ; iter++) {
                if(NATIVE_THEMES[iter].equals(theme)) {
                    ze = new ZipEntry(NATIVE_THEME_FILES[iter]);
                    zos.putNextEntry(ze);
                    InputStream is = Display.getInstance().getResourceAsStream(getClass(), "/" + NATIVE_THEME_FILES[iter]);
                    Util.copyNoClose(is, zos, 8192);
                    break;
                }
            }

            Properties props = new Properties();
            props.put("touch", "true");
            props.put("platformName", platformName.getSelectedString());
            props.put("tablet", "" + tablet.isValue());
            props.put("systemFontFamily", systemFontFamily.getText());
            props.put("proportionalFontFamily", proportionalFontFamily.getText());
            props.put("monospaceFontFamily", monospaceFontFamily.getText());
            props.put("smallFontSize", smallFontSize.getText());
            props.put("mediumFontSize", mediumFontSize.getText());
            props.put("largeFontSize", largeFontSize.getText());
            props.put("pixelRatio", pixelRatio.getText());
            props.put("overrideNames", overrideNamePrimary.getSelectedString() + "," +
                    overrideNameSecondary.getSelectedString() + "," +
                    overrideNameLast.getSelectedString());
            props.put("safePortraitX", "" + imPortrait.getSafeX());
            props.put("safePortraitY", "" + imPortrait.getSafeY());
            props.put("safePortraitWidth", "" + imPortrait.getSafeWidth());
            props.put("safePortraitHeight", "" + imPortrait.getSafeHeight());
            props.put("safeLandscapeX", "" + imLandscape.getSafeX());
            props.put("safeLandscapeY", "" + imLandscape.getSafeY());
            props.put("safeLandscapeWidth", "" + imLandscape.getSafeWidth());
            props.put("safeLandscapeHeight", "" + imLandscape.getSafeHeight());

            ze = new ZipEntry("skin.properties");
            zos.putNextEntry(ze);
            props.store(zos, "Created by the Codename One skin designer see https://www.codenameone.com/");
        } catch(IOException err) {
            Log.e(err);
            ToastBar.showErrorMessage("Error while saving file: " + err);
        }
        return bos.toByteArray();
    }
}
