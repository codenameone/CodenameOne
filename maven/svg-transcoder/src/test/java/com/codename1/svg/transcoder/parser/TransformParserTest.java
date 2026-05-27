package com.codename1.svg.transcoder.parser;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransformParserTest {

    @Test
    public void emptyOrNull() {
        assertNull(TransformParser.parse(null));
        assertNull(TransformParser.parse(""));
        assertNull(TransformParser.parse("   "));
    }

    @Test
    public void singleTranslate() {
        SVGTransform t = TransformParser.parse("translate(10, 20)");
        assertNotNull(t);
        assertEquals(10f, t.e, 0f);
        assertEquals(20f, t.f, 0f);
        assertEquals(1f, t.a, 0f);
        assertEquals(1f, t.d, 0f);
    }

    @Test
    public void singleScale() {
        SVGTransform t = TransformParser.parse("scale(2, 3)");
        assertNotNull(t);
        assertEquals(2f, t.a, 0f);
        assertEquals(3f, t.d, 0f);
    }

    @Test
    public void scaleUniform() {
        SVGTransform t = TransformParser.parse("scale(2)");
        assertEquals(2f, t.a, 0f);
        assertEquals(2f, t.d, 0f);
    }

    @Test
    public void rotateAt90Degrees() {
        SVGTransform t = TransformParser.parse("rotate(90)");
        // cos(90) ~ 0, sin(90) = 1
        assertEquals(0f, t.a, 1e-5f);
        assertEquals(1f, t.b, 1e-5f);
        assertEquals(-1f, t.c, 1e-5f);
        assertEquals(0f, t.d, 1e-5f);
    }

    @Test
    public void translateThenScaleAccumulates() {
        SVGTransform t = TransformParser.parse("translate(5, 5) scale(2)");
        // translate then scale: T = translate * scale
        // For a point P at (1,1): scale -> (2,2), then translate -> (7,7)
        // Matrix form: a=2 b=0 c=0 d=2 e=5 f=5
        assertEquals(2f, t.a, 0f);
        assertEquals(2f, t.d, 0f);
        assertEquals(5f, t.e, 0f);
        assertEquals(5f, t.f, 0f);
    }

    @Test
    public void matrixFunction() {
        SVGTransform t = TransformParser.parse("matrix(1 2 3 4 5 6)");
        assertEquals(1f, t.a, 0f);
        assertEquals(2f, t.b, 0f);
        assertEquals(3f, t.c, 0f);
        assertEquals(4f, t.d, 0f);
        assertEquals(5f, t.e, 0f);
        assertEquals(6f, t.f, 0f);
    }
}
