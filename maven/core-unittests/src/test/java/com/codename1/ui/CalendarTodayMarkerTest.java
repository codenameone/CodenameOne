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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Today's cell in the month grid: marked only when a UIID is named for it,
/// and only while today is not the selected day.
public class CalendarTodayMarkerTest extends UITestBase {

    private static java.util.Calendar today() {
        return java.util.Calendar.getInstance();
    }

    /// A day of this month that is not today.
    private static Date anotherDay() {
        java.util.Calendar c = today();
        c.set(java.util.Calendar.DAY_OF_MONTH, c.get(java.util.Calendar.DAY_OF_MONTH) == 1 ? 2 : 1);
        return c.getTime();
    }

    private static List<Button> cells(Container root, String uiid, List<Button> out) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Button && uiid.equals(c.getUIID()) && c.isEnabled()) {
                out.add((Button) c);
            } else if (c instanceof Container) {
                cells((Container) c, uiid, out);
            }
        }
        return out;
    }

    @FormTest
    public void nothingIsMarkedWithoutAUiid() {
        Calendar cal = new Calendar();
        assertNull(cal.getTodayUIID());
        cal.setDate(anotherDay());
        assertEquals(0, cells(cal, "CalendarToday", new ArrayList<Button>()).size());
    }

    @FormTest
    public void todayIsMarkedWhileAnotherDayIsSelected() {
        Calendar cal = new Calendar();
        cal.setTodayUIID("CalendarToday");
        cal.setDate(anotherDay());
        List<Button> marked = cells(cal, "CalendarToday", new ArrayList<Button>());
        assertEquals(1, marked.size());
        assertEquals(String.valueOf(today().get(java.util.Calendar.DAY_OF_MONTH)), marked.get(0).getText().trim());
        assertEquals(1, cells(cal, "CalendarSelectedDay", new ArrayList<Button>()).size());
    }

    @FormTest
    public void selectedTodayIsDrawnSelectedAndMarkedAgainAfter() {
        Calendar cal = new Calendar();
        Form f = Display.getInstance().getCurrent();
        f.setLayout(new BorderLayout());
        f.add(BorderLayout.CENTER, cal);
        f.revalidate();
        cal.setTodayUIID("CalendarToday");
        cal.setDate(today().getTime());
        f.revalidate();
        assertEquals(0, cells(cal, "CalendarToday", new ArrayList<Button>()).size());
        assertEquals(1, cells(cal, "CalendarSelectedDay", new ArrayList<Button>()).size());

        // choosing another day by tapping it hands the marker back to today
        String other = String.valueOf(today().get(java.util.Calendar.DAY_OF_MONTH) == 1 ? 2 : 1);
        Button target = null;
        for (Button b : cells(cal, "CalendarDay", new ArrayList<Button>())) {
            if (other.equals(b.getText().trim())) {
                target = b;
                break;
            }
        }
        tapComponent(target);
        flushSerialCalls();
        assertEquals(1, cells(cal, "CalendarToday", new ArrayList<Button>()).size());
        assertEquals(other, cells(cal, "CalendarSelectedDay", new ArrayList<Button>()).get(0).getText().trim());

        cal.setTodayUIID(null);
        assertEquals(0, cells(cal, "CalendarToday", new ArrayList<Button>()).size());
    }
}
