package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Font;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UIManagerLargeTextScaleTest extends UITestBase {
    @Test
    public void testLargeTextScaleFlagAppliesToThemeFonts() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(true);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(20f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Button").getFont();
        assertEquals(30f, scaledFont.getPixelSize(), 0.01f);
    }

    @Test
    public void testThemeConstantEnablesLargeTextScaling() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.25f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(false);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(16f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("@useLargerTextScaleBool", "true");
        theme.put("Label.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Label").getFont();
        assertEquals(20f, scaledFont.getPixelSize(), 0.01f);
    }

    @Test
    public void testLargeTextScaleDisabledByDefault() {
        TestCodenameOneImplementation impl = implementation;
        impl.setLargerTextEnabled(true);
        impl.setLargerTextScale(1.5f);

        UIManager manager = UIManager.getInstance();
        manager.setUseLargerTextScale(false);

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(12f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Title.font", baseFont);

        manager.setThemeProps(theme);

        Font scaledFont = manager.getComponentStyle("Title").getFont();
        assertEquals(12f, scaledFont.getPixelSize(), 0.01f);
    }
}
