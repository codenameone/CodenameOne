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
import com.codename1.testing.TestWindowManager;
import com.codename1.ui.geom.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopMonitorTest extends UITestBase {

    /// A laptop panel at 2x with a dock reserved at the bottom, plus a conventional
    /// external display placed to its right. The mixed scale is the point: it is what
    /// makes per-monitor density observable.
    private TestWindowManager twoMonitors() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        List<TestWindowManager.FakeMonitor> monitors =
                new ArrayList<TestWindowManager.FakeMonitor>(Arrays.asList(
                        new TestWindowManager.FakeMonitor(0, 0, 1440, 900, 2.0, 220, "laptop")
                                .withReservedBottom(60),
                        new TestWindowManager.FakeMonitor(1440, 0, 1920, 1080, 1.0, 96, "external")));
        wm.setMonitors(monitors);
        return wm;
    }

    @FormTest
    void monitorsAreEnumeratedWithTheirCharacteristics() {
        twoMonitors();
        Monitor[] all = Desktop.getInstance().getMonitors();
        assertEquals(2, all.length);

        assertEquals("laptop", all[0].getName());
        assertTrue(all[0].isPrimary());
        assertEquals(2.0, all[0].getScale(), 0.001);
        assertEquals(220, all[0].getDotsPerInch());

        assertEquals("external", all[1].getName());
        assertFalse(all[1].isPrimary());
        assertEquals(1.0, all[1].getScale(), 0.001);
    }

    @FormTest
    void workAreaExcludesReservedSpace() {
        twoMonitors();
        Monitor laptop = Desktop.getInstance().getMonitors()[0];
        assertEquals(900, laptop.getBounds().getHeight());
        assertEquals(840, laptop.getWorkArea().getHeight(),
                "The dock's 60px must be excluded from the usable area");
    }

    @FormTest
    void monitorAtResolvesByDesktopCoordinate() {
        twoMonitors();
        assertEquals("laptop", Desktop.getInstance().getMonitorAt(100, 100).getName());
        assertEquals("external", Desktop.getInstance().getMonitorAt(1500, 100).getName(),
                "A coordinate past the primary's width belongs to the display beside it");
    }

    @FormTest
    void desktopBoundsSpanEveryMonitor() {
        twoMonitors();
        Rectangle all = Desktop.getInstance().getDesktopBounds();
        assertEquals(0, all.getX());
        assertEquals(0, all.getY());
        assertEquals(3360, all.getWidth(), "1440 + 1920");
        assertEquals(1080, all.getHeight(), "the taller of the two");
    }

    @FormTest
    void aWindowReportsItsOwnMonitorsDensityNotTheGlobalOne() {
        TestWindowManager wm = twoMonitors();
        Window w = new Window("scaled");
        w.show();
        TestWindowManager.FakeWindow peer = wm.getLastWindow();

        peer.setMonitor(0);
        w.monitorChanged();
        assertEquals(2.0, w.getScale(), 0.001);
        assertEquals("laptop", w.getMonitor().getName());
        int hiDensity = w.getDensity();

        // drag it onto the conventional display
        peer.setMonitor(1);
        w.monitorChanged();
        assertEquals(1.0, w.getScale(), 0.001);
        assertEquals("external", w.getMonitor().getName());
        assertTrue(w.getDensity() < hiDensity,
                "Moving to a lower resolution display must lower the reported density");
        w.dispose();
    }

    @FormTest
    void movingToADifferentScaleInvalidatesTheLayout() {
        TestWindowManager wm = twoMonitors();
        Window w = new Window("relayout");
        w.show();
        w.shouldCalcPreferredSize = false;

        wm.getLastWindow().setMonitor(1);
        w.monitorChanged();

        assertTrue(w.shouldCalcPreferredSize,
                "A scale change must mark preferred sizes stale, or the window renders "
                        + "at the size it was measured for on the previous display");
        w.dispose();
    }

    @FormTest
    void windowingOffStillReportsOneUsableMonitor() {
        assertFalse(Desktop.isSupported());
        Monitor[] all = Desktop.getInstance().getMonitors();
        assertEquals(1, all.length);
        assertTrue(all[0].isPrimary());
        assertEquals(1.0, all[0].getScale(), 0.001);
    }
}
