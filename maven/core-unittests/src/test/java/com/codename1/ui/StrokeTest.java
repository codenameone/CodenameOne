package com.codename1.ui;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrokeTest extends UITestBase {

    @Test
    void testConstructorWithWidth() {
        Stroke stroke = new Stroke(5.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertNotNull(stroke);
        assertEquals(5.0f, stroke.getLineWidth(), 0.01f);
    }

    @Test
    void testGetLineWidth() {
        Stroke stroke = new Stroke(3.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1.0f);
        assertEquals(3.5f, stroke.getLineWidth(), 0.01f);
    }

    @Test
    void testGetCapStyle() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_SQUARE, Stroke.JOIN_BEVEL, 1.0f);
        assertEquals(Stroke.CAP_SQUARE, stroke.getCapStyle());
    }

    @Test
    void testGetJoinStyle() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertEquals(Stroke.JOIN_MITER, stroke.getJoinStyle());
    }

    @Test
    void testGetMiterLimit() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 10.0f);
        assertEquals(10.0f, stroke.getMiterLimit(), 0.01f);
    }

    @Test
    void testCapButtConstant() {
        assertEquals(0, Stroke.CAP_BUTT);
    }

    @Test
    void testCapRoundConstant() {
        assertEquals(1, Stroke.CAP_ROUND);
    }

    @Test
    void testCapSquareConstant() {
        assertEquals(2, Stroke.CAP_SQUARE);
    }

    @Test
    void testJoinBevelConstant() {
        assertEquals(2, Stroke.JOIN_BEVEL);
    }

    @Test
    void testJoinMiterConstant() {
        assertEquals(0, Stroke.JOIN_MITER);
    }

    @Test
    void testJoinRoundConstant() {
        assertEquals(1, Stroke.JOIN_ROUND);
    }

    @Test
    void testStrokeWithCapButt() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertEquals(Stroke.CAP_BUTT, stroke.getCapStyle());
    }

    @Test
    void testStrokeWithCapRound() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        assertEquals(Stroke.CAP_ROUND, stroke.getCapStyle());
    }

    @Test
    void testStrokeWithCapSquare() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1.0f);
        assertEquals(Stroke.CAP_SQUARE, stroke.getCapStyle());
    }

    @Test
    void testStrokeWithJoinBevel() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_BEVEL, 1.0f);
        assertEquals(Stroke.JOIN_BEVEL, stroke.getJoinStyle());
    }

    @Test
    void testStrokeWithJoinMiter() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertEquals(Stroke.JOIN_MITER, stroke.getJoinStyle());
    }

    @Test
    void testStrokeWithJoinRound() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1.0f);
        assertEquals(Stroke.JOIN_ROUND, stroke.getJoinStyle());
    }

    @Test
    void testStrokeEquals() {
        Stroke s1 = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        Stroke s2 = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        Stroke s3 = new Stroke(3.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);

        assertTrue(s1.equals(s2));
        assertFalse(s1.equals(s3));
    }

    @Test
    void testStrokeHashCode() {
        Stroke s1 = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        Stroke s2 = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);

        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void testStrokeWithZeroWidth() {
        Stroke stroke = new Stroke(0.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertEquals(0.0f, stroke.getLineWidth(), 0.01f);
    }

    @Test
    void testStrokeWithLargeWidth() {
        Stroke stroke = new Stroke(100.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        assertEquals(100.0f, stroke.getLineWidth(), 0.01f);
    }

    @Test
    void testStrokeWithLargeMiterLimit() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 100.0f);
        assertEquals(100.0f, stroke.getMiterLimit(), 0.01f);
    }

    @Test
    void testStrokeToString() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        String str = stroke.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    void testStrokeConstructorVariations() {
        Stroke s1 = new Stroke(1.0f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1.0f);
        Stroke s2 = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 2.0f);
        Stroke s3 = new Stroke(3.0f, Stroke.CAP_SQUARE, Stroke.JOIN_BEVEL, 3.0f);

        assertNotEquals(s1.getLineWidth(), s2.getLineWidth());
        assertNotEquals(s2.getLineWidth(), s3.getLineWidth());
    }

    @Test
    void testStrokeEqualsNull() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        assertFalse(stroke.equals(null));
    }

    @Test
    void testStrokeEqualsDifferentObject() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        assertFalse(stroke.equals("Not a stroke"));
    }

    @Test
    void testStrokeEqualsSelf() {
        Stroke stroke = new Stroke(2.0f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1.0f);
        assertTrue(stroke.equals(stroke));
    }
}
