package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.spinner.Picker;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class PickerComponentTest extends UITestBase {

    @FormTest
    void testCreateStringsPicker() {
        PickerComponent pickerComponent = PickerComponent.createStrings("One", "Two");
        Picker picker = pickerComponent.getPicker();
        assertEquals(Display.PICKER_TYPE_STRINGS, picker.getType());
        assertArrayEquals(new String[]{"One", "Two"}, picker.getStrings());
        assertEquals("One", picker.getSelectedString());
    }

    @FormTest
    void testCreateDatePicker() {
        Date now = new Date();
        PickerComponent pickerComponent = PickerComponent.createDate(now);
        Picker picker = pickerComponent.getPicker();
        assertEquals(Display.PICKER_TYPE_DATE, picker.getType());
        assertEquals(now, picker.getDate());
    }

    @FormTest
    void testCreateTimePicker() {
        PickerComponent pickerComponent = PickerComponent.createTime(90);
        Picker picker = pickerComponent.getPicker();
        assertEquals(Display.PICKER_TYPE_TIME, picker.getType());
        assertEquals(90, picker.getTime());
    }

    @FormTest
    void testDurationPickers() {
        PickerComponent minutes = PickerComponent.createDurationMinutes(60000);
        assertEquals(Display.PICKER_TYPE_DURATION_MINUTES, minutes.getPicker().getType());

        PickerComponent hoursMinutes = PickerComponent.createDurationHoursMinutes(1, 30);
        assertEquals(Display.PICKER_TYPE_DURATION_HOURS, hoursMinutes.getPicker().getType());
    }

    @FormTest
    void testMethodChaining() {
        PickerComponent pickerComponent = PickerComponent.createStrings("A", "B");
        assertSame(pickerComponent, pickerComponent.label("Label"));
        assertSame(pickerComponent, pickerComponent.errorMessage("Error"));
        assertSame(pickerComponent, pickerComponent.onTopMode(true));
    }
}
