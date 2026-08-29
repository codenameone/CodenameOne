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

import com.codename1.components.InfiniteProgress;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestWindowManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The opt-in that backs a `Dialog` with a real operating system window.
class NativeWindowDialogTest extends UITestBase {

    private Dialog newDialog(String title) {
        Dialog d = new Dialog(title);
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        return d;
    }

    @org.junit.jupiter.api.AfterEach
    void clearDefault() {
        Dialog.setDefaultNativeWindowMode(false);
    }

    @FormTest
    void nativeModeIsOffWithoutAWindowSystem() {
        // Written first, and the one that matters most: with no windowing system the
        // request is silently ignored and nothing about the historical path moves.
        assertFalse(Desktop.isSupported());
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("no window system");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        assertSame(d, Display.getInstance().getCurrent(),
                "the dialog still takes over the main surface");
        assertNull(d.getNativeWindow(), "and no window was created for it");
        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void theInstanceSettingBeatsTheStaticDefault() {
        Dialog.setDefaultNativeWindowMode(true);
        Dialog on = newDialog("inherits");
        assertTrue(on.isNativeWindowMode(), "unset follows the static default");

        Dialog off = newDialog("overrides");
        off.setNativeWindowMode(false);
        assertFalse(off.isNativeWindowMode(), "an explicit setting wins");

        Dialog.setDefaultNativeWindowMode(false);
        Dialog forced = newDialog("forces");
        forced.setNativeWindowMode(true);
        assertTrue(forced.isNativeWindowMode(), "in both directions");
    }

    @FormTest
    void theThemeConstantSeedsTheDefaultAndAnExplicitCallStillWins() {
        UIManager.getInstance().getThemeConstant("dummy", null);
        java.util.Hashtable<String, String> props = new java.util.Hashtable<String, String>();
        props.put("@defaultNativeWindowModeBool", "true");
        UIManager.getInstance().setThemeProps(props);

        Dialog.setDefaultNativeWindowMode(true);
        assertTrue(Dialog.isDefaultNativeWindowMode());
        Dialog.setDefaultNativeWindowMode(false);
        assertFalse(Dialog.isDefaultNativeWindowMode(),
                "an explicit call always beats the theme constant behind it");
    }

    @FormTest
    void aModalDialogOpensOneWindowOwnedByItsHost() throws Exception {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        final Dialog d = newDialog("confirm");
        Command ok = new Command("OK");
        d.addCommand(ok);
        d.setNativeWindowMode(true);

        final Command[] result = new Command[1];
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = d.showDialog();
            }
        }, "cn1-test-native-dialog");
        caller.start();
        try {
            // Wait for the window to actually be on screen, not merely created:
            // showModal() from a background thread queues the show onto the dispatch
            // thread, so the window exists for a moment before it is showing.
            for (int i = 0; i < 400; i++) {
                Window probe = d.getNativeWindow();
                if (probe != null && probe.isWindowShowing()) {
                    break;
                }
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            Window w = d.getNativeWindow();
            assertNotNull(w, "the dialog has to open a window of its own");
            TestWindowManager.FakeWindow peer = wm.getLastWindow();
            assertNotNull(peer);
            assertEquals(1, wm.getWindows().size(), "exactly one window, not one per hop");
            assertEquals(Window.MODALITY_WINDOW, w.getModalityType(),
                    "a modal dialog blocks its host, which is what it always meant");
            assertSame(main, w.getOwnerWindow(),
                    "and is owned by the surface it was opened from");
            assertTrue(w.isDecorated(),
                    "decorated, or the user cannot move or close it");

            d.dispatchCommand(ok, new com.codename1.ui.events.ActionEvent(ok));
            for (int i = 0; i < 400 && caller.isAlive(); i++) {
                DisplayTest.flushEdt();
                Thread.sleep(5);
            }
            caller.join(2000);
            assertFalse(caller.isAlive(), "a command has to end the wait");
            assertSame(ok, result[0], "and showDialog reports which command it was");
            assertNull(d.getNativeWindow(), "the window is gone afterwards");
            assertTrue(peer.isDisposed());
        } finally {
            d.dispose();
            DisplayTest.flushEdt();
            caller.join(2000);
        }
    }

    @FormTest
    void theDialogLeavesTheWindowSoItCanBeShownAgain() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("reusable");
        d.setNativeWindowMode(true);
        assertNull(d.getParent(), "precondition: a dialog starts unparented");

        d.showModeless();
        DisplayTest.flushEdt();
        assertNotNull(d.getNativeWindow());
        assertNotNull(d.getParent(), "the dialog goes into the window");

        d.dispose();
        DisplayTest.flushEdt();
        assertNull(d.getParent(),
                "and comes back out, or it can never be shown again");
        // The title component, not the title area: a Dialog's title area is already
        // invisible before a native window touches it, so asserting on that would pass
        // whatever the restore did.
        assertTrue(d.getTitleComponent().isVisible(),
                "with its own title restored");

        d.showModeless();
        DisplayTest.flushEdt();
        assertNotNull(d.getNativeWindow(), "showing the same dialog again works");
        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anAnchoredPopupNeverOpensAWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        Button anchor = new Button("anchor");
        main.add(BorderLayout.CENTER, anchor);
        main.show();
        DisplayTest.flushEdt();

        final Dialog d = newDialog("popup");
        d.setNativeWindowMode(true);
        final boolean[] hadWindow = new boolean[1];
        d.addShowListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                hadWindow[0] = d.getNativeWindow() != null;
                d.dispose();
            }
        });
        d.showPopupDialog(anchor);
        DisplayTest.flushEdt();

        assertFalse(hadWindow[0],
                "a popup points at a rectangle in its host's space, so it stays there");
    }

    @FormTest
    void aMenuDialogNeverOpensAWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("menu");
        d.setNativeWindowMode(true);
        d.setMenu(true);
        d.showModeless();
        DisplayTest.flushEdt();
        assertNull(d.getNativeWindow(), "framework menu furniture gets no title bar");

        d.setMenu(false);
        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void buttonCommandsAreNotPublishedAsANativeMenu() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("buttons");
        d.setNativeWindowMode(true);
        // configureCommands with commandsAsButtons puts them in the dialog's own button
        // bar rather than registering them, so nothing should reach the window.
        d.setCommandsAsButtons(true);
        d.configureCommands(new Command[]{new Command("OK"), new Command("Cancel")}, true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        assertEquals(0, w.getCommandCount(),
                "OK and Cancel are buttons in the dialog, not entries in a menu bar");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void theWindowIsSizedByItsDrawableAreaNotItsFrame() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        // A title bar and borders outside the drawable, as every decorated window has.
        wm.setChromeInsets(16, 40);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("sized");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertNotNull(peer);
        assertEquals(40, peer.getHeight() - wm.getHeight(peer),
                "the frame has to be asked for more than the drawable, or the box is "
                        + "clipped by exactly the height of the title bar");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void closingTheWindowWithNoBackCommandDisposesTheDialog() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("closable");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        Window w = d.getNativeWindow();
        assertNotNull(w);

        Desktop.getInstance().windowCloseRequested(w.getWindowId());
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(), "the platform's close control has to close the dialog");
        assertNull(d.getNativeWindow());
    }

    @FormTest
    void anInteractionDialogAlsoOpensAWindowAndComesBackOut() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();

        assertNotNull(id.getNativeWindow(),
                "InteractionDialog takes the same opt-in as Dialog");
        assertNotNull(id.getParent(), "and goes into that window");

        id.dispose();
        DisplayTest.flushEdt();
        assertNull(id.getNativeWindow());
        assertNull(id.getParent(), "and comes back out of it");
    }

    @FormTest
    void anInteractionDialogWithoutAWindowSystemIsUnchanged() {
        assertFalse(Desktop.isSupported());
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();

        assertNull(id.getNativeWindow(), "no window system, no window");
        assertTrue(id.isShowing(), "and it still shows on the layered pane as before");
        id.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void closingTheWindowFiresTheBackCommand() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("cancellable");
        Command back = new Command("Cancel");
        d.setBackCommand(back);
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        Window w = d.getNativeWindow();
        assertNotNull(w);

        Desktop.getInstance().windowCloseRequested(w.getWindowId());
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(),
                "closing with the title bar has to mean the same as pressing Cancel");
    }

    @FormTest
    void setWindowContentSizeAsksForTheFrameThatYieldsThatDrawable() {
        // Standalone, because it is the piece most likely to look like a layout bug
        // when it is wrong: the dialog is clipped along the bottom by exactly the
        // height of the title bar.
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        wm.setChromeInsets(20, 50);
        Window w = new Window("sized", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        w.setWindowContentSize(400, 300);
        DisplayTest.flushEdt();

        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        assertEquals(420, peer.getWidth(),
                "the frame asked for is the drawable plus the chrome outside it");
        assertEquals(350, peer.getHeight());

        // And once the port reports the resize, as a real one does, the drawable is
        // exactly what was asked for rather than short by the title bar.
        Desktop.getInstance().windowSizeChanged(w.getWindowId(),
                peer.getWidth() - 20, peer.getHeight() - 50);
        DisplayTest.flushEdt();
        assertEquals(400, w.getWidth());
        assertEquals(300, w.getHeight());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void setWindowContentSizeIsAPlainSizeWhenThereIsNoChrome() {
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        Window w = new Window("sized", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        w.setWindowContentSize(400, 300);
        DisplayTest.flushEdt();
        assertEquals(400, w.getWidth());
        assertEquals(300, w.getHeight());
        assertEquals(400, wm.getLastWindow().getWidth(),
                "with no chrome it must not over-correct");
        assertEquals(300, wm.getLastWindow().getHeight());

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aModelessNativeDialogDoesNotBlockAndDoesNotPark() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("modeless");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        assertEquals(Window.MODALITY_NONE, w.getModalityType(),
                "a modeless dialog blocks nothing");
        assertFalse(Desktop.getInstance().isWindowInputBlocked(0),
                "and the main surface stays usable");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogOpenedFromAWindowIsOwnedByThatWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window host = new Window("host", new BorderLayout());
        host.setWindowSize(600, 500);
        host.show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("owned");
        d.setTopLevelHost(host);
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        assertSame(host, w.getOwnerWindow(),
                "a dialog opened from a window belongs to that window, so it stays "
                        + "above it and goes away with it");

        d.dispose();
        DisplayTest.flushEdt();
        host.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void disposingTheOwnerCascadesToTheDialogsWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();
        Window host = new Window("host", new BorderLayout());
        host.setWindowSize(600, 500);
        host.show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("cascades");
        d.setTopLevelHost(host);
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        assertNotNull(d.getNativeWindow());

        host.dispose();
        DisplayTest.flushEdt();

        assertNull(d.getNativeWindow(),
                "the dialog has to be torn down however its window died");
        assertNull(d.getParent(), "and come back out of it");
    }

    @FormTest
    void aMenuStyleCommandIsPublishedToTheWindow() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("menu commands");
        d.setNativeWindowMode(true);
        d.addCommand(new Command("Help"));
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        assertEquals(1, w.getCommandCount(),
                "a command added with addCommand goes where the platform shows a "
                        + "window's commands");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void theDialogTitleBecomesTheWindowTitleAndItsOwnIsHidden() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("Delete document");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        assertEquals("Delete document", w.getTitle());
        assertFalse(d.getTitleComponent().isVisible(),
                "the platform draws the title, so the dialog must not draw it twice");

        d.dispose();
        DisplayTest.flushEdt();
        assertTrue(d.getTitleComponent().isVisible(), "and it comes back afterwards");
    }

    @FormTest
    void flippingTheModeWhileShowingDoesNotMigrateTheDialog() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("stays put");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        Window w = d.getNativeWindow();
        assertNotNull(w);

        d.setNativeWindowMode(false);
        DisplayTest.flushEdt();
        assertSame(w, d.getNativeWindow(),
                "a showing dialog is never reparented between strategies; there is no "
                        + "safe point to do it while a caller may be parked on it");

        d.dispose();
        DisplayTest.flushEdt();

        d.showModeless();
        DisplayTest.flushEdt();
        assertNull(d.getNativeWindow(), "the new setting takes effect on the next show");
        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void initNativeWindowCanReconfigureTheWindowBeforeItAppears() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final boolean[] called = new boolean[1];
        Dialog d = new Dialog("configurable") {
            @Override
            protected void initNativeWindow(Window w) {
                called[0] = true;
                w.setResizable(true);
            }
        };
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        assertTrue(called[0], "the hook has to run");
        assertTrue(d.getNativeWindow().isResizable(),
                "and what it did has to stick");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anInteractionDialogPopupNeverOpensAWindowEither() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        Button anchor = new Button("anchor");
        main.add(BorderLayout.CENTER, anchor);
        main.show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.showPopupDialog(anchor);
        DisplayTest.flushEdt();

        assertNull(id.getNativeWindow(),
                "an anchored popup points into its host's coordinate space either way");
        id.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogIsSizedByItsDrawableEvenThoughItIsSizedAtShowTime() {
        // The dialog is sized as part of showing, so the correction has to happen after
        // the peer exists. Sizing before it treated the content size as a frame size
        // and left the box short by the whole title bar.
        TestWindowManager wm = implementation.setMultiWindowSupported(true);
        wm.setChromeInsets(20, 50);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("sized at show");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        TestWindowManager.FakeWindow peer = wm.getLastWindow();
        // The frame asked for, which is what setWindowContentSize controls. The
        // component's own width only changes when the port reports a resize, which the
        // fake manager does not do on setBounds, so asserting on it would be asserting
        // about the double rather than about the code.
        assertEquals(50, peer.getHeight() - wm.getHeight(peer),
                "the frame has to carry the chrome on top of the drawable");
        assertEquals(20, peer.getWidth() - wm.getWidth(peer));

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogRunsItsShowCallbacks() {
        // Sounds, onShowCompleted and every show listener. A modal dialog whose show
        // listener is what disposes it would otherwise never be released.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final boolean[] shown = new boolean[1];
        final boolean[] completed = new boolean[1];
        Dialog d = new Dialog("callbacks") {
            @Override
            protected void onShowCompleted() {
                completed[0] = true;
            }
        };
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, new Label("body"));
        d.addShowListener(new com.codename1.ui.events.ActionListener() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                shown[0] = true;
            }
        });
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        assertTrue(shown[0], "show listeners have to run in native window mode too");
        assertTrue(completed[0], "and so does onShowCompleted");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDialogWhoseWindowIsDisposedElsewhereCountsAsDisposed() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("externally closed");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);
        // Straight through the window, as an owner cascade or a desktop shutdown does.
        w.dispose();
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed(),
                "a dialog whose window died is over, however the window died");
        assertNull(d.getNativeWindow());
    }

    @FormTest
    void anInteractionDialogHidesItsOwnTitleBehindTheWindowChrome() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog("Details", new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();

        Window w = id.getNativeWindow();
        assertNotNull(w);
        assertEquals("Details", w.getTitle());
        assertFalse(id.getTitleComponent().isVisible(),
                "the platform draws the title, so the dialog must not draw a second one");

        id.dispose();
        DisplayTest.flushEdt();
        assertTrue(id.getTitleComponent().isVisible(), "and it comes back afterwards");
    }

    @FormTest
    void aDirectionalDisposeClosesTheWindowToo() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        Window w = id.getNativeWindow();
        assertNotNull(w);

        // There is no layer to slide out of; the window has to go instead, or a modal
        // caller waits on a window that stays on screen.
        id.disposeToTheLeft();
        DisplayTest.flushEdt();

        assertNull(id.getNativeWindow());
        assertTrue(w.isWindowDisposed(), "the window goes with it");
    }

    @FormTest
    void aNativeInteractionDialogFollowsTheFocusedWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        Window host = new Window("host", new BorderLayout());
        host.setWindowSize(600, 500);
        host.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(host.getWindowId(), true);
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();

        Window w = id.getNativeWindow();
        assertNotNull(w);
        assertSame(host, w.getOwnerWindow(),
                "a dialog opened from a focused window belongs to that window, not the "
                        + "main form behind it");

        id.dispose();
        DisplayTest.flushEdt();
        host.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogIsResizedForContentItsShowListenerAdds() {
        // A show listener is the ordinary place to fill a dialog in, and a decorated
        // non-resizable window will not grow itself afterwards. Sizing only before the
        // callbacks left whatever they added compressed into the empty box's size, with
        // no way for the user to drag it bigger.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog empty = newDialog("empty");
        empty.setNativeWindowMode(true);
        empty.showModeless();
        DisplayTest.flushEdt();
        TestWindowManager wm = (TestWindowManager) implementation.getWindowManager();
        int emptyHeight = wm.getLastWindow().getHeight();
        empty.dispose();
        DisplayTest.flushEdt();

        final Dialog d = newDialog("filled late");
        d.setNativeWindowMode(true);
        d.setLayout(com.codename1.ui.layouts.BoxLayout.y());
        d.addShowListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                for (int i = 0; i < 8; i++) {
                    d.add(new Label("added by the show listener " + i));
                }
            }
        });
        d.showModeless();
        DisplayTest.flushEdt();

        assertTrue(wm.getLastWindow().getHeight() > emptyHeight,
                "the window has to account for what the show listener added");

        d.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogDisposedByItsShowListenerIsNotResizedAfterwards() {
        // The re-size above must not run against a window that the callbacks tore down.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final Dialog d = newDialog("self disposing");
        d.setNativeWindowMode(true);
        d.addShowListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                d.dispose();
            }
        });
        d.showModeless();
        DisplayTest.flushEdt();

        assertTrue(d.isDisposed());
        assertNull(d.getNativeWindow());

        DisplayTest.flushEdt();
    }

    @FormTest
    void aTitleSetWhileShowingReachesTheNativeTitleBar() {
        // The dialog's own title label is hidden behind the OS title bar, so a
        // setTitle after showing -- onShow is the ordinary place for one -- used to
        // change nothing the user could see.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final Dialog d = newDialog("before");
        d.setNativeWindowMode(true);
        d.addShowListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                d.setTitle("decided at show time");
            }
        });
        d.showModeless();
        DisplayTest.flushEdt();

        assertEquals("decided at show time", d.getNativeWindow().getTitle());

        d.setTitle("changed later still");
        DisplayTest.flushEdt();
        assertEquals("changed later still", d.getNativeWindow().getTitle());

        d.dispose();
        DisplayTest.flushEdt();
        // And setting one on a disposed dialog must not raise anything.
        d.setTitle("after it has gone");

        DisplayTest.flushEdt();
    }

    @FormTest
    void aNativeDialogPutsTheTitleBackTheWayItFoundIt() {
        // An application is free to suppress the title before showing, and a dialog is
        // reusable: restoring "visible" unconditionally resurrected title UI that had
        // been deliberately hidden the next time it was shown the ordinary way.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("suppressed");
        d.getTitleComponent().setVisible(false);
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        d.dispose();
        DisplayTest.flushEdt();

        assertFalse(d.getTitleComponent().isVisible(),
                "the title was suppressed before the dialog was shown, and must stay so");

        // The ordinary case still comes back visible.
        Dialog shown = newDialog("ordinary");
        shown.setNativeWindowMode(true);
        shown.showModeless();
        DisplayTest.flushEdt();
        shown.dispose();
        DisplayTest.flushEdt();
        assertTrue(shown.getTitleComponent().isVisible());
    }

    @FormTest
    void anInteractionDialogTitleReachesTheNativeTitleBarToo() {
        // The same defect Dialog had: the window took a copy at construction and the
        // lightweight label is hidden behind the native title bar.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.setTitle("before");
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        assertEquals("before", id.getNativeWindow().getTitle());

        id.setTitle("changed while showing");
        DisplayTest.flushEdt();
        assertEquals("changed while showing", id.getNativeWindow().getTitle());

        id.dispose();
        DisplayTest.flushEdt();
        // And after teardown it must not raise anything.
        id.setTitle("after it has gone");
    }

    @FormTest
    void anInteractionDialogPutsTheTitleBackTheWayItFoundIt() {
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.setTitle("suppressed");
        id.add(BorderLayout.CENTER, new Label("body"));
        id.getTitleComponent().setVisible(false);
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        id.dispose();
        DisplayTest.flushEdt();

        assertFalse(id.getTitleComponent().isVisible(),
                "a title the application suppressed must stay suppressed");

        com.codename1.components.InteractionDialog shown =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        shown.setTitle("ordinary");
        shown.add(BorderLayout.CENTER, new Label("body"));
        shown.setNativeWindowMode(true);
        shown.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        shown.dispose();
        DisplayTest.flushEdt();
        assertTrue(shown.getTitleComponent().isVisible(),
                "and the ordinary case still comes back");
    }

    @FormTest
    void showingANativeInteractionDialogAgainClosesTheFirstWindow() {
        // Showing again without disposing first is something the lightweight path
        // tolerates. Overwriting the window reference left the first one on screen and
        // empty, with its own bridges now pointing at the second showing.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.components.InteractionDialog id =
                new com.codename1.components.InteractionDialog(new BorderLayout());
        id.add(BorderLayout.CENTER, new Label("body"));
        id.setNativeWindowMode(true);
        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        Window first = id.getNativeWindow();
        assertNotNull(first);

        id.show(0, 0, 0, 0);
        DisplayTest.flushEdt();
        Window second = id.getNativeWindow();
        assertNotNull(second);
        assertNotSame(first, second, "the second showing gets its own window");
        assertTrue(first.isWindowDisposed(),
                "and the first one must not be left open and empty");
        assertNotNull(id.getParent(), "the payload is in the second window");

        id.dispose();
        DisplayTest.flushEdt();
        assertTrue(second.isWindowDisposed());
    }

    @FormTest
    void showingANativeDialogAgainClosesTheFirstWindow() {
        // The same defect InteractionDialog had: a modeless dialog shown again before
        // it is disposed left the first window open and empty, with its own bridges
        // pointing at the second showing.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Dialog d = newDialog("shown twice");
        d.setNativeWindowMode(true);
        d.showModeless();
        DisplayTest.flushEdt();
        Window first = d.getNativeWindow();
        assertNotNull(first);

        d.showModeless();
        DisplayTest.flushEdt();
        Window second = d.getNativeWindow();
        assertNotNull(second);
        assertNotSame(first, second, "the second showing gets its own window");
        assertTrue(first.isWindowDisposed(),
                "and the first must not be left open and empty");
        assertNotNull(d.getParent(), "the dialog is in the second window");

        d.dispose();
        DisplayTest.flushEdt();
        assertTrue(second.isWindowDisposed());
    }

    @FormTest
    void aNativeDialogKeepsItsShortcutsAndDefaultCommand() {
        // A window never reads a nested form's key listeners, and its default-command
        // dispatch only runs for whatever holds the key scope -- neither of which the
        // native path set up, so a dialog in a window of its own had neither.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        final int[] shortcut = new int[1];
        final int[] defaultCmd = new int[1];

        Dialog d = newDialog("keyboard");
        d.setNativeWindowMode(true);
        d.addKeyListener('k', new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                shortcut[0]++;
            }
        });
        d.setDefaultCommand(new Command("OK") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                defaultCmd[0]++;
            }
        });
        d.showModeless();
        DisplayTest.flushEdt();

        Window w = d.getNativeWindow();
        assertNotNull(w);

        w.keyPressed('k');
        w.keyReleased('k');
        DisplayTest.flushEdt();
        assertEquals(1, shortcut[0], "a shortcut on a native dialog has to fire");

        int enter = Display.getInstance().getKeyCode(Display.GAME_FIRE);
        w.keyPressed(enter);
        w.keyReleased(enter);
        DisplayTest.flushEdt();
        assertEquals(1, defaultCmd[0], "and so does its default command");

        d.dispose();
        DisplayTest.flushEdt();
        assertTrue(w.isKeyInputScopeEmpty(),
                "and the window gets its keyboard back afterwards");
    }

    /// An application's own AbstractDialog, written against the interface as it was
    /// published. It must keep compiling: the core is built at Java 5, which has no
    /// default methods, so anything added to that interface breaks every existing
    /// implementation at build time and throws AbstractMethodError at runtime for one
    /// already compiled.
    private static final class ThirdPartyDialog implements AbstractDialog {
        public void addComponent(Object constraints, Component cmp) {
        }

        public void setScrollable(boolean scrollable) {
        }

        public void setDialogType(int dialogType) {
        }

        public void setTransitions(Transition transition) {
        }

        public void configureCommands(Command[] cmds, boolean commandsAsButtons) {
        }

        public void setDefaultCommand(Command defaultCommand) {
        }

        public void setTimeout(long timeout) {
        }

        public void dispose() {
        }

        public Command showDialog() {
            return null;
        }
    }

    @FormTest
    void anApplicationsOwnAbstractDialogStillCompiles() {
        // The assertion is that this file compiles at all -- the class above implements
        // nothing but the members the interface published, and a new abstract member
        // would stop it.
        AbstractDialog theirs = new ThirdPartyDialog();
        assertNotNull(theirs);
        assertNull(theirs.showDialog());
    }

    @FormTest
    void frameworkOverlaysStayLightweightWhenNativeModeIsOnGlobally() {
        // Both are shown by the framework rather than asked for by the application, and
        // both stop working as operating system windows: the spinner is modeless and
        // does its blocking with a scrim a window would not install, and a combo popup
        // is placed by margins against the surface it drops out of and dismissed by a
        // press outside itself, neither of which a window has.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        boolean previous = Dialog.isDefaultNativeWindowMode();
        Dialog.setDefaultNativeWindowMode(true);
        try {
            InfiniteProgress ip = new InfiniteProgress();
            Dialog spinner = ip.showInfiniteBlocking();
            DisplayTest.flushEdt();
            assertFalse(spinner.isNativeWindowMode(),
                    "the blocking spinner must not open a window of its own");
            assertNull(spinner.getNativeWindow());
            spinner.dispose();
            DisplayTest.flushEdt();

            ComboBox<String> combo = new ComboBox<String>("a", "b");
            Dialog popup = combo.createPopupDialog(
                    new com.codename1.ui.List<String>(combo.getModel()));
            assertFalse(popup.isNativeWindowMode(),
                    "and neither must a combo popup");
        } finally {
            Dialog.setDefaultNativeWindowMode(previous);
        }
    }
}
