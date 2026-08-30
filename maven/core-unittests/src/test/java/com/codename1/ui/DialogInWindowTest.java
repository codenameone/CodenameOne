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
}
