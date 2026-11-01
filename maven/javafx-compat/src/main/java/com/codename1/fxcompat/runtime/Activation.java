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
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityNode;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;

/// Gives the peer of a node that handles clicks an accessibility "activate"
/// action, so VoiceOver, TalkBack and the MCP UI tools can press it.
///
/// Codename One's own activate presses a button semantically and fires no
/// pointer event, and a JavaFX application navigates in `onMouseClicked`
/// far more often than in `onAction`: a card or a list cell is a region with
/// a click handler, not a button. The action delivers press, release and
/// click to the node itself, as a still finger at its centre would. They
/// are fired at the node rather than tapped through the form, so a child
/// drawn over the centre (a button inside a card), a transform, or a node
/// scrolled out of view cannot send the press somewhere else.
public final class Activation {

    private Activation() {
    }

    /// Adds or removes the action to match whether the node handles clicks
    /// now, and returns the semantics. Handlers come and go after the peer
    /// is made, so this runs every time the semantics are read. `added`
    /// holds the action this class added to these semantics, the only one
    /// it ever removes.
    public static AccessibilityNode sync(Node node, AccessibilityNode semantics, AccessibilityAction[] added) {
        boolean wanted = node.cn1HandlesClicks() && !node.isDisabled();
        AccessibilityAction present = null;
        for (AccessibilityAction a : semantics.getActions()) {
            if (AccessibilityAction.ACTIVATE.equals(a.getId())) {
                present = a;
                break;
            }
        }
        if (wanted && present == null) {
            added[0] = new AccessibilityAction(AccessibilityAction.ACTIVATE, null, new Tap(node));
            semantics.addAction(added[0]);
        } else if (!wanted && present != null && present == added[0]) {
            semantics.removeAction(AccessibilityAction.ACTIVATE);
            added[0] = null;
        }
        return semantics;
    }

    /// Whether a handler registered for `registered` sees a click: the
    /// click's own types and every type above them, `Event.ANY` included.
    public static boolean seesClicks(EventType<?> registered) {
        for (EventType<?> t = MouseEvent.MOUSE_CLICKED; t != null; t = t.getSuperType()) {
            if (t == registered) {
                return true;
            }
        }
        return registered == MouseEvent.MOUSE_PRESSED || registered == MouseEvent.MOUSE_RELEASED;
    }

    private static final class Tap implements AccessibilityAction.Handler {
        private final Node node;

        Tap(Node node) {
            this.node = node;
        }

        @Override
        public boolean perform(Component source, Object argument) {
            if (node.isDisabled() || node.getScene() == null) {
                return false;
            }
            Bounds b = node.getLayoutBounds();
            Point2D scene = node.localToScene(b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2);
            fire(MouseEvent.MOUSE_PRESSED, scene, 1, true);
            fire(MouseEvent.MOUSE_RELEASED, scene, 1, false);
            fire(MouseEvent.MOUSE_CLICKED, scene, 1, false);
            return true;
        }

        private void fire(EventType<MouseEvent> type, Point2D scene, int clicks, boolean down) {
            double x = scene.getX();
            double y = scene.getY();
            Event.fireEvent(node, new MouseEvent(node, node, type, x, y, x, y, MouseButton.PRIMARY, clicks,
                    false, false, false, false, down, false, false, false, false, true,
                    new PickResult(node, x, y)));
        }
    }
}
