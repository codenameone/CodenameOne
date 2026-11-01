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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityNode;

import javafx.scene.Node;

/// Gives the peer of a node that handles clicks an accessibility "activate"
/// action, so VoiceOver, TalkBack and the MCP UI tools can press it.
///
/// Codename One's own activate presses a button semantically and fires no
/// pointer event, and a JavaFX application navigates in `onMouseClicked`
/// far more often than in `onAction`: a card or a list cell is a region with
/// a click handler, not a button. The action here taps the middle of the
/// peer through the form, the path a finger takes, so the scene picks the
/// target and delivers press, release and click exactly as for a touch.
public final class Activation {

    private Activation() {
    }

    /// Adds or removes the action to match whether the node handles clicks
    /// now, and returns the semantics. Handlers come and go after the peer
    /// is made, so this runs every time the semantics are read.
    public static AccessibilityNode sync(Component peer, Node node, AccessibilityNode semantics) {
        boolean wanted = node.cn1HandlesClicks();
        boolean present = false;
        for (AccessibilityAction a : semantics.getActions()) {
            if (AccessibilityAction.ACTIVATE.equals(a.getId())) {
                present = true;
                break;
            }
        }
        if (wanted && !present) {
            semantics.addAction(new AccessibilityAction(AccessibilityAction.ACTIVATE, null, new Tap()));
        } else if (!wanted && present) {
            semantics.removeAction(AccessibilityAction.ACTIVATE);
        }
        return semantics;
    }

    private static final class Tap implements AccessibilityAction.Handler {
        @Override
        public boolean perform(Component source, Object argument) {
            Form form = source.getComponentForm();
            if (form == null || !source.isEnabled()) {
                return false;
            }
            int x = source.getAbsoluteX() + source.getWidth() / 2;
            int y = source.getAbsoluteY() + source.getHeight() / 2;
            form.pointerPressed(x, y);
            form.pointerReleased(x, y);
            return true;
        }
    }
}
