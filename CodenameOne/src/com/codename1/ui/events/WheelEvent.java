/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.events;

/// Event delivered to mouse wheel listeners when the wheel or a trackpad scroll gesture is moved.
///
/// The deltas are reported in display pixels (already converted from the native notch count by the
/// port). A positive `deltaY` reveals content above (the user scrolled the wheel down), and a
/// positive `deltaX` reveals content to the left. Consuming this event with `#consume()` prevents
/// the default scrolling behavior, which is useful for gestures such as control plus wheel to zoom.
public class WheelEvent extends ActionEvent {

    private final int deltaX;
    private final int deltaY;
    private final boolean precise;
    private final int modifiers;

    /// Creates a new wheel event.
    ///
    /// #### Parameters
    ///
    /// - `source`: the component the wheel was moved over
    ///
    /// - `x`: the pointer x position in display pixels
    ///
    /// - `y`: the pointer y position in display pixels
    ///
    /// - `deltaX`: the horizontal scroll amount in display pixels
    ///
    /// - `deltaY`: the vertical scroll amount in display pixels
    ///
    /// - `precise`: true if the deltas come from a high resolution device such as a trackpad
    ///
    /// - `modifiers`: bitmask of the `PointerEvent` `MODIFIER_*` constants for held keyboard modifiers
    public WheelEvent(Object source, int x, int y, int deltaX, int deltaY, boolean precise, int modifiers) {
        super(source, Type.PointerWheel, x, y);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.precise = precise;
        this.modifiers = modifiers;
    }

    /// The horizontal scroll amount in display pixels. A positive value reveals content to the left.
    public int getDeltaX() {
        return deltaX;
    }

    /// The vertical scroll amount in display pixels. A positive value reveals content above.
    public int getDeltaY() {
        return deltaY;
    }

    /// True if the deltas come from a high resolution device such as a trackpad rather than a
    /// notched mouse wheel.
    public boolean isPrecise() {
        return precise;
    }

    /// A bitmask of the keyboard modifiers held during this event, built from the `PointerEvent`
    /// `MODIFIER_*` constants.
    public int getModifiers() {
        return modifiers;
    }

    /// True if the shift key was held during this event.
    public boolean isShiftDown() {
        return (modifiers & PointerEvent.MODIFIER_SHIFT) != 0;
    }

    /// True if the control key was held during this event.
    public boolean isControlDown() {
        return (modifiers & PointerEvent.MODIFIER_CONTROL) != 0;
    }

    /// True if the alt/option key was held during this event.
    public boolean isAltDown() {
        return (modifiers & PointerEvent.MODIFIER_ALT) != 0;
    }

    /// True if the meta/command/windows key was held during this event.
    public boolean isMetaDown() {
        return (modifiers & PointerEvent.MODIFIER_META) != 0;
    }
}
