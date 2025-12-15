package com.codename1.charts.renderers;

import com.codename1.ui.Stroke;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class BasicStrokeTest extends UITestBase {

    @FormTest
    public void testBasicStroke() {
        float[] intervals = new float[]{10, 10};
        BasicStroke stroke = new BasicStroke(Stroke.CAP_BUTT, Stroke.JOIN_MITER, 4, intervals, 1);

        Assertions.assertEquals(Stroke.CAP_BUTT, stroke.getCap());
        Assertions.assertEquals(Stroke.JOIN_MITER, stroke.getJoin());
        Assertions.assertEquals(4, stroke.getMiter(), 0.001);
        Assertions.assertArrayEquals(intervals, stroke.getIntervals());
        Assertions.assertEquals(1, stroke.getPhase(), 0.001);
    }

    @FormTest
    public void testConstants() {
        Assertions.assertNotNull(BasicStroke.SOLID);
        Assertions.assertNotNull(BasicStroke.DASHED);
        Assertions.assertNotNull(BasicStroke.DOTTED);
    }
}
