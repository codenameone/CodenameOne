package com.codename1.charts.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {
    @Test
    void argbAndRgbProduceExpectedValues() {
        int color = ColorUtil.argb(0x12, 0x34, 0x56, 0x78);
        assertEquals(0x12345678, color);

        int rgb = ColorUtil.rgb(0xAA, 0xBB, 0xCC);
        assertEquals(0xFFAABBCC, rgb);
    }

    @Test
    void colorComponentExtractionReturnsOriginalValues() {
        int color = 0x7FAB5623;
        assertEquals(0x7F, ColorUtil.alpha(color));
        assertEquals(0xAB, ColorUtil.red(color));
        assertEquals(0x56, ColorUtil.green(color));
        assertEquals(0x23, ColorUtil.blue(color));
    }

    @Test
    void predefinedColorsMatchRgbValues() {
        assertEquals(ColorUtil.rgb(0, 0, 255), ColorUtil.BLUE);
        assertEquals(ColorUtil.rgb(0, 255, 0), ColorUtil.GREEN);
        assertEquals(ColorUtil.rgb(255, 255, 0), ColorUtil.YELLOW);
    }
}
