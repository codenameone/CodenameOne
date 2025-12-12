package com.codename1.ui.geom;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class PointTest extends UITestBase {

    @FormTest
    public void testPoint() {
        Point p = new Point(10, 20);
        Assertions.assertEquals(10, p.getX());
        Assertions.assertEquals(20, p.getY());

        p.setX(30);
        p.setY(40);

        Assertions.assertEquals(30, p.getX());
        Assertions.assertEquals(40, p.getY());

        Assertions.assertEquals("30, 40", p.toString());
    }
}
