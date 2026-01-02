package com.codename1.ui.spinner;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeSpinner3DTest extends UITestBase {

    @Test
    public void testDurationMode() {
        TimeSpinner3D ts = new TimeSpinner3D();
        ts.setDurationMode(true);

        Assertions.assertTrue(ts.isDurationMode(), "durationMode should be true");
        Assertions.assertFalse(ts.isShowMeridiem(), "showMeridiem should be false in duration mode");

        // Check components
        // In duration mode we expect: Hour spinner, Hour label, Minute spinner, Minute label
        // That is 4 components.

        Assertions.assertEquals(4, ts.getComponentCount(), "Should have 4 components in duration mode");

        boolean foundHoursLabel = false;
        boolean foundMinutesLabel = false;

        for (int i = 0; i < ts.getComponentCount(); i++) {
            Component c = ts.getComponentAt(i);
            if (c instanceof Label) {
                Label l = (Label) c;
                if ("hours".equals(l.getText()) || "hours".equals(l.getUIID())) {
                   if ("TimeSpinnerHoursLabel".equals(l.getUIID())) {
                       foundHoursLabel = true;
                   }
                }
                if ("minutes".equals(l.getText()) || "minutes".equals(l.getUIID())) {
                   if ("TimeSpinnerMinutesLabel".equals(l.getUIID())) {
                       foundMinutesLabel = true;
                   }
                }
            }
        }

        Assertions.assertTrue(foundHoursLabel, "Should find hours label");
        Assertions.assertTrue(foundMinutesLabel, "Should find minutes label");
    }
}
