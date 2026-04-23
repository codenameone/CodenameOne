package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class PickerThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "PickerTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("String picker"));
        Picker stringPicker = new Picker();
        stringPicker.setStrings("Red", "Green", "Blue", "Yellow", "Purple");
        stringPicker.setSelectedString("Green");
        form.add(stringPicker);

        form.add(new Label("Disabled picker"));
        Picker disabled = new Picker();
        disabled.setStrings("Option 1", "Option 2");
        disabled.setSelectedString("Option 1");
        disabled.setEnabled(false);
        form.add(disabled);
    }
}
