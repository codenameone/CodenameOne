package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringPickerSampleTest extends UITestBase {

    @FormTest
    public void testStringPickerToggleAndRange() {
        Form form = new Form("Hi World", BoxLayout.y());

        final Picker languagePicker = new Picker();
        languagePicker.setType(Display.PICKER_TYPE_STRINGS);
        languagePicker.setStrings("Italian", "English", "German");
        languagePicker.setSelectedString("English");
        final AtomicInteger actionCount = new AtomicInteger();
        languagePicker.addActionListener(e -> actionCount.incrementAndGet());

        final Picker datePicker = new Picker();
        datePicker.setType(Display.PICKER_TYPE_DATE);
        final Picker timePicker = new Picker();
        timePicker.setType(Display.PICKER_TYPE_TIME);
        final Picker dateTimePicker = new Picker();
        dateTimePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);

        final Picker rangePicker = new Picker();
        rangePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        rangePicker.setUseLightweightPopup(true);
        Date startDate = newDate(119, 6, 7);
        Date endDate = newDate(119, 6, 12);
        rangePicker.setStartDate(startDate);
        rangePicker.setEndDate(endDate);
        rangePicker.setHourRange(8, 11);
        rangePicker.setDate(newDate(119, 6, 8, 9));

        final CheckBox lightweight = new CheckBox("LightWeight");
        lightweight.setSelected(languagePicker.isUseLightweightPopup());
        lightweight.addActionListener(e -> {
            boolean selected = lightweight.isSelected();
            languagePicker.setUseLightweightPopup(selected);
            datePicker.setUseLightweightPopup(selected);
            timePicker.setUseLightweightPopup(selected);
            dateTimePicker.setUseLightweightPopup(selected);
            rangePicker.setUseLightweightPopup(selected);
        });

        form.add(languagePicker);
        form.add(datePicker);
        form.add(timePicker);
        form.add(lightweight);
        form.add(dateTimePicker);
        form.add(rangePicker);
        form.show();
        DisplayTest.flushEdt();
        flushSerialCalls();
        ensureSized(form, lightweight);
        ensureSized(form, languagePicker);
        ensureSized(form, rangePicker);

        lightweight.setSelected(false);
        toggleWithPointer(lightweight);
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertTrue(lightweight.isSelected(), "Checkbox toggle should change selection");
        assertTrue(languagePicker.isUseLightweightPopup(), "Toggled picker should use lightweight popup");
        assertTrue(datePicker.isUseLightweightPopup(), "Date picker should sync lightweight state");
        assertTrue(timePicker.isUseLightweightPopup(), "Time picker should sync lightweight state");
        assertTrue(dateTimePicker.isUseLightweightPopup(), "Datetime picker should sync lightweight state");
        assertTrue(rangePicker.isUseLightweightPopup(), "Range picker should sync lightweight state");

        tap(languagePicker);
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals(1, actionCount.get(), "Pointer tap should fire action listener on picker");
        assertEquals(startDate, rangePicker.getStartDate(), "Start date should remain configured");
        assertEquals(endDate, rangePicker.getEndDate(), "End date should remain configured");
        assertEquals(8, rangePicker.getMinHour());
        assertEquals(11, rangePicker.getMaxHour());
    }

    private void toggleWithPointer(CheckBox checkBox) {
        tap(checkBox);
    }

    private void tap(com.codename1.ui.Component component) {
        int x = component.getAbsoluteX() + component.getWidth() / 2;
        int y = component.getAbsoluteY() + component.getHeight() / 2;
        implementation.dispatchPointerPressAndRelease(x, y);
    }

    private void ensureSized(Form form, com.codename1.ui.Component component) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            DisplayTest.flushEdt();
            flushSerialCalls();
        }
    }

    private Date newDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year + 1900);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    private Date newDate(int year, int month, int day, int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year + 1900);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }
}
