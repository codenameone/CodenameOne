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

/// A quick stroke of the finger in one of the four directions.
///
/// The scene produces one when a press is dragged a clear distance along
/// one axis and let go within a moment. It is delivered to the node the
/// press began on, after the release of that press, and it carries the
/// position the stroke began at.
public final class SwipeEvent extends GestureEvent {

    private static final long serialVersionUID = 1L;

    /// The supertype of the four swipes.
    public static final EventType<SwipeEvent> ANY = new EventType<SwipeEvent>(GestureEvent.ANY, "ANY_SWIPE");

    /// A stroke to the left.
    public static final EventType<SwipeEvent> SWIPE_LEFT = new EventType<SwipeEvent>(ANY, "SWIPE_LEFT");

    /// A stroke to the right.
    public static final EventType<SwipeEvent> SWIPE_RIGHT = new EventType<SwipeEvent>(ANY, "SWIPE_RIGHT");

    /// A stroke upwards.
    public static final EventType<SwipeEvent> SWIPE_UP = new EventType<SwipeEvent>(ANY, "SWIPE_UP");

    /// A stroke downwards.
    public static final EventType<SwipeEvent> SWIPE_DOWN = new EventType<SwipeEvent>(ANY, "SWIPE_DOWN");

    private final int touchCount;

    /// Creates a swipe at scene coordinates.
    public SwipeEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<SwipeEvent> eventType, @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct, @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType, x, y, screenX, screenY, shiftDown, controlDown, altDown, metaDown, direct,
                false, pickResult);
        this.touchCount = touchCount;
    }

    /// Creates a swipe with no source or target.
    public SwipeEvent(@NamedArg("eventType") EventType<SwipeEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct, @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown, altDown, metaDown, direct,
                touchCount, pickResult);
    }

    /// Returns how many fingers made the stroke.
    public int getTouchCount() {
        return touchCount;
    }

    @Override
    public SwipeEvent copyFor(Object newSource, EventTarget newTarget) {
        return new SwipeEvent(newSource, newTarget, getEventType(), getSceneX(), getSceneY(), getScreenX(),
                getScreenY(), isShiftDown(), isControlDown(), isAltDown(), isMetaDown(), isDirect(), touchCount,
                getPickResult());
    }

    /// Returns a copy with another source, target and type.
    public SwipeEvent copyFor(Object newSource, EventTarget newTarget, EventType<SwipeEvent> type) {
        return new SwipeEvent(newSource, newTarget, type, getSceneX(), getSceneY(), getScreenX(), getScreenY(),
                isShiftDown(), isControlDown(), isAltDown(), isMetaDown(), isDirect(), touchCount, getPickResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<SwipeEvent> getEventType() {
        return (EventType<SwipeEvent>) super.getEventType();
    }

    @Override
    public String toString() {
        return "SwipeEvent [type = " + getEventType() + ", touchCount = " + touchCount + ", x = " + getX()
                + ", y = " + getY() + "]";
    }
}
