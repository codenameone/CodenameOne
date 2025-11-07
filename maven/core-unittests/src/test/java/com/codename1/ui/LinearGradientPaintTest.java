package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

class LinearGradientPaintTest extends UITestBase {

    @FormTest
    void testGradientPropertiesAndPainting() {
        float[] fractions = new float[]{0f, 0.5f, 1f};
        int[] colors = new int[]{0xff0000, 0x00ff00, 0x0000ff};
        LinearGradientPaint paint = new LinearGradientPaint(0f, 0f, 10f, 10f, fractions, colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                null);

        assertArrayEquals(fractions, paint.getFractions());
        assertArrayEquals(colors, paint.getColors());
        assertEquals(MultipleGradientPaint.CycleMethod.NO_CYCLE, paint.getCycleMethod());
        assertEquals(MultipleGradientPaint.ColorSpaceType.SRGB, paint.getColorSpace());
        assertEquals(255, paint.getTransparency());

        paint.setTransparency(128);
        assertEquals(128, paint.getTransparency());

        float[] newFractions = new float[]{0f, 1f};
        int[] newColors = new int[]{0xffffff, 0x000000};
        paint.setFractions(newFractions);
        paint.setColors(newColors);
        paint.setCycleMethod(MultipleGradientPaint.CycleMethod.REFLECT);
        paint.setColorSpace(MultipleGradientPaint.ColorSpaceType.LINEAR_RGB);

        assertArrayEquals(newFractions, paint.getFractions());
        assertArrayEquals(newColors, paint.getColors());
        assertEquals(MultipleGradientPaint.CycleMethod.REFLECT, paint.getCycleMethod());
        assertEquals(MultipleGradientPaint.ColorSpaceType.LINEAR_RGB, paint.getColorSpace());

        Image img = Image.createImage(20, 20);
        Graphics g = img.getGraphics();
        g.setPaint(paint);
        paint.paint(g, new Rectangle2D(0, 0, 20, 20));
        paint.paint(g, 0, 0, 20, 20);
    }
}
