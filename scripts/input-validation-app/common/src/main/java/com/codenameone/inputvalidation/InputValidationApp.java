/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation;

import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codenameone.inputvalidation.gestures.GestureSuite;

/// Lifecycle entry point for the input-validation CN1 app. The whole app does
/// one thing: it runs `GestureSuite` once and exits. No theme, no resources,
/// no asset bundle -- by design, so a regression in input handling can never
/// hide behind a missing texture, a slow startup, or a stale screenshot
/// baseline.
public class InputValidationApp extends Lifecycle {
    @Override
    public void runApp() {
        Runnable suite = () -> new GestureSuite().start();
        if ("HTML5".equals(Display.getInstance().getPlatformName())) {
            CN.callSerially(suite);
        } else {
            new Thread(suite, "CN1IV-Suite").start();
        }
    }
}
