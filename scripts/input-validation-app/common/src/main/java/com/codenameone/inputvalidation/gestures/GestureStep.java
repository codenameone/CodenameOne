/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.inputvalidation.gestures;

import com.codename1.ui.Container;

/// One gesture under test. {@link #install} populates the supplied target
/// container with whatever UI it needs and arms its detection logic. When the
/// gesture fires, the step calls {@link Callback#onDetected(String)} exactly
/// once with optional details (e.g. sample count for a drag).
public interface GestureStep {
    String name();

    void install(Container target, Callback callback);

    interface Callback {
        void onDetected(String details);
    }
}
