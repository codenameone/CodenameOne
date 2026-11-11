package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.scheduling.TimeMath;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.spinner.MaterialDatePicker;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class MaterialDatePickerThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "MaterialDatePickerTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        MaterialDatePicker picker = new MaterialDatePicker();
        picker.setFirstDayOfWeek(Calendar.MONDAY);
        // Noon on 2026-03-18 where the device is, so it is that date in
        // every zone; a month in the past has no "today" to mark.
        picker.setDate(new Date(TimeMath.at(SchedulingFixtures.MONDAY + 9, 12 * 60,
                ZoneId.systemDefault())));
        form.add(BorderLayout.NORTH, picker);
    }
}
