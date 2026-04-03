/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.dom.events;

import org.teavm.jso.JSObject;

public interface MouseEvent extends Event {
    int getClientX();
    int getClientY();
    int getScreenX();
    int getScreenY();
    int getButton();
    boolean isShiftKey();
    boolean isCtrlKey();
    boolean isAltKey();
    boolean isMetaKey();
}