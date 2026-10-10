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
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/// The mouse wheel or a track pad scrolling. A positive `deltaY` scrolls
/// the content down, which is the wheel turning away from the user.
public final class ScrollEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    /// Every scroll event.
    public static final EventType<ScrollEvent> ANY = new EventType<ScrollEvent>(InputEvent.ANY, "ANY_SCROLL");

    /// The content was scrolled.
    public static final EventType<ScrollEvent> SCROLL = new EventType<ScrollEvent>(ANY, "SCROLL");

    /// A scroll gesture started.
    public static final EventType<ScrollEvent> SCROLL_STARTED = new EventType<ScrollEvent>(ANY, "SCROLL_STARTED");

    /// A scroll gesture ended.
    public static final EventType<ScrollEvent> SCROLL_FINISHED = new EventType<ScrollEvent>(ANY, "SCROLL_FINISHED");

    /// The unit of the horizontal text based scroll amount.
    public enum HorizontalTextScrollUnits {
        /// No text based amount is available.
        NONE,
        /// The amount is in characters.
        CHARACTERS
    }

    /// The unit of the vertical text based scroll amount.
    public enum VerticalTextScrollUnits {
        /// No text based amount is available.
        NONE,
        /// The amount is in lines.
        LINES,
        /// The amount is in pages.
        PAGES
    }

    private transient double x;
    private transient double y;
    private final double screenX;
    private final double screenY;
    private final double sceneX;
    private final double sceneY;
    private final double deltaX;
    private final double deltaY;
    private final double totalDeltaX;
    private final double totalDeltaY;
    private final HorizontalTextScrollUnits textDeltaXUnits;
    private final VerticalTextScrollUnits textDeltaYUnits;
    private final double textDeltaX;
    private final double textDeltaY;
    private final int touchCount;
    private final boolean inertia;
    private final boolean direct;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;
    private final transient PickResult pickResult;

    /// Creates a scroll event; `x` and `y` are scene coordinates.
    public ScrollEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
            @NamedArg("eventType") EventType<ScrollEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct, @NamedArg("inertia") boolean inertia,
            @NamedArg("deltaX") double deltaX, @NamedArg("deltaY") double deltaY,
            @NamedArg("totalDeltaX") double totalDeltaX, @NamedArg("totalDeltaY") double totalDeltaY,
            @NamedArg("textDeltaXUnits") HorizontalTextScrollUnits textDeltaXUnits,
            @NamedArg("textDeltaX") double textDeltaX,
            @NamedArg("textDeltaYUnits") VerticalTextScrollUnits textDeltaYUnits,
            @NamedArg("textDeltaY") double textDeltaY, @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
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
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.totalDeltaX = totalDeltaX;
        this.totalDeltaY = totalDeltaY;
        this.textDeltaXUnits = textDeltaXUnits;
        this.textDeltaX = textDeltaX;
        this.textDeltaYUnits = textDeltaYUnits;
        this.textDeltaY = textDeltaY;
        this.touchCount = touchCount;
        this.pickResult = pickResult != null ? pickResult : new PickResult(target, x, y);
        localize(source);
    }

    /// Creates a scroll event with no source and no target.
    public ScrollEvent(@NamedArg("eventType") EventType<ScrollEvent> eventType, @NamedArg("x") double x,
            @NamedArg("y") double y, @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct, @NamedArg("inertia") boolean inertia,
            @NamedArg("deltaX") double deltaX, @NamedArg("deltaY") double deltaY,
            @NamedArg("totalDeltaX") double totalDeltaX, @NamedArg("totalDeltaY") double totalDeltaY,
            @NamedArg("textDeltaXUnits") HorizontalTextScrollUnits textDeltaXUnits,
            @NamedArg("textDeltaX") double textDeltaX,
            @NamedArg("textDeltaYUnits") VerticalTextScrollUnits textDeltaYUnits,
            @NamedArg("textDeltaY") double textDeltaY, @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown, altDown, metaDown, direct,
                inertia, deltaX, deltaY, totalDeltaX, totalDeltaY, textDeltaXUnits, textDeltaX, textDeltaYUnits,
                textDeltaY, touchCount, pickResult);
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
    public ScrollEvent copyFor(Object newSource, EventTarget newTarget) {
        return copyFor(newSource, newTarget, getEventType());
    }

    /// Returns a copy with another source, target and type.
    public ScrollEvent copyFor(Object newSource, EventTarget newTarget, EventType<ScrollEvent> type) {
        return new ScrollEvent(newSource, newTarget, type, sceneX, sceneY, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, deltaX, deltaY, totalDeltaX, totalDeltaY, textDeltaXUnits,
                textDeltaX, textDeltaYUnits, textDeltaY, touchCount, pickResult);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventType<ScrollEvent> getEventType() {
        return (EventType<ScrollEvent>) super.getEventType();
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

    /// Returns the horizontal amount scrolled, in logical pixels.
    public double getDeltaX() {
        return deltaX;
    }

    /// Returns the vertical amount scrolled, in logical pixels.
    public double getDeltaY() {
        return deltaY;
    }

    /// Returns the horizontal amount scrolled over the whole gesture.
    public double getTotalDeltaX() {
        return totalDeltaX;
    }

    /// Returns the vertical amount scrolled over the whole gesture.
    public double getTotalDeltaY() {
        return totalDeltaY;
    }

    /// Returns the unit of the horizontal text based amount.
    public HorizontalTextScrollUnits getTextDeltaXUnits() {
        return textDeltaXUnits;
    }

    /// Returns the unit of the vertical text based amount.
    public VerticalTextScrollUnits getTextDeltaYUnits() {
        return textDeltaYUnits;
    }

    /// Returns the horizontal text based amount.
    public double getTextDeltaX() {
        return textDeltaX;
    }

    /// Returns the vertical text based amount.
    public double getTextDeltaY() {
        return textDeltaY;
    }

    /// Returns the number of fingers; 0 for a wheel.
    public int getTouchCount() {
        return touchCount;
    }

    /// Returns whether the event was produced by scrolling inertia.
    public boolean isInertia() {
        return inertia;
    }

    /// Returns whether the event came from a touch on the screen itself.
    public final boolean isDirect() {
        return direct;
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

    /// Returns whether the platform's shortcut modifier is down.
    public final boolean isShortcutDown() {
        return SceneInput.shortcutIsMeta() ? metaDown : controlDown;
    }

    /// Returns what was under the pointer.
    public final PickResult getPickResult() {
        return pickResult;
    }
}
