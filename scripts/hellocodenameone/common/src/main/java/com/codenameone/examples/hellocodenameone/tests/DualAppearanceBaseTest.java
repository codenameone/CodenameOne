package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.UITimer;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;

/**
 * Base for theme-fidelity screenshot tests that emit a light + dark image
 * pair. Subclasses implement {@link #populate(Form, String)} to add the
 * component(s) to be captured; the helper takes care of toggling
 * {@code Display.setDarkMode(...)}, clearing the UIManager style caches so
 * the next style lookups re-resolve against the new appearance, showing
 * the form, waiting for onShowCompleted, and emitting the CN1SS chunk
 * with the right filename suffix.
 *
 * Used by the CSS-driven native-themes work to validate that both the iOS
 * Modern and Android Material themes render each core UIID correctly in
 * both appearances.
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

    @Override
    public boolean runTest() {
        runAppearance(false, "light", () -> runAppearance(true, "dark", this::finish));
        return true;
    }

    private void runAppearance(boolean dark, final String suffix, final Runnable next) {
        Display.getInstance().setDarkMode(dark);
        // UIManager caches resolved Style objects in its styles / selectedStyles
        // maps keyed by UIID. A Style created while Display.isDarkMode() was
        // false is the LIGHT variant; the cache then returns that light Style
        // for later lookups even after we flip to dark. Reflectively clear the
        // caches so the next Component.initLaf -> UIManager.getComponentStyle
        // re-runs the style resolution path (shouldUseDarkStyle consults
        // CN.isDarkMode() per call and picks up the $Dark<UIID> entries that
        // the native themes' @media (prefers-color-scheme: dark) block emits).
        clearStyleCaches();

        final String imageName = baseName() + "_" + suffix;
        Form form = new Form(baseName() + " / " + suffix, newLayout()) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(imageName);
                    next.run();
                });
            }
        };
        populate(form, suffix);
        form.show();
    }

    private void finish() {
        // Restore platform-default dark mode so subsequent tests in the
        // suite start from a clean slate, and clear caches once more so any
        // follow-up test resolves its styles from scratch.
        Display.getInstance().setDarkMode(null);
        clearStyleCaches();
        done();
    }

    private static void clearStyleCaches() {
        clearField("styles");
        clearField("selectedStyles");
    }

    private static void clearField(String name) {
        try {
            Field f = UIManager.class.getDeclaredField(name);
            f.setAccessible(true);
            Object value = f.get(UIManager.getInstance());
            if (value instanceof Hashtable) {
                ((Hashtable) value).clear();
            } else if (value instanceof Map) {
                ((Map) value).clear();
            }
        } catch (Throwable ignored) {
            // Reflection not available (e.g. no-reflection VM on device):
            // best effort. On those platforms the dark-mode screenshot
            // mirrors whichever appearance was active on first lookup.
        }
    }
}
