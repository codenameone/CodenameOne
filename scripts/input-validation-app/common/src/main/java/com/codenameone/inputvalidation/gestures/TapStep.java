/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.layouts.BorderLayout;

/// Validates that a single tap dispatches Button.actionPerformed end-to-end.
/// This is the regression PR #5003 fixed -- a window-level UITapGestureRecognizer
/// on iOS 26 was eating the touch before CN1TapGestureRecognizer saw it, so
/// every button tap silently did nothing.
public final class TapStep implements GestureStep {
    @Override
    public String name() {
        return "tap";
    }

    @Override
    public void install(Container target, Callback callback) {
        Button btn = new Button("Tap me");
        btn.setName("cn1iv-tap-target");
        btn.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        btn.getAllStyles().setPadding(48, 48, 48, 48);
        btn.getAllStyles().setMargin(48, 48, 48, 48);
        btn.getAllStyles().setBgColor(0x2563eb);
        btn.getAllStyles().setBgTransparency(255);
        btn.getAllStyles().setFgColor(0xffffff);
        btn.addActionListener(evt -> callback.onDetected("x=" + evt.getX() + ",y=" + evt.getY()));
        target.add(BorderLayout.CENTER, btn);
    }
}
