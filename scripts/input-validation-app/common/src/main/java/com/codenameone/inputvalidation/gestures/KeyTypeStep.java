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
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;

/// Validates that keyboard input lands in a CN1 TextField end-to-end.
/// The XCUITest driver taps the field to bring up native iOS editing
/// (CN1UITextField becomes first responder), then synthesises keystrokes
/// with `XCUIApplication.typeKey`, which drives the simulator's
/// hardware-keyboard pathway: on iOS 13.4+ those arrive as UIPress events
/// that walk the responder chain through `GLViewController`.
///
/// Regression coverage for issue #5709. The `pressesBegan:` handler in
/// CodenameOne_GLViewController.m treated every UIPress whose UIKey
/// mapped to a non-zero CN1 keycode as consumed -- and a printable
/// character maps to its unicode codepoint, which is never zero. That
/// swallowed the keystroke before UIKit could turn it into `insertText:`
/// on the focused CN1UITextField, so a hardware keyboard typed nothing
/// while the on-screen keyboard, which raises no UIPress, still worked.
/// This step fails to receive EXPECTED_TEXT and times out if the bug
/// ever returns.
public final class KeyTypeStep implements GestureStep {
    /// XCUITest types this exact string. Kept short, lowercase, and not a
    /// dictionary word so iOS auto-capitalisation / auto-correct cannot
    /// silently rewrite the characters before the CN1 TextField sees them.
    public static final String EXPECTED_TEXT = "cn1";

    @Override
    public String name() {
        return "keytype";
    }

    @Override
    public void install(Container target, Callback callback) {
        // Disable predictive text in the constraint so simulated keystrokes
        // land verbatim. Without this iOS auto-capitalisation can rewrite
        // the first character (`Cn1` instead of `cn1`) and make the
        // assertion brittle across keyboard configurations.
        final TextField field = new TextField("", "Type " + EXPECTED_TEXT + " here",
                EXPECTED_TEXT.length() + 8,
                TextArea.ANY | TextArea.NON_PREDICTIVE);
        field.setName("cn1iv-keytype-target");
        // Match TapStep / LongPressStep tap-target sizing so the XCUITest
        // driver can use the same (0.5, 0.5) coordinate to focus the field
        // on every iPhone size class on the CI runner. Sizing it any
        // smaller, or placing it NORTH, leaves the driver's centre tap in
        // empty space and the field never enters editing.
        field.getAllStyles().setPadding(48, 48, 48, 48);
        field.getAllStyles().setMargin(48, 48, 48, 48);
        final boolean[] fired = {false};
        field.addDataChangedListener((type, index) -> {
            String text = field.getText();
            if (!fired[0] && text != null && text.contains(EXPECTED_TEXT)) {
                fired[0] = true;
                callback.onDetected("text=" + text);
            }
        });
        target.add(BorderLayout.CENTER, field);
    }
}
