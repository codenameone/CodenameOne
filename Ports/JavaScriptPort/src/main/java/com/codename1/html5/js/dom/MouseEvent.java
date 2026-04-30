/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.dom;

/**
 * Interface for the JavaScript MouseEvent.
 * https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent
 */
public interface MouseEvent extends Event {
    int getClientX();
    int getClientY();
    int getPageX();
    int getPageY();
    int getScreenX();
    int getScreenY();
    int getButton();
    int getButtons();
    boolean isCtrlKey();
    boolean isShiftKey();
    boolean isAltKey();
    boolean isMetaKey();
    int getDetail();
}
