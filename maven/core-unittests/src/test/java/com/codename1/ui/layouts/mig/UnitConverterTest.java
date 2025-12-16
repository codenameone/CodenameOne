package com.codename1.ui.layouts.mig;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class UnitConverterTest extends UITestBase {

    @FormTest
    public void testConstantsAndImpl() {
        Assertions.assertEquals(-87654312, UnitConverter.UNABLE);

        UnitConverter converter = new UnitConverter() {
            @Override
            public int convertToPixels(float value, String unit, boolean isHor, float refValue, ContainerWrapper parent, ComponentWrapper comp) {
                return (int) value;
            }
        };

        Assertions.assertEquals(10, converter.convertToPixels(10.5f, "px", true, 0, null, null));
    }
}
