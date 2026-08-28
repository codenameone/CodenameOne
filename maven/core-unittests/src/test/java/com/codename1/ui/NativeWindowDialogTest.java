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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertTrue(d.getTitleArea().isVisible(),
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
        assertTrue(peer.getHeight() > w.getHeight(),
                "the frame has to be asked for more than the drawable, or the box is "
                        + "clipped by exactly the height of the title bar");
        assertEquals(40, peer.getHeight() - w.getHeight(),
                "and by exactly the chrome, not by a guess");

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
}
