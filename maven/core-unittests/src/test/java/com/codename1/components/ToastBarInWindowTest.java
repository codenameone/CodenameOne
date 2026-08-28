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
}
