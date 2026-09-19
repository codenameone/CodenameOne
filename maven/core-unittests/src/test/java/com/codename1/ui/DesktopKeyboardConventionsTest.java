/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;

import java.lang.reflect.Field;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keyboard conventions a desktop toolkit has and Codename One did not: Tab and Shift-Tab
 * move focus, and Escape cancels. The traversal order itself is old ({@code TabIterator},
 * {@code getNextComponent}); what is new is that a key is wired to it.
 *
 * <p>Every case here is also asserted in its mobile form, because the whole feature is gated on
 * {@code isDesktop()} and a gate nobody tests in both directions is not a gate.</p>
 */
class DesktopKeyboardConventionsTest extends UITestBase {

    /** Tab, as a port delivers it: the character code, not an AWT virtual key. */
    private static final int KEY_TAB = 9;

    /** Escape, likewise. */
    private static final int KEY_ESCAPE = 27;

    private Form threeButtonForm() {
        Form f = new Form("Keys", new BoxLayout(BoxLayout.Y_AXIS));
        f.add(new Button("One")).add(new Button("Two")).add(new Button("Three"));
        return f;
    }

    private Button button(Form f, int index) {
        return (Button) f.getContentPane().getComponentAt(index);
    }

    @FormTest
    void tabMovesFocusForwardOnDesktop() {
        implementation.setDesktop(true);
        Form f = threeButtonForm();
        f.show();
        DisplayTest.flushEdt();

        f.setFocused(button(f, 0));
        f.keyPressed(KEY_TAB);
        DisplayTest.flushEdt();

        assertSame(button(f, 1), f.getFocused(), "Tab must advance focus to the next component");
    }

    @FormTest
    void shiftTabMovesFocusBackwardOnDesktop() {
        implementation.setDesktop(true);
        implementation.setShiftKeyDown(true);
        Form f = threeButtonForm();
        f.show();
        DisplayTest.flushEdt();

        f.setFocused(button(f, 2));
        f.keyPressed(KEY_TAB);
        DisplayTest.flushEdt();

        assertSame(button(f, 1), f.getFocused(), "Shift-Tab must walk focus backwards");
    }

    @FormTest
    void tabIsInertOnMobile() {
        // The mobile branch is the one that must not move, because every existing screenshot
        // baseline on every phone port was captured with Tab doing nothing.
        implementation.setDesktop(false);
        Form f = threeButtonForm();
        f.show();
        DisplayTest.flushEdt();

        Button first = button(f, 0);
        f.setFocused(first);
        f.keyPressed(KEY_TAB);
        DisplayTest.flushEdt();

        assertSame(first, f.getFocused(), "Tab must not traverse focus on a phone");
    }

    @FormTest
    void escapeFiresTheFormsBackCommand() {
        implementation.setDesktop(true);
        Form f = threeButtonForm();
        final boolean[] fired = new boolean[1];
        Command back = new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0] = true;
            }
        };
        f.setBackCommand(back);
        f.show();
        DisplayTest.flushEdt();

        f.keyPressed(KEY_ESCAPE);
        DisplayTest.flushEdt();

        assertTrue(fired[0], "Escape must fire the back command on the desktop");
    }

    @FormTest
    void oneEscapeKeystrokeFiresTheBackCommandOnlyOnce() throws Exception {
        // Escape is the back key on the desktop, not merely a convention beside it:
        // JavaSEPort.getBackKeyCode() returns VK_ESCAPE and Display.init assigns it to
        // MenuBar.backSK. So the PRESS runs the back command through the desktop path and the
        // RELEASE satisfies menuBar.handlesKeycode(backSK) and runs it a second time -- one
        // keystroke, two screens popped.
        //
        // backSK has to be forced to Escape for this to be a real test. The test
        // implementation reports -1 as its back key, so a press/release pair would take the
        // desktop path and never reach the MenuBar branch at all, and the test would pass with
        // the bug present. Reflection matches what MenuBarTest already does with these fields.
        Field backField = MenuBar.class.getDeclaredField("backSK");
        backField.setAccessible(true);
        int originalBack = backField.getInt(null);
        try {
            backField.setInt(null, KEY_ESCAPE);
            implementation.setDesktop(true);
            Form f = threeButtonForm();
            final int[] fired = new int[1];
            Command back = new Command("Back") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    fired[0]++;
                }
            };
            f.setBackCommand(back);
            f.show();
            DisplayTest.flushEdt();

            f.keyPressed(KEY_ESCAPE);
            f.keyReleased(KEY_ESCAPE);
            DisplayTest.flushEdt();

            assertEquals(1, fired[0],
                    "one Escape keystroke must invoke the back command exactly once; twice means "
                    + "the release reached the MenuBar back-key path after the press had already "
                    + "been consumed");
        } finally {
            backField.setInt(null, originalBack);
        }
    }

    @FormTest
    void theEscapeSuppressionIsNotSticky() throws Exception {
        // The suppression must last exactly one release. If the flag were left set, the NEXT
        // Escape's release would be swallowed too and the back command would run once per two
        // keystrokes -- trading a double fire for a missed one.
        //
        // Note what this deliberately does not assert: that a release with no matching press
        // reaches the back command. Measured, it does not, and it did not before this change
        // either -- the suppression cannot affect it, because the flag is only ever set by a
        // press. Asserting that would be inventing a contract rather than testing one.
        Field backField = MenuBar.class.getDeclaredField("backSK");
        backField.setAccessible(true);
        int originalBack = backField.getInt(null);
        try {
            backField.setInt(null, KEY_ESCAPE);
            implementation.setDesktop(true);
            Form f = threeButtonForm();
            final int[] fired = new int[1];
            Command back = new Command("Back") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    fired[0]++;
                }
            };
            f.setBackCommand(back);
            f.show();
            DisplayTest.flushEdt();

            f.keyPressed(KEY_ESCAPE);
            f.keyReleased(KEY_ESCAPE);
            f.keyPressed(KEY_ESCAPE);
            f.keyReleased(KEY_ESCAPE);
            DisplayTest.flushEdt();

            assertEquals(2, fired[0],
                    "two Escape keystrokes must invoke the back command twice; one means the "
                    + "release suppression stayed set after the first keystroke");
        } finally {
            backField.setInt(null, originalBack);
        }
    }

    @FormTest
    void escapeWithoutABackCommandDoesNothing() {
        // Deliberate: Escape must never be able to exit an application, so a form with nothing
        // to cancel simply ignores it rather than falling through to any exit path.
        implementation.setDesktop(true);
        Form f = threeButtonForm();
        f.show();
        DisplayTest.flushEdt();

        Button first = button(f, 0);
        f.setFocused(first);
        f.keyPressed(KEY_ESCAPE);
        DisplayTest.flushEdt();

        assertSame(f, Display.getInstance().getCurrent(), "the form must still be showing");
        assertSame(first, f.getFocused(), "and focus must be where it was");
    }

    @FormTest
    void escapeIsInertOnMobile() {
        implementation.setDesktop(false);
        Form f = threeButtonForm();
        final boolean[] fired = new boolean[1];
        Command back = new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0] = true;
            }
        };
        f.setBackCommand(back);
        f.show();
        DisplayTest.flushEdt();

        f.keyPressed(KEY_ESCAPE);
        DisplayTest.flushEdt();

        assertFalse(fired[0], "Escape is a desktop convention and must stay inert on a phone");
    }

    @FormTest
    void escapeDisposesADialogWithNoBackCommand() {
        implementation.setDesktop(true);
        Form host = threeButtonForm();
        host.show();
        DisplayTest.flushEdt();

        Dialog d = new Dialog("Confirm");
        d.add(new Label("Body"));
        d.setDisposeWhenPointerOutOfBounds(false);
        d.showModeless();
        DisplayTest.flushEdt();

        d.keyPressed(KEY_ESCAPE);
        DisplayTest.flushEdt();

        assertFalse(d.isVisible() && d.getParent() != null,
                "Escape must close a dialog that has nothing else to cancel");
    }

    /**
     * The theme constant only speaks when the project did not. Windows and macOS themes carry
     * {@code native}, GNOME carries {@code custom}, and a project that spelled out
     * {@code desktop.titleBar} must still win.
     */
    @FormTest
    void themeConstantSuppliesTheTitleBarModeWhenNothingElseDid() {
        implementation.setDesktop(true);
        Hashtable props = new Hashtable();
        props.put("@desktopTitleBarMode", "native");
        UIManager.getInstance().addThemeProps(props);
        Toolbar.setGlobalToolbar(true);

        Form f = new Form("Themed");
        f.show();
        DisplayTest.flushEdt();

        assertEquals("native", f.getDesktopTitleBarMode(),
                "with no build hint the installed theme's constant must be read");
    }

    @FormTest
    void aConfiguredModeOutranksTheThemeConstant() {
        implementation.setDesktop(true);
        implementation.setConfiguredDesktopTitleBarMode("toolbar");
        Hashtable props = new Hashtable();
        props.put("@desktopTitleBarMode", "native");
        UIManager.getInstance().addThemeProps(props);
        Toolbar.setGlobalToolbar(true);

        Form f = new Form("Themed");
        f.show();
        DisplayTest.flushEdt();

        assertEquals("toolbar", f.getDesktopTitleBarMode(),
                "a project that asked for the legacy look must keep it");
    }

    @FormTest
    void themeConstantIsIgnoredOffTheDesktop() {
        implementation.setDesktop(false);
        Hashtable props = new Hashtable();
        props.put("@desktopTitleBarMode", "native");
        UIManager.getInstance().addThemeProps(props);

        Form f = new Form("Themed");
        f.show();
        DisplayTest.flushEdt();

        assertEquals("toolbar", f.getDesktopTitleBarMode(),
                "a phone that somehow loaded a desktop theme still draws its own chrome");
    }

    /**
     * The four interactive-scrollbar UIIDs are picked by {@code LookAndFeel.initScroll} but were
     * seeded by nothing, so a theme that turned the constant on without defining all four drew a
     * track and a thumb out of the blank default style -- an invisible scrollbar that still
     * reserved its gutter, with nothing reporting a problem.
     */
    @FormTest
    void theInteractiveScrollbarUiidsAreSeeded() {
        UIManager m = UIManager.getInstance();
        assertNotNull(m.getComponentStyle("DesktopScroll"), "DesktopScroll must have a style");
        assertNotNull(m.getComponentStyle("DesktopScrollThumb"), "DesktopScrollThumb must have a style");
        assertNotNull(m.getComponentStyle("DesktopHorizontalScroll"),
                "DesktopHorizontalScroll must have a style");
        assertNotNull(m.getComponentStyle("DesktopHorizontalScrollThumb"),
                "DesktopHorizontalScrollThumb must have a style");

        assertTrue(m.getComponentStyle("DesktopScroll").getPaddingRight(false) > 0,
                "the vertical track needs a gutter wide enough to grab");
        assertTrue(m.getComponentStyle("DesktopHorizontalScroll").getPaddingTop() > 0,
                "the horizontal track needs one too");
        assertNotNull(m.getComponentSelectedStyle("DesktopScrollThumb"),
                "the thumb needs a hover (selected) style to highlight with");
        assertNotNull(m.getComponentCustomStyle("DesktopScrollThumb", "press"),
                "and a pressed style to highlight with while dragged");
    }
}
