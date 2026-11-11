package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.spinner.MaterialTimePicker;

public class MaterialTimePickerThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "MaterialTimePickerTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        MaterialTimePicker picker = new MaterialTimePicker();
        // Said outright, so the capture does not follow the device's setting.
        picker.setShowMeridiem(true);
        picker.setTime(9, 25);
        form.add(BorderLayout.NORTH, picker);
    }
}
