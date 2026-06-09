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

/// An on-screen touch button. Add one with `TouchControls#addButton(int, float,
/// float, float)`; while a finger is on it the framework holds down the key code it
/// is mapped to, so `GameInput#isKeyDown(int)` and `GameInput#wasKeyPressed(int)`
/// report it exactly as a hardware key. Map it to a game action with
/// `com.codename1.ui.Display#getKeyCode(int)` (e.g.
/// `Display.getInstance().getKeyCode(Display.GAME_FIRE)`) or to any key code of your
/// own.
///
/// The position and radius are in the `GameView`'s pixel coordinates.
public class VirtualButton {
    private final int keyCode;
    private final float centerX;
    private final float centerY;
    private final float radius;
    private String label;
    private int color = 0xc0ffffff;
    private boolean pressed;

    VirtualButton(int keyCode, float centerX, float centerY, float radius) {
        this.keyCode = keyCode;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    /// Optional short label drawn on the button (for example "A" or "Fire").
    public VirtualButton setLabel(String label) {
        this.label = label;
        return this;
    }

    /// The ARGB color the button is tinted (default a translucent white).
    public VirtualButton setColor(int argb) {
        this.color = argb;
        return this;
    }

    boolean contains(float px, float py) {
        float dx = px - centerX;
        float dy = py - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public boolean isPressed() {
        return pressed;
    }

    public int getKeyCode() {
        return keyCode;
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

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }
}
