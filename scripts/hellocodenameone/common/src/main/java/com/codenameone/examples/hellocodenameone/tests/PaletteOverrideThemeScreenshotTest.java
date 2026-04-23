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
 * The native CSS declares a palette (--cn1-accent, --cn1-primary etc.)
 * that's inlined into each UIID at compile time. At runtime a user app
 * overrides specific colors by layering an additional {@link Hashtable}
 * of theme props on top of the installed native theme via
 * {@link UIManager#addThemeProps}. This test installs a magenta
 * override - vivid enough that a visual diff against the native
 * baseline is unmistakable - and verifies both the light and dark
 * captures pick it up.
 *
 * The override is installed once when the suite reaches this test; the
 * light capture exercises it with the light base styles, the dark
 * capture exercises it with the base styles picking up the
 * {@code $Dark<UIID>} variants merged under the same override layer.
 * Because {@link Style#setBgColor} on an override key blows away the
 * {@code $Dark} variant for that specific key, the dark capture also
 * ends up showing the override color - proving the override reaches
 * every appearance.
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
     * theme. Uses {@link UIManager#addThemeProps} so the native theme
     * stays resident underneath - the override table only has to
     * redeclare the handful of keys it wants to change, plus the
     * matching {@code $Dark} keys so the override applies in dark
     * mode too.
     */
    private void installPaletteOverride() {
        Hashtable override = new Hashtable();
        override.put("RaisedButton.bgColor", OVERRIDE_ACCENT);
        override.put("RaisedButton.fgColor", OVERRIDE_ACCENT_TEXT);
        override.put("RaisedButton.press#bgColor", OVERRIDE_ACCENT_PRESSED);
        override.put("RaisedButton.press#fgColor", OVERRIDE_ACCENT_TEXT);
        override.put("Button.fgColor", OVERRIDE_ACCENT);
        override.put("Button.press#fgColor", OVERRIDE_ACCENT_PRESSED);
        // Dark override mirrors the light override so the magenta
        // applies across both appearances. A real user theme would
        // probably choose two variants; this test keeps them identical
        // for easy visual confirmation.
        override.put("$DarkRaisedButton.bgColor", OVERRIDE_ACCENT);
        override.put("$DarkRaisedButton.fgColor", OVERRIDE_ACCENT_TEXT);
        override.put("$DarkRaisedButton.press#bgColor", OVERRIDE_ACCENT_PRESSED);
        override.put("$DarkRaisedButton.press#fgColor", OVERRIDE_ACCENT_TEXT);
        override.put("$DarkButton.fgColor", OVERRIDE_ACCENT);
        override.put("$DarkButton.press#fgColor", OVERRIDE_ACCENT_PRESSED);
        UIManager.getInstance().addThemeProps(override);
    }
}
