package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

class MultipleGradientPaintTest extends UITestBase {

    private static class SimpleGradientPaint extends MultipleGradientPaint {
        SimpleGradientPaint(float[] fractions, int[] colors) {
            super(fractions, colors, CycleMethod.NO_CYCLE, ColorSpaceType.SRGB, null);
        }

        @Override
        public void paint(Graphics g, Rectangle2D bounds) {
        }

        @Override
        public void paint(Graphics g, double x, double y, double w, double h) {
        }
    }

    @FormTest
    void testGettersAndSetters() {
        float[] fractions = new float[]{0f, 1f};
        int[] colors = new int[]{0xff0000, 0x00ff00};
        SimpleGradientPaint paint = new SimpleGradientPaint(fractions, colors);
        assertArrayEquals(fractions, paint.getFractions());
        assertArrayEquals(colors, paint.getColors());
        assertEquals(MultipleGradientPaint.CycleMethod.NO_CYCLE, paint.getCycleMethod());
        assertEquals(MultipleGradientPaint.ColorSpaceType.SRGB, paint.getColorSpace());

        paint.setTransparency(100);
        assertEquals(100, paint.getTransparency());

        paint.setFractions(new float[]{0f, 0.5f, 1f});
        paint.setColors(new int[]{0xffffff, 0x000000, 0x123456});
        paint.setCycleMethod(MultipleGradientPaint.CycleMethod.REPEAT);
        paint.setColorSpace(MultipleGradientPaint.ColorSpaceType.LINEAR_RGB);

        assertEquals(MultipleGradientPaint.CycleMethod.REPEAT, paint.getCycleMethod());
        assertEquals(MultipleGradientPaint.ColorSpaceType.LINEAR_RGB, paint.getColorSpace());
    }
}
