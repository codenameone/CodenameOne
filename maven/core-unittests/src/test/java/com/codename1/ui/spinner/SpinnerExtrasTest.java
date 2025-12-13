package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

class SpinnerExtrasTest extends UITestBase {

    @FormTest
    void testDateTimeSpinner() {
        DateTimeSpinner spinner = new DateTimeSpinner();
        spinner.setCurrentDate(new java.util.Date());
        assertNotNull(spinner.getCurrentDate());
    }

    @FormTest
    void testPickerRunnables() {
        Form f = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_STRINGS);
        p.setStrings("A", "B", "C");
        p.setSelectedString("B");
        f.add(p);
        f.show();

        // Trigger generic interactions
        // Note: showing interaction dialog might block or fail if not handled by test implementation correctly,
        // but we want to exercise the code.
        // In headless mode, showInteractionDialog might throw exception or just work if using Lightweight mode.
        p.setUseLightweightPopup(true);

        p.pressed();
        p.released();
    }
}
