/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gaming;

/// An on-screen analog stick. Add one with `TouchControls#addJoystick(float, float,
/// float)`; the framework draws it and feeds its direction into `GameInput`, so the
/// game reads it the same way whether the player uses touch or a keyboard:
///
/// - as **analog** axes via `GameInput#getAxisX()` / `GameInput#getAxisY()` (each
///   -1..1, with x pointing right and y pointing down the screen), and
/// - as **digital** game actions: pushing the stick past its dead zone presses
///   `com.codename1.ui.Display#GAME_LEFT` / `GAME_RIGHT` / `GAME_UP` / `GAME_DOWN`,
///   so `GameInput#isGameKeyDown(int)` works unchanged.
///
/// Positions and sizes are in the `GameView`'s pixel coordinates (origin at the top
/// left).
public class VirtualJoystick {
    private final float centerX;
    private final float centerY;
    private final float radius;
    private float knobRadius;
    private float deadZone = 0.2f;

    private boolean active;
    private float knobX;
    private float knobY;
    private float axisX;
    private float axisY;

    VirtualJoystick(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.knobRadius = radius * 0.45f;
        this.knobX = centerX;
        this.knobY = centerY;
    }

    /// The fraction of the radius that registers as no input (0..1, default 0.2).
    public VirtualJoystick setDeadZone(float deadZone) {
        this.deadZone = deadZone;
        return this;
    }

    public VirtualJoystick setKnobRadius(float knobRadius) {
        this.knobRadius = knobRadius;
        return this;
    }

    /// True if the given view-local point is within the stick's base.
    boolean contains(float px, float py) {
        float dx = px - centerX;
        float dy = py - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    /// Grabs the stick with a touch at the given point.
    void press(float px, float py) {
        active = true;
        moveTo(px, py);
    }

    /// Moves the grabbed knob toward the point, clamped to the base radius.
    void drag(float px, float py) {
        if (active) {
            moveTo(px, py);
        }
    }

    /// Releases the stick, recentering the knob and zeroing the axes.
    void release() {
        active = false;
        knobX = centerX;
        knobY = centerY;
        axisX = 0;
        axisY = 0;
    }

    private void moveTo(float px, float py) {
        float dx = px - centerX;
        float dy = py - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > radius && dist > 0) {
            dx = dx / dist * radius;
            dy = dy / dist * radius;
            dist = radius;
        }
        knobX = centerX + dx;
        knobY = centerY + dy;
        float ax = dx / radius;
        float ay = dy / radius;
        axisX = applyDeadZone(ax);
        axisY = applyDeadZone(ay);
    }

    private float applyDeadZone(float v) {
        if (v > -deadZone && v < deadZone) {
            return 0f;
        }
        return v;
    }

    public boolean isActive() {
        return active;
    }

    public float getAxisX() {
        return axisX;
    }

    public float getAxisY() {
        return axisY;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    public float getKnobRadius() {
        return knobRadius;
    }

    public float getKnobX() {
        return knobX;
    }

    public float getKnobY() {
        return knobY;
    }

    float getDeadZone() {
        return deadZone;
    }
}
