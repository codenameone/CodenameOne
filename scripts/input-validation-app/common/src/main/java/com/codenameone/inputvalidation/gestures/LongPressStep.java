/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.layouts.BorderLayout;

/// Validates that addLongPressListener fires for a press-and-hold gesture.
/// Long-press uses the same touch chain as tap on iOS; if a window-level
/// recognizer cancels touches mid-press (the PR #5003 path), the long-press
/// listener never fires and this step times out.
public final class LongPressStep implements GestureStep {
    @Override
    public String name() {
        return "longpress";
    }

    @Override
    public void install(Container target, Callback callback) {
        Button btn = new Button("Long-press me");
        btn.setName("cn1iv-longpress-target");
        btn.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        btn.getAllStyles().setPadding(48, 48, 48, 48);
        btn.getAllStyles().setMargin(48, 48, 48, 48);
        btn.getAllStyles().setBgColor(0x16a34a);
        btn.getAllStyles().setBgTransparency(255);
        btn.getAllStyles().setFgColor(0xffffff);
        final long[] pressedAt = {0L};
        btn.addPointerPressedListener(evt -> pressedAt[0] = System.currentTimeMillis());
        btn.addLongPressListener(evt -> {
            long elapsed = pressedAt[0] == 0L ? -1 : (System.currentTimeMillis() - pressedAt[0]);
            callback.onDetected("durMs=" + elapsed);
        });
        target.add(BorderLayout.CENTER, btn);
    }
}
