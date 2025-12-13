package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;

public class Picker3Test extends UITestBase {

    @FormTest
    public void testPickerInputDeviceClose() {
        Form f = new Form("Picker Test", new BoxLayout(BoxLayout.Y_AXIS));
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_STRINGS);
        p.setStrings("A", "B", "C");
        p.setUseLightweightPopup(true);

        f.add(p);
        f.show();

        // Trigger editing
        p.pressed();
        p.released();

        DisplayTest.flushEdt();

        if (p.isEditing()) {
            final boolean[] called = new boolean[1];
            p.stopEditing(new Runnable() {
                public void run() {
                    called[0] = true;
                }
            });
            f.getAnimationManager().flushAnimation(new Runnable() { public void run() {} });
            DisplayTest.flushEdt();
        }
    }
}
