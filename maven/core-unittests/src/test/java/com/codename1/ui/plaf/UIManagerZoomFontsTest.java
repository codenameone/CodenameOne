package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Font;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UIManagerZoomFontsTest extends UITestBase {
    @Test
    public void testZoomInScalesTTFFont() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(25f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(1.2f);

        Font scaled = manager.getComponentStyle("Button").getFont();
        assertEquals(30f, scaled.getPixelSize(), 0.01f);
    }

    @Test
    public void testZoomOutScalesTTFFont() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(25f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Label.font", baseFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(0.8f);

        Font scaled = manager.getComponentStyle("Label").getFont();
        assertEquals(20f, scaled.getPixelSize(), 0.01f);
    }

    @Test
    public void testZoomSkipsSystemFonts() {
        UIManager manager = UIManager.getInstance();

        Font sysFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        assertFalse(sysFont.isTTFNativeFont(), "precondition: createSystemFont must return a non-TTF font");
        Hashtable theme = new Hashtable();
        theme.put("Title.font", sysFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(1.5f);

        Object stored = manager.getThemeProps().get("Title.font");
        assertSame(sysFont, stored, "system fonts stored in the theme must be left untouched by zoomFonts");
        Font afterZoom = manager.getComponentStyle("Title").getFont();
        assertNotNull(afterZoom);
        assertFalse(afterZoom.isTTFNativeFont(), "resolved Title font must still be a non-scalable system font");
    }

    @Test
    public void testZoomCompoundsOnRepeatedCalls() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(10f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(1.2f);
        manager.zoomFonts(1.5f);

        Font scaled = manager.getComponentStyle("Button").getFont();
        assertEquals(18f, scaled.getPixelSize(), 0.01f);
    }

    @Test
    public void testReciprocalZoomRestoresOriginalSize() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(18f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(1.25f);
        manager.zoomFonts(1f / 1.25f);

        Font restored = manager.getComponentStyle("Button").getFont();
        assertEquals(18f, restored.getPixelSize(), 0.01f);
    }

    @Test
    public void testZoomFactorOfOneIsNoOp() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(14f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("Button.font", baseFont);
        manager.setThemeProps(theme);

        Object before = manager.getThemeProps().get("Button.font");
        manager.zoomFonts(1f);
        Object after = manager.getThemeProps().get("Button.font");

        assertSame(before, after);
    }

    @Test
    public void testZoomRejectsNonPositiveFactor() {
        UIManager manager = UIManager.getInstance();
        assertThrows(IllegalArgumentException.class, () -> manager.zoomFonts(0f));
        assertThrows(IllegalArgumentException.class, () -> manager.zoomFonts(-0.5f));
    }

    @Test
    public void testZoomScalesDefaultStyleFont() {
        UIManager manager = UIManager.getInstance();

        Font baseFont = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(14f, Font.STYLE_PLAIN);
        Hashtable theme = new Hashtable();
        theme.put("font", baseFont);
        manager.setThemeProps(theme);

        manager.zoomFonts(1.5f);

        Font scaled = manager.getComponentStyle("").getFont();
        assertEquals(21f, scaled.getPixelSize(), 0.01f);
    }
}
