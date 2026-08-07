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

import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Key codes and character codes share one value space, so a port is free to map a soft key onto a
 * value that is also a printable character. The desktop port maps the left soft key to VK_F1, which
 * is 112 -- the character code of a lowercase 'p'. Before this was handled, a component editing text
 * never received that character and one letter of the alphabet silently stopped working.
 */
class SoftKeyCollisionTest extends UITestBase {

    /** Records what actually reached the component, which is the only thing that matters here. */
    private static final class RecordingInput extends Component {
        final StringBuilder typed = new StringBuilder();

        boolean editsText = true;

        RecordingInput() {
            setFocusable(true);
            // Lists and editable sliders set this too, which is why it alone must not divert soft keys.
            setHandlesInput(true);
        }

        /** Declares that key codes reaching this component are text, exactly as the editors do. */
        @Override
        protected boolean consumesRawTextInput() {
            return editsText;
        }

        @Override
        public void keyReleased(int keyCode) {
            typed.append((char) keyCode);
        }
    }

    private RecordingInput showFocusedInput() {
        RecordingInput input = new RecordingInput();
        Form form = new Form("keys", new BorderLayout());
        form.add(BorderLayout.CENTER, input);
        form.show();
        flushSerialCalls();
        form.setFocused(input);
        return input;
    }

    /// The desktop port maps the left soft key to VK_F1. The test implementation reports no soft
    /// keys at all, so the collision is reproduced explicitly rather than inherited from it.
    private static final int LOWERCASE_P = 'p';

    @Test
    void aFocusedInputReceivesCharactersThatCollideWithASoftKey() {
        int previous = MenuBar.leftSK;
        MenuBar.leftSK = LOWERCASE_P;
        try {
            RecordingInput input = showFocusedInput();

            input.getComponentForm().keyReleased(LOWERCASE_P);

            assertEquals("p", input.typed.toString(),
                    "a component that handles its own input must receive a character whose code"
                            + " equals the left soft key, or that character can never be typed");
        } finally {
            MenuBar.leftSK = previous;
        }
    }

    /**
     * A port is free to use a negative code for Back. That code cannot stand for a character, so a
     * focused text editor must not divert it: the form's back command, pop guard and
     * minimize-on-back all hang off the menu bar seeing it, and the editor would discard it anyway.
     */
    @Test
    void aNegativeSoftKeyReachesTheMenuBarEvenWhileTextIsBeingEdited() {
        int previous = MenuBar.leftSK;
        MenuBar.leftSK = -11;
        try {
            RecordingInput input = showFocusedInput();
            assertTrue(input.editsText, "this test is about a component that does edit text");

            input.getComponentForm().keyReleased(-11);

            assertEquals("", input.typed.toString(),
                    "a code that cannot be typed must stay with the menu bar even while editing");
        } finally {
            MenuBar.leftSK = previous;
        }
    }

    @Test
    void softKeysStillReachTheMenuBarWhenNothingIsEditing() {
        int previous = MenuBar.leftSK;
        MenuBar.leftSK = LOWERCASE_P;
        try {
            RecordingInput input = showFocusedInput();
            // Hand the keyboard back: the component no longer claims raw input.
            input.setHandlesInput(false);
            input.editsText = false;

            input.getComponentForm().keyReleased(LOWERCASE_P);

            assertEquals("", input.typed.toString(),
                    "soft key handling must still take precedence for components that do not edit text");
        } finally {
            MenuBar.leftSK = previous;
        }
    }

    /**
     * A list in single focus mode, an editable slider and a map all set handlesInput so the focus
     * manager leaves their arrow keys alone. None of them turn key codes into characters, so a soft
     * key must still reach the menu bar and fire its command while one of them holds the focus.
     */
    @Test
    void aComponentThatMerelyHandlesInputDoesNotSwallowSoftKeys() {
        int previous = MenuBar.leftSK;
        MenuBar.leftSK = LOWERCASE_P;
        try {
            RecordingInput input = showFocusedInput();
            input.editsText = false;
            assertTrue(input.handlesInput(), "this is the case being guarded against");

            input.getComponentForm().keyReleased(LOWERCASE_P);

            assertEquals("", input.typed.toString(),
                    "handlesInput alone must not divert a soft key away from the menu bar");
        } finally {
            MenuBar.leftSK = previous;
        }
    }
}
