package com.codename1.charts.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilTest {
    @Test
    void argbAndRgbProduceExpectedValues() {
        int color = ColorUtil.argb(0x12, 0x34, 0x56, 0x78);
        assertEquals(0x12345678, color);

        int rgb = ColorUtil.rgb(0xAA, 0xBB, 0xCC);
        assertEquals(0xFFAABBCC, rgb);
    }

    @Test
    void colorComponentExtractionReturnsOriginalValues() {
        int color = 0x7FAB5623;
        assertEquals(0x7F, ColorUtil.alpha(color));
        assertEquals(0xAB, ColorUtil.red(color));
        assertEquals(0x56, ColorUtil.green(color));
        assertEquals(0x23, ColorUtil.blue(color));
    }

    @Test
    void predefinedColorsMatchRgbValues() {
        assertEquals(ColorUtil.rgb(0, 0, 255), ColorUtil.BLUE);
        assertEquals(ColorUtil.rgb(0, 255, 0), ColorUtil.GREEN);
        assertEquals(ColorUtil.rgb(255, 255, 0), ColorUtil.YELLOW);
    }

    @Test
    void testTransparentInnerClass() throws Exception {
        // Access ColorUtil.IColor (private static)
        Class<?> iColorClass = null;
        for (Class<?> cls : ColorUtil.class.getDeclaredClasses()) {
            if (cls.getSimpleName().equals("IColor")) {
                iColorClass = cls;
                break;
            }
        }
        assertNotNull(iColorClass, "IColor inner class not found");

        // Access ColorUtil.IColor.Transparent (public static)
        Class<?> transparentClass = null;
        for (Class<?> cls : iColorClass.getDeclaredClasses()) {
            if (cls.getSimpleName().equals("Transparent")) {
                transparentClass = cls;
                break;
            }
        }
        assertNotNull(transparentClass, "Transparent inner class not found");

        // Instantiate Transparent via reflection (constructor is public but class is inside private class, so usually okay if static?)
        // Actually since Transparent is public static inside private static, it is accessible if we know the name?
        // No, direct access via source fails if outer is private.

        Constructor<?> ctor = transparentClass.getConstructor(int.class, int.class, int.class);
        Object transparentObj = ctor.newInstance(255, 0, 0); // Red

        // Check fields via reflection
        // IColor fields: alpha, red, green, blue
        Field alphaField = iColorClass.getDeclaredField("alpha");
        Field redField = iColorClass.getDeclaredField("red");
        Field greenField = iColorClass.getDeclaredField("green");
        Field blueField = iColorClass.getDeclaredField("blue");

        // Transparent constructor sets alpha to 0
        assertEquals(0, alphaField.getInt(transparentObj));
        assertEquals(255, redField.getInt(transparentObj));
        assertEquals(0, greenField.getInt(transparentObj));
        assertEquals(0, blueField.getInt(transparentObj));

        // Check static constants like Transparent.Red
        Field redConstant = transparentClass.getField("Red");
        Object redObj = redConstant.get(null);
        assertEquals(0, alphaField.getInt(redObj));
        assertEquals(255, redField.getInt(redObj));
    }
}
