/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.ajax;

import com.codename1.html5.js.JSFunctor;

/**
 * ReadyStateChange handler callback interface.
 */
@JSFunctor
public interface ReadyStateChangeHandler {
    void stateChanged();
}