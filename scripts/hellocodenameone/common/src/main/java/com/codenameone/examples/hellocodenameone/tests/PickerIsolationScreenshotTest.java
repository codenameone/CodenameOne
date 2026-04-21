package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/// Pure-Java isolation repro for the Picker date-wheel-missing bug on the JS port.
/// Shows a Picker in lightweight mode with a fixed date, triggers
/// startEditingAsync to open the popup, dumps a one-liner of what the wheel
/// should contain (list model size + selected index + row height), and snaps
/// a screenshot. Matches the minimal-repro style of SwitchIsolation/Sheet so
/// we can iterate without the full suite.
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
        UITimer.timer(1200, false, parent, () -> {
            System.out.println("CN1SS:DIAG:picker-isolation"
                    + " pickerClass=" + picker.getClass().getName()
                    + " lightweight=" + picker.isUseLightweightPopup()
                    + " type=" + picker.getType()
                    + " date=" + picker.getDate());
            run.run();
        });
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
