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

/// Immutable snapshot describing the rich detail of a pointer interaction that goes
/// beyond the simple x/y coordinates carried by the legacy pointer callbacks.
///
/// A `PointerEvent` exposes which mouse button triggered the interaction, the kind of
/// pointing device used (finger, mouse, stylus or eraser), the pressure applied, stylus
/// tilt, the size of the contact area and the keyboard modifiers that were held down.
///
/// The snapshot is available in two ways:
///
/// - From an `ActionEvent` fired by a pointer listener via `ActionEvent#getPointerEvent()`.
/// - By polling `com.codename1.ui.Display#getCurrentPointerEvent()` (or the matching `com.codename1.ui.CN`
///   convenience method) while a pointer event is being dispatched.
///
/// Every value carries a safe default so code written against the rich API behaves predictably
/// on devices and ports that do not supply the extra detail. On such ports the button defaults to
/// `BUTTON_PRIMARY`, the pressure to `1.0`, the type to `TYPE_UNKNOWN` and tilt/contact size to `0`.
public final class PointerEvent {

    /// The pointing device could not be determined.
    public static final int TYPE_UNKNOWN = 0;

    /// The interaction came from a finger on a touch screen.
    public static final int TYPE_TOUCH = 1;

    /// The interaction came from a mouse or trackpad.
    public static final int TYPE_MOUSE = 2;

    /// The interaction came from a stylus or pen (Apple Pencil, S-Pen, Surface Pen and similar).
    public static final int TYPE_STYLUS = 3;

    /// The interaction came from the eraser end of a stylus.
    public static final int TYPE_ERASER = 4;

    /// No button is associated with this event (for example a hover or a plain touch).
    public static final int BUTTON_NONE = -1;

    /// The primary (usually left) mouse button, or a plain touch/stylus contact.
    public static final int BUTTON_PRIMARY = 0;

    /// The secondary (usually right) mouse button or stylus barrel button.
    public static final int BUTTON_SECONDARY = 1;

    /// The middle mouse button (often the scroll wheel click).
    public static final int BUTTON_MIDDLE = 2;

    /// The back (fourth) mouse button.
    public static final int BUTTON_BACK = 3;

    /// The forward (fifth) mouse button.
    public static final int BUTTON_FORWARD = 4;

    /// Button mask bit for the primary button.
    public static final int MASK_PRIMARY = 1;

    /// Button mask bit for the secondary button.
    public static final int MASK_SECONDARY = 1 << 1;

    /// Button mask bit for the middle button.
    public static final int MASK_MIDDLE = 1 << 2;

    /// Button mask bit for the back button.
    public static final int MASK_BACK = 1 << 3;

    /// Button mask bit for the forward button.
    public static final int MASK_FORWARD = 1 << 4;

    /// Modifier mask bit indicating the shift key was held.
    public static final int MODIFIER_SHIFT = 1;

    /// Modifier mask bit indicating the control key was held.
    public static final int MODIFIER_CONTROL = 1 << 1;

    /// Modifier mask bit indicating the alt/option key was held.
    public static final int MODIFIER_ALT = 1 << 2;

    /// Modifier mask bit indicating the meta/command/windows key was held.
    public static final int MODIFIER_META = 1 << 3;

    private final int x;
    private final int y;
    private final int button;
    private final int buttonMask;
    private final int pointerType;
    private final float pressure;
    private final float tiltX;
    private final float tiltY;
    private final float contactSize;
    private final int modifiers;
    private final boolean hovering;

    /// Creates a new immutable pointer event snapshot.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x position of the pointer in display pixels
    ///
    /// - `y`: the y position of the pointer in display pixels
    ///
    /// - `button`: one of the `BUTTON_*` constants describing the triggering button
    ///
    /// - `buttonMask`: bitmask of the `MASK_*` constants describing all currently held buttons
    ///
    /// - `pointerType`: one of the `TYPE_*` constants describing the pointing device
    ///
    /// - `pressure`: normalized pressure between `0.0` and `1.0`, defaulting to `1.0`
    ///
    /// - `tiltX`: stylus tilt across the x axis in degrees, `0` when not available
    ///
    /// - `tiltY`: stylus tilt across the y axis in degrees, `0` when not available
    ///
    /// - `contactSize`: normalized size of the contact area between `0.0` and `1.0`
    ///
    /// - `modifiers`: bitmask of the `MODIFIER_*` constants describing held keyboard modifiers
    ///
    /// - `hovering`: true if the pointer is hovering above the surface without contact
    public PointerEvent(int x, int y, int button, int buttonMask, int pointerType,
            float pressure, float tiltX, float tiltY, float contactSize, int modifiers, boolean hovering) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.buttonMask = buttonMask;
        this.pointerType = pointerType;
        this.pressure = pressure;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.contactSize = contactSize;
        this.modifiers = modifiers;
        this.hovering = hovering;
    }

    /// The x position of the pointer in display pixels.
    public int getX() {
        return x;
    }

    /// The y position of the pointer in display pixels.
    public int getY() {
        return y;
    }

    /// The button that triggered this event, one of the `BUTTON_*` constants. Defaults to
    /// `BUTTON_PRIMARY` for plain touch interactions and `BUTTON_NONE` for hover events.
    public int getButton() {
        return button;
    }

    /// A bitmask of all buttons currently held down, built from the `MASK_*` constants.
    public int getButtonMask() {
        return buttonMask;
    }

    /// The kind of pointing device that produced this event, one of the `TYPE_*` constants.
    public int getPointerType() {
        return pointerType;
    }

    /// The normalized pressure applied by the pointer between `0.0` and `1.0`. Ports that do not
    /// report pressure return `1.0` so that pressure aware code keeps working.
    public float getPressure() {
        return pressure;
    }

    /// The stylus tilt across the x axis in degrees, or `0` when not reported.
    public float getTiltX() {
        return tiltX;
    }

    /// The stylus tilt across the y axis in degrees, or `0` when not reported.
    public float getTiltY() {
        return tiltY;
    }

    /// The normalized size of the contact area between `0.0` and `1.0`, or `0` when not reported.
    public float getContactSize() {
        return contactSize;
    }

    /// A bitmask of the keyboard modifiers held during this event, built from the `MODIFIER_*` constants.
    public int getModifiers() {
        return modifiers;
    }

    /// True if the pointer is hovering above the surface without making contact.
    public boolean isHovering() {
        return hovering;
    }

    /// True if this event came from a finger on a touch screen.
    public boolean isTouch() {
        return pointerType == TYPE_TOUCH;
    }

    /// True if this event came from a mouse or trackpad.
    public boolean isMouse() {
        return pointerType == TYPE_MOUSE;
    }

    /// True if this event came from a stylus or the eraser end of a stylus.
    public boolean isStylus() {
        return pointerType == TYPE_STYLUS || pointerType == TYPE_ERASER;
    }

    /// True if this event came from the eraser end of a stylus.
    public boolean isEraser() {
        return pointerType == TYPE_ERASER;
    }

    /// True if the triggering button is the primary (left) button.
    public boolean isPrimaryButton() {
        return button == BUTTON_PRIMARY;
    }

    /// True if the triggering button is the secondary (right) button or stylus barrel button.
    public boolean isSecondaryButton() {
        return button == BUTTON_SECONDARY;
    }

    /// True if the triggering button is the middle button.
    public boolean isMiddleButton() {
        return button == BUTTON_MIDDLE;
    }

    /// True if the shift key was held during this event.
    public boolean isShiftDown() {
        return (modifiers & MODIFIER_SHIFT) != 0;
    }

    /// True if the control key was held during this event.
    public boolean isControlDown() {
        return (modifiers & MODIFIER_CONTROL) != 0;
    }

    /// True if the alt/option key was held during this event.
    public boolean isAltDown() {
        return (modifiers & MODIFIER_ALT) != 0;
    }

    /// True if the meta/command/windows key was held during this event.
    public boolean isMetaDown() {
        return (modifiers & MODIFIER_META) != 0;
    }

    public String toString() {
        return "PointerEvent[x=" + x + ", y=" + y + ", button=" + button + ", buttonMask=" + buttonMask
                + ", type=" + pointerType + ", pressure=" + pressure + ", tiltX=" + tiltX + ", tiltY=" + tiltY
                + ", contactSize=" + contactSize + ", modifiers=" + modifiers + ", hovering=" + hovering + "]";
    }
}
