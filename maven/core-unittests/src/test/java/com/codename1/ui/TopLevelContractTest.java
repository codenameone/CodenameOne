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
import com.codename1.ui.util.UITimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The vocabulary a `Form` and a `Window` have to answer the same way.
///
/// Everything that became window-aware leans on this, so it is worth pinning down
/// on its own rather than only through the components that consume it.
class TopLevelContractTest extends UITestBase {

    // ---- getCurrentTopLevel ----------------------------------------------------

    @FormTest
    void currentTopLevelIsTheCurrentFormWhenThereAreNoWindows() {
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        assertSame(main, CN.getCurrentTopLevel(),
                "with no windowing system this has to answer exactly what getCurrentForm does");
        assertSame(main, Display.getInstance().getCurrentTopLevel());
        assertSame(CN.getCurrentForm(), CN.getCurrentTopLevel());
    }

    @FormTest
    void currentTopLevelPrefersTheFocusedWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("focused", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        assertSame(w, CN.getCurrentTopLevel(),
                "an overlay with no component to resolve from belongs on the surface the "
                        + "user is looking at, not on the main form behind it");
        assertSame(main, CN.getCurrentForm(),
                "while getCurrentForm keeps its original meaning");

        w.dispose();
        DisplayTest.flushEdt();
        assertSame(main, CN.getCurrentTopLevel(),
                "and it falls back once the window is gone");
    }

    @FormTest
    void currentTopLevelIgnoresAWindowThatIsNotShowing() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("hidden", new BorderLayout());
        w.setCloseOperation(Window.HIDE_ON_CLOSE);
        w.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        w.hide();
        DisplayTest.flushEdt();

        assertSame(main, CN.getCurrentTopLevel(),
                "a hidden window is not where the user is");
        w.dispose();
        DisplayTest.flushEdt();
    }

    // ---- isTopLevelShowing -----------------------------------------------------

    @FormTest
    void bothTopLevelsAnswerWhetherTheyAreShowing() {
        Form shown = new Form("shown", new BorderLayout());
        Form other = new Form("other", new BorderLayout());
        shown.show();
        DisplayTest.flushEdt();
        assertTrue(shown.isTopLevelShowing());
        assertFalse(other.isTopLevelShowing(), "a form that was never shown is not showing");

        other.show();
        DisplayTest.flushEdt();
        assertTrue(other.isTopLevelShowing());
        assertFalse(shown.isTopLevelShowing(), "and only one form is current at a time");
    }

    // ---- tint ------------------------------------------------------------------

    @FormTest
    void bothTopLevelsCarryATintColour() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("form", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();
        Window w = new Window("window", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        // Reached through the interface, which is the whole point: ComboBox and
        // FloatingActionButton save and restore it without caring which they have.
        TopLevelContainer[] tops = new TopLevelContainer[]{f, w};
        for (TopLevelContainer top : tops) {
            int original = top.getTintColor();
            top.setTintColor(0x66FF0000);
            assertEquals(0x66FF0000, top.getTintColor());
            top.setTintColor(original);
            assertEquals(original, top.getTintColor());
        }

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aWindowsDefaultTintComesFromTheLookAndFeel() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("window", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        assertEquals(w.getUIManager().getLookAndFeel().getDefaultFormTintColor(),
                w.getTintColor(),
                "a window resolves its default the same way a form does, just later -- "
                        + "its UIManager can be set after construction");
        w.dispose();
        DisplayTest.flushEdt();
    }

    // ---- layered pane accessors ------------------------------------------------

    @FormTest
    void theIfExistsAccessorsDoNotCreateALayerOnEitherTopLevel() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("form", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();
        Window w = new Window("window", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        assertNull(f.getFormLayeredPaneIfExists());
        assertNull(f.getLayeredPaneIfExists());
        assertNull(w.getFormLayeredPaneIfExists());
        assertNull(w.getLayeredPaneIfExists());

        assertNotNull(f.getFormLayeredPane(TopLevelContractTest.class, true));
        assertNotNull(f.getFormLayeredPaneIfExists());
        assertNotNull(w.getFormLayeredPane(TopLevelContractTest.class, true));
        assertNotNull(w.getFormLayeredPaneIfExists());

        assertNotNull(f.getLayeredPane());
        assertNotNull(f.getLayeredPaneIfExists());
        assertNotNull(w.getLayeredPane());
        assertNotNull(w.getLayeredPaneIfExists());

        w.dispose();
        DisplayTest.flushEdt();
    }

    // ---- soft buttons ----------------------------------------------------------

    @FormTest
    void aWindowHasNoSoftButtonAreaAndAPlainFormHasNoneEither() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("form", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();
        Window w = new Window("window", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();

        assertEquals(0, w.softButtonAreaHeight(),
                "a window has no soft button bar and never will");
        assertEquals(f.getSoftButtonCount() > 1 ? f.softButtonAreaHeight() : 0,
                f.softButtonAreaHeight(),
                "and a form answers from its own soft button count");

        w.dispose();
        DisplayTest.flushEdt();
    }

    // ---- command host walk -----------------------------------------------------

    @FormTest
    void theCommandHostOfAnOrdinaryHierarchyIsItsTopLevel() {
        // The no-change guarantee for the walk that replaced getTopLevelContainer() in
        // Button and List: for every hierarchy that has no hosted dialog in it, it must
        // end at exactly the container the old lookup ended at.
        Form f = new Form("form", new BorderLayout());
        Container middle = new Container(new BorderLayout());
        Button b = new Button("press");
        middle.add(BorderLayout.CENTER, b);
        f.add(BorderLayout.CENTER, middle);
        f.show();
        DisplayTest.flushEdt();

        assertSame(f, TopLevelSupport.commandHostOf(b));
        assertSame(b.getTopLevelContainer().asContainer(), TopLevelSupport.commandHostOf(b));
    }

    @FormTest
    void theCommandHostInsideAWindowIsTheWindow() {
        implementation.setMultiWindowSupported(true);
        Window w = new Window("window", new BorderLayout());
        Button b = new Button("press");
        w.add(BorderLayout.CENTER, b);
        w.show();
        DisplayTest.flushEdt();

        assertSame(w, TopLevelSupport.commandHostOf(b));
        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aDetachedComponentHasNoCommandHost() {
        assertNull(TopLevelSupport.commandHostOf(new Button("orphan")));
        assertNull(TopLevelSupport.commandHostOf(null));
    }

    @FormTest
    void anEmbeddedFormIsWalkedPastJustAsTheTopLevelWalkWalksPastIt() {
        // Form.isCommandHost is deliberately the unparented test rather than "always
        // true": an embedded form keeps handing its commands outwards, as it always did.
        implementation.setMultiWindowSupported(true);
        Window w = new Window("window", new BorderLayout());
        Form nested = new Form("nested", new BorderLayout());
        Button b = new Button("press");
        nested.add(BorderLayout.CENTER, b);
        w.getContentPane().addComponent(BorderLayout.CENTER, nested);
        w.show();
        DisplayTest.flushEdt();

        assertSame(w, TopLevelSupport.commandHostOf(b),
                "the walk goes past the embedded form, exactly as getTopLevelContainer does");
        assertSame(w, b.getTopLevelContainer());

        w.dispose();
        DisplayTest.flushEdt();
    }

    // ---- host sizing helpers ---------------------------------------------------

    @FormTest
    void hostSizingMeasuresAWindowByItselfAndAFormByTheDisplay() {
        implementation.setMultiWindowSupported(true);
        Form f = new Form("form", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();
        Window w = new Window("window", new BorderLayout());
        w.setWindowSize(321, 234);
        w.show();
        DisplayTest.flushEdt();

        assertEquals(Display.getInstance().getDisplayWidth(), TopLevelSupport.hostWidth(f),
                "a form is measured by the display, which is the expression every caller "
                        + "evaluated before windows existed");
        assertEquals(Display.getInstance().getDisplayHeight(), TopLevelSupport.hostHeight(f));
        assertEquals(Display.getInstance().getDisplayWidth(), TopLevelSupport.hostWidth(null));

        assertEquals(w.getWidth(), TopLevelSupport.hostWidth(w));
        assertEquals(w.getHeight(), TopLevelSupport.hostHeight(w));

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void aWindowDecidesPortraitFromItsOwnShape() {
        implementation.setMultiWindowSupported(true);
        Window wide = new Window("wide", new BorderLayout());
        wide.setWindowSize(1000, 400);
        wide.show();
        DisplayTest.flushEdt();
        Window tall = new Window("tall", new BorderLayout());
        tall.setWindowSize(400, 1000);
        tall.show();
        DisplayTest.flushEdt();

        // The device bias is what a form uses; a window has no orientation, so its own
        // shape is all there is to go on and the bias is ignored either way.
        assertFalse(wide.prefersPortraitLayout(true));
        assertTrue(tall.prefersPortraitLayout(false));

        Form f = new Form("form", new BorderLayout());
        assertTrue(f.prefersPortraitLayout(true), "a form inherits the device orientation");
        assertFalse(f.prefersPortraitLayout(false));

        wide.dispose();
        tall.dispose();
        DisplayTest.flushEdt();
    }

    // ---- UITimer ---------------------------------------------------------------

    @FormTest
    void aTimerWithNoHostBindsToTheTopLevelTheUserIsIn() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();
        Window w = new Window("window", new BorderLayout());
        w.show();
        DisplayTest.flushEdt();
        Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        final boolean[] fired = new boolean[1];
        UITimer.timer(1, false, new Runnable() {
            @Override
            public void run() {
                fired[0] = true;
            }
        });
        assertTrue(w.isTopLevelShowing());
        // Bound to the window, so only the window's animation pass can run it. Bound to
        // the current form it would have been registered on a surface that is not being
        // painted there and would never have elapsed.
        for (int i = 0; i < 200 && !fired[0]; i++) {
            // repaintAnimations, not the AnimationManager: a UITimer registers itself in
            // the top level's animatable list, which is what the paint pass walks.
            w.repaintAnimations();
            DisplayTest.flushEdt();
            try {
                Thread.sleep(2);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
        }
        assertTrue(fired[0], "a timer started from a window has to elapse in that window");

        w.dispose();
        DisplayTest.flushEdt();
    }
}
