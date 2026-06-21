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
package com.codename1.gaming.level;

/// A serializable directional light authored in a 3D `GameLevel`: a direction, a
/// diffuse `#getColor()` and an `#getAmbientColor()`. A `GameSceneView` copies the
/// level's primary light onto the `GameView`'s shared `com.codename1.gpu.Light`.
public class LevelLight {
    private float dirX;
    private float dirY = -1f;
    private float dirZ;
    private int color = 0xffffffff;
    private int ambientColor = 0xff202020;

    public LevelLight() {
    }

    public LevelLight(float dirX, float dirY, float dirZ, int color, int ambientColor) {
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.color = color;
        this.ambientColor = ambientColor;
    }

    public float getDirectionX() {
        return dirX;
    }

    public float getDirectionY() {
        return dirY;
    }

    public float getDirectionZ() {
        return dirZ;
    }

    public LevelLight setDirection(float x, float y, float z) {
        this.dirX = x;
        this.dirY = y;
        this.dirZ = z;
        return this;
    }

    public int getColor() {
        return color;
    }

    public LevelLight setColor(int color) {
        this.color = color;
        return this;
    }

    public int getAmbientColor() {
        return ambientColor;
    }

    public LevelLight setAmbientColor(int ambientColor) {
        this.ambientColor = ambientColor;
        return this;
    }
}
