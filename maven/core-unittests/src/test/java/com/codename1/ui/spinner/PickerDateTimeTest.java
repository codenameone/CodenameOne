package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class PickerDateTimeTest extends UITestBase {

    @FormTest
    void pickerRetainsDateAndTimeAfterProgrammaticSelection() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2020);
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.DAY_OF_MONTH, 3);
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date target = calendar.getTime();

        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        picker.setUseLightweightPopup(true);
        picker.setDate(target);

        Form form = new Form(BoxLayout.y());
        form.add(picker);
        form.show();

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.tapComponent(picker);

        if (Display.getInstance().getCurrent() instanceof Dialog) {
            ((Dialog) Display.getInstance().getCurrent()).dispose();
        }

        Date chosen = picker.getDate();
        assertNotNull(chosen, "Picker should expose a selected date");

        Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        picked.setTime(chosen);

        assertEquals(calendar.get(Calendar.YEAR), picked.get(Calendar.YEAR));
        assertEquals(calendar.get(Calendar.MONTH), picked.get(Calendar.MONTH));
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), picked.get(Calendar.DAY_OF_MONTH));
        assertEquals(calendar.get(Calendar.HOUR_OF_DAY), picked.get(Calendar.HOUR_OF_DAY));
        assertEquals(calendar.get(Calendar.MINUTE), picked.get(Calendar.MINUTE));
    }
}
