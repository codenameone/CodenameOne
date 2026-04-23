package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.GeneralPath;
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

    private Container toolbar;
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
    private boolean mobile;
    private String savedDeviceBeforeMobile;
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
                orientationWrapper.setHidden(DEVICE_NO_SKIN.equals(key));
                orientationWrapper.setVisible(!DEVICE_NO_SKIN.equals(key));
            }
            rebuildStage();
            if (listener != null) {
                listener.onPreviewSettingsChanged();
            }
            if (toolbar != null && toolbar.getComponentForm() != null) {
                toolbar.revalidate();
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
        // Orientation uses icons only in every layout density -- the labels
        // add no value when the two icons are self-explanatory.
        orientationSegmented.setIconsOnly(true);
        orientationWrapper = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        orientationWrapper.getAllStyles().setBgTransparency(0);
        orientationWrapper.add(orientationSegmented);
        boolean hideOrientation = DEVICE_NO_SKIN.equals(this.device);
        orientationWrapper.setHidden(hideOrientation);
        orientationWrapper.setVisible(!hideOrientation);

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
        applyStageScroll();

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

    /// Refreshes theme styles only on the user's preview content. Skips the bezel,
    /// screen, and corner-mask overlay so their programmatic borders / transparency
    /// are not overwritten by `refreshTheme()`.
    void refreshPreviewTheme() {
        if (currentPreview == null) {
            return;
        }
        currentPreview.refreshTheme();
        Container parent = currentPreview.getParent();
        if (parent != null) {
            parent.revalidate();
        } else {
            currentPreview.repaint();
        }
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

    /// Mobile: toolbar is hidden entirely, device is forced to no-skin so the
    /// preview fills 100% of the tab area without a synthetic phone bezel
    /// inside the real phone. Reverts on desktop/tablet to whichever device
    /// was active before the shift.
    void setMobile(boolean mobile) {
        if (this.mobile == mobile) {
            return;
        }
        this.mobile = mobile;
        toolbar.setVisible(!mobile);
        toolbar.setHidden(mobile);
        if (mobile) {
            if (!DEVICE_NO_SKIN.equals(device)) {
                savedDeviceBeforeMobile = device;
            }
            device = DEVICE_NO_SKIN;
            deviceSegmented.setSelected(DEVICE_NO_SKIN);
        } else if (savedDeviceBeforeMobile != null) {
            device = savedDeviceBeforeMobile;
            deviceSegmented.setSelected(savedDeviceBeforeMobile);
            savedDeviceBeforeMobile = null;
        }
        applyStageScroll();
        rebuildStage();
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

    /// Exclusive scroll direction based on orientation: portrait scrolls Y, landscape
    /// scrolls X. Never both at once - CN1 doesn't render two-axis scroll well.
    private void applyStageScroll() {
        boolean landscape = ORIENTATION_LANDSCAPE.equals(orientation) && !DEVICE_NO_SKIN.equals(device);
        if (landscape) {
            stageWrapper.setScrollableY(false);
            stageWrapper.setScrollableX(true);
        } else {
            stageWrapper.setScrollableX(false);
            stageWrapper.setScrollableY(true);
        }
    }

    private void rebuildStage() {
        detachPreview();
        applyStageScroll();
        contentHost.removeAll();
        if (DEVICE_NO_SKIN.equals(device)) {
            contentHost.setUIID(darkMode ? "PlaygroundNoSkinStageDark" : "PlaygroundNoSkinStage");
            stageWrapper.setUIID(darkMode ? "PlaygroundNoSkinStageDark" : "PlaygroundNoSkinStage");
            if (currentPreview != null) {
                contentHost.add(BorderLayout.CENTER, currentPreview);
            }
            dimensionsLabel.setText("Fills preview");
        } else {
            contentHost.setUIID(darkMode ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
            stageWrapper.setUIID(darkMode ? "PlaygroundDeviceStageDark" : "PlaygroundDeviceStage");
            Container bezel = buildBezel();
            Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
            center.getAllStyles().setBgTransparency(0);
            center.add(bezel);
            contentHost.add(BorderLayout.CENTER, center);
            int[] logical = logicalResolution();
            dimensionsLabel.setText(logical[0] + " x " + logical[1]);
        }

        if (getComponentForm() != null) {
            revalidate();
        }
    }

    private void detachPreview() {
        if (currentPreview == null) {
            return;
        }
        Container parent = currentPreview.getParent();
        if (parent != null) {
            parent.removeComponent(currentPreview);
        }
    }

    /// Returns logical CSS-pixel resolution advertised to the user.
    /// Width first, height second, already rotated for the current orientation.
    private int[] logicalResolution() {
        int w = DEVICE_PIXEL.equals(device) ? 393 : 375;
        int h = DEVICE_PIXEL.equals(device) ? 852 : 812;
        return ORIENTATION_LANDSCAPE.equals(orientation) ? new int[]{h, w} : new int[]{w, h};
    }

    /// Returns physical device dimensions in millimeters for the current device + orientation.
    /// Array layout: [deviceW, deviceH, screenW, screenH, bezelCorner, screenCorner].
    private float[] physicalMillimeters() {
        float deviceW, deviceH, screenW, screenH, bezelCorner, screenCorner;
        if (DEVICE_PIXEL.equals(device)) {
            deviceW = 73f; deviceH = 155f;
            screenW = 68f; screenH = 150f;
            bezelCorner = 6f; screenCorner = 4f;
        } else {
            deviceW = 72f; deviceH = 147f;
            screenW = 67f; screenH = 142f;
            bezelCorner = 8f; screenCorner = 6f;
        }
        if (ORIENTATION_LANDSCAPE.equals(orientation)) {
            float t = deviceW; deviceW = deviceH; deviceH = t;
            t = screenW; screenW = screenH; screenH = t;
        }
        return new float[]{deviceW, deviceH, screenW, screenH, bezelCorner, screenCorner};
    }

    private Container buildBezel() {
        float[] mm = physicalMillimeters();
        Display display = Display.getInstance();
        int bezelPxW = display.convertToPixels(mm[0]);
        int bezelPxH = display.convertToPixels(mm[1]);
        int screenPxW = display.convertToPixels(mm[2]);
        int screenPxH = display.convertToPixels(mm[3]);
        int screenCornerPx = display.convertToPixels(mm[5]);

        Container content = new Container(new BorderLayout());
        content.setUIID(darkMode ? "PlaygroundDeviceScreenDark" : "PlaygroundDeviceScreen");
        if (currentPreview != null) {
            content.add(BorderLayout.CENTER, currentPreview);
        }

        CornerMaskOverlay cornerMask = new CornerMaskOverlay(BEZEL_FILL_COLOR, screenCornerPx);

        Container screen = new Container(new LayeredLayout());
        screen.setPreferredW(screenPxW);
        screen.setPreferredH(screenPxH);
        screen.getAllStyles().setPadding(0, 0, 0, 0);
        screen.getAllStyles().setMargin(0, 0, 0, 0);
        screen.add(content);
        screen.add(cornerMask);

        Container bezel = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        bezel.setUIID(darkMode ? "PlaygroundDeviceBezelDark" : "PlaygroundDeviceBezel");
        bezel.setPreferredW(bezelPxW);
        bezel.setPreferredH(bezelPxH);
        bezel.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(mm[4]));
        bezel.add(screen);
        return bezel;
    }

    private static final int BEZEL_FILL_COLOR = 0x1A1A1C;

    /// Overlay component that paints bezel-color pixels over the four corners of the
    /// layered-layout sibling beneath it, producing the illusion that content is
    /// clipped to the device's inner corner radius.
    ///
    /// Lives as a sibling of the content in `LayeredLayout` rather than as a Border
    /// because `getAllStyles().setBorder(...)` is wiped whenever `refreshTheme()` runs
    /// on a UIID that has no `border:` declaration in theme.css. As a real Component
    /// it survives theme refreshes. `setIgnorePointerEvents(true)` so taps fall through
    /// to the preview beneath.
    ///
    /// Each corner is a closed path: two straight edges of the outer-corner square plus
    /// a cubic Bezier approximating a quarter-circle arc (k ~= 0.5523). This renders
    /// identically on every CN1 port without arc-direction ambiguity.
    private static final class CornerMaskOverlay extends Component {
        private static final float BEZIER_K = 0.5522847498f;
        /// Pixels of overhang past the component bounds on each outer straight edge.
        /// CN1 clips the fill at the component's bounds, so the overhang is invisible,
        /// but it ensures boundary pixels are fully inside the fill polygon rather than
        /// sitting exactly on an anti-aliased edge (which would leave ~50% coverage).
        private static final int OVERHANG = 2;

        private final int color;
        private final int radius;

        CornerMaskOverlay(int color, int radius) {
            this.color = color;
            this.radius = Math.max(1, radius);
            setFocusable(false);
            setEnabled(false);
            setIgnorePointerEvents(true);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(0, 0);
        }

        @Override
        public void paint(Graphics g) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int r = Math.min(radius, Math.min(w, h) / 2);
            if (r <= 0) {
                return;
            }
            int oldColor = g.getColor();
            int oldAlpha = g.getAlpha();
            boolean oldAA = g.isAntiAliased();
            g.setColor(color);
            g.setAlpha(255);
            g.setAntiAliased(true);

            float k = BEZIER_K * r;
            int o = OVERHANG;

            // Each sliver: overhang rectangle outside the component bounds + Bezier arc
            // approximating the device's inner corner radius. The overhang ensures the
            // component's boundary pixels are inside the polygon instead of on its edge.

            GeneralPath tl = new GeneralPath();
            tl.moveTo(x + r, y - o);
            tl.lineTo(x - o, y - o);
            tl.lineTo(x - o, y + r);
            tl.lineTo(x, y + r);
            tl.curveTo(x, y + r - k, x + r - k, y, x + r, y);
            tl.closePath();
            g.fillShape(tl);

            GeneralPath tr = new GeneralPath();
            tr.moveTo(x + w - r, y - o);
            tr.lineTo(x + w + o, y - o);
            tr.lineTo(x + w + o, y + r);
            tr.lineTo(x + w, y + r);
            tr.curveTo(x + w, y + r - k, x + w - r + k, y, x + w - r, y);
            tr.closePath();
            g.fillShape(tr);

            GeneralPath br = new GeneralPath();
            br.moveTo(x + w - r, y + h + o);
            br.lineTo(x + w + o, y + h + o);
            br.lineTo(x + w + o, y + h - r);
            br.lineTo(x + w, y + h - r);
            br.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
            br.closePath();
            g.fillShape(br);

            GeneralPath bl = new GeneralPath();
            bl.moveTo(x + r, y + h + o);
            bl.lineTo(x - o, y + h + o);
            bl.lineTo(x - o, y + h - r);
            bl.lineTo(x, y + h - r);
            bl.curveTo(x, y + h - r + k, x + r - k, y + h, x + r, y + h);
            bl.closePath();
            g.fillShape(bl);

            g.setAntiAliased(oldAA);
            g.setAlpha(oldAlpha);
            g.setColor(oldColor);
        }
    }
}
