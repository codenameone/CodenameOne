/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSFunctor;

@JSFunctor
public interface AnimationFrameCallback extends JSObject {
    void onAnimationFrame(double timestamp);
}