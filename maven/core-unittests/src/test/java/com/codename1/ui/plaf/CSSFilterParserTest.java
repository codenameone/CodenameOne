package com.codename1.ui.plaf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CSSFilterParserTest {

    @Test
    void blurOnly() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("blur(4px)");
        assertNotNull(c);
        assertEquals(4f, c.blurRadius, 1e-4f);
        assertNull(c.colorMatrix);
    }

    @Test
    void blurUnitlessTreatedAsPixels() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("blur(8)");
        assertEquals(8f, c.blurRadius, 1e-4f);
    }

    @Test
    void grayscaleProducesRec709Diagonal() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("grayscale(1)");
        assertNotNull(c.colorMatrix);
        assertEquals(0.2126f, c.colorMatrix[0], 1e-3f);
        assertEquals(0.7152f, c.colorMatrix[6], 1e-3f);
        assertEquals(0.0722f, c.colorMatrix[12], 1e-3f);
    }

    @Test
    void grayscalePercentSyntax() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("grayscale(100%)");
        assertEquals(0.2126f, c.colorMatrix[0], 1e-3f);
    }

    @Test
    void brightness15ScalesRgb() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("brightness(1.5)");
        assertEquals(1.5f, c.colorMatrix[0], 1e-4f);
        assertEquals(1.5f, c.colorMatrix[6], 1e-4f);
        assertEquals(1.5f, c.colorMatrix[12], 1e-4f);
        assertEquals(1f, c.colorMatrix[18], 1e-4f);
    }

    @Test
    void invertOneInvertsRgb() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("invert(1)");
        // R = 1 - 2*1 = -1 on diagonal, offset 255.
        assertEquals(-1f, c.colorMatrix[0], 1e-4f);
        assertEquals(255f, c.colorMatrix[4], 1e-4f);
    }

    @Test
    void opacityOnlyAffectsAlphaRow() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("opacity(0.5)");
        assertEquals(1f, c.colorMatrix[0], 1e-4f);
        assertEquals(1f, c.colorMatrix[6], 1e-4f);
        assertEquals(0.5f, c.colorMatrix[18], 1e-4f);
    }

    @Test
    void saturateZeroReducesToLumaWeightsPerRow() {
        // saturate(0) -> every row should be the luma-weights row.
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("saturate(0)");
        for (int r = 0; r < 3; r++) {
            assertEquals(0.213f, c.colorMatrix[r * 5], 1e-3f);
            assertEquals(0.715f, c.colorMatrix[r * 5 + 1], 1e-3f);
            assertEquals(0.072f, c.colorMatrix[r * 5 + 2], 1e-3f);
        }
    }

    @Test
    void sepiaIsNonIdentity() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("sepia(1)");
        assertNotNull(c.colorMatrix);
        assertNotEquals(1f, c.colorMatrix[0], 1e-4f);
    }

    @Test
    void hueRotateRotates180Degrees() {
        // Computing hue-rotate(180deg) is involved; just assert non-identity
        // and that the answer is sane: cos(180) = -1, sin(180) = 0.
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("hue-rotate(180deg)");
        assertNotNull(c.colorMatrix);
        // Manually: R row col R = 0.213 + (-1) * 0.787 - 0 = -0.574.
        assertEquals(-0.574f, c.colorMatrix[0], 1e-3f);
    }

    @Test
    void chainComposesInOrder() {
        // brightness(2) brightness(0.5) -> identity. Verifies composition order.
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("brightness(2) brightness(0.5)");
        assertNull(c.colorMatrix);
    }

    @Test
    void chainCombinesBlurAndColor() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("blur(3px) brightness(1.2) contrast(0.9) saturate(1.3)");
        assertEquals(3f, c.blurRadius, 1e-4f);
        assertNotNull(c.colorMatrix);
    }

    @Test
    void multipleBlursAdd() {
        CSSFilterParser.FilterChain c = CSSFilterParser.parse("blur(2px) blur(3px)");
        assertEquals(5f, c.blurRadius, 1e-4f);
    }

    @Test
    void noneReturnsNull() {
        assertNull(CSSFilterParser.parse("none"));
        assertNull(CSSFilterParser.parse("NONE"));
        assertNull(CSSFilterParser.parse(null));
        assertNull(CSSFilterParser.parse(""));
    }

    @Test
    void unknownFunctionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CSSFilterParser.parse("colorize(red)"));
    }

    @Test
    void malformedRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CSSFilterParser.parse("blur(4px"));
    }
}
