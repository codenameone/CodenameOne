/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

final class JavaScriptAnimationFrameCallback {
    private final HTML5Implementation implementation;

    JavaScriptAnimationFrameCallback(HTML5Implementation implementation) {
        this.implementation = implementation;
    }

    public void onAnimationFrame(double time) {
        implementation.handleAnimationFrame(time);
    }
}
