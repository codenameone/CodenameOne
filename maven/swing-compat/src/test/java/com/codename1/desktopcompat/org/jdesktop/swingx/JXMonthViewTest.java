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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DaySelectionModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.Test;

/// The month view: where its days are, what presses select and how the
/// month moves. Every date is fixed; March 2024 starts on a Friday and
/// has 31 days.
public class JXMonthViewTest extends KernelTestBase {

    private static Date date(int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2024);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date march(int day) {
        return date(Calendar.MARCH, day, 0);
    }

    private JXMonthView view;
    private JFrame frame;

    private void open() {
        view = new JXMonthView(march(1));
        frame = new JFrame();
        frame.add(view, BorderLayout.CENTER);
        show(frame);
    }

    private void click(Rectangle r) {
        int x = r.x + r.width / 2;
        int y = r.y + r.height / 2;
        press(frame, view, x, y);
        release(frame, view, x, y);
    }

    private Date at(Rectangle r) {
        return view.getDayAtLocation(r.x + r.width / 2, r.y + r.height / 2);
    }

    @Test
    public void aPointFindsItsDay() {
        open();
        assertEquals(march(1), view.getFirstDisplayedDay());
        int[] days = {1, 2, 9, 15, 30, 31};
        for (int i = 0; i < days.length; i++) {
            Rectangle r = view.cn1DayBounds(date(Calendar.MARCH, days[i], 13));
            assertNotNull(r);
            assertTrue(r.width > 0 && r.height > 0);
            assertEquals(march(days[i]), at(r));
            assertEquals(march(days[i]), view.getDayAtLocation(r.x, r.y));
            assertEquals(march(days[i]), view.getDayAtLocation(r.x + r.width - 1, r.y + r.height - 1));
        }
        // A week is a row: the 8th sits right under the 1st.
        Rectangle first = view.cn1DayBounds(march(1));
        Rectangle eighth = view.cn1DayBounds(march(8));
        assertEquals(first.x, eighth.x);
        assertEquals(first.y + first.height, eighth.y);
        assertNull("a day of another month has no box", view.cn1DayBounds(date(Calendar.APRIL, 1, 0)));
    }

    @Test
    public void theTitleTheDayNamesAndTheEmptyBoxesHoldNoDay() {
        open();
        Rectangle previous = view.cn1ArrowBounds(false);
        Rectangle next = view.cn1ArrowBounds(true);
        assertTrue(previous.x < next.x);
        assertNull(at(previous));
        assertNull(at(next));
        assertNull("the title", view.getDayAtLocation(view.getWidth() / 2, previous.y + previous.height / 2));
        assertNull("the day names",
                view.getDayAtLocation(view.getWidth() / 2, previous.y + previous.height + previous.height / 2));
        // With Sunday first the 1st, a Friday, has five empty boxes
        // before it.
        Rectangle first = view.cn1DayBounds(march(1));
        assertEquals(previous.x + 5 * first.width, first.x);
        assertNull(view.getDayAtLocation(first.x - 1, first.y + 1));
        assertNull(view.getDayAtLocation(previous.x + 1, first.y + 1));
        // And the 31st, a Sunday, starts the last row alone.
        Rectangle last = view.cn1DayBounds(march(31));
        assertEquals(previous.x, last.x);
        assertNull(view.getDayAtLocation(last.x + last.width + 1, last.y + 1));
        assertNull(view.getDayAtLocation(-5, last.y + 1));
        assertNull(view.getDayAtLocation(last.x + 1, view.getHeight() + 50));
    }

    @Test
    public void theFirstDayOfTheWeekMovesTheGrid() {
        open();
        assertEquals(Calendar.SUNDAY, view.getFirstDayOfWeek());
        Rectangle sunday = view.cn1DayBounds(march(1));
        String firstName = view.getDayOfTheWeek(Calendar.SUNDAY);
        List<Object[]> text = paint(frame);
        assertNotNull(find(text, view.cn1Title()));
        assertTrue(view.cn1Title().indexOf("2024") >= 0);
        Object[] sun = find(text, firstName);
        assertNotNull(sun);

        view.setFirstDayOfWeek(Calendar.MONDAY);
        assertEquals(Calendar.MONDAY, view.getFirstDayOfWeek());
        assertEquals(Calendar.MONDAY, view.getSelectionModel().getFirstDayOfWeek());
        Rectangle monday = view.cn1DayBounds(march(1));
        assertEquals("one box to the left", sunday.x - sunday.width, monday.x);
        assertEquals(sunday.y, monday.y);
        assertEquals(march(2), at(sunday));
        // The 31st, a Sunday, now ends the row before instead of
        // starting one.
        Rectangle last = view.cn1DayBounds(march(31));
        assertEquals(monday.x + 2 * monday.width, last.x);
        // And Sunday's name is drawn in the last column.
        Object[] moved = find(paint(frame), firstName);
        assertNotNull(moved);
        assertTrue(((Number) moved[1]).intValue() > ((Number) sun[1]).intValue());
    }

    @Test
    public void aPressSelectsADayAndCommits() {
        open();
        final List<String> commands = new ArrayList<String>();
        view.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commands.add(e.getActionCommand());
            }
        });
        assertTrue(view.isSelectionEmpty());
        assertNull(view.getSelectionDate());
        click(view.cn1DayBounds(march(10)));
        assertEquals(march(10), view.getSelectionDate());
        assertTrue(view.isSelected(date(Calendar.MARCH, 10, 16)));
        assertEquals(1, commands.size());
        assertEquals(JXMonthView.COMMIT_KEY, commands.get(0));
        assertFalse(view.getSelectionModel().isAdjusting());
        // One day at a time: the next press replaces it.
        click(view.cn1DayBounds(march(12)));
        assertEquals(1, view.getSelection().size());
        assertEquals(march(12), view.getSelectionDate());
        assertEquals(2, commands.size());
        // A press on an empty box selects nothing and commits nothing.
        Rectangle first = view.cn1DayBounds(march(1));
        press(frame, view, first.x - first.width / 2, first.y + 2);
        release(frame, view, first.x - first.width / 2, first.y + 2);
        assertEquals(march(12), view.getSelectionDate());
        assertEquals(2, commands.size());
        view.cancelSelection();
        assertEquals(JXMonthView.CANCEL_KEY, commands.get(2));
    }

    @Test
    public void theSelectionModeDecidesWhatIsSelected() {
        open();
        assertEquals(SelectionMode.SINGLE_SELECTION, view.getSelectionMode());
        view.setSelectionInterval(march(5), march(8));
        assertEquals(1, view.getSelection().size());

        view.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        view.setSelectionInterval(march(5), march(8));
        assertEquals(4, view.getSelection().size());
        assertEquals(march(5), view.getFirstSelectionDate());
        assertEquals(march(8), view.getLastSelectionDate());
        view.addSelectionInterval(march(20), march(21));
        assertEquals(2, view.getSelection().size());

        view.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL_SELECTION);
        view.setSelectionInterval(march(5), march(8));
        view.addSelectionInterval(march(20), march(21));
        assertEquals(6, view.getSelection().size());
        view.removeSelectionInterval(march(6), march(6));
        assertEquals(5, view.getSelection().size());
        // A plain press starts over with the day pressed.
        click(view.cn1DayBounds(march(14)));
        assertEquals(1, view.getSelection().size());
        assertEquals(march(14), view.getSelectionDate());
        view.clearSelection();
        assertTrue(view.isSelectionEmpty());

        // The view follows a model handed to it.
        DaySelectionModel model = new DaySelectionModel();
        model.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        view.setSelectionModel(model);
        assertEquals(SelectionMode.SINGLE_INTERVAL_SELECTION, view.getSelectionMode());
        model.setSelectionInterval(march(2), march(4));
        assertEquals(3, view.getSelection().size());
        assertTrue(view.isSelected(march(3)));
    }

    @Test
    public void aDragSpansAnIntervalWithAMouse() {
        open();
        view.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        Rectangle from = view.cn1DayBounds(march(11));
        Rectangle to = view.cn1DayBounds(march(14));
        int y = from.y + from.height / 2;
        press(frame, view, from.x + from.width / 2, y);
        drag(frame, view, to.x + to.width / 2, y);
        release(frame, view, to.x + to.width / 2, y);
        if (com.codename1.ui.Display.getInstance().isTouchScreenDevice()) {
            // A finger that moves scrolls; it selects nothing.
            assertTrue(view.getSelection().size() <= 1);
        } else {
            assertEquals(4, view.getSelection().size());
            assertEquals(march(11), view.getFirstSelectionDate());
            assertEquals(march(14), view.getLastSelectionDate());
        }
    }

    @Test
    public void unselectableDaysAndBoundsRefuseAPress() {
        open();
        view.setUnselectableDates(date(Calendar.MARCH, 11, 9));
        assertTrue(view.isUnselectableDate(march(11)));
        assertFalse(view.isUnselectableDate(march(12)));
        click(view.cn1DayBounds(march(11)));
        assertTrue(view.isSelectionEmpty());

        view.setLowerBound(march(5));
        view.setUpperBound(march(25));
        assertEquals(march(5), view.getLowerBound());
        assertEquals(march(25), view.getUpperBound());
        assertTrue(view.isUnselectableDate(march(4)));
        assertTrue(view.isUnselectableDate(march(26)));
        assertFalse(view.isUnselectableDate(march(5)));
        assertFalse(view.isUnselectableDate(march(25)));
        click(view.cn1DayBounds(march(3)));
        assertTrue(view.isSelectionEmpty());
        click(view.cn1DayBounds(march(28)));
        assertTrue(view.isSelectionEmpty());
        click(view.cn1DayBounds(march(25)));
        assertEquals(march(25), view.getSelectionDate());
        view.setSelectionDate(march(2));
        assertFalse("a date beyond the bounds is not taken", view.isSelected(march(2)));
        view.setUpperBound(null);
        assertNull(view.getUpperBound());
        assertFalse(view.isUnselectableDate(march(26)));
    }

    @Test
    public void flaggedDatesAreKeptByDay() {
        open();
        assertFalse(view.hasFlaggedDates());
        view.setFlaggedDates(date(Calendar.MARCH, 7, 14), march(9));
        assertTrue(view.hasFlaggedDates());
        assertTrue(view.isFlaggedDate(march(7)));
        assertTrue(view.isFlaggedDate(date(Calendar.MARCH, 9, 22)));
        assertFalse(view.isFlaggedDate(march(8)));
        assertEquals(2, view.getFlaggedDates().size());
        assertEquals(march(7), view.getFlaggedDates().first());
        view.addFlaggedDates(march(8));
        assertEquals(3, view.getFlaggedDates().size());
        view.removeFlaggedDates(date(Calendar.MARCH, 7, 3));
        assertFalse(view.isFlaggedDate(march(7)));
        assertEquals(2, view.getFlaggedDates().size());
        // A flag does not stand in the way of selecting.
        click(view.cn1DayBounds(march(8)));
        assertEquals(march(8), view.getSelectionDate());
        view.clearFlaggedDates();
        assertFalse(view.hasFlaggedDates());
    }

    @Test
    public void theArrowsMoveAMonth() {
        open();
        view.setTraversable(true);
        click(view.cn1ArrowBounds(true));
        assertEquals(date(Calendar.APRIL, 1, 0), view.getFirstDisplayedDay());
        assertTrue(view.isSelectionEmpty());
        click(view.cn1ArrowBounds(false));
        click(view.cn1ArrowBounds(false));
        assertEquals(date(Calendar.FEBRUARY, 1, 0), view.getFirstDisplayedDay());
        // 2024 is a leap year.
        assertEquals(date(Calendar.FEBRUARY, 29, 0),
                view.getSelectionModel().getNormalizedDate(view.getLastDisplayedDay()));
        assertNotNull(view.cn1DayBounds(date(Calendar.FEBRUARY, 29, 0)));
        assertNull(view.cn1DayBounds(march(1)));
        // February 2024 starts on a Thursday.
        assertEquals(view.cn1ArrowBounds(false).x + 4 * view.cn1ArrowBounds(false).width,
                view.cn1DayBounds(date(Calendar.FEBRUARY, 1, 0)).x);

        view.setTraversable(false);
        click(view.cn1ArrowBounds(true));
        assertEquals(date(Calendar.FEBRUARY, 1, 0), view.getFirstDisplayedDay());

        view.setFirstDisplayedDay(date(Calendar.JULY, 19, 11));
        assertEquals(date(Calendar.JULY, 1, 0), view.getFirstDisplayedDay());
        view.ensureDateVisible(date(Calendar.JULY, 30, 0));
        assertEquals(date(Calendar.JULY, 1, 0), view.getFirstDisplayedDay());
        view.ensureDateVisible(date(Calendar.DECEMBER, 25, 0));
        assertEquals(date(Calendar.DECEMBER, 1, 0), view.getFirstDisplayedDay());
        assertEquals(date(Calendar.DECEMBER, 25, 0), at(view.cn1DayBounds(date(Calendar.DECEMBER, 25, 0))));
    }
}
