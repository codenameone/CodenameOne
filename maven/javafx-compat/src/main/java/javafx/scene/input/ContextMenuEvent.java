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
package javafx.scene.input;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/// The user asked for a context menu: a secondary click with a mouse, a
/// long press on a touch screen.
public class ContextMenuEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// A context menu was asked for.
    public static final EventType<ContextMenuEvent> CONTEXT_MENU_REQUESTED = new EventType<ContextMenuEvent>(
            InputEvent.ANY, "CONTEXTMENUREQUESTED");

    /// Every context menu event.
    public static final EventType<ContextMenuEvent> ANY = CONTEXT_MENU_REQUESTED;

    private final boolean keyboardTrigger;
    private transient double x;
    private transient double y;
    private final double screenX;
    private final double screenY;
    private final double sceneX;
    private final double sceneY;
    private final transient PickResult pickResult;

    /// Creates a context menu event; `x` and `y` are scene coordinates.
    public ContextMenuEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<ContextMenuEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("keyboardTrigger") boolean keyboardTrigger, @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType);
        this.sceneX = x;
        this.sceneY = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.keyboardTrigger = keyboardTrigger;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        localize(source);
    }

    /// Creates a context menu event with no source and no target.
    public ContextMenuEvent(@NamedArg("eventType") EventType<ContextMenuEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("keyboardTrigger") boolean keyboardTrigger, @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, keyboardTrigger, pickResult);
    }

    private void localize(Object source) {
        if (source instanceof Node) {
            Point2D local = ((Node) source).sceneToLocal(sceneX, sceneY);
            x = local.getX();
            y = local.getY();
        } else {
            x = sceneX;
            y = sceneY;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContextMenuEvent copyFor(Object newSource, EventTarget newTarget) {
        if (getClass() != ContextMenuEvent.class) {
            super.copyFor(newSource, newTarget);
            localize(getSource());
            return this;
        }
        return new ContextMenuEvent(newSource, newTarget, (EventType<ContextMenuEvent>) getEventType(), sceneX,
                sceneY, screenX, screenY, keyboardTrigger, pickResult);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<ContextMenuEvent> getEventType() {
        return (EventType<ContextMenuEvent>) super.getEventType();
    }

    /// Returns whether the keyboard asked for the menu.
    public boolean isKeyboardTrigger() {
        return keyboardTrigger;
    }

    /// Returns the x of the request in the source's coordinates.
    public final double getX() {
        return x;
    }

    /// Returns the y of the request in the source's coordinates.
    public final double getY() {
        return y;
    }

    /// Always 0: this layer is two dimensional.
    public final double getZ() {
        return 0;
    }

    /// Returns the x of the request on the screen.
    public final double getScreenX() {
        return screenX;
    }

    /// Returns the y of the request on the screen.
    public final double getScreenY() {
        return screenY;
    }

    /// Returns the x of the request in the scene.
    public final double getSceneX() {
        return sceneX;
    }

    /// Returns the y of the request in the scene.
    public final double getSceneY() {
        return sceneY;
    }

    /// Returns what was under the pointer.
    public final PickResult getPickResult() {
        return pickResult;
    }
}
