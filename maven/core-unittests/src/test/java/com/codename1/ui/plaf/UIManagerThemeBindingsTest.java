package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
