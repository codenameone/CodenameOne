package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
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
        Form form = new Form(baseName() + " / " + suffix, newLayout()) {
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
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, next);
                });
            }
        };
        populate(form, suffix);
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
        // Restore platform-default dark mode + the installed native theme
        // so subsequent tests in the suite (including ones that rely on
        // the legacy theme) start from a clean slate.
        Display.getInstance().setDarkMode(null);
        if (useModernTheme()) {
            // Reload the platform's default theme via the native impl.
            // This reinstalls iPhoneTheme / iOS7Theme / android_holo_light
            // / androidTheme per the non-modern defaults - exactly what
            // the app saw before this test ran.
            Display.getInstance().installNativeTheme();
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
            return;
        }
        InputStream in = getClass().getResourceAsStream(resourceName);
        if (in == null) {
            // Modern theme isn't packaged on this platform - stay on the
            // legacy default rather than crashing the test.
            return;
        }
        try {
            Resources r = Resources.open(in);
            UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
        } catch (IOException ex) {
            // Leave the legacy theme in place on error.
        } finally {
            Util.cleanup(in);
        }
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
}
