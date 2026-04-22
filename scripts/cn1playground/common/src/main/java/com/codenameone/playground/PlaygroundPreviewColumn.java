package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.RoundRectBorder;

final class PlaygroundPreviewColumn extends Container {
    interface Listener {
        void onPreviewSettingsChanged();
    }

    static final String DEVICE_IPHONE = "iphone";
    static final String DEVICE_PIXEL = "pixel";
    static final String DEVICE_NO_SKIN = "none";

    static final String ORIENTATION_PORTRAIT = "portrait";
    static final String ORIENTATION_LANDSCAPE = "landscape";

    private final Container toolbar;
    private final PlaygroundSegmented deviceSegmented;
    private final PlaygroundSegmented orientationSegmented;
    private Container orientationWrapper;
    private final Label dimensionsLabel;

    private final Container stageWrapper;
    private final Container contentHost;
    private final Label stalePill;

    private String device;
    private String orientation;
    private boolean darkMode;
    private boolean compact;
    private boolean stale;
    private Component currentPreview;
    private final Listener listener;

    PlaygroundPreviewColumn(String device, String orientation, boolean darkMode, Listener listener) {
        super(new BorderLayout());
        this.device = device == null ? DEVICE_IPHONE : device;
        this.orientation = orientation == null ? ORIENTATION_PORTRAIT : orientation;
        this.darkMode = darkMode;
        this.listener = listener;

        PlaygroundSegmented.Option[] deviceOptions = new PlaygroundSegmented.Option[]{
                new PlaygroundSegmented.Option(DEVICE_IPHONE, "iPhone", FontImage.MATERIAL_PHONE_IPHONE),
                new PlaygroundSegmented.Option(DEVICE_PIXEL, "Pixel", FontImage.MATERIAL_PHONE_ANDROID),
                new PlaygroundSegmented.Option(DEVICE_NO_SKIN, "No skin", FontImage.MATERIAL_DESKTOP_WINDOWS)
        };
        deviceSegmented = new PlaygroundSegmented(deviceOptions, this.device, darkMode, key -> {
            this.device = key;
            if (orientationWrapper != null) {
                orientationWrapper.setVisible(!DEVICE_NO_SKIN.equals(key));
            }
            rebuildStage();
            if (listener != null) {
                listener.onPreviewSettingsChanged();
            }
        });

        PlaygroundSegmented.Option[] orientationOptions = new PlaygroundSegmented.Option[]{
                new PlaygroundSegmented.Option(ORIENTATION_PORTRAIT, "Portrait", FontImage.MATERIAL_STAY_CURRENT_PORTRAIT),
                new PlaygroundSegmented.Option(ORIENTATION_LANDSCAPE, "Landscape", FontImage.MATERIAL_STAY_CURRENT_LANDSCAPE)
        };
        orientationSegmented = new PlaygroundSegmented(orientationOptions, this.orientation, darkMode, key -> {
            this.orientation = key;
            rebuildStage();
            if (listener != null) {
                listener.onPreviewSettingsChanged();
            }
        });
        orientationWrapper = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        orientationWrapper.getAllStyles().setBgTransparency(0);
        orientationWrapper.add(orientationSegmented);
        orientationWrapper.setVisible(!DEVICE_NO_SKIN.equals(this.device));

        dimensionsLabel = new Label("");
        dimensionsLabel.setUIID(darkMode ? "PlaygroundDimensionsDark" : "PlaygroundDimensions");

        Container left = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        left.getAllStyles().setBgTransparency(0);
        left.add(deviceSegmented);
        left.add(orientationWrapper);

        Container right = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        right.getAllStyles().setBgTransparency(0);
        right.add(dimensionsLabel);

        toolbar = new Container(new BorderLayout());
        toolbar.setUIID(darkMode ? "PlaygroundPreviewToolbarDark" : "PlaygroundPreviewToolbar");
        toolbar.add(BorderLayout.CENTER, left);
        toolbar.add(BorderLayout.EAST, right);

        stageWrapper = new Container(new LayeredLayout());
        stageWrapper.setUIID(darkMode ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
        stageWrapper.setScrollableY(true);

        contentHost = new Container(new BorderLayout());
        contentHost.getAllStyles().setBgTransparency(0);

        stalePill = new Label("Preview paused - fix build to resume");
        stalePill.setUIID(darkMode ? "PlaygroundStaleChipDark" : "PlaygroundStaleChip");
        FontImage.setMaterialIcon(stalePill, FontImage.MATERIAL_WARNING, 2.8f);
        stalePill.setVisible(false);

        stageWrapper.add(contentHost);
        Container pillWrap = new Container(new FlowLayout(Component.CENTER, Component.TOP));
        pillWrap.getAllStyles().setBgTransparency(0);
        pillWrap.add(stalePill);
        stageWrapper.add(pillWrap);

        add(BorderLayout.NORTH, toolbar);
        add(BorderLayout.CENTER, stageWrapper);

        rebuildStage();
    }

    String getDevice() {
        return device;
    }

    String getOrientation() {
        return orientation;
    }

    void setStale(boolean stale) {
        this.stale = stale;
        stalePill.setVisible(stale);
        if (currentPreview != null) {
            if (stale) {
                currentPreview.setEnabled(false);
            } else {
                currentPreview.setEnabled(true);
            }
        }
        stageWrapper.repaint();
    }

    void setPreview(Component preview) {
        currentPreview = preview;
        rebuildStage();
    }

    Container getContentHost() {
        return contentHost;
    }

    void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        deviceSegmented.setIconsOnly(compact);
        orientationSegmented.setIconsOnly(compact);
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        toolbar.setUIID(dark ? "PlaygroundPreviewToolbarDark" : "PlaygroundPreviewToolbar");
        stageWrapper.setUIID(dark ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
        deviceSegmented.applyTheme(dark);
        orientationSegmented.applyTheme(dark);
        dimensionsLabel.setUIID(dark ? "PlaygroundDimensionsDark" : "PlaygroundDimensions");
        stalePill.setUIID(dark ? "PlaygroundStaleChipDark" : "PlaygroundStaleChip");
        FontImage.setMaterialIcon(stalePill, FontImage.MATERIAL_WARNING, 2.8f);
        rebuildStage();
    }

    private void rebuildStage() {
        contentHost.removeAll();
        if (DEVICE_NO_SKIN.equals(device)) {
            contentHost.setUIID(darkMode ? "PlaygroundNoSkinStageDark" : "PlaygroundNoSkinStage");
            stageWrapper.setUIID(darkMode ? "PlaygroundNoSkinStageDark" : "PlaygroundNoSkinStage");
            if (currentPreview != null) {
                contentHost.add(BorderLayout.CENTER, currentPreview);
            }
            updateDimensionsLabel(0, 0);
        } else {
            contentHost.setUIID(darkMode ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
            stageWrapper.setUIID(darkMode ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
            int[] logical = logicalDimensions(device);
            int screenW = logical[0];
            int screenH = logical[1];
            if (ORIENTATION_LANDSCAPE.equals(orientation)) {
                int tmp = screenW;
                screenW = screenH;
                screenH = tmp;
            }
            Container bezel = buildBezel(device, screenW, screenH);
            Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
            center.getAllStyles().setBgTransparency(0);
            center.add(bezel);
            contentHost.add(BorderLayout.CENTER, center);
            updateDimensionsLabel(screenW, screenH);
        }

        if (getComponentForm() != null) {
            revalidate();
        }
    }

    private void updateDimensionsLabel(int w, int h) {
        if (DEVICE_NO_SKIN.equals(device)) {
            dimensionsLabel.setText("Fills preview");
        } else {
            dimensionsLabel.setText(w + " x " + h);
        }
    }

    private int[] logicalDimensions(String d) {
        if (DEVICE_PIXEL.equals(d)) {
            return new int[]{393, 852};
        }
        return new int[]{375, 812};
    }

    private Container buildBezel(String device, int screenW, int screenH) {
        int pad = Display.getInstance().convertToPixels(3f);
        int bezelRadius;
        int screenRadius;
        if (DEVICE_IPHONE.equals(device)) {
            bezelRadius = Display.getInstance().convertToPixels(8f);
            screenRadius = Display.getInstance().convertToPixels(5f);
        } else {
            bezelRadius = Display.getInstance().convertToPixels(6f);
            screenRadius = Display.getInstance().convertToPixels(3f);
        }

        Container screen = new Container(new BorderLayout());
        screen.setUIID(darkMode ? "PlaygroundDeviceScreenDark" : "PlaygroundDeviceScreen");
        screen.setPreferredW(screenW);
        screen.setPreferredH(screenH);
        RoundRectBorder screenBorder = RoundRectBorder.create()
                .cornerRadius(screenRadius / (float) Display.getInstance().convertToPixels(1f));
        screen.getAllStyles().setBorder(screenBorder);
        if (currentPreview != null) {
            screen.add(BorderLayout.CENTER, currentPreview);
        }

        Container bezel = new Container(new BorderLayout());
        bezel.setUIID(darkMode ? "PlaygroundDeviceBezelDark" : "PlaygroundDeviceBezel");
        bezel.setPreferredW(screenW + pad * 2);
        bezel.setPreferredH(screenH + pad * 2);
        RoundRectBorder bezelBorder = RoundRectBorder.create()
                .cornerRadius(bezelRadius / (float) Display.getInstance().convertToPixels(1f));
        bezel.getAllStyles().setBorder(bezelBorder);
        bezel.add(BorderLayout.CENTER, screen);
        return bezel;
    }
}
