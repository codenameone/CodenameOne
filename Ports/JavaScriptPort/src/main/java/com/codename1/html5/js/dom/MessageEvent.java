/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Message event interface for postMessage etc.
 * https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent
 */
public interface MessageEvent extends Event {
    Object getData();
    String getDataAsString();
    String getOrigin();
    String getLastEventId();
    Object getSource();
}