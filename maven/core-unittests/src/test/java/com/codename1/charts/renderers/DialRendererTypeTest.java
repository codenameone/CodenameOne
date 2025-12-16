package com.codename1.charts.renderers;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class DialRendererTypeTest extends UITestBase {

    @FormTest
    public void testEnum() {
        Assertions.assertEquals(DialRenderer.Type.NEEDLE, DialRenderer.Type.valueOf("NEEDLE"));
        Assertions.assertEquals(DialRenderer.Type.ARROW, DialRenderer.Type.valueOf("ARROW"));
        Assertions.assertEquals(2, DialRenderer.Type.values().length);
    }
}
