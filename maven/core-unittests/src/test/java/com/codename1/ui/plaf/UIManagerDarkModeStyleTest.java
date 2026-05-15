package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import java.util.Hashtable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UIManagerDarkModeStyleTest extends UITestBase {

    @AfterEach
    public void resetDarkMode() {
        display.setDarkMode(null);
    }

    @Test
    public void testDarkStyleIsUsedAndInheritsBaseStyle() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "112233");
        theme.put("Button.bgColor", "445566");
        theme.put("$DarkButton.bgColor", "000000");
        manager.setThemeProps(theme);

        display.setDarkMode(Boolean.TRUE);
        Style style = manager.getComponentStyle("Button");

        assertEquals(0x112233, style.getFgColor());
        assertEquals(0x000000, style.getBgColor());
    }

    @Test
    public void testDarkStyleSelectedInheritsSelectedBaseStyle() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.sel#fgColor", "00ff00");
        theme.put("$DarkButton.sel#bgColor", "101010");
        manager.setThemeProps(theme);

        display.setDarkMode(Boolean.TRUE);
        Style style = manager.getComponentSelectedStyle("Button");

        assertEquals(0x00ff00, style.getFgColor());
        assertEquals(0x101010, style.getBgColor());
    }

    @Test
    public void testDarkStyleCanOverrideImplicitInheritanceWithExplicitDerive() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "ff0000");
        theme.put("Label.fgColor", "0000ff");
        theme.put("$DarkButton.derive", "Label");
        theme.put("$DarkButton.bgColor", "202020");
        manager.setThemeProps(theme);

        display.setDarkMode(Boolean.TRUE);
        Style style = manager.getComponentStyle("Button");

        assertEquals(0x0000ff, style.getFgColor());
        assertEquals(0x202020, style.getBgColor());
    }

    @Test
    public void testFallsBackToRegularStyleWhenNoDarkStyleExists() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "aabbcc");
        manager.setThemeProps(theme);

        display.setDarkMode(Boolean.TRUE);
        Style style = manager.getComponentStyle("Button");

        assertEquals(0xaabbcc, style.getFgColor());
    }

    /// Regression test for the simulator's Dark/Light Mode toggle. When the user flips dark mode
    /// after styles have already been resolved once (the common case: the toggle lives on a
    /// running simulator), the next style lookup must reflect the dark variant -- not the cached
    /// light style. The bug surfaced because `getComponentStyleImpl` caches results by UIID
    /// without keying the cache on dark-mode state, so without an explicit cache invalidation
    /// the cached light-mode style would win even after `Display.setDarkMode(TRUE)`. The
    /// simulator's documented escape hatch for users is `UIManager.refreshTheme()`, which
    /// clears the cache.
    @Test
    public void testRefreshThemeAppliesDarkStyleAfterCachedLightLookup() {
        UIManager manager = UIManager.getInstance();
        Hashtable theme = new Hashtable();
        theme.put("Button.fgColor", "112233");
        theme.put("Button.bgColor", "445566");
        theme.put("$DarkButton.bgColor", "000000");
        manager.setThemeProps(theme);

        display.setDarkMode(Boolean.FALSE);
        Style lightStyle = manager.getComponentStyle("Button");
        assertEquals(0x445566, lightStyle.getBgColor(),
                "Sanity: light-mode lookup populates the styles cache");

        display.setDarkMode(Boolean.TRUE);
        manager.refreshTheme();
        Style darkStyle = manager.getComponentStyle("Button");

        assertEquals(0x112233, darkStyle.getFgColor(),
                "Dark variant should inherit fgColor from the base Button rule");
        assertEquals(0x000000, darkStyle.getBgColor(),
                "Dark variant bgColor must win after refreshTheme; otherwise the simulator's"
                        + " Dark/Light toggle silently keeps the light-cached bgColor.");
    }

    /// Regression test for the simulator's Dark/Light Mode menu path. The toggle handler used
    /// to call `JavaSEPort.refreshSkin`, which itself calls `Display.installNativeTheme()`.
    /// `installNativeTheme` calls `UIManager.setThemeProps(nativeProps)`, and `setThemeProps`
    /// blows away the previously-installed theme entirely -- so a project that had loaded an
    /// app theme (CSS-generated `theme.res`) with custom font sizes, custom margins, or
    /// overridden UIID styling would, after one click on Dark/Light/Unsupported, find every
    /// app-level customization gone. Visually that read as "fonts are completely wrong" until
    /// the user re-launched the simulator.
    ///
    /// `UIManager.refreshTheme()` re-applies the *current* themeProps (so app customizations
    /// survive) and clears the style cache (so dark/light variants resolve correctly). This
    /// test pins that contract: after a refresh, app-theme keys are still effective.
    @Test
    public void testRefreshThemePreservesAppThemeCustomizationsAcrossDarkModeFlip() {
        UIManager manager = UIManager.getInstance();
        Hashtable appTheme = new Hashtable();
        // Pretend this is the CSS-generated user theme: it sets app-specific colors on Button
        // and a separate UIID the native theme would never define.
        appTheme.put("Button.fgColor", "ff8800");
        appTheme.put("Button.bgColor", "eeeeee");
        appTheme.put("$DarkButton.bgColor", "111111");
        appTheme.put("AppOnlyLabel.fgColor", "abcdef");
        manager.setThemeProps(appTheme);

        display.setDarkMode(Boolean.FALSE);
        // Warm the style cache the way a real simulator session would.
        manager.getComponentStyle("Button");
        manager.getComponentStyle("AppOnlyLabel");

        display.setDarkMode(Boolean.TRUE);
        manager.refreshTheme();

        Style buttonDark = manager.getComponentStyle("Button");
        assertEquals(0xff8800, buttonDark.getFgColor(),
                "App theme's Button.fgColor must survive the dark-mode refresh");
        assertEquals(0x111111, buttonDark.getBgColor(),
                "Dark Button.bgColor override must apply after refreshTheme");

        Style appOnly = manager.getComponentStyle("AppOnlyLabel");
        assertEquals(0xabcdef, appOnly.getFgColor(),
                "App-only UIIDs not present in any native theme must still resolve after a "
                        + "dark-mode refresh; if this fails the toggle is wiping the app theme.");
    }
}
