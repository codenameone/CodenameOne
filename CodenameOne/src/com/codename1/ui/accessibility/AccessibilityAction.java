/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ui.accessibility;

import com.codename1.ui.Component;

/// An action that assistive technology can invoke on a semantic node.
public final class AccessibilityAction {
    public static final String ACTIVATE = "activate";
    public static final String LONG_PRESS = "longPress";
    public static final String INCREMENT = "increment";
    public static final String DECREMENT = "decrement";
    public static final String SET_VALUE = "setValue";
    public static final String SET_TEXT = "setText";
    public static final String FOCUS = "focus";
    public static final String DISMISS = "dismiss";
    public static final String EXPAND = "expand";
    public static final String COLLAPSE = "collapse";
    public static final String SCROLL_FORWARD = "scrollForward";
    public static final String SCROLL_BACKWARD = "scrollBackward";
    public static final String COPY = "copy";
    public static final String CUT = "cut";
    public static final String PASTE = "paste";
    public static final String SHOW_ON_SCREEN = "showOnScreen";
    public static final String SET_SELECTION = "setSelection";
    public static final String MOVE_CURSOR_FORWARD_BY_CHARACTER = "moveCursorForwardByCharacter";
    public static final String MOVE_CURSOR_BACKWARD_BY_CHARACTER = "moveCursorBackwardByCharacter";
    public static final String MOVE_CURSOR_FORWARD_BY_WORD = "moveCursorForwardByWord";
    public static final String MOVE_CURSOR_BACKWARD_BY_WORD = "moveCursorBackwardByWord";

    /// Handles an accessibility action on the Codename One EDT.
    public interface Handler {
        boolean perform(Component component, Object argument);
    }

    private final String id;
    private final String label;
    private final Handler handler;
    private final boolean enabled;

    public AccessibilityAction(String id, String label, Handler handler) {
        this(id, label, handler, true);
    }

    public AccessibilityAction(String id, String label, Handler handler, boolean enabled) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Accessibility action id must not be empty");
        }
        this.id = id;
        this.label = label;
        this.handler = handler;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }
    public String getLabel() {
        return label;
    }
    public boolean isEnabled() {
        return enabled;
    }

    public boolean perform(Component component, Object argument) {
        return enabled && handler != null && handler.perform(component, argument);
    }
}
