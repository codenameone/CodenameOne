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

import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/// The base of the events a gesture produces, such as a swipe.
///
/// A gesture is reported where it began. Its position is the centre of the
/// gesture in the coordinates of the node the event is delivered to, in the
/// scene and on the screen.
public class GestureEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// The supertype of every gesture event.
    public static final EventType<GestureEvent> ANY = new EventType<GestureEvent>(InputEvent.ANY, "GESTURE");

    private transient double x;
    private transient double y;
    private final double screenX;
    private final double screenY;
    private final double sceneX;
    private final double sceneY;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;
    private final boolean direct;
    private final boolean inertia;
    private final transient PickResult pickResult;

    /// Creates a gesture event at scene coordinates.
    protected GestureEvent(Object source, EventTarget target, EventType<? extends GestureEvent> eventType, double x,
            double y, double screenX, double screenY, boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown, boolean direct, boolean inertia, PickResult pickResult) {
        super(source, target, eventType);
        this.sceneX = x;
        this.sceneY = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.direct = direct;
        this.inertia = inertia;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        localize(source);
    }

    /// Creates a gesture event with no source or target.
    protected GestureEvent(EventType<? extends GestureEvent> eventType, double x, double y, double screenX,
            double screenY, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown,
            boolean direct, boolean inertia, PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown, altDown, metaDown, direct,
                inertia, pickResult);
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

    /// Re-targets this event; the position follows the new source.
    @Override
    public GestureEvent copyFor(Object newSource, EventTarget newTarget) {
        Object copied = super.copyFor(newSource, newTarget);
        if (copied instanceof GestureEvent) {
            GestureEvent e = (GestureEvent) copied;
            e.localize(e.getSource());
            return e;
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends GestureEvent> getEventType() {
        return (EventType<? extends GestureEvent>) super.getEventType();
    }

    /// Returns the horizontal position in the coordinates of the source.
    public final double getX() {
        return x;
    }

    /// Returns the vertical position in the coordinates of the source.
    public final double getY() {
        return y;
    }

    /// Returns the depth of the position, always zero in a flat scene.
    public final double getZ() {
        return 0;
    }

    /// Returns the horizontal position on the screen.
    public final double getScreenX() {
        return screenX;
    }

    /// Returns the vertical position on the screen.
    public final double getScreenY() {
        return screenY;
    }

    /// Returns the horizontal position in the scene.
    public final double getSceneX() {
        return sceneX;
    }

    /// Returns the vertical position in the scene.
    public final double getSceneY() {
        return sceneY;
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

    /// Returns whether the gesture was made on the screen itself rather
    /// than on a trackpad.
    public final boolean isDirect() {
        return direct;
    }

    /// Returns whether the event continues a gesture the user already
    /// ended.
    public boolean isInertia() {
        return inertia;
    }

    /// Returns what was under the gesture.
    public final PickResult getPickResult() {
        return pickResult;
    }

    /// Returns whether the platform's shortcut modifier was held.
    public final boolean isShortcutDown() {
        return controlDown || metaDown;
    }
}
