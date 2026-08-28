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
        final Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        Button anchor = new Button("anchor");
        w.add(BorderLayout.CENTER, anchor);
        w.show();
        DisplayTest.flushEdt();

        // showPopupDialog blocks until the popup goes, so the popup disposes itself the
        // moment it is on screen. That keeps the whole case on one thread: a background
        // caller parked in invokeAndBlock outlives the test if anything goes wrong.
        final Dialog d = new Dialog();
        final boolean[] hostedOnTheWindow = new boolean[1];
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("popup"));
        d.addShowListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                hostedOnTheWindow[0] = isUnder(w, d);
                d.dispose();
            }
        });
        assertNull(d.getTopLevelHost(), "precondition: no host was named");

        d.showPopupDialog(anchor);
        DisplayTest.flushEdt();

        assertTrue(hostedOnTheWindow[0],
                "a popup takes its host from the anchor, whose coordinate space it points into");
        assertNull(d.getTopLevelHost(),
                "the inferred host belongs to that one showing and must not outlive it");
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aCommandButtonInAWindowHostedDialogReachesTheDialog() {
        // The regression guard for the command host walk. Before it the button reported
        // to the Window, whose command listeners are not the dialog's, so the dialog
        // never learned its own button had been pressed: lastCommandPressed stayed null
        // and autoDispose never fired. Asserting on the dispose is what makes this a
        // test of the routing rather than of the button.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("confirm");
        d.setLayout(new BorderLayout());
        Command ok = new Command("OK");
        Button okButton = new Button(ok);
        d.add(BorderLayout.CENTER, okButton);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "precondition: the dialog is up on the window");

        okButton.pressed();
        okButton.released();
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(),
                "a command from a button inside the dialog has to reach the dialog itself");
        assertNull(d.getParent(), "and take it back out of the window's layer");
        w.dispose();
        DisplayTest.flushEdt();
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

        // Clear the menu flag before disposing. Dialog.dispose() deliberately skips
        // super.dispose() for a menu, so a menu disposed as one never restores the form
        // it replaced -- it would stay Display.getCurrent() for every later test in the
        // JVM, which is a leak this test has no business creating.
        d.setMenu(false);
        d.dispose();
        DisplayTest.flushEdt();
        assertFalse(Display.getInstance().getCurrent() instanceof Dialog,
                "the surface has to be handed back, or the next test inherits this dialog");
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
}
