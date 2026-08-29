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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// `ToastBar` on a secondary window.
class ToastBarInWindowTest extends UITestBase {

    @FormTest
    void aWindowGetsItsOwnToastBarAndAFormKeepsTheSingleton() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        ToastBar singleton = ToastBar.getInstance();
        assertSame(singleton, ToastBar.getInstance(main),
                "a form keeps the singleton, which is what follows the current form");
        assertSame(singleton, ToastBar.getInstance(null));

        ToastBar forWindow = ToastBar.getInstance(w);
        assertNotSame(singleton, forWindow,
                "a window needs its own, or one window's toasts redirect another's");
        assertSame(forWindow, ToastBar.getInstance(w),
                "and the same window has to keep giving back the same one");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aWindowsToastBarResolvesToThatWindowRatherThanTheCurrentForm() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        // The instance is cached on the window itself, so it dies with the window and
        // needs no registry to clean up. Disposing and reopening gives a fresh one.
        ToastBar bound = ToastBar.getInstance(w);
        assertSame(bound, w.getClientProperty("cn1$ToastBar"),
                "a window's toast bar is cached on the window");
        assertNull(main.getClientProperty("cn1$ToastBar"),
                "and the main form keeps using the singleton");

        w.dispose();
        DisplayTest.flushEdt();
    }

    // Whether a toast actually renders inside the window is a rendering question, and
    // ToastBar.Status.show() drives it through slideUpAndWait -- which parks the event
    // dispatch thread until the animation finishes. That belongs in the device
    // conformance suite, which runs a real surface, rather than here.

    @FormTest
    void aDisposedWindowTakesItsToastBarWithIt() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        ToastBar first = ToastBar.getInstance(w);
        assertNotNull(first);

        w.dispose();
        DisplayTest.flushEdt();

        // Cached on the window itself rather than in a registry, so there is nothing
        // to clean up and nothing that outlives the window.
        Window second = new Window("host again", new BorderLayout());
        second.setWindowSize(400, 300);
        second.show();
        DisplayTest.flushEdt();
        assertNotSame(first, ToastBar.getInstance(second),
                "a new window gets a new one rather than inheriting the old window's");

        second.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNullTopLevelFallsBackToTheSingleton() {
        assertSame(ToastBar.getInstance(), ToastBar.getInstance(null),
                "callers with nothing to resolve still get the singleton");
    }

    @FormTest
    void theSingletonStaysOnTheFormEvenWhileAWindowIsFocused() {
        // Two instances sharing one window's cached component, with two status lists,
        // would fight: one expiring a toast the other still thinks it is showing.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();
        com.codename1.ui.Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        ToastBar singleton = ToastBar.getInstance();
        ToastBar forWindow = ToastBar.getInstance(w);
        assertNotSame(singleton, forWindow);

        // The singleton is form-only by contract, so a legacy caller reaching for it
        // while a window has focus still targets the main form and cannot collide with
        // the window's own instance.
        ToastBar.Status s = singleton.createStatus();
        s.setMessage("legacy");
        assertNull(w.getClientProperty("ToastBarComponent"),
                "the singleton must not build itself onto the focused window");

        w.dispose();
        DisplayTest.flushEdt();
    }
}
