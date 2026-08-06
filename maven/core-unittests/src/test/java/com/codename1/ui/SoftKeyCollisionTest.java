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

        RecordingInput() {
            setFocusable(true);
            // Declares that this component owns the keyboard, exactly as the text editors do.
            setHandlesInput(true);
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

    @Test
    void softKeysStillReachTheMenuBarWhenNothingIsEditing() {
        int previous = MenuBar.leftSK;
        MenuBar.leftSK = LOWERCASE_P;
        try {
            RecordingInput input = showFocusedInput();
            // Hand the keyboard back: the component no longer claims raw input.
            input.setHandlesInput(false);

            input.getComponentForm().keyReleased(LOWERCASE_P);

            assertEquals("", input.typed.toString(),
                    "soft key handling must still take precedence for components that do not edit text");
        } finally {
            MenuBar.leftSK = previous;
        }
    }
}
