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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    /// Opens a window with a form behind it, ready to host a dialog.
    private Window openHost(int w, int h) {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window win = new Window("host", new BorderLayout());
        win.setWindowSize(w, h);
        win.show();
        DisplayTest.flushEdt();
        return win;
    }

    @FormTest
    void everyModelessEntryPointReachesTheWindow() {
        // show(), showModeless(), show(t,b,l,r,...), showAtPosition, showPacked and
        // showStretched all funnel through one dispatch point. Each is checked because
        // they do not all reach it the same way -- showPacked and showPopupDialog get
        // there through show(), the rest through Form.showDialog.
        Window w = openHost(700, 600);
        try {
            Dialog a = new Dialog("modeless");
            a.setLayout(new BorderLayout());
            a.add(BorderLayout.CENTER, new Label("body"));
            a.setTopLevelHost(w);
            a.showModeless();
            DisplayTest.flushEdt();
            assertTrue(isUnder(w, a), "showModeless");
            a.dispose();
            DisplayTest.flushEdt();

            Dialog b = new Dialog("packed");
            b.setLayout(new BorderLayout());
            b.add(BorderLayout.CENTER, new Label("body"));
            b.setTopLevelHost(w);
            b.showPacked(BorderLayout.CENTER, false);
            DisplayTest.flushEdt();
            assertTrue(isUnder(w, b), "showPacked");
            b.dispose();
            DisplayTest.flushEdt();

            Dialog c = new Dialog("stretched");
            c.setLayout(new BorderLayout());
            c.add(BorderLayout.CENTER, new Label("body"));
            c.setTopLevelHost(w);
            c.showStretched(BorderLayout.SOUTH, false);
            DisplayTest.flushEdt();
            assertTrue(isUnder(w, c), "showStretched");
            c.dispose();
            DisplayTest.flushEdt();

            Dialog d = new Dialog("margins");
            d.setLayout(new BorderLayout());
            d.add(BorderLayout.CENTER, new Label("body"));
            d.setTopLevelHost(w);
            d.show(20, 20, 20, 20, true, false);
            DisplayTest.flushEdt();
            assertTrue(isUnder(w, d), "show(top,bottom,left,right,includeTitle,modal)");
            d.dispose();
            DisplayTest.flushEdt();

            Dialog e = new Dialog("positioned");
            e.setLayout(new BorderLayout());
            e.add(BorderLayout.CENTER, new Label("body"));
            e.setTopLevelHost(w);
            e.showAtPosition(20, 20, 20, 20, false);
            DisplayTest.flushEdt();
            assertTrue(isUnder(w, e), "showAtPosition");
            e.dispose();
            DisplayTest.flushEdt();
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aModalDialogInAWindowBlocksPressesReachingTheContentBehindIt() throws Exception {
        // The scrim is what makes a hosted dialog modal at all: without something in
        // the layer that responds to pointer events the window hands the press to its
        // content pane and the button underneath fires.
        final Window w = openHost(600, 500);
        final boolean[] pressedBehind = new boolean[1];
        Button behind = new Button("behind");
        behind.addActionListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                pressedBehind[0] = true;
            }
        });
        w.add(BorderLayout.CENTER, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        int bx = behind.getAbsoluteX() + behind.getWidth() / 2;
        int by = behind.getAbsoluteY() + behind.getHeight() / 2;

        // Sanity: the press reaches the button when nothing is over it. Without this
        // the assertion below would pass on a window that ignores input entirely.
        w.pointerPressed(bx, by);
        w.pointerReleased(bx, by);
        DisplayTest.flushEdt();
        assertTrue(pressedBehind[0], "sanity: the button is reachable to begin with");
        pressedBehind[0] = false;

        final Dialog d = new Dialog("modal");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setDisposeWhenPointerOutOfBounds(false);
        d.setTopLevelHost(w);

        // A modal show parks its caller, so it is driven off the dispatch thread.
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                d.show(10, 10, 10, 10, true, true);
            }
        }, "cn1-test-modal-dialog");
        caller.start();
        try {
            // The claim, not the parent: the dialog is added to the layer several
            // steps before the show finishes claiming input, so waiting on the parent
            // alone can catch it half installed -- with the scrim not yet laid out and
            // presses still reaching what is behind.
            for (int i = 0; i < 400
                    && (d.getParent() == null || w.isPointerInputScopeEmpty()); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            assertTrue(isUnder(w, d), "precondition: the modal dialog is up");

            w.pointerPressed(bx, by);
            w.pointerReleased(bx, by);
            DisplayTest.flushEdt();
            assertFalse(pressedBehind[0],
                    "a press aimed through a modal dialog must not reach what is behind it");
        } finally {
            d.dispose();
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            assertFalse(caller.isAlive(), "the modal wait has to end, not leak a thread");
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void pressingOutsideAHostedDialogDismissesItWhenAsked() {
        // The other half of what the scrim is for: it is also the only thing that can
        // deliver an outside press to the dialog. Without it the press goes to the
        // window's content pane and Dialog.pointerReleased never runs.
        Window w = openHost(600, 500);
        Dialog d = new Dialog("dismissable");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setTopLevelHost(w);
        d.show(80, 80, 80, 80, true, false);
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "precondition: the dialog is up");

        // The very top left of the window, which the inset dialog cannot cover.
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(),
                "a press outside the dialog has to reach it and close it");
        assertTrue(d.wasDisposedDueToOutOfBoundsTouch());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aTimeoutDisposesAWindowHostedDialog() {
        // The guard for the animation registration fix: setTimeout registers the dialog
        // as an animation, and inside a window that registration used to reach nobody,
        // so the timeout never elapsed and the dialog stayed up for good.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("timed");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        // Long enough that the dialog is demonstrably up before it elapses.
        d.setTimeout(120);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "precondition: the dialog is up");

        for (int i = 0; i < 200 && !d.isDisposed(); i++) {
            w.repaintAnimations();
            DisplayTest.flushEdt();
            try {
                Thread.sleep(2);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        assertTrue(d.isDisposed(), "the timeout has to elapse inside a window too");
        assertNull(d.getParent());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void theBackKeyDisposesAWindowHostedDialog() {
        // A window has no menu bar to map the back key, so the dialog listens for it.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("backable");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d));

        w.keyReleased(MenuBar.backSK);
        DisplayTest.flushEdt();
        assertTrue(d.isDisposed(), "back has to close a dialog hosted on a window");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aBackCommandIsFiredRatherThanJustDisposing() {
        Window w = openHost(500, 400);
        Dialog d = new Dialog("with back");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        Command back = new Command("Cancel");
        d.setBackCommand(back);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.keyReleased(MenuBar.backSK);
        DisplayTest.flushEdt();
        assertTrue(d.isDisposed());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void resizingTheHostReadjustsAHostedDialog() {
        Window w = openHost(600, 500);
        Dialog d = new Dialog("resizes");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showPacked(BorderLayout.CENTER, false);
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d));

        Desktop.getInstance().windowSizeChanged(w.getWindowId(), 900, 700);
        DisplayTest.flushEdt();

        assertTrue(isUnder(w, d), "a resize must not evict the dialog");
        assertTrue(d.getWidth() <= w.getWidth());
        assertTrue(d.getHeight() <= w.getHeight());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aPopupInAWindowSurvivesAResizeButNotAnOrientationFlip() {
        // showPopupDialog is what turns disposeOnRotation on. A window has no
        // orientation, so firing on any resize at all would close the popup the moment
        // the user dragged the window wider.
        final Window w = openHost(900, 500);
        Button anchor = new Button("anchor");
        w.add(BorderLayout.CENTER, anchor);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        final Dialog d = new Dialog();
        final boolean[] survivedResize = new boolean[1];
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("popup"));
        d.addShowListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                // Wider, still landscape: the popup has to survive this.
                Desktop.getInstance().windowSizeChanged(w.getWindowId(), 1100, 500);
                survivedResize[0] = !d.isDisposed();
                // Now actually flipped, which does close it and ends the blocking show.
                Desktop.getInstance().windowSizeChanged(w.getWindowId(), 500, 1100);
                if (!d.isDisposed()) {
                    d.dispose();
                }
            }
        });
        d.showPopupDialog(anchor);
        DisplayTest.flushEdt();

        assertTrue(survivedResize[0],
                "a resize that keeps the window's shape must not close a popup on it");
        assertTrue(d.isDisposed(), "but an orientation flip does");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void disposingTheHostWindowLeavesNoLiveDialogBehindIt() {
        // Exactly what the device harness does between sizes: open a window, put a
        // dialog in it, dispose the window without disposing the dialog, open another.
        // A dialog still holding listeners on a disposed window would accumulate.
        Window first = openHost(500, 400);
        Dialog d = new Dialog("stranded");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setTopLevelHost(first);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(first, d));

        first.dispose();
        DisplayTest.flushEdt();

        // Opening another window and hosting a fresh dialog has to work.
        Window second = new Window("second", new BorderLayout());
        second.setWindowSize(900, 700);
        second.show();
        DisplayTest.flushEdt();

        Dialog again = new Dialog("second dialog");
        again.setLayout(new BorderLayout());
        again.add(BorderLayout.CENTER, new Label("body"));
        again.setDisposeWhenPointerOutOfBounds(true);
        again.setTopLevelHost(second);
        again.showModeless();
        DisplayTest.flushEdt();

        assertTrue(isUnder(second, again),
                "a second window after a disposed one still hosts a dialog");
        assertFalse(isUnder(second, d), "and the stranded one does not come with it");

        again.dispose();
        DisplayTest.flushEdt();
        second.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void closingTheHostWindowEndsAHostedDialog() {
        // A window closed through its title bar disposes by default and takes the
        // layered pane with it. Without hearing about that a modal caller waits on a
        // dialog whose surface is gone.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("hosted");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "precondition: the dialog is up");

        Desktop.getInstance().windowCloseRequested(w.getWindowId());
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(),
                "the dialog has to end when the window it is on is closed");
        assertNull(d.getParent());
    }

    @FormTest
    void backGoesToTheTopmostHostedDialogAndDoesNotThrow() {
        // Two dialogs on one window each listen for the back key. Dispatch runs them in
        // the order they were shown, so without top-dialog routing the older one closes
        // instead -- and its listener leaving mid-dispatch used to read past the end of
        // the list Window.fireKeyEvent was walking.
        Window w = openHost(600, 500);
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

        w.keyReleased(MenuBar.backSK);
        DisplayTest.flushEdt();

        assertTrue(second.isDisposed(), "back closes the dialog on top");
        assertFalse(first.isDisposed(), "and leaves the one underneath alone");

        w.keyReleased(MenuBar.backSK);
        DisplayTest.flushEdt();
        assertTrue(first.isDisposed(), "the next back closes the next one down");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogTakesItsTintFromTheHost() {
        // ComboBox and the floating action button submenu set the host's tint to zero
        // to opt out of dimming, and the historical path honours that. Reading the
        // dialog's own tint instead would dim anyway.
        Window w = openHost(600, 500);
        w.setTintColor(0);
        Dialog d = new Dialog("undimmed");
        d.setLayout(new BorderLayout());
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        Container layer = w.getFormLayeredPaneIfExists();
        assertNotNull(layer);
        assertEquals(0, w.getTintColor(),
                "the host's opt-out is what the scrim has to read");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aReusedHostedDialogKeepsThePointerListenersItWasBuiltWith() {
        // An embedded form hands its pointer listeners to its host and clears its own
        // dispatchers. Taking them off the host without putting them back would lose
        // them outright, so the second showing would have none of them.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("reused");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        final int[] presses = new int[1];
        d.addPointerPressedListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                presses[0]++;
            }
        });
        d.setTopLevelHost(w);

        d.showModeless();
        DisplayTest.flushEdt();
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        int afterFirst = presses[0];
        assertTrue(afterFirst > 0, "the listener fires while the dialog is up");

        d.dispose();
        DisplayTest.flushEdt();
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertEquals(afterFirst, presses[0],
                "and stops firing once the dialog has gone");

        d.showModeless();
        DisplayTest.flushEdt();
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertTrue(presses[0] > afterFirst,
                "a dialog shown again still has the listeners it was built with");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDialogWithNoBackgroundPainterGetsItsNullBack() {
        // Whether one was saved, not whether it was non-null: a style can legitimately
        // have no painter, and the no-op stand-in would otherwise stay for good.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("no painter");
        d.setLayout(new BorderLayout());
        d.getStyle().setBgPainter(null);
        d.setTopLevelHost(w);

        d.showModeless();
        DisplayTest.flushEdt();
        d.dispose();
        DisplayTest.flushEdt();

        assertNull(d.getStyle().getBgPainter(),
                "a dialog that had no painter must not be left with the no-op one");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void hidingTheHostReleasesAModalHostedDialog() throws Exception {
        // A hidden window cannot be reached, so a modal dialog on it can never be
        // dismissed and its caller would wait for good.
        final Window w = openHost(500, 400);
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        final Dialog d = new Dialog("modal");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);

        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                d.show(10, 10, 10, 10, true, true);
            }
        }, "cn1-test-hidden-host");
        caller.start();
        try {
            // The claim, not the parent: the dialog is added to the layer several
            // steps before the show finishes claiming input, so waiting on the parent
            // alone can catch it half installed -- with the scrim not yet laid out and
            // presses still reaching what is behind.
            for (int i = 0; i < 400
                    && (d.getParent() == null || w.isPointerInputScopeEmpty()); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            assertTrue(isUnder(w, d), "precondition: the modal dialog is up");

            w.hide();
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            assertFalse(caller.isAlive(),
                    "hiding the host has to release a caller parked on a dialog it can "
                            + "no longer reach");
            assertTrue(d.isDisposed());
        } finally {
            d.dispose();
            DisplayTest.flushEdt();
            caller.join(2000);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void hidingTheHostKeepsAModelessHostedDialogForWhenItComesBack() {
        // The other half, and the reason Hidden is not simply terminal: a window hidden
        // through HIDE_ON_CLOSE is kept alive to be shown again, and throwing away what
        // is in its layered pane between the two would lose the content silently.
        Window w = openHost(500, 400);
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        Dialog d = new Dialog("modeless");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d));

        w.hide();
        DisplayTest.flushEdt();
        assertFalse(d.isDisposed(),
                "nobody is waiting on a modeless dialog, so it survives the hide");
        assertTrue(isUnder(w, d), "and is still there for the next show");

        w.show();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "which is what the user sees when the window returns");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void minimizingTheHostKeepsItsDialogs() {
        // Minimizing clears nativeVisible too, but arrives as Minimized rather than
        // Hidden -- a window the user shrank still has its dialogs when it comes back.
        Window w = openHost(500, 400);
        Dialog d = new Dialog("survives minimize");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.minimize();
        DisplayTest.flushEdt();
        assertFalse(d.isDisposed(), "a minimized window keeps what is on it");
        assertTrue(isUnder(w, d));

        w.restore();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d));

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogWithNothingFocusableStillTakesTheKeyboard() {
        // The scrim swallows presses but keys go to whatever the window last focused.
        // A dialog with no focusable child of its own -- which is exactly what
        // InfiniteProgress builds -- left the control behind it fully operable.
        Window w = openHost(600, 500);
        final int[] pressesBehind = new int[1];
        Button behind = new Button("behind") {
            @Override
            public void keyPressed(int keyCode) {
                pressesBehind[0]++;
                super.keyPressed(keyCode);
            }
        };
        w.add(BorderLayout.CENTER, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        w.setFocused(behind);
        DisplayTest.flushEdt();

        Dialog d = new Dialog("no focusables");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("please wait"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.keyPressed('a');
        w.keyReleased('a');
        DisplayTest.flushEdt();
        assertEquals(0, pressesBehind[0],
                "a key must not reach a control behind a dialog that covers it");

        d.dispose();
        DisplayTest.flushEdt();
        w.keyPressed('a');
        DisplayTest.flushEdt();
        assertTrue(pressesBehind[0] > 0,
                "and the control is reachable again once the dialog has gone");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aModelessHostedDialogStillDimsWhatIsBehindIt() {
        // Form.showModal tints the previous surface whether or not the dialog blocks,
        // so a modeless Dialog has always dimmed. Gating the scrim on modality dropped
        // that, and with it the tint InfiniteProgress configures before showing.
        Window w = openHost(600, 500);
        Dialog d = new Dialog("modeless");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setDisposeWhenPointerOutOfBounds(false);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        // The Dialog class layer inside the window's overlay, not the overlay itself:
        // the overlay's children are the per-class layers.
        Container layer = w.getFormLayeredPane(Dialog.class, true);
        assertNotNull(layer);
        assertEquals(2, layer.getComponentCount(),
                "a backdrop and the dialog, not the dialog on its own");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aPointerListenerAddedWhileHostedStillFires() {
        // Initialization hands over the listeners that existed at that instant and
        // clears the dialog's dispatchers. One added afterwards -- from onShow, say --
        // would sit in a dispatcher the window never consults.
        Window w = openHost(600, 500);
        final int[] presses = new int[1];
        Dialog d = new Dialog("late listener");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        com.codename1.ui.events.ActionListener late =
                new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                presses[0]++;
            }
        };
        d.addPointerPressedListener(late);
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertTrue(presses[0] > 0, "a listener added while hosted has to fire");

        int before = presses[0];
        d.removePointerPressedListener(late);
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertEquals(before, presses[0], "and removing it has to stop it");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anOlderDialogClosingDoesNotTakeTheKeyboardFromANewerOne() {
        // Hosted dialogs do not necessarily close in the order they opened: a timed one
        // can expire while a newer one is up. With each remembering what was in force
        // when it arrived, the older one put its own answer back over the newer one's
        // and the newer one later restored a scope pointing at a detached hierarchy.
        Window w = openHost(600, 500);
        final int[] pressesBehind = new int[1];
        Button behind = new Button("behind") {
            @Override
            public void keyPressed(int keyCode) {
                pressesBehind[0]++;
                super.keyPressed(keyCode);
            }
        };
        w.add(BorderLayout.CENTER, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        w.setFocused(behind);
        DisplayTest.flushEdt();

        Dialog older = new Dialog("older");
        older.setLayout(new BorderLayout());
        older.add(BorderLayout.CENTER, new Label("older"));
        older.setTopLevelHost(w);
        older.showModeless();
        DisplayTest.flushEdt();

        Dialog newer = new Dialog("newer");
        newer.setLayout(new BorderLayout());
        newer.add(BorderLayout.CENTER, new Label("newer"));
        newer.setTopLevelHost(w);
        newer.showModeless();
        DisplayTest.flushEdt();

        // The older one expires underneath the newer one.
        older.dispose();
        DisplayTest.flushEdt();
        assertFalse(w.isKeyInputScopeEmpty(),
                "the newer dialog still owns the keyboard");
        w.keyPressed('a');
        w.keyReleased('a');
        DisplayTest.flushEdt();
        assertEquals(0, pressesBehind[0],
                "and a key must not reach the control the newer dialog covers");

        newer.dispose();
        DisplayTest.flushEdt();
        assertTrue(w.isKeyInputScopeEmpty(), "the keyboard is free once both have gone");
        w.keyPressed('a');
        DisplayTest.flushEdt();
        assertTrue(pressesBehind[0] > 0, "and the control is reachable again");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void theHostsOwnPointerListenersDoNotRunUnderAModalDialog() {
        // The historical path replaced the surface, so the surface's listeners were
        // simply unreachable while a dialog was up. Hosted in a layer the window stays
        // current and its listeners run before anything is hit tested, so one that
        // consumed the press took it away from the dialog altogether.
        Window w = openHost(600, 500);
        final int[] hostPresses = new int[1];
        w.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hostPresses[0]++;
                evt.consume();
            }
        });
        w.addPointerReleasedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hostPresses[0]++;
                evt.consume();
            }
        });

        w.pointerPressed(10, 10);
        w.pointerReleased(10, 10);
        DisplayTest.flushEdt();
        assertTrue(hostPresses[0] > 0, "the window's own listeners work normally");

        Dialog d = new Dialog("modal");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        int before = hostPresses[0];
        w.pointerPressed(10, 10);
        w.pointerReleased(10, 10);
        DisplayTest.flushEdt();
        assertEquals(before, hostPresses[0],
                "a press meant for the dialog must not reach the window's listeners");

        d.dispose();
        DisplayTest.flushEdt();
        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertTrue(hostPresses[0] > before,
                "and they work again once the dialog has gone");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aListenerOnTheDialogItselfStillRunsWhileItOwnsThePointer() {
        // The claim suppresses the window's own listeners, not the ones the application
        // registered on the dialog -- those are transferred onto the window when the
        // dialog is embedded, and suppressing them too would break every hosted dialog
        // that listens for its own presses.
        Window w = openHost(600, 500);
        final int[] dialogPresses = new int[1];

        Dialog d = new Dialog("listening");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dialogPresses[0]++;
            }
        });
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertTrue(dialogPresses[0] > 0,
                "the dialog's own pointer listener has to keep working");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aPressDoesNotRunTheDialogsReleaseOrDragListeners() {
        // The exemptions are held in one list for every kind and every overlay, so
        // firing it whole ran a dialog's release and drag listeners on a press.
        Window w = openHost(600, 500);
        final int[] pressed = new int[1];
        final int[] released = new int[1];
        final int[] dragged = new int[1];

        Dialog d = new Dialog("one dialog");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pressed[0]++;
            }
        });
        d.addPointerReleasedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                released[0]++;
            }
        });
        d.addPointerDraggedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dragged[0]++;
            }
        });
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, pressed[0], "the press listener runs");
        assertEquals(0, released[0], "the release listener must not run on a press");
        assertEquals(0, dragged[0], "and neither must the drag listener");

        w.pointerReleased(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, released[0], "the release listener runs on a release");
        assertEquals(1, pressed[0]);

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void onlyTheTopmostDialogHearsThePointer() {
        // Every overlay that ever handed listeners over is in the same list, so without
        // filtering by owner a dialog stacked underneath kept hearing presses meant for
        // the one covering it.
        Window w = openHost(600, 500);
        final int[] lowerPresses = new int[1];
        final int[] upperPresses = new int[1];

        Dialog lower = new Dialog("lower");
        lower.setLayout(new BorderLayout());
        lower.add(BorderLayout.CENTER, new Label("lower"));
        lower.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerPresses[0]++;
            }
        });
        lower.setTopLevelHost(w);
        lower.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, lowerPresses[0], "the only dialog up hears the press");

        Dialog upper = new Dialog("upper");
        upper.setLayout(new BorderLayout());
        upper.add(BorderLayout.CENTER, new Label("upper"));
        upper.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                upperPresses[0]++;
            }
        });
        upper.setTopLevelHost(w);
        upper.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, upperPresses[0], "the dialog on top hears it");
        assertEquals(1, lowerPresses[0], "the one underneath must not");

        upper.dispose();
        DisplayTest.flushEdt();
        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(2, lowerPresses[0],
                "and hears it again once the one above has gone");

        lower.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDialogShownMidGestureDoesNotLetTheReleaseActivateWhatIsBehindIt() {
        // Showing a dialog from a press is ordinary, and a blocking progress dialog
        // goes up while the finger is still down. The window kept holding the button
        // underneath as the release target, so lifting over the new dialog fired it.
        Window w = openHost(600, 500);
        final int[] activations = new int[1];
        Button behind = new Button("behind");
        behind.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                activations[0]++;
            }
        });
        w.add(BorderLayout.CENTER, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        int x = behind.getAbsoluteX() + behind.getWidth() / 2;
        int y = behind.getAbsoluteY() + behind.getHeight() / 2;

        // The press lands on the button, and the dialog goes up before the release.
        w.pointerPressed(x, y);
        DisplayTest.flushEdt();

        Dialog d = new Dialog("in the way");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertEquals(0, activations[0],
                "the control behind the dialog must not fire on the release");

        d.dispose();
        DisplayTest.flushEdt();

        // A gesture that starts after the dialog has gone still works.
        w.pointerPressed(x, y);
        w.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertEquals(1, activations[0], "and an ordinary press still activates it");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aListenerSharedWithTheWindowSurvivesTheDialogClosing() {
        // EventDispatcher ignores a listener it already holds, so handing over one the
        // window already had added nothing -- and taking it back on the way out removed
        // the window's own registration, silently killing a listener the application
        // never attached to the dialog.
        Window w = openHost(600, 500);
        final int[] presses = new int[1];
        ActionListener shared = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                presses[0]++;
            }
        };
        w.addPointerPressedListener(shared);

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, presses[0], "the window's own listener works");

        Dialog d = new Dialog("shares a handler");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addPointerPressedListener(shared);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(2, presses[0], "and still fires exactly once while the dialog is up");

        d.dispose();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(3, presses[0],
                "the window's listener must outlive the dialog that shared it");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void showingAHostedDialogAgainReplacesItRatherThanStrandingAScrim() {
        // Still parented to the old layer, so adding it again threw -- but only after a
        // second scrim had been built and the field pointing at the first overwritten,
        // leaving that one covering the window and swallowing every press.
        Window w = openHost(600, 500);
        final int[] behindPresses = new int[1];
        w.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                behindPresses[0]++;
            }
        });

        Dialog d = new Dialog("shown twice");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertNotNull(d.getParent());

        d.showModeless();
        DisplayTest.flushEdt();
        assertNotNull(d.getParent(), "the second showing is attached");

        d.dispose();
        DisplayTest.flushEdt();
        assertNull(d.getParent(), "and disposal takes it back out");

        Container layer = w.getFormLayeredPane(Dialog.class, true);
        assertEquals(0, layer.getComponentCount(),
                "with no scrim stranded in the layer behind it");

        // Which the window can prove: presses reach it again.
        int before = behindPresses[0];
        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertTrue(behindPresses[0] > before,
                "and the window is interactive again");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void removingAListenerTheDialogNeverAddedLeavesTheWindowsOwnAlone() {
        // Removing a listener you never registered has always been a no-op. While
        // hosted it was routed to the window and removed there, deleting a listener the
        // application had attached to the window itself.
        Window w = openHost(600, 500);
        final int[] presses = new int[1];
        ActionListener windowOnly = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                presses[0]++;
            }
        };
        w.addPointerPressedListener(windowOnly);

        Dialog d = new Dialog("meddling");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        // The dialog never registered this one.
        d.removePointerPressedListener(windowOnly);
        DisplayTest.flushEdt();

        d.dispose();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, presses[0],
                "the window's own listener must still be there");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogsOwnKeyListenersStillFire() {
        // A window dispatches keys through its own map and never looks at a nested
        // form's, so an application's shortcuts stopped working the moment the dialog
        // was hosted rather than shown the historical way.
        Window w = openHost(600, 500);
        final int[] shortcut = new int[1];
        final int[] addedLater = new int[1];

        Dialog d = new Dialog("shortcuts");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addKeyListener('s', new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                shortcut[0]++;
            }
        });
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.keyPressed('s');
        w.keyReleased('s');
        DisplayTest.flushEdt();
        assertEquals(1, shortcut[0], "a shortcut registered before showing fires");

        // And one registered afterwards, which is what onShow would do.
        d.addKeyListener('t', new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                addedLater[0]++;
            }
        });
        w.keyPressed('t');
        w.keyReleased('t');
        DisplayTest.flushEdt();
        assertEquals(1, addedLater[0], "and so does one registered after");

        d.dispose();
        DisplayTest.flushEdt();
        w.keyPressed('s');
        w.keyReleased('s');
        DisplayTest.flushEdt();
        assertEquals(1, shortcut[0],
                "but not once the dialog has gone");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void onlyTheTopmostDialogsShortcutsFire() {
        // Every hosted dialog publishes onto the same window, so without scoping a
        // dialog covered by another would still be running its shortcuts.
        Window w = openHost(600, 500);
        final int[] lower = new int[1];
        final int[] upper = new int[1];

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.addKeyListener('x', new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lower[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        w.keyPressed('x');
        w.keyReleased('x');
        DisplayTest.flushEdt();
        assertEquals(1, lower[0]);

        Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        high.add(BorderLayout.CENTER, new Label("upper"));
        high.addKeyListener('y', new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                upper[0]++;
            }
        });
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();

        w.keyPressed('x');
        w.keyReleased('x');
        w.keyPressed('y');
        w.keyReleased('y');
        DisplayTest.flushEdt();
        assertEquals(1, lower[0], "the covered dialog's shortcut must not fire");
        assertEquals(1, upper[0], "the one on top must");

        high.dispose();
        DisplayTest.flushEdt();
        w.keyPressed('x');
        w.keyReleased('x');
        DisplayTest.flushEdt();
        assertEquals(2, lower[0], "and works again once uncovered");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogsDefaultCommandFiresOnEnter() {
        // Form.keyReleased runs the default command on a fire key. A window forwards a
        // release to the focused component and the key listeners and nothing else, so
        // a hosted dialog lost its standard keyboard default.
        Window w = openHost(600, 500);
        final int[] fired = new int[1];

        Dialog d = new Dialog("has a default");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        Command ok = new Command("OK") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        };
        d.setDefaultCommand(ok);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertTrue(fired[0] > 0, "the dialog's default command has to run on a fire key");

        d.dispose();
        DisplayTest.flushEdt();
        int before = fired[0];
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(before, fired[0], "and not once the dialog has gone");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void addingTheSameKeyListenerTwiceLeavesNothingBehindOnRemoval() {
        // The form's map ignores a duplicate, so publishing on every call put two
        // wrappers on the host for one registration -- and the single removal that
        // matches it left the other wrapper calling a listener the application had
        // removed.
        Window w = openHost(600, 500);
        final int[] hits = new int[1];
        ActionListener shortcut = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hits[0]++;
            }
        };

        Dialog d = new Dialog("double registered");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        d.addKeyListener('z', shortcut);
        d.addKeyListener('z', shortcut);
        w.keyPressed('z');
        w.keyReleased('z');
        DisplayTest.flushEdt();
        assertEquals(1, hits[0], "one registration means one call");

        d.removeKeyListener('z', shortcut);
        w.keyPressed('z');
        w.keyReleased('z');
        DisplayTest.flushEdt();
        assertEquals(1, hits[0], "and removing it stops it completely");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void oneDialogRemovingASharedListenerLeavesTheOthersExemptionAlone() {
        // Two overlays can share a listener instance for the same event. Matching only
        // the pair let the dialog on top delete the entry belonging to the one
        // underneath, which then had its own listener suppressed for good once it owned
        // the pointer again.
        Window w = openHost(600, 500);
        final int[] lowerHits = new int[1];
        ActionListener shared = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerHits[0]++;
            }
        };

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.addPointerPressedListener(shared);
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, lowerHits[0]);

        Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        high.add(BorderLayout.CENTER, new Label("upper"));
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();

        // The top dialog removes a listener it never registered, which the underlying
        // one is using.
        high.removePointerPressedListener(shared);
        DisplayTest.flushEdt();

        high.dispose();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(2, lowerHits[0],
                "the dialog underneath keeps its own listener");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aCancelledPressLeavesNothingWaitingForARelease() {
        // The cancelled press is never released, so the component it left in the
        // awaiting-release list stays there. The next gesture's list then holds two,
        // which takes autoRelease out of its single-component branch -- so dragging off
        // the newly pressed button no longer cancels it and the release still fires.
        Window w = openHost(600, 500);
        final int[] secondFired = new int[1];
        Button first = new Button("first");
        Button second = new Button("second");
        second.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                secondFired[0]++;
            }
        });
        Container row = new Container(new com.codename1.ui.layouts.BoxLayout(
                com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        row.add(first);
        row.add(second);
        w.add(BorderLayout.CENTER, row);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        // A press on the first button, interrupted by a dialog before its release.
        w.pointerPressed(first.getAbsoluteX() + 2, first.getAbsoluteY() + 2);
        DisplayTest.flushEdt();

        Dialog d = new Dialog("interrupting");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        d.dispose();
        DisplayTest.flushEdt();

        // Now an ordinary gesture on the second button, dragged well off it.
        int sx = second.getAbsoluteX() + 2;
        int sy = second.getAbsoluteY() + 2;
        w.pointerPressed(sx, sy);
        w.pointerDragged(new int[] {sx}, new int[] {sy + 400});
        w.pointerReleased(sx, sy + 400);
        DisplayTest.flushEdt();

        assertEquals(0, secondFired[0],
                "a press dragged off its button must not fire, whatever an earlier "
                        + "cancelled gesture left behind");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void addingTheSamePointerListenerTwiceWhileHostedStillCallsItOnce() {
        // Registered twice is registered once, as it is on any dispatcher. The host's
        // own collection ignores the duplicate, but the exemption list is walked
        // directly while an overlay owns the pointer, so a second entry meant one press
        // called the listener twice.
        Window w = openHost(600, 500);
        final int[] hits = new int[1];
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hits[0]++;
            }
        };

        Dialog d = new Dialog("double registered");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        d.addPointerPressedListener(l);
        d.addPointerPressedListener(l);
        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, hits[0], "one registration means one call");

        d.removePointerPressedListener(l);
        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, hits[0], "and removing it stops it completely");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void oneEnterDoesNotDismissTwoStackedDialogs() {
        // The focused control in the top dialog is free to close it, which releases the
        // key scope. Reading the scope after that found the dialog underneath newly
        // exposed and ran its default command for the same release.
        Window w = openHost(600, 500);
        final int[] lowerDefault = new int[1];

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.setDefaultCommand(new Command("lower default") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                lowerDefault[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        Button ok = new Button("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                high.dispose();
            }
        });
        high.add(BorderLayout.CENTER, ok);
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();
        w.setFocused(ok);
        DisplayTest.flushEdt();

        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();

        assertTrue(high.isDisposed(), "the top dialog closes on its own OK button");
        assertEquals(0, lowerDefault[0],
                "and the one it uncovers must not act on the same key release");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogsGameKeyShortcutsFireToo() {
        // A form dispatches both the raw code and the game action; a window that was
        // given only the raw map left every game key shortcut on a hosted dialog dead.
        Window w = openHost(600, 500);
        final int[] gameHits = new int[1];
        ActionListener onFire = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                gameHits[0]++;
            }
        };

        Dialog d = new Dialog("game keys");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addGameKeyListener(Display.GAME_FIRE, onFire);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, gameHits[0], "a game key shortcut has to fire");

        // Added after showing, the way onShow would.
        final int[] later = new int[1];
        d.addGameKeyListener(Display.GAME_LEFT, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                later[0]++;
            }
        });
        int left = Display.getInstance().getKeyCode(Display.GAME_LEFT);
        w.keyPressed(left);
        w.keyReleased(left);
        DisplayTest.flushEdt();
        assertEquals(1, later[0], "and so does one registered after the show");

        d.removeGameKeyListener(Display.GAME_FIRE, onFire);
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, gameHits[0], "removing it stops it");

        d.dispose();
        DisplayTest.flushEdt();
        w.keyPressed(left);
        w.keyReleased(left);
        DisplayTest.flushEdt();
        assertEquals(1, later[0], "and disposal takes the rest away");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void oneEnterDoesNotRunTheUncoveredDialogsShortcut() {
        // The wrappers check who owns the keyboard as they run, and by the time they do
        // the top dialog may have closed itself -- so the dialog it uncovered answered a
        // release whose press it never saw.
        Window w = openHost(600, 500);
        final int[] lowerShortcut = new int[1];
        final int[] lowerGame = new int[1];

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        low.addKeyListener(enter, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerShortcut[0]++;
            }
        });
        low.addGameKeyListener(Display.GAME_FIRE, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerGame[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        Button ok = new Button("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                high.dispose();
            }
        });
        high.add(BorderLayout.CENTER, ok);
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();
        w.setFocused(ok);
        DisplayTest.flushEdt();

        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();

        assertTrue(high.isDisposed());
        assertEquals(0, lowerShortcut[0],
                "the uncovered dialog's raw shortcut must not run");
        assertEquals(0, lowerGame[0], "nor its game key one");

        // It works normally once it is the one being used.
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, lowerShortcut[0], "and works when the key is really for it");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aCancelledGestureDoesNotDeliverItsReleaseToTheNewDialog() {
        // The press was taken away by the dialog, so the rest of that gesture is not
        // addressed to it: neither the long press that was still pending nor the
        // eventual physical release.
        Window w = openHost(600, 500);
        final int[] dialogReleases = new int[1];
        final int[] dialogLongPresses = new int[1];

        Button behind = new Button("behind");
        w.add(BorderLayout.CENTER, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        int x = behind.getAbsoluteX() + 2;
        int y = behind.getAbsoluteY() + 2;
        w.pointerPressed(x, y);
        DisplayTest.flushEdt();

        Dialog d = new Dialog("interrupting");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addPointerReleasedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dialogReleases[0]++;
            }
        });
        d.addLongPressListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dialogLongPresses[0]++;
            }
        });
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        // The gesture that was already under way finishes.
        w.longPointerPress(x, y);
        w.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertEquals(0, dialogLongPresses[0],
                "a long press the dialog was never pressed on must not reach it");
        assertEquals(0, dialogReleases[0],
                "and neither must the release of that gesture");

        // A gesture that starts on the dialog reaches it normally.
        w.pointerPressed(x, y);
        w.pointerReleased(x, y);
        DisplayTest.flushEdt();
        assertEquals(1, dialogReleases[0], "a new gesture does reach it");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void twoDialogsSharingAListenerBothKeepItWhenTheOtherCloses() {
        // A flag saying "handing it over is what put it there" could not express two
        // owners: the first dialog recorded that it had added the listener and the
        // second that it had not, so disposing the first removed the only registration
        // and the second stopped hearing anything.
        Window w = openHost(600, 500);
        final int[] hits = new int[1];
        ActionListener shared = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                hits[0]++;
            }
        };

        Dialog first = new Dialog("first");
        first.setLayout(new BorderLayout());
        first.add(BorderLayout.CENTER, new Label("first"));
        first.addPointerPressedListener(shared);
        first.setTopLevelHost(w);
        first.showModeless();
        DisplayTest.flushEdt();

        Dialog second = new Dialog("second");
        second.setLayout(new BorderLayout());
        second.add(BorderLayout.CENTER, new Label("second"));
        second.addPointerPressedListener(shared);
        second.setTopLevelHost(w);
        second.showModeless();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(1, hits[0],
                "only the dialog on top hears the press, and it hears it once");

        // The one underneath closes while the top one is still up.
        first.dispose();
        DisplayTest.flushEdt();

        w.pointerPressed(10, 10);
        DisplayTest.flushEdt();
        assertEquals(2, hits[0],
                "the dialog still showing keeps its own registration");

        second.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDialogClosedFromTheKeyPressDoesNotHandItsReleaseDownwards() {
        // A control that acts on the press rather than the release closes its dialog
        // before the release arrives, so by then the dialog underneath has been
        // uncovered and focused. Sampling the scope at release time finds that one --
        // the earlier fix read it before the release handler ran, which is too late
        // when the press is what closed things.
        Window w = openHost(600, 500);
        final int[] lowerDefault = new int[1];
        final int[] lowerShortcut = new int[1];
        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.setDefaultCommand(new Command("lower default") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                lowerDefault[0]++;
            }
        });
        low.addKeyListener(enter, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerShortcut[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        // Acts on the press, not the release.
        Button ok = new Button("OK") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                high.dispose();
            }
        };
        high.add(BorderLayout.CENTER, ok);
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();
        w.setFocused(ok);
        DisplayTest.flushEdt();

        w.keyPressed(enter);
        DisplayTest.flushEdt();
        assertTrue(high.isDisposed(), "the press closed the dialog on top");

        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(0, lowerDefault[0],
                "the dialog it uncovered must not run its default command");
        assertEquals(0, lowerShortcut[0], "nor its shortcut");

        // A whole keystroke of its own still works.
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, lowerDefault[0], "and it works for a key really meant for it");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aPressListenerThatShowsADialogDoesNotHandItTheRelease() {
        // Showing a dialog from a press is routine, and a blocking progress dialog goes
        // up while the finger is still down. The overlay ends the gesture as it takes
        // the pointer, but the press was still being dispatched: carrying on to hit
        // testing afterwards resolves the scrim that was not there when the finger went
        // down and makes it the release target, so the lift dismisses a dialog this
        // press never touched.
        final Window w = openHost(600, 500);
        final Dialog[] shown = new Dialog[1];
        final boolean[] once = new boolean[1];
        w.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (once[0]) {
                    return;
                }
                once[0] = true;
                Dialog over = new Dialog("over");
                over.setLayout(new BorderLayout());
                over.add(BorderLayout.CENTER, new Label("over"));
                over.setDisposeWhenPointerOutOfBounds(true);
                over.setTopLevelHost(w);
                shown[0] = over;
                // Deliberately not consumed: a listener that puts UI up has no reason
                // to claim the event, and that is the case this guards.
                // Inset and modeless, so the card cannot cover the corner pressed
                // below and that press lands on the backdrop.
                over.show(80, 80, 80, 80, true, false);
            }
        });
        DisplayTest.flushEdt();

        // A corner, so it is outside the dialog card both when the finger goes down and
        // when it comes up -- which is exactly what arms dispose-out-of-bounds.
        w.pointerPressed(2, 2);
        DisplayTest.flushEdt();
        assertNotNull(shown[0], "the listener showed a dialog");
        assertFalse(shown[0].isDisposed(), "which is up before the finger lifts");
        // The Dialog container spans the window; the card is the title and content
        // inside it, which is what dispose-out-of-bounds measures against.
        assertFalse(shown[0].getContentPane().containsOrOwns(2, 2)
                        || shown[0].getTitleComponent().containsOrOwns(2, 2)
                        || shown[0].getMenuBar().containsOrOwns(2, 2),
                "sanity: the corner pressed is on the backdrop, not on the card");

        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertFalse(shown[0].isDisposed(),
                "the lift must not dismiss a dialog the press never touched");

        // A whole gesture of its own still dismisses it, so this is a scoping fix and
        // not a switch that turns dispose-out-of-bounds off.
        w.pointerPressed(2, 2);
        DisplayTest.flushEdt();
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertTrue(shown[0].isDisposed(), "a press and release of its own still works");

        w.dispose();
        DisplayTest.flushEdt();
    }
    @FormTest
    void aDialogOpenedByAKeyReleaseDoesNotAlsoHandleThatKey() {
        // Nothing holds the keyboard when the key arrives, so the release belongs to the
        // window itself. A focused control opening a dialog from it is ordinary -- and
        // the dialog publishes its shortcuts onto this same window as it goes up. The
        // listeners are dispatched from two snapshots, so one taken after the dialog
        // appeared contains it, and the key that opened the dialog then ran its Enter
        // shortcut: the dialog shut again before it was ever seen.
        final Window w = openHost(600, 500);
        final int[] closedByShortcut = new int[1];
        final Dialog[] opened = new Dialog[1];
        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        final int enterCode = enter;

        Button opener = new Button("open") {
            @Override
            public void keyReleased(int keyCode) {
                super.keyReleased(keyCode);
                if (opened[0] != null) {
                    return;
                }
                final Dialog d = new Dialog("opened");
                d.setLayout(new BorderLayout());
                d.add(BorderLayout.CENTER, new Label("body"));
                d.addKeyListener(enterCode, new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        closedByShortcut[0]++;
                        d.dispose();
                    }
                });
                d.setTopLevelHost(w);
                opened[0] = d;
                d.showModeless();
            }
        };
        w.add(BorderLayout.CENTER, opener);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        w.setFocused(opener);
        DisplayTest.flushEdt();

        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();

        assertNotNull(opened[0], "the release opened a dialog");
        assertEquals(0, closedByShortcut[0],
                "the key that opened it must not also run its shortcut");
        assertFalse(opened[0].isDisposed(), "so the dialog is still up");

        // Its own keystroke still reaches it, so this scopes the dispatch rather than
        // silencing the dialog.
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, closedByShortcut[0], "a keystroke of its own still works");
        assertTrue(opened[0].isDisposed());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void overlappingKeysEachKeepTheirOwnReleaseScope() {
        // Keys overlap. Hold one down, press another on a control that closes the
        // dialog, release that one, then release the first. A single saved slot was
        // consumed by the second key's release, so the first key's release fell back to
        // whatever had just been uncovered -- and ran its shortcut for a press it never
        // saw.
        final Window w = openHost(600, 500);
        final int[] lowerShortcut = new int[1];
        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        int held = 'x';

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.addKeyListener(held, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerShortcut[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        Button ok = new Button("OK") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                high.dispose();
            }
        };
        high.add(BorderLayout.CENTER, ok);
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();
        w.setFocused(ok);
        DisplayTest.flushEdt();

        w.keyPressed(held);            // goes down while the upper dialog owns the keys
        DisplayTest.flushEdt();
        w.keyPressed(enter);           // and this one closes it
        DisplayTest.flushEdt();
        assertTrue(high.isDisposed(), "the second press closed the dialog on top");
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        w.keyReleased(held);
        DisplayTest.flushEdt();

        assertEquals(0, lowerShortcut[0],
                "the uncovered dialog must not answer a key it never saw pressed");

        // Its own press and release still reach it.
        w.keyPressed(held);
        DisplayTest.flushEdt();
        w.keyReleased(held);
        DisplayTest.flushEdt();
        assertEquals(1, lowerShortcut[0], "a keystroke of its own still works");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aRepeatDoesNotHandTheHeldKeysReleaseToWhatComesNext() {
        // Holding a key makes the window synthesize a press/release pair per repeat, for
        // a key that is still physically down. Consuming the record on those releases
        // left the hardware release with nothing: it fell back to whoever held the
        // keyboard by then, so a dialog that went away under a held key handed its
        // release to the surface it uncovered.
        final Window w = openHost(600, 500);
        final int[] lowerShortcut = new int[1];
        int held = 'x';

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        low.add(BorderLayout.CENTER, new Label("lower"));
        low.addKeyListener(held, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lowerShortcut[0]++;
            }
        });
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        high.add(BorderLayout.CENTER, new Label("upper"));
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();

        // Down while the upper dialog owns the keyboard, then held long enough to repeat.
        w.keyPressed(held);
        DisplayTest.flushEdt();
        w.keyRepeated(held);
        w.keyRepeated(held);
        DisplayTest.flushEdt();

        // It goes away with the key still down -- a timeout does exactly this.
        high.dispose();
        DisplayTest.flushEdt();

        // The hardware release finally arrives.
        w.keyReleased(held);
        DisplayTest.flushEdt();
        assertEquals(0, lowerShortcut[0],
                "the uncovered dialog must not answer a key it never saw pressed");

        // And a whole keystroke of its own still reaches it.
        w.keyPressed(held);
        DisplayTest.flushEdt();
        w.keyReleased(held);
        DisplayTest.flushEdt();
        assertEquals(1, lowerShortcut[0], "a keystroke of its own still works");

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    /// A focusable component that takes the default `Component#keyRepeated`, which runs
    /// its own press and release handlers. A `Button` deliberately does not repeat, so it
    /// cannot show this.
    private static final class RepeatCountingComponent extends Component {
        private int presses;

        RepeatCountingComponent() {
            setFocusable(true);
        }

        @Override
        public void keyPressed(int keyCode) {
            presses++;
        }
    }

    @FormTest
    void aRepeatAfterTheOwnerIsGoneDoesNotReachWhatItUncovered() {
        // Component.keyRepeated runs its own press and release handlers directly, so a
        // repeat forwarded to the newly focused component acts on it without ever
        // passing the scoped-release guard. A dialog going away under a held key -- a
        // timeout does exactly that -- would have the repeats still arriving act on the
        // surface it had been covering.
        final Window w = openHost(600, 500);
        int fire = Display.getInstance().getKeyCode(Display.GAME_FIRE);

        Dialog low = new Dialog("lower");
        low.setLayout(new BorderLayout());
        RepeatCountingComponent below = new RepeatCountingComponent();
        low.add(BorderLayout.CENTER, below);
        low.setTopLevelHost(w);
        low.showModeless();
        DisplayTest.flushEdt();

        final Dialog high = new Dialog("upper");
        high.setLayout(new BorderLayout());
        high.add(BorderLayout.CENTER, new Label("upper"));
        high.setTopLevelHost(w);
        high.showModeless();
        DisplayTest.flushEdt();

        // Held down while the upper dialog owns the keyboard.
        w.keyPressed(fire);
        DisplayTest.flushEdt();

        // It goes away with the key still down, and the component below takes the focus.
        high.dispose();
        DisplayTest.flushEdt();
        w.setFocused(below);
        DisplayTest.flushEdt();
        int beforeRepeats = below.presses;

        w.keyRepeated(fire);
        w.keyRepeated(fire);
        DisplayTest.flushEdt();
        assertEquals(beforeRepeats, below.presses,
                "repeats of a key the uncovered surface never received must not reach it");

        w.keyReleased(fire);
        DisplayTest.flushEdt();

        // A whole keystroke of its own still reaches it.
        w.keyPressed(fire);
        DisplayTest.flushEdt();
        assertEquals(beforeRepeats + 1, below.presses, "a press of its own still works");
        w.keyReleased(fire);
        DisplayTest.flushEdt();

        low.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogsPointerListenerStillSeesTheDialogAsTheSource() {
        // A Form builds these events with itself as the source, and a listener handed to
        // the host still belongs to the dialog. Forwarding the host's own event gave the
        // listener a Window where it had always been given the dialog -- so anything
        // comparing the source, or casting it to Form, saw something else, and only on
        // the hosted path.
        final Window w = openHost(600, 500);
        final Object[] pressSource = new Object[1];
        final Object[] releaseSource = new Object[1];

        final Dialog d = new Dialog("hosted");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pressSource[0] = evt.getSource();
            }
        });
        d.addPointerReleasedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                releaseSource[0] = evt.getSource();
            }
        });
        d.setTopLevelHost(w);
        d.show(80, 80, 80, 80, true, false);
        DisplayTest.flushEdt();

        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();

        assertSame(d, pressSource[0], "the dialog is the source of its own press event");
        assertSame(d, releaseSource[0], "and of its own release event");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aHostedDialogsListenerCanStillConsumeTheGesture() {
        // The listener is handed a copy of the host's event, so consuming it has to
        // travel back or the listener can no longer stop the gesture. A consumed press
        // makes the window return before hit testing, which is what keeps the scrim from
        // taking it -- and with no press recorded, the lift cannot dismiss the dialog.
        final Window w = openHost(600, 500);

        final Dialog quiet = new Dialog("not consumed");
        quiet.setLayout(new BorderLayout());
        quiet.add(BorderLayout.CENTER, new Label("body"));
        quiet.setDisposeWhenPointerOutOfBounds(true);
        quiet.setTopLevelHost(w);
        quiet.show(80, 80, 80, 80, true, false);
        DisplayTest.flushEdt();

        // Sanity: without a listener in the way, the corner press dismisses it.
        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertTrue(quiet.isDisposed(), "sanity: an outside press dismisses it normally");

        final Dialog d = new Dialog("consumed");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setDisposeWhenPointerOutOfBounds(true);
        d.addPointerPressedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
            }
        });
        d.setTopLevelHost(w);
        d.show(80, 80, 80, 80, true, false);
        DisplayTest.flushEdt();

        w.pointerPressed(2, 2);
        w.pointerReleased(2, 2);
        DisplayTest.flushEdt();
        assertFalse(d.isDisposed(),
                "a consumed press has to stop the gesture, so nothing dismisses it");

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aCoveredSurfacesOwnKeyListenerDoesNotFireUnderADialog() {
        // A component in the window's content registers its shortcut on the window -- an
        // HTML document registers one per accesskey attribute. The window dispatched its
        // whole raw map regardless of who owned the keyboard, so those still fired from
        // under a modal dialog and activated a link or checkbox nobody could see. On a
        // Form the covered surface stops hearing keys because the dialog becomes the
        // current form; a window has to say so.
        final Window w = openHost(600, 500);
        final int[] underneath = new int[1];
        int shortcut = 'q';
        w.addKeyListener(shortcut, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                underneath[0]++;
            }
        });
        DisplayTest.flushEdt();

        // Sanity: it works while the window itself owns the keyboard.
        w.keyPressed(shortcut);
        w.keyReleased(shortcut);
        DisplayTest.flushEdt();
        assertEquals(1, underneath[0], "sanity: the shortcut works with nothing over it");

        Dialog d = new Dialog("over");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();

        w.keyPressed(shortcut);
        w.keyReleased(shortcut);
        DisplayTest.flushEdt();
        assertEquals(1, underneath[0],
                "the covered surface must not hear a key the dialog owns");

        // And it hears them again once the dialog has gone.
        d.dispose();
        DisplayTest.flushEdt();
        w.keyPressed(shortcut);
        w.keyReleased(shortcut);
        DisplayTest.flushEdt();
        assertEquals(2, underneath[0], "and hears them again once it is uncovered");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aFloatingActionButtonInsideAHostedDialogClosesIt() {
        // The button looks for the dialog to close by asking for its top level. A hosted
        // dialog is parented in its window's layered pane, so that answer is the window
        // and the dialog around the button was never found: it fired its action and left
        // the dialog -- and anyone blocked on it -- open.
        final Window w = openHost(600, 500);
        final int[] fired = new int[1];

        Dialog d = new Dialog("hosted");
        d.setLayout(new BorderLayout());
        com.codename1.components.FloatingActionButton fab =
                com.codename1.components.FloatingActionButton.createFAB(
                        com.codename1.ui.FontImage.MATERIAL_ADD);
        fab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        });
        d.add(BorderLayout.CENTER, fab);
        d.setTopLevelHost(w);
        d.showModeless();
        DisplayTest.flushEdt();
        assertTrue(isUnder(w, d), "precondition: the dialog is up on the window");

        fab.released(fab.getAbsoluteX() + 1, fab.getAbsoluteY() + 1);
        DisplayTest.flushEdt();

        assertEquals(1, fired[0], "the button still fires its own action");
        assertTrue(d.isDisposed(), "and the dialog it sits inside has to close with it");

        w.dispose();
        DisplayTest.flushEdt();
    }

    /// Whether a component sits anywhere inside the given container.
    private static boolean isInside(Container ancestor, Component c) {
        Component probe = c;
        while (probe != null) {
            if (probe == ancestor) { //NOPMD CompareObjectsWithEquals
                return true;
            }
            probe = probe.getParent();
        }
        return false;
    }

    @FormTest
    void anArrowKeyCannotMoveFocusOutOfAHostedDialog() {
        // Traversal searched the layers and then the content behind them, so an arrow
        // press with nothing left in that direction moved focus to a component the
        // dialog covers. The next key is refused for being outside the scope and focus
        // is reset to the *first* control in the dialog -- so the press silently threw
        // away where the user was.
        final Window w = openHost(600, 500);
        // Below the dialog's card, so "down" has somewhere to escape to if traversal is
        // allowed to leave the scope.
        Button behind = new Button("behind");
        w.add(BorderLayout.SOUTH, behind);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("over");
        d.setLayout(new com.codename1.ui.layouts.BoxLayout(
                com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        Button firstInDialog = new Button("first");
        Button lastInDialog = new Button("last");
        d.add(firstInDialog);
        d.add(lastInDialog);
        d.setTopLevelHost(w);
        d.show(20, 220, 20, 20, true, false);
        DisplayTest.flushEdt();

        // The lower of the dialog's two controls: there is nothing below it inside the
        // dialog, and something below it outside.
        w.setFocused(lastInDialog);
        DisplayTest.flushEdt();
        assertSame(lastInDialog, w.getFocused(),
                "precondition: the lower control actually holds the focus");

        int down = Display.getInstance().getKeyCode(Display.GAME_DOWN);
        w.keyPressed(down);

        // Checked here, between the press and the release: the release repairs an escape
        // by re-focusing inside the scope, so by then the damage is invisible. It is real
        // while it lasts -- the window scrolls the component it moved to into view, and
        // that component is one the dialog covers.
        Component afterPress = w.getFocused();
        assertFalse(afterPress == behind, //NOPMD CompareObjectsWithEquals
                "an arrow press must not move focus to the component the dialog covers");
        assertTrue(afterPress == null || isInside(d, afterPress), //NOPMD CompareObjectsWithEquals
                "focus stays inside the dialog holding the keyboard");

        w.keyReleased(down);
        DisplayTest.flushEdt();

        d.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    /// A tooltip manager that records whether a tooltip was ever actually built.
    private static final class CountingTooltipManager extends TooltipManager {
        private int shown;

        @Override
        protected void showTooltip(String tip, Component cmp) {
            shown++;
        }
    }

    @FormTest
    void aPressThatOpensAnOverlayStillCancelsAPendingTooltip() {
        // The press dismisses any pending tooltip so a hover timer cannot go on to build
        // one for a component the overlay now covers. Returning early once the gesture
        // was cancelled skipped that, leaving a tooltip to appear over -- or behind --
        // the dialog the press had just opened.
        final Window w = openHost(600, 500);
        Button hovered = new Button("hover me");
        hovered.setTooltip("a tip");
        w.add(BorderLayout.CENTER, hovered);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();

        TooltipManager previous = TooltipManager.getInstance();
        CountingTooltipManager counter = new CountingTooltipManager();
        counter.setTooltipShowDelay(10);
        TooltipManager.enableTooltips(counter);
        final boolean[] once = new boolean[1];
        try {
            int hx = hovered.getAbsoluteX() + hovered.getWidth() / 2;
            int hy = hovered.getAbsoluteY() + hovered.getHeight() / 2;
            w.pointerHover(new int[] { hx }, new int[] { hy });
            DisplayTest.flushEdt();

            w.addPointerPressedListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (once[0]) {
                        return;
                    }
                    once[0] = true;
                    Dialog over = new Dialog("over");
                    over.setLayout(new BorderLayout());
                    over.add(BorderLayout.CENTER, new Label("body"));
                    over.setTopLevelHost(w);
                    over.showModeless();
                }
            });
            w.pointerPressed(hx, hy);
            DisplayTest.flushEdt();
            assertTrue(once[0], "precondition: the press put an overlay up");

            // Long enough for a surviving hover timer to have elapsed.
            for (int iter = 0; iter < 80; iter++) {
                w.repaintAnimations();
                DisplayTest.flushEdt();
                try {
                    Thread.sleep(2);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
            assertEquals(0, counter.shown,
                    "the press has to cancel the pending tooltip even when it opened an overlay");
        } finally {
            TooltipManager.enableTooltips(previous);
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// Opens one inset, dismiss-on-outside-press dialog on the host, once.
    private static Dialog openOverlayOnce(Window w, boolean[] once) {
        if (once[0]) {
            return null;
        }
        once[0] = true;
        Dialog over = new Dialog("over");
        over.setLayout(new BorderLayout());
        over.add(BorderLayout.CENTER, new Label("body"));
        over.setDisposeWhenPointerOutOfBounds(true);
        over.setTopLevelHost(w);
        over.show(80, 80, 80, 80, true, false);
        return over;
    }

    @FormTest
    void aStylusPressThatOpensAnOverlayDoesNotHandItTheRelease() {
        // The stylus dispatch runs before the gesture is started, so an overlay opened
        // from a stylus listener raised the cancelled flag and the gesture start cleared
        // it again a moment later -- the press then carried on into UI that had not
        // existed when the pen went down.
        final Window w = openHost(600, 500);
        final Dialog[] shown = new Dialog[1];
        final boolean[] once = new boolean[1];
        Label pad = new Label("pad");
        w.add(BorderLayout.CENTER, pad);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        pad.addStylusListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                shown[0] = openOverlayOnce(w, once);
            }
        });

        implementation.setPointerType(com.codename1.ui.events.PointerEvent.TYPE_STYLUS);
        try {
            w.pointerPressed(2, 2);
            DisplayTest.flushEdt();
            assertNotNull(shown[0], "the stylus listener showed a dialog");

            w.pointerReleased(2, 2);
            DisplayTest.flushEdt();
            assertFalse(shown[0].isDisposed(),
                    "the lift must not dismiss a dialog the pen press never touched");
        } finally {
            implementation.setPointerType(com.codename1.ui.events.PointerEvent.TYPE_UNKNOWN);
            if (shown[0] != null && !shown[0].isDisposed()) {
                shown[0].dispose();
            }
            DisplayTest.flushEdt();
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    @FormTest
    void aReleaseListenerThatOpensAnOverlayDoesNotAlsoReleaseWhatWasPressed() {
        // The release forwarded to the component captured before the listeners ran, so a
        // listener that put an overlay up still let that component act on the lift the
        // overlay had just claimed.
        final Window w = openHost(600, 500);
        final int[] released = new int[1];
        Button under = new Button("under");
        under.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                released[0]++;
            }
        });
        w.add(BorderLayout.CENTER, under);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        int bx = under.getAbsoluteX() + under.getWidth() / 2;
        int by = under.getAbsoluteY() + under.getHeight() / 2;

        final boolean[] once = new boolean[1];
        final Dialog[] shown = new Dialog[1];
        w.addPointerReleasedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                shown[0] = openOverlayOnce(w, once);
            }
        });

        w.pointerPressed(bx, by);
        DisplayTest.flushEdt();
        w.pointerReleased(bx, by);
        DisplayTest.flushEdt();

        assertNotNull(shown[0], "the release listener showed a dialog");
        assertEquals(0, released[0],
                "the component under it must not also act on a lift the overlay claimed");

        shown[0].dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }

    /// A button that records long presses delivered to it.
    private static final class HoldCountingButton extends Button {
        private int holds;

        HoldCountingButton(String text) {
            super(text);
        }

        @Override
        public void longPointerPress(int x, int y) {
            holds++;
        }
    }

    @FormTest
    void aLongPressListenerThatOpensAnOverlayStopsThere() {
        // The hold continued into hit testing after the overlay went up. The pressed
        // component had been cleared, so the fallback target was whatever now held the
        // focus -- a control inside the overlay that had just appeared under the finger,
        // which then received a hold the user never aimed at it.
        final Window w = openHost(600, 500);
        Button under = new Button("under");
        w.add(BorderLayout.CENTER, under);
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        int bx = under.getAbsoluteX() + under.getWidth() / 2;
        int by = under.getAbsoluteY() + under.getHeight() / 2;

        final boolean[] once = new boolean[1];
        final Dialog[] shown = new Dialog[1];
        final HoldCountingButton[] inOverlay = new HoldCountingButton[1];
        w.addLongPressListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (once[0]) {
                    return;
                }
                once[0] = true;
                Dialog over = new Dialog("over");
                over.setLayout(new BorderLayout());
                HoldCountingButton inside = new HoldCountingButton("inside");
                over.add(BorderLayout.CENTER, inside);
                over.setTopLevelHost(w);
                inOverlay[0] = inside;
                shown[0] = over;
                over.show(80, 80, 80, 80, true, false);
            }
        });

        w.pointerPressed(bx, by);
        DisplayTest.flushEdt();
        w.longPointerPress(bx, by);
        DisplayTest.flushEdt();

        assertNotNull(shown[0], "the long press listener showed a dialog");
        assertNotNull(inOverlay[0]);
        assertEquals(0, inOverlay[0].holds,
                "the hold must not carry on into the overlay it just opened");

        shown[0].dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }
    @FormTest
    void focusDoesNotLandInsideAHiddenBranchOfTheDialog() {
        // Visibility is asked of each component on its own, so a child of a hidden
        // container still answers that it is visible and focusable. Descending into that
        // branch put the keyboard on a control the user cannot see, and the scope check
        // then called it valid because it does belong to the dialog.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog d = new Dialog("pick");
            d.setLayout(new com.codename1.ui.layouts.BoxLayout(
                    com.codename1.ui.layouts.BoxLayout.Y_AXIS));
            Container hidden = new Container(new BorderLayout());
            Button buried = new Button("in the hidden branch");
            hidden.add(BorderLayout.CENTER, buried);
            hidden.setVisible(false);
            Button visible = new Button("the one you can see");
            d.add(hidden);
            d.add(visible);
            d.setTopLevelHost(w);

            d.showModeless();
            DisplayTest.flushEdt();
            try {
                assertNotSame(buried, w.getFocused(),
                        "focus must not land in a branch that is not on screen");
                assertSame(visible, w.getFocused(),
                        "it belongs on the first control the user can actually see");
            } finally {
                d.dispose();
                DisplayTest.flushEdt();
            }
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void aDialogTimingOutElsewhereLeavesTheOtherWindowsKeyboardAlone() throws Exception {
        // The teardown clears the host before the keyboard check runs, so resolving it
        // again answered with whatever had the focus by then -- for a dialog that timed
        // out while the user had moved to another window, that is the window they moved
        // to, which made the test pass and stopped the editing it exists to leave alone.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final Window shownOn = new Window("dialog host", new BorderLayout());
        shownOn.setWindowSize(400, 300);
        shownOn.show();
        DisplayTest.flushEdt();

        final Window typingIn = new Window("typing here", new BorderLayout());
        typingIn.setWindowSize(400, 300);
        TextField field = new TextField();
        typingIn.add(BorderLayout.CENTER, field);
        typingIn.show();
        DisplayTest.flushEdt();

        // The keyboard is only actually asked to hide on a touch device with one
        // registered, so without both this assertion could never see anything.
        implementation.setTouchDevice(true);
        final int[] hidden = new int[1];
        Display.getInstance().setDefaultVirtualKeyboard(new com.codename1.impl.VirtualKeyboardInterface() {
            @Override
            public String getVirtualKeyboardName() {
                return "recording";
            }

            @Override
            public void setInputType(int inputType) {
            }

            @Override
            public void showKeyboard(boolean show) {
                if (!show) {
                    hidden[0]++;
                }
            }

            @Override
            public boolean isVirtualKeyboardShowing() {
                return false;
            }
        });

        final Dialog d = new Dialog("please wait");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("working"));
        // Hosted implicitly, from the focused window, which is what makes resolving the
        // host again after teardown answer with a different surface. A dialog told its
        // host outright keeps that answer and never had the problem.
        Desktop.getInstance().windowFocusChanged(shownOn.getWindowId(), true);
        DisplayTest.flushEdt();

        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                d.showDialog();
            }
        }, "cn1-test-hosted-modal");
        caller.start();
        try {
            for (int i = 0; i < 400 && d.getParent() == null; i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            assertNotNull(d.getParent(), "precondition: the dialog is up on its host");

            // The user moves to the other window and starts typing there.
            Desktop.getInstance().windowFocusChanged(typingIn.getWindowId(), true);
            implementation.setFocusedEditingText(field);
            DisplayTest.flushEdt();
            assertSame(field, Display.impl.getEditingText(),
                    "precondition: the other window is the one being typed into");
            hidden[0] = 0;

            d.dispose();
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);

            assertEquals(0, hidden[0],
                    "a dialog closing on one window must not hide another window's keyboard");
        } finally {
            Display.getInstance().setDefaultVirtualKeyboard(null);
            implementation.setTouchDevice(false);
            implementation.setFocusedEditingText(null);
            d.dispose();
            DisplayTest.flushEdt();
            caller.join(2000);
            typingIn.dispose();
            shownOn.dispose();
            DisplayTest.flushEdt();
        }
    }
    /// A dialog that registers its shortcut the way a subclass naturally would.
    private static final class ShortcutDialog extends Dialog {
        private final ActionListener shortcut;
        private boolean registered;

        ShortcutDialog(ActionListener shortcut) {
            this.shortcut = shortcut;
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            // Registered from here, which is where a subclass would do it -- and by then
            // the dialog is already attached to its host.
            if (!registered) {
                registered = true;
                addKeyListener('x', shortcut);
            }
        }
    }

    @FormTest
    void aShortcutRegisteredWhileInitialisingRunsOnce() {
        // Adding the dialog to the layer initializes it, so a listener registered from
        // initComponent() is published to the host there and then -- and the bulk
        // publication that follows added a second wrapper for the same listener, so one
        // key press ran the application's callback twice.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            final int[] fired = new int[1];
            ShortcutDialog d = new ShortcutDialog(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    fired[0]++;
                }
            });
            d.setLayout(new BorderLayout());
            d.add(BorderLayout.CENTER, new Label("body"));
            d.setTopLevelHost(w);
            d.showModeless();
            DisplayTest.flushEdt();
            try {
                w.keyPressed('x');
                w.keyReleased('x');
                DisplayTest.flushEdt();

                assertEquals(1, fired[0],
                        "one key press has to run the shortcut once, not once per wrapper");
            } finally {
                d.dispose();
                DisplayTest.flushEdt();
            }
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    @FormTest
    void aHostedDialogsKeyListenerSeesTheDialogAndItsOwnCode() {
        // A listener added to a Dialog is handed an event sourced from that dialog when
        // it is shown the historical way, and a game listener is handed the game action
        // rather than the key that produced it. Forwarding the host window's own event
        // changed both, so a listener that compares the source, or reads the code it
        // registered for, saw something else.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog d = new Dialog("shortcuts");
            d.setLayout(new BorderLayout());
            d.add(BorderLayout.CENTER, new Label("body"));
            d.setTopLevelHost(w);

            final Object[] keySource = new Object[1];
            final int[] keyCode = new int[1];
            d.addKeyListener('k', new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    keySource[0] = evt.getSource();
                    keyCode[0] = evt.getKeyEvent();
                }
            });

            final Object[] gameSource = new Object[1];
            final int[] gameCode = new int[1];
            int fire = Display.GAME_FIRE;
            d.addGameKeyListener(fire, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    gameSource[0] = evt.getSource();
                    gameCode[0] = evt.getKeyEvent();
                }
            });

            d.showModeless();
            DisplayTest.flushEdt();
            try {
                w.keyPressed('k');
                w.keyReleased('k');
                DisplayTest.flushEdt();
                assertSame(d, keySource[0],
                        "the listener belongs to the dialog, so the dialog is the source");
                assertEquals('k', keyCode[0], "and the key it registered for");

                // Asserted rather than guarded: a key that maps to the fire action
                // has to exist, and skipping this half quietly if it did not would
                // leave the game contract untested while still reporting green.
                int gameKey = keyForGameAction(fire);
                assertTrue(gameKey != 0, "precondition: some key maps to the fire action");
                w.keyPressed(gameKey);
                w.keyReleased(gameKey);
                DisplayTest.flushEdt();
                assertSame(d, gameSource[0], "same for a game listener");
                assertEquals(fire, gameCode[0],
                        "a game listener is handed the action, not the key behind it");
            } finally {
                d.dispose();
                DisplayTest.flushEdt();
            }
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// A physical key that maps to this game action, or 0 when none does here.
    private static int keyForGameAction(int action) {
        for (int code = -100; code < 200; code++) {
            if (code != 0 && Display.getInstance().getGameAction(code) == action) {
                return code;
            }
        }
        return 0;
    }
    /// Counts the animation passes it is given.
    private static final class CountingAnimation implements com.codename1.ui.animations.Animation {
        private int ticks;

        @Override
        public boolean animate() {
            ticks++;
            return false;
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
        }
    }

    @FormTest
    void anAnimationRegisteredOnAHostedDialogStillAdvances() {
        // A hosted dialog is a form nested in another surface, and the inherited pass is
        // what drains the animations registered on it. Overriding that away for the
        // timeout check meant anything registered through registerAnimated() on the
        // dialog sat in its own list and never advanced while it was hosted.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog d = new Dialog("animated");
            d.setLayout(new BorderLayout());
            d.add(BorderLayout.CENTER, new Label("body"));
            d.setTopLevelHost(w);
            d.showModeless();
            DisplayTest.flushEdt();
            try {
                CountingAnimation anim = new CountingAnimation();
                d.registerAnimated(anim);
                DisplayTest.flushEdt();

                for (int i = 0; i < 20; i++) {
                    w.repaintAnimations();
                    DisplayTest.flushEdt();
                }

                assertTrue(anim.ticks > 0,
                        "an animation registered on the hosted dialog has to advance");
            } finally {
                d.dispose();
                DisplayTest.flushEdt();
            }
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }
    /// The off-surface half of a dialog timeout, via reflection.
    private static Object timeoutClockOf(Dialog d) {
        try {
            java.lang.reflect.Field f = Dialog.class.getDeclaredField("timeoutClock");
            f.setAccessible(true);
            return f.get(d);
        } catch (Exception err) {
            throw new IllegalStateException(err);
        }
    }

    /// A timeout must not depend on the dialog's surface being painted.
    ///
    /// The deadline is polled from animate(), which rides the animation loop of whatever
    /// is being painted. A dialog hosted in a window, or living in a native window of
    /// its own, is not painted at all while that window is minimized -- and minimizing
    /// deliberately does not dispose it, so a modal caller waiting on the timeout to
    /// release it waited for as long as the window stayed down. Asserted structurally:
    /// arming a timeout has to leave a clock that the surface does not drive, and ending
    /// the showing has to take it back down again.
    @FormTest
    void aDialogTimeoutIsArmedOffTheSurface() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog dlg = new Dialog("timed");
            assertNull(timeoutClockOf(dlg), "precondition: nothing armed before setTimeout");

            dlg.setTimeout(600000);
            assertNotNull(timeoutClockOf(dlg),
                    "arming a timeout has to leave a clock the surface does not drive,"
                            + " or a minimized window stalls it indefinitely");

            dlg.dispose();
            assertNull(timeoutClockOf(dlg),
                    "and ending the showing has to take that clock back down");

            // Longer than an int holds in milliseconds -- about 24.8 days. setTimeout
            // takes a long and the deadline poll has always honoured these, so arming
            // the clock must not narrow the delay: the wrap goes negative and
            // Timer.schedule throws on the spot rather than at the deadline.
            Dialog longDlg = new Dialog("long");
            longDlg.setTimeout(((long) Integer.MAX_VALUE) + 60000L);
            assertNotNull(timeoutClockOf(longDlg),
                    "a timeout too large for an int still has to arm a clock");
            longDlg.dispose();
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// The dialog's timeout deadline, via reflection.
    private static long deadlineOf(Dialog d) {
        try {
            java.lang.reflect.Field f = Dialog.class.getDeclaredField("time");
            f.setAccessible(true);
            return f.getLong(d);
        } catch (Exception err) {
            throw new IllegalStateException(err);
        }
    }

    /// A deadline that runs out before show() must survive to be honoured by it.
    ///
    /// setTimeout() is routinely called while a dialog is still being configured. The
    /// off-surface clock fires on wall time whether or not anything is on screen, so
    /// acting on it there clears the deadline and disposes nothing -- and the show()
    /// that follows puts up a dialog with no timeout at all, open for good. Nothing
    /// polled a detached dialog before that clock existed, which is the behaviour being
    /// preserved: the deadline waits, and the first paint after show() closes it.
    @FormTest
    void aTimeoutElapsingBeforeShowKeepsItsDeadline() {
        Form f = new Form("host", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();

        Dialog dlg = new Dialog("configured but not shown");
        dlg.setTimeout(40);
        assertNotEquals(0L, deadlineOf(dlg), "precondition: a deadline was set");

        // Well past it, with the event thread free to deliver the clock's callback.
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        DisplayTest.flushEdt();
        DisplayTest.flushEdt();

        assertNotEquals(0L, deadlineOf(dlg),
                "the clock must not retire a deadline for a dialog that was never shown,"
                        + " or the show that follows has no timeout at all");
        assertNull(timeoutClockOf(dlg),
                "and it has to drop the spent clock, or nothing will arm the replacement"
                        + " that the kept deadline exists for");

        // The whole point of keeping the deadline: the show it was kept for honours it.
        // Already past, so the clock armed for it fires at once and closes the dialog --
        // which is only reachable because the spent clock was dropped above.
        dlg.show(0, 0, 0, 0, false, false);
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        DisplayTest.flushEdt();

        assertTrue(dlg.isDisposed(),
                "the deadline was kept so that this show would honour it");
    }

    /// Showing a dialog again re-arms the clock its previous showing took down.
    ///
    /// A deadline outlives the showing that set it: disposing before the timeout stops
    /// the clock but keeps the deadline, so the next show still honours it. setTimeout
    /// is not called a second time, so nothing else would arm one -- and without a clock
    /// the reused dialog is polled only while it is painted, which a minimized window
    /// does not do. That is the stall the clock exists to prevent, returning by reuse.
    @FormTest
    void showingATimedDialogAgainRearmsItsClock() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog dlg = new Dialog("reused");
            dlg.setTimeout(600000);
            assertNotNull(timeoutClockOf(dlg), "precondition: setTimeout armed a clock");

            dlg.dispose();
            assertNull(timeoutClockOf(dlg), "precondition: dispose took it down");
            assertNotEquals(0L, deadlineOf(dlg),
                    "precondition: the deadline survives, so the next show owes a timeout");

            // The public entry, not showModal directly: it is what clears the disposed
            // flag, and without that the showing tears itself straight back down.
            dlg.show(0, 0, 0, 0, false, false);
            DisplayTest.flushEdt();

            assertNotNull(timeoutClockOf(dlg),
                    "showing it again has to arm a clock for what is left of the"
                            + " deadline, or only painting can close it");
            dlg.dispose();
            DisplayTest.flushEdt();
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// A menu dialog is torn down through disposeImpl, and that ends a showing too.
    ///
    /// MenuBar.showMenu calls disposeImpl directly rather than dispose(), which is why
    /// the dialog overrides the lower one. A showing that ends without stopping its
    /// clock leaves a timer thread holding the dialog until a deadline nobody is
    /// waiting for -- the same gap the directional disposes had.
    @FormTest
    void disposeImplStopsTheTimeoutClock() {
        Form f = new Form("host", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();

        Dialog dlg = new Dialog("menu-like");
        dlg.setTimeout(600000);
        assertNotNull(timeoutClockOf(dlg), "precondition: setTimeout armed a clock");

        // The teardown MenuBar uses, not the public one.
        dlg.disposeImpl();
        DisplayTest.flushEdt();

        assertNull(timeoutClockOf(dlg),
                "the lower teardown ends a showing too, so it has to stop the clock");
    }

    /// A window that stops being on screen ends the showing; minimizing does not.
    ///
    /// Both arrive as the platform telling the window it is no longer visible, and the
    /// difference is the whole of the rule: hide() means the showing is over, while a
    /// minimize is recorded separately so the window keeps its dialog and gets it back
    /// on restore. Driven through hideNotify, which is what the ports actually call --
    /// Window.minimize() only asks the window manager, and a fake one answers nothing.
    @FormTest
    void aMinimizedNativeDialogSurvivesWhereAHiddenOneDoesNot() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("host", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();

        Dialog minimized = new Dialog("minimized");
        minimized.setNativeWindowMode(true);
        minimized.show(0, 0, 0, 0, false, false);
        DisplayTest.flushEdt();
        Window mw = minimized.getNativeWindow();
        assertNotNull(mw, "precondition: it really is in a window");

        mw.hideNotify();
        DisplayTest.flushEdt();
        assertNotNull(minimized.getNativeWindow(),
                "a minimized window keeps its dialog and gets it back on restore");
        minimized.dispose();
        DisplayTest.flushEdt();

        Dialog hidden = new Dialog("hidden");
        hidden.setNativeWindowMode(true);
        hidden.show(0, 0, 0, 0, false, false);
        DisplayTest.flushEdt();
        Window hw = hidden.getNativeWindow();
        assertNotNull(hw, "precondition: it really is in a window");

        hw.hide();
        DisplayTest.flushEdt();
        assertNull(hidden.getNativeWindow(),
                "a window that is not on screen is not showing the dialog any more");
        assertTrue(hw.isWindowDisposed(),
                "and the window that showing owned is released rather than left"
                        + " allocated with nothing able to reach it");
    }

    /// A window disposed from outside ends the showing, so it stops the clock.
    ///
    /// This is the path an owner cascade and getNativeWindow().dispose() take, and it
    /// reaches neither dispose() nor disposeImpl(): the dialog is detached from the
    /// window that is going away. Leaving the clock armed there holds a non-daemon
    /// timer thread, and the dialog it references, until a deadline nobody awaits.
    @FormTest
    void aNativeWindowDisposedFromOutsideStopsTheTimeoutClock() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("host", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();

        Dialog dlg = new Dialog("native");
        dlg.setNativeWindowMode(true);
        dlg.setTimeout(600000);
        dlg.show(0, 0, 0, 0, false, false);
        DisplayTest.flushEdt();
        Window w = dlg.getNativeWindow();
        assertNotNull(w, "precondition: it really is in a window");
        assertNotNull(timeoutClockOf(dlg), "precondition: the timeout armed a clock");

        // Not dlg.dispose() -- the window going away underneath it.
        w.dispose();
        DisplayTest.flushEdt();

        assertNull(timeoutClockOf(dlg),
                "the window dying ends the showing, so it has to stop the clock too");
    }

    /// Flipping the mode while up and showing again ends the old representation first.
    ///
    /// setNativeWindowMode() takes effect on the next showing, so a caller may flip it
    /// while the dialog is up. Dispatching straight into the new representation carried
    /// a dialog still parented in the old one, which throws on attach -- after a window
    /// or a scrim had already been built, so the failure left half an overlay behind.
    @FormTest
    void switchingModeBetweenShowingsEndsThePreviousOne() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            // Hosted first, then asked for a window of its own.
            Dialog toNative = new Dialog("hosted first");
            toNative.setNativeWindowMode(false);
            toNative.showModeless();
            DisplayTest.flushEdt();
            assertNull(toNative.getNativeWindow(), "precondition: hosted, not native");

            toNative.setNativeWindowMode(true);
            toNative.showModeless();
            DisplayTest.flushEdt();
            assertNotNull(toNative.getNativeWindow(),
                    "the second showing has to be the representation just asked for");
            toNative.dispose();
            DisplayTest.flushEdt();

            // And the other way.
            Dialog toHosted = new Dialog("native first");
            toHosted.setNativeWindowMode(true);
            toHosted.showModeless();
            DisplayTest.flushEdt();
            assertNotNull(toHosted.getNativeWindow(), "precondition: native");
            Window owned = toHosted.getNativeWindow();

            toHosted.setNativeWindowMode(false);
            toHosted.showModeless();
            DisplayTest.flushEdt();
            assertNull(toHosted.getNativeWindow(),
                    "the window showing has to end when the dialog moves into a layer");
            assertTrue(owned.isWindowDisposed(),
                    "and the window that showing owned goes with it");
            toHosted.dispose();
            DisplayTest.flushEdt();
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// A listener equal to the registered one removes it, wrapper included.
    ///
    /// The form removes a key listener by equality -- it hands it to a list, which
    /// matches on equals -- so a caller passing a distinct but equal listener has its
    /// registration taken off. Comparing by identity when taking the hosted wrapper off
    /// the window left that wrapper published: the listener the caller had just removed
    /// went on firing until the dialog was torn down.
    @FormTest
    void removingAnEqualListenerAlsoTakesItsHostedWrapperOff() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(400, 300);
        w.show();
        DisplayTest.flushEdt();
        try {
            Dialog dlg = new Dialog("hosted");
            dlg.setTopLevelHost(w);
            dlg.showModeless();
            DisplayTest.flushEdt();
            assertSame(w, dlg.getTopLevelContainer(),
                    "precondition: hosted in the window, which is what publishes");

            dlg.addKeyListener(-92, new NamedListener("shortcut"));
            assertEquals(1, hostedKeyListenerCountOf(dlg),
                    "precondition: the wrapper was published on the host");

            // Equal, not the same object -- which is all the form needs.
            dlg.removeKeyListener(-92, new NamedListener("shortcut"));

            assertEquals(0, hostedKeyListenerCountOf(dlg),
                    "removing by equality has to take the wrapper off too, or the"
                            + " listener keeps firing after it was removed");
            dlg.dispose();
            DisplayTest.flushEdt();
        } finally {
            w.dispose();
            DisplayTest.flushEdt();
        }
    }

    /// A listener whose equality is by name rather than by identity.
    private static final class NamedListener implements ActionListener {
        private final String name;

        NamedListener(String name) {
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof NamedListener && name.equals(((NamedListener) other).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /// How many wrappers this dialog has published on its host, via reflection.
    private static int hostedKeyListenerCountOf(Dialog d) {
        try {
            java.lang.reflect.Field f = Dialog.class.getDeclaredField("hostedKeyListeners");
            f.setAccessible(true);
            java.util.List<?> published = (java.util.List<?>) f.get(d);
            return published == null ? 0 : published.size();
        } catch (Exception err) {
            throw new IllegalStateException(err);
        }
    }

}
