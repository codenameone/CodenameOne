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
package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The styles the scheduler, the Gantt chart and the Material pickers paint
/// from: the framework's fallbacks for a theme that knows nothing of them, and
/// what the two modern native themes ship.
public class SchedulingThemeTest extends UITestBase {

    /// A theme with no rule for these UIIDs still gets a grid that is not
    /// black lines on white, and a chosen day that is not the default text.
    @Test
    public void fallbackStylesAreInstalled() {
        UIManager m = UIManager.getInstance();
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("Label.fgColor", "112233");
        m.setThemeProps(theme);
        try {
            assertEquals(0xdadce0, m.getComponentStyle("SchedulerGrid").getFgColor());
            assertEquals(0x1a73e8, m.getComponentStyle("SchedulerItem").getBgColor());
            assertEquals(0xd3e3fd, m.getComponentSelectedStyle("GanttRow").getBgColor());
            assertEquals(0x1a73e8, m.getComponentSelectedStyle("PickerMaterialDay").getBgColor());
            assertEquals(0xffffff, m.getComponentSelectedStyle("PickerMaterialDay").getFgColor());
            assertEquals(0, m.getComponentStyle("PickerMaterialDay").getBgTransparency() & 0xff);
            // a toggle that is down draws its pressed style
            assertEquals(0xd3e3fd, m.getComponentCustomStyle("SchedulerViewButton", "press").getBgColor());
        } finally {
            m.setThemeProps(new Hashtable<String, Object>());
        }
    }

    /// A theme whose own background is dark gets the dark fallbacks, with or
    /// without the dark mode switch.
    @Test
    public void darkThemeGetsDarkFallbacks() {
        UIManager m = UIManager.getInstance();
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("bgColor", "101010");
        m.setThemeProps(theme);
        try {
            assertEquals(0x3c4043, m.getComponentStyle("SchedulerGrid").getFgColor());
            assertEquals(0x1c1b1f, m.getComponentStyle("Gantt").getBgColor());
        } finally {
            m.setThemeProps(new Hashtable<String, Object>());
        }
    }

    /// A UIID the theme styles is the theme's whole: no fallback is merged
    /// into it, in any state.
    @Test
    public void themedUiidIsLeftAlone() {
        UIManager m = UIManager.getInstance();
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("GanttRow.bgColor", "ff0000");
        m.setThemeProps(theme);
        try {
            assertEquals(0xff0000, m.getComponentStyle("GanttRow").getBgColor());
            assertNotEquals(0xd3e3fd, m.getComponentSelectedStyle("GanttRow").getBgColor());
            // its neighbours still fall back
            assertEquals(0xf1f3f4, m.getComponentStyle("GanttSummaryRow").getBgColor());
        } finally {
            m.setThemeProps(new Hashtable<String, Object>());
        }
    }

    @Test
    public void androidMaterialThemeTurnsMaterialPickersOn() throws Exception {
        Hashtable theme = load("AndroidMaterialTheme.res");
        if (theme == null) {
            return;
        }
        assertEquals("material", theme.get("@pickerStyle"));
        // the style alone is enough; this one would move string pickers too
        assertNull(theme.get("@lightweightPickerBool"));
        // the circle under the chosen day follows the accent, in both modes
        assertEquals("6750a4", theme.get("PickerMaterialDay.sel#bgColor"));
        assertEquals("accent-color", theme.get("@cn1-bind:PickerMaterialDay.sel#bgColor"));
        assertEquals("d0bcff", theme.get("$DarkPickerMaterialDay.sel#bgColor"));
        assertEquals("accent-color-dark", theme.get("@cn1-bind:$DarkPickerMaterialDay.sel#bgColor"));
        assertEquals("cac4d0", theme.get("SchedulerGrid.fgColor"));
        assertEquals("49454f", theme.get("$DarkSchedulerGrid.fgColor"));
        assertEquals("eaddff", theme.get("GanttRow.sel#bgColor"));
        assertNotNull(theme.get("PickerMaterialDialog.border"));
        assertEquals("CalendarToday", theme.get("@calendarTodayUIID"));
        assertEquals("6750a4", theme.get("CalendarSelectedDay.bgColor"));
        assertNotNull(theme.get("CalendarToday.border"));
    }

    /// iOS keeps its wheels; it styles the scheduler and the chart, and the
    /// Material widgets for an application that asks for them.
    @Test
    public void iosModernThemeStylesTheSchedulerAndKeepsWheels() throws Exception {
        Hashtable theme = load("iOSModernTheme.res");
        if (theme == null) {
            return;
        }
        assertNull(theme.get("@pickerStyle"));
        assertEquals("7aff", theme.get("SchedulerItem.bgColor"));
        assertEquals("accent-color", theme.get("@cn1-bind:SchedulerItem.bgColor"));
        assertEquals("a84ff", theme.get("$DarkSchedulerItem.bgColor"));
        assertEquals("7aff", theme.get("PickerMaterialDial.sel#bgColor"));
        assertEquals("CalendarToday", theme.get("@calendarTodayUIID"));
        assertEquals("7aff", theme.get("CalendarToday.fgColor"));
    }

    private static Hashtable load(String fileName) throws Exception {
        File cwd = new File(".").getAbsoluteFile();
        File found = null;
        for (int i = 0; i < 6 && cwd != null && found == null; i++) {
            File candidate = new File(cwd, "Themes/" + fileName);
            if (candidate.isFile()) {
                found = candidate;
            }
            cwd = cwd.getParentFile();
        }
        if (found == null) {
            return null;
        }
        InputStream stream = new FileInputStream(found);
        try {
            Resources res = Resources.open(stream);
            return res.getTheme(res.getThemeResourceNames()[0]);
        } finally {
            stream.close();
        }
    }
}
