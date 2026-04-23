package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/**
 * Base for theme-fidelity screenshot tests that emit a light + dark image
 * pair. Subclasses implement {@link #populate(Form, String)} to add the
 * component(s) to be captured; the helper takes care of toggling
 * {@code Display.setDarkMode(...)}, refreshing the UIManager style cache
 * so the next style lookups re-resolve against the new appearance, showing
 * the form, waiting for onShowCompleted, and emitting the CN1SS chunk
 * with the right filename suffix.
 *
 * Instead of painting a fine uniform grid across the form (too busy to
 * read), a designer-style per-component overlay annotates each Button,
 * Label, Switch etc. with measurement guide lines and an "H=NNmm"
 * callout, letting reviewers visually verify each component is the
 * height the design system calls for (Material 40dp / iOS 44pt, etc.)
 * without squinting at uniform cross-hatching.
 */
public abstract class DualAppearanceBaseTest extends BaseTest {

    /**
     * Populate the given form with the component(s) to exercise. Called
     * once per appearance (first light, then dark) on a fresh form.
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
     * Whether the designer-style per-component guide overlay paints above
     * the form contents. Defaults to {@code true}.
     */
    protected boolean gridOverlayEnabled() {
        return true;
    }

    @Override
    public boolean runTest() {
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
        // entries (emitted by the native theme's
        // @media (prefers-color-scheme: dark) block).
        UIManager.getInstance().refreshTheme();

        final String imageName = baseName() + "_" + suffix;
        final boolean showGrid = gridOverlayEnabled();
        Form form = new Form(baseName() + " / " + suffix, newLayout()) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> {
                    // Chain next.run() through emitCurrentFormScreenshot's
                    // onComplete callback. If we call next.run() inline the
                    // dark-appearance flow kicks off Form2.show() before the
                    // Display.screenshot() callback has fired, so both emits
                    // race over the same transitioning screen buffer and
                    // produce byte-identical PNGs (classic symptom was
                    // ButtonTheme_light.png == ButtonTheme_dark.png).
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName, next);
                });
            }
        };
        if (showGrid) {
            form.setGlassPane(new ComponentGuidePainter(form, dark));
        }
        populate(form, suffix);
        form.show();
    }

    private void finish() {
        // Restore platform-default dark mode so subsequent tests in the
        // suite start from a clean slate, and refresh the theme once more
        // so any follow-up test resolves styles against the restored state.
        Display.getInstance().setDarkMode(null);
        UIManager.getInstance().refreshTheme();
        done();
    }

    /**
     * Designer-style overlay: walks the Form's component tree and draws
     * thin horizontal guide lines at the top and bottom of each
     * interactive component (Button, Label, Switch etc.) plus a small
     * "H=NNmm" callout to its right. Gives a per-component visual proof
     * of height that reviewers can eyeball against the design system's
     * spec (Material 40dp / iOS 44pt etc.) without a distracting uniform
     * cross-hatch covering the whole screen.
     */
    private static final class ComponentGuidePainter implements Painter {
        private final Form form;
        private final boolean dark;

        ComponentGuidePainter(Form form, boolean dark) {
            this.form = form;
            this.dark = dark;
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            int prevColor = g.getColor();
            int prevAlpha = g.getAlpha();
            int pxPerMm = Math.max(1, Display.getInstance().convertToPixels(1f));
            int rightEdge = rect.getX() + rect.getWidth();
            paintGuides(g, form, pxPerMm, rightEdge);
            g.setAlpha(prevAlpha);
            g.setColor(prevColor);
        }

        private void paintGuides(Graphics g, Container root, int pxPerMm, int rightEdge) {
            int count = root.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component c = root.getComponentAt(i);
                if (c == null) {
                    continue;
                }
                if (shouldAnnotate(c)) {
                    drawGuideFor(g, c, pxPerMm, rightEdge);
                }
                if (c instanceof Container) {
                    paintGuides(g, (Container) c, pxPerMm, rightEdge);
                }
            }
        }

        private boolean shouldAnnotate(Component c) {
            String id = c.getUIID();
            if (id == null) {
                return false;
            }
            return id.equals("Button")
                    || id.equals("RaisedButton")
                    || id.equals("FlatButton")
                    || id.equals("Label")
                    || id.equals("SecondaryLabel")
                    || id.equals("Switch")
                    || id.equals("OnOffSwitch")
                    || id.equals("CheckBox")
                    || id.equals("RadioButton")
                    || id.equals("TextField")
                    || id.equals("TextArea")
                    || id.equals("Tab")
                    || id.equals("MultiButton")
                    || id.equals("Title");
        }

        private void drawGuideFor(Graphics g, Component c, int pxPerMm, int rightEdge) {
            int x = c.getAbsoluteX();
            int y = c.getAbsoluteY();
            int w = c.getWidth();
            int h = c.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            int heightMm = Math.round(((float) h) / pxPerMm);

            int guideColor = dark ? 0x66bbff : 0xcc0088;
            int labelBg = dark ? 0x224466 : 0xfff0f8;

            g.setColor(guideColor);
            g.setAlpha(110);
            // Horizontal guide lines at top + bottom of the component.
            g.drawLine(x, y, x + w, y);
            g.drawLine(x, y + h - 1, x + w, y + h - 1);

            // Vertical tick marks at left/right edges so the reviewer can
            // visually measure the box without hunting for the lines.
            g.setAlpha(150);
            int tick = Math.max(2, pxPerMm / 3);
            g.drawLine(x, y - tick, x, y + tick);
            g.drawLine(x, y + h - 1 - tick, x, y + h - 1 + tick);
            g.drawLine(x + w - 1, y - tick, x + w - 1, y + tick);
            g.drawLine(x + w - 1, y + h - 1 - tick, x + w - 1, y + h - 1 + tick);

            // Height callout to the right (or below if no room).
            String label = "H=" + heightMm + "mm";
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
                labelX = Math.max(0, x + w - textW - 6);
                labelY = y + h + 2;
            }

            g.setAlpha(200);
            g.setColor(labelBg);
            g.fillRect(labelX - 2, labelY - 1, textW + 4, textH + 2);
            g.setColor(guideColor);
            g.drawString(label, labelX, labelY);
        }
    }
}
