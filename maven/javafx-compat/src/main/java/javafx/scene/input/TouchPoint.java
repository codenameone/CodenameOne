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
import javafx.geometry.Point2D;
import javafx.scene.Node;

/// One finger on the screen, as a [TouchEvent] reports it.
public final class TouchPoint {

    /// What a finger did since the event before.
    public enum State {
        /// The finger went down.
        PRESSED,
        /// The finger moved.
        MOVED,
        /// The finger stayed where it was.
        STATIONARY,
        /// The finger was lifted.
        RELEASED
    }

    private final int id;
    private final State state;
    private double x;
    private double y;
    private final double screenX;
    private final double screenY;
    private final double sceneX;
    private final double sceneY;
    private final EventTarget target;
    private EventTarget grabbed;
    private final PickResult pickResult;

    /// Creates a touch point at scene coordinates.
    public TouchPoint(@NamedArg("id") int id, @NamedArg("state") State state, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("target") EventTarget target, @NamedArg("pickResult") PickResult pickResult) {
        this.id = id;
        this.state = state;
        this.x = x;
        this.y = y;
        this.sceneX = x;
        this.sceneY = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.target = target;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
    }

    /// Moves the point into the coordinates of the node an event is being
    /// delivered to.
    void localize(Object source) {
        if (source instanceof Node) {
            Point2D local = ((Node) source).sceneToLocal(sceneX, sceneY);
            x = local.getX();
            y = local.getY();
        } else {
            x = sceneX;
            y = sceneY;
        }
    }

    /// Returns whether this point is delivered to a target or to one of
    /// the nodes inside it.
    public boolean belongsTo(EventTarget target) {
        EventTarget mine = this.target;
        if (mine instanceof Node) {
            Node node = (Node) mine;
            for (Node n = node; n != null; n = n.getParent()) {
                if (n == target) {
                    return true;
                }
            }
            return target != null && node.getScene() == target;
        }
        return mine == target;
    }

    /// Delivers the rest of this finger's events to a target.
    public void grab(EventTarget target) {
        grabbed = target;
    }

    /// Delivers the rest of this finger's events to where this one went.
    public void grab() {
        grabbed = target;
    }

    /// Delivers this finger's events to whatever is under it again.
    public void ungrab() {
        grabbed = null;
    }

    /// Returns the target that grabbed this finger, `null` for none.
    public EventTarget getGrabbed() {
        return grabbed;
    }

    /// Returns the number that tells this finger from the others.
    public final int getId() {
        return id;
    }

    /// Returns what the finger did.
    public final State getState() {
        return state;
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

    /// Returns what was under the finger.
    public final PickResult getPickResult() {
        return pickResult;
    }

    /// Returns where the finger's events are delivered.
    public EventTarget getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "TouchPoint [state = " + state + ", id = " + id + ", x = " + x + ", y = " + y + "]";
    }
}
