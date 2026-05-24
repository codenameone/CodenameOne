/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;

/// Validates that keyboard input lands in a CN1 TextField end-to-end.
/// The XCUITest driver taps the field to bring up native iOS editing
/// (CN1UITextField becomes first responder), then synthesises keystrokes
/// via `XCUIApplication.typeText`. typeText drives the simulator's
/// hardware-keyboard pathway, which on iOS 13.4+ surfaces as UIPress
/// events that walk the responder chain through `GLViewController`.
///
/// Regression coverage for #5010: a pressesBegan: handler in
/// CodenameOne_GLViewController.m treated every UIPress whose UIKey
/// mapped to a non-zero CN1 keycode as consumed -- and printable
/// characters map to their unicode codepoint, which is always non-zero.
/// That swallowed every HW keystroke before UIKit could convert it into
/// insertText: on the focused CN1UITextField, and on iOS 26.x devices
/// the same path also swallowed virtual-keyboard input. The fix bypasses
/// the intercept while editingComponent != nil. This step fails to
/// receive EXPECTED_TEXT and times out if either bug ever returns.
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
        // driver can use the same (0.5, 0.5) coordinate to focus the
        // field on every iPhone size class on the CI runner. A NORTH
        // placement put the field above where the existing steps tap
        // and the driver missed it -- see #5010 CI failure.
        field.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
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
