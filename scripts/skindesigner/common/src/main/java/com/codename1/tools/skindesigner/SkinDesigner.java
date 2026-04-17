package com.codename1.tools.skindesigner;

import com.codename1.system.Lifecycle;
import com.codename1.components.ImageViewer;
import com.codename1.components.OnOffSwitch;
import com.codename1.components.ScaleImageLabel;
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
import com.codename1.ui.layouts.LayeredLayout;
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
    private static final String[] NATIVE_THEMES = {"iOS 7+", "iOS 6", "Android 4 +","Android 2.x", "Windows"};
    private static final String[] NATIVE_THEME_FILES = {"iOS7Theme.res", "iPhoneTheme.res",
            "android_holo_light.res","androidTheme.res", "winTheme.res"};
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
        final TextField floodTolerance = new TextField("24", "Tolerance", 4, TextField.NUMERIC);
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

        Button aim = new Button();
        styleIconActionButton(aim, FontImage.MATERIAL_PAN_TOOL);

        aim.addActionListener(e ->
                aimPosition(sl.getIcon(),
                        screenPositionX,
                        screenPositionY,
                        safeX,
                        safeY,
                        safeWidth,
                        safeHeight,
                        screenWidthPixels.getAsInt(768),
                        screenHeightPixels.getAsInt(1024)));

        Button helpButton = new Button();
        styleIconActionButton(helpButton, FontImage.MATERIAL_HELP);
        helpButton.addActionListener(e -> helpCallback.run());

        Button saveButton = new Button();
        styleIconActionButton(saveButton, FontImage.MATERIAL_SAVE);
        saveButton.addActionListener(e -> saveCallback.run());

        ScaleImageLabel maskLabel = new ScaleImageLabel();
        Button detectScreenButton = new Button("Detect Screen by Color");
        detectScreenButton.setUIID("SkinDesignerActionButton");
        detectScreenButton.addActionListener(e -> {
            Image source = sl.getIcon();
            if(source == null) {
                ToastBar.showErrorMessage("Please select a skin image first");
                return;
            }
            Image generatedMask = createFloodFillMask(source,
                    screenPositionX.getAsInt(0),
                    screenPositionY.getAsInt(0),
                    floodTolerance.getAsInt(24));
            if(generatedMask != null) {
                maskLabel.setIcon(generatedMask);
                maskLabel.getParent().revalidate();
                try(OutputStream os = Storage.getInstance().createOutputStream(prefix + ".mask.png")) {
                    ImageIO.getImageIO().save(generatedMask, os, ImageIO.FORMAT_PNG, 1);
                } catch(IOException err) {
                    Log.e(err);
                    ToastBar.showErrorMessage("Error saving generated mask: " + err.getMessage());
                }
            }
        });

        Button maskPicker = new Button("Select Screen Mask (Optional)");
        maskPicker.setUIID("SkinDesignerActionButton");
        maskPicker.addActionListener((e) -> {
            Display.getInstance().openGallery((ee) -> {
                if(ee != null && ee.getSource() != null) {
                    try {
                        String fileName = (String)ee.getSource();
                        Image mask = Image.createImage(fileName);
                        maskLabel.setIcon(mask);
                        maskLabel.getParent().revalidate();
                        Util.copy(FileSystemStorage.getInstance().openInputStream(fileName),
                                Storage.getInstance().createOutputStream(prefix + ".mask.png"));
                    } catch(IOException err) {
                        ToastBar.showErrorMessage("Error Loading Mask: " + err);
                    }
                }
            }, Display.GALLERY_IMAGE);
        });
        if(Storage.getInstance().exists(prefix + ".mask.png")) {
            try(InputStream is = Storage.getInstance().createInputStream(prefix + ".mask.png")) {
                maskLabel.setIcon(Image.createImage(is));
            } catch(IOException err) {
                Log.e(err);
            }
        }

        final Container cnt = BoxLayout.encloseY(imagePicker,
                BorderLayout.center(labeledFieldTitle("Screen Position (X/Y/Width/Height)")).
                        add(BorderLayout.EAST, BoxLayout.encloseX(aim, helpButton, saveButton)),
                GridLayout.encloseIn(4, screenPositionX, screenPositionY, screenWidthPixels, screenHeightPixels),
                labeledFieldTitle("Safe Area (X/Y/Width/Height)"),
                GridLayout.encloseIn(4, safeX, safeY, safeWidth, safeHeight),
                BorderLayout.center(detectScreenButton).add(BorderLayout.EAST, floodTolerance),
                maskPicker,
                maskLabel,
                sl);
        cnt.setUIID("SkinDesignerCard");
        cnt.setScrollableY(true);
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
                if(mask != null && mask.getWidth() == width && mask.getHeight() == height) {
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
                if(mask != null && mask.getWidth() == skinImage.getWidth() && mask.getHeight() == skinImage.getHeight()) {
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
                return safeX.getAsInt(getScreenX());
            }

            @Override
            public int getSafeY() {
                return safeY.getAsInt(getScreenY());
            }

            @Override
            public int getSafeWidth() {
                return safeWidth.getAsInt(getScreenWidth());
            }

            @Override
            public int getSafeHeight() {
                return safeHeight.getAsInt(getScreenHeight());
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
            if(colorDistance(src[idx], seedColor) <= tolerance) {
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

    private int colorDistance(int c1, int c2) {
        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = c1 & 0xff;
        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = c2 & 0xff;
        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;
        return (int)Math.sqrt(dr * dr + dg * dg + db * db);
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

    private Image createMute(int x, int y, int w, int h, int safeX, int safeY, int safeW, int safeH, Image img) {
        Image mute = Image.createImage(img.getWidth(), img.getHeight(), 0);
        Graphics g = mute.getGraphics();
        g.setAlpha(150);
        g.setColor(0);
        g.fillRect(x, y, w, h);
        g.setAlpha(80);
        int checker = 12;
        for(int yy = y; yy < y + h; yy += checker) {
            for(int xx = x; xx < x + w; xx += checker) {
                boolean dark = (((xx - x) / checker) + ((yy - y) / checker)) % 2 == 0;
                g.setColor(dark ? 0x999999 : 0xcccccc);
                g.fillRect(xx, yy, Math.min(checker, x + w - xx), Math.min(checker, y + h - yy));
            }
        }
        g.setColor(0xff0000);
        int safeRight = safeX + safeW;
        int safeBottom = safeY + safeH;
        int screenRight = x + w;
        int screenBottom = y + h;
        if(safeY > y) {
            g.fillRect(x, y, w, Math.max(0, safeY - y));
        }
        if(safeX > x) {
            g.fillRect(x, safeY, Math.max(0, safeX - x), Math.max(0, safeH));
        }
        if(safeRight < screenRight) {
            g.fillRect(safeRight, safeY, Math.max(0, screenRight - safeRight), Math.max(0, safeH));
        }
        if(safeBottom < screenBottom) {
            g.fillRect(x, safeBottom, w, Math.max(0, screenBottom - safeBottom));
        }
        g.setColor(0xff);
        g.drawRect(x, y, w, h);
        g.setColor(0xff0000);
        g.drawRect(safeX, safeY, safeW, safeH);
        g.setAlpha(255);
        return mute;
    }

    void aimPosition(final Image img, final TextField x, final TextField y, final TextField safeX, final TextField safeY, final TextField safeW, final TextField safeH, final int w, final int h) {
        if(img == null) {
            ToastBar.showErrorMessage("You need to pick a skin image first");
            return;
        }
        String originalX = x.getText();
        String originalY = y.getText();
        Form editPosition = new Form("", new BorderLayout());
        editPosition.setUIID("SkinDesignerForm");
        Button done = new Button("Done");
        styleActionButton(done, FontImage.MATERIAL_CHECK);
        done.addActionListener(e -> x.getComponentForm().showBack());
        Button cancel = new Button("Cancel");
        styleActionButton(cancel, FontImage.MATERIAL_CANCEL);
        cancel.addActionListener(e -> {
            x.setText(originalX);
            y.setText(originalY);
            x.getComponentForm().showBack();
        });
        Container topActions = GridLayout.encloseIn(2, cancel, done);
        topActions.setUIID("SkinDesignerTabBar");
        editPosition.add(BorderLayout.NORTH, topActions);

        Image mute = createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img);
        class oo extends ImageViewer {
            private int lastDragX = -1;
            private int lastDragY = -1;

            public oo(Image img) {
                super(img);
            }
            @Override
            public boolean pinch(float scale) {
                return super.pinch(scale);
            }

            @Override
            public void pointerPressed(int xPos, int yPos) {
                super.pointerPressed(xPos, yPos);
                lastDragX = xPos;
                lastDragY = yPos;
            }

            @Override
            public void pointerDragged(int xPos, int yPos) {
                if(lastDragX < 0 || lastDragY < 0) {
                    lastDragX = xPos;
                    lastDragY = yPos;
                    return;
                }
                int dx = Math.round((xPos - lastDragX) / getZoom());
                int dy = Math.round((yPos - lastDragY) / getZoom());
                if(dx != 0 || dy != 0) {
                    int maxX = Math.max(0, img.getWidth() - w);
                    int maxY = Math.max(0, img.getHeight() - h);
                    int newX = Math.min(maxX, Math.max(0, x.getAsInt(0) + dx));
                    int newY = Math.min(maxY, Math.max(0, y.getAsInt(0) + dy));
                    x.setText("" + newX);
                    y.setText("" + newY);
                    setImageNoReposition(createMute(newX, newY, w, h, safeX.getAsInt(newX), safeY.getAsInt(newY), safeW.getAsInt(w), safeH.getAsInt(h), img));
                    lastDragX = xPos;
                    lastDragY = yPos;
                }
            }

            @Override
            public void pointerReleased(int xPos, int yPos) {
                super.pointerReleased(xPos, yPos);
                lastDragX = -1;
                lastDragY = -1;
            }
        }
        final oo overlay = new oo(mute);
        ImageViewer iv = new ImageViewer(img) {
            @Override
            public void pointerDragged(int x, int y) {
                super.pointerDragged(x, y);
                overlay.pointerDragged(x, y);
            }

            @Override
            protected boolean pinch(float scale) {
                boolean b = super.pinch(scale);
                overlay.pinch(scale);
                return b;
            }

            @Override
            public void pointerPressed(int x, int y) {
                super.pointerPressed(x, y);
                overlay.pointerPressed(x, y);
            }

            @Override
            public void pointerReleased(int x, int y) {
                super.pointerReleased(x, y);
                overlay.pointerReleased(x, y);
            }

            @Override
            public void keyReleased(int key) {
                super.keyReleased(key);
                overlay.keyReleased(key);
            }
        };
        overlay.setFocusable(false);

        Button zoomIn = new Button();
        Button zoomOut = new Button();
        Button left = new Button();
        Button right = new Button();
        Button up = new Button();
        Button down = new Button();
        FontImage.setMaterialIcon(zoomIn, FontImage.MATERIAL_ZOOM_IN, 3);
        FontImage.setMaterialIcon(zoomOut, FontImage.MATERIAL_ZOOM_OUT, 3);
        FontImage.setMaterialIcon(left, FontImage.MATERIAL_KEYBOARD_ARROW_LEFT, 3);
        FontImage.setMaterialIcon(right, FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, 3);
        FontImage.setMaterialIcon(up, FontImage.MATERIAL_KEYBOARD_ARROW_UP, 3);
        FontImage.setMaterialIcon(down, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN, 3);

        zoomIn.addActionListener(e -> {
            iv.setZoom(iv.getZoom() + 1);
            overlay.setZoom(iv.getZoom());
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        zoomOut.addActionListener(e -> {
            iv.setZoom(iv.getZoom() - 1);
            overlay.setZoom(iv.getZoom());
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        left.addActionListener(e -> {
            int newX = x.getAsInt(0) - 1;
            x.setText("" + newX);
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        right.addActionListener(e -> {
            int newX = x.getAsInt(0) + 1;
            x.setText("" + newX);
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        up.addActionListener(e -> {
            int newY = y.getAsInt(0) - 1;
            y.setText("" + newY);
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        down.addActionListener(e -> {
            int newY = y.getAsInt(0) + 1;
            y.setText("" + newY);
            overlay.setImageNoReposition(createMute(x.getAsInt(0), y.getAsInt(0), w, h, safeX.getAsInt(x.getAsInt(0)), safeY.getAsInt(y.getAsInt(0)), safeW.getAsInt(w), safeH.getAsInt(h), img));
        });

        editPosition.add(BorderLayout.CENTER, LayeredLayout.encloseIn(iv, overlay,
                BorderLayout.south(GridLayout.encloseIn(6, zoomIn, zoomOut, left, right, up, down))));
        applyWebsiteTheme(editPosition, websiteDarkMode);

        editPosition.show();
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
