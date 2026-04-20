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
                        screenWidthPixels,
                        screenHeightPixels,
                        safeX,
                        safeY,
                        safeWidth,
                        safeHeight,
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

    static final int DRAG_NONE = 0;
    static final int DRAG_MOVE = 1;
    static final int DRAG_N = 1 << 1;
    static final int DRAG_S = 1 << 2;
    static final int DRAG_W = 1 << 3;
    static final int DRAG_E = 1 << 4;
    static final int MIN_SAFE_SIZE = 10;

    final class AimView extends Component {
        private final Image img;
        private final int imgW;
        private final int imgH;
        private final TextField xField;
        private final TextField yField;
        private final TextField screenWField;
        private final TextField screenHField;
        private final TextField safeXField;
        private final TextField safeYField;
        private final TextField safeWField;
        private final TextField safeHField;
        private final OnOffSwitch useSafeArea;
        private Runnable onChange;
        private int mode = AIM_MODE_SCREEN;
        private float zoomMul = 1f;
        private float panNX = 0.5f;
        private float panNY = 0.5f;
        private int lastPX = -1;
        private int lastPY = -1;
        private int dragOp = DRAG_NONE;

        AimView(Image img, TextField xField, TextField yField,
                TextField screenWField, TextField screenHField,
                TextField safeXField, TextField safeYField,
                TextField safeWField, TextField safeHField,
                OnOffSwitch useSafeArea) {
            this.img = img;
            this.imgW = img.getWidth();
            this.imgH = img.getHeight();
            this.xField = xField;
            this.yField = yField;
            this.screenWField = screenWField;
            this.screenHField = screenHField;
            this.safeXField = safeXField;
            this.safeYField = safeYField;
            this.safeWField = safeWField;
            this.safeHField = safeHField;
            this.useSafeArea = useSafeArea;
            setUIID("SkinDesignerCard");
            setFocusable(true);
        }

        void setOnChange(Runnable onChange) {
            this.onChange = onChange;
        }

        private void notifyChanged() {
            if (onChange != null) {
                onChange.run();
            }
        }

        int screenW() {
            return Math.max(1, screenWField.getAsInt(1));
        }

        int screenH() {
            return Math.max(1, screenHField.getAsInt(1));
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
                centerOnActiveRect();
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
                } else {
                    centerOnActiveRect();
                }
                repaint();
            }
        }

        private void centerOnActiveRect() {
            int cx, cy;
            if (mode == AIM_MODE_SAFE && isSafeAreaEnabled()) {
                cx = safeXField.getAsInt(0) + safeWField.getAsInt(screenW()) / 2;
                cy = safeYField.getAsInt(0) + safeHField.getAsInt(screenH()) / 2;
            } else {
                cx = xField.getAsInt(0) + screenW() / 2;
                cy = yField.getAsInt(0) + screenH() / 2;
            }
            if (imgW > 0) {
                panNX = Math.max(0f, Math.min(1f, ((float) cx) / imgW));
            }
            if (imgH > 0) {
                panNY = Math.max(0f, Math.min(1f, ((float) cy) / imgH));
            }
        }

        void nudge(int dx, int dy) {
            int savedOp = dragOp;
            dragOp = DRAG_MOVE;
            applyDelta(dx, dy);
            dragOp = savedOp;
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
            if (dImgX == 0 && dImgY == 0 || dragOp == DRAG_NONE) {
                return;
            }
            int sW = screenW();
            int sH = screenH();
            if (mode == AIM_MODE_SAFE) {
                if (!isSafeAreaEnabled()) {
                    return;
                }
                int boundsX = xField.getAsInt(0);
                int boundsY = yField.getAsInt(0);
                int boundsW = sW;
                int boundsH = sH;
                int curX = safeXField.getAsInt(boundsX);
                int curY = safeYField.getAsInt(boundsY);
                int curW = Math.min(safeWField.getAsInt(boundsW), boundsW);
                int curH = Math.min(safeHField.getAsInt(boundsH), boundsH);
                int[] rect = applyResize(curX, curY, curW, curH, dImgX, dImgY,
                        boundsX, boundsY, boundsW, boundsH);
                boolean changed = false;
                if (rect[0] != curX) { safeXField.setText("" + rect[0]); changed = true; }
                if (rect[1] != curY) { safeYField.setText("" + rect[1]); changed = true; }
                if (rect[2] != curW) { safeWField.setText("" + rect[2]); changed = true; }
                if (rect[3] != curH) { safeHField.setText("" + rect[3]); changed = true; }
                if (changed) { notifyChanged(); }
                return;
            }
            int curX = xField.getAsInt(0);
            int curY = yField.getAsInt(0);
            int newX = clamp(curX + dImgX, 0, imgW - sW);
            int newY = clamp(curY + dImgY, 0, imgH - sH);
            int actualDx = newX - curX;
            int actualDy = newY - curY;
            boolean changed = false;
            if (actualDx != 0) { xField.setText("" + newX); changed = true; }
            if (actualDy != 0) { yField.setText("" + newY); changed = true; }
            if ((actualDx != 0 || actualDy != 0) && isSafeAreaEnabled()) {
                int saw = Math.min(safeWField.getAsInt(sW), sW);
                int sah = Math.min(safeHField.getAsInt(sH), sH);
                int curSafeX = safeXField.getAsInt(curX);
                int curSafeY = safeYField.getAsInt(curY);
                int newSafeX = clamp(curSafeX + actualDx, newX, newX + sW - saw);
                int newSafeY = clamp(curSafeY + actualDy, newY, newY + sH - sah);
                if (newSafeX != curSafeX) { safeXField.setText("" + newSafeX); changed = true; }
                if (newSafeY != curSafeY) { safeYField.setText("" + newSafeY); changed = true; }
            }
            if (changed) { notifyChanged(); }
        }

        private int[] applyResize(int curX, int curY, int curW, int curH, int dx, int dy,
                                  int boundsX, int boundsY, int boundsW, int boundsH) {
            boolean moveLeft = (dragOp & DRAG_W) != 0 || dragOp == DRAG_MOVE;
            boolean moveRight = (dragOp & DRAG_E) != 0 || dragOp == DRAG_MOVE;
            boolean moveTop = (dragOp & DRAG_N) != 0 || dragOp == DRAG_MOVE;
            boolean moveBottom = (dragOp & DRAG_S) != 0 || dragOp == DRAG_MOVE;
            int left = curX;
            int right = curX + curW;
            int top = curY;
            int bottom = curY + curH;
            if (moveLeft) { left += dx; }
            if (moveRight) { right += dx; }
            if (moveTop) { top += dy; }
            if (moveBottom) { bottom += dy; }
            int minX = boundsX;
            int minY = boundsY;
            int maxX = boundsX + boundsW;
            int maxY = boundsY + boundsH;
            if (left < minX) { left = minX; if (moveLeft && !moveRight && right < left + MIN_SAFE_SIZE) right = left + MIN_SAFE_SIZE; }
            if (right > maxX) { right = maxX; if (moveRight && !moveLeft && left > right - MIN_SAFE_SIZE) left = right - MIN_SAFE_SIZE; }
            if (top < minY) { top = minY; if (moveTop && !moveBottom && bottom < top + MIN_SAFE_SIZE) bottom = top + MIN_SAFE_SIZE; }
            if (bottom > maxY) { bottom = maxY; if (moveBottom && !moveTop && top > bottom - MIN_SAFE_SIZE) top = bottom - MIN_SAFE_SIZE; }
            if (right - left < MIN_SAFE_SIZE) {
                if (moveLeft && !moveRight) { left = right - MIN_SAFE_SIZE; }
                else if (moveRight && !moveLeft) { right = left + MIN_SAFE_SIZE; }
            }
            if (bottom - top < MIN_SAFE_SIZE) {
                if (moveTop && !moveBottom) { top = bottom - MIN_SAFE_SIZE; }
                else if (moveBottom && !moveTop) { bottom = top + MIN_SAFE_SIZE; }
            }
            if (dragOp == DRAG_MOVE) {
                int w = right - left;
                int h = bottom - top;
                left = clamp(left, minX, maxX - w);
                top = clamp(top, minY, maxY - h);
                right = left + w;
                bottom = top + h;
            }
            return new int[] { left, top, right - left, bottom - top };
        }

        @Override
        public void pointerPressed(int x, int y) {
            lastPX = x;
            lastPY = y;
            dragOp = hitTestDragOp(x, y);
        }

        @Override
        public void pointerReleased(int x, int y) {
            lastPX = -1;
            lastPY = -1;
            dragOp = DRAG_NONE;
        }

        private int hitTestDragOp(int px, int py) {
            if (mode == AIM_MODE_SAFE && isSafeAreaEnabled()) {
                float scale = currentScale();
                if (scale <= 0f) { return DRAG_MOVE; }
                int drawX = drawOriginX(scale);
                int drawY = drawOriginY(scale);
                int sX = drawX + Math.round(safeXField.getAsInt(0) * scale);
                int sY = drawY + Math.round(safeYField.getAsInt(0) * scale);
                int sW = Math.max(1, Math.round(safeWField.getAsInt(screenW()) * scale));
                int sH = Math.max(1, Math.round(safeHField.getAsInt(screenH()) * scale));
                int zone = Math.max(8, Display.getInstance().convertToPixels(2.5f));
                boolean nearLeft = px >= sX - zone && px <= sX + zone;
                boolean nearRight = px >= sX + sW - zone && px <= sX + sW + zone;
                boolean nearTop = py >= sY - zone && py <= sY + zone;
                boolean nearBottom = py >= sY + sH - zone && py <= sY + sH + zone;
                int op = 0;
                if (nearLeft) { op |= DRAG_W; }
                if (nearRight) { op |= DRAG_E; }
                if (nearTop) { op |= DRAG_N; }
                if (nearBottom) { op |= DRAG_S; }
                if (op != 0) { return op; }
            }
            return DRAG_MOVE;
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

            int sW = screenW();
            int sH = screenH();
            int screenImgX = xField.getAsInt(0);
            int screenImgY = yField.getAsInt(0);
            boolean safeOn = isSafeAreaEnabled();
            int safeImgX = safeOn ? safeXField.getAsInt(screenImgX) : screenImgX;
            int safeImgY = safeOn ? safeYField.getAsInt(screenImgY) : screenImgY;
            int safeImgW = safeOn ? safeWField.getAsInt(sW) : sW;
            int safeImgH = safeOn ? safeHField.getAsInt(sH) : sH;

            int sx = drawX + Math.round(screenImgX * scale);
            int sy = drawY + Math.round(screenImgY * scale);
            int sw = Math.max(1, Math.round(sW * scale));
            int sh = Math.max(1, Math.round(sH * scale));
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
                if (mode == AIM_MODE_SAFE) {
                    int handle = Math.max(6, Display.getInstance().convertToPixels(2f));
                    int half = handle / 2;
                    g.fillRect(fx - half, fy - half, handle, handle);
                    g.fillRect(fx + fw - half, fy - half, handle, handle);
                    g.fillRect(fx - half, fy + fh - half, handle, handle);
                    g.fillRect(fx + fw - half, fy + fh - half, handle, handle);
                    g.fillRect(fx + fw / 2 - half, fy - half, handle, handle);
                    g.fillRect(fx + fw / 2 - half, fy + fh - half, handle, handle);
                    g.fillRect(fx - half, fy + fh / 2 - half, handle, handle);
                    g.fillRect(fx + fw - half, fy + fh / 2 - half, handle, handle);
                }
            }

            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
            g.popClip();
            g.setClip(clip);
        }
    }

    void aimPosition(final Image img,
                    final TextField screenX, final TextField screenY,
                    final TextField screenW, final TextField screenH,
                    final TextField safeX, final TextField safeY,
                    final TextField safeW, final TextField safeH,
                    final OnOffSwitch useSafeArea) {
        if(img == null) {
            ToastBar.showErrorMessage("You need to pick a skin image first");
            return;
        }
        final Form previousForm = Display.getInstance().getCurrent();
        final boolean safeEnabled = useSafeArea != null && useSafeArea.isValue();
        final Form editPosition = new Form("Positioning", new BorderLayout());
        editPosition.setUIID("SkinDesignerForm");

        final AimView view = new AimView(img, screenX, screenY, screenW, screenH,
                safeX, safeY, safeW, safeH, useSafeArea);

        final TextField aimScreenX = numericMirror(screenX);
        final TextField aimScreenY = numericMirror(screenY);
        final TextField aimScreenW = numericMirror(screenW);
        final TextField aimScreenH = numericMirror(screenH);
        final TextField aimSafeX = numericMirror(safeX);
        final TextField aimSafeY = numericMirror(safeY);
        final TextField aimSafeW = numericMirror(safeW);
        final TextField aimSafeH = numericMirror(safeH);
        if (!safeEnabled) {
            aimSafeX.setEnabled(false);
            aimSafeY.setEnabled(false);
            aimSafeW.setEnabled(false);
            aimSafeH.setEnabled(false);
        }
        Runnable refreshAimFields = () -> {
            syncMirror(aimScreenX, screenX);
            syncMirror(aimScreenY, screenY);
            syncMirror(aimScreenW, screenW);
            syncMirror(aimScreenH, screenH);
            syncMirror(aimSafeX, safeX);
            syncMirror(aimSafeY, safeY);
            syncMirror(aimSafeW, safeW);
            syncMirror(aimSafeH, safeH);
        };
        view.setOnChange(refreshAimFields);
        bindAimToMain(aimScreenX, screenX, view);
        bindAimToMain(aimScreenY, screenY, view);
        bindAimToMain(aimScreenW, screenW, view);
        bindAimToMain(aimScreenH, screenH, view);
        bindAimToMain(aimSafeX, safeX, view);
        bindAimToMain(aimSafeY, safeY, view);
        bindAimToMain(aimSafeW, safeW, view);
        bindAimToMain(aimSafeH, safeH, view);

        final String originalScreenX = screenX.getText();
        final String originalScreenY = screenY.getText();
        final String originalScreenW = screenW.getText();
        final String originalScreenH = screenH.getText();
        final String originalSafeX = safeX.getText();
        final String originalSafeY = safeY.getText();
        final String originalSafeW = safeW.getText();
        final String originalSafeH = safeH.getText();

        Button done = new Button("Done");
        styleActionButton(done, FontImage.MATERIAL_CHECK);
        done.setTooltip("Keep changes and return");
        done.addActionListener(e -> previousForm.showBack());
        Button cancel = new Button("Cancel");
        styleActionButton(cancel, FontImage.MATERIAL_CANCEL);
        cancel.setTooltip("Discard changes and return");
        Runnable cancelAction = () -> {
            screenX.setText(originalScreenX);
            screenY.setText(originalScreenY);
            screenW.setText(originalScreenW);
            screenH.setText(originalScreenH);
            safeX.setText(originalSafeX);
            safeY.setText(originalSafeY);
            safeW.setText(originalSafeW);
            safeH.setText(originalSafeH);
            previousForm.showBack();
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

        Container south = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        if (safeEnabled) {
            Button modeScreen = new Button("Screen");
            Button modeSafe = new Button("Safe");
            modeScreen.setTooltip("Drag to reposition the screen area");
            modeSafe.setTooltip("Drag inside to move the safe area; drag its edges or corners to resize");
            final Button[] modeButtons = { modeScreen, modeSafe };
            final int[] modeValues = { AIM_MODE_SCREEN, AIM_MODE_SAFE };
            updateModeButtons(modeButtons, view.getMode());
            for (int i = 0; i < modeButtons.length; i++) {
                final int target = modeValues[i];
                modeButtons[i].addActionListener(e -> {
                    view.setMode(target);
                    updateModeButtons(modeButtons, target);
                    view.repaint();
                });
            }
            Container modeBar = GridLayout.encloseIn(2, modeScreen, modeSafe);
            modeBar.setUIID("SkinDesignerTabBar");
            south.add(modeBar);
        }

        south.add(labeledFieldTitle("Screen (X / Y / W / H)"));
        south.add(GridLayout.encloseIn(4, aimScreenX, aimScreenY, aimScreenW, aimScreenH));
        if (safeEnabled) {
            south.add(labeledFieldTitle("Safe (X / Y / W / H)"));
            south.add(GridLayout.encloseIn(4, aimSafeX, aimSafeY, aimSafeW, aimSafeH));
        }

        Button zoomIn = new Button();
        Button zoomOut = new Button();
        Button leftBtn = new Button();
        Button rightBtn = new Button();
        Button upBtn = new Button();
        Button downBtn = new Button();
        styleIconActionButton(zoomIn, FontImage.MATERIAL_ZOOM_IN);
        styleIconActionButton(zoomOut, FontImage.MATERIAL_ZOOM_OUT);
        styleIconActionButton(leftBtn, FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
        styleIconActionButton(rightBtn, FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT);
        styleIconActionButton(upBtn, FontImage.MATERIAL_KEYBOARD_ARROW_UP);
        styleIconActionButton(downBtn, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
        zoomIn.setTooltip("Zoom in (centers on the active rectangle)");
        zoomOut.setTooltip("Zoom out");
        leftBtn.setTooltip("Nudge the active rectangle 1 px left");
        rightBtn.setTooltip("Nudge the active rectangle 1 px right");
        upBtn.setTooltip("Nudge the active rectangle 1 px up");
        downBtn.setTooltip("Nudge the active rectangle 1 px down");

        zoomIn.addActionListener(e -> view.zoomIn());
        zoomOut.addActionListener(e -> view.zoomOut());
        leftBtn.addActionListener(e -> { view.nudge(-1, 0); refreshAimFields.run(); });
        rightBtn.addActionListener(e -> { view.nudge(1, 0); refreshAimFields.run(); });
        upBtn.addActionListener(e -> { view.nudge(0, -1); refreshAimFields.run(); });
        downBtn.addActionListener(e -> { view.nudge(0, 1); refreshAimFields.run(); });

        Container navBar = GridLayout.encloseIn(6, zoomOut, zoomIn, leftBtn, rightBtn, upBtn, downBtn);
        south.add(navBar);

        editPosition.add(BorderLayout.CENTER, view);
        editPosition.add(BorderLayout.SOUTH, south);
        applyWebsiteTheme(editPosition, websiteDarkMode);
        editPosition.show();

        String intro = safeEnabled
                ? "Drag the screen or safe rectangle to move it. Drag safe-area edges or corners to resize. Use the fields below for exact values."
                : "Drag the screen rectangle to move it. Use the X / Y / W / H fields below for exact values.";
        ToastBar.showMessage(intro, FontImage.MATERIAL_INFO, 6000);
    }

    private TextField numericMirror(TextField source) {
        TextField t = new TextField(source.getText(), source.getHint(), 6, TextField.NUMERIC);
        t.setUIID("SkinDesignerField");
        return t;
    }

    private void syncMirror(TextField mirror, TextField source) {
        if (!source.getText().equals(mirror.getText())) {
            mirror.setText(source.getText());
        }
    }

    private void bindAimToMain(final TextField mirror, final TextField main, final AimView view) {
        mirror.addActionListener(e -> {
            String text = mirror.getText();
            if (!main.getText().equals(text)) {
                main.setText(text);
            }
            view.repaint();
        });
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
