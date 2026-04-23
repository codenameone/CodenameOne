package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
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
 * A design-system gridline overlay is painted on top of every capture so
 * reviewers can eyeball component sizing against a physical 4mm grid
 * (rough 8pt-equivalent rhythm). Subclasses can opt out via
 * {@link #gridOverlayEnabled()} when the overlay would obscure the signal
 * the test is trying to establish.
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
     * Whether the 4mm design-system gridline overlay is painted above the
     * form contents. Defaults to {@code true}. Override to {@code false}
     * for tests where the grid would obscure the signal (e.g. a gradient
     * rendering test).
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
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName);
                    next.run();
                });
            }
        };
        if (showGrid) {
            form.setGlassPane(new GridOverlayPainter(dark));
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
     * Low-contrast 4mm major / 1mm minor grid painted above the form.
     * 4mm is roughly the 8pt Apple / 8dp Material design-system cell,
     * scaled via Display.convertToPixels so it matches physical size
     * on every DPI.
     */
    private static final class GridOverlayPainter implements Painter {
        private final boolean dark;

        GridOverlayPainter(boolean dark) {
            this.dark = dark;
        }

        @Override
        public void paint(Graphics g, Rectangle rect) {
            int minor = Display.getInstance().convertToPixels(1f);
            int major = Display.getInstance().convertToPixels(4f);
            if (minor < 1) {
                minor = 1;
            }
            if (major < minor) {
                major = minor * 4;
            }
            int x0 = rect.getX();
            int y0 = rect.getY();
            int x1 = x0 + rect.getWidth();
            int y1 = y0 + rect.getHeight();

            int minorColor = dark ? 0x202020 : 0xe8e8e8;
            int majorColor = dark ? 0x353535 : 0xc8c8c8;

            int prevColor = g.getColor();
            int prevAlpha = g.getAlpha();
            g.setAlpha(90);

            g.setColor(minorColor);
            for (int x = x0; x <= x1; x += minor) {
                g.drawLine(x, y0, x, y1);
            }
            for (int y = y0; y <= y1; y += minor) {
                g.drawLine(x0, y, x1, y);
            }

            g.setColor(majorColor);
            g.setAlpha(150);
            for (int x = x0; x <= x1; x += major) {
                g.drawLine(x, y0, x, y1);
            }
            for (int y = y0; y <= y1; y += major) {
                g.drawLine(x0, y, x1, y);
            }

            g.setAlpha(prevAlpha);
            g.setColor(prevColor);
        }
    }
}
