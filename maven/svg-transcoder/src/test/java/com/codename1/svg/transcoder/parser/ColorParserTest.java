package com.codename1.svg.transcoder.parser;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorParserTest {

    @Test
    public void hex3() {
        assertEquals(0xFFAABBCC, ColorParser.parse("#abc"));
    }

    @Test
    public void hex6() {
        assertEquals(0xFF112233, ColorParser.parse("#112233"));
    }

    @Test
    public void hex8WithAlpha() {
        // SVG-style #RRGGBBAA — alpha is last
        int v = ColorParser.parse("#11223380");
        assertEquals(0x80, (v >>> 24) & 0xFF);
        assertEquals(0x11, (v >>> 16) & 0xFF);
    }

    @Test
    public void namedColor() {
        assertEquals(0xFFFF0000, ColorParser.parse("red"));
        assertEquals(0xFF000000, ColorParser.parse("black"));
        assertEquals(0xFFFFFFFF, ColorParser.parse("WHITE"));
    }

    @Test
    public void rgbFunction() {
        assertEquals(0xFF80A0C0, ColorParser.parse("rgb(128,160,192)"));
    }

    @Test
    public void rgbaFunction() {
        int v = ColorParser.parse("rgba(255,128,0,0.5)");
        // alpha is round(0.5 * 255) == 128
        assertEquals(128, (v >>> 24) & 0xFF);
        assertEquals(255, (v >>> 16) & 0xFF);
    }

    @Test
    public void rgbWithPercent() {
        int v = ColorParser.parse("rgb(100%,0%,0%)");
        assertEquals(0xFF, (v >>> 16) & 0xFF);
        assertEquals(0x00, (v >>> 8) & 0xFF);
    }

    @Test
    public void noneRecognized() {
        assertTrue(ColorParser.isNone("none"));
        assertTrue(ColorParser.isNone(" NONE "));
        assertFalse(ColorParser.isNone("red"));
    }

    @Test
    public void currentColorRecognized() {
        assertTrue(ColorParser.isCurrentColor("currentColor"));
        assertFalse(ColorParser.isCurrentColor("red"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownColorThrows() {
        ColorParser.parse("definitely-not-a-color");
    }

    @Test
    public void parseOrDefaultReturnsFallback() {
        assertEquals(0xDEADBEEF, ColorParser.parseOrDefault("nope", 0xDEADBEEF));
    }
}
