package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/// Pure-Java repro for the Picker lightweight popup on the JS port. Opens a
/// date Picker in lightweight mode at a fixed date so the three Spinner3D
/// wheels (month / day / year) are visible, then screenshots after the
/// popup has had time to settle. A regression guard against the class of
/// Scene-graph render bugs that collapsed the wheels into invisible no-ops
/// (isTablet misclassification, JSO-method dispatch) while the full
/// LightweightPickerButtons test is noisier to iterate on.
public class PickerIsolationScreenshotTest extends BaseTest {
    private Picker picker;

    @Override
    public boolean runTest() {
        Form form = createForm("Picker Isolation", BoxLayout.y(), "picker-isolation");
        Date fixed = toDate(LocalDate.of(2026, 4, 11));
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(fixed);
        form.add(picker);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        picker.startEditingAsync();
        UITimer.timer(1200, false, parent, run);
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
