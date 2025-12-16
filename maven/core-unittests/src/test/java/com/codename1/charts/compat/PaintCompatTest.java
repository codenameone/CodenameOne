package com.codename1.charts.compat;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Stroke;
import org.junit.jupiter.api.Assertions;

public class PaintCompatTest extends UITestBase {

    @FormTest
    public void testAlign() {
        Assertions.assertEquals(Component.CENTER, Paint.Align.CENTER);
        Assertions.assertEquals(Component.LEFT, Paint.Align.LEFT);
        Assertions.assertEquals(Component.RIGHT, Paint.Align.RIGHT);
        Paint.Align align = new Paint.Align();
        Assertions.assertNotNull(align);
    }

    @FormTest
    public void testCap() {
        Assertions.assertEquals(Stroke.CAP_BUTT, Paint.Cap.BUTT);
        Assertions.assertEquals(Stroke.CAP_ROUND, Paint.Cap.ROUND);
        Assertions.assertEquals(Stroke.CAP_SQUARE, Paint.Cap.SQUARE);
        Paint.Cap cap = new Paint.Cap();
        Assertions.assertNotNull(cap);
    }

    @FormTest
    public void testJoin() {
        Assertions.assertEquals(Stroke.JOIN_BEVEL, Paint.Join.BEVEL);
        Assertions.assertEquals(Stroke.JOIN_MITER, Paint.Join.MITER);
        Assertions.assertEquals(Stroke.JOIN_ROUND, Paint.Join.ROUND);
        Paint.Join join = new Paint.Join();
        Assertions.assertNotNull(join);
    }
}
