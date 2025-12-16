package com.codename1.ui.spinner;

import com.codename1.ui.Calendar;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalendarPickerTest extends UITestBase {

    @Test
    public void testTimezoneIssue() {
        TimeZone originalDefault = TimeZone.getDefault();
        try {
            // Set default timezone to UTC-8 (PST) to simulate user environment
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-08:00"));

            CalendarPicker picker = new CalendarPicker();

            // Set the picker timezone to UTC to match the target date timezone
            picker.setTimeZone(TimeZone.getTimeZone("UTC"));

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            calendar.set(2023, java.util.Calendar.FEBRUARY, 10, 0, 0, 0);
            calendar.set(java.util.Calendar.MILLISECOND, 0);
            Date target = calendar.getTime();

            picker.setValue(target);
            Date picked = (Date) picker.getValue();
            java.util.Calendar pickedCalendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            pickedCalendar.setTime(picked);

            assertEquals(calendar.get(java.util.Calendar.YEAR), pickedCalendar.get(java.util.Calendar.YEAR));
            assertEquals(calendar.get(java.util.Calendar.MONTH), pickedCalendar.get(java.util.Calendar.MONTH));
            assertEquals(calendar.get(java.util.Calendar.DAY_OF_MONTH), pickedCalendar.get(java.util.Calendar.DAY_OF_MONTH));

        } finally {
            TimeZone.setDefault(originalDefault);
        }
    }
}
