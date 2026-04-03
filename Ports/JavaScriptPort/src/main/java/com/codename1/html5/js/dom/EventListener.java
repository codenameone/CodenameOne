/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSFunctor;

/**
 * Event listener interface for handling DOM events.
 */
@JSFunctor
public interface EventListener<T extends Event> extends JSObject {
    void handleEvent(T evt);
}