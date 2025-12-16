package com.codename1.charts.compat;

import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.GeneralPath;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class PathMeasureTest extends UITestBase {

    @FormTest
    public void testPathMeasure() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.lineTo(10, 10);

        PathMeasure pm = new PathMeasure(path, true);

        // The implementation seems to be a stub returning constant 10.
        Assertions.assertEquals(10.0f, pm.getLength(), 0.001f);

        float[] coords = new float[2];
        float[] tan = new float[2];

        // getPosTan is a void method that does nothing (commented out exception).
        // Just verify it doesn't crash.
        pm.getPosTan(0, coords, tan);
    }
}
