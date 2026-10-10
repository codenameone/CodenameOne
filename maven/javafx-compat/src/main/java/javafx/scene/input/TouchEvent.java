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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// What a finger on the screen did.
///
/// Codename One hands every pointer to the scene as a mouse event, which
/// is what JavaFX itself does on a screen it does not report as a touch
/// screen: `Platform.isSupported(ConditionalFeature.INPUT_TOUCH)` answers
/// `false`, and the scene never produces a touch event of its own. The
/// type is here so that handlers can be registered and so that an
/// application can fire one. A handler on `onTouchPressed` that repeats
/// what `onMouseClicked` or `onAction` does is therefore reached through
/// the mouse handler alone.
public final class TouchEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// The supertype of every touch event.
    public static final EventType<TouchEvent> ANY = new EventType<TouchEvent>(InputEvent.ANY, "TOUCH");

    /// A finger went down.
    public static final EventType<TouchEvent> TOUCH_PRESSED = new EventType<TouchEvent>(ANY, "TOUCH_PRESSED");

    /// A finger moved.
    public static final EventType<TouchEvent> TOUCH_MOVED = new EventType<TouchEvent>(ANY, "TOUCH_MOVED");

    /// A finger was lifted.
    public static final EventType<TouchEvent> TOUCH_RELEASED = new EventType<TouchEvent>(ANY, "TOUCH_RELEASED");

    /// A finger stayed where it was while another moved.
    public static final EventType<TouchEvent> TOUCH_STATIONARY = new EventType<TouchEvent>(ANY,
            "TOUCH_STATIONARY");

    private final int eventSetId;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;
    private final transient TouchPoint touchPoint;
    private final transient List<TouchPoint> touchPoints;

    /// Creates a touch event for one finger out of those on the screen.
    public TouchEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<TouchEvent> eventType, @NamedArg("touchPoint") TouchPoint touchPoint,
            @NamedArg("touchPoints") List<TouchPoint> touchPoints, @NamedArg("eventSetId") int eventSetId,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown) {
        super(source, target, eventType);
        this.touchPoint = touchPoint;
        List<TouchPoint> all = new ArrayList<TouchPoint>();
        if (touchPoints != null) {
            all.addAll(touchPoints);
        }
        this.touchPoints = Collections.unmodifiableList(all);
        this.eventSetId = eventSetId;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        localize(source);
    }

    /// Creates a touch event with no source or target.
    public TouchEvent(@NamedArg("eventType") EventType<TouchEvent> eventType,
            @NamedArg("touchPoint") TouchPoint touchPoint, @NamedArg("touchPoints") List<TouchPoint> touchPoints,
            @NamedArg("eventSetId") int eventSetId, @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown, @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown) {
        this(null, null, eventType, touchPoint, touchPoints, eventSetId, shiftDown, controlDown, altDown, metaDown);
    }

    private void localize(Object source) {
        if (touchPoint != null) {
            touchPoint.localize(source);
        }
        for (int i = 0; i < touchPoints.size(); i++) {
            TouchPoint p = touchPoints.get(i);
            if (p != null && p != touchPoint) {
                p.localize(source);
            }
        }
    }

    /// Returns how many fingers were on the screen.
    public int getTouchCount() {
        return touchPoints.size();
    }

    @Override
    public TouchEvent copyFor(Object newSource, EventTarget newTarget) {
        return new TouchEvent(newSource, newTarget, getEventType(), touchPoint, touchPoints, eventSetId, shiftDown,
                controlDown, altDown, metaDown);
    }

    /// Returns a copy with another source, target and type.
    public TouchEvent copyFor(Object newSource, EventTarget newTarget, EventType<TouchEvent> type) {
        return new TouchEvent(newSource, newTarget, type, touchPoint, touchPoints, eventSetId, shiftDown,
                controlDown, altDown, metaDown);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<TouchEvent> getEventType() {
        return (EventType<TouchEvent>) super.getEventType();
    }

    /// Returns the number shared by the events one movement of several
    /// fingers produced.
    public final int getEventSetId() {
        return eventSetId;
    }

    /// Returns whether Shift was held.
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /// Returns whether Control was held.
    public final boolean isControlDown() {
        return controlDown;
    }

    /// Returns whether Alt was held.
    public final boolean isAltDown() {
        return altDown;
    }

    /// Returns whether Meta was held.
    public final boolean isMetaDown() {
        return metaDown;
    }

    /// Returns the finger this event is about.
    public TouchPoint getTouchPoint() {
        return touchPoint;
    }

    /// Returns every finger on the screen, this event's among them.
    public List<TouchPoint> getTouchPoints() {
        return touchPoints;
    }

    @Override
    public String toString() {
        return "TouchEvent [type = " + getEventType() + ", touchCount = " + getTouchCount() + ", eventSetId = "
                + eventSetId + "]";
    }
}
