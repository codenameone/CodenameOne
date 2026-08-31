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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertSame(singleton, ToastBar.getForTopLevel(main),
                "a form keeps the singleton, which is what follows the current form");
        assertSame(singleton, ToastBar.getForTopLevel(null));

        ToastBar forWindow = ToastBar.getForTopLevel(w);
        assertNotSame(singleton, forWindow,
                "a window needs its own, or one window's toasts redirect another's");
        assertSame(forWindow, ToastBar.getForTopLevel(w),
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
        ToastBar bound = ToastBar.getForTopLevel(w);
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
        ToastBar first = ToastBar.getForTopLevel(w);
        assertNotNull(first);

        w.dispose();
        DisplayTest.flushEdt();

        // Cached on the window itself rather than in a registry, so there is nothing
        // to clean up and nothing that outlives the window.
        Window second = new Window("host again", new BorderLayout());
        second.setWindowSize(400, 300);
        second.show();
        DisplayTest.flushEdt();
        assertNotSame(first, ToastBar.getForTopLevel(second),
                "a new window gets a new one rather than inheriting the old window's");

        second.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNullTopLevelFallsBackToTheSingleton() {
        assertSame(ToastBar.getInstance(), ToastBar.getForTopLevel(null),
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
        ToastBar forWindow = ToastBar.getForTopLevel(w);
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

    @FormTest
    void aWindowInstanceStartsFromTheSingletonsConfiguration() {
        // An application sets the position and the UIIDs once at start-up and then
        // calls the static helpers. Those reach the window's instance when a window has
        // focus, and a factory-fresh one quietly ignored every one of those settings.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        ToastBar singleton = ToastBar.getInstance();
        int originalPosition = singleton.getPosition();
        String originalUiid = singleton.getDefaultUIID();
        String originalMessageUiid = singleton.getDefaultMessageUIID();
        try {
            singleton.setPosition(Component.TOP);
            singleton.setDefaultUIID("ConfiguredToastBar");
            singleton.setDefaultMessageUIID("ConfiguredToastBarMessage");

            Window w = new Window("host", new BorderLayout());
            w.setWindowSize(500, 400);
            w.show();
            DisplayTest.flushEdt();

            ToastBar forWindow = ToastBar.getForTopLevel(w);
            assertNotSame(singleton, forWindow);
            assertEquals(Component.TOP, forWindow.getPosition(),
                    "a window's toast bar starts from the configuration the "
                            + "application gave the singleton");
            assertEquals("ConfiguredToastBar", forWindow.getDefaultUIID());
            assertEquals("ConfiguredToastBarMessage", forWindow.getDefaultMessageUIID());

            // Copied rather than shared, so the window can still differ afterwards.
            forWindow.setPosition(Component.BOTTOM);
            assertEquals(Component.TOP, singleton.getPosition(),
                    "and configuring the window does not reach back into the singleton");

            w.dispose();
            DisplayTest.flushEdt();
        } finally {
            singleton.setPosition(originalPosition);
            singleton.setDefaultUIID(originalUiid);
            singleton.setDefaultMessageUIID(originalMessageUiid);
        }
    }

    @FormTest
    void aWindowInstanceKeepsFollowingLaterConfigurationChanges() {
        // Seeding only on the cache miss froze the window's bar at whatever the shared
        // defaults happened to be the first time it was asked for. Every configuration
        // change after that point was silently dropped, though the static helpers have
        // always honoured them.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        ToastBar singleton = ToastBar.getInstance();
        int originalPosition = singleton.getPosition();
        String originalUiid = singleton.getDefaultUIID();
        Window w = new Window("host", new BorderLayout());
        try {
            w.setWindowSize(500, 400);
            w.show();
            DisplayTest.flushEdt();

            // Created before the application configures anything.
            ToastBar forWindow = ToastBar.getForTopLevel(w);
            assertNotSame(singleton, forWindow);

            singleton.setPosition(Component.TOP);
            singleton.setDefaultUIID("LaterToastBar");
            assertEquals(Component.TOP, ToastBar.getForTopLevel(w).getPosition(),
                    "a change made after the window's bar existed still reaches it");
            assertEquals("LaterToastBar", ToastBar.getForTopLevel(w).getDefaultUIID());

            // But something set on the window itself is its own, and a later change to
            // the shared default must not take it back.
            forWindow.setPosition(Component.BOTTOM);
            singleton.setPosition(Component.TOP);
            assertEquals(Component.BOTTOM, ToastBar.getForTopLevel(w).getPosition(),
                    "an explicit per-window setting outranks the shared default");
            assertEquals(Component.TOP, singleton.getPosition(),
                    "and setting it on the window does not reach back into the singleton");
        } finally {
            singleton.setPosition(originalPosition);
            singleton.setDefaultUIID(originalUiid);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void inheritingANewPositionMovesTheToastAlreadyOnScreen() {
        // Synchronising the field is only half of it: the slot is chosen when the
        // component is built or reattached, so a bar already on screen stayed where it
        // was created while animating as though it had moved to the other end.
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        ToastBar singleton = ToastBar.getInstance();
        int originalPosition = singleton.getPosition();
        Window w = new Window("host", new BorderLayout());
        try {
            singleton.setPosition(Component.BOTTOM);
            w.setWindowSize(500, 400);
            w.show();
            DisplayTest.flushEdt();

            ToastBar forWindow = ToastBar.getForTopLevel(w);
            ToastBar.Status s = forWindow.createStatus();
            s.setMessage("hello");
            s.show();
            DisplayTest.flushEdt();

            Component bar = (Component) w.getClientProperty("ToastBarComponent");
            assertNotNull(bar, "precondition: the window has a toast component up");
            Container parent = bar.getParent();
            assertNotNull(parent);
            assertEquals(BorderLayout.SOUTH, parent.getLayout().getComponentConstraint(bar),
                    "precondition: it was built at the bottom");

            singleton.setPosition(Component.TOP);
            // What every static helper starts by doing: resolve the window's bar, which
            // is where the inherited position is taken up.
            ToastBar.getForTopLevel(w);
            DisplayTest.flushEdt();

            assertEquals(BorderLayout.NORTH,
                    bar.getParent().getLayout().getComponentConstraint(bar),
                    "the bar already on screen has to move to the new position");

            s.clear();
            DisplayTest.flushEdt();
        } finally {
            singleton.setPosition(originalPosition);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
}
