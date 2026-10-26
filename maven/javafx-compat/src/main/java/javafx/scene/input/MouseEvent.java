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

import com.codename1.fxcompat.runtime.SceneInput;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/// A mouse or touch event: a press, a release, a click, a move, a drag,
/// or the pointer entering or leaving a node.
///
/// `getX()` and `getY()` are in the coordinates of the event's source and
/// are recomputed each time the event is delivered to another node. On a
/// device without a mouse a touch is the primary button; there are no
/// move events and no hover.
public class MouseEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// Every mouse event.
    public static final EventType<MouseEvent> ANY = new EventType<MouseEvent>(InputEvent.ANY, "MOUSE");

    /// A button went down.
    public static final EventType<MouseEvent> MOUSE_PRESSED = new EventType<MouseEvent>(ANY, "MOUSE_PRESSED");

    /// A button went up.
    public static final EventType<MouseEvent> MOUSE_RELEASED = new EventType<MouseEvent>(ANY, "MOUSE_RELEASED");

    /// A button went down and up over the same node.
    public static final EventType<MouseEvent> MOUSE_CLICKED = new EventType<MouseEvent>(ANY, "MOUSE_CLICKED");

    /// The pointer entered a node or one of its descendants; delivered
    /// along the whole dispatch chain.
    public static final EventType<MouseEvent> MOUSE_ENTERED_TARGET = new EventType<MouseEvent>(ANY,
            "MOUSE_ENTERED_TARGET");

    /// The pointer entered a node; delivered to that node only.
    public static final EventType<MouseEvent> MOUSE_ENTERED = new EventType<MouseEvent>(MOUSE_ENTERED_TARGET,
            "MOUSE_ENTERED");

    /// The pointer left a node or one of its descendants; delivered along
    /// the whole dispatch chain.
    public static final EventType<MouseEvent> MOUSE_EXITED_TARGET = new EventType<MouseEvent>(ANY,
            "MOUSE_EXITED_TARGET");

    /// The pointer left a node; delivered to that node only.
    public static final EventType<MouseEvent> MOUSE_EXITED = new EventType<MouseEvent>(MOUSE_EXITED_TARGET,
            "MOUSE_EXITED");

    /// The pointer moved with no button down.
    public static final EventType<MouseEvent> MOUSE_MOVED = new EventType<MouseEvent>(ANY, "MOUSE_MOVED");

    /// The pointer moved with a button down.
    public static final EventType<MouseEvent> MOUSE_DRAGGED = new EventType<MouseEvent>(ANY, "MOUSE_DRAGGED");

    /// A drag gesture was recognised.
    public static final EventType<MouseEvent> DRAG_DETECTED = new EventType<MouseEvent>(ANY, "DRAG_DETECTED");

    private transient double x;
    private transient double y;
    private final double screenX;
    private final double screenY;
    private final double sceneX;
    private final double sceneY;
    private final MouseButton button;
    private final int clickCount;
    private final boolean stillSincePress;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;
    private final boolean synthesized;
    private final boolean popupTrigger;
    private final boolean primaryButtonDown;
    private final boolean secondaryButtonDown;
    private final boolean middleButtonDown;
    private final transient PickResult pickResult;
    private boolean dragDetect = true;

    /// Creates a mouse event with no source and no target; `x` and `y`
    /// are scene coordinates.
    public MouseEvent(@NamedArg("eventType") EventType<? extends MouseEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button, @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown,
            @NamedArg("middleButtonDown") boolean middleButtonDown,
            @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized, @NamedArg("popupTrigger") boolean popupTrigger,
            @NamedArg("stillSincePress") boolean stillSincePress, @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, button, clickCount, shiftDown, controlDown, altDown,
                metaDown, primaryButtonDown, middleButtonDown, secondaryButtonDown, synthesized, popupTrigger,
                stillSincePress, pickResult);
    }

    /// Creates a mouse event; `x` and `y` are scene coordinates.
    public MouseEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<? extends MouseEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("button") MouseButton button, @NamedArg("clickCount") int clickCount,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("primaryButtonDown") boolean primaryButtonDown,
            @NamedArg("middleButtonDown") boolean middleButtonDown,
            @NamedArg("secondaryButtonDown") boolean secondaryButtonDown,
            @NamedArg("synthesized") boolean synthesized, @NamedArg("popupTrigger") boolean popupTrigger,
            @NamedArg("stillSincePress") boolean stillSincePress, @NamedArg("pickResult") PickResult pickResult) {
        super(source, target, eventType);
        this.sceneX = x;
        this.sceneY = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.button = button == null ? MouseButton.NONE : button;
        this.clickCount = clickCount;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
        this.primaryButtonDown = primaryButtonDown;
        this.middleButtonDown = middleButtonDown;
        this.secondaryButtonDown = secondaryButtonDown;
        this.synthesized = synthesized;
        this.popupTrigger = popupTrigger;
        this.stillSincePress = stillSincePress;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        localize(source);
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
    public MouseEvent copyFor(Object newSource, EventTarget newTarget) {
        if (getClass() != MouseEvent.class) {
            super.copyFor(newSource, newTarget);
            localize(getSource());
            return this;
        }
        return copyFor(newSource, newTarget, getEventType());
    }

    /// Returns a copy with another source, target and type.
    public MouseEvent copyFor(Object newSource, EventTarget newTarget, EventType<? extends MouseEvent> eventType) {
        MouseEvent copy = new MouseEvent(newSource, newTarget, eventType, sceneX, sceneY, screenX, screenY, button,
                clickCount, shiftDown, controlDown, altDown, metaDown, primaryButtonDown, middleButtonDown,
                secondaryButtonDown, synthesized, popupTrigger, stillSincePress, pickResult);
        copy.dragDetect = dragDetect;
        return copy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<? extends MouseEvent> getEventType() {
        return (EventType<? extends MouseEvent>) super.getEventType();
    }

    /// Returns the x of the pointer in the source's coordinates.
    public final double getX() {
        return x;
    }

    /// Returns the y of the pointer in the source's coordinates.
    public final double getY() {
        return y;
    }

    /// Always 0: this layer is two dimensional.
    public final double getZ() {
        return 0;
    }

    /// Returns the x of the pointer on the screen.
    public final double getScreenX() {
        return screenX;
    }

    /// Returns the y of the pointer on the screen.
    public final double getScreenY() {
        return screenY;
    }

    /// Returns the x of the pointer in the scene.
    public final double getSceneX() {
        return sceneX;
    }

    /// Returns the y of the pointer in the scene.
    public final double getSceneY() {
        return sceneY;
    }

    /// Returns the button whose change caused the event.
    public final MouseButton getButton() {
        return button;
    }

    /// Returns how many clicks in a row this is.
    public final int getClickCount() {
        return clickCount;
    }

    /// Returns whether the pointer stayed in place since the press.
    public final boolean isStillSincePress() {
        return stillSincePress;
    }

    /// Returns whether Shift is down.
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /// Returns whether Control is down.
    public final boolean isControlDown() {
        return controlDown;
    }

    /// Returns whether Alt is down.
    public final boolean isAltDown() {
        return altDown;
    }

    /// Returns whether Meta is down.
    public final boolean isMetaDown() {
        return metaDown;
    }

    /// Returns whether the event comes from a touch rather than a mouse.
    public boolean isSynthesized() {
        return synthesized;
    }

    /// Returns whether the platform's shortcut modifier is down: Meta on
    /// Apple platforms, Control elsewhere.
    public final boolean isShortcutDown() {
        return SceneInput.shortcutIsMeta() ? metaDown : controlDown;
    }

    /// Returns whether this event should open a context menu.
    public final boolean isPopupTrigger() {
        return popupTrigger;
    }

    /// Returns whether the primary button is down.
    public final boolean isPrimaryButtonDown() {
        return primaryButtonDown;
    }

    /// Returns whether the secondary button is down.
    public final boolean isSecondaryButtonDown() {
        return secondaryButtonDown;
    }

    /// Returns whether the middle button is down.
    public final boolean isMiddleButtonDown() {
        return middleButtonDown;
    }

    /// Returns whether a drag is to be detected after this event.
    public boolean isDragDetect() {
        return dragDetect;
    }

    /// Sets whether a drag is to be detected after this event.
    public void setDragDetect(boolean dragDetect) {
        this.dragDetect = dragDetect;
    }

    /// Returns what was under the pointer.
    public final PickResult getPickResult() {
        return pickResult;
    }

    /// Returns a copy of a mouse event with another type.
    public static MouseEvent copyForMouseDragEvent(MouseEvent e, Object source, EventTarget target,
            EventType<MouseEvent> type, Object gestureSource, PickResult pickResult) {
        return e.copyFor(source, target, type);
    }

    @Override
    public String toString() {
        return "MouseEvent [source = " + getSource() + ", target = " + getTarget() + ", eventType = "
                + getEventType() + ", consumed = " + isConsumed() + ", x = " + x + ", y = " + y + ", button = "
                + button + ", clickCount = " + clickCount + "]";
    }
}
