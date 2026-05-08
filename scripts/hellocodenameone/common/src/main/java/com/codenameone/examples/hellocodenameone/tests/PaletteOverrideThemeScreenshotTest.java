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
 * UIID quietly tracking the underlying palette variable. At app
 * launch the user calls
 * {@link UIManager#addThemeProps(Hashtable)} with a single
 * `@accent-color`-style constant per palette role and the runtime
 * binding pass overlays the override onto every bound UIID at once -
 * no per-UIID rule duplication, no theme recompile.
 *
 * This test installs a magenta override - vivid enough that a visual
 * diff against the native baseline is unmistakable - and verifies both
 * the light and dark captures pick it up. The light capture exercises
 * the light base styles; the dark capture exercises the
 * {@code $Dark<UIID>} variants which are bound to the matching
 * `-dark` palette variables.
 */
public class PaletteOverrideThemeScreenshotTest extends DualAppearanceBaseTest {

    private static final String OVERRIDE_ACCENT = "ff2d95";
    private static final String OVERRIDE_ACCENT_PRESSED = "c71a75";
    private static final String OVERRIDE_ACCENT_TEXT = "ffffff";
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
        Hashtable override = new Hashtable();
        override.put("@accent-color", OVERRIDE_ACCENT);
        override.put("@accent-color-dark", OVERRIDE_ACCENT);
        override.put("@accent-pressed-color", OVERRIDE_ACCENT_PRESSED);
        override.put("@accent-pressed-color-dark", OVERRIDE_ACCENT_PRESSED);
        override.put("@accent-on-color", OVERRIDE_ACCENT_TEXT);
        override.put("@accent-on-color-dark", OVERRIDE_ACCENT_TEXT);
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
