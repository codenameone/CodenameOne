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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Desktop;
import com.codename1.ui.Label;
import com.codename1.ui.Monitor;
import com.codename1.ui.Window;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

/**
 * The behavioural half of the multi-window suite: no screenshot, pass or fail, and it
 * runs on every target.
 *
 * <p>This is what actually proves a real operating-system window exists, because it
 * asserts against state the <em>port</em> reports rather than against pixels. On a
 * platform with no windowing system it asserts the opposite: that the capability query
 * says so and that constructing a window throws rather than silently degrading.</p>
 *
 * @author Shai Almog
 */
public class MultiWindowApiTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        if (!Desktop.isSupported()) {
            assertUnsupported();
        } else {
            assertSupported();
        }
        done();
        return true;
    }

    private void assertUnsupported() {
        // Degrading safely still has to hold: portable code loops over these.
        if (Desktop.getInstance().getWindows().length != 0) {
            fail("getWindows() must be empty where there is no windowing system");
            return;
        }
        if (Desktop.getInstance().getFocusedWindow() != null) {
            fail("getFocusedWindow() must be null where there is no windowing system");
            return;
        }
        if (Desktop.getInstance().getMonitors().length < 1) {
            fail("getMonitors() must still report the main display");
            return;
        }
        boolean threw = false;
        try {
            new Window("should not open");
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        if (!threw) {
            fail("Constructing a Window must throw where there is no windowing system");
        }
    }

    private void assertSupported() {
        Monitor[] monitors = Desktop.getInstance().getMonitors();
        if (monitors.length < 1) {
            fail("A windowing platform must report at least one monitor");
            return;
        }
        for (Monitor m : monitors) {
            Rectangle bounds = m.getBounds();
            if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                fail("Monitor " + m.getName() + " reported empty bounds");
                return;
            }
            Rectangle work = m.getWorkArea();
            if (work.getWidth() > bounds.getWidth() || work.getHeight() > bounds.getHeight()) {
                fail("Monitor " + m.getName() + " work area is larger than its bounds");
                return;
            }
            if (m.getScale() <= 0) {
                fail("Monitor " + m.getName() + " reported a non-positive scale");
                return;
            }
        }

        int before = Desktop.getInstance().getWindows().length;
        Window w = new Window("api", new BorderLayout());
        Component content = new Label("content");
        w.add(BorderLayout.CENTER, content);
        w.setWindowSize(500, 360);
        w.show();

        try {
            if (Desktop.getInstance().getWindows().length != before + 1) {
                fail("show() must register exactly one window");
                return;
            }
            if (Desktop.getInstance().windowById(w.getWindowId()) != w) {
                fail("A window must be resolvable by the id its events are routed with");
                return;
            }
            // The load-bearing property of the whole design: a component inside a
            // window resolves that window, and is honestly not in any Form.
            if (content.getTopLevelContainer() != w) {
                fail("A component in a Window must resolve that Window as its top level");
                return;
            }
            if (content.getComponentForm() != null) {
                fail("getComponentForm() must be null inside a Window");
                return;
            }
            if (w.getMonitor() == null) {
                fail("A shown window must report the monitor it is on");
                return;
            }
            if (w.getScale() <= 0) {
                fail("A shown window must report its monitor's scale");
                return;
            }
            // Content is laid out to the window, not to the main display.
            if (w.getWidth() <= 0 || w.getHeight() <= 0) {
                fail("A shown window must have a laid-out size");
                return;
            }
            if (w.getWidth() == CN.getDisplayWidth() && w.getHeight() == CN.getDisplayHeight()) {
                fail("A window sized 500x360 must not report the main display's size");
                return;
            }
            w.setTitle("renamed");
            if (!"renamed".equals(w.getTitle())) {
                fail("setTitle must be readable back");
                return;
            }
        } finally {
            w.dispose();
        }

        if (!w.isWindowDisposed()) {
            fail("dispose() must mark the window disposed");
            return;
        }
        if (Desktop.getInstance().getWindows().length != before) {
            fail("A disposed window must leave the desktop registry");
        }
    }
}
