package com.codename1.properties;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class UiBindingConvertersTest extends UITestBase {

    @FormTest
    public void testIntegerConverter() {
        UiBinding.IntegerConverter converter = new UiBinding.IntegerConverter();
        Assertions.assertNull(converter.convert(null));
        Assertions.assertEquals(123, converter.convert(123));
        Assertions.assertEquals(123, converter.convert("123"));
        Assertions.assertEquals(123, converter.convert(123.45));
    }

    @FormTest
    public void testLongConverter() {
        UiBinding.LongConverter converter = new UiBinding.LongConverter();
        Assertions.assertNull(converter.convert(null));
        Assertions.assertEquals(123L, converter.convert(123L));
        Assertions.assertEquals(123L, converter.convert("123"));
        Assertions.assertEquals(123L, converter.convert(123.45));
    }

    @FormTest
    public void testFloatConverter() {
        UiBinding.FloatConverter converter = new UiBinding.FloatConverter();
        Assertions.assertNull(converter.convert(null));
        Assertions.assertEquals(123.45f, (Float) converter.convert(123.45f), 0.001f);
        Assertions.assertEquals(123.45f, (Float) converter.convert("123.45"), 0.001f);
    }

    @FormTest
    public void testDoubleConverter() {
        UiBinding.DoubleConverter converter = new UiBinding.DoubleConverter();
        Assertions.assertNull(converter.convert(null));
        Assertions.assertEquals(123.45, (Double) converter.convert(123.45), 0.001);
        Assertions.assertEquals(123.45, (Double) converter.convert("123.45"), 0.001);
    }
}
