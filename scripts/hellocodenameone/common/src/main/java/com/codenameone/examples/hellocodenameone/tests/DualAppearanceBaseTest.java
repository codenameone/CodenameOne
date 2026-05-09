package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for theme-fidelity screenshot tests that emit a light + dark image
 * pair against the modern iOS or Android Material native theme. The
 * legacy iOS 7 / Android Holo themes stay in place as the framework's
 * default - the modern theme is opt-in specifically for tests in this
 * family so existing screenshot goldens aren't silently redesigned.
 *
 * Subclasses implement {@link #populate(Form, String)} to add the
 * component(s) to exercise. During populate() they can also call
 * {@link #annotateComponent(Component, String)} to register a specific
 * component for the designer-style grid overlay - a thin horizontal
 * line at its top, its content/text band, its bottom, plus a legend
 * SpanLabel at the bottom of the form describing the measurement. The
 * overlay is opt-in per component instead of painted on every Button/
 * Label blindly, so it stays readable even when the form is dense.
 */
public abstract class DualAppearanceBaseTest extends BaseTest {

    /**
     * Populate the given form with the component(s) to exercise. Called
     * once per appearance (first light, then dark) on a fresh form.
     * Use {@link #annotateComponent(Component, String)} from inside
     * populate() to tag specific components for the grid overlay.
     *
     * @param form   fresh form with its Layout already set
     * @param suffix "light" or "dark" - useful if populate() wants to
     *               surface the active appearance in a Label, for example.
     */
    protected abstract void populate(Form form, String suffix);

    /**
     * Subclasses override to provide the image-name prefix used for both
     * captures. The emitted chunks will be named {@code <baseName>_light}
     * and {@code <baseName>_dark}.
     */
    protected abstract String baseName();

    /**
     * Subclasses override to provide the root layout. A fresh instance is
     * requested for each appearance.
     */
    protected abstract Layout newLayout();

    /**
     * Subclasses override if a specific test should stay on the legacy
     * default theme (iOS 7 / Android Holo Light) - e.g. a regression
     * test that must exercise the legacy palette. Default: modern theme.
     */
    protected boolean useModernTheme() {
        return true;
    }

    /**
     * Subclasses override (return true) when the widget under test has
     * translucent aspects (Dialog, Tabs pill, PopupContent, ...). A
     * colourful diagonal-stripe texture is painted behind the form
     * so any see-through tint is visible in the screenshot rather than
     * blending into a plain Form bg. Default: plain form background.
     */
    protected boolean useTexturedBackdrop() {
        return false;
    }

    private final List<Annotation> annotations = new ArrayList<Annotation>();

    /**
     * Register a component for the designer-style grid overlay. Call
     * from inside {@link #populate(Form, String)}. A thin guide line is
     * drawn at the component's top, text band, and bottom, and the
     * supplied legend is appended as a SpanLabel at the bottom of the
     * form describing what's being measured (e.g. "Primary button:
     * Material 3 full rounded, target H=10mm / 40dp, text centered").
     */
    protected final void annotateComponent(Component c, String legend) {
        if (c == null) {
            return;
        }
        annotations.add(new Annotation(c, legend));
    }

    @Override
    public boolean runTest() {
        installModernThemeIfRequested();
        runAppearance(false, "light", () -> runAppearance(true, "dark", this::finish));
        return true;
    }

    private void runAppearance(boolean dark, final String suffix, final Runnable next) {
        Display.getInstance().setDarkMode(dark);
        // UIManager caches resolved Style objects per UIID; without this call
        // the next lookup returns the Style that was resolved while the other
        // appearance was active, and the screenshot comes out in the wrong
        // appearance. UIManager.refreshTheme() clears the caches and re-runs
        // the theme build pass against CN.isDarkMode()'s current value, so
        // fresh components on the new Form pick up the correct $Dark<UIID>
        // entries (emitted by the native theme's @media dark block).
        UIManager.getInstance().refreshTheme();

        annotations.clear();

        final String imageName = baseName() + "_" + suffix;
        final boolean textured = useTexturedBackdrop();
        final TextureBackdropPainter backdrop = textured
                ? new TextureBackdropPainter(dark)
                : null;
        Form form = new Form(baseName() + " / " + suffix, newLayout()) {
            @Override
            public void paintBackground(Graphics g) {
                if (backdrop != null) {
                    // Paint the diagonal-stripe pattern into the form's
                    // backing area before the rest of the render pipeline
                    // runs. Any translucent widget above (Dialog, pill
                    // Tabs, Popup) then reveals its see-through tint
                    // against a visible pattern instead of a plain surface.
                    backdrop.paint(g, new Rectangle(0, 0, getWidth(), getHeight()));
                    return;
                }
                super.paintBackground(g);
            }

            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> {
                    // Chain next.run() through emitCurrentFormScreenshot's
                    // onComplete callback. If we call next.run() inline the
                    // dark-appearance flow kicks off Form2.show() before the
                    // Display.screenshot() callback has fired, so both emits
                    // race over the same transitioning buffer and produce
                    // byte-identical PNGs (classic symptom was
                    // ButtonTheme_light.png == ButtonTheme_dark.png).
                    //
                    // Even with that chain in place, on iOS Metal the
                    // light->dark show transition was leaving the previous
                    // frame's pixels in the CAMetalLayer at the moment
                    // cn1_captureView ran with afterScreenUpdates:NO, so
                    // the dark-tagged screenshot grabbed light-form pixels
                    // (visible victims: DialogTheme_dark, FloatingAction-
                    // ButtonTheme_light). Pump three Display.callSerially
                    // hops before emit so at least three EDT paint cycles
                    // (and therefore three Metal frame presents) land
                    // between the form's own onShowCompleted hand-off and
                    // the actual capture; combined with the createEmpty()
                    // transition below this gives the new form's pixels
                    // time to reach the front buffer.
                    Display.getInstance().callSerially(() ->
                        Display.getInstance().callSerially(() ->
                            Display.getInstance().callSerially(() ->
                                Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, next))));
                });
            }
        };
        // Skip the form-show transition entirely. The default fade/slide
        // takes ~300ms during which CN1 is still drawing the *previous*
        // form into the back buffer; on iOS Metal that means the screen-
        // shot's CAMetalLayer contents linger on the old frame even after
        // onShowCompleted fires. createEmpty() makes form.show() switch
        // synchronously so onShowCompleted fires with the new form
        // already painted, removing the transition window from the race.
        form.setTransitionInAnimator(CommonTransitions.createEmpty());
        form.setTransitionOutAnimator(CommonTransitions.createEmpty());
        populate(form, suffix);
        if (textured) {
            // The ContentPane sits on top of the Form and paints its own
            // theme-supplied bgColor on every render; without making it
            // transparent the texture paint underneath is hidden by a
            // solid wash. TitleArea / Toolbar likewise opaque - clear
            // them too so the backdrop reads edge-to-edge.
            form.getContentPane().getUnselectedStyle().setBgTransparency((byte) 0);
            form.getTitleArea().getUnselectedStyle().setBgTransparency((byte) 0);
        }
        if (!annotations.isEmpty()) {
            form.setGlassPane(new AnnotationPainter(annotations, dark));
            SpanLabel legend = buildLegend();
            if (legend != null) {
                form.add(legend);
            }
        }
        form.show();
    }

    private SpanLabel buildLegend() {
        if (annotations.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Grid: ");
        for (int i = 0; i < annotations.size(); i++) {
            Annotation a = annotations.get(i);
            if (a.legend == null || a.legend.length() == 0) {
                continue;
            }
            if (sb.length() > 7) {
                sb.append(" - ");
            }
            sb.append(a.legend);
        }
        SpanLabel s = new SpanLabel(sb.toString());
        s.setUIID("TertiaryLabel");
        return s;
    }

    private void finish() {
        // Restore platform-default dark mode + the app's own theme so
        // subsequent tests in the suite (legacy screenshots matching
        // pre-change goldens) see exactly the state they had before
        // this test ran.
        Display.getInstance().setDarkMode(null);
        if (useModernTheme()) {
            // UIManager.initFirstTheme loads /theme.res (the app's
            // compiled theme.css). With includeNativeBool=true in its
            // constants it triggers Display.installNativeTheme() -
            // Holo Light / iPhoneTheme per the platform's legacy default -
            // and then layers the user's UIID overrides on top. This
            // recreates the original startup theme state, which
            // Display.installNativeTheme alone doesn't (it drops the
            // user's font / padding / colour overrides).
            UIManager.initFirstTheme("/theme");
        }
        UIManager.getInstance().refreshTheme();
        done();
    }

    private void installModernThemeIfRequested() {
        if (!useModernTheme()) {
            return;
        }
        String resourceName = pickModernThemeResource();
        if (resourceName == null) {
            logDiag("CN1SS:INFO:DualAppearance no modern theme resource for platform="
                    + Display.getInstance().getPlatformName());
            return;
        }
        // Try the CN1 resource path first (Display.getResourceAsStream goes
        // through each port's impl - Android via getAssets(), iOS via
        // nativeInstance.getResourceSize/NSFileInputStream), then fall back
        // to Class.getResourceAsStream for platforms where the .res sits on
        // the Java classpath (JavaSE / JavaScript).
        InputStream in = openModernResource(resourceName);
        if (in == null) {
            logDiag("CN1SS:WARN:DualAppearance modern theme resource missing: " + resourceName
                    + " test=" + baseName() + " platform=" + Display.getInstance().getPlatformName());
            return;
        }
        try {
            Resources r = Resources.open(in);
            String[] names = r.getThemeResourceNames();
            if (names == null || names.length == 0) {
                logDiag("CN1SS:ERR:DualAppearance modern theme has no themes resource=" + resourceName
                        + " test=" + baseName());
                return;
            }
            UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
            logDiag("CN1SS:INFO:DualAppearance installed modern theme " + resourceName
                    + " themeName=" + names[0] + " test=" + baseName());
        } catch (IOException ex) {
            logDiag("CN1SS:ERR:DualAppearance modern theme load failed: " + ex
                    + " resource=" + resourceName + " test=" + baseName());
        } finally {
            Util.cleanup(in);
        }
    }

    private InputStream openModernResource(String resourceName) {
        InputStream in = Display.getInstance().getResourceAsStream(getClass(), resourceName);
        if (in != null) {
            return in;
        }
        return DualAppearanceBaseTest.class.getResourceAsStream(resourceName);
    }

    // Route diagnostic messages through com.codename1.io.Log as well as
    // System.out. On iOS, `simctl log stream` sheds `stdout` lines when the
    // CN1SS base64 PNG burst saturates unified logging; Log.p ends up in
    // device-runner.log's fallback persistence path and survives the drop.
    private static void logDiag(String message) {
        System.out.println(message);
        Log.p(message);
    }

    private String pickModernThemeResource() {
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform)) {
            return "/iOSModernTheme.res";
        }
        if ("and".equals(platform)) {
            return "/AndroidMaterialTheme.res";
        }
        return null;
    }

    private static final class Annotation {
        final Component component;
        final String legend;

        Annotation(Component component, String legend) {
            this.component = component;
            this.legend = legend;
        }
    }

    /**
     * Designer-style overlay: for each annotated component, paints three
     * thin horizontal guide lines (top edge, content/text band, bottom
     * edge) plus an H=NNmm callout. The text band is derived from the
     * component's padding so reviewers can eyeball the spec (e.g.
     * "button text centered with 2mm top/bottom padding").
     */
    private static final class AnnotationPainter implements Painter {
        private final List<Annotation> annotations;
        private final boolean dark;

        AnnotationPainter(List<Annotation> annotations, boolean dark) {
            this.annotations = annotations;
            this.dark = dark;
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            if (annotations.isEmpty()) {
                return;
            }
            int prevColor = g.getColor();
            int prevAlpha = g.getAlpha();
            int pxPerMm = Math.max(1, Display.getInstance().convertToPixels(1f));
            int rightEdge = rect.getX() + rect.getWidth();

            int edgeColor = dark ? 0x66bbff : 0xcc0088;
            int textBandColor = dark ? 0x88ff99 : 0x00aa55;
            int labelBg = dark ? 0x002233 : 0xfff0f8;

            for (Annotation a : annotations) {
                Component c = a.component;
                if (c == null) {
                    continue;
                }
                int x = c.getAbsoluteX();
                int y = c.getAbsoluteY();
                int w = c.getWidth();
                int h = c.getHeight();
                if (w <= 0 || h <= 0) {
                    continue;
                }

                Style s = c.getUnselectedStyle();
                int padTop = s != null ? s.getPaddingTop() : 0;
                int padBottom = s != null ? s.getPaddingBottom() : 0;
                int textTop = y + padTop;
                int textBottom = y + h - padBottom;
                int heightMm = Math.round(((float) h) / pxPerMm);
                int textHeightMm = Math.round(((float) (textBottom - textTop)) / pxPerMm);

                // Edge guide lines at the top and bottom of the component.
                g.setColor(edgeColor);
                g.setAlpha(180);
                g.drawLine(x, y, x + w - 1, y);
                g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);

                // End ticks so the reviewer can visually measure the box.
                int tick = Math.max(2, pxPerMm / 2);
                g.drawLine(x, y - tick, x, y + tick);
                g.drawLine(x + w - 1, y - tick, x + w - 1, y + tick);
                g.drawLine(x, y + h - 1 - tick, x, y + h - 1 + tick);
                g.drawLine(x + w - 1, y + h - 1 - tick, x + w - 1, y + h - 1 + tick);

                // Text-band guides (inset by padding) in a second colour
                // so the text position inside the component is measurable
                // too, not just the outer box.
                if (textBottom > textTop + pxPerMm) {
                    g.setColor(textBandColor);
                    g.setAlpha(140);
                    g.drawLine(x, textTop, x + w - 1, textTop);
                    g.drawLine(x, textBottom, x + w - 1, textBottom);
                }

                // Callout placed outside the component (to the right if
                // there's room, otherwise below).
                String label = "H=" + heightMm + "mm, text=" + textHeightMm + "mm";
                Font f = g.getFont();
                if (f == null) {
                    f = Font.getDefaultFont();
                    g.setFont(f);
                }
                int textW = f.stringWidth(label);
                int textH = f.getHeight();
                int labelX = x + w + 2;
                int labelY = y + (h - textH) / 2;
                if (labelX + textW + 4 > rightEdge) {
                    labelX = Math.max(0, rightEdge - textW - 6);
                    labelY = y + h + 2;
                }

                g.setAlpha(210);
                g.setColor(labelBg);
                g.fillRect(labelX - 2, labelY - 1, textW + 4, textH + 2);
                g.setColor(edgeColor);
                g.drawString(label, labelX, labelY);
            }

            g.setAlpha(prevAlpha);
            g.setColor(prevColor);
        }
    }

    /**
     * Diagonal-stripe texture backdrop. Bright alternating bands behind
     * the Form so a translucent widget above (Dialog, pill Tabs,
     * PopupContent) reveals its see-through tint in the screenshot
     * instead of painting over a plain surface that would make the
     * translucency invisible.
     */
    private static final class TextureBackdropPainter implements Painter {
        private final boolean dark;

        TextureBackdropPainter(boolean dark) {
            this.dark = dark;
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            int prevColor = g.getColor();
            int prevAlpha = g.getAlpha();
            int x = rect.getX();
            int y = rect.getY();
            int w = rect.getWidth();
            int h = rect.getHeight();

            // Base fill - a neutral mid-tone so stripes have somewhere
            // to sit. Dark mode uses a dark base, light uses a light base.
            g.setAlpha(255);
            g.setColor(dark ? 0x202030 : 0xf0e8f8);
            g.fillRect(x, y, w, h);

            // Diagonal stripes painted as rotated rectangles. 6mm-ish band
            // width reads well at phone resolution. Palette is kept
            // saturated so even a 10% translucent widget's tint is clearly
            // picked up against it.
            int pxPerMm = Math.max(1, Display.getInstance().convertToPixels(1f));
            int bandW = pxPerMm * 6;
            int[] lightPalette = { 0xff7eb2, 0x7ec8ff, 0xffd67e, 0x9affc8, 0xd8a0ff };
            int[] darkPalette  = { 0x882244, 0x224488, 0x886622, 0x226644, 0x664488 };
            int[] palette = dark ? darkPalette : lightPalette;
            g.setAlpha(180);
            int diagonalOffset = -h; // start off-screen so the pattern fills
            int band = 0;
            int[] xCoords = new int[4];
            int[] yCoords = new int[4];
            while (diagonalOffset < w + h) {
                g.setColor(palette[band % palette.length]);
                // Each band is a parallelogram with two horizontal edges
                // (top at y, bottom at y+h) and two diagonal edges. Fill
                // it with one fillPolygon call rather than per-row
                // fillRect: at phone resolution h is ~2500 px and there
                // are ~50 bands, so the scanline approach used to issue
                // ~125k draw calls per backdrop. On iOS Metal each
                // fillRect submits a fresh setRenderPipelineState +
                // setVertexBytes pair, and at that volume the CAMetalLayer
                // command-buffer commit was stalling the dark-mode
                // transition for TabsTheme by 18 minutes. Polygon fill
                // is one draw call per band (50 total) and is universally
                // supported by every CN1 port we ship (Graphics.fillPolygon
                // is a core API, not a port-specific extension). The
                // previous comment on this loop suggested otherwise; it
                // was wrong.
                xCoords[0] = x + diagonalOffset;
                yCoords[0] = y;
                xCoords[1] = x + diagonalOffset + bandW;
                yCoords[1] = y;
                xCoords[2] = x + diagonalOffset + bandW + h;
                yCoords[2] = y + h;
                xCoords[3] = x + diagonalOffset + h;
                yCoords[3] = y + h;
                g.fillPolygon(xCoords, yCoords, 4);
                diagonalOffset += bandW;
                band++;
            }

            g.setAlpha(prevAlpha);
            g.setColor(prevColor);
        }
    }
}
