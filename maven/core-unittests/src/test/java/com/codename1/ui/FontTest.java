package com.codename1.ui;

import com.codename1.test.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FontTest extends UITestBase {
    @BeforeEach
    void clearCaches() throws Exception {
        getDerivedFontCache().clear();
        setFontReturnedHeight(0f);
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        when(implementation.isNativeFontSchemeSupported()).thenReturn(true);
    }

    @AfterEach
    void resetSupportFlags() {
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        when(implementation.isNativeFontSchemeSupported()).thenReturn(true);
    }

    @Test
    void testCreateTrueTypeFontCachesByFileNameAndHeight() {
        Font first = Font.createTrueTypeFont("CustomFont", "custom.ttf");
        Font second = Font.createTrueTypeFont("CustomFont", "custom.ttf");
        assertSame(first, second);
        assertTrue(first.isTTFNativeFont());
    }

    @Test
    void testCreateTrueTypeFontRejectsInvalidFileNames() {
        assertThrows(IllegalArgumentException.class, () -> Font.createTrueTypeFont("BadFont", "path/bad.ttf"));
        assertThrows(IllegalArgumentException.class, () -> Font.createTrueTypeFont("BadFont", "badfont.otf"));
    }

    @Test
    void testCreateTrueTypeFontReturnsNullWhenNativeSchemeUnsupported() {
        when(implementation.isNativeFontSchemeSupported()).thenReturn(false);
        Font font = Font.createTrueTypeFont("native:Roboto", "native:Roboto");
        assertNull(font);
    }

    @Test
    void testCreateTrueTypeFontReturnsNullWhenLoadingFails() {
        when(implementation.loadTrueTypeFont("native:Missing", "native:Missing")).thenReturn(null);
        Font font = Font.createTrueTypeFont("native:Missing", "native:Missing");
        assertNull(font);
    }

    @Test
    void testDeriveCachesByRequestedSizeAndWeight() {
        Font base = Font.createTrueTypeFont("BaseFont", "base.ttf");
        Font derived1 = base.derive(24f, Font.STYLE_BOLD);
        Font derived2 = base.derive(24f, Font.STYLE_BOLD);
        assertSame(derived1, derived2);
        assertEquals(24f, derived1.getPixelSize(), 0.01f);
        assertTrue(derived1.isTTFNativeFont());
    }

    @Test
    void testDeriveCreatesDistinctFontsForDifferentWeights() {
        Font base = Font.createTrueTypeFont("WeightFont", "weight.ttf");
        Font plain = base.derive(18f, Font.STYLE_PLAIN);
        Font bold = base.derive(18f, Font.STYLE_BOLD);
        assertNotSame(plain, bold);
    }

    @Test
    void testStringWidthHandlesSpecialCases() {
        Font font = Font.getDefaultFont();
        assertEquals(0, font.stringWidth(null));
        assertEquals(0, font.stringWidth(""));
        assertEquals(5, font.stringWidth(" "));
        assertEquals(24, font.stringWidth("abc"));
    }

    @Test
    void testCharsWidthDelegatesToImplementation() {
        Font font = Font.getDefaultFont();
        char[] chars = new char[]{'a', 'b', 'c', 'd'};
        assertEquals(32, font.charsWidth(chars, 0, chars.length));
    }

    @Test
    void testIsTrueTypeFileSupportedDelegatesToImplementation() {
        when(implementation.isTrueTypeSupported()).thenReturn(false);
        assertFalse(Font.isTrueTypeFileSupported());
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        assertTrue(Font.isTrueTypeFileSupported());
    }

    private HashMap<String, Font> getDerivedFontCache() throws Exception {
        Field field = Font.class.getDeclaredField("derivedFontCache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, Font> cache = (HashMap<String, Font>) field.get(null);
        return cache;
    }

    private void setFontReturnedHeight(float value) throws Exception {
        Field field = Font.class.getDeclaredField("fontReturnedHeight");
        field.setAccessible(true);
        field.set(null, value);
    }
}
