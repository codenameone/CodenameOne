/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.dom.events;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSFunctor;

public interface Event extends JSObject {
    String getType();
    JSObject getTarget();
    void preventDefault();
    void stopPropagation();
    void stopImmediatePropagation();
    JSObject getCurrentTarget();
    boolean isDefaultPrevented();
    boolean isPropagationStopped();
}

@JSFunctor
public interface EventListener<T extends Event> extends JSObject {
    void handleEvent(T evt);
}