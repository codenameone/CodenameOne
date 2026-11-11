/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Material date and time pickers, by themselves and as what a `Picker`
/// opens when the theme asks for them.
class MaterialPickerTest extends UITestBase {
    private static Date date(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static void tap(Component c, int[] at) {
        c.pointerPressed(at[0], at[1]);
        c.pointerReleased(at[0], at[1]);
    }

    private static void place(Component c) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, c);
        form.revalidate();
    }

    @FormTest
    void theHandTurnsToATimeItWasNotDraggedTo() {
        MaterialTimePicker p = new MaterialTimePicker();
        p.setShowMeridiem(true);
        p.setTime(10, 10);
        place(p);
        p.setAnimationTime(1000);
        assertEquals(300f, p.handAt(), 0.01f, "ten o'clock");
        p.setTime(14, 10);
        p.setAnimationTime(1000);
        assertEquals(300f, p.handAt(), 0.01f, "the turn starts where the hand was");
        p.setAnimationTime(1110);
        assertEquals(0f, p.handAt() > 180 ? p.handAt() - 360 : p.handAt(), 1f,
                "halfway, by the short way round through twelve");
        p.setAnimationTime(1220);
        assertEquals(60f, p.handAt(), 0.01f, "two o'clock");
        p.setAnimationTime(-1);
    }

    @FormTest
    void aDraggedHandIsWhereTheFingerIs() {
        MaterialTimePicker p = new MaterialTimePicker();
        p.setShowMeridiem(true);
        p.setTime(10, 10);
        place(p);
        p.setAnimationTime(1000);
        assertEquals(300f, p.handAt(), 0.01f);
        int[] at = p.pointOf(2, false);
        p.dialPane().pointerPressed(at[0], at[1]);
        assertEquals(60f, p.handAt(), 0.01f, "no turn under a finger");
        p.dialPane().pointerReleased(at[0], at[1]);
        p.setAnimationTime(-1);
    }

    @FormTest
    void thePointerMarksTheNumberAndTheDayItIsOver() {
        MaterialTimePicker t = new MaterialTimePicker();
        t.setShowMeridiem(true);
        t.setTime(10, 10);
        place(t);
        int[] at = t.pointOf(7, false);
        t.dialPane().pointerHover(new int[]{at[0]}, new int[]{at[1]});
        assertEquals(7, t.overNumber());
        t.dialPane().setHovered(false);
        assertEquals(-1, t.overNumber(), "nothing is marked once the pointer has left");
        t.remove();

        MaterialDatePicker d = new MaterialDatePicker();
        d.setDate(date(2026, 0, 5, 14, 30));
        d.setDateRange(date(2026, 0, 3, 0, 0), date(2026, 0, 25, 0, 0));
        place(d);
        at = d.pointOfDay(20);
        d.dayPane().pointerHover(new int[]{at[0]}, new int[]{at[1]});
        assertEquals(20, d.overDay());
        at = d.pointOfDay(28);
        d.dayPane().pointerHover(new int[]{at[0]}, new int[]{at[1]});
        assertEquals(-1, d.overDay(), "a day that cannot be chosen is not marked");
        at = d.pointOfDay(20);
        d.dayPane().pointerHover(new int[]{at[0]}, new int[]{at[1]});
        d.dayPane().setHovered(false);
        assertEquals(-1, d.overDay());
    }

    @FormTest
    void aMonthAskedForSlidesIn() {
        MaterialDatePicker p = new MaterialDatePicker();
        p.setDate(date(2026, 0, 5, 14, 30));
        place(p);
        p.setAnimationTime(1000);
        assertEquals(1f, p.slideAt(), 0.001f, "nothing moves until a month is asked for");
        p.page(1);
        assertEquals(0f, p.slideAt(), 0.001f);
        p.setAnimationTime(1110);
        assertTrue(p.slideAt() > 0.5f && p.slideAt() < 1f, "more than half way in half the time: it slows into place");
        p.setAnimationTime(1220);
        assertEquals(1f, p.slideAt(), 0.001f);
        int[] at = p.pointOfDay(20);
        tap(p.dayPane(), at);
        assertEquals(date(2026, 1, 20, 14, 30), p.getDate(), "the month shown is the one a tap chooses in");
        p.setAnimationTime(-1);
    }

    private static final class Counter implements ActionListener {
        int count;

        @Override
        public void actionPerformed(ActionEvent evt) {
            count++;
        }
    }

    @FormTest
    void aTappedDayIsChosenAtTheTimeOfDayItHad() {
        MaterialDatePicker p = new MaterialDatePicker();
        p.setDate(date(2026, 0, 5, 14, 30));
        Counter c = new Counter();
        p.addActionListener(c);
        place(p);
        tap(p.dayPane(), p.pointOfDay(20));
        assertEquals(date(2026, 0, 20, 14, 30), p.getDate());
        assertEquals(1, c.count);
        tap(p.dayPane(), p.pointOfDay(20));
        assertEquals(1, c.count, "the day it already is tells nobody");

        p.page(1);
        assertEquals(1, p.getShownMonth());
        assertEquals(date(2026, 0, 20, 14, 30), p.getDate(), "another month shown is not another date");
        tap(p.dayPane(), p.pointOfDay(28));
        assertEquals(date(2026, 1, 28, 14, 30), p.getDate());
        p.page(-2);
        assertEquals(2025, p.getShownYear());
        assertEquals(11, p.getShownMonth());

        p.setFirstDayOfWeek(Calendar.MONDAY);
        tap(p.dayPane(), p.pointOfDay(31));
        assertEquals(date(2025, 11, 31, 14, 30), p.getDate());
        assertEquals(29, MaterialDatePicker.daysIn(2024, 1));
        assertEquals(28, MaterialDatePicker.daysIn(2100, 1));
    }

    @FormTest
    void daysOutsideTheRangeCannotBeChosen() {
        MaterialDatePicker p = new MaterialDatePicker();
        p.setDate(date(2026, 0, 15, 9, 0));
        p.setDateRange(date(2026, 0, 10, 0, 0), date(2026, 0, 20, 23, 0));
        place(p);
        tap(p.dayPane(), p.pointOfDay(5));
        assertEquals(date(2026, 0, 15, 9, 0), p.getDate());
        tap(p.dayPane(), p.pointOfDay(10));
        assertEquals(date(2026, 0, 10, 9, 0), p.getDate());
        p.page(3);
        assertEquals(0, p.getShownMonth(), "there is no month to page to outside the range");
        p.setDate(date(2026, 5, 1, 9, 0));
        assertEquals(date(2026, 0, 20, 9, 0), p.getDate(), "a date past the range is its last day");
    }

    @FormTest
    void aTypedDateIsChosenAndOneThatCannotBeReadIsSaidSo() {
        MaterialDatePicker p = new MaterialDatePicker();
        p.setDate(date(2026, 0, 5, 8, 0));
        place(p);
        p.setInputMode(true);
        assertTrue(p.isInputMode());
        assertEquals(p.typedForm(2026, 0, 5), p.inputField().getText());
        p.inputField().setText(p.typedForm(2026, 2, 9));
        assertEquals(date(2026, 2, 9, 8, 0), p.getDate());
        assertEquals("", p.errorText());
        p.inputField().setText(p.typedForm(2026, 1, 30));
        assertEquals(date(2026, 2, 9, 8, 0), p.getDate(), "there is no 30th of February");
        assertTrue(p.errorText().length() > 0);
        p.inputField().setText("12");
        assertTrue(p.errorText().length() > 0);
        assertFalse(p.acceptInput(), "text that is no date is not confirmed");
        // A keyboard of digits has no separator: eight digits are a date.
        String digits = p.typedForm(2026, 3, 14).replace("/", "").replace(".", "").replace("-", "");
        if (digits.length() == 8) {
            p.inputField().setText(digits);
            assertEquals(date(2026, 3, 14, 8, 0), p.getDate());
            assertTrue(p.acceptInput());
        }
        p.setInputMode(false);
        assertEquals(p.getDate().getTime() == date(2026, 3, 14, 8, 0).getTime() ? 3 : 2, p.getShownMonth());

        // A date outside the years on offer brings them to it.
        p.setDate(date(1850, 5, 15, 8, 0));
        p.page(1);
        assertEquals(1850, p.getShownYear());
        assertEquals(6, p.getShownMonth());
    }

    @FormTest
    void theFaceTakesAnHourThenAMinute() {
        MaterialTimePicker p = new MaterialTimePicker();
        p.setTime(9, 15);
        Counter c = new Counter();
        p.addActionListener(c);
        place(p);
        assertFalse(p.isMinutesShown());
        tap(p.dialPane(), p.pointOf(17, false));
        assertEquals(17, p.getHour(), "the inner ring is the afternoon");
        assertTrue(p.isMinutesShown(), "the hour chosen, the face turns to minutes");
        tap(p.dialPane(), p.pointOf(40, true));
        assertEquals(40, p.getMinute());
        assertEquals(Integer.valueOf(17 * 60 + 40), p.getValue());
        assertEquals(2, c.count);

        p.showMinutes(false);
        tap(p.dialPane(), p.pointOf(0, false));
        assertEquals(0, p.getHour());
        p.showMinutes(false);
        tap(p.dialPane(), p.pointOf(12, false));
        assertEquals(12, p.getHour());
        p.setValue(Integer.valueOf(23 * 60 + 59));
        assertEquals(23, p.getHour());
        assertEquals(59, p.getMinute());
    }

    @FormTest
    void twelveHoursKeepTheirHalfOfTheDayAndMinutesKeepTheirStep() {
        MaterialTimePicker p = new MaterialTimePicker();
        p.setShowMeridiem(true);
        p.setMinuteStep(15);
        p.setTime(15, 0);
        place(p);
        tap(p.dialPane(), p.pointOf(7, false));
        assertEquals(19, p.getHour(), "seven in the afternoon");
        tap(p.dialPane(), p.pointOf(22, true));
        assertEquals(15, p.getMinute());
        tap(p.dialPane(), p.pointOf(58, true));
        assertEquals(0, p.getMinute(), "the nearest stop to 58 is the hour");
        assertEquals(19, p.getHour());
        p.showMinutes(false);
        tap(p.dialPane(), p.pointOf(12, false));
        assertEquals(12, p.getHour(), "twelve in the afternoon is noon");

        p.setHourRange(9, 17);
        assertEquals(12, p.getHour());
        p.showMinutes(false);
        tap(p.dialPane(), p.pointOf(8, false));
        assertEquals(12, p.getHour(), "eight in the evening is outside the range");
        p.showMinutes(false);
        tap(p.dialPane(), p.pointOf(4, false));
        assertEquals(16, p.getHour());
        p.setTime(22, 0);
        assertEquals(16, p.getHour(), "a time past the range is its last hour");
    }

    @FormTest
    void aTypedTimeIsChosen() {
        MaterialTimePicker p = new MaterialTimePicker();
        p.setTime(9, 15);
        place(p);
        p.setInputMode(true);
        assertEquals("09", p.hourInputField().getText());
        p.hourInputField().setText("21");
        p.minuteInputField().setText("5");
        assertEquals(21, p.getHour());
        assertEquals(5, p.getMinute());
        p.hourInputField().setText("31");
        assertEquals(21, p.getHour(), "no such hour");
        assertFalse(p.acceptInput(), "what is typed is not a time");
        assertEquals("21", p.hourInputField().getText(), "and the field says again what is set");
        assertTrue(p.acceptInput());
        // The step is for the hand: a minute that is typed, or set, is kept.
        p.setMinuteStep(5);
        p.minuteInputField().setText("07");
        assertEquals(7, p.getMinute());
        p.setTime(10, 7);
        assertEquals(7, p.getMinute());
        p.setInputMode(false);
        assertFalse(p.dialPane().isHidden());
    }

    private static Component named(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component found = named((Container) c, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void style(String value) {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("@pickerStyle", value);
        UIManager.getInstance().addThemeProps(props);
    }

    private Picker open(Picker picker) {
        if (picker.getParent() == null) {
            Form form = new Form(BoxLayout.y());
            form.add(picker);
            form.show();
            flushSerialCalls();
        }
        picker.pointerPressed(picker.getAbsoluteX() + 1, picker.getAbsoluteY() + 1);
        picker.pointerReleased(picker.getAbsoluteX() + 1, picker.getAbsoluteY() + 1);
        flushSerialCalls();
        return picker;
    }

    /// Android's default is the platform dialog. The theme's style still
    /// wins there, because the theme is what an application chose; only an
    /// explicit `setUseLightweightPopup(false)` keeps the platform's.
    @FormTest
    void theMaterialStyleWinsOverANativePickerUnlessTheApplicationOptsOut() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        style("material");
        impl.setNativePickerTypeSupported(Boolean.TRUE);
        try {
            Picker byDefault = new Picker();
            byDefault.setType(Display.PICKER_TYPE_TIME);
            open(byDefault);
            Dialog dlg = byDefault.getMaterialDialog();
            assertNotNull(dlg, "the theme's style opens with no setter called");
            tapComponent(named(dlg, "PickerMaterialCancel"));
            flushSerialCalls();
            assertNull(byDefault.getMaterialDialog());

            Picker optedOut = new Picker();
            optedOut.setUseLightweightPopup(false);
            optedOut.setType(Display.PICKER_TYPE_TIME);
            open(optedOut);
            assertNull(optedOut.getMaterialDialog(), "an explicit opt-out keeps the platform dialog");
        } finally {
            impl.setNativePickerTypeSupported((Boolean[]) null);
            style("wheel");
        }
    }

    @FormTest
    void aDialogTheApplicationCoveredIsACancelAndThePickerOpensAgain() {
        style("material");
        try {
            Picker picker = new Picker();
            picker.setUseLightweightPopup(true);
            picker.setType(Display.PICKER_TYPE_DATE);
            picker.setDate(date(2026, 0, 5, 0, 0));
            open(picker);
            Dialog first = picker.getMaterialDialog();
            assertNotNull(first);
            ((MaterialDatePicker) picker.getOpenWidget()).choose(2026, 0, 9);
            // The application goes somewhere else and comes back.
            Form owner = picker.getComponentForm();
            new Form("Elsewhere", BoxLayout.y()).show();
            flushSerialCalls();
            assertEquals(date(2026, 0, 5, 0, 0), picker.getDate(), "what was never confirmed is not the date");
            owner.show();
            flushSerialCalls();
            assertEquals(date(2026, 0, 5, 0, 0), picker.getDate());
            open(picker);
            Dialog second = picker.getMaterialDialog();
            assertNotNull(second, "the picker opens again");
            assertTrue(first != second);
            tapComponent(named(second, "PickerMaterialCancel"));
            flushSerialCalls();
            assertNull(picker.getMaterialDialog());
            assertEquals(date(2026, 0, 5, 0, 0), picker.getDate(), "and Cancel keeps the date it had");
        } finally {
            style("wheel");
        }
    }

    @FormTest
    void aPickerOpensTheMaterialDialogWhenTheThemeAsksForIt() {
        style("material");
        try {
            Picker picker = new Picker();
            picker.setUseLightweightPopup(true);
            picker.setType(Display.PICKER_TYPE_DATE);
            picker.setDate(date(2026, 0, 5, 0, 0));
            Counter c = new Counter();
            picker.addActionListener(c);
            open(picker);
            Dialog dlg = picker.getMaterialDialog();
            assertNotNull(dlg);
            MaterialDatePicker widget = (MaterialDatePicker) picker.getOpenWidget();
            widget.choose(2026, 0, 22);
            assertEquals(date(2026, 0, 22, 0, 0), picker.getDate(), "the picker answers with what the dialog shows");
            tapComponent(named(dlg, "PickerMaterialOk"));
            flushSerialCalls();
            assertNull(picker.getMaterialDialog());
            assertNull(picker.getOpenWidget());
            assertEquals(date(2026, 0, 22, 0, 0), picker.getDate());
            assertEquals(1, c.count);

            // Cancel puts back what there was.
            open(picker);
            dlg = picker.getMaterialDialog();
            ((MaterialDatePicker) picker.getOpenWidget()).choose(2026, 0, 9);
            tapComponent(named(dlg, "PickerMaterialCancel"));
            flushSerialCalls();
            assertEquals(date(2026, 0, 22, 0, 0), picker.getDate());
            assertEquals(1, c.count);

            Picker time = new Picker();
            time.setUseLightweightPopup(true);
            time.setType(Display.PICKER_TYPE_TIME);
            time.setShowMeridiem(true);
            time.setTime(8 * 60 + 30);
            open(time);
            MaterialTimePicker face = (MaterialTimePicker) time.getOpenWidget();
            assertTrue(face.isShowMeridiem());
            assertEquals(8, face.getHour());
            face.setTime(20, 45);
            tapComponent(named(time.getMaterialDialog(), "PickerMaterialOk"));
            flushSerialCalls();
            assertEquals(20 * 60 + 45, time.getTime());

            // Looking at a time and confirming it changes nothing, and the
            // last hour of the range is one that can be chosen.
            time.setHourRange(9, 17);
            time.setTime(17 * 60 + 32);
            open(time);
            Dialog first = time.getMaterialDialog();
            open(time);
            assertTrue(first == time.getMaterialDialog(), "asked twice, one dialog");
            tapComponent(named(first, "PickerMaterialOk"));
            flushSerialCalls();
            assertNull(time.getMaterialDialog());
            assertEquals(17 * 60 + 32, time.getTime());

            Picker both = new Picker();
            both.setUseLightweightPopup(true);
            both.setType(Display.PICKER_TYPE_DATE_AND_TIME);
            both.setDate(date(2026, 0, 5, 10, 0));
            open(both);
            MaterialDateTimePicker dt = (MaterialDateTimePicker) both.getOpenWidget();
            dt.datePicker().choose(2026, 0, 7);
            dt.showTime(true);
            dt.timePicker().setTime(11, 20);
            // Closing from outside keeps nothing and runs what waits on it.
            final boolean[] ran = new boolean[1];
            both.stopEditing(new Runnable() {
                @Override
                public void run() {
                    ran[0] = true;
                }
            });
            flushSerialCalls();
            assertTrue(ran[0]);
            assertNull(both.getMaterialDialog());
            assertEquals(date(2026, 0, 5, 10, 0), both.getDate());
            open(both);
            dt = (MaterialDateTimePicker) both.getOpenWidget();
            dt.datePicker().choose(2026, 0, 7);
            dt.timePicker().setTime(11, 20);
            tapComponent(named(both.getMaterialDialog(), "PickerMaterialOk"));
            flushSerialCalls();
            assertEquals(date(2026, 0, 7, 11, 20), both.getDate());
        } finally {
            style("wheel");
        }
    }

    @FormTest
    void withoutTheConstantAPickerKeepsItsWheels() {
        Picker picker = new Picker();
        picker.setUseLightweightPopup(true);
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setDate(date(2026, 0, 5, 0, 0));
        open(picker);
        assertNull(picker.getMaterialDialog());
        assertNotNull(picker.getOpenWidget());
        assertFalse(picker.getOpenWidget() instanceof MaterialDatePicker);
        picker.stopEditing(null);
        flushSerialCalls();
    }
}
