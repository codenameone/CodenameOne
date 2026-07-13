package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the runtime theme-binding pass installed in
 * {@link UIManager#buildTheme} resolves `@cn1-bind:&lt;themeKey&gt;=&lt;varname&gt;`
 * entries against the live theme constants and overlays the resulting
 * value onto the bound theme key.
 *
 * The CSS compiler emits the binding entries when it expands a
 * `var(--name, fallback)` reference - the fallback is inlined as the
 * baked-in default and the binding survives in the .res file as a
 * `@cn1-bind:Button.fgColor=name` constant. At app launch the user
 * passes {@link UIManager#addThemeProps(Hashtable)} a single
 * `@accent-color` override and every bound UIID picks it up without
 * having to redeclare per-UIID rules.
 */
public class UIManagerThemeBindingsTest extends UITestBase {

    @Test
    public void boundThemeKeyKeepsDefaultWithoutOverride() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");

        UIManager.getInstance().setThemeProps(theme);

        Button b = new Button("default");
        b.setUIID("Button");
        assertEquals(0x007aff, b.getUnselectedStyle().getFgColor());
    }

    @Test
    public void boundThemeKeyPicksUpAccentOverride() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        theme.put("RaisedButton.bgColor", "007aff");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");
        theme.put("@cn1-bind:RaisedButton.bgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(override);

        Button b = new Button("themed");
        b.setUIID("Button");
        assertEquals(0xff2d95, b.getUnselectedStyle().getFgColor());

        Button raised = new Button("raised");
        raised.setUIID("RaisedButton");
        assertEquals(0xff2d95, raised.getUnselectedStyle().getBgColor());
    }

    @Test
    public void boundBackgroundUpdatesLegacyRoundBorderWithoutChangingGeometryMode() {
        Hashtable theme = new Hashtable();
        RoundBorder border = RoundBorder.create().rectangle(false).color(0x007aff);
        theme.put("IconButton.bgColor", "007aff");
        theme.put("IconButton.border", border);
        theme.put("@cn1-bind:IconButton.bgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(override);

        Button icon = new Button();
        icon.setUIID("IconButton");
        RoundBorder rebound = (RoundBorder) icon.getUnselectedStyle().getBorder();
        assertEquals(0xff2d95, rebound.getColor());
        assertFalse(rebound.isRectangle(), "A circular border must remain circular after palette rebinding");
        assertFalse(rebound.getUIID(), "Palette rebinding must not opt into UIID painter mode");
    }

    @Test
    public void overrideAcceptsHashPrefixAndCaseVariants() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "#FF2D95");
        UIManager.getInstance().addThemeProps(override);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0xff2d95, b.getUnselectedStyle().getFgColor());
    }

    @Test
    public void shorthand3DigitOverrideExpandsTo6Digits() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "#f0a");
        UIManager.getInstance().addThemeProps(override);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0xff00aa, b.getUnselectedStyle().getFgColor());
    }

    @Test
    public void unboundThemeKeyIsNotMaterializedFromOverride() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        // Stale binding referencing a key the theme does not actually
        // emit - the runtime must NOT create RaisedButton.fgColor out of
        // thin air when the user supplies an @accent-color override.
        theme.put("@cn1-bind:RaisedButton.fgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(override);

        // Button stays at the baked-in default (no binding).
        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0x007aff, b.getUnselectedStyle().getFgColor());
    }

    @Test
    public void invalidColorOverrideLeavesDefaultIntact() {
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "007aff");
        theme.put("@cn1-bind:Button.fgColor", "accent-color");
        UIManager.getInstance().setThemeProps(theme);

        Hashtable override = new Hashtable();
        override.put("@accent-color", "not-a-color");
        UIManager.getInstance().addThemeProps(override);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0x007aff, b.getUnselectedStyle().getFgColor());
    }

    /// Simulates CSSWatcher's live-reload sequence: an initial theme load
    /// (`setThemeProps`) followed by `addThemeProps` carrying a fresh
    /// theme.res whose source CSS has dropped a `var()` reference in favor of
    /// a literal. The reloaded Hashtable contains the new literal value for
    /// `Button.fgColor` but no `@cn1-bind:Button.fgColor` entry, because the
    /// CSS compiler only emits bindings for properties that still reference a
    /// `var()`. The stale binding left in `themeConstants` from the first
    /// load must not stomp the user's new literal value.
    @Test
    public void cssReloadDropsStaleBindingWhenRuleBecomesLiteral() {
        Hashtable initial = new Hashtable();
        initial.put("Button.fgColor", "ff0000");
        initial.put("@cn1-bind:Button.fgColor", "accent-color");
        initial.put("@accent-color", "ff0000");
        UIManager.getInstance().setThemeProps(initial);

        Hashtable reloaded = new Hashtable();
        reloaded.put("Button.fgColor", "0000ff");
        UIManager.getInstance().addThemeProps(reloaded);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0x0000ff, b.getUnselectedStyle().getFgColor());
    }

    /// Companion to [#cssReloadDropsStaleBindingWhenRuleBecomesLiteral]: when
    /// the reload Hashtable carries BOTH the property and a fresh binding,
    /// the binding still applies. This guards against an over-eager fix that
    /// would drop bindings every time a style key shows up in the reload.
    @Test
    public void cssReloadKeepsBindingWhenStillEmittedTogether() {
        Hashtable initial = new Hashtable();
        initial.put("Button.fgColor", "ff0000");
        initial.put("@cn1-bind:Button.fgColor", "accent-color");
        initial.put("@accent-color", "ff0000");
        UIManager.getInstance().setThemeProps(initial);

        Hashtable reloaded = new Hashtable();
        reloaded.put("Button.fgColor", "0000ff");
        reloaded.put("@cn1-bind:Button.fgColor", "accent-color");
        reloaded.put("@accent-color", "00ff00");
        UIManager.getInstance().addThemeProps(reloaded);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0x00ff00, b.getUnselectedStyle().getFgColor());
    }

    /// A pure override Hashtable (no style keys, only a single `@varname`
    /// constant) must not invalidate the existing bindings. This is the
    /// canonical "user rebrands the accent" call path and the existing
    /// [#boundThemeKeyPicksUpAccentOverride] covers a single hop; this test
    /// adds a follow-up override to make sure repeated retunes keep working.
    @Test
    public void overrideOnlyReloadKeepsBindings() {
        Hashtable initial = new Hashtable();
        initial.put("Button.fgColor", "007aff");
        initial.put("@cn1-bind:Button.fgColor", "accent-color");
        UIManager.getInstance().setThemeProps(initial);

        Hashtable firstOverride = new Hashtable();
        firstOverride.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(firstOverride);

        Hashtable secondOverride = new Hashtable();
        secondOverride.put("@accent-color", "00aa66");
        UIManager.getInstance().addThemeProps(secondOverride);

        Button b = new Button();
        b.setUIID("Button");
        assertEquals(0x00aa66, b.getUnselectedStyle().getFgColor());
    }
}
