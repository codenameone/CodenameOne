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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A `Dialog` shown from inside a `Window`.
///
/// Every case here failed before the dialog learned about top levels: it either
/// landed on the main form while the user was looking at a window, or it never
/// came back at all.
class DialogInWindowTest extends UITestBase {

    /// The layer a hosted dialog attaches to, without creating one.
    private static Container hostedLayer(Window w) {
        return w.getFormLayeredPaneIfExists();
    }

    /// Whether the given dialog is somewhere under the given window.
    private static boolean isUnder(Window w, Component c) {
        Component probe = c;
        while (probe != null) {
            if (probe == w) {
                return true;
            }
            probe = probe.getParent();
        }
        return false;
    }

    @FormTest
    void aDialogShownWithNoWindowStillReplacesTheCurrentForm() {
        // The compatibility assertion, and the reason it is written first. With no
        // window system nothing may reach the new path at all.
        assertFalse(Desktop.isSupported());
        Form host = new Form("main", new BorderLayout());
        host.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("legacy");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.showModeless();
        DisplayTest.flushEdt();

        assertSame(d, Display.getInstance().getCurrent(),
                "with no window to host it a dialog still takes over the main surface");
        assertNull(d.getParent(), "and is not parented into anything");
        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDialogShownFromAWindowLandsInThatWindowsLayeredPane() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("in a window");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        assertNotNull(d.getParent(), "the dialog has to be parented into the window");
        assertTrue(isUnder(w, d), "and specifically into the window, not the main form");
        assertSame(main, Display.getInstance().getCurrent(),
                "the main surface must not have been taken over");
        assertNull(main.getFormLayeredPaneIfExists(),
                "and the main form must not have grown a layer for it");

        d.dispose();
        DisplayTest.flushEdt();
        assertNull(d.getParent(), "dispose takes the dialog back out of the layer");
        Container layer = hostedLayer(w);
        assertTrue(layer == null || layer.getComponentCount() == 0,
                "and leaves no layer behind holding it");
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anExplicitHostBeatsTheFocusedWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window a = new Window("a", new BorderLayout());
        a.show();
        Window b = new Window("b", new BorderLayout());
        b.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("explicit");
        d.setLayout(new BorderLayout());
        d.setTopLevelHost(a);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(a, d), "the explicitly named host wins");
        assertFalse(isUnder(b, d));

        d.dispose();
        DisplayTest.flushEdt();
        a.dispose();
        b.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aPopupAnchoredInAWindowInfersThatWindowAndReleasesItAfterwards() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button anchor = new Button("anchor");
        w.add(BorderLayout.CENTER, anchor);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog();
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("popup"));
        assertNull(d.getTopLevelHost(), "precondition: no host was named");

        // showPopupDialog blocks, so drive it from a background thread and dismiss it.
        d.setDisposeWhenPointerOutOfBounds(true);
        final Dialog dlg = d;
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                dlg.showPopupDialog(anchor);
            }
        }, "cn1-test-popup");
        caller.start();
        try {
            for (int i = 0; i < 200 && dlg.getParent() == null; i++) {
                DisplayTest.flushEdt();
                sleepQuietly();
            }
            assertTrue(isUnder(w, dlg),
                    "a popup takes its host from the anchor, whose coordinate space it points into");
        } finally {
            dlg.dispose();
            for (int i = 0; i < 200 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                sleepQuietly();
            }
            joinQuietly(caller);
        }
        DisplayTest.flushEdt();
        assertNull(d.getTopLevelHost(),
                "the inferred host belongs to that one showing and must not outlive it");
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aCommandButtonInAWindowHostedDialogReachesTheDialog() throws Exception {
        // The regression guard for the command host walk. Before it the button told
        // the Window, whose listeners are not the dialog's, so lastCommandPressed
        // stayed null and a modal showDialog() never returned.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        final Dialog d = new Dialog("confirm");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("delete?"));
        Command ok = new Command("OK");
        d.addCommand(ok);
        d.setTopLevelHost(w);

        final Command[] result = new Command[1];
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = d.showDialog();
            }
        }, "cn1-test-dialog-caller");
        caller.start();
        try {
            for (int i = 0; i < 200 && d.getParent() == null; i++) {
                DisplayTest.flushEdt();
                sleepQuietly();
            }
            assertTrue(isUnder(w, d), "precondition: the dialog is up on the window");

            d.dispatchCommand(ok, new com.codename1.ui.events.ActionEvent(ok));
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                sleepQuietly();
            }
            joinQuietly(caller);
            assertFalse(caller.isAlive(),
                    "a command must end the wait, or a modal dialog in a window never returns");
            assertSame(ok, result[0], "and showDialog() has to report which command it was");
        } finally {
            d.dispose();
            DisplayTest.flushEdt();
            joinQuietly(caller);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aHostedDialogIsSizedAgainstItsWindowNotTheDisplay() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        // Deliberately far from the display size, and wider than it is tall, which is
        // the shape that catches anything still asking Display for its measurements.
        Window w = new Window("wide", new BorderLayout());
        w.setWindowSize(1000, 400);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("packed");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showPacked(BorderLayout.CENTER, false);
        DisplayTest.flushEdt();

        assertTrue(isUnder(w, d));
        assertTrue(d.getWidth() <= w.getWidth(),
                "a dialog on a window must fit that window, not the display");
        assertTrue(d.getHeight() <= w.getHeight(),
                "a dialog on a window must fit that window, not the display");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aMenuDialogNeverTakesTheHostedPath() {
        // Display.getCurrent() deliberately looks through a menu, and Dialog.dispose()
        // skips super.dispose() for one. Hosting it would strand it.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("menu");
        d.setLayout(new BorderLayout());
        d.setMenu(true);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        assertFalse(isUnder(w, d), "a menu dialog stays on the main surface");
        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void twoDialogsOnOneWindowStackAndUnstackIndependently() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        w.show();
        DisplayTest.flushEdt();

        Dialog first = new Dialog("first");
        first.setLayout(new BorderLayout());
        first.setTopLevelHost(w);
        first.showModeless();
        DisplayTest.flushEdt();

        Dialog second = new Dialog("second");
        second.setLayout(new BorderLayout());
        second.setTopLevelHost(w);
        second.showModeless();
        DisplayTest.flushEdt();

        assertTrue(isUnder(w, first));
        assertTrue(isUnder(w, second));

        second.dispose();
        DisplayTest.flushEdt();
        assertNull(second.getParent(), "the second dialog leaves");
        assertNotNull(first.getParent(), "and must not take the first one with it");

        first.dispose();
        DisplayTest.flushEdt();
        Container layer = hostedLayer(w);
        assertTrue(layer == null || layer.getComponentCount() == 0,
                "the shared layer goes only once the last dialog has left");
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void disposingAHostedDialogRestoresItsBackgroundPainter() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("painter");
        d.setLayout(new BorderLayout());
        d.setTopLevelHost(w);
        com.codename1.ui.plaf.Style st = d.getStyle();
        Object before = st.getBgPainter();

        d.showModeless();
        DisplayTest.flushEdt();
        d.dispose();
        DisplayTest.flushEdt();

        assertSame(before, st.getBgPainter(),
                "the dialog's own painter has to come back, or a later legacy show paints nothing");
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogCanBeShownAgainAfterDispose() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("reusable");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);

        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d));
        d.dispose();
        DisplayTest.flushEdt();

        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "showing the same dialog a second time has to work");
        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinQuietly(Thread t) {
        try {
            t.join(2000);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }
}
