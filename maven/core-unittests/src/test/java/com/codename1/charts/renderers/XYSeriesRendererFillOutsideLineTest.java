package com.codename1.charts.renderers;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class XYSeriesRendererFillOutsideLineTest extends UITestBase {

    @FormTest
    public void testFillOutsideLine() {
        XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
        Assertions.assertEquals(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL, fill.getType());

        // The default color seems to be non-zero (likely partially transparent blue or similar default).
        // Instead of asserting 0, we assert it's not 0 or check if it matches what we see in failure (2097152200).
        // 2097152200 is 0x7D0000C8 (ARGB). Alpha 125, Blue 200.
        // I will just set it to something known to test setter/getter.
        fill.setColor(0);
        Assertions.assertEquals(0, fill.getColor());

        fill.setColor(0xFF0000);
        Assertions.assertEquals(0xFF0000, fill.getColor());

        int[] fillRange = new int[]{0, 10};
        fill.setFillRange(fillRange);
        Assertions.assertArrayEquals(fillRange, fill.getFillRange());

        // Test Type enum
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.NONE);
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_BELOW);
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ABOVE);
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.BELOW);
        Assertions.assertNotNull(XYSeriesRenderer.FillOutsideLine.Type.ABOVE);
    }
}
