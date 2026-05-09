package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

/**
 * Verifies that a sub-theme can re-skin the native palette without
 * touching the native theme's CSS source.
 *
 * The native CSS declares an accent palette via `var(--accent-color,
 * fallback)` references. The compiler bakes the fallback into each
 * UIID at compile time AND emits `@cn1-bind:&lt;UIID&gt;.&lt;key&gt;=accent-color`
 * constants alongside, so the .res ships with every accent-bearing
 * UIID quietly tracking the underlying palette variable. The
 * recommended override path is from the user app's own `theme.css`
 * (see Native-Themes docs - declaring `--accent-color: #ff2d95;`
 * inside the app's `#Constants` block exports it as a
 * `@accent-color` theme constant which the framework then fans out
 * to every bound UIID at theme-install time). This screenshot test
 * exercises the equivalent runtime path -
 * {@link UIManager#addThemeProps(Hashtable)} with the same
 * `@`-prefixed constants - because (a) screenshot tests cannot
 * easily mutate the app's compiled theme.css, and (b) the runtime
 * mechanism is what ships for dynamic theming use cases (in-app
 * accent toggles, A/B tests, branded flavours).
 *
 * This test installs a magenta override on the primary accent and a
 * vivid teal on the disabled accent. The teal is the load-bearing
 * choice for cross-platform coverage: on iOS Modern the only visible
 * widgets that rebind when accent-color changes are RaisedButton +
 * Button (which the magenta already exercises), but the disabled
 * RaisedButton stays at the default light-blue accent-disabled tone
 * unless `@accent-disabled-color` is also retuned. Adding the teal
 * therefore produces a visible iOS pixel diff against a baseline that
 * predates the binding mechanism, confirming the binding fires on iOS
 * and isn't merely a no-op coincidence with the magenta. Android
 * Material 3 doesn't bind its disabled state to accent-disabled (its
 * disabled colours are hard-coded in CSS), so the teal is iOS-only;
 * Android's diff is driven by the magenta `@accent-container-color`
 * retuning RaisedButton's tonal fill.
 *
 * The light capture exercises the light base styles; the dark capture
 * exercises the {@code $Dark<UIID>} variants which are bound to the
 * matching `-dark` palette variables.
 *
 * Suite hygiene: this test installs the `@`-prefixed override
 * constants once during the light populate(). DualAppearanceBaseTest's
 * `finish()` runs after the dark capture and reloads `/theme` via
 * {@link UIManager#initFirstTheme}, which routes through
 * `setThemePropsImpl` and clears `themeConstants` before re-populating
 * from the freshly-loaded theme - so the `@accent-color` (etc.)
 * constants do NOT survive into the next test in the suite. The
 * test's slot in `Cn1ssDeviceRunner` (after the theme-fidelity
 * sub-suite, before OrientationLock) keeps it on the back end of the
 * run regardless, so any future regression that drops
 * `initFirstTheme` would still only affect tests that explicitly opt
 * in to this run's tail.
 */
public class PaletteOverrideThemeScreenshotTest extends DualAppearanceBaseTest {

    private static final String OVERRIDE_ACCENT = "ff2d95";
    private static final String OVERRIDE_ACCENT_PRESSED = "c71a75";
    private static final String OVERRIDE_ACCENT_TEXT = "ffffff";
    /// Vivid teal for the disabled accent slot. Distinct from the iOS
    /// Modern light/dark blue defaults (#b3d4ff / #004a99) and from the
    /// magenta accent so the disabled RaisedButton on the form reads
    /// as a third independent colour at a glance.
    private static final String OVERRIDE_ACCENT_DISABLED = "00b894";
    private boolean overrideInstalled;

    @Override
    protected String baseName() {
        return "PaletteOverrideTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        if (!overrideInstalled) {
            installPaletteOverride();
            overrideInstalled = true;
        }

        form.add(new Label("Primary / accent UIIDs"));

        Button primary = new Button("Raised");
        primary.setUIID("RaisedButton");
        form.add(primary);

        Button text = new Button("Text");
        form.add(text);

        form.add(new Label("Disabled state"));
        Button disabled = new Button("Disabled");
        disabled.setUIID("RaisedButton");
        disabled.setEnabled(false);
        form.add(disabled);

        Label footer = new Label("Magenta override active in both appearances");
        footer.setUIID("SecondaryLabel");
        form.add(footer);
    }

    /**
     * Adds a palette-override layer on top of the installed native
     * theme by declaring `@`-prefixed accent constants. The runtime
     * binding pass in {@link UIManager} fans each constant out to every
     * bound UIID/state/dark variant, so this short Hashtable replaces
     * the 12+ explicit per-UIID keys the override used to require.
     *
     * Both Android and iOS native themes share the same variable
     * vocabulary (see native-themes/&lt;family&gt;/theme.css `#Constants`).
     * The Android theme additionally exposes M3-flavoured container
     * tokens (`accent-container-color`, `accent-on-container-color`)
     * which we override too so RaisedButton-style "tonal" surfaces also
     * pick up the magenta - leaving them at the default would let
     * Android-only RaisedButton.bgColor remain at the M3 baseline tone
     * even though Button.fgColor and the matching iOS RaisedButton
     * already shifted.
     */
    private void installPaletteOverride() {
        // Sanity check: log if a previous test in the suite leaked an
        // accent constant into themeConstants. The test class that
        // runs immediately before us (DarkLightShowcaseThemeScreenshot
        // Test) does not touch the accent vocabulary, and the
        // theme-fidelity tests that precede it install the modern
        // theme via DualAppearanceBaseTest.installModernThemeIfRequest
        // ed which routes through setThemeProps -> setThemePropsImpl
        // -> themeConstants.clear(). So the expected pre-state here
        // is "no @accent-color set". Any leaked value surfaces as a
        // CN1SS:WARN line in the run log, making post-mortem
        // investigation of suite-state cross-talk much cheaper.
        String stale = UIManager.getInstance().getThemeConstant("accent-color", null);
        if (stale != null) {
            System.out.println("CN1SS:WARN:test=PaletteOverrideThemeScreenshotTest "
                    + "stale-accent-color=" + stale
                    + " (a previous test left an @accent-color constant in UIManager state)");
        }
        Hashtable override = new Hashtable();
        override.put("@accent-color", OVERRIDE_ACCENT);
        override.put("@accent-color-dark", OVERRIDE_ACCENT);
        override.put("@accent-pressed-color", OVERRIDE_ACCENT_PRESSED);
        override.put("@accent-pressed-color-dark", OVERRIDE_ACCENT_PRESSED);
        override.put("@accent-on-color", OVERRIDE_ACCENT_TEXT);
        override.put("@accent-on-color-dark", OVERRIDE_ACCENT_TEXT);
        // iOS-only: retunes the disabled RaisedButton's bg and the
        // disabled Button.fgColor away from the platform accent-
        // disabled blue. Without this slot, iOS captures resolve to
        // byte-identical pixels as the pre-binding baseline (every
        // visible widget on the form happens to bind to accent-color
        // / accent-on-color, both of which still produce the same
        // magenta the old per-UIID override forced). Including a
        // unique colour here makes the iOS diff vs baseline
        // unambiguous and proves the runtime binding fires on iOS too.
        // Android Material 3 leaves disabled-state colours hard-coded
        // in CSS, so this constant has no Android effect (and that's
        // fine - Android's accent-container-color override below
        // produces its own visible diff there).
        override.put("@accent-disabled-color", OVERRIDE_ACCENT_DISABLED);
        override.put("@accent-disabled-color-dark", OVERRIDE_ACCENT_DISABLED);
        // Material 3 RaisedButton uses the "container" tonal pair; iOS
        // ignores these vars (no bindings reference them) so it's safe
        // to set them unconditionally for both platforms.
        override.put("@accent-container-color", OVERRIDE_ACCENT);
        override.put("@accent-container-color-dark", OVERRIDE_ACCENT);
        override.put("@accent-on-container-color", OVERRIDE_ACCENT_TEXT);
        override.put("@accent-on-container-color-dark", OVERRIDE_ACCENT_TEXT);
        UIManager.getInstance().addThemeProps(override);
    }
}
