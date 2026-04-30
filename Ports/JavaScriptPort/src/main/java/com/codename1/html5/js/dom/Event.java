/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript Event object.
 * https://developer.mozilla.org/en-US/docs/Web/API/Event
 */
public interface Event extends JSObject {
    String getType();
    Object getTarget();
    Object getCurrentTarget();
    int getTimestamp();
    void stopPropagation();
    void preventDefault();
    boolean isDefaultPrevented();
    boolean isBubbles();
    boolean isCancelable();
}
