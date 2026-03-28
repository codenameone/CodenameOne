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
}
