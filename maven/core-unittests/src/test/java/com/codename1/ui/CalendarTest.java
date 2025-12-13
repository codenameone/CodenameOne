package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalendarTest extends UITestBase {

    @FormTest
    void selectingDayUpdatesSelectedDate() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        com.codename1.ui.Calendar widget = new com.codename1.ui.Calendar();
        form.add(BorderLayout.CENTER, widget);
        form.revalidate();

        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.APRIL, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        widget.setDate(cal.getTime());
        form.revalidate();

        Button target = findDayButton(widget, 20);
        assertNotNull(target, "Expected to find button for day 20");

        int x = target.getAbsoluteX() + target.getWidth() / 2;
        int y = target.getAbsoluteY() + target.getHeight() / 2;
        target.pointerPressed(x, y);
        target.pointerReleased(x, y);

        Date selected = widget.getDate();
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(selected);
        assertEquals(20, selectedCal.get(Calendar.DAY_OF_MONTH), "Selecting day button should update selected date");
    }

    @FormTest
    void disablingSelectionPreventsDateChanges() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        com.codename1.ui.Calendar widget = new com.codename1.ui.Calendar();
        widget.setChangesSelectedDateEnabled(false);
        form.add(BorderLayout.CENTER, widget);
        form.revalidate();

        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 5, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        widget.setDate(cal.getTime());
        form.revalidate();

        Date original = widget.getDate();

        Button target = findDayButton(widget, 10);
        assertNotNull(target, "Expected to find day button");

        int x = target.getAbsoluteX() + target.getWidth() / 2;
        int y = target.getAbsoluteY() + target.getHeight() / 2;
        target.pointerPressed(x, y);
        target.pointerReleased(x, y);

        assertEquals(original, widget.getDate(), "Date should remain unchanged when selection disabled");
    }

    @FormTest
    void multipleSelectionTracksAllSelectedDays() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        com.codename1.ui.Calendar widget = new com.codename1.ui.Calendar();
        widget.setMultipleSelectionEnabled(true);
        form.add(BorderLayout.CENTER, widget);
        form.revalidate();

        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        widget.setDate(cal.getTime());
        form.revalidate();

        Button day5 = findDayButton(widget, 5);
        Button day7 = findDayButton(widget, 7);
        assertNotNull(day5, "Expected day 5 button");
        assertNotNull(day7, "Expected day 7 button");

        pressButton(day5);
        pressButton(day7);

        Collection<Date> selectedDays = widget.getSelectedDays();
        assertNotNull(selectedDays, "Selected days collection should not be null");
        assertEquals(2, selectedDays.size(), "Two days should be selected");

        List<Integer> dayValues = new ArrayList<>();
        for (Date d : selectedDays) {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            dayValues.add(c.get(Calendar.DAY_OF_MONTH));
        }
        assertTrue(dayValues.contains(5), "Selected days should include 5");
        assertTrue(dayValues.contains(7), "Selected days should include 7");
    }

    private void pressButton(Button button) {
        int x = button.getAbsoluteX() + button.getWidth() / 2;
        int y = button.getAbsoluteY() + button.getHeight() / 2;
        button.pointerPressed(x, y);
        button.pointerReleased(x, y);
    }

    private Button findDayButton(com.codename1.ui.Calendar calendar, int day) {
        Container monthView = (Container) calendar.getComponentAt(calendar.getComponentCount() - 1);
        Container days = (Container) monthView.getComponentAt(1);
        for (int i = 0; i < days.getComponentCount(); i++) {
            Component cmp = days.getComponentAt(i);
            if (cmp instanceof Button) {
                Button button = (Button) cmp;
                if (button.isEnabled()) {
                    String text = button.getText();
                    if (text != null && text.trim().equals(String.valueOf(day))) {
                        return button;
                    }
                }
            }
        }
        return null;
    }
}
